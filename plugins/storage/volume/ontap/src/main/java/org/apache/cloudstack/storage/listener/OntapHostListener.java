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

import javax.inject.Inject;

import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.alert.AlertManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import com.cloud.host.dao.HostDao;

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
    @Inject private StoragePoolHostDao storagePoolHostDao;


    @Override
    public boolean hostConnect(long hostId, long poolId)  {
        logger.info("Connect to host " + hostId + " from pool " + poolId);
        Host host = _hostDao.findById(hostId);
        if (host == null) {
            logger.error("host was not found with id : {}", hostId);
            return false;
        }

        StoragePool pool = _storagePoolDao.findById(poolId);
        if (pool == null) {
            logger.error("Failed to connect host - storage pool not found with id: {}", poolId);
            return false;
        }
        logger.info("Connecting host {} to ONTAP storage pool {}", host.getName(), pool.getName());
        try {
            ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool);

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

            ModifyStoragePoolAnswer mspAnswer = (ModifyStoragePoolAnswer) answer;
            StoragePoolInfo poolInfo = mspAnswer.getPoolInfo();
            if (poolInfo == null) {
                throw new CloudRuntimeException("ModifyStoragePoolAnswer returned null poolInfo");
            }

            String localPath = poolInfo.getLocalPath();
            logger.info("Storage pool {} successfully mounted at: {}", pool.getName(), localPath);

            StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(poolId, hostId);

            if (storagePoolHost == null) {
                storagePoolHost = new StoragePoolHostVO(poolId, hostId, localPath);
                storagePoolHostDao.persist(storagePoolHost);
                logger.info("Created storage_pool_host_ref entry for pool {} and host {}", pool.getName(), host.getName());
            } else {
                storagePoolHost.setLocalPath(localPath);
                storagePoolHostDao.update(storagePoolHost.getId(), storagePoolHost);
                logger.info("Updated storage_pool_host_ref entry with local_path: {}", localPath);
            }

            StoragePoolVO poolVO = _storagePoolDao.findById(poolId);
            if (poolVO != null && poolInfo.getCapacityBytes() > 0) {
                poolVO.setCapacityBytes(poolInfo.getCapacityBytes());
                poolVO.setUsedBytes(poolInfo.getCapacityBytes() - poolInfo.getAvailableBytes());
                _storagePoolDao.update(poolVO.getId(), poolVO);
                logger.info("Updated storage pool capacity: {} GB, used: {} GB", poolInfo.getCapacityBytes() / (1024 * 1024 * 1024), (poolInfo.getCapacityBytes() - poolInfo.getAvailableBytes()) / (1024 * 1024 * 1024));
            }

        } catch (Exception e) {
            logger.error("Exception while connecting host {} to storage pool {}", host.getName(), pool.getName(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean hostDisconnected(Host host, StoragePool pool) {
        logger.info("Disconnect from host " + host.getId() + " from pool " + pool.getName());

        Host hostToremove = _hostDao.findById(host.getId());
        if (hostToremove == null) {
            logger.error("Failed to add host by HostListener as host was not found with id : {}", host.getId());
            return false;
        }
        logger.info("Disconnecting host {} from ONTAP storage pool {}", host.getName(), pool.getName());

        try {
            DeleteStoragePoolCommand cmd = new DeleteStoragePoolCommand(pool);
            long hostId = host.getId();
            Answer answer = _agentMgr.easySend(hostId, cmd);

            if (answer != null && answer.getResult()) {
                logger.info("Successfully disconnected host {} from ONTAP storage pool {}", host.getName(), pool.getName());
                return true;
            } else {
                String errMsg = (answer != null) ? answer.getDetails() : "Unknown error";
                logger.warn("Failed to disconnect host {} from storage pool {}. Error: {}", host.getName(), pool.getName(), errMsg);
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception while disconnecting host {} from storage pool {}", host.getName(), pool.getName(), e);
            return false;
        }
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        return false;
    }

    @Override
    public boolean hostAboutToBeRemoved(long hostId) {
        return false;
    }

    @Override
    public boolean hostRemoved(long hostId, long clusterId) {
        return false;
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
