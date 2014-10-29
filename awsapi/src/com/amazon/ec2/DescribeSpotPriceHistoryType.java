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
 * DescribeSpotPriceHistoryType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  DescribeSpotPriceHistoryType bean class
*/

public class DescribeSpotPriceHistoryType implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = DescribeSpotPriceHistoryType
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
    * field for StartTime
    */

    protected java.util.Calendar localStartTime;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localStartTimeTracker = false;

    /**
    * Auto generated getter method
    * @return java.util.Calendar
    */
    public java.util.Calendar getStartTime() {
        return localStartTime;
    }

    /**
       * Auto generated setter method
       * @param param StartTime
       */
    public void setStartTime(java.util.Calendar param) {

        if (param != null) {
            //update the setting tracker
            localStartTimeTracker = true;
        } else {
            localStartTimeTracker = false;

        }

        this.localStartTime = param;

    }

    /**
    * field for EndTime
    */

    protected java.util.Calendar localEndTime;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localEndTimeTracker = false;

    /**
    * Auto generated getter method
    * @return java.util.Calendar
    */
    public java.util.Calendar getEndTime() {
        return localEndTime;
    }

    /**
       * Auto generated setter method
       * @param param EndTime
       */
    public void setEndTime(java.util.Calendar param) {

        if (param != null) {
            //update the setting tracker
            localEndTimeTracker = true;
        } else {
            localEndTimeTracker = false;

        }

        this.localEndTime = param;

    }

    /**
    * field for InstanceTypeSet
    */

    protected com.amazon.ec2.InstanceTypeSetType localInstanceTypeSet;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localInstanceTypeSetTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.InstanceTypeSetType
    */
    public com.amazon.ec2.InstanceTypeSetType getInstanceTypeSet() {
        return localInstanceTypeSet;
    }

    /**
       * Auto generated setter method
       * @param param InstanceTypeSet
       */
    public void setInstanceTypeSet(com.amazon.ec2.InstanceTypeSetType param) {

        if (param != null) {
            //update the setting tracker
            localInstanceTypeSetTracker = true;
        } else {
            localInstanceTypeSetTracker = false;

        }

        this.localInstanceTypeSet = param;

    }

    /**
    * field for ProductDescriptionSet
    */

    protected com.amazon.ec2.ProductDescriptionSetType localProductDescriptionSet;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localProductDescriptionSetTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.ProductDescriptionSetType
    */
    public com.amazon.ec2.ProductDescriptionSetType getProductDescriptionSet() {
        return localProductDescriptionSet;
    }

    /**
       * Auto generated setter method
       * @param param ProductDescriptionSet
       */
    public void setProductDescriptionSet(com.amazon.ec2.ProductDescriptionSetType param) {

        if (param != null) {
            //update the setting tracker
            localProductDescriptionSetTracker = true;
        } else {
            localProductDescriptionSetTracker = false;

        }

        this.localProductDescriptionSet = param;

    }

    /**
    * field for FilterSet
    */

    protected com.amazon.ec2.FilterSetType localFilterSet;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localFilterSetTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.FilterSetType
    */
    public com.amazon.ec2.FilterSetType getFilterSet() {
        return localFilterSet;
    }

    /**
       * Auto generated setter method
       * @param param FilterSet
       */
    public void setFilterSet(com.amazon.ec2.FilterSetType param) {

        if (param != null) {
            //update the setting tracker
            localFilterSetTracker = true;
        } else {
            localFilterSetTracker = false;

        }

        this.localFilterSet = param;

    }

    /**
    * field for AvailabilityZone
    */

    protected java.lang.String localAvailabilityZone;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localAvailabilityZoneTracker = false;

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

        if (param != null) {
            //update the setting tracker
            localAvailabilityZoneTracker = true;
        } else {
            localAvailabilityZoneTracker = false;

        }

        this.localAvailabilityZone = param;

    }

    /**
    * field for MaxResults
    */

    protected java.math.BigInteger localMaxResults;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localMaxResultsTracker = false;

    /**
    * Auto generated getter method
    * @return java.math.BigInteger
    */
    public java.math.BigInteger getMaxResults() {
        return localMaxResults;
    }

    /**
       * Auto generated setter method
       * @param param MaxResults
       */
    public void setMaxResults(java.math.BigInteger param) {

        if (param != null) {
            //update the setting tracker
            localMaxResultsTracker = true;
        } else {
            localMaxResultsTracker = false;

        }

        this.localMaxResults = param;

    }

    /**
    * field for NextToken
    */

    protected java.lang.String localNextToken;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localNextTokenTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getNextToken() {
        return localNextToken;
    }

    /**
       * Auto generated setter method
       * @param param NextToken
       */
    public void setNextToken(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localNextTokenTracker = true;
        } else {
            localNextTokenTracker = false;

        }

        this.localNextToken = param;

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
                DescribeSpotPriceHistoryType.this.serialize(parentQName, factory, xmlWriter);
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
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":DescribeSpotPriceHistoryType", xmlWriter);
            } else {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "DescribeSpotPriceHistoryType", xmlWriter);
            }

        }
        if (localStartTimeTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "startTime", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "startTime");
                }

            } else {
                xmlWriter.writeStartElement("startTime");
            }

            if (localStartTime == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("startTime cannot be null!!");

            } else {

                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localStartTime));

            }

            xmlWriter.writeEndElement();
        }
        if (localEndTimeTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "endTime", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "endTime");
                }

            } else {
                xmlWriter.writeStartElement("endTime");
            }

            if (localEndTime == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("endTime cannot be null!!");

            } else {

                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localEndTime));

            }

            xmlWriter.writeEndElement();
        }
        if (localInstanceTypeSetTracker) {
            if (localInstanceTypeSet == null) {
                throw new org.apache.axis2.databinding.ADBException("instanceTypeSet cannot be null!!");
            }
            localInstanceTypeSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceTypeSet"), factory, xmlWriter);
        }
        if (localProductDescriptionSetTracker) {
            if (localProductDescriptionSet == null) {
                throw new org.apache.axis2.databinding.ADBException("productDescriptionSet cannot be null!!");
            }
            localProductDescriptionSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "productDescriptionSet"), factory, xmlWriter);
        }
        if (localFilterSetTracker) {
            if (localFilterSet == null) {
                throw new org.apache.axis2.databinding.ADBException("filterSet cannot be null!!");
            }
            localFilterSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "filterSet"), factory, xmlWriter);
        }
        if (localAvailabilityZoneTracker) {
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
        }
        if (localMaxResultsTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "maxResults", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "maxResults");
                }

            } else {
                xmlWriter.writeStartElement("maxResults");
            }

            if (localMaxResults == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("maxResults cannot be null!!");

            } else {

                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMaxResults));

            }

            xmlWriter.writeEndElement();
        }
        if (localNextTokenTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "nextToken", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "nextToken");
                }

            } else {
                xmlWriter.writeStartElement("nextToken");
            }

            if (localNextToken == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("nextToken cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localNextToken);

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

        if (localStartTimeTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "startTime"));

            if (localStartTime != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localStartTime));
            } else {
                throw new org.apache.axis2.databinding.ADBException("startTime cannot be null!!");
            }
        }
        if (localEndTimeTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "endTime"));

            if (localEndTime != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localEndTime));
            } else {
                throw new org.apache.axis2.databinding.ADBException("endTime cannot be null!!");
            }
        }
        if (localInstanceTypeSetTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceTypeSet"));

            if (localInstanceTypeSet == null) {
                throw new org.apache.axis2.databinding.ADBException("instanceTypeSet cannot be null!!");
            }
            elementList.add(localInstanceTypeSet);
        }
        if (localProductDescriptionSetTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "productDescriptionSet"));

            if (localProductDescriptionSet == null) {
                throw new org.apache.axis2.databinding.ADBException("productDescriptionSet cannot be null!!");
            }
            elementList.add(localProductDescriptionSet);
        }
        if (localFilterSetTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "filterSet"));

            if (localFilterSet == null) {
                throw new org.apache.axis2.databinding.ADBException("filterSet cannot be null!!");
            }
            elementList.add(localFilterSet);
        }
        if (localAvailabilityZoneTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "availabilityZone"));

            if (localAvailabilityZone != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAvailabilityZone));
            } else {
                throw new org.apache.axis2.databinding.ADBException("availabilityZone cannot be null!!");
            }
        }
        if (localMaxResultsTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "maxResults"));

            if (localMaxResults != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMaxResults));
            } else {
                throw new org.apache.axis2.databinding.ADBException("maxResults cannot be null!!");
            }
        }
        if (localNextTokenTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "nextToken"));

            if (localNextToken != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNextToken));
            } else {
                throw new org.apache.axis2.databinding.ADBException("nextToken cannot be null!!");
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
        public static DescribeSpotPriceHistoryType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            DescribeSpotPriceHistoryType object = new DescribeSpotPriceHistoryType();

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

                        if (!"DescribeSpotPriceHistoryType".equals(type)) {
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (DescribeSpotPriceHistoryType)com.amazon.ec2.ExtensionMapper.getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "startTime").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setStartTime(org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "endTime").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setEndTime(org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceTypeSet").equals(reader.getName())) {

                    object.setInstanceTypeSet(com.amazon.ec2.InstanceTypeSetType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() &&
                    new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "productDescriptionSet").equals(reader.getName())) {

                    object.setProductDescriptionSet(com.amazon.ec2.ProductDescriptionSetType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "filterSet").equals(reader.getName())) {

                    object.setFilterSet(com.amazon.ec2.FilterSetType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "availabilityZone").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setAvailabilityZone(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "maxResults").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setMaxResults(org.apache.axis2.databinding.utils.ConverterUtil.convertToInteger(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "nextToken").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setNextToken(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

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
