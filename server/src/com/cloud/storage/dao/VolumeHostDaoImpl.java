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
package com.cloud.storage.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.storage.VolumeHostVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.UpdateBuilder;

@Component
@Local(value={VolumeHostDao.class})
public class VolumeHostDaoImpl extends GenericDaoBase<VolumeHostVO, Long> implements VolumeHostDao {
    private static final Logger s_logger = Logger.getLogger(VolumeHostDaoImpl.class);
    protected final SearchBuilder<VolumeHostVO> HostVolumeSearch;
    protected final SearchBuilder<VolumeHostVO> ZoneVolumeSearch;
    protected final SearchBuilder<VolumeHostVO> VolumeSearch;
    protected final SearchBuilder<VolumeHostVO> HostSearch;
    protected final SearchBuilder<VolumeHostVO> HostDestroyedSearch;
    protected final SearchBuilder<VolumeHostVO> updateStateSearch;
    public VolumeHostDaoImpl(){
        HostVolumeSearch = createSearchBuilder();
        HostVolumeSearch.and("host_id", HostVolumeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostVolumeSearch.and("volume_id", HostVolumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        HostVolumeSearch.and("destroyed", HostVolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        HostVolumeSearch.done();

        ZoneVolumeSearch = createSearchBuilder();
        ZoneVolumeSearch.and("zone_id", ZoneVolumeSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        ZoneVolumeSearch.and("volume_id", ZoneVolumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        ZoneVolumeSearch.and("destroyed", ZoneVolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        ZoneVolumeSearch.done();

        HostSearch = createSearchBuilder();
        HostSearch.and("host_id", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);		
        HostSearch.and("destroyed", HostSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        HostSearch.done();

        VolumeSearch = createSearchBuilder();
        VolumeSearch.and("volume_id", VolumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeSearch.and("destroyed", VolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        VolumeSearch.done();

        HostDestroyedSearch = createSearchBuilder();
        HostDestroyedSearch.and("host_id", HostDestroyedSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostDestroyedSearch.and("destroyed", HostDestroyedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        HostDestroyedSearch.done();	
        
        updateStateSearch = this.createSearchBuilder();
        updateStateSearch.and("id", updateStateSearch.entity().getId(), Op.EQ);
        updateStateSearch.and("state", updateStateSearch.entity().getState(), Op.EQ);
        updateStateSearch.and("updatedCount", updateStateSearch.entity().getUpdatedCount(), Op.EQ);
        updateStateSearch.done();
    }



    @Override
    public VolumeHostVO findByHostVolume(long hostId, long volumeId) {
        SearchCriteria<VolumeHostVO> sc = HostVolumeSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("volume_id", volumeId);
        sc.setParameters("destroyed", false);
        return findOneIncludingRemovedBy(sc);
    }	

    @Override
    public VolumeHostVO findVolumeByZone(long volumeId, long zoneId) {
        SearchCriteria<VolumeHostVO> sc = ZoneVolumeSearch.create();
        sc.setParameters("zone_id", zoneId);
        sc.setParameters("volume_id", volumeId);
        sc.setParameters("destroyed", false);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public VolumeHostVO findByVolumeId(long volumeId) {
        SearchCriteria<VolumeHostVO> sc = VolumeSearch.create();
        sc.setParameters("volume_id", volumeId);
        sc.setParameters("destroyed", false);
        return findOneBy(sc);
    }



    @Override
    public List<VolumeHostVO> listBySecStorage(long ssHostId) {
        SearchCriteria<VolumeHostVO> sc = HostSearch.create();
        sc.setParameters("host_id", ssHostId);
        sc.setParameters("destroyed", false);
        return listAll();
    }

    @Override
    public List<VolumeHostVO> listDestroyed(long hostId){
        SearchCriteria<VolumeHostVO> sc = HostDestroyedSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("destroyed", true);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public boolean updateState(State currentState, Event event,
            State nextState, DataObjectInStore vo, Object data) {
        VolumeHostVO volHost = (VolumeHostVO) vo;
        Long oldUpdated = volHost.getUpdatedCount();
        Date oldUpdatedTime = volHost.getUpdated();
    
        
        SearchCriteria<VolumeHostVO> sc = updateStateSearch.create();
        sc.setParameters("id", volHost.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", volHost.getUpdatedCount());

        volHost.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((VolumeHostVO) vo, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            VolumeHostVO dbVol = findByIdIncludingRemoved(volHost.getId());
            if (dbVol != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=").append(dbVol.getId()).append("; state=").append(dbVol.getState()).append("; updatecount=").append(dbVol.getUpdatedCount()).append(";updatedTime=")
                        .append(dbVol.getUpdated());
                str.append(": New Data={id=").append(volHost.getId()).append("; state=").append(nextState).append("; event=").append(event).append("; updatecount=").append(volHost.getUpdatedCount())
                        .append("; updatedTime=").append(volHost.getUpdated());
                str.append(": stale Data={id=").append(volHost.getId()).append("; state=").append(currentState).append("; event=").append(event).append("; updatecount=").append(oldUpdated)
                        .append("; updatedTime=").append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update objectIndatastore: id=" + volHost.getId() + ", as there is no such object exists in the database anymore");
            }
        }
        return rows > 0;
    }

}
