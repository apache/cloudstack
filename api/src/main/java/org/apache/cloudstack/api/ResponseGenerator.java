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
package org.apache.cloudstack.api;

import java.text.DecimalFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ApplicationLoadBalancerResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.AutoScalePolicyResponse;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;
import org.apache.cloudstack.api.response.BackupOfferingResponse;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.BackupScheduleResponse;
import org.apache.cloudstack.api.response.CapacityResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ConditionResponse;
import org.apache.cloudstack.api.response.ConfigurationGroupResponse;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.CounterResponse;
import org.apache.cloudstack.api.response.CreateCmdResponse;
import org.apache.cloudstack.api.response.DataCenterGuestIpv6PrefixResponse;
import org.apache.cloudstack.api.response.DirectDownloadCertificateHostStatusResponse;
import org.apache.cloudstack.api.response.DirectDownloadCertificateResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.GuestOsMappingResponse;
import org.apache.cloudstack.api.response.GuestVlanRangeResponse;
import org.apache.cloudstack.api.response.GuestVlanResponse;
import org.apache.cloudstack.api.response.HostForMigrationResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.HypervisorCapabilitiesResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.InternalLoadBalancerElementResponse;
import org.apache.cloudstack.api.response.IpForwardingRuleResponse;
import org.apache.cloudstack.api.response.IsolationMethodResponse;
import org.apache.cloudstack.api.response.LBHealthCheckResponse;
import org.apache.cloudstack.api.response.LBStickinessResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.LoadBalancerConfigResponse;
import org.apache.cloudstack.api.response.LoadBalancerResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.api.response.NetworkACLItemResponse;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.NetworkPermissionsResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.api.response.OvsProviderResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.PortableIpRangeResponse;
import org.apache.cloudstack.api.response.PortableIpResponse;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.cloudstack.api.response.RemoteAccessVpnResponse;
import org.apache.cloudstack.api.response.ResourceCountResponse;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.cloudstack.api.response.ResourceLimitResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.RollingMaintenanceResponse;
import org.apache.cloudstack.api.response.RouterHealthCheckResultResponse;
import org.apache.cloudstack.api.response.SSHKeyPairResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.ServiceResponse;
import org.apache.cloudstack.api.response.Site2SiteCustomerGatewayResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnConnectionResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnGatewayResponse;
import org.apache.cloudstack.api.response.SnapshotPolicyResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.SnapshotScheduleResponse;
import org.apache.cloudstack.api.response.StaticRouteResponse;
import org.apache.cloudstack.api.response.StorageNetworkIpRangeResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.SystemVmInstanceResponse;
import org.apache.cloudstack.api.response.SystemVmResponse;
import org.apache.cloudstack.api.response.TemplatePermissionsResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.TrafficMonitorResponse;
import org.apache.cloudstack.api.response.TrafficTypeResponse;
import org.apache.cloudstack.api.response.UpgradeRouterTemplateResponse;
import org.apache.cloudstack.api.response.UsageRecordResponse;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.cloudstack.api.response.VirtualRouterProviderResponse;
import org.apache.cloudstack.api.response.VlanIpRangeResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.VpnUsersResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.BackupSchedule;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.config.ConfigurationGroup;
import org.apache.cloudstack.direct.download.DirectDownloadCertificate;
import org.apache.cloudstack.direct.download.DirectDownloadCertificateHostMap;
import org.apache.cloudstack.direct.download.DirectDownloadManager;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerRule;
import org.apache.cloudstack.network.lb.LoadBalancerConfig;
import org.apache.cloudstack.region.PortableIp;
import org.apache.cloudstack.region.PortableIpRange;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.usage.Usage;

import com.cloud.capacity.Capacity;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterGuestIpv6Prefix;
import com.cloud.dc.Pod;
import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.event.Event;
import com.cloud.host.Host;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.network.GuestVlan;
import com.cloud.network.GuestVlanRange;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkPermission;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.OvsProvider;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.RouterHealthCheckResult;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VpnUser;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.network.as.Condition;
import com.cloud.network.as.Counter;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityRule;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectInvitation;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.resource.RollingMaintenanceManager;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceIcon;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.storage.ImageStore;
import com.cloud.storage.Snapshot;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.storage.snapshot.SnapshotSchedule;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserData;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.Ip;
import com.cloud.utils.Pair;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;

public interface ResponseGenerator {
    UserResponse createUserResponse(UserAccount user);

    AccountResponse createAccountResponse(ResponseView view, Account account);

    DomainResponse createDomainResponse(Domain domain);

    DiskOfferingResponse createDiskOfferingResponse(DiskOffering offering);

    ResourceLimitResponse createResourceLimitResponse(ResourceLimit limit);

    ResourceCountResponse createResourceCountResponse(ResourceCount resourceCount);

    ServiceOfferingResponse createServiceOfferingResponse(ServiceOffering offering);

    ConfigurationResponse createConfigurationResponse(Configuration cfg);

    ConfigurationGroupResponse createConfigurationGroupResponse(ConfigurationGroup cfgGroup);

    SnapshotResponse createSnapshotResponse(Snapshot snapshot);

    SnapshotPolicyResponse createSnapshotPolicyResponse(SnapshotPolicy policy);

    List<UserVmResponse> createUserVmResponse(ResponseView view, String objectName, UserVm... userVms);

    List<UserVmResponse> createUserVmResponse(ResponseView view, String objectName, EnumSet<VMDetails> details, UserVm... userVms);

    SystemVmResponse createSystemVmResponse(VirtualMachine systemVM);

    DomainRouterResponse createDomainRouterResponse(VirtualRouter router);

    HostResponse createHostResponse(Host host, EnumSet<HostDetails> details);

    HostResponse createHostResponse(Host host);

    HostForMigrationResponse createHostForMigrationResponse(Host host);

    HostForMigrationResponse createHostForMigrationResponse(Host host, EnumSet<HostDetails> details);

    VlanIpRangeResponse createVlanIpRangeResponse(Vlan vlan);

    VlanIpRangeResponse createVlanIpRangeResponse(Class<? extends VlanIpRangeResponse> subClass, Vlan vlan);

    IPAddressResponse createIPAddressResponse(ResponseView view, IpAddress ipAddress);

    GuestVlanRangeResponse createDedicatedGuestVlanRangeResponse(GuestVlanRange result);

    GlobalLoadBalancerResponse createGlobalLoadBalancerResponse(GlobalLoadBalancerRule globalLoadBalancerRule);

    LoadBalancerResponse createLoadBalancerResponse(LoadBalancer loadBalancer);

    LoadBalancerConfigResponse createLoadBalancerConfigResponse(LoadBalancerConfig config);

    List<LoadBalancerConfigResponse> createLoadBalancerConfigResponse(List<? extends LoadBalancerConfig> configs);

    LBStickinessResponse createLBStickinessPolicyResponse(List<? extends StickinessPolicy> stickinessPolicies, LoadBalancer lb);

    LBStickinessResponse createLBStickinessPolicyResponse(StickinessPolicy stickinessPolicy, LoadBalancer lb);

    LBHealthCheckResponse createLBHealthCheckPolicyResponse(List<? extends HealthCheckPolicy> healthcheckPolicies, LoadBalancer lb);

    LBHealthCheckResponse createLBHealthCheckPolicyResponse(HealthCheckPolicy healthcheckPolicy, LoadBalancer lb);

    PodResponse createPodResponse(Pod pod, Boolean showCapacities);

    ZoneResponse createZoneResponse(ResponseView view, DataCenter dataCenter, Boolean showCapacities, Boolean showResourceIcon);

    DataCenterGuestIpv6PrefixResponse createDataCenterGuestIpv6PrefixResponse(DataCenterGuestIpv6Prefix prefix);

    VolumeResponse createVolumeResponse(ResponseView view, Volume volume);

    InstanceGroupResponse createInstanceGroupResponse(InstanceGroup group);

    StoragePoolResponse createStoragePoolResponse(StoragePool pool);

    StoragePoolResponse createStoragePoolForMigrationResponse(StoragePool pool);

    ClusterResponse createClusterResponse(Cluster cluster, Boolean showCapacities);

    FirewallRuleResponse createPortForwardingRuleResponse(PortForwardingRule fwRule);

    IpForwardingRuleResponse createIpForwardingRuleResponse(StaticNatRule fwRule);

    User findUserById(Long userId);

    UserVm findUserVmById(Long vmId);

    Volume findVolumeById(Long volumeId);

    Account findAccountByNameDomain(String accountName, Long domainId);

    VirtualMachineTemplate findTemplateById(Long templateId);

    Host findHostById(Long hostId);

    DiskOffering findDiskOfferingById(Long diskOfferingId);

    VpnUsersResponse createVpnUserResponse(VpnUser user);

    RemoteAccessVpnResponse createRemoteAccessVpnResponse(RemoteAccessVpn vpn);

    List<TemplateResponse> createTemplateResponses(ResponseView view, long templateId, Long zoneId, boolean readyOnly);

    List<TemplateResponse> createTemplateResponses(ResponseView view, long templateId, Long snapshotId, Long volumeId, boolean readyOnly);

    SecurityGroupResponse createSecurityGroupResponseFromSecurityGroupRule(List<? extends SecurityRule> securityRules);

    SecurityGroupResponse createSecurityGroupResponse(SecurityGroup group);

    ExtractResponse createExtractResponse(Long uploadId, Long id, Long zoneId, Long accountId, String mode, String url);

    ExtractResponse createExtractResponse(Long id, Long zoneId, Long accountId, String mode, String url);

    String toSerializedString(CreateCmdResponse response, String responseType);

    EventResponse createEventResponse(Event event);

    TemplateResponse createTemplateUpdateResponse(ResponseView view, VirtualMachineTemplate result);

    List<TemplateResponse> createTemplateResponses(ResponseView view, VirtualMachineTemplate result,
                                                   Long zoneId, boolean readyOnly);

    List<TemplateResponse> createTemplateResponses(ResponseView view, VirtualMachineTemplate result,
                                                   List<Long> zoneIds, boolean readyOnly);

    List<CapacityResponse> createCapacityResponse(List<? extends Capacity> result, DecimalFormat format);

    TemplatePermissionsResponse createTemplatePermissionsResponse(ResponseView view, List<String> accountNames, Long id);

    AsyncJobResponse queryJobResult(QueryAsyncJobResultCmd cmd);

    NetworkOfferingResponse createNetworkOfferingResponse(NetworkOffering offering);

    NetworkResponse createNetworkResponse(ResponseView view, Network network);

    UserResponse createUserResponse(User user);

    AccountResponse createUserAccountResponse(ResponseView view, UserAccount user);

    Long getSecurityGroupId(String groupName, long accountId);

    List<TemplateResponse> createIsoResponses(ResponseView view, VirtualMachineTemplate iso, Long zoneId, boolean readyOnly);

    ProjectResponse createProjectResponse(Project project);

    List<TemplateResponse> createTemplateResponses(ResponseView view, long templateId, Long vmId);

    FirewallResponse createFirewallResponse(FirewallRule fwRule);

    HypervisorCapabilitiesResponse createHypervisorCapabilitiesResponse(HypervisorCapabilities hpvCapabilities);

    ProjectAccountResponse createProjectAccountResponse(ProjectAccount projectAccount);

    ProjectInvitationResponse createProjectInvitationResponse(ProjectInvitation invite);

    SystemVmInstanceResponse createSystemVmInstanceResponse(VirtualMachine systemVM);

    PhysicalNetworkResponse createPhysicalNetworkResponse(PhysicalNetwork result);

    ServiceResponse createNetworkServiceResponse(Service service);

    ProviderResponse createNetworkServiceProviderResponse(PhysicalNetworkServiceProvider result);

    TrafficTypeResponse createTrafficTypeResponse(PhysicalNetworkTrafficType result);

    VirtualRouterProviderResponse createVirtualRouterProviderResponse(VirtualRouterProvider result);

    OvsProviderResponse createOvsProviderResponse(OvsProvider result);

    StorageNetworkIpRangeResponse createStorageNetworkIpRangeResponse(StorageNetworkIpRange result);

    RegionResponse createRegionResponse(Region region);

    ImageStoreResponse createImageStoreResponse(ImageStore os);

    /**
     * @param resourceTag
     * @param keyValueOnly TODO
     * @return
     */
    ResourceTagResponse createResourceTagResponse(ResourceTag resourceTag, boolean keyValueOnly);

    Site2SiteVpnGatewayResponse createSite2SiteVpnGatewayResponse(Site2SiteVpnGateway result);

    /**
     * @param offering
     * @return
     */
    VpcOfferingResponse createVpcOfferingResponse(VpcOffering offering);

    /**
     * @param vpc
     * @return
     */
    VpcResponse createVpcResponse(ResponseView view, Vpc vpc);

    /**
     * @param networkACLItem
     * @return
     */
    NetworkACLItemResponse createNetworkACLItemResponse(NetworkACLItem networkACLItem);

    /**
     * @param networkACL
     * @return
     */
    NetworkACLResponse createNetworkACLResponse(NetworkACL networkACL);

    /**
     * @param result
     * @return
     */
    PrivateGatewayResponse createPrivateGatewayResponse(PrivateGateway result);

    /**
     * @param result
     * @return
     */
    StaticRouteResponse createStaticRouteResponse(StaticRoute result);

    Site2SiteCustomerGatewayResponse createSite2SiteCustomerGatewayResponse(Site2SiteCustomerGateway result);

    Site2SiteVpnConnectionResponse createSite2SiteVpnConnectionResponse(Site2SiteVpnConnection result);

    CounterResponse createCounterResponse(Counter ctr);

    ConditionResponse createConditionResponse(Condition cndn);

    AutoScalePolicyResponse createAutoScalePolicyResponse(AutoScalePolicy policy);

    AutoScaleVmProfileResponse createAutoScaleVmProfileResponse(AutoScaleVmProfile profile);

    AutoScaleVmGroupResponse createAutoScaleVmGroupResponse(AutoScaleVmGroup vmGroup);

    GuestOSResponse createGuestOSResponse(GuestOS os);

    GuestOsMappingResponse createGuestOSMappingResponse(GuestOSHypervisor osHypervisor);

    SnapshotScheduleResponse createSnapshotScheduleResponse(SnapshotSchedule sched);

    UsageRecordResponse createUsageResponse(Usage usageRecord);

    UsageRecordResponse createUsageResponse(Usage usageRecord, Map<String, Set<ResourceTagResponse>> resourceTagResponseMap, boolean oldFormat);

    public Map<String, Set<ResourceTagResponse>> getUsageResourceTags();

    TrafficMonitorResponse createTrafficMonitorResponse(Host trafficMonitor);

    VMSnapshotResponse createVMSnapshotResponse(VMSnapshot vmSnapshot);

    NicSecondaryIpResponse createSecondaryIPToNicResponse(NicSecondaryIp result);

    public NicResponse createNicResponse(Nic result);

    ApplicationLoadBalancerResponse createLoadBalancerContainerReponse(ApplicationLoadBalancerRule lb, Map<Ip, UserVm> lbInstances);

    AffinityGroupResponse createAffinityGroupResponse(AffinityGroup group);

    Long getAffinityGroupId(String name, long entityOwnerId);

    PortableIpRangeResponse createPortableIPRangeResponse(PortableIpRange range);

    PortableIpResponse createPortableIPResponse(PortableIp portableIp);

    InternalLoadBalancerElementResponse createInternalLbElementResponse(VirtualRouterProvider result);

    IsolationMethodResponse createIsolationMethodResponse(IsolationType method);

    ListResponse<UpgradeRouterTemplateResponse> createUpgradeRouterTemplateResponse(List<Long> jobIds);

    SSHKeyPairResponse createSSHKeyPairResponse(SSHKeyPair sshkeyPair, boolean privatekey);

    UserDataResponse createUserDataResponse(UserData userData);

    BackupResponse createBackupResponse(Backup backup);

    BackupScheduleResponse createBackupScheduleResponse(BackupSchedule backup);

    BackupOfferingResponse createBackupOfferingResponse(BackupOffering policy);

    ManagementServerResponse createManagementResponse(ManagementServerHost mgmt);

    List<RouterHealthCheckResultResponse> createHealthCheckResponse(VirtualMachine router, List<RouterHealthCheckResult> healthCheckResults);

    RollingMaintenanceResponse createRollingMaintenanceResponse(Boolean success, String details, List<RollingMaintenanceManager.HostUpdated> hostsUpdated, List<RollingMaintenanceManager.HostSkipped> hostsSkipped);

    ResourceIconResponse createResourceIconResponse(ResourceIcon resourceIcon);

    GuestVlanResponse createGuestVlanResponse(GuestVlan vlan);

    NetworkPermissionsResponse createNetworkPermissionsResponse(NetworkPermission permission);

    DirectDownloadCertificateResponse createDirectDownloadCertificateResponse(DirectDownloadCertificate certificate);

    List<DirectDownloadCertificateHostStatusResponse> createDirectDownloadCertificateHostMapResponse(List<DirectDownloadCertificateHostMap> hostMappings);

    DirectDownloadCertificateHostStatusResponse createDirectDownloadCertificateHostStatusResponse(DirectDownloadManager.HostCertificateStatus status);

    DirectDownloadCertificateHostStatusResponse createDirectDownloadCertificateProvisionResponse(Long certificateId, Long hostId, Pair<Boolean, String> result);

    FirewallResponse createIpv6FirewallRuleResponse(FirewallRule acl);
}
