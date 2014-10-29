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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;

public class MockVmMgr implements VmMgr {
    private static final Logger s_logger = Logger.getLogger(MockVmMgr.class);

    private static final int DEFAULT_DOM0_MEM_MB = 128;
    private static final Random randSeed = new Random();

    private final Map<String, MockVm> vms = new HashMap<String, MockVm>();
    private long vncPortMap = 0;

    private Map<String, Object> _params = null;

    public MockVmMgr() {
    }

    @Override
    public Set<String> getCurrentVMs() {
        HashSet<String> vmNameSet = new HashSet<String>();
        synchronized (this) {
            for (String vmName : vms.keySet())
                vmNameSet.add(vmName);
        }
        return vmNameSet;
    }

    @Override
    public String startVM(String vmName, String vnetId, String gateway, String dns, String privateIP, String privateMac, String privateMask, String publicIP,
        String publicMac, String publicMask, int cpuCount, int cpuUtilization, long ramSize, String localPath, String vncPassword) {

        if (s_logger.isInfoEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Start VM. name: " + vmName + ", vnet: " + vnetId + ", dns: " + dns);
            sb.append(", privateIP: " + privateIP + ", privateMac: " + privateMac + ", privateMask: " + privateMask);
            sb.append(", publicIP: " + publicIP + ", publicMac: " + publicMac + ", publicMask: " + publicMask);
            sb.append(", cpu count: " + cpuCount + ", cpuUtilization: " + cpuUtilization + ", ram : " + ramSize);
            sb.append(", localPath: " + localPath);
            s_logger.info(sb.toString());
        }

        synchronized (this) {
            MockVm vm = vms.get(vmName);
            if (vm == null) {
                if (ramSize > getHostFreeMemory())
                    return "Out of memory";

                int vncPort = allocVncPort();
                if (vncPort < 0)
                    return "Unable to allocate VNC port";

                vm = new MockVm(vmName, State.Running, ramSize, cpuCount, cpuUtilization, vncPort);
                vms.put(vmName, vm);
            }
        }

        return null;
    }

    @Override
    public String stopVM(String vmName, boolean force) {
        if (s_logger.isInfoEnabled())
            s_logger.info("Stop VM. name: " + vmName);

        synchronized (this) {
            MockVm vm = vms.get(vmName);
            if (vm != null) {
                vm.setState(State.Stopped);
                freeVncPort(vm.getVncPort());
            }
        }

        return null;
    }

    @Override
    public String rebootVM(String vmName) {
        if (s_logger.isInfoEnabled())
            s_logger.info("Reboot VM. name: " + vmName);

        synchronized (this) {
            MockVm vm = vms.get(vmName);
            if (vm != null)
                vm.setState(State.Running);
        }
        return null;
    }

    @Override
    public boolean migrate(String vmName, String params) {
        if (s_logger.isInfoEnabled())
            s_logger.info("Migrate VM. name: " + vmName);

        synchronized (this) {
            MockVm vm = vms.get(vmName);
            if (vm != null) {
                vm.setState(State.Stopped);
                freeVncPort(vm.getVncPort());

                vms.remove(vmName);
                return true;
            }
        }

        return false;
    }

    @Override
    public MockVm getVm(String vmName) {
        synchronized (this) {
            MockVm vm = vms.get(vmName);
            return vm;
        }
    }

    @Override
    public State checkVmState(String vmName) {

        synchronized (this) {
            MockVm vm = vms.get(vmName);
            if (vm != null)
                return vm.getState();
        }
        return State.Unknown;
    }

    @Override
    public Map<String, State> getVmStates() {
        Map<String, State> states = new HashMap<String, State>();

        synchronized (this) {
            for (MockVm vm : vms.values()) {
                states.put(vm.getName(), vm.getState());
            }
        }
        return states;
    }

    @Override
    public void cleanupVM(String vmName, String local, String vnet) {
        synchronized (this) {
            MockVm vm = vms.get(vmName);
            if (vm != null) {
                freeVncPort(vm.getVncPort());
            }
            vms.remove(vmName);
        }
    }

    @Override
    public double getHostCpuUtilization() {
        return 0.0d;
    }

    @Override
    public int getHostCpuCount() {

        return getConfiguredProperty("cpus", 4);
    }

    @Override
    public long getHostCpuSpeed() {
        return getConfiguredProperty("cpuspeed", 4000L);

    }

    @Override
    public long getHostTotalMemory() { // total memory in bytes
        return getConfiguredProperty("memory", 16000L);

    }

    @Override
    public long getHostFreeMemory() { // free memory in bytes
        long memSize = getHostTotalMemory();
        memSize -= getHostDom0Memory();

        synchronized (this) {
            for (MockVm vm : vms.values()) {
                if (vm.getState() != State.Stopped)
                    memSize -= vm.getRamSize();
            }
        }

        return memSize;
    }

    @Override
    public long getHostDom0Memory() { // memory size in bytes
        return DEFAULT_DOM0_MEM_MB * 1024 * 1024L;
    }

    @Override
    public String cleanupVnet(String vnetId) {
        return null;
    }

    @Override
    public Integer getVncPort(String name) {
        synchronized (this) {
            MockVm vm = vms.get(name);
            if (vm != null)
                return vm.getVncPort();
        }

        return new Integer(-1);
    }

    public int allocVncPort() {
        for (int i = 0; i < 64; i++) {
            if (((1L << i) & vncPortMap) == 0) {
                vncPortMap |= (1L << i);
                return i;
            }
        }
        return -1;
    }

    public void freeVncPort(int port) {
        vncPortMap &= ~(1L << port);
    }

    @Override
    public MockVm createVmFromSpec(VirtualMachineTO vmSpec) {
        String vmName = vmSpec.getName();
        long ramSize = vmSpec.getMinRam();
        int utilizationPercent = randSeed.nextInt() % 100;
        MockVm vm = null;

        synchronized (this) {
            vm = vms.get(vmName);
            if (vm == null) {
                if (ramSize > getHostFreeMemory()) {
                    s_logger.debug("host is out of memory");
                    throw new CloudRuntimeException("Host is out of Memory");
                }

                int vncPort = allocVncPort();
                if (vncPort < 0) {
                    s_logger.debug("Unable to allocate VNC port");
                    throw new CloudRuntimeException("Unable to allocate vnc port");
                }

                vm = new MockVm(vmName, State.Running, ramSize, vmSpec.getCpus(), utilizationPercent, vncPort);
                vms.put(vmName, vm);
            }
        }
        return vm;
    }

    @Override
    public void createVbd(VirtualMachineTO vmSpec, String vmName, MockVm vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createVif(VirtualMachineTO vmSpec, String vmName, MockVm vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configure(Map<String, Object> params) {
        _params = params;
    }

    protected Long getConfiguredProperty(String key, Long defaultValue) {
        String val = (String)_params.get(key);

        if (val != null) {
            Long result = Long.parseLong(val);
            return result;
        }
        return defaultValue;
    }

    protected Integer getConfiguredProperty(String key, Integer defaultValue) {
        String val = (String)_params.get(key);

        if (val != null) {
            Integer result = Integer.parseInt(val);
            return result;
        }
        return defaultValue;
    }
}
