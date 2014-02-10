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

import com.cloud.vm.VirtualMachine.State;

// As storage is mapped from storage device, can virtually treat that VM here does
public class MockVm {

    private String vmName;
    private State state = State.Stopped;

    private long ramSize; // unit of Mbytes
    private int cpuCount;
    private int utilization; // in percentage
    private int vncPort; // 0-based allocation, real port number needs to be
                         // applied with base

    public MockVm() {
    }

    public MockVm(String vmName, State state, long ramSize, int cpuCount, int utilization, int vncPort) {
        this.vmName = vmName;
        this.state = state;
        this.ramSize = ramSize;
        this.cpuCount = cpuCount;
        this.utilization = utilization;
        this.vncPort = vncPort;
    }

    public String getName() {
        return vmName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public long getRamSize() {
        return ramSize;
    }

    public int getCpuCount() {
        return cpuCount;
    }

    public int getUtilization() {
        return utilization;
    }

    public int getVncPort() {
        return vncPort;
    }

}
