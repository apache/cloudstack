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
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value=HostDetailsDao.class)
public class HostDetailsDaoImpl extends GenericDaoBase<DetailVO, Long> implements HostDetailsDao {
    protected final SearchBuilder<DetailVO> HostSearch;
    protected final SearchBuilder<DetailVO> DetailSearch;
    
    protected HostDetailsDaoImpl() {
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
        SearchCriteria<DetailVO> sc = DetailSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("name", name);
        
        DetailVO detail = findOneIncludingRemovedBy(sc);
        if("password".equals(name) && detail != null){
        	detail.setValue(DBEncryptionUtil.decrypt(detail.getValue()));
        }
        return detail;
    }

    @Override
    public Map<String, String> findDetails(long hostId) {
        SearchCriteria<DetailVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        
        List<DetailVO> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (DetailVO result : results) {
        	if("password".equals(result.getName())){
        		details.put(result.getName(), DBEncryptionUtil.decrypt(result.getValue()));
        	} else {
        		details.put(result.getName(), result.getValue());
        	}
        }
        return details;
    }
    
    @Override
    public void deleteDetails(long hostId) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        
        List<DetailVO> results = search(sc, null);
        for (DetailVO result : results) {
        	remove(result.getId());
        }
    }

    @Override
    public void persist(long hostId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<DetailVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        expunge(sc);
        
        for (Map.Entry<String, String> detail : details.entrySet()) {
        	String value = detail.getValue();
        	if("password".equals(detail.getKey())){
        		value = DBEncryptionUtil.encrypt(value);
        	}
            DetailVO vo = new DetailVO(hostId, detail.getKey(), value);
            persist(vo);
        }
        txn.commit();
    }
}
