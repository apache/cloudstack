// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.motion;


import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelMigrationAnswer;
import com.cloud.agent.api.CancelMigrationCommand;
import com.cloud.agent.api.MigrateWithStorageAnswer;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KVMStorageMotionStrategy  implements DataMotionStrategy {
    private static final Logger s_logger = Logger.getLogger(KVMStorageMotionStrategy.class);

    protected final ConfigKey<Integer> MigrateWait = new ConfigKey<Integer>(Integer.class, "migratewait", "Advanced", "3600", "Time (in seconds) to wait for VM migrate finish.", true, ConfigKey.Scope.Global, null);

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
    @Inject
    DiskOfferingDao diskOfferingDao;
    @Inject
    TemplateDataFactory tmplFactory;
    @Inject
    StorageManager storageManager;
    @Inject
    EndPointSelector endPointSelector;
    @Inject
    DataStoreManager dataStoreManager;

    @Override
    public StrategyPriority canHandle(DataObject srcData, DataObject destData) {
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        if (srcHost.getHypervisorType() == Hypervisor.HypervisorType.KVM && destHost.getHypervisorType() == Hypervisor.HypervisorType.KVM) {
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
                answer = migrateVmWithVolumes(instance, vmTo, srcHost, destHost, volumeMap);
                errMsg = answer.getDetails();

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Got answer from migration, result: " + (answer.getResult() ? "ok" : "failed, reason: " + (answer.getDetails() == null ? "<null>" : answer.getDetails())));
                }

            } else {
                throw new CloudRuntimeException("Unsupported operation requested for moving data.");
            }
        } catch (Exception e) {
            s_logger.error("copyAsync failed", e);
            errMsg = e.getMessage();
        }

        CopyCommandResult result = new CopyCommandResult("", answer);

        if (answer != null) {
            result.setSuccess(answer.getResult());
            if (!answer.getResult()) {
                result.setResult(errMsg);
            }
        } else {
            result.setSuccess(false);
            result.setResult(errMsg);
        }

        callback.complete(result);
    }

    private Answer migrateVmWithVolumes(VMInstanceVO vm, VirtualMachineTO to, Host srcHost, Host destHost, Map<VolumeInfo, DataStore> volumeToPool) throws AgentUnavailableException {

        // Initiate migration of a virtual machine with it's volumes.
        List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerTo = new ArrayList<Pair<VolumeTO, StorageFilerTO>>();
        VolumeVO rootVolume = null;
        VolumeInfo rootVolumeInfo = null;
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
            VolumeInfo volume = entry.getKey();
            VolumeTO volumeTo = new VolumeTO(volume, storagePoolDao.findById(volume.getPoolId()));
            StorageFilerTO filerTo = new StorageFilerTO((StoragePool)entry.getValue());
            volumeToFilerTo.add(new Pair<VolumeTO, StorageFilerTO>(volumeTo, filerTo));
            if (volume.getType().equals(DataObjectType.VOLUME) && volume.getVolumeType().equals(Volume.Type.ROOT)) {
                rootVolume = volDao.findById(volume.getId());
                rootVolumeInfo = volume;
            }
        }

        DiskOfferingVO diskOffering = diskOfferingDao.findById(rootVolume.getDiskOfferingId());
        DiskProfile diskProfile = new DiskProfile(rootVolume, diskOffering, vm.getHypervisorType());
        StoragePool destStoragePool = storageManager.findLocalStorageOnHost(destHost.getId());
        TemplateInfo templateImage = tmplFactory.getTemplate(rootVolume.getTemplateId(), DataStoreRole.Image);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Provisioning disk " + diskProfile.toString() + " on destination host");
        }

        try {
            CreateCommand provisioningCommand = new CreateCommand(diskProfile, templateImage.getUuid(), destStoragePool, true);
            CreateAnswer provisioningAnwer = (CreateAnswer) agentMgr.send(destHost.getId(), provisioningCommand);
            if (provisioningAnwer == null) {
                s_logger.error("Migration with storage of vm " + vm + " failed while provisioning root image");
                throw new CloudRuntimeException("Error while provisioning root image for the vm " + vm + " during migration to host " + destHost);
            } else if (!provisioningAnwer.getResult()) {
                s_logger.error("Migration with storage of vm " + vm + " failed. Details: " + provisioningAnwer.getDetails());
                throw new CloudRuntimeException("Error while provisioning root image for the vm " + vm + " during migration to host " + destHost +
                        ". " + provisioningAnwer.getDetails());
            }
        } catch (OperationTimedoutException e) {
            throw new AgentUnavailableException("Operation timed out while provisioning for migration for " + vm, destHost.getId());
        }

        MigrateWithStorageCommand command = null;
        try {
            command = new MigrateWithStorageCommand(to, volumeToFilerTo, destHost.getPrivateIpAddress());
            command.setWait(MigrateWait.value());
            MigrateWithStorageAnswer answer = (MigrateWithStorageAnswer) agentMgr.send(srcHost.getId(), command);
            if (answer == null) {
                s_logger.error("Migration with storage of vm " + vm + " failed.");
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            } else if (!answer.getResult()) {
                s_logger.error("Migration with storage of vm " + vm+ " failed. Details: " + answer.getDetails());
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost +
                        ". " + answer.getDetails());
            } else {
                // Update the volume details after migration.
                updateVolumePathsAfterMigration(volumeToPool, answer.getVolumeTos());
            }

            return answer;

        } catch (OperationTimedoutException e) {
            s_logger.error("Operation timeout error while migrating vm " + vm + " to host " + destHost + ", aborting the migration.", e);
            // Trying to abort the migration when we reach the timeout. It required to abort the job but we're not
            // sure it will work. Only when the command has been aborted successfully should we delete the remote
            // disk. Otherwise we leave everything as it is and leave it to a manual intervention to decide.
            try {
                CancelMigrationCommand cancelMigrationCommand = new CancelMigrationCommand(vm.getInstanceName());
                CancelMigrationAnswer cancelMigrationAnswer = (CancelMigrationAnswer) agentMgr.send(srcHost.getId(), cancelMigrationCommand);

                if (cancelMigrationAnswer.getResult()) {
                    s_logger.info("Migration aborted successfully.");
                    // We can safely delete the previously created disk at the destination
                    VolumeObjectTO volumeTO = new VolumeObjectTO(rootVolumeInfo);
                    DataStore destDataStore = dataStoreManager.getDataStore(destStoragePool.getId(), DataStoreRole.Primary);
                    s_logger.info("Requesting volume deletion on destination host " + destDataStore.getId());
                    volumeTO.setDataStore(destDataStore.getTO());
                    DeleteCommand dtCommand = new DeleteCommand(volumeTO);
                    EndPoint ep = endPointSelector.select(destDataStore);
                    if (ep != null) {
                        Answer answer = ep.sendMessage(dtCommand);
                        if (answer.getResult()) {
                            s_logger.info("Volume on the migration destination has been removed.");
                        } else {
                            s_logger.warn("Could not remove the volume on the migration destination.");
                        }
                    } else {
                        s_logger.error("Could not find an endpoint to send the delete command");
                    }

                } else {
                    s_logger.fatal("Could not abort the migration, manual intervention is required!");
                }
                return new MigrateWithStorageAnswer(command, false, cancelMigrationAnswer.getResult(), e);
            } catch (OperationTimedoutException e1) {
                s_logger.error("Timeout error while trying to abort the migration job", e);
                throw new AgentUnavailableException("Operation timed out on storage motion for " + vm + " but migration job could not be aborted. Manual intervention required!", destHost.getId());
            }
        }
    }

    private void updateVolumePathsAfterMigration(Map<VolumeInfo, DataStore> volumeToPool, List<VolumeObjectTO> volumeTos) {
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
            boolean updated = false;
            VolumeInfo volume = entry.getKey();
            StoragePool pool = (StoragePool)entry.getValue();
            for (VolumeObjectTO volumeTo : volumeTos) {
                if (volume.getId() == volumeTo.getId()) {
                    VolumeVO volumeVO = volDao.findById(volume.getId());
                    Long oldPoolId = volumeVO.getPoolId();
                    volumeVO.setPath(volumeTo.getPath());
                    volumeVO.setPodId(pool.getPodId());
                    volumeVO.setPoolId(pool.getId());
                    volumeVO.setLastPoolId(oldPoolId);
                    volumeVO.setFolder(pool.getPath());

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
