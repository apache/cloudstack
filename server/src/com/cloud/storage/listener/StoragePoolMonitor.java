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
package com.cloud.storage.listener;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.OCFS2Manager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManagerImpl;
import com.cloud.storage.StoragePoolStatus;


public class StoragePoolMonitor implements Listener {
    private static final Logger s_logger = Logger.getLogger(StoragePoolMonitor.class);
    private final StorageManagerImpl _storageManager;
    private final PrimaryDataStoreDao _poolDao;
    @Inject OCFS2Manager _ocfs2Mgr;

    public StoragePoolMonitor(StorageManagerImpl mgr, PrimaryDataStoreDao poolDao) {
        this._storageManager = mgr;
        this._poolDao = poolDao;

    }


    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public synchronized boolean processAnswers(long agentId, long seq, Answer[] resp) {
        return true;
    }

    @Override
    public synchronized boolean processDisconnect(long agentId, Status state) {
        return true;
    }

    @Override
    public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (cmd instanceof StartupRoutingCommand) {
            StartupRoutingCommand scCmd = (StartupRoutingCommand)cmd;
            if (scCmd.getHypervisorType() == HypervisorType.XenServer || scCmd.getHypervisorType() ==  HypervisorType.KVM ||
                    scCmd.getHypervisorType() == HypervisorType.VMware || scCmd.getHypervisorType() ==  HypervisorType.Simulator || scCmd.getHypervisorType() == HypervisorType.Ovm) {
                List<StoragePoolVO> pools = _poolDao.listBy(host.getDataCenterId(), host.getPodId(), host.getClusterId(), ScopeType.CLUSTER);
                pools.addAll(_poolDao.findZoneWideStoragePoolsByTags(host.getDataCenterId(), null));
                for (StoragePoolVO pool : pools) {
                    if (pool.getStatus() != StoragePoolStatus.Up) {
                        continue;
                    }
                    if (!pool.isShared()) {
                        continue;
                    }

                    if (pool.getPoolType() == StoragePoolType.OCFS2 && !_ocfs2Mgr.prepareNodes(pool.getClusterId())) {
                        throw new ConnectionException(true, "Unable to prepare OCFS2 nodes for pool " + pool.getId());
                    }

                    Long hostId = host.getId();
                    s_logger.debug("Host " + hostId + " connected, sending down storage pool information ...");
                    try {
                        _storageManager.connectHostToSharedPool(hostId, pool.getId());
                        _storageManager.createCapacityEntry(pool.getId());
                    } catch (Exception e) {
                        s_logger.warn("Unable to connect host " + hostId + " to pool " + pool + " due to " + e.toString(), e);
                    }
                }
            }
        }
    }


    @Override
    public boolean processCommands(long agentId, long seq, Command[] req) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return true;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

}
