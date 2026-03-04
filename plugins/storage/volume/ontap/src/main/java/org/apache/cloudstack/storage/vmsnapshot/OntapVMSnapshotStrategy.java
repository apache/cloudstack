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
package org.apache.cloudstack.storage.vmsnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.client.SnapshotFeignClient;
import org.apache.cloudstack.storage.feign.model.CliSnapshotRestoreRequest;
import org.apache.cloudstack.storage.feign.model.FlexVolSnapshot;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.FreezeThawVMAnswer;
import com.cloud.agent.api.FreezeThawVMCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;

/**
 * VM Snapshot strategy for NetApp ONTAP managed storage using FlexVolume-level snapshots.
 *
 * <p>This strategy handles VM-level (instance) snapshots for VMs whose volumes
 * reside on ONTAP managed primary storage. Instead of creating per-file clones
 * (the old approach), it takes <b>ONTAP FlexVolume-level snapshots</b> via the
 * ONTAP REST API ({@code POST /api/storage/volumes/{uuid}/snapshots}).</p>
 *
 * <h3>Key Advantage:</h3>
 * <p>When multiple CloudStack disks (ROOT + DATA) reside on the same ONTAP
 * FlexVolume, a single FlexVolume snapshot atomically captures all of them.
 * This is both faster and more storage-efficient than per-file clones.</p>
 *
 * <h3>Flow:</h3>
 * <ol>
 *   <li>Group all VM volumes by their parent FlexVolume UUID</li>
 *   <li>Freeze the VM via QEMU guest agent ({@code fsfreeze}) — if quiesce requested</li>
 *   <li>For each unique FlexVolume, create one ONTAP snapshot</li>
 *   <li>Thaw the VM</li>
 *   <li>Record FlexVolume → snapshot UUID mappings in {@code vm_snapshot_details}</li>
 * </ol>
 *
 * <h3>Metadata in vm_snapshot_details:</h3>
 * <p>Each FlexVolume snapshot is stored as a detail row with:
 * <ul>
 *   <li>name = {@value Constants#ONTAP_FLEXVOL_SNAPSHOT}</li>
 *   <li>value = {@code "<flexVolUuid>::<snapshotUuid>::<snapshotName>::<volumePath>::<poolId>::<protocol>"}</li>
 * </ul>
 * One row is persisted per CloudStack volume (not per FlexVolume) so that the
 * revert operation can restore individual files/LUNs using the ONTAP Snapshot
 * File Restore API ({@code POST /api/storage/volumes/{vol}/snapshots/{snap}/files/{path}/restore}).</p>
 *
 * <h3>Strategy Selection:</h3>
 * <p>Returns {@code StrategyPriority.HIGHEST} when:</p>
 * <ul>
 *   <li>Hypervisor is KVM</li>
 *   <li>Snapshot type is Disk-only (no memory)</li>
 *   <li>All VM volumes are on ONTAP managed primary storage</li>
 * </ul>
 */
public class OntapVMSnapshotStrategy extends StorageVMSnapshotStrategy {

    private static final Logger logger = LogManager.getLogger(OntapVMSnapshotStrategy.class);

    /** Separator used in the vm_snapshot_details value to delimit FlexVol UUID, snapshot UUID, snapshot name, and pool ID. */
    static final String DETAIL_SEPARATOR = "::";

    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;

    @Inject
    private VolumeDetailsDao volumeDetailsDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return super.configure(name, params);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Strategy Selection
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public StrategyPriority canHandle(VMSnapshot vmSnapshot) {
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;

        // For existing (non-Allocated) snapshots, check if we created them
        if (!VMSnapshot.State.Allocated.equals(vmSnapshotVO.getState())) {
            // Check for our FlexVolume snapshot details first
            List<VMSnapshotDetailsVO> flexVolDetails = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), Constants.ONTAP_FLEXVOL_SNAPSHOT);
            if (CollectionUtils.isNotEmpty(flexVolDetails)) {
                // Verify the volumes are still on ONTAP storage
                if (allVolumesOnOntapManagedStorage(vmSnapshot.getVmId())) {
                    return StrategyPriority.HIGHEST;
                }
                return StrategyPriority.CANT_HANDLE;
            }
            // Also check legacy STORAGE_SNAPSHOT details for backward compatibility
            List<VMSnapshotDetailsVO> legacyDetails = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), STORAGE_SNAPSHOT);
            if (CollectionUtils.isNotEmpty(legacyDetails) && allVolumesOnOntapManagedStorage(vmSnapshot.getVmId())) {
                return StrategyPriority.HIGHEST;
            }
            return StrategyPriority.CANT_HANDLE;
        }

        // For new snapshots, check if Disk-only and all volumes on ONTAP
        if (vmSnapshotVO.getType() != VMSnapshot.Type.Disk) {
            // Memory snapshots are not supported by ONTAP strategy - return CANT_HANDLE
            // so other strategies can be tried or proper error handling can occur
            logger.debug("canHandle: ONTAP VM snapshot strategy cannot handle memory snapshots for VM [{}]", vmSnapshot.getVmId());
            return StrategyPriority.CANT_HANDLE;
        }

        if (allVolumesOnOntapManagedStorage(vmSnapshot.getVmId())) {
            return StrategyPriority.HIGHEST;
        }

        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public StrategyPriority canHandle(Long vmId, Long rootPoolId, boolean snapshotMemory) {
        if (snapshotMemory) {
            logger.debug("canHandle: ONTAP VM snapshot strategy cannot handle memory snapshots for VM [{}]", vmId);
            return StrategyPriority.CANT_HANDLE;
        }

        if (allVolumesOnOntapManagedStorage(vmId)) {
            return StrategyPriority.HIGHEST;
        }

        return StrategyPriority.CANT_HANDLE;
    }

    /**
     * Checks whether all volumes of a VM reside on ONTAP managed primary storage.
     */
    boolean allVolumesOnOntapManagedStorage(long vmId) {
        UserVm userVm = userVmDao.findById(vmId);
        if (userVm == null) {
            logger.debug("allVolumesOnOntapManagedStorage: VM with id [{}] not found", vmId);
            return false;
        }

        if (!Hypervisor.HypervisorType.KVM.equals(userVm.getHypervisorType())) {
            logger.debug("allVolumesOnOntapManagedStorage: ONTAP VM snapshot strategy only supports KVM hypervisor, VM [{}] uses [{}]",
                    vmId, userVm.getHypervisorType());
            return false;
        }

        // ONTAP VM snapshots work for both Running and Stopped VMs.
        // Running VMs may be frozen/thawed (if quiesce is requested).
        // Stopped VMs don't need freeze/thaw - just take the FlexVol snapshot directly.
        VirtualMachine.State vmState = userVm.getState();
        if (!VirtualMachine.State.Running.equals(vmState) && !VirtualMachine.State.Stopped.equals(vmState)) {
            logger.info("allVolumesOnOntapManagedStorage: ONTAP VM snapshot strategy requires VM to be Running or Stopped, VM [{}] is in state [{}], returning false",
                    vmId, vmState);
            return false;
        }

        List<VolumeVO> volumes = volumeDao.findByInstance(vmId);
        if (volumes == null || volumes.isEmpty()) {
            logger.debug("allVolumesOnOntapManagedStorage: No volumes found for VM [{}]", vmId);
            return false;
        }

        for (VolumeVO volume : volumes) {
            if (volume.getPoolId() == null) {
                return false;
            }
            StoragePoolVO pool = storagePool.findById(volume.getPoolId());
            if (pool == null) {
                return false;
            }
            if (!pool.isManaged()) {
                logger.debug("allVolumesOnOntapManagedStorage: Volume [{}] is on non-managed storage pool [{}], not ONTAP",
                        volume.getId(), pool.getName());
                return false;
            }
            if (!Constants.ONTAP_PLUGIN_NAME.equals(pool.getStorageProviderName())) {
                logger.debug("allVolumesOnOntapManagedStorage: Volume [{}] is on managed pool [{}] with provider [{}], not ONTAP",
                        volume.getId(), pool.getName(), pool.getStorageProviderName());
                return false;
            }
        }

        logger.debug("allVolumesOnOntapManagedStorage: All volumes of VM [{}] are on ONTAP managed storage, this strategy can handle", vmId);
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Take VM Snapshot (FlexVolume-level)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Takes a VM-level snapshot by freezing the VM, creating ONTAP FlexVolume-level
     * snapshots (one per unique FlexVolume), and then thawing the VM.
     *
     * <p>Volumes are grouped by their parent FlexVolume UUID (from storage pool details).
     * For each unique FlexVolume, exactly one ONTAP snapshot is created via
     * {@code POST /api/storage/volumes/{uuid}/snapshots}. This means if a VM has
     * ROOT and DATA disks on the same FlexVolume, only one snapshot is created.</p>
     */
    @Override
    public VMSnapshot takeVMSnapshot(VMSnapshot vmSnapshot) {
        Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshot.getVmId());
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;

        FreezeThawVMAnswer freezeAnswer = null;
        FreezeThawVMCommand thawCmd = null;
        FreezeThawVMAnswer thawAnswer = null;
        long startFreeze = 0;

        // Track which FlexVolume snapshots were created (for rollback)
        List<FlexVolSnapshotDetail> createdSnapshots = new ArrayList<>();

        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.CreateRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());

            long prev_chain_size = 0;
            long virtual_size = 0;

            // Build snapshot parent chain
            VMSnapshotTO current = null;
            VMSnapshotVO currentSnapshot = vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
            if (currentSnapshot != null) {
                current = vmSnapshotHelper.getSnapshotWithParents(currentSnapshot);
            }

            // Respect the user's quiesce option from the VM snapshot request
            boolean quiescevm = true; // default to true for safety
            VMSnapshotOptions options = vmSnapshotVO.getOptions();
            if (options != null) {
                quiescevm = options.needQuiesceVM();
            }

            // Check if VM is actually running - freeze/thaw only makes sense for running VMs
            boolean vmIsRunning = VirtualMachine.State.Running.equals(userVm.getState());
            boolean shouldFreezeThaw = quiescevm && vmIsRunning;

            if (!vmIsRunning) {
                logger.info("takeVMSnapshot: VM [{}] is in state [{}] (not Running). Skipping freeze/thaw - " +
                        "FlexVolume snapshot will be taken directly.", userVm.getInstanceName(), userVm.getState());
            } else if (quiescevm) {
                logger.info("takeVMSnapshot: Quiesce option is enabled for ONTAP VM Snapshot of VM [{}]. " +
                        "VM file systems will be frozen/thawed for application-consistent snapshots.", userVm.getInstanceName());
            } else {
                logger.info("takeVMSnapshot: Quiesce option is disabled for ONTAP VM Snapshot of VM [{}]. " +
                        "Snapshots will be crash-consistent only.", userVm.getInstanceName());
            }

            VMSnapshotTO target = new VMSnapshotTO(vmSnapshot.getId(), vmSnapshot.getName(),
                    vmSnapshot.getType(), null, vmSnapshot.getDescription(), false, current, quiescevm);

            if (current == null) {
                vmSnapshotVO.setParent(null);
            } else {
                vmSnapshotVO.setParent(current.getId());
            }

            CreateVMSnapshotCommand ccmd = new CreateVMSnapshotCommand(
                    userVm.getInstanceName(), userVm.getUuid(), target, volumeTOs, guestOS.getDisplayName());

            logger.info("takeVMSnapshot: Creating ONTAP FlexVolume VM Snapshot for VM [{}] with quiesce={}", userVm.getInstanceName(), quiescevm);

            // Prepare volume info list and calculate sizes
            for (VolumeObjectTO volumeObjectTO : volumeTOs) {
                virtual_size += volumeObjectTO.getSize();
                VolumeVO volumeVO = volumeDao.findById(volumeObjectTO.getId());
                prev_chain_size += volumeVO.getVmSnapshotChainSize() == null ? 0 : volumeVO.getVmSnapshotChainSize();
            }

            // ── Group volumes by FlexVolume UUID ──
            Map<String, FlexVolGroupInfo> flexVolGroups = groupVolumesByFlexVol(volumeTOs);

            logger.info("takeVMSnapshot: VM [{}] has {} volumes across {} unique FlexVolume(s)",
                    userVm.getInstanceName(), volumeTOs.size(), flexVolGroups.size());

            // ── Step 1: Freeze the VM (only if quiescing is requested AND VM is running) ──
            if (shouldFreezeThaw) {
                FreezeThawVMCommand freezeCommand = new FreezeThawVMCommand(userVm.getInstanceName());
                freezeCommand.setOption(FreezeThawVMCommand.FREEZE);
                freezeAnswer = (FreezeThawVMAnswer) agentMgr.send(hostId, freezeCommand);
                startFreeze = System.nanoTime();

                thawCmd = new FreezeThawVMCommand(userVm.getInstanceName());
                thawCmd.setOption(FreezeThawVMCommand.THAW);

                if (freezeAnswer == null || !freezeAnswer.getResult()) {
                    String detail = (freezeAnswer != null) ? freezeAnswer.getDetails() : "no response from agent";
                    throw new CloudRuntimeException("Could not freeze VM [" + userVm.getInstanceName() +
                            "] for ONTAP snapshot. Ensure qemu-guest-agent is installed and running. Details: " + detail);
                }

                logger.info("takeVMSnapshot: VM [{}] frozen successfully via QEMU guest agent", userVm.getInstanceName());
            } else {
                logger.info("takeVMSnapshot: Skipping VM freeze for VM [{}] (quiesce={}, vmIsRunning={})",
                        userVm.getInstanceName(), quiescevm, vmIsRunning);
            }

            // ── Step 2: Create FlexVolume-level snapshots ──
            try {
                String snapshotNameBase = buildSnapshotName(vmSnapshot);

                for (Map.Entry<String, FlexVolGroupInfo> entry : flexVolGroups.entrySet()) {
                    String flexVolUuid = entry.getKey();
                    FlexVolGroupInfo groupInfo = entry.getValue();
                    long startSnapshot = System.nanoTime();

                    // Build storage strategy from pool details to get the feign client
                    StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(groupInfo.poolDetails);
                    SnapshotFeignClient snapshotClient = storageStrategy.getSnapshotFeignClient();
                    String authHeader = storageStrategy.getAuthHeader();

                    // Use the same snapshot name for all FlexVolumes in this VM snapshot
                    // (each FlexVolume gets its own independent snapshot with this name)
                    FlexVolSnapshot snapshotRequest = new FlexVolSnapshot(snapshotNameBase,
                            "CloudStack VM snapshot " + vmSnapshot.getName() + " for VM " + userVm.getInstanceName());

                    logger.info("takeVMSnapshot: Creating ONTAP FlexVolume snapshot [{}] on FlexVol UUID [{}] covering {} volume(s)",
                            snapshotNameBase, flexVolUuid, groupInfo.volumeIds.size());

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
                    String snapshotUuid = resolveSnapshotUuid(snapshotClient, authHeader, flexVolUuid, snapshotNameBase);

                    String protocol = groupInfo.poolDetails.get(Constants.PROTOCOL);

                    // Create one detail per CloudStack volume in this FlexVol group (for single-file restore during revert)
                    for (Long volumeId : groupInfo.volumeIds) {
                        String volumePath = resolveVolumePathOnOntap(volumeId, protocol, groupInfo.poolDetails);
                        FlexVolSnapshotDetail detail = new FlexVolSnapshotDetail(
                                flexVolUuid, snapshotUuid, snapshotNameBase, volumePath, groupInfo.poolId, protocol);
                        createdSnapshots.add(detail);
                    }

                    logger.info("takeVMSnapshot: ONTAP FlexVolume snapshot [{}] (uuid={}) on FlexVol [{}] completed in {} ms. Covers volumes: {}",
                            snapshotNameBase, snapshotUuid, flexVolUuid,
                            TimeUnit.MILLISECONDS.convert(System.nanoTime() - startSnapshot, TimeUnit.NANOSECONDS),
                            groupInfo.volumeIds);
                }
            } finally {
                // ── Step 3: Thaw the VM (only if it was frozen, always even on error) ──
                if (quiescevm && freezeAnswer != null && freezeAnswer.getResult()) {
                    try {
                        thawAnswer = (FreezeThawVMAnswer) agentMgr.send(hostId, thawCmd);
                        if (thawAnswer != null && thawAnswer.getResult()) {
                            logger.info("takeVMSnapshot: VM [{}] thawed successfully. Total freeze duration: {} ms",
                                    userVm.getInstanceName(),
                                    TimeUnit.MILLISECONDS.convert(System.nanoTime() - startFreeze, TimeUnit.NANOSECONDS));
                        } else {
                            logger.warn("takeVMSnapshot: Failed to thaw VM [{}]: {}", userVm.getInstanceName(),
                                    (thawAnswer != null) ? thawAnswer.getDetails() : "no response");
                        }
                    } catch (Exception thawEx) {
                        logger.error("takeVMSnapshot: Exception while thawing VM [{}]: {}", userVm.getInstanceName(), thawEx.getMessage(), thawEx);
                    }
                }
            }

            // ── Step 4: Persist FlexVolume snapshot details (one row per CloudStack volume) ──
            for (FlexVolSnapshotDetail detail : createdSnapshots) {
                vmSnapshotDetailsDao.persist(new VMSnapshotDetailsVO(
                        vmSnapshot.getId(), Constants.ONTAP_FLEXVOL_SNAPSHOT, detail.toString(), true));
            }

            // ── Step 5: Finalize via parent processAnswer ──
            CreateVMSnapshotAnswer answer = new CreateVMSnapshotAnswer(ccmd, true, "");
            answer.setVolumeTOs(volumeTOs);

            processAnswer(vmSnapshotVO, userVm, answer, null);
            logger.info("takeVMSnapshot: ONTAP FlexVolume VM Snapshot [{}] created successfully for VM [{}] ({} FlexVol snapshot(s))",
                    vmSnapshot.getName(), userVm.getInstanceName(), createdSnapshots.size());

            long new_chain_size = 0;
            for (VolumeObjectTO volumeTo : answer.getVolumeTOs()) {
                publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_CREATE, vmSnapshot, userVm, volumeTo);
                new_chain_size += volumeTo.getSize();
            }
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_ON_PRIMARY, vmSnapshot, userVm,
                    new_chain_size - prev_chain_size, virtual_size);

            result = true;
            return vmSnapshot;

        } catch (OperationTimedoutException e) {
            logger.error("takeVMSnapshot: ONTAP VM Snapshot [{}] timed out: {}", vmSnapshot.getName(), e.getMessage());
            throw new CloudRuntimeException("Creating Instance Snapshot: " + vmSnapshot.getName() + " timed out: " + e.getMessage());
        } catch (AgentUnavailableException e) {
            logger.error("takeVMSnapshot: ONTAP VM Snapshot [{}] failed, agent unavailable: {}", vmSnapshot.getName(), e.getMessage());
            throw new CloudRuntimeException("Creating Instance Snapshot: " + vmSnapshot.getName() + " failed: " + e.getMessage());
        } catch (CloudRuntimeException e) {
            throw e;
        } finally {
            if (!result) {
                // Rollback all FlexVolume snapshots created so far (deduplicate by FlexVol+Snapshot)
                Map<String, Boolean> rolledBack = new HashMap<>();
                for (FlexVolSnapshotDetail detail : createdSnapshots) {
                    String dedupeKey = detail.flexVolUuid + "::" + detail.snapshotUuid;
                    if (!rolledBack.containsKey(dedupeKey)) {
                        try {
                            rollbackFlexVolSnapshot(detail);
                            rolledBack.put(dedupeKey, Boolean.TRUE);
                        } catch (Exception rollbackEx) {
                            logger.error("takeVMSnapshot: Failed to rollback FlexVol snapshot [{}] on FlexVol [{}]: {}",
                                    detail.snapshotUuid, detail.flexVolUuid, rollbackEx.getMessage());
                        }
                    }
                }

                // Ensure VM is thawed if we haven't done so
                if (thawAnswer == null && freezeAnswer != null && freezeAnswer.getResult()) {
                    try {
                        logger.info("takeVMSnapshot: Thawing VM [{}] during error cleanup", userVm.getInstanceName());
                        thawAnswer = (FreezeThawVMAnswer) agentMgr.send(hostId, thawCmd);
                    } catch (Exception ex) {
                        logger.error("takeVMSnapshot: Could not thaw VM during cleanup: {}", ex.getMessage());
                    }
                }

                // Clean up VM snapshot details and transition state
                try {
                    List<VMSnapshotDetailsVO> vmSnapshotDetails = vmSnapshotDetailsDao.listDetails(vmSnapshot.getId());
                    for (VMSnapshotDetailsVO detail : vmSnapshotDetails) {
                        if (Constants.ONTAP_FLEXVOL_SNAPSHOT.equals(detail.getName())) {
                            vmSnapshotDetailsDao.remove(detail.getId());
                        }
                    }
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    logger.error("takeVMSnapshot: Cannot set VM Snapshot state to OperationFailed: {}", e1.getMessage());
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Delete VM Snapshot
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean deleteVMSnapshot(VMSnapshot vmSnapshot) {
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());

        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.ExpungeRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        try {
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
            String vmInstanceName = userVm.getInstanceName();
            VMSnapshotTO parent = vmSnapshotHelper.getSnapshotWithParents(vmSnapshotVO).getParent();

            VMSnapshotTO vmSnapshotTO = new VMSnapshotTO(vmSnapshotVO.getId(), vmSnapshotVO.getName(), vmSnapshotVO.getType(),
                    vmSnapshotVO.getCreated().getTime(), vmSnapshotVO.getDescription(), vmSnapshotVO.getCurrent(), parent, true);
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
            DeleteVMSnapshotCommand deleteSnapshotCommand = new DeleteVMSnapshotCommand(vmInstanceName, vmSnapshotTO,
                    volumeTOs, guestOS.getDisplayName());

            // Check for FlexVolume snapshots (new approach)
            List<VMSnapshotDetailsVO> flexVolDetails = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), Constants.ONTAP_FLEXVOL_SNAPSHOT);
            if (CollectionUtils.isNotEmpty(flexVolDetails)) {
                deleteFlexVolSnapshots(flexVolDetails);
            }

            // Also handle legacy STORAGE_SNAPSHOT details (backward compatibility)
            List<VMSnapshotDetailsVO> legacyDetails = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), STORAGE_SNAPSHOT);
            if (CollectionUtils.isNotEmpty(legacyDetails)) {
                deleteDiskSnapshot(vmSnapshot);
            }

            processAnswer(vmSnapshotVO, userVm, new DeleteVMSnapshotAnswer(deleteSnapshotCommand, volumeTOs), null);
            long full_chain_size = 0;
            for (VolumeObjectTO volumeTo : volumeTOs) {
                publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_DELETE, vmSnapshot, userVm, volumeTo);
                full_chain_size += volumeTo.getSize();
            }
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY, vmSnapshot, userVm, full_chain_size, 0L);
            return true;
        } catch (CloudRuntimeException err) {
            String errMsg = String.format("Delete of ONTAP VM Snapshot [%s] of VM [%s] failed: %s",
                    vmSnapshot.getName(), userVm.getInstanceName(), err.getMessage());
            logger.error(errMsg, err);
            throw new CloudRuntimeException(errMsg, err);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Revert VM Snapshot
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean revertVMSnapshot(VMSnapshot vmSnapshot) {
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());

        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.RevertRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
            String vmInstanceName = userVm.getInstanceName();
            VMSnapshotTO parent = vmSnapshotHelper.getSnapshotWithParents(vmSnapshotVO).getParent();

            VMSnapshotTO vmSnapshotTO = new VMSnapshotTO(vmSnapshotVO.getId(), vmSnapshotVO.getName(), vmSnapshotVO.getType(),
                    vmSnapshotVO.getCreated().getTime(), vmSnapshotVO.getDescription(), vmSnapshotVO.getCurrent(), parent, true);
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
            RevertToVMSnapshotCommand revertToSnapshotCommand = new RevertToVMSnapshotCommand(vmInstanceName,
                    userVm.getUuid(), vmSnapshotTO, volumeTOs, guestOS.getDisplayName());

            // Check for FlexVolume snapshots (new approach)
            List<VMSnapshotDetailsVO> flexVolDetails = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), Constants.ONTAP_FLEXVOL_SNAPSHOT);
            if (CollectionUtils.isNotEmpty(flexVolDetails)) {
                revertFlexVolSnapshots(flexVolDetails);
            }

            // Also handle legacy STORAGE_SNAPSHOT details (backward compatibility)
            List<VMSnapshotDetailsVO> legacyDetails = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), STORAGE_SNAPSHOT);
            if (CollectionUtils.isNotEmpty(legacyDetails)) {
                revertDiskSnapshot(vmSnapshot);
            }

            RevertToVMSnapshotAnswer answer = new RevertToVMSnapshotAnswer(revertToSnapshotCommand, true, "");
            answer.setVolumeTOs(volumeTOs);
            processAnswer(vmSnapshotVO, userVm, answer, null);
            result = true;
        } catch (CloudRuntimeException e) {
            logger.error("revertVMSnapshot: Revert ONTAP VM Snapshot [{}] failed: {}", vmSnapshot.getName(), e.getMessage(), e);
            throw new CloudRuntimeException("Revert ONTAP VM Snapshot ["+ vmSnapshot.getName() +"] failed.");
        } finally {
            if (!result) {
                try {
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    logger.error("Cannot set Instance Snapshot state due to: " + e1.getMessage());
                }
            }
        }
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // FlexVolume Snapshot Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Groups volumes by their parent FlexVolume UUID using storage pool details.
     *
     * @param volumeTOs list of volume transfer objects
     * @return map of FlexVolume UUID → group info (pool details, pool ID, volume IDs)
     */
    Map<String, FlexVolGroupInfo> groupVolumesByFlexVol(List<VolumeObjectTO> volumeTOs) {
        Map<String, FlexVolGroupInfo> groups = new HashMap<>();

        for (VolumeObjectTO volumeTO : volumeTOs) {
            VolumeVO volumeVO = volumeDao.findById(volumeTO.getId());
            if (volumeVO == null || volumeVO.getPoolId() == null) {
                throw new CloudRuntimeException("Volume [" + volumeTO.getId() + "] not found or has no pool assigned");
            }

            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(volumeVO.getPoolId());
            String flexVolUuid = poolDetails.get(Constants.VOLUME_UUID);
            if (flexVolUuid == null || flexVolUuid.isEmpty()) {
                throw new CloudRuntimeException("FlexVolume UUID not found in pool details for pool [" + volumeVO.getPoolId() + "]");
            }

            FlexVolGroupInfo group = groups.get(flexVolUuid);
            if (group == null) {
                group = new FlexVolGroupInfo(poolDetails, volumeVO.getPoolId());
                groups.put(flexVolUuid, group);
            }
            group.volumeIds.add(volumeVO.getId());
        }

        return groups;
    }

    /**
     * Builds a deterministic, ONTAP-safe snapshot name for a VM snapshot.
     * Format: {@code vmsnap_<vmSnapshotId>_<timestamp>}
     */
    String buildSnapshotName(VMSnapshot vmSnapshot) {
        String name = "vmsnap_" + vmSnapshot.getId() + "_" + System.currentTimeMillis();
        // ONTAP snapshot names: max 256 chars, must start with letter, only alphanumeric and underscores
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
            throw new CloudRuntimeException("Could not find FlexVolume snapshot [" + snapshotName +
                    "] on FlexVol [" + flexVolUuid + "] after creation");
        }
        return response.getRecords().get(0).getUuid();
    }

    /**
     * Resolves the ONTAP-side path of a CloudStack volume within its FlexVolume.
     *
     * <ul>
     *   <li>For NFS volumes the path is the filename (e.g. {@code uuid.qcow2})
     *       retrieved via {@link VolumeVO#getPath()}.</li>
     *   <li>For iSCSI volumes the path is the LUN name within the FlexVolume
     *       (e.g. {@code /vol/vol1/lun_name}) stored in volume_details.</li>
     * </ul>
     *
     * @param volumeId   the CloudStack volume ID
     * @param protocol   the storage protocol (e.g. "NFS3", "ISCSI")
     * @param poolDetails storage pool detail map (used for fall-back lookups)
     * @return the volume path relative to the FlexVolume root
     */
    String resolveVolumePathOnOntap(Long volumeId, String protocol, Map<String, String> poolDetails) {
        if (ProtocolType.ISCSI.name().equalsIgnoreCase(protocol)) {
            // iSCSI – the LUN's ONTAP name is stored as a volume detail
            VolumeDetailVO lunDetail = volumeDetailsDao.findDetail(volumeId, Constants.LUN_DOT_NAME);
            if (lunDetail == null || lunDetail.getValue() == null || lunDetail.getValue().isEmpty()) {
                throw new CloudRuntimeException(
                        "LUN name (volume detail '" + Constants.LUN_DOT_NAME + "') not found for iSCSI volume [" + volumeId + "]");
            }
            return lunDetail.getValue();
        } else {
            // NFS – volumeVO.getPath() holds the file path (e.g. "uuid.qcow2")
            VolumeVO vol = volumeDao.findById(volumeId);
            if (vol == null || vol.getPath() == null || vol.getPath().isEmpty()) {
                throw new CloudRuntimeException("Volume path not found for NFS volume [" + volumeId + "]");
            }
            return vol.getPath();
        }
    }

    /**
     * Rolls back (deletes) a FlexVolume snapshot that was created during a failed takeVMSnapshot.
     */
    void rollbackFlexVolSnapshot(FlexVolSnapshotDetail detail) {
        try {
            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(detail.poolId);
            StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);
            SnapshotFeignClient client = storageStrategy.getSnapshotFeignClient();
            String authHeader = storageStrategy.getAuthHeader();

            logger.info("rollbackFlexVolSnapshot: Rolling back FlexVol snapshot [{}] (uuid={}) on FlexVol [{}]",
                    detail.snapshotName, detail.snapshotUuid, detail.flexVolUuid);

            JobResponse jobResponse = client.deleteSnapshot(authHeader, detail.flexVolUuid, detail.snapshotUuid);
            if (jobResponse != null && jobResponse.getJob() != null) {
                storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 10, 2);
            }
        } catch (Exception e) {
            logger.error("rollbackFlexVolSnapshot: Rollback of FlexVol snapshot failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Deletes all FlexVolume snapshots associated with a VM snapshot.
     *
     * <p>Since there is one detail row per CloudStack volume, multiple rows may reference
     * the same FlexVol + snapshot combination. This method deduplicates to delete each
     * underlying ONTAP snapshot only once.</p>
     */
    void deleteFlexVolSnapshots(List<VMSnapshotDetailsVO> flexVolDetails) {
        // Track which FlexVol+Snapshot pairs have already been deleted
        Map<String, Boolean> deletedSnapshots = new HashMap<>();

        for (VMSnapshotDetailsVO detailVO : flexVolDetails) {
            FlexVolSnapshotDetail detail = FlexVolSnapshotDetail.parse(detailVO.getValue());
            String dedupeKey = detail.flexVolUuid + "::" + detail.snapshotUuid;

            // Only delete the ONTAP snapshot once per FlexVol+Snapshot pair
            if (!deletedSnapshots.containsKey(dedupeKey)) {
                Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(detail.poolId);
                StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);
                SnapshotFeignClient client = storageStrategy.getSnapshotFeignClient();
                String authHeader = storageStrategy.getAuthHeader();

                logger.info("deleteFlexVolSnapshots: Deleting ONTAP FlexVol snapshot [{}] (uuid={}) on FlexVol [{}]",
                        detail.snapshotName, detail.snapshotUuid, detail.flexVolUuid);

                JobResponse jobResponse = client.deleteSnapshot(authHeader, detail.flexVolUuid, detail.snapshotUuid);
                if (jobResponse != null && jobResponse.getJob() != null) {
                    storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 30, 2);
                }

                deletedSnapshots.put(dedupeKey, Boolean.TRUE);
                logger.info("deleteFlexVolSnapshots: Deleted ONTAP FlexVol snapshot [{}] on FlexVol [{}]", detail.snapshotName, detail.flexVolUuid);
            }

            // Always remove the DB detail row
            vmSnapshotDetailsDao.remove(detailVO.getId());
        }
    }

    /**
     * Reverts all volumes of a VM snapshot using ONTAP CLI-based Snapshot File Restore.
     *
     * <p>Instead of restoring the entire FlexVolume to a snapshot (which would affect
     * other VMs/files on the same FlexVol), this method restores <b>only the individual
     * files or LUNs</b> belonging to this VM using the dedicated ONTAP CLI snapshot file
     * restore API:</p>
     *
     * <p>{@code POST /api/private/cli/volume/snapshot/restore-file}</p>
     *
     * <p>For each persisted detail row (one per CloudStack volume):</p>
     * <ul>
     *   <li><b>NFS</b>: restores {@code <filename>} from the snapshot to the live volume</li>
     *   <li><b>iSCSI</b>: restores {@code <lunPath>} from the snapshot to the live volume</li>
     * </ul>
     */
    void revertFlexVolSnapshots(List<VMSnapshotDetailsVO> flexVolDetails) {
        for (VMSnapshotDetailsVO detailVO : flexVolDetails) {
            FlexVolSnapshotDetail detail = FlexVolSnapshotDetail.parse(detailVO.getValue());

            if (detail.volumePath == null || detail.volumePath.isEmpty()) {
                // Legacy detail row without volumePath – cannot do single-file restore
                logger.warn("revertFlexVolSnapshots: FlexVol snapshot detail for FlexVol [{}] has no volumePath (legacy format). " +
                        "Skipping single-file restore for this entry.", detail.flexVolUuid);
                continue;
            }

            Map<String, String> poolDetails = storagePoolDetailsDao.listDetailsKeyPairs(detail.poolId);
            StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(poolDetails);
            SnapshotFeignClient snapshotClient = storageStrategy.getSnapshotFeignClient();
            String authHeader = storageStrategy.getAuthHeader();

            // Get SVM name and FlexVolume name from pool details
            String svmName = poolDetails.get(Constants.SVM_NAME);
            String flexVolName = poolDetails.get(Constants.VOLUME_NAME);

            if (svmName == null || svmName.isEmpty()) {
                throw new CloudRuntimeException("revertFlexVolSnapshots: SVM name not found in pool details for pool [" + detail.poolId + "]");
            }
            if (flexVolName == null || flexVolName.isEmpty()) {
                throw new CloudRuntimeException("revertFlexVolSnapshots: FlexVolume name not found in pool details for pool [" + detail.poolId + "]");
            }

            // The path must start with "/" for the ONTAP CLI API
            String ontapFilePath = detail.volumePath.startsWith("/") ? detail.volumePath : "/" + detail.volumePath;

            logger.info("revertFlexVolSnapshots: Restoring volume [{}] from FlexVol snapshot [{}] on FlexVol [{}] (protocol={})",
                    ontapFilePath, detail.snapshotName, flexVolName, detail.protocol);

            // Use CLI-based restore API: POST /api/private/cli/volume/snapshot/restore-file
            CliSnapshotRestoreRequest restoreRequest = new CliSnapshotRestoreRequest(
                    svmName, flexVolName, detail.snapshotName, ontapFilePath);

            JobResponse jobResponse = snapshotClient.restoreFileFromSnapshotCli(authHeader, restoreRequest);

            if (jobResponse != null && jobResponse.getJob() != null) {
                Boolean success = storageStrategy.jobPollForSuccess(jobResponse.getJob().getUuid(), 60, 2);
                if (!success) {
                    throw new CloudRuntimeException("Snapshot file restore failed for volume path [" +
                            ontapFilePath + "] from snapshot [" + detail.snapshotName +
                            "] on FlexVol [" + flexVolName + "]");
                }
            }

            logger.info("revertFlexVolSnapshots: Successfully restored volume [{}] from snapshot [{}] on FlexVol [{}]",
                    ontapFilePath, detail.snapshotName, flexVolName);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Inner classes for grouping & detail tracking
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Groups information about volumes that share the same FlexVolume.
     */
    static class FlexVolGroupInfo {
        final Map<String, String> poolDetails;
        final long poolId;
        final List<Long> volumeIds = new ArrayList<>();

        FlexVolGroupInfo(Map<String, String> poolDetails, long poolId) {
            this.poolDetails = poolDetails;
            this.poolId = poolId;
        }
    }

    /**
     * Holds the metadata for a single volume's FlexVolume snapshot entry (used during create and for
     * serialization/deserialization to/from vm_snapshot_details).
     *
     * <p>One row is persisted per CloudStack volume. Multiple volumes may share the same
     * FlexVol snapshot (if they reside on the same FlexVolume).</p>
     *
     * <p>Serialized format: {@code "<flexVolUuid>::<snapshotUuid>::<snapshotName>::<volumePath>::<poolId>::<protocol>"}</p>
     */
    static class FlexVolSnapshotDetail {
        final String flexVolUuid;
        final String snapshotUuid;
        final String snapshotName;
        /** The ONTAP-side path of the file or LUN within the FlexVolume (e.g. "uuid.qcow2" for NFS, "/vol/vol1/lun1" for iSCSI). */
        final String volumePath;
        final long poolId;
        /** Storage protocol: NFS3, ISCSI, etc. */
        final String protocol;

        FlexVolSnapshotDetail(String flexVolUuid, String snapshotUuid, String snapshotName,
                              String volumePath, long poolId, String protocol) {
            this.flexVolUuid = flexVolUuid;
            this.snapshotUuid = snapshotUuid;
            this.snapshotName = snapshotName;
            this.volumePath = volumePath;
            this.poolId = poolId;
            this.protocol = protocol;
        }

        /**
         * Parses a vm_snapshot_details value string back into a FlexVolSnapshotDetail.
         */
        static FlexVolSnapshotDetail parse(String value) {
            String[] parts = value.split(DETAIL_SEPARATOR);
            if (parts.length == 4) {
                // Legacy format without volumePath and protocol: flexVolUuid::snapshotUuid::snapshotName::poolId
                return new FlexVolSnapshotDetail(parts[0], parts[1], parts[2], null, Long.parseLong(parts[3]), null);
            }
            if (parts.length != 6) {
                throw new CloudRuntimeException("Invalid ONTAP FlexVol snapshot detail format: " + value);
            }
            return new FlexVolSnapshotDetail(parts[0], parts[1], parts[2], parts[3], Long.parseLong(parts[4]), parts[5]);
        }

        @Override
        public String toString() {
            return flexVolUuid + DETAIL_SEPARATOR + snapshotUuid + DETAIL_SEPARATOR + snapshotName +
                    DETAIL_SEPARATOR + volumePath + DETAIL_SEPARATOR + poolId + DETAIL_SEPARATOR + protocol;
        }
    }
}
