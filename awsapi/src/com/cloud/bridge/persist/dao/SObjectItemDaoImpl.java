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

import com.cloud.bridge.model.SObjectItemVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value={SObjectItemDao.class})
public class SObjectItemDaoImpl extends GenericDaoBase<SObjectItemVO, Long> implements SObjectItemDao {
    
    	
	public SObjectItemDaoImpl() {
	}
	
	@Override
	public SObjectItemVO getByObjectIdNullVersion(long id) {
	    
	    TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
	    SearchBuilder <SObjectItemVO> SearchByID = createSearchBuilder();
	    SearchByID.and("ID", SearchByID.entity().getId(), SearchCriteria.Op.EQ);
	    
	    try {
    		txn.start();
    		SearchCriteria<SObjectItemVO> sc = SearchByID.create();
    		sc.setParameters("ID", id);
		return findOneBy(sc);
	    }finally {
		txn.close();
	    }
   	}
	
	@Override
    public List<SObjectItemVO> getItems(long sobjectID) {

	    TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        SearchBuilder<SObjectItemVO> SearchBySobjectID = createSearchBuilder();
        SearchBySobjectID.and("SObjectID", SearchBySobjectID.entity().getId(), SearchCriteria.Op.EQ);

        try {
            txn.start();
            SearchCriteria<SObjectItemVO> sc = SearchBySobjectID.create();
            sc.setParameters("SObjectID", sobjectID);
            return listBy(sc);
            //findOneIncludingRemovedBy(sc);
        } finally {
            txn.close();
        }
    }

}
