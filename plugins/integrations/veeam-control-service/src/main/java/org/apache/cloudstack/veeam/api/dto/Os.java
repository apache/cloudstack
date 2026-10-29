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

package org.apache.cloudstack.veeam.api.dto;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Os {
    private String type;
    private String version;
    private Boot boot;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boot getBoot() {
        return boot;
    }

    public void setBoot(Boot boot) {
        this.boot = boot;
    }

    public final static class Boot {
        private NamedList<String> devices;

        public NamedList<String> getDevices() {
            return devices;
        }

        public void setDevices(NamedList<String> devices) {
            this.devices = devices;
        }
    }

    /**
     * Infers the oVirt guest OS identifier expected by Veeam from the operating system name.
     * This allows Veeam to correctly recognize Windows virtual machines and enable features
     * such as file-level restore.
     *<p>
     * Identifier values are derived from the
     * <a href="https://github.com/oVirt/ovirt-engine/blob/master/packaging/conf/osinfo-defaults.properties">
     * oVirt OS defaults
     * </a>.
     *</p>
     *
     * @param name normalized operating system name
     * @return the matching oVirt Windows OS identifier
     */
    public static String getWindowsOsKey(final String name) {
        if (name.contains("2025")) {
            return "windows_2025";
        }

        if (name.contains("2022")) {
            return "windows_2022";
        }

        if (name.contains("11")) {
            return "windows_11";
        }

        if (name.contains("2019")) {
            return "windows_2019x64";
        }

        if (name.contains("2016")) {
            return "windows_2016x64";
        }

        if (name.contains("10") && name.contains("64")) {
            return "windows_10x64";
        }

        return "windows_10";
    }

    public static String inferTypeFromOsName(String name) {
        if (StringUtils.isBlank(name)) {
            return "other";
        }
        String normalized = name.trim().toLowerCase();
        if (normalized.contains("windows")) {
            return getWindowsOsKey(normalized);
        }
        if (normalized.contains("linux") || normalized.contains("ubuntu") || normalized.contains("debian") ||
                normalized.contains("centos") || normalized.contains("red hat") || normalized.contains("suse")) {
            return "linux";
        }
        return "other";
    }
}
