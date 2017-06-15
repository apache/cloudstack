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

package org.apache.cloudstack.storage.motion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VmSnapshotObject;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.VmSnapshotTemplateObject;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVolumeFromVMSnapshotCommand;
import com.cloud.agent.api.MigrateWithStorageAnswer;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.SeedTemplateFromVmSnapshotCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class VmwareStorageMotionStrategy implements DataMotionStrategy {
    private static final Logger s_logger = Logger.getLogger(VmwareStorageMotionStrategy.class);
    @Inject
    AgentManager agentMgr;
    @Inject
    VolumeDao volDao;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    PrimaryDataStoreDao storagePoolDao;
    @Inject
    VMInstanceDao instanceDao;

    @Override
    public StrategyPriority canHandle(DataObject srcData, DataObject destData) {
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        if (srcHost.getHypervisorType() == HypervisorType.VMware && destHost.getHypervisorType() == HypervisorType.VMware) {
            s_logger.debug(this.getClass() + " can handle the request because the hosts have VMware hypervisor");
            return StrategyPriority.HYPERVISOR;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyAsync(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTo, Host srcHost, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        Answer answer = null;
        String errMsg = null;
        try {
            VMInstanceVO instance = instanceDao.findById(vmTo.getId());
            if (instance != null) {
                if (srcHost.getClusterId().equals(destHost.getClusterId())) {
                    answer = migrateVmWithVolumesWithinCluster(instance, vmTo, srcHost, destHost, volumeMap);
                } else {
                    answer = migrateVmWithVolumesAcrossCluster(instance, vmTo, srcHost, destHost, volumeMap);
                }
            } else {
                throw new CloudRuntimeException("Unsupported operation requested for moving data.");
            }
        } catch (Exception e) {
            s_logger.error("copy failed", e);
            errMsg = e.toString();
        }

        CopyCommandResult result = new CopyCommandResult(null, answer);
        result.setResult(errMsg);
        callback.complete(result);
    }

    @Override
    public StrategyPriority canHandle(DataObject templateOnPrimaryStoreObj, VmSnapshotObject vmSnapshotObj, UserVm userVm, Host tgtHost) {
        if (tgtHost.getHypervisorType() == HypervisorType.VMware) {
            s_logger.debug(this.getClass() + " can handle the request because the target is a VMware hypervisor host");
            return StrategyPriority.HYPERVISOR;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public Void copyAsync(DataObject templateOnPrimaryStoreObj, VmSnapshotObject vmSnapshotObj, UserVm userVm, Host tgtHost,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        Answer answer = null;
        String errMsg = null;
        try {
            VMInstanceVO instance = instanceDao.findById(vmSnapshotObj.getVmSnapshot().getVmId());
            if (instance != null && tgtHost != null) {
                answer = createTemplateFromVmSnapshot(templateOnPrimaryStoreObj, userVm, vmSnapshotObj, tgtHost);
            } else {
                throw new CloudRuntimeException("Unsupported operation requested for moving data.");
            }
        } catch (Exception e) {
            s_logger.error("copy failed", e);
            errMsg = e.toString();
        }

        CopyCommandResult result = new CopyCommandResult(null, answer);
        result.setResult(errMsg);
        callback.complete(result);
        return null;
    }

    private Answer createTemplateFromVmSnapshot(DataObject templateOnPrimaryStoreObj, UserVm userVm, VmSnapshotObject vmSnapshotObj, Host tgtHost)
            throws AgentUnavailableException {
        DataStore templateDataStore = templateOnPrimaryStoreObj.getDataStore();

        String tempalteDsUuid = null;
        if (templateDataStore != null) {
            tempalteDsUuid = templateDataStore.getUuid();
        } else {
            throw new CloudRuntimeException("Invalid primary storage pool detected for template while trying seed template from vm snapshot : "
                    + vmSnapshotObj.getVmSnapshot().getUuid());
        }

        // Get datastore for root volume of VM owning the VM snapshot
        List<VolumeInfo> vols = vmSnapshotObj.getVolumes();
        String rootVolPoolUuid = null;
        DataStore rootVolDs = null;
        String rootVolumePath = null;
        for (VolumeInfo vol : vols) {
            if (vol.getVolumeType() == Volume.Type.ROOT) {
                rootVolDs = vol.getDataStore();
                rootVolumePath = vol.getPath();
                break;
            }
        }
        if (rootVolDs != null) {
            rootVolPoolUuid = rootVolDs.getUuid();
        } else {
            throw new CloudRuntimeException("Invalid primary storage pool detected for root volume of instance : " + userVm.getInstanceName());
        }

        // Seed the template on primary storage pool
        String taskMsg = "seed template [" + templateOnPrimaryStoreObj.getUuid() +
                "] on storage pool [" + templateDataStore.getUuid() +
                "] from VM snapshot [" + vmSnapshotObj.getVmSnapshot().getUuid() +
                "] using host [" + tgtHost.getUuid() + "]";

        s_logger.info("Trying to " + taskMsg);

        // Clone Root volume of vm snapshot to seed template in primary storage
        try {
            String srcVmSnapshotName = vmSnapshotObj.getVmSnapshot().getName();
            String srcVmSnapshotUuid = vmSnapshotObj.getVmSnapshot().getUuid();
            String vmName = userVm.getInstanceName();
            String templateName = ((VmSnapshotTemplateObject)templateOnPrimaryStoreObj).getUniqueName();
            String dataStoreUuid = templateOnPrimaryStoreObj.getDataStore().getUuid();
            String dsName = templateOnPrimaryStoreObj.getDataStore().getName();

            SeedTemplateFromVmSnapshotCommand seedTemplateFromVmSnapshotCmd = new SeedTemplateFromVmSnapshotCommand(templateName,
                    dsName, dataStoreUuid, rootVolumePath, srcVmSnapshotName, srcVmSnapshotUuid, vmName);

            CopyCmdAnswer seedTemplateFromVmSnapshotAnswer = (CopyCmdAnswer)agentMgr.send(tgtHost.getId(), seedTemplateFromVmSnapshotCmd);
            if (seedTemplateFromVmSnapshotAnswer == null) {
                s_logger.error("Failed to " + taskMsg);
                throw new CloudRuntimeException("Failed to " + taskMsg);
            } else if (!seedTemplateFromVmSnapshotAnswer.getResult()) {
                s_logger.error("Failed to " + taskMsg + ". Details: " + seedTemplateFromVmSnapshotAnswer.getDetails());
                throw new CloudRuntimeException("Failed to " + taskMsg + ". Details: " + seedTemplateFromVmSnapshotAnswer.getDetails());
            } else {
                // Post template creation tasks
            }
            s_logger.info("Successfully completed the operation to " + taskMsg);

            return seedTemplateFromVmSnapshotAnswer;
        } catch (OperationTimedoutException e) {
            s_logger.error("Error trying to : " + taskMsg, e);
            throw new AgentUnavailableException("Operation timed out to " + taskMsg, tgtHost.getId());
        }
    }

    @Override
    public StrategyPriority canHandle(DataObject volumeOnStore, VmSnapshotObject vmSnapshot, Volume srcVolume, UserVm userVm, Host tgtHost) {
        if (tgtHost.getHypervisorType() == HypervisorType.VMware) {
            s_logger.debug(this.getClass() + " can handle the request because the target is a VMware hypervisor host");
            return StrategyPriority.HYPERVISOR;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public Void copyAsync(DataObject volumeOnStore, VmSnapshotObject vmSnapshotObj, Volume srcVolume, UserVm userVm, Host tgtHost,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        Answer answer = null;
        String errMsg = null;
        try {
            VMInstanceVO instance = instanceDao.findById(vmSnapshotObj.getVmSnapshot().getVmId());
            if (instance != null && tgtHost != null) {
                answer = createVolumeFromVmSnapshot(volumeOnStore, userVm, vmSnapshotObj, tgtHost, srcVolume);
            } else {
                throw new CloudRuntimeException("Unsupported operation requested for moving data.");
            }
        } catch (Exception e) {
            s_logger.error("copy failed", e);
            errMsg = e.toString();
        }

        CopyCommandResult result = new CopyCommandResult(null, answer);
        result.setResult(errMsg);
        callback.complete(result);
        return null;
    }

    private Answer createVolumeFromVmSnapshot(DataObject volumeOnStore, UserVm userVm, VmSnapshotObject vmSnapshotObj, Host tgtHost, Volume srcVolume)
            throws AgentUnavailableException {
        DataStore newVolDataStore = volumeOnStore.getDataStore();

        String newDataVolDsUuid = null;
        if (newVolDataStore != null) {
            newDataVolDsUuid = newVolDataStore.getUuid();
        } else {
            throw new CloudRuntimeException("Invalid primary storage pool detected for volume while creating volume from vm snapshot : "
                    + vmSnapshotObj.getVmSnapshot().getUuid());
        }

        // Get datastore for root volume of VM owning the VM snapshot
        List<VolumeInfo> vols = vmSnapshotObj.getVolumes();
        String dataVolPoolUuid = null;
        DataStore rootVolDs = null;
        String srcDataVolumePath = srcVolume.getPath();
        String dataVolumePathInVm = null;

        for (VolumeInfo vol : vols) {
            if (vol.getVolumeType() == Volume.Type.DATADISK) {
                dataVolumePathInVm = vol.getPath();
                if (dataVolumePathInVm.equals(srcDataVolumePath)) {
                    dataVolPoolUuid = vol.getDataStore().getUuid();
                    break;
                }
            }
        }

        // Validate if volume needs to be created is allocated the same storage pool as that of the data volume of VM owning the VM snapshot
        // This check should go if we extend the functionality to seed to any storage pool in zone
        if (dataVolPoolUuid != null && newDataVolDsUuid != null) {
            if (!dataVolPoolUuid.equals(newDataVolDsUuid)) {
                String msg = "Unable to create volume from vm snapshot on storage pool : " + newDataVolDsUuid +
                        ", because the vm snapshot is on a different storage pool : " + dataVolPoolUuid +
                        ". Volume creation from vmsnapshot is supported only if the vm snapshot is also present on same storage pool.";
                s_logger.warn(msg);
                throw new CloudRuntimeException(msg);
            }
        }

        // Seed the template on primary storage pool
        String taskMsg = "create volume [" + volumeOnStore.getUuid() +
                "] on storage pool [" + newDataVolDsUuid +
                "] from VM snapshot [" + vmSnapshotObj.getVmSnapshot().getUuid() +
                "] using host [" + tgtHost.getUuid() + "]";

        s_logger.info("Trying to " + taskMsg);

        // Clone data volume from specified volume in vm snapshot on to specified primary storage
        try {
            String srcVmSnapshotName = vmSnapshotObj.getVmSnapshot().getName();
            String srcVmSnapshotUuid = vmSnapshotObj.getVmSnapshot().getUuid();
            String vmName = userVm.getInstanceName();
            String dataStoreUuid = volumeOnStore.getDataStore().getUuid();
            String dsName = volumeOnStore.getDataStore().getName();
            CreateVolumeFromVMSnapshotCommand createVolumeFromVMSnapshotCommand = new CreateVolumeFromVMSnapshotCommand(dsName, dataStoreUuid, srcDataVolumePath,
                    srcVmSnapshotName, srcVmSnapshotUuid, vmName);
            CopyCmdAnswer createVolumeFromVMSnapshotAnswer = (CopyCmdAnswer)agentMgr.send(tgtHost.getId(), createVolumeFromVMSnapshotCommand);
            if (createVolumeFromVMSnapshotAnswer == null) {
                s_logger.error("Failed to " + taskMsg);
                throw new CloudRuntimeException("Failed to " + taskMsg);
            } else if (!createVolumeFromVMSnapshotAnswer.getResult()) {
                s_logger.error("Failed to " + taskMsg + ". Details: " + createVolumeFromVMSnapshotAnswer.getDetails());
                throw new CloudRuntimeException("Failed to " + taskMsg + ". Details: " + createVolumeFromVMSnapshotAnswer.getDetails());
            }
            s_logger.info("Successfully completed the operation to " + taskMsg);

            return createVolumeFromVMSnapshotAnswer;
        } catch (OperationTimedoutException e) {
            s_logger.error("Error trying to : " + taskMsg, e);
            throw new AgentUnavailableException("Operation timed out to " + taskMsg, tgtHost.getId());
        }
    }

    private Answer migrateVmWithVolumesAcrossCluster(VMInstanceVO vm, VirtualMachineTO to, Host srcHost, Host destHost, Map<VolumeInfo, DataStore> volumeToPool)
            throws AgentUnavailableException {

        // Initiate migration of a virtual machine with it's volumes.
        try {
            List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerto = new ArrayList<Pair<VolumeTO, StorageFilerTO>>();
            for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
                VolumeInfo volume = entry.getKey();
                VolumeTO volumeTo = new VolumeTO(volume, storagePoolDao.findById(volume.getPoolId()));
                StorageFilerTO filerTo = new StorageFilerTO((StoragePool)entry.getValue());
                volumeToFilerto.add(new Pair<VolumeTO, StorageFilerTO>(volumeTo, filerTo));
            }

            // Migration across cluster needs to be done in three phases.
            // 1. Send a migrate command to source resource to initiate migration
            //      Run validations against target!!
            // 2. Complete the process. Update the volume details.
            MigrateWithStorageCommand migrateWithStorageCmd = new MigrateWithStorageCommand(to, volumeToFilerto, destHost.getGuid());
            MigrateWithStorageAnswer migrateWithStorageAnswer = (MigrateWithStorageAnswer)agentMgr.send(srcHost.getId(), migrateWithStorageCmd);
            if (migrateWithStorageAnswer == null) {
                s_logger.error("Migration with storage of vm " + vm + " to host " + destHost + " failed.");
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            } else if (!migrateWithStorageAnswer.getResult()) {
                s_logger.error("Migration with storage of vm " + vm + " failed. Details: " + migrateWithStorageAnswer.getDetails());
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost + ". " + migrateWithStorageAnswer.getDetails());
            } else {
                // Update the volume details after migration.
                updateVolumesAfterMigration(volumeToPool, migrateWithStorageAnswer.getVolumeTos());
            }
            s_logger.debug("Storage migration of VM " + vm.getInstanceName() + " completed successfully. Migrated to host " + destHost.getName());

            return migrateWithStorageAnswer;
        } catch (OperationTimedoutException e) {
            s_logger.error("Error while migrating vm " + vm + " to host " + destHost, e);
            throw new AgentUnavailableException("Operation timed out on storage motion for " + vm, destHost.getId());
        }
    }

    private Answer migrateVmWithVolumesWithinCluster(VMInstanceVO vm, VirtualMachineTO to, Host srcHost, Host destHost, Map<VolumeInfo, DataStore> volumeToPool)
            throws AgentUnavailableException {

        // Initiate migration of a virtual machine with it's volumes.
        try {
            List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerto = new ArrayList<Pair<VolumeTO, StorageFilerTO>>();
            for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
                VolumeInfo volume = entry.getKey();
                VolumeTO volumeTo = new VolumeTO(volume, storagePoolDao.findById(volume.getPoolId()));
                StorageFilerTO filerTo = new StorageFilerTO((StoragePool)entry.getValue());
                volumeToFilerto.add(new Pair<VolumeTO, StorageFilerTO>(volumeTo, filerTo));
            }

            MigrateWithStorageCommand command = new MigrateWithStorageCommand(to, volumeToFilerto, destHost.getGuid());
            MigrateWithStorageAnswer answer = (MigrateWithStorageAnswer)agentMgr.send(srcHost.getId(), command);
            if (answer == null) {
                s_logger.error("Migration with storage of vm " + vm + " failed.");
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            } else if (!answer.getResult()) {
                s_logger.error("Migration with storage of vm " + vm + " failed. Details: " + answer.getDetails());
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost + ". " + answer.getDetails());
            } else {
                // Update the volume details after migration.
                updateVolumesAfterMigration(volumeToPool, answer.getVolumeTos());
            }

            return answer;
        } catch (OperationTimedoutException e) {
            s_logger.error("Error while migrating vm " + vm + " to host " + destHost, e);
            throw new AgentUnavailableException("Operation timed out on storage motion for " + vm, destHost.getId());
        }
    }

    private void updateVolumesAfterMigration(Map<VolumeInfo, DataStore> volumeToPool, List<VolumeObjectTO> volumeTos) {
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
            boolean updated = false;
            VolumeInfo volume = entry.getKey();
            StoragePool pool = (StoragePool)entry.getValue();
            for (VolumeObjectTO volumeTo : volumeTos) {
                if (volume.getId() == volumeTo.getId()) {
                    VolumeVO volumeVO = volDao.findById(volume.getId());
                    Long oldPoolId = volumeVO.getPoolId();
                    volumeVO.setPath(volumeTo.getPath());
                    if (volumeTo.getChainInfo() != null) {
                        volumeVO.setChainInfo(volumeTo.getChainInfo());
                    }
                    volumeVO.setLastPoolId(oldPoolId);
                    volumeVO.setFolder(pool.getPath());
                    volumeVO.setPodId(pool.getPodId());
                    volumeVO.setPoolId(pool.getId());
                    volDao.update(volume.getId(), volumeVO);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                s_logger.error("Volume path wasn't updated for volume " + volume + " after it was migrated.");
            }
        }
    }
}
