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
package com.cloud.server;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.AffinityGroupProcessor;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseUpdateTemplateOrIsoCmd;
import org.apache.cloudstack.api.command.admin.account.CreateAccountCmd;
import org.apache.cloudstack.api.command.admin.account.DeleteAccountCmd;
import org.apache.cloudstack.api.command.admin.account.DisableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.EnableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.LockAccountCmd;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.alert.GenerateAlertCmd;
import org.apache.cloudstack.api.command.admin.autoscale.CreateCounterCmd;
import org.apache.cloudstack.api.command.admin.autoscale.DeleteCounterCmd;
import org.apache.cloudstack.api.command.admin.cluster.AddClusterCmd;
import org.apache.cloudstack.api.command.admin.cluster.DeleteClusterCmd;
import org.apache.cloudstack.api.command.admin.cluster.ListClustersCmd;
import org.apache.cloudstack.api.command.admin.cluster.UpdateClusterCmd;
import org.apache.cloudstack.api.command.admin.config.ListCfgsByCmd;
import org.apache.cloudstack.api.command.admin.config.ListDeploymentPlannersCmd;
import org.apache.cloudstack.api.command.admin.config.ListHypervisorCapabilitiesCmd;
import org.apache.cloudstack.api.command.admin.config.UpdateCfgCmd;
import org.apache.cloudstack.api.command.admin.config.UpdateHypervisorCapabilitiesCmd;
import org.apache.cloudstack.api.command.admin.domain.CreateDomainCmd;
import org.apache.cloudstack.api.command.admin.domain.DeleteDomainCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainChildrenCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.host.AddHostCmd;
import org.apache.cloudstack.api.command.admin.host.AddSecondaryStorageCmd;
import org.apache.cloudstack.api.command.admin.host.CancelMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.host.DeleteHostCmd;
import org.apache.cloudstack.api.command.admin.host.FindHostsForMigrationCmd;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.command.admin.host.PrepareForMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.host.ReconnectHostCmd;
import org.apache.cloudstack.api.command.admin.host.ReleaseHostReservationCmd;
import org.apache.cloudstack.api.command.admin.host.UpdateHostCmd;
import org.apache.cloudstack.api.command.admin.host.UpdateHostPasswordCmd;
import org.apache.cloudstack.api.command.admin.internallb.ConfigureInternalLoadBalancerElementCmd;
import org.apache.cloudstack.api.command.admin.internallb.CreateInternalLoadBalancerElementCmd;
import org.apache.cloudstack.api.command.admin.internallb.ListInternalLBVMsCmd;
import org.apache.cloudstack.api.command.admin.internallb.ListInternalLoadBalancerElementsCmd;
import org.apache.cloudstack.api.command.admin.internallb.StartInternalLBVMCmd;
import org.apache.cloudstack.api.command.admin.internallb.StopInternalLBVMCmd;
import org.apache.cloudstack.api.command.admin.network.AddNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.AddNetworkServiceProviderCmd;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.CreatePhysicalNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.CreateStorageNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkServiceProviderCmd;
import org.apache.cloudstack.api.command.admin.network.DeletePhysicalNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteStorageNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListDedicatedGuestVlanRangesCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkIsolationMethodsCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkServiceProvidersCmd;
import org.apache.cloudstack.api.command.admin.network.ListPhysicalNetworksCmd;
import org.apache.cloudstack.api.command.admin.network.ListStorageNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListSupportedNetworkServicesCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateNetworkServiceProviderCmd;
import org.apache.cloudstack.api.command.admin.network.UpdatePhysicalNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateStorageNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.offering.CreateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.CreateServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.DeleteDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.DeleteServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.pod.CreatePodCmd;
import org.apache.cloudstack.api.command.admin.pod.DeletePodCmd;
import org.apache.cloudstack.api.command.admin.pod.ListPodsByCmd;
import org.apache.cloudstack.api.command.admin.pod.UpdatePodCmd;
import org.apache.cloudstack.api.command.admin.region.AddRegionCmd;
import org.apache.cloudstack.api.command.admin.region.CreatePortableIpRangeCmd;
import org.apache.cloudstack.api.command.admin.region.DeletePortableIpRangeCmd;
import org.apache.cloudstack.api.command.admin.region.ListPortableIpRangesCmd;
import org.apache.cloudstack.api.command.admin.region.RemoveRegionCmd;
import org.apache.cloudstack.api.command.admin.region.UpdateRegionCmd;
import org.apache.cloudstack.api.command.admin.resource.ArchiveAlertsCmd;
import org.apache.cloudstack.api.command.admin.resource.CleanVMReservationsCmd;
import org.apache.cloudstack.api.command.admin.resource.DeleteAlertsCmd;
import org.apache.cloudstack.api.command.admin.resource.ListAlertsCmd;
import org.apache.cloudstack.api.command.admin.resource.ListCapacityCmd;
import org.apache.cloudstack.api.command.admin.resource.UploadCustomCertificateCmd;
import org.apache.cloudstack.api.command.admin.router.ConfigureOvsElementCmd;
import org.apache.cloudstack.api.command.admin.router.ConfigureVirtualRouterElementCmd;
import org.apache.cloudstack.api.command.admin.router.CreateVirtualRouterElementCmd;
import org.apache.cloudstack.api.command.admin.router.DestroyRouterCmd;
import org.apache.cloudstack.api.command.admin.router.ListOvsElementsCmd;
import org.apache.cloudstack.api.command.admin.router.ListRoutersCmd;
import org.apache.cloudstack.api.command.admin.router.ListVirtualRouterElementsCmd;
import org.apache.cloudstack.api.command.admin.router.RebootRouterCmd;
import org.apache.cloudstack.api.command.admin.router.StartRouterCmd;
import org.apache.cloudstack.api.command.admin.router.StopRouterCmd;
import org.apache.cloudstack.api.command.admin.router.UpgradeRouterCmd;
import org.apache.cloudstack.api.command.admin.router.UpgradeRouterTemplateCmd;
import org.apache.cloudstack.api.command.admin.storage.AddImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.AddS3Cmd;
import org.apache.cloudstack.api.command.admin.storage.CancelPrimaryStorageMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateStoragePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.DeletePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.FindStoragePoolsForMigrationCmd;
import org.apache.cloudstack.api.command.admin.storage.ListImageStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListS3sCmd;
import org.apache.cloudstack.api.command.admin.storage.ListSecondaryStagingStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStorageProvidersCmd;
import org.apache.cloudstack.api.command.admin.storage.PreparePrimaryStorageForMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.storage.PrepareSecondaryStorageForMigrationCmd;
import org.apache.cloudstack.api.command.admin.storage.UpdateStoragePoolCmd;
import org.apache.cloudstack.api.command.admin.swift.AddSwiftCmd;
import org.apache.cloudstack.api.command.admin.swift.ListSwiftsCmd;
import org.apache.cloudstack.api.command.admin.systemvm.DestroySystemVmCmd;
import org.apache.cloudstack.api.command.admin.systemvm.ListSystemVMsCmd;
import org.apache.cloudstack.api.command.admin.systemvm.MigrateSystemVMCmd;
import org.apache.cloudstack.api.command.admin.systemvm.RebootSystemVmCmd;
import org.apache.cloudstack.api.command.admin.systemvm.ScaleSystemVMCmd;
import org.apache.cloudstack.api.command.admin.systemvm.StartSystemVMCmd;
import org.apache.cloudstack.api.command.admin.systemvm.StopSystemVmCmd;
import org.apache.cloudstack.api.command.admin.systemvm.UpgradeSystemVMCmd;
import org.apache.cloudstack.api.command.admin.template.PrepareTemplateCmd;
import org.apache.cloudstack.api.command.admin.usage.AddTrafficMonitorCmd;
import org.apache.cloudstack.api.command.admin.usage.AddTrafficTypeCmd;
import org.apache.cloudstack.api.command.admin.usage.DeleteTrafficMonitorCmd;
import org.apache.cloudstack.api.command.admin.usage.DeleteTrafficTypeCmd;
import org.apache.cloudstack.api.command.admin.usage.GenerateUsageRecordsCmd;
import org.apache.cloudstack.api.command.admin.usage.GetUsageRecordsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficMonitorsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypeImplementorsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypesCmd;
import org.apache.cloudstack.api.command.admin.usage.ListUsageTypesCmd;
import org.apache.cloudstack.api.command.admin.usage.UpdateTrafficTypeCmd;
import org.apache.cloudstack.api.command.admin.user.CreateUserCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.DisableUserCmd;
import org.apache.cloudstack.api.command.admin.user.EnableUserCmd;
import org.apache.cloudstack.api.command.admin.user.GetUserCmd;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.command.admin.user.LockUserCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.command.admin.vlan.CreateVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DedicatePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DeleteVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.ListVlanIpRangesCmd;
import org.apache.cloudstack.api.command.admin.vlan.ReleasePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.admin.vm.ExpungeVMCmd;
import org.apache.cloudstack.api.command.admin.vm.MigrateVMCmd;
import org.apache.cloudstack.api.command.admin.vm.MigrateVirtualMachineWithVolumeCmd;
import org.apache.cloudstack.api.command.admin.vm.RecoverVMCmd;
import org.apache.cloudstack.api.command.admin.vpc.CreatePrivateGatewayCmd;
import org.apache.cloudstack.api.command.admin.vpc.CreateVPCOfferingCmd;
import org.apache.cloudstack.api.command.admin.vpc.DeletePrivateGatewayCmd;
import org.apache.cloudstack.api.command.admin.vpc.DeleteVPCOfferingCmd;
import org.apache.cloudstack.api.command.admin.vpc.UpdateVPCOfferingCmd;
import org.apache.cloudstack.api.command.admin.zone.CreateZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.DeleteZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.MarkDefaultZoneForAccountCmd;
import org.apache.cloudstack.api.command.admin.zone.UpdateZoneCmd;
import org.apache.cloudstack.api.command.user.account.AddAccountToProjectCmd;
import org.apache.cloudstack.api.command.user.account.DeleteAccountFromProjectCmd;
import org.apache.cloudstack.api.command.user.account.ListAccountsCmd;
import org.apache.cloudstack.api.command.user.account.ListProjectAccountsCmd;
import org.apache.cloudstack.api.command.user.address.AssociateIPAddrCmd;
import org.apache.cloudstack.api.command.user.address.DisassociateIPAddrCmd;
import org.apache.cloudstack.api.command.user.address.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.command.user.affinitygroup.CreateAffinityGroupCmd;
import org.apache.cloudstack.api.command.user.affinitygroup.DeleteAffinityGroupCmd;
import org.apache.cloudstack.api.command.user.affinitygroup.ListAffinityGroupTypesCmd;
import org.apache.cloudstack.api.command.user.affinitygroup.ListAffinityGroupsCmd;
import org.apache.cloudstack.api.command.user.affinitygroup.UpdateVMAffinityGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateConditionCmd;
import org.apache.cloudstack.api.command.user.autoscale.DeleteAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.DeleteAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.DeleteAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.autoscale.DeleteConditionCmd;
import org.apache.cloudstack.api.command.user.autoscale.DisableAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.EnableAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScalePoliciesCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScaleVmGroupsCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScaleVmProfilesCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListConditionsCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListCountersCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.config.ListCapabilitiesCmd;
import org.apache.cloudstack.api.command.user.event.ArchiveEventsCmd;
import org.apache.cloudstack.api.command.user.event.DeleteEventsCmd;
import org.apache.cloudstack.api.command.user.event.ListEventTypesCmd;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.api.command.user.firewall.CreateEgressFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.CreateFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.CreatePortForwardingRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.DeleteEgressFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.DeleteFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.DeletePortForwardingRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.ListEgressFirewallRulesCmd;
import org.apache.cloudstack.api.command.user.firewall.ListFirewallRulesCmd;
import org.apache.cloudstack.api.command.user.firewall.ListPortForwardingRulesCmd;
import org.apache.cloudstack.api.command.user.firewall.UpdatePortForwardingRuleCmd;
import org.apache.cloudstack.api.command.user.guest.ListGuestOsCategoriesCmd;
import org.apache.cloudstack.api.command.user.guest.ListGuestOsCmd;
import org.apache.cloudstack.api.command.user.iso.AttachIsoCmd;
import org.apache.cloudstack.api.command.user.iso.CopyIsoCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.DetachIsoCmd;
import org.apache.cloudstack.api.command.user.iso.ExtractIsoCmd;
import org.apache.cloudstack.api.command.user.iso.ListIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.iso.ListIsosCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.iso.UpdateIsoCmd;
import org.apache.cloudstack.api.command.user.iso.UpdateIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.job.ListAsyncJobsCmd;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.AssignCertToLoadBalancerCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.AssignToLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateApplicationLoadBalancerCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBHealthCheckPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBStickinessPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteApplicationLoadBalancerCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteLBHealthCheckPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteLBStickinessPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteSslCertCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListApplicationLoadBalancersCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLBHealthCheckPoliciesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLBStickinessPoliciesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRuleInstancesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRulesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListSslCertsCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.RemoveCertFromLoadBalancerCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.RemoveFromLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UploadSslCertCmd;
import org.apache.cloudstack.api.command.user.nat.CreateIpForwardingRuleCmd;
import org.apache.cloudstack.api.command.user.nat.DeleteIpForwardingRuleCmd;
import org.apache.cloudstack.api.command.user.nat.DisableStaticNatCmd;
import org.apache.cloudstack.api.command.user.nat.EnableStaticNatCmd;
import org.apache.cloudstack.api.command.user.nat.ListIpForwardingRulesCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLListCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.DeleteNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.DeleteNetworkACLListCmd;
import org.apache.cloudstack.api.command.user.network.DeleteNetworkCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLListsCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkOfferingsCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.network.ReplaceNetworkACLListCmd;
import org.apache.cloudstack.api.command.user.network.RestartNetworkCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLItemCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkCmd;
import org.apache.cloudstack.api.command.user.offering.ListDiskOfferingsCmd;
import org.apache.cloudstack.api.command.user.offering.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.command.user.project.ActivateProjectCmd;
import org.apache.cloudstack.api.command.user.project.CreateProjectCmd;
import org.apache.cloudstack.api.command.user.project.DeleteProjectCmd;
import org.apache.cloudstack.api.command.user.project.DeleteProjectInvitationCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectInvitationsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectsCmd;
import org.apache.cloudstack.api.command.user.project.SuspendProjectCmd;
import org.apache.cloudstack.api.command.user.project.UpdateProjectCmd;
import org.apache.cloudstack.api.command.user.project.UpdateProjectInvitationCmd;
import org.apache.cloudstack.api.command.user.region.ListRegionsCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.AssignToGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.CreateGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.DeleteGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.ListGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.RemoveFromGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.UpdateGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.resource.GetCloudIdentifierCmd;
import org.apache.cloudstack.api.command.user.resource.ListHypervisorsCmd;
import org.apache.cloudstack.api.command.user.resource.ListResourceLimitsCmd;
import org.apache.cloudstack.api.command.user.resource.UpdateResourceCountCmd;
import org.apache.cloudstack.api.command.user.resource.UpdateResourceLimitCmd;
import org.apache.cloudstack.api.command.user.securitygroup.AuthorizeSecurityGroupEgressCmd;
import org.apache.cloudstack.api.command.user.securitygroup.AuthorizeSecurityGroupIngressCmd;
import org.apache.cloudstack.api.command.user.securitygroup.CreateSecurityGroupCmd;
import org.apache.cloudstack.api.command.user.securitygroup.DeleteSecurityGroupCmd;
import org.apache.cloudstack.api.command.user.securitygroup.ListSecurityGroupsCmd;
import org.apache.cloudstack.api.command.user.securitygroup.RevokeSecurityGroupEgressCmd;
import org.apache.cloudstack.api.command.user.securitygroup.RevokeSecurityGroupIngressCmd;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotPolicyCmd;
import org.apache.cloudstack.api.command.user.snapshot.DeleteSnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.DeleteSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotsCmd;
import org.apache.cloudstack.api.command.user.snapshot.RevertSnapshotCmd;
import org.apache.cloudstack.api.command.user.ssh.CreateSSHKeyPairCmd;
import org.apache.cloudstack.api.command.user.ssh.DeleteSSHKeyPairCmd;
import org.apache.cloudstack.api.command.user.ssh.ListSSHKeyPairsCmd;
import org.apache.cloudstack.api.command.user.ssh.RegisterSSHKeyPairCmd;
import org.apache.cloudstack.api.command.user.tag.CreateTagsCmd;
import org.apache.cloudstack.api.command.user.tag.DeleteTagsCmd;
import org.apache.cloudstack.api.command.user.tag.ListTagsCmd;
import org.apache.cloudstack.api.command.user.template.CopyTemplateCmd;
import org.apache.cloudstack.api.command.user.template.CreateTemplateCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ExtractTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ListTemplatePermissionsCmd;
import org.apache.cloudstack.api.command.user.template.ListTemplatesCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateTemplatePermissionsCmd;
import org.apache.cloudstack.api.command.user.vm.AddIpToVmNicCmd;
import org.apache.cloudstack.api.command.user.vm.AddNicToVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.GetVMPasswordCmd;
import org.apache.cloudstack.api.command.user.vm.ListNicsCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.RemoveIpFromVmNicCmd;
import org.apache.cloudstack.api.command.user.vm.RemoveNicFromVMCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMPasswordCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMSSHKeyCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateDefaultNicForVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpgradeVMCmd;
import org.apache.cloudstack.api.command.user.vmgroup.CreateVMGroupCmd;
import org.apache.cloudstack.api.command.user.vmgroup.DeleteVMGroupCmd;
import org.apache.cloudstack.api.command.user.vmgroup.ListVMGroupsCmd;
import org.apache.cloudstack.api.command.user.vmgroup.UpdateVMGroupCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.CreateVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.DeleteVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.ListVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.RevertToVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.volume.AddResourceDetailCmd;
import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DeleteVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ExtractVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ListResourceDetailsCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.RemoveResourceDetailCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UpdateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UploadVolumeCmd;
import org.apache.cloudstack.api.command.user.vpc.CreateStaticRouteCmd;
import org.apache.cloudstack.api.command.user.vpc.CreateVPCCmd;
import org.apache.cloudstack.api.command.user.vpc.DeleteStaticRouteCmd;
import org.apache.cloudstack.api.command.user.vpc.DeleteVPCCmd;
import org.apache.cloudstack.api.command.user.vpc.ListPrivateGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpc.ListStaticRoutesCmd;
import org.apache.cloudstack.api.command.user.vpc.ListVPCOfferingsCmd;
import org.apache.cloudstack.api.command.user.vpc.ListVPCsCmd;
import org.apache.cloudstack.api.command.user.vpc.RestartVPCCmd;
import org.apache.cloudstack.api.command.user.vpc.UpdateVPCCmd;
import org.apache.cloudstack.api.command.user.vpn.AddVpnUserCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateRemoteAccessVpnCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteRemoteAccessVpnCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.ListRemoteAccessVpnsCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnConnectionsCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnCustomerGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnUsersCmd;
import org.apache.cloudstack.api.command.user.vpn.RemoveVpnUserCmd;
import org.apache.cloudstack.api.command.user.vpn.ResetVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.UpdateVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesByCmd;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.alert.Alert;
import com.cloud.alert.AlertManager;
import com.cloud.alert.AlertVO;
import com.cloud.alert.dao.AlertDao;
import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.consoleproxy.ConsoleProxyManagementState;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostTagVO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.keystore.KeystoreManager;
import com.cloud.network.IpAddress;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.projects.Project;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SSHKeysHelper;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class ManagementServerImpl extends ManagerBase implements ManagementServer {
    public static final Logger s_logger = Logger.getLogger(ManagementServerImpl.class.getName());

    @Inject
    public AccountManager _accountMgr;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private IPAddressDao _publicIpAddressDao;
    @Inject
    private ConsoleProxyDao _consoleProxyDao;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private SecondaryStorageVmDao _secStorageVmDao;
    @Inject
    public EventDao _eventDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private AccountVlanMapDao _accountVlanMapDao;
    @Inject
    private PodVlanMapDao _podVlanMapDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private HostDetailsDao _detailsDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ConsoleProxyManager _consoleProxyMgr;
    @Inject
    private SecondaryStorageVmManager _secStorageVmMgr;
    @Inject
    private DiskOfferingDao _diskOfferingDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    public AlertDao _alertDao;
    @Inject
    private CapacityDao _capacityDao;
    @Inject
    private GuestOSDao _guestOSDao;
    @Inject
    private GuestOSCategoryDao _guestOSCategoryDao;
    @Inject
    private PrimaryDataStoreDao _poolDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private StorageManager _storageMgr;
    @Inject
    private VolumeOrchestrationService _volumeMgr;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private HostPodDao _hostPodDao;
    @Inject
    private VMInstanceDao _vmInstanceDao;
    @Inject
    private VolumeDao _volumeDao;
    private int _purgeDelay;
    private int _alertPurgeDelay;
    @Inject
    private InstanceGroupDao _vmGroupDao;
    @Inject
    private SSHKeyPairDao _sshKeyPairDao;
    @Inject
    private LoadBalancerDao _loadbalancerDao;
    @Inject
    private HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    private List<HostAllocator> _hostAllocators;
    private List<StoragePoolAllocator> _storagePoolAllocators;
    @Inject
    private ResourceTagDao _resourceTagDao;
    @Inject
    private ImageStoreDao _imgStoreDao;

    @Inject
    ProjectManager _projectMgr;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    SnapshotManager _snapshotMgr;
    @Inject
    HighAvailabilityManager _haMgr;
    @Inject
    TemplateManager templateMgr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    HostTagsDao _hostTagsDao;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    UserVmManager _userVmMgr;
    @Inject
    VolumeDataFactory _volFactory;
    @Inject
    AccountService _accountService;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    ServiceOfferingDao _offeringDao;

    @Inject
    DeploymentPlanningManager _dpMgr;

    LockMasterListener _lockMasterListener;

    private final ScheduledExecutorService _eventExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("EventChecker"));
    private final ScheduledExecutorService _alertExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AlertChecker"));
    @Inject
    private KeystoreManager _ksMgr;

    private Map<String, String> _configs;

    private Map<String, Boolean> _availableIdsMap;

    private List<UserAuthenticator> _userAuthenticators;
    private List<UserAuthenticator> _userPasswordEncoders;
    protected boolean _executeInSequence;

    protected List<DeploymentPlanner> _planners;

    public List<DeploymentPlanner> getPlanners() {
        return _planners;
    }

    public void setPlanners(List<DeploymentPlanner> _planners) {
        this._planners = _planners;
    }

    @Inject
    ClusterManager _clusterMgr;
    private String _hashKey = null;
    private String _encryptionKey = null;
    private String _encryptionIV = null;

    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;

    protected List<AffinityGroupProcessor> _affinityProcessors;

    public List<AffinityGroupProcessor> getAffinityGroupProcessors() {
        return _affinityProcessors;
    }

    public void setAffinityGroupProcessors(List<AffinityGroupProcessor> affinityProcessors) {
        _affinityProcessors = affinityProcessors;
    }

    public ManagementServerImpl() {
        setRunLevel(ComponentLifecycle.RUN_LEVEL_APPLICATION_MAINLOOP);
    }

    public List<UserAuthenticator> getUserAuthenticators() {
        return _userAuthenticators;
    }

    public void setUserAuthenticators(List<UserAuthenticator> authenticators) {
        _userAuthenticators = authenticators;
    }

    public List<UserAuthenticator> getUserPasswordEncoders() {
        return _userPasswordEncoders;
    }

    public void setUserPasswordEncoders(List<UserAuthenticator> encoders) {
        _userPasswordEncoders = encoders;
    }

    public List<HostAllocator> getHostAllocators() {
        return _hostAllocators;
    }

    public void setHostAllocators(List<HostAllocator> _hostAllocators) {
        this._hostAllocators = _hostAllocators;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        _configs = _configDao.getConfiguration();

        String value = _configs.get("event.purge.interval");
        int cleanup = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 day.

        _purgeDelay = NumbersUtil.parseInt(_configs.get("event.purge.delay"), 0);
        if (_purgeDelay != 0) {
            _eventExecutor.scheduleAtFixedRate(new EventPurgeTask(), cleanup, cleanup, TimeUnit.SECONDS);
        }

        //Alerts purge configurations
        int alertPurgeInterval = NumbersUtil.parseInt(_configDao.getValue(Config.AlertPurgeInterval.key()), 60 * 60 * 24); // 1 day.
        _alertPurgeDelay = NumbersUtil.parseInt(_configDao.getValue(Config.AlertPurgeDelay.key()), 0);
        if (_alertPurgeDelay != 0) {
            _alertExecutor.scheduleAtFixedRate(new AlertPurgeTask(), alertPurgeInterval, alertPurgeInterval, TimeUnit.SECONDS);
        }

        String[] availableIds = TimeZone.getAvailableIDs();
        _availableIdsMap = new HashMap<String, Boolean>(availableIds.length);
        for (String id : availableIds) {
            _availableIdsMap.put(id, true);
        }

        return true;
    }

    @Override
    public boolean start() {
        s_logger.info("Startup CloudStack management server...");

        if (_lockMasterListener == null) {
            _lockMasterListener = new LockMasterListener(ManagementServerNode.getManagementServerId());
        }

        _clusterMgr.registerListener(_lockMasterListener);

        enableAdminUser("password");
        return true;
    }

    protected Map<String, String> getConfigs() {
        return _configs;
    }

    @Override
    public String generateRandomPassword() {
        return PasswordGenerator.generateRandomPassword(6);
    }

    @Override
    public HostVO getHostBy(long hostId) {
        return _hostDao.findById(hostId);
    }

    @Override
    public long getId() {
        return MacAddress.getMacAddress().toLong();
    }

    protected void checkPortParameters(String publicPort, String privatePort, String privateIp, String proto) {

        if (!NetUtils.isValidPort(publicPort)) {
            throw new InvalidParameterValueException("publicPort is an invalid value");
        }
        if (!NetUtils.isValidPort(privatePort)) {
            throw new InvalidParameterValueException("privatePort is an invalid value");
        }

        // s_logger.debug("Checking if " + privateIp +
        // " is a valid private IP address. Guest IP address is: " +
        // _configs.get("guest.ip.network"));
        //
        // if (!NetUtils.isValidPrivateIp(privateIp,
        // _configs.get("guest.ip.network"))) {
        // throw new
        // InvalidParameterValueException("Invalid private ip address");
        // }
        if (!NetUtils.isValidProto(proto)) {
            throw new InvalidParameterValueException("Invalid protocol");
        }
    }

    @Override
    public boolean archiveEvents(ArchiveEventsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> ids = cmd.getIds();
        boolean result = true;
        List<Long> permittedAccountIds = new ArrayList<Long>();

        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL || caller.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            permittedAccountIds.add(caller.getId());
        } else {
            DomainVO domain = _domainDao.findById(caller.getDomainId());
            List<Long> permittedDomainIds = _domainDao.getDomainChildrenIds(domain.getPath());
            permittedAccountIds = _accountDao.getAccountIdsForDomains(permittedDomainIds);
        }

        List<EventVO> events = _eventDao.listToArchiveOrDeleteEvents(ids, cmd.getType(), cmd.getStartDate(), cmd.getEndDate(), permittedAccountIds);
        ControlledEntity[] sameOwnerEvents = events.toArray(new ControlledEntity[events.size()]);
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, sameOwnerEvents);

        if (ids != null && events.size() < ids.size()) {
            result = false;
            return result;
        }
        _eventDao.archiveEvents(events);
        return result;
    }

    @Override
    public boolean deleteEvents(DeleteEventsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> ids = cmd.getIds();
        boolean result = true;
        List<Long> permittedAccountIds = new ArrayList<Long>();

        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL || caller.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            permittedAccountIds.add(caller.getId());
        } else {
            DomainVO domain = _domainDao.findById(caller.getDomainId());
            List<Long> permittedDomainIds = _domainDao.getDomainChildrenIds(domain.getPath());
            permittedAccountIds = _accountDao.getAccountIdsForDomains(permittedDomainIds);
        }

        List<EventVO> events = _eventDao.listToArchiveOrDeleteEvents(ids, cmd.getType(), cmd.getStartDate(), cmd.getEndDate(), permittedAccountIds);
        ControlledEntity[] sameOwnerEvents = events.toArray(new ControlledEntity[events.size()]);
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, sameOwnerEvents);

        if (ids != null && events.size() < ids.size()) {
            result = false;
            return result;
        }
        for (EventVO event : events) {
            _eventDao.remove(event.getId());
        }
        return result;
    }

    private Date massageDate(Date date, int hourOfDay, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        return cal.getTime();
    }

    @Override
    public List<? extends Cluster> searchForClusters(long zoneId, Long startIndex, Long pageSizeVal, String hypervisorType) {
        Filter searchFilter = new Filter(ClusterVO.class, "id", true, startIndex, pageSizeVal);
        SearchCriteria<ClusterVO> sc = _clusterDao.createSearchCriteria();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);

        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisorType);

        return _clusterDao.search(sc, searchFilter);
    }

    @Override
    public Pair<List<? extends Cluster>, Integer> searchForClusters(ListClustersCmd cmd) {
        Object id = cmd.getId();
        Object name = cmd.getClusterName();
        Object podId = cmd.getPodId();
        Long zoneId = cmd.getZoneId();
        Object hypervisorType = cmd.getHypervisorType();
        Object clusterType = cmd.getClusterType();
        Object allocationState = cmd.getAllocationState();
        String keyword = cmd.getKeyword();
        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);

        Filter searchFilter = new Filter(ClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<ClusterVO> sb = _clusterDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("clusterType", sb.entity().getClusterType(), SearchCriteria.Op.EQ);
        sb.and("allocationState", sb.entity().getAllocationState(), SearchCriteria.Op.EQ);

        SearchCriteria<ClusterVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (podId != null) {
            sc.setParameters("podId", podId);
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }

        if (hypervisorType != null) {
            sc.setParameters("hypervisorType", hypervisorType);
        }

        if (clusterType != null) {
            sc.setParameters("clusterType", clusterType);
        }

        if (allocationState != null) {
            sc.setParameters("allocationState", allocationState);
        }

        if (keyword != null) {
            SearchCriteria<ClusterVO> ssc = _clusterDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("hypervisorType", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        Pair<List<ClusterVO>, Integer> result = _clusterDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Cluster>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Host>, Integer> searchForServers(ListHostsCmd cmd) {

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Object name = cmd.getHostName();
        Object type = cmd.getType();
        Object state = cmd.getState();
        Object pod = cmd.getPodId();
        Object cluster = cmd.getClusterId();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Object resourceState = cmd.getResourceState();
        Object haHosts = cmd.getHaHost();

        Pair<List<HostVO>, Integer> result =
            searchForServers(cmd.getStartIndex(), cmd.getPageSizeVal(), name, type, state, zoneId, pod, cluster, id, keyword, resourceState, haHosts, null, null);
        return new Pair<List<? extends Host>, Integer>(result.first(), result.second());
    }

    @Override
    public Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> listHostsForMigrationOfVM(Long vmId, Long startIndex, Long pageSize) {
        // access check - only root admin can migrate VM
        Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException("No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find the VM with given id");
            throw ex;
        }

        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not running, cannot migrate the vm" + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException("VM is not Running, cannot " + "migrate the vm with specified id");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        if (!vm.getHypervisorType().equals(HypervisorType.XenServer) && !vm.getHypervisorType().equals(HypervisorType.VMware)
                && !vm.getHypervisorType().equals(HypervisorType.KVM) && !vm.getHypervisorType().equals(HypervisorType.Ovm)
                && !vm.getHypervisorType().equals(HypervisorType.Hyperv)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm + " is not XenServer/VMware/KVM/OVM/Hyperv, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for VM migration, we support " +
                    "XenServer/VMware/KVM/Ovm/Hyperv only");
        }

        long srcHostId = vm.getHostId();
        Host srcHost = _hostDao.findById(srcHostId);
        if (srcHost == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find the host with id: " + srcHostId + " of this VM:" + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find the host (with specified id) of VM with specified id");
            ex.addProxyObject(String.valueOf(srcHostId), "hostId");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        // Check if the vm can be migrated with storage.
        boolean canMigrateWithStorage = false;

        if (vm.getType() == VirtualMachine.Type.User) {
            HypervisorCapabilitiesVO capabilities =
                _hypervisorCapabilitiesDao.findByHypervisorTypeAndVersion(srcHost.getHypervisorType(), srcHost.getHypervisorVersion());
            if (capabilities != null) {
                canMigrateWithStorage = capabilities.isStorageMotionSupported();
            }
        }

        // Check if the vm is using any disks on local storage.
        VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vm);
        List<VolumeVO> volumes = _volumeDao.findCreatedByInstance(vmProfile.getId());
        boolean usesLocal = false;
        for (VolumeVO volume : volumes) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
            DiskProfile diskProfile = new DiskProfile(volume, diskOffering, vmProfile.getHypervisorType());
            if (diskProfile.useLocalStorage()) {
                usesLocal = true;
                break;
            }
        }

        if (!canMigrateWithStorage && usesLocal) {
            throw new InvalidParameterValueException("Unsupported operation, VM uses Local storage, cannot migrate");
        }

        Type hostType = srcHost.getType();
        Pair<List<HostVO>, Integer> allHostsPair = null;
        List<HostVO> allHosts = null;
        Map<Host, Boolean> requiresStorageMotion = new HashMap<Host, Boolean>();
        DataCenterDeployment plan = null;
        boolean zoneWideStoragePool = false;
        if (canMigrateWithStorage) {
            allHostsPair =
                searchForServers(startIndex, pageSize, null, hostType, null, srcHost.getDataCenterId(), null, null, null, null, null, null, srcHost.getHypervisorType(),
                    srcHost.getHypervisorVersion());
            allHosts = allHostsPair.first();
            allHosts.remove(srcHost);

            // Check if the host has storage pools for all the volumes of the vm to be migrated.
            for (Iterator<HostVO> iterator = allHosts.iterator(); iterator.hasNext();) {
                Host host = iterator.next();
                Map<Volume, List<StoragePool>> volumePools = findSuitablePoolsForVolumes(vmProfile, host);
                if (volumePools.isEmpty()) {
                    iterator.remove();
                } else {
                    if (srcHost.getHypervisorType() == HypervisorType.VMware || srcHost.getHypervisorType() == HypervisorType.KVM) {
                        zoneWideStoragePool = checkForZoneWideStoragePool(volumePools);
                    }
                    if ((!host.getClusterId().equals(srcHost.getClusterId()) || usesLocal) && !zoneWideStoragePool) {
                        requiresStorageMotion.put(host, true);
                    }
                }
            }

            plan = new DataCenterDeployment(srcHost.getDataCenterId(), null, null, null, null, null);
        } else {
            Long cluster = srcHost.getClusterId();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Searching for all hosts in cluster " + cluster + " for migrating VM " + vm);
            }
            allHostsPair = searchForServers(startIndex, pageSize, null, hostType, null, null, null, cluster, null, null, null, null, null, null);
            // Filter out the current host.
            allHosts = allHostsPair.first();
            allHosts.remove(srcHost);
            plan = new DataCenterDeployment(srcHost.getDataCenterId(), srcHost.getPodId(), srcHost.getClusterId(), null, null, null);
        }

        Pair<List<? extends Host>, Integer> otherHosts = new Pair<List<? extends Host>, Integer>(allHosts, new Integer(allHosts.size()));
        List<Host> suitableHosts = new ArrayList<Host>();
        ExcludeList excludes = new ExcludeList();
        excludes.addHost(srcHostId);

        // call affinitygroup chain
        long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        if (vmGroupCount > 0) {
            for (AffinityGroupProcessor processor : _affinityProcessors) {
                processor.process(vmProfile, plan, excludes);
            }
        }

        for (HostAllocator allocator : _hostAllocators) {
            if (canMigrateWithStorage) {
                suitableHosts = allocator.allocateTo(vmProfile, plan, Host.Type.Routing, excludes, allHosts, HostAllocator.RETURN_UPTO_ALL, false);
            } else {
                suitableHosts = allocator.allocateTo(vmProfile, plan, Host.Type.Routing, excludes, HostAllocator.RETURN_UPTO_ALL, false);
            }

            if (suitableHosts != null && !suitableHosts.isEmpty()) {
                break;
            }
        }

        if (s_logger.isDebugEnabled()) {
            if (suitableHosts.isEmpty()) {
                s_logger.debug("No suitable hosts found");
            } else {
                s_logger.debug("Hosts having capacity and suitable for migration: " + suitableHosts);
            }
        }

        return new Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>>(otherHosts, suitableHosts, requiresStorageMotion);
    }

    private boolean checkForZoneWideStoragePool(Map<Volume, List<StoragePool>> volumePools) {
        boolean zoneWideStoragePool = false;
        Collection<List<StoragePool>> pools = volumePools.values();
        List<StoragePool> aggregatePoolList = new ArrayList<StoragePool>();
        for (Iterator<List<StoragePool>> volumePoolsIter = pools.iterator(); volumePoolsIter.hasNext();) {
            aggregatePoolList.addAll(volumePoolsIter.next());
        }
        for (StoragePool pool : aggregatePoolList) {
            if (null == pool.getClusterId()) {
                zoneWideStoragePool = true;
                break;
            }
        }
        return zoneWideStoragePool;
    }

    private Map<Volume, List<StoragePool>> findSuitablePoolsForVolumes(VirtualMachineProfile vmProfile, Host host) {
        List<VolumeVO> volumes = _volumeDao.findCreatedByInstance(vmProfile.getId());
        Map<Volume, List<StoragePool>> suitableVolumeStoragePools = new HashMap<Volume, List<StoragePool>>();

        // For each volume find list of suitable storage pools by calling the allocators
        for (VolumeVO volume : volumes) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
            DiskProfile diskProfile = new DiskProfile(volume, diskOffering, vmProfile.getHypervisorType());
            DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), host.getId(), null, null);
            ExcludeList avoid = new ExcludeList();

            boolean foundPools = false;
            for (StoragePoolAllocator allocator : _storagePoolAllocators) {
                List<StoragePool> poolList = allocator.allocateToPool(diskProfile, vmProfile, plan, avoid, StoragePoolAllocator.RETURN_UPTO_ALL);
                if (poolList != null && !poolList.isEmpty()) {
                    suitableVolumeStoragePools.put(volume, poolList);
                    foundPools = true;
                    break;
                }
            }

            if (!foundPools) {
                suitableVolumeStoragePools.clear();
                break;
            }
        }

        return suitableVolumeStoragePools;
    }

    @Override
    public Pair<List<? extends StoragePool>, List<? extends StoragePool>> listStoragePoolsForMigrationOfVolume(Long volumeId) {
        // Access check - only root administrator can migrate volumes.
        Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the volume");
            }
            throw new PermissionDeniedException("No permission to migrate volume, only root admin can migrate a volume");
        }

        VolumeVO volume = _volumeDao.findById(volumeId);
        if (volume == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find volume with" + " specified id.");
            ex.addProxyObject(volumeId.toString(), "volumeId");
            throw ex;
        }

        // Volume must be attached to an instance for live migration.
        List<StoragePool> allPools = new ArrayList<StoragePool>();
        List<StoragePool> suitablePools = new ArrayList<StoragePool>();

        // Volume must be in Ready state to be migrated.
        if (!Volume.State.Ready.equals(volume.getState())) {
            s_logger.info("Volume " + volume + " must be in ready state for migration.");
            return new Pair<List<? extends StoragePool>, List<? extends StoragePool>>(allPools, suitablePools);
        }

        if (!_volumeMgr.volumeOnSharedStoragePool(volume)) {
            s_logger.info("Volume " + volume + " is on local storage. It cannot be migrated to another pool.");
            return new Pair<List<? extends StoragePool>, List<? extends StoragePool>>(allPools, suitablePools);
        }

        Long instanceId = volume.getInstanceId();
        VMInstanceVO vm = null;
        if (instanceId != null) {
            vm = _vmInstanceDao.findById(instanceId);
        }

        if (vm == null) {
            s_logger.info("Volume " + volume + " isn't attached to any vm. Looking for storage pools in the " + "zone to which this volumes can be migrated.");
        } else if (vm.getState() != State.Running) {
            s_logger.info("Volume " + volume + " isn't attached to any running vm. Looking for storage pools in the " + "cluster to which this volumes can be migrated.");
        } else {
            s_logger.info("Volume " + volume + " is attached to any running vm. Looking for storage pools in the " + "cluster to which this volumes can be migrated.");
            boolean storageMotionSupported = false;
            // Check if the underlying hypervisor supports storage motion.
            Long hostId = vm.getHostId();
            if (hostId != null) {
                HostVO host = _hostDao.findById(hostId);
                HypervisorCapabilitiesVO capabilities = null;
                if (host != null) {
                    capabilities = _hypervisorCapabilitiesDao.findByHypervisorTypeAndVersion(host.getHypervisorType(), host.getHypervisorVersion());
                } else {
                    s_logger.error("Details of the host on which the vm " + vm + ", to which volume " + volume + " is " + "attached, couldn't be retrieved.");
                }

                if (capabilities != null) {
                    storageMotionSupported = capabilities.isStorageMotionSupported();
                } else {
                    s_logger.error("Capabilities for host " + host + " couldn't be retrieved.");
                }
            }

            if (!storageMotionSupported) {
                s_logger.info("Volume " + volume + " is attached to a running vm and the hypervisor doesn't support" + " storage motion.");
                return new Pair<List<? extends StoragePool>, List<? extends StoragePool>>(allPools, suitablePools);
            }
        }

        // Source pool of the volume.
        StoragePoolVO srcVolumePool = _poolDao.findById(volume.getPoolId());
        // Get all the pools available. Only shared pools are considered because only a volume on a shared pools
        // can be live migrated while the virtual machine stays on the same host.
        List<StoragePoolVO> storagePools = null;
        if (srcVolumePool.getClusterId() == null) {
            storagePools = _poolDao.findZoneWideStoragePoolsByTags(volume.getDataCenterId(), null);
        } else {
            storagePools = _poolDao.findPoolsByTags(volume.getDataCenterId(), srcVolumePool.getPodId(), srcVolumePool.getClusterId(), null);
        }

        storagePools.remove(srcVolumePool);
        for (StoragePoolVO pool : storagePools) {
            if (pool.isShared()) {
                allPools.add((StoragePool)dataStoreMgr.getPrimaryDataStore(pool.getId()));
            }
        }

        // Get all the suitable pools.
        // Exclude the current pool from the list of pools to which the volume can be migrated.
        ExcludeList avoid = new ExcludeList();
        avoid.addPool(srcVolumePool.getId());

        // Volume stays in the same cluster after migration.
        DataCenterDeployment plan = new DataCenterDeployment(volume.getDataCenterId(), srcVolumePool.getPodId(), srcVolumePool.getClusterId(), null, null, null);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);

        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        DiskProfile diskProfile = new DiskProfile(volume, diskOffering, profile.getHypervisorType());

        // Call the storage pool allocator to find the list of storage pools.
        for (StoragePoolAllocator allocator : _storagePoolAllocators) {
            List<StoragePool> pools = allocator.allocateToPool(diskProfile, profile, plan, avoid, StoragePoolAllocator.RETURN_UPTO_ALL);
            if (pools != null && !pools.isEmpty()) {
                suitablePools.addAll(pools);
                break;
            }
        }

        return new Pair<List<? extends StoragePool>, List<? extends StoragePool>>(allPools, suitablePools);
    }

    private Pair<List<HostVO>, Integer> searchForServers(Long startIndex, Long pageSize, Object name, Object type, Object state, Object zone, Object pod, Object cluster,
        Object id, Object keyword, Object resourceState, Object haHosts, Object hypervisorType, Object hypervisorVersion) {
        Filter searchFilter = new Filter(HostVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<HostVO> sb = _hostDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.LIKE);
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("resourceState", sb.entity().getResourceState(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("hypervisorVersion", sb.entity().getHypervisorVersion(), SearchCriteria.Op.EQ);

        String haTag = _haMgr.getHaTag();
        SearchBuilder<HostTagVO> hostTagSearch = null;
        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            hostTagSearch = _hostTagsDao.createSearchBuilder();
            if ((Boolean)haHosts) {
                hostTagSearch.and().op("tag", hostTagSearch.entity().getTag(), SearchCriteria.Op.EQ);
            } else {
                hostTagSearch.and().op("tag", hostTagSearch.entity().getTag(), SearchCriteria.Op.NEQ);
                hostTagSearch.or("tagNull", hostTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
            }

            hostTagSearch.cp();
            sb.join("hostTagSearch", hostTagSearch, sb.entity().getId(), hostTagSearch.entity().getHostId(), JoinBuilder.JoinType.LEFTOUTER);
        }

        SearchCriteria<HostVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<HostVO> ssc = _hostDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("status", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }
        if (type != null) {
            sc.setParameters("type", "%" + type);
        }
        if (state != null) {
            sc.setParameters("status", state);
        }
        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
        }
        if (cluster != null) {
            sc.setParameters("clusterId", cluster);
        }
        if (hypervisorType != null) {
            sc.setParameters("hypervisorType", hypervisorType);
        }
        if (hypervisorVersion != null) {
            sc.setParameters("hypervisorVersion", hypervisorVersion);
        }

        if (resourceState != null) {
            sc.setParameters("resourceState", resourceState);
        }

        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            sc.setJoinParameters("hostTagSearch", "tag", haTag);
        }

        return _hostDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public Pair<List<? extends Pod>, Integer> searchForPods(ListPodsByCmd cmd) {
        String podName = cmd.getPodName();
        Long id = cmd.getId();
        Long zoneId = cmd.getZoneId();
        Object keyword = cmd.getKeyword();
        Object allocationState = cmd.getAllocationState();
        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);

        Filter searchFilter = new Filter(HostPodVO.class, "dataCenterId", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<HostPodVO> sb = _hostPodDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("allocationState", sb.entity().getAllocationState(), SearchCriteria.Op.EQ);

        SearchCriteria<HostPodVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<HostPodVO> ssc = _hostPodDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (podName != null) {
            sc.setParameters("name", "%" + podName + "%");
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }

        if (allocationState != null) {
            sc.setParameters("allocationState", allocationState);
        }

        Pair<List<HostPodVO>, Integer> result = _hostPodDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Pod>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Vlan>, Integer> searchForVlans(ListVlanIpRangesCmd cmd) {
        // If an account name and domain ID are specified, look up the account
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        Long networkId = cmd.getNetworkId();
        Boolean forVirtual = cmd.getForVirtualNetwork();
        String vlanType = null;
        Long projectId = cmd.getProjectId();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();

        if (accountName != null && domainId != null) {
            if (projectId != null) {
                throw new InvalidParameterValueException("Account and projectId can't be specified together");
            }
            Account account = _accountDao.findActiveAccount(accountName, domainId);
            if (account == null) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find account " + accountName + " in specified domain");
                // Since we don't have a DomainVO object here, we directly set
                // tablename to "domain".
                DomainVO domain = ApiDBUtils.findDomainById(domainId);
                String domainUuid = domainId.toString();
                if (domain != null) {
                    domainUuid = domain.getUuid();
                }
                ex.addProxyObject(domainUuid, "domainId");
                throw ex;
            } else {
                accountId = account.getId();
            }
        }

        if (forVirtual != null) {
            if (forVirtual) {
                vlanType = VlanType.VirtualNetwork.toString();
            } else {
                vlanType = VlanType.DirectAttached.toString();
            }
        }

        // set project information
        if (projectId != null) {
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project by id " + projectId);
                ex.addProxyObject(projectId.toString(), "projectId");
                throw ex;
            }
            accountId = project.getProjectAccountId();
        }

        Filter searchFilter = new Filter(VlanVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object vlan = cmd.getVlan();
        Object dataCenterId = cmd.getZoneId();
        Object podId = cmd.getPodId();
        Object keyword = cmd.getKeyword();

        SearchBuilder<VlanVO> sb = _vlanDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vlan", sb.entity().getVlanTag(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("vlan", sb.entity().getVlanTag(), SearchCriteria.Op.EQ);
        sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        sb.and("vlanType", sb.entity().getVlanType(), SearchCriteria.Op.EQ);
        sb.and("physicalNetworkId", sb.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);

        if (accountId != null) {
            SearchBuilder<AccountVlanMapVO> accountVlanMapSearch = _accountVlanMapDao.createSearchBuilder();
            accountVlanMapSearch.and("accountId", accountVlanMapSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
            sb.join("accountVlanMapSearch", accountVlanMapSearch, sb.entity().getId(), accountVlanMapSearch.entity().getVlanDbId(), JoinBuilder.JoinType.INNER);
        }

        if (podId != null) {
            SearchBuilder<PodVlanMapVO> podVlanMapSearch = _podVlanMapDao.createSearchBuilder();
            podVlanMapSearch.and("podId", podVlanMapSearch.entity().getPodId(), SearchCriteria.Op.EQ);
            sb.join("podVlanMapSearch", podVlanMapSearch, sb.entity().getId(), podVlanMapSearch.entity().getVlanDbId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<VlanVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<VlanVO> ssc = _vlanDao.createSearchCriteria();
            ssc.addOr("vlanId", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("ipRange", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("vlanId", SearchCriteria.Op.SC, ssc);
        } else {
            if (id != null) {
                sc.setParameters("id", id);
            }

            if (vlan != null) {
                sc.setParameters("vlan", vlan);
            }

            if (dataCenterId != null) {
                sc.setParameters("dataCenterId", dataCenterId);
            }

            if (networkId != null) {
                sc.setParameters("networkId", networkId);
            }

            if (accountId != null) {
                sc.setJoinParameters("accountVlanMapSearch", "accountId", accountId);
            }

            if (podId != null) {
                sc.setJoinParameters("podVlanMapSearch", "podId", podId);
            }
            if (vlanType != null) {
                sc.setParameters("vlanType", vlanType);
            }

            if (physicalNetworkId != null) {
                sc.setParameters("physicalNetworkId", physicalNetworkId);
            }
        }

        Pair<List<VlanVO>, Integer> result = _vlanDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Vlan>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Configuration>, Integer> searchForConfigurations(ListCfgsByCmd cmd) {
        Filter searchFilter = new Filter(ConfigurationVO.class, "name", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<ConfigurationVO> sc = _configDao.createSearchCriteria();

        Object name = cmd.getConfigName();
        Object category = cmd.getCategory();
        Object keyword = cmd.getKeyword();
        Long zoneId = cmd.getZoneId();
        Long clusterId = cmd.getClusterId();
        Long storagepoolId = cmd.getStoragepoolId();
        Long accountId = cmd.getAccountId();
        String scope = null;
        Long id = null;
        int paramCountCheck = 0;

        if (zoneId != null) {
            scope = ConfigKey.Scope.Zone.toString();
            id = zoneId;
            paramCountCheck++;
        }
        if (clusterId != null) {
            scope = ConfigKey.Scope.Cluster.toString();
            id = clusterId;
            paramCountCheck++;
        }
        if (accountId != null) {
            scope = ConfigKey.Scope.Account.toString();
            id = accountId;
            paramCountCheck++;
        }
        if (storagepoolId != null) {
            scope = ConfigKey.Scope.StoragePool.toString();
            id = storagepoolId;
            paramCountCheck++;
        }

        if (paramCountCheck > 1) {
            throw new InvalidParameterValueException("cannot handle multiple IDs, provide only one ID corresponding to the scope");
        }

        if (scope != null && !scope.isEmpty()) {
            // getting the list of parameters at requested scope
            if (id == null) {
                throw new InvalidParameterValueException("Invalid id null, id is needed corresponding to the scope");
            }
            List<ConfigurationVO> configList = _configServer.getConfigListByScope(scope, id);
            return new Pair<List<? extends Configuration>, Integer>(configList, configList.size());
        }

        if (keyword != null) {
            SearchCriteria<ConfigurationVO> ssc = _configDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instance", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("component", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("category", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("value", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (category != null) {
            sc.addAnd("category", SearchCriteria.Op.EQ, category);
        }

        // hidden configurations are not displayed using the search API
        sc.addAnd("category", SearchCriteria.Op.NEQ, "Hidden");

        Pair<List<ConfigurationVO>, Integer> result = _configDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Configuration>, Integer>(result.first(), result.second());
    }

    /* TODO: this method should go away. Keep here just in case that our latest refactoring using template_store_ref missed anything
     * in handling Swift or S3.
    private Set<Pair<Long, Long>> listTemplates(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso,
            Boolean bootable, Long pageSize, Long startIndex, Long zoneId, HypervisorType hyperType, boolean showDomr, boolean onlyReady,
            List<Account> permittedAccounts, Account caller, ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags, String zoneType) {

        VMTemplateVO template = null;
        if (templateId != null) {
            template = _templateDao.findById(templateId);
            if (template == null) {
                throw new InvalidParameterValueException("Please specify a valid template ID.");
            }// If ISO requested then it should be ISO.
            if (isIso && template.getFormat() != ImageFormat.ISO) {
                s_logger.error("Template Id " + templateId + " is not an ISO");
                InvalidParameterValueException ex = new InvalidParameterValueException("Specified Template Id is not an ISO");
                ex.addProxyObject(template.getUuid(), "templateId");
                throw ex;
            }// If ISO not requested then it shouldn't be an ISO.
            if (!isIso && template.getFormat() == ImageFormat.ISO) {
                s_logger.error("Incorrect format of the template id " + templateId);
                InvalidParameterValueException ex = new InvalidParameterValueException("Incorrect format " + template.getFormat()
                        + " of the specified template id");
                ex.addProxyObject(template.getUuid(), "templateId");
                throw ex;
            }
        }

        DomainVO domain = null;
        if (!permittedAccounts.isEmpty()) {
            domain = _domainDao.findById(permittedAccounts.get(0).getDomainId());
        } else {
            domain = _domainDao.findById(DomainVO.ROOT_DOMAIN);
        }

        List<HypervisorType> hypers = null;
        if (!isIso) {
            hypers = _resourceMgr.listAvailHypervisorInZone(null, null);
        }
        Set<Pair<Long, Long>> templateZonePairSet = new HashSet<Pair<Long, Long>>();
        if (_swiftMgr.isSwiftEnabled()) {
            if (template == null) {
                templateZonePairSet = _templateDao.searchSwiftTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize,
                        startIndex, zoneId, hyperType, onlyReady, showDomr, permittedAccounts, caller, tags);
                Set<Pair<Long, Long>> templateZonePairSet2 = new HashSet<Pair<Long, Long>>();
                templateZonePairSet2 = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize,
                        startIndex, zoneId, hyperType, onlyReady, showDomr, permittedAccounts, caller, listProjectResourcesCriteria, tags, zoneType);

                for (Pair<Long, Long> tmpltPair : templateZonePairSet2) {
                    if (!templateZonePairSet.contains(new Pair<Long, Long>(tmpltPair.first(), -1L))) {
                        templateZonePairSet.add(tmpltPair);
                    }
                }

            } else {
                // if template is not public, perform permission check here
                if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    Account owner = _accountMgr.getAccount(template.getAccountId());
                    _accountMgr.checkAccess(caller, null, true, owner);
                }
                templateZonePairSet.add(new Pair<Long, Long>(template.getId(), zoneId));
            }
        } else if (_s3Mgr.isS3Enabled()) {
            if (template == null) {
                templateZonePairSet = _templateDao.searchSwiftTemplates(name, keyword, templateFilter, isIso,
                        hypers, bootable, domain, pageSize, startIndex, zoneId, hyperType, onlyReady, showDomr,
                        permittedAccounts, caller, tags);
                Set<Pair<Long, Long>> templateZonePairSet2 = new HashSet<Pair<Long, Long>>();
                templateZonePairSet2 = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, hypers,
                        bootable, domain, pageSize, startIndex, zoneId, hyperType, onlyReady, showDomr,
                        permittedAccounts, caller, listProjectResourcesCriteria, tags, zoneType);

                for (Pair<Long, Long> tmpltPair : templateZonePairSet2) {
                    if (!templateZonePairSet.contains(new Pair<Long, Long>(tmpltPair.first(), -1L))) {
                        templateZonePairSet.add(tmpltPair);
                    }
                }
            } else {
                // if template is not public, perform permission check here
                if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    Account owner = _accountMgr.getAccount(template.getAccountId());
                    _accountMgr.checkAccess(caller, null, true, owner);
                }
                templateZonePairSet.add(new Pair<Long, Long>(template.getId(), zoneId));
            }
        } else {
            if (template == null) {
                templateZonePairSet = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize,
                        startIndex, zoneId, hyperType, onlyReady, showDomr, permittedAccounts, caller, listProjectResourcesCriteria, tags, zoneType);
            } else {
                // if template is not public, perform permission check here
                if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    Account owner = _accountMgr.getAccount(template.getAccountId());
                    _accountMgr.checkAccess(caller, null, true, owner);
                }
                templateZonePairSet.add(new Pair<Long, Long>(template.getId(), zoneId));
            }
        }

        return templateZonePairSet;
    }
     */

    private VMTemplateVO updateTemplateOrIso(BaseUpdateTemplateOrIsoCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getTemplateName();
        String displayText = cmd.getDisplayText();
        String format = cmd.getFormat();
        Long guestOSId = cmd.getOsTypeId();
        Boolean passwordEnabled = cmd.isPasswordEnabled();
        Boolean bootable = cmd.isBootable();
        Integer sortKey = cmd.getSortKey();
        Boolean isDynamicallyScalable = cmd.isDynamicallyScalable();
        Boolean isRoutingTemplate = cmd.isRoutingType();
        Account account = CallContext.current().getCallingAccount();

        // verify that template exists
        VMTemplateVO template = _templateDao.findById(id);
        if (template == null || template.getRemoved() != null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find template/iso with specified id");
            ex.addProxyObject(id.toString(), "templateId");
            throw ex;
        }

        // Don't allow to modify system template
        if (id.equals(Long.valueOf(1))) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to update template/iso of specified id");
            ex.addProxyObject(template.getUuid(), "templateId");
            throw ex;
        }

        // do a permission check
        _accountMgr.checkAccess(account, AccessType.ModifyEntry, true, template);

        if (cmd.isRoutingType() != null) {
            if (!_accountService.isRootAdmin(account.getType())) {
                throw new PermissionDeniedException("Parameter isrouting can only be specified by a Root Admin, permission denied");
            }
        }
        boolean updateNeeded =
            !(name == null && displayText == null && format == null && guestOSId == null && passwordEnabled == null && bootable == null && sortKey == null &&
                isDynamicallyScalable == null && isRoutingTemplate == null);
        if (!updateNeeded) {
            return template;
        }

        template = _templateDao.createForUpdate(id);

        if (name != null) {
            template.setName(name);
        }

        if (displayText != null) {
            template.setDisplayText(displayText);
        }

        if (sortKey != null) {
            template.setSortKey(sortKey);
        }

        ImageFormat imageFormat = null;
        if (format != null) {
            try {
                imageFormat = ImageFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("Image format: " + format + " is incorrect. Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
            }

            template.setFormat(imageFormat);
        }

        if (guestOSId != null) {
            GuestOSVO guestOS = _guestOSDao.findById(guestOSId);

            if (guestOS == null) {
                throw new InvalidParameterValueException("Please specify a valid guest OS ID.");
            } else {
                template.setGuestOSId(guestOSId);
            }
        }

        if (passwordEnabled != null) {
            template.setEnablePassword(passwordEnabled);
        }

        if (bootable != null) {
            template.setBootable(bootable);
        }

        if (isDynamicallyScalable != null) {
            template.setDynamicallyScalable(isDynamicallyScalable);
        }

        if (isRoutingTemplate != null) {
            if (isRoutingTemplate) {
                template.setTemplateType(TemplateType.ROUTING);
            } else {
                template.setTemplateType(TemplateType.USER);
            }
        }

        _templateDao.update(id, template);

        return _templateDao.findById(id);
    }

    @Override
    public Pair<List<? extends IpAddress>, Integer> searchForIPAddresses(ListPublicIpAddressesCmd cmd) {
        Object keyword = cmd.getKeyword();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long associatedNetworkId = cmd.getAssociatedNetworkId();
        Long zone = cmd.getZoneId();
        String address = cmd.getIpAddress();
        Long vlan = cmd.getVlanId();
        Boolean forVirtualNetwork = cmd.isForVirtualNetwork();
        Boolean forLoadBalancing = cmd.isForLoadBalancing();
        Long ipId = cmd.getId();
        Boolean sourceNat = cmd.getIsSourceNat();
        Boolean staticNat = cmd.getIsStaticNat();
        Long vpcId = cmd.getVpcId();
        Map<String, String> tags = cmd.getTags();

        Boolean isAllocated = cmd.isAllocatedOnly();
        if (isAllocated == null) {
            isAllocated = Boolean.TRUE;
        }

        Filter searchFilter = new Filter(IPAddressVO.class, "address", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<IPAddressVO> sb = _publicIpAddressDao.createSearchBuilder();
        Long domainId = null;
        Boolean isRecursive = null;
        List<Long> permittedAccounts = new ArrayList<Long>();
        ListProjectResourcesCriteria listProjectResourcesCriteria = null;
        if (isAllocated) {
            Account caller = CallContext.current().getCallingAccount();

            Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject =
                new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
            _accountMgr.buildACLSearchParameters(caller, cmd.getId(), cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject,
                cmd.listAll(), false);
            domainId = domainIdRecursiveListProject.first();
            isRecursive = domainIdRecursiveListProject.second();
            listProjectResourcesCriteria = domainIdRecursiveListProject.third();
            _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        }

        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("address", sb.entity().getAddress(), SearchCriteria.Op.EQ);
        sb.and("vlanDbId", sb.entity().getVlanId(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("physicalNetworkId", sb.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        sb.and("associatedNetworkIdEq", sb.entity().getAssociatedWithNetworkId(), SearchCriteria.Op.EQ);
        sb.and("isSourceNat", sb.entity().isSourceNat(), SearchCriteria.Op.EQ);
        sb.and("isStaticNat", sb.entity().isOneToOneNat(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);

        if (forLoadBalancing != null && forLoadBalancing) {
            SearchBuilder<LoadBalancerVO> lbSearch = _loadbalancerDao.createSearchBuilder();
            sb.join("lbSearch", lbSearch, sb.entity().getId(), lbSearch.entity().getSourceIpAddressId(), JoinType.INNER);
            sb.groupBy(sb.entity().getId());
        }

        if (keyword != null && address == null) {
            sb.and("addressLIKE", sb.entity().getAddress(), SearchCriteria.Op.LIKE);
        }

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        vlanSearch.and("vlanType", vlanSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        sb.join("vlanSearch", vlanSearch, sb.entity().getVlanId(), vlanSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        boolean allocatedOnly = false;
        if ((isAllocated != null) && (isAllocated == true)) {
            sb.and("allocated", sb.entity().getAllocatedTime(), SearchCriteria.Op.NNULL);
            allocatedOnly = true;
        }

        VlanType vlanType = null;
        if (forVirtualNetwork != null) {
            vlanType = forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;
        } else {
            vlanType = VlanType.VirtualNetwork;
        }

        SearchCriteria<IPAddressVO> sc = sb.create();
        if (isAllocated) {
            _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        }

        sc.setJoinParameters("vlanSearch", "vlanType", vlanType);

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.PublicIpAddress.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }

        if (vpcId != null) {
            sc.setParameters("vpcId", vpcId);
        }

        if (ipId != null) {
            sc.setParameters("id", ipId);
        }

        if (sourceNat != null) {
            sc.setParameters("isSourceNat", sourceNat);
        }

        if (staticNat != null) {
            sc.setParameters("isStaticNat", staticNat);
        }

        if (address == null && keyword != null) {
            sc.setParameters("addressLIKE", "%" + keyword + "%");
        }

        if (address != null) {
            sc.setParameters("address", address);
        }

        if (vlan != null) {
            sc.setParameters("vlanDbId", vlan);
        }

        if (physicalNetworkId != null) {
            sc.setParameters("physicalNetworkId", physicalNetworkId);
        }

        if (associatedNetworkId != null) {
            sc.setParameters("associatedNetworkIdEq", associatedNetworkId);
        }

        Pair<List<IPAddressVO>, Integer> result = _publicIpAddressDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends IpAddress>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends GuestOS>, Integer> listGuestOSByCriteria(ListGuestOsCmd cmd) {
        Filter searchFilter = new Filter(GuestOSVO.class, "displayName", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long id = cmd.getId();
        Long osCategoryId = cmd.getOsCategoryId();
        String description = cmd.getDescription();
        String keyword = cmd.getKeyword();

        SearchCriteria<GuestOSVO> sc = _guestOSDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (osCategoryId != null) {
            sc.addAnd("categoryId", SearchCriteria.Op.EQ, osCategoryId);
        }

        if (description != null) {
            sc.addAnd("displayName", SearchCriteria.Op.LIKE, "%" + description + "%");
        }

        if (keyword != null) {
            sc.addAnd("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        Pair<List<GuestOSVO>, Integer> result = _guestOSDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends GuestOS>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends GuestOsCategory>, Integer> listGuestOSCategoriesByCriteria(ListGuestOsCategoriesCmd cmd) {
        Filter searchFilter = new Filter(GuestOSCategoryVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long id = cmd.getId();
        String name = cmd.getName();
        String keyword = cmd.getKeyword();

        SearchCriteria<GuestOSCategoryVO> sc = _guestOSCategoryDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (keyword != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        Pair<List<GuestOSCategoryVO>, Integer> result = _guestOSCategoryDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends GuestOsCategory>, Integer>(result.first(), result.second());
    }

    protected ConsoleProxyInfo getConsoleProxyForVm(long dataCenterId, long userVmId) {
        return _consoleProxyMgr.assignProxy(dataCenterId, userVmId);
    }

    private ConsoleProxyVO startConsoleProxy(long instanceId) {
        return _consoleProxyMgr.startProxy(instanceId);
    }

    private ConsoleProxyVO stopConsoleProxy(VMInstanceVO systemVm, boolean isForced) throws ResourceUnavailableException, OperationTimedoutException,
        ConcurrentOperationException {

        _itMgr.advanceStop(systemVm.getUuid(), isForced);
        return _consoleProxyDao.findById(systemVm.getId());
    }

    private ConsoleProxyVO rebootConsoleProxy(long instanceId) {
        _consoleProxyMgr.rebootProxy(instanceId);
        return _consoleProxyDao.findById(instanceId);
    }

    protected ConsoleProxyVO destroyConsoleProxy(long instanceId) {
        ConsoleProxyVO proxy = _consoleProxyDao.findById(instanceId);

        if (_consoleProxyMgr.destroyProxy(instanceId)) {
            return proxy;
        }
        return null;
    }

    @Override
    public String getConsoleAccessUrlRoot(long vmId) {
        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm != null) {
            ConsoleProxyInfo proxy = getConsoleProxyForVm(vm.getDataCenterId(), vmId);
            if (proxy != null) {
                return proxy.getProxyImageUrl();
            }
        }
        return null;
    }

    @Override
    public Pair<String, Integer> getVncPort(VirtualMachine vm) {
        if (vm.getHostId() == null) {
            s_logger.warn("VM " + vm.getHostName() + " does not have host, return -1 for its VNC port");
            return new Pair<String, Integer>(null, -1);
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Trying to retrieve VNC port from agent about VM " + vm.getHostName());
        }

        GetVncPortAnswer answer = (GetVncPortAnswer)_agentMgr.easySend(vm.getHostId(), new GetVncPortCommand(vm.getId(), vm.getInstanceName()));
        if (answer != null && answer.getResult()) {
            return new Pair<String, Integer>(answer.getAddress(), answer.getPort());
        }

        return new Pair<String, Integer>(null, -1);
    }

    @Override
    @DB
    public DomainVO updateDomain(UpdateDomainCmd cmd) {
        final Long domainId = cmd.getId();
        final String domainName = cmd.getDomainName();
        final String networkDomain = cmd.getNetworkDomain();

        // check if domain exists in the system
        final DomainVO domain = _domainDao.findById(domainId);
        if (domain == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find domain with specified domain id");
            ex.addProxyObject(domainId.toString(), "domainId");
            throw ex;
        } else if (domain.getParent() == null && domainName != null) {
            // check if domain is ROOT domain - and deny to edit it with the new
            // name
            throw new InvalidParameterValueException("ROOT domain can not be edited with a new name");
        }

        // check permissions
        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, domain);

        // domain name is unique under the parent domain
        if (domainName != null) {
            SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
            sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
            sc.addAnd("parent", SearchCriteria.Op.EQ, domain.getParent());
            List<DomainVO> domains = _domainDao.search(sc, null);

            boolean sameDomain = (domains.size() == 1 && domains.get(0).getId() == domainId);

            if (!domains.isEmpty() && !sameDomain) {
                InvalidParameterValueException ex =
                    new InvalidParameterValueException("Failed to update specified domain id with name '" + domainName + "' since it already exists in the system");
                ex.addProxyObject(domain.getUuid(), "domainId");
                throw ex;
            }
        }

        // validate network domain
        if (networkDomain != null && !networkDomain.isEmpty()) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                    "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                        + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                if (domainName != null) {
                    String updatedDomainPath = getUpdatedDomainPath(domain.getPath(), domainName);
                    updateDomainChildren(domain, updatedDomainPath);
                    domain.setName(domainName);
                    domain.setPath(updatedDomainPath);
                }

                if (networkDomain != null) {
                    if (networkDomain.isEmpty()) {
                        domain.setNetworkDomain(null);
                    } else {
                        domain.setNetworkDomain(networkDomain);
                    }
                }
                _domainDao.update(domainId, domain);
            }
        });

        return _domainDao.findById(domainId);

    }

    private String getUpdatedDomainPath(String oldPath, String newName) {
        String[] tokenizedPath = oldPath.split("/");
        tokenizedPath[tokenizedPath.length - 1] = newName;
        StringBuilder finalPath = new StringBuilder();
        for (String token : tokenizedPath) {
            finalPath.append(token);
            finalPath.append("/");
        }
        return finalPath.toString();
    }

    private void updateDomainChildren(DomainVO domain, String updatedDomainPrefix) {
        List<DomainVO> domainChildren = _domainDao.findAllChildren(domain.getPath(), domain.getId());
        // for each child, update the path
        for (DomainVO dom : domainChildren) {
            dom.setPath(dom.getPath().replaceFirst(domain.getPath(), updatedDomainPrefix));
            _domainDao.update(dom.getId(), dom);
        }
    }

    @Override
    public Pair<List<? extends Alert>, Integer> searchForAlerts(ListAlertsCmd cmd) {
        Filter searchFilter = new Filter(AlertVO.class, "lastSent", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<AlertVO> sc = _alertDao.createSearchCriteria();

        Object id = cmd.getId();
        Object type = cmd.getType();
        Object keyword = cmd.getKeyword();
        Object name = cmd.getName();

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), null);
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        if (zoneId != null) {
            sc.addAnd("data_center_id", SearchCriteria.Op.EQ, zoneId);
        }

        if (keyword != null) {
            SearchCriteria<AlertVO> ssc = _alertDao.createSearchCriteria();
            ssc.addOr("subject", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("subject", SearchCriteria.Op.SC, ssc);
        }

        if (type != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, type);
        }
        
        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        sc.addAnd("archived", SearchCriteria.Op.EQ, false);
        Pair<List<AlertVO>, Integer> result = _alertDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Alert>, Integer>(result.first(), result.second());
    }

    @Override
    public boolean archiveAlerts(ArchiveAlertsCmd cmd) {
        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), null);
        boolean result = _alertDao.archiveAlert(cmd.getIds(), cmd.getType(), cmd.getStartDate(), cmd.getEndDate(), zoneId);
        return result;
    }

    @Override
    public boolean deleteAlerts(DeleteAlertsCmd cmd) {
        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), null);
        boolean result = _alertDao.deleteAlert(cmd.getIds(), cmd.getType(), cmd.getStartDate(), cmd.getEndDate(), zoneId);
        return result;
    }

    @Override
    public List<CapacityVO> listTopConsumedResources(ListCapacityCmd cmd) {

        Integer capacityType = cmd.getType();
        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        Long clusterId = cmd.getClusterId();

        if (clusterId != null) {
            throw new InvalidParameterValueException("Currently clusterId param is not suppoerted");
        }
        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);
        List<SummedCapacity> summedCapacities = new ArrayList<SummedCapacity>();

        if (zoneId == null && podId == null) {// Group by Zone, capacity type
            List<SummedCapacity> summedCapacitiesAtZone =
                _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 1, cmd.getPageSizeVal());
            if (summedCapacitiesAtZone != null) {
                summedCapacities.addAll(summedCapacitiesAtZone);
            }
        } else if (podId == null) {// Group by Pod, capacity type
            List<SummedCapacity> summedCapacitiesAtPod =
                _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 2, cmd.getPageSizeVal());
            if (summedCapacitiesAtPod != null) {
                summedCapacities.addAll(summedCapacitiesAtPod);
            }
        } else { // Group by Cluster, capacity type
            List<SummedCapacity> summedCapacitiesAtCluster =
                _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 3, cmd.getPageSizeVal());
            if (summedCapacitiesAtCluster != null) {
                summedCapacities.addAll(summedCapacitiesAtCluster);
            }
        }

        List<SummedCapacity> summedCapacitiesForSecStorage = getSecStorageUsed(zoneId, capacityType);
        if (summedCapacitiesForSecStorage != null) {
            summedCapacities.addAll(summedCapacitiesForSecStorage);
        }

        // Sort Capacities
        Collections.sort(summedCapacities, new Comparator<SummedCapacity>() {
            @Override
            public int compare(SummedCapacity arg0, SummedCapacity arg1) {
                if (arg0.getPercentUsed() < arg1.getPercentUsed()) {
                    return 1;
                } else if (arg0.getPercentUsed().equals(arg1.getPercentUsed())) {
                    return 0;
                }
                return -1;
            }
        });

        List<CapacityVO> capacities = new ArrayList<CapacityVO>();

        Integer pageSize = null;
        try {
            pageSize = Integer.valueOf(cmd.getPageSizeVal().toString());
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException("pageSize " + cmd.getPageSizeVal() + " is out of Integer range is not supported for this call");
        }

        summedCapacities = summedCapacities.subList(0, summedCapacities.size() < cmd.getPageSizeVal() ? summedCapacities.size() : pageSize);
        for (SummedCapacity summedCapacity : summedCapacities) {
            CapacityVO capacity =
                new CapacityVO(summedCapacity.getDataCenterId(), summedCapacity.getPodId(), summedCapacity.getClusterId(), summedCapacity.getCapacityType(),
                    summedCapacity.getPercentUsed());
            capacity.setUsedCapacity(summedCapacity.getUsedCapacity() + summedCapacity.getReservedCapacity());
            capacity.setTotalCapacity(summedCapacity.getTotalCapacity());
            capacities.add(capacity);
        }
        return capacities;
    }

    List<SummedCapacity> getSecStorageUsed(Long zoneId, Integer capacityType) {
        if (capacityType == null || capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE) {
            List<SummedCapacity> list = new ArrayList<SummedCapacity>();
            if (zoneId != null) {
                DataCenterVO zone = ApiDBUtils.findZoneById(zoneId);
                if (zone == null || zone.getAllocationState() == AllocationState.Disabled) {
                    return null;
                }
                CapacityVO capacity = _storageMgr.getSecondaryStorageUsedStats(null, zoneId);
                if (capacity.getTotalCapacity() != 0) {
                    capacity.setUsedPercentage(capacity.getUsedCapacity() / capacity.getTotalCapacity());
                } else {
                    capacity.setUsedPercentage(0);
                }
                SummedCapacity summedCapacity =
                    new SummedCapacity(capacity.getUsedCapacity(), capacity.getTotalCapacity(), capacity.getUsedPercentage(), capacity.getCapacityType(),
                        capacity.getDataCenterId(), capacity.getPodId(), capacity.getClusterId());
                list.add(summedCapacity);
            } else {
                List<DataCenterVO> dcList = _dcDao.listEnabledZones();
                for (DataCenterVO dc : dcList) {
                    CapacityVO capacity = _storageMgr.getSecondaryStorageUsedStats(null, dc.getId());
                    if (capacity.getTotalCapacity() != 0) {
                        capacity.setUsedPercentage((float)capacity.getUsedCapacity() / capacity.getTotalCapacity());
                    } else {
                        capacity.setUsedPercentage(0);
                    }
                    SummedCapacity summedCapacity =
                        new SummedCapacity(capacity.getUsedCapacity(), capacity.getTotalCapacity(), capacity.getUsedPercentage(), capacity.getCapacityType(),
                            capacity.getDataCenterId(), capacity.getPodId(), capacity.getClusterId());
                    list.add(summedCapacity);
                }// End of for
            }
            return list;
        }
        return null;
    }

    @Override
    public List<CapacityVO> listCapacities(ListCapacityCmd cmd) {

        Integer capacityType = cmd.getType();
        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        Long clusterId = cmd.getClusterId();
        Boolean fetchLatest = cmd.getFetchLatest();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);
        if (fetchLatest != null && fetchLatest) {
            _alertMgr.recalculateCapacity();
        }

        List<SummedCapacity> summedCapacities = _capacityDao.findCapacityBy(capacityType, zoneId, podId, clusterId);
        List<CapacityVO> capacities = new ArrayList<CapacityVO>();

        for (SummedCapacity summedCapacity : summedCapacities) {
            CapacityVO capacity =
                new CapacityVO(null, summedCapacity.getDataCenterId(), podId, clusterId, summedCapacity.getUsedCapacity() + summedCapacity.getReservedCapacity(),
                    summedCapacity.getTotalCapacity(), summedCapacity.getCapacityType());
            capacities.add(capacity);
        }

        // op_host_Capacity contains only allocated stats and the real time
        // stats are stored "in memory".
        // Show Sec. Storage only when the api is invoked for the zone layer.
        List<DataCenterVO> dcList = new ArrayList<DataCenterVO>();
        if (zoneId == null && podId == null && clusterId == null) {
            dcList = ApiDBUtils.listZones();
        } else if (zoneId != null) {
            dcList.add(ApiDBUtils.findZoneById(zoneId));
        } else {
            if (clusterId != null) {
                zoneId = ApiDBUtils.findClusterById(clusterId).getDataCenterId();
            } else {
                zoneId = ApiDBUtils.findPodById(podId).getDataCenterId();
            }
            if (capacityType == null || capacityType == Capacity.CAPACITY_TYPE_STORAGE) {
                capacities.add(_storageMgr.getStoragePoolUsedStats(null, clusterId, podId, zoneId));
            }
        }

        for (DataCenterVO zone : dcList) {
            zoneId = zone.getId();
            if ((capacityType == null || capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE) && podId == null && clusterId == null) {
                capacities.add(_storageMgr.getSecondaryStorageUsedStats(null, zoneId));
            }
            if (capacityType == null || capacityType == Capacity.CAPACITY_TYPE_STORAGE) {
                capacities.add(_storageMgr.getStoragePoolUsedStats(null, clusterId, podId, zoneId));
            }
        }
        return capacities;
    }

    @Override
    public long getMemoryOrCpuCapacityByHost(Long hostId, short capacityType) {

        CapacityVO capacity = _capacityDao.findByHostIdType(hostId, capacityType);
        return capacity == null ? 0 : capacity.getReservedCapacity() + capacity.getUsedCapacity();

    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) ||
            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateAccountCmd.class);
        cmdList.add(DeleteAccountCmd.class);
        cmdList.add(DisableAccountCmd.class);
        cmdList.add(EnableAccountCmd.class);
        cmdList.add(LockAccountCmd.class);
        cmdList.add(UpdateAccountCmd.class);
        cmdList.add(CreateCounterCmd.class);
        cmdList.add(DeleteCounterCmd.class);
        cmdList.add(AddClusterCmd.class);
        cmdList.add(DeleteClusterCmd.class);
        cmdList.add(ListClustersCmd.class);
        cmdList.add(UpdateClusterCmd.class);
        cmdList.add(ListCfgsByCmd.class);
        cmdList.add(ListHypervisorCapabilitiesCmd.class);
        cmdList.add(UpdateCfgCmd.class);
        cmdList.add(UpdateHypervisorCapabilitiesCmd.class);
        cmdList.add(CreateDomainCmd.class);
        cmdList.add(DeleteDomainCmd.class);
        cmdList.add(ListDomainChildrenCmd.class);
        cmdList.add(ListDomainsCmd.class);
        cmdList.add(UpdateDomainCmd.class);
        cmdList.add(AddHostCmd.class);
        cmdList.add(AddSecondaryStorageCmd.class);
        cmdList.add(CancelMaintenanceCmd.class);
        cmdList.add(DeleteHostCmd.class);
        cmdList.add(ListHostsCmd.class);
        cmdList.add(FindHostsForMigrationCmd.class);
        cmdList.add(PrepareForMaintenanceCmd.class);
        cmdList.add(ReconnectHostCmd.class);
        cmdList.add(UpdateHostCmd.class);
        cmdList.add(UpdateHostPasswordCmd.class);
        cmdList.add(AddNetworkDeviceCmd.class);
        cmdList.add(AddNetworkServiceProviderCmd.class);
        cmdList.add(CreateNetworkOfferingCmd.class);
        cmdList.add(CreatePhysicalNetworkCmd.class);
        cmdList.add(CreateStorageNetworkIpRangeCmd.class);
        cmdList.add(DeleteNetworkDeviceCmd.class);
        cmdList.add(DeleteNetworkOfferingCmd.class);
        cmdList.add(DeleteNetworkServiceProviderCmd.class);
        cmdList.add(DeletePhysicalNetworkCmd.class);
        cmdList.add(DeleteStorageNetworkIpRangeCmd.class);
        cmdList.add(ListNetworkDeviceCmd.class);
        cmdList.add(ListNetworkServiceProvidersCmd.class);
        cmdList.add(ListPhysicalNetworksCmd.class);
        cmdList.add(ListStorageNetworkIpRangeCmd.class);
        cmdList.add(ListSupportedNetworkServicesCmd.class);
        cmdList.add(UpdateNetworkOfferingCmd.class);
        cmdList.add(UpdateNetworkServiceProviderCmd.class);
        cmdList.add(UpdatePhysicalNetworkCmd.class);
        cmdList.add(UpdateStorageNetworkIpRangeCmd.class);
        cmdList.add(DedicateGuestVlanRangeCmd.class);
        cmdList.add(ListDedicatedGuestVlanRangesCmd.class);
        cmdList.add(ReleaseDedicatedGuestVlanRangeCmd.class);
        cmdList.add(CreateDiskOfferingCmd.class);
        cmdList.add(CreateServiceOfferingCmd.class);
        cmdList.add(DeleteDiskOfferingCmd.class);
        cmdList.add(DeleteServiceOfferingCmd.class);
        cmdList.add(UpdateDiskOfferingCmd.class);
        cmdList.add(UpdateServiceOfferingCmd.class);
        cmdList.add(CreatePodCmd.class);
        cmdList.add(DeletePodCmd.class);
        cmdList.add(ListPodsByCmd.class);
        cmdList.add(UpdatePodCmd.class);
        cmdList.add(AddRegionCmd.class);
        cmdList.add(RemoveRegionCmd.class);
        cmdList.add(UpdateRegionCmd.class);
        cmdList.add(ListAlertsCmd.class);
        cmdList.add(ListCapacityCmd.class);
        cmdList.add(UploadCustomCertificateCmd.class);
        cmdList.add(ConfigureVirtualRouterElementCmd.class);
        cmdList.add(CreateVirtualRouterElementCmd.class);
        cmdList.add(DestroyRouterCmd.class);
        cmdList.add(ListRoutersCmd.class);
        cmdList.add(ListVirtualRouterElementsCmd.class);
        cmdList.add(RebootRouterCmd.class);
        cmdList.add(StartRouterCmd.class);
        cmdList.add(StopRouterCmd.class);
        cmdList.add(UpgradeRouterCmd.class);
        cmdList.add(AddS3Cmd.class);
        cmdList.add(AddSwiftCmd.class);
        cmdList.add(CancelPrimaryStorageMaintenanceCmd.class);
        cmdList.add(CreateStoragePoolCmd.class);
        cmdList.add(DeletePoolCmd.class);
        cmdList.add(ListS3sCmd.class);
        cmdList.add(ListSwiftsCmd.class);
        cmdList.add(ListStoragePoolsCmd.class);
        cmdList.add(FindStoragePoolsForMigrationCmd.class);
        cmdList.add(PreparePrimaryStorageForMaintenanceCmd.class);
        cmdList.add(UpdateStoragePoolCmd.class);
        cmdList.add(DestroySystemVmCmd.class);
        cmdList.add(ListSystemVMsCmd.class);
        cmdList.add(MigrateSystemVMCmd.class);
        cmdList.add(RebootSystemVmCmd.class);
        cmdList.add(StartSystemVMCmd.class);
        cmdList.add(StopSystemVmCmd.class);
        cmdList.add(UpgradeSystemVMCmd.class);
        cmdList.add(PrepareTemplateCmd.class);
        cmdList.add(AddTrafficMonitorCmd.class);
        cmdList.add(AddTrafficTypeCmd.class);
        cmdList.add(DeleteTrafficMonitorCmd.class);
        cmdList.add(DeleteTrafficTypeCmd.class);
        cmdList.add(GenerateUsageRecordsCmd.class);
        cmdList.add(GetUsageRecordsCmd.class);
        cmdList.add(ListTrafficMonitorsCmd.class);
        cmdList.add(ListTrafficTypeImplementorsCmd.class);
        cmdList.add(ListTrafficTypesCmd.class);
        cmdList.add(ListUsageTypesCmd.class);
        cmdList.add(UpdateTrafficTypeCmd.class);
        cmdList.add(CreateUserCmd.class);
        cmdList.add(DeleteUserCmd.class);
        cmdList.add(DisableUserCmd.class);
        cmdList.add(EnableUserCmd.class);
        cmdList.add(GetUserCmd.class);
        cmdList.add(ListUsersCmd.class);
        cmdList.add(LockUserCmd.class);
        cmdList.add(RegisterCmd.class);
        cmdList.add(UpdateUserCmd.class);
        cmdList.add(CreateVlanIpRangeCmd.class);
        cmdList.add(DeleteVlanIpRangeCmd.class);
        cmdList.add(ListVlanIpRangesCmd.class);
        cmdList.add(DedicatePublicIpRangeCmd.class);
        cmdList.add(ReleasePublicIpRangeCmd.class);
        cmdList.add(AssignVMCmd.class);
        cmdList.add(MigrateVMCmd.class);
        cmdList.add(MigrateVirtualMachineWithVolumeCmd.class);
        cmdList.add(RecoverVMCmd.class);
        cmdList.add(CreatePrivateGatewayCmd.class);
        cmdList.add(CreateVPCOfferingCmd.class);
        cmdList.add(DeletePrivateGatewayCmd.class);
        cmdList.add(DeleteVPCOfferingCmd.class);
        cmdList.add(UpdateVPCOfferingCmd.class);
        cmdList.add(CreateZoneCmd.class);
        cmdList.add(DeleteZoneCmd.class);
        cmdList.add(MarkDefaultZoneForAccountCmd.class);
        cmdList.add(UpdateZoneCmd.class);
        cmdList.add(AddAccountToProjectCmd.class);
        cmdList.add(DeleteAccountFromProjectCmd.class);
        cmdList.add(ListAccountsCmd.class);
        cmdList.add(ListProjectAccountsCmd.class);
        cmdList.add(AssociateIPAddrCmd.class);
        cmdList.add(DisassociateIPAddrCmd.class);
        cmdList.add(ListPublicIpAddressesCmd.class);
        cmdList.add(CreateAutoScalePolicyCmd.class);
        cmdList.add(CreateAutoScaleVmGroupCmd.class);
        cmdList.add(CreateAutoScaleVmProfileCmd.class);
        cmdList.add(CreateConditionCmd.class);
        cmdList.add(DeleteAutoScalePolicyCmd.class);
        cmdList.add(DeleteAutoScaleVmGroupCmd.class);
        cmdList.add(DeleteAutoScaleVmProfileCmd.class);
        cmdList.add(DeleteConditionCmd.class);
        cmdList.add(DisableAutoScaleVmGroupCmd.class);
        cmdList.add(EnableAutoScaleVmGroupCmd.class);
        cmdList.add(ListAutoScalePoliciesCmd.class);
        cmdList.add(ListAutoScaleVmGroupsCmd.class);
        cmdList.add(ListAutoScaleVmProfilesCmd.class);
        cmdList.add(ListConditionsCmd.class);
        cmdList.add(ListCountersCmd.class);
        cmdList.add(UpdateAutoScalePolicyCmd.class);
        cmdList.add(UpdateAutoScaleVmGroupCmd.class);
        cmdList.add(UpdateAutoScaleVmProfileCmd.class);
        cmdList.add(ListCapabilitiesCmd.class);
        cmdList.add(ListEventsCmd.class);
        cmdList.add(ListEventTypesCmd.class);
        cmdList.add(CreateEgressFirewallRuleCmd.class);
        cmdList.add(CreateFirewallRuleCmd.class);
        cmdList.add(CreatePortForwardingRuleCmd.class);
        cmdList.add(DeleteEgressFirewallRuleCmd.class);
        cmdList.add(DeleteFirewallRuleCmd.class);
        cmdList.add(DeletePortForwardingRuleCmd.class);
        cmdList.add(ListEgressFirewallRulesCmd.class);
        cmdList.add(ListFirewallRulesCmd.class);
        cmdList.add(ListPortForwardingRulesCmd.class);
        cmdList.add(UpdatePortForwardingRuleCmd.class);
        cmdList.add(ListGuestOsCategoriesCmd.class);
        cmdList.add(ListGuestOsCmd.class);
        cmdList.add(AttachIsoCmd.class);
        cmdList.add(CopyIsoCmd.class);
        cmdList.add(DeleteIsoCmd.class);
        cmdList.add(DetachIsoCmd.class);
        cmdList.add(ExtractIsoCmd.class);
        cmdList.add(ListIsoPermissionsCmd.class);
        cmdList.add(ListIsosCmd.class);
        cmdList.add(RegisterIsoCmd.class);
        cmdList.add(UpdateIsoCmd.class);
        cmdList.add(UpdateIsoPermissionsCmd.class);
        cmdList.add(ListAsyncJobsCmd.class);
        cmdList.add(QueryAsyncJobResultCmd.class);
        cmdList.add(AssignToLoadBalancerRuleCmd.class);
        cmdList.add(CreateLBStickinessPolicyCmd.class);
        cmdList.add(CreateLBHealthCheckPolicyCmd.class);
        cmdList.add(CreateLoadBalancerRuleCmd.class);
        cmdList.add(DeleteLBStickinessPolicyCmd.class);
        cmdList.add(DeleteLBHealthCheckPolicyCmd.class);
        cmdList.add(DeleteLoadBalancerRuleCmd.class);
        cmdList.add(ListLBStickinessPoliciesCmd.class);
        cmdList.add(ListLBHealthCheckPoliciesCmd.class);
        cmdList.add(ListLoadBalancerRuleInstancesCmd.class);
        cmdList.add(ListLoadBalancerRulesCmd.class);
        cmdList.add(RemoveFromLoadBalancerRuleCmd.class);
        cmdList.add(UpdateLoadBalancerRuleCmd.class);
        cmdList.add(CreateIpForwardingRuleCmd.class);
        cmdList.add(DeleteIpForwardingRuleCmd.class);
        cmdList.add(DisableStaticNatCmd.class);
        cmdList.add(EnableStaticNatCmd.class);
        cmdList.add(ListIpForwardingRulesCmd.class);
        cmdList.add(CreateNetworkACLCmd.class);
        cmdList.add(CreateNetworkCmd.class);
        cmdList.add(DeleteNetworkACLCmd.class);
        cmdList.add(DeleteNetworkCmd.class);
        cmdList.add(ListNetworkACLsCmd.class);
        cmdList.add(ListNetworkOfferingsCmd.class);
        cmdList.add(ListNetworksCmd.class);
        cmdList.add(RestartNetworkCmd.class);
        cmdList.add(UpdateNetworkCmd.class);
        cmdList.add(ListDiskOfferingsCmd.class);
        cmdList.add(ListServiceOfferingsCmd.class);
        cmdList.add(ActivateProjectCmd.class);
        cmdList.add(CreateProjectCmd.class);
        cmdList.add(DeleteProjectCmd.class);
        cmdList.add(DeleteProjectInvitationCmd.class);
        cmdList.add(ListProjectInvitationsCmd.class);
        cmdList.add(ListProjectsCmd.class);
        cmdList.add(SuspendProjectCmd.class);
        cmdList.add(UpdateProjectCmd.class);
        cmdList.add(UpdateProjectInvitationCmd.class);
        cmdList.add(ListRegionsCmd.class);
        cmdList.add(GetCloudIdentifierCmd.class);
        cmdList.add(ListHypervisorsCmd.class);
        cmdList.add(ListResourceLimitsCmd.class);
        cmdList.add(UpdateResourceCountCmd.class);
        cmdList.add(UpdateResourceLimitCmd.class);
        cmdList.add(AuthorizeSecurityGroupEgressCmd.class);
        cmdList.add(AuthorizeSecurityGroupIngressCmd.class);
        cmdList.add(CreateSecurityGroupCmd.class);
        cmdList.add(DeleteSecurityGroupCmd.class);
        cmdList.add(ListSecurityGroupsCmd.class);
        cmdList.add(RevokeSecurityGroupEgressCmd.class);
        cmdList.add(RevokeSecurityGroupIngressCmd.class);
        cmdList.add(CreateSnapshotCmd.class);
        cmdList.add(CreateSnapshotPolicyCmd.class);
        cmdList.add(DeleteSnapshotCmd.class);
        cmdList.add(DeleteSnapshotPoliciesCmd.class);
        cmdList.add(ListSnapshotPoliciesCmd.class);
        cmdList.add(ListSnapshotsCmd.class);
        cmdList.add(RevertSnapshotCmd.class);
        cmdList.add(CreateSSHKeyPairCmd.class);
        cmdList.add(DeleteSSHKeyPairCmd.class);
        cmdList.add(ListSSHKeyPairsCmd.class);
        cmdList.add(RegisterSSHKeyPairCmd.class);
        cmdList.add(CreateTagsCmd.class);
        cmdList.add(DeleteTagsCmd.class);
        cmdList.add(ListTagsCmd.class);
        cmdList.add(CopyTemplateCmd.class);
        cmdList.add(CreateTemplateCmd.class);
        cmdList.add(DeleteTemplateCmd.class);
        cmdList.add(ExtractTemplateCmd.class);
        cmdList.add(ListTemplatePermissionsCmd.class);
        cmdList.add(ListTemplatesCmd.class);
        cmdList.add(RegisterTemplateCmd.class);
        cmdList.add(UpdateTemplateCmd.class);
        cmdList.add(UpdateTemplatePermissionsCmd.class);
        cmdList.add(AddNicToVMCmd.class);
        cmdList.add(DeployVMCmd.class);
        cmdList.add(DestroyVMCmd.class);
        cmdList.add(ExpungeVMCmd.class);
        cmdList.add(GetVMPasswordCmd.class);
        cmdList.add(ListVMsCmd.class);
        cmdList.add(ScaleVMCmd.class);
        cmdList.add(RebootVMCmd.class);
        cmdList.add(RemoveNicFromVMCmd.class);
        cmdList.add(ResetVMPasswordCmd.class);
        cmdList.add(ResetVMSSHKeyCmd.class);
        cmdList.add(RestoreVMCmd.class);
        cmdList.add(StartVMCmd.class);
        cmdList.add(StopVMCmd.class);
        cmdList.add(UpdateDefaultNicForVMCmd.class);
        cmdList.add(UpdateVMCmd.class);
        cmdList.add(UpgradeVMCmd.class);
        cmdList.add(CreateVMGroupCmd.class);
        cmdList.add(DeleteVMGroupCmd.class);
        cmdList.add(ListVMGroupsCmd.class);
        cmdList.add(UpdateVMGroupCmd.class);
        cmdList.add(AttachVolumeCmd.class);
        cmdList.add(CreateVolumeCmd.class);
        cmdList.add(DeleteVolumeCmd.class);
        cmdList.add(UpdateVolumeCmd.class);
        cmdList.add(DetachVolumeCmd.class);
        cmdList.add(ExtractVolumeCmd.class);
        cmdList.add(ListVolumesCmd.class);
        cmdList.add(MigrateVolumeCmd.class);
        cmdList.add(ResizeVolumeCmd.class);
        cmdList.add(UploadVolumeCmd.class);
        cmdList.add(CreateStaticRouteCmd.class);
        cmdList.add(CreateVPCCmd.class);
        cmdList.add(DeleteStaticRouteCmd.class);
        cmdList.add(DeleteVPCCmd.class);
        cmdList.add(ListPrivateGatewaysCmd.class);
        cmdList.add(ListStaticRoutesCmd.class);
        cmdList.add(ListVPCOfferingsCmd.class);
        cmdList.add(ListVPCsCmd.class);
        cmdList.add(RestartVPCCmd.class);
        cmdList.add(UpdateVPCCmd.class);
        cmdList.add(AddVpnUserCmd.class);
        cmdList.add(CreateRemoteAccessVpnCmd.class);
        cmdList.add(CreateVpnConnectionCmd.class);
        cmdList.add(CreateVpnCustomerGatewayCmd.class);
        cmdList.add(CreateVpnGatewayCmd.class);
        cmdList.add(DeleteRemoteAccessVpnCmd.class);
        cmdList.add(DeleteVpnConnectionCmd.class);
        cmdList.add(DeleteVpnCustomerGatewayCmd.class);
        cmdList.add(DeleteVpnGatewayCmd.class);
        cmdList.add(ListRemoteAccessVpnsCmd.class);
        cmdList.add(ListVpnConnectionsCmd.class);
        cmdList.add(ListVpnCustomerGatewaysCmd.class);
        cmdList.add(ListVpnGatewaysCmd.class);
        cmdList.add(ListVpnUsersCmd.class);
        cmdList.add(RemoveVpnUserCmd.class);
        cmdList.add(ResetVpnConnectionCmd.class);
        cmdList.add(UpdateVpnCustomerGatewayCmd.class);
        cmdList.add(ListZonesByCmd.class);
        cmdList.add(ListVMSnapshotCmd.class);
        cmdList.add(CreateVMSnapshotCmd.class);
        cmdList.add(RevertToVMSnapshotCmd.class);
        cmdList.add(DeleteVMSnapshotCmd.class);
        cmdList.add(AddIpToVmNicCmd.class);
        cmdList.add(RemoveIpFromVmNicCmd.class);
        cmdList.add(ListNicsCmd.class);
        cmdList.add(ArchiveAlertsCmd.class);
        cmdList.add(DeleteAlertsCmd.class);
        cmdList.add(ArchiveEventsCmd.class);
        cmdList.add(DeleteEventsCmd.class);
        cmdList.add(CreateGlobalLoadBalancerRuleCmd.class);
        cmdList.add(DeleteGlobalLoadBalancerRuleCmd.class);
        cmdList.add(ListGlobalLoadBalancerRuleCmd.class);
        cmdList.add(UpdateGlobalLoadBalancerRuleCmd.class);
        cmdList.add(AssignToGlobalLoadBalancerRuleCmd.class);
        cmdList.add(RemoveFromGlobalLoadBalancerRuleCmd.class);
        cmdList.add(ListStorageProvidersCmd.class);
        cmdList.add(AddImageStoreCmd.class);
        cmdList.add(ListImageStoresCmd.class);
        cmdList.add(DeleteImageStoreCmd.class);
        cmdList.add(CreateSecondaryStagingStoreCmd.class);
        cmdList.add(ListSecondaryStagingStoresCmd.class);
        cmdList.add(DeleteSecondaryStagingStoreCmd.class);
        cmdList.add(PrepareSecondaryStorageForMigrationCmd.class);
        cmdList.add(CreateApplicationLoadBalancerCmd.class);
        cmdList.add(ListApplicationLoadBalancersCmd.class);
        cmdList.add(DeleteApplicationLoadBalancerCmd.class);
        cmdList.add(ConfigureInternalLoadBalancerElementCmd.class);
        cmdList.add(CreateInternalLoadBalancerElementCmd.class);
        cmdList.add(ListInternalLoadBalancerElementsCmd.class);
        cmdList.add(CreateAffinityGroupCmd.class);
        cmdList.add(DeleteAffinityGroupCmd.class);
        cmdList.add(ListAffinityGroupsCmd.class);
        cmdList.add(UpdateVMAffinityGroupCmd.class);
        cmdList.add(ListAffinityGroupTypesCmd.class);
        cmdList.add(CreatePortableIpRangeCmd.class);
        cmdList.add(DeletePortableIpRangeCmd.class);
        cmdList.add(ListPortableIpRangesCmd.class);
        cmdList.add(ListDeploymentPlannersCmd.class);
        cmdList.add(ReleaseHostReservationCmd.class);
        cmdList.add(ScaleSystemVMCmd.class);
        cmdList.add(AddResourceDetailCmd.class);
        cmdList.add(RemoveResourceDetailCmd.class);
        cmdList.add(ListResourceDetailsCmd.class);
        cmdList.add(StopInternalLBVMCmd.class);
        cmdList.add(StartInternalLBVMCmd.class);
        cmdList.add(ListInternalLBVMsCmd.class);
        cmdList.add(ListNetworkIsolationMethodsCmd.class);
        cmdList.add(ListNetworkIsolationMethodsCmd.class);
        cmdList.add(CreateNetworkACLListCmd.class);
        cmdList.add(DeleteNetworkACLListCmd.class);
        cmdList.add(ListNetworkACLListsCmd.class);
        cmdList.add(ReplaceNetworkACLListCmd.class);
        cmdList.add(UpdateNetworkACLItemCmd.class);
        cmdList.add(CleanVMReservationsCmd.class);
        cmdList.add(UpgradeRouterTemplateCmd.class);
        cmdList.add(UploadSslCertCmd.class);
        cmdList.add(DeleteSslCertCmd.class);
        cmdList.add(ListSslCertsCmd.class);
        cmdList.add(AssignCertToLoadBalancerCmd.class);
        cmdList.add(RemoveCertFromLoadBalancerCmd.class);
        cmdList.add(GenerateAlertCmd.class);
	cmdList.add(ListOvsElementsCmd.class);
	cmdList.add(ConfigureOvsElementCmd.class);
        return cmdList;
    }

    protected class EventPurgeTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                GlobalLock lock = GlobalLock.getInternLock("EventPurge");
                if (lock == null) {
                    s_logger.debug("Couldn't get the global lock");
                    return;
                }
                if (!lock.lock(30)) {
                    s_logger.debug("Couldn't lock the db");
                    return;
                }
                try {
                    final Calendar purgeCal = Calendar.getInstance();
                    purgeCal.add(Calendar.DAY_OF_YEAR, -_purgeDelay);
                    Date purgeTime = purgeCal.getTime();
                    s_logger.debug("Deleting events older than: " + purgeTime.toString());
                    List<EventVO> oldEvents = _eventDao.listOlderEvents(purgeTime);
                    s_logger.debug("Found " + oldEvents.size() + " events to be purged");
                    for (EventVO event : oldEvents) {
                        _eventDao.expunge(event.getId());
                    }
                } catch (Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }

    protected class AlertPurgeTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                GlobalLock lock = GlobalLock.getInternLock("AlertPurge");
                if (lock == null) {
                    s_logger.debug("Couldn't get the global lock");
                    return;
                }
                if (!lock.lock(30)) {
                    s_logger.debug("Couldn't lock the db");
                    return;
                }
                try {
                    final Calendar purgeCal = Calendar.getInstance();
                    purgeCal.add(Calendar.DAY_OF_YEAR, -_alertPurgeDelay);
                    Date purgeTime = purgeCal.getTime();
                    s_logger.debug("Deleting alerts older than: " + purgeTime.toString());
                    List<AlertVO> oldAlerts = _alertDao.listOlderAlerts(purgeTime);
                    s_logger.debug("Found " + oldAlerts.size() + " events to be purged");
                    for (AlertVO alert : oldAlerts) {
                        _alertDao.expunge(alert.getId());
                    }
                } catch (Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }

    @Override
    public Pair<List<StoragePoolVO>, Integer> searchForStoragePools(Criteria c) {
        Filter searchFilter = new Filter(StoragePoolVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria<StoragePoolVO> sc = _poolDao.createSearchCriteria();

        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object host = c.getCriteria(Criteria.HOST);
        Object path = c.getCriteria(Criteria.PATH);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object cluster = c.getCriteria(Criteria.CLUSTERID);
        Object address = c.getCriteria(Criteria.ADDRESS);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria<StoragePoolVO> ssc = _poolDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("poolType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        if (host != null) {
            sc.addAnd("host", SearchCriteria.Op.EQ, host);
        }
        if (path != null) {
            sc.addAnd("path", SearchCriteria.Op.EQ, path);
        }
        if (zone != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zone);
        }
        if (pod != null) {
            sc.addAnd("podId", SearchCriteria.Op.EQ, pod);
        }
        if (address != null) {
            sc.addAnd("hostAddress", SearchCriteria.Op.EQ, address);
        }
        if (cluster != null) {
            sc.addAnd("clusterId", SearchCriteria.Op.EQ, cluster);
        }

        return _poolDao.searchAndCount(sc, searchFilter);
    }

    private SecondaryStorageVmVO startSecondaryStorageVm(long instanceId) {
        return _secStorageVmMgr.startSecStorageVm(instanceId);
    }

    private SecondaryStorageVmVO stopSecondaryStorageVm(VMInstanceVO systemVm, boolean isForced) throws ResourceUnavailableException, OperationTimedoutException,
        ConcurrentOperationException {

        _itMgr.advanceStop(systemVm.getUuid(), isForced);
        return _secStorageVmDao.findById(systemVm.getId());
    }

    public SecondaryStorageVmVO rebootSecondaryStorageVm(long instanceId) {
        _secStorageVmMgr.rebootSecStorageVm(instanceId);
        return _secStorageVmDao.findById(instanceId);
    }

    protected SecondaryStorageVmVO destroySecondaryStorageVm(long instanceId) {
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(instanceId);
        if (_secStorageVmMgr.destroySecStorageVm(instanceId)) {
            return secStorageVm;
        }
        return null;
    }

    @Override
    public Pair<List<? extends VirtualMachine>, Integer> searchForSystemVm(ListSystemVMsCmd cmd) {
        String type = cmd.getSystemVmType();
        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Long id = cmd.getId();
        String name = cmd.getSystemVmName();
        String state = cmd.getState();
        String keyword = cmd.getKeyword();
        Long podId = cmd.getPodId();
        Long hostId = cmd.getHostId();
        Long storageId = cmd.getStorageId();

        Filter searchFilter = new Filter(VMInstanceVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<VMInstanceVO> sb = _vmInstanceDao.createSearchBuilder();

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("hostName", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("nulltype", sb.entity().getType(), SearchCriteria.Op.IN);

        if (storageId != null) {
            SearchBuilder<VolumeVO> volumeSearch = _volumeDao.createSearchBuilder();
            volumeSearch.and("poolId", volumeSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
            sb.join("volumeSearch", volumeSearch, sb.entity().getId(), volumeSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<VMInstanceVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<VMInstanceVO> ssc = _vmInstanceDao.createSearchCriteria();
            ssc.addOr("hostName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("hostName", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("hostName", name);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (podId != null) {
            sc.setParameters("podId", podId);
        }
        if (hostId != null) {
            sc.setParameters("hostId", hostId);
        }

        if (type != null) {
            sc.setParameters("type", type);
        } else {
            sc.setParameters("nulltype", VirtualMachine.Type.SecondaryStorageVm, VirtualMachine.Type.ConsoleProxy);
        }

        if (storageId != null) {
            sc.setJoinParameters("volumeSearch", "poolId", storageId);
        }

        Pair<List<VMInstanceVO>, Integer> result = _vmInstanceDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends VirtualMachine>, Integer>(result.first(), result.second());
    }

    @Override
    public VirtualMachine.Type findSystemVMTypeById(long instanceId) {
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a system vm of specified instanceId");
            ex.addProxyObject(String.valueOf(instanceId), "instanceId");
            throw ex;
        }
        return systemVm.getType();
    }

    @Override
    @ActionEvent(eventType = "", eventDescription = "", async = true)
    public VirtualMachine startSystemVM(long vmId) {

        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(vmId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
            ex.addProxyObject(String.valueOf(vmId), "vmId");
            throw ex;
        }

        if (systemVm.getType() == VirtualMachine.Type.ConsoleProxy) {
            ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_PROXY_START, "starting console proxy Vm");
            return startConsoleProxy(vmId);
        } else if (systemVm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
            ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_SSVM_START, "starting secondary storage Vm");
            return startSecondaryStorageVm(vmId);
        } else {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a system vm with specified vmId");
            ex.addProxyObject(systemVm.getUuid(), "vmId");
            throw ex;
        }
    }

    @Override
    @ActionEvent(eventType = "", eventDescription = "", async = true)
    public VMInstanceVO stopSystemVM(StopSystemVmCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        Long id = cmd.getId();

        // verify parameters
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(id, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
            ex.addProxyObject(id.toString(), "vmId");
            throw ex;
        }

        try {
            if (systemVm.getType() == VirtualMachine.Type.ConsoleProxy) {
                ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_PROXY_STOP, "stopping console proxy Vm");
                return stopConsoleProxy(systemVm, cmd.isForced());
            } else if (systemVm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
                ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_SSVM_STOP, "stopping secondary storage Vm");
                return stopSecondaryStorageVm(systemVm, cmd.isForced());
            }
            return null;
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + systemVm, e);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROXY_REBOOT, eventDescription = "", async = true)
    public VMInstanceVO rebootSystemVM(RebootSystemVmCmd cmd) {
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(cmd.getId(), VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);

        if (systemVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
            ex.addProxyObject(cmd.getId().toString(), "vmId");
            throw ex;
        }

        if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)) {
            ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_PROXY_REBOOT, "rebooting console proxy Vm");
            return rebootConsoleProxy(cmd.getId());
        } else {
            ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_SSVM_REBOOT, "rebooting secondary storage Vm");
            return rebootSecondaryStorageVm(cmd.getId());
        }
    }

    @Override
    @ActionEvent(eventType = "", eventDescription = "", async = true)
    public VMInstanceVO destroySystemVM(DestroySystemVmCmd cmd) {
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(cmd.getId(), VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);

        if (systemVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
            ex.addProxyObject(cmd.getId().toString(), "vmId");
            throw ex;
        }

        if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)) {
            ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_PROXY_DESTROY, "destroying console proxy Vm");
            return destroyConsoleProxy(cmd.getId());
        } else {
            ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_SSVM_DESTROY, "destroying secondary storage Vm");
            return destroySecondaryStorageVm(cmd.getId());
        }
    }

    private String signRequest(String request, String key) {
        try {
            s_logger.info("Request: " + request);
            s_logger.info("Key: " + key);

            if (key != null && request != null) {
                Mac mac = Mac.getInstance("HmacSHA1");
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                mac.init(keySpec);
                mac.update(request.getBytes());
                byte[] encryptedBytes = mac.doFinal();
                return new String((Base64.encodeBase64(encryptedBytes)));
            }
        } catch (Exception ex) {
            s_logger.error("unable to sign request", ex);
        }
        return null;
    }

    @Override
    public ArrayList<String> getCloudIdentifierResponse(long userId) {
        Account caller = CallContext.current().getCallingAccount();

        // verify that user exists
        User user = _accountMgr.getUserIncludingRemoved(userId);
        if ((user == null) || (user.getRemoved() != null)) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find active user of specified id");
            ex.addProxyObject(String.valueOf(userId), "userId");
            throw ex;
        }

        // check permissions
        _accountMgr.checkAccess(caller, null, true, _accountMgr.getAccount(user.getAccountId()));

        String cloudIdentifier = _configDao.getValue("cloud.identifier");
        if (cloudIdentifier == null) {
            cloudIdentifier = "";
        }

        String signature = "";
        try {
            // get the user obj to get his secret key
            user = _accountMgr.getActiveUser(userId);
            String secretKey = user.getSecretKey();
            String input = cloudIdentifier;
            signature = signRequest(input, secretKey);
        } catch (Exception e) {
            s_logger.warn("Exception whilst creating a signature:" + e);
        }

        ArrayList<String> cloudParams = new ArrayList<String>();
        cloudParams.add(cloudIdentifier);
        cloudParams.add(signature);

        return cloudParams;
    }

    @Override
    public Map<String, Object> listCapabilities(ListCapabilitiesCmd cmd) {
        Map<String, Object> capabilities = new HashMap<String, Object>();

        Account caller = CallContext.current().getCallingAccount();
        boolean securityGroupsEnabled = false;
        boolean elasticLoadBalancerEnabled = false;
        boolean KVMSnapshotEnabled = false;
        String supportELB = "false";
        List<NetworkVO> networks = _networkDao.listSecurityGroupEnabledNetworks();
        if (networks != null && !networks.isEmpty()) {
            securityGroupsEnabled = true;
            String elbEnabled = _configDao.getValue(Config.ElasticLoadBalancerEnabled.key());
            elasticLoadBalancerEnabled = elbEnabled == null ? false : Boolean.parseBoolean(elbEnabled);
            if (elasticLoadBalancerEnabled) {
                String networkType = _configDao.getValue(Config.ElasticLoadBalancerNetwork.key());
                if (networkType != null) {
                    supportELB = networkType;
                }
            }
        }

        long diskOffMaxSize = Long.valueOf(_configDao.getValue(Config.CustomDiskOfferingMaxSize.key()));
        KVMSnapshotEnabled = Boolean.parseBoolean(_configDao.getValue("KVM.snapshot.enabled"));

        boolean userPublicTemplateEnabled = TemplateManager.AllowPublicUserTemplates.valueIn(caller.getId());

        // add some parameters UI needs to handle API throttling
        boolean apiLimitEnabled = Boolean.parseBoolean(_configDao.getValue(Config.ApiLimitEnabled.key()));
        Integer apiLimitInterval = Integer.valueOf(_configDao.getValue(Config.ApiLimitInterval.key()));
        Integer apiLimitMax = Integer.valueOf(_configDao.getValue(Config.ApiLimitMax.key()));

        // check if region-wide secondary storage is used
        boolean regionSecondaryEnabled = false;
        List<ImageStoreVO> imgStores = _imgStoreDao.findRegionImageStores();
        if (imgStores != null && imgStores.size() > 0) {
            regionSecondaryEnabled = true;
        }

        capabilities.put("securityGroupsEnabled", securityGroupsEnabled);
        capabilities.put("userPublicTemplateEnabled", userPublicTemplateEnabled);
        capabilities.put("cloudStackVersion", getVersion());
        capabilities.put("supportELB", supportELB);
        capabilities.put("projectInviteRequired", _projectMgr.projectInviteRequired());
        capabilities.put("allowusercreateprojects", _projectMgr.allowUserToCreateProject());
        capabilities.put("customDiskOffMaxSize", diskOffMaxSize);
        capabilities.put("regionSecondaryEnabled", regionSecondaryEnabled);
        capabilities.put("KVMSnapshotEnabled", KVMSnapshotEnabled);
        if (apiLimitEnabled) {
            capabilities.put("apiLimitInterval", apiLimitInterval);
            capabilities.put("apiLimitMax", apiLimitMax);
        }

        return capabilities;
    }

    @Override
    public GuestOSVO getGuestOs(Long guestOsId) {
        return _guestOSDao.findById(guestOsId);
    }

    @Override
    public InstanceGroupVO updateVmGroup(UpdateVMGroupCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long groupId = cmd.getId();
        String groupName = cmd.getGroupName();

        // Verify input parameters
        InstanceGroupVO group = _vmGroupDao.findById(groupId.longValue());
        if (group == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a vm group with specified groupId");
            ex.addProxyObject(groupId.toString(), "groupId");
            throw ex;
        }

        _accountMgr.checkAccess(caller, null, true, group);

        // Check if name is already in use by this account (exclude this group)
        boolean isNameInUse = _vmGroupDao.isNameInUse(group.getAccountId(), groupName);

        if (isNameInUse && !group.getName().equals(groupName)) {
            throw new InvalidParameterValueException("Unable to update vm group, a group with name " + groupName + " already exists for account");
        }

        if (groupName != null) {
            _vmGroupDao.updateVmGroup(groupId, groupName);
        }

        return _vmGroupDao.findById(groupId);
    }

    @Override
    public String getVersion() {
        final Class<?> c = ManagementServer.class;
        String fullVersion = c.getPackage().getImplementationVersion();
        if (fullVersion != null && fullVersion.length() > 0) {
            return fullVersion;
        }

        return "unknown";
    }

    @Override
    public Long saveStartedEvent(Long userId, Long accountId, String type, String description, long startEventId) {
        return ActionEventUtils.onStartedActionEvent(userId, accountId, type, description, startEventId);
    }

    @Override
    public Long saveCompletedEvent(Long userId, Long accountId, String level, String type, String description, long startEventId) {
        return ActionEventUtils.onCompletedActionEvent(userId, accountId, level, type, description, startEventId);
    }

    @Override
    @DB
    public String uploadCertificate(UploadCustomCertificateCmd cmd) {
        if (cmd.getPrivateKey() != null && cmd.getAlias() != null) {
            throw new InvalidParameterValueException("Can't change the alias for private key certification");
        }

        if (cmd.getPrivateKey() == null) {
            if (cmd.getAlias() == null) {
                throw new InvalidParameterValueException("alias can't be empty, if it's a certification chain");
            }

            if (cmd.getCertIndex() == null) {
                throw new InvalidParameterValueException("index can't be empty, if it's a certifciation chain");
            }
        }

        String certificate = cmd.getCertificate();
        String key = cmd.getPrivateKey();
        try {
            if (certificate != null) {
                certificate = URLDecoder.decode(certificate, "UTF-8");
            }
            if (key != null) {
                key = URLDecoder.decode(key, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
        } finally {
        }

        if (cmd.getPrivateKey() != null && !_ksMgr.validateCertificate(certificate, key, cmd.getDomainSuffix())) {
            throw new InvalidParameterValueException("Failed to pass certificate validation check");
        }

        if (cmd.getPrivateKey() != null) {
            _ksMgr.saveCertificate(ConsoleProxyManager.CERTIFICATE_NAME, certificate, key, cmd.getDomainSuffix());
        } else {
            _ksMgr.saveCertificate(cmd.getAlias(), certificate, cmd.getCertIndex(), cmd.getDomainSuffix());
        }

        _consoleProxyMgr.setManagementState(ConsoleProxyManagementState.ResetSuspending);
        List<SecondaryStorageVmVO> alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates(null, State.Running, State.Migrating, State.Starting);
        for (SecondaryStorageVmVO ssVmVm : alreadyRunning) {
            _secStorageVmMgr.rebootSecStorageVm(ssVmVm.getId());
        }
        return "Certificate has been updated, we will stop all running console proxy VMs and secondary storage VMs to propagate the new certificate, please give a few minutes for console access service to be up again";
    }

    @Override
    public List<String> getHypervisors(Long zoneId) {
        List<String> result = new ArrayList<String>();
        String hypers = _configDao.getValue(Config.HypervisorList.key());
        String[] hypervisors = hypers.split(",");

        if (zoneId != null) {
            if (zoneId.longValue() == -1L) {
                List<DataCenterVO> zones = _dcDao.listAll();

                for (String hypervisor : hypervisors) {
                    int hyperCount = 0;
                    for (DataCenterVO zone : zones) {
                        List<ClusterVO> clusters = _clusterDao.listByDcHyType(zone.getId(), hypervisor);
                        if (!clusters.isEmpty()) {
                            hyperCount++;
                        }
                    }
                    if (hyperCount == zones.size()) {
                        result.add(hypervisor);
                    }
                }
            } else {
                List<ClusterVO> clustersForZone = _clusterDao.listByZoneId(zoneId);
                for (ClusterVO cluster : clustersForZone) {
                    result.add(cluster.getHypervisorType().toString());
                }
            }

        } else {
            return Arrays.asList(hypervisors);
        }
        return result;
    }

    @Override
    public String getHashKey() {
        // although we may have race conditioning here, database transaction serialization should
        // give us the same key
        if (_hashKey == null) {
            _hashKey =
                _configDao.getValueAndInitIfNotExist(Config.HashKey.key(), Config.HashKey.getCategory(), getBase64EncodedRandomKey(128), Config.HashKey.getDescription());
        }
        return _hashKey;
    }

    @Override
    public String getEncryptionKey() {
        if (_encryptionKey == null) {
            _encryptionKey =
                _configDao.getValueAndInitIfNotExist(Config.EncryptionKey.key(), Config.EncryptionKey.getCategory(), getBase64EncodedRandomKey(128),
                    Config.EncryptionKey.getDescription());
        }
        return _encryptionKey;
    }

    @Override
    public String getEncryptionIV() {
        if (_encryptionIV == null) {
            _encryptionIV =
                _configDao.getValueAndInitIfNotExist(Config.EncryptionIV.key(), Config.EncryptionIV.getCategory(), getBase64EncodedRandomKey(128),
                    Config.EncryptionIV.getDescription());
        }
        return _encryptionIV;
    }

    @Override
    @DB
    public void resetEncryptionKeyIV() {

        SearchBuilder<ConfigurationVO> sb = _configDao.createSearchBuilder();
        sb.and("name1", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.or("name2", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.done();

        SearchCriteria<ConfigurationVO> sc = sb.create();
        sc.setParameters("name1", Config.EncryptionKey.key());
        sc.setParameters("name2", Config.EncryptionIV.key());

        _configDao.expunge(sc);
        _encryptionKey = null;
        _encryptionIV = null;
    }

    private static String getBase64EncodedRandomKey(int nBits) {
        SecureRandom random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
            byte[] keyBytes = new byte[nBits / 8];
            random.nextBytes(keyBytes);
            return Base64.encodeBase64URLSafeString(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("Unhandled exception: ", e);
        }
        return null;
    }

    @Override
    public SSHKeyPair createSSHKeyPair(CreateSSHKeyPairCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long projectId = cmd.getProjectId();

        Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);

        SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s != null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' already exists.");
        }

        SSHKeysHelper keys = new SSHKeysHelper();

        String name = cmd.getName();
        String publicKey = keys.getPublicKey();
        String fingerprint = keys.getPublicKeyFingerPrint();
        String privateKey = keys.getPrivateKey();

        return createAndSaveSSHKeyPair(name, fingerprint, publicKey, privateKey, owner);
    }

    @Override
    public boolean deleteSSHKeyPair(DeleteSSHKeyPairCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long projectId = cmd.getProjectId();

        Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);

        SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s == null) {
            InvalidParameterValueException ex =
                new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' does not exist for account " + owner.getAccountName() +
                    " in specified domain id");
            DomainVO domain = ApiDBUtils.findDomainById(owner.getDomainId());
            String domainUuid = String.valueOf(owner.getDomainId());
            if (domain != null) {
                domainUuid = domain.getUuid();
            }
            ex.addProxyObject(domainUuid, "domainId");
            throw ex;
        }

        return _sshKeyPairDao.deleteByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
    }

    @Override
    public Pair<List<? extends SSHKeyPair>, Integer> listSSHKeyPairs(ListSSHKeyPairsCmd cmd) {
        String name = cmd.getName();
        String fingerPrint = cmd.getFingerprint();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject =
            new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(),
            false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        SearchBuilder<SSHKeyPairVO> sb = _sshKeyPairDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        Filter searchFilter = new Filter(SSHKeyPairVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchCriteria<SSHKeyPairVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        if (fingerPrint != null) {
            sc.addAnd("fingerprint", SearchCriteria.Op.EQ, fingerPrint);
        }

        Pair<List<SSHKeyPairVO>, Integer> result = _sshKeyPairDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends SSHKeyPair>, Integer>(result.first(), result.second());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_REGISTER_SSH_KEYPAIR, eventDescription = "registering ssh keypair", async = true)
    public SSHKeyPair registerSSHKeyPair(RegisterSSHKeyPairCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();

        Account owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());

        SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s != null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' already exists.");
        }

        String name = cmd.getName();
        String publicKey = SSHKeysHelper.getPublicKeyFromKeyMaterial(cmd.getPublicKey());

        if (publicKey == null) {
            throw new InvalidParameterValueException("Public key is invalid");
        }

        String fingerprint = SSHKeysHelper.getPublicKeyFingerprint(publicKey);

        return createAndSaveSSHKeyPair(name, fingerprint, publicKey, null, owner);
    }

    private SSHKeyPair createAndSaveSSHKeyPair(String name, String fingerprint, String publicKey, String privateKey, Account owner) {
        SSHKeyPairVO newPair = new SSHKeyPairVO();

        newPair.setAccountId(owner.getAccountId());
        newPair.setDomainId(owner.getDomainId());
        newPair.setName(name);
        newPair.setFingerprint(fingerprint);
        newPair.setPublicKey(publicKey);
        newPair.setPrivateKey(privateKey); // transient; not saved.

        _sshKeyPairDao.persist(newPair);

        return newPair;
    }

    @Override
    public String getVMPassword(GetVMPasswordCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();

        UserVmVO vm = _userVmDao.findById(cmd.getId());
        if (vm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("No VM with specified id found.");
            ex.addProxyObject(cmd.getId().toString(), "vmId");
            throw ex;
        }

        // make permission check
        _accountMgr.checkAccess(caller, null, true, vm);

        _userVmDao.loadDetails(vm);
        String password = vm.getDetail("Encrypted.Password");
        if (password == null || password.equals("")) {
            InvalidParameterValueException ex =
                new InvalidParameterValueException("No password for VM with specified id found. "
                    + "If VM is created from password enabled template and SSH keypair is assigned to VM then only password can be retrieved.");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        return password;
    }

    @Override
    @DB
    public boolean updateHostPassword(final UpdateHostPasswordCmd cmd) {
        if (cmd.getClusterId() == null && cmd.getHostId() == null) {
            throw new InvalidParameterValueException("You should provide one of cluster id or a host id.");
        } else if (cmd.getClusterId() == null) {
            HostVO host = _hostDao.findById(cmd.getHostId());
            if (host != null && host.getHypervisorType() == HypervisorType.XenServer) {
                throw new InvalidParameterValueException("You should provide cluster id for Xenserver cluster.");
            } else {
                throw new InvalidParameterValueException("This operation is not supported for this hypervisor type");
            }
        } else {

            ClusterVO cluster = ApiDBUtils.findClusterById(cmd.getClusterId());
            if (cluster == null || cluster.getHypervisorType() != HypervisorType.XenServer) {
                throw new InvalidParameterValueException("This operation is not supported for this hypervisor type");
            }
            // get all the hosts in this cluster
            final List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(cmd.getClusterId());

            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    for (HostVO h : hosts) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Changing password for host name = " + h.getName());
                        }
                        // update password for this host
                        DetailVO nv = _detailsDao.findDetail(h.getId(), ApiConstants.USERNAME);
                        if (nv.getValue().equals(cmd.getUsername())) {
                            DetailVO nvp = _detailsDao.findDetail(h.getId(), ApiConstants.PASSWORD);
                            nvp.setValue(DBEncryptionUtil.encrypt(cmd.getPassword()));
                            _detailsDao.persist(nvp);
                        } else {
                            // if one host in the cluster has diff username then
                            // rollback to maintain consistency
                            throw new InvalidParameterValueException("The username is not same for all hosts, please modify passwords for individual hosts.");
                        }
                    }
                }
            });
        }

        return true;
    }

    @Override
    public String[] listEventTypes() {
        Object eventObj = new EventTypes();
        Class<EventTypes> c = EventTypes.class;
        Field[] fields = c.getFields();
        String[] eventTypes = new String[fields.length];
        try {
            int i = 0;
            for (Field field : fields) {
                eventTypes[i++] = field.get(eventObj).toString();
            }
            return eventTypes;
        } catch (IllegalArgumentException e) {
            s_logger.error("Error while listing Event Types", e);
        } catch (IllegalAccessException e) {
            s_logger.error("Error while listing Event Types", e);
        }
        return null;
    }

    @Override
    public Pair<List<? extends HypervisorCapabilities>, Integer> listHypervisorCapabilities(Long id, HypervisorType hypervisorType, String keyword, Long startIndex,
        Long pageSizeVal) {
        Filter searchFilter = new Filter(HypervisorCapabilitiesVO.class, "id", true, startIndex, pageSizeVal);
        SearchCriteria<HypervisorCapabilitiesVO> sc = _hypervisorCapabilitiesDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (hypervisorType != null) {
            sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisorType);
        }

        if (keyword != null) {
            SearchCriteria<HypervisorCapabilitiesVO> ssc = _hypervisorCapabilitiesDao.createSearchCriteria();
            ssc.addOr("hypervisorType", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("hypervisorType", SearchCriteria.Op.SC, ssc);
        }

        Pair<List<HypervisorCapabilitiesVO>, Integer> result = _hypervisorCapabilitiesDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends HypervisorCapabilities>, Integer>(result.first(), result.second());
    }

    @Override
    public HypervisorCapabilities updateHypervisorCapabilities(Long id, Long maxGuestsLimit, Boolean securityGroupEnabled) {
        HypervisorCapabilitiesVO hpvCapabilities = _hypervisorCapabilitiesDao.findById(id, true);

        if (hpvCapabilities == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find the hypervisor capabilities for specified id");
            ex.addProxyObject(id.toString(), "Id");
            throw ex;
        }

        boolean updateNeeded = (maxGuestsLimit != null || securityGroupEnabled != null);
        if (!updateNeeded) {
            return hpvCapabilities;
        }

        hpvCapabilities = _hypervisorCapabilitiesDao.createForUpdate(id);

        if (maxGuestsLimit != null) {
            hpvCapabilities.setMaxGuestsLimit(maxGuestsLimit);
        }

        if (securityGroupEnabled != null) {
            hpvCapabilities.setSecurityGroupEnabled(securityGroupEnabled);
        }

        if (_hypervisorCapabilitiesDao.update(id, hpvCapabilities)) {
            hpvCapabilities = _hypervisorCapabilitiesDao.findById(id);
            CallContext.current().setEventDetails("Hypervisor Capabilities id=" + hpvCapabilities.getId());
            return hpvCapabilities;
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UPGRADE, eventDescription = "Upgrading system VM", async = true)
    public VirtualMachine upgradeSystemVM(ScaleSystemVMCmd cmd) throws ResourceUnavailableException, ManagementServerException, VirtualMachineMigrationException,
        ConcurrentOperationException {

        VMInstanceVO vmInstance = _vmInstanceDao.findById(cmd.getId());
        if (vmInstance.getHypervisorType() == HypervisorType.XenServer && vmInstance.getState().equals(State.Running)) {
            throw new InvalidParameterValueException("Dynamic Scaling operation is not permitted for this hypervisor on system vm");
        }
        boolean result = _userVmMgr.upgradeVirtualMachine(cmd.getId(), cmd.getServiceOfferingId(), cmd.getCustomParameters());
        if(result){
            VirtualMachine vm = _vmInstanceDao.findById(cmd.getId());
            return vm;
        } else {
            throw new CloudRuntimeException("Failed to upgrade System VM");
        }
    }

    @Override
    public VirtualMachine upgradeSystemVM(UpgradeSystemVMCmd cmd) {
        Long systemVmId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        return upgradeStoppedSystemVm(systemVmId, serviceOfferingId, cmd.getCustomParameters());

    }

    private VirtualMachine upgradeStoppedSystemVm(Long systemVmId, Long serviceOfferingId, Map<String, String> customparameters){
        Account caller = CallContext.current().getCallingAccount();

        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(systemVmId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            throw new InvalidParameterValueException("Unable to find SystemVm with id " + systemVmId);
        }

        _accountMgr.checkAccess(caller, null, true, systemVm);

        // Check that the specified service offering ID is valid
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(serviceOfferingId);
        ServiceOfferingVO currentServiceOffering = _offeringDao.findById(systemVmId,systemVm.getServiceOfferingId());
        if (newServiceOffering.isDynamic()){
            newServiceOffering.setDynamicFlag(true);
            _userVmMgr.validateCustomParameters(newServiceOffering, customparameters);
            newServiceOffering = _offeringDao.getcomputeOffering(newServiceOffering, customparameters);
        }
        _itMgr.checkIfCanUpgrade(systemVm, newServiceOffering);

        boolean result = _itMgr.upgradeVmDb(systemVmId, serviceOfferingId);

        if (newServiceOffering.isDynamic()) {
            //save the custom values to the database.
            _userVmMgr.saveCustomOfferingDetails(systemVmId, newServiceOffering);
        }
        if (currentServiceOffering.isDynamic() && !newServiceOffering.isDynamic()) {
            _userVmMgr.removeCustomOfferingDetails(systemVmId);
        }

        if (result) {
            return _vmInstanceDao.findById(systemVmId);
        } else {
            throw new CloudRuntimeException("Unable to upgrade system vm " + systemVm);
        }

    }

    private void enableAdminUser(String password) {
        String encodedPassword = null;

        UserVO adminUser = _userDao.getUser(2);
        if (adminUser.getState() == Account.State.disabled) {
            // This means its a new account, set the password using the
            // authenticator

            for (UserAuthenticator authenticator : _userPasswordEncoders) {
                encodedPassword = authenticator.encode(password);
                if (encodedPassword != null) {
                    break;
                }
            }

            adminUser.setPassword(encodedPassword);
            adminUser.setState(Account.State.enabled);
            _userDao.persist(adminUser);
            s_logger.info("Admin user enabled");
        }

    }

    @Override
    public List<String> listDeploymentPlanners() {
        List<String> plannersAvailable = new ArrayList<String>();
        for (DeploymentPlanner planner : _planners) {
            plannersAvailable.add(planner.getName());
        }

        return plannersAvailable;
    }

    @Override
    public void cleanupVMReservations() {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Processing cleanupVMReservations");
        }

        _dpMgr.cleanupVMReservations();
    }

    public List<StoragePoolAllocator> getStoragePoolAllocators() {
        return _storagePoolAllocators;
    }

    @Inject
    public void setStoragePoolAllocators(List<StoragePoolAllocator> storagePoolAllocators) {
        _storagePoolAllocators = storagePoolAllocators;
    }

    public LockMasterListener getLockMasterListener() {
        return _lockMasterListener;
    }

    public void setLockMasterListener(LockMasterListener lockMasterListener) {
        _lockMasterListener = lockMasterListener;
    }
}
