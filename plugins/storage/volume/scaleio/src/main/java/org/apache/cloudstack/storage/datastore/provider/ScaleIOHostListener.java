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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.manager.ScaleIOSDCManager;
import org.apache.cloudstack.storage.datastore.manager.ScaleIOSDCManagerImpl;
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
import com.cloud.utils.component.ComponentContext;
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
    private ScaleIOSDCManager _sdcManager = new ScaleIOSDCManagerImpl();

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

        DataStore dataStore = _dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);
        StoragePool storagePool = (StoragePool) dataStore;
        StoragePoolHostVO storagePoolHost = _storagePoolHostDao.findByPoolHost(poolId, hostId);
        String sdcId = getSdcIdOfHost(host, dataStore);
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
            logger.info("Connection established between storage pool: {} and host: {}", storagePool, host);
        }
        return true;
    }

    private String getSdcIdOfHost(HostVO host, DataStore dataStore) {
        StoragePool storagePool = (StoragePool) dataStore;
        long hostId = host.getId();
        long poolId = storagePool.getId();
        String systemId = null;
        StoragePoolDetailVO systemIdDetail = _storagePoolDetailsDao.findDetail(poolId, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        if (systemIdDetail != null) {
            systemId = systemIdDetail.getValue();
        }
        if (systemId == null) {
            throw new CloudRuntimeException("Failed to get the system id for PowerFlex storage pool " + storagePool.getName());
        }
        Map<String,String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);
        _sdcManager = ComponentContext.inject(_sdcManager);
        if (_sdcManager.areSDCConnectionsWithinLimit(poolId)) {
            details.put(ScaleIOSDCManager.ConnectOnDemand.key(), String.valueOf(ScaleIOSDCManager.ConnectOnDemand.valueIn(host.getDataCenterId())));
            String mdms = _sdcManager.getMdms(poolId);
            details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, mdms);
        }

        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, storagePool, storagePool.getPath(), details);
        ModifyStoragePoolAnswer answer  = sendModifyStoragePoolCommand(cmd, storagePool, host);
        Map<String,String> poolDetails = answer.getPoolInfo().getDetails();
        if (MapUtils.isEmpty(poolDetails)) {
            String msg = String.format("PowerFlex storage SDC details not found on the host: %s, (re)install SDC and restart agent", host);
            logger.warn(msg);
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "SDC details not found on host: " + host.getUuid(), msg);
            return null;
        }

        String sdcId = null;
        if (poolDetails.containsKey(ScaleIOGatewayClient.SDC_ID)) {
            sdcId = poolDetails.get(ScaleIOGatewayClient.SDC_ID);
        } else if (poolDetails.containsKey(ScaleIOGatewayClient.SDC_GUID)) {
            String sdcGuid = poolDetails.get(ScaleIOGatewayClient.SDC_GUID);
            sdcId = _sdcManager.getHostSdcId(sdcGuid, dataStore);
        }

        if (StringUtils.isBlank(sdcId)) {
            String msg = String.format("Couldn't retrieve PowerFlex storage SDC details from the host: %s, (re)install SDC and restart agent", host);
            logger.warn(msg);
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "SDC details not found on host: " + host.getUuid(), msg);
            return null;
        }

        if (details.containsKey(ScaleIOSDCManager.ConnectOnDemand.key())) {
            String connectOnDemand = details.get(ScaleIOSDCManager.ConnectOnDemand.key());
            if (connectOnDemand != null && !Boolean.parseBoolean(connectOnDemand) && !_sdcManager.isHostSdcConnected(sdcId, dataStore, 15)) {
                logger.warn("SDC not connected on the host: " + hostId);
                String msg = "SDC not connected on the host: " + hostId + ", reconnect the SDC to MDM and restart agent";
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "SDC not connected on host: " + host.getUuid(), msg);
                return null;
            }
        }

        return sdcId;
    }

    private ModifyStoragePoolAnswer sendModifyStoragePoolCommand(ModifyStoragePoolCommand cmd, StoragePool storagePool, HostVO host) {
        Answer answer = _agentMgr.easySend(host.getId(), cmd);

        if (answer == null) {
            throw new CloudRuntimeException(String.format("Unable to get an answer to the modify storage pool command (add: %s) for PowerFlex storage pool %s, sent to host %s",
                    cmd.getAdd(), getStoragePoolDetails(storagePool), host));
        }

        if (!answer.getResult()) {
            if (cmd.getAdd()) {
                String msg = "Unable to attach PowerFlex storage pool " + getStoragePoolDetails(storagePool) + " to the host " + host;

                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, storagePool.getDataCenterId(), storagePool.getPodId(), msg, msg);

                throw new CloudRuntimeException("Unable to connect to PowerFlex storage pool " + getStoragePoolDetails(storagePool) + " due to " + answer.getDetails());
            } else {
                String msg = "Unable to detach PowerFlex storage pool " + getStoragePoolDetails(storagePool) + " from the host " + host;

                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, storagePool.getDataCenterId(), storagePool.getPodId(), msg, msg);
            }
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "ModifyStoragePoolAnswer expected ; PowerFlex Storage Pool = " + storagePool + " Host = " + host;

        return (ModifyStoragePoolAnswer) answer;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            logger.error("Failed to disconnect host by HostListener as host was not found with id : " + hostId);
            return false;
        }

        DataStore dataStore = _dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);
        StoragePool storagePool = (StoragePool) dataStore;
        String systemId = null;
        StoragePoolDetailVO systemIdDetail = _storagePoolDetailsDao.findDetail(poolId, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        if (systemIdDetail != null) {
            systemId = systemIdDetail.getValue();
        }
        if (systemId == null) {
            throw new CloudRuntimeException("Failed to get the system id for PowerFlex storage pool " + storagePool.getName());
        }
        Map<String,String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);
        _sdcManager = ComponentContext.inject(_sdcManager);
        if (_sdcManager.canUnprepareSDC(host, dataStore)) {
            details.put(ScaleIOSDCManager.ConnectOnDemand.key(), String.valueOf(ScaleIOSDCManager.ConnectOnDemand.valueIn(host.getDataCenterId())));
            String mdms = _sdcManager.getMdms(poolId);
            details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, mdms);
        }

        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(false, storagePool, storagePool.getPath(), details);
        ModifyStoragePoolAnswer answer  = sendModifyStoragePoolCommand(cmd, storagePool, host);
        if (!answer.getResult()) {
            logger.error("Failed to disconnect storage pool: " + storagePool + " and host: " + hostId);
            return false;
        }

        StoragePoolHostVO storagePoolHost = _storagePoolHostDao.findByPoolHost(poolId, hostId);
        if (storagePoolHost != null) {
            _storagePoolHostDao.deleteStoragePoolHostDetails(hostId, poolId);
        }
        logger.info("Connection removed between storage pool: " + storagePool + " and host: " + hostId);
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

    private String getStoragePoolDetails(StoragePool storagePool) {
        String poolDetails = "";
        if (storagePool != null) {
            poolDetails = String.format("%s (id: %d, uuid: %s)", storagePool.getName(), storagePool.getId(), storagePool.getUuid());
        }
        return poolDetails;
    }
}
