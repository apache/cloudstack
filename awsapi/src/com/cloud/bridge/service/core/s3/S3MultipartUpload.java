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

public class S3MultipartUpload {
	private int    id = -1;
	private String accessKey = null;
	private String bucketName = null;
	private String key = null;
	private Calendar lastModified = null;
	
	public S3MultipartUpload() {
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getAccessKey() {
		return accessKey;
	}
	
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
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
	
	public Calendar getLastModified() {
		return lastModified;
	}
	
	public void setLastModified(Calendar lastModified) {
		this.lastModified = lastModified;
	}
}
