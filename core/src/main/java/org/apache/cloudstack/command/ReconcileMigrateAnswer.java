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

package org.apache.cloudstack.command;

import com.cloud.vm.VirtualMachine;

import java.util.List;

public class ReconcileMigrateAnswer extends ReconcileAnswer {

    Long hostId;
    String vmName;
    VirtualMachine.State vmState;
    List<String> vmDisks;

    public ReconcileMigrateAnswer() {
    }

    public ReconcileMigrateAnswer(String vmName, VirtualMachine.State vmState) {
        this.vmName = vmName;
        this.vmState = vmState;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getVmName() {
        return vmName;
    }

    public VirtualMachine.State getVmState() {
        return vmState;
    }

    public void setVmState(VirtualMachine.State vmState) {
        this.vmState = vmState;
    }

    public List<String> getVmDisks() {
        return vmDisks;
    }

    public void setVmDisks(List<String> vmDisks) {
        this.vmDisks = vmDisks;
    }
}
