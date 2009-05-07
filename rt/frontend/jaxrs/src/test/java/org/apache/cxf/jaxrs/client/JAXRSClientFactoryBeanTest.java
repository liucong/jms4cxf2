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

package org.apache.cxf.jaxrs.client;

import org.apache.cxf.jaxrs.resources.BookStore;
import org.apache.cxf.jaxrs.resources.BookStoreSubresourcesOnly;

import org.junit.Assert;
import org.junit.Test;

public class JAXRSClientFactoryBeanTest extends Assert {

    @Test
    public void testCreateClient() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStore.class);
        assertTrue(bean.create() instanceof BookStore);
    }
    
    @Test
    public void testTemplateInRootPathInherit() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStoreSubresourcesOnly.class);
        BookStoreSubresourcesOnly store = bean.create(BookStoreSubresourcesOnly.class, 1, 2, 3);
        BookStoreSubresourcesOnly store2 = store.getItself();
        assertEquals("http://bar/bookstore/1/2/3/sub1", 
                     WebClient.client(store2).getCurrentURI().toString());
    }
    
    @Test
    public void testTemplateInRootReplace() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStoreSubresourcesOnly.class);
        BookStoreSubresourcesOnly store = bean.create(BookStoreSubresourcesOnly.class, 1, 2, 3);
        BookStoreSubresourcesOnly store2 = store.getItself2("11", "33");
        assertEquals("http://bar/bookstore/11/2/33/sub2", 
                     WebClient.client(store2).getCurrentURI().toString());
    }
    
    @Test
    public void testTemplateInRootAppend() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStoreSubresourcesOnly.class);
        BookStoreSubresourcesOnly store = bean.create(BookStoreSubresourcesOnly.class, 1, 2, 3);
        BookStoreSubresourcesOnly store2 = store.getItself3("id4");
        assertEquals("http://bar/bookstore/1/2/3/id4/sub3", 
                     WebClient.client(store2).getCurrentURI().toString());
    }
    
}
