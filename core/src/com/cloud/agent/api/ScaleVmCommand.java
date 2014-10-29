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

import com.cloud.agent.api.to.VirtualMachineTO;

public class ScaleVmCommand extends Command {

    VirtualMachineTO vm;
    String vmName;
    int cpus;
    Integer minSpeed;
    Integer maxSpeed;
    long minRam;
    long maxRam;

    public VirtualMachineTO getVm() {
        return vm;
    }

    public void setVm(VirtualMachineTO vm) {
        this.vm = vm;
    }

    public int getCpus() {
        return cpus;
    }

    public ScaleVmCommand(String vmName, int cpus, Integer minSpeed, Integer maxSpeed, long minRam, long maxRam, boolean limitCpuUse) {
        super();
        this.vmName = vmName;
        this.cpus = cpus;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.minRam = minRam;
        this.maxRam = maxRam;
        this.vm = new VirtualMachineTO(1L, vmName, null, cpus, minSpeed, maxSpeed, minRam, maxRam, null, null, false, limitCpuUse, null);
        /*vm.setName(vmName);
        vm.setCpus(cpus);
        vm.setRam(minRam, maxRam);*/
    }

    public void setCpus(int cpus) {
        this.cpus = cpus;
    }

    public Integer getMinSpeed() {
        return minSpeed;
    }

    public void setMinSpeed(Integer minSpeed) {
        this.minSpeed = minSpeed;
    }

    public Integer getMaxSpeed() {
        return minSpeed;
    }

    public void setMaxSpeed(Integer maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public long getMinRam() {
        return minRam;
    }

    public void setMinRam(long minRam) {
        this.minRam = minRam;
    }

    public long getMaxRam() {
        return maxRam;
    }

    public void setMaxRam(long maxRam) {
        this.maxRam = maxRam;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public VirtualMachineTO getVirtualMachine() {
        return vm;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    protected ScaleVmCommand() {
    }

    public ScaleVmCommand(VirtualMachineTO vm) {
        this.vm = vm;
    }

    public boolean getLimitCpuUse() {
        // TODO Auto-generated method stub
        return false;
    }

}
