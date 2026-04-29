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
package com.cloud.baremetal.manager;

import java.util.List;

/**
 * Created by frank on 5/8/14.
 */
public class BaremetalRct {
    public static class SwitchEntry {
        private String ip;
        private String username;
        private String password;
        private String type;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class HostEntry {
        private String uuid;
        private String mac;
        private String port;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }
    }

    public static class Rack {
        private SwitchEntry l2Switch;
        private List<HostEntry> hosts;

        public SwitchEntry getL2Switch() {
            return l2Switch;
        }

        public void setL2Switch(SwitchEntry l2Switch) {
            this.l2Switch = l2Switch;
        }

        public List<HostEntry> getHosts() {
            return hosts;
        }

        public void setHosts(List<HostEntry> hosts) {
            this.hosts = hosts;
        }
    }

    private List<Rack> racks;

    public List<Rack> getRacks() {
        return racks;
    }

    public void setRacks(List<Rack> racks) {
        this.racks = racks;
    }
}
