//
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
//

package org.apache.cloudstack.storage.datastore.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.FileSystem;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.Tsm;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.UpdateTsmCmdResponse;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.UpdateTsmStorageCmdResponse;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class ElastistorPrimaryDataStoreLifeCycle implements PrimaryDataStoreLifeCycle {
    private static final Logger s_logger = Logger.getLogger(ElastistorPrimaryDataStoreLifeCycle.class);

    @Inject
    HostDao _hostDao;
    @Inject
    StoragePoolHostDao _storagePoolHostDao;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    StorageManager storageMgr;
    @Inject
    PrimaryDataStoreHelper dataStoreHelper;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    PrimaryDataStoreHelper _dataStoreHelper;
    @Inject
    StoragePoolAutomation _storagePoolAutomation;
    @Inject
    StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    CapacityManager _capacityMgr;

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {

        String url = (String) dsInfos.get("url");
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long) dsInfos.get("podId");
        Long clusterId = (Long) dsInfos.get("clusterId");
        String storagePoolName = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Long capacityBytes = (Long) dsInfos.get("capacityBytes");
        Long capacityIops = (Long) dsInfos.get("capacityIops");
        String tags = (String) dsInfos.get("tags");
        boolean managed = (Boolean) dsInfos.get("managed");
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");
        String domainName = details.get("domainname");

        String storageIp;
        int storagePort = 0;
        StoragePoolType storagetype = null;
        String accesspath = null;
        String protocoltype = null;
        String mountpoint = null;

        if (!managed) {
            storageIp = getStorageIp(url);
            storagePort = getDefaultStoragePort(url);
            storagetype = getStorageType(url);
            accesspath = getAccessPath(url);
            protocoltype = getProtocolType(url);
            String[] mp = accesspath.split("/");
            mountpoint = mp[1];

        } else if (details.get("hypervisortype") == "KVM") {
            storageIp = url;
            storagePort = 3260;
            storagetype = StoragePoolType.Iscsi;
            accesspath = storageIp + ":/" + storagePoolName;
        }else{
            storageIp = url;
            storagePort = 2049;
            storagetype = StoragePoolType.NetworkFilesystem;
            accesspath = storageIp + ":/" + storagePoolName;
        }
        /**
         * if the elastistor params which are required for plugin configuration
         * are not injected through spring-storage-volume-cloudbyte-context.xml,
         * it can be set from details map.
         */
        if (details.get("esaccountid") != null)
            ElastistorUtil.setElastistorAccountId(details.get("esaccountid"));
        if (details.get("esdefaultgateway") != null)
            ElastistorUtil.setElastistorGateway(details.get("esdefaultgateway"));
        if (details.get("estntinterface") != null)
            ElastistorUtil.setElastistorInterface(details.get("estntinterface"));
        if (details.get("espoolid") != null)
            ElastistorUtil.setElastistorPoolId(details.get("espoolid"));
        if (details.get("essubnet") != null)
            ElastistorUtil.setElastistorSubnet(details.get("essubnet"));

        s_logger.info("Elastistor details was set successfully.");

        if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops <= 0) {
            throw new IllegalArgumentException("'capacityIops' must be present and greater than 0.");
        }

        if (domainName == null) {
            domainName = "ROOT";
            s_logger.debug("setting the domain to ROOT");
        }

        // elastistor does not allow same name and ip pools.
        List<StoragePoolVO> storagePoolVO = _storagePoolDao.listAll();
        for (StoragePoolVO poolVO : storagePoolVO) {
            if (storagePoolName.equals(poolVO.getName())) {
                throw new IllegalArgumentException("Storage pool with this name already exists in elastistor, please specify a unique name. [name:" + storagePoolName + "]");
            }
            if (storageIp.equals(poolVO.getHostAddress())) {
                throw new IllegalArgumentException("Storage pool with this ip already exists in elastistor, please specify a unique ip. [ip:" + storageIp + "]");
            }
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        parameters.setHost(storageIp);
        parameters.setPort(storagePort);
        parameters.setPath(accesspath);
        parameters.setType(storagetype);
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(managed);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(HypervisorType.Any);
        parameters.setTags(tags);
        parameters.setDetails(details);
        parameters.setClusterId(clusterId);

        Tsm tsm = null;
        if (managed) {
            // creates the TSM in elastistor
            tsm = createElastistorTSM(storagePoolName, storageIp, capacityBytes, capacityIops, domainName);
        } else {
            // creates the TSM & Volume in elastistor
            tsm = createElastistorTSM(storagePoolName, storageIp, capacityBytes, capacityIops, domainName);

            parameters = createElastistorVolume(parameters, tsm, storagePoolName, capacityBytes, capacityIops, protocoltype, mountpoint);
        }

        // setting tsm's uuid as storagepool's uuid
        parameters.setUuid(tsm.getUuid());

        return _dataStoreHelper.createPrimaryDataStore(parameters);
    }

    private Tsm createElastistorTSM(String storagePoolName, String storageIp, Long capacityBytes, Long capacityIops, String domainName) {

        s_logger.info("Creation of elastistor TSM started.");

        Tsm tsm;
        String elastistorAccountId;
        try {
            // to create a tsm , account id is required, so getting the account id for the given cloudstack domain
            elastistorAccountId = ElastistorUtil.getElastistorAccountId(domainName);

            // create the tsm for the given account id
            tsm = ElastistorUtil.createElastistorTsm(storagePoolName, storageIp, capacityBytes, capacityIops, elastistorAccountId);
        } catch (Throwable e) {
            s_logger.error("Failed to create TSM in elastistor.", e);
            throw new CloudRuntimeException("Failed to create TSM in elastistor. " + e.getMessage());
        }

        s_logger.info("Creation of elastistor TSM completed successfully.");

        return tsm;
    }

    private PrimaryDataStoreParameters createElastistorVolume(PrimaryDataStoreParameters parameters, Tsm tsm, String storagePoolName, Long capacityBytes, Long capacityIops, String protocoltype,
            String mountpoint) {

        try {

            s_logger.info("Creation of elastistor volume started.");

            FileSystem volume = ElastistorUtil.createElastistorVolume(storagePoolName, tsm.getUuid(), capacityBytes, capacityIops, protocoltype, mountpoint);

            if (protocoltype.contentEquals("iscsi")) {
                String accesspath = "/" + volume.getIqn() + "/0";
                parameters.setPath(accesspath);
            }
            s_logger.info("Creation of elastistor volume completed successfully.");

            return parameters;
        } catch (Throwable e) {
            s_logger.error("Failed to create volume in elastistor.", e);
            throw new CloudRuntimeException("Failed to create volume in elastistor. " + e.getMessage());
        }

    }

    private String getAccessPath(String url) {
        StringTokenizer st = new StringTokenizer(url, "/");
        int count = 0;
        while (st.hasMoreElements()) {
            if (count == 2) {
                String s = "/";
                return s.concat(st.nextElement().toString());
            }
            st.nextElement();
            count++;
        }
        return null;
    }

    private StoragePoolType getStorageType(String url) {

        StringTokenizer st = new StringTokenizer(url, ":");

        while (st.hasMoreElements()) {
            String accessprotocol = st.nextElement().toString();

            if (accessprotocol.contentEquals("nfs")) {
                return StoragePoolType.NetworkFilesystem;
            } else if (accessprotocol.contentEquals("iscsi")) {
                return StoragePoolType.IscsiLUN;
            }

            else

                break;

        }
        return null;
    }

    private String getProtocolType(String url) {
        StringTokenizer st = new StringTokenizer(url, ":");

        while (st.hasMoreElements()) {
            String accessprotocol = st.nextElement().toString();

            if (accessprotocol.contentEquals("nfs")) {
                return "nfs";
            } else if (accessprotocol.contentEquals("iscsi")) {
                return "iscsi";
            } else
                break;
        }
        return null;
    }

    // this method parses the url and gets the default storage port based on
    // access protocol
    private int getDefaultStoragePort(String url) {

        StringTokenizer st = new StringTokenizer(url, ":");

        while (st.hasMoreElements()) {

            String accessprotocol = st.nextElement().toString();

            if (accessprotocol.contentEquals("nfs")) {
                return 2049;
            } else if (accessprotocol.contentEquals("iscsi")) {
                return 3260;
            } else
                break;

        }
        return -1;

    }

    // parses the url and returns the storage volume ip
    private String getStorageIp(String url) {

        StringTokenizer st = new StringTokenizer(url, "/");
        int count = 0;

        while (st.hasMoreElements()) {
            if (count == 1)
                return st.nextElement().toString();

            st.nextElement();
            count++;
        }
        return null;
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {

        dataStoreHelper.attachCluster(store);

        StoragePoolVO dataStoreVO = _storagePoolDao.findById(store.getId());

        PrimaryDataStoreInfo primarystore = (PrimaryDataStoreInfo) store;
        // Check if there is host up in this cluster
        List<HostVO> allHosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, primarystore.getClusterId(), primarystore.getPodId(), primarystore.getDataCenterId());
        if (allHosts.isEmpty()) {
            primaryDataStoreDao.expunge(primarystore.getId());
            throw new CloudRuntimeException("No host up to associate a storage pool with in cluster " + primarystore.getClusterId());
        }

        if (!dataStoreVO.isManaged()) {
            boolean success = false;
            for (HostVO h : allHosts) {
                success = createStoragePool(h.getId(), primarystore);
                if (success) {
                    break;
                }
            }
        }

        s_logger.debug("In createPool Adding the pool to each of the hosts");
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO h : allHosts) {
            try {
                storageMgr.connectHostToSharedPool(h.getId(), primarystore.getId());
                poolHosts.add(h);
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + h + " and " + primarystore, e);
            }

            if (poolHosts.isEmpty()) {
                s_logger.warn("No host can access storage pool " + primarystore + " on cluster " + primarystore.getClusterId());
                primaryDataStoreDao.expunge(primarystore.getId());
                throw new CloudRuntimeException("Failed to access storage pool");
            }
        }

        return true;
    }

    private boolean createStoragePool(long hostId, StoragePool pool) {
        s_logger.debug("creating pool " + pool.getName() + " on  host " + hostId);
        if (pool.getPoolType() != StoragePoolType.NetworkFilesystem && pool.getPoolType() != StoragePoolType.Filesystem && pool.getPoolType() != StoragePoolType.IscsiLUN
                && pool.getPoolType() != StoragePoolType.Iscsi && pool.getPoolType() != StoragePoolType.VMFS && pool.getPoolType() != StoragePoolType.SharedMountPoint
                && pool.getPoolType() != StoragePoolType.PreSetup && pool.getPoolType() != StoragePoolType.OCFS2 && pool.getPoolType() != StoragePoolType.RBD
                && pool.getPoolType() != StoragePoolType.CLVM) {
            s_logger.warn(" Doesn't support storage pool type " + pool.getPoolType());
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
                msg = "Can not create storage pool through host " + hostId + " due to " + answer.getDetails();
                s_logger.warn(msg);
            } else {
                msg = "Can not create storage pool through host " + hostId + " due to CreateStoragePoolCommand returns null";
                s_logger.warn(msg);
            }
            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        _dataStoreHelper.attachHost(store, scope, existingInfo);
        return true;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(hypervisorType, scope.getScopeId());
        s_logger.debug("In createPool. Attaching the pool to each of the hosts.");
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO host : hosts) {
            try {
                storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
                poolHosts.add(host);
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }
        if (poolHosts.isEmpty()) {
            s_logger.warn("No host can access storage pool " + dataStore + " in this zone.");
            primaryDataStoreDao.expunge(dataStore.getId());
            throw new CloudRuntimeException("Failed to create storage pool as it is not accessible to hosts.");
        }
        dataStoreHelper.attachZone(dataStore, hypervisorType);
        return true;
    }

    @Override
    public boolean maintain(DataStore store) {
        _storagePoolAutomation.maintain(store);
        _dataStoreHelper.maintain(store);
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        _dataStoreHelper.cancelMaintain(store);
        _storagePoolAutomation.cancelMaintain(store);
        return true;
    }

    @Override
    public void enableStoragePool(DataStore dataStore) {
        _dataStoreHelper.enable(dataStore);
    }

    @Override
    public void disableStoragePool(DataStore dataStore) {
        _dataStoreHelper.disable(dataStore);
    }

    @SuppressWarnings("finally")
    @Override
    public boolean deleteDataStore(DataStore store) {
        List<StoragePoolHostVO> hostPoolRecords = _storagePoolHostDao.listByPoolId(store.getId());
        StoragePool pool = (StoragePool) store;

        // find the hypervisor where the storage is attached to.
        HypervisorType hType = null;
        if (hostPoolRecords.size() > 0) {
            hType = getHypervisorType(hostPoolRecords.get(0).getHostId());
        }

        StoragePoolVO storagePoolVO = _storagePoolDao.findById(store.getId());

        if (!(storagePoolVO.isManaged())) {
            // Remove the SR associated with the Xenserver
            for (StoragePoolHostVO host : hostPoolRecords) {
                DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(pool);
                final Answer answer = agentMgr.easySend(host.getHostId(), deleteCmd);

                if (answer != null && answer.getResult()) {
                    // if host is KVM hypervisor then send deleteStoragepoolcmd
                    // to all the kvm hosts.
                    if (HypervisorType.KVM != hType) {
                        break;
                    }
                } else {
                    if (answer != null) {
                        s_logger.error("Failed to delete storage pool: " + answer.getResult());
                    }
                }
            }
        }
        // delete the Elastistor volume at backend
        deleteElastistorVolume(pool, storagePoolVO.isManaged());

        return _dataStoreHelper.deletePrimaryDataStore(store);
    }

    private void deleteElastistorVolume(StoragePool pool, boolean managed) {

        String poolid = pool.getUuid();
        boolean status;

        try {
            status = ElastistorUtil.deleteElastistorTsm(poolid, managed);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Failed to delete primary storage on elastistor" + e);
        }

        if (status == true) {
            s_logger.info("deletion of elastistor primary storage complete");
        } else {
            s_logger.error("deletion of elastistor volume failed");
        }

    }

    private HypervisorType getHypervisorType(long hostId) {
        HostVO host = _hostDao.findById(hostId);
        if (host != null)
            return host.getHypervisorType();
        return HypervisorType.None;
    }

    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {
        String capacityBytes = details.get(PrimaryDataStoreLifeCycle.CAPACITY_BYTES);
        String capacityIops = details.get(PrimaryDataStoreLifeCycle.CAPACITY_IOPS);

        StoragePoolVO storagePoolVO = _storagePoolDao.findById(storagePool.getId());

           try {
                if(capacityBytes != null){
                    long usedBytes = _capacityMgr.getUsedBytes(storagePoolVO);

                    if (Long.parseLong(capacityBytes) < usedBytes) {
                        throw new CloudRuntimeException("Cannot reduce the number of bytes for this storage pool as it would lead to an insufficient number of bytes");
                    }

                    UpdateTsmStorageCmdResponse updateTsmStorageCmdResponse  = ElastistorUtil.updateElastistorTsmStorage(capacityBytes,storagePool.getUuid());

                   if(updateTsmStorageCmdResponse.getStorage().getId() != null){
                    // update the cloudstack db
                    _storagePoolDao.updateCapacityBytes(storagePool.getId(), Long.parseLong(capacityBytes));

                    s_logger.info("elastistor TSM storage successfully updated");
                   }else{
                       throw new CloudRuntimeException("Failed to update the storage of Elastistor TSM" + updateTsmStorageCmdResponse.toString());
                   }
                }

                if(capacityIops != null){

                    long usedIops = _capacityMgr.getUsedIops(storagePoolVO);
                long capacity = Long.parseLong(capacityIops);

                if (capacity < usedIops) {
                        throw new CloudRuntimeException("Cannot reduce the number of IOPS for this storage pool as it would lead to an insufficient number of IOPS");
                    }

                    UpdateTsmCmdResponse updateTsmCmdResponse   = ElastistorUtil.updateElastistorTsmIOPS(capacityIops,storagePool.getUuid());

                   if(updateTsmCmdResponse.getTsm(0).getUuid() != null){
                   // update the cloudstack db
                    _storagePoolDao.updateCapacityIops(storagePool.getId(), capacity);

                    s_logger.info("elastistor TSM IOPS successfully updated");

                   }else{
                       throw new CloudRuntimeException("Failed to update the IOPS of Elastistor TSM" + updateTsmCmdResponse.toString());
                   }
                }

            } catch (Throwable e) {
                throw new CloudRuntimeException("Failed to update the storage pool" + e);
            }


    }

}
