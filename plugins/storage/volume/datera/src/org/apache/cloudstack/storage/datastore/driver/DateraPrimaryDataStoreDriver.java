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

package org.apache.cloudstack.storage.datastore.driver;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.DateraObject;
import org.apache.cloudstack.storage.datastore.util.DateraUtil;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DateraPrimaryDataStoreDriver implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger.getLogger(DateraPrimaryDataStoreDriver.class);
    private static final int s_lockTimeInSeconds = 300;
    private static final int s_lowestHypervisorSnapshotReserve = 10;

    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private ClusterDetailsDao _clusterDetailsDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private VMTemplatePoolDao tmpltPoolDao;
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private VolumeDetailsDao volumeDetailsDao;
    @Inject
    private SnapshotDetailsDao snapshotDetailsDao;
    @Inject
    private VolumeDataFactory volumeDataFactory;

    /**
     * Returns a map which lists the capabilities that this storage device can
     * offer. Currently supported STORAGE_SYSTEM_SNAPSHOT: Has the ability to create
     * native snapshots CAN_CREATE_VOLUME_FROM_SNAPSHOT: Can create new volumes from
     * native snapshots. CAN_CREATE_VOLUME_FROM_VOLUME: Device can clone volumes.
     * This is used for template caching.
     * @return a Map<String,String> which determines the capabilities of the driver
     */
    @Override
    public Map<String, String> getCapabilities() {
        Map<String, String> mapCapabilities = new HashMap<>();

        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString(), Boolean.TRUE.toString());

        return mapCapabilities;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject) {
        // We don't support auth yet
        return null;
    }

    /**
     * Fetches an App Instance from Datera, throws exception if it doesn't find it
     * @param conn            Datera Connection
     * @param appInstanceName Name of the Aplication Instance
     * @return application instance
     */
    public DateraObject.AppInstance getDateraAppInstance(DateraObject.DateraConnection conn, String appInstanceName) {

        DateraObject.AppInstance appInstance = null;
        try {
            appInstance = DateraUtil.getAppInstance(conn, appInstanceName);
        } catch (DateraObject.DateraError dateraError) {
            s_logger.warn("Error getting appInstance " + appInstanceName, dateraError);
            throw new CloudRuntimeException(dateraError.getMessage());
        }

        if (appInstance == null) {
            throw new CloudRuntimeException("App instance not found " + appInstanceName);
        }

        return appInstance;
    }

    /**
     * Given a {@code dataObject} this function makes sure that the {@code host} has
     * access to it. All hosts which are in the same cluster are added to an
     * initiator group and that group is assigned to the appInstance. If an
     * initiator group does not exist, it is created. If the host does not have an
     * initiator registered on dataera, that is created and added to the initiator
     * group
     * @param dataObject The volume that needs to be accessed
     * @param host       The host which needs to access the volume
     * @param dataStore  Identifies which primary storage the volume resides in
     * @return True if access is granted. False otherwise
     */
    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {

        s_logger.debug("grantAccess() called");

        Preconditions.checkArgument(dataObject != null, "'dataObject' should not be 'null'");
        Preconditions.checkArgument(host != null, "'host' should not be 'null'");
        Preconditions.checkArgument(dataStore != null, "'dataStore' should not be 'null'");

        long storagePoolId = dataStore.getId();

        DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

        String appInstanceName = getAppInstanceName(dataObject);
        DateraObject.AppInstance appInstance = getDateraAppInstance(conn, appInstanceName);

        Preconditions.checkArgument(appInstance != null);

        long clusterId = host.getClusterId();

        ClusterVO cluster = _clusterDao.findById(clusterId);

        GlobalLock lock = GlobalLock.getInternLock(cluster.getUuid());

        if (!lock.lock(s_lockTimeInSeconds)) {
            s_logger.debug("Couldn't lock the DB (in grantAccess) on the following string: " + cluster.getUuid());
        }

        try {

            DateraObject.InitiatorGroup initiatorGroup = null;
            String initiatorGroupKey = DateraUtil.getInitiatorGroupKey(storagePoolId);

            List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

            if (!DateraUtil.hostsSupport_iScsi(hosts)) {
                s_logger.debug("hostsSupport_iScsi() :Host does NOT support iscsci");
                return false;
            }

            // We don't have the initiator group, create one
            String initiatorGroupName = DateraUtil.INITIATOR_GROUP_PREFIX + "-" + cluster.getUuid();
            s_logger.debug("Will use initiator group " + String.valueOf(initiatorGroupName));

            initiatorGroup = DateraUtil.getInitiatorGroup(conn, initiatorGroupName);

            if (initiatorGroup == null) {
                s_logger.debug("create initiator group " + String.valueOf(initiatorGroupName));
                initiatorGroup = DateraUtil.createInitiatorGroup(conn, initiatorGroupName);
                // Save it to the DB
                ClusterDetailsVO clusterDetail = new ClusterDetailsVO(clusterId, initiatorGroupKey, initiatorGroupName);
                _clusterDetailsDao.persist(clusterDetail);

            } else {
                initiatorGroup = DateraUtil.getInitiatorGroup(conn, initiatorGroupName);
            }

            Preconditions.checkNotNull(initiatorGroup, "initiatorGroup should not be Null");

            // We create an initiator for every host in this cluster and add it to the
            // initator group
            addClusterHostsToInitiatorGroup(conn, clusterId, initiatorGroupName);

            // assgin the initiatorgroup to appInstance

            if (!isInitiatorGroupAssignedToAppInstance(conn, initiatorGroup, appInstance)) {
                DateraUtil.assignGroupToAppInstance(conn, initiatorGroupName, appInstanceName);
                int retries = DateraUtil.DEFAULT_RETRIES;
                while (!isInitiatorGroupAssignedToAppInstance(conn, initiatorGroup, appInstance) && retries > 0) {
                    Thread.sleep(DateraUtil.POLL_TIMEOUT_MS);
                    retries--;
                }

                Preconditions.checkArgument(isInitiatorGroupAssignedToAppInstance(conn, initiatorGroup, appInstance),
                        "Initgroup is not assigned to appinstance");
                // FIXME: Sleep anyways
                s_logger.debug("sleep " + String.valueOf(DateraUtil.POLL_TIMEOUT_MS) + " msec for ACL to be applied");

                Thread.sleep(DateraUtil.POLL_TIMEOUT_MS); // ms
                s_logger.debug(
                        "Initiator group " + String.valueOf(initiatorGroupName) + " is assigned to " + appInstanceName);

            }

            return true;
        } catch (DateraObject.DateraError | UnsupportedEncodingException | InterruptedException dateraError) {
            s_logger.warn(dateraError.getMessage(), dateraError);
            throw new CloudRuntimeException("Unable to grant access to volume " + dateraError.getMessage());
        } finally {
            lock.unlock();
            lock.releaseRef();
        }
    }

    private void addClusterHostsToInitiatorGroup(DateraObject.DateraConnection conn, long clusterId,
            String initiatorGroupName) throws DateraObject.DateraError, UnsupportedEncodingException {

        List<HostVO> clusterHosts = _hostDao.findByClusterId(clusterId);
        DateraObject.InitiatorGroup initiatorGroup = DateraUtil.getInitiatorGroup(conn, initiatorGroupName);

        for (HostVO host : clusterHosts) {

            // check if we have an initiator for the host
            String iqn = host.getStorageUrl();

            DateraObject.Initiator initiator = DateraUtil.getInitiator(conn, iqn);
            String initiatorName = "";
            // initiator can not be found, create it
            if (initiator == null) {

                initiatorName = DateraUtil.INITIATOR_PREFIX + "-" + host.getUuid();
                initiator = DateraUtil.createInitiator(conn, initiatorName, iqn);
                s_logger.debug("Initiator " + initiatorName + " with " + iqn + "added ");

            }
            Preconditions.checkNotNull(initiator);

            if (!DateraUtil.isInitiatorPresentInGroup(initiator, initiatorGroup)) {
                s_logger.debug("Add " + initiatorName + " to " + initiatorGroupName);
                DateraUtil.addInitiatorToGroup(conn, initiator.getPath(), initiatorGroupName);
            }
        }
    }

    /**
     * Checks if an initiator group is assigned to an appInstance
     * @param conn           Datera connection
     * @param initiatorGroup Initiator group to check
     * @param appInstance    App Instance
     * @return True if initiator group is assigned to app instnace, false otherwise
     * @throws DateraObject.DateraError
     */

    private boolean isInitiatorGroupAssignedToAppInstance(DateraObject.DateraConnection conn,
            DateraObject.InitiatorGroup initiatorGroup, DateraObject.AppInstance appInstance)
            throws DateraObject.DateraError {

        Map<String, DateraObject.InitiatorGroup> assignedInitiatorGroups = DateraUtil
                .getAppInstanceInitiatorGroups(conn, appInstance.getName());

        Preconditions.checkNotNull(assignedInitiatorGroups);

        for (DateraObject.InitiatorGroup ig : assignedInitiatorGroups.values()) {
            if (initiatorGroup.getName().equals(ig.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes access of the initiator group to which {@code host} belongs from the
     * appInstance given by {@code dataObject}
     * @param dataObject Datera volume
     * @param host       the host which is currently having access to the volume
     * @param dataStore  The primary store to which volume belongs
     */
    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
        s_logger.debug("revokeAccess() called");

        Preconditions.checkArgument(dataObject != null, "'dataObject' should not be 'null'");
        Preconditions.checkArgument(host != null, "'host' should not be 'null'");
        Preconditions.checkArgument(dataStore != null, "'dataStore' should not be 'null'");

        String appInstanceName = getAppInstanceName(dataObject);
        long clusterId = host.getClusterId();
        long storagePoolId = dataStore.getId();

        ClusterVO cluster = _clusterDao.findById(clusterId);

        GlobalLock lock = GlobalLock.getInternLock(cluster.getUuid());

        if (!lock.lock(s_lockTimeInSeconds)) {
            s_logger.debug("Couldn't lock the DB (in revokeAccess) on the following string: " + cluster.getUuid());
        }

        try {

            String initiatorGroupName = DateraUtil.INITIATOR_GROUP_PREFIX + "-" + cluster.getUuid();

            DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

            DateraObject.AppInstance appInstance = DateraUtil.getAppInstance(conn, appInstanceName);
            DateraObject.InitiatorGroup initiatorGroup = DateraUtil.getInitiatorGroup(conn, initiatorGroupName);

            if (initiatorGroup != null && appInstance != null) {

                DateraUtil.removeGroupFromAppInstance(conn, initiatorGroupName, appInstanceName);
                int retries = DateraUtil.DEFAULT_RETRIES;
                while (isInitiatorGroupAssignedToAppInstance(conn, initiatorGroup, appInstance) && retries > 0) {
                    Thread.sleep(DateraUtil.POLL_TIMEOUT_MS);
                    retries--;
                }
            }

        } catch (DateraObject.DateraError | UnsupportedEncodingException | InterruptedException dateraError) {
            String errMesg = "Error revoking access for Volume : " + dataObject.getId();
            s_logger.warn(errMesg, dateraError);
            throw new CloudRuntimeException(errMesg);
        } finally {
            lock.unlock();
            lock.releaseRef();
        }
    }

    /**
     * Returns the size of template on this primary storage. If we already have a
     * template on this storage, we return 0
     * @param templateInfo Information about the template
     * @param storagePool  The pool where we want to store the template
     * @return Size in bytes
     */
    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {

        List<VMTemplateStoragePoolVO> lstTemplatePoolRefs = tmpltPoolDao.listByPoolId(storagePool.getId());

        if (lstTemplatePoolRefs != null) {
            for (VMTemplateStoragePoolVO templatePoolRef : lstTemplatePoolRefs) {
                if (templatePoolRef.getTemplateId() == templateInfo.getId()) {
                    // This indicates that we already have this template stored on this primary
                    // storage, so
                    // we do not require additional space.
                    return 0;
                }
            }
        }

        // This indicates that we do not have a copy of this template on this primary
        // storage, so
        // we need to take it into consideration from a space standpoint (ex. when a new
        // VM is spun
        // up and wants to use this particular template for its root disk).
        return getDataObjectSizeIncludingHypervisorSnapshotReserve(templateInfo, storagePool);
    }

    /**
     * Returns Datera appInstanceName
     * @param dataObject volume or template
     * @return Derived Datera appInstanceName based on dataObject, Eg.
     *         CS-V-ROOT-123-6db58e3f-14c4-45ac-95e9-60e3a00ce7d0
     */
    private String getAppInstanceName(DataObject dataObject) {

        ArrayList<String> name = new ArrayList<>();

        name.add(DateraUtil.APPINSTANCE_PREFIX); // CS

        String dataObjectTypeString = dataObject.getType().name(); // TEMPLATE, VOLUME, SNAPSHOT
        String dataObjectTypeBrief;
        dataObjectTypeBrief = org.apache.commons.lang.StringUtils.substring(dataObjectTypeString, 0, 1);
        name.add(dataObjectTypeBrief); // T, V

        switch (dataObject.getType()) {
        case TEMPLATE:
            TemplateInfo templateInfo = (TemplateInfo) dataObject;

            name.add(dataObject.getUuid()); // 6db58e3f-14c4-45ac-95e9-60e3a00ce7d0

            // For cached templates, we will also add the storage pool ID
            name.add(String.valueOf(dataObject.getDataStore().getId()));
            break;

        case VOLUME:
            VolumeInfo volumeInfo = (VolumeInfo) dataObject;
            String volumeName = volumeInfo.getName();
            name.add(String.valueOf(volumeName));
            name.add(dataObject.getUuid()); // 6db58e3f-14c4-45ac-95e9-60e3a00ce7d0

            VolumeVO volumeVo = _volumeDao.findById(dataObject.getId());
            s_logger.debug("volumeName : " + volumeName);
            break;

        case SNAPSHOT:
            name.add(dataObject.getUuid()); // 6db58e3f-14c4-45ac-95e9-60e3a00ce7d0

        }

        String appInstanceName = StringUtils.join("-", name.toArray());
        return org.apache.commons.lang.StringUtils.substring(appInstanceName, 0, DateraUtil.APPINSTANCE_MAX_LENTH);
    }

    // Not being used right now as Datera doesn't support min IOPS
    private long getDefaultMinIops(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId,
                DateraUtil.CLUSTER_DEFAULT_MIN_IOPS);

        String clusterDefaultMinIops = storagePoolDetail.getValue();

        return Long.parseLong(clusterDefaultMinIops);
    }

    /**
     * If user doesn't specify the IOPS, use this IOPS
     * @param storagePoolId the primary storage
     * @return default max IOPS for this storage configured when the storage is
     *         added
     */
    private long getDefaultMaxIops(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId,
                DateraUtil.CLUSTER_DEFAULT_MAX_IOPS);

        String clusterDefaultMaxIops = storagePoolDetail.getValue();

        return Long.parseLong(clusterDefaultMaxIops);
    }

    /**
     * Return the default number of replicas to use (configured at storage addition
     * time)
     * @param storagePoolId the primary storage
     * @return the number of replicas to use
     */
    private int getNumReplicas(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId,
                DateraUtil.NUM_REPLICAS);

        String clusterDefaultReplicas = storagePoolDetail.getValue();

        return Integer.parseInt(clusterDefaultReplicas);

    }

    /**
     * Return the default volume placement to use (configured at storage addition
     * time)
     * @param storagePoolId the primary storage
     * @return volume placement string
     */
    private String getVolPlacement(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId,
                DateraUtil.VOL_PLACEMENT);

        String clusterDefaultVolPlacement = storagePoolDetail.getValue();

        return clusterDefaultVolPlacement;

    }

    /**
     * Return the default IP pool name to use (configured at storage addition time)
     * @param storagePoolId the primary storage
     * @return IP pool name
     */
    private String getIpPool(long storagePoolId) {
        String ipPool = DateraUtil.DEFAULT_IP_POOL;
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, DateraUtil.IP_POOL);
        if (storagePoolDetail != null) {
            ipPool = storagePoolDetail.getValue();
        }
        s_logger.debug("ipPool: " + ipPool);
        return ipPool;

    }

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        return getUsedBytes(storagePool, Long.MIN_VALUE);
    }

    /**
     * Get the total space used by all the entities on the storage.
     * Total space = volume space + snapshot space + template space
     * @param storagePool      Primary storage
     * @param volumeIdToIgnore Ignore this volume (used when we delete a volume and
     *                         want to update the space)
     * @return size in bytes
     */
    private long getUsedBytes(StoragePool storagePool, long volumeIdToIgnore) {
        long usedSpaceBytes = 0;

        List<VolumeVO> lstVolumes = _volumeDao.findByPoolId(storagePool.getId(), null);

        if (lstVolumes != null) {
            for (VolumeVO volume : lstVolumes) {
                if (volume.getId() == volumeIdToIgnore) {
                    continue;
                }

                VolumeDetailVO volumeDetail = volumeDetailsDao.findDetail(volume.getId(), DateraUtil.VOLUME_SIZE);

                if (volumeDetail != null && volumeDetail.getValue() != null) {
                    long volumeSizeGib = Long.parseLong(volumeDetail.getValue());
                    long volumeSizeBytes = DateraUtil.gibToBytes((int) (volumeSizeGib));
                    usedSpaceBytes += volumeSizeBytes;
                } else {
                    DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePool.getId(),
                            _storagePoolDetailsDao);
                    try {

                        String appInstanceName = getAppInstanceName(volumeDataFactory.getVolume(volume.getId()));
                        DateraObject.AppInstance appInstance = DateraUtil.getAppInstance(conn, appInstanceName);
                        if (appInstance != null) {
                            usedSpaceBytes += DateraUtil.gibToBytes(appInstance.getSize());
                        }
                    } catch (DateraObject.DateraError dateraError) {
                        String errMesg = "Error getting used bytes for storage pool : " + storagePool.getId();
                        s_logger.warn(errMesg, dateraError);
                        throw new CloudRuntimeException(errMesg);
                    }
                }
            }
        }

        List<SnapshotVO> lstSnapshots = _snapshotDao.listAll();

        if (lstSnapshots != null) {
            for (SnapshotVO snapshot : lstSnapshots) {
                SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(snapshot.getId(),
                        DateraUtil.STORAGE_POOL_ID);

                // if this snapshot belongs to the storagePool that was passed in
                if (snapshotDetails != null && snapshotDetails.getValue() != null
                        && Long.parseLong(snapshotDetails.getValue()) == storagePool.getId()) {
                    snapshotDetails = _snapshotDetailsDao.findDetail(snapshot.getId(), DateraUtil.VOLUME_SIZE);

                    if (snapshotDetails != null && snapshotDetails.getValue() != null) {
                        long snapshotSize = Long.parseLong(snapshotDetails.getValue());

                        usedSpaceBytes += snapshotSize;
                    }
                }
            }
        }

        List<VMTemplateStoragePoolVO> lstTemplatePoolRefs = tmpltPoolDao.listByPoolId(storagePool.getId());

        if (lstTemplatePoolRefs != null) {
            for (VMTemplateStoragePoolVO templatePoolRef : lstTemplatePoolRefs) {
                usedSpaceBytes += templatePoolRef.getTemplateSize();
            }
        }
        s_logger.debug("usedSpaceBytes: " + String.valueOf(usedSpaceBytes));

        return usedSpaceBytes;
    }

    /**
     * Get total IOPS used by the storage array. Since Datera doesn't support min
     * IOPS, return zero for now
     * @param storagePool primary storage
     * @return total IOPS used
     */
    @Override
    public long getUsedIops(StoragePool storagePool) {
        long usedIops = 0;
        return usedIops;
    }

    /**
     * Rreturns the size of the volume including the hypervisor snapshot reserve
     * (HSR).
     * @param dataObject Volume or a Template
     * @param pool       primary storage where it resides
     * @return size in bytes
     */

    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool pool) {

        long volumeSize = 0;

        switch (dataObject.getType()) {
        case VOLUME:

            VolumeInfo volume = (VolumeInfo) dataObject;
            volumeSize = volume.getSize();
            Integer hypervisorSnapshotReserve = volume.getHypervisorSnapshotReserve();

            if (hypervisorSnapshotReserve != null) {
                hypervisorSnapshotReserve = Math.max(hypervisorSnapshotReserve, s_lowestHypervisorSnapshotReserve);
                volumeSize += volumeSize * (hypervisorSnapshotReserve / 100f);
            }
            s_logger.debug("Volume size:" + String.valueOf(volumeSize));
            break;

        case TEMPLATE:

            TemplateInfo templateInfo = (TemplateInfo) dataObject;
            long templateSize = templateInfo.getSize() != null ? templateInfo.getSize() : 0;

            if (templateInfo.getHypervisorType() == Hypervisor.HypervisorType.KVM) {
                volumeSize = templateSize;
            } else {
                volumeSize = (long) (templateSize + templateSize * (s_lowestHypervisorSnapshotReserve / 100f));
            }
            s_logger.debug("Template volume size:" + String.valueOf(volumeSize));

            break;
        }
        return volumeSize;
    }

    /**
     * Deletes a volume from Datera. If we are using native snapshots, we first
     * check if the volume is holding a native snapshot, if it does, then we don't
     * delete it from Datera but instead mark it so that when the snapshot is
     * deleted, we delete the volume
     *
     * @param volumeInfo    The volume which needs to be deleted
     * @param storagePoolId Primary storage where volume resides
     */
    private void deleteVolume(VolumeInfo volumeInfo, long storagePoolId) {

        DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);
        Long volumeStoragePoolId = volumeInfo.getPoolId();
        long volumeId = volumeInfo.getId();

        if (volumeStoragePoolId == null) {
            return; // this volume was never assigned to a storage pool, so no SAN volume should
                    // exist for it
        }

        try {

            // If there are native snapshots on this appInstance, we want to keep it on
            // Datera
            // but remove it from cloudstack
            if (shouldDeleteVolume(volumeId, null)) {
                DateraUtil.deleteAppInstance(conn, getAppInstanceName(volumeInfo));
            }

            volumeDetailsDao.removeDetails(volumeId);

            StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);

            long usedBytes = getUsedBytes(storagePool, volumeId);
            storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);
            storagePoolDao.update(storagePoolId, storagePool);

        } catch (UnsupportedEncodingException | DateraObject.DateraError e) {
            String errMesg = "Error deleting app instance for Volume : " + volumeInfo.getId();
            s_logger.warn(errMesg, e);
            throw new CloudRuntimeException(errMesg);
        }
    }

    /**
     * given a {@code volumeInfo} and {@code storagePoolId}, creates an App instance
     * on Datera. Updates the usedBytes count in the DB for this storage pool. A
     * volume could be created in 3 ways
     *
     * 1) A fresh volume with no data: New volume created from Cloudstack
     *
     * 2) A volume created from a native snapshot. This is used when creating volume
     * from snapshot and native snapshots are supported
     *
     * 3) A volume created by cloning from another volume: This is used when
     * creating volume from template or volume from snapshot stored as another
     * volume when native snapshots are not supported by the hypervisor
     *
     *
     * @param volumeInfo    Info about the volume like size,QoS
     * @param storagePoolId The pool to create the vo
     * @return returns the IQN path which will be used by storage substem
     *
     */

    private String createVolume(VolumeInfo volumeInfo, long storagePoolId) {
        s_logger.debug("createVolume() called");

        Preconditions.checkArgument(volumeInfo != null, "volumeInfo cannot be null");
        Preconditions.checkArgument(storagePoolId > 0, "storagePoolId should be > 0");

        verifySufficientBytesForStoragePool(volumeInfo, storagePoolId);

        DateraObject.AppInstance appInstance;

        DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

        long csSnapshotId = getCsIdForCloning(volumeInfo.getId(), "cloneOfSnapshot");
        long csTemplateId = getCsIdForCloning(volumeInfo.getId(), "cloneOfTemplate");
        s_logger.debug("csTemplateId is " + String.valueOf(csTemplateId));

        try {

            if (csSnapshotId > 0) {
                // creating volume from snapshot. The snapshot could either be a native snapshot
                // or another volume.
                s_logger.debug("Creating volume from snapshot ");
                appInstance = createDateraClone(conn, csSnapshotId, volumeInfo, storagePoolId, DataObjectType.SNAPSHOT);

            } else if (csTemplateId > 0) {

                // create volume from template. Invoked when creating new ROOT volume
                s_logger.debug("Creating volume from template ");

                appInstance = createDateraClone(conn, csTemplateId, volumeInfo, storagePoolId, DataObjectType.TEMPLATE);
                String appInstanceName = appInstance.getName();

                long volumeSize = getDataObjectSizeIncludingHypervisorSnapshotReserve(volumeInfo,
                        storagePoolDao.findById(storagePoolId));

                // expand the template
                if (volumeSize > DateraUtil.gibToBytes(appInstance.getSize())) {

                    // Expand the volume to include HSR depending on the volume's service offering
                    DateraUtil.updateAppInstanceSize(conn, appInstanceName, DateraUtil.bytesToGib(volumeSize));

                    // refresh appInstance
                    appInstance = DateraUtil.getAppInstance(conn, appInstanceName);

                    Preconditions.checkNotNull(appInstance);
                    // update IOPS
                    if ((volumeInfo.getMaxIops() != null) && (volumeInfo.getMaxIops() != appInstance.getTotalIops())) {
                        int newIops = Ints.checkedCast(volumeInfo.getMaxIops());
                        DateraUtil.updateAppInstanceIops(conn, appInstanceName, newIops);
                    }
                    // refresh appInstance
                    appInstance = DateraUtil.getAppInstance(conn, appInstanceName);
                }

            } else {
                // Just create a standard volume
                s_logger.debug("Creating a standard volume ");
                appInstance = createDateraVolume(conn, volumeInfo, storagePoolId);
            }
        } catch (UnsupportedEncodingException | DateraObject.DateraError e) {
            String errMesg = "Unable to create Volume Error: " + e.getMessage();
            s_logger.warn(errMesg);
            throw new CloudRuntimeException(errMesg, e);
        }

        if (appInstance == null) {
            String errMesg = "appInstance returned null";
            s_logger.warn(errMesg);
            throw new CloudRuntimeException(errMesg);
        }

        Preconditions.checkNotNull(appInstance);
        String iqn = appInstance.getIqn();
        String iqnPath = DateraUtil.generateIqnPath(iqn);

        VolumeVO volumeVo = _volumeDao.findById(volumeInfo.getId());
        s_logger.debug("volume ID : " + volumeInfo.getId());
        s_logger.debug("volume uuid : " + volumeInfo.getUuid());

        volumeVo.set_iScsiName(iqnPath);
        volumeVo.setFolder(appInstance.getName());
        volumeVo.setPoolType(Storage.StoragePoolType.IscsiLUN);
        volumeVo.setPoolId(storagePoolId);

        _volumeDao.update(volumeVo.getId(), volumeVo);

        updateVolumeDetails(volumeVo.getId(), appInstance.getSize());

        StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

        long capacityBytes = storagePool.getCapacityBytes();
        long usedBytes = getUsedBytes(storagePool);

        storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);

        _storagePoolDao.update(storagePoolId, storagePool);

        return appInstance.getIqn();
    }

    /**
     * Helper function to create a Datera app instance. Throws an exception if
     * unsuccessful
     * @param conn          Datera connection
     * @param volumeInfo    Volume information
     * @param storagePoolId primary storage
     * @return The AppInstance which is created
     * @throws UnsupportedEncodingException
     * @throws                              DateraObject.DateraError
     */
    private DateraObject.AppInstance createDateraVolume(DateraObject.DateraConnection conn, VolumeInfo volumeInfo,
            long storagePoolId) throws UnsupportedEncodingException, DateraObject.DateraError {

        s_logger.debug("createDateraVolume() called");
        DateraObject.AppInstance appInstance = null;
        try {

            int minIops = Ints.checkedCast(
                    volumeInfo.getMinIops() != null ? volumeInfo.getMinIops() : getDefaultMinIops(storagePoolId));

            // int minIops = Ints.checkedCast(volumeInfo.getMinIops());

            int maxIops = Ints.checkedCast(
                    volumeInfo.getMaxIops() != null ? volumeInfo.getMaxIops() : getDefaultMaxIops(storagePoolId));

            // int maxIops = Ints.checkedCast(volumeInfo.getMaxIops());

            if (maxIops <= 0) { // We don't care about min iops for now
                maxIops = Ints.checkedCast(getDefaultMaxIops(storagePoolId));
            }

            int replicas = getNumReplicas(storagePoolId);
            String volumePlacement = getVolPlacement(storagePoolId);
            String ipPool = getIpPool(storagePoolId);

            long volumeSizeBytes = getDataObjectSizeIncludingHypervisorSnapshotReserve(volumeInfo,
                    _storagePoolDao.findById(storagePoolId));
            int volumeSizeGib = DateraUtil.bytesToGib(volumeSizeBytes);
            if (volumePlacement == null) {
                appInstance = DateraUtil.createAppInstance(conn, getAppInstanceName(volumeInfo), volumeSizeGib, maxIops,
                        replicas);
            } else {
                appInstance = DateraUtil.createAppInstance(conn, getAppInstanceName(volumeInfo), volumeSizeGib, maxIops,
                        replicas, volumePlacement, ipPool);
            }
        } catch (Exception ex) {
            s_logger.debug("createDateraVolume() failed");
            s_logger.error(ex);
        }
        return appInstance;
    }

    /**
     * This function creates a new AppInstance on datera by cloning. We can clone
     * either from a volume snapshot (in case of native snapshots) or clone from
     * another app Instance in case of templates or snapshots as volumes
     *
     * @param conn          Datera Connection
     * @param dataObjectId  The ID of the clone, used to fetch details on how to
     *                      clone
     * @param volumeInfo    Information about the clone
     * @param storagePoolId Primary store to create the clone on
     * @param dataType      Type of the source (snapshot or template)
     * @return The cloned AppInstance
     */
    private DateraObject.AppInstance createDateraClone(DateraObject.DateraConnection conn, long dataObjectId,
            VolumeInfo volumeInfo, long storagePoolId, DataObjectType dataType)
            throws UnsupportedEncodingException, DateraObject.DateraError {

        s_logger.debug("createDateraClone() called");

        String clonedAppInstanceName = getAppInstanceName(volumeInfo);
        String baseAppInstanceName = null;
        DateraObject.AppInstance appInstance = null;
        String ipPool = getIpPool(storagePoolId);

        if (dataType == DataObjectType.SNAPSHOT) {
            SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(dataObjectId, DateraUtil.SNAPSHOT_ID);

            // Clone volume from a snapshot
            if (snapshotDetails != null && snapshotDetails.getValue() != null) {
                s_logger.debug("Clone volume from a snapshot");

                appInstance = DateraUtil.cloneAppInstanceFromSnapshot(conn, clonedAppInstanceName,
                        snapshotDetails.getValue(), ipPool);

                if (volumeInfo.getMaxIops() != null) {

                    int totalIops = Math.min(DateraUtil.MAX_IOPS, Ints.checkedCast(volumeInfo.getMaxIops()));
                    DateraUtil.updateAppInstanceIops(conn, clonedAppInstanceName, totalIops);
                    appInstance = DateraUtil.getAppInstance(conn, clonedAppInstanceName);
                }

                if (appInstance == null) {
                    throw new CloudRuntimeException("Unable to create an app instance from snapshot "
                            + volumeInfo.getId() + " type " + dataType);
                }
                return appInstance;

            } else {

                // Clone volume from an appInstance
                s_logger.debug("Clone volume from an appInstance");

                snapshotDetails = snapshotDetailsDao.findDetail(dataObjectId, DateraUtil.VOLUME_ID);
                baseAppInstanceName = snapshotDetails.getValue();

            }
        } else if (dataType == DataObjectType.TEMPLATE) {
            s_logger.debug("Clone volume from a template");

            VMTemplateStoragePoolVO templatePoolRef = tmpltPoolDao.findByPoolTemplate(storagePoolId, dataObjectId);

            if (templatePoolRef != null) {
                baseAppInstanceName = templatePoolRef.getLocalDownloadPath();
            }
        }

        if (baseAppInstanceName == null) {
            throw new CloudRuntimeException(
                    "Unable to find a base volume to clone " + volumeInfo.getId() + " type " + dataType);
        }

        // Clone the app Instance
        appInstance = DateraUtil.cloneAppInstanceFromVolume(conn, clonedAppInstanceName, baseAppInstanceName, ipPool);

        if (dataType == DataObjectType.TEMPLATE) {
            // Only update volume parameters if clone from cached template
            // Update maxIops
            if (volumeInfo.getMaxIops() != null) {

                int totalIops = Math.min(DateraUtil.MAX_IOPS, Ints.checkedCast(volumeInfo.getMaxIops()));

                DateraUtil.updateAppInstanceIops(conn, clonedAppInstanceName, totalIops);
                appInstance = DateraUtil.getAppInstance(conn, clonedAppInstanceName);
            }
            // Update placementMode
            String newPlacementMode = getVolPlacement(storagePoolId);
            if (newPlacementMode != null) {
                DateraUtil.updateAppInstancePlacement(conn, clonedAppInstanceName, newPlacementMode);
            }
            appInstance = DateraUtil.getAppInstance(conn, clonedAppInstanceName);
        }
        if (appInstance == null) {
            throw new CloudRuntimeException("Unable to create an app instance from snapshot or template "
                    + volumeInfo.getId() + " type " + dataType);
        }
        s_logger.debug("Datera - Cloned " + baseAppInstanceName + " to " + clonedAppInstanceName);

        return appInstance;
    }

    /**
     * This function gets invoked when you want to do operations on a snapshot. The
     * snapshot could be a native snapshot and you want to create a template out of
     * it. Since snapshots don't have an IQN, we create a temp volume for this
     * snapshot which will be used to carry out further operations. This function
     * also handles deletion of temp volumes. A flag in the snapshot details table
     * decides which action is performed.
     *
     * @param snapshotInfo  snapshot on Datera
     * @param storagePoolId primary store ID
     */
    private void createTempVolume(SnapshotInfo snapshotInfo, long storagePoolId) {
        s_logger.debug("createTempVolume() from snapshot called");
        String ipPool = getIpPool(storagePoolId);
        long csSnapshotId = snapshotInfo.getId();

        SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DateraUtil.SNAPSHOT_ID);

        if (snapshotDetails == null || snapshotDetails.getValue() == null) {
            throw new CloudRuntimeException("'createTempVolume(SnapshotInfo, long)' should not be invoked unless "
                    + DateraUtil.SNAPSHOT_ID + " exists.");
        }

        DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

        snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, "tempVolume");

        if (snapshotDetails != null && snapshotDetails.getValue() != null
                && snapshotDetails.getValue().equalsIgnoreCase("create")) {

            snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DateraUtil.SNAPSHOT_ID);
            String snapshotName = snapshotDetails.getValue();

            String clonedAppInstanceName = getAppInstanceName(snapshotInfo);
            DateraObject.AppInstance clonedAppInstance;

            try {
                clonedAppInstance = DateraUtil.cloneAppInstanceFromSnapshot(conn, clonedAppInstanceName, snapshotName,
                        ipPool);
                DateraUtil.pollAppInstanceAvailable(conn, clonedAppInstanceName);
            } catch (DateraObject.DateraError | UnsupportedEncodingException e) {
                String errMesg = "Unable to create temp volume " + csSnapshotId + "Error:" + e.getMessage();
                s_logger.error(errMesg, e);
                throw new CloudRuntimeException(errMesg, e);
            }

            if (clonedAppInstance == null) {
                throw new CloudRuntimeException("Unable to clone volume for snapshot " + snapshotName);
            }
            s_logger.debug("Temp app_instance " + clonedAppInstanceName + " created");
            addTempVolumeToDb(csSnapshotId, clonedAppInstanceName);
            handleSnapshotDetails(csSnapshotId, DiskTO.IQN, DateraUtil.generateIqnPath(clonedAppInstance.getIqn()));

        } else if (snapshotDetails != null && snapshotDetails.getValue() != null
                && snapshotDetails.getValue().equalsIgnoreCase("delete")) {

            snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DateraUtil.VOLUME_ID);
            try {
                s_logger.debug("Deleting temp app_instance " + snapshotDetails.getValue());
                DateraUtil.deleteAppInstance(conn, snapshotDetails.getValue());
            } catch (UnsupportedEncodingException | DateraObject.DateraError dateraError) {
                String errMesg = "Error deleting temp volume " + dateraError.getMessage();
                throw new CloudRuntimeException(errMesg, dateraError);
            }

            removeTempVolumeFromDb(csSnapshotId);

            snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DiskTO.IQN);
            snapshotDetailsDao.remove(snapshotDetails.getId());
        } else {
            throw new CloudRuntimeException("Invalid state in 'createTempVolume(SnapshotInfo, long)'");
        }
    }

    /**
     * This function gets invoked when we want to create a volume that caches the
     * template on the primary storage. This 'template volume' will then be cloned
     * to create new ROOT volumes.
     *
     * @param templateInfo  Information about the template like id, size
     * @param storagePoolId the primary store to create this volume on
     * @return IQN of the template volume
     */
    public String createTemplateVolume(TemplateInfo templateInfo, long storagePoolId) {
        s_logger.debug("createTemplateVolume() as cache template called");

        verifySufficientBytesForStoragePool(templateInfo, storagePoolId);

        DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

        String iqn = null;
        String appInstanceName = null;
        try {

            long templateSizeBytes = getDataObjectSizeIncludingHypervisorSnapshotReserve(templateInfo,
                    storagePoolDao.findById(storagePoolId));

            s_logger.debug("cached VM template sizeBytes: " + String.valueOf(templateSizeBytes));

            int templateSizeGib = DateraUtil.bytesToGib(templateSizeBytes);

            int templateIops = DateraUtil.MAX_IOPS;
            int replicaCount = getNumReplicas(storagePoolId);
            appInstanceName = getAppInstanceName(templateInfo);
            String volumePlacement = getVolPlacement(storagePoolId);
            String ipPool = getIpPool(storagePoolId);

            s_logger.debug("cached VM template app_instance: " + appInstanceName + " ipPool: " + ipPool + " sizeGib: " + String.valueOf(templateSizeGib));
            DateraObject.AppInstance appInstance = DateraUtil.createAppInstance(conn, appInstanceName, templateSizeGib,
                    templateIops, replicaCount, volumePlacement, ipPool);

            if (appInstance == null) {
                throw new CloudRuntimeException("Unable to create Template volume " + templateInfo.getId());
            }

            iqn = appInstance.getIqn();

            VMTemplateStoragePoolVO templatePoolRef = tmpltPoolDao.findByPoolTemplate(storagePoolId,
                    templateInfo.getId());

            templatePoolRef.setInstallPath(DateraUtil.generateIqnPath(iqn));
            templatePoolRef.setLocalDownloadPath(appInstance.getName());
            templatePoolRef.setTemplateSize(DateraUtil.gibToBytes(appInstance.getSize()));

            tmpltPoolDao.update(templatePoolRef.getId(), templatePoolRef);

            StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);

            long capacityBytes = storagePool.getCapacityBytes();

            long usedBytes = getUsedBytes(storagePool);

            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);

            storagePoolDao.update(storagePoolId, storagePool);

        } catch (UnsupportedEncodingException | DateraObject.DateraError dateraError) {
            if (DateraObject.DateraErrorTypes.ConflictError.equals(dateraError)) {
                String errMesg = "template app Instance " + appInstanceName + " exists";
                s_logger.debug(errMesg, dateraError);
            } else {
                String errMesg = "Unable to create template app Instance " + dateraError.getMessage();
                s_logger.error(errMesg, dateraError);
                throw new CloudRuntimeException(errMesg, dateraError);
            }
        }
        return DateraUtil.generateIqnPath(iqn);
    }

    /**
     * Entry point into the create logic. The storage subsystem call this method to
     * create various data objects (volume/snapshot/template)
     *
     * @param dataStore
     * @param dataObject
     * @param callback
     */
    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errMsg = null;

        try {
            if (dataObject.getType() == DataObjectType.VOLUME) {
                s_logger.debug("createAsync - creating volume");
                iqn = createVolume((VolumeInfo) dataObject, dataStore.getId());
            } else if (dataObject.getType() == DataObjectType.SNAPSHOT) {
                s_logger.debug("createAsync - creating snapshot");
                createTempVolume((SnapshotInfo) dataObject, dataStore.getId());
            } else if (dataObject.getType() == DataObjectType.TEMPLATE) {
                s_logger.debug("createAsync - creating template");
                iqn = createTemplateVolume((TemplateInfo) dataObject, dataStore.getId());
            } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
                s_logger.error(errMsg);
            }
        } catch (Exception ex) {
            errMsg = ex.getMessage();

            s_logger.error(errMsg);

            if (callback == null) {
                throw ex;
            }
        }

        if (callback != null) {

            CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    /**
     * Helper function which updates volume size in the volume_details table
     * @param volumeId   Volume information
     * @param volumeSize Size in GB
     */
    private void updateVolumeDetails(long volumeId, long volumeSize) {
        VolumeDetailVO volumeDetailVo = volumeDetailsDao.findDetail(volumeId, DateraUtil.VOLUME_SIZE);

        if (volumeDetailVo == null || volumeDetailVo.getValue() == null) {
            volumeDetailVo = new VolumeDetailVO(volumeId, DateraUtil.VOLUME_SIZE, String.valueOf(volumeSize), false);

            volumeDetailsDao.persist(volumeDetailVo);
        }
    }

    /**
     * Entrypoint for delete operations.
     *
     * @param dataStore  Primary storage
     * @param dataObject object to delete
     * @param callback   used for async, complete the callback after the operation
     *                   is done.
     */
    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject,
            AsyncCompletionCallback<CommandResult> callback) {
        String errMsg = null;

        try {
            if (dataObject.getType() == DataObjectType.VOLUME) {
                s_logger.debug("deleteAsync - deleting volume");
                deleteVolume((VolumeInfo) dataObject, dataStore.getId());
            } else if (dataObject.getType() == DataObjectType.SNAPSHOT) {
                s_logger.debug("deleteAsync - deleting snapshot");
                deleteSnapshot((SnapshotInfo) dataObject, dataStore.getId());
            } else if (dataObject.getType() == DataObjectType.TEMPLATE) {
                s_logger.debug("deleteAsync - deleting template");
                deleteTemplate((TemplateInfo) dataObject, dataStore.getId());
            } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to deleteAsync";
            }
        } catch (Exception ex) {
            errMsg = ex.getMessage();

            s_logger.error(errMsg);
        }

        CommandResult result = new CommandResult();

        result.setResult(errMsg);

        callback.complete(result);

    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    /**
     * Entry point for taking a snapshot. A native snpashot is taken if the
     * hypervisor supports it, otherwise a volume is created and the data is copied
     * via the hypervisor and Cloudstack will treat this volume as a snapshot.
     *
     * @param snapshotInfo Snapshot information
     * @param callback     Async context
     */
    @Override
    public void takeSnapshot(SnapshotInfo snapshotInfo, AsyncCompletionCallback<CreateCmdResult> callback) {
        s_logger.debug("takeSnapshot() called");

        CreateCmdResult result;

        try {

            VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();
            VolumeVO volumeVO = _volumeDao.findById(volumeInfo.getId());

            long storagePoolId = volumeVO.getPoolId();

            DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

            String baseAppInstanceName = getAppInstanceName(volumeInfo);

            DateraObject.AppInstance baseAppInstance = DateraUtil.getAppInstance(conn, baseAppInstanceName);

            Preconditions.checkNotNull(baseAppInstance);

            SnapshotObjectTO snapshotObjectTo = (SnapshotObjectTO) snapshotInfo.getTO();

            if (shouldTakeSnapshot(snapshotInfo.getId())) {

                DateraObject.VolumeSnapshot volumeSnapshot = DateraUtil.takeVolumeSnapshot(conn, baseAppInstanceName);
                if (volumeSnapshot == null) {
                    s_logger.error("Unable to take native snapshot appInstance name:" + baseAppInstanceName
                            + " volume ID " + volumeInfo.getId());
                    throw new CloudRuntimeException("Unable to take native snapshot for volume " + volumeInfo.getId());
                }

                String snapshotName = baseAppInstanceName + ":" + volumeSnapshot.getTimestamp();
                updateSnapshotDetails(snapshotInfo.getId(), baseAppInstanceName, snapshotName, storagePoolId,
                        baseAppInstance.getSize());

                snapshotObjectTo.setPath("DateraSnapshotId=" + snapshotName);
                s_logger.info(" snapshot taken: " + snapshotName);

            } else {

                StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

                long capacityBytes = storagePool.getCapacityBytes();
                long usedBytes = getUsedBytes(storagePool);
                int volumeSizeGib = baseAppInstance.getSize();
                long volumeSizeBytes = DateraUtil.gibToBytes(volumeSizeGib);
                String volumePlacement = getVolPlacement(storagePoolId);
                String ipPool = getIpPool(storagePoolId);

                usedBytes += volumeSizeBytes;

                if (usedBytes > capacityBytes) {
                    throw new CloudRuntimeException(
                            "Insufficient amount of space remains in this primary storage to create a snapshot volume");
                }

                String appInstanceName = getAppInstanceName(snapshotInfo);
                DateraObject.AppInstance snapshotAppInstance = DateraUtil.createAppInstance(conn, appInstanceName,
                        volumeSizeGib, DateraUtil.MAX_IOPS, getNumReplicas(storagePoolId), volumePlacement, ipPool);

                snapshotObjectTo.setPath(snapshotAppInstance.getName());
                String iqnPath = DateraUtil.generateIqnPath(snapshotAppInstance.getIqn());
                updateSnapshotDetails(snapshotInfo.getId(), snapshotAppInstance.getName(), storagePoolId,
                        snapshotAppInstance.getSize(), iqnPath);

                snapshotObjectTo.setPath("DateraVolumeId=" + snapshotAppInstance.getName());

                storagePool.setUsedBytes(usedBytes);
                // update size in storage pool
                _storagePoolDao.update(storagePoolId, storagePool);
            }

            CreateObjectAnswer createObjectAnswer = new CreateObjectAnswer(snapshotObjectTo);

            result = new CreateCmdResult(null, createObjectAnswer);

            result.setResult(null);
        } catch (Exception ex) {
            s_logger.debug("Failed to take CloudStack snapshot: " + snapshotInfo.getId(), ex);

            result = new CreateCmdResult(null, new CreateObjectAnswer(ex.toString()));

            result.setResult(ex.toString());
        }

        callback.complete(result);
    }

    /**
     * If a native snapshot is used, this function updates the snapshot_detauls
     * table with the correct attributes
     *
     * @param csSnapshotId  Cloudstack snapshot ID
     * @param volumeId      Base volume ID
     * @param newSnapshotId SnapshotID on Datera (appInstanceName:Timestamp)
     * @param storagePoolId Primary storage
     * @param newVolumeSize VolumeSize in GB
     */
    private void updateSnapshotDetails(long csSnapshotId, String volumeId, String newSnapshotId, long storagePoolId,
            long newVolumeSize) {
        SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(csSnapshotId, DateraUtil.VOLUME_ID,
                String.valueOf(volumeId), false);

        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, DateraUtil.SNAPSHOT_ID, String.valueOf(newSnapshotId),
                false);

        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, DateraUtil.STORAGE_POOL_ID, String.valueOf(storagePoolId),
                false);

        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, DateraUtil.VOLUME_SIZE, String.valueOf(newVolumeSize),
                false);

        snapshotDetailsDao.persist(snapshotDetail);
    }

    /**
     * If a snapshot is represented as a volume, this function updates the
     * snapshot_details table with the right attributes so that Cloudstack knows
     * that this snapshot is a volume on the backend
     *
     * @param csSnapshotId            Snapshot ID on Cloudstack
     * @param snapshotAppInstanceName snapshot name on Datera
     *                                <appInstanceName>:<Timestamp>
     * @param storagePoolId           primary storage
     * @param snapshotSizeGb          snapshotSize
     * @param snapshotIqn             IQN of snapshot
     */
    private void updateSnapshotDetails(long csSnapshotId, String snapshotAppInstanceName, long storagePoolId,
            long snapshotSizeGb, String snapshotIqn) {
        SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(csSnapshotId, DateraUtil.VOLUME_ID,
                String.valueOf(snapshotAppInstanceName), false);

        _snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, DateraUtil.STORAGE_POOL_ID, String.valueOf(storagePoolId),
                false);

        _snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, DateraUtil.VOLUME_SIZE, String.valueOf(snapshotSizeGb),
                false);

        _snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, DiskTO.IQN, snapshotIqn, false);

        _snapshotDetailsDao.persist(snapshotDetail);
    }

    /**
     * Deletes snapshot on Datera
     * @param snapshotInfo  snapshot information
     * @param storagePoolId primary storage
     * @throws UnsupportedEncodingException
     * @throws                              DateraObject.DateraError
     */
    private void deleteSnapshot(SnapshotInfo snapshotInfo, long storagePoolId)
            throws UnsupportedEncodingException, DateraObject.DateraError {

        long csSnapshotId = snapshotInfo.getId();

        try {
            DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

            SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DateraUtil.SNAPSHOT_ID);

            if (snapshotDetails != null && snapshotDetails.getValue() != null) {
                // Native snapshot being used, delete that

                String snapshotName = snapshotDetails.getValue();

                DateraUtil.deleteVolumeSnapshot(conn, snapshotName);

                // check if the underlying volume needs to be deleted
                SnapshotVO snapshot = _snapshotDao.findById(csSnapshotId);
                VolumeVO volume = _volumeDao.findById(snapshot.getVolumeId());

                if (volume == null) {

                    // deleted from Cloudstack. Check if other snapshots are using this volume
                    volume = _volumeDao.findByIdIncludingRemoved(snapshot.getVolumeId());

                    if (shouldDeleteVolume(snapshot.getVolumeId(), snapshot.getId())) {
                        DateraUtil.deleteAppInstance(conn, volume.getFolder());
                    }
                }
            } else {

                // An App Instance is being used to support the CloudStack volume snapshot.

                snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DateraUtil.VOLUME_ID);
                String appInstanceName = snapshotDetails.getValue();

                DateraUtil.deleteAppInstance(conn, appInstanceName);
            }

            snapshotDetailsDao.removeDetails(csSnapshotId);

            StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);

            // getUsedBytes(StoragePool) will not include the snapshot to delete because it
            // has already been deleted by this point
            long usedBytes = getUsedBytes(storagePool);

            storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);

            storagePoolDao.update(storagePoolId, storagePool);
        } catch (Exception ex) {
            s_logger.debug("Error in 'deleteSnapshot(SnapshotInfo, long)'. CloudStack snapshot ID: " + csSnapshotId,
                    ex);
            throw ex;
        }
    }

    /**
     * Deletes a template from Datera
     * @param templateInfo  Information about Template
     * @param storagePoolId Primary storage
     * @throws UnsupportedEncodingException
     * @throws                              DateraObject.DateraError
     */
    private void deleteTemplate(TemplateInfo templateInfo, long storagePoolId)
            throws UnsupportedEncodingException, DateraObject.DateraError {
        try {
            DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

            String appInstanceName = getAppInstanceName(templateInfo);

            DateraUtil.deleteAppInstance(conn, appInstanceName);

            VMTemplateStoragePoolVO templatePoolRef = tmpltPoolDao.findByPoolTemplate(storagePoolId,
                    templateInfo.getId());

            tmpltPoolDao.remove(templatePoolRef.getId());

            StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);

            // getUsedBytes(StoragePool) will not include the template to delete because the
            // "template_spool_ref" table has already been updated by this point
            long usedBytes = getUsedBytes(storagePool);

            storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);

            storagePoolDao.update(storagePoolId, storagePool);
        } catch (Exception ex) {
            s_logger.debug("Failed to delete template volume. CloudStack template ID: " + templateInfo.getId(), ex);

            throw ex;
        }
    }

    /**
     * Revert snapshot for a volume
     * @param snapshotInfo           Information about volume snapshot
     * @param snapshotOnPrimaryStore Not used
     * @throws CloudRuntimeException
     */
    @Override
    public void revertSnapshot(SnapshotInfo snapshotInfo, SnapshotInfo snapshotOnPrimaryStore,
            AsyncCompletionCallback<CommandResult> callback) {

        VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();
        VolumeVO volumeVO = _volumeDao.findById(volumeInfo.getId());

        long storagePoolId = volumeVO.getPoolId();
        long csSnapshotId = snapshotInfo.getId();
        s_logger.info("Datera - restoreVolumeSnapshot from snapshotId " + String.valueOf(csSnapshotId) + " to volume"
                + volumeVO.getName());

        DateraObject.AppInstance appInstance;

        try {

            if (volumeVO == null || volumeVO.getRemoved() != null) {
                String errMsg = "The volume that the snapshot belongs to no longer exists.";

                CommandResult commandResult = new CommandResult();

                commandResult.setResult(errMsg);

                callback.complete(commandResult);

                return;
            }

            DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

            SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DateraUtil.SNAPSHOT_ID);

            if (snapshotDetails != null && snapshotDetails.getValue() != null) {
                // Native snapshot being used, restore snapshot from Datera AppInstance

                String snapshotName = snapshotDetails.getValue();

                s_logger.info("Datera - restoreVolumeSnapshot: " + snapshotName);

                appInstance = DateraUtil.restoreVolumeSnapshot(conn, snapshotName);

                Preconditions.checkNotNull(appInstance);

                updateVolumeDetails(volumeInfo.getId(), appInstance.getSize());
            }

            CommandResult commandResult = new CommandResult();

            callback.complete(commandResult);

        } catch (Exception ex) {
            s_logger.debug("Error in 'revertSnapshot()'. CloudStack snapshot ID: " + csSnapshotId, ex);
            throw new CloudRuntimeException(ex.getMessage());
        }

    }

    /**
     * Resizes a volume on Datera, shrinking is not allowed. Resize also takes into
     * account the HSR
     * @param dataObject volume to resize
     * @param callback   async context
     */
    @Override
    public void resize(DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo) dataObject;
            String iqnPath = volumeInfo.get_iScsiName();
            iqn = DateraUtil.extractIqn(iqnPath);

            long storagePoolId = volumeInfo.getPoolId();
            ResizeVolumePayload payload = (ResizeVolumePayload) volumeInfo.getpayload();
            String appInstanceName = getAppInstanceName(volumeInfo);
            long newSizeBytes = payload.newSize;

            Integer hsr = volumeInfo.getHypervisorSnapshotReserve();

            if (payload.newSize != null || payload.newHypervisorSnapshotReserve != null) {
                if (payload.newHypervisorSnapshotReserve != null) {
                    if (hsr != null) {
                        if (payload.newHypervisorSnapshotReserve > hsr) {
                            hsr = payload.newHypervisorSnapshotReserve;
                        }
                    } else {
                        hsr = payload.newHypervisorSnapshotReserve;
                    }
                }

                newSizeBytes = getVolumeSizeIncludingHypervisorSnapshotReserve(payload.newSize, hsr);
            }

            int newSize = DateraUtil.bytesToGib(newSizeBytes);

            DateraObject.DateraConnection conn = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

            try {

                DateraObject.AppInstance appInstance = DateraUtil.getAppInstance(conn, appInstanceName);

                Preconditions.checkNotNull(appInstance);

                if (appInstance.getSize() < newSize) {
                    DateraUtil.updateAppInstanceSize(conn, appInstanceName, Ints.checkedCast(newSize));
                }

                if (payload.newMaxIops != null && appInstance.getTotalIops() != payload.newMaxIops) {
                    DateraUtil.updateAppInstanceIops(conn, appInstanceName, Ints.checkedCast(payload.newMaxIops));
                }

                appInstance = DateraUtil.getAppInstance(conn, appInstanceName);

                Preconditions.checkNotNull(appInstance);

                VolumeVO volume = _volumeDao.findById(volumeInfo.getId());

                volume.setMinIops(payload.newMinIops);
                volume.setMaxIops(payload.newMaxIops);

                _volumeDao.update(volume.getId(), volume);

                updateVolumeDetails(volume.getId(), appInstance.getSize());

                Preconditions.checkNotNull(appInstance);

            } catch (DateraObject.DateraError | UnsupportedEncodingException dateraError) {
                dateraError.printStackTrace();
            }

        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to resize";
        }

        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

        result.setResult(errMsg);

        callback.complete(result);
    }

    /**
     * Adding temp volume to the snapshot_details table. This is used if we are
     * using a native snapshot and we want to create a template out of the snapshot
     *
     * @param csSnapshotId   Source snasphot
     * @param tempVolumeName temp volume app instance on Datera
     */
    private void addTempVolumeToDb(long csSnapshotId, String tempVolumeName) {
        SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DateraUtil.VOLUME_ID);

        if (snapshotDetails == null || snapshotDetails.getValue() == null) {
            throw new CloudRuntimeException(
                    "'addTempVolumeId' should not be invoked unless " + DateraUtil.VOLUME_ID + " exists.");
        }

        String originalVolumeId = snapshotDetails.getValue();

        handleSnapshotDetails(csSnapshotId, DateraUtil.TEMP_VOLUME_ID, originalVolumeId);
        handleSnapshotDetails(csSnapshotId, DateraUtil.VOLUME_ID, tempVolumeName);
    }

    private void removeTempVolumeFromDb(long csSnapshotId) {
        SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(csSnapshotId, DateraUtil.TEMP_VOLUME_ID);

        if (snapshotDetails == null || snapshotDetails.getValue() == null) {
            throw new CloudRuntimeException(
                    "'removeTempVolumeId' should not be invoked unless " + DateraUtil.TEMP_VOLUME_ID + " exists.");
        }

        String originalVolumeId = snapshotDetails.getValue();

        handleSnapshotDetails(csSnapshotId, DateraUtil.VOLUME_ID, originalVolumeId);

        snapshotDetailsDao.remove(snapshotDetails.getId());
    }

    /**
     * Helper function to update snapshot_details table
     *
     * @param csSnapshotId Snapshot
     * @param name         attribute name
     * @param value        attribute value
     */
    private void handleSnapshotDetails(long csSnapshotId, String name, String value) {
        snapshotDetailsDao.removeDetail(csSnapshotId, name);
        SnapshotDetailsVO snapshotDetails = new SnapshotDetailsVO(csSnapshotId, name, value, false);
        snapshotDetailsDao.persist(snapshotDetails);
    }

    private void verifySufficientBytesForStoragePool(DataObject dataObject, long storagePoolId) {
        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);

        long requestedBytes = getDataObjectSizeIncludingHypervisorSnapshotReserve(dataObject, storagePool);

        verifySufficientBytesForStoragePool(requestedBytes, storagePoolId);
    }

    private void verifySufficientBytesForStoragePool(long requestedBytes, long storagePoolId) {
        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);

        long capacityBytes = storagePool.getCapacityBytes();
        long usedBytes = getUsedBytes(storagePool);

        usedBytes += requestedBytes;

        if (usedBytes > capacityBytes) {
            throw new CloudRuntimeException("Insufficient amount of space remains in this primary storage");
        }
    }

    /**
     * Returns true if we can take a native snapshot else returns false. Set in
     * StorageSystemSnapshotStrategy
     * @param snapshotId Snapshot
     * @return true if native snapshot, false otherwise
     */
    private boolean shouldTakeSnapshot(long snapshotId) {
        SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(snapshotId, "takeSnapshot");

        if (snapshotDetails != null && snapshotDetails.getValue() != null) {
            return new Boolean(snapshotDetails.getValue());
        }

        return false;
    }

    private long getCsIdForCloning(long volumeId, String cloneOf) {
        VolumeDetailVO volumeDetail = volumeDetailsDao.findDetail(volumeId, cloneOf);

        if (volumeDetail != null && volumeDetail.getValue() != null) {
            return new Long(volumeDetail.getValue());
        }

        return Long.MIN_VALUE;
    }

    /**
     * Checks if the volume can be safely deleted. ie it has no native snapshots
     * @param csVolumeId         Volume
     * @param snapshotToIgnoreId Used to check if this is the only snapshot on the
     *                           volume
     * @return true if we can delete, false otherwise
     */
    private boolean shouldDeleteVolume(Long csVolumeId, Long snapshotToIgnoreId) {

        List<SnapshotVO> lstSnapshots = getNonDestroyedSnapshots(csVolumeId);

        for (SnapshotVO snapshot : lstSnapshots) {
            if (snapshotToIgnoreId != null && snapshot.getId() == snapshotToIgnoreId) {
                continue;
            }
            SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(snapshot.getId(), DateraUtil.SNAPSHOT_ID);

            if (snapshotDetails != null && snapshotDetails.getValue() != null) {
                return false;
            }
        }

        return true;
    }

    private List<SnapshotVO> getNonDestroyedSnapshots(long csVolumeId) {
        List<SnapshotVO> lstSnapshots = _snapshotDao.listByVolumeId(csVolumeId);

        if (lstSnapshots == null) {
            lstSnapshots = new ArrayList<>();
        }

        List<SnapshotVO> lstSnapshots2 = new ArrayList<>();

        for (SnapshotVO snapshot : lstSnapshots) {
            if (!Snapshot.State.Destroyed.equals(snapshot.getState())) {
                lstSnapshots2.add(snapshot);
            }
        }

        return lstSnapshots2;
    }

    private long getVolumeSizeIncludingHypervisorSnapshotReserve(long volumeSize, Integer hypervisorSnapshotReserve) {
        if (hypervisorSnapshotReserve != null) {
            hypervisorSnapshotReserve = Math.max(hypervisorSnapshotReserve, s_lowestHypervisorSnapshotReserve);

            volumeSize += volumeSize * (hypervisorSnapshotReserve / 100f);
        }

        return volumeSize;
    }
}
