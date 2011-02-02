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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import com.cloud.dc.ClusterVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=ClusterDao.class)
public class ClusterDaoImpl extends GenericDaoBase<ClusterVO, Long> implements ClusterDao {

    protected final SearchBuilder<ClusterVO> PodSearch;
    protected final SearchBuilder<ClusterVO> HyTypeWithoutGuidSearch;
    protected final SearchBuilder<ClusterVO> AvailHyperSearch;
    protected final SearchBuilder<ClusterVO> ZoneSearch;
    protected ClusterDaoImpl() {
        super();
        
        HyTypeWithoutGuidSearch = createSearchBuilder();
        HyTypeWithoutGuidSearch.and("hypervisorType", HyTypeWithoutGuidSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        HyTypeWithoutGuidSearch.and("guid", HyTypeWithoutGuidSearch.entity().getGuid(), SearchCriteria.Op.NULL);
        HyTypeWithoutGuidSearch.done();
        
        PodSearch = createSearchBuilder();
        PodSearch.and("pod", PodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodSearch.and("name", PodSearch.entity().getName(), SearchCriteria.Op.EQ);
        PodSearch.done();
        
        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("dataCenterId", ZoneSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        ZoneSearch.done();
        
        AvailHyperSearch = createSearchBuilder();
        AvailHyperSearch.and("zoneId", AvailHyperSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AvailHyperSearch.groupBy(AvailHyperSearch.entity().getHypervisorType());
        AvailHyperSearch.done();
    }
    
    @Override
    public List<ClusterVO> listByZoneId(long zoneId) {
        SearchCriteria<ClusterVO> sc = ZoneSearch.create();
        sc.setParameters("dataCenterId", zoneId);        
        return listBy(sc);
    }
    
    @Override
    public List<ClusterVO> listByPodId(long podId) {
        SearchCriteria<ClusterVO> sc = PodSearch.create();
        sc.setParameters("pod", podId);
        
        return listBy(sc);
    }
    
    @Override
    public ClusterVO findBy(String name, long podId) {
        SearchCriteria<ClusterVO> sc = PodSearch.create();
        sc.setParameters("pod", podId);
        sc.setParameters("name", name);
        
        return findOneBy(sc);
    }
    
    @Override
    public List<ClusterVO> listByHyTypeWithoutGuid(String hyType) {
        SearchCriteria<ClusterVO> sc = HyTypeWithoutGuidSearch.create();
        sc.setParameters("hypervisorType", hyType);
        
        return listBy(sc);
    }
    
    @Override
    public List<HypervisorType> getAvailableHypervisorInZone(long zoneId) {
        SearchCriteria<ClusterVO> sc = AvailHyperSearch.create();
        sc.setParameters("zoneId", zoneId);
        List<ClusterVO> clusters = listBy(sc);
        List<HypervisorType> hypers = new ArrayList<HypervisorType>(4);
        for (ClusterVO cluster : clusters) {
            hypers.add(cluster.getHypervisorType());
        }
        
        return hypers;
    }
}
