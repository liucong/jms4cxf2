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

package org.apache.cxf.jms.testsuite.testcases;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import javax.jms.MessageListener;
import javax.xml.namespace.QName;

import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.jms_testsuite.JMSTestSuitePortType;
import org.apache.cxf.jms_testsuite.JMSTestSuiteService;
import org.apache.cxf.transport.jms.AbstractJMSTester;
import org.apache.cxf.transport.jms.JMSBrokerSetup;

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

    public URL getWSDLURL(String s) throws Exception {
        return getClass().getResource(s);
    }

    public QName getServiceName(QName q) {
        return q;
    }

    public QName getPortName(QName q) {
        return q;
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

        QName serviceName = getServiceName(new QName("http://cxf.apache.org/jms_testsuite",
                                                     "JMSTestSuiteService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/jms_testsuite",
                                               "TestSuitePort"));
        URL wsdl = getWSDLURL("/wsdl/jms_spec_testsuite.wsdl");
        assertNotNull(wsdl);

        JMSTestSuiteService service = new JMSTestSuiteService(wsdl, serviceName);
        assertNotNull(service);

        try {
            JMSTestSuitePortType test = service.getPort(portName, JMSTestSuitePortType.class);
            test.greetMeOneWay("test String");
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    private class MessageTest implements MessageListener {
        public void onMessage(javax.jms.Message message) {
            System.out.println("test ok");
        }
    }
}
