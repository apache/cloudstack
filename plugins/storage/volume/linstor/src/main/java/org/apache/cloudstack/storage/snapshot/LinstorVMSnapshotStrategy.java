//
//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
//
package org.apache.cloudstack.storage.snapshot;

import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.CreateMultiSnapshotRequest;
import com.linbit.linstor.api.model.Snapshot;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.DefaultVMSnapshotStrategy;
import org.apache.cloudstack.storage.vmsnapshot.VMSnapshotHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class LinstorVMSnapshotStrategy extends DefaultVMSnapshotStrategy {
    private static final Logger log = LogManager.getLogger(LinstorVMSnapshotStrategy.class);

    @Inject
    private VMSnapshotHelper _vmSnapshotHelper;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private VMSnapshotDao vmSnapshotDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;

    private void linstorCreateMultiSnapshot(
        DevelopersApi api, VMSnapshotVO vmSnapshotVO, List<VolumeObjectTO> volumeTOs)
        throws ApiException {
        CreateMultiSnapshotRequest cmsReq = new CreateMultiSnapshotRequest();
        for (VolumeObjectTO vol : volumeTOs) {
            Snapshot snap = new Snapshot();
            snap.setName(vmSnapshotVO.getName());
            snap.setResourceName(LinstorUtil.RSC_PREFIX + vol.getPath());
            log.debug(String.format("Add volume %s;%s to snapshot", vol.getName(), snap.getResourceName()));
            cmsReq.addSnapshotsItem(snap);
        }
        log.debug(String.format("Creating multi snapshot %s", vmSnapshotVO.getName()));
        ApiCallRcList answers = api.createMultiSnapshot(cmsReq);
        log.debug(String.format("Created multi snapshot %s", vmSnapshotVO.getName()));
        if (answers.hasError()) {
            throw new CloudRuntimeException(
                "Error creating vm snapshots: " + LinstorUtil.getBestErrorMessage(answers));
        }
    }

    private VMSnapshotVO findAndSetCurrentSnapshot(long vmId, VMSnapshotVO vmSnapshotVO) {
        VMSnapshotTO current = null;
        VMSnapshotVO currentSnapshot = vmSnapshotDao.findCurrentSnapshotByVmId(vmId);
        if (currentSnapshot != null) {
            current = _vmSnapshotHelper.getSnapshotWithParents(currentSnapshot);
        }

        if (current == null) {
            vmSnapshotVO.setParent(null);
        } else {
            vmSnapshotVO.setParent(current.getId());
        }

        return vmSnapshotVO;
    }

    private long getNewChainSizeAndPublishCreate(VMSnapshot vmSnapshot, List<VolumeObjectTO> volumeTOs, UserVm userVm) {
        long new_chain_size = 0;
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            publishUsageEvents(EventTypes.EVENT_VM_SNAPSHOT_CREATE, vmSnapshot, userVm, volumeObjectTO);
            new_chain_size += volumeObjectTO.getSize();
            log.info("EventTypes.EVENT_VM_SNAPSHOT_CREATE publishUsageEvent" + volumeObjectTO);
        }
        return new_chain_size;
    }

    @Override
    public VMSnapshot takeVMSnapshot(VMSnapshot vmSnapshot) {
        log.info("Take vm snapshot: " + vmSnapshot.getName());
        UserVm userVm = _userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;

        try {
            _vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.CreateRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException("No transition: " + e.getMessage());
        }

        boolean result = false;
        try {
            final List<VolumeObjectTO> volumeTOs = _vmSnapshotHelper.getVolumeTOList(userVm.getId());
            final StoragePoolVO storagePool = _storagePoolDao.findById(volumeTOs.get(0).getPoolId());
            final DevelopersApi api = LinstorUtil.getLinstorAPI(storagePool.getHostAddress());

            long prev_chain_size = 0;
            long virtual_size = 0;
            for (VolumeObjectTO volume : volumeTOs) {
                virtual_size += volume.getSize();
                VolumeVO volumeVO = volumeDao.findById(volume.getId());
                prev_chain_size += volumeVO.getVmSnapshotChainSize() == null ? 0 : volumeVO.getVmSnapshotChainSize();
            }

            findAndSetCurrentSnapshot(userVm.getId(), vmSnapshotVO);

            linstorCreateMultiSnapshot(api, vmSnapshotVO, volumeTOs);

            log.debug(String.format("finalize vm snapshot create for %s", vmSnapshotVO.getName()));
            finalizeCreate(vmSnapshotVO, volumeTOs);

            result = _vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
            long new_chain_size = getNewChainSizeAndPublishCreate(vmSnapshot, volumeTOs, userVm);
            publishUsageEvents(
                EventTypes.EVENT_VM_SNAPSHOT_ON_PRIMARY, vmSnapshot, userVm, new_chain_size - prev_chain_size, virtual_size);
            return vmSnapshot;
        } catch (Exception e) {
            log.debug("Could not create VM snapshot:" + e.getMessage());
            throw new CloudRuntimeException("Could not create VM snapshot:" + e.getMessage());
        } finally {
            if (!result) {
                try {
                    _vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                    log.info(String.format("VMSnapshot.Event.OperationFailed vmSnapshot=%s", vmSnapshot));
                } catch (NoTransitionException nte) {
                    log.error("Cannot set vm state:" + nte.getMessage());
                }
            }
        }
    }

    @Override
    public StrategyPriority canHandle(Long vmId, Long rootPoolId, boolean snapshotMemory) {
        if (snapshotMemory) {
            return StrategyPriority.CANT_HANDLE;
        }
        return allVolumesOnLinstor(vmId);
    }

    @Override
    public StrategyPriority canHandle(VMSnapshot vmSnapshot) {
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        if (vmSnapshotVO.getType() != VMSnapshot.Type.Disk) {
            return StrategyPriority.CANT_HANDLE;
        }
        return allVolumesOnLinstor(vmSnapshot.getVmId());
    }

    private StrategyPriority allVolumesOnLinstor(Long vmId) {
        List<VolumeObjectTO> volumeTOs = _vmSnapshotHelper.getVolumeTOList(vmId);
        if (volumeTOs == null || volumeTOs.isEmpty()) {
            return StrategyPriority.CANT_HANDLE;
        }
        for (VolumeObjectTO volumeTO : volumeTOs) {
            Long poolId = volumeTO.getPoolId();
            StoragePoolVO pool = _storagePoolDao.findById(poolId);
            if (!pool.getStorageProviderName().equals(LinstorUtil.PROVIDER_NAME)) {
                return StrategyPriority.CANT_HANDLE;
            }
        }
        return StrategyPriority.HIGHEST;
    }

    private String linstorDeleteSnapshot(final DevelopersApi api, final String rscName, final String snapshotName) {
        String resultMsg = null;
        try {
            ApiCallRcList answers = api.resourceSnapshotDelete(rscName, snapshotName, Collections.emptyList());
            if (answers.hasError()) {
                resultMsg = LinstorUtil.getBestErrorMessage(answers);
            }
        } catch (ApiException apiEx) {
            log.error("Linstor: ApiEx - " + apiEx.getBestMessage());
            resultMsg = apiEx.getBestMessage();
        }

        return resultMsg;
    }

    @Override
    public boolean deleteVMSnapshot(VMSnapshot vmSnapshot) {
        UserVmVO userVm = _userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        try {
            _vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.ExpungeRequested);
        } catch (NoTransitionException e) {
            log.debug("Failed to change vm snapshot state with event ExpungeRequested");
            throw new CloudRuntimeException(
                    "Failed to change vm snapshot state with event ExpungeRequested: " + e.getMessage());
        }

        List<VolumeObjectTO> volumeTOs = _vmSnapshotHelper.getVolumeTOList(vmSnapshot.getVmId());
        final StoragePoolVO storagePool = _storagePoolDao.findById(volumeTOs.get(0).getPoolId());
        final DevelopersApi api = LinstorUtil.getLinstorAPI(storagePool.getHostAddress());

        final String snapshotName = vmSnapshotVO.getName();
        final List<String> failedToDelete = new ArrayList<>();
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            final String rscName = LinstorUtil.RSC_PREFIX + volumeObjectTO.getPath();
            String err = linstorDeleteSnapshot(api, rscName, snapshotName);

            if (err != null)
            {
                String errMsg = String.format("Unable to delete Linstor resource %s snapshot %s: %s",
                        rscName, snapshotName, err);
                log.error(errMsg);
                failedToDelete.add(errMsg);
            }
            log.info("Linstor: Deleted snapshot " + snapshotName + " for resource " + rscName);
        }

        if (!failedToDelete.isEmpty()) {
            throw new CloudRuntimeException(StringUtils.join(failedToDelete, "\n"));
        }

        finalizeDelete(vmSnapshotVO, volumeTOs);
        vmSnapshotDao.remove(vmSnapshot.getId());

        long full_chain_size = 0;
        for (VolumeObjectTO volumeTo : volumeTOs) {
            publishUsageEvents(EventTypes.EVENT_VM_SNAPSHOT_DELETE, vmSnapshot, userVm, volumeTo);
            full_chain_size += volumeTo.getSize();
        }
        publishUsageEvents(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY, vmSnapshot, userVm, full_chain_size, 0L);
        return true;
    }

    private String linstorRevertSnapshot(final DevelopersApi api, final String rscName, final String snapshotName) {
        String resultMsg = null;
        try {
            ApiCallRcList answers = api.resourceSnapshotRollback(rscName, snapshotName);
            if (answers.hasError()) {
                resultMsg = LinstorUtil.getBestErrorMessage(answers);
            }
        } catch (ApiException apiEx) {
            log.error("Linstor: ApiEx - " + apiEx.getBestMessage());
            resultMsg = apiEx.getBestMessage();
        }

        return resultMsg;
    }

    private boolean revertVMSnapshotOperation(VMSnapshot vmSnapshot, long userVmId) throws NoTransitionException {
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        List<VolumeObjectTO> volumeTOs = _vmSnapshotHelper.getVolumeTOList(userVmId);

        final StoragePoolVO storagePool = _storagePoolDao.findById(volumeTOs.get(0).getPoolId());
        final DevelopersApi api = LinstorUtil.getLinstorAPI(storagePool.getHostAddress());
        final String snapshotName = vmSnapshotVO.getName();

        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            final String rscName = LinstorUtil.RSC_PREFIX + volumeObjectTO.getPath();
            String err = linstorRevertSnapshot(api, rscName, snapshotName);
            if (err != null) {
                throw new CloudRuntimeException(String.format(
                        "Unable to revert Linstor resource %s with snapshot %s: %s", rscName, snapshotName, err));
            }
        }
        finalizeRevert(vmSnapshotVO, volumeTOs);
        return _vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
    }

    @Override
    public boolean revertVMSnapshot(VMSnapshot vmSnapshot) {
        log.debug("Revert vm snapshot: " + vmSnapshot.getName());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        UserVmVO userVm = _userVmDao.findById(vmSnapshot.getVmId());

        if (userVm.getState() == VirtualMachine.State.Running && vmSnapshotVO.getType() == VMSnapshot.Type.Disk) {
            throw new CloudRuntimeException("Virtual machine should be in stopped state for revert operation");
        }

        try {
            _vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.RevertRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
            result = revertVMSnapshotOperation(vmSnapshot, userVm.getId());
        } catch (CloudRuntimeException | NoTransitionException  e) {
            String errMsg = String.format(
                "Error while finalize create vm snapshot [%s] due to %s", vmSnapshot.getName(), e.getMessage());
            log.error(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        } finally {
            if (!result) {
                try {
                    _vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    log.error("Cannot set vm snapshot state due to: " + e1.getMessage());
                }
            }
        }
        return result;
    }

    private void publishUsageEvents(String type, VMSnapshot vmSnapshot, UserVm userVm, VolumeObjectTO volumeTo) {
        VolumeVO volume = volumeDao.findById(volumeTo.getId());
        Long diskOfferingId = volume.getDiskOfferingId();
        Long offeringId = null;
        if (diskOfferingId != null) {
            DiskOfferingVO offering = diskOfferingDao.findById(diskOfferingId);
            if (offering != null && offering.isComputeOnly()) {
                offeringId = offering.getId();
            }
        }
        UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(),
                vmSnapshot.getName(), offeringId, volume.getId(), volumeTo.getSize(), VMSnapshot.class.getName(),
            vmSnapshot.getUuid());
    }

    private void publishUsageEvents(
        String type,
        VMSnapshot vmSnapshot,
        UserVm userVm,
        Long vmSnapSize,
        Long virtualSize) {
        try {
            UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(),
                    vmSnapshot.getName(), 0L, 0L, vmSnapSize, virtualSize, VMSnapshot.class.getName(),
                    vmSnapshot.getUuid());
        } catch (Exception e) {
            log.error("Failed to publish usage event " + type, e);
        }
    }
}
