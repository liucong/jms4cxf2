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
package org.apache.cxf.systest.jms;

import javax.xml.ws.Binding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.systest.jaxws.Hello;
import org.apache.cxf.systest.jaxws.HelloImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

public class Server extends AbstractBusTestServerBase {


    protected void run()  {
        Object implementor = new GreeterImplTwoWayJMS();
        Object impl2 =  new GreeterImplQueueOneWay();
        Object impl3  = new GreeterImplTopicOneWay();
        Object impleDoc = new GreeterImplDoc();
        Object impl4 = new GreeterByteMessageImpl();
        Object impl5 =  new SoapService6SoapPort6Impl();
        Object impl6 = new JmsDestPubSubImpl();
        Object i1 = new GreeterImplTwoWayJMSAppCorrelationIDNoPrefix();
        Object i2 = new GreeterImplTwoWayJMSAppCorrelationIDStaticPrefixEng();
        Object i3 = new GreeterImplTwoWayJMSAppCorrelationIDStaticPrefixSales();
        Object i4 = new GreeterImplTwoWayJMSRuntimeCorrelationIDDynamicPrefix();
        Object i5 = new GreeterImplTwoWayJMSRuntimeCorrelationIDStaticPrefixEng();
        Object i6 = new GreeterImplTwoWayJMSRuntimeCorrelationIDStaticPrefixSales();
        Object i7 = new GreeterImplTwoWayJMSAppCorrelationIDEng();
        Object i8 = new GreeterImplTwoWayJMSAppCorrelationIDSales();
        Object mtom = new JMSMTOMImpl();
        
        Endpoint.publish(null, impleDoc);
        String address = "http://localhost:9000/SoapContext/SoapPort";
        Endpoint.publish(address, implementor);
        Endpoint.publish("http://testaddr.not.required/", impl2);
        Endpoint.publish("http://testaddr.not.required.topic/", impl3);
        Endpoint.publish("http://testaddr.not.required.byte/", impl4);
        Endpoint.publish("http://testaddr.not.required.jms/", impl5);
        Endpoint.publish("http://ignore", impl6);
        Endpoint.publish("", i1);
        Endpoint.publish("", i2);
        Endpoint.publish("", i3);
        Endpoint.publish("", i4);
        Endpoint.publish("", i5);
        Endpoint.publish("", i6);
        Endpoint.publish("", i7);
        Endpoint.publish("", i8);
        EndpointImpl ep = (EndpointImpl)Endpoint.publish("http://cxf.apache.org/transports/jms", mtom);
        Binding binding = ep.getBinding();        
        ((SOAPBinding)binding).setMTOMEnabled(true);  
        
        Object spec1 = new GreeterSpecImpl();
        String address1 = "jms:jndi:dynamicQueues/test.cxf.jmstransport.queue2"
                         + "?jndiInitialContextFactory"
                         + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                         + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=tcp://localhost:61500";
        Endpoint.publish(address1, spec1);
        
        Object spec2 = new GreeterSpecWithPortError();
        String address2 = "jms:jndi:dynamicQueues/test.cxf.jmstransport.queue5"
            + "?jndiInitialContextFactory"
            + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=tcp://localhost:61500";
        Endpoint.publish(address2, spec2);
        
        initNoWsdlServer();
    }


    /**
     * 
     */
    private void initNoWsdlServer() {
        String address = "jms:jndi:dynamicQueues/test.cxf.jmstransport.queue3"
            + "?jndiInitialContextFactory"
            + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=tcp://localhost:61500";
        Hello implementor = new HelloImpl();
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(Hello.class);
        svrFactory.setAddress(address);
        svrFactory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID);
        svrFactory.setServiceBean(implementor);
        svrFactory.create();
    }


    public static void main(String[] args) {
        try {
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
