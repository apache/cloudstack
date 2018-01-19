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
package com.cloud.simulator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.vm.VirtualMachine.PowerState;

@Entity
@Table(name = "mockvm")
public class MockVMVO implements MockVm, InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "type")
    private String vmType;

    @Column(name = "power_state")
    protected PowerState powerState;

    @Column(name = "vnc_port")
    private int vncPort;

    @Column(name = "memory")
    private long memory;

    @Column(name = "cpu")
    private int cpu;

    @Column(name = "bootargs")
    private String bootargs;

    public MockVMVO() {

    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public long getHostId() {
        return this.hostId;
    }

    @Override
    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public String getVmType() {
        return this.vmType;
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
    }

    @Override
    public PowerState getPowerState() {
        return powerState;
    }

    @Override
    public void setPowerState(PowerState powerState) {
        this.powerState = powerState;
    }


    @Override
    public String getType() {
        return this.vmType;
    }

    @Override
    public int getVncPort() {
        return this.vncPort;
    }

    @Override
    public void setVncPort(int vncPort) {
        this.vncPort = vncPort;
    }

    @Override
    public long getMemory() {
        return this.memory;
    }

    @Override
    public void setMemory(long memory) {
        this.memory = memory;
    }

    @Override
    public int getCpu() {
        return this.cpu;
    }

    @Override
    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    @Override
    public void setType(String type) {
        this.vmType = type;
    }

    @Override
    public String getBootargs() {
        return bootargs;
    }

    @Override
    public void setBootargs(String bootargs) {
        this.bootargs = bootargs;
    }
}
