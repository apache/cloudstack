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
public class TungstenProviderResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "tungsten provider name")
    private String name;

    @SerializedName(ApiConstants.UUID)
    @Param(description = "tungsten provider uuid")
    private String uuid;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_HOSTNAME)
    @Param(description = "tungsten provider hostname")
    private String hostname;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_PORT)
    @Param(description = "tungsten provider port")
    private String port;

    @SerializedName(ApiConstants.TUNGSTEN_GATEWAY)
    @Param(description = "tungsten gateway")
    private String gateway;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_VROUTER_PORT)
    @Param(description = "tungsten provider vrouter port")
    private String vrouterPort;

    @SerializedName(ApiConstants.TUNGSTEN_PROVIDER_INTROSPECT_PORT)
    @Param(description = "tungsten provider introspect port")
    private String introspectPort;

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
