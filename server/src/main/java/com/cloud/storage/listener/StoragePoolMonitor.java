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
package com.cloud.storage.listener;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.OCFS2Manager;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManagerImpl;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;

public class StoragePoolMonitor implements Listener {
    private static final Logger s_logger = Logger.getLogger(StoragePoolMonitor.class);
    private final StorageManagerImpl _storageManager;
    private final PrimaryDataStoreDao _poolDao;
    private DataStoreProviderManager _dataStoreProviderMgr;
    @Inject
    OCFS2Manager _ocfs2Mgr;

    public StoragePoolMonitor(StorageManagerImpl mgr, PrimaryDataStoreDao poolDao, DataStoreProviderManager dataStoreProviderMgr) {
        _storageManager = mgr;
        _poolDao = poolDao;
        _dataStoreProviderMgr = dataStoreProviderMgr;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public synchronized boolean processAnswers(long agentId, long seq, Answer[] resp) {
        return true;
    }

    @Override
    public void processHostAdded(long hostId) {
        List<DataStoreProvider> providers = _dataStoreProviderMgr.getProviders();

        if (providers != null) {
            for (DataStoreProvider provider : providers) {
                if (provider instanceof PrimaryDataStoreProvider) {
                    try {
                        HypervisorHostListener hypervisorHostListener = provider.getHostListener();

                        if (hypervisorHostListener != null) {
                            hypervisorHostListener.hostAdded(hostId);
                        }
                    }
                    catch (Exception ex) {
                        s_logger.error("hostAdded(long) failed for storage provider " + provider.getName(), ex);
                    }
                }
            }
        }
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (cmd instanceof StartupRoutingCommand) {
            StartupRoutingCommand scCmd = (StartupRoutingCommand)cmd;
            if (scCmd.getHypervisorType() == HypervisorType.XenServer || scCmd.getHypervisorType() ==  HypervisorType.KVM ||
                scCmd.getHypervisorType() == HypervisorType.VMware || scCmd.getHypervisorType() ==  HypervisorType.Simulator ||
                scCmd.getHypervisorType() == HypervisorType.Ovm || scCmd.getHypervisorType() == HypervisorType.Hyperv ||
                scCmd.getHypervisorType() == HypervisorType.LXC || scCmd.getHypervisorType() == HypervisorType.Ovm3) {
                List<StoragePoolVO> pools = _poolDao.listBy(host.getDataCenterId(), host.getPodId(), host.getClusterId(), ScopeType.CLUSTER);
                List<StoragePoolVO> zoneStoragePoolsByTags = _poolDao.findZoneWideStoragePoolsByTags(host.getDataCenterId(), null);
                List<StoragePoolVO> zoneStoragePoolsByHypervisor = _poolDao.findZoneWideStoragePoolsByHypervisor(host.getDataCenterId(), scCmd.getHypervisorType());
                zoneStoragePoolsByTags.retainAll(zoneStoragePoolsByHypervisor);
                pools.addAll(zoneStoragePoolsByTags);
                List<StoragePoolVO> zoneStoragePoolsByAnyHypervisor = _poolDao.findZoneWideStoragePoolsByHypervisor(host.getDataCenterId(), HypervisorType.Any);
                pools.addAll(zoneStoragePoolsByAnyHypervisor);

                for (StoragePoolVO pool : pools) {
                    if (pool.getStatus() != StoragePoolStatus.Up) {
                        continue;
                    }
                    if (!pool.isShared()) {
                        continue;
                    }

                    if (pool.getPoolType() == StoragePoolType.OCFS2 && !_ocfs2Mgr.prepareNodes(pool.getClusterId())) {
                        throw new ConnectionException(true, "Unable to prepare OCFS2 nodes for pool " + pool.getId());
                    }

                    Long hostId = host.getId();
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Host " + hostId + " connected, connecting host to shared pool id " + pool.getId() + " and sending storage pool information ...");
                    }
                    try {
                        _storageManager.connectHostToSharedPool(hostId, pool.getId());
                        _storageManager.createCapacityEntry(pool.getId());
                    } catch (Exception e) {
                        throw new ConnectionException(true, "Unable to connect host " + hostId + " to storage pool id " + pool.getId() + " due to " + e.toString(), e);
                    }
                }
            }
        }
    }

    @Override
    public synchronized boolean processDisconnect(long agentId, Status state) {
        Host host = _storageManager.getHost(agentId);
        if (host == null) {
            s_logger.warn("Agent: " + agentId + " not found, not disconnecting pools");
            return false;
        }

        if (host.getType() != Host.Type.Routing) {
            return false;
        }

        List<StoragePoolHostVO> storagePoolHosts = _storageManager.findStoragePoolsConnectedToHost(host.getId());
        if (storagePoolHosts == null) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("No pools to disconnect for host: " + host.getId());
            }
            return true;
        }

        boolean disconnectResult = true;
        for (StoragePoolHostVO storagePoolHost : storagePoolHosts) {
            StoragePoolVO pool = _poolDao.findById(storagePoolHost.getPoolId());
            if (pool == null) {
                continue;
            }

            if (!pool.isShared()) {
                continue;
            }

            // Handle only PowerFlex pool for now, not to impact other pools behavior
            if (pool.getPoolType() != StoragePoolType.PowerFlex) {
                continue;
            }

            try {
                _storageManager.disconnectHostFromSharedPool(host.getId(), pool.getId());
            } catch (Exception e) {
                s_logger.error("Unable to disconnect host " + host.getId() + " from storage pool id " + pool.getId() + " due to " + e.toString());
                disconnectResult = false;
            }
        }

        return disconnectResult;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
        List<DataStoreProvider> providers = _dataStoreProviderMgr.getProviders();

        if (providers != null) {
            for (DataStoreProvider provider : providers) {
                if (provider instanceof PrimaryDataStoreProvider) {
                    try {
                        HypervisorHostListener hypervisorHostListener = provider.getHostListener();

                        if (hypervisorHostListener != null) {
                            hypervisorHostListener.hostAboutToBeRemoved(hostId);
                        }
                    }
                    catch (Exception ex) {
                        s_logger.error("hostAboutToBeRemoved(long) failed for storage provider " + provider.getName(), ex);
                    }
                }
            }
        }
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
        List<DataStoreProvider> providers = _dataStoreProviderMgr.getProviders();

        if (providers != null) {
            for (DataStoreProvider provider : providers) {
                if (provider instanceof PrimaryDataStoreProvider) {
                    try {
                        HypervisorHostListener hypervisorHostListener = provider.getHostListener();

                        if (hypervisorHostListener != null) {
                            hypervisorHostListener.hostRemoved(hostId, clusterId);
                        }
                    }
                    catch (Exception ex) {
                        s_logger.error("hostRemoved(long, long) failed for storage provider " + provider.getName(), ex);
                    }
                }
            }
        }
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] req) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return true;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

}
