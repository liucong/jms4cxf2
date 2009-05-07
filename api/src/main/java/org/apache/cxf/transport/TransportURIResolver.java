/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.service.model.EndpointInfo;

/**
 * 
 */
public class TransportURIResolver extends ExtendedURIResolver {
    protected Bus bus;
    
    public TransportURIResolver(Bus b) {
        super();
        bus = b;
    }
    
    public InputSource resolve(String curUri, String baseUri) {
        InputSource is = null;
        URI base;
        try {
            if (baseUri == null) {
                base = new URI(curUri);
            } else {
                base = new URI(baseUri);
                base = base.resolve(curUri);
            }
        } catch (URISyntaxException use) {
            //ignore
            base = null;
        }
        try {
            if (base == null || !"https".equals(base.getScheme())) {
                is = super.resolve(curUri, baseUri);
            }
        } catch (Exception ex) {
            //nothing
        }
        if (is == null && base != null && base.getScheme() != null) {
            try {
                ConduitInitiator ci = bus.getExtension(ConduitInitiatorManager.class)
                    .getConduitInitiatorForUri(base.toString());
                EndpointInfo info = new EndpointInfo();
                info.setAddress(base.toString());
                final Conduit c = ci.getConduit(info);
                Message message = new MessageImpl();
                Exchange exch = new ExchangeImpl();
                message.setExchange(exch);
                
                message.put(Message.HTTP_REQUEST_METHOD, "GET");
                c.setMessageObserver(new MessageObserver() {
                    public void onMessage(Message message) {
                        LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream();
                        try {
                            IOUtils.copy(message.getContent(InputStream.class), bout);
                            message.getExchange().put(InputStream.class, bout.createInputStream());
                            c.close(message);
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                });
                c.prepare(message);
                c.close(message);
                InputStream ins = exch.get(InputStream.class);
                resourceOpened.addElement(ins);
                InputSource src = new InputSource(ins);
                src.setPublicId(base.toString());
                src.setSystemId(base.toString());
                lastestImportUri = base.toString();
                currentResolver.unresolve();
                return src;
            } catch (Exception e) {
                //ignore
            }
        }
        if (is == null && "https".equals(base.getScheme())) {
            is = super.resolve(curUri, baseUri);            
        }
        return is;
    }
}
