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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

/**
 * A <a href="http://activemq.apache.org/jms.html">JMS Endpoint</a> for working with a {@link TemporaryQueue}
 * 
 * @version $Revision: 748494 $
 */
// TODO need to be really careful to always use the same Connection otherwise the destination goes stale
public class JMSTemporaryQueueEndpoint extends JMSQueueEndpoint implements DestinationEndpoint {
    private Destination jmsDestination;

    public JMSTemporaryQueueEndpoint(String uri, String destination,
                                     JMSConfiguration configuration) {
        super(uri, destination, configuration);
    }

    public JMSTemporaryQueueEndpoint(String endpointUri, String destination) {
        super(endpointUri, destination);
    }

    public JMSTemporaryQueueEndpoint(TemporaryQueue jmsDestination) throws JMSException {
        super("jms:temp:queue:" + jmsDestination.getQueueName(), null);
        this.jmsDestination = jmsDestination;
        setDestination(jmsDestination);
    }

    /**
     * This endpoint is a singleton so that the temporary destination instances are shared across all
     * producers and consumers of the same endpoint URI
     * 
     * @return true
     */
    public boolean isSingleton() {
        return true;
    }

    public synchronized Destination getJmsDestination(Session session) throws JMSException {
        if (jmsDestination == null) {
            jmsDestination = createJmsDestination(session);
        }
        return jmsDestination;
    }

    protected Destination createJmsDestination(Session session) throws JMSException {
        return session.createTemporaryQueue();
    }
}
