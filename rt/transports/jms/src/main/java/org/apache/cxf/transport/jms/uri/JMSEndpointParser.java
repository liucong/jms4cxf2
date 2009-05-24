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

import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
public class JMSEndpointParser {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSEndpointParser.class);

    public JMSEndpointParser() {
    }

    public JMSEndpoint createEndpoint(String uri) throws Exception {
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

        LOG.log(Level.FINE, "Creating endpoint uri=[" + uri + "], path=[" + path + "], parameters=["
                            + parameters + "]");
        JMSEndpoint endpoint = createEndpoint(uri, path, parameters);
        if (endpoint == null) {
            return null;
        }

        if (parameters != null) {
            endpoint.configureProperties(parameters);
            /*
             * if (useIntrospectionOnEndpoint()) { setProperties(endpoint, parameters); } // if endpoint is
             * strict (not lenient) and we have unknown parameters configured then // fail if there are
             * parameters that could not be set, then they are probably miss spelt or not // supported at all
             * if (!endpoint.isLenientProperties()) { validateParameters(uri, parameters, null); }
             */
        }

        return endpoint;
    }

    /**
     * Strategy for validation of parameters, that was not able to be resolved to any endpoint options.
     * 
     * @param uri the uri - the uri the end user provided untouched
     * @param parameters the parameters, an empty map if no parameters given
     * @param optionPrefix optional prefix to filter the parameters for validation. Use <tt>null</tt> for
     *            validate all.
     * @throws ResolveEndpointFailedException should be thrown if the URI validation failed
     */
    /*
     * protected void validateParameters(String uri, Map parameters, String optionPrefix) { Map param =
     * parameters; if (optionPrefix != null) { param = IntrospectionSupport.extractProperties(parameters,
     * optionPrefix); } if (param.size() > 0) { throw new ResolveEndpointFailedException( uri, "There are " +
     * param.size() + " parameters that couldn't be set on the endpoint." +
     * " Check the uri if the parameters are spelt correctly and that they are properties of the endpoint." +
     * " Unknown parameters=[" + param + "]"); } }
     */

    /**
     * Strategy for validation of the uri when creating the endpoint.
     * 
     * @param uri the uri - the uri the end user provided untouched
     * @param path the path - part after the scheme
     * @param parameters the parameters, an empty map if no parameters given
     * @throws ResolveEndpointFailedException should be thrown if the URI validation failed
     */
    protected void validateURI(String uri, String path, Map parameters) 
        throws ResolveEndpointFailedException {
        // check for uri containing & but no ? marker
        if (uri.contains("&") && !uri.contains("?")) {
            throw new ResolveEndpointFailedException(uri, "Invalid uri syntax: no ? marker however the uri "
                                                          + "has & parameter separators. "
                                                          + "Check the uri if its missing a ? marker.");

        }

        // check for uri containing double && markers
        if (uri.contains("&&")) {
            throw new ResolveEndpointFailedException(uri,
                                                     "Invalid uri syntax: Double && marker found. "
                                                     + "Check the uri and remove the duplicate & marker.");
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
    protected JMSEndpoint createEndpoint(String uri, String remaining, Map parameters) throws Exception {
        boolean pubSubDomain = false;
        boolean tempDestination = false;
        if (remaining.startsWith(JMSConfiguration.QUEUE_PREFIX)) {
            pubSubDomain = false;
            remaining = removeStartingCharacters(remaining.substring(JMSConfiguration.QUEUE_PREFIX.length()),
                                                 '/');
        } else if (remaining.startsWith(JMSConfiguration.TOPIC_PREFIX)) {
            pubSubDomain = true;
            remaining = removeStartingCharacters(remaining.substring(JMSConfiguration.TOPIC_PREFIX.length()),
                                                 '/');
        } else if (remaining.startsWith(JMSConfiguration.TEMP_QUEUE_PREFIX)) {
            pubSubDomain = false;
            tempDestination = true;
            remaining = removeStartingCharacters(remaining.substring(JMSConfiguration.TEMP_QUEUE_PREFIX
                .length()), '/');
        } else if (remaining.startsWith(JMSConfiguration.TEMP_TOPIC_PREFIX)) {
            pubSubDomain = true;
            tempDestination = true;
            remaining = removeStartingCharacters(remaining.substring(JMSConfiguration.TEMP_TOPIC_PREFIX
                .length()), '/');
        }

        final String subject = convertPathToActualDestination(remaining, parameters);

        // lets make sure we copy the configuration as each endpoint can
        // customize its own version
        JMSConfiguration newConfiguration = getConfiguration().copy();
        JMSEndpoint endpoint;
        if (pubSubDomain) {
            if (tempDestination) {
                endpoint = new JmsTemporaryTopicEndpoint(uri, this, subject, newConfiguration);
            } else {
                endpoint = new JMSEndpoint(uri, this, subject, pubSubDomain, newConfiguration);
            }
        } else {
            QueueBrowseStrategy strategy = getQueueBrowseStrategy();
            if (tempDestination) {
                endpoint = new JmsTemporaryQueueEndpoint(uri, this, subject, newConfiguration, strategy);
            } else {
                endpoint = new JmsQueueEndpoint(uri, this, subject, newConfiguration, strategy);
            }
        }

        String selector = getAndRemoveParameter(parameters, "selector", String.class);
        if (selector != null) {
            endpoint.setSelector(selector);
        }
        String username = getAndRemoveParameter(parameters, "username", String.class);
        String password = getAndRemoveParameter(parameters, "password", String.class);
        if (username != null && password != null) {
            ConnectionFactory cf = endpoint.getConfiguration().getConnectionFactory();
            UserCredentialsConnectionFactoryAdapter ucfa = new UserCredentialsConnectionFactoryAdapter();
            ucfa.setTargetConnectionFactory(cf);
            ucfa.setPassword(password);
            ucfa.setUsername(username);
            endpoint.getConfiguration().setConnectionFactory(ucfa);
        } else {
            if (username != null || password != null) {
                // exclude the the saturation of username and password are all empty
                throw new IllegalArgumentException("The JmsComponent's username or password is null");
            }
        }
        setProperties(endpoint.getConfiguration(), parameters);

        endpoint.setHeaderFilterStrategy(getHeaderFilterStrategy());

        return endpoint;
        return null;
    }

    /**
     * Is the given parameter a reference parameter (starting with a # char)
     */
    protected boolean isReferenceParameter(String parameter) {
        return parameter != null && parameter.startsWith("#");
    }

    /*    *//**
     * Derived classes may wish to overload this to prevent the default introspection of URI parameters
     * on the created Endpoint instance
     */
    /*
     * protected boolean useIntrospectionOnEndpoint() { return true; }
     */

    // Some helper methods
    // -------------------------------------------------------------------------
    /**
     * Returns the reminder of the text if it starts with the prefix.
     * <p/>
     * Is useable for string parameters that contains commands.
     * 
     * @param prefix the prefix
     * @param text the text
     * @return the reminder, or null if no reminder
     */
    protected String ifStartsWithReturnRemainder(String prefix, String text) {
        if (text.startsWith(prefix)) {
            String remainder = text.substring(prefix.length());
            if (remainder.length() > 0) {
                return remainder;
            }
        }
        return null;
    }

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
