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
 * ImportInstanceLaunchSpecificationType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */
            
                package com.amazon.ec2;
            

            /**
            *  ImportInstanceLaunchSpecificationType bean class
            */
        
        public  class ImportInstanceLaunchSpecificationType
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = ImportInstanceLaunchSpecificationType
                Namespace URI = http://ec2.amazonaws.com/doc/2012-08-15/
                Namespace Prefix = ns1
                */
            

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://ec2.amazonaws.com/doc/2012-08-15/")){
                return "ns1";
            }
            return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
        }

        

                        /**
                        * field for Architecture
                        */

                        
                                    protected java.lang.String localArchitecture ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getArchitecture(){
                               return localArchitecture;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Architecture
                               */
                               public void setArchitecture(java.lang.String param){
                            
                                            this.localArchitecture=param;
                                    

                               }
                            

                        /**
                        * field for GroupSet
                        */

                        
                                    protected com.amazon.ec2.ImportInstanceGroupSetType localGroupSet ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localGroupSetTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.ImportInstanceGroupSetType
                           */
                           public  com.amazon.ec2.ImportInstanceGroupSetType getGroupSet(){
                               return localGroupSet;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param GroupSet
                               */
                               public void setGroupSet(com.amazon.ec2.ImportInstanceGroupSetType param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localGroupSetTracker = true;
                                       } else {
                                          localGroupSetTracker = false;
                                              
                                       }
                                   
                                            this.localGroupSet=param;
                                    

                               }
                            

                        /**
                        * field for UserData
                        */

                        
                                    protected com.amazon.ec2.UserDataType localUserData ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localUserDataTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.UserDataType
                           */
                           public  com.amazon.ec2.UserDataType getUserData(){
                               return localUserData;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param UserData
                               */
                               public void setUserData(com.amazon.ec2.UserDataType param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localUserDataTracker = true;
                                       } else {
                                          localUserDataTracker = false;
                                              
                                       }
                                   
                                            this.localUserData=param;
                                    

                               }
                            

                        /**
                        * field for InstanceType
                        */

                        
                                    protected java.lang.String localInstanceType ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getInstanceType(){
                               return localInstanceType;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param InstanceType
                               */
                               public void setInstanceType(java.lang.String param){
                            
                                            this.localInstanceType=param;
                                    

                               }
                            

                        /**
                        * field for Placement
                        */

                        
                                    protected com.amazon.ec2.InstancePlacementType localPlacement ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localPlacementTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.InstancePlacementType
                           */
                           public  com.amazon.ec2.InstancePlacementType getPlacement(){
                               return localPlacement;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Placement
                               */
                               public void setPlacement(com.amazon.ec2.InstancePlacementType param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localPlacementTracker = true;
                                       } else {
                                          localPlacementTracker = false;
                                              
                                       }
                                   
                                            this.localPlacement=param;
                                    

                               }
                            

                        /**
                        * field for Monitoring
                        */

                        
                                    protected com.amazon.ec2.MonitoringInstanceType localMonitoring ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localMonitoringTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.MonitoringInstanceType
                           */
                           public  com.amazon.ec2.MonitoringInstanceType getMonitoring(){
                               return localMonitoring;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Monitoring
                               */
                               public void setMonitoring(com.amazon.ec2.MonitoringInstanceType param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localMonitoringTracker = true;
                                       } else {
                                          localMonitoringTracker = false;
                                              
                                       }
                                   
                                            this.localMonitoring=param;
                                    

                               }
                            

                        /**
                        * field for SubnetId
                        */

                        
                                    protected java.lang.String localSubnetId ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localSubnetIdTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getSubnetId(){
                               return localSubnetId;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param SubnetId
                               */
                               public void setSubnetId(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localSubnetIdTracker = true;
                                       } else {
                                          localSubnetIdTracker = false;
                                              
                                       }
                                   
                                            this.localSubnetId=param;
                                    

                               }
                            

                        /**
                        * field for InstanceInitiatedShutdownBehavior
                        */

                        
                                    protected java.lang.String localInstanceInitiatedShutdownBehavior ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localInstanceInitiatedShutdownBehaviorTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getInstanceInitiatedShutdownBehavior(){
                               return localInstanceInitiatedShutdownBehavior;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param InstanceInitiatedShutdownBehavior
                               */
                               public void setInstanceInitiatedShutdownBehavior(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localInstanceInitiatedShutdownBehaviorTracker = true;
                                       } else {
                                          localInstanceInitiatedShutdownBehaviorTracker = false;
                                              
                                       }
                                   
                                            this.localInstanceInitiatedShutdownBehavior=param;
                                    

                               }
                            

                        /**
                        * field for PrivateIpAddress
                        */

                        
                                    protected java.lang.String localPrivateIpAddress ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localPrivateIpAddressTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getPrivateIpAddress(){
                               return localPrivateIpAddress;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param PrivateIpAddress
                               */
                               public void setPrivateIpAddress(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localPrivateIpAddressTracker = true;
                                       } else {
                                          localPrivateIpAddressTracker = false;
                                              
                                       }
                                   
                                            this.localPrivateIpAddress=param;
                                    

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
                       ImportInstanceLaunchSpecificationType.this.serialize(parentQName,factory,xmlWriter);
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
                
                  if (serializeType){
               

                   java.lang.String namespacePrefix = registerPrefix(xmlWriter,"http://ec2.amazonaws.com/doc/2012-08-15/");
                   if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)){
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           namespacePrefix+":ImportInstanceLaunchSpecificationType",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "ImportInstanceLaunchSpecificationType",
                           xmlWriter);
                   }

               
                   }
               
                                    namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"architecture", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"architecture");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("architecture");
                                    }
                                

                                          if (localArchitecture==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("architecture cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localArchitecture);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                              if (localGroupSetTracker){
                                            if (localGroupSet==null){
                                                 throw new org.apache.axis2.databinding.ADBException("groupSet cannot be null!!");
                                            }
                                           localGroupSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","groupSet"),
                                               factory,xmlWriter);
                                        } if (localUserDataTracker){
                                            if (localUserData==null){
                                                 throw new org.apache.axis2.databinding.ADBException("userData cannot be null!!");
                                            }
                                           localUserData.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","userData"),
                                               factory,xmlWriter);
                                        }
                                    namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"instanceType", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"instanceType");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("instanceType");
                                    }
                                

                                          if (localInstanceType==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localInstanceType);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                              if (localPlacementTracker){
                                            if (localPlacement==null){
                                                 throw new org.apache.axis2.databinding.ADBException("placement cannot be null!!");
                                            }
                                           localPlacement.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","placement"),
                                               factory,xmlWriter);
                                        } if (localMonitoringTracker){
                                            if (localMonitoring==null){
                                                 throw new org.apache.axis2.databinding.ADBException("monitoring cannot be null!!");
                                            }
                                           localMonitoring.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","monitoring"),
                                               factory,xmlWriter);
                                        } if (localSubnetIdTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"subnetId", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"subnetId");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("subnetId");
                                    }
                                

                                          if (localSubnetId==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("subnetId cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localSubnetId);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localInstanceInitiatedShutdownBehaviorTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"instanceInitiatedShutdownBehavior", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"instanceInitiatedShutdownBehavior");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("instanceInitiatedShutdownBehavior");
                                    }
                                

                                          if (localInstanceInitiatedShutdownBehavior==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("instanceInitiatedShutdownBehavior cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localInstanceInitiatedShutdownBehavior);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localPrivateIpAddressTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2012-08-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"privateIpAddress", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"privateIpAddress");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("privateIpAddress");
                                    }
                                

                                          if (localPrivateIpAddress==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("privateIpAddress cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localPrivateIpAddress);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             }
                    xmlWriter.writeEndElement();
               

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

                
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "architecture"));
                                 
                                        if (localArchitecture != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localArchitecture));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("architecture cannot be null!!");
                                        }
                                     if (localGroupSetTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "groupSet"));
                            
                            
                                    if (localGroupSet==null){
                                         throw new org.apache.axis2.databinding.ADBException("groupSet cannot be null!!");
                                    }
                                    elementList.add(localGroupSet);
                                } if (localUserDataTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "userData"));
                            
                            
                                    if (localUserData==null){
                                         throw new org.apache.axis2.databinding.ADBException("userData cannot be null!!");
                                    }
                                    elementList.add(localUserData);
                                }
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "instanceType"));
                                 
                                        if (localInstanceType != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInstanceType));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");
                                        }
                                     if (localPlacementTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "placement"));
                            
                            
                                    if (localPlacement==null){
                                         throw new org.apache.axis2.databinding.ADBException("placement cannot be null!!");
                                    }
                                    elementList.add(localPlacement);
                                } if (localMonitoringTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "monitoring"));
                            
                            
                                    if (localMonitoring==null){
                                         throw new org.apache.axis2.databinding.ADBException("monitoring cannot be null!!");
                                    }
                                    elementList.add(localMonitoring);
                                } if (localSubnetIdTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "subnetId"));
                                 
                                        if (localSubnetId != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSubnetId));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("subnetId cannot be null!!");
                                        }
                                    } if (localInstanceInitiatedShutdownBehaviorTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "instanceInitiatedShutdownBehavior"));
                                 
                                        if (localInstanceInitiatedShutdownBehavior != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInstanceInitiatedShutdownBehavior));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("instanceInitiatedShutdownBehavior cannot be null!!");
                                        }
                                    } if (localPrivateIpAddressTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/",
                                                                      "privateIpAddress"));
                                 
                                        if (localPrivateIpAddress != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPrivateIpAddress));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("privateIpAddress cannot be null!!");
                                        }
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
        public static ImportInstanceLaunchSpecificationType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            ImportInstanceLaunchSpecificationType object =
                new ImportInstanceLaunchSpecificationType();

            int event;
            java.lang.String nillableValue = null;
            java.lang.String prefix ="";
            java.lang.String namespaceuri ="";
            try {
                
                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                
                if (reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","type")!=null){
                  java.lang.String fullTypeName = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance",
                        "type");
                  if (fullTypeName!=null){
                    java.lang.String nsPrefix = null;
                    if (fullTypeName.indexOf(":") > -1){
                        nsPrefix = fullTypeName.substring(0,fullTypeName.indexOf(":"));
                    }
                    nsPrefix = nsPrefix==null?"":nsPrefix;

                    java.lang.String type = fullTypeName.substring(fullTypeName.indexOf(":")+1);
                    
                            if (!"ImportInstanceLaunchSpecificationType".equals(type)){
                                //find namespace for the prefix
                                java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                                return (ImportInstanceLaunchSpecificationType)com.amazon.ec2.ExtensionMapper.getTypeObject(
                                     nsUri,type,reader);
                              }
                        

                  }
                

                }

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                 
                    
                    reader.next();
                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","architecture").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setArchitecture(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","groupSet").equals(reader.getName())){
                                
                                                object.setGroupSet(com.amazon.ec2.ImportInstanceGroupSetType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","userData").equals(reader.getName())){
                                
                                                object.setUserData(com.amazon.ec2.UserDataType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","instanceType").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setInstanceType(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","placement").equals(reader.getName())){
                                
                                                object.setPlacement(com.amazon.ec2.InstancePlacementType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","monitoring").equals(reader.getName())){
                                
                                                object.setMonitoring(com.amazon.ec2.MonitoringInstanceType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","subnetId").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSubnetId(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","instanceInitiatedShutdownBehavior").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setInstanceInitiatedShutdownBehavior(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2012-08-15/","privateIpAddress").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setPrivateIpAddress(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
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
           
          