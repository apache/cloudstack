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
package com.cloud.bridge.persist.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.bridge.model.SAcl;
import com.cloud.bridge.model.SAclVO;
import com.cloud.bridge.service.core.s3.S3AccessControlList;
import com.cloud.bridge.service.core.s3.S3Grant;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value={SAclDao.class})
public class SAclDaoImpl extends GenericDaoBase<SAclVO, Long> implements SAclDao {

	public SAclDaoImpl() {}
	
	@Override
	public List<SAclVO> listGrants(String target, long targetId) {
	    SearchBuilder<SAclVO> SearchByTarget = createSearchBuilder();
	    SearchByTarget.and("Target", SearchByTarget.entity().getTarget(), SearchCriteria.Op.EQ);
	    SearchByTarget.and("TargetID", SearchByTarget.entity().getTargetId(), SearchCriteria.Op.EQ);
	    SearchByTarget.done();
	    Filter filter = new Filter(SAclVO.class, "grantOrder", Boolean.TRUE, null, null);
	    TransactionLegacy txn = TransactionLegacy.open( TransactionLegacy.AWSAPI_DB);
	    try {
		txn.start();
		SearchCriteria<SAclVO> sc = SearchByTarget.create();
		sc.setParameters("Target", target);
		sc.setParameters("TargetID", targetId);
		return listBy(sc, filter);
		
	    } finally {
		txn.close();
	    }
	}
	
	@Override
	public List<SAclVO> listGrants(String target, long targetId, String userCanonicalId) {
	    SearchBuilder<SAclVO> SearchByAcl = createSearchBuilder();
	    SearchByAcl.and("Target", SearchByAcl.entity().getTarget(), SearchCriteria.Op.EQ);
	    SearchByAcl.and("TargetID", SearchByAcl.entity().getTargetId(), SearchCriteria.Op.EQ);
	    SearchByAcl.and("GranteeCanonicalID", SearchByAcl.entity().getGranteeCanonicalId(), SearchCriteria.Op.EQ);
	    Filter filter = new Filter(SAclVO.class, "grantOrder", Boolean.TRUE, null, null);
	    TransactionLegacy txn = TransactionLegacy.open( TransactionLegacy.AWSAPI_DB);
	    try {
    		txn.start();
    		SearchCriteria<SAclVO> sc = SearchByAcl.create();
    		sc.setParameters("Target", target);
    		sc.setParameters("TargetID", targetId);
    		sc.setParameters("GranteeCanonicalID", userCanonicalId);
    		return listBy(sc, filter);
	    } finally {
		txn.close();
	    }
	}

	@Override
	public void save(String target, long targetId, S3AccessControlList acl) {
	    SearchBuilder<SAclVO> SearchByTarget = createSearchBuilder();
	    SearchByTarget.and("Target", SearchByTarget.entity().getTarget(), SearchCriteria.Op.EQ);
	    SearchByTarget.and("TargetID", SearchByTarget.entity().getTargetId(), SearchCriteria.Op.EQ);

	    TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
	    try {
		txn.start();
		SearchCriteria<SAclVO> sc = SearchByTarget.create();
		sc.setParameters("Target", target);
		sc.setParameters("TargetID", targetId);
		this.remove(sc);
		if(acl != null) {
			S3Grant[] grants = acl.getGrants();
			if(grants != null && grants.length > 0) {
				int grantOrder = 1;
				for(S3Grant grant : grants) {
					save(target, targetId, grant, grantOrder++);
				}
			}
		}
		txn.commit();
	    } finally {
		txn.close();
	    }
	    
	    
	}
	
	@Override
	public SAcl save(String target, long targetId, S3Grant grant, int grantOrder) {
		SAclVO aclEntry = new SAclVO();
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
		aclEntry = this.persist(aclEntry);
		return aclEntry;
	}
}
