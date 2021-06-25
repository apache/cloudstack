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
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.VirtualNetworkPolicyType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenFabricPolicyResponse extends BaseResponse {
    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric tag type uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric policy name")
    private String name;

    @SerializedName(ApiConstants.NETWORK)
    @Param(description = "list Tungsten-Fabric policy network name")
    private List<TungstenFabricNetworkResponse> networks;

    public TungstenFabricPolicyResponse(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.setObjectName("policy");
    }

    public TungstenFabricPolicyResponse(NetworkPolicy networkPolicy) {
        this.uuid = networkPolicy.getUuid();
        this.name = networkPolicy.getName();
        List<TungstenFabricNetworkResponse> networks = new ArrayList<>();
        List<ObjectReference<VirtualNetworkPolicyType>> objectReferenceList = networkPolicy.getVirtualNetworkBackRefs();
        if (objectReferenceList != null) {
            for (ObjectReference<VirtualNetworkPolicyType> objectReference : objectReferenceList) {
                TungstenFabricNetworkResponse tungstenFabricNetworkResponse = new TungstenFabricNetworkResponse(objectReference.getUuid(),
                    objectReference.getReferredName().get(objectReference.getReferredName().size() - 1));
                networks.add(tungstenFabricNetworkResponse);
            }
        }
        this.networks = networks;
        this.setObjectName("policy");
    }

    public List<TungstenFabricNetworkResponse> getNetworks() {
        return networks;
    }

    public void setNetworks(final List<TungstenFabricNetworkResponse> networks) {
        this.networks = networks;
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
