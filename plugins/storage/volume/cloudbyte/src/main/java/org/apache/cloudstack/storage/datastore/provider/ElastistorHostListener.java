//
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
//

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
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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

public class ElastistorHostListener implements HypervisorHostListener {
    protected Logger logger = LogManager.getLogger(getClass());
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
    @Inject
    PrimaryDataStoreDao storagePoolDao;
    @Inject
    HostDao  _hostDao;

    @Override
    public boolean hostAdded(long hostId) {
        return true;
    }

    @Override
    public boolean hostConnect(long hostId, long poolId) {
        StoragePool pool = (StoragePool) this.dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);

        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(poolId, hostId);
        HostVO host = _hostDao.findById(hostId);

        if (storagePoolHost == null) {
            storagePoolHost = new StoragePoolHostVO(poolId, hostId, "");

            storagePoolHostDao.persist(storagePoolHost);
        }

        StoragePoolVO poolVO = storagePoolDao.findById(pool.getId());

        if(poolVO.isManaged() && (host.getHypervisorType() != HypervisorType.KVM)){
            return true;
        }

        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool);
        final Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command" + pool.getId());
        }

        if (!answer.getResult()) {
            String msg = "Unable to attach storage pool" + poolId + " to the host" + hostId;

            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST,pool.getDataCenterId(), pool.getPodId(), msg, msg);

            throw new CloudRuntimeException("Unable establish connection from storage head to storage pool " + pool.getId() + " due to " + answer.getDetails() + pool.getId());
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "Well, now why won't you actually return the ModifyStoragePoolAnswer when it's ModifyStoragePoolCommand? Pool=" + pool.getId() + "Host=" + hostId;

        logger.info("Connection established between " + pool + " host + " + hostId);
        return true;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(poolId, hostId);

        if (storagePoolHost != null) {
            storagePoolHostDao.deleteStoragePoolHostDetails(hostId, poolId);
        }
        return false;
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
