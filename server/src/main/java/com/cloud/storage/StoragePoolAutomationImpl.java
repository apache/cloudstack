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
package com.cloud.storage;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.alert.AlertManager;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class StoragePoolAutomationImpl implements StoragePoolAutomation {
    protected Logger logger = LogManager.getLogger(getClass());
    @Inject
    protected VirtualMachineManager vmMgr;
    @Inject
    protected SecondaryStorageVmDao _secStrgDao;
    @Inject
    UserVmDao userVmDao;
    @Inject
    protected UserDao _userDao;
    @Inject
    protected DomainRouterDao _domrDao;
    @Inject
    protected StoragePoolHostDao _storagePoolHostDao;
    @Inject
    protected AlertManager _alertMgr;
    @Inject
    protected ConsoleProxyDao _consoleProxyDao;

    @Inject
    protected StoragePoolWorkDao _storagePoolWorkDao;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    AgentManager agentMgr;
    @Inject
    VolumeDao volumeDao;
    @Inject
    VMInstanceDao vmDao;
    @Inject
    ManagementServer server;
    @Inject
    DataStoreProviderManager providerMgr;
    @Inject
    StorageManager storageManager;

    @Override
    public boolean maintain(DataStore store) {
        return maintain(store, null);
    }

    @Override
    public boolean maintain(DataStore store, Map<String,String> details) {
        StoragePoolVO pool = primaryDataStoreDao.findById(store.getId());
        try {
            getStoragePoolForSpecification(pool);

            List<HostVO> hosts = getHostsForStoragePool(pool);

            if (setNextStateForMaintenance(hosts, pool) == StoragePoolStatus.PrepareForMaintenance) {
                removeHeartbeatForHostsFromPool(hosts, pool);
                // check to see if other ps exist
                // if they do, then we can migrate over the system vms to them
                // if they don't, then just stop all vms on this one
                List<StoragePoolVO> upPools = primaryDataStoreDao.listByStatusInZone(pool.getDataCenterId(), StoragePoolStatus.Up);
                boolean restart = !CollectionUtils.isEmpty(upPools);

                // 2. Get a list of all the ROOT volumes within this storage pool
                List<VolumeVO> allVolumes = volumeDao.findNonDestroyedVolumesByPoolId(pool.getId());
                // 3. Enqueue to the work queue
                enqueueMigrationsForVolumes(allVolumes, pool);
                // 4. Process the queue
                processMigrationWorkloads(pool, restart);
            }
        } catch (Exception e) {
            logger.error("Exception in enabling primary storage maintenance:", e);
            pool.setStatus(StoragePoolStatus.ErrorInMaintenance);
            primaryDataStoreDao.update(pool.getId(), pool);
            // TODO decide on what recovery is possible
            throw new CloudRuntimeException(e.getMessage());
        }
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        return cancelMaintain(store, null);
    }

    @Override
    public boolean cancelMaintain(DataStore store, Map<String,String> details) {
        // Change the storage state back to up
        StoragePoolVO poolVO = primaryDataStoreDao.findById(store.getId());
        StoragePool pool = (StoragePool)store;

        List<HostVO> hosts = getHostsForStoragePool(poolVO);

        if (CollectionUtils.isEmpty(hosts)) {
            return true;
        }

        Pair<Map<String, String>, Boolean> nfsMountOpts = storageManager.getStoragePoolNFSMountOpts(pool, null);
        addHeartbeatToHostsInPool(hosts, pool, nfsMountOpts);

        // 2. Get a list of pending work for this queue
        List<StoragePoolWorkVO> pendingWork = _storagePoolWorkDao.listPendingWorkForCancelMaintenanceByPoolId(poolVO.getId());

        // 3. work through the queue
        cancelMigrationWorkloads(pendingWork);
        return false;
    }

    private StoragePoolStatus setNextStateForMaintenance(List<HostVO> hosts, StoragePoolVO pool) {
        if (CollectionUtils.isEmpty(hosts)) {
            pool.setStatus(StoragePoolStatus.Maintenance);
            primaryDataStoreDao.update(pool.getId(), pool);
            return StoragePoolStatus.Maintenance;
        } else {
            // set the pool state to prepare for maintenance
            pool.setStatus(StoragePoolStatus.PrepareForMaintenance);
            primaryDataStoreDao.update(pool.getId(), pool);
            return StoragePoolStatus.PrepareForMaintenance;
        }
    }

    private void processMigrationWorkloads(StoragePoolVO pool, boolean restart) throws ResourceUnavailableException, OperationTimedoutException, InsufficientCapacityException {
        List<StoragePoolWorkVO> pendingWork = _storagePoolWorkDao.listPendingWorkForPrepareForMaintenanceByPoolId(pool.getId());

        for (StoragePoolWorkVO work : pendingWork) {
            // shut down the running vms
            VMInstanceVO vmInstance = vmDao.findById(work.getVmId());

            if (vmInstance == null) {
                continue;
            }

            switch (vmInstance.getType()) {
                case ConsoleProxy:
                case SecondaryStorageVm:
                case DomainRouter:
                    handleVmMigration(restart, work, vmInstance);
                    break;
                case User:
                    handleStopVmForMigration(work, vmInstance);
                    break;
            }
        }
    }

    private void cancelMigrationWorkloads(List<StoragePoolWorkVO> pendingWork) {
        for (StoragePoolWorkVO work : pendingWork) {
            try {
                VMInstanceVO vmInstance = vmDao.findById(work.getVmId());

                if (vmInstance == null) {
                    continue;
                }

                switch (vmInstance.getType()) {
                    case ConsoleProxy:
                    case SecondaryStorageVm:
                    case DomainRouter:
                        handleVmStart(work, vmInstance);
                        break;
                    case User:
                        handleUserVmStart(work, vmInstance);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Failed start vm", e);
                throw new CloudRuntimeException(e.toString());
            }
        }
    }

    private void handleStopVmForMigration(StoragePoolWorkVO work, VMInstanceVO vmInstance) throws ResourceUnavailableException, OperationTimedoutException {
        vmMgr.advanceStop(vmInstance.getUuid(), false);
        // update work status
        work.setStoppedForMaintenance(true);
        _storagePoolWorkDao.update(work.getId(), work);
    }

    private void handleVmMigration(boolean restart, StoragePoolWorkVO work, VMInstanceVO vmInstance) throws ResourceUnavailableException, OperationTimedoutException, InsufficientCapacityException {
        handleStopVmForMigration(work, vmInstance);

        if (restart) {
            handleVmStart(work, vmInstance);
        }
    }

    private void handleVmStart(StoragePoolWorkVO work, VMInstanceVO vmInstance) throws InsufficientCapacityException, ResourceUnavailableException, OperationTimedoutException {
        vmMgr.advanceStart(vmInstance.getUuid(), null, null);
        // update work queue
        work.setStartedAfterMaintenance(true);
        _storagePoolWorkDao.update(work.getId(), work);
    }

    private void enqueueMigrationsForVolumes(List<VolumeVO> allVolumes, StoragePoolVO pool) {
        for (VolumeVO volume : allVolumes) {
            VMInstanceVO vmInstance = vmDao.findById(volume.getInstanceId());

            if (vmInstance == null) {
                continue;
            }

            // enqueue sp work
            if (vmInstance.getState().equals(State.Running) || vmInstance.getState().equals(State.Starting) || vmInstance.getState().equals(State.Stopping)) {

                try {
                    StoragePoolWorkVO work = new StoragePoolWorkVO(vmInstance.getId(), pool.getId(), false, false, server.getId());
                    _storagePoolWorkDao.persist(work);
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Work record already exists, reusing by re-setting values");
                    }
                    StoragePoolWorkVO work = _storagePoolWorkDao.findByPoolIdAndVmId(pool.getId(), vmInstance.getId());
                    work.setStartedAfterMaintenance(false);
                    work.setStoppedForMaintenance(false);
                    work.setManagementServerId(server.getId());
                    _storagePoolWorkDao.update(work.getId(), work);
                }
            }
        }
    }

    private void removeHeartbeatForHostsFromPool(List<HostVO> hosts, StoragePool storagePool) {
        // remove heartbeat
        for (HostVO host : hosts) {
            ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(false, storagePool);
            final Answer answer = agentMgr.easySend(host.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("ModifyStoragePool false failed due to {}", ((answer == null) ? "answer null" : answer.getDetails()));
                }
            } else {
                reportSucceededModifyStorePool(storagePool, (ModifyStoragePoolAnswer) answer, host, false);
            }
        }
    }

    private void reportSucceededModifyStorePool(StoragePool storagePool, ModifyStoragePoolAnswer answer, HostVO host, boolean add) {
        if (logger.isDebugEnabled()) {
            logger.debug("ModifyStoragePool succeeded for {}", add ? "adding" : "removing");
        }
        if (storagePool.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
            logger.debug("Started synchronising datastore cluster storage pool {} with vCenter", storagePool);
            storageManager.syncDatastoreClusterStoragePool(storagePool.getId(), answer.getDatastoreClusterChildren(), host.getId());
        }
    }

    /**
     * Handling the Zone wide and cluster wide primary storage
     * if the storage scope is ZONE wide, then get all the hosts for which hypervisor ZoneWideStoragePools created to send ModifyStoragePoolCommand
     * TODO: if it's zone wide, this code will list a lot of hosts in the zone, which may cause performance/OOM issue.
     * @param pool pool to check for connected hosts
     * @return a list of connected hosts
     */
    private List<HostVO> getHostsForStoragePool(StoragePoolVO pool) {
        List<HostVO> hosts;
        if (pool.getScope().equals(ScopeType.ZONE)) {
            if (HypervisorType.Any.equals(pool.getHypervisor())) {
                hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZone(pool.getDataCenterId());
            }
            else {
                hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(pool.getHypervisor(), pool.getDataCenterId());
            }
        } else {
            hosts = _resourceMgr.listHostsInClusterByStatus(pool.getClusterId(), Status.Up);
        }
        return hosts;
    }

    /**
     * Handling Zone and Cluster wide storage scopes. Depending on the scope of the pool, check for other storage pools in the same scope
     * If the storage is ZONE wide then we pass podId and cluster id as null as they will be empty for Zone wide storage
     *
     * @param pool pool to check for other pools in the same scope
     */
    private void getStoragePoolForSpecification(StoragePoolVO pool) {
        List<StoragePoolVO> storagePools;
        if (pool.getScope() == ScopeType.ZONE) {
            storagePools = primaryDataStoreDao.listBy(pool.getDataCenterId(), null, null, ScopeType.ZONE);
        } else {
            storagePools = primaryDataStoreDao.listBy(pool.getDataCenterId(), pool.getPodId(), pool.getClusterId(), ScopeType.CLUSTER);
        }
        checkHierarchyForPreparingForMaintenance(pool, storagePools);
    }

    /**
     * If Datastore cluster is tried to prepare for maintenance then child storage pools are also kept in PrepareForMaintenance mode
     * @param pool target to put in maintenance
     * @param storagePools list of possible peers/parents/children
     */
    private static void checkHierarchyForPreparingForMaintenance(StoragePoolVO pool, List<StoragePoolVO> storagePools) {
        for (StoragePoolVO storagePool : storagePools) {
            if (!(storagePool.getParent().equals(pool.getParent()) || !pool.getParent().equals(storagePool.getId())) &&
                (storagePool.getStatus() == StoragePoolStatus.PrepareForMaintenance)) {
                    throw new CloudRuntimeException(String.format("Only one storage pool in a cluster can be in PrepareForMaintenance mode, %s is already in  PrepareForMaintenance mode ", storagePool));
            }
        }
    }

    /**
     *         // check if the vm has a root volume. If not, remove the item from the queue, the vm should be
     *         // started only when it has at least one root volume attached to it
     *         // don't allow to start vm that doesn't have a root volume
     * @param work work item to handle for this VM
     * @param vmInstance VM to start
     * @throws InsufficientCapacityException no migration target found
     * @throws ResourceUnavailableException a resource required for migration is not in the expected state
     * @throws OperationTimedoutException migration operation took too long
     */
    private void handleUserVmStart(StoragePoolWorkVO work, VMInstanceVO vmInstance) throws InsufficientCapacityException, ResourceUnavailableException, OperationTimedoutException {
        if (volumeDao.findByInstanceAndType(vmInstance.getId(), Volume.Type.ROOT).isEmpty()) {
            _storagePoolWorkDao.remove(work.getId());
        } else {
            handleVmStart(work, vmInstance);
        }
    }

    private void addHeartbeatToHostsInPool(List<HostVO> hosts, StoragePool pool, Pair<Map<String, String>, Boolean> nfsMountOpts) {
        for (HostVO host : hosts) {
            ModifyStoragePoolCommand msPoolCmd = new ModifyStoragePoolCommand(true, pool, nfsMountOpts.first());
            final Answer answer = agentMgr.easySend(host.getId(), msPoolCmd);
            if (answer == null || !answer.getResult()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("ModifyStoragePool add failed due to {}", ((answer == null) ? "answer null" : answer.getDetails()));
                }
                if (answer != null && nfsMountOpts.second()) {
                    logger.error("Unable to attach storage pool to the host {} due to {}",  host,  answer.getDetails());
                    StringBuilder exceptionSB = new StringBuilder("Unable to attach storage pool to the host ").append(host.getName());
                    String reason = storageManager.getStoragePoolMountFailureReason(answer.getDetails());
                    if (reason!= null) {
                        exceptionSB.append(". ").append(reason).append(".");
                    }
                    throw new CloudRuntimeException(exceptionSB.toString());
                }
            } else {
                storageManager.updateStoragePoolHostVOAndBytes(pool, host.getId(), (ModifyStoragePoolAnswer) answer);
                reportSucceededModifyStorePool(pool, (ModifyStoragePoolAnswer) answer, host, true);
            }
        }
    }
}
