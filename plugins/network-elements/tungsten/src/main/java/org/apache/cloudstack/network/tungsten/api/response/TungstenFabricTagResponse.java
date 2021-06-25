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
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.Tag;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenFabricTagResponse extends BaseResponse {
    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric tag type uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric tag name")
    private String name;

    @SerializedName(ApiConstants.NETWORK)
    @Param(description = "list Tungsten-Fabric network")
    private List<TungstenFabricNetworkResponse> networks;

    @SerializedName(ApiConstants.VM)
    @Param(description = "list Tungsten-Fabric vm")
    private List<TungstenFabricVmResponse> vms;

    @SerializedName(ApiConstants.NIC)
    @Param(description = "list Tungsten-Fabric nic")
    private List<TungstenFabricNicResponse> nics;

    @SerializedName(ApiConstants.POLICY)
    @Param(description = "list Tungsten-Fabric policy")
    private List<TungstenFabricPolicyResponse> policys;

    public TungstenFabricTagResponse(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.setObjectName("tag");
    }

    public TungstenFabricTagResponse(Tag tag) {
        this.uuid = tag.getUuid();
        this.name = tag.getName();
        this.setObjectName("tag");
        List<TungstenFabricNetworkResponse> networks = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> networkReferenceList = tag.getVirtualNetworkBackRefs();
        if (networkReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> network : networkReferenceList) {
                TungstenFabricNetworkResponse tungstenFabricNetworkResponse = new TungstenFabricNetworkResponse(
                    network.getUuid(), network.getReferredName().get(network.getReferredName().size() - 1));
                networks.add(tungstenFabricNetworkResponse);
            }
        }
        this.networks = networks;

        List<TungstenFabricVmResponse> vms = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> vmReferenceList = tag.getVirtualMachineBackRefs();
        if (vmReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> vm : vmReferenceList) {
                TungstenFabricVmResponse tungstenFabricVmResponse = new TungstenFabricVmResponse(vm.getUuid(),
                    vm.getReferredName().get(vm.getReferredName().size() - 1));
                vms.add(tungstenFabricVmResponse);
            }
        }
        this.vms = vms;

        List<TungstenFabricNicResponse> nics = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> nicReferenceList = tag.getVirtualMachineInterfaceBackRefs();
        if (nicReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> nic : nicReferenceList) {
                TungstenFabricNicResponse tungstenFabricNicResponse = new TungstenFabricNicResponse(nic.getUuid(),
                    nic.getReferredName().get(nic.getReferredName().size() - 1));
                nics.add(tungstenFabricNicResponse);
            }
        }
        this.nics = nics;

        List<TungstenFabricPolicyResponse> policys = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> policyReferenceList = tag.getNetworkPolicyBackRefs();
        if (policyReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> policy : policyReferenceList) {
                TungstenFabricPolicyResponse tungstenFabricPolicyResponse = new TungstenFabricPolicyResponse(
                    policy.getUuid(), policy.getReferredName().get(policy.getReferredName().size() - 1));
                policys.add(tungstenFabricPolicyResponse);
            }
        }
        this.policys = policys;
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

    public List<TungstenFabricNetworkResponse> getNetworks() {
        return networks;
    }

    public void setNetworks(final List<TungstenFabricNetworkResponse> networks) {
        this.networks = networks;
    }

    public List<TungstenFabricNicResponse> getNics() {
        return nics;
    }

    public void setNics(final List<TungstenFabricNicResponse> nics) {
        this.nics = nics;
    }

    public List<TungstenFabricPolicyResponse> getPolicys() {
        return policys;
    }

    public void setPolicys(final List<TungstenFabricPolicyResponse> policys) {
        this.policys = policys;
    }
}
