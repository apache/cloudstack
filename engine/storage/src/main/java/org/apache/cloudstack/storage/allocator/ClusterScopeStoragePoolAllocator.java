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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class ClusterScopeStoragePoolAllocator extends AbstractStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(ClusterScopeStoragePoolAllocator.class);

    @Inject
    DiskOfferingDao _diskOfferingDao;

    @Override
    protected List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck) {
        logStartOfSearch(dskCh, vmProfile, plan, returnUpTo, bypassStorageTypeCheck);

        if (!bypassStorageTypeCheck && dskCh.useLocalStorage()) {
            return null;
        }

        List<StoragePool> suitablePools = new ArrayList<StoragePool>();

        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();

        if (podId == null) {
            // for zone wide storage, podId should be null. We cannot check
            // clusterId == null here because it will break ClusterWide primary
            // storage volume operation where
            // only podId is passed into this call.
            s_logger.debug("ClusterScopeStoragePoolAllocator is returning null since the pod ID is null. This may be a zone wide storage.");
            return null;
        }
        if (dskCh.getTags() != null && dskCh.getTags().length != 0) {
            s_logger.debug(String.format("Looking for pools in dc [%s], pod [%s], cluster [%s], and having tags [%s]. Disabled pools will be ignored.", dcId, podId, clusterId,
                    Arrays.toString(dskCh.getTags())));
        } else {
            s_logger.debug(String.format("Looking for pools in dc [%s], pod [%s] and cluster [%s]. Disabled pools will be ignored.", dcId, podId, clusterId));
        }

        if (s_logger.isTraceEnabled()) {
            // Log the pools details that are ignored because they are in disabled state
            logDisabledStoragePools(dcId, podId, clusterId, ScopeType.CLUSTER);
        }

        List<StoragePoolVO> pools = storagePoolDao.findPoolsByTags(dcId, podId, clusterId, dskCh.getTags());
        s_logger.debug(String.format("Found pools [%s] that match with tags [%s].", pools, Arrays.toString(dskCh.getTags())));

        // add remaining pools in cluster, that did not match tags, to avoid set
        List<StoragePoolVO> allPools = storagePoolDao.findPoolsByTags(dcId, podId, clusterId, null);
        allPools.removeAll(pools);
        for (StoragePoolVO pool : allPools) {
            s_logger.trace(String.format("Adding pool [%s] to the 'avoid' set since it did not match any tags.", pool));
            avoid.addPool(pool.getId());
        }

        if (pools.size() == 0) {
            s_logger.debug(String.format("No storage pools available for [%s] volume allocation.", ServiceOffering.StorageType.shared));
            return suitablePools;
        }

        for (StoragePoolVO pool : pools) {
            if (suitablePools.size() == returnUpTo) {
                break;
            }
            StoragePool storagePool = (StoragePool)dataStoreMgr.getPrimaryDataStore(pool.getId());
            if (filter(avoid, storagePool, dskCh, plan)) {
                s_logger.debug(String.format("Found suitable local storage pool [%s] to allocate disk [%s] to it, adding to list.", pool, dskCh));
                suitablePools.add(storagePool);
            } else {
                s_logger.debug(String.format("Adding storage pool [%s] to avoid set during allocation of disk [%s].", pool, dskCh));
                avoid.addPool(pool.getId());
            }
        }

        logEndOfSearch(suitablePools);

        return suitablePools;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        if (configDao != null) {
            Map<String, String> configs = configDao.getConfiguration(params);
            String allocationAlgorithm = configs.get("vm.allocation.algorithm");
            if (allocationAlgorithm != null) {
                this.allocationAlgorithm = allocationAlgorithm;
            }
        }
        return true;
    }
}
