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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Hypervisor {

    static Map<String, HypervisorType> hypervisorTypeMap;
    static Map<HypervisorType, ImageFormat> supportedImageFormatMap;

    public enum HypervisorType {
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
        Ovm3,
        LXC,
        Custom,

        Any; /*If you don't care about the hypervisor type*/

        static {
            hypervisorTypeMap = new HashMap<>();
            hypervisorTypeMap.put("xenserver", HypervisorType.XenServer);
            hypervisorTypeMap.put("kvm", HypervisorType.KVM);
            hypervisorTypeMap.put("vmware", HypervisorType.VMware);
            hypervisorTypeMap.put("hyperv", HypervisorType.Hyperv);
            hypervisorTypeMap.put("virtualbox", HypervisorType.VirtualBox);
            hypervisorTypeMap.put("parallels", HypervisorType.Parralels);
            hypervisorTypeMap.put("baremetal", HypervisorType.BareMetal);
            hypervisorTypeMap.put("simulator", HypervisorType.Simulator);
            hypervisorTypeMap.put("ovm", HypervisorType.Ovm);
            hypervisorTypeMap.put("lxc", HypervisorType.LXC);
            hypervisorTypeMap.put("any", HypervisorType.Any);
            hypervisorTypeMap.put("ovm3", HypervisorType.Ovm3);
            hypervisorTypeMap.put("custom", HypervisorType.Custom);

            supportedImageFormatMap = new HashMap<>();
            supportedImageFormatMap.put(HypervisorType.XenServer, ImageFormat.VHD);
            supportedImageFormatMap.put(HypervisorType.KVM, ImageFormat.QCOW2);
            supportedImageFormatMap.put(HypervisorType.VMware, ImageFormat.OVA);
            supportedImageFormatMap.put(HypervisorType.Ovm, ImageFormat.RAW);
            supportedImageFormatMap.put(HypervisorType.Ovm3, ImageFormat.RAW);
        }

        public static HypervisorType getType(String hypervisor) {
            return hypervisor == null ? HypervisorType.None :
                    (hypervisor.toLowerCase(Locale.ROOT).equalsIgnoreCase(
                            HypervisorGuru.HypervisorCustomDisplayName.value()) ? Custom :
                            hypervisorTypeMap.getOrDefault(hypervisor.toLowerCase(Locale.ROOT), HypervisorType.None));
        }

        /**
         * Returns the display name of a hypervisor type in case the custom hypervisor is used,
         * using the 'hypervisor.custom.display.name' setting. Otherwise, returns hypervisor name
         */
        public String getHypervisorDisplayName() {
            return !Hypervisor.HypervisorType.Custom.equals(this) ?
                    this.toString() :
                    HypervisorGuru.HypervisorCustomDisplayName.value();
        }

        /**
         * This method really needs to be part of the properties of the hypervisor type itself.
         *
         * @param hyperType
         * @return
         */
        public static ImageFormat getSupportedImageFormat(HypervisorType hyperType) {
            return supportedImageFormatMap.getOrDefault(hyperType, null);
        }
    }

}
