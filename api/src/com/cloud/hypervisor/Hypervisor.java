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
package com.cloud.hypervisor;

import com.cloud.storage.Storage.ImageFormat;

public class Hypervisor {

    public static enum HypervisorType {
        None, //for storage hosts
        XenServer,
        KVM,
        VMware,
        Hyperv,
        VirtualBox,
        Parralels,
        BareMetal,
        Simulator,
        Ovm,
        LXC,

        Any; /*If you don't care about the hypervisor type*/

        public static HypervisorType getType(String hypervisor) {
            if (hypervisor == null) {
                return HypervisorType.None;
            }
            if (hypervisor.equalsIgnoreCase("XenServer")) {
                return HypervisorType.XenServer;
            } else if (hypervisor.equalsIgnoreCase("KVM")) {
                return HypervisorType.KVM;
            } else if (hypervisor.equalsIgnoreCase("VMware")) {
                return HypervisorType.VMware;
            } else if (hypervisor.equalsIgnoreCase("Hyperv")) {
                return HypervisorType.Hyperv;
            } else if (hypervisor.equalsIgnoreCase("VirtualBox")) {
                return HypervisorType.VirtualBox;
            } else if (hypervisor.equalsIgnoreCase("Parralels")) {
                return HypervisorType.Parralels;
            }else if (hypervisor.equalsIgnoreCase("BareMetal")) {
                return HypervisorType.BareMetal;
            } else if (hypervisor.equalsIgnoreCase("Simulator")) {
                return HypervisorType.Simulator;
            } else if (hypervisor.equalsIgnoreCase("Ovm")) {
                return HypervisorType.Ovm;
            } else if (hypervisor.equalsIgnoreCase("LXC")) {
                return HypervisorType.LXC;
            } else if (hypervisor.equalsIgnoreCase("Any")) {
                return HypervisorType.Any;
            } else {
                return HypervisorType.None;
            }
        }

        /**
         * This method really needs to be part of the properties of the hypervisor type itself.
         * 
         * @param hyperType
         * @return
         */
        public static ImageFormat getSupportedImageFormat(HypervisorType hyperType) {
            if (hyperType == HypervisorType.XenServer) {
                return ImageFormat.VHD;
            } else if (hyperType == HypervisorType.KVM) {
                return ImageFormat.QCOW2;
            } else if (hyperType == HypervisorType.VMware) {
                return ImageFormat.OVA;
            } else if (hyperType == HypervisorType.Ovm) {
                return ImageFormat.RAW;
            } else {
                return null;
            }
        }
    }

}
