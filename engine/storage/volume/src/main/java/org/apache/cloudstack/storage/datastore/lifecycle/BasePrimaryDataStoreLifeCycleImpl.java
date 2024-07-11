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

package org.apache.cloudstack.storage.datastore.lifecycle;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BasePrimaryDataStoreLifeCycleImpl {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    AgentManager agentMgr;
    @Inject
    protected ResourceManager resourceMgr;
    @Inject
    StorageManager storageMgr;
    @Inject
    PrimaryDataStoreHelper dataStoreHelper;
    @Inject
    protected HostDao hostDao;
    @Inject
    protected StoragePoolHostDao storagePoolHostDao;

    private List<HostVO> getPoolHostsList(ClusterScope clusterScope, HypervisorType hypervisorType) {
        List<HostVO> hosts;
        if (hypervisorType != null) {
             hosts = resourceMgr
                    .listAllHostsInOneZoneNotInClusterByHypervisor(hypervisorType, clusterScope.getZoneId(), clusterScope.getScopeId());
        } else {
            List<HypervisorType> hypervisorTypes = Arrays.asList(HypervisorType.KVM, HypervisorType.VMware);
            hosts = resourceMgr
                    .listAllHostsInOneZoneNotInClusterByHypervisors(hypervisorTypes, clusterScope.getZoneId(), clusterScope.getScopeId());
        }
        return hosts;
    }

    public void changeStoragePoolScopeToZone(DataStore store, ClusterScope clusterScope, HypervisorType hypervisorType) {
        List<HostVO> hosts = getPoolHostsList(clusterScope, hypervisorType);
        logger.debug("Changing scope of the storage pool to Zone");
        if (hosts != null) {
            for (HostVO host : hosts) {
                try {
                    storageMgr.connectHostToSharedPool(host.getId(), store.getId());
                } catch (Exception e) {
                    logger.warn("Unable to establish a connection between " + host + " and " + store, e);
                }
            }
        }
        dataStoreHelper.switchToZone(store, hypervisorType);
    }

    public void changeStoragePoolScopeToCluster(DataStore store, ClusterScope clusterScope, HypervisorType hypervisorType) {
        Pair<List<StoragePoolHostVO>, Integer> hostPoolRecords = storagePoolHostDao.listByPoolIdNotInCluster(clusterScope.getScopeId(), store.getId());
        logger.debug("Changing scope of the storage pool to Cluster");
        if (hostPoolRecords.second() > 0) {
            StoragePool pool = (StoragePool) store;
            for (StoragePoolHostVO host : hostPoolRecords.first()) {
                DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(pool);
                final Answer answer = agentMgr.easySend(host.getHostId(), deleteCmd);

                if (answer != null) {
                    if (!answer.getResult()) {
                        logger.debug("Failed to delete storage pool: " + answer.getResult());
                    } else if (HypervisorType.KVM != hypervisorType) {
                        break;
                    }
                }
            }
        }
        dataStoreHelper.switchToCluster(store, clusterScope);
    }
}
