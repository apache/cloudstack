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
package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine.State;

public class StartupRoutingCommand extends StartupCommand {
    public static class VmState {
        State state;
        String host;

        public VmState() {
        }

        public VmState(State state, String host) {
            this.state = state;
            this.host = host;
        }

        public State getState() {
            return state;
        }

        public String getHost() {
            return host;
        }
    }

    Integer cpuSockets;
    int cpus;
    long speed;
    long memory;
    long dom0MinMemory;
    boolean poolSync;

    // VM power state report is added in a side-by-side way as old VM state report
    // this is to allow a graceful migration from the old VM state sync model to the new model
    //
    // side-by-side addition of power state sync
    Map<String, HostVmStateReportEntry> _hostVmStateReport;

    // TODO vmsync
    // deprecated, will delete after full replacement
    Map<String, VmState> vms;
    HashMap<String, Pair<String, State>> _clusterVMStates;

    String caps;
    String pool;
    HypervisorType hypervisorType;
    Map<String, String> hostDetails; //stuff like host os, cpu capabilities
    String hypervisorVersion;
    HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = new HashMap<String, HashMap<String, VgpuTypesInfo>>();

    public StartupRoutingCommand() {
        super(Host.Type.Routing);
        hostDetails = new HashMap<String, String>();
        getHostDetails().put(RouterPrivateIpStrategy.class.getCanonicalName(), RouterPrivateIpStrategy.DcGlobal.toString());

    }

    public StartupRoutingCommand(int cpus, long speed, long memory, long dom0MinMemory, String caps, HypervisorType hypervisorType,
            RouterPrivateIpStrategy privIpStrategy, Map<String, VmState> vms, Map<String, HostVmStateReportEntry> hostVmStateReport) {
        this(cpus, speed, memory, dom0MinMemory, caps, hypervisorType, vms, hostVmStateReport);
        getHostDetails().put(RouterPrivateIpStrategy.class.getCanonicalName(), privIpStrategy.toString());
    }

    public StartupRoutingCommand(int cpus, long speed, long memory, long dom0MinMemory, String caps, HypervisorType hypervisorType, RouterPrivateIpStrategy privIpStrategy) {
        this(cpus,
            speed,
            memory,
            dom0MinMemory,
            caps,
            hypervisorType,
            new HashMap<String, String>(),
            new HashMap<String, VmState>(),
            new HashMap<String, HostVmStateReportEntry>());

        getHostDetails().put(RouterPrivateIpStrategy.class.getCanonicalName(), privIpStrategy.toString());
    }

    public StartupRoutingCommand(int cpus, long speed, long memory, long dom0MinMemory, final String caps, final HypervisorType hypervisorType,
            final Map<String, String> hostDetails, Map<String, VmState> vms, Map<String, HostVmStateReportEntry> hostVmStateReport) {
        super(Host.Type.Routing);
        this.cpus = cpus;
        this.speed = speed;
        this.memory = memory;
        this.dom0MinMemory = dom0MinMemory;
        this.vms = vms;
        this._hostVmStateReport = hostVmStateReport;
        this.hypervisorType = hypervisorType;
        this.hostDetails = hostDetails;
        this.caps = caps;
        this.poolSync = false;
    }

    public StartupRoutingCommand(int cpus2, long speed2, long memory2, long dom0MinMemory2, String caps2, HypervisorType hypervisorType2, Map<String, VmState> vms2,
            Map<String, HostVmStateReportEntry> hostVmStateReport) {
        this(cpus2, speed2, memory2, dom0MinMemory2, caps2, hypervisorType2, new HashMap<String, String>(), vms2, hostVmStateReport);
    }

    public StartupRoutingCommand(int cpus, long speed, long memory, long dom0MinMemory, final String caps, final HypervisorType hypervisorType,
            final Map<String, String> hostDetails, Map<String, VmState> vms, Map<String, HostVmStateReportEntry> vmPowerStates, String hypervisorVersion) {
        this(cpus, speed, memory, dom0MinMemory, caps, hypervisorType, hostDetails, vms, vmPowerStates);
        this.hypervisorVersion = hypervisorVersion;
    }

    public void setChanges(Map<String, VmState> vms) {
        this.vms = vms;
    }

    public void setStateChanges(Map<String, State> vms) {
        for (String vm_name : vms.keySet()) {
            if (this.vms == null) {
                this.vms = new HashMap<String, VmState>();
            }
            this.vms.put(vm_name, new VmState(vms.get(vm_name), null));
        }
    }

    public void setClusterVMStateChanges(HashMap<String, Pair<String, State>> allStates) {
        _clusterVMStates = allStates;
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public int getCpus() {
        return cpus;
    }

    public String getCapabilities() {
        return caps;
    }

    public long getSpeed() {
        return speed;
    }

    public long getMemory() {
        return memory;
    }

    public long getDom0MinMemory() {
        return dom0MinMemory;
    }

    public Map<String, VmState> getVmStates() {
        return vms;
    }

    public HashMap<String, Pair<String, State>> getClusterVMStateChanges() {
        return _clusterVMStates;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public void setCpuSockets(Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public void setCpus(int cpus) {
        this.cpus = cpus;
    }

    public void setMemory(long memory) {
        this.memory = memory;
    }

    public void setDom0MinMemory(long dom0MinMemory) {
        this.dom0MinMemory = dom0MinMemory;
    }

    public void setCaps(String caps) {
        this.caps = caps;
    }

    public String getPool() {
        return pool;
    }

    public void setPool(String pool) {
        this.pool = pool;
    }

    public boolean isPoolSync() {
        return poolSync;
    }

    public void setPoolSync(boolean poolSync) {
        this.poolSync = poolSync;
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public Map<String, String> getHostDetails() {
        return hostDetails;
    }

    public void setHostDetails(Map<String, String> hostDetails) {
        this.hostDetails = hostDetails;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    public Map<String, HostVmStateReportEntry> getHostVmStateReport() {
        return this._hostVmStateReport;
    }

    public void setHostVmStateReport(Map<String, HostVmStateReportEntry> hostVmStateReport) {
        this._hostVmStateReport = hostVmStateReport;
    }

    public  HashMap<String, HashMap<String, VgpuTypesInfo>> getGpuGroupDetails() {
        return groupDetails;
    }

    public void setGpuGroupDetails(HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails) {
        this.groupDetails = groupDetails;
    }
}
