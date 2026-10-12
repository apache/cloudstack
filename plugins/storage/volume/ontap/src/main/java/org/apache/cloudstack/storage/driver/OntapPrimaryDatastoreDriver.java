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

import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.client.SnapshotFeignClient;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.FlexVolSnapshot;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.LunSpace;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.VolumeQosPolicy;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.SANStrategy;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.UnifiedNASStrategy;
import org.apache.cloudstack.storage.service.UnifiedSANStrategy;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Primary datastore driver for NetApp ONTAP storage systems.
 * Handles volume lifecycle operations for iSCSI and NFS protocols.
 */
public class OntapPrimaryDatastoreDriver implements PrimaryDataStoreDriver {

    private static final Logger logger = LogManager.getLogger(OntapPrimaryDatastoreDriver.class);

    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private VolumeDao volumeDao;
    @Inject private VolumeDetailsDao volumeDetailsDao;
    @Inject private SnapshotDetailsDao snapshotDetailsDao;
    @Inject private SnapshotDao snapshotDao;
    @Inject private VMTemplatePoolDao vmTemplatePoolDao;

    @Override
    public Map<String, String> getCapabilities() {
        logger.trace("OntapPrimaryDatastoreDriver: getCapabilities: Called");
        Map<String, String> mapCapabilities = new HashMap<>();
        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString(), Boolean.TRUE.toString());
        // Enables the framework to cache a template on the FlexVolume once and serve every later
        // deployment with an array-side clone instead of another copy from secondary storage.
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString(), Boolean.TRUE.toString());
        // createTemplate(snapshotid) reads the snapshot from this pool instead of backing it up to secondary first.
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_TEMPLATE_FROM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        // Must be present and false: StorageSystemDataMotionStrategy then clones the snapshot into a
        // temporary volume (see copyAsync) and copies that volume to secondary storage.
        mapCapabilities.put(OntapStorageConstants.CAN_DIRECT_ATTACH_SNAPSHOT, Boolean.FALSE.toString());
        return mapCapabilities;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) { return null; }

    @Override
    public boolean volumesRequireGrantAccessWhenUsed() {
        logger.trace("volumesRequireGrantAccessWhenUsed invoked");
        return true;
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
            logger.info("Started for data store name [{}] and data object name [{}] of type [{}]",
                    dataStore.getName(), dataObject.getName(), dataObject.getType());

            StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
            if (storagePool == null) {
                logger.error("createAsync: Storage Pool not found for id: " + dataStore.getId());
                throw new CloudRuntimeException("Storage Pool not found for id: " + dataStore.getId());
            }

            Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(dataStore.getId());
            validateProtocol(details, dataStore);

            if (dataObject.getType() == DataObjectType.VOLUME) {
                VolumeInfo volInfo = (VolumeInfo) dataObject;

                // Update CloudStack volume record with storage pool association and protocol-specific details
                VolumeVO volumeVO = volumeDao.findById(volInfo.getId());
                if (volumeVO != null) {
                    /*
                     * Create-volume combinations on ONTAP primary (v1):
                     *
                     * 1) cloneOfSnapshot — StorageSystemDataMotionStrategy sets volume_details.cloneOfSnapshot
                     *    when createVolume(snapshotid) targets a managed backend snapshot.
                     *    - Same primary pool / FlexVol only (PRIMARY_POOL_ID must match dataStore).
                     *    - DATA or ROOT snapshot → new attachable data volume (ROOT is never bootable
                     *      via this path; bootable recovery is createTemplate(snapshotid) → deploy).
                     *    - Backend: iSCSI → POST /api/storage/luns (clone.source in .snapshot/);
                     *      NFS → POST /api/storage/file/clone with snapshot.name.
                     *    - IOPS: MIN_IOPS/MAX_IOPS may be on snapshot_details; apply is TODO below.
                     *
                     * 2) cloneOfTemplate — deploy / create from cached template on this pool.
                     *
                     * 3) else — blank LUN (iSCSI) or qcow2 (NFS).
                     *
                     * Mutually exclusive from the motion/orchestrator layer; snapshot is checked first
                     * (SolidFire-style) so a create-from-snapshot never accidentally falls through to blank create.
                     */
                    Long cloneOfSnapshotId = getSnapshotIdForCloning(volInfo.getId());
                    Long cloneOfTemplateId = getTemplateIdForCloning(volInfo.getId());
                    CloudStackVolume clonedCloudStackVolume;
                    if (cloneOfSnapshotId != null) {
                        clonedCloudStackVolume = cloneCloudStackVolumeFromSnapshot(
                                storagePool, volInfo, details, cloneOfSnapshotId);
                        // TODO(CSTACKEX-306): apply persisted MIN_IOPS / MAX_IOPS from snapshot_details
                        // onto this CloudStack volume (and ONTAP QoS if applicable) after successful clone.
                    } else if (cloneOfTemplateId != null) {
                        clonedCloudStackVolume = cloneCloudStackVolumeFromTemplate(
                                storagePool, volInfo, details, cloneOfTemplateId);
                    } else {
                        clonedCloudStackVolume = createCloudStackVolume(storagePool, volInfo, details);
                    }

                    createCmdResult = recordCreatedVolume(storagePool, volInfo, volumeVO, details, clonedCloudStackVolume);
                }
            } else if (dataObject.getType() == DataObjectType.TEMPLATE) {
                createCmdResult = createTemplateOnPrimary(storagePool, (TemplateInfo) dataObject, details);
            } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
                logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (Exception e) {
            errMsg = e.getMessage();
            logger.error("createAsync: Failed for dataObject name [{}]: {}", dataObject.getName(), errMsg);
            createCmdResult = new CreateCmdResult(null, new Answer(null, false, errMsg));
            createCmdResult.setResult(errMsg);
        } finally {
            if (createCmdResult != null && createCmdResult.isSuccess()) {
                logger.info("createAsync: Operation completed successfully for {}", dataObject.getType());
            }
            callback.complete(createCmdResult);
        }
    }

    /**
     * Records pool association, image format and protocol-specific identity (LUN name/uuid for iSCSI)
     * of a volume that was just created or cloned on ONTAP, and returns the create result whose path
     * the framework stores on the volume.
     */
    private CreateCmdResult recordCreatedVolume(StoragePoolVO storagePool, VolumeInfo volInfo, VolumeVO volumeVO,
                                                Map<String, String> details, CloudStackVolume createdCloudStackVolume) {
        CreateCmdResult createCmdResult = null;
        volumeVO.setPoolType(storagePool.getPoolType());
        volumeVO.setPoolId(storagePool.getId());
        volumeVO.setFormat(getImageFormat(storagePool));
        logger.info("createAsync: Volume format set to [{}] for pool type [{}]", volumeVO.getFormat(), storagePool.getPoolType());

        if (ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
            // createCloudStackVolume validates the Feign response (LUN name + uuid) before returning
            Lun createdLun = createdCloudStackVolume.getLun();
            String lunName = createdLun.getName();

            // Persist LUN details for future operations (delete, grant/revoke access)
            volumeDetailsDao.addDetail(volInfo.getId(), OntapStorageConstants.LUN_DOT_UUID, createdLun.getUuid(), false);
            volumeDetailsDao.addDetail(volInfo.getId(), OntapStorageConstants.LUN_DOT_NAME, lunName, false);
            volumeVO.setFolder(createdLun.getUuid());

            logger.info("createAsync: Created LUN [{}] for volume [{}]. LUN mapping will occur during grantAccess() to per-host igroup.",
                    lunName, volumeVO.getId());
            createCmdResult = new CreateCmdResult(lunName, new Answer(null, true, null));
        } else if (ProtocolType.NFS3.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
            createCmdResult = new CreateCmdResult(volInfo.getUuid(), new Answer(null, true, null));
            logger.info("createAsync: Managed NFS volume [{}] with path [{}] associated with pool {}",
                    volumeVO.getId(), volInfo.getUuid(), storagePool.getId());
        }
        volumeDao.update(volumeVO.getId(), volumeVO);
        return createCmdResult;
    }

    /**
     * Creates a volume on the ONTAP backend.
     */
    private CloudStackVolume createCloudStackVolume(StoragePoolVO storagePool, VolumeInfo volumeObject, Map<String, String> details) {
        verifySufficientIopsForStoragePool(storagePool, volumeObject.getMinIops(), volumeObject.getId());
        StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);
        VolumeQosPolicy qosPolicy = null;
        Volume.Type volumeType = volumeObject.getVolumeType();
        if (volumeType == Volume.Type.DATADISK || volumeType == Volume.Type.ROOT) {
            qosPolicy = createQosPolicyIfNeeded(storageStrategy, details,
                    volumeObject.getMinIops(), volumeObject.getMaxIops(), storagePool.getId());
        }
        CloudStackVolume request = createCloudStackVolumeRequestByProtocol(
                storagePool, details, volumeObject, qosPolicy);
        try {
            CloudStackVolume created = storageStrategy.createCloudStackVolume(request);
            persistQosPolicyDetails(volumeObject.getId(), qosPolicy);
            return created;
        } catch (RuntimeException e) {
            if (qosPolicy != null) {
                storageStrategy.deleteVolumeQosPolicy(qosPolicy.getUuid());
            }
            throw e;
        }
    }

    private VolumeQosPolicy createQosPolicyIfNeeded(StorageStrategy storageStrategy, Map<String, String> details,
                                                    Long minIops, Long maxIops, Long poolId) {
        if (!validateIops(storageStrategy, details, poolId, minIops, maxIops)) {
            return null;
        }
        String policyName = getQosPolicyName(details.get(OntapStorageConstants.SVM_NAME), minIops, maxIops);
        return storageStrategy.createVolumeQosPolicy(policyName, minIops, maxIops);
    }

    private boolean validateIops(StorageStrategy storageStrategy, Map<String, String> details,
                                 Long poolId, Long minIops, Long maxIops) {
        long min = minIops == null ? 0 : minIops;
        long max = maxIops == null ? 0 : maxIops;
        if (min <= 0 && max <= 0) {
            return false;
        }
        if (min > 0 && max > 0 && min > max) {
            throw new CloudRuntimeException("Minimum IOPS cannot be greater than maximum IOPS");
        }
        if (min > 0) {
            String isAff = details.get(OntapStorageConstants.IS_AFF);
            if (StringUtils.isBlank(isAff)) {
                try {
                    isAff = Boolean.toString(storageStrategy.isAff());
                    details.put(OntapStorageConstants.IS_AFF, isAff);
                    storagePoolDetailsDao.addDetail(poolId, OntapStorageConstants.IS_AFF, isAff, false);
                } catch (Exception e) {
                    logger.error("Unable to determine whether storage pool [{}] is AFF or FAS: {}",
                            poolId, e.getMessage());
                    return true;
                }
            }
            if (!Boolean.parseBoolean(isAff)) {
                throw new CloudRuntimeException(OntapStorageConstants.MIN_IOPS_NOT_SUPPORTED_ON_FAS);
            }
        }
        return true;
    }

    /**
     * Builds a reusable SVM-scoped QoS policy name: cs_{min}_to_{max}_iops_{svmName}.
     * Dots in the SVM name are replaced with underscores (ONTAP QoS names cannot contain '.').
     */
    private String getQosPolicyName(String svmName, Long minIops, Long maxIops) {
        String sanitizedSvmName = svmName == null ? "" : svmName.replace(".", OntapStorageConstants.UNDERSCORE);
        long min = minIops != null && minIops > 0 ? minIops : 0;
        long max = maxIops != null && maxIops > 0 ? maxIops : 0;
        return OntapStorageConstants.QOS_POLICY_NAME_PREFIX + min + OntapStorageConstants.UNDERSCORE
                + OntapStorageConstants.QOS_POLICY_NAME_TO + max + OntapStorageConstants.UNDERSCORE
                + OntapStorageConstants.QOS_POLICY_NAME_IOPS + sanitizedSvmName;
    }

    private void persistQosPolicyDetails(long volumeId, VolumeQosPolicy qosPolicy) {
        volumeDetailsDao.removeDetail(volumeId, OntapStorageConstants.QOS_POLICY_UUID);
        if (qosPolicy == null) {
            return;
        }
        volumeDetailsDao.addDetail(volumeId, OntapStorageConstants.QOS_POLICY_UUID, qosPolicy.getUuid(), false);
    }

    /**
     * Creates the backend object that caches a template on this pool's FlexVolume.
     *
     * <p>Protocol-specific work is delegated to {@link StorageStrategy#createTemplateCache}.
     * This method maps the result to {@link CreateCmdResult} and records SAN identity on
     * {@code template_spool_ref} ({@code local_download_path} = LUN uuid).</p>
     */
    private CreateCmdResult createTemplateOnPrimary(StoragePoolVO storagePool, TemplateInfo templateInfo, Map<String, String> details) {
        StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);
        long sizeInBytes = getDataObjectSizeIncludingHypervisorSnapshotReserve(templateInfo, storagePool);
        CloudStackVolume created = null;
        try {
            created = storageStrategy.createTemplateCache(storagePool, templateInfo, details, sizeInBytes);
            String path = recordTemplateCacheOnSpoolRef(storagePool, templateInfo, created, sizeInBytes);
            return new CreateCmdResult(path, new Answer(null, true, null));
        } catch (Exception e) {
            // Compensating delete for SAN: strategy cleans create-time LUN failures; this covers
            // post-create failures (e.g. template_spool_ref update) after a LUN was returned.
            if (created != null && created.getLun() != null) {
                bestEffortDeleteTemplateCacheLun(storageStrategy, details.get(OntapStorageConstants.SVM_NAME),
                        created.getLun().getName(), created.getLun().getUuid());
            }
            if (e instanceof CloudRuntimeException) {
                throw (CloudRuntimeException) e;
            }
            throw new CloudRuntimeException("Failed to create template cache for template [" + templateInfo.getId()
                    + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Persists SAN cache identity on {@code template_spool_ref} when a LUN was created.
     * NFS returns the template uuid as the create path; {@code install_path} is filled later.
     */
    private String recordTemplateCacheOnSpoolRef(StoragePoolVO storagePool, TemplateInfo templateInfo,
                                                 CloudStackVolume created, long sizeInBytes) {
        if (created == null || created.getLun() == null) {
            return templateInfo.getUuid();
        }
        Lun lun = created.getLun();
        VMTemplateStoragePoolVO templatePoolRef = findTemplatePoolRef(storagePool.getId(), templateInfo.getId());
        templatePoolRef.setLocalDownloadPath(lun.getUuid());
        templatePoolRef.setTemplateSize(sizeInBytes);
        vmTemplatePoolDao.update(templatePoolRef.getId(), templatePoolRef);
        return lun.getName();
    }

    /**
     * Clones the cached template into a new volume on the same FlexVolume.
     *
     * <p>Invoked when {@code StorageSystemDataMotionStrategy} has recorded a
     * {@code cloneOfTemplate} detail on the volume.</p>
     */
    private CloudStackVolume cloneCloudStackVolumeFromTemplate(StoragePoolVO storagePool, VolumeInfo volumeInfo,
                                                               Map<String, String> details, long templateId) {
        VMTemplateStoragePoolVO templatePoolRef = findTemplatePoolRef(storagePool.getId(), templateId);
        StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);
        boolean iscsi = isIscsi(details);

        verifySufficientIopsForStoragePool(storagePool, volumeInfo.getMinIops(), volumeInfo.getId());
        VolumeQosPolicy qosPolicy = null;
        Volume.Type volumeType = volumeInfo.getVolumeType();
        if (volumeType == Volume.Type.DATADISK || volumeType == Volume.Type.ROOT) {
            qosPolicy = createQosPolicyIfNeeded(storageStrategy, details,
                    volumeInfo.getMinIops(), volumeInfo.getMaxIops(), storagePool.getId());
        }

        CloudStackVolume request = iscsi
                ? createCloneLunRequest(storagePool, details, volumeInfo, templatePoolRef, templateId, qosPolicy)
                : createCloneFileRequest(storagePool, volumeInfo, templatePoolRef, templateId);

        try {
            CloudStackVolume cloned = storageStrategy.cloneCloudStackVolume(request);
            if (cloned == null) {
                throw new CloudRuntimeException("ONTAP returned nothing when cloning template [" + templateId
                        + "] for volume [" + volumeInfo.getId() + "]");
            }

            logger.info("cloneCloudStackVolumeFromTemplate: Cloned template [{}] for volume [{}] on pool [{}]",
                    templateId, volumeInfo.getId(), storagePool.getId());

            long requestedSize = getDataObjectSizeIncludingHypervisorSnapshotReserve(volumeInfo, storagePool);
            if (requestedSize > templatePoolRef.getTemplateSize()) {
                logger.info("cloneCloudStackVolumeFromTemplate: Growing clone of template [{}] from {} to {} bytes for volume [{}]",
                        templateId, templatePoolRef.getTemplateSize(), requestedSize, volumeInfo.getId());
                resizeClonedVolumeOrCleanup(storageStrategy, cloned, requestedSize,
                        "cloneCloudStackVolumeFromTemplate template [" + templateId + "] volume [" + volumeInfo.getId() + "]");
            }

            if (!iscsi && qosPolicy != null) {
                CloudStackVolume cloudStackVolume = createCloudStackVolumeRequestByProtocol(
                        storagePool, details, volumeInfo, qosPolicy);
                storageStrategy.updateCloudStackVolume(cloudStackVolume);
            }
            persistQosPolicyDetails(volumeInfo.getId(), qosPolicy);
            return cloned;
        } catch (Exception e) {
            if (qosPolicy != null) {
                storageStrategy.deleteVolumeQosPolicy(qosPolicy.getUuid());
            }
            throw e;
        }
    }

    /**
     * Returns the CloudStack template id the volume should be cloned from, or null for a blank volume.
     *
     * <p>{@code StorageSystemDataMotionStrategy} persists this detail immediately before calling
     * {@code createAsync} and removes it right after, so it is only visible during creation.</p>
     */
    private Long getTemplateIdForCloning(long volumeId) {
        VolumeDetailVO detail = volumeDetailsDao.findDetail(volumeId, OntapStorageConstants.CLONE_OF_TEMPLATE);
        if (detail == null || detail.getValue() == null || detail.getValue().isEmpty()) {
            return null;
        }
        return Long.valueOf(detail.getValue());
    }

    /**
     * Returns the CloudStack snapshot id to clone from when {@code volume_details.cloneOfSnapshot}
     * is set, or null when this create is not a create-volume-from-snapshot.
     *
     * <p>Set by {@code StorageSystemDataMotionStrategy.handleCreateManagedVolumeFromManagedSnapshot}
     * for the duration of {@code createAsync} only (same pattern as {@link #getTemplateIdForCloning}).</p>
     */
    private Long getSnapshotIdForCloning(long volumeId) {
        VolumeDetailVO detail = volumeDetailsDao.findDetail(volumeId, OntapStorageConstants.CLONE_OF_SNAPSHOT);
        if (detail == null || detail.getValue() == null || detail.getValue().isEmpty()) {
            return null;
        }
        return Long.valueOf(detail.getValue());
    }

    /**
     * Creates a new volume on this pool by cloning a file/LUN from a CloudStack volume snapshot
     * that already lives on the same FlexVolume.
     *
     * <p><b>Combinations (product + plugin v1):</b></p>
     * <ul>
     *   <li><b>Same pool only</b> — {@code snapshot_details.PRIMARY_POOL_ID} must equal this
     *       {@code storagePool}. Creating the volume on a different pool is descoped; use migrate later if needed.</li>
     *   <li><b>DATA snapshot</b> → attachable data disk (disk offering usually inherited).</li>
     *   <li><b>ROOT snapshot</b> → still a data disk here (not bootable). Bootable path remains
     *       {@code createTemplate(snapshotid)} then deploy.</li>
     *   <li><b>iSCSI</b> — {@code POST /api/storage/luns} with
     *       {@code clone.source.name=/vol/&lt;fv&gt;/.snapshot/&lt;snap&gt;/&lt;lun&gt;}</li>
     *   <li><b>NFS3</b> — {@code POST /api/storage/file/clone} with {@code snapshot.name}</li>
     *   <li><b>Not this path</b> — in-place revert ({@code revertSnapshot}); VM/instance snapshots;
     *       secondary-storage archive restore.</li>
     * </ul>
     *
     * <p>Optional grow when the disk offering is larger than the snapshot size (same pattern as
     * clone-from-template).</p>
     */
    private CloudStackVolume cloneCloudStackVolumeFromSnapshot(StoragePoolVO storagePool, VolumeInfo volumeInfo,
                                                               Map<String, String> details, long csSnapshotId) {
        String snapshotName = requireSnapshotDetail(csSnapshotId, OntapStorageConstants.ONTAP_SNAP_NAME);
        String volumePath = requireSnapshotDetail(csSnapshotId, OntapStorageConstants.VOLUME_PATH);
        String primaryPoolId = requireSnapshotDetail(csSnapshotId, OntapStorageConstants.PRIMARY_POOL_ID);
        String snapProtocol = requireSnapshotDetail(csSnapshotId, OntapStorageConstants.PROTOCOL);

        // Same-pool / same-protocol gate (v1). Fail before any ONTAP call.
        if (!String.valueOf(storagePool.getId()).equals(primaryPoolId)) {
            throw new CloudRuntimeException("Create volume from snapshot [" + csSnapshotId
                    + "] requires the snapshot's primary pool [" + primaryPoolId
                    + "]; requested pool is [" + storagePool.getId() + "] (creating a volume from a snapshot on a different pool is not supported)");
        }
        String poolProtocol = details.get(OntapStorageConstants.PROTOCOL);
        if (poolProtocol == null || !poolProtocol.equalsIgnoreCase(snapProtocol)) {
            throw new CloudRuntimeException("Create volume from snapshot [" + csSnapshotId
                    + "] protocol mismatch: snapshot=[" + snapProtocol + "], pool=[" + poolProtocol + "]");
        }

        StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);

        logger.info("cloneCloudStackVolumeFromSnapshot: Cloning from CS snapshot [{}] (ONTAP snap [{}], path [{}]) "
                        + "for volume [{}] on pool [{}] protocol [{}]",
                csSnapshotId, snapshotName, volumePath, volumeInfo.getId(), storagePool.getId(), poolProtocol);

        CloudStackVolume request = isIscsi(details)
                ? createCloneLunFromSnapshotRequest(storagePool, details, volumeInfo, volumePath, snapshotName)
                : createCloneFileFromSnapshotRequest(storagePool, details, volumeInfo, volumePath, snapshotName);
        CloudStackVolume cloned = storageStrategy.cloneCloudStackVolume(request);
        if (cloned == null) {
            throw new CloudRuntimeException("ONTAP returned nothing when cloning snapshot [" + csSnapshotId
                    + "] for volume [" + volumeInfo.getId() + "]");
        }

        long requestedSize = getDataObjectSizeIncludingHypervisorSnapshotReserve(volumeInfo, storagePool);
        long snapshotSize = resolveSnapshotSizeBytes(csSnapshotId);
        if (snapshotSize > 0 && requestedSize > snapshotSize) {
            logger.info("cloneCloudStackVolumeFromSnapshot: Growing clone of snapshot [{}] from {} to {} bytes for volume [{}]",
                    csSnapshotId, snapshotSize, requestedSize, volumeInfo.getId());
            resizeClonedVolumeOrCleanup(storageStrategy, cloned, requestedSize,
                    "cloneCloudStackVolumeFromSnapshot snapshot [" + csSnapshotId + "] volume [" + volumeInfo.getId() + "]");
        }

        return cloned;
    }

    /**
     * Grows a just-created ONTAP clone to the requested size. If resize fails, deletes the clone
     * so the CloudStack create-failure path does not leave an orphaned LUN/file on the array.
     */
    private void resizeClonedVolumeOrCleanup(StorageStrategy storageStrategy, CloudStackVolume cloned,
                                             long requestedSize, String context) {
        try {
            storageStrategy.resizeCloudStackVolume(cloned, requestedSize);
        } catch (RuntimeException resizeEx) {
            logger.error("{}: Resize to {} bytes failed; deleting orphaned ONTAP clone: {}",
                    context, requestedSize, resizeEx.getMessage());
            bestEffortDeleteOrphanedClone(storageStrategy, cloned);
            throw resizeEx;
        }
    }

    private void bestEffortDeleteOrphanedClone(StorageStrategy storageStrategy, CloudStackVolume cloned) {
        try {
            storageStrategy.deleteCloudStackVolume(cloned);
            logger.info("Deleted orphaned ONTAP clone after resize failure");
        } catch (Exception cleanupEx) {
            logger.warn("Failed to delete orphaned ONTAP clone after resize failure: {}", cleanupEx.getMessage());
        }
    }

    private String requireSnapshotDetail(long csSnapshotId, String key) {
        String value = getSnapshotDetail(csSnapshotId, key);
        if (value == null || value.isEmpty()) {
            throw new CloudRuntimeException("Missing snapshot_details [" + key + "] for snapshot [" + csSnapshotId
                    + "]; cannot create volume from snapshot");
        }
        return value;
    }

    private long resolveSnapshotSizeBytes(long csSnapshotId) {
        SnapshotVO snapshotVO = snapshotDao.findById(csSnapshotId);
        if (snapshotVO == null || snapshotVO.getSize() <= 0) {
            return 0L;
        }
        return snapshotVO.getSize();
    }

    private VMTemplateStoragePoolVO findTemplatePoolRef(long poolId, long templateId) {
        VMTemplateStoragePoolVO templatePoolRef = vmTemplatePoolDao.findByPoolTemplate(poolId, templateId, null);
        if (templatePoolRef == null) {
            throw new CloudRuntimeException("No template_spool_ref row for template [" + templateId + "] on pool [" + poolId + "]");
        }
        return templatePoolRef;
    }

    /**
     * Deletes the LUN caching a template on this pool, invoked by template eviction
     * ({@code TemplateManagerImpl.evictTemplateFromStoragePool}).
     *
     * <p>Volumes previously cloned from this LUN are unaffected: an ONTAP sis-clone shares blocks
     * with its source through reference counting rather than depending on it, so the source can be
     * removed while its clones stay online.</p>
     *
     * <p>{@code deleteCloudStackVolume} already unmaps as it deletes ({@code allow_delete_while_mapped})
     * and treats a missing LUN as success.</p>
     */
    private void deleteTemplateOnPrimary(DataStore store, TemplateInfo templateInfo) {
        StoragePoolVO storagePool = storagePoolDao.findById(store.getId());
        if (storagePool == null) {
            throw new CloudRuntimeException("Storage Pool not found for id: " + store.getId());
        }

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(store.getId());
        VMTemplateStoragePoolVO templatePoolRef = vmTemplatePoolDao.findByPoolTemplate(storagePool.getId(), templateInfo.getId(), null);
        if (templatePoolRef == null) {
            logger.warn("deleteTemplateOnPrimary: No template_spool_ref for template [{}] on pool [{}]; nothing to delete",
                    templateInfo.getId(), storagePool.getId());
            return;
        }

        StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);
        if (isIscsi(details)) {
            deleteIscsiTemplateCache(storagePool, templateInfo, templatePoolRef, storageStrategy, details);
        } else {
            deleteNfsTemplateCache(details, templateInfo, templatePoolRef, storageStrategy);
        }
    }

    private void deleteIscsiTemplateCache(StoragePoolVO storagePool, TemplateInfo templateInfo,
                                          VMTemplateStoragePoolVO templatePoolRef, StorageStrategy storageStrategy,
                                          Map<String, String> details) {
        String lunName = getTemplateLunName(storagePool, templateInfo.getId());
        String lunUuid = templatePoolRef.getLocalDownloadPath();
        deleteTemplateCacheLun(storageStrategy, details.get(OntapStorageConstants.SVM_NAME), lunName, lunUuid);
        logger.info("deleteIscsiTemplateCache: Deleted template cache LUN [{}] for template [{}] on pool [{}]",
                lunName, templateInfo.getId(), storagePool.getId());
    }

    /**
     * Deletes a template-cache LUN. When {@code lunUuid} is missing, resolves the LUN by its
     * deterministic name so eviction and create-time rollback still work.
     */
    private void deleteTemplateCacheLun(StorageStrategy storageStrategy, String svmName, String lunName, String lunUuid) {
        String uuid = lunUuid;
        if (uuid == null || uuid.isEmpty()) {
            CloudStackVolume existing = getCloudStackVolumeByName(storageStrategy, svmName, lunName);
            if (existing == null || existing.getLun() == null || existing.getLun().getUuid() == null) {
                logger.warn("deleteTemplateCacheLun: LUN [{}] not found on SVM [{}]; nothing to delete", lunName, svmName);
                return;
            }
            uuid = existing.getLun().getUuid();
        }

        Lun lun = new Lun();
        lun.setUuid(uuid);
        lun.setName(lunName);

        CloudStackVolume deleteRequest = new CloudStackVolume();
        deleteRequest.setLun(lun);
        storageStrategy.deleteCloudStackVolume(deleteRequest);
    }

    /**
     * Best-effort cleanup of a template-cache LUN after a failed create. Never masks the original
     * create failure.
     */
    private void bestEffortDeleteTemplateCacheLun(StorageStrategy storageStrategy, String svmName, String lunName,
                                                  String lunUuid) {
        try {
            deleteTemplateCacheLun(storageStrategy, svmName, lunName, lunUuid);
            logger.info("bestEffortDeleteTemplateCacheLun: Removed leftover template cache LUN [{}] on SVM [{}]",
                    lunName, svmName);
        } catch (Exception cleanupEx) {
            logger.warn("bestEffortDeleteTemplateCacheLun: Failed to remove leftover template cache LUN [{}] on SVM [{}]: {}",
                    lunName, svmName, cleanupEx.getMessage());
        }
    }

    private void deleteNfsTemplateCache(Map<String, String> details, TemplateInfo templateInfo,
                                        VMTemplateStoragePoolVO templatePoolRef, StorageStrategy storageStrategy) {
        String filePath = templatePoolRef.getInstallPath();
        if (filePath == null || filePath.isEmpty()) {
            logger.warn("deleteNfsTemplateCache: No install_path recorded for template [{}]; nothing to delete",
                    templateInfo.getId());
            return;
        }
        String flexVolUuid = details.get(OntapStorageConstants.VOLUME_UUID);
        if (flexVolUuid == null || flexVolUuid.isEmpty()) {
            // Misconfigured pool detail — fail eviction rather than calling ONTAP with a null
            // volume UUID (which would hit /api/storage/volumes/null and still look like success
            // upstream if we swallowed the error).
            throw new CloudRuntimeException("FlexVolume UUID (volumeUUID) is missing from storage pool details; "
                    + "cannot delete NFS template cache file [" + filePath + "] for template ["
                    + templateInfo.getId() + "]");
        }
        ((UnifiedNASStrategy) storageStrategy).deleteFileByPath(flexVolUuid, filePath);
        logger.info("deleteNfsTemplateCache: Deleted template cache file [{}] for template [{}]",
                filePath, templateInfo.getId());
    }

    /**
     * Rejects the request when the minimum IOPS being asked for would push the pool past its
     * configured IOPS capacity. Pools without an IOPS capacity enforce no ceiling.
     *
     * Used IOPS include volumes still in {@link Volume.State#Creating} so a second overlapping
     * create sees the first reservation. The volume being checked is omitted so its own min IOPS
     * are not counted twice (CloudStack has already moved it to Creating before this runs).
     */
    private void verifySufficientIopsForStoragePool(StoragePoolVO storagePool, Long requestedMinIops, long excludeVolumeId) {
        Long capacityIops = storagePool.getCapacityIops();
        if (capacityIops == null || requestedMinIops == null || requestedMinIops <= 0) {
            return;
        }

        long requestedTotalIops = getAllocatedMinIops(storagePool, excludeVolumeId) + requestedMinIops;
        if (requestedTotalIops > capacityIops) {
            throw new CloudRuntimeException(String.format(
                    "Insufficient IOPS capacity on storage pool %s: requested total of %d IOPS exceeds the pool IOPS capacity of %d",
                    storagePool.getName(), requestedTotalIops, capacityIops));
        }
    }

    /**
     * Deletes a volume or snapshot from the ONTAP storage system.
     *
     * <p>For volumes, deletes the backend storage object (LUN for iSCSI, file for NFS) via
     * {@link StorageStrategy#deleteCloudStackVolume}.</p>
     *
     * <p>For <b>volume snapshots</b>, this driver is invoked by the standard CloudStack delete chain
     * ({@code StorageSystemSnapshotStrategy} → {@code SnapshotServiceImpl.deleteSnapshot} →
     * {@code deleteAsync}). It reads ONTAP metadata from {@code snapshot_details} and delegates
     * the actual FlexVol snapshot delete to {@link StorageStrategy} (NFS or iSCSI implementation).
     * ONTAP REST/delete-job logic must not live here — keep it in the storage-strategy layer.</p>
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
                    logger.error("deleteAsync: Storage Pool not found for id: " + store.getId());
                    throw new CloudRuntimeException("Storage Pool not found for id: " + store.getId());
                }
                Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(store.getId());
                StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);
                logger.info("createCloudStackVolumeForTypeVolume: Connection to Ontap SVM [{}] successful, preparing CloudStackVolumeRequest", details.get(OntapStorageConstants.SVM_NAME));
                VolumeInfo volumeInfo = (VolumeInfo) data;
                VolumeDetailVO qosPolicyDetail = volumeDetailsDao.findDetail(
                        volumeInfo.getId(), OntapStorageConstants.QOS_POLICY_UUID);
                CloudStackVolume cloudStackVolumeRequest = createDeleteCloudStackVolumeRequest(storagePool, details, volumeInfo);
                storageStrategy.deleteCloudStackVolume(cloudStackVolumeRequest);
                if (qosPolicyDetail != null) {
                    deleteUnusedQosPolicy(storageStrategy, qosPolicyDetail.getValue());
                }
                logger.info("deleteAsync: Volume deleted: " + volumeInfo.getId());
                removeTemporarySnapshotCopyRecord(volumeInfo.getId());
                commandResult.setResult(null);
                commandResult.setSuccess(true);
            } else if (data.getType() == DataObjectType.TEMPLATE) {
                deleteTemplateOnPrimary(store, (TemplateInfo) data);
                commandResult.setResult(null);
                commandResult.setSuccess(true);
            } else if (data.getType() == DataObjectType.SNAPSHOT) {
                logger.info("deleteAsync: volume-snapshot delete for CloudStack snapshot [{}] on primary pool [{}] — "
                        + "delegating ONTAP FlexVol cleanup to StorageStrategy", data.getId(), store.getId());
                deleteCloudStackVolumeSnapshot((SnapshotInfo) data, commandResult);
            } else {
                throw new CloudRuntimeException("Unsupported data object type: " + data.getType());
            }
        } catch (Exception e) {
            logger.error("deleteAsync: Failed for data object [{}]: {}", data, e.getMessage());
            commandResult.setSuccess(false);
            commandResult.setResult(e.getMessage());
        } finally {
            // StorageSystemDataMotionStrategy deletes its temporary snapshot copy with a null callback.
            if (callback != null) {
                callback.complete(commandResult);
            }
        }
    }

    /**
     * Orchestrates CloudStack volume-snapshot delete on ONTAP.
     *
     * <p>This method is intentionally thin: it resolves identifiers persisted during
     * {@link #takeSnapshot} into {@code snapshot_details} and delegates to the protocol
     * {@link StorageStrategy} selected from pool details (NFS → {@code UnifiedNASStrategy},
     * iSCSI → {@code UnifiedSANStrategy}). Both protocols share the same FlexVol-level
     * snapshot delete REST API.</p>
     *
     * <p>Required {@code snapshot_details} keys (see {@link OntapStorageConstants}):</p>
     * <ul>
     *   <li>{@code base_ontap_fv_id} — FlexVol UUID</li>
     *   <li>{@code ontap_snap_id} — ONTAP snapshot UUID</li>
     *   <li>{@code ontap_snap_name} — snapshot name (logging)</li>
     *   <li>{@code primary_pool_id} — pool used to obtain credentials/protocol strategy</li>
     * </ul>
     */
    private void deleteCloudStackVolumeSnapshot(SnapshotInfo snapshotInfo, CommandResult commandResult) {
        long snapshotId = snapshotInfo.getId();
        logger.info("deleteCloudStackVolumeSnapshot: starting ONTAP delete for CloudStack volume snapshot [{}]", snapshotId);

        try {
            String flexVolUuid = getSnapshotDetail(snapshotId, OntapStorageConstants.BASE_ONTAP_FV_ID);
            String ontapSnapshotUuid = getSnapshotDetail(snapshotId, OntapStorageConstants.ONTAP_SNAP_ID);
            String snapshotName = getSnapshotDetail(snapshotId, OntapStorageConstants.ONTAP_SNAP_NAME);
            String poolIdStr = getSnapshotDetail(snapshotId, OntapStorageConstants.PRIMARY_POOL_ID);

            if (flexVolUuid == null || ontapSnapshotUuid == null) {
                logger.warn("deleteCloudStackVolumeSnapshot: missing ONTAP identity for snapshot [{}] "
                        + "(flexVolUuid={}, ontapSnapshotUuid={}). Cannot call ONTAP delete; "
                        + "treating as no-op — verify snapshot_details were written during takeSnapshot",
                        snapshotId, flexVolUuid, ontapSnapshotUuid);
                commandResult.setSuccess(true);
                commandResult.setResult(null);
                return;
            }

            long poolId = resolveSnapshotPoolId(poolIdStr, snapshotId);
            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(poolId);
            String protocol = poolDetails.get(OntapStorageConstants.PROTOCOL);
            StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(poolDetails);

            logger.info("deleteCloudStackVolumeSnapshot: snapshot [{}] — protocol [{}], pool [{}], "
                    + "flexVol [{}], ontapSnapshot [{}] (name [{}])",
                    snapshotId, protocol, poolId, flexVolUuid, ontapSnapshotUuid, snapshotName);

            storageStrategy.deleteFlexVolSnapshotForCloudStackVolume(flexVolUuid, ontapSnapshotUuid, snapshotName);

            logger.info("deleteCloudStackVolumeSnapshot: completed ONTAP delete for CloudStack volume snapshot [{}]", snapshotId);
            commandResult.setSuccess(true);
            commandResult.setResult(null);
        } catch (Exception e) {
            if (OntapStorageUtils.isOntapObjectNotFoundError(e)) {
                logger.warn("deleteCloudStackVolumeSnapshot: ONTAP snapshot for CloudStack snapshot [{}] "
                        + "already absent (idempotent success): {}", snapshotId, e.getMessage());
                commandResult.setSuccess(true);
                commandResult.setResult(null);
                return;
            }
            logger.error("deleteCloudStackVolumeSnapshot: ONTAP delete failed for CloudStack snapshot [{}]: {}",
                    snapshotId, e.getMessage(), e);
            commandResult.setSuccess(false);
            commandResult.setResult(e.getMessage());
        }
    }

    private long resolveSnapshotPoolId(String poolIdStr, long snapshotId) {
        if (poolIdStr != null && !poolIdStr.isEmpty()) {
            return Long.parseLong(poolIdStr);
        }
        SnapshotVO snapshotVO = snapshotDao.findById(snapshotId);
        if (snapshotVO == null) {
            throw new CloudRuntimeException("Snapshot not found for snapshot [" + snapshotId + "]");
        }
        VolumeVO volumeVO = volumeDao.findByIdIncludingRemoved(snapshotVO.getVolumeId());
        if (volumeVO == null) {
            throw new CloudRuntimeException("CloudStack Volume not found for snapshot [" + snapshotId + "]");
        }
        Long poolId = volumeVO.getPoolId() != null ? volumeVO.getPoolId() : volumeVO.getLastPoolId();
        if (poolId == null || poolId <= 0) {
            throw new CloudRuntimeException("Cannot resolve storage pool for snapshot [" + snapshotId + "]");
        }
        return poolId;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        copyAsync(srcData, destData, null, callback);
    }

    /**
     * Dispatches a driver-level copy by (source type, destination type). Only the pairs accepted by
     * {@link #canCopy} are handled; everything else is left to the data motion strategies.
     *
     * <p>Callers may pass a null callback (for example {@code StorageSystemDataMotionStrategy} for the
     * temporary snapshot copy), in which case a failed copy is thrown.</p>
     */
    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCommandResult result;
        if (isSnapshotToTemporaryVolumeCopy(srcData, destData)) {
            result = copySnapshotToTemporaryVolume(srcData, (VolumeInfo) destData);
        } else {
            throw new UnsupportedOperationException("Copy from " + describeForCopy(srcData) + " to "
                    + describeForCopy(destData) + " is not supported for ONTAP primary storage.");
        }

        if (callback != null) {
            callback.complete(result);
        } else if (!result.isSuccess()) {
            throw new CloudRuntimeException("Failed to copy " + srcData.getType() + " [" + srcData.getId() + "] to "
                    + destData.getType() + " [" + destData.getId() + "]: " + result.getResult());
        }
    }

    /**
     * Returns true only for the (source type, destination type) pairs the driver copies itself.
     *
     * <p>{@code DataMotionServiceImpl} consults {@code canCopy} before choosing a data motion strategy,
     * so any pair accepted here bypasses {@code StorageSystemDataMotionStrategy} (volume migration,
     * createVolume(snapshotid), template caching). Add a pair only together with its handler in
     * {@link #copyAsync}.</p>
     */
    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        if (srcData == null || destData == null || srcData.getDataStore() == null || destData.getDataStore() == null) {
            return false;
        }
        return isSnapshotToTemporaryVolumeCopy(srcData, destData);
    }

    private String describeForCopy(DataObject data) {
        return data == null ? "null" : data.getType() + " [" + data.getId() + "]";
    }

    /**
     * Matches the temporary volume {@code StorageSystemDataMotionStrategy} creates on the snapshot's own
     * pool while copying the snapshot to secondary storage for {@code createTemplate(snapshotid)}.
     */
    private boolean isSnapshotToTemporaryVolumeCopy(DataObject srcData, DataObject destData) {
        if (srcData == null || destData == null || srcData.getDataStore() == null || destData.getDataStore() == null) {
            return false;
        }
        if (srcData.getType() != DataObjectType.SNAPSHOT || destData.getType() != DataObjectType.VOLUME) {
            return false;
        }
        if (srcData.getDataStore().getRole() != DataStoreRole.Primary
                || srcData.getDataStore().getId() != destData.getDataStore().getId()) {
            return false;
        }
        return isTemporarySnapshotCopyVolume(volumeDao.findById(destData.getId()));
    }

    /**
     * Clones a CloudStack volume snapshot into the temporary volume used for
     * {@code createTemplate(snapshotid)}. The strategy then maps that volume to a KVM host, copies it
     * into the template and deletes it again through {@link #deleteAsync}.
     */
    private CopyCommandResult copySnapshotToTemporaryVolume(DataObject srcData, VolumeInfo volInfo) {
        try {
            StoragePoolVO storagePool = storagePoolDao.findById(volInfo.getDataStore().getId());
            if (storagePool == null) {
                throw new CloudRuntimeException("Storage Pool not found for id: " + volInfo.getDataStore().getId());
            }
            Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
            validateProtocol(details, volInfo.getDataStore());
            VolumeVO volumeVO = volumeDao.findById(volInfo.getId());

            renameTemporarySnapshotCopy(volInfo, volumeVO, srcData.getId());
            logger.info("copySnapshotToTemporaryVolume: Cloning CS snapshot [{}] into temporary volume [{}] on pool [{}] for template creation",
                    srcData.getId(), volInfo.getId(), storagePool.getId());
            CloudStackVolume cloned = cloneCloudStackVolumeFromSnapshot(storagePool, volInfo, details, srcData.getId());
            if (ProtocolType.NFS3.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
                volumeVO.setPath(volInfo.getUuid());
            }
            recordCreatedVolume(storagePool, volInfo, volumeVO, details, cloned);
            return new CopyCommandResult(null, new CopyCmdAnswer(volInfo.getTO()));
        } catch (Exception e) {
            logger.error("copySnapshotToTemporaryVolume: Failed to clone snapshot [{}] into volume [{}]: {}",
                    srcData.getId(), volInfo.getId(), e.getMessage());
            CopyCommandResult result = new CopyCommandResult(null, new CopyCmdAnswer(e.getMessage()));
            result.setResult(e.getMessage());
            return result;
        }
    }

    /**
     * The temporary volume created by {@code StorageSystemDataMotionStrategy} for a snapshot copy is
     * persisted in Allocated state without a disk offering; user volumes always carry a disk offering.
     */
    private boolean isTemporarySnapshotCopyVolume(VolumeVO volumeVO) {
        if (volumeVO == null || volumeVO.getState() != Volume.State.Allocated) {
            return false;
        }
        Long diskOfferingId = volumeVO.getDiskOfferingId();
        return diskOfferingId == null || diskOfferingId == 0L;
    }

    /**
     * The framework names the temporary volume {@code <snapshot name>_<millis>.TMP}, which is not a valid
     * ONTAP LUN name, so it is renamed before cloning. The in-memory object is updated as well because the
     * SAN clone derives the LUN name from {@link VolumeInfo#getName()}.
     */
    private void renameTemporarySnapshotCopy(VolumeInfo volInfo, VolumeVO volumeVO, long csSnapshotId) {
        String name = OntapStorageConstants.TEMP_SNAPSHOT_COPY_NAME_PREFIX + csSnapshotId + OntapStorageConstants.UNDERSCORE + volInfo.getId();
        volumeVO.setName(name);
        if (volInfo.getVolume() instanceof VolumeVO) {
            ((VolumeVO) volInfo.getVolume()).setName(name);
        }
    }

    /**
     * {@code StorageSystemDataMotionStrategy} deletes the temporary snapshot copy on the array but leaves its
     * volume row behind on success, so it is removed here once the backend object is gone.
     */
    private void removeTemporarySnapshotCopyRecord(long volumeId) {
        VolumeVO volumeVO = volumeDao.findById(volumeId);
        if (!isTemporarySnapshotCopyVolume(volumeVO)
                || volumeVO.getName() == null || !volumeVO.getName().startsWith(OntapStorageConstants.TEMP_SNAPSHOT_COPY_NAME_PREFIX)) {
            return;
        }
        volumeDetailsDao.removeDetails(volumeId);
        volumeDao.remove(volumeId);
        logger.info("deleteAsync: Removed temporary snapshot copy volume record [{}]", volumeId);
    }

    /**
     * Resizes a volume. Size and QoS from the resize window are applied together by
     * {@link StorageStrategy#resizeCloudStackVolume}.
     */
    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateCmdResult result = null;
        try {
            if (data != null && data.getType() == DataObjectType.VOLUME) {
                if (data instanceof VolumeInfo) {
                    VolumeQosPolicy policyToApply = null;
                    VolumeInfo volumeInfo = (VolumeInfo) data;
                    Object rawPayload = volumeInfo.getpayload();
                    ResizeVolumePayload payload = (rawPayload instanceof ResizeVolumePayload)
                            ? (ResizeVolumePayload) rawPayload : null;
                    if (payload == null || payload.newSize == null) {
                        throw new CloudRuntimeException("Invalid resize payload for volume " + volumeInfo.getId());
                    }
                    if (volumeInfo.getDataStore() == null) {
                        throw new CloudRuntimeException("Data store not found for volume " + volumeInfo.getId());
                    }

                    StoragePoolVO storagePool = storagePoolDao.findById(volumeInfo.getDataStore().getId());
                    if (storagePool == null) {
                        throw new CloudRuntimeException("Storage pool not found for volume " + volumeInfo.getId());
                    }
                    Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());

                    VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());
                    if (volumeVO == null) {
                        throw new CloudRuntimeException("Volume not found for id " + volumeInfo.getId());
                    }
                    if (payload.newSize < volumeVO.getSize()) {
                        throw new CloudRuntimeException("Unable to shrink volume.");
                    }

                    verifySufficientIopsForStoragePool(storagePool, payload.newMinIops, volumeVO.getId());
                    StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);
                    VolumeDetailVO qosDetail = volumeDetailsDao.findDetail(volumeVO.getId(), OntapStorageConstants.QOS_POLICY_UUID);
                    String previousUuid = qosDetail != null ? qosDetail.getValue() : null;
                    VolumeQosPolicy qosPolicy = createQosPolicyIfNeeded(storageStrategy, details,
                            payload.newMinIops, payload.newMaxIops, storagePool.getId());

                    if (qosPolicy != null && !Objects.equals(previousUuid, qosPolicy.getUuid())) {
                        policyToApply = qosPolicy;
                    } else if (qosPolicy == null && previousUuid != null) {
                        policyToApply = new VolumeQosPolicy();
                        policyToApply.setName(OntapStorageConstants.QOS_POLICY_NONE);
                    }

                    try {
                        CloudStackVolume cloudStackVolume = resizeCloudStackVolumeRequest(details, volumeInfo, policyToApply);
                        storageStrategy.resizeCloudStackVolume(cloudStackVolume, payload.newSize);
                    } catch (RuntimeException e) {
                        if (qosPolicy != null && !Objects.equals(previousUuid, qosPolicy.getUuid())) {
                            deleteUnusedQosPolicy(storageStrategy, qosPolicy.getUuid());
                        }
                        throw e;
                    }
                    if (policyToApply != null) {
                        persistQosPolicyDetails(volumeVO.getId(), qosPolicy);
                        deleteUnusedQosPolicy(storageStrategy, previousUuid);
                    }

                volumeVO.setSize(payload.newSize);
                if (!volumeDao.update(volumeVO.getId(), volumeVO)) {
                    throw new CloudRuntimeException("Failed to update volume " + volumeVO.getId()
                            + " after resizing the ONTAP backing object");
                }
                result = new CreateCmdResult(volumeVO.getPath(), new Answer(null, true, null));
                logger.info("resize: Successfully resized volume [{}] to [{}] bytes", volumeInfo.getId(), payload.newSize);
                }else{
                    String errorMessage = "Invalid DataObjectType (" + data.getType() + ") passed to resize";
                    result = new CreateCmdResult(null, new Answer(null, false, errorMessage));
                    result.setResult(errorMessage);
                    return;
                }
            } else {
                throw new CloudRuntimeException("Expected a VOLUME DataObject but received " +
                        (data != null ? data.getType() : "null"));
            }
        } catch (Exception e) {
            String errMsg = e.getMessage();
            logger.error("resize: Failed for volume [{}]: {}", data != null ? data.getId() : null, errMsg, e);
            result = new CreateCmdResult(null, new Answer(null, false, errMsg));
            result.setResult(errMsg);
        } finally {
            callback.complete(result);
        }
    }

    private void deleteUnusedQosPolicy(StorageStrategy storageStrategy, String policyUuid) {
        if (policyUuid == null || policyUuid.isEmpty()) {
            return;
        }
        try {
            storageStrategy.deleteVolumeQosPolicy(policyUuid);
        } catch (Exception e) {
            logger.error("Unused QoS policy [{}] was not deleted: {}", policyUuid, e.getMessage());
        }
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
                logger.error("grantAccess: Storage Pool not found for id: " + dataStore.getId());
                throw new CloudRuntimeException("Storage Pool not found for id: " + dataStore.getId());
            }

            // ONTAP managed storage only supports cluster and zone scoped pools
            if (storagePool.getScope() != ScopeType.CLUSTER && storagePool.getScope() != ScopeType.ZONE) {
                logger.error("grantAccess: Only Cluster and Zone scoped primary storage is supported for storage Pool: " + storagePool.getName());
                throw new CloudRuntimeException("Only Cluster and Zone scoped primary storage is supported for Storage Pool: " + storagePool.getName());
            }

            if (dataObject.getType() == DataObjectType.VOLUME) {
                VolumeVO volumeVO = volumeDao.findById(dataObject.getId());
                if (volumeVO == null) {
                    logger.error("grantAccess: CloudStack Volume not found for id: " + dataObject.getId());
                    throw new CloudRuntimeException("CloudStack Volume not found for id: " + dataObject.getId());
                }

                Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
                String svmName = details.get(OntapStorageConstants.SVM_NAME);

                if (ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
                    // Only retrieve LUN name for iSCSI volumes
                    grantAccessIscsi(host, volumeVO, details, svmName, storagePool);
                } else if (ProtocolType.NFS3.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
                    // For NFS, no access grant needed - file is accessible via mount
                    logger.debug("grantAccess: NFS volume [{}], no igroup mapping required", volumeVO.getUuid());
                    return true;
                }
                volumeVO.setPoolType(storagePool.getPoolType());
                volumeVO.setPoolId(storagePool.getId());
                volumeDao.update(volumeVO.getId(), volumeVO);
                syncTemporarySnapshotCopyPath(dataObject, volumeVO);
            } else if (dataObject.getType() == DataObjectType.TEMPLATE) {
                grantAccessTemplate((TemplateInfo) dataObject, host, dataStore, storagePool);
            } else {
                logger.error("Invalid DataObjectType (" + dataObject.getType() + ") passed to grantAccess");
                throw new CloudRuntimeException("Invalid DataObjectType (" + dataObject.getType() + ") passed to grantAccess");
            }
            return true;
        } catch (Exception e) {
            logger.error("grantAccess: Failed for dataObject [{}]: {}", dataObject, e.getMessage());
            throw new CloudRuntimeException("Failed with error: " + e.getMessage(), e);
        }
    }

    /**
     * For the temporary snapshot copy, {@code StorageSystemDataMotionStrategy} builds the CopyCommand from
     * the same in-memory volume object it passed to grantAccess, so the iSCSI path resolved by the LUN
     * mapping must be reflected on that object too.
     */
    private void syncTemporarySnapshotCopyPath(DataObject dataObject, VolumeVO volumeVO) {
        if (!isTemporarySnapshotCopyVolume(volumeVO) || !(dataObject instanceof VolumeInfo)) {
            return;
        }
        Volume inMemoryVolume = ((VolumeInfo) dataObject).getVolume();
        if (inMemoryVolume instanceof VolumeVO) {
            ((VolumeVO) inMemoryVolume).setPath(volumeVO.getPath());
            ((VolumeVO) inMemoryVolume).set_iScsiName(volumeVO.get_iScsiName());
        }
    }

    private void grantAccessIscsi(Host host, VolumeVO volumeVO, Map<String, String> details, String svmName, StoragePoolVO storagePool) {
        String cloudStackVolumeName = volumeDetailsDao.findDetail(volumeVO.getId(), OntapStorageConstants.LUN_DOT_NAME).getValue();
        UnifiedSANStrategy sanStrategy = (UnifiedSANStrategy) OntapStorageUtils.getStrategyByStoragePoolDetails(details);
        String accessGroupName = OntapStorageUtils.getIgroupName(svmName, host.getUuid());

        ensureAccessGroupForHost(sanStrategy, host, storagePool, svmName, accessGroupName);

        // Create or retrieve existing LUN mapping
        String lunNumber = sanStrategy.ensureLunMapped(svmName, cloudStackVolumeName, accessGroupName);

        // Update volume path if changed (e.g., after migration or re-mapping)
        String iscsiPath = buildIscsiPath(storagePool, lunNumber);
        if (volumeVO.getPath() == null || !volumeVO.getPath().equals(iscsiPath)) {
            volumeVO.set_iScsiName(iscsiPath);
            volumeVO.setPath(iscsiPath);
        }
    }

    /**
     * Maps the cached template LUN to the host so the KVM agent can write the image into it.
     *
     * <p>Called by the framework from {@code copyTemplateToManagedTemplateVolume} just before it
     * issues the {@code CopyCommand}. That method reads {@code managedStoreTarget} from the pool
     * details <em>before</em> this call, when the LUN number does not exist yet, so the stale
     * value is corrected here. The datastore details are re-read when the command is built, so the
     * update lands in time.</p>
     */
    private void grantAccessTemplate(TemplateInfo templateInfo, Host host, DataStore dataStore, StoragePoolVO storagePool) {
        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
        if (!ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
            logger.debug("grantAccessTemplate: NFS template [{}], no igroup mapping required", templateInfo.getUuid());
            return;
        }

        String svmName = details.get(OntapStorageConstants.SVM_NAME);
        VMTemplateStoragePoolVO templatePoolRef = findTemplatePoolRef(storagePool.getId(), templateInfo.getId());
        String lunName = getTemplateLunName(storagePool, templateInfo.getId());

        UnifiedSANStrategy sanStrategy = (UnifiedSANStrategy) OntapStorageUtils.getStrategyByStoragePoolDetails(details);
        String accessGroupName = OntapStorageUtils.getIgroupName(svmName, host.getUuid());

        ensureAccessGroupForHost(sanStrategy, host, storagePool, svmName, accessGroupName);

        boolean lunMapped = false;
        try {
            String lunNumber = sanStrategy.ensureLunMapped(svmName, lunName, accessGroupName);
            lunMapped = true;
            String iscsiPath = buildIscsiPath(storagePool, lunNumber);

            templatePoolRef.setInstallPath(iscsiPath);
            vmTemplatePoolDao.update(templatePoolRef.getId(), templatePoolRef);
            refreshManagedStoreTarget(dataStore, iscsiPath);

            logger.info("grantAccessTemplate: Mapped template cache LUN [{}] to igroup [{}] as [{}] for template [{}]",
                    lunName, accessGroupName, iscsiPath, templateInfo.getId());
        } catch (RuntimeException e) {
            // Undo only this call's side effect (map). Do not delete the cache LUN — create already succeeded.
            if (lunMapped) {
                try {
                    unmapLunFromHost(sanStrategy, svmName, lunName, accessGroupName, host);
                } catch (Exception rollbackEx) {
                    logger.warn("grantAccessTemplate: Failed to unmap template cache LUN [{}] from igroup [{}] after grant failure: {}",
                            lunName, accessGroupName, rollbackEx.getMessage());
                }
            }
            throw e;
        }
    }

    /**
     * Ensures an igroup containing this host's initiator exists on the SVM.
     *
     * <p>The igroup may be absent even for a host that used the pool before, because LUN maps are
     * created with {@code delete_on_unmap}, which lets ONTAP remove the igroup on its own.</p>
     */
    private void ensureAccessGroupForHost(UnifiedSANStrategy sanStrategy, Host host, StoragePoolVO storagePool,
                                          String svmName, String accessGroupName) {
        Map<String, String> getAccessGroupMap = Map.of(
                OntapStorageConstants.NAME, accessGroupName,
                OntapStorageConstants.SVM_DOT_NAME, svmName
        );
        AccessGroup accessGroup = sanStrategy.getAccessGroup(getAccessGroupMap);
        if (accessGroup == null || accessGroup.getIgroup() == null) {
            logger.info("ensureAccessGroupForHost: Igroup {} does not exist for the host {} : Need to create Igroup for the host ", accessGroupName, host.getName());
            accessGroup = new AccessGroup();
            List<HostVO> hosts = new ArrayList<>();
            hosts.add((HostVO) host);
            accessGroup.setHostsToConnect(hosts);
            accessGroup.setStoragePoolId(storagePool.getId());
            accessGroup = sanStrategy.createAccessGroup(accessGroup);
        } else {
            logger.info("ensureAccessGroupForHost: Igroup {} already exist for the host {}: ", accessGroup.getIgroup().getName(), host.getName());
            /* TODO Below cases will be covered later, for now they will be a pre-requisite on customer side
              1. Igroup exist with the same name but host initiator has been removed
              2.  Igroup exist with the same name but host initiator has been changed may be due to new NIC or new adapter
              In both cases we need to verify current host initiator is registered in the igroup before allowing access
              Incase it is not , add it and proceed for lun-mapping
             */
        }
        logger.info("ensureAccessGroupForHost: Igroup {}  is present now with initiators {} ", accessGroup.getIgroup().getName(), accessGroup.getIgroup().getInitiators());
    }

    /**
     * Builds the volume path the KVM agent expects for managed iSCSI: {@code /<targetIQN>/<lunNumber>}.
     */
    private String buildIscsiPath(StoragePoolVO storagePool, String lunNumber) {
        return OntapStorageConstants.SLASH + storagePool.getPath() + OntapStorageConstants.SLASH + lunNumber;
    }

    private void refreshManagedStoreTarget(DataStore dataStore, String iscsiPath) {
        if (!(dataStore instanceof PrimaryDataStore)) {
            return;
        }
        PrimaryDataStore primaryDataStore = (PrimaryDataStore) dataStore;
        Map<String, String> storeDetails = primaryDataStore.getDetails();
        if (storeDetails == null) {
            return;
        }
        Map<String, String> updated = new HashMap<>(storeDetails);
        updated.put(PrimaryDataStore.MANAGED_STORE_TARGET, iscsiPath);
        primaryDataStore.setDetails(updated);
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

            StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
            if (storagePool == null) {
                logger.error("revokeAccess: Storage Pool not found for id: " + dataStore.getId());
                throw new CloudRuntimeException("Storage Pool not found for id: " + dataStore.getId());
            }

            if (storagePool.getScope() != ScopeType.CLUSTER && storagePool.getScope() != ScopeType.ZONE) {
                logger.error("revokeAccess: Only Cluster and Zone scoped primary storage is supported for storage Pool: " + storagePool.getName());
                throw new CloudRuntimeException("Only Cluster and Zone scoped primary storage is supported for Storage Pool: " + storagePool.getName());
            }

            if (dataObject.getType() == DataObjectType.VOLUME) {
                VolumeVO volumeVO = volumeDao.findById(dataObject.getId());
                if (volumeVO == null) {
                    logger.error("revokeAccess: CloudStack Volume not found for id: " + dataObject.getId());
                    throw new CloudRuntimeException("CloudStack Volume not found for id: " + dataObject.getId());
                }
                revokeAccessForVolume(storagePool, volumeVO, host);
            } else if (dataObject.getType() == DataObjectType.TEMPLATE) {
                revokeAccessForTemplate(storagePool, (TemplateInfo) dataObject, host);
            } else {
                logger.error("revokeAccess: Invalid DataObjectType (" + dataObject.getType() + ") passed to revokeAccess");
                throw new CloudRuntimeException("Invalid DataObjectType (" + dataObject.getType() + ") passed to revokeAccess");
            }
        } catch (Exception e) {
            logger.error("revokeAccess: Failed for dataObject [{}]: {}", dataObject, e.getMessage());
            throw new CloudRuntimeException("Failed with error: " + e.getMessage(), e);
        }
    }

    /**
     * Revokes volume access for the specified host.
     */
    private void revokeAccessForVolume(StoragePoolVO storagePool, VolumeVO volumeVO, Host host) {
        logger.info("revokeAccessForVolume: Revoking access to volume [{}] for host [{}]", volumeVO.getName(), host.getName());

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
        StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);
        String svmName = details.get(OntapStorageConstants.SVM_NAME);

        if (ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
            String accessGroupName = OntapStorageUtils.getIgroupName(svmName, host.getUuid());

            // Retrieve LUN name from volume details; if missing, volume may not have been fully created
            VolumeDetailVO lunDetail = volumeDetailsDao.findDetail(volumeVO.getId(), OntapStorageConstants.LUN_DOT_NAME);
            String lunName = lunDetail != null ? lunDetail.getValue() : null;
            if (lunName == null) {
                logger.warn("revokeAccessForVolume: No LUN name found for volume [{}]; skipping revoke", volumeVO.getId());
                return;
            }
            unmapLunFromHost(storageStrategy, svmName, lunName, accessGroupName, host);
        }
    }

    /**
     * Unmaps the cached template LUN once the framework has finished writing the image into it.
     */
    private void revokeAccessForTemplate(StoragePoolVO storagePool, TemplateInfo templateInfo, Host host) {
        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
        if (!ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
            logger.debug("revokeAccessForTemplate: NFS template [{}], no igroup mapping to remove", templateInfo.getUuid());
            return;
        }

        String svmName = details.get(OntapStorageConstants.SVM_NAME);
        StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);
        String accessGroupName = OntapStorageUtils.getIgroupName(svmName, host.getUuid());
        String lunName = getTemplateLunName(storagePool, templateInfo.getId());

        logger.info("revokeAccessForTemplate: Revoking access to template cache LUN [{}] for host [{}]", lunName, host.getName());
        unmapLunFromHost(storageStrategy, svmName, lunName, accessGroupName, host);
    }

    /**
     * Removes a LUN-to-igroup mapping, skipping quietly when the LUN, the igroup or the host
     * initiator is already gone.
     */
    private void unmapLunFromHost(StorageStrategy storageStrategy, String svmName, String lunName,
                                  String accessGroupName, Host host) {
        ValidateRevoke result = getValidateRevoke(lunName, host, storageStrategy, svmName, accessGroupName);
        if (result == null) {
            return;
        }

        Map<String, String> disableLogicalAccessMap = new HashMap<>();
        disableLogicalAccessMap.put(OntapStorageConstants.LUN_DOT_UUID, result.cloudStackVolume.getLun().getUuid());
        disableLogicalAccessMap.put(OntapStorageConstants.IGROUP_DOT_UUID, result.accessGroup.getIgroup().getUuid());
        storageStrategy.disableLogicalAccess(disableLogicalAccessMap);

        logger.info("unmapLunFromHost: Successfully revoked access to LUN [{}] for host [{}]", result.lunName, host.getName());
    }

    @Nullable
    private ValidateRevoke getValidateRevoke(String lunName, Host host, StorageStrategy storageStrategy, String svmName, String accessGroupName) {
        // Verify LUN still exists on ONTAP (may have been manually deleted)
        CloudStackVolume cloudStackVolume = getCloudStackVolumeByName(storageStrategy, svmName, lunName);
        if (cloudStackVolume == null || cloudStackVolume.getLun() == null || cloudStackVolume.getLun().getUuid() == null) {
            logger.warn("getValidateRevoke: LUN [{}] not found on ONTAP, skipping revoke", lunName);
            return null;
        }

        // Verify igroup still exists on ONTAP
        AccessGroup accessGroup = getAccessGroupByName(storageStrategy, svmName, accessGroupName);
        if (accessGroup == null || accessGroup.getIgroup() == null || accessGroup.getIgroup().getUuid() == null) {
            logger.warn("getValidateRevoke: iGroup [{}] not found on ONTAP, skipping revoke", accessGroupName);
            return null;
        }

        // Verify host initiator is in the igroup before attempting to remove mapping
        SANStrategy sanStrategy = (UnifiedSANStrategy) storageStrategy;
        if (!sanStrategy.validateInitiatorInAccessGroup(host.getStorageUrl(), svmName, accessGroup.getIgroup())) {
            logger.warn("getValidateRevoke: Initiator [{}] is not in iGroup [{}], skipping revoke",
                    host.getStorageUrl(), accessGroupName);
            return null;
        }
        return new ValidateRevoke(lunName, cloudStackVolume, accessGroup);
    }

    private static class ValidateRevoke {
        public final String lunName;
        public final CloudStackVolume cloudStackVolume;
        public final AccessGroup accessGroup;

        public ValidateRevoke(String lunName, CloudStackVolume cloudStackVolume, AccessGroup accessGroup) {
            this.lunName = lunName;
            this.cloudStackVolume = cloudStackVolume;
            this.accessGroup = accessGroup;
        }
    }

    /**
     * Retrieves a volume from ONTAP by name.
     */
    private CloudStackVolume getCloudStackVolumeByName(StorageStrategy storageStrategy, String svmName, String cloudStackVolumeName) {
        Map<String, String> getCloudStackVolumeMap = new HashMap<>();
        getCloudStackVolumeMap.put(OntapStorageConstants.NAME, cloudStackVolumeName);
        getCloudStackVolumeMap.put(OntapStorageConstants.SVM_DOT_NAME, svmName);

        CloudStackVolume cloudStackVolume = storageStrategy.getCloudStackVolume(getCloudStackVolumeMap);
        if (cloudStackVolume == null || cloudStackVolume.getLun() == null || cloudStackVolume.getLun().getName() == null) {
            logger.warn("getCloudStackVolumeByName: LUN [{}] not found on ONTAP", cloudStackVolumeName);
            return null;
        }
        return cloudStackVolume;
    }

    /**
     * Retrieves an access group from ONTAP by name.
     */
    private AccessGroup getAccessGroupByName(StorageStrategy storageStrategy, String svmName, String accessGroupName) {
        Map<String, String> getAccessGroupMap = new HashMap<>();
        getAccessGroupMap.put(OntapStorageConstants.NAME, accessGroupName);
        getAccessGroupMap.put(OntapStorageConstants.SVM_DOT_NAME, svmName);

        AccessGroup accessGroup = storageStrategy.getAccessGroup(getAccessGroupMap);
        if (accessGroup == null || accessGroup.getIgroup() == null || accessGroup.getIgroup().getName() == null) {
            logger.warn("getAccessGroupByName: iGroup [{}] not found on ONTAP", accessGroupName);
            return null;
        }
        return accessGroup;
    }

    /**
     * ONTAP is only supported with KVM, which does not take hypervisor-side snapshots into the
     * volume itself, so no reserve is added on top of the requested size.
     *
     * <p>For a template this returns the <b>virtual</b> size ({@code VMTemplateVO.size}), not the
     * compressed size on secondary storage. The cached template LUN is written by the KVM agent
     * with {@code qemu-img convert} from QCOW2 to RAW, so it must be able to hold the fully
     * expanded image.</p>
     */
    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool storagePool) {
        if (dataObject == null) {
            return 0;
        }
        Long size = dataObject.getSize();
        return size != null && size > 0 ? size : 0;
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {
        if (templateInfo == null || storagePool == null) {
            return 0;
        }
        // template_spool_ref is inserted in Allocated before the cache exists;
        // only skip reservation when the template is Ready and has a backend identity.
        VMTemplateStoragePoolVO templatePoolRef =
                vmTemplatePoolDao.findByPoolTemplate(storagePool.getId(), templateInfo.getId(), null);
        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
        if (isTemplateCachedOnPool(templatePoolRef, details)) {
            return 0;
        }
        return getDataObjectSizeIncludingHypervisorSnapshotReserve(templateInfo, storagePool);
    }

    /**
     * Returns true when the primary template cache is present and usable for clone/deploy.
     * A spool_ref row alone is not enough: CloudStack creates it in Allocated before the LUN/file exists.
     * Ready is sufficient; downloadState is set alongside Ready on the managed-cache success path.
     */
    private boolean isTemplateCachedOnPool(VMTemplateStoragePoolVO templatePoolRef, Map<String, String> details) {
        if (templatePoolRef == null) {
            return false;
        }
        if (templatePoolRef.getState() != ObjectInDataStoreStateMachine.State.Ready) {
            return false;
        }
        if (details != null && isIscsi(details)) {
            return StringUtils.isNotBlank(templatePoolRef.getLocalDownloadPath());
        }
        return StringUtils.isNotBlank(templatePoolRef.getInstallPath());
    }

    /**
     * Returns the bytes used on the FlexVolume backing this pool, read directly from ONTAP
     * ({@code space.used}).
     *
     * <p>Fails closed when ONTAP cannot provide trustworthy usage data. Returning zero for an
     * unreachable or incomplete backend would make capacity checks treat an unknown pool as empty
     * and could incorrectly authorize a volume grow.</p>
     *
     * @throws InvalidParameterValueException if {@code storagePool} is null
     * @throws CloudRuntimeException if the pool has no FlexVolume UUID in its details, ONTAP
     *         cannot be queried, or used-space data is missing
     */
    @Override
    public long getUsedBytes(StoragePool storagePool) {
        if (storagePool == null) {
            throw new InvalidParameterValueException("storagePool is null, ensure the pool exists and is fully initialised before querying used bytes");
        }

        Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(storagePool.getId());
        String flexVolUuid = poolDetails != null ? poolDetails.get(OntapStorageConstants.VOLUME_UUID) : null;

        if (StringUtils.isBlank(flexVolUuid)) {
            throw new CloudRuntimeException("FlexVolume UUID not found in pool details for pool " + storagePool.getId());
        }

        try {
            StorageStrategy strategy = OntapStorageUtils.getStrategyByStoragePoolDetails(poolDetails);
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put(OntapStorageConstants.FIELDS, OntapStorageConstants.SPACE_USED);
            var flexVol = strategy.getStorageVolume(flexVolUuid, queryParams);

            if (flexVol == null) {
                throw new CloudRuntimeException(String.format(
                        "FlexVolume [%s] backing pool [%s] was not found on ONTAP",
                        flexVolUuid, storagePool.getId()));
            }
            if (flexVol.getSpace() == null) {
                throw new CloudRuntimeException(String.format(
                        "ONTAP returned no space information for FlexVolume [%s] backing pool [%s]",
                        flexVolUuid, storagePool.getId()));
            }

            logger.debug("getUsedBytes: FlexVolume [{}] backing pool [{}] reports {} bytes used",
                    flexVolUuid, storagePool.getId(), flexVol.getSpace().getUsed());
            return flexVol.getSpace().getUsed();
        } catch (CloudRuntimeException e) {
            logger.error("getUsedBytes: Failed to get used bytes for pool [{}]", storagePool.getId(), e);
            throw e;
        } catch (Exception e) {
            logger.error("getUsedBytes: Failed to get used bytes for pool [{}]", storagePool.getId(), e);
            throw new CloudRuntimeException(
                    String.format("Could not read used space for pool [%s]: %s", storagePool.getId(), e.getMessage()), e);
        }
    }

    /**
     * Returns min IOPS reserved on the pool, including volumes still being created.
     * Destroyed and expunged volumes are omitted by the DAO query.
     */
    @Override
    public long getUsedIops(StoragePool storagePool) {
        return getAllocatedMinIops(storagePool, null);
    }

    private long getAllocatedMinIops(StoragePool storagePool, Long excludeVolumeId) {
        long usedIops = 0;

        List<VolumeVO> volumes = volumeDao.findNonDestroyedVolumesByPoolId(storagePool.getId(), null);
        if (volumes != null) {
            for (VolumeVO volume : volumes) {
                if (excludeVolumeId != null && excludeVolumeId.equals(volume.getId())) {
                    continue;
                }
                if (volume.getMinIops() != null) {
                    usedIops += volume.getMinIops();
                }
            }
        }

        return usedIops;
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
        logger.info("OntapPrimaryDatastoreDriver.takeSnapshot: Creating FlexVolume snapshot for snapshot [{}]", snapshot.getId());
        CreateCmdResult result;

        try {
            VolumeInfo volumeInfo = snapshot.getBaseVolume();

            VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());
            if (volumeVO == null) {
                throw new CloudRuntimeException("CloudStack Volume not found for id: " + volumeInfo.getId());
            }

            StoragePoolVO storagePool = storagePoolDao.findById(volumeVO.getPoolId());
            if (storagePool == null) {
                logger.error("takeSnapshot: Storage Pool not found for id: {}", volumeVO.getPoolId());
                throw new CloudRuntimeException("Storage Pool not found for id: " + volumeVO.getPoolId());
            }

            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(volumeVO.getPoolId());
            String protocol = poolDetails.get(OntapStorageConstants.PROTOCOL);
            String flexVolUuid = poolDetails.get(OntapStorageConstants.VOLUME_UUID);

            if (flexVolUuid == null || flexVolUuid.isEmpty()) {
                throw new CloudRuntimeException("FlexVolume UUID not found in pool details for pool " + volumeVO.getPoolId());
            }

            StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(poolDetails);
            SnapshotFeignClient snapshotClient = storageStrategy.getSnapshotFeignClient();
            String authHeader = storageStrategy.getAuthHeader();

            SnapshotObjectTO snapshotObjectTo = (SnapshotObjectTO) snapshot.getTO();

            // Preserve CloudStack UI snapshot name with stable uniqueness suffix.
            String snapshotName = buildSnapshotName(snapshot.getName(), snapshot.getId());

            // Resolve the volume path for storing in snapshot details (for revert operation)
            String volumePath = resolveVolumePathOnOntap(volumeVO, protocol, poolDetails);

            // For iSCSI, retrieve LUN UUID for restore operations
            String lunUuid = null;
            if (ProtocolType.ISCSI.name().equalsIgnoreCase(protocol)) {
                VolumeDetailVO lunDetail = volumeDetailsDao.findDetail(volumeVO.getId(), OntapStorageConstants.LUN_DOT_UUID);
                if (lunDetail == null || lunDetail.getValue() == null) {
                    throw new CloudRuntimeException("LUN UUID not found for iSCSI volume " + volumeVO.getId());
                }
                lunUuid = lunDetail.getValue();
            }

            // Create FlexVolume snapshot via ONTAP REST API
            FlexVolSnapshot snapshotRequest = new FlexVolSnapshot(snapshotName,
                    "CloudStack volume snapshot for volume " + volumeInfo.getName());

            logger.info("takeSnapshot: Creating ONTAP FlexVolume snapshot [{}] on FlexVol UUID [{}] for volume [{}]",
                    snapshotName, flexVolUuid, volumeVO.getId());

            JobResponse jobResponse = snapshotClient.createSnapshot(authHeader, flexVolUuid, snapshotRequest);
            if (jobResponse == null || jobResponse.getJob() == null) {
                throw new CloudRuntimeException("Failed to initiate FlexVolume snapshot on FlexVol UUID [" + flexVolUuid + "]");
            }

            // Poll for job completion
            Boolean jobSucceeded = storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 30, 2000);
            if (!jobSucceeded) {
                throw new CloudRuntimeException("FlexVolume snapshot job failed on FlexVol UUID [" + flexVolUuid + "]");
            }

            // Retrieve the created snapshot UUID by name
            String ontapSnapshotUuid = resolveSnapshotUuid(snapshotClient, authHeader, flexVolUuid, snapshotName);
            if (ontapSnapshotUuid == null || ontapSnapshotUuid.isEmpty()) {
                throw new CloudRuntimeException("Failed to resolve snapshot UUID for snapshot name [" + snapshotName + "]");
            }

            // Set snapshot path for CloudStack (format: snapshotName for identification)
            snapshotObjectTo.setPath(OntapStorageConstants.ONTAP_SNAP_ID + "=" + ontapSnapshotUuid);

            // Persist snapshot_details so deleteAsync can resolve ONTAP FlexVol/snapshot UUIDs
            // (see deleteCloudStackVolumeSnapshot and StorageStrategy.deleteFlexVolSnapshotForCloudStackVolume).
            // Also store source volume min/max IOPS when configured for later create-volume-from-snapshot.
            Map<String, String> snapshotDetails = new LinkedHashMap<>();
            snapshotDetails.put(OntapStorageConstants.SRC_CS_VOLUME_ID, String.valueOf(volumeInfo.getId()));
            snapshotDetails.put(OntapStorageConstants.BASE_ONTAP_FV_ID, flexVolUuid);
            snapshotDetails.put(OntapStorageConstants.ONTAP_SNAP_ID, ontapSnapshotUuid);
            snapshotDetails.put(OntapStorageConstants.ONTAP_SNAP_NAME, snapshotName);
            snapshotDetails.put(OntapStorageConstants.VOLUME_PATH, volumePath);
            snapshotDetails.put(OntapStorageConstants.PRIMARY_POOL_ID, String.valueOf(volumeVO.getPoolId()));
            snapshotDetails.put(OntapStorageConstants.PROTOCOL, protocol);
            // iSCSI only — needed for LUN restore / identity; null (skipped) for NFS.
            snapshotDetails.put(OntapStorageConstants.LUN_DOT_UUID, lunUuid);
            // TODO(CSTACKEX-306): on create-volume-from-snapshot, read these and apply to the new volume.
            putPositiveIopsDetail(snapshotDetails, OntapStorageConstants.MIN_IOPS, volumeVO.getMinIops());
            putPositiveIopsDetail(snapshotDetails, OntapStorageConstants.MAX_IOPS, volumeVO.getMaxIops());
            updateSnapshotDetails(snapshot.getId(), snapshotDetails);

            CreateObjectAnswer createObjectAnswer = new CreateObjectAnswer(snapshotObjectTo);
            result = new CreateCmdResult(null, createObjectAnswer);
            result.setResult(null);

            logger.info("takeSnapshot: Successfully created FlexVolume snapshot [{}] (uuid={}) for volume [{}]",
                    snapshotName, ontapSnapshotUuid, volumeVO.getId());

        } catch (Exception ex) {
            logger.error("takeSnapshot: Failed due to ", ex);
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
            VolumeDetailVO volumeDetails = volumeDetailsDao.findDetail(volumeVO.getId(), OntapStorageConstants.LUN_DOT_NAME);

             if(volumeDetails != null) {
                 String lunName = volumeDetails.getValue();
                 if (lunName == null) {
                     throw new CloudRuntimeException("No LUN name found for volume " + volumeVO.getId());
                 }
                 return lunName;
             }
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
     * <p>Both NFS and iSCSI delegate to CLI-based SFSR:
     * {@code POST /api/private/cli/volume/snapshot/restore-file}</p>
     */
    @Override
    public void revertSnapshot(SnapshotInfo snapshotOnImageStore, SnapshotInfo snapshotOnPrimaryStore,
                               AsyncCompletionCallback<CommandResult> callback) {
        logger.info("OntapPrimaryDatastoreDriver.revertSnapshot: Reverting snapshot [{}]",
                snapshotOnImageStore.getId());

        CommandResult result = new CommandResult();

        try {
            // Use the snapshot that has the ONTAP details stored
            SnapshotInfo snapshot = snapshotOnPrimaryStore != null ? snapshotOnPrimaryStore : snapshotOnImageStore;
            long snapshotId = snapshot.getId();

            // Retrieve snapshot details stored during takeSnapshot
            String flexVolUuid = getSnapshotDetail(snapshotId, OntapStorageConstants.BASE_ONTAP_FV_ID);
            String ontapSnapshotUuid = getSnapshotDetail(snapshotId, OntapStorageConstants.ONTAP_SNAP_ID);
            String snapshotName = getSnapshotDetail(snapshotId, OntapStorageConstants.ONTAP_SNAP_NAME);
            String volumePath = getSnapshotDetail(snapshotId, OntapStorageConstants.VOLUME_PATH);
            String poolIdStr = getSnapshotDetail(snapshotId, OntapStorageConstants.PRIMARY_POOL_ID);
            String protocol = getSnapshotDetail(snapshotId, OntapStorageConstants.PROTOCOL);

            if (flexVolUuid == null || snapshotName == null || volumePath == null || poolIdStr == null) {
                throw new CloudRuntimeException("Missing required snapshot details for snapshot " + snapshotId +
                        " (flexVolUuid=" + flexVolUuid + ", snapshotName=" + snapshotName +
                        ", volumePath=" + volumePath + ", poolId=" + poolIdStr + ")");
            }

            long poolId = Long.parseLong(poolIdStr);
            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(poolId);

            StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(poolDetails);

            // Get the FlexVolume name (required for CLI-based restore API for all protocols)
            String flexVolName = poolDetails.get(OntapStorageConstants.VOLUME_NAME);
            if (flexVolName == null || flexVolName.isEmpty()) {
                throw new CloudRuntimeException("FlexVolume name not found in pool details for pool " + poolId);
            }

            // Prepare protocol-specific parameters (lunUuid is only needed for backward compatibility)
            String lunUuid = null;
            if (ProtocolType.ISCSI.name().equalsIgnoreCase(protocol)) {
                lunUuid = getSnapshotDetail(snapshotId, OntapStorageConstants.LUN_DOT_UUID);
            }

            // Delegate to strategy class for protocol-specific restore
            JobResponse jobResponse = storageStrategy.revertSnapshotForCloudStackVolume(
                    snapshotName, flexVolUuid, ontapSnapshotUuid, volumePath, lunUuid, flexVolName);

            storageStrategy.executeCliSfsrRestore(jobResponse, "revert snapshot [" + snapshotName + "]");

            logger.info("revertSnapshot: Successfully restored {} [{}] from snapshot [{}]",
                    ProtocolType.ISCSI.name().equalsIgnoreCase(protocol) ? "LUN" : "file",
                    volumePath, snapshotName);

            result.setResult(null); // Success

        } catch (Exception ex) {
            logger.error("revertSnapshot: Failed to revert snapshot {}", snapshotOnImageStore, ex);
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
    public void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, QualityOfServiceState qualityOfServiceState) {}

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
    public void provideVmInfo(long vmId, long volumeId) {}

    @Override
    public boolean isVmTagsNeeded(String tagKey) {
        return true;
    }

    @Override
    public void provideVmTags(long vmId, long volumeId, String tagValue) {}

    @Override
    public boolean isStorageSupportHA(Storage.StoragePoolType type) {
        return true;
    }

    @Override
    public void detachVolumeFromAllStorageNodes(Volume volume) {
    }

    private CloudStackVolume createDeleteCloudStackVolumeRequest(StoragePool storagePool, Map<String, String> details, VolumeInfo volumeInfo) {
        CloudStackVolume cloudStackVolumeDeleteRequest = null;

        String protocol = details.get(OntapStorageConstants.PROTOCOL);
        ProtocolType protocolType = ProtocolType.valueOf(protocol);
        switch (protocolType) {
            case NFS3:
                cloudStackVolumeDeleteRequest = new CloudStackVolume();
                cloudStackVolumeDeleteRequest.setDatastoreId(String.valueOf(storagePool.getId()));
                cloudStackVolumeDeleteRequest.setVolumeInfo(volumeInfo);
                break;
            case ISCSI:
                // Retrieve LUN identifiers stored during volume creation
                String lunName = volumeDetailsDao.findDetail(volumeInfo.getId(), OntapStorageConstants.LUN_DOT_NAME).getValue();
                String lunUUID = volumeDetailsDao.findDetail(volumeInfo.getId(), OntapStorageConstants.LUN_DOT_UUID).getValue();
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

    private boolean isIscsi(Map<String, String> details) {
        return ProtocolType.ISCSI.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL));
    }

    /**
     * Resize request. Size is passed separately to {@code resizeCloudStackVolume}.
     * When QoS changes, iSCSI carries the LUN uuid and policy, and NFS carries the FlexVol uuid and file policy.
     */
    private CloudStackVolume resizeCloudStackVolumeRequest(Map<String, String> details, VolumeInfo volumeInfo,
                                                           VolumeQosPolicy qosPolicy) {
        CloudStackVolume resizeRequest = new CloudStackVolume();
        resizeRequest.setVolumeInfo(volumeInfo);
        if (qosPolicy == null) {
            return resizeRequest;
        }
        VolumeQosPolicy qosPolicyReference = new VolumeQosPolicy();
        qosPolicyReference.setName(qosPolicy.getName());
        qosPolicyReference.setUuid(qosPolicy.getUuid());

        String protocol = details.get(OntapStorageConstants.PROTOCOL);
        ProtocolType protocolType = ProtocolType.valueOf(protocol);
        switch (protocolType) {
            case NFS3:
                FileInfo fileInfo = new FileInfo();
                fileInfo.setQosPolicy(qosPolicyReference);
                resizeRequest.setFile(fileInfo);
                resizeRequest.setFlexVolumeUuid(details.get(OntapStorageConstants.VOLUME_UUID));
                return resizeRequest;
            case ISCSI:
                VolumeDetailVO lunUuid = volumeDetailsDao.findDetail(volumeInfo.getId(), OntapStorageConstants.LUN_DOT_UUID);
                if (lunUuid == null || lunUuid.getValue() == null) {
                    throw new CloudRuntimeException("LUN UUID is missing for volume " + volumeInfo.getId());
                }
                Lun lun = new Lun();
                lun.setUuid(lunUuid.getValue());
                lun.setQosPolicy(qosPolicyReference);
                resizeRequest.setLun(lun);
                return resizeRequest;
            default:
                throw new CloudRuntimeException("Unsupported protocol " + protocol);
        }
    }

    /**
     * Builds the request that creates or updates a volume (LUN for iSCSI, qcow2 file for NFS),
     * attaching a QoS policy reference when one is provided.
     */
    private CloudStackVolume createCloudStackVolumeRequestByProtocol(StoragePoolVO storagePool, Map<String, String> details,
                                                                     DataObject volumeObject, VolumeQosPolicy qosPolicy) {
        VolumeQosPolicy qosPolicyReference = null;
        if (qosPolicy != null) {
            qosPolicyReference = new VolumeQosPolicy();
            qosPolicyReference.setName(qosPolicy.getName());
            qosPolicyReference.setUuid(qosPolicy.getUuid());
        }

        String protocol = details.get(OntapStorageConstants.PROTOCOL);
        ProtocolType protocolType = ProtocolType.valueOf(protocol);
        switch (protocolType) {
            case NFS3:
                CloudStackVolume nfsRequest = new CloudStackVolume();
                nfsRequest.setDatastoreId(String.valueOf(storagePool.getId()));
                nfsRequest.setFlexVolumeUuid(details.get(OntapStorageConstants.VOLUME_UUID));
                nfsRequest.setVolumeInfo(volumeObject);
                if (qosPolicyReference != null) {
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setQosPolicy(qosPolicyReference);
                    nfsRequest.setFile(fileInfo);
                }
                return nfsRequest;
            case ISCSI:
                Svm svm = new Svm();
                svm.setName(details.get(OntapStorageConstants.SVM_NAME));
                CloudStackVolume iscsiRequest = new CloudStackVolume();
                Lun lunRequest = new Lun();
                lunRequest.setSvm(svm);

                LunSpace lunSpace = new LunSpace();
                lunSpace.setSize(volumeObject.getSize());
                lunRequest.setSpace(lunSpace);
                String lunName = volumeObject.getName().replace(OntapStorageConstants.HYPHEN, OntapStorageConstants.UNDERSCORE);
                if (!OntapStorageUtils.isValidName(lunName)) {
                    throw new InvalidParameterValueException("Invalid dataObject name [" + lunName
                            + "]. It must start with a letter and can only contain letters, digits, and underscores, and be up to 200 characters long.");
                }
                lunRequest.setName(OntapStorageUtils.getLunName(storagePool.getName(), lunName));
                lunRequest.setOsType(Lun.OsTypeEnum.valueOf(
                        OntapStorageUtils.getOSTypeFromHypervisor(storagePool.getHypervisor().name())));
                lunRequest.setQosPolicy(qosPolicyReference);
                iscsiRequest.setLun(lunRequest);
                return iscsiRequest;
            default:
                throw new CloudRuntimeException("Unsupported protocol " + protocol);
        }
    }

    /**
     * LUN path used to cache a template on this pool: {@code /vol/<flexvol>/cs_tmpl_<id>}.
     */
    private String getTemplateLunName(StoragePoolVO storagePool, long templateId) {
        return OntapStorageUtils.getLunName(storagePool.getName(), OntapStorageConstants.TEMPLATE_LUN_PREFIX + templateId);
    }

    /**
     * Builds the request that clones the cached template LUN into a new volume LUN.
     *
     * <p>Source identity mirrors the NFS file-clone path workflow: {@code clone.source.name} is
     * the same deterministic ONTAP path used at template create
     * ({@code /vol/<flexVol>/cs_tmpl_<templateId>}). {@code local_download_path} (LUN uuid) is
     * still sent as a secondary identity.</p>
     *
     * <p>Size is omitted: ONTAP rejects a size on a clone create, and the clone inherits the
     * source size. Growing to the requested volume size is a separate PATCH.</p>
     */
    private CloudStackVolume createCloneLunRequest(StoragePoolVO storagePool, Map<String, String> details,
                                                   VolumeInfo volumeObject, VMTemplateStoragePoolVO templatePoolRef,
                                                   long templateId, VolumeQosPolicy qosPolicy) {
        String sourceLunUuid = templatePoolRef.getLocalDownloadPath();
        if (sourceLunUuid == null || sourceLunUuid.isEmpty()) {
            throw new CloudRuntimeException("Template [" + templateId + "] has no cached LUN on pool ["
                    + storagePool.getId() + "]; cannot clone volume [" + volumeObject.getId() + "]");
        }

        Svm svm = new Svm();
        svm.setName(details.get(OntapStorageConstants.SVM_NAME));

        String lunName = volumeObject.getName().replace(OntapStorageConstants.HYPHEN, OntapStorageConstants.UNDERSCORE);
        if (!OntapStorageUtils.isValidName(lunName)) {
            throw new InvalidParameterValueException("Invalid dataObject name [" + lunName
                    + "]. It must start with a letter and can only contain letters, digits, and underscores, and be up to 200 characters long.");
        }

        Lun.Source source = new Lun.Source();
        source.setName(getTemplateLunName(storagePool, templateId));
        source.setUuid(sourceLunUuid);
        Lun.Clone clone = new Lun.Clone();
        clone.setSource(source);

        Lun lunRequest = new Lun();
        lunRequest.setSvm(svm);
        lunRequest.setName(OntapStorageUtils.getLunName(storagePool.getName(), lunName));
        lunRequest.setClone(clone);
        if (qosPolicy != null) {
            VolumeQosPolicy qosPolicyReference = new VolumeQosPolicy();
            qosPolicyReference.setName(qosPolicy.getName());
            qosPolicyReference.setUuid(qosPolicy.getUuid());
            lunRequest.setQosPolicy(qosPolicyReference);
        }

        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lunRequest);
        request.setVolumeInfo(volumeObject);
        return request;
    }

    /**
     * Builds the request that clones the cached qcow2 ({@code install_path}) into a new file
     * named after the volume uuid, inside the same FlexVolume.
     */
    private CloudStackVolume createCloneFileRequest(StoragePoolVO storagePool, VolumeInfo volumeInfo,
                                                    VMTemplateStoragePoolVO templatePoolRef, long templateId) {
        String sourcePath = templatePoolRef.getInstallPath();
        if (sourcePath == null || sourcePath.isEmpty()) {
            throw new CloudRuntimeException("Template [" + templateId + "] has no cached file on pool ["
                    + storagePool.getId() + "]; cannot clone volume [" + volumeInfo.getId() + "]");
        }

        FileInfo file = new FileInfo();
        file.setPath(sourcePath);

        CloudStackVolume request = new CloudStackVolume();
        request.setDatastoreId(String.valueOf(storagePool.getId()));
        request.setVolumeInfo(volumeInfo);
        request.setFile(file);
        request.setDestinationPath(volumeInfo.getUuid());
        return request;
    }

    /**
     * Builds the request that clones a LUN out of a FlexVolume snapshot into a new volume LUN.
     *
     * <p>{@code clone.source.name} is {@code /vol/<flexVol>/.snapshot/<snap>/<lun>}; ONTAP cannot
     * resolve a snapshot-resident LUN by uuid, so no uuid is sent.</p>
     */
    private CloudStackVolume createCloneLunFromSnapshotRequest(StoragePoolVO storagePool, Map<String, String> details,
                                                               VolumeInfo volumeInfo, String sourceVolumePath,
                                                               String snapshotName) {
        String lunName = volumeInfo.getName().replace(OntapStorageConstants.HYPHEN, OntapStorageConstants.UNDERSCORE);
        if (!OntapStorageUtils.isValidName(lunName)) {
            throw new CloudRuntimeException("Invalid dataObject name [" + lunName
                    + "]. It must start with a letter and can only contain letters, digits, and underscores, and be up to 200 characters long.");
        }

        String flexVolName = details.get(OntapStorageConstants.VOLUME_NAME);
        if (flexVolName == null || flexVolName.isEmpty()) {
            flexVolName = storagePool.getName();
        }

        Svm svm = new Svm();
        svm.setName(details.get(OntapStorageConstants.SVM_NAME));

        Lun.Source source = new Lun.Source();
        source.setName(OntapStorageUtils.toLunCloneSourcePathInSnapshot(sourceVolumePath, flexVolName, snapshotName));
        Lun.Clone clone = new Lun.Clone();
        clone.setSource(source);

        Lun lunRequest = new Lun();
        lunRequest.setSvm(svm);
        lunRequest.setName(OntapStorageUtils.getLunName(storagePool.getName(), lunName));
        lunRequest.setClone(clone);

        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lunRequest);
        request.setVolumeInfo(volumeInfo);
        return request;
    }

    /**
     * Builds the request that clones a file out of a FlexVolume snapshot into a new file named after
     * the volume uuid, inside the same FlexVolume.
     */
    private CloudStackVolume createCloneFileFromSnapshotRequest(StoragePoolVO storagePool, Map<String, String> details,
                                                                VolumeInfo volumeInfo, String sourceVolumePath,
                                                                String snapshotName) {
        String flexVolName = details.get(OntapStorageConstants.VOLUME_NAME);

        FileInfo file = new FileInfo();
        file.setPath(OntapStorageUtils.toFlexVolRelativePath(sourceVolumePath, flexVolName));

        CloudStackVolume request = new CloudStackVolume();
        request.setDatastoreId(String.valueOf(storagePool.getId()));
        request.setVolumeInfo(volumeInfo);
        request.setFile(file);
        request.setDestinationPath(OntapStorageUtils.toFlexVolRelativePath(volumeInfo.getUuid(), flexVolName));
        request.setSnapshotName(snapshotName);
        return request;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Snapshot Helper Methods
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds an ONTAP-safe snapshot name from the CloudStack UI name with uniqueness suffix.
     */
    private String buildSnapshotName(String cloudStackSnapshotName, long snapshotId) {
        return OntapStorageUtils.buildOntapSnapshotName(cloudStackSnapshotName, OntapStorageConstants.CS + snapshotId);
    }

    /**
     * Only ISCSI and NFS3 pools can be provisioned; any other protocol would fall through
     * createAsync without producing a result, leaving the caller with a null callback value.
     */
    private void validateProtocol(Map<String, String> details, DataStore dataStore) {
        String protocol = details == null ? null : details.get(OntapStorageConstants.PROTOCOL);
        boolean supported = protocol != null && Arrays.stream(ProtocolType.values())
                .anyMatch(type -> type.name().equalsIgnoreCase(protocol));
        if (!supported) {
            throw new CloudRuntimeException("Unsupported protocol [" + protocol + "] on storage pool ["
                    + dataStore.getName() + "]; supported protocols are " + Arrays.toString(ProtocolType.values()));
        }
    }

    private Storage.ImageFormat getImageFormat(StoragePoolVO storagePool) {
        HypervisorType hypervisorType = storagePool.getHypervisor();
        if (!HypervisorType.KVM.equals(hypervisorType)) {
            throw new CloudRuntimeException("Unsupported hypervisor [" + hypervisorType + "] for ONTAP image format resolution");
        }
        Storage.StoragePoolType spType = storagePool.getPoolType();
        switch (spType) {
            case OntapiSCSI:
                return Storage.ImageFormat.RAW;
            case NetworkFilesystem:
                return Storage.ImageFormat.QCOW2;
            default:
                throw new CloudRuntimeException("Unsupported pool type [" +  spType + "] for ONTAP image format resolution");
        }
    }

    /**
     * Persists ONTAP snapshot metadata in {@code snapshot_details} for revert, delete, and
     * create-volume-from-snapshot.
     *
     * <p>Volume-snapshot delete reads {@code base_ontap_fv_id} and {@code ontap_snap_id} here
     * during {@link #deleteCloudStackVolumeSnapshot}; missing rows prevent ONTAP cleanup.</p>
     *
     * <p>All rows go through {@link #persistSnapshotDetail} so DAO writes stay consistent;
     * optional fields (LUN uuid, IOPS) are skipped when unset.</p>
     *
     * @param csSnapshotId CloudStack snapshot ID
     * @param details      {@code snapshot_details} name to value, keyed by {@link OntapStorageConstants};
     *                     entries with null or blank values are skipped
     */
    private void updateSnapshotDetails(long csSnapshotId, Map<String, String> details) {
        for (Map.Entry<String, String> detail : details.entrySet()) {
            persistSnapshotDetail(csSnapshotId, detail.getKey(), detail.getValue());
        }
    }

    /**
     * Adds an IOPS entry to {@code details} only when {@code iops} is non-null and positive.
     */
    private static void putPositiveIopsDetail(Map<String, String> details, String detailName, Long iops) {
        if (iops != null && iops > 0) {
            details.put(detailName, String.valueOf(iops));
        }
    }

    /**
     * Persists one {@code snapshot_details} row. No-op when {@code value} is null or blank so
     * optional keys (e.g. LUN uuid on NFS) share the same DAO path as required keys.
     */
    private void persistSnapshotDetail(long csSnapshotId, String detailName, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        snapshotDetailsDao.persist(new SnapshotDetailsVO(csSnapshotId, detailName, value, false));
    }

}
