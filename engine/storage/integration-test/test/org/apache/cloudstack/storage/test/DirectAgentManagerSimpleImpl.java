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
package org.apache.cloudstack.storage.test;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.StartupCommandProcessor;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.manager.AgentAttache;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostEnvironment;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.xen.resource.XcpOssResource;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;

public class DirectAgentManagerSimpleImpl extends ManagerBase implements AgentManager {
    private static final Logger logger = Logger.getLogger(DirectAgentManagerSimpleImpl.class);
    private Map<Long, ServerResource> hostResourcesMap = new HashMap<Long, ServerResource>();
    @Inject
    HostDao hostDao;
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer easySend(Long hostId, Command cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    protected void loadResource(Long hostId) {
        HostVO host = hostDao.findById(hostId);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("guid", host.getGuid());
        params.put("ipaddress", host.getPrivateIpAddress());
        params.put("username", "root");
        params.put("password", "password");
        params.put("zone", String.valueOf(host.getDataCenterId()));
        params.put("pod", String.valueOf(host.getPodId()));
        
        ServerResource resource = null;
        if (host.getHypervisorType() == HypervisorType.XenServer) {
             resource = new XcpOssResource();
        }
        
        try {
            resource.configure(host.getName(), params);
            hostResourcesMap.put(hostId, resource);
        } catch (ConfigurationException e) {
            logger.debug("Failed to load resource:" + e.toString());
        }
        HostEnvironment env = new HostEnvironment();
        SetupCommand cmd = new SetupCommand(env);
        cmd.setNeedSetup(true);
        
        resource.executeRequest(cmd);
    }

    @Override
    public synchronized Answer send(Long hostId, Command cmd) throws AgentUnavailableException, OperationTimedoutException {
        ServerResource resource = hostResourcesMap.get(hostId);
        if (resource == null) {
            loadResource(hostId);
            resource = hostResourcesMap.get(hostId);
        }
        
        if (resource == null) {
            return null;
        }
        
        Answer answer = resource.executeRequest(cmd);
        return answer;
    }

    @Override
    public Answer[] send(Long hostId, Commands cmds) throws AgentUnavailableException, OperationTimedoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer[] send(Long hostId, Commands cmds, int timeout) throws AgentUnavailableException, OperationTimedoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long send(Long hostId, Commands cmds, Listener listener) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int registerForHostEvents(Listener listener, boolean connections, boolean commands, boolean priority) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int registerForInitialConnects(StartupCommandProcessor creator, boolean priority) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void unregisterForHostEvents(int id) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean executeUserRequest(long hostId, Event event) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Answer sendTo(Long dcId, HypervisorType type, Command cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void sendToSecStorage(HostVO ssHost, Command cmd, Listener listener) throws AgentUnavailableException {
        // TODO Auto-generated method stub

    }

    @Override
    public Answer sendToSecStorage(HostVO ssHost, Command cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean tapLoadingAgents(Long hostId, TapAgentsAction action) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AgentAttache handleDirectConnectAgent(HostVO host, StartupCommand[] cmds, ServerResource resource, boolean forRebalance) throws ConnectionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean agentStatusTransitTo(HostVO host, Event e, long msId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AgentAttache findAttache(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void disconnectWithoutInvestigation(long hostId, Event event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pullAgentToMaintenance(long hostId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pullAgentOutMaintenance(long hostId) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean reconnect(long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Answer sendToSSVM(Long dcId, Command cmd) {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public void disconnectWithInvestigation(long hostId, Event event) {
		// TODO Auto-generated method stub
		
	}

}
