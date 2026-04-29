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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.network.router.VirtualRouter;
import com.cloud.serializer.Param;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = {VirtualMachine.class, UserVm.class, VirtualRouter.class})
public class UserVmResponse extends BaseResponseWithTagInformation implements ControlledViewEntityResponse, SetResourceIconResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the Instance")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the Instance")
    private String name;

    @SerializedName("displayname")
    @Param(description = "User generated name. The name of the Instance is returned if no displayname exists.")
    private String displayName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The Account associated with the Instance")
    private String accountName;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "The User's ID who deployed the Instance")
    private String userId;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "The User's name who deployed the Instance")
    private String userName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project ID of the Instance")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the Instance")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The ID of the domain in which the Instance exists")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The name of the domain in which the Instance exists")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "Path of the domain in which the virtual machine exists", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date when this Instance was created")
    private Date created;

    @SerializedName("lastupdated")
    @Param(description = "The date when this Instance was updated last time", since="4.16.0")
    private Date lastUpdated;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the Instance")
    private String state;

    @SerializedName(ApiConstants.HA_ENABLE)
    @Param(description = "True if high-availability is enabled, false otherwise")
    private Boolean haEnable;

    @SerializedName(ApiConstants.GROUP_ID)
    @Param(description = "The group ID of the Instance")
    private String groupId;

    @SerializedName(ApiConstants.GROUP)
    @Param(description = "The group name of the Instance")
    private String group;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "The ID of the availability zone for the Instance")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "The name of the availability zone for the Instance")
    private String zoneName;

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "The ID of the host for the Instance")
    private String hostId;

    @SerializedName("hostname")
    @Param(description = "The name of the host for the Instance")
    private String hostName;

    @SerializedName(ApiConstants.HOST_CONTROL_STATE)
    @Param(description = "The control state of the host for the Instance")
    private String hostControlState;

    @SerializedName(ApiConstants.TEMPLATE_ID)
    @Param(description = "The ID of the Template for the Instance. A -1 is returned if the Instance was created from an ISO file.")
    private String templateId;

    @SerializedName("templatename")
    @Param(description = "The name of the Template for the Instance")
    private String templateName;

    @SerializedName(ApiConstants.TEMPLATE_TYPE)
    @Param(description = "The type of the template for the virtual machine", since = "4.19.0")
    private String templateType;

    @SerializedName(ApiConstants.TEMPLATE_FORMAT)
    @Param(description = "The format of the template for the virtual machine", since = "4.19.1")
    private String templateFormat;

    @SerializedName("templatedisplaytext")
    @Param(description = "An alternate display text of the Template for the Instance")
    private String templateDisplayText;

    @SerializedName(ApiConstants.PASSWORD_ENABLED)
    @Param(description = "True if the password rest feature is enabled, false otherwise")
    private Boolean passwordEnabled;

    @SerializedName("isoid")
    @Param(description = "The ID of the ISO attached to the Instance")
    private String isoId;

    @SerializedName("isoname")
    @Param(description = "The name of the ISO attached to the Instance")
    private String isoName;

    @SerializedName("isodisplaytext")
    @Param(description = "An alternate display text of the ISO attached to the Instance")
    private String isoDisplayText;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "The ID of the service offering of the Instance")
    private String serviceOfferingId;

    @SerializedName("serviceofferingname")
    @Param(description = "The name of the service offering of the Instance")
    private String serviceOfferingName;

    @SerializedName(ApiConstants.DISK_OFFERING_ID)
    @Param(description = "The ID of the disk offering of the Instance. This parameter should not be used for retrieving disk offering details of DATA volumes. Use listVolumes API instead", since = "4.4")
    private String diskOfferingId;

    @SerializedName("diskofferingname")
    @Param(description = "The name of the disk offering of the Instance. This parameter should not be used for retrieving disk offering details of DATA volumes. Use listVolumes API instead", since = "4.4")
    private String diskOfferingName;

    @SerializedName(ApiConstants.GPU_CARD_ID)
    @Param(description = "the ID of the gpu card to which service offering is linked", since = "4.21")
    private String gpuCardId;

    @SerializedName(ApiConstants.GPU_CARD_NAME)
    @Param(description = "the name of the gpu card to which service offering is linked", since = "4.21")
    private String gpuCardName;

    @SerializedName(ApiConstants.VGPU_PROFILE_ID)
    @Param(description = "the ID of the vgpu profile to which service offering is linked", since = "4.21")
    private String vgpuProfileId;

    @SerializedName(ApiConstants.VGPU_PROFILE_NAME)
    @Param(description = "the name of the vgpu profile to which service offering is linked", since = "4.21")
    private String vgpuProfileName;

    @SerializedName(ApiConstants.VIDEORAM)
    @Param(description = "the video RAM size in MB")
    private Long videoRam;

    @SerializedName(ApiConstants.MAXHEADS)
    @Param(description = "the maximum number of display heads")
    private Long maxHeads;

    @SerializedName(ApiConstants.MAXRESOLUTIONX)
    @Param(description = "the maximum X resolution")
    private Long maxResolutionX;

    @SerializedName(ApiConstants.MAXRESOLUTIONY)
    @Param(description = "the maximum Y resolution")
    private Long maxResolutionY;

    @SerializedName(ApiConstants.GPU_COUNT)
    @Param(description = "the count of GPUs on the virtual machine", since = "4.21")
    private Integer gpuCount;

    @SerializedName(ApiConstants.BACKUP_OFFERING_ID)
    @Param(description = "The ID of the backup offering of the Instance", since = "4.14")
    private String backupOfferingId;

    @SerializedName(ApiConstants.BACKUP_OFFERING_NAME)
    @Param(description = "The name of the backup offering of the Instance", since = "4.14")
    private String backupOfferingName;

    @SerializedName("forvirtualnetwork")
    @Param(description = "The virtual Network for the service offering")
    private Boolean forVirtualNetwork;

    @SerializedName(ApiConstants.CPU_NUMBER)
    @Param(description = "The number of vCPUs this Instance is using")
    private Integer cpuNumber;

    @SerializedName(ApiConstants.CPU_SPEED)
    @Param(description = "The speed of each vCPU")
    private Integer cpuSpeed;

    @SerializedName(ApiConstants.MEMORY)
    @Param(description = "The memory allocated for the Instance")
    private Integer memory;

    @SerializedName(ApiConstants.VGPU)
    @Param(description = "The vGPU type used by the Instance", since = "4.4")
    private String vgpu;

    @SerializedName("cpuused")
    @Param(description = "The amount of the Instance's CPU currently used")
    private String cpuUsed;

    @SerializedName("networkkbsread")
    @Param(description = "The incoming Network traffic on the Instance in KiB")
    private Long networkKbsRead;

    @SerializedName("networkkbswrite")
    @Param(description = "The outgoing Network traffic on the host in KiB")
    private Long networkKbsWrite;

    @SerializedName(ApiConstants.DISK_KBS_READ)
    @Param(description = "The Instance's disk read in KiB")
    private Long diskKbsRead;

    @SerializedName(ApiConstants.DISK_KBS_WRITE)
    @Param(description = "The Instance's disk write in KiB")
    private Long diskKbsWrite;

    @SerializedName("memorykbs")
    @Param(description = "The memory used by the Instance in KiB")
    private Long memoryKBs;

    @SerializedName("memoryintfreekbs")
    @Param(description = "The internal memory (KiB) that's free in Instance or zero if it can not be calculated")
    private Long memoryIntFreeKBs;

    @SerializedName("memorytargetkbs")
    @Param(description = "The target memory in Instance (KiB)")
    private Long memoryTargetKBs;

    @SerializedName(ApiConstants.DISK_IO_READ)
    @Param(description = "The read (IO) of disk on the Instance")
    private Long diskIORead;

    @SerializedName(ApiConstants.DISK_IO_WRITE)
    @Param(description = "The write (IO) of disk on the Instance")
    private Long diskIOWrite;

    @SerializedName("guestosid")
    @Param(description = "OS type ID of the Instance")
    private String guestOsId;

    @SerializedName("rootdeviceid")
    @Param(description = "Device ID of the root volume")
    private Long rootDeviceId;

    @SerializedName("rootdevicetype")
    @Param(description = "Device type of the root volume")
    private String rootDeviceType;

    @SerializedName("securitygroup")
    @Param(description = "List of security groups associated with the Instance", responseObject = SecurityGroupResponse.class)
    private Set<SecurityGroupResponse> securityGroupList;

    @SerializedName(ApiConstants.PASSWORD)
    @Param(description = "The password (if exists) of the Instance", isSensitive = true)
    private String password;

    @SerializedName("nic")
    @Param(description = "The list of NICs associated with Instance", responseObject = NicResponse.class)
    private Set<NicResponse> nics;

    @SerializedName("hypervisor")
    @Param(description = "The hypervisor on which the Template runs")
    private String hypervisor;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the VM's primary IP address")
    private String ipAddress;

    @SerializedName(ApiConstants.PUBLIC_IP_ID)
    @Param(description = "Public IP address id associated with Instance via Static NAT rule")
    private String publicIpId;

    @SerializedName(ApiConstants.PUBLIC_IP)
    @Param(description = "Public IP address id associated with Instance via Static NAT rule")
    private String publicIp;

    @SerializedName(ApiConstants.INSTANCE_NAME)
    @Param(description = "Instance name of the user Instance; this parameter is returned to the ROOT admin only", since = "3.0.1")
    private String instanceName;

    transient Set<Long> tagIds;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "Instance details in key/value pairs.", since = "4.2.1")
    private Map details;

    @SerializedName("readonlydetails")
    @Param(description = "List of read-only Instance details as comma separated string.", since = "4.16.0")
    private String readOnlyDetails;

    @SerializedName("alloweddetails")
    @Param(description = "List of allowed Vm details as comma separated string if VM instance settings are read from OVA.", since = "4.22.1")
    private String allowedDetails;

    @SerializedName(ApiConstants.SSH_KEYPAIRS)
    @Param(description = "SSH key-pairs")
    private String keyPairNames;

    @SerializedName("affinitygroup")
    @Param(description = "List of Affinity groups associated with the Instance", responseObject = AffinityGroupResponse.class)
    private Set<AffinityGroupResponse> affinityGroupList;

    @SerializedName(ApiConstants.DISPLAY_VM)
    @Param(description = "An optional field whether to the display the Instance to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayVm;

    @SerializedName(ApiConstants.IS_DYNAMICALLY_SCALABLE)
    @Param(description = "True if Instance contains XS/VMWare tools in order to support dynamic scaling of Instance CPU/memory.")
    private Boolean isDynamicallyScalable;

    @SerializedName(ApiConstants.DELETE_PROTECTION)
    @Param(description = "true if vm has delete protection.", since = "4.20.0")
    private boolean deleteProtection;

    @SerializedName(ApiConstants.SERVICE_STATE)
    @Param(description = "State of the Service from LB rule")
    private String serviceState;

    @SerializedName(ApiConstants.OS_TYPE_ID)
    @Param(description = "OS type id of the Instance", since = "4.4")
    private String osTypeId;

    @SerializedName(ApiConstants.OS_DISPLAY_NAME)
    @Param(description = "OS name of the Instance", since = "4.13.2")
    private String osDisplayName;

    @SerializedName(ApiConstants.BOOT_MODE)
    @Param(description = "Guest Instance Boot Mode")
    private String bootMode;

    @SerializedName(ApiConstants.BOOT_TYPE)
    @Param(description = "Guest Instance Boot Type")
    private String bootType;

    @SerializedName(ApiConstants.POOL_TYPE)
    @Param(description = "The pool type of the Instance", since = "4.16")
    private String poolType;

    @SerializedName(ApiConstants.RECEIVED_BYTES)
    @Param(description = "The total number of Network traffic bytes received")
    private Long bytesReceived;

    @SerializedName(ApiConstants.SENT_BYTES)
    @Param(description = "The total number of Network traffic bytes sent")
    private Long bytesSent;

    @SerializedName(ApiConstants.RESOURCE_ICON)
    @Param(description = "Base64 string representation of the resource icon", since = "4.16.0")
    ResourceIconResponse resourceIconResponse;

    @SerializedName(ApiConstants.AUTOSCALE_VMGROUP_ID)
    @Param(description = "ID of AutoScale Instance group", since = "4.18.0")
    String autoScaleVmGroupId;

    @SerializedName(ApiConstants.AUTOSCALE_VMGROUP_NAME)
    @Param(description = "Name of AutoScale Instance group", since = "4.18.0")
    String autoScaleVmGroupName;

    @SerializedName(ApiConstants.USER_DATA)
    @Param(description = "Base64 string containing the user data", since = "4.18.0.0")
    private String userData;

    @SerializedName(ApiConstants.USER_DATA_ID) @Param(description = "The ID of userdata used for the Instance", since = "4.18.0")
    private String userDataId;

    @SerializedName(ApiConstants.USER_DATA_NAME) @Param(description = "The name of userdata used for the Instance", since = "4.18.0")
    private String userDataName;

    @SerializedName(ApiConstants.USER_DATA_POLICY) @Param(description = "The userdata override policy with the userdata provided while deploying Instance", since = "4.18.0")
    private String userDataPolicy;

    @SerializedName(ApiConstants.USER_DATA_DETAILS) @Param(description = "List of variables and values for the variables declared in userdata", since = "4.18.0")
    private String userDataDetails;

    @SerializedName(ApiConstants.VNF_NICS)
    @Param(description = "NICs of the VNF appliance", since = "4.19.0")
    private List<VnfNicResponse> vnfNics;

    @SerializedName(ApiConstants.VNF_DETAILS)
    @Param(description = "VNF details", since = "4.19.0")
    private Map<String, String> vnfDetails;

    @SerializedName(ApiConstants.VM_TYPE)
    @Param(description = "User VM type", since = "4.20.0")
    private String vmType;

    @SerializedName(ApiConstants.ARCH)
    @Param(description = "CPU arch of the VM", since = "4.20.1")
    private String arch;

    @SerializedName(ApiConstants.INSTANCE_LEASE_DURATION)
    @Param(description = "Instance lease duration in days", since = "4.21.0")
    private Integer leaseDuration;

    @SerializedName(ApiConstants.INSTANCE_LEASE_EXPIRY_DATE)
    @Param(description = "Instance lease expiry date", since = "4.21.0")
    private Date leaseExpiryDate;

    @SerializedName(ApiConstants.INSTANCE_LEASE_EXPIRY_ACTION)
    @Param(description = "Instance lease expiry action", since = "4.21.0")
    private String leaseExpiryAction;

    public UserVmResponse() {
        securityGroupList = new LinkedHashSet<>();
        nics = new TreeSet<>(Comparator.comparingInt(x -> Integer.parseInt(x.getDeviceId())));
        tags = new LinkedHashSet<>();
        tagIds = new LinkedHashSet<>();
        affinityGroupList = new LinkedHashSet<>();
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

    public Boolean getDisplayVm() {
        return displayVm;
    }

    public void setDisplayVm(Boolean displayVm) {
        this.displayVm = displayVm;
    }

    @Override
    public String getObjectId() {
        return this.getId();
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public Date getCreated() {
        return created;
    }

    public String getState() {
        return state;
    }

    public Boolean getHaEnable() {
        return haEnable;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroup() {
        return group;
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public String getHostControlState() {
        return hostControlState;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplateDisplayText() {
        return templateDisplayText;
    }

    public Boolean getPasswordEnabled() {
        return passwordEnabled;
    }

    public String getIsoId() {
        return isoId;
    }

    public String getIsoName() {
        return isoName;
    }

    public String getIsoDisplayText() {
        return isoDisplayText;
    }

    public String getServiceOfferingId() {
        return serviceOfferingId;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public String getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    public String getGpuCardId() {
        return gpuCardId;
    }

    public String getGpuCardName() {
        return gpuCardName;
    }

    public String getVgpuProfileId() {
        return vgpuProfileId;
    }

    public String getVgpuProfileName() {
        return vgpuProfileName;
    }

    public Long getVideoRam() {
        return videoRam;
    }

    public Long getMaxHeads() {
        return maxHeads;
    }

    public Long getMaxResolutionX() {
        return maxResolutionX;
    }

    public Long getMaxResolutionY() {
        return maxResolutionY;
    }

    public Integer getGpuCount() {
        return gpuCount;
    }

    public String getBackupOfferingId() {
        return backupOfferingId;
    }

    public String getBackupOfferingName() {
        return backupOfferingName;
    }

    public Boolean getForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public Integer getCpuNumber() {
        return cpuNumber;
    }

    public Integer getCpuSpeed() {
        return cpuSpeed;
    }

    public Integer getMemory() {
        return memory;
    }

    public String getVgpu() {
        return vgpu;
    }
    public String getCpuUsed() {
        return cpuUsed;
    }

    public Long getNetworkKbsRead() {
        return networkKbsRead;
    }

    public Long getNetworkKbsWrite() {
        return networkKbsWrite;
    }

    public Long getDiskKbsRead() {
        return diskKbsRead;
    }

    public Long getDiskKbsWrite() {
        return diskKbsWrite;
    }

    public Long getMemoryKBs() {
        return memoryKBs;
    }

    public Long getMemoryIntFreeKBs() {
        return memoryIntFreeKBs;
    }

    public Long getMemoryTargetKBs() {
        return memoryTargetKBs;
    }

    public Long getDiskIORead() {
        return diskIORead;
    }

    public Long getDiskIOWrite() {
        return diskIOWrite;
    }

    public String getGuestOsId() {
        return guestOsId;
    }

    public Long getRootDeviceId() {
        return rootDeviceId;
    }

    public String getRootDeviceType() {
        return rootDeviceType;
    }

    public Set<SecurityGroupResponse> getSecurityGroupList() {
        return securityGroupList;
    }

    public String getPassword() {
        return password;
    }

    public Set<NicResponse> getNics() {
        return nics;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPublicIpId() {
        return publicIpId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getKeyPairNames() {
        return keyPairNames;
    }

    public Set<AffinityGroupResponse> getAffinityGroupList() {
        return affinityGroupList;
    }

    public Boolean getIsDynamicallyScalable() {
        return isDynamicallyScalable;
    }

    public String getServiceState() {
        return serviceState;
    }

    public String getUserData() {
        return userData;
    }

    public void setIsDynamicallyScalable(Boolean isDynamicallyScalable) {
        this.isDynamicallyScalable = isDynamicallyScalable;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public void setHostControlState(String hostControlState) {
        this.hostControlState = hostControlState;
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

    public void setDiskKbsRead(Long diskKbsRead) {
        this.diskKbsRead = diskKbsRead;
    }

    public void setDiskKbsWrite(Long diskKbsWrite) {
        this.diskKbsWrite = diskKbsWrite;
    }

    public void setDiskIORead(Long diskIORead) {
        this.diskIORead = diskIORead;
    }

    public void setMemoryKBs(Long memoryKBs) {
        this.memoryKBs = memoryKBs;
    }

    public void setMemoryIntFreeKBs(Long memoryIntFreeKBs) {
        this.memoryIntFreeKBs = memoryIntFreeKBs;
    }

    public void setMemoryTargetKBs(Long memoryTargetKBs) {
        this.memoryTargetKBs = memoryTargetKBs;
    }

    public void setDiskIOWrite(Long diskIOWrite) {
        this.diskIOWrite = diskIOWrite;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public void setDiskOfferingId(String diskOfferingId) {
        this.diskOfferingId = diskOfferingId;
    }

    public void setDiskOfferingName(String diskOfferingName) {
        this.diskOfferingName = diskOfferingName;
    }

    public void setGpuCardId(String gpuCardId) {
        this.gpuCardId = gpuCardId;
    }

    public void setGpuCardName(String gpuCardName) {
        this.gpuCardName = gpuCardName;
    }

    public void setVgpuProfileId(String vgpuProfileId) {
        this.vgpuProfileId = vgpuProfileId;
    }

    public void setVgpuProfileName(String vgpuProfileName) {
        this.vgpuProfileName = vgpuProfileName;
    }

    public void setVideoRam(Long videoRam) {
        this.videoRam = videoRam;
    }

    public void setMaxHeads(Long maxHeads) {
        this.maxHeads = maxHeads;
    }

    public void setMaxResolutionX(Long maxResolutionX) {
        this.maxResolutionX = maxResolutionX;
    }

    public void setMaxResolutionY(Long maxResolutionY) {
        this.maxResolutionY = maxResolutionY;
    }

    public void setGpuCount(Integer gpuCount) {
        this.gpuCount = gpuCount;
    }

    public void setBackupOfferingId(String backupOfferingId) {
        this.backupOfferingId = backupOfferingId;
    }

    public void setBackupOfferingName(String backupOfferingName) {
        this.backupOfferingName = backupOfferingName;
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

    public void setVgpu(String vgpu) {
        this.vgpu = vgpu;
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
        setIpAddress(nics);
    }

    public void setIpAddress(final Set<NicResponse> nics) {
        if (CollectionUtils.isNotEmpty(nics)) {
            this.ipAddress = nics.iterator().next().getIpaddress();
        }
    }

    public void addNic(NicResponse nic) {
        this.nics.add(nic);
    }

    public void setSecurityGroupList(Set<SecurityGroupResponse> securityGroups) {
        this.securityGroupList = securityGroups;
    }

    public void addSecurityGroup(SecurityGroupResponse securityGroup) {
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

    public boolean containTag(Long tagId) {
        return tagIds.contains(tagId);
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void setKeyPairNames(String keyPairNames) {
        this.keyPairNames = keyPairNames;
    }

    public void setAffinityGroupList(Set<AffinityGroupResponse> affinityGroups) {
        this.affinityGroupList = affinityGroups;
    }

    public void addAffinityGroup(AffinityGroupResponse affinityGroup) {
        this.affinityGroupList.add(affinityGroup);
    }

    public void setDynamicallyScalable(boolean isDynamicallyScalable) {
        this.isDynamicallyScalable = isDynamicallyScalable;
    }

    public void setServiceState(String state) {
        this.serviceState = state;
    }

    public void setDetails(Map details) {
        this.details = details;
    }

    public void setReadOnlyDetails(String readOnlyDetails) {
        this.readOnlyDetails = readOnlyDetails;
    }

    public void setAllowedDetails(String allowedDetails) {
        this.allowedDetails = allowedDetails;
    }

    public void setOsTypeId(String osTypeId) {
        this.osTypeId = osTypeId;
    }

    public void setOsDisplayName(String osDisplayName) {
        this.osDisplayName = osDisplayName;
    }

    public Set<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(Set<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public Map getDetails() {
        return details;
    }

    public String getReadOnlyDetails() {
        return readOnlyDetails;
    }

    public String getAllowedDetails() {
        return allowedDetails;
    }

    public Boolean getDynamicallyScalable() {
        return isDynamicallyScalable;
    }

    public void setDynamicallyScalable(Boolean dynamicallyScalable) {
        isDynamicallyScalable = dynamicallyScalable;
    }

    public boolean isDeleteProtection() {
        return deleteProtection;
    }

    public void setDeleteProtection(boolean deleteProtection) {
        this.deleteProtection = deleteProtection;
    }

    public String getOsTypeId() {
        return osTypeId;
    }

    public String getOsDisplayName() {
        return osDisplayName;
    }

    public String getBootType() { return bootType; }

    public void setBootType(String bootType) { this.bootType = bootType; }

    public String getBootMode() { return bootMode; }

    public void setBootMode(String bootMode) { this.bootMode = bootMode; }

    public String getPoolType() { return poolType; }

    public void setPoolType(String poolType) { this.poolType = poolType; }

    @Override
    public void setResourceIconResponse(ResourceIconResponse resourceIconResponse) {
        this.resourceIconResponse = resourceIconResponse;
    }

    public ResourceIconResponse getResourceIconResponse() {
        return resourceIconResponse;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setBytesReceived(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public void setBytesSent(Long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public void setAutoScaleVmGroupId(String autoScaleVmGroupId) {
        this.autoScaleVmGroupId = autoScaleVmGroupId;
    }

    public void setAutoScaleVmGroupName(String autoScaleVmGroupName) {
        this.autoScaleVmGroupName = autoScaleVmGroupName;
    }

    public String getAutoScaleVmGroupId() {
        return autoScaleVmGroupId;
    }

    public String getAutoScaleVmGroupName() {
        return autoScaleVmGroupName;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public String getUserDataId() {
        return userDataId;
    }

    public void setUserDataId(String userDataId) {
        this.userDataId = userDataId;
    }

    public String getUserDataName() {
        return userDataName;
    }

    public void setUserDataName(String userDataName) {
        this.userDataName = userDataName;
    }

    public String getUserDataPolicy() {
        return userDataPolicy;
    }

    public void setUserDataPolicy(String userDataPolicy) {
        this.userDataPolicy = userDataPolicy;
    }

    public String getUserDataDetails() {
        return userDataDetails;
    }

    public void setUserDataDetails(String userDataDetails) {
        this.userDataDetails = userDataDetails;
    }

    public Long getBytesReceived() {
        return bytesReceived;
    }

    public Long getBytesSent() {
        return bytesSent;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getTemplateFormat() {
        return templateFormat;
    }

    public void setTemplateFormat(String templateFormat) {
        this.templateFormat = templateFormat;
    }

    public List<VnfNicResponse> getVnfNics() {
        return vnfNics;
    }

    public void setVnfNics(List<VnfNicResponse> vnfNics) {
        this.vnfNics = vnfNics;
    }

    public Map<String, String> getVnfDetails() {
        return vnfDetails;
    }

    public void setVnfDetails(Map<String, String> vnfDetails) {
        this.vnfDetails = vnfDetails;
    }

    public void addVnfNic(VnfNicResponse vnfNic) {
        if (this.vnfNics == null) {
            this.vnfNics = new ArrayList<>();
        }
        this.vnfNics.add(vnfNic);
    }

    public void addVnfDetail(String key, String value) {
        if (this.vnfDetails == null) {
            this.vnfDetails = new LinkedHashMap<>();
        }
        this.vnfDetails.put(key,value);
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
    }

    public String getVmType() {
        return vmType;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public Integer getLeaseDuration() {
        return leaseDuration;
    }

    public void setLeaseDuration(Integer leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    public String getLeaseExpiryAction() {
        return leaseExpiryAction;
    }

    public void setLeaseExpiryAction(String leaseExpiryAction) {
        this.leaseExpiryAction = leaseExpiryAction;
    }

    public Date getLeaseExpiryDate() {
        return leaseExpiryDate;
    }

    public void setLeaseExpiryDate(Date leaseExpiryDate) {
        this.leaseExpiryDate = leaseExpiryDate;
    }

}
