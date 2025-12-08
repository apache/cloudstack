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

package org.apache.cloudstack.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class InfrastructureResponse extends BaseResponse {

    @SerializedName("zones")
    @Param(description = "Number of zones")
    private Integer zones;

    @SerializedName("pods")
    @Param(description = "Number of pods")
    private Integer pods;

    @SerializedName("clusters")
    @Param(description = "Number of clusters")
    private Integer clusters;

    @SerializedName("hosts")
    @Param(description = "Number of hypervisor hosts")
    private Integer hosts;

    @SerializedName("storagepools")
    @Param(description = "Number of storage pools")
    private Integer storagePools;

    @SerializedName("imagestores")
    @Param(description = "Number of images stores")
    private Integer imageStores;

    @SerializedName("objectstores")
    @Param(description = "Number of object stores")
    private Integer objectStores;

    @SerializedName("systemvms")
    @Param(description = "Number of systemvms")
    private Integer systemvms;

    @SerializedName("routers")
    @Param(description = "Number of routers")
    private Integer routers;

    @SerializedName("ilbvms")
    @Param(description = "Number of internal LBs")
    private Integer internalLbs;

    @SerializedName("cpusockets")
    @Param(description = "Number of cpu sockets")
    private Integer cpuSockets;

    @SerializedName("managementservers")
    @Param(description = "Number of management servers")
    private Integer managementServers;

    @SerializedName("alerts")
    @Param(description = "Number of Alerts")
    private Integer alerts;

    public InfrastructureResponse() {
        setObjectName("infrastructure");
    }

    public void setZones(final Integer zones) {
        this.zones = zones;
    }

    public void setPods(final Integer pods) {
        this.pods = pods;
    }

    public void setClusters(final Integer clusters) {
        this.clusters = clusters;
    }

    public void setHosts(final Integer hosts) {
        this.hosts = hosts;
    }

    public void setStoragePools(final Integer storagePools) {
        this.storagePools = storagePools;
    }

    public void setImageStores(final Integer imageStores) {
        this.imageStores = imageStores;
    }

    public void setSystemvms(final Integer systemvms) {
        this.systemvms = systemvms;
    }

    public void setRouters(final Integer routers) {
        this.routers = routers;
    }

    public void setCpuSockets(final Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public void setManagementServers(Integer managementServers) {
        this.managementServers = managementServers;
    }

    public void setAlerts(Integer alerts) { this.alerts = alerts; }

    public void setInternalLbs(Integer internalLbs) { this.internalLbs = internalLbs; }

    public void setObjectStores(Integer objectStores) {
        this.objectStores = objectStores;
    }
}
