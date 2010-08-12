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

import java.util.List;

import javax.ejb.Local;

import com.cloud.dc.ClusterVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=ClusterDao.class)
public class ClusterDaoImpl extends GenericDaoBase<ClusterVO, Long> implements ClusterDao {

    protected final SearchBuilder<ClusterVO> PodSearch;
    
    protected ClusterDaoImpl() {
        super();
        
        PodSearch = createSearchBuilder();
        PodSearch.and("pod", PodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodSearch.and("name", PodSearch.entity().getName(), SearchCriteria.Op.EQ);
        PodSearch.done();
    }
    
    @Override
    public List<ClusterVO> listByPodId(long podId) {
        SearchCriteria sc = PodSearch.create();
        sc.setParameters("pod", podId);
        
        return listActiveBy(sc);
    }
    
    @Override
    public ClusterVO findBy(String name, long podId) {
        SearchCriteria sc = PodSearch.create();
        sc.setParameters("pod", podId);
        sc.setParameters("name", name);
        
        return findOneActiveBy(sc);
    }
}
