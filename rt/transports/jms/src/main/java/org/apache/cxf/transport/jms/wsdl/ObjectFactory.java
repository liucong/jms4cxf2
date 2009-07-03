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

package org.apache.cxf.transport.jms.wsdl;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.apache.cxf.transport.jms.wsdl package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName JNDICONNECTIONFACTORYNAME_QNAME = 
        new QName("http://www.w3.org/2008/07/soap/bindings/JMS/", "jndiConnectionFactoryName");
    private static final QName REPLYTONAME_QNAME = 
        new QName("http://www.w3.org/2008/07/soap/bindings/JMS/", "replyToName");
    private static final QName PRIORITY_QNAME = 
        new QName("http://www.w3.org/2008/07/soap/bindings/JMS/", "priority");
    private static final QName JNDIURL_QNAME = 
        new QName("http://www.w3.org/2008/07/soap/bindings/JMS/", "jndiURL");
    private static final QName JNDIINITIALCONTEXTFACTORY_0020_QNAME = 
        new QName("http://www.w3.org/2008/07/soap/bindings/JMS/", "jndiInitialContextFactory");
    private static final QName TIMETOLIVE_QNAME = 
        new QName("http://www.w3.org/2008/07/soap/bindings/JMS/", "timeToLive");
    private static final QName DELIVERYMODE_QNAME = 
        new QName("http://www.w3.org/2008/07/soap/bindings/JMS/", "deliveryMode");
    private static final QName JNDICONTEXTPARAMETER_QNAME = 
        new QName("http://www.w3.org/2008/07/soap/bindings/JMS/", "jndiContextParameter");

    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JndiContextParameterType }
     * 
     */
    public JndiContextParameterType createJndiContextParameterType() {
        return new JndiContextParameterType();
    }

    /**
     * Create an instance of {@link JndiURLType }
     * 
     */
    public JndiURLType createJndiURLType() {
        return new JndiURLType();
    }

    /**
     * Create an instance of {@link JndiConnectionFactoryNameType }
     * 
     */
    public JndiConnectionFactoryNameType createJndiConnectionFactoryNameType() {
        return new JndiConnectionFactoryNameType();
    }

    /**
     * Create an instance of {@link JndiInitialContextFactoryType }
     * 
     */
    public JndiInitialContextFactoryType createJndiInitialContextFactoryType() {
        return new JndiInitialContextFactoryType();
    }

    /**
     * Create an instance of {@link DeliveryModeType }
     * 
     */
    public DeliveryModeType createDeliveryModeType() {
        return new DeliveryModeType();
    }

    /**
     * Create an instance of {@link PriorityType }
     * 
     */
    public PriorityType createPriorityType() {
        return new PriorityType();
    }

    /**
     * Create an instance of {@link ReplyToNameType }
     * 
     */
    public ReplyToNameType createReplyToNameType() {
        return new ReplyToNameType();
    }

    /**
     * Create an instance of {@link TimeToLiveType }
     * 
     */
    public TimeToLiveType createTimeToLiveType() {
        return new TimeToLiveType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JndiConnectionFactoryNameType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/", 
                    name = "jndiConnectionFactoryName")
    public JAXBElement<JndiConnectionFactoryNameType> createJndiConnectionFactoryName(
                    JndiConnectionFactoryNameType value) {
        return new JAXBElement<JndiConnectionFactoryNameType>(
            JNDICONNECTIONFACTORYNAME_QNAME, JndiConnectionFactoryNameType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReplyToNameType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/", name = "replyToName")
    public JAXBElement<ReplyToNameType> createReplyToName(ReplyToNameType value) {
        return new JAXBElement<ReplyToNameType>(REPLYTONAME_QNAME, ReplyToNameType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PriorityType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/", name = "priority")
    public JAXBElement<PriorityType> createPriority(PriorityType value) {
        return new JAXBElement<PriorityType>(PRIORITY_QNAME, PriorityType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JndiURLType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/", name = "jndiURL")
    public JAXBElement<JndiURLType> createJndiURL(JndiURLType value) {
        return new JAXBElement<JndiURLType>(JNDIURL_QNAME, JndiURLType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JndiInitialContextFactoryType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/", 
                    name = "jndiInitialContextFactory")
    public JAXBElement<JndiInitialContextFactoryType> createJndiInitialContextFactory(
                    JndiInitialContextFactoryType value) {
        return new JAXBElement<JndiInitialContextFactoryType>(
            JNDIINITIALCONTEXTFACTORY_0020_QNAME, JndiInitialContextFactoryType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TimeToLiveType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/", name = "timeToLive")
    public JAXBElement<TimeToLiveType> createTimeToLive(TimeToLiveType value) {
        return new JAXBElement<TimeToLiveType>(TIMETOLIVE_QNAME, TimeToLiveType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeliveryModeType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/", name = "deliveryMode")
    public JAXBElement<DeliveryModeType> createDeliveryMode(DeliveryModeType value) {
        return new JAXBElement<DeliveryModeType>(DELIVERYMODE_QNAME, DeliveryModeType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JndiContextParameterType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/", name = "jndiContextParameter")
    public JAXBElement<JndiContextParameterType> createJndiContextParameter(JndiContextParameterType value) {
        return new JAXBElement<JndiContextParameterType>(
            JNDICONTEXTPARAMETER_QNAME, JndiContextParameterType.class, null, value);
    }

}
