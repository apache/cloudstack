
/**
 * DescribeInstanceAttributeResponseTypeChoice_type0.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:34 EDT)
 */
            
                package com.amazon.ec2;
            

            /**
            *  DescribeInstanceAttributeResponseTypeChoice_type0 bean class
            */
        
        public  class DescribeInstanceAttributeResponseTypeChoice_type0
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = DescribeInstanceAttributeResponseTypeChoice_type0
                Namespace URI = http://ec2.amazonaws.com/doc/2010-11-15/
                Namespace Prefix = ns1
                */
            

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://ec2.amazonaws.com/doc/2010-11-15/")){
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
                
            }
        

                        /**
                        * field for InstanceType
                        */

                        
                                    protected com.amazon.ec2.NullableAttributeValueType localInstanceType ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localInstanceTypeTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.NullableAttributeValueType
                           */
                           public  com.amazon.ec2.NullableAttributeValueType getInstanceType(){
                               return localInstanceType;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param InstanceType
                               */
                               public void setInstanceType(com.amazon.ec2.NullableAttributeValueType param){
                            
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

                        
                                    protected com.amazon.ec2.NullableAttributeValueType localKernel ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localKernelTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.NullableAttributeValueType
                           */
                           public  com.amazon.ec2.NullableAttributeValueType getKernel(){
                               return localKernel;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Kernel
                               */
                               public void setKernel(com.amazon.ec2.NullableAttributeValueType param){
                            
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

                        
                                    protected com.amazon.ec2.NullableAttributeValueType localRamdisk ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localRamdiskTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.NullableAttributeValueType
                           */
                           public  com.amazon.ec2.NullableAttributeValueType getRamdisk(){
                               return localRamdisk;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Ramdisk
                               */
                               public void setRamdisk(com.amazon.ec2.NullableAttributeValueType param){
                            
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

                        
                                    protected com.amazon.ec2.NullableAttributeValueType localUserData ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localUserDataTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.NullableAttributeValueType
                           */
                           public  com.amazon.ec2.NullableAttributeValueType getUserData(){
                               return localUserData;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param UserData
                               */
                               public void setUserData(com.amazon.ec2.NullableAttributeValueType param){
                            
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

                        
                                    protected com.amazon.ec2.NullableAttributeBooleanValueType localDisableApiTermination ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDisableApiTerminationTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.NullableAttributeBooleanValueType
                           */
                           public  com.amazon.ec2.NullableAttributeBooleanValueType getDisableApiTermination(){
                               return localDisableApiTermination;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param DisableApiTermination
                               */
                               public void setDisableApiTermination(com.amazon.ec2.NullableAttributeBooleanValueType param){
                            
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

                        
                                    protected com.amazon.ec2.NullableAttributeValueType localInstanceInitiatedShutdownBehavior ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localInstanceInitiatedShutdownBehaviorTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.NullableAttributeValueType
                           */
                           public  com.amazon.ec2.NullableAttributeValueType getInstanceInitiatedShutdownBehavior(){
                               return localInstanceInitiatedShutdownBehavior;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param InstanceInitiatedShutdownBehavior
                               */
                               public void setInstanceInitiatedShutdownBehavior(com.amazon.ec2.NullableAttributeValueType param){
                            
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

                        
                                    protected com.amazon.ec2.NullableAttributeValueType localRootDeviceName ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localRootDeviceNameTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.NullableAttributeValueType
                           */
                           public  com.amazon.ec2.NullableAttributeValueType getRootDeviceName(){
                               return localRootDeviceName;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param RootDeviceName
                               */
                               public void setRootDeviceName(com.amazon.ec2.NullableAttributeValueType param){
                            
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

                        
                                    protected com.amazon.ec2.InstanceBlockDeviceMappingResponseType localBlockDeviceMapping ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localBlockDeviceMappingTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.InstanceBlockDeviceMappingResponseType
                           */
                           public  com.amazon.ec2.InstanceBlockDeviceMappingResponseType getBlockDeviceMapping(){
                               return localBlockDeviceMapping;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param BlockDeviceMapping
                               */
                               public void setBlockDeviceMapping(com.amazon.ec2.InstanceBlockDeviceMappingResponseType param){
                            
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
                       DescribeInstanceAttributeResponseTypeChoice_type0.this.serialize(parentQName,factory,xmlWriter);
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
               

                   java.lang.String namespacePrefix = registerPrefix(xmlWriter,"http://ec2.amazonaws.com/doc/2010-11-15/");
                   if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)){
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           namespacePrefix+":DescribeInstanceAttributeResponseTypeChoice_type0",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "DescribeInstanceAttributeResponseTypeChoice_type0",
                           xmlWriter);
                   }

               
                   }
                if (localInstanceTypeTracker){
                                            if (localInstanceType==null){
                                                 throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");
                                            }
                                           localInstanceType.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","instanceType"),
                                               factory,xmlWriter);
                                        } if (localKernelTracker){
                                            if (localKernel==null){
                                                 throw new org.apache.axis2.databinding.ADBException("kernel cannot be null!!");
                                            }
                                           localKernel.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","kernel"),
                                               factory,xmlWriter);
                                        } if (localRamdiskTracker){
                                            if (localRamdisk==null){
                                                 throw new org.apache.axis2.databinding.ADBException("ramdisk cannot be null!!");
                                            }
                                           localRamdisk.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","ramdisk"),
                                               factory,xmlWriter);
                                        } if (localUserDataTracker){
                                            if (localUserData==null){
                                                 throw new org.apache.axis2.databinding.ADBException("userData cannot be null!!");
                                            }
                                           localUserData.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","userData"),
                                               factory,xmlWriter);
                                        } if (localDisableApiTerminationTracker){
                                            if (localDisableApiTermination==null){
                                                 throw new org.apache.axis2.databinding.ADBException("disableApiTermination cannot be null!!");
                                            }
                                           localDisableApiTermination.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","disableApiTermination"),
                                               factory,xmlWriter);
                                        } if (localInstanceInitiatedShutdownBehaviorTracker){
                                            if (localInstanceInitiatedShutdownBehavior==null){
                                                 throw new org.apache.axis2.databinding.ADBException("instanceInitiatedShutdownBehavior cannot be null!!");
                                            }
                                           localInstanceInitiatedShutdownBehavior.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","instanceInitiatedShutdownBehavior"),
                                               factory,xmlWriter);
                                        } if (localRootDeviceNameTracker){
                                            if (localRootDeviceName==null){
                                                 throw new org.apache.axis2.databinding.ADBException("rootDeviceName cannot be null!!");
                                            }
                                           localRootDeviceName.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","rootDeviceName"),
                                               factory,xmlWriter);
                                        } if (localBlockDeviceMappingTracker){
                                            if (localBlockDeviceMapping==null){
                                                 throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
                                            }
                                           localBlockDeviceMapping.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","blockDeviceMapping"),
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
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "instanceType"));
                            
                            
                                    if (localInstanceType==null){
                                         throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");
                                    }
                                    elementList.add(localInstanceType);
                                } if (localKernelTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "kernel"));
                            
                            
                                    if (localKernel==null){
                                         throw new org.apache.axis2.databinding.ADBException("kernel cannot be null!!");
                                    }
                                    elementList.add(localKernel);
                                } if (localRamdiskTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "ramdisk"));
                            
                            
                                    if (localRamdisk==null){
                                         throw new org.apache.axis2.databinding.ADBException("ramdisk cannot be null!!");
                                    }
                                    elementList.add(localRamdisk);
                                } if (localUserDataTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "userData"));
                            
                            
                                    if (localUserData==null){
                                         throw new org.apache.axis2.databinding.ADBException("userData cannot be null!!");
                                    }
                                    elementList.add(localUserData);
                                } if (localDisableApiTerminationTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "disableApiTermination"));
                            
                            
                                    if (localDisableApiTermination==null){
                                         throw new org.apache.axis2.databinding.ADBException("disableApiTermination cannot be null!!");
                                    }
                                    elementList.add(localDisableApiTermination);
                                } if (localInstanceInitiatedShutdownBehaviorTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "instanceInitiatedShutdownBehavior"));
                            
                            
                                    if (localInstanceInitiatedShutdownBehavior==null){
                                         throw new org.apache.axis2.databinding.ADBException("instanceInitiatedShutdownBehavior cannot be null!!");
                                    }
                                    elementList.add(localInstanceInitiatedShutdownBehavior);
                                } if (localRootDeviceNameTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "rootDeviceName"));
                            
                            
                                    if (localRootDeviceName==null){
                                         throw new org.apache.axis2.databinding.ADBException("rootDeviceName cannot be null!!");
                                    }
                                    elementList.add(localRootDeviceName);
                                } if (localBlockDeviceMappingTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "blockDeviceMapping"));
                            
                            
                                    if (localBlockDeviceMapping==null){
                                         throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
                                    }
                                    elementList.add(localBlockDeviceMapping);
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
        public static DescribeInstanceAttributeResponseTypeChoice_type0 parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            DescribeInstanceAttributeResponseTypeChoice_type0 object =
                new DescribeInstanceAttributeResponseTypeChoice_type0();

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
                

                 
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","instanceType").equals(reader.getName())){
                                
                                                object.setInstanceType(com.amazon.ec2.NullableAttributeValueType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","kernel").equals(reader.getName())){
                                
                                                object.setKernel(com.amazon.ec2.NullableAttributeValueType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","ramdisk").equals(reader.getName())){
                                
                                                object.setRamdisk(com.amazon.ec2.NullableAttributeValueType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","userData").equals(reader.getName())){
                                
                                                object.setUserData(com.amazon.ec2.NullableAttributeValueType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","disableApiTermination").equals(reader.getName())){
                                
                                                object.setDisableApiTermination(com.amazon.ec2.NullableAttributeBooleanValueType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","instanceInitiatedShutdownBehavior").equals(reader.getName())){
                                
                                                object.setInstanceInitiatedShutdownBehavior(com.amazon.ec2.NullableAttributeValueType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","rootDeviceName").equals(reader.getName())){
                                
                                                object.setRootDeviceName(com.amazon.ec2.NullableAttributeValueType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                        else
                                    
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","blockDeviceMapping").equals(reader.getName())){
                                
                                                object.setBlockDeviceMapping(com.amazon.ec2.InstanceBlockDeviceMappingResponseType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                



            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

        }//end of factory class

        

        }
           
          