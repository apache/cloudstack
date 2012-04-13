/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.service.core.s3;

import java.util.List;

import com.cloud.bridge.model.SAcl;

/**
 * @author Kelven Yang
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
	
	public static S3Grant[] toGrants(List<SAcl> grants) {
		if(grants != null) 
		{
			S3Grant[] entries = new S3Grant[grants.size()];
			int i = 0;
			for(SAcl acl: grants) {
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
