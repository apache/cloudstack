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
package com.cloud.vm.dao;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.api.response.SecurityGroupRuleResponse;

public class UserVmData {
    private Long id;
    private String name;
    private String uuid;
    private String displayName;
    private String ipAddress;
    private String accountName;
    private Long domainId;
    private String domainName;
    private Date created;
    private String state;
    private Boolean haEnable;
    private Long groupId;
    private String group;
    private Long zoneId;
    private String zoneName;
    private Long hostId;
    private String hostName;
    private Long templateId;
    private String templateName;
    private String templateDisplayText;
    private Boolean passwordEnabled;
    private Long isoId;
    private String isoName;
    private String isoDisplayText;
    private Long serviceOfferingId;
    private String serviceOfferingName;
    private Boolean forVirtualNetwork;
    private Integer cpuNumber;
    private Integer cpuSpeed;
    private Integer memory;
    private String cpuUsed;
    private Long networkKbsRead;
    private Long networkKbsWrite;
    private Long diskKbsRead;
    private Long diskKbsWrite;
    private Long diskIORead;
    private Long diskIOWrite;
    private Long guestOsId;
    private Long rootDeviceId;
    private String rootDeviceType;
    private Set<SecurityGroupData> securityGroupList;
    private String password;
    private Long jobId;
    private Integer jobStatus;
    private Set<NicData> nics;
    private String hypervisor;
    private long accountId;
    private Long publicIpId;
    private String publicIp;
    private String instanceName;
    private String sshPublicKey;

    private boolean initialized;

    public UserVmData() {
        securityGroupList = new HashSet<SecurityGroupData>();
        nics = new HashSet<NicData>();
        initialized = false;
    }

    public void setInitialized() {
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public NicData newNicData() {
        return new NicData();
    }

    public SecurityGroupData newSecurityGroupData() {
        return new SecurityGroupData();
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public Long getObjectId() {
        return getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getHaEnable() {
        return haEnable;
    }

    public void setHaEnable(Boolean haEnable) {
        this.haEnable = haEnable;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
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

    public Boolean getPasswordEnabled() {
        return passwordEnabled;
    }

    public void setPasswordEnabled(Boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }

    public Long getIsoId() {
        return isoId;
    }

    public void setIsoId(Long isoId) {
        this.isoId = isoId;
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

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public Integer getCpuNumber() {
        return cpuNumber;
    }

    public void setCpuNumber(Integer cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public Integer getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(Integer cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public String getCpuUsed() {
        return cpuUsed;
    }

    public void setCpuUsed(String cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public Long getNetworkKbsRead() {
        return networkKbsRead;
    }

    public void setNetworkKbsRead(Long networkKbsRead) {
        this.networkKbsRead = networkKbsRead;
    }

    public Long getNetworkKbsWrite() {
        return networkKbsWrite;
    }

    public void setNetworkKbsWrite(Long networkKbsWrite) {
        this.networkKbsWrite = networkKbsWrite;
    }

    public Long getDiskKbsRead() {
        return diskKbsRead;
    }

    public void setDiskKbsRead(Long diskKbsRead) {
        this.diskKbsRead = diskKbsRead;
    }

    public Long getDiskKbsWrite() {
        return diskKbsWrite;
    }

    public void setDiskKbsWrite(Long diskKbsWrite) {
        this.diskKbsWrite = diskKbsWrite;
    }

    public Long getDiskIORead() {
        return diskIORead;
    }

    public void setDiskIORead(Long diskIORead) {
        this.diskIORead = diskIORead;
    }

    public Long getDiskIOWrite() {
        return diskIOWrite;
    }

    public void setDiskIOWrite(Long diskIOWrite) {
        this.diskIOWrite = diskIOWrite;
    }

    public Long getGuestOsId() {
        return guestOsId;
    }

    public void setGuestOsId(Long guestOsId) {
        this.guestOsId = guestOsId;
    }

    public Long getRootDeviceId() {
        return rootDeviceId;
    }

    public void setRootDeviceId(Long rootDeviceId) {
        this.rootDeviceId = rootDeviceId;
    }

    public String getRootDeviceType() {
        return rootDeviceType;
    }

    public void setRootDeviceType(String rootDeviceType) {
        this.rootDeviceType = rootDeviceType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }

    public Boolean getForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public Set<NicData> getNics() {
        return nics;
    }

    public void addNic(NicData nics) {
        this.nics.add(nics);
    }

    public Set<SecurityGroupData> getSecurityGroupList() {
        return securityGroupList;
    }

    public void addSecurityGroup(SecurityGroupData securityGroups) {
        this.securityGroupList.add(securityGroups);
    }

    public class NicData {
        private String objectName;
        private Long id;
        private Long networkid;
        private String netmask;
        private String gateway;
        private String ipaddress;
        private String isolationUri;
        private String broadcastUri;
        private String trafficType;
        private String type;
        private Boolean isDefault;
        private String macAddress;

        public String getObjectName() {
            return objectName;
        }

        public void setObjectName(String objectName) {
            this.objectName = objectName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getNetworkid() {
            return networkid;
        }

        public void setNetworkid(Long networkid) {
            this.networkid = networkid;
        }

        public String getNetmask() {
            return netmask;
        }

        public void setNetmask(String netmask) {
            this.netmask = netmask;
        }

        public String getGateway() {
            return gateway;
        }

        public void setGateway(String gateway) {
            this.gateway = gateway;
        }

        public String getIpaddress() {
            return ipaddress;
        }

        public void setIpaddress(String ipaddress) {
            this.ipaddress = ipaddress;
        }

        public String getIsolationUri() {
            return isolationUri;
        }

        public void setIsolationUri(String isolationUri) {
            this.isolationUri = isolationUri;
        }

        public String getBroadcastUri() {
            return broadcastUri;
        }

        public void setBroadcastUri(String broadcastUri) {
            this.broadcastUri = broadcastUri;
        }

        public String getTrafficType() {
            return trafficType;
        }

        public void setTrafficType(String trafficType) {
            this.trafficType = trafficType;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(Boolean isDefault) {
            this.isDefault = isDefault;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
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
            NicData other = (NicData)obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }
    }

    public class SecurityGroupData {
        private String objectName;
        private Long id;
        private String name;
        private String description;
        private String accountName;
        private Long domainId;
        private String domainName;
        private Long jobId;
        private Integer jobStatus;
        private List<SecurityGroupRuleResponse> securityGroupRules;

        public String getObjectName() {
            return objectName;
        }

        public void setObjectName(String objectName) {
            this.objectName = objectName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public Long getDomainId() {
            return domainId;
        }

        public void setDomainId(Long domainId) {
            this.domainId = domainId;
        }

        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }

        /* FIXME : the below functions are not used, so commenting out later need to include egress list
                public List<SecurityGroupRuleResponse> getIngressRules() {
                    return securityGroupRules;
                }

                public void setIngressRules(List<SecurityGroupRuleResponse> securityGroupRules) {
                    this.securityGroupRules = securityGroupRules;
                } */

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
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
            SecurityGroupData other = (SecurityGroupData)obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

    }

    @Override
    public String toString() {
        return "id=" + id + ", name=" + name;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public Long getPublicIpId() {
        return publicIpId;
    }

    public void setPublicIpId(Long publicIpId) {
        this.publicIpId = publicIpId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getSshPublicKey() {
        return sshPublicKey;
    }

    public void setSshPublicKey(String sshPublicKey) {
        this.sshPublicKey = sshPublicKey;
    }
}
