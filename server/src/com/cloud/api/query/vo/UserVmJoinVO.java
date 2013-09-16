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
package com.cloud.api.query.vo;

import java.net.URI;
import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.storage.Volume;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

@Entity
@Table(name="user_vm_view")
public class UserVmJoinVO extends BaseViewVO implements ControlledViewEntity {

    @Id
    @Column(name="id", updatable=false, nullable = false)
    private long id;

    @Column(name="name", updatable=false, nullable=false, length=255)
    private String name = null;

    @Column(name="display_name", updatable=false, nullable=false, length=255)
    private String displayName = null;

    @Column(name="account_id")
    private long accountId;

    @Column(name="account_uuid")
    private String accountUuid;

    @Column(name="account_name")
    private String accountName = null;

    @Column(name="account_type")
    private short accountType;

    @Column(name="domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name="domain_name")
    private String domainName = null;

    @Column(name="domain_path")
    private String domainPath = null;

    @Column(name="instance_group_id")
    private long instanceGroupId;

    @Column(name="instance_group_uuid")
    private String instanceGroupUuid;

    @Column(name="instance_group_name")
    private String instanceGroupName;

    @Column(name="vm_type", updatable=false, nullable=false, length=32)
    @Enumerated(value=EnumType.STRING)
    protected VirtualMachine.Type type;

    /**
     * Note that state is intentionally missing the setter.  Any updates to
     * the state machine needs to go through the DAO object because someone
     * else could be updating it as well.
     */
    @Enumerated(value=EnumType.STRING)
    @Column(name="state", updatable=true, nullable=false, length=32)
    private State state = null;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="instance_name", updatable=true, nullable=false)
    private String instanceName;

    @Column(name="guest_os_id", nullable=false, length=17)
    private long guestOsId;

    @Column(name="guest_os_uuid")
    private String guestOsUuid;

    @Column(name="hypervisor_type")
    @Enumerated(value=EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name="ha_enabled", updatable=true, nullable=true)
    private boolean haEnabled;

    @Encrypt
    @Column(name="vnc_password", updatable=true, nullable=false, length=255)
    protected String vncPassword;

    @Column(name="limit_cpu_use", updatable=true, nullable=true)
    private boolean limitCpuUse;

    @Column(name="display_vm", updatable=true, nullable=false)
    protected boolean displayVm = true;

    @Column(name="last_host_id", updatable=true, nullable=true)
    private Long lastHostId;

    @Column(name="private_ip_address", updatable=true)
    private String privateIpAddress;


    @Column(name="private_mac_address", updatable=true, nullable=true)
    private String privateMacAddress;

    @Column(name="pod_id", updatable=true, nullable=false)
    private Long podId;

    @Column(name="pod_uuid")
    private String podUuid;

    @Column(name="data_center_id")
    private long dataCenterId;

    @Column(name="data_center_uuid")
    private String dataCenterUuid;

    @Column(name="data_center_name")
    private String dataCenterName = null;

    @Column(name="security_group_enabled")
    private boolean securityGroupEnabled;

    @Column(name="host_id", updatable=true, nullable=true)
    private long hostId;

    @Column(name="host_uuid")
    private String hostUuid;

    @Column(name="host_name", nullable=false)
    private String hostName;

    @Column(name="template_id", updatable=true, nullable=true, length=17)
    private long templateId;

    @Column(name="template_uuid")
    private String templateUuid;

    @Column(name="template_name")
    private String templateName;

    @Column(name="template_display_text", length=4096)
    private String templateDisplayText;

    @Column(name="password_enabled")
    private boolean passwordEnabled;

    @Column(name="iso_id", updatable=true, nullable=true, length=17)
    private long isoId;

    @Column(name="iso_uuid")
    private String isoUuid;

    @Column(name="iso_name")
    private String isoName;

    @Column(name="iso_display_text", length=4096)
    private String isoDisplayText;

    @Column(name="service_offering_id")
    private long serviceOfferingId;

    @Column(name="service_offering_uuid")
    private String serviceOfferingUuid;

    @Column(name="service_offering_name")
    private String serviceOfferingName;

    @Column(name="cpu")
    private int cpu;

    @Column(name="speed")
    private int speed;

    @Column(name="ram_size")
    private int ramSize;

    @Column(name="pool_id", updatable=false, nullable = false)
    private long poolId;

    @Column(name="pool_uuid")
    private String poolUuid;

    @Column(name="pool_type", updatable=false, nullable=false, length=32)
    @Enumerated(value=EnumType.STRING)
    private StoragePoolType poolType;

    @Column(name = "volume_id")
    private long volume_id;

    @Column(name = "volume_uuid")
    private String volume_uuid;

    @Column(name = "volume_device_id")
    private Long volumeDeviceId = null;

    @Column(name = "volume_type")
    @Enumerated(EnumType.STRING)
    private Volume.Type volumeType;

    @Column(name = "security_group_id")
    private long securityGroupId;

    @Column(name = "security_group_uuid")
    private String securityGroupUuid;

    @Column(name = "security_group_name")
    private String securityGroupName;

    @Column(name = "security_group_description")
    private String securityGroupDescription;

    @Column(name = "vpc_id")
    private long vpcId;

    @Column(name = "vpc_uuid")
    private String vpcUuid;

    @Column(name = "nic_id")
    private long nicId;

    @Column(name = "nic_uuid")
    private String nicUuid;

    @Column(name = "is_default_nic")
    private boolean isDefaultNic;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "netmask")
    private String netmask;

    @Column(name = "ip6_address")
    private String ip6Address;

    @Column(name = "ip6_gateway")
    private String ip6Gateway;

    @Column(name = "ip6_cidr")
    private String ip6Cidr;

    @Column(name = "mac_address")
    private String macAddress;

    @Column(name = "broadcast_uri")
    private URI broadcastUri;

    @Column(name = "isolation_uri")
    private URI isolationUri;

    @Column(name="network_id")
    private long networkId;

    @Column(name="network_uuid")
    private String networkUuid;

    @Column(name="network_name")
    private String networkName;

    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    private TrafficType trafficType;

    @Column(name="guest_type")
    @Enumerated(value=EnumType.STRING)
    private GuestType guestType;

    @Column(name = "public_ip_id")
    private long publicIpId;

    @Column(name = "public_ip_uuid")
    private String publicIpUuid;

    @Column(name = "public_ip_address")
    private String publicIpAddress;

    @Column(name="user_data", updatable=true, nullable=true, length=2048)
    private String userData;

    @Column(name="project_id")
    private long projectId;

    @Column(name="project_uuid")
    private String projectUuid;

    @Column(name="project_name")
    private String projectName;

    @Column(name="keypair_name")
    private String keypairName;

    @Column(name="job_id")
    private Long jobId;

    @Column(name="job_uuid")
    private String jobUuid;

    @Column(name="job_status")
    private int jobStatus;

    @Column(name="tag_id")
    private long tagId;

    @Column(name="tag_uuid")
    private String tagUuid;

    @Column(name="tag_key")
    private String tagKey;

    @Column(name="tag_value")
    private String tagValue;

    @Column(name="tag_domain_id")
    private long tagDomainId;

    @Column(name="tag_account_id")
    private long tagAccountId;

    @Column(name="tag_resource_id")
    private long tagResourceId;

    @Column(name="tag_resource_uuid")
    private String tagResourceUuid;

    @Column(name="tag_resource_type")
    @Enumerated(value=EnumType.STRING)
    private TaggedResourceType tagResourceType;

    @Column(name="tag_customer")
    private String tagCustomer;

    @Column(name = "affinity_group_id")
    private long affinityGroupId;

    @Column(name = "affinity_group_uuid")
    private String affinityGroupUuid;

    @Column(name = "affinity_group_name")
    private String affinityGroupName;

    @Column(name = "affinity_group_description")
    private String affinityGroupDescription;

    transient String password;

    @Transient
    Map<String, String> details;

    @Column(name="uuid")
    private String uuid;

    @Column(name="dynamically_scalable")
    private boolean isDynamicallyScalable;

    public UserVmJoinVO() {
    }


    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }


    @Override
    public String getUuid() {
        return uuid;
    }




    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getDisplayName() {
        return displayName;
    }


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    @Override
    public long getAccountId() {
        return accountId;
    }


    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }


    @Override
    public String getAccountUuid() {
        return accountUuid;
    }




    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }




    @Override
    public String getAccountName() {
        return accountName;
    }


    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }


    @Override
    public short getAccountType() {
        return accountType;
    }


    public void setAccountType(short accountType) {
        this.accountType = accountType;
    }


    @Override
    public long getDomainId() {
        return domainId;
    }


    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }


    @Override
    public String getDomainUuid() {
        return domainUuid;
    }




    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }




    @Override
    public String getDomainName() {
        return domainName;
    }


    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }


    @Override
    public String getDomainPath() {
        return domainPath;
    }


    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }




    public long getInstanceGroupId() {
        return instanceGroupId;
    }


    public void setInstanceGroupId(long instanceGroupId) {
        this.instanceGroupId = instanceGroupId;
    }


    public String getInstanceGroupUuid() {
        return instanceGroupUuid;
    }




    public void setInstanceGroupUuid(String instanceGroupUuid) {
        this.instanceGroupUuid = instanceGroupUuid;
    }




    public String getInstanceGroupName() {
        return instanceGroupName;
    }


    public void setInstanceGroupName(String instanceGroupName) {
        this.instanceGroupName = instanceGroupName;
    }


    public VirtualMachine.Type getType() {
        return type;
    }




    public void setType(VirtualMachine.Type type) {
        this.type = type;
    }




    public State getState() {
        return state;
    }


    public void setState(State state) {
        this.state = state;
    }


    public Date getCreated() {
        return created;
    }


    public void setCreated(Date created) {
        this.created = created;
    }


    public Date getRemoved() {
        return removed;
    }


    public void setRemoved(Date removed) {
        this.removed = removed;
    }


    public String getInstanceName() {
        return instanceName;
    }


    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }


    public long getGuestOSId() {
        return guestOsId;
    }


    public void setGuestOSId(long guestOSId) {
        this.guestOsId = guestOSId;
    }


    public String getGuestOsUuid() {
        return guestOsUuid;
    }




    public void setGuestOsUuid(String guestOsUuid) {
        this.guestOsUuid = guestOsUuid;
    }




    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }


    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }


    public boolean isHaEnabled() {
        return haEnabled;
    }


    public void setHaEnabled(boolean haEnabled) {
        this.haEnabled = haEnabled;
    }

    public void setVncPassword(String vncPassword) {
        this.vncPassword = vncPassword;
    }

    public String getVncPassword() {
        return vncPassword;
    }




    public String getPrivateIpAddress() {
        return privateIpAddress;
    }




    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }




    public String getPrivateMacAddress() {
        return privateMacAddress;
    }




    public void setPrivateMacAddress(String privateMacAddress) {
        this.privateMacAddress = privateMacAddress;
    }




    public Long getLastHostId() {
        return lastHostId;
    }




    public void setLastHostId(Long lastHostId) {
        this.lastHostId = lastHostId;
    }






    public Long getPodId() {
        return podId;
    }




    public void setPodId(Long podIdToDeployIn) {
        this.podId = podIdToDeployIn;
    }




    public String getPodUuid() {
        return podUuid;
    }




    public void setPodUuid(String podUuid) {
        this.podUuid = podUuid;
    }




    public long getDataCenterId() {
        return dataCenterId;
    }




    public void setDataCenterId(long dataCenterIdToDeployIn) {
        this.dataCenterId = dataCenterIdToDeployIn;
    }


    public boolean limitCpuUse() {
        return limitCpuUse;
    }

    public void setLimitCpuUse(boolean value) {
        limitCpuUse = value;
    }

    public boolean isDisplayVm() {
        return displayVm;
    }

    public void setDisplayVm(boolean displayVm) {
        this.displayVm = displayVm;
    }

    public String getDataCenterUuid() {
        return dataCenterUuid;
    }




    public void setDataCenterUuid(String zoneUuid) {
        this.dataCenterUuid = zoneUuid;
    }




    public String getDataCenterName() {
        return dataCenterName;
    }


    public void setDataCenterName(String zoneName) {
        this.dataCenterName = zoneName;
    }


    public boolean isSecurityGroupEnabled() {
        return securityGroupEnabled;
    }


    public void setSecurityGroupEnabled(boolean securityGroupEnabled) {
        this.securityGroupEnabled = securityGroupEnabled;
    }


    public Long getHostId() {
        return hostId;
    }


    public void setHostId(long hostId) {
        this.hostId = hostId;
    }


    public String getHostUuid() {
        return hostUuid;
    }




    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }




    public String getHostName() {
        return hostName;
    }


    public void setHostName(String hostName) {
        this.hostName = hostName;
    }


    public long getTemplateId() {
        return templateId;
    }


    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }



    public String getTemplateUuid() {
        return templateUuid;
    }




    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }




    public String getTemplateName() {
        return templateName;
    }


    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }


    public String getTemplateDisplayText() {
        return templateDisplayText;
    }


    public void setTemplateDisplayText(String templateDisplayText) {
        this.templateDisplayText = templateDisplayText;
    }


    public boolean isPasswordEnabled() {
        return passwordEnabled;
    }


    public void setPasswordEnabled(boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }


    public Long getIsoId() {
        return isoId;
    }


    public void setIsoId(long isoId) {
        this.isoId = isoId;
    }


    public String getIsoUuid() {
        return isoUuid;
    }




    public void setIsoUuid(String isoUuid) {
        this.isoUuid = isoUuid;
    }




    public String getIsoName() {
        return isoName;
    }


    public void setIsoName(String isoName) {
        this.isoName = isoName;
    }


    public String getIsoDisplayText() {
        return isoDisplayText;
    }

    public void setIsoDisplayText(String isoDisplayText) {
        this.isoDisplayText = isoDisplayText;
    }




    public String getServiceOfferingUuid() {
        return serviceOfferingUuid;
    }




    public void setServiceOfferingUuid(String serviceOfferingUuid) {
        this.serviceOfferingUuid = serviceOfferingUuid;
    }




    public String getServiceOfferingName() {
        return serviceOfferingName;
    }


    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }


    public int getCpu() {
        return cpu;
    }


    public void setCpu(int cpu) {
        this.cpu = cpu;
    }


    public int getSpeed() {
        return speed;
    }


    public void setSpeed(int speed) {
        this.speed = speed;
    }


    public int getRamSize() {
        return ramSize;
    }


    public void setRamSize(int ramSize) {
        this.ramSize = ramSize;
    }


    public long getPoolId() {
        return poolId;
    }


    public void setPoolId(long poolId) {
        this.poolId = poolId;
    }


    public StoragePoolType getPoolType() {
        return poolType;
    }


    public void setPoolType(StoragePoolType poolType) {
        this.poolType = poolType;
    }


    public long getVolume_id() {
        return volume_id;
    }


    public void setVolume_id(long volume_id) {
        this.volume_id = volume_id;
    }


    public Long getVolumeDeviceId() {
        return volumeDeviceId;
    }


    public void setVolumeDeviceId(Long volumeDeviceId) {
        this.volumeDeviceId = volumeDeviceId;
    }


    public Volume.Type getVolumeType() {
        return volumeType;
    }


    public void setVolumeType(Volume.Type volumeType) {
        this.volumeType = volumeType;
    }


    public long getSecurityGroupId() {
        return securityGroupId;
    }


    public void setSecurityGroupId(long securityGroupId) {
        this.securityGroupId = securityGroupId;
    }


    public String getSecurityGroupName() {
        return securityGroupName;
    }


    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }


    public String getSecurityGroupDescription() {
        return securityGroupDescription;
    }


    public void setSecurityGroupDescription(String securityGroupDescription) {
        this.securityGroupDescription = securityGroupDescription;
    }


    public long getVpcId() {
        return vpcId;
    }



    public void setVpcId(long vpcId) {
        this.vpcId = vpcId;
    }




    public long getNicId() {
        return nicId;
    }


    public void setNicId(long nicId) {
        this.nicId = nicId;
    }


    public boolean isDefaultNic() {
        return isDefaultNic;
    }


    public void setDefaultNic(boolean isDefaultNic) {
        this.isDefaultNic = isDefaultNic;
    }


    public String getIpAddress() {
        return ipAddress;
    }


    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public String getGateway() {
        return gateway;
    }


    public void setGateway(String gateway) {
        this.gateway = gateway;
    }


    public String getNetmask() {
        return netmask;
    }


    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }


    public String getMacAddress() {
        return macAddress;
    }


    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }


    public URI getBroadcastUri() {
        return broadcastUri;
    }


    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }


    public URI getIsolationUri() {
        return isolationUri;
    }


    public void setIsolationUri(URI isolationUri) {
        this.isolationUri = isolationUri;
    }


    public long getNetworkId() {
        return networkId;
    }


    public void setNetworkId(long networkId) {
        this.networkId = networkId;
    }


    public String getNetworkName() {
        return networkName;
    }


    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }


    public TrafficType getTrafficType() {
        return trafficType;
    }


    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }


    public GuestType getGuestType() {
        return guestType;
    }


    public void setGuestType(GuestType guestType) {
        this.guestType = guestType;
    }


    public long getPublicIpId() {
        return publicIpId;
    }




    public void setPublicIpId(long publicIpId) {
        this.publicIpId = publicIpId;
    }




    public String getPublicIpAddress() {
        return publicIpAddress;
    }


    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }



    public long getServiceOfferingId() {
        return serviceOfferingId;
    }




    public void setServiceOfferingId(long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }


    public Map<String, String> getDetails() {
        return details;
    }

    public String getDetail(String name) {
        assert (details != null) : "Did you forget to load the details?";

        return details != null ? details.get(name) : null;
    }

    public void setDetail(String name, String value) {
        assert (details != null) : "Did you forget to load the details?";

        details.put(name, value);
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public String getUserData() {
        return userData;
    }



    public long getGuestOsId() {
        return guestOsId;
    }




    public void setGuestOsId(long guestOsId) {
        this.guestOsId = guestOsId;
    }




    public long getProjectId() {
        return projectId;
    }




    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }




    @Override
    public String getProjectUuid() {
        return projectUuid;
    }




    public void setProjectUuid(String projectUuid) {
        this.projectUuid = projectUuid;
    }




    @Override
    public String getProjectName() {
        return projectName;
    }




    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }




    public String getKeypairName() {
        return keypairName;
    }




    public void setKeypairName(String keypairName) {
        this.keypairName = keypairName;
    }




    public long getTagId() {
        return tagId;
    }




    public void setTagId(long tagId) {
        this.tagId = tagId;
    }




    public String getTagUuid() {
        return tagUuid;
    }




    public void setTagUuid(String tagUuid) {
        this.tagUuid = tagUuid;
    }




    public String getTagKey() {
        return tagKey;
    }




    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }




    public String getTagValue() {
        return tagValue;
    }




    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }




    public long getTagDomainId() {
        return tagDomainId;
    }




    public void setTagDomainId(long tagDomainId) {
        this.tagDomainId = tagDomainId;
    }




    public long getTagAccountId() {
        return tagAccountId;
    }




    public void setTagAccountId(long tagAccountId) {
        this.tagAccountId = tagAccountId;
    }




    public long getTagResourceId() {
        return tagResourceId;
    }




    public void setTagResourceId(long tagResourceId) {
        this.tagResourceId = tagResourceId;
    }




    public String getTagResourceUuid() {
        return tagResourceUuid;
    }




    public void setTagResourceUuid(String tagResourceUuid) {
        this.tagResourceUuid = tagResourceUuid;
    }




    public TaggedResourceType getTagResourceType() {
        return tagResourceType;
    }




    public void setTagResourceType(TaggedResourceType tagResourceType) {
        this.tagResourceType = tagResourceType;
    }




    public String getTagCustomer() {
        return tagCustomer;
    }




    public void setTagCustomer(String tagCustomer) {
        this.tagCustomer = tagCustomer;
    }




    public boolean isLimitCpuUse() {
        return limitCpuUse;
    }



    public String getPoolUuid() {
        return poolUuid;
    }




    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }




    public String getVolume_uuid() {
        return volume_uuid;
    }




    public void setVolume_uuid(String volume_uuid) {
        this.volume_uuid = volume_uuid;
    }




    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }




    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }




    public String getVpcUuid() {
        return vpcUuid;
    }




    public void setVpcUuid(String vpcUuid) {
        this.vpcUuid = vpcUuid;
    }




    public String getNicUuid() {
        return nicUuid;
    }




    public void setNicUuid(String nicUuid) {
        this.nicUuid = nicUuid;
    }




    public String getNetworkUuid() {
        return networkUuid;
    }




    public void setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
    }




    public String getPublicIpUuid() {
        return publicIpUuid;
    }




    public void setPublicIpUuid(String publicIpUuid) {
        this.publicIpUuid = publicIpUuid;
    }



    public Long getJobId() {
        return jobId;
    }




    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }




    public String getJobUuid() {
        return jobUuid;
    }




    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }




    public int getJobStatus() {
        return jobStatus;
    }




    public void setJobStatus(int jobStatus) {
        this.jobStatus = jobStatus;
    }



    transient String toString;
    @Override
    public String toString() {
        if (toString == null) {
            toString = new StringBuilder("VM[").append(id).append("|").append(name).append("]").toString();
        }
        return toString;
    }

	public String getIp6Address() {
		return ip6Address;
	}




	public void setIp6Address(String ip6Address) {
		this.ip6Address = ip6Address;
	}




	public String getIp6Gateway() {
		return ip6Gateway;
	}




	public void setIp6Gateway(String ip6Gateway) {
		this.ip6Gateway = ip6Gateway;
	}




	public String getIp6Cidr() {
		return ip6Cidr;
	}




	public void setIp6Cidr(String ip6Cidr) {
		this.ip6Cidr = ip6Cidr;
	}


    public long getAffinityGroupId() {
        return affinityGroupId;
    }



    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }



    public String getAffinityGroupName() {
        return affinityGroupName;
    }



    public String getAffinityGroupDescription() {
        return affinityGroupDescription;
    }

    public Boolean isDynamicallyScalable() {
        return isDynamicallyScalable;
    }

    public void setDynamicallyScalable(boolean isDynamicallyScalable) {
        this.isDynamicallyScalable = isDynamicallyScalable;
    }


}
