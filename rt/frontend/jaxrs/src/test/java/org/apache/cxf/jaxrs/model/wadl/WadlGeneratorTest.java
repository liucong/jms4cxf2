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
package org.apache.cxf.jaxrs.model.wadl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WadlGeneratorTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true);
    }
    
    @Test
    public void testNoWadl() {
        WadlGenerator wg = new WadlGenerator();
        assertNull(wg.handleRequest(new MessageImpl(), null));
    }
    
    @Test
    public void testSingleRootResource() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0));
        
    }
    
    private void checkResponse(Response r) throws Exception {
        assertNotNull(r);
        assertEquals(WadlGenerator.WADL_TYPE.toString(),
                     r.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE));
//        File f = new File("test.xml");
//        f.delete();
//        f.createNewFile();
//        System.out.println(f.getAbsolutePath());
//        FileOutputStream fos = new FileOutputStream(f);
//        fos.write(r.getEntity().toString().getBytes());
//        fos.flush();
//        fos.close();
    }
    
    @Test
    public void testMultipleRootResources() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        ClassResourceInfo cri1 = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        ClassResourceInfo cri2 = 
            ResourceUtils.createClassResourceInfo(Orders.class, Orders.class, true, true);
        List<ClassResourceInfo> cris = new ArrayList<ClassResourceInfo>();
        cris.add(cri1);
        cris.add(cri2);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, cris);
        Response r = wg.handleRequest(m, null);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        checkGrammars(doc.getDocumentElement());
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 2);
        checkBookStoreInfo(els.get(0));
        Element orderResource = els.get(1);
        assertEquals("/orders", orderResource.getAttribute("path"));
    }

    private void checkGrammars(Element appElement) {
        List<Element> grammarEls = DOMUtils.getChildrenWithName(appElement, WadlGenerator.WADL_NS, 
                                                                "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0), 
                                                          XmlSchemaConstants.XSD_NAMESPACE_URI, "schema");
        assertEquals(1, schemasEls.size());
        assertEquals("http://superbooks", schemasEls.get(0).getAttribute("targetNamespace"));
        assertEquals(3, DOMUtils.getChildrenWithName(schemasEls.get(0), 
                       XmlSchemaConstants.XSD_NAMESPACE_URI, "element").size());
        assertEquals(3, DOMUtils.getChildrenWithName(schemasEls.get(0), 
                       XmlSchemaConstants.XSD_NAMESPACE_URI, "complexType").size());
    }
    
    private void checkBookStoreInfo(Element resource) {
        assertEquals("/bookstore/{id}", resource.getAttribute("path"));
        
        List<Element> resourceEls = DOMUtils.getChildrenWithName(resource, 
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(6, resourceEls.size());        
        assertEquals("/", resourceEls.get(0).getAttribute("path"));
        assertEquals("/book2", resourceEls.get(1).getAttribute("path"));
        assertEquals("/books/{bookid}", resourceEls.get(2).getAttribute("path"));
        assertEquals("/chapter", resourceEls.get(3).getAttribute("path"));
        assertEquals("/booksubresource", resourceEls.get(4).getAttribute("path"));
        assertEquals("/itself", resourceEls.get(5).getAttribute("path"));
        
        
        List<Element> methodEls = DOMUtils.getChildrenWithName(resourceEls.get(0), 
                                                               WadlGenerator.WADL_NS, "method");
        
        assertEquals(1, methodEls.size());
        assertEquals("GET", methodEls.get(0).getAttribute("name"));
        
        List<Element> paramsEls = DOMUtils.getChildrenWithName(resourceEls.get(0), 
                                                               WadlGenerator.WADL_NS, "param");
        assertEquals(1, paramsEls.size());
        checkParameter(paramsEls.get(0), "id", "template");
        
        List<Element> requestEls = DOMUtils.getChildrenWithName(methodEls.get(0), 
                                                               WadlGenerator.WADL_NS, "request");
        assertEquals(1, requestEls.size());
        
        paramsEls = DOMUtils.getChildrenWithName(requestEls.get(0), 
                                                 WadlGenerator.WADL_NS, "param");
        assertEquals(2, paramsEls.size());
        checkParameter(paramsEls.get(0), "a", "query");
        checkParameter(paramsEls.get(1), "b", "query");
        
        paramsEls = DOMUtils.getChildrenWithName(resourceEls.get(2), 
                                                               WadlGenerator.WADL_NS, "param");
        assertEquals(3, paramsEls.size());
        checkParameter(paramsEls.get(0), "id", "template");
        checkParameter(paramsEls.get(1), "bookid", "template");
        checkParameter(paramsEls.get(2), "mid", "matrix");
        
        methodEls = DOMUtils.getChildrenWithName(resourceEls.get(2), 
                                                 WadlGenerator.WADL_NS, "method");
        assertEquals(1, methodEls.size());
        assertEquals("POST", methodEls.get(0).getAttribute("name"));
        
        requestEls = DOMUtils.getChildrenWithName(methodEls.get(0), 
                                                                WadlGenerator.WADL_NS, "request");
        assertEquals(1, requestEls.size());
        List<Element> repEls = DOMUtils.getChildrenWithName(requestEls.get(0), 
                                                            WadlGenerator.WADL_NS, "representation");
        assertEquals(2, repEls.size());
        assertEquals("application/xml", repEls.get(0).getAttribute("mediaType"));
        assertEquals("prefix1:thebook", repEls.get(0).getAttribute("element"));
        assertEquals("application/json", repEls.get(1).getAttribute("mediaType"));
        assertEquals("", repEls.get(1).getAttribute("element"));
        
    }
    
    private void checkParameter(Element paramEl, String name, String type) {
        assertEquals(name, paramEl.getAttribute("name"));
        assertEquals(type, paramEl.getAttribute("style"));    
    }
    
    private List<Element> getWadlResourcesInfo(Document doc, String baseURI, int size) throws Exception {
        Element root = doc.getDocumentElement();
        assertEquals(WadlGenerator.WADL_NS, root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root, 
                                                                  WadlGenerator.WADL_NS, "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl =  resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls = 
            DOMUtils.getChildrenWithName(resourcesEl, 
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(size, resourceEls.size());
        return resourceEls;
    }
    
    
    private Message mockMessage(String baseAddress, String pathInfo, String query,
                                List<ClassResourceInfo> cris) {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        e.put(Service.class, new JAXRSServiceImpl(cris));
        
        m.setExchange(e);
        control.reset();
        ServletDestination d = control.createMock(ServletDestination.class);
        EndpointInfo epr = new EndpointInfo(); 
        epr.setAddress(baseAddress);
        d.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(epr).anyTimes();
        e.setDestination(d);
        m.put(Message.REQUEST_URI, pathInfo);
        m.put(Message.QUERY_STRING, query);
        m.put(Message.HTTP_REQUEST_METHOD, "GET");
        control.replay();
        return m;
    }
    
}
