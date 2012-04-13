/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.service;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

import com.amazon.s3.*;

/**
 * @author Kelven Yang
 */
public class S3SoapService implements AmazonS3SkeletonInterface {
    protected final static Logger logger = Logger.getLogger(S3SoapService.class);
    
	public GetBucketLoggingStatusResponse getBucketLoggingStatus(GetBucketLoggingStatus getBucketLoggingStatus0) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.getBucketLoggingStatus(getBucketLoggingStatus0);
    }
     
	public CopyObjectResponse copyObject(com.amazon.s3.CopyObject copyObject2) throws AxisFault {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.copyObject(copyObject2);
    }
 
	public GetBucketAccessControlPolicyResponse getBucketAccessControlPolicy (
		GetBucketAccessControlPolicy getBucketAccessControlPolicy4) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.getBucketAccessControlPolicy (getBucketAccessControlPolicy4);
    }
 
	public ListBucketResponse listBucket (ListBucket listBucket6) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.listBucket (listBucket6);
    }
     
	public PutObjectResponse putObject(PutObject putObject8) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.putObject(putObject8);
    }
 
	public CreateBucketResponse createBucket (CreateBucket createBucket) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.createBucket(createBucket);
    }
 
	public ListAllMyBucketsResponse listAllMyBuckets (
		ListAllMyBuckets listAllMyBuckets12) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.listAllMyBuckets (listAllMyBuckets12);
    }
     
	public GetObjectResponse getObject(com.amazon.s3.GetObject getObject14) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.getObject(getObject14);
    }
     
	public DeleteBucketResponse deleteBucket(DeleteBucket deleteBucket16) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.deleteBucket(deleteBucket16);
    }
     
	public SetBucketLoggingStatusResponse setBucketLoggingStatus(
          SetBucketLoggingStatus setBucketLoggingStatus18) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.setBucketLoggingStatus(setBucketLoggingStatus18);
    }
 
     
	public GetObjectAccessControlPolicyResponse getObjectAccessControlPolicy(
          GetObjectAccessControlPolicy getObjectAccessControlPolicy20) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.getObjectAccessControlPolicy(getObjectAccessControlPolicy20);
    }
     
	public DeleteObjectResponse deleteObject (DeleteObject deleteObject22) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.deleteObject (deleteObject22);
    }
     
	public SetBucketAccessControlPolicyResponse setBucketAccessControlPolicy(
          SetBucketAccessControlPolicy setBucketAccessControlPolicy24) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.setBucketAccessControlPolicy(setBucketAccessControlPolicy24);
    }
 
	public SetObjectAccessControlPolicyResponse setObjectAccessControlPolicy(
          SetObjectAccessControlPolicy setObjectAccessControlPolicy26) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.setObjectAccessControlPolicy(setObjectAccessControlPolicy26);
    }
 
	public PutObjectInlineResponse putObjectInline (PutObjectInline putObjectInline28) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.putObjectInline (putObjectInline28);
    }
 
	public GetObjectExtendedResponse getObjectExtended(GetObjectExtended getObjectExtended30) {
		AmazonS3SkeletonInterface s3Service = ServiceProvider.getInstance().getServiceImpl(AmazonS3SkeletonInterface.class);
		return s3Service.getObjectExtended(getObjectExtended30);
    }
}
