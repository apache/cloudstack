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

import java.util.Map;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class CapabilitiesResponse extends BaseResponse {
    @SerializedName("securitygroupsenabled")
    @Param(description = "True if security groups support is enabled, false otherwise")
    private boolean securityGroupsEnabled;

    @SerializedName("dynamicrolesenabled")
    @Param(description = "True if dynamic role-based api checker is enabled, false otherwise")
    private boolean dynamicRolesEnabled;

    @SerializedName("cloudstackversion")
    @Param(description = "Version of the CloudStack")
    private String cloudStackVersion;

    @SerializedName("userpublictemplateenabled")
    @Param(description = "True if user and domain admins can set Templates to be shared, false otherwise")
    private boolean userPublicTemplateEnabled;

    @SerializedName("supportELB")
    @Param(description = "True if region supports elastic Load balancer on basic zones")
    private String supportELB;

    @SerializedName(ApiConstants.PROJECT_INVITE_REQUIRED)
    @Param(description = "If invitation confirmation is required when add Account to project")
    private Boolean projectInviteRequired;

    @SerializedName(ApiConstants.ALLOW_USER_CREATE_PROJECTS)
    @Param(description = "True if regular User is allowed to create projects")
    private Boolean allowUsersCreateProjects;

    @SerializedName(ApiConstants.CUSTOM_DISK_OFF_MIN_SIZE)
    @Param(description = "Minimum size that can be specified when " + "create disk from disk offering with custom size")
    private Long diskOffMinSize;

    @SerializedName(ApiConstants.CUSTOM_DISK_OFF_MAX_SIZE)
    @Param(description = "Maximum size that can be specified when " + "create disk from disk offering with custom size")
    private Long diskOffMaxSize;

    @SerializedName("regionsecondaryenabled")
    @Param(description = "True if region wide secondary is enabled, false otherwise")
    private boolean regionSecondaryEnabled;

    @SerializedName("apilimitinterval")
    @Param(description = "Time interval (in seconds) to reset api count")
    private Integer apiLimitInterval;

    @SerializedName("kvmsnapshotenabled")
    @Param(description = "True if Snapshot is supported for KVM host, false otherwise")
    private boolean kvmSnapshotEnabled;

    @SerializedName("snapshotshowchainsize")
    @Param(description = "True to show the parent and chain size (sum of physical size of snapshot and all its parents) for incremental snapshots", since = "4.22.1")
    private boolean snapshotShowChainSize;

    @SerializedName("apilimitmax")
    @Param(description = "Max allowed number of api requests within the specified interval")
    private Integer apiLimitMax;

    @SerializedName("allowuserviewdestroyedvm")
    @Param(description = "True if the User is allowed to view the destroyed Instances, false otherwise", since = "4.6.0")
    private boolean allowUserViewDestroyedVM;

    @SerializedName("allowuserexpungerecovervm")
    @Param(description = "True if the User can recover and expunge Instances, false otherwise", since = "4.6.0")
    private boolean allowUserExpungeRecoverVM;

    @SerializedName("allowuserexpungerecovervolume")
    @Param(description = "True if the User can recover and expunge volumes, false otherwise", since = "4.14.0")
    private boolean allowUserExpungeRecoverVolume;

    @SerializedName("allowuserviewalldomainaccounts")
    @Param(description = "True if Users can see all Accounts within the same domain, false otherwise")
    private boolean allowUserViewAllDomainAccounts;

    @SerializedName(ApiConstants.ALLOW_USER_FORCE_STOP_VM)
    @Param(description = "true if users are allowed to force stop a vm, false otherwise", since = "4.20.0")
    private boolean allowUserForceStopVM;

    @SerializedName("kubernetesserviceenabled")
    @Param(description = "True if Kubernetes Service plugin is enabled, false otherwise")
    private boolean kubernetesServiceEnabled;

    @SerializedName("kubernetesclusterexperimentalfeaturesenabled")
    @Param(description = "True if experimental features for Kubernetes cluster such as Docker private registry are enabled, false otherwise")
    private boolean kubernetesClusterExperimentalFeaturesEnabled;

    @SerializedName("customhypervisordisplayname")
    @Param(description = "Display name for custom hypervisor", since = "4.19.0")
    private String customHypervisorDisplayName;

    @SerializedName("defaultuipagesize")
    @Param(description = "Default page size in the UI for various views, value set in the configurations", since = "4.15.2")
    private Long defaultUiPageSize;

    @SerializedName(ApiConstants.INSTANCES_STATS_RETENTION_TIME)
    @Param(description = "The retention time for Instances stats", since = "4.18.0")
    private Integer instancesStatsRetentionTime;

    @SerializedName(ApiConstants.INSTANCES_STATS_USER_ONLY)
    @Param(description = "True if stats are collected only for User Instances, false if System VM stats are also collected", since = "4.18.0")
    private Boolean instancesStatsUserOnly;

    @SerializedName(ApiConstants.INSTANCES_DISKS_STATS_RETENTION_ENABLED)
    @Param(description = "True if stats are retained for Instance disks otherwise false", since = "4.18.0")
    private Boolean instancesDisksStatsRetentionEnabled;

    @SerializedName(ApiConstants.INSTANCES_DISKS_STATS_RETENTION_TIME)
    @Param(description = "The retention time for Instances disks stats", since = "4.18.0")
    private Integer instancesDisksStatsRetentionTime;

    @SerializedName(ApiConstants.SHAREDFSVM_MIN_CPU_COUNT)
    @Param(description = "the min CPU count for the service offering used by the shared filesystem instance", since = "4.20.0")
    private Integer sharedFsVmMinCpuCount;

    @SerializedName(ApiConstants.SHAREDFSVM_MIN_RAM_SIZE)
    @Param(description = "the min Ram size for the service offering used by the shared filesystem instance", since = "4.20.0")
    private Integer sharedFsVmMinRamSize;

    @SerializedName(ApiConstants.INSTANCE_LEASE_ENABLED)
    @Param(description = "true if instance lease feature is enabled", since = "4.21.0")
    private Boolean instanceLeaseEnabled;

    @SerializedName(ApiConstants.EXTENSIONS_PATH)
    @Param(description = "The path of the extensions directory", since = "4.21.0", authorized = {RoleType.Admin})
    private String extensionsPath;

    @SerializedName(ApiConstants.DYNAMIC_SCALING_ENABLED)
    @Param(description = "true if dynamically scaling for instances is enabled", since = "4.21.0")
    private Boolean dynamicScalingEnabled;

    @SerializedName(ApiConstants.ADDITONAL_CONFIG_ENABLED)
    @Param(description = "true if additional configurations or extraconfig can be passed to Instances", since = "4.20.2")
    private Boolean additionalConfigEnabled;

    @SerializedName(ApiConstants.VPN_CUSTOMER_GATEWAY_PARAMETERS)
    @Param(description = "Excluded and obsolete VPN customer gateway cryptographic parameters")
    private Map<String, Object> vpnCustomerGatewayParameters;

    public void setSecurityGroupsEnabled(boolean securityGroupsEnabled) {
        this.securityGroupsEnabled = securityGroupsEnabled;
    }

    public void setDynamicRolesEnabled(boolean dynamicRolesEnabled) {
        this.dynamicRolesEnabled = dynamicRolesEnabled;
    }

    public void setCloudStackVersion(String cloudStackVersion) {
        this.cloudStackVersion = cloudStackVersion;
    }

    public void setUserPublicTemplateEnabled(boolean userPublicTemplateEnabled) {
        this.userPublicTemplateEnabled = userPublicTemplateEnabled;
    }

    public void setSupportELB(String supportELB) {
        this.supportELB = supportELB;
    }

    public void setProjectInviteRequired(Boolean projectInviteRequired) {
        this.projectInviteRequired = projectInviteRequired;
    }

    public void setAllowUsersCreateProjects(Boolean allowUsersCreateProjects) {
        this.allowUsersCreateProjects = allowUsersCreateProjects;
    }

    public void setDiskOffMinSize(Long diskOffMinSize) {
        this.diskOffMinSize = diskOffMinSize;
    }

    public void setDiskOffMaxSize(Long diskOffMaxSize) {
        this.diskOffMaxSize = diskOffMaxSize;
    }

    public void setRegionSecondaryEnabled(boolean regionSecondaryEnabled) {
        this.regionSecondaryEnabled = regionSecondaryEnabled;
    }

    public void setKVMSnapshotEnabled(boolean kvmSnapshotEnabled) {
        this.kvmSnapshotEnabled = kvmSnapshotEnabled;
    }

    public void setSnapshotShowChainSize(boolean snapshotShowChainSize) {
        this.snapshotShowChainSize = snapshotShowChainSize;
    }

    public void setApiLimitInterval(Integer apiLimitInterval) {
        this.apiLimitInterval = apiLimitInterval;
    }

    public void setApiLimitMax(Integer apiLimitMax) {
        this.apiLimitMax = apiLimitMax;
    }

    public void setAllowUserViewDestroyedVM(boolean allowUserViewDestroyedVM) {
        this.allowUserViewDestroyedVM = allowUserViewDestroyedVM;
    }

    public void setAllowUserExpungeRecoverVM(boolean allowUserExpungeRecoverVM) {
        this.allowUserExpungeRecoverVM = allowUserExpungeRecoverVM;
    }

    public void setAllowUserExpungeRecoverVolume(boolean allowUserExpungeRecoverVolume) {
        this.allowUserExpungeRecoverVolume = allowUserExpungeRecoverVolume;
    }

    public void setAllowUserViewAllDomainAccounts(boolean allowUserViewAllDomainAccounts) {
        this.allowUserViewAllDomainAccounts = allowUserViewAllDomainAccounts;
    }

    public void setAllowUserForceStopVM(boolean allowUserForceStopVM) {
        this.allowUserForceStopVM = allowUserForceStopVM;
    }

    public void setKubernetesServiceEnabled(boolean kubernetesServiceEnabled) {
        this.kubernetesServiceEnabled = kubernetesServiceEnabled;
    }

    public void setKubernetesClusterExperimentalFeaturesEnabled(boolean kubernetesClusterExperimentalFeaturesEnabled) {
        this.kubernetesClusterExperimentalFeaturesEnabled = kubernetesClusterExperimentalFeaturesEnabled;
    }

    public void setDefaultUiPageSize(Long defaultUiPageSize) {
        this.defaultUiPageSize = defaultUiPageSize;
    }

    public void setInstancesStatsRetentionTime(Integer instancesStatsRetentionTime) {
        this.instancesStatsRetentionTime = instancesStatsRetentionTime;
    }

    public void setInstancesStatsUserOnly(Boolean instancesStatsUserOnly) {
        this.instancesStatsUserOnly = instancesStatsUserOnly;
    }

    public void setInstancesDisksStatsRetentionEnabled(Boolean instancesDisksStatsRetentionEnabled) {
        this.instancesDisksStatsRetentionEnabled = instancesDisksStatsRetentionEnabled;
    }

    public void setInstancesDisksStatsRetentionTime(Integer instancesDisksStatsRetentionTime) {
        this.instancesDisksStatsRetentionTime = instancesDisksStatsRetentionTime;
    }

    public void setCustomHypervisorDisplayName(String customHypervisorDisplayName) {
        this.customHypervisorDisplayName = customHypervisorDisplayName;
    }

    public void setSharedFsVmMinCpuCount(Integer sharedFsVmMinCpuCount) {
        this.sharedFsVmMinCpuCount = sharedFsVmMinCpuCount;
    }

    public void setSharedFsVmMinRamSize(Integer sharedFsVmMinRamSize) {
        this.sharedFsVmMinRamSize = sharedFsVmMinRamSize;
    }

    public void setInstanceLeaseEnabled(Boolean instanceLeaseEnabled) {
        this.instanceLeaseEnabled = instanceLeaseEnabled;
    }

    public void setExtensionsPath(String extensionsPath) {
        this.extensionsPath = extensionsPath;
    }

    public void setDynamicScalingEnabled(Boolean dynamicScalingEnabled) {
        this.dynamicScalingEnabled = dynamicScalingEnabled;
    }

    public void setAdditionalConfigEnabled(Boolean additionalConfigEnabled) {
        this.additionalConfigEnabled = additionalConfigEnabled;
    }

    public void setVpnCustomerGatewayParameters(Map<String, Object> vpnCustomerGatewayParameters) {
        this.vpnCustomerGatewayParameters = vpnCustomerGatewayParameters;
    }
}
