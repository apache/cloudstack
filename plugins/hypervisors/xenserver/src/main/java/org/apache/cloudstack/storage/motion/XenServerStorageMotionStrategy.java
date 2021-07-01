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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.MigrateWithStorageAnswer;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.MigrateWithStorageCompleteAnswer;
import com.cloud.agent.api.MigrateWithStorageCompleteCommand;
import com.cloud.agent.api.MigrateWithStorageReceiveAnswer;
import com.cloud.agent.api.MigrateWithStorageReceiveCommand;
import com.cloud.agent.api.MigrateWithStorageSendAnswer;
import com.cloud.agent.api.MigrateWithStorageSendCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.xenserver.resource.CitrixHelper;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class XenServerStorageMotionStrategy implements DataMotionStrategy {
    private static final Logger s_logger = Logger.getLogger(XenServerStorageMotionStrategy.class);
    @Inject
    AgentManager agentMgr;
    @Inject
    VolumeDao volDao;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    PrimaryDataStoreDao storagePoolDao;
    @Inject
    private VolumeDetailsDao volumeDetailsDao;
    @Inject
    VMInstanceDao instanceDao;
    @Inject
    SnapshotDao snapshotDao;

    @Override
    public StrategyPriority canHandle(DataObject srcData, DataObject destData) {
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        if (srcHost.getHypervisorType() == HypervisorType.XenServer && destHost.getHypervisorType() == HypervisorType.XenServer) {
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

    private String getBasicIqn(long volumeId) {
        VolumeDetailVO volumeDetail = volumeDetailsDao.findDetail(volumeId, PrimaryDataStoreDriver.BASIC_IQN);

        return volumeDetail.getValue();
    }

    private void verifyNoSnapshotsOnManagedStorageVolumes(Map<VolumeInfo, DataStore> volumeToPool) {
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
            VolumeInfo volumeInfo = entry.getKey();
            StoragePool storagePool = storagePoolDao.findById(volumeInfo.getPoolId());

            if (storagePool.isManaged()) {
                List<SnapshotVO> snapshots = getNonDestroyedSnapshots(volumeInfo.getId());

                if (snapshots != null && snapshots.size() > 0) {
                    throw new CloudRuntimeException("Cannot perform this action on a volume with one or more snapshots");
                }
            }
        }
    }

    /**
     * Tell the underlying storage plug-in to create a new volume, put it in the VAG of the destination cluster, and
     * send a command to the destination cluster to create an SR and to attach to the SR from all hosts in the cluster.
     */
    private String handleManagedVolumePreMigration(VolumeInfo volumeInfo, StoragePool storagePool, Host destHost) {
        final PrimaryDataStoreDriver pdsd = (PrimaryDataStoreDriver)volumeInfo.getDataStore().getDriver();

        VolumeDetailVO volumeDetailVo = new VolumeDetailVO(volumeInfo.getId(), PrimaryDataStoreDriver.BASIC_CREATE, Boolean.TRUE.toString(), false);

        volumeDetailsDao.persist(volumeDetailVo);

        pdsd.createAsync(volumeInfo.getDataStore(), volumeInfo, null);

        volumeDetailVo = new VolumeDetailVO(volumeInfo.getId(), PrimaryDataStoreDriver.BASIC_GRANT_ACCESS, Boolean.TRUE.toString(), false);

        volumeDetailsDao.persist(volumeDetailVo);

        pdsd.grantAccess(volumeInfo, destHost, volumeInfo.getDataStore());

        final Map<String, String> details = new HashMap<>();

        final String iqn = getBasicIqn(volumeInfo.getId());

        details.put(CreateStoragePoolCommand.DATASTORE_NAME, iqn);

        details.put(CreateStoragePoolCommand.IQN, iqn);

        details.put(CreateStoragePoolCommand.STORAGE_HOST, storagePool.getHostAddress());

        details.put(CreateStoragePoolCommand.STORAGE_PORT, String.valueOf(storagePool.getPort()));

        final CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, storagePool);

        cmd.setDetails(details);
        cmd.setCreateDatastore(true);

        final Answer answer = agentMgr.easySend(destHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            String errMsg = "Error interacting with host (related to CreateStoragePoolCommand)" +
                    (StringUtils.isNotBlank(answer.getDetails()) ? ": " + answer.getDetails() : "");

            s_logger.error(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        return iqn;
    }

    private List<SnapshotVO> getNonDestroyedSnapshots(long csVolumeId) {
        List<SnapshotVO> lstSnapshots = snapshotDao.listByVolumeId(csVolumeId);

        if (lstSnapshots == null) {
            lstSnapshots = new ArrayList<>();
        }

        List<SnapshotVO> lstSnapshots2 = new ArrayList<>();

        for (SnapshotVO snapshot : lstSnapshots) {
            if (!Snapshot.State.Destroyed.equals(snapshot.getState())) {
                lstSnapshots2.add(snapshot);
            }
        }

        return lstSnapshots2;
    }

    private void handleManagedVolumePostMigration(VolumeInfo volumeInfo, Host srcHost, VolumeObjectTO volumeTO) {
        final Map<String, String> details = new HashMap<>();

        details.put(DeleteStoragePoolCommand.DATASTORE_NAME, volumeInfo.get_iScsiName());

        final DeleteStoragePoolCommand cmd = new DeleteStoragePoolCommand();

        cmd.setDetails(details);
        cmd.setRemoveDatastore(true);

        final Answer answer = agentMgr.easySend(srcHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            String errMsg = "Error interacting with host (related to DeleteStoragePoolCommand)" +
                    (StringUtils.isNotBlank(answer.getDetails()) ? ": " + answer.getDetails() : "");

            s_logger.error(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        final PrimaryDataStoreDriver pdsd = (PrimaryDataStoreDriver)volumeInfo.getDataStore().getDriver();

        pdsd.revokeAccess(volumeInfo, srcHost, volumeInfo.getDataStore());

        VolumeDetailVO volumeDetailVo = new VolumeDetailVO(volumeInfo.getId(), PrimaryDataStoreDriver.BASIC_DELETE, Boolean.TRUE.toString(), false);

        volumeDetailsDao.persist(volumeDetailVo);

        pdsd.deleteAsync(volumeInfo.getDataStore(), volumeInfo, null);

        VolumeVO volumeVO = volDao.findById(volumeInfo.getId());

        volumeVO.setPath(volumeTO.getPath());

        volDao.update(volumeVO.getId(), volumeVO);
    }

    private void handleManagedVolumesAfterFailedMigration(Map<VolumeInfo, DataStore> volumeToPool, Host destHost) {
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
            VolumeInfo volumeInfo = entry.getKey();
            StoragePool storagePool = storagePoolDao.findById(volumeInfo.getPoolId());

            if (storagePool.isManaged()) {
                final Map<String, String> details = new HashMap<>();

                details.put(DeleteStoragePoolCommand.DATASTORE_NAME, getBasicIqn(volumeInfo.getId()));

                final DeleteStoragePoolCommand cmd = new DeleteStoragePoolCommand();

                cmd.setDetails(details);
                cmd.setRemoveDatastore(true);

                final Answer answer = agentMgr.easySend(destHost.getId(), cmd);

                if (answer == null || !answer.getResult()) {
                    String errMsg = "Error interacting with host (related to handleManagedVolumesAfterFailedMigration)" +
                            (StringUtils.isNotBlank(answer.getDetails()) ? ": " + answer.getDetails() : "");

                    s_logger.error(errMsg);

                    // no need to throw an exception here as the calling code is responsible for doing so
                    // regardless of the success or lack thereof concerning this method
                    return;
                }

                final PrimaryDataStoreDriver pdsd = (PrimaryDataStoreDriver)volumeInfo.getDataStore().getDriver();

                VolumeDetailVO volumeDetailVo = new VolumeDetailVO(volumeInfo.getId(), PrimaryDataStoreDriver.BASIC_REVOKE_ACCESS, Boolean.TRUE.toString(), false);

                volumeDetailsDao.persist(volumeDetailVo);

                pdsd.revokeAccess(volumeInfo, destHost, volumeInfo.getDataStore());

                volumeDetailVo = new VolumeDetailVO(volumeInfo.getId(), PrimaryDataStoreDriver.BASIC_DELETE_FAILURE, Boolean.TRUE.toString(), false);

                volumeDetailsDao.persist(volumeDetailVo);

                pdsd.deleteAsync(volumeInfo.getDataStore(), volumeInfo, null);
            }
        }
    }

    private Answer migrateVmWithVolumesAcrossCluster(VMInstanceVO vm, VirtualMachineTO to, Host srcHost, Host destHost, Map<VolumeInfo, DataStore> volumeToPool)
            throws AgentUnavailableException {
        // Initiate migration of a virtual machine with its volumes.

        try {
            verifyNoSnapshotsOnManagedStorageVolumes(volumeToPool);

            List<Pair<VolumeTO, String>> volumeToStorageUuid = new ArrayList<>();

            for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
                VolumeInfo volumeInfo = entry.getKey();
                StoragePool storagePool = storagePoolDao.findById(volumeInfo.getPoolId());
                VolumeTO volumeTo = new VolumeTO(volumeInfo, storagePool);

                if (storagePool.isManaged()) {
                    String iqn = handleManagedVolumePreMigration(volumeInfo, storagePool, destHost);

                    volumeToStorageUuid.add(new Pair<>(volumeTo, iqn));
                }
                else {
                    StoragePool pool = (StoragePool)entry.getValue();
                    String srNameLabel = CitrixHelper.getSRNameLabel(pool.getUuid(), pool.getPoolType(), pool.getPath());
                    volumeToStorageUuid.add(new Pair<>(volumeTo, srNameLabel));
                }
            }

            // Migration across cluster needs to be done in three phases.
            // 1. Send a migrate receive command to the destination host so that it is ready to receive a vm.
            // 2. Send a migrate send command to the source host. This actually migrates the vm to the destination.
            // 3. Complete the process. Update the volume details.

            MigrateWithStorageReceiveCommand receiveCmd = new MigrateWithStorageReceiveCommand(to, volumeToStorageUuid);
            MigrateWithStorageReceiveAnswer receiveAnswer = (MigrateWithStorageReceiveAnswer)agentMgr.send(destHost.getId(), receiveCmd);

            if (receiveAnswer == null) {
                s_logger.error("Migration with storage of vm " + vm + " to host " + destHost + " failed.");
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            } else if (!receiveAnswer.getResult()) {
                s_logger.error("Migration with storage of vm " + vm + " failed. Details: " + receiveAnswer.getDetails());
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            }

            MigrateWithStorageSendCommand sendCmd =
                    new MigrateWithStorageSendCommand(to, receiveAnswer.getVolumeToSr(), receiveAnswer.getNicToNetwork(), receiveAnswer.getToken());
            MigrateWithStorageSendAnswer sendAnswer = (MigrateWithStorageSendAnswer)agentMgr.send(srcHost.getId(), sendCmd);

            if (sendAnswer == null) {
                handleManagedVolumesAfterFailedMigration(volumeToPool, destHost);

                s_logger.error("Migration with storage of vm " + vm + " to host " + destHost + " failed.");
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            } else if (!sendAnswer.getResult()) {
                handleManagedVolumesAfterFailedMigration(volumeToPool, destHost);

                s_logger.error("Migration with storage of vm " + vm + " failed. Details: " + sendAnswer.getDetails());
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            }

            MigrateWithStorageCompleteCommand command = new MigrateWithStorageCompleteCommand(to);
            MigrateWithStorageCompleteAnswer answer = (MigrateWithStorageCompleteAnswer)agentMgr.send(destHost.getId(), command);

            if (answer == null) {
                s_logger.error("Migration with storage of vm " + vm + " failed.");
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            } else if (!answer.getResult()) {
                s_logger.error("Migration with storage of vm " + vm + " failed. Details: " + answer.getDetails());
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            } else {
                // Update the volume details after migration.
                updateVolumePathsAfterMigration(volumeToPool, answer.getVolumeTos(), srcHost);
            }

            return answer;
        } catch (OperationTimedoutException e) {
            s_logger.error("Error while migrating vm " + vm + " to host " + destHost, e);
            throw new AgentUnavailableException("Operation timed out on storage motion for " + vm, destHost.getId());
        }
    }

    private Answer migrateVmWithVolumesWithinCluster(VMInstanceVO vm, VirtualMachineTO to, Host srcHost, Host destHost, Map<VolumeInfo, DataStore> volumeToPool)
            throws AgentUnavailableException {

        // Initiate migration of a virtual machine with its volumes.
        try {
            List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerto = new ArrayList<Pair<VolumeTO, StorageFilerTO>>();
            for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
                VolumeInfo volume = entry.getKey();
                VolumeTO volumeTo = new VolumeTO(volume, storagePoolDao.findById(volume.getPoolId()));
                StorageFilerTO filerTo = new StorageFilerTO((StoragePool)entry.getValue());
                volumeToFilerto.add(new Pair<VolumeTO, StorageFilerTO>(volumeTo, filerTo));
            }

            MigrateWithStorageCommand command = new MigrateWithStorageCommand(to, volumeToFilerto);
            MigrateWithStorageAnswer answer = (MigrateWithStorageAnswer)agentMgr.send(destHost.getId(), command);
            if (answer == null) {
                s_logger.error("Migration with storage of vm " + vm + " failed.");
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost);
            } else if (!answer.getResult()) {
                s_logger.error("Migration with storage of vm " + vm + " failed. Details: " + answer.getDetails());
                throw new CloudRuntimeException("Error while migrating the vm " + vm + " to host " + destHost + ". " + answer.getDetails());
            } else {
                // Update the volume details after migration.
                updateVolumePathsAfterMigration(volumeToPool, answer.getVolumeTos(), srcHost);
            }

            return answer;
        } catch (OperationTimedoutException e) {
            s_logger.error("Error while migrating vm " + vm + " to host " + destHost, e);
            throw new AgentUnavailableException("Operation timed out on storage motion for " + vm, destHost.getId());
        }
    }

    private void updateVolumePathsAfterMigration(Map<VolumeInfo, DataStore> volumeToPool, List<VolumeObjectTO> volumeTos, Host srcHost) {
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
            VolumeInfo volumeInfo = entry.getKey();
            StoragePool storagePool = (StoragePool)entry.getValue();

            boolean updated = false;

            for (VolumeObjectTO volumeTo : volumeTos) {
                if (volumeInfo.getId() == volumeTo.getId()) {
                    if (storagePool.isManaged()) {
                        handleManagedVolumePostMigration(volumeInfo, srcHost, volumeTo);
                    }
                    else {
                        VolumeVO volumeVO = volDao.findById(volumeInfo.getId());
                        Long oldPoolId = volumeVO.getPoolId();

                        volumeVO.setPath(volumeTo.getPath());
                        volumeVO.setFolder(storagePool.getPath());
                        volumeVO.setPodId(storagePool.getPodId());
                        volumeVO.setPoolId(storagePool.getId());
                        volumeVO.setLastPoolId(oldPoolId);

                        volDao.update(volumeInfo.getId(), volumeVO);
                    }

                    updated = true;

                    break;
                }
            }

            if (!updated) {
                s_logger.error("The volume path wasn't updated for volume '" + volumeInfo + "' after it was migrated.");
            }
        }
    }
}
