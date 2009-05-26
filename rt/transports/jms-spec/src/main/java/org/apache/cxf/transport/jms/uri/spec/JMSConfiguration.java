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

/**
 * 
 */
public class JMSConfiguration {
    
    public static final String QUEUE_PREFIX = "queue:";
    public static final String TOPIC_PREFIX = "topic:";
    public static final String JNDI_PREFIX = "jndi:";

    public static final String DELIVERYMODE_PERSISTENT = "PERSISTENT";
    public static final String DELIVERYMODE_NON_PERSISTENT = "NON_PERSISTENT";
    
    //shared parameters
    public static final String DELIVERYMODE_PARAMETER_NAME = "deliveryMode";
    public static final String TIMETOLIVE_PARAMETER_NAME = "timeToLive";
    public static final String PRIORITY_PARAMETER_NAME = "priority";
    public static final String REPLYTONAME_PARAMETER_NAME = "replyToName";
    
    //jndi parameters
    public static final String JNDICONNECTIONFACTORYNAME_PARAMETER_NAME = "jndiConnectionFactoryName";
    public static final String JNDIINITIALCONTEXTFACTORY_PARAMETER_NAME = "jndiInitialContextFacroty";
    public static final String JNDIURL_PARAMETER_NAME = "jndiURL";
    public static final String JNDI_PREFIX_PARAMETER_NAME = "jndi-";
    
    //queue and topic parameters
    
    public JMSConfiguration() {
    }
    
    public void get() {
        
    }
}
