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

import com.cloud.dc.DataCenter;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.Tag;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.network.tungsten.model.TungstenTag;

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

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricTagResponse(String uuid, String name, DataCenter zone) {
        this.uuid = uuid;
        this.name = name;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("tag");
    }

    public TungstenFabricTagResponse(Tag tag, DataCenter zone) {
        this.uuid = tag.getUuid();
        this.name = tag.getName();
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("tag");
        List<TungstenFabricNetworkResponse> responseNetworks = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> networkReferenceList = tag.getVirtualNetworkBackRefs();
        if (networkReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> network : networkReferenceList) {
                TungstenFabricNetworkResponse tungstenFabricNetworkResponse = new TungstenFabricNetworkResponse(
                    network.getUuid(), network.getReferredName().get(network.getReferredName().size() - 1));
                responseNetworks.add(tungstenFabricNetworkResponse);
            }
        }
        this.networks = responseNetworks;

        List<TungstenFabricVmResponse> responseVms = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> vmReferenceList = tag.getVirtualMachineBackRefs();
        if (vmReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> vm : vmReferenceList) {
                TungstenFabricVmResponse tungstenFabricVmResponse = new TungstenFabricVmResponse(vm.getUuid(),
                    vm.getReferredName().get(vm.getReferredName().size() - 1));
                responseVms.add(tungstenFabricVmResponse);
            }
        }
        this.vms = responseVms;

        List<TungstenFabricNicResponse> responsesNics = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> nicReferenceList = tag.getVirtualMachineInterfaceBackRefs();
        if (nicReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> nic : nicReferenceList) {
                TungstenFabricNicResponse tungstenFabricNicResponse = new TungstenFabricNicResponse(nic.getUuid(),
                    nic.getReferredName().get(nic.getReferredName().size() - 1), zone);
                responsesNics.add(tungstenFabricNicResponse);
            }
        }
        this.nics = responsesNics;

        List<TungstenFabricPolicyResponse> responsePolicys = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> policyReferenceList = tag.getNetworkPolicyBackRefs();
        if (policyReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> policy : policyReferenceList) {
                TungstenFabricPolicyResponse tungstenFabricPolicyResponse = new TungstenFabricPolicyResponse(
                    policy.getUuid(), policy.getReferredName().get(policy.getReferredName().size() - 1), zone);
                responsePolicys.add(tungstenFabricPolicyResponse);
            }
        }
        this.policys = responsePolicys;
    }

    public TungstenFabricTagResponse(TungstenTag tungstenTag, DataCenter zone) {
        this.uuid = tungstenTag.getTag().getUuid();
        this.name = tungstenTag.getTag().getName();
        this.setObjectName("tag");
        List<TungstenFabricNetworkResponse> responseNetworks = new ArrayList<>();
        for (VirtualNetwork virtualNetwork : tungstenTag.getVirtualNetworkList()) {
            responseNetworks.add(new TungstenFabricNetworkResponse(virtualNetwork, zone));
        }
        this.networks = responseNetworks;

        List<TungstenFabricVmResponse> responseVms = new ArrayList<>();
        for (VirtualMachine virtualMachine : tungstenTag.getVirtualMachineList()) {
            responseVms.add(new TungstenFabricVmResponse(virtualMachine, zone));
        }
        this.vms = responseVms;

        List<TungstenFabricNicResponse> responseNics = new ArrayList<>();
        for (VirtualMachineInterface virtualMachineInterface : tungstenTag.getVirtualMachineInterfaceList()) {
            responseNics.add(new TungstenFabricNicResponse(virtualMachineInterface, zone));
        }
        this.nics = responseNics;

        List<TungstenFabricPolicyResponse> responsePolicys = new ArrayList<>();
        for (NetworkPolicy networkPolicy : tungstenTag.getNetworkPolicyList()) {
            responsePolicys.add(new TungstenFabricPolicyResponse(networkPolicy, zone));
        }
        this.policys = responsePolicys;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
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

    public List<TungstenFabricVmResponse> getVms() {
        return vms;
    }

    public void setVms(final List<TungstenFabricVmResponse> vms) {
        this.vms = vms;
    }

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
}
