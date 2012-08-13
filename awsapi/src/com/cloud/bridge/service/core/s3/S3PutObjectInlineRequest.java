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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;

public class S3PutObjectInlineRequest extends S3Request {
	protected String bucketName;
	protected String key;
	protected long contentLength;
	protected S3MetaDataEntry[] metaEntries;
	protected S3AccessControlList acl;
	protected String cannedAccessPolicy;    // Canned ACLs are public-read, public-read-write, private, authenticated-read or log-delivery-write
	protected DataHandler data;
	protected String dataAsString;
	
	public S3PutObjectInlineRequest() {
		super();
		data = null;
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
	
	public String getCannedAccess() {
		return cannedAccessPolicy;
	}

	public void setCannedAccess(String cannedAccessPolicy) {
		this.cannedAccessPolicy = cannedAccessPolicy;
	}
	
	public DataHandler getData() {
		return data;
	}
	
	public void setData(DataHandler data) {
		this.data = data;
	}
	
	public void setDataAsString( String data ) {
		this.dataAsString = data;
	}
	
	public InputStream getDataInputStream() throws IOException 
	{
		if ( null == data ) 
		{
			 ByteArrayInputStream bs = new ByteArrayInputStream( dataAsString.getBytes());
			 return bs;
		}
		else return data.getInputStream();
	}
}
