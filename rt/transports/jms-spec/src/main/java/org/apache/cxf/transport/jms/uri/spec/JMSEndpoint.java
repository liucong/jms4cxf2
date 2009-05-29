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

import java.util.Map;

/**
 * 
 */
public class JMSEndpoint {
    private String endpointUri;
    private JMSConfiguration jmsConfiguration;
    private Map parameters;

    public JMSEndpoint(String endpointUri) {
        this.endpointUri = endpointUri;
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
    public JMSConfiguration getConfiguration() {
        // TODO Auto-generated method stub
        return jmsConfiguration;
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
     * @param string
     * @return
     */
    public String getParameter(String key) {
        if (this.parameters == null) {
            return null;
        }
        return (String)this.parameters.get(key);
    }
}
