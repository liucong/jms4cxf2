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

package org.apache.cxf.transport.jms.uri.spec;

import org.junit.Assert;
import org.junit.Test;

public class JMSEndpointTest extends Assert {

    @Test
    public void testBasicQueue() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:queue:Foo.Bar");
        assertTrue(endpoint instanceof JMSQueueEndpoint);
    }

    @Test
    public void testQueueParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:queue:Foo.Bar?foo=bar&foo2=bar2");
        assertTrue(endpoint instanceof JMSQueueEndpoint);
        assertEquals(endpoint.getParameters().size(), 2);
        assertEquals(endpoint.getParameter("foo"), "bar");
        assertEquals(endpoint.getParameter("foo2"), "bar2");
    }

    @Test
    public void testBasicTopic() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:topic:Foo.Bar");
        assertTrue(endpoint instanceof JMSTopicEndpoint);
    }

    @Test
    public void testTopicParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:topic:Foo.Bar?foo=bar&foo2=bar2");
        assertTrue(endpoint instanceof JMSTopicEndpoint);
        assertEquals(endpoint.getParameters().size(), 2);
        assertEquals(endpoint.getParameter("foo"), "bar");
        assertEquals(endpoint.getParameter("foo2"), "bar2");
    }

    @Test
    public void testBasicJNDI() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:jndi:Foo.Bar");
        assertTrue(endpoint instanceof JMSJNDIEndpoint);
    }

    @Test
    public void testJNDIParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:jndi:Foo.Bar?"
                                               + "jndiInitialContextFactory"
                                               + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                                               + "&jndiConnectionFactoryName=ConnectionFactory"
                                               + "&jndiURL=tcp://localhost:61616");
        assertTrue(endpoint instanceof JMSJNDIEndpoint);
        assertEquals(endpoint.getParameters().size(), 3);
        assertEquals(endpoint
            .getParameter(JMSConfiguration.JNDIINITIALCONTEXTFACTORY_PARAMETER_NAME),
                     "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        assertEquals(endpoint
            .getParameter(JMSConfiguration.JNDICONNECTIONFACTORYNAME_PARAMETER_NAME),
                     "ConnectionFactory");
        assertEquals(endpoint.getParameter(JMSConfiguration.JNDIURL_PARAMETER_NAME),
                     "tcp://localhost:61616");

    }

    private JMSEndpoint resolveEndpoint(String uri) {
        JMSEndpoint endpoint = null;
        try {
            endpoint = JMSEndpointParser.createEndpoint(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return endpoint;
    }
}
