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
package com.cloud.network.bigswitch;

public class NetworkData {
    private final Network network;

    public Network getNetwork() {
        return network;
    }

    public NetworkData() {
        network = new Network();
    }

    public class Network {
        private String id;
        private String name;
        private String tenantId;
        private int vlan;
        private String gateway;
        private String state;

        public String getUuid() {
            return id;
        }

        public void setUuid(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return name;
        }

        public void setDisplay_name(String displayName) {
            name = displayName;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }

        public String getGateway() {
            return gateway;
        }

        public void setGateway(String gateway) {
            this.gateway = gateway;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }
}
