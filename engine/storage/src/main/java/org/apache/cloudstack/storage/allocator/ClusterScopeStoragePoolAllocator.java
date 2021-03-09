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
        s_logger.debug("ClusterScopeStoragePoolAllocator looking for storage pool");

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
            return null;
        }
        if (dskCh.getTags() != null && dskCh.getTags().length != 0) {
            s_logger.debug("Looking for pools in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId + " having tags:" + Arrays.toString(dskCh.getTags()) +
                    ". Disabled pools will be ignored.");
        } else {
            s_logger.debug("Looking for pools in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId + ". Disabled pools will be ignored.");
        }

        if (s_logger.isTraceEnabled()) {
            // Log the pools details that are ignored because they are in disabled state
            List<StoragePoolVO> disabledPools = storagePoolDao.findDisabledPoolsByScope(dcId, podId, clusterId, ScopeType.CLUSTER);
            if (disabledPools != null && !disabledPools.isEmpty()) {
                for (StoragePoolVO pool : disabledPools) {
                    s_logger.trace("Ignoring pool " + pool + " as it is in disabled state.");
                }
            }
        }

        List<StoragePoolVO> pools = storagePoolDao.findPoolsByTags(dcId, podId, clusterId, dskCh.getTags());
        s_logger.debug("Found pools matching tags: " + pools);

        // add remaining pools in cluster, that did not match tags, to avoid set
        List<StoragePoolVO> allPools = storagePoolDao.findPoolsByTags(dcId, podId, clusterId, null);
        allPools.removeAll(pools);
        for (StoragePoolVO pool : allPools) {
            s_logger.debug("Adding pool " + pool + " to avoid set since it did not match tags");
            avoid.addPool(pool.getId());
        }

        if (pools.size() == 0) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No storage pools available for " + ServiceOffering.StorageType.shared.toString() + " volume allocation, returning");
            }
            return suitablePools;
        }

        for (StoragePoolVO pool : pools) {
            if (suitablePools.size() == returnUpTo) {
                break;
            }
            StoragePool storagePool = (StoragePool)dataStoreMgr.getPrimaryDataStore(pool.getId());
            if (filter(avoid, storagePool, dskCh, plan)) {
                suitablePools.add(storagePool);
            } else {
                avoid.addPool(pool.getId());
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("ClusterScopeStoragePoolAllocator returning " + suitablePools.size() + " suitable storage pools");
        }

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
