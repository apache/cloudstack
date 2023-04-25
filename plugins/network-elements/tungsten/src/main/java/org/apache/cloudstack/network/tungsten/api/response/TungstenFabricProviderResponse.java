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
package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.network.TungstenProvider;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = {TungstenProvider.class})
public class TungstenFabricProviderResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric provider name")
    private String name;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_UUID)
    @Param(description = "Tungsten-Fabric provider uuid")
    private String uuid;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    @SerializedName("securitygroupsenabled")
    @Param(description = "true if security groups support is enabled, false otherwise")
    private boolean securityGroupsEnabled;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_HOSTNAME)
    @Param(description = "Tungsten-Fabric provider hostname")
    private String hostname;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_PORT)
    @Param(description = "Tungsten-Fabric provider port")
    private String port;

    @SerializedName(ApiConstants.TUNGSTEN_GATEWAY)
    @Param(description = "Tungsten-Fabric provider gateway")
    private String gateway;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_VROUTER_PORT)
    @Param(description = "Tungsten-Fabric provider vrouter port")
    private String vrouterPort;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_INTROSPECT_PORT)
    @Param(description = "Tungsten-Fabric provider introspect port")
    private String introspectPort;

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(final String zoneName) {
        this.zoneName = zoneName;
    }

    public boolean isSecurityGroupsEnabled() {
        return securityGroupsEnabled;
    }

    public void setSecurityGroupsEnabled(final boolean securityGroupsEnabled) {
        this.securityGroupsEnabled = securityGroupsEnabled;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(final String gateway) {
        this.gateway = gateway;
    }

    public String getIntrospectPort() {
        return introspectPort;
    }

    public void setIntrospectPort(final String introspectPort) {
        this.introspectPort = introspectPort;
    }

    public String getVrouterPort() {
        return vrouterPort;
    }

    public void setVrouterPort(final String vrouterPort) {
        this.vrouterPort = vrouterPort;
    }
}
