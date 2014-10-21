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
 * DescribeVolumesSetItemResponseType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  DescribeVolumesSetItemResponseType bean class
*/

public class DescribeVolumesSetItemResponseType implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = DescribeVolumesSetItemResponseType
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
    * field for VolumeId
    */

    protected java.lang.String localVolumeId;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getVolumeId() {
        return localVolumeId;
    }

    /**
       * Auto generated setter method
       * @param param VolumeId
       */
    public void setVolumeId(java.lang.String param) {

        this.localVolumeId = param;

    }

    /**
    * field for Size
    */

    protected java.lang.String localSize;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getSize() {
        return localSize;
    }

    /**
       * Auto generated setter method
       * @param param Size
       */
    public void setSize(java.lang.String param) {

        this.localSize = param;

    }

    /**
    * field for SnapshotId
    */

    protected java.lang.String localSnapshotId;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getSnapshotId() {
        return localSnapshotId;
    }

    /**
       * Auto generated setter method
       * @param param SnapshotId
       */
    public void setSnapshotId(java.lang.String param) {

        this.localSnapshotId = param;

    }

    /**
    * field for AvailabilityZone
    */

    protected java.lang.String localAvailabilityZone;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getAvailabilityZone() {
        return localAvailabilityZone;
    }

    /**
       * Auto generated setter method
       * @param param AvailabilityZone
       */
    public void setAvailabilityZone(java.lang.String param) {

        this.localAvailabilityZone = param;

    }

    /**
    * field for Status
    */

    protected java.lang.String localStatus;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getStatus() {
        return localStatus;
    }

    /**
       * Auto generated setter method
       * @param param Status
       */
    public void setStatus(java.lang.String param) {

        this.localStatus = param;

    }

    /**
    * field for CreateTime
    */

    protected java.util.Calendar localCreateTime;

    /**
    * Auto generated getter method
    * @return java.util.Calendar
    */
    public java.util.Calendar getCreateTime() {
        return localCreateTime;
    }

    /**
       * Auto generated setter method
       * @param param CreateTime
       */
    public void setCreateTime(java.util.Calendar param) {

        this.localCreateTime = param;

    }

    /**
    * field for AttachmentSet
    */

    protected com.amazon.ec2.AttachmentSetResponseType localAttachmentSet;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.AttachmentSetResponseType
    */
    public com.amazon.ec2.AttachmentSetResponseType getAttachmentSet() {
        return localAttachmentSet;
    }

    /**
       * Auto generated setter method
       * @param param AttachmentSet
       */
    public void setAttachmentSet(com.amazon.ec2.AttachmentSetResponseType param) {

        this.localAttachmentSet = param;

    }

    /**
    * field for TagSet
    */

    protected com.amazon.ec2.ResourceTagSetType localTagSet;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localTagSetTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.ResourceTagSetType
    */
    public com.amazon.ec2.ResourceTagSetType getTagSet() {
        return localTagSet;
    }

    /**
       * Auto generated setter method
       * @param param TagSet
       */
    public void setTagSet(com.amazon.ec2.ResourceTagSetType param) {

        if (param != null) {
            //update the setting tracker
            localTagSetTracker = true;
        } else {
            localTagSetTracker = false;

        }

        this.localTagSet = param;

    }

    /**
    * field for VolumeType
    */

    protected java.lang.String localVolumeType;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getVolumeType() {
        return localVolumeType;
    }

    /**
       * Auto generated setter method
       * @param param VolumeType
       */
    public void setVolumeType(java.lang.String param) {

        this.localVolumeType = param;

    }

    /**
    * field for Iops
    */

    protected int localIops;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localIopsTracker = false;

    /**
    * Auto generated getter method
    * @return int
    */
    public int getIops() {
        return localIops;
    }

    /**
       * Auto generated setter method
       * @param param Iops
       */
    public void setIops(int param) {

        // setting primitive attribute tracker to true

        if (param == java.lang.Integer.MIN_VALUE) {
            localIopsTracker = false;

        } else {
            localIopsTracker = true;
        }

        this.localIops = param;

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
                DescribeVolumesSetItemResponseType.this.serialize(parentQName, factory, xmlWriter);
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
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":DescribeVolumesSetItemResponseType", xmlWriter);
            } else {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "DescribeVolumesSetItemResponseType", xmlWriter);
            }

        }

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "volumeId", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "volumeId");
            }

        } else {
            xmlWriter.writeStartElement("volumeId");
        }

        if (localVolumeId == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("volumeId cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localVolumeId);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "size", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "size");
            }

        } else {
            xmlWriter.writeStartElement("size");
        }

        if (localSize == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("size cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localSize);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "snapshotId", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "snapshotId");
            }

        } else {
            xmlWriter.writeStartElement("snapshotId");
        }

        if (localSnapshotId == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("snapshotId cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localSnapshotId);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "availabilityZone", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "availabilityZone");
            }

        } else {
            xmlWriter.writeStartElement("availabilityZone");
        }

        if (localAvailabilityZone == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("availabilityZone cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localAvailabilityZone);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "status", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "status");
            }

        } else {
            xmlWriter.writeStartElement("status");
        }

        if (localStatus == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("status cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localStatus);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "createTime", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "createTime");
            }

        } else {
            xmlWriter.writeStartElement("createTime");
        }

        if (localCreateTime == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("createTime cannot be null!!");

        } else {

            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCreateTime));

        }

        xmlWriter.writeEndElement();

        if (localAttachmentSet == null) {
            throw new org.apache.axis2.databinding.ADBException("attachmentSet cannot be null!!");
        }
        localAttachmentSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "attachmentSet"), factory, xmlWriter);
        if (localTagSetTracker) {
            if (localTagSet == null) {
                throw new org.apache.axis2.databinding.ADBException("tagSet cannot be null!!");
            }
            localTagSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "tagSet"), factory, xmlWriter);
        }
        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "volumeType", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "volumeType");
            }

        } else {
            xmlWriter.writeStartElement("volumeType");
        }

        if (localVolumeType == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("volumeType cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localVolumeType);

        }

        xmlWriter.writeEndElement();
        if (localIopsTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "iops", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "iops");
                }

            } else {
                xmlWriter.writeStartElement("iops");
            }

            if (localIops == java.lang.Integer.MIN_VALUE) {

                throw new org.apache.axis2.databinding.ADBException("iops cannot be null!!");

            } else {
                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIops));
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

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "volumeId"));

        if (localVolumeId != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localVolumeId));
        } else {
            throw new org.apache.axis2.databinding.ADBException("volumeId cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "size"));

        if (localSize != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSize));
        } else {
            throw new org.apache.axis2.databinding.ADBException("size cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "snapshotId"));

        if (localSnapshotId != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSnapshotId));
        } else {
            throw new org.apache.axis2.databinding.ADBException("snapshotId cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "availabilityZone"));

        if (localAvailabilityZone != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAvailabilityZone));
        } else {
            throw new org.apache.axis2.databinding.ADBException("availabilityZone cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "status"));

        if (localStatus != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localStatus));
        } else {
            throw new org.apache.axis2.databinding.ADBException("status cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "createTime"));

        if (localCreateTime != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCreateTime));
        } else {
            throw new org.apache.axis2.databinding.ADBException("createTime cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "attachmentSet"));

        if (localAttachmentSet == null) {
            throw new org.apache.axis2.databinding.ADBException("attachmentSet cannot be null!!");
        }
        elementList.add(localAttachmentSet);
        if (localTagSetTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "tagSet"));

            if (localTagSet == null) {
                throw new org.apache.axis2.databinding.ADBException("tagSet cannot be null!!");
            }
            elementList.add(localTagSet);
        }
        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "volumeType"));

        if (localVolumeType != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localVolumeType));
        } else {
            throw new org.apache.axis2.databinding.ADBException("volumeType cannot be null!!");
        }
        if (localIopsTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "iops"));

            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIops));
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
        public static DescribeVolumesSetItemResponseType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            DescribeVolumesSetItemResponseType object = new DescribeVolumesSetItemResponseType();

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

                        if (!"DescribeVolumesSetItemResponseType".equals(type)) {
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (DescribeVolumesSetItemResponseType)com.amazon.ec2.ExtensionMapper.getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "volumeId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setVolumeId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "size").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setSize(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "snapshotId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setSnapshotId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "availabilityZone").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setAvailabilityZone(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "status").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setStatus(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "createTime").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setCreateTime(org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "attachmentSet").equals(reader.getName())) {

                    object.setAttachmentSet(com.amazon.ec2.AttachmentSetResponseType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "tagSet").equals(reader.getName())) {

                    object.setTagSet(com.amazon.ec2.ResourceTagSetType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "volumeType").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setVolumeType(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "iops").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setIops(org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                    object.setIops(java.lang.Integer.MIN_VALUE);

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
