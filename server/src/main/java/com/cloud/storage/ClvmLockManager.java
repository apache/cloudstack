package com.cloud.storage;

import javax.inject.Inject;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.ClvmLockTransferCommand;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ClvmLockManager {
    @Inject
    private VolumeDetailsDao _volsDetailsDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private HostDao _hostDao;

    protected Logger logger = LogManager.getLogger(getClass());

    public Long getClvmLockHostId(Long volumeId, String volumeUuid) {
        VolumeDetailVO detail = _volsDetailsDao.findDetail(volumeId, VolumeInfo.CLVM_LOCK_HOST_ID);
        if (detail != null && detail.getValue() != null && !detail.getValue().isEmpty()) {
            try {
                return Long.parseLong(detail.getValue());
            } catch (NumberFormatException e) {
                logger.warn("Invalid clvmLockHostId in volume_details for volume {}: {}",
                        volumeUuid, detail.getValue());
            }
        }
        return null;
    }

    /**
     * Safely sets or updates the CLVM_LOCK_HOST_ID detail for a volume.
     * If the detail already exists, it will be updated. Otherwise, it will be created.
     *
     * @param volumeId The ID of the volume
     * @param hostId The host ID that holds/should hold the CLVM exclusive lock
     */
    public void setClvmLockHostId(long volumeId, long hostId) {
        VolumeDetailVO existingDetail = _volsDetailsDao.findDetail(volumeId, VolumeInfo.CLVM_LOCK_HOST_ID);
        if (existingDetail != null) {
            existingDetail.setValue(String.valueOf(hostId));
            _volsDetailsDao.update(existingDetail.getId(), existingDetail);
            logger.debug("Updated CLVM_LOCK_HOST_ID for volume {} to host {}", volumeId, hostId);
            return;
        }
        _volsDetailsDao.addDetail(volumeId, VolumeInfo.CLVM_LOCK_HOST_ID, String.valueOf(hostId), false);
        logger.debug("Created CLVM_LOCK_HOST_ID for volume {} with host {}", volumeId, hostId);
    }

    /**
     * Cleans up CLVM lock host tracking detail from volume_details table.
     * Called after successful volume deletion to prevent orphaned records.
     *
     * @param volume The volume being deleted
     */
    public void clearClvmLockHostDetail(VolumeVO volume) {
        try {
            VolumeDetailVO detail = _volsDetailsDao.findDetail(volume.getId(), VolumeInfo.CLVM_LOCK_HOST_ID);
            if (detail != null) {
                logger.debug("Removing CLVM lock host detail for deleted volume {}", volume.getUuid());
                _volsDetailsDao.remove(detail.getId());
            }
        } catch (Exception e) {
            logger.warn("Failed to clean up CLVM lock host detail for volume {}: {}",
                    volume.getUuid(), e.getMessage());
        }
    }

    public boolean transferClvmVolumeLock(String volumeUuid, Long volumeId, String volumePath,
                                          StoragePoolVO pool, Long sourceHostId, Long destHostId) {
        if (pool == null) {
            logger.error("Cannot transfer CLVM lock for volume {} - pool is null", volumeUuid);
            return false;
        }

        String vgName = pool.getPath();
        if (vgName.startsWith("/")) {
            vgName = vgName.substring(1);
        }

        String lvPath = String.format("/dev/%s/%s", vgName, volumePath);

        try {
            if (!sourceHostId.equals(destHostId)) {
                HostVO sourceHost = _hostDao.findById(sourceHostId);
                if (sourceHost != null && sourceHost.getStatus() == Status.Up) {
                    ClvmLockTransferCommand deactivateCmd = new ClvmLockTransferCommand(
                            ClvmLockTransferCommand.Operation.DEACTIVATE,
                            lvPath,
                            volumeUuid
                    );

                    Answer deactivateAnswer = _agentMgr.send(sourceHostId, deactivateCmd);

                    if (deactivateAnswer == null || !deactivateAnswer.getResult()) {
                        logger.warn("Failed to deactivate CLVM volume {} on source host {}. Will attempt activation on destination.",
                                volumeUuid, sourceHostId);
                    }
                } else {
                    logger.warn("Source host {} is down. Will attempt force claim on destination host {}",
                            sourceHostId, destHostId);
                }
            }

            ClvmLockTransferCommand activateCmd = new ClvmLockTransferCommand(
                    ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE,
                    lvPath,
                    volumeUuid
            );

            Answer activateAnswer = _agentMgr.send(destHostId, activateCmd);

            if (activateAnswer == null || !activateAnswer.getResult()) {
                String error = activateAnswer != null ? activateAnswer.getDetails() : "null answer";
                logger.error("Failed to activate CLVM volume {} exclusively on dest host {}: {}",
                        volumeUuid, destHostId, error);
                return false;
            }

            setClvmLockHostId(volumeId, destHostId);

            logger.info("Successfully transferred CLVM lock for volume {} from host {} to host {}",
                    volumeUuid, sourceHostId, destHostId);

            return true;

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.error("Exception during CLVM lock transfer for volume {}: {}", volumeUuid, e.getMessage(), e);
            return false;
        }
    }
}

