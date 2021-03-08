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
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.user.Account;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Account.class)
public class AccountResponse extends BaseResponse implements ResourceLimitAndCountResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the account")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the account")
    private String name;

    @SerializedName(ApiConstants.ACCOUNT_TYPE)
    @Param(description = "account type (admin, domain-admin, user)")
    private Short accountType;

    @SerializedName(ApiConstants.ROLE_ID)
    @Param(description = "the ID of the role")
    private String roleId;

    @SerializedName(ApiConstants.ROLE_TYPE)
    @Param(description = "the type of the role (Admin, ResourceAdmin, DomainAdmin, User)")
    private String roleType;

    @SerializedName(ApiConstants.ROLE_NAME)
    @Param(description = "the name of the role")
    private String roleName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "id of the Domain the account belongs to")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "name of the Domain the account belongs to")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the Domain the account belongs to", since = "4.13")
    private String domainPath;

    @SerializedName(ApiConstants.DEFAULT_ZONE_ID)
    @Param(description = "the default zone of the account")
    private String defaultZoneId;

    @SerializedName(ApiConstants.RECEIVED_BYTES)
    @Param(description = "the total number of network traffic bytes received")
    private Long bytesReceived;

    @SerializedName(ApiConstants.SENT_BYTES)
    @Param(description = "the total number of network traffic bytes sent")
    private Long bytesSent;

    @SerializedName(ApiConstants.VM_LIMIT)
    @Param(description = "the total number of virtual machines that can be deployed by this account")
    private String vmLimit;

    @SerializedName(ApiConstants.VM_TOTAL)
    @Param(description = "the total number of virtual machines deployed by this account")
    private Long vmTotal;

    @SerializedName(ApiConstants.VM_AVAILABLE)
    @Param(description = "the total number of virtual machines available for this account to acquire")
    private String vmAvailable;

    @SerializedName(ApiConstants.IP_LIMIT)
    @Param(description = "the total number of public ip addresses this account can acquire")
    private String ipLimit;

    @SerializedName(ApiConstants.IP_TOTAL)
    @Param(description = "the total number of public ip addresses allocated for this account")
    private Long ipTotal;

    @SerializedName(ApiConstants.IP_AVAILABLE)
    @Param(description = "the total number of public ip addresses available for this account to acquire")
    private String ipAvailable;

    @SerializedName("volumelimit")
    @Param(description = "the total volume which can be used by this account")
    private String volumeLimit;

    @SerializedName("volumetotal")
    @Param(description = "the total volume being used by this account")
    private Long volumeTotal;

    @SerializedName("volumeavailable")
    @Param(description = "the total volume available for this account")
    private String volumeAvailable;

    @SerializedName("snapshotlimit")
    @Param(description = "the total number of snapshots which can be stored by this account")
    private String snapshotLimit;

    @SerializedName("snapshottotal")
    @Param(description = "the total number of snapshots stored by this account")
    private Long snapshotTotal;

    @SerializedName("snapshotavailable")
    @Param(description = "the total number of snapshots available for this account")
    private String snapshotAvailable;

    @SerializedName("templatelimit")
    @Param(description = "the total number of templates which can be created by this account")
    private String templateLimit;

    @SerializedName("templatetotal")
    @Param(description = "the total number of templates which have been created by this account")
    private Long templateTotal;

    @SerializedName("templateavailable")
    @Param(description = "the total number of templates available to be created by this account")
    private String templateAvailable;

    @SerializedName("vmstopped")
    @Param(description = "the total number of virtual machines stopped for this account")
    private Integer vmStopped;

    @SerializedName("vmrunning")
    @Param(description = "the total number of virtual machines running for this account")
    private Integer vmRunning;

    @SerializedName("projectlimit")
    @Param(description = "the total number of projects the account can own", since = "3.0.1")
    private String projectLimit;

    @SerializedName("projecttotal")
    @Param(description = "the total number of projects being administrated by this account", since = "3.0.1")
    private Long projectTotal;

    @SerializedName("projectavailable")
    @Param(description = "the total number of projects available for administration by this account", since = "3.0.1")
    private String projectAvailable;

    @SerializedName("networklimit")
    @Param(description = "the total number of networks the account can own", since = "3.0.1")
    private String networkLimit;

    @SerializedName("networktotal")
    @Param(description = "the total number of networks owned by account", since = "3.0.1")
    private Long networkTotal;

    @SerializedName("networkavailable")
    @Param(description = "the total number of networks available to be created for this account", since = "3.0.1")
    private String networkAvailable;

    @SerializedName("vpclimit")
    @Param(description = "the total number of vpcs the account can own", since = "4.0.0")
    private String vpcLimit;

    @SerializedName("vpctotal")
    @Param(description = "the total number of vpcs owned by account", since = "4.0.0")
    private Long vpcTotal;

    @SerializedName("vpcavailable")
    @Param(description = "the total number of vpcs available to be created for this account", since = "4.0.0")
    private String vpcAvailable;

    @SerializedName("cpulimit")
    @Param(description = "the total number of cpu cores the account can own", since = "4.2.0")
    private String cpuLimit;

    @SerializedName("cputotal")
    @Param(description = "the total number of cpu cores owned by account", since = "4.2.0")
    private Long cpuTotal;

    @SerializedName("cpuavailable")
    @Param(description = "the total number of cpu cores available to be created for this account", since = "4.2.0")
    private String cpuAvailable;

    @SerializedName("memorylimit")
    @Param(description = "the total memory (in MB) the account can own", since = "4.2.0")
    private String memoryLimit;

    @SerializedName("memorytotal")
    @Param(description = "the total memory (in MB) owned by account", since = "4.2.0")
    private Long memoryTotal;

    @SerializedName("memoryavailable")
    @Param(description = "the total memory (in MB) available to be created for this account", since = "4.2.0")
    private String memoryAvailable;

    @SerializedName("primarystoragelimit")
    @Param(description = "the total primary storage space (in GiB) the account can own", since = "4.2.0")
    private String primaryStorageLimit;

    @SerializedName("primarystoragetotal")
    @Param(description = "the total primary storage space (in GiB) owned by account", since = "4.2.0")
    private Long primaryStorageTotal;

    @SerializedName("primarystorageavailable")
    @Param(description = "the total primary storage space (in GiB) available to be used for this account", since = "4.2.0")
    private String primaryStorageAvailable;

    @SerializedName("secondarystoragelimit")
    @Param(description = "the total secondary storage space (in GiB) the account can own", since = "4.2.0")
    private String secondaryStorageLimit;

    @SerializedName("secondarystoragetotal")
    @Param(description = "the total secondary storage space (in GiB) owned by account", since = "4.2.0")
    private float secondaryStorageTotal;

    @SerializedName("secondarystorageavailable")
    @Param(description = "the total secondary storage space (in GiB) available to be used for this account", since = "4.2.0")
    private String secondaryStorageAvailable;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the account")
    private String state;

    @SerializedName(ApiConstants.IS_CLEANUP_REQUIRED)
    @Param(description = "true if the account requires cleanup")
    private Boolean cleanupRequired;

    @SerializedName(ApiConstants.CREATED)
    @Param(description="the date when this account was created")
    private Date created;

    @SerializedName("user")
    @Param(description = "the list of users associated with account", responseObject = UserResponse.class)
    private List<UserResponse> users;

    @SerializedName(ApiConstants.NETWORK_DOMAIN)
    @Param(description = "the network domain")
    private String networkDomain;

    @SerializedName(ApiConstants.ACCOUNT_DETAILS)
    @Param(description = "details for the account")
    private Map<String, String> details;

    @SerializedName(ApiConstants.IS_DEFAULT)
    @Param(description = "true if account is default, false otherwise", since = "4.2.0")
    private Boolean isDefault;

    @SerializedName(ApiConstants.IAM_GROUPS)
    @Param(description = "the list of acl groups that account belongs to", since = "4.4")
    private List<String> groups;

    @Override
    public String getObjectId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccountType(Short accountType) {
        this.accountType = accountType;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setRoleType(RoleType roleType) {
        if (roleType != null) {
            this.roleType = roleType.name();
        }
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public void setBytesReceived(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public void setBytesSent(Long bytesSent) {
        this.bytesSent = bytesSent;
    }

    @Override
    public void setVmLimit(String vmLimit) {
        this.vmLimit = vmLimit;
    }

    @Override
    public void setVmTotal(Long vmTotal) {
        this.vmTotal = vmTotal;
    }

    @Override
    public void setVmAvailable(String vmAvailable) {
        this.vmAvailable = vmAvailable;
    }

    @Override
    public void setIpLimit(String ipLimit) {
        this.ipLimit = ipLimit;
    }

    @Override
    public void setIpTotal(Long ipTotal) {
        this.ipTotal = ipTotal;
    }

    @Override
    public void setIpAvailable(String ipAvailable) {
        this.ipAvailable = ipAvailable;
    }

    @Override
    public void setVolumeLimit(String volumeLimit) {
        this.volumeLimit = volumeLimit;
    }

    @Override
    public void setVolumeTotal(Long volumeTotal) {
        this.volumeTotal = volumeTotal;
    }

    @Override
    public void setVolumeAvailable(String volumeAvailable) {
        this.volumeAvailable = volumeAvailable;
    }

    @Override
    public void setSnapshotLimit(String snapshotLimit) {
        this.snapshotLimit = snapshotLimit;
    }

    @Override
    public void setSnapshotTotal(Long snapshotTotal) {
        this.snapshotTotal = snapshotTotal;
    }

    @Override
    public void setSnapshotAvailable(String snapshotAvailable) {
        this.snapshotAvailable = snapshotAvailable;
    }

    @Override
    public void setTemplateLimit(String templateLimit) {
        this.templateLimit = templateLimit;
    }

    @Override
    public void setTemplateTotal(Long templateTotal) {
        this.templateTotal = templateTotal;
    }

    @Override
    public void setTemplateAvailable(String templateAvailable) {
        this.templateAvailable = templateAvailable;
    }

    @Override
    public void setVmStopped(Integer vmStopped) {
        this.vmStopped = vmStopped;
    }

    @Override
    public void setVmRunning(Integer vmRunning) {
        this.vmRunning = vmRunning;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setCleanupRequired(Boolean cleanupRequired) {
        this.cleanupRequired = cleanupRequired;
    }

    public void setUsers(List<UserResponse> users) {
        this.users = users;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public void setProjectLimit(String projectLimit) {
        this.projectLimit = projectLimit;
    }

    public void setProjectTotal(Long projectTotal) {
        this.projectTotal = projectTotal;
    }

    public void setProjectAvailable(String projectAvailable) {
        this.projectAvailable = projectAvailable;
    }

    @Override
    public void setNetworkLimit(String networkLimit) {
        this.networkLimit = networkLimit;
    }

    @Override
    public void setNetworkTotal(Long networkTotal) {
        this.networkTotal = networkTotal;
    }

    @Override
    public void setNetworkAvailable(String networkAvailable) {
        this.networkAvailable = networkAvailable;
    }

    @Override
    public void setVpcLimit(String vpcLimit) {
        this.vpcLimit = networkLimit;
    }

    @Override
    public void setVpcTotal(Long vpcTotal) {
        this.vpcTotal = vpcTotal;
    }

    @Override
    public void setVpcAvailable(String vpcAvailable) {
        this.vpcAvailable = vpcAvailable;
    }

    @Override
    public void setCpuLimit(String cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    @Override
    public void setCpuTotal(Long cpuTotal) {
        this.cpuTotal = cpuTotal;
    }

    @Override
    public void setCpuAvailable(String cpuAvailable) {
        this.cpuAvailable = cpuAvailable;
    }

    @Override
    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    @Override
    public void setMemoryTotal(Long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    @Override
    public void setMemoryAvailable(String memoryAvailable) {
        this.memoryAvailable = memoryAvailable;
    }

    @Override
    public void setPrimaryStorageLimit(String primaryStorageLimit) {
        this.primaryStorageLimit = primaryStorageLimit;
    }

    @Override
    public void setPrimaryStorageTotal(Long primaryStorageTotal) {
        this.primaryStorageTotal = primaryStorageTotal;
    }

    @Override
    public void setPrimaryStorageAvailable(String primaryStorageAvailable) {
        this.primaryStorageAvailable = primaryStorageAvailable;
    }

    @Override
    public void setSecondaryStorageLimit(String secondaryStorageLimit) {
        this.secondaryStorageLimit = secondaryStorageLimit;
    }

    @Override
    public void setSecondaryStorageTotal(float secondaryStorageTotal) {
        this.secondaryStorageTotal = secondaryStorageTotal;
    }

    @Override
    public void setSecondaryStorageAvailable(String secondaryStorageAvailable) {
        this.secondaryStorageAvailable = secondaryStorageAvailable;
    }

    public void setDefaultZone(String defaultZoneId) {
        this.defaultZoneId = defaultZoneId;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

}
