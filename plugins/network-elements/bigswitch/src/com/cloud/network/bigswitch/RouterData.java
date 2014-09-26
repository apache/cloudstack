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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class RouterData {
    @SerializedName("router") private Router router;

    public RouterData(String tenantId){
        router = new Router(tenantId);
    }

    public class Router {
        @SerializedName("tenant_id") private String tenantId;
        @SerializedName("id") private String id;
        @SerializedName("external_gateway_info") private ExternalGateway externalGateway;
        @SerializedName("router_rules") private List<AclData> acls;
        @SerializedName("interfaces") private List<RouterInterfaceData.RouterInterface> interfaces;

        public Router(String tenantId){
            this.tenantId = tenantId;
            this.id = tenantId;
            this.externalGateway = null;
            this.acls = new ArrayList<AclData>();
            this.interfaces = null;
        }

        public List<AclData> getAcls() {
            return acls;
        }

        public class ExternalGateway{
            @SerializedName("tenant_id") private String tenantId;
            @SerializedName("network_id") private String networkId;
            @SerializedName("external_fixed_ips") private List<ExternalIp> externalIps;

            public ExternalGateway(String gatewayIp){
                this.tenantId = "external";
                this.networkId = "external";
                this.externalIps = new ArrayList<ExternalIp>();
                if(gatewayIp!=null){
                    this.externalIps.add(new ExternalIp(gatewayIp));
                }
            }

            public class ExternalIp{
                @SerializedName("subnet_id") private String subnetId; //assume don't care for now
                @SerializedName("ip_address") private String ipAddress;

                public ExternalIp(String ipAddress){
                    this.subnetId = "";
                    this.ipAddress = ipAddress;
                }

                public String getSubnetId() {
                    return subnetId;
                }

                public String getIpAddress() {
                    return ipAddress;
                }
            }

            public String getNetworkId() {
                return networkId;
            }

            public List<ExternalIp> getExternalIps() {
                return externalIps;
            }
        }

        public void addExternalGateway(String gatewayIp){
            if(gatewayIp != null){
                this.externalGateway = new ExternalGateway(gatewayIp);
            }
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getId() {
            return id;
        }

        public ExternalGateway getExternalGateway() {
            return externalGateway;
        }

        public List<RouterInterfaceData.RouterInterface> getInterfaces() {
            return interfaces;
        }

        public void addInterface(RouterInterfaceData intf){
            if(this.interfaces == null){
                this.interfaces = new ArrayList<RouterInterfaceData.RouterInterface>();
            }
            this.interfaces.add(intf.getRouterInterface());
        }

        public void setInterfaces(List<RouterInterfaceData.RouterInterface> interfaces) {
            this.interfaces = interfaces;
        }
    }

    public Router getRouter() {
        return router;
    }
}
