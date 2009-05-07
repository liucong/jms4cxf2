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

package org.apache.cxf.ws.policy;

import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;

/**
 * 
 */
public class PolicyVerificationInInterceptor extends AbstractPolicyInterceptor {
    public static final PolicyVerificationInInterceptor INSTANCE = new PolicyVerificationInInterceptor();
    
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyVerificationInInterceptor.class);

    public PolicyVerificationInInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    /** 
     * Determines the effective policy, and checks if one of its alternatives  
     * is supported.
     *  
     * @param message
     * @throws PolicyException if none of the alternatives is supported
     */
    protected void handle(Message message) {
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (null == aim) {
            return;
        }

        Exchange exchange = message.getExchange();
        BindingOperationInfo boi = exchange.get(BindingOperationInfo.class);
        if (null == boi) {
            LOG.fine("No binding operation info.");
            return;
        }
        
        Endpoint e = exchange.get(Endpoint.class);
        if (null == e) {
            LOG.fine("No endpoint.");
            return;
        } 
        EndpointInfo ei = e.getEndpointInfo();

        Bus bus = exchange.get(Bus.class);
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        if (null == pe) {
            return;
        }
        
        if (MessageUtils.isPartialResponse(message)) {
            LOG.fine("Not verifying policies on inbound partial response.");
            return;
        }
        
        getTransportAssertions(message);  
        
        EffectivePolicy effectivePolicy = message.get(EffectivePolicy.class);
        if (effectivePolicy == null) {
            if (MessageUtils.isRequestor(message)) {
                effectivePolicy = pe.getEffectiveClientResponsePolicy(ei, boi);
            } else {
                effectivePolicy = pe.getEffectiveServerRequestPolicy(ei, boi);
            }
        }
                
        aim.checkEffectivePolicy(effectivePolicy.getPolicy());
        LOG.fine("Verified policies for inbound message.");
    }

}
