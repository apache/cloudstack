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
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
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
import com.cloud.storage.StoragePoolAutomation;
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

public class CloudStackPrimaryDataStoreLifeCycleImpl implements
        PrimaryDataStoreLifeCycle {
    private static final Logger s_logger = Logger
            .getLogger(CloudStackPrimaryDataStoreLifeCycleImpl.class);
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
    @Inject
    PrimaryDataStoreHelper dataStoreHelper;
    @Inject
    StoragePoolAutomation storagePoolAutmation;

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        Long clusterId = (Long) dsInfos.get("clusterId");
        Long podId = (Long) dsInfos.get("podId");
        Long zoneId = (Long) dsInfos.get("zoneId");
        String url = (String) dsInfos.get("url");
        String providerName = (String)dsInfos.get("providerName");
        if (clusterId != null && podId == null) {
            throw new InvalidParameterValueException(
                    "Cluster id requires pod id");
        }
        
        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

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
        
        parameters.setTags(tags);
        parameters.setDetails(details);

        String scheme = uri.getScheme();
        String storageHost = uri.getHost();
        String hostPath = uri.getPath();
        Object localStorage = dsInfos.get("localStorage");
        if (localStorage != null) {
            hostPath = hostPath.replaceFirst("/", "");
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
            parameters.setType(StoragePoolType.NetworkFilesystem);
            parameters.setHost(storageHost);
            parameters.setPort(port);
            parameters.setPath(hostPath);
        } else if (scheme.equalsIgnoreCase("file")) {
            if (port == -1) {
                port = 0;
            }
            parameters.setType(StoragePoolType.Filesystem);
            parameters.setHost("localhost");
            parameters.setPort(0);
            parameters.setPath(hostPath);
        } else if (scheme.equalsIgnoreCase("sharedMountPoint")) {
            parameters.setType(StoragePoolType.SharedMountPoint);
            parameters.setHost(storageHost);
            parameters.setPort(0);
            parameters.setPath(hostPath);
        } else if (scheme.equalsIgnoreCase("clvm")) {
            parameters.setType(StoragePoolType.CLVM);
            parameters.setHost(storageHost);
            parameters.setPort(0);
            parameters.setPath(hostPath.replaceFirst("/", ""));
        } else if (scheme.equalsIgnoreCase("rbd")) {
            if (port == -1) {
                port = 6789;
            }
            parameters.setType(StoragePoolType.RBD);
            parameters.setHost(storageHost);
            parameters.setPort(port);
            parameters.setPath(hostPath.replaceFirst("/", ""));
            parameters.setUserInfo(userInfo);
        } else if (scheme.equalsIgnoreCase("PreSetup")) {
            parameters.setType(StoragePoolType.PreSetup);
            parameters.setHost(storageHost);
            parameters.setPort(0);
            parameters.setPath(hostPath);
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
                parameters.setType(StoragePoolType.IscsiLUN);
                parameters.setHost(storageHost);
                parameters.setPort(port);
                parameters.setPath(hostPath);
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
            parameters.setType(StoragePoolType.ISO);
            parameters.setHost(storageHost);
            parameters.setPort(port);
            parameters.setPath(hostPath);
        } else if (scheme.equalsIgnoreCase("vmfs")) {
            parameters.setType(StoragePoolType.VMFS);
            parameters.setHost("VMFS datastore: " + hostPath);
            parameters.setPort(0);
            parameters.setPath(hostPath);
        } else if (scheme.equalsIgnoreCase("ocfs2")) {
            port = 7777;
            parameters.setType(StoragePoolType.OCFS2);
            parameters.setHost("clustered");
            parameters.setPort(port);
            parameters.setPath(hostPath);
        } else {
            StoragePoolType type = Enum.valueOf(StoragePoolType.class, scheme);
                
            if (type != null) {
                parameters.setType(type);
                parameters.setHost(storageHost);
                parameters.setPort(0);
                parameters.setPath(hostPath);
            } else {
            s_logger.warn("Unable to figure out the scheme for URI: " + uri);
            throw new IllegalArgumentException(
                    "Unable to figure out the scheme for URI: " + uri);
            }
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

        parameters.setUuid(uuid);
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setName(poolName);
        parameters.setClusterId(clusterId);
        parameters.setProviderName(providerName);
        
        return dataStoreHelper.createPrimaryDataStore(parameters);
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
        }
        
        this.dataStoreHelper.attachCluster(store);
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
    	this.dataStoreHelper.attachZone(dataStore);
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
    public boolean maintain(DataStore dataStore) {
        storagePoolAutmation.maintain(dataStore);
        this.dataStoreHelper.maintain(dataStore);
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        this.dataStoreHelper.cancelMaintain(store);
        storagePoolAutmation.cancelMaintain(store);
        return true;
    }

    @DB
    @Override
    public boolean deleteDataStore(DataStore store) {
        List<StoragePoolHostVO> hostPoolRecords = this._storagePoolHostDao
                .listByPoolId(store.getId());
        StoragePool pool = (StoragePool)store;
        boolean deleteFlag = false;
        // Remove the SR associated with the Xenserver
        for (StoragePoolHostVO host : hostPoolRecords) {
            DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(
                    pool);
            final Answer answer = agentMgr.easySend(host.getHostId(),
                    deleteCmd);

            if (answer != null && answer.getResult()) {
                deleteFlag = true;
                break;
            } else {
                if (answer != null) {
                    s_logger.debug("Failed to delete storage pool: " + answer.getResult());
                }
            }
        }
        
        if (!deleteFlag) {
            throw new CloudRuntimeException("Failed to delete storage pool on host");
        }
        
        this.dataStoreHelper.deletePrimaryDataStore(store);
        return false;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        this.dataStoreHelper.attachHost(store, scope, existingInfo);
        return true;
    }
}
