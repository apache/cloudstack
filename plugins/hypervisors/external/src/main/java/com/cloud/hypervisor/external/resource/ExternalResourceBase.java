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
package com.cloud.hypervisor.external.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.PrepareExternalProvisioningAnswer;
import com.cloud.agent.api.PrepareExternalProvisioningCommand;
import com.cloud.agent.api.PostExternalProvisioningCommand;
import com.cloud.agent.api.PostExternalProvisioningAnswer;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.manager.ExternalAgentManager;
import com.cloud.agent.manager.ExternalAgentManagerImpl;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.network.Networks;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalResourceBase implements ServerResource {

    private static final Logger logger = Logger.getLogger(ExternalResourceBase.class);

    @Inject
    ExternalAgentManager _externalAgentMgr = null;

    @Inject
    UserVmDao _uservmDao;

    protected String _url;
    protected String _dcId;
    protected String _pod;
    protected String _cluster;
    protected String _guid;
    private Host.Type _type;
    private ExternalProvisioner externalProvisioner = null;

    public ExternalResourceBase() {
        setType(Host.Type.Routing);
    }

    @Override
    public Host.Type getType() {
        return null;
    }

    public void setType(Host.Type type) {
        this._type = type;
    }

    @Override
    public StartupCommand[] initialize() {
        final List<Object> info = getHostInfo();

        final StartupRoutingCommand cmd =
                new StartupRoutingCommand((Integer)info.get(0), (Long)info.get(1), (Long)info.get(2), (Long)info.get(4), (String)info.get(3), Hypervisor.HypervisorType.External,
                        Networks.RouterPrivateIpStrategy.HostLocal);
        cmd.setDataCenter(_dcId);
        cmd.setPod(_pod);
        cmd.setCluster(_cluster);
        cmd.setHostType(Host.Type.Routing);
        cmd.setName(_guid);
        cmd.setPrivateIpAddress(Hypervisor.HypervisorType.External.toString());
        cmd.setGuid(_guid);
        cmd.setIqn(_guid);
        cmd.setVersion(ExternalResourceBase.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {cmd};
    }

    protected List<Object> getHostInfo() {
        final ArrayList<Object> info = new ArrayList<Object>();
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
        final HashMap<String, HostVmStateReportEntry> vmStates = externalProvisioner.getHostVmStateReport(id);

        return new PingRoutingCommand(Host.Type.Routing, id, vmStates);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        try {
            if (cmd instanceof CheckNetworkCommand) {
                return new CheckNetworkAnswer((CheckNetworkCommand) cmd, true, "Network Setup check by names is done");
            } else if (cmd instanceof ReadyCommand) {
                return new ReadyAnswer((ReadyCommand) cmd);
            } else if (cmd instanceof StartCommand) {
                return execute((StartCommand) cmd);
            } else if (cmd instanceof StopCommand) {
                return execute((StopCommand) cmd);
            } else if (cmd instanceof RebootCommand) {
                return execute((RebootCommand) cmd);
            } else if (cmd instanceof PingTestCommand) {
                return new Answer(cmd);
            } else if (cmd instanceof PrepareExternalProvisioningCommand) {
                return execute((PrepareExternalProvisioningCommand) cmd);
            } else if (cmd instanceof GetHostStatsCommand) {
                return execute((GetHostStatsCommand) cmd);
            } else if (cmd instanceof PingTestCommand) {
                return execute((PingTestCommand) cmd);
            } else if (cmd instanceof MaintainCommand) {
                return execute((MaintainCommand) cmd);
            }  else {
                return null;
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

    public StopAnswer execute(StopCommand cmd) {
        Boolean expungeVM = cmd.isExpungeVM();
        if (expungeVM != null && expungeVM) {
            return externalProvisioner.expungeInstance(cmd);
        }
        return externalProvisioner.stopInstance(cmd);
    }

    public RebootAnswer execute(RebootCommand cmd) {
        return externalProvisioner.rebootInstance(cmd);
    }

    public PostExternalProvisioningAnswer execute(PostExternalProvisioningCommand cmd) {
        return externalProvisioner.postsetupInstance(cmd);
    }

    public GetHostStatsAnswer execute(GetHostStatsCommand cmd) {
        return new GetHostStatsAnswer(cmd, null);
    }

    public PrepareExternalProvisioningAnswer execute(PrepareExternalProvisioningCommand cmd) {
        return externalProvisioner.prepareExternalProvisioning(cmd);
    }

    public StartAnswer execute(StartCommand cmd) {
        return externalProvisioner.startInstance(cmd);
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
        _externalAgentMgr = ComponentContext.inject(ExternalAgentManagerImpl.class);
        _externalAgentMgr.configure(name, params);

        String provisionerName = (String) params.get(ApiConstants.EXTERNAL_PROVISIONER);
        externalProvisioner = _externalAgentMgr.getExternalProvisioner(provisionerName);

        _dcId = (String) params.get("zone");
        _pod = (String) params.get("pod");
        _cluster = (String) params.get("cluster");
        _guid = (String) params.get("guid");
        _url = (String) params.get("guid");

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
