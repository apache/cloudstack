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
package org.apache.cloudstack.engine.cloud.entity.api.db.dao;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMEntityVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMReservationVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.VolumeReservationVO;
import org.springframework.stereotype.Component;

import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
@Local(value = { VMReservationDao.class })
public class VMReservationDaoImpl extends GenericDaoBase<VMReservationVO, Long> implements VMReservationDao {

    protected SearchBuilder<VMReservationVO> VmIdSearch;
    
    @Inject protected VolumeReservationDao _volumeReservationDao;
    
    public VMReservationDaoImpl() {
    }
    
    @PostConstruct
    public void init() {
        VmIdSearch = createSearchBuilder();
        VmIdSearch.and("vmId", VmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VmIdSearch.done();
    }
    
    @Override
    public VMReservationVO findByVmId(long vmId) {
        SearchCriteria<VMReservationVO> sc = VmIdSearch.create("vmId", vmId);
        VMReservationVO vmRes = findOneBy(sc);
        loadVolumeReservation(vmRes);
        return vmRes;
    }
    
    
    @Override
    public void loadVolumeReservation(VMReservationVO reservation){
        if(reservation != null){
            List<VolumeReservationVO> volumeResList = _volumeReservationDao.listVolumeReservation(reservation.getId());
            Map<Long, Long> volumeReservationMap = new HashMap<Long,Long>();
            
            for(VolumeReservationVO res : volumeResList){
                volumeReservationMap.put(res.getVolumeId(), res.getPoolId());
            }
            reservation.setVolumeReservation(volumeReservationMap);
        }
    }
    
    @Override
    @DB
    public VMReservationVO persist(VMReservationVO reservation) {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        VMReservationVO dbVO = super.persist(reservation);

        saveVolumeReservation(reservation);
        loadVolumeReservation(dbVO);

        txn.commit();

        return dbVO;
    }

    private void saveVolumeReservation(VMReservationVO reservation) {
        if(reservation.getVolumeReservation() != null){
            for(Long volumeId : reservation.getVolumeReservation().keySet()){
                VolumeReservationVO volumeReservation = new VolumeReservationVO(reservation.getVmId(), volumeId, reservation.getVolumeReservation().get(volumeId), reservation.getId());
                _volumeReservationDao.persist(volumeReservation);
            }
        }        
    }

    @Override
    public VMReservationVO findByReservationId(String reservationId) {
        VMReservationVO vmRes = super.findByUuid(reservationId);
        loadVolumeReservation(vmRes);
        return vmRes;
    }
}
