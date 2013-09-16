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
 * DescribeImageAttributesGroup.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */
            
                package com.amazon.ec2;
            

            /**
            *  DescribeImageAttributesGroup bean class
            */
        
        public  class DescribeImageAttributesGroup
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = DescribeImageAttributesGroup
                Namespace URI = http://ec2.amazonaws.com/doc/2012-08-15/
                Namespace Prefix = ns1
                */
            

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://ec2.amazonaws.com/doc/2012-08-15/")){
                return "ns1";
            }
            return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
        }

        
            /** Whenever a new property is set ensure all others are unset
             *  There can be only one choice and the last one wins
             */
            private void clearAllSettingTrackers() {
            
                   localLaunchPermissionTracker = false;
                
                   localProductCodesTracker = false;
                
                   localKernelTracker = false;
                
                   localRamdiskTracker = false;
                
                   localBlockDeviceMappingTracker = false;
                
                   localDescriptionTracker = false;
                
                   localInstanceTypeCategoryTracker = false;
                
            }
        

                        /**
                        * field for LaunchPermission
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localLaunchPermission ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localLaunchPermissionTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getLaunchPermission(){
                               return localLaunchPermission;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param LaunchPermission
                               */
                               public void setLaunchPermission(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localLaunchPermissionTracker = true;
                                       } else {
                                          localLaunchPermissionTracker = false;
                                              
                                       }
                                   
                                            this.localLaunchPermission=param;
                                    

                               }
                            

                        /**
                        * field for ProductCodes
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localProductCodes ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localProductCodesTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getProductCodes(){
                               return localProductCodes;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ProductCodes
                               */
                               public void setProductCodes(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localProductCodesTracker = true;
                                       } else {
                                          localProductCodesTracker = false;
                                              
                                       }
                                   
                                            this.localProductCodes=param;
                                    

                               }
                            

                        /**
                        * field for Kernel
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localKernel ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localKernelTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getKernel(){
                               return localKernel;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Kernel
                               */
                               public void setKernel(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localKernelTracker = true;
                                       } else {
                                          localKernelTracker = false;
                                              
                                       }
                                   
                                            this.localKernel=param;
                                    

                               }
                            

                        /**
                        * field for Ramdisk
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localRamdisk ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localRamdiskTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getRamdisk(){
                               return localRamdisk;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Ramdisk
                               */
                               public void setRamdisk(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localRamdiskTracker = true;
                                       } else {
                                          localRamdiskTracker = false;
                                              
                                       }
                                   
                                            this.localRamdisk=param;
                                    

                               }
                            

                        /**
                        * field for BlockDeviceMapping
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localBlockDeviceMapping ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localBlockDeviceMappingTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getBlockDeviceMapping(){
                               return localBlockDeviceMapping;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param BlockDeviceMapping
                               */
                               public void setBlockDeviceMapping(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localBlockDeviceMappingTracker = true;
                                       } else {
                                          localBlockDeviceMappingTracker = false;
                                              
                                       }
                                   
                                            this.localBlockDeviceMapping=param;
                                    

                               }
                            

                        /**
                        * field for Description
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localDescription ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDescriptionTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getDescription(){
                               return localDescription;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Description
                               */
                               public void setDescription(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localDescriptionTracker = true;
                                       } else {
                                          localDescriptionTracker = false;
                                              
                                       }
                                   
                                            this.localDescription=param;
                                    

                               }
                            

                        /**
                        * field for InstanceTypeCategory
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localInstanceTypeCategory ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localInstanceTypeCategoryTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getInstanceTypeCategory(){
                               return localInstanceTypeCategory;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param InstanceTypeCategory
                               */
                               public void setInstanceTypeCategory(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localInstanceTypeCategoryTracker = true;
                                       } else {
                                          localInstanceTypeCategoryTracker = false;
                                              
                                       }
                                   
                                            this.localInstanceTypeCategory=param;
                                    

                               }
                            

     /**
     * isReaderMTOMAware
     * @return true if the reader supports MTOM
     */
   public static boolean isReaderMTOMAware(javax.xml.stream.XMLStreamReader reader) {
        boolean isReaderMTOMAware = false;
        
        try{
          isReaderMTOMAware = java.lang.Boolean.TRUE.equals(reader.getProperty(org.apache.axiom.om.OMConstants.IS_DATA_HANDLERS_AWARE));
        }catch(java.lang.IllegalArgumentException e){
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
       public org.apache.axiom.om.OMElement getOMElement (
               final javax.xml.namespace.QName parentQName,
               final org.apache.axiom.om.OMFactory factory) throws org.apache.axis2.databinding.ADBException{


        
               org.apache.axiom.om.OMDataSource dataSource =
                       new org.apache.axis2.databinding.ADBDataSource(this,parentQName){

                 public void serialize(org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
                       DescribeImageAttributesGroup.this.serialize(parentQName,factory,xmlWriter);
                 }
               };
               return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(
               parentQName,factory,dataSource);
            
       }

         public void serialize(final javax.xml.namespace.QName parentQName,
                                       final org.apache.axiom.om.OMFactory factory,
                                       org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter)
                                throws javax.xml.stream.XMLStreamException, org.apache.axis2.databinding.ADBException{
                           serialize(parentQName,factory,xmlWriter,false);
         }

         public void serialize(final javax.xml.namespace.QName parentQName,
                               final org.apache.axiom.om.OMFactory factory,
                               org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter,
                               boolean serializeType)
            throws javax.xml.stream.XMLStreamException, org.apache.axis2.databinding.ADBException{
            
                


                java.lang.String prefix = null;
                java.lang.String namespace = null;
                
                  if (serializeType){
               

                   java.lang.String namespacePrefix = registerPrefix(xmlWriter,"http://ec2.amazonaws.com/doc/2012-08-15/");
                   if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)){
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           namespacePrefix+":DescribeImageAttributesGroup",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "DescribeImageAttributesGroup",
                           xmlWriter);
                   }

               
                   }
                if (localLaunchPermissionTracker){
                                            if (localLaunchPermission==null){
                                                 throw new org.apache.axis2.databinding.ADBException("launchPermission cannot be null!!");
                                            }
                                           localLaunchPermission.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","launchPermission"),
                                               factory,xmlWriter);
                                        } if (localProductCodesTracker){
                                            if (localProductCodes==null){
                                                 throw new org.apache.axis2.databinding.ADBException("productCodes cannot be null!!");
                                            }
                                           localProductCodes.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","productCodes"),
                                               factory,xmlWriter);
                                        } if (localKernelTracker){
                                            if (localKernel==null){
                                                 throw new org.apache.axis2.databinding.ADBException("kernel cannot be null!!");
                                            }
                                           localKernel.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","kernel"),
                                               factory,xmlWriter);
                                        } if (localRamdiskTracker){
                                            if (localRamdisk==null){
                                                 throw new org.apache.axis2.databinding.ADBException("ramdisk cannot be null!!");
                                            }
                                           localRamdisk.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","ramdisk"),
                                               factory,xmlWriter);
                                        } if (localBlockDeviceMappingTracker){
                                            if (localBlockDeviceMapping==null){
                                                 throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
                                            }
                                           localBlockDeviceMapping.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","blockDeviceMapping"),
                                               factory,xmlWriter);
                                        } if (localDescriptionTracker){
                                            if (localDescription==null){
                                                 throw new org.apache.axis2.databinding.ADBException("description cannot be null!!");
                                            }
                                           localDescription.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","description"),
                                               factory,xmlWriter);
                                        } if (localInstanceTypeCategoryTracker){
                                            if (localInstanceTypeCategory==null){
                                                 throw new org.apache.axis2.databinding.ADBException("instanceTypeCategory cannot be null!!");
                                            }
                                           localInstanceTypeCategory.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","instanceTypeCategory"),
                                               factory,xmlWriter);
                                        }

        }

         /**
          * Util method to write an attribute with the ns prefix
          */
          private void writeAttribute(java.lang.String prefix,java.lang.String namespace,java.lang.String attName,
                                      java.lang.String attValue,javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException{
              if (xmlWriter.getPrefix(namespace) == null) {
                       xmlWriter.writeNamespace(prefix, namespace);
                       xmlWriter.setPrefix(prefix, namespace);

              }

              xmlWriter.writeAttribute(namespace,attName,attValue);

         }

        /**
          * Util method to write an attribute without the ns prefix
          */
          private void writeAttribute(java.lang.String namespace,java.lang.String attName,
                                      java.lang.String attValue,javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException{
                if (namespace.equals(""))
              {
                  xmlWriter.writeAttribute(attName,attValue);
              }
              else
              {
                  registerPrefix(xmlWriter, namespace);
                  xmlWriter.writeAttribute(namespace,attName,attValue);
              }
          }


           /**
             * Util method to write an attribute without the ns prefix
             */
            private void writeQNameAttribute(java.lang.String namespace, java.lang.String attName,
                                             javax.xml.namespace.QName qname, javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {

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

        private void writeQName(javax.xml.namespace.QName qname,
                                javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
            java.lang.String namespaceURI = qname.getNamespaceURI();
            if (namespaceURI != null) {
                java.lang.String prefix = xmlWriter.getPrefix(namespaceURI);
                if (prefix == null) {
                    prefix = generatePrefix(namespaceURI);
                    xmlWriter.writeNamespace(prefix, namespaceURI);
                    xmlWriter.setPrefix(prefix,namespaceURI);
                }

                if (prefix.trim().length() > 0){
                    xmlWriter.writeCharacters(prefix + ":" + org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
                } else {
                    // i.e this is the default namespace
                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
                }

            } else {
                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
            }
        }

        private void writeQNames(javax.xml.namespace.QName[] qnames,
                                 javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {

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
                            xmlWriter.setPrefix(prefix,namespaceURI);
                        }

                        if (prefix.trim().length() > 0){
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
        public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName)
                    throws org.apache.axis2.databinding.ADBException{


        
                 java.util.ArrayList elementList = new java.util.ArrayList();
                 java.util.ArrayList attribList = new java.util.ArrayList();

                 if (localLaunchPermissionTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "launchPermission"));
                            
                            
                                    if (localLaunchPermission==null){
                                         throw new org.apache.axis2.databinding.ADBException("launchPermission cannot be null!!");
                                    }
                                    elementList.add(localLaunchPermission);
                                } if (localProductCodesTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "productCodes"));
                            
                            
                                    if (localProductCodes==null){
                                         throw new org.apache.axis2.databinding.ADBException("productCodes cannot be null!!");
                                    }
                                    elementList.add(localProductCodes);
                                } if (localKernelTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "kernel"));
                            
                            
                                    if (localKernel==null){
                                         throw new org.apache.axis2.databinding.ADBException("kernel cannot be null!!");
                                    }
                                    elementList.add(localKernel);
                                } if (localRamdiskTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "ramdisk"));
                            
                            
                                    if (localRamdisk==null){
                                         throw new org.apache.axis2.databinding.ADBException("ramdisk cannot be null!!");
                                    }
                                    elementList.add(localRamdisk);
                                } if (localBlockDeviceMappingTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "blockDeviceMapping"));
                            
                            
                                    if (localBlockDeviceMapping==null){
                                         throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
                                    }
                                    elementList.add(localBlockDeviceMapping);
                                } if (localDescriptionTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "description"));
                            
                            
                                    if (localDescription==null){
                                         throw new org.apache.axis2.databinding.ADBException("description cannot be null!!");
                                    }
                                    elementList.add(localDescription);
                                } if (localInstanceTypeCategoryTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "instanceTypeCategory"));
                            
                            
                                    if (localInstanceTypeCategory==null){
                                         throw new org.apache.axis2.databinding.ADBException("instanceTypeCategory cannot be null!!");
                                    }
                                    elementList.add(localInstanceTypeCategory);
                                }

                return new org.apache.axis2.databinding.utils.reader.ADBXMLStreamReaderImpl(qName, elementList.toArray(), attribList.toArray());
            
            

        }

  

     /**
      *  Factory class that keeps the parse method
      */
    public static class Factory{

        
        

        /**
        * static method to create the object
        * Precondition:  If this object is an element, the current or next start element starts this object and any intervening reader events are ignorable
        *                If this object is not an element, it is a complex type and the reader is at the event just after the outer start element
        * Postcondition: If this object is an element, the reader is positioned at its end element
        *                If this object is a complex type, the reader is positioned at the end element of its outer element
        */
        public static DescribeImageAttributesGroup parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            DescribeImageAttributesGroup object =
                new DescribeImageAttributesGroup();

            int event;
            java.lang.String nillableValue = null;
            java.lang.String prefix ="";
            java.lang.String namespaceuri ="";
            try {
                
                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                 
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","launchPermission").equals(reader.getName())){
                                
                                                object.setLaunchPermission(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","productCodes").equals(reader.getName())){
                                
                                                object.setProductCodes(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","kernel").equals(reader.getName())){
                                
                                                object.setKernel(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","ramdisk").equals(reader.getName())){
                                
                                                object.setRamdisk(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","blockDeviceMapping").equals(reader.getName())){
                                
                                                object.setBlockDeviceMapping(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","description").equals(reader.getName())){
                                
                                                object.setDescription(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","instanceTypeCategory").equals(reader.getName())){
                                
                                                object.setInstanceTypeCategory(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                



            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

        }//end of factory class

        

        }
           
          