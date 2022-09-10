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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.alert.AlertManager;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class StoragePoolAutomationImpl implements StoragePoolAutomation {
    private static final Logger s_logger = Logger.getLogger(StoragePoolAutomationImpl.class);
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
        Long userId = CallContext.current().getCallingUserId();
        User user = _userDao.findById(userId);
        Account account = CallContext.current().getCallingAccount();
        StoragePoolVO pool = primaryDataStoreDao.findById(store.getId());
        try {
            List<StoragePoolVO> spes = null;
            // Handling Zone and Cluster wide storage scopes.
            // if the storage is ZONE wide then we pass podid and cluster id as null as they will be empty for ZWPS
            if (pool.getScope() == ScopeType.ZONE) {
                spes = primaryDataStoreDao.listBy(pool.getDataCenterId(), null, null, ScopeType.ZONE);
            } else {
                spes = primaryDataStoreDao.listBy(pool.getDataCenterId(), pool.getPodId(), pool.getClusterId(), ScopeType.CLUSTER);
            }
            for (StoragePoolVO sp : spes) {
                if (sp.getParent() != pool.getParent() && sp.getId() != pool.getParent()) { // If Datastore cluster is tried to prepare for maintenance then child storage pools are also kept in PrepareForMaintenance mode
                    if (sp.getStatus() == StoragePoolStatus.PrepareForMaintenance) {
                        throw new CloudRuntimeException("Only one storage pool in a cluster can be in PrepareForMaintenance mode, " + sp.getId() +
                                " is already in  PrepareForMaintenance mode ");
                    }
                }
            }
            StoragePool storagePool = (StoragePool)store;

            //Handeling the Zone wide and cluster wide primay storage
            List<HostVO> hosts = new ArrayList<HostVO>();
            // if the storage scope is ZONE wide, then get all the hosts for which hypervisor ZWSP created to send Modifystoragepoolcommand
            //TODO: if it's zone wide, this code will list a lot of hosts in the zone, which may cause performance/OOM issue.
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

            if (hosts == null || hosts.size() == 0) {
                pool.setStatus(StoragePoolStatus.Maintenance);
                primaryDataStoreDao.update(pool.getId(), pool);
                return true;
            } else {
                // set the pool state to prepare for maintenance
                pool.setStatus(StoragePoolStatus.PrepareForMaintenance);
                primaryDataStoreDao.update(pool.getId(), pool);
            }
            // remove heartbeat
            for (HostVO host : hosts) {
                ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(false, storagePool);
                final Answer answer = agentMgr.easySend(host.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("ModifyStoragePool false failed due to " + ((answer == null) ? "answer null" : answer.getDetails()));
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("ModifyStoragePool false succeeded");
                    }
                    if (pool.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                        s_logger.debug(String.format("Started synchronising datastore cluster storage pool %s with vCenter", pool.getUuid()));
                        storageManager.syncDatastoreClusterStoragePool(pool.getId(), ((ModifyStoragePoolAnswer) answer).getDatastoreClusterChildren(), host.getId());
                    }
                }
            }
            // check to see if other ps exist
            // if they do, then we can migrate over the system vms to them
            // if they don't, then just stop all vms on this one
            List<StoragePoolVO> upPools = primaryDataStoreDao.listByStatusInZone(pool.getDataCenterId(), StoragePoolStatus.Up);
            boolean restart = true;
            if (upPools == null || upPools.size() == 0) {
                restart = false;
            }

            // 2. Get a list of all the ROOT volumes within this storage pool
            List<VolumeVO> allVolumes = volumeDao.findByPoolId(pool.getId());

            // 3. Enqueue to the work queue
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
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Work record already exists, re-using by re-setting values");
                        }
                        StoragePoolWorkVO work = _storagePoolWorkDao.findByPoolIdAndVmId(pool.getId(), vmInstance.getId());
                        work.setStartedAfterMaintenance(false);
                        work.setStoppedForMaintenance(false);
                        work.setManagementServerId(server.getId());
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }
            }

            // 4. Process the queue
            List<StoragePoolWorkVO> pendingWork = _storagePoolWorkDao.listPendingWorkForPrepareForMaintenanceByPoolId(pool.getId());

            for (StoragePoolWorkVO work : pendingWork) {
                // shut down the running vms
                VMInstanceVO vmInstance = vmDao.findById(work.getVmId());

                if (vmInstance == null) {
                    continue;
                }

                // if the instance is of type consoleproxy, call the console
                // proxy
                if (vmInstance.getType().equals(VirtualMachine.Type.ConsoleProxy)) {
                    // call the consoleproxymanager
                    ConsoleProxyVO consoleProxy = _consoleProxyDao.findById(vmInstance.getId());
                    vmMgr.advanceStop(consoleProxy.getUuid(), false);
                    // update work status
                    work.setStoppedForMaintenance(true);
                    _storagePoolWorkDao.update(work.getId(), work);

                    if (restart) {

                        vmMgr.advanceStart(consoleProxy.getUuid(), null, null);
                        // update work status
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type uservm, call the user vm manager
                if (vmInstance.getType() == VirtualMachine.Type.User) {
                    UserVmVO userVm = userVmDao.findById(vmInstance.getId());
                    vmMgr.advanceStop(userVm.getUuid(), false);
                    // update work status
                    work.setStoppedForMaintenance(true);
                    _storagePoolWorkDao.update(work.getId(), work);
                }

                // if the instance is of type secondary storage vm, call the
                // secondary storage vm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.SecondaryStorageVm)) {
                    SecondaryStorageVmVO secStrgVm = _secStrgDao.findById(vmInstance.getId());
                    vmMgr.advanceStop(secStrgVm.getUuid(), false);
                    // update work status
                    work.setStoppedForMaintenance(true);
                    _storagePoolWorkDao.update(work.getId(), work);

                    if (restart) {
                        vmMgr.advanceStart(secStrgVm.getUuid(), null, null);
                        // update work status
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type domain router vm, call the network
                // manager
                if (vmInstance.getType().equals(VirtualMachine.Type.DomainRouter)) {
                    DomainRouterVO domR = _domrDao.findById(vmInstance.getId());
                    vmMgr.advanceStop(domR.getUuid(), false);
                    // update work status
                    work.setStoppedForMaintenance(true);
                    _storagePoolWorkDao.update(work.getId(), work);

                    if (restart) {
                        vmMgr.advanceStart(domR.getUuid(), null, null);
                        // update work status
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }
            }
        } catch (Exception e) {
            s_logger.error("Exception in enabling primary storage maintenance:", e);
            pool.setStatus(StoragePoolStatus.ErrorInMaintenance);
            primaryDataStoreDao.update(pool.getId(), pool);
            throw new CloudRuntimeException(e.getMessage());
        }
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        // Change the storage state back to up
        Long userId = CallContext.current().getCallingUserId();
        User user = _userDao.findById(userId);
        Account account = CallContext.current().getCallingAccount();
        StoragePoolVO poolVO = primaryDataStoreDao.findById(store.getId());
        StoragePool pool = (StoragePool)store;

        //Handeling the Zone wide and cluster wide primay storage
        List<HostVO> hosts = new ArrayList<HostVO>();
        // if the storage scope is ZONE wide, then get all the hosts for which hypervisor ZWSP created to send Modifystoragepoolcommand
        if (poolVO.getScope().equals(ScopeType.ZONE)) {
            if (HypervisorType.Any.equals(pool.getHypervisor())) {
                hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZone(pool.getDataCenterId());
            }
            else {
                hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(poolVO.getHypervisor(), pool.getDataCenterId());
            }
        } else {
            hosts = _resourceMgr.listHostsInClusterByStatus(pool.getClusterId(), Status.Up);
        }

        if (hosts == null || hosts.size() == 0) {
            return true;
        }
        // add heartbeat
        for (HostVO host : hosts) {
            ModifyStoragePoolCommand msPoolCmd = new ModifyStoragePoolCommand(true, pool);
            final Answer answer = agentMgr.easySend(host.getId(), msPoolCmd);
            if (answer == null || !answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("ModifyStoragePool add failed due to " + ((answer == null) ? "answer null" : answer.getDetails()));
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("ModifyStoragePool add succeeded");
                }
                if (pool.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                    s_logger.debug(String.format("Started synchronising datastore cluster storage pool %s with vCenter", pool.getUuid()));
                    storageManager.syncDatastoreClusterStoragePool(pool.getId(), ((ModifyStoragePoolAnswer) answer).getDatastoreClusterChildren(), host.getId());
                }
            }
        }

        // 2. Get a list of pending work for this queue
        List<StoragePoolWorkVO> pendingWork = _storagePoolWorkDao.listPendingWorkForCancelMaintenanceByPoolId(poolVO.getId());

        // 3. work through the queue
        for (StoragePoolWorkVO work : pendingWork) {
            try {
                VMInstanceVO vmInstance = vmDao.findById(work.getVmId());

                if (vmInstance == null) {
                    continue;
                }

                // if the instance is of type consoleproxy, call the console
                // proxy
                if (vmInstance.getType().equals(VirtualMachine.Type.ConsoleProxy)) {

                    ConsoleProxyVO consoleProxy = _consoleProxyDao
                            .findById(vmInstance.getId());
                    vmMgr.advanceStart(consoleProxy.getUuid(), null, null);
                    // update work queue
                    work.setStartedAfterMaintenance(true);
                    _storagePoolWorkDao.update(work.getId(), work);
                }

                // if the instance is of type ssvm, call the ssvm manager
                if (vmInstance.getType().equals(
                        VirtualMachine.Type.SecondaryStorageVm)) {
                    SecondaryStorageVmVO ssVm = _secStrgDao.findById(vmInstance
                            .getId());
                    vmMgr.advanceStart(ssVm.getUuid(), null, null);

                    // update work queue
                    work.setStartedAfterMaintenance(true);
                    _storagePoolWorkDao.update(work.getId(), work);
                }

                // if the instance is of type domain router vm, call the network
                // manager
                if (vmInstance.getType().equals(VirtualMachine.Type.DomainRouter)) {
                    DomainRouterVO domR = _domrDao.findById(vmInstance.getId());
                    vmMgr.advanceStart(domR.getUuid(), null, null);
                    // update work queue
                    work.setStartedAfterMaintenance(true);
                    _storagePoolWorkDao.update(work.getId(), work);
                }

                // if the instance is of type user vm, call the user vm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.User)) {
                    // check if the vm has a root volume. If not, remove the item from the queue, the vm should be
                    // started only when it has at least one root volume attached to it
                    // don't allow to start vm that doesn't have a root volume
                    if (volumeDao.findByInstanceAndType(vmInstance.getId(), Volume.Type.ROOT).isEmpty()) {
                        _storagePoolWorkDao.remove(work.getId());
                    } else {
                        UserVmVO userVm = userVmDao.findById(vmInstance.getId());

                        vmMgr.advanceStart(userVm.getUuid(), null, null);
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }
            } catch (Exception e) {
                s_logger.debug("Failed start vm", e);
                throw new CloudRuntimeException(e.toString());
            }
        }
        return false;
    }

}
