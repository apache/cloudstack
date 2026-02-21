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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.FreezeThawVMAnswer;
import com.cloud.agent.api.FreezeThawVMCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VolumeVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;

/**
 * VM Snapshot strategy for NetApp ONTAP managed storage.
 *
 * <p>This strategy handles VM-level (instance) snapshots for VMs whose volumes
 * reside on ONTAP managed primary storage using the NFS protocol. It uses the
 * QEMU guest agent to freeze/thaw the VM file systems for consistency, and
 * delegates per-volume snapshot creation to the existing CloudStack snapshot
 * framework which routes to {@code StorageSystemSnapshotStrategy} →
 * {@code OntapPrimaryDatastoreDriver.takeSnapshot()} (ONTAP file clone).</p>
 *
 * <h3>Flow:</h3>
 * <ol>
 *   <li>Freeze the VM via QEMU guest agent ({@code fsfreeze})</li>
 *   <li>For each attached volume, create a storage-level snapshot (ONTAP file clone)</li>
 *   <li>Thaw the VM</li>
 *   <li>Record VM snapshot ↔ volume snapshot mappings in {@code vm_snapshot_details}</li>
 * </ol>
 *
 * <h3>Strategy Selection:</h3>
 * <p>Returns {@code StrategyPriority.HIGHEST} when:</p>
 * <ul>
 *   <li>Hypervisor is KVM</li>
 *   <li>Snapshot type is Disk-only (no memory)</li>
 *   <li>All VM volumes are on ONTAP managed NFS primary storage</li>
 * </ul>
 */
public class OntapVMSnapshotStrategy extends StorageVMSnapshotStrategy {

    private static final Logger logger = LogManager.getLogger(OntapVMSnapshotStrategy.class);

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
            List<VMSnapshotDetailsVO> vmSnapshotDetails = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), STORAGE_SNAPSHOT);
            if (CollectionUtils.isEmpty(vmSnapshotDetails)) {
                return StrategyPriority.CANT_HANDLE;
            }
            // Verify the volumes are still on ONTAP storage
            if (allVolumesOnOntapManagedStorage(vmSnapshot.getVmId())) {
                return StrategyPriority.HIGHEST;
            }
            return StrategyPriority.CANT_HANDLE;
        }

        // For new snapshots, check if Disk-only and all volumes on ONTAP
        if (vmSnapshotVO.getType() != VMSnapshot.Type.Disk) {
            logger.debug("ONTAP VM snapshot strategy cannot handle memory snapshots for VM [{}]", vmSnapshot.getVmId());
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
            logger.debug("ONTAP VM snapshot strategy cannot handle memory snapshots for VM [{}]", vmId);
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
    private boolean allVolumesOnOntapManagedStorage(long vmId) {
        UserVm userVm = userVmDao.findById(vmId);
        if (userVm == null) {
            logger.debug("VM with id [{}] not found", vmId);
            return false;
        }

        if (!Hypervisor.HypervisorType.KVM.equals(userVm.getHypervisorType())) {
            logger.debug("ONTAP VM snapshot strategy only supports KVM hypervisor, VM [{}] uses [{}]",
                    vmId, userVm.getHypervisorType());
            return false;
        }

        if (!VirtualMachine.State.Running.equals(userVm.getState())) {
            logger.debug("ONTAP VM snapshot strategy requires a running VM, VM [{}] is in state [{}]",
                    vmId, userVm.getState());
            return false;
        }

        List<VolumeVO> volumes = volumeDao.findByInstance(vmId);
        if (volumes == null || volumes.isEmpty()) {
            logger.debug("No volumes found for VM [{}]", vmId);
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
                logger.debug("Volume [{}] is on non-managed storage pool [{}], not ONTAP",
                        volume.getId(), pool.getName());
                return false;
            }
            if (!Constants.ONTAP_PLUGIN_NAME.equals(pool.getStorageProviderName())) {
                logger.debug("Volume [{}] is on managed pool [{}] with provider [{}], not ONTAP",
                        volume.getId(), pool.getName(), pool.getStorageProviderName());
                return false;
            }
        }

        logger.debug("All volumes of VM [{}] are on ONTAP managed storage, this strategy can handle", vmId);
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Per-Volume Snapshot (quiesce override)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a per-volume disk snapshot as part of a VM snapshot operation.
     *
     * <p>Overrides the parent to ensure {@code quiescevm} is always {@code false}
     * in the per-volume snapshot payload. ONTAP handles quiescing at the VM level
     * via QEMU guest agent freeze/thaw in {@link #takeVMSnapshot}, so the
     * individual volume snapshot must not request quiescing again. Without this
     * override, {@link org.apache.cloudstack.storage.snapshot.DefaultSnapshotStrategy#takeSnapshot}
     * would reject the request with "can't handle quiescevm equal true for volume snapshot"
     * when the user selects the quiesce option in the UI.</p>
     */
    @Override
    protected SnapshotInfo createDiskSnapshot(VMSnapshot vmSnapshot, List<SnapshotInfo> forRollback, VolumeInfo vol) {
        // Temporarily override the quiesce option to false for the per-volume snapshot
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        VMSnapshotOptions originalOptions = vmSnapshotVO.getOptions();
        try {
            vmSnapshotVO.setOptions(new VMSnapshotOptions(false));
            return super.createDiskSnapshot(vmSnapshot, forRollback, vol);
        } finally {
            vmSnapshotVO.setOptions(originalOptions);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Take VM Snapshot
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Takes a VM-level snapshot by freezing the VM, creating per-volume snapshots
     * on ONTAP storage (file clones), and then thawing the VM.
     *
     * <p>The quiesce option is always {@code true} for ONTAP to ensure filesystem
     * consistency across all volumes. The QEMU guest agent must be installed and
     * running inside the guest VM.</p>
     */
    @Override
    public VMSnapshot takeVMSnapshot(VMSnapshot vmSnapshot) {
        Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshot.getVmId());
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;

        CreateVMSnapshotAnswer answer = null;
        FreezeThawVMAnswer freezeAnswer = null;
        FreezeThawVMCommand thawCmd = null;
        FreezeThawVMAnswer thawAnswer = null;
        List<SnapshotInfo> forRollback = new ArrayList<>();
        long startFreeze = 0;

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

            // For ONTAP managed NFS, always quiesce the VM for filesystem consistency
            boolean quiescevm = true;
            VMSnapshotOptions options = vmSnapshotVO.getOptions();
            if (options != null && !options.needQuiesceVM()) {
                logger.info("Quiesce option was set to false, but overriding to true for ONTAP managed storage " +
                        "to ensure filesystem consistency across all volumes");
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

            logger.info("Creating ONTAP VM Snapshot for VM [{}] with quiesce=true", userVm.getInstanceName());

            // Prepare volume info list
            List<VolumeInfo> volumeInfos = new ArrayList<>();
            for (VolumeObjectTO volumeObjectTO : volumeTOs) {
                volumeInfos.add(volumeDataFactory.getVolume(volumeObjectTO.getId()));
                virtual_size += volumeObjectTO.getSize();
                VolumeVO volumeVO = volumeDao.findById(volumeObjectTO.getId());
                prev_chain_size += volumeVO.getVmSnapshotChainSize() == null ? 0 : volumeVO.getVmSnapshotChainSize();
            }

            // ── Step 1: Freeze the VM ──
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

            logger.info("VM [{}] frozen successfully via QEMU guest agent", userVm.getInstanceName());

            // ── Step 2: Create per-volume snapshots (ONTAP file clones) ──
            try {
                for (VolumeInfo vol : volumeInfos) {
                    long startSnapshot = System.nanoTime();

                    SnapshotInfo snapInfo = createDiskSnapshot(vmSnapshot, forRollback, vol);

                    if (snapInfo == null) {
                        throw new CloudRuntimeException("Could not take ONTAP snapshot for volume id=" + vol.getId());
                    }

                    logger.info("ONTAP snapshot for volume [{}] (id={}) completed in {} ms",
                            vol.getName(), vol.getId(),
                            TimeUnit.MILLISECONDS.convert(System.nanoTime() - startSnapshot, TimeUnit.NANOSECONDS));
                }
            } finally {
                // ── Step 3: Thaw the VM (always, even on error) ──
                try {
                    thawAnswer = (FreezeThawVMAnswer) agentMgr.send(hostId, thawCmd);
                    if (thawAnswer != null && thawAnswer.getResult()) {
                        logger.info("VM [{}] thawed successfully. Total freeze duration: {} ms",
                                userVm.getInstanceName(),
                                TimeUnit.MILLISECONDS.convert(System.nanoTime() - startFreeze, TimeUnit.NANOSECONDS));
                    } else {
                        logger.warn("Failed to thaw VM [{}]: {}", userVm.getInstanceName(),
                                (thawAnswer != null) ? thawAnswer.getDetails() : "no response");
                    }
                } catch (Exception thawEx) {
                    logger.error("Exception while thawing VM [{}]: {}", userVm.getInstanceName(), thawEx.getMessage(), thawEx);
                }
            }

            // ── Step 4: Finalize ──
            answer = new CreateVMSnapshotAnswer(ccmd, true, "");
            answer.setVolumeTOs(volumeTOs);

            processAnswer(vmSnapshotVO, userVm, answer, null);
            logger.info("ONTAP VM Snapshot [{}] created successfully for VM [{}]",
                    vmSnapshot.getName(), userVm.getInstanceName());

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
            logger.error("ONTAP VM Snapshot [{}] timed out: {}", vmSnapshot.getName(), e.getMessage());
            throw new CloudRuntimeException("Creating Instance Snapshot: " + vmSnapshot.getName() + " timed out: " + e.getMessage());
        } catch (AgentUnavailableException e) {
            logger.error("ONTAP VM Snapshot [{}] failed, agent unavailable: {}", vmSnapshot.getName(), e.getMessage());
            throw new CloudRuntimeException("Creating Instance Snapshot: " + vmSnapshot.getName() + " failed: " + e.getMessage());
        } catch (CloudRuntimeException e) {
            throw e;
        } finally {
            if (!result) {
                // Rollback all disk snapshots created so far
                for (SnapshotInfo snapshotInfo : forRollback) {
                    try {
                        rollbackDiskSnapshot(snapshotInfo);
                    } catch (Exception rollbackEx) {
                        logger.error("Failed to rollback snapshot [{}]: {}", snapshotInfo.getId(), rollbackEx.getMessage());
                    }
                }

                // Ensure VM is thawed if we haven't done so
                if (thawAnswer == null && freezeAnswer != null && freezeAnswer.getResult()) {
                    try {
                        logger.info("Thawing VM [{}] during error cleanup", userVm.getInstanceName());
                        thawAnswer = (FreezeThawVMAnswer) agentMgr.send(hostId, thawCmd);
                    } catch (Exception ex) {
                        logger.error("Could not thaw VM during cleanup: {}", ex.getMessage());
                    }
                }

                // Clean up VM snapshot details and transition state
                try {
                    List<VMSnapshotDetailsVO> vmSnapshotDetails = vmSnapshotDetailsDao.listDetails(vmSnapshot.getId());
                    for (VMSnapshotDetailsVO detail : vmSnapshotDetails) {
                        if (STORAGE_SNAPSHOT.equals(detail.getName())) {
                            vmSnapshotDetailsDao.remove(detail.getId());
                        }
                    }
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    logger.error("Cannot set VM Snapshot state to OperationFailed: {}", e1.getMessage());
                }
            }
        }
    }
}
