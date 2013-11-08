# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

"""Test Client for CloudStack API"""
import copy
from createAccount import createAccountResponse
from deleteAccount import deleteAccountResponse
from updateAccount import updateAccountResponse
from disableAccount import disableAccountResponse
from enableAccount import enableAccountResponse
from lockAccount import lockAccountResponse
from listAccounts import listAccountsResponse
from markDefaultZoneForAccount import markDefaultZoneForAccountResponse
from createUser import createUserResponse
from deleteUser import deleteUserResponse
from updateUser import updateUserResponse
from listUsers import listUsersResponse
from lockUser import lockUserResponse
from disableUser import disableUserResponse
from enableUser import enableUserResponse
from getUser import getUserResponse
from createDomain import createDomainResponse
from updateDomain import updateDomainResponse
from deleteDomain import deleteDomainResponse
from listDomains import listDomainsResponse
from listDomainChildren import listDomainChildrenResponse
from getCloudIdentifier import getCloudIdentifierResponse
from updateResourceLimit import updateResourceLimitResponse
from updateResourceCount import updateResourceCountResponse
from listResourceLimits import listResourceLimitsResponse
from deployVirtualMachine import deployVirtualMachineResponse
from destroyVirtualMachine import destroyVirtualMachineResponse
from rebootVirtualMachine import rebootVirtualMachineResponse
from startVirtualMachine import startVirtualMachineResponse
from stopVirtualMachine import stopVirtualMachineResponse
from resetPasswordForVirtualMachine import resetPasswordForVirtualMachineResponse
from resetSSHKeyForVirtualMachine import resetSSHKeyForVirtualMachineResponse
from updateVirtualMachine import updateVirtualMachineResponse
from listVirtualMachines import listVirtualMachinesResponse
from getVMPassword import getVMPasswordResponse
from restoreVirtualMachine import restoreVirtualMachineResponse
from changeServiceForVirtualMachine import changeServiceForVirtualMachineResponse
from scaleVirtualMachine import scaleVirtualMachineResponse
from assignVirtualMachine import assignVirtualMachineResponse
from migrateVirtualMachine import migrateVirtualMachineResponse
from migrateVirtualMachineWithVolume import migrateVirtualMachineWithVolumeResponse
from recoverVirtualMachine import recoverVirtualMachineResponse
from createSnapshot import createSnapshotResponse
from listSnapshots import listSnapshotsResponse
from deleteSnapshot import deleteSnapshotResponse
from createSnapshotPolicy import createSnapshotPolicyResponse
from deleteSnapshotPolicies import deleteSnapshotPoliciesResponse
from listSnapshotPolicies import listSnapshotPoliciesResponse
from revertSnapshot import revertSnapshotResponse
from createTemplate import createTemplateResponse
from registerTemplate import registerTemplateResponse
from updateTemplate import updateTemplateResponse
from copyTemplate import copyTemplateResponse
from deleteTemplate import deleteTemplateResponse
from listTemplates import listTemplatesResponse
from updateTemplatePermissions import updateTemplatePermissionsResponse
from listTemplatePermissions import listTemplatePermissionsResponse
from extractTemplate import extractTemplateResponse
from prepareTemplate import prepareTemplateResponse
from attachIso import attachIsoResponse
from detachIso import detachIsoResponse
from listIsos import listIsosResponse
from registerIso import registerIsoResponse
from updateIso import updateIsoResponse
from deleteIso import deleteIsoResponse
from copyIso import copyIsoResponse
from updateIsoPermissions import updateIsoPermissionsResponse
from listIsoPermissions import listIsoPermissionsResponse
from extractIso import extractIsoResponse
from listOsTypes import listOsTypesResponse
from listOsCategories import listOsCategoriesResponse
from createServiceOffering import createServiceOfferingResponse
from deleteServiceOffering import deleteServiceOfferingResponse
from updateServiceOffering import updateServiceOfferingResponse
from listServiceOfferings import listServiceOfferingsResponse
from createDiskOffering import createDiskOfferingResponse
from updateDiskOffering import updateDiskOfferingResponse
from deleteDiskOffering import deleteDiskOfferingResponse
from listDiskOfferings import listDiskOfferingsResponse
from createVlanIpRange import createVlanIpRangeResponse
from deleteVlanIpRange import deleteVlanIpRangeResponse
from listVlanIpRanges import listVlanIpRangesResponse
from dedicatePublicIpRange import dedicatePublicIpRangeResponse
from releasePublicIpRange import releasePublicIpRangeResponse
from dedicateGuestVlanRange import dedicateGuestVlanRangeResponse
from releaseDedicatedGuestVlanRange import releaseDedicatedGuestVlanRangeResponse
from listDedicatedGuestVlanRanges import listDedicatedGuestVlanRangesResponse
from associateIpAddress import associateIpAddressResponse
from disassociateIpAddress import disassociateIpAddressResponse
from listPublicIpAddresses import listPublicIpAddressesResponse
from listPortForwardingRules import listPortForwardingRulesResponse
from createPortForwardingRule import createPortForwardingRuleResponse
from deletePortForwardingRule import deletePortForwardingRuleResponse
from updatePortForwardingRule import updatePortForwardingRuleResponse
from enableStaticNat import enableStaticNatResponse
from createIpForwardingRule import createIpForwardingRuleResponse
from deleteIpForwardingRule import deleteIpForwardingRuleResponse
from listIpForwardingRules import listIpForwardingRulesResponse
from disableStaticNat import disableStaticNatResponse
from createLoadBalancerRule import createLoadBalancerRuleResponse
from deleteLoadBalancerRule import deleteLoadBalancerRuleResponse
from removeFromLoadBalancerRule import removeFromLoadBalancerRuleResponse
from assignToLoadBalancerRule import assignToLoadBalancerRuleResponse
from createLBStickinessPolicy import createLBStickinessPolicyResponse
from deleteLBStickinessPolicy import deleteLBStickinessPolicyResponse
from listLoadBalancerRules import listLoadBalancerRulesResponse
from listLBStickinessPolicies import listLBStickinessPoliciesResponse
from listLBHealthCheckPolicies import listLBHealthCheckPoliciesResponse
from createLBHealthCheckPolicy import createLBHealthCheckPolicyResponse
from deleteLBHealthCheckPolicy import deleteLBHealthCheckPolicyResponse
from listLoadBalancerRuleInstances import listLoadBalancerRuleInstancesResponse
from updateLoadBalancerRule import updateLoadBalancerRuleResponse
from createCounter import createCounterResponse
from createCondition import createConditionResponse
from createAutoScalePolicy import createAutoScalePolicyResponse
from createAutoScaleVmProfile import createAutoScaleVmProfileResponse
from createAutoScaleVmGroup import createAutoScaleVmGroupResponse
from deleteCounter import deleteCounterResponse
from deleteCondition import deleteConditionResponse
from deleteAutoScalePolicy import deleteAutoScalePolicyResponse
from deleteAutoScaleVmProfile import deleteAutoScaleVmProfileResponse
from deleteAutoScaleVmGroup import deleteAutoScaleVmGroupResponse
from listCounters import listCountersResponse
from listConditions import listConditionsResponse
from listAutoScalePolicies import listAutoScalePoliciesResponse
from listAutoScaleVmProfiles import listAutoScaleVmProfilesResponse
from listAutoScaleVmGroups import listAutoScaleVmGroupsResponse
from enableAutoScaleVmGroup import enableAutoScaleVmGroupResponse
from disableAutoScaleVmGroup import disableAutoScaleVmGroupResponse
from updateAutoScalePolicy import updateAutoScalePolicyResponse
from updateAutoScaleVmProfile import updateAutoScaleVmProfileResponse
from updateAutoScaleVmGroup import updateAutoScaleVmGroupResponse
from startRouter import startRouterResponse
from rebootRouter import rebootRouterResponse
from stopRouter import stopRouterResponse
from destroyRouter import destroyRouterResponse
from changeServiceForRouter import changeServiceForRouterResponse
from listRouters import listRoutersResponse
from listVirtualRouterElements import listVirtualRouterElementsResponse
from configureVirtualRouterElement import configureVirtualRouterElementResponse
from createVirtualRouterElement import createVirtualRouterElementResponse
from startSystemVm import startSystemVmResponse
from rebootSystemVm import rebootSystemVmResponse
from stopSystemVm import stopSystemVmResponse
from destroySystemVm import destroySystemVmResponse
from listSystemVms import listSystemVmsResponse
from migrateSystemVm import migrateSystemVmResponse
from changeServiceForSystemVm import changeServiceForSystemVmResponse
from scaleSystemVm import scaleSystemVmResponse
from updateConfiguration import updateConfigurationResponse
from listConfigurations import listConfigurationsResponse
from listCapabilities import listCapabilitiesResponse
from listDeploymentPlanners import listDeploymentPlannersResponse
from cleanVMReservations import cleanVMReservationsResponse
from createPod import createPodResponse
from updatePod import updatePodResponse
from deletePod import deletePodResponse
from listPods import listPodsResponse
from createZone import createZoneResponse
from updateZone import updateZoneResponse
from deleteZone import deleteZoneResponse
from listZones import listZonesResponse
from listEvents import listEventsResponse
from listEventTypes import listEventTypesResponse
from archiveEvents import archiveEventsResponse
from deleteEvents import deleteEventsResponse
from listAlerts import listAlertsResponse
from archiveAlerts import archiveAlertsResponse
from deleteAlerts import deleteAlertsResponse
from listCapacity import listCapacityResponse
from addSwift import addSwiftResponse
from listSwifts import listSwiftsResponse
from addS3 import addS3Response
from listS3s import listS3sResponse
from addImageStore import addImageStoreResponse
from listImageStores import listImageStoresResponse
from deleteImageStore import deleteImageStoreResponse
from createSecondaryStagingStore import createSecondaryStagingStoreResponse
from listSecondaryStagingStores import listSecondaryStagingStoresResponse
from deleteSecondaryStagingStore import deleteSecondaryStagingStoreResponse
from addHost import addHostResponse
from addCluster import addClusterResponse
from deleteCluster import deleteClusterResponse
from updateCluster import updateClusterResponse
from reconnectHost import reconnectHostResponse
from updateHost import updateHostResponse
from deleteHost import deleteHostResponse
from prepareHostForMaintenance import prepareHostForMaintenanceResponse
from cancelHostMaintenance import cancelHostMaintenanceResponse
from listHosts import listHostsResponse
from findHostsForMigration import findHostsForMigrationResponse
from addSecondaryStorage import addSecondaryStorageResponse
from updateHostPassword import updateHostPasswordResponse
from releaseHostReservation import releaseHostReservationResponse
from attachVolume import attachVolumeResponse
from uploadVolume import uploadVolumeResponse
from detachVolume import detachVolumeResponse
from createVolume import createVolumeResponse
from deleteVolume import deleteVolumeResponse
from listVolumes import listVolumesResponse
from extractVolume import extractVolumeResponse
from migrateVolume import migrateVolumeResponse
from resizeVolume import resizeVolumeResponse
from updateVolume import updateVolumeResponse
from registerUserKeys import registerUserKeysResponse
from queryAsyncJobResult import queryAsyncJobResultResponse
from listAsyncJobs import listAsyncJobsResponse
from listStoragePools import listStoragePoolsResponse
from listStorageProviders import listStorageProvidersResponse
from createStoragePool import createStoragePoolResponse
from updateStoragePool import updateStoragePoolResponse
from deleteStoragePool import deleteStoragePoolResponse
from listClusters import listClustersResponse
from enableStorageMaintenance import enableStorageMaintenanceResponse
from cancelStorageMaintenance import cancelStorageMaintenanceResponse
from findStoragePoolsForMigration import findStoragePoolsForMigrationResponse
from createSecurityGroup import createSecurityGroupResponse
from deleteSecurityGroup import deleteSecurityGroupResponse
from authorizeSecurityGroupIngress import authorizeSecurityGroupIngressResponse
from revokeSecurityGroupIngress import revokeSecurityGroupIngressResponse
from authorizeSecurityGroupEgress import authorizeSecurityGroupEgressResponse
from revokeSecurityGroupEgress import revokeSecurityGroupEgressResponse
from listSecurityGroups import listSecurityGroupsResponse
from createInstanceGroup import createInstanceGroupResponse
from deleteInstanceGroup import deleteInstanceGroupResponse
from updateInstanceGroup import updateInstanceGroupResponse
from listInstanceGroups import listInstanceGroupsResponse
from uploadCustomCertificate import uploadCustomCertificateResponse
from listHypervisors import listHypervisorsResponse
from createRemoteAccessVpn import createRemoteAccessVpnResponse
from deleteRemoteAccessVpn import deleteRemoteAccessVpnResponse
from listRemoteAccessVpns import listRemoteAccessVpnsResponse
from addVpnUser import addVpnUserResponse
from removeVpnUser import removeVpnUserResponse
from listVpnUsers import listVpnUsersResponse
from createNetworkOffering import createNetworkOfferingResponse
from updateNetworkOffering import updateNetworkOfferingResponse
from deleteNetworkOffering import deleteNetworkOfferingResponse
from listNetworkOfferings import listNetworkOfferingsResponse
from createNetwork import createNetworkResponse
from deleteNetwork import deleteNetworkResponse
from listNetworks import listNetworksResponse
from restartNetwork import restartNetworkResponse
from updateNetwork import updateNetworkResponse
from addNicToVirtualMachine import addNicToVirtualMachineResponse
from removeNicFromVirtualMachine import removeNicFromVirtualMachineResponse
from updateDefaultNicForVirtualMachine import updateDefaultNicForVirtualMachineResponse
from addIpToNic import addIpToNicResponse
from removeIpFromNic import removeIpFromNicResponse
from listNics import listNicsResponse
from registerSSHKeyPair import registerSSHKeyPairResponse
from createSSHKeyPair import createSSHKeyPairResponse
from deleteSSHKeyPair import deleteSSHKeyPairResponse
from listSSHKeyPairs import listSSHKeyPairsResponse
from createProject import createProjectResponse
from deleteProject import deleteProjectResponse
from updateProject import updateProjectResponse
from activateProject import activateProjectResponse
from suspendProject import suspendProjectResponse
from listProjects import listProjectsResponse
from addAccountToProject import addAccountToProjectResponse
from deleteAccountFromProject import deleteAccountFromProjectResponse
from listProjectAccounts import listProjectAccountsResponse
from listProjectInvitations import listProjectInvitationsResponse
from updateProjectInvitation import updateProjectInvitationResponse
from deleteProjectInvitation import deleteProjectInvitationResponse
from createFirewallRule import createFirewallRuleResponse
from deleteFirewallRule import deleteFirewallRuleResponse
from listFirewallRules import listFirewallRulesResponse
from createEgressFirewallRule import createEgressFirewallRuleResponse
from deleteEgressFirewallRule import deleteEgressFirewallRuleResponse
from listEgressFirewallRules import listEgressFirewallRulesResponse
from updateHypervisorCapabilities import updateHypervisorCapabilitiesResponse
from listHypervisorCapabilities import listHypervisorCapabilitiesResponse
from createPhysicalNetwork import createPhysicalNetworkResponse
from deletePhysicalNetwork import deletePhysicalNetworkResponse
from listPhysicalNetworks import listPhysicalNetworksResponse
from updatePhysicalNetwork import updatePhysicalNetworkResponse
from listSupportedNetworkServices import listSupportedNetworkServicesResponse
from addNetworkServiceProvider import addNetworkServiceProviderResponse
from deleteNetworkServiceProvider import deleteNetworkServiceProviderResponse
from listNetworkServiceProviders import listNetworkServiceProvidersResponse
from updateNetworkServiceProvider import updateNetworkServiceProviderResponse
from addTrafficType import addTrafficTypeResponse
from deleteTrafficType import deleteTrafficTypeResponse
from listTrafficTypes import listTrafficTypesResponse
from updateTrafficType import updateTrafficTypeResponse
from listTrafficTypeImplementors import listTrafficTypeImplementorsResponse
from createStorageNetworkIpRange import createStorageNetworkIpRangeResponse
from deleteStorageNetworkIpRange import deleteStorageNetworkIpRangeResponse
from listStorageNetworkIpRange import listStorageNetworkIpRangeResponse
from updateStorageNetworkIpRange import updateStorageNetworkIpRangeResponse
from addNetworkDevice import addNetworkDeviceResponse
from listNetworkDevice import listNetworkDeviceResponse
from deleteNetworkDevice import deleteNetworkDeviceResponse
from createVPC import createVPCResponse
from listVPCs import listVPCsResponse
from deleteVPC import deleteVPCResponse
from updateVPC import updateVPCResponse
from restartVPC import restartVPCResponse
from createVPCOffering import createVPCOfferingResponse
from updateVPCOffering import updateVPCOfferingResponse
from deleteVPCOffering import deleteVPCOfferingResponse
from listVPCOfferings import listVPCOfferingsResponse
from createPrivateGateway import createPrivateGatewayResponse
from listPrivateGateways import listPrivateGatewaysResponse
from deletePrivateGateway import deletePrivateGatewayResponse
from createNetworkACL import createNetworkACLResponse
from updateNetworkACLItem import updateNetworkACLItemResponse
from deleteNetworkACL import deleteNetworkACLResponse
from listNetworkACLs import listNetworkACLsResponse
from createNetworkACLList import createNetworkACLListResponse
from deleteNetworkACLList import deleteNetworkACLListResponse
from replaceNetworkACLList import replaceNetworkACLListResponse
from listNetworkACLLists import listNetworkACLListsResponse
from createStaticRoute import createStaticRouteResponse
from deleteStaticRoute import deleteStaticRouteResponse
from listStaticRoutes import listStaticRoutesResponse
from createTags import createTagsResponse
from deleteTags import deleteTagsResponse
from listTags import listTagsResponse
from addResourceDetail import addResourceDetailResponse
from removeResourceDetail import removeResourceDetailResponse
from listResourceDetails import listResourceDetailsResponse
from createVpnCustomerGateway import createVpnCustomerGatewayResponse
from createVpnGateway import createVpnGatewayResponse
from createVpnConnection import createVpnConnectionResponse
from deleteVpnCustomerGateway import deleteVpnCustomerGatewayResponse
from deleteVpnGateway import deleteVpnGatewayResponse
from deleteVpnConnection import deleteVpnConnectionResponse
from updateVpnCustomerGateway import updateVpnCustomerGatewayResponse
from resetVpnConnection import resetVpnConnectionResponse
from listVpnCustomerGateways import listVpnCustomerGatewaysResponse
from listVpnGateways import listVpnGatewaysResponse
from listVpnConnections import listVpnConnectionsResponse
from generateUsageRecords import generateUsageRecordsResponse
from listUsageRecords import listUsageRecordsResponse
from listUsageTypes import listUsageTypesResponse
from addTrafficMonitor import addTrafficMonitorResponse
from deleteTrafficMonitor import deleteTrafficMonitorResponse
from listTrafficMonitors import listTrafficMonitorsResponse
from addNiciraNvpDevice import addNiciraNvpDeviceResponse
from deleteNiciraNvpDevice import deleteNiciraNvpDeviceResponse
from listNiciraNvpDevices import listNiciraNvpDevicesResponse
from listNiciraNvpDeviceNetworks import listNiciraNvpDeviceNetworksResponse
from addBigSwitchVnsDevice import addBigSwitchVnsDeviceResponse
from deleteBigSwitchVnsDevice import deleteBigSwitchVnsDeviceResponse
from listBigSwitchVnsDevices import listBigSwitchVnsDevicesResponse
from listApis import listApisResponse
from getApiLimit import getApiLimitResponse
from resetApiLimit import resetApiLimitResponse
from addRegion import addRegionResponse
from updateRegion import updateRegionResponse
from removeRegion import removeRegionResponse
from listRegions import listRegionsResponse
from createGlobalLoadBalancerRule import createGlobalLoadBalancerRuleResponse
from deleteGlobalLoadBalancerRule import deleteGlobalLoadBalancerRuleResponse
from updateGlobalLoadBalancerRule import updateGlobalLoadBalancerRuleResponse
from listGlobalLoadBalancerRules import listGlobalLoadBalancerRulesResponse
from assignToGlobalLoadBalancerRule import assignToGlobalLoadBalancerRuleResponse
from removeFromGlobalLoadBalancerRule import removeFromGlobalLoadBalancerRuleResponse
from listVMSnapshot import listVMSnapshotResponse
from createVMSnapshot import createVMSnapshotResponse
from deleteVMSnapshot import deleteVMSnapshotResponse
from revertToVMSnapshot import revertToVMSnapshotResponse
from addBaremetalHost import addBaremetalHostResponse
from addBaremetalPxeKickStartServer import addBaremetalPxeKickStartServerResponse
from addBaremetalPxePingServer import addBaremetalPxePingServerResponse
from addBaremetalDhcp import addBaremetalDhcpResponse
from listBaremetalDhcp import listBaremetalDhcpResponse
from listBaremetalPxeServers import listBaremetalPxeServersResponse
from addUcsManager import addUcsManagerResponse
from listUcsManagers import listUcsManagersResponse
from listUcsProfiles import listUcsProfilesResponse
from listUcsBlades import listUcsBladesResponse
from associateUcsProfileToBlade import associateUcsProfileToBladeResponse
from createLoadBalancer import createLoadBalancerResponse
from listLoadBalancers import listLoadBalancersResponse
from deleteLoadBalancer import deleteLoadBalancerResponse
from configureInternalLoadBalancerElement import configureInternalLoadBalancerElementResponse
from createInternalLoadBalancerElement import createInternalLoadBalancerElementResponse
from listInternalLoadBalancerElements import listInternalLoadBalancerElementsResponse
from createAffinityGroup import createAffinityGroupResponse
from deleteAffinityGroup import deleteAffinityGroupResponse
from listAffinityGroups import listAffinityGroupsResponse
from updateVMAffinityGroup import updateVMAffinityGroupResponse
from listAffinityGroupTypes import listAffinityGroupTypesResponse
from createPortableIpRange import createPortableIpRangeResponse
from deletePortableIpRange import deletePortableIpRangeResponse
from listPortableIpRanges import listPortableIpRangesResponse
from stopInternalLoadBalancerVM import stopInternalLoadBalancerVMResponse
from startInternalLoadBalancerVM import startInternalLoadBalancerVMResponse
from listInternalLoadBalancerVMs import listInternalLoadBalancerVMsResponse
from listNetworkIsolationMethods import listNetworkIsolationMethodsResponse
from dedicateZone import dedicateZoneResponse
from dedicatePod import dedicatePodResponse
from dedicateCluster import dedicateClusterResponse
from dedicateHost import dedicateHostResponse
from releaseDedicatedZone import releaseDedicatedZoneResponse
from releaseDedicatedPod import releaseDedicatedPodResponse
from releaseDedicatedCluster import releaseDedicatedClusterResponse
from releaseDedicatedHost import releaseDedicatedHostResponse
from listDedicatedZones import listDedicatedZonesResponse
from listDedicatedPods import listDedicatedPodsResponse
from listDedicatedClusters import listDedicatedClustersResponse
from listDedicatedHosts import listDedicatedHostsResponse
from listLdapConfigurations import listLdapConfigurationsResponse
from addLdapConfiguration import addLdapConfigurationResponse
from deleteLdapConfiguration import deleteLdapConfigurationResponse
from listLdapUsers import listLdapUsersResponse
from ldapCreateAccount import ldapCreateAccountResponse
from login import loginResponse
from logout import logoutResponse
class CloudStackAPIClient(object):
    def __init__(self, connection):
        self.connection = connection
        self._id = None

    def __copy__(self):
        return CloudStackAPIClient(copy.copy(self.connection))

    @property
    def id(self):
        return self._id

    @id.setter
    def id(self, identifier):
        self._id = identifier

    def createAccount(self, command, method="GET"):
        response = createAccountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteAccount(self, command, method="GET"):
        response = deleteAccountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateAccount(self, command, method="GET"):
        response = updateAccountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def disableAccount(self, command, method="GET"):
        response = disableAccountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def enableAccount(self, command, method="GET"):
        response = enableAccountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def lockAccount(self, command, method="GET"):
        response = lockAccountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listAccounts(self, command, method="GET"):
        response = listAccountsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def markDefaultZoneForAccount(self, command, method="GET"):
        response = markDefaultZoneForAccountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createUser(self, command, method="GET"):
        response = createUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteUser(self, command, method="GET"):
        response = deleteUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateUser(self, command, method="GET"):
        response = updateUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listUsers(self, command, method="GET"):
        response = listUsersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def lockUser(self, command, method="GET"):
        response = lockUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def disableUser(self, command, method="GET"):
        response = disableUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def enableUser(self, command, method="GET"):
        response = enableUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def getUser(self, command, method="GET"):
        response = getUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createDomain(self, command, method="GET"):
        response = createDomainResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateDomain(self, command, method="GET"):
        response = updateDomainResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteDomain(self, command, method="GET"):
        response = deleteDomainResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDomains(self, command, method="GET"):
        response = listDomainsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDomainChildren(self, command, method="GET"):
        response = listDomainChildrenResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def getCloudIdentifier(self, command, method="GET"):
        response = getCloudIdentifierResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateResourceLimit(self, command, method="GET"):
        response = updateResourceLimitResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateResourceCount(self, command, method="GET"):
        response = updateResourceCountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listResourceLimits(self, command, method="GET"):
        response = listResourceLimitsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deployVirtualMachine(self, command, method="GET"):
        response = deployVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def destroyVirtualMachine(self, command, method="GET"):
        response = destroyVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def rebootVirtualMachine(self, command, method="GET"):
        response = rebootVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def startVirtualMachine(self, command, method="GET"):
        response = startVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def stopVirtualMachine(self, command, method="GET"):
        response = stopVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def resetPasswordForVirtualMachine(self, command, method="GET"):
        response = resetPasswordForVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def resetSSHKeyForVirtualMachine(self, command, method="GET"):
        response = resetSSHKeyForVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateVirtualMachine(self, command, method="GET"):
        response = updateVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVirtualMachines(self, command, method="GET"):
        response = listVirtualMachinesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def getVMPassword(self, command, method="GET"):
        response = getVMPasswordResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def restoreVirtualMachine(self, command, method="GET"):
        response = restoreVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def changeServiceForVirtualMachine(self, command, method="GET"):
        response = changeServiceForVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def scaleVirtualMachine(self, command, method="GET"):
        response = scaleVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def assignVirtualMachine(self, command, method="GET"):
        response = assignVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def migrateVirtualMachine(self, command, method="GET"):
        response = migrateVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def migrateVirtualMachineWithVolume(self, command, method="GET"):
        response = migrateVirtualMachineWithVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def recoverVirtualMachine(self, command, method="GET"):
        response = recoverVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createSnapshot(self, command, method="GET"):
        response = createSnapshotResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listSnapshots(self, command, method="GET"):
        response = listSnapshotsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteSnapshot(self, command, method="GET"):
        response = deleteSnapshotResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createSnapshotPolicy(self, command, method="GET"):
        response = createSnapshotPolicyResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteSnapshotPolicies(self, command, method="GET"):
        response = deleteSnapshotPoliciesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listSnapshotPolicies(self, command, method="GET"):
        response = listSnapshotPoliciesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def revertSnapshot(self, command, method="GET"):
        response = revertSnapshotResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createTemplate(self, command, method="GET"):
        response = createTemplateResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def registerTemplate(self, command, method="GET"):
        response = registerTemplateResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateTemplate(self, command, method="GET"):
        response = updateTemplateResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def copyTemplate(self, command, method="GET"):
        response = copyTemplateResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteTemplate(self, command, method="GET"):
        response = deleteTemplateResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listTemplates(self, command, method="GET"):
        response = listTemplatesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateTemplatePermissions(self, command, method="GET"):
        response = updateTemplatePermissionsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listTemplatePermissions(self, command, method="GET"):
        response = listTemplatePermissionsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def extractTemplate(self, command, method="GET"):
        response = extractTemplateResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def prepareTemplate(self, command, method="GET"):
        response = prepareTemplateResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def attachIso(self, command, method="GET"):
        response = attachIsoResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def detachIso(self, command, method="GET"):
        response = detachIsoResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listIsos(self, command, method="GET"):
        response = listIsosResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def registerIso(self, command, method="GET"):
        response = registerIsoResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateIso(self, command, method="GET"):
        response = updateIsoResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteIso(self, command, method="GET"):
        response = deleteIsoResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def copyIso(self, command, method="GET"):
        response = copyIsoResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateIsoPermissions(self, command, method="GET"):
        response = updateIsoPermissionsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listIsoPermissions(self, command, method="GET"):
        response = listIsoPermissionsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def extractIso(self, command, method="GET"):
        response = extractIsoResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listOsTypes(self, command, method="GET"):
        response = listOsTypesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listOsCategories(self, command, method="GET"):
        response = listOsCategoriesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createServiceOffering(self, command, method="GET"):
        response = createServiceOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteServiceOffering(self, command, method="GET"):
        response = deleteServiceOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateServiceOffering(self, command, method="GET"):
        response = updateServiceOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listServiceOfferings(self, command, method="GET"):
        response = listServiceOfferingsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createDiskOffering(self, command, method="GET"):
        response = createDiskOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateDiskOffering(self, command, method="GET"):
        response = updateDiskOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteDiskOffering(self, command, method="GET"):
        response = deleteDiskOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDiskOfferings(self, command, method="GET"):
        response = listDiskOfferingsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVlanIpRange(self, command, method="GET"):
        response = createVlanIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteVlanIpRange(self, command, method="GET"):
        response = deleteVlanIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVlanIpRanges(self, command, method="GET"):
        response = listVlanIpRangesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def dedicatePublicIpRange(self, command, method="GET"):
        response = dedicatePublicIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def releasePublicIpRange(self, command, method="GET"):
        response = releasePublicIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def dedicateGuestVlanRange(self, command, method="GET"):
        response = dedicateGuestVlanRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def releaseDedicatedGuestVlanRange(self, command, method="GET"):
        response = releaseDedicatedGuestVlanRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDedicatedGuestVlanRanges(self, command, method="GET"):
        response = listDedicatedGuestVlanRangesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def associateIpAddress(self, command, method="GET"):
        response = associateIpAddressResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def disassociateIpAddress(self, command, method="GET"):
        response = disassociateIpAddressResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listPublicIpAddresses(self, command, method="GET"):
        response = listPublicIpAddressesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listPortForwardingRules(self, command, method="GET"):
        response = listPortForwardingRulesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createPortForwardingRule(self, command, method="GET"):
        response = createPortForwardingRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deletePortForwardingRule(self, command, method="GET"):
        response = deletePortForwardingRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updatePortForwardingRule(self, command, method="GET"):
        response = updatePortForwardingRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def enableStaticNat(self, command, method="GET"):
        response = enableStaticNatResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createIpForwardingRule(self, command, method="GET"):
        response = createIpForwardingRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteIpForwardingRule(self, command, method="GET"):
        response = deleteIpForwardingRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listIpForwardingRules(self, command, method="GET"):
        response = listIpForwardingRulesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def disableStaticNat(self, command, method="GET"):
        response = disableStaticNatResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createLoadBalancerRule(self, command, method="GET"):
        response = createLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteLoadBalancerRule(self, command, method="GET"):
        response = deleteLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def removeFromLoadBalancerRule(self, command, method="GET"):
        response = removeFromLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def assignToLoadBalancerRule(self, command, method="GET"):
        response = assignToLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createLBStickinessPolicy(self, command, method="GET"):
        response = createLBStickinessPolicyResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteLBStickinessPolicy(self, command, method="GET"):
        response = deleteLBStickinessPolicyResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listLoadBalancerRules(self, command, method="GET"):
        response = listLoadBalancerRulesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listLBStickinessPolicies(self, command, method="GET"):
        response = listLBStickinessPoliciesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listLBHealthCheckPolicies(self, command, method="GET"):
        response = listLBHealthCheckPoliciesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createLBHealthCheckPolicy(self, command, method="GET"):
        response = createLBHealthCheckPolicyResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteLBHealthCheckPolicy(self, command, method="GET"):
        response = deleteLBHealthCheckPolicyResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listLoadBalancerRuleInstances(self, command, method="GET"):
        response = listLoadBalancerRuleInstancesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateLoadBalancerRule(self, command, method="GET"):
        response = updateLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createCounter(self, command, method="GET"):
        response = createCounterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createCondition(self, command, method="GET"):
        response = createConditionResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createAutoScalePolicy(self, command, method="GET"):
        response = createAutoScalePolicyResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createAutoScaleVmProfile(self, command, method="GET"):
        response = createAutoScaleVmProfileResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createAutoScaleVmGroup(self, command, method="GET"):
        response = createAutoScaleVmGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteCounter(self, command, method="GET"):
        response = deleteCounterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteCondition(self, command, method="GET"):
        response = deleteConditionResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteAutoScalePolicy(self, command, method="GET"):
        response = deleteAutoScalePolicyResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteAutoScaleVmProfile(self, command, method="GET"):
        response = deleteAutoScaleVmProfileResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteAutoScaleVmGroup(self, command, method="GET"):
        response = deleteAutoScaleVmGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listCounters(self, command, method="GET"):
        response = listCountersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listConditions(self, command, method="GET"):
        response = listConditionsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listAutoScalePolicies(self, command, method="GET"):
        response = listAutoScalePoliciesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listAutoScaleVmProfiles(self, command, method="GET"):
        response = listAutoScaleVmProfilesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listAutoScaleVmGroups(self, command, method="GET"):
        response = listAutoScaleVmGroupsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def enableAutoScaleVmGroup(self, command, method="GET"):
        response = enableAutoScaleVmGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def disableAutoScaleVmGroup(self, command, method="GET"):
        response = disableAutoScaleVmGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateAutoScalePolicy(self, command, method="GET"):
        response = updateAutoScalePolicyResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateAutoScaleVmProfile(self, command, method="GET"):
        response = updateAutoScaleVmProfileResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateAutoScaleVmGroup(self, command, method="GET"):
        response = updateAutoScaleVmGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def startRouter(self, command, method="GET"):
        response = startRouterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def rebootRouter(self, command, method="GET"):
        response = rebootRouterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def stopRouter(self, command, method="GET"):
        response = stopRouterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def destroyRouter(self, command, method="GET"):
        response = destroyRouterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def changeServiceForRouter(self, command, method="GET"):
        response = changeServiceForRouterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listRouters(self, command, method="GET"):
        response = listRoutersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVirtualRouterElements(self, command, method="GET"):
        response = listVirtualRouterElementsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def configureVirtualRouterElement(self, command, method="GET"):
        response = configureVirtualRouterElementResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVirtualRouterElement(self, command, method="GET"):
        response = createVirtualRouterElementResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def startSystemVm(self, command, method="GET"):
        response = startSystemVmResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def rebootSystemVm(self, command, method="GET"):
        response = rebootSystemVmResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def stopSystemVm(self, command, method="GET"):
        response = stopSystemVmResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def destroySystemVm(self, command, method="GET"):
        response = destroySystemVmResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listSystemVms(self, command, method="GET"):
        response = listSystemVmsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def migrateSystemVm(self, command, method="GET"):
        response = migrateSystemVmResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def changeServiceForSystemVm(self, command, method="GET"):
        response = changeServiceForSystemVmResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def scaleSystemVm(self, command, method="GET"):
        response = scaleSystemVmResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateConfiguration(self, command, method="GET"):
        response = updateConfigurationResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listConfigurations(self, command, method="GET"):
        response = listConfigurationsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listCapabilities(self, command, method="GET"):
        response = listCapabilitiesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDeploymentPlanners(self, command, method="GET"):
        response = listDeploymentPlannersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def cleanVMReservations(self, command, method="GET"):
        response = cleanVMReservationsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createPod(self, command, method="GET"):
        response = createPodResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updatePod(self, command, method="GET"):
        response = updatePodResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deletePod(self, command, method="GET"):
        response = deletePodResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listPods(self, command, method="GET"):
        response = listPodsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createZone(self, command, method="GET"):
        response = createZoneResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateZone(self, command, method="GET"):
        response = updateZoneResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteZone(self, command, method="GET"):
        response = deleteZoneResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listZones(self, command, method="GET"):
        response = listZonesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listEvents(self, command, method="GET"):
        response = listEventsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listEventTypes(self, command, method="GET"):
        response = listEventTypesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def archiveEvents(self, command, method="GET"):
        response = archiveEventsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteEvents(self, command, method="GET"):
        response = deleteEventsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listAlerts(self, command, method="GET"):
        response = listAlertsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def archiveAlerts(self, command, method="GET"):
        response = archiveAlertsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteAlerts(self, command, method="GET"):
        response = deleteAlertsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listCapacity(self, command, method="GET"):
        response = listCapacityResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addSwift(self, command, method="GET"):
        response = addSwiftResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listSwifts(self, command, method="GET"):
        response = listSwiftsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addS3(self, command, method="GET"):
        response = addS3Response()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listS3s(self, command, method="GET"):
        response = listS3sResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addImageStore(self, command, method="GET"):
        response = addImageStoreResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listImageStores(self, command, method="GET"):
        response = listImageStoresResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteImageStore(self, command, method="GET"):
        response = deleteImageStoreResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createSecondaryStagingStore(self, command, method="GET"):
        response = createSecondaryStagingStoreResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listSecondaryStagingStores(self, command, method="GET"):
        response = listSecondaryStagingStoresResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteSecondaryStagingStore(self, command, method="GET"):
        response = deleteSecondaryStagingStoreResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addHost(self, command, method="GET"):
        response = addHostResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addCluster(self, command, method="GET"):
        response = addClusterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteCluster(self, command, method="GET"):
        response = deleteClusterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateCluster(self, command, method="GET"):
        response = updateClusterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def reconnectHost(self, command, method="GET"):
        response = reconnectHostResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateHost(self, command, method="GET"):
        response = updateHostResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteHost(self, command, method="GET"):
        response = deleteHostResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def prepareHostForMaintenance(self, command, method="GET"):
        response = prepareHostForMaintenanceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def cancelHostMaintenance(self, command, method="GET"):
        response = cancelHostMaintenanceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listHosts(self, command, method="GET"):
        response = listHostsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def findHostsForMigration(self, command, method="GET"):
        response = findHostsForMigrationResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addSecondaryStorage(self, command, method="GET"):
        response = addSecondaryStorageResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateHostPassword(self, command, method="GET"):
        response = updateHostPasswordResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def releaseHostReservation(self, command, method="GET"):
        response = releaseHostReservationResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def attachVolume(self, command, method="GET"):
        response = attachVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def uploadVolume(self, command, method="GET"):
        response = uploadVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def detachVolume(self, command, method="GET"):
        response = detachVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVolume(self, command, method="GET"):
        response = createVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteVolume(self, command, method="GET"):
        response = deleteVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVolumes(self, command, method="GET"):
        response = listVolumesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def extractVolume(self, command, method="GET"):
        response = extractVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def migrateVolume(self, command, method="GET"):
        response = migrateVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def resizeVolume(self, command, method="GET"):
        response = resizeVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateVolume(self, command, method="GET"):
        response = updateVolumeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def registerUserKeys(self, command, method="GET"):
        response = registerUserKeysResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def queryAsyncJobResult(self, command, method="GET"):
        response = queryAsyncJobResultResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listAsyncJobs(self, command, method="GET"):
        response = listAsyncJobsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listStoragePools(self, command, method="GET"):
        response = listStoragePoolsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listStorageProviders(self, command, method="GET"):
        response = listStorageProvidersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createStoragePool(self, command, method="GET"):
        response = createStoragePoolResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateStoragePool(self, command, method="GET"):
        response = updateStoragePoolResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteStoragePool(self, command, method="GET"):
        response = deleteStoragePoolResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listClusters(self, command, method="GET"):
        response = listClustersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def enableStorageMaintenance(self, command, method="GET"):
        response = enableStorageMaintenanceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def cancelStorageMaintenance(self, command, method="GET"):
        response = cancelStorageMaintenanceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def findStoragePoolsForMigration(self, command, method="GET"):
        response = findStoragePoolsForMigrationResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createSecurityGroup(self, command, method="GET"):
        response = createSecurityGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteSecurityGroup(self, command, method="GET"):
        response = deleteSecurityGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def authorizeSecurityGroupIngress(self, command, method="GET"):
        response = authorizeSecurityGroupIngressResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def revokeSecurityGroupIngress(self, command, method="GET"):
        response = revokeSecurityGroupIngressResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def authorizeSecurityGroupEgress(self, command, method="GET"):
        response = authorizeSecurityGroupEgressResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def revokeSecurityGroupEgress(self, command, method="GET"):
        response = revokeSecurityGroupEgressResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listSecurityGroups(self, command, method="GET"):
        response = listSecurityGroupsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createInstanceGroup(self, command, method="GET"):
        response = createInstanceGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteInstanceGroup(self, command, method="GET"):
        response = deleteInstanceGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateInstanceGroup(self, command, method="GET"):
        response = updateInstanceGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listInstanceGroups(self, command, method="GET"):
        response = listInstanceGroupsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def uploadCustomCertificate(self, command, method="GET"):
        response = uploadCustomCertificateResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listHypervisors(self, command, method="GET"):
        response = listHypervisorsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createRemoteAccessVpn(self, command, method="GET"):
        response = createRemoteAccessVpnResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteRemoteAccessVpn(self, command, method="GET"):
        response = deleteRemoteAccessVpnResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listRemoteAccessVpns(self, command, method="GET"):
        response = listRemoteAccessVpnsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addVpnUser(self, command, method="GET"):
        response = addVpnUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def removeVpnUser(self, command, method="GET"):
        response = removeVpnUserResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVpnUsers(self, command, method="GET"):
        response = listVpnUsersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createNetworkOffering(self, command, method="GET"):
        response = createNetworkOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateNetworkOffering(self, command, method="GET"):
        response = updateNetworkOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteNetworkOffering(self, command, method="GET"):
        response = deleteNetworkOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNetworkOfferings(self, command, method="GET"):
        response = listNetworkOfferingsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createNetwork(self, command, method="GET"):
        response = createNetworkResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteNetwork(self, command, method="GET"):
        response = deleteNetworkResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNetworks(self, command, method="GET"):
        response = listNetworksResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def restartNetwork(self, command, method="GET"):
        response = restartNetworkResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateNetwork(self, command, method="GET"):
        response = updateNetworkResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addNicToVirtualMachine(self, command, method="GET"):
        response = addNicToVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def removeNicFromVirtualMachine(self, command, method="GET"):
        response = removeNicFromVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateDefaultNicForVirtualMachine(self, command, method="GET"):
        response = updateDefaultNicForVirtualMachineResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addIpToNic(self, command, method="GET"):
        response = addIpToNicResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def removeIpFromNic(self, command, method="GET"):
        response = removeIpFromNicResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNics(self, command, method="GET"):
        response = listNicsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def registerSSHKeyPair(self, command, method="GET"):
        response = registerSSHKeyPairResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createSSHKeyPair(self, command, method="GET"):
        response = createSSHKeyPairResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteSSHKeyPair(self, command, method="GET"):
        response = deleteSSHKeyPairResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listSSHKeyPairs(self, command, method="GET"):
        response = listSSHKeyPairsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createProject(self, command, method="GET"):
        response = createProjectResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteProject(self, command, method="GET"):
        response = deleteProjectResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateProject(self, command, method="GET"):
        response = updateProjectResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def activateProject(self, command, method="GET"):
        response = activateProjectResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def suspendProject(self, command, method="GET"):
        response = suspendProjectResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listProjects(self, command, method="GET"):
        response = listProjectsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addAccountToProject(self, command, method="GET"):
        response = addAccountToProjectResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteAccountFromProject(self, command, method="GET"):
        response = deleteAccountFromProjectResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listProjectAccounts(self, command, method="GET"):
        response = listProjectAccountsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listProjectInvitations(self, command, method="GET"):
        response = listProjectInvitationsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateProjectInvitation(self, command, method="GET"):
        response = updateProjectInvitationResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteProjectInvitation(self, command, method="GET"):
        response = deleteProjectInvitationResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createFirewallRule(self, command, method="GET"):
        response = createFirewallRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteFirewallRule(self, command, method="GET"):
        response = deleteFirewallRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listFirewallRules(self, command, method="GET"):
        response = listFirewallRulesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createEgressFirewallRule(self, command, method="GET"):
        response = createEgressFirewallRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteEgressFirewallRule(self, command, method="GET"):
        response = deleteEgressFirewallRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listEgressFirewallRules(self, command, method="GET"):
        response = listEgressFirewallRulesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateHypervisorCapabilities(self, command, method="GET"):
        response = updateHypervisorCapabilitiesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listHypervisorCapabilities(self, command, method="GET"):
        response = listHypervisorCapabilitiesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createPhysicalNetwork(self, command, method="GET"):
        response = createPhysicalNetworkResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deletePhysicalNetwork(self, command, method="GET"):
        response = deletePhysicalNetworkResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listPhysicalNetworks(self, command, method="GET"):
        response = listPhysicalNetworksResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updatePhysicalNetwork(self, command, method="GET"):
        response = updatePhysicalNetworkResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listSupportedNetworkServices(self, command, method="GET"):
        response = listSupportedNetworkServicesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addNetworkServiceProvider(self, command, method="GET"):
        response = addNetworkServiceProviderResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteNetworkServiceProvider(self, command, method="GET"):
        response = deleteNetworkServiceProviderResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNetworkServiceProviders(self, command, method="GET"):
        response = listNetworkServiceProvidersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateNetworkServiceProvider(self, command, method="GET"):
        response = updateNetworkServiceProviderResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addTrafficType(self, command, method="GET"):
        response = addTrafficTypeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteTrafficType(self, command, method="GET"):
        response = deleteTrafficTypeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listTrafficTypes(self, command, method="GET"):
        response = listTrafficTypesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateTrafficType(self, command, method="GET"):
        response = updateTrafficTypeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listTrafficTypeImplementors(self, command, method="GET"):
        response = listTrafficTypeImplementorsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createStorageNetworkIpRange(self, command, method="GET"):
        response = createStorageNetworkIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteStorageNetworkIpRange(self, command, method="GET"):
        response = deleteStorageNetworkIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listStorageNetworkIpRange(self, command, method="GET"):
        response = listStorageNetworkIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateStorageNetworkIpRange(self, command, method="GET"):
        response = updateStorageNetworkIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addNetworkDevice(self, command, method="GET"):
        response = addNetworkDeviceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNetworkDevice(self, command, method="GET"):
        response = listNetworkDeviceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteNetworkDevice(self, command, method="GET"):
        response = deleteNetworkDeviceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVPC(self, command, method="GET"):
        response = createVPCResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVPCs(self, command, method="GET"):
        response = listVPCsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteVPC(self, command, method="GET"):
        response = deleteVPCResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateVPC(self, command, method="GET"):
        response = updateVPCResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def restartVPC(self, command, method="GET"):
        response = restartVPCResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVPCOffering(self, command, method="GET"):
        response = createVPCOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateVPCOffering(self, command, method="GET"):
        response = updateVPCOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteVPCOffering(self, command, method="GET"):
        response = deleteVPCOfferingResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVPCOfferings(self, command, method="GET"):
        response = listVPCOfferingsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createPrivateGateway(self, command, method="GET"):
        response = createPrivateGatewayResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listPrivateGateways(self, command, method="GET"):
        response = listPrivateGatewaysResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deletePrivateGateway(self, command, method="GET"):
        response = deletePrivateGatewayResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createNetworkACL(self, command, method="GET"):
        response = createNetworkACLResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateNetworkACLItem(self, command, method="GET"):
        response = updateNetworkACLItemResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteNetworkACL(self, command, method="GET"):
        response = deleteNetworkACLResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNetworkACLs(self, command, method="GET"):
        response = listNetworkACLsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createNetworkACLList(self, command, method="GET"):
        response = createNetworkACLListResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteNetworkACLList(self, command, method="GET"):
        response = deleteNetworkACLListResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def replaceNetworkACLList(self, command, method="GET"):
        response = replaceNetworkACLListResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNetworkACLLists(self, command, method="GET"):
        response = listNetworkACLListsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createStaticRoute(self, command, method="GET"):
        response = createStaticRouteResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteStaticRoute(self, command, method="GET"):
        response = deleteStaticRouteResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listStaticRoutes(self, command, method="GET"):
        response = listStaticRoutesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createTags(self, command, method="GET"):
        response = createTagsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteTags(self, command, method="GET"):
        response = deleteTagsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listTags(self, command, method="GET"):
        response = listTagsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addResourceDetail(self, command, method="GET"):
        response = addResourceDetailResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def removeResourceDetail(self, command, method="GET"):
        response = removeResourceDetailResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listResourceDetails(self, command, method="GET"):
        response = listResourceDetailsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVpnCustomerGateway(self, command, method="GET"):
        response = createVpnCustomerGatewayResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVpnGateway(self, command, method="GET"):
        response = createVpnGatewayResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVpnConnection(self, command, method="GET"):
        response = createVpnConnectionResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteVpnCustomerGateway(self, command, method="GET"):
        response = deleteVpnCustomerGatewayResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteVpnGateway(self, command, method="GET"):
        response = deleteVpnGatewayResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteVpnConnection(self, command, method="GET"):
        response = deleteVpnConnectionResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateVpnCustomerGateway(self, command, method="GET"):
        response = updateVpnCustomerGatewayResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def resetVpnConnection(self, command, method="GET"):
        response = resetVpnConnectionResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVpnCustomerGateways(self, command, method="GET"):
        response = listVpnCustomerGatewaysResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVpnGateways(self, command, method="GET"):
        response = listVpnGatewaysResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVpnConnections(self, command, method="GET"):
        response = listVpnConnectionsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def generateUsageRecords(self, command, method="GET"):
        response = generateUsageRecordsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listUsageRecords(self, command, method="GET"):
        response = listUsageRecordsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listUsageTypes(self, command, method="GET"):
        response = listUsageTypesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addTrafficMonitor(self, command, method="GET"):
        response = addTrafficMonitorResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteTrafficMonitor(self, command, method="GET"):
        response = deleteTrafficMonitorResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listTrafficMonitors(self, command, method="GET"):
        response = listTrafficMonitorsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addNiciraNvpDevice(self, command, method="GET"):
        response = addNiciraNvpDeviceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteNiciraNvpDevice(self, command, method="GET"):
        response = deleteNiciraNvpDeviceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNiciraNvpDevices(self, command, method="GET"):
        response = listNiciraNvpDevicesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNiciraNvpDeviceNetworks(self, command, method="GET"):
        response = listNiciraNvpDeviceNetworksResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addBigSwitchVnsDevice(self, command, method="GET"):
        response = addBigSwitchVnsDeviceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteBigSwitchVnsDevice(self, command, method="GET"):
        response = deleteBigSwitchVnsDeviceResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listBigSwitchVnsDevices(self, command, method="GET"):
        response = listBigSwitchVnsDevicesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listApis(self, command, method="GET"):
        response = listApisResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def getApiLimit(self, command, method="GET"):
        response = getApiLimitResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def resetApiLimit(self, command, method="GET"):
        response = resetApiLimitResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addRegion(self, command, method="GET"):
        response = addRegionResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateRegion(self, command, method="GET"):
        response = updateRegionResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def removeRegion(self, command, method="GET"):
        response = removeRegionResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listRegions(self, command, method="GET"):
        response = listRegionsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createGlobalLoadBalancerRule(self, command, method="GET"):
        response = createGlobalLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteGlobalLoadBalancerRule(self, command, method="GET"):
        response = deleteGlobalLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateGlobalLoadBalancerRule(self, command, method="GET"):
        response = updateGlobalLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listGlobalLoadBalancerRules(self, command, method="GET"):
        response = listGlobalLoadBalancerRulesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def assignToGlobalLoadBalancerRule(self, command, method="GET"):
        response = assignToGlobalLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def removeFromGlobalLoadBalancerRule(self, command, method="GET"):
        response = removeFromGlobalLoadBalancerRuleResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listVMSnapshot(self, command, method="GET"):
        response = listVMSnapshotResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createVMSnapshot(self, command, method="GET"):
        response = createVMSnapshotResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteVMSnapshot(self, command, method="GET"):
        response = deleteVMSnapshotResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def revertToVMSnapshot(self, command, method="GET"):
        response = revertToVMSnapshotResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addBaremetalHost(self, command, method="GET"):
        response = addBaremetalHostResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addBaremetalPxeKickStartServer(self, command, method="GET"):
        response = addBaremetalPxeKickStartServerResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addBaremetalPxePingServer(self, command, method="GET"):
        response = addBaremetalPxePingServerResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addBaremetalDhcp(self, command, method="GET"):
        response = addBaremetalDhcpResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listBaremetalDhcp(self, command, method="GET"):
        response = listBaremetalDhcpResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listBaremetalPxeServers(self, command, method="GET"):
        response = listBaremetalPxeServersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addUcsManager(self, command, method="GET"):
        response = addUcsManagerResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listUcsManagers(self, command, method="GET"):
        response = listUcsManagersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listUcsProfiles(self, command, method="GET"):
        response = listUcsProfilesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listUcsBlades(self, command, method="GET"):
        response = listUcsBladesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def associateUcsProfileToBlade(self, command, method="GET"):
        response = associateUcsProfileToBladeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createLoadBalancer(self, command, method="GET"):
        response = createLoadBalancerResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listLoadBalancers(self, command, method="GET"):
        response = listLoadBalancersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteLoadBalancer(self, command, method="GET"):
        response = deleteLoadBalancerResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def configureInternalLoadBalancerElement(self, command, method="GET"):
        response = configureInternalLoadBalancerElementResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createInternalLoadBalancerElement(self, command, method="GET"):
        response = createInternalLoadBalancerElementResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listInternalLoadBalancerElements(self, command, method="GET"):
        response = listInternalLoadBalancerElementsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createAffinityGroup(self, command, method="GET"):
        response = createAffinityGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteAffinityGroup(self, command, method="GET"):
        response = deleteAffinityGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listAffinityGroups(self, command, method="GET"):
        response = listAffinityGroupsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def updateVMAffinityGroup(self, command, method="GET"):
        response = updateVMAffinityGroupResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listAffinityGroupTypes(self, command, method="GET"):
        response = listAffinityGroupTypesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def createPortableIpRange(self, command, method="GET"):
        response = createPortableIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deletePortableIpRange(self, command, method="GET"):
        response = deletePortableIpRangeResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listPortableIpRanges(self, command, method="GET"):
        response = listPortableIpRangesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def stopInternalLoadBalancerVM(self, command, method="GET"):
        response = stopInternalLoadBalancerVMResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def startInternalLoadBalancerVM(self, command, method="GET"):
        response = startInternalLoadBalancerVMResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listInternalLoadBalancerVMs(self, command, method="GET"):
        response = listInternalLoadBalancerVMsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listNetworkIsolationMethods(self, command, method="GET"):
        response = listNetworkIsolationMethodsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def dedicateZone(self, command, method="GET"):
        response = dedicateZoneResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def dedicatePod(self, command, method="GET"):
        response = dedicatePodResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def dedicateCluster(self, command, method="GET"):
        response = dedicateClusterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def dedicateHost(self, command, method="GET"):
        response = dedicateHostResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def releaseDedicatedZone(self, command, method="GET"):
        response = releaseDedicatedZoneResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def releaseDedicatedPod(self, command, method="GET"):
        response = releaseDedicatedPodResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def releaseDedicatedCluster(self, command, method="GET"):
        response = releaseDedicatedClusterResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def releaseDedicatedHost(self, command, method="GET"):
        response = releaseDedicatedHostResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDedicatedZones(self, command, method="GET"):
        response = listDedicatedZonesResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDedicatedPods(self, command, method="GET"):
        response = listDedicatedPodsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDedicatedClusters(self, command, method="GET"):
        response = listDedicatedClustersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listDedicatedHosts(self, command, method="GET"):
        response = listDedicatedHostsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listLdapConfigurations(self, command, method="GET"):
        response = listLdapConfigurationsResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def addLdapConfiguration(self, command, method="GET"):
        response = addLdapConfigurationResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def deleteLdapConfiguration(self, command, method="GET"):
        response = deleteLdapConfigurationResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def listLdapUsers(self, command, method="GET"):
        response = listLdapUsersResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def ldapCreateAccount(self, command, method="GET"):
        response = ldapCreateAccountResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def login(self, command, method="GET"):
        response = loginResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

    def logout(self, command, method="GET"):
        response = logoutResponse()
        response = self.connection.marvin_request(command, response_type=response, method=method)
        return response

