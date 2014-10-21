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

import org.apache.log4j.Logger;

import com.amazon.ec2.ActivateLicense;
import com.amazon.ec2.ActivateLicenseResponse;
import com.amazon.ec2.AllocateAddress;
import com.amazon.ec2.AllocateAddressResponse;
import com.amazon.ec2.AmazonEC2SkeletonInterface;
import com.amazon.ec2.AssignPrivateIpAddresses;
import com.amazon.ec2.AssignPrivateIpAddressesResponse;
import com.amazon.ec2.AssociateAddress;
import com.amazon.ec2.AssociateAddressResponse;
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
import com.amazon.ec2.AttachVpnGateway;
import com.amazon.ec2.AttachVpnGatewayResponse;
import com.amazon.ec2.AuthorizeSecurityGroupEgress;
import com.amazon.ec2.AuthorizeSecurityGroupEgressResponse;
import com.amazon.ec2.AuthorizeSecurityGroupIngress;
import com.amazon.ec2.AuthorizeSecurityGroupIngressResponse;
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
import com.amazon.ec2.CreateInstanceExportTask;
import com.amazon.ec2.CreateInstanceExportTaskResponse;
import com.amazon.ec2.CreateInternetGateway;
import com.amazon.ec2.CreateInternetGatewayResponse;
import com.amazon.ec2.CreateKeyPair;
import com.amazon.ec2.CreateKeyPairResponse;
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
import com.amazon.ec2.CreateSnapshot;
import com.amazon.ec2.CreateSnapshotResponse;
import com.amazon.ec2.CreateSpotDatafeedSubscription;
import com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse;
import com.amazon.ec2.CreateSubnet;
import com.amazon.ec2.CreateSubnetResponse;
import com.amazon.ec2.CreateTags;
import com.amazon.ec2.CreateTagsResponse;
import com.amazon.ec2.CreateVolume;
import com.amazon.ec2.CreateVolumeResponse;
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
import com.amazon.ec2.DeleteSnapshot;
import com.amazon.ec2.DeleteSnapshotResponse;
import com.amazon.ec2.DeleteSpotDatafeedSubscription;
import com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse;
import com.amazon.ec2.DeleteSubnet;
import com.amazon.ec2.DeleteSubnetResponse;
import com.amazon.ec2.DeleteTags;
import com.amazon.ec2.DeleteTagsResponse;
import com.amazon.ec2.DeleteVolume;
import com.amazon.ec2.DeleteVolumeResponse;
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
import com.amazon.ec2.DescribeAddresses;
import com.amazon.ec2.DescribeAddressesResponse;
import com.amazon.ec2.DescribeAvailabilityZones;
import com.amazon.ec2.DescribeAvailabilityZonesResponse;
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
import com.amazon.ec2.DescribeImages;
import com.amazon.ec2.DescribeImagesResponse;
import com.amazon.ec2.DescribeInstanceAttribute;
import com.amazon.ec2.DescribeInstanceAttributeResponse;
import com.amazon.ec2.DescribeInstanceStatus;
import com.amazon.ec2.DescribeInstanceStatusResponse;
import com.amazon.ec2.DescribeInstances;
import com.amazon.ec2.DescribeInstancesResponse;
import com.amazon.ec2.DescribeInternetGateways;
import com.amazon.ec2.DescribeInternetGatewaysResponse;
import com.amazon.ec2.DescribeKeyPairs;
import com.amazon.ec2.DescribeKeyPairsResponse;
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
import com.amazon.ec2.DescribeSnapshotAttribute;
import com.amazon.ec2.DescribeSnapshotAttributeResponse;
import com.amazon.ec2.DescribeSnapshots;
import com.amazon.ec2.DescribeSnapshotsResponse;
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
import com.amazon.ec2.DescribeVolumeAttribute;
import com.amazon.ec2.DescribeVolumeAttributeResponse;
import com.amazon.ec2.DescribeVolumeStatus;
import com.amazon.ec2.DescribeVolumeStatusResponse;
import com.amazon.ec2.DescribeVolumes;
import com.amazon.ec2.DescribeVolumesResponse;
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
import com.amazon.ec2.DetachVpnGateway;
import com.amazon.ec2.DetachVpnGatewayResponse;
import com.amazon.ec2.DisableVgwRoutePropagation;
import com.amazon.ec2.DisableVgwRoutePropagationResponse;
import com.amazon.ec2.DisassociateAddress;
import com.amazon.ec2.DisassociateAddressResponse;
import com.amazon.ec2.DisassociateRouteTable;
import com.amazon.ec2.DisassociateRouteTableResponse;
import com.amazon.ec2.EnableVgwRoutePropagation;
import com.amazon.ec2.EnableVgwRoutePropagationResponse;
import com.amazon.ec2.EnableVolumeIO;
import com.amazon.ec2.EnableVolumeIOResponse;
import com.amazon.ec2.GetConsoleOutput;
import com.amazon.ec2.GetConsoleOutputResponse;
import com.amazon.ec2.GetPasswordData;
import com.amazon.ec2.GetPasswordDataResponse;
import com.amazon.ec2.ImportInstance;
import com.amazon.ec2.ImportInstanceResponse;
import com.amazon.ec2.ImportKeyPair;
import com.amazon.ec2.ImportKeyPairResponse;
import com.amazon.ec2.ImportVolume;
import com.amazon.ec2.ImportVolumeResponse;
import com.amazon.ec2.ModifyImageAttribute;
import com.amazon.ec2.ModifyImageAttributeResponse;
import com.amazon.ec2.ModifyInstanceAttribute;
import com.amazon.ec2.ModifyInstanceAttributeResponse;
import com.amazon.ec2.ModifyNetworkInterfaceAttribute;
import com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse;
import com.amazon.ec2.ModifySnapshotAttribute;
import com.amazon.ec2.ModifySnapshotAttributeResponse;
import com.amazon.ec2.ModifyVolumeAttribute;
import com.amazon.ec2.ModifyVolumeAttributeResponse;
import com.amazon.ec2.MonitorInstances;
import com.amazon.ec2.MonitorInstancesResponse;
import com.amazon.ec2.PurchaseReservedInstancesOffering;
import com.amazon.ec2.PurchaseReservedInstancesOfferingResponse;
import com.amazon.ec2.RebootInstances;
import com.amazon.ec2.RebootInstancesResponse;
import com.amazon.ec2.RegisterImage;
import com.amazon.ec2.RegisterImageResponse;
import com.amazon.ec2.ReleaseAddress;
import com.amazon.ec2.ReleaseAddressResponse;
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
import com.amazon.ec2.ResetImageAttribute;
import com.amazon.ec2.ResetImageAttributeResponse;
import com.amazon.ec2.ResetInstanceAttribute;
import com.amazon.ec2.ResetInstanceAttributeResponse;
import com.amazon.ec2.ResetNetworkInterfaceAttribute;
import com.amazon.ec2.ResetNetworkInterfaceAttributeResponse;
import com.amazon.ec2.ResetSnapshotAttribute;
import com.amazon.ec2.ResetSnapshotAttributeResponse;
import com.amazon.ec2.RevokeSecurityGroupEgress;
import com.amazon.ec2.RevokeSecurityGroupEgressResponse;
import com.amazon.ec2.RevokeSecurityGroupIngress;
import com.amazon.ec2.RevokeSecurityGroupIngressResponse;
import com.amazon.ec2.RunInstances;
import com.amazon.ec2.RunInstancesResponse;
import com.amazon.ec2.StartInstances;
import com.amazon.ec2.StartInstancesResponse;
import com.amazon.ec2.StopInstances;
import com.amazon.ec2.StopInstancesResponse;
import com.amazon.ec2.TerminateInstances;
import com.amazon.ec2.TerminateInstancesResponse;
import com.amazon.ec2.UnassignPrivateIpAddresses;
import com.amazon.ec2.UnassignPrivateIpAddressesResponse;
import com.amazon.ec2.UnmonitorInstances;
import com.amazon.ec2.UnmonitorInstancesResponse;

import com.cloud.bridge.service.controller.s3.ServiceProvider;

public class EC2SoapService implements AmazonEC2SkeletonInterface {
    protected final static Logger logger = Logger.getLogger(EC2SoapService.class);

    public AllocateAddressResponse allocateAddress(AllocateAddress allocateAddress) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.allocateAddress(allocateAddress);
    }

    public AssociateAddressResponse associateAddress(AssociateAddress associateAddress) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.associateAddress(associateAddress);
    }

    public AssociateDhcpOptionsResponse associateDhcpOptions(AssociateDhcpOptions associateDhcpOptions) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.associateDhcpOptions(associateDhcpOptions);
    }

    public AttachVolumeResponse attachVolume(AttachVolume attachVolume) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.attachVolume(attachVolume);
    }

    public AttachVpnGatewayResponse attachVpnGateway(AttachVpnGateway attachVpnGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.attachVpnGateway(attachVpnGateway);
    }

    public AuthorizeSecurityGroupIngressResponse authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngress authorizeSecurityGroupIngress) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.authorizeSecurityGroupIngress(authorizeSecurityGroupIngress);
    }

    public BundleInstanceResponse bundleInstance(BundleInstance bundleInstance) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.bundleInstance(bundleInstance);
    }

    public CancelBundleTaskResponse cancelBundleTask(CancelBundleTask cancelBundleTask) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.cancelBundleTask(cancelBundleTask);
    }

    public CancelSpotInstanceRequestsResponse cancelSpotInstanceRequests(CancelSpotInstanceRequests cancelSpotInstanceRequests) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.cancelSpotInstanceRequests(cancelSpotInstanceRequests);
    }

    public ConfirmProductInstanceResponse confirmProductInstance(ConfirmProductInstance confirmProductInstance) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.confirmProductInstance(confirmProductInstance);
    }

    public CreateCustomerGatewayResponse createCustomerGateway(CreateCustomerGateway createCustomerGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createCustomerGateway(createCustomerGateway);
    }

    public CreateDhcpOptionsResponse createDhcpOptions(CreateDhcpOptions createDhcpOptions) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createDhcpOptions(createDhcpOptions);
    }

    public CreateImageResponse createImage(CreateImage createImage) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createImage(createImage);
    }

    public CreateKeyPairResponse createKeyPair(CreateKeyPair createKeyPair) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createKeyPair(createKeyPair);
    }

    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroup createSecurityGroup) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createSecurityGroup(createSecurityGroup);
    }

    public CreateSnapshotResponse createSnapshot(CreateSnapshot createSnapshot) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createSnapshot(createSnapshot);
    }

    public CreateSpotDatafeedSubscriptionResponse createSpotDatafeedSubscription(CreateSpotDatafeedSubscription createSpotDatafeedSubscription) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createSpotDatafeedSubscription(createSpotDatafeedSubscription);
    }

    public CreateSubnetResponse createSubnet(CreateSubnet createSubnet) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createSubnet(createSubnet);
    }

    public CreateVolumeResponse createVolume(CreateVolume createVolume) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createVolume(createVolume);
    }

    public CreateVpcResponse createVpc(CreateVpc createVpc) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createVpc(createVpc);
    }

    public CreateVpnConnectionResponse createVpnConnection(CreateVpnConnection createVpnConnection) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createVpnConnection(createVpnConnection);
    }

    public CreateVpnGatewayResponse createVpnGateway(CreateVpnGateway createVpnGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createVpnGateway(createVpnGateway);
    }

    public DeleteCustomerGatewayResponse deleteCustomerGateway(DeleteCustomerGateway deleteCustomerGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteCustomerGateway(deleteCustomerGateway);
    }

    public DeleteDhcpOptionsResponse deleteDhcpOptions(DeleteDhcpOptions deleteDhcpOptions) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteDhcpOptions(deleteDhcpOptions);
    }

    public DeleteKeyPairResponse deleteKeyPair(DeleteKeyPair deleteKeyPair) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteKeyPair(deleteKeyPair);
    }

    public DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroup deleteSecurityGroup) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteSecurityGroup(deleteSecurityGroup);
    }

    public DeleteSnapshotResponse deleteSnapshot(DeleteSnapshot deleteSnapshot) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteSnapshot(deleteSnapshot);
    }

    public DeleteSpotDatafeedSubscriptionResponse deleteSpotDatafeedSubscription(DeleteSpotDatafeedSubscription deleteSpotDatafeedSubscription) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteSpotDatafeedSubscription(deleteSpotDatafeedSubscription);
    }

    public DeleteSubnetResponse deleteSubnet(DeleteSubnet deleteSubnet) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteSubnet(deleteSubnet);
    }

    public DeleteVolumeResponse deleteVolume(DeleteVolume deleteVolume) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteVolume(deleteVolume);
    }

    public DeleteVpcResponse deleteVpc(DeleteVpc deleteVpc) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteVpc(deleteVpc);
    }

    public DeleteVpnConnectionResponse deleteVpnConnection(DeleteVpnConnection deleteVpnConnection) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteVpnConnection(deleteVpnConnection);
    }

    public DeleteVpnGatewayResponse deleteVpnGateway(DeleteVpnGateway deleteVpnGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteVpnGateway(deleteVpnGateway);
    }

    public DeregisterImageResponse deregisterImage(DeregisterImage deregisterImage) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deregisterImage(deregisterImage);
    }

    public DescribeAddressesResponse describeAddresses(DescribeAddresses describeAddresses) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeAddresses(describeAddresses);
    }

    public DescribeAvailabilityZonesResponse describeAvailabilityZones(DescribeAvailabilityZones describeAvailabilityZones) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeAvailabilityZones(describeAvailabilityZones);
    }

    public DescribeBundleTasksResponse describeBundleTasks(DescribeBundleTasks describeBundleTasks) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeBundleTasks(describeBundleTasks);
    }

    public DescribeCustomerGatewaysResponse describeCustomerGateways(DescribeCustomerGateways describeCustomerGateways) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeCustomerGateways(describeCustomerGateways);
    }

    public DescribeDhcpOptionsResponse describeDhcpOptions(DescribeDhcpOptions describeDhcpOptions) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeDhcpOptions(describeDhcpOptions);
    }

    public DescribeImageAttributeResponse describeImageAttribute(DescribeImageAttribute describeImageAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeImageAttribute(describeImageAttribute);
    }

    public DescribeImagesResponse describeImages(DescribeImages describeImages) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeImages(describeImages);
    }

    public DescribeInstanceAttributeResponse describeInstanceAttribute(DescribeInstanceAttribute describeInstanceAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeInstanceAttribute(describeInstanceAttribute);
    }

    public DescribeInstancesResponse describeInstances(DescribeInstances describeInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeInstances(describeInstances);
    }

    public DescribeKeyPairsResponse describeKeyPairs(DescribeKeyPairs describeKeyPairs) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeKeyPairs(describeKeyPairs);
    }

    public DescribeRegionsResponse describeRegions(DescribeRegions describeRegions) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeRegions(describeRegions);
    }

    public DescribeReservedInstancesResponse describeReservedInstances(DescribeReservedInstances describeReservedInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeReservedInstances(describeReservedInstances);
    }

    public DescribeReservedInstancesOfferingsResponse describeReservedInstancesOfferings(DescribeReservedInstancesOfferings describeReservedInstancesOfferings) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeReservedInstancesOfferings(describeReservedInstancesOfferings);
    }

    public DescribeSecurityGroupsResponse describeSecurityGroups(DescribeSecurityGroups describeSecurityGroups) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeSecurityGroups(describeSecurityGroups);
    }

    public DescribeSnapshotAttributeResponse describeSnapshotAttribute(DescribeSnapshotAttribute describeSnapshotAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeSnapshotAttribute(describeSnapshotAttribute);
    }

    public DescribeSnapshotsResponse describeSnapshots(DescribeSnapshots describeSnapshots) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeSnapshots(describeSnapshots);
    }

    public DescribeSpotDatafeedSubscriptionResponse describeSpotDatafeedSubscription(DescribeSpotDatafeedSubscription describeSpotDatafeedSubscription) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeSpotDatafeedSubscription(describeSpotDatafeedSubscription);
    }

    public DescribeSpotInstanceRequestsResponse describeSpotInstanceRequests(DescribeSpotInstanceRequests describeSpotInstanceRequests) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeSpotInstanceRequests(describeSpotInstanceRequests);
    }

    public DescribeSpotPriceHistoryResponse describeSpotPriceHistory(DescribeSpotPriceHistory describeSpotPriceHistory) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeSpotPriceHistory(describeSpotPriceHistory);
    }

    public DescribeSubnetsResponse describeSubnets(DescribeSubnets describeSubnets) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeSubnets(describeSubnets);
    }

    public DescribeVolumesResponse describeVolumes(DescribeVolumes describeVolumes) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeVolumes(describeVolumes);
    }

    public DescribeVpcsResponse describeVpcs(DescribeVpcs describeVpcs) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeVpcs(describeVpcs);
    }

    public DescribeVpnConnectionsResponse describeVpnConnections(DescribeVpnConnections describeVpnConnections) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeVpnConnections(describeVpnConnections);
    }

    public DescribeVpnGatewaysResponse describeVpnGateways(DescribeVpnGateways describeVpnGateways) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeVpnGateways(describeVpnGateways);
    }

    public DetachVolumeResponse detachVolume(DetachVolume detachVolume) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.detachVolume(detachVolume);
    }

    public DetachVpnGatewayResponse detachVpnGateway(DetachVpnGateway detachVpnGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.detachVpnGateway(detachVpnGateway);
    }

    public DisassociateAddressResponse disassociateAddress(DisassociateAddress disassociateAddress) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.disassociateAddress(disassociateAddress);
    }

    public GetConsoleOutputResponse getConsoleOutput(GetConsoleOutput getConsoleOutput) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.getConsoleOutput(getConsoleOutput);
    }

    public GetPasswordDataResponse getPasswordData(GetPasswordData getPasswordData) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.getPasswordData(getPasswordData);
    }

    public ModifyImageAttributeResponse modifyImageAttribute(ModifyImageAttribute modifyImageAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.modifyImageAttribute(modifyImageAttribute);
    }

    public ModifyInstanceAttributeResponse modifyInstanceAttribute(ModifyInstanceAttribute modifyInstanceAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.modifyInstanceAttribute(modifyInstanceAttribute);
    }

    public ModifySnapshotAttributeResponse modifySnapshotAttribute(ModifySnapshotAttribute modifySnapshotAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.modifySnapshotAttribute(modifySnapshotAttribute);
    }

    public MonitorInstancesResponse monitorInstances(MonitorInstances monitorInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.monitorInstances(monitorInstances);
    }

    public PurchaseReservedInstancesOfferingResponse purchaseReservedInstancesOffering(PurchaseReservedInstancesOffering purchaseReservedInstancesOffering) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.purchaseReservedInstancesOffering(purchaseReservedInstancesOffering);
    }

    public RebootInstancesResponse rebootInstances(RebootInstances rebootInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.rebootInstances(rebootInstances);
    }

    public RegisterImageResponse registerImage(RegisterImage registerImage) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.registerImage(registerImage);
    }

    public ReleaseAddressResponse releaseAddress(ReleaseAddress releaseAddress) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.releaseAddress(releaseAddress);
    }

    public RequestSpotInstancesResponse requestSpotInstances(RequestSpotInstances requestSpotInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.requestSpotInstances(requestSpotInstances);
    }

    public ResetImageAttributeResponse resetImageAttribute(ResetImageAttribute resetImageAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.resetImageAttribute(resetImageAttribute);
    }

    public ResetInstanceAttributeResponse resetInstanceAttribute(ResetInstanceAttribute resetInstanceAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.resetInstanceAttribute(resetInstanceAttribute);
    }

    public ResetSnapshotAttributeResponse resetSnapshotAttribute(ResetSnapshotAttribute resetSnapshotAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.resetSnapshotAttribute(resetSnapshotAttribute);
    }

    public RevokeSecurityGroupIngressResponse revokeSecurityGroupIngress(RevokeSecurityGroupIngress revokeSecurityGroupIngress) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.revokeSecurityGroupIngress(revokeSecurityGroupIngress);
    }

    public RunInstancesResponse runInstances(RunInstances runInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.runInstances(runInstances);
    }

    public StartInstancesResponse startInstances(StartInstances startInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.startInstances(startInstances);
    }

    public StopInstancesResponse stopInstances(StopInstances stopInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.stopInstances(stopInstances);
    }

    public TerminateInstancesResponse terminateInstances(TerminateInstances terminateInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.terminateInstances(terminateInstances);
    }

    public UnmonitorInstancesResponse unmonitorInstances(UnmonitorInstances unmonitorInstances) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.unmonitorInstances(unmonitorInstances);
    }

    public ActivateLicenseResponse activateLicense(ActivateLicense activateLicense) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.activateLicense(activateLicense);
    }

    public CreatePlacementGroupResponse createPlacementGroup(CreatePlacementGroup createPlacementGroup) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createPlacementGroup(createPlacementGroup);
    }

    public DeactivateLicenseResponse deactivateLicense(DeactivateLicense deactivateLicense) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deactivateLicense(deactivateLicense);
    }

    public DeletePlacementGroupResponse deletePlacementGroup(DeletePlacementGroup deletePlacementGroup) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deletePlacementGroup(deletePlacementGroup);
    }

    public DescribeLicensesResponse describeLicenses(DescribeLicenses describeLicenses) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeLicenses(describeLicenses);
    }

    public DescribePlacementGroupsResponse describePlacementGroups(DescribePlacementGroups describePlacementGroups) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describePlacementGroups(describePlacementGroups);
    }

    public DescribeTagsResponse describeTags(DescribeTags describeTags) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeTags(describeTags);
    }

    public CreateTagsResponse createTags(CreateTags createTags) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createTags(createTags);
    }

    public DeleteTagsResponse deleteTags(DeleteTags deleteTags) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteTags(deleteTags);
    }

    public ImportKeyPairResponse importKeyPair(ImportKeyPair importKeyPair) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.importKeyPair(importKeyPair);
    }

    @Override
    public CancelConversionTaskResponse cancelConversionTask(CancelConversionTask cancelConversionTask) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.cancelConversionTask(cancelConversionTask);
    }

    @Override
    public DescribeConversionTasksResponse describeConversionTasks(DescribeConversionTasks describeConversionTasks) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeConversionTasks(describeConversionTasks);
    }

    @Override
    public ImportInstanceResponse importInstance(ImportInstance importInstance) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.importInstance(importInstance);
    }

    @Override
    public ImportVolumeResponse importVolume(ImportVolume importVolume) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.importVolume(importVolume);
    }

    @Override
    public ResetNetworkInterfaceAttributeResponse resetNetworkInterfaceAttribute(ResetNetworkInterfaceAttribute resetNetworkInterfaceAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.resetNetworkInterfaceAttribute(resetNetworkInterfaceAttribute);
    }

    @Override
    public CreateRouteTableResponse createRouteTable(CreateRouteTable createRouteTable) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createRouteTable(createRouteTable);
    }

    @Override
    public CreateNetworkAclEntryResponse createNetworkAclEntry(CreateNetworkAclEntry createNetworkAclEntry) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createNetworkAclEntry(createNetworkAclEntry);
    }

    @Override
    public DescribeVolumeAttributeResponse describeVolumeAttribute(DescribeVolumeAttribute describeVolumeAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeVolumeAttribute(describeVolumeAttribute);
    }

    @Override
    public DeleteNetworkInterfaceResponse deleteNetworkInterface(DeleteNetworkInterface deleteNetworkInterface) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteNetworkInterface(deleteNetworkInterface);
    }

    @Override
    public CreateInternetGatewayResponse createInternetGateway(CreateInternetGateway createInternetGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createInternetGateway(createInternetGateway);
    }

    @Override
    public DisassociateRouteTableResponse disassociateRouteTable(DisassociateRouteTable disassociateRouteTable) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.disassociateRouteTable(disassociateRouteTable);
    }

    @Override
    public ReplaceNetworkAclEntryResponse replaceNetworkAclEntry(ReplaceNetworkAclEntry replaceNetworkAclEntry) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.replaceNetworkAclEntry(replaceNetworkAclEntry);
    }

    @Override
    public AuthorizeSecurityGroupEgressResponse authorizeSecurityGroupEgress(AuthorizeSecurityGroupEgress authorizeSecurityGroupEgress) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.authorizeSecurityGroupEgress(authorizeSecurityGroupEgress);
    }

    @Override
    public DeleteNetworkAclEntryResponse deleteNetworkAclEntry(DeleteNetworkAclEntry deleteNetworkAclEntry) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteNetworkAclEntry(deleteNetworkAclEntry);
    }

    @Override
    public DeleteRouteTableResponse deleteRouteTable(DeleteRouteTable deleteRouteTable) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteRouteTable(deleteRouteTable);
    }

    @Override
    public DescribeNetworkInterfaceAttributeResponse describeNetworkInterfaceAttribute(DescribeNetworkInterfaceAttribute describeNetworkInterfaceAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeNetworkInterfaceAttribute(describeNetworkInterfaceAttribute);
    }

    @Override
    public CreateReservedInstancesListingResponse createReservedInstancesListing(CreateReservedInstancesListing createReservedInstancesListing) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createReservedInstancesListing(createReservedInstancesListing);
    }

    @Override
    public CreateNetworkAclResponse createNetworkAcl(CreateNetworkAcl createNetworkAcl) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createNetworkAcl(createNetworkAcl);
    }

    @Override
    public ModifyVolumeAttributeResponse modifyVolumeAttribute(ModifyVolumeAttribute modifyVolumeAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.modifyVolumeAttribute(modifyVolumeAttribute);
    }

    @Override
    public ReplaceNetworkAclAssociationResponse replaceNetworkAclAssociation(ReplaceNetworkAclAssociation replaceNetworkAclAssociation) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.replaceNetworkAclAssociation(replaceNetworkAclAssociation);
    }

    @Override
    public EnableVgwRoutePropagationResponse enableVgwRoutePropagation(EnableVgwRoutePropagation enableVgwRoutePropagation) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.enableVgwRoutePropagation(enableVgwRoutePropagation);
    }

    @Override
    public UnassignPrivateIpAddressesResponse unassignPrivateIpAddresses(UnassignPrivateIpAddresses unassignPrivateIpAddresses) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.unassignPrivateIpAddresses(unassignPrivateIpAddresses);
    }

    @Override
    public DeleteVpnConnectionRouteResponse deleteVpnConnectionRoute(DeleteVpnConnectionRoute deleteVpnConnectionRoute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteVpnConnectionRoute(deleteVpnConnectionRoute);
    }

    @Override
    public CancelReservedInstancesListingResponse cancelReservedInstancesListing(CancelReservedInstancesListing cancelReservedInstancesListing) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.cancelReservedInstancesListing(cancelReservedInstancesListing);
    }

    @Override
    public DescribeNetworkAclsResponse describeNetworkAcls(DescribeNetworkAcls describeNetworkAcls) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeNetworkAcls(describeNetworkAcls);
    }

    @Override
    public EnableVolumeIOResponse enableVolumeIO(EnableVolumeIO enableVolumeIO) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.enableVolumeIO(enableVolumeIO);
    }

    @Override
    public DescribeInternetGatewaysResponse describeInternetGateways(DescribeInternetGateways describeInternetGateways) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeInternetGateways(describeInternetGateways);
    }

    @Override
    public DescribeReservedInstancesListingsResponse describeReservedInstancesListings(DescribeReservedInstancesListings describeReservedInstancesListings) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeReservedInstancesListings(describeReservedInstancesListings);
    }

    @Override
    public DescribeInstanceStatusResponse describeInstanceStatus(DescribeInstanceStatus describeInstanceStatus) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeInstanceStatus(describeInstanceStatus);
    }

    @Override
    public ModifyNetworkInterfaceAttributeResponse modifyNetworkInterfaceAttribute(ModifyNetworkInterfaceAttribute modifyNetworkInterfaceAttribute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.modifyNetworkInterfaceAttribute(modifyNetworkInterfaceAttribute);
    }

    @Override
    public DisableVgwRoutePropagationResponse disableVgwRoutePropagation(DisableVgwRoutePropagation disableVgwRoutePropagation) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.disableVgwRoutePropagation(disableVgwRoutePropagation);
    }

    @Override
    public DescribeVolumeStatusResponse describeVolumeStatus(DescribeVolumeStatus describeVolumeStatus) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeVolumeStatus(describeVolumeStatus);
    }

    @Override
    public DetachNetworkInterfaceResponse detachNetworkInterface(DetachNetworkInterface detachNetworkInterface) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.detachNetworkInterface(detachNetworkInterface);
    }

    @Override
    public DescribeNetworkInterfacesResponse describeNetworkInterfaces(DescribeNetworkInterfaces describeNetworkInterfaces) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeNetworkInterfaces(describeNetworkInterfaces);
    }

    @Override
    public CancelExportTaskResponse cancelExportTask(CancelExportTask cancelExportTask) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.cancelExportTask(cancelExportTask);
    }

    @Override
    public CreateRouteResponse createRoute(CreateRoute createRoute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createRoute(createRoute);
    }

    @Override
    public DescribeRouteTablesResponse describeRouteTables(DescribeRouteTables describeRouteTables) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeRouteTables(describeRouteTables);
    }

    @Override
    public DeleteNetworkAclResponse deleteNetworkAcl(DeleteNetworkAcl deleteNetworkAcl) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteNetworkAcl(deleteNetworkAcl);
    }

    @Override
    public DeleteRouteResponse deleteRoute(DeleteRoute deleteRoute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteRoute(deleteRoute);
    }

    @Override
    public CreateVpnConnectionRouteResponse createVpnConnectionRoute(CreateVpnConnectionRoute createVpnConnectionRoute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createVpnConnectionRoute(createVpnConnectionRoute);
    }

    @Override
    public AttachInternetGatewayResponse attachInternetGateway(AttachInternetGateway attachInternetGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.attachInternetGateway(attachInternetGateway);
    }

    @Override
    public ReplaceRouteTableAssociationResponse replaceRouteTableAssociation(ReplaceRouteTableAssociation replaceRouteTableAssociation) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.replaceRouteTableAssociation(replaceRouteTableAssociation);
    }

    @Override
    public AssociateRouteTableResponse associateRouteTable(AssociateRouteTable associateRouteTable) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.associateRouteTable(associateRouteTable);
    }

    @Override
    public DetachInternetGatewayResponse detachInternetGateway(DetachInternetGateway detachInternetGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.detachInternetGateway(detachInternetGateway);
    }

    @Override
    public DescribeExportTasksResponse describeExportTasks(DescribeExportTasks describeExportTasks) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.describeExportTasks(describeExportTasks);
    }

    @Override
    public CreateInstanceExportTaskResponse createInstanceExportTask(CreateInstanceExportTask createInstanceExportTask) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createInstanceExportTask(createInstanceExportTask);
    }

    @Override
    public AssignPrivateIpAddressesResponse assignPrivateIpAddresses(AssignPrivateIpAddresses assignPrivateIpAddresses) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.assignPrivateIpAddresses(assignPrivateIpAddresses);
    }

    @Override
    public ReportInstanceStatusResponse reportInstanceStatus(ReportInstanceStatus reportInstanceStatus) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.reportInstanceStatus(reportInstanceStatus);
    }

    @Override
    public DeleteInternetGatewayResponse deleteInternetGateway(DeleteInternetGateway deleteInternetGateway) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.deleteInternetGateway(deleteInternetGateway);
    }

    @Override
    public AttachNetworkInterfaceResponse attachNetworkInterface(AttachNetworkInterface attachNetworkInterface) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.attachNetworkInterface(attachNetworkInterface);
    }

    @Override
    public CreateNetworkInterfaceResponse createNetworkInterface(CreateNetworkInterface createNetworkInterface) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.createNetworkInterface(createNetworkInterface);
    }

    @Override
    public RevokeSecurityGroupEgressResponse revokeSecurityGroupEgress(RevokeSecurityGroupEgress revokeSecurityGroupEgress) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.revokeSecurityGroupEgress(revokeSecurityGroupEgress);
    }

    @Override
    public ReplaceRouteResponse replaceRoute(ReplaceRoute replaceRoute) {
        AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
        return ec2Service.replaceRoute(replaceRoute);

    }
}