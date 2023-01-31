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

import com.cloud.serializer.Param;
import com.cloud.vm.Nic;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = Nic.class)
public class NicResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the nic")
    private String id;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "the ID of the corresponding network")
    private String networkId;

    @SerializedName(ApiConstants.NETWORK_NAME)
    @Param(description = "the name of the corresponding network")
    private String networkName;

    @SerializedName(ApiConstants.NETMASK)
    @Param(description = "the netmask of the nic")
    private String netmask;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "the gateway of the nic")
    private String gateway;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the ip address of the nic")
    private String ipaddress;

    @SerializedName(ApiConstants.ISOLATION_URI)
    @Param(description = "the isolation uri of the nic")
    private String isolationUri;

    @SerializedName(ApiConstants.BROADCAST_URI)
    @Param(description = "the broadcast uri of the nic")
    private String broadcastUri;

    @SerializedName(ApiConstants.TRAFFIC_TYPE)
    @Param(description = "the traffic type of the nic")
    private String trafficType;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "the type of the nic")
    private String type;

    @SerializedName(ApiConstants.IS_DEFAULT)
    @Param(description = "true if nic is default, false otherwise")
    private Boolean isDefault;

    @SerializedName(ApiConstants.MAC_ADDRESS)
    @Param(description = "true if nic is default, false otherwise")
    private String macAddress;

    @SerializedName(ApiConstants.IP6_GATEWAY)
    @Param(description = "the gateway of IPv6 network")
    private String ip6Gateway;

    @SerializedName(ApiConstants.IP6_CIDR)
    @Param(description = "the cidr of IPv6 network")
    private String ip6Cidr;

    @SerializedName(ApiConstants.IP6_ADDRESS)
    @Param(description = "the IPv6 address of network")
    private String ip6Address;

    @SerializedName(ApiConstants.SECONDARY_IP)
    @Param(description = "the Secondary ipv4 addr of nic")
    private List<NicSecondaryIpResponse> secondaryIps;

    @SerializedName(ApiConstants.EXTRA_DHCP_OPTION)
    @Param(description = "the extra dhcp options on the nic", since = "4.11.0")
    private List<NicExtraDhcpOptionResponse> extraDhcpOptions;

    @SerializedName(ApiConstants.DEVICE_ID)
    @Param(description = "device id for the network when plugged into the virtual machine", since = "4.4")
    private String deviceId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "Id of the vm to which the nic belongs")
    private String vmId;

    @SerializedName(ApiConstants.NSX_LOGICAL_SWITCH)
    @Param(description = "Id of the NSX Logical Switch (if NSX based), null otherwise", since="4.6.0")
    private String nsxLogicalSwitch;

    @SerializedName(ApiConstants.NSX_LOGICAL_SWITCH_PORT)
    @Param(description = "Id of the NSX Logical Switch Port (if NSX based), null otherwise", since="4.6.0")
    private String nsxLogicalSwitchPort;

    @SerializedName(ApiConstants.VLAN_ID)
    @Param(description = "ID of the VLAN/VNI if available", since="4.14.0")
    private Integer vlanId;

    @SerializedName(ApiConstants.ISOLATED_PVLAN)
    @Param(description = "the isolated private VLAN if available", since="4.14.0")
    private Integer isolatedPvlanId;

    @SerializedName(ApiConstants.ISOLATED_PVLAN_TYPE)
    @Param(description = "the isolated private VLAN type if available", since="4.14.0")
    private String isolatedPvlanType;

    @SerializedName(ApiConstants.ADAPTER_TYPE)
    @Param(description = "Type of adapter if available", since="4.14.0")
    private String adapterType;

    @SerializedName(ApiConstants.IP_ADDRESSES)
    @Param(description = "IP addresses associated with NIC found for unmanaged VM", since="4.14.0")
    private List<String> ipAddresses;

    @SerializedName(ApiConstants.MTU)
    @Param(description = "MTU configured on the NIC", since="4.18.0")
    private Integer mtu;

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNetworkid(String networkid) {
        this.networkId = networkid;
    }

    public void setNetworkName(String networkname) {
        this.networkName = networkname;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public void setIsolationUri(String isolationUri) {
        this.isolationUri = isolationUri;
    }

    public void setBroadcastUri(String broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setIp6Gateway(String ip6Gateway) {
        this.ip6Gateway = ip6Gateway;
    }

    public void setIp6Cidr(String ip6Cidr) {
        this.ip6Cidr = ip6Cidr;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setExtraDhcpOptions(List<NicExtraDhcpOptionResponse> extraDhcpOptions) {
        this.extraDhcpOptions = extraDhcpOptions;
    }

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "Id of the vpc to which the nic belongs")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "name of the vpc to which the nic belongs")
    private String vpcName;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String oid = this.getId();
        result = prime * result + ((oid == null) ? 0 : oid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NicResponse other = (NicResponse)obj;
        String oid = this.getId();
        if (oid == null) {
            if (other.getId() != null)
                return false;
        } else if (!oid.equals(other.getId()))
            return false;
        return true;
    }

    public void setSecondaryIps(List<NicSecondaryIpResponse> ipList) {
        this.secondaryIps = ipList;
    }

    public void setNsxLogicalSwitch(String nsxLogicalSwitch) {
        this.nsxLogicalSwitch = nsxLogicalSwitch;
    }

    public void setNsxLogicalSwitchPort(String nsxLogicalSwitchPort) {
        this.nsxLogicalSwitchPort = nsxLogicalSwitchPort;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public String getIsolationUri() {
        return isolationUri;
    }

    public String getBroadcastUri() {
        return broadcastUri;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public String getType() {
        return type;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public String getIp6Gateway() {
        return ip6Gateway;
    }

    public String getIp6Cidr() {
        return ip6Cidr;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public List<NicSecondaryIpResponse> getSecondaryIps() {
        return secondaryIps;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getVmId() {
        return vmId;
    }

    public String getNsxLogicalSwitch() {
        return nsxLogicalSwitch;
    }

    public String getNsxLogicalSwitchPort() {
        return nsxLogicalSwitchPort;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public Integer getIsolatedPvlanId() {
        return isolatedPvlanId;
    }

    public void setIsolatedPvlanId(Integer isolatedPvlanId) {
        this.isolatedPvlanId = isolatedPvlanId;
    }

    public String getIsolatedPvlanType() {
        return isolatedPvlanType;
    }

    public void setIsolatedPvlanType(String isolatedPvlanType) {
        this.isolatedPvlanType = isolatedPvlanType;
    }

    public String getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(String adapterType) {
        this.adapterType = adapterType;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

    public String getVpcId() {
        return vpcId;
    }
}
