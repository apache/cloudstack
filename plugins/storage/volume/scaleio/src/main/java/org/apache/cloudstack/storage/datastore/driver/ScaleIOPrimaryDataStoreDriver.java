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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.api.Sdc;
import org.apache.cloudstack.storage.datastore.api.StoragePoolStatistics;
import org.apache.cloudstack.storage.datastore.api.VolumeStatistics;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.host.Host;
import com.cloud.server.ManagementServerImpl;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class ScaleIOPrimaryDataStoreDriver implements PrimaryDataStoreDriver {
    private static final Logger LOGGER = Logger.getLogger(ScaleIOPrimaryDataStoreDriver.class);

    @Inject
    EndPointSelector selector;
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private VolumeDetailsDao volumeDetailsDao;
    @Inject
    private VMTemplatePoolDao vmTemplatePoolDao;
    @Inject
    private SnapshotDataStoreDao snapshotDataStoreDao;
    @Inject
    protected SnapshotDao snapshotDao;
    @Inject
    private AlertManager alertMgr;
    @Inject
    private ConfigurationDao configDao;

    public ScaleIOPrimaryDataStoreDriver() {

    }

    private ScaleIOGatewayClient getScaleIOClient(final Long storagePoolId) throws Exception {
        return ScaleIOGatewayClientConnectionPool.getInstance().getClient(storagePoolId, storagePoolDetailsDao);
    }

    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {
        try {
            if (DataObjectType.VOLUME.equals(dataObject.getType())) {
                final VolumeVO volume = volumeDao.findById(dataObject.getId());
                LOGGER.debug("Granting access for PowerFlex volume: " + volume.getPath());

                Long bandwidthLimitInKbps = Long.valueOf(0); // Unlimited
                // Check Bandwidht Limit parameter in volume details
                final VolumeDetailVO bandwidthVolumeDetail = volumeDetailsDao.findDetail(volume.getId(), Volume.BANDWIDTH_LIMIT_IN_MBPS);
                if (bandwidthVolumeDetail != null && bandwidthVolumeDetail.getValue() != null) {
                    bandwidthLimitInKbps = Long.parseLong(bandwidthVolumeDetail.getValue()) * 1024;
                }

                Long iopsLimit = Long.valueOf(0); // Unlimited
                // Check IOPS Limit parameter in volume details, else try MaxIOPS
                final VolumeDetailVO iopsVolumeDetail = volumeDetailsDao.findDetail(volume.getId(), Volume.IOPS_LIMIT);
                if (iopsVolumeDetail != null && iopsVolumeDetail.getValue() != null) {
                    iopsLimit = Long.parseLong(iopsVolumeDetail.getValue());
                } else if (volume.getMaxIops() != null) {
                    iopsLimit = volume.getMaxIops();
                }
                if (iopsLimit > 0 && iopsLimit < ScaleIOUtil.MINIMUM_ALLOWED_IOPS_LIMIT) {
                    iopsLimit = ScaleIOUtil.MINIMUM_ALLOWED_IOPS_LIMIT;
                }

                final ScaleIOGatewayClient client = getScaleIOClient(dataStore.getId());
                final Sdc sdc = client.getConnectedSdcByIp(host.getPrivateIpAddress());
                if (sdc == null) {
                    alertHostSdcDisconnection(host);
                    throw new CloudRuntimeException("Unable to grant access to volume: " + dataObject.getId() + ", no Sdc connected with host ip: " + host.getPrivateIpAddress());
                }

                return client.mapVolumeToSdcWithLimits(ScaleIOUtil.getVolumePath(volume.getPath()), sdc.getId(), iopsLimit, bandwidthLimitInKbps);
            } else if (DataObjectType.TEMPLATE.equals(dataObject.getType())) {
                final VMTemplateStoragePoolVO templatePoolRef = vmTemplatePoolDao.findByPoolTemplate(dataStore.getId(), dataObject.getId(), null);
                LOGGER.debug("Granting access for PowerFlex template volume: " + templatePoolRef.getInstallPath());

                final ScaleIOGatewayClient client = getScaleIOClient(dataStore.getId());
                final Sdc sdc = client.getConnectedSdcByIp(host.getPrivateIpAddress());
                if (sdc == null) {
                    alertHostSdcDisconnection(host);
                    throw new CloudRuntimeException("Unable to grant access to template: " + dataObject.getId() + ", no Sdc connected with host ip: " + host.getPrivateIpAddress());
                }

                return client.mapVolumeToSdc(ScaleIOUtil.getVolumePath(templatePoolRef.getInstallPath()), sdc.getId());
            } else if (DataObjectType.SNAPSHOT.equals(dataObject.getType())) {
                SnapshotInfo snapshot = (SnapshotInfo) dataObject;
                LOGGER.debug("Granting access for PowerFlex volume snapshot: " + snapshot.getPath());

                final ScaleIOGatewayClient client = getScaleIOClient(dataStore.getId());
                final Sdc sdc = client.getConnectedSdcByIp(host.getPrivateIpAddress());
                if (sdc == null) {
                    alertHostSdcDisconnection(host);
                    throw new CloudRuntimeException("Unable to grant access to snapshot: " + dataObject.getId() + ", no Sdc connected with host ip: " + host.getPrivateIpAddress());
                }

                return client.mapVolumeToSdc(ScaleIOUtil.getVolumePath(snapshot.getPath()), sdc.getId());
            }

            return false;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
        try {
            if (DataObjectType.VOLUME.equals(dataObject.getType())) {
                final VolumeVO volume = volumeDao.findById(dataObject.getId());
                LOGGER.debug("Revoking access for PowerFlex volume: " + volume.getPath());

                final ScaleIOGatewayClient client = getScaleIOClient(dataStore.getId());
                final Sdc sdc = client.getConnectedSdcByIp(host.getPrivateIpAddress());
                if (sdc == null) {
                    throw new CloudRuntimeException("Unable to revoke access for volume: " + dataObject.getId() + ", no Sdc connected with host ip: " + host.getPrivateIpAddress());
                }

                client.unmapVolumeFromSdc(ScaleIOUtil.getVolumePath(volume.getPath()), sdc.getId());
            } else if (DataObjectType.TEMPLATE.equals(dataObject.getType())) {
                final VMTemplateStoragePoolVO templatePoolRef = vmTemplatePoolDao.findByPoolTemplate(dataStore.getId(), dataObject.getId(), null);
                LOGGER.debug("Revoking access for PowerFlex template volume: " + templatePoolRef.getInstallPath());

                final ScaleIOGatewayClient client = getScaleIOClient(dataStore.getId());
                final Sdc sdc = client.getConnectedSdcByIp(host.getPrivateIpAddress());
                if (sdc == null) {
                    throw new CloudRuntimeException("Unable to revoke access for template: " + dataObject.getId() + ", no Sdc connected with host ip: " + host.getPrivateIpAddress());
                }

                client.unmapVolumeFromSdc(ScaleIOUtil.getVolumePath(templatePoolRef.getInstallPath()), sdc.getId());
            } else if (DataObjectType.SNAPSHOT.equals(dataObject.getType())) {
                SnapshotInfo snapshot = (SnapshotInfo) dataObject;
                LOGGER.debug("Revoking access for PowerFlex volume snapshot: " + snapshot.getPath());

                final ScaleIOGatewayClient client = getScaleIOClient(dataStore.getId());
                final Sdc sdc = client.getConnectedSdcByIp(host.getPrivateIpAddress());
                if (sdc == null) {
                    throw new CloudRuntimeException("Unable to revoke access for snapshot: " + dataObject.getId() + ", no Sdc connected with host ip: " + host.getPrivateIpAddress());
                }

                client.unmapVolumeFromSdc(ScaleIOUtil.getVolumePath(snapshot.getPath()), sdc.getId());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to revoke access due to: " + e.getMessage(), e);
        }
    }

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        long usedSpaceBytes = 0;
        // Volumes
        List<VolumeVO> volumes = volumeDao.findByPoolIdAndState(storagePool.getId(), Volume.State.Ready);
        if (volumes != null) {
            for (VolumeVO volume : volumes) {
                usedSpaceBytes += volume.getSize();

                long vmSnapshotChainSize = volume.getVmSnapshotChainSize() == null ? 0 : volume.getVmSnapshotChainSize();
                usedSpaceBytes += vmSnapshotChainSize;
            }
        }

        //Snapshots
        List<SnapshotDataStoreVO> snapshots = snapshotDataStoreDao.listByStoreIdAndState(storagePool.getId(), ObjectInDataStoreStateMachine.State.Ready);
        if (snapshots != null) {
            for (SnapshotDataStoreVO snapshot : snapshots) {
                usedSpaceBytes += snapshot.getSize();
            }
        }

        // Templates
        List<VMTemplateStoragePoolVO> templates = vmTemplatePoolDao.listByPoolIdAndState(storagePool.getId(), ObjectInDataStoreStateMachine.State.Ready);
        if (templates != null) {
            for (VMTemplateStoragePoolVO template : templates) {
                usedSpaceBytes += template.getTemplateSize();
            }
        }

        LOGGER.debug("Used/Allocated storage space (in bytes): " + String.valueOf(usedSpaceBytes));

        return usedSpaceBytes;
    }

    @Override
    public long getUsedIops(StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool pool) {
        return ((dataObject != null && dataObject.getSize() != null) ? dataObject.getSize() : 0);
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {
        if (templateInfo == null || storagePool == null) {
            return 0;
        }

        VMTemplateStoragePoolVO templatePoolRef = vmTemplatePoolDao.findByPoolTemplate(storagePool.getId(), templateInfo.getId(), null);
        if (templatePoolRef != null) {
            // Template exists on this primary storage, do not require additional space
            return 0;
        }

        return getDataObjectSizeIncludingHypervisorSnapshotReserve(templateInfo, storagePool);
    }

    @Override
    public Map<String, String> getCapabilities() {
        Map<String, String> mapCapabilities = new HashMap<>();
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        return mapCapabilities;
    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject) {
        return null;
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
    public void takeSnapshot(SnapshotInfo snapshotInfo, AsyncCompletionCallback<CreateCmdResult> callback) {
        LOGGER.debug("Taking PowerFlex volume snapshot");

        Preconditions.checkArgument(snapshotInfo != null, "snapshotInfo cannot be null");

        VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();
        Preconditions.checkArgument(volumeInfo != null, "volumeInfo cannot be null");

        VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());

        long storagePoolId = volumeVO.getPoolId();
        Preconditions.checkArgument(storagePoolId > 0, "storagePoolId should be > 0");

        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);
        Preconditions.checkArgument(storagePool != null && storagePool.getHostAddress() != null, "storagePool and host address should not be null");

        CreateCmdResult result;

        try {
            SnapshotObjectTO snapshotObjectTo = (SnapshotObjectTO)snapshotInfo.getTO();

            final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
            final String scaleIOVolumeId = ScaleIOUtil.getVolumePath(volumeVO.getPath());
            String snapshotName = String.format("%s-%s-%s-%s", ScaleIOUtil.SNAPSHOT_PREFIX, snapshotInfo.getId(),
                    storagePool.getUuid().split("-")[0].substring(4), ManagementServerImpl.customCsIdentifier.value());

            org.apache.cloudstack.storage.datastore.api.Volume scaleIOVolume = null;
            scaleIOVolume = client.takeSnapshot(scaleIOVolumeId, snapshotName);

            if (scaleIOVolume == null) {
                throw new CloudRuntimeException("Failed to take snapshot on PowerFlex cluster");
            }

            snapshotObjectTo.setPath(ScaleIOUtil.updatedPathWithVolumeName(scaleIOVolume.getId(), snapshotName));
            CreateObjectAnswer createObjectAnswer = new CreateObjectAnswer(snapshotObjectTo);
            result = new CreateCmdResult(null, createObjectAnswer);
            result.setResult(null);
        } catch (Exception e) {
            String errMsg = "Unable to take PowerFlex volume snapshot for volume: " + volumeInfo.getId() + " due to " + e.getMessage();
            LOGGER.warn(errMsg);
            result = new CreateCmdResult(null, new CreateObjectAnswer(e.toString()));
            result.setResult(e.toString());
        }

        callback.complete(result);
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, SnapshotInfo snapshotOnPrimaryStore, AsyncCompletionCallback<CommandResult> callback) {
        LOGGER.debug("Reverting to PowerFlex volume snapshot");

        Preconditions.checkArgument(snapshot != null, "snapshotInfo cannot be null");

        VolumeInfo volumeInfo = snapshot.getBaseVolume();
        Preconditions.checkArgument(volumeInfo != null, "volumeInfo cannot be null");

        VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());

        try {
            if (volumeVO == null || volumeVO.getRemoved() != null) {
                String errMsg = "The volume that the snapshot belongs to no longer exists.";
                CommandResult commandResult = new CommandResult();
                commandResult.setResult(errMsg);
                callback.complete(commandResult);
                return;
            }

            long storagePoolId = volumeVO.getPoolId();
            final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
            String snapshotVolumeId = ScaleIOUtil.getVolumePath(snapshot.getPath());
            final String destVolumeId = ScaleIOUtil.getVolumePath(volumeVO.getPath());
            client.revertSnapshot(snapshotVolumeId, destVolumeId);

            CommandResult commandResult = new CommandResult();
            callback.complete(commandResult);
        } catch (Exception ex) {
            LOGGER.debug("Unable to revert to PowerFlex snapshot: " + snapshot.getId(), ex);
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    private String createVolume(VolumeInfo volumeInfo, long storagePoolId) {
        LOGGER.debug("Creating PowerFlex volume");

        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);

        Preconditions.checkArgument(volumeInfo != null, "volumeInfo cannot be null");
        Preconditions.checkArgument(storagePoolId > 0, "storagePoolId should be > 0");
        Preconditions.checkArgument(storagePool != null && storagePool.getHostAddress() != null, "storagePool and host address should not be null");

        try {
            final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
            final String scaleIOStoragePoolId = storagePool.getPath();
            final Long sizeInBytes = volumeInfo.getSize();
            final long sizeInGb = (long) Math.ceil(sizeInBytes / (1024.0 * 1024.0 * 1024.0));
            final String scaleIOVolumeName = String.format("%s-%s-%s-%s", ScaleIOUtil.VOLUME_PREFIX, volumeInfo.getId(),
                    storagePool.getUuid().split("-")[0].substring(4), ManagementServerImpl.customCsIdentifier.value());

            org.apache.cloudstack.storage.datastore.api.Volume scaleIOVolume = null;
            scaleIOVolume = client.createVolume(scaleIOVolumeName, scaleIOStoragePoolId, (int) sizeInGb, volumeInfo.getProvisioningType());

            if (scaleIOVolume == null) {
                throw new CloudRuntimeException("Failed to create volume on PowerFlex cluster");
            }

            VolumeVO volume = volumeDao.findById(volumeInfo.getId());
            String volumePath = ScaleIOUtil.updatedPathWithVolumeName(scaleIOVolume.getId(), scaleIOVolumeName);
            volume.set_iScsiName(volumePath);
            volume.setPath(volumePath);
            volume.setFolder(scaleIOVolume.getVtreeId());
            volume.setSize(scaleIOVolume.getSizeInKb() * 1024);
            volume.setPoolType(Storage.StoragePoolType.PowerFlex);
            volume.setFormat(Storage.ImageFormat.RAW);
            volume.setPoolId(storagePoolId);
            volumeDao.update(volume.getId(), volume);

            long capacityBytes = storagePool.getCapacityBytes();
            long usedBytes = storagePool.getUsedBytes();
            usedBytes += volume.getSize();
            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);
            storagePoolDao.update(storagePoolId, storagePool);

            return volumePath;
        } catch (Exception e) {
            String errMsg = "Unable to create PowerFlex Volume due to " + e.getMessage();
            LOGGER.warn(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    private String  createTemplateVolume(TemplateInfo templateInfo, long storagePoolId) {
        LOGGER.debug("Creating PowerFlex template volume");

        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);
        Preconditions.checkArgument(templateInfo != null, "templateInfo cannot be null");
        Preconditions.checkArgument(storagePoolId > 0, "storagePoolId should be > 0");
        Preconditions.checkArgument(storagePool != null && storagePool.getHostAddress() != null, "storagePool and host address should not be null");

        try {
            final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
            final String scaleIOStoragePoolId = storagePool.getPath();
            final Long sizeInBytes = templateInfo.getSize();
            final long sizeInGb = (long) Math.ceil(sizeInBytes / (1024.0 * 1024.0 * 1024.0));
            final String scaleIOVolumeName = String.format("%s-%s-%s-%s", ScaleIOUtil.TEMPLATE_PREFIX, templateInfo.getId(),
                    storagePool.getUuid().split("-")[0].substring(4), ManagementServerImpl.customCsIdentifier.value());

            org.apache.cloudstack.storage.datastore.api.Volume scaleIOVolume = null;
            scaleIOVolume = client.createVolume(scaleIOVolumeName, scaleIOStoragePoolId, (int) sizeInGb, Storage.ProvisioningType.THIN);

            if (scaleIOVolume == null) {
                throw new CloudRuntimeException("Failed to create template volume on PowerFlex cluster");
            }

            VMTemplateStoragePoolVO templatePoolRef = vmTemplatePoolDao.findByPoolTemplate(storagePoolId, templateInfo.getId(), null);
            String templatePath = ScaleIOUtil.updatedPathWithVolumeName(scaleIOVolume.getId(), scaleIOVolumeName);
            templatePoolRef.setInstallPath(templatePath);
            templatePoolRef.setLocalDownloadPath(scaleIOVolume.getId());
            templatePoolRef.setTemplateSize(scaleIOVolume.getSizeInKb() * 1024);
            vmTemplatePoolDao.update(templatePoolRef.getId(), templatePoolRef);

            long capacityBytes = storagePool.getCapacityBytes();
            long usedBytes = storagePool.getUsedBytes();
            usedBytes += templatePoolRef.getTemplateSize();
            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);
            storagePoolDao.update(storagePoolId, storagePool);

            return templatePath;
        } catch (Exception e) {
            String errMsg = "Unable to create PowerFlex template volume due to " + e.getMessage();
            LOGGER.warn(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String scaleIOVolumePath = null;
        String errMsg = null;
        try {
            if (dataObject.getType() == DataObjectType.VOLUME) {
                LOGGER.debug("createAsync - creating volume");
                scaleIOVolumePath = createVolume((VolumeInfo) dataObject, dataStore.getId());
            } else if (dataObject.getType() == DataObjectType.TEMPLATE) {
                LOGGER.debug("createAsync - creating template");
                scaleIOVolumePath = createTemplateVolume((TemplateInfo)dataObject, dataStore.getId());
            } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
                LOGGER.error(errMsg);
            }
        } catch (Exception ex) {
            errMsg = ex.getMessage();
            LOGGER.error(errMsg);
            if (callback == null) {
                throw ex;
            }
        }

        if (callback != null) {
            CreateCmdResult result = new CreateCmdResult(scaleIOVolumePath, new Answer(null, errMsg == null, errMsg));
            result.setResult(errMsg);
            callback.complete(result);
        }
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CommandResult> callback) {
        Preconditions.checkArgument(dataObject != null, "dataObject cannot be null");

        long storagePoolId = dataStore.getId();
        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);
        Preconditions.checkArgument(storagePoolId > 0, "storagePoolId should be > 0");
        Preconditions.checkArgument(storagePool != null && storagePool.getHostAddress() != null, "storagePool and host address should not be null");

        String errMsg = null;
        String scaleIOVolumePath = null;
        try {
            boolean deleteResult = false;
            if (dataObject.getType() == DataObjectType.VOLUME) {
                LOGGER.debug("deleteAsync - deleting volume");
                scaleIOVolumePath = ((VolumeInfo) dataObject).getPath();
            } else if (dataObject.getType() == DataObjectType.SNAPSHOT) {
                LOGGER.debug("deleteAsync - deleting snapshot");
                scaleIOVolumePath = ((SnapshotInfo) dataObject).getPath();
            } else if (dataObject.getType() == DataObjectType.TEMPLATE) {
                LOGGER.debug("deleteAsync - deleting template");
                scaleIOVolumePath = ((TemplateInfo) dataObject).getInstallPath();
            } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to deleteAsync";
                LOGGER.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }

            try {
                String scaleIOVolumeId = ScaleIOUtil.getVolumePath(scaleIOVolumePath);
                final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
                deleteResult =  client.deleteVolume(scaleIOVolumeId);
                if (!deleteResult) {
                    errMsg = "Failed to delete PowerFlex volume with id: " + scaleIOVolumeId;
                }

                long usedBytes = storagePool.getUsedBytes();
                usedBytes -= dataObject.getSize();
                storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);
                storagePoolDao.update(storagePoolId, storagePool);
            } catch (Exception e) {
                errMsg = "Unable to delete PowerFlex volume: " + scaleIOVolumePath + " due to " + e.getMessage();
                LOGGER.warn(errMsg);
                throw new CloudRuntimeException(errMsg, e);
            }
        } catch (Exception ex) {
            errMsg = ex.getMessage();
            LOGGER.error(errMsg);
            if (callback == null) {
                throw ex;
            }
        }

        if (callback != null) {
            CommandResult result = new CommandResult();
            result.setResult(errMsg);
            callback.complete(result);
        }
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        copyAsync(srcData, destData, null, callback);
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        Answer answer = null;
        String errMsg = null;

        try {
            DataStore srcStore = srcData.getDataStore();
            DataStore destStore = destData.getDataStore();
            if (srcStore.getRole() == DataStoreRole.Primary && (destStore.getRole() == DataStoreRole.Primary && destData.getType() == DataObjectType.VOLUME)) {
                if (srcData.getType() == DataObjectType.TEMPLATE) {
                    answer = copyTemplateToVolume(srcData, destData, destHost);
                    if (answer == null) {
                        errMsg = "No answer for copying template to PowerFlex volume";
                    } else if (!answer.getResult()) {
                        errMsg = answer.getDetails();
                    }
                } else if (srcData.getType() == DataObjectType.VOLUME) {
                    if (isSameScaleIOStorageInstance(srcStore, destStore)) {
                        answer = migrateVolume(srcData, destData);
                    } else {
                        answer = copyVolume(srcData, destData, destHost);
                    }

                    if (answer == null) {
                        errMsg = "No answer for migrate PowerFlex volume";
                    } else if (!answer.getResult()) {
                        errMsg = answer.getDetails();
                    }
                } else {
                    errMsg = "Unsupported copy operation from src object: (" + srcData.getType() + ", " + srcData.getDataStore() + "), dest object: ("
                            + destData.getType() + ", " + destData.getDataStore() + ")";
                    LOGGER.warn(errMsg);
                }
            } else {
                errMsg = "Unsupported copy operation";
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to copy due to " + e.getMessage(), e);
            errMsg = e.toString();
        }

        CopyCommandResult result = new CopyCommandResult(null, answer);
        result.setResult(errMsg);
        callback.complete(result);
    }

    private Answer copyTemplateToVolume(DataObject srcData, DataObject destData, Host destHost) {
        // Copy PowerFlex/ScaleIO template to volume
        LOGGER.debug("Initiating copy from PowerFlex template volume on host " + destHost != null ? destHost.getId() : "");
        int primaryStorageDownloadWait = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();
        CopyCommand cmd = new CopyCommand(srcData.getTO(), destData.getTO(), primaryStorageDownloadWait, VirtualMachineManager.ExecuteInSequence.value());

        Answer answer = null;
        EndPoint ep = destHost != null ? RemoteHostEndPoint.getHypervisorHostEndPoint(destHost) : selector.select(srcData.getDataStore());
        if (ep == null) {
            String errorMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            LOGGER.error(errorMsg);
            answer = new Answer(cmd, false, errorMsg);
        } else {
            answer = ep.sendMessage(cmd);
        }

        return answer;
    }

    private Answer copyVolume(DataObject srcData, DataObject destData, Host destHost) {
        // Copy PowerFlex/ScaleIO volume
        LOGGER.debug("Initiating copy from PowerFlex volume on host " + destHost != null ? destHost.getId() : "");
        String value = configDao.getValue(Config.CopyVolumeWait.key());
        int copyVolumeWait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));

        CopyCommand cmd = new CopyCommand(srcData.getTO(), destData.getTO(), copyVolumeWait, VirtualMachineManager.ExecuteInSequence.value());

        Answer answer = null;
        EndPoint ep = destHost != null ? RemoteHostEndPoint.getHypervisorHostEndPoint(destHost) : selector.select(srcData.getDataStore());
        if (ep == null) {
            String errorMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            LOGGER.error(errorMsg);
            answer = new Answer(cmd, false, errorMsg);
        } else {
            answer = ep.sendMessage(cmd);
        }

        return answer;
    }

    private Answer migrateVolume(DataObject srcData, DataObject destData) {
        // Volume migration within same PowerFlex/ScaleIO cluster (with same System ID)
        DataStore srcStore = srcData.getDataStore();
        DataStore destStore = destData.getDataStore();
        Answer answer = null;
        try {
            long srcPoolId = srcStore.getId();
            long destPoolId = destStore.getId();

            final ScaleIOGatewayClient client = getScaleIOClient(srcPoolId);
            final String srcVolumePath = ((VolumeInfo) srcData).getPath();
            final String srcVolumeId = ScaleIOUtil.getVolumePath(srcVolumePath);
            final StoragePoolVO destStoragePool = storagePoolDao.findById(destPoolId);
            final String destStoragePoolId = destStoragePool.getPath();
            int migrationTimeout = StorageManager.KvmStorageOfflineMigrationWait.value();
            boolean migrateStatus = client.migrateVolume(srcVolumeId, destStoragePoolId, migrationTimeout);
            if (migrateStatus) {
                String newVolumeName = String.format("%s-%s-%s-%s", ScaleIOUtil.VOLUME_PREFIX, destData.getId(),
                        destStoragePool.getUuid().split("-")[0].substring(4), ManagementServerImpl.customCsIdentifier.value());
                boolean renamed = client.renameVolume(srcVolumeId, newVolumeName);

                if (srcData.getId() != destData.getId()) {
                    VolumeVO destVolume = volumeDao.findById(destData.getId());
                    // Volume Id in the PowerFlex/ScaleIO pool remains the same after the migration
                    // Update PowerFlex volume name only after it is renamed, to maintain the consistency
                    if (renamed) {
                        String newVolumePath = ScaleIOUtil.updatedPathWithVolumeName(srcVolumeId, newVolumeName);
                        destVolume.set_iScsiName(newVolumePath);
                        destVolume.setPath(newVolumePath);
                    } else {
                        destVolume.set_iScsiName(srcVolumePath);
                        destVolume.setPath(srcVolumePath);
                    }
                    volumeDao.update(destData.getId(), destVolume);

                    VolumeVO srcVolume = volumeDao.findById(srcData.getId());
                    srcVolume.set_iScsiName(null);
                    srcVolume.setPath(null);
                    srcVolume.setFolder(null);
                    volumeDao.update(srcData.getId(), srcVolume);
                } else {
                    // Live migrate volume
                    VolumeVO volume = volumeDao.findById(srcData.getId());
                    Long oldPoolId = volume.getPoolId();
                    volume.setPoolId(destPoolId);
                    volume.setLastPoolId(oldPoolId);
                    volumeDao.update(srcData.getId(), volume);
                }

                List<SnapshotVO> snapshots = snapshotDao.listByVolumeId(srcData.getId());
                if (CollectionUtils.isNotEmpty(snapshots)) {
                    for (SnapshotVO snapshot : snapshots) {
                        SnapshotDataStoreVO snapshotStore = snapshotDataStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Primary);
                        if (snapshotStore == null) {
                            continue;
                        }

                        String snapshotVolumeId = ScaleIOUtil.getVolumePath(snapshotStore.getInstallPath());
                        String newSnapshotName = String.format("%s-%s-%s-%s", ScaleIOUtil.SNAPSHOT_PREFIX, snapshot.getId(),
                                destStoragePool.getUuid().split("-")[0].substring(4), ManagementServerImpl.customCsIdentifier.value());
                        renamed = client.renameVolume(snapshotVolumeId, newSnapshotName);

                        snapshotStore.setDataStoreId(destPoolId);
                        // Snapshot Id in the PowerFlex/ScaleIO pool remains the same after the migration
                        // Update PowerFlex snapshot name only after it is renamed, to maintain the consistency
                        if (renamed) {
                            snapshotStore.setInstallPath(ScaleIOUtil.updatedPathWithVolumeName(snapshotVolumeId, newSnapshotName));
                        }
                        snapshotDataStoreDao.update(snapshotStore.getId(), snapshotStore);
                    }
                }

                answer = new Answer(null, true, null);
            } else {
                String errorMsg = "Failed to migrate PowerFlex volume: " + srcData.getId() + " to storage pool " + destPoolId;
                LOGGER.debug(errorMsg);
                answer = new Answer(null, false, errorMsg);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to migrate PowerFlex volume: " + srcData.getId() + " due to: " + e.getMessage());
            answer = new Answer(null, false, e.getMessage());
        }

        return answer;
    }

    private boolean isSameScaleIOStorageInstance(DataStore srcStore, DataStore destStore) {
        long srcPoolId = srcStore.getId();
        String srcPoolSystemId = null;
        StoragePoolDetailVO srcPoolSystemIdDetail = storagePoolDetailsDao.findDetail(srcPoolId, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        if (srcPoolSystemIdDetail != null) {
            srcPoolSystemId = srcPoolSystemIdDetail.getValue();
        }

        long destPoolId = destStore.getId();
        String destPoolSystemId = null;
        StoragePoolDetailVO destPoolSystemIdDetail = storagePoolDetailsDao.findDetail(destPoolId, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        if (destPoolSystemIdDetail != null) {
            destPoolSystemId = destPoolSystemIdDetail.getValue();
        }

        if (Strings.isNullOrEmpty(srcPoolSystemId) || Strings.isNullOrEmpty(destPoolSystemId)) {
            throw new CloudRuntimeException("Failed to validate PowerFlex pools compatibility for migration as storage instance details are not available");
        }

        if (srcPoolSystemId.equals(destPoolSystemId)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        DataStore srcStore = destData.getDataStore();
        DataStore destStore = destData.getDataStore();
        if ((srcStore.getRole() == DataStoreRole.Primary && (srcData.getType() == DataObjectType.TEMPLATE || srcData.getType() == DataObjectType.VOLUME))
                && (destStore.getRole() == DataStoreRole.Primary && destData.getType() == DataObjectType.VOLUME)) {
            StoragePoolVO srcPoolVO = storagePoolDao.findById(srcStore.getId());
            StoragePoolVO destPoolVO = storagePoolDao.findById(destStore.getId());
            if (srcPoolVO != null && srcPoolVO.getPoolType() == Storage.StoragePoolType.PowerFlex
                    && destPoolVO != null && destPoolVO.getPoolType() == Storage.StoragePoolType.PowerFlex) {
                return true;
            }
        }
        return false;
    }

    private void resizeVolume(VolumeInfo volumeInfo) {
        LOGGER.debug("Resizing PowerFlex volume");

        Preconditions.checkArgument(volumeInfo != null, "volumeInfo cannot be null");

        try {
            String scaleIOVolumeId = ScaleIOUtil.getVolumePath(volumeInfo.getPath());
            Long storagePoolId = volumeInfo.getPoolId();

            ResizeVolumePayload payload = (ResizeVolumePayload)volumeInfo.getpayload();
            long newSizeInBytes = payload.newSize != null ? payload.newSize : volumeInfo.getSize();
            // Only increase size is allowed and size should be specified in granularity of 8 GB
            if (newSizeInBytes <= volumeInfo.getSize()) {
                throw new CloudRuntimeException("Only increase size is allowed for volume: " + volumeInfo.getName());
            }

            org.apache.cloudstack.storage.datastore.api.Volume scaleIOVolume = null;
            long newSizeInGB = newSizeInBytes / (1024 * 1024 * 1024);
            long newSizeIn8gbBoundary = (long) (Math.ceil(newSizeInGB / 8.0) * 8.0);
            final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
            scaleIOVolume = client.resizeVolume(scaleIOVolumeId, (int) newSizeIn8gbBoundary);
            if (scaleIOVolume == null) {
                throw new CloudRuntimeException("Failed to resize volume: " + volumeInfo.getName());
            }

            VolumeVO volume = volumeDao.findById(volumeInfo.getId());
            long oldVolumeSize = volume.getSize();
            volume.setSize(scaleIOVolume.getSizeInKb() * 1024);
            volumeDao.update(volume.getId(), volume);

            StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);
            long capacityBytes = storagePool.getCapacityBytes();
            long usedBytes = storagePool.getUsedBytes();

            long newVolumeSize = volume.getSize();
            usedBytes += newVolumeSize - oldVolumeSize;
            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);
            storagePoolDao.update(storagePoolId, storagePool);
        } catch (Exception e) {
            String errMsg = "Unable to resize PowerFlex volume: " + volumeInfo.getId() + " due to " + e.getMessage();
            LOGGER.warn(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    @Override
    public void resize(DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String scaleIOVolumePath = null;
        String errMsg = null;
        try {
            if (dataObject.getType() == DataObjectType.VOLUME) {
                scaleIOVolumePath = ((VolumeInfo) dataObject).getPath();
                resizeVolume((VolumeInfo) dataObject);
            } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to resize";
            }
        } catch (Exception ex) {
            errMsg = ex.getMessage();
            LOGGER.error(errMsg);
            if (callback == null) {
                throw ex;
            }
        }

        if (callback != null) {
            CreateCmdResult result = new CreateCmdResult(scaleIOVolumePath, new Answer(null, errMsg == null, errMsg));
            result.setResult(errMsg);
            callback.complete(result);
        }
    }

    @Override
    public void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, QualityOfServiceState qualityOfServiceState) {
    }

    @Override
    public boolean canProvideStorageStats() {
        return true;
    }

    @Override
    public Pair<Long, Long> getStorageStats(StoragePool storagePool) {
        Preconditions.checkArgument(storagePool != null, "storagePool cannot be null");

        try {
            final ScaleIOGatewayClient client = getScaleIOClient(storagePool.getId());
            StoragePoolStatistics poolStatistics = client.getStoragePoolStatistics(storagePool.getPath());
            if (poolStatistics != null && poolStatistics.getNetMaxCapacityInBytes() != null && poolStatistics.getNetUsedCapacityInBytes() != null) {
                Long capacityBytes = poolStatistics.getNetMaxCapacityInBytes();
                Long usedBytes = poolStatistics.getNetUsedCapacityInBytes();
                return new Pair<Long, Long>(capacityBytes, usedBytes);
            }
        }  catch (Exception e) {
            String errMsg = "Unable to get storage stats for the pool: " + storagePool.getId() + " due to " + e.getMessage();
            LOGGER.warn(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        }

        return null;
    }

    @Override
    public boolean canProvideVolumeStats() {
        return true;
    }

    @Override
    public Pair<Long, Long> getVolumeStats(StoragePool storagePool, String volumePath) {
        Preconditions.checkArgument(storagePool != null, "storagePool cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumePath), "volumePath cannot be null");

        try {
            final ScaleIOGatewayClient client = getScaleIOClient(storagePool.getId());
            VolumeStatistics volumeStatistics = client.getVolumeStatistics(ScaleIOUtil.getVolumePath(volumePath));
            if (volumeStatistics != null) {
                Long provisionedSizeInBytes = volumeStatistics.getNetProvisionedAddressesInBytes();
                Long allocatedSizeInBytes = volumeStatistics.getAllocatedSizeInBytes();
                return new Pair<Long, Long>(provisionedSizeInBytes, allocatedSizeInBytes);
            }
        }  catch (Exception e) {
            String errMsg = "Unable to get stats for the volume: " + volumePath + " in the pool: " + storagePool.getId() + " due to " + e.getMessage();
            LOGGER.warn(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        }

        return null;
    }

    @Override
    public boolean canHostAccessStoragePool(Host host, StoragePool pool) {
        if (host == null || pool == null) {
            return false;
        }

        try {
            final ScaleIOGatewayClient client = getScaleIOClient(pool.getId());
            return client.isSdcConnected(host.getPrivateIpAddress());
        } catch (Exception e) {
            LOGGER.warn("Unable to check the host: " + host.getId() + " access to storage pool: " + pool.getId() + " due to " + e.getMessage(), e);
            return false;
        }
    }

    private void alertHostSdcDisconnection(Host host) {
        if (host == null) {
            return;
        }

        LOGGER.warn("SDC not connected on the host: " + host.getId());
        String msg = "SDC not connected on the host: " + host.getId() + ", reconnect the SDC to MDM";
        alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "SDC disconnected on host: " + host.getUuid(), msg);
    }
}
