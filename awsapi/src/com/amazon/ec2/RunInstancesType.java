
/**
 * RunInstancesType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:34 EDT)
 */
            
                package com.amazon.ec2;
            

            /**
            *  RunInstancesType bean class
            */
        
        public  class RunInstancesType
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = RunInstancesType
                Namespace URI = http://ec2.amazonaws.com/doc/2010-11-15/
                Namespace Prefix = ns1
                */
            

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://ec2.amazonaws.com/doc/2010-11-15/")){
                return "ns1";
            }
            return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
        }

        

                        /**
                        * field for ImageId
                        */

                        
                                    protected java.lang.String localImageId ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getImageId(){
                               return localImageId;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ImageId
                               */
                               public void setImageId(java.lang.String param){
                            
                                            this.localImageId=param;
                                    

                               }
                            

                        /**
                        * field for MinCount
                        */

                        
                                    protected int localMinCount ;
                                

                           /**
                           * Auto generated getter method
                           * @return int
                           */
                           public  int getMinCount(){
                               return localMinCount;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param MinCount
                               */
                               public void setMinCount(int param){
                            
                                            this.localMinCount=param;
                                    

                               }
                            

                        /**
                        * field for MaxCount
                        */

                        
                                    protected int localMaxCount ;
                                

                           /**
                           * Auto generated getter method
                           * @return int
                           */
                           public  int getMaxCount(){
                               return localMaxCount;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param MaxCount
                               */
                               public void setMaxCount(int param){
                            
                                            this.localMaxCount=param;
                                    

                               }
                            

                        /**
                        * field for KeyName
                        */

                        
                                    protected java.lang.String localKeyName ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localKeyNameTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getKeyName(){
                               return localKeyName;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param KeyName
                               */
                               public void setKeyName(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localKeyNameTracker = true;
                                       } else {
                                          localKeyNameTracker = false;
                                              
                                       }
                                   
                                            this.localKeyName=param;
                                    

                               }
                            

                        /**
                        * field for GroupSet
                        */

                        
                                    protected com.amazon.ec2.GroupSetType localGroupSet ;
                                

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.GroupSetType
                           */
                           public  com.amazon.ec2.GroupSetType getGroupSet(){
                               return localGroupSet;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param GroupSet
                               */
                               public void setGroupSet(com.amazon.ec2.GroupSetType param){
                            
                                            this.localGroupSet=param;
                                    

                               }
                            

                        /**
                        * field for AdditionalInfo
                        */

                        
                                    protected java.lang.String localAdditionalInfo ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localAdditionalInfoTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getAdditionalInfo(){
                               return localAdditionalInfo;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param AdditionalInfo
                               */
                               public void setAdditionalInfo(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localAdditionalInfoTracker = true;
                                       } else {
                                          localAdditionalInfoTracker = false;
                                              
                                       }
                                   
                                            this.localAdditionalInfo=param;
                                    

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
                        * field for AddressingType
                        */

                        
                                    protected java.lang.String localAddressingType ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localAddressingTypeTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getAddressingType(){
                               return localAddressingType;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param AddressingType
                               */
                               public void setAddressingType(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localAddressingTypeTracker = true;
                                       } else {
                                          localAddressingTypeTracker = false;
                                              
                                       }
                                   
                                            this.localAddressingType=param;
                                    

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

                        
                                    protected com.amazon.ec2.PlacementRequestType localPlacement ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localPlacementTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.PlacementRequestType
                           */
                           public  com.amazon.ec2.PlacementRequestType getPlacement(){
                               return localPlacement;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Placement
                               */
                               public void setPlacement(com.amazon.ec2.PlacementRequestType param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localPlacementTracker = true;
                                       } else {
                                          localPlacementTracker = false;
                                              
                                       }
                                   
                                            this.localPlacement=param;
                                    

                               }
                            

                        /**
                        * field for KernelId
                        */

                        
                                    protected java.lang.String localKernelId ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localKernelIdTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getKernelId(){
                               return localKernelId;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param KernelId
                               */
                               public void setKernelId(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localKernelIdTracker = true;
                                       } else {
                                          localKernelIdTracker = false;
                                              
                                       }
                                   
                                            this.localKernelId=param;
                                    

                               }
                            

                        /**
                        * field for RamdiskId
                        */

                        
                                    protected java.lang.String localRamdiskId ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localRamdiskIdTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getRamdiskId(){
                               return localRamdiskId;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param RamdiskId
                               */
                               public void setRamdiskId(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localRamdiskIdTracker = true;
                                       } else {
                                          localRamdiskIdTracker = false;
                                              
                                       }
                                   
                                            this.localRamdiskId=param;
                                    

                               }
                            

                        /**
                        * field for BlockDeviceMapping
                        */

                        
                                    protected com.amazon.ec2.BlockDeviceMappingType localBlockDeviceMapping ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localBlockDeviceMappingTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.BlockDeviceMappingType
                           */
                           public  com.amazon.ec2.BlockDeviceMappingType getBlockDeviceMapping(){
                               return localBlockDeviceMapping;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param BlockDeviceMapping
                               */
                               public void setBlockDeviceMapping(com.amazon.ec2.BlockDeviceMappingType param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localBlockDeviceMappingTracker = true;
                                       } else {
                                          localBlockDeviceMappingTracker = false;
                                              
                                       }
                                   
                                            this.localBlockDeviceMapping=param;
                                    

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
                        * field for DisableApiTermination
                        */

                        
                                    protected boolean localDisableApiTermination ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDisableApiTerminationTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getDisableApiTermination(){
                               return localDisableApiTermination;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param DisableApiTermination
                               */
                               public void setDisableApiTermination(boolean param){
                            
                                       // setting primitive attribute tracker to true
                                       
                                               if (false) {
                                           localDisableApiTerminationTracker = false;
                                              
                                       } else {
                                          localDisableApiTerminationTracker = true;
                                       }
                                   
                                            this.localDisableApiTermination=param;
                                    

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
                        * field for License
                        */

                        
                                    protected com.amazon.ec2.InstanceLicenseRequestType localLicense ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localLicenseTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return com.amazon.ec2.InstanceLicenseRequestType
                           */
                           public  com.amazon.ec2.InstanceLicenseRequestType getLicense(){
                               return localLicense;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param License
                               */
                               public void setLicense(com.amazon.ec2.InstanceLicenseRequestType param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localLicenseTracker = true;
                                       } else {
                                          localLicenseTracker = false;
                                              
                                       }
                                   
                                            this.localLicense=param;
                                    

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
                        * field for ClientToken
                        */

                        
                                    protected java.lang.String localClientToken ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localClientTokenTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getClientToken(){
                               return localClientToken;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ClientToken
                               */
                               public void setClientToken(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localClientTokenTracker = true;
                                       } else {
                                          localClientTokenTracker = false;
                                              
                                       }
                                   
                                            this.localClientToken=param;
                                    

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
                       RunInstancesType.this.serialize(parentQName,factory,xmlWriter);
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
               

                   java.lang.String namespacePrefix = registerPrefix(xmlWriter,"http://ec2.amazonaws.com/doc/2010-11-15/");
                   if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)){
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           namespacePrefix+":RunInstancesType",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "RunInstancesType",
                           xmlWriter);
                   }

               
                   }
               
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"imageId", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"imageId");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("imageId");
                                    }
                                

                                          if (localImageId==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("imageId cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localImageId);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"minCount", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"minCount");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("minCount");
                                    }
                                
                                               if (localMinCount==java.lang.Integer.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("minCount cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMinCount));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"maxCount", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"maxCount");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("maxCount");
                                    }
                                
                                               if (localMaxCount==java.lang.Integer.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("maxCount cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMaxCount));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                              if (localKeyNameTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"keyName", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"keyName");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("keyName");
                                    }
                                

                                          if (localKeyName==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("keyName cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localKeyName);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             }
                                            if (localGroupSet==null){
                                                 throw new org.apache.axis2.databinding.ADBException("groupSet cannot be null!!");
                                            }
                                           localGroupSet.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","groupSet"),
                                               factory,xmlWriter);
                                         if (localAdditionalInfoTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"additionalInfo", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"additionalInfo");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("additionalInfo");
                                    }
                                

                                          if (localAdditionalInfo==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("additionalInfo cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localAdditionalInfo);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localUserDataTracker){
                                            if (localUserData==null){
                                                 throw new org.apache.axis2.databinding.ADBException("userData cannot be null!!");
                                            }
                                           localUserData.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","userData"),
                                               factory,xmlWriter);
                                        } if (localAddressingTypeTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"addressingType", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"addressingType");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("addressingType");
                                    }
                                

                                          if (localAddressingType==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("addressingType cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localAddressingType);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             }
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
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
                                           localPlacement.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","placement"),
                                               factory,xmlWriter);
                                        } if (localKernelIdTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"kernelId", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"kernelId");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("kernelId");
                                    }
                                

                                          if (localKernelId==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("kernelId cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localKernelId);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localRamdiskIdTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"ramdiskId", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"ramdiskId");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("ramdiskId");
                                    }
                                

                                          if (localRamdiskId==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("ramdiskId cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localRamdiskId);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localBlockDeviceMappingTracker){
                                            if (localBlockDeviceMapping==null){
                                                 throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
                                            }
                                           localBlockDeviceMapping.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","blockDeviceMapping"),
                                               factory,xmlWriter);
                                        } if (localMonitoringTracker){
                                            if (localMonitoring==null){
                                                 throw new org.apache.axis2.databinding.ADBException("monitoring cannot be null!!");
                                            }
                                           localMonitoring.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","monitoring"),
                                               factory,xmlWriter);
                                        } if (localSubnetIdTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
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
                             } if (localDisableApiTerminationTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"disableApiTermination", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"disableApiTermination");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("disableApiTermination");
                                    }
                                
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("disableApiTermination cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDisableApiTermination));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localInstanceInitiatedShutdownBehaviorTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
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
                             } if (localLicenseTracker){
                                            if (localLicense==null){
                                                 throw new org.apache.axis2.databinding.ADBException("license cannot be null!!");
                                            }
                                           localLicense.serialize(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","license"),
                                               factory,xmlWriter);
                                        } if (localPrivateIpAddressTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
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
                             } if (localClientTokenTracker){
                                    namespace = "http://ec2.amazonaws.com/doc/2010-11-15/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"clientToken", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"clientToken");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("clientToken");
                                    }
                                

                                          if (localClientToken==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("clientToken cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localClientToken);
                                            
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

                
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "imageId"));
                                 
                                        if (localImageId != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localImageId));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("imageId cannot be null!!");
                                        }
                                    
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "minCount"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMinCount));
                            
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "maxCount"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMaxCount));
                             if (localKeyNameTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "keyName"));
                                 
                                        if (localKeyName != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localKeyName));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("keyName cannot be null!!");
                                        }
                                    }
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "groupSet"));
                            
                            
                                    if (localGroupSet==null){
                                         throw new org.apache.axis2.databinding.ADBException("groupSet cannot be null!!");
                                    }
                                    elementList.add(localGroupSet);
                                 if (localAdditionalInfoTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "additionalInfo"));
                                 
                                        if (localAdditionalInfo != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAdditionalInfo));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("additionalInfo cannot be null!!");
                                        }
                                    } if (localUserDataTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "userData"));
                            
                            
                                    if (localUserData==null){
                                         throw new org.apache.axis2.databinding.ADBException("userData cannot be null!!");
                                    }
                                    elementList.add(localUserData);
                                } if (localAddressingTypeTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "addressingType"));
                                 
                                        if (localAddressingType != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAddressingType));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("addressingType cannot be null!!");
                                        }
                                    }
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "instanceType"));
                                 
                                        if (localInstanceType != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInstanceType));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("instanceType cannot be null!!");
                                        }
                                     if (localPlacementTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "placement"));
                            
                            
                                    if (localPlacement==null){
                                         throw new org.apache.axis2.databinding.ADBException("placement cannot be null!!");
                                    }
                                    elementList.add(localPlacement);
                                } if (localKernelIdTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "kernelId"));
                                 
                                        if (localKernelId != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localKernelId));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("kernelId cannot be null!!");
                                        }
                                    } if (localRamdiskIdTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "ramdiskId"));
                                 
                                        if (localRamdiskId != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRamdiskId));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("ramdiskId cannot be null!!");
                                        }
                                    } if (localBlockDeviceMappingTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "blockDeviceMapping"));
                            
                            
                                    if (localBlockDeviceMapping==null){
                                         throw new org.apache.axis2.databinding.ADBException("blockDeviceMapping cannot be null!!");
                                    }
                                    elementList.add(localBlockDeviceMapping);
                                } if (localMonitoringTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "monitoring"));
                            
                            
                                    if (localMonitoring==null){
                                         throw new org.apache.axis2.databinding.ADBException("monitoring cannot be null!!");
                                    }
                                    elementList.add(localMonitoring);
                                } if (localSubnetIdTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "subnetId"));
                                 
                                        if (localSubnetId != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSubnetId));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("subnetId cannot be null!!");
                                        }
                                    } if (localDisableApiTerminationTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "disableApiTermination"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDisableApiTermination));
                            } if (localInstanceInitiatedShutdownBehaviorTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "instanceInitiatedShutdownBehavior"));
                                 
                                        if (localInstanceInitiatedShutdownBehavior != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localInstanceInitiatedShutdownBehavior));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("instanceInitiatedShutdownBehavior cannot be null!!");
                                        }
                                    } if (localLicenseTracker){
                            elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "license"));
                            
                            
                                    if (localLicense==null){
                                         throw new org.apache.axis2.databinding.ADBException("license cannot be null!!");
                                    }
                                    elementList.add(localLicense);
                                } if (localPrivateIpAddressTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "privateIpAddress"));
                                 
                                        if (localPrivateIpAddress != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPrivateIpAddress));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("privateIpAddress cannot be null!!");
                                        }
                                    } if (localClientTokenTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/",
                                                                      "clientToken"));
                                 
                                        if (localClientToken != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localClientToken));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("clientToken cannot be null!!");
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
        public static RunInstancesType parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            RunInstancesType object =
                new RunInstancesType();

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
                    
                            if (!"RunInstancesType".equals(type)){
                                //find namespace for the prefix
                                java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                                return (RunInstancesType)com.amazon.ec2.ExtensionMapper.getTypeObject(
                                     nsUri,type,reader);
                              }
                        

                  }
                

                }

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                 
                    
                    reader.next();
                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","imageId").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setImageId(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","minCount").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setMinCount(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","maxCount").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setMaxCount(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","keyName").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setKeyName(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","groupSet").equals(reader.getName())){
                                
                                                object.setGroupSet(com.amazon.ec2.GroupSetType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","additionalInfo").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setAdditionalInfo(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","userData").equals(reader.getName())){
                                
                                                object.setUserData(com.amazon.ec2.UserDataType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","addressingType").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setAddressingType(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","instanceType").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setInstanceType(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    //throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","placement").equals(reader.getName())){
                                
                                                object.setPlacement(com.amazon.ec2.PlacementRequestType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","kernelId").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setKernelId(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","ramdiskId").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setRamdiskId(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","blockDeviceMapping").equals(reader.getName())){
                                
                                                object.setBlockDeviceMapping(com.amazon.ec2.BlockDeviceMappingType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","monitoring").equals(reader.getName())){
                                
                                                object.setMonitoring(com.amazon.ec2.MonitoringInstanceType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","subnetId").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSubnetId(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","disableApiTermination").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDisableApiTermination(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","instanceInitiatedShutdownBehavior").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setInstanceInitiatedShutdownBehavior(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","license").equals(reader.getName())){
                                
                                                object.setLicense(com.amazon.ec2.InstanceLicenseRequestType.Factory.parse(reader));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","privateIpAddress").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setPrivateIpAddress(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://ec2.amazonaws.com/doc/2010-11-15/","clientToken").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setClientToken(
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
           
          
