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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class HttpHeadersImpl implements HttpHeaders {

    private MultivaluedMap<String, String> headers;
    
    @SuppressWarnings("unchecked")
    public HttpHeadersImpl(Message message) {
        this.headers = new MetadataMap<String, String>(
            (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS), true, true);
    }
    
    public List<MediaType> getAcceptableMediaTypes() {
        List<String> lValues = headers.get(HttpHeaders.ACCEPT);
        if (lValues == null || lValues.isEmpty()) {
            return Collections.emptyList();
        }
        List<MediaType> mediaTypes = JAXRSUtils.parseMediaTypes(lValues.get(0));
        sortMediaTypesUsingQualityFactor(mediaTypes); 
        return mediaTypes;
    }

    public Map<String, Cookie> getCookies() {
        List<String> cs = getListValues(HttpHeaders.COOKIE);
        Map<String, Cookie> cl = new HashMap<String, Cookie>(); 
        for (String c : cs) {
            Cookie cookie = Cookie.valueOf(c);
            cl.put(cookie.getName(), cookie);
        }
        return cl;
    }

    public Locale getLanguage() {
        List<String> values = getListValues(HttpHeaders.CONTENT_LANGUAGE);
        return values.size() == 0 ? null : createLocale(values.get(0).trim());
    }

    public MediaType getMediaType() {
        List<String> values = getListValues(HttpHeaders.CONTENT_TYPE);
        return values.size() == 0 ? null : MediaType.valueOf(values.get(0));
    }

    public MultivaluedMap<String, String> getRequestHeaders() {
        Map<String, List<String>> newHeaders = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            newHeaders.put(entry.getKey(), getListValues(entry.getKey()));
        }
        return new MetadataMap<String, String>(newHeaders, true, true);
    }

    public List<Locale> getAcceptableLanguages() {
        List<String> ls = getListValues(HttpHeaders.ACCEPT_LANGUAGE);
        
        List<Locale> newLs = new ArrayList<Locale>(); 
        Map<Locale, Float> prefs = new HashMap<Locale, Float>();
        for (String l : ls) {
            String[] pair = l.split(";");
            
            Locale locale = createLocale(pair[0].trim());
            
            newLs.add(locale);
            if (pair.length > 1) {
                String[] pair2 = pair[1].split("=");
                if (pair2.length > 1) {
                    prefs.put(locale, JAXRSUtils.getMediaTypeQualityFactor(pair2[1].trim()));
                } else {
                    prefs.put(locale, 1F);
                }
            } else {
                prefs.put(locale, 1F);
            }
        }
        if (newLs.size() <= 1) {
            return newLs;
        }
        
        Collections.sort(newLs, new AcceptLanguageComparator(prefs));
        return newLs;
        
    }

    public List<String> getRequestHeader(String name) {
        return getListValues(name);
    }

    private List<String> getListValues(String headerName) {
        List<String> values = headers.get(headerName);
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        if (HttpUtils.isDateRelatedHeader(headerName)) {
            return values;
        }
        String[] ls =  values.get(0).split(",");
        if (ls.length == 1) {
            return Collections.singletonList(ls[0].trim());
        } else {
            List<String> newValues = new ArrayList<String>();
            for (String v : ls) {
                newValues.add(v.trim());
            }
            return newValues;
        }
    }
    
    private static class AcceptLanguageComparator implements Comparator<Locale> {
        private Map<Locale, Float> prefs;
        
        public AcceptLanguageComparator(Map<Locale, Float> prefs) {
            this.prefs = prefs;
        }

        public int compare(Locale lang1, Locale lang2) {
            float p1 = prefs.get(lang1);
            float p2 = prefs.get(lang2);
            return Float.compare(p1, p2) * -1;
        }
    }
    
    private Locale createLocale(String value) {
        String[] values = value.split("-");
        if (values.length == 0 || values.length > 2) {
            throw new IllegalArgumentException("Illegal locale value : " + value);
        }
        if (values.length == 1) {
            return new Locale(values[0]);
        } else {
            return new Locale(values[0], values[1]);
        }
        
    }
    
    private void sortMediaTypesUsingQualityFactor(List<MediaType> types) {
        if (types.size() > 1) {
            Collections.sort(types, new Comparator<MediaType>() {

                public int compare(MediaType mt1, MediaType mt2) {
                    return JAXRSUtils.compareMediaTypesQualityFactors(mt1, mt2);
                }
                
            });
        }
    }
    
}
