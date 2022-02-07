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

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.alert.AlertManager;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SolidFireSharedHostListener implements HypervisorHostListener {
    private static final Logger LOGGER = Logger.getLogger(SolidFireSharedHostListener.class);

    @Inject private AgentManager agentMgr;
    @Inject private AlertManager alertMgr;
    @Inject private ClusterDao clusterDao;
    @Inject private DataStoreManager dataStoreMgr;
    @Inject private HostDao hostDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private StoragePoolHostDao storagePoolHostDao;
    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;

    @Override
    public boolean hostAdded(long hostId) {
        HostVO host = hostDao.findById(hostId);

        if (host == null) {
            LOGGER.error(String.format("Failed to add host by SolidFireSharedHostListener as host was not found with id = %s ", hostId));

            return false;
        }

        if (host.getClusterId() == null) {
            LOGGER.error("Failed to add host by SolidFireSharedHostListener as host has no associated cluster id");
            return false;
        }

        SolidFireUtil.hostAddedToCluster(hostId, host.getClusterId(), SolidFireUtil.SHARED_PROVIDER_NAME,
                    clusterDao, hostDao, storagePoolDao, storagePoolDetailsDao);

        handleVMware(host, true, ModifyTargetsCommand.TargetTypeToRemove.NEITHER);

        return true;
    }

    @Override
    public boolean hostConnect(long hostId, long storagePoolId) {
        StoragePool storagePool = (StoragePool) dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);
        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, storagePool);

        ModifyStoragePoolAnswer answer = sendModifyStoragePoolCommand(cmd, storagePool, hostId);

        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(storagePoolId, hostId);

        if (storagePoolHost != null) {
            storagePoolHost.setLocalPath(answer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
        } else {
            storagePoolHost = new StoragePoolHostVO(storagePoolId, hostId, answer.getPoolInfo().getLocalPath().replaceAll("//", "/"));

            storagePoolHostDao.persist(storagePoolHost);
        }

        StoragePoolVO storagePoolVO = storagePoolDao.findById(storagePoolId);

        storagePoolVO.setCapacityBytes(answer.getPoolInfo().getCapacityBytes());
        storagePoolVO.setUsedBytes(answer.getPoolInfo().getCapacityBytes() - answer.getPoolInfo().getAvailableBytes());

        storagePoolDao.update(storagePoolId, storagePoolVO);

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
        HostVO host = hostDao.findById(hostId);

        SolidFireUtil.hostRemovedFromCluster(hostId, host.getClusterId(), SolidFireUtil.SHARED_PROVIDER_NAME,
                clusterDao, hostDao, storagePoolDao, storagePoolDetailsDao);

        handleVMware(host, false, ModifyTargetsCommand.TargetTypeToRemove.BOTH);

        return true;
    }

    @Override
    public boolean hostRemoved(long hostId, long clusterId) {
        return true;
    }

    private void handleVMware(HostVO host, boolean add, ModifyTargetsCommand.TargetTypeToRemove targetTypeToRemove) {
        if (HypervisorType.VMware.equals(host.getHypervisorType())) {
            List<StoragePoolVO> storagePools = storagePoolDao.findPoolsByProvider(SolidFireUtil.SHARED_PROVIDER_NAME);

            if (storagePools != null && storagePools.size() > 0) {
                List<Map<String, String>> targets = new ArrayList<>();

                for (StoragePoolVO storagePool : storagePools) {
                    if (storagePool.getClusterId().equals(host.getClusterId())) {
                        long storagePoolId = storagePool.getId();

                        StoragePoolDetailVO storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.IQN);

                        String iqn = storagePoolDetail.getValue();

                        storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.STORAGE_VIP);

                        String sVip = storagePoolDetail.getValue();

                        storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.STORAGE_PORT);

                        String sPort = storagePoolDetail.getValue();

                        Map<String, String> details = new HashMap<>();

                        details.put(ModifyTargetsCommand.IQN, iqn);
                        details.put(ModifyTargetsCommand.STORAGE_HOST, sVip);
                        details.put(ModifyTargetsCommand.STORAGE_PORT, sPort);

                        targets.add(details);
                    }
                }

                if (targets.size() > 0) {
                    ModifyTargetsCommand cmd = new ModifyTargetsCommand();

                    cmd.setTargets(targets);
                    cmd.setAdd(add);
                    cmd.setTargetTypeToRemove(targetTypeToRemove);
                    cmd.setRemoveAsync(true);

                    sendModifyTargetsCommand(cmd, host.getId());
                }
            }
        }
    }

    private void sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify targets command");
        }

        if (!answer.getResult()) {
            String msg = "Unable to modify targets on the following host: " + hostId;

            HostVO host = hostDao.findById(hostId);

            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), msg, msg);

            throw new CloudRuntimeException(msg);
        }
    }

    private ModifyStoragePoolAnswer sendModifyStoragePoolCommand(ModifyStoragePoolCommand cmd, StoragePool storagePool, long hostId) {
        Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command for storage pool: " + storagePool.getId());
        }

        if (!answer.getResult()) {
            String msg = "Unable to attach storage pool " + storagePool.getId() + " to the host " + hostId;

            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, storagePool.getDataCenterId(), storagePool.getPodId(), msg, msg);

            throw new CloudRuntimeException(msg);
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "ModifyStoragePoolAnswer not returned from ModifyStoragePoolCommand; Storage pool = " +
            storagePool.getId() + "; Host = " + hostId;

        LOGGER.info("Connection established between storage pool " + storagePool + " and host " + hostId);

        return (ModifyStoragePoolAnswer)answer;
    }
}
