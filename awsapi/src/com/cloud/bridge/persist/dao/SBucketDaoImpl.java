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

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.bridge.model.SBucket;
import com.cloud.bridge.model.SBucketVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value={SBucketDao.class})
public class SBucketDaoImpl extends GenericDaoBase<SBucketVO, Long> implements SBucketDao {
    	
	public SBucketDaoImpl() {
	}

	@Override
	public SBucketVO getByName(String bucketName) {
	    SearchBuilder<SBucketVO> SearchByName = createSearchBuilder();
	    SearchByName.and("Name", SearchByName.entity().getName(), SearchCriteria.Op.EQ);
	    //Transaction txn = Transaction.open(Transaction.AWSAPI_DB);
	    TransactionLegacy txn = TransactionLegacy.open("cloudbridge", TransactionLegacy.AWSAPI_DB, true);
	    try {
		txn.start();
		SearchCriteria<SBucketVO> sc = SearchByName.create();
		sc.setParameters("Name", bucketName);
		return findOneBy(sc);
		
	    }finally {
		txn.close();
	    }
	}
	
	@Override
	public List<SBucketVO> listBuckets(String canonicalId) {
	    SearchBuilder<SBucketVO> ByCanonicalID = createSearchBuilder();
	    ByCanonicalID.and("OwnerCanonicalID", ByCanonicalID.entity().getOwnerCanonicalId(), SearchCriteria.Op.EQ);
	    Filter filter = new Filter(SBucketVO.class, "createTime", Boolean.TRUE, null, null);
	    TransactionLegacy txn = TransactionLegacy.currentTxn();  // Transaction.open("cloudbridge", Transaction.AWSAPI_DB, true);
	    try {
            txn.start();
            SearchCriteria<SBucketVO> sc = ByCanonicalID.create();
            sc.setParameters("OwnerCanonicalID", canonicalId);
		return listBy(sc, filter);
	    }finally {
            txn.close();
	    }

	}
	

}
