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
package org.apache.cloudstack.api.response;

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.rules.FirewallRule;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=FirewallRule.class)
@SuppressWarnings("unused")
public class FirewallRuleResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the port forwarding rule")
    private String id;

    @SerializedName(ApiConstants.PRIVATE_START_PORT) @Param(description = "the starting port of port forwarding rule's private port range")
    private String privateStartPort;

    @SerializedName(ApiConstants.PRIVATE_END_PORT) @Param(description = "the ending port of port forwarding rule's private port range")
    private String privateEndPort;

    @SerializedName(ApiConstants.PROTOCOL) @Param(description="the protocol of the port forwarding rule")
    private String protocol;

    @SerializedName(ApiConstants.PUBLIC_START_PORT) @Param(description="the starting port of port forwarding rule's public port range")
    private String publicStartPort;

    @SerializedName(ApiConstants.PUBLIC_END_PORT)  @Param(description = "the ending port of port forwarding rule's private port range")
    private String publicEndPort;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="the VM ID for the port forwarding rule")
    private String virtualMachineId;

    @SerializedName("virtualmachinename") @Param(description="the VM name for the port forwarding rule")
    private String virtualMachineName;

    @SerializedName("virtualmachinedisplayname") @Param(description="the VM display name for the port forwarding rule")
    private String virtualMachineDisplayName;

    @SerializedName(ApiConstants.IP_ADDRESS_ID) @Param(description="the public ip address id for the port forwarding rule")
    private String publicIpAddressId;

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the public ip address for the port forwarding rule")
    private String publicIpAddress;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the rule")
    private String state;

    @SerializedName(ApiConstants.CIDR_LIST) @Param(description="the cidr list to forward traffic from")
    private String cidrList;

    @SerializedName(ApiConstants.TAGS)  @Param(description="the list of resource tags associated with the rule", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    @SerializedName(ApiConstants.VM_GUEST_IP) @Param(description="the vm ip address for the port forwarding rule")
    private String destNatVmIp;


    public String getDestNatVmIp() {
        return destNatVmIp;
    }

    public void setDestNatVmIp(String destNatVmIp) {
        this.destNatVmIp = destNatVmIp;
    }


    @Override
    public String getObjectId() {
        return this.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPrivateStartPort() {
        return privateStartPort;
    }

    public String getPrivateEndPort() {
        return privateEndPort;
    }

    public void setPrivateStartPort(String privatePort) {
        this.privateStartPort = privatePort;
    }

    public void setPrivateEndPort(String privatePort) {
        this.privateEndPort = privatePort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPublicStartPort() {
        return publicStartPort;
    }

    public String getPublicEndPort() {
        return publicEndPort;
    }

    public void setPublicStartPort(String publicPort) {
        this.publicStartPort = publicPort;
    }

    public void setPublicEndPort(String publicPort) {
        this.publicEndPort = publicPort;
    }

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }

    public String getVirtualMachineDisplayName() {
        return virtualMachineDisplayName;
    }

    public void setVirtualMachineDisplayName(String virtualMachineDisplayName) {
        this.virtualMachineDisplayName = virtualMachineDisplayName;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPublicIpAddressId() {
        return publicIpAddressId;
    }

    public void setPublicIpAddressId(String publicIpAddressId) {
        this.publicIpAddressId = publicIpAddressId;
    }

    public String getCidrList() {
        return cidrList;
    }

    public void setCidrList(String cidrs) {
        this.cidrList = cidrs;
    }

    public void setTags(List<ResourceTagResponse> tags) {
        this.tags = tags;
    }
}
