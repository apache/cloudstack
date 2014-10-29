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
package com.cloud.agent.mockvm;

import java.util.Map;
import java.util.Set;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.vm.VirtualMachine.State;

public interface VmMgr {
    public Set<String> getCurrentVMs();

    public String startVM(String vmName, String vnetId, String gateway, String dns, String privateIP, String privateMac, String privateMask, String publicIP,
        String publicMac, String publicMask, int cpuCount, int cpuUtilization, long ramSize, String localPath, String vncPassword);

    public String stopVM(String vmName, boolean force);

    public String rebootVM(String vmName);

    public void cleanupVM(String vmName, String local, String vnet);

    public boolean migrate(String vmName, String params);

    public MockVm getVm(String vmName);

    public State checkVmState(String vmName);

    public Map<String, State> getVmStates();

    public Integer getVncPort(String name);

    public String cleanupVnet(String vnetId);

    public double getHostCpuUtilization();

    public int getHostCpuCount();

    public long getHostCpuSpeed();

    public long getHostTotalMemory();

    public long getHostFreeMemory();

    public long getHostDom0Memory();

    public MockVm createVmFromSpec(VirtualMachineTO vmSpec);

    public void createVbd(VirtualMachineTO vmSpec, String vmName, MockVm vm);

    public void createVif(VirtualMachineTO vmSpec, String vmName, MockVm vm);

    public void configure(Map<String, Object> params);
}
