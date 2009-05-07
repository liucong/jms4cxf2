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

package org.apache.cxf.jaxrs.impl;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

public class NewCookieHeaderProvider implements HeaderDelegate<NewCookie> {

    private static final String VERSION = "Version";
    private static final String PATH = "Path";
    private static final String DOMAIN = "Domain";
    private static final String MAX_AGE = "Max-Age";
    private static final String COMMENT = "Comment";
    private static final String SECURE = "Secure";
    
    public NewCookie fromString(String c) {
        
        if (c == null) {
            throw new IllegalArgumentException("SetCookie value can not be null");
        }
        
        String name = null;
        String value = null;
        String path = null;
        String domain = null;
        String comment = null;
        int maxAge = -1;
        boolean isSecure = false;
        
        String[] tokens = c.split(";");
        for (String token : tokens) {
            String theToken = token.trim();
            if (theToken.startsWith(VERSION)) {
                // should we throw an exception if it's not == 1 ?
            } else if (theToken.startsWith(MAX_AGE)) {
                maxAge = Integer.parseInt(theToken.substring(MAX_AGE.length() + 1));
            } else if (theToken.startsWith(PATH)) {
                path = theToken.substring(PATH.length() + 1);
            } else if (theToken.startsWith(DOMAIN)) {
                domain = theToken.substring(DOMAIN.length() + 1);
            } else if (theToken.startsWith(COMMENT)) {
                comment = theToken.substring(COMMENT.length() + 1);
            } else if (theToken.startsWith(SECURE)) {
                isSecure = true;
            } else {
                int i = theToken.indexOf('=');
                if (i != -1) {
                    name = theToken.substring(0, i);
                    value = i == theToken.length()  + 1 ? "" : theToken.substring(i + 1);
                }
            }
        }
        
        if (name == null || value == null) {
            throw new IllegalArgumentException("Set-Cookie is malformed : " + c);
        }
        
        return new NewCookie(name, value, path, domain, comment, maxAge, isSecure);
    }

    public String toString(NewCookie value) {
        StringBuilder sb = new StringBuilder();
        sb.append(value.getName()).append('=').append(value.getValue());
        if (value.getComment() != null) {
            sb.append(';').append(COMMENT).append('=').append(value.getComment());
        }
        if (value.getDomain() != null) {
            sb.append(';').append(DOMAIN).append('=').append(value.getDomain());
        }
        if (value.getMaxAge() != -1) {
            sb.append(';').append(MAX_AGE).append('=').append(value.getMaxAge());
        }
        if (value.getPath() != null) {
            sb.append(';').append(PATH).append('=').append(value.getPath());
        }
        if (value.isSecure()) {
            sb.append(';').append(SECURE);
        }
        sb.append(';').append(VERSION).append('=').append(value.getVersion());
        return sb.toString();
    }

}
