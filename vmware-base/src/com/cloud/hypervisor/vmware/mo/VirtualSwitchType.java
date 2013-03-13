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


public enum VirtualSwitchType {
    None,
    StandardVirtualSwitch,
    VMwareDistributedVirtualSwitch,
    NexusDistributedVirtualSwitch;

    public final static String vmwareStandardVirtualSwitch = "vmwaresvs";
    public final static String vmwareDistributedVirtualSwitch = "vmwaredvs";
    public final static String nexusDistributedVirtualSwitch = "nexusdvs";

    public static VirtualSwitchType getType(String vSwitchType) {
        if (vSwitchType == null || vSwitchType.equalsIgnoreCase(vmwareStandardVirtualSwitch)) {
            return VirtualSwitchType.StandardVirtualSwitch;
        } else if (vSwitchType.equalsIgnoreCase(vmwareDistributedVirtualSwitch)) {
            return VirtualSwitchType.VMwareDistributedVirtualSwitch;
        } else if (vSwitchType.equalsIgnoreCase(nexusDistributedVirtualSwitch)) {
            return VirtualSwitchType.NexusDistributedVirtualSwitch;
        }
        return VirtualSwitchType.None;
    }

    @Override
    public String toString() {
        if (this == VirtualSwitchType.StandardVirtualSwitch) {
            return vmwareStandardVirtualSwitch;
        } else if (this == VirtualSwitchType.VMwareDistributedVirtualSwitch) {
            return vmwareDistributedVirtualSwitch;
        } else if (this == VirtualSwitchType.NexusDistributedVirtualSwitch) {
            return nexusDistributedVirtualSwitch;
        }
        return "";
    }
}
