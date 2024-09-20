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
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class ScaleIOSDCManagerImpl implements ScaleIOSDCManager, Configurable {
    private Logger logger = LogManager.getLogger(getClass());

    static ConfigKey<Boolean> ConnectOnDemand = new ConfigKey<>("Storage",
            Boolean.class,
            "powerflex.connect.on.demand",
            Boolean.FALSE.toString(),
            "Connect PowerFlex client on Host when first Volume is mapped to SDC and disconnect when last Volume is unmapped from SDC," +
                    " otherwise no action (that is connection remains in the same state whichever it is, connected or disconnected).",
            Boolean.TRUE,
            ConfigKey.Scope.Zone);

    @Inject
    AgentManager agentManager;
    @Inject
    StoragePoolHostDao storagePoolHostDao;
    @Inject
    StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    ConfigurationDao configDao;

    private static final String POWERFLEX_SDC_HOSTID_SYSTEMID_LOCK_FORMAT = "PowerFlexSDC-HostId:%s-SystemId:%s";
    private static final String POWERFLEX_SDC_SYSTEMID_LOCK_FORMAT = "PowerFlexSDC-SystemId:%s";

    public ScaleIOSDCManagerImpl() {

    }

    @Override
    public boolean areSDCConnectionsWithinLimit(Long storagePoolId) {
        try {
            int connectedClientsLimit = StorageManager.STORAGE_POOL_CONNECTED_CLIENTS_LIMIT.valueIn(storagePoolId);
            if (connectedClientsLimit <= 0) {
                return true;
            }

            int connectedSdcsCount = getScaleIOClient(storagePoolId).getConnectedSdcsCount();
            if (connectedSdcsCount < connectedClientsLimit) {
                logger.debug(String.format("Current connected SDCs count: %d - SDC connections are within the limit (%d) on PowerFlex Storage with pool id: %d", connectedSdcsCount, connectedClientsLimit, storagePoolId));
                return true;
            }
            logger.debug(String.format("Current connected SDCs count: %d - SDC connections limit (%d) reached on PowerFlex Storage with pool id: %d", connectedSdcsCount, connectedClientsLimit, storagePoolId));
            return false;
        } catch (Exception e) {
            String errMsg = "Unable to check SDC connections for the PowerFlex storage pool with id: " + storagePoolId + " due to " + e.getMessage();
            logger.warn(errMsg, e);
            return false;
        }
    }

    @Override
    public String prepareSDC(Host host, DataStore dataStore) {
        if (Boolean.FALSE.equals(ConnectOnDemand.valueIn(host.getDataCenterId()))) {
            logger.debug(String.format("On-demand connect/disconnect config %s disabled in the zone %d, no need to prepare SDC (check for connected SDC)", ConnectOnDemand.key(), host.getDataCenterId()));
            return getConnectedSdc(host, dataStore);
        }

        String systemId = storagePoolDetailsDao.findDetail(dataStore.getId(), ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID).getValue();
        if (systemId == null) {
            throw new CloudRuntimeException("Unable to prepare SDC, failed to get the system id for PowerFlex storage pool: " + dataStore.getName());
        }

        GlobalLock hostIdStorageSystemIdLock = null;
        GlobalLock storageSystemIdLock = null;
        try {
            String hostIdStorageSystemIdLockString = String.format(POWERFLEX_SDC_HOSTID_SYSTEMID_LOCK_FORMAT, host.getId(), systemId);
            hostIdStorageSystemIdLock = GlobalLock.getInternLock(hostIdStorageSystemIdLockString);
            if (hostIdStorageSystemIdLock == null) {
                throw new CloudRuntimeException("Unable to prepare SDC, couldn't get global lock on " + hostIdStorageSystemIdLockString);
            }

            int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
            if (!hostIdStorageSystemIdLock.lock(storagePoolMaxWaitSeconds)) {
                logger.debug("Unable to prepare SDC, couldn't lock on " + hostIdStorageSystemIdLockString);
                throw new CloudRuntimeException("Unable to prepare SDC, couldn't lock on " + hostIdStorageSystemIdLockString);
            }

            long poolId = dataStore.getId();
            long hostId = host.getId();
            String sdcId = getConnectedSdc(host, dataStore);
            if (StringUtils.isNotBlank(sdcId)) {
                logger.debug(String.format("SDC %s already connected for the pool: %d on host: %d, no need to prepare/start it", sdcId, poolId, hostId));
                return sdcId;
            }

            String storageSystemIdLockString = String.format(POWERFLEX_SDC_SYSTEMID_LOCK_FORMAT, systemId);
            storageSystemIdLock = GlobalLock.getInternLock(storageSystemIdLockString);
            if (storageSystemIdLock == null) {
                logger.error("Unable to prepare SDC, couldn't get global lock on: " + storageSystemIdLockString);
                throw new CloudRuntimeException("Unable to prepare SDC, couldn't get global lock on " + storageSystemIdLockString);
            }

            if (!storageSystemIdLock.lock(storagePoolMaxWaitSeconds)) {
                logger.error("Unable to prepare SDC, couldn't lock on " + storageSystemIdLockString);
                throw new CloudRuntimeException("Unable to prepare SDC, couldn't lock on " + storageSystemIdLockString);
            }

            if (!areSDCConnectionsWithinLimit(poolId)) {
                String errorMsg = String.format("Unable to check SDC connections or the connections limit reached for Powerflex storage (System ID: %s)", systemId);
                logger.error(errorMsg);
                throw new CloudRuntimeException(errorMsg);
            }

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

            int waitTimeInSecs = 15; // Wait for 15 secs (usual tests with SDC service start took 10-15 secs)
            if (hostSdcConnected(sdcId, poolId, waitTimeInSecs)) {
                return sdcId;
            }
            return null;
        } finally {
            if (storageSystemIdLock != null) {
                storageSystemIdLock.unlock();
                storageSystemIdLock.releaseRef();
            }
            if (hostIdStorageSystemIdLock != null) {
                hostIdStorageSystemIdLock.unlock();
                hostIdStorageSystemIdLock.releaseRef();
            }
        }
    }

    private String prepareSDCOnHost(Host host, DataStore dataStore, String systemId) {
        logger.debug(String.format("Preparing SDC on the host %s (%s)", host.getId(), host.getName()));
        Map<String,String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);
        PrepareStorageClientCommand cmd = new PrepareStorageClientCommand(((PrimaryDataStore) dataStore).getPoolType(), dataStore.getUuid(), details);
        int timeoutSeconds = 60;
        cmd.setWait(timeoutSeconds);

        PrepareStorageClientAnswer prepareStorageClientAnswer;
        try {
            prepareStorageClientAnswer = (PrepareStorageClientAnswer) agentManager.send(host.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String err = String.format("Failed to prepare SDC on the host %s, due to: %s", host.getName(), e.getMessage());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        if (prepareStorageClientAnswer == null) {
            String err = String.format("Unable to prepare SDC on the host %s", host.getName());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        if (!prepareStorageClientAnswer.getResult()) {
            String err = String.format("Unable to prepare SDC on the host %s, due to: %s", host.getName(), prepareStorageClientAnswer.getDetails());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        Map<String,String> poolDetails = prepareStorageClientAnswer.getDetailsMap();
        if (MapUtils.isEmpty(poolDetails)) {
            logger.warn(String.format("PowerFlex storage SDC details not found on the host: %s, try (re)install SDC and restart agent", host.getId()));
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
            logger.warn(String.format("Couldn't retrieve PowerFlex storage SDC details from the host: %s, try (re)install SDC and restart agent", host.getId()));
            return null;
        }

        return sdcId;
    }

    @Override
    public boolean stopSDC(Host host, DataStore dataStore) {
        if (Boolean.FALSE.equals(ConnectOnDemand.valueIn(host.getDataCenterId()))) {
            logger.debug(String.format("On-demand connect/disconnect config %s disabled in the zone %d, no need to unprepare SDC", ConnectOnDemand.key(), host.getDataCenterId()));
            return true;
        }

        String systemId = storagePoolDetailsDao.findDetail(dataStore.getId(), ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID).getValue();
        if (systemId == null) {
            throw new CloudRuntimeException("Unable to unprepare SDC, failed to get the system id for PowerFlex storage pool: " + dataStore.getName());
        }

        GlobalLock lock = null;
        try {
            String hostIdStorageSystemIdLockString = String.format(POWERFLEX_SDC_HOSTID_SYSTEMID_LOCK_FORMAT, host.getId(), systemId);
            lock = GlobalLock.getInternLock(hostIdStorageSystemIdLockString);
            if (lock == null) {
                throw new CloudRuntimeException("Unable to unprepare SDC, couldn't get global lock on " + hostIdStorageSystemIdLockString);
            }

            int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
            if (!lock.lock(storagePoolMaxWaitSeconds)) {
                logger.debug("Unable to unprepare SDC, couldn't lock on " + hostIdStorageSystemIdLockString);
                throw new CloudRuntimeException("Unable to unprepare SDC, couldn't lock on " + hostIdStorageSystemIdLockString);
            }

            long poolId = dataStore.getId();
            long hostId = host.getId();
            String sdcId = getConnectedSdc(host, dataStore);
            if (StringUtils.isBlank(sdcId)) {
                logger.debug("SDC not connected, no need to unprepare it");
                return true;
            }

            return unprepareSDCOnHost(host, dataStore);
        } finally {
            if (lock != null) {
                lock.unlock();
                lock.releaseRef();
            }
        }
    }

    private boolean unprepareSDCOnHost(Host host, DataStore dataStore) {
        logger.debug(String.format("Unpreparing SDC on the host %s (%s)", host.getId(), host.getName()));
        UnprepareStorageClientCommand cmd = new UnprepareStorageClientCommand(((PrimaryDataStore) dataStore).getPoolType(), dataStore.getUuid());
        int timeoutSeconds = 60;
        cmd.setWait(timeoutSeconds);

        Answer unprepareStorageClientAnswer;
        try {
            unprepareStorageClientAnswer = agentManager.send(host.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String err = String.format("Failed to unprepare SDC on the host %s due to: %s", host.getName(), e.getMessage());
            logger.error(err);
            return false;
        }

        if (!unprepareStorageClientAnswer.getResult()) {
            String err = String.format("Unable to unprepare SDC on the the host %s due to: %s", host.getName(), unprepareStorageClientAnswer.getDetails());
            logger.error(err);
            return false;
        }
        return true;
    }

    private String getHostSdcId(String sdcGuid, long poolId) {
        try {
            logger.debug(String.format("Try to get host SDC Id for pool: %s, with SDC guid %s", poolId, sdcGuid));
            ScaleIOGatewayClient client = getScaleIOClient(poolId);
            return client.getSdcIdByGuid(sdcGuid);
        } catch (Exception e) {
            logger.error(String.format("Failed to get host SDC Id for pool: %s", poolId), e);
            throw new CloudRuntimeException(String.format("Failed to establish connection with PowerFlex Gateway to get host SDC Id for pool: %s", poolId));
        }
    }

    private String getConnectedSdc(Host host, DataStore dataStore) {
        long poolId = dataStore.getId();
        long hostId = host.getId();

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
            logger.warn("Unable to get connected SDC for the host: " + hostId + " and storage pool: " + poolId + " due to " + e.getMessage(), e);
        }

        return null;
    }

    private boolean hostSdcConnected(String sdcId, long poolId, int waitTimeInSecs) {
        logger.debug(String.format("Waiting (for %d secs) for the SDC %s of the pool id: %d to connect", waitTimeInSecs, sdcId, poolId));
        int timeBetweenTries = 1000; // Try more frequently (every sec) and return early if connected
        while (waitTimeInSecs > 0) {
            if (isHostSdcConnected(sdcId, poolId)) {
                return true;
            }
            waitTimeInSecs--;
            try {
                Thread.sleep(timeBetweenTries);
            } catch (Exception ignore) {
            }
        }
        return isHostSdcConnected(sdcId, poolId);
    }

    private boolean isHostSdcConnected(String sdcId, long poolId) {
        try {
            final ScaleIOGatewayClient client = getScaleIOClient(poolId);
            return client.isSdcConnected(sdcId);
        } catch (Exception e) {
            logger.error("Failed to check host SDC connection", e);
            throw new CloudRuntimeException("Failed to establish connection with PowerFlex Gateway to check host SDC connection");
        }
    }

    private ScaleIOGatewayClient getScaleIOClient(final Long storagePoolId) throws Exception {
        return ScaleIOGatewayClientConnectionPool.getInstance().getClient(storagePoolId, storagePoolDetailsDao);
    }

    @Override
    public String getConfigComponentName() {
        return ScaleIOSDCManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{ConnectOnDemand};
    }
}
