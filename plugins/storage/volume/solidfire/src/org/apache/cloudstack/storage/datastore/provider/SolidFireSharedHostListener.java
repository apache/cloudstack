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

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.alert.AlertManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SolidFireSharedHostListener implements HypervisorHostListener {
    private static final Logger s_logger = Logger.getLogger(DefaultHostListener.class);

    @Inject private AgentManager agentMgr;
    @Inject private DataStoreManager dataStoreMgr;
    @Inject private AlertManager alertMgr;
    @Inject private StoragePoolHostDao storagePoolHostDao;
    @Inject private PrimaryDataStoreDao primaryStoreDao;

    @Override
    public boolean hostConnect(long hostId, long storagePoolId) {
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(storagePoolId, hostId);

        if (storagePoolHost == null) {
            storagePoolHost = new StoragePoolHostVO(storagePoolId, hostId, "");

            storagePoolHostDao.persist(storagePoolHost);
        }

        StoragePool storagePool = (StoragePool)dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);
        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, storagePool);
        Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command for storage pool: " + storagePool.getId());
        }

        if (!answer.getResult()) {
            String msg = "Unable to attach storage pool " + storagePoolId + " to the host " + hostId;

            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, storagePool.getDataCenterId(), storagePool.getPodId(), msg, msg);

            throw new CloudRuntimeException(msg);
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "ModifyStoragePoolAnswer not returned from ModifyStoragePoolCommand; Storage pool = " +
            storagePool.getId() + "; Host=" + hostId;

        s_logger.info("Connection established between storage pool " + storagePool + " and host + " + hostId);

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
}
