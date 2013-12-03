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

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.alert.AlertManager;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SolidFireHostListener implements HypervisorHostListener {
    private static final Logger s_logger = Logger.getLogger(SolidFireHostListener.class);

    @Inject
    private AgentManager _agentMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private DataStoreManager _dataStoreMgr;
    @Inject
    private HostDao _hostDao;
    @Inject
    private StoragePoolHostDao storagePoolHostDao;

    @Override
    public boolean hostConnect(long hostId, long storagePoolId) {
        HostVO host = _hostDao.findById(hostId);

        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(storagePoolId, hostId);

        if (storagePoolHost == null) {
            storagePoolHost = new StoragePoolHostVO(storagePoolId, hostId, "");

            storagePoolHostDao.persist(storagePoolHost);
        }

        // just want to send the ModifyStoragePoolCommand for KVM
        if (host.getHypervisorType() != HypervisorType.KVM) {
            return true;
        }

        StoragePool storagePool = (StoragePool)_dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);
        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, storagePool);

        Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command (" + storagePool.getId() + ")");
        }

        if (!answer.getResult()) {
            String msg = "Unable to attach storage pool " + storagePoolId + " to host " + hostId;

            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, storagePool.getDataCenterId(), storagePool.getPodId(), msg, msg);

            throw new CloudRuntimeException("Unable to establish a connection from agent to storage pool " + storagePool.getId() + " due to " + answer.getDetails() +
                " (" + storagePool.getId() + ")");
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "ModifyStoragePoolAnswer expected ; Pool = " + storagePool.getId() + " Host = " + hostId;

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
