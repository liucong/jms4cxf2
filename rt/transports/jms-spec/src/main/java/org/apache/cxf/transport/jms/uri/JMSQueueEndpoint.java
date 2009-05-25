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

import javax.jms.JMSException;
import javax.jms.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An endpoint for a JMS Queue which is also browsable
 *
 * @version $Revision: 749939 $
 */
public class JMSQueueEndpoint extends JMSEndpoint {
    private static final transient Log LOG = LogFactory.getLog(JMSQueueEndpoint.class);

    public JMSQueueEndpoint(Queue destination) throws JMSException {
        this("jms:queue:" + destination.getQueueName(), null);
        setDestination(destination);
    }

    public JMSQueueEndpoint(String uri, String destination,
            JMSConfiguration configuration) {
        super(uri, destination, false, configuration);
    }

    public JMSQueueEndpoint(String endpointUri, String destination) {
        super(endpointUri, destination, false);
    }
}
