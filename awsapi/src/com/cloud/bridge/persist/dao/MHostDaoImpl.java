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
import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.bridge.model.MHostVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value={MHostDao.class})
public class MHostDaoImpl extends GenericDaoBase<MHostVO, Long> implements MHostDao{
	final SearchBuilder<MHostVO> NameSearch= createSearchBuilder();

	public MHostDaoImpl() {
	}
	
	@DB
	@Override
	public MHostVO getByHostKey(String hostKey) {
	    NameSearch.and("MHostKey", NameSearch.entity().getHostKey(), SearchCriteria.Op.EQ);
	    TransactionLegacy txn = TransactionLegacy.open("cloudbridge", TransactionLegacy.AWSAPI_DB, true);
	    try {
		txn.start();
		SearchCriteria<MHostVO> sc = NameSearch.create();
		sc.setParameters("MHostKey", hostKey);
		return findOneBy(sc);
        } finally {
            txn.commit();
            txn.close();
        }
	}

    @Override
    public void updateHeartBeat(MHostVO mhost) {
        TransactionLegacy txn = TransactionLegacy.open("cloudbridge", TransactionLegacy.AWSAPI_DB, true);
        try {
            txn.start();
            update(mhost.getId(), mhost);
            txn.commit();
        }finally {
            txn.close();
        }
    }
}