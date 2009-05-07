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
package org.apache.cxf.binding.http.interceptor;

import org.apache.cxf.binding.xml.interceptor.XMLMessageInInterceptor;
import org.apache.cxf.interceptor.DocLiteralInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.interceptor.WrappedInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class DatabindingInSetupInterceptor extends AbstractPhaseInterceptor<Message> {
    
    private static final WrappedInInterceptor WRAPPED_IN = new WrappedInInterceptor();
    private static final XMLMessageInInterceptor XML_IN = new XMLMessageInInterceptor();
    private static final DocLiteralInInterceptor DOCLIT_IN = new DocLiteralInInterceptor();
    private static final URIParameterInInterceptor URI_IN = new URIParameterInInterceptor();
    private static final StaxInInterceptor STAX_IN = new StaxInInterceptor();
    private static final DispatchInterceptor DISPATCH_IN = new DispatchInterceptor();
    
    public DatabindingInSetupInterceptor() {
        super(Phase.RECEIVE);
    }

    public void handleMessage(Message message) throws Fault {
        boolean client = Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE));
        InterceptorChain chain = message.getInterceptorChain();
        
        if (client) {
            chain.add(DOCLIT_IN);
            chain.add(XML_IN);
            chain.add(WRAPPED_IN);
            chain.add(STAX_IN);
        } else {
            chain.add(URI_IN);
            chain.add(DISPATCH_IN);
        }
    }

}
