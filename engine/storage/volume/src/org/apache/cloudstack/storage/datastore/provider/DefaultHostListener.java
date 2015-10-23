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

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.alert.AlertManager;
import com.cloud.exception.StorageConflictException;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class DefaultHostListener implements HypervisorHostListener {
    private static final Logger s_logger = Logger.getLogger(DefaultHostListener.class);
    @Inject
    AgentManager agentMgr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    AlertManager alertMgr;
    @Inject
    StoragePoolHostDao storagePoolHostDao;
    @Inject
    PrimaryDataStoreDao primaryStoreDao;

    @Override
    public boolean hostConnect(long hostId, long poolId) throws StorageConflictException {
        StoragePool pool = (StoragePool)this.dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);
        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool);
        final Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command" + pool.getId());
        }

        if (!answer.getResult()) {
            String msg = "Unable to attach storage pool" + poolId + " to the host" + hostId;
            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, pool.getDataCenterId(), pool.getPodId(), msg, msg);
            throw new CloudRuntimeException("Unable establish connection from storage head to storage pool " + pool.getId() + " due to " + answer.getDetails() +
                pool.getId());
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "Well, now why won't you actually return the ModifyStoragePoolAnswer when it's ModifyStoragePoolCommand? Pool=" +
            pool.getId() + "Host=" + hostId;
        ModifyStoragePoolAnswer mspAnswer = (ModifyStoragePoolAnswer)answer;
        if (mspAnswer.getLocalDatastoreName() != null && pool.isShared()) {
            String datastoreName = mspAnswer.getLocalDatastoreName();
            List<StoragePoolVO> localStoragePools = this.primaryStoreDao.listLocalStoragePoolByPath(pool.getDataCenterId(), datastoreName);
            for (StoragePoolVO localStoragePool : localStoragePools) {
                if (datastoreName.equals(localStoragePool.getPath())) {
                    s_logger.warn("Storage pool: " + pool.getId() + " has already been added as local storage: " + localStoragePool.getName());
                    throw new StorageConflictException("Cannot add shared storage pool: " + pool.getId() + " because it has already been added as local storage:"
                            + localStoragePool.getName());
                }
            }
        }

        StoragePoolHostVO poolHost = storagePoolHostDao.findByPoolHost(pool.getId(), hostId);
        if (poolHost == null) {
            poolHost = new StoragePoolHostVO(pool.getId(), hostId, mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
            storagePoolHostDao.persist(poolHost);
        } else {
            poolHost.setLocalPath(mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
        }

        StoragePoolVO poolVO = this.primaryStoreDao.findById(poolId);
        poolVO.setUsedBytes(mspAnswer.getPoolInfo().getCapacityBytes() - mspAnswer.getPoolInfo().getAvailableBytes());
        poolVO.setCapacityBytes(mspAnswer.getPoolInfo().getCapacityBytes());
        primaryStoreDao.update(pool.getId(), poolVO);

        s_logger.info("Connection established between storage pool " + pool + " and host " + hostId);
        return true;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        // TODO Auto-generated method stub
        return false;
    }

}
