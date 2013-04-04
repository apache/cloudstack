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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.StoragePool;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Component
@Local(value=StoragePoolAllocator.class)
public class ClusterScopeStoragePoolAllocator extends AbstractStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(ClusterScopeStoragePoolAllocator.class);
    protected String _allocationAlgorithm = "random";

    @Inject
    DiskOfferingDao _diskOfferingDao;

    @Override
	protected List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
	    
        s_logger.debug("ClusterScopeStoragePoolAllocator looking for storage pool");
    	List<StoragePool> suitablePools = new ArrayList<StoragePool>();

		long dcId = plan.getDataCenterId();
		Long podId = plan.getPodId();
		Long clusterId = plan.getClusterId();

        if(dskCh.getTags() != null && dskCh.getTags().length != 0){
        	s_logger.debug("Looking for pools in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId + " having tags:" + Arrays.toString(dskCh.getTags()));
        }else{
        	s_logger.debug("Looking for pools in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId);
        }

        List<StoragePoolVO> pools = _storagePoolDao.findPoolsByTags(dcId, podId, clusterId, dskCh.getTags());
        if (pools.size() == 0) {
            if (s_logger.isDebugEnabled()) {
                String storageType = dskCh.useLocalStorage() ? ServiceOffering.StorageType.local.toString() : ServiceOffering.StorageType.shared.toString();
                s_logger.debug("No storage pools available for " + storageType + " volume allocation, returning");
            }
            return suitablePools;
        }
    	
        for (StoragePoolVO pool: pools) {
        	if(suitablePools.size() == returnUpTo){
        		break;
        	}
        	StoragePool pol = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(pool.getId());
        	if (filter(avoid, pol, dskCh, plan)) {
        		suitablePools.add(pol);
        	}
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("FirstFitStoragePoolAllocator returning "+suitablePools.size() +" suitable storage pools");
        }
        
        return suitablePools;
	}

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        if (_configDao != null) {
            Map<String, String> configs = _configDao.getConfiguration(params);
            String allocationAlgorithm = configs.get("vm.allocation.algorithm");
            if (allocationAlgorithm != null) {
                _allocationAlgorithm = allocationAlgorithm;
            }
        }
        return true;
    }
}
