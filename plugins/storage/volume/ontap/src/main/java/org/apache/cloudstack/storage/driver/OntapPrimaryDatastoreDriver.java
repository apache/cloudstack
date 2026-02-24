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
package org.apache.cloudstack.storage.driver;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.service.SANStrategy;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.UnifiedSANStrategy;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Primary datastore driver for NetApp ONTAP storage systems.
 * Handles volume lifecycle operations for iSCSI and NFS protocols.
 */
public class OntapPrimaryDatastoreDriver implements PrimaryDataStoreDriver {

    private static final Logger s_logger = LogManager.getLogger(OntapPrimaryDatastoreDriver.class);

    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private VMInstanceDao vmDao;
    @Inject private VolumeDao volumeDao;
    @Inject private VolumeDetailsDao volumeDetailsDao;
    @Inject private SnapshotDetailsDao snapshotDetailsDao;

    @Override
    public Map<String, String> getCapabilities() {
        s_logger.trace("OntapPrimaryDatastoreDriver: getCapabilities: Called");
        Map<String, String> mapCapabilities = new HashMap<>();
        // RAW managed initial implementation: snapshot features not yet supported
        // TODO Set it to false once we start supporting snapshot feature
        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString(), Boolean.TRUE.toString());
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

    /**
     * Creates a volume on the ONTAP storage system.
     */
    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateCmdResult createCmdResult = null;
        String errMsg;

        if (dataObject == null) {
            throw new InvalidParameterValueException("createAsync: dataObject should not be null");
        }
        if (dataStore == null) {
            throw new InvalidParameterValueException("createAsync: dataStore should not be null");
        }
        if (callback == null) {
            throw new InvalidParameterValueException("createAsync: callback should not be null");
        }

        try {
            s_logger.info("createAsync: Started for data store name [{}] and data object name [{}] of type [{}]",
                    dataStore.getName(), dataObject.getName(), dataObject.getType());

            StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
            if (storagePool == null) {
                s_logger.error("createAsync: Storage Pool not found for id: " + dataStore.getId());
                throw new CloudRuntimeException("createAsync: Storage Pool not found for id: " + dataStore.getId());
            }
            String storagePoolUuid = dataStore.getUuid();

            Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(dataStore.getId());

            if (dataObject.getType() == DataObjectType.VOLUME) {
                VolumeInfo volInfo = (VolumeInfo) dataObject;

                // Create the backend storage object (LUN for iSCSI, no-op for NFS)
                CloudStackVolume created = createCloudStackVolume(dataStore, volInfo, details);

                // Update CloudStack volume record with storage pool association and protocol-specific details
                VolumeVO volumeVO = volumeDao.findById(volInfo.getId());
                if (volumeVO != null) {
                    volumeVO.setPoolType(storagePool.getPoolType());
                    volumeVO.setPoolId(storagePool.getId());

                    if (ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(Constants.PROTOCOL))) {
                        String svmName = details.get(Constants.SVM_NAME);
                        String lunName = created != null && created.getLun() != null ? created.getLun().getName() : null;
                        if (lunName == null) {
                            throw new CloudRuntimeException("createAsync: Missing LUN name for volume " + volInfo.getId());
                        }

                        // Persist LUN details for future operations (delete, grant/revoke access)
                        volumeDetailsDao.addDetail(volInfo.getId(), Constants.LUN_DOT_UUID, created.getLun().getUuid(), false);
                        volumeDetailsDao.addDetail(volInfo.getId(), Constants.LUN_DOT_NAME, lunName, false);
                        if (created.getLun().getUuid() != null) {
                            volumeVO.setFolder(created.getLun().getUuid());
                        }

                        // Create LUN-to-igroup mapping and retrieve the assigned LUN ID
                        UnifiedSANStrategy sanStrategy = (UnifiedSANStrategy) Utility.getStrategyByStoragePoolDetails(details);
                        String accessGroupName = Utility.getIgroupName(svmName, storagePoolUuid);
                        String lunNumber = sanStrategy.ensureLunMapped(svmName, lunName, accessGroupName);

                        // Construct iSCSI path: /<iqn>/<lun_id> format for KVM/libvirt attachment
                        String iscsiPath = Constants.SLASH + storagePool.getPath() + Constants.SLASH + lunNumber;
                        volumeVO.set_iScsiName(iscsiPath);
                        volumeVO.setPath(iscsiPath);
                        s_logger.info("createAsync: Volume [{}] iSCSI path set to {}", volumeVO.getId(), iscsiPath);
                        createCmdResult = new CreateCmdResult(null, new Answer(null, true, null));

                    } else if (ProtocolType.NFS3.name().equalsIgnoreCase(details.get(Constants.PROTOCOL))) {
                        createCmdResult = new CreateCmdResult(volInfo.getUuid(), new Answer(null, true, null));
                        s_logger.info("createAsync: Managed NFS volume [{}] associated with pool {}",
                                volumeVO.getId(), storagePool.getId());
                    }
                    volumeDao.update(volumeVO.getId(), volumeVO);
                }
            } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
                s_logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (Exception e) {
            errMsg = e.getMessage();
            s_logger.error("createAsync: Failed for dataObject name [{}]: {}", dataObject.getName(), errMsg);
            createCmdResult = new CreateCmdResult(null, new Answer(null, false, errMsg));
            createCmdResult.setResult(e.toString());
        } finally {
            if (createCmdResult != null && createCmdResult.isSuccess()) {
                s_logger.info("createAsync: Operation completed successfully for {}", dataObject.getType());
            }
            callback.complete(createCmdResult);
        }
    }

    /**
     * Creates a volume on the ONTAP backend.
     */
    private CloudStackVolume createCloudStackVolume(DataStore dataStore, DataObject dataObject, Map<String, String> details) {
        StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
        if (storagePool == null) {
            s_logger.error("createCloudStackVolume: Storage Pool not found for id: {}", dataStore.getId());
            throw new CloudRuntimeException("createCloudStackVolume: Storage Pool not found for id: " + dataStore.getId());
        }

        StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(details);

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeObject = (VolumeInfo) dataObject;
            CloudStackVolume cloudStackVolumeRequest = Utility.createCloudStackVolumeRequestByProtocol(storagePool, details, volumeObject);
            return storageStrategy.createCloudStackVolume(cloudStackVolumeRequest);
        } else {
            throw new CloudRuntimeException("createCloudStackVolume: Unsupported DataObjectType: " + dataObject.getType());
        }
    }

    /**
     * Deletes a volume from the ONTAP storage system.
     */
    @Override
    public void deleteAsync(DataStore store, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        CommandResult commandResult = new CommandResult();
        try {
            if (store == null || data == null) {
                throw new CloudRuntimeException("deleteAsync: store or data is null");
            }

            if (data.getType() == DataObjectType.VOLUME) {
                StoragePoolVO storagePool = storagePoolDao.findById(store.getId());
                if (storagePool == null) {
                    s_logger.error("deleteAsync: Storage Pool not found for id: " + store.getId());
                    throw new CloudRuntimeException("deleteAsync: Storage Pool not found for id: " + store.getId());
                }
                Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(store.getId());
                StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(details);
                s_logger.info("createCloudStackVolumeForTypeVolume: Connection to Ontap SVM [{}] successful, preparing CloudStackVolumeRequest", details.get(Constants.SVM_NAME));
                VolumeInfo volumeInfo = (VolumeInfo) data;
                CloudStackVolume cloudStackVolumeRequest = createDeleteCloudStackVolumeRequest(storagePool,details,volumeInfo);
                storageStrategy.deleteCloudStackVolume(cloudStackVolumeRequest);
                s_logger.error("deleteAsync : Volume deleted: " + volumeInfo.getId());
                commandResult.setResult(null);
                commandResult.setSuccess(true);
            }
        } catch (Exception e) {
            s_logger.error("deleteAsync: Failed for data object [{}]: {}", data, e.getMessage());
            commandResult.setSuccess(false);
            commandResult.setResult(e.getMessage());
        } finally {
            callback.complete(commandResult);
        }
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {

    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {

    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject) {
        return null;
    }

    /**
     * Grants a host access to a volume.
     */
    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {
        try {
            if (dataStore == null) {
                throw new InvalidParameterValueException("grantAccess: dataStore should not be null");
            }
            if (dataObject == null) {
                throw new InvalidParameterValueException("grantAccess: dataObject should not be null");
            }
            if (host == null) {
                throw new InvalidParameterValueException("grantAccess: host should not be null");
            }

            StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
            if (storagePool == null) {
                s_logger.error("grantAccess: Storage Pool not found for id: " + dataStore.getId());
                throw new CloudRuntimeException("grantAccess: Storage Pool not found for id: " + dataStore.getId());
            }
            String storagePoolUuid = dataStore.getUuid();

            // ONTAP managed storage only supports cluster and zone scoped pools
            if (storagePool.getScope() != ScopeType.CLUSTER && storagePool.getScope() != ScopeType.ZONE) {
                s_logger.error("grantAccess: Only Cluster and Zone scoped primary storage is supported for storage Pool: " + storagePool.getName());
                throw new CloudRuntimeException("grantAccess: Only Cluster and Zone scoped primary storage is supported for Storage Pool: " + storagePool.getName());
            }

            if (dataObject.getType() == DataObjectType.VOLUME) {
                VolumeVO volumeVO = volumeDao.findById(dataObject.getId());
                if (volumeVO == null) {
                    s_logger.error("grantAccess: CloudStack Volume not found for id: " + dataObject.getId());
                    throw new CloudRuntimeException("grantAccess: CloudStack Volume not found for id: " + dataObject.getId());
                }

                Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
                String svmName = details.get(Constants.SVM_NAME);

                if (ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(Constants.PROTOCOL))) {
                    // Only retrieve LUN name for iSCSI volumes
                    String cloudStackVolumeName = volumeDetailsDao.findDetail(volumeVO.getId(), Constants.LUN_DOT_NAME).getValue();
                    UnifiedSANStrategy sanStrategy = (UnifiedSANStrategy) Utility.getStrategyByStoragePoolDetails(details);
                    String accessGroupName = Utility.getIgroupName(svmName, storagePoolUuid);

                    // Verify host initiator is registered in the igroup before allowing access
                    if (!sanStrategy.validateInitiatorInAccessGroup(host.getStorageUrl(), svmName, accessGroupName)) {
                        throw new CloudRuntimeException("grantAccess: Host initiator [" + host.getStorageUrl() +
                                "] is not present in iGroup [" + accessGroupName + "]");
                    }

                    // Create or retrieve existing LUN mapping
                    String lunNumber = sanStrategy.ensureLunMapped(svmName, cloudStackVolumeName, accessGroupName);

                    // Update volume path if changed (e.g., after migration or re-mapping)
                    String iscsiPath = Constants.SLASH + storagePool.getPath() + Constants.SLASH + lunNumber;
                    if (volumeVO.getPath() == null || !volumeVO.getPath().equals(iscsiPath)) {
                        volumeVO.set_iScsiName(iscsiPath);
                        volumeVO.setPath(iscsiPath);
                    }
                } else if (ProtocolType.NFS3.name().equalsIgnoreCase(details.get(Constants.PROTOCOL))) {
                    // For NFS, no access grant needed - file is accessible via mount
                    s_logger.debug("grantAccess: NFS volume [{}], no igroup mapping required", volumeVO.getUuid());
                    return true;
                }
                volumeVO.setPoolType(storagePool.getPoolType());
                volumeVO.setPoolId(storagePool.getId());
                volumeDao.update(volumeVO.getId(), volumeVO);
            } else {
                s_logger.error("Invalid DataObjectType (" + dataObject.getType() + ") passed to grantAccess");
                throw new CloudRuntimeException("Invalid DataObjectType (" + dataObject.getType() + ") passed to grantAccess");
            }
            return true;
        } catch (Exception e) {
            s_logger.error("grantAccess: Failed for dataObject [{}]: {}", dataObject, e.getMessage());
            throw new CloudRuntimeException("grantAccess: Failed with error: " + e.getMessage(), e);
        }
    }

    /**
     * Revokes a host's access to a volume.
     */
    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
        try {
            if (dataStore == null) {
                throw new InvalidParameterValueException("revokeAccess: dataStore should not be null");
            }
            if (dataObject == null) {
                throw new InvalidParameterValueException("revokeAccess: dataObject should not be null");
            }
            if (host == null) {
                throw new InvalidParameterValueException("revokeAccess: host should not be null");
            }

            // Safety check: don't revoke access if volume is still attached to an active VM
            if (dataObject.getType() == DataObjectType.VOLUME) {
                Volume volume = volumeDao.findById(dataObject.getId());
                if (volume.getInstanceId() != null) {
                    VirtualMachine vm = vmDao.findById(volume.getInstanceId());
                    if (vm != null && !Arrays.asList(
                            VirtualMachine.State.Destroyed,
                            VirtualMachine.State.Expunging,
                            VirtualMachine.State.Error).contains(vm.getState())) {
                        s_logger.warn("revokeAccess: Volume [{}] is still attached to VM [{}] in state [{}], skipping revokeAccess",
                                dataObject.getId(), vm.getInstanceName(), vm.getState());
                        return;
                    }
                }
            }

            StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
            if (storagePool == null) {
                s_logger.error("revokeAccess: Storage Pool not found for id: " + dataStore.getId());
                throw new CloudRuntimeException("revokeAccess: Storage Pool not found for id: " + dataStore.getId());
            }

            if (storagePool.getScope() != ScopeType.CLUSTER && storagePool.getScope() != ScopeType.ZONE) {
                s_logger.error("revokeAccess: Only Cluster and Zone scoped primary storage is supported for storage Pool: " + storagePool.getName());
                throw new CloudRuntimeException("revokeAccess: Only Cluster and Zone scoped primary storage is supported for Storage Pool: " + storagePool.getName());
            }

            if (dataObject.getType() == DataObjectType.VOLUME) {
                VolumeVO volumeVO = volumeDao.findById(dataObject.getId());
                if (volumeVO == null) {
                    s_logger.error("revokeAccess: CloudStack Volume not found for id: " + dataObject.getId());
                    throw new CloudRuntimeException("revokeAccess: CloudStack Volume not found for id: " + dataObject.getId());
                }
                revokeAccessForVolume(storagePool, volumeVO, host);
            } else {
                s_logger.error("revokeAccess: Invalid DataObjectType (" + dataObject.getType() + ") passed to revokeAccess");
                throw new CloudRuntimeException("Invalid DataObjectType (" + dataObject.getType() + ") passed to revokeAccess");
            }
        } catch (Exception e) {
            s_logger.error("revokeAccess: Failed for dataObject [{}]: {}", dataObject, e.getMessage());
            throw new CloudRuntimeException("revokeAccess: Failed with error: " + e.getMessage(), e);
        }
    }

    /**
     * Revokes volume access for the specified host.
     */
    private void revokeAccessForVolume(StoragePoolVO storagePool, VolumeVO volumeVO, Host host) {
        s_logger.info("revokeAccessForVolume: Revoking access to volume [{}] for host [{}]", volumeVO.getName(), host.getName());

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
        StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(details);
        String svmName = details.get(Constants.SVM_NAME);
        String storagePoolUuid = storagePool.getUuid();

        if (ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(Constants.PROTOCOL))) {
            String accessGroupName = Utility.getIgroupName(svmName, storagePoolUuid);

            // Retrieve LUN name from volume details; if missing, volume may not have been fully created
            String lunName = volumeDetailsDao.findDetail(volumeVO.getId(), Constants.LUN_DOT_NAME) != null ?
                    volumeDetailsDao.findDetail(volumeVO.getId(), Constants.LUN_DOT_NAME).getValue() : null;
            if (lunName == null) {
                s_logger.warn("revokeAccessForVolume: No LUN name found for volume [{}]; skipping revoke", volumeVO.getId());
                return;
            }

            // Verify LUN still exists on ONTAP (may have been manually deleted)
            CloudStackVolume cloudStackVolume = getCloudStackVolumeByName(storageStrategy, svmName, lunName);
            if (cloudStackVolume == null || cloudStackVolume.getLun() == null || cloudStackVolume.getLun().getUuid() == null) {
                s_logger.warn("revokeAccessForVolume: LUN for volume [{}] not found on ONTAP, skipping revoke", volumeVO.getId());
                return;
            }

            // Verify igroup still exists on ONTAP
            AccessGroup accessGroup = getAccessGroupByName(storageStrategy, svmName, accessGroupName);
            if (accessGroup == null || accessGroup.getIgroup() == null || accessGroup.getIgroup().getUuid() == null) {
                s_logger.warn("revokeAccessForVolume: iGroup [{}] not found on ONTAP, skipping revoke", accessGroupName);
                return;
            }

            // Verify host initiator is in the igroup before attempting to remove mapping
            SANStrategy sanStrategy = (UnifiedSANStrategy) storageStrategy;
            if (!sanStrategy.validateInitiatorInAccessGroup(host.getStorageUrl(), svmName, accessGroup.getIgroup().getName())) {
                s_logger.warn("revokeAccessForVolume: Initiator [{}] is not in iGroup [{}], skipping revoke",
                        host.getStorageUrl(), accessGroupName);
                return;
            }

            // Remove the LUN mapping from the igroup
            Map<String, String> disableLogicalAccessMap = new HashMap<>();
            disableLogicalAccessMap.put(Constants.LUN_DOT_UUID, cloudStackVolume.getLun().getUuid());
            disableLogicalAccessMap.put(Constants.IGROUP_DOT_UUID, accessGroup.getIgroup().getUuid());
            storageStrategy.disableLogicalAccess(disableLogicalAccessMap);

            s_logger.info("revokeAccessForVolume: Successfully revoked access to LUN [{}] for host [{}]",
                    lunName, host.getName());
        }
    }

    /**
     * Retrieves a volume from ONTAP by name.
     */
    private CloudStackVolume getCloudStackVolumeByName(StorageStrategy storageStrategy, String svmName, String cloudStackVolumeName) {
        Map<String, String> getCloudStackVolumeMap = new HashMap<>();
        getCloudStackVolumeMap.put(Constants.NAME, cloudStackVolumeName);
        getCloudStackVolumeMap.put(Constants.SVM_DOT_NAME, svmName);

        CloudStackVolume cloudStackVolume = storageStrategy.getCloudStackVolume(getCloudStackVolumeMap);
        if (cloudStackVolume == null || cloudStackVolume.getLun() == null || cloudStackVolume.getLun().getName() == null) {
            s_logger.warn("getCloudStackVolumeByName: LUN [{}] not found on ONTAP", cloudStackVolumeName);
            return null;
        }
        return cloudStackVolume;
    }

    /**
     * Retrieves an access group from ONTAP by name.
     */
    private AccessGroup getAccessGroupByName(StorageStrategy storageStrategy, String svmName, String accessGroupName) {
        Map<String, String> getAccessGroupMap = new HashMap<>();
        getAccessGroupMap.put(Constants.NAME, accessGroupName);
        getAccessGroupMap.put(Constants.SVM_DOT_NAME, svmName);

        AccessGroup accessGroup = storageStrategy.getAccessGroup(getAccessGroupMap);
        if (accessGroup == null || accessGroup.getIgroup() == null || accessGroup.getIgroup().getName() == null) {
            s_logger.warn("getAccessGroupByName: iGroup [{}] not found on ONTAP", accessGroupName);
            return null;
        }
        return accessGroup;
    }

    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getUsedIops(StoragePool storagePool) {
        return 0;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        s_logger.info("takeSnapshot : entered with snapshot id: " + snapshot.getId() + " and name: " + snapshot.getName());
        CreateCmdResult result;

        try {
            VolumeInfo volumeInfo = snapshot.getBaseVolume();

            VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());
            if(volumeVO == null) {
                throw new CloudRuntimeException("takeSnapshot: VolumeVO not found for id: " + volumeInfo.getId());
            }

            /** we are keeping file path at volumeVO.getPath() */

            StoragePoolVO storagePool = storagePoolDao.findById(volumeVO.getPoolId());
            if(storagePool == null) {
                s_logger.error("takeSnapshot : Storage Pool not found for id: " + volumeVO.getPoolId());
                throw new CloudRuntimeException("takeSnapshot : Storage Pool not found for id: " + volumeVO.getPoolId());
            }
            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(volumeVO.getPoolId());
            StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);

            CloudStackVolume cloudStackVolume = null;
            long usedBytes = getUsedBytes(storagePool);
            long capacityBytes = storagePool.getCapacityBytes();
            long fileSize = 0l;
            // Only proceed for NFS3 protocol
            if (ProtocolType.NFS3.name().equalsIgnoreCase(poolDetails.get(Constants.PROTOCOL))) {
                Map<String, String> cloudStackVolumeRequestMap = new HashMap<>();
                cloudStackVolumeRequestMap.put(Constants.VOLUME_UUID, poolDetails.get(Constants.VOLUME_UUID));
                cloudStackVolumeRequestMap.put(Constants.FILE_PATH, volumeVO.getPath());
                cloudStackVolume = storageStrategy.getCloudStackVolume(cloudStackVolumeRequestMap);
                if (cloudStackVolume == null || cloudStackVolume.getFile() == null) {
                    throw new CloudRuntimeException("Failed to get source file to take snapshot");
                    }
                s_logger.info("takeSnapshot : entered after getting cloudstack volume with file path: " + cloudStackVolume.getFile().getPath() + " and size: " + cloudStackVolume.getFile().getSize());
                fileSize = cloudStackVolume.getFile().getSize();
                usedBytes += fileSize;
            }

            if (usedBytes > capacityBytes) {
                throw new CloudRuntimeException("Insufficient space remains in this primary storage to take a snapshot");
            }

            storagePool.setUsedBytes(usedBytes);

            SnapshotObjectTO snapshotObjectTo = (SnapshotObjectTO)snapshot.getTO();

            String snapshotName = volumeInfo.getName() + "-" + snapshot.getUuid();

            int trimRequired = snapshotName.length() - Constants.MAX_SNAPSHOT_NAME_LENGTH;

            if (trimRequired > 0) {
                snapshotName = StringUtils.left(volumeInfo.getName(), (volumeInfo.getName().length() - trimRequired)) + "-" + snapshot.getUuid();
            }

            CloudStackVolume snapCloudStackVolumeRequest = snapshotCloudStackVolumeRequestByProtocol(poolDetails, volumeVO.getPath(), snapshotName);
            CloudStackVolume cloneCloudStackVolume = storageStrategy.snapshotCloudStackVolume(snapCloudStackVolumeRequest);

            updateSnapshotDetails(snapshot.getId(), volumeInfo.getId(), poolDetails.get(Constants.VOLUME_UUID), cloneCloudStackVolume.getFile().getPath(), volumeVO.getPoolId(), fileSize);

            snapshotObjectTo.setPath(Constants.ONTAP_SNAP_ID +"="+cloneCloudStackVolume.getFile().getPath());

            /** Update size for the storage-pool including snapshot size */
            storagePoolDao.update(volumeVO.getPoolId(), storagePool);

            CreateObjectAnswer createObjectAnswer = new CreateObjectAnswer(snapshotObjectTo);

            result = new CreateCmdResult(null, createObjectAnswer);

            result.setResult(null);
        }
        catch (Exception ex) {
            s_logger.error("takeSnapshot: Failed due to ", ex);
            result = new CreateCmdResult(null, new CreateObjectAnswer(ex.toString()));

            result.setResult(ex.toString());
        }

        callback.complete(result);
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshotOnImageStore, SnapshotInfo snapshotOnPrimaryStore, AsyncCompletionCallback<CommandResult> callback) {

    }

    @Override
    public void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, QualityOfServiceState qualityOfServiceState) {

    }

    @Override
    public boolean canProvideStorageStats() {
        return false;
    }

    @Override
    public Pair<Long, Long> getStorageStats(StoragePool storagePool) {
        return null;
    }

    @Override
    public boolean canProvideVolumeStats() {
        return false; // Not yet implemented for RAW managed NFS
    }

    @Override
    public Pair<Long, Long> getVolumeStats(StoragePool storagePool, String volumeId) {
        return null;
    }

    @Override
    public boolean canHostAccessStoragePool(Host host, StoragePool pool) {
        return true;
    }

    @Override
    public boolean isVmInfoNeeded() {
        return true;
    }

    @Override
    public void provideVmInfo(long vmId, long volumeId) {

    }

    @Override
    public boolean isVmTagsNeeded(String tagKey) {
        return true;
    }

    @Override
    public void provideVmTags(long vmId, long volumeId, String tagValue) {

    }

    @Override
    public boolean isStorageSupportHA(Storage.StoragePoolType type) {
        return true;
    }

    @Override
    public void detachVolumeFromAllStorageNodes(Volume volume) {
    }

    private CloudStackVolume createDeleteCloudStackVolumeRequest(StoragePool storagePool, Map<String, String> details, VolumeInfo volumeInfo) {
        CloudStackVolume cloudStackVolumeDeleteRequest = null;

        String protocol = details.get(Constants.PROTOCOL);
        ProtocolType protocolType = ProtocolType.valueOf(protocol);
        switch (protocolType) {
            case NFS3:
                cloudStackVolumeDeleteRequest = new CloudStackVolume();
                cloudStackVolumeDeleteRequest.setDatastoreId(String.valueOf(storagePool.getId()));
                cloudStackVolumeDeleteRequest.setVolumeInfo(volumeInfo);
                break;
            case ISCSI:
                // Retrieve LUN identifiers stored during volume creation
                String lunName = volumeDetailsDao.findDetail(volumeInfo.getId(), Constants.LUN_DOT_NAME).getValue();
                String lunUUID = volumeDetailsDao.findDetail(volumeInfo.getId(), Constants.LUN_DOT_UUID).getValue();
                if (lunName == null) {
                    throw new CloudRuntimeException("deleteAsync: Missing LUN name for volume " + volumeInfo.getId());
                }
                cloudStackVolumeDeleteRequest = new CloudStackVolume();
                Lun lun = new Lun();
                lun.setName(lunName);
                lun.setUuid(lunUUID);
                cloudStackVolumeDeleteRequest.setLun(lun);
                break;
            default:
                throw new CloudRuntimeException("createDeleteCloudStackVolumeRequest: Unsupported protocol " + protocol);

        }
        return cloudStackVolumeDeleteRequest;

    }

    private CloudStackVolume snapshotCloudStackVolumeRequestByProtocol(Map<String, String> details,
                                                                       String sourcePath,
                                                                       String destinationPath) {
        CloudStackVolume cloudStackVolumeRequest = null;
        ProtocolType protocolType = null;
        String protocol = null;

        try {
            protocol = details.get(Constants.PROTOCOL);
            protocolType = ProtocolType.valueOf(protocol);
        } catch (IllegalArgumentException e) {
            throw new CloudRuntimeException("getCloudStackVolumeRequestByProtocol: Protocol: "+ protocol +" is not valid");
        }
        switch (protocolType) {
            case NFS3:
                cloudStackVolumeRequest = new CloudStackVolume();
                FileInfo fileInfo = new FileInfo();
                fileInfo.setPath(sourcePath);
                cloudStackVolumeRequest.setFile(fileInfo);
                String volumeUuid = details.get(Constants.VOLUME_UUID);
                cloudStackVolumeRequest.setFlexVolumeUuid(volumeUuid);
                cloudStackVolumeRequest.setDestinationPath(destinationPath);
                break;
            default:
                throw new CloudRuntimeException("createCloudStackVolumeRequestByProtocol: Unsupported protocol " + protocol);

        }
        return cloudStackVolumeRequest;
    }

    /**
     *
     * @param csSnapshotId: generated snapshot id from cloudstack
     * @param csVolumeId: Source CS volume id
     * @param ontapVolumeUuid: storage flexvolume id
     * @param ontapNewSnapshot: generated snapshot id from ONTAP
     * @param storagePoolId: primary storage pool id
     * @param ontapSnapSize: Size of snapshot CS volume(LUN/file)
     */
    private void updateSnapshotDetails(long csSnapshotId, long csVolumeId, String ontapVolumeUuid, String ontapNewSnapshot, long storagePoolId, long ontapSnapSize) {
        SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(csSnapshotId, Constants.SRC_CS_VOLUME_ID,  String.valueOf(csVolumeId), false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, Constants.BASE_ONTAP_FV_ID, String.valueOf(ontapVolumeUuid), false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, Constants.ONTAP_SNAP_ID, String.valueOf(ontapNewSnapshot), false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, Constants.PRIMARY_POOL_ID, String.valueOf(storagePoolId), false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId, Constants.ONTAP_SNAP_SIZE, String.valueOf(ontapSnapSize), false);
        snapshotDetailsDao.persist(snapshotDetail);
    }

}
