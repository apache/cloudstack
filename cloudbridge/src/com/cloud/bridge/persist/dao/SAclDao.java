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
package com.cloud.bridge.persist.dao;

import java.util.Date;
import java.util.List;

import com.cloud.bridge.model.SAcl;
import com.cloud.bridge.persist.EntityDao;
import com.cloud.bridge.persist.PersistContext;
import com.cloud.bridge.service.core.s3.S3AccessControlList;
import com.cloud.bridge.service.core.s3.S3Grant;

/**
 * @author Kelven Yang
 */
public class SAclDao extends EntityDao<SAcl> {
	
	public SAclDao() {
		super(SAcl.class);
	}
	
	public List<SAcl> listGrants(String target, long targetId) {
		return queryEntities("from SAcl where target=? and targetId=? order by grantOrder asc",
			new Object[] { target, new Long(targetId)});
	}

	public List<SAcl> listGrants(String target, long targetId, String userCanonicalId) {
		return queryEntities("from SAcl where target=? and targetId=? and granteeCanonicalId=? order by grantOrder asc",
			new Object[] { target, new Long(targetId), userCanonicalId });
	}

	public void save(String target, long targetId, S3AccessControlList acl) {
		// -> the target's ACLs are being redefined
		executeUpdate("delete from SAcl where target=? and targetId=?",	new Object[] { target, new Long(targetId)});
		
		if(acl != null) {
			S3Grant[] grants = acl.getGrants();
			if(grants != null && grants.length > 0) {
				int grantOrder = 1;
				for(S3Grant grant : grants) {
					save(target, targetId, grant, grantOrder++);
				}
			}
		}
	}
	
	public SAcl save(String target, long targetId, S3Grant grant, int grantOrder) {
		SAcl aclEntry = new SAcl();
		aclEntry.setTarget(target);
		aclEntry.setTargetId(targetId);
		aclEntry.setGrantOrder(grantOrder);
		
		int grantee = grant.getGrantee();
		aclEntry.setGranteeType(grantee);
		aclEntry.setPermission(grant.getPermission());
		aclEntry.setGranteeCanonicalId(grant.getCanonicalUserID());
		
		Date ts = new Date();
		aclEntry.setCreateTime(ts);
		aclEntry.setLastModifiedTime(ts);
		PersistContext.getSession().save(aclEntry);
		return aclEntry;
	}
}
