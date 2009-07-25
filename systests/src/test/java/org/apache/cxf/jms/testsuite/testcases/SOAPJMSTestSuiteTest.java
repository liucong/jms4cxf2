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

import javax.jms.JMSException;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.jms.testsuite.services.Server;
import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.jms_simple.JMSSimplePortType;
import org.apache.cxf.jms_simple.JMSSimpleService0001;
import org.apache.cxf.jms_simple.JMSSimpleService0003;
import org.apache.cxf.systest.jms.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.testsuite.testcase.TestCaseType;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class SOAPJMSTestSuiteTest extends AbstractSOAPJMSTestSuite {

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

    private void oneWayTest(TestCaseType testcase, JMSSimplePortType port) throws Exception {
        InvocationHandler handler = Proxy.getInvocationHandler(port);
        BindingProvider bp = (BindingProvider)handler;

        Map<String, Object> requestContext = bp.getRequestContext();
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);

        port.ping("test");
        checkJMSProperties(testcase, requestHeader);
    }
    
    private void twoWayTest(TestCaseType testcase, final JMSSimplePortType port)
        throws JMSException {
        InvocationHandler handler = Proxy.getInvocationHandler(port);
        BindingProvider bp = (BindingProvider)handler;

        Map<String, Object> requestContext = bp.getRequestContext();
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);

        String response = port.echo("test");
        assertEquals(response, "test");

        Map<String, Object> responseContext = bp.getResponseContext();
        JMSMessageHeadersType responseHeader = (JMSMessageHeadersType)responseContext
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        checkJMSProperties(testcase, requestHeader, responseHeader);
    }
    
    @Test
    public void test0001() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0001");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0001", "SimplePort",
                                                     JMSSimpleService0001.class,
                                                     JMSSimplePortType.class);
        oneWayTest(testcase, simplePort);  
    }
    
    @Test
    public void test0002() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0002");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0001", "SimplePort",
                                                     JMSSimpleService0001.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }
    
    @Test
    public void test0003() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0003");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0003", "SimplePort",
                                                     JMSSimpleService0003.class,
                                                     JMSSimplePortType.class);
        oneWayTest(testcase, simplePort); 
    }
    
    @Test
    public void test0004() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0004");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0003", "SimplePort",
                                                     JMSSimpleService0001.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }
}
