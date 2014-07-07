// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
package com.cloud.baremetal.networkservice.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SecurityGroupRule complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SecurityGroupRule">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="protocol" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="startPort" type="{http://www.w3.org/2001/XMLSchema}unsignedInt"/>
 *         &lt;element name="endPort" type="{http://www.w3.org/2001/XMLSchema}unsignedInt"/>
 *         &lt;sequence maxOccurs="unbounded" minOccurs="0">
 *           &lt;element name="ip" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;/sequence>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SecurityGroupRule", propOrder = {
    "protocol",
    "startPort",
    "endPort",
    "ip"
})
public class SecurityGroupRule {

    @XmlElement(required = true)
    protected String protocol;
    @XmlSchemaType(name = "unsignedInt")
    protected long startPort;
    @XmlSchemaType(name = "unsignedInt")
    protected long endPort;
    protected List<String> ip;

    /**
     * Gets the value of the protocol property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the value of the protocol property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setProtocol(String value) {
        this.protocol = value;
    }

    /**
     * Gets the value of the startPort property.
     *
     */
    public long getStartPort() {
        return startPort;
    }

    /**
     * Sets the value of the startPort property.
     *
     */
    public void setStartPort(long value) {
        this.startPort = value;
    }

    /**
     * Gets the value of the endPort property.
     *
     */
    public long getEndPort() {
        return endPort;
    }

    /**
     * Sets the value of the endPort property.
     *
     */
    public void setEndPort(long value) {
        this.endPort = value;
    }

    /**
     * Gets the value of the ip property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ip property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIp().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getIp() {
        if (ip == null) {
            ip = new ArrayList<String>();
        }
        return this.ip;
    }

}
