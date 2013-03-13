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
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreStatus;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.alert.AlertManager;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.OCFS2Manager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolDiscoverer;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolWorkVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
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

public class AncientPrimaryDataStoreLifeCycleImpl implements
        PrimaryDataStoreLifeCycle {
    private static final Logger s_logger = Logger
            .getLogger(AncientPrimaryDataStoreLifeCycleImpl.class);
    @Inject
    protected ResourceManager _resourceMgr;
    protected List<StoragePoolDiscoverer> _discoverers;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    protected OCFS2Manager _ocfs2Mgr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    AgentManager agentMgr;
    @Inject
    StorageManager storageMgr;
    @Inject
    protected CapacityDao _capacityDao;

    @Inject
    VolumeDao volumeDao;
    @Inject
    VMInstanceDao vmDao;
    @Inject
    ManagementServer server;
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

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        Long clusterId = (Long) dsInfos.get("clusterId");
        Long podId = (Long) dsInfos.get("podId");
        Long zoneId = (Long) dsInfos.get("zoneId");
        String url = (String) dsInfos.get("url");
        Long providerId = (Long)dsInfos.get("providerId");
        if (clusterId != null && podId == null) {
            throw new InvalidParameterValueException(
                    "Cluster id requires pod id");
        }

        URI uri = null;
        try {
            uri = new URI(UriUtils.encodeURIComponent(url));
            if (uri.getScheme() == null) {
                throw new InvalidParameterValueException("scheme is null "
                        + url + ", add nfs:// as a prefix");
            } else if (uri.getScheme().equalsIgnoreCase("nfs")) {
                String uriHost = uri.getHost();
                String uriPath = uri.getPath();
                if (uriHost == null || uriPath == null
                        || uriHost.trim().isEmpty() || uriPath.trim().isEmpty()) {
                    throw new InvalidParameterValueException(
                            "host or path is null, should be nfs://hostname/path");
                }
            } else if (uri.getScheme().equalsIgnoreCase("sharedMountPoint")) {
                String uriPath = uri.getPath();
                if (uriPath == null) {
                    throw new InvalidParameterValueException(
                            "host or path is null, should be sharedmountpoint://localhost/path");
                }
            } else if (uri.getScheme().equalsIgnoreCase("rbd")) {
                String uriPath = uri.getPath();
                if (uriPath == null) {
                    throw new InvalidParameterValueException(
                            "host or path is null, should be rbd://hostname/pool");
                }
            }
        } catch (URISyntaxException e) {
            throw new InvalidParameterValueException(url
                    + " is not a valid uri");
        }

        String tags = (String) dsInfos.get("tags");
        Map<String, String> details = (Map<String, String>) dsInfos
                .get("details");
        if (tags != null) {
            String[] tokens = tags.split(",");

            for (String tag : tokens) {
                tag = tag.trim();
                if (tag.length() == 0) {
                    continue;
                }
                details.put(tag, "true");
            }
        }

        String scheme = uri.getScheme();
        String storageHost = uri.getHost();
        String hostPath = uri.getPath();
        Object localStorage = dsInfos.get("localStorage");
        if (localStorage != null) {
            hostPath = hostPath.replace("/", "");
        }
        String userInfo = uri.getUserInfo();
        int port = uri.getPort();
        StoragePoolVO pool = null;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("createPool Params @ scheme - " + scheme
                    + " storageHost - " + storageHost + " hostPath - "
                    + hostPath + " port - " + port);
        }
        if (scheme.equalsIgnoreCase("nfs")) {
            if (port == -1) {
                port = 2049;
            }
            pool = new StoragePoolVO(StoragePoolType.NetworkFilesystem,
                    storageHost, port, hostPath);
        } else if (scheme.equalsIgnoreCase("file")) {
            if (port == -1) {
                port = 0;
            }
            pool = new StoragePoolVO(StoragePoolType.Filesystem,
                    "localhost", 0, hostPath);
        } else if (scheme.equalsIgnoreCase("sharedMountPoint")) {
            pool = new StoragePoolVO(StoragePoolType.SharedMountPoint,
                    storageHost, 0, hostPath);
        } else if (scheme.equalsIgnoreCase("clvm")) {
            pool = new StoragePoolVO(StoragePoolType.CLVM, storageHost, 0,
                    hostPath.replaceFirst("/", ""));
        } else if (scheme.equalsIgnoreCase("rbd")) {
            if (port == -1) {
                port = 6789;
            }
            pool = new StoragePoolVO(StoragePoolType.RBD, storageHost,
                    port, hostPath.replaceFirst("/", ""));
            pool.setUserInfo(userInfo);
        } else if (scheme.equalsIgnoreCase("PreSetup")) {
            pool = new StoragePoolVO(StoragePoolType.PreSetup,
                    storageHost, 0, hostPath);
        } else if (scheme.equalsIgnoreCase("iscsi")) {
            String[] tokens = hostPath.split("/");
            int lun = NumbersUtil.parseInt(tokens[tokens.length - 1], -1);
            if (port == -1) {
                port = 3260;
            }
            if (lun != -1) {
                if (clusterId == null) {
                    throw new IllegalArgumentException(
                            "IscsiLUN need to have clusters specified");
                }
                hostPath.replaceFirst("/", "");
                pool = new StoragePoolVO(StoragePoolType.IscsiLUN,
                        storageHost, port, hostPath);
            } else {
                for (StoragePoolDiscoverer discoverer : _discoverers) {
                    Map<StoragePoolVO, Map<String, String>> pools;
                    try {
                        pools = discoverer.find(zoneId, podId, uri, details);
                    } catch (DiscoveryException e) {
                        throw new IllegalArgumentException(
                                "Not enough information for discovery " + uri,
                                e);
                    }
                    if (pools != null) {
                        Map.Entry<StoragePoolVO, Map<String, String>> entry = pools
                                .entrySet().iterator().next();
                        pool = entry.getKey();
                        details = entry.getValue();
                        break;
                    }
                }
            }
        } else if (scheme.equalsIgnoreCase("iso")) {
            if (port == -1) {
                port = 2049;
            }
            pool = new StoragePoolVO(StoragePoolType.ISO, storageHost,
                    port, hostPath);
        } else if (scheme.equalsIgnoreCase("vmfs")) {
            pool = new StoragePoolVO(StoragePoolType.VMFS,
                    "VMFS datastore: " + hostPath, 0, hostPath);
        } else if (scheme.equalsIgnoreCase("ocfs2")) {
            port = 7777;
            pool = new StoragePoolVO(StoragePoolType.OCFS2, "clustered",
                    port, hostPath);
        } else {
            StoragePoolType type = Enum.valueOf(StoragePoolType.class, scheme);
                
            if (type != null) {
                pool = new StoragePoolVO(type, storageHost,
                        0, hostPath);
            } else {
            s_logger.warn("Unable to figure out the scheme for URI: " + uri);
            throw new IllegalArgumentException(
                    "Unable to figure out the scheme for URI: " + uri);
            }
        }

        if (pool == null) {
            s_logger.warn("Unable to figure out the scheme for URI: " + uri);
            throw new IllegalArgumentException(
                    "Unable to figure out the scheme for URI: " + uri);
        }

        if (localStorage == null) {
            List<StoragePoolVO> pools = primaryDataStoreDao
                    .listPoolByHostPath(storageHost, hostPath);
            if (!pools.isEmpty() && !scheme.equalsIgnoreCase("sharedmountpoint")) {
                Long oldPodId = pools.get(0).getPodId();
                throw new CloudRuntimeException("Storage pool " + uri
                        + " already in use by another pod (id=" + oldPodId + ")");
            }
        }

        long poolId = primaryDataStoreDao.getNextInSequence(Long.class, "id");
        Object existingUuid = dsInfos.get("uuid");
        String uuid = null;

        if (existingUuid != null) {
            uuid = (String)existingUuid;
        } else if (scheme.equalsIgnoreCase("sharedmountpoint")
                || scheme.equalsIgnoreCase("clvm")) {
            uuid = UUID.randomUUID().toString();
        } else if (scheme.equalsIgnoreCase("PreSetup")) {
            uuid = hostPath.replace("/", "");
        } else {
            uuid = UUID.nameUUIDFromBytes(
                    new String(storageHost + hostPath).getBytes()).toString();
        }

        List<StoragePoolVO> spHandles = primaryDataStoreDao
                .findIfDuplicatePoolsExistByUUID(uuid);
        if ((spHandles != null) && (spHandles.size() > 0)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Another active pool with the same uuid already exists");
            }
            throw new CloudRuntimeException(
                    "Another active pool with the same uuid already exists");
        }

        String poolName = (String) dsInfos.get("name");
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("In createPool Setting poolId - " + poolId
                    + " uuid - " + uuid + " zoneId - " + zoneId + " podId - "
                    + podId + " poolName - " + poolName);
        }

        pool.setId(poolId);
        pool.setUuid(uuid);
        pool.setDataCenterId(zoneId);
        pool.setPodId(podId);
        pool.setName(poolName);
        pool.setClusterId(clusterId);
        pool.setStorageProviderId(providerId);
        pool.setStatus(StoragePoolStatus.Initialized);
        pool = primaryDataStoreDao.persist(pool, details);

        return dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
    }

    protected boolean createStoragePool(long hostId, StoragePool pool) {
        s_logger.debug("creating pool " + pool.getName() + " on  host "
                + hostId);
        if (pool.getPoolType() != StoragePoolType.NetworkFilesystem
                && pool.getPoolType() != StoragePoolType.Filesystem
                && pool.getPoolType() != StoragePoolType.IscsiLUN
                && pool.getPoolType() != StoragePoolType.Iscsi
                && pool.getPoolType() != StoragePoolType.VMFS
                && pool.getPoolType() != StoragePoolType.SharedMountPoint
                && pool.getPoolType() != StoragePoolType.PreSetup
                && pool.getPoolType() != StoragePoolType.OCFS2
                && pool.getPoolType() != StoragePoolType.RBD
                && pool.getPoolType() != StoragePoolType.CLVM) {
            s_logger.warn(" Doesn't support storage pool type "
                    + pool.getPoolType());
            return false;
        }
        CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, pool);
        final Answer answer = agentMgr.easySend(hostId, cmd);
        if (answer != null && answer.getResult()) {
            return true;
        } else {
            primaryDataStoreDao.expunge(pool.getId());
            String msg = "";
            if (answer != null) {
                msg = "Can not create storage pool through host " + hostId
                        + " due to " + answer.getDetails();
                s_logger.warn(msg);
            } else {
                msg = "Can not create storage pool through host " + hostId
                        + " due to CreateStoragePoolCommand returns null";
                s_logger.warn(msg);
            }
            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        PrimaryDataStoreInfo primarystore = (PrimaryDataStoreInfo) store;
        // Check if there is host up in this cluster
        List<HostVO> allHosts = _resourceMgr.listAllUpAndEnabledHosts(
                Host.Type.Routing, primarystore.getClusterId(),
                primarystore.getPodId(), primarystore.getDataCenterId());
        if (allHosts.isEmpty()) {
            throw new CloudRuntimeException(
                    "No host up to associate a storage pool with in cluster "
                            + primarystore.getClusterId());
        }

        if (primarystore.getPoolType() == StoragePoolType.OCFS2
                && !_ocfs2Mgr.prepareNodes(allHosts, primarystore)) {
            s_logger.warn("Can not create storage pool " + primarystore
                    + " on cluster " + primarystore.getClusterId());
            primaryDataStoreDao.expunge(primarystore.getId());
            return false;
        }

        boolean success = false;
        for (HostVO h : allHosts) {
            success = createStoragePool(h.getId(), primarystore);
            if (success) {
                break;
            }
        }

        s_logger.debug("In createPool Adding the pool to each of the hosts");
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO h : allHosts) {
            try {
                this.storageMgr.connectHostToSharedPool(h.getId(),
                        primarystore.getId());
                poolHosts.add(h);
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + h
                        + " and " + primarystore, e);
            }
        }

        if (poolHosts.isEmpty()) {
            s_logger.warn("No host can access storage pool " + primarystore
                    + " on cluster " + primarystore.getClusterId());
            primaryDataStoreDao.expunge(primarystore.getId());
            return false;
        } else {
            storageMgr.createCapacityEntry(primarystore.getId());
        }
        StoragePoolVO pool = this.primaryDataStoreDao.findById(store.getId());
        pool.setScope(ScopeType.CLUSTER);
        pool.setStatus(StoragePoolStatus.Up);
        this.primaryDataStoreDao.update(pool.getId(), pool);
        return true;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope) {
    	List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.KVM, scope.getScopeId());
    	for (HostVO host : hosts) {
    		try {
    			this.storageMgr.connectHostToSharedPool(host.getId(),
    					dataStore.getId());
    		} catch (Exception e) {
    			s_logger.warn("Unable to establish a connection between " + host
    					+ " and " + dataStore, e);
    		}
    	}
    	StoragePoolVO pool = this.primaryDataStoreDao.findById(dataStore.getId());
        
        pool.setScope(ScopeType.ZONE);
        pool.setStatus(StoragePoolStatus.Up);
        this.primaryDataStoreDao.update(pool.getId(), pool);
        return true;
    }

    @Override
    public boolean dettach() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unmanaged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean maintain(long storeId) {
        Long userId = UserContext.current().getCallerUserId();
        User user = _userDao.findById(userId);
        Account account = UserContext.current().getCaller();
        StoragePoolVO pool = this.primaryDataStoreDao.findById(storeId);
        try {
            StoragePool storagePool = (StoragePool) this.dataStoreMgr
                    .getDataStore(storeId, DataStoreRole.Primary);
            List<HostVO> hosts = _resourceMgr.listHostsInClusterByStatus(
                    pool.getClusterId(), Status.Up);
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
                ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(
                        false, storagePool);
                final Answer answer = agentMgr.easySend(host.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("ModifyStoragePool false failed due to "
                                + ((answer == null) ? "answer null" : answer
                                        .getDetails()));
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("ModifyStoragePool false secceeded");
                    }
                }
            }
            // check to see if other ps exist
            // if they do, then we can migrate over the system vms to them
            // if they dont, then just stop all vms on this one
            List<StoragePoolVO> upPools = primaryDataStoreDao
                    .listByStatusInZone(pool.getDataCenterId(),
                            StoragePoolStatus.Up);
            boolean restart = true;
            if (upPools == null || upPools.size() == 0) {
                restart = false;
            }

            // 2. Get a list of all the ROOT volumes within this storage pool
            List<VolumeVO> allVolumes = this.volumeDao.findByPoolId(pool
                    .getId());

            // 3. Enqueue to the work queue
            for (VolumeVO volume : allVolumes) {
                VMInstanceVO vmInstance = vmDao
                        .findById(volume.getInstanceId());

                if (vmInstance == null) {
                    continue;
                }

                // enqueue sp work
                if (vmInstance.getState().equals(State.Running)
                        || vmInstance.getState().equals(State.Starting)
                        || vmInstance.getState().equals(State.Stopping)) {

                    try {
                        StoragePoolWorkVO work = new StoragePoolWorkVO(
                                vmInstance.getId(), pool.getId(), false, false,
                                server.getId());
                        _storagePoolWorkDao.persist(work);
                    } catch (Exception e) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Work record already exists, re-using by re-setting values");
                        }
                        StoragePoolWorkVO work = _storagePoolWorkDao
                                .findByPoolIdAndVmId(pool.getId(),
                                        vmInstance.getId());
                        work.setStartedAfterMaintenance(false);
                        work.setStoppedForMaintenance(false);
                        work.setManagementServerId(server.getId());
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }
            }

            // 4. Process the queue
            List<StoragePoolWorkVO> pendingWork = _storagePoolWorkDao
                    .listPendingWorkForPrepareForMaintenanceByPoolId(pool
                            .getId());

            for (StoragePoolWorkVO work : pendingWork) {
                // shut down the running vms
                VMInstanceVO vmInstance = vmDao.findById(work.getVmId());

                if (vmInstance == null) {
                    continue;
                }

                // if the instance is of type consoleproxy, call the console
                // proxy
                if (vmInstance.getType().equals(
                        VirtualMachine.Type.ConsoleProxy)) {
                    // call the consoleproxymanager
                    ConsoleProxyVO consoleProxy = _consoleProxyDao
                            .findById(vmInstance.getId());
                    if (!vmMgr.advanceStop(consoleProxy, true, user, account)) {
                        String errorMsg = "There was an error stopping the console proxy id: "
                                + vmInstance.getId()
                                + " ,cannot enable storage maintenance";
                        s_logger.warn(errorMsg);
                        throw new CloudRuntimeException(errorMsg);
                    } else {
                        // update work status
                        work.setStoppedForMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }

                    if (restart) {

                        if (this.vmMgr.advanceStart(consoleProxy, null, user,
                                account) == null) {
                            String errorMsg = "There was an error starting the console proxy id: "
                                    + vmInstance.getId()
                                    + " on another storage pool, cannot enable primary storage maintenance";
                            s_logger.warn(errorMsg);
                        } else {
                            // update work status
                            work.setStartedAfterMaintenance(true);
                            _storagePoolWorkDao.update(work.getId(), work);
                        }
                    }
                }

                // if the instance is of type uservm, call the user vm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.User)) {
                    UserVmVO userVm = userVmDao.findById(vmInstance.getId());
                    if (!vmMgr.advanceStop(userVm, true, user, account)) {
                        String errorMsg = "There was an error stopping the user vm id: "
                                + vmInstance.getId()
                                + " ,cannot enable storage maintenance";
                        s_logger.warn(errorMsg);
                        throw new CloudRuntimeException(errorMsg);
                    } else {
                        // update work status
                        work.setStoppedForMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type secondary storage vm, call the
                // secondary storage vm manager
                if (vmInstance.getType().equals(
                        VirtualMachine.Type.SecondaryStorageVm)) {
                    SecondaryStorageVmVO secStrgVm = _secStrgDao
                            .findById(vmInstance.getId());
                    if (!vmMgr.advanceStop(secStrgVm, true, user, account)) {
                        String errorMsg = "There was an error stopping the ssvm id: "
                                + vmInstance.getId()
                                + " ,cannot enable storage maintenance";
                        s_logger.warn(errorMsg);
                        throw new CloudRuntimeException(errorMsg);
                    } else {
                        // update work status
                        work.setStoppedForMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }

                    if (restart) {
                        if (vmMgr.advanceStart(secStrgVm, null, user, account) == null) {
                            String errorMsg = "There was an error starting the ssvm id: "
                                    + vmInstance.getId()
                                    + " on another storage pool, cannot enable primary storage maintenance";
                            s_logger.warn(errorMsg);
                        } else {
                            // update work status
                            work.setStartedAfterMaintenance(true);
                            _storagePoolWorkDao.update(work.getId(), work);
                        }
                    }
                }

                // if the instance is of type domain router vm, call the network
                // manager
                if (vmInstance.getType().equals(
                        VirtualMachine.Type.DomainRouter)) {
                    DomainRouterVO domR = _domrDao.findById(vmInstance.getId());
                    if (!vmMgr.advanceStop(domR, true, user, account)) {
                        String errorMsg = "There was an error stopping the domain router id: "
                                + vmInstance.getId()
                                + " ,cannot enable primary storage maintenance";
                        s_logger.warn(errorMsg);
                        throw new CloudRuntimeException(errorMsg);
                    } else {
                        // update work status
                        work.setStoppedForMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }

                    if (restart) {
                        if (vmMgr.advanceStart(domR, null, user, account) == null) {
                            String errorMsg = "There was an error starting the domain router id: "
                                    + vmInstance.getId()
                                    + " on another storage pool, cannot enable primary storage maintenance";
                            s_logger.warn(errorMsg);
                        } else {
                            // update work status
                            work.setStartedAfterMaintenance(true);
                            _storagePoolWorkDao.update(work.getId(), work);
                        }
                    }
                }
            }

            // 5. Update the status
            pool.setStatus(StoragePoolStatus.Maintenance);
            this.primaryDataStoreDao.update(pool.getId(), pool);

            return true;
        } catch (Exception e) {
            s_logger.error(
                    "Exception in enabling primary storage maintenance:", e);
            setPoolStateToError(pool);
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    private void setPoolStateToError(StoragePoolVO primaryStorage) {
        primaryStorage.setStatus(StoragePoolStatus.ErrorInMaintenance);
        this.primaryDataStoreDao.update(primaryStorage.getId(), primaryStorage);
    }

    @Override
    public boolean cancelMaintain(long storageId) {
        // Change the storage state back to up
        Long userId = UserContext.current().getCallerUserId();
        User user = _userDao.findById(userId);
        Account account = UserContext.current().getCaller();
        StoragePoolVO poolVO = this.primaryDataStoreDao
                .findById(storageId);
        StoragePool pool = (StoragePool) this.dataStoreMgr.getDataStore(
                storageId, DataStoreRole.Primary);
        poolVO.setStatus(StoragePoolStatus.Up);
        primaryDataStoreDao.update(storageId, poolVO);

        List<HostVO> hosts = _resourceMgr.listHostsInClusterByStatus(
                pool.getClusterId(), Status.Up);
        if (hosts == null || hosts.size() == 0) {
            return true;
        }
        // add heartbeat
        for (HostVO host : hosts) {
            ModifyStoragePoolCommand msPoolCmd = new ModifyStoragePoolCommand(
                    true, pool);
            final Answer answer = agentMgr.easySend(host.getId(), msPoolCmd);
            if (answer == null || !answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("ModifyStoragePool add failed due to "
                            + ((answer == null) ? "answer null" : answer
                                    .getDetails()));
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("ModifyStoragePool add secceeded");
                }
            }
        }

        // 2. Get a list of pending work for this queue
        List<StoragePoolWorkVO> pendingWork = _storagePoolWorkDao
                .listPendingWorkForCancelMaintenanceByPoolId(poolVO.getId());

        // 3. work through the queue
        for (StoragePoolWorkVO work : pendingWork) {
            try {
                VMInstanceVO vmInstance = vmDao.findById(work.getVmId());

                if (vmInstance == null) {
                    continue;
                }

                // if the instance is of type consoleproxy, call the console
                // proxy
                if (vmInstance.getType().equals(
                        VirtualMachine.Type.ConsoleProxy)) {

                    ConsoleProxyVO consoleProxy = _consoleProxyDao
                            .findById(vmInstance.getId());
                    if (vmMgr.advanceStart(consoleProxy, null, user, account) == null) {
                        String msg = "There was an error starting the console proxy id: "
                                + vmInstance.getId()
                                + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg);
                        throw new ExecutionException(msg);
                    } else {
                        // update work queue
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type ssvm, call the ssvm manager
                if (vmInstance.getType().equals(
                        VirtualMachine.Type.SecondaryStorageVm)) {
                    SecondaryStorageVmVO ssVm = _secStrgDao.findById(vmInstance
                            .getId());
                    if (vmMgr.advanceStart(ssVm, null, user, account) == null) {
                        String msg = "There was an error starting the ssvm id: "
                                + vmInstance.getId()
                                + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg);
                        throw new ExecutionException(msg);
                    } else {
                        // update work queue
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type ssvm, call the ssvm manager
                if (vmInstance.getType().equals(
                        VirtualMachine.Type.DomainRouter)) {
                    DomainRouterVO domR = _domrDao.findById(vmInstance.getId());
                    if (vmMgr.advanceStart(domR, null, user, account) == null) {
                        String msg = "There was an error starting the domR id: "
                                + vmInstance.getId()
                                + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg);
                        throw new ExecutionException(msg);
                    } else {
                        // update work queue
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type user vm, call the user vm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.User)) {
                    UserVmVO userVm = userVmDao.findById(vmInstance.getId());

                    if (vmMgr.advanceStart(userVm, null, user, account) == null) {

                        String msg = "There was an error starting the user vm id: "
                                + vmInstance.getId()
                                + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg);
                        throw new ExecutionException(msg);
                    } else {
                        // update work queue
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }
            } catch (Exception e) {
                s_logger.debug("Failed start vm", e);
                throw new CloudRuntimeException(e.toString());
            }
        }
        return true;
    }

    @DB
    @Override
    public boolean deleteDataStore(long storeId) {
        // for the given pool id, find all records in the storage_pool_host_ref
        List<StoragePoolHostVO> hostPoolRecords = this._storagePoolHostDao
                .listByPoolId(storeId);
        StoragePoolVO poolVO = this.primaryDataStoreDao.findById(storeId);
        StoragePool pool = (StoragePool)this.dataStoreMgr.getDataStore(storeId, DataStoreRole.Primary);
        boolean deleteFlag = false;
        Transaction txn = Transaction.currentTxn();
        try {
            // if not records exist, delete the given pool (base case)
            if (hostPoolRecords.size() == 0) {

                txn.start();
                poolVO.setUuid(null);
                this.primaryDataStoreDao.update(poolVO.getId(), poolVO);
                primaryDataStoreDao.remove(poolVO.getId());
                deletePoolStats(poolVO.getId());
                txn.commit();

                deleteFlag = true;
                return true;
            } else {
                // Remove the SR associated with the Xenserver
                for (StoragePoolHostVO host : hostPoolRecords) {
                    DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(
                            pool);
                    final Answer answer = agentMgr.easySend(host.getHostId(),
                            deleteCmd);

                    if (answer != null && answer.getResult()) {
                        deleteFlag = true;
                        break;
                    }
                }
            }
        } finally {
            if (deleteFlag) {
                // now delete the storage_pool_host_ref and storage_pool records
                txn.start();
                for (StoragePoolHostVO host : hostPoolRecords) {
                    _storagePoolHostDao.deleteStoragePoolHostDetails(
                            host.getHostId(), host.getPoolId());
                }
                poolVO.setUuid(null);
                this.primaryDataStoreDao.update(poolVO.getId(), poolVO);
                primaryDataStoreDao.remove(poolVO.getId());
                deletePoolStats(poolVO.getId());
                // Delete op_host_capacity entries
                this._capacityDao.removeBy(Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED,
                        null, null, null, poolVO.getId());
                txn.commit();

                s_logger.debug("Storage pool id=" + poolVO.getId()
                        + " is removed successfully");
                return true;
            } else {
                // alert that the storage cleanup is required
                s_logger.warn("Failed to Delete storage pool id: " + poolVO.getId());
                _alertMgr
                        .sendAlert(AlertManager.ALERT_TYPE_STORAGE_DELETE,
                                poolVO.getDataCenterId(), poolVO.getPodId(),
                                "Unable to delete storage pool id= " + poolVO.getId(),
                                "Delete storage pool command failed.  Please check logs.");
            }
        }
        return false;
    }

    @DB
    private boolean deletePoolStats(Long poolId) {
        CapacityVO capacity1 = _capacityDao.findByHostIdType(poolId,
                CapacityVO.CAPACITY_TYPE_STORAGE);
        CapacityVO capacity2 = _capacityDao.findByHostIdType(poolId,
                CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED);
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (capacity1 != null) {
            _capacityDao.remove(capacity1.getId());
        }

        if (capacity2 != null) {
            _capacityDao.remove(capacity2.getId());
        }

        txn.commit();
        return true;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        StoragePoolHostVO poolHost = _storagePoolHostDao.findByPoolHost(store.getId(), scope.getScopeId());
        if (poolHost == null) {
            poolHost = new StoragePoolHostVO(store.getId(), scope.getScopeId(), existingInfo.getLocalPath());
            _storagePoolHostDao.persist(poolHost);
        }
       
        StoragePoolVO pool = this.primaryDataStoreDao.findById(store.getId());
        pool.setScope(scope.getScopeType());
        pool.setAvailableBytes(existingInfo.getAvailableBytes());
        pool.setCapacityBytes(existingInfo.getCapacityBytes());
        pool.setStatus(StoragePoolStatus.Up);
        this.primaryDataStoreDao.update(pool.getId(), pool);
        this.storageMgr.createCapacityEntry(pool, Capacity.CAPACITY_TYPE_LOCAL_STORAGE, pool.getCapacityBytes() - pool.getAvailableBytes());
        
        return true;
    }

}
