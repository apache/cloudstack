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
 * NetworkAclEntryType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  NetworkAclEntryType bean class
*/

public class NetworkAclEntryType implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = NetworkAclEntryType
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
    * field for RuleNumber
    */

    protected int localRuleNumber;

    /**
    * Auto generated getter method
    * @return int
    */
    public int getRuleNumber() {
        return localRuleNumber;
    }

    /**
       * Auto generated setter method
       * @param param RuleNumber
       */
    public void setRuleNumber(int param) {

        this.localRuleNumber = param;

    }

    /**
    * field for Protocol
    */

    protected java.lang.String localProtocol;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getProtocol() {
        return localProtocol;
    }

    /**
       * Auto generated setter method
       * @param param Protocol
       */
    public void setProtocol(java.lang.String param) {

        this.localProtocol = param;

    }

    /**
    * field for RuleAction
    */

    protected java.lang.String localRuleAction;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getRuleAction() {
        return localRuleAction;
    }

    /**
       * Auto generated setter method
       * @param param RuleAction
       */
    public void setRuleAction(java.lang.String param) {

        this.localRuleAction = param;

    }

    /**
    * field for Egress
    */

    protected boolean localEgress;

    /**
    * Auto generated getter method
    * @return boolean
    */
    public boolean getEgress() {
        return localEgress;
    }

    /**
       * Auto generated setter method
       * @param param Egress
       */
    public void setEgress(boolean param) {

        this.localEgress = param;

    }

    /**
    * field for CidrBlock
    */

    protected java.lang.String localCidrBlock;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getCidrBlock() {
        return localCidrBlock;
    }

    /**
       * Auto generated setter method
       * @param param CidrBlock
       */
    public void setCidrBlock(java.lang.String param) {

        this.localCidrBlock = param;

    }

    /**
    * field for IcmpTypeCode
    */

    protected com.amazon.ec2.IcmpTypeCodeType localIcmpTypeCode;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localIcmpTypeCodeTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.IcmpTypeCodeType
    */
    public com.amazon.ec2.IcmpTypeCodeType getIcmpTypeCode() {
        return localIcmpTypeCode;
    }

    /**
       * Auto generated setter method
       * @param param IcmpTypeCode
       */
    public void setIcmpTypeCode(com.amazon.ec2.IcmpTypeCodeType param) {

        if (param != null) {
            //update the setting tracker
            localIcmpTypeCodeTracker = true;
        } else {
            localIcmpTypeCodeTracker = false;

        }

        this.localIcmpTypeCode = param;

    }

    /**
    * field for PortRange
    */

    protected com.amazon.ec2.PortRangeType localPortRange;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localPortRangeTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.PortRangeType
    */
    public com.amazon.ec2.PortRangeType getPortRange() {
        return localPortRange;
    }

    /**
       * Auto generated setter method
       * @param param PortRange
       */
    public void setPortRange(com.amazon.ec2.PortRangeType param) {

        if (param != null) {
            //update the setting tracker
            localPortRangeTracker = true;
        } else {
            localPortRangeTracker = false;

        }

        this.localPortRange = param;

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
                NetworkAclEntryType.this.serialize(parentQName, factory, xmlWriter);
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
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":NetworkAclEntryType", xmlWriter);
            } else {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "NetworkAclEntryType", xmlWriter);
            }

        }

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "ruleNumber", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "ruleNumber");
            }

        } else {
            xmlWriter.writeStartElement("ruleNumber");
        }

        if (localRuleNumber == java.lang.Integer.MIN_VALUE) {

            throw new org.apache.axis2.databinding.ADBException("ruleNumber cannot be null!!");

        } else {
            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRuleNumber));
        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "protocol", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "protocol");
            }

        } else {
            xmlWriter.writeStartElement("protocol");
        }

        if (localProtocol == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("protocol cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localProtocol);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "ruleAction", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "ruleAction");
            }

        } else {
            xmlWriter.writeStartElement("ruleAction");
        }

        if (localRuleAction == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("ruleAction cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localRuleAction);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "egress", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "egress");
            }

        } else {
            xmlWriter.writeStartElement("egress");
        }

        if (false) {

            throw new org.apache.axis2.databinding.ADBException("egress cannot be null!!");

        } else {
            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localEgress));
        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "cidrBlock", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "cidrBlock");
            }

        } else {
            xmlWriter.writeStartElement("cidrBlock");
        }

        if (localCidrBlock == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("cidrBlock cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localCidrBlock);

        }

        xmlWriter.writeEndElement();
        if (localIcmpTypeCodeTracker) {
            if (localIcmpTypeCode == null) {
                throw new org.apache.axis2.databinding.ADBException("icmpTypeCode cannot be null!!");
            }
            localIcmpTypeCode.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "icmpTypeCode"), factory, xmlWriter);
        }
        if (localPortRangeTracker) {
            if (localPortRange == null) {
                throw new org.apache.axis2.databinding.ADBException("portRange cannot be null!!");
            }
            localPortRange.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "portRange"), factory, xmlWriter);
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

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "ruleNumber"));

        elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRuleNumber));

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "protocol"));

        if (localProtocol != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localProtocol));
        } else {
            throw new org.apache.axis2.databinding.ADBException("protocol cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "ruleAction"));

        if (localRuleAction != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRuleAction));
        } else {
            throw new org.apache.axis2.databinding.ADBException("ruleAction cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "egress"));

        elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localEgress));

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "cidrBlock"));

        if (localCidrBlock != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCidrBlock));
        } else {
            throw new org.apache.axis2.databinding.ADBException("cidrBlock cannot be null!!");
        }
        if (localIcmpTypeCodeTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "icmpTypeCode"));

            if (localIcmpTypeCode == null) {
                throw new org.apache.axis2.databinding.ADBException("icmpTypeCode cannot be null!!");
            }
            elementList.add(localIcmpTypeCode);
        }
        if (localPortRangeTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "portRange"));

            if (localPortRange == null) {
                throw new org.apache.axis2.databinding.ADBException("portRange cannot be null!!");
            }
            elementList.add(localPortRange);
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
        public static NetworkAclEntryType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            NetworkAclEntryType object = new NetworkAclEntryType();

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

                        if (!"NetworkAclEntryType".equals(type)) {
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (NetworkAclEntryType)com.amazon.ec2.ExtensionMapper.getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "ruleNumber").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setRuleNumber(org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "protocol").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setProtocol(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "ruleAction").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setRuleAction(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "egress").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setEgress(org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "cidrBlock").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setCidrBlock(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "icmpTypeCode").equals(reader.getName())) {

                    object.setIcmpTypeCode(com.amazon.ec2.IcmpTypeCodeType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "portRange").equals(reader.getName())) {

                    object.setPortRange(com.amazon.ec2.PortRangeType.Factory.parse(reader));

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
