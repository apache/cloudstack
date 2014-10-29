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

/**
 * DescribeAddressesResponseItemType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  DescribeAddressesResponseItemType bean class
*/

public class DescribeAddressesResponseItemType implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = DescribeAddressesResponseItemType
            Namespace URI = http://ec2.amazonaws.com/doc/2012-08-15/
            Namespace Prefix = ns1
            */

    private static java.lang.String generatePrefix(java.lang.String namespace) {
        if (namespace.equals("http://ec2.amazonaws.com/doc/2012-08-15/")) {
            return "ns1";
        }
        return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
    }

    /**
    * field for PublicIp
    */

    protected java.lang.String localPublicIp;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getPublicIp() {
        return localPublicIp;
    }

    /**
       * Auto generated setter method
       * @param param PublicIp
       */
    public void setPublicIp(java.lang.String param) {

        this.localPublicIp = param;

    }

    /**
    * field for AllocationId
    */

    protected java.lang.String localAllocationId;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localAllocationIdTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getAllocationId() {
        return localAllocationId;
    }

    /**
       * Auto generated setter method
       * @param param AllocationId
       */
    public void setAllocationId(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localAllocationIdTracker = true;
        } else {
            localAllocationIdTracker = false;

        }

        this.localAllocationId = param;

    }

    /**
    * field for Domain
    */

    protected java.lang.String localDomain;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getDomain() {
        return localDomain;
    }

    /**
       * Auto generated setter method
       * @param param Domain
       */
    public void setDomain(java.lang.String param) {

        this.localDomain = param;

    }

    /**
    * field for InstanceId
    */

    protected java.lang.String localInstanceId;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localInstanceIdTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getInstanceId() {
        return localInstanceId;
    }

    /**
       * Auto generated setter method
       * @param param InstanceId
       */
    public void setInstanceId(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localInstanceIdTracker = true;
        } else {
            localInstanceIdTracker = false;

        }

        this.localInstanceId = param;

    }

    /**
    * field for AssociationId
    */

    protected java.lang.String localAssociationId;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localAssociationIdTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getAssociationId() {
        return localAssociationId;
    }

    /**
       * Auto generated setter method
       * @param param AssociationId
       */
    public void setAssociationId(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localAssociationIdTracker = true;
        } else {
            localAssociationIdTracker = false;

        }

        this.localAssociationId = param;

    }

    /**
    * field for NetworkInterfaceId
    */

    protected java.lang.String localNetworkInterfaceId;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localNetworkInterfaceIdTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getNetworkInterfaceId() {
        return localNetworkInterfaceId;
    }

    /**
       * Auto generated setter method
       * @param param NetworkInterfaceId
       */
    public void setNetworkInterfaceId(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localNetworkInterfaceIdTracker = true;
        } else {
            localNetworkInterfaceIdTracker = false;

        }

        this.localNetworkInterfaceId = param;

    }

    /**
    * field for NetworkInterfaceOwnerId
    */

    protected java.lang.String localNetworkInterfaceOwnerId;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localNetworkInterfaceOwnerIdTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getNetworkInterfaceOwnerId() {
        return localNetworkInterfaceOwnerId;
    }

    /**
       * Auto generated setter method
       * @param param NetworkInterfaceOwnerId
       */
    public void setNetworkInterfaceOwnerId(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localNetworkInterfaceOwnerIdTracker = true;
        } else {
            localNetworkInterfaceOwnerIdTracker = false;

        }

        this.localNetworkInterfaceOwnerId = param;

    }

    /**
    * field for PrivateIpAddress
    */

    protected java.lang.String localPrivateIpAddress;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localPrivateIpAddressTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getPrivateIpAddress() {
        return localPrivateIpAddress;
    }

    /**
       * Auto generated setter method
       * @param param PrivateIpAddress
       */
    public void setPrivateIpAddress(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localPrivateIpAddressTracker = true;
        } else {
            localPrivateIpAddressTracker = false;

        }

        this.localPrivateIpAddress = param;

    }

    /**
    * isReaderMTOMAware
    * @return true if the reader supports MTOM
    */
    public static boolean isReaderMTOMAware(javax.xml.stream.XMLStreamReader reader) {
        boolean isReaderMTOMAware = false;

        try {
            isReaderMTOMAware = java.lang.Boolean.TRUE.equals(reader.getProperty(org.apache.axiom.om.OMConstants.IS_DATA_HANDLERS_AWARE));
        } catch (java.lang.IllegalArgumentException e) {
            isReaderMTOMAware = false;
        }
        return isReaderMTOMAware;
    }

    /**
    *
    * @param parentQName
    * @param factory
    * @return org.apache.axiom.om.OMElement
    */
    public org.apache.axiom.om.OMElement getOMElement(final javax.xml.namespace.QName parentQName, final org.apache.axiom.om.OMFactory factory)
        throws org.apache.axis2.databinding.ADBException {

        org.apache.axiom.om.OMDataSource dataSource = new org.apache.axis2.databinding.ADBDataSource(this, parentQName) {

            public void serialize(org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
                DescribeAddressesResponseItemType.this.serialize(parentQName, factory, xmlWriter);
            }
        };
        return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(parentQName, factory, dataSource);

    }

    public void serialize(final javax.xml.namespace.QName parentQName, final org.apache.axiom.om.OMFactory factory,
        org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException,
        org.apache.axis2.databinding.ADBException {
        serialize(parentQName, factory, xmlWriter, false);
    }

    public void serialize(final javax.xml.namespace.QName parentQName, final org.apache.axiom.om.OMFactory factory,
        org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter, boolean serializeType) throws javax.xml.stream.XMLStreamException,
        org.apache.axis2.databinding.ADBException {

        java.lang.String prefix = null;
        java.lang.String namespace = null;

        prefix = parentQName.getPrefix();
        namespace = parentQName.getNamespaceURI();

        if ((namespace != null) && (namespace.trim().length() > 0)) {
            java.lang.String writerPrefix = xmlWriter.getPrefix(namespace);
            if (writerPrefix != null) {
                xmlWriter.writeStartElement(namespace, parentQName.getLocalPart());
            } else {
                if (prefix == null) {
                    prefix = generatePrefix(namespace);
                }

                xmlWriter.writeStartElement(prefix, parentQName.getLocalPart(), namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);
            }
        } else {
            xmlWriter.writeStartElement(parentQName.getLocalPart());
        }

        if (serializeType) {

            java.lang.String namespacePrefix = registerPrefix(xmlWriter, "http://ec2.amazonaws.com/doc/2012-08-15/");
            if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)) {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":DescribeAddressesResponseItemType", xmlWriter);
            } else {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "DescribeAddressesResponseItemType", xmlWriter);
            }

        }

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "publicIp", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "publicIp");
            }

        } else {
            xmlWriter.writeStartElement("publicIp");
        }

        if (localPublicIp == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("publicIp cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localPublicIp);

        }

        xmlWriter.writeEndElement();
        if (localAllocationIdTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "allocationId", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "allocationId");
                }

            } else {
                xmlWriter.writeStartElement("allocationId");
            }

            if (localAllocationId == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("allocationId cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localAllocationId);

            }

            xmlWriter.writeEndElement();
        }
        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "domain", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "domain");
            }

        } else {
            xmlWriter.writeStartElement("domain");
        }

        if (localDomain == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("domain cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localDomain);

        }

        xmlWriter.writeEndElement();
        if (localInstanceIdTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "instanceId", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "instanceId");
                }

            } else {
                xmlWriter.writeStartElement("instanceId");
            }

            if (localInstanceId == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("instanceId cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localInstanceId);

            }

            xmlWriter.writeEndElement();
        }
        if (localAssociationIdTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "associationId", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "associationId");
                }

            } else {
                xmlWriter.writeStartElement("associationId");
            }

            if (localAssociationId == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("associationId cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localAssociationId);

            }

            xmlWriter.writeEndElement();
        }
        if (localNetworkInterfaceIdTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "networkInterfaceId", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "networkInterfaceId");
                }

            } else {
                xmlWriter.writeStartElement("networkInterfaceId");
            }

            if (localNetworkInterfaceId == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("networkInterfaceId cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localNetworkInterfaceId);

            }

            xmlWriter.writeEndElement();
        }
        if (localNetworkInterfaceOwnerIdTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "networkInterfaceOwnerId", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "networkInterfaceOwnerId");
                }

            } else {
                xmlWriter.writeStartElement("networkInterfaceOwnerId");
            }

            if (localNetworkInterfaceOwnerId == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("networkInterfaceOwnerId cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localNetworkInterfaceOwnerId);

            }

            xmlWriter.writeEndElement();
        }
        if (localPrivateIpAddressTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "privateIpAddress", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "privateIpAddress");
                }

            } else {
                xmlWriter.writeStartElement("privateIpAddress");
            }

            if (localPrivateIpAddress == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("privateIpAddress cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localPrivateIpAddress);

            }

            xmlWriter.writeEndElement();
        }
        xmlWriter.writeEndElement();

    }

    /**
     * Util method to write an attribute with the ns prefix
     */
    private void writeAttribute(java.lang.String prefix, java.lang.String namespace, java.lang.String attName, java.lang.String attValue,
        javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
        if (xmlWriter.getPrefix(namespace) == null) {
            xmlWriter.writeNamespace(prefix, namespace);
            xmlWriter.setPrefix(prefix, namespace);

        }

        xmlWriter.writeAttribute(namespace, attName, attValue);

    }

    /**
      * Util method to write an attribute without the ns prefix
      */
    private void writeAttribute(java.lang.String namespace, java.lang.String attName, java.lang.String attValue, javax.xml.stream.XMLStreamWriter xmlWriter)
        throws javax.xml.stream.XMLStreamException {
        if (namespace.equals("")) {
            xmlWriter.writeAttribute(attName, attValue);
        } else {
            registerPrefix(xmlWriter, namespace);
            xmlWriter.writeAttribute(namespace, attName, attValue);
        }
    }

    /**
      * Util method to write an attribute without the ns prefix
      */
    private void writeQNameAttribute(java.lang.String namespace, java.lang.String attName, javax.xml.namespace.QName qname, javax.xml.stream.XMLStreamWriter xmlWriter)
        throws javax.xml.stream.XMLStreamException {

        java.lang.String attributeNamespace = qname.getNamespaceURI();
        java.lang.String attributePrefix = xmlWriter.getPrefix(attributeNamespace);
        if (attributePrefix == null) {
            attributePrefix = registerPrefix(xmlWriter, attributeNamespace);
        }
        java.lang.String attributeValue;
        if (attributePrefix.trim().length() > 0) {
            attributeValue = attributePrefix + ":" + qname.getLocalPart();
        } else {
            attributeValue = qname.getLocalPart();
        }

        if (namespace.equals("")) {
            xmlWriter.writeAttribute(attName, attributeValue);
        } else {
            registerPrefix(xmlWriter, namespace);
            xmlWriter.writeAttribute(namespace, attName, attributeValue);
        }
    }

    /**
     *  method to handle Qnames
     */

    private void writeQName(javax.xml.namespace.QName qname, javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
        java.lang.String namespaceURI = qname.getNamespaceURI();
        if (namespaceURI != null) {
            java.lang.String prefix = xmlWriter.getPrefix(namespaceURI);
            if (prefix == null) {
                prefix = generatePrefix(namespaceURI);
                xmlWriter.writeNamespace(prefix, namespaceURI);
                xmlWriter.setPrefix(prefix, namespaceURI);
            }

            if (prefix.trim().length() > 0) {
                xmlWriter.writeCharacters(prefix + ":" + org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
            } else {
                // i.e this is the default namespace
                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
            }

        } else {
            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
        }
    }

    private void writeQNames(javax.xml.namespace.QName[] qnames, javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {

        if (qnames != null) {
            // we have to store this data until last moment since it is not possible to write any
            // namespace data after writing the charactor data
            java.lang.StringBuffer stringToWrite = new java.lang.StringBuffer();
            java.lang.String namespaceURI = null;
            java.lang.String prefix = null;

            for (int i = 0; i < qnames.length; i++) {
                if (i > 0) {
                    stringToWrite.append(" ");
                }
                namespaceURI = qnames[i].getNamespaceURI();
                if (namespaceURI != null) {
                    prefix = xmlWriter.getPrefix(namespaceURI);
                    if ((prefix == null) || (prefix.length() == 0)) {
                        prefix = generatePrefix(namespaceURI);
                        xmlWriter.writeNamespace(prefix, namespaceURI);
                        xmlWriter.setPrefix(prefix, namespaceURI);
                    }

                    if (prefix.trim().length() > 0) {
                        stringToWrite.append(prefix).append(":").append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                    } else {
                        stringToWrite.append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                    }
                } else {
                    stringToWrite.append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                }
            }
            xmlWriter.writeCharacters(stringToWrite.toString());
        }

    }

    /**
    * Register a namespace prefix
    */
    private java.lang.String registerPrefix(javax.xml.stream.XMLStreamWriter xmlWriter, java.lang.String namespace) throws javax.xml.stream.XMLStreamException {
        java.lang.String prefix = xmlWriter.getPrefix(namespace);

        if (prefix == null) {
            prefix = generatePrefix(namespace);

            while (xmlWriter.getNamespaceContext().getNamespaceURI(prefix) != null) {
                prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
            }

            xmlWriter.writeNamespace(prefix, namespace);
            xmlWriter.setPrefix(prefix, namespace);
        }

        return prefix;
    }

    /**
    * databinding method to get an XML representation of this object
    *
    */
    public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) throws org.apache.axis2.databinding.ADBException {

        java.util.ArrayList elementList = new java.util.ArrayList();
        java.util.ArrayList attribList = new java.util.ArrayList();

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "publicIp"));

        if (localPublicIp != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPublicIp));
        } else {
            throw new org.apache.axis2.databinding.ADBException("publicIp cannot be null!!");
        }
        if (localAllocationIdTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "allocationId"));

            if (localAllocationId != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAllocationId));
            } else {
                throw new org.apache.axis2.databinding.ADBException("allocationId cannot be null!!");
            }
        }
        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "domain"));

        if (localDomain != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDomain));
        } else {
            throw new org.apache.axis2.databinding.ADBException("domain cannot be null!!");
        }
        if (localInstanceIdTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceId"));

            if (localInstanceId != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInstanceId));
            } else {
                throw new org.apache.axis2.databinding.ADBException("instanceId cannot be null!!");
            }
        }
        if (localAssociationIdTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "associationId"));

            if (localAssociationId != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAssociationId));
            } else {
                throw new org.apache.axis2.databinding.ADBException("associationId cannot be null!!");
            }
        }
        if (localNetworkInterfaceIdTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "networkInterfaceId"));

            if (localNetworkInterfaceId != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNetworkInterfaceId));
            } else {
                throw new org.apache.axis2.databinding.ADBException("networkInterfaceId cannot be null!!");
            }
        }
        if (localNetworkInterfaceOwnerIdTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "networkInterfaceOwnerId"));

            if (localNetworkInterfaceOwnerId != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNetworkInterfaceOwnerId));
            } else {
                throw new org.apache.axis2.databinding.ADBException("networkInterfaceOwnerId cannot be null!!");
            }
        }
        if (localPrivateIpAddressTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "privateIpAddress"));

            if (localPrivateIpAddress != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPrivateIpAddress));
            } else {
                throw new org.apache.axis2.databinding.ADBException("privateIpAddress cannot be null!!");
            }
        }

        return new org.apache.axis2.databinding.utils.reader.ADBXMLStreamReaderImpl(qName, elementList.toArray(), attribList.toArray());

    }

    /**
     *  Factory class that keeps the parse method
     */
    public static class Factory {

        /**
        * static method to create the object
        * Precondition:  If this object is an element, the current or next start element starts this object and any intervening reader events are ignorable
        *                If this object is not an element, it is a complex type and the reader is at the event just after the outer start element
        * Postcondition: If this object is an element, the reader is positioned at its end element
        *                If this object is a complex type, the reader is positioned at the end element of its outer element
        */
        public static DescribeAddressesResponseItemType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            DescribeAddressesResponseItemType object = new DescribeAddressesResponseItemType();

            int event;
            java.lang.String nillableValue = null;
            java.lang.String prefix = "";
            java.lang.String namespaceuri = "";
            try {

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "type") != null) {
                    java.lang.String fullTypeName = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "type");
                    if (fullTypeName != null) {
                        java.lang.String nsPrefix = null;
                        if (fullTypeName.indexOf(":") > -1) {
                            nsPrefix = fullTypeName.substring(0, fullTypeName.indexOf(":"));
                        }
                        nsPrefix = nsPrefix == null ? "" : nsPrefix;

                        java.lang.String type = fullTypeName.substring(fullTypeName.indexOf(":") + 1);

                        if (!"DescribeAddressesResponseItemType".equals(type)) {
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (DescribeAddressesResponseItemType)com.amazon.ec2.ExtensionMapper.getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "publicIp").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setPublicIp(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "allocationId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setAllocationId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "domain").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setDomain(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setInstanceId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "associationId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setAssociationId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "networkInterfaceId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setNetworkInterfaceId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() &&
                    new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "networkInterfaceOwnerId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setNetworkInterfaceOwnerId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "privateIpAddress").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setPrivateIpAddress(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement())
                    // A start element we are not expecting indicates a trailing invalid property
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());

            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

    }//end of factory class

}
