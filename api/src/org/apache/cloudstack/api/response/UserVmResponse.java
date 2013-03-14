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

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.router.VirtualRouter;
import com.cloud.serializer.Param;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value={VirtualMachine.class, UserVm.class, VirtualRouter.class})
public class UserVmResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the virtual machine")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the virtual machine")
    private String name;

    @SerializedName("displayname") @Param(description="user generated name. The name of the virtual machine is returned if no displayname exists.")
    private String displayName;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the virtual machine")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the vm")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the vm")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the domain in which the virtual machine exists")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the name of the domain in which the virtual machine exists")
    private String domainName;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date when this virtual machine was created")
    private Date created;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the virtual machine")
    private String state;

    @SerializedName(ApiConstants.HA_ENABLE) @Param(description="true if high-availability is enabled, false otherwise")
    private Boolean haEnable;

    @SerializedName(ApiConstants.GROUP_ID) @Param(description="the group ID of the virtual machine")
    private String groupId;

    @SerializedName(ApiConstants.GROUP) @Param(description="the group name of the virtual machine")
    private String group;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the ID of the availablility zone for the virtual machine")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the name of the availability zone for the virtual machine")
    private String zoneName;

    @SerializedName(ApiConstants.HOST_ID) @Param(description="the ID of the host for the virtual machine")
    private String hostId;

    @SerializedName("hostname") @Param(description="the name of the host for the virtual machine")
    private String hostName;

    @SerializedName(ApiConstants.TEMPLATE_ID) @Param(description="the ID of the template for the virtual machine. A -1 is returned if the virtual machine was created from an ISO file.")
    private String templateId;

    @SerializedName("templatename") @Param(description="the name of the template for the virtual machine")
    private String templateName;

    @SerializedName("templatedisplaytext") @Param(description=" an alternate display text of the template for the virtual machine")
    private String templateDisplayText;

    @SerializedName(ApiConstants.PASSWORD_ENABLED) @Param(description="true if the password rest feature is enabled, false otherwise")
    private Boolean passwordEnabled;

    @SerializedName("isoid") @Param(description="the ID of the ISO attached to the virtual machine")
    private String isoId;

    @SerializedName("isoname") @Param(description="the name of the ISO attached to the virtual machine")
    private String isoName;

    @SerializedName("isodisplaytext") @Param(description="an alternate display text of the ISO attached to the virtual machine")
    private String isoDisplayText;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID) @Param(description="the ID of the service offering of the virtual machine")
    private String serviceOfferingId;

    @SerializedName("serviceofferingname") @Param(description="the name of the service offering of the virtual machine")
    private String serviceOfferingName;

    @SerializedName("forvirtualnetwork") @Param(description="the virtual network for the service offering")
    private Boolean forVirtualNetwork;

    @SerializedName(ApiConstants.CPU_NUMBER) @Param(description="the number of cpu this virtual machine is running with")
    private Integer cpuNumber;

    @SerializedName(ApiConstants.CPU_SPEED) @Param(description="the speed of each cpu")
    private Integer cpuSpeed;

    @SerializedName(ApiConstants.MEMORY) @Param(description="the memory allocated for the virtual machine")
    private Integer memory;

    @SerializedName("cpuused") @Param(description="the amount of the vm's CPU currently used")
    private String cpuUsed;

    @SerializedName("networkkbsread") @Param(description="the incoming network traffic on the vm")
    private Long networkKbsRead;

    @SerializedName("networkkbswrite") @Param(description="the outgoing network traffic on the host")
    private Long networkKbsWrite;

    @SerializedName("guestosid") @Param(description="Os type ID of the virtual machine")
    private String guestOsId;

    @SerializedName("rootdeviceid") @Param(description="device ID of the root volume")
    private Long rootDeviceId;

    @SerializedName("rootdevicetype") @Param(description="device type of the root volume")
    private String rootDeviceType;

    @SerializedName("securitygroup") @Param(description="list of security groups associated with the virtual machine", responseObject = SecurityGroupResponse.class)
    private Set<SecurityGroupResponse> securityGroupList;

    @SerializedName(ApiConstants.PASSWORD) @Param(description="the password (if exists) of the virtual machine")
    private String password;

    @SerializedName("nic")  @Param(description="the list of nics associated with vm", responseObject = NicResponse.class)
    private Set<NicResponse> nics;

    @SerializedName("hypervisor") @Param(description="the hypervisor on which the template runs")
    private String hypervisor;

    @SerializedName(ApiConstants.PUBLIC_IP_ID) @Param(description="public IP address id associated with vm via Static nat rule")
    private String publicIpId;

    @SerializedName(ApiConstants.PUBLIC_IP) @Param(description="public IP address id associated with vm via Static nat rule")
    private String publicIp;

    @SerializedName(ApiConstants.INSTANCE_NAME) @Param(description="instance name of the user vm; this parameter is returned to the ROOT admin only", since="3.0.1")
    private String instanceName;

    @SerializedName(ApiConstants.TAGS)  @Param(description="the list of resource tags associated with vm", responseObject = ResourceTagResponse.class)
    private Set<ResourceTagResponse> tags;

    @SerializedName(ApiConstants.SSH_KEYPAIR) @Param(description="ssh key-pair")
    private String keyPairName;

    public UserVmResponse(){
        securityGroupList = new LinkedHashSet<SecurityGroupResponse>();
        nics = new LinkedHashSet<NicResponse>();
        tags = new LinkedHashSet<ResourceTagResponse>();
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }



    @Override
    public String getObjectId() {
        return this.getId();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setHaEnable(Boolean haEnable) {
        this.haEnable = haEnable;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public void setTemplateDisplayText(String templateDisplayText) {
        this.templateDisplayText = templateDisplayText;
    }

    public void setPasswordEnabled(Boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }

    public void setIsoId(String isoId) {
        this.isoId = isoId;
    }

    public void setIsoName(String isoName) {
        this.isoName = isoName;
    }

    public void setIsoDisplayText(String isoDisplayText) {
        this.isoDisplayText = isoDisplayText;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public void setCpuNumber(Integer cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public void setCpuSpeed(Integer cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public void setCpuUsed(String cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public void setNetworkKbsRead(Long networkKbsRead) {
        this.networkKbsRead = networkKbsRead;
    }

    public void setNetworkKbsWrite(Long networkKbsWrite) {
        this.networkKbsWrite = networkKbsWrite;
    }

    public void setGuestOsId(String guestOsId) {
        this.guestOsId = guestOsId;
    }

    public void setRootDeviceId(Long rootDeviceId) {
        this.rootDeviceId = rootDeviceId;
    }

    public void setRootDeviceType(String rootDeviceType) {
        this.rootDeviceType = rootDeviceType;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public void setNics(Set<NicResponse> nics) {
        this.nics = nics;
    }

    public void addNic(NicResponse nic) {
        this.nics.add(nic);
    }

    public void setSecurityGroupList(Set<SecurityGroupResponse> securityGroups) {
        this.securityGroupList = securityGroups;
    }

    public void addSecurityGroup(SecurityGroupResponse securityGroup){
        this.securityGroupList.add(securityGroup);
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setPublicIpId(String publicIpId) {
        this.publicIpId = publicIpId;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void addTag(ResourceTagResponse tag){
        this.tags.add(tag);
    }

    public void setKeyPairName(String keyPairName) {
        this.keyPairName = keyPairName;
    }

}
