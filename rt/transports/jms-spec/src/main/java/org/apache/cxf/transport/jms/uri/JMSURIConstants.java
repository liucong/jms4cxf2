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

/**
 * 
 */
public class JMSURIConstants {

    public static final String SOAP_JMS_SPECIFICIATION_TRANSPORTID = "http://www.w3.org/2008/07/"
                                                                     + "soap/bindings/JMS/";

    // common constants
    public static final String QUEUE = "queue";
    public static final String TOPIC = "topic";
    public static final String JNDI = "jndi";

    public static final String QUEUE_PREFIX = "queue:";
    public static final String TOPIC_PREFIX = "topic:";
    public static final String JNDI_PREFIX = "jndi:";

    public static final String DELIVERYMODE_PERSISTENT = "PERSISTENT";
    public static final String DELIVERYMODE_NON_PERSISTENT = "NON_PERSISTENT";

    // shared parameters
    public static final String DELIVERYMODE_PARAMETER_NAME = "deliveryMode";
    public static final String TIMETOLIVE_PARAMETER_NAME = "timeToLive";
    public static final String PRIORITY_PARAMETER_NAME = "priority";
    public static final String REPLYTONAME_PARAMETER_NAME = "replyToName";

    // default parameters
    public static final String DELIVERYMODE_DEFAULT = DELIVERYMODE_NON_PERSISTENT;
    public static final int TIMETOLIVE_DEFAULT = -1;
    public static final int PRIORITY_DEFAULT = 5;

    // jndi parameters ? need to be sure.
    public static final String JNDICONNECTIONFACTORYNAME_PARAMETER_NAME = "jndiConnectionFactoryName";
    public static final String JNDIINITIALCONTEXTFACTORY_PARAMETER_NAME = "jndiInitialContextFactory";
    public static final String JNDIURL_PARAMETER_NAME = "jndiURL";
    public static final String JNDI_PARAMETER_NAME_PREFIX = "jndi-";

    // queue and topic parameters
    public static final String TOPICREPLYTONAME_PARAMETER_NAME = "topicReplyToName";

    public JMSURIConstants() {
    }

    public void get() {

    }
}
