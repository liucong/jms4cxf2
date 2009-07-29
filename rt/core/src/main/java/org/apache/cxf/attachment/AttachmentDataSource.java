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

package org.apache.cxf.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;

public class AttachmentDataSource implements DataSource {

    private final String ct;    
    private CachedOutputStream cache;
    private InputStream ins;
    private DelegatingInputStream delegating;
    
    public AttachmentDataSource(String ctParam, InputStream inParam) throws IOException {
        this.ct = ctParam;        
        ins = inParam;
        if (ins instanceof DelegatingInputStream) {
            delegating = (DelegatingInputStream)ins;
        }
    }
    public AttachmentDataSource(String ctParam, 
                                InputStream inParam,
                                InputStream delegate) throws IOException {
        this.ct = ctParam;        
        ins = inParam;
        if (delegate instanceof DelegatingInputStream) {
            delegating = (DelegatingInputStream)delegate;
        }
    }

    public boolean isCached() {
        return cache != null;
    }
    public void cache() throws IOException {
        if (cache == null) {
            cache = new CachedOutputStream();
            IOUtils.copy(ins, cache);
            cache.lockOutputStream();  
            ins.close();
            ins = null;
            if (delegating != null) {
                delegating.setInputStream(cache.getInputStream());
            }
        }
    }
    public void hold() throws IOException {
        cache();
        cache.holdTempFile();
    }
    public void release() {
        cache.releaseTempFileHold();
    }
    
    public String getContentType() {
        return ct;
    }
    public DelegatingInputStream getDelegatingInputStream() {
        return delegating;
    }
    public InputStream getInputStream() {
        try {
            if (cache != null) {
                return cache.getInputStream();
            }
            return ins;
        } catch (IOException e) {
            return null;
        }
    }

    public String getName() {
        return null;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }
}