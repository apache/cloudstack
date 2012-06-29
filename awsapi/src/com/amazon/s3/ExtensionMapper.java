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
 * ExtensionMapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:34 EDT)
 */

            package com.amazon.s3;
            /**
            *  ExtensionMapper class
            */
        
        public  class ExtensionMapper{

          public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
                                                       java.lang.String typeName,
                                                       javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "PutObjectResult".equals(typeName)){
                   
                            return  com.amazon.s3.PutObjectResult.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "AccessControlList".equals(typeName)){
                   
                            return  com.amazon.s3.AccessControlList.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "VersioningConfiguration".equals(typeName)){
                   
                            return  com.amazon.s3.VersioningConfiguration.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "ListAllMyBucketsEntry".equals(typeName)){
                   
                            return  com.amazon.s3.ListAllMyBucketsEntry.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "ListBucketResult".equals(typeName)){
                   
                            return  com.amazon.s3.ListBucketResult.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "PrefixEntry".equals(typeName)){
                   
                            return  com.amazon.s3.PrefixEntry.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "AccessControlPolicy".equals(typeName)){
                   
                            return  com.amazon.s3.AccessControlPolicy.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "VersioningStatus".equals(typeName)){
                   
                            return  com.amazon.s3.VersioningStatus.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "ListAllMyBucketsList".equals(typeName)){
                   
                            return  com.amazon.s3.ListAllMyBucketsList.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "CreateBucketResult".equals(typeName)){
                   
                            return  com.amazon.s3.CreateBucketResult.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "Status".equals(typeName)){
                   
                            return  com.amazon.s3.Status.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "VersionEntry".equals(typeName)){
                   
                            return  com.amazon.s3.VersionEntry.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "LoggingSettings".equals(typeName)){
                   
                            return  com.amazon.s3.LoggingSettings.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "AmazonCustomerByEmail".equals(typeName)){
                   
                            return  com.amazon.s3.AmazonCustomerByEmail.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "CanonicalUser".equals(typeName)){
                   
                            return  com.amazon.s3.CanonicalUser.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "CopyObjectResult".equals(typeName)){
                   
                            return  com.amazon.s3.CopyObjectResult.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "ListEntry".equals(typeName)){
                   
                            return  com.amazon.s3.ListEntry.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "Permission".equals(typeName)){
                   
                            return  com.amazon.s3.Permission.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "Grantee".equals(typeName)){
                   
                            return  com.amazon.s3.Grantee.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "Result".equals(typeName)){
                   
                            return  com.amazon.s3.Result.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "RequestPaymentConfiguration".equals(typeName)){
                   
                            return  com.amazon.s3.RequestPaymentConfiguration.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "MfaDeleteStatus".equals(typeName)){
                   
                            return  com.amazon.s3.MfaDeleteStatus.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "Payer".equals(typeName)){
                   
                            return  com.amazon.s3.Payer.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "ListAllMyBucketsResult".equals(typeName)){
                   
                            return  com.amazon.s3.ListAllMyBucketsResult.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "BucketLoggingStatus".equals(typeName)){
                   
                            return  com.amazon.s3.BucketLoggingStatus.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "ListVersionsResult".equals(typeName)){
                   
                            return  com.amazon.s3.ListVersionsResult.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "MetadataDirective".equals(typeName)){
                   
                            return  com.amazon.s3.MetadataDirective.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "GetObjectResult".equals(typeName)){
                   
                            return  com.amazon.s3.GetObjectResult.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "Grant".equals(typeName)){
                   
                            return  com.amazon.s3.Grant.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "CreateBucketConfiguration".equals(typeName)){
                   
                            return  com.amazon.s3.CreateBucketConfiguration.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "TopicConfiguration".equals(typeName)){
                   
                            return  com.amazon.s3.TopicConfiguration.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "Group".equals(typeName)){
                   
                            return  com.amazon.s3.Group.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "NotificationConfiguration".equals(typeName)){
                   
                            return  com.amazon.s3.NotificationConfiguration.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "MetadataEntry".equals(typeName)){
                   
                            return  com.amazon.s3.MetadataEntry.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "DeleteMarkerEntry".equals(typeName)){
                   
                            return  com.amazon.s3.DeleteMarkerEntry.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "LocationConstraint".equals(typeName)){
                   
                            return  com.amazon.s3.LocationConstraint.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "StorageClass".equals(typeName)){
                   
                            return  com.amazon.s3.StorageClass.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://s3.amazonaws.com/doc/2006-03-01/".equals(namespaceURI) &&
                  "User".equals(typeName)){
                   
                            return  com.amazon.s3.User.Factory.parse(reader);
                        

                  }

              
             throw new org.apache.axis2.databinding.ADBException("Unsupported type " + namespaceURI + " " + typeName);
          }

        }
    