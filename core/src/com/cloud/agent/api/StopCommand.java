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

import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.vm.VirtualMachine;

public class StopCommand extends RebootCommand {
    private Boolean isProxy = false;
    private String urlPort = null;
    private String publicConsoleProxyIpAddress = null;
    Boolean executeInSequence = false;
    private GPUDeviceTO gpuDevice;
    Boolean checkBeforeCleanup = false;

    protected StopCommand() {
    }

    public StopCommand(VirtualMachine vm, Boolean isProxy, String urlPort, String publicConsoleProxyIpAddress, Boolean executeInSequence, Boolean checkBeforeCleanup) {
        super(vm);
        this.isProxy = isProxy;
        this.urlPort = urlPort;
        this.publicConsoleProxyIpAddress = publicConsoleProxyIpAddress;
        this.executeInSequence = executeInSequence;
        this.checkBeforeCleanup = checkBeforeCleanup;
    }

    public StopCommand(VirtualMachine vm, Boolean executeInSequence, Boolean checkBeforeCleanup) {
        super(vm);
        this.executeInSequence = executeInSequence;
        this.checkBeforeCleanup = checkBeforeCleanup;
    }

    public StopCommand(String vmName, Boolean executeInSequence, Boolean checkBeforeCleanup) {
        super(vmName);
        this.executeInSequence = executeInSequence;
        this.checkBeforeCleanup = checkBeforeCleanup;
    }

    @Override
    public Boolean executeInSequence() {
        //VR stop doesn't go through queue
        if (vmName != null && vmName.startsWith("r-")) {
            return false;
        }
        return executeInSequence;
    }

    public Boolean isProxy() {
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

    public Boolean checkBeforeCleanup() {
        return this.checkBeforeCleanup;
    }
}
