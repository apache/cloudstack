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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import com.cloud.host.HostTagVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;


@Local(value=HostTagsDao.class)
public class HostTagsDaoImpl extends GenericDaoBase<HostTagVO, Long> implements HostTagsDao {
    protected final SearchBuilder<HostTagVO> HostSearch;
    
    protected HostTagsDaoImpl() {
        HostSearch = createSearchBuilder();
        HostSearch.and("hostId", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();        
    }

    @Override
    public List<String> gethostTags(long hostId) {
        SearchCriteria<HostTagVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        
        List<HostTagVO> results = search(sc, null);
        List<String> hostTags = new ArrayList<String>(results.size());
        for (HostTagVO result : results) {
        	hostTags.add(result.getTag());
        }

        return hostTags;
    }
    
    @Override
    public void persist(long hostId, List<String> hostTags) {
        Transaction txn = Transaction.currentTxn();

        txn.start();
        SearchCriteria<HostTagVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        expunge(sc);
        
        for (String tag : hostTags) {
            tag.trim();
            if(tag.length() > 0) {
                HostTagVO vo = new HostTagVO(hostId, tag);
                persist(vo);
            }
        }
        txn.commit();
    }
}
