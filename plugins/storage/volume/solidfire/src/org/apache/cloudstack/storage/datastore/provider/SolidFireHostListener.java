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
package org.apache.cloudstack.storage.datastore.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.alert.AlertManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

public class SolidFireHostListener implements HypervisorHostListener {
    private static final Logger s_logger = Logger.getLogger(SolidFireHostListener.class);

    @Inject private AgentManager _agentMgr;
    @Inject private AlertManager _alertMgr;
    @Inject private ClusterDao _clusterDao;
    @Inject private ClusterDetailsDao _clusterDetailsDao;
    @Inject private DataStoreManager _dataStoreMgr;
    @Inject private HostDao _hostDao;
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject private StoragePoolHostDao storagePoolHostDao;
    @Inject private VMInstanceDao _vmDao;
    @Inject private VolumeDao _volumeDao;

    @Override
    public boolean hostAdded(long hostId) {
        HostVO host = _hostDao.findById(hostId);

        SolidFireUtil.hostAddedToOrRemovedFromCluster(hostId, host.getClusterId(), true, SolidFireUtil.PROVIDER_NAME,
                _clusterDao, _clusterDetailsDao, _storagePoolDao, _storagePoolDetailsDao, _hostDao);

        handleVMware(host, true);

        return true;
    }

    @Override
    public boolean hostConnect(long hostId, long storagePoolId) {
        HostVO host = _hostDao.findById(hostId);

        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(storagePoolId, hostId);

        if (storagePoolHost == null) {
            storagePoolHost = new StoragePoolHostVO(storagePoolId, hostId, "");

            storagePoolHostDao.persist(storagePoolHost);
        }

        if (host.getHypervisorType().equals(HypervisorType.XenServer)) {
            handleXenServer(host.getClusterId(), host.getId(), storagePoolId);
        }
        else if (host.getHypervisorType().equals(HypervisorType.KVM)) {
            handleKVM(hostId, storagePoolId);
        }

        return true;
    }

    @Override
    public boolean hostDisconnected(long hostId, long storagePoolId) {
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(storagePoolId, hostId);

        if (storagePoolHost != null) {
            storagePoolHostDao.deleteStoragePoolHostDetails(hostId, storagePoolId);
        }

        return true;
    }

    @Override
    public boolean hostAboutToBeRemoved(long hostId) {
        HostVO host = _hostDao.findById(hostId);

        handleVMware(host, false);

        return true;
    }

    @Override
    public boolean hostRemoved(long hostId, long clusterId) {
        SolidFireUtil.hostAddedToOrRemovedFromCluster(hostId, clusterId, false, SolidFireUtil.PROVIDER_NAME,
                _clusterDao, _clusterDetailsDao, _storagePoolDao, _storagePoolDetailsDao, _hostDao);

        return true;
    }

    private void handleXenServer(long clusterId, long hostId, long storagePoolId) {
        List<String> storagePaths = getStoragePaths(clusterId, storagePoolId);

        StoragePool storagePool = (StoragePool)_dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        for (String storagePath : storagePaths) {
            ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, storagePool);

            cmd.setStoragePath(storagePath);

            sendModifyStoragePoolCommand(cmd, storagePool, hostId);
        }
    }

    private void handleVMware(HostVO host, boolean add) {
        if (HypervisorType.VMware.equals(host.getHypervisorType())) {
            List<StoragePoolVO> storagePools = _storagePoolDao.findPoolsByProvider(SolidFireUtil.PROVIDER_NAME);

            if (storagePools != null && storagePools.size() > 0) {
                List<Map<String, String>> targets = new ArrayList<>();

                for (StoragePoolVO storagePool : storagePools) {
                    List<Map<String, String>> targetsForClusterAndStoragePool = getTargets(host.getClusterId(), storagePool.getId());

                    targets.addAll(targetsForClusterAndStoragePool);
                }

                ModifyTargetsCommand cmd = new ModifyTargetsCommand();

                cmd.setAdd(add);
                cmd.setTargets(targets);

                sendModifyTargetsCommand(cmd, host.getId());
            }
        }
    }

    private void handleKVM(long hostId, long storagePoolId) {
        StoragePool storagePool = (StoragePool)_dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, storagePool);

        sendModifyStoragePoolCommand(cmd, storagePool, hostId);
    }

    private List<String> getStoragePaths(long clusterId, long storagePoolId) {
        List<String> storagePaths = new ArrayList<>();

        // If you do not pass in null for the second parameter, you only get back applicable ROOT disks.
        List<VolumeVO> volumes = _volumeDao.findByPoolId(storagePoolId, null);

        if (volumes != null) {
            for (VolumeVO volume : volumes) {
                Long instanceId = volume.getInstanceId();

                if (instanceId != null) {
                    VMInstanceVO vmInstance = _vmDao.findById(instanceId);

                    Long hostIdForVm = vmInstance.getHostId() != null ? vmInstance.getHostId() : vmInstance.getLastHostId();

                    if (hostIdForVm != null) {
                        HostVO hostForVm = _hostDao.findById(hostIdForVm);

                        if (hostForVm != null && hostForVm.getClusterId().equals(clusterId)) {
                            storagePaths.add(volume.get_iScsiName());
                        }
                    }
                }
            }
        }

        return storagePaths;
    }

    private List<Map<String, String>> getTargets(long clusterId, long storagePoolId) {
        List<Map<String, String>> targets = new ArrayList<>();

        StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

        // If you do not pass in null for the second parameter, you only get back applicable ROOT disks.
        List<VolumeVO> volumes = _volumeDao.findByPoolId(storagePoolId, null);

        if (volumes != null) {
            for (VolumeVO volume : volumes) {
                Long instanceId = volume.getInstanceId();

                if (instanceId != null) {
                    VMInstanceVO vmInstance = _vmDao.findById(instanceId);

                    Long hostIdForVm = vmInstance.getHostId() != null ? vmInstance.getHostId() : vmInstance.getLastHostId();

                    if (hostIdForVm != null) {
                        HostVO hostForVm = _hostDao.findById(hostIdForVm);

                        if (hostForVm.getClusterId().equals(clusterId)) {
                            Map<String, String> details = new HashMap<>();

                            details.put(ModifyTargetsCommand.IQN, volume.get_iScsiName());
                            details.put(ModifyTargetsCommand.STORAGE_HOST, storagePool.getHostAddress());
                            details.put(ModifyTargetsCommand.STORAGE_PORT, String.valueOf(storagePool.getPort()));

                            targets.add(details);
                        }
                    }
                }
            }
        }

        return targets;
    }

    private void sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify targets command");
        }

        if (!answer.getResult()) {
            String msg = "Unable to modify targets on the following host: " + hostId;

            HostVO host = _hostDao.findById(hostId);

            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), msg, msg);

            throw new CloudRuntimeException(msg);
        }
    }

    private void sendModifyStoragePoolCommand(ModifyStoragePoolCommand cmd, StoragePool storagePool, long hostId) {
        Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command (" + storagePool.getId() + ")");
        }

        if (!answer.getResult()) {
            String msg = "Unable to attach storage pool " + storagePool.getId() + " to host " + hostId;

            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, storagePool.getDataCenterId(), storagePool.getPodId(), msg, msg);

            throw new CloudRuntimeException("Unable to establish a connection from agent to storage pool " + storagePool.getId() + " due to " + answer.getDetails() +
                " (" + storagePool.getId() + ")");
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "ModifyStoragePoolAnswer expected ; Pool = " + storagePool.getId() + " Host = " + hostId;

        s_logger.info("Connection established between storage pool " + storagePool + " and host + " + hostId);
    }
}
