/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import javax.ejb.Local;

import com.cloud.storage.StoragePoolDetailVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value=StoragePoolDetailsDao.class)
public class StoragePoolDetailsDaoImpl extends GenericDaoBase<StoragePoolDetailVO, Long> implements StoragePoolDetailsDao {
    
    protected final SearchBuilder<StoragePoolDetailVO> PoolSearch;
    
    protected StoragePoolDetailsDaoImpl() {
        super();
        PoolSearch = createSearchBuilder();
        PoolSearch.and("pool", PoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PoolSearch.done();
    }
    
    @Override
    public void update(long poolId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        SearchCriteria sc = PoolSearch.create();
        sc.setParameters("pool", poolId);
        
        txn.start();
        delete(sc);
        for (Map.Entry<String, String> entry : details.entrySet()) {
            StoragePoolDetailVO detail = new StoragePoolDetailVO(poolId, entry.getKey(), entry.getValue());
            persist(detail);
        }
        txn.commit();
    }
    
    @Override
    public Map<String, String> getDetails(long poolId) {
    	SearchCriteria sc = PoolSearch.create();
    	sc.setParameters("pool", poolId);
    	
    	List<StoragePoolDetailVO> details = listActiveBy(sc);
    	Map<String, String> detailsMap = new HashMap<String, String>();
    	for (StoragePoolDetailVO detail : details) {
    		detailsMap.put(detail.getName(), detail.getValue());
    	}
    	
    	return detailsMap;
    }
}
