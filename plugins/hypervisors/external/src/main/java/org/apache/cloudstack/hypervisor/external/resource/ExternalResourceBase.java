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
package org.apache.cloudstack.hypervisor.external.resource;

import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.hypervisor.external.provisioner.simpleprovisioner.SimpleExternalProvisioner;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PostExternalProvisioningAnswer;
import com.cloud.agent.api.PostExternalProvisioningCommand;
import com.cloud.agent.api.PrepareExternalProvisioningAnswer;
import com.cloud.agent.api.PrepareExternalProvisioningCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.host.Host;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Networks;
import com.cloud.resource.ServerResource;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentContext;

public class ExternalResourceBase implements ServerResource {
    protected static final int CPU = 4;
    protected static final long CPU_SPEED = 4000L;
    protected static final long RAM = 16000 * 1024 * 1024L;
    protected static final long DOM0_RAM = 768 * 1024 * 1024L;
    protected static final String CAPABILITIES = "hvm";

    @Inject
    ExternalProvisioner externalProvisioner;

    protected String url;
    protected String dcId;
    protected String pod;
    protected String cluster;
    protected String name;
    protected String guid;
    private final Host.Type type;

    private String extensionName;
    private String extensionRelativeEntryPoint;

    protected boolean isExtensionConnected() {
        return StringUtils.isNoneBlank(extensionName, extensionRelativeEntryPoint);
    }

    public ExternalResourceBase() {
        type = Host.Type.Routing;
    }

    @Override
    public Host.Type getType() {
        return type;
    }

    @Override
    public StartupCommand[] initialize() {
        final StartupRoutingCommand cmd =
                new StartupRoutingCommand(CPU, CPU_SPEED, RAM, DOM0_RAM, CAPABILITIES,
                        Hypervisor.HypervisorType.External,Networks.RouterPrivateIpStrategy.HostLocal);
        cmd.setDataCenter(dcId);
        cmd.setPod(pod);
        cmd.setCluster(cluster);
        cmd.setHostType(type);
        cmd.setName(name);
        cmd.setPrivateIpAddress(Hypervisor.HypervisorType.External.toString());
        cmd.setGuid(guid);
        cmd.setIqn(guid);
        cmd.setVersion(ExternalResourceBase.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {cmd};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        if (isExtensionConnected()) {
            return null;
        }
        final Map<String, HostVmStateReportEntry> vmStates = externalProvisioner.getHostVmStateReport(id, extensionName,
                extensionRelativeEntryPoint);
        return new PingRoutingCommand(Host.Type.Routing, id, vmStates);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        try {
            if (cmd instanceof CheckNetworkCommand) {
                return new CheckNetworkAnswer((CheckNetworkCommand) cmd, true, "Network Setup check by names is done");
            }
            if (cmd instanceof ReadyCommand) {
                return new ReadyAnswer((ReadyCommand) cmd);
            } else if (cmd instanceof StartCommand) {
                return execute((StartCommand) cmd);
            } else if (cmd instanceof StopCommand) {
                return execute((StopCommand) cmd);
            } else if (cmd instanceof RebootCommand) {
                return execute((RebootCommand) cmd);
            } else if (cmd instanceof PrepareExternalProvisioningCommand) {
                return execute((PrepareExternalProvisioningCommand) cmd);
            } else if (cmd instanceof GetHostStatsCommand) {
                return execute((GetHostStatsCommand) cmd);
            } else if (cmd instanceof PingTestCommand) {
                return execute((PingTestCommand) cmd);
            } else if (cmd instanceof MaintainCommand) {
                return execute((MaintainCommand) cmd);
            } else if (cmd instanceof CheckHealthCommand) {
                return execute((CheckHealthCommand) cmd);
            } else if (cmd instanceof CleanupNetworkRulesCmd) {
                return execute((CleanupNetworkRulesCmd) cmd);
            } else if (cmd instanceof GetVmStatsCommand) {
                return execute((GetVmStatsCommand) cmd);
            } else if (cmd instanceof RunCustomActionCommand) {
                return execute((RunCustomActionCommand) cmd);
            } else {
                return execute(cmd);
            }
        } catch (IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private Answer execute(PingTestCommand cmd) {
        return new Answer(cmd);
    }

    private MaintainAnswer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd, false);
    }

    public RunCustomActionAnswer execute(RunCustomActionCommand cmd) {
        return externalProvisioner.runCustomAction(guid, extensionName, extensionRelativeEntryPoint, cmd);
    }

    public Answer execute(Command cmd) {
        RunCustomActionCommand runCustomActionCommand = new RunCustomActionCommand(cmd.toString());
        RunCustomActionAnswer customActionAnswer = externalProvisioner.runCustomAction(guid, extensionName,
                extensionRelativeEntryPoint, runCustomActionCommand);
        return new Answer(cmd, customActionAnswer.getResult(), customActionAnswer.getDetails());
    }

    public StopAnswer execute(StopCommand cmd) {
        if (cmd.isExpungeVM()) {
            return externalProvisioner.expungeInstance(guid, extensionName, extensionRelativeEntryPoint, cmd);
        }
        return externalProvisioner.stopInstance(guid, extensionName, extensionRelativeEntryPoint, cmd);
    }

    public RebootAnswer execute(RebootCommand cmd) {
        return externalProvisioner.rebootInstance(guid, extensionName, extensionRelativeEntryPoint, cmd);
    }

    public PostExternalProvisioningAnswer execute(PostExternalProvisioningCommand cmd) {
        return externalProvisioner.postSetupInstance(guid, extensionName, extensionRelativeEntryPoint, cmd);
    }

    public GetHostStatsAnswer execute(GetHostStatsCommand cmd) {
        return new GetHostStatsAnswer(cmd, null);
    }

    public PrepareExternalProvisioningAnswer execute(PrepareExternalProvisioningCommand cmd) {
        return externalProvisioner.prepareExternalProvisioning(guid, extensionName, extensionRelativeEntryPoint, cmd);
    }

    public StartAnswer execute(StartCommand cmd) {
        return externalProvisioner.startInstance(guid, extensionName, extensionRelativeEntryPoint, cmd);
    }

    private Answer execute(CheckHealthCommand cmd) {
        return externalProvisioner.checkHealth(guid, extensionName, extensionRelativeEntryPoint, cmd);
    }

    private Answer execute(CleanupNetworkRulesCmd cmd) {
        return new Answer(cmd, false, "Not supported");
    }

    private Answer execute(GetVmStatsCommand cmd) {
        return new Answer(cmd, false, "Not supported");
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

    @Override
    public String getName() {
        return null;
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

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        externalProvisioner = ComponentContext.inject(SimpleExternalProvisioner.class);
        externalProvisioner.configure(name, params);
        dcId = (String) params.get("zone");
        pod = (String) params.get("pod");
        cluster = (String) params.get("cluster");
        this.name = (String)params.get("name");
        guid = (String) params.get("guid");
        url = (String) params.get("url");
        extensionName = (String) params.get("extensionName");
        extensionRelativeEntryPoint = (String) params.get("extensionRelativeEntryPoint");
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
}
