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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
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
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

public class SolidFireSharedPrimaryDataStoreLifeCycle implements PrimaryDataStoreLifeCycle {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject private AccountDao accountDao;
    @Inject private AccountDetailsDao accountDetailsDao;
    @Inject private AgentManager agentMgr;
    @Inject private ClusterDao clusterDao;
    @Inject private DataCenterDao zoneDao;
    @Inject private HostDao hostDao;
    @Inject private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject private PrimaryDataStoreHelper primaryDataStoreHelper;
    @Inject private ResourceManager resourceMgr;
    @Inject private StorageManager storageMgr;
    @Inject private StoragePoolAutomation storagePoolAutomation;
    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject private StoragePoolHostDao storagePoolHostDao;
    @Inject private TemplateManager tmpltMgr;

    // invoked to add primary storage that is based on the SolidFire plug-in
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        final String CAPACITY_IOPS = "capacityIops";

        String url = (String)dsInfos.get("url");
        Long zoneId = (Long)dsInfos.get("zoneId");
        Long podId = (Long)dsInfos.get("podId");
        Long clusterId = (Long)dsInfos.get("clusterId");
        String storagePoolName = (String)dsInfos.get("name");
        String providerName = (String)dsInfos.get("providerName");
        Long capacityBytes = (Long)dsInfos.get("capacityBytes");
        Long capacityIops = (Long)dsInfos.get(CAPACITY_IOPS);
        String tags = (String)dsInfos.get("tags");
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");

        if (podId == null) {
            throw new CloudRuntimeException("The Pod ID must be specified.");
        }

        if (clusterId == null) {
            throw new CloudRuntimeException("The Cluster ID must be specified.");
        }

        String storageVip = SolidFireUtil.getStorageVip(url);
        int storagePort = SolidFireUtil.getStoragePort(url);

        if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops <= 0) {
            throw new IllegalArgumentException("'capacityIops' must be present and greater than 0.");
        }

        HypervisorType hypervisorType = getHypervisorTypeForCluster(clusterId);

        if (!isSupportedHypervisorType(hypervisorType)) {
            throw new CloudRuntimeException(hypervisorType + " is not a supported hypervisor type.");
        }

        String datacenter = SolidFireUtil.getValue(SolidFireUtil.DATACENTER, url, false);

        if (HypervisorType.VMware.equals(hypervisorType) && datacenter == null) {
            throw new CloudRuntimeException("'Datacenter' must be set for hypervisor type of " + HypervisorType.VMware);
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        parameters.setType(getStorageType(hypervisorType));
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setClusterId(clusterId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(false);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(hypervisorType);
        parameters.setTags(tags);
        parameters.setDetails(details);

        String managementVip = SolidFireUtil.getManagementVip(url);
        int managementPort = SolidFireUtil.getManagementPort(url);

        details.put(SolidFireUtil.MANAGEMENT_VIP, managementVip);
        details.put(SolidFireUtil.MANAGEMENT_PORT, String.valueOf(managementPort));

        String clusterAdminUsername = SolidFireUtil.getValue(SolidFireUtil.CLUSTER_ADMIN_USERNAME, url);
        String clusterAdminPassword = SolidFireUtil.getValue(SolidFireUtil.CLUSTER_ADMIN_PASSWORD, url);

        details.put(SolidFireUtil.CLUSTER_ADMIN_USERNAME, clusterAdminUsername);
        details.put(SolidFireUtil.CLUSTER_ADMIN_PASSWORD, clusterAdminPassword);

        if (capacityBytes < SolidFireUtil.MIN_VOLUME_SIZE) {
            capacityBytes = SolidFireUtil.MIN_VOLUME_SIZE;
        }

        long lMinIops = 100;
        long lMaxIops = 15000;
        long lBurstIops = 15000;

        try {
            String minIops = SolidFireUtil.getValue(SolidFireUtil.MIN_IOPS, url);

            if (minIops != null && minIops.trim().length() > 0) {
                lMinIops = Long.parseLong(minIops);
            }
        } catch (Exception ex) {
            logger.info("[ignored] error getting Min IOPS: " + ex.getLocalizedMessage());
        }

        try {
            String maxIops = SolidFireUtil.getValue(SolidFireUtil.MAX_IOPS, url);

            if (maxIops != null && maxIops.trim().length() > 0) {
                lMaxIops = Long.parseLong(maxIops);
            }
        } catch (Exception ex) {
            logger.info("[ignored] error getting Max IOPS: " + ex.getLocalizedMessage());
        }

        try {
            String burstIops = SolidFireUtil.getValue(SolidFireUtil.BURST_IOPS, url);

            if (burstIops != null && burstIops.trim().length() > 0) {
                lBurstIops = Long.parseLong(burstIops);
            }
        } catch (Exception ex) {
            logger.info("[ignored] error getting Burst IOPS: " + ex.getLocalizedMessage());
        }

        if (lMinIops > lMaxIops) {
            throw new CloudRuntimeException("The parameter '" + SolidFireUtil.MIN_IOPS + "' must be less than or equal to the parameter '" + SolidFireUtil.MAX_IOPS + "'.");
        }

        if (lMaxIops > lBurstIops) {
            throw new CloudRuntimeException("The parameter '" + SolidFireUtil.MAX_IOPS + "' must be less than or equal to the parameter '" + SolidFireUtil.BURST_IOPS + "'.");
        }

        if (lMinIops != capacityIops) {
            throw new CloudRuntimeException("The parameter '" + CAPACITY_IOPS + "' must be equal to the parameter '" + SolidFireUtil.MIN_IOPS + "'.");
        }

        if (lMinIops > SolidFireUtil.MAX_MIN_IOPS_PER_VOLUME) {
            throw new CloudRuntimeException("This volume's Min IOPS cannot exceed " + NumberFormat.getInstance().format(SolidFireUtil.MAX_MIN_IOPS_PER_VOLUME) + " IOPS.");
        }

        if (lMaxIops > SolidFireUtil.MAX_IOPS_PER_VOLUME) {
            throw new CloudRuntimeException("This volume's Max IOPS cannot exceed " + NumberFormat.getInstance().format(SolidFireUtil.MAX_IOPS_PER_VOLUME) + " IOPS.");
        }

        if (lBurstIops > SolidFireUtil.MAX_IOPS_PER_VOLUME) {
            throw new CloudRuntimeException("This volume's Burst IOPS cannot exceed " + NumberFormat.getInstance().format(SolidFireUtil.MAX_IOPS_PER_VOLUME) + " IOPS.");
        }

        details.put(SolidFireUtil.MIN_IOPS, String.valueOf(lMinIops));
        details.put(SolidFireUtil.MAX_IOPS, String.valueOf(lMaxIops));
        details.put(SolidFireUtil.BURST_IOPS, String.valueOf(lBurstIops));

        SolidFireUtil.SolidFireConnection sfConnection = new SolidFireUtil.SolidFireConnection(managementVip, managementPort, clusterAdminUsername, clusterAdminPassword);

        SolidFireCreateVolume sfCreateVolume = createSolidFireVolume(sfConnection, storagePoolName, capacityBytes, lMinIops, lMaxIops, lBurstIops);

        SolidFireUtil.SolidFireVolume sfVolume = sfCreateVolume.getVolume();

        String iqn = sfVolume.getIqn();

        details.put(SolidFireUtil.VOLUME_ID, String.valueOf(sfVolume.getId()));

        parameters.setUuid(iqn);

        if (HypervisorType.VMware.equals(hypervisorType)) {
            String datastore = iqn.replace("/", "_");
            String path = "/" + datacenter + "/" + datastore;

            parameters.setHost("VMFS datastore: " + path);
            parameters.setPort(0);
            parameters.setPath(path);

            details.put(SolidFireUtil.DATASTORE_NAME, datastore);
            details.put(SolidFireUtil.IQN, iqn);
            details.put(SolidFireUtil.STORAGE_VIP, storageVip);
            details.put(SolidFireUtil.STORAGE_PORT, String.valueOf(storagePort));
        }
        else {
            parameters.setHost(storageVip);
            parameters.setPort(storagePort);
            parameters.setPath(iqn);
        }

        ClusterVO cluster = clusterDao.findById(clusterId);

        GlobalLock lock = GlobalLock.getInternLock(cluster.getUuid());

        if (!lock.lock(SolidFireUtil.LOCK_TIME_IN_SECONDS)) {
            String errMsg = "Couldn't lock the DB on the following string: " + cluster.getUuid();

            logger.debug(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        DataStore dataStore = null;

        try {
            // this adds a row in the cloud.storage_pool table for this SolidFire volume
            dataStore = primaryDataStoreHelper.createPrimaryDataStore(parameters);

            // now that we have a DataStore (we need the id from the DataStore instance), we can create a Volume Access Group, if need be, and
            // place the newly created volume in the Volume Access Group
            List<HostVO> hosts = hostDao.findByClusterId(clusterId);

            String clusterUuId = clusterDao.findById(clusterId).getUuid();

            SolidFireUtil.placeVolumeInVolumeAccessGroups(sfConnection, sfVolume.getId(), hosts, clusterUuId);

            SolidFireUtil.SolidFireAccount sfAccount = sfCreateVolume.getAccount();
            Account csAccount = CallContext.current().getCallingAccount();

            SolidFireUtil.updateCsDbWithSolidFireAccountInfo(csAccount.getId(), sfAccount, dataStore.getId(), accountDetailsDao);
        } catch (Exception ex) {
            if (dataStore != null) {
                primaryDataStoreDao.expunge(dataStore.getId());
            }

            throw new CloudRuntimeException(ex.getMessage());
        }
        finally {
            lock.unlock();
            lock.releaseRef();
        }

        return dataStore;
    }

    private HypervisorType getHypervisorTypeForCluster(long clusterId) {
        ClusterVO cluster = clusterDao.findById(clusterId);

        if (cluster == null) {
            throw new CloudRuntimeException("Cluster ID '" + clusterId + "' was not found in the database.");
        }

        return cluster.getHypervisorType();
    }

    private StoragePoolType getStorageType(HypervisorType hypervisorType) {
        if (HypervisorType.XenServer.equals(hypervisorType)) {
            return StoragePoolType.IscsiLUN;
        }

        if (HypervisorType.VMware.equals(hypervisorType)) {
            return StoragePoolType.VMFS;
        }

        throw new CloudRuntimeException("The 'hypervisor' parameter must be '" + HypervisorType.XenServer + "' or '" + HypervisorType.VMware + "'.");
    }

    private class SolidFireCreateVolume {
        private final SolidFireUtil.SolidFireVolume _sfVolume;
        private final SolidFireUtil.SolidFireAccount _sfAccount;

        SolidFireCreateVolume(SolidFireUtil.SolidFireVolume sfVolume, SolidFireUtil.SolidFireAccount sfAccount) {
            _sfVolume = sfVolume;
            _sfAccount = sfAccount;
        }

        public SolidFireUtil.SolidFireVolume getVolume() {
            return _sfVolume;
        }

        public SolidFireUtil.SolidFireAccount getAccount() {
            return _sfAccount;
        }
    }

    private SolidFireCreateVolume createSolidFireVolume(SolidFireUtil.SolidFireConnection sfConnection,
            String volumeName, long volumeSize, long minIops, long maxIops, long burstIops) {
        try {
            Account csAccount = CallContext.current().getCallingAccount();
            long csAccountId = csAccount.getId();
            AccountVO accountVo = accountDao.findById(csAccountId);

            String sfAccountName = SolidFireUtil.getSolidFireAccountName(accountVo.getUuid(), csAccountId);

            SolidFireUtil.SolidFireAccount sfAccount = SolidFireUtil.getAccount(sfConnection, sfAccountName);

            if (sfAccount == null) {
                long sfAccountNumber = SolidFireUtil.createAccount(sfConnection, sfAccountName);

                sfAccount = SolidFireUtil.getAccountById(sfConnection, sfAccountNumber);
            }

            long sfVolumeId = SolidFireUtil.createVolume(sfConnection, SolidFireUtil.getSolidFireVolumeName(volumeName), sfAccount.getId(), volumeSize,
                    true, null, minIops, maxIops, burstIops);
            SolidFireUtil.SolidFireVolume sfVolume = SolidFireUtil.getVolume(sfConnection, sfVolumeId);

            return new SolidFireCreateVolume(sfVolume, sfAccount);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Failed to create a SolidFire volume: " + e.toString());
        }
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return true;
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        PrimaryDataStoreInfo primaryDataStoreInfo = (PrimaryDataStoreInfo)store;

        // check if there is at least one host up in this cluster
        List<HostVO> allHosts = resourceMgr.listAllUpHosts(Host.Type.Routing, primaryDataStoreInfo.getClusterId(),
                primaryDataStoreInfo.getPodId(), primaryDataStoreInfo.getDataCenterId());

        if (allHosts.isEmpty()) {
            primaryDataStoreDao.expunge(primaryDataStoreInfo.getId());

            throw new CloudRuntimeException("No host up to associate a storage pool with in cluster " + primaryDataStoreInfo.getClusterId());
        }

        boolean success = false;

        for (HostVO host : allHosts) {
            success = createStoragePool(host, primaryDataStoreInfo);

            if (success) {
                break;
            }
        }

        if (!success) {
            throw new CloudRuntimeException("Unable to create storage in cluster " + primaryDataStoreInfo.getClusterId());
        }

        List<HostVO> poolHosts = new ArrayList<>();

        for (HostVO host : allHosts) {
            try {
                storageMgr.connectHostToSharedPool(host.getId(), primaryDataStoreInfo.getId());

                poolHosts.add(host);
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + primaryDataStoreInfo, e);
            }
        }

        if (poolHosts.isEmpty()) {
            logger.warn("No host can access storage pool '" + primaryDataStoreInfo + "' on cluster '" + primaryDataStoreInfo.getClusterId() + "'.");

            primaryDataStoreDao.expunge(primaryDataStoreInfo.getId());

            throw new CloudRuntimeException("Failed to access storage pool");
        }

        primaryDataStoreHelper.attachCluster(store);

        return true;
    }

    private boolean createStoragePool(HostVO host, StoragePool storagePool) {
        long hostId = host.getId();
        HypervisorType hypervisorType = host.getHypervisorType();
        CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, storagePool);

        if (HypervisorType.VMware.equals(hypervisorType)) {
            cmd.setCreateDatastore(true);

            Map<String, String> details = new HashMap<>();

            StoragePoolDetailVO storagePoolDetail = storagePoolDetailsDao.findDetail(storagePool.getId(), SolidFireUtil.DATASTORE_NAME);

            details.put(CreateStoragePoolCommand.DATASTORE_NAME, storagePoolDetail.getValue());

            storagePoolDetail = storagePoolDetailsDao.findDetail(storagePool.getId(), SolidFireUtil.IQN);

            details.put(CreateStoragePoolCommand.IQN, storagePoolDetail.getValue());

            storagePoolDetail = storagePoolDetailsDao.findDetail(storagePool.getId(), SolidFireUtil.STORAGE_VIP);

            details.put(CreateStoragePoolCommand.STORAGE_HOST, storagePoolDetail.getValue());

            storagePoolDetail = storagePoolDetailsDao.findDetail(storagePool.getId(), SolidFireUtil.STORAGE_PORT);

            details.put(CreateStoragePoolCommand.STORAGE_PORT, storagePoolDetail.getValue());

            cmd.setDetails(details);
        }

        Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer != null && answer.getResult()) {
            return true;
        } else {
            primaryDataStoreDao.expunge(storagePool.getId());

            final String msg;

            if (answer != null) {
                msg = "Cannot create storage pool through host '" + hostId + "' due to the following: " + answer.getDetails();
            } else {
                msg = "Cannot create storage pool through host '" + hostId + "' due to CreateStoragePoolCommand returns null";
            }

            logger.warn(msg);

            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        return true;
    }

    @Override
    public boolean maintain(DataStore dataStore) {
        storagePoolAutomation.maintain(dataStore);
        primaryDataStoreHelper.maintain(dataStore);

        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        primaryDataStoreHelper.cancelMaintain(store);
        storagePoolAutomation.cancelMaintain(store);

        return true;
    }

    // invoked to delete primary storage that is based on the SolidFire plug-in
    @Override
    public boolean deleteDataStore(DataStore dataStore) {
        List<StoragePoolHostVO> hostPoolRecords = storagePoolHostDao.listByPoolId(dataStore.getId());

        HypervisorType hypervisorType = null;

        if (hostPoolRecords.size() > 0 ) {
            hypervisorType = getHypervisorType(hostPoolRecords.get(0).getHostId());
        }

        if (!isSupportedHypervisorType(hypervisorType)) {
            throw new CloudRuntimeException(hypervisorType + " is not a supported hypervisor type.");
        }

        StoragePool storagePool = (StoragePool)dataStore;
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(storagePool.getId());
        List<VMTemplateStoragePoolVO> unusedTemplatesInPool = tmpltMgr.getUnusedTemplatesInPool(storagePoolVO);

        for (VMTemplateStoragePoolVO templatePoolVO : unusedTemplatesInPool) {
            tmpltMgr.evictTemplateFromStoragePool(templatePoolVO);
        }

        Long clusterId = null;
        Long hostId = null;

        for (StoragePoolHostVO host : hostPoolRecords) {
            DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(storagePool);

            if (HypervisorType.VMware.equals(hypervisorType)) {
                deleteCmd.setRemoveDatastore(true);

                Map<String, String> details = new HashMap<>();

                StoragePoolDetailVO storagePoolDetail = storagePoolDetailsDao.findDetail(storagePool.getId(), SolidFireUtil.DATASTORE_NAME);

                details.put(DeleteStoragePoolCommand.DATASTORE_NAME, storagePoolDetail.getValue());

                storagePoolDetail = storagePoolDetailsDao.findDetail(storagePool.getId(), SolidFireUtil.IQN);

                details.put(DeleteStoragePoolCommand.IQN, storagePoolDetail.getValue());

                storagePoolDetail = storagePoolDetailsDao.findDetail(storagePool.getId(), SolidFireUtil.STORAGE_VIP);

                details.put(DeleteStoragePoolCommand.STORAGE_HOST, storagePoolDetail.getValue());

                storagePoolDetail = storagePoolDetailsDao.findDetail(storagePool.getId(), SolidFireUtil.STORAGE_PORT);

                details.put(DeleteStoragePoolCommand.STORAGE_PORT, storagePoolDetail.getValue());

                deleteCmd.setDetails(details);
            }

            final Answer answer = agentMgr.easySend(host.getHostId(), deleteCmd);

            if (answer != null && answer.getResult()) {
                logger.info("Successfully deleted storage pool using Host ID " + host.getHostId());

                HostVO hostVO = hostDao.findById(host.getHostId());

                if (hostVO != null) {
                    clusterId = hostVO.getClusterId();
                    hostId = hostVO.getId();
                }

                break;
            }
            else {
                if (answer != null) {
                    logger.error("Failed to delete storage pool using Host ID " + host.getHostId() + ": " + answer.getResult());
                }
                else {
                    logger.error("Failed to delete storage pool using Host ID " + host.getHostId());
                }
            }
        }

        if (clusterId != null) {
            ClusterVO cluster = clusterDao.findById(clusterId);

            GlobalLock lock = GlobalLock.getInternLock(cluster.getUuid());

            if (!lock.lock(SolidFireUtil.LOCK_TIME_IN_SECONDS)) {
                String errMsg = "Couldn't lock the DB on the following string: " + cluster.getUuid();

                logger.debug(errMsg);

                throw new CloudRuntimeException(errMsg);
            }

            try {
                long sfVolumeId = getVolumeId(storagePool.getId());

                SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePool.getId(), storagePoolDetailsDao);

                List<SolidFireUtil.SolidFireVag> sfVags = SolidFireUtil.getAllVags(sfConnection);

                for (SolidFireUtil.SolidFireVag sfVag : sfVags) {
                    if (SolidFireUtil.sfVagContains(sfVag, sfVolumeId, clusterId, hostDao)) {
                        SolidFireUtil.removeVolumeIdsFromSolidFireVag(sfConnection, sfVag.getId(), new Long[] { sfVolumeId });
                    }
                }
            }
            finally {
                lock.unlock();
                lock.releaseRef();
            }
        }

        if (hostId != null) {
            handleTargetsForVMware(hostId, storagePool.getId());
        }

        deleteSolidFireVolume(storagePool.getId());

        return primaryDataStoreHelper.deletePrimaryDataStore(dataStore);
    }

    private void handleTargetsForVMware(long hostId, long storagePoolId) {
        HostVO host = hostDao.findById(hostId);

        if (host.getHypervisorType() == HypervisorType.VMware) {
            String storageAddress = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.STORAGE_VIP).getValue();
            int storagePort = Integer.parseInt(storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.STORAGE_PORT).getValue());
            String iqn = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.IQN).getValue();

            ModifyTargetsCommand cmd = new ModifyTargetsCommand();

            List<Map<String, String>> targets = new ArrayList<>();

            Map<String, String> target = new HashMap<>();

            target.put(ModifyTargetsCommand.STORAGE_HOST, storageAddress);
            target.put(ModifyTargetsCommand.STORAGE_PORT, String.valueOf(storagePort));
            target.put(ModifyTargetsCommand.IQN, iqn);

            targets.add(target);

            cmd.setTargets(targets);
            cmd.setApplyToAllHostsInCluster(true);
            cmd.setAdd(false);
            cmd.setTargetTypeToRemove(ModifyTargetsCommand.TargetTypeToRemove.DYNAMIC);
            cmd.setRemoveAsync(true);

            sendModifyTargetsCommand(cmd, hostId);
        }
    }

    private void sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            String msg = "Unable to get an answer to the modify targets command";

            logger.warn(msg);
        }
        else if (!answer.getResult()) {
            String msg = "Unable to modify target on the following host: " + hostId;

            logger.warn(msg);
        }
    }

    private void deleteSolidFireVolume(long storagePoolId) {
        SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePoolId, storagePoolDetailsDao);

        long sfVolumeId = getVolumeId(storagePoolId);

        SolidFireUtil.deleteVolume(sfConnection, sfVolumeId);
    }

    private long getVolumeId(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.VOLUME_ID);

        String volumeId = storagePoolDetail.getValue();

        return Long.parseLong(volumeId);
    }

    private long getIopsValue(long storagePoolId, String iopsKey) {
        StoragePoolDetailVO storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, iopsKey);

        String iops = storagePoolDetail.getValue();

        return Long.parseLong(iops);
    }

    private static boolean isSupportedHypervisorType(HypervisorType hypervisorType) {
        return HypervisorType.XenServer.equals(hypervisorType) || HypervisorType.VMware.equals(hypervisorType);
    }

    private HypervisorType getHypervisorType(long hostId) {
        HostVO host = hostDao.findById(hostId);

        if (host != null) {
            return host.getHypervisorType();
        }

        return HypervisorType.None;
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
        String strCapacityBytes = details.get(PrimaryDataStoreLifeCycle.CAPACITY_BYTES);
        String strCapacityIops = details.get(PrimaryDataStoreLifeCycle.CAPACITY_IOPS);

        Long capacityBytes = strCapacityBytes != null ? Long.parseLong(strCapacityBytes) : null;
        Long capacityIops = strCapacityIops != null ? Long.parseLong(strCapacityIops) : null;

        SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePool.getId(), storagePoolDetailsDao);

        long size = capacityBytes != null ? capacityBytes : storagePool.getCapacityBytes();

        long currentMinIops = getIopsValue(storagePool.getId(), SolidFireUtil.MIN_IOPS);
        long currentMaxIops = getIopsValue(storagePool.getId(), SolidFireUtil.MAX_IOPS);
        long currentBurstIops = getIopsValue(storagePool.getId(), SolidFireUtil.BURST_IOPS);

        long minIops = currentMinIops;
        long maxIops = currentMaxIops;
        long burstIops = currentBurstIops;

        if (capacityIops != null) {
            if (capacityIops > SolidFireUtil.MAX_MIN_IOPS_PER_VOLUME) {
                throw new CloudRuntimeException("This volume cannot exceed " + NumberFormat.getInstance().format(SolidFireUtil.MAX_MIN_IOPS_PER_VOLUME) + " IOPS.");
            }

            float maxPercentOfMin = currentMaxIops / (float)currentMinIops;
            float burstPercentOfMax = currentBurstIops / (float)currentMaxIops;

            minIops = capacityIops;
            maxIops = (long)(minIops * maxPercentOfMin);
            burstIops = (long)(maxIops * burstPercentOfMax);

            if (maxIops > SolidFireUtil.MAX_IOPS_PER_VOLUME) {
                maxIops = SolidFireUtil.MAX_IOPS_PER_VOLUME;
            }

            if (burstIops > SolidFireUtil.MAX_IOPS_PER_VOLUME) {
                burstIops = SolidFireUtil.MAX_IOPS_PER_VOLUME;
            }
        }

        SolidFireUtil.modifyVolume(sfConnection, getVolumeId(storagePool.getId()), size, null, minIops, maxIops, burstIops);

        SolidFireUtil.updateCsDbWithSolidFireIopsInfo(storagePool.getId(), primaryDataStoreDao, storagePoolDetailsDao, minIops, maxIops, burstIops);
    }

    @Override
    public void enableStoragePool(DataStore dataStore) {
        primaryDataStoreHelper.enable(dataStore);
    }

    @Override
    public void disableStoragePool(DataStore dataStore) {
        primaryDataStoreHelper.disable(dataStore);
    }
}
