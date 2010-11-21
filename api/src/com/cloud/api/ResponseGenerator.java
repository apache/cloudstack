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
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.api.response.InstanceGroupResponse;
import com.cloud.api.response.IpForwardingRuleResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.api.response.NetworkGroupResponse;
import com.cloud.api.response.PodResponse;
import com.cloud.api.response.PreallocatedLunResponse;
import com.cloud.api.response.RemoteAccessVpnResponse;
import com.cloud.api.response.ResourceLimitResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.api.response.StoragePoolResponse;
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
import com.cloud.configuration.ResourceLimit;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.event.Event;
import com.cloud.host.Host;
import com.cloud.network.IpAddress;
import com.cloud.network.LoadBalancer;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.security.NetworkGroup;
import com.cloud.network.security.NetworkGroupRules;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
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

    ServiceOfferingResponse createServiceOfferingResponse(ServiceOffering offering);

    ConfigurationResponse createConfigurationResponse(Configuration cfg);

    SnapshotResponse createSnapshotResponse(Snapshot snapshot);

    SnapshotPolicyResponse createSnapshotPolicyResponse(SnapshotPolicy policy);

    UserVmResponse createUserVmResponse(UserVm userVm);

    SystemVmResponse createSystemVmResponse(VirtualMachine systemVM);

    DomainRouterResponse createDomainRouterResponse(VirtualRouter router);

    HostResponse createHostResponse(Host host);

    VlanIpRangeResponse createVlanIpRangeResponse(Vlan vlan);

    IPAddressResponse createIPAddressResponse(IpAddress ipAddress);

    LoadBalancerResponse createLoadBalancerResponse(LoadBalancer loadBalancer);

    PodResponse createPodResponse(Pod pod);

    ZoneResponse createZoneResponse(DataCenter dataCenter);

    VolumeResponse createVolumeResponse(Volume volume);

    InstanceGroupResponse createInstanceGroupResponse(InstanceGroup group);

    PreallocatedLunResponse createPreallocatedLunResponse(Object preallocatedLun);

    StoragePoolResponse createStoragePoolResponse(StoragePool pool);

    ClusterResponse createClusterResponse(Cluster cluster);

    FirewallRuleResponse createFirewallRuleResponse(FirewallRule fwRule);

    IpForwardingRuleResponse createIpForwardingRuleResponse(FirewallRule fwRule);

    UserVmResponse createUserVm2Response(UserVm userVm);

    DomainRouterResponse createDomainRouter2Response(VirtualRouter router);

    SystemVmResponse createSystemVm2Response(VirtualMachine systemVM);

    void synchronizeCommand(Object job, String syncObjType, long syncObjId);

    User findUserById(long userId);

    UserVm findUserVmById(long vmId);

    Volume findVolumeById(long volumeId);

    Account findAccountByNameDomain(String accountName, long domainId);

    VirtualMachineTemplate findTemplateById(long templateId);

    VpnUsersResponse createVpnUserResponse(VpnUser user);

    RemoteAccessVpnResponse createRemoteAccessVpnResponse(RemoteAccessVpn vpn);

    void createTemplateResponse(List<TemplateResponse> responses, VirtualMachineTemplate template, boolean onlyReady, Long zoneId, boolean isAdmin,
            Account account);

    ListResponse<TemplateResponse> createTemplateResponse2(VirtualMachineTemplate template, Long zoneId);

    ListResponse<TemplateResponse> createIsoResponses(VirtualMachineTemplate template, Long zoneId);

    ListResponse<NetworkGroupResponse> createNetworkGroupResponses(List<? extends NetworkGroupRules> networkGroups);

    NetworkGroupResponse createNetworkGroupResponse(NetworkGroup group);

    ExtractResponse createExtractResponse(long uploadId, long id, long zoneId, long accountId, String mode);

    TemplateResponse createTemplateResponse(VirtualMachineTemplate template, long destZoneId);

    TemplateResponse createIsoResponse3(VirtualMachineTemplate iso, long destZoneId);

    String toSerializedString(CreateCmdResponse response, String responseType);

    AsyncJobResponse createAsyncJobResponse(AsyncJob job);

    TemplateResponse createTemplateResponse(VirtualMachineTemplate template, Long snapshotId, long volumeId);

    EventResponse createEventResponse(Event event);

    ListResponse<TemplateResponse> createIsoResponse(List<? extends VirtualMachineTemplate> isos, Long zoneId, boolean onlyReady, boolean isAdmin,
            Account account);

    TemplateResponse createIsoResponse(VirtualMachineTemplate result);

    List<CapacityResponse> createCapacityResponse(List<? extends Capacity> result, DecimalFormat format);

    TemplatePermissionsResponse createTemplatePermissionsResponse(List<String> accountNames, long id, boolean isAdmin);

    AsyncJobResponse queryJobResult(QueryAsyncJobResultCmd cmd);

}
