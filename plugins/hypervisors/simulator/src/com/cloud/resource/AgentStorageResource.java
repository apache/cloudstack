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

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.storage.resource.SecondaryStorageResource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingStorageCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.manager.SimulatorManager;
import com.cloud.agent.manager.SimulatorManager.AgentType;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.vm.SecondaryStorageVm;

public class AgentStorageResource extends AgentResourceBase implements SecondaryStorageResource {
    private static final Logger s_logger = Logger.getLogger(AgentStorageResource.class);

    final protected String _parent = "/mnt/SecStorage";
    protected String _role;

    public AgentStorageResource(long instanceId, AgentType agentType, SimulatorManager simMgr, String hostGuid) {
        super(instanceId, agentType, simMgr, hostGuid);
    }

    public AgentStorageResource() {
        setType(Type.SecondaryStorageVM);
    }

    @Override
    public Answer executeRequestInContext(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand)cmd);
        } else {
            return _simMgr.simulate(cmd, hostGuid);
        }
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        if (isStopped()) {
            return null;
        }
        return new PingStorageCommand(Host.Type.Storage, id, new HashMap<String, Boolean>());
    }

    @Override
    public Type getType() {
        if (SecondaryStorageVm.Role.templateProcessor.toString().equals(_role))
            return Host.Type.SecondaryStorage;
        return Host.Type.SecondaryStorageCmdExecutor;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupSecondaryStorageCommand cmd = new StartupSecondaryStorageCommand();

        cmd.setPrivateIpAddress(agentHost.getPrivateIpAddress());
        cmd.setPrivateNetmask(agentHost.getPrivateNetMask());
        cmd.setPrivateMacAddress(agentHost.getPrivateMacAddress());
        cmd.setStorageIpAddress(agentHost.getStorageIpAddress());
        cmd.setStorageNetmask(agentHost.getStorageNetMask());
        cmd.setStorageMacAddress(agentHost.getStorageMacAddress());
        cmd.setPublicIpAddress(agentHost.getPublicIpAddress());

        cmd.setName(agentHost.getName());
        cmd.setAgentTag("agent-simulator");
        cmd.setVersion(agentHost.getVersion());
        cmd.setDataCenter(String.valueOf(agentHost.getDataCenterId()));
        cmd.setPod(String.valueOf(agentHost.getPodId()));
        cmd.setGuid(agentHost.getGuid());
        return new StartupCommand[] {cmd};
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (!super.configure(name, params)) {
            s_logger.warn("Base class was unable to configure");
            return false;
        }

        return true;
    }

    @Override
    public String getRootDir(String url, String nfsVersion) {
        // TODO Auto-generated method stub
        return null;
    }
}
