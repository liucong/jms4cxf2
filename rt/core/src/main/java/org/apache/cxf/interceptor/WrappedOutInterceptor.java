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

import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;

public class WrappedOutInterceptor extends AbstractOutDatabindingInterceptor {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(WrappedOutInterceptor.class);

    private WrappedOutEndingInterceptor ending = new WrappedOutEndingInterceptor();
    
    public WrappedOutInterceptor() {
        this(Phase.MARSHAL);
    }
    public WrappedOutInterceptor(String phase) {
        super(phase);
        addBefore(BareOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);

        if (bop != null && bop.isUnwrapped()) {
            XMLStreamWriter xmlWriter = message.getContent(XMLStreamWriter.class);

            MessageInfo messageInfo;
            if (isRequestor(message)) {
                messageInfo = bop.getWrappedOperation().getOperationInfo().getInput();
            } else {
                messageInfo = bop.getWrappedOperation().getOperationInfo().getOutput();
            }

            MessagePartInfo part = messageInfo.getMessageParts().get(0);
            QName name = part.getConcreteName();

            try {
                
                int x = 1;
                while (xmlWriter.getNamespaceContext().getNamespaceURI("ns" + x) != null) {
                    x++;
                }
                xmlWriter.setPrefix("ns" + x, name.getNamespaceURI());
                xmlWriter.writeStartElement("ns" + x, name.getLocalPart(), name.getNamespaceURI());
                xmlWriter.writeNamespace("ns" + x, name.getNamespaceURI());
            } catch (XMLStreamException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE), e);
            }

            // Add a final interceptor to write end element
            message.getInterceptorChain().add(ending);
        }
    }
    
    public class WrappedOutEndingInterceptor extends AbstractOutDatabindingInterceptor {
        public WrappedOutEndingInterceptor() {
            super(Phase.MARSHAL_ENDING);
        }

        public void handleMessage(Message message) throws Fault {
            try {
                XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
                if (xtw != null) {
                    xtw.writeEndElement();
                }
            } catch (XMLStreamException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE), e);
            }
        }

    }  
}
