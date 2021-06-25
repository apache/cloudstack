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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import net.juniper.tungsten.api.types.ServiceGroup;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class TungstenFabricServiceGroupResponse extends BaseResponse {
    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric service group uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric service group name")
    private String name;

    @SerializedName(ApiConstants.PROTOCOL)
    @Param(description = "Tungsten-Fabric service group protocol")
    private String protocol;

    @SerializedName(ApiConstants.START_PORT)
    @Param(description = "Tungsten-Fabric service group start port")
    private int startPort;

    @SerializedName(ApiConstants.END_PORT)
    @Param(description = "Tungsten-Fabric service group end port")
    private int endPort;

    public TungstenFabricServiceGroupResponse(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.setObjectName("servicegroup");
    }

    public TungstenFabricServiceGroupResponse(ServiceGroup serviceGroup) {
        this.uuid = serviceGroup.getUuid();
        this.name = serviceGroup.getName();
        this.protocol = serviceGroup.getFirewallServiceList().getFirewallService().get(0).getProtocol();
        this.startPort = serviceGroup.getFirewallServiceList().getFirewallService().get(0).getDstPorts().getStartPort();
        this.endPort = serviceGroup.getFirewallServiceList().getFirewallService().get(0).getDstPorts().getEndPort();
        this.setObjectName("servicegroup");
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

}
