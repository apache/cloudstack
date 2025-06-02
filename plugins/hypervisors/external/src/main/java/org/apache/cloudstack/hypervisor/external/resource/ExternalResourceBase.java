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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.agent.manager.ExternalAgentManager;
import org.apache.cloudstack.agent.manager.ExternalAgentManagerImpl;
import org.apache.cloudstack.hypervisor.external.provisioner.simpleprovisioner.SimpleExternalProvisioner;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
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
import com.cloud.utils.component.ComponentContext;

public class ExternalResourceBase implements ServerResource {

    @Inject
    ExternalAgentManager externalAgentManager = null;
    @Inject
    ExternalProvisioner externalProvisioner;
    protected String url;
    protected String dcId;
    protected String pod;
    protected String cluster;
    protected String guid;
    private Host.Type type;

    private String extensionName;

    public ExternalResourceBase() {
        setType(Host.Type.Routing);
    }

    @Override
    public Host.Type getType() {
        return type;
    }

    public void setType(Host.Type type) {
        this.type = type;
    }

    @Override
    public StartupCommand[] initialize() {
        final List<Object> info = getHostInfo();

        final StartupRoutingCommand cmd =
                new StartupRoutingCommand((Integer)info.get(0), (Long)info.get(1), (Long)info.get(2), (Long)info.get(4), (String)info.get(3), Hypervisor.HypervisorType.External,
                        Networks.RouterPrivateIpStrategy.HostLocal);
        cmd.setDataCenter(dcId);
        cmd.setPod(pod);
        cmd.setCluster(cluster);
        cmd.setHostType(Host.Type.Routing);
        cmd.setName(guid);
        cmd.setPrivateIpAddress(Hypervisor.HypervisorType.External.toString());
        cmd.setGuid(guid);
        cmd.setIqn(guid);
        cmd.setVersion(ExternalResourceBase.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {cmd};
    }

    protected List<Object> getHostInfo() {
        final ArrayList<Object> info = new ArrayList<>();
        long speed = 4000L;
        long cpus = 4L;
        long ram = 16000L * 1024L * 1024L;
        long dom0ram = Math.min(ram / 10, 768 * 1024 * 1024L);

        String cap = "hvm";
        info.add((int)cpus);
        info.add(speed);
        info.add(ram);
        info.add(cap);
        info.add(dom0ram);
        return info;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        final Map<String, HostVmStateReportEntry> vmStates = externalProvisioner.getHostVmStateReport(extensionName, id);
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
        return externalProvisioner.runCustomAction(extensionName, cmd);
    }

    public Answer execute(Command cmd) {
        RunCustomActionCommand runCustomActionCommand = new RunCustomActionCommand(cmd.toString(), new HashMap<>(),
                new HashMap<>());
        RunCustomActionAnswer customActionAnswer = externalProvisioner.runCustomAction(extensionName,
                runCustomActionCommand);
        return new Answer(cmd, customActionAnswer.getResult(), customActionAnswer.getRunDetails().toString());
    }

    public StopAnswer execute(StopCommand cmd) {
        if (cmd.isExpungeVM()) {
            return externalProvisioner.expungeInstance(extensionName, cmd);
        }
        return externalProvisioner.stopInstance(extensionName, cmd);
    }

    public RebootAnswer execute(RebootCommand cmd) {
        return externalProvisioner.rebootInstance(extensionName, cmd);
    }

    public PostExternalProvisioningAnswer execute(PostExternalProvisioningCommand cmd) {
        return externalProvisioner.postSetupInstance(extensionName, cmd);
    }

    public GetHostStatsAnswer execute(GetHostStatsCommand cmd) {
        return new GetHostStatsAnswer(cmd, null);
    }

    public PrepareExternalProvisioningAnswer execute(PrepareExternalProvisioningCommand cmd) {
        return externalProvisioner.prepareExternalProvisioning(extensionName, cmd);
    }

    public StartAnswer execute(StartCommand cmd) {
        return externalProvisioner.startInstance(extensionName, cmd);
    }

    private Answer execute(CheckHealthCommand cmd) {
        return externalProvisioner.checkHealth(extensionName, cmd);
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
        externalAgentManager = ComponentContext.inject(ExternalAgentManagerImpl.class);
        externalAgentManager.configure(name, params);

        dcId = (String) params.get("zone");
        pod = (String) params.get("pod");
        cluster = (String) params.get("cluster");
        guid = (String) params.get("guid");
        url = (String) params.get("guid");
        extensionName = (String) params.get("extensionName");

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
