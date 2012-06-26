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
 * ListVersionsResult.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:34 EDT)
 */
            
                package com.amazon.s3;
            

            /**
            *  ListVersionsResult bean class
            */
        
        public  class ListVersionsResult
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = ListVersionsResult
                Namespace URI = http://s3.amazonaws.com/doc/2006-03-01/
                Namespace Prefix = ns1
                */
            

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://s3.amazonaws.com/doc/2006-03-01/")){
                return "ns1";
            }
            return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
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
                        * field for Name
                        */

                        
                                    protected java.lang.String localName ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getName(){
                               return localName;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Name
                               */
                               public void setName(java.lang.String param){
                            
                                            this.localName=param;
                                    

                               }
                            

                        /**
                        * field for Prefix
                        */

                        
                                    protected java.lang.String localPrefix ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getPrefix(){
                               return localPrefix;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Prefix
                               */
                               public void setPrefix(java.lang.String param){
                            
                                            this.localPrefix=param;
                                    

                               }
                            

                        /**
                        * field for KeyMarker
                        */

                        
                                    protected java.lang.String localKeyMarker ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getKeyMarker(){
                               return localKeyMarker;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param KeyMarker
                               */
                               public void setKeyMarker(java.lang.String param){
                            
                                            this.localKeyMarker=param;
                                    

                               }
                            

                        /**
                        * field for VersionIdMarker
                        */

                        
                                    protected java.lang.String localVersionIdMarker ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getVersionIdMarker(){
                               return localVersionIdMarker;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param VersionIdMarker
                               */
                               public void setVersionIdMarker(java.lang.String param){
                            
                                            this.localVersionIdMarker=param;
                                    

                               }
                            

                        /**
                        * field for NextKeyMarker
                        */

                        
                                    protected java.lang.String localNextKeyMarker ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localNextKeyMarkerTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getNextKeyMarker(){
                               return localNextKeyMarker;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param NextKeyMarker
                               */
                               public void setNextKeyMarker(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localNextKeyMarkerTracker = true;
                                       } else {
                                          localNextKeyMarkerTracker = false;
                                              
                                       }
                                   
                                            this.localNextKeyMarker=param;
                                    

                               }
                            

                        /**
                        * field for NextVersionIdMarker
                        */

                        
                                    protected java.lang.String localNextVersionIdMarker ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localNextVersionIdMarkerTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getNextVersionIdMarker(){
                               return localNextVersionIdMarker;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param NextVersionIdMarker
                               */
                               public void setNextVersionIdMarker(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localNextVersionIdMarkerTracker = true;
                                       } else {
                                          localNextVersionIdMarkerTracker = false;
                                              
                                       }
                                   
                                            this.localNextVersionIdMarker=param;
                                    

                               }
                            

                        /**
                        * field for MaxKeys
                        */

                        
                                    protected int localMaxKeys ;
                                

                           /**
                           * Auto generated getter method
                           * @return int
                           */
                           public  int getMaxKeys(){
                               return localMaxKeys;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param MaxKeys
                               */
                               public void setMaxKeys(int param){
                            
                                            this.localMaxKeys=param;
                                    

                               }
                            

                        /**
                        * field for Delimiter
                        */

                        
                                    protected java.lang.String localDelimiter ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDelimiterTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getDelimiter(){
                               return localDelimiter;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Delimiter
                               */
                               public void setDelimiter(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localDelimiterTracker = true;
                                       } else {
                                          localDelimiterTracker = false;
                                              
                                       }
                                   
                                            this.localDelimiter=param;
                                    

                               }
                            

                        /**
                        * field for IsTruncated
                        */

                        
                                    protected boolean localIsTruncated ;
                                

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getIsTruncated(){
                               return localIsTruncated;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param IsTruncated
                               */
                               public void setIsTruncated(boolean param){
                            
                                            this.localIsTruncated=param;
                                    

                               }
                            

                        /**
                        * field for ListVersionsResultChoice_type0
                        * This was an Array!
                        */

                        
                                    protected com.amazon.s3.ListVersionsResultChoice_type0[] localListVersionsResultChoice_type0 ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localListVersionsResultChoice_type0Tracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.s3.ListVersionsResultChoice_type0[]
                           */
                           public  com.amazon.s3.ListVersionsResultChoice_type0[] getListVersionsResultChoice_type0(){
                               return localListVersionsResultChoice_type0;
                           }

                           
                        


                               
                              /**
                               * validate the array for ListVersionsResultChoice_type0
                               */
                              protected void validateListVersionsResultChoice_type0(com.amazon.s3.ListVersionsResultChoice_type0[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param ListVersionsResultChoice_type0
                              */
                              public void setListVersionsResultChoice_type0(com.amazon.s3.ListVersionsResultChoice_type0[] param){
                              
                                   validateListVersionsResultChoice_type0(param);

                               
                                          if (param != null){
                                             //update the setting tracker
                                             localListVersionsResultChoice_type0Tracker = true;
                                          } else {
                                             localListVersionsResultChoice_type0Tracker = false;
                                                 
                                          }
                                      
                                      this.localListVersionsResultChoice_type0=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param com.amazon.s3.ListVersionsResultChoice_type0
                             */
                             public void addListVersionsResultChoice_type0(com.amazon.s3.ListVersionsResultChoice_type0 param){
                                   if (localListVersionsResultChoice_type0 == null){
                                   localListVersionsResultChoice_type0 = new com.amazon.s3.ListVersionsResultChoice_type0[]{};
                                   }

                            
                                 //update the setting tracker
                                localListVersionsResultChoice_type0Tracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localListVersionsResultChoice_type0);
                               list.add(param);
                               this.localListVersionsResultChoice_type0 =
                             (com.amazon.s3.ListVersionsResultChoice_type0[])list.toArray(
                            new com.amazon.s3.ListVersionsResultChoice_type0[list.size()]);

                             }
                             

                        /**
                        * field for CommonPrefixes
                        * This was an Array!
                        */

                        
                                    protected com.amazon.s3.PrefixEntry[] localCommonPrefixes ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localCommonPrefixesTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.s3.PrefixEntry[]
                           */
                           public  com.amazon.s3.PrefixEntry[] getCommonPrefixes(){
                               return localCommonPrefixes;
                           }

                           
                        


                               
                              /**
                               * validate the array for CommonPrefixes
                               */
                              protected void validateCommonPrefixes(com.amazon.s3.PrefixEntry[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param CommonPrefixes
                              */
                              public void setCommonPrefixes(com.amazon.s3.PrefixEntry[] param){
                              
                                   validateCommonPrefixes(param);

                               
                                          if (param != null){
                                             //update the setting tracker
                                             localCommonPrefixesTracker = true;
                                          } else {
                                             localCommonPrefixesTracker = false;
                                                 
                                          }
                                      
                                      this.localCommonPrefixes=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param com.amazon.s3.PrefixEntry
                             */
                             public void addCommonPrefixes(com.amazon.s3.PrefixEntry param){
                                   if (localCommonPrefixes == null){
                                   localCommonPrefixes = new com.amazon.s3.PrefixEntry[]{};
                                   }

                            
                                 //update the setting tracker
                                localCommonPrefixesTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localCommonPrefixes);
                               list.add(param);
                               this.localCommonPrefixes =
                             (com.amazon.s3.PrefixEntry[])list.toArray(
                            new com.amazon.s3.PrefixEntry[list.size()]);

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
                       ListVersionsResult.this.serialize(parentQName,factory,xmlWriter);
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
               

                   java.lang.String namespacePrefix = registerPrefix(xmlWriter,"http://s3.amazonaws.com/doc/2006-03-01/");
                   if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)){
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           namespacePrefix+":ListVersionsResult",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "ListVersionsResult",
                           xmlWriter);
                   }

               
                   }
                if (localMetadataTracker){
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
                                 }
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"Name", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"Name");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("Name");
                                    }
                                

                                          if (localName==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("Name cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localName);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"Prefix", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"Prefix");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("Prefix");
                                    }
                                

                                          if (localPrefix==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("Prefix cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localPrefix);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"KeyMarker", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"KeyMarker");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("KeyMarker");
                                    }
                                

                                          if (localKeyMarker==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("KeyMarker cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localKeyMarker);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"VersionIdMarker", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"VersionIdMarker");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("VersionIdMarker");
                                    }
                                

                                          if (localVersionIdMarker==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("VersionIdMarker cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localVersionIdMarker);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                              if (localNextKeyMarkerTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"NextKeyMarker", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"NextKeyMarker");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("NextKeyMarker");
                                    }
                                

                                          if (localNextKeyMarker==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("NextKeyMarker cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localNextKeyMarker);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localNextVersionIdMarkerTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"NextVersionIdMarker", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"NextVersionIdMarker");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("NextVersionIdMarker");
                                    }
                                

                                          if (localNextVersionIdMarker==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("NextVersionIdMarker cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localNextVersionIdMarker);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             }
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"MaxKeys", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"MaxKeys");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("MaxKeys");
                                    }
                                
                                               if (localMaxKeys==java.lang.Integer.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("MaxKeys cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMaxKeys));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                              if (localDelimiterTracker){
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"Delimiter", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"Delimiter");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("Delimiter");
                                    }
                                

                                          if (localDelimiter==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("Delimiter cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localDelimiter);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             }
                                    namespace = "http://s3.amazonaws.com/doc/2006-03-01/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"IsTruncated", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"IsTruncated");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("IsTruncated");
                                    }
                                
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("IsTruncated cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIsTruncated));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                              if (localListVersionsResultChoice_type0Tracker){
                                     
                                      if (localListVersionsResultChoice_type0!=null){
                                            for (int i = 0;i < localListVersionsResultChoice_type0.length;i++){
                                                if (localListVersionsResultChoice_type0[i] != null){
                                                 localListVersionsResultChoice_type0[i].serialize(null,factory,xmlWriter);
                                                } else {
                                                   
                                                        // we don't have to do any thing since minOccures is zero
                                                    
                                                }

                                            }
                                     } else {
                                        throw new org.apache.axis2.databinding.ADBException("ListVersionsResultChoice_type0 cannot be null!!");
                                     }
                                 } if (localCommonPrefixesTracker){
                                       if (localCommonPrefixes!=null){
                                            for (int i = 0;i < localCommonPrefixes.length;i++){
                                                if (localCommonPrefixes[i] != null){
                                                 localCommonPrefixes[i].serialize(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CommonPrefixes"),
                                                           factory,xmlWriter);
                                                } else {
                                                   
                                                        // we don't have to do any thing since minOccures is zero
                                                    
                                                }

                                            }
                                     } else {
                                        
                                               throw new org.apache.axis2.databinding.ADBException("CommonPrefixes cannot be null!!");
                                        
                                    }
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

                 if (localMetadataTracker){
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

                        }
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "Name"));
                                 
                                        if (localName != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localName));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("Name cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "Prefix"));
                                 
                                        if (localPrefix != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPrefix));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("Prefix cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "KeyMarker"));
                                 
                                        if (localKeyMarker != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localKeyMarker));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("KeyMarker cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "VersionIdMarker"));
                                 
                                        if (localVersionIdMarker != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localVersionIdMarker));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("VersionIdMarker cannot be null!!");
                                        }
                                     if (localNextKeyMarkerTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "NextKeyMarker"));
                                 
                                        if (localNextKeyMarker != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNextKeyMarker));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("NextKeyMarker cannot be null!!");
                                        }
                                    } if (localNextVersionIdMarkerTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "NextVersionIdMarker"));
                                 
                                        if (localNextVersionIdMarker != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNextVersionIdMarker));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("NextVersionIdMarker cannot be null!!");
                                        }
                                    }
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "MaxKeys"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMaxKeys));
                             if (localDelimiterTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "Delimiter"));
                                 
                                        if (localDelimiter != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDelimiter));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("Delimiter cannot be null!!");
                                        }
                                    }
                                      elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                      "IsTruncated"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localIsTruncated));
                             if (localListVersionsResultChoice_type0Tracker){
                             if (localListVersionsResultChoice_type0!=null) {
                                 for (int i = 0;i < localListVersionsResultChoice_type0.length;i++){

                                    if (localListVersionsResultChoice_type0[i] != null){
                                         elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                          "ListVersionsResultChoice_type0"));
                                         elementList.add(localListVersionsResultChoice_type0[i]);
                                    } else {
                                        
                                                // nothing to do
                                            
                                    }

                                 }
                             } else {
                                 
                                        throw new org.apache.axis2.databinding.ADBException("ListVersionsResultChoice_type0 cannot be null!!");
                                    
                             }

                        } if (localCommonPrefixesTracker){
                             if (localCommonPrefixes!=null) {
                                 for (int i = 0;i < localCommonPrefixes.length;i++){

                                    if (localCommonPrefixes[i] != null){
                                         elementList.add(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/",
                                                                          "CommonPrefixes"));
                                         elementList.add(localCommonPrefixes[i]);
                                    } else {
                                        
                                                // nothing to do
                                            
                                    }

                                 }
                             } else {
                                 
                                        throw new org.apache.axis2.databinding.ADBException("CommonPrefixes cannot be null!!");
                                    
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
        public static ListVersionsResult parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            ListVersionsResult object =
                new ListVersionsResult();

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
                    
                            if (!"ListVersionsResult".equals(type)){
                                //find namespace for the prefix
                                java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                                return (ListVersionsResult)com.amazon.s3.ExtensionMapper.getTypeObject(
                                     nsUri,type,reader);
                              }
                        

                  }
                

                }

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                 
                    
                    reader.next();
                
                        java.util.ArrayList list1 = new java.util.ArrayList();
                    
                        java.util.ArrayList list11 = new java.util.ArrayList();
                    
                        java.util.ArrayList list12 = new java.util.ArrayList();
                    
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Metadata").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    list1.add(com.amazon.s3.MetadataEntry.Factory.parse(reader));
                                                                
                                                        //loop until we find a start element that is not part of this array
                                                        boolean loopDone1 = false;
                                                        while(!loopDone1){
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
                                                                loopDone1 = true;
                                                            } else {
                                                                if (new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Metadata").equals(reader.getName())){
                                                                    list1.add(com.amazon.s3.MetadataEntry.Factory.parse(reader));
                                                                        
                                                                }else{
                                                                    loopDone1 = true;
                                                                }
                                                            }
                                                        }
                                                        // call the converter utility  to convert and set the array
                                                        
                                                        object.setMetadata((com.amazon.s3.MetadataEntry[])
                                                            org.apache.axis2.databinding.utils.ConverterUtil.convertToArray(
                                                                com.amazon.s3.MetadataEntry.class,
                                                                list1));
                                                            
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Name").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setName(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Prefix").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setPrefix(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","KeyMarker").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setKeyMarker(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","VersionIdMarker").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setVersionIdMarker(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","NextKeyMarker").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setNextKeyMarker(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","NextVersionIdMarker").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setNextVersionIdMarker(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","MaxKeys").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setMaxKeys(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","Delimiter").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDelimiter(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","IsTruncated").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setIsTruncated(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                        
                                         try{
                                    
                                    if (reader.isStartElement() ){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    list11.add(com.amazon.s3.ListVersionsResultChoice_type0.Factory.parse(reader));
                                                        //loop until we find a start element that is not part of this array
                                                        boolean loopDone11 = false;
                                                        while(!loopDone11){

                                                            // Step to next element event.
                                                            while (!reader.isStartElement() && !reader.isEndElement())
                                                                reader.next();
                                                            if (reader.isEndElement()){
                                                                //two continuous end elements means we are exiting the xml structure
                                                                loopDone11 = true;
                                                            } else {
                                                                list11.add(com.amazon.s3.ListVersionsResultChoice_type0.Factory.parse(reader));
                                                            }
                                                        }
                                                        // call the converter utility  to convert and set the array
                                                        object.setListVersionsResultChoice_type0((com.amazon.s3.ListVersionsResultChoice_type0[])
                                                            org.apache.axis2.databinding.utils.ConverterUtil.convertToArray(
                                                                com.amazon.s3.ListVersionsResultChoice_type0.class,
                                                                list11));

                                                 
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                
                                 } catch (java.lang.Exception e) {}
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CommonPrefixes").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    list12.add(com.amazon.s3.PrefixEntry.Factory.parse(reader));
                                                                
                                                        //loop until we find a start element that is not part of this array
                                                        boolean loopDone12 = false;
                                                        while(!loopDone12){
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
                                                                loopDone12 = true;
                                                            } else {
                                                                if (new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/","CommonPrefixes").equals(reader.getName())){
                                                                    list12.add(com.amazon.s3.PrefixEntry.Factory.parse(reader));
                                                                        
                                                                }else{
                                                                    loopDone12 = true;
                                                                }
                                                            }
                                                        }
                                                        // call the converter utility  to convert and set the array
                                                        
                                                        object.setCommonPrefixes((com.amazon.s3.PrefixEntry[])
                                                            org.apache.axis2.databinding.utils.ConverterUtil.convertToArray(
                                                                com.amazon.s3.PrefixEntry.class,
                                                                list12));
                                                            
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
           
          