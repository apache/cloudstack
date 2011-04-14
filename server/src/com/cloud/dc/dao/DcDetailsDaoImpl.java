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
package com.cloud.dc.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.dc.DcDetailVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value=DcDetailsDao.class)
public class DcDetailsDaoImpl extends GenericDaoBase<DcDetailVO, Long> implements DcDetailsDao {
    protected final SearchBuilder<DcDetailVO> DcSearch;
    protected final SearchBuilder<DcDetailVO> DetailSearch;
    
    protected DcDetailsDaoImpl() {
        DcSearch = createSearchBuilder();
        DcSearch.and("dcId", DcSearch.entity().getDcId(), SearchCriteria.Op.EQ);
        DcSearch.done();
        
        DetailSearch = createSearchBuilder();
        DetailSearch.and("dcId", DetailSearch.entity().getDcId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }

    @Override
    public DcDetailVO findDetail(long dcId, String name) {
        SearchCriteria<DcDetailVO> sc = DetailSearch.create();
        sc.setParameters("dcId", dcId);
        sc.setParameters("name", name);
        
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public Map<String, String> findDetails(long dcId) {
        SearchCriteria<DcDetailVO> sc = DcSearch.create();
        sc.setParameters("dcId", dcId);
        
        List<DcDetailVO> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (DcDetailVO result : results) {
            details.put(result.getName(), result.getValue());
        }
        return details;
    }
    
    @Override
    public void deleteDetails(long dcId) {
        SearchCriteria sc = DcSearch.create();
        sc.setParameters("dcId", dcId);
        
        List<DcDetailVO> results = search(sc, null);
        for (DcDetailVO result : results) {
        	remove(result.getId());
        }
    }

    @Override
    public void persist(long dcId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<DcDetailVO> sc = DcSearch.create();
        sc.setParameters("dcId", dcId);
        expunge(sc);
        
        for (Map.Entry<String, String> detail : details.entrySet()) {
            DcDetailVO vo = new DcDetailVO(dcId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }
}
