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
package com.cloud.bridge.service.core.s3;

import java.io.InputStream;

public class S3PutObjectRequest extends S3Request {
	protected String bucketName;
	protected String key;
	protected String rawTimestamp;   // -> original is needed for authentication
	protected String storageClass;
	protected String credential;
	protected long contentLength;
	protected S3MetaDataEntry[] metaEntries;
	protected S3AccessControlList acl;
	protected InputStream data;
	
	public S3PutObjectRequest() {
		super();
	}
	
	public String getBucketName() {
		return bucketName;
	}
	
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public long getContentLength() {
		return contentLength;
	}
	
	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}
	
	public S3MetaDataEntry[] getMetaEntries() {
		return metaEntries;
	}
	
	public void setMetaEntries(S3MetaDataEntry[] metaEntries) {
		this.metaEntries = metaEntries;
	}
	
	public S3AccessControlList getAcl() {
		return acl;
	}
	
	public void setAcl(S3AccessControlList acl) {
		this.acl = acl;
	}
	
	public InputStream getInputStream() {
		return data;
	}
	
	public void setData(InputStream is) {
		this.data = is;
	}
	
	public String getStorageClass() {
		return storageClass;
	}
	
	public void setStorageClass(String storageClass) {
		this.storageClass = storageClass;
	}

	public String getCredential() {
		return credential;
	}
	
	public void setCredential(String credential) {
		this.credential = credential;
	}
	
	public String getRawTimestamp() {
		return rawTimestamp;
	}
	
	public void setRawTimestamp(String rawTimestamp) {
		this.rawTimestamp = rawTimestamp;
	}
}
