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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpConnectionDesc;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.DefaultVMSnapshotStrategy;
import org.apache.cloudstack.storage.vmsnapshot.VMSnapshotHelper;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Component
public class StorPoolVMSnapshotStrategy extends DefaultVMSnapshotStrategy {
    private static final Logger log = Logger.getLogger(StorPoolVMSnapshotStrategy.class);

    @Inject
    private VMSnapshotHelper vmSnapshotHelper;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private VMSnapshotDao vmSnapshotDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private PrimaryDataStoreDao storagePool;
    @Inject
    private VMSnapshotDetailsDao vmSnapshotDetailsDao;
    @Inject
    private VolumeDataFactory volFactory;
    @Inject
    private VolumeDetailsDao volumeDetailsDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    private DataStoreManager dataStoreManager;
    int _wait;

    @Override
    public VMSnapshot takeVMSnapshot(VMSnapshot vmSnapshot) {
        log.info("KVMVMSnapshotStrategy take snapshot");
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;

        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.CreateRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException("No transiontion " + e.getMessage());
        }

        boolean result = false;
        try {

            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
            DataStore dataStore = dataStoreManager.getPrimaryDataStore(volumeTOs.get(0).getDataStore().getUuid());
            SpConnectionDesc conn = StorPoolUtil.getSpConnection(dataStore.getUuid(), dataStore.getId(), storagePoolDetailsDao, storagePool);

            long prev_chain_size = 0;
            long virtual_size = 0;
            for (VolumeObjectTO volume : volumeTOs) {
                virtual_size += volume.getSize();
                VolumeVO volumeVO = volumeDao.findById(volume.getId());
                prev_chain_size += volumeVO.getVmSnapshotChainSize() == null ? 0 : volumeVO.getVmSnapshotChainSize();
            }

            VMSnapshotTO current = null;
            VMSnapshotVO currentSnapshot = vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
            if (currentSnapshot != null) {
                current = vmSnapshotHelper.getSnapshotWithParents(currentSnapshot);
            }

            if (current == null) {
                vmSnapshotVO.setParent(null);
            } else {
                vmSnapshotVO.setParent(current.getId());
            }

            SpApiResponse resp = StorPoolUtil.volumesGroupSnapshot(volumeTOs, userVm.getUuid(), vmSnapshotVO.getUuid(), "group", conn);
            JsonObject obj = resp.fullJson.getAsJsonObject();
            JsonArray snapshots = obj.getAsJsonObject("data").getAsJsonArray("snapshots");
            StorPoolUtil.spLog("Volumes=%s attached to virtual machine", volumeTOs.toString());
            for (VolumeObjectTO vol : volumeTOs) {
                for (JsonElement jsonElement : snapshots) {
                    JsonObject snapshotObject = jsonElement.getAsJsonObject();
                    String snapshot = StorPoolUtil
                            .devPath(snapshotObject.getAsJsonPrimitive(StorPoolUtil.GLOBAL_ID).getAsString());
                    if (snapshotObject.getAsJsonPrimitive("volume").getAsString().equals(StorPoolStorageAdaptor.getVolumeNameFromPath(vol.getPath(), true))
                            || snapshotObject.getAsJsonPrimitive("volumeGlobalId").getAsString().equals(StorPoolStorageAdaptor.getVolumeNameFromPath(vol.getPath(), false))) {
                        VMSnapshotDetailsVO vmSnapshotDetailsVO = new VMSnapshotDetailsVO(vmSnapshot.getId(), vol.getUuid(), snapshot, false);
                        vmSnapshotDetailsDao.persist(vmSnapshotDetailsVO);
                        Long poolId = volumeDao.findById(vol.getId()).getPoolId();
                        if (poolId != null) {
                            VMSnapshotDetailsVO vmSnapshotDetailStoragePoolId = new VMSnapshotDetailsVO(
                                    vmSnapshot.getId(), StorPoolUtil.SP_STORAGE_POOL_ID, String.valueOf(poolId), false);
                            vmSnapshotDetailsDao.persist(vmSnapshotDetailStoragePoolId);
                        }
                        StorPoolUtil.spLog("Snapshot=%s of volume=%s for a group snapshot=%s.", snapshot, vol.getUuid(), vmSnapshot.getUuid());
                    }
                }
            }

            if (resp.getError() == null) {
                StorPoolUtil.spLog("StorpoolVMSnapshotStrategy.takeSnapshot answer=%s", resp.getError());
                finalizeCreate(vmSnapshotVO, volumeTOs);
                result = vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
                long new_chain_size = 0;
                for (VolumeObjectTO volumeObjectTO : volumeTOs) {
                    publishUsageEvents(EventTypes.EVENT_VM_SNAPSHOT_CREATE, vmSnapshot, userVm, volumeObjectTO);
                    new_chain_size += volumeObjectTO.getSize();
                    log.info("EventTypes.EVENT_VM_SNAPSHOT_CREATE publishUsageEvent" + volumeObjectTO);
                }
                publishUsageEvents(EventTypes.EVENT_VM_SNAPSHOT_ON_PRIMARY, vmSnapshot, userVm, new_chain_size - prev_chain_size, virtual_size);
            } else {
                throw new CloudRuntimeException("Could not create vm snapshot");
            }
            return vmSnapshot;
        } catch (Exception e) {
            log.debug("Could not create VM snapshot:" + e.getMessage());
            throw new CloudRuntimeException("Could not create VM snapshot:" + e.getMessage());
        } finally {
            if (!result) {
                try {
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                    log.info(String.format("VMSnapshot.Event.OperationFailed vmSnapshot=%s", vmSnapshot));
                } catch (NoTransitionException nte) {
                    log.error("Cannot set vm state:" + nte.getMessage());
                }
            }
        }
    }

    @Override
    public StrategyPriority canHandle(VMSnapshot vmSnapshot) {
        return areAllVolumesOnStorPool(vmSnapshot.getVmId());
    }

    public StrategyPriority canHandle(Long vmId, Long rootPoolId, boolean snapshotMemory) {
        if (snapshotMemory) {
            return StrategyPriority.CANT_HANDLE;
        }
        return areAllVolumesOnStorPool(vmId);
    }

    private StrategyPriority areAllVolumesOnStorPool(Long vmId) {
        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vmId);
        if (volumeTOs == null || volumeTOs.isEmpty()) {
            return StrategyPriority.CANT_HANDLE;
        }
        for (VolumeObjectTO volumeTO : volumeTOs) {
            Long poolId = volumeTO.getPoolId();
            StoragePoolVO pool = storagePool.findById(poolId);
            if (!pool.getStorageProviderName().equals(StorPoolUtil.SP_PROVIDER_NAME)) {
                return StrategyPriority.CANT_HANDLE;
            }
        }
        return StrategyPriority.HIGHEST;
    }

    @Override
    public boolean deleteVMSnapshot(VMSnapshot vmSnapshot) {
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.ExpungeRequested);
        } catch (NoTransitionException e) {
            log.debug("Failed to change vm snapshot state with event ExpungeRequested");
            throw new CloudRuntimeException(
                    "Failed to change vm snapshot state with event ExpungeRequested: " + e.getMessage());
        }

        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vmSnapshot.getVmId());
        DataStore dataStore = dataStoreManager.getPrimaryDataStore(volumeTOs.get(0).getDataStore().getUuid());
        SpConnectionDesc conn = null;
        try {
            conn = StorPoolUtil.getSpConnection(dataStore.getUuid(), dataStore.getId(), storagePoolDetailsDao, storagePool);
        } catch (CloudRuntimeException e) {
            throw e;
        }

        SpApiResponse resp = null;
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            String err = null;
            VMSnapshotDetailsVO snapshotDetailsVO = vmSnapshotDetailsDao.findDetail(vmSnapshot.getId(), volumeObjectTO.getUuid());
            String snapshotName = StorPoolStorageAdaptor.getVolumeNameFromPath(snapshotDetailsVO.getValue(), true);
            if (snapshotName == null) {
                err = String.format("Could not find StorPool's snapshot vm snapshot uuid=%s and volume uui=%s",
                        vmSnapshot.getUuid(), volumeObjectTO.getUuid());
                log.error("Could not delete snapshot for vm:" + err);
            }
            StorPoolUtil.spLog("StorpoolVMSnapshotStrategy.deleteVMSnapshot snapshotName=%s", snapshotName);
            resp = StorPoolUtil.snapshotDelete(snapshotName, conn);
            if (resp.getError() != null) {
                err = String.format("Could not delete storpool vm error=%s", resp.getError());
                log.error("Could not delete snapshot for vm:" + err);
            } else {
                // do we need to clean database?
                if (snapshotDetailsVO != null) {
                    vmSnapshotDetailsDao.remove(snapshotDetailsVO.getId());
                }
            }
            if (err != null) {
                StorPoolUtil.spLog(
                        "StorpoolVMSnapshotStrategy.deleteVMSnapshot delete snapshot=%s of gropusnapshot=%s failed due to %s",
                        snapshotName, userVm.getInstanceName(), err);
                throw new CloudRuntimeException("Delete vm snapshot " + vmSnapshot.getName() + " of vm "
                        + userVm.getInstanceName() + " failed due to " + err);
            }
        }
        vmSnapshotDetailsDao.removeDetails(vmSnapshot.getId());

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

    @Override
    public boolean revertVMSnapshot(VMSnapshot vmSnapshot) {
        log.debug("Revert vm snapshot");
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());

        if (userVm.getState() == VirtualMachine.State.Running && vmSnapshotVO.getType() == VMSnapshot.Type.Disk) {
            throw new CloudRuntimeException("Virtual machine should be in stopped state for revert operation");
        }

        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.RevertRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());

            DataStore dataStore = dataStoreManager.getPrimaryDataStore(volumeTOs.get(0).getDataStore().getUuid());
            SpConnectionDesc conn = StorPoolUtil.getSpConnection(dataStore.getUuid(), dataStore.getId(), storagePoolDetailsDao, storagePool);
            for (VolumeObjectTO volumeObjectTO : volumeTOs) {
                String err = null;
                VMSnapshotDetailsVO snapshotDetailsVO = vmSnapshotDetailsDao.findDetail(vmSnapshot.getId(),
                        volumeObjectTO.getUuid());
                String snapshotName = StorPoolStorageAdaptor.getVolumeNameFromPath(snapshotDetailsVO.getValue(), true);
                if (snapshotName == null) {
                    err = String.format("Could not find StorPool's snapshot vm snapshot uuid=%s and volume uui=%s",
                            vmSnapshot.getUuid(), volumeObjectTO.getUuid());
                    log.error("Could not delete snapshot for vm:" + err);
                }
                String volumeName = StorPoolStorageAdaptor.getVolumeNameFromPath(volumeObjectTO.getPath(), true);
                VolumeDetailVO detail = volumeDetailsDao.findDetail(volumeObjectTO.getId(), StorPoolUtil.SP_PROVIDER_NAME);
                if (detail != null) {
                    SpApiResponse updateVolumeResponse = StorPoolUtil.volumeUpdateRename(volumeName, "", StorPoolStorageAdaptor.getVolumeNameFromPath(detail.getValue(), false), conn);

                    if (updateVolumeResponse.getError() != null) {
                        StorPoolUtil.spLog("StorpoolVMSnapshotStrategy.canHandle - Could not update StorPool's volume %s to it's globalId due to %s", volumeName, updateVolumeResponse.getError().getDescr());
                        err = String.format("StorpoolVMSnapshotStrategy.canHandle - Could not update StorPool's volume %s to it's globalId due to %s", volumeName, updateVolumeResponse.getError().getDescr());
                    } else {
                        volumeDetailsDao.remove(detail.getId());
                    }
                }

                SpApiResponse resp = StorPoolUtil.detachAllForced(volumeName, false, conn);
                if (resp.getError() != null) {
                    err = String.format("Could not detach StorPool volume %s from a group snapshot, due to %s",
                            volumeName, resp.getError());
                    throw new CloudRuntimeException(err);
                }
                resp = StorPoolUtil.volumeRevert(volumeName, snapshotName, conn);
                if (resp.getError() != null) {
                    err = String.format("Create Could not complete revert task for volumeName=%s , and snapshotName=%s",
                            volumeName, snapshotName);
                    throw new CloudRuntimeException(err);
                }
                VolumeInfo vinfo = volFactory.getVolume(volumeObjectTO.getId());
                if (vinfo.getMaxIops() != null) {
                    resp = StorPoolUtil.volumeUpadateTags(volumeName, null, vinfo.getMaxIops(), conn, null);

                    if (resp.getError() != null) {
                        StorPoolUtil.spLog("Volume was reverted successfully but max iops could not be set due to %s",
                                resp.getError().getDescr());
                    }
                }
            }
            finalizeRevert(vmSnapshotVO, volumeTOs);
            result = vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
        } catch (CloudRuntimeException | NoTransitionException  e) {
            String errMsg = String.format("Error while finalize create vm snapshot [%s] due to %s", vmSnapshot.getName(), e.getMessage());
            log.error(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        } finally {
            if (!result) {
                try {
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
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
                vmSnapshot.getName(), offeringId, volume.getId(), volumeTo.getSize(), VMSnapshot.class.getName(), vmSnapshot.getUuid());
    }

    private void publishUsageEvents(String type, VMSnapshot vmSnapshot, UserVm userVm, Long vmSnapSize, Long virtualSize) {
        try {
            UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(),
                    vmSnapshot.getName(), 0L, 0L, vmSnapSize, virtualSize, VMSnapshot.class.getName(),
                    vmSnapshot.getUuid());
        } catch (Exception e) {
            log.error("Failed to publis usage event " + type, e);
        }
    }
}
