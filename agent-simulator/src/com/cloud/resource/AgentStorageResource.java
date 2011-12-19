/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.resource;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingStorageCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.storage.ssCommand;
import com.cloud.agent.manager.SimulatorManager;
import com.cloud.agent.manager.SimulatorManager.AgentType;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.storage.resource.SecondaryStorageResource;
import com.cloud.vm.SecondaryStorageVm;


public class AgentStorageResource extends AgentResourceBase implements SecondaryStorageResource {
    private static final Logger s_logger = Logger.getLogger(AgentStorageResource.class);

    final protected String _parent = "/mnt/SecStorage";
    protected String _role;

    public AgentStorageResource(long instanceId, AgentType agentType, SimulatorManager simMgr, String hostGuid) {
        super(instanceId, agentType, simMgr, hostGuid);
    }

    public AgentStorageResource() {
        setType(Host.Type.SecondaryStorage);
    }

    @Override
    public Answer executeRequest(Command cmd) {
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
    	if(SecondaryStorageVm.Role.templateProcessor.toString().equals(_role))
    		return Host.Type.SecondaryStorage;    	
    	return Host.Type.SecondaryStorageCmdExecutor;
    }

    @Override
    public StartupCommand[] initialize() {
    	StartupStorageCommand cmd = new StartupStorageCommand();

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
        return new StartupCommand[] { cmd };
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
    public String getRootDir(ssCommand cmd) {
        // TODO Auto-generated method stub
        return null;
    }
}
