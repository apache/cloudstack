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
package com.cloud.network.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateLogicalSwitchAnswer;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupNiciraNvpCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.network.nicira.ControlClusterStatus;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpTag;
import com.cloud.network.nicira.TransportZoneBinding;
import com.cloud.network.nicira.VifAttachment;
import com.cloud.resource.ServerResource;

public class NiciraNvpResource implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(NiciraNvpResource.class);
    
    private String _name;
    private String _ip;
    private String _adminuser;
    private String _adminpass;
    private String _guid;
    private String _zoneId;
    private int _numRetries;
    
    private NiciraNvpApi _niciraNvpApi;
    
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        
        _name = (String) params.get("name");
        if (_name == null) {
            throw new ConfigurationException("Unable to find name");
        }
        
        _ip = (String) params.get("ip");
        if (_ip == null) {
            throw new ConfigurationException("Unable to find IP");
        }
        
        _adminuser = (String) params.get("adminuser");
        if (_adminuser == null) {
            throw new ConfigurationException("Unable to find admin username");
        }
        
        _adminpass = (String) params.get("adminpass");
        if (_adminpass == null) {
            throw new ConfigurationException("Unable to find admin password");
        }               
        
        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }
        
        _zoneId = (String) params.get("zoneId");
        if (_zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }
        
        _numRetries = 2;

        try {
            _niciraNvpApi = new NiciraNvpApi(_ip, _adminuser, _adminpass);
        } catch (NiciraNvpApiException e) {
            throw new ConfigurationException(e.getMessage());
        }

        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Type getType() {
        // Think up a better name for this Type?
        return Host.Type.L2Networking;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupNiciraNvpCommand sc = new StartupNiciraNvpCommand();
        sc.setGuid(_guid);
        sc.setName(_name);
        sc.setDataCenter(_zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion("");
        return new StartupCommand[] { sc };
    }

	@Override
	public PingCommand getCurrentStatus(long id) {
        try {
            ControlClusterStatus ccs = _niciraNvpApi.getControlClusterStatus();
            if (!"stable".equals(ccs.getClusterStatus())) {
            	s_logger.error("ControlCluster state is not stable: "
            			+ ccs.getClusterStatus());
            	return null;
            }
        } catch (NiciraNvpApiException e) {
        	s_logger.error("getControlClusterStatus failed", e);
        	return null;
        }
        return new PingCommand(Host.Type.L2Networking, id);
	}

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    public Answer executeRequest(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand) cmd);
        }
        else if (cmd instanceof MaintainCommand) {
            return executeRequest((MaintainCommand)cmd);
        }
        else if (cmd instanceof CreateLogicalSwitchCommand) {
            return executeRequest((CreateLogicalSwitchCommand)cmd, numRetries);
        }
        else if (cmd instanceof DeleteLogicalSwitchCommand) {
            return executeRequest((DeleteLogicalSwitchCommand) cmd, numRetries);
        }
        else if (cmd instanceof CreateLogicalSwitchPortCommand) {
            return executeRequest((CreateLogicalSwitchPortCommand) cmd, numRetries);
        }
        else if (cmd instanceof DeleteLogicalSwitchPortCommand) {
            return executeRequest((DeleteLogicalSwitchPortCommand) cmd, numRetries);
        }
        s_logger.debug("Received unsupported command " + cmd.toString());
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    @Override
    public void disconnected() {
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
    }
    
    private Answer executeRequest(CreateLogicalSwitchCommand cmd, int numRetries) {
        LogicalSwitch logicalSwitch = new LogicalSwitch();
        logicalSwitch.setDisplay_name("lswitch-" + cmd.getName());
        logicalSwitch.setPort_isolation_enabled(false);

        // Set transport binding
        List<TransportZoneBinding> ltzb = new ArrayList<TransportZoneBinding>();
        ltzb.add(new TransportZoneBinding(cmd.getTransportUuid(), cmd.getTransportType()));
        logicalSwitch.setTransport_zones(ltzb);

        // Tags set to scope cs_account and account name
        List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account",cmd.getOwnerName()));
        logicalSwitch.setTags(tags);
        
        try {
            logicalSwitch = _niciraNvpApi.createLogicalSwitch(logicalSwitch);
            return new CreateLogicalSwitchAnswer(cmd, true, "Logicalswitch " + logicalSwitch.getUuid() + " created", logicalSwitch.getUuid());
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new CreateLogicalSwitchAnswer(cmd, e);
        	}
        }
        
    }
    
    private Answer executeRequest(DeleteLogicalSwitchCommand cmd, int numRetries) {
        try {
            _niciraNvpApi.deleteLogicalSwitch(cmd.getLogicalSwitchUuid());
            return new DeleteLogicalSwitchAnswer(cmd, true, "Logicalswitch " + cmd.getLogicalSwitchUuid() + " deleted");
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new DeleteLogicalSwitchAnswer(cmd, e);
        	}
        }
    }
    
    private Answer executeRequest(CreateLogicalSwitchPortCommand cmd, int numRetries) {
        String logicalSwitchUuid = cmd.getLogicalSwitchUuid();
        String attachmentUuid = cmd.getAttachmentUuid();
        
        try {
            // Tags set to scope cs_account and account name
            List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
            tags.add(new NiciraNvpTag("cs_account",cmd.getOwnerName()));

            LogicalSwitchPort logicalSwitchPort = new LogicalSwitchPort(attachmentUuid, tags, true);
            LogicalSwitchPort newPort = _niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, logicalSwitchPort);
            _niciraNvpApi.modifyLogicalSwitchPortAttachment(cmd.getLogicalSwitchUuid(), newPort.getUuid(), new VifAttachment(attachmentUuid));
            return new CreateLogicalSwitchPortAnswer(cmd, true, "Logical switch port " + newPort.getUuid() + " created", newPort.getUuid());
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new CreateLogicalSwitchPortAnswer(cmd, e);
        	}
        }
        
    }
    
    private Answer executeRequest(DeleteLogicalSwitchPortCommand cmd, int numRetries) {
        try {
            _niciraNvpApi.deleteLogicalSwitchPort(cmd.getLogicalSwitchUuid(), cmd.getLogicalSwitchPortUuid());
            return new DeleteLogicalSwitchPortAnswer(cmd, true, "Logical switch port " + cmd.getLogicalSwitchPortUuid() + " deleted");
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new DeleteLogicalSwitchPortAnswer(cmd, e);
        	}
        }
    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }
    
    private Answer executeRequest(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }    

    private Answer retry(Command cmd, int numRetries) {
        int numRetriesRemaining = numRetries - 1;
        s_logger.warn("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetriesRemaining);
        return executeRequest(cmd, numRetriesRemaining);
    }
    
}
