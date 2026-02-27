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
package org.apache.cloudstack.storage.snapshot;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.client.SnapshotFeignClient;
import org.apache.cloudstack.storage.feign.model.FlexVolSnapshot;
import org.apache.cloudstack.storage.feign.model.SnapshotFileRestoreRequest;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

/**
 * Snapshot strategy for individual CloudStack volume snapshots on ONTAP managed storage,
 * using ONTAP FlexVolume-level snapshots.
 *
 * <p>This strategy intercepts volume-level snapshot operations (TAKE, REVERT, DELETE)
 * for volumes residing on NetApp ONTAP managed primary storage. Instead of delegating
 * to the hypervisor or using file-level clones, it creates <b>ONTAP FlexVolume-level
 * snapshots</b> directly via the ONTAP REST API.</p>
 *
 * <h3>Key Differences from VM Snapshots ({@code OntapVMSnapshotStrategy}):</h3>
 * <ul>
 *   <li>VM snapshots capture <i>all</i> volumes of a VM in one atomic operation</li>
 *   <li>This strategy captures a <i>single</i> CloudStack volume at a time</li>
 *   <li>Uses different CloudStack interfaces ({@code SnapshotStrategy} vs {@code VMSnapshotStrategy})</li>
 *   <li>Persists metadata in {@code snapshot_details} (not {@code vm_snapshot_details})</li>
 * </ul>
 *
 * <h3>Take:</h3>
 * <p>Creates an ONTAP FlexVolume snapshot via
 * {@code POST /api/storage/volumes/{uuid}/snapshots} and records the snapshot UUID,
 * FlexVol UUID, volume path, pool ID, and protocol in {@code snapshot_details}.</p>
 *
 * <h3>Revert:</h3>
 * <p>Uses ONTAP Snapshot File Restore API
 * ({@code POST /api/storage/volumes/{vol}/snapshots/{snap}/files/{path}/restore})
 * to restore only the individual file or LUN belonging to this CloudStack volume,
 * without reverting the entire FlexVolume (which may contain other volumes).</p>
 *
 * <h3>Delete:</h3>
 * <p>Deletes the ONTAP FlexVolume snapshot via
 * {@code DELETE /api/storage/volumes/{uuid}/snapshots/{uuid}} and removes
 * the corresponding {@code snapshot_details} rows.</p>
 */
public class OntapSnapshotStrategy extends SnapshotStrategyBase {

    private static final Logger logger = LogManager.getLogger(OntapSnapshotStrategy.class);

    /** Separator used in snapshot_details value to delimit fields. */
    static final String DETAIL_SEPARATOR = "::";

    /** Key used in snapshot_details for the ONTAP FlexVol snapshot metadata. */
    static final String ONTAP_FLEXVOL_SNAPSHOT_DETAIL = "ontapFlexVolVolumeSnapshot";

    @Inject
    private SnapshotDao snapshotDao;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private VolumeDetailsDao volumeDetailsDao;

    @Inject
    private SnapshotDetailsDao snapshotDetailsDao;

    @Inject
    private SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    private PrimaryDataStoreDao storagePoolDao;

    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;

    // ──────────────────────────────────────────────────────────────────────────
    // Strategy Selection
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, Long zoneId, SnapshotOperation op) {
        if (SnapshotOperation.COPY.equals(op) || SnapshotOperation.BACKUP.equals(op)) {
            return StrategyPriority.CANT_HANDLE;
        }

        // For DELETE and REVERT, check if this snapshot was created by us
        if (SnapshotOperation.DELETE.equals(op) || SnapshotOperation.REVERT.equals(op)) {
            SnapshotDetailsVO flexVolDetail = snapshotDetailsDao.findDetail(snapshot.getId(), ONTAP_FLEXVOL_SNAPSHOT_DETAIL);
            if (flexVolDetail != null && flexVolDetail.getValue() != null) {
                return StrategyPriority.HIGHEST;
            }
            return StrategyPriority.CANT_HANDLE;
        }

        // For TAKE, check if the volume is on ONTAP managed storage
        if (SnapshotOperation.TAKE.equals(op)) {
            return isVolumeOnOntapManagedStorage(snapshot.getVolumeId())
                    ? StrategyPriority.HIGHEST
                    : StrategyPriority.CANT_HANDLE;
        }

        return StrategyPriority.CANT_HANDLE;
    }

    /**
     * Checks whether the given volume resides on ONTAP managed primary storage.
     */
    boolean isVolumeOnOntapManagedStorage(long volumeId) {
        VolumeVO volumeVO = volumeDao.findById(volumeId);
        if (volumeVO == null || volumeVO.getPoolId() == null) {
            return false;
        }

        StoragePoolVO pool = storagePoolDao.findById(volumeVO.getPoolId());
        if (pool == null) {
            return false;
        }

        if (!pool.isManaged()) {
            return false;
        }

        if (!Constants.ONTAP_PLUGIN_NAME.equals(pool.getStorageProviderName())) {
            return false;
        }

        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Take Snapshot
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Takes a volume-level snapshot by creating an ONTAP FlexVolume snapshot.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Determine the FlexVol UUID and protocol from pool details</li>
     *   <li>Create ONTAP FlexVolume snapshot via REST API</li>
     *   <li>Resolve the snapshot UUID by name</li>
     *   <li>Resolve the volume path on ONTAP (file path for NFS, LUN path for iSCSI)</li>
     *   <li>Persist metadata in {@code snapshot_details}</li>
     *   <li>Mark the snapshot as BackedUp</li>
     * </ol>
     */
    @Override
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshotInfo) {
        logger.info("OntapSnapshotStrategy.takeSnapshot: Taking FlexVol snapshot for CS snapshot [{}]",
                snapshotInfo.getId());

        SnapshotObject snapshotObject = (SnapshotObject) snapshotInfo;

        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshotInfo.getId());
        if (snapshotVO == null) {
            throw new CloudRuntimeException("Failed to acquire lock on snapshot [" + snapshotInfo.getId() + "]");
        }

        try {
            VolumeVO volumeVO = volumeDao.findById(snapshotInfo.getVolumeId());
            if (volumeVO == null) {
                throw new CloudRuntimeException("Volume not found for snapshot [" + snapshotInfo.getId() + "]");
            }

            // Transit volume state
            snapshotInfo.getBaseVolume().stateTransit(Volume.Event.SnapshotRequested);

            boolean success = false;
            try {
                Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(volumeVO.getPoolId());
                String flexVolUuid = poolDetails.get(Constants.VOLUME_UUID);
                String protocol = poolDetails.get(Constants.PROTOCOL);

                if (flexVolUuid == null || flexVolUuid.isEmpty()) {
                    throw new CloudRuntimeException("FlexVolume UUID not found in pool details for pool [" + volumeVO.getPoolId() + "]");
                }

                // Build ONTAP storage strategy & Feign client
                StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);
                SnapshotFeignClient snapshotClient = storageStrategy.getSnapshotFeignClient();
                String authHeader = storageStrategy.getAuthHeader();

                // Build snapshot name
                String snapshotName = buildSnapshotName(snapshotInfo);

                // Create FlexVolume snapshot
                FlexVolSnapshot snapshotRequest = new FlexVolSnapshot(snapshotName,
                        "CloudStack volume snapshot " + snapshotInfo.getName() + " for volume " + volumeVO.getUuid());

                logger.info("Creating ONTAP FlexVol snapshot [{}] on FlexVol [{}] for volume [{}]",
                        snapshotName, flexVolUuid, volumeVO.getId());

                JobResponse jobResponse = snapshotClient.createSnapshot(authHeader, flexVolUuid, snapshotRequest);
                if (jobResponse == null || jobResponse.getJob() == null) {
                    throw new CloudRuntimeException("Failed to initiate FlexVol snapshot on FlexVol [" + flexVolUuid + "]");
                }

                Boolean jobSucceeded = storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 30, 2);
                if (!jobSucceeded) {
                    throw new CloudRuntimeException("FlexVol snapshot job failed on FlexVol [" + flexVolUuid + "]");
                }

                // Resolve the snapshot UUID by name
                String ontapSnapshotUuid = resolveSnapshotUuid(snapshotClient, authHeader, flexVolUuid, snapshotName);

                // Resolve volume path on ONTAP
                String volumePath = resolveVolumePathOnOntap(volumeVO.getId(), protocol, poolDetails);

                // Persist snapshot detail (one row with all metadata)
                OntapSnapshotDetail detail = new OntapSnapshotDetail(
                        flexVolUuid, ontapSnapshotUuid, snapshotName, volumePath, volumeVO.getPoolId(), protocol);
                snapshotDetailsDao.persist(new SnapshotDetailsVO(snapshotInfo.getId(),
                        ONTAP_FLEXVOL_SNAPSHOT_DETAIL, detail.toString(), false));

                // Also persist individual detail keys for compatibility with existing driver code
                snapshotDetailsDao.persist(new SnapshotDetailsVO(snapshotInfo.getId(),
                        Constants.SRC_CS_VOLUME_ID, String.valueOf(volumeVO.getId()), false));
                snapshotDetailsDao.persist(new SnapshotDetailsVO(snapshotInfo.getId(),
                        Constants.BASE_ONTAP_FV_ID, flexVolUuid, false));
                snapshotDetailsDao.persist(new SnapshotDetailsVO(snapshotInfo.getId(),
                        Constants.ONTAP_SNAP_ID, ontapSnapshotUuid, false));
                snapshotDetailsDao.persist(new SnapshotDetailsVO(snapshotInfo.getId(),
                        Constants.PRIMARY_POOL_ID, String.valueOf(volumeVO.getPoolId()), false));
                snapshotDetailsDao.persist(new SnapshotDetailsVO(snapshotInfo.getId(),
                        Constants.FILE_PATH, volumePath, false));

                // Transition snapshot state to BackedUp
                snapshotObject.processEvent(Snapshot.Event.BackupToSecondary);
                snapshotObject.processEvent(Snapshot.Event.OperationSucceeded);

                logger.info("ONTAP FlexVol snapshot [{}] (uuid={}) created successfully for CS snapshot [{}] on FlexVol [{}]",
                        snapshotName, ontapSnapshotUuid, snapshotInfo.getId(), flexVolUuid);

                success = true;
                return snapshotInfo;

            } finally {
                if (success) {
                    snapshotInfo.getBaseVolume().stateTransit(Volume.Event.OperationSucceeded);
                } else {
                    snapshotInfo.getBaseVolume().stateTransit(Volume.Event.OperationFailed);
                }
            }

        } catch (NoTransitionException e) {
            logger.error("State transition error during ONTAP snapshot: {}", e.getMessage(), e);
            throw new CloudRuntimeException("State transition error during ONTAP snapshot: " + e.getMessage(), e);
        } finally {
            snapshotDao.releaseFromLockTable(snapshotInfo.getId());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Revert Snapshot
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Reverts a volume to an ONTAP FlexVolume snapshot using single-file restore.
     *
     * <p>Uses the ONTAP Snapshot File Restore API
     * ({@code POST /api/storage/volumes/{vol}/snapshots/{snap}/files/{path}/restore})
     * to restore only the specific file or LUN from the snapshot, without reverting
     * the entire FlexVolume.</p>
     */
    @Override
    public boolean revertSnapshot(SnapshotInfo snapshotInfo) {
        logger.info("OntapSnapshotStrategy.revertSnapshot: Reverting CS snapshot [{}] using ONTAP single-file restore",
                snapshotInfo.getId());

        SnapshotDetailsVO flexVolDetailVO = snapshotDetailsDao.findDetail(snapshotInfo.getId(), ONTAP_FLEXVOL_SNAPSHOT_DETAIL);
        if (flexVolDetailVO == null || flexVolDetailVO.getValue() == null) {
            throw new CloudRuntimeException("ONTAP FlexVol snapshot detail not found for snapshot [" + snapshotInfo.getId() + "]");
        }

        OntapSnapshotDetail detail = OntapSnapshotDetail.parse(flexVolDetailVO.getValue());

        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshotInfo.getId());
        if (snapshotVO == null) {
            throw new CloudRuntimeException("Failed to acquire lock on snapshot [" + snapshotInfo.getId() + "]");
        }

        try {
            VolumeVO volumeVO = volumeDao.findById(snapshotInfo.getVolumeId());
            if (volumeVO == null) {
                throw new CloudRuntimeException("Volume not found for snapshot [" + snapshotInfo.getId() + "]");
            }

            // Transit volume state
            snapshotInfo.getBaseVolume().stateTransit(Volume.Event.RevertSnapshotRequested);

            boolean success = false;
            try {
                Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(detail.poolId);
                StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);
                SnapshotFeignClient snapshotClient = storageStrategy.getSnapshotFeignClient();
                String authHeader = storageStrategy.getAuthHeader();

                logger.info("Restoring volume path [{}] from FlexVol snapshot [{}] (uuid={}) on FlexVol [{}] (protocol={})",
                        detail.volumePath, detail.snapshotName, detail.snapshotUuid,
                        detail.flexVolUuid, detail.protocol);

                // POST /api/storage/volumes/{vol}/snapshots/{snap}/files/{path}/restore
                SnapshotFileRestoreRequest restoreRequest = new SnapshotFileRestoreRequest(detail.volumePath);

                JobResponse jobResponse = snapshotClient.restoreFileFromSnapshot(
                        authHeader, detail.flexVolUuid, detail.snapshotUuid, detail.volumePath, restoreRequest);

                if (jobResponse != null && jobResponse.getJob() != null) {
                    Boolean jobSucceeded = storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 60, 2);
                    if (!jobSucceeded) {
                        throw new CloudRuntimeException("Snapshot file restore failed for volume path [" +
                                detail.volumePath + "] from snapshot [" + detail.snapshotName +
                                "] on FlexVol [" + detail.flexVolUuid + "]");
                    }
                }

                logger.info("Successfully restored volume [{}] from ONTAP snapshot [{}] on FlexVol [{}]",
                        detail.volumePath, detail.snapshotName, detail.flexVolUuid);

                success = true;
                return true;

            } finally {
                if (success) {
                    snapshotInfo.getBaseVolume().stateTransit(Volume.Event.OperationSucceeded);
                } else {
                    snapshotInfo.getBaseVolume().stateTransit(Volume.Event.OperationFailed);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to revert ONTAP snapshot [{}]: {}", snapshotInfo.getId(), e.getMessage(), e);
            throw new CloudRuntimeException("Failed to revert ONTAP snapshot: " + e.getMessage(), e);
        } finally {
            snapshotDao.releaseFromLockTable(snapshotInfo.getId());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Delete Snapshot
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Deletes an ONTAP FlexVolume snapshot and cleans up the corresponding
     * {@code snapshot_details} rows.
     */
    @Override
    public boolean deleteSnapshot(Long snapshotId, Long zoneId) {
        logger.info("OntapSnapshotStrategy.deleteSnapshot: Deleting ONTAP FlexVol snapshot for CS snapshot [{}]",
                snapshotId);

        SnapshotVO snapshotVO = snapshotDao.findById(snapshotId);
        if (snapshotVO == null) {
            logger.warn("Snapshot [{}] not found in database, nothing to delete", snapshotId);
            return true;
        }

        if (Snapshot.State.Destroyed.equals(snapshotVO.getState())) {
            return true;
        }

        if (Snapshot.State.Error.equals(snapshotVO.getState())) {
            cleanupSnapshotDetails(snapshotId);
            snapshotDao.remove(snapshotId);
            return true;
        }

        SnapshotDetailsVO flexVolDetailVO = snapshotDetailsDao.findDetail(snapshotId, ONTAP_FLEXVOL_SNAPSHOT_DETAIL);
        if (flexVolDetailVO == null || flexVolDetailVO.getValue() == null) {
            logger.warn("ONTAP FlexVol snapshot detail not found for snapshot [{}], cleaning up DB only", snapshotId);
            cleanupSnapshotDetails(snapshotId);
            markSnapshotDestroyed(snapshotVO);
            return true;
        }

        OntapSnapshotDetail detail = OntapSnapshotDetail.parse(flexVolDetailVO.getValue());

        try {
            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(detail.poolId);
            StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);
            SnapshotFeignClient snapshotClient = storageStrategy.getSnapshotFeignClient();
            String authHeader = storageStrategy.getAuthHeader();

            logger.info("Deleting ONTAP FlexVol snapshot [{}] (uuid={}) on FlexVol [{}]",
                    detail.snapshotName, detail.snapshotUuid, detail.flexVolUuid);

            JobResponse jobResponse = snapshotClient.deleteSnapshot(authHeader, detail.flexVolUuid, detail.snapshotUuid);
            if (jobResponse != null && jobResponse.getJob() != null) {
                storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 30, 2);
            }

            logger.info("Deleted ONTAP FlexVol snapshot [{}] on FlexVol [{}]", detail.snapshotName, detail.flexVolUuid);

        } catch (Exception e) {
            logger.error("Failed to delete ONTAP FlexVol snapshot [{}] on FlexVol [{}]: {}",
                    detail.snapshotName, detail.flexVolUuid, e.getMessage(), e);
            // Continue with DB cleanup even if ONTAP delete fails
        }

        // Clean up DB
        cleanupSnapshotDetails(snapshotId);
        markSnapshotDestroyed(snapshotVO);

        // Remove snapshot_data_store_ref entries
        SnapshotDataStoreVO snapshotStoreRef = snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(
                snapshotId, DataStoreRole.Primary);
        if (snapshotStoreRef != null) {
            snapshotDataStoreDao.remove(snapshotStoreRef.getId());
        }

        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Post-snapshot creation (backup to secondary)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * For ONTAP snapshots, the snapshot already lives on the primary (ONTAP) storage.
     * No additional backup to secondary is needed, so this is a no-op.
     */
    @Override
    public void postSnapshotCreation(SnapshotInfo snapshot) {
        // ONTAP FlexVol snapshots are stored on primary storage.
        // No need to back up to secondary.
        logger.debug("OntapSnapshotStrategy.postSnapshotCreation: No secondary backup needed for ONTAP snapshot [{}]",
                snapshot.getId());
    }

    /**
     * Backup is not needed for ONTAP snapshots – they live on primary storage.
     */
    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshotInfo) {
        logger.debug("OntapSnapshotStrategy.backupSnapshot: ONTAP snapshot [{}] lives on primary, marking as backed up",
                snapshotInfo.getId());
        try {
            SnapshotObject snapshotObject = (SnapshotObject) snapshotInfo;
            snapshotObject.processEvent(Snapshot.Event.BackupToSecondary);
            snapshotObject.processEvent(Snapshot.Event.OperationSucceeded);
        } catch (NoTransitionException e) {
            logger.warn("Failed to transition snapshot state during backup: {}", e.getMessage());
        }
        return snapshotInfo;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds a deterministic, ONTAP-safe snapshot name for a volume snapshot.
     * Format: {@code volsnap_<snapshotId>_<timestamp>}
     */
    String buildSnapshotName(SnapshotInfo snapshot) {
        String name = "volsnap_" + snapshot.getId() + "_" + System.currentTimeMillis();
        if (name.length() > Constants.MAX_SNAPSHOT_NAME_LENGTH) {
            name = name.substring(0, Constants.MAX_SNAPSHOT_NAME_LENGTH);
        }
        return name;
    }

    /**
     * Resolves the UUID of a newly created FlexVolume snapshot by name.
     */
    String resolveSnapshotUuid(SnapshotFeignClient client, String authHeader,
                               String flexVolUuid, String snapshotName) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("name", snapshotName);
        OntapResponse<FlexVolSnapshot> response = client.getSnapshots(authHeader, flexVolUuid, queryParams);
        if (response == null || response.getRecords() == null || response.getRecords().isEmpty()) {
            throw new CloudRuntimeException("Could not find FlexVol snapshot [" + snapshotName +
                    "] on FlexVol [" + flexVolUuid + "] after creation");
        }
        return response.getRecords().get(0).getUuid();
    }

    /**
     * Resolves the ONTAP-side path of a CloudStack volume within its FlexVolume.
     *
     * <ul>
     *   <li>NFS: filename from {@code volumeVO.getPath()} (e.g. {@code uuid.qcow2})</li>
     *   <li>iSCSI: LUN name from volume_details (e.g. {@code /vol/vol1/lun_name})</li>
     * </ul>
     */
    String resolveVolumePathOnOntap(Long volumeId, String protocol, Map<String, String> poolDetails) {
        if (ProtocolType.ISCSI.name().equalsIgnoreCase(protocol)) {
            VolumeDetailVO lunDetail = volumeDetailsDao.findDetail(volumeId, Constants.LUN_DOT_NAME);
            if (lunDetail == null || lunDetail.getValue() == null || lunDetail.getValue().isEmpty()) {
                throw new CloudRuntimeException(
                        "LUN name (volume detail '" + Constants.LUN_DOT_NAME + "') not found for iSCSI volume [" + volumeId + "]");
            }
            return lunDetail.getValue();
        } else {
            VolumeVO vol = volumeDao.findById(volumeId);
            if (vol == null || vol.getPath() == null || vol.getPath().isEmpty()) {
                throw new CloudRuntimeException("Volume path not found for NFS volume [" + volumeId + "]");
            }
            return vol.getPath();
        }
    }

    /**
     * Removes all snapshot_details rows for the given snapshot ID that were created by this strategy.
     */
    private void cleanupSnapshotDetails(long snapshotId) {
        snapshotDetailsDao.removeDetails(snapshotId);
    }

    /**
     * Marks a snapshot as Destroyed in the database.
     */
    private void markSnapshotDestroyed(SnapshotVO snapshotVO) {
        snapshotVO.setState(Snapshot.State.Destroyed);
        snapshotDao.update(snapshotVO.getId(), snapshotVO);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Inner class: OntapSnapshotDetail
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Holds the metadata for a volume's FlexVol snapshot entry persisted in {@code snapshot_details}.
     *
     * <p>Serialized format:
     * {@code "<flexVolUuid>::<snapshotUuid>::<snapshotName>::<volumePath>::<poolId>::<protocol>"}</p>
     */
    static class OntapSnapshotDetail {
        final String flexVolUuid;
        final String snapshotUuid;
        final String snapshotName;
        final String volumePath;
        final long poolId;
        final String protocol;

        OntapSnapshotDetail(String flexVolUuid, String snapshotUuid, String snapshotName,
                            String volumePath, long poolId, String protocol) {
            this.flexVolUuid = flexVolUuid;
            this.snapshotUuid = snapshotUuid;
            this.snapshotName = snapshotName;
            this.volumePath = volumePath;
            this.poolId = poolId;
            this.protocol = protocol;
        }

        static OntapSnapshotDetail parse(String value) {
            String[] parts = value.split(DETAIL_SEPARATOR);
            if (parts.length != 6) {
                throw new CloudRuntimeException("Invalid ONTAP FlexVol volume snapshot detail format: " + value);
            }
            return new OntapSnapshotDetail(parts[0], parts[1], parts[2], parts[3], Long.parseLong(parts[4]), parts[5]);
        }

        @Override
        public String toString() {
            return flexVolUuid + DETAIL_SEPARATOR + snapshotUuid + DETAIL_SEPARATOR + snapshotName +
                    DETAIL_SEPARATOR + volumePath + DETAIL_SEPARATOR + poolId + DETAIL_SEPARATOR + protocol;
        }
    }
}
