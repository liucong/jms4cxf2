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

package org.apache.cxf.interceptor;



import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.sun.xml.fastinfoset.stax.factory.StAXInputFactory;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class FIStaxInInterceptor extends AbstractPhaseInterceptor<Message> {
    XMLInputFactory factory = new StAXInputFactory();

    public FIStaxInInterceptor() {
        this(Phase.POST_STREAM);
    }
    public FIStaxInInterceptor(String phase) {
        super(phase);
        addBefore(StaxInInterceptor.class.getName());
    }
    protected boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE));
    }

    public void handleMessage(Message message) {
        if (isGET(message) || message.getContentFormats().contains(XMLStreamReader.class)) {
            return;
        }

        String ct = (String)message.get(Message.CONTENT_TYPE);
        if (ct != null && ct.indexOf("fastinfoset") != -1) {
            message.put(XMLInputFactory.class.getName(), factory);
            ct = ct.replace("fastinfoset", "xml");
            if (ct.contains("application/xml")) {
                ct = ct.replace("application/xml", "text/xml"); 
            }
            message.put(Message.CONTENT_TYPE, ct);
            
            message.getExchange().put(FIStaxOutInterceptor.FI_ENABLED, Boolean.TRUE);
            if (isRequestor(message)) {
                //record the fact that is worked so future requests will 
                //automatically be FI enabled
                Endpoint ep = message.getExchange().get(Endpoint.class);
                ep.put(FIStaxOutInterceptor.FI_ENABLED, Boolean.TRUE);
            }
        }
    }
}
