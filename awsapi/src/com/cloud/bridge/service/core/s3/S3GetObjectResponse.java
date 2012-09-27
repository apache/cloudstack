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

import javax.activation.DataHandler;

public class S3GetObjectResponse extends S3Response {
	protected String ETag;
	protected Calendar lastModified;
	protected DataHandler data;
	protected S3MetaDataEntry[] metaEntries;	
	protected long contentLength;
	protected String deleteMarker;
	protected String version;
	
	public S3GetObjectResponse() {
		super();
		deleteMarker = null;
	}

	public String getETag() {
		return ETag;
	}

	public void setETag(String eTag) {
		ETag = eTag;
	}

	public String getDeleteMarker() {
		return this.deleteMarker;
	}

	public void setDeleteMarker(String deleteMarker) {
		this.deleteMarker = deleteMarker;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Calendar getLastModified() {
		return lastModified;
	}

	public void setLastModified(Calendar lastModified) {
		this.lastModified = lastModified;
	}

	public DataHandler getData() {
		return data;
	}

	public void setData(DataHandler data) {
		this.data = data;
	}

	public S3MetaDataEntry[] getMetaEntries() {
		return metaEntries;
	}

	public void setMetaEntries(S3MetaDataEntry[] metaEntries) {
		this.metaEntries = metaEntries;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}
}

