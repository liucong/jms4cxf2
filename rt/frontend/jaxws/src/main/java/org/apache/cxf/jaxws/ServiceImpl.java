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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.spi.ServiceDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.ServiceContractResolverRegistry;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.binding.soap.JaxWsSoapBindingConfiguration;
import org.apache.cxf.jaxws.handler.HandlerResolverImpl;
import org.apache.cxf.jaxws.handler.PortInfoImpl;
import org.apache.cxf.jaxws.support.BindingID;
import org.apache.cxf.jaxws.support.DummyImpl;
import org.apache.cxf.jaxws.support.JaxWsClientEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.tools.common.extensions.soap.SoapAddress;
import org.apache.cxf.tools.common.extensions.soap.SoapBinding;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.workqueue.OneShotAsyncExecutor;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl.http.AddressType;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

public class ServiceImpl extends ServiceDelegate {

    private static final Logger LOG = LogUtils.getL7dLogger(ServiceImpl.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    private Bus bus;
    private String wsdlURL;

    private HandlerResolver handlerResolver;
    private Executor executor;
    private QName serviceName;
    private Class<?> clazz;

    private final Collection<QName> ports = new HashSet<QName>();
    private Map<QName, PortInfoImpl> portInfos = new HashMap<QName, PortInfoImpl>();


    public ServiceImpl(Bus b, URL url, QName name, Class<?> cls) {
        bus = b;
        this.serviceName = name;
        clazz = cls;
        
        handlerResolver = new HandlerResolverImpl(bus, name, clazz);
        
        if (null == url && null != bus) {
            ServiceContractResolverRegistry registry = 
                bus.getExtension(ServiceContractResolverRegistry.class);
            if (null != registry) {
                URI uri = registry.getContractLocation(name);
                if (null != uri) {
                    try {
                        url = uri.toURL();
                    } catch (MalformedURLException e) {
                        LOG.log(Level.FINE, "resolve qname failed", name);
                        throw new WebServiceException(e);
                    }
                }
            }
        }

        wsdlURL = url == null ? null : url.toString();
        
        if (url != null) {
            try {
                initializePorts();
            } catch (ServiceConstructionException e) {
                throw new WebServiceException(e);
            }
        }
    }
    
    private void initializePorts() {
        try {
            Definition def = bus.getExtension(WSDLManager.class).getDefinition(wsdlURL);
            javax.wsdl.Service serv = def.getService(serviceName);
            if (serv == null) {
                throw new WebServiceException("Could not find service named " + serviceName 
                                              + " in wsdl " + wsdlURL);
            }
            
            Map<String, Port> wsdlports = CastUtils.cast(serv.getPorts());
            for (Port port : wsdlports.values()) {
                QName name = new QName(serviceName.getNamespaceURI(), port.getName());
                
                String tpId = null;
                String address = null;
                List<? extends ExtensibilityElement> extensions 
                    = CastUtils.cast(port.getBinding().getExtensibilityElements());
                if (!extensions.isEmpty()) {
                    ExtensibilityElement e = extensions.get(0);
                    if (e instanceof SoapBinding) {
                        tpId = ((SoapBinding)e).getTransportURI();
                    } else if (e instanceof SOAP12Binding) {
                        tpId = ((SOAP12Binding)e).getTransportURI();
                    } else if (e instanceof SOAPBinding) {
                        tpId = ((SOAPBinding)e).getTransportURI();
                    }
                }
                extensions = CastUtils.cast(port.getExtensibilityElements());
                if (!extensions.isEmpty()) {
                    ExtensibilityElement e = extensions.get(0);
                    if (tpId == null) {
                        tpId = e.getElementType().getNamespaceURI();
                    }
                    if (e instanceof SoapAddress) {
                        address = ((SoapAddress)e).getLocationURI();
                    } else if (e instanceof AddressType) {
                        address = ((AddressType)e).getLocation();                        
                    } else if (e instanceof SOAP12Address) {
                        address = ((SOAP12Address)e).getLocationURI();
                    } else if (e instanceof SOAPAddress) {
                        address = ((SOAPAddress)e).getLocationURI();                        
                    }
                }
                this.ports.add(name);
                String bindingID = BindingID.getJaxwsBindingID(tpId);
                addPort(name, bindingID, address);
            }
        } catch (WebServiceException e) {
            throw e;
        } catch (Throwable e) {
            WSDLServiceFactory sf = new WSDLServiceFactory(bus, wsdlURL, serviceName);
            Service service = sf.create();
            for (ServiceInfo si : service.getServiceInfos()) { 
                for (EndpointInfo ei : si.getEndpoints()) {
                    this.ports.add(ei.getName());
                    String bindingID = BindingID.getJaxwsBindingID(ei.getTransportId());
                    addPort(ei.getName(), bindingID, ei.getAddress());
                }
            }
        }
    }

    public final void addPort(QName portName, String bindingId, String address) {
        PortInfoImpl portInfo = new PortInfoImpl(bindingId, portName, serviceName);
        portInfo.setAddress(address);
        portInfos.put(portName, portInfo);
    }

    private Endpoint getJaxwsEndpoint(QName portName, AbstractServiceFactoryBean sf, 
                                      WebServiceFeature...features) {
        Service service = sf.getService();
        EndpointInfo ei = null;
        if (portName == null) {
            ei = service.getServiceInfos().get(0).getEndpoints().iterator().next();
        } else {
            ei = service.getEndpointInfo(portName);
            if (ei == null) {
                PortInfoImpl portInfo = getPortInfo(portName);
                if (null != portInfo) {
                    try {
                        ei = createEndpointInfo(sf, portName, portInfo);
                    } catch (BusException e) {
                        throw new WebServiceException(e);
                    }
                }
            }
        }

        if (ei == null) {
            Message msg = new Message("INVALID_PORT", BUNDLE, portName);
            throw new WebServiceException(msg.toString());
        }

        try {
            return new JaxWsClientEndpointImpl(bus, service, ei, this, features);
        } catch (EndpointException e) {
            throw new WebServiceException(e);
        }
    }

    private AbstractServiceFactoryBean createDispatchService(DataBinding db) {
        AbstractServiceFactoryBean serviceFactory;

        Service dispatchService = null;        
        
        if (null != wsdlURL) {
            WSDLServiceFactory sf = new WSDLServiceFactory(bus, wsdlURL, serviceName);
            dispatchService = sf.create();            
            dispatchService.setDataBinding(db);
            serviceFactory = sf;
        } else {
            ReflectionServiceFactoryBean sf = new JaxWsServiceFactoryBean();
            sf.setBus(bus);
            sf.setServiceName(serviceName);
            // maybe we can find another way to create service which have no SEI
            sf.setServiceClass(DummyImpl.class);
            sf.setDataBinding(db);
            dispatchService = sf.create();
            serviceFactory = sf;
        }    
        configureObject(dispatchService);
        return serviceFactory;
    }    

    public Executor getExecutor() {
        return executor;
    }

    public HandlerResolver getHandlerResolver() {
        return handlerResolver;
    }

    public <T> T getPort(Class<T> serviceEndpointInterface) {
        return getPort(serviceEndpointInterface, new WebServiceFeature[]{});
    }

    public <T> T getPort(Class<T> serviceEndpointInterface, WebServiceFeature... features) {
        try {
            return createPort(null, null, serviceEndpointInterface, features);
        } catch (ServiceConstructionException e) {
            throw new WebServiceException(e);
        }
    }

    public <T> T getPort(QName portName, Class<T> serviceEndpointInterface) {
        return getPort(portName, serviceEndpointInterface, new WebServiceFeature[]{});
    }

    public <T> T getPort(QName portName, Class<T> serviceEndpointInterface, WebServiceFeature... features) {
        if (portName == null) {
            throw new WebServiceException(BUNDLE.getString("PORT_NAME_NULL_EXC"));
        }
        
        if (!ports.contains(portName) && !portInfos.containsKey(portName)) {
            throw new WebServiceException(new Message("INVALID_PORT", BUNDLE, portName).toString());
        }
        
        try {
            return createPort(portName, null, serviceEndpointInterface, features);
        } catch (ServiceConstructionException e) {
            throw new WebServiceException(e);
        }
    }

    public <T> T getPort(EndpointReferenceType endpointReference,
                            Class<T> type) {
        return getPort(endpointReference, type, new WebServiceFeature[]{});
    }    
    
    public <T> T getPort(EndpointReferenceType endpointReference, Class<T> type,
                         WebServiceFeature... features) {
        endpointReference = EndpointReferenceUtils.resolve(endpointReference, bus);
        QName serviceQName = EndpointReferenceUtils.getServiceName(endpointReference, bus);
        String portName = EndpointReferenceUtils.getPortName(endpointReference);

        QName portQName = null;
        if (portName != null && serviceQName != null) {
            String ns = serviceQName.getNamespaceURI();
            if (StringUtils.isEmpty(ns)) {
                //hack to workaround a xalan bug
                for (QName qn : portInfos.keySet()) {
                    if (portName.equals(qn.getLocalPart())) {
                        ns = qn.getNamespaceURI();
                    }
                }
            }
            if (StringUtils.isEmpty(ns) && serviceName != null) {
                ns = serviceName.getNamespaceURI();
            }
            portQName = new QName(ns, portName);
        }

        return createPort(portQName, endpointReference, type, features);
    } 
    
    public Iterator<QName> getPorts() {
        return ports.iterator();
    }

    public QName getServiceName() {
        return serviceName;
    }

    public URL getWSDLDocumentLocation() {
        try {
            return new URL(wsdlURL);
        } catch (MalformedURLException e) {
            throw new WebServiceException(e);
        }
    }

    public void setExecutor(Executor e) {
        this.executor = e;
    }

    public void setHandlerResolver(HandlerResolver hr) {
        handlerResolver = hr;
    }

    public Bus getBus() {
        return bus;
    }

    protected <T> T createPort(QName portName, EndpointReferenceType epr, Class<T> serviceEndpointInterface) {
        return createPort(portName, epr, serviceEndpointInterface, new WebServiceFeature[]{});
    }

    protected <T> T createPort(QName portName, EndpointReferenceType epr, Class<T> serviceEndpointInterface, 
                               WebServiceFeature... features) {
        LOG.log(Level.FINE, "creating port for portName", portName);
        LOG.log(Level.FINE, "endpoint reference:", epr);
        LOG.log(Level.FINE, "endpoint interface:", serviceEndpointInterface);

        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        JaxWsClientFactoryBean clientFac = (JaxWsClientFactoryBean) proxyFac.getClientFactoryBean();
        JaxWsServiceFactoryBean serviceFactory = (JaxWsServiceFactoryBean) proxyFac.getServiceFactory();
        proxyFac.initFeatures();
        serviceFactory.setWsFeatures(Arrays.asList(features));
        
        proxyFac.setBus(bus);
        proxyFac.setServiceClass(serviceEndpointInterface);
        proxyFac.setServiceName(serviceName);

        if (wsdlURL != null) {
            proxyFac.setWsdlURL(wsdlURL);
        }
        
        if (portName == null) {
            QName portTypeName = getPortTypeName(serviceEndpointInterface);
            
            Service service = serviceFactory.getService();
            if (service == null) {
                serviceFactory.setServiceClass(serviceEndpointInterface);
                serviceFactory.setBus(getBus());                
                service = serviceFactory.create();
            }
            
            EndpointInfo ei = ServiceModelUtil.findBestEndpointInfo(portTypeName, service.getServiceInfos());
            if (ei != null) {
                portName = ei.getName();
            } else {
                portName = serviceFactory.getEndpointName();
            }
        }
 
        serviceFactory.setEndpointName(portName);
        
        if (epr != null) {
            clientFac.setEndpointReference(epr);
        }
        PortInfoImpl portInfo = portInfos.get(portName);
        if (portInfo != null) {
            clientFac.setBindingId(portInfo.getBindingID());
            clientFac.setAddress(portInfo.getAddress());
        }
        configureObject(portName.toString() + ".jaxws-client.proxyFactory", proxyFac);
        if (clazz != ServiceImpl.class) {
            // handlerchain should be on the generated Service object
            proxyFac.setLoadHandlers(false);
        }
        Object obj = proxyFac.create();
        
        // Configure the Service
        Service service = serviceFactory.getService();
        service.setExecutor(new Executor() {
            public void execute(Runnable command) {
                Executor ex = getExecutor();
                if (ex == null) {
                    ex = OneShotAsyncExecutor.getInstance();
                } 
                ex.execute(command);
            }
        });
        configureObject(service);
                
        // Configure the JaxWsEndpoitnImpl
        JaxWsEndpointImpl jaxwsEndpoint = (JaxWsEndpointImpl) clientFac.getClient().getEndpoint();
        configureObject(jaxwsEndpoint);  
        List<Handler> hc = jaxwsEndpoint.getJaxwsBinding().getHandlerChain();
        
        hc.addAll(handlerResolver.getHandlerChain(portInfos.get(portName)));

        LOG.log(Level.FINE, "created proxy", obj);

        ports.add(portName);
        return serviceEndpointInterface.cast(obj);
    }
    
    private EndpointInfo createEndpointInfo(AbstractServiceFactoryBean serviceFactory, 
                                            QName portName,
                                            PortInfoImpl portInfo) throws BusException {
        EndpointInfo ei = null;               
        String address = portInfo.getAddress();
        String bindingID = BindingID.getBindingID(portInfo.getBindingID());
       
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            //the bindingId might be the transportId, just attempt to 
            //load it to force the factory to load
            dfm.getDestinationFactory(bindingID);
        } catch (BusException ex) {
            //ignore
        }
        DestinationFactory df = dfm.getDestinationFactoryForUri(address);

        String transportId = null;
        if (df != null && df.getTransportIds() != null && !df.getTransportIds().isEmpty()) {
            transportId = df.getTransportIds().get(0);
        } else {
            transportId = bindingID;
        }
                
        Object config = null;
        if (serviceFactory instanceof JaxWsServiceFactoryBean) {
            config = new JaxWsSoapBindingConfiguration((JaxWsServiceFactoryBean)serviceFactory);
        }
        BindingInfo bindingInfo = bus.getExtension(BindingFactoryManager.class).getBindingFactory(bindingID)
                .createBindingInfo(serviceFactory.getService(), bindingID, config);


        Service service = serviceFactory.getService();
        service.getServiceInfos().get(0).addBinding(bindingInfo);

        ei = new EndpointInfo(service.getServiceInfos().get(0), transportId);
        ei.setName(portName);
        ei.setAddress(address);
        ei.setBinding(bindingInfo);

        service.getServiceInfos().get(0).addEndpoint(ei);
        return ei;
    }

    private void configureObject(Object instance) {
        configureObject(null, instance);
    }
    
    private void configureObject(String name, Object instance) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(name, instance);
        }
    }

    private PortInfoImpl getPortInfo(QName portName) {
        // TODO if the portName null ?
        return portInfos.get(portName);
    }
    
    private QName getPortTypeName(Class<?> serviceEndpointInterface) {
        Class<?> seiClass = serviceEndpointInterface;
        if (!serviceEndpointInterface.isAnnotationPresent(WebService.class)) {
            Message msg = new Message("SEI_NO_WEBSERVICE_ANNOTATION", BUNDLE, serviceEndpointInterface
                .getCanonicalName());
            throw new WebServiceException(msg.toString());
        }
 
        if (!serviceEndpointInterface.isInterface()) {
            WebService webService = serviceEndpointInterface.getAnnotation(WebService.class);
            String epi = webService.endpointInterface();
            if (epi.length() > 0) {
                try {
                    seiClass = Thread.currentThread().getContextClassLoader().loadClass(epi);
                } catch (ClassNotFoundException e) {
                    Message msg = new Message("COULD_NOT_LOAD_CLASS", BUNDLE,
                                              seiClass.getCanonicalName());
                    throw new WebServiceException(msg.toString());   
                }
                if (!seiClass.isAnnotationPresent(javax.jws.WebService.class)) {
                    Message msg = new Message("SEI_NO_WEBSERVICE_ANNOTATION", BUNDLE,
                                              seiClass.getCanonicalName());
                    throw new WebServiceException(msg.toString());                
                }
            }
        }

        WebService webService = seiClass.getAnnotation(WebService.class);
        String name = webService.name();
        if (name.length() == 0) {
            name = seiClass.getSimpleName();
        }

        String tns = webService.targetNamespace();
        if (tns.length() == 0) {
            tns = URIParserUtil.getNamespace(PackageUtils.getPackageName(seiClass));
        }

        return new QName(tns, name);
    }
    
    @Override
    public <T> Dispatch<T> createDispatch(QName portName, Class<T> type, Mode mode) {
        return createDispatch(portName, type, mode, new WebServiceFeature[]{});
    }
    
    @Override
    public <T> Dispatch<T> createDispatch(QName portName,
                                          Class<T> type,
                                          Mode mode,
                                          WebServiceFeature... features) {
        JaxWsClientFactoryBean clientFac = new JaxWsClientFactoryBean();

        //Initialize Features.
        configureObject(portName.toString() + ".jaxws-client.proxyFactory", clientFac);

        AbstractServiceFactoryBean sf = null;
        try {
            sf = createDispatchService(new SourceDataBinding());
        } catch (ServiceConstructionException e) {
            throw new WebServiceException(e);
        }
        Endpoint endpoint = getJaxwsEndpoint(portName, sf, features);
        Client client = new ClientImpl(getBus(), endpoint, clientFac.getConduitSelector());
        for (AbstractFeature af : clientFac.getFeatures()) {
            af.initialize(client, bus);
        }
        if (executor != null) {
            client.getEndpoint().setExecutor(executor);
        }
        Dispatch<T> disp = new DispatchImpl<T>(client, mode, type);
        configureObject(disp);

        return disp;
    }
    
    @Override
    public <T> Dispatch<T> createDispatch(EndpointReference endpointReference,
                                          Class<T> type,
                                          Mode mode,
                                          WebServiceFeature... features) {
        EndpointReferenceType ref = VersionTransformer.convertToInternal(endpointReference);
        return createDispatch(EndpointReferenceUtils.getPortQName(ref, bus), 
                              type, mode, features);
    }   


    @Override
    public Dispatch<Object> createDispatch(QName portName, JAXBContext context, Mode mode) {
        return createDispatch(portName, context, mode, new WebServiceFeature[]{});
    }    

    @Override
    public Dispatch<Object> createDispatch(QName portName,
                                           JAXBContext context,
                                           Mode mode,
                                           WebServiceFeature... features) {
        JaxWsClientFactoryBean clientFac = new JaxWsClientFactoryBean();
        
        //Initialize Features.
        configureObject(portName.toString() + ".jaxws-client.proxyFactory", clientFac);

        AbstractServiceFactoryBean sf = null;
        try {
            JAXBDataBinding db = new JAXBDataBinding(context);
            db.setUnwrapJAXBElement(false);
            sf = createDispatchService(db);
        } catch (ServiceConstructionException e) {
            throw new WebServiceException(e);
        }
        Endpoint endpoint = getJaxwsEndpoint(portName, sf, features);
        Client client = new ClientImpl(getBus(), endpoint, clientFac.getConduitSelector());
        for (AbstractFeature af : clientFac.getFeatures()) {
            af.initialize(client, bus);
        }
        if (executor != null) {
            client.getEndpoint().setExecutor(executor);
        }
        Dispatch<Object> disp = new DispatchImpl<Object>(client, mode, 
                                                         context, Object.class);
        configureObject(disp);

        return disp;
    }

    @Override
    public Dispatch<Object> createDispatch(EndpointReference endpointReference,
                                           JAXBContext context,
                                           Mode mode,
                                           WebServiceFeature... features) {
        EndpointReferenceType ref = VersionTransformer.convertToInternal(endpointReference);
        return createDispatch(EndpointReferenceUtils.getPortQName(ref, bus), 
                              context, mode, features);        
    }

    @Override
    public <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface,
                         WebServiceFeature... features) {
        return getPort(VersionTransformer.convertToInternal(endpointReference), serviceEndpointInterface,
                       features);

    }
}
