/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import java.util.Date;
import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class UserVmResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the virtual machine")
    private IdentityProxy id = new IdentityProxy("vm_instance");

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the virtual machine")
    private String name;

    @SerializedName("displayname") @Param(description="user generated name. The name of the virtual machine is returned if no displayname exists.")
    private String displayName;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the virtual machine")
    private String accountName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the vm")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the vm")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the domain in which the virtual machine exists")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the name of the domain in which the virtual machine exists")
    private String domainName;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date when this virtual machine was created")
    private Date created;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the virtual machine")
    private String state;

    @SerializedName(ApiConstants.HA_ENABLE) @Param(description="true if high-availability is enabled, false otherwise")
    private Boolean haEnable;

    @SerializedName(ApiConstants.GROUP_ID) @Param(description="the group ID of the virtual machine")
    private IdentityProxy groupId = new IdentityProxy("instance_group");

    @SerializedName(ApiConstants.GROUP) @Param(description="the group name of the virtual machine")
    private String group;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the ID of the availablility zone for the virtual machine")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the name of the availability zone for the virtual machine")
    private String zoneName;

    @SerializedName(ApiConstants.HOST_ID) @Param(description="the ID of the host for the virtual machine")
    private IdentityProxy hostId = new IdentityProxy("host");

    @SerializedName("hostname") @Param(description="the name of the host for the virtual machine")
    private String hostName;

    @SerializedName(ApiConstants.TEMPLATE_ID) @Param(description="the ID of the template for the virtual machine. A -1 is returned if the virtual machine was created from an ISO file.")
    private IdentityProxy templateId = new IdentityProxy("vm_template");

    @SerializedName("templatename") @Param(description="the name of the template for the virtual machine")
    private String templateName;

    @SerializedName("templatedisplaytext") @Param(description="	an alternate display text of the template for the virtual machine")
    private String templateDisplayText;

    @SerializedName(ApiConstants.PASSWORD_ENABLED) @Param(description="true if the password rest feature is enabled, false otherwise")
    private Boolean passwordEnabled;

    @SerializedName("isoid") @Param(description="the ID of the ISO attached to the virtual machine")
    private IdentityProxy isoId = new IdentityProxy("vm_template");

    @SerializedName("isoname") @Param(description="the name of the ISO attached to the virtual machine")
    private String isoName;

    @SerializedName("isodisplaytext") @Param(description="an alternate display text of the ISO attached to the virtual machine")
    private String isoDisplayText;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID) @Param(description="the ID of the service offering of the virtual machine")
    private IdentityProxy serviceOfferingId = new IdentityProxy("disk_offering");

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
    private IdentityProxy guestOsId = new IdentityProxy("guest_os");

    @SerializedName("rootdeviceid") @Param(description="device ID of the root volume")
    private Long rootDeviceId;

    @SerializedName("rootdevicetype") @Param(description="device type of the root volume")
    private String rootDeviceType;

    @SerializedName("securitygroup") @Param(description="list of security groups associated with the virtual machine", responseObject = SecurityGroupResponse.class)
    private List<SecurityGroupResponse> securityGroupList;

    @SerializedName(ApiConstants.PASSWORD) @Param(description="the password (if exists) of the virtual machine")
    private String password;

    @SerializedName("nic")  @Param(description="the list of nics associated with vm", responseObject = NicResponse.class)
    private List<NicResponse> nics;
    
    @SerializedName("hypervisor") @Param(description="the hypervisor on which the template runs")
    private String hypervisor;
    
    @SerializedName(ApiConstants.PUBLIC_IP_ID) @Param(description="public IP address id associated with vm via Static nat rule")
    private IdentityProxy publicIpId = new IdentityProxy("user_ip_address");
    
    @SerializedName(ApiConstants.PUBLIC_IP) @Param(description="public IP address id associated with vm via Static nat rule")
    private String publicIp;
    
    @SerializedName(ApiConstants.INSTANCE_NAME) @Param(description="instance name of the user vm; this parameter is returned to the ROOT admin only")
    private String instanceName;

	public void setHypervisor(String hypervisor) {
		this.hypervisor = hypervisor;
	}
    
    public void setId(Long id) {
        this.id.setValue(id);
    }

    public Long getId() {
        return this.id.getValue();
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

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
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

    public void setGroupId(Long groupId) {
        this.groupId.setValue(groupId);
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setHostId(Long hostId) {
        this.hostId.setValue(hostId);
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setTemplateId(Long templateId) {
        this.templateId.setValue(templateId);
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

    public void setIsoId(Long isoId) {
        this.isoId.setValue(isoId);
    }

    public void setIsoName(String isoName) {
        this.isoName = isoName;
    }

    public void setIsoDisplayText(String isoDisplayText) {
        this.isoDisplayText = isoDisplayText;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId.setValue(serviceOfferingId);
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

    public void setGuestOsId(Long guestOsId) {
        this.guestOsId.setValue(guestOsId);
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

/*   
    public void setJobId(Long jobId) {
        super.setJobId(jobId);
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }
*/
    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public void setNics(List<NicResponse> nics) {
        this.nics = nics;
    }

    public void setSecurityGroupList(List<SecurityGroupResponse> securityGroups) {
        this.securityGroupList = securityGroups;
    }
    
    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }
    
    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public void setPublicIpId(Long publicIpId) {
        this.publicIpId.setValue(publicIpId);
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
