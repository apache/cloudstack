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
package com.cloud.host.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.host.DetailVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value=DetailsDao.class)
public class DetailsDaoImpl extends GenericDaoBase<DetailVO, Long> implements DetailsDao {
    protected final SearchBuilder<DetailVO> HostSearch;
    protected final SearchBuilder<DetailVO> DetailSearch;
    
    protected DetailsDaoImpl() {
        HostSearch = createSearchBuilder();
        HostSearch.and("hostId", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();
        
        DetailSearch = createSearchBuilder();
        DetailSearch.and("hostId", DetailSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }

    @Override
    public DetailVO findDetail(long hostId, String name) {
        SearchCriteria sc = DetailSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("name", name);
        
        return findOneBy(sc);
    }

    @Override
    public Map<String, String> findDetails(long hostId) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        
        List<DetailVO> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (DetailVO result : results) {
            details.put(result.getName(), result.getValue());
        }
        return details;
    }
    
    @Override
    public void deleteDetails(long hostId) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        
        List<DetailVO> results = search(sc, null);
        for (DetailVO result : results) {
        	delete(result.getId());
        }
    }

    @Override
    public void persist(long hostId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        delete(sc);
        
        for (Map.Entry<String, String> detail : details.entrySet()) {
            DetailVO vo = new DetailVO(hostId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }
}
