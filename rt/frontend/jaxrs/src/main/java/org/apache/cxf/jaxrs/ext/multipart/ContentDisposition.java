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

package org.apache.cxf.jaxrs.ext.multipart;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContentDisposition {

    private List<String> values;
    
    public ContentDisposition(String value) {
        values = Arrays.asList(value.split(";"));
    }
    
    public String getType() {
        return values.get(0).trim();
    }
    
    public String getParameter(String name) {
        for (int i = 1; i < values.size(); i++) {
            String v = values.get(i).trim();
            if (v.startsWith(name)) {
                String[] parts = v.split("=");
                return parts.length == 2 ? parts[1].trim() : ""; 
            }
        }
        return null;
    }
    
    public Map<String, String> getParameters() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 1; i < values.size(); i++) {
            String[] parts = values.get(i).split("=");
            map.put(parts[0].trim(), parts.length == 2 ? parts[1].trim() : ""); 
        }
        return map;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).length() == 0) {
                continue;
            }
            sb.append(values.get(i));
            if (i + 1 < values.size()) {
                sb.append(';');
            }
        }
        return sb.toString();
    }
}
