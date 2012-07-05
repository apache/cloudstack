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

import java.util.Set;

import com.cloud.bridge.service.core.s3.S3ConditionFactory.PolicyConditions;
import com.cloud.bridge.service.exception.PermissionDeniedException;

/**
 * In the Bucket Policy language a condition block can hold one or more conditions.
 * A condition has one or more keys, where each key can have one or more values.
 */
public abstract class S3PolicyCondition {

	public enum ConditionKeys {
		UnknownKey,
	    CurrentTime, SecureTransport, SourceIp, SourceArn, UserAgent, EpochTime, Referer,
	    Acl, Location, Prefix, Delimiter, MaxKeys, CopySource, MetaData, VersionId
	}
	
	protected PolicyConditions condition = null;   
	
	public S3PolicyCondition() {
	}
	
	public PolicyConditions getCondition() {
		return condition;
	}
	
	public void setCondition(PolicyConditions param) {
		condition = param;
	}
	
	public static ConditionKeys toConditionKeys(String keyName) 
	{
	         if (keyName.equalsIgnoreCase( "aws:CurrentTime"             )) return ConditionKeys.CurrentTime;
	    else if (keyName.equalsIgnoreCase( "aws:SecureTransport"         )) return ConditionKeys.SecureTransport;
	    else if (keyName.equalsIgnoreCase( "aws:SourceIp"                )) return ConditionKeys.SourceIp;
	    else if (keyName.equalsIgnoreCase( "aws:SourceArn"               )) return ConditionKeys.SourceArn;
	    else if (keyName.equalsIgnoreCase( "aws:UserAgent"               )) return ConditionKeys.UserAgent;
	    else if (keyName.equalsIgnoreCase( "aws:EpochTime"               )) return ConditionKeys.EpochTime;
	    else if (keyName.equalsIgnoreCase( "aws:Referer"                 )) return ConditionKeys.Referer;
	    else if (keyName.equalsIgnoreCase( "s3:x-amz-acl"                )) return ConditionKeys.Acl;
	    else if (keyName.equalsIgnoreCase( "s3:LocationConstraint"       )) return ConditionKeys.Location;
	    else if (keyName.equalsIgnoreCase( "s3:prefix"                   )) return ConditionKeys.Prefix;
	    else if (keyName.equalsIgnoreCase( "s3:delimiter"                )) return ConditionKeys.Delimiter;
	    else if (keyName.equalsIgnoreCase( "s3:max-keys"                 )) return ConditionKeys.MaxKeys;
	    else if (keyName.equalsIgnoreCase( "s3:x-amz-copy-source"        )) return ConditionKeys.CopySource;
	    else if (keyName.equalsIgnoreCase( "s3:x-amz-metadata-directive" )) return ConditionKeys.MetaData;
	    else if (keyName.equalsIgnoreCase( "s3:VersionId"                )) return ConditionKeys.VersionId;
	    else return ConditionKeys.UnknownKey;
	}

	public Set<ConditionKeys> getAllKeys() {
		return null;
	}
	
	/**
	 * After calling getAllKeys(), pass in each key from that result to get
	 * the key's associated list of values.
	 * @param key
	 * @return object[]
	 */
	public Object[] getKeyValues(ConditionKeys key) {
		return null;
	}
	
	public void setKey(ConditionKeys key, String[] values) throws Exception {
	}

	public boolean isTrue(S3PolicyContext params, String SID) throws Exception {
		return false;
	}
	
	public void verify() throws PermissionDeniedException {
	}
	
	public String toString() {
		return "";
	}
}
