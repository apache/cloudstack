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
 * DescribeInstanceAttributesGroup.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */
            
                package com.amazon.ec2;
            

            /**
            *  DescribeInstanceAttributesGroup bean class
            */
        
        public  class DescribeInstanceAttributesGroup
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = DescribeInstanceAttributesGroup
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
            
                   localInstanceTypeTracker = false;
                
                   localKernelTracker = false;
                
                   localRamdiskTracker = false;
                
                   localUserDataTracker = false;
                
                   localDisableApiTerminationTracker = false;
                
                   localInstanceInitiatedShutdownBehaviorTracker = false;
                
                   localRootDeviceNameTracker = false;
                
                   localBlockDeviceMappingTracker = false;
                
                   localSourceDestCheckTracker = false;
                
                   localGroupSetTracker = false;
                
                   localProductCodesTracker = false;
                
                   localEbsOptimizedTracker = false;
                
            }
        

                        /**
                        * field for InstanceType
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localInstanceType ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localInstanceTypeTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getInstanceType(){
                               return localInstanceType;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param InstanceType
                               */
                               public void setInstanceType(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localInstanceTypeTracker = true;
                                       } else {
                                          localInstanceTypeTracker = false;
                                              
                                       }
                                   
                                            this.localInstanceType=param;
                                    

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
                        * field for UserData
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localUserData ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localUserDataTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getUserData(){
                               return localUserData;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param UserData
                               */
                               public void setUserData(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localUserDataTracker = true;
                                       } else {
                                          localUserDataTracker = false;
                                              
                                       }
                                   
                                            this.localUserData=param;
                                    

                               }
                            

                        /**
                        * field for DisableApiTermination
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localDisableApiTermination ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDisableApiTerminationTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getDisableApiTermination(){
                               return localDisableApiTermination;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param DisableApiTermination
                               */
                               public void setDisableApiTermination(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localDisableApiTerminationTracker = true;
                                       } else {
                                          localDisableApiTerminationTracker = false;
                                              
                                       }
                                   
                                            this.localDisableApiTermination=param;
                                    

                               }
                            

                        /**
                        * field for InstanceInitiatedShutdownBehavior
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localInstanceInitiatedShutdownBehavior ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localInstanceInitiatedShutdownBehaviorTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getInstanceInitiatedShutdownBehavior(){
                               return localInstanceInitiatedShutdownBehavior;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param InstanceInitiatedShutdownBehavior
                               */
                               public void setInstanceInitiatedShutdownBehavior(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localInstanceInitiatedShutdownBehaviorTracker = true;
                                       } else {
                                          localInstanceInitiatedShutdownBehaviorTracker = false;
                                              
                                       }
                                   
                                            this.localInstanceInitiatedShutdownBehavior=param;
                                    

                               }
                            

                        /**
                        * field for RootDeviceName
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localRootDeviceName ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localRootDeviceNameTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getRootDeviceName(){
                               return localRootDeviceName;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param RootDeviceName
                               */
                               public void setRootDeviceName(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localRootDeviceNameTracker = true;
                                       } else {
                                          localRootDeviceNameTracker = false;
                                              
                                       }
                                   
                                            this.localRootDeviceName=param;
                                    

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
                        * field for SourceDestCheck
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localSourceDestCheck ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localSourceDestCheckTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getSourceDestCheck(){
                               return localSourceDestCheck;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param SourceDestCheck
                               */
                               public void setSourceDestCheck(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localSourceDestCheckTracker = true;
                                       } else {
                                          localSourceDestCheckTracker = false;
                                              
                                       }
                                   
                                            this.localSourceDestCheck=param;
                                    

                               }
                            

                        /**
                        * field for GroupSet
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localGroupSet ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localGroupSetTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getGroupSet(){
                               return localGroupSet;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param GroupSet
                               */
                               public void setGroupSet(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localGroupSetTracker = true;
                                       } else {
                                          localGroupSetTracker = false;
                                              
                                       }
                                   
                                            this.localGroupSet=param;
                                    

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
                        * field for EbsOptimized
                        */

                        
                                    protected com.amazon.ec2.EmptyElementType localEbsOptimized ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localEbsOptimizedTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.EmptyElementType
                           */
                           public  com.amazon.ec2.EmptyElementType getEbsOptimized(){
                               return localEbsOptimized;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param EbsOptimized
                               */
                               public void setEbsOptimized(com.amazon.ec2.EmptyElementType param){
                            
                                clearAllSettingTrackers();
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localEbsOptimizedTracker = true;
                                       } else {
                                          localEbsOptimizedTracker = false;
                                              
                                       }
                                   
                                            this.localEbsOptimized=param;
                                    

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
                       DescribeInstanceAttributesGroup.this.serialize(parentQName,factory,xmlWriter);
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
                           namespacePrefix+":DescribeInstanceAttributesGroup",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "DescribeInstanceAttributesGroup",
                           xmlWriter);
                   }

               
                   }
                if (localInstanceTypeTracker){
                                            if (localInstanceType==null){
                                                 throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");
                                            }
                                           localInstanceType.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","instanceType"),
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
                                        } if (localUserDataTracker){
                                            if (localUserData==null){
                                                 throw new org.apache.axis2.databinding.ADBException("userData cannot be null!!");
                                            }
                                           localUserData.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","userData"),
                                               factory,xmlWriter);
                                        } if (localDisableApiTerminationTracker){
                                            if (localDisableApiTermination==null){
                                                 throw new org.apache.axis2.databinding.ADBException("disableApiTermination cannot be null!!");
                                            }
                                           localDisableApiTermination.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","disableApiTermination"),
                                               factory,xmlWriter);
                                        } if (localInstanceInitiatedShutdownBehaviorTracker){
                                            if (localInstanceInitiatedShutdownBehavior==null){
                                                 throw new org.apache.axis2.databinding.ADBException("instanceInitiatedShutdownBehavior cannot be null!!");
                                            }
                                           localInstanceInitiatedShutdownBehavior.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","instanceInitiatedShutdownBehavior"),
                                               factory,xmlWriter);
                                        } if (localRootDeviceNameTracker){
                                            if (localRootDeviceName==null){
                                                 throw new org.apache.axis2.databinding.ADBException("rootDeviceName cannot be null!!");
                                            }
                                           localRootDeviceName.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","rootDeviceName"),
                                               factory,xmlWriter);
                                        } if (localBlockDeviceMappingTracker){
                                            if (localBlockDeviceMapping==null){
                                                 throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
                                            }
                                           localBlockDeviceMapping.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","blockDeviceMapping"),
                                               factory,xmlWriter);
                                        } if (localSourceDestCheckTracker){
                                            if (localSourceDestCheck==null){
                                                 throw new org.apache.axis2.databinding.ADBException("sourceDestCheck cannot be null!!");
                                            }
                                           localSourceDestCheck.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","sourceDestCheck"),
                                               factory,xmlWriter);
                                        } if (localGroupSetTracker){
                                            if (localGroupSet==null){
                                                 throw new org.apache.axis2.databinding.ADBException("groupSet cannot be null!!");
                                            }
                                           localGroupSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","groupSet"),
                                               factory,xmlWriter);
                                        } if (localProductCodesTracker){
                                            if (localProductCodes==null){
                                                 throw new org.apache.axis2.databinding.ADBException("productCodes cannot be null!!");
                                            }
                                           localProductCodes.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","productCodes"),
                                               factory,xmlWriter);
                                        } if (localEbsOptimizedTracker){
                                            if (localEbsOptimized==null){
                                                 throw new org.apache.axis2.databinding.ADBException("ebsOptimized cannot be null!!");
                                            }
                                           localEbsOptimized.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","ebsOptimized"),
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

                 if (localInstanceTypeTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "instanceType"));
                            
                            
                                    if (localInstanceType==null){
                                         throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");
                                    }
                                    elementList.add(localInstanceType);
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
                                } if (localUserDataTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "userData"));
                            
                            
                                    if (localUserData==null){
                                         throw new org.apache.axis2.databinding.ADBException("userData cannot be null!!");
                                    }
                                    elementList.add(localUserData);
                                } if (localDisableApiTerminationTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "disableApiTermination"));
                            
                            
                                    if (localDisableApiTermination==null){
                                         throw new org.apache.axis2.databinding.ADBException("disableApiTermination cannot be null!!");
                                    }
                                    elementList.add(localDisableApiTermination);
                                } if (localInstanceInitiatedShutdownBehaviorTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "instanceInitiatedShutdownBehavior"));
                            
                            
                                    if (localInstanceInitiatedShutdownBehavior==null){
                                         throw new org.apache.axis2.databinding.ADBException("instanceInitiatedShutdownBehavior cannot be null!!");
                                    }
                                    elementList.add(localInstanceInitiatedShutdownBehavior);
                                } if (localRootDeviceNameTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "rootDeviceName"));
                            
                            
                                    if (localRootDeviceName==null){
                                         throw new org.apache.axis2.databinding.ADBException("rootDeviceName cannot be null!!");
                                    }
                                    elementList.add(localRootDeviceName);
                                } if (localBlockDeviceMappingTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "blockDeviceMapping"));
                            
                            
                                    if (localBlockDeviceMapping==null){
                                         throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
                                    }
                                    elementList.add(localBlockDeviceMapping);
                                } if (localSourceDestCheckTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "sourceDestCheck"));
                            
                            
                                    if (localSourceDestCheck==null){
                                         throw new org.apache.axis2.databinding.ADBException("sourceDestCheck cannot be null!!");
                                    }
                                    elementList.add(localSourceDestCheck);
                                } if (localGroupSetTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "groupSet"));
                            
                            
                                    if (localGroupSet==null){
                                         throw new org.apache.axis2.databinding.ADBException("groupSet cannot be null!!");
                                    }
                                    elementList.add(localGroupSet);
                                } if (localProductCodesTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "productCodes"));
                            
                            
                                    if (localProductCodes==null){
                                         throw new org.apache.axis2.databinding.ADBException("productCodes cannot be null!!");
                                    }
                                    elementList.add(localProductCodes);
                                } if (localEbsOptimizedTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "ebsOptimized"));
                            
                            
                                    if (localEbsOptimized==null){
                                         throw new org.apache.axis2.databinding.ADBException("ebsOptimized cannot be null!!");
                                    }
                                    elementList.add(localEbsOptimized);
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
        public static DescribeInstanceAttributesGroup parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            DescribeInstanceAttributesGroup object =
                new DescribeInstanceAttributesGroup();

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
                

                 
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","instanceType").equals(reader.getName())){
                                
                                                object.setInstanceType(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
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
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","userData").equals(reader.getName())){
                                
                                                object.setUserData(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","disableApiTermination").equals(reader.getName())){
                                
                                                object.setDisableApiTermination(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","instanceInitiatedShutdownBehavior").equals(reader.getName())){
                                
                                                object.setInstanceInitiatedShutdownBehavior(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","rootDeviceName").equals(reader.getName())){
                                
                                                object.setRootDeviceName(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","blockDeviceMapping").equals(reader.getName())){
                                
                                                object.setBlockDeviceMapping(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","sourceDestCheck").equals(reader.getName())){
                                
                                                object.setSourceDestCheck(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","groupSet").equals(reader.getName())){
                                
                                                object.setGroupSet(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","productCodes").equals(reader.getName())){
                                
                                                object.setProductCodes(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","ebsOptimized").equals(reader.getName())){
                                
                                                object.setEbsOptimized(com.amazon.ec2.EmptyElementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                



            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

        }//end of factory class

        

        }
           
          