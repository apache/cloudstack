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

/**
 * ExtensionMapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:01:01 CEST)
 */

package com.amazon.ec2;

/**
*  ExtensionMapper class
*/

public class ExtensionMapper {

    public static java.lang.Object getTypeObject(java.lang.String namespaceURI, java.lang.String typeName, javax.xml.stream.XMLStreamReader reader)
        throws java.lang.Exception {

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateInternetGatewayType".equals(typeName)) {

            return com.amazon.ec2.CreateInternetGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RouteTableIdSetType".equals(typeName)) {

            return com.amazon.ec2.RouteTableIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateRouteTableType".equals(typeName)) {

            return com.amazon.ec2.CreateRouteTableType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachInternetGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.AttachInternetGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateSnapshotType".equals(typeName)) {

            return com.amazon.ec2.CreateSnapshotType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReportInstanceStatusResponseType".equals(typeName)) {

            return com.amazon.ec2.ReportInstanceStatusResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "TerminateInstancesType".equals(typeName)) {

            return com.amazon.ec2.TerminateInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnGatewayType".equals(typeName)) {

            return com.amazon.ec2.VpnGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesExecutableBySetType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesExecutableBySetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkAclIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.NetworkAclIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeRegionsSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeRegionsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVpnGatewayType".equals(typeName)) {

            return com.amazon.ec2.DeleteVpnGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResourceIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.ResourceIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumesSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumesSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnTunnelTelemetryType".equals(typeName)) {

            return com.amazon.ec2.VpnTunnelTelemetryType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeRegionsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeRegionsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LicenseIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.LicenseIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ExportTaskResponseType".equals(typeName)) {

            return com.amazon.ec2.ExportTaskResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateSubnetResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateSubnetResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteNetworkAclResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteNetworkAclResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DhcpValueType".equals(typeName)) {

            return com.amazon.ec2.DhcpValueType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceIdType".equals(typeName)) {

            return com.amazon.ec2.InstanceIdType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SecurityGroupIdSetType".equals(typeName)) {

            return com.amazon.ec2.SecurityGroupIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DetachInternetGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.DetachInternetGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateInstanceExportTaskResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateInstanceExportTaskResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachmentSetType".equals(typeName)) {

            return com.amazon.ec2.AttachmentSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "GetPasswordDataResponseType".equals(typeName)) {

            return com.amazon.ec2.GetPasswordDataResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusSetType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVolumePermissionOperationType".equals(typeName)) {

            return com.amazon.ec2.CreateVolumePermissionOperationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "FilterSetType".equals(typeName)) {

            return com.amazon.ec2.FilterSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ProductDescriptionSetType".equals(typeName)) {

            return com.amazon.ec2.ProductDescriptionSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResetNetworkInterfaceAttributeType".equals(typeName)) {

            return com.amazon.ec2.ResetNetworkInterfaceAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportInstanceLaunchSpecificationType".equals(typeName)) {

            return com.amazon.ec2.ImportInstanceLaunchSpecificationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesResponseSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesResponseSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportInstanceVolumeDetailSetType".equals(typeName)) {

            return com.amazon.ec2.ImportInstanceVolumeDetailSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyInstanceAttributeType".equals(typeName)) {

            return com.amazon.ec2.ModifyInstanceAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RunningInstancesItemType".equals(typeName)) {

            return com.amazon.ec2.RunningInstancesItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReplaceNetworkAclEntryResponseType".equals(typeName)) {

            return com.amazon.ec2.ReplaceNetworkAclEntryResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportInstanceGroupSetType".equals(typeName)) {

            return com.amazon.ec2.ImportInstanceGroupSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeNetworkInterfaceAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeNetworkInterfaceAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesOwnersType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesOwnersType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreatePlacementGroupType".equals(typeName)) {

            return com.amazon.ec2.CreatePlacementGroupType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeletePlacementGroupType".equals(typeName)) {

            return com.amazon.ec2.DeletePlacementGroupType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateSpotDatafeedSubscriptionResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateSpotDatafeedSubscriptionResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateSubnetType".equals(typeName)) {

            return com.amazon.ec2.CreateSubnetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResourceIdSetType".equals(typeName)) {

            return com.amazon.ec2.ResourceIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssociateRouteTableType".equals(typeName)) {

            return com.amazon.ec2.AssociateRouteTableType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelSpotInstanceRequestsResponseSetItemType".equals(typeName)) {

            return com.amazon.ec2.CancelSpotInstanceRequestsResponseSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResetSnapshotAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ResetSnapshotAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PrivateIpAddressesSetItemRequestType".equals(typeName)) {

            return com.amazon.ec2.PrivateIpAddressesSetItemRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotAttributeType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "MonitorInstancesSetItemType".equals(typeName)) {

            return com.amazon.ec2.MonitorInstancesSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "StopInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.StopInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkAclEntryType".equals(typeName)) {

            return com.amazon.ec2.NetworkAclEntryType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RouteSetType".equals(typeName)) {

            return com.amazon.ec2.RouteSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteSnapshotResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteSnapshotResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateNetworkAclEntryType".equals(typeName)) {

            return com.amazon.ec2.CreateNetworkAclEntryType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsSetItemResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsSetItemResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceExportTaskResponseType".equals(typeName)) {

            return com.amazon.ec2.InstanceExportTaskResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ExportTaskIdSetType".equals(typeName)) {

            return com.amazon.ec2.ExportTaskIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribePlacementGroupsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribePlacementGroupsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ProductCodeItemType".equals(typeName)) {

            return com.amazon.ec2.ProductCodeItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateSnapshotResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateSnapshotResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssignPrivateIpAddressesSetRequestType".equals(typeName)) {

            return com.amazon.ec2.AssignPrivateIpAddressesSetRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateReservedInstancesListingResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateReservedInstancesListingResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BlockDeviceMappingType".equals(typeName)) {

            return com.amazon.ec2.BlockDeviceMappingType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusActionItemType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusActionItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LicenseIdSetType".equals(typeName)) {

            return com.amazon.ec2.LicenseIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkInterfaceAssociationType".equals(typeName)) {

            return com.amazon.ec2.NetworkInterfaceAssociationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AllocationIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.AllocationIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkInterfaceSetType".equals(typeName)) {

            return com.amazon.ec2.NetworkInterfaceSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceBlockDeviceMappingResponseItemType".equals(typeName)) {

            return com.amazon.ec2.InstanceBlockDeviceMappingResponseItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVpnConnectionRouteType".equals(typeName)) {

            return com.amazon.ec2.DeleteVpnConnectionRouteType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyNetworkInterfaceAttachmentType".equals(typeName)) {

            return com.amazon.ec2.ModifyNetworkInterfaceAttachmentType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstancePlacementType".equals(typeName)) {

            return com.amazon.ec2.InstancePlacementType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotPlacementRequestType".equals(typeName)) {

            return com.amazon.ec2.SpotPlacementRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsOwnerType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsOwnerType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotPriceHistorySetItemType".equals(typeName)) {

            return com.amazon.ec2.SpotPriceHistorySetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnConnectionIdSetType".equals(typeName)) {

            return com.amazon.ec2.VpnConnectionIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateTagsResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateTagsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteSnapshotType".equals(typeName)) {

            return com.amazon.ec2.DeleteSnapshotType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelBundleTaskResponseType".equals(typeName)) {

            return com.amazon.ec2.CancelBundleTaskResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReplaceRouteResponseType".equals(typeName)) {

            return com.amazon.ec2.ReplaceRouteResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DetachNetworkInterfaceResponseType".equals(typeName)) {

            return com.amazon.ec2.DetachNetworkInterfaceResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSecurityGroupsType".equals(typeName)) {

            return com.amazon.ec2.DescribeSecurityGroupsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeDhcpOptionsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeDhcpOptionsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesOfferingsResponseSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesOfferingsResponseSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssignPrivateIpAddressesType".equals(typeName)) {

            return com.amazon.ec2.AssignPrivateIpAddressesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteNetworkAclEntryType".equals(typeName)) {

            return com.amazon.ec2.DeleteNetworkAclEntryType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceCountsSetType".equals(typeName)) {

            return com.amazon.ec2.InstanceCountsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceCountsSetItemType".equals(typeName)) {

            return com.amazon.ec2.InstanceCountsSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DiskImageType".equals(typeName)) {

            return com.amazon.ec2.DiskImageType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RegionItemType".equals(typeName)) {

            return com.amazon.ec2.RegionItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkInterfaceIdSetType".equals(typeName)) {

            return com.amazon.ec2.NetworkInterfaceIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PurchaseReservedInstancesOfferingResponseType".equals(typeName)) {

            return com.amazon.ec2.PurchaseReservedInstancesOfferingResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LicenseSetItemType".equals(typeName)) {

            return com.amazon.ec2.LicenseSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceIdSetType".equals(typeName)) {

            return com.amazon.ec2.InstanceIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssignPrivateIpAddressesResponseType".equals(typeName)) {

            return com.amazon.ec2.AssignPrivateIpAddressesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteRouteResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteRouteResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RebootInstancesItemType".equals(typeName)) {

            return com.amazon.ec2.RebootInstancesItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "EbsBlockDeviceType".equals(typeName)) {

            return com.amazon.ec2.EbsBlockDeviceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumeStatusType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumeStatusType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteSubnetType".equals(typeName)) {

            return com.amazon.ec2.DeleteSubnetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesListingsResponseSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesListingsResponseSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RouteType".equals(typeName)) {

            return com.amazon.ec2.RouteType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeBundleTasksType".equals(typeName)) {

            return com.amazon.ec2.DescribeBundleTasksType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteNetworkInterfaceResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteNetworkInterfaceResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReportInstanceStatusType".equals(typeName)) {

            return com.amazon.ec2.ReportInstanceStatusType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAddressesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeAddressesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteRouteType".equals(typeName)) {

            return com.amazon.ec2.DeleteRouteType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "MonitorInstancesType".equals(typeName)) {

            return com.amazon.ec2.MonitorInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSubnetsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSubnetsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "TagSetType".equals(typeName)) {

            return com.amazon.ec2.TagSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpcType".equals(typeName)) {

            return com.amazon.ec2.VpcType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelBundleTaskType".equals(typeName)) {

            return com.amazon.ec2.CancelBundleTaskType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PurchaseReservedInstancesOfferingType".equals(typeName)) {

            return com.amazon.ec2.PurchaseReservedInstancesOfferingType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DhcpOptionsIdSetType".equals(typeName)) {

            return com.amazon.ec2.DhcpOptionsIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReplaceRouteTableAssociationResponseType".equals(typeName)) {

            return com.amazon.ec2.ReplaceRouteTableAssociationResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PropagatingVgwSetType".equals(typeName)) {

            return com.amazon.ec2.PropagatingVgwSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "TagSetItemType".equals(typeName)) {

            return com.amazon.ec2.TagSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkAclAssociationSetType".equals(typeName)) {

            return com.amazon.ec2.NetworkAclAssociationSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImageAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeImageAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReservationSetType".equals(typeName)) {

            return com.amazon.ec2.ReservationSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumesSetResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumesSetResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SubnetIdSetType".equals(typeName)) {

            return com.amazon.ec2.SubnetIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceMonitoringStateType".equals(typeName)) {

            return com.amazon.ec2.InstanceMonitoringStateType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeKeyPairsItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeKeyPairsItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteInternetGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteInternetGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkInterfaceAttachmentType".equals(typeName)) {

            return com.amazon.ec2.NetworkInterfaceAttachmentType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResetImageAttributeType".equals(typeName)) {

            return com.amazon.ec2.ResetImageAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVpnConnectionType".equals(typeName)) {

            return com.amazon.ec2.CreateVpnConnectionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotInstanceStateFaultType".equals(typeName)) {

            return com.amazon.ec2.SpotInstanceStateFaultType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeExportTasksType".equals(typeName)) {

            return com.amazon.ec2.DescribeExportTasksType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribePlacementGroupsInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribePlacementGroupsInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AvailabilityZoneSetType".equals(typeName)) {

            return com.amazon.ec2.AvailabilityZoneSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "MonitorInstancesResponseSetItemType".equals(typeName)) {

            return com.amazon.ec2.MonitorInstancesResponseSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "EnableVgwRoutePropagationResponseType".equals(typeName)) {

            return com.amazon.ec2.EnableVgwRoutePropagationResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusEventsSetType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusEventsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesResponseItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesResponseItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeConversionTasksType".equals(typeName)) {

            return com.amazon.ec2.DescribeConversionTasksType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceTypeSetType".equals(typeName)) {

            return com.amazon.ec2.InstanceTypeSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PricingDetailsSetType".equals(typeName)) {

            return com.amazon.ec2.PricingDetailsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssignPrivateIpAddressesSetItemRequestType".equals(typeName)) {

            return com.amazon.ec2.AssignPrivateIpAddressesSetItemRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BundleInstanceS3StorageType".equals(typeName)) {

            return com.amazon.ec2.BundleInstanceS3StorageType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteSpotDatafeedSubscriptionType".equals(typeName)) {

            return com.amazon.ec2.DeleteSpotDatafeedSubscriptionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssociateAddressType".equals(typeName)) {

            return com.amazon.ec2.AssociateAddressType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "TerminateInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.TerminateInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ExportToS3TaskResponseType".equals(typeName)) {

            return com.amazon.ec2.ExportToS3TaskResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeCustomerGatewaysType".equals(typeName)) {

            return com.amazon.ec2.DescribeCustomerGatewaysType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnConnectionType".equals(typeName)) {

            return com.amazon.ec2.VpnConnectionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "IpRangeSetType".equals(typeName)) {

            return com.amazon.ec2.IpRangeSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteTagsSetItemType".equals(typeName)) {

            return com.amazon.ec2.DeleteTagsSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DisassociateRouteTableType".equals(typeName)) {

            return com.amazon.ec2.DisassociateRouteTableType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteSecurityGroupType".equals(typeName)) {

            return com.amazon.ec2.DeleteSecurityGroupType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DisassociateAddressType".equals(typeName)) {

            return com.amazon.ec2.DisassociateAddressType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotInstanceRequestIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.SpotInstanceRequestIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeTagsType".equals(typeName)) {

            return com.amazon.ec2.DescribeTagsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReportInstanceStatusReasonCodeSetItemType".equals(typeName)) {

            return com.amazon.ec2.ReportInstanceStatusReasonCodeSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RequestSpotInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.RequestSpotInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ActivateLicenseResponseType".equals(typeName)) {

            return com.amazon.ec2.ActivateLicenseResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportVolumeType".equals(typeName)) {

            return com.amazon.ec2.ImportVolumeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAvailabilityZonesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeAvailabilityZonesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AuthorizeSecurityGroupEgressResponseType".equals(typeName)) {

            return com.amazon.ec2.AuthorizeSecurityGroupEgressResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SubnetType".equals(typeName)) {

            return com.amazon.ec2.SubnetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStateChangeSetType".equals(typeName)) {

            return com.amazon.ec2.InstanceStateChangeSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportVolumeTaskDetailsType".equals(typeName)) {

            return com.amazon.ec2.ImportVolumeTaskDetailsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "EnableVolumeIOType".equals(typeName)) {

            return com.amazon.ec2.EnableVolumeIOType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeRouteTablesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeRouteTablesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusEventItemType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusEventItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAddressesResponseItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeAddressesResponseItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVpcsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeVpcsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkAclIdSetType".equals(typeName)) {

            return com.amazon.ec2.NetworkAclIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumeStatusResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumeStatusResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeNetworkAclsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeNetworkAclsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeletePlacementGroupResponseType".equals(typeName)) {

            return com.amazon.ec2.DeletePlacementGroupResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeregisterImageResponseType".equals(typeName)) {

            return com.amazon.ec2.DeregisterImageResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceTypeSetItemType".equals(typeName)) {

            return com.amazon.ec2.InstanceTypeSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInstanceAttributeType".equals(typeName)) {

            return com.amazon.ec2.DescribeInstanceAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateSecurityGroupType".equals(typeName)) {

            return com.amazon.ec2.CreateSecurityGroupType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVolumeResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteVolumeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumesSetItemResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumesSetItemResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BundleInstanceType".equals(typeName)) {

            return com.amazon.ec2.BundleInstanceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVolumePermissionItemType".equals(typeName)) {

            return com.amazon.ec2.CreateVolumePermissionItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteKeyPairResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteKeyPairResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ProductCodeListType".equals(typeName)) {

            return com.amazon.ec2.ProductCodeListType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InternetGatewayIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.InternetGatewayIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVpcResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteVpcResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeKeyPairsResponseInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribeKeyPairsResponseInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReplaceNetworkAclAssociationType".equals(typeName)) {

            return com.amazon.ec2.ReplaceNetworkAclAssociationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsOwnersType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsOwnersType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportInstanceGroupItemType".equals(typeName)) {

            return com.amazon.ec2.ImportInstanceGroupItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ExportToS3TaskType".equals(typeName)) {

            return com.amazon.ec2.ExportToS3TaskType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LicenseCapacitySetItemType".equals(typeName)) {

            return com.amazon.ec2.LicenseCapacitySetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportInstanceType".equals(typeName)) {

            return com.amazon.ec2.ImportInstanceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesOwnerType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesOwnerType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RouteTableAssociationType".equals(typeName)) {

            return com.amazon.ec2.RouteTableAssociationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVpnConnectionType".equals(typeName)) {

            return com.amazon.ec2.DeleteVpnConnectionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeBundleTasksItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeBundleTasksItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RevokeSecurityGroupEgressType".equals(typeName)) {

            return com.amazon.ec2.RevokeSecurityGroupEgressType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "IpRangeItemType".equals(typeName)) {

            return com.amazon.ec2.IpRangeItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVpcType".equals(typeName)) {

            return com.amazon.ec2.DeleteVpcType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LicenseSetType".equals(typeName)) {

            return com.amazon.ec2.LicenseSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PlacementGroupSetType".equals(typeName)) {

            return com.amazon.ec2.PlacementGroupSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesResponseSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesResponseSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DetachVpnGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.DetachVpnGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LaunchPermissionItemType".equals(typeName)) {

            return com.amazon.ec2.LaunchPermissionItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpcSetType".equals(typeName)) {

            return com.amazon.ec2.VpcSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceNetworkInterfaceSetItemRequestType".equals(typeName)) {

            return com.amazon.ec2.InstanceNetworkInterfaceSetItemRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelExportTaskType".equals(typeName)) {

            return com.amazon.ec2.CancelExportTaskType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumeAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumeAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVpnConnectionRouteType".equals(typeName)) {

            return com.amazon.ec2.CreateVpnConnectionRouteType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "UserIdGroupPairSetType".equals(typeName)) {

            return com.amazon.ec2.UserIdGroupPairSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStateChangeType".equals(typeName)) {

            return com.amazon.ec2.InstanceStateChangeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateInstanceExportTaskType".equals(typeName)) {

            return com.amazon.ec2.CreateInstanceExportTaskType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportInstanceVolumeDetailItemType".equals(typeName)) {

            return com.amazon.ec2.ImportInstanceVolumeDetailItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnConnectionOptionsResponseType".equals(typeName)) {

            return com.amazon.ec2.VpnConnectionOptionsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteTagsType".equals(typeName)) {

            return com.amazon.ec2.DeleteTagsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstancePrivateIpAddressesSetItemType".equals(typeName)) {

            return com.amazon.ec2.InstancePrivateIpAddressesSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVolumeResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateVolumeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateKeyPairResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateKeyPairResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteCustomerGatewayType".equals(typeName)) {

            return com.amazon.ec2.DeleteCustomerGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AllocateAddressResponseType".equals(typeName)) {

            return com.amazon.ec2.AllocateAddressResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RegionSetType".equals(typeName)) {

            return com.amazon.ec2.RegionSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVpnConnectionResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteVpnConnectionResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PriceScheduleRequestSetType".equals(typeName)) {

            return com.amazon.ec2.PriceScheduleRequestSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeExportTasksResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeExportTasksResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ConversionTaskIdItemType".equals(typeName)) {

            return com.amazon.ec2.ConversionTaskIdItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachVolumeType".equals(typeName)) {

            return com.amazon.ec2.AttachVolumeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AvailabilityZoneMessageType".equals(typeName)) {

            return com.amazon.ec2.AvailabilityZoneMessageType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSecurityGroupsSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeSecurityGroupsSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BlockDeviceMappingItemType".equals(typeName)) {

            return com.amazon.ec2.BlockDeviceMappingItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InternetGatewayType".equals(typeName)) {

            return com.amazon.ec2.InternetGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BundleInstanceTaskStorageType".equals(typeName)) {

            return com.amazon.ec2.BundleInstanceTaskStorageType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DiskImageVolumeType".equals(typeName)) {

            return com.amazon.ec2.DiskImageVolumeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PriceScheduleSetItemType".equals(typeName)) {

            return com.amazon.ec2.PriceScheduleSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PlacementResponseType".equals(typeName)) {

            return com.amazon.ec2.PlacementResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PropagatingVgwType".equals(typeName)) {

            return com.amazon.ec2.PropagatingVgwType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DiskImageSetType".equals(typeName)) {

            return com.amazon.ec2.DiskImageSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeactivateLicenseType".equals(typeName)) {

            return com.amazon.ec2.DeactivateLicenseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeNetworkInterfaceAttributeType".equals(typeName)) {

            return com.amazon.ec2.DescribeNetworkInterfaceAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnStaticRouteType".equals(typeName)) {

            return com.amazon.ec2.VpnStaticRouteType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ConversionTaskSetType".equals(typeName)) {

            return com.amazon.ec2.ConversionTaskSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnGatewayIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.VpnGatewayIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyImageAttributeType".equals(typeName)) {

            return com.amazon.ec2.ModifyImageAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RunningInstancesSetType".equals(typeName)) {

            return com.amazon.ec2.RunningInstancesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RunInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.RunInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "GetConsoleOutputResponseType".equals(typeName)) {

            return com.amazon.ec2.GetConsoleOutputResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "StopInstancesType".equals(typeName)) {

            return com.amazon.ec2.StopInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DhcpOptionsType".equals(typeName)) {

            return com.amazon.ec2.DhcpOptionsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LaunchPermissionListType".equals(typeName)) {

            return com.amazon.ec2.LaunchPermissionListType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStatusType".equals(typeName)) {

            return com.amazon.ec2.InstanceStatusType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SecurityGroupIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.SecurityGroupIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVpnConnectionsType".equals(typeName)) {

            return com.amazon.ec2.DescribeVpnConnectionsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyInstanceAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ModifyInstanceAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpcIdSetType".equals(typeName)) {

            return com.amazon.ec2.VpcIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceBlockDeviceMappingResponseType".equals(typeName)) {

            return com.amazon.ec2.InstanceBlockDeviceMappingResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RevokeSecurityGroupIngressType".equals(typeName)) {

            return com.amazon.ec2.RevokeSecurityGroupIngressType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BundleInstanceTaskErrorType".equals(typeName)) {

            return com.amazon.ec2.BundleInstanceTaskErrorType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeKeyPairsResponseItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeKeyPairsResponseItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "MonitorInstancesSetType".equals(typeName)) {

            return com.amazon.ec2.MonitorInstancesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NullableAttributeValueType".equals(typeName)) {

            return com.amazon.ec2.NullableAttributeValueType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "IpPermissionSetType".equals(typeName)) {

            return com.amazon.ec2.IpPermissionSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsSetResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsSetResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVolumeType".equals(typeName)) {

            return com.amazon.ec2.DeleteVolumeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportInstanceResponseType".equals(typeName)) {

            return com.amazon.ec2.ImportInstanceResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportKeyPairType".equals(typeName)) {

            return com.amazon.ec2.ImportKeyPairType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeKeyPairsType".equals(typeName)) {

            return com.amazon.ec2.DescribeKeyPairsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeNetworkAclsType".equals(typeName)) {

            return com.amazon.ec2.DescribeNetworkAclsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteRouteTableResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteRouteTableResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateTagsType".equals(typeName)) {

            return com.amazon.ec2.CreateTagsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifySnapshotAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ModifySnapshotAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AvailabilityZoneMessageSetType".equals(typeName)) {

            return com.amazon.ec2.AvailabilityZoneMessageSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PlacementGroupInfoType".equals(typeName)) {

            return com.amazon.ec2.PlacementGroupInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusDetailsItemType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusDetailsItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVpcResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateVpcResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteDhcpOptionsType".equals(typeName)) {

            return com.amazon.ec2.DeleteDhcpOptionsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ProductCodesSetType".equals(typeName)) {

            return com.amazon.ec2.ProductCodesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SecurityGroupItemType".equals(typeName)) {

            return com.amazon.ec2.SecurityGroupItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotInstanceRequestSetItemType".equals(typeName)) {

            return com.amazon.ec2.SpotInstanceRequestSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeBundleTasksResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeBundleTasksResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DhcpConfigurationItemSetType".equals(typeName)) {

            return com.amazon.ec2.DhcpConfigurationItemSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVpcType".equals(typeName)) {

            return com.amazon.ec2.CreateVpcType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ExportTaskIdType".equals(typeName)) {

            return com.amazon.ec2.ExportTaskIdType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ProductCodesSetItemType".equals(typeName)) {

            return com.amazon.ec2.ProductCodesSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStatusEventsSetType".equals(typeName)) {

            return com.amazon.ec2.InstanceStatusEventsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DetachInternetGatewayType".equals(typeName)) {

            return com.amazon.ec2.DetachInternetGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateDhcpOptionsResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateDhcpOptionsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "GroupItemType".equals(typeName)) {

            return com.amazon.ec2.GroupItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateImageType".equals(typeName)) {

            return com.amazon.ec2.CreateImageType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateRouteResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateRouteResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReplaceRouteType".equals(typeName)) {

            return com.amazon.ec2.ReplaceRouteType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResetInstanceAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ResetInstanceAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RevokeSecurityGroupIngressResponseType".equals(typeName)) {

            return com.amazon.ec2.RevokeSecurityGroupIngressResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DiskImageVolumeDescriptionType".equals(typeName)) {

            return com.amazon.ec2.DiskImageVolumeDescriptionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "StartInstancesType".equals(typeName)) {

            return com.amazon.ec2.StartInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DhcpConfigurationItemType".equals(typeName)) {

            return com.amazon.ec2.DhcpConfigurationItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateInternetGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateInternetGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusItemType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "FilterType".equals(typeName)) {

            return com.amazon.ec2.FilterType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsRestorableBySetType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsRestorableBySetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesOfferingsResponseSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesOfferingsResponseSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVolumeType".equals(typeName)) {

            return com.amazon.ec2.CreateVolumeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumesSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RebootInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.RebootInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSubnetsType".equals(typeName)) {

            return com.amazon.ec2.DescribeSubnetsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DiskImageDescriptionType".equals(typeName)) {

            return com.amazon.ec2.DiskImageDescriptionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "UnassignPrivateIpAddressesResponseType".equals(typeName)) {

            return com.amazon.ec2.UnassignPrivateIpAddressesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeNetworkInterfacesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeNetworkInterfacesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VgwTelemetryType".equals(typeName)) {

            return com.amazon.ec2.VgwTelemetryType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAvailabilityZonesType".equals(typeName)) {

            return com.amazon.ec2.DescribeAvailabilityZonesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DetachVolumeType".equals(typeName)) {

            return com.amazon.ec2.DetachVolumeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportInstanceTaskDetailsType".equals(typeName)) {

            return com.amazon.ec2.ImportInstanceTaskDetailsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "MonitorInstancesResponseSetType".equals(typeName)) {

            return com.amazon.ec2.MonitorInstancesResponseSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteNetworkAclType".equals(typeName)) {

            return com.amazon.ec2.DeleteNetworkAclType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSecurityGroupsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSecurityGroupsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CustomerGatewayIdSetType".equals(typeName)) {

            return com.amazon.ec2.CustomerGatewayIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReportInstanceStatusReasonCodesSetType".equals(typeName)) {

            return com.amazon.ec2.ReportInstanceStatusReasonCodesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachVpnGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.AttachVpnGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStatusEventType".equals(typeName)) {

            return com.amazon.ec2.InstanceStatusEventType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateReservedInstancesListingType".equals(typeName)) {

            return com.amazon.ec2.CreateReservedInstancesListingType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "EnableVolumeIOResponseType".equals(typeName)) {

            return com.amazon.ec2.EnableVolumeIOResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceNetworkInterfaceSetItemType".equals(typeName)) {

            return com.amazon.ec2.InstanceNetworkInterfaceSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RevokeSecurityGroupEgressResponseType".equals(typeName)) {

            return com.amazon.ec2.RevokeSecurityGroupEgressResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelReservedInstancesListingResponseType".equals(typeName)) {

            return com.amazon.ec2.CancelReservedInstancesListingResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelConversionTaskType".equals(typeName)) {

            return com.amazon.ec2.CancelConversionTaskType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DhcpOptionsIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.DhcpOptionsIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "GetPasswordDataType".equals(typeName)) {

            return com.amazon.ec2.GetPasswordDataType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InternetGatewaySetType".equals(typeName)) {

            return com.amazon.ec2.InternetGatewaySetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReplaceNetworkAclAssociationResponseType".equals(typeName)) {

            return com.amazon.ec2.ReplaceNetworkAclAssociationResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeNetworkInterfacesType".equals(typeName)) {

            return com.amazon.ec2.DescribeNetworkInterfacesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAddressesType".equals(typeName)) {

            return com.amazon.ec2.DescribeAddressesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CustomerGatewaySetType".equals(typeName)) {

            return com.amazon.ec2.CustomerGatewaySetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesExecutableByType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesExecutableByType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVpnConnectionRouteResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateVpnConnectionRouteResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateKeyPairType".equals(typeName)) {

            return com.amazon.ec2.CreateKeyPairType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportKeyPairResponseType".equals(typeName)) {

            return com.amazon.ec2.ImportKeyPairResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusDetailsSetType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusDetailsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ConversionTaskIdSetType".equals(typeName)) {

            return com.amazon.ec2.ConversionTaskIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PricingDetailsSetItemType".equals(typeName)) {

            return com.amazon.ec2.PricingDetailsSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateCustomerGatewayType".equals(typeName)) {

            return com.amazon.ec2.CreateCustomerGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInternetGatewaysResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeInternetGatewaysResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssociateDhcpOptionsType".equals(typeName)) {

            return com.amazon.ec2.AssociateDhcpOptionsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AllocationIdSetType".equals(typeName)) {

            return com.amazon.ec2.AllocationIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AuthorizeSecurityGroupEgressType".equals(typeName)) {

            return com.amazon.ec2.AuthorizeSecurityGroupEgressType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceNetworkInterfaceSetRequestType".equals(typeName)) {

            return com.amazon.ec2.InstanceNetworkInterfaceSetRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInstancesType".equals(typeName)) {

            return com.amazon.ec2.DescribeInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ValueType".equals(typeName)) {

            return com.amazon.ec2.ValueType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotPriceHistorySetType".equals(typeName)) {

            return com.amazon.ec2.SpotPriceHistorySetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachmentSetResponseType".equals(typeName)) {

            return com.amazon.ec2.AttachmentSetResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStatusDetailsSetItemType".equals(typeName)) {

            return com.amazon.ec2.InstanceStatusDetailsSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnConnectionIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.VpnConnectionIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RunInstancesType".equals(typeName)) {

            return com.amazon.ec2.RunInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BundleInstanceResponseType".equals(typeName)) {

            return com.amazon.ec2.BundleInstanceResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteTagsSetType".equals(typeName)) {

            return com.amazon.ec2.DeleteTagsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyVolumeAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ModifyVolumeAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVpnConnectionRouteResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteVpnConnectionRouteResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSpotPriceHistoryType".equals(typeName)) {

            return com.amazon.ec2.DescribeSpotPriceHistoryType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteCustomerGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteCustomerGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AllocateAddressType".equals(typeName)) {

            return com.amazon.ec2.AllocateAddressType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnConnectionOptionsRequestType".equals(typeName)) {

            return com.amazon.ec2.VpnConnectionOptionsRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateNetworkInterfaceType".equals(typeName)) {

            return com.amazon.ec2.CreateNetworkInterfaceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CustomerGatewayType".equals(typeName)) {

            return com.amazon.ec2.CustomerGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInstanceAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeInstanceAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LaunchPermissionOperationType".equals(typeName)) {

            return com.amazon.ec2.LaunchPermissionOperationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteTagsResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteTagsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyNetworkInterfaceAttributeType".equals(typeName)) {

            return com.amazon.ec2.ModifyNetworkInterfaceAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkAclEntrySetType".equals(typeName)) {

            return com.amazon.ec2.NetworkAclEntrySetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVpcsType".equals(typeName)) {

            return com.amazon.ec2.DescribeVpcsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResetNetworkInterfaceAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ResetNetworkInterfaceAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DetachNetworkInterfaceType".equals(typeName)) {

            return com.amazon.ec2.DetachNetworkInterfaceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteSubnetResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteSubnetResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyImageAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ModifyImageAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceNetworkInterfaceSetType".equals(typeName)) {

            return com.amazon.ec2.InstanceNetworkInterfaceSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "MonitoringInstanceType".equals(typeName)) {

            return com.amazon.ec2.MonitoringInstanceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RouteTableAssociationSetType".equals(typeName)) {

            return com.amazon.ec2.RouteTableAssociationSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceEbsBlockDeviceType".equals(typeName)) {

            return com.amazon.ec2.InstanceEbsBlockDeviceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InternetGatewayAttachmentSetType".equals(typeName)) {

            return com.amazon.ec2.InternetGatewayAttachmentSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkInterfaceIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.NetworkInterfaceIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateNetworkAclEntryResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateNetworkAclEntryResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSecurityGroupsSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeSecurityGroupsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceNetworkInterfaceAttachmentType".equals(typeName)) {

            return com.amazon.ec2.InstanceNetworkInterfaceAttachmentType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReplaceNetworkAclEntryType".equals(typeName)) {

            return com.amazon.ec2.ReplaceNetworkAclEntryType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifySnapshotAttributeType".equals(typeName)) {

            return com.amazon.ec2.ModifySnapshotAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "StartInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.StartInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVpnConnectionsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeVpnConnectionsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachNetworkInterfaceType".equals(typeName)) {

            return com.amazon.ec2.AttachNetworkInterfaceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DisableVgwRoutePropagationRequestType".equals(typeName)) {

            return com.amazon.ec2.DisableVgwRoutePropagationRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DhcpOptionsSetType".equals(typeName)) {

            return com.amazon.ec2.DhcpOptionsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStatusItemType".equals(typeName)) {

            return com.amazon.ec2.InstanceStatusItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AuthorizeSecurityGroupIngressType".equals(typeName)) {

            return com.amazon.ec2.AuthorizeSecurityGroupIngressType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "EmptyElementType".equals(typeName)) {

            return com.amazon.ec2.EmptyElementType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeDhcpOptionsType".equals(typeName)) {

            return com.amazon.ec2.DescribeDhcpOptionsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeactivateLicenseResponseType".equals(typeName)) {

            return com.amazon.ec2.DeactivateLicenseResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateImageResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateImageResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "IpPermissionType".equals(typeName)) {

            return com.amazon.ec2.IpPermissionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpcIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.VpcIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PortRangeType".equals(typeName)) {

            return com.amazon.ec2.PortRangeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVpnGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateVpnGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "MonitorInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.MonitorInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachmentSetItemResponseType".equals(typeName)) {

            return com.amazon.ec2.AttachmentSetItemResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInstancesInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribeInstancesInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSpotInstanceRequestsType".equals(typeName)) {

            return com.amazon.ec2.DescribeSpotInstanceRequestsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInternetGatewaysType".equals(typeName)) {

            return com.amazon.ec2.DescribeInternetGatewaysType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DhcpValueSetType".equals(typeName)) {

            return com.amazon.ec2.DhcpValueSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ConfirmProductInstanceResponseType".equals(typeName)) {

            return com.amazon.ec2.ConfirmProductInstanceResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttributeValueType".equals(typeName)) {

            return com.amazon.ec2.AttributeValueType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStatusSetType".equals(typeName)) {

            return com.amazon.ec2.InstanceStatusSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateSecurityGroupResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateSecurityGroupResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeCustomerGatewaysResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeCustomerGatewaysResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "GroupSetType".equals(typeName)) {

            return com.amazon.ec2.GroupSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateRouteType".equals(typeName)) {

            return com.amazon.ec2.CreateRouteType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RouteTableSetType".equals(typeName)) {

            return com.amazon.ec2.RouteTableSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateNetworkAclType".equals(typeName)) {

            return com.amazon.ec2.CreateNetworkAclType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "UserIdGroupPairType".equals(typeName)) {

            return com.amazon.ec2.UserIdGroupPairType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceLicenseResponseType".equals(typeName)) {

            return com.amazon.ec2.InstanceLicenseResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSecurityGroupsIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeSecurityGroupsIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReservedInstanceLimitPriceType".equals(typeName)) {

            return com.amazon.ec2.ReservedInstanceLimitPriceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteDhcpOptionsResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteDhcpOptionsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreatePlacementGroupResponseType".equals(typeName)) {

            return com.amazon.ec2.CreatePlacementGroupResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CustomerGatewayIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.CustomerGatewayIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteNetworkAclEntryResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteNetworkAclEntryResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "UnassignPrivateIpAddressesType".equals(typeName)) {

            return com.amazon.ec2.UnassignPrivateIpAddressesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SecurityGroupSetType".equals(typeName)) {

            return com.amazon.ec2.SecurityGroupSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesOfferingsSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesOfferingsSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PriceScheduleSetType".equals(typeName)) {

            return com.amazon.ec2.PriceScheduleSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInstanceStatusType".equals(typeName)) {

            return com.amazon.ec2.DescribeInstanceStatusType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeBundleTasksInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribeBundleTasksInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SubnetSetType".equals(typeName)) {

            return com.amazon.ec2.SubnetSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAddressesItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeAddressesItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesListingSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesListingSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkInterfacePrivateIpAddressesSetType".equals(typeName)) {

            return com.amazon.ec2.NetworkInterfacePrivateIpAddressesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceNetworkInterfaceAssociationType".equals(typeName)) {

            return com.amazon.ec2.InstanceNetworkInterfaceAssociationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachNetworkInterfaceResponseType".equals(typeName)) {

            return com.amazon.ec2.AttachNetworkInterfaceResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LaunchSpecificationRequestType".equals(typeName)) {

            return com.amazon.ec2.LaunchSpecificationRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkAclSetType".equals(typeName)) {

            return com.amazon.ec2.NetworkAclSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInstancesItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeInstancesItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateNetworkInterfaceResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateNetworkInterfaceResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ActivateLicenseType".equals(typeName)) {

            return com.amazon.ec2.ActivateLicenseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeregisterImageType".equals(typeName)) {

            return com.amazon.ec2.DeregisterImageType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusInfoType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelConversionTaskResponseType".equals(typeName)) {

            return com.amazon.ec2.CancelConversionTaskResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteRouteTableType".equals(typeName)) {

            return com.amazon.ec2.DeleteRouteTableType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotInstanceRequestIdSetType".equals(typeName)) {

            return com.amazon.ec2.SpotInstanceRequestIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceLicenseRequestType".equals(typeName)) {

            return com.amazon.ec2.InstanceLicenseRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AuthorizeSecurityGroupIngressResponseType".equals(typeName)) {

            return com.amazon.ec2.AuthorizeSecurityGroupIngressResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PriceScheduleRequestSetItemType".equals(typeName)) {

            return com.amazon.ec2.PriceScheduleRequestSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "EnableVgwRoutePropagationRequestType".equals(typeName)) {

            return com.amazon.ec2.EnableVgwRoutePropagationRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeTagsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeTagsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInstancesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeInstancesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachmentType".equals(typeName)) {

            return com.amazon.ec2.AttachmentType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssociateRouteTableResponseType".equals(typeName)) {

            return com.amazon.ec2.AssociateRouteTableResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "UserDataType".equals(typeName)) {

            return com.amazon.ec2.UserDataType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribePlacementGroupItemType".equals(typeName)) {

            return com.amazon.ec2.DescribePlacementGroupItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeLicensesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeLicensesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BundleInstanceTasksSetType".equals(typeName)) {

            return com.amazon.ec2.BundleInstanceTasksSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnStaticRoutesSetType".equals(typeName)) {

            return com.amazon.ec2.VpnStaticRoutesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteSecurityGroupResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteSecurityGroupResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResetSnapshotAttributeType".equals(typeName)) {

            return com.amazon.ec2.ResetSnapshotAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribePlacementGroupsType".equals(typeName)) {

            return com.amazon.ec2.DescribePlacementGroupsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesOfferingsType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesOfferingsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RecurringChargesSetItemType".equals(typeName)) {

            return com.amazon.ec2.RecurringChargesSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteVpnGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteVpnGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeLicensesType".equals(typeName)) {

            return com.amazon.ec2.DescribeLicensesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkInterfaceType".equals(typeName)) {

            return com.amazon.ec2.NetworkInterfaceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResourceTagSetItemType".equals(typeName)) {

            return com.amazon.ec2.ResourceTagSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PlacementRequestType".equals(typeName)) {

            return com.amazon.ec2.PlacementRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVpnConnectionResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateVpnConnectionResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateNetworkAclResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateNetworkAclResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeKeyPairsInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribeKeyPairsInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssociateAddressResponseType".equals(typeName)) {

            return com.amazon.ec2.AssociateAddressResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelSpotInstanceRequestsResponseType".equals(typeName)) {

            return com.amazon.ec2.CancelSpotInstanceRequestsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LaunchSpecificationResponseType".equals(typeName)) {

            return com.amazon.ec2.LaunchSpecificationResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeRouteTablesType".equals(typeName)) {

            return com.amazon.ec2.DescribeRouteTablesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReleaseAddressType".equals(typeName)) {

            return com.amazon.ec2.ReleaseAddressType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RegisterImageResponseType".equals(typeName)) {

            return com.amazon.ec2.RegisterImageResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVpnGatewaysType".equals(typeName)) {

            return com.amazon.ec2.DescribeVpnGatewaysType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSecurityGroupsIdSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeSecurityGroupsIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelExportTaskResponseType".equals(typeName)) {

            return com.amazon.ec2.CancelExportTaskResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesOfferingsSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesOfferingsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ConversionTaskType".equals(typeName)) {

            return com.amazon.ec2.ConversionTaskType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ValueSetType".equals(typeName)) {

            return com.amazon.ec2.ValueSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceBlockDeviceMappingItemType".equals(typeName)) {

            return com.amazon.ec2.InstanceBlockDeviceMappingItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeKeyPairsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeKeyPairsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "LicenseCapacitySetType".equals(typeName)) {

            return com.amazon.ec2.LicenseCapacitySetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RebootInstancesInfoType".equals(typeName)) {

            return com.amazon.ec2.RebootInstancesInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkAclAssociationType".equals(typeName)) {

            return com.amazon.ec2.NetworkAclAssociationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsRestorableByType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsRestorableByType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteSpotDatafeedSubscriptionResponseType".equals(typeName)) {

            return com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RouteTableType".equals(typeName)) {

            return com.amazon.ec2.RouteTableType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateCustomerGatewayResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateCustomerGatewayResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DiskImageDetailType".equals(typeName)) {

            return com.amazon.ec2.DiskImageDetailType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RebootInstancesType".equals(typeName)) {

            return com.amazon.ec2.RebootInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumeAttributeType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumeAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReplaceRouteTableAssociationType".equals(typeName)) {

            return com.amazon.ec2.ReplaceRouteTableAssociationType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesOfferingsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesOfferingsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "PrivateIpAddressesSetRequestType".equals(typeName)) {

            return com.amazon.ec2.PrivateIpAddressesSetRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteKeyPairType".equals(typeName)) {

            return com.amazon.ec2.DeleteKeyPairType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSpotPriceHistoryResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSpotPriceHistoryResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AvailabilityZoneItemType".equals(typeName)) {

            return com.amazon.ec2.AvailabilityZoneItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeRegionsType".equals(typeName)) {

            return com.amazon.ec2.DescribeRegionsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AssociateDhcpOptionsResponseType".equals(typeName)) {

            return com.amazon.ec2.AssociateDhcpOptionsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnGatewaySetType".equals(typeName)) {

            return com.amazon.ec2.VpnGatewaySetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttributeBooleanValueType".equals(typeName)) {

            return com.amazon.ec2.AttributeBooleanValueType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceBlockDeviceMappingType".equals(typeName)) {

            return com.amazon.ec2.InstanceBlockDeviceMappingType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateSpotDatafeedSubscriptionType".equals(typeName)) {

            return com.amazon.ec2.CreateSpotDatafeedSubscriptionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ExportTaskSetResponseType".equals(typeName)) {

            return com.amazon.ec2.ExportTaskSetResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResetInstanceAttributeType".equals(typeName)) {

            return com.amazon.ec2.ResetInstanceAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "IamInstanceProfileResponseType".equals(typeName)) {

            return com.amazon.ec2.IamInstanceProfileResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstancePrivateIpAddressesSetType".equals(typeName)) {

            return com.amazon.ec2.InstancePrivateIpAddressesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelSpotInstanceRequestsType".equals(typeName)) {

            return com.amazon.ec2.CancelSpotInstanceRequestsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResetImageAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ResetImageAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "BundleInstanceTaskType".equals(typeName)) {

            return com.amazon.ec2.BundleInstanceTaskType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAvailabilityZonesSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeAvailabilityZonesSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesListingsType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesListingsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DisassociateAddressResponseType".equals(typeName)) {

            return com.amazon.ec2.DisassociateAddressResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSpotDatafeedSubscriptionResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachVpnGatewayType".equals(typeName)) {

            return com.amazon.ec2.AttachVpnGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ResourceTagSetType".equals(typeName)) {

            return com.amazon.ec2.ResourceTagSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RegisterImageType".equals(typeName)) {

            return com.amazon.ec2.RegisterImageType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesListingsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesListingsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelReservedInstancesListingType".equals(typeName)) {

            return com.amazon.ec2.CancelReservedInstancesListingType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteInternetGatewayType".equals(typeName)) {

            return com.amazon.ec2.DeleteInternetGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkAclType".equals(typeName)) {

            return com.amazon.ec2.NetworkAclType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnGatewayIdSetType".equals(typeName)) {

            return com.amazon.ec2.VpnGatewayIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NetworkInterfacePrivateIpAddressesSetItemType".equals(typeName)) {

            return com.amazon.ec2.NetworkInterfacePrivateIpAddressesSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAddressesResponseInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribeAddressesResponseInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ImportVolumeResponseType".equals(typeName)) {

            return com.amazon.ec2.ImportVolumeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "IamInstanceProfileRequestType".equals(typeName)) {

            return com.amazon.ec2.IamInstanceProfileRequestType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "IcmpTypeCodeType".equals(typeName)) {

            return com.amazon.ec2.IcmpTypeCodeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSpotInstanceRequestsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSpotInstanceRequestsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DetachVolumeResponseType".equals(typeName)) {

            return com.amazon.ec2.DetachVolumeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ProductDescriptionSetItemType".equals(typeName)) {

            return com.amazon.ec2.ProductDescriptionSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVpnGatewayType".equals(typeName)) {

            return com.amazon.ec2.CreateVpnGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSpotDatafeedSubscriptionType".equals(typeName)) {

            return com.amazon.ec2.DescribeSpotDatafeedSubscriptionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyNetworkInterfaceAttributeResponseType".equals(typeName)) {

            return com.amazon.ec2.ModifyNetworkInterfaceAttributeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DisassociateRouteTableResponseType".equals(typeName)) {

            return com.amazon.ec2.DisassociateRouteTableResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesListingsResponseSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesListingsResponseSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateDhcpOptionsType".equals(typeName)) {

            return com.amazon.ec2.CreateDhcpOptionsType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeRegionsSetItemType".equals(typeName)) {

            return com.amazon.ec2.DescribeRegionsSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachInternetGatewayType".equals(typeName)) {

            return com.amazon.ec2.AttachInternetGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotInstanceRequestSetType".equals(typeName)) {

            return com.amazon.ec2.SpotInstanceRequestSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeInstanceStatusResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeInstanceStatusResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStateType".equals(typeName)) {

            return com.amazon.ec2.InstanceStateType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RouteTableIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.RouteTableIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "AttachVolumeResponseType".equals(typeName)) {

            return com.amazon.ec2.AttachVolumeResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InternetGatewayIdSetType".equals(typeName)) {

            return com.amazon.ec2.InternetGatewayIdSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAddressesInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribeAddressesInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VpnConnectionSetType".equals(typeName)) {

            return com.amazon.ec2.VpnConnectionSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeReservedInstancesListingSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeReservedInstancesListingSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "EbsInstanceBlockDeviceMappingResponseType".equals(typeName)) {

            return com.amazon.ec2.EbsInstanceBlockDeviceMappingResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeConversionTasksResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeConversionTasksResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateVolumePermissionListType".equals(typeName)) {

            return com.amazon.ec2.CreateVolumePermissionListType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImageAttributeType".equals(typeName)) {

            return com.amazon.ec2.DescribeImageAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ModifyVolumeAttributeType".equals(typeName)) {

            return com.amazon.ec2.ModifyVolumeAttributeType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DeleteNetworkInterfaceType".equals(typeName)) {

            return com.amazon.ec2.DeleteNetworkInterfaceType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeSnapshotsResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeSnapshotsResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CancelSpotInstanceRequestsResponseSetType".equals(typeName)) {

            return com.amazon.ec2.CancelSpotInstanceRequestsResponseSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVolumesType".equals(typeName)) {

            return com.amazon.ec2.DescribeVolumesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RequestSpotInstancesType".equals(typeName)) {

            return com.amazon.ec2.RequestSpotInstancesType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "StateReasonType".equals(typeName)) {

            return com.amazon.ec2.StateReasonType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeVpnGatewaysResponseType".equals(typeName)) {

            return com.amazon.ec2.DescribeVpnGatewaysResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "CreateRouteTableResponseType".equals(typeName)) {

            return com.amazon.ec2.CreateRouteTableResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeAvailabilityZonesSetType".equals(typeName)) {

            return com.amazon.ec2.DescribeAvailabilityZonesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "VolumeStatusActionsSetType".equals(typeName)) {

            return com.amazon.ec2.VolumeStatusActionsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "RecurringChargesSetType".equals(typeName)) {

            return com.amazon.ec2.RecurringChargesSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SpotDatafeedSubscriptionType".equals(typeName)) {

            return com.amazon.ec2.SpotDatafeedSubscriptionType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReleaseAddressResponseType".equals(typeName)) {

            return com.amazon.ec2.ReleaseAddressResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "GetConsoleOutputType".equals(typeName)) {

            return com.amazon.ec2.GetConsoleOutputType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "SubnetIdSetItemType".equals(typeName)) {

            return com.amazon.ec2.SubnetIdSetItemType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DescribeImagesResponseInfoType".equals(typeName)) {

            return com.amazon.ec2.DescribeImagesResponseInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ReservationInfoType".equals(typeName)) {

            return com.amazon.ec2.ReservationInfoType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InternetGatewayAttachmentType".equals(typeName)) {

            return com.amazon.ec2.InternetGatewayAttachmentType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DisableVgwRoutePropagationResponseType".equals(typeName)) {

            return com.amazon.ec2.DisableVgwRoutePropagationResponseType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "NullableAttributeBooleanValueType".equals(typeName)) {

            return com.amazon.ec2.NullableAttributeBooleanValueType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "DetachVpnGatewayType".equals(typeName)) {

            return com.amazon.ec2.DetachVpnGatewayType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "InstanceStatusDetailsSetType".equals(typeName)) {

            return com.amazon.ec2.InstanceStatusDetailsSetType.Factory.parse(reader);

        }

        if ("http://ec2.amazonaws.com/doc/2012-08-15/".equals(namespaceURI) && "ConfirmProductInstanceType".equals(typeName)) {

            return com.amazon.ec2.ConfirmProductInstanceType.Factory.parse(reader);

        }

        throw new org.apache.axis2.databinding.ADBException("Unsupported type " + namespaceURI + " " + typeName);
    }

}
