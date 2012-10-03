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

import java.util.Calendar;

public class S3PutObjectInlineResponse extends S3Response {
	protected String ETag;
	protected Calendar lastModified;
	protected String version;
	protected int uploadId;
	
	public S3PutObjectInlineResponse() {
		super();
		uploadId = -1;
	}

	 // add ETag header computed as Base64 MD5 whenever object is uploaded or updated
	 // the Base64 is represented in lowercase
	public String getETag() {
		return ETag;
	}

	public void setETag(String eTag) {
		this.ETag = eTag;
	}

	public Calendar getLastModified() {
		return lastModified;
	}

	public void setLastModified(Calendar lastModified) {
		this.lastModified = lastModified;
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public int getUploadId() {
		return uploadId;
	}

	public void setUploadId(int uploadId) {
		this.uploadId = uploadId;
	}
}
