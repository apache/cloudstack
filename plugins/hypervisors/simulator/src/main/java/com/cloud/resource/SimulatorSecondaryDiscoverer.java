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
package com.cloud.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.resource.SecondaryStorageDiscoverer;
import org.apache.cloudstack.storage.resource.SecondaryStorageResource;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.manager.MockStorageManager;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.storage.dao.SnapshotDao;

public class SimulatorSecondaryDiscoverer extends SecondaryStorageDiscoverer implements ResourceStateAdapter, Listener {
    @Inject
    MockStorageManager _mockStorageMgr = null;
    @Inject
    SnapshotDao _snapshotDao;
    @Inject
    ImageStoreDao imageStoreDao;
    protected SecondaryStorageResource resource;

    public void setResource(SecondaryStorageResource resource) {
        this.resource = resource;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _agentMgr.registerForHostEvents(this, true, false, false);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return super.configure(name, params);
    }

    @Override
    public Map<? extends ServerResource, Map<String, String>>
        find(long dcId, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) {
        if (!uri.getScheme().equalsIgnoreCase("sim")) {
            logger.debug("It's not NFS or file or ISO, so not a secondary storage server: " + uri.toString());
            return null;
        }
        List<ImageStoreVO> stores = imageStoreDao.listImageStores();
        for (ImageStoreVO store : stores) {
            _mockStorageMgr.preinstallTemplates(store.getUrl(), dcId);
        }
        Map<SecondaryStorageResource, Map<String, String>> resources = new HashMap<SecondaryStorageResource, Map<String, String>>();
        resources.put(this.resource, new HashMap<String, String>());
        return resources;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {

    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        //for detecting SSVM dispatch
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupSecondaryStorageCommand)) {
            return null;
        }

        host.setType(com.cloud.host.Host.Type.SecondaryStorageVM);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        // no need to handle, since secondary storage is no longer a host anymore.
        return null;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return true;
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {

    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }
}
