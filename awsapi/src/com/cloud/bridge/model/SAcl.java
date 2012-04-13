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
package com.cloud.bridge.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Kelven Yang
 */
public class SAcl implements Serializable {
	private static final long serialVersionUID = 7900837117165018850L;

	public static final int GRANTEE_USER = 0;
	public static final int GRANTEE_ALLUSERS = 1;
	public static final int GRANTEE_AUTHENTICATED = 2;

	public static final int PERMISSION_PASS = -1;   // -> no ACL test required
	public static final int PERMISSION_NONE = 0;
	public static final int PERMISSION_READ = 1;
	public static final int PERMISSION_WRITE = 2;
	public static final int PERMISSION_READ_ACL = 4;
	public static final int PERMISSION_WRITE_ACL = 8;
	public static final int PERMISSION_FULL = (PERMISSION_READ | PERMISSION_WRITE | PERMISSION_READ_ACL | PERMISSION_WRITE_ACL);
	
	private Long id;
	
	private String target;
	private long targetId;

	private int granteeType;
	private String granteeCanonicalId;
	
	private int permission;
	private int grantOrder;
	
	private Date createTime;
	private Date lastModifiedTime;
	
	public SAcl() {
	}
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}
	
	public String getTarget() {
		return target;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public long getTargetId() {
		return targetId;
	}
	
	public void setTargetId(long targetId) {
		this.targetId = targetId;
	}
	
	public int getGranteeType() {
		return granteeType;
	}
	
	public void setGranteeType(int granteeType) {
		this.granteeType = granteeType;
	}
	
	public String getGranteeCanonicalId() {
		return granteeCanonicalId;
	}
	
	public void setGranteeCanonicalId(String granteeCanonicalId) {
		this.granteeCanonicalId = granteeCanonicalId;
	}
	
	public int getPermission() {
		return permission;
	}
	
	public void setPermission(int permission) {
		this.permission = permission;
	}
	
	public int getGrantOrder() {
		return grantOrder;
	}
	
	public void setGrantOrder(int grantOrder) {
		this.grantOrder = grantOrder;
	}
	
	public Date getCreateTime() {
		return createTime;
	}
	
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
	public Date getLastModifiedTime() {
		return lastModifiedTime;
	}
	
	public void setLastModifiedTime(Date lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}
}
