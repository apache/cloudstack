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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.log4j.Logger;

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

public class BasePrimaryDataStoreLifeCycleImpl {
    private static final Logger s_logger = Logger.getLogger(BasePrimaryDataStoreLifeCycleImpl.class);
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

    private HypervisorType getHypervisorType(long hostId) {
        HostVO host = hostDao.findById(hostId);
        if (host != null)
            return host.getHypervisorType();
        return HypervisorType.None;
    }

    private List<HostVO> getPoolHostsList(ClusterScope clusterScope, HypervisorType hypervisorType) {

        List<HostVO> hosts = new ArrayList<HostVO>();

        if (hypervisorType != null) {
             hosts = resourceMgr
                    .listAllHostsInOneZoneNotInClusterByHypervisor(hypervisorType, clusterScope.getZoneId(), clusterScope.getScopeId());
        } else {
            List<HostVO> xenServerHosts = resourceMgr
                    .listAllHostsInOneZoneNotInClusterByHypervisor(HypervisorType.XenServer, clusterScope.getZoneId(), clusterScope.getScopeId());
            List<HostVO> vmWareServerHosts = resourceMgr
                    .listAllHostsInOneZoneNotInClusterByHypervisor(HypervisorType.VMware, clusterScope.getZoneId(), clusterScope.getScopeId());
            List<HostVO> kvmHosts = resourceMgr.
                    listAllHostsInOneZoneNotInClusterByHypervisor(HypervisorType.KVM, clusterScope.getZoneId(), clusterScope.getScopeId());

            hosts.addAll(xenServerHosts);
            hosts.addAll(vmWareServerHosts);
            hosts.addAll(kvmHosts);
        }
        return hosts;
    }

    public void changeStoragePoolScopeToZone(DataStore store, ClusterScope clusterScope, HypervisorType hypervisorType) {
        List<HostVO> hosts = getPoolHostsList(clusterScope, hypervisorType);
        s_logger.debug("Changing scope of the storage pool to Zone");
        for (HostVO host : hosts) {
            try {
                storageMgr.connectHostToSharedPool(host.getId(), store.getId());
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + host + " and " + store, e);
            }
        }
        dataStoreHelper.switchToZone(store, hypervisorType);
    }

    public void changeStoragePoolScopeToCluster(DataStore store, ClusterScope clusterScope, HypervisorType hypervisorType) {
        Pair<List<StoragePoolHostVO>, Integer> hostPoolRecords = storagePoolHostDao.listByPoolIdNotInCluster(clusterScope.getScopeId(), store.getId());
        s_logger.debug("Changing scope of the storage pool to Cluster");
        HypervisorType hType = null;
        if (hostPoolRecords.second() > 0) {
            hType = getHypervisorType(hostPoolRecords.first().get(0).getHostId());
        }

        StoragePool pool = (StoragePool) store;
        for (StoragePoolHostVO host : hostPoolRecords.first()) {
            DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(pool);
            final Answer answer = agentMgr.easySend(host.getHostId(), deleteCmd);

            if (answer != null && answer.getResult()) {
                if (HypervisorType.KVM != hType) {
                    break;
                }
            } else {
                if (answer != null) {
                    s_logger.debug("Failed to delete storage pool: " + answer.getResult());
                }
            }
        }
        dataStoreHelper.switchToCluster(store, clusterScope);
    }
}
