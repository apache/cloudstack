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

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AssociateMacToNetworkAnswer;
import com.cloud.agent.api.AssociateMacToNetworkCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateNetworkAnswer;
import com.cloud.agent.api.CreateNetworkCommand;
import com.cloud.agent.api.DeleteNetworkAnswer;
import com.cloud.agent.api.DeleteNetworkCommand;
import com.cloud.agent.api.DisassociateMacFromNetworkAnswer;
import com.cloud.agent.api.DisassociateMacFromNetworkCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupBrocadeVcsCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.network.brocade.BrocadeVcsApi;
import com.cloud.network.brocade.BrocadeVcsApiException;
import com.cloud.network.schema.showvcs.Output;
import com.cloud.network.schema.showvcs.VcsNodeInfo;
import com.cloud.resource.ServerResource;

public class BrocadeVcsResource implements ServerResource {
    protected Logger logger = LogManager.getLogger(getClass());

    private String _name;
    private String _guid;
    private String _zoneId;
    private int _numRetries;

    private BrocadeVcsApi _brocadeVcsApi;

    protected BrocadeVcsApi createBrocadeVcsApi(String ip, String username, String password) {

        return new BrocadeVcsApi(ip, username, password);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        _name = (String)params.get("name");
        if (_name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        _zoneId = (String)params.get("zoneId");
        if (_zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        _numRetries = 2;

        String ip = (String)params.get("ip");
        if (ip == null) {
            throw new ConfigurationException("Unable to find IP");
        }

        String adminuser = (String)params.get("adminuser");
        if (adminuser == null) {
            throw new ConfigurationException("Unable to find admin username");
        }

        String adminpass = (String)params.get("adminpass");
        if (adminpass == null) {
            throw new ConfigurationException("Unable to find admin password");
        }

        _brocadeVcsApi = createBrocadeVcsApi(ip, adminuser, adminpass);

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
        return Host.Type.L2Networking;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupBrocadeVcsCommand sc = new StartupBrocadeVcsCommand();
        sc.setGuid(_guid);
        sc.setName(_name);
        sc.setDataCenter(_zoneId);
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion(BrocadeVcsResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {sc};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {

        Output output;
        try {
            output = _brocadeVcsApi.getSwitchStatus();
        } catch (BrocadeVcsApiException e) {
            logger.error("getSwitchStatus failed", e);
            return null;
        }

        List<VcsNodeInfo> vcsNodes = output.getVcsNodes().getVcsNodeInfo();
        if (vcsNodes != null && !vcsNodes.isEmpty()) {
            for (VcsNodeInfo vcsNodeInfo : vcsNodes) {
                if (!"Online".equals(vcsNodeInfo.getNodeState())) {
                    logger.error("Brocade Switch is not ready: " + id);
                    return null;
                }
            }
        }
        return new PingCommand(Host.Type.L2Networking, id);

    }

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    public Answer executeRequest(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return executeRequest((MaintainCommand)cmd);
        } else if (cmd instanceof CreateNetworkCommand) {
            return executeRequest((CreateNetworkCommand)cmd, numRetries);
        } else if (cmd instanceof AssociateMacToNetworkCommand) {
            return executeRequest((AssociateMacToNetworkCommand)cmd, numRetries);
        } else if (cmd instanceof DisassociateMacFromNetworkCommand) {
            return executeRequest((DisassociateMacFromNetworkCommand)cmd, numRetries);
        } else if (cmd instanceof DeleteNetworkCommand) {
            return executeRequest((DeleteNetworkCommand)cmd, numRetries);
        }
        logger.debug("Received unsupported command " + cmd.toString());
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

    private Answer executeRequest(CreateNetworkCommand cmd, int numRetries) {

        try {
            boolean result = _brocadeVcsApi.createNetwork(cmd.getVlanId(), cmd.getNetworkId());
            return new CreateNetworkAnswer(cmd, result, "Port Profile " + cmd.getNetworkId() + " created");
        } catch (BrocadeVcsApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new CreateNetworkAnswer(cmd, e);
            }
        }

    }

    private Answer executeRequest(AssociateMacToNetworkCommand cmd, int numRetries) {

        try {

            String mac = macReformat64To32(cmd.getInterfaceMac());
            boolean result = _brocadeVcsApi.associateMacToNetwork(cmd.getNetworkId(), mac);
            return new AssociateMacToNetworkAnswer(cmd, result, "Association of mac " + cmd.getInterfaceMac() + " to network " + cmd.getNetworkId() + " done");
        } catch (BrocadeVcsApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new AssociateMacToNetworkAnswer(cmd, e);
            }
        }

    }

    private Answer executeRequest(DisassociateMacFromNetworkCommand cmd, int numRetries) {

        try {

            String mac = macReformat64To32(cmd.getInterfaceMac());
            boolean result = _brocadeVcsApi.disassociateMacFromNetwork(cmd.getNetworkId(), mac);
            return new DisassociateMacFromNetworkAnswer(cmd, result, "Disassociation of mac " + cmd.getInterfaceMac() + " from network " + cmd.getNetworkId() + " done");
        } catch (BrocadeVcsApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new DisassociateMacFromNetworkAnswer(cmd, e);
            }
        }

    }

    private Answer executeRequest(DeleteNetworkCommand cmd, int numRetries) {

        try {
            boolean result = _brocadeVcsApi.deleteNetwork(cmd.getVlanId(), cmd.getNetworkId());
            return new DeleteNetworkAnswer(cmd, result, "Port Profile " + cmd.getNetworkId() + " deleted");
        } catch (BrocadeVcsApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new DeleteNetworkAnswer(cmd, e);
            }
        }

    }

    private String macReformat64To32(String interfaceMac) {
        String mac = interfaceMac.replace(":", "");
        mac = mac.substring(0, 4) + "." + mac.substring(4, 8) + "." + mac.subSequence(8, 12);
        return mac;

    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private Answer retry(Command cmd, int numRetries) {
        logger.warn("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetries);
        return executeRequest(cmd, numRetries);
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void setConfigParams(Map<String, Object> params) {

    }

    @Override
    public Map<String, Object> getConfigParams() {

        return null;
    }

    @Override
    public int getRunLevel() {

        return 0;
    }

    @Override
    public void setRunLevel(int level) {

    }

}
