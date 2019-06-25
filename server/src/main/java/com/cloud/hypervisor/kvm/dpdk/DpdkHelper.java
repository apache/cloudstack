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
package com.cloud.hypervisor.kvm.dpdk;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.api.ApiConstants;

public interface DpdkHelper {

    String DPDK_VHOST_USER_MODE = "DPDK-VHOSTUSER";
    String DPDK_NUMA = ApiConstants.EXTRA_CONFIG + "-dpdk-numa";
    String DPDK_HUGE_PAGES = ApiConstants.EXTRA_CONFIG + "-dpdk-hugepages";
    String DPDK_INTERFACE_PREFIX = ApiConstants.EXTRA_CONFIG + "-dpdk-interface-";

    enum VHostUserMode {
        CLIENT("client"), SERVER("server");

        private String str;

        VHostUserMode(String str) {
            this.str = str;
        }

        public static VHostUserMode fromValue(String val) {
            if (val.equalsIgnoreCase("client")) {
                return CLIENT;
            } else if (val.equalsIgnoreCase("server")) {
                return SERVER;
            } else {
                throw new IllegalArgumentException("Invalid DPDK vHost User mode:" + val);
            }
        }

        @Override
        public String toString() {
            return str;
        }
    }

    /**
     * True if the DPDK vHost user mode setting is part of the VM service offering details, false if not.
     * @param vm
     */
    boolean isDpdkvHostUserModeSettingOnServiceOffering(VirtualMachineProfile vm);

    /**
     * Add DPDK vHost User Mode as extra configuration to the VM TO (if present on the VM service offering details)
     */
    void setDpdkVhostUserMode(VirtualMachineTO to, VirtualMachineProfile vm);

    /**
     * True if VM is a guest DPDK enabled VM, false if not.
     * It is determined by:
     *      - VM type is guest
     *      - VM details contains NUMA and Huge pages configurations for DPDK
     *      - VM host contains the DPDK capability
     */
    boolean isVMDpdkEnabled(long vmId);

    /**
     * True if host is DPDK enabled, false if not.
     * Host is DPDK enabled when:
     *      - 'dpdk' is part of the host capabilities
     */
    boolean isHostDpdkEnabled(long hostId);
}
