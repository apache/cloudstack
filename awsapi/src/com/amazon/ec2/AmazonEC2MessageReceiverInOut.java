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
 * AmazonEC2MessageReceiverInOut.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.6  Built on : Aug 30, 2011 (10:00:16 CEST)
 */
package com.amazon.ec2;

/**
*  AmazonEC2MessageReceiverInOut message receiver
*/

public class AmazonEC2MessageReceiverInOut extends org.apache.axis2.receivers.AbstractInOutMessageReceiver {

    public void invokeBusinessLogic(org.apache.axis2.context.MessageContext msgContext, org.apache.axis2.context.MessageContext newMsgContext)
        throws org.apache.axis2.AxisFault {

        try {

            // get the implementation class for the Web Service
            Object obj = getTheImplementationObject(msgContext);

            AmazonEC2SkeletonInterface skel = (AmazonEC2SkeletonInterface)obj;
            //Out Envelop
            org.apache.axiom.soap.SOAPEnvelope envelope = null;
            //Find the axisOperation that has been set by the Dispatch phase.
            org.apache.axis2.description.AxisOperation op = msgContext.getOperationContext().getAxisOperation();
            if (op == null) {
                throw new org.apache.axis2.AxisFault(
                    "Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
            }

            java.lang.String methodName;
            if ((op.getName() != null) && ((methodName = org.apache.axis2.util.JavaUtils.xmlNameToJavaIdentifier(op.getName().getLocalPart())) != null)) {

                if ("describePlacementGroups".equals(methodName)) {

                    com.amazon.ec2.DescribePlacementGroupsResponse describePlacementGroupsResponse289 = null;
                    com.amazon.ec2.DescribePlacementGroups wrappedParam =
                        (com.amazon.ec2.DescribePlacementGroups)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribePlacementGroups.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describePlacementGroupsResponse289 =

                    skel.describePlacementGroups(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describePlacementGroupsResponse289, false);
                } else

                if ("createSecurityGroup".equals(methodName)) {

                    com.amazon.ec2.CreateSecurityGroupResponse createSecurityGroupResponse291 = null;
                    com.amazon.ec2.CreateSecurityGroup wrappedParam =
                        (com.amazon.ec2.CreateSecurityGroup)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateSecurityGroup.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createSecurityGroupResponse291 =

                    skel.createSecurityGroup(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createSecurityGroupResponse291, false);
                } else

                if ("resetNetworkInterfaceAttribute".equals(methodName)) {

                    com.amazon.ec2.ResetNetworkInterfaceAttributeResponse resetNetworkInterfaceAttributeResponse293 = null;
                    com.amazon.ec2.ResetNetworkInterfaceAttribute wrappedParam =
                        (com.amazon.ec2.ResetNetworkInterfaceAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.ResetNetworkInterfaceAttribute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    resetNetworkInterfaceAttributeResponse293 =

                    skel.resetNetworkInterfaceAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), resetNetworkInterfaceAttributeResponse293, false);
                } else

                if ("createDhcpOptions".equals(methodName)) {

                    com.amazon.ec2.CreateDhcpOptionsResponse createDhcpOptionsResponse295 = null;
                    com.amazon.ec2.CreateDhcpOptions wrappedParam =
                        (com.amazon.ec2.CreateDhcpOptions)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateDhcpOptions.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createDhcpOptionsResponse295 =

                    skel.createDhcpOptions(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createDhcpOptionsResponse295, false);
                } else

                if ("createRouteTable".equals(methodName)) {

                    com.amazon.ec2.CreateRouteTableResponse createRouteTableResponse297 = null;
                    com.amazon.ec2.CreateRouteTable wrappedParam =
                        (com.amazon.ec2.CreateRouteTable)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateRouteTable.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createRouteTableResponse297 =

                    skel.createRouteTable(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createRouteTableResponse297, false);
                } else

                if ("describeSubnets".equals(methodName)) {

                    com.amazon.ec2.DescribeSubnetsResponse describeSubnetsResponse299 = null;
                    com.amazon.ec2.DescribeSubnets wrappedParam =
                        (com.amazon.ec2.DescribeSubnets)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeSubnets.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeSubnetsResponse299 =

                    skel.describeSubnets(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeSubnetsResponse299, false);
                } else

                if ("deactivateLicense".equals(methodName)) {

                    com.amazon.ec2.DeactivateLicenseResponse deactivateLicenseResponse301 = null;
                    com.amazon.ec2.DeactivateLicense wrappedParam =
                        (com.amazon.ec2.DeactivateLicense)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeactivateLicense.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deactivateLicenseResponse301 =

                    skel.deactivateLicense(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deactivateLicenseResponse301, false);
                } else

                if ("deleteVpc".equals(methodName)) {

                    com.amazon.ec2.DeleteVpcResponse deleteVpcResponse303 = null;
                    com.amazon.ec2.DeleteVpc wrappedParam =
                        (com.amazon.ec2.DeleteVpc)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteVpc.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteVpcResponse303 =

                    skel.deleteVpc(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteVpcResponse303, false);
                } else

                if ("cancelSpotInstanceRequests".equals(methodName)) {

                    com.amazon.ec2.CancelSpotInstanceRequestsResponse cancelSpotInstanceRequestsResponse305 = null;
                    com.amazon.ec2.CancelSpotInstanceRequests wrappedParam =
                        (com.amazon.ec2.CancelSpotInstanceRequests)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.CancelSpotInstanceRequests.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    cancelSpotInstanceRequestsResponse305 =

                    skel.cancelSpotInstanceRequests(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), cancelSpotInstanceRequestsResponse305, false);
                } else

                if ("createSubnet".equals(methodName)) {

                    com.amazon.ec2.CreateSubnetResponse createSubnetResponse307 = null;
                    com.amazon.ec2.CreateSubnet wrappedParam =
                        (com.amazon.ec2.CreateSubnet)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateSubnet.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createSubnetResponse307 =

                    skel.createSubnet(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createSubnetResponse307, false);
                } else

                if ("deleteVpnGateway".equals(methodName)) {

                    com.amazon.ec2.DeleteVpnGatewayResponse deleteVpnGatewayResponse309 = null;
                    com.amazon.ec2.DeleteVpnGateway wrappedParam =
                        (com.amazon.ec2.DeleteVpnGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteVpnGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteVpnGatewayResponse309 =

                    skel.deleteVpnGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteVpnGatewayResponse309, false);
                } else

                if ("createNetworkAclEntry".equals(methodName)) {

                    com.amazon.ec2.CreateNetworkAclEntryResponse createNetworkAclEntryResponse311 = null;
                    com.amazon.ec2.CreateNetworkAclEntry wrappedParam =
                        (com.amazon.ec2.CreateNetworkAclEntry)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateNetworkAclEntry.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createNetworkAclEntryResponse311 =

                    skel.createNetworkAclEntry(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createNetworkAclEntryResponse311, false);
                } else

                if ("requestSpotInstances".equals(methodName)) {

                    com.amazon.ec2.RequestSpotInstancesResponse requestSpotInstancesResponse313 = null;
                    com.amazon.ec2.RequestSpotInstances wrappedParam =
                        (com.amazon.ec2.RequestSpotInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.RequestSpotInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    requestSpotInstancesResponse313 =

                    skel.requestSpotInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), requestSpotInstancesResponse313, false);
                } else

                if ("describeVolumeAttribute".equals(methodName)) {

                    com.amazon.ec2.DescribeVolumeAttributeResponse describeVolumeAttributeResponse315 = null;
                    com.amazon.ec2.DescribeVolumeAttribute wrappedParam =
                        (com.amazon.ec2.DescribeVolumeAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeVolumeAttribute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeVolumeAttributeResponse315 =

                    skel.describeVolumeAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeVolumeAttributeResponse315, false);
                } else

                if ("associateDhcpOptions".equals(methodName)) {

                    com.amazon.ec2.AssociateDhcpOptionsResponse associateDhcpOptionsResponse317 = null;
                    com.amazon.ec2.AssociateDhcpOptions wrappedParam =
                        (com.amazon.ec2.AssociateDhcpOptions)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.AssociateDhcpOptions.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    associateDhcpOptionsResponse317 =

                    skel.associateDhcpOptions(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), associateDhcpOptionsResponse317, false);
                } else

                if ("describeTags".equals(methodName)) {

                    com.amazon.ec2.DescribeTagsResponse describeTagsResponse319 = null;
                    com.amazon.ec2.DescribeTags wrappedParam =
                        (com.amazon.ec2.DescribeTags)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeTags.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeTagsResponse319 =

                    skel.describeTags(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeTagsResponse319, false);
                } else

                if ("importKeyPair".equals(methodName)) {

                    com.amazon.ec2.ImportKeyPairResponse importKeyPairResponse321 = null;
                    com.amazon.ec2.ImportKeyPair wrappedParam =
                        (com.amazon.ec2.ImportKeyPair)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ImportKeyPair.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    importKeyPairResponse321 =

                    skel.importKeyPair(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), importKeyPairResponse321, false);
                } else

                if ("deleteNetworkInterface".equals(methodName)) {

                    com.amazon.ec2.DeleteNetworkInterfaceResponse deleteNetworkInterfaceResponse323 = null;
                    com.amazon.ec2.DeleteNetworkInterface wrappedParam =
                        (com.amazon.ec2.DeleteNetworkInterface)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteNetworkInterface.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteNetworkInterfaceResponse323 =

                    skel.deleteNetworkInterface(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteNetworkInterfaceResponse323, false);
                } else

                if ("describeVpcs".equals(methodName)) {

                    com.amazon.ec2.DescribeVpcsResponse describeVpcsResponse325 = null;
                    com.amazon.ec2.DescribeVpcs wrappedParam =
                        (com.amazon.ec2.DescribeVpcs)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeVpcs.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeVpcsResponse325 =

                    skel.describeVpcs(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeVpcsResponse325, false);
                } else

                if ("describeLicenses".equals(methodName)) {

                    com.amazon.ec2.DescribeLicensesResponse describeLicensesResponse327 = null;
                    com.amazon.ec2.DescribeLicenses wrappedParam =
                        (com.amazon.ec2.DescribeLicenses)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeLicenses.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeLicensesResponse327 =

                    skel.describeLicenses(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeLicensesResponse327, false);
                } else

                if ("bundleInstance".equals(methodName)) {

                    com.amazon.ec2.BundleInstanceResponse bundleInstanceResponse329 = null;
                    com.amazon.ec2.BundleInstance wrappedParam =
                        (com.amazon.ec2.BundleInstance)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.BundleInstance.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    bundleInstanceResponse329 =

                    skel.bundleInstance(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), bundleInstanceResponse329, false);
                } else

                if ("describeVpnConnections".equals(methodName)) {

                    com.amazon.ec2.DescribeVpnConnectionsResponse describeVpnConnectionsResponse331 = null;
                    com.amazon.ec2.DescribeVpnConnections wrappedParam =
                        (com.amazon.ec2.DescribeVpnConnections)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeVpnConnections.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeVpnConnectionsResponse331 =

                    skel.describeVpnConnections(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeVpnConnectionsResponse331, false);
                } else

                if ("describeImages".equals(methodName)) {

                    com.amazon.ec2.DescribeImagesResponse describeImagesResponse333 = null;
                    com.amazon.ec2.DescribeImages wrappedParam =
                        (com.amazon.ec2.DescribeImages)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeImages.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeImagesResponse333 =

                    skel.describeImages(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeImagesResponse333, false);
                } else

                if ("createInternetGateway".equals(methodName)) {

                    com.amazon.ec2.CreateInternetGatewayResponse createInternetGatewayResponse335 = null;
                    com.amazon.ec2.CreateInternetGateway wrappedParam =
                        (com.amazon.ec2.CreateInternetGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateInternetGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createInternetGatewayResponse335 =

                    skel.createInternetGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createInternetGatewayResponse335, false);
                } else

                if ("disassociateRouteTable".equals(methodName)) {

                    com.amazon.ec2.DisassociateRouteTableResponse disassociateRouteTableResponse337 = null;
                    com.amazon.ec2.DisassociateRouteTable wrappedParam =
                        (com.amazon.ec2.DisassociateRouteTable)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DisassociateRouteTable.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    disassociateRouteTableResponse337 =

                    skel.disassociateRouteTable(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), disassociateRouteTableResponse337, false);
                } else

                if ("replaceNetworkAclEntry".equals(methodName)) {

                    com.amazon.ec2.ReplaceNetworkAclEntryResponse replaceNetworkAclEntryResponse339 = null;
                    com.amazon.ec2.ReplaceNetworkAclEntry wrappedParam =
                        (com.amazon.ec2.ReplaceNetworkAclEntry)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ReplaceNetworkAclEntry.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    replaceNetworkAclEntryResponse339 =

                    skel.replaceNetworkAclEntry(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), replaceNetworkAclEntryResponse339, false);
                } else

                if ("authorizeSecurityGroupIngress".equals(methodName)) {

                    com.amazon.ec2.AuthorizeSecurityGroupIngressResponse authorizeSecurityGroupIngressResponse341 = null;
                    com.amazon.ec2.AuthorizeSecurityGroupIngress wrappedParam =
                        (com.amazon.ec2.AuthorizeSecurityGroupIngress)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.AuthorizeSecurityGroupIngress.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    authorizeSecurityGroupIngressResponse341 =

                    skel.authorizeSecurityGroupIngress(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), authorizeSecurityGroupIngressResponse341, false);
                } else

                if ("describeSnapshotAttribute".equals(methodName)) {

                    com.amazon.ec2.DescribeSnapshotAttributeResponse describeSnapshotAttributeResponse343 = null;
                    com.amazon.ec2.DescribeSnapshotAttribute wrappedParam =
                        (com.amazon.ec2.DescribeSnapshotAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeSnapshotAttribute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeSnapshotAttributeResponse343 =

                    skel.describeSnapshotAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeSnapshotAttributeResponse343, false);
                } else

                if ("createVpnGateway".equals(methodName)) {

                    com.amazon.ec2.CreateVpnGatewayResponse createVpnGatewayResponse345 = null;
                    com.amazon.ec2.CreateVpnGateway wrappedParam =
                        (com.amazon.ec2.CreateVpnGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateVpnGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createVpnGatewayResponse345 =

                    skel.createVpnGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createVpnGatewayResponse345, false);
                } else

                if ("resetInstanceAttribute".equals(methodName)) {

                    com.amazon.ec2.ResetInstanceAttributeResponse resetInstanceAttributeResponse347 = null;
                    com.amazon.ec2.ResetInstanceAttribute wrappedParam =
                        (com.amazon.ec2.ResetInstanceAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ResetInstanceAttribute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    resetInstanceAttributeResponse347 =

                    skel.resetInstanceAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), resetInstanceAttributeResponse347, false);
                } else

                if ("createTags".equals(methodName)) {

                    com.amazon.ec2.CreateTagsResponse createTagsResponse349 = null;
                    com.amazon.ec2.CreateTags wrappedParam =
                        (com.amazon.ec2.CreateTags)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateTags.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createTagsResponse349 =

                    skel.createTags(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createTagsResponse349, false);
                } else

                if ("authorizeSecurityGroupEgress".equals(methodName)) {

                    com.amazon.ec2.AuthorizeSecurityGroupEgressResponse authorizeSecurityGroupEgressResponse351 = null;
                    com.amazon.ec2.AuthorizeSecurityGroupEgress wrappedParam =
                        (com.amazon.ec2.AuthorizeSecurityGroupEgress)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.AuthorizeSecurityGroupEgress.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    authorizeSecurityGroupEgressResponse351 =

                    skel.authorizeSecurityGroupEgress(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), authorizeSecurityGroupEgressResponse351, false);
                } else

                if ("associateAddress".equals(methodName)) {

                    com.amazon.ec2.AssociateAddressResponse associateAddressResponse353 = null;
                    com.amazon.ec2.AssociateAddress wrappedParam =
                        (com.amazon.ec2.AssociateAddress)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.AssociateAddress.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    associateAddressResponse353 =

                    skel.associateAddress(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), associateAddressResponse353, false);
                } else

                if ("describeImageAttribute".equals(methodName)) {

                    com.amazon.ec2.DescribeImageAttributeResponse describeImageAttributeResponse355 = null;
                    com.amazon.ec2.DescribeImageAttribute wrappedParam =
                        (com.amazon.ec2.DescribeImageAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeImageAttribute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeImageAttributeResponse355 =

                    skel.describeImageAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeImageAttributeResponse355, false);
                } else

                if ("describeSpotPriceHistory".equals(methodName)) {

                    com.amazon.ec2.DescribeSpotPriceHistoryResponse describeSpotPriceHistoryResponse357 = null;
                    com.amazon.ec2.DescribeSpotPriceHistory wrappedParam =
                        (com.amazon.ec2.DescribeSpotPriceHistory)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeSpotPriceHistory.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeSpotPriceHistoryResponse357 =

                    skel.describeSpotPriceHistory(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeSpotPriceHistoryResponse357, false);
                } else

                if ("modifySnapshotAttribute".equals(methodName)) {

                    com.amazon.ec2.ModifySnapshotAttributeResponse modifySnapshotAttributeResponse359 = null;
                    com.amazon.ec2.ModifySnapshotAttribute wrappedParam =
                        (com.amazon.ec2.ModifySnapshotAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.ModifySnapshotAttribute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    modifySnapshotAttributeResponse359 =

                    skel.modifySnapshotAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), modifySnapshotAttributeResponse359, false);
                } else

                if ("describeSpotInstanceRequests".equals(methodName)) {

                    com.amazon.ec2.DescribeSpotInstanceRequestsResponse describeSpotInstanceRequestsResponse361 = null;
                    com.amazon.ec2.DescribeSpotInstanceRequests wrappedParam =
                        (com.amazon.ec2.DescribeSpotInstanceRequests)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeSpotInstanceRequests.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeSpotInstanceRequestsResponse361 =

                    skel.describeSpotInstanceRequests(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeSpotInstanceRequestsResponse361, false);
                } else

                if ("importInstance".equals(methodName)) {

                    com.amazon.ec2.ImportInstanceResponse importInstanceResponse363 = null;
                    com.amazon.ec2.ImportInstance wrappedParam =
                        (com.amazon.ec2.ImportInstance)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ImportInstance.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    importInstanceResponse363 =

                    skel.importInstance(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), importInstanceResponse363, false);
                } else

                if ("describeKeyPairs".equals(methodName)) {

                    com.amazon.ec2.DescribeKeyPairsResponse describeKeyPairsResponse365 = null;
                    com.amazon.ec2.DescribeKeyPairs wrappedParam =
                        (com.amazon.ec2.DescribeKeyPairs)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeKeyPairs.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeKeyPairsResponse365 =

                    skel.describeKeyPairs(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeKeyPairsResponse365, false);
                } else

                if ("revokeSecurityGroupIngress".equals(methodName)) {

                    com.amazon.ec2.RevokeSecurityGroupIngressResponse revokeSecurityGroupIngressResponse367 = null;
                    com.amazon.ec2.RevokeSecurityGroupIngress wrappedParam =
                        (com.amazon.ec2.RevokeSecurityGroupIngress)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.RevokeSecurityGroupIngress.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    revokeSecurityGroupIngressResponse367 =

                    skel.revokeSecurityGroupIngress(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), revokeSecurityGroupIngressResponse367, false);
                } else

                if ("createPlacementGroup".equals(methodName)) {

                    com.amazon.ec2.CreatePlacementGroupResponse createPlacementGroupResponse369 = null;
                    com.amazon.ec2.CreatePlacementGroup wrappedParam =
                        (com.amazon.ec2.CreatePlacementGroup)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreatePlacementGroup.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createPlacementGroupResponse369 =

                    skel.createPlacementGroup(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createPlacementGroupResponse369, false);
                } else

                if ("deleteNetworkAclEntry".equals(methodName)) {

                    com.amazon.ec2.DeleteNetworkAclEntryResponse deleteNetworkAclEntryResponse371 = null;
                    com.amazon.ec2.DeleteNetworkAclEntry wrappedParam =
                        (com.amazon.ec2.DeleteNetworkAclEntry)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteNetworkAclEntry.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteNetworkAclEntryResponse371 =

                    skel.deleteNetworkAclEntry(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteNetworkAclEntryResponse371, false);
                } else

                if ("activateLicense".equals(methodName)) {

                    com.amazon.ec2.ActivateLicenseResponse activateLicenseResponse373 = null;
                    com.amazon.ec2.ActivateLicense wrappedParam =
                        (com.amazon.ec2.ActivateLicense)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ActivateLicense.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    activateLicenseResponse373 =

                    skel.activateLicense(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), activateLicenseResponse373, false);
                } else

                if ("deleteRouteTable".equals(methodName)) {

                    com.amazon.ec2.DeleteRouteTableResponse deleteRouteTableResponse375 = null;
                    com.amazon.ec2.DeleteRouteTable wrappedParam =
                        (com.amazon.ec2.DeleteRouteTable)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteRouteTable.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteRouteTableResponse375 =

                    skel.deleteRouteTable(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteRouteTableResponse375, false);
                } else

                if ("unmonitorInstances".equals(methodName)) {

                    com.amazon.ec2.UnmonitorInstancesResponse unmonitorInstancesResponse377 = null;
                    com.amazon.ec2.UnmonitorInstances wrappedParam =
                        (com.amazon.ec2.UnmonitorInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.UnmonitorInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    unmonitorInstancesResponse377 =

                    skel.unmonitorInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), unmonitorInstancesResponse377, false);
                } else

                if ("startInstances".equals(methodName)) {

                    com.amazon.ec2.StartInstancesResponse startInstancesResponse379 = null;
                    com.amazon.ec2.StartInstances wrappedParam =
                        (com.amazon.ec2.StartInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.StartInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    startInstancesResponse379 =

                    skel.startInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), startInstancesResponse379, false);
                } else

                if ("confirmProductInstance".equals(methodName)) {

                    com.amazon.ec2.ConfirmProductInstanceResponse confirmProductInstanceResponse381 = null;
                    com.amazon.ec2.ConfirmProductInstance wrappedParam =
                        (com.amazon.ec2.ConfirmProductInstance)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ConfirmProductInstance.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    confirmProductInstanceResponse381 =

                    skel.confirmProductInstance(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), confirmProductInstanceResponse381, false);
                } else

                if ("describeNetworkInterfaceAttribute".equals(methodName)) {

                    com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse describeNetworkInterfaceAttributeResponse383 = null;
                    com.amazon.ec2.DescribeNetworkInterfaceAttribute wrappedParam =
                        (com.amazon.ec2.DescribeNetworkInterfaceAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeNetworkInterfaceAttribute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeNetworkInterfaceAttributeResponse383 =

                    skel.describeNetworkInterfaceAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeNetworkInterfaceAttributeResponse383, false);
                } else

                if ("runInstances".equals(methodName)) {

                    com.amazon.ec2.RunInstancesResponse runInstancesResponse385 = null;
                    com.amazon.ec2.RunInstances wrappedParam =
                        (com.amazon.ec2.RunInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.RunInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    runInstancesResponse385 =

                    skel.runInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), runInstancesResponse385, false);
                } else

                if ("createReservedInstancesListing".equals(methodName)) {

                    com.amazon.ec2.CreateReservedInstancesListingResponse createReservedInstancesListingResponse387 = null;
                    com.amazon.ec2.CreateReservedInstancesListing wrappedParam =
                        (com.amazon.ec2.CreateReservedInstancesListing)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.CreateReservedInstancesListing.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createReservedInstancesListingResponse387 =

                    skel.createReservedInstancesListing(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createReservedInstancesListingResponse387, false);
                } else

                if ("createCustomerGateway".equals(methodName)) {

                    com.amazon.ec2.CreateCustomerGatewayResponse createCustomerGatewayResponse389 = null;
                    com.amazon.ec2.CreateCustomerGateway wrappedParam =
                        (com.amazon.ec2.CreateCustomerGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateCustomerGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createCustomerGatewayResponse389 =

                    skel.createCustomerGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createCustomerGatewayResponse389, false);
                } else

                if ("createNetworkAcl".equals(methodName)) {

                    com.amazon.ec2.CreateNetworkAclResponse createNetworkAclResponse391 = null;
                    com.amazon.ec2.CreateNetworkAcl wrappedParam =
                        (com.amazon.ec2.CreateNetworkAcl)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateNetworkAcl.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createNetworkAclResponse391 =

                    skel.createNetworkAcl(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createNetworkAclResponse391, false);
                } else

                if ("resetImageAttribute".equals(methodName)) {

                    com.amazon.ec2.ResetImageAttributeResponse resetImageAttributeResponse393 = null;
                    com.amazon.ec2.ResetImageAttribute wrappedParam =
                        (com.amazon.ec2.ResetImageAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ResetImageAttribute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    resetImageAttributeResponse393 =

                    skel.resetImageAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), resetImageAttributeResponse393, false);
                } else

                if ("modifyVolumeAttribute".equals(methodName)) {

                    com.amazon.ec2.ModifyVolumeAttributeResponse modifyVolumeAttributeResponse395 = null;
                    com.amazon.ec2.ModifyVolumeAttribute wrappedParam =
                        (com.amazon.ec2.ModifyVolumeAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ModifyVolumeAttribute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    modifyVolumeAttributeResponse395 =

                    skel.modifyVolumeAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), modifyVolumeAttributeResponse395, false);
                } else

                if ("describeReservedInstances".equals(methodName)) {

                    com.amazon.ec2.DescribeReservedInstancesResponse describeReservedInstancesResponse397 = null;
                    com.amazon.ec2.DescribeReservedInstances wrappedParam =
                        (com.amazon.ec2.DescribeReservedInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeReservedInstances.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeReservedInstancesResponse397 =

                    skel.describeReservedInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeReservedInstancesResponse397, false);
                } else

                if ("resetSnapshotAttribute".equals(methodName)) {

                    com.amazon.ec2.ResetSnapshotAttributeResponse resetSnapshotAttributeResponse399 = null;
                    com.amazon.ec2.ResetSnapshotAttribute wrappedParam =
                        (com.amazon.ec2.ResetSnapshotAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ResetSnapshotAttribute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    resetSnapshotAttributeResponse399 =

                    skel.resetSnapshotAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), resetSnapshotAttributeResponse399, false);
                } else

                if ("deleteVolume".equals(methodName)) {

                    com.amazon.ec2.DeleteVolumeResponse deleteVolumeResponse401 = null;
                    com.amazon.ec2.DeleteVolume wrappedParam =
                        (com.amazon.ec2.DeleteVolume)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteVolume.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteVolumeResponse401 =

                    skel.deleteVolume(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteVolumeResponse401, false);
                } else

                if ("describeAvailabilityZones".equals(methodName)) {

                    com.amazon.ec2.DescribeAvailabilityZonesResponse describeAvailabilityZonesResponse403 = null;
                    com.amazon.ec2.DescribeAvailabilityZones wrappedParam =
                        (com.amazon.ec2.DescribeAvailabilityZones)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeAvailabilityZones.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeAvailabilityZonesResponse403 =

                    skel.describeAvailabilityZones(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeAvailabilityZonesResponse403, false);
                } else

                if ("createVpnConnection".equals(methodName)) {

                    com.amazon.ec2.CreateVpnConnectionResponse createVpnConnectionResponse405 = null;
                    com.amazon.ec2.CreateVpnConnection wrappedParam =
                        (com.amazon.ec2.CreateVpnConnection)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateVpnConnection.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createVpnConnectionResponse405 =

                    skel.createVpnConnection(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createVpnConnectionResponse405, false);
                } else

                if ("cancelBundleTask".equals(methodName)) {

                    com.amazon.ec2.CancelBundleTaskResponse cancelBundleTaskResponse407 = null;
                    com.amazon.ec2.CancelBundleTask wrappedParam =
                        (com.amazon.ec2.CancelBundleTask)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CancelBundleTask.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    cancelBundleTaskResponse407 =

                    skel.cancelBundleTask(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), cancelBundleTaskResponse407, false);
                } else

                if ("replaceNetworkAclAssociation".equals(methodName)) {

                    com.amazon.ec2.ReplaceNetworkAclAssociationResponse replaceNetworkAclAssociationResponse409 = null;
                    com.amazon.ec2.ReplaceNetworkAclAssociation wrappedParam =
                        (com.amazon.ec2.ReplaceNetworkAclAssociation)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.ReplaceNetworkAclAssociation.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    replaceNetworkAclAssociationResponse409 =

                    skel.replaceNetworkAclAssociation(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), replaceNetworkAclAssociationResponse409, false);
                } else

                if ("detachVpnGateway".equals(methodName)) {

                    com.amazon.ec2.DetachVpnGatewayResponse detachVpnGatewayResponse411 = null;
                    com.amazon.ec2.DetachVpnGateway wrappedParam =
                        (com.amazon.ec2.DetachVpnGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DetachVpnGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    detachVpnGatewayResponse411 =

                    skel.detachVpnGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), detachVpnGatewayResponse411, false);
                } else

                if ("describeSnapshots".equals(methodName)) {

                    com.amazon.ec2.DescribeSnapshotsResponse describeSnapshotsResponse413 = null;
                    com.amazon.ec2.DescribeSnapshots wrappedParam =
                        (com.amazon.ec2.DescribeSnapshots)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeSnapshots.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeSnapshotsResponse413 =

                    skel.describeSnapshots(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeSnapshotsResponse413, false);
                } else

                if ("deleteSubnet".equals(methodName)) {

                    com.amazon.ec2.DeleteSubnetResponse deleteSubnetResponse415 = null;
                    com.amazon.ec2.DeleteSubnet wrappedParam =
                        (com.amazon.ec2.DeleteSubnet)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteSubnet.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteSubnetResponse415 =

                    skel.deleteSubnet(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteSubnetResponse415, false);
                } else

                if ("describeBundleTasks".equals(methodName)) {

                    com.amazon.ec2.DescribeBundleTasksResponse describeBundleTasksResponse417 = null;
                    com.amazon.ec2.DescribeBundleTasks wrappedParam =
                        (com.amazon.ec2.DescribeBundleTasks)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeBundleTasks.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeBundleTasksResponse417 =

                    skel.describeBundleTasks(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeBundleTasksResponse417, false);
                } else

                if ("createKeyPair".equals(methodName)) {

                    com.amazon.ec2.CreateKeyPairResponse createKeyPairResponse419 = null;
                    com.amazon.ec2.CreateKeyPair wrappedParam =
                        (com.amazon.ec2.CreateKeyPair)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateKeyPair.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createKeyPairResponse419 =

                    skel.createKeyPair(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createKeyPairResponse419, false);
                } else

                if ("createImage".equals(methodName)) {

                    com.amazon.ec2.CreateImageResponse createImageResponse421 = null;
                    com.amazon.ec2.CreateImage wrappedParam =
                        (com.amazon.ec2.CreateImage)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateImage.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createImageResponse421 =

                    skel.createImage(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createImageResponse421, false);
                } else

                if ("enableVgwRoutePropagation".equals(methodName)) {

                    com.amazon.ec2.EnableVgwRoutePropagationResponse enableVgwRoutePropagationResponse423 = null;
                    com.amazon.ec2.EnableVgwRoutePropagation wrappedParam =
                        (com.amazon.ec2.EnableVgwRoutePropagation)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.EnableVgwRoutePropagation.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    enableVgwRoutePropagationResponse423 =

                    skel.enableVgwRoutePropagation(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), enableVgwRoutePropagationResponse423, false);
                } else

                if ("unassignPrivateIpAddresses".equals(methodName)) {

                    com.amazon.ec2.UnassignPrivateIpAddressesResponse unassignPrivateIpAddressesResponse425 = null;
                    com.amazon.ec2.UnassignPrivateIpAddresses wrappedParam =
                        (com.amazon.ec2.UnassignPrivateIpAddresses)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.UnassignPrivateIpAddresses.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    unassignPrivateIpAddressesResponse425 =

                    skel.unassignPrivateIpAddresses(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), unassignPrivateIpAddressesResponse425, false);
                } else

                if ("deregisterImage".equals(methodName)) {

                    com.amazon.ec2.DeregisterImageResponse deregisterImageResponse427 = null;
                    com.amazon.ec2.DeregisterImage wrappedParam =
                        (com.amazon.ec2.DeregisterImage)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeregisterImage.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deregisterImageResponse427 =

                    skel.deregisterImage(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deregisterImageResponse427, false);
                } else

                if ("deleteVpnConnectionRoute".equals(methodName)) {

                    com.amazon.ec2.DeleteVpnConnectionRouteResponse deleteVpnConnectionRouteResponse429 = null;
                    com.amazon.ec2.DeleteVpnConnectionRoute wrappedParam =
                        (com.amazon.ec2.DeleteVpnConnectionRoute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DeleteVpnConnectionRoute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteVpnConnectionRouteResponse429 =

                    skel.deleteVpnConnectionRoute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteVpnConnectionRouteResponse429, false);
                } else

                if ("modifyImageAttribute".equals(methodName)) {

                    com.amazon.ec2.ModifyImageAttributeResponse modifyImageAttributeResponse431 = null;
                    com.amazon.ec2.ModifyImageAttribute wrappedParam =
                        (com.amazon.ec2.ModifyImageAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ModifyImageAttribute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    modifyImageAttributeResponse431 =

                    skel.modifyImageAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), modifyImageAttributeResponse431, false);
                } else

                if ("cancelConversionTask".equals(methodName)) {

                    com.amazon.ec2.CancelConversionTaskResponse cancelConversionTaskResponse433 = null;
                    com.amazon.ec2.CancelConversionTask wrappedParam =
                        (com.amazon.ec2.CancelConversionTask)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CancelConversionTask.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    cancelConversionTaskResponse433 =

                    skel.cancelConversionTask(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), cancelConversionTaskResponse433, false);
                } else

                if ("describeVolumes".equals(methodName)) {

                    com.amazon.ec2.DescribeVolumesResponse describeVolumesResponse435 = null;
                    com.amazon.ec2.DescribeVolumes wrappedParam =
                        (com.amazon.ec2.DescribeVolumes)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeVolumes.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeVolumesResponse435 =

                    skel.describeVolumes(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeVolumesResponse435, false);
                } else

                if ("cancelReservedInstancesListing".equals(methodName)) {

                    com.amazon.ec2.CancelReservedInstancesListingResponse cancelReservedInstancesListingResponse437 = null;
                    com.amazon.ec2.CancelReservedInstancesListing wrappedParam =
                        (com.amazon.ec2.CancelReservedInstancesListing)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.CancelReservedInstancesListing.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    cancelReservedInstancesListingResponse437 =

                    skel.cancelReservedInstancesListing(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), cancelReservedInstancesListingResponse437, false);
                } else

                if ("getPasswordData".equals(methodName)) {

                    com.amazon.ec2.GetPasswordDataResponse getPasswordDataResponse439 = null;
                    com.amazon.ec2.GetPasswordData wrappedParam =
                        (com.amazon.ec2.GetPasswordData)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.GetPasswordData.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    getPasswordDataResponse439 =

                    skel.getPasswordData(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), getPasswordDataResponse439, false);
                } else

                if ("allocateAddress".equals(methodName)) {

                    com.amazon.ec2.AllocateAddressResponse allocateAddressResponse441 = null;
                    com.amazon.ec2.AllocateAddress wrappedParam =
                        (com.amazon.ec2.AllocateAddress)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.AllocateAddress.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    allocateAddressResponse441 =

                    skel.allocateAddress(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), allocateAddressResponse441, false);
                } else

                if ("deleteSecurityGroup".equals(methodName)) {

                    com.amazon.ec2.DeleteSecurityGroupResponse deleteSecurityGroupResponse443 = null;
                    com.amazon.ec2.DeleteSecurityGroup wrappedParam =
                        (com.amazon.ec2.DeleteSecurityGroup)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteSecurityGroup.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteSecurityGroupResponse443 =

                    skel.deleteSecurityGroup(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteSecurityGroupResponse443, false);
                } else

                if ("deletePlacementGroup".equals(methodName)) {

                    com.amazon.ec2.DeletePlacementGroupResponse deletePlacementGroupResponse445 = null;
                    com.amazon.ec2.DeletePlacementGroup wrappedParam =
                        (com.amazon.ec2.DeletePlacementGroup)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeletePlacementGroup.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deletePlacementGroupResponse445 =

                    skel.deletePlacementGroup(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deletePlacementGroupResponse445, false);
                } else

                if ("disassociateAddress".equals(methodName)) {

                    com.amazon.ec2.DisassociateAddressResponse disassociateAddressResponse447 = null;
                    com.amazon.ec2.DisassociateAddress wrappedParam =
                        (com.amazon.ec2.DisassociateAddress)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DisassociateAddress.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    disassociateAddressResponse447 =

                    skel.disassociateAddress(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), disassociateAddressResponse447, false);
                } else

                if ("deleteDhcpOptions".equals(methodName)) {

                    com.amazon.ec2.DeleteDhcpOptionsResponse deleteDhcpOptionsResponse449 = null;
                    com.amazon.ec2.DeleteDhcpOptions wrappedParam =
                        (com.amazon.ec2.DeleteDhcpOptions)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteDhcpOptions.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteDhcpOptionsResponse449 =

                    skel.deleteDhcpOptions(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteDhcpOptionsResponse449, false);
                } else

                if ("describeSpotDatafeedSubscription".equals(methodName)) {

                    com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse describeSpotDatafeedSubscriptionResponse451 = null;
                    com.amazon.ec2.DescribeSpotDatafeedSubscription wrappedParam =
                        (com.amazon.ec2.DescribeSpotDatafeedSubscription)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeSpotDatafeedSubscription.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeSpotDatafeedSubscriptionResponse451 =

                    skel.describeSpotDatafeedSubscription(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeSpotDatafeedSubscriptionResponse451, false);
                } else

                if ("describeNetworkAcls".equals(methodName)) {

                    com.amazon.ec2.DescribeNetworkAclsResponse describeNetworkAclsResponse453 = null;
                    com.amazon.ec2.DescribeNetworkAcls wrappedParam =
                        (com.amazon.ec2.DescribeNetworkAcls)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeNetworkAcls.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeNetworkAclsResponse453 =

                    skel.describeNetworkAcls(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeNetworkAclsResponse453, false);
                } else

                if ("enableVolumeIO".equals(methodName)) {

                    com.amazon.ec2.EnableVolumeIOResponse enableVolumeIOResponse455 = null;
                    com.amazon.ec2.EnableVolumeIO wrappedParam =
                        (com.amazon.ec2.EnableVolumeIO)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.EnableVolumeIO.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    enableVolumeIOResponse455 =

                    skel.enableVolumeIO(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), enableVolumeIOResponse455, false);
                } else

                if ("attachVpnGateway".equals(methodName)) {

                    com.amazon.ec2.AttachVpnGatewayResponse attachVpnGatewayResponse457 = null;
                    com.amazon.ec2.AttachVpnGateway wrappedParam =
                        (com.amazon.ec2.AttachVpnGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.AttachVpnGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    attachVpnGatewayResponse457 =

                    skel.attachVpnGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), attachVpnGatewayResponse457, false);
                } else

                if ("describeInternetGateways".equals(methodName)) {

                    com.amazon.ec2.DescribeInternetGatewaysResponse describeInternetGatewaysResponse459 = null;
                    com.amazon.ec2.DescribeInternetGateways wrappedParam =
                        (com.amazon.ec2.DescribeInternetGateways)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeInternetGateways.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeInternetGatewaysResponse459 =

                    skel.describeInternetGateways(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeInternetGatewaysResponse459, false);
                } else

                if ("describeDhcpOptions".equals(methodName)) {

                    com.amazon.ec2.DescribeDhcpOptionsResponse describeDhcpOptionsResponse461 = null;
                    com.amazon.ec2.DescribeDhcpOptions wrappedParam =
                        (com.amazon.ec2.DescribeDhcpOptions)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeDhcpOptions.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeDhcpOptionsResponse461 =

                    skel.describeDhcpOptions(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeDhcpOptionsResponse461, false);
                } else

                if ("createSpotDatafeedSubscription".equals(methodName)) {

                    com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse createSpotDatafeedSubscriptionResponse463 = null;
                    com.amazon.ec2.CreateSpotDatafeedSubscription wrappedParam =
                        (com.amazon.ec2.CreateSpotDatafeedSubscription)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.CreateSpotDatafeedSubscription.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createSpotDatafeedSubscriptionResponse463 =

                    skel.createSpotDatafeedSubscription(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createSpotDatafeedSubscriptionResponse463, false);
                } else

                if ("describeReservedInstancesListings".equals(methodName)) {

                    com.amazon.ec2.DescribeReservedInstancesListingsResponse describeReservedInstancesListingsResponse465 = null;
                    com.amazon.ec2.DescribeReservedInstancesListings wrappedParam =
                        (com.amazon.ec2.DescribeReservedInstancesListings)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeReservedInstancesListings.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeReservedInstancesListingsResponse465 =

                    skel.describeReservedInstancesListings(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeReservedInstancesListingsResponse465, false);
                } else

                if ("describeInstanceStatus".equals(methodName)) {

                    com.amazon.ec2.DescribeInstanceStatusResponse describeInstanceStatusResponse467 = null;
                    com.amazon.ec2.DescribeInstanceStatus wrappedParam =
                        (com.amazon.ec2.DescribeInstanceStatus)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeInstanceStatus.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeInstanceStatusResponse467 =

                    skel.describeInstanceStatus(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeInstanceStatusResponse467, false);
                } else

                if ("modifyNetworkInterfaceAttribute".equals(methodName)) {

                    com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse modifyNetworkInterfaceAttributeResponse469 = null;
                    com.amazon.ec2.ModifyNetworkInterfaceAttribute wrappedParam =
                        (com.amazon.ec2.ModifyNetworkInterfaceAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.ModifyNetworkInterfaceAttribute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    modifyNetworkInterfaceAttributeResponse469 =

                    skel.modifyNetworkInterfaceAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), modifyNetworkInterfaceAttributeResponse469, false);
                } else

                if ("disableVgwRoutePropagation".equals(methodName)) {

                    com.amazon.ec2.DisableVgwRoutePropagationResponse disableVgwRoutePropagationResponse471 = null;
                    com.amazon.ec2.DisableVgwRoutePropagation wrappedParam =
                        (com.amazon.ec2.DisableVgwRoutePropagation)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DisableVgwRoutePropagation.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    disableVgwRoutePropagationResponse471 =

                    skel.disableVgwRoutePropagation(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), disableVgwRoutePropagationResponse471, false);
                } else

                if ("describeVolumeStatus".equals(methodName)) {

                    com.amazon.ec2.DescribeVolumeStatusResponse describeVolumeStatusResponse473 = null;
                    com.amazon.ec2.DescribeVolumeStatus wrappedParam =
                        (com.amazon.ec2.DescribeVolumeStatus)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeVolumeStatus.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeVolumeStatusResponse473 =

                    skel.describeVolumeStatus(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeVolumeStatusResponse473, false);
                } else

                if ("detachNetworkInterface".equals(methodName)) {

                    com.amazon.ec2.DetachNetworkInterfaceResponse detachNetworkInterfaceResponse475 = null;
                    com.amazon.ec2.DetachNetworkInterface wrappedParam =
                        (com.amazon.ec2.DetachNetworkInterface)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DetachNetworkInterface.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    detachNetworkInterfaceResponse475 =

                    skel.detachNetworkInterface(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), detachNetworkInterfaceResponse475, false);
                } else

                if ("describeSecurityGroups".equals(methodName)) {

                    com.amazon.ec2.DescribeSecurityGroupsResponse describeSecurityGroupsResponse477 = null;
                    com.amazon.ec2.DescribeSecurityGroups wrappedParam =
                        (com.amazon.ec2.DescribeSecurityGroups)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeSecurityGroups.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeSecurityGroupsResponse477 =

                    skel.describeSecurityGroups(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeSecurityGroupsResponse477, false);
                } else

                if ("describeInstances".equals(methodName)) {

                    com.amazon.ec2.DescribeInstancesResponse describeInstancesResponse479 = null;
                    com.amazon.ec2.DescribeInstances wrappedParam =
                        (com.amazon.ec2.DescribeInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeInstancesResponse479 =

                    skel.describeInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeInstancesResponse479, false);
                } else

                if ("describeNetworkInterfaces".equals(methodName)) {

                    com.amazon.ec2.DescribeNetworkInterfacesResponse describeNetworkInterfacesResponse481 = null;
                    com.amazon.ec2.DescribeNetworkInterfaces wrappedParam =
                        (com.amazon.ec2.DescribeNetworkInterfaces)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeNetworkInterfaces.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeNetworkInterfacesResponse481 =

                    skel.describeNetworkInterfaces(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeNetworkInterfacesResponse481, false);
                } else

                if ("deleteKeyPair".equals(methodName)) {

                    com.amazon.ec2.DeleteKeyPairResponse deleteKeyPairResponse483 = null;
                    com.amazon.ec2.DeleteKeyPair wrappedParam =
                        (com.amazon.ec2.DeleteKeyPair)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteKeyPair.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteKeyPairResponse483 =

                    skel.deleteKeyPair(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteKeyPairResponse483, false);
                } else

                if ("createSnapshot".equals(methodName)) {

                    com.amazon.ec2.CreateSnapshotResponse createSnapshotResponse485 = null;
                    com.amazon.ec2.CreateSnapshot wrappedParam =
                        (com.amazon.ec2.CreateSnapshot)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateSnapshot.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createSnapshotResponse485 =

                    skel.createSnapshot(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createSnapshotResponse485, false);
                } else

                if ("describeVpnGateways".equals(methodName)) {

                    com.amazon.ec2.DescribeVpnGatewaysResponse describeVpnGatewaysResponse487 = null;
                    com.amazon.ec2.DescribeVpnGateways wrappedParam =
                        (com.amazon.ec2.DescribeVpnGateways)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeVpnGateways.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeVpnGatewaysResponse487 =

                    skel.describeVpnGateways(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeVpnGatewaysResponse487, false);
                } else

                if ("deleteTags".equals(methodName)) {

                    com.amazon.ec2.DeleteTagsResponse deleteTagsResponse489 = null;
                    com.amazon.ec2.DeleteTags wrappedParam =
                        (com.amazon.ec2.DeleteTags)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteTags.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteTagsResponse489 =

                    skel.deleteTags(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteTagsResponse489, false);
                } else

                if ("deleteSnapshot".equals(methodName)) {

                    com.amazon.ec2.DeleteSnapshotResponse deleteSnapshotResponse491 = null;
                    com.amazon.ec2.DeleteSnapshot wrappedParam =
                        (com.amazon.ec2.DeleteSnapshot)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteSnapshot.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteSnapshotResponse491 =

                    skel.deleteSnapshot(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteSnapshotResponse491, false);
                } else

                if ("deleteCustomerGateway".equals(methodName)) {

                    com.amazon.ec2.DeleteCustomerGatewayResponse deleteCustomerGatewayResponse493 = null;
                    com.amazon.ec2.DeleteCustomerGateway wrappedParam =
                        (com.amazon.ec2.DeleteCustomerGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteCustomerGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteCustomerGatewayResponse493 =

                    skel.deleteCustomerGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteCustomerGatewayResponse493, false);
                } else

                if ("createVolume".equals(methodName)) {

                    com.amazon.ec2.CreateVolumeResponse createVolumeResponse495 = null;
                    com.amazon.ec2.CreateVolume wrappedParam =
                        (com.amazon.ec2.CreateVolume)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateVolume.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createVolumeResponse495 =

                    skel.createVolume(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createVolumeResponse495, false);
                } else

                if ("cancelExportTask".equals(methodName)) {

                    com.amazon.ec2.CancelExportTaskResponse cancelExportTaskResponse497 = null;
                    com.amazon.ec2.CancelExportTask wrappedParam =
                        (com.amazon.ec2.CancelExportTask)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CancelExportTask.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    cancelExportTaskResponse497 =

                    skel.cancelExportTask(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), cancelExportTaskResponse497, false);
                } else

                if ("registerImage".equals(methodName)) {

                    com.amazon.ec2.RegisterImageResponse registerImageResponse499 = null;
                    com.amazon.ec2.RegisterImage wrappedParam =
                        (com.amazon.ec2.RegisterImage)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.RegisterImage.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    registerImageResponse499 =

                    skel.registerImage(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), registerImageResponse499, false);
                } else

                if ("detachVolume".equals(methodName)) {

                    com.amazon.ec2.DetachVolumeResponse detachVolumeResponse501 = null;
                    com.amazon.ec2.DetachVolume wrappedParam =
                        (com.amazon.ec2.DetachVolume)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DetachVolume.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    detachVolumeResponse501 =

                    skel.detachVolume(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), detachVolumeResponse501, false);
                } else

                if ("stopInstances".equals(methodName)) {

                    com.amazon.ec2.StopInstancesResponse stopInstancesResponse503 = null;
                    com.amazon.ec2.StopInstances wrappedParam =
                        (com.amazon.ec2.StopInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.StopInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    stopInstancesResponse503 =

                    skel.stopInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), stopInstancesResponse503, false);
                } else

                if ("createRoute".equals(methodName)) {

                    com.amazon.ec2.CreateRouteResponse createRouteResponse505 = null;
                    com.amazon.ec2.CreateRoute wrappedParam =
                        (com.amazon.ec2.CreateRoute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateRoute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createRouteResponse505 =

                    skel.createRoute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createRouteResponse505, false);
                } else

                if ("releaseAddress".equals(methodName)) {

                    com.amazon.ec2.ReleaseAddressResponse releaseAddressResponse507 = null;
                    com.amazon.ec2.ReleaseAddress wrappedParam =
                        (com.amazon.ec2.ReleaseAddress)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ReleaseAddress.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    releaseAddressResponse507 =

                    skel.releaseAddress(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), releaseAddressResponse507, false);
                } else

                if ("describeRouteTables".equals(methodName)) {

                    com.amazon.ec2.DescribeRouteTablesResponse describeRouteTablesResponse509 = null;
                    com.amazon.ec2.DescribeRouteTables wrappedParam =
                        (com.amazon.ec2.DescribeRouteTables)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeRouteTables.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeRouteTablesResponse509 =

                    skel.describeRouteTables(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeRouteTablesResponse509, false);
                } else

                if ("describeCustomerGateways".equals(methodName)) {

                    com.amazon.ec2.DescribeCustomerGatewaysResponse describeCustomerGatewaysResponse511 = null;
                    com.amazon.ec2.DescribeCustomerGateways wrappedParam =
                        (com.amazon.ec2.DescribeCustomerGateways)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeCustomerGateways.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeCustomerGatewaysResponse511 =

                    skel.describeCustomerGateways(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeCustomerGatewaysResponse511, false);
                } else

                if ("deleteNetworkAcl".equals(methodName)) {

                    com.amazon.ec2.DeleteNetworkAclResponse deleteNetworkAclResponse513 = null;
                    com.amazon.ec2.DeleteNetworkAcl wrappedParam =
                        (com.amazon.ec2.DeleteNetworkAcl)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteNetworkAcl.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteNetworkAclResponse513 =

                    skel.deleteNetworkAcl(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteNetworkAclResponse513, false);
                } else

                if ("deleteRoute".equals(methodName)) {

                    com.amazon.ec2.DeleteRouteResponse deleteRouteResponse515 = null;
                    com.amazon.ec2.DeleteRoute wrappedParam =
                        (com.amazon.ec2.DeleteRoute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteRoute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteRouteResponse515 =

                    skel.deleteRoute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteRouteResponse515, false);
                } else

                if ("rebootInstances".equals(methodName)) {

                    com.amazon.ec2.RebootInstancesResponse rebootInstancesResponse517 = null;
                    com.amazon.ec2.RebootInstances wrappedParam =
                        (com.amazon.ec2.RebootInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.RebootInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    rebootInstancesResponse517 =

                    skel.rebootInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), rebootInstancesResponse517, false);
                } else

                if ("modifyInstanceAttribute".equals(methodName)) {

                    com.amazon.ec2.ModifyInstanceAttributeResponse modifyInstanceAttributeResponse519 = null;
                    com.amazon.ec2.ModifyInstanceAttribute wrappedParam =
                        (com.amazon.ec2.ModifyInstanceAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.ModifyInstanceAttribute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    modifyInstanceAttributeResponse519 =

                    skel.modifyInstanceAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), modifyInstanceAttributeResponse519, false);
                } else

                if ("terminateInstances".equals(methodName)) {

                    com.amazon.ec2.TerminateInstancesResponse terminateInstancesResponse521 = null;
                    com.amazon.ec2.TerminateInstances wrappedParam =
                        (com.amazon.ec2.TerminateInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.TerminateInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    terminateInstancesResponse521 =

                    skel.terminateInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), terminateInstancesResponse521, false);
                } else

                if ("createVpnConnectionRoute".equals(methodName)) {

                    com.amazon.ec2.CreateVpnConnectionRouteResponse createVpnConnectionRouteResponse523 = null;
                    com.amazon.ec2.CreateVpnConnectionRoute wrappedParam =
                        (com.amazon.ec2.CreateVpnConnectionRoute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.CreateVpnConnectionRoute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createVpnConnectionRouteResponse523 =

                    skel.createVpnConnectionRoute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createVpnConnectionRouteResponse523, false);
                } else

                if ("describeConversionTasks".equals(methodName)) {

                    com.amazon.ec2.DescribeConversionTasksResponse describeConversionTasksResponse525 = null;
                    com.amazon.ec2.DescribeConversionTasks wrappedParam =
                        (com.amazon.ec2.DescribeConversionTasks)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeConversionTasks.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeConversionTasksResponse525 =

                    skel.describeConversionTasks(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeConversionTasksResponse525, false);
                } else

                if ("describeAddresses".equals(methodName)) {

                    com.amazon.ec2.DescribeAddressesResponse describeAddressesResponse527 = null;
                    com.amazon.ec2.DescribeAddresses wrappedParam =
                        (com.amazon.ec2.DescribeAddresses)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeAddresses.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeAddressesResponse527 =

                    skel.describeAddresses(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeAddressesResponse527, false);
                } else

                if ("describeInstanceAttribute".equals(methodName)) {

                    com.amazon.ec2.DescribeInstanceAttributeResponse describeInstanceAttributeResponse529 = null;
                    com.amazon.ec2.DescribeInstanceAttribute wrappedParam =
                        (com.amazon.ec2.DescribeInstanceAttribute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeInstanceAttribute.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeInstanceAttributeResponse529 =

                    skel.describeInstanceAttribute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeInstanceAttributeResponse529, false);
                } else

                if ("attachInternetGateway".equals(methodName)) {

                    com.amazon.ec2.AttachInternetGatewayResponse attachInternetGatewayResponse531 = null;
                    com.amazon.ec2.AttachInternetGateway wrappedParam =
                        (com.amazon.ec2.AttachInternetGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.AttachInternetGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    attachInternetGatewayResponse531 =

                    skel.attachInternetGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), attachInternetGatewayResponse531, false);
                } else

                if ("createVpc".equals(methodName)) {

                    com.amazon.ec2.CreateVpcResponse createVpcResponse533 = null;
                    com.amazon.ec2.CreateVpc wrappedParam =
                        (com.amazon.ec2.CreateVpc)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateVpc.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createVpcResponse533 =

                    skel.createVpc(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createVpcResponse533, false);
                } else

                if ("replaceRouteTableAssociation".equals(methodName)) {

                    com.amazon.ec2.ReplaceRouteTableAssociationResponse replaceRouteTableAssociationResponse535 = null;
                    com.amazon.ec2.ReplaceRouteTableAssociation wrappedParam =
                        (com.amazon.ec2.ReplaceRouteTableAssociation)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.ReplaceRouteTableAssociation.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    replaceRouteTableAssociationResponse535 =

                    skel.replaceRouteTableAssociation(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), replaceRouteTableAssociationResponse535, false);
                } else

                if ("associateRouteTable".equals(methodName)) {

                    com.amazon.ec2.AssociateRouteTableResponse associateRouteTableResponse537 = null;
                    com.amazon.ec2.AssociateRouteTable wrappedParam =
                        (com.amazon.ec2.AssociateRouteTable)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.AssociateRouteTable.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    associateRouteTableResponse537 =

                    skel.associateRouteTable(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), associateRouteTableResponse537, false);
                } else

                if ("detachInternetGateway".equals(methodName)) {

                    com.amazon.ec2.DetachInternetGatewayResponse detachInternetGatewayResponse539 = null;
                    com.amazon.ec2.DetachInternetGateway wrappedParam =
                        (com.amazon.ec2.DetachInternetGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DetachInternetGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    detachInternetGatewayResponse539 =

                    skel.detachInternetGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), detachInternetGatewayResponse539, false);
                } else

                if ("purchaseReservedInstancesOffering".equals(methodName)) {

                    com.amazon.ec2.PurchaseReservedInstancesOfferingResponse purchaseReservedInstancesOfferingResponse541 = null;
                    com.amazon.ec2.PurchaseReservedInstancesOffering wrappedParam =
                        (com.amazon.ec2.PurchaseReservedInstancesOffering)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.PurchaseReservedInstancesOffering.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    purchaseReservedInstancesOfferingResponse541 =

                    skel.purchaseReservedInstancesOffering(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), purchaseReservedInstancesOfferingResponse541, false);
                } else

                if ("importVolume".equals(methodName)) {

                    com.amazon.ec2.ImportVolumeResponse importVolumeResponse543 = null;
                    com.amazon.ec2.ImportVolume wrappedParam =
                        (com.amazon.ec2.ImportVolume)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ImportVolume.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    importVolumeResponse543 =

                    skel.importVolume(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), importVolumeResponse543, false);
                } else

                if ("describeExportTasks".equals(methodName)) {

                    com.amazon.ec2.DescribeExportTasksResponse describeExportTasksResponse545 = null;
                    com.amazon.ec2.DescribeExportTasks wrappedParam =
                        (com.amazon.ec2.DescribeExportTasks)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeExportTasks.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeExportTasksResponse545 =

                    skel.describeExportTasks(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeExportTasksResponse545, false);
                } else

                if ("createInstanceExportTask".equals(methodName)) {

                    com.amazon.ec2.CreateInstanceExportTaskResponse createInstanceExportTaskResponse547 = null;
                    com.amazon.ec2.CreateInstanceExportTask wrappedParam =
                        (com.amazon.ec2.CreateInstanceExportTask)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.CreateInstanceExportTask.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createInstanceExportTaskResponse547 =

                    skel.createInstanceExportTask(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createInstanceExportTaskResponse547, false);
                } else

                if ("assignPrivateIpAddresses".equals(methodName)) {

                    com.amazon.ec2.AssignPrivateIpAddressesResponse assignPrivateIpAddressesResponse549 = null;
                    com.amazon.ec2.AssignPrivateIpAddresses wrappedParam =
                        (com.amazon.ec2.AssignPrivateIpAddresses)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.AssignPrivateIpAddresses.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    assignPrivateIpAddressesResponse549 =

                    skel.assignPrivateIpAddresses(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), assignPrivateIpAddressesResponse549, false);
                } else

                if ("reportInstanceStatus".equals(methodName)) {

                    com.amazon.ec2.ReportInstanceStatusResponse reportInstanceStatusResponse551 = null;
                    com.amazon.ec2.ReportInstanceStatus wrappedParam =
                        (com.amazon.ec2.ReportInstanceStatus)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ReportInstanceStatus.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    reportInstanceStatusResponse551 =

                    skel.reportInstanceStatus(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), reportInstanceStatusResponse551, false);
                } else

                if ("describeReservedInstancesOfferings".equals(methodName)) {

                    com.amazon.ec2.DescribeReservedInstancesOfferingsResponse describeReservedInstancesOfferingsResponse553 = null;
                    com.amazon.ec2.DescribeReservedInstancesOfferings wrappedParam =
                        (com.amazon.ec2.DescribeReservedInstancesOfferings)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DescribeReservedInstancesOfferings.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeReservedInstancesOfferingsResponse553 =

                    skel.describeReservedInstancesOfferings(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeReservedInstancesOfferingsResponse553, false);
                } else

                if ("deleteVpnConnection".equals(methodName)) {

                    com.amazon.ec2.DeleteVpnConnectionResponse deleteVpnConnectionResponse555 = null;
                    com.amazon.ec2.DeleteVpnConnection wrappedParam =
                        (com.amazon.ec2.DeleteVpnConnection)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteVpnConnection.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteVpnConnectionResponse555 =

                    skel.deleteVpnConnection(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteVpnConnectionResponse555, false);
                } else

                if ("deleteInternetGateway".equals(methodName)) {

                    com.amazon.ec2.DeleteInternetGatewayResponse deleteInternetGatewayResponse557 = null;
                    com.amazon.ec2.DeleteInternetGateway wrappedParam =
                        (com.amazon.ec2.DeleteInternetGateway)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DeleteInternetGateway.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteInternetGatewayResponse557 =

                    skel.deleteInternetGateway(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteInternetGatewayResponse557, false);
                } else

                if ("deleteSpotDatafeedSubscription".equals(methodName)) {

                    com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse deleteSpotDatafeedSubscriptionResponse559 = null;
                    com.amazon.ec2.DeleteSpotDatafeedSubscription wrappedParam =
                        (com.amazon.ec2.DeleteSpotDatafeedSubscription)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.DeleteSpotDatafeedSubscription.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteSpotDatafeedSubscriptionResponse559 =

                    skel.deleteSpotDatafeedSubscription(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteSpotDatafeedSubscriptionResponse559, false);
                } else

                if ("attachNetworkInterface".equals(methodName)) {

                    com.amazon.ec2.AttachNetworkInterfaceResponse attachNetworkInterfaceResponse561 = null;
                    com.amazon.ec2.AttachNetworkInterface wrappedParam =
                        (com.amazon.ec2.AttachNetworkInterface)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.AttachNetworkInterface.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    attachNetworkInterfaceResponse561 =

                    skel.attachNetworkInterface(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), attachNetworkInterfaceResponse561, false);
                } else

                if ("createNetworkInterface".equals(methodName)) {

                    com.amazon.ec2.CreateNetworkInterfaceResponse createNetworkInterfaceResponse563 = null;
                    com.amazon.ec2.CreateNetworkInterface wrappedParam =
                        (com.amazon.ec2.CreateNetworkInterface)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.CreateNetworkInterface.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createNetworkInterfaceResponse563 =

                    skel.createNetworkInterface(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createNetworkInterfaceResponse563, false);
                } else

                if ("revokeSecurityGroupEgress".equals(methodName)) {

                    com.amazon.ec2.RevokeSecurityGroupEgressResponse revokeSecurityGroupEgressResponse565 = null;
                    com.amazon.ec2.RevokeSecurityGroupEgress wrappedParam =
                        (com.amazon.ec2.RevokeSecurityGroupEgress)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.ec2.RevokeSecurityGroupEgress.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    revokeSecurityGroupEgressResponse565 =

                    skel.revokeSecurityGroupEgress(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), revokeSecurityGroupEgressResponse565, false);
                } else

                if ("monitorInstances".equals(methodName)) {

                    com.amazon.ec2.MonitorInstancesResponse monitorInstancesResponse567 = null;
                    com.amazon.ec2.MonitorInstances wrappedParam =
                        (com.amazon.ec2.MonitorInstances)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.MonitorInstances.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    monitorInstancesResponse567 =

                    skel.monitorInstances(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), monitorInstancesResponse567, false);
                } else

                if ("replaceRoute".equals(methodName)) {

                    com.amazon.ec2.ReplaceRouteResponse replaceRouteResponse569 = null;
                    com.amazon.ec2.ReplaceRoute wrappedParam =
                        (com.amazon.ec2.ReplaceRoute)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.ReplaceRoute.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    replaceRouteResponse569 =

                    skel.replaceRoute(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), replaceRouteResponse569, false);
                } else

                if ("attachVolume".equals(methodName)) {

                    com.amazon.ec2.AttachVolumeResponse attachVolumeResponse571 = null;
                    com.amazon.ec2.AttachVolume wrappedParam =
                        (com.amazon.ec2.AttachVolume)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.AttachVolume.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    attachVolumeResponse571 =

                    skel.attachVolume(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), attachVolumeResponse571, false);
                } else

                if ("getConsoleOutput".equals(methodName)) {

                    com.amazon.ec2.GetConsoleOutputResponse getConsoleOutputResponse573 = null;
                    com.amazon.ec2.GetConsoleOutput wrappedParam =
                        (com.amazon.ec2.GetConsoleOutput)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.GetConsoleOutput.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    getConsoleOutputResponse573 =

                    skel.getConsoleOutput(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), getConsoleOutputResponse573, false);
                } else

                if ("describeRegions".equals(methodName)) {

                    com.amazon.ec2.DescribeRegionsResponse describeRegionsResponse575 = null;
                    com.amazon.ec2.DescribeRegions wrappedParam =
                        (com.amazon.ec2.DescribeRegions)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.ec2.DescribeRegions.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    describeRegionsResponse575 =

                    skel.describeRegions(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), describeRegionsResponse575, false);

                } else {
                    throw new java.lang.RuntimeException("method not found");
                }

                newMsgContext.setEnvelope(envelope);
            }
        } catch (java.lang.Exception e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    //
    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribePlacementGroups param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribePlacementGroups.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribePlacementGroupsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribePlacementGroupsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateSecurityGroup param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateSecurityGroup.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateSecurityGroupResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateSecurityGroupResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ResetNetworkInterfaceAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ResetNetworkInterfaceAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ResetNetworkInterfaceAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ResetNetworkInterfaceAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateDhcpOptions param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateDhcpOptions.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateDhcpOptionsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateDhcpOptionsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateRouteTable param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateRouteTable.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateRouteTableResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateRouteTableResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSubnets param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSubnets.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSubnetsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSubnetsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeactivateLicense param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeactivateLicense.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeactivateLicenseResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeactivateLicenseResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVpc param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVpc.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVpcResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVpcResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelSpotInstanceRequests param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelSpotInstanceRequests.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelSpotInstanceRequestsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelSpotInstanceRequestsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateSubnet param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateSubnet.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateSubnetResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateSubnetResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVpnGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVpnGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVpnGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVpnGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateNetworkAclEntry param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateNetworkAclEntry.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateNetworkAclEntryResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateNetworkAclEntryResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RequestSpotInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RequestSpotInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RequestSpotInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RequestSpotInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVolumeAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVolumeAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVolumeAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVolumeAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AssociateDhcpOptions param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AssociateDhcpOptions.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AssociateDhcpOptionsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AssociateDhcpOptionsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeTags param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeTags.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeTagsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeTagsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ImportKeyPair param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ImportKeyPair.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ImportKeyPairResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ImportKeyPairResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteNetworkInterface param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteNetworkInterface.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteNetworkInterfaceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteNetworkInterfaceResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVpcs param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVpcs.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVpcsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVpcsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeLicenses param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeLicenses.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeLicensesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeLicensesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.BundleInstance param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.BundleInstance.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.BundleInstanceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.BundleInstanceResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVpnConnections param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVpnConnections.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVpnConnectionsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVpnConnectionsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeImages param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeImages.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeImagesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeImagesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateInternetGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateInternetGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateInternetGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateInternetGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DisassociateRouteTable param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DisassociateRouteTable.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DisassociateRouteTableResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DisassociateRouteTableResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReplaceNetworkAclEntry param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReplaceNetworkAclEntry.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReplaceNetworkAclEntryResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReplaceNetworkAclEntryResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AuthorizeSecurityGroupIngress param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AuthorizeSecurityGroupIngress.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AuthorizeSecurityGroupIngressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AuthorizeSecurityGroupIngressResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSnapshotAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSnapshotAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSnapshotAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSnapshotAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVpnGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVpnGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVpnGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVpnGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ResetInstanceAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ResetInstanceAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ResetInstanceAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ResetInstanceAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateTags param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateTags.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateTagsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateTagsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AuthorizeSecurityGroupEgress param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AuthorizeSecurityGroupEgress.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AuthorizeSecurityGroupEgressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AuthorizeSecurityGroupEgressResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AssociateAddress param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AssociateAddress.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AssociateAddressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AssociateAddressResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeImageAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeImageAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeImageAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeImageAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSpotPriceHistory param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSpotPriceHistory.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSpotPriceHistoryResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSpotPriceHistoryResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifySnapshotAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifySnapshotAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifySnapshotAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifySnapshotAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSpotInstanceRequests param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSpotInstanceRequests.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSpotInstanceRequestsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSpotInstanceRequestsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ImportInstance param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ImportInstance.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ImportInstanceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ImportInstanceResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeKeyPairs param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeKeyPairs.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeKeyPairsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeKeyPairsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RevokeSecurityGroupIngress param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RevokeSecurityGroupIngress.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RevokeSecurityGroupIngressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RevokeSecurityGroupIngressResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreatePlacementGroup param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreatePlacementGroup.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreatePlacementGroupResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreatePlacementGroupResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteNetworkAclEntry param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteNetworkAclEntry.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteNetworkAclEntryResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteNetworkAclEntryResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ActivateLicense param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ActivateLicense.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ActivateLicenseResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ActivateLicenseResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteRouteTable param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteRouteTable.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteRouteTableResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteRouteTableResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.UnmonitorInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.UnmonitorInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.UnmonitorInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.UnmonitorInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.StartInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.StartInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.StartInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.StartInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ConfirmProductInstance param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ConfirmProductInstance.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ConfirmProductInstanceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ConfirmProductInstanceResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeNetworkInterfaceAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeNetworkInterfaceAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RunInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RunInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RunInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RunInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateReservedInstancesListing param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateReservedInstancesListing.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateReservedInstancesListingResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateReservedInstancesListingResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateCustomerGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateCustomerGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateCustomerGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateCustomerGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateNetworkAcl param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateNetworkAcl.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateNetworkAclResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateNetworkAclResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ResetImageAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ResetImageAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ResetImageAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ResetImageAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifyVolumeAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifyVolumeAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifyVolumeAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifyVolumeAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeReservedInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeReservedInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeReservedInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeReservedInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ResetSnapshotAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ResetSnapshotAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ResetSnapshotAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ResetSnapshotAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVolume param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVolume.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVolumeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVolumeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeAvailabilityZones param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeAvailabilityZones.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeAvailabilityZonesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeAvailabilityZonesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVpnConnection param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVpnConnection.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVpnConnectionResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVpnConnectionResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelBundleTask param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelBundleTask.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelBundleTaskResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelBundleTaskResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReplaceNetworkAclAssociation param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReplaceNetworkAclAssociation.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReplaceNetworkAclAssociationResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReplaceNetworkAclAssociationResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DetachVpnGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DetachVpnGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DetachVpnGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DetachVpnGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSnapshots param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSnapshots.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSnapshotsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSnapshotsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteSubnet param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteSubnet.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteSubnetResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteSubnetResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeBundleTasks param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeBundleTasks.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeBundleTasksResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeBundleTasksResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateKeyPair param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateKeyPair.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateKeyPairResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateKeyPairResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateImage param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateImage.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateImageResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateImageResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.EnableVgwRoutePropagation param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.EnableVgwRoutePropagation.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.EnableVgwRoutePropagationResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.EnableVgwRoutePropagationResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.UnassignPrivateIpAddresses param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.UnassignPrivateIpAddresses.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.UnassignPrivateIpAddressesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.UnassignPrivateIpAddressesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeregisterImage param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeregisterImage.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeregisterImageResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeregisterImageResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVpnConnectionRoute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVpnConnectionRoute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVpnConnectionRouteResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVpnConnectionRouteResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifyImageAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifyImageAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifyImageAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifyImageAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelConversionTask param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelConversionTask.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelConversionTaskResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelConversionTaskResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVolumes param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVolumes.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVolumesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVolumesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelReservedInstancesListing param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelReservedInstancesListing.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelReservedInstancesListingResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelReservedInstancesListingResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.GetPasswordData param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.GetPasswordData.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.GetPasswordDataResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.GetPasswordDataResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AllocateAddress param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AllocateAddress.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AllocateAddressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AllocateAddressResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteSecurityGroup param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteSecurityGroup.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteSecurityGroupResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteSecurityGroupResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeletePlacementGroup param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeletePlacementGroup.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeletePlacementGroupResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeletePlacementGroupResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DisassociateAddress param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DisassociateAddress.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DisassociateAddressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DisassociateAddressResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteDhcpOptions param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteDhcpOptions.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteDhcpOptionsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteDhcpOptionsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSpotDatafeedSubscription param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSpotDatafeedSubscription.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeNetworkAcls param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeNetworkAcls.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeNetworkAclsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeNetworkAclsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.EnableVolumeIO param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.EnableVolumeIO.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.EnableVolumeIOResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.EnableVolumeIOResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AttachVpnGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AttachVpnGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AttachVpnGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AttachVpnGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeInternetGateways param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeInternetGateways.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeInternetGatewaysResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeInternetGatewaysResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeDhcpOptions param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeDhcpOptions.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeDhcpOptionsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeDhcpOptionsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateSpotDatafeedSubscription param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateSpotDatafeedSubscription.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeReservedInstancesListings param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeReservedInstancesListings.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeReservedInstancesListingsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeReservedInstancesListingsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeInstanceStatus param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeInstanceStatus.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeInstanceStatusResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeInstanceStatusResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifyNetworkInterfaceAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifyNetworkInterfaceAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DisableVgwRoutePropagation param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DisableVgwRoutePropagation.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DisableVgwRoutePropagationResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DisableVgwRoutePropagationResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVolumeStatus param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVolumeStatus.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVolumeStatusResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVolumeStatusResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DetachNetworkInterface param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DetachNetworkInterface.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DetachNetworkInterfaceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DetachNetworkInterfaceResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSecurityGroups param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSecurityGroups.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeSecurityGroupsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeSecurityGroupsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeNetworkInterfaces param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeNetworkInterfaces.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeNetworkInterfacesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeNetworkInterfacesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteKeyPair param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteKeyPair.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteKeyPairResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteKeyPairResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateSnapshot param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateSnapshot.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateSnapshotResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateSnapshotResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVpnGateways param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVpnGateways.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeVpnGatewaysResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeVpnGatewaysResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteTags param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteTags.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteTagsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteTagsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteSnapshot param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteSnapshot.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteSnapshotResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteSnapshotResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteCustomerGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteCustomerGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteCustomerGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteCustomerGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVolume param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVolume.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVolumeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVolumeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelExportTask param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelExportTask.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CancelExportTaskResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CancelExportTaskResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RegisterImage param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RegisterImage.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RegisterImageResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RegisterImageResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DetachVolume param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DetachVolume.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DetachVolumeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DetachVolumeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.StopInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.StopInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.StopInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.StopInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateRoute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateRoute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateRouteResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateRouteResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReleaseAddress param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReleaseAddress.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReleaseAddressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReleaseAddressResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeRouteTables param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeRouteTables.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeRouteTablesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeRouteTablesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeCustomerGateways param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeCustomerGateways.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeCustomerGatewaysResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeCustomerGatewaysResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteNetworkAcl param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteNetworkAcl.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteNetworkAclResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteNetworkAclResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteRoute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteRoute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteRouteResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteRouteResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RebootInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RebootInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RebootInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RebootInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifyInstanceAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifyInstanceAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ModifyInstanceAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ModifyInstanceAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.TerminateInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.TerminateInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.TerminateInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.TerminateInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVpnConnectionRoute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVpnConnectionRoute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVpnConnectionRouteResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVpnConnectionRouteResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeConversionTasks param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeConversionTasks.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeConversionTasksResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeConversionTasksResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeAddresses param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeAddresses.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeAddressesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeAddressesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeInstanceAttribute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeInstanceAttribute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeInstanceAttributeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeInstanceAttributeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AttachInternetGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AttachInternetGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AttachInternetGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AttachInternetGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVpc param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVpc.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateVpcResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateVpcResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReplaceRouteTableAssociation param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReplaceRouteTableAssociation.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReplaceRouteTableAssociationResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReplaceRouteTableAssociationResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AssociateRouteTable param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AssociateRouteTable.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AssociateRouteTableResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AssociateRouteTableResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DetachInternetGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DetachInternetGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DetachInternetGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DetachInternetGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.PurchaseReservedInstancesOffering param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.PurchaseReservedInstancesOffering.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.PurchaseReservedInstancesOfferingResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.PurchaseReservedInstancesOfferingResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ImportVolume param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ImportVolume.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ImportVolumeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ImportVolumeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeExportTasks param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeExportTasks.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeExportTasksResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeExportTasksResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateInstanceExportTask param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateInstanceExportTask.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateInstanceExportTaskResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateInstanceExportTaskResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AssignPrivateIpAddresses param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AssignPrivateIpAddresses.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AssignPrivateIpAddressesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AssignPrivateIpAddressesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReportInstanceStatus param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReportInstanceStatus.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReportInstanceStatusResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReportInstanceStatusResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeReservedInstancesOfferings param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeReservedInstancesOfferings.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeReservedInstancesOfferingsResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeReservedInstancesOfferingsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVpnConnection param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVpnConnection.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteVpnConnectionResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteVpnConnectionResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteInternetGateway param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteInternetGateway.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteInternetGatewayResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteInternetGatewayResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteSpotDatafeedSubscription param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteSpotDatafeedSubscription.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AttachNetworkInterface param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AttachNetworkInterface.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AttachNetworkInterfaceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AttachNetworkInterfaceResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateNetworkInterface param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateNetworkInterface.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.CreateNetworkInterfaceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.CreateNetworkInterfaceResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RevokeSecurityGroupEgress param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RevokeSecurityGroupEgress.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.RevokeSecurityGroupEgressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.RevokeSecurityGroupEgressResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.MonitorInstances param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.MonitorInstances.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.MonitorInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.MonitorInstancesResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReplaceRoute param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReplaceRoute.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.ReplaceRouteResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.ReplaceRouteResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AttachVolume param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AttachVolume.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.AttachVolumeResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.AttachVolumeResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.GetConsoleOutput param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.GetConsoleOutput.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.GetConsoleOutputResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.GetConsoleOutputResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeRegions param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeRegions.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.ec2.DescribeRegionsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.ec2.DescribeRegionsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribePlacementGroupsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribePlacementGroupsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribePlacementGroupsResponse wrapDescribePlacementGroups() {
        com.amazon.ec2.DescribePlacementGroupsResponse wrappedElement = new com.amazon.ec2.DescribePlacementGroupsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateSecurityGroupResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateSecurityGroupResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateSecurityGroupResponse wrapCreateSecurityGroup() {
        com.amazon.ec2.CreateSecurityGroupResponse wrappedElement = new com.amazon.ec2.CreateSecurityGroupResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ResetNetworkInterfaceAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ResetNetworkInterfaceAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ResetNetworkInterfaceAttributeResponse wrapResetNetworkInterfaceAttribute() {
        com.amazon.ec2.ResetNetworkInterfaceAttributeResponse wrappedElement = new com.amazon.ec2.ResetNetworkInterfaceAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateDhcpOptionsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateDhcpOptionsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateDhcpOptionsResponse wrapCreateDhcpOptions() {
        com.amazon.ec2.CreateDhcpOptionsResponse wrappedElement = new com.amazon.ec2.CreateDhcpOptionsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateRouteTableResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateRouteTableResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateRouteTableResponse wrapCreateRouteTable() {
        com.amazon.ec2.CreateRouteTableResponse wrappedElement = new com.amazon.ec2.CreateRouteTableResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeSubnetsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeSubnetsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeSubnetsResponse wrapDescribeSubnets() {
        com.amazon.ec2.DescribeSubnetsResponse wrappedElement = new com.amazon.ec2.DescribeSubnetsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeactivateLicenseResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeactivateLicenseResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeactivateLicenseResponse wrapDeactivateLicense() {
        com.amazon.ec2.DeactivateLicenseResponse wrappedElement = new com.amazon.ec2.DeactivateLicenseResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteVpcResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteVpcResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteVpcResponse wrapDeleteVpc() {
        com.amazon.ec2.DeleteVpcResponse wrappedElement = new com.amazon.ec2.DeleteVpcResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CancelSpotInstanceRequestsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CancelSpotInstanceRequestsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CancelSpotInstanceRequestsResponse wrapCancelSpotInstanceRequests() {
        com.amazon.ec2.CancelSpotInstanceRequestsResponse wrappedElement = new com.amazon.ec2.CancelSpotInstanceRequestsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateSubnetResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateSubnetResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateSubnetResponse wrapCreateSubnet() {
        com.amazon.ec2.CreateSubnetResponse wrappedElement = new com.amazon.ec2.CreateSubnetResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteVpnGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteVpnGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteVpnGatewayResponse wrapDeleteVpnGateway() {
        com.amazon.ec2.DeleteVpnGatewayResponse wrappedElement = new com.amazon.ec2.DeleteVpnGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateNetworkAclEntryResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateNetworkAclEntryResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateNetworkAclEntryResponse wrapCreateNetworkAclEntry() {
        com.amazon.ec2.CreateNetworkAclEntryResponse wrappedElement = new com.amazon.ec2.CreateNetworkAclEntryResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.RequestSpotInstancesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.RequestSpotInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.RequestSpotInstancesResponse wrapRequestSpotInstances() {
        com.amazon.ec2.RequestSpotInstancesResponse wrappedElement = new com.amazon.ec2.RequestSpotInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeVolumeAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeVolumeAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeVolumeAttributeResponse wrapDescribeVolumeAttribute() {
        com.amazon.ec2.DescribeVolumeAttributeResponse wrappedElement = new com.amazon.ec2.DescribeVolumeAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AssociateDhcpOptionsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AssociateDhcpOptionsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AssociateDhcpOptionsResponse wrapAssociateDhcpOptions() {
        com.amazon.ec2.AssociateDhcpOptionsResponse wrappedElement = new com.amazon.ec2.AssociateDhcpOptionsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeTagsResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeTagsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeTagsResponse wrapDescribeTags() {
        com.amazon.ec2.DescribeTagsResponse wrappedElement = new com.amazon.ec2.DescribeTagsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ImportKeyPairResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ImportKeyPairResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ImportKeyPairResponse wrapImportKeyPair() {
        com.amazon.ec2.ImportKeyPairResponse wrappedElement = new com.amazon.ec2.ImportKeyPairResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteNetworkInterfaceResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteNetworkInterfaceResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteNetworkInterfaceResponse wrapDeleteNetworkInterface() {
        com.amazon.ec2.DeleteNetworkInterfaceResponse wrappedElement = new com.amazon.ec2.DeleteNetworkInterfaceResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeVpcsResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeVpcsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeVpcsResponse wrapDescribeVpcs() {
        com.amazon.ec2.DescribeVpcsResponse wrappedElement = new com.amazon.ec2.DescribeVpcsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeLicensesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeLicensesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeLicensesResponse wrapDescribeLicenses() {
        com.amazon.ec2.DescribeLicensesResponse wrappedElement = new com.amazon.ec2.DescribeLicensesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.BundleInstanceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.BundleInstanceResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.BundleInstanceResponse wrapBundleInstance() {
        com.amazon.ec2.BundleInstanceResponse wrappedElement = new com.amazon.ec2.BundleInstanceResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeVpnConnectionsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeVpnConnectionsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeVpnConnectionsResponse wrapDescribeVpnConnections() {
        com.amazon.ec2.DescribeVpnConnectionsResponse wrappedElement = new com.amazon.ec2.DescribeVpnConnectionsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeImagesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeImagesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeImagesResponse wrapDescribeImages() {
        com.amazon.ec2.DescribeImagesResponse wrappedElement = new com.amazon.ec2.DescribeImagesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateInternetGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateInternetGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateInternetGatewayResponse wrapCreateInternetGateway() {
        com.amazon.ec2.CreateInternetGatewayResponse wrappedElement = new com.amazon.ec2.CreateInternetGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DisassociateRouteTableResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DisassociateRouteTableResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DisassociateRouteTableResponse wrapDisassociateRouteTable() {
        com.amazon.ec2.DisassociateRouteTableResponse wrappedElement = new com.amazon.ec2.DisassociateRouteTableResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ReplaceNetworkAclEntryResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ReplaceNetworkAclEntryResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ReplaceNetworkAclEntryResponse wrapReplaceNetworkAclEntry() {
        com.amazon.ec2.ReplaceNetworkAclEntryResponse wrappedElement = new com.amazon.ec2.ReplaceNetworkAclEntryResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AuthorizeSecurityGroupIngressResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AuthorizeSecurityGroupIngressResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AuthorizeSecurityGroupIngressResponse wrapAuthorizeSecurityGroupIngress() {
        com.amazon.ec2.AuthorizeSecurityGroupIngressResponse wrappedElement = new com.amazon.ec2.AuthorizeSecurityGroupIngressResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeSnapshotAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeSnapshotAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeSnapshotAttributeResponse wrapDescribeSnapshotAttribute() {
        com.amazon.ec2.DescribeSnapshotAttributeResponse wrappedElement = new com.amazon.ec2.DescribeSnapshotAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateVpnGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateVpnGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateVpnGatewayResponse wrapCreateVpnGateway() {
        com.amazon.ec2.CreateVpnGatewayResponse wrappedElement = new com.amazon.ec2.CreateVpnGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ResetInstanceAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ResetInstanceAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ResetInstanceAttributeResponse wrapResetInstanceAttribute() {
        com.amazon.ec2.ResetInstanceAttributeResponse wrappedElement = new com.amazon.ec2.ResetInstanceAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateTagsResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateTagsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateTagsResponse wrapCreateTags() {
        com.amazon.ec2.CreateTagsResponse wrappedElement = new com.amazon.ec2.CreateTagsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AuthorizeSecurityGroupEgressResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AuthorizeSecurityGroupEgressResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AuthorizeSecurityGroupEgressResponse wrapAuthorizeSecurityGroupEgress() {
        com.amazon.ec2.AuthorizeSecurityGroupEgressResponse wrappedElement = new com.amazon.ec2.AuthorizeSecurityGroupEgressResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AssociateAddressResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AssociateAddressResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AssociateAddressResponse wrapAssociateAddress() {
        com.amazon.ec2.AssociateAddressResponse wrappedElement = new com.amazon.ec2.AssociateAddressResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeImageAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeImageAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeImageAttributeResponse wrapDescribeImageAttribute() {
        com.amazon.ec2.DescribeImageAttributeResponse wrappedElement = new com.amazon.ec2.DescribeImageAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeSpotPriceHistoryResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeSpotPriceHistoryResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeSpotPriceHistoryResponse wrapDescribeSpotPriceHistory() {
        com.amazon.ec2.DescribeSpotPriceHistoryResponse wrappedElement = new com.amazon.ec2.DescribeSpotPriceHistoryResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ModifySnapshotAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ModifySnapshotAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ModifySnapshotAttributeResponse wrapModifySnapshotAttribute() {
        com.amazon.ec2.ModifySnapshotAttributeResponse wrappedElement = new com.amazon.ec2.ModifySnapshotAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeSpotInstanceRequestsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeSpotInstanceRequestsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeSpotInstanceRequestsResponse wrapDescribeSpotInstanceRequests() {
        com.amazon.ec2.DescribeSpotInstanceRequestsResponse wrappedElement = new com.amazon.ec2.DescribeSpotInstanceRequestsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ImportInstanceResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ImportInstanceResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ImportInstanceResponse wrapImportInstance() {
        com.amazon.ec2.ImportInstanceResponse wrappedElement = new com.amazon.ec2.ImportInstanceResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeKeyPairsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeKeyPairsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeKeyPairsResponse wrapDescribeKeyPairs() {
        com.amazon.ec2.DescribeKeyPairsResponse wrappedElement = new com.amazon.ec2.DescribeKeyPairsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.RevokeSecurityGroupIngressResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.RevokeSecurityGroupIngressResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.RevokeSecurityGroupIngressResponse wrapRevokeSecurityGroupIngress() {
        com.amazon.ec2.RevokeSecurityGroupIngressResponse wrappedElement = new com.amazon.ec2.RevokeSecurityGroupIngressResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreatePlacementGroupResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreatePlacementGroupResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreatePlacementGroupResponse wrapCreatePlacementGroup() {
        com.amazon.ec2.CreatePlacementGroupResponse wrappedElement = new com.amazon.ec2.CreatePlacementGroupResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteNetworkAclEntryResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteNetworkAclEntryResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteNetworkAclEntryResponse wrapDeleteNetworkAclEntry() {
        com.amazon.ec2.DeleteNetworkAclEntryResponse wrappedElement = new com.amazon.ec2.DeleteNetworkAclEntryResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ActivateLicenseResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ActivateLicenseResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ActivateLicenseResponse wrapActivateLicense() {
        com.amazon.ec2.ActivateLicenseResponse wrappedElement = new com.amazon.ec2.ActivateLicenseResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteRouteTableResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteRouteTableResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteRouteTableResponse wrapDeleteRouteTable() {
        com.amazon.ec2.DeleteRouteTableResponse wrappedElement = new com.amazon.ec2.DeleteRouteTableResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.UnmonitorInstancesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.UnmonitorInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.UnmonitorInstancesResponse wrapUnmonitorInstances() {
        com.amazon.ec2.UnmonitorInstancesResponse wrappedElement = new com.amazon.ec2.UnmonitorInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.StartInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.StartInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.StartInstancesResponse wrapStartInstances() {
        com.amazon.ec2.StartInstancesResponse wrappedElement = new com.amazon.ec2.StartInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ConfirmProductInstanceResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ConfirmProductInstanceResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ConfirmProductInstanceResponse wrapConfirmProductInstance() {
        com.amazon.ec2.ConfirmProductInstanceResponse wrappedElement = new com.amazon.ec2.ConfirmProductInstanceResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse wrapDescribeNetworkInterfaceAttribute() {
        com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse wrappedElement = new com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.RunInstancesResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.RunInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.RunInstancesResponse wrapRunInstances() {
        com.amazon.ec2.RunInstancesResponse wrappedElement = new com.amazon.ec2.RunInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateReservedInstancesListingResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateReservedInstancesListingResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateReservedInstancesListingResponse wrapCreateReservedInstancesListing() {
        com.amazon.ec2.CreateReservedInstancesListingResponse wrappedElement = new com.amazon.ec2.CreateReservedInstancesListingResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateCustomerGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateCustomerGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateCustomerGatewayResponse wrapCreateCustomerGateway() {
        com.amazon.ec2.CreateCustomerGatewayResponse wrappedElement = new com.amazon.ec2.CreateCustomerGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateNetworkAclResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateNetworkAclResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateNetworkAclResponse wrapCreateNetworkAcl() {
        com.amazon.ec2.CreateNetworkAclResponse wrappedElement = new com.amazon.ec2.CreateNetworkAclResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ResetImageAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ResetImageAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ResetImageAttributeResponse wrapResetImageAttribute() {
        com.amazon.ec2.ResetImageAttributeResponse wrappedElement = new com.amazon.ec2.ResetImageAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ModifyVolumeAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ModifyVolumeAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ModifyVolumeAttributeResponse wrapModifyVolumeAttribute() {
        com.amazon.ec2.ModifyVolumeAttributeResponse wrappedElement = new com.amazon.ec2.ModifyVolumeAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeReservedInstancesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeReservedInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeReservedInstancesResponse wrapDescribeReservedInstances() {
        com.amazon.ec2.DescribeReservedInstancesResponse wrappedElement = new com.amazon.ec2.DescribeReservedInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ResetSnapshotAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ResetSnapshotAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ResetSnapshotAttributeResponse wrapResetSnapshotAttribute() {
        com.amazon.ec2.ResetSnapshotAttributeResponse wrappedElement = new com.amazon.ec2.ResetSnapshotAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteVolumeResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteVolumeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteVolumeResponse wrapDeleteVolume() {
        com.amazon.ec2.DeleteVolumeResponse wrappedElement = new com.amazon.ec2.DeleteVolumeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeAvailabilityZonesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeAvailabilityZonesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeAvailabilityZonesResponse wrapDescribeAvailabilityZones() {
        com.amazon.ec2.DescribeAvailabilityZonesResponse wrappedElement = new com.amazon.ec2.DescribeAvailabilityZonesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateVpnConnectionResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateVpnConnectionResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateVpnConnectionResponse wrapCreateVpnConnection() {
        com.amazon.ec2.CreateVpnConnectionResponse wrappedElement = new com.amazon.ec2.CreateVpnConnectionResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CancelBundleTaskResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CancelBundleTaskResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CancelBundleTaskResponse wrapCancelBundleTask() {
        com.amazon.ec2.CancelBundleTaskResponse wrappedElement = new com.amazon.ec2.CancelBundleTaskResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ReplaceNetworkAclAssociationResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ReplaceNetworkAclAssociationResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ReplaceNetworkAclAssociationResponse wrapReplaceNetworkAclAssociation() {
        com.amazon.ec2.ReplaceNetworkAclAssociationResponse wrappedElement = new com.amazon.ec2.ReplaceNetworkAclAssociationResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DetachVpnGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DetachVpnGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DetachVpnGatewayResponse wrapDetachVpnGateway() {
        com.amazon.ec2.DetachVpnGatewayResponse wrappedElement = new com.amazon.ec2.DetachVpnGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeSnapshotsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeSnapshotsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeSnapshotsResponse wrapDescribeSnapshots() {
        com.amazon.ec2.DescribeSnapshotsResponse wrappedElement = new com.amazon.ec2.DescribeSnapshotsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteSubnetResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteSubnetResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteSubnetResponse wrapDeleteSubnet() {
        com.amazon.ec2.DeleteSubnetResponse wrappedElement = new com.amazon.ec2.DeleteSubnetResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeBundleTasksResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeBundleTasksResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeBundleTasksResponse wrapDescribeBundleTasks() {
        com.amazon.ec2.DescribeBundleTasksResponse wrappedElement = new com.amazon.ec2.DescribeBundleTasksResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateKeyPairResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateKeyPairResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateKeyPairResponse wrapCreateKeyPair() {
        com.amazon.ec2.CreateKeyPairResponse wrappedElement = new com.amazon.ec2.CreateKeyPairResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateImageResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateImageResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateImageResponse wrapCreateImage() {
        com.amazon.ec2.CreateImageResponse wrappedElement = new com.amazon.ec2.CreateImageResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.EnableVgwRoutePropagationResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.EnableVgwRoutePropagationResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.EnableVgwRoutePropagationResponse wrapEnableVgwRoutePropagation() {
        com.amazon.ec2.EnableVgwRoutePropagationResponse wrappedElement = new com.amazon.ec2.EnableVgwRoutePropagationResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.UnassignPrivateIpAddressesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.UnassignPrivateIpAddressesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.UnassignPrivateIpAddressesResponse wrapUnassignPrivateIpAddresses() {
        com.amazon.ec2.UnassignPrivateIpAddressesResponse wrappedElement = new com.amazon.ec2.UnassignPrivateIpAddressesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeregisterImageResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeregisterImageResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeregisterImageResponse wrapDeregisterImage() {
        com.amazon.ec2.DeregisterImageResponse wrappedElement = new com.amazon.ec2.DeregisterImageResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteVpnConnectionRouteResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteVpnConnectionRouteResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteVpnConnectionRouteResponse wrapDeleteVpnConnectionRoute() {
        com.amazon.ec2.DeleteVpnConnectionRouteResponse wrappedElement = new com.amazon.ec2.DeleteVpnConnectionRouteResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ModifyImageAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ModifyImageAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ModifyImageAttributeResponse wrapModifyImageAttribute() {
        com.amazon.ec2.ModifyImageAttributeResponse wrappedElement = new com.amazon.ec2.ModifyImageAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CancelConversionTaskResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CancelConversionTaskResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CancelConversionTaskResponse wrapCancelConversionTask() {
        com.amazon.ec2.CancelConversionTaskResponse wrappedElement = new com.amazon.ec2.CancelConversionTaskResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeVolumesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeVolumesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeVolumesResponse wrapDescribeVolumes() {
        com.amazon.ec2.DescribeVolumesResponse wrappedElement = new com.amazon.ec2.DescribeVolumesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CancelReservedInstancesListingResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CancelReservedInstancesListingResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CancelReservedInstancesListingResponse wrapCancelReservedInstancesListing() {
        com.amazon.ec2.CancelReservedInstancesListingResponse wrappedElement = new com.amazon.ec2.CancelReservedInstancesListingResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.GetPasswordDataResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.GetPasswordDataResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.GetPasswordDataResponse wrapGetPasswordData() {
        com.amazon.ec2.GetPasswordDataResponse wrappedElement = new com.amazon.ec2.GetPasswordDataResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AllocateAddressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AllocateAddressResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AllocateAddressResponse wrapAllocateAddress() {
        com.amazon.ec2.AllocateAddressResponse wrappedElement = new com.amazon.ec2.AllocateAddressResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteSecurityGroupResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteSecurityGroupResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteSecurityGroupResponse wrapDeleteSecurityGroup() {
        com.amazon.ec2.DeleteSecurityGroupResponse wrappedElement = new com.amazon.ec2.DeleteSecurityGroupResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeletePlacementGroupResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeletePlacementGroupResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeletePlacementGroupResponse wrapDeletePlacementGroup() {
        com.amazon.ec2.DeletePlacementGroupResponse wrappedElement = new com.amazon.ec2.DeletePlacementGroupResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DisassociateAddressResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DisassociateAddressResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DisassociateAddressResponse wrapDisassociateAddress() {
        com.amazon.ec2.DisassociateAddressResponse wrappedElement = new com.amazon.ec2.DisassociateAddressResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteDhcpOptionsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteDhcpOptionsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteDhcpOptionsResponse wrapDeleteDhcpOptions() {
        com.amazon.ec2.DeleteDhcpOptionsResponse wrappedElement = new com.amazon.ec2.DeleteDhcpOptionsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse wrapDescribeSpotDatafeedSubscription() {
        com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse wrappedElement = new com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeNetworkAclsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeNetworkAclsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeNetworkAclsResponse wrapDescribeNetworkAcls() {
        com.amazon.ec2.DescribeNetworkAclsResponse wrappedElement = new com.amazon.ec2.DescribeNetworkAclsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.EnableVolumeIOResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.EnableVolumeIOResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.EnableVolumeIOResponse wrapEnableVolumeIO() {
        com.amazon.ec2.EnableVolumeIOResponse wrappedElement = new com.amazon.ec2.EnableVolumeIOResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AttachVpnGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AttachVpnGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AttachVpnGatewayResponse wrapAttachVpnGateway() {
        com.amazon.ec2.AttachVpnGatewayResponse wrappedElement = new com.amazon.ec2.AttachVpnGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeInternetGatewaysResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeInternetGatewaysResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeInternetGatewaysResponse wrapDescribeInternetGateways() {
        com.amazon.ec2.DescribeInternetGatewaysResponse wrappedElement = new com.amazon.ec2.DescribeInternetGatewaysResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeDhcpOptionsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeDhcpOptionsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeDhcpOptionsResponse wrapDescribeDhcpOptions() {
        com.amazon.ec2.DescribeDhcpOptionsResponse wrappedElement = new com.amazon.ec2.DescribeDhcpOptionsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse wrapCreateSpotDatafeedSubscription() {
        com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse wrappedElement = new com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeReservedInstancesListingsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeReservedInstancesListingsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeReservedInstancesListingsResponse wrapDescribeReservedInstancesListings() {
        com.amazon.ec2.DescribeReservedInstancesListingsResponse wrappedElement = new com.amazon.ec2.DescribeReservedInstancesListingsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeInstanceStatusResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeInstanceStatusResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeInstanceStatusResponse wrapDescribeInstanceStatus() {
        com.amazon.ec2.DescribeInstanceStatusResponse wrappedElement = new com.amazon.ec2.DescribeInstanceStatusResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse wrapModifyNetworkInterfaceAttribute() {
        com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse wrappedElement = new com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DisableVgwRoutePropagationResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DisableVgwRoutePropagationResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DisableVgwRoutePropagationResponse wrapDisableVgwRoutePropagation() {
        com.amazon.ec2.DisableVgwRoutePropagationResponse wrappedElement = new com.amazon.ec2.DisableVgwRoutePropagationResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeVolumeStatusResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeVolumeStatusResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeVolumeStatusResponse wrapDescribeVolumeStatus() {
        com.amazon.ec2.DescribeVolumeStatusResponse wrappedElement = new com.amazon.ec2.DescribeVolumeStatusResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DetachNetworkInterfaceResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DetachNetworkInterfaceResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DetachNetworkInterfaceResponse wrapDetachNetworkInterface() {
        com.amazon.ec2.DetachNetworkInterfaceResponse wrappedElement = new com.amazon.ec2.DetachNetworkInterfaceResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeSecurityGroupsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeSecurityGroupsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeSecurityGroupsResponse wrapDescribeSecurityGroups() {
        com.amazon.ec2.DescribeSecurityGroupsResponse wrappedElement = new com.amazon.ec2.DescribeSecurityGroupsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeInstancesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeInstancesResponse wrapDescribeInstances() {
        com.amazon.ec2.DescribeInstancesResponse wrappedElement = new com.amazon.ec2.DescribeInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeNetworkInterfacesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeNetworkInterfacesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeNetworkInterfacesResponse wrapDescribeNetworkInterfaces() {
        com.amazon.ec2.DescribeNetworkInterfacesResponse wrappedElement = new com.amazon.ec2.DescribeNetworkInterfacesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteKeyPairResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteKeyPairResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteKeyPairResponse wrapDeleteKeyPair() {
        com.amazon.ec2.DeleteKeyPairResponse wrappedElement = new com.amazon.ec2.DeleteKeyPairResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateSnapshotResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateSnapshotResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateSnapshotResponse wrapCreateSnapshot() {
        com.amazon.ec2.CreateSnapshotResponse wrappedElement = new com.amazon.ec2.CreateSnapshotResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeVpnGatewaysResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeVpnGatewaysResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeVpnGatewaysResponse wrapDescribeVpnGateways() {
        com.amazon.ec2.DescribeVpnGatewaysResponse wrappedElement = new com.amazon.ec2.DescribeVpnGatewaysResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteTagsResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteTagsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteTagsResponse wrapDeleteTags() {
        com.amazon.ec2.DeleteTagsResponse wrappedElement = new com.amazon.ec2.DeleteTagsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteSnapshotResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteSnapshotResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteSnapshotResponse wrapDeleteSnapshot() {
        com.amazon.ec2.DeleteSnapshotResponse wrappedElement = new com.amazon.ec2.DeleteSnapshotResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteCustomerGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteCustomerGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteCustomerGatewayResponse wrapDeleteCustomerGateway() {
        com.amazon.ec2.DeleteCustomerGatewayResponse wrappedElement = new com.amazon.ec2.DeleteCustomerGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateVolumeResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateVolumeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateVolumeResponse wrapCreateVolume() {
        com.amazon.ec2.CreateVolumeResponse wrappedElement = new com.amazon.ec2.CreateVolumeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CancelExportTaskResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CancelExportTaskResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CancelExportTaskResponse wrapCancelExportTask() {
        com.amazon.ec2.CancelExportTaskResponse wrappedElement = new com.amazon.ec2.CancelExportTaskResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.RegisterImageResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.RegisterImageResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.RegisterImageResponse wrapRegisterImage() {
        com.amazon.ec2.RegisterImageResponse wrappedElement = new com.amazon.ec2.RegisterImageResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DetachVolumeResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DetachVolumeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DetachVolumeResponse wrapDetachVolume() {
        com.amazon.ec2.DetachVolumeResponse wrappedElement = new com.amazon.ec2.DetachVolumeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.StopInstancesResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.StopInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.StopInstancesResponse wrapStopInstances() {
        com.amazon.ec2.StopInstancesResponse wrappedElement = new com.amazon.ec2.StopInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateRouteResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateRouteResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateRouteResponse wrapCreateRoute() {
        com.amazon.ec2.CreateRouteResponse wrappedElement = new com.amazon.ec2.CreateRouteResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ReleaseAddressResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ReleaseAddressResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ReleaseAddressResponse wrapReleaseAddress() {
        com.amazon.ec2.ReleaseAddressResponse wrappedElement = new com.amazon.ec2.ReleaseAddressResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeRouteTablesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeRouteTablesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeRouteTablesResponse wrapDescribeRouteTables() {
        com.amazon.ec2.DescribeRouteTablesResponse wrappedElement = new com.amazon.ec2.DescribeRouteTablesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeCustomerGatewaysResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeCustomerGatewaysResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeCustomerGatewaysResponse wrapDescribeCustomerGateways() {
        com.amazon.ec2.DescribeCustomerGatewaysResponse wrappedElement = new com.amazon.ec2.DescribeCustomerGatewaysResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteNetworkAclResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteNetworkAclResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteNetworkAclResponse wrapDeleteNetworkAcl() {
        com.amazon.ec2.DeleteNetworkAclResponse wrappedElement = new com.amazon.ec2.DeleteNetworkAclResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteRouteResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteRouteResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteRouteResponse wrapDeleteRoute() {
        com.amazon.ec2.DeleteRouteResponse wrappedElement = new com.amazon.ec2.DeleteRouteResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.RebootInstancesResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.RebootInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.RebootInstancesResponse wrapRebootInstances() {
        com.amazon.ec2.RebootInstancesResponse wrappedElement = new com.amazon.ec2.RebootInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ModifyInstanceAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ModifyInstanceAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ModifyInstanceAttributeResponse wrapModifyInstanceAttribute() {
        com.amazon.ec2.ModifyInstanceAttributeResponse wrappedElement = new com.amazon.ec2.ModifyInstanceAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.TerminateInstancesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.TerminateInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.TerminateInstancesResponse wrapTerminateInstances() {
        com.amazon.ec2.TerminateInstancesResponse wrappedElement = new com.amazon.ec2.TerminateInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateVpnConnectionRouteResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateVpnConnectionRouteResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateVpnConnectionRouteResponse wrapCreateVpnConnectionRoute() {
        com.amazon.ec2.CreateVpnConnectionRouteResponse wrappedElement = new com.amazon.ec2.CreateVpnConnectionRouteResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeConversionTasksResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeConversionTasksResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeConversionTasksResponse wrapDescribeConversionTasks() {
        com.amazon.ec2.DescribeConversionTasksResponse wrappedElement = new com.amazon.ec2.DescribeConversionTasksResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeAddressesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeAddressesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeAddressesResponse wrapDescribeAddresses() {
        com.amazon.ec2.DescribeAddressesResponse wrappedElement = new com.amazon.ec2.DescribeAddressesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeInstanceAttributeResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeInstanceAttributeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeInstanceAttributeResponse wrapDescribeInstanceAttribute() {
        com.amazon.ec2.DescribeInstanceAttributeResponse wrappedElement = new com.amazon.ec2.DescribeInstanceAttributeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AttachInternetGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AttachInternetGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AttachInternetGatewayResponse wrapAttachInternetGateway() {
        com.amazon.ec2.AttachInternetGatewayResponse wrappedElement = new com.amazon.ec2.AttachInternetGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateVpcResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateVpcResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateVpcResponse wrapCreateVpc() {
        com.amazon.ec2.CreateVpcResponse wrappedElement = new com.amazon.ec2.CreateVpcResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ReplaceRouteTableAssociationResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ReplaceRouteTableAssociationResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ReplaceRouteTableAssociationResponse wrapReplaceRouteTableAssociation() {
        com.amazon.ec2.ReplaceRouteTableAssociationResponse wrappedElement = new com.amazon.ec2.ReplaceRouteTableAssociationResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AssociateRouteTableResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AssociateRouteTableResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AssociateRouteTableResponse wrapAssociateRouteTable() {
        com.amazon.ec2.AssociateRouteTableResponse wrappedElement = new com.amazon.ec2.AssociateRouteTableResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DetachInternetGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DetachInternetGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DetachInternetGatewayResponse wrapDetachInternetGateway() {
        com.amazon.ec2.DetachInternetGatewayResponse wrappedElement = new com.amazon.ec2.DetachInternetGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.PurchaseReservedInstancesOfferingResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.PurchaseReservedInstancesOfferingResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.PurchaseReservedInstancesOfferingResponse wrapPurchaseReservedInstancesOffering() {
        com.amazon.ec2.PurchaseReservedInstancesOfferingResponse wrappedElement = new com.amazon.ec2.PurchaseReservedInstancesOfferingResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ImportVolumeResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ImportVolumeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ImportVolumeResponse wrapImportVolume() {
        com.amazon.ec2.ImportVolumeResponse wrappedElement = new com.amazon.ec2.ImportVolumeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeExportTasksResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeExportTasksResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeExportTasksResponse wrapDescribeExportTasks() {
        com.amazon.ec2.DescribeExportTasksResponse wrappedElement = new com.amazon.ec2.DescribeExportTasksResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateInstanceExportTaskResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateInstanceExportTaskResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateInstanceExportTaskResponse wrapCreateInstanceExportTask() {
        com.amazon.ec2.CreateInstanceExportTaskResponse wrappedElement = new com.amazon.ec2.CreateInstanceExportTaskResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AssignPrivateIpAddressesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AssignPrivateIpAddressesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AssignPrivateIpAddressesResponse wrapAssignPrivateIpAddresses() {
        com.amazon.ec2.AssignPrivateIpAddressesResponse wrappedElement = new com.amazon.ec2.AssignPrivateIpAddressesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ReportInstanceStatusResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ReportInstanceStatusResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ReportInstanceStatusResponse wrapReportInstanceStatus() {
        com.amazon.ec2.ReportInstanceStatusResponse wrappedElement = new com.amazon.ec2.ReportInstanceStatusResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeReservedInstancesOfferingsResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeReservedInstancesOfferingsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeReservedInstancesOfferingsResponse wrapDescribeReservedInstancesOfferings() {
        com.amazon.ec2.DescribeReservedInstancesOfferingsResponse wrappedElement = new com.amazon.ec2.DescribeReservedInstancesOfferingsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteVpnConnectionResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteVpnConnectionResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteVpnConnectionResponse wrapDeleteVpnConnection() {
        com.amazon.ec2.DeleteVpnConnectionResponse wrappedElement = new com.amazon.ec2.DeleteVpnConnectionResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteInternetGatewayResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteInternetGatewayResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteInternetGatewayResponse wrapDeleteInternetGateway() {
        com.amazon.ec2.DeleteInternetGatewayResponse wrappedElement = new com.amazon.ec2.DeleteInternetGatewayResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse wrapDeleteSpotDatafeedSubscription() {
        com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse wrappedElement = new com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AttachNetworkInterfaceResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AttachNetworkInterfaceResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AttachNetworkInterfaceResponse wrapAttachNetworkInterface() {
        com.amazon.ec2.AttachNetworkInterfaceResponse wrappedElement = new com.amazon.ec2.AttachNetworkInterfaceResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.CreateNetworkInterfaceResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.CreateNetworkInterfaceResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.CreateNetworkInterfaceResponse wrapCreateNetworkInterface() {
        com.amazon.ec2.CreateNetworkInterfaceResponse wrappedElement = new com.amazon.ec2.CreateNetworkInterfaceResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.RevokeSecurityGroupEgressResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.RevokeSecurityGroupEgressResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.RevokeSecurityGroupEgressResponse wrapRevokeSecurityGroupEgress() {
        com.amazon.ec2.RevokeSecurityGroupEgressResponse wrappedElement = new com.amazon.ec2.RevokeSecurityGroupEgressResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.MonitorInstancesResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.MonitorInstancesResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.MonitorInstancesResponse wrapMonitorInstances() {
        com.amazon.ec2.MonitorInstancesResponse wrappedElement = new com.amazon.ec2.MonitorInstancesResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.ReplaceRouteResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.ReplaceRouteResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.ReplaceRouteResponse wrapReplaceRoute() {
        com.amazon.ec2.ReplaceRouteResponse wrappedElement = new com.amazon.ec2.ReplaceRouteResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.AttachVolumeResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.AttachVolumeResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.AttachVolumeResponse wrapAttachVolume() {
        com.amazon.ec2.AttachVolumeResponse wrappedElement = new com.amazon.ec2.AttachVolumeResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.GetConsoleOutputResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.GetConsoleOutputResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.GetConsoleOutputResponse wrapGetConsoleOutput() {
        com.amazon.ec2.GetConsoleOutputResponse wrappedElement = new com.amazon.ec2.GetConsoleOutputResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.ec2.DescribeRegionsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.ec2.DescribeRegionsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.ec2.DescribeRegionsResponse wrapDescribeRegions() {
        com.amazon.ec2.DescribeRegionsResponse wrappedElement = new com.amazon.ec2.DescribeRegionsResponse();
        return wrappedElement;
    }

    /**
    *  get the default envelope
    */
    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory) {
        return factory.getDefaultEnvelope();
    }

    private java.lang.Object fromOM(org.apache.axiom.om.OMElement param, java.lang.Class type, java.util.Map extraNamespaces) throws org.apache.axis2.AxisFault {

        try {

            if (com.amazon.ec2.DescribePlacementGroups.class.equals(type)) {

                return com.amazon.ec2.DescribePlacementGroups.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribePlacementGroupsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribePlacementGroupsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateSecurityGroup.class.equals(type)) {

                return com.amazon.ec2.CreateSecurityGroup.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateSecurityGroupResponse.class.equals(type)) {

                return com.amazon.ec2.CreateSecurityGroupResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ResetNetworkInterfaceAttribute.class.equals(type)) {

                return com.amazon.ec2.ResetNetworkInterfaceAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ResetNetworkInterfaceAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ResetNetworkInterfaceAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateDhcpOptions.class.equals(type)) {

                return com.amazon.ec2.CreateDhcpOptions.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateDhcpOptionsResponse.class.equals(type)) {

                return com.amazon.ec2.CreateDhcpOptionsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateRouteTable.class.equals(type)) {

                return com.amazon.ec2.CreateRouteTable.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateRouteTableResponse.class.equals(type)) {

                return com.amazon.ec2.CreateRouteTableResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSubnets.class.equals(type)) {

                return com.amazon.ec2.DescribeSubnets.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSubnetsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeSubnetsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeactivateLicense.class.equals(type)) {

                return com.amazon.ec2.DeactivateLicense.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeactivateLicenseResponse.class.equals(type)) {

                return com.amazon.ec2.DeactivateLicenseResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVpc.class.equals(type)) {

                return com.amazon.ec2.DeleteVpc.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVpcResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteVpcResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelSpotInstanceRequests.class.equals(type)) {

                return com.amazon.ec2.CancelSpotInstanceRequests.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelSpotInstanceRequestsResponse.class.equals(type)) {

                return com.amazon.ec2.CancelSpotInstanceRequestsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateSubnet.class.equals(type)) {

                return com.amazon.ec2.CreateSubnet.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateSubnetResponse.class.equals(type)) {

                return com.amazon.ec2.CreateSubnetResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVpnGateway.class.equals(type)) {

                return com.amazon.ec2.DeleteVpnGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVpnGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteVpnGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateNetworkAclEntry.class.equals(type)) {

                return com.amazon.ec2.CreateNetworkAclEntry.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateNetworkAclEntryResponse.class.equals(type)) {

                return com.amazon.ec2.CreateNetworkAclEntryResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RequestSpotInstances.class.equals(type)) {

                return com.amazon.ec2.RequestSpotInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RequestSpotInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.RequestSpotInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVolumeAttribute.class.equals(type)) {

                return com.amazon.ec2.DescribeVolumeAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVolumeAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeVolumeAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AssociateDhcpOptions.class.equals(type)) {

                return com.amazon.ec2.AssociateDhcpOptions.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AssociateDhcpOptionsResponse.class.equals(type)) {

                return com.amazon.ec2.AssociateDhcpOptionsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeTags.class.equals(type)) {

                return com.amazon.ec2.DescribeTags.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeTagsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeTagsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ImportKeyPair.class.equals(type)) {

                return com.amazon.ec2.ImportKeyPair.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ImportKeyPairResponse.class.equals(type)) {

                return com.amazon.ec2.ImportKeyPairResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteNetworkInterface.class.equals(type)) {

                return com.amazon.ec2.DeleteNetworkInterface.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteNetworkInterfaceResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteNetworkInterfaceResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVpcs.class.equals(type)) {

                return com.amazon.ec2.DescribeVpcs.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVpcsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeVpcsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeLicenses.class.equals(type)) {

                return com.amazon.ec2.DescribeLicenses.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeLicensesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeLicensesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.BundleInstance.class.equals(type)) {

                return com.amazon.ec2.BundleInstance.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.BundleInstanceResponse.class.equals(type)) {

                return com.amazon.ec2.BundleInstanceResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVpnConnections.class.equals(type)) {

                return com.amazon.ec2.DescribeVpnConnections.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVpnConnectionsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeVpnConnectionsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeImages.class.equals(type)) {

                return com.amazon.ec2.DescribeImages.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeImagesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeImagesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateInternetGateway.class.equals(type)) {

                return com.amazon.ec2.CreateInternetGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateInternetGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.CreateInternetGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DisassociateRouteTable.class.equals(type)) {

                return com.amazon.ec2.DisassociateRouteTable.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DisassociateRouteTableResponse.class.equals(type)) {

                return com.amazon.ec2.DisassociateRouteTableResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReplaceNetworkAclEntry.class.equals(type)) {

                return com.amazon.ec2.ReplaceNetworkAclEntry.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReplaceNetworkAclEntryResponse.class.equals(type)) {

                return com.amazon.ec2.ReplaceNetworkAclEntryResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AuthorizeSecurityGroupIngress.class.equals(type)) {

                return com.amazon.ec2.AuthorizeSecurityGroupIngress.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AuthorizeSecurityGroupIngressResponse.class.equals(type)) {

                return com.amazon.ec2.AuthorizeSecurityGroupIngressResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSnapshotAttribute.class.equals(type)) {

                return com.amazon.ec2.DescribeSnapshotAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSnapshotAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeSnapshotAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVpnGateway.class.equals(type)) {

                return com.amazon.ec2.CreateVpnGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVpnGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.CreateVpnGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ResetInstanceAttribute.class.equals(type)) {

                return com.amazon.ec2.ResetInstanceAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ResetInstanceAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ResetInstanceAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateTags.class.equals(type)) {

                return com.amazon.ec2.CreateTags.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateTagsResponse.class.equals(type)) {

                return com.amazon.ec2.CreateTagsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AuthorizeSecurityGroupEgress.class.equals(type)) {

                return com.amazon.ec2.AuthorizeSecurityGroupEgress.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AuthorizeSecurityGroupEgressResponse.class.equals(type)) {

                return com.amazon.ec2.AuthorizeSecurityGroupEgressResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AssociateAddress.class.equals(type)) {

                return com.amazon.ec2.AssociateAddress.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AssociateAddressResponse.class.equals(type)) {

                return com.amazon.ec2.AssociateAddressResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeImageAttribute.class.equals(type)) {

                return com.amazon.ec2.DescribeImageAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeImageAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeImageAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSpotPriceHistory.class.equals(type)) {

                return com.amazon.ec2.DescribeSpotPriceHistory.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSpotPriceHistoryResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeSpotPriceHistoryResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifySnapshotAttribute.class.equals(type)) {

                return com.amazon.ec2.ModifySnapshotAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifySnapshotAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ModifySnapshotAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSpotInstanceRequests.class.equals(type)) {

                return com.amazon.ec2.DescribeSpotInstanceRequests.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSpotInstanceRequestsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeSpotInstanceRequestsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ImportInstance.class.equals(type)) {

                return com.amazon.ec2.ImportInstance.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ImportInstanceResponse.class.equals(type)) {

                return com.amazon.ec2.ImportInstanceResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeKeyPairs.class.equals(type)) {

                return com.amazon.ec2.DescribeKeyPairs.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeKeyPairsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeKeyPairsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RevokeSecurityGroupIngress.class.equals(type)) {

                return com.amazon.ec2.RevokeSecurityGroupIngress.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RevokeSecurityGroupIngressResponse.class.equals(type)) {

                return com.amazon.ec2.RevokeSecurityGroupIngressResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreatePlacementGroup.class.equals(type)) {

                return com.amazon.ec2.CreatePlacementGroup.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreatePlacementGroupResponse.class.equals(type)) {

                return com.amazon.ec2.CreatePlacementGroupResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteNetworkAclEntry.class.equals(type)) {

                return com.amazon.ec2.DeleteNetworkAclEntry.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteNetworkAclEntryResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteNetworkAclEntryResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ActivateLicense.class.equals(type)) {

                return com.amazon.ec2.ActivateLicense.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ActivateLicenseResponse.class.equals(type)) {

                return com.amazon.ec2.ActivateLicenseResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteRouteTable.class.equals(type)) {

                return com.amazon.ec2.DeleteRouteTable.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteRouteTableResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteRouteTableResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.UnmonitorInstances.class.equals(type)) {

                return com.amazon.ec2.UnmonitorInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.UnmonitorInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.UnmonitorInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.StartInstances.class.equals(type)) {

                return com.amazon.ec2.StartInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.StartInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.StartInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ConfirmProductInstance.class.equals(type)) {

                return com.amazon.ec2.ConfirmProductInstance.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ConfirmProductInstanceResponse.class.equals(type)) {

                return com.amazon.ec2.ConfirmProductInstanceResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeNetworkInterfaceAttribute.class.equals(type)) {

                return com.amazon.ec2.DescribeNetworkInterfaceAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeNetworkInterfaceAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RunInstances.class.equals(type)) {

                return com.amazon.ec2.RunInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RunInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.RunInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateReservedInstancesListing.class.equals(type)) {

                return com.amazon.ec2.CreateReservedInstancesListing.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateReservedInstancesListingResponse.class.equals(type)) {

                return com.amazon.ec2.CreateReservedInstancesListingResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateCustomerGateway.class.equals(type)) {

                return com.amazon.ec2.CreateCustomerGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateCustomerGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.CreateCustomerGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateNetworkAcl.class.equals(type)) {

                return com.amazon.ec2.CreateNetworkAcl.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateNetworkAclResponse.class.equals(type)) {

                return com.amazon.ec2.CreateNetworkAclResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ResetImageAttribute.class.equals(type)) {

                return com.amazon.ec2.ResetImageAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ResetImageAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ResetImageAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifyVolumeAttribute.class.equals(type)) {

                return com.amazon.ec2.ModifyVolumeAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifyVolumeAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ModifyVolumeAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeReservedInstances.class.equals(type)) {

                return com.amazon.ec2.DescribeReservedInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeReservedInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeReservedInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ResetSnapshotAttribute.class.equals(type)) {

                return com.amazon.ec2.ResetSnapshotAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ResetSnapshotAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ResetSnapshotAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVolume.class.equals(type)) {

                return com.amazon.ec2.DeleteVolume.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVolumeResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteVolumeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeAvailabilityZones.class.equals(type)) {

                return com.amazon.ec2.DescribeAvailabilityZones.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeAvailabilityZonesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeAvailabilityZonesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVpnConnection.class.equals(type)) {

                return com.amazon.ec2.CreateVpnConnection.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVpnConnectionResponse.class.equals(type)) {

                return com.amazon.ec2.CreateVpnConnectionResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelBundleTask.class.equals(type)) {

                return com.amazon.ec2.CancelBundleTask.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelBundleTaskResponse.class.equals(type)) {

                return com.amazon.ec2.CancelBundleTaskResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReplaceNetworkAclAssociation.class.equals(type)) {

                return com.amazon.ec2.ReplaceNetworkAclAssociation.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReplaceNetworkAclAssociationResponse.class.equals(type)) {

                return com.amazon.ec2.ReplaceNetworkAclAssociationResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DetachVpnGateway.class.equals(type)) {

                return com.amazon.ec2.DetachVpnGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DetachVpnGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.DetachVpnGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSnapshots.class.equals(type)) {

                return com.amazon.ec2.DescribeSnapshots.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSnapshotsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeSnapshotsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteSubnet.class.equals(type)) {

                return com.amazon.ec2.DeleteSubnet.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteSubnetResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteSubnetResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeBundleTasks.class.equals(type)) {

                return com.amazon.ec2.DescribeBundleTasks.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeBundleTasksResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeBundleTasksResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateKeyPair.class.equals(type)) {

                return com.amazon.ec2.CreateKeyPair.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateKeyPairResponse.class.equals(type)) {

                return com.amazon.ec2.CreateKeyPairResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateImage.class.equals(type)) {

                return com.amazon.ec2.CreateImage.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateImageResponse.class.equals(type)) {

                return com.amazon.ec2.CreateImageResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.EnableVgwRoutePropagation.class.equals(type)) {

                return com.amazon.ec2.EnableVgwRoutePropagation.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.EnableVgwRoutePropagationResponse.class.equals(type)) {

                return com.amazon.ec2.EnableVgwRoutePropagationResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.UnassignPrivateIpAddresses.class.equals(type)) {

                return com.amazon.ec2.UnassignPrivateIpAddresses.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.UnassignPrivateIpAddressesResponse.class.equals(type)) {

                return com.amazon.ec2.UnassignPrivateIpAddressesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeregisterImage.class.equals(type)) {

                return com.amazon.ec2.DeregisterImage.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeregisterImageResponse.class.equals(type)) {

                return com.amazon.ec2.DeregisterImageResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVpnConnectionRoute.class.equals(type)) {

                return com.amazon.ec2.DeleteVpnConnectionRoute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVpnConnectionRouteResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteVpnConnectionRouteResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifyImageAttribute.class.equals(type)) {

                return com.amazon.ec2.ModifyImageAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifyImageAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ModifyImageAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelConversionTask.class.equals(type)) {

                return com.amazon.ec2.CancelConversionTask.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelConversionTaskResponse.class.equals(type)) {

                return com.amazon.ec2.CancelConversionTaskResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVolumes.class.equals(type)) {

                return com.amazon.ec2.DescribeVolumes.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVolumesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeVolumesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelReservedInstancesListing.class.equals(type)) {

                return com.amazon.ec2.CancelReservedInstancesListing.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelReservedInstancesListingResponse.class.equals(type)) {

                return com.amazon.ec2.CancelReservedInstancesListingResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.GetPasswordData.class.equals(type)) {

                return com.amazon.ec2.GetPasswordData.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.GetPasswordDataResponse.class.equals(type)) {

                return com.amazon.ec2.GetPasswordDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AllocateAddress.class.equals(type)) {

                return com.amazon.ec2.AllocateAddress.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AllocateAddressResponse.class.equals(type)) {

                return com.amazon.ec2.AllocateAddressResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteSecurityGroup.class.equals(type)) {

                return com.amazon.ec2.DeleteSecurityGroup.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteSecurityGroupResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteSecurityGroupResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeletePlacementGroup.class.equals(type)) {

                return com.amazon.ec2.DeletePlacementGroup.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeletePlacementGroupResponse.class.equals(type)) {

                return com.amazon.ec2.DeletePlacementGroupResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DisassociateAddress.class.equals(type)) {

                return com.amazon.ec2.DisassociateAddress.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DisassociateAddressResponse.class.equals(type)) {

                return com.amazon.ec2.DisassociateAddressResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteDhcpOptions.class.equals(type)) {

                return com.amazon.ec2.DeleteDhcpOptions.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteDhcpOptionsResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteDhcpOptionsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSpotDatafeedSubscription.class.equals(type)) {

                return com.amazon.ec2.DescribeSpotDatafeedSubscription.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeSpotDatafeedSubscriptionResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeNetworkAcls.class.equals(type)) {

                return com.amazon.ec2.DescribeNetworkAcls.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeNetworkAclsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeNetworkAclsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.EnableVolumeIO.class.equals(type)) {

                return com.amazon.ec2.EnableVolumeIO.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.EnableVolumeIOResponse.class.equals(type)) {

                return com.amazon.ec2.EnableVolumeIOResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AttachVpnGateway.class.equals(type)) {

                return com.amazon.ec2.AttachVpnGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AttachVpnGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.AttachVpnGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeInternetGateways.class.equals(type)) {

                return com.amazon.ec2.DescribeInternetGateways.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeInternetGatewaysResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeInternetGatewaysResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeDhcpOptions.class.equals(type)) {

                return com.amazon.ec2.DescribeDhcpOptions.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeDhcpOptionsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeDhcpOptionsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateSpotDatafeedSubscription.class.equals(type)) {

                return com.amazon.ec2.CreateSpotDatafeedSubscription.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse.class.equals(type)) {

                return com.amazon.ec2.CreateSpotDatafeedSubscriptionResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeReservedInstancesListings.class.equals(type)) {

                return com.amazon.ec2.DescribeReservedInstancesListings.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeReservedInstancesListingsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeReservedInstancesListingsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeInstanceStatus.class.equals(type)) {

                return com.amazon.ec2.DescribeInstanceStatus.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeInstanceStatusResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeInstanceStatusResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifyNetworkInterfaceAttribute.class.equals(type)) {

                return com.amazon.ec2.ModifyNetworkInterfaceAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ModifyNetworkInterfaceAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DisableVgwRoutePropagation.class.equals(type)) {

                return com.amazon.ec2.DisableVgwRoutePropagation.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DisableVgwRoutePropagationResponse.class.equals(type)) {

                return com.amazon.ec2.DisableVgwRoutePropagationResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVolumeStatus.class.equals(type)) {

                return com.amazon.ec2.DescribeVolumeStatus.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVolumeStatusResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeVolumeStatusResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DetachNetworkInterface.class.equals(type)) {

                return com.amazon.ec2.DetachNetworkInterface.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DetachNetworkInterfaceResponse.class.equals(type)) {

                return com.amazon.ec2.DetachNetworkInterfaceResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSecurityGroups.class.equals(type)) {

                return com.amazon.ec2.DescribeSecurityGroups.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeSecurityGroupsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeSecurityGroupsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeInstances.class.equals(type)) {

                return com.amazon.ec2.DescribeInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeNetworkInterfaces.class.equals(type)) {

                return com.amazon.ec2.DescribeNetworkInterfaces.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeNetworkInterfacesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeNetworkInterfacesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteKeyPair.class.equals(type)) {

                return com.amazon.ec2.DeleteKeyPair.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteKeyPairResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteKeyPairResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateSnapshot.class.equals(type)) {

                return com.amazon.ec2.CreateSnapshot.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateSnapshotResponse.class.equals(type)) {

                return com.amazon.ec2.CreateSnapshotResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVpnGateways.class.equals(type)) {

                return com.amazon.ec2.DescribeVpnGateways.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeVpnGatewaysResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeVpnGatewaysResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteTags.class.equals(type)) {

                return com.amazon.ec2.DeleteTags.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteTagsResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteTagsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteSnapshot.class.equals(type)) {

                return com.amazon.ec2.DeleteSnapshot.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteSnapshotResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteSnapshotResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteCustomerGateway.class.equals(type)) {

                return com.amazon.ec2.DeleteCustomerGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteCustomerGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteCustomerGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVolume.class.equals(type)) {

                return com.amazon.ec2.CreateVolume.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVolumeResponse.class.equals(type)) {

                return com.amazon.ec2.CreateVolumeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelExportTask.class.equals(type)) {

                return com.amazon.ec2.CancelExportTask.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CancelExportTaskResponse.class.equals(type)) {

                return com.amazon.ec2.CancelExportTaskResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RegisterImage.class.equals(type)) {

                return com.amazon.ec2.RegisterImage.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RegisterImageResponse.class.equals(type)) {

                return com.amazon.ec2.RegisterImageResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DetachVolume.class.equals(type)) {

                return com.amazon.ec2.DetachVolume.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DetachVolumeResponse.class.equals(type)) {

                return com.amazon.ec2.DetachVolumeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.StopInstances.class.equals(type)) {

                return com.amazon.ec2.StopInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.StopInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.StopInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateRoute.class.equals(type)) {

                return com.amazon.ec2.CreateRoute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateRouteResponse.class.equals(type)) {

                return com.amazon.ec2.CreateRouteResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReleaseAddress.class.equals(type)) {

                return com.amazon.ec2.ReleaseAddress.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReleaseAddressResponse.class.equals(type)) {

                return com.amazon.ec2.ReleaseAddressResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeRouteTables.class.equals(type)) {

                return com.amazon.ec2.DescribeRouteTables.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeRouteTablesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeRouteTablesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeCustomerGateways.class.equals(type)) {

                return com.amazon.ec2.DescribeCustomerGateways.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeCustomerGatewaysResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeCustomerGatewaysResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteNetworkAcl.class.equals(type)) {

                return com.amazon.ec2.DeleteNetworkAcl.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteNetworkAclResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteNetworkAclResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteRoute.class.equals(type)) {

                return com.amazon.ec2.DeleteRoute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteRouteResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteRouteResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RebootInstances.class.equals(type)) {

                return com.amazon.ec2.RebootInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RebootInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.RebootInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifyInstanceAttribute.class.equals(type)) {

                return com.amazon.ec2.ModifyInstanceAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ModifyInstanceAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.ModifyInstanceAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.TerminateInstances.class.equals(type)) {

                return com.amazon.ec2.TerminateInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.TerminateInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.TerminateInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVpnConnectionRoute.class.equals(type)) {

                return com.amazon.ec2.CreateVpnConnectionRoute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVpnConnectionRouteResponse.class.equals(type)) {

                return com.amazon.ec2.CreateVpnConnectionRouteResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeConversionTasks.class.equals(type)) {

                return com.amazon.ec2.DescribeConversionTasks.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeConversionTasksResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeConversionTasksResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeAddresses.class.equals(type)) {

                return com.amazon.ec2.DescribeAddresses.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeAddressesResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeAddressesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeInstanceAttribute.class.equals(type)) {

                return com.amazon.ec2.DescribeInstanceAttribute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeInstanceAttributeResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeInstanceAttributeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AttachInternetGateway.class.equals(type)) {

                return com.amazon.ec2.AttachInternetGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AttachInternetGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.AttachInternetGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVpc.class.equals(type)) {

                return com.amazon.ec2.CreateVpc.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateVpcResponse.class.equals(type)) {

                return com.amazon.ec2.CreateVpcResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReplaceRouteTableAssociation.class.equals(type)) {

                return com.amazon.ec2.ReplaceRouteTableAssociation.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReplaceRouteTableAssociationResponse.class.equals(type)) {

                return com.amazon.ec2.ReplaceRouteTableAssociationResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AssociateRouteTable.class.equals(type)) {

                return com.amazon.ec2.AssociateRouteTable.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AssociateRouteTableResponse.class.equals(type)) {

                return com.amazon.ec2.AssociateRouteTableResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DetachInternetGateway.class.equals(type)) {

                return com.amazon.ec2.DetachInternetGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DetachInternetGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.DetachInternetGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.PurchaseReservedInstancesOffering.class.equals(type)) {

                return com.amazon.ec2.PurchaseReservedInstancesOffering.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.PurchaseReservedInstancesOfferingResponse.class.equals(type)) {

                return com.amazon.ec2.PurchaseReservedInstancesOfferingResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ImportVolume.class.equals(type)) {

                return com.amazon.ec2.ImportVolume.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ImportVolumeResponse.class.equals(type)) {

                return com.amazon.ec2.ImportVolumeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeExportTasks.class.equals(type)) {

                return com.amazon.ec2.DescribeExportTasks.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeExportTasksResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeExportTasksResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateInstanceExportTask.class.equals(type)) {

                return com.amazon.ec2.CreateInstanceExportTask.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateInstanceExportTaskResponse.class.equals(type)) {

                return com.amazon.ec2.CreateInstanceExportTaskResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AssignPrivateIpAddresses.class.equals(type)) {

                return com.amazon.ec2.AssignPrivateIpAddresses.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AssignPrivateIpAddressesResponse.class.equals(type)) {

                return com.amazon.ec2.AssignPrivateIpAddressesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReportInstanceStatus.class.equals(type)) {

                return com.amazon.ec2.ReportInstanceStatus.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReportInstanceStatusResponse.class.equals(type)) {

                return com.amazon.ec2.ReportInstanceStatusResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeReservedInstancesOfferings.class.equals(type)) {

                return com.amazon.ec2.DescribeReservedInstancesOfferings.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeReservedInstancesOfferingsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeReservedInstancesOfferingsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVpnConnection.class.equals(type)) {

                return com.amazon.ec2.DeleteVpnConnection.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteVpnConnectionResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteVpnConnectionResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteInternetGateway.class.equals(type)) {

                return com.amazon.ec2.DeleteInternetGateway.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteInternetGatewayResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteInternetGatewayResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteSpotDatafeedSubscription.class.equals(type)) {

                return com.amazon.ec2.DeleteSpotDatafeedSubscription.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse.class.equals(type)) {

                return com.amazon.ec2.DeleteSpotDatafeedSubscriptionResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AttachNetworkInterface.class.equals(type)) {

                return com.amazon.ec2.AttachNetworkInterface.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AttachNetworkInterfaceResponse.class.equals(type)) {

                return com.amazon.ec2.AttachNetworkInterfaceResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateNetworkInterface.class.equals(type)) {

                return com.amazon.ec2.CreateNetworkInterface.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.CreateNetworkInterfaceResponse.class.equals(type)) {

                return com.amazon.ec2.CreateNetworkInterfaceResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RevokeSecurityGroupEgress.class.equals(type)) {

                return com.amazon.ec2.RevokeSecurityGroupEgress.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.RevokeSecurityGroupEgressResponse.class.equals(type)) {

                return com.amazon.ec2.RevokeSecurityGroupEgressResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.MonitorInstances.class.equals(type)) {

                return com.amazon.ec2.MonitorInstances.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.MonitorInstancesResponse.class.equals(type)) {

                return com.amazon.ec2.MonitorInstancesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReplaceRoute.class.equals(type)) {

                return com.amazon.ec2.ReplaceRoute.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.ReplaceRouteResponse.class.equals(type)) {

                return com.amazon.ec2.ReplaceRouteResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AttachVolume.class.equals(type)) {

                return com.amazon.ec2.AttachVolume.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.AttachVolumeResponse.class.equals(type)) {

                return com.amazon.ec2.AttachVolumeResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.GetConsoleOutput.class.equals(type)) {

                return com.amazon.ec2.GetConsoleOutput.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.GetConsoleOutputResponse.class.equals(type)) {

                return com.amazon.ec2.GetConsoleOutputResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeRegions.class.equals(type)) {

                return com.amazon.ec2.DescribeRegions.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.ec2.DescribeRegionsResponse.class.equals(type)) {

                return com.amazon.ec2.DescribeRegionsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

        } catch (java.lang.Exception e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
        return null;
    }

    /**
    *  A utility method that copies the namepaces from the SOAPEnvelope
    */
    private java.util.Map getEnvelopeNamespaces(org.apache.axiom.soap.SOAPEnvelope env) {
        java.util.Map returnMap = new java.util.HashMap();
        java.util.Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
            org.apache.axiom.om.OMNamespace ns = (org.apache.axiom.om.OMNamespace)namespaceIterator.next();
            returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
        }
        return returnMap;
    }

    private org.apache.axis2.AxisFault createAxisFault(java.lang.Exception e) {
        org.apache.axis2.AxisFault f;
        Throwable cause = e.getCause();
        if (cause != null) {
            f = new org.apache.axis2.AxisFault(e.getMessage(), cause);
        } else {
            f = new org.apache.axis2.AxisFault(e.getMessage());
        }

        return f;
    }

}//end of class
