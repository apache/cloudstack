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
package com.cloud.hypervisor.vmware.mo;

import com.cloud.vm.VirtualMachine;

public class VmwareVmOnDatacenter {

    private String clusterName;
    private String hostName;
    private String vmName;
    private VirtualMachine.PowerState powerState;

    public VmwareVmOnDatacenter(String clusterName, String hostName, String vmName, VirtualMachine.PowerState powerState) {
        this.clusterName = clusterName;
        this.hostName = hostName;
        this.vmName = vmName;
        this.powerState = powerState;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getHostName() {
        return hostName;
    }

    public String getVmName() {
        return vmName;
    }

    public VirtualMachine.PowerState getPowerState() {
        return powerState;
    }
}
