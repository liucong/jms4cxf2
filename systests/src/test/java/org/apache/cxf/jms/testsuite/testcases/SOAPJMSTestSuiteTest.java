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

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.Message;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.jms_testsuite.JMSTestSuitePortType;
import org.apache.cxf.jms_testsuite.JMSTestSuiteService;
import org.apache.cxf.systest.jms.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.springframework.jms.core.JmsTemplate;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class SOAPJMSTestSuiteTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        Map<String, String> props = new HashMap<String, String>();
        if (System.getProperty("activemq.store.dir") != null) {
            props.put("activemq.store.dir", System.getProperty("activemq.store.dir"));
        }
        props.put("java.util.logging.config.file", System
            .getProperty("java.util.logging.config.file"));

        assertTrue("server did not launch correctly", launchServer(EmbeddedJMSBrokerLauncher.class,
                                                                   props, null));
    }

    public <T1, T2> T2 getPort(String serviceName, String portName, Class<T1> serviceClass,
                               Class<T2> portTypeClass) throws Exception {
        QName qServiceName = new QName("http://cxf.apache.org/jms_testsuite", "JMSTestSuiteService");
        QName qPortName = new QName("http://cxf.apache.org/jms_testsuite", "TestSuitePort");
        URL wsdl = getClass().getResource("/wsdl/jms_spec_testsuite.wsdl");

        Class<? extends Service> svcls = serviceClass.asSubclass(Service.class);

        Constructor<? extends Service> serviceConstructor = svcls.getConstructor(URL.class,
                                                                                 QName.class);
        Service service = serviceConstructor.newInstance(new Object[] {
            wsdl, qServiceName
        });
        return service.getPort(qPortName, portTypeClass);
    }

    @Test
    public void test0001() throws Exception {
        String destinationName = "dynamicQueues/testqueue";
        String address = "jms:jndi:"
                         + destinationName
                         + "?jndiInitialContextFactory"
                         + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                         + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=tcp://localhost:61500";
        JmsTemplate jmsTemplate = JMSTestUtil.getJmsTemplate(address);
        Destination dest = JMSTestUtil.getJmsDestination(jmsTemplate, destinationName,
                                                                 false);

        JMSTestSuitePortType test = getPort("JMSTestSuiteService", "TestSuitePort",
                                            JMSTestSuiteService.class, JMSTestSuitePortType.class);
        test.greetMeOneWay("test");

        Message message = jmsTemplate.receive(dest);
        assertEquals(message.getJMSDeliveryMode(), 2);
        assertEquals(message.getJMSPriority(), 4);
        assertEquals(message.getJMSExpiration(), 0);
        assertEquals(message.getJMSReplyTo(), null);
        assertEquals(message.getJMSCorrelationID(), null);
        assertEquals(message.getJMSDestination(), destinationName);
    }
}
