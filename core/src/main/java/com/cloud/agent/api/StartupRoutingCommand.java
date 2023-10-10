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

package com.cloud.agent.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.RouterPrivateIpStrategy;

public class StartupRoutingCommand extends StartupCommand {
    Integer cpuSockets;
    int cpus;
    long speed;
    long memory;
    long dom0MinMemory;
    boolean poolSync;
    private boolean supportsClonedVolumes;

    String caps;
    String pool;
    HypervisorType hypervisorType;
    Map<String, String> hostDetails; //stuff like host os, cpu capabilities
    List<String> hostTags = new ArrayList<String>();
    String hypervisorVersion;
    HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = new HashMap<String, HashMap<String, VgpuTypesInfo>>();
    private Boolean hostHealthCheckResult;

    public StartupRoutingCommand() {
        super(Host.Type.Routing);
        hostDetails = new HashMap<String, String>();
        getHostDetails().put(RouterPrivateIpStrategy.class.getCanonicalName(), RouterPrivateIpStrategy.DcGlobal.toString());

    }

    public StartupRoutingCommand(int cpus, long speed, long memory, long dom0MinMemory, final String caps, final HypervisorType hypervisorType,
            final Map<String, String> hostDetails) {
        super(Host.Type.Routing);
        this.cpus = cpus;
        this.speed = speed;
        this.memory = memory;
        this.dom0MinMemory = dom0MinMemory;
        this.hypervisorType = hypervisorType;
        this.hostDetails = hostDetails;
        this.caps = caps;
        this.poolSync = false;
    }

    public StartupRoutingCommand(int cpus, long speed, long memory, long dom0MinMemory, String caps, HypervisorType hypervisorType,
            RouterPrivateIpStrategy privIpStrategy) {
        this(cpus, speed, memory, dom0MinMemory, caps, hypervisorType);
        getHostDetails().put(RouterPrivateIpStrategy.class.getCanonicalName(), privIpStrategy.toString());
    }


    public StartupRoutingCommand(int cpus2, long speed2, long memory2, long dom0MinMemory2, String caps2, HypervisorType hypervisorType2) {
        this(cpus2, speed2, memory2, dom0MinMemory2, caps2, hypervisorType2, new HashMap<String, String>());
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

    public List<String> getHostTags() {
        return hostTags;
    }

    public void setHostTags(String hostTag) {
        this.hostTags.add(hostTag);
    }

    public  HashMap<String, HashMap<String, VgpuTypesInfo>> getGpuGroupDetails() {
        return groupDetails;
    }

    public void setGpuGroupDetails(HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails) {
        this.groupDetails = groupDetails;
    }

    public boolean getSupportsClonedVolumes() {
        return supportsClonedVolumes;
    }

    public void setSupportsClonedVolumes(boolean supportsClonedVolumes) {
        this.supportsClonedVolumes = supportsClonedVolumes;
    }

    public Boolean getHostHealthCheckResult() {
        return hostHealthCheckResult;
    }

    public void setHostHealthCheckResult(Boolean hostHealthCheckResult) {
        this.hostHealthCheckResult = hostHealthCheckResult;
    }
}
