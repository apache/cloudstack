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

package org.apache.cloudstack.storage.datastore.manager;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PrepareStorageClientAnswer;
import com.cloud.agent.api.PrepareStorageClientCommand;
import com.cloud.agent.api.UnprepareStorageClientCommand;
import com.cloud.configuration.Config;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class ScaleIOSDCManagerImpl implements ScaleIOSDCManager {
    private static final Logger LOGGER = Logger.getLogger(ScaleIOSDCManagerImpl.class);

    @Inject
    AgentManager agentManager;
    @Inject
    StoragePoolHostDao storagePoolHostDao;
    @Inject
    StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    ConfigurationDao configDao;

    public ScaleIOSDCManagerImpl() {

    }

    private boolean areSDCConnectionsWithinLimit(Long storagePoolId) {
        int connectedClientsLimit = StorageManager.STORAGE_POOL_CONNECTED_CLIENTS_LIMIT.valueIn(storagePoolId);
        if (connectedClientsLimit <= 0) {
            return true;
        }

        try {
            int connectedSdcsCount = getScaleIOClient(storagePoolId).getConnectedSdcsCount();
            if (connectedSdcsCount < connectedClientsLimit) {
                return true;
            }
        } catch (Exception e) {
            String errMsg = "Unable to check SDC connections for the storage pool with id: " + storagePoolId + " due to " + e.getMessage();
            LOGGER.warn(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }

        return false;
    }

    @Override
    public String prepareSDC(Host host, DataStore dataStore) {
        String systemId = storagePoolDetailsDao.findDetail(dataStore.getId(), ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID).getValue();
        if (systemId == null) {
            throw new CloudRuntimeException("Unable to prepare SDC, failed to get the system id for PowerFlex storage pool: " + dataStore.getName());
        }

        GlobalLock lock = null;
        try {
            String hostIdStorageSystemIdLockString = "HostId:" + host.getId() + "-SystemId:" + systemId;
            lock = GlobalLock.getInternLock(hostIdStorageSystemIdLockString);
            if (lock == null) {
                throw new CloudRuntimeException("Unable to prepare SDC, couldn't get global lock on " + hostIdStorageSystemIdLockString);
            }

            int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
            if (!lock.lock(storagePoolMaxWaitSeconds)) {
                LOGGER.debug("Unable to prepare SDC, couldn't lock on " + hostIdStorageSystemIdLockString);
                throw new CloudRuntimeException("Unable to prepare SDC, couldn't lock on " + hostIdStorageSystemIdLockString);
            }

            // Check connected
            long poolId = dataStore.getId();
            long hostId = host.getId();
            String sdcId = getConnectedSdc(poolId, hostId);
            if (StringUtils.isNotBlank(sdcId)) {
                return sdcId;
            }

            if (!areSDCConnectionsWithinLimit(poolId)) {
                throw new CloudRuntimeException("SDC connections limit reached");
            }

            // Send PrepareSDCCommand & Check Answer
            sdcId = prepareSDCOnHost(host, dataStore, systemId);
            StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(poolId, hostId);

            if (StringUtils.isBlank(sdcId)) {
                if (storagePoolHost != null) {
                    storagePoolHostDao.deleteStoragePoolHostDetails(hostId, poolId);
                }
            } else {
                if (storagePoolHost == null) {
                    storagePoolHost = new StoragePoolHostVO(poolId, hostId, sdcId);
                    storagePoolHostDao.persist(storagePoolHost);
                } else {
                    storagePoolHost.setLocalPath(sdcId);
                    storagePoolHostDao.update(storagePoolHost.getId(), storagePoolHost);
                }
            }

            if (isHostSdcConnected(sdcId, poolId)) {
                return sdcId;
            }
            return null;
        } finally {
            if (lock != null) {
                lock.unlock();
                lock.releaseRef();
            }
        }
    }

    private String prepareSDCOnHost(Host host, DataStore dataStore, String systemId) {
        LOGGER.debug(String.format("Preparing SDC on the host %s (%s)", host.getId(), host.getName()));
        Map<String,String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);
        PrepareStorageClientCommand cmd = new PrepareStorageClientCommand(details);
        int timeoutSeconds = 60;
        cmd.setWait(timeoutSeconds);

        PrepareStorageClientAnswer prepareStorageClientAnswer;
        try {
            prepareStorageClientAnswer = (PrepareStorageClientAnswer) agentManager.send(host.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String err = String.format("Failed to prepare SDC on the host %s, due to: %s", host.getName(), e.getMessage());
            LOGGER.error(err);
            throw new CloudRuntimeException(err);
        }

        if (prepareStorageClientAnswer == null) {
            String err = String.format("Unable to prepare SDC on the host %s", host.getName());
            LOGGER.error(err);
            throw new CloudRuntimeException(err);
        }

        if (!prepareStorageClientAnswer.getResult()) {
            String err = String.format("Unable to prepare SDC on the host %s, due to: %s", host.getName(), prepareStorageClientAnswer.getDetails());
            LOGGER.error(err);
            throw new CloudRuntimeException(err);
        }

        Map<String,String> poolDetails = prepareStorageClientAnswer.getDetailsMap();
        if (MapUtils.isEmpty(poolDetails)) {
            LOGGER.warn(String.format("PowerFlex storage SDC details not found on the host: %s, try (re)install SDC and restart agent", host.getId()));
            return null;
        }

        String sdcId = null;
        if (poolDetails.containsKey(ScaleIOGatewayClient.SDC_ID)) {
            sdcId = poolDetails.get(ScaleIOGatewayClient.SDC_ID);
        } else if (poolDetails.containsKey(ScaleIOGatewayClient.SDC_GUID)) {
            String sdcGuid = poolDetails.get(ScaleIOGatewayClient.SDC_GUID);
            sdcId = getHostSdcId(sdcGuid, dataStore.getId());
        }

        if (StringUtils.isBlank(sdcId)) {
            LOGGER.warn(String.format("Couldn't retrieve PowerFlex storage SDC details from the host: %s, try (re)install SDC and restart agent", host.getId()));
            return null;
        }

        return sdcId;
    }

    @Override
    public boolean stopSDC(Host host, DataStore dataStore) {
        String systemId = storagePoolDetailsDao.findDetail(dataStore.getId(), ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID).getValue();
        if (systemId == null) {
            throw new CloudRuntimeException("Unable to unprepare SDC, failed to get the system id for PowerFlex storage pool: " + dataStore.getName());
        }

        GlobalLock lock = null;
        try {
            String hostIdStorageSystemIdLockString = "HostId:" + host.getId() + "-SystemId:" + systemId;
            lock = GlobalLock.getInternLock(hostIdStorageSystemIdLockString);
            if (lock == null) {
                throw new CloudRuntimeException("Unable to unprepare SDC, couldn't get global lock on " + hostIdStorageSystemIdLockString);
            }

            int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
            if (!lock.lock(storagePoolMaxWaitSeconds)) {
                LOGGER.debug("Unable to unprepare SDC, couldn't lock on " + hostIdStorageSystemIdLockString);
                throw new CloudRuntimeException("Unable to unprepare SDC, couldn't lock on " + hostIdStorageSystemIdLockString);
            }

            // Check not connected
            long poolId = dataStore.getId();
            long hostId = host.getId();
            String sdcId = getConnectedSdc(poolId, hostId);
            if (StringUtils.isBlank(sdcId)) {
                LOGGER.debug("SDC not connected, no need to unprepare it");
                return true;
            }

            // Send StopSDCCommand & Check Answer
            return unprepareSDCOnHost(host);
        } finally {
            if (lock != null) {
                lock.unlock();
                lock.releaseRef();
            }
        }
    }

    private boolean unprepareSDCOnHost(Host host) {
        LOGGER.debug(String.format("Unpreparing SDC on the host %s (%s)", host.getId(), host.getName()));
        UnprepareStorageClientCommand cmd = new UnprepareStorageClientCommand();
        int timeoutSeconds = 60;
        cmd.setWait(timeoutSeconds);

        Answer unprepareStorageClientAnswer;
        try {
            unprepareStorageClientAnswer = agentManager.send(host.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String err = String.format("Failed to unprepare SDC on the host %s due to: %s", host.getName(), e.getMessage());
            LOGGER.error(err);
            return false;
        }

        if (!unprepareStorageClientAnswer.getResult()) {
            String err = String.format("Unable to unprepare SDC on the the host %s due to: %s", host.getName(), unprepareStorageClientAnswer.getDetails());
            LOGGER.error(err);
            return false;
        }
        return true;
    }

    private String getHostSdcId(String sdcGuid, long poolId) {
        try {
            LOGGER.debug(String.format("Try to get host SDC Id for pool: %s, with SDC guid %s", poolId, sdcGuid));
            ScaleIOGatewayClient client = getScaleIOClient(poolId);
            return client.getSdcIdByGuid(sdcGuid);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to get host SDC Id for pool: %s", poolId), e);
            throw new CloudRuntimeException(String.format("Failed to establish connection with PowerFlex Gateway to get host SDC Id for pool: %s", poolId));
        }
    }

    private String getConnectedSdc(long poolId, long hostId) {
        try {
            StoragePoolHostVO poolHostVO = storagePoolHostDao.findByPoolHost(poolId, hostId);
            if (poolHostVO == null) {
                return null;
            }

            final ScaleIOGatewayClient client = getScaleIOClient(poolId);
            if (client.isSdcConnected(poolHostVO.getLocalPath())) {
                return poolHostVO.getLocalPath();
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to get connected SDC for the host: " + hostId + " and storage pool: " + poolId + " due to " + e.getMessage(), e);
        }

        return null;
    }

    private boolean isHostSdcConnected(String sdcId, long poolId) {
        try {
            final ScaleIOGatewayClient client = getScaleIOClient(poolId);
            return client.isSdcConnected(sdcId);
        } catch (Exception e) {
            LOGGER.error("Failed to check host SDC connection", e);
            throw new CloudRuntimeException("Failed to establish connection with PowerFlex Gateway to check host SDC connection");
        }
    }

    private ScaleIOGatewayClient getScaleIOClient(final Long storagePoolId) throws Exception {
        return ScaleIOGatewayClientConnectionPool.getInstance().getClient(storagePoolId, storagePoolDetailsDao);
    }
}
