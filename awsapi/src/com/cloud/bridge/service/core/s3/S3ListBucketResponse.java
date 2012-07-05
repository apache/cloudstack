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

import com.cloud.bridge.util.XElement;

public class S3ListBucketResponse extends S3Response {
	
	@XElement(name="Name")
	protected String bucketName;
	
	@XElement(name="Prefix")
	protected String prefix;
	
	@XElement(name="Marker")
	protected String marker;
	
	@XElement(name="MaxKeys")
	protected int maxKeys;
	
	@XElement(name="IsTruncated")
	protected boolean isTruncated;
	
	protected String delimiter;
	protected String nextMarker;
	
	@XElement(name="ContentsList", item="Contents", itemClass="com.cloud.gate.service.core.s3.S3ListBucketObjectEntry")
	protected S3ListBucketObjectEntry[] contents;
	
	@XElement(name="CommonPrefixesList", item="CommonPrefixes", itemClass="com.cloud.gate.service.core.s3.S3ListBucketPrefixEntry")
	protected S3ListBucketPrefixEntry[] commonPrefixes;
	
	public S3ListBucketResponse() {
		super();
	}
	
	public String getBucketName() {
		return bucketName;
	}
	
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	
	public String getMarker() {
		return marker;
	}
	
	public void setMarker(String marker) {
		this.marker = marker;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public String getDelimiter() {
		return delimiter;
	}
	
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	
	public int getMaxKeys() {
		return maxKeys;
	}
	
	public void setMaxKeys(int maxKeys) {
		this.maxKeys = maxKeys;
	}
	
	public boolean isTruncated() {
		return isTruncated;
	}
	
	public void setTruncated(boolean isTruncated) {
		this.isTruncated = isTruncated;
	}
	
	public String getNextMarker() {
		return nextMarker;
	}
	
	public void setNextMarker(String nextMarker) {
		this.nextMarker = nextMarker;
	}
	
	public S3ListBucketPrefixEntry[] getCommonPrefixes() {
		return commonPrefixes;
	}
	
	public void setCommonPrefixes(S3ListBucketPrefixEntry[] commonPrefixes) {
		this.commonPrefixes = commonPrefixes;
	}
	
	public S3ListBucketObjectEntry[] getContents() {
		return contents;
	}
	
	public void setContents(S3ListBucketObjectEntry[] contents) {
		this.contents = contents;
	}
}
