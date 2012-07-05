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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class S3PolicyAction {

	public enum PolicyActions {
		UnknownAction, AllActions,
	    GetObject, GetObjectVersion, PutObject, GetObjectAcl,
	    GetObjectVersionAcl, PutObjectAcl, PutObjectAclVersion, DeleteObject,
	    DeleteObjectVersion, ListMultipartUploadParts, AbortMultipartUpload, CreateBucket,
	    DeleteBucket, ListBucket, ListBucketVersions, ListAllMyBuckets, 
	    ListBucketMultipartUploads, GetBucketAcl, PutBucketAcl, GetBucketVersioning,
	    PutBucketVersioning, GetBucketLocation, PutBucketPolicy, GetBucketPolicy
	}
	
	// -> map a string name into a policy action constant
	private Map<String,PolicyActions> actionNames = new HashMap<String,PolicyActions>();
	
	private List<PolicyActions> actionList = new ArrayList<PolicyActions>();
	
	public S3PolicyAction() {
		actionNames.put("s3:GetObject",                  PolicyActions.GetObject);
		actionNames.put("s3:GetObjectVersion",           PolicyActions.GetObjectVersion);
		actionNames.put("s3:PutObject",                  PolicyActions.PutObject);
		actionNames.put("s3:GetObjectAcl",               PolicyActions.GetObjectAcl);
		actionNames.put("s3:GetObjectVersionAcl",        PolicyActions.GetObjectVersionAcl);
		actionNames.put("s3:PutObjectAcl",               PolicyActions.PutObjectAcl);
		actionNames.put("s3:PutObjectAclVersion",        PolicyActions.PutObjectAclVersion);
		actionNames.put("s3:DeleteObject",               PolicyActions.DeleteObject);
		actionNames.put("s3:DeleteObjectVersion",        PolicyActions.DeleteObjectVersion);
		actionNames.put("s3:ListMultipartUploadParts",   PolicyActions.ListMultipartUploadParts);
		actionNames.put("s3:AbortMultipartUpload",       PolicyActions.AbortMultipartUpload);
		actionNames.put("s3:CreateBucket",               PolicyActions.CreateBucket);
		actionNames.put("s3:DeleteBucket",               PolicyActions.DeleteBucket);
		actionNames.put("s3:ListBucket",                 PolicyActions.ListBucket);
		actionNames.put("s3:ListBucketVersions",         PolicyActions.ListBucketVersions);
		actionNames.put("s3:ListAllMyBuckets",           PolicyActions.ListAllMyBuckets);
		actionNames.put("s3:ListBucketMultipartUploads", PolicyActions.ListBucketMultipartUploads);
		actionNames.put("s3:GetBucketAcl",               PolicyActions.GetBucketAcl);
		actionNames.put("s3:PutBucketAcl",               PolicyActions.PutBucketAcl);
		actionNames.put("s3:GetBucketVersioning",        PolicyActions.GetBucketVersioning);
		actionNames.put("s3:PutBucketVersioning",        PolicyActions.PutBucketVersioning);
		actionNames.put("s3:GetBucketLocation",          PolicyActions.GetBucketLocation);
		actionNames.put("s3:PutBucketPolicy",            PolicyActions.PutBucketPolicy);
		actionNames.put("s3:GetBucketPolicy",            PolicyActions.GetBucketPolicy);
	}

	public PolicyActions[] getActions() {
		return actionList.toArray(new PolicyActions[0]);
	}
	
	public void addAction(PolicyActions param) {
		actionList.add( param );
	}
	
	public PolicyActions toPolicyActions(String operation) {
		
		if (operation.equalsIgnoreCase("s3:*")) return PolicyActions.AllActions;
		
		Object value = actionNames.get( operation );

		if ( null == value ) 
			 return PolicyActions.UnknownAction;
		else return (PolicyActions)value;
	}
	
	public boolean contains(PolicyActions operationRequested) {
		Iterator<PolicyActions> itr = actionList.iterator();
	    while( itr.hasNext()) {
	    	PolicyActions oneAction = itr.next();
	    	if (PolicyActions.AllActions == oneAction) return true;
	    	if (oneAction == operationRequested) return true;
	    }
	    return false;
	}

	public String toString() {
		
		StringBuffer value = new StringBuffer();
		Iterator<PolicyActions> itr = actionList.iterator();
		
		value.append( "Actions: \n" );
		while( itr.hasNext()) {
			PolicyActions oneAction = itr.next();
			value.append( oneAction );
			value.append( "\n" );
		}
		
		return value.toString();
	}
}
