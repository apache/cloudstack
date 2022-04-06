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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotStrategy;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.api.SnapshotGroup;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.alert.AlertManager;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.server.ManagementServerImpl;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

public class ScaleIOVMSnapshotStrategy extends ManagerBase implements VMSnapshotStrategy {
    private static final Logger LOGGER = Logger.getLogger(ScaleIOVMSnapshotStrategy.class);
    @Inject
    VMSnapshotHelper vmSnapshotHelper;
    @Inject
    UserVmDao userVmDao;
    @Inject
    VMSnapshotDao vmSnapshotDao;
    @Inject
    protected VMSnapshotDetailsDao vmSnapshotDetailsDao;
    int _wait;
    @Inject
    ConfigurationDao configurationDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    DiskOfferingDao diskOfferingDao;
    @Inject
    PrimaryDataStoreDao storagePoolDao;
    @Inject
    StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    AlertManager alertManager;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        String value = configurationDao.getValue("vmsnapshot.create.wait");
        _wait = NumbersUtil.parseInt(value, 1800);
        return true;
    }

    @Override
    public StrategyPriority canHandle(VMSnapshot vmSnapshot) {
        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vmSnapshot.getVmId());
        if (volumeTOs == null) {
            throw new CloudRuntimeException("Failed to get the volumes for the vm snapshot: " + vmSnapshot.getUuid());
        }

        if (volumeTOs != null && !volumeTOs.isEmpty()) {
            for (VolumeObjectTO volumeTO: volumeTOs) {
                Long poolId  = volumeTO.getPoolId();
                Storage.StoragePoolType poolType = vmSnapshotHelper.getStoragePoolType(poolId);
                if (poolType != Storage.StoragePoolType.PowerFlex) {
                    return StrategyPriority.CANT_HANDLE;
                }
            }
        }

        return StrategyPriority.HIGHEST;
    }

    @Override
    public VMSnapshot takeVMSnapshot(VMSnapshot vmSnapshot) {
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO)vmSnapshot;

        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.CreateRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
            Map<String, String> srcVolumeDestSnapshotMap = new HashMap<>();
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());

            final Long storagePoolId = vmSnapshotHelper.getStoragePoolForVM(userVm.getId());
            StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);
            long prev_chain_size = 0;
            long virtual_size=0;
            for (VolumeObjectTO volume : volumeTOs) {
                String volumeSnapshotName = String.format("%s-%s-%s-%s-%s", ScaleIOUtil.VMSNAPSHOT_PREFIX, vmSnapshotVO.getId(), volume.getId(),
                        storagePool.getUuid().split("-")[0].substring(4), ManagementServerImpl.customCsIdentifier.value());
                srcVolumeDestSnapshotMap.put(ScaleIOUtil.getVolumePath(volume.getPath()), volumeSnapshotName);

                virtual_size += volume.getSize();
                VolumeVO volumeVO = volumeDao.findById(volume.getId());
                prev_chain_size += volumeVO.getVmSnapshotChainSize() == null ? 0 : volumeVO.getVmSnapshotChainSize();
            }

            VMSnapshotTO current = null;
            VMSnapshotVO currentSnapshot = vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
            if (currentSnapshot != null) {
                current = vmSnapshotHelper.getSnapshotWithParents(currentSnapshot);
            }

            if (current == null)
                vmSnapshotVO.setParent(null);
            else
                vmSnapshotVO.setParent(current.getId());

            try {
                final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
                SnapshotGroup snapshotGroup = client.takeSnapshot(srcVolumeDestSnapshotMap);
                if (snapshotGroup == null) {
                    throw new CloudRuntimeException("Failed to take VM snapshot on PowerFlex storage pool");
                }

                String snapshotGroupId = snapshotGroup.getSnapshotGroupId();
                List<String> volumeIds = snapshotGroup.getVolumeIds();
                if (volumeIds != null && !volumeIds.isEmpty()) {
                    List<VMSnapshotDetailsVO> vmSnapshotDetails = new ArrayList<VMSnapshotDetailsVO>();
                    vmSnapshotDetails.add(new VMSnapshotDetailsVO(vmSnapshot.getId(), "SnapshotGroupId", snapshotGroupId, false));

                    for (int index = 0; index < volumeIds.size(); index++) {
                        String volumeSnapshotName = srcVolumeDestSnapshotMap.get(ScaleIOUtil.getVolumePath(volumeTOs.get(index).getPath()));
                        String pathWithScaleIOVolumeName = ScaleIOUtil.updatedPathWithVolumeName(volumeIds.get(index), volumeSnapshotName);
                        vmSnapshotDetails.add(new VMSnapshotDetailsVO(vmSnapshot.getId(), "Vol_" + volumeTOs.get(index).getId() + "_Snapshot", pathWithScaleIOVolumeName, false));
                    }

                    vmSnapshotDetailsDao.saveDetails(vmSnapshotDetails);
                }

                finalizeCreate(vmSnapshotVO, volumeTOs);
                result = true;
                LOGGER.debug("Create vm snapshot " + vmSnapshot.getName() + " succeeded for vm: " + userVm.getInstanceName());

                long new_chain_size=0;
                for (VolumeObjectTO volumeTo : volumeTOs) {
                    publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_CREATE, vmSnapshot, userVm, volumeTo);
                    new_chain_size += volumeTo.getSize();
                }
                publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_ON_PRIMARY, vmSnapshot, userVm, new_chain_size - prev_chain_size, virtual_size);
                return vmSnapshot;
            } catch (Exception e) {
                String errMsg = "Unable to take vm snapshot due to: " + e.getMessage();
                LOGGER.warn(errMsg, e);
                throw new CloudRuntimeException(errMsg);
            }
        } finally {
            if (!result) {
                try {
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);

                    String subject = "Take snapshot failed for VM: " + userVm.getDisplayName();
                    String message = "Snapshot operation failed for VM: " + userVm.getDisplayName() + ", Please check and delete if any stale volumes created with VM snapshot id: " + vmSnapshot.getVmId();
                    alertManager.sendAlert(AlertManager.AlertType.ALERT_TYPE_VM_SNAPSHOT, userVm.getDataCenterId(), userVm.getPodIdToDeployIn(), subject, message);
                } catch (NoTransitionException e1) {
                    LOGGER.error("Cannot set vm snapshot state due to: " + e1.getMessage());
                }
            }
        }
    }

    @DB
    protected void finalizeCreate(VMSnapshotVO vmSnapshot, List<VolumeObjectTO> volumeTOs) {
        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<NoTransitionException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws NoTransitionException {
                    // update chain size for the volumes in the VM snapshot
                    for (VolumeObjectTO volume : volumeTOs) {
                        VolumeVO volumeVO = volumeDao.findById(volume.getId());
                        if (volumeVO != null) {
                            long vmSnapshotChainSize = volumeVO.getVmSnapshotChainSize() == null ? 0 : volumeVO.getVmSnapshotChainSize();
                            vmSnapshotChainSize += volumeVO.getSize();
                            volumeVO.setVmSnapshotChainSize(vmSnapshotChainSize);
                            volumeDao.persist(volumeVO);
                        }
                    }

                    vmSnapshot.setCurrent(true);

                    // change current snapshot
                    if (vmSnapshot.getParent() != null) {
                        VMSnapshotVO previousCurrent = vmSnapshotDao.findById(vmSnapshot.getParent());
                        previousCurrent.setCurrent(false);
                        vmSnapshotDao.persist(previousCurrent);
                    }
                    vmSnapshotDao.persist(vmSnapshot);

                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
                }
            });
        } catch (Exception e) {
            String errMsg = "Error while finalize create vm snapshot: " + vmSnapshot.getName() + " due to " + e.getMessage();
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @Override
    public boolean revertVMSnapshot(VMSnapshot vmSnapshot) {
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO)vmSnapshot;
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());

        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.RevertRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
            Long storagePoolId = vmSnapshotHelper.getStoragePoolForVM(userVm.getId());
            Map<String, String> srcSnapshotDestVolumeMap = new HashMap<>();
            for (VolumeObjectTO volume : volumeTOs) {
                VMSnapshotDetailsVO vmSnapshotDetail = vmSnapshotDetailsDao.findDetail(vmSnapshotVO.getId(), "Vol_" + volume.getId() + "_Snapshot");
                String srcSnapshotVolumeId = ScaleIOUtil.getVolumePath(vmSnapshotDetail.getValue());
                String destVolumeId = ScaleIOUtil.getVolumePath(volume.getPath());
                srcSnapshotDestVolumeMap.put(srcSnapshotVolumeId, destVolumeId);
            }

            String systemId = storagePoolDetailsDao.findDetail(storagePoolId, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID).getValue();
            if (systemId == null) {
                throw new CloudRuntimeException("Failed to get the system id for PowerFlex storage pool for reverting VM snapshot: " + vmSnapshot.getName());
            }

            final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
            result = client.revertSnapshot(systemId, srcSnapshotDestVolumeMap);
            if (!result) {
                throw new CloudRuntimeException("Failed to revert VM snapshot on PowerFlex storage pool");
            }

            finalizeRevert(vmSnapshotVO, volumeTOs);
            result = true;
        } catch (Exception e) {
            String errMsg = "Revert VM: " + userVm.getInstanceName() + " to snapshot: " + vmSnapshotVO.getName() + " failed due to " + e.getMessage();
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        } finally {
            if (!result) {
                try {
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    LOGGER.error("Cannot set vm snapshot state due to: " + e1.getMessage());
                }
            }
        }
        return result;
    }

    @DB
    protected void finalizeRevert(VMSnapshotVO vmSnapshot, List<VolumeObjectTO> volumeToList) {
        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<NoTransitionException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws NoTransitionException {
                    // update chain size for the volumes in the VM snapshot
                    for (VolumeObjectTO volume : volumeToList) {
                        VolumeVO volumeVO = volumeDao.findById(volume.getId());
                        if (volumeVO != null && volumeVO.getVmSnapshotChainSize() != null && volumeVO.getVmSnapshotChainSize() >= volumeVO.getSize()) {
                            long vmSnapshotChainSize = volumeVO.getVmSnapshotChainSize() - volumeVO.getSize();
                            volumeVO.setVmSnapshotChainSize(vmSnapshotChainSize);
                            volumeDao.persist(volumeVO);
                        }
                    }

                    // update current snapshot, current snapshot is the one reverted to
                    VMSnapshotVO previousCurrent = vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
                    if (previousCurrent != null) {
                        previousCurrent.setCurrent(false);
                        vmSnapshotDao.persist(previousCurrent);
                    }
                    vmSnapshot.setCurrent(true);
                    vmSnapshotDao.persist(vmSnapshot);

                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
                }
            });
        } catch (Exception e) {
            String errMsg = "Error while finalize revert vm snapshot: " + vmSnapshot.getName() + " due to " + e.getMessage();
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @Override
    public boolean deleteVMSnapshot(VMSnapshot vmSnapshot) {
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO)vmSnapshot;

        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.ExpungeRequested);
        } catch (NoTransitionException e) {
            LOGGER.debug("Failed to change vm snapshot state with event ExpungeRequested");
            throw new CloudRuntimeException("Failed to change vm snapshot state with event ExpungeRequested: " + e.getMessage());
        }

        try {
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vmSnapshot.getVmId());
            Long storagePoolId = vmSnapshotHelper.getStoragePoolForVM(userVm.getId());
            String systemId = storagePoolDetailsDao.findDetail(storagePoolId, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID).getValue();
            if (systemId == null) {
                throw new CloudRuntimeException("Failed to get the system id for PowerFlex storage pool for deleting VM snapshot: " + vmSnapshot.getName());
            }

            VMSnapshotDetailsVO vmSnapshotDetailsVO = vmSnapshotDetailsDao.findDetail(vmSnapshot.getId(), "SnapshotGroupId");
            if (vmSnapshotDetailsVO == null) {
                throw new CloudRuntimeException("Failed to get snapshot group id for the VM snapshot: " + vmSnapshot.getName());
            }

            String snapshotGroupId = vmSnapshotDetailsVO.getValue();
            final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
            int volumesDeleted = client.deleteSnapshotGroup(systemId, snapshotGroupId);
            if (volumesDeleted <= 0) {
                throw new CloudRuntimeException("Failed to delete VM snapshot: " + vmSnapshot.getName());
            } else if (volumesDeleted != volumeTOs.size()) {
                LOGGER.warn("Unable to delete all volumes of the VM snapshot: " + vmSnapshot.getName());
            }

            finalizeDelete(vmSnapshotVO, volumeTOs);
            long full_chain_size=0;
            for (VolumeObjectTO volumeTo : volumeTOs) {
                publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_DELETE, vmSnapshot, userVm, volumeTo);
                full_chain_size += volumeTo.getSize();
            }
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY, vmSnapshot, userVm, full_chain_size, 0L);
            return true;
        } catch (Exception e) {
            String errMsg = "Unable to delete vm snapshot: " + vmSnapshot.getName() + " of vm " + userVm.getInstanceName() + " due to " + e.getMessage();
            LOGGER.warn(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @DB
    protected void finalizeDelete(VMSnapshotVO vmSnapshot, List<VolumeObjectTO> volumeTOs) {
        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<NoTransitionException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws NoTransitionException {
                    // update chain size for the volumes in the VM snapshot
                    for (VolumeObjectTO volume : volumeTOs) {
                        VolumeVO volumeVO = volumeDao.findById(volume.getId());
                        if (volumeVO != null && volumeVO.getVmSnapshotChainSize() != null && volumeVO.getVmSnapshotChainSize() >= volumeVO.getSize()) {
                            long vmSnapshotChainSize = volumeVO.getVmSnapshotChainSize() - volumeVO.getSize();
                            volumeVO.setVmSnapshotChainSize(vmSnapshotChainSize);
                            volumeDao.persist(volumeVO);
                        }
                    }

                    // update children's parent snapshots
                    List<VMSnapshotVO> children = vmSnapshotDao.listByParent(vmSnapshot.getId());
                    for (VMSnapshotVO child : children) {
                        child.setParent(vmSnapshot.getParent());
                        vmSnapshotDao.persist(child);
                    }

                    // update current snapshot
                    VMSnapshotVO current = vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
                    if (current != null && current.getId() == vmSnapshot.getId() && vmSnapshot.getParent() != null) {
                        VMSnapshotVO parent = vmSnapshotDao.findById(vmSnapshot.getParent());
                        parent.setCurrent(true);
                        vmSnapshotDao.persist(parent);
                    }
                    vmSnapshot.setCurrent(false);
                    vmSnapshotDao.persist(vmSnapshot);

                    vmSnapshotDao.remove(vmSnapshot.getId());
                }
            });
        } catch (Exception e) {
            String errMsg = "Error while finalize delete vm snapshot: " + vmSnapshot.getName() + " due to " + e.getMessage();
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @Override
    public boolean deleteVMSnapshotFromDB(VMSnapshot vmSnapshot, boolean unmanage) {
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.ExpungeRequested);
        } catch (NoTransitionException e) {
            LOGGER.debug("Failed to change vm snapshot state with event ExpungeRequested");
            throw new CloudRuntimeException("Failed to change vm snapshot state with event ExpungeRequested: " + e.getMessage());
        }
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());
        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
        long full_chain_size = 0;
        for (VolumeObjectTO volumeTo: volumeTOs) {
            volumeTo.setSize(0);
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_DELETE, vmSnapshot, userVm, volumeTo);
            full_chain_size += volumeTo.getSize();
        }
        if (unmanage) {
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY, vmSnapshot, userVm, full_chain_size, 0L);
        }
        return vmSnapshotDao.remove(vmSnapshot.getId());
    }

    private void publishUsageEvent(String type, VMSnapshot vmSnapshot, UserVm userVm, VolumeObjectTO volumeTo) {
        VolumeVO volume = volumeDao.findById(volumeTo.getId());
        Long diskOfferingId = volume.getDiskOfferingId();
        Long offeringId = null;
        if (diskOfferingId != null) {
            DiskOfferingVO offering = diskOfferingDao.findById(diskOfferingId);
            if (offering != null && !offering.isComputeOnly()) {
                offeringId = offering.getId();
            }
        }
        Map<String, String> details = new HashMap<>();
        if (vmSnapshot != null) {
            details.put(UsageEventVO.DynamicParameters.vmSnapshotId.name(), String.valueOf(vmSnapshot.getId()));
        }
        UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(), vmSnapshot.getName(), offeringId, volume.getId(), // save volume's id into templateId field
                volumeTo.getSize(), VMSnapshot.class.getName(), vmSnapshot.getUuid(), details);
    }

    private void publishUsageEvent(String type, VMSnapshot vmSnapshot, UserVm userVm, Long vmSnapSize, Long virtualSize) {
        try {
            Map<String, String> details = new HashMap<>();
            if (vmSnapshot != null) {
                details.put(UsageEventVO.DynamicParameters.vmSnapshotId.name(), String.valueOf(vmSnapshot.getId()));
            }
            UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(), vmSnapshot.getName(), 0L, 0L, vmSnapSize, virtualSize,
                    VMSnapshot.class.getName(), vmSnapshot.getUuid(), details);
        } catch (Exception e) {
            LOGGER.error("Failed to publish usage event " + type, e);
        }
    }

    private ScaleIOGatewayClient getScaleIOClient(final Long storagePoolId) throws Exception {
        return ScaleIOGatewayClientConnectionPool.getInstance().getClient(storagePoolId, storagePoolDetailsDao);
    }
}
