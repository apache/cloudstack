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

import com.amazon.ec2.*;
import com.cloud.bridge.service.controller.s3.ServiceProvider;

public class EC2SoapService implements AmazonEC2SkeletonInterface {
	    protected final static Logger logger = Logger.getLogger(EC2SoapService.class);
	    
		public AllocateAddressResponse allocateAddress(
				AllocateAddress allocateAddress) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.allocateAddress(allocateAddress);
		}
		
		public AssociateAddressResponse associateAddress(
				AssociateAddress associateAddress) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.associateAddress(associateAddress);
		}

		public AssociateDhcpOptionsResponse associateDhcpOptions(
				AssociateDhcpOptions associateDhcpOptions) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.associateDhcpOptions(associateDhcpOptions);
		}
		
		public AttachVolumeResponse attachVolume(AttachVolume attachVolume) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.attachVolume(attachVolume);
		}
		
		public AttachVpnGatewayResponse attachVpnGateway(
				AttachVpnGateway attachVpnGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.attachVpnGateway(attachVpnGateway);
		}
	
		public AuthorizeSecurityGroupIngressResponse authorizeSecurityGroupIngress(
				AuthorizeSecurityGroupIngress authorizeSecurityGroupIngress) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.authorizeSecurityGroupIngress(authorizeSecurityGroupIngress);
		}
	
		public BundleInstanceResponse bundleInstance(
				BundleInstance bundleInstance) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.bundleInstance(bundleInstance);
		}
		
		public CancelBundleTaskResponse cancelBundleTask(
				CancelBundleTask cancelBundleTask) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.cancelBundleTask(cancelBundleTask);
		}
		
		public CancelSpotInstanceRequestsResponse cancelSpotInstanceRequests(
				CancelSpotInstanceRequests cancelSpotInstanceRequests) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.cancelSpotInstanceRequests(cancelSpotInstanceRequests);
		}
		
		public ConfirmProductInstanceResponse confirmProductInstance(
				ConfirmProductInstance confirmProductInstance) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.confirmProductInstance(confirmProductInstance);
		}
		
		public CreateCustomerGatewayResponse createCustomerGateway(
				CreateCustomerGateway createCustomerGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createCustomerGateway(createCustomerGateway);
		}
		
		public CreateDhcpOptionsResponse createDhcpOptions(
				CreateDhcpOptions createDhcpOptions) {
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
		
		public CreateSecurityGroupResponse createSecurityGroup(
				CreateSecurityGroup createSecurityGroup) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createSecurityGroup(createSecurityGroup);
		}
		
		public CreateSnapshotResponse createSnapshot(
				CreateSnapshot createSnapshot) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createSnapshot(createSnapshot);
		}
		
		public CreateSpotDatafeedSubscriptionResponse createSpotDatafeedSubscription(
				CreateSpotDatafeedSubscription createSpotDatafeedSubscription) {
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
		
		public CreateVpnConnectionResponse createVpnConnection(
				CreateVpnConnection createVpnConnection) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createVpnConnection(createVpnConnection);
		}
		
		public CreateVpnGatewayResponse createVpnGateway(
				CreateVpnGateway createVpnGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createVpnGateway(createVpnGateway);
		}
	
		public DeleteCustomerGatewayResponse deleteCustomerGateway(
				DeleteCustomerGateway deleteCustomerGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteCustomerGateway(deleteCustomerGateway);
		}
		
		public DeleteDhcpOptionsResponse deleteDhcpOptions(
				DeleteDhcpOptions deleteDhcpOptions) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteDhcpOptions(deleteDhcpOptions);
		}
		
		public DeleteKeyPairResponse deleteKeyPair(DeleteKeyPair deleteKeyPair) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteKeyPair(deleteKeyPair);
		}
		
		public DeleteSecurityGroupResponse deleteSecurityGroup(
				DeleteSecurityGroup deleteSecurityGroup) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteSecurityGroup(deleteSecurityGroup);
		}
		
		public DeleteSnapshotResponse deleteSnapshot(
				DeleteSnapshot deleteSnapshot) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteSnapshot(deleteSnapshot);
		}

		public DeleteSpotDatafeedSubscriptionResponse deleteSpotDatafeedSubscription(
				DeleteSpotDatafeedSubscription deleteSpotDatafeedSubscription) {
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
	
		public DeleteVpnConnectionResponse deleteVpnConnection(
				DeleteVpnConnection deleteVpnConnection) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteVpnConnection(deleteVpnConnection);
		}
	
		public DeleteVpnGatewayResponse deleteVpnGateway(
				DeleteVpnGateway deleteVpnGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteVpnGateway(deleteVpnGateway);
		}

		public DeregisterImageResponse deregisterImage(
				DeregisterImage deregisterImage) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deregisterImage(deregisterImage);
		}
	
		public DescribeAddressesResponse describeAddresses(
				DescribeAddresses describeAddresses) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeAddresses(describeAddresses);
		}
		
		public DescribeAvailabilityZonesResponse describeAvailabilityZones(
				DescribeAvailabilityZones describeAvailabilityZones) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeAvailabilityZones(describeAvailabilityZones);
		}
		
		public DescribeBundleTasksResponse describeBundleTasks(
				DescribeBundleTasks describeBundleTasks) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeBundleTasks(describeBundleTasks);
		}
		
		public DescribeCustomerGatewaysResponse describeCustomerGateways(
				DescribeCustomerGateways describeCustomerGateways) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeCustomerGateways(describeCustomerGateways);
		}
		
		public DescribeDhcpOptionsResponse describeDhcpOptions(
				DescribeDhcpOptions describeDhcpOptions) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeDhcpOptions(describeDhcpOptions);
		}
		
		public DescribeImageAttributeResponse describeImageAttribute(
				DescribeImageAttribute describeImageAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeImageAttribute(describeImageAttribute);
		}
	
		public DescribeImagesResponse describeImages(
				DescribeImages describeImages) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeImages(describeImages);
		}
		
		public DescribeInstanceAttributeResponse describeInstanceAttribute(
				DescribeInstanceAttribute describeInstanceAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeInstanceAttribute(describeInstanceAttribute);
		}
		
		public DescribeInstancesResponse describeInstances(
				DescribeInstances describeInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeInstances(describeInstances);
		}
		
		public DescribeKeyPairsResponse describeKeyPairs(
				DescribeKeyPairs describeKeyPairs) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeKeyPairs(describeKeyPairs);
		}
		
		public DescribeRegionsResponse describeRegions(
				DescribeRegions describeRegions) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeRegions(describeRegions);
		}
		
		public DescribeReservedInstancesResponse describeReservedInstances(
				DescribeReservedInstances describeReservedInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeReservedInstances(describeReservedInstances);
		}
		
		public DescribeReservedInstancesOfferingsResponse describeReservedInstancesOfferings(
				DescribeReservedInstancesOfferings describeReservedInstancesOfferings) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeReservedInstancesOfferings(describeReservedInstancesOfferings);
		}
		
		public DescribeSecurityGroupsResponse describeSecurityGroups(
				DescribeSecurityGroups describeSecurityGroups) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeSecurityGroups(describeSecurityGroups);
		}
		
		public DescribeSnapshotAttributeResponse describeSnapshotAttribute(
				DescribeSnapshotAttribute describeSnapshotAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeSnapshotAttribute(describeSnapshotAttribute);
		}
		
		public DescribeSnapshotsResponse describeSnapshots(
				DescribeSnapshots describeSnapshots) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeSnapshots(describeSnapshots);
		}
		
		public DescribeSpotDatafeedSubscriptionResponse describeSpotDatafeedSubscription(
				DescribeSpotDatafeedSubscription describeSpotDatafeedSubscription) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeSpotDatafeedSubscription(describeSpotDatafeedSubscription);
		}
		
		public DescribeSpotInstanceRequestsResponse describeSpotInstanceRequests(
				DescribeSpotInstanceRequests describeSpotInstanceRequests) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeSpotInstanceRequests(describeSpotInstanceRequests);
		}
		
		public DescribeSpotPriceHistoryResponse describeSpotPriceHistory(
				DescribeSpotPriceHistory describeSpotPriceHistory) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeSpotPriceHistory(describeSpotPriceHistory);
		}
		
		public DescribeSubnetsResponse describeSubnets(
				DescribeSubnets describeSubnets) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeSubnets(describeSubnets);
		}
		
		public DescribeVolumesResponse describeVolumes(
				DescribeVolumes describeVolumes) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeVolumes(describeVolumes);
		}
		
		public DescribeVpcsResponse describeVpcs(DescribeVpcs describeVpcs) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeVpcs(describeVpcs);
		}
		
		public DescribeVpnConnectionsResponse describeVpnConnections(
				DescribeVpnConnections describeVpnConnections) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeVpnConnections(describeVpnConnections);
		}
		
		public DescribeVpnGatewaysResponse describeVpnGateways(
				DescribeVpnGateways describeVpnGateways) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeVpnGateways(describeVpnGateways);
		}
		
		public DetachVolumeResponse detachVolume(DetachVolume detachVolume) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.detachVolume(detachVolume);
		}
		
		public DetachVpnGatewayResponse detachVpnGateway(
				DetachVpnGateway detachVpnGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.detachVpnGateway(detachVpnGateway);
		}
		
		public DisassociateAddressResponse disassociateAddress(
				DisassociateAddress disassociateAddress) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.disassociateAddress(disassociateAddress);
		}
		
		public GetConsoleOutputResponse getConsoleOutput(
				GetConsoleOutput getConsoleOutput) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.getConsoleOutput(getConsoleOutput);
		}
		
		public GetPasswordDataResponse getPasswordData(
				GetPasswordData getPasswordData) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.getPasswordData(getPasswordData);
		}
		
		public ModifyImageAttributeResponse modifyImageAttribute(
				ModifyImageAttribute modifyImageAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.modifyImageAttribute(modifyImageAttribute);
		}
		
		public ModifyInstanceAttributeResponse modifyInstanceAttribute(
				ModifyInstanceAttribute modifyInstanceAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.modifyInstanceAttribute(modifyInstanceAttribute);
		}
		
		public ModifySnapshotAttributeResponse modifySnapshotAttribute(
				ModifySnapshotAttribute modifySnapshotAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.modifySnapshotAttribute(modifySnapshotAttribute);
		}
		
		public MonitorInstancesResponse monitorInstances(
				MonitorInstances monitorInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.monitorInstances(monitorInstances);
		}
		
		public PurchaseReservedInstancesOfferingResponse purchaseReservedInstancesOffering(
				PurchaseReservedInstancesOffering purchaseReservedInstancesOffering) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.purchaseReservedInstancesOffering(purchaseReservedInstancesOffering);
		}
		
		public RebootInstancesResponse rebootInstances(
				RebootInstances rebootInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.rebootInstances(rebootInstances);
		}
		
		public RegisterImageResponse registerImage(RegisterImage registerImage) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.registerImage(registerImage);
		}
		
		public ReleaseAddressResponse releaseAddress(
				ReleaseAddress releaseAddress) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.releaseAddress(releaseAddress);
		}
		
		public RequestSpotInstancesResponse requestSpotInstances(
				RequestSpotInstances requestSpotInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.requestSpotInstances(requestSpotInstances);
		}
		
		public ResetImageAttributeResponse resetImageAttribute(
				ResetImageAttribute resetImageAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.resetImageAttribute(resetImageAttribute);
		}
		
		public ResetInstanceAttributeResponse resetInstanceAttribute(
				ResetInstanceAttribute resetInstanceAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.resetInstanceAttribute(resetInstanceAttribute);
		}
		
		public ResetSnapshotAttributeResponse resetSnapshotAttribute(
				ResetSnapshotAttribute resetSnapshotAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.resetSnapshotAttribute(resetSnapshotAttribute);
		}
		
		public RevokeSecurityGroupIngressResponse revokeSecurityGroupIngress(
				RevokeSecurityGroupIngress revokeSecurityGroupIngress) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.revokeSecurityGroupIngress(revokeSecurityGroupIngress);
		}
		
		public RunInstancesResponse runInstances(RunInstances runInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.runInstances(runInstances);
		}
		
		public StartInstancesResponse startInstances(
				StartInstances startInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.startInstances(startInstances);
		}
		
		public StopInstancesResponse stopInstances(StopInstances stopInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.stopInstances(stopInstances);
		}
		
		public TerminateInstancesResponse terminateInstances(
				TerminateInstances terminateInstances) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.terminateInstances(terminateInstances);
		}
		
		public UnmonitorInstancesResponse unmonitorInstances(
				UnmonitorInstances unmonitorInstances) {
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
		public ResetNetworkInterfaceAttributeResponse resetNetworkInterfaceAttribute(
				ResetNetworkInterfaceAttribute resetNetworkInterfaceAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.resetNetworkInterfaceAttribute(resetNetworkInterfaceAttribute);
		}

		@Override
		public CreateRouteTableResponse createRouteTable(
				CreateRouteTable createRouteTable) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createRouteTable(createRouteTable);
		}

		@Override
		public CreateNetworkAclEntryResponse createNetworkAclEntry(
				CreateNetworkAclEntry createNetworkAclEntry) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createNetworkAclEntry(createNetworkAclEntry);
		}

		@Override
		public DescribeVolumeAttributeResponse describeVolumeAttribute(
				DescribeVolumeAttribute describeVolumeAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeVolumeAttribute(describeVolumeAttribute);
		}

		@Override
		public DeleteNetworkInterfaceResponse deleteNetworkInterface(
				DeleteNetworkInterface deleteNetworkInterface) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteNetworkInterface(deleteNetworkInterface);
		}

		@Override
		public CreateInternetGatewayResponse createInternetGateway(
				CreateInternetGateway createInternetGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createInternetGateway(createInternetGateway);
		}

		@Override
		public DisassociateRouteTableResponse disassociateRouteTable(
				DisassociateRouteTable disassociateRouteTable) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.disassociateRouteTable(disassociateRouteTable);
		}

		@Override
		public ReplaceNetworkAclEntryResponse replaceNetworkAclEntry(
				ReplaceNetworkAclEntry replaceNetworkAclEntry) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.replaceNetworkAclEntry(replaceNetworkAclEntry);
		}

		@Override
		public AuthorizeSecurityGroupEgressResponse authorizeSecurityGroupEgress(
				AuthorizeSecurityGroupEgress authorizeSecurityGroupEgress) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.authorizeSecurityGroupEgress(authorizeSecurityGroupEgress);
		}

		@Override
		public DeleteNetworkAclEntryResponse deleteNetworkAclEntry(
				DeleteNetworkAclEntry deleteNetworkAclEntry) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteNetworkAclEntry(deleteNetworkAclEntry);
		}

		@Override
		public DeleteRouteTableResponse deleteRouteTable(
				DeleteRouteTable deleteRouteTable) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteRouteTable(deleteRouteTable);
		}

		@Override
		public DescribeNetworkInterfaceAttributeResponse describeNetworkInterfaceAttribute(
				DescribeNetworkInterfaceAttribute describeNetworkInterfaceAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeNetworkInterfaceAttribute(describeNetworkInterfaceAttribute);
		}

		@Override
		public CreateReservedInstancesListingResponse createReservedInstancesListing(
				CreateReservedInstancesListing createReservedInstancesListing) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createReservedInstancesListing(createReservedInstancesListing);
		}

		@Override
		public CreateNetworkAclResponse createNetworkAcl(
				CreateNetworkAcl createNetworkAcl) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createNetworkAcl(createNetworkAcl);
		}

		@Override
		public ModifyVolumeAttributeResponse modifyVolumeAttribute(
				ModifyVolumeAttribute modifyVolumeAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.modifyVolumeAttribute(modifyVolumeAttribute);
		}

		@Override
		public ReplaceNetworkAclAssociationResponse replaceNetworkAclAssociation(
				ReplaceNetworkAclAssociation replaceNetworkAclAssociation) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.replaceNetworkAclAssociation(replaceNetworkAclAssociation);
		}

		@Override
		public EnableVgwRoutePropagationResponse enableVgwRoutePropagation(
				EnableVgwRoutePropagation enableVgwRoutePropagation) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.enableVgwRoutePropagation(enableVgwRoutePropagation);
		}

		@Override
		public UnassignPrivateIpAddressesResponse unassignPrivateIpAddresses(
				UnassignPrivateIpAddresses unassignPrivateIpAddresses) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.unassignPrivateIpAddresses(unassignPrivateIpAddresses);
		}

		@Override
		public DeleteVpnConnectionRouteResponse deleteVpnConnectionRoute(
				DeleteVpnConnectionRoute deleteVpnConnectionRoute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteVpnConnectionRoute(deleteVpnConnectionRoute);
		}

		@Override
		public CancelReservedInstancesListingResponse cancelReservedInstancesListing(
				CancelReservedInstancesListing cancelReservedInstancesListing) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.cancelReservedInstancesListing(cancelReservedInstancesListing);
		}

		@Override
		public DescribeNetworkAclsResponse describeNetworkAcls(
				DescribeNetworkAcls describeNetworkAcls) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeNetworkAcls(describeNetworkAcls);
		}

		@Override
		public EnableVolumeIOResponse enableVolumeIO(
				EnableVolumeIO enableVolumeIO) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.enableVolumeIO(enableVolumeIO);
		}

		@Override
		public DescribeInternetGatewaysResponse describeInternetGateways(
				DescribeInternetGateways describeInternetGateways) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeInternetGateways(describeInternetGateways);
		}

		@Override
		public DescribeReservedInstancesListingsResponse describeReservedInstancesListings(
				DescribeReservedInstancesListings describeReservedInstancesListings) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeReservedInstancesListings(describeReservedInstancesListings);
		}

		@Override
		public DescribeInstanceStatusResponse describeInstanceStatus(
				DescribeInstanceStatus describeInstanceStatus) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeInstanceStatus(describeInstanceStatus);
		}

		@Override
		public ModifyNetworkInterfaceAttributeResponse modifyNetworkInterfaceAttribute(
				ModifyNetworkInterfaceAttribute modifyNetworkInterfaceAttribute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.modifyNetworkInterfaceAttribute(modifyNetworkInterfaceAttribute);
		}

		@Override
		public DisableVgwRoutePropagationResponse disableVgwRoutePropagation(
				DisableVgwRoutePropagation disableVgwRoutePropagation) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.disableVgwRoutePropagation(disableVgwRoutePropagation);
		}

		@Override
		public DescribeVolumeStatusResponse describeVolumeStatus(
				DescribeVolumeStatus describeVolumeStatus) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeVolumeStatus(describeVolumeStatus);
		}

		@Override
		public DetachNetworkInterfaceResponse detachNetworkInterface(
				DetachNetworkInterface detachNetworkInterface) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.detachNetworkInterface(detachNetworkInterface);
		}

		@Override
		public DescribeNetworkInterfacesResponse describeNetworkInterfaces(
				DescribeNetworkInterfaces describeNetworkInterfaces) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeNetworkInterfaces(describeNetworkInterfaces);
		}

		@Override
		public CancelExportTaskResponse cancelExportTask(
				CancelExportTask cancelExportTask) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.cancelExportTask(cancelExportTask);
		}

		@Override
		public CreateRouteResponse createRoute(CreateRoute createRoute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createRoute(createRoute);
		}

		@Override
		public DescribeRouteTablesResponse describeRouteTables(
				DescribeRouteTables describeRouteTables) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeRouteTables(describeRouteTables);
		}

		@Override
		public DeleteNetworkAclResponse deleteNetworkAcl(
				DeleteNetworkAcl deleteNetworkAcl) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteNetworkAcl(deleteNetworkAcl);
		}

		@Override
		public DeleteRouteResponse deleteRoute(DeleteRoute deleteRoute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteRoute(deleteRoute);
		}

		@Override
		public CreateVpnConnectionRouteResponse createVpnConnectionRoute(
				CreateVpnConnectionRoute createVpnConnectionRoute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createVpnConnectionRoute(createVpnConnectionRoute);
		}

		@Override
		public AttachInternetGatewayResponse attachInternetGateway(
				AttachInternetGateway attachInternetGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.attachInternetGateway(attachInternetGateway);
		}

		@Override
		public ReplaceRouteTableAssociationResponse replaceRouteTableAssociation(
				ReplaceRouteTableAssociation replaceRouteTableAssociation) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.replaceRouteTableAssociation(replaceRouteTableAssociation);
		}

		@Override
		public AssociateRouteTableResponse associateRouteTable(
				AssociateRouteTable associateRouteTable) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.associateRouteTable(associateRouteTable);
		}

		@Override
		public DetachInternetGatewayResponse detachInternetGateway(
				DetachInternetGateway detachInternetGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.detachInternetGateway(detachInternetGateway);
		}

		@Override
		public DescribeExportTasksResponse describeExportTasks(
				DescribeExportTasks describeExportTasks) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.describeExportTasks(describeExportTasks);
		}

		@Override
		public CreateInstanceExportTaskResponse createInstanceExportTask(
				CreateInstanceExportTask createInstanceExportTask) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createInstanceExportTask(createInstanceExportTask);
		}

		@Override
		public AssignPrivateIpAddressesResponse assignPrivateIpAddresses(
				AssignPrivateIpAddresses assignPrivateIpAddresses) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.assignPrivateIpAddresses(assignPrivateIpAddresses);
		}

		@Override
		public ReportInstanceStatusResponse reportInstanceStatus(
				ReportInstanceStatus reportInstanceStatus) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.reportInstanceStatus(reportInstanceStatus);
		}

		@Override
		public DeleteInternetGatewayResponse deleteInternetGateway(
				DeleteInternetGateway deleteInternetGateway) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.deleteInternetGateway(deleteInternetGateway);
		}

		@Override
		public AttachNetworkInterfaceResponse attachNetworkInterface(
				AttachNetworkInterface attachNetworkInterface) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.attachNetworkInterface(attachNetworkInterface);
		}

		@Override
		public CreateNetworkInterfaceResponse createNetworkInterface(
				CreateNetworkInterface createNetworkInterface) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.createNetworkInterface(createNetworkInterface);
		}

		@Override
		public RevokeSecurityGroupEgressResponse revokeSecurityGroupEgress(
				RevokeSecurityGroupEgress revokeSecurityGroupEgress) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.revokeSecurityGroupEgress(revokeSecurityGroupEgress);
		}

		@Override
		public ReplaceRouteResponse replaceRoute(ReplaceRoute replaceRoute) {
			AmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);
			return ec2Service.replaceRoute(replaceRoute);
	
		}
}