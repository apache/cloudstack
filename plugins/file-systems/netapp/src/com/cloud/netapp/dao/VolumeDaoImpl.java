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
package com.cloud.netapp.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.netapp.NetappVolumeVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component(value = "netappVolumeDaoImpl")
@Local(value = {VolumeDao.class})
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

        List<NetappVolumeVO> volList = listBy(sc);

        return (volList.size() == 0 ? null : volList.get(0));
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

        if (marker.size() > 0)
            return marker.get(0);
        else
            return null;
    }

    @Override
    public List<NetappVolumeVO> listVolumesAscending(String poolName) {
        Filter searchFilter = new Filter(NetappVolumeVO.class, "id", Boolean.TRUE, Long.valueOf(0), Long.valueOf(10000));

        SearchCriteria<NetappVolumeVO> sc = NetappListVolumeSearch.create();
        sc.setParameters("poolName", poolName);

        return listBy(sc, searchFilter);
    }

}
