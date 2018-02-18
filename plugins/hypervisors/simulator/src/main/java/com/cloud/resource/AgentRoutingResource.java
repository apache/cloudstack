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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingWithNwGroupsCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.ShutdownCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.SimulatorManager;
import com.cloud.agent.manager.SimulatorManager.AgentType;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.serializer.GsonHelper;
import com.cloud.simulator.MockConfigurationVO;
import com.cloud.simulator.MockVMVO;
import com.cloud.storage.Storage.StorageResourceType;
import com.cloud.storage.template.TemplateProp;
import com.cloud.utils.Pair;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.vm.VirtualMachine.PowerState;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class AgentRoutingResource extends AgentStorageResource {
    private static final Logger s_logger = Logger.getLogger(AgentRoutingResource.class);
    private static final Gson s_gson = GsonHelper.getGson();

    private Map<String, Pair<Long, Long>> _runningVms = new HashMap<String, Pair<Long, Long>>();
    long usedCpu = 0;
    long usedMem = 0;
    long totalCpu;
    long totalMem;
    protected String _mountParent;

    public AgentRoutingResource(long instanceId, AgentType agentType, SimulatorManager simMgr, String hostGuid) {
        super(instanceId, agentType, simMgr, hostGuid);
    }

    public AgentRoutingResource() {
        setType(Host.Type.Routing);
    }

    @Override
    public Answer executeRequestInContext(Command cmd) {
        try {
            if (cmd instanceof StartCommand) {
                return execute((StartCommand)cmd);
            } else if (cmd instanceof StopCommand) {
                return execute((StopCommand)cmd);
            } else if (cmd instanceof CheckVirtualMachineCommand) {
                return execute((CheckVirtualMachineCommand)cmd);
            } else if (cmd instanceof ReadyCommand) {
                return new ReadyAnswer((ReadyCommand)cmd);
            } else if (cmd instanceof ShutdownCommand) {
                return execute((ShutdownCommand)cmd);
            } else {
                return _simMgr.simulate(cmd, hostGuid);
            }
        } catch (IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }

    @Override
    public Type getType() {
        return Host.Type.Routing;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            MockConfigurationVO config = _simMgr.getMockConfigurationDao().findByNameBottomUP(agentHost.getDataCenterId(), agentHost.getPodId(), agentHost.getClusterId(), agentHost.getId(), "PingCommand");
            if (config != null) {
                Map<String, String> configParameters = config.getParameters();
                for (Map.Entry<String, String> entry : configParameters.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("result")) {
                        String value = entry.getValue();
                        if (value.equalsIgnoreCase("fail")) {
                            return null;
                        }
                    }
                }
            }

            config = _simMgr.getMockConfigurationDao().findByNameBottomUP(agentHost.getDataCenterId(), agentHost.getPodId(), agentHost.getClusterId(), agentHost.getId(), "PingRoutingWithNwGroupsCommand");
            if (config != null) {
                String message = config.getJsonResponse();
                if (message != null) {
                    // json response looks like {"<Type>":....}
                    String objectType = message.split(":")[0].substring(2).replace("\"", "");
                    String objectData = message.substring(message.indexOf(':') + 1, message.length() - 1);
                    if (objectType != null) {
                        Class<?> clz = null;
                        try {
                            clz = Class.forName(objectType);
                        } catch (ClassNotFoundException e) {
                            s_logger.info("[ignored] ping returned class", e);
                        }
                        if (clz != null) {
                            StringReader reader = new StringReader(objectData);
                            JsonReader jsonReader = new JsonReader(reader);
                            jsonReader.setLenient(true);
                            return (PingCommand)s_gson.fromJson(jsonReader, clz);
                        }
                    }
                }
            }
        } catch (Exception e) {
            txn.rollback();
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        if (isStopped()) {
            return null;
        }
        HashMap<String, Pair<Long, Long>> nwGrpStates = _simMgr.syncNetworkGroups(hostGuid);
        return new PingRoutingWithNwGroupsCommand(getType(), id, getHostVmStateReport(), nwGrpStates);
    }

    @Override
    public StartupCommand[] initialize() {
        Map<String, MockVMVO> vmsMaps = _simMgr.getVms(this.hostGuid);
        totalCpu = agentHost.getCpuCount() * agentHost.getCpuSpeed();
        totalMem = agentHost.getMemorySize();
        for (Map.Entry<String, MockVMVO> entry : vmsMaps.entrySet()) {
            MockVMVO vm = entry.getValue();
            usedCpu += vm.getCpu();
            usedMem += vm.getMemory();
            _runningVms.put(entry.getKey(), new Pair<Long, Long>(Long.valueOf(vm.getCpu()), vm.getMemory()));
        }

        List<Object> info = getHostInfo();

        StartupRoutingCommand cmd =
            new StartupRoutingCommand((Integer)info.get(0), (Long)info.get(1), (Long)info.get(2), (Long)info.get(4), (String)info.get(3), HypervisorType.Simulator,
                RouterPrivateIpStrategy.HostLocal);

        Map<String, String> hostDetails = new HashMap<String, String>();
        hostDetails.put(RouterPrivateIpStrategy.class.getCanonicalName(), RouterPrivateIpStrategy.DcGlobal.toString());

        cmd.setHostDetails(hostDetails);
        cmd.setAgentTag("agent-simulator");
        cmd.setPrivateIpAddress(agentHost.getPrivateIpAddress());
        cmd.setPrivateNetmask(agentHost.getPrivateNetMask());
        cmd.setPrivateMacAddress(agentHost.getPrivateMacAddress());
        cmd.setStorageIpAddress(agentHost.getStorageIpAddress());
        cmd.setStorageNetmask(agentHost.getStorageNetMask());
        cmd.setStorageMacAddress(agentHost.getStorageMacAddress());
        cmd.setStorageIpAddressDeux(agentHost.getStorageIpAddress());
        cmd.setStorageNetmaskDeux(agentHost.getStorageNetMask());
        cmd.setStorageMacAddressDeux(agentHost.getStorageIpAddress());

        cmd.setName(agentHost.getName());
        cmd.setGuid(agentHost.getGuid());
        cmd.setVersion(agentHost.getVersion());
        cmd.setAgentTag("agent-simulator");
        cmd.setDataCenter(String.valueOf(agentHost.getDataCenterId()));
        cmd.setPod(String.valueOf(agentHost.getPodId()));
        cmd.setCluster(String.valueOf(agentHost.getClusterId()));

        StartupStorageCommand ssCmd = initializeLocalSR();

        return new StartupCommand[] {cmd, ssCmd};
    }

    private StartupStorageCommand initializeLocalSR() {
        Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();

        StoragePoolInfo poolInfo = _simMgr.getLocalStorage(hostGuid);

        StartupStorageCommand cmd = new StartupStorageCommand(poolInfo.getHostPath(), poolInfo.getPoolType(), poolInfo.getCapacityBytes(), tInfo);

        cmd.setPoolInfo(poolInfo);
        cmd.setGuid(agentHost.getGuid());
        cmd.setResourceType(StorageResourceType.STORAGE_POOL);
        return cmd;
    }

    protected synchronized Answer execute(StartCommand cmd) throws IllegalArgumentException {
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        String vmName = vmSpec.getName();
        if (this.totalCpu < (vmSpec.getCpus() * vmSpec.getMaxSpeed() + this.usedCpu) || this.totalMem < (vmSpec.getMaxRam() + this.usedMem)) {
            return new StartAnswer(cmd, "Not enough resource to start the vm");
        }
        Answer result = _simMgr.simulate(cmd, hostGuid);
        if (!result.getResult()) {
            return new StartAnswer(cmd, result.getDetails());
        }

        this.usedCpu += vmSpec.getCpus() * vmSpec.getMaxSpeed();
        this.usedMem += vmSpec.getMaxRam();
        _runningVms.put(vmName, new Pair<Long, Long>(Long.valueOf(vmSpec.getCpus() * vmSpec.getMaxSpeed()), vmSpec.getMaxRam()));


        return new StartAnswer(cmd);

    }

    protected synchronized StopAnswer execute(StopCommand cmd) {

        StopAnswer answer = null;
        String vmName = cmd.getVmName();

        Answer result = _simMgr.simulate(cmd, hostGuid);

        if (!result.getResult()) {
            return new StopAnswer(cmd, result.getDetails(), false);
        }

        answer = new StopAnswer(cmd, null, true);
        Pair<Long, Long> data = _runningVms.get(vmName);
        if (data != null) {
            this.usedCpu -= data.first();
            this.usedMem -= data.second();
         }


        return answer;
    }

    protected CheckVirtualMachineAnswer execute(final CheckVirtualMachineCommand cmd) {
        final String vmName = cmd.getVmName();
        CheckVirtualMachineAnswer result = (CheckVirtualMachineAnswer)_simMgr.simulate(cmd, hostGuid);
        return result;
    }

    protected List<Object> getHostInfo() {
        ArrayList<Object> info = new ArrayList<Object>();
        long speed = agentHost.getCpuSpeed();
        long cpus = agentHost.getCpuCount();
        long ram = agentHost.getMemorySize();
        long dom0Ram = agentHost.getMemorySize() / 10;

        info.add((int)cpus);
        info.add(speed);
        info.add(ram);
        info.add(agentHost.getCapabilities());
        info.add(dom0Ram);

        return info;
    }

    protected HashMap<String, HostVmStateReportEntry> getHostVmStateReport() {
        HashMap<String, HostVmStateReportEntry> report = new HashMap<String, HostVmStateReportEntry>();
        Map<String, PowerState> states = _simMgr.getVmStates(this.hostGuid);
        for (String vmName : states.keySet()) {
            report.put(vmName, new HostVmStateReportEntry(states.get(vmName), agentHost.getName()));
        }
        return report;
    }

    private Answer execute(ShutdownCommand cmd) {
        this.stopped = true;
        return new Answer(cmd);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (!super.configure(name, params)) {
            s_logger.warn("Base class was unable to configure");
            return false;
        }
        return true;
    }
}
