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
 * ModifyNetworkInterfaceAttributeTypeChoice_type0.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  ModifyNetworkInterfaceAttributeTypeChoice_type0 bean class
*/

public class ModifyNetworkInterfaceAttributeTypeChoice_type0 implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = ModifyNetworkInterfaceAttributeTypeChoice_type0
            Namespace URI = http://ec2.amazonaws.com/doc/2012-08-15/
            Namespace Prefix = ns1
            */

    private static java.lang.String generatePrefix(java.lang.String namespace) {
        if (namespace.equals("http://ec2.amazonaws.com/doc/2012-08-15/")) {
            return "ns1";
        }
        return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
    }

    /** Whenever a new property is set ensure all others are unset
     *  There can be only one choice and the last one wins
     */
    private void clearAllSettingTrackers() {

        localDescriptionTracker = false;

        localSourceDestCheckTracker = false;

        localGroupSetTracker = false;

        localAttachmentTracker = false;

    }

    /**
    * field for Description
    */

    protected com.amazon.ec2.NullableAttributeValueType localDescription;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localDescriptionTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.NullableAttributeValueType
    */
    public com.amazon.ec2.NullableAttributeValueType getDescription() {
        return localDescription;
    }

    /**
       * Auto generated setter method
       * @param param Description
       */
    public void setDescription(com.amazon.ec2.NullableAttributeValueType param) {

        clearAllSettingTrackers();

        if (param != null) {
            //update the setting tracker
            localDescriptionTracker = true;
        } else {
            localDescriptionTracker = false;

        }

        this.localDescription = param;

    }

    /**
    * field for SourceDestCheck
    */

    protected com.amazon.ec2.AttributeBooleanValueType localSourceDestCheck;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localSourceDestCheckTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.AttributeBooleanValueType
    */
    public com.amazon.ec2.AttributeBooleanValueType getSourceDestCheck() {
        return localSourceDestCheck;
    }

    /**
       * Auto generated setter method
       * @param param SourceDestCheck
       */
    public void setSourceDestCheck(com.amazon.ec2.AttributeBooleanValueType param) {

        clearAllSettingTrackers();

        if (param != null) {
            //update the setting tracker
            localSourceDestCheckTracker = true;
        } else {
            localSourceDestCheckTracker = false;

        }

        this.localSourceDestCheck = param;

    }

    /**
    * field for GroupSet
    */

    protected com.amazon.ec2.SecurityGroupIdSetType localGroupSet;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localGroupSetTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.SecurityGroupIdSetType
    */
    public com.amazon.ec2.SecurityGroupIdSetType getGroupSet() {
        return localGroupSet;
    }

    /**
       * Auto generated setter method
       * @param param GroupSet
       */
    public void setGroupSet(com.amazon.ec2.SecurityGroupIdSetType param) {

        clearAllSettingTrackers();

        if (param != null) {
            //update the setting tracker
            localGroupSetTracker = true;
        } else {
            localGroupSetTracker = false;

        }

        this.localGroupSet = param;

    }

    /**
    * field for Attachment
    */

    protected com.amazon.ec2.ModifyNetworkInterfaceAttachmentType localAttachment;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localAttachmentTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.ModifyNetworkInterfaceAttachmentType
    */
    public com.amazon.ec2.ModifyNetworkInterfaceAttachmentType getAttachment() {
        return localAttachment;
    }

    /**
       * Auto generated setter method
       * @param param Attachment
       */
    public void setAttachment(com.amazon.ec2.ModifyNetworkInterfaceAttachmentType param) {

        clearAllSettingTrackers();

        if (param != null) {
            //update the setting tracker
            localAttachmentTracker = true;
        } else {
            localAttachmentTracker = false;

        }

        this.localAttachment = param;

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
                ModifyNetworkInterfaceAttributeTypeChoice_type0.this.serialize(parentQName, factory, xmlWriter);
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

        if (serializeType) {

            java.lang.String namespacePrefix = registerPrefix(xmlWriter, "http://ec2.amazonaws.com/doc/2012-08-15/");
            if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)) {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":ModifyNetworkInterfaceAttributeTypeChoice_type0",
                    xmlWriter);
            } else {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "ModifyNetworkInterfaceAttributeTypeChoice_type0", xmlWriter);
            }

        }
        if (localDescriptionTracker) {
            if (localDescription == null) {
                throw new org.apache.axis2.databinding.ADBException("description cannot be null!!");
            }
            localDescription.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "description"), factory, xmlWriter);
        }
        if (localSourceDestCheckTracker) {
            if (localSourceDestCheck == null) {
                throw new org.apache.axis2.databinding.ADBException("sourceDestCheck cannot be null!!");
            }
            localSourceDestCheck.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "sourceDestCheck"), factory, xmlWriter);
        }
        if (localGroupSetTracker) {
            if (localGroupSet == null) {
                throw new org.apache.axis2.databinding.ADBException("groupSet cannot be null!!");
            }
            localGroupSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "groupSet"), factory, xmlWriter);
        }
        if (localAttachmentTracker) {
            if (localAttachment == null) {
                throw new org.apache.axis2.databinding.ADBException("attachment cannot be null!!");
            }
            localAttachment.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "attachment"), factory, xmlWriter);
        }

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

        if (localDescriptionTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "description"));

            if (localDescription == null) {
                throw new org.apache.axis2.databinding.ADBException("description cannot be null!!");
            }
            elementList.add(localDescription);
        }
        if (localSourceDestCheckTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "sourceDestCheck"));

            if (localSourceDestCheck == null) {
                throw new org.apache.axis2.databinding.ADBException("sourceDestCheck cannot be null!!");
            }
            elementList.add(localSourceDestCheck);
        }
        if (localGroupSetTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "groupSet"));

            if (localGroupSet == null) {
                throw new org.apache.axis2.databinding.ADBException("groupSet cannot be null!!");
            }
            elementList.add(localGroupSet);
        }
        if (localAttachmentTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "attachment"));

            if (localAttachment == null) {
                throw new org.apache.axis2.databinding.ADBException("attachment cannot be null!!");
            }
            elementList.add(localAttachment);
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
        public static ModifyNetworkInterfaceAttributeTypeChoice_type0 parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            ModifyNetworkInterfaceAttributeTypeChoice_type0 object = new ModifyNetworkInterfaceAttributeTypeChoice_type0();

            int event;
            java.lang.String nillableValue = null;
            java.lang.String prefix = "";
            java.lang.String namespaceuri = "";
            try {

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "description").equals(reader.getName())) {

                    object.setDescription(com.amazon.ec2.NullableAttributeValueType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "sourceDestCheck").equals(reader.getName())) {

                    object.setSourceDestCheck(com.amazon.ec2.AttributeBooleanValueType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "groupSet").equals(reader.getName())) {

                    object.setGroupSet(com.amazon.ec2.SecurityGroupIdSetType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "attachment").equals(reader.getName())) {

                    object.setAttachment(com.amazon.ec2.ModifyNetworkInterfaceAttachmentType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

    }//end of factory class

}
