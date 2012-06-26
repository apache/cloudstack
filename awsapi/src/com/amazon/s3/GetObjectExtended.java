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
 * GetObjectExtended.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:34 EDT)
 */
            
                package com.amazon.s3;
            

            /**
            *  GetObjectExtended bean class
            */
        
        public  class GetObjectExtended
        implements org.apache.axis2.databinding.ADBBean{
        
                public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName(
                "http://s3.amazonaws.com/doc/2006-03-01/",
                "GetObjectExtended",
                "ns1");

            

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://s3.amazonaws.com/doc/2006-03-01/")){
                return "ns1";
            }
            return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
        }

        

                        /**
                        * field for Bucket
                        */

                        
                                    protected java.lang.String localBucket ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getBucket(){
                               return localBucket;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Bucket
                               */
                               public void setBucket(java.lang.String param){
                            
                                            this.localBucket=param;
                                    

                               }
                            

                        /**
                        * field for Key
                        */

                        
                                    protected java.lang.String localKey ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getKey(){
                               return localKey;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Key
                               */
                               public void setKey(java.lang.String param){
                            
                                            this.localKey=param;
                                    

                               }
                            

                        /**
                        * field for GetMetadata
                        */

                        
                                    protected boolean localGetMetadata ;
                                

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getGetMetadata(){
                               return localGetMetadata;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param GetMetadata
                               */
                               public void setGetMetadata(boolean param){
                            
                                            this.localGetMetadata=param;
                                    

                               }
                            

                        /**
                        * field for GetData
                        */

                        
                                    protected boolean localGetData ;
                                

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getGetData(){
                               return localGetData;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param GetData
                               */
                               public void setGetData(boolean param){
                            
                                            this.localGetData=param;
                                    

                               }
                            

                        /**
                        * field for InlineData
                        */

                        
                                    protected boolean localInlineData ;
                                

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getInlineData(){
                               return localInlineData;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param InlineData
                               */
                               public void setInlineData(boolean param){
                            
                                            this.localInlineData=param;
                                    

                               }
                            

                        /**
                        * field for ByteRangeStart
                        */

                        
                                    protected long localByteRangeStart ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localByteRangeStartTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return long
                           */
                           public  long getByteRangeStart(){
                               return localByteRangeStart;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ByteRangeStart
                               */
                               public void setByteRangeStart(long param){
                            
                                       // setting primitive attribute tracker to true
                                       
                                               if (param==java.lang.Long.MIN_VALUE) {
                                           localByteRangeStartTracker = false;
                                              
                                       } else {
                                          localByteRangeStartTracker = true;
                                       }
                                   
                                            this.localByteRangeStart=param;
                                    

                               }
                            

                        /**
                        * field for ByteRangeEnd
                        */

                        
                                    protected long localByteRangeEnd ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localByteRangeEndTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return long
                           */
                           public  long getByteRangeEnd(){
                               return localByteRangeEnd;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ByteRangeEnd
                               */
                               public void setByteRangeEnd(long param){
                            
                                       // setting primitive attribute tracker to true
                                       
                                               if (param==java.lang.Long.MIN_VALUE) {
                                           localByteRangeEndTracker = false;
                                              
                                       } else {
                                          localByteRangeEndTracker = true;
                                       }
                                   
                                            this.localByteRangeEnd=param;
                                    

                               }
                            

                        /**
                        * field for IfModifiedSince
                        */

                        
                                    protected java.util.Calendar localIfModifiedSince ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localIfModifiedSinceTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.util.Calendar
                           */
                           public  java.util.Calendar getIfModifiedSince(){
                               return localIfModifiedSince;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param IfModifiedSince
                               */
                               public void setIfModifiedSince(java.util.Calendar param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localIfModifiedSinceTracker = true;
                                       } else {
                                          localIfModifiedSinceTracker = false;
                                              
                                       }
                                   
                                            this.localIfModifiedSince=param;
                                    

                               }
                            

                        /**
                        * field for IfUnmodifiedSince
                        */

                        
                                    protected java.util.Calendar localIfUnmodifiedSince ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localIfUnmodifiedSinceTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.util.Calendar
                           */
                           public  java.util.Calendar getIfUnmodifiedSince(){
                               return localIfUnmodifiedSince;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param IfUnmodifiedSince
                               */
                               public void setIfUnmodifiedSince(java.util.Calendar param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localIfUnmodifiedSinceTracker = true;
                                       } else {
                                          localIfUnmodifiedSinceTracker = false;
                                              
                                       }
                                   
                                            this.localIfUnmodifiedSince=param;
                                    

                               }
                            

                        /**
                        * field for IfMatch
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localIfMatch ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localIfMatchTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getIfMatch(){
                               return localIfMatch;
                           }

                           
                        


                               
                              /**
                               * validate the array for IfMatch
                               */
                              protected void validateIfMatch(java.lang.String[] param){
                             
                              if ((param != null) && (param.length > 100)){
                                throw new java.lang.RuntimeException();
                              }
                              
                              }


                             /**
                              * Auto generated setter method
                              * @param param IfMatch
                              */
                              public void setIfMatch(java.lang.String[] param){
                              
                                   validateIfMatch(param);

                               
                                          if (param != null){
                                             //update the setting tracker
                                             localIfMatchTracker = true;
                                          } else {
                                             localIfMatchTracker = false;
                                                 
                                          }
                                      
                                      this.localIfMatch=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addIfMatch(java.lang.String param){
                                   if (localIfMatch == null){
                                   localIfMatch = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localIfMatchTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localIfMatch);
                               list.add(param);
                               this.localIfMatch =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for IfNoneMatch
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localIfNoneMatch ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localIfNoneMatchTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getIfNoneMatch(){
                               return localIfNoneMatch;
                           }

                           
                        


                               
                              /**
                               * validate the array for IfNoneMatch
                               */
                              protected void validateIfNoneMatch(java.lang.String[] param){
                             
                              if ((param != null) && (param.length > 100)){
                                throw new java.lang.RuntimeException();
                              }
                              
                              }


                             /**
                              * Auto generated setter method
                              * @param param IfNoneMatch
                              */
                              public void setIfNoneMatch(java.lang.String[] param){
                              
                                   validateIfNoneMatch(param);

                               
                                          if (param != null){
                                             //update the setting tracker
                                             localIfNoneMatchTracker = true;
                                          } else {
                                             localIfNoneMatchTracker = false;
                                                 
                                          }
                                      
                                      this.localIfNoneMatch=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addIfNoneMatch(java.lang.String param){
                                   if (localIfNoneMatch == null){
                                   localIfNoneMatch = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localIfNoneMatchTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localIfNoneMatch);
                               list.add(param);
                               this.localIfNoneMatch =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for ReturnCompleteObjectOnConditionFailure
                        */

                        
                                    protected boolean localReturnCompleteObjectOnConditionFailure ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localReturnCompleteObjectOnConditionFailureTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getReturnCompleteObjectOnConditionFailure(){
                               return localReturnCompleteObjectOnConditionFailure;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ReturnCompleteObjectOnConditionFailure
                               */
                               public void setReturnCompleteObjectOnConditionFailure(boolean param){
                            
                                       // setting primitive attribute tracker to true
                                       
                                               if (false) {
                                           localReturnCompleteObjectOnConditionFailureTracker = false;
                                              
                                       } else {
                                          localReturnCompleteObjectOnConditionFailureTracker = true;
                                       }
                                   
                                            this.localReturnCompleteObjectOnConditionFailure=param;
                                    

                               }
                            

                        /**
                        * field for AWSAccessKeyId
                        */

                        
                                    protected java.lang.String localAWSAccessKeyId ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localAWSAccessKeyIdTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getAWSAccessKeyId(){
                               return localAWSAccessKeyId;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param AWSAccessKeyId
                               */
                               public void setAWSAccessKeyId(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localAWSAccessKeyIdTracker = true;
                                       } else {
                                          localAWSAccessKeyIdTracker = false;
                                              
                                       }
                                   
                                            this.localAWSAccessKeyId=param;
                                    

                               }
                            

                        /**
                        * field for Timestamp
                        */

                        
                                    protected java.util.Calendar localTimestamp ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localTimestampTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.util.Calendar
                           */
                           public  java.util.Calendar getTimestamp(){
                               return localTimestamp;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Timestamp
                               */
                               public void setTimestamp(java.util.Calendar param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localTimestampTracker = true;
                                       } else {
                                          localTimestampTracker = false;
                                              
                                       }
                                   
                                            this.localTimestamp=param;
                                    

                               }
                            

                        /**
                        * field for Signature
                        */

                        
                                    protected java.lang.String localSignature ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localSignatureTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getSignature(){
                               return localSignature;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Signature
                               */
                               public void setSignature(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localSignatureTracker = true;
                                       } else {
                                          localSignatureTracker = false;
                                              
                                       }
                                   
                                            this.localSignature=param;
                                    

                               }
                            

                        /**
                        * field for Credential
                        */

                        
                                    protected java.lang.String localCredential ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localCredentialTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getCredential(){
                               return localCredential;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Credential
                               */
                               public void setCredential(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localCredentialTracker = true;
                                       } else {
                                          localCredentialTracker = false;
                                              
                                       }
                                   
                                            this.localCredential=param;
                                    

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
                       new org.apache.axis2.databinding.ADBDataSource(this,MY_QNAME){

                 public void serialize(org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
                       GetObjectExtended.this.serialize(MY_QNAME,factory,xmlWriter);
                 }
               };
               return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(
               MY_QNAME,factory,dataSource);
            
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
               

                   java.lang.String namespacePrefix = registerPrefix(xmlWriter,"http://s3.amazonaws.com/doc/2006-03-01/");
                   if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)){
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           namespacePrefix+":GetObjectExtended",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "GetObjectExtended",
                           xmlWriter);
                   }

               
                   }
               
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"Bucket", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"Bucket");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("Bucket");
                                    }
                                

                                          if (localBucket==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("Bucket cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localBucket);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"Key", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"Key");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("Key");
                                    }
                                

                                          if (localKey==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("Key cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localKey);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"GetMetadata", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"GetMetadata");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("GetMetadata");
                                    }
                                
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("GetMetadata cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localGetMetadata));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"GetData", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"GetData");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("GetData");
                                    }
                                
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("GetData cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localGetData));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"InlineData", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"InlineData");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("InlineData");
                                    }
                                
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("InlineData cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInlineData));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                              if (localByteRangeStartTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"ByteRangeStart", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"ByteRangeStart");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("ByteRangeStart");
                                    }
                                
                                               if (localByteRangeStart==java.lang.Long.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("ByteRangeStart cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localByteRangeStart));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localByteRangeEndTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"ByteRangeEnd", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"ByteRangeEnd");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("ByteRangeEnd");
                                    }
                                
                                               if (localByteRangeEnd==java.lang.Long.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("ByteRangeEnd cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localByteRangeEnd));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localIfModifiedSinceTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"IfModifiedSince", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"IfModifiedSince");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("IfModifiedSince");
                                    }
                                

                                          if (localIfModifiedSince==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("IfModifiedSince cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIfModifiedSince));
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localIfUnmodifiedSinceTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"IfUnmodifiedSince", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"IfUnmodifiedSince");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("IfUnmodifiedSince");
                                    }
                                

                                          if (localIfUnmodifiedSince==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("IfUnmodifiedSince cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIfUnmodifiedSince));
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localIfMatchTracker){
                             if (localIfMatch!=null) {
                                   namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                   boolean emptyNamespace = namespace == null || namespace.length() == 0;
                                   prefix =  emptyNamespace ? null : xmlWriter.getPrefix(namespace);
                                   for (int i = 0;i < localIfMatch.length;i++){
                                        
                                            if (localIfMatch[i] != null){
                                        
                                                if (!emptyNamespace) {
                                                    if (prefix == null) {
                                                        java.lang.String prefix2 = generatePrefix(namespace);

                                                        xmlWriter.writeStartElement(prefix2,"IfMatch", namespace);
                                                        xmlWriter.writeNamespace(prefix2, namespace);
                                                        xmlWriter.setPrefix(prefix2, namespace);

                                                    } else {
                                                        xmlWriter.writeStartElement(namespace,"IfMatch");
                                                    }

                                                } else {
                                                    xmlWriter.writeStartElement("IfMatch");
                                                }

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIfMatch[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // we have to do nothing since minOccurs is zero
                                                       
                                                }

                                   }
                             } else {
                                 
                                         throw new org.apache.axis2.databinding.ADBException("IfMatch cannot be null!!");
                                    
                             }

                        } if (localIfNoneMatchTracker){
                             if (localIfNoneMatch!=null) {
                                   namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                   boolean emptyNamespace = namespace == null || namespace.length() == 0;
                                   prefix =  emptyNamespace ? null : xmlWriter.getPrefix(namespace);
                                   for (int i = 0;i < localIfNoneMatch.length;i++){
                                        
                                            if (localIfNoneMatch[i] != null){
                                        
                                                if (!emptyNamespace) {
                                                    if (prefix == null) {
                                                        java.lang.String prefix2 = generatePrefix(namespace);

                                                        xmlWriter.writeStartElement(prefix2,"IfNoneMatch", namespace);
                                                        xmlWriter.writeNamespace(prefix2, namespace);
                                                        xmlWriter.setPrefix(prefix2, namespace);

                                                    } else {
                                                        xmlWriter.writeStartElement(namespace,"IfNoneMatch");
                                                    }

                                                } else {
                                                    xmlWriter.writeStartElement("IfNoneMatch");
                                                }

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIfNoneMatch[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // we have to do nothing since minOccurs is zero
                                                       
                                                }

                                   }
                             } else {
                                 
                                         throw new org.apache.axis2.databinding.ADBException("IfNoneMatch cannot be null!!");
                                    
                             }

                        } if (localReturnCompleteObjectOnConditionFailureTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"ReturnCompleteObjectOnConditionFailure", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"ReturnCompleteObjectOnConditionFailure");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("ReturnCompleteObjectOnConditionFailure");
                                    }
                                
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("ReturnCompleteObjectOnConditionFailure cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localReturnCompleteObjectOnConditionFailure));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localAWSAccessKeyIdTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"AWSAccessKeyId", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"AWSAccessKeyId");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("AWSAccessKeyId");
                                    }
                                

                                          if (localAWSAccessKeyId==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("AWSAccessKeyId cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localAWSAccessKeyId);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localTimestampTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"Timestamp", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"Timestamp");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("Timestamp");
                                    }
                                

                                          if (localTimestamp==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("Timestamp cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localTimestamp));
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localSignatureTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"Signature", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"Signature");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("Signature");
                                    }
                                

                                          if (localSignature==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("Signature cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localSignature);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localCredentialTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"Credential", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"Credential");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("Credential");
                                    }
                                

                                          if (localCredential==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("Credential cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localCredential);
                                            
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

                
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "Bucket"));
                                 
                                        if (localBucket != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localBucket));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("Bucket cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "Key"));
                                 
                                        if (localKey != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localKey));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("Key cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "GetMetadata"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localGetMetadata));
                            
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "GetData"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localGetData));
                            
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "InlineData"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInlineData));
                             if (localByteRangeStartTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "ByteRangeStart"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localByteRangeStart));
                            } if (localByteRangeEndTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "ByteRangeEnd"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localByteRangeEnd));
                            } if (localIfModifiedSinceTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "IfModifiedSince"));
                                 
                                        if (localIfModifiedSince != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIfModifiedSince));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("IfModifiedSince cannot be null!!");
                                        }
                                    } if (localIfUnmodifiedSinceTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "IfUnmodifiedSince"));
                                 
                                        if (localIfUnmodifiedSince != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIfUnmodifiedSince));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("IfUnmodifiedSince cannot be null!!");
                                        }
                                    } if (localIfMatchTracker){
                            if (localIfMatch!=null){
                                  for (int i = 0;i < localIfMatch.length;i++){
                                      
                                         if (localIfMatch[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                              "IfMatch"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIfMatch[i]));
                                          } else {
                                             
                                                    // have to do nothing
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    throw new org.apache.axis2.databinding.ADBException("IfMatch cannot be null!!");
                                
                            }

                        } if (localIfNoneMatchTracker){
                            if (localIfNoneMatch!=null){
                                  for (int i = 0;i < localIfNoneMatch.length;i++){
                                      
                                         if (localIfNoneMatch[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                              "IfNoneMatch"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIfNoneMatch[i]));
                                          } else {
                                             
                                                    // have to do nothing
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    throw new org.apache.axis2.databinding.ADBException("IfNoneMatch cannot be null!!");
                                
                            }

                        } if (localReturnCompleteObjectOnConditionFailureTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "ReturnCompleteObjectOnConditionFailure"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localReturnCompleteObjectOnConditionFailure));
                            } if (localAWSAccessKeyIdTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "AWSAccessKeyId"));
                                 
                                        if (localAWSAccessKeyId != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAWSAccessKeyId));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("AWSAccessKeyId cannot be null!!");
                                        }
                                    } if (localTimestampTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "Timestamp"));
                                 
                                        if (localTimestamp != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localTimestamp));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("Timestamp cannot be null!!");
                                        }
                                    } if (localSignatureTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "Signature"));
                                 
                                        if (localSignature != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSignature));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("Signature cannot be null!!");
                                        }
                                    } if (localCredentialTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "Credential"));
                                 
                                        if (localCredential != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCredential));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("Credential cannot be null!!");
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
        public static GetObjectExtended parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            GetObjectExtended object =
                new GetObjectExtended();

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
                    
                            if (!"GetObjectExtended".equals(type)){
                                //find namespace for the prefix
                                java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                                return (GetObjectExtended)com.amazon.s3.ExtensionMapper.getTypeObject(
                                     nsUri,type,reader);
                              }
                        

                  }
                

                }

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                 
                    
                    reader.next();
                
                        java.util.ArrayList list10 = new java.util.ArrayList();
                    
                        java.util.ArrayList list11 = new java.util.ArrayList();
                    
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Bucket").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setBucket(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Key").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setKey(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","GetMetadata").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setGetMetadata(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","GetData").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setGetData(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","InlineData").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setInlineData(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","ByteRangeStart").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setByteRangeStart(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToLong(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                               object.setByteRangeStart(java.lang.Long.MIN_VALUE);
                                           
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","ByteRangeEnd").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setByteRangeEnd(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToLong(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                               object.setByteRangeEnd(java.lang.Long.MIN_VALUE);
                                           
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","IfModifiedSince").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setIfModifiedSince(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","IfUnmodifiedSince").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setIfUnmodifiedSince(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","IfMatch").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    list10.add(reader.getElementText());
                                            
                                            //loop until we find a start element that is not part of this array
                                            boolean loopDone10 = false;
                                            while(!loopDone10){
                                                // Ensure we are at the EndElement
                                                while (!reader.isEndElement()){
                                                    reader.next();
                                                }
                                                // Step out of this element
                                                reader.next();
                                                // Step to next element event.
                                                while (!reader.isStartElement() && !reader.isEndElement())
                                                    reader.next();
                                                if (reader.isEndElement()){
                                                    //two continuous end elements means we are exiting the xml structure
                                                    loopDone10 = true;
                                                } else {
                                                    if (new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","IfMatch").equals(reader.getName())){
                                                         list10.add(reader.getElementText());
                                                        
                                                    }else{
                                                        loopDone10 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setIfMatch((java.lang.String[])
                                                        list10.toArray(new java.lang.String[list10.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","IfNoneMatch").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    list11.add(reader.getElementText());
                                            
                                            //loop until we find a start element that is not part of this array
                                            boolean loopDone11 = false;
                                            while(!loopDone11){
                                                // Ensure we are at the EndElement
                                                while (!reader.isEndElement()){
                                                    reader.next();
                                                }
                                                // Step out of this element
                                                reader.next();
                                                // Step to next element event.
                                                while (!reader.isStartElement() && !reader.isEndElement())
                                                    reader.next();
                                                if (reader.isEndElement()){
                                                    //two continuous end elements means we are exiting the xml structure
                                                    loopDone11 = true;
                                                } else {
                                                    if (new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","IfNoneMatch").equals(reader.getName())){
                                                         list11.add(reader.getElementText());
                                                        
                                                    }else{
                                                        loopDone11 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setIfNoneMatch((java.lang.String[])
                                                        list11.toArray(new java.lang.String[list11.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","ReturnCompleteObjectOnConditionFailure").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setReturnCompleteObjectOnConditionFailure(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","AWSAccessKeyId").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setAWSAccessKeyId(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Timestamp").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setTimestamp(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Signature").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSignature(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Credential").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setCredential(
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
           
          