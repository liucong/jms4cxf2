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

package org.apache.cxf.jms.testsuite.util;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.cxf.transport.jms.AbstractJMSTester;
import org.apache.cxf.transport.jms.JMSBrokerSetup;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSFactory;
import org.apache.cxf.transport.jms.JMSOldConfigHolder;
import org.apache.cxf.transport.jms.JNDIConfiguration;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.uri.JMSEndpointParser;
import org.apache.cxf.transport.jms.uri.JMSURIConstants;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

/**
 * 
 */
public final class CopyOfJMSTestUtil {

    private CopyOfJMSTestUtil() {
    }

    public static void createSession(String address) throws Exception {
        Context jndiContext = null;
        ConnectionFactory connectionFactory = null;
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        TextMessage message = null;
        Destination destination = null;

        JMSEndpoint endpoint = JMSEndpointParser.createEndpoint(address);

        Properties environment = new Properties();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, endpoint.getJndiInitialContextFactory());
        environment.put(Context.PROVIDER_URL, endpoint.getJndiURL());
        try {
            jndiContext = new InitialContext(environment);
        } catch (NamingException e) {
            System.out.println("Could not create JNDI API " + "context: " + e.toString());
            System.exit(1);
        }

        /*
         * Look up connection factory and queue. If either does not exist, exit.
         */
        try {
            connectionFactory = (ConnectionFactory)jndiContext.lookup(endpoint
                .getJndiConnectionFactoryName());
        } catch (NamingException e) {
            System.out.println("JNDI API lookup failed: " + e.toString());
            System.exit(1);
        }

        /*
         * Create connection. Create session from connection; false means session is not transacted. Create
         * sender and text message. Send messages, varying text slightly. Finally, close connection.
         */
        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(endpoint.getDestinationName());
            JmsTemplate jmsTemplate = JMSFactory
                .createJmsTemplate(getInitJMSConfiguration(address), null);
            Destination dest = JMSFactory.resolveOrCreateDestination(jmsTemplate, endpoint
                .getDestinationName(), false);
            producer = session.createProducer(dest);
            message = session.createTextMessage();
            message.setText("This is message " + 1);
            producer.send(message);

            MessageConsumer consumer = session.createConsumer(dest);
            Message respMsg = (Message)consumer.receive(300000);
            System.out.println(respMsg);
        } catch (JMSException e) {
            System.out.println("Exception occurred: " + e.toString());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    System.out.println("test");
                }
            }
            System.exit(0);
        }
    }

    public static void main(String[] args) throws Exception {
        AbstractJMSTester.startBroker(new JMSBrokerSetup("tcp://localhost:61500"));

        String destinationName = "dynamicQueues/testqueue";
        String address = "jms:jndi:"
                         + destinationName
                         + "?jndiInitialContextFactory"
                         + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                         + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=tcp://localhost:61500";

        createSession(address);
    }

    private static JMSConfiguration getInitJMSConfiguration(String address) throws Exception {
        JMSEndpoint endpoint = JMSEndpointParser.createEndpoint(address);

        JMSConfiguration jmsConfig = new JMSConfiguration();

        if (endpoint.isSetDeliveryMode()) {
            int deliveryMode = endpoint.getDeliveryMode()
                .equals(JMSURIConstants.DELIVERYMODE_PERSISTENT)
                ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
            jmsConfig.setDeliveryMode(deliveryMode);
        }

        if (endpoint.isSetPriority()) {
            int priority = endpoint.getPriority();
            jmsConfig.setPriority(priority);
        }

        if (endpoint.isSetTimeToLive()) {
            long timeToLive = endpoint.getTimeToLive();
            jmsConfig.setTimeToLive(timeToLive);
        }

        if (jmsConfig.isUsingEndpointInfo()) {
            JndiTemplate jt = new JndiTemplate();
            jt.setEnvironment(JMSOldConfigHolder.getInitialContextEnv(endpoint));
            boolean pubSubDomain = false;
            pubSubDomain = endpoint.getJmsVariant().equals(JMSURIConstants.TOPIC);
            JNDIConfiguration jndiConfig = new JNDIConfiguration();
            jndiConfig.setJndiConnectionFactoryName(endpoint.getJndiConnectionFactoryName());
            jmsConfig.setJndiTemplate(jt);
            jmsConfig.setJndiConfig(jndiConfig);
            jmsConfig.setExplicitQosEnabled(true);
            jmsConfig.setPubSubDomain(pubSubDomain);
            jmsConfig.setPubSubNoLocal(true);
            boolean useJndi = endpoint.getJmsVariant().equals(JMSURIConstants.JNDI);
            if (useJndi) {
                // Setup Destination jndi destination resolver
                final JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
                jndiDestinationResolver.setJndiTemplate(jt);
                jmsConfig.setDestinationResolver(jndiDestinationResolver);
                jmsConfig.setTargetDestination(endpoint.getDestinationName());
                jmsConfig.setReplyDestination(endpoint.getReplyToName());
            } else {
                // Use the default dynamic destination resolver
                jmsConfig.setTargetDestination(endpoint.getDestinationName());
                jmsConfig.setReplyDestination(endpoint.getReplyToName());
            }
        }
        return jmsConfig;
    }

    public static DefaultMessageListenerContainer createJmsListener(
                                                                    String address,
                                                                    MessageListener listenerHandler,
                                                                    String destinationName) {
        JMSConfiguration jmsConfig = null;
        try {
            jmsConfig = getInitJMSConfiguration(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JMSFactory.createJmsListener(jmsConfig, listenerHandler, destinationName, null,
                                            false);
    }
}
