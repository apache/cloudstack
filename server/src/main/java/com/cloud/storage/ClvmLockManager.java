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
package com.cloud.storage;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.dao.VolumeDetailsDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.ClvmLockTransferCommand;
import org.apache.cloudstack.storage.command.ClvmLockTransferAnswer;
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

    public static boolean isClvmPoolType(Storage.StoragePoolType poolType) {
        return Arrays.asList(Storage.StoragePoolType.CLVM, Storage.StoragePoolType.CLVM_NG).contains(poolType);
    }

    /**
     * Gets the CLVM lock host ID for a volume, optionally querying actual LVM state.
     *
     * @param volumeId The volume ID
     * @param volumeUuid The volume UUID
     * @return Host ID that holds the lock, or null if not found
     * @deprecated Use getClvmLockHostId(volumeId, volumeUuid, volumePath, pool, queryActual) instead
     */
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
     * Gets the CLVM lock host ID for a volume, optionally querying actual LVM state.
     * This method can query the actual lock state from LVM (source of truth) instead of
     * relying solely on potentially stale database records.
     *
     * @param volumeId The volume ID
     * @param volumeUuid The volume UUID
     * @param volumePath The LV path (required if queryActual is true)
     * @param pool The storage pool (required if queryActual is true)
     * @param queryActual If true, queries actual LVM state instead of database
     * @return Host ID that holds the lock, or null if not found
     */
    public Long getClvmLockHostId(Long volumeId, String volumeUuid, String volumePath,
                                  StoragePool pool, boolean queryActual) {
        if (queryActual) {
            if (volumePath == null || pool == null) {
                logger.warn("Cannot query actual CLVM lock state for volume {} - missing volumePath or pool", volumeUuid);
                return getClvmLockHostId(volumeId, volumeUuid);
            }
            return queryCurrentLockHolder(volumeId, volumeUuid, volumePath, pool, true);
        }

        return getClvmLockHostId(volumeId, volumeUuid);
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
     * Query LVM to find the actual current lock holder for a volume.
     * This is the SOURCE OF TRUTH - it queries the actual LVM state via sanlock/lvmlockd.
     *
     * @param volumeId The volume ID
     * @param volumeUuid The volume UUID
     * @param volumePath The LV path (e.g., "vm-123-disk-0")
     * @param pool The storage pool
     * @param updateDatabase If true, updates the database with the actual value (for debugging/audit)
     * @return Host ID of current lock holder, or null if no lock is held or query failed
     */
    public Long queryCurrentLockHolder(Long volumeId, String volumeUuid, String volumePath,
                                       StoragePool pool, boolean updateDatabase) {
        if (pool == null) {
            logger.error("Cannot query CLVM lock for volume {} - pool is null", volumeUuid);
            return null;
        }

        List<HostVO> hosts = null;

        Long clusterId = pool.getClusterId();
        if (clusterId != null) {
            hosts = _hostDao.findByClusterId(clusterId, Host.Type.Routing);
            logger.debug("Found {} routing hosts in cluster {} for pool {}",
                    hosts != null ? hosts.size() : 0, clusterId, pool.getName());
        }

        if ((hosts == null || hosts.isEmpty()) && pool.getDataCenterId() > 0) {
            logger.debug("Pool {} is zone-scoped or no hosts in cluster, checking zone {} for available routing hosts",
                    pool.getName(), pool.getDataCenterId());
            hosts = _hostDao.findByDataCenterId(pool.getDataCenterId());
        }

        if (hosts == null || hosts.isEmpty()) {
            logger.warn("No KVM routing hosts found to query CLVM lock state for volume {} (pool: {}, cluster: {}, zone: {})",
                    volumeUuid, pool.getName(), clusterId, pool.getDataCenterId());
            return null;
        }

        logger.debug("Querying lock state for volume {} from {} available hosts", volumeUuid, hosts.size());

        for (HostVO host : hosts) {
            if (host.getStatus() != Status.Up ||
                host.getType() != com.cloud.host.Host.Type.Routing ||
                host.getHypervisorType() != Hypervisor.HypervisorType.KVM) {
                continue;
            }

            String vgName = pool.getPath();
            if (vgName.startsWith("/")) {
                vgName = vgName.substring(1);
            }
            String lvPath = String.format("/dev/%s/%s", vgName, volumePath);

            try {
                ClvmLockTransferCommand queryCmd = new ClvmLockTransferCommand(
                        ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE,
                        lvPath,
                        volumeUuid
                );

                Answer answer = _agentMgr.send(host.getId(), queryCmd);

                if (answer == null || !answer.getResult()) {
                    logger.debug("Failed to query lock state from host {}: {}",
                            host.getId(), answer != null ? answer.getDetails() : "null answer");
                    continue;
                }

                if (!(answer instanceof ClvmLockTransferAnswer)) {
                    logger.warn("Unexpected answer type for query lock state: {}", answer.getClass());
                    continue;
                }

                ClvmLockTransferAnswer queryAnswer = (ClvmLockTransferAnswer) answer;
                String hostname = queryAnswer.getCurrentLockHostname();

                if (hostname == null || hostname.isEmpty()) {
                    logger.debug("Volume {} is not locked (no exclusive lock held)", volumeUuid);
                    if (updateDatabase) {
                        VolumeDetailVO detail = _volsDetailsDao.findDetail(volumeId, VolumeInfo.CLVM_LOCK_HOST_ID);
                        if (detail != null) {
                            _volsDetailsDao.remove(detail.getId());
                        }
                    }
                    return null;
                }

                HostVO lockHost = _hostDao.findByName(hostname);
                if (lockHost == null) {
                    logger.warn("Could not resolve hostname {} to host ID for volume {}",
                            hostname, volumeUuid);
                    return null;
                }

                Long lockHostId = lockHost.getId();
                logger.info("Queried CLVM lock state for volume {}: locked by host {} ({}), exclusive={}",
                        volumeUuid, lockHostId, hostname, queryAnswer.isExclusive());

                if (updateDatabase) {
                    Long dbHostId = getClvmLockHostId(volumeId, volumeUuid);
                    if (dbHostId == null || !dbHostId.equals(lockHostId)) {
                        logger.info("Correcting database: volume {} lock host: {} -> {} (actual)",
                                volumeUuid, dbHostId, lockHostId);
                        setClvmLockHostId(volumeId, lockHostId);
                    }
                }

                return lockHostId;

            } catch (AgentUnavailableException | OperationTimedoutException e) {
                logger.debug("Could not query host {} for lock state: {}", host.getId(), e.getMessage());
            }
        }

        logger.warn("Could not query CLVM lock state for volume {} from any host", volumeUuid);
        return null;
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
                                          StoragePool pool, Long sourceHostId, Long destHostId) {
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
            Long actualLockHostId = queryCurrentLockHolder(volumeId, volumeUuid, volumePath, pool, false);

            Long hostToDeactivate = actualLockHostId != null ? actualLockHostId : sourceHostId;

            logger.info("Transferring CLVM lock for volume {}: actual holder={}, provided source={}, destination={}",
                    volumeUuid, actualLockHostId, sourceHostId, destHostId);

            if (hostToDeactivate != null && !hostToDeactivate.equals(destHostId)) {
                HostVO deactivateHost = _hostDao.findById(hostToDeactivate);
                if (deactivateHost != null && deactivateHost.getStatus() == Status.Up) {
                    ClvmLockTransferCommand deactivateCmd = new ClvmLockTransferCommand(
                            ClvmLockTransferCommand.Operation.DEACTIVATE,
                            lvPath,
                            volumeUuid
                    );

                    Answer deactivateAnswer = _agentMgr.send(hostToDeactivate, deactivateCmd);

                    if (deactivateAnswer == null || !deactivateAnswer.getResult()) {
                        logger.warn("Failed to deactivate CLVM volume {} on host {}. Will attempt activation on destination.",
                                volumeUuid, hostToDeactivate);
                    } else {
                        logger.debug("Successfully deactivated volume {} on host {}", volumeUuid, hostToDeactivate);
                    }
                } else {
                    logger.warn("Host {} (current lock holder) is down. Will attempt force claim on destination host {}",
                            hostToDeactivate, destHostId);
                }
            } else if (actualLockHostId == null) {
                logger.debug("Volume {} has no active lock, will directly activate on destination", volumeUuid);
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
                    volumeUuid, actualLockHostId != null ? actualLockHostId : "none", destHostId);

            return true;

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.error("Exception during CLVM lock transfer for volume {}: {}", volumeUuid, e.getMessage(), e);
            return false;
        }
    }
}
