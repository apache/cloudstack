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
 * AmazonEC2CallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:00:16 CEST)
 */

package com.amazon.ec2.client;

/**
 *  AmazonEC2CallbackHandler Callback class, Users can extend this class and implement
 *  their own receiveResult and receiveError methods.
 */
public abstract class AmazonEC2CallbackHandler {

    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public AmazonEC2CallbackHandler(Object clientData) {
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public AmazonEC2CallbackHandler() {
        clientData = null;
    }

    /**
     * Get the client data
     */

    public Object getClientData() {
        return clientData;
    }

    /**
     * auto generated Axis2 call back method for describePlacementGroups method
     * override this method for handling normal response from describePlacementGroups operation
     */
    public void receiveResultdescribePlacementGroups(com.amazon.ec2.client.AmazonEC2Stub.DescribePlacementGroupsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describePlacementGroups operation
     */
    public void receiveErrordescribePlacementGroups(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createSecurityGroup method
     * override this method for handling normal response from createSecurityGroup operation
     */
    public void receiveResultcreateSecurityGroup(com.amazon.ec2.client.AmazonEC2Stub.CreateSecurityGroupResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createSecurityGroup operation
     */
    public void receiveErrorcreateSecurityGroup(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for resetNetworkInterfaceAttribute method
     * override this method for handling normal response from resetNetworkInterfaceAttribute operation
     */
    public void receiveResultresetNetworkInterfaceAttribute(com.amazon.ec2.client.AmazonEC2Stub.ResetNetworkInterfaceAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from resetNetworkInterfaceAttribute operation
     */
    public void receiveErrorresetNetworkInterfaceAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createDhcpOptions method
     * override this method for handling normal response from createDhcpOptions operation
     */
    public void receiveResultcreateDhcpOptions(com.amazon.ec2.client.AmazonEC2Stub.CreateDhcpOptionsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createDhcpOptions operation
     */
    public void receiveErrorcreateDhcpOptions(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createRouteTable method
     * override this method for handling normal response from createRouteTable operation
     */
    public void receiveResultcreateRouteTable(com.amazon.ec2.client.AmazonEC2Stub.CreateRouteTableResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createRouteTable operation
     */
    public void receiveErrorcreateRouteTable(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeSubnets method
     * override this method for handling normal response from describeSubnets operation
     */
    public void receiveResultdescribeSubnets(com.amazon.ec2.client.AmazonEC2Stub.DescribeSubnetsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeSubnets operation
     */
    public void receiveErrordescribeSubnets(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deactivateLicense method
     * override this method for handling normal response from deactivateLicense operation
     */
    public void receiveResultdeactivateLicense(com.amazon.ec2.client.AmazonEC2Stub.DeactivateLicenseResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deactivateLicense operation
     */
    public void receiveErrordeactivateLicense(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteVpc method
     * override this method for handling normal response from deleteVpc operation
     */
    public void receiveResultdeleteVpc(com.amazon.ec2.client.AmazonEC2Stub.DeleteVpcResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteVpc operation
     */
    public void receiveErrordeleteVpc(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for cancelSpotInstanceRequests method
     * override this method for handling normal response from cancelSpotInstanceRequests operation
     */
    public void receiveResultcancelSpotInstanceRequests(com.amazon.ec2.client.AmazonEC2Stub.CancelSpotInstanceRequestsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from cancelSpotInstanceRequests operation
     */
    public void receiveErrorcancelSpotInstanceRequests(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createSubnet method
     * override this method for handling normal response from createSubnet operation
     */
    public void receiveResultcreateSubnet(com.amazon.ec2.client.AmazonEC2Stub.CreateSubnetResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createSubnet operation
     */
    public void receiveErrorcreateSubnet(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteVpnGateway method
     * override this method for handling normal response from deleteVpnGateway operation
     */
    public void receiveResultdeleteVpnGateway(com.amazon.ec2.client.AmazonEC2Stub.DeleteVpnGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteVpnGateway operation
     */
    public void receiveErrordeleteVpnGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createNetworkAclEntry method
     * override this method for handling normal response from createNetworkAclEntry operation
     */
    public void receiveResultcreateNetworkAclEntry(com.amazon.ec2.client.AmazonEC2Stub.CreateNetworkAclEntryResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createNetworkAclEntry operation
     */
    public void receiveErrorcreateNetworkAclEntry(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for requestSpotInstances method
     * override this method for handling normal response from requestSpotInstances operation
     */
    public void receiveResultrequestSpotInstances(com.amazon.ec2.client.AmazonEC2Stub.RequestSpotInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from requestSpotInstances operation
     */
    public void receiveErrorrequestSpotInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeVolumeAttribute method
     * override this method for handling normal response from describeVolumeAttribute operation
     */
    public void receiveResultdescribeVolumeAttribute(com.amazon.ec2.client.AmazonEC2Stub.DescribeVolumeAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeVolumeAttribute operation
     */
    public void receiveErrordescribeVolumeAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for associateDhcpOptions method
     * override this method for handling normal response from associateDhcpOptions operation
     */
    public void receiveResultassociateDhcpOptions(com.amazon.ec2.client.AmazonEC2Stub.AssociateDhcpOptionsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from associateDhcpOptions operation
     */
    public void receiveErrorassociateDhcpOptions(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeTags method
     * override this method for handling normal response from describeTags operation
     */
    public void receiveResultdescribeTags(com.amazon.ec2.client.AmazonEC2Stub.DescribeTagsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeTags operation
     */
    public void receiveErrordescribeTags(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for importKeyPair method
     * override this method for handling normal response from importKeyPair operation
     */
    public void receiveResultimportKeyPair(com.amazon.ec2.client.AmazonEC2Stub.ImportKeyPairResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from importKeyPair operation
     */
    public void receiveErrorimportKeyPair(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteNetworkInterface method
     * override this method for handling normal response from deleteNetworkInterface operation
     */
    public void receiveResultdeleteNetworkInterface(com.amazon.ec2.client.AmazonEC2Stub.DeleteNetworkInterfaceResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteNetworkInterface operation
     */
    public void receiveErrordeleteNetworkInterface(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeVpcs method
     * override this method for handling normal response from describeVpcs operation
     */
    public void receiveResultdescribeVpcs(com.amazon.ec2.client.AmazonEC2Stub.DescribeVpcsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeVpcs operation
     */
    public void receiveErrordescribeVpcs(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeLicenses method
     * override this method for handling normal response from describeLicenses operation
     */
    public void receiveResultdescribeLicenses(com.amazon.ec2.client.AmazonEC2Stub.DescribeLicensesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeLicenses operation
     */
    public void receiveErrordescribeLicenses(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for bundleInstance method
     * override this method for handling normal response from bundleInstance operation
     */
    public void receiveResultbundleInstance(com.amazon.ec2.client.AmazonEC2Stub.BundleInstanceResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from bundleInstance operation
     */
    public void receiveErrorbundleInstance(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeVpnConnections method
     * override this method for handling normal response from describeVpnConnections operation
     */
    public void receiveResultdescribeVpnConnections(com.amazon.ec2.client.AmazonEC2Stub.DescribeVpnConnectionsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeVpnConnections operation
     */
    public void receiveErrordescribeVpnConnections(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeImages method
     * override this method for handling normal response from describeImages operation
     */
    public void receiveResultdescribeImages(com.amazon.ec2.client.AmazonEC2Stub.DescribeImagesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeImages operation
     */
    public void receiveErrordescribeImages(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createInternetGateway method
     * override this method for handling normal response from createInternetGateway operation
     */
    public void receiveResultcreateInternetGateway(com.amazon.ec2.client.AmazonEC2Stub.CreateInternetGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createInternetGateway operation
     */
    public void receiveErrorcreateInternetGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for disassociateRouteTable method
     * override this method for handling normal response from disassociateRouteTable operation
     */
    public void receiveResultdisassociateRouteTable(com.amazon.ec2.client.AmazonEC2Stub.DisassociateRouteTableResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from disassociateRouteTable operation
     */
    public void receiveErrordisassociateRouteTable(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for replaceNetworkAclEntry method
     * override this method for handling normal response from replaceNetworkAclEntry operation
     */
    public void receiveResultreplaceNetworkAclEntry(com.amazon.ec2.client.AmazonEC2Stub.ReplaceNetworkAclEntryResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from replaceNetworkAclEntry operation
     */
    public void receiveErrorreplaceNetworkAclEntry(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for authorizeSecurityGroupIngress method
     * override this method for handling normal response from authorizeSecurityGroupIngress operation
     */
    public void receiveResultauthorizeSecurityGroupIngress(com.amazon.ec2.client.AmazonEC2Stub.AuthorizeSecurityGroupIngressResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from authorizeSecurityGroupIngress operation
     */
    public void receiveErrorauthorizeSecurityGroupIngress(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeSnapshotAttribute method
     * override this method for handling normal response from describeSnapshotAttribute operation
     */
    public void receiveResultdescribeSnapshotAttribute(com.amazon.ec2.client.AmazonEC2Stub.DescribeSnapshotAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeSnapshotAttribute operation
     */
    public void receiveErrordescribeSnapshotAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createVpnGateway method
     * override this method for handling normal response from createVpnGateway operation
     */
    public void receiveResultcreateVpnGateway(com.amazon.ec2.client.AmazonEC2Stub.CreateVpnGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createVpnGateway operation
     */
    public void receiveErrorcreateVpnGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for resetInstanceAttribute method
     * override this method for handling normal response from resetInstanceAttribute operation
     */
    public void receiveResultresetInstanceAttribute(com.amazon.ec2.client.AmazonEC2Stub.ResetInstanceAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from resetInstanceAttribute operation
     */
    public void receiveErrorresetInstanceAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createTags method
     * override this method for handling normal response from createTags operation
     */
    public void receiveResultcreateTags(com.amazon.ec2.client.AmazonEC2Stub.CreateTagsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createTags operation
     */
    public void receiveErrorcreateTags(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for authorizeSecurityGroupEgress method
     * override this method for handling normal response from authorizeSecurityGroupEgress operation
     */
    public void receiveResultauthorizeSecurityGroupEgress(com.amazon.ec2.client.AmazonEC2Stub.AuthorizeSecurityGroupEgressResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from authorizeSecurityGroupEgress operation
     */
    public void receiveErrorauthorizeSecurityGroupEgress(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for associateAddress method
     * override this method for handling normal response from associateAddress operation
     */
    public void receiveResultassociateAddress(com.amazon.ec2.client.AmazonEC2Stub.AssociateAddressResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from associateAddress operation
     */
    public void receiveErrorassociateAddress(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeImageAttribute method
     * override this method for handling normal response from describeImageAttribute operation
     */
    public void receiveResultdescribeImageAttribute(com.amazon.ec2.client.AmazonEC2Stub.DescribeImageAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeImageAttribute operation
     */
    public void receiveErrordescribeImageAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeSpotPriceHistory method
     * override this method for handling normal response from describeSpotPriceHistory operation
     */
    public void receiveResultdescribeSpotPriceHistory(com.amazon.ec2.client.AmazonEC2Stub.DescribeSpotPriceHistoryResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeSpotPriceHistory operation
     */
    public void receiveErrordescribeSpotPriceHistory(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for modifySnapshotAttribute method
     * override this method for handling normal response from modifySnapshotAttribute operation
     */
    public void receiveResultmodifySnapshotAttribute(com.amazon.ec2.client.AmazonEC2Stub.ModifySnapshotAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from modifySnapshotAttribute operation
     */
    public void receiveErrormodifySnapshotAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeSpotInstanceRequests method
     * override this method for handling normal response from describeSpotInstanceRequests operation
     */
    public void receiveResultdescribeSpotInstanceRequests(com.amazon.ec2.client.AmazonEC2Stub.DescribeSpotInstanceRequestsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeSpotInstanceRequests operation
     */
    public void receiveErrordescribeSpotInstanceRequests(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for importInstance method
     * override this method for handling normal response from importInstance operation
     */
    public void receiveResultimportInstance(com.amazon.ec2.client.AmazonEC2Stub.ImportInstanceResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from importInstance operation
     */
    public void receiveErrorimportInstance(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeKeyPairs method
     * override this method for handling normal response from describeKeyPairs operation
     */
    public void receiveResultdescribeKeyPairs(com.amazon.ec2.client.AmazonEC2Stub.DescribeKeyPairsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeKeyPairs operation
     */
    public void receiveErrordescribeKeyPairs(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for revokeSecurityGroupIngress method
     * override this method for handling normal response from revokeSecurityGroupIngress operation
     */
    public void receiveResultrevokeSecurityGroupIngress(com.amazon.ec2.client.AmazonEC2Stub.RevokeSecurityGroupIngressResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from revokeSecurityGroupIngress operation
     */
    public void receiveErrorrevokeSecurityGroupIngress(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createPlacementGroup method
     * override this method for handling normal response from createPlacementGroup operation
     */
    public void receiveResultcreatePlacementGroup(com.amazon.ec2.client.AmazonEC2Stub.CreatePlacementGroupResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createPlacementGroup operation
     */
    public void receiveErrorcreatePlacementGroup(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteNetworkAclEntry method
     * override this method for handling normal response from deleteNetworkAclEntry operation
     */
    public void receiveResultdeleteNetworkAclEntry(com.amazon.ec2.client.AmazonEC2Stub.DeleteNetworkAclEntryResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteNetworkAclEntry operation
     */
    public void receiveErrordeleteNetworkAclEntry(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for activateLicense method
     * override this method for handling normal response from activateLicense operation
     */
    public void receiveResultactivateLicense(com.amazon.ec2.client.AmazonEC2Stub.ActivateLicenseResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from activateLicense operation
     */
    public void receiveErroractivateLicense(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteRouteTable method
     * override this method for handling normal response from deleteRouteTable operation
     */
    public void receiveResultdeleteRouteTable(com.amazon.ec2.client.AmazonEC2Stub.DeleteRouteTableResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteRouteTable operation
     */
    public void receiveErrordeleteRouteTable(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for unmonitorInstances method
     * override this method for handling normal response from unmonitorInstances operation
     */
    public void receiveResultunmonitorInstances(com.amazon.ec2.client.AmazonEC2Stub.UnmonitorInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from unmonitorInstances operation
     */
    public void receiveErrorunmonitorInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for startInstances method
     * override this method for handling normal response from startInstances operation
     */
    public void receiveResultstartInstances(com.amazon.ec2.client.AmazonEC2Stub.StartInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from startInstances operation
     */
    public void receiveErrorstartInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for confirmProductInstance method
     * override this method for handling normal response from confirmProductInstance operation
     */
    public void receiveResultconfirmProductInstance(com.amazon.ec2.client.AmazonEC2Stub.ConfirmProductInstanceResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from confirmProductInstance operation
     */
    public void receiveErrorconfirmProductInstance(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeNetworkInterfaceAttribute method
     * override this method for handling normal response from describeNetworkInterfaceAttribute operation
     */
    public void receiveResultdescribeNetworkInterfaceAttribute(com.amazon.ec2.client.AmazonEC2Stub.DescribeNetworkInterfaceAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeNetworkInterfaceAttribute operation
     */
    public void receiveErrordescribeNetworkInterfaceAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for runInstances method
     * override this method for handling normal response from runInstances operation
     */
    public void receiveResultrunInstances(com.amazon.ec2.client.AmazonEC2Stub.RunInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from runInstances operation
     */
    public void receiveErrorrunInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createReservedInstancesListing method
     * override this method for handling normal response from createReservedInstancesListing operation
     */
    public void receiveResultcreateReservedInstancesListing(com.amazon.ec2.client.AmazonEC2Stub.CreateReservedInstancesListingResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createReservedInstancesListing operation
     */
    public void receiveErrorcreateReservedInstancesListing(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createCustomerGateway method
     * override this method for handling normal response from createCustomerGateway operation
     */
    public void receiveResultcreateCustomerGateway(com.amazon.ec2.client.AmazonEC2Stub.CreateCustomerGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createCustomerGateway operation
     */
    public void receiveErrorcreateCustomerGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createNetworkAcl method
     * override this method for handling normal response from createNetworkAcl operation
     */
    public void receiveResultcreateNetworkAcl(com.amazon.ec2.client.AmazonEC2Stub.CreateNetworkAclResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createNetworkAcl operation
     */
    public void receiveErrorcreateNetworkAcl(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for resetImageAttribute method
     * override this method for handling normal response from resetImageAttribute operation
     */
    public void receiveResultresetImageAttribute(com.amazon.ec2.client.AmazonEC2Stub.ResetImageAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from resetImageAttribute operation
     */
    public void receiveErrorresetImageAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for modifyVolumeAttribute method
     * override this method for handling normal response from modifyVolumeAttribute operation
     */
    public void receiveResultmodifyVolumeAttribute(com.amazon.ec2.client.AmazonEC2Stub.ModifyVolumeAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from modifyVolumeAttribute operation
     */
    public void receiveErrormodifyVolumeAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeReservedInstances method
     * override this method for handling normal response from describeReservedInstances operation
     */
    public void receiveResultdescribeReservedInstances(com.amazon.ec2.client.AmazonEC2Stub.DescribeReservedInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeReservedInstances operation
     */
    public void receiveErrordescribeReservedInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for resetSnapshotAttribute method
     * override this method for handling normal response from resetSnapshotAttribute operation
     */
    public void receiveResultresetSnapshotAttribute(com.amazon.ec2.client.AmazonEC2Stub.ResetSnapshotAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from resetSnapshotAttribute operation
     */
    public void receiveErrorresetSnapshotAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteVolume method
     * override this method for handling normal response from deleteVolume operation
     */
    public void receiveResultdeleteVolume(com.amazon.ec2.client.AmazonEC2Stub.DeleteVolumeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteVolume operation
     */
    public void receiveErrordeleteVolume(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeAvailabilityZones method
     * override this method for handling normal response from describeAvailabilityZones operation
     */
    public void receiveResultdescribeAvailabilityZones(com.amazon.ec2.client.AmazonEC2Stub.DescribeAvailabilityZonesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeAvailabilityZones operation
     */
    public void receiveErrordescribeAvailabilityZones(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createVpnConnection method
     * override this method for handling normal response from createVpnConnection operation
     */
    public void receiveResultcreateVpnConnection(com.amazon.ec2.client.AmazonEC2Stub.CreateVpnConnectionResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createVpnConnection operation
     */
    public void receiveErrorcreateVpnConnection(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for cancelBundleTask method
     * override this method for handling normal response from cancelBundleTask operation
     */
    public void receiveResultcancelBundleTask(com.amazon.ec2.client.AmazonEC2Stub.CancelBundleTaskResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from cancelBundleTask operation
     */
    public void receiveErrorcancelBundleTask(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for replaceNetworkAclAssociation method
     * override this method for handling normal response from replaceNetworkAclAssociation operation
     */
    public void receiveResultreplaceNetworkAclAssociation(com.amazon.ec2.client.AmazonEC2Stub.ReplaceNetworkAclAssociationResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from replaceNetworkAclAssociation operation
     */
    public void receiveErrorreplaceNetworkAclAssociation(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for detachVpnGateway method
     * override this method for handling normal response from detachVpnGateway operation
     */
    public void receiveResultdetachVpnGateway(com.amazon.ec2.client.AmazonEC2Stub.DetachVpnGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from detachVpnGateway operation
     */
    public void receiveErrordetachVpnGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeSnapshots method
     * override this method for handling normal response from describeSnapshots operation
     */
    public void receiveResultdescribeSnapshots(com.amazon.ec2.client.AmazonEC2Stub.DescribeSnapshotsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeSnapshots operation
     */
    public void receiveErrordescribeSnapshots(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteSubnet method
     * override this method for handling normal response from deleteSubnet operation
     */
    public void receiveResultdeleteSubnet(com.amazon.ec2.client.AmazonEC2Stub.DeleteSubnetResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteSubnet operation
     */
    public void receiveErrordeleteSubnet(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeBundleTasks method
     * override this method for handling normal response from describeBundleTasks operation
     */
    public void receiveResultdescribeBundleTasks(com.amazon.ec2.client.AmazonEC2Stub.DescribeBundleTasksResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeBundleTasks operation
     */
    public void receiveErrordescribeBundleTasks(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createKeyPair method
     * override this method for handling normal response from createKeyPair operation
     */
    public void receiveResultcreateKeyPair(com.amazon.ec2.client.AmazonEC2Stub.CreateKeyPairResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createKeyPair operation
     */
    public void receiveErrorcreateKeyPair(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createImage method
     * override this method for handling normal response from createImage operation
     */
    public void receiveResultcreateImage(com.amazon.ec2.client.AmazonEC2Stub.CreateImageResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createImage operation
     */
    public void receiveErrorcreateImage(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for enableVgwRoutePropagation method
     * override this method for handling normal response from enableVgwRoutePropagation operation
     */
    public void receiveResultenableVgwRoutePropagation(com.amazon.ec2.client.AmazonEC2Stub.EnableVgwRoutePropagationResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from enableVgwRoutePropagation operation
     */
    public void receiveErrorenableVgwRoutePropagation(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for unassignPrivateIpAddresses method
     * override this method for handling normal response from unassignPrivateIpAddresses operation
     */
    public void receiveResultunassignPrivateIpAddresses(com.amazon.ec2.client.AmazonEC2Stub.UnassignPrivateIpAddressesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from unassignPrivateIpAddresses operation
     */
    public void receiveErrorunassignPrivateIpAddresses(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deregisterImage method
     * override this method for handling normal response from deregisterImage operation
     */
    public void receiveResultderegisterImage(com.amazon.ec2.client.AmazonEC2Stub.DeregisterImageResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deregisterImage operation
     */
    public void receiveErrorderegisterImage(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteVpnConnectionRoute method
     * override this method for handling normal response from deleteVpnConnectionRoute operation
     */
    public void receiveResultdeleteVpnConnectionRoute(com.amazon.ec2.client.AmazonEC2Stub.DeleteVpnConnectionRouteResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteVpnConnectionRoute operation
     */
    public void receiveErrordeleteVpnConnectionRoute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for modifyImageAttribute method
     * override this method for handling normal response from modifyImageAttribute operation
     */
    public void receiveResultmodifyImageAttribute(com.amazon.ec2.client.AmazonEC2Stub.ModifyImageAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from modifyImageAttribute operation
     */
    public void receiveErrormodifyImageAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for cancelConversionTask method
     * override this method for handling normal response from cancelConversionTask operation
     */
    public void receiveResultcancelConversionTask(com.amazon.ec2.client.AmazonEC2Stub.CancelConversionTaskResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from cancelConversionTask operation
     */
    public void receiveErrorcancelConversionTask(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeVolumes method
     * override this method for handling normal response from describeVolumes operation
     */
    public void receiveResultdescribeVolumes(com.amazon.ec2.client.AmazonEC2Stub.DescribeVolumesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeVolumes operation
     */
    public void receiveErrordescribeVolumes(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for cancelReservedInstancesListing method
     * override this method for handling normal response from cancelReservedInstancesListing operation
     */
    public void receiveResultcancelReservedInstancesListing(com.amazon.ec2.client.AmazonEC2Stub.CancelReservedInstancesListingResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from cancelReservedInstancesListing operation
     */
    public void receiveErrorcancelReservedInstancesListing(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for getPasswordData method
     * override this method for handling normal response from getPasswordData operation
     */
    public void receiveResultgetPasswordData(com.amazon.ec2.client.AmazonEC2Stub.GetPasswordDataResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from getPasswordData operation
     */
    public void receiveErrorgetPasswordData(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for allocateAddress method
     * override this method for handling normal response from allocateAddress operation
     */
    public void receiveResultallocateAddress(com.amazon.ec2.client.AmazonEC2Stub.AllocateAddressResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from allocateAddress operation
     */
    public void receiveErrorallocateAddress(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteSecurityGroup method
     * override this method for handling normal response from deleteSecurityGroup operation
     */
    public void receiveResultdeleteSecurityGroup(com.amazon.ec2.client.AmazonEC2Stub.DeleteSecurityGroupResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteSecurityGroup operation
     */
    public void receiveErrordeleteSecurityGroup(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deletePlacementGroup method
     * override this method for handling normal response from deletePlacementGroup operation
     */
    public void receiveResultdeletePlacementGroup(com.amazon.ec2.client.AmazonEC2Stub.DeletePlacementGroupResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deletePlacementGroup operation
     */
    public void receiveErrordeletePlacementGroup(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for disassociateAddress method
     * override this method for handling normal response from disassociateAddress operation
     */
    public void receiveResultdisassociateAddress(com.amazon.ec2.client.AmazonEC2Stub.DisassociateAddressResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from disassociateAddress operation
     */
    public void receiveErrordisassociateAddress(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteDhcpOptions method
     * override this method for handling normal response from deleteDhcpOptions operation
     */
    public void receiveResultdeleteDhcpOptions(com.amazon.ec2.client.AmazonEC2Stub.DeleteDhcpOptionsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteDhcpOptions operation
     */
    public void receiveErrordeleteDhcpOptions(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeSpotDatafeedSubscription method
     * override this method for handling normal response from describeSpotDatafeedSubscription operation
     */
    public void receiveResultdescribeSpotDatafeedSubscription(com.amazon.ec2.client.AmazonEC2Stub.DescribeSpotDatafeedSubscriptionResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeSpotDatafeedSubscription operation
     */
    public void receiveErrordescribeSpotDatafeedSubscription(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeNetworkAcls method
     * override this method for handling normal response from describeNetworkAcls operation
     */
    public void receiveResultdescribeNetworkAcls(com.amazon.ec2.client.AmazonEC2Stub.DescribeNetworkAclsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeNetworkAcls operation
     */
    public void receiveErrordescribeNetworkAcls(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for enableVolumeIO method
     * override this method for handling normal response from enableVolumeIO operation
     */
    public void receiveResultenableVolumeIO(com.amazon.ec2.client.AmazonEC2Stub.EnableVolumeIOResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from enableVolumeIO operation
     */
    public void receiveErrorenableVolumeIO(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for attachVpnGateway method
     * override this method for handling normal response from attachVpnGateway operation
     */
    public void receiveResultattachVpnGateway(com.amazon.ec2.client.AmazonEC2Stub.AttachVpnGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from attachVpnGateway operation
     */
    public void receiveErrorattachVpnGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeInternetGateways method
     * override this method for handling normal response from describeInternetGateways operation
     */
    public void receiveResultdescribeInternetGateways(com.amazon.ec2.client.AmazonEC2Stub.DescribeInternetGatewaysResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeInternetGateways operation
     */
    public void receiveErrordescribeInternetGateways(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeDhcpOptions method
     * override this method for handling normal response from describeDhcpOptions operation
     */
    public void receiveResultdescribeDhcpOptions(com.amazon.ec2.client.AmazonEC2Stub.DescribeDhcpOptionsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeDhcpOptions operation
     */
    public void receiveErrordescribeDhcpOptions(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createSpotDatafeedSubscription method
     * override this method for handling normal response from createSpotDatafeedSubscription operation
     */
    public void receiveResultcreateSpotDatafeedSubscription(com.amazon.ec2.client.AmazonEC2Stub.CreateSpotDatafeedSubscriptionResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createSpotDatafeedSubscription operation
     */
    public void receiveErrorcreateSpotDatafeedSubscription(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeReservedInstancesListings method
     * override this method for handling normal response from describeReservedInstancesListings operation
     */
    public void receiveResultdescribeReservedInstancesListings(com.amazon.ec2.client.AmazonEC2Stub.DescribeReservedInstancesListingsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeReservedInstancesListings operation
     */
    public void receiveErrordescribeReservedInstancesListings(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeInstanceStatus method
     * override this method for handling normal response from describeInstanceStatus operation
     */
    public void receiveResultdescribeInstanceStatus(com.amazon.ec2.client.AmazonEC2Stub.DescribeInstanceStatusResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeInstanceStatus operation
     */
    public void receiveErrordescribeInstanceStatus(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for modifyNetworkInterfaceAttribute method
     * override this method for handling normal response from modifyNetworkInterfaceAttribute operation
     */
    public void receiveResultmodifyNetworkInterfaceAttribute(com.amazon.ec2.client.AmazonEC2Stub.ModifyNetworkInterfaceAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from modifyNetworkInterfaceAttribute operation
     */
    public void receiveErrormodifyNetworkInterfaceAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for disableVgwRoutePropagation method
     * override this method for handling normal response from disableVgwRoutePropagation operation
     */
    public void receiveResultdisableVgwRoutePropagation(com.amazon.ec2.client.AmazonEC2Stub.DisableVgwRoutePropagationResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from disableVgwRoutePropagation operation
     */
    public void receiveErrordisableVgwRoutePropagation(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeVolumeStatus method
     * override this method for handling normal response from describeVolumeStatus operation
     */
    public void receiveResultdescribeVolumeStatus(com.amazon.ec2.client.AmazonEC2Stub.DescribeVolumeStatusResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeVolumeStatus operation
     */
    public void receiveErrordescribeVolumeStatus(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for detachNetworkInterface method
     * override this method for handling normal response from detachNetworkInterface operation
     */
    public void receiveResultdetachNetworkInterface(com.amazon.ec2.client.AmazonEC2Stub.DetachNetworkInterfaceResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from detachNetworkInterface operation
     */
    public void receiveErrordetachNetworkInterface(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeSecurityGroups method
     * override this method for handling normal response from describeSecurityGroups operation
     */
    public void receiveResultdescribeSecurityGroups(com.amazon.ec2.client.AmazonEC2Stub.DescribeSecurityGroupsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeSecurityGroups operation
     */
    public void receiveErrordescribeSecurityGroups(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeInstances method
     * override this method for handling normal response from describeInstances operation
     */
    public void receiveResultdescribeInstances(com.amazon.ec2.client.AmazonEC2Stub.DescribeInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeInstances operation
     */
    public void receiveErrordescribeInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeNetworkInterfaces method
     * override this method for handling normal response from describeNetworkInterfaces operation
     */
    public void receiveResultdescribeNetworkInterfaces(com.amazon.ec2.client.AmazonEC2Stub.DescribeNetworkInterfacesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeNetworkInterfaces operation
     */
    public void receiveErrordescribeNetworkInterfaces(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteKeyPair method
     * override this method for handling normal response from deleteKeyPair operation
     */
    public void receiveResultdeleteKeyPair(com.amazon.ec2.client.AmazonEC2Stub.DeleteKeyPairResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteKeyPair operation
     */
    public void receiveErrordeleteKeyPair(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createSnapshot method
     * override this method for handling normal response from createSnapshot operation
     */
    public void receiveResultcreateSnapshot(com.amazon.ec2.client.AmazonEC2Stub.CreateSnapshotResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createSnapshot operation
     */
    public void receiveErrorcreateSnapshot(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeVpnGateways method
     * override this method for handling normal response from describeVpnGateways operation
     */
    public void receiveResultdescribeVpnGateways(com.amazon.ec2.client.AmazonEC2Stub.DescribeVpnGatewaysResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeVpnGateways operation
     */
    public void receiveErrordescribeVpnGateways(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteTags method
     * override this method for handling normal response from deleteTags operation
     */
    public void receiveResultdeleteTags(com.amazon.ec2.client.AmazonEC2Stub.DeleteTagsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteTags operation
     */
    public void receiveErrordeleteTags(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteSnapshot method
     * override this method for handling normal response from deleteSnapshot operation
     */
    public void receiveResultdeleteSnapshot(com.amazon.ec2.client.AmazonEC2Stub.DeleteSnapshotResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteSnapshot operation
     */
    public void receiveErrordeleteSnapshot(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteCustomerGateway method
     * override this method for handling normal response from deleteCustomerGateway operation
     */
    public void receiveResultdeleteCustomerGateway(com.amazon.ec2.client.AmazonEC2Stub.DeleteCustomerGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteCustomerGateway operation
     */
    public void receiveErrordeleteCustomerGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createVolume method
     * override this method for handling normal response from createVolume operation
     */
    public void receiveResultcreateVolume(com.amazon.ec2.client.AmazonEC2Stub.CreateVolumeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createVolume operation
     */
    public void receiveErrorcreateVolume(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for cancelExportTask method
     * override this method for handling normal response from cancelExportTask operation
     */
    public void receiveResultcancelExportTask(com.amazon.ec2.client.AmazonEC2Stub.CancelExportTaskResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from cancelExportTask operation
     */
    public void receiveErrorcancelExportTask(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for registerImage method
     * override this method for handling normal response from registerImage operation
     */
    public void receiveResultregisterImage(com.amazon.ec2.client.AmazonEC2Stub.RegisterImageResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from registerImage operation
     */
    public void receiveErrorregisterImage(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for detachVolume method
     * override this method for handling normal response from detachVolume operation
     */
    public void receiveResultdetachVolume(com.amazon.ec2.client.AmazonEC2Stub.DetachVolumeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from detachVolume operation
     */
    public void receiveErrordetachVolume(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for stopInstances method
     * override this method for handling normal response from stopInstances operation
     */
    public void receiveResultstopInstances(com.amazon.ec2.client.AmazonEC2Stub.StopInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from stopInstances operation
     */
    public void receiveErrorstopInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createRoute method
     * override this method for handling normal response from createRoute operation
     */
    public void receiveResultcreateRoute(com.amazon.ec2.client.AmazonEC2Stub.CreateRouteResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createRoute operation
     */
    public void receiveErrorcreateRoute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for releaseAddress method
     * override this method for handling normal response from releaseAddress operation
     */
    public void receiveResultreleaseAddress(com.amazon.ec2.client.AmazonEC2Stub.ReleaseAddressResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from releaseAddress operation
     */
    public void receiveErrorreleaseAddress(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeRouteTables method
     * override this method for handling normal response from describeRouteTables operation
     */
    public void receiveResultdescribeRouteTables(com.amazon.ec2.client.AmazonEC2Stub.DescribeRouteTablesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeRouteTables operation
     */
    public void receiveErrordescribeRouteTables(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeCustomerGateways method
     * override this method for handling normal response from describeCustomerGateways operation
     */
    public void receiveResultdescribeCustomerGateways(com.amazon.ec2.client.AmazonEC2Stub.DescribeCustomerGatewaysResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeCustomerGateways operation
     */
    public void receiveErrordescribeCustomerGateways(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteNetworkAcl method
     * override this method for handling normal response from deleteNetworkAcl operation
     */
    public void receiveResultdeleteNetworkAcl(com.amazon.ec2.client.AmazonEC2Stub.DeleteNetworkAclResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteNetworkAcl operation
     */
    public void receiveErrordeleteNetworkAcl(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteRoute method
     * override this method for handling normal response from deleteRoute operation
     */
    public void receiveResultdeleteRoute(com.amazon.ec2.client.AmazonEC2Stub.DeleteRouteResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteRoute operation
     */
    public void receiveErrordeleteRoute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for rebootInstances method
     * override this method for handling normal response from rebootInstances operation
     */
    public void receiveResultrebootInstances(com.amazon.ec2.client.AmazonEC2Stub.RebootInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from rebootInstances operation
     */
    public void receiveErrorrebootInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for modifyInstanceAttribute method
     * override this method for handling normal response from modifyInstanceAttribute operation
     */
    public void receiveResultmodifyInstanceAttribute(com.amazon.ec2.client.AmazonEC2Stub.ModifyInstanceAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from modifyInstanceAttribute operation
     */
    public void receiveErrormodifyInstanceAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for terminateInstances method
     * override this method for handling normal response from terminateInstances operation
     */
    public void receiveResultterminateInstances(com.amazon.ec2.client.AmazonEC2Stub.TerminateInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from terminateInstances operation
     */
    public void receiveErrorterminateInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createVpnConnectionRoute method
     * override this method for handling normal response from createVpnConnectionRoute operation
     */
    public void receiveResultcreateVpnConnectionRoute(com.amazon.ec2.client.AmazonEC2Stub.CreateVpnConnectionRouteResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createVpnConnectionRoute operation
     */
    public void receiveErrorcreateVpnConnectionRoute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeConversionTasks method
     * override this method for handling normal response from describeConversionTasks operation
     */
    public void receiveResultdescribeConversionTasks(com.amazon.ec2.client.AmazonEC2Stub.DescribeConversionTasksResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeConversionTasks operation
     */
    public void receiveErrordescribeConversionTasks(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeAddresses method
     * override this method for handling normal response from describeAddresses operation
     */
    public void receiveResultdescribeAddresses(com.amazon.ec2.client.AmazonEC2Stub.DescribeAddressesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeAddresses operation
     */
    public void receiveErrordescribeAddresses(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeInstanceAttribute method
     * override this method for handling normal response from describeInstanceAttribute operation
     */
    public void receiveResultdescribeInstanceAttribute(com.amazon.ec2.client.AmazonEC2Stub.DescribeInstanceAttributeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeInstanceAttribute operation
     */
    public void receiveErrordescribeInstanceAttribute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for attachInternetGateway method
     * override this method for handling normal response from attachInternetGateway operation
     */
    public void receiveResultattachInternetGateway(com.amazon.ec2.client.AmazonEC2Stub.AttachInternetGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from attachInternetGateway operation
     */
    public void receiveErrorattachInternetGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createVpc method
     * override this method for handling normal response from createVpc operation
     */
    public void receiveResultcreateVpc(com.amazon.ec2.client.AmazonEC2Stub.CreateVpcResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createVpc operation
     */
    public void receiveErrorcreateVpc(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for replaceRouteTableAssociation method
     * override this method for handling normal response from replaceRouteTableAssociation operation
     */
    public void receiveResultreplaceRouteTableAssociation(com.amazon.ec2.client.AmazonEC2Stub.ReplaceRouteTableAssociationResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from replaceRouteTableAssociation operation
     */
    public void receiveErrorreplaceRouteTableAssociation(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for associateRouteTable method
     * override this method for handling normal response from associateRouteTable operation
     */
    public void receiveResultassociateRouteTable(com.amazon.ec2.client.AmazonEC2Stub.AssociateRouteTableResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from associateRouteTable operation
     */
    public void receiveErrorassociateRouteTable(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for detachInternetGateway method
     * override this method for handling normal response from detachInternetGateway operation
     */
    public void receiveResultdetachInternetGateway(com.amazon.ec2.client.AmazonEC2Stub.DetachInternetGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from detachInternetGateway operation
     */
    public void receiveErrordetachInternetGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for purchaseReservedInstancesOffering method
     * override this method for handling normal response from purchaseReservedInstancesOffering operation
     */
    public void receiveResultpurchaseReservedInstancesOffering(com.amazon.ec2.client.AmazonEC2Stub.PurchaseReservedInstancesOfferingResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from purchaseReservedInstancesOffering operation
     */
    public void receiveErrorpurchaseReservedInstancesOffering(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for importVolume method
     * override this method for handling normal response from importVolume operation
     */
    public void receiveResultimportVolume(com.amazon.ec2.client.AmazonEC2Stub.ImportVolumeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from importVolume operation
     */
    public void receiveErrorimportVolume(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeExportTasks method
     * override this method for handling normal response from describeExportTasks operation
     */
    public void receiveResultdescribeExportTasks(com.amazon.ec2.client.AmazonEC2Stub.DescribeExportTasksResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeExportTasks operation
     */
    public void receiveErrordescribeExportTasks(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createInstanceExportTask method
     * override this method for handling normal response from createInstanceExportTask operation
     */
    public void receiveResultcreateInstanceExportTask(com.amazon.ec2.client.AmazonEC2Stub.CreateInstanceExportTaskResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createInstanceExportTask operation
     */
    public void receiveErrorcreateInstanceExportTask(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for assignPrivateIpAddresses method
     * override this method for handling normal response from assignPrivateIpAddresses operation
     */
    public void receiveResultassignPrivateIpAddresses(com.amazon.ec2.client.AmazonEC2Stub.AssignPrivateIpAddressesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from assignPrivateIpAddresses operation
     */
    public void receiveErrorassignPrivateIpAddresses(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for reportInstanceStatus method
     * override this method for handling normal response from reportInstanceStatus operation
     */
    public void receiveResultreportInstanceStatus(com.amazon.ec2.client.AmazonEC2Stub.ReportInstanceStatusResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from reportInstanceStatus operation
     */
    public void receiveErrorreportInstanceStatus(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeReservedInstancesOfferings method
     * override this method for handling normal response from describeReservedInstancesOfferings operation
     */
    public void receiveResultdescribeReservedInstancesOfferings(com.amazon.ec2.client.AmazonEC2Stub.DescribeReservedInstancesOfferingsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeReservedInstancesOfferings operation
     */
    public void receiveErrordescribeReservedInstancesOfferings(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteVpnConnection method
     * override this method for handling normal response from deleteVpnConnection operation
     */
    public void receiveResultdeleteVpnConnection(com.amazon.ec2.client.AmazonEC2Stub.DeleteVpnConnectionResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteVpnConnection operation
     */
    public void receiveErrordeleteVpnConnection(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteInternetGateway method
     * override this method for handling normal response from deleteInternetGateway operation
     */
    public void receiveResultdeleteInternetGateway(com.amazon.ec2.client.AmazonEC2Stub.DeleteInternetGatewayResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteInternetGateway operation
     */
    public void receiveErrordeleteInternetGateway(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteSpotDatafeedSubscription method
     * override this method for handling normal response from deleteSpotDatafeedSubscription operation
     */
    public void receiveResultdeleteSpotDatafeedSubscription(com.amazon.ec2.client.AmazonEC2Stub.DeleteSpotDatafeedSubscriptionResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteSpotDatafeedSubscription operation
     */
    public void receiveErrordeleteSpotDatafeedSubscription(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for attachNetworkInterface method
     * override this method for handling normal response from attachNetworkInterface operation
     */
    public void receiveResultattachNetworkInterface(com.amazon.ec2.client.AmazonEC2Stub.AttachNetworkInterfaceResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from attachNetworkInterface operation
     */
    public void receiveErrorattachNetworkInterface(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createNetworkInterface method
     * override this method for handling normal response from createNetworkInterface operation
     */
    public void receiveResultcreateNetworkInterface(com.amazon.ec2.client.AmazonEC2Stub.CreateNetworkInterfaceResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createNetworkInterface operation
     */
    public void receiveErrorcreateNetworkInterface(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for revokeSecurityGroupEgress method
     * override this method for handling normal response from revokeSecurityGroupEgress operation
     */
    public void receiveResultrevokeSecurityGroupEgress(com.amazon.ec2.client.AmazonEC2Stub.RevokeSecurityGroupEgressResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from revokeSecurityGroupEgress operation
     */
    public void receiveErrorrevokeSecurityGroupEgress(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for monitorInstances method
     * override this method for handling normal response from monitorInstances operation
     */
    public void receiveResultmonitorInstances(com.amazon.ec2.client.AmazonEC2Stub.MonitorInstancesResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from monitorInstances operation
     */
    public void receiveErrormonitorInstances(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for replaceRoute method
     * override this method for handling normal response from replaceRoute operation
     */
    public void receiveResultreplaceRoute(com.amazon.ec2.client.AmazonEC2Stub.ReplaceRouteResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from replaceRoute operation
     */
    public void receiveErrorreplaceRoute(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for attachVolume method
     * override this method for handling normal response from attachVolume operation
     */
    public void receiveResultattachVolume(com.amazon.ec2.client.AmazonEC2Stub.AttachVolumeResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from attachVolume operation
     */
    public void receiveErrorattachVolume(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for getConsoleOutput method
     * override this method for handling normal response from getConsoleOutput operation
     */
    public void receiveResultgetConsoleOutput(com.amazon.ec2.client.AmazonEC2Stub.GetConsoleOutputResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from getConsoleOutput operation
     */
    public void receiveErrorgetConsoleOutput(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for describeRegions method
     * override this method for handling normal response from describeRegions operation
     */
    public void receiveResultdescribeRegions(com.amazon.ec2.client.AmazonEC2Stub.DescribeRegionsResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from describeRegions operation
     */
    public void receiveErrordescribeRegions(java.lang.Exception e) {
    }

}
