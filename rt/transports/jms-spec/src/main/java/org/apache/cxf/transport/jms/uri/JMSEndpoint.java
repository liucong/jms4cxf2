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

package org.apache.cxf.transport.jms.uri;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * 
 */
public class JMSEndpoint {
    private String endpointUri;
    private String destinationName;
    private String jmsVariant;
    private Map parameters;

    /**
     * @param uri
     * @param subject
     */
    public JMSEndpoint(String endpointUri, String jmsVariant, String destinationName) {
        this.endpointUri = endpointUri;
        this.jmsVariant = jmsVariant;
        this.destinationName = destinationName;
    }

    /**
     * @param parameters
     */
    public void configureProperties(Map jmsParameters) {
        this.parameters = jmsParameters;
    }

    /**
     * @return
     */
    public Map getParameters() {
        return parameters;
    }

    public String getEnpointUri() {
        return this.endpointUri;
    }

    /**
     * @param key
     * @return
     */
    public String getParameter(String key) {
        if (this.parameters == null) {
            return null;
        }
        return (String)this.parameters.get(key);
    }

    public String getDeliveryMode() {
        String deliveryMode = getParameter(JMSURIConstants.DELIVERYMODE_PARAMETER_NAME);
        if (deliveryMode == null) {
            deliveryMode = JMSURIConstants.DELIVERYMODE_DEFAULT;
        }
        return deliveryMode;
    }

    public long getTimeToLive() {
        String timeToLive = getParameter(JMSURIConstants.TIMETOLIVE_PARAMETER_NAME);
        if (timeToLive == null) {
            return JMSURIConstants.TIMETOLIVE_DEFAULT;
        }
        return Integer.parseInt(timeToLive);
    }

    public int getPriority() {
        String priority = getParameter(JMSURIConstants.PRIORITY_PARAMETER_NAME);
        if (priority == null) {
            return JMSURIConstants.PRIORITY_DEFAULT;
        }
        return Integer.parseInt(priority);
    }

    public String getReplyToName() {
        return getParameter(JMSURIConstants.REPLYTONAME_PARAMETER_NAME);
    }

    /**
     * @return
     */
    public String getDestinationName() {
        // TODO Auto-generated method stub
        return this.destinationName;
    }

    /**
     * @param destinationName The destinationName to set.
     */
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    /**
     * @param jmsVariant The jmsVariant to set.
     */
    public void setJmsVariant(String jmsVariant) {
        this.jmsVariant = jmsVariant;
    }

    /**
     * * @return Returns the jmsVariant.
     */
    public String getJmsVariant() {
        return jmsVariant;
    }

    public String getJndiConnectionFactoryName() {
        return getParameter(JMSURIConstants.JNDICONNECTIONFACTORYNAME_PARAMETER_NAME);
    }

    public String getJndiInitialContextFactory() {
        return getParameter(JMSURIConstants.JNDIINITIALCONTEXTFACTORY_PARAMETER_NAME);
    }

    public String getJndiURL() {
        return getParameter(JMSURIConstants.JNDIURL_PARAMETER_NAME);
    }

    public Map getJndiContextParameters() {
        Map addParas = new HashMap();
        Iterator keyIter = parameters.keySet().iterator();
        while (keyIter.hasNext()) {
            String key = (String)keyIter.next();
            if (key.startsWith(JMSURIConstants.JNDI_PARAMETER_NAME_PREFIX)) {
                addParas.put(key.substring(5), this.parameters.get(key));
            }
        }

        return addParas;
    }

    public String getRequestURI() {
        return "jms:" + jmsVariant + ":" + destinationName;
    }
}
