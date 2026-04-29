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

public class FloatingIpData {
    @SerializedName("router_id") private String routerId;
    @SerializedName("tenant_id") private String tenantId;
    @SerializedName("floating_network_id") private String networkId;
    @SerializedName("fixed_ip_address") private String fixedIp;
    @SerializedName("floating_ip_address") private String floatingIp;
    @SerializedName("floating_mac_address") private String mac;
    @SerializedName("id") private String id;

    public FloatingIpData(){
        this.mac = null;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getFixedIp() {
        return fixedIp;
    }

    public void setFixedIp(String fixedIp) {
        this.fixedIp = fixedIp;
    }

    public String getFloatingIp() {
        return floatingIp;
    }

    /***
     * current implementation auto-generates id using public ip
     * replacing "." with "-"
     ***/
    public void setFloatingIpAndId(String floatingIp) {
        this.floatingIp = floatingIp;
        this.id = floatingIp.replace(".", "-");
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getId() {
        return id;
    }
}
