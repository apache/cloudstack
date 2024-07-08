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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
    protected Logger logger = LogManager.getLogger(getClass());

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
            logger.error("Failed to connect host by HostListener as host was not found with id : " + hostId);
            return false;
        }

        StoragePool storagePool = (StoragePool)_dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);
        StoragePoolHostVO storagePoolHost = _storagePoolHostDao.findByPoolHost(poolId, hostId);
        String sdcId = getSdcIdOfHost(host, storagePool);
        if (StringUtils.isBlank(sdcId)) {
            if (storagePoolHost != null) {
                _storagePoolHostDao.deleteStoragePoolHostDetails(hostId, poolId);
            }
        } else {
            if (storagePoolHost == null) {
                storagePoolHost = new StoragePoolHostVO(poolId, hostId, sdcId);
                _storagePoolHostDao.persist(storagePoolHost);
            } else {
                storagePoolHost.setLocalPath(sdcId);
                _storagePoolHostDao.update(storagePoolHost.getId(), storagePoolHost);
            }
            logger.info("Connection established between storage pool: " + storagePool + " and host: " + hostId);
        }
        return true;
    }

    private String getSdcIdOfHost(HostVO host, StoragePool storagePool) {
        long hostId = host.getId();
        long poolId = storagePool.getId();
        String systemId = _storagePoolDetailsDao.findDetail(poolId, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID).getValue();
        if (systemId == null) {
            throw new CloudRuntimeException("Failed to get the system id for PowerFlex storage pool " + storagePool.getName());
        }
        Map<String,String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);

        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, storagePool, storagePool.getPath(), details);
        ModifyStoragePoolAnswer answer  = sendModifyStoragePoolCommand(cmd, storagePool, hostId);
        Map<String,String> poolDetails = answer.getPoolInfo().getDetails();
        if (MapUtils.isEmpty(poolDetails)) {
            String msg = "PowerFlex storage SDC details not found on the host: " + hostId + ", (re)install SDC and restart agent";
            logger.warn(msg);
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "SDC not found on host: " + host.getUuid(), msg);
            return null;
        }

        String sdcId = null;
        if (poolDetails.containsKey(ScaleIOGatewayClient.SDC_ID)) {
            sdcId = poolDetails.get(ScaleIOGatewayClient.SDC_ID);
        } else if (poolDetails.containsKey(ScaleIOGatewayClient.SDC_GUID)) {
            String sdcGuid = poolDetails.get(ScaleIOGatewayClient.SDC_GUID);
            sdcId = getHostSdcId(sdcGuid, poolId);
        }

        if (StringUtils.isBlank(sdcId)) {
            String msg = "Couldn't retrieve PowerFlex storage SDC details from the host: " + hostId + ", (re)install SDC and restart agent";
            logger.warn(msg);
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "SDC details not found on host: " + host.getUuid(), msg);
            return null;
        }

        return sdcId;
    }

    private String getHostSdcId(String sdcGuid, long poolId) {
        try {
            logger.debug(String.format("Try to get host SDC Id for pool: %s, with SDC guid %s", poolId, sdcGuid));
            ScaleIOGatewayClient client = ScaleIOGatewayClientConnectionPool.getInstance().getClient(poolId, _storagePoolDetailsDao);
            return client.getSdcIdByGuid(sdcGuid);
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            logger.error(String.format("Failed to get host SDC Id for pool: %s", poolId), e);
            throw new CloudRuntimeException(String.format("Failed to establish connection with PowerFlex Gateway to get host SDC Id for pool: %s", poolId));
        }
    }

    private ModifyStoragePoolAnswer sendModifyStoragePoolCommand(ModifyStoragePoolCommand cmd, StoragePool storagePool, long hostId) {
        Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command (" + storagePool.getId() + ")");
        }

        if (!answer.getResult()) {
            String msg = "Unable to attach  PowerFlex storage pool " + storagePool.getId() + " to host " + hostId;

            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, storagePool.getDataCenterId(), storagePool.getPodId(), msg, msg);

            throw new CloudRuntimeException("Unable to establish a connection from agent to  PowerFlex storage pool " + storagePool.getId() + " due to " + answer.getDetails() +
                    " (" + storagePool.getId() + ")");
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "ModifyStoragePoolAnswer expected ; PowerFlex Storage Pool = " + storagePool.getId() + " Host = " + hostId;

        return (ModifyStoragePoolAnswer) answer;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        // SDC ID is getting updated upon host connect, no need to delete the storage_pool_host_ref entry
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

    @Override
    public boolean hostEnabled(long hostId) {
        return true;
    }
}
