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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateWithStorageAnswer;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

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
    @Inject
    private HostDao hostDao;

    @Override
    public StrategyPriority canHandle(DataObject srcData, DataObject destData) {
        // OfflineVmwareMigration: return StrategyPriority.HYPERVISOR when destData is in a storage pool in the same vmware-cluster and both are volumes
        if (isOnVmware(srcData, destData)
                && isOnPrimary(srcData, destData)
                && isVolumesOnly(srcData, destData)
                && isDettached(srcData)
                && isIntraCluster(srcData, destData)
                && isStoreScopeEqual(srcData, destData)) {
            if (s_logger.isDebugEnabled()) {
                String msg = String.format("%s can handle the request because %d(%s) and %d(%s) share the VMware cluster %s (== %s)"
                        , this.getClass()
                        , srcData.getId()
                        , srcData.getUuid()
                        , destData.getId()
                        , destData.getUuid()
                        , storagePoolDao.findById(srcData.getDataStore().getId()).getClusterId()
                        , storagePoolDao.findById(destData.getDataStore().getId()).getClusterId());
                s_logger.debug(msg);
            }
            return StrategyPriority.HYPERVISOR;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    private boolean isDettached(DataObject srcData) {
        VolumeVO volume = volDao.findById(srcData.getId());
        return volume.getInstanceId() == null;
    }

    private boolean isVolumesOnly(DataObject srcData, DataObject destData) {
        return DataObjectType.VOLUME.equals(srcData.getType())
                && DataObjectType.VOLUME.equals(destData.getType());
    }

    private boolean isOnPrimary(DataObject srcData, DataObject destData) {
        return DataStoreRole.Primary.equals(srcData.getDataStore().getRole())
                && DataStoreRole.Primary.equals(destData.getDataStore().getRole());
    }

    private boolean isOnVmware(DataObject srcData, DataObject destData) {
        return HypervisorType.VMware.equals(srcData.getTO().getHypervisorType())
                && HypervisorType.VMware.equals(destData.getTO().getHypervisorType());
    }

    private boolean isIntraCluster(DataObject srcData, DataObject destData) {
        DataStore srcStore = srcData.getDataStore();
        StoragePool srcPool = storagePoolDao.findById(srcStore.getId());
        DataStore destStore = destData.getDataStore();
        StoragePool destPool = storagePoolDao.findById(destStore.getId());
        return srcPool.getClusterId().equals(destPool.getClusterId());
    }

    /**
     * Ensure that the scope of source and destination storage pools match
     *
     * @param srcData
     * @param destData
     * @return
     */
    private boolean isStoreScopeEqual(DataObject srcData, DataObject destData) {
        DataStore srcStore = srcData.getDataStore();
        DataStore destStore = destData.getDataStore();
        String msg = String.format("Storage scope of source pool is %s and of destination pool is %s", srcStore.getScope().toString(), destStore.getScope().toString());
        s_logger.debug(msg);
        return srcStore.getScope().getScopeType() == (destStore.getScope().getScopeType());
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        if (srcHost.getHypervisorType() == HypervisorType.VMware && destHost.getHypervisorType() == HypervisorType.VMware) {
            s_logger.debug(this.getClass() + " can handle the request because the hosts have VMware hypervisor");
            return StrategyPriority.HYPERVISOR;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    /**
     * the Vmware storageMotion strategy allows to copy to a destination pool but not to a destination host
     *
     * @param srcData  volume to move
     * @param destData volume description as intended after the move
     * @param destHost null or else
     * @param callback where to report completion or failure to
     */
    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        if (destHost != null) {
            String format = "%s cannot target a host in moving an object from {%s}\n to {%s}";
            String msg = String.format(format
                    , this.getClass().getName()
                    , srcData.toString()
                    , destData.toString()
            );
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        // OfflineVmwareMigration: extract the destination pool from destData and construct a migrateVolume command
        if (!isOnPrimary(srcData, destData)) {
            // OfflineVmwareMigration: we shouldn't be here as we would have refused in the canHandle call
            throw new UnsupportedOperationException();
        }
        StoragePool sourcePool = (StoragePool) srcData.getDataStore();
        StoragePool targetPool = (StoragePool) destData.getDataStore();
        MigrateVolumeCommand cmd = new MigrateVolumeCommand(srcData.getId()
                , srcData.getTO().getPath()
                , sourcePool
                , targetPool);
        // OfflineVmwareMigration: should be ((StoragePool)srcData.getDataStore()).getHypervisor() but that is NULL, so hardcoding
        Answer answer;
        ScopeType scopeType = srcData.getDataStore().getScope().getScopeType();
        if (ScopeType.CLUSTER == scopeType) {
            // Find Volume source cluster and select any Vmware hypervisor host to attach worker VM
            Long hostId = findSuitableHostIdForWorkerVmPlacement(sourcePool.getClusterId());
            if (hostId == null) {
                throw new CloudRuntimeException("Offline Migration failed, unable to find suitable host for worker VM placement in cluster: " + sourcePool.getName());
            }
            answer = agentMgr.easySend(hostId, cmd);
        } else {
            answer = agentMgr.sendTo(sourcePool.getDataCenterId(), HypervisorType.VMware, cmd);
        }
        updateVolumeAfterMigration(answer, srcData, destData);
        CopyCommandResult result = new CopyCommandResult(null, answer);
        callback.complete(result);
    }

    /**
     * Selects a host from the cluster housing the source storage pool
     * Assumption is that Primary Storage is cluster-wide
     * <p>
     * returns any host ID within the cluster if storage-pool is cluster-wide, and exception is thrown otherwise
     *
     * @param clusterId
     * @return
     */
    private Long findSuitableHostIdForWorkerVmPlacement(Long clusterId) {
        List<HostVO> hostLists = hostDao.findByClusterId(clusterId);
        Long hostId = null;
        for (HostVO hostVO : hostLists) {
            if (hostVO.getHypervisorType().equals(HypervisorType.VMware) && hostVO.getStatus() == Status.Up) {
                hostId = hostVO.getId();
                break;
            }
        }
        return hostId;
    }

    private void updateVolumeAfterMigration(Answer answer, DataObject srcData, DataObject destData) {
        VolumeVO destinationVO = volDao.findById(destData.getId());
        if (!(answer instanceof MigrateVolumeAnswer)) {
            // OfflineVmwareMigration: reset states and such
            VolumeVO sourceVO = volDao.findById(srcData.getId());
            sourceVO.setState(Volume.State.Ready);
            volDao.update(sourceVO.getId(), sourceVO);
            destinationVO.setState(Volume.State.Expunged);
            destinationVO.setRemoved(new Date());
            volDao.update(destinationVO.getId(), destinationVO);
            throw new CloudRuntimeException("unexpected answer from hypervisor agent: " + answer.getDetails());
        }
        MigrateVolumeAnswer ans = (MigrateVolumeAnswer) answer;
        if (s_logger.isDebugEnabled()) {
            String format = "retrieved '%s' as new path for volume(%d)";
            s_logger.debug(String.format(format, ans.getVolumePath(), destData.getId()));
        }
        // OfflineVmwareMigration: update the volume with new pool/volume path
        destinationVO.setPath(ans.getVolumePath());
        volDao.update(destinationVO.getId(), destinationVO);
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

    private Answer migrateVmWithVolumesAcrossCluster(VMInstanceVO vm, VirtualMachineTO to, Host srcHost, Host destHost, Map<VolumeInfo, DataStore> volumeToPool)
            throws AgentUnavailableException {

        // Initiate migration of a virtual machine with it's volumes.
        try {
            List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerto = new ArrayList<Pair<VolumeTO, StorageFilerTO>>();
            for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
                VolumeInfo volume = entry.getKey();
                VolumeTO volumeTo = new VolumeTO(volume, storagePoolDao.findById(volume.getPoolId()));
                StorageFilerTO filerTo = new StorageFilerTO((StoragePool) entry.getValue());
                volumeToFilerto.add(new Pair<VolumeTO, StorageFilerTO>(volumeTo, filerTo));
            }

            // Migration across cluster needs to be done in three phases.
            // 1. Send a migrate command to source resource to initiate migration
            //      Run validations against target!!
            // 2. Complete the process. Update the volume details.
            MigrateWithStorageCommand migrateWithStorageCmd = new MigrateWithStorageCommand(to, volumeToFilerto, destHost.getGuid());
            MigrateWithStorageAnswer migrateWithStorageAnswer = (MigrateWithStorageAnswer) agentMgr.send(srcHost.getId(), migrateWithStorageCmd);
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
                StorageFilerTO filerTo = new StorageFilerTO((StoragePool) entry.getValue());
                volumeToFilerto.add(new Pair<VolumeTO, StorageFilerTO>(volumeTo, filerTo));
            }

            MigrateWithStorageCommand command = new MigrateWithStorageCommand(to, volumeToFilerto, destHost.getGuid());
            MigrateWithStorageAnswer answer = (MigrateWithStorageAnswer) agentMgr.send(srcHost.getId(), command);
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
            StoragePool pool = (StoragePool) entry.getValue();
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
