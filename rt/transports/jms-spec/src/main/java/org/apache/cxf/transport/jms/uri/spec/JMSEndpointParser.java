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

import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
public final class JMSEndpointParser {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSEndpointParser.class);

    private JMSEndpointParser() {
    }

    public static JMSEndpoint createEndpoint(String uri) throws Exception {
        // encode URI string to the unsafe URI characters
        URI u = new URI(UnsafeUriCharactersEncoder.encode(uri));
        String path = u.getSchemeSpecificPart();

        // lets trim off any query arguments
        if (path.startsWith("//")) {
            path = path.substring(2);
        }
        int idx = path.indexOf('?');
        if (idx > 0) {
            path = path.substring(0, idx);
        }
        Map parameters = URISupport.parseParameters(u);

        validateURI(uri, path, parameters);

        LOG.log(Level.FINE, "Creating endpoint uri=[" + uri + "], path=[" + path
                            + "], parameters=[" + parameters + "]");
        JMSEndpoint endpoint = createEndpoint(uri, path, parameters);
        if (endpoint == null) {
            return null;
        }

        if (parameters != null) {
            endpoint.configureProperties(parameters);
        }

        return endpoint;
    }

    /**
     * Strategy for validation of the uri when creating the endpoint.
     * 
     * @param uri the uri - the uri the end user provided untouched
     * @param path the path - part after the scheme
     * @param parameters the parameters, an empty map if no parameters given
     * @throws ResolveEndpointFailedException should be thrown if the URI validation failed
     */
    protected static void validateURI(String uri, String path, Map parameters)
        throws ResolveEndpointFailedException {
        // check for uri containing & but no ? marker
        if (uri.contains("&") && !uri.contains("?")) {
            throw new ResolveEndpointFailedException(
                                                     uri,
                                                     "Invalid uri syntax: no ? marker however the uri "
                                                         + "has & parameter separators. "
                                                         + "Check the uri if its missing a ? marker.");

        }

        // check for uri containing double && markers
        if (uri.contains("&&")) {
            throw new ResolveEndpointFailedException(uri,
                                                     "Invalid uri syntax: Double && marker found. "
                                                         + "Check the uri and remove the "
                                                         + "duplicate & marker.");
        }
    }

    /**
     * A factory method allowing derived components to create a new endpoint from the given URI, remaining
     * path and optional parameters
     * 
     * @param uri the full URI of the endpoint
     * @param remaining the remaining part of the URI without the query parameters or component prefix
     * @param parameters the optional parameters passed in
     * @return a newly created endpoint or null if the endpoint cannot be created based on the inputs
     */
    protected static JMSEndpoint createEndpoint(String uri, String remaining, Map parameters)
        throws Exception {
        boolean isQueue = false;
        boolean isTopic = false;
        boolean isJndi = false;
        if (remaining.startsWith(JMSSpecConstants.QUEUE_PREFIX)) {
            remaining = removeStartingCharacters(remaining.substring(JMSSpecConstants.QUEUE_PREFIX
                .length()), '/');
            isQueue = true;
        } else if (remaining.startsWith(JMSSpecConstants.TOPIC_PREFIX)) {
            remaining = removeStartingCharacters(remaining.substring(JMSSpecConstants.TOPIC_PREFIX
                .length()), '/');
            isTopic = true;
        } else if (remaining.startsWith(JMSSpecConstants.JNDI_PREFIX)) {
            remaining = removeStartingCharacters(remaining.substring(JMSSpecConstants.JNDI_PREFIX
                .length()), '/');
            isJndi = true;
        }

        final String subject = convertPathToActualDestination(remaining, parameters);

        // lets make sure we copy the configuration as each endpoint can
        // customize its own version
        // JMSConfiguration newConfiguration = getConfiguration().copy();
        JMSEndpoint endpoint = null;
        if (isQueue) {
            endpoint = new JMSQueueEndpoint(uri, subject);
        } else if (isTopic) {
            endpoint = new JMSTopicEndpoint(uri, subject);
        } else if (isJndi) {
            endpoint = new JMSJNDIEndpoint(uri, subject);
        }
        return endpoint;
    }

    /**
     * A strategy method allowing the URI destination to be translated into the actual JMS destination name
     * (say by looking up in JNDI or something)
     */
    protected static String convertPathToActualDestination(String path, Map parameters) {
        return path;
    }

    public static JMSSpecConstants getConfiguration() {
        return null;
    }

    // Some helper methods
    // -------------------------------------------------------------------------
    /**
     * Removes any starting characters on the given text which match the given character
     * 
     * @param text the string
     * @param ch the initial characters to remove
     * @return either the original string or the new substring
     */
    public static String removeStartingCharacters(String text, char ch) {
        int idx = 0;
        while (text.charAt(idx) == ch) {
            idx++;
        }
        if (idx > 0) {
            return text.substring(idx);
        }
        return text;
    }
}
