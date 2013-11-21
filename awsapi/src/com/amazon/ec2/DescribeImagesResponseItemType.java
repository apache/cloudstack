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
 * DescribeImagesResponseItemType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  DescribeImagesResponseItemType bean class
*/

public class DescribeImagesResponseItemType implements org.apache.axis2.databinding.ADBBean {
    /* This type was generated from the piece of schema that had
            name = DescribeImagesResponseItemType
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
    * field for ImageId
    */

    protected java.lang.String localImageId;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getImageId() {
        return localImageId;
    }

    /**
       * Auto generated setter method
       * @param param ImageId
       */
    public void setImageId(java.lang.String param) {

        this.localImageId = param;

    }

    /**
    * field for ImageLocation
    */

    protected java.lang.String localImageLocation;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localImageLocationTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getImageLocation() {
        return localImageLocation;
    }

    /**
       * Auto generated setter method
       * @param param ImageLocation
       */
    public void setImageLocation(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localImageLocationTracker = true;
        } else {
            localImageLocationTracker = false;

        }

        this.localImageLocation = param;

    }

    /**
    * field for ImageState
    */

    protected java.lang.String localImageState;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getImageState() {
        return localImageState;
    }

    /**
       * Auto generated setter method
       * @param param ImageState
       */
    public void setImageState(java.lang.String param) {

        this.localImageState = param;

    }

    /**
    * field for ImageOwnerId
    */

    protected java.lang.String localImageOwnerId;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getImageOwnerId() {
        return localImageOwnerId;
    }

    /**
       * Auto generated setter method
       * @param param ImageOwnerId
       */
    public void setImageOwnerId(java.lang.String param) {

        this.localImageOwnerId = param;

    }

    /**
    * field for IsPublic
    */

    protected boolean localIsPublic;

    /**
    * Auto generated getter method
    * @return boolean
    */
    public boolean getIsPublic() {
        return localIsPublic;
    }

    /**
       * Auto generated setter method
       * @param param IsPublic
       */
    public void setIsPublic(boolean param) {

        this.localIsPublic = param;

    }

    /**
    * field for ProductCodes
    */

    protected com.amazon.ec2.ProductCodesSetType localProductCodes;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localProductCodesTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.ProductCodesSetType
    */
    public com.amazon.ec2.ProductCodesSetType getProductCodes() {
        return localProductCodes;
    }

    /**
       * Auto generated setter method
       * @param param ProductCodes
       */
    public void setProductCodes(com.amazon.ec2.ProductCodesSetType param) {

        if (param != null) {
            //update the setting tracker
            localProductCodesTracker = true;
        } else {
            localProductCodesTracker = false;

        }

        this.localProductCodes = param;

    }

    /**
    * field for Architecture
    */

    protected java.lang.String localArchitecture;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localArchitectureTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getArchitecture() {
        return localArchitecture;
    }

    /**
       * Auto generated setter method
       * @param param Architecture
       */
    public void setArchitecture(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localArchitectureTracker = true;
        } else {
            localArchitectureTracker = false;

        }

        this.localArchitecture = param;

    }

    /**
    * field for ImageType
    */

    protected java.lang.String localImageType;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localImageTypeTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getImageType() {
        return localImageType;
    }

    /**
       * Auto generated setter method
       * @param param ImageType
       */
    public void setImageType(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localImageTypeTracker = true;
        } else {
            localImageTypeTracker = false;

        }

        this.localImageType = param;

    }

    /**
    * field for KernelId
    */

    protected java.lang.String localKernelId;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localKernelIdTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getKernelId() {
        return localKernelId;
    }

    /**
       * Auto generated setter method
       * @param param KernelId
       */
    public void setKernelId(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localKernelIdTracker = true;
        } else {
            localKernelIdTracker = false;

        }

        this.localKernelId = param;

    }

    /**
    * field for RamdiskId
    */

    protected java.lang.String localRamdiskId;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localRamdiskIdTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getRamdiskId() {
        return localRamdiskId;
    }

    /**
       * Auto generated setter method
       * @param param RamdiskId
       */
    public void setRamdiskId(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localRamdiskIdTracker = true;
        } else {
            localRamdiskIdTracker = false;

        }

        this.localRamdiskId = param;

    }

    /**
    * field for Platform
    */

    protected java.lang.String localPlatform;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localPlatformTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getPlatform() {
        return localPlatform;
    }

    /**
       * Auto generated setter method
       * @param param Platform
       */
    public void setPlatform(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localPlatformTracker = true;
        } else {
            localPlatformTracker = false;

        }

        this.localPlatform = param;

    }

    /**
    * field for StateReason
    */

    protected com.amazon.ec2.StateReasonType localStateReason;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localStateReasonTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.StateReasonType
    */
    public com.amazon.ec2.StateReasonType getStateReason() {
        return localStateReason;
    }

    /**
       * Auto generated setter method
       * @param param StateReason
       */
    public void setStateReason(com.amazon.ec2.StateReasonType param) {

        if (param != null) {
            //update the setting tracker
            localStateReasonTracker = true;
        } else {
            localStateReasonTracker = false;

        }

        this.localStateReason = param;

    }

    /**
    * field for ImageOwnerAlias
    */

    protected java.lang.String localImageOwnerAlias;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localImageOwnerAliasTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getImageOwnerAlias() {
        return localImageOwnerAlias;
    }

    /**
       * Auto generated setter method
       * @param param ImageOwnerAlias
       */
    public void setImageOwnerAlias(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localImageOwnerAliasTracker = true;
        } else {
            localImageOwnerAliasTracker = false;

        }

        this.localImageOwnerAlias = param;

    }

    /**
    * field for Name
    */

    protected java.lang.String localName;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localNameTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getName() {
        return localName;
    }

    /**
       * Auto generated setter method
       * @param param Name
       */
    public void setName(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localNameTracker = true;
        } else {
            localNameTracker = false;

        }

        this.localName = param;

    }

    /**
    * field for Description
    */

    protected java.lang.String localDescription;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localDescriptionTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getDescription() {
        return localDescription;
    }

    /**
       * Auto generated setter method
       * @param param Description
       */
    public void setDescription(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localDescriptionTracker = true;
        } else {
            localDescriptionTracker = false;

        }

        this.localDescription = param;

    }

    /**
    * field for RootDeviceType
    */

    protected java.lang.String localRootDeviceType;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localRootDeviceTypeTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getRootDeviceType() {
        return localRootDeviceType;
    }

    /**
       * Auto generated setter method
       * @param param RootDeviceType
       */
    public void setRootDeviceType(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localRootDeviceTypeTracker = true;
        } else {
            localRootDeviceTypeTracker = false;

        }

        this.localRootDeviceType = param;

    }

    /**
    * field for RootDeviceName
    */

    protected java.lang.String localRootDeviceName;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localRootDeviceNameTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getRootDeviceName() {
        return localRootDeviceName;
    }

    /**
       * Auto generated setter method
       * @param param RootDeviceName
       */
    public void setRootDeviceName(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localRootDeviceNameTracker = true;
        } else {
            localRootDeviceNameTracker = false;

        }

        this.localRootDeviceName = param;

    }

    /**
    * field for BlockDeviceMapping
    */

    protected com.amazon.ec2.BlockDeviceMappingType localBlockDeviceMapping;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localBlockDeviceMappingTracker = false;

    /**
    * Auto generated getter method
    * @return com.amazon.ec2.BlockDeviceMappingType
    */
    public com.amazon.ec2.BlockDeviceMappingType getBlockDeviceMapping() {
        return localBlockDeviceMapping;
    }

    /**
       * Auto generated setter method
       * @param param BlockDeviceMapping
       */
    public void setBlockDeviceMapping(com.amazon.ec2.BlockDeviceMappingType param) {

        if (param != null) {
            //update the setting tracker
            localBlockDeviceMappingTracker = true;
        } else {
            localBlockDeviceMappingTracker = false;

        }

        this.localBlockDeviceMapping = param;

    }

    /**
    * field for VirtualizationType
    */

    protected java.lang.String localVirtualizationType;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localVirtualizationTypeTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getVirtualizationType() {
        return localVirtualizationType;
    }

    /**
       * Auto generated setter method
       * @param param VirtualizationType
       */
    public void setVirtualizationType(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localVirtualizationTypeTracker = true;
        } else {
            localVirtualizationTypeTracker = false;

        }

        this.localVirtualizationType = param;

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
    * field for Hypervisor
    */

    protected java.lang.String localHypervisor;

    /*  This tracker boolean wil be used to detect whether the user called the set method
    *   for this attribute. It will be used to determine whether to include this field
    *   in the serialized XML
    */
    protected boolean localHypervisorTracker = false;

    /**
    * Auto generated getter method
    * @return java.lang.String
    */
    public java.lang.String getHypervisor() {
        return localHypervisor;
    }

    /**
       * Auto generated setter method
       * @param param Hypervisor
       */
    public void setHypervisor(java.lang.String param) {

        if (param != null) {
            //update the setting tracker
            localHypervisorTracker = true;
        } else {
            localHypervisorTracker = false;

        }

        this.localHypervisor = param;

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
                DescribeImagesResponseItemType.this.serialize(parentQName, factory, xmlWriter);
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
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", namespacePrefix + ":DescribeImagesResponseItemType", xmlWriter);
            } else {
                writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "type", "DescribeImagesResponseItemType", xmlWriter);
            }

        }

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "imageId", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "imageId");
            }

        } else {
            xmlWriter.writeStartElement("imageId");
        }

        if (localImageId == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("imageId cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localImageId);

        }

        xmlWriter.writeEndElement();
        if (localImageLocationTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "imageLocation", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "imageLocation");
                }

            } else {
                xmlWriter.writeStartElement("imageLocation");
            }

            if (localImageLocation == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("imageLocation cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localImageLocation);

            }

            xmlWriter.writeEndElement();
        }
        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "imageState", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "imageState");
            }

        } else {
            xmlWriter.writeStartElement("imageState");
        }

        if (localImageState == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("imageState cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localImageState);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "imageOwnerId", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "imageOwnerId");
            }

        } else {
            xmlWriter.writeStartElement("imageOwnerId");
        }

        if (localImageOwnerId == null) {
            // write the nil attribute

            throw new org.apache.axis2.databinding.ADBException("imageOwnerId cannot be null!!");

        } else {

            xmlWriter.writeCharacters(localImageOwnerId);

        }

        xmlWriter.writeEndElement();

        namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
        if (!namespace.equals("")) {
            prefix = xmlWriter.getPrefix(namespace);

            if (prefix == null) {
                prefix = generatePrefix(namespace);

                xmlWriter.writeStartElement(prefix, "isPublic", namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);

            } else {
                xmlWriter.writeStartElement(namespace, "isPublic");
            }

        } else {
            xmlWriter.writeStartElement("isPublic");
        }

        if (false) {

            throw new org.apache.axis2.databinding.ADBException("isPublic cannot be null!!");

        } else {
            xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIsPublic));
        }

        xmlWriter.writeEndElement();
        if (localProductCodesTracker) {
            if (localProductCodes == null) {
                throw new org.apache.axis2.databinding.ADBException("productCodes cannot be null!!");
            }
            localProductCodes.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "productCodes"), factory, xmlWriter);
        }
        if (localArchitectureTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "architecture", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "architecture");
                }

            } else {
                xmlWriter.writeStartElement("architecture");
            }

            if (localArchitecture == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("architecture cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localArchitecture);

            }

            xmlWriter.writeEndElement();
        }
        if (localImageTypeTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "imageType", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "imageType");
                }

            } else {
                xmlWriter.writeStartElement("imageType");
            }

            if (localImageType == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("imageType cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localImageType);

            }

            xmlWriter.writeEndElement();
        }
        if (localKernelIdTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "kernelId", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "kernelId");
                }

            } else {
                xmlWriter.writeStartElement("kernelId");
            }

            if (localKernelId == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("kernelId cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localKernelId);

            }

            xmlWriter.writeEndElement();
        }
        if (localRamdiskIdTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "ramdiskId", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "ramdiskId");
                }

            } else {
                xmlWriter.writeStartElement("ramdiskId");
            }

            if (localRamdiskId == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("ramdiskId cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localRamdiskId);

            }

            xmlWriter.writeEndElement();
        }
        if (localPlatformTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "platform", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "platform");
                }

            } else {
                xmlWriter.writeStartElement("platform");
            }

            if (localPlatform == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("platform cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localPlatform);

            }

            xmlWriter.writeEndElement();
        }
        if (localStateReasonTracker) {
            if (localStateReason == null) {
                throw new org.apache.axis2.databinding.ADBException("stateReason cannot be null!!");
            }
            localStateReason.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "stateReason"), factory, xmlWriter);
        }
        if (localImageOwnerAliasTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "imageOwnerAlias", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "imageOwnerAlias");
                }

            } else {
                xmlWriter.writeStartElement("imageOwnerAlias");
            }

            if (localImageOwnerAlias == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("imageOwnerAlias cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localImageOwnerAlias);

            }

            xmlWriter.writeEndElement();
        }
        if (localNameTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "name", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "name");
                }

            } else {
                xmlWriter.writeStartElement("name");
            }

            if (localName == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("name cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localName);

            }

            xmlWriter.writeEndElement();
        }
        if (localDescriptionTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "description", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "description");
                }

            } else {
                xmlWriter.writeStartElement("description");
            }

            if (localDescription == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("description cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localDescription);

            }

            xmlWriter.writeEndElement();
        }
        if (localRootDeviceTypeTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "rootDeviceType", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "rootDeviceType");
                }

            } else {
                xmlWriter.writeStartElement("rootDeviceType");
            }

            if (localRootDeviceType == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("rootDeviceType cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localRootDeviceType);

            }

            xmlWriter.writeEndElement();
        }
        if (localRootDeviceNameTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "rootDeviceName", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "rootDeviceName");
                }

            } else {
                xmlWriter.writeStartElement("rootDeviceName");
            }

            if (localRootDeviceName == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("rootDeviceName cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localRootDeviceName);

            }

            xmlWriter.writeEndElement();
        }
        if (localBlockDeviceMappingTracker) {
            if (localBlockDeviceMapping == null) {
                throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
            }
            localBlockDeviceMapping.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "blockDeviceMapping"), factory, xmlWriter);
        }
        if (localVirtualizationTypeTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "virtualizationType", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "virtualizationType");
                }

            } else {
                xmlWriter.writeStartElement("virtualizationType");
            }

            if (localVirtualizationType == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("virtualizationType cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localVirtualizationType);

            }

            xmlWriter.writeEndElement();
        }
        if (localTagSetTracker) {
            if (localTagSet == null) {
                throw new org.apache.axis2.databinding.ADBException("tagSet cannot be null!!");
            }
            localTagSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "tagSet"), factory, xmlWriter);
        }
        if (localHypervisorTracker) {
            namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
            if (!namespace.equals("")) {
                prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    xmlWriter.writeStartElement(prefix, "hypervisor", namespace);
                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);

                } else {
                    xmlWriter.writeStartElement(namespace, "hypervisor");
                }

            } else {
                xmlWriter.writeStartElement("hypervisor");
            }

            if (localHypervisor == null) {
                // write the nil attribute

                throw new org.apache.axis2.databinding.ADBException("hypervisor cannot be null!!");

            } else {

                xmlWriter.writeCharacters(localHypervisor);

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

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageId"));

        if (localImageId != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localImageId));
        } else {
            throw new org.apache.axis2.databinding.ADBException("imageId cannot be null!!");
        }
        if (localImageLocationTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageLocation"));

            if (localImageLocation != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localImageLocation));
            } else {
                throw new org.apache.axis2.databinding.ADBException("imageLocation cannot be null!!");
            }
        }
        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageState"));

        if (localImageState != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localImageState));
        } else {
            throw new org.apache.axis2.databinding.ADBException("imageState cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageOwnerId"));

        if (localImageOwnerId != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localImageOwnerId));
        } else {
            throw new org.apache.axis2.databinding.ADBException("imageOwnerId cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "isPublic"));

        elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIsPublic));
        if (localProductCodesTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "productCodes"));

            if (localProductCodes == null) {
                throw new org.apache.axis2.databinding.ADBException("productCodes cannot be null!!");
            }
            elementList.add(localProductCodes);
        }
        if (localArchitectureTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "architecture"));

            if (localArchitecture != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localArchitecture));
            } else {
                throw new org.apache.axis2.databinding.ADBException("architecture cannot be null!!");
            }
        }
        if (localImageTypeTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageType"));

            if (localImageType != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localImageType));
            } else {
                throw new org.apache.axis2.databinding.ADBException("imageType cannot be null!!");
            }
        }
        if (localKernelIdTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "kernelId"));

            if (localKernelId != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localKernelId));
            } else {
                throw new org.apache.axis2.databinding.ADBException("kernelId cannot be null!!");
            }
        }
        if (localRamdiskIdTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "ramdiskId"));

            if (localRamdiskId != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRamdiskId));
            } else {
                throw new org.apache.axis2.databinding.ADBException("ramdiskId cannot be null!!");
            }
        }
        if (localPlatformTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "platform"));

            if (localPlatform != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPlatform));
            } else {
                throw new org.apache.axis2.databinding.ADBException("platform cannot be null!!");
            }
        }
        if (localStateReasonTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "stateReason"));

            if (localStateReason == null) {
                throw new org.apache.axis2.databinding.ADBException("stateReason cannot be null!!");
            }
            elementList.add(localStateReason);
        }
        if (localImageOwnerAliasTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageOwnerAlias"));

            if (localImageOwnerAlias != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localImageOwnerAlias));
            } else {
                throw new org.apache.axis2.databinding.ADBException("imageOwnerAlias cannot be null!!");
            }
        }
        if (localNameTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "name"));

            if (localName != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localName));
            } else {
                throw new org.apache.axis2.databinding.ADBException("name cannot be null!!");
            }
        }
        if (localDescriptionTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "description"));

            if (localDescription != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDescription));
            } else {
                throw new org.apache.axis2.databinding.ADBException("description cannot be null!!");
            }
        }
        if (localRootDeviceTypeTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "rootDeviceType"));

            if (localRootDeviceType != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRootDeviceType));
            } else {
                throw new org.apache.axis2.databinding.ADBException("rootDeviceType cannot be null!!");
            }
        }
        if (localRootDeviceNameTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "rootDeviceName"));

            if (localRootDeviceName != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRootDeviceName));
            } else {
                throw new org.apache.axis2.databinding.ADBException("rootDeviceName cannot be null!!");
            }
        }
        if (localBlockDeviceMappingTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "blockDeviceMapping"));

            if (localBlockDeviceMapping == null) {
                throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
            }
            elementList.add(localBlockDeviceMapping);
        }
        if (localVirtualizationTypeTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "virtualizationType"));

            if (localVirtualizationType != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localVirtualizationType));
            } else {
                throw new org.apache.axis2.databinding.ADBException("virtualizationType cannot be null!!");
            }
        }
        if (localTagSetTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "tagSet"));

            if (localTagSet == null) {
                throw new org.apache.axis2.databinding.ADBException("tagSet cannot be null!!");
            }
            elementList.add(localTagSet);
        }
        if (localHypervisorTracker) {
            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "hypervisor"));

            if (localHypervisor != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localHypervisor));
            } else {
                throw new org.apache.axis2.databinding.ADBException("hypervisor cannot be null!!");
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
        public static DescribeImagesResponseItemType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            DescribeImagesResponseItemType object = new DescribeImagesResponseItemType();

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

                        if (!"DescribeImagesResponseItemType".equals(type)) {
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (DescribeImagesResponseItemType)com.amazon.ec2.ExtensionMapper.getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setImageId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageLocation").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setImageLocation(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageState").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setImageState(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageOwnerId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setImageOwnerId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "isPublic").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setIsPublic(org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));

                    reader.next();

                }  // End of if for expected property start element

                else {
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "productCodes").equals(reader.getName())) {

                    object.setProductCodes(com.amazon.ec2.ProductCodesSetType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "architecture").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setArchitecture(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageType").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setImageType(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "kernelId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setKernelId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "ramdiskId").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setRamdiskId(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "platform").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setPlatform(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "stateReason").equals(reader.getName())) {

                    object.setStateReason(com.amazon.ec2.StateReasonType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "imageOwnerAlias").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setImageOwnerAlias(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "name").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setName(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "description").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setDescription(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "rootDeviceType").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setRootDeviceType(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "rootDeviceName").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setRootDeviceName(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "blockDeviceMapping").equals(reader.getName())) {

                    object.setBlockDeviceMapping(com.amazon.ec2.BlockDeviceMappingType.Factory.parse(reader));

                    reader.next();

                }  // End of if for expected property start element

                else {

                }

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "virtualizationType").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setVirtualizationType(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else {

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

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/", "hypervisor").equals(reader.getName())) {

                    java.lang.String content = reader.getElementText();

                    object.setHypervisor(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

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
