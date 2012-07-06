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

public class S3CopyObjectRequest extends S3Request {
	public enum MetadataDirective { COPY, REPLACE  };
	
	protected String sourceBucketName;
	protected String sourceKey;
	protected String destinationBucketName;
	protected String destinationKey;
	protected String version;
	protected MetadataDirective directive;
	protected S3MetaDataEntry[] metaEntries;
	protected S3AccessControlList acl;
	protected String cannedAccessPolicy;    // -> REST only sets an acl with a simple keyword
	protected S3ConditionalHeaders conds;

	public S3CopyObjectRequest() {
		super();
		version   = null;
		directive = MetadataDirective.COPY;
	}
	
	public String getSourceBucketName() {
		return sourceBucketName;
	}

	public void setSourceBucketName(String bucketName) {
		sourceBucketName = bucketName;
	}
	
	public String getDestinationBucketName() {
		return destinationBucketName;
	}

	public void setDestinationBucketName(String bucketName) {
		destinationBucketName = bucketName;
	}
	
	public String getSourceKey() {
		return sourceKey;
	}

	public void setSourceKey(String key) {
		sourceKey = key;
	}

	public String getDestinationKey() {
		return destinationKey;
	}

	public void setDestinationKey(String key) {
		destinationKey = key;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	public MetadataDirective getDirective() {
	    return directive;	
	}
	
	public void setDataDirective(String dataDirective) {
		if (null == dataDirective) return;
		
	     	 if (dataDirective.equalsIgnoreCase( "COPY"    )) directive = MetadataDirective.COPY;
		else if (dataDirective.equalsIgnoreCase( "REPLACE" )) directive = MetadataDirective.REPLACE;
		else throw new UnsupportedOperationException("Unknown Metadata Directive: " + dataDirective );
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
	
	public void setConditions(S3ConditionalHeaders conds) {
		this.conds = conds;
	}
	
	public S3ConditionalHeaders getConditions() {
		return conds;
	}
}
