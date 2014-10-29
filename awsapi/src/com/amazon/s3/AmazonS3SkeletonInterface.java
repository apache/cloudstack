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
 * AmazonS3SkeletonInterface.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:00 EDT)
 */
package com.amazon.s3;

import org.apache.axis2.AxisFault;

/**
 *  AmazonS3SkeletonInterface java skeleton interface for the axisService
 */
public interface AmazonS3SkeletonInterface {

    /**
     * Auto generated method signature
     *
                                * @param getBucketLoggingStatus
     */

    public com.amazon.s3.GetBucketLoggingStatusResponse getBucketLoggingStatus(com.amazon.s3.GetBucketLoggingStatus getBucketLoggingStatus);

    /**
     * Auto generated method signature
     *
                                * @param copyObject
     * @throws AxisFault
     */

    public com.amazon.s3.CopyObjectResponse copyObject(com.amazon.s3.CopyObject copyObject) throws AxisFault;

    /**
     * Auto generated method signature
     *
                                * @param getBucketAccessControlPolicy
     */

    public com.amazon.s3.GetBucketAccessControlPolicyResponse getBucketAccessControlPolicy(com.amazon.s3.GetBucketAccessControlPolicy getBucketAccessControlPolicy);

    /**
     * Auto generated method signature
     *
                                * @param listBucket
     */

    public com.amazon.s3.ListBucketResponse listBucket(com.amazon.s3.ListBucket listBucket);

    /**
     * Auto generated method signature
     *
                                * @param putObject
     */

    public com.amazon.s3.PutObjectResponse putObject(com.amazon.s3.PutObject putObject);

    /**
     * Auto generated method signature
     *
                                * @param createBucket
     */

    public com.amazon.s3.CreateBucketResponse createBucket(com.amazon.s3.CreateBucket createBucket);

    /**
     * Auto generated method signature
     *
                                * @param listAllMyBuckets
     */

    public com.amazon.s3.ListAllMyBucketsResponse listAllMyBuckets(com.amazon.s3.ListAllMyBuckets listAllMyBuckets);

    /**
     * Auto generated method signature
     *
                                * @param getObject
     */

    public com.amazon.s3.GetObjectResponse getObject(com.amazon.s3.GetObject getObject);

    /**
     * Auto generated method signature
     *
                                * @param deleteBucket
     */

    public com.amazon.s3.DeleteBucketResponse deleteBucket(com.amazon.s3.DeleteBucket deleteBucket);

    /**
     * Auto generated method signature
     *
                                * @param setBucketLoggingStatus
     */

    public com.amazon.s3.SetBucketLoggingStatusResponse setBucketLoggingStatus(com.amazon.s3.SetBucketLoggingStatus setBucketLoggingStatus);

    /**
     * Auto generated method signature
     *
                                * @param getObjectAccessControlPolicy
     */

    public com.amazon.s3.GetObjectAccessControlPolicyResponse getObjectAccessControlPolicy(com.amazon.s3.GetObjectAccessControlPolicy getObjectAccessControlPolicy);

    /**
     * Auto generated method signature
     *
                                * @param deleteObject
     */

    public com.amazon.s3.DeleteObjectResponse deleteObject(com.amazon.s3.DeleteObject deleteObject);

    /**
     * Auto generated method signature
     *
                                * @param setBucketAccessControlPolicy
     */

    public com.amazon.s3.SetBucketAccessControlPolicyResponse setBucketAccessControlPolicy(com.amazon.s3.SetBucketAccessControlPolicy setBucketAccessControlPolicy);

    /**
     * Auto generated method signature
     *
                                * @param setObjectAccessControlPolicy
     */

    public com.amazon.s3.SetObjectAccessControlPolicyResponse setObjectAccessControlPolicy(com.amazon.s3.SetObjectAccessControlPolicy setObjectAccessControlPolicy);

    /**
     * Auto generated method signature
     *
                                * @param putObjectInline
     */

    public com.amazon.s3.PutObjectInlineResponse putObjectInline(com.amazon.s3.PutObjectInline putObjectInline);

    /**
     * Auto generated method signature
     *
                                * @param getObjectExtended
     */

    public com.amazon.s3.GetObjectExtendedResponse getObjectExtended(com.amazon.s3.GetObjectExtended getObjectExtended);

}
