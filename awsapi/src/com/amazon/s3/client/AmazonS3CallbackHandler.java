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
 * AmazonS3CallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:00 EDT)
 */

    package com.amazon.s3.client;

    /**
     *  AmazonS3CallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class AmazonS3CallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public AmazonS3CallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public AmazonS3CallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for getBucketLoggingStatus method
            * override this method for handling normal response from getBucketLoggingStatus operation
            */
           public void receiveResultgetBucketLoggingStatus(
                    com.amazon.s3.client.AmazonS3Stub.GetBucketLoggingStatusResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getBucketLoggingStatus operation
           */
            public void receiveErrorgetBucketLoggingStatus(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for copyObject method
            * override this method for handling normal response from copyObject operation
            */
           public void receiveResultcopyObject(
                    com.amazon.s3.client.AmazonS3Stub.CopyObjectResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from copyObject operation
           */
            public void receiveErrorcopyObject(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getBucketAccessControlPolicy method
            * override this method for handling normal response from getBucketAccessControlPolicy operation
            */
           public void receiveResultgetBucketAccessControlPolicy(
                    com.amazon.s3.client.AmazonS3Stub.GetBucketAccessControlPolicyResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getBucketAccessControlPolicy operation
           */
            public void receiveErrorgetBucketAccessControlPolicy(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for listBucket method
            * override this method for handling normal response from listBucket operation
            */
           public void receiveResultlistBucket(
                    com.amazon.s3.client.AmazonS3Stub.ListBucketResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from listBucket operation
           */
            public void receiveErrorlistBucket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for putObject method
            * override this method for handling normal response from putObject operation
            */
           public void receiveResultputObject(
                    com.amazon.s3.client.AmazonS3Stub.PutObjectResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from putObject operation
           */
            public void receiveErrorputObject(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for createBucket method
            * override this method for handling normal response from createBucket operation
            */
           public void receiveResultcreateBucket(
                    com.amazon.s3.client.AmazonS3Stub.CreateBucketResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from createBucket operation
           */
            public void receiveErrorcreateBucket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for listAllMyBuckets method
            * override this method for handling normal response from listAllMyBuckets operation
            */
           public void receiveResultlistAllMyBuckets(
                    com.amazon.s3.client.AmazonS3Stub.ListAllMyBucketsResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from listAllMyBuckets operation
           */
            public void receiveErrorlistAllMyBuckets(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getObject method
            * override this method for handling normal response from getObject operation
            */
           public void receiveResultgetObject(
                    com.amazon.s3.client.AmazonS3Stub.GetObjectResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getObject operation
           */
            public void receiveErrorgetObject(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for deleteBucket method
            * override this method for handling normal response from deleteBucket operation
            */
           public void receiveResultdeleteBucket(
                    com.amazon.s3.client.AmazonS3Stub.DeleteBucketResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from deleteBucket operation
           */
            public void receiveErrordeleteBucket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for setBucketLoggingStatus method
            * override this method for handling normal response from setBucketLoggingStatus operation
            */
           public void receiveResultsetBucketLoggingStatus(
                    com.amazon.s3.client.AmazonS3Stub.SetBucketLoggingStatusResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from setBucketLoggingStatus operation
           */
            public void receiveErrorsetBucketLoggingStatus(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getObjectAccessControlPolicy method
            * override this method for handling normal response from getObjectAccessControlPolicy operation
            */
           public void receiveResultgetObjectAccessControlPolicy(
                    com.amazon.s3.client.AmazonS3Stub.GetObjectAccessControlPolicyResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getObjectAccessControlPolicy operation
           */
            public void receiveErrorgetObjectAccessControlPolicy(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for deleteObject method
            * override this method for handling normal response from deleteObject operation
            */
           public void receiveResultdeleteObject(
                    com.amazon.s3.client.AmazonS3Stub.DeleteObjectResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from deleteObject operation
           */
            public void receiveErrordeleteObject(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for setBucketAccessControlPolicy method
            * override this method for handling normal response from setBucketAccessControlPolicy operation
            */
           public void receiveResultsetBucketAccessControlPolicy(
                    com.amazon.s3.client.AmazonS3Stub.SetBucketAccessControlPolicyResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from setBucketAccessControlPolicy operation
           */
            public void receiveErrorsetBucketAccessControlPolicy(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for setObjectAccessControlPolicy method
            * override this method for handling normal response from setObjectAccessControlPolicy operation
            */
           public void receiveResultsetObjectAccessControlPolicy(
                    com.amazon.s3.client.AmazonS3Stub.SetObjectAccessControlPolicyResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from setObjectAccessControlPolicy operation
           */
            public void receiveErrorsetObjectAccessControlPolicy(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for putObjectInline method
            * override this method for handling normal response from putObjectInline operation
            */
           public void receiveResultputObjectInline(
                    com.amazon.s3.client.AmazonS3Stub.PutObjectInlineResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from putObjectInline operation
           */
            public void receiveErrorputObjectInline(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getObjectExtended method
            * override this method for handling normal response from getObjectExtended operation
            */
           public void receiveResultgetObjectExtended(
                    com.amazon.s3.client.AmazonS3Stub.GetObjectExtendedResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getObjectExtended operation
           */
            public void receiveErrorgetObjectExtended(java.lang.Exception e) {
            }
                


    }
    