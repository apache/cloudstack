<?xml version="1.0"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="1.0">
<xsl:template match="/">
<html>
<head><title>Cloudstack API</title></head>
<body>
<table border="1" cellpadding="1" cellspacing="0.5">
<tr>
<h1 ALIGN='CENTER'>Cloudstack API Version 4.6.0</h1>
<br/>
<h2 ALIGN='CENTER'>Table of Contents</h2>
<th><h3>Name</h3></th>
<th><h3>Description</h3></th>
</tr>
<xsl:for-each select="commands/command">
<tr>
<xsl:if test="name='updateAccount'">
<td><A HREF="/home/aj/workspace/xxml/xml/commands.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteDiskOffering'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateDiskOffering'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listOsTypes'">
<tr>
<td><h4 ALIGN='CENTER'>OS Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>OS Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listLoadBalancerRuleInstances'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listIsos'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createDomain'">
<tr>
<td><h4 ALIGN='CENTER'>Domain Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Domain Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='extractVolume'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deployVirtualMachine'">
<tr>
<td><h4 ALIGN='CENTER'>Virtual Machine Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Virtual Machine Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listIsoPermissions'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listUsers'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listHypervisors'">
<tr>
<td><h4 ALIGN='CENTER'>Hypervisor Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Hypervisor Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='registerPreallocatedLun'">
<tr>
<td><h4 ALIGN='CENTER'>Lun Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Lun Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='extractTemplate'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='prepareHostForMaintenance'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listRouters'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteVlanIpRange'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listTemplates'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='authorizeSecurityGroupIngress'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='detachIso'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createTemplate'">
<tr>
<td><h4 ALIGN='CENTER'>Template Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Template Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='cancelHostMaintenance'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateVirtualMachine'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createConfiguration'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listStoragePools'">
<tr>
<td><h4 ALIGN='CENTER'>Storage Pool Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Storage Pool Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteServiceOffering'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listPortForwardingRules'">
<tr>
<td><h4 ALIGN='CENTER'>Firewall Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Firewall Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='enableStorageMaintenance'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='copyIso'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listTemplatePermissions'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createInstanceGroup'">
<tr>
<td><h4 ALIGN='CENTER'>VM Group Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>VM Group Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listZones'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateZone'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listServiceOfferings'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='copyTemplate'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createVlanIpRange'">
<tr>
<td><h4 ALIGN='CENTER'>Vlan Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Vlan Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listDomainChildren'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteZone'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='rebootVirtualMachine'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='addSecondaryStorage'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='startRouter'">
<tr>
<td><h4 ALIGN='CENTER'>Router Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Router Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createServiceOffering'">
<tr>
<td><h4 ALIGN='CENTER'>Service Offering Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Service Offering Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateServiceOffering'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createNetwork'">
<tr>
<td><h4 ALIGN='CENTER'>Network Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Network Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listSecurityGroups'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='stopVirtualMachine'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listVlanIpRanges'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='restartNetwork'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listInstanceGroups'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='disableUser'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='rebootRouter'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='reconnectHost'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='destroyVirtualMachine'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='rebootSystemVm'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='removeVpnUser'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteStoragePool'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteVolume'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateConfiguration'">
<tr>
<td><h4 ALIGN='CENTER'>Configuration Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Configuration Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='assignToLoadBalancerRule'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listOsCategories'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createAccount'">
<tr>
<td><h4 ALIGN='CENTER'>Account Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Account Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listAccounts'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='registerUserKeys'">
<tr>
<td><h4 ALIGN='CENTER'>Registration Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Registration Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateLoadBalancerRule'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteSnapshot'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createZone'">
<tr>
<td><h4 ALIGN='CENTER'>Zone Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Zone Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createSecurityGroup'">
<tr>
<td><h4 ALIGN='CENTER'>Security Group Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Security Group Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteNetwork'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listAlerts'">
<tr>
<td><h4 ALIGN='CENTER'>Alert Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Alert Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='stopSystemVm'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteDomain'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listVirtualMachines'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateInstanceGroup'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createPod'">
<tr>
<td><h4 ALIGN='CENTER'>Pod Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Pod Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='recoverVirtualMachine'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='queryAsyncJobResult'">
<tr>
<td><h4 ALIGN='CENTER'>Async job Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Async job Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteAccount'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateResourceLimit'">
<tr>
<td><h4 ALIGN='CENTER'>Resource limit Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Resource limit Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listLoadBalancerRules'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listPods'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteLoadBalancerRule'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listNetworkOfferings'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteRemoteAccessVpn'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listIpForwardingRules'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listPreallocatedLuns'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listSnapshotPolicies'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='getCloudIdentifier'">
<tr>
<td><h4 ALIGN='CENTER'>Cloudstack Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Cloudstack Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='removeFromLoadBalancerRule'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='resetPasswordForVirtualMachine'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createStoragePool'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateNetworkOffering'">
<tr>
<td><h4 ALIGN='CENTER'>Network Offering Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Network Offering Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateIsoPermissions'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='addVpnUser'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listResourceLimits'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listHosts'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='attachVolume'">
<tr>
<td><h4 ALIGN='CENTER'>Volume Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Volume Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteSnapshotPolicies'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='cancelStorageMaintenance'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listPublicIpAddresses'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateTemplatePermissions'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='enableUser'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='registerTemplate'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='associateIpAddress'">
<tr>
<td><h4 ALIGN='CENTER'>Ip Address Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Ip Address Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='enableAccount'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listCapabilities'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createRemoteAccessVpn'">
<tr>
<td><h4 ALIGN='CENTER'>VPN Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>VPN Address Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listEvents'">
<tr>
<td><h4 ALIGN='CENTER'>Event Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Event Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listSnapshots'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createPortForwardingRule'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listConfigurations'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='detachVolume'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteTemplate'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='registerIso'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createLoadBalancerRule'">
<tr>
<td><h4 ALIGN='CENTER'>Load Balancer Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Load Balancer Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updatePod'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listVolumes'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='stopRouter'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listAsyncJobs'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listNetworks'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listDiskOfferings'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deletePreallocatedLun'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteIpForwardingRule'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='changeServiceForVirtualMachine'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteCluster'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='attachIso'">
<tr>
<td><h4 ALIGN='CENTER'>Iso Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Iso Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='changeServiceForRouter'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listRemoteAccessVpns'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='disassociateIpAddress'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listClusters'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listDomains'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deletePod'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='disableAccount'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteInstanceGroup'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='uploadCustomCertificate'">
<tr>
<td><h4 ALIGN='CENTER'>Certificate Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Certificate Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateUser'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listVpnUsers'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='addHost'">
<tr>
<td><h4 ALIGN='CENTER'>Host Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Host Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createSnapshot'">
<tr>
<td><h4 ALIGN='CENTER'>Snapshot Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Snapshot Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteUser'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listSystemVms'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='extractIso'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateTemplate'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateHost'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateDomain'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='revokeSecurityGroupIngress'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createDiskOffering'">
<tr>
<td><h4 ALIGN='CENTER'>Disk Offering Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Disk Offering Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='updateIso'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createSnapshotPolicy'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createIpForwardingRule'">
<tr>
<td><h4 ALIGN='CENTER'>NAT Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>NAT Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteSecurityGroup'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='listCapacity'">
<tr>
<td><h4 ALIGN='CENTER'>Capacity Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>Capacity Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteHost'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createUser'">
<tr>
<td><h4 ALIGN='CENTER'>User Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>User Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='startSystemVm'">
<tr>
<td><h4 ALIGN='CENTER'>System VM Specific Commands</h4><br></br></td>
<td><h4 ALIGN='CENTER'>System VM Specific Command Descriptions</h4><br></br></td>
</tr>
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='addCluster'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deletePortForwardingRule'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='createVolume'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='deleteIso'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
<tr>
<xsl:if test="name='startVirtualMachine'">
<td><A HREF="resumepage.html"><xsl:value-of select="name"/></A></td>
<td><xsl:value-of select="description"/></td>
</xsl:if>
</tr>
</xsl:for-each>
</table>
</body></html>
</xsl:template>
</xsl:stylesheet>

