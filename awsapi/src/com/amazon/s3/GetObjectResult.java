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
 * GetObjectResult.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:34 EDT)
 */

package com.amazon.s3;

/**
*  GetObjectResult bean class
*/

public class GetObjectResult extends com.amazon.s3.Result implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = GetObjectResult
            Namespace URI = http://s3.amazonaws.com/doc/2006-03-01/
            Namespace Prefix = ns1
            */

    private static java.lang.String generatePrefix(java.lang.String namespace) {
        if (namespace.equals("http://s3.amazonaws.com/doc/2006-03-01/")) {
            return "ns1";
        }
        return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
    }

    /**
    * field for Metadata
    * This was an Array!
    */

    protected com.amazon.s3.MetadataEntry[] localMetadata;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localMetadataTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.s3.MetadataEntry[]
    */
    public com.amazon.s3.MetadataEntry[] getMetadata() {
        return localMetadata;
    }

    /**
     * validate the array for Metadata
     */
    protected void validateMetadata(com.amazon.s3.MetadataEntry[] param) {

    }

    /**
     * Auto generated setter method
     * @param param Metadata
     */
    public void setMetadata(com.amazon.s3.MetadataEntry[] param) {

        validateMetadata(param);

        if (param != null) {
            //update the setting tracker
            localMetadataTracker = true;
        } else {
            localMetadataTracker = false;

        }

        this.localMetadata = param;
    }

    /**
    * Auto generated add method for the array for convenience
    * @param param com.amazon.s3.MetadataEntry
    */
    public void addMetadata(com.amazon.s3.MetadataEntry param) {
        if (localMetadata == null) {
            localMetadata = new com.amazon.s3.MetadataEntry[] {};
        }

        //update the setting tracker
        localMetadataTracker = true;

        java.util.List list = org.apache.axis2.databinding.utils.ConverterUtil.toList(localMetadata);
        list.add(param);
        this.localMetadata = (com.amazon.s3.MetadataEntry[])list.toArray(new com.amazon.s3.MetadataEntry[list.size()]);

    }

    /**
    * field for Data
    */

    protected javax.activation.DataHandler localData;

    /**
    * Auto generated getter method
    * @return javax.activation.DataHandler
    */
    public javax.activation.DataHandler getData() {
        return localData;
    }

    /**
       * Auto generated setter method
       * @param param Data
       */
    public void setData(javax.activation.DataHandler param) {

        this.localData = param;

    }

    /**
    * field for LastModified
    */

    protected java.util.Calendar localLastModified;

    /**
    * Auto generated getter method
    * @return java.util.Calendar
    */
    public java.util.Calendar getLastModified() {
        return localLastModified;
    }

    /**
       * Auto generated setter method
       * @param param LastModified
       */
    public void setLastModified(java.util.Calendar param) {

        this.localLastModified = param;

    }

    /**
    * field for ETag
    */

    protected java.lang.String localETag;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getETag() {
        return localETag;
    }

    /**
       * Auto generated setter method
       * @param param ETag
       */
    public void setETag(java.lang.String param) {

        this.localETag = param;

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
                GetObjectResult.this.serialize(parentQName, factory, xmlWriter);
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

        java.lang.String namespacePrefix = registerPrefix(xmlWriter, "http://s3.amazonaws.com/doc/2006-03-01/");
        if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)) {
            writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":GetObjectResult", xmlWriter);
        } else {
            writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "GetObjectResult", xmlWriter);
        }

        if (localStatus == null) {
            throw new org.apache.axis2.databinding.ADBException("Status cannot be null!!");
        }
        localStatus.serialize(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Status"), factory, xmlWriter);
        if (localMetadataTracker) {
            if (localMetadata != null) {
                for (int i = 0; i < localMetadata.length; i++) {
                    if (localMetadata[i] != null) {
                        localMetadata[i].serialize(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Metadata"), factory, xmlWriter);
                    } else {

                        // we don't have to do any thing since minOccures is zero

                    }

                }
            } else {

                throw new org.apache.axis2.databinding.ADBException("Metadata cannot be null!!");

            }
        }
        namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "Data", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "Data");
            }

        } else {
            xmlWriter.writeStartElement("Data");
        }

        if (localData != null) {
            xmlWriter.writeDataHandler(localData);
        }

        xmlWriter.writeEndElement();

        namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "LastModified", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "LastModified");
            }

        } else {
            xmlWriter.writeStartElement("LastModified");
        }

        if (localLastModified == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("LastModified cannot be null!!");

        } else {

            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localLastModified));

        }

        xmlWriter.writeEndElement();

        namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "ETag", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "ETag");
            }

        } else {
            xmlWriter.writeStartElement("ETag");
        }

        if (localETag == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("ETag cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localETag);

        }

        xmlWriter.writeEndElement();

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

        attribList.add(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema-instance", "type"));
        attribList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "GetObjectResult"));

        elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Status"));

        if (localStatus == null) {
            throw new org.apache.axis2.databinding.ADBException("Status cannot be null!!");
        }
        elementList.add(localStatus);
        if (localMetadataTracker) {
            if (localMetadata != null) {
                for (int i = 0; i < localMetadata.length; i++) {

                    if (localMetadata[i] != null) {
                        elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Metadata"));
                        elementList.add(localMetadata[i]);
                    } else {

                        // nothing to do

                    }

                }
            } else {

                throw new org.apache.axis2.databinding.ADBException("Metadata cannot be null!!");

            }

        }
        elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Data"));

        elementList.add(localData);

        elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "LastModified"));

        if (localLastModified != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localLastModified));
        } else {
            throw new org.apache.axis2.databinding.ADBException("LastModified cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "ETag"));

        if (localETag != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localETag));
        } else {
            throw new org.apache.axis2.databinding.ADBException("ETag cannot be null!!");
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
        public static GetObjectResult parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            GetObjectResult object = new GetObjectResult();

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

                        if (!"GetObjectResult".equals(type)) {
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (GetObjectResult)com.amazon.s3.ExtensionMapper.getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                java.util.ArrayList list2 = new java.util.ArrayList();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Status").equals(reader.getName())) {

                    object.setStatus(com.amazon.s3.Status.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Metadata").equals(reader.getName())) {

                    // Process the array and step past its final element's end.
                    list2.add(com.amazon.s3.MetadataEntry.Factory.parse(reader));

                    //loop until we find a start element that is not part of this array
                    boolean loopDone2 = false;
                    while (!loopDone2) {
                        // We should be at the end element, but make sure
                        while (!reader.isEndElement())
                            reader.next();
                        // Step out of this element
                        reader.next();
                        // Step to next element event.
                        while (!reader.isStartElement() && !reader.isEndElement())
                            reader.next();
                        if (reader.isEndElement()) {
                            //two continuous end elements means we are exiting the xml structure
                            loopDone2 = true;
                        } else {
                            if (new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Metadata").equals(reader.getName())) {
                                list2.add(com.amazon.s3.MetadataEntry.Factory.parse(reader));

                            } else {
                                loopDone2 = true;
                            }
                        }
                    }
                    // call the converter utility  to convert and set the array

                    object.setMetadata((com.amazon.s3.MetadataEntry[])org.apache.axis2.databinding.utils.ConverterUtil.convertToArray(com.amazon.s3.MetadataEntry.class,
                        list2));

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Data").equals(reader.getName())) {
                    reader.next();
                    if (isReaderMTOMAware(reader) && java.lang.Boolean.TRUE.equals(reader.getProperty(org.apache.axiom.om.OMConstants.IS_BINARY))) {
                        //MTOM aware reader - get the datahandler directly and put it in the object
                        object.setData((javax.activation.DataHandler)reader.getProperty(org.apache.axiom.om.OMConstants.DATA_HANDLER));
                    } else {
                        if (reader.getEventType() == javax.xml.stream.XMLStreamConstants.START_ELEMENT &&
                            reader.getName().equals(
                                new javax.xml.namespace.QName(org.apache.axiom.om.impl.MTOMConstants.XOP_NAMESPACE_URI,
                                    org.apache.axiom.om.impl.MTOMConstants.XOP_INCLUDE))) {
                            java.lang.String id = org.apache.axiom.om.util.ElementHelper.getContentID(reader, "UTF-8");
                            object.setData(((org.apache.axiom.soap.impl.builder.MTOMStAXSOAPModelBuilder)((org.apache.axiom.om.impl.llom.OMStAXWrapper)reader).getBuilder()).getDataHandler(id));
                            reader.next();

                            reader.next();

                        } else if (reader.hasText()) {
                            //Do the usual conversion
                            java.lang.String content = reader.getText();
                            object.setData(org.apache.axis2.databinding.utils.ConverterUtil.convertToBase64Binary(content));

                            reader.next();

                        }
                    }

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "LastModified").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setLastModified(org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "ETag").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setETag(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
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
