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

package org.apache.cloudstack.network.opendaylight.agent;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.network.opendaylight.agent.commands.AddHypervisorCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.ConfigureNetworkCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.ConfigurePortCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.DestroyNetworkCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.DestroyPortCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.StartupOpenDaylightControllerCommand;
import org.apache.cloudstack.network.opendaylight.agent.responses.AddHypervisorAnswer;
import org.apache.cloudstack.network.opendaylight.agent.responses.ConfigureNetworkAnswer;
import org.apache.cloudstack.network.opendaylight.agent.responses.ConfigurePortAnswer;
import org.apache.cloudstack.network.opendaylight.agent.responses.DestroyNetworkAnswer;
import org.apache.cloudstack.network.opendaylight.agent.responses.DestroyPortAnswer;
import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetwork;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetworkWrapper;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetworksList;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNode;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNodeWrapper;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNodesList;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronPort;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronPortWrapper;
import org.apache.cloudstack.network.opendaylight.api.resources.NeutronNetworksNorthboundAction;
import org.apache.cloudstack.network.opendaylight.api.resources.NeutronNodesNorthboundAction;
import org.apache.cloudstack.network.opendaylight.api.resources.NeutronPortsNorthboundAction;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;

public class OpenDaylightControllerResource implements ServerResource {
    protected Logger logger = LogManager.getLogger(getClass());
    private Map<String, Object> configuration = new HashMap<String, Object>();

    private URL controllerUrl;
    private String controllerUsername;
    private String controllerPassword;

    private int runLevel;

    @Override
    public String getName() {
        if (configuration.containsKey("name"))
            return (String)configuration.get("name");
        else
            return null;
    }

    @Override
    public void setName(String name) {
        configuration.put("name", name);
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        for (Entry<String, Object> entry : params.entrySet()) {
            configuration.put(entry.getKey(), entry.getValue());
        }
        updateConfiguration();
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return Collections.unmodifiableMap(configuration);
    }

    @Override
    public int getRunLevel() {
        return runLevel;
    }

    @Override
    public void setRunLevel(int level) {
        runLevel = level;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        for (Entry<String, Object> entry : params.entrySet()) {
            configuration.put(entry.getKey(), entry.getValue());
        }
        updateConfiguration();
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
    public Type getType() {
        return Type.L2Networking;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupOpenDaylightControllerCommand sc = new StartupOpenDaylightControllerCommand();
        sc.setGuid((String)configuration.get("guid"));
        sc.setName(getName());
        sc.setDataCenter((String)configuration.get("zoneId"));
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion(OpenDaylightControllerResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {sc};

    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(Host.Type.L2Networking, id);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ConfigureNetworkCommand) {
            return executeRequest((ConfigureNetworkCommand)cmd);
        } else if (cmd instanceof DestroyNetworkCommand) {
            return executeRequest((DestroyNetworkCommand)cmd);
        } else if (cmd instanceof ConfigurePortCommand) {
            return executeRequest((ConfigurePortCommand)cmd);
        } else if (cmd instanceof DestroyPortCommand) {
            return executeRequest((DestroyPortCommand)cmd);
        } else if (cmd instanceof AddHypervisorCommand) {
            return executeRequest((AddHypervisorCommand)cmd);
        } else if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return executeRequest((MaintainCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public void disconnected() {
        logger.warn("OpenDaylightControllerResource is disconnected from the controller at " + controllerUrl);

    }

    @Override
    public IAgentControl getAgentControl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        // TODO Auto-generated method stub

    }

    private Answer executeRequest(final ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(final MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private Answer executeRequest(ConfigureNetworkCommand cmd) {
        NeutronNetworksNorthboundAction configureNetwork = new NeutronNetworksNorthboundAction(controllerUrl, controllerUsername, controllerPassword);

        // Find free gre key
        int gre_key = -1;
        Random keyGenerator = new Random(System.currentTimeMillis());
        try {
            NeutronNetworksList<NeutronNetwork> networks = configureNetwork.listAllNetworks();
            while (true) {
                int i = keyGenerator.nextInt();
                for (NeutronNetwork network : networks.getNetworks()) {
                    if (network.getSegmentationId() == i) {
                        continue;
                    }
                }
                gre_key = i;
                break;
            }
        } catch (NeutronRestApiException e) {
            logger.error("Failed to list existing networks on the ODL Controller", e);
            return new ConfigureNetworkAnswer(cmd, e);
        }

        NeutronNetwork newNetwork = new NeutronNetwork();

        // Configuration from the command
        newNetwork.setName(cmd.getName());
        newNetwork.setTenantId(cmd.getTenantId());

        // Static configuration
        newNetwork.setNetworkType("gre");
        newNetwork.setShared(false);
        newNetwork.setSegmentationId(gre_key);
        newNetwork.setId(UUID.randomUUID());

        NeutronNetworkWrapper wrapper = new NeutronNetworkWrapper();
        wrapper.setNetwork(newNetwork);
        try {
            wrapper = configureNetwork.createNeutronNetwork(wrapper);
        } catch (NeutronRestApiException e) {
            logger.error("createNeutronNetwork failed", e);
            return new ConfigureNetworkAnswer(cmd, e);
        }

        return new ConfigureNetworkAnswer(cmd, true, null, wrapper.getNetwork().getId().toString());
    }

    private Answer executeRequest(DestroyNetworkCommand cmd) {
        NeutronNetworksNorthboundAction configureNetwork = new NeutronNetworksNorthboundAction(controllerUrl, controllerUsername, controllerPassword);
        try {
            configureNetwork.deleteNeutronNetwork(cmd.getNetworkUuid());
        } catch (NeutronRestApiException e) {
            logger.error("deleteNeutronNetwork failed", e);
            return new DestroyNetworkAnswer(cmd, e);
        }

        return new DestroyNetworkAnswer(cmd, true, "Network " + cmd.getNetworkUuid() + " deleted");
    }

    private Answer executeRequest(ConfigurePortCommand cmd) {
        NeutronPortsNorthboundAction configurePort = new NeutronPortsNorthboundAction(controllerUrl, controllerUsername, controllerPassword);
        NeutronPort newPort = new NeutronPort();

        // Configuration from the command
        newPort.setId(cmd.getPortId());
        newPort.setTenantId(cmd.getTennantId());
        newPort.setAdminStateUp(true);
        newPort.setName(cmd.getPortId().toString());
        newPort.setNetworkId(cmd.getNetworkId());
        newPort.setMacAddress(cmd.getMacAddress());
        newPort.setDeviceId(UUID.randomUUID());

        // Static valus
        newPort.setStatus("ACTIVE");
        newPort.setFixedIps(Collections.<String> emptyList());

        NeutronPortWrapper portWrapper = new NeutronPortWrapper();
        portWrapper.setPort(newPort);
        try {
            portWrapper = configurePort.createNeutronPort(portWrapper);
        } catch (NeutronRestApiException e) {
            logger.error("createPortCommand failed", e);
            return new ConfigurePortAnswer(cmd, e);
        }

        return new ConfigurePortAnswer(cmd, true, "Port " + portWrapper.getPort().getId().toString() + " created");

    }

    private Answer executeRequest(DestroyPortCommand cmd) {
        NeutronPortsNorthboundAction configurePort = new NeutronPortsNorthboundAction(controllerUrl, controllerUsername, controllerPassword);
        try {
            configurePort.deleteNeutronPort(cmd.getPortId().toString());
        } catch (NeutronRestApiException e) {
            logger.error("deleteNeutronPort failed", e);
            return new DestroyPortAnswer(cmd, e);
        }

        return new DestroyPortAnswer(cmd, true, "Port " + cmd.getPortId().toString() + " deleted");
    }

    private Answer executeRequest(AddHypervisorCommand cmd) {
        NeutronNodesNorthboundAction nodeActions = new NeutronNodesNorthboundAction(controllerUrl, controllerUsername, controllerPassword);
        try {
            NeutronNodesList<NeutronNodeWrapper> nodes = nodeActions.listAllNodes();
            if (nodes.getNodes() != null) {
                for (NeutronNodeWrapper nodeWrapper : nodes.getNodes()) {
                    NeutronNode node = nodeWrapper.getNode();
                    if (node.getId().equals(cmd.getHostId())) {
                        return new AddHypervisorAnswer(cmd, true, "Hypervisor already connected");
                    }
                }
            }

            // Not found in the existing node list, add it
            nodeActions.updateNeutronNodeV2("OVS", cmd.getHostId(), cmd.getIpAddress(), 6640);
        } catch (NeutronRestApiException e) {
            logger.error("Call to OpenDaylight failed", e);
            return new AddHypervisorAnswer(cmd, e);
        }
        return new AddHypervisorAnswer(cmd, true, "Hypervisor " + cmd.getHostId() + " added");
    }

    private void updateConfiguration() {
        if (!configuration.containsKey("url") || !configuration.containsKey("username") || !configuration.containsKey("password"))
            throw new InvalidParameterException("OpenDaylightControllerResource needs a url, username and password.");
        try {
            controllerUrl = new URL((String)configuration.get("url"));
        } catch (MalformedURLException e) {
            throw new InvalidParameterException("OpenDaylightControllerResource found an invalid controller url");
        }
        controllerUsername = (String)configuration.get("username");
        controllerPassword = (String)configuration.get("password");
    }

}
