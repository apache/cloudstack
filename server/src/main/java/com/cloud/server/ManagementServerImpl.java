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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.affinity.AffinityGroupProcessor;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.account.CreateAccountCmd;
import org.apache.cloudstack.api.command.admin.account.DeleteAccountCmd;
import org.apache.cloudstack.api.command.admin.account.DisableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.EnableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.ListAccountsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.account.LockAccountCmd;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.address.AcquirePodIpCmdByAdmin;
import org.apache.cloudstack.api.command.admin.address.AssociateIPAddrCmdByAdmin;
import org.apache.cloudstack.api.command.admin.address.ListPublicIpAddressesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.address.ReleasePodIpCmdByAdmin;
import org.apache.cloudstack.api.command.admin.affinitygroup.UpdateVMAffinityGroupCmdByAdmin;
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
import org.apache.cloudstack.api.command.admin.direct.download.RevokeTemplateDirectDownloadCertificateCmd;
import org.apache.cloudstack.api.command.admin.direct.download.UploadTemplateDirectDownloadCertificateCmd;
import org.apache.cloudstack.api.command.admin.domain.CreateDomainCmd;
import org.apache.cloudstack.api.command.admin.domain.DeleteDomainCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainChildrenCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.guest.AddGuestOsCmd;
import org.apache.cloudstack.api.command.admin.guest.AddGuestOsMappingCmd;
import org.apache.cloudstack.api.command.admin.guest.ListGuestOsMappingCmd;
import org.apache.cloudstack.api.command.admin.guest.RemoveGuestOsCmd;
import org.apache.cloudstack.api.command.admin.guest.RemoveGuestOsMappingCmd;
import org.apache.cloudstack.api.command.admin.guest.UpdateGuestOsCmd;
import org.apache.cloudstack.api.command.admin.guest.UpdateGuestOsMappingCmd;
import org.apache.cloudstack.api.command.admin.host.AddHostCmd;
import org.apache.cloudstack.api.command.admin.host.AddSecondaryStorageCmd;
import org.apache.cloudstack.api.command.admin.host.CancelMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.host.DeleteHostCmd;
import org.apache.cloudstack.api.command.admin.host.FindHostsForMigrationCmd;
import org.apache.cloudstack.api.command.admin.host.ListHostTagsCmd;
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
import org.apache.cloudstack.api.command.admin.iso.AttachIsoCmdByAdmin;
import org.apache.cloudstack.api.command.admin.iso.CopyIsoCmdByAdmin;
import org.apache.cloudstack.api.command.admin.iso.DetachIsoCmdByAdmin;
import org.apache.cloudstack.api.command.admin.iso.ListIsoPermissionsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.iso.ListIsosCmdByAdmin;
import org.apache.cloudstack.api.command.admin.iso.RegisterIsoCmdByAdmin;
import org.apache.cloudstack.api.command.admin.loadbalancer.ListLoadBalancerRuleInstancesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.management.ListMgmtsCmd;
import org.apache.cloudstack.api.command.admin.network.AddNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.AddNetworkServiceProviderCmd;
import org.apache.cloudstack.api.command.admin.network.CreateManagementNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkCmdByAdmin;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.CreatePhysicalNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.CreateStorageNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteManagementNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkServiceProviderCmd;
import org.apache.cloudstack.api.command.admin.network.DeletePhysicalNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteStorageNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListDedicatedGuestVlanRangesCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkIsolationMethodsCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkServiceProvidersCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworksCmdByAdmin;
import org.apache.cloudstack.api.command.admin.network.ListPhysicalNetworksCmd;
import org.apache.cloudstack.api.command.admin.network.ListStorageNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListSupportedNetworkServicesCmd;
import org.apache.cloudstack.api.command.admin.network.MigrateNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.MigrateVPCCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateNetworkCmdByAdmin;
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
import org.apache.cloudstack.api.command.admin.outofbandmanagement.ChangeOutOfBandManagementPasswordCmd;
import org.apache.cloudstack.api.command.admin.outofbandmanagement.ConfigureOutOfBandManagementCmd;
import org.apache.cloudstack.api.command.admin.outofbandmanagement.DisableOutOfBandManagementForClusterCmd;
import org.apache.cloudstack.api.command.admin.outofbandmanagement.DisableOutOfBandManagementForHostCmd;
import org.apache.cloudstack.api.command.admin.outofbandmanagement.DisableOutOfBandManagementForZoneCmd;
import org.apache.cloudstack.api.command.admin.outofbandmanagement.EnableOutOfBandManagementForClusterCmd;
import org.apache.cloudstack.api.command.admin.outofbandmanagement.EnableOutOfBandManagementForHostCmd;
import org.apache.cloudstack.api.command.admin.outofbandmanagement.EnableOutOfBandManagementForZoneCmd;
import org.apache.cloudstack.api.command.admin.outofbandmanagement.IssueOutOfBandManagementPowerActionCmd;
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
import org.apache.cloudstack.api.command.admin.storage.AddImageStoreS3CMD;
import org.apache.cloudstack.api.command.admin.storage.CancelPrimaryStorageMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateStoragePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.DeletePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.FindStoragePoolsForMigrationCmd;
import org.apache.cloudstack.api.command.admin.storage.ListImageStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListSecondaryStagingStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStorageProvidersCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStorageTagsCmd;
import org.apache.cloudstack.api.command.admin.storage.PreparePrimaryStorageForMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.storage.UpdateCloudToUseObjectStoreCmd;
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
import org.apache.cloudstack.api.command.admin.template.CopyTemplateCmdByAdmin;
import org.apache.cloudstack.api.command.admin.template.CreateTemplateCmdByAdmin;
import org.apache.cloudstack.api.command.admin.template.ListTemplatePermissionsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.template.ListTemplatesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.template.PrepareTemplateCmd;
import org.apache.cloudstack.api.command.admin.template.RegisterTemplateCmdByAdmin;
import org.apache.cloudstack.api.command.admin.usage.AddTrafficMonitorCmd;
import org.apache.cloudstack.api.command.admin.usage.AddTrafficTypeCmd;
import org.apache.cloudstack.api.command.admin.usage.DeleteTrafficMonitorCmd;
import org.apache.cloudstack.api.command.admin.usage.DeleteTrafficTypeCmd;
import org.apache.cloudstack.api.command.admin.usage.GenerateUsageRecordsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficMonitorsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypeImplementorsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypesCmd;
import org.apache.cloudstack.api.command.admin.usage.ListUsageRecordsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListUsageTypesCmd;
import org.apache.cloudstack.api.command.admin.usage.RemoveRawUsageRecordsCmd;
import org.apache.cloudstack.api.command.admin.usage.UpdateTrafficTypeCmd;
import org.apache.cloudstack.api.command.admin.user.CreateUserCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.DisableUserCmd;
import org.apache.cloudstack.api.command.admin.user.EnableUserCmd;
import org.apache.cloudstack.api.command.admin.user.GetUserCmd;
import org.apache.cloudstack.api.command.admin.user.GetUserKeysCmd;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.command.admin.user.LockUserCmd;
import org.apache.cloudstack.api.command.admin.user.MoveUserCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.command.admin.vlan.CreateVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DedicatePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DeleteVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.ListVlanIpRangesCmd;
import org.apache.cloudstack.api.command.admin.vlan.ReleasePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vm.AddNicToVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.admin.vm.DeployVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.DestroyVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.ExpungeVMCmd;
import org.apache.cloudstack.api.command.admin.vm.GetVMUserDataCmd;
import org.apache.cloudstack.api.command.admin.vm.ListVMsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.MigrateVMCmd;
import org.apache.cloudstack.api.command.admin.vm.MigrateVirtualMachineWithVolumeCmd;
import org.apache.cloudstack.api.command.admin.vm.RebootVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.RecoverVMCmd;
import org.apache.cloudstack.api.command.admin.vm.RemoveNicFromVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.ResetVMPasswordCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.ResetVMSSHKeyCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.RestoreVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.ScaleVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.StartVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.StopVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.UpdateDefaultNicForVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.UpdateVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.UpgradeVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vmsnapshot.RevertToVMSnapshotCmdByAdmin;
import org.apache.cloudstack.api.command.admin.volume.AttachVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.admin.volume.CreateVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.admin.volume.DetachVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.admin.volume.ListVolumesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.volume.MigrateVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.admin.volume.ResizeVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.admin.volume.UpdateVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.admin.volume.UploadVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vpc.CreatePrivateGatewayCmd;
import org.apache.cloudstack.api.command.admin.vpc.CreateVPCCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vpc.CreateVPCOfferingCmd;
import org.apache.cloudstack.api.command.admin.vpc.DeletePrivateGatewayCmd;
import org.apache.cloudstack.api.command.admin.vpc.DeleteVPCOfferingCmd;
import org.apache.cloudstack.api.command.admin.vpc.ListVPCsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vpc.UpdateVPCCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vpc.UpdateVPCOfferingCmd;
import org.apache.cloudstack.api.command.admin.zone.CreateZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.DeleteZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.ListZonesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.zone.MarkDefaultZoneForAccountCmd;
import org.apache.cloudstack.api.command.admin.zone.UpdateZoneCmd;
import org.apache.cloudstack.api.command.user.account.AddAccountToProjectCmd;
import org.apache.cloudstack.api.command.user.account.DeleteAccountFromProjectCmd;
import org.apache.cloudstack.api.command.user.account.ListAccountsCmd;
import org.apache.cloudstack.api.command.user.account.ListProjectAccountsCmd;
import org.apache.cloudstack.api.command.user.address.AssociateIPAddrCmd;
import org.apache.cloudstack.api.command.user.address.DisassociateIPAddrCmd;
import org.apache.cloudstack.api.command.user.address.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.command.user.address.UpdateIPAddrCmd;
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
import org.apache.cloudstack.api.command.user.firewall.UpdateEgressFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.UpdateFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.UpdatePortForwardingRuleCmd;
import org.apache.cloudstack.api.command.user.guest.ListGuestOsCategoriesCmd;
import org.apache.cloudstack.api.command.user.guest.ListGuestOsCmd;
import org.apache.cloudstack.api.command.user.iso.AttachIsoCmd;
import org.apache.cloudstack.api.command.user.iso.CopyIsoCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.DetachIsoCmd;
import org.apache.cloudstack.api.command.user.iso.ExtractIsoCmd;
import org.apache.cloudstack.api.command.user.iso.GetUploadParamsForIsoCmd;
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
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateApplicationLoadBalancerCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLBHealthCheckPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLBStickinessPolicyCmd;
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
import org.apache.cloudstack.api.command.user.network.MoveNetworkAclItemCmd;
import org.apache.cloudstack.api.command.user.network.ReplaceNetworkACLListCmd;
import org.apache.cloudstack.api.command.user.network.RestartNetworkCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLItemCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLListCmd;
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
import org.apache.cloudstack.api.command.user.resource.ListDetailOptionsCmd;
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
import org.apache.cloudstack.api.command.user.snapshot.ArchiveSnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotFromVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotPolicyCmd;
import org.apache.cloudstack.api.command.user.snapshot.DeleteSnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.DeleteSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotsCmd;
import org.apache.cloudstack.api.command.user.snapshot.RevertSnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.UpdateSnapshotPolicyCmd;
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
import org.apache.cloudstack.api.command.user.template.GetUploadParamsForTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ListTemplateOVFProperties;
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
import org.apache.cloudstack.api.command.user.vm.UpdateVmNicIpCmd;
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
import org.apache.cloudstack.api.command.user.volume.GetUploadParamsForVolumeCmd;
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
import org.apache.cloudstack.api.command.user.vpn.UpdateRemoteAccessVpnCmd;
import org.apache.cloudstack.api.command.user.vpn.UpdateVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.UpdateVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.UpdateVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesCmd;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.framework.security.keystore.KeystoreManager;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.resourcedetail.dao.GuestOsDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

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
import com.cloud.gpu.GPU;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostTagVO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.hypervisor.kvm.dpdk.DpdkHelper;
import com.cloud.info.ConsoleProxyInfo;
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
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
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

public class ManagementServerImpl extends ManagerBase implements ManagementServer, Configurable {
    public static final Logger s_logger = Logger.getLogger(ManagementServerImpl.class.getName());

    static final ConfigKey<Integer> vmPasswordLength = new ConfigKey<Integer>("Advanced", Integer.class, "vm.password.length", "6", "Specifies the length of a randomly generated password", false);
    static final ConfigKey<Integer> sshKeyLength = new ConfigKey<Integer>("Advanced", Integer.class, "ssh.key.length", "2048", "Specifies custom SSH key length (bit)", true, ConfigKey.Scope.Global);
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
    private GuestOSHypervisorDao _guestOSHypervisorDao;
    @Inject
    private PrimaryDataStoreDao _poolDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private StorageManager _storageMgr;
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
    protected SSHKeyPairDao _sshKeyPairDao;
    @Inject
    private LoadBalancerDao _loadbalancerDao;
    @Inject
    private HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    private List<HostAllocator> hostAllocators;
    private List<StoragePoolAllocator> _storagePoolAllocators;
    @Inject
    private ResourceTagDao _resourceTagDao;
    @Inject
    private ImageStoreDao _imgStoreDao;
    @Inject
    private ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    private ProjectManager _projectMgr;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private HighAvailabilityManager _haMgr;
    @Inject
    private HostTagsDao _hostTagsDao;
    @Inject
    private ConfigDepot _configDepot;
    @Inject
    private UserVmManager _userVmMgr;
    @Inject
    private AccountService _accountService;
    @Inject
    private ServiceOfferingDao _offeringDao;
    @Inject
    private DeploymentPlanningManager _dpMgr;
    @Inject
    private GuestOsDetailsDao _guestOsDetailsDao;
    @Inject
    private KeystoreManager _ksMgr;
    @Inject
    private DpdkHelper dpdkHelper;

    private LockMasterListener _lockMasterListener;
    private final ScheduledExecutorService _eventExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("EventChecker"));
    private final ScheduledExecutorService _alertExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AlertChecker"));

    private Map<String, String> _configs;

    private Map<String, Boolean> _availableIdsMap;

    private List<UserAuthenticator> _userAuthenticators;
    private List<UserAuthenticator> _userPasswordEncoders;
    protected boolean _executeInSequence;

    protected List<DeploymentPlanner> _planners;

    private final List<HypervisorType> supportedHypervisors = new ArrayList<Hypervisor.HypervisorType>();

    public List<DeploymentPlanner> getPlanners() {
        return _planners;
    }

    public void setPlanners(final List<DeploymentPlanner> planners) {
        _planners = planners;
    }

    @Inject
    ClusterManager _clusterMgr;

    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;

    protected List<AffinityGroupProcessor> _affinityProcessors;

    public List<AffinityGroupProcessor> getAffinityGroupProcessors() {
        return _affinityProcessors;
    }

    public void setAffinityGroupProcessors(final List<AffinityGroupProcessor> affinityProcessors) {
        _affinityProcessors = affinityProcessors;
    }

    public ManagementServerImpl() {
        setRunLevel(ComponentLifecycle.RUN_LEVEL_APPLICATION_MAINLOOP);
    }

    public List<UserAuthenticator> getUserAuthenticators() {
        return _userAuthenticators;
    }

    public void setUserAuthenticators(final List<UserAuthenticator> authenticators) {
        _userAuthenticators = authenticators;
    }

    public List<UserAuthenticator> getUserPasswordEncoders() {
        return _userPasswordEncoders;
    }

    public void setUserPasswordEncoders(final List<UserAuthenticator> encoders) {
        _userPasswordEncoders = encoders;
    }

    public List<HostAllocator> getHostAllocators() {
        return hostAllocators;
    }

    public void setHostAllocators(final List<HostAllocator> hostAllocators) {
        this.hostAllocators = hostAllocators;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {

        _configs = _configDao.getConfiguration();

        final String value = _configs.get("event.purge.interval");
        final int cleanup = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 day.

        _purgeDelay = NumbersUtil.parseInt(_configs.get("event.purge.delay"), 0);
        if (_purgeDelay != 0) {
            _eventExecutor.scheduleAtFixedRate(new EventPurgeTask(), cleanup, cleanup, TimeUnit.SECONDS);
        }

        //Alerts purge configurations
        final int alertPurgeInterval = NumbersUtil.parseInt(_configDao.getValue(Config.AlertPurgeInterval.key()), 60 * 60 * 24); // 1 day.
        _alertPurgeDelay = NumbersUtil.parseInt(_configDao.getValue(Config.AlertPurgeDelay.key()), 0);
        if (_alertPurgeDelay != 0) {
            _alertExecutor.scheduleAtFixedRate(new AlertPurgeTask(), alertPurgeInterval, alertPurgeInterval, TimeUnit.SECONDS);
        }

        final String[] availableIds = TimeZone.getAvailableIDs();
        _availableIdsMap = new HashMap<String, Boolean>(availableIds.length);
        for (final String id : availableIds) {
            _availableIdsMap.put(id, true);
        }

        supportedHypervisors.add(HypervisorType.KVM);
        supportedHypervisors.add(HypervisorType.XenServer);

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
        final Integer passwordLength = vmPasswordLength.value();
        return PasswordGenerator.generateRandomPassword(passwordLength);
    }

    @Override
    public HostVO getHostBy(final long hostId) {
        return _hostDao.findById(hostId);
    }

    @Override
    public DetailVO findDetail(final long hostId, final String name) {
        return _detailsDao.findDetail(hostId, name);
    }

    @Override
    public long getId() {
        return MacAddress.getMacAddress().toLong();
    }

    protected void checkPortParameters(final String publicPort, final String privatePort, final String privateIp, final String proto) {

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
    public boolean archiveEvents(final ArchiveEventsCmd cmd) {
        final Account caller = getCaller();
        final List<Long> ids = cmd.getIds();
        boolean result = true;
        List<Long> permittedAccountIds = new ArrayList<Long>();

        if (_accountService.isNormalUser(caller.getId()) || caller.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            permittedAccountIds.add(caller.getId());
        } else {
            final DomainVO domain = _domainDao.findById(caller.getDomainId());
            final List<Long> permittedDomainIds = _domainDao.getDomainChildrenIds(domain.getPath());
            permittedAccountIds = _accountDao.getAccountIdsForDomains(permittedDomainIds);
        }

        final List<EventVO> events = _eventDao.listToArchiveOrDeleteEvents(ids, cmd.getType(), cmd.getStartDate(), cmd.getEndDate(), permittedAccountIds);
        final ControlledEntity[] sameOwnerEvents = events.toArray(new ControlledEntity[events.size()]);
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, false, sameOwnerEvents);

        if (ids != null && events.size() < ids.size()) {
            result = false;
            return result;
        }
        _eventDao.archiveEvents(events);
        return result;
    }

    @Override
    public boolean deleteEvents(final DeleteEventsCmd cmd) {
        final Account caller = getCaller();
        final List<Long> ids = cmd.getIds();
        boolean result = true;
        List<Long> permittedAccountIds = new ArrayList<Long>();

        if (_accountMgr.isNormalUser(caller.getId()) || caller.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            permittedAccountIds.add(caller.getId());
        } else {
            final DomainVO domain = _domainDao.findById(caller.getDomainId());
            final List<Long> permittedDomainIds = _domainDao.getDomainChildrenIds(domain.getPath());
            permittedAccountIds = _accountDao.getAccountIdsForDomains(permittedDomainIds);
        }

        final List<EventVO> events = _eventDao.listToArchiveOrDeleteEvents(ids, cmd.getType(), cmd.getStartDate(), cmd.getEndDate(), permittedAccountIds);
        final ControlledEntity[] sameOwnerEvents = events.toArray(new ControlledEntity[events.size()]);
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, false, sameOwnerEvents);

        if (ids != null && events.size() < ids.size()) {
            result = false;
            return result;
        }
        for (final EventVO event : events) {
            _eventDao.remove(event.getId());
        }
        return result;
    }

    @Override
    public List<? extends Cluster> searchForClusters(long zoneId, final Long startIndex, final Long pageSizeVal, final String hypervisorType) {
        final Filter searchFilter = new Filter(ClusterVO.class, "id", true, startIndex, pageSizeVal);
        final SearchCriteria<ClusterVO> sc = _clusterDao.createSearchCriteria();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);

        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisorType);

        return _clusterDao.search(sc, searchFilter);
    }

    @Override
    public Pair<List<? extends Cluster>, Integer> searchForClusters(final ListClustersCmd cmd) {
        final Object id = cmd.getId();
        final Object name = cmd.getClusterName();
        final Object podId = cmd.getPodId();
        Long zoneId = cmd.getZoneId();
        final Object hypervisorType = cmd.getHypervisorType();
        final Object clusterType = cmd.getClusterType();
        final Object allocationState = cmd.getAllocationState();
        final String keyword = cmd.getKeyword();
        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);

        final Filter searchFilter = new Filter(ClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        final SearchBuilder<ClusterVO> sb = _clusterDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("clusterType", sb.entity().getClusterType(), SearchCriteria.Op.EQ);
        sb.and("allocationState", sb.entity().getAllocationState(), SearchCriteria.Op.EQ);

        final SearchCriteria<ClusterVO> sc = sb.create();
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
            final SearchCriteria<ClusterVO> ssc = _clusterDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("hypervisorType", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        final Pair<List<ClusterVO>, Integer> result = _clusterDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Cluster>, Integer>(result.first(), result.second());
    }

    private HypervisorType getHypervisorType(VMInstanceVO vm, StoragePool srcVolumePool, VirtualMachineProfile profile) {
        HypervisorType type = null;
        if (vm == null) {
            StoragePoolVO poolVo = _poolDao.findById(srcVolumePool.getId());
            if (ScopeType.CLUSTER.equals(poolVo.getScope())) {
                Long clusterId = poolVo.getClusterId();
                if (clusterId != null) {
                    ClusterVO cluster = _clusterDao.findById(clusterId);
                    type = cluster.getHypervisorType();
                }
            } else if (ScopeType.ZONE.equals(poolVo.getScope())) {
                Long zoneId = poolVo.getDataCenterId();
                if (zoneId != null) {
                    DataCenterVO dc = _dcDao.findById(zoneId);
                }
            }

            if (null == type) {
                type = srcVolumePool.getHypervisor();
            }
        } else {
            type = profile.getHypervisorType();
        }
        return type;
    }

    @Override
    public Pair<List<? extends Host>, Integer> searchForServers(final ListHostsCmd cmd) {

        final Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        final Object name = cmd.getHostName();
        final Object type = cmd.getType();
        final Object state = cmd.getState();
        final Object pod = cmd.getPodId();
        final Object cluster = cmd.getClusterId();
        final Object id = cmd.getId();
        final Object keyword = cmd.getKeyword();
        final Object resourceState = cmd.getResourceState();
        final Object haHosts = cmd.getHaHost();

        final Pair<List<HostVO>, Integer> result = searchForServers(cmd.getStartIndex(), cmd.getPageSizeVal(), name, type, state, zoneId, pod, cluster, id, keyword, resourceState, haHosts, null,
                null);
        return new Pair<List<? extends Host>, Integer>(result.first(), result.second());
    }

    @Override
    public Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> listHostsForMigrationOfVM(final Long vmId, final Long startIndex, final Long pageSize,
            final String keyword) {
        final Account caller = getCaller();
        if (!_accountMgr.isRootAdmin(caller.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException("No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find the VM with given id");
            throw ex;
        }

        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not running, cannot migrate the vm" + vm);
            }
            final InvalidParameterValueException ex = new InvalidParameterValueException("VM is not Running, cannot " + "migrate the vm with specified id");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        if (_serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()) != null) {
            s_logger.info(" Live Migration of GPU enabled VM : " + vm.getInstanceName() + " is not supported");
            // Return empty list.
            return new Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>>(new Pair<List<? extends Host>, Integer>(new ArrayList<HostVO>(), new Integer(0)),
                    new ArrayList<Host>(), new HashMap<Host, Boolean>());
        }

        if (!vm.getHypervisorType().equals(HypervisorType.XenServer) && !vm.getHypervisorType().equals(HypervisorType.VMware) && !vm.getHypervisorType().equals(HypervisorType.KVM)
                && !vm.getHypervisorType().equals(HypervisorType.Ovm) && !vm.getHypervisorType().equals(HypervisorType.Hyperv) && !vm.getHypervisorType().equals(HypervisorType.LXC)
                && !vm.getHypervisorType().equals(HypervisorType.Simulator) && !vm.getHypervisorType().equals(HypervisorType.Ovm3)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm + " is not XenServer/VMware/KVM/Ovm/Hyperv/Ovm3, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for VM migration, we support " + "XenServer/VMware/KVM/Ovm/Hyperv/Ovm3 only");
        }

        if (vm.getType().equals(VirtualMachine.Type.User) && vm.getHypervisorType().equals(HypervisorType.LXC)) {
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for User VM migration, we support XenServer/VMware/KVM/Ovm/Hyperv/Ovm3 only");
        }

        final long srcHostId = vm.getHostId();
        final Host srcHost = _hostDao.findById(srcHostId);
        if (srcHost == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find the host with id: " + srcHostId + " of this VM:" + vm);
            }
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find the host (with specified id) of VM with specified id");
            ex.addProxyObject(String.valueOf(srcHostId), "hostId");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        // Check if the vm can be migrated with storage.
        boolean canMigrateWithStorage = false;

        if (vm.getType() == VirtualMachine.Type.User) {
            final HypervisorCapabilitiesVO capabilities = _hypervisorCapabilitiesDao.findByHypervisorTypeAndVersion(srcHost.getHypervisorType(), srcHost.getHypervisorVersion());
            if (capabilities != null) {
                canMigrateWithStorage = capabilities.isStorageMotionSupported();
            }
        }

        // Check if the vm is using any disks on local storage.
        final VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vm, null, _offeringDao.findById(vm.getId(), vm.getServiceOfferingId()), null, null);
        final List<VolumeVO> volumes = _volumeDao.findCreatedByInstance(vmProfile.getId());
        boolean usesLocal = false;
        for (final VolumeVO volume : volumes) {
            final DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
            final DiskProfile diskProfile = new DiskProfile(volume, diskOffering, vmProfile.getHypervisorType());
            if (diskProfile.useLocalStorage()) {
                usesLocal = true;
                break;
            }
        }

        if (!canMigrateWithStorage && usesLocal) {
            throw new InvalidParameterValueException("Unsupported operation, VM uses Local storage, cannot migrate");
        }

        final Type hostType = srcHost.getType();
        Pair<List<HostVO>, Integer> allHostsPair = null;
        List<HostVO> allHosts = null;
        final Map<Host, Boolean> requiresStorageMotion = new HashMap<Host, Boolean>();
        DataCenterDeployment plan = null;
        if (canMigrateWithStorage) {
            allHostsPair = searchForServers(startIndex, pageSize, null, hostType, null, srcHost.getDataCenterId(), null, null, null, keyword, null, null, srcHost.getHypervisorType(),
                    srcHost.getHypervisorVersion());
            allHosts = allHostsPair.first();
            allHosts.remove(srcHost);

            for (final VolumeVO volume : volumes) {
                StoragePool storagePool = _poolDao.findById(volume.getPoolId());
                Long volClusterId = storagePool.getClusterId();

                for (Iterator<HostVO> iterator = allHosts.iterator(); iterator.hasNext();) {
                    final Host host = iterator.next();

                    if (volClusterId != null) {
                        if (storagePool.isLocal() || !host.getClusterId().equals(volClusterId) || usesLocal) {
                            if (storagePool.isManaged()) {
                                // At the time being, we do not support storage migration of a volume from managed storage unless the managed storage
                                // is at the zone level and the source and target storage pool is the same.
                                // If the source and target storage pool is the same and it is managed, then we still have to perform a storage migration
                                // because we need to create a new target volume and copy the contents of the source volume into it before deleting the
                                // source volume.
                                iterator.remove();
                            } else {
                                if (hasSuitablePoolsForVolume(volume, host, vmProfile)) {
                                    requiresStorageMotion.put(host, true);
                                } else {
                                    iterator.remove();
                                }
                            }
                        }
                    } else {
                        if (storagePool.isManaged()) {
                            if (srcHost.getClusterId() != host.getClusterId()) {
                                // If the volume's storage pool is managed and at the zone level, then we still have to perform a storage migration
                                // because we need to create a new target volume and copy the contents of the source volume into it before deleting
                                // the source volume.
                                requiresStorageMotion.put(host, true);
                            }
                        }
                    }
                }
            }

            plan = new DataCenterDeployment(srcHost.getDataCenterId(), null, null, null, null, null);
        } else {
            final Long cluster = srcHost.getClusterId();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Searching for all hosts in cluster " + cluster + " for migrating VM " + vm);
            }
            allHostsPair = searchForServers(startIndex, pageSize, null, hostType, null, null, null, cluster, null, keyword, null, null, null, null);
            // Filter out the current host.
            allHosts = allHostsPair.first();
            allHosts.remove(srcHost);
            plan = new DataCenterDeployment(srcHost.getDataCenterId(), srcHost.getPodId(), srcHost.getClusterId(), null, null, null);
        }

        final Pair<List<? extends Host>, Integer> otherHosts = new Pair<List<? extends Host>, Integer>(allHosts, new Integer(allHosts.size()));
        List<Host> suitableHosts = new ArrayList<Host>();
        final ExcludeList excludes = new ExcludeList();
        excludes.addHost(srcHostId);

        if (dpdkHelper.isVMDpdkEnabled(vm.getId())) {
            excludeNonDPDKEnabledHosts(plan, excludes);
        }

        // call affinitygroup chain
        final long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        if (vmGroupCount > 0) {
            for (final AffinityGroupProcessor processor : _affinityProcessors) {
                processor.process(vmProfile, plan, excludes);
            }
        }

        for (final HostAllocator allocator : hostAllocators) {
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

    /**
     * Add non DPDK enabled hosts to the avoid list
     */
    private void excludeNonDPDKEnabledHosts(DataCenterDeployment plan, ExcludeList excludes) {
        long dataCenterId = plan.getDataCenterId();
        Long clusterId = plan.getClusterId();
        Long podId = plan.getPodId();
        List<HostVO> hosts = _hostDao.listAllUpAndEnabledNonHAHosts(Type.Routing, clusterId, podId, dataCenterId, null);
        for (HostVO host : hosts) {
            if (!dpdkHelper.isHostDpdkEnabled(host.getId())) {
                excludes.addHost(host.getId());
            }
        }
    }

    private boolean hasSuitablePoolsForVolume(final VolumeVO volume, final Host host, final VirtualMachineProfile vmProfile) {
        final DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        final DiskProfile diskProfile = new DiskProfile(volume, diskOffering, vmProfile.getHypervisorType());
        final DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), host.getId(), null, null);
        final ExcludeList avoid = new ExcludeList();

        for (final StoragePoolAllocator allocator : _storagePoolAllocators) {
            final List<StoragePool> poolList = allocator.allocateToPool(diskProfile, vmProfile, plan, avoid, 1);
            if (poolList != null && !poolList.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Pair<List<? extends StoragePool>, List<? extends StoragePool>> listStoragePoolsForMigrationOfVolume(final Long volumeId) {
        final Account caller = getCaller();
        if (!_accountMgr.isRootAdmin(caller.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the volume");
            }
            throw new PermissionDeniedException("No permission to migrate volume, only root admin can migrate a volume");
        }

        final VolumeVO volume = _volumeDao.findById(volumeId);
        if (volume == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find volume with" + " specified id.");
            ex.addProxyObject(volumeId.toString(), "volumeId");
            throw ex;
        }

        // Volume must be attached to an instance for live migration.
        List<? extends StoragePool> allPools = new ArrayList<StoragePool>();
        List<? extends StoragePool> suitablePools = new ArrayList<StoragePool>();

        // Volume must be in Ready state to be migrated.
        if (!Volume.State.Ready.equals(volume.getState())) {
            s_logger.info("Volume " + volume + " must be in ready state for migration.");
            return new Pair<List<? extends StoragePool>, List<? extends StoragePool>>(allPools, suitablePools);
        }

        final Long instanceId = volume.getInstanceId();
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
            final Long hostId = vm.getHostId();
            if (hostId != null) {
                final HostVO host = _hostDao.findById(hostId);
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

        StoragePool srcVolumePool = _poolDao.findById(volume.getPoolId());
        allPools = getAllStoragePoolCompatileWithVolumeSourceStoragePool(srcVolumePool);
        allPools.remove(srcVolumePool);
        if (vm != null) {
            suitablePools = findAllSuitableStoragePoolsForVm(volume, vm, srcVolumePool);
        } else {
            suitablePools = allPools;
        }

        return new Pair<List<? extends StoragePool>, List<? extends StoragePool>>(allPools, suitablePools);
    }

    /**
     * This method looks for all storage pools that are compatible with the given volume.
     * <ul>
     *  <li>We will look for storage systems that are zone wide.</li>
     *  <li>We also all storage available filtering by data center, pod and cluster as the current storage pool used by the given volume.</li>
     * </ul>
     */
    private List<? extends StoragePool> getAllStoragePoolCompatileWithVolumeSourceStoragePool(StoragePool srcVolumePool) {
        List<StoragePoolVO> storagePools = new ArrayList<>();
        List<StoragePoolVO> zoneWideStoragePools = _poolDao.findZoneWideStoragePoolsByTags(srcVolumePool.getDataCenterId(), null);
        if (CollectionUtils.isNotEmpty(zoneWideStoragePools)) {
            storagePools.addAll(zoneWideStoragePools);
        }
        List<StoragePoolVO> clusterAndLocalStoragePools = _poolDao.listBy(srcVolumePool.getDataCenterId(), srcVolumePool.getPodId(), srcVolumePool.getClusterId(), null);
        if (CollectionUtils.isNotEmpty(clusterAndLocalStoragePools)) {
            storagePools.addAll(clusterAndLocalStoragePools);
        }
        return storagePools;
    }

    /**
     *  Looks for all suitable storage pools to allocate the given volume.
     *  We take into account the service offering of the VM and volume to find suitable storage pools. It is also excluded from the search the current storage pool used by the volume.
     *  We use {@link StoragePoolAllocator} to look for possible storage pools to allocate the given volume. We will look for possible local storage poosl even if the volume is using a shared storage disk offering.
     *
     *  Side note: the idea behind this method is to provide power for administrators of manually overriding deployments defined by CloudStack.
     */
    private List<StoragePool> findAllSuitableStoragePoolsForVm(final VolumeVO volume, VMInstanceVO vm, StoragePool srcVolumePool) {
        List<StoragePool> suitablePools = new ArrayList<>();

        HostVO host = _hostDao.findById(vm.getHostId());
        if (host == null) {
            host = _hostDao.findById(vm.getLastHostId());
        }

        ExcludeList avoid = new ExcludeList();
        avoid.addPool(srcVolumePool.getId());

        DataCenterDeployment plan = new DataCenterDeployment(volume.getDataCenterId(), srcVolumePool.getPodId(), srcVolumePool.getClusterId(), null, null, null);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        // OfflineVmwareMigration: vm might be null here; deal!
        HypervisorType type = getHypervisorType(vm, srcVolumePool, profile);

        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        //This is an override mechanism so we can list the possible local storage pools that a volume in a shared pool might be able to be migrated to
        DiskProfile diskProfile = new DiskProfile(volume, diskOffering, type);
        diskProfile.setUseLocalStorage(true);

        for (StoragePoolAllocator allocator : _storagePoolAllocators) {
            List<StoragePool> pools = allocator.allocateToPool(diskProfile, profile, plan, avoid, StoragePoolAllocator.RETURN_UPTO_ALL);
            if (CollectionUtils.isEmpty(pools)) {
                continue;
            }
            for (StoragePool pool : pools) {
                boolean isLocalPoolSameHostAsSourcePool = pool.isLocal() && StringUtils.equals(host.getPrivateIpAddress(), pool.getHostAddress());
                if (isLocalPoolSameHostAsSourcePool || pool.isShared()) {
                    suitablePools.add(pool);
                }

            }
        }
        return suitablePools;
    }

    private Pair<List<HostVO>, Integer> searchForServers(final Long startIndex, final Long pageSize, final Object name, final Object type, final Object state, final Object zone, final Object pod,
            final Object cluster, final Object id, final Object keyword, final Object resourceState, final Object haHosts, final Object hypervisorType, final Object hypervisorVersion) {
        final Filter searchFilter = new Filter(HostVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        final SearchBuilder<HostVO> sb = _hostDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.LIKE);
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("resourceState", sb.entity().getResourceState(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("hypervisorVersion", sb.entity().getHypervisorVersion(), SearchCriteria.Op.GTEQ);

        final String haTag = _haMgr.getHaTag();
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

        final SearchCriteria<HostVO> sc = sb.create();

        if (keyword != null) {
            final SearchCriteria<HostVO> ssc = _hostDao.createSearchCriteria();
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
    public Pair<List<? extends Pod>, Integer> searchForPods(final ListPodsByCmd cmd) {
        final String podName = cmd.getPodName();
        final Long id = cmd.getId();
        Long zoneId = cmd.getZoneId();
        final Object keyword = cmd.getKeyword();
        final Object allocationState = cmd.getAllocationState();
        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);

        final Filter searchFilter = new Filter(HostPodVO.class, "dataCenterId", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchBuilder<HostPodVO> sb = _hostPodDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("allocationState", sb.entity().getAllocationState(), SearchCriteria.Op.EQ);

        final SearchCriteria<HostPodVO> sc = sb.create();
        if (keyword != null) {
            final SearchCriteria<HostPodVO> ssc = _hostPodDao.createSearchCriteria();
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

        final Pair<List<HostPodVO>, Integer> result = _hostPodDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Pod>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Vlan>, Integer> searchForVlans(final ListVlanIpRangesCmd cmd) {
        // If an account name and domain ID are specified, look up the account
        final String accountName = cmd.getAccountName();
        final Long domainId = cmd.getDomainId();
        Long accountId = null;
        final Long networkId = cmd.getNetworkId();
        final Boolean forVirtual = cmd.isForVirtualNetwork();
        String vlanType = null;
        final Long projectId = cmd.getProjectId();
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();

        if (accountName != null && domainId != null) {
            if (projectId != null) {
                throw new InvalidParameterValueException("Account and projectId can't be specified together");
            }
            final Account account = _accountDao.findActiveAccount(accountName, domainId);
            if (account == null) {
                final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find account " + accountName + " in specified domain");
                // Since we don't have a DomainVO object here, we directly set
                // tablename to "domain".
                final DomainVO domain = ApiDBUtils.findDomainById(domainId);
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
            final Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project by id " + projectId);
                ex.addProxyObject(projectId.toString(), "projectId");
                throw ex;
            }
            accountId = project.getProjectAccountId();
        }

        final Filter searchFilter = new Filter(VlanVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        final Object id = cmd.getId();
        final Object vlan = cmd.getVlan();
        final Object dataCenterId = cmd.getZoneId();
        final Object podId = cmd.getPodId();
        final Object keyword = cmd.getKeyword();

        final SearchBuilder<VlanVO> sb = _vlanDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vlan", sb.entity().getVlanTag(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("vlan", sb.entity().getVlanTag(), SearchCriteria.Op.EQ);
        sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        sb.and("vlanType", sb.entity().getVlanType(), SearchCriteria.Op.EQ);
        sb.and("physicalNetworkId", sb.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);

        if (accountId != null) {
            final SearchBuilder<AccountVlanMapVO> accountVlanMapSearch = _accountVlanMapDao.createSearchBuilder();
            accountVlanMapSearch.and("accountId", accountVlanMapSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
            sb.join("accountVlanMapSearch", accountVlanMapSearch, sb.entity().getId(), accountVlanMapSearch.entity().getVlanDbId(), JoinBuilder.JoinType.INNER);
        }

        if (podId != null) {
            final SearchBuilder<PodVlanMapVO> podVlanMapSearch = _podVlanMapDao.createSearchBuilder();
            podVlanMapSearch.and("podId", podVlanMapSearch.entity().getPodId(), SearchCriteria.Op.EQ);
            sb.join("podVlanMapSearch", podVlanMapSearch, sb.entity().getId(), podVlanMapSearch.entity().getVlanDbId(), JoinBuilder.JoinType.INNER);
        }

        final SearchCriteria<VlanVO> sc = sb.create();
        if (keyword != null) {
            final SearchCriteria<VlanVO> ssc = _vlanDao.createSearchCriteria();
            ssc.addOr("vlanTag", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("ipRange", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("vlanTag", SearchCriteria.Op.SC, ssc);
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

        final Pair<List<VlanVO>, Integer> result = _vlanDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Vlan>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Configuration>, Integer> searchForConfigurations(final ListCfgsByCmd cmd) {
        final Filter searchFilter = new Filter(ConfigurationVO.class, "name", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchCriteria<ConfigurationVO> sc = _configDao.createSearchCriteria();

        final Object name = cmd.getConfigName();
        final Object category = cmd.getCategory();
        final Object keyword = cmd.getKeyword();
        final Long zoneId = cmd.getZoneId();
        final Long clusterId = cmd.getClusterId();
        final Long storagepoolId = cmd.getStoragepoolId();
        final Long accountId = cmd.getAccountId();
        final Long domainId = cmd.getDomainId();
        final Long imageStoreId = cmd.getImageStoreId();
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
        if (domainId != null) {
            scope = ConfigKey.Scope.Domain.toString();
            id = domainId;
            paramCountCheck++;
        }
        if (storagepoolId != null) {
            scope = ConfigKey.Scope.StoragePool.toString();
            id = storagepoolId;
            paramCountCheck++;
        }
        if (imageStoreId != null) {
            scope = ConfigKey.Scope.ImageStore.toString();
            id = imageStoreId;
            paramCountCheck++;
        }

        if (paramCountCheck > 1) {
            throw new InvalidParameterValueException("cannot handle multiple IDs, provide only one ID corresponding to the scope");
        }

        if (keyword != null) {
            final SearchCriteria<ConfigurationVO> ssc = _configDao.createSearchCriteria();
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

        if (scope != null && !scope.isEmpty()) {
            // getting the list of parameters at requested scope
            sc.addAnd("scope", SearchCriteria.Op.EQ, scope);
        }

        final Pair<List<ConfigurationVO>, Integer> result = _configDao.searchAndCount(sc, searchFilter);

        if (scope != null && !scope.isEmpty()) {
            // Populate values corresponding the resource id
            final List<ConfigurationVO> configVOList = new ArrayList<ConfigurationVO>();
            for (final ConfigurationVO param : result.first()) {
                final ConfigurationVO configVo = _configDao.findByName(param.getName());
                if (configVo != null) {
                    final ConfigKey<?> key = _configDepot.get(param.getName());
                    if (key != null) {
                        configVo.setValue(key.valueIn(id) == null ? null : key.valueIn(id).toString());
                        configVOList.add(configVo);
                    } else {
                        s_logger.warn("ConfigDepot could not find parameter " + param.getName() + " for scope " + scope);
                    }
                } else {
                    s_logger.warn("Configuration item  " + param.getName() + " not found in " + scope);
                }
            }

            return new Pair<List<? extends Configuration>, Integer>(configVOList, configVOList.size());
        }

        return new Pair<List<? extends Configuration>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends IpAddress>, Integer> searchForIPAddresses(final ListPublicIpAddressesCmd cmd) {
        final Object keyword = cmd.getKeyword();
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        final Long associatedNetworkId = cmd.getAssociatedNetworkId();
        final Long sourceNetworkId = cmd.getNetworkId();
        final Long zone = cmd.getZoneId();
        final String address = cmd.getIpAddress();
        final Long vlan = cmd.getVlanId();
        final Boolean forVirtualNetwork = cmd.isForVirtualNetwork();
        final Boolean forLoadBalancing = cmd.isForLoadBalancing();
        final Long ipId = cmd.getId();
        final Boolean sourceNat = cmd.isSourceNat();
        final Boolean staticNat = cmd.isStaticNat();
        final Long vpcId = cmd.getVpcId();
        final Boolean forDisplay = cmd.getDisplay();
        final Map<String, String> tags = cmd.getTags();

        final String state = cmd.getState();
        Boolean isAllocated = cmd.isAllocatedOnly();
        if (isAllocated == null) {
            isAllocated = Boolean.TRUE;

            if (state != null) {
                isAllocated = Boolean.FALSE;
            }
        }

        final Filter searchFilter = new Filter(IPAddressVO.class, "address", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchBuilder<IPAddressVO> sb = _publicIpAddressDao.createSearchBuilder();
        Long domainId = null;
        Boolean isRecursive = null;
        final List<Long> permittedAccounts = new ArrayList<Long>();
        ListProjectResourcesCriteria listProjectResourcesCriteria = null;
        if (isAllocated) {
            final Account caller = getCaller();

            final Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(),
                    null);
            _accountMgr.buildACLSearchParameters(caller, cmd.getId(), cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
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
        sb.and("associatedNetworkId", sb.entity().getAssociatedWithNetworkId(), SearchCriteria.Op.EQ);
        sb.and("sourceNetworkId", sb.entity().getSourceNetworkId(), SearchCriteria.Op.EQ);
        sb.and("isSourceNat", sb.entity().isSourceNat(), SearchCriteria.Op.EQ);
        sb.and("isStaticNat", sb.entity().isOneToOneNat(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplay(), SearchCriteria.Op.EQ);

        if (forLoadBalancing != null && forLoadBalancing) {
            final SearchBuilder<LoadBalancerVO> lbSearch = _loadbalancerDao.createSearchBuilder();
            sb.join("lbSearch", lbSearch, sb.entity().getId(), lbSearch.entity().getSourceIpAddressId(), JoinType.INNER);
            sb.groupBy(sb.entity().getId());
        }

        if (keyword != null && address == null) {
            sb.and("addressLIKE", sb.entity().getAddress(), SearchCriteria.Op.LIKE);
        }

        if (tags != null && !tags.isEmpty()) {
            final SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        final SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        vlanSearch.and("vlanType", vlanSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        sb.join("vlanSearch", vlanSearch, sb.entity().getVlanId(), vlanSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        if (isAllocated != null && isAllocated == true) {
            sb.and("allocated", sb.entity().getAllocatedTime(), SearchCriteria.Op.NNULL);
        }

        VlanType vlanType = null;
        if (forVirtualNetwork != null) {
            vlanType = forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;
        } else {
            vlanType = VlanType.VirtualNetwork;
        }

        final SearchCriteria<IPAddressVO> sc = sb.create();
        if (isAllocated) {
            _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        }

        sc.setJoinParameters("vlanSearch", "vlanType", vlanType);

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.PublicIpAddress.toString());
            for (final String key : tags.keySet()) {
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
            sc.setParameters("associatedNetworkId", associatedNetworkId);
        }

        if (sourceNetworkId != null) {
            sc.setParameters("sourceNetworkId", sourceNetworkId);
        }

        if (forDisplay != null) {
            sc.setParameters("display", forDisplay);
        }

        if (state != null) {
            sc.setParameters("state", state);
        }

        final Pair<List<IPAddressVO>, Integer> result = _publicIpAddressDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends IpAddress>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends GuestOS>, Integer> listGuestOSByCriteria(final ListGuestOsCmd cmd) {
        final Filter searchFilter = new Filter(GuestOSVO.class, "displayName", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        final Long id = cmd.getId();
        final Long osCategoryId = cmd.getOsCategoryId();
        final String description = cmd.getDescription();
        final String keyword = cmd.getKeyword();

        final SearchCriteria<GuestOSVO> sc = _guestOSDao.createSearchCriteria();

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

        final Pair<List<GuestOSVO>, Integer> result = _guestOSDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends GuestOS>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends GuestOsCategory>, Integer> listGuestOSCategoriesByCriteria(final ListGuestOsCategoriesCmd cmd) {
        final Filter searchFilter = new Filter(GuestOSCategoryVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        final Long id = cmd.getId();
        final String name = cmd.getName();
        final String keyword = cmd.getKeyword();

        final SearchCriteria<GuestOSCategoryVO> sc = _guestOSCategoryDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (keyword != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        final Pair<List<GuestOSCategoryVO>, Integer> result = _guestOSCategoryDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends GuestOsCategory>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends GuestOSHypervisor>, Integer> listGuestOSMappingByCriteria(final ListGuestOsMappingCmd cmd) {
        final Filter searchFilter = new Filter(GuestOSHypervisorVO.class, "hypervisorType", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        final Long id = cmd.getId();
        final Long osTypeId = cmd.getOsTypeId();
        final String hypervisor = cmd.getHypervisor();
        final String hypervisorVersion = cmd.getHypervisorVersion();

        //throw exception if hypervisor name is not passed, but version is
        if (hypervisorVersion != null && (hypervisor == null || hypervisor.isEmpty())) {
            throw new InvalidParameterValueException("Hypervisor version parameter cannot be used without specifying a hypervisor : XenServer, KVM or VMware");
        }

        final SearchCriteria<GuestOSHypervisorVO> sc = _guestOSHypervisorDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (osTypeId != null) {
            sc.addAnd("guestOsId", SearchCriteria.Op.EQ, osTypeId);
        }

        if (hypervisor != null) {
            sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisor);
        }

        if (hypervisorVersion != null) {
            sc.addAnd("hypervisorVersion", SearchCriteria.Op.EQ, hypervisorVersion);
        }

        final Pair<List<GuestOSHypervisorVO>, Integer> result = _guestOSHypervisorDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends GuestOSHypervisor>, Integer>(result.first(), result.second());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_OS_MAPPING_ADD, eventDescription = "Adding new guest OS to hypervisor name mapping", create = true)
    public GuestOSHypervisor addGuestOsMapping(final AddGuestOsMappingCmd cmd) {
        final Long osTypeId = cmd.getOsTypeId();
        final String osStdName = cmd.getOsStdName();
        final String hypervisor = cmd.getHypervisor();
        final String hypervisorVersion = cmd.getHypervisorVersion();
        final String osNameForHypervisor = cmd.getOsNameForHypervisor();
        GuestOS guestOs = null;

        if (osTypeId == null && (osStdName == null || osStdName.isEmpty())) {
            throw new InvalidParameterValueException("Please specify either a guest OS name or UUID");
        }

        final HypervisorType hypervisorType = HypervisorType.getType(hypervisor);

        if (!(hypervisorType == HypervisorType.KVM || hypervisorType == HypervisorType.XenServer || hypervisorType == HypervisorType.VMware)) {
            throw new InvalidParameterValueException("Please specify a valid hypervisor : XenServer, KVM or VMware");
        }

        final HypervisorCapabilitiesVO hypervisorCapabilities = _hypervisorCapabilitiesDao.findByHypervisorTypeAndVersion(hypervisorType, hypervisorVersion);
        if (hypervisorCapabilities == null) {
            throw new InvalidParameterValueException("Please specify a valid hypervisor and supported version");
        }

        //by this point either osTypeId or osStdType is non-empty. Find by either of them. ID takes preference if both are specified
        if (osTypeId != null) {
            guestOs = ApiDBUtils.findGuestOSById(osTypeId);
        } else if (osStdName != null) {
            guestOs = ApiDBUtils.findGuestOSByDisplayName(osStdName);
        }

        if (guestOs == null) {
            throw new InvalidParameterValueException("Unable to find the guest OS by name or UUID");
        }
        //check for duplicates
        final GuestOSHypervisorVO duplicate = _guestOSHypervisorDao.findByOsIdAndHypervisorAndUserDefined(guestOs.getId(), hypervisorType.toString(), hypervisorVersion, true);

        if (duplicate != null) {
            throw new InvalidParameterValueException(
                    "Mapping from hypervisor : " + hypervisorType.toString() + ", version : " + hypervisorVersion + " and guest OS : " + guestOs.getDisplayName() + " already exists!");
        }
        final GuestOSHypervisorVO guestOsMapping = new GuestOSHypervisorVO();
        guestOsMapping.setGuestOsId(guestOs.getId());
        guestOsMapping.setGuestOsName(osNameForHypervisor);
        guestOsMapping.setHypervisorType(hypervisorType.toString());
        guestOsMapping.setHypervisorVersion(hypervisorVersion);
        guestOsMapping.setIsUserDefined(true);
        return _guestOSHypervisorDao.persist(guestOsMapping);

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_OS_MAPPING_ADD, eventDescription = "Adding a new guest OS to hypervisor name mapping", async = true)
    public GuestOSHypervisor getAddedGuestOsMapping(final Long guestOsMappingId) {
        return getGuestOsHypervisor(guestOsMappingId);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_OS_ADD, eventDescription = "Adding new guest OS type", create = true)
    public GuestOS addGuestOs(final AddGuestOsCmd cmd) {
        final Long categoryId = cmd.getOsCategoryId();
        final String displayName = cmd.getOsDisplayName();
        final String name = cmd.getOsName();

        final GuestOSCategoryVO guestOsCategory = ApiDBUtils.findGuestOsCategoryById(categoryId);
        if (guestOsCategory == null) {
            throw new InvalidParameterValueException("Guest OS category not found. Please specify a valid Guest OS category");
        }

        final GuestOS guestOs = ApiDBUtils.findGuestOSByDisplayName(displayName);
        if (guestOs != null) {
            throw new InvalidParameterValueException("The specified Guest OS name : " + displayName + " already exists. Please specify a unique name");
        }

        s_logger.debug("GuestOSDetails");
        final GuestOSVO guestOsVo = new GuestOSVO();
        guestOsVo.setCategoryId(categoryId.longValue());
        guestOsVo.setDisplayName(displayName);
        guestOsVo.setName(name);
        guestOsVo.setIsUserDefined(true);
        final GuestOS guestOsPersisted = _guestOSDao.persist(guestOsVo);

        if (cmd.getDetails() != null && !cmd.getDetails().isEmpty()) {
            Map<String, String> detailsMap = cmd.getDetails();
            for (Object key : detailsMap.keySet()) {
                _guestOsDetailsDao.addDetail(guestOsPersisted.getId(), (String)key, detailsMap.get(key), false);
            }
        }

        return guestOsPersisted;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_OS_ADD, eventDescription = "Adding a new guest OS type", async = true)
    public GuestOS getAddedGuestOs(final Long guestOsId) {
        return getGuestOs(guestOsId);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_OS_UPDATE, eventDescription = "updating guest OS type", async = true)
    public GuestOS updateGuestOs(final UpdateGuestOsCmd cmd) {
        final Long id = cmd.getId();
        final String displayName = cmd.getOsDisplayName();

        //check if guest OS exists
        final GuestOS guestOsHandle = ApiDBUtils.findGuestOSById(id);
        if (guestOsHandle == null) {
            throw new InvalidParameterValueException("Guest OS not found. Please specify a valid ID for the Guest OS");
        }

        if (!guestOsHandle.isUserDefined()) {
            throw new InvalidParameterValueException("Unable to modify system defined guest OS");
        }

        if (cmd.getDetails() != null && !cmd.getDetails().isEmpty()) {
            Map<String, String> detailsMap = cmd.getDetails();
            for (Object key : detailsMap.keySet()) {
                _guestOsDetailsDao.addDetail(id, (String)key, detailsMap.get(key), false);
            }
        }

        //Check if update is needed
        if (displayName.equals(guestOsHandle.getDisplayName())) {
            return guestOsHandle;
        }

        //Check if another Guest OS by same name exists
        final GuestOS duplicate = ApiDBUtils.findGuestOSByDisplayName(displayName);
        if (duplicate != null) {
            throw new InvalidParameterValueException("The specified Guest OS name : " + displayName + " already exists. Please specify a unique guest OS name");
        }
        final GuestOSVO guestOs = _guestOSDao.createForUpdate(id);
        guestOs.setDisplayName(displayName);
        if (_guestOSDao.update(id, guestOs)) {
            return _guestOSDao.findById(id);
        } else {
            return null;
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_OS_REMOVE, eventDescription = "removing guest OS type", async = true)
    public boolean removeGuestOs(final RemoveGuestOsCmd cmd) {
        final Long id = cmd.getId();

        //check if guest OS exists
        final GuestOS guestOs = ApiDBUtils.findGuestOSById(id);
        if (guestOs == null) {
            throw new InvalidParameterValueException("Guest OS not found. Please specify a valid ID for the Guest OS");
        }

        if (!guestOs.isUserDefined()) {
            throw new InvalidParameterValueException("Unable to remove system defined guest OS");
        }

        return _guestOSDao.remove(id);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_OS_MAPPING_UPDATE, eventDescription = "updating guest OS mapping", async = true)
    public GuestOSHypervisor updateGuestOsMapping(final UpdateGuestOsMappingCmd cmd) {
        final Long id = cmd.getId();
        final String osNameForHypervisor = cmd.getOsNameForHypervisor();

        //check if mapping exists
        final GuestOSHypervisor guestOsHypervisorHandle = _guestOSHypervisorDao.findById(id);
        if (guestOsHypervisorHandle == null) {
            throw new InvalidParameterValueException("Guest OS Mapping not found. Please specify a valid ID for the Guest OS Mapping");
        }

        if (!guestOsHypervisorHandle.getIsUserDefined()) {
            throw new InvalidParameterValueException("Unable to modify system defined Guest OS mapping");
        }

        final GuestOSHypervisorVO guestOsHypervisor = _guestOSHypervisorDao.createForUpdate(id);
        guestOsHypervisor.setGuestOsName(osNameForHypervisor);
        if (_guestOSHypervisorDao.update(id, guestOsHypervisor)) {
            return _guestOSHypervisorDao.findById(id);
        } else {
            return null;
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_OS_MAPPING_REMOVE, eventDescription = "removing guest OS mapping", async = true)
    public boolean removeGuestOsMapping(final RemoveGuestOsMappingCmd cmd) {
        final Long id = cmd.getId();

        //check if mapping exists
        final GuestOSHypervisor guestOsHypervisorHandle = _guestOSHypervisorDao.findById(id);
        if (guestOsHypervisorHandle == null) {
            throw new InvalidParameterValueException("Guest OS Mapping not found. Please specify a valid ID for the Guest OS Mapping");
        }

        if (!guestOsHypervisorHandle.getIsUserDefined()) {
            throw new InvalidParameterValueException("Unable to remove system defined Guest OS mapping");
        }

        return _guestOSHypervisorDao.removeGuestOsMapping(id);

    }

    protected ConsoleProxyInfo getConsoleProxyForVm(final long dataCenterId, final long userVmId) {
        return _consoleProxyMgr.assignProxy(dataCenterId, userVmId);
    }

    private ConsoleProxyVO startConsoleProxy(final long instanceId) {
        return _consoleProxyMgr.startProxy(instanceId, true);
    }

    private ConsoleProxyVO stopConsoleProxy(final VMInstanceVO systemVm, final boolean isForced) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException {

        _itMgr.advanceStop(systemVm.getUuid(), isForced);
        return _consoleProxyDao.findById(systemVm.getId());
    }

    private ConsoleProxyVO rebootConsoleProxy(final long instanceId) {
        _consoleProxyMgr.rebootProxy(instanceId);
        return _consoleProxyDao.findById(instanceId);
    }

    protected ConsoleProxyVO destroyConsoleProxy(final long instanceId) {
        final ConsoleProxyVO proxy = _consoleProxyDao.findById(instanceId);

        if (_consoleProxyMgr.destroyProxy(instanceId)) {
            return proxy;
        }
        return null;
    }

    @Override
    public String getConsoleAccessUrlRoot(final long vmId) {
        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm != null) {
            final ConsoleProxyInfo proxy = getConsoleProxyForVm(vm.getDataCenterId(), vmId);
            if (proxy != null) {
                return proxy.getProxyImageUrl();
            }
        }
        return null;
    }

    @Override
    public Pair<String, Integer> getVncPort(final VirtualMachine vm) {
        if (vm.getHostId() == null) {
            s_logger.warn("VM " + vm.getHostName() + " does not have host, return -1 for its VNC port");
            return new Pair<String, Integer>(null, -1);
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Trying to retrieve VNC port from agent about VM " + vm.getHostName());
        }

        final GetVncPortAnswer answer = (GetVncPortAnswer)_agentMgr.easySend(vm.getHostId(), new GetVncPortCommand(vm.getId(), vm.getInstanceName()));
        if (answer != null && answer.getResult()) {
            return new Pair<String, Integer>(answer.getAddress(), answer.getPort());
        }

        return new Pair<String, Integer>(null, -1);
    }

    @Override
    public Pair<List<? extends Alert>, Integer> searchForAlerts(final ListAlertsCmd cmd) {
        final Filter searchFilter = new Filter(AlertVO.class, "lastSent", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchCriteria<AlertVO> sc = _alertDao.createSearchCriteria();

        final Object id = cmd.getId();
        final Object type = cmd.getType();
        final Object keyword = cmd.getKeyword();
        final Object name = cmd.getName();

        final Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), null);
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        if (zoneId != null) {
            sc.addAnd("data_center_id", SearchCriteria.Op.EQ, zoneId);
        }

        if (keyword != null) {
            final SearchCriteria<AlertVO> ssc = _alertDao.createSearchCriteria();
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
        final Pair<List<AlertVO>, Integer> result = _alertDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Alert>, Integer>(result.first(), result.second());
    }

    @Override
    public boolean archiveAlerts(final ArchiveAlertsCmd cmd) {
        final Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), null);
        final boolean result = _alertDao.archiveAlert(cmd.getIds(), cmd.getType(), cmd.getStartDate(), cmd.getEndDate(), zoneId);
        return result;
    }

    @Override
    public boolean deleteAlerts(final DeleteAlertsCmd cmd) {
        final Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), null);
        final boolean result = _alertDao.deleteAlert(cmd.getIds(), cmd.getType(), cmd.getStartDate(), cmd.getEndDate(), zoneId);
        return result;
    }

    @Override
    public List<CapacityVO> listTopConsumedResources(final ListCapacityCmd cmd) {

        final Integer capacityType = cmd.getType();
        Long zoneId = cmd.getZoneId();
        final Long podId = cmd.getPodId();
        final Long clusterId = cmd.getClusterId();
        final Boolean fetchLatest = cmd.getFetchLatest();

        if (clusterId != null) {
            throw new InvalidParameterValueException("Currently clusterId param is not suppoerted");
        }
        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);

        if (fetchLatest != null && fetchLatest) {
            _alertMgr.recalculateCapacity();
        }
        List<SummedCapacity> summedCapacities = new ArrayList<SummedCapacity>();

        if (zoneId == null && podId == null) {// Group by Zone, capacity type
            final List<SummedCapacity> summedCapacitiesAtZone = _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 1, cmd.getPageSizeVal());
            if (summedCapacitiesAtZone != null) {
                summedCapacities.addAll(summedCapacitiesAtZone);
            }
        } else if (podId == null) {// Group by Pod, capacity type
            final List<SummedCapacity> summedCapacitiesAtPod = _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 2, cmd.getPageSizeVal());
            if (summedCapacitiesAtPod != null) {
                summedCapacities.addAll(summedCapacitiesAtPod);
            }
        } else { // Group by Cluster, capacity type
            final List<SummedCapacity> summedCapacitiesAtCluster = _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 3, cmd.getPageSizeVal());
            if (summedCapacitiesAtCluster != null) {
                summedCapacities.addAll(summedCapacitiesAtCluster);
            }
        }

        List<SummedCapacity> summedCapacitiesForSecStorage = getStorageUsed(clusterId, podId, zoneId, capacityType);
        if (summedCapacitiesForSecStorage != null) {
            summedCapacities.addAll(summedCapacitiesForSecStorage);
        }

        // Sort Capacities
        Collections.sort(summedCapacities, new Comparator<SummedCapacity>() {
            @Override
            public int compare(final SummedCapacity arg0, final SummedCapacity arg1) {
                if (arg0.getPercentUsed() < arg1.getPercentUsed()) {
                    return 1;
                } else if (arg0.getPercentUsed().equals(arg1.getPercentUsed())) {
                    return 0;
                }
                return -1;
            }
        });

        final List<CapacityVO> capacities = new ArrayList<CapacityVO>();

        Integer pageSize = null;
        try {
            pageSize = Integer.valueOf(cmd.getPageSizeVal().toString());
        } catch (final IllegalArgumentException e) {
            throw new InvalidParameterValueException("pageSize " + cmd.getPageSizeVal() + " is out of Integer range is not supported for this call");
        }

        summedCapacities = summedCapacities.subList(0, summedCapacities.size() < cmd.getPageSizeVal() ? summedCapacities.size() : pageSize);
        for (final SummedCapacity summedCapacity : summedCapacities) {
            final CapacityVO capacity = new CapacityVO(summedCapacity.getDataCenterId(), summedCapacity.getPodId(), summedCapacity.getClusterId(), summedCapacity.getCapacityType(),
                    summedCapacity.getPercentUsed());
            capacity.setUsedCapacity(summedCapacity.getUsedCapacity() + summedCapacity.getReservedCapacity());
            capacity.setTotalCapacity(summedCapacity.getTotalCapacity());
            capacities.add(capacity);
        }
        return capacities;
    }

    List<SummedCapacity> getStorageUsed(Long clusterId, Long podId, Long zoneId, Integer capacityType) {
        if (capacityType == null || capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE) {
            final List<SummedCapacity> list = new ArrayList<SummedCapacity>();
            if (zoneId != null) {
                final DataCenterVO zone = ApiDBUtils.findZoneById(zoneId);
                if (zone == null || zone.getAllocationState() == AllocationState.Disabled) {
                    return null;
                }
                List<CapacityVO> capacities = new ArrayList<CapacityVO>();
                capacities.add(_storageMgr.getSecondaryStorageUsedStats(null, zoneId));
                capacities.add(_storageMgr.getStoragePoolUsedStats(null, clusterId, podId, zoneId));
                for (CapacityVO capacity : capacities) {
                    if (capacity.getTotalCapacity() != 0) {
                        capacity.setUsedPercentage((float)capacity.getUsedCapacity() / capacity.getTotalCapacity());
                    } else {
                        capacity.setUsedPercentage(0);
                    }
                    final SummedCapacity summedCapacity = new SummedCapacity(capacity.getUsedCapacity(), capacity.getTotalCapacity(), capacity.getUsedPercentage(), capacity.getCapacityType(),
                            capacity.getDataCenterId(), capacity.getPodId(), capacity.getClusterId());
                    list.add(summedCapacity);
                }
            } else {
                List<DataCenterVO> dcList = _dcDao.listEnabledZones();
                for (DataCenterVO dc : dcList) {
                    List<CapacityVO> capacities = new ArrayList<CapacityVO>();
                    capacities.add(_storageMgr.getSecondaryStorageUsedStats(null, dc.getId()));
                    capacities.add(_storageMgr.getStoragePoolUsedStats(null, null, null, dc.getId()));
                    for (CapacityVO capacity : capacities) {
                        if (capacity.getTotalCapacity() != 0) {
                            capacity.setUsedPercentage((float)capacity.getUsedCapacity() / capacity.getTotalCapacity());
                        } else {
                            capacity.setUsedPercentage(0);
                        }
                        SummedCapacity summedCapacity = new SummedCapacity(capacity.getUsedCapacity(), capacity.getTotalCapacity(), capacity.getUsedPercentage(), capacity.getCapacityType(),
                                capacity.getDataCenterId(), capacity.getPodId(), capacity.getClusterId());
                        list.add(summedCapacity);
                    }
                }// End of for
            }
            return list;
        }
        return null;
    }

    @Override
    public List<CapacityVO> listCapacities(final ListCapacityCmd cmd) {

        final Integer capacityType = cmd.getType();
        Long zoneId = cmd.getZoneId();
        final Long podId = cmd.getPodId();
        final Long clusterId = cmd.getClusterId();
        final Boolean fetchLatest = cmd.getFetchLatest();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), zoneId);
        if (fetchLatest != null && fetchLatest) {
            _alertMgr.recalculateCapacity();
        }

        final List<SummedCapacity> summedCapacities = _capacityDao.findCapacityBy(capacityType, zoneId, podId, clusterId);
        final List<CapacityVO> capacities = new ArrayList<CapacityVO>();

        for (final SummedCapacity summedCapacity : summedCapacities) {
            final CapacityVO capacity = new CapacityVO(null, summedCapacity.getDataCenterId(), summedCapacity.getPodId(), summedCapacity.getClusterId(),
                    summedCapacity.getUsedCapacity() + summedCapacity.getReservedCapacity(), summedCapacity.getTotalCapacity(), summedCapacity.getCapacityType());
            capacity.setAllocatedCapacity(summedCapacity.getAllocatedCapacity());
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

        for (final DataCenterVO zone : dcList) {
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
    public long getMemoryOrCpuCapacityByHost(final Long hostId, final short capacityType) {

        final CapacityVO capacity = _capacityDao.findByHostIdType(hostId, capacityType);
        return capacity == null ? 0 : capacity.getReservedCapacity() + capacity.getUsedCapacity();

    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
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
        cmdList.add(ListDomainsCmdByAdmin.class);
        cmdList.add(UpdateDomainCmd.class);
        cmdList.add(AddHostCmd.class);
        cmdList.add(AddSecondaryStorageCmd.class);
        cmdList.add(CancelMaintenanceCmd.class);
        cmdList.add(DeleteHostCmd.class);
        cmdList.add(ListHostsCmd.class);
        cmdList.add(ListHostTagsCmd.class);
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
        cmdList.add(AddSwiftCmd.class);
        cmdList.add(CancelPrimaryStorageMaintenanceCmd.class);
        cmdList.add(CreateStoragePoolCmd.class);
        cmdList.add(DeletePoolCmd.class);
        cmdList.add(ListSwiftsCmd.class);
        cmdList.add(ListStoragePoolsCmd.class);
        cmdList.add(ListStorageTagsCmd.class);
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
        cmdList.add(ListUsageRecordsCmd.class);
        cmdList.add(RemoveRawUsageRecordsCmd.class);
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
        cmdList.add(MoveUserCmd.class);
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
        cmdList.add(ListGuestOsMappingCmd.class);
        cmdList.add(AddGuestOsCmd.class);
        cmdList.add(AddGuestOsMappingCmd.class);
        cmdList.add(UpdateGuestOsCmd.class);
        cmdList.add(UpdateGuestOsMappingCmd.class);
        cmdList.add(RemoveGuestOsCmd.class);
        cmdList.add(RemoveGuestOsMappingCmd.class);
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
        cmdList.add(CreateSnapshotFromVMSnapshotCmd.class);
        cmdList.add(DeleteSnapshotCmd.class);
        cmdList.add(ArchiveSnapshotCmd.class);
        cmdList.add(CreateSnapshotPolicyCmd.class);
        cmdList.add(UpdateSnapshotPolicyCmd.class);
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
        cmdList.add(ListDetailOptionsCmd.class);
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
        cmdList.add(ListZonesCmd.class);
        cmdList.add(ListVMSnapshotCmd.class);
        cmdList.add(CreateVMSnapshotCmd.class);
        cmdList.add(RevertToVMSnapshotCmd.class);
        cmdList.add(DeleteVMSnapshotCmd.class);
        cmdList.add(AddIpToVmNicCmd.class);
        cmdList.add(RemoveIpFromVmNicCmd.class);
        cmdList.add(UpdateVmNicIpCmd.class);
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
        cmdList.add(AddImageStoreS3CMD.class);
        cmdList.add(ListImageStoresCmd.class);
        cmdList.add(DeleteImageStoreCmd.class);
        cmdList.add(CreateSecondaryStagingStoreCmd.class);
        cmdList.add(ListSecondaryStagingStoresCmd.class);
        cmdList.add(DeleteSecondaryStagingStoreCmd.class);
        cmdList.add(UpdateCloudToUseObjectStoreCmd.class);
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
        cmdList.add(MoveNetworkAclItemCmd.class);
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
        cmdList.add(GetVMUserDataCmd.class);
        cmdList.add(UpdateEgressFirewallRuleCmd.class);
        cmdList.add(UpdateFirewallRuleCmd.class);
        cmdList.add(UpdateNetworkACLListCmd.class);
        cmdList.add(UpdateApplicationLoadBalancerCmd.class);
        cmdList.add(UpdateIPAddrCmd.class);
        cmdList.add(UpdateRemoteAccessVpnCmd.class);
        cmdList.add(UpdateVpnConnectionCmd.class);
        cmdList.add(UpdateVpnGatewayCmd.class);
        // separated admin commands
        cmdList.add(ListAccountsCmdByAdmin.class);
        cmdList.add(ListZonesCmdByAdmin.class);
        cmdList.add(ListTemplatesCmdByAdmin.class);
        cmdList.add(CreateTemplateCmdByAdmin.class);
        cmdList.add(CopyTemplateCmdByAdmin.class);
        cmdList.add(RegisterTemplateCmdByAdmin.class);
        cmdList.add(ListTemplatePermissionsCmdByAdmin.class);
        cmdList.add(RegisterIsoCmdByAdmin.class);
        cmdList.add(CopyIsoCmdByAdmin.class);
        cmdList.add(ListIsosCmdByAdmin.class);
        cmdList.add(AttachIsoCmdByAdmin.class);
        cmdList.add(DetachIsoCmdByAdmin.class);
        cmdList.add(ListIsoPermissionsCmdByAdmin.class);
        cmdList.add(UpdateVMAffinityGroupCmdByAdmin.class);
        cmdList.add(AddNicToVMCmdByAdmin.class);
        cmdList.add(RemoveNicFromVMCmdByAdmin.class);
        cmdList.add(UpdateDefaultNicForVMCmdByAdmin.class);
        cmdList.add(ListLoadBalancerRuleInstancesCmdByAdmin.class);
        cmdList.add(DeployVMCmdByAdmin.class);
        cmdList.add(DestroyVMCmdByAdmin.class);
        cmdList.add(RebootVMCmdByAdmin.class);
        cmdList.add(ResetVMPasswordCmdByAdmin.class);
        cmdList.add(ResetVMSSHKeyCmdByAdmin.class);
        cmdList.add(RestoreVMCmdByAdmin.class);
        cmdList.add(ScaleVMCmdByAdmin.class);
        cmdList.add(StartVMCmdByAdmin.class);
        cmdList.add(StopVMCmdByAdmin.class);
        cmdList.add(UpdateVMCmdByAdmin.class);
        cmdList.add(UpgradeVMCmdByAdmin.class);
        cmdList.add(RevertToVMSnapshotCmdByAdmin.class);
        cmdList.add(ListVMsCmdByAdmin.class);
        cmdList.add(AttachVolumeCmdByAdmin.class);
        cmdList.add(CreateVolumeCmdByAdmin.class);
        cmdList.add(DetachVolumeCmdByAdmin.class);
        cmdList.add(MigrateVolumeCmdByAdmin.class);
        cmdList.add(ResizeVolumeCmdByAdmin.class);
        cmdList.add(UpdateVolumeCmdByAdmin.class);
        cmdList.add(UploadVolumeCmdByAdmin.class);
        cmdList.add(ListVolumesCmdByAdmin.class);
        cmdList.add(AssociateIPAddrCmdByAdmin.class);
        cmdList.add(ListPublicIpAddressesCmdByAdmin.class);
        cmdList.add(CreateNetworkCmdByAdmin.class);
        cmdList.add(UpdateNetworkCmdByAdmin.class);
        cmdList.add(ListNetworksCmdByAdmin.class);
        cmdList.add(CreateVPCCmdByAdmin.class);
        cmdList.add(ListVPCsCmdByAdmin.class);
        cmdList.add(UpdateVPCCmdByAdmin.class);
        cmdList.add(UpdateLBStickinessPolicyCmd.class);
        cmdList.add(UpdateLBHealthCheckPolicyCmd.class);
        cmdList.add(GetUploadParamsForTemplateCmd.class);
        cmdList.add(GetUploadParamsForVolumeCmd.class);
        cmdList.add(MigrateNetworkCmd.class);
        cmdList.add(MigrateVPCCmd.class);
        cmdList.add(AcquirePodIpCmdByAdmin.class);
        cmdList.add(ReleasePodIpCmdByAdmin.class);
        cmdList.add(CreateManagementNetworkIpRangeCmd.class);
        cmdList.add(DeleteManagementNetworkIpRangeCmd.class);
        cmdList.add(UploadTemplateDirectDownloadCertificateCmd.class);
        cmdList.add(RevokeTemplateDirectDownloadCertificateCmd.class);
        cmdList.add(ListMgmtsCmd.class);
        cmdList.add(GetUploadParamsForIsoCmd.class);
        cmdList.add(ListTemplateOVFProperties.class);

        // Out-of-band management APIs for admins
        cmdList.add(EnableOutOfBandManagementForHostCmd.class);
        cmdList.add(DisableOutOfBandManagementForHostCmd.class);
        cmdList.add(EnableOutOfBandManagementForClusterCmd.class);
        cmdList.add(DisableOutOfBandManagementForClusterCmd.class);
        cmdList.add(EnableOutOfBandManagementForZoneCmd.class);
        cmdList.add(DisableOutOfBandManagementForZoneCmd.class);
        cmdList.add(ConfigureOutOfBandManagementCmd.class);
        cmdList.add(IssueOutOfBandManagementPowerActionCmd.class);
        cmdList.add(ChangeOutOfBandManagementPasswordCmd.class);
        cmdList.add(GetUserKeysCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return ManagementServer.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {vmPasswordLength, sshKeyLength};
    }

    protected class EventPurgeTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                final GlobalLock lock = GlobalLock.getInternLock("EventPurge");
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
                    final Date purgeTime = purgeCal.getTime();
                    s_logger.debug("Deleting events older than: " + purgeTime.toString());
                    final List<EventVO> oldEvents = _eventDao.listOlderEvents(purgeTime);
                    s_logger.debug("Found " + oldEvents.size() + " events to be purged");
                    for (final EventVO event : oldEvents) {
                        _eventDao.expunge(event.getId());
                    }
                } catch (final Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    lock.unlock();
                }
            } catch (final Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }

    protected class AlertPurgeTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                final GlobalLock lock = GlobalLock.getInternLock("AlertPurge");
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
                    final Date purgeTime = purgeCal.getTime();
                    s_logger.debug("Deleting alerts older than: " + purgeTime.toString());
                    final List<AlertVO> oldAlerts = _alertDao.listOlderAlerts(purgeTime);
                    s_logger.debug("Found " + oldAlerts.size() + " events to be purged");
                    for (final AlertVO alert : oldAlerts) {
                        _alertDao.expunge(alert.getId());
                    }
                } catch (final Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    lock.unlock();
                }
            } catch (final Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }

    private SecondaryStorageVmVO startSecondaryStorageVm(final long instanceId) {
        return _secStorageVmMgr.startSecStorageVm(instanceId);
    }

    private SecondaryStorageVmVO stopSecondaryStorageVm(final VMInstanceVO systemVm, final boolean isForced)
            throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException {

        _itMgr.advanceStop(systemVm.getUuid(), isForced);
        return _secStorageVmDao.findById(systemVm.getId());
    }

    public SecondaryStorageVmVO rebootSecondaryStorageVm(final long instanceId) {
        _secStorageVmMgr.rebootSecStorageVm(instanceId);
        return _secStorageVmDao.findById(instanceId);
    }

    protected SecondaryStorageVmVO destroySecondaryStorageVm(final long instanceId) {
        final SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(instanceId);
        if (_secStorageVmMgr.destroySecStorageVm(instanceId)) {
            return secStorageVm;
        }
        return null;
    }

    @Override
    public Pair<List<? extends VirtualMachine>, Integer> searchForSystemVm(final ListSystemVMsCmd cmd) {
        final String type = cmd.getSystemVmType();
        final Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        final Long id = cmd.getId();
        final String name = cmd.getSystemVmName();
        final String state = cmd.getState();
        final String keyword = cmd.getKeyword();
        final Long podId = cmd.getPodId();
        final Long hostId = cmd.getHostId();
        final Long storageId = cmd.getStorageId();

        final Filter searchFilter = new Filter(VMInstanceVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchBuilder<VMInstanceVO> sb = _vmInstanceDao.createSearchBuilder();

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("hostName", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("nulltype", sb.entity().getType(), SearchCriteria.Op.IN);

        if (storageId != null) {
            final SearchBuilder<VolumeVO> volumeSearch = _volumeDao.createSearchBuilder();
            volumeSearch.and("poolId", volumeSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
            sb.join("volumeSearch", volumeSearch, sb.entity().getId(), volumeSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
        }

        final SearchCriteria<VMInstanceVO> sc = sb.create();

        if (keyword != null) {
            final SearchCriteria<VMInstanceVO> ssc = _vmInstanceDao.createSearchCriteria();
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

        final Pair<List<VMInstanceVO>, Integer> result = _vmInstanceDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends VirtualMachine>, Integer>(result.first(), result.second());
    }

    @Override
    public VirtualMachine.Type findSystemVMTypeById(final long instanceId) {
        final VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a system vm of specified instanceId");
            ex.addProxyObject(String.valueOf(instanceId), "instanceId");
            throw ex;
        }
        return systemVm.getType();
    }

    @Override
    @ActionEvent(eventType = "", eventDescription = "", async = true)
    public VirtualMachine startSystemVM(final long vmId) {

        final VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(vmId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
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
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a system vm with specified vmId");
            ex.addProxyObject(systemVm.getUuid(), "vmId");
            throw ex;
        }
    }

    @Override
    @ActionEvent(eventType = "", eventDescription = "", async = true)
    public VMInstanceVO stopSystemVM(final StopSystemVmCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        final Long id = cmd.getId();

        // verify parameters
        final VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(id, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
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
        } catch (final OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + systemVm, e);
        }
    }

    @Override
    public VMInstanceVO rebootSystemVM(final RebootSystemVmCmd cmd) {
        final VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(cmd.getId(), VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);

        if (systemVm == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
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
    public VMInstanceVO destroySystemVM(final DestroySystemVmCmd cmd) {
        final VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(cmd.getId(), VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);

        if (systemVm == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
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

    private String signRequest(final String request, final String key) {
        try {
            s_logger.info("Request: " + request);
            s_logger.info("Key: " + key);

            if (key != null && request != null) {
                final Mac mac = Mac.getInstance("HmacSHA1");
                final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                mac.init(keySpec);
                mac.update(request.getBytes());
                final byte[] encryptedBytes = mac.doFinal();
                return new String(Base64.encodeBase64(encryptedBytes));
            }
        } catch (final Exception ex) {
            s_logger.error("unable to sign request", ex);
        }
        return null;
    }

    @Override
    public ArrayList<String> getCloudIdentifierResponse(final long userId) {
        final Account caller = getCaller();

        // verify that user exists
        User user = _accountMgr.getUserIncludingRemoved(userId);
        if (user == null || user.getRemoved() != null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find active user of specified id");
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
            final String secretKey = user.getSecretKey();
            final String input = cloudIdentifier;
            signature = signRequest(input, secretKey);
        } catch (final Exception e) {
            s_logger.warn("Exception whilst creating a signature:" + e);
        }

        final ArrayList<String> cloudParams = new ArrayList<String>();
        cloudParams.add(cloudIdentifier);
        cloudParams.add(signature);

        return cloudParams;
    }

    @Override
    public Map<String, Object> listCapabilities(final ListCapabilitiesCmd cmd) {
        final Map<String, Object> capabilities = new HashMap<String, Object>();

        final Account caller = getCaller();
        boolean securityGroupsEnabled = false;
        boolean elasticLoadBalancerEnabled = false;
        boolean KVMSnapshotEnabled = false;
        String supportELB = "false";
        final List<NetworkVO> networks = _networkDao.listSecurityGroupEnabledNetworks();
        if (networks != null && !networks.isEmpty()) {
            securityGroupsEnabled = true;
            final String elbEnabled = _configDao.getValue(Config.ElasticLoadBalancerEnabled.key());
            elasticLoadBalancerEnabled = elbEnabled == null ? false : Boolean.parseBoolean(elbEnabled);
            if (elasticLoadBalancerEnabled) {
                final String networkType = _configDao.getValue(Config.ElasticLoadBalancerNetwork.key());
                if (networkType != null) {
                    supportELB = networkType;
                }
            }
        }

        final long diskOffMinSize = VolumeOrchestrationService.CustomDiskOfferingMinSize.value();
        final long diskOffMaxSize = VolumeOrchestrationService.CustomDiskOfferingMaxSize.value();
        KVMSnapshotEnabled = Boolean.parseBoolean(_configDao.getValue("KVM.snapshot.enabled"));

        final boolean userPublicTemplateEnabled = TemplateManager.AllowPublicUserTemplates.valueIn(caller.getId());

        // add some parameters UI needs to handle API throttling
        final boolean apiLimitEnabled = Boolean.parseBoolean(_configDao.getValue(Config.ApiLimitEnabled.key()));
        final Integer apiLimitInterval = Integer.valueOf(_configDao.getValue(Config.ApiLimitInterval.key()));
        final Integer apiLimitMax = Integer.valueOf(_configDao.getValue(Config.ApiLimitMax.key()));

        final boolean allowUserViewDestroyedVM = (QueryService.AllowUserViewDestroyedVM.valueIn(caller.getId()) | _accountService.isAdmin(caller.getId()));
        final boolean allowUserExpungeRecoverVM = (UserVmManager.AllowUserExpungeRecoverVm.valueIn(caller.getId()) | _accountService.isAdmin(caller.getId()));

        final boolean allowUserViewAllDomainAccounts = (QueryService.AllowUserViewAllDomainAccounts.valueIn(caller.getDomainId()));

        // check if region-wide secondary storage is used
        boolean regionSecondaryEnabled = false;
        final List<ImageStoreVO> imgStores = _imgStoreDao.findRegionImageStores();
        if (imgStores != null && imgStores.size() > 0) {
            regionSecondaryEnabled = true;
        }

        capabilities.put("securityGroupsEnabled", securityGroupsEnabled);
        capabilities.put("userPublicTemplateEnabled", userPublicTemplateEnabled);
        capabilities.put("cloudStackVersion", getVersion());
        capabilities.put("supportELB", supportELB);
        capabilities.put("projectInviteRequired", _projectMgr.projectInviteRequired());
        capabilities.put("allowusercreateprojects", _projectMgr.allowUserToCreateProject());
        capabilities.put("customDiskOffMinSize", diskOffMinSize);
        capabilities.put("customDiskOffMaxSize", diskOffMaxSize);
        capabilities.put("regionSecondaryEnabled", regionSecondaryEnabled);
        capabilities.put("KVMSnapshotEnabled", KVMSnapshotEnabled);
        capabilities.put("allowUserViewDestroyedVM", allowUserViewDestroyedVM);
        capabilities.put("allowUserExpungeRecoverVM", allowUserExpungeRecoverVM);
        capabilities.put("allowUserViewAllDomainAccounts", allowUserViewAllDomainAccounts);
        if (apiLimitEnabled) {
            capabilities.put("apiLimitInterval", apiLimitInterval);
            capabilities.put("apiLimitMax", apiLimitMax);
        }

        return capabilities;
    }

    @Override
    public GuestOSVO getGuestOs(final Long guestOsId) {
        return _guestOSDao.findById(guestOsId);
    }

    @Override
    public GuestOSHypervisorVO getGuestOsHypervisor(final Long guestOsHypervisorId) {
        return _guestOSHypervisorDao.findById(guestOsHypervisorId);
    }

    @Override
    public InstanceGroupVO updateVmGroup(final UpdateVMGroupCmd cmd) {
        final Account caller = getCaller();
        final Long groupId = cmd.getId();
        final String groupName = cmd.getGroupName();

        // Verify input parameters
        final InstanceGroupVO group = _vmGroupDao.findById(groupId.longValue());
        if (group == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a vm group with specified groupId");
            ex.addProxyObject(groupId.toString(), "groupId");
            throw ex;
        }

        _accountMgr.checkAccess(caller, null, true, group);

        // Check if name is already in use by this account (exclude this group)
        final boolean isNameInUse = _vmGroupDao.isNameInUse(group.getAccountId(), groupName);

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
        final String fullVersion = c.getPackage().getImplementationVersion();
        if (fullVersion != null && fullVersion.length() > 0) {
            return fullVersion;
        }

        return "unknown";
    }

    @Override
    @DB
    public String uploadCertificate(final UploadCustomCertificateCmd cmd) {
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

        final String certificate = cmd.getCertificate();
        final String key = cmd.getPrivateKey();

        if (cmd.getPrivateKey() != null && !_ksMgr.validateCertificate(certificate, key, cmd.getDomainSuffix())) {
            throw new InvalidParameterValueException("Failed to pass certificate validation check");
        }

        if (cmd.getPrivateKey() != null) {
            _ksMgr.saveCertificate(ConsoleProxyManager.CERTIFICATE_NAME, certificate, key, cmd.getDomainSuffix());

            // Reboot ssvm here since private key is present - meaning server cert being passed
            final List<SecondaryStorageVmVO> alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates(null, State.Running, State.Migrating, State.Starting);
            for (final SecondaryStorageVmVO ssVmVm : alreadyRunning) {
                _secStorageVmMgr.rebootSecStorageVm(ssVmVm.getId());
            }
        } else {
            _ksMgr.saveCertificate(cmd.getAlias(), certificate, cmd.getCertIndex(), cmd.getDomainSuffix());
        }

        _consoleProxyMgr.setManagementState(ConsoleProxyManagementState.ResetSuspending);
        return "Certificate has been successfully updated, if its the server certificate we would reboot all "
        + "running console proxy VMs and secondary storage VMs to propagate the new certificate, "
        + "please give a few minutes for console access and storage services service to be up and working again";
    }

    @Override
    public List<String> getHypervisors(final Long zoneId) {
        final List<String> result = new ArrayList<String>();
        final String hypers = _configDao.getValue(Config.HypervisorList.key());
        final String[] hypervisors = hypers.split(",");

        if (zoneId != null) {
            if (zoneId.longValue() == -1L) {
                final List<DataCenterVO> zones = _dcDao.listAll();

                for (final String hypervisor : hypervisors) {
                    int hyperCount = 0;
                    for (final DataCenterVO zone : zones) {
                        final List<ClusterVO> clusters = _clusterDao.listByDcHyType(zone.getId(), hypervisor);
                        if (!clusters.isEmpty()) {
                            hyperCount++;
                        }
                    }
                    if (hyperCount == zones.size()) {
                        result.add(hypervisor);
                    }
                }
            } else {
                final List<ClusterVO> clustersForZone = _clusterDao.listByZoneId(zoneId);
                for (final ClusterVO cluster : clustersForZone) {
                    result.add(cluster.getHypervisorType().toString());
                }
            }

        } else {
            return Arrays.asList(hypervisors);
        }
        return result;
    }

    @Override
    public SSHKeyPair createSSHKeyPair(final CreateSSHKeyPairCmd cmd) {
        final Account caller = getCaller();
        final String accountName = cmd.getAccountName();
        final Long domainId = cmd.getDomainId();
        final Long projectId = cmd.getProjectId();

        final Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);

        final SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s != null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' already exists.");
        }

        final SSHKeysHelper keys = new SSHKeysHelper(sshKeyLength.value());

        final String name = cmd.getName();
        final String publicKey = keys.getPublicKey();
        final String fingerprint = keys.getPublicKeyFingerPrint();
        final String privateKey = keys.getPrivateKey();

        return createAndSaveSSHKeyPair(name, fingerprint, publicKey, privateKey, owner);
    }

    @Override
    public boolean deleteSSHKeyPair(final DeleteSSHKeyPairCmd cmd) {
        final Account caller = getCaller();
        final String accountName = cmd.getAccountName();
        final Long domainId = cmd.getDomainId();
        final Long projectId = cmd.getProjectId();

        Account owner = null;
        try {
            owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);
        } catch (InvalidParameterValueException ex) {
            if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN && accountName != null && domainId != null) {
                owner = _accountDao.findAccountIncludingRemoved(accountName, domainId);
            }
            if (owner == null) {
                throw ex;
            }
        }

        final SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException(
                    "A key pair with name '" + cmd.getName() + "' does not exist for account " + owner.getAccountName() + " in specified domain id");
            final DomainVO domain = ApiDBUtils.findDomainById(owner.getDomainId());
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
    public Pair<List<? extends SSHKeyPair>, Integer> listSSHKeyPairs(final ListSSHKeyPairsCmd cmd) {
        final String name = cmd.getName();
        final String fingerPrint = cmd.getFingerprint();
        final String keyword = cmd.getKeyword();

        final Account caller = getCaller();
        final List<Long> permittedAccounts = new ArrayList<Long>();

        final Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        final Long domainId = domainIdRecursiveListProject.first();
        final Boolean isRecursive = domainIdRecursiveListProject.second();
        final ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        final SearchBuilder<SSHKeyPairVO> sb = _sshKeyPairDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        final Filter searchFilter = new Filter(SSHKeyPairVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        final SearchCriteria<SSHKeyPairVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        if (fingerPrint != null) {
            sc.addAnd("fingerprint", SearchCriteria.Op.EQ, fingerPrint);
        }

        if (keyword != null) {
            sc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addOr("fingerprint", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        final Pair<List<SSHKeyPairVO>, Integer> result = _sshKeyPairDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends SSHKeyPair>, Integer>(result.first(), result.second());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_REGISTER_SSH_KEYPAIR, eventDescription = "registering ssh keypair", async = true)
    public SSHKeyPair registerSSHKeyPair(final RegisterSSHKeyPairCmd cmd) {
        final Account owner = getOwner(cmd);
        checkForKeyByName(cmd, owner);
        checkForKeyByPublicKey(cmd, owner);

        final String name = cmd.getName();
        String key = cmd.getPublicKey();

        final String publicKey = getPublicKeyFromKeyKeyMaterial(key);
        final String fingerprint = getFingerprint(publicKey);

        return createAndSaveSSHKeyPair(name, fingerprint, publicKey, null, owner);
    }

    /**
     * @param cmd
     * @param owner
     * @throws InvalidParameterValueException
     */
    private void checkForKeyByPublicKey(final RegisterSSHKeyPairCmd cmd, final Account owner) throws InvalidParameterValueException {
        final SSHKeyPairVO existingPair = _sshKeyPairDao.findByPublicKey(owner.getAccountId(), owner.getDomainId(), getPublicKeyFromKeyKeyMaterial(cmd.getPublicKey()));
        if (existingPair != null) {
            throw new InvalidParameterValueException("A key pair with key '" + cmd.getPublicKey() + "' already exists for this account.");
        }
    }

    /**
     * @param cmd
     * @param owner
     * @throws InvalidParameterValueException
     */
    protected void checkForKeyByName(final RegisterSSHKeyPairCmd cmd, final Account owner) throws InvalidParameterValueException {
        final SSHKeyPairVO existingPair = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (existingPair != null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' already exists for this account.");
        }
    }

    /**
     * @param publicKey
     * @return
     */
    private String getFingerprint(final String publicKey) {
        final String fingerprint = SSHKeysHelper.getPublicKeyFingerprint(publicKey);
        return fingerprint;
    }

    /**
     * @param key
     * @return
     * @throws InvalidParameterValueException
     */
    protected String getPublicKeyFromKeyKeyMaterial(final String key) throws InvalidParameterValueException {
        final String publicKey = SSHKeysHelper.getPublicKeyFromKeyMaterial(key);

        if (publicKey == null) {
            throw new InvalidParameterValueException("Public key is invalid");
        }
        return publicKey;
    }

    /**
     * @param cmd
     * @return
     */
    protected Account getOwner(final RegisterSSHKeyPairCmd cmd) {
        final Account caller = getCaller();

        final Account owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        return owner;
    }

    /**
     * @return
     */
    protected Account getCaller() {
        final Account caller = CallContext.current().getCallingAccount();
        return caller;
    }

    private SSHKeyPair createAndSaveSSHKeyPair(final String name, final String fingerprint, final String publicKey, final String privateKey, final Account owner) {
        final SSHKeyPairVO newPair = new SSHKeyPairVO();

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
    public String getVMPassword(final GetVMPasswordCmd cmd) {
        final Account caller = getCaller();

        final UserVmVO vm = _userVmDao.findById(cmd.getId());
        if (vm == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("No VM with specified id found.");
            ex.addProxyObject(cmd.getId().toString(), "vmId");
            throw ex;
        }

        // make permission check
        _accountMgr.checkAccess(caller, null, true, vm);

        _userVmDao.loadDetails(vm);
        final String password = vm.getDetail("Encrypted.Password");
        if (password == null || password.equals("")) {
            final InvalidParameterValueException ex = new InvalidParameterValueException(
                    "No password for VM with specified id found. " + "If VM is created from password enabled template and SSH keypair is assigned to VM then only password can be retrieved.");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        return password;
    }

    private boolean updateHostsInCluster(final UpdateHostPasswordCmd command) {
        // get all the hosts in this cluster
        final List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(command.getClusterId());

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                for (final HostVO h : hosts) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Changing password for host name = " + h.getName());
                    }
                    // update password for this host
                    final DetailVO nv = _detailsDao.findDetail(h.getId(), ApiConstants.USERNAME);
                    if (nv.getValue().equals(command.getUsername())) {
                        final DetailVO nvp = _detailsDao.findDetail(h.getId(), ApiConstants.PASSWORD);
                        nvp.setValue(DBEncryptionUtil.encrypt(command.getPassword()));
                        _detailsDao.persist(nvp);
                    } else {
                        // if one host in the cluster has diff username then
                        // rollback to maintain consistency
                        throw new InvalidParameterValueException("The username is not same for all hosts, please modify passwords for individual hosts.");
                    }
                }
            }
        });
        return true;
    }

    /**
     * This method updates the password of all hosts in a given cluster.
     */
    @Override
    @DB
    public boolean updateClusterPassword(final UpdateHostPasswordCmd command) {
        if (command.getClusterId() == null) {
            throw new InvalidParameterValueException("You should provide a cluster id.");
        }

        final ClusterVO cluster = ApiDBUtils.findClusterById(command.getClusterId());
        if (cluster == null || !supportedHypervisors.contains(cluster.getHypervisorType())) {
            throw new InvalidParameterValueException("This operation is not supported for this hypervisor type");
        }
        return updateHostsInCluster(command);
    }

    @Override
    @DB
    public boolean updateHostPassword(final UpdateHostPasswordCmd cmd) {
        if (cmd.getHostId() == null) {
            throw new InvalidParameterValueException("You should provide an host id.");
        }

        final HostVO host = _hostDao.findById(cmd.getHostId());

        if (host.getHypervisorType() == HypervisorType.XenServer) {
            throw new InvalidParameterValueException("Single host update is not supported by XenServer hypervisors. Please try again informing the Cluster ID.");
        }

        if (!supportedHypervisors.contains(host.getHypervisorType())) {
            throw new InvalidParameterValueException("This operation is not supported for this hypervisor type");
        }
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Changing password for host name = " + host.getName());
                }
                // update password for this host
                final DetailVO nv = _detailsDao.findDetail(host.getId(), ApiConstants.USERNAME);
                if (nv.getValue().equals(cmd.getUsername())) {
                    final DetailVO nvp = _detailsDao.findDetail(host.getId(), ApiConstants.PASSWORD);
                    nvp.setValue(DBEncryptionUtil.encrypt(cmd.getPassword()));
                    _detailsDao.persist(nvp);
                } else {
                    // if one host in the cluster has diff username then
                    // rollback to maintain consistency
                    throw new InvalidParameterValueException("The username is not same for the hosts..");
                }
            }
        });
        return true;
    }

    @Override
    public String[] listEventTypes() {
        final Object eventObj = new EventTypes();
        final Class<EventTypes> c = EventTypes.class;
        final Field[] fields = c.getFields();
        final String[] eventTypes = new String[fields.length];
        try {
            int i = 0;
            for (final Field field : fields) {
                eventTypes[i++] = field.get(eventObj).toString();
            }
            return eventTypes;
        } catch (final IllegalArgumentException e) {
            s_logger.error("Error while listing Event Types", e);
        } catch (final IllegalAccessException e) {
            s_logger.error("Error while listing Event Types", e);
        }
        return null;
    }

    @Override
    public Pair<List<? extends HypervisorCapabilities>, Integer> listHypervisorCapabilities(final Long id, final HypervisorType hypervisorType, final String keyword, final Long startIndex,
            final Long pageSizeVal) {
        final Filter searchFilter = new Filter(HypervisorCapabilitiesVO.class, "id", true, startIndex, pageSizeVal);
        final SearchCriteria<HypervisorCapabilitiesVO> sc = _hypervisorCapabilitiesDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (hypervisorType != null) {
            sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisorType);
        }

        if (keyword != null) {
            final SearchCriteria<HypervisorCapabilitiesVO> ssc = _hypervisorCapabilitiesDao.createSearchCriteria();
            ssc.addOr("hypervisorType", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("hypervisorType", SearchCriteria.Op.SC, ssc);
        }

        final Pair<List<HypervisorCapabilitiesVO>, Integer> result = _hypervisorCapabilitiesDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends HypervisorCapabilities>, Integer>(result.first(), result.second());
    }

    @Override
    public HypervisorCapabilities updateHypervisorCapabilities(final Long id, final Long maxGuestsLimit, final Boolean securityGroupEnabled) {
        HypervisorCapabilitiesVO hpvCapabilities = _hypervisorCapabilitiesDao.findById(id, true);

        if (hpvCapabilities == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("unable to find the hypervisor capabilities for specified id");
            ex.addProxyObject(id.toString(), "Id");
            throw ex;
        }

        final boolean updateNeeded = maxGuestsLimit != null || securityGroupEnabled != null;
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
    public VirtualMachine upgradeSystemVM(final ScaleSystemVMCmd cmd) throws ResourceUnavailableException, ManagementServerException, VirtualMachineMigrationException, ConcurrentOperationException {

        final VMInstanceVO vmInstance = _vmInstanceDao.findById(cmd.getId());
        if (vmInstance.getHypervisorType() == HypervisorType.XenServer && vmInstance.getState().equals(State.Running)) {
            throw new InvalidParameterValueException("Dynamic Scaling operation is not permitted for this hypervisor on system vm");
        }
        final boolean result = _userVmMgr.upgradeVirtualMachine(cmd.getId(), cmd.getServiceOfferingId(), cmd.getDetails());
        if (result) {
            final VirtualMachine vm = _vmInstanceDao.findById(cmd.getId());
            return vm;
        } else {
            throw new CloudRuntimeException("Failed to upgrade System VM");
        }
    }

    @Override
    public VirtualMachine upgradeSystemVM(final UpgradeSystemVMCmd cmd) {
        final Long systemVmId = cmd.getId();
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        return upgradeStoppedSystemVm(systemVmId, serviceOfferingId, cmd.getDetails());

    }

    private VirtualMachine upgradeStoppedSystemVm(final Long systemVmId, final Long serviceOfferingId, final Map<String, String> customparameters) {
        final Account caller = getCaller();

        final VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(systemVmId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            throw new InvalidParameterValueException("Unable to find SystemVm with id " + systemVmId);
        }

        _accountMgr.checkAccess(caller, null, true, systemVm);

        // Check that the specified service offering ID is valid
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(serviceOfferingId);
        final ServiceOfferingVO currentServiceOffering = _offeringDao.findById(systemVmId, systemVm.getServiceOfferingId());
        if (newServiceOffering.isDynamic()) {
            newServiceOffering.setDynamicFlag(true);
            _userVmMgr.validateCustomParameters(newServiceOffering, customparameters);
            newServiceOffering = _offeringDao.getcomputeOffering(newServiceOffering, customparameters);
        }
        _itMgr.checkIfCanUpgrade(systemVm, newServiceOffering);

        final boolean result = _itMgr.upgradeVmDb(systemVmId, serviceOfferingId);

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

    private void enableAdminUser(final String password) {
        String encodedPassword = null;

        final UserVO adminUser = _userDao.getUser(2);
        if (adminUser == null) {
            final String msg = "CANNOT find admin user";
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        if (adminUser.getState() == Account.State.disabled) {
            // This means its a new account, set the password using the
            // authenticator

            for (final UserAuthenticator authenticator : _userPasswordEncoders) {
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
        final List<String> plannersAvailable = new ArrayList<String>();
        for (final DeploymentPlanner planner : _planners) {
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
    public void setStoragePoolAllocators(final List<StoragePoolAllocator> storagePoolAllocators) {
        _storagePoolAllocators = storagePoolAllocators;
    }

    public LockMasterListener getLockMasterListener() {
        return _lockMasterListener;
    }

    public void setLockMasterListener(final LockMasterListener lockMasterListener) {
        _lockMasterListener = lockMasterListener;
    }

}
