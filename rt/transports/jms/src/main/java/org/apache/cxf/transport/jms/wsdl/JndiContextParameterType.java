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

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

/**
 * <p>
 * Java class for jndiContextParameterType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name=&quot;jndiContextParameterType&quot;&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base=&quot;{http://www.w3.org/2001/XMLSchema}anyType&quot;&gt;
 *       &lt;attribute name=&quot;name&quot; use=&quot;required&quot; type=&quot;
 *       {http://www.w3.org/2001/XMLSchema}string&quot; /&gt;
 *       &lt;attribute name=&quot;value&quot; use=&quot;required&quot;&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base=&quot;{http://www.w3.org/2001/XMLSchema}string&quot;&gt;
 *             &lt;minLength value=&quot;1&quot;/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "jndiContextParameterType")
public class JndiContextParameterType implements ExtensibilityElement {

    @XmlAttribute(required = true)
    protected String name;
    @XmlAttribute(required = true)
    protected String value;

    @XmlTransient()
    QName elementType;
    
    @XmlAttribute(namespace = "http://schemas.xmlsoap.org/wsdl/")
    Boolean required;
    
    /**
     * Gets the value of the name property.
     * 
     * @return possible object is {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value allowed object is {@link String }
     */
    public void setName(String v) {
        this.name = value;
    }

    public boolean isSetName() {
        return this.name != null;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return possible object is {@link String }
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value allowed object is {@link String }
     */
    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSetValue() {
        return this.value != null;
    }
    /**
     * Returns the type of this extensibility element.
     * @return QName the type of this element.
     */
    public QName getElementType() {
        return elementType;
    }
    
    /**
     * Sets the type of this extensibility element.
     * @param type QName the type of this element.
     */
    public void setElementType(QName type) {
        elementType = type;
    }

    /**
     * Get whether or not the semantics of this extension are required.
     * Relates to the wsdl:required attribute.
     * @return Boolean
     */
    public Boolean getRequired() {
        return isSetRequired() ? isRequired() : null;
    }
    public void setRequired(Boolean required) {
        this.required = required;
    }
    
    public boolean isSetRequired() {
        return this.required != null;
    }
    
    public boolean isRequired() {
        return required;
    }
}
