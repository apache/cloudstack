/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.ha;

import com.cloud.agent.AgentManager;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.AdapterBase;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.ha.HAManager;
import org.apache.cloudstack.kvm.ha.KVMHostActivityChecker;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class KVMInvestigator extends AdapterBase implements Investigator {
    @Inject
    private HostDao _hostDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private HAManager haManager;
    @Inject
    private DataStoreProviderManager dataStoreProviderMgr;
    @Inject
    private KVMHostActivityChecker hostActivityChecker;

    @Override
    public boolean isVmAlive(com.cloud.vm.VirtualMachine vm, Host host) throws UnknownVM {
        if (haManager.isHAEligible(host)) {
            return haManager.isVMAliveOnHost(host);
        }
        Status status = getHostAgentStatus(host);
        logger.debug("HA: HOST is ineligible legacy state {} for host {}", status, host);
        if (status == null) {
            throw new UnknownVM();
        }
        if (status == Status.Up) {
            return true;
        } else {
            throw new UnknownVM();
        }
    }

    @Override
    public Status getHostAgentStatus(Host host) {
        if (host.getHypervisorType() != Hypervisor.HypervisorType.KVM && host.getHypervisorType() != Hypervisor.HypervisorType.LXC) {
            return null;
        }

        if (haManager.isHAEligible(host)) {
            return haManager.getHostStatus(host);
        }

        List<StoragePoolVO> clusterPools = _storagePoolDao.findPoolsInClusters(Collections.singletonList(host.getClusterId()), null);
        boolean storageSupportsHA = storageSupportsHA(clusterPools);
        if (!storageSupportsHA) {
            List<StoragePoolVO> zonePools = _storagePoolDao.findZoneWideStoragePoolsByHypervisor(host.getDataCenterId(), host.getHypervisorType());
            storageSupportsHA = storageSupportsHA(zonePools);
        }
        if (!storageSupportsHA) {
            logger.warn("Agent investigation was requested on host {}, but host does not support investigation" +
                    " because it has no HA supported storage. Skipping investigation.", host);
            return null;
        }

        return hostActivityChecker.getHostAgentStatus(host);
    }

    private boolean storageSupportsHA(List<StoragePoolVO> pools) {
        for (StoragePoolVO pool : pools) {
            DataStoreProvider storeProvider = dataStoreProviderMgr.getDataStoreProvider(pool.getStorageProviderName());
            DataStoreDriver storeDriver = storeProvider.getDataStoreDriver();
            if (storeDriver instanceof PrimaryDataStoreDriver) {
                PrimaryDataStoreDriver primaryStoreDriver = (PrimaryDataStoreDriver)storeDriver;
                if (primaryStoreDriver.isStorageSupportHA(pool.getPoolType())) {
                    return true;
                }
            }
        }
        return false;
    }
}
