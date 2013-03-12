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
package com.cloud.async;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.event.dao.EventDao;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;

@Component
@Local(value={AsyncJobExecutorContext.class})
public class AsyncJobExecutorContextImpl extends ManagerBase implements AsyncJobExecutorContext {

    @Inject private AgentManager _agentMgr;
    @Inject private NetworkModel _networkMgr;
    @Inject private UserVmManager _vmMgr;
    @Inject private SnapshotManager _snapMgr;
    @Inject private AccountManager _accountMgr;
    @Inject private StorageManager _storageMgr;
    @Inject private EventDao _eventDao;
    @Inject private UserVmDao _vmDao;
    @Inject private AccountDao _accountDao;
    @Inject private VolumeDao _volumeDao;
    @Inject private DomainRouterDao _routerDao;
    @Inject private IPAddressDao _ipAddressDao;
    @Inject private AsyncJobDao _jobDao;
    @Inject private UserDao _userDao;
    @Inject private VirtualMachineManager _itMgr;

    @Inject private ManagementServer _managementServer;

    @Override
    public ManagementServer getManagementServer() {
        return _managementServer;
    }

    @Override
    public AgentManager getAgentMgr() {
        return _agentMgr;
    }

    @Override
    public NetworkModel getNetworkMgr() {
        return _networkMgr;
    }

    @Override
    public UserVmManager getVmMgr() {
        return _vmMgr;
    }

    @Override
    public StorageManager getStorageMgr() {
        return _storageMgr;
    }

    /**server/src/com/cloud/async/AsyncJobExecutorContext.java
     * @return the _snapMgr
     */
    @Override
    public SnapshotManager getSnapshotMgr() {
        return _snapMgr;
    }

    @Override
    public AccountManager getAccountMgr() {
        return _accountMgr;
    }

    @Override
    public EventDao getEventDao() {
        return _eventDao;
    }

    @Override
    public UserVmDao getVmDao() {
        return _vmDao;
    }

    @Override
    public AccountDao getAccountDao() {
        return _accountDao;
    }

    @Override
    public VolumeDao getVolumeDao() {
        return _volumeDao;
    }

    @Override
    public DomainRouterDao getRouterDao() {
        return _routerDao;
    }

    @Override
    public IPAddressDao getIpAddressDao() {
        return _ipAddressDao;
    }

    @Override
    public AsyncJobDao getJobDao() {
        return _jobDao;
    }

    @Override
    public UserDao getUserDao() {
        return _userDao;
    }

    @Override
    public VirtualMachineManager getItMgr() {
        return _itMgr;
    }
}
