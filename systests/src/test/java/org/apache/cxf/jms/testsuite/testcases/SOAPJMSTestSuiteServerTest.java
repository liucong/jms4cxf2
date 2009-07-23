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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.jms.testsuite.services.Server;
import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.jms_simple.JMSSimplePortType;
import org.apache.cxf.jms_simple.JMSSimpleService;
import org.apache.cxf.systest.jms.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.testsuite.testcase.TestCaseType;
import org.apache.cxf.transport.jms.JMSConstants;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class SOAPJMSTestSuiteServerTest extends AbstractSOAPJMSTestSuite {

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
        assertTrue("server did not launch correctly", launchServer(Server.class, false));
    }

    @Test
    public void test0002() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0002");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService", "SimplePort",
                                                     JMSSimpleService.class,
                                                     JMSSimplePortType.class);
        InvocationHandler handler = Proxy.getInvocationHandler(simplePort);
        BindingProvider bp = null;

        if (handler instanceof BindingProvider) {
            bp = (BindingProvider)handler;
        }

        String response = simplePort.echo("test");
        assertEquals(response, "test");

        if (bp != null) {
            Map<String, Object> responseContext = bp.getResponseContext();
            Message m = (Message)responseContext
                .get(JMSConstants.JMS_CLIENT_RESPONSE_JMSMESSAGE);
            checkJMSProperties(m, testcase.getResponseMessage(), false);
        }
    }
}
