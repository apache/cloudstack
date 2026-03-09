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
import org.apache.cloudstack.storage.feign.client.SnapshotFeignClient;
import org.apache.cloudstack.storage.feign.model.FlexVolSnapshot;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
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
            throw new InvalidParameterValueException("dataObject should not be null");
        }
        if (dataStore == null) {
            throw new InvalidParameterValueException("dataStore should not be null");
        }
        if (callback == null) {
            throw new InvalidParameterValueException("callback should not be null");
        }

        try {
            s_logger.info("Started for data store name [{}] and data object name [{}] of type [{}]",
                    dataStore.getName(), dataObject.getName(), dataObject.getType());

            StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
            if (storagePool == null) {
                s_logger.error("createAsync: Storage Pool not found for id: " + dataStore.getId());
                throw new CloudRuntimeException("Storage Pool not found for id: " + dataStore.getId());
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
                            throw new CloudRuntimeException("Missing LUN name for volume " + volInfo.getId());
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
            throw new CloudRuntimeException("Storage Pool not found for id: " + dataStore.getId());
        }

        StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(details);

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeObject = (VolumeInfo) dataObject;
            CloudStackVolume cloudStackVolumeRequest = Utility.createCloudStackVolumeRequestByProtocol(storagePool, details, volumeObject);
            return storageStrategy.createCloudStackVolume(cloudStackVolumeRequest);
        } else {
            throw new CloudRuntimeException("Unsupported DataObjectType: " + dataObject.getType());
        }
    }

    /**
     * Deletes a volume or snapshot from the ONTAP storage system.
     *
     * <p>For volumes, deletes the backend storage object (LUN for iSCSI, no-op for NFS).
     * For snapshots, deletes the FlexVolume snapshot from ONTAP that was created by takeSnapshot.</p>
     */
    @Override
    public void deleteAsync(DataStore store, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        CommandResult commandResult = new CommandResult();
        try {
            if (store == null || data == null) {
                throw new CloudRuntimeException("store or data is null");
            }

            if (data.getType() == DataObjectType.VOLUME) {
                StoragePoolVO storagePool = storagePoolDao.findById(store.getId());
                if (storagePool == null) {
                    s_logger.error("deleteAsync: Storage Pool not found for id: " + store.getId());
                    throw new CloudRuntimeException("Storage Pool not found for id: " + store.getId());
                }
                Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(store.getId());
                StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(details);
                s_logger.info("createCloudStackVolumeForTypeVolume: Connection to Ontap SVM [{}] successful, preparing CloudStackVolumeRequest", details.get(Constants.SVM_NAME));
                VolumeInfo volumeInfo = (VolumeInfo) data;
                CloudStackVolume cloudStackVolumeRequest = createDeleteCloudStackVolumeRequest(storagePool,details,volumeInfo);
                storageStrategy.deleteCloudStackVolume(cloudStackVolumeRequest);
                s_logger.info("deleteAsync: Volume deleted: " + volumeInfo.getId());
                commandResult.setResult(null);
                commandResult.setSuccess(true);
            } else if (data.getType() == DataObjectType.SNAPSHOT) {
                // Delete the ONTAP FlexVolume snapshot that was created by takeSnapshot
                deleteOntapSnapshot((SnapshotInfo) data, commandResult);
            } else {
                throw new CloudRuntimeException("Unsupported data object type: " + data.getType());
            }
        } catch (Exception e) {
            s_logger.error("deleteAsync: Failed for data object [{}]: {}", data, e.getMessage());
            commandResult.setSuccess(false);
            commandResult.setResult(e.getMessage());
        } finally {
            callback.complete(commandResult);
        }
    }

    /**
     * Deletes an ONTAP FlexVolume snapshot.
     *
     * <p>Retrieves the snapshot details stored during takeSnapshot and calls the ONTAP
     * REST API to delete the FlexVolume snapshot.</p>
     *
     * @param snapshotInfo  The CloudStack snapshot to delete
     * @param commandResult Result object to populate with success/failure
     */
    private void deleteOntapSnapshot(SnapshotInfo snapshotInfo, CommandResult commandResult) {
        long snapshotId = snapshotInfo.getId();
        s_logger.info("deleteOntapSnapshot: Deleting ONTAP FlexVolume snapshot for CloudStack snapshot [{}]", snapshotId);

        try {
            // Retrieve snapshot details stored during takeSnapshot
            String flexVolUuid = getSnapshotDetail(snapshotId, Constants.BASE_ONTAP_FV_ID);
            String ontapSnapshotUuid = getSnapshotDetail(snapshotId, Constants.ONTAP_SNAP_ID);
            String snapshotName = getSnapshotDetail(snapshotId, Constants.ONTAP_SNAP_NAME);
            String poolIdStr = getSnapshotDetail(snapshotId, Constants.PRIMARY_POOL_ID);

            if (flexVolUuid == null || ontapSnapshotUuid == null) {
                s_logger.warn("deleteOntapSnapshot: Missing ONTAP snapshot details for snapshot [{}]. " +
                        "flexVolUuid={}, ontapSnapshotUuid={}. Snapshot may have been created by a different method or already deleted.",
                        snapshotId, flexVolUuid, ontapSnapshotUuid);
                // Consider this a success since there's nothing to delete on ONTAP
                commandResult.setSuccess(true);
                commandResult.setResult(null);
                return;
            }

            long poolId = Long.parseLong(poolIdStr);
            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(poolId);

            StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);
            SnapshotFeignClient snapshotClient = storageStrategy.getSnapshotFeignClient();
            String authHeader = storageStrategy.getAuthHeader();

            s_logger.info("deleteOntapSnapshot: Deleting ONTAP snapshot [{}] (uuid={}) from FlexVol [{}]",
                    snapshotName, ontapSnapshotUuid, flexVolUuid);

            // Call ONTAP REST API to delete the snapshot
            JobResponse jobResponse = snapshotClient.deleteSnapshot(authHeader, flexVolUuid, ontapSnapshotUuid);

            if (jobResponse != null && jobResponse.getJob() != null) {
                // Poll for job completion
                Boolean jobSucceeded = storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 30, 2);
                if (!jobSucceeded) {
                    throw new CloudRuntimeException("Delete job failed for snapshot [" +
                            snapshotName + "] on FlexVol [" + flexVolUuid + "]");
                }
            }

            s_logger.info("deleteOntapSnapshot: Successfully deleted ONTAP snapshot [{}] (uuid={}) for CloudStack snapshot [{}]",
                    snapshotName, ontapSnapshotUuid, snapshotId);

            commandResult.setSuccess(true);
            commandResult.setResult(null);

        } catch (Exception e) {
            // Check if the error indicates snapshot doesn't exist (already deleted)
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("404") || errorMsg.contains("not found") ||
                    errorMsg.contains("does not exist"))) {
                s_logger.warn("deleteOntapSnapshot: ONTAP snapshot for CloudStack snapshot [{}] not found, " +
                        "may have been already deleted. Treating as success.", snapshotId);
                commandResult.setSuccess(true);
                commandResult.setResult(null);
            } else {
                s_logger.error("deleteOntapSnapshot: Failed to delete ONTAP snapshot for CloudStack snapshot [{}]: {}",
                        snapshotId, e.getMessage(), e);
                commandResult.setSuccess(false);
                commandResult.setResult(e.getMessage());
            }
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
                throw new InvalidParameterValueException("dataStore should not be null");
            }
            if (dataObject == null) {
                throw new InvalidParameterValueException("dataObject should not be null");
            }
            if (host == null) {
                throw new InvalidParameterValueException("host should not be null");
            }

            StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
            if (storagePool == null) {
                s_logger.error("grantAccess: Storage Pool not found for id: " + dataStore.getId());
                throw new CloudRuntimeException("Storage Pool not found for id: " + dataStore.getId());
            }
            String storagePoolUuid = dataStore.getUuid();

            // ONTAP managed storage only supports cluster and zone scoped pools
            if (storagePool.getScope() != ScopeType.CLUSTER && storagePool.getScope() != ScopeType.ZONE) {
                s_logger.error("grantAccess: Only Cluster and Zone scoped primary storage is supported for storage Pool: " + storagePool.getName());
                throw new CloudRuntimeException("Only Cluster and Zone scoped primary storage is supported for Storage Pool: " + storagePool.getName());
            }

            if (dataObject.getType() == DataObjectType.VOLUME) {
                VolumeVO volumeVO = volumeDao.findById(dataObject.getId());
                if (volumeVO == null) {
                    s_logger.error("grantAccess: CloudStack Volume not found for id: " + dataObject.getId());
                    throw new CloudRuntimeException("CloudStack Volume not found for id: " + dataObject.getId());
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
                        throw new CloudRuntimeException("Host initiator [" + host.getStorageUrl() +
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
            throw new CloudRuntimeException("Failed with error: " + e.getMessage(), e);
        }
    }

    /**
     * Revokes a host's access to a volume.
     */
    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
        try {
            if (dataStore == null) {
                throw new InvalidParameterValueException("dataStore should not be null");
            }
            if (dataObject == null) {
                throw new InvalidParameterValueException("dataObject should not be null");
            }
            if (host == null) {
                throw new InvalidParameterValueException("host should not be null");
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
                throw new CloudRuntimeException("Storage Pool not found for id: " + dataStore.getId());
            }

            if (storagePool.getScope() != ScopeType.CLUSTER && storagePool.getScope() != ScopeType.ZONE) {
                s_logger.error("revokeAccess: Only Cluster and Zone scoped primary storage is supported for storage Pool: " + storagePool.getName());
                throw new CloudRuntimeException("Only Cluster and Zone scoped primary storage is supported for Storage Pool: " + storagePool.getName());
            }

            if (dataObject.getType() == DataObjectType.VOLUME) {
                VolumeVO volumeVO = volumeDao.findById(dataObject.getId());
                if (volumeVO == null) {
                    s_logger.error("revokeAccess: CloudStack Volume not found for id: " + dataObject.getId());
                    throw new CloudRuntimeException("CloudStack Volume not found for id: " + dataObject.getId());
                }
                revokeAccessForVolume(storagePool, volumeVO, host);
            } else {
                s_logger.error("revokeAccess: Invalid DataObjectType (" + dataObject.getType() + ") passed to revokeAccess");
                throw new CloudRuntimeException("Invalid DataObjectType (" + dataObject.getType() + ") passed to revokeAccess");
            }
        } catch (Exception e) {
            s_logger.error("revokeAccess: Failed for dataObject [{}]: {}", dataObject, e.getMessage());
            throw new CloudRuntimeException("Failed with error: " + e.getMessage(), e);
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

    /**
     * Takes a snapshot by creating an ONTAP FlexVolume-level snapshot.
     *
     * <p>This method creates a point-in-time, space-efficient snapshot of the entire
     * FlexVolume containing the CloudStack volume. FlexVolume snapshots are atomic
     * and capture all files/LUNs within the volume at the moment of creation.</p>
     *
     * <p>Both NFS and iSCSI protocols use the same FlexVolume snapshot approach:
     * <ul>
     *   <li>NFS: The QCOW2 file is captured within the FlexVolume snapshot</li>
     *   <li>iSCSI: The LUN is captured within the FlexVolume snapshot</li>
     * </ul>
     * </p>
     *
     * <p>With {@code STORAGE_SYSTEM_SNAPSHOT=true}, {@code StorageSystemSnapshotStrategy}
     * handles the workflow.</p>
     */
    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        s_logger.info("OntapPrimaryDatastoreDriver.takeSnapshot: Creating FlexVolume snapshot for snapshot [{}]", snapshot.getId());
        CreateCmdResult result;

        try {
            VolumeInfo volumeInfo = snapshot.getBaseVolume();

            VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());
            if (volumeVO == null) {
                throw new CloudRuntimeException("VolumeVO not found for id: " + volumeInfo.getId());
            }

            StoragePoolVO storagePool = storagePoolDao.findById(volumeVO.getPoolId());
            if (storagePool == null) {
                s_logger.error("takeSnapshot: Storage Pool not found for id: {}", volumeVO.getPoolId());
                throw new CloudRuntimeException("Storage Pool not found for id: " + volumeVO.getPoolId());
            }

            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(volumeVO.getPoolId());
            String protocol = poolDetails.get(Constants.PROTOCOL);
            String flexVolUuid = poolDetails.get(Constants.VOLUME_UUID);

            if (flexVolUuid == null || flexVolUuid.isEmpty()) {
                throw new CloudRuntimeException("FlexVolume UUID not found in pool details for pool " + volumeVO.getPoolId());
            }

            StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);
            SnapshotFeignClient snapshotClient = storageStrategy.getSnapshotFeignClient();
            String authHeader = storageStrategy.getAuthHeader();

            SnapshotObjectTO snapshotObjectTo = (SnapshotObjectTO) snapshot.getTO();

            // Build snapshot name using volume name and snapshot UUID
            String snapshotName = buildSnapshotName(volumeInfo.getName(), snapshot.getUuid());

            // Resolve the volume path for storing in snapshot details (for revert operation)
            String volumePath = resolveVolumePathOnOntap(volumeVO, protocol, poolDetails);

            // For iSCSI, retrieve LUN UUID for restore operations
            String lunUuid = null;
            if (ProtocolType.ISCSI.name().equalsIgnoreCase(protocol)) {
                lunUuid = volumeDetailsDao.findDetail(volumeVO.getId(), Constants.LUN_DOT_UUID) != null
                        ? volumeDetailsDao.findDetail(volumeVO.getId(), Constants.LUN_DOT_UUID).getValue()
                        : null;
                if (lunUuid == null) {
                    throw new CloudRuntimeException("LUN UUID not found for iSCSI volume " + volumeVO.getId());
                }
            }

            // Create FlexVolume snapshot via ONTAP REST API
            FlexVolSnapshot snapshotRequest = new FlexVolSnapshot(snapshotName,
                    "CloudStack volume snapshot for volume " + volumeInfo.getName());

            s_logger.info("takeSnapshot: Creating ONTAP FlexVolume snapshot [{}] on FlexVol UUID [{}] for volume [{}]",
                    snapshotName, flexVolUuid, volumeVO.getId());

            JobResponse jobResponse = snapshotClient.createSnapshot(authHeader, flexVolUuid, snapshotRequest);
            if (jobResponse == null || jobResponse.getJob() == null) {
                throw new CloudRuntimeException("Failed to initiate FlexVolume snapshot on FlexVol UUID [" + flexVolUuid + "]");
            }

            // Poll for job completion
            Boolean jobSucceeded = storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 30, 2);
            if (!jobSucceeded) {
                throw new CloudRuntimeException("FlexVolume snapshot job failed on FlexVol UUID [" + flexVolUuid + "]");
            }

            // Retrieve the created snapshot UUID by name
            String ontapSnapshotUuid = resolveSnapshotUuid(snapshotClient, authHeader, flexVolUuid, snapshotName);
            if (ontapSnapshotUuid == null || ontapSnapshotUuid.isEmpty()) {
                throw new CloudRuntimeException("Failed to resolve snapshot UUID for snapshot name [" + snapshotName + "]");
            }

            // Set snapshot path for CloudStack (format: snapshotName for identification)
            snapshotObjectTo.setPath(Constants.ONTAP_SNAP_ID + "=" + ontapSnapshotUuid);

            // Persist snapshot details for revert/delete operations
            updateSnapshotDetails(snapshot.getId(), volumeInfo.getId(), flexVolUuid,
                    ontapSnapshotUuid, snapshotName, volumePath, volumeVO.getPoolId(), protocol, lunUuid);

            CreateObjectAnswer createObjectAnswer = new CreateObjectAnswer(snapshotObjectTo);
            result = new CreateCmdResult(null, createObjectAnswer);
            result.setResult(null);

            s_logger.info("takeSnapshot: Successfully created FlexVolume snapshot [{}] (uuid={}) for volume [{}]",
                    snapshotName, ontapSnapshotUuid, volumeVO.getId());

        } catch (Exception ex) {
            s_logger.error("takeSnapshot: Failed due to ", ex);
            result = new CreateCmdResult(null, new CreateObjectAnswer(ex.toString()));
            result.setResult(ex.toString());
        }

        callback.complete(result);
    }

    /**
     * Resolves the volume path on ONTAP for snapshot restore operations.
     *
     * @param volumeVO    The CloudStack volume
     * @param protocol    Storage protocol (NFS3 or ISCSI)
     * @param poolDetails Pool configuration details
     * @return The ONTAP path (file path for NFS, LUN name for iSCSI)
     */
    private String resolveVolumePathOnOntap(VolumeVO volumeVO, String protocol, Map<String, String> poolDetails) {
        if (ProtocolType.NFS3.name().equalsIgnoreCase(protocol)) {
            // For NFS, use the volume's file path
            return volumeVO.getPath();
        } else if (ProtocolType.ISCSI.name().equalsIgnoreCase(protocol)) {
            // For iSCSI, retrieve the LUN name from volume details
            String lunName = volumeDetailsDao.findDetail(volumeVO.getId(), Constants.LUN_DOT_NAME) != null ?
                    volumeDetailsDao.findDetail(volumeVO.getId(), Constants.LUN_DOT_NAME).getValue() : null;
            if (lunName == null) {
                throw new CloudRuntimeException("No LUN name found for volume " + volumeVO.getId());
            }
            return lunName;
        }
        throw new CloudRuntimeException("Unsupported protocol " + protocol);
    }

    /**
     * Resolves the ONTAP snapshot UUID by querying for the snapshot by name.
     *
     * @param snapshotClient The ONTAP snapshot Feign client
     * @param authHeader     Authorization header
     * @param flexVolUuid    FlexVolume UUID
     * @param snapshotName   Name of the snapshot to find
     * @return The UUID of the snapshot, or null if not found
     */
    private String resolveSnapshotUuid(SnapshotFeignClient snapshotClient, String authHeader,
                                        String flexVolUuid, String snapshotName) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("name", snapshotName);
        queryParams.put("fields", "uuid,name");

        OntapResponse<FlexVolSnapshot> response = snapshotClient.getSnapshots(authHeader, flexVolUuid, queryParams);
        if (response != null && response.getRecords() != null && !response.getRecords().isEmpty()) {
            return response.getRecords().get(0).getUuid();
        }
        return null;
    }

    /**
     * Reverts a volume to a snapshot using protocol-specific ONTAP restore APIs.
     *
     * <p>This method delegates to the appropriate StorageStrategy to restore the
     * specific file (NFS) or LUN (iSCSI) from the FlexVolume snapshot directly
     * via ONTAP REST API, without involving the hypervisor agent.</p>
     *
     * <p><b>Protocol-specific handling (delegated to strategy classes):</b></p>
     * <ul>
     *   <li><b>NFS (UnifiedNASStrategy):</b> Uses the single-file restore API:
     *       {@code POST /api/storage/volumes/{volume_uuid}/snapshots/{snapshot_uuid}/files/{file_path}/restore}
     *       Restores the QCOW2 file from the FlexVolume snapshot to its original location.</li>
     *   <li><b>iSCSI (UnifiedSANStrategy):</b> Uses the LUN restore API:
     *       {@code POST /api/storage/luns/{lun.uuid}/restore}
     *       Restores the LUN data from the snapshot to the specified destination path.</li>
     * </ul>
     */
    @Override
    public void revertSnapshot(SnapshotInfo snapshotOnImageStore, SnapshotInfo snapshotOnPrimaryStore,
                               AsyncCompletionCallback<CommandResult> callback) {
        s_logger.info("OntapPrimaryDatastoreDriver.revertSnapshot: Reverting snapshot [{}]",
                snapshotOnImageStore.getId());

        CommandResult result = new CommandResult();

        try {
            // Use the snapshot that has the ONTAP details stored
            SnapshotInfo snapshot = snapshotOnPrimaryStore != null ? snapshotOnPrimaryStore : snapshotOnImageStore;
            long snapshotId = snapshot.getId();

            // Retrieve snapshot details stored during takeSnapshot
            String flexVolUuid = getSnapshotDetail(snapshotId, Constants.BASE_ONTAP_FV_ID);
            String ontapSnapshotUuid = getSnapshotDetail(snapshotId, Constants.ONTAP_SNAP_ID);
            String snapshotName = getSnapshotDetail(snapshotId, Constants.ONTAP_SNAP_NAME);
            String volumePath = getSnapshotDetail(snapshotId, Constants.VOLUME_PATH);
            String poolIdStr = getSnapshotDetail(snapshotId, Constants.PRIMARY_POOL_ID);
            String protocol = getSnapshotDetail(snapshotId, Constants.PROTOCOL);

            if (flexVolUuid == null || snapshotName == null || volumePath == null || poolIdStr == null) {
                throw new CloudRuntimeException("Missing required snapshot details for snapshot " + snapshotId +
                        " (flexVolUuid=" + flexVolUuid + ", snapshotName=" + snapshotName +
                        ", volumePath=" + volumePath + ", poolId=" + poolIdStr + ")");
            }

            long poolId = Long.parseLong(poolIdStr);
            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(poolId);

            StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);

            // Get the FlexVolume name (required for CLI-based restore API for all protocols)
            String flexVolName = poolDetails.get(Constants.VOLUME_NAME);
            if (flexVolName == null || flexVolName.isEmpty()) {
                throw new CloudRuntimeException("FlexVolume name not found in pool details for pool " + poolId);
            }

            // Prepare protocol-specific parameters (lunUuid is only needed for backward compatibility)
            String lunUuid = null;
            if (ProtocolType.ISCSI.name().equalsIgnoreCase(protocol)) {
                lunUuid = getSnapshotDetail(snapshotId, Constants.LUN_DOT_UUID);
            }

            // Delegate to strategy class for protocol-specific restore
            JobResponse jobResponse = storageStrategy.revertSnapshotForCloudStackVolume(
                    snapshotName, flexVolUuid, ontapSnapshotUuid, volumePath, lunUuid, flexVolName);

            if (jobResponse == null || jobResponse.getJob() == null) {
                throw new CloudRuntimeException("Failed to initiate restore from snapshot [" +
                        snapshotName + "]");
            }

            // Poll for job completion (use longer timeout for large LUNs/files)
            Boolean jobSucceeded = storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 60, 2);
            if (!jobSucceeded) {
                throw new CloudRuntimeException("Restore job failed for snapshot [" +
                        snapshotName + "]");
            }

            s_logger.info("revertSnapshot: Successfully restored {} [{}] from snapshot [{}]",
                    ProtocolType.ISCSI.name().equalsIgnoreCase(protocol) ? "LUN" : "file",
                    volumePath, snapshotName);

            result.setResult(null); // Success

        } catch (Exception ex) {
            s_logger.error("revertSnapshot: Failed to revert snapshot {}", snapshotOnImageStore, ex);
            result.setResult(ex.toString());
        }

        callback.complete(result);
    }

    /**
     * Retrieves a snapshot detail value by key.
     *
     * @param snapshotId The CloudStack snapshot ID
     * @param key        The detail key
     * @return The detail value, or null if not found
     */
    private String getSnapshotDetail(long snapshotId, String key) {
        SnapshotDetailsVO detail = snapshotDetailsDao.findDetail(snapshotId, key);
        return detail != null ? detail.getValue() : null;
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
                    throw new CloudRuntimeException("Missing LUN name for volume " + volumeInfo.getId());
                }
                cloudStackVolumeDeleteRequest = new CloudStackVolume();
                Lun lun = new Lun();
                lun.setName(lunName);
                lun.setUuid(lunUUID);
                cloudStackVolumeDeleteRequest.setLun(lun);
                break;
            default:
                throw new CloudRuntimeException("Unsupported protocol " + protocol);

        }
        return cloudStackVolumeDeleteRequest;

    }

    // ──────────────────────────────────────────────────────────────────────────
    // Snapshot Helper Methods
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds a snapshot name with proper length constraints.
     * Format: {@code <volumeName>-<snapshotUuid>}
     */
    private String buildSnapshotName(String volumeName, String snapshotUuid) {
        String name = volumeName + "-" + snapshotUuid;
        int maxLength = Constants.MAX_SNAPSHOT_NAME_LENGTH;
        int trimRequired = name.length() - maxLength;

        if (trimRequired > 0) {
            name = StringUtils.left(volumeName, volumeName.length() - trimRequired) + "-" + snapshotUuid;
        }
        return name;
    }

    /**
     * Persists snapshot metadata in snapshot_details table.
     *
     * @param csSnapshotId      CloudStack snapshot ID
     * @param csVolumeId        Source CloudStack volume ID
     * @param flexVolUuid       ONTAP FlexVolume UUID
     * @param ontapSnapshotUuid ONTAP FlexVolume snapshot UUID
     * @param snapshotName      ONTAP snapshot name
     * @param volumePath        Path of the volume file/LUN within the FlexVolume (for restore)
     * @param storagePoolId     Primary storage pool ID
     * @param protocol          Storage protocol (NFS3 or ISCSI)
     * @param lunUuid           LUN UUID (only for iSCSI, null for NFS)
     */
    private void updateSnapshotDetails(long csSnapshotId, long csVolumeId, String flexVolUuid,
                                        String ontapSnapshotUuid, String snapshotName,
                                        String volumePath, long storagePoolId, String protocol,
                                        String lunUuid) {
        SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(csSnapshotId,
                Constants.SRC_CS_VOLUME_ID, String.valueOf(csVolumeId), false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId,
                Constants.BASE_ONTAP_FV_ID, flexVolUuid, false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId,
                Constants.ONTAP_SNAP_ID, ontapSnapshotUuid, false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId,
                Constants.ONTAP_SNAP_NAME, snapshotName, false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId,
                Constants.VOLUME_PATH, volumePath, false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId,
                Constants.PRIMARY_POOL_ID, String.valueOf(storagePoolId), false);
        snapshotDetailsDao.persist(snapshotDetail);

        snapshotDetail = new SnapshotDetailsVO(csSnapshotId,
                Constants.PROTOCOL, protocol, false);
        snapshotDetailsDao.persist(snapshotDetail);

        // Store LUN UUID for iSCSI volumes (required for LUN restore API)
        if (lunUuid != null && !lunUuid.isEmpty()) {
            snapshotDetail = new SnapshotDetailsVO(csSnapshotId,
                    Constants.LUN_DOT_UUID, lunUuid, false);
            snapshotDetailsDao.persist(snapshotDetail);
        }
    }

}
