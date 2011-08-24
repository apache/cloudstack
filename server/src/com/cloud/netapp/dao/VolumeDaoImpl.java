/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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
 * @author-aj
 */

package com.cloud.netapp.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.netapp.NetappVolumeVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={VolumeDao.class})
public class VolumeDaoImpl extends GenericDaoBase<NetappVolumeVO, Long> implements VolumeDao {
    private static final Logger s_logger = Logger.getLogger(VolumeDaoImpl.class);
		
    protected final SearchBuilder<NetappVolumeVO> NetappVolumeSearch;
    protected final SearchBuilder<NetappVolumeVO> NetappListVolumeSearch;
    protected final SearchBuilder<NetappVolumeVO> NetappRoundRobinMarkerSearch;
    
    @Override
    public NetappVolumeVO findVolume(String ipAddress, String aggregateName, String volumeName) {
        SearchCriteria<NetappVolumeVO> sc = NetappVolumeSearch.create();
        sc.setParameters("ipAddress", ipAddress);
        sc.setParameters("aggregateName", aggregateName);
        sc.setParameters("volumeName", volumeName);
        
        List<NetappVolumeVO>volList = listBy(sc);
        
        return(volList.size()==0?null:volList.get(0));
    }
	    
	protected VolumeDaoImpl() {
        NetappVolumeSearch = createSearchBuilder();
        NetappVolumeSearch.and("ipAddress", NetappVolumeSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        NetappVolumeSearch.and("aggregateName", NetappVolumeSearch.entity().getAggregateName(), SearchCriteria.Op.EQ);
        NetappVolumeSearch.and("volumeName", NetappVolumeSearch.entity().getVolumeName(), SearchCriteria.Op.EQ);
        NetappVolumeSearch.done();
        
        NetappListVolumeSearch = createSearchBuilder();
        NetappListVolumeSearch.and("poolName", NetappListVolumeSearch.entity().getPoolName(), SearchCriteria.Op.EQ);
        NetappListVolumeSearch.done();
        
        NetappRoundRobinMarkerSearch = createSearchBuilder();
        NetappRoundRobinMarkerSearch.and("roundRobinMarker", NetappRoundRobinMarkerSearch.entity().getRoundRobinMarker(), SearchCriteria.Op.EQ);
        NetappRoundRobinMarkerSearch.and("poolName", NetappRoundRobinMarkerSearch.entity().getPoolName(), SearchCriteria.Op.EQ);
        NetappRoundRobinMarkerSearch.done();
	}

    @Override
    public List<NetappVolumeVO> listVolumes(String poolName) {
        SearchCriteria<NetappVolumeVO> sc = NetappListVolumeSearch.create();
        sc.setParameters("poolName", poolName);
        return listBy(sc);
    }
    
    @Override
    public NetappVolumeVO returnRoundRobinMarkerInPool(String poolName, int roundRobinMarker) {
        SearchCriteria<NetappVolumeVO> sc = NetappRoundRobinMarkerSearch.create();
        sc.setParameters("roundRobinMarker", roundRobinMarker);
        sc.setParameters("poolName", poolName);
        
        List<NetappVolumeVO> marker = listBy(sc);
        
        if(marker.size()>0)
        	return marker.get(0);
        else
        	return null;
    }
    
    @Override
    public List<NetappVolumeVO> listVolumesAscending(String poolName)
    {
        Filter searchFilter = new Filter(NetappVolumeVO.class, "id", Boolean.TRUE, Long.valueOf(0), Long.valueOf(10000));

        SearchCriteria<NetappVolumeVO> sc = NetappListVolumeSearch.create();
        sc.setParameters("poolName", poolName);
        
        return listBy(sc, searchFilter);
    }
}
