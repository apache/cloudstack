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
 * CopyObject.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:34 EDT)
 */
            
                package com.amazon.s3;
            

            /**
            *  CopyObject bean class
            */
        
        public  class CopyObject
        implements org.apache.axis2.databinding.ADBBean{
        
                public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName(
                "http://s3.amazonaws.com/doc/2006-03-01/",
                "CopyObject",
                "ns1");

            

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://s3.amazonaws.com/doc/2006-03-01/")){
                return "ns1";
            }
            return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
        }

        

                        /**
                        * field for SourceBucket
                        */

                        
                                    protected java.lang.String localSourceBucket ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getSourceBucket(){
                               return localSourceBucket;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param SourceBucket
                               */
                               public void setSourceBucket(java.lang.String param){
                            
                                            this.localSourceBucket=param;
                                    

                               }
                            

                        /**
                        * field for SourceKey
                        */

                        
                                    protected java.lang.String localSourceKey ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getSourceKey(){
                               return localSourceKey;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param SourceKey
                               */
                               public void setSourceKey(java.lang.String param){
                            
                                            this.localSourceKey=param;
                                    

                               }
                            

                        /**
                        * field for DestinationBucket
                        */

                        
                                    protected java.lang.String localDestinationBucket ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getDestinationBucket(){
                               return localDestinationBucket;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param DestinationBucket
                               */
                               public void setDestinationBucket(java.lang.String param){
                            
                                            this.localDestinationBucket=param;
                                    

                               }
                            

                        /**
                        * field for DestinationKey
                        */

                        
                                    protected java.lang.String localDestinationKey ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getDestinationKey(){
                               return localDestinationKey;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param DestinationKey
                               */
                               public void setDestinationKey(java.lang.String param){
                            
                                            this.localDestinationKey=param;
                                    

                               }
                            

                        /**
                        * field for MetadataDirective
                        */

                        
                                    protected com.amazon.s3.MetadataDirective localMetadataDirective ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localMetadataDirectiveTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.s3.MetadataDirective
                           */
                           public  com.amazon.s3.MetadataDirective getMetadataDirective(){
                               return localMetadataDirective;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param MetadataDirective
                               */
                               public void setMetadataDirective(com.amazon.s3.MetadataDirective param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localMetadataDirectiveTracker = true;
                                       } else {
                                          localMetadataDirectiveTracker = false;
                                              
                                       }
                                   
                                            this.localMetadataDirective=param;
                                    

                               }
                            

                        /**
                        * field for Metadata
                        * This was an Array!
                        */

                        
                                    protected com.amazon.s3.MetadataEntry[] localMetadata ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localMetadataTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.s3.MetadataEntry[]
                           */
                           public  com.amazon.s3.MetadataEntry[] getMetadata(){
                               return localMetadata;
                           }

                           
                        


                               
                              /**
                               * validate the array for Metadata
                               */
                              protected void validateMetadata(com.amazon.s3.MetadataEntry[] param){
                             
                              if ((param != null) && (param.length > 100)){
                                throw new java.lang.RuntimeException();
                              }
                              
                              }


                             /**
                              * Auto generated setter method
                              * @param param Metadata
                              */
                              public void setMetadata(com.amazon.s3.MetadataEntry[] param){
                              
                                   validateMetadata(param);

                               
                                          if (param != null){
                                             //update the setting tracker
                                             localMetadataTracker = true;
                                          } else {
                                             localMetadataTracker = false;
                                                 
                                          }
                                      
                                      this.localMetadata=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param com.amazon.s3.MetadataEntry
                             */
                             public void addMetadata(com.amazon.s3.MetadataEntry param){
                                   if (localMetadata == null){
                                   localMetadata = new com.amazon.s3.MetadataEntry[]{};
                                   }

                            
                                 //update the setting tracker
                                localMetadataTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localMetadata);
                               list.add(param);
                               this.localMetadata =
                             (com.amazon.s3.MetadataEntry[])list.toArray(
                            new com.amazon.s3.MetadataEntry[list.size()]);

                             }
                             

                        /**
                        * field for AccessControlList
                        */

                        
                                    protected com.amazon.s3.AccessControlList localAccessControlList ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localAccessControlListTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.s3.AccessControlList
                           */
                           public  com.amazon.s3.AccessControlList getAccessControlList(){
                               return localAccessControlList;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param AccessControlList
                               */
                               public void setAccessControlList(com.amazon.s3.AccessControlList param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localAccessControlListTracker = true;
                                       } else {
                                          localAccessControlListTracker = false;
                                              
                                       }
                                   
                                            this.localAccessControlList=param;
                                    

                               }
                            

                        /**
                        * field for CopySourceIfModifiedSince
                        */

                        
                                    protected java.util.Calendar localCopySourceIfModifiedSince ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localCopySourceIfModifiedSinceTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.util.Calendar
                           */
                           public  java.util.Calendar getCopySourceIfModifiedSince(){
                               return localCopySourceIfModifiedSince;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param CopySourceIfModifiedSince
                               */
                               public void setCopySourceIfModifiedSince(java.util.Calendar param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localCopySourceIfModifiedSinceTracker = true;
                                       } else {
                                          localCopySourceIfModifiedSinceTracker = false;
                                              
                                       }
                                   
                                            this.localCopySourceIfModifiedSince=param;
                                    

                               }
                            

                        /**
                        * field for CopySourceIfUnmodifiedSince
                        */

                        
                                    protected java.util.Calendar localCopySourceIfUnmodifiedSince ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localCopySourceIfUnmodifiedSinceTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.util.Calendar
                           */
                           public  java.util.Calendar getCopySourceIfUnmodifiedSince(){
                               return localCopySourceIfUnmodifiedSince;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param CopySourceIfUnmodifiedSince
                               */
                               public void setCopySourceIfUnmodifiedSince(java.util.Calendar param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localCopySourceIfUnmodifiedSinceTracker = true;
                                       } else {
                                          localCopySourceIfUnmodifiedSinceTracker = false;
                                              
                                       }
                                   
                                            this.localCopySourceIfUnmodifiedSince=param;
                                    

                               }
                            

                        /**
                        * field for CopySourceIfMatch
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localCopySourceIfMatch ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localCopySourceIfMatchTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getCopySourceIfMatch(){
                               return localCopySourceIfMatch;
                           }

                           
                        


                               
                              /**
                               * validate the array for CopySourceIfMatch
                               */
                              protected void validateCopySourceIfMatch(java.lang.String[] param){
                             
                              if ((param != null) && (param.length > 100)){
                                throw new java.lang.RuntimeException();
                              }
                              
                              }


                             /**
                              * Auto generated setter method
                              * @param param CopySourceIfMatch
                              */
                              public void setCopySourceIfMatch(java.lang.String[] param){
                              
                                   validateCopySourceIfMatch(param);

                               
                                          if (param != null){
                                             //update the setting tracker
                                             localCopySourceIfMatchTracker = true;
                                          } else {
                                             localCopySourceIfMatchTracker = false;
                                                 
                                          }
                                      
                                      this.localCopySourceIfMatch=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addCopySourceIfMatch(java.lang.String param){
                                   if (localCopySourceIfMatch == null){
                                   localCopySourceIfMatch = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localCopySourceIfMatchTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localCopySourceIfMatch);
                               list.add(param);
                               this.localCopySourceIfMatch =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for CopySourceIfNoneMatch
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localCopySourceIfNoneMatch ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localCopySourceIfNoneMatchTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getCopySourceIfNoneMatch(){
                               return localCopySourceIfNoneMatch;
                           }

                           
                        


                               
                              /**
                               * validate the array for CopySourceIfNoneMatch
                               */
                              protected void validateCopySourceIfNoneMatch(java.lang.String[] param){
                             
                              if ((param != null) && (param.length > 100)){
                                throw new java.lang.RuntimeException();
                              }
                              
                              }


                             /**
                              * Auto generated setter method
                              * @param param CopySourceIfNoneMatch
                              */
                              public void setCopySourceIfNoneMatch(java.lang.String[] param){
                              
                                   validateCopySourceIfNoneMatch(param);

                               
                                          if (param != null){
                                             //update the setting tracker
                                             localCopySourceIfNoneMatchTracker = true;
                                          } else {
                                             localCopySourceIfNoneMatchTracker = false;
                                                 
                                          }
                                      
                                      this.localCopySourceIfNoneMatch=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addCopySourceIfNoneMatch(java.lang.String param){
                                   if (localCopySourceIfNoneMatch == null){
                                   localCopySourceIfNoneMatch = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localCopySourceIfNoneMatchTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localCopySourceIfNoneMatch);
                               list.add(param);
                               this.localCopySourceIfNoneMatch =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for StorageClass
                        */

                        
                                    protected com.amazon.s3.StorageClass localStorageClass ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localStorageClassTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.s3.StorageClass
                           */
                           public  com.amazon.s3.StorageClass getStorageClass(){
                               return localStorageClass;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param StorageClass
                               */
                               public void setStorageClass(com.amazon.s3.StorageClass param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localStorageClassTracker = true;
                                       } else {
                                          localStorageClassTracker = false;
                                              
                                       }
                                   
                                            this.localStorageClass=param;
                                    

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
                       CopyObject.this.serialize(MY_QNAME,factory,xmlWriter);
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
                           namespacePrefix+":CopyObject",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "CopyObject",
                           xmlWriter);
                   }

               
                   }
               
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"SourceBucket", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"SourceBucket");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("SourceBucket");
                                    }
                                

                                          if (localSourceBucket==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("SourceBucket cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localSourceBucket);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"SourceKey", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"SourceKey");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("SourceKey");
                                    }
                                

                                          if (localSourceKey==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("SourceKey cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localSourceKey);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"DestinationBucket", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"DestinationBucket");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("DestinationBucket");
                                    }
                                

                                          if (localDestinationBucket==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("DestinationBucket cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localDestinationBucket);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"DestinationKey", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"DestinationKey");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("DestinationKey");
                                    }
                                

                                          if (localDestinationKey==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("DestinationKey cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localDestinationKey);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                              if (localMetadataDirectiveTracker){
                                            if (localMetadataDirective==null){
                                                 throw new org.apache.axis2.databinding.ADBException("MetadataDirective cannot be null!!");
                                            }
                                           localMetadataDirective.serialize(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","MetadataDirective"),
                                               factory,xmlWriter);
                                        } if (localMetadataTracker){
                                       if (localMetadata!=null){
                                            for (int i = 0;i < localMetadata.length;i++){
                                                if (localMetadata[i] != null){
                                                 localMetadata[i].serialize(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Metadata"),
                                                           factory,xmlWriter);
                                                } else {
                                                   
                                                        // we don't have to do any thing since minOccures is zero
                                                    
                                                }

                                            }
                                     } else {
                                        
                                               throw new org.apache.axis2.databinding.ADBException("Metadata cannot be null!!");
                                        
                                    }
                                 } if (localAccessControlListTracker){
                                            if (localAccessControlList==null){
                                                 throw new org.apache.axis2.databinding.ADBException("AccessControlList cannot be null!!");
                                            }
                                           localAccessControlList.serialize(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","AccessControlList"),
                                               factory,xmlWriter);
                                        } if (localCopySourceIfModifiedSinceTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"CopySourceIfModifiedSince", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"CopySourceIfModifiedSince");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("CopySourceIfModifiedSince");
                                    }
                                

                                          if (localCopySourceIfModifiedSince==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("CopySourceIfModifiedSince cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCopySourceIfModifiedSince));
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localCopySourceIfUnmodifiedSinceTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"CopySourceIfUnmodifiedSince", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"CopySourceIfUnmodifiedSince");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("CopySourceIfUnmodifiedSince");
                                    }
                                

                                          if (localCopySourceIfUnmodifiedSince==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("CopySourceIfUnmodifiedSince cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCopySourceIfUnmodifiedSince));
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localCopySourceIfMatchTracker){
                             if (localCopySourceIfMatch!=null) {
                                   namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                   boolean emptyNamespace = namespace == null || namespace.length() == 0;
                                   prefix =  emptyNamespace ? null : xmlWriter.getPrefix(namespace);
                                   for (int i = 0;i < localCopySourceIfMatch.length;i++){
                                        
                                            if (localCopySourceIfMatch[i] != null){
                                        
                                                if (!emptyNamespace) {
                                                    if (prefix == null) {
                                                        java.lang.String prefix2 = generatePrefix(namespace);

                                                        xmlWriter.writeStartElement(prefix2,"CopySourceIfMatch", namespace);
                                                        xmlWriter.writeNamespace(prefix2, namespace);
                                                        xmlWriter.setPrefix(prefix2, namespace);

                                                    } else {
                                                        xmlWriter.writeStartElement(namespace,"CopySourceIfMatch");
                                                    }

                                                } else {
                                                    xmlWriter.writeStartElement("CopySourceIfMatch");
                                                }

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCopySourceIfMatch[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // we have to do nothing since minOccurs is zero
                                                       
                                                }

                                   }
                             } else {
                                 
                                         throw new org.apache.axis2.databinding.ADBException("CopySourceIfMatch cannot be null!!");
                                    
                             }

                        } if (localCopySourceIfNoneMatchTracker){
                             if (localCopySourceIfNoneMatch!=null) {
                                   namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                   boolean emptyNamespace = namespace == null || namespace.length() == 0;
                                   prefix =  emptyNamespace ? null : xmlWriter.getPrefix(namespace);
                                   for (int i = 0;i < localCopySourceIfNoneMatch.length;i++){
                                        
                                            if (localCopySourceIfNoneMatch[i] != null){
                                        
                                                if (!emptyNamespace) {
                                                    if (prefix == null) {
                                                        java.lang.String prefix2 = generatePrefix(namespace);

                                                        xmlWriter.writeStartElement(prefix2,"CopySourceIfNoneMatch", namespace);
                                                        xmlWriter.writeNamespace(prefix2, namespace);
                                                        xmlWriter.setPrefix(prefix2, namespace);

                                                    } else {
                                                        xmlWriter.writeStartElement(namespace,"CopySourceIfNoneMatch");
                                                    }

                                                } else {
                                                    xmlWriter.writeStartElement("CopySourceIfNoneMatch");
                                                }

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCopySourceIfNoneMatch[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // we have to do nothing since minOccurs is zero
                                                       
                                                }

                                   }
                             } else {
                                 
                                         throw new org.apache.axis2.databinding.ADBException("CopySourceIfNoneMatch cannot be null!!");
                                    
                             }

                        } if (localStorageClassTracker){
                                            if (localStorageClass==null){
                                                 throw new org.apache.axis2.databinding.ADBException("StorageClass cannot be null!!");
                                            }
                                           localStorageClass.serialize(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","StorageClass"),
                                               factory,xmlWriter);
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
                                                                      "SourceBucket"));
                                 
                                        if (localSourceBucket != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSourceBucket));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("SourceBucket cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "SourceKey"));
                                 
                                        if (localSourceKey != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSourceKey));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("SourceKey cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "DestinationBucket"));
                                 
                                        if (localDestinationBucket != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDestinationBucket));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("DestinationBucket cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "DestinationKey"));
                                 
                                        if (localDestinationKey != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDestinationKey));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("DestinationKey cannot be null!!");
                                        }
                                     if (localMetadataDirectiveTracker){
                            elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "MetadataDirective"));
                            
                            
                                    if (localMetadataDirective==null){
                                         throw new org.apache.axis2.databinding.ADBException("MetadataDirective cannot be null!!");
                                    }
                                    elementList.add(localMetadataDirective);
                                } if (localMetadataTracker){
                             if (localMetadata!=null) {
                                 for (int i = 0;i < localMetadata.length;i++){

                                    if (localMetadata[i] != null){
                                         elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                          "Metadata"));
                                         elementList.add(localMetadata[i]);
                                    } else {
                                        
                                                // nothing to do
                                            
                                    }

                                 }
                             } else {
                                 
                                        throw new org.apache.axis2.databinding.ADBException("Metadata cannot be null!!");
                                    
                             }

                        } if (localAccessControlListTracker){
                            elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "AccessControlList"));
                            
                            
                                    if (localAccessControlList==null){
                                         throw new org.apache.axis2.databinding.ADBException("AccessControlList cannot be null!!");
                                    }
                                    elementList.add(localAccessControlList);
                                } if (localCopySourceIfModifiedSinceTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "CopySourceIfModifiedSince"));
                                 
                                        if (localCopySourceIfModifiedSince != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCopySourceIfModifiedSince));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("CopySourceIfModifiedSince cannot be null!!");
                                        }
                                    } if (localCopySourceIfUnmodifiedSinceTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "CopySourceIfUnmodifiedSince"));
                                 
                                        if (localCopySourceIfUnmodifiedSince != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCopySourceIfUnmodifiedSince));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("CopySourceIfUnmodifiedSince cannot be null!!");
                                        }
                                    } if (localCopySourceIfMatchTracker){
                            if (localCopySourceIfMatch!=null){
                                  for (int i = 0;i < localCopySourceIfMatch.length;i++){
                                      
                                         if (localCopySourceIfMatch[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                              "CopySourceIfMatch"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCopySourceIfMatch[i]));
                                          } else {
                                             
                                                    // have to do nothing
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    throw new org.apache.axis2.databinding.ADBException("CopySourceIfMatch cannot be null!!");
                                
                            }

                        } if (localCopySourceIfNoneMatchTracker){
                            if (localCopySourceIfNoneMatch!=null){
                                  for (int i = 0;i < localCopySourceIfNoneMatch.length;i++){
                                      
                                         if (localCopySourceIfNoneMatch[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                              "CopySourceIfNoneMatch"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCopySourceIfNoneMatch[i]));
                                          } else {
                                             
                                                    // have to do nothing
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    throw new org.apache.axis2.databinding.ADBException("CopySourceIfNoneMatch cannot be null!!");
                                
                            }

                        } if (localStorageClassTracker){
                            elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "StorageClass"));
                            
                            
                                    if (localStorageClass==null){
                                         throw new org.apache.axis2.databinding.ADBException("StorageClass cannot be null!!");
                                    }
                                    elementList.add(localStorageClass);
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
        public static CopyObject parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            CopyObject object =
                new CopyObject();

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
                    
                            if (!"CopyObject".equals(type)){
                                //find namespace for the prefix
                                java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                                return (CopyObject)com.amazon.s3.ExtensionMapper.getTypeObject(
                                     nsUri,type,reader);
                              }
                        

                  }
                

                }

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                 
                    
                    reader.next();
                
                        java.util.ArrayList list6 = new java.util.ArrayList();
                    
                        java.util.ArrayList list10 = new java.util.ArrayList();
                    
                        java.util.ArrayList list11 = new java.util.ArrayList();
                    
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","SourceBucket").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSourceBucket(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","SourceKey").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSourceKey(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","DestinationBucket").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDestinationBucket(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","DestinationKey").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDestinationKey(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","MetadataDirective").equals(reader.getName())){
                                
                                                object.setMetadataDirective(com.amazon.s3.MetadataDirective.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Metadata").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    list6.add(com.amazon.s3.MetadataEntry.Factory.parse(reader));
                                                                
                                                        //loop until we find a start element that is not part of this array
                                                        boolean loopDone6 = false;
                                                        while(!loopDone6){
                                                            // We should be at the end element, but make sure
                                                            while (!reader.isEndElement())
                                                                reader.next();
                                                            // Step out of this element
                                                            reader.next();
                                                            // Step to next element event.
                                                            while (!reader.isStartElement() && !reader.isEndElement())
                                                                reader.next();
                                                            if (reader.isEndElement()){
                                                                //two continuous end elements means we are exiting the xml structure
                                                                loopDone6 = true;
                                                            } else {
                                                                if (new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Metadata").equals(reader.getName())){
                                                                    list6.add(com.amazon.s3.MetadataEntry.Factory.parse(reader));
                                                                        
                                                                }else{
                                                                    loopDone6 = true;
                                                                }
                                                            }
                                                        }
                                                        // call the converter utility  to convert and set the array
                                                        
                                                        object.setMetadata((com.amazon.s3.MetadataEntry[])
                                                            org.apache.axis2.databinding.utils.ConverterUtil.convertToArray(
                                                                com.amazon.s3.MetadataEntry.class,
                                                                list6));
                                                            
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","AccessControlList").equals(reader.getName())){
                                
                                                object.setAccessControlList(com.amazon.s3.AccessControlList.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CopySourceIfModifiedSince").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setCopySourceIfModifiedSince(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CopySourceIfUnmodifiedSince").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setCopySourceIfUnmodifiedSince(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CopySourceIfMatch").equals(reader.getName())){
                                
                                    
                                    
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
                                                    if (new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CopySourceIfMatch").equals(reader.getName())){
                                                         list10.add(reader.getElementText());
                                                        
                                                    }else{
                                                        loopDone10 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setCopySourceIfMatch((java.lang.String[])
                                                        list10.toArray(new java.lang.String[list10.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CopySourceIfNoneMatch").equals(reader.getName())){
                                
                                    
                                    
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
                                                    if (new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CopySourceIfNoneMatch").equals(reader.getName())){
                                                         list11.add(reader.getElementText());
                                                        
                                                    }else{
                                                        loopDone11 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setCopySourceIfNoneMatch((java.lang.String[])
                                                        list11.toArray(new java.lang.String[list11.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","StorageClass").equals(reader.getName())){
                                
                                                object.setStorageClass(com.amazon.s3.StorageClass.Factory.parse(reader));
                                              
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
           
          