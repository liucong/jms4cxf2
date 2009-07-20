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

package org.apache.cxf.transport.jms.spec.util;

import javax.jms.Destination;
import javax.jms.MessageListener;

import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * 
 */
public final class JMSTestUtil {

    private JMSTestUtil() {
    }

    public static DefaultMessageListenerContainer createJmsListener(
                                                                    JMSConfiguration jmsConfig,
                                                                    MessageListener listenerHandler,
                                                                    String destinationName,
                                                                    String messageSelectorPrefix,
                                                                    boolean userCID) {
        DefaultMessageListenerContainer jmsListener = new DefaultMessageListenerContainer();
        jmsListener.setConcurrentConsumers(1);
        jmsListener.setMaxConcurrentConsumers(jmsConfig.getMaxConcurrentConsumers());
        jmsListener.setPubSubDomain(jmsConfig.isPubSubDomain());
        jmsListener.setPubSubNoLocal(jmsConfig.isPubSubNoLocal());
        jmsListener.setAutoStartup(true);
        jmsListener.setConnectionFactory(jmsConfig.getOrCreateWrappedConnectionFactory());
        jmsListener.setMessageSelector(jmsConfig.getMessageSelector());
        // jmsListener.setSubscriptionDurable(jmsConfig.isSubscriptionDurable());
        jmsListener.setDurableSubscriptionName(jmsConfig.getDurableSubscriptionName());
        jmsListener.setSessionTransacted(jmsConfig.isSessionTransacted());
        jmsListener.setTransactionManager(jmsConfig.getTransactionManager());
        jmsListener.setMessageListener(listenerHandler);
        if (jmsConfig.getReceiveTimeout() != null) {
            jmsListener.setReceiveTimeout(jmsConfig.getReceiveTimeout());
        }
        if (jmsConfig.getRecoveryInterval() != JMSConfiguration.DEFAULT_VALUE) {
            jmsListener.setRecoveryInterval(jmsConfig.getRecoveryInterval());
        }
        if (jmsConfig.getCacheLevelName() != null
            && (jmsConfig.getCacheLevelName().trim().length() > 0)) {
            jmsListener.setCacheLevelName(jmsConfig.getCacheLevelName());
        } else if (jmsConfig.getCacheLevel() != JMSConfiguration.DEFAULT_VALUE) {
            jmsListener.setCacheLevel(jmsConfig.getCacheLevel());
        }
        if (jmsListener.getCacheLevel() >= DefaultMessageListenerContainer.CACHE_CONSUMER
            && jmsConfig.getMaxSuspendedContinuations() > 0) {
            LOG
                .info("maxSuspendedContinuations value will be ignored - "
                      + ", please set cacheLevel to the value less than "
                      + " org.springframework.jms.listener.DefaultMessageListenerContainer.CACHE_CONSUMER");
        }
        if (jmsConfig.isAcceptMessagesWhileStopping()) {
            jmsListener.setAcceptMessagesWhileStopping(jmsConfig.isAcceptMessagesWhileStopping());
        }
        String staticSelectorPrefix = jmsConfig.getConduitSelectorPrefix();
        if (!userCID && messageSelectorPrefix != null && jmsConfig.isUseConduitIdSelector()) {
            jmsListener.setMessageSelector("JMSCorrelationID LIKE '" + staticSelectorPrefix
                                           + messageSelectorPrefix + "%'");
        } else if (staticSelectorPrefix.length() > 0) {
            jmsListener.setMessageSelector("JMSCorrelationID LIKE '" + staticSelectorPrefix + "%'");
        }
        if (jmsConfig.getDestinationResolver() != null) {
            jmsListener.setDestinationResolver(jmsConfig.getDestinationResolver());
        }
        if (jmsConfig.getTaskExecutor() != null) {
            jmsListener.setTaskExecutor(jmsConfig.getTaskExecutor());
        }

        if (jmsConfig.isAutoResolveDestination()) {
            jmsListener.setDestinationName(destinationName);
        } else {
            JmsTemplate jmsTemplate = createJmsTemplate(jmsConfig, null);
            Destination dest = JMSFactory.resolveOrCreateDestination(jmsTemplate, destinationName,
                                                                     jmsConfig.isPubSubDomain());
            jmsListener.setDestination(dest);
        }
        jmsListener.initialize();
        return jmsListener;
    }

}
