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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.ValidateVcenterDetailsCommand;
import com.cloud.alert.AlertManager;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.StorageConflictException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.OCFS2Manager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolAndAccessGroupMapDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class CloudStackPrimaryDataStoreLifeCycleImpl extends BasePrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    @Inject
    protected ResourceManager _resourceMgr;
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
    ClusterDao clusterDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    VMInstanceDao vmDao;
    @Inject
    ManagementServer server;
    @Inject
    protected VirtualMachineManager vmMgr;
    @Inject
    HostPodDao podDao;
    @Inject
    protected SecondaryStorageVmDao _secStrgDao;
    @Inject
    UserVmDao userVmDao;
    @Inject
    protected UserDao _userDao;
    @Inject
    protected DomainRouterDao _domrDao;
    @Inject
    DataCenterDao zoneDao;
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
    @Inject
    protected HostDao _hostDao;
    @Inject
    private StoragePoolAndAccessGroupMapDao storagePoolAndAccessGroupMapDao;

    @SuppressWarnings("unchecked")
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        Long clusterId = (Long)dsInfos.get("clusterId");
        Long podId = (Long)dsInfos.get("podId");
        Long zoneId = (Long)dsInfos.get("zoneId");
        String providerName = (String)dsInfos.get("providerName");
        HypervisorType hypervisorType = (HypervisorType)dsInfos.get("hypervisorType");
        if (clusterId != null && podId == null) {
            throw new InvalidParameterValueException("Cluster id requires pod id");
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        Map<String, String> details = (Map<String, String>)dsInfos.get("details");
        if (dsInfos.get("capacityBytes") != null) {
            Long capacityBytes = (Long)dsInfos.get("capacityBytes");
            if (capacityBytes <= 0) {
                throw new IllegalArgumentException("'capacityBytes' must be greater than 0.");
            }
            if (details == null) {
                details = new HashMap<>();
            }
            details.put(PrimaryDataStoreLifeCycle.CAPACITY_BYTES, String.valueOf(capacityBytes));
            parameters.setCapacityBytes(capacityBytes);
        }

        if (dsInfos.get("capacityIops") != null) {
            Long capacityIops = (Long)dsInfos.get("capacityIops");
            if (capacityIops <= 0) {
                throw new IllegalArgumentException("'capacityIops' must be greater than 0.");
            }
            if (details == null) {
                details = new HashMap<>();
            }
            details.put(PrimaryDataStoreLifeCycle.CAPACITY_IOPS, String.valueOf(capacityIops));
            parameters.setCapacityIops(capacityIops);
        }

        parameters.setDetails(details);

        String tags = (String)dsInfos.get("tags");
        parameters.setTags(tags);
        parameters.setIsTagARule((Boolean)dsInfos.get("isTagARule"));

        String storageAccessGroups = (String)dsInfos.get(ApiConstants.STORAGE_ACCESS_GROUPS);
        parameters.setStorageAccessGroups(storageAccessGroups);

        String scheme = dsInfos.get("scheme").toString();
        String storageHost = dsInfos.get("host").toString();
        String hostPath = dsInfos.get("hostPath").toString();

        Object localStorage = dsInfos.get("localStorage");
        if (localStorage != null) {
            hostPath = hostPath.contains("//") ? hostPath.replaceFirst("/", "") : hostPath;
            hostPath = hostPath.replace("+", " ");
        }

        String userInfo = dsInfos.get("userInfo") != null ? dsInfos.get("userInfo").toString() : null;
        int port = dsInfos.get("port") != null ? Integer.parseInt(dsInfos.get("port").toString()) : -1;

        if (logger.isDebugEnabled()) {
            logger.debug("createPool Params @ scheme - " + scheme + " storageHost - " + storageHost + " hostPath - " + hostPath + " port - " + port);
        }
        if (scheme.equalsIgnoreCase("nfs")) {
            if (port == -1) {
                port = 2049;
            }
            parameters.setType(StoragePoolType.NetworkFilesystem);
            parameters.setHost(storageHost);
            parameters.setPort(port);
            parameters.setPath(hostPath);
        } else if (scheme.equalsIgnoreCase("cifs")) {
            if (port == -1) {
                port = 445;
            }

            parameters.setType(StoragePoolType.SMB);
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
                port = 0;
            }
            parameters.setType(StoragePoolType.RBD);
            parameters.setHost(storageHost);
            parameters.setPort(port);
            parameters.setPath(hostPath.replaceFirst("/", ""));
            parameters.setUserInfo(userInfo);
        } else if (scheme.equalsIgnoreCase("PreSetup")) {
            if (HypervisorType.VMware.equals(hypervisorType)) {
                validateVcenterDetails(zoneId, podId, clusterId,storageHost);
            }
            parameters.setType(StoragePoolType.PreSetup);
            parameters.setHost(storageHost);
            parameters.setPort(0);
            parameters.setPath(hostPath);
        } else if (scheme.equalsIgnoreCase("DatastoreCluster")) {
            if (HypervisorType.VMware.equals(hypervisorType)) {
                validateVcenterDetails(zoneId, podId, clusterId,storageHost);
            }
            parameters.setType(StoragePoolType.DatastoreCluster);
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
                    throw new IllegalArgumentException("IscsiLUN need to have clusters specified");
                }
                parameters.setType(StoragePoolType.IscsiLUN);
                parameters.setHost(storageHost);
                parameters.setPort(port);
                parameters.setPath(hostPath);
            } else {
                throw new IllegalArgumentException("iSCSI needs to have LUN number");
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
        } else if (scheme.equalsIgnoreCase("gluster")) {
            if (port == -1) {
                port = 24007;
            }
            parameters.setType(StoragePoolType.Gluster);
            parameters.setHost(storageHost);
            parameters.setPort(port);
            parameters.setPath(hostPath);
        } else {
            StoragePoolType type = StoragePoolType.valueOf(scheme);

            if (type != null) {
                parameters.setType(type);
                parameters.setHost(storageHost);
                parameters.setPort(0);
                parameters.setPath(hostPath);
            } else {
                logger.warn("Unable to figure out the scheme for URI: " + scheme);
                throw new IllegalArgumentException("Unable to figure out the scheme for URI: " + scheme);
            }
        }

        if (localStorage == null) {
            List<StoragePoolVO> pools = primaryDataStoreDao.listPoolByHostPath(storageHost, hostPath);
            if (!pools.isEmpty() && !scheme.equalsIgnoreCase("sharedmountpoint")) {
                Long oldPodId = pools.get(0).getPodId();
                throw new CloudRuntimeException("Storage pool " + hostPath + " already in use by another pod (id=" + oldPodId + ")");
            }
        }

        Object existingUuid = dsInfos.get("uuid");
        String uuid = null;

        if (existingUuid != null) {
            uuid = (String)existingUuid;
        } else if (scheme.equalsIgnoreCase("sharedmountpoint") || scheme.equalsIgnoreCase("clvm")) {
            uuid = UUID.randomUUID().toString();
        } else if ("PreSetup".equalsIgnoreCase(scheme) && !HypervisorType.VMware.equals(hypervisorType)) {
            uuid = hostPath.replace("/", "");
        } else {
            uuid = UuidUtils.nameUUIDFromBytes((storageHost + hostPath).getBytes()).toString();
        }

        List<StoragePoolVO> spHandles = primaryDataStoreDao.findIfDuplicatePoolsExistByUUID(uuid);
        if ((spHandles != null) && (spHandles.size() > 0)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Another active pool with the same uuid already exists");
            }
            throw new CloudRuntimeException("Another active pool with the same uuid already exists");
        }

        String poolName = (String)dsInfos.get("name");

        parameters.setUuid(uuid);
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setName(poolName);
        parameters.setClusterId(clusterId);
        parameters.setProviderName(providerName);
        parameters.setHypervisorType(hypervisorType);

        return dataStoreHelper.createPrimaryDataStore(parameters);
    }

    private void validateVcenterDetails(Long zoneId, Long podId, Long clusterId, String storageHost) {
        List<Long> allHostIds = _hostDao.listIdsForUpRouting(zoneId, podId, clusterId);
        if (allHostIds.isEmpty()) {
            throw new CloudRuntimeException(String.format("No host up to associate a storage pool with in zone: %s pod: %s cluster: %s",
                    zoneDao.findById(zoneId), podDao.findById(podId), clusterDao.findById(clusterId)));
        }
        for (Long hId : allHostIds) {
            ValidateVcenterDetailsCommand cmd = new ValidateVcenterDetailsCommand(storageHost);
            final Answer answer = agentMgr.easySend(hId, cmd);
            if (answer != null && answer.getResult()) {
                logger.info("Successfully validated vCenter details provided");
                return;
            } else {
                if (answer != null) {
                    throw new InvalidParameterValueException(String.format("Provided vCenter server details does not match with the existing vCenter in zone: %s",
                            zoneDao.findById(zoneId)));
                } else {
                    logger.warn("Can not validate vCenter through host {} due to ValidateVcenterDetailsCommand returns null", hostDao.findById(hId));
                }
            }
        }
        throw new CloudRuntimeException(String.format("Could not validate vCenter details through any of the hosts with in zone: %s, pod: %s, cluster: %s",
                zoneDao.findById(zoneId), podDao.findById(podId), clusterDao.findById(clusterId)));
    }

    protected boolean createStoragePool(HostVO host, StoragePool pool) {
        long hostId = host.getId();
        logger.debug("creating pool {} on  host {}", pool, host);

        if (pool.getPoolType() != StoragePoolType.NetworkFilesystem && pool.getPoolType() != StoragePoolType.Filesystem &&
                pool.getPoolType() != StoragePoolType.IscsiLUN && pool.getPoolType() != StoragePoolType.Iscsi && pool.getPoolType() != StoragePoolType.VMFS &&
                pool.getPoolType() != StoragePoolType.SharedMountPoint && pool.getPoolType() != StoragePoolType.PreSetup && pool.getPoolType() != StoragePoolType.DatastoreCluster && pool.getPoolType() != StoragePoolType.OCFS2 &&
                pool.getPoolType() != StoragePoolType.RBD && pool.getPoolType() != StoragePoolType.CLVM && pool.getPoolType() != StoragePoolType.SMB &&
                pool.getPoolType() != StoragePoolType.Gluster) {
            logger.warn(" Doesn't support storage pool type " + pool.getPoolType());
            return false;
        }
        CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, pool);
        final Answer answer = agentMgr.easySend(hostId, cmd);
        if (answer != null && answer.getResult()) {
            storageMgr.updateStorageCapabilities(pool.getId(), false);
            return true;
        } else {
            primaryDataStoreDao.expunge(pool.getId());
            String msg = "";
            if (answer != null) {
                msg = String.format("Can not create storage pool through host %s due to %s", host, answer.getDetails());
                logger.warn(msg);
            } else {
                msg = String.format("Can not create storage pool through host %s due to CreateStoragePoolCommand returns null", host);
                logger.warn(msg);
            }
            throw new CloudRuntimeException(msg);
        }
    }

    private Pair<List<Long>, Boolean> prepareOcfs2NodesIfNeeded(PrimaryDataStoreInfo primaryStore) {
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpHostsInClusterForStorageConnection(primaryStore);
        logger.debug(String.format("Attaching the pool to each of the hosts %s in the cluster: %s", hostsToConnect, primaryStore.getClusterId()));
        List<Long> hostIds = hostsToConnect.stream().map(HostVO::getId).collect(Collectors.toList());

        if (!StoragePoolType.OCFS2.equals(primaryStore.getPoolType())) {
            return new Pair<>(hostIds, true);
        }

        if (!_ocfs2Mgr.prepareNodes(hostsToConnect, primaryStore)) {
            return new Pair<>(hostIds, false);
        }
        return new Pair<>(hostIds, true);
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        PrimaryDataStoreInfo primaryStore = (PrimaryDataStoreInfo)store;
        Pair<List<Long>, Boolean> result = prepareOcfs2NodesIfNeeded(primaryStore);
        List<Long> hostIds = result.first();
        if (hostIds.isEmpty()) {
            primaryDataStoreDao.expunge(primaryStore.getId());
            throw new CloudRuntimeException("No host up to associate a storage pool with in cluster: " +
                    clusterDao.findById(primaryStore.getClusterId()));
        }
        if (!result.second()) {
            logger.warn("Can not create storage pool {} on {}", primaryStore,
                    clusterDao.findById(primaryStore.getClusterId()));
            primaryDataStoreDao.expunge(primaryStore.getId());
            return false;
        }
        for (Long hId : hostIds) {
            HostVO host = _hostDao.findById(hId);
            if (createStoragePool(host, primaryStore)) {
                break;
            }
        }
        logger.debug("In createPool Adding the pool to each of the hosts");
        storageMgr.connectHostsToPool(store, hostIds, scope, true, true);
        dataStoreHelper.attachCluster(store);
        return true;
    }

    @Override
    public boolean attachZone(DataStore store, ZoneScope scope, HypervisorType hypervisorType) {
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(store, scope.getScopeId(), hypervisorType);
        logger.debug(String.format("In createPool. Attaching the pool to each of the hosts in %s.", hostsToConnect));
        List<Long> hostIds = hostsToConnect.stream().map(HostVO::getId).collect(Collectors.toList());
        storageMgr.connectHostsToPool(store, hostIds, scope, true, true);
        dataStoreHelper.attachZone(store, hypervisorType);
        return true;
    }

    @Override
    public boolean maintain(DataStore dataStore) {
        storagePoolAutmation.maintain(dataStore);
        dataStoreHelper.maintain(dataStore);
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        storagePoolAutmation.cancelMaintain(store);
        dataStoreHelper.cancelMaintain(store);
        return true;
    }

    @DB
    @Override
    public boolean deleteDataStore(DataStore store) {
        List<StoragePoolHostVO> hostPoolRecords = _storagePoolHostDao.listByPoolId(store.getId());
        StoragePool pool = (StoragePool)store;
        boolean deleteFlag = false;
        // find the hypervisor where the storage is attached to.
        HypervisorType hType = null;
        if (hostPoolRecords.size() > 0) {
            hType = getHypervisorType(hostPoolRecords.get(0).getHostId());
        }

        // Remove the SR associated with the Xenserver
        for (StoragePoolHostVO host : hostPoolRecords) {
            DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(pool);
            final Answer answer = agentMgr.easySend(host.getHostId(), deleteCmd);

            if (answer != null && answer.getResult()) {
                deleteFlag = true;
                // if host is KVM hypervisor then send deleteStoragepoolcmd to all the kvm hosts.
                if (HypervisorType.KVM != hType) {
                    break;
                }
            } else {
                if (answer != null) {
                    logger.debug("Failed to delete storage pool: " + answer.getResult());
                }
            }
        }

        if (!hostPoolRecords.isEmpty() && !deleteFlag) {
            throw new CloudRuntimeException("Failed to delete storage pool on host");
        }

        return dataStoreHelper.deletePrimaryDataStore(store);
    }

    private HypervisorType getHypervisorType(long hostId) {
        HostVO host = _hostDao.findById(hostId);
        if (host != null)
            return host.getHypervisorType();
        return HypervisorType.None;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        DataStore dataStore = dataStoreHelper.attachHost(store, scope, existingInfo);
        if (existingInfo.getCapacityBytes() == 0) {
            try {
                storageMgr.connectHostToSharedPool(hostDao.findById(scope.getScopeId()), dataStore.getId());
            } catch (StorageUnavailableException ex) {
                logger.error("Storage unavailable ",ex);
            } catch (StorageConflictException ex) {
                logger.error("Storage already exists ",ex);
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle#migrateToObjectStore(org.apache.cloudstack.engine.subsystem.api.storage.DataStore)
     */
    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {
    }

    @Override
    public void enableStoragePool(DataStore dataStore) {
        dataStoreHelper.enable(dataStore);
    }

    @Override
    public void disableStoragePool(DataStore dataStore) {
        dataStoreHelper.disable(dataStore);
    }
}
