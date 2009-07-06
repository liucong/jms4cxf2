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

package org.apache.cxf.transport.jms;

import java.util.Iterator;
import java.util.List;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;

import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.wsdl11.SoapAddressPlugin;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.extensions.soap.SoapAddress;
import org.apache.cxf.tools.util.SOAPBindingUtil;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;

/**
 * 
 */
public class JMSSpecTransportFactory extends JMSTransportFactory implements WSDLEndpointFactory {
    public EndpointInfo createEndpointInfo(ServiceInfo serviceInfo, BindingInfo b, Port port) {
        String transportURI = JMSSpecConstants.SOAP_JMS_NAMESPACE;
        if (b instanceof SoapBindingInfo) {
            SoapBindingInfo sbi = (SoapBindingInfo)b;
            transportURI = sbi.getTransportURI();
        }
        if (port != null) {
            List ees = port.getExtensibilityElements();
            for (Iterator itr = ees.iterator(); itr.hasNext();) {
                Object extensor = itr.next();

                if (SOAPBindingUtil.isSOAPAddress(extensor)) {
                    final SoapAddress sa = SOAPBindingUtil.getSoapAddress(extensor);

                    EndpointInfo info = new SoapEndpointInfo(serviceInfo, transportURI);
                    info.addExtensor(sa);
                    info.setAddress(sa.getLocationURI());
                    return info;
                }
            }
        }
        return new SoapEndpointInfo(serviceInfo, transportURI);
    }

    public void createPortExtensors(EndpointInfo ei, Service service) {
        if (ei.getBinding() instanceof SoapBindingInfo) {
            SoapBindingInfo bi = (SoapBindingInfo)ei.getBinding();
            createSoapExtensors(ei, bi, bi.getSoapVersion() instanceof Soap12);
        }
    }

    private void createSoapExtensors(EndpointInfo ei, SoapBindingInfo bi, boolean isSoap12) {
        try {
            // We need to populate the soap extensibilityelement proxy for soap11 and soap12
            ExtensionRegistry extensionRegistry = WSDLFactory.newInstance()
                .newPopulatedExtensionRegistry();
            SoapAddressPlugin addresser = new SoapAddressPlugin();
            addresser.setExtensionRegistry(extensionRegistry);
            // SoapAddress soapAddress = SOAPBindingUtil.createSoapAddress(extensionRegistry, isSoap12);
            String address = ei.getAddress();
            if (address == null) {
                address = "http://localhost:9090";
            }

            // soapAddress.setLocationURI(address);
            ei.addExtensor(addresser.createExtension(isSoap12, address));

            // createSoapBinding(isSoap12, extensionRegistry, bi);

        } catch (WSDLException e) {
            e.printStackTrace();
        }
    }

    private static class SoapEndpointInfo extends EndpointInfo {
        SoapAddress saddress;

        SoapEndpointInfo(ServiceInfo serv, String trans) {
            super(serv, trans);
        }

        public void setAddress(String s) {
            super.setAddress(s);
            if (saddress != null) {
                saddress.setLocationURI(s);
            }
        }

        public void addExtensor(Object el) {
            super.addExtensor(el);
            if (el instanceof SoapAddress) {
                saddress = (SoapAddress)el;
            }
        }
    }
}
