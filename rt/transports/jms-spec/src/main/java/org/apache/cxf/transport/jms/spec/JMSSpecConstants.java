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

package org.apache.cxf.transport.jms.spec;

import javax.xml.namespace.QName;

/**
 * 
 */
public final class JMSSpecConstants {

    public static final String SOAP_JMS_SPECIFICIATION_TRANSPORTID = "http://www.w3.org/2008/07/"
                                                                     + "soap/bindings/JMS/";
    public static final String SOAP_JMS_NAMESPACE = SOAP_JMS_SPECIFICIATION_TRANSPORTID;

    public static final String SOAP_JMS_PREFIX = "SOAPJMS_";

    // Connection to a destination properties
    // just for jms uri
    public static final String LOOKUPVARIANT_PARAMETER_NAME = "lookupVariant";
    public static final String DESTINATIONNAME_PARAMETER_NAME = "destinationName";
    // other connection destination properties
    public static final String JNDICONNECTIONFACTORYNAME_PARAMETER_NAME = "jndiConnectionFactoryName";
    public static final String JNDIINITIALCONTEXTFACTORY_PARAMETER_NAME = "jndiInitialContextFactory";
    public static final String JNDIURL_PARAMETER_NAME = "jndiURL";
    public static final String JNDICONTEXTPARAMETER_PARAMETER_NAME = "jndiContextParameter";

    // JMS Message Header properties
    public static final String DELIVERYMODE_PARAMETER_NAME = "deliveryMode";
    // Expiration Time
    public static final String TIMETOLIVE_PARAMETER_NAME = "timeToLive";
    public static final String PRIORITY_PARAMETER_NAME = "priority";
    // Destination
    public static final String REPLYTONAME_PARAMETER_NAME = "replyToName";

    // JMS Message properties' names.
    public static final String REQUESTURI_PARAMETER_NAME = "requestURI";
    public static final String BINDINGVERSION_PARAMETER_NAME = "bindingVersion";
    public static final String SOAPACTION_PARAMETER_NAME = "soapAction";
    public static final String TARGETSERVICE_PARAMETER_NAME = "targetService";
    public static final String CONTENTTYPE_PARAMETER_NAME = "contentType";
    public static final String ISFAULT_PARAMETER_NAME = "isFault";

    // JMS Field name
    public static final String REQUESTURI_FIELD = SOAP_JMS_PREFIX + REQUESTURI_PARAMETER_NAME;
    public static final String BINDINGVERSION_FIELD = SOAP_JMS_PREFIX
                                                      + BINDINGVERSION_PARAMETER_NAME;
    public static final String SOAPACTION_FIELD = SOAP_JMS_PREFIX + SOAPACTION_PARAMETER_NAME;
    public static final String TARGETSERVICE_FIELD = SOAP_JMS_PREFIX + TARGETSERVICE_PARAMETER_NAME;
    public static final String CONTENTTYPE_FIELD = SOAP_JMS_PREFIX + CONTENTTYPE_PARAMETER_NAME;
    public static final String ISFAULT_FIELD = SOAP_JMS_PREFIX + ISFAULT_PARAMETER_NAME;

/*    public static final String JMS_SERVER_REQUEST_MESSAGE_PROPERTIES = 
        "org.apache.cxf.jms.server.request.message.properteis";
    public static final String JMS_SERVER_RESPONSE_MESSAGE_PROPERTIES = 
        "org.apache.cxf.jms.server.response.message.properteis";
    public static final String JMS_CLIENT_REQUEST_MESSAGE_PROPERTIES = 
        "org.apache.cxf.jms.client.request.message.properteis";
    public static final String JMS_CLIENT_RESPONSE_MESSAGE_PROPERTIES = 
        "org.apache.cxf.jms.client.response.message.properteis";*/

    //fault codes
    private static final String JMS_CONTENTTYPEMISMATCH_FAULT_CODE = "contentTypeMismatch";
    private static final String JMS_MALFORMEDREQUESTURI_FAULT_CODE = "malformedRequestURI";
    private static final String JMS_MISMATCHEDSOAPACTION_FAULT_CODE = "mismatchedSoapAction";
    private static final String JMS_MISSINGCONTENTTYPE_FAULT_CODE = "missingContentType";
    private static final String JMS_MISSINGREQUESTURI_FAULT_CODE = "missingRequestURI";
    private static final String JMS_TARGETSERVICENOTALLOWEDINREQUESTURI_FAULT_CODE = 
        "targetServiceNotAllowedInRequestURI";
    private static final String JMS_UNRECOGNIZEDBINDINGVERSION_FAULT_CODE = "unrecognizedBindingVersion";
    private static final String JMS_UNSUPPORTEDJMSMESSAGEFORMAT_FAULT_CODE = "unsupportedJMSMessageFormat";
    
    private JMSSpecConstants() {
    }
    
    public static QName getContentTypeMismatchQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_CONTENTTYPEMISMATCH_FAULT_CODE);   
    }
    
    public static QName getMalformedRequestURIQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_MALFORMEDREQUESTURI_FAULT_CODE);
    }
    
    public static QName getMismatchedSoapActionQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_MISMATCHEDSOAPACTION_FAULT_CODE);
    }
    
    public static QName getMissingContentTypeQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_MISSINGCONTENTTYPE_FAULT_CODE);
    }
    
    public static QName getMissingRequestURIQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_MISSINGREQUESTURI_FAULT_CODE);
    }
    
    public static QName getTargetServiceNotAllowedInRequestURIQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_TARGETSERVICENOTALLOWEDINREQUESTURI_FAULT_CODE);
    }
    
    public static QName getUnrecognizedBindingVersionQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_UNRECOGNIZEDBINDINGVERSION_FAULT_CODE);
    }
    
    public static QName getUnsupportedJMSMessageFormatQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_UNSUPPORTEDJMSMESSAGEFORMAT_FAULT_CODE);
    }
}
