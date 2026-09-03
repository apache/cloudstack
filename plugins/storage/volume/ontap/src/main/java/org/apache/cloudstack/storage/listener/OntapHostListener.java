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

package org.apache.cloudstack.storage.listener;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.alert.AlertManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class OntapHostListener implements HypervisorHostListener {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private AgentManager _agentMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private StoragePoolHostDao storagePoolHostDao;
    @Inject
    private StoragePoolDetailsDao _storagePoolDetailsDao;


    @Override
    public boolean hostConnect(long hostId, long poolId)  {
        logger.info("hostConnect: Connecting host {} to pool {}", hostId, poolId);
        Host host = _hostDao.findById(hostId);
        if (host == null) {
            logger.error("hostConnect: Host was not found with id: {}", hostId);
            return false;
        }
        if (!host.getHypervisorType().equals(Hypervisor.HypervisorType.KVM)) {
            logger.error("hostConnect: ONTAP plugin does not support {} type host currently", host.getHypervisorType());
            return false;
        }

        StoragePool pool = _storagePoolDao.findById(poolId);
        if (pool == null) {
            logger.error("hostConnect: Failed to connect host - storage pool not found with id: {}", poolId);
            return false;
        }
        logger.info("hostConnect: Connecting host {} to ONTAP storage pool {}", host.getName(), pool.getName());
        try {
            // Load storage pool details from database to pass mount options and other config to agent
            Map<String, String> detailsMap = _storagePoolDetailsDao.listDetailsKeyPairs(poolId);
            if (detailsMap == null || detailsMap.isEmpty()) {
                logger.error("hostConnect: Failed to load storage pool details for pool id: {}", poolId);
                return false;
            }

            if (detailsMap.get(OntapStorageConstants.PROTOCOL) == null) {
                logger.error("hostConnect: Storage pool details missing required protocol type for pool id: {}", poolId);
                return false;
            }

            // Update NFS export policy for this connected host when the pool protocol is NFS3.
            updateNfsExportPolicyForConnectedHostIfNeeded(poolId, hostId, host, detailsMap);

            // Create the ModifyStoragePoolCommand to send to the agent
            // Note: Always send command even if database entry exists, because agent may have restarted
            // and lost in-memory pool registration. The command handler is idempotent.
            ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool, detailsMap);

            Answer answer = _agentMgr.easySend(hostId, cmd);

            if (answer == null) {
                throw new CloudRuntimeException(String.format("Unable to get an answer to the modify storage pool command (%s)", pool));
            }

            if (!answer.getResult()) {
                String msg = String.format("Unable to attach storage pool %s to host %d", pool, hostId);

                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, pool.getDataCenterId(), pool.getPodId(), msg, msg);

                throw new CloudRuntimeException(String.format(
                        "Unable to establish a connection from agent to storage pool %s due to %s", pool, answer.getDetails()));
            }

            // Get the mount path from the answer

            if (!(answer instanceof ModifyStoragePoolAnswer)) {
                throw new CloudRuntimeException(String.format(
                        "Unexpected answer type %s returned for modify storage pool command for pool %s on host %d",
                        answer.getClass().getName(), pool, hostId));
            }

            ModifyStoragePoolAnswer mspAnswer = (ModifyStoragePoolAnswer) answer;
            StoragePoolInfo poolInfo = mspAnswer.getPoolInfo();
            if (poolInfo == null) {
                throw new CloudRuntimeException("ModifyStoragePoolAnswer returned null poolInfo");
            }

            String localPath = poolInfo.getLocalPath();
            logger.info("hostConnect: Storage pool {} successfully mounted at: {}", pool.getName(), localPath);

            // Update or create the storage_pool_host_ref entry with the correct local_path
            StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(poolId, hostId);

            if (storagePoolHost == null) {
                storagePoolHost = new StoragePoolHostVO(poolId, hostId, localPath);
                storagePoolHostDao.persist(storagePoolHost);
                logger.info("hostConnect: Created storage_pool_host_ref entry for pool {} and host {}", pool.getName(), host.getName());
            } else {
                storagePoolHost.setLocalPath(localPath);
                storagePoolHostDao.update(storagePoolHost.getId(), storagePoolHost);
                logger.info("hostConnect: Updated storage_pool_host_ref entry with local_path: {}", localPath);
            }

            // Update pool capacity/usage information
            StoragePoolVO poolVO = _storagePoolDao.findById(poolId);
            if (poolVO != null && poolInfo.getCapacityBytes() > 0) {
                poolVO.setCapacityBytes(poolInfo.getCapacityBytes());
                poolVO.setUsedBytes(poolInfo.getCapacityBytes() - poolInfo.getAvailableBytes());
                _storagePoolDao.update(poolVO.getId(), poolVO);
                logger.info("hostConnect: Updated storage pool capacity: {} GB, used: {} GB", poolInfo.getCapacityBytes() / (1024 * 1024 * 1024), (poolInfo.getCapacityBytes() - poolInfo.getAvailableBytes()) / (1024 * 1024 * 1024));
            }

        } catch (Exception e) {
            logger.error("hostConnect: Exception while connecting host {} to storage pool {}", host.getName(), pool.getName(), e);
            // CRITICAL: Don't throw exception - it crashes the agent and causes restart loops
            // Return false to indicate failure without crashing
            return false;
        }
        return true;
    }

    private void updateNfsExportPolicyForConnectedHostIfNeeded(long poolId, long hostId, Host host, Map<String, String> detailsMap) {
        if (!ProtocolType.NFS3.name().equalsIgnoreCase(detailsMap.get(OntapStorageConstants.PROTOCOL))) {
            return;
        }

        if (!isNfs3EnabledOnHost(host)) {
            throw new CloudRuntimeException("NFS protocol is not enabled on host with id: " + hostId);
        }

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(poolId);
        accessGroup.setHostsToConnect(List.of((HostVO) host));

        StorageStrategy strategy = OntapStorageUtils.getStrategyByStoragePoolDetails(detailsMap);
        strategy.updateAccessGroup(accessGroup);
        logger.info("hostConnect: updateNfsExportPolicyForConnectedHostIfNeeded: Updated NFS export policy rules for host {} on storage pool {}", host.getName(), poolId);
    }

    private boolean isNfs3EnabledOnHost(Host host) {
        if (host == null) {
            return false;
        }

        String storageIp = host.getStorageIpAddress() != null ? host.getStorageIpAddress().trim() : "";
        if (storageIp.isEmpty() && StringUtils.isBlank(host.getPrivateIpAddress())) {
            logger.warn("isNfs3EnabledOnHost: Host {} is not eligible for NFS3 protocol: both storage IP and private IP are empty",
                    host.getId());
            return false;
        }

        return true;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        logger.info("hostDisconnected: Disconnecting host {} from pool {}", hostId, poolId);
        // Note: This is not currently being called for NetApp ONTAP storage plugin.
        return false;
    }

    @Override
    public boolean hostAboutToBeRemoved(long hostId) {
        logger.info("hostAboutToBeRemoved: Host {} is about to be removed", hostId);

        Host host = _hostDao.findById(hostId);
        if (host == null) {
            logger.warn("hostAboutToBeRemoved: Host not found with id: {}, considering it as no-op", hostId);
            return true;
        }

        List<StoragePoolHostVO> poolHostRefs = storagePoolHostDao.listByHostId(hostId);
        if (poolHostRefs == null || poolHostRefs.isEmpty()) {
            logger.debug("hostAboutToBeRemoved: No storage pool associations found for host {}", hostId);
            return true;
        }

        for (StoragePoolHostVO ref : poolHostRefs) {
            StoragePoolVO pool = _storagePoolDao.findById(ref.getPoolId());
            if (pool != null) {
                removeHostFromOntapPoolIfNeeded(pool, host);
            }
        }

        logger.info("hostAboutToBeRemoved: Cleaned up ONTAP export policies for host {} about to be removed", hostId);
        return true;
    }

    @Override
    public boolean hostRemoved(long hostId, long clusterId) {
        return false;
    }

    private void removeHostFromOntapPoolIfNeeded(StoragePoolVO pool, Host host) {
        try {
            Map<String, String> detailsMap = _storagePoolDetailsDao.listDetailsKeyPairs(pool.getId());
            if (detailsMap == null || detailsMap.isEmpty()) {
                logger.debug("hostAboutToBeRemoved: removeHostFromOntapPoolIfNeeded: No pool details found for pool id: {}", pool.getId());
                return;
            }

            // Skip non-NFS3 pools; Currently, for iSCSI type, iGroup rules are being handled as part of revokeAccess in OntapPrimaryDataStoreDriver, so no need to handle here.
            if (!ProtocolType.NFS3.name().equalsIgnoreCase(detailsMap.get(OntapStorageConstants.PROTOCOL))) {
                return;
            }

            logger.info("hostAboutToBeRemoved: removeHostFromOntapPoolIfNeeded: Removing export policy rule for host {} from storage pool {}", host.getName(), pool.getName());
            if (!isNfs3EnabledOnHost(host)) {
                logger.warn("hostAboutToBeRemoved: removeHostFromOntapPoolIfNeeded: Skipping NFS export policy removal for host {} on pool {} as host is not NFS-enabled",
                        host.getId(), pool.getId());
                return;
            }
            AccessGroup accessGroup = new AccessGroup();
            accessGroup.setStoragePoolId(pool.getId());
            accessGroup.setHostsToConnect(List.of((HostVO) host));
            accessGroup.setHostRuleAction(AccessGroup.HostRuleAction.REMOVE);

            StorageStrategy strategy = OntapStorageUtils.getStrategyByStoragePoolDetails(detailsMap);
            strategy.updateAccessGroup(accessGroup);
            logger.info("hostAboutToBeRemoved: removeHostFromOntapPoolIfNeeded: Removed NFS export policy rules for removed host {} from storage pool {}", host.getName(), pool.getName());
        } catch (Exception e) {
            logger.warn("hostAboutToBeRemoved: removeHostFromOntapPoolIfNeeded: Failed to remove NFS export policy rule for host {} from pool {}: {}", host.getId(), pool.getName(), e.getMessage());
            // Continue processing other pools even if one fails
        }
    }

    @Override
    public boolean hostEnabled(long hostId) {
        return false;
    }

    @Override
    public boolean hostAdded(long hostId) {
        return false;
    }

}
