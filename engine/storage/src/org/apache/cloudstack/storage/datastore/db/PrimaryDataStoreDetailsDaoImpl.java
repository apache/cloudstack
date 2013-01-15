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
package org.apache.cloudstack.storage.datastore.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
public class PrimaryDataStoreDetailsDaoImpl extends GenericDaoBase<PrimaryDataStoreDetailVO, Long> implements PrimaryDataStoreDetailsDao {
    
    protected final SearchBuilder<PrimaryDataStoreDetailVO> PoolSearch = null;
    
    protected PrimaryDataStoreDetailsDaoImpl() {
        /*
        super();
        PoolSearch = createSearchBuilder();
        PoolSearch.and("pool", PoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PoolSearch.done();
        */
    }
    
    @Override
    public void update(long poolId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        SearchCriteria<PrimaryDataStoreDetailVO> sc = PoolSearch.create();
        sc.setParameters("pool", poolId);
        
        txn.start();
        expunge(sc);
        for (Map.Entry<String, String> entry : details.entrySet()) {
            PrimaryDataStoreDetailVO detail = new PrimaryDataStoreDetailVO(poolId, entry.getKey(), entry.getValue());
            persist(detail);
        }
        txn.commit();
    }
    
    @Override
    public Map<String, String> getDetails(long poolId) {
    	SearchCriteria<PrimaryDataStoreDetailVO> sc = PoolSearch.create();
    	sc.setParameters("pool", poolId);
    	
    	List<PrimaryDataStoreDetailVO> details = listBy(sc);
    	Map<String, String> detailsMap = new HashMap<String, String>();
    	for (PrimaryDataStoreDetailVO detail : details) {
    		detailsMap.put(detail.getName(), detail.getValue());
    	}
    	
    	return detailsMap;
    }
}
