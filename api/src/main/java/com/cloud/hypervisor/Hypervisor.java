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
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Hypervisor {
    public static class HypervisorType {
        private static final Map<String, HypervisorType> hypervisorTypeMap = new LinkedHashMap<>();
        public static final HypervisorType None = new HypervisorType("None"); //for storage hosts
        public static final HypervisorType XenServer = new HypervisorType("XenServer", ImageFormat.VHD);
        public static final HypervisorType KVM = new HypervisorType("KVM", ImageFormat.QCOW2);
        public static final HypervisorType VMware = new HypervisorType("VMware", ImageFormat.OVA);
        public static final HypervisorType Hyperv = new HypervisorType("Hyperv");
        public static final HypervisorType VirtualBox = new HypervisorType("VirtualBox");
        public static final HypervisorType Parralels = new HypervisorType("Parralels");
        public static final HypervisorType BareMetal = new HypervisorType("BareMetal");
        public static final HypervisorType Simulator = new HypervisorType("Simulator");
        public static final HypervisorType Ovm = new HypervisorType("Ovm", ImageFormat.RAW);
        public static final HypervisorType Ovm3 = new HypervisorType("Ovm3", ImageFormat.RAW);
        public static final HypervisorType LXC = new HypervisorType("LXC");
        public static final HypervisorType Custom = new HypervisorType("Custom");
        public static final HypervisorType Any = new HypervisorType("Any"); /*If you don't care about the hypervisor type*/
        private final String name;
        private final ImageFormat imageFormat;

        public HypervisorType(String name) {
            this(name, null);
        }

        public HypervisorType(String name, ImageFormat imageFormat) {
            this.name = name;
            this.imageFormat = imageFormat;
            if (name.equals("Parralels")){ // typo in the original code
                hypervisorTypeMap.put("parallels", this);
            } else {
                hypervisorTypeMap.putIfAbsent(name.toLowerCase(Locale.ROOT), this);
            }
        }

        public static HypervisorType getType(String hypervisor) {
            return hypervisor == null ? HypervisorType.None :
                    (hypervisor.toLowerCase(Locale.ROOT).equalsIgnoreCase(
                            HypervisorGuru.HypervisorCustomDisplayName.value()) ? Custom :
                            hypervisorTypeMap.getOrDefault(hypervisor.toLowerCase(Locale.ROOT), HypervisorType.None));
        }

        public static HypervisorType[] values() {
            return hypervisorTypeMap.values().toArray(HypervisorType[]::new).clone();
        }

        public static HypervisorType valueOf(String name) {
            if (StringUtils.isBlank(name)) {
                return null;
            }

            HypervisorType hypervisorType = hypervisorTypeMap.get(name.toLowerCase(Locale.ROOT));
            if (hypervisorType == null) {
                throw new IllegalArgumentException("HypervisorType '" + name + "' not found");
            }
            return hypervisorType;
        }

        /**
         * Returns the display name of a hypervisor type in case the custom hypervisor is used,
         * using the 'hypervisor.custom.display.name' setting. Otherwise, returns hypervisor name
         */
        public String getHypervisorDisplayName() {
            return HypervisorType.Custom.equals(this) ? HypervisorGuru.HypervisorCustomDisplayName.value() : name;
        }

        /**
         * This method really needs to be part of the properties of the hypervisor type itself.
         *
         * @return
         */
        public ImageFormat getSupportedImageFormat() {
            return imageFormat;
        }

        public String name() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HypervisorType that = (HypervisorType) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
