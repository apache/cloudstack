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

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.command.ExtensionRoutingUpdateCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetExternalConsoleAnswer;
import com.cloud.agent.api.GetExternalConsoleCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
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

public class ExternalResource implements ServerResource {
    protected Logger logger = LogManager.getLogger(getClass());
    protected static final int CPU = 0;
    protected static final long CPU_SPEED = 0;
    protected static final long RAM = 0;
    protected static final long DOM0_RAM = 0;
    protected static final String CAPABILITIES = "hvm";

    protected ExternalProvisioner externalProvisioner;

    protected String url;
    protected String dcId;
    protected String pod;
    protected String cluster;
    protected String name;
    protected String guid;
    private final Host.Type type;

    private String extensionName;
    private String extensionRelativePath;
    private Extension.State extensionState;
    private boolean extensionPathReady;

    protected boolean isExtensionDisconnected() {
        return StringUtils.isAnyBlank(extensionName, extensionRelativePath);
    }

    protected boolean isExtensionNotEnabled() {
        return !Extension.State.Enabled.equals(extensionState);
    }

    protected boolean isExtensionPathNotReady() {
        return !extensionPathReady;
    }

    public ExternalResource() {
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
        cmd.setVersion(ExternalResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {cmd};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        if (isExtensionDisconnected()) {
            return new PingRoutingCommand(Host.Type.Routing, id, new HashMap<>());
        }
        final Map<String, HostVmStateReportEntry> vmStates = externalProvisioner.getHostVmStateReport(id, extensionName,
                extensionRelativePath);
        return new PingRoutingCommand(Host.Type.Routing, id, vmStates);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        try {
            if (cmd instanceof ExtensionRoutingUpdateCommand) {
                return execute((ExtensionRoutingUpdateCommand)cmd);
            } else if (cmd instanceof PingTestCommand) {
                return execute((PingTestCommand) cmd);
            } else if (cmd instanceof ReadyCommand) {
                return execute((ReadyCommand)cmd);
            } else if (cmd instanceof CheckHealthCommand) {
                return execute((CheckHealthCommand) cmd);
            } else if (cmd instanceof CheckNetworkCommand) {
                return execute((CheckNetworkCommand)cmd);
            }else if (cmd instanceof CleanupNetworkRulesCmd) {
                return execute((CleanupNetworkRulesCmd) cmd);
            } else if (cmd instanceof GetVmStatsCommand) {
                return execute((GetVmStatsCommand) cmd);
            } else if (cmd instanceof MaintainCommand) {
                return execute((MaintainCommand) cmd);
            } else if (cmd instanceof StartCommand) {
                return execute((StartCommand) cmd);
            } else if (cmd instanceof StopCommand) {
                return execute((StopCommand) cmd);
            } else if (cmd instanceof RebootCommand) {
                return execute((RebootCommand) cmd);
            } else if (cmd instanceof GetExternalConsoleCommand) {
                return execute((GetExternalConsoleCommand) cmd);
            } else if (cmd instanceof PrepareExternalProvisioningCommand) {
                return execute((PrepareExternalProvisioningCommand) cmd);
            } else if (cmd instanceof GetHostStatsCommand) {
                return execute((GetHostStatsCommand) cmd);
            } else if (cmd instanceof RunCustomActionCommand) {
                return execute((RunCustomActionCommand) cmd);
            } else {
                return execute(cmd);
            }
        } catch (IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }

    protected String logAndGetExtensionNotConnectedOrDisabledError() {
        if (isExtensionDisconnected()) {
            logger.error("Extension not connected to host: {}", name);
            return "Extension not connected";
        }
        if (isExtensionNotEnabled()) {
            logger.error("Extension: {} connected to host: {} is not in Enabled state", extensionName, name);
            return "Extension is disabled";
        }
        logger.error("Extension: {} connected to host: {} is not having path in Ready state", extensionName, name);
        return "Extension is not ready";
    }

    private Answer execute(ExtensionRoutingUpdateCommand cmd) {
        if (StringUtils.isNotBlank(extensionName) && !extensionName.equals(cmd.getExtensionName())) {
            return new Answer(cmd, false, "Not same extension");
        }
        if (cmd.isRemoved()) {
            extensionName = null;
            extensionRelativePath = null;
            extensionState = Extension.State.Disabled;
            return new Answer(cmd);
        }
        extensionName = cmd.getExtensionName();
        extensionRelativePath = cmd.getExtensionRelativePath();
        extensionState = cmd.getExtensionState();
        return new Answer(cmd);
    }

    private Answer execute(PingTestCommand cmd) {
        return new Answer(cmd);
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer execute(CheckHealthCommand cmd) {
        if (isExtensionDisconnected()) {
            logAndGetExtensionNotConnectedOrDisabledError();
        }
        return new CheckHealthAnswer(cmd, !isExtensionDisconnected());
    }

    private Answer execute(CheckNetworkCommand cmd) {
        return new CheckNetworkAnswer(cmd, true, "Network Setup check by names is done");
    }

    private Answer execute(CleanupNetworkRulesCmd cmd) {
        if (isExtensionDisconnected()) {
            return new Answer(cmd, false, logAndGetExtensionNotConnectedOrDisabledError());
        }
        return new Answer(cmd, false, "Not supported");
    }

    private Answer execute(GetVmStatsCommand cmd) {
        if (isExtensionDisconnected()) {
            return new Answer(cmd, false, logAndGetExtensionNotConnectedOrDisabledError());
        }
        return new Answer(cmd, false, "Not supported");
    }

    private MaintainAnswer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd, false);
    }

    public GetHostStatsAnswer execute(GetHostStatsCommand cmd) {
        if (isExtensionDisconnected()) {
            logAndGetExtensionNotConnectedOrDisabledError();
        }
        return new GetHostStatsAnswer(cmd, null);
    }

    public StartAnswer execute(StartCommand cmd) {
        if (isExtensionDisconnected() || isExtensionNotEnabled() || isExtensionPathNotReady()) {
            return new StartAnswer(cmd, logAndGetExtensionNotConnectedOrDisabledError());
        }
        return externalProvisioner.startInstance(guid, extensionName, extensionRelativePath, cmd);
    }

    public StopAnswer execute(StopCommand cmd) {
        if (isExtensionDisconnected() || isExtensionNotEnabled() || isExtensionPathNotReady()) {
            return new StopAnswer(cmd, logAndGetExtensionNotConnectedOrDisabledError(), false);
        }
        if (cmd.isExpungeVM()) {
            return externalProvisioner.expungeInstance(guid, extensionName, extensionRelativePath, cmd);
        }
        return externalProvisioner.stopInstance(guid, extensionName, extensionRelativePath, cmd);
    }

    public RebootAnswer execute(RebootCommand cmd) {
        if (isExtensionDisconnected() || isExtensionNotEnabled() || isExtensionPathNotReady()) {
            return new RebootAnswer(cmd, logAndGetExtensionNotConnectedOrDisabledError(), false);
        }
        return externalProvisioner.rebootInstance(guid, extensionName, extensionRelativePath, cmd);
    }

    public GetExternalConsoleAnswer execute(GetExternalConsoleCommand cmd) {
        if (isExtensionDisconnected() || isExtensionNotEnabled() || isExtensionPathNotReady()) {
            return new GetExternalConsoleAnswer(cmd, logAndGetExtensionNotConnectedOrDisabledError());
        }
        return externalProvisioner.getInstanceConsole(guid, extensionName, extensionRelativePath, cmd);
    }

    public PrepareExternalProvisioningAnswer execute(PrepareExternalProvisioningCommand cmd) {
        if (isExtensionDisconnected() || isExtensionNotEnabled() || isExtensionPathNotReady()) {
            return new PrepareExternalProvisioningAnswer(cmd, false, logAndGetExtensionNotConnectedOrDisabledError());
        }
        return externalProvisioner.prepareExternalProvisioning(guid, extensionName, extensionRelativePath, cmd);
    }

    public RunCustomActionAnswer execute(RunCustomActionCommand cmd) {
        if (isExtensionDisconnected() || isExtensionNotEnabled() || isExtensionPathNotReady()) {
            return new RunCustomActionAnswer(cmd, false, logAndGetExtensionNotConnectedOrDisabledError());
        }
        return externalProvisioner.runCustomAction(guid, extensionName, extensionRelativePath, cmd);
    }

    public Answer execute(Command cmd) {
        if (isExtensionDisconnected() || isExtensionNotEnabled() || isExtensionPathNotReady()) {
            return new Answer(cmd, false, logAndGetExtensionNotConnectedOrDisabledError());
        }
        RunCustomActionCommand runCustomActionCommand = new RunCustomActionCommand(cmd.toString());
        RunCustomActionAnswer customActionAnswer = externalProvisioner.runCustomAction(guid, extensionName,
                extensionRelativePath, runCustomActionCommand);
        return new Answer(cmd, customActionAnswer.getResult(), customActionAnswer.getDetails());
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
        try {
            externalProvisioner = ComponentContext.getDelegateComponentOfType(ExternalProvisioner.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new ConfigurationException(
                    String.format("Unable to find an ExternalProvisioner for the external resource: %s", name)
            );
        }
        externalProvisioner.configure(name, params);
        dcId = (String)params.get("zone");
        pod = (String)params.get("pod");
        cluster = (String)params.get("cluster");
        this.name = name;
        guid = (String)params.get("guid");
        extensionName = (String)params.get("extensionName");
        extensionRelativePath = (String)params.get("extensionRelativePath");
        extensionState = (Extension.State)params.get("extensionState");
        extensionPathReady = (boolean)params.get("extensionPathReady");
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
