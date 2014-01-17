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
package com.cloud.bridge.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

import com.amazon.ec2.ActivateLicense;
import com.amazon.ec2.ActivateLicenseResponse;
import com.amazon.ec2.AllocateAddress;
import com.amazon.ec2.AllocateAddressResponse;
import com.amazon.ec2.AllocateAddressResponseType;
import com.amazon.ec2.AmazonEC2SkeletonInterface;
import com.amazon.ec2.AssignPrivateIpAddresses;
import com.amazon.ec2.AssignPrivateIpAddressesResponse;
import com.amazon.ec2.AssociateAddress;
import com.amazon.ec2.AssociateAddressResponse;
import com.amazon.ec2.AssociateAddressResponseType;
import com.amazon.ec2.AssociateDhcpOptions;
import com.amazon.ec2.AssociateDhcpOptionsResponse;
import com.amazon.ec2.AssociateRouteTable;
import com.amazon.ec2.AssociateRouteTableResponse;
import com.amazon.ec2.AttachInternetGateway;
import com.amazon.ec2.AttachInternetGatewayResponse;
import com.amazon.ec2.AttachNetworkInterface;
import com.amazon.ec2.AttachNetworkInterfaceResponse;
import com.amazon.ec2.AttachVolume;
import com.amazon.ec2.AttachVolumeResponse;
import com.amazon.ec2.AttachVolumeResponseType;
import com.amazon.ec2.AttachVolumeType;
import com.amazon.ec2.AttachVpnGateway;
import com.amazon.ec2.AttachVpnGatewayResponse;
import com.amazon.ec2.AttachmentSetItemResponseType;
import com.amazon.ec2.AttachmentSetResponseType;
import com.amazon.ec2.AttributeValueType;
import com.amazon.ec2.AuthorizeSecurityGroupEgress;
import com.amazon.ec2.AuthorizeSecurityGroupEgressResponse;
import com.amazon.ec2.AuthorizeSecurityGroupIngress;
import com.amazon.ec2.AuthorizeSecurityGroupIngressResponse;
import com.amazon.ec2.AuthorizeSecurityGroupIngressResponseType;
import com.amazon.ec2.AuthorizeSecurityGroupIngressType;
import com.amazon.ec2.AvailabilityZoneItemType;
import com.amazon.ec2.AvailabilityZoneMessageSetType;
import com.amazon.ec2.AvailabilityZoneMessageType;
import com.amazon.ec2.AvailabilityZoneSetType;
import com.amazon.ec2.BlockDeviceMappingItemType;
import com.amazon.ec2.BlockDeviceMappingItemTypeChoice_type0;
import com.amazon.ec2.BlockDeviceMappingType;
import com.amazon.ec2.BundleInstance;
import com.amazon.ec2.BundleInstanceResponse;
import com.amazon.ec2.CancelBundleTask;
import com.amazon.ec2.CancelBundleTaskResponse;
import com.amazon.ec2.CancelConversionTask;
import com.amazon.ec2.CancelConversionTaskResponse;
import com.amazon.ec2.CancelExportTask;
import com.amazon.ec2.CancelExportTaskResponse;
import com.amazon.ec2.CancelReservedInstancesListing;
import com.amazon.ec2.CancelReservedInstancesListingResponse;
import com.amazon.ec2.CancelSpotInstanceRequests;
import com.amazon.ec2.CancelSpotInstanceRequestsResponse;
import com.amazon.ec2.ConfirmProductInstance;
import com.amazon.ec2.ConfirmProductInstanceResponse;
import com.amazon.ec2.CreateCustomerGateway;
import com.amazon.ec2.CreateCustomerGatewayResponse;
import com.amazon.ec2.CreateDhcpOptions;
import com.amazon.ec2.CreateDhcpOptionsResponse;
import com.amazon.ec2.CreateImage;
import com.amazon.ec2.CreateImageResponse;
import com.amazon.ec2.CreateImageResponseType;
import com.amazon.ec2.CreateImageType;
import com.amazon.ec2.CreateInstanceExportTask;
import com.amazon.ec2.CreateInstanceExportTaskResponse;
import com.amazon.ec2.CreateInternetGateway;
import com.amazon.ec2.CreateInternetGatewayResponse;
import com.amazon.ec2.CreateKeyPair;
import com.amazon.ec2.CreateKeyPairResponse;
import com.amazon.ec2.CreateKeyPairResponseType;
import com.amazon.ec2.CreateNetworkAcl;
import com.amazon.ec2.CreateNetworkAclEntry;
import com.amazon.ec2.CreateNetworkAclEntryResponse;
import com.amazon.ec2.CreateNetworkAclResponse;
import com.amazon.ec2.CreateNetworkInterface;
import com.amazon.ec2.CreateNetworkInterfaceResponse;
import com.amazon.ec2.CreatePlacementGroup;
import com.amazon.ec2.CreatePlacementGroupResponse;
import com.amazon.ec2.CreateReservedInstancesListing;
import com.amazon.ec2.CreateReservedInstancesListingResponse;
import com.amazon.ec2.CreateRoute;
import com.amazon.ec2.CreateRouteResponse;
import com.amazon.ec2.CreateRouteTable;
import com.amazon.ec2.CreateRouteTableResponse;
import com.amazon.ec2.CreateSecurityGroup;
import com.amazon.ec2.CreateSecurityGroupResponse;
import com.amazon.ec2.CreateSecurityGroupResponseType;
import com.amazon.ec2.CreateSecurityGroupType;
import com.amazon.ec2.CreateSnapshot;
import com.amazon.ec2.CreateSnapshotResponse;
import com.amazon.ec2.CreateSnapshotResponseType;
import com.amazon.ec2.CreateSnapshotType;
import com.amazon.ec2.CreateSpotDatafeedSubscription;
import com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse;
import com.amazon.ec2.CreateSubnet;
import com.amazon.ec2.CreateSubnetResponse;
import com.amazon.ec2.CreateTags;
import com.amazon.ec2.CreateTagsResponse;
import com.amazon.ec2.CreateTagsResponseType;
import com.amazon.ec2.CreateTagsType;
import com.amazon.ec2.CreateVolume;
import com.amazon.ec2.CreateVolumeResponse;
import com.amazon.ec2.CreateVolumeResponseType;
import com.amazon.ec2.CreateVolumeType;
import com.amazon.ec2.CreateVpc;
import com.amazon.ec2.CreateVpcResponse;
import com.amazon.ec2.CreateVpnConnection;
import com.amazon.ec2.CreateVpnConnectionResponse;
import com.amazon.ec2.CreateVpnConnectionRoute;
import com.amazon.ec2.CreateVpnConnectionRouteResponse;
import com.amazon.ec2.CreateVpnGateway;
import com.amazon.ec2.CreateVpnGatewayResponse;
import com.amazon.ec2.DeactivateLicense;
import com.amazon.ec2.DeactivateLicenseResponse;
import com.amazon.ec2.DeleteCustomerGateway;
import com.amazon.ec2.DeleteCustomerGatewayResponse;
import com.amazon.ec2.DeleteDhcpOptions;
import com.amazon.ec2.DeleteDhcpOptionsResponse;
import com.amazon.ec2.DeleteInternetGateway;
import com.amazon.ec2.DeleteInternetGatewayResponse;
import com.amazon.ec2.DeleteKeyPair;
import com.amazon.ec2.DeleteKeyPairResponse;
import com.amazon.ec2.DeleteKeyPairResponseType;
import com.amazon.ec2.DeleteNetworkAcl;
import com.amazon.ec2.DeleteNetworkAclEntry;
import com.amazon.ec2.DeleteNetworkAclEntryResponse;
import com.amazon.ec2.DeleteNetworkAclResponse;
import com.amazon.ec2.DeleteNetworkInterface;
import com.amazon.ec2.DeleteNetworkInterfaceResponse;
import com.amazon.ec2.DeletePlacementGroup;
import com.amazon.ec2.DeletePlacementGroupResponse;
import com.amazon.ec2.DeleteRoute;
import com.amazon.ec2.DeleteRouteResponse;
import com.amazon.ec2.DeleteRouteTable;
import com.amazon.ec2.DeleteRouteTableResponse;
import com.amazon.ec2.DeleteSecurityGroup;
import com.amazon.ec2.DeleteSecurityGroupResponse;
import com.amazon.ec2.DeleteSecurityGroupResponseType;
import com.amazon.ec2.DeleteSecurityGroupType;
import com.amazon.ec2.DeleteSnapshot;
import com.amazon.ec2.DeleteSnapshotResponse;
import com.amazon.ec2.DeleteSnapshotResponseType;
import com.amazon.ec2.DeleteSnapshotType;
import com.amazon.ec2.DeleteSpotDatafeedSubscription;
import com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse;
import com.amazon.ec2.DeleteSubnet;
import com.amazon.ec2.DeleteSubnetResponse;
import com.amazon.ec2.DeleteTags;
import com.amazon.ec2.DeleteTagsResponse;
import com.amazon.ec2.DeleteTagsResponseType;
import com.amazon.ec2.DeleteTagsSetItemType;
import com.amazon.ec2.DeleteTagsSetType;
import com.amazon.ec2.DeleteTagsType;
import com.amazon.ec2.DeleteVolume;
import com.amazon.ec2.DeleteVolumeResponse;
import com.amazon.ec2.DeleteVolumeResponseType;
import com.amazon.ec2.DeleteVolumeType;
import com.amazon.ec2.DeleteVpc;
import com.amazon.ec2.DeleteVpcResponse;
import com.amazon.ec2.DeleteVpnConnection;
import com.amazon.ec2.DeleteVpnConnectionResponse;
import com.amazon.ec2.DeleteVpnConnectionRoute;
import com.amazon.ec2.DeleteVpnConnectionRouteResponse;
import com.amazon.ec2.DeleteVpnGateway;
import com.amazon.ec2.DeleteVpnGatewayResponse;
import com.amazon.ec2.DeregisterImage;
import com.amazon.ec2.DeregisterImageResponse;
import com.amazon.ec2.DeregisterImageResponseType;
import com.amazon.ec2.DeregisterImageType;
import com.amazon.ec2.DescribeAddresses;
import com.amazon.ec2.DescribeAddressesInfoType;
import com.amazon.ec2.DescribeAddressesItemType;
import com.amazon.ec2.DescribeAddressesResponse;
import com.amazon.ec2.DescribeAddressesResponseInfoType;
import com.amazon.ec2.DescribeAddressesResponseItemType;
import com.amazon.ec2.DescribeAddressesResponseType;
import com.amazon.ec2.DescribeAddressesType;
import com.amazon.ec2.DescribeAvailabilityZones;
import com.amazon.ec2.DescribeAvailabilityZonesResponse;
import com.amazon.ec2.DescribeAvailabilityZonesResponseType;
import com.amazon.ec2.DescribeAvailabilityZonesSetItemType;
import com.amazon.ec2.DescribeAvailabilityZonesSetType;
import com.amazon.ec2.DescribeAvailabilityZonesType;
import com.amazon.ec2.DescribeBundleTasks;
import com.amazon.ec2.DescribeBundleTasksResponse;
import com.amazon.ec2.DescribeConversionTasks;
import com.amazon.ec2.DescribeConversionTasksResponse;
import com.amazon.ec2.DescribeCustomerGateways;
import com.amazon.ec2.DescribeCustomerGatewaysResponse;
import com.amazon.ec2.DescribeDhcpOptions;
import com.amazon.ec2.DescribeDhcpOptionsResponse;
import com.amazon.ec2.DescribeExportTasks;
import com.amazon.ec2.DescribeExportTasksResponse;
import com.amazon.ec2.DescribeImageAttribute;
import com.amazon.ec2.DescribeImageAttributeResponse;
import com.amazon.ec2.DescribeImageAttributeResponseType;
import com.amazon.ec2.DescribeImageAttributeResponseTypeChoice_type0;
import com.amazon.ec2.DescribeImageAttributeType;
import com.amazon.ec2.DescribeImageAttributesGroup;
import com.amazon.ec2.DescribeImages;
import com.amazon.ec2.DescribeImagesExecutableBySetType;
import com.amazon.ec2.DescribeImagesExecutableByType;
import com.amazon.ec2.DescribeImagesInfoType;
import com.amazon.ec2.DescribeImagesItemType;
import com.amazon.ec2.DescribeImagesOwnerType;
import com.amazon.ec2.DescribeImagesOwnersType;
import com.amazon.ec2.DescribeImagesResponse;
import com.amazon.ec2.DescribeImagesResponseInfoType;
import com.amazon.ec2.DescribeImagesResponseItemType;
import com.amazon.ec2.DescribeImagesResponseType;
import com.amazon.ec2.DescribeImagesType;
import com.amazon.ec2.DescribeInstanceAttribute;
import com.amazon.ec2.DescribeInstanceAttributeResponse;
import com.amazon.ec2.DescribeInstanceAttributeResponseType;
import com.amazon.ec2.DescribeInstanceAttributeResponseTypeChoice_type0;
import com.amazon.ec2.DescribeInstanceAttributeType;
import com.amazon.ec2.DescribeInstanceAttributesGroup;
import com.amazon.ec2.DescribeInstanceStatus;
import com.amazon.ec2.DescribeInstanceStatusResponse;
import com.amazon.ec2.DescribeInstances;
import com.amazon.ec2.DescribeInstancesInfoType;
import com.amazon.ec2.DescribeInstancesItemType;
import com.amazon.ec2.DescribeInstancesResponse;
import com.amazon.ec2.DescribeInstancesResponseType;
import com.amazon.ec2.DescribeInstancesType;
import com.amazon.ec2.DescribeInternetGateways;
import com.amazon.ec2.DescribeInternetGatewaysResponse;
import com.amazon.ec2.DescribeKeyPairs;
import com.amazon.ec2.DescribeKeyPairsInfoType;
import com.amazon.ec2.DescribeKeyPairsItemType;
import com.amazon.ec2.DescribeKeyPairsResponse;
import com.amazon.ec2.DescribeKeyPairsResponseInfoType;
import com.amazon.ec2.DescribeKeyPairsResponseItemType;
import com.amazon.ec2.DescribeKeyPairsResponseType;
import com.amazon.ec2.DescribeLicenses;
import com.amazon.ec2.DescribeLicensesResponse;
import com.amazon.ec2.DescribeNetworkAcls;
import com.amazon.ec2.DescribeNetworkAclsResponse;
import com.amazon.ec2.DescribeNetworkInterfaceAttribute;
import com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse;
import com.amazon.ec2.DescribeNetworkInterfaces;
import com.amazon.ec2.DescribeNetworkInterfacesResponse;
import com.amazon.ec2.DescribePlacementGroups;
import com.amazon.ec2.DescribePlacementGroupsResponse;
import com.amazon.ec2.DescribeRegions;
import com.amazon.ec2.DescribeRegionsResponse;
import com.amazon.ec2.DescribeReservedInstances;
import com.amazon.ec2.DescribeReservedInstancesListings;
import com.amazon.ec2.DescribeReservedInstancesListingsResponse;
import com.amazon.ec2.DescribeReservedInstancesOfferings;
import com.amazon.ec2.DescribeReservedInstancesOfferingsResponse;
import com.amazon.ec2.DescribeReservedInstancesResponse;
import com.amazon.ec2.DescribeRouteTables;
import com.amazon.ec2.DescribeRouteTablesResponse;
import com.amazon.ec2.DescribeSecurityGroups;
import com.amazon.ec2.DescribeSecurityGroupsResponse;
import com.amazon.ec2.DescribeSecurityGroupsResponseType;
import com.amazon.ec2.DescribeSecurityGroupsSetItemType;
import com.amazon.ec2.DescribeSecurityGroupsSetType;
import com.amazon.ec2.DescribeSecurityGroupsType;
import com.amazon.ec2.DescribeSnapshotAttribute;
import com.amazon.ec2.DescribeSnapshotAttributeResponse;
import com.amazon.ec2.DescribeSnapshots;
import com.amazon.ec2.DescribeSnapshotsResponse;
import com.amazon.ec2.DescribeSnapshotsResponseType;
import com.amazon.ec2.DescribeSnapshotsSetItemResponseType;
import com.amazon.ec2.DescribeSnapshotsSetItemType;
import com.amazon.ec2.DescribeSnapshotsSetResponseType;
import com.amazon.ec2.DescribeSnapshotsSetType;
import com.amazon.ec2.DescribeSnapshotsType;
import com.amazon.ec2.DescribeSpotDatafeedSubscription;
import com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse;
import com.amazon.ec2.DescribeSpotInstanceRequests;
import com.amazon.ec2.DescribeSpotInstanceRequestsResponse;
import com.amazon.ec2.DescribeSpotPriceHistory;
import com.amazon.ec2.DescribeSpotPriceHistoryResponse;
import com.amazon.ec2.DescribeSubnets;
import com.amazon.ec2.DescribeSubnetsResponse;
import com.amazon.ec2.DescribeTags;
import com.amazon.ec2.DescribeTagsResponse;
import com.amazon.ec2.DescribeTagsResponseType;
import com.amazon.ec2.DescribeTagsType;
import com.amazon.ec2.DescribeVolumeAttribute;
import com.amazon.ec2.DescribeVolumeAttributeResponse;
import com.amazon.ec2.DescribeVolumeStatus;
import com.amazon.ec2.DescribeVolumeStatusResponse;
import com.amazon.ec2.DescribeVolumes;
import com.amazon.ec2.DescribeVolumesResponse;
import com.amazon.ec2.DescribeVolumesResponseType;
import com.amazon.ec2.DescribeVolumesSetItemResponseType;
import com.amazon.ec2.DescribeVolumesSetItemType;
import com.amazon.ec2.DescribeVolumesSetResponseType;
import com.amazon.ec2.DescribeVolumesSetType;
import com.amazon.ec2.DescribeVolumesType;
import com.amazon.ec2.DescribeVpcs;
import com.amazon.ec2.DescribeVpcsResponse;
import com.amazon.ec2.DescribeVpnConnections;
import com.amazon.ec2.DescribeVpnConnectionsResponse;
import com.amazon.ec2.DescribeVpnGateways;
import com.amazon.ec2.DescribeVpnGatewaysResponse;
import com.amazon.ec2.DetachInternetGateway;
import com.amazon.ec2.DetachInternetGatewayResponse;
import com.amazon.ec2.DetachNetworkInterface;
import com.amazon.ec2.DetachNetworkInterfaceResponse;
import com.amazon.ec2.DetachVolume;
import com.amazon.ec2.DetachVolumeResponse;
import com.amazon.ec2.DetachVolumeResponseType;
import com.amazon.ec2.DetachVolumeType;
import com.amazon.ec2.DetachVpnGateway;
import com.amazon.ec2.DetachVpnGatewayResponse;
import com.amazon.ec2.DisableVgwRoutePropagation;
import com.amazon.ec2.DisableVgwRoutePropagationResponse;
import com.amazon.ec2.DisassociateAddress;
import com.amazon.ec2.DisassociateAddressResponse;
import com.amazon.ec2.DisassociateAddressResponseType;
import com.amazon.ec2.DisassociateRouteTable;
import com.amazon.ec2.DisassociateRouteTableResponse;
import com.amazon.ec2.EbsBlockDeviceType;
import com.amazon.ec2.EmptyElementType;
import com.amazon.ec2.EnableVgwRoutePropagation;
import com.amazon.ec2.EnableVgwRoutePropagationResponse;
import com.amazon.ec2.EnableVolumeIO;
import com.amazon.ec2.EnableVolumeIOResponse;
import com.amazon.ec2.FilterSetType;
import com.amazon.ec2.FilterType;
import com.amazon.ec2.GetConsoleOutput;
import com.amazon.ec2.GetConsoleOutputResponse;
import com.amazon.ec2.GetPasswordData;
import com.amazon.ec2.GetPasswordDataResponse;
import com.amazon.ec2.GetPasswordDataResponseType;
import com.amazon.ec2.GroupItemType;
import com.amazon.ec2.GroupSetType;
import com.amazon.ec2.ImportInstance;
import com.amazon.ec2.ImportInstanceResponse;
import com.amazon.ec2.ImportKeyPair;
import com.amazon.ec2.ImportKeyPairResponse;
import com.amazon.ec2.ImportKeyPairResponseType;
import com.amazon.ec2.ImportVolume;
import com.amazon.ec2.ImportVolumeResponse;
import com.amazon.ec2.InstanceIdSetType;
import com.amazon.ec2.InstanceIdType;
import com.amazon.ec2.InstanceMonitoringStateType;
import com.amazon.ec2.InstanceStateChangeSetType;
import com.amazon.ec2.InstanceStateChangeType;
import com.amazon.ec2.InstanceStateType;
import com.amazon.ec2.IpPermissionSetType;
import com.amazon.ec2.IpPermissionType;
import com.amazon.ec2.IpRangeItemType;
import com.amazon.ec2.IpRangeSetType;
import com.amazon.ec2.LaunchPermissionItemType;
import com.amazon.ec2.LaunchPermissionListType;
import com.amazon.ec2.LaunchPermissionOperationType;
import com.amazon.ec2.ModifyImageAttribute;
import com.amazon.ec2.ModifyImageAttributeResponse;
import com.amazon.ec2.ModifyImageAttributeResponseType;
import com.amazon.ec2.ModifyImageAttributeType;
import com.amazon.ec2.ModifyImageAttributeTypeChoice_type0;
import com.amazon.ec2.ModifyInstanceAttribute;
import com.amazon.ec2.ModifyInstanceAttributeResponse;
import com.amazon.ec2.ModifyInstanceAttributeResponseType;
import com.amazon.ec2.ModifyInstanceAttributeType;
import com.amazon.ec2.ModifyInstanceAttributeTypeChoice_type0;
import com.amazon.ec2.ModifyNetworkInterfaceAttribute;
import com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse;
import com.amazon.ec2.ModifySnapshotAttribute;
import com.amazon.ec2.ModifySnapshotAttributeResponse;
import com.amazon.ec2.ModifyVolumeAttribute;
import com.amazon.ec2.ModifyVolumeAttributeResponse;
import com.amazon.ec2.MonitorInstances;
import com.amazon.ec2.MonitorInstancesResponse;
import com.amazon.ec2.MonitorInstancesResponseSetItemType;
import com.amazon.ec2.MonitorInstancesResponseSetType;
import com.amazon.ec2.MonitorInstancesResponseType;
import com.amazon.ec2.MonitorInstancesSetItemType;
import com.amazon.ec2.MonitorInstancesSetType;
import com.amazon.ec2.MonitorInstancesType;
import com.amazon.ec2.NullableAttributeValueType;
import com.amazon.ec2.PlacementRequestType;
import com.amazon.ec2.PlacementResponseType;
import com.amazon.ec2.ProductCodesSetItemType;
import com.amazon.ec2.ProductCodesSetType;
import com.amazon.ec2.PurchaseReservedInstancesOffering;
import com.amazon.ec2.PurchaseReservedInstancesOfferingResponse;
import com.amazon.ec2.RebootInstances;
import com.amazon.ec2.RebootInstancesInfoType;
import com.amazon.ec2.RebootInstancesItemType;
import com.amazon.ec2.RebootInstancesResponse;
import com.amazon.ec2.RebootInstancesResponseType;
import com.amazon.ec2.RebootInstancesType;
import com.amazon.ec2.RegisterImage;
import com.amazon.ec2.RegisterImageResponse;
import com.amazon.ec2.RegisterImageResponseType;
import com.amazon.ec2.RegisterImageType;
import com.amazon.ec2.ReleaseAddress;
import com.amazon.ec2.ReleaseAddressResponse;
import com.amazon.ec2.ReleaseAddressResponseType;
import com.amazon.ec2.ReplaceNetworkAclAssociation;
import com.amazon.ec2.ReplaceNetworkAclAssociationResponse;
import com.amazon.ec2.ReplaceNetworkAclEntry;
import com.amazon.ec2.ReplaceNetworkAclEntryResponse;
import com.amazon.ec2.ReplaceRoute;
import com.amazon.ec2.ReplaceRouteResponse;
import com.amazon.ec2.ReplaceRouteTableAssociation;
import com.amazon.ec2.ReplaceRouteTableAssociationResponse;
import com.amazon.ec2.ReportInstanceStatus;
import com.amazon.ec2.ReportInstanceStatusResponse;
import com.amazon.ec2.RequestSpotInstances;
import com.amazon.ec2.RequestSpotInstancesResponse;
import com.amazon.ec2.ReservationInfoType;
import com.amazon.ec2.ReservationSetType;
import com.amazon.ec2.ResetImageAttribute;
import com.amazon.ec2.ResetImageAttributeResponse;
import com.amazon.ec2.ResetImageAttributeResponseType;
import com.amazon.ec2.ResetImageAttributeType;
import com.amazon.ec2.ResetInstanceAttribute;
import com.amazon.ec2.ResetInstanceAttributeResponse;
import com.amazon.ec2.ResetNetworkInterfaceAttribute;
import com.amazon.ec2.ResetNetworkInterfaceAttributeResponse;
import com.amazon.ec2.ResetSnapshotAttribute;
import com.amazon.ec2.ResetSnapshotAttributeResponse;
import com.amazon.ec2.ResourceIdSetItemType;
import com.amazon.ec2.ResourceIdSetType;
import com.amazon.ec2.ResourceTagSetItemType;
import com.amazon.ec2.ResourceTagSetType;
import com.amazon.ec2.RevokeSecurityGroupEgress;
import com.amazon.ec2.RevokeSecurityGroupEgressResponse;
import com.amazon.ec2.RevokeSecurityGroupIngress;
import com.amazon.ec2.RevokeSecurityGroupIngressResponse;
import com.amazon.ec2.RevokeSecurityGroupIngressResponseType;
import com.amazon.ec2.RevokeSecurityGroupIngressType;
import com.amazon.ec2.RunInstances;
import com.amazon.ec2.RunInstancesResponse;
import com.amazon.ec2.RunInstancesResponseType;
import com.amazon.ec2.RunInstancesType;
import com.amazon.ec2.RunningInstancesItemType;
import com.amazon.ec2.RunningInstancesSetType;
import com.amazon.ec2.SecurityGroupItemType;
import com.amazon.ec2.SecurityGroupSetType;
import com.amazon.ec2.StartInstances;
import com.amazon.ec2.StartInstancesResponse;
import com.amazon.ec2.StartInstancesResponseType;
import com.amazon.ec2.StartInstancesType;
import com.amazon.ec2.StateReasonType;
import com.amazon.ec2.StopInstances;
import com.amazon.ec2.StopInstancesResponse;
import com.amazon.ec2.StopInstancesResponseType;
import com.amazon.ec2.StopInstancesType;
import com.amazon.ec2.TagSetItemType;
import com.amazon.ec2.TagSetType;
import com.amazon.ec2.TerminateInstances;
import com.amazon.ec2.TerminateInstancesResponse;
import com.amazon.ec2.TerminateInstancesResponseType;
import com.amazon.ec2.TerminateInstancesType;
import com.amazon.ec2.UnassignPrivateIpAddresses;
import com.amazon.ec2.UnassignPrivateIpAddressesResponse;
import com.amazon.ec2.UnmonitorInstances;
import com.amazon.ec2.UnmonitorInstancesResponse;
import com.amazon.ec2.UserDataType;
import com.amazon.ec2.UserIdGroupPairSetType;
import com.amazon.ec2.UserIdGroupPairType;
import com.amazon.ec2.ValueSetType;
import com.amazon.ec2.ValueType;

import com.cloud.bridge.service.core.ec2.EC2Address;
import com.cloud.bridge.service.core.ec2.EC2AddressFilterSet;
import com.cloud.bridge.service.core.ec2.EC2AssociateAddress;
import com.cloud.bridge.service.core.ec2.EC2AuthorizeRevokeSecurityGroup;
import com.cloud.bridge.service.core.ec2.EC2AvailabilityZone;
import com.cloud.bridge.service.core.ec2.EC2AvailabilityZonesFilterSet;
import com.cloud.bridge.service.core.ec2.EC2CreateImage;
import com.cloud.bridge.service.core.ec2.EC2CreateImageResponse;
import com.cloud.bridge.service.core.ec2.EC2CreateKeyPair;
import com.cloud.bridge.service.core.ec2.EC2CreateVolume;
import com.cloud.bridge.service.core.ec2.EC2DeleteKeyPair;
import com.cloud.bridge.service.core.ec2.EC2DescribeAddresses;
import com.cloud.bridge.service.core.ec2.EC2DescribeAddressesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeAvailabilityZones;
import com.cloud.bridge.service.core.ec2.EC2DescribeAvailabilityZonesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeImageAttribute;
import com.cloud.bridge.service.core.ec2.EC2DescribeImages;
import com.cloud.bridge.service.core.ec2.EC2DescribeImagesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeInstances;
import com.cloud.bridge.service.core.ec2.EC2DescribeInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeKeyPairs;
import com.cloud.bridge.service.core.ec2.EC2DescribeKeyPairsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeSecurityGroups;
import com.cloud.bridge.service.core.ec2.EC2DescribeSecurityGroupsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeSnapshots;
import com.cloud.bridge.service.core.ec2.EC2DescribeSnapshotsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeTags;
import com.cloud.bridge.service.core.ec2.EC2DescribeTagsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeVolumes;
import com.cloud.bridge.service.core.ec2.EC2DescribeVolumesResponse;
import com.cloud.bridge.service.core.ec2.EC2DisassociateAddress;
import com.cloud.bridge.service.core.ec2.EC2Engine;
import com.cloud.bridge.service.core.ec2.EC2Filter;
import com.cloud.bridge.service.core.ec2.EC2GroupFilterSet;
import com.cloud.bridge.service.core.ec2.EC2Image;
import com.cloud.bridge.service.core.ec2.EC2ImageAttributes;
import com.cloud.bridge.service.core.ec2.EC2ImageAttributes.ImageAttribute;
import com.cloud.bridge.service.core.ec2.EC2ImageFilterSet;
import com.cloud.bridge.service.core.ec2.EC2ImageLaunchPermission;
import com.cloud.bridge.service.core.ec2.EC2ImportKeyPair;
import com.cloud.bridge.service.core.ec2.EC2Instance;
import com.cloud.bridge.service.core.ec2.EC2InstanceFilterSet;
import com.cloud.bridge.service.core.ec2.EC2IpPermission;
import com.cloud.bridge.service.core.ec2.EC2KeyPairFilterSet;
import com.cloud.bridge.service.core.ec2.EC2ModifyImageAttribute;
import com.cloud.bridge.service.core.ec2.EC2ModifyInstanceAttribute;
import com.cloud.bridge.service.core.ec2.EC2PasswordData;
import com.cloud.bridge.service.core.ec2.EC2RebootInstances;
import com.cloud.bridge.service.core.ec2.EC2RegisterImage;
import com.cloud.bridge.service.core.ec2.EC2ReleaseAddress;
import com.cloud.bridge.service.core.ec2.EC2ResourceTag;
import com.cloud.bridge.service.core.ec2.EC2RunInstances;
import com.cloud.bridge.service.core.ec2.EC2RunInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2SSHKeyPair;
import com.cloud.bridge.service.core.ec2.EC2SecurityGroup;
import com.cloud.bridge.service.core.ec2.EC2Snapshot;
import com.cloud.bridge.service.core.ec2.EC2SnapshotFilterSet;
import com.cloud.bridge.service.core.ec2.EC2StartInstances;
import com.cloud.bridge.service.core.ec2.EC2StartInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2StopInstances;
import com.cloud.bridge.service.core.ec2.EC2StopInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2TagKeyValue;
import com.cloud.bridge.service.core.ec2.EC2TagTypeId;
import com.cloud.bridge.service.core.ec2.EC2Tags;
import com.cloud.bridge.service.core.ec2.EC2TagsFilterSet;
import com.cloud.bridge.service.core.ec2.EC2Volume;
import com.cloud.bridge.service.core.ec2.EC2VolumeFilterSet;
import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;
import com.cloud.bridge.util.EC2RestAuth;

public class EC2SoapServiceImpl implements AmazonEC2SkeletonInterface {

    private static EC2Engine engine;

    @SuppressWarnings("static-access")
    public EC2SoapServiceImpl(EC2Engine engine) {
        this.engine = engine;
    }

    public AttachVolumeResponse attachVolume(AttachVolume attachVolume) {
        EC2Volume request = new EC2Volume();
        AttachVolumeType avt = attachVolume.getAttachVolume();

        request.setId(avt.getVolumeId());
        request.setInstanceId(avt.getInstanceId());
        request.setDevice(avt.getDevice());
        return toAttachVolumeResponse(engine.attachVolume(request));
    }

    public AuthorizeSecurityGroupIngressResponse authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngress authorizeSecurityGroupIngress) {
        AuthorizeSecurityGroupIngressType sgit = authorizeSecurityGroupIngress.getAuthorizeSecurityGroupIngress();
        IpPermissionSetType ipPerms = sgit.getIpPermissions();

        EC2AuthorizeRevokeSecurityGroup request = toSecurityGroup(sgit.getAuthorizeSecurityGroupIngressTypeChoice_type0().getGroupName(), ipPerms.getItem());
        return toAuthorizeSecurityGroupIngressResponse(engine.authorizeSecurityGroup(request));
    }

    public RevokeSecurityGroupIngressResponse revokeSecurityGroupIngress(RevokeSecurityGroupIngress revokeSecurityGroupIngress) {
        RevokeSecurityGroupIngressType sgit = revokeSecurityGroupIngress.getRevokeSecurityGroupIngress();
        IpPermissionSetType ipPerms = sgit.getIpPermissions();

        EC2AuthorizeRevokeSecurityGroup request = toSecurityGroup(sgit.getRevokeSecurityGroupIngressTypeChoice_type0().getGroupName(), ipPerms.getItem());
        return toRevokeSecurityGroupIngressResponse(engine.revokeSecurityGroup(request));
    }

    /**
     * Authorize and Revoke Security Group Ingress have the same parameters.
     */
    private EC2AuthorizeRevokeSecurityGroup toSecurityGroup(String groupName, IpPermissionType[] items) {
        EC2AuthorizeRevokeSecurityGroup request = new EC2AuthorizeRevokeSecurityGroup();

        request.setName(groupName);

        for (IpPermissionType ipPerm : items) {
            EC2IpPermission perm = new EC2IpPermission();
            perm.setProtocol(ipPerm.getIpProtocol());
            if (ipPerm.getIpProtocol().equalsIgnoreCase("icmp")) {
                perm.setIcmpType(Integer.toString(ipPerm.getFromPort()));
                perm.setIcmpCode(Integer.toString(ipPerm.getToPort()));
            } else {
                perm.setFromPort(ipPerm.getFromPort());
                perm.setToPort(ipPerm.getToPort());
            }
            UserIdGroupPairSetType groups = ipPerm.getGroups();
            if (null != groups && groups.getItem() != null) {
                UserIdGroupPairType[] groupItems = groups.getItem();
                for (UserIdGroupPairType groupPair : groupItems) {
                    EC2SecurityGroup user = new EC2SecurityGroup();
                    user.setName(groupPair.getGroupName());
                    user.setAccount(groupPair.getUserId());
                    perm.addUser(user);
                }
            }

            IpRangeSetType ranges = ipPerm.getIpRanges();
            if (ranges != null && ranges.getItem() != null) {
                IpRangeItemType[] rangeItems = ranges.getItem();
                for (IpRangeItemType ipRange : rangeItems) {
                    perm.addIpRange(ipRange.getCidrIp());
                    perm.setCIDR(ipRange.getCidrIp());
                }
            }

            request.addIpPermission(perm);
        }
        return request;
    }

    public CreateImageResponse createImage(CreateImage createImage) {
        EC2CreateImage request = new EC2CreateImage();
        CreateImageType cit = createImage.getCreateImage();

        request.setInstanceId(cit.getInstanceId());
        request.setName(cit.getName());
        request.setDescription(cit.getDescription());
        return toCreateImageResponse(engine.createImage(request));
    }

    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroup createSecurityGroup) {
        CreateSecurityGroupType sgt = createSecurityGroup.getCreateSecurityGroup();

        return toCreateSecurityGroupResponse(engine.createSecurityGroup(sgt.getGroupName(), sgt.getGroupDescription()));
    }

    public CreateSnapshotResponse createSnapshot(CreateSnapshot createSnapshot) {
        CreateSnapshotType cst = createSnapshot.getCreateSnapshot();
        return toCreateSnapshotResponse(engine.createSnapshot(cst.getVolumeId()), engine);
    }

    public CreateVolumeResponse createVolume(CreateVolume createVolume) {
        EC2CreateVolume request = new EC2CreateVolume();
        CreateVolumeType cvt = createVolume.getCreateVolume();

        request.setSize(cvt.getSize());
        request.setSnapshotId(cvt.getSnapshotId() != null ? cvt.getSnapshotId() : null);
        request.setZoneName(cvt.getAvailabilityZone());
        return toCreateVolumeResponse(engine.createVolume(request));
    }

    public CreateTagsResponse createTags(CreateTags createTags) {
        EC2Tags request = new EC2Tags();
        ArrayList<String> resourceIdList = new ArrayList<String>();
        Map<String, String> resourceTagList = new HashMap<String, String>();

        CreateTagsType ctt = createTags.getCreateTags();

        ResourceIdSetType resourceIds = ctt.getResourcesSet();
        ResourceTagSetType resourceTags = ctt.getTagSet();

        ResourceIdSetItemType[] resourceIdItems = resourceIds.getItem();
        if (resourceIdItems != null) {
            for (int i = 0; i < resourceIdItems.length; i++)
                resourceIdList.add(resourceIdItems[i].getResourceId());
        }
        request = toResourceTypeAndIds(request, resourceIdList);

        //add resource tag's to the request
        ResourceTagSetItemType[] resourceTagItems = resourceTags.getItem();
        if (resourceTagItems != null) {
            for (int i = 0; i < resourceTagItems.length; i++)
                resourceTagList.put(resourceTagItems[i].getKey(), resourceTagItems[i].getValue());
        }
        request = toResourceTag(request, resourceTagList);

        return toCreateTagsResponse(engine.modifyTags(request, "create"));
    }

    public DeleteTagsResponse deleteTags(DeleteTags deleteTags) {
        EC2Tags request = new EC2Tags();
        ArrayList<String> resourceIdList = new ArrayList<String>();
        Map<String, String> resourceTagList = new HashMap<String, String>();

        DeleteTagsType dtt = deleteTags.getDeleteTags();

        ResourceIdSetType resourceIds = dtt.getResourcesSet();
        DeleteTagsSetType resourceTags = dtt.getTagSet();

        ResourceIdSetItemType[] resourceIdItems = resourceIds.getItem();

        if (resourceIdItems != null) {
            for (int i = 0; i < resourceIdItems.length; i++)
                resourceIdList.add(resourceIdItems[i].getResourceId());
        }
        request = toResourceTypeAndIds(request, resourceIdList);

        //add resource tag's to the request
        DeleteTagsSetItemType[] resourceTagItems = resourceTags.getItem();
        if (resourceTagItems != null) {
            for (int i = 0; i < resourceTagItems.length; i++)
                resourceTagList.put(resourceTagItems[i].getKey(), resourceTagItems[i].getValue());
        }
        request = toResourceTag(request, resourceTagList);

        return toDeleteTagsResponse(engine.modifyTags(request, "delete"));
    }

    public static EC2Tags toResourceTypeAndIds(EC2Tags request, ArrayList<String> resourceIdList) {
        List<String> resourceTypeList = new ArrayList<String>();
        for (String resourceId : resourceIdList) {
            if (!resourceId.contains(":") || resourceId.split(":").length != 2) {
                throw new EC2ServiceException(ClientError.InvalidParameterValue, "Invalid usage. ResourceId format is resource-type:resource-uuid");
            }
            String resourceType = resourceId.split(":")[0];
            if (resourceTypeList.isEmpty())
                resourceTypeList.add(resourceType);
            else {
                Boolean existsInList = false;
                for (String addedResourceType : resourceTypeList) {
                    if (addedResourceType.equalsIgnoreCase(resourceType)) {
                        existsInList = true;
                        break;
                    }
                }
                if (!existsInList)
                    resourceTypeList.add(resourceType);
            }
        }
        for (String resourceType : resourceTypeList) {
            EC2TagTypeId param1 = new EC2TagTypeId();
            param1.setResourceType(resourceType);
            for (String resourceId : resourceIdList) {
                String[] resourceTag = resourceId.split(":");
                if (resourceType.equals(resourceTag[0]))
                    param1.addResourceId(resourceTag[1]);
            }
            request.addResourceType(param1);
        }
        return request;
    }

    public static EC2Tags toResourceTag(EC2Tags request, Map<String, String> resourceTagList) {
        Set<String> resourceTagKeySet = resourceTagList.keySet();
        for (String resourceTagKey : resourceTagKeySet) {
            EC2TagKeyValue param1 = new EC2TagKeyValue();
            param1.setKey(resourceTagKey);
            param1.setValue(resourceTagList.get(resourceTagKey));
            request.addResourceTag(param1);
        }
        return request;
    }

    public DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroup deleteSecurityGroup) {
        DeleteSecurityGroupType sgt = deleteSecurityGroup.getDeleteSecurityGroup();
        return toDeleteSecurityGroupResponse(engine.deleteSecurityGroup(sgt.getGroupName()));
    }

    public DeleteSnapshotResponse deleteSnapshot(DeleteSnapshot deleteSnapshot) {
        DeleteSnapshotType dst = deleteSnapshot.getDeleteSnapshot();
        return toDeleteSnapshotResponse(engine.deleteSnapshot(dst.getSnapshotId()));
    }

    public DeleteVolumeResponse deleteVolume(DeleteVolume deleteVolume) {
        EC2Volume request = new EC2Volume();
        DeleteVolumeType avt = deleteVolume.getDeleteVolume();

        request.setId(avt.getVolumeId());
        return toDeleteVolumeResponse(engine.deleteVolume(request));
    }

    public DeregisterImageResponse deregisterImage(DeregisterImage deregisterImage) {
        DeregisterImageType dit = deregisterImage.getDeregisterImage();
        EC2Image image = new EC2Image();

        image.setId(dit.getImageId());
        return toDeregisterImageResponse(engine.deregisterImage(image));
    }

    public DescribeAvailabilityZonesResponse describeAvailabilityZones(DescribeAvailabilityZones describeAvailabilityZones) {
        EC2DescribeAvailabilityZones request = new EC2DescribeAvailabilityZones();

        DescribeAvailabilityZonesType dazt = describeAvailabilityZones.getDescribeAvailabilityZones();
        DescribeAvailabilityZonesSetType dazs = dazt.getAvailabilityZoneSet();
        DescribeAvailabilityZonesSetItemType[] items = dazs.getItem();
        if (null != items) {  // -> can be empty
            for (int i = 0; i < items.length; i++)
                request.addZone(items[i].getZoneName());
        }

        FilterSetType fst = dazt.getFilterSet();
        if (fst != null) {
            request.setFilterSet(toAvailabiltyZonesFilterSet(fst));
        }

        return toDescribeAvailabilityZonesResponse(engine.describeAvailabilityZones(request));
    }

    /**
     * This only supports a query about description.
     */
    public DescribeImageAttributeResponse describeImageAttribute(DescribeImageAttribute describeImageAttribute) {
        EC2DescribeImageAttribute request = new EC2DescribeImageAttribute();
        DescribeImageAttributeType diat = describeImageAttribute.getDescribeImageAttribute();
        DescribeImageAttributesGroup diag = diat.getDescribeImageAttributesGroup();
        EmptyElementType description = diag.getDescription();
        EmptyElementType launchPermission = diag.getLaunchPermission();

        if (null != description) {
            request.setImageId(diat.getImageId());
            request.setAttribute(ImageAttribute.description);
            return toDescribeImageAttributeResponse(engine.describeImageAttribute(request));
        } else if (launchPermission != null) {
            request.setImageId(diat.getImageId());
            request.setAttribute(ImageAttribute.launchPermission);
            return toDescribeImageAttributeResponse(engine.describeImageAttribute(request));
        } else
            throw new EC2ServiceException(ClientError.Unsupported, "Unsupported - only description or launchPermission supported");
    }

    public DescribeImagesResponse describeImages(DescribeImages describeImages) {
        EC2DescribeImages request = new EC2DescribeImages();
        DescribeImagesType dit = describeImages.getDescribeImages();

        // -> toEC2DescribeImages
        DescribeImagesExecutableBySetType param1 = dit.getExecutableBySet();
        if (null != param1) {
            DescribeImagesExecutableByType[] items1 = param1.getItem();
            if (null != items1) {
                for (int i = 0; i < items1.length; i++)
                    request.addExecutableBySet(items1[i].getUser());
            }
        }
        DescribeImagesInfoType param2 = dit.getImagesSet();
        if (null != param2) {
            DescribeImagesItemType[] items2 = param2.getItem();
            if (null != items2) {
                for (int i = 0; i < items2.length; i++)
                    request.addImageSet(items2[i].getImageId());
            }
        }
        DescribeImagesOwnersType param3 = dit.getOwnersSet();
        if (null != param3) {
            DescribeImagesOwnerType[] items3 = param3.getItem();
            if (null != items3) {
                for (int i = 0; i < items3.length; i++)
                    request.addOwnersSet(items3[i].getOwner());
            }
        }
        FilterSetType fst = dit.getFilterSet();
        if (fst != null) {
            request.setFilterSet(toImageFilterSet(fst));
        }
        return toDescribeImagesResponse(engine.describeImages(request));
    }

    public DescribeInstanceAttributeResponse describeInstanceAttribute(DescribeInstanceAttribute describeInstanceAttribute) {
        EC2DescribeInstances request = new EC2DescribeInstances();
        DescribeInstanceAttributeType diat = describeInstanceAttribute.getDescribeInstanceAttribute();
        DescribeInstanceAttributesGroup diag = diat.getDescribeInstanceAttributesGroup();
        EmptyElementType instanceType = diag.getInstanceType();

        // -> toEC2DescribeInstances
        if (null != instanceType) {
            request.addInstanceId(diat.getInstanceId());
            return toDescribeInstanceAttributeResponse(engine.describeInstances(request));
        }
        throw new EC2ServiceException(ClientError.Unsupported, "Unsupported - only instanceType supported");
    }

    public DescribeInstancesResponse describeInstances(DescribeInstances describeInstances) {
        EC2DescribeInstances request = new EC2DescribeInstances();
        DescribeInstancesType dit = describeInstances.getDescribeInstances();
        FilterSetType fst = dit.getFilterSet();

        // -> toEC2DescribeInstances
        DescribeInstancesInfoType diit = dit.getInstancesSet();
        DescribeInstancesItemType[] items = diit.getItem();
        if (null != items) {  // -> can be empty
            for (int i = 0; i < items.length; i++)
                request.addInstanceId(items[i].getInstanceId());
        }

        if (null != fst)
            request = toInstanceFilterSet(request, fst);

        return toDescribeInstancesResponse(engine.describeInstances(request), engine);
    }

    @Override
    public DescribeAddressesResponse describeAddresses(DescribeAddresses describeAddresses) {
        EC2DescribeAddresses ec2Request = new EC2DescribeAddresses();
        DescribeAddressesType dat = describeAddresses.getDescribeAddresses();

        DescribeAddressesInfoType dait = dat.getPublicIpsSet();
        DescribeAddressesItemType[] items = dait.getItem();
        if (items != null) {  // -> can be empty
            for (DescribeAddressesItemType itemType : items)
                ec2Request.addPublicIp(itemType.getPublicIp());
        }

        FilterSetType fset = dat.getFilterSet();
        if (fset != null) {
            ec2Request.setFilterSet(toAddressFilterSet(fset));
        }

        return toDescribeAddressesResponse(engine.describeAddresses(ec2Request));
    }

    @Override
    public AllocateAddressResponse allocateAddress(AllocateAddress allocateAddress) {
        return toAllocateAddressResponse(engine.allocateAddress());
    }

    @Override
    public ReleaseAddressResponse releaseAddress(ReleaseAddress releaseAddress) {
        EC2ReleaseAddress request = new EC2ReleaseAddress();

        request.setPublicIp(releaseAddress.getReleaseAddress().getReleaseAddressTypeChoice_type0().getPublicIp());

        return toReleaseAddressResponse(engine.releaseAddress(request));
    }

    @Override
    public AssociateAddressResponse associateAddress(AssociateAddress associateAddress) {
        EC2AssociateAddress request = new EC2AssociateAddress();

        request.setPublicIp(associateAddress.getAssociateAddress().getAssociateAddressTypeChoice_type0().getPublicIp());
        request.setInstanceId(associateAddress.getAssociateAddress().getAssociateAddressTypeChoice_type1().getInstanceId());

        return toAssociateAddressResponse(engine.associateAddress(request));
    }

    @Override
    public DisassociateAddressResponse disassociateAddress(DisassociateAddress disassociateAddress) {
        EC2DisassociateAddress request = new EC2DisassociateAddress();

        request.setPublicIp(disassociateAddress.getDisassociateAddress().getPublicIp());

        return toDisassociateAddressResponse(engine.disassociateAddress(request));
    }

    public DescribeSecurityGroupsResponse describeSecurityGroups(DescribeSecurityGroups describeSecurityGroups) {
        EC2DescribeSecurityGroups request = new EC2DescribeSecurityGroups();

        DescribeSecurityGroupsType sgt = describeSecurityGroups.getDescribeSecurityGroups();

        FilterSetType fst = sgt.getFilterSet();

        // -> toEC2DescribeSecurityGroups
        DescribeSecurityGroupsSetType sgst = sgt.getSecurityGroupSet();
        DescribeSecurityGroupsSetItemType[] items = sgst.getItem();
        if (null != items) {  // -> can be empty
            for (DescribeSecurityGroupsSetItemType item : items)
                request.addGroupName(item.getGroupName());
        }

        if (null != fst) {
            request.setFilterSet(toGroupFilterSet(fst));
        }

        return toDescribeSecurityGroupsResponse(engine.describeSecurityGroups(request));
    }

    public DescribeSnapshotsResponse describeSnapshots(DescribeSnapshots describeSnapshots) {
        EC2DescribeSnapshots request = new EC2DescribeSnapshots();
        DescribeSnapshotsType dst = describeSnapshots.getDescribeSnapshots();

        DescribeSnapshotsSetType dsst = dst.getSnapshotSet();
        FilterSetType fst = dst.getFilterSet();

        if (null != dsst) {
            DescribeSnapshotsSetItemType[] items = dsst.getItem();
            if (null != items) {
                for (int i = 0; i < items.length; i++)
                    request.addSnapshotId(items[i].getSnapshotId());
            }
        }

        if (null != fst) {
            String[] timeFilters = new String[1];
            timeFilters[0] = new String("start-time");
            request = toSnapshotFilterSet(request, fst, timeFilters);
        }

        return toDescribeSnapshotsResponse(engine.describeSnapshots(request));
    }

    public DescribeTagsResponse describeTags(DescribeTags decsribeTags) {
        EC2DescribeTags request = new EC2DescribeTags();
        DescribeTagsType dtt = decsribeTags.getDescribeTags();

        FilterSetType fst = dtt.getFilterSet();

        if (fst != null)
            request.setFilterSet(toTagsFilterSet(fst));

        return toDescribeTagsResponse(engine.describeTags(request));
    }

    public DescribeVolumesResponse describeVolumes(DescribeVolumes describeVolumes) {
        EC2DescribeVolumes request = new EC2DescribeVolumes();
        DescribeVolumesType dvt = describeVolumes.getDescribeVolumes();

        DescribeVolumesSetType dvst = dvt.getVolumeSet();
        FilterSetType fst = dvt.getFilterSet();

        if (null != dvst) {
            DescribeVolumesSetItemType[] items = dvst.getItem();
            if (null != items) {
                for (int i = 0; i < items.length; i++)
                    request.addVolumeId(items[i].getVolumeId());
            }
        }

        if (null != fst) {
            String[] timeFilters = new String[2];
            timeFilters[0] = new String("attachment.attach-time");
            timeFilters[1] = new String("create-time");
            request = toVolumeFilterSet(request, fst, timeFilters);
        }

        return toDescribeVolumesResponse(engine.describeVolumes(request), engine);
    }

    public DetachVolumeResponse detachVolume(DetachVolume detachVolume) {
        EC2Volume request = new EC2Volume();
        DetachVolumeType avt = detachVolume.getDetachVolume();

        request.setId(avt.getVolumeId());
        request.setInstanceId(avt.getInstanceId());
        request.setDevice(avt.getDevice());
        return toDetachVolumeResponse(engine.detachVolume(request));
    }

    public ModifyImageAttributeResponse modifyImageAttribute(ModifyImageAttribute modifyImageAttribute) {
        EC2ModifyImageAttribute request = new EC2ModifyImageAttribute();

        ModifyImageAttributeType miat = modifyImageAttribute.getModifyImageAttribute();
        ModifyImageAttributeTypeChoice_type0 item = miat.getModifyImageAttributeTypeChoice_type0();

        AttributeValueType description = item.getDescription();

        LaunchPermissionOperationType launchPermOp = item.getLaunchPermission();

        if (null != description) {
            request.setImageId(miat.getImageId());
            request.setAttribute(ImageAttribute.description);
            request.setDescription(description.getValue());
            return toModifyImageAttributeResponse(engine.modifyImageAttribute(request));
        } else if (launchPermOp != null) {
            request.setImageId(miat.getImageId());
            request.setAttribute(ImageAttribute.launchPermission);
            if (launchPermOp.getAdd() != null) {
                setAccountOrGroupList(launchPermOp.getAdd().getItem(), request, "add");
            } else if (launchPermOp.getRemove() != null) {
                setAccountOrGroupList(launchPermOp.getRemove().getItem(), request, "remove");
            }
            return toModifyImageAttributeResponse(engine.modifyImageAttribute(request));
        }
        throw new EC2ServiceException(ClientError.Unsupported, "Unsupported - can only modify image description or launchPermission");
    }

    public ModifyInstanceAttributeResponse modifyInstanceAttribute(ModifyInstanceAttribute modifyInstanceAttribute) {
        EC2ModifyInstanceAttribute request = new EC2ModifyInstanceAttribute();

        ModifyInstanceAttributeType modifyInstanceAttribute2 = modifyInstanceAttribute.getModifyInstanceAttribute();
        ModifyInstanceAttributeTypeChoice_type0 mia = modifyInstanceAttribute2.getModifyInstanceAttributeTypeChoice_type0();

        request.setInstanceId(modifyInstanceAttribute2.getInstanceId());

        // we only support instanceType and userData
        if (mia.getInstanceType() != null) {
            request.setInstanceType(mia.getInstanceType().getValue());
        } else if (mia.getUserData() != null) {
            request.setUserData(mia.getUserData().getValue());
        } else {
            throw new EC2ServiceException(ClientError.MissingParamter, "Missing required parameter - InstanceType/UserData should be provided");
        }
        return toModifyInstanceAttributeResponse(engine.modifyInstanceAttribute(request));
    }

    private void setAccountOrGroupList(LaunchPermissionItemType[] items, EC2ModifyImageAttribute request, String operation) {
        EC2ImageLaunchPermission launchPermission = new EC2ImageLaunchPermission();

        if (operation.equalsIgnoreCase("add"))
            launchPermission.setLaunchPermOp(EC2ImageLaunchPermission.Operation.add);
        else
            launchPermission.setLaunchPermOp(EC2ImageLaunchPermission.Operation.remove);

        for (LaunchPermissionItemType lpItem : items) {
            if (lpItem.getGroup() != null) {
                launchPermission.addLaunchPermission(lpItem.getGroup());
            } else if (lpItem.getUserId() != null) {
                launchPermission.addLaunchPermission(lpItem.getUserId());
            }
        }

        request.addLaunchPermission(launchPermission);
    }

    /**
     * Did not find a matching service offering so for now we just return disabled
     * for each instance request.  We could verify that all of the specified instances
     * exist to detect an error which would require a listVirtualMachines.
     */
    public MonitorInstancesResponse monitorInstances(MonitorInstances monitorInstances) {
        MonitorInstancesResponse response = new MonitorInstancesResponse();
        MonitorInstancesResponseType param1 = new MonitorInstancesResponseType();
        MonitorInstancesResponseSetType param2 = new MonitorInstancesResponseSetType();

        MonitorInstancesType mit = monitorInstances.getMonitorInstances();
        MonitorInstancesSetType mist = mit.getInstancesSet();
        MonitorInstancesSetItemType[] misit = mist.getItem();

        if (null != misit) {
            for (int i = 0; i < misit.length; i++) {
                String instanceId = misit[i].getInstanceId();
                MonitorInstancesResponseSetItemType param3 = new MonitorInstancesResponseSetItemType();
                param3.setInstanceId(instanceId);
                InstanceMonitoringStateType param4 = new InstanceMonitoringStateType();
                param4.setState("disabled");
                param3.setMonitoring(param4);
                param2.addItem(param3);
            }
        }

        param1.setRequestId(UUID.randomUUID().toString());
        param1.setInstancesSet(param2);
        response.setMonitorInstancesResponse(param1);
        return response;
    }

    public RebootInstancesResponse rebootInstances(RebootInstances rebootInstances) {
        EC2RebootInstances request = new EC2RebootInstances();
        RebootInstancesType rit = rebootInstances.getRebootInstances();

        // -> toEC2StartInstances
        RebootInstancesInfoType rist = rit.getInstancesSet();
        RebootInstancesItemType[] items = rist.getItem();
        if (null != items) {  // -> should not be empty
            for (int i = 0; i < items.length; i++)
                request.addInstanceId(items[i].getInstanceId());
        }
        return toRebootInstancesResponse(engine.rebootInstances(request));
    }

    /**
     * Processes ec2-register
     *
     * @param
     *
     * @see <a href="http://docs.amazonwebservices.com/AWSEC2/2010-11-15/APIReference/index.html?ApiReference-query-RegisterImage.html">RegisterImage</a>
     */
    public RegisterImageResponse registerImage(RegisterImage registerImage) {
        EC2RegisterImage request = new EC2RegisterImage();
        RegisterImageType rit = registerImage.getRegisterImage();

        // -> we redefine the architecture field to hold: "format:zonename:osTypeName",
        //    these are the bare minimum that we need to call the cloud registerTemplate call.
        request.setLocation(rit.getImageLocation());   // -> should be a URL for us
        request.setName(rit.getName());
        request.setDescription(rit.getDescription());
        request.setArchitecture(rit.getArchitecture());
        return toRegisterImageResponse(engine.registerImage(request));
    }

    /**
     * Processes ec2-reset-image-attribute
     *
     * @param resetImageAttribute
     *
     * @see <a href="http://docs.amazonwebservices.com/AWSEC2/2010-11-15/APIReference/index.html?ApiReference-query-ResetInstanceAttribute.html">ResetInstanceAttribute</a>
     */

    public ResetImageAttributeResponse resetImageAttribute(ResetImageAttribute resetImageAttribute) {
        EC2ModifyImageAttribute request = new EC2ModifyImageAttribute();
        ResetImageAttributeType riat = resetImageAttribute.getResetImageAttribute();
        EmptyElementType elementType = riat.getResetImageAttributesGroup().getLaunchPermission();
        if (elementType != null) {
            request.setImageId(riat.getImageId());
            request.setAttribute(ImageAttribute.launchPermission);
            EC2ImageLaunchPermission launchPermission = new EC2ImageLaunchPermission();
            launchPermission.setLaunchPermOp(EC2ImageLaunchPermission.Operation.reset);
            request.addLaunchPermission(launchPermission);
            return toResetImageAttributeResponse(engine.modifyImageAttribute(request));
        }
        throw new EC2ServiceException(ClientError.Unsupported, "Unsupported - can only reset image launchPermission");
    }

    /**
     *  ec2-run-instances
     *
     * @param runInstances
     *
     * @see <a href="http://docs.amazonwebservices.com/AWSEC2/2010-11-15/APIReference/index.html?ApiReference-query-RunInstances.html">RunInstances</a>
     */
    public RunInstancesResponse runInstances(RunInstances runInstances) {
        RunInstancesType rit = runInstances.getRunInstances();
        GroupSetType gst = rit.getGroupSet();
        PlacementRequestType prt = rit.getPlacement();
        UserDataType userData = rit.getUserData();
        String type = rit.getInstanceType();
        String keyName = rit.getKeyName();

        EC2RunInstances request = new EC2RunInstances();

        request.setTemplateId(rit.getImageId());

        if (rit.getMinCount() < 1) {
            throw new EC2ServiceException(ClientError.InvalidParameterValue, "Value of parameter MinCount should be greater than 0");
        } else
            request.setMinCount(rit.getMinCount());

        if (rit.getMaxCount() < 1) {
            throw new EC2ServiceException(ClientError.InvalidParameterValue, "Value of parameter MaxCount should be greater than 0");
        } else
            request.setMaxCount(rit.getMaxCount());

        if (null != type)
            request.setInstanceType(type);
        if (null != prt)
            request.setZoneName(prt.getAvailabilityZone());
        if (null != userData)
            request.setUserData(userData.getData());
        if (null != keyName)
            request.setKeyName(rit.getKeyName());

        // -> we can only support one group per instance
        if (null != gst) {
            GroupItemType[] items = gst.getItem();
            if (null != items) {
                for (int i = 0; i < items.length; i++) {
                    if (items[i].getGroupName() != null) // either SG-name or SG-id can be provided
                        request.addSecuritGroupName(items[i].getGroupName());
                    else
                        request.addSecuritGroupId(items[i].getGroupId());
                }
            }
        }
        return toRunInstancesResponse(engine.runInstances(request), engine);
    }

    public StartInstancesResponse startInstances(StartInstances startInstances) {
        EC2StartInstances request = new EC2StartInstances();
        StartInstancesType sit = startInstances.getStartInstances();

        // -> toEC2StartInstances
        InstanceIdSetType iist = sit.getInstancesSet();
        InstanceIdType[] items = iist.getItem();
        if (null != items) {  // -> should not be empty
            for (int i = 0; i < items.length; i++)
                request.addInstanceId(items[i].getInstanceId());
        }
        return toStartInstancesResponse(engine.startInstances(request));
    }

    public StopInstancesResponse stopInstances(StopInstances stopInstances) {
        EC2StopInstances request = new EC2StopInstances();
        StopInstancesType sit = stopInstances.getStopInstances();
        Boolean force = sit.getForce();

        // -> toEC2StopInstances
        InstanceIdSetType iist = sit.getInstancesSet();
        InstanceIdType[] items = iist.getItem();
        if (null != items) {  // -> should not be empty
            for (int i = 0; i < items.length; i++)
                request.addInstanceId(items[i].getInstanceId());
        }

        if (force)
            request.setForce(sit.getForce());
        return toStopInstancesResponse(engine.stopInstances(request));
    }

    /**
     * Mapping this to the destroyVirtualMachine cloud API concept.
     * This makes sense since when considering the rebootInstances function.   In reboot
     * any terminated instances are left alone.   We will do the same with destroyed instances.
     */
    public TerminateInstancesResponse terminateInstances(TerminateInstances terminateInstances) {
        EC2StopInstances request = new EC2StopInstances();
        TerminateInstancesType sit = terminateInstances.getTerminateInstances();

        // -> toEC2StopInstances
        InstanceIdSetType iist = sit.getInstancesSet();
        InstanceIdType[] items = iist.getItem();
        if (null != items) {  // -> should not be empty
            for (int i = 0; i < items.length; i++)
                request.addInstanceId(items[i].getInstanceId());
        }

        request.setDestroyInstances(true);
        return toTermInstancesResponse(engine.stopInstances(request));
    }

    /**
     * See comment for monitorInstances.
     */
    public UnmonitorInstancesResponse unmonitorInstances(UnmonitorInstances unmonitorInstances) {
        UnmonitorInstancesResponse response = new UnmonitorInstancesResponse();
        MonitorInstancesResponseType param1 = new MonitorInstancesResponseType();
        MonitorInstancesResponseSetType param2 = new MonitorInstancesResponseSetType();

        MonitorInstancesType mit = unmonitorInstances.getUnmonitorInstances();
        MonitorInstancesSetType mist = mit.getInstancesSet();
        MonitorInstancesSetItemType[] items = mist.getItem();

        if (null != items) {
            for (int i = 0; i < items.length; i++) {
                String instanceId = items[i].getInstanceId();
                MonitorInstancesResponseSetItemType param3 = new MonitorInstancesResponseSetItemType();
                param3.setInstanceId(instanceId);
                InstanceMonitoringStateType param4 = new InstanceMonitoringStateType();
                param4.setState("disabled");
                param3.setMonitoring(param4);
                param2.addItem(param3);
            }
        }

        param1.setInstancesSet(param2);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setUnmonitorInstancesResponse(param1);
        return response;
    }

    /**
     * @param modifyInstanceAttribute
     * @return
     */
    public static ModifyInstanceAttributeResponse toModifyInstanceAttributeResponse(Boolean status) {
        ModifyInstanceAttributeResponse miat = new ModifyInstanceAttributeResponse();

        ModifyInstanceAttributeResponseType param = new ModifyInstanceAttributeResponseType();
        param.set_return(status);
        param.setRequestId(UUID.randomUUID().toString());
        miat.setModifyInstanceAttributeResponse(param);
        return miat;
    }

    public static DescribeImageAttributeResponse toDescribeImageAttributeResponse(EC2ImageAttributes engineResponse) {
        DescribeImageAttributeResponse response = new DescribeImageAttributeResponse();
        DescribeImageAttributeResponseType param1 = new DescribeImageAttributeResponseType();

        if (engineResponse != null) {
            DescribeImageAttributeResponseTypeChoice_type0 param2 = new DescribeImageAttributeResponseTypeChoice_type0();

            if (engineResponse.getIsPublic()) {
                LaunchPermissionListType param3 = new LaunchPermissionListType();
                LaunchPermissionItemType param4 = new LaunchPermissionItemType();
                param4.setGroup("all");
                param3.addItem(param4);
                param2.setLaunchPermission(param3);
            } else if (engineResponse.getAccountNamesWithLaunchPermission() != null) {
                LaunchPermissionListType param3 = new LaunchPermissionListType();
                for (String accountName : engineResponse.getAccountNamesWithLaunchPermission()) {
                    LaunchPermissionItemType param4 = new LaunchPermissionItemType();
                    param4.setUserId(accountName);
                    param3.addItem(param4);
                }
                param2.setLaunchPermission(param3);

            } else if (engineResponse.getDescription() != null) {
                NullableAttributeValueType param3 = new NullableAttributeValueType();
                param3.setValue(engineResponse.getDescription());
                param2.setDescription(param3);
            }

            param1.setDescribeImageAttributeResponseTypeChoice_type0(param2);
            param1.setImageId(engineResponse.getImageId());
        }

        param1.setRequestId(UUID.randomUUID().toString());
        response.setDescribeImageAttributeResponse(param1);
        return response;
    }

    public static ModifyImageAttributeResponse toModifyImageAttributeResponse(boolean engineResponse) {
        ModifyImageAttributeResponse response = new ModifyImageAttributeResponse();
        ModifyImageAttributeResponseType param1 = new ModifyImageAttributeResponseType();

        param1.set_return(engineResponse);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setModifyImageAttributeResponse(param1);
        return response;
    }

    public static ResetImageAttributeResponse toResetImageAttributeResponse(boolean engineResponse) {
        ResetImageAttributeResponse response = new ResetImageAttributeResponse();
        ResetImageAttributeResponseType param1 = new ResetImageAttributeResponseType();

        param1.set_return(engineResponse);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setResetImageAttributeResponse(param1);
        return response;
    }

    public static DescribeImagesResponse toDescribeImagesResponse(EC2DescribeImagesResponse engineResponse) {
        DescribeImagesResponse response = new DescribeImagesResponse();
        DescribeImagesResponseType param1 = new DescribeImagesResponseType();
        DescribeImagesResponseInfoType param2 = new DescribeImagesResponseInfoType();

        EC2Image[] images = engineResponse.getImageSet();
        for (int i = 0; i < images.length; i++) {
            String accountName = images[i].getAccountName();
            String domainId = images[i].getDomainId();
            String ownerId = domainId + ":" + accountName;

            DescribeImagesResponseItemType param3 = new DescribeImagesResponseItemType();
            param3.setImageId(images[i].getId());
            param3.setImageLocation("");
            param3.setImageState(images[i].getState());
            param3.setImageOwnerId(ownerId);
            param3.setIsPublic(images[i].getIsPublic());

            ProductCodesSetType param4 = new ProductCodesSetType();
            ProductCodesSetItemType param5 = new ProductCodesSetItemType();
            param5.setProductCode("");
            param5.setType("");
            param4.addItem(param5);
            param3.setProductCodes(param4);

            String description = images[i].getDescription();
            param3.setDescription((null == description ? "" : description));

            param3.setArchitecture(images[i].getArchitecture());

            param3.setImageType(images[i].getImageType());
            param3.setKernelId("");
            param3.setRamdiskId("");
            param3.setPlatform("");
            param3.setHypervisor(images[i].getHypervisor());

            StateReasonType param6 = new StateReasonType();
            param6.setCode("");
            param6.setMessage("");
            param3.setStateReason(param6);

            param3.setImageOwnerAlias("");
            param3.setName(images[i].getName());
            param3.setRootDeviceType("");
            param3.setRootDeviceName("");

            BlockDeviceMappingType param7 = new BlockDeviceMappingType();
            BlockDeviceMappingItemType param8 = new BlockDeviceMappingItemType();
            BlockDeviceMappingItemTypeChoice_type0 param9 = new BlockDeviceMappingItemTypeChoice_type0();
            param8.setDeviceName("");
            param9.setVirtualName("");
            EbsBlockDeviceType param10 = new EbsBlockDeviceType();
            param10.setSnapshotId("");
            param10.setVolumeSize(0);
            param10.setDeleteOnTermination(false);
            param9.setEbs(param10);
            param8.setBlockDeviceMappingItemTypeChoice_type0(param9);
            param7.addItem(param8);

            param3.setBlockDeviceMapping(param7);

            EC2TagKeyValue[] tags = images[i].getResourceTags();
            param3.setTagSet(setResourceTags(tags));

            param2.addItem(param3);
        }

        param1.setImagesSet(param2);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDescribeImagesResponse(param1);
        return response;
    }

    public static CreateImageResponse toCreateImageResponse(EC2CreateImageResponse engineResponse) {
        CreateImageResponse response = new CreateImageResponse();
        CreateImageResponseType param1 = new CreateImageResponseType();

        param1.setImageId(engineResponse.getId());
        param1.setRequestId(UUID.randomUUID().toString());
        response.setCreateImageResponse(param1);
        return response;
    }

    public static RegisterImageResponse toRegisterImageResponse(EC2CreateImageResponse engineResponse) {
        RegisterImageResponse response = new RegisterImageResponse();
        RegisterImageResponseType param1 = new RegisterImageResponseType();

        param1.setImageId(engineResponse.getId());
        param1.setRequestId(UUID.randomUUID().toString());
        response.setRegisterImageResponse(param1);
        return response;
    }

    public static DeregisterImageResponse toDeregisterImageResponse(boolean engineResponse) {
        DeregisterImageResponse response = new DeregisterImageResponse();
        DeregisterImageResponseType param1 = new DeregisterImageResponseType();

        param1.set_return(engineResponse);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDeregisterImageResponse(param1);
        return response;
    }

    // filtersets
    private EC2AddressFilterSet toAddressFilterSet(FilterSetType fst) {
        EC2AddressFilterSet vfs = new EC2AddressFilterSet();

        FilterType[] items = fst.getItem();
        if (items != null) {
            // -> each filter can have one or more values associated with it
            for (FilterType item : items) {
                EC2Filter oneFilter = new EC2Filter();
                String filterName = item.getName();
                oneFilter.setName(filterName);

                ValueSetType vst = item.getValueSet();
                ValueType[] valueItems = vst.getItem();
                for (ValueType valueItem : valueItems) {
                    oneFilter.addValueEncoded(valueItem.getValue());
                }
                vfs.addFilter(oneFilter);
            }
        }
        return vfs;
    }

    private EC2KeyPairFilterSet toKeyPairFilterSet(FilterSetType fst) {
        EC2KeyPairFilterSet vfs = new EC2KeyPairFilterSet();

        FilterType[] items = fst.getItem();
        if (items != null) {
            // -> each filter can have one or more values associated with it
            for (FilterType item : items) {
                EC2Filter oneFilter = new EC2Filter();
                String filterName = item.getName();
                oneFilter.setName(filterName);

                ValueSetType vst = item.getValueSet();
                ValueType[] valueItems = vst.getItem();
                for (ValueType valueItem : valueItems) {
                    oneFilter.addValueEncoded(valueItem.getValue());
                }
                vfs.addFilter(oneFilter);
            }
        }
        return vfs;
    }

    private EC2DescribeVolumes toVolumeFilterSet(EC2DescribeVolumes request, FilterSetType fst, String[] timeStrs) {
        EC2VolumeFilterSet vfs = new EC2VolumeFilterSet();
        boolean timeFilter = false;

        FilterType[] items = fst.getItem();
        if (null != items) {
            // -> each filter can have one or more values associated with it
            for (int j = 0; j < items.length; j++) {
                String filterName = items[j].getName();
                ValueSetType vst = items[j].getValueSet();
                ValueType[] valueItems = vst.getItem();

                if (filterName.startsWith("tag:")) {
                    String key = filterName.split(":")[1];
                    for (ValueType valueItem : valueItems) {
                        EC2TagKeyValue tag = new EC2TagKeyValue();
                        tag.setKey(key);
                        tag.setValue(valueItem.getValue());
                        request.addResourceTag(tag);
                    }
                } else {
                    EC2Filter oneFilter = new EC2Filter();
                    oneFilter.setName(filterName);

                    // -> is the filter one of the xsd:dateTime filters?
                    timeFilter = false;
                    for (int m = 0; m < timeStrs.length; m++) {
                        timeFilter = filterName.equalsIgnoreCase(timeStrs[m]);
                        if (timeFilter)
                            break;
                    }

                    for (int k = 0; k < valueItems.length; k++) {
                        // -> time values are not encoded as regexes
                        if (timeFilter)
                            oneFilter.addValue(valueItems[k].getValue());
                        else
                            oneFilter.addValueEncoded(valueItems[k].getValue());
                    }
                    vfs.addFilter(oneFilter);
                }
            }
            request.setFilterSet(vfs);
        }
        return request;
    }

    private EC2DescribeSnapshots toSnapshotFilterSet(EC2DescribeSnapshots request, FilterSetType fst, String[] timeStrs) {
        EC2SnapshotFilterSet sfs = new EC2SnapshotFilterSet();
        boolean timeFilter = false;

        FilterType[] items = fst.getItem();
        if (null != items) {
            // -> each filter can have one or more values associated with it
            for (int j = 0; j < items.length; j++) {
                String filterName = items[j].getName();
                ValueSetType vst = items[j].getValueSet();
                ValueType[] valueItems = vst.getItem();

                if (filterName.startsWith("tag:")) {
                    String key = filterName.split(":")[1];
                    for (ValueType valueItem : valueItems) {
                        EC2TagKeyValue tag = new EC2TagKeyValue();
                        tag.setKey(key);
                        tag.setValue(valueItem.getValue());
                        request.addResourceTag(tag);
                    }
                } else {
                    EC2Filter oneFilter = new EC2Filter();
                    oneFilter.setName(filterName);

                    // -> is the filter one of the xsd:dateTime filters?
                    timeFilter = false;
                    for (int m = 0; m < timeStrs.length; m++) {
                        timeFilter = filterName.equalsIgnoreCase(timeStrs[m]);
                        if (timeFilter)
                            break;
                    }

                    for (int k = 0; k < valueItems.length; k++) {
                        // -> time values are not encoded as regexes
                        if (timeFilter)
                            oneFilter.addValue(valueItems[k].getValue());
                        else
                            oneFilter.addValueEncoded(valueItems[k].getValue());
                    }
                    sfs.addFilter(oneFilter);
                }
            }
            request.setFilterSet(sfs);
        }
        return request;
    }

    // TODO make these filter set functions use generics
    private EC2GroupFilterSet toGroupFilterSet(FilterSetType fst) {
        EC2GroupFilterSet gfs = new EC2GroupFilterSet();

        FilterType[] items = fst.getItem();
        if (null != items) {
            // -> each filter can have one or more values associated with it
            for (int j = 0; j < items.length; j++) {
                EC2Filter oneFilter = new EC2Filter();
                String filterName = items[j].getName();
                oneFilter.setName(filterName);

                ValueSetType vst = items[j].getValueSet();
                ValueType[] valueItems = vst.getItem();
                for (int k = 0; k < valueItems.length; k++) {
                    oneFilter.addValueEncoded(valueItems[k].getValue());
                }
                gfs.addFilter(oneFilter);
            }
        }
        return gfs;
    }

    private EC2DescribeInstances toInstanceFilterSet(EC2DescribeInstances request, FilterSetType fst) {
        EC2InstanceFilterSet ifs = new EC2InstanceFilterSet();

        FilterType[] items = fst.getItem();
        if (null != items) {
            // -> each filter can have one or more values associated with it
            for (int j = 0; j < items.length; j++) {
                String filterName = items[j].getName();
                ValueSetType vst = items[j].getValueSet();
                ValueType[] valueItems = vst.getItem();

                if (filterName.startsWith("tag:")) {
                    String key = filterName.split(":")[1];
                    for (ValueType valueItem : valueItems) {
                        EC2TagKeyValue tag = new EC2TagKeyValue();
                        tag.setKey(key);
                        tag.setValue(valueItem.getValue());
                        request.addResourceTag(tag);
                    }
                } else {
                    EC2Filter oneFilter = new EC2Filter();
                    oneFilter.setName(filterName);
                    for (int k = 0; k < valueItems.length; k++)
                        oneFilter.addValueEncoded(valueItems[k].getValue());
                    ifs.addFilter(oneFilter);
                }
            }
            request.setFilterSet(ifs);
        }
        return request;
    }

    private EC2AvailabilityZonesFilterSet toAvailabiltyZonesFilterSet(FilterSetType fst) {
        EC2AvailabilityZonesFilterSet azfs = new EC2AvailabilityZonesFilterSet();

        FilterType[] items = fst.getItem();
        if (items != null) {
            for (FilterType item : items) {
                EC2Filter oneFilter = new EC2Filter();
                String filterName = item.getName();
                oneFilter.setName(filterName);

                ValueSetType vft = item.getValueSet();
                ValueType[] valueItems = vft.getItem();
                for (ValueType valueItem : valueItems) {
                    oneFilter.addValueEncoded(valueItem.getValue());
                }
                azfs.addFilter(oneFilter);
            }
        }
        return azfs;
    }

    private EC2TagsFilterSet toTagsFilterSet(FilterSetType fst) {
        EC2TagsFilterSet tfs = new EC2TagsFilterSet();

        FilterType[] items = fst.getItem();
        if (items != null) {
            for (FilterType item : items) {
                EC2Filter oneFilter = new EC2Filter();
                String filterName = item.getName();
                oneFilter.setName(filterName);

                ValueSetType vft = item.getValueSet();
                ValueType[] valueItems = vft.getItem();
                for (ValueType valueItem : valueItems) {
                    oneFilter.addValueEncoded(valueItem.getValue());
                }
                tfs.addFilter(oneFilter);
            }
        }
        return tfs;
    }

    private EC2ImageFilterSet toImageFilterSet(FilterSetType fst) {
        EC2ImageFilterSet ifs = new EC2ImageFilterSet();

        FilterType[] items = fst.getItem();
        if (items != null) {
            for (FilterType item : items) {
                EC2Filter oneFilter = new EC2Filter();
                String filterName = item.getName();
                oneFilter.setName(filterName);

                ValueSetType vft = item.getValueSet();
                ValueType[] valueItems = vft.getItem();
                for (ValueType valueItem : valueItems) {
                    oneFilter.addValueEncoded(valueItem.getValue());
                }
                ifs.addFilter(oneFilter);
            }
        }
        return ifs;
    }

    // toMethods
    public static DescribeVolumesResponse toDescribeVolumesResponse(EC2DescribeVolumesResponse engineResponse, EC2Engine engine) {
        DescribeVolumesResponse response = new DescribeVolumesResponse();
        DescribeVolumesResponseType param1 = new DescribeVolumesResponseType();
        DescribeVolumesSetResponseType param2 = new DescribeVolumesSetResponseType();

        EC2Volume[] volumes = engineResponse.getVolumeSet();
        for (EC2Volume vol : volumes) {
            DescribeVolumesSetItemResponseType param3 = new DescribeVolumesSetItemResponseType();
            param3.setVolumeId(vol.getId().toString());

            Long volSize = new Long(vol.getSize());
            param3.setSize(volSize.toString());
            String snapId = vol.getSnapshotId() != null ? vol.getSnapshotId().toString() : "";
            param3.setSnapshotId(snapId);
            param3.setAvailabilityZone(vol.getZoneName());
            param3.setStatus(vol.getState());
            param3.setVolumeType("standard");

            // -> CloudStack seems to have issues with timestamp formats so just in case
            Calendar cal = EC2RestAuth.parseDateString(vol.getCreated());
            if (cal == null) {
                cal = Calendar.getInstance();
                cal.set(1970, 1, 1);
            }
            param3.setCreateTime(cal);

            AttachmentSetResponseType param4 = new AttachmentSetResponseType();
            if (null != vol.getInstanceId()) {
                AttachmentSetItemResponseType param5 = new AttachmentSetItemResponseType();
                param5.setVolumeId(vol.getId().toString());
                param5.setInstanceId(vol.getInstanceId().toString());
                String devicePath = engine.cloudDeviceIdToDevicePath(vol.getHypervisor(), vol.getDeviceId());
                param5.setDevice(devicePath);
                param5.setStatus(vol.getAttachmentState());
                if (vol.getAttached() == null) {
                    param5.setAttachTime(cal);
                } else {
                    Calendar attachTime = EC2RestAuth.parseDateString(vol.getAttached());
                    param5.setAttachTime(attachTime);
                }
                param5.setDeleteOnTermination(false);
                param4.addItem(param5);
            }

            param3.setAttachmentSet(param4);

            EC2TagKeyValue[] tags = vol.getResourceTags();
            param3.setTagSet(setResourceTags(tags));
            param2.addItem(param3);
        }
        param1.setVolumeSet(param2);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDescribeVolumesResponse(param1);
        return response;
    }

    public static DescribeInstanceAttributeResponse toDescribeInstanceAttributeResponse(EC2DescribeInstancesResponse engineResponse) {
        DescribeInstanceAttributeResponse response = new DescribeInstanceAttributeResponse();
        DescribeInstanceAttributeResponseType param1 = new DescribeInstanceAttributeResponseType();

        EC2Instance[] instanceSet = engineResponse.getInstanceSet();
        if (0 < instanceSet.length) {
            DescribeInstanceAttributeResponseTypeChoice_type0 param2 = new DescribeInstanceAttributeResponseTypeChoice_type0();
            NullableAttributeValueType param3 = new NullableAttributeValueType();
            param3.setValue(instanceSet[0].getServiceOffering());
            param2.setInstanceType(param3);
            param1.setDescribeInstanceAttributeResponseTypeChoice_type0(param2);
            param1.setInstanceId(instanceSet[0].getId());
        }
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDescribeInstanceAttributeResponse(param1);
        return response;
    }

    public static DescribeInstancesResponse toDescribeInstancesResponse(EC2DescribeInstancesResponse engineResponse, EC2Engine engine) {
        DescribeInstancesResponse response = new DescribeInstancesResponse();
        DescribeInstancesResponseType param1 = new DescribeInstancesResponseType();
        ReservationSetType param2 = new ReservationSetType();

        EC2Instance[] instances = engineResponse.getInstanceSet();

        for (EC2Instance inst : instances) {
            String accountName = inst.getAccountName();
            String domainId = inst.getDomainId();
            String ownerId = domainId + ":" + accountName;

            ReservationInfoType param3 = new ReservationInfoType();

            param3.setReservationId(inst.getId());   // -> an id we could track down if needed
            param3.setOwnerId(ownerId);
            param3.setRequesterId("");

            GroupSetType param4 = new GroupSetType();

            EC2SecurityGroup[] groups = inst.getGroupSet();
            if (null == groups || 0 == groups.length) {
                GroupItemType param5 = new GroupItemType();
                param5.setGroupId("");
                param5.setGroupName("");
                param4.addItem(param5);
            } else {
                for (EC2SecurityGroup group : groups) {
                    GroupItemType param5 = new GroupItemType();
                    param5.setGroupId(group.getId());
                    param5.setGroupName("");
                    param4.addItem(param5);
                }
            }
            param3.setGroupSet(param4);

            RunningInstancesSetType param6 = new RunningInstancesSetType();
            RunningInstancesItemType param7 = new RunningInstancesItemType();

            param7.setInstanceId(inst.getId());
            param7.setImageId(inst.getTemplateId());

            InstanceStateType param8 = new InstanceStateType();
            param8.setCode(toAmazonCode(inst.getState()));
            param8.setName(toAmazonStateName(inst.getState()));
            param7.setInstanceState(param8);

            param7.setPrivateDnsName("");
            param7.setDnsName("");
            param7.setReason("");
            param7.setKeyName(inst.getKeyPairName());
            param7.setAmiLaunchIndex(null);
            param7.setInstanceType(inst.getServiceOffering());

            ProductCodesSetType param9 = new ProductCodesSetType();
            ProductCodesSetItemType param10 = new ProductCodesSetItemType();
            param10.setProductCode("");
            param10.setType("");
            param9.addItem(param10);
            param7.setProductCodes(param9);

            Calendar cal = inst.getCreated();
            if (null == cal) {
                cal = Calendar.getInstance();
//                 cal.set( 1970, 1, 1 );
            }
            param7.setLaunchTime(cal);

            PlacementResponseType param11 = new PlacementResponseType();
            param11.setAvailabilityZone(inst.getZoneName());
            param11.setGroupName("");
            param7.setPlacement(param11);
            param7.setKernelId("");
            param7.setRamdiskId("");
            param7.setPlatform("");

            InstanceMonitoringStateType param12 = new InstanceMonitoringStateType();
            param12.setState("");
            param7.setMonitoring(param12);
            param7.setSubnetId("");
            param7.setVpcId("");
//            String ipAddr = inst.getPrivateIpAddress();
//            param7.setPrivateIpAddress((null != ipAddr ? ipAddr : ""));
            param7.setPrivateIpAddress(inst.getPrivateIpAddress());
            param7.setIpAddress(inst.getIpAddress());

            StateReasonType param13 = new StateReasonType();
            param13.setCode("");
            param13.setMessage("");
            param7.setStateReason(param13);
            param7.setArchitecture("");
            param7.setRootDeviceType("");
            String devicePath = engine.cloudDeviceIdToDevicePath(inst.getHypervisor(), inst.getRootDeviceId());
            param7.setRootDeviceName(devicePath);

            GroupSetType param14 = new GroupSetType();
            GroupItemType param15 = new GroupItemType(); // VPC security group
            param15.setGroupName("");
            param15.setGroupName("");
            param14.addItem(param15);
            param7.setGroupSet(param14);

            param7.setInstanceLifecycle("");
            param7.setSpotInstanceRequestId("");
            param7.setHypervisor(inst.getHypervisor());

            EC2TagKeyValue[] tags = inst.getResourceTags();
            param7.setTagSet(setResourceTags(tags));

            param6.addItem(param7);
            param3.setInstancesSet(param6);
            param2.addItem(param3);
        }
        param1.setReservationSet(param2);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDescribeInstancesResponse(param1);
        return response;
    }

    public static DescribeAddressesResponse toDescribeAddressesResponse(EC2DescribeAddressesResponse engineResponse) {
        List<DescribeAddressesResponseItemType> items = new ArrayList<DescribeAddressesResponseItemType>();
        EC2Address[] addressSet = engineResponse.getAddressSet();

        for (EC2Address addr : addressSet) {
            DescribeAddressesResponseItemType item = new DescribeAddressesResponseItemType();
            item.setPublicIp(addr.getIpAddress());
            item.setInstanceId(addr.getAssociatedInstanceId());
            items.add(item);
        }
        DescribeAddressesResponseInfoType descAddrRespInfoType = new DescribeAddressesResponseInfoType();
        descAddrRespInfoType.setItem(items.toArray(new DescribeAddressesResponseItemType[0]));

        DescribeAddressesResponseType descAddrRespType = new DescribeAddressesResponseType();
        descAddrRespType.setRequestId(UUID.randomUUID().toString());
        descAddrRespType.setAddressesSet(descAddrRespInfoType);

        DescribeAddressesResponse descAddrResp = new DescribeAddressesResponse();
        descAddrResp.setDescribeAddressesResponse(descAddrRespType);

        return descAddrResp;
    }

    public static AllocateAddressResponse toAllocateAddressResponse(final EC2Address ec2Address) {
        AllocateAddressResponse response = new AllocateAddressResponse();
        AllocateAddressResponseType param1 = new AllocateAddressResponseType();

        param1.setPublicIp(ec2Address.getIpAddress());
        param1.setDomain("standard");
        param1.setAllocationId("");
        param1.setRequestId(UUID.randomUUID().toString());
        response.setAllocateAddressResponse(param1);
        return response;
    }

    public static ReleaseAddressResponse toReleaseAddressResponse(final boolean result) {
        ReleaseAddressResponse response = new ReleaseAddressResponse();
        ReleaseAddressResponseType param1 = new ReleaseAddressResponseType();

        param1.set_return(result);
        param1.setRequestId(UUID.randomUUID().toString());

        response.setReleaseAddressResponse(param1);
        return response;
    }

    public static AssociateAddressResponse toAssociateAddressResponse(final boolean result) {
        AssociateAddressResponse response = new AssociateAddressResponse();
        AssociateAddressResponseType param1 = new AssociateAddressResponseType();

        param1.setAssociationId("");
        param1.setRequestId(UUID.randomUUID().toString());
        param1.set_return(result);

        response.setAssociateAddressResponse(param1);
        return response;
    }

    public static DisassociateAddressResponse toDisassociateAddressResponse(final boolean result) {
        DisassociateAddressResponse response = new DisassociateAddressResponse();
        DisassociateAddressResponseType param1 = new DisassociateAddressResponseType();

        param1.setRequestId(UUID.randomUUID().toString());
        param1.set_return(result);

        response.setDisassociateAddressResponse(param1);
        return response;
    }

    /**
     * Map our cloud state values into what Amazon defines.
     * Where are the values that can be returned by our cloud api defined?
     *
     * @param cloudState
     * @return
     */
    public static int toAmazonCode(String cloudState) {
        if (null == cloudState)
            return 48;

        if (cloudState.equalsIgnoreCase("Destroyed"))
            return 48;
        else if (cloudState.equalsIgnoreCase("Stopped"))
            return 80;
        else if (cloudState.equalsIgnoreCase("Running"))
            return 16;
        else if (cloudState.equalsIgnoreCase("Starting"))
            return 0;
        else if (cloudState.equalsIgnoreCase("Stopping"))
            return 64;
        else if (cloudState.equalsIgnoreCase("Error"))
            return 1;
        else if (cloudState.equalsIgnoreCase("Expunging"))
            return 48;
        else
            return 16;
    }

    public static String toAmazonStateName(String cloudState) {
        if (null == cloudState)
            return new String("terminated");

        if (cloudState.equalsIgnoreCase("Destroyed"))
            return new String("terminated");
        else if (cloudState.equalsIgnoreCase("Stopped"))
            return new String("stopped");
        else if (cloudState.equalsIgnoreCase("Running"))
            return new String("running");
        else if (cloudState.equalsIgnoreCase("Starting"))
            return new String("pending");
        else if (cloudState.equalsIgnoreCase("Stopping"))
            return new String("stopping");
        else if (cloudState.equalsIgnoreCase("Error"))
            return new String("error");
        else if (cloudState.equalsIgnoreCase("Expunging"))
            return new String("terminated");
        else
            return new String("running");
    }

    public static StopInstancesResponse toStopInstancesResponse(EC2StopInstancesResponse engineResponse) {
        StopInstancesResponse response = new StopInstancesResponse();
        StopInstancesResponseType param1 = new StopInstancesResponseType();
        InstanceStateChangeSetType param2 = new InstanceStateChangeSetType();

        EC2Instance[] instances = engineResponse.getInstanceSet();
        for (int i = 0; i < instances.length; i++) {
            InstanceStateChangeType param3 = new InstanceStateChangeType();
            param3.setInstanceId(instances[i].getId());

            InstanceStateType param4 = new InstanceStateType();
            param4.setCode(toAmazonCode(instances[i].getState()));
            param4.setName(toAmazonStateName(instances[i].getState()));
            param3.setCurrentState(param4);

            InstanceStateType param5 = new InstanceStateType();
            param5.setCode(toAmazonCode(instances[i].getPreviousState()));
            param5.setName(toAmazonStateName(instances[i].getPreviousState()));
            param3.setPreviousState(param5);

            param2.addItem(param3);
        }

        param1.setRequestId(UUID.randomUUID().toString());
        param1.setInstancesSet(param2);
        response.setStopInstancesResponse(param1);
        return response;
    }

    public static StartInstancesResponse toStartInstancesResponse(EC2StartInstancesResponse engineResponse) {
        StartInstancesResponse response = new StartInstancesResponse();
        StartInstancesResponseType param1 = new StartInstancesResponseType();
        InstanceStateChangeSetType param2 = new InstanceStateChangeSetType();

        EC2Instance[] instances = engineResponse.getInstanceSet();
        for (int i = 0; i < instances.length; i++) {
            InstanceStateChangeType param3 = new InstanceStateChangeType();
            param3.setInstanceId(instances[i].getId());

            InstanceStateType param4 = new InstanceStateType();
            param4.setCode(toAmazonCode(instances[i].getState()));
            param4.setName(toAmazonStateName(instances[i].getState()));
            param3.setCurrentState(param4);

            InstanceStateType param5 = new InstanceStateType();
            param5.setCode(toAmazonCode(instances[i].getPreviousState()));
            param5.setName(toAmazonStateName(instances[i].getPreviousState()));
            param3.setPreviousState(param5);

            param2.addItem(param3);
        }

        param1.setRequestId(UUID.randomUUID().toString());
        param1.setInstancesSet(param2);
        response.setStartInstancesResponse(param1);
        return response;
    }

    public static TerminateInstancesResponse toTermInstancesResponse(EC2StopInstancesResponse engineResponse) {
        TerminateInstancesResponse response = new TerminateInstancesResponse();
        TerminateInstancesResponseType param1 = new TerminateInstancesResponseType();
        InstanceStateChangeSetType param2 = new InstanceStateChangeSetType();

        EC2Instance[] instances = engineResponse.getInstanceSet();
        for (int i = 0; i < instances.length; i++) {
            InstanceStateChangeType param3 = new InstanceStateChangeType();
            param3.setInstanceId(instances[i].getId());

            InstanceStateType param4 = new InstanceStateType();
            param4.setCode(toAmazonCode(instances[i].getState()));
            param4.setName(toAmazonStateName(instances[i].getState()));
            param3.setCurrentState(param4);

            InstanceStateType param5 = new InstanceStateType();
            param5.setCode(toAmazonCode(instances[i].getPreviousState()));
            param5.setName(toAmazonStateName(instances[i].getPreviousState()));
            param3.setPreviousState(param5);

            param2.addItem(param3);
        }

        param1.setRequestId(UUID.randomUUID().toString());
        param1.setInstancesSet(param2);
        response.setTerminateInstancesResponse(param1);
        return response;
    }

    public static RebootInstancesResponse toRebootInstancesResponse(boolean engineResponse) {
        RebootInstancesResponse response = new RebootInstancesResponse();
        RebootInstancesResponseType param1 = new RebootInstancesResponseType();

        param1.setRequestId(UUID.randomUUID().toString());
        param1.set_return(engineResponse);
        response.setRebootInstancesResponse(param1);
        return response;
    }

    public static RunInstancesResponse toRunInstancesResponse(EC2RunInstancesResponse engineResponse, EC2Engine engine) {
        RunInstancesResponse response = new RunInstancesResponse();
        RunInstancesResponseType param1 = new RunInstancesResponseType();

        param1.setReservationId("");

        RunningInstancesSetType param6 = new RunningInstancesSetType();
        EC2Instance[] instances = engineResponse.getInstanceSet();
        for (EC2Instance inst : instances) {
            RunningInstancesItemType param7 = new RunningInstancesItemType();
            param7.setInstanceId(inst.getId());
            param7.setImageId(inst.getTemplateId());

            String accountName = inst.getAccountName();
            String domainId = inst.getDomainId();
            String ownerId = domainId + ":" + accountName;

            param1.setOwnerId(ownerId);

            EC2SecurityGroup[] groups = inst.getGroupSet();
            GroupSetType param2 = new GroupSetType();
            if (null == groups || 0 == groups.length) {
                GroupItemType param3 = new GroupItemType();
                param3.setGroupId("");
                param2.addItem(param3);
            } else {
                for (EC2SecurityGroup group : groups) {
                    GroupItemType param3 = new GroupItemType();
                    param3.setGroupId(group.getId());
                    param2.addItem(param3);
                }
            }
            param1.setGroupSet(param2);

            InstanceStateType param8 = new InstanceStateType();
            param8.setCode(toAmazonCode(inst.getState()));
            param8.setName(toAmazonStateName(inst.getState()));
            param7.setInstanceState(param8);

            param7.setPrivateDnsName("");
            param7.setDnsName("");
            param7.setReason("");
            param7.setKeyName(inst.getKeyPairName());
            param7.setAmiLaunchIndex(null);

            ProductCodesSetType param9 = new ProductCodesSetType();
            ProductCodesSetItemType param10 = new ProductCodesSetItemType();
            param10.setProductCode("");
            param10.setType("");
            param9.addItem(param10);
            param7.setProductCodes(param9);

            param7.setInstanceType(inst.getServiceOffering());
            // -> CloudStack seems to have issues with timestamp formats so just in case
            Calendar cal = inst.getCreated();
            if (null == cal) {
                cal = Calendar.getInstance();
                cal.set(1970, 1, 1);
            }
            param7.setLaunchTime(cal);

            PlacementResponseType param11 = new PlacementResponseType();
            param11.setAvailabilityZone(inst.getZoneName());
            param7.setPlacement(param11);

            param7.setKernelId("");
            param7.setRamdiskId("");
            param7.setPlatform("");

            InstanceMonitoringStateType param12 = new InstanceMonitoringStateType();
            param12.setState("");
            param7.setMonitoring(param12);
            param7.setSubnetId("");
            param7.setVpcId("");
            String ipAddr = inst.getPrivateIpAddress();
            param7.setPrivateIpAddress((null != ipAddr ? ipAddr : ""));
            param7.setIpAddress(inst.getIpAddress());

            StateReasonType param13 = new StateReasonType();
            param13.setCode("");
            param13.setMessage("");
            param7.setStateReason(param13);
            param7.setArchitecture("");
            param7.setRootDeviceType("");
            param7.setRootDeviceName("");

            param7.setInstanceLifecycle("");
            param7.setSpotInstanceRequestId("");
            param7.setVirtualizationType("");
            param7.setClientToken("");

            ResourceTagSetType param18 = new ResourceTagSetType();
            ResourceTagSetItemType param19 = new ResourceTagSetItemType();
            param19.setKey("");
            param19.setValue("");
            param18.addItem(param19);
            param7.setTagSet(param18);

            GroupSetType param14 = new GroupSetType();
            GroupItemType param15 = new GroupItemType();
            param15.setGroupId("");
            param15.setGroupName("");
            param14.addItem(param15);
            param7.setGroupSet(param14);

            String hypervisor = inst.getHypervisor();
            param7.setHypervisor((null != hypervisor ? hypervisor : ""));
            param6.addItem(param7);
        }
        param1.setInstancesSet(param6);
        param1.setRequesterId("");

        param1.setRequestId(UUID.randomUUID().toString());
        response.setRunInstancesResponse(param1);
        return response;
    }

    public static DescribeAvailabilityZonesResponse toDescribeAvailabilityZonesResponse(EC2DescribeAvailabilityZonesResponse engineResponse) {
        DescribeAvailabilityZonesResponse response = new DescribeAvailabilityZonesResponse();
        DescribeAvailabilityZonesResponseType param1 = new DescribeAvailabilityZonesResponseType();
        AvailabilityZoneSetType param2 = new AvailabilityZoneSetType();

        EC2AvailabilityZone[] zones = engineResponse.getAvailabilityZoneSet();
        for (EC2AvailabilityZone zone : zones) {
            AvailabilityZoneItemType param3 = new AvailabilityZoneItemType();
            param3.setZoneName(zone.getName());
            param3.setZoneState("available");
            param3.setRegionName("");

            AvailabilityZoneMessageSetType param4 = new AvailabilityZoneMessageSetType();
            AvailabilityZoneMessageType param5 = new AvailabilityZoneMessageType();
            param5.setMessage(zone.getMessage());
            param4.addItem(param5);
            param3.setMessageSet(param4);
            param2.addItem(param3);
        }

        param1.setRequestId(UUID.randomUUID().toString());
        param1.setAvailabilityZoneInfo(param2);
        response.setDescribeAvailabilityZonesResponse(param1);
        return response;
    }

    public static AttachVolumeResponse toAttachVolumeResponse(EC2Volume engineResponse) {
        AttachVolumeResponse response = new AttachVolumeResponse();
        AttachVolumeResponseType param1 = new AttachVolumeResponseType();

        Calendar cal = Calendar.getInstance();

        // -> if the instanceId was not given in the request then we have no way to get it
        param1.setVolumeId(engineResponse.getId().toString());
        param1.setInstanceId(engineResponse.getInstanceId().toString());
        param1.setDevice(engineResponse.getDevice());
        param1.setStatus(engineResponse.getAttachmentState());
        param1.setAttachTime(cal);

        param1.setRequestId(UUID.randomUUID().toString());
        response.setAttachVolumeResponse(param1);
        return response;
    }

    public static DetachVolumeResponse toDetachVolumeResponse(EC2Volume engineResponse) {
        DetachVolumeResponse response = new DetachVolumeResponse();
        DetachVolumeResponseType param1 = new DetachVolumeResponseType();
        Calendar cal = Calendar.getInstance();
        cal.set(1970, 1, 1);   // return one value, Unix Epoch, what else can we return?

        param1.setVolumeId(engineResponse.getId().toString());
        param1.setInstanceId((null == engineResponse.getInstanceId() ? "" : engineResponse.getInstanceId().toString()));
        param1.setDevice((null == engineResponse.getDevice() ? "" : engineResponse.getDevice()));
        param1.setStatus(engineResponse.getAttachmentState());
        param1.setAttachTime(cal);

        param1.setRequestId(UUID.randomUUID().toString());
        response.setDetachVolumeResponse(param1);
        return response;
    }

    public static CreateVolumeResponse toCreateVolumeResponse(EC2Volume engineResponse) {
        CreateVolumeResponse response = new CreateVolumeResponse();
        CreateVolumeResponseType param1 = new CreateVolumeResponseType();

        param1.setVolumeId(engineResponse.getId().toString());
        Long volSize = new Long(engineResponse.getSize());
        param1.setSize(volSize.toString());
        if (engineResponse.getSnapshotId() != null)
            param1.setSnapshotId(engineResponse.getSnapshotId());
        else
            param1.setSnapshotId("");
        param1.setAvailabilityZone(engineResponse.getZoneName());
        if (null != engineResponse.getState())
            param1.setStatus(engineResponse.getState());
        else
            param1.setStatus("");  // ToDo - throw an Soap Fault

        // -> CloudStack seems to have issues with timestamp formats so just in case
        Calendar cal = EC2RestAuth.parseDateString(engineResponse.getCreated());
        if (null == cal) {
            cal = Calendar.getInstance();
//             cal.set( 1970, 1, 1 );
        }
        param1.setCreateTime(cal);

        param1.setVolumeType("standard");
        param1.setRequestId(UUID.randomUUID().toString());
        response.setCreateVolumeResponse(param1);
        return response;
    }

    public static DeleteVolumeResponse toDeleteVolumeResponse(EC2Volume engineResponse) {
        DeleteVolumeResponse response = new DeleteVolumeResponse();
        DeleteVolumeResponseType param1 = new DeleteVolumeResponseType();

        if (null != engineResponse.getState())
            param1.set_return(true);
        else
            param1.set_return(false);  // ToDo - supposed to return an error

        param1.setRequestId(UUID.randomUUID().toString());
        response.setDeleteVolumeResponse(param1);
        return response;
    }

    public static DescribeSnapshotsResponse toDescribeSnapshotsResponse(EC2DescribeSnapshotsResponse engineResponse) {
        DescribeSnapshotsResponse response = new DescribeSnapshotsResponse();
        DescribeSnapshotsResponseType param1 = new DescribeSnapshotsResponseType();
        DescribeSnapshotsSetResponseType param2 = new DescribeSnapshotsSetResponseType();

        EC2Snapshot[] snaps = engineResponse.getSnapshotSet();
        for (EC2Snapshot snap : snaps) {
            DescribeSnapshotsSetItemResponseType param3 = new DescribeSnapshotsSetItemResponseType();
            param3.setSnapshotId(snap.getId());
            param3.setVolumeId(snap.getVolumeId());

            // our semantics are different than those ec2 uses
            if (snap.getState().equalsIgnoreCase("backedup")) {
                param3.setStatus("completed");
                param3.setProgress("100%");
            } else if (snap.getState().equalsIgnoreCase("creating")) {
                param3.setStatus("pending");
                param3.setProgress("33%");
            } else if (snap.getState().equalsIgnoreCase("backingup")) {
                param3.setStatus("pending");
                param3.setProgress("66%");
            } else {
                // if we see anything besides: backedup/creating/backingup, we assume error
                param3.setStatus("error");
                param3.setProgress("0%");
            }
//             param3.setStatus( snap.getState());

            String ownerId = snap.getDomainId() + ":" + snap.getAccountName();

            // -> CloudStack seems to have issues with timestamp formats so just in case
            Calendar cal = snap.getCreated();
            if (null == cal) {
                cal = Calendar.getInstance();
                cal.set(1970, 1, 1);
            }
            param3.setStartTime(cal);

            param3.setOwnerId(ownerId);
            if (snap.getVolumeSize() == null)
                param3.setVolumeSize("0");
            else
                param3.setVolumeSize(snap.getVolumeSize().toString());
            param3.setDescription(snap.getName());
            param3.setOwnerAlias(snap.getAccountName());

            EC2TagKeyValue[] tags = snap.getResourceTags();
            param3.setTagSet(setResourceTags(tags));
            param2.addItem(param3);
        }

        param1.setSnapshotSet(param2);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDescribeSnapshotsResponse(param1);
        return response;
    }

    public static DeleteSnapshotResponse toDeleteSnapshotResponse(boolean engineResponse) {
        DeleteSnapshotResponse response = new DeleteSnapshotResponse();
        DeleteSnapshotResponseType param1 = new DeleteSnapshotResponseType();

        param1.set_return(engineResponse);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDeleteSnapshotResponse(param1);
        return response;
    }

    public static CreateSnapshotResponse toCreateSnapshotResponse(EC2Snapshot engineResponse, EC2Engine engine) {
        CreateSnapshotResponse response = new CreateSnapshotResponse();
        CreateSnapshotResponseType param1 = new CreateSnapshotResponseType();

        String accountName = engineResponse.getAccountName();
        String domainId = engineResponse.getDomainId().toString();
        String ownerId = domainId + ":" + accountName;

        param1.setSnapshotId(engineResponse.getId().toString());
        param1.setVolumeId(engineResponse.getVolumeId().toString());
        param1.setStatus("completed");

        // -> CloudStack seems to have issues with timestamp formats so just in case
        Calendar cal = engineResponse.getCreated();
        if (null == cal) {
            cal = Calendar.getInstance();
            cal.set(1970, 1, 1);
        }
        param1.setStartTime(cal);

        param1.setProgress("100");
        param1.setOwnerId(ownerId);
        Long volSize = new Long(engineResponse.getVolumeSize());
        param1.setVolumeSize(volSize.toString());
        param1.setDescription(engineResponse.getName());
        param1.setRequestId(UUID.randomUUID().toString());
        response.setCreateSnapshotResponse(param1);
        return response;
    }

    public static DescribeSecurityGroupsResponse toDescribeSecurityGroupsResponse(EC2DescribeSecurityGroupsResponse engineResponse) {
        DescribeSecurityGroupsResponse response = new DescribeSecurityGroupsResponse();
        DescribeSecurityGroupsResponseType param1 = new DescribeSecurityGroupsResponseType();
        SecurityGroupSetType param2 = new SecurityGroupSetType();

        EC2SecurityGroup[] groups = engineResponse.getGroupSet();
        for (EC2SecurityGroup group : groups) {
            SecurityGroupItemType param3 = new SecurityGroupItemType();
            String accountName = group.getAccountName();
            String domainId = group.getDomainId();
            String ownerId = domainId + ":" + accountName;

            param3.setOwnerId(ownerId);
            param3.setGroupName(group.getName());
            String desc = group.getDescription();
            param3.setGroupDescription((null != desc ? desc : ""));
            param3.setGroupId(group.getId());
            param3.setVpcId("");

            IpPermissionSetType param4 = new IpPermissionSetType();
            EC2IpPermission[] perms = group.getIpPermissionSet();
            for (EC2IpPermission perm : perms) {
                // TODO: Fix kludges like this...
                if (perm == null)
                    continue;
                IpPermissionType param5 = new IpPermissionType();
                param5.setIpProtocol(perm.getProtocol());
                if (perm.getProtocol().equalsIgnoreCase("icmp")) {
                    param5.setFromPort(Integer.parseInt(perm.getIcmpType()));
                    param5.setToPort(Integer.parseInt(perm.getIcmpCode()));
                } else {
                    param5.setFromPort(perm.getFromPort());
                    param5.setToPort(perm.getToPort());
                }

                // -> user groups
                EC2SecurityGroup[] userSet = perm.getUserSet();
                if (null == userSet || 0 == userSet.length) {
                    UserIdGroupPairSetType param8 = new UserIdGroupPairSetType();
                    param5.setGroups(param8);
                } else {
                    for (EC2SecurityGroup secGroup : userSet) {
                        UserIdGroupPairSetType param8 = new UserIdGroupPairSetType();
                        UserIdGroupPairType param9 = new UserIdGroupPairType();
                        param9.setUserId(secGroup.getAccount());
                        param9.setGroupName(secGroup.getName());
                        param8.addItem(param9);
                        param5.setGroups(param8);
                    }
                }

                // -> or CIDR list
                String[] rangeSet = perm.getIpRangeSet();
                if (null == rangeSet || 0 == rangeSet.length) {
                    IpRangeSetType param6 = new IpRangeSetType();
                    param5.setIpRanges(param6);
                } else {
                    for (String range : rangeSet) {
                        // TODO: This needs further attention...
                        IpRangeSetType param6 = new IpRangeSetType();
                        if (range != null) {
                            IpRangeItemType param7 = new IpRangeItemType();
                            param7.setCidrIp(range);
                            param6.addItem(param7);
                        }
                        param5.setIpRanges(param6);
                    }
                }
                param4.addItem(param5);
            }
            param3.setIpPermissions(param4);
            EC2TagKeyValue[] tags = group.getResourceTags();
            param3.setTagSet(setResourceTags(tags));
            param2.addItem(param3);
        }
        param1.setSecurityGroupInfo(param2);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDescribeSecurityGroupsResponse(param1);
        return response;
    }

    public static CreateSecurityGroupResponse toCreateSecurityGroupResponse(EC2SecurityGroup sg) {
        CreateSecurityGroupResponse response = new CreateSecurityGroupResponse();
        CreateSecurityGroupResponseType param1 = new CreateSecurityGroupResponseType();

        param1.setGroupId(sg.getId());
        if (sg.getId() != null)
            param1.set_return(true);
        else
            param1.set_return(false);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setCreateSecurityGroupResponse(param1);
        return response;
    }

    public static DeleteSecurityGroupResponse toDeleteSecurityGroupResponse(boolean success) {
        DeleteSecurityGroupResponse response = new DeleteSecurityGroupResponse();
        DeleteSecurityGroupResponseType param1 = new DeleteSecurityGroupResponseType();

        param1.set_return(success);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDeleteSecurityGroupResponse(param1);
        return response;
    }

    public static AuthorizeSecurityGroupIngressResponse toAuthorizeSecurityGroupIngressResponse(boolean success) {
        AuthorizeSecurityGroupIngressResponse response = new AuthorizeSecurityGroupIngressResponse();
        AuthorizeSecurityGroupIngressResponseType param1 = new AuthorizeSecurityGroupIngressResponseType();

        param1.set_return(success);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setAuthorizeSecurityGroupIngressResponse(param1);
        return response;
    }

    public static RevokeSecurityGroupIngressResponse toRevokeSecurityGroupIngressResponse(boolean success) {
        RevokeSecurityGroupIngressResponse response = new RevokeSecurityGroupIngressResponse();
        RevokeSecurityGroupIngressResponseType param1 = new RevokeSecurityGroupIngressResponseType();

        param1.set_return(success);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setRevokeSecurityGroupIngressResponse(param1);
        return response;
    }

    public static CreateTagsResponse toCreateTagsResponse(boolean success) {
        CreateTagsResponse response = new CreateTagsResponse();
        CreateTagsResponseType param1 = new CreateTagsResponseType();

        param1.set_return(success);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setCreateTagsResponse(param1);
        return response;
    }

    public static DeleteTagsResponse toDeleteTagsResponse(boolean success) {
        DeleteTagsResponse response = new DeleteTagsResponse();
        DeleteTagsResponseType param1 = new DeleteTagsResponseType();

        param1.set_return(success);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDeleteTagsResponse(param1);
        return response;
    }

    public static DescribeTagsResponse toDescribeTagsResponse(EC2DescribeTagsResponse engineResponse) {
        DescribeTagsResponse response = new DescribeTagsResponse();
        DescribeTagsResponseType param1 = new DescribeTagsResponseType();

        EC2ResourceTag[] tags = engineResponse.getTagsSet();
        TagSetType param2 = new TagSetType();
        for (EC2ResourceTag tag : tags) {
            TagSetItemType param3 = new TagSetItemType();
            param3.setResourceId(tag.getResourceId());
            param3.setResourceType(tag.getResourceType());
            param3.setKey(tag.getKey());
            if (tag.getValue() != null)
                param3.setValue(tag.getValue());
            param2.addItem(param3);
        }
        param1.setTagSet(param2);
        param1.setRequestId(UUID.randomUUID().toString());
        response.setDescribeTagsResponse(param1);
        return response;
    }

    public DescribeKeyPairsResponse describeKeyPairs(DescribeKeyPairs describeKeyPairs) {

        EC2DescribeKeyPairs ec2Request = new EC2DescribeKeyPairs();

        // multiple keynames may be provided
        DescribeKeyPairsInfoType kset = describeKeyPairs.getDescribeKeyPairs().getKeySet();
        if (kset != null) {
            DescribeKeyPairsItemType[] keyPairKeys = kset.getItem();
            if (keyPairKeys != null) {
                for (DescribeKeyPairsItemType key : keyPairKeys) {
                    ec2Request.addKeyName(key.getKeyName());
                }
            }
        }

        // multiple filters may be provided
        FilterSetType fset = describeKeyPairs.getDescribeKeyPairs().getFilterSet();
        if (fset != null) {
            ec2Request.setKeyFilterSet(toKeyPairFilterSet(fset));
        }

        return toDescribeKeyPairs(engine.describeKeyPairs(ec2Request));
    }

    public static DescribeKeyPairsResponse toDescribeKeyPairs(final EC2DescribeKeyPairsResponse response) {
        EC2SSHKeyPair[] keyPairs = response.getKeyPairSet();

        DescribeKeyPairsResponseInfoType respInfoType = new DescribeKeyPairsResponseInfoType();
        if (keyPairs != null && keyPairs.length > 0) {
            for (final EC2SSHKeyPair key : keyPairs) {
                DescribeKeyPairsResponseItemType respItemType = new DescribeKeyPairsResponseItemType();
                respItemType.setKeyFingerprint(key.getFingerprint());
                respItemType.setKeyName(key.getKeyName());
                respInfoType.addItem(respItemType);
            }
        }

        DescribeKeyPairsResponseType respType = new DescribeKeyPairsResponseType();
        respType.setRequestId(UUID.randomUUID().toString());
        respType.setKeySet(respInfoType);

        DescribeKeyPairsResponse resp = new DescribeKeyPairsResponse();
        resp.setDescribeKeyPairsResponse(respType);
        return resp;
    }

    public ImportKeyPairResponse importKeyPair(ImportKeyPair importKeyPair) {
        String publicKey = importKeyPair.getImportKeyPair().getPublicKeyMaterial();
        if (!publicKey.contains(" "))
            publicKey = new String(Base64.decodeBase64(publicKey.getBytes()));

        EC2ImportKeyPair ec2Request = new EC2ImportKeyPair();
        if (ec2Request != null) {
            ec2Request.setKeyName(importKeyPair.getImportKeyPair().getKeyName());
            ec2Request.setPublicKeyMaterial(publicKey);
        }

        return toImportKeyPair(engine.importKeyPair(ec2Request));
    }

    public static ImportKeyPairResponse toImportKeyPair(final EC2SSHKeyPair key) {
        ImportKeyPairResponseType respType = new ImportKeyPairResponseType();
        respType.setRequestId(UUID.randomUUID().toString());
        respType.setKeyName(key.getKeyName());
        respType.setKeyFingerprint(key.getFingerprint());

        ImportKeyPairResponse response = new ImportKeyPairResponse();
        response.setImportKeyPairResponse(respType);

        return response;
    }

    public CreateKeyPairResponse createKeyPair(CreateKeyPair createKeyPair) {
        EC2CreateKeyPair ec2Request = new EC2CreateKeyPair();
        if (ec2Request != null) {
            ec2Request.setKeyName(createKeyPair.getCreateKeyPair().getKeyName());
        }

        return toCreateKeyPair(engine.createKeyPair(ec2Request));
    }

    public static CreateKeyPairResponse toCreateKeyPair(final EC2SSHKeyPair key) {
        CreateKeyPairResponseType respType = new CreateKeyPairResponseType();
        respType.setRequestId(UUID.randomUUID().toString());
        respType.setKeyName(key.getKeyName());
        respType.setKeyFingerprint(key.getFingerprint());
        respType.setKeyMaterial(key.getPrivateKey());

        CreateKeyPairResponse response = new CreateKeyPairResponse();
        response.setCreateKeyPairResponse(respType);

        return response;
    }

    public DeleteKeyPairResponse deleteKeyPair(DeleteKeyPair deleteKeyPair) {
        EC2DeleteKeyPair ec2Request = new EC2DeleteKeyPair();
        ec2Request.setKeyName(deleteKeyPair.getDeleteKeyPair().getKeyName());

        return toDeleteKeyPair(engine.deleteKeyPair(ec2Request));
    }

    public static DeleteKeyPairResponse toDeleteKeyPair(final boolean success) {
        DeleteKeyPairResponseType respType = new DeleteKeyPairResponseType();
        respType.setRequestId(UUID.randomUUID().toString());
        respType.set_return(success);

        DeleteKeyPairResponse response = new DeleteKeyPairResponse();
        response.setDeleteKeyPairResponse(respType);

        return response;
    }

    public GetPasswordDataResponse getPasswordData(GetPasswordData getPasswordData) {
        return toGetPasswordData(engine.getPasswordData(getPasswordData.getGetPasswordData().getInstanceId()));
    }

    public static ResourceTagSetType setResourceTags(EC2TagKeyValue[] tags) {
        ResourceTagSetType param1 = new ResourceTagSetType();
        if (null == tags || 0 == tags.length) {
            ResourceTagSetItemType param2 = new ResourceTagSetItemType();
            param2.setKey("");
            param2.setValue("");
            param1.addItem(param2);
        } else {
            for (EC2TagKeyValue tag : tags) {
                ResourceTagSetItemType param2 = new ResourceTagSetItemType();
                param2.setKey(tag.getKey());
                if (tag.getValue() != null)
                    param2.setValue(tag.getValue());
                else
                    param2.setValue("");
                param1.addItem(param2);
            }
        }
        return param1;
    }

    @SuppressWarnings("serial")
    public static GetPasswordDataResponse toGetPasswordData(final EC2PasswordData passwdData) {
        return new GetPasswordDataResponse() {
            {
                setGetPasswordDataResponse(new GetPasswordDataResponseType() {
                    {
                        setRequestId(UUID.randomUUID().toString());
                        setTimestamp(Calendar.getInstance());
                        setPasswordData(passwdData.getEncryptedPassword());
                        setInstanceId(passwdData.getInstanceId());
                    }
                });
            }
        };
    }

    // Actions not yet implemented:

    public ActivateLicenseResponse activateLicense(ActivateLicense activateLicense) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public AssociateDhcpOptionsResponse associateDhcpOptions(AssociateDhcpOptions associateDhcpOptions) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    };

    public AttachVpnGatewayResponse attachVpnGateway(AttachVpnGateway attachVpnGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public BundleInstanceResponse bundleInstance(BundleInstance bundleInstance) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CancelBundleTaskResponse cancelBundleTask(CancelBundleTask cancelBundleTask) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CancelConversionTaskResponse cancelConversionTask(CancelConversionTask cancelConversionTask) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CancelSpotInstanceRequestsResponse cancelSpotInstanceRequests(CancelSpotInstanceRequests cancelSpotInstanceRequests) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ConfirmProductInstanceResponse confirmProductInstance(ConfirmProductInstance confirmProductInstance) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateCustomerGatewayResponse createCustomerGateway(CreateCustomerGateway createCustomerGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateDhcpOptionsResponse createDhcpOptions(CreateDhcpOptions createDhcpOptions) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreatePlacementGroupResponse createPlacementGroup(CreatePlacementGroup createPlacementGroup) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateSpotDatafeedSubscriptionResponse createSpotDatafeedSubscription(CreateSpotDatafeedSubscription createSpotDatafeedSubscription) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateSubnetResponse createSubnet(CreateSubnet createSubnet) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateVpcResponse createVpc(CreateVpc createVpc) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateVpnConnectionResponse createVpnConnection(CreateVpnConnection createVpnConnection) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateVpnGatewayResponse createVpnGateway(CreateVpnGateway createVpnGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeactivateLicenseResponse deactivateLicense(DeactivateLicense deactivateLicense) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteCustomerGatewayResponse deleteCustomerGateway(DeleteCustomerGateway deleteCustomerGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteDhcpOptionsResponse deleteDhcpOptions(DeleteDhcpOptions deleteDhcpOptions) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeletePlacementGroupResponse deletePlacementGroup(DeletePlacementGroup deletePlacementGroup) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteSpotDatafeedSubscriptionResponse deleteSpotDatafeedSubscription(DeleteSpotDatafeedSubscription deleteSpotDatafeedSubscription) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteSubnetResponse deleteSubnet(DeleteSubnet deleteSubnet) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteVpcResponse deleteVpc(DeleteVpc deleteVpc) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteVpnConnectionResponse deleteVpnConnection(DeleteVpnConnection deleteVpnConnection) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteVpnGatewayResponse deleteVpnGateway(DeleteVpnGateway deleteVpnGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeBundleTasksResponse describeBundleTasks(DescribeBundleTasks describeBundleTasks) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeConversionTasksResponse describeConversionTasks(DescribeConversionTasks describeConversionTasks) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeCustomerGatewaysResponse describeCustomerGateways(DescribeCustomerGateways describeCustomerGateways) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeDhcpOptionsResponse describeDhcpOptions(DescribeDhcpOptions describeDhcpOptions) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeLicensesResponse describeLicenses(DescribeLicenses describeLicenses) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribePlacementGroupsResponse describePlacementGroups(DescribePlacementGroups describePlacementGroups) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeRegionsResponse describeRegions(DescribeRegions describeRegions) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeReservedInstancesResponse describeReservedInstances(DescribeReservedInstances describeReservedInstances) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeReservedInstancesOfferingsResponse describeReservedInstancesOfferings(DescribeReservedInstancesOfferings describeReservedInstancesOfferings) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeSnapshotAttributeResponse describeSnapshotAttribute(DescribeSnapshotAttribute describeSnapshotAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeSpotDatafeedSubscriptionResponse describeSpotDatafeedSubscription(DescribeSpotDatafeedSubscription describeSpotDatafeedSubscription) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeSpotInstanceRequestsResponse describeSpotInstanceRequests(DescribeSpotInstanceRequests describeSpotInstanceRequests) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeSpotPriceHistoryResponse describeSpotPriceHistory(DescribeSpotPriceHistory describeSpotPriceHistory) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeSubnetsResponse describeSubnets(DescribeSubnets describeSubnets) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeVpcsResponse describeVpcs(DescribeVpcs describeVpcs) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeVpnConnectionsResponse describeVpnConnections(DescribeVpnConnections describeVpnConnections) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeVpnGatewaysResponse describeVpnGateways(DescribeVpnGateways describeVpnGateways) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DetachVpnGatewayResponse detachVpnGateway(DetachVpnGateway detachVpnGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public GetConsoleOutputResponse getConsoleOutput(GetConsoleOutput getConsoleOutput) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ImportInstanceResponse importInstance(ImportInstance importInstance) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ImportVolumeResponse importVolume(ImportVolume importVolume) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ModifySnapshotAttributeResponse modifySnapshotAttribute(ModifySnapshotAttribute modifySnapshotAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public PurchaseReservedInstancesOfferingResponse purchaseReservedInstancesOffering(PurchaseReservedInstancesOffering purchaseReservedInstancesOffering) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public RequestSpotInstancesResponse requestSpotInstances(RequestSpotInstances requestSpotInstances) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ResetInstanceAttributeResponse resetInstanceAttribute(ResetInstanceAttribute resetInstanceAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ResetSnapshotAttributeResponse resetSnapshotAttribute(ResetSnapshotAttribute resetSnapshotAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ResetNetworkInterfaceAttributeResponse resetNetworkInterfaceAttribute(ResetNetworkInterfaceAttribute resetNetworkInterfaceAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateRouteTableResponse createRouteTable(CreateRouteTable createRouteTable) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateNetworkAclEntryResponse createNetworkAclEntry(CreateNetworkAclEntry createNetworkAclEntry) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeVolumeAttributeResponse describeVolumeAttribute(DescribeVolumeAttribute describeVolumeAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteNetworkInterfaceResponse deleteNetworkInterface(DeleteNetworkInterface deleteNetworkInterface) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateInternetGatewayResponse createInternetGateway(CreateInternetGateway createInternetGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DisassociateRouteTableResponse disassociateRouteTable(DisassociateRouteTable disassociateRouteTable) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ReplaceNetworkAclEntryResponse replaceNetworkAclEntry(ReplaceNetworkAclEntry replaceNetworkAclEntry) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public AuthorizeSecurityGroupEgressResponse authorizeSecurityGroupEgress(AuthorizeSecurityGroupEgress authorizeSecurityGroupEgress) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteNetworkAclEntryResponse deleteNetworkAclEntry(DeleteNetworkAclEntry deleteNetworkAclEntry) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteRouteTableResponse deleteRouteTable(DeleteRouteTable deleteRouteTable) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeNetworkInterfaceAttributeResponse describeNetworkInterfaceAttribute(DescribeNetworkInterfaceAttribute describeNetworkInterfaceAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateReservedInstancesListingResponse createReservedInstancesListing(CreateReservedInstancesListing createReservedInstancesListing) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateNetworkAclResponse createNetworkAcl(CreateNetworkAcl createNetworkAcl) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ModifyVolumeAttributeResponse modifyVolumeAttribute(ModifyVolumeAttribute modifyVolumeAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ReplaceNetworkAclAssociationResponse replaceNetworkAclAssociation(ReplaceNetworkAclAssociation replaceNetworkAclAssociation) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public EnableVgwRoutePropagationResponse enableVgwRoutePropagation(EnableVgwRoutePropagation enableVgwRoutePropagation) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public UnassignPrivateIpAddressesResponse unassignPrivateIpAddresses(UnassignPrivateIpAddresses unassignPrivateIpAddresses) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteVpnConnectionRouteResponse deleteVpnConnectionRoute(DeleteVpnConnectionRoute deleteVpnConnectionRoute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CancelReservedInstancesListingResponse cancelReservedInstancesListing(CancelReservedInstancesListing cancelReservedInstancesListing) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeNetworkAclsResponse describeNetworkAcls(DescribeNetworkAcls describeNetworkAcls) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public EnableVolumeIOResponse enableVolumeIO(EnableVolumeIO enableVolumeIO) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeInternetGatewaysResponse describeInternetGateways(DescribeInternetGateways describeInternetGateways) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeReservedInstancesListingsResponse describeReservedInstancesListings(DescribeReservedInstancesListings describeReservedInstancesListings) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeInstanceStatusResponse describeInstanceStatus(DescribeInstanceStatus describeInstanceStatus) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ModifyNetworkInterfaceAttributeResponse modifyNetworkInterfaceAttribute(ModifyNetworkInterfaceAttribute modifyNetworkInterfaceAttribute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DisableVgwRoutePropagationResponse disableVgwRoutePropagation(DisableVgwRoutePropagation disableVgwRoutePropagation) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeVolumeStatusResponse describeVolumeStatus(DescribeVolumeStatus describeVolumeStatus) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DetachNetworkInterfaceResponse detachNetworkInterface(DetachNetworkInterface detachNetworkInterface) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeNetworkInterfacesResponse describeNetworkInterfaces(DescribeNetworkInterfaces describeNetworkInterfaces) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CancelExportTaskResponse cancelExportTask(CancelExportTask cancelExportTask) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateRouteResponse createRoute(CreateRoute createRoute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeRouteTablesResponse describeRouteTables(DescribeRouteTables describeRouteTables) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteNetworkAclResponse deleteNetworkAcl(DeleteNetworkAcl deleteNetworkAcl) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteRouteResponse deleteRoute(DeleteRoute deleteRoute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateVpnConnectionRouteResponse createVpnConnectionRoute(CreateVpnConnectionRoute createVpnConnectionRoute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public AttachInternetGatewayResponse attachInternetGateway(AttachInternetGateway attachInternetGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ReplaceRouteTableAssociationResponse replaceRouteTableAssociation(ReplaceRouteTableAssociation replaceRouteTableAssociation) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public AssociateRouteTableResponse associateRouteTable(AssociateRouteTable associateRouteTable) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DetachInternetGatewayResponse detachInternetGateway(DetachInternetGateway detachInternetGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DescribeExportTasksResponse describeExportTasks(DescribeExportTasks describeExportTasks) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateInstanceExportTaskResponse createInstanceExportTask(CreateInstanceExportTask createInstanceExportTask) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public AssignPrivateIpAddressesResponse assignPrivateIpAddresses(AssignPrivateIpAddresses assignPrivateIpAddresses) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ReportInstanceStatusResponse reportInstanceStatus(ReportInstanceStatus reportInstanceStatus) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public DeleteInternetGatewayResponse deleteInternetGateway(DeleteInternetGateway deleteInternetGateway) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public AttachNetworkInterfaceResponse attachNetworkInterface(AttachNetworkInterface attachNetworkInterface) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public CreateNetworkInterfaceResponse createNetworkInterface(CreateNetworkInterface createNetworkInterface) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public RevokeSecurityGroupEgressResponse revokeSecurityGroupEgress(RevokeSecurityGroupEgress revokeSecurityGroupEgress) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }

    public ReplaceRouteResponse replaceRoute(ReplaceRoute replaceRoute) {
        throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
    }
}
