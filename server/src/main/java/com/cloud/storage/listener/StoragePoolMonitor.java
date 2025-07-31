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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.StorageConflictException;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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

public class StoragePoolMonitor implements Listener {
    protected Logger logger = LogManager.getLogger(getClass());
    private final StorageManagerImpl _storageManager;
    private final PrimaryDataStoreDao _poolDao;
    private DataStoreProviderManager _dataStoreProviderMgr;
    private final StoragePoolHostDao _storagePoolHostDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    HostPodDao _podDao;
    @Inject
    OCFS2Manager _ocfs2Mgr;

    public StoragePoolMonitor(StorageManagerImpl mgr, PrimaryDataStoreDao poolDao, StoragePoolHostDao storagePoolHostDao, DataStoreProviderManager dataStoreProviderMgr) {
        _storageManager = mgr;
        _poolDao = poolDao;
        _storagePoolHostDao = storagePoolHostDao;
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
                        logger.error("hostAdded(long) failed for storage provider " + provider.getName(), ex);
                    }
                }
            }
        }
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand) || cmd.isConnectionTransferred()) {
            return;
        }

        StartupRoutingCommand scCmd = (StartupRoutingCommand)cmd;
        if (scCmd.getHypervisorType() == HypervisorType.XenServer || scCmd.getHypervisorType() ==  HypervisorType.KVM ||
                scCmd.getHypervisorType() == HypervisorType.VMware || scCmd.getHypervisorType() ==  HypervisorType.Simulator ||
                scCmd.getHypervisorType() == HypervisorType.Ovm || scCmd.getHypervisorType() == HypervisorType.Hyperv ||
                scCmd.getHypervisorType() == HypervisorType.LXC || scCmd.getHypervisorType() == HypervisorType.Ovm3) {
            String sags[] = _storageManager.getStorageAccessGroups(null, null, null, host.getId());

            List<StoragePoolVO> pools = new ArrayList<>();
            // SAG -> Storage Access Group
            if (ArrayUtils.isEmpty(sags)) {
                List<StoragePoolVO> clusterStoragePoolsByEmptySAGs = _poolDao.findStoragePoolsByEmptyStorageAccessGroups(host.getDataCenterId(), host.getPodId(), host.getClusterId(), ScopeType.CLUSTER, null);
                List<StoragePoolVO> storagePoolsByEmptySAGs = _poolDao.findStoragePoolsByEmptyStorageAccessGroups(host.getDataCenterId(), null, null, ScopeType.ZONE, null);
                List<StoragePoolVO> zoneStoragePoolsByHypervisor = _poolDao.findStoragePoolsByEmptyStorageAccessGroups(host.getDataCenterId(), null, null, ScopeType.ZONE, scCmd.getHypervisorType());
                storagePoolsByEmptySAGs.retainAll(zoneStoragePoolsByHypervisor);
                pools.addAll(storagePoolsByEmptySAGs);
                pools.addAll(clusterStoragePoolsByEmptySAGs);
                List<StoragePoolVO> zoneStoragePoolsByAnyHypervisor = _poolDao.findStoragePoolsByEmptyStorageAccessGroups(host.getDataCenterId(), null, null, ScopeType.ZONE, HypervisorType.Any);
                pools.addAll(zoneStoragePoolsByAnyHypervisor);
            } else {
                List<StoragePoolVO> storagePoolsBySAGs = new ArrayList<>();
                List<StoragePoolVO> clusterStoragePoolsBySAGs = _poolDao.findPoolsByAccessGroupsForHostConnection(host.getDataCenterId(), host.getPodId(), host.getClusterId(), ScopeType.CLUSTER, sags);
                List<StoragePoolVO> clusterStoragePoolsByEmptySAGs = _poolDao.findStoragePoolsByEmptyStorageAccessGroups(host.getDataCenterId(), host.getPodId(), host.getClusterId(), ScopeType.CLUSTER, null);
                List<StoragePoolVO> zoneStoragePoolsBySAGs = _poolDao.findZoneWideStoragePoolsByAccessGroupsAndHypervisorTypeForHostConnection(host.getDataCenterId(), sags, scCmd.getHypervisorType());
                List<StoragePoolVO> zoneStoragePoolsByHypervisorTypeAny = _poolDao.findZoneWideStoragePoolsByAccessGroupsAndHypervisorTypeForHostConnection(host.getDataCenterId(), sags, HypervisorType.Any);
                List<StoragePoolVO> zoneStoragePoolsByEmptySAGs = _poolDao.findStoragePoolsByEmptyStorageAccessGroups(host.getDataCenterId(), null, null, ScopeType.ZONE, null);

                storagePoolsBySAGs.addAll(zoneStoragePoolsBySAGs);
                storagePoolsBySAGs.addAll(zoneStoragePoolsByEmptySAGs);
                storagePoolsBySAGs.addAll(zoneStoragePoolsByHypervisorTypeAny);
                storagePoolsBySAGs.addAll(clusterStoragePoolsBySAGs);
                storagePoolsBySAGs.addAll(clusterStoragePoolsByEmptySAGs);
                pools.addAll(storagePoolsBySAGs);
            }

            // get the zone wide disabled pools list if global setting is true.
            if (StorageManager.MountDisabledStoragePool.value()) {
                pools.addAll(_poolDao.findDisabledPoolsByScope(host.getDataCenterId(), null, null, ScopeType.ZONE));
            }

            // get the cluster wide disabled pool list
            if (StorageManager.MountDisabledStoragePool.valueIn(host.getClusterId())) {
                pools.addAll(_poolDao.findDisabledPoolsByScope(host.getDataCenterId(), host.getPodId(), host.getClusterId(), ScopeType.CLUSTER));
            }

            List<StoragePoolHostVO> previouslyConnectedPools = new ArrayList<>();
            previouslyConnectedPools.addAll(_storageManager.findStoragePoolsConnectedToHost(host.getId()));

            for (StoragePoolVO pool : pools) {
                if (!pool.isShared()) {
                    continue;
                }

                if (pool.getPoolType() == StoragePoolType.OCFS2 && !_ocfs2Mgr.prepareNodes(pool.getClusterId())) {
                    throw new ConnectionException(true, String.format("Unable to prepare OCFS2 nodes for pool %s", pool));
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Host {} connected, connecting host to shared pool {} and sending storage pool information ...", host, pool);
                }
                try {
                    _storageManager.connectHostToSharedPool(host, pool.getId());
                    _storageManager.createCapacityEntry(pool.getId());
                } catch (Exception e) {
                    throw new ConnectionException(true, String.format("Unable to connect host %s to storage pool %s due to %s", host, pool, e.toString()), e);
                }

                previouslyConnectedPools.removeIf(sp -> sp.getPoolId() == pool.getId());
            }

            // Disconnect any pools which are not expected to be connected
            for (StoragePoolHostVO poolToDisconnect: previouslyConnectedPools) {
                StoragePoolVO pool = _poolDao.findById(poolToDisconnect.getPoolId());
                if (!pool.isShared()) {
                    continue;
                }
                try {
                    _storageManager.disconnectHostFromSharedPool(host, pool);
                    _storagePoolHostDao.deleteStoragePoolHostDetails(host.getId(), pool.getId());
                } catch (StorageConflictException se) {
                    throw new CloudRuntimeException(String.format("Unable to disconnect the pool %s and the host %s", pool, host));
                } catch (Exception e) {
                    logger.warn(String.format("Unable to disconnect the pool %s and the host %s", pool, host), e);
                }
            }
        }
    }

    @Override
    public synchronized boolean processDisconnect(long agentId, Status state) {
        return processDisconnect(agentId, null, null, state);
    }

    @Override
    public synchronized boolean processDisconnect(long agentId, String uuid, String name, Status state) {
        Host host = _storageManager.getHost(agentId);
        if (host == null) {
            logger.warn("Agent [id: {}, uuid: {}, name: {}] not found, not disconnecting pools", agentId, uuid, name);
            return false;
        }

        if (host.getType() != Host.Type.Routing) {
            return false;
        }

        List<StoragePoolHostVO> storagePoolHosts = _storageManager.findStoragePoolsConnectedToHost(host.getId());
        if (storagePoolHosts == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No pools to disconnect for host: {}", host);
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
                _storageManager.disconnectHostFromSharedPool(host, pool);
            } catch (Exception e) {
                logger.error("Unable to disconnect host {} from storage pool {} due to {}", host, pool, e.toString());
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
                        logger.error("hostAboutToBeRemoved(long) failed for storage provider " + provider.getName(), ex);
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
                        logger.error("hostRemoved(long, long) failed for storage provider " + provider.getName(), ex);
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
