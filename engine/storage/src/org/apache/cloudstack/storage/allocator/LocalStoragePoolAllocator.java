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
package org.apache.cloudstack.storage.allocator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
@Local(value = StoragePoolAllocator.class)
public class LocalStoragePoolAllocator extends AbstractStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(LocalStoragePoolAllocator.class);

    @Inject
    StoragePoolHostDao _poolHostDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    UserVmDao _vmDao;
    @Inject
    ServiceOfferingDao _offeringDao;
    @Inject
    CapacityDao _capacityDao;
    @Inject
    ConfigurationDao _configDao;

    @Override
    protected List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {

        List<StoragePool> suitablePools = new ArrayList<StoragePool>();

        s_logger.debug("LocalStoragePoolAllocator trying to find storage pool to fit the vm");

        if (!dskCh.useLocalStorage()) {
            return suitablePools;
        }
        
        // data disk and host identified from deploying vm (attach volume case)
        if (dskCh.getType() == Volume.Type.DATADISK && plan.getHostId() != null) {
            List<StoragePoolHostVO> hostPools = _poolHostDao.listByHostId(plan.getHostId());
            for (StoragePoolHostVO hostPool: hostPools) {
                StoragePoolVO pool = _storagePoolDao.findById(hostPool.getPoolId());
                if (pool != null && pool.isLocal()) {
                	StoragePool pol = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(pool.getId());
                	if (filter(avoid, pol, dskCh, plan)) {
                		s_logger.debug("Found suitable local storage pool " + pool.getId() + ", adding to list");
                		suitablePools.add(pol);
                	}
                }

                if (suitablePools.size() == returnUpTo) {
                    break;
                }
            }
        } else {
        	List<StoragePoolVO> availablePools = _storagePoolDao.findLocalStoragePoolsByTags(plan.getDataCenterId(), plan.getPodId(), plan.getClusterId(), dskCh.getTags());
        	for (StoragePoolVO pool : availablePools) {
        		if (suitablePools.size() == returnUpTo) {
            		break;
            	}
        		StoragePool pol = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(pool.getId());
        		if (filter(avoid, pol, dskCh, plan)) {
        			suitablePools.add(pol);
        		}
        	}
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("LocalStoragePoolAllocator returning " + suitablePools.size() + " suitable storage pools");
        }

        return suitablePools;
    }
   
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _storageOverprovisioningFactor = new BigDecimal(1);
        _extraBytesPerVolume = NumbersUtil.parseLong((String) params.get("extra.bytes.per.volume"), 50 * 1024L * 1024L);

        return true;
    }

    public LocalStoragePoolAllocator() {
    }
}
