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

import java.util.List;

import com.cloud.bridge.model.SAcl;
import com.cloud.bridge.model.SAclVO;
import com.cloud.bridge.model.SBucket;
import com.cloud.bridge.service.exception.UnsupportedException;

/**
 * Each relation holds
 * a grantee - which is one of SAcl.GRANTEE_USER, SAcl.GRANTEE_ALLUSERS, SAcl.GRANTEE_AUTHENTICATED 
 * a permission - which is one of SAcl.PERMISSION_PASS, SAcl.PERMISSION_NONE, SAcl.PERMISSION_READ,
 *     SAcl.PERMISSION_WRITE, SAcl.PERMISSION_READ_ACL, SAcl.PERMISSION_WRITE_ACL, SAcl.PERMISSION_FULL
 * canonicalUserID
 */
public class S3Grant {
	private int grantee;			// SAcl.GRANTEE_USER etc
	private int permission;			// SAcl.PERMISSION_READ etc
	private String canonicalUserID;
	
	public S3Grant() {
	}

	public int getGrantee() {
		return grantee;
	}

	public void setGrantee(int grantee) {
		this.grantee = grantee;
	}

	public int getPermission() {
		return permission;
	}

	public void setPermission(int permission) {
		this.permission = permission;
	}

	public String getCanonicalUserID() {
		return canonicalUserID;
	}

	public void setCanonicalUserID(String canonicalUserID) {
		this.canonicalUserID = canonicalUserID;
	}
	
	/* Return an array of S3Grants holding the permissions of grantees by grantee type and their canonicalUserIds.
	 * Used by S3 engine to get ACL policy requests for buckets and objects.
	 */
	public static S3Grant[] toGrants(List<SAclVO> grants) {
		if(grants != null) 
		{
			S3Grant[] entries = new S3Grant[grants.size()];
			int i = 0;
			for(SAclVO acl: grants) {
				entries[i] = new S3Grant();
				entries[i].setGrantee(acl.getGranteeType());
				entries[i].setCanonicalUserID(acl.getGranteeCanonicalId());
				entries[i].setPermission(acl.getPermission());
				i++;
			}
			return entries;
		}
		return null;
	}
		
}
