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

package com.cloud.network.bigswitch;

import com.google.gson.annotations.SerializedName;

public class RouterInterfaceData {
    @SerializedName("interface") private RouterInterface routerInterface;

    public RouterInterfaceData(String tenantId, String gatewayIp, String cidr, String id, String name){
        routerInterface = new RouterInterface(tenantId, gatewayIp, cidr, id, name);
    }

    public class RouterInterface {
        @SerializedName("subnet") private NetworkData.Segment subnet;
        @SerializedName("id") private String id;
        @SerializedName("network") private NetworkData.Network network;

        public RouterInterface(String tenantId, String gatewayIp, String cidr, String id, String name){
            NetworkData data = new NetworkData();
            this.subnet = data.new Segment();
            this.subnet.setTenantId(tenantId);
            this.subnet.setGatewayIp(gatewayIp);
            this.subnet.setCidr(cidr);

            this.id = id;

            this.network = data.getNetwork();
            this.network.setTenantId(tenantId);
            this.network.setId(id);
            this.network.setName(name);
        }

        public NetworkData.Segment getSubnet() {
            return subnet;
        }

        public String getId() {
            return id;
        }

        public NetworkData.Network getNetwork() {
            return network;
        }
    }

    public RouterInterface getRouterInterface() {
        return routerInterface;
    }
}
