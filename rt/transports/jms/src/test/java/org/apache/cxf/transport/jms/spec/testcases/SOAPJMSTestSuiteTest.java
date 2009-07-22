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

package org.apache.cxf.transport.jms.spec.testcases;

import javax.jms.MessageListener;

import org.apache.cxf.transport.jms.AbstractJMSTester;
import org.apache.cxf.transport.jms.JMSBrokerSetup;
import org.apache.cxf.transport.jms.spec.util.JMSTestUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.jms.listener.DefaultMessageListenerContainer;
/**
 * 
 */
public class SOAPJMSTestSuiteTest extends AbstractJMSTester {
    
    @BeforeClass
    public static void createAndStartBroker() throws Exception {
        startBroker(new JMSBrokerSetup("tcp://localhost:61500"));
    }
    
    @Test
    public void test0001() throws Exception {
        String destinationName = "dynamicQueues/testqueue";
        String address = "jms:jndi:"
                         + destinationName
                         + "?jndiInitialContextFactory"
                         + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                         + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=tcp://localhost:61500";

        DefaultMessageListenerContainer jmsListener = JMSTestUtil
            .createJmsListener(address, new MessageTest(), destinationName);
        
        
    }

    private class MessageTest implements MessageListener {
        public void onMessage(javax.jms.Message message) {
            System.out.println("test ok");
        }
    }
}
