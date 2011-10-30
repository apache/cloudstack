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
package com.cloud.api;

import java.text.DecimalFormat;
import java.util.List;

import com.cloud.api.commands.QueryAsyncJobResultCmd;
import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.AsyncJobResponse;
import com.cloud.api.response.CapacityResponse;
import com.cloud.api.response.ClusterResponse;
import com.cloud.api.response.ConfigurationResponse;
import com.cloud.api.response.CreateCmdResponse;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.api.response.EventResponse;
import com.cloud.api.response.ExtractResponse;
import com.cloud.api.response.FirewallResponse;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.HypervisorCapabilitiesResponse;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.api.response.InstanceGroupResponse;
import com.cloud.api.response.IpForwardingRuleResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.api.response.NetworkOfferingResponse;
import com.cloud.api.response.NetworkResponse;
import com.cloud.api.response.PodResponse;
import com.cloud.api.response.ProjectAccountResponse;
import com.cloud.api.response.ProjectInvitationResponse;
import com.cloud.api.response.ProjectResponse;
import com.cloud.api.response.RemoteAccessVpnResponse;
import com.cloud.api.response.ResourceCountResponse;
import com.cloud.api.response.ResourceLimitResponse;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.api.response.SystemVmInstanceResponse;
import com.cloud.api.response.SystemVmResponse;
import com.cloud.api.response.TemplatePermissionsResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.api.response.UserResponse;
import com.cloud.api.response.UserVmResponse;
import com.cloud.api.response.VlanIpRangeResponse;
import com.cloud.api.response.VolumeResponse;
import com.cloud.api.response.VpnUsersResponse;
import com.cloud.api.response.ZoneResponse;
import com.cloud.async.AsyncJob;
import com.cloud.capacity.Capacity;
import com.cloud.configuration.Configuration;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.event.Event;
import com.cloud.host.Host;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.security.EgressRule;
import com.cloud.network.security.IngressRule;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupRules;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectInvitation;
import com.cloud.storage.Snapshot;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.uservm.UserVm;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.VirtualMachine;

public interface ResponseGenerator {
    UserResponse createUserResponse(UserAccount user);

    AccountResponse createAccountResponse(Account account);

    DomainResponse createDomainResponse(Domain domain);

    DiskOfferingResponse createDiskOfferingResponse(DiskOffering offering);

    ResourceLimitResponse createResourceLimitResponse(ResourceLimit limit);

    ResourceCountResponse createResourceCountResponse(ResourceCount resourceCount);

    ServiceOfferingResponse createServiceOfferingResponse(ServiceOffering offering);

    ConfigurationResponse createConfigurationResponse(Configuration cfg);

    SnapshotResponse createSnapshotResponse(Snapshot snapshot);

    SnapshotPolicyResponse createSnapshotPolicyResponse(SnapshotPolicy policy);
    
    List<UserVmResponse> createUserVmResponse(String objectName, UserVm... userVms);

    List<UserVmResponse> createUserVmResponse(String objectName, int details, UserVm... userVms);

    SystemVmResponse createSystemVmResponse(VirtualMachine systemVM);

    DomainRouterResponse createDomainRouterResponse(VirtualRouter router);

    HostResponse createHostResponse(Host host);

    VlanIpRangeResponse createVlanIpRangeResponse(Vlan vlan);

    IPAddressResponse createIPAddressResponse(IpAddress ipAddress);

    LoadBalancerResponse createLoadBalancerResponse(LoadBalancer loadBalancer);

    PodResponse createPodResponse(Pod pod, Boolean showCapacities);

    ZoneResponse createZoneResponse(DataCenter dataCenter, Boolean showCapacities);

    VolumeResponse createVolumeResponse(Volume volume);

    InstanceGroupResponse createInstanceGroupResponse(InstanceGroup group);

    StoragePoolResponse createStoragePoolResponse(StoragePool pool);

    ClusterResponse createClusterResponse(Cluster cluster, Boolean showCapacities);

    FirewallRuleResponse createPortForwardingRuleResponse(PortForwardingRule fwRule);

    IpForwardingRuleResponse createIpForwardingRuleResponse(StaticNatRule fwRule);

    User findUserById(Long userId);

    UserVm findUserVmById(Long vmId);

    Volume findVolumeById(Long volumeId);

    Account findAccountByNameDomain(String accountName, Long domainId);

    VirtualMachineTemplate findTemplateById(Long templateId);

    Host findHostById(Long hostId);

    List<TemplateResponse> createTemplateResponses(long templateId, long zoneId, boolean readyOnly);

    VpnUsersResponse createVpnUserResponse(VpnUser user);

    RemoteAccessVpnResponse createRemoteAccessVpnResponse(RemoteAccessVpn vpn);
    List<TemplateResponse> createTemplateResponses(long templateId, Long zoneId, boolean readyOnly);

    List<TemplateResponse> createTemplateResponses(long templateId, Long snapshotId, Long volumeId, boolean readyOnly);

    ListResponse<SecurityGroupResponse> createSecurityGroupResponses(List<? extends SecurityGroupRules> networkGroups);

    SecurityGroupResponse createSecurityGroupResponseFromIngressRule(List<? extends IngressRule> ingressRules);

    SecurityGroupResponse createSecurityGroupResponseFromEgressRule(List<? extends EgressRule> egressRules);

    SecurityGroupResponse createSecurityGroupResponse(SecurityGroup group);

    ExtractResponse createExtractResponse(Long uploadId, Long id, Long zoneId, Long accountId, String mode);

    String toSerializedString(CreateCmdResponse response, String responseType);

    AsyncJobResponse createAsyncJobResponse(AsyncJob job);

    EventResponse createEventResponse(Event event);

    TemplateResponse createIsoResponse(VirtualMachineTemplate result);

    List<CapacityResponse> createCapacityResponse(List<? extends Capacity> result, DecimalFormat format);

    TemplatePermissionsResponse createTemplatePermissionsResponse(List<String> accountNames, Long id, boolean isAdmin);

    AsyncJobResponse queryJobResult(QueryAsyncJobResultCmd cmd);

    NetworkOfferingResponse createNetworkOfferingResponse(NetworkOffering offering);

    NetworkResponse createNetworkResponse(Network network);

    UserResponse createUserResponse(User user);

    AccountResponse createUserAccountResponse(UserAccount user);

    Long getSecurityGroupId(String groupName, long accountId);

    List<TemplateResponse> createIsoResponses(long isoId, Long zoneId, boolean readyOnly);

    ProjectResponse createProjectResponse(Project project);

    List<TemplateResponse> createIsoResponses(VirtualMachineTemplate iso, long zoneId, boolean readyOnly);

    List<TemplateResponse> createTemplateResponses(long templateId, Long vmId);

    FirewallResponse createFirewallResponse(FirewallRule fwRule);

    HypervisorCapabilitiesResponse createHypervisorCapabilitiesResponse(HypervisorCapabilities hpvCapabilities);
    
    ProjectAccountResponse createProjectAccountResponse(ProjectAccount projectAccount);

    ProjectInvitationResponse createProjectInvitationResponse(ProjectInvitation invite);

    SystemVmInstanceResponse createSystemVmInstanceResponse(VirtualMachine systemVM);

}
