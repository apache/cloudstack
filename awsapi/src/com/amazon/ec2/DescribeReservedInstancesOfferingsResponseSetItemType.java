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
 * DescribeReservedInstancesOfferingsResponseSetItemType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  DescribeReservedInstancesOfferingsResponseSetItemType bean class
*/

public class DescribeReservedInstancesOfferingsResponseSetItemType implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = DescribeReservedInstancesOfferingsResponseSetItemType
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
    * field for ReservedInstancesOfferingId
    */

    protected java.lang.String localReservedInstancesOfferingId;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getReservedInstancesOfferingId() {
        return localReservedInstancesOfferingId;
    }

    /**
       * Auto generated setter method
       * @param param ReservedInstancesOfferingId
       */
    public void setReservedInstancesOfferingId(java.lang.String param) {

        this.localReservedInstancesOfferingId = param;

    }

    /**
    * field for InstanceType
    */

    protected java.lang.String localInstanceType;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getInstanceType() {
        return localInstanceType;
    }

    /**
       * Auto generated setter method
       * @param param InstanceType
       */
    public void setInstanceType(java.lang.String param) {

        this.localInstanceType = param;

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
    * field for Duration
    */

    protected long localDuration;

    /**
    * Auto generated getter method
    * @return long
    */
    public long getDuration() {
        return localDuration;
    }

    /**
       * Auto generated setter method
       * @param param Duration
       */
    public void setDuration(long param) {

        this.localDuration = param;

    }

    /**
    * field for FixedPrice
    */

    protected double localFixedPrice;

    /**
    * Auto generated getter method
    * @return double
    */
    public double getFixedPrice() {
        return localFixedPrice;
    }

    /**
       * Auto generated setter method
       * @param param FixedPrice
       */
    public void setFixedPrice(double param) {

        this.localFixedPrice = param;

    }

    /**
    * field for UsagePrice
    */

    protected double localUsagePrice;

    /**
    * Auto generated getter method
    * @return double
    */
    public double getUsagePrice() {
        return localUsagePrice;
    }

    /**
       * Auto generated setter method
       * @param param UsagePrice
       */
    public void setUsagePrice(double param) {

        this.localUsagePrice = param;

    }

    /**
    * field for ProductDescription
    */

    protected java.lang.String localProductDescription;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getProductDescription() {
        return localProductDescription;
    }

    /**
       * Auto generated setter method
       * @param param ProductDescription
       */
    public void setProductDescription(java.lang.String param) {

        this.localProductDescription = param;

    }

    /**
    * field for InstanceTenancy
    */

    protected java.lang.String localInstanceTenancy;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getInstanceTenancy() {
        return localInstanceTenancy;
    }

    /**
       * Auto generated setter method
       * @param param InstanceTenancy
       */
    public void setInstanceTenancy(java.lang.String param) {

        this.localInstanceTenancy = param;

    }

    /**
    * field for CurrencyCode
    */

    protected java.lang.String localCurrencyCode;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getCurrencyCode() {
        return localCurrencyCode;
    }

    /**
       * Auto generated setter method
       * @param param CurrencyCode
       */
    public void setCurrencyCode(java.lang.String param) {

        this.localCurrencyCode = param;

    }

    /**
    * field for OfferingType
    */

    protected java.lang.String localOfferingType;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getOfferingType() {
        return localOfferingType;
    }

    /**
       * Auto generated setter method
       * @param param OfferingType
       */
    public void setOfferingType(java.lang.String param) {

        this.localOfferingType = param;

    }

    /**
    * field for RecurringCharges
    */

    protected com.amazon.ec2.RecurringChargesSetType localRecurringCharges;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.RecurringChargesSetType
    */
    public com.amazon.ec2.RecurringChargesSetType getRecurringCharges() {
        return localRecurringCharges;
    }

    /**
       * Auto generated setter method
       * @param param RecurringCharges
       */
    public void setRecurringCharges(com.amazon.ec2.RecurringChargesSetType param) {

        this.localRecurringCharges = param;

    }

    /**
    * field for Marketplace
    */

    protected boolean localMarketplace;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localMarketplaceTracker = false;

    /**
    * Auto generated getter method
    * @return boolean
    */
    public boolean getMarketplace() {
        return localMarketplace;
    }

    /**
       * Auto generated setter method
       * @param param Marketplace
       */
    public void setMarketplace(boolean param) {

        // setting primitive attribute tracker to true

        if (false) {
            localMarketplaceTracker = false;

        } else {
            localMarketplaceTracker = true;
        }

        this.localMarketplace = param;

    }

    /**
    * field for PricingDetailsSet
    */

    protected com.amazon.ec2.PricingDetailsSetType localPricingDetailsSet;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localPricingDetailsSetTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.PricingDetailsSetType
    */
    public com.amazon.ec2.PricingDetailsSetType getPricingDetailsSet() {
        return localPricingDetailsSet;
    }

    /**
       * Auto generated setter method
       * @param param PricingDetailsSet
       */
    public void setPricingDetailsSet(com.amazon.ec2.PricingDetailsSetType param) {

        if (param != null) {
            //update the setting tracker
            localPricingDetailsSetTracker = true;
        } else {
            localPricingDetailsSetTracker = false;

        }

        this.localPricingDetailsSet = param;

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
                DescribeReservedInstancesOfferingsResponseSetItemType.this.serialize(parentQName, factory, xmlWriter);
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
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":DescribeReservedInstancesOfferingsResponseSetItemType",
                    xmlWriter);
            } else {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "DescribeReservedInstancesOfferingsResponseSetItemType", xmlWriter);
            }

        }

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "reservedInstancesOfferingId", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "reservedInstancesOfferingId");
            }

        } else {
            xmlWriter.writeStartElement("reservedInstancesOfferingId");
        }

        if (localReservedInstancesOfferingId == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("reservedInstancesOfferingId cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localReservedInstancesOfferingId);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "instanceType", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "instanceType");
            }

        } else {
            xmlWriter.writeStartElement("instanceType");
        }

        if (localInstanceType == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localInstanceType);

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

                xmlWriter.writeStartElement(prefix, "duration", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "duration");
            }

        } else {
            xmlWriter.writeStartElement("duration");
        }

        if (localDuration == java.lang.Long.MIN_VALUE) {

            throw new org.apache.axis2.databinding.ADBException("duration cannot be null!!");

        } else {
            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDuration));
        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "fixedPrice", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "fixedPrice");
            }

        } else {
            xmlWriter.writeStartElement("fixedPrice");
        }

        if (java.lang.Double.isNaN(localFixedPrice)) {

            throw new org.apache.axis2.databinding.ADBException("fixedPrice cannot be null!!");

        } else {
            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localFixedPrice));
        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "usagePrice", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "usagePrice");
            }

        } else {
            xmlWriter.writeStartElement("usagePrice");
        }

        if (java.lang.Double.isNaN(localUsagePrice)) {

            throw new org.apache.axis2.databinding.ADBException("usagePrice cannot be null!!");

        } else {
            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUsagePrice));
        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "productDescription", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "productDescription");
            }

        } else {
            xmlWriter.writeStartElement("productDescription");
        }

        if (localProductDescription == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("productDescription cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localProductDescription);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "instanceTenancy", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "instanceTenancy");
            }

        } else {
            xmlWriter.writeStartElement("instanceTenancy");
        }

        if (localInstanceTenancy == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("instanceTenancy cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localInstanceTenancy);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "currencyCode", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "currencyCode");
            }

        } else {
            xmlWriter.writeStartElement("currencyCode");
        }

        if (localCurrencyCode == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("currencyCode cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localCurrencyCode);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "offeringType", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "offeringType");
            }

        } else {
            xmlWriter.writeStartElement("offeringType");
        }

        if (localOfferingType == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("offeringType cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localOfferingType);

        }

        xmlWriter.writeEndElement();

        if (localRecurringCharges == null) {
            throw new org.apache.axis2.databinding.ADBException("recurringCharges cannot be null!!");
        }
        localRecurringCharges.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "recurringCharges"), factory, xmlWriter);
        if (localMarketplaceTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "marketplace", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "marketplace");
                }

            } else {
                xmlWriter.writeStartElement("marketplace");
            }

            if (false) {

                throw new org.apache.axis2.databinding.ADBException("marketplace cannot be null!!");

            } else {
                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMarketplace));
            }

            xmlWriter.writeEndElement();
        }
        if (localPricingDetailsSetTracker) {
            if (localPricingDetailsSet == null) {
                throw new org.apache.axis2.databinding.ADBException("pricingDetailsSet cannot be null!!");
            }
            localPricingDetailsSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "pricingDetailsSet"), factory, xmlWriter);
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

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "reservedInstancesOfferingId"));

        if (localReservedInstancesOfferingId != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localReservedInstancesOfferingId));
        } else {
            throw new org.apache.axis2.databinding.ADBException("reservedInstancesOfferingId cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceType"));

        if (localInstanceType != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInstanceType));
        } else {
            throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "availabilityZone"));

        if (localAvailabilityZone != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAvailabilityZone));
        } else {
            throw new org.apache.axis2.databinding.ADBException("availabilityZone cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "duration"));

        elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDuration));

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "fixedPrice"));

        elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localFixedPrice));

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "usagePrice"));

        elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUsagePrice));

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "productDescription"));

        if (localProductDescription != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localProductDescription));
        } else {
            throw new org.apache.axis2.databinding.ADBException("productDescription cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceTenancy"));

        if (localInstanceTenancy != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInstanceTenancy));
        } else {
            throw new org.apache.axis2.databinding.ADBException("instanceTenancy cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "currencyCode"));

        if (localCurrencyCode != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCurrencyCode));
        } else {
            throw new org.apache.axis2.databinding.ADBException("currencyCode cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "offeringType"));

        if (localOfferingType != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localOfferingType));
        } else {
            throw new org.apache.axis2.databinding.ADBException("offeringType cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "recurringCharges"));

        if (localRecurringCharges == null) {
            throw new org.apache.axis2.databinding.ADBException("recurringCharges cannot be null!!");
        }
        elementList.add(localRecurringCharges);
        if (localMarketplaceTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "marketplace"));

            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMarketplace));
        }
        if (localPricingDetailsSetTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "pricingDetailsSet"));

            if (localPricingDetailsSet == null) {
                throw new org.apache.axis2.databinding.ADBException("pricingDetailsSet cannot be null!!");
            }
            elementList.add(localPricingDetailsSet);
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
        public static DescribeReservedInstancesOfferingsResponseSetItemType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            DescribeReservedInstancesOfferingsResponseSetItemType object = new DescribeReservedInstancesOfferingsResponseSetItemType();

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

                        if (!"DescribeReservedInstancesOfferingsResponseSetItemType".equals(type)) {
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (DescribeReservedInstancesOfferingsResponseSetItemType)com.amazon.ec2.ExtensionMapper.getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() &&
                    new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "reservedInstancesOfferingId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setReservedInstancesOfferingId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceType").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setInstanceType(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

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

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "duration").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setDuration(org.apache.axis2.databinding.utils.ConverterUtil.convertToLong(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "fixedPrice").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setFixedPrice(org.apache.axis2.databinding.utils.ConverterUtil.convertToDouble(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "usagePrice").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setUsagePrice(org.apache.axis2.databinding.utils.ConverterUtil.convertToDouble(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "productDescription").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setProductDescription(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "instanceTenancy").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setInstanceTenancy(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "currencyCode").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setCurrencyCode(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "offeringType").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setOfferingType(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "recurringCharges").equals(reader.getName())) {

                    object.setRecurringCharges(com.amazon.ec2.RecurringChargesSetType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "marketplace").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setMarketplace(org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "pricingDetailsSet").equals(reader.getName())) {

                    object.setPricingDetailsSet(com.amazon.ec2.PricingDetailsSetType.Factory.parse(reader));

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
