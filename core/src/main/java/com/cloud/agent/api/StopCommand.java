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

import com.cloud.agent.api.to.DpdkTO;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.vm.VirtualMachine;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class StopCommand extends RebootCommand {
    private boolean isProxy = false;
    private String urlPort = null;
    private String publicConsoleProxyIpAddress = null;
    private GPUDeviceTO gpuDevice;
    boolean checkBeforeCleanup = false;
    String controlIp = null;
    boolean forceStop = false;
    private Map<String, DpdkTO> dpdkInterfaceMapping;
    Map<String, Boolean> vlanToPersistenceMap;

    public Map<String, DpdkTO> getDpdkInterfaceMapping() {
        return dpdkInterfaceMapping;
    }

    public void setDpdkInterfaceMapping(Map<String, DpdkTO> dpdkInterfaceMapping) {
        this.dpdkInterfaceMapping = dpdkInterfaceMapping;
    }
    /**
     * On KVM when using iSCSI-based managed storage, if the user shuts a VM down from the guest OS (as opposed to doing so from CloudStack),
     * we need to pass to the KVM agent a list of applicable iSCSI volumes that need to be disconnected.
     */
    private List<Map<String, String>> volumesToDisconnect = new ArrayList<>();

    protected StopCommand() {
    }

    public StopCommand(VirtualMachine vm, boolean isProxy, String urlPort, String publicConsoleProxyIpAddress, boolean executeInSequence, boolean checkBeforeCleanup) {
        super(vm.getInstanceName(), executeInSequence);
        this.isProxy = isProxy;
        this.urlPort = urlPort;
        this.publicConsoleProxyIpAddress = publicConsoleProxyIpAddress;
        this.checkBeforeCleanup = checkBeforeCleanup;
    }

    public StopCommand(VirtualMachine vm, boolean executeInSequence, boolean checkBeforeCleanup) {
        super(vm.getInstanceName(), executeInSequence);
        this.checkBeforeCleanup = checkBeforeCleanup;
    }

    public StopCommand(VirtualMachine vm, boolean executeInSequence, boolean checkBeforeCleanup, boolean forceStop) {
        super(vm.getInstanceName(), executeInSequence);
        this.checkBeforeCleanup = checkBeforeCleanup;
        this.forceStop = forceStop;
    }

    public StopCommand(String vmName, boolean executeInSequence, boolean checkBeforeCleanup) {
        super(vmName, executeInSequence);
        this.checkBeforeCleanup = checkBeforeCleanup;
    }

    @Override
    public boolean executeInSequence() {
        // VR stop doesn't go through queue
        if (this.vmName != null && this.vmName.startsWith("r-")) {
            return false;
        }
        return this.executeInSequence;
    }

    public boolean isProxy() {
        return this.isProxy;
    }

    public String getURLPort() {
        return this.urlPort;
    }

    public String getPublicConsoleProxyIpAddress() {
        return this.publicConsoleProxyIpAddress;
    }

    public GPUDeviceTO getGpuDevice() {
        return this.gpuDevice;
    }

    public void setGpuDevice(GPUDeviceTO gpuDevice) {
        this.gpuDevice = gpuDevice;
    }

    public boolean checkBeforeCleanup() {
        return this.checkBeforeCleanup;
    }

    public String getControlIp(){
        return controlIp;
    }

    public void setControlIp(String controlIp){
        this.controlIp = controlIp;
    }

    public boolean isForceStop() {
        return forceStop;
    }

    public void setVolumesToDisconnect(List<Map<String, String>> volumesToDisconnect) {
        this.volumesToDisconnect = volumesToDisconnect;
    }

    public List<Map<String, String>> getVolumesToDisconnect() {
        return volumesToDisconnect;
    }

    public Map<String, Boolean> getVlanToPersistenceMap() {
        return vlanToPersistenceMap;
    }

    public void setVlanToPersistenceMap(Map<String, Boolean> vlanToPersistenceMap) {
        this.vlanToPersistenceMap = vlanToPersistenceMap;
    }
}
