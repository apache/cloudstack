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
 * BundleInstanceS3StorageType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  BundleInstanceS3StorageType bean class
*/

public class BundleInstanceS3StorageType implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = BundleInstanceS3StorageType
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
    * field for Bucket
    */

    protected java.lang.String localBucket;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getBucket() {
        return localBucket;
    }

    /**
       * Auto generated setter method
       * @param param Bucket
       */
    public void setBucket(java.lang.String param) {

        this.localBucket = param;

    }

    /**
    * field for Prefix
    */

    protected java.lang.String localPrefix;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getPrefix() {
        return localPrefix;
    }

    /**
       * Auto generated setter method
       * @param param Prefix
       */
    public void setPrefix(java.lang.String param) {

        this.localPrefix = param;

    }

    /**
    * field for AwsAccessKeyId
    */

    protected java.lang.String localAwsAccessKeyId;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localAwsAccessKeyIdTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getAwsAccessKeyId() {
        return localAwsAccessKeyId;
    }

    /**
       * Auto generated setter method
       * @param param AwsAccessKeyId
       */
    public void setAwsAccessKeyId(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localAwsAccessKeyIdTracker = true;
        } else {
            localAwsAccessKeyIdTracker = false;

        }

        this.localAwsAccessKeyId = param;

    }

    /**
    * field for UploadPolicy
    */

    protected java.lang.String localUploadPolicy;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localUploadPolicyTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getUploadPolicy() {
        return localUploadPolicy;
    }

    /**
       * Auto generated setter method
       * @param param UploadPolicy
       */
    public void setUploadPolicy(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localUploadPolicyTracker = true;
        } else {
            localUploadPolicyTracker = false;

        }

        this.localUploadPolicy = param;

    }

    /**
    * field for UploadPolicySignature
    */

    protected java.lang.String localUploadPolicySignature;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localUploadPolicySignatureTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getUploadPolicySignature() {
        return localUploadPolicySignature;
    }

    /**
       * Auto generated setter method
       * @param param UploadPolicySignature
       */
    public void setUploadPolicySignature(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localUploadPolicySignatureTracker = true;
        } else {
            localUploadPolicySignatureTracker = false;

        }

        this.localUploadPolicySignature = param;

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
                BundleInstanceS3StorageType.this.serialize(parentQName, factory, xmlWriter);
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
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":BundleInstanceS3StorageType", xmlWriter);
            } else {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "BundleInstanceS3StorageType", xmlWriter);
            }

        }

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "bucket", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "bucket");
            }

        } else {
            xmlWriter.writeStartElement("bucket");
        }

        if (localBucket == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("bucket cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localBucket);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "prefix", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "prefix");
            }

        } else {
            xmlWriter.writeStartElement("prefix");
        }

        if (localPrefix == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("prefix cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localPrefix);

        }

        xmlWriter.writeEndElement();
        if (localAwsAccessKeyIdTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "awsAccessKeyId", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "awsAccessKeyId");
                }

            } else {
                xmlWriter.writeStartElement("awsAccessKeyId");
            }

            if (localAwsAccessKeyId == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("awsAccessKeyId cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localAwsAccessKeyId);

            }

            xmlWriter.writeEndElement();
        }
        if (localUploadPolicyTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "uploadPolicy", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "uploadPolicy");
                }

            } else {
                xmlWriter.writeStartElement("uploadPolicy");
            }

            if (localUploadPolicy == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("uploadPolicy cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localUploadPolicy);

            }

            xmlWriter.writeEndElement();
        }
        if (localUploadPolicySignatureTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "uploadPolicySignature", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "uploadPolicySignature");
                }

            } else {
                xmlWriter.writeStartElement("uploadPolicySignature");
            }

            if (localUploadPolicySignature == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("uploadPolicySignature cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localUploadPolicySignature);

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

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "bucket"));

        if (localBucket != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localBucket));
        } else {
            throw new org.apache.axis2.databinding.ADBException("bucket cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "prefix"));

        if (localPrefix != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPrefix));
        } else {
            throw new org.apache.axis2.databinding.ADBException("prefix cannot be null!!");
        }
        if (localAwsAccessKeyIdTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "awsAccessKeyId"));

            if (localAwsAccessKeyId != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAwsAccessKeyId));
            } else {
                throw new org.apache.axis2.databinding.ADBException("awsAccessKeyId cannot be null!!");
            }
        }
        if (localUploadPolicyTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "uploadPolicy"));

            if (localUploadPolicy != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUploadPolicy));
            } else {
                throw new org.apache.axis2.databinding.ADBException("uploadPolicy cannot be null!!");
            }
        }
        if (localUploadPolicySignatureTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "uploadPolicySignature"));

            if (localUploadPolicySignature != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUploadPolicySignature));
            } else {
                throw new org.apache.axis2.databinding.ADBException("uploadPolicySignature cannot be null!!");
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
        public static BundleInstanceS3StorageType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            BundleInstanceS3StorageType object = new BundleInstanceS3StorageType();

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

                        if (!"BundleInstanceS3StorageType".equals(type)) {
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (BundleInstanceS3StorageType)com.amazon.ec2.ExtensionMapper.getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "bucket").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setBucket(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "prefix").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setPrefix(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "awsAccessKeyId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setAwsAccessKeyId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "uploadPolicy").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setUploadPolicy(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() &&
                    new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "uploadPolicySignature").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setUploadPolicySignature(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

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
