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

package org.apache.cxf.transport.jms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.JmsUtils;

/**
 * JMSConduit is instantiated by the JMSTransportfactory which is selected by a client if the transport
 * protocol starts with jms:// JMSConduit converts CXF Messages to JMS Messages and sends the request by using
 * a JMS destination. If the Exchange is not oneway it then recevies the response and converts it to a CXF
 * Message. This is then provided in the Exchange and also sent to the incomingObserver
 */
public class JMSConduit extends AbstractConduit implements JMSExchangeSender, MessageListener {

    static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);
    
    private static final String CORRELATED = JMSConduit.class.getName() + ".correlated";
    
    private EndpointInfo endpointInfo;
    private JMSConfiguration jmsConfig;
    private Map<String, Exchange> correlationMap;
    private DefaultMessageListenerContainer jmsListener;
    private DefaultMessageListenerContainer allListener;
    private String conduitId;
    private AtomicLong messageCount;
    private JMSBusLifeCycleListener listener;

    public JMSConduit(EndpointInfo endpointInfo, EndpointReferenceType target, JMSConfiguration jmsConfig) {
        super(target);
        this.jmsConfig = jmsConfig;
        this.endpointInfo = endpointInfo;
        correlationMap = new ConcurrentHashMap<String, Exchange>();
        conduitId = UUID.randomUUID().toString().replaceAll("-", "");
        messageCount = new AtomicLong(0);
    }
    
    /**
     * Prepare the message for send out. The message will be sent after the caller has written the payload to
     * the OutputStream of the message and calls the close method of the stream. In the JMS case the
     * JMSOutputStream will then call back the sendExchange method of this class. {@inheritDoc}
     */
    public void prepare(Message message) throws IOException {
        String name =  endpointInfo.getName().toString() + ".jms-conduit";
        org.apache.cxf.common.i18n.Message msg = 
            new org.apache.cxf.common.i18n.Message("INSUFFICIENT_CONFIGURATION_CONDUIT", LOG, name);
        jmsConfig.ensureProperlyConfigured(msg);
        boolean isTextPayload = JMSConstants.TEXT_MESSAGE_TYPE.equals(jmsConfig.getMessageType());
        JMSOutputStream out = new JMSOutputStream(this, message.getExchange(), isTextPayload);
        message.setContent(OutputStream.class, out);
    }

    /**
     * Send the JMS Request out and if not oneWay receive the response
     * 
     * @param outMessage
     * @param request
     * @return inMessage
     */
    public void sendExchange(final Exchange exchange, final Object request) {
        LOG.log(Level.FINE, "JMSConduit send message");

        final Message outMessage = exchange.getOutMessage();
        if (outMessage == null) {
            throw new RuntimeException("Exchange to be sent has no outMessage");
        }

        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);

        JmsTemplate jmsTemplate = JMSFactory.createJmsTemplate(jmsConfig, headers);
        String userCID = headers != null ? headers.getJMSCorrelationID() : null;
        DefaultMessageListenerContainer jmsList = jmsListener;
        if (!exchange.isOneWay()) {
            if (userCID == null || !jmsConfig.isUseConduitIdSelector()) { 
                if (jmsListener == null) {
                    jmsListener = JMSFactory.createJmsListener(jmsConfig, this, 
                                                               jmsConfig.getReplyDestination(), 
                                                               conduitId, 
                                                               false);
                    addBusListener(exchange.get(Bus.class));
                }
                jmsList = jmsListener;
            } else {
                if (allListener == null) {
                    allListener = JMSFactory.createJmsListener(jmsConfig, 
                                                               this, 
                                                               jmsConfig.getReplyDestination(), 
                                                               null, 
                                                               true);
                    addBusListener(exchange.get(Bus.class));
                }
                jmsList = allListener;
            }
        }
        
        final javax.jms.Destination replyTo = exchange.isOneWay() ? null : jmsList.getDestination();

        final String correlationId = (headers != null && headers.isSetJMSCorrelationID()) 
            ? headers.getJMSCorrelationID() 
            : JMSUtils.createCorrelationId(jmsConfig.getConduitSelectorPrefix() + conduitId, 
                                           messageCount.incrementAndGet());
            
        MessageCreator messageCreator = new MessageCreator() {
            public javax.jms.Message createMessage(Session session) throws JMSException {
                String messageType = jmsConfig.getMessageType();
                final javax.jms.Message jmsMessage;
                jmsMessage = JMSUtils.buildJMSMessageFromCXFMessage(outMessage, request, messageType,
                                                                    session, replyTo,
                                                                    correlationId);
                LOG.log(Level.FINE, "client sending request: ", jmsMessage);
                return jmsMessage;
            }
        };

        /**
         * If the message is not oneWay we will expect to receive a reply on the listener. To receive this
         * reply we add the correlationId and an empty CXF Message to the correlationMap. The listener will
         * fill to Message and notify this thread
         */
        if (!exchange.isOneWay()) {
            synchronized (exchange) {
                correlationMap.put(correlationId, exchange);
                jmsTemplate.send(jmsConfig.getTargetDestination(), messageCreator);
                
                if (exchange.isSynchronous()) {
                    try {
                        exchange.wait(jmsTemplate.getReceiveTimeout());
                    } catch (InterruptedException e) {
                        correlationMap.remove(correlationId);
                        throw new RuntimeException(e);
                    }
                    correlationMap.remove(correlationId);
                    if (exchange.get(CORRELATED) == null) {
                        throw new RuntimeException("Timeout receiving message with correlationId "
                                                   + correlationId);
                    }
                }
            }
        } else {
            jmsTemplate.send(jmsConfig.getTargetDestination(), messageCreator);
        }
    }

    static class JMSBusLifeCycleListener implements BusLifeCycleListener {
        final WeakReference<JMSConduit> ref;
        BusLifeCycleManager blcm;
        JMSBusLifeCycleListener(JMSConduit c, BusLifeCycleManager b) {
            ref = new WeakReference<JMSConduit>(c);
            blcm = b;
            blcm.registerLifeCycleListener(this);
        }
        
        public void initComplete() {
        }

        public void postShutdown() {
        }

        public void preShutdown() {
            unreg();
            blcm = null;
            JMSConduit c = ref.get();
            if (c != null) {
                c.listener = null;
                c.close();
            }
        }
        public void unreg() {
            if (blcm != null) {
                blcm.unregisterLifeCycleListener(this);
            }
        }
    }
    private synchronized void addBusListener(Bus bus) {
        if (listener == null && bus != null) {
            BusLifeCycleManager blcm = bus.getExtension(BusLifeCycleManager.class);
            if (blcm != null) {
                listener = new JMSBusLifeCycleListener(this,
                                                       blcm);
            }
        }
    }

    /**
     * When a message is received on the reply destination the correlation map is searched for the
     * correlationId. If it is found the message is converted to a CXF message and the thread sending the
     * request is notified {@inheritDoc}
     */
    public void onMessage(javax.jms.Message jmsMessage) {
        String correlationId;
        try {
            correlationId = jmsMessage.getJMSCorrelationID();
        } catch (JMSException e) {
            throw JmsUtils.convertJmsAccessException(e);
        }

        Exchange exchange = correlationMap.remove(correlationId);
        if (exchange == null) {
            LOG.log(Level.WARNING, "Could not correlate message with correlationId " + correlationId);
            return;
        }
        Message inMessage = new MessageImpl();
        exchange.setInMessage(inMessage);
        LOG.log(Level.FINE, "client received reply: ", jmsMessage);
        try {
            JMSUtils.populateIncomingContext(jmsMessage, inMessage, JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        
            byte[] response = JMSUtils.retrievePayload(jmsMessage, (String)inMessage.get(Message.ENCODING));
            LOG.log(Level.FINE, "The Response Message payload is : [" + response + "]");
            inMessage.setContent(InputStream.class, new ByteArrayInputStream(response));

            if (exchange.isSynchronous()) {
                synchronized (exchange) {
                    exchange.put(CORRELATED, Boolean.TRUE);
                    exchange.notifyAll();
                }
            }
        
            //REVISIT: put on a workqueue?
            if (incomingObserver != null) {
                incomingObserver.onMessage(exchange.getInMessage());
            }
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.WARNING, "can't get the right encoding information " + ex);
        }
    }

    public void close() {
        if (listener != null) {
            listener.unreg();
            listener = null;
        }
        if (jmsListener != null) {
            jmsListener.shutdown();
        }
        if (allListener != null) {
            allListener.shutdown();
        }
        LOG.log(Level.FINE, "JMSConduit closed ");
    }

    protected Logger getLogger() {
        return LOG;
    }

    public JMSConfiguration getJmsConfig() {
        return jmsConfig;
    }

    public void setJmsConfig(JMSConfiguration jmsConfig) {
        this.jmsConfig = jmsConfig;
    }

    @Override
    protected void finalize() throws Throwable {
        if (listener != null) {
            listener.unreg();
            listener = null;
        }
        if (jmsListener != null) {
            jmsListener.shutdown();
        }
        if (allListener != null) {
            allListener.shutdown();
        }
        super.finalize();
    }

}
