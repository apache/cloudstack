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


import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMEntityVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMReservationVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;


import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.NicProfile;



@Component
@Local(value = { VMEntityDao.class })
public class VMEntityDaoImpl extends GenericDaoBase<VMEntityVO, Long> implements VMEntityDao {

    public static final Logger s_logger = Logger.getLogger(VMEntityDaoImpl.class);

    
    @Inject protected VMReservationDao _vmReservationDao;

    @Inject protected VMComputeTagDao _vmComputeTagDao;
    
    @Inject protected VMRootDiskTagDao _vmRootDiskTagsDao;
    
    @Inject protected VMNetworkMapDao _vmNetworkMapDao;

    
    @Inject
    protected NetworkDao _networkDao;
    
    public VMEntityDaoImpl() {
    }
    
    @PostConstruct
    protected void init() {

    }

    @Override
    public void loadVmReservation(VMEntityVO vm) {
        VMReservationVO vmReservation = _vmReservationDao.findByVmId(vm.getId());
        vm.setVmReservation(vmReservation);
    }

    @Override
    @DB
    public VMEntityVO persist(VMEntityVO vm) {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        VMEntityVO dbVO = super.persist(vm);

        saveVmNetworks(vm);
        loadVmNetworks(dbVO);
        saveVmReservation(vm);
        loadVmReservation(dbVO);
        saveComputeTags(vm.getId(), vm.getComputeTags());
        loadComputeTags(dbVO);
        saveRootDiskTags(vm.getId(), vm.getRootDiskTags());
        loadRootDiskTags(dbVO);

        txn.commit();

        return dbVO;
    }

    private void loadVmNetworks(VMEntityVO dbVO) {
        List<Long> networksIds = _vmNetworkMapDao.getNetworks(dbVO.getId());
        
        List<String> networks = new ArrayList<String>();
        for(Long networkId : networksIds){
            NetworkVO network = _networkDao.findById(networkId);
            if(network != null){
                networks.add(network.getUuid());
            }
        }
        
        dbVO.setNetworkIds(networks);
        
    }

    private void saveVmNetworks(VMEntityVO vm) {
        List<Long> networks = new ArrayList<Long>();
        
        List<String> networksIds = vm.getNetworkIds();
        
        if (networksIds == null || (networksIds != null && networksIds.isEmpty())) {
            return;
        }

        
        for(String uuid : networksIds){
            NetworkVO network = _networkDao.findByUuid(uuid);
            if(network != null){
                networks.add(network.getId());
            }
        }
        _vmNetworkMapDao.persist(vm.getId(), networks);
        
    }

    private void loadRootDiskTags(VMEntityVO dbVO) {
        List<String> rootDiskTags = _vmRootDiskTagsDao.getRootDiskTags(dbVO.getId());
        dbVO.setRootDiskTags(rootDiskTags);
        
    }

    private void loadComputeTags(VMEntityVO dbVO) {
        List<String> computeTags = _vmComputeTagDao.getComputeTags(dbVO.getId());
        dbVO.setComputeTags(computeTags);
        
    }

    private void saveRootDiskTags(long vmId, List<String> rootDiskTags) {
        if (rootDiskTags == null || (rootDiskTags != null && rootDiskTags.isEmpty())) {
            return;
        }
        _vmRootDiskTagsDao.persist(vmId, rootDiskTags);
        
    }

    private void saveComputeTags(long vmId, List<String> computeTags) {
        if (computeTags == null || (computeTags != null && computeTags.isEmpty())) {
            return;
        }

        _vmComputeTagDao.persist(vmId, computeTags);        
    }

    private void saveVmReservation(VMEntityVO vm) {
        if(vm.getVmReservation() != null){
            _vmReservationDao.persist(vm.getVmReservation());
        }
    }

}
