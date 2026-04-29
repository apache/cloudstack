//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.response;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.IpAddress;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = IpAddress.class)
@SuppressWarnings("unused")
public class AcquireIPAddressResponse  extends BaseResponse implements ControlledEntityResponse {
 @SerializedName(ApiConstants.ID)
 @Param(description = "Public IP address id")
 private String id;

 @SerializedName(ApiConstants.IP_ADDRESS)
 @Param(description = "Public IP address")
 private String ipAddress;

 @SerializedName("allocated")
 @Param(description = "Date the public IP address was acquired")
 private Date allocated;

 @SerializedName(ApiConstants.ZONE_ID)
 @Param(description = "The ID of the zone the public IP address belongs to")
 private String zoneId;

 @SerializedName(ApiConstants.ZONE_NAME)
 @Param(description = "The name of the zone the public IP address belongs to")
 private String zoneName;

 @SerializedName("issourcenat")
 @Param(description = "True if the IP address is a source NAT address, false otherwise")
 private Boolean sourceNat;

 @SerializedName(ApiConstants.ACCOUNT)
 @Param(description = "The Account the public IP address is associated with")
 private String accountName;

 @SerializedName(ApiConstants.PROJECT_ID)
 @Param(description = "The project id of the IP address")
 private String projectId;

 @SerializedName(ApiConstants.PROJECT)
 @Param(description = "The project name of the address")
 private String projectName;

 @SerializedName(ApiConstants.DOMAIN_ID)
 @Param(description = "The domain ID the public IP address is associated with")
 private String domainId;

 @SerializedName(ApiConstants.DOMAIN)
 @Param(description = "The domain the public IP address is associated with")
 private String domainName;

 @SerializedName(ApiConstants.DOMAIN_PATH)
 @Param(description = "path of the domain to which the public IP address belongs", since = "4.19.2.0")
 private String domainPath;

 @SerializedName(ApiConstants.FOR_VIRTUAL_NETWORK)
 @Param(description = "The virtual Network for the IP address")
 private Boolean forVirtualNetwork;

 @SerializedName(ApiConstants.VLAN_ID)
 @Param(description = "The ID of the VLAN associated with the IP address." + " This parameter is visible to ROOT admins only")
 private String vlanId;

 @SerializedName("vlanname")
 @Param(description = "The VLAN associated with the IP address")
 private String vlanName;

 @SerializedName("isstaticnat")
 @Param(description = "True if this IP is for static NAT, false otherwise")
 private Boolean staticNat;

 @SerializedName(ApiConstants.IS_SYSTEM)
 @Param(description = "True if this IP is system IP (was allocated as a part of deployVm or createLbRule)")
 private Boolean isSystem;

 @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
 @Param(description = "Instance id the IP address is assigned to (not null only for static NAT IP)")
 private String virtualMachineId;

 @SerializedName("vmipaddress")
 @Param(description = "Instance (DNAT) IP address (not null only for static NAT IP)")
 private String virtualMachineIp;

 @SerializedName("virtualmachinename")
 @Param(description = "Instance name the IP address is assigned to (not null only for static NAT IP)")
 private String virtualMachineName;

 @SerializedName("virtualmachinedisplayname")
 @Param(description = "Instance display name the IP address is assigned to (not null only for static NAT IP)")
 private String virtualMachineDisplayName;

 @SerializedName(ApiConstants.ASSOCIATED_NETWORK_ID)
 @Param(description = "The ID of the Network associated with the IP address")
 private String associatedNetworkId;

 @SerializedName(ApiConstants.ASSOCIATED_NETWORK_NAME)
 @Param(description = "The name of the Network associated with the IP address")
 private String associatedNetworkName;

 @SerializedName(ApiConstants.NETWORK_ID)
 @Param(description = "The ID of the Network where IP belongs to")
 private String networkId;

 @SerializedName(ApiConstants.STATE)
 @Param(description = "State of the IP address. Can be: Allocating, Allocated and Releasing")
 private String state;

 @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
 @Param(description = "The physical Network this belongs to")
 private String physicalNetworkId;

 @SerializedName(ApiConstants.PURPOSE)
 @Param(description = "Purpose of the IP address. In Acton this value is not null for IPs with isSystem=true, and can have either StaticNat or LB value")
 private String purpose;

 @SerializedName(ApiConstants.VPC_ID)
 @Param(description = "VPC the IP belongs to")
 private String vpcId;
 @SerializedName(ApiConstants.TAGS)
 @Param(description = "The list of resource tags associated with IP address", responseObject = ResourceTagResponse.class)
 private List<ResourceTagResponse> tags;

 @SerializedName(ApiConstants.IS_PORTABLE)
 @Param(description = "Is public IP portable across the zones")
 private Boolean isPortable;

 @SerializedName(ApiConstants.FOR_DISPLAY)
 @Param(description = "Is public IP for display to the regular User", since = "4.4", authorized = {RoleType.Admin})
 private Boolean forDisplay;

 public void setIpAddress(String ipAddress) {
     this.ipAddress = ipAddress;
 }

 @Override
 public String getObjectId() {
     return this.getId();
 }

 public void setAllocated(Date allocated) {
     this.allocated = allocated;
 }

 public void setZoneId(String zoneId) {
     this.zoneId = zoneId;
 }

 public void setZoneName(String zoneName) {
     this.zoneName = zoneName;
 }

 public void setSourceNat(Boolean sourceNat) {
     this.sourceNat = sourceNat;
 }

 @Override
 public void setAccountName(String accountName) {
     this.accountName = accountName;
 }

 @Override
 public void setDomainId(String domainId) {
     this.domainId = domainId;
 }

 @Override
 public void setDomainName(String domainName) {
     this.domainName = domainName;
 }

 @Override
 public void setDomainPath(String domainPath) {
  this.domainPath = domainPath;
 }

 public void setForVirtualNetwork(Boolean forVirtualNetwork) {
     this.forVirtualNetwork = forVirtualNetwork;
 }

 public void setVlanId(String vlanId) {
     this.vlanId = vlanId;
 }

 public void setVlanName(String vlanName) {
     this.vlanName = vlanName;
 }

 public void setStaticNat(Boolean staticNat) {
     this.staticNat = staticNat;
 }

 public void setAssociatedNetworkId(String networkId) {
     this.associatedNetworkId = networkId;
 }

 public void setNetworkId(String networkId) {
     this.networkId = networkId;
 }

 public void setVirtualMachineId(String virtualMachineId) {
     this.virtualMachineId = virtualMachineId;
 }

 public void setVirtualMachineIp(String virtualMachineIp) {
     this.virtualMachineIp = virtualMachineIp;
 }

 public void setVirtualMachineName(String virtualMachineName) {
     this.virtualMachineName = virtualMachineName;
 }

 public void setVirtualMachineDisplayName(String virtualMachineDisplayName) {
     this.virtualMachineDisplayName = virtualMachineDisplayName;
 }

 public String getId() {
     return id;
 }

 public void setId(String id) {
     this.id = id;
 }

 public void setState(String state) {
     this.state = state;
 }

 @Override
 public void setProjectId(String projectId) {
     this.projectId = projectId;
 }

 @Override
 public void setProjectName(String projectName) {
     this.projectName = projectName;
 }

 public void setPhysicalNetworkId(String physicalNetworkId) {
     this.physicalNetworkId = physicalNetworkId;
 }

 public void setIsSystem(Boolean isSystem) {
     this.isSystem = isSystem;
 }

 public void setPurpose(String purpose) {
     this.purpose = purpose;
 }

 public void setVpcId(String vpcId) {
     this.vpcId = vpcId;
 }

 public void setTags(List<ResourceTagResponse> tags) {
     this.tags = tags;
 }

 public void setAssociatedNetworkName(String associatedNetworkName) {
     this.associatedNetworkName = associatedNetworkName;
 }

 public void setPortable(Boolean portable) {
     this.isPortable = portable;
 }

 public void setForDisplay(Boolean forDisplay) {
     this.forDisplay = forDisplay;
 }
}
