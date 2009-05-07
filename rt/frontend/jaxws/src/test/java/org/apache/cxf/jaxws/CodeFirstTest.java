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
package org.apache.cxf.jaxws;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.service.ArrayService;
import org.apache.cxf.jaxws.service.ArrayServiceImpl;
import org.apache.cxf.jaxws.service.FooServiceImpl;
import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.jaxws.service.HelloInterface;
import org.apache.cxf.jaxws.service.SayHi;
import org.apache.cxf.jaxws.service.SayHiImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.junit.Test;


public class CodeFirstTest extends AbstractJaxWsTest {
    String address = "local://localhost:9000/Hello";
    
    @Test
    public void testDocLitModel() throws Exception {
        Definition d = createService(false);

        Document wsdl = WSDLFactory.newInstance().newWSDLWriter().getDocument(d);
        addNamespace("svc", "http://service.jaxws.cxf.apache.org/");
        
        assertValid("/wsdl:definitions/wsdl:service[@name='HelloService']", wsdl);
        assertValid("//wsdl:port/wsdlsoap:address[@location='" + address + "']", wsdl);
        assertValid("//wsdl:portType[@name='Hello']", wsdl);

        assertValid("/wsdl:definitions/wsdl:types/xsd:schema" 
                    + "[@targetNamespace='http://service.jaxws.cxf.apache.org/']" 
                    + "/xsd:import[@namespace='http://jaxb.dev.java.net/array']", wsdl);
        
        assertValid("/wsdl:definitions/wsdl:types/xsd:schema" 
                    + "[@targetNamespace='http://service.jaxws.cxf.apache.org/']" 
                    + "/xsd:element[@type='ns0:stringArray']", wsdl);
        
        assertValid("/wsdl:definitions/wsdl:message[@name='sayHi']"
                    + "/wsdl:part[@element='tns:sayHi'][@name='sayHi']",
                    wsdl);

        assertValid("/wsdl:definitions/wsdl:message[@name='getGreetingsResponse']"
                    + "/wsdl:part[@element='tns:getGreetingsResponse'][@name='getGreetingsResponse']",
                    wsdl);    

        assertValid("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='getGreetings']"
                    + "/wsdlsoap:operation[@soapAction='myaction']",
                    wsdl);
        
        
    }

    @Test
    public void testWrappedModel() throws Exception {
        Definition d = createService(true);
        
        Document wsdl = WSDLFactory.newInstance().newWSDLWriter().getDocument(d);
        
        addNamespace("svc", "http://service.jaxws.cxf.apache.org");
        
        assertValid("/wsdl:definitions/wsdl:service[@name='HelloService']", wsdl);
        assertValid("//wsdl:port/wsdlsoap:address[@location='" + address + "']", wsdl);
        assertValid("//wsdl:portType[@name='Hello']", wsdl);
        assertValid("/wsdl:definitions/wsdl:message[@name='sayHi']"
                    + "/wsdl:part[@element='tns:sayHi'][@name='parameters']",
                    wsdl);
        assertValid("/wsdl:definitions/wsdl:message[@name='sayHiResponse']"
                    + "/wsdl:part[@element='tns:sayHiResponse'][@name='parameters']",
                    wsdl);
        assertValid("//xsd:complexType[@name='sayHi']"
                    + "/xsd:sequence/xsd:element[@name='arg0']",
                    wsdl);
    }
    
    private Definition createService(boolean wrapped) throws Exception {
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();

        Bus bus = getBus();
        bean.setBus(bus);
        bean.setServiceClass(Hello.class);
        bean.setWrapped(wrapped);
        
        Service service = bean.create();

        InterfaceInfo i = service.getServiceInfos().get(0).getInterface();
        assertEquals(4, i.getOperations().size());

        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setBus(bus);
        svrFactory.setServiceFactory(bean);
        svrFactory.setAddress(address);
        svrFactory.create();
        
        Collection<BindingInfo> bindings = service.getServiceInfos().get(0).getBindings();
        assertEquals(1, bindings.size());
        
        ServiceWSDLBuilder wsdlBuilder = 
            new ServiceWSDLBuilder(bus, service.getServiceInfos().get(0));
        return wsdlBuilder.build();
    }

    
    @Test
    public void testEndpoint() throws Exception {
        Hello service = new Hello();

        EndpointImpl ep = new EndpointImpl(getBus(), service, (String) null);
        ep.publish("local://localhost:9090/hello");

        Node res = invoke("local://localhost:9090/hello", 
                          LocalTransportFactory.TRANSPORT_ID,
                          "sayHi.xml");
        
        assertNotNull(res);
       
        addNamespace("h", "http://service.jaxws.cxf.apache.org/");
        assertValid("//s:Body/h:sayHiResponse/return", res);
        
        res = invoke("local://localhost:9090/hello", 
                     LocalTransportFactory.TRANSPORT_ID,
                     "getGreetings.xml");

        assertNotNull(res);
        
        addNamespace("h", "http://service.jaxws.cxf.apache.org/");
        assertValid("//s:Body/h:getGreetingsResponse/return[1]", res);
        assertValid("//s:Body/h:getGreetingsResponse/return[2]", res);
    }
    
    @Test
    public void testClient() throws Exception {
        Hello serviceImpl = new Hello();
        EndpointImpl ep = new EndpointImpl(getBus(), serviceImpl, (String) null);
        ep.publish("local://localhost:9090/hello");
        ep.getServer().getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
        ep.getServer().getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
        QName serviceName = new QName("http://service.jaxws.cxf.apache.org/", "HelloService");
        QName portName = new QName("http://service.jaxws.cxf.apache.org/", "HelloPort");
        
        // need to set the same bus with service , so use the ServiceImpl
        ServiceImpl service = new ServiceImpl(getBus(), (URL)null, serviceName, null);
        service.addPort(portName, "http://schemas.xmlsoap.org/soap/", "local://localhost:9090/hello"); 
        
        HelloInterface proxy = service.getPort(portName, HelloInterface.class);
        assertEquals("Get the wrong result", "hello", proxy.sayHi("hello"));
        String[] strInput = new String[2];
        strInput[0] = "Hello";
        strInput[1] = "Bonjour";
        String[] strings = proxy.getStringArray(strInput);
        assertEquals(strings.length, 2);
        assertEquals(strings[0], "HelloHello");
        assertEquals(strings[1], "BonjourBonjour");
        List<String> listInput = new ArrayList<String>();
        listInput.add("Hello");
        listInput.add("Bonjour");
        List<String> list = proxy.getStringList(listInput);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), "HelloHello");
        assertEquals(list.get(1), "BonjourBonjour");
        //now the client side can't unmarshal the complex type without binding types annoutation 
        List<String> result = proxy.getGreetings();
        assertEquals(2, result.size());
    }
    
    
    @Test
    public void testRpcClient() throws Exception {
        SayHiImpl serviceImpl = new SayHiImpl();
        EndpointImpl ep = new EndpointImpl(getBus(), serviceImpl, (String) null);
        ep.publish("http://localhost:9090/hello");
        
        QName serviceName = new QName("http://mynamespace.com/", "SayHiService");
        QName portName = new QName("http://mynamespace.com/", "HelloPort");
        
        // need to set the same bus with service , so use the ServiceImpl
        ServiceImpl service = new ServiceImpl(getBus(), (URL)null, serviceName, null);
        service.addPort(portName, "http://schemas.xmlsoap.org/soap/", "http://localhost:9090/hello"); 
        
        SayHi proxy = service.getPort(portName, SayHi.class);
        long res = proxy.sayHi(3);
        assertEquals(3, res);
        String[] strInput = new String[2];
        strInput[0] = "Hello";
        strInput[1] = "Bonjour";
        String[] strings = proxy.getStringArray(strInput);
        assertEquals(strings.length, 2);
        assertEquals(strings[0], "HelloHello");
        assertEquals(strings[1], "BonjourBonjour");
        
    }
    
    @Test
    public void testArrayAndList() throws Exception {
        ArrayServiceImpl serviceImpl = new ArrayServiceImpl();
        EndpointImpl ep = new EndpointImpl(getBus(), serviceImpl, (String) null);
        ep.publish("local://localhost:9090/array");
        ep.getServer().getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
        ep.getServer().getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
        QName serviceName = new QName("http://service.jaxws.cxf.apache.org/", "ArrayService");
        QName portName = new QName("http://service.jaxws.cxf.apache.org/", "ArrayPort");
        
        // need to set the same bus with service , so use the ServiceImpl
        ServiceImpl service = new ServiceImpl(getBus(), (URL)null, serviceName, null);
        service.addPort(portName, "http://schemas.xmlsoap.org/soap/", "local://localhost:9090/array"); 
        
        ArrayService proxy = service.getPort(portName, ArrayService.class);
        String[] arrayOut = proxy.arrayOutput();
        assertEquals(arrayOut.length, 3);
        assertEquals(arrayOut[0], "string1");
        assertEquals(arrayOut[1], "string2");
        assertEquals(arrayOut[2], "string3");
        String[] arrayIn = new String[3];
        arrayIn[0] = "string1";
        arrayIn[1] = "string2";
        arrayIn[2] = "string3";
        assertEquals(proxy.arrayInput(arrayIn), "string1string2string3");
        arrayOut = proxy.arrayInputAndOutput(arrayIn);
        assertEquals(arrayOut.length, 3);
        assertEquals(arrayOut[0], "string11");
        assertEquals(arrayOut[1], "string22");
        assertEquals(arrayOut[2], "string33");
        
        List<String> listOut = proxy.listOutput();
        assertEquals(listOut.size(), 3);
        assertEquals(listOut.get(0), "string1");
        assertEquals(listOut.get(1), "string2");
        assertEquals(listOut.get(2), "string3");
        List<String> listIn = new ArrayList<String>();
        listIn.add("list1");
        listIn.add("list2");
        listIn.add("list3");
        assertEquals(proxy.listInput(listIn), "list1list2list3");
    }

    @Test
    public void testNamespacedWebParamsBare() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setAddress("http://localhost/test");
        sf.setServiceClass(FooServiceImpl.class);
        
        Server server = sf.create();
        
        Document doc = getWSDLDocument(server);
        
        assertValid("//xsd:schema[@targetNamespace='http://namespace3']", doc);
        assertValid("//xsd:schema[@targetNamespace='http://namespace5']", doc);
        
        assertValid("//xsd:element[@name='FooEcho2HeaderRequest'][1]", doc);
        assertInvalid("//xsd:element[@name='FooEcho2HeaderRequest'][2]", doc);
    }

    @Test
    public void testNamespacedWebParamsWrapped() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setAddress("http://localhost/test");
        sf.setServiceBean(new FooServiceImpl());
        sf.getServiceFactory().setWrapped(true);
        
        Server server = sf.create();
        
        Document doc = getWSDLDocument(server);
 
        // DOMUtils.writeXml(doc, System.out);
        assertValid("//xsd:schema[@targetNamespace='http://namespace3']", doc);
        assertValid("//xsd:schema[@targetNamespace='http://namespace5']", doc);
    }
    
}
