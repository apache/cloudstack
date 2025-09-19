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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
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
            Boolean.TRUE.toString(),
            "Connect PowerFlex client on Host when first Volume is mapped to SDC and disconnect when last Volume is unmapped from SDC," +
                    " otherwise no action (that is connection remains in the same state whichever it is, connected or disconnected).",
            Boolean.TRUE,
            ConfigKey.Scope.Zone);

    @Inject
    AgentManager agentManager;
    @Inject
    StoragePoolHostDao storagePoolHostDao;
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
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
        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);
        try {
            int connectedClientsLimit = StorageManager.STORAGE_POOL_CONNECTED_CLIENTS_LIMIT.valueIn(storagePoolId);
            if (connectedClientsLimit <= 0) {
                logger.debug(String.format("SDC connections limit (unlimited) on PowerFlex Storage with pool id: %d", storagePoolId));
                return true;
            }

            int connectedSdcsCount = getScaleIOClient(storagePoolId).getConnectedSdcsCount();
            if (connectedSdcsCount < connectedClientsLimit) {
                logger.debug("Current connected SDCs count: {} - SDC connections are " +
                        "within the limit ({}) on PowerFlex Storage with pool {}",
                        connectedSdcsCount, connectedClientsLimit, storagePool);
                return true;
            }
            logger.debug("Current connected SDCs count: {} - SDC connections limit ({}) " +
                    "reached on PowerFlex Storage with pool {}",
                    connectedSdcsCount, connectedClientsLimit, storagePool);
            return false;
        } catch (Exception e) {
            String errMsg = String.format(
                    "Unable to check SDC connections for the PowerFlex storage pool %s due to %s",
                    storagePool, e.getMessage());
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

        String systemId = null;
        StoragePoolDetailVO systemIdDetail = storagePoolDetailsDao.findDetail(dataStore.getId(), ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        if (systemIdDetail != null) {
            systemId = systemIdDetail.getValue();
        }
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
                logger.debug("SDC {} already connected for the pool: {} on host: {}, " +
                        "no need to prepare/start it", sdcId, dataStore, host);
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

            String mdms = getMdms(dataStore.getId());
            sdcId = prepareSDCOnHost(host, dataStore, systemId, mdms);
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

                int waitTimeInSecs = 15; // Wait for 15 secs (usual tests with SDC service start took 10-15 secs)
                if (isHostSdcConnected(sdcId, dataStore, waitTimeInSecs)) {
                    return sdcId;
                }
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

    private String prepareSDCOnHost(Host host, DataStore dataStore, String systemId, String mdms) {
        logger.debug("Preparing SDC on the host {}", host);
        Map<String, String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);
        details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, mdms);
        populateSdcSettings(details, host.getDataCenterId());
        PrepareStorageClientCommand cmd = new PrepareStorageClientCommand(((PrimaryDataStore) dataStore).getPoolType(), dataStore.getUuid(), details);
        int timeoutSeconds = 60;
        cmd.setWait(timeoutSeconds);

        PrepareStorageClientAnswer prepareStorageClientAnswer;
        try {
            prepareStorageClientAnswer = (PrepareStorageClientAnswer) agentManager.send(host.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String err = String.format("Failed to prepare SDC on the host %s, due to: %s", host, e.getMessage());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        if (prepareStorageClientAnswer == null) {
            String err = String.format("Unable to prepare SDC on the host %s", host);
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        if (!prepareStorageClientAnswer.getResult()) {
            String err = String.format("Unable to prepare SDC on the host %s, due to: %s", host, prepareStorageClientAnswer.getDetails());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        Map<String,String> poolDetails = prepareStorageClientAnswer.getDetailsMap();
        if (MapUtils.isEmpty(poolDetails)) {
            logger.warn("PowerFlex storage SDC details not found on the host: {}, try (re)install SDC and restart agent", host);
            return null;
        }

        String sdcId = null;
        if (poolDetails.containsKey(ScaleIOGatewayClient.SDC_ID)) {
            sdcId = poolDetails.get(ScaleIOGatewayClient.SDC_ID);
        } else if (poolDetails.containsKey(ScaleIOGatewayClient.SDC_GUID)) {
            String sdcGuid = poolDetails.get(ScaleIOGatewayClient.SDC_GUID);
            sdcId = getHostSdcId(sdcGuid, dataStore);
        }

        if (StringUtils.isBlank(sdcId)) {
            logger.warn("Couldn't retrieve PowerFlex storage SDC details from the host: {}, add MDMs if On-demand connect disabled or try (re)install SDC & restart agent", host);
            return null;
        }

        return sdcId;
    }

    @Override
    public boolean unprepareSDC(Host host, DataStore dataStore) {
        if (Boolean.FALSE.equals(ConnectOnDemand.valueIn(host.getDataCenterId()))) {
            logger.debug(String.format("On-demand connect/disconnect config %s disabled in the zone %d, no need to unprepare SDC", ConnectOnDemand.key(), host.getDataCenterId()));
            return true;
        }

        String systemId = null;
        StoragePoolDetailVO systemIdDetail = storagePoolDetailsDao.findDetail(dataStore.getId(), ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        if (systemIdDetail != null) {
            systemId = systemIdDetail.getValue();
        }
        if (systemId == null) {
            throw new CloudRuntimeException("Unable to unprepare SDC, failed to get the system id for PowerFlex storage pool: " + dataStore);
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
                StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(dataStore.getId(), host.getId());
                if (storagePoolHost != null) {
                    storagePoolHostDao.deleteStoragePoolHostDetails(host.getId(), dataStore.getId());
                }
                return true;
            }

            if (!canUnprepareSDC(host, dataStore)) {
                logger.debug("Cannot unprepare SDC, there are other pools of the same PowerFlex storage cluster with some volumes mapped to the host SDC");
                return false;
            }

            String mdms = getMdms(dataStore.getId());;
            boolean unprepareSDCStatus = unprepareSDCOnHost(host, dataStore, mdms);
            if (unprepareSDCStatus) {
                StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(dataStore.getId(), host.getId());
                if (storagePoolHost != null) {
                    storagePoolHostDao.deleteStoragePoolHostDetails(host.getId(), dataStore.getId());
                }
            }

            return unprepareSDCStatus;
        } finally {
            if (lock != null) {
                lock.unlock();
                lock.releaseRef();
            }
        }
    }

    private boolean unprepareSDCOnHost(Host host, DataStore dataStore, String mdms) {
        logger.debug(String.format("Unpreparing SDC on the host %s (%s)", host.getId(), host.getName()));
        Map<String,String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, mdms);
        populateSdcSettings(details, host.getDataCenterId());
        UnprepareStorageClientCommand cmd = new UnprepareStorageClientCommand(((PrimaryDataStore) dataStore).getPoolType(), dataStore.getUuid(), details);
        int timeoutSeconds = 60;
        cmd.setWait(timeoutSeconds);

        Answer unprepareStorageClientAnswer;
        try {
            unprepareStorageClientAnswer = agentManager.send(host.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.error("Failed to unprepare SDC on the host {} due to: {}", host, e.getMessage());
            return false;
        }

        if (!unprepareStorageClientAnswer.getResult()) {
            logger.error("Unable to unprepare SDC on the the host {} due to: {}", host, unprepareStorageClientAnswer.getDetails());
            return false;
        }
        return true;
    }

    @Override
    public boolean canUnprepareSDC(Host host, DataStore dataStore) {
        if (host == null || dataStore == null) {
            return false;
        }

        StoragePoolHostVO poolHostVO = storagePoolHostDao.findByPoolHost(dataStore.getId(), host.getId());
        if (poolHostVO == null) {
            return false;
        }

        final String sdcId = poolHostVO.getLocalPath();
        if (StringUtils.isBlank(sdcId)) {
            return false;
        }

        try {
            if (logger.isDebugEnabled()) {
                List<StoragePoolHostVO> poolHostVOsBySdc = storagePoolHostDao.findByLocalPath(sdcId);
                if (CollectionUtils.isNotEmpty(poolHostVOsBySdc) && poolHostVOsBySdc.size() > 1) {
                    logger.debug(String.format("There are other connected pools with the same SDC of the host %s", host));
                }
            }

            return !areVolumesMappedToPoolSdc(dataStore.getId(), sdcId);
        } catch (Exception e) {
            logger.warn("Unable to check whether the SDC of the pool: " + dataStore.getId() + " can be unprepared on the host: " + host.getId() + ", due to " + e.getMessage(), e);
            return false;
        }
    }

    private boolean areVolumesMappedToPoolSdc(long storagePoolId, String sdcId) throws Exception {
        try {
            final ScaleIOGatewayClient client = getScaleIOClient(storagePoolId);
            return CollectionUtils.isNotEmpty(client.listVolumesMappedToSdc(sdcId));
        } catch (Exception e) {
            logger.warn("Unable to check the volumes mapped to SDC of the pool: " + storagePoolId + ", due to " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String getHostSdcId(String sdcGuid, DataStore dataStore) {
        try {
            logger.debug("Try to get host SDC Id for pool: {}, with SDC guid {}", dataStore, sdcGuid);
            ScaleIOGatewayClient client = getScaleIOClient(dataStore.getId());
            return client.getSdcIdByGuid(sdcGuid);
        } catch (Exception e) {
            logger.error(String.format("Failed to get host SDC Id for pool: %s", dataStore), e);
            throw new CloudRuntimeException(String.format("Failed to establish connection with PowerFlex Gateway to get host SDC Id for pool: %s", dataStore));
        }
    }

    @Override
    public String getConnectedSdc(Host host, DataStore dataStore) {
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
            logger.warn(
                    String.format("Unable to get connected SDC for the host: %s and storage pool: %s due to %s",
                    host, dataStore, e.getMessage()), e);
        }

        return null;
    }

    @Override
    public boolean isHostSdcConnected(String sdcId, DataStore dataStore, int waitTimeInSecs) {
        long poolId = dataStore.getId();
        logger.debug(String.format("Waiting (for %d secs) for the SDC %s of the pool %s to connect",
                waitTimeInSecs, sdcId, dataStore));
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

    @Override
    public String getMdms(long poolId) {
        String mdms = null;
        StoragePoolDetailVO mdmsDetail = storagePoolDetailsDao.findDetail(poolId, ScaleIOGatewayClient.STORAGE_POOL_MDMS);
        if (mdmsDetail != null) {
            mdms = mdmsDetail.getValue();
        }
        if (StringUtils.isNotBlank(mdms)) {
            return mdms;
        }

        try {
            final ScaleIOGatewayClient client = getScaleIOClient(poolId);
            List<String> mdmAddresses = client.getMdmAddresses();
            if (CollectionUtils.isNotEmpty(mdmAddresses)) {
                mdms = StringUtils.join(mdmAddresses, ",");
                StoragePoolDetailVO storagePoolDetailVO = new StoragePoolDetailVO(poolId, ScaleIOGatewayClient.STORAGE_POOL_MDMS, mdms, false);
                storagePoolDetailsDao.persist(storagePoolDetailVO);
            }
            return mdms;
        } catch (Exception e) {
            logger.error("Failed to get MDMs", e);
            throw new CloudRuntimeException("Failed to fetch PowerFlex MDM details");
        }
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
        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);
        if (storagePool == null) {
            throw new CloudRuntimeException("Unable to find the storage pool with id " + storagePoolId);
        }
        return ScaleIOGatewayClientConnectionPool.getInstance().getClient(storagePool, storagePoolDetailsDao);
    }

    @Override
    public void populateSdcSettings(Map<String, String> details, long dataCenterId) {
        if (details == null) {
            details = new HashMap<>();
        }

        details.put(ScaleIOSDCManager.MdmsChangeApplyWaitTime.key(), String.valueOf(ScaleIOSDCManager.MdmsChangeApplyWaitTime.valueIn(dataCenterId)));
        details.put(ScaleIOSDCManager.ValidateMdmsOnConnect.key(), String.valueOf(ScaleIOSDCManager.ValidateMdmsOnConnect.valueIn(dataCenterId)));
        details.put(ScaleIOSDCManager.BlockSdcUnprepareIfRestartNeededAndVolumesAreAttached.key(), String.valueOf(ScaleIOSDCManager.BlockSdcUnprepareIfRestartNeededAndVolumesAreAttached.valueIn(dataCenterId)));
    }

    @Override
    public String getConfigComponentName() {
        return ScaleIOSDCManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{ConnectOnDemand, MdmsChangeApplyWaitTime, ValidateMdmsOnConnect, BlockSdcUnprepareIfRestartNeededAndVolumesAreAttached};
    }
}
