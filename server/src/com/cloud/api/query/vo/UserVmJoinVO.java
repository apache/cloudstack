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
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

@Entity
@Table(name = "user_vm_view")
public class UserVmJoinVO extends BaseViewVO implements ControlledViewEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "name", updatable = false, nullable = false, length = 255)
    private String name = null;

    @Column(name = "display_name", updatable = false, nullable = false, length = 255)
    private String displayName = null;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName = null;

    @Column(name = "account_type")
    private short accountType;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName = null;

    @Column(name = "domain_path")
    private String domainPath = null;

    @Column(name = "instance_group_id")
    private long instanceGroupId;

    @Column(name = "instance_group_uuid")
    private String instanceGroupUuid;

    @Column(name = "instance_group_name")
    private String instanceGroupName;

    @Column(name = "vm_type", updatable = false, nullable = false, length = 32)
    @Enumerated(value = EnumType.STRING)
    protected VirtualMachine.Type type;

    /**
     * Note that state is intentionally missing the setter.  Any updates to
     * the state machine needs to go through the DAO object because someone
     * else could be updating it as well.
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "state", updatable = true, nullable = false, length = 32)
    private State state = null;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "instance_name", updatable = true, nullable = false)
    private String instanceName;

    @Column(name = "guest_os_id", nullable = false, length = 17)
    private long guestOsId;

    @Column(name = "guest_os_uuid")
    private String guestOsUuid;

    @Column(name = "hypervisor_type")
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name = "ha_enabled", updatable = true, nullable = true)
    private boolean haEnabled;

    @Column(name = "limit_cpu_use", updatable = true, nullable = true)
    private boolean limitCpuUse;

    @Column(name = "display_vm", updatable = true, nullable = false)
    protected boolean displayVm = true;

    @Column(name = "last_host_id", updatable = true, nullable = true)
    private Long lastHostId;

    @Column(name = "private_ip_address", updatable = true)
    private String privateIpAddress;

    @Column(name = "private_mac_address", updatable = true, nullable = true)
    private String privateMacAddress;

    @Column(name = "pod_id", updatable = true, nullable = false)
    private Long podId;

    @Column(name = "pod_uuid")
    private String podUuid;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "data_center_uuid")
    private String dataCenterUuid;

    @Column(name = "data_center_name")
    private String dataCenterName = null;

    @Column(name = "security_group_enabled")
    private boolean securityGroupEnabled;

    @Column(name = "host_id", updatable = true, nullable = true)
    private long hostId;

    @Column(name = "host_uuid")
    private String hostUuid;

    @Column(name = "host_name", nullable = false)
    private String hostName;

    @Column(name = "template_id", updatable = true, nullable = true, length = 17)
    private long templateId;

    @Column(name = "template_uuid")
    private String templateUuid;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_display_text", length = 4096)
    private String templateDisplayText;

    @Column(name = "password_enabled")
    private boolean passwordEnabled;

    @Column(name = "iso_id", updatable = true, nullable = true, length = 17)
    private long isoId;

    @Column(name = "iso_uuid")
    private String isoUuid;

    @Column(name = "iso_name")
    private String isoName;

    @Column(name = "iso_display_text", length = 4096)
    private String isoDisplayText;

    @Column(name = "disk_offering_id")
    private long diskOfferingId;

    @Column(name = "disk_offering_uuid")
    private String diskOfferingUuid;

    @Column(name = "disk_offering_name")
    private String diskOfferingName;

    @Column(name = "service_offering_id")
    private long serviceOfferingId;

    @Column(name = "service_offering_uuid")
    private String serviceOfferingUuid;

    @Column(name = "service_offering_name")
    private String serviceOfferingName;

    @Column(name = "cpu")
    private int cpu;

    @Column(name = "speed")
    private int speed;

    @Column(name = "ram_size")
    private int ramSize;

    @Column(name = "pool_id", updatable = false, nullable = false)
    private long poolId;

    @Column(name = "pool_uuid")
    private String poolUuid;

    @Column(name = "pool_type", updatable = false, nullable = false, length = 32)
    @Enumerated(value = EnumType.STRING)
    private StoragePoolType poolType;

    @Column(name = "volume_id")
    private long volumeId;

    @Column(name = "volume_uuid")
    private String volumeUuid;

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

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "network_uuid")
    private String networkUuid;

    @Column(name = "network_name")
    private String networkName;

    @Column(name = "traffic_type")
    @Enumerated(value = EnumType.STRING)
    private TrafficType trafficType;

    @Column(name = "guest_type")
    @Enumerated(value = EnumType.STRING)
    private GuestType guestType;

    @Column(name = "public_ip_id")
    private long publicIpId;

    @Column(name = "public_ip_uuid")
    private String publicIpUuid;

    @Column(name = "public_ip_address")
    private String publicIpAddress;

    @Column(name = "user_data", updatable = true, nullable = true, length = 2048)
    private String userData;

    @Column(name = "project_id")
    private long projectId;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "keypair_name")
    private String keypairName;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_uuid")
    private String jobUuid;

    @Column(name = "job_status")
    private int jobStatus;

    @Column(name = "tag_id")
    private long tagId;

    @Column(name = "tag_uuid")
    private String tagUuid;

    @Column(name = "tag_key")
    private String tagKey;

    @Column(name = "tag_value")
    private String tagValue;

    @Column(name = "tag_domain_id")
    private long tagDomainId;

    @Column(name = "tag_account_id")
    private long tagAccountId;

    @Column(name = "tag_resource_id")
    private long tagResourceId;

    @Column(name = "tag_resource_uuid")
    private String tagResourceUuid;

    @Column(name = "tag_resource_type")
    @Enumerated(value = EnumType.STRING)
    private ResourceObjectType tagResourceType;

    @Column(name = "tag_customer")
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

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "dynamically_scalable")
    private boolean isDynamicallyScalable;


    public UserVmJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getPassword() {
        return password;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public long getDiskOfferingId() {
        return diskOfferingId;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getUserId() {
        return userId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public short getAccountType() {
        return accountType;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getDomainUuid() {
        return domainUuid;
    }

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }

    public long getInstanceGroupId() {
        return instanceGroupId;
    }

    public String getInstanceGroupUuid() {
        return instanceGroupUuid;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public VirtualMachine.Type getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public long getGuestOSId() {
        return guestOsId;
    }

    public String getGuestOsUuid() {
        return guestOsUuid;
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public boolean isHaEnabled() {
        return haEnabled;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    public Long getLastHostId() {
        return lastHostId;
    }

    public Long getPodId() {
        return podId;
    }

    public String getPodUuid() {
        return podUuid;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public boolean limitCpuUse() {
        return limitCpuUse;
    }

    public boolean isDisplayVm() {
        return displayVm;
    }

    public String getDataCenterUuid() {
        return dataCenterUuid;
    }

    public String getDataCenterName() {
        return dataCenterName;
    }

    public boolean isSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    public Long getHostId() {
        return hostId;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getHostName() {
        return hostName;
    }

    public long getTemplateId() {
        return templateId;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplateDisplayText() {
        return templateDisplayText;
    }

    public boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public Long getIsoId() {
        return isoId;
    }

    public String getIsoUuid() {
        return isoUuid;
    }

    public String getIsoName() {
        return isoName;
    }

    public String getIsoDisplayText() {
        return isoDisplayText;
    }

    public String getServiceOfferingUuid() {
        return serviceOfferingUuid;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public int getCpu() {
        return cpu;
    }

    public int getSpeed() {
        return speed;
    }

    public int getRamSize() {
        return ramSize;
    }

    public long getPoolId() {
        return poolId;
    }

    public StoragePoolType getPoolType() {
        return poolType;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public Long getVolumeDeviceId() {
        return volumeDeviceId;
    }

    public Volume.Type getVolumeType() {
        return volumeType;
    }

    public long getSecurityGroupId() {
        return securityGroupId;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public String getSecurityGroupDescription() {
        return securityGroupDescription;
    }

    public long getVpcId() {
        return vpcId;
    }

    public long getNicId() {
        return nicId;
    }

    public boolean isDefaultNic() {
        return isDefaultNic;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getGateway() {
        return gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public URI getBroadcastUri() {
        return broadcastUri;
    }

    public URI getIsolationUri() {
        return isolationUri;
    }

    public long getNetworkId() {
        return networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public TrafficType getTrafficType() {
        return trafficType;
    }

    public GuestType getGuestType() {
        return guestType;
    }

    public long getPublicIpId() {
        return publicIpId;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public String getDetail(String name) {
        return details != null ? details.get(name) : null;
    }

    public String getUserData() {
        return userData;
    }

    public long getGuestOsId() {
        return guestOsId;
    }

    public long getProjectId() {
        return projectId;
    }

    @Override
    public String getProjectUuid() {
        return projectUuid;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    public String getKeypairName() {
        return keypairName;
    }

    public long getTagId() {
        return tagId;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    public String getTagKey() {
        return tagKey;
    }

    public String getTagValue() {
        return tagValue;
    }

    public long getTagDomainId() {
        return tagDomainId;
    }

    public long getTagAccountId() {
        return tagAccountId;
    }

    public long getTagResourceId() {
        return tagResourceId;
    }

    public String getTagResourceUuid() {
        return tagResourceUuid;
    }

    public ResourceObjectType getTagResourceType() {
        return tagResourceType;
    }

    public String getTagCustomer() {
        return tagCustomer;
    }

    public boolean isLimitCpuUse() {
        return limitCpuUse;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public String getVolume_uuid() {
        return volumeUuid;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public String getVpcUuid() {
        return vpcUuid;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getPublicIpUuid() {
        return publicIpUuid;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public int getJobStatus() {
        return jobStatus;
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

    public String getIp6Gateway() {
        return ip6Gateway;
    }

    public String getIp6Cidr() {
        return ip6Cidr;
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


    @Override
    public Class<?> getEntityType() {
        return VirtualMachine.class;
    }
}
