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

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.alert.AlertManager;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class ScaleIOHostListener implements HypervisorHostListener {
    private static final Logger s_logger = Logger.getLogger(ScaleIOHostListener.class);

    @Inject private AgentManager _agentMgr;
    @Inject private AlertManager _alertMgr;
    @Inject private DataStoreManager _dataStoreMgr;
    @Inject private HostDao _hostDao;
    @Inject private StoragePoolHostDao _storagePoolHostDao;
    @Inject private PrimaryDataStoreDao _primaryDataStoreDao;
    @Inject private StoragePoolDetailsDao _storagePoolDetailsDao;

    @Override
    public boolean hostAdded(long hostId) {
        return true;
    }

    @Override
    public boolean hostConnect(long hostId, long poolId) {
        HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            s_logger.error("Failed to add host by HostListener as host was not found with id : " + hostId);
            return false;
        }

        if (!isHostSdcConnected(host.getPrivateIpAddress(), poolId)) {
            s_logger.warn("SDC not connected on the host: " + hostId);
            String msg = "SDC not connected on the host: " + hostId + ", reconnect the SDC to MDM and restart agent";
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "SDC disconnected on host: " + host.getUuid(), msg);
            return false;
        }

        StoragePoolHostVO storagePoolHost = _storagePoolHostDao.findByPoolHost(poolId, hostId);
        if (storagePoolHost == null) {
            storagePoolHost = new StoragePoolHostVO(poolId, hostId, "");
            _storagePoolHostDao.persist(storagePoolHost);
        }

        StoragePool storagePool = (StoragePool)_dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);
        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, storagePool);
        sendModifyStoragePoolCommand(cmd, storagePool, hostId);
        return true;
    }

    private boolean isHostSdcConnected(String hostIpAddress, long poolId) {
        try {
            ScaleIOGatewayClient client = ScaleIOGatewayClientConnectionPool.getInstance().getClient(poolId, _storagePoolDetailsDao);
            return client.isSdcConnected(hostIpAddress);
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            s_logger.error("Failed to check host sdc connection", e);
            throw new CloudRuntimeException("Failed to establish connection with PowerFlex Gateway to check host sdc connection");
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

        s_logger.info("Connection established between storage pool " + storagePool + " and host: " + hostId);
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        StoragePoolHostVO storagePoolHost = _storagePoolHostDao.findByPoolHost(poolId, hostId);
        if (storagePoolHost != null) {
            _storagePoolHostDao.deleteStoragePoolHostDetails(hostId, poolId);
        }

        return true;
    }

    @Override
    public boolean hostAboutToBeRemoved(long hostId) {
        return true;
    }

    @Override
    public boolean hostRemoved(long hostId, long clusterId) {
        return true;
    }
}
