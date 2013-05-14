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
""" Tests for configuring Internal Load Balancing Rules.
"""
#Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *


class TestInternalLb(cloudstackTestCase):
    networkOfferingId = None
    networkId = None
    vmId = None
    lbId = None

    zoneId = 1
    serviceOfferingId = 1 
    templateId = 5


    serviceProviderList = [
        {
            "provider": "VpcVirtualRouter",
            "service": "Vpn"
        },
        {
            "provider": "VpcVirtualRouter",
            "service": "UserData"
        },
        {
            "provider": "VpcVirtualRouter",
            "service": "Dhcp"
        },
        {
            "provider": "VpcVirtualRouter",
            "service": "Dns"
        },
        {
            "provider": "InternalLbVM",
            "service": "Lb"
        },
        {
            "provider": "VpcVirtualRouter",
            "service": "SourceNat"
        },
        {
            "provider": "VpcVirtualRouter",
            "service": "StaticNat"
        },
        {
            "provider": "VpcVirtualRouter",
            "service": "PortForwarding"
        },
        {
            "provider": "VpcVirtualRouter",
            "service": "NetworkACL"
        }
    ]

    serviceCapsList = [
        {
            "service": "SourceNat",
            "capabilitytype": "SupportedSourceNatTypes",
            "capabilityvalue": "peraccount"
        },
        {
            "service": "Lb",
            "capabilitytype": "SupportedLbIsolation",
            "capabilityvalue": "dedicated"
        },
        {
            "service": "Lb",
            "capabilitytype": "lbSchemes",
            "capabilityvalue": "internal"
        }
    ]

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()


    
    def test_internallb(self):

        #1) Create and enable network offering with Internal Lb vm service
        self.createNetworkOffering()
        
        #2) Create VPC and network in it
        self.createNetwork()
      
        #3) Deploy a vm 
        self.deployVm()
        
        #4) Create an Internal Load Balancer
        self.createInternalLoadBalancer()

        #5) Assign the VM to the Internal Load Balancer
        self.assignToLoadBalancerRule()

        #6) Remove the vm from the Interanl Load Balancer
        self.removeFromLoadBalancerRule()

        #7) Delete the Load Balancer
        self.deleteLoadBalancer()


    def deployVm(self):
        deployVirtualMachineCmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVirtualMachineCmd.networkids = TestInternalLb.networkId
        deployVirtualMachineCmd.serviceofferingid = TestInternalLb.serviceOfferingId
        deployVirtualMachineCmd.zoneid = TestInternalLb.zoneId
        deployVirtualMachineCmd.templateid = TestInternalLb.templateId
        deployVirtualMachineCmd.hypervisor = "XenServer"
        deployVMResponse = self.apiClient.deployVirtualMachine(deployVirtualMachineCmd)
        TestInternalLb.vmId = deployVMResponse.id


    def createInternalLoadBalancer(self):
        createLoadBalancerCmd = createLoadBalancer.createLoadBalancerCmd()
        createLoadBalancerCmd.name = "lb rule"
        createLoadBalancerCmd.sourceport = 22
        createLoadBalancerCmd.instanceport = 22
        createLoadBalancerCmd.algorithm = "roundrobin"
        createLoadBalancerCmd.scheme = "internal"
        createLoadBalancerCmd.sourceipaddressnetworkid = TestInternalLb.networkId
        createLoadBalancerCmd.networkid = TestInternalLb.networkId
        createLoadBalancerResponse = self.apiClient.createLoadBalancer(createLoadBalancerCmd)
        TestInternalLb.lbId = createLoadBalancerResponse.id
        self.assertIsNotNone(createLoadBalancerResponse.id, "Failed to create a load balancer")


    def assignToLoadBalancerRule(self):
        assignToLoadBalancerRuleCmd = assignToLoadBalancerRule.assignToLoadBalancerRuleCmd()
        assignToLoadBalancerRuleCmd.id = TestInternalLb.lbId
        assignToLoadBalancerRuleCmd.virtualMachineIds = TestInternalLb.vmId
        assignToLoadBalancerRuleResponse = self.apiClient.assignToLoadBalancerRule(assignToLoadBalancerRuleCmd)
        self.assertTrue(assignToLoadBalancerRuleResponse.success, "Failed to assign the vm to the load balancer")



    def removeFromLoadBalancerRule(self):
        removeFromLoadBalancerRuleCmd = removeFromLoadBalancerRule.removeFromLoadBalancerRuleCmd()
        removeFromLoadBalancerRuleCmd.id = TestInternalLb.lbId
        removeFromLoadBalancerRuleCmd.virtualMachineIds = TestInternalLb.vmId
        removeFromLoadBalancerRuleResponse = self.apiClient.removeFromLoadBalancerRule(removeFromLoadBalancerRuleCmd)
        self.assertTrue(removeFromLoadBalancerRuleResponse.success, "Failed to remove the vm from the load balancer")



    #def removeInternalLoadBalancer(self):
    def deleteLoadBalancer(self):
        deleteLoadBalancerCmd = deleteLoadBalancer.deleteLoadBalancerCmd()
        deleteLoadBalancerCmd.id = TestInternalLb.lbId
        deleteLoadBalancerResponse = self.apiClient.deleteLoadBalancer(deleteLoadBalancerCmd)
        self.assertTrue(deleteLoadBalancerResponse.success, "Failed to remove the load balancer")



    def createNetwork(self):
        createVPCCmd = createVPC.createVPCCmd()
        createVPCCmd.name = "new vpc"
        createVPCCmd.cidr = "10.1.1.0/24"
        createVPCCmd.displaytext = "new vpc"
        createVPCCmd.vpcofferingid = 1
        createVPCCmd.zoneid = self.zoneId
        createVPCResponse = self.apiClient.createVPC(createVPCCmd)


        createNetworkCmd = createNetwork.createNetworkCmd()
        createNetworkCmd.name = "vpc network"
        createNetworkCmd.displaytext = "vpc network"
        createNetworkCmd.netmask = "255.255.255.0"
        createNetworkCmd.gateway = "10.1.1.1"
        createNetworkCmd.zoneid = self.zoneId
        createNetworkCmd.vpcid = createVPCResponse.id
        createNetworkCmd.networkofferingid = TestInternalLb.networkOfferingId
        createNetworkResponse = self.apiClient.createNetwork(createNetworkCmd)
        TestInternalLb.networkId = createNetworkResponse.id

        self.assertIsNotNone(createNetworkResponse.id, "Network failed to create")


    def createNetworkOffering(self):
            createNetworkOfferingCmd = createNetworkOffering.createNetworkOfferingCmd()
            createNetworkOfferingCmd.name = "Network offering for internal lb service - " + str(random.randrange(1,100+1))
            createNetworkOfferingCmd.displaytext = "Network offering for internal lb service"
            createNetworkOfferingCmd.guestiptype = "isolated"
            createNetworkOfferingCmd.traffictype = "Guest"
            createNetworkOfferingCmd.conservemode = "false"
            createNetworkOfferingCmd.supportedservices = "Vpn,Dhcp,Dns,Lb,UserData,SourceNat,StaticNat,PortForwarding,NetworkACL"


            createNetworkOfferingCmd.serviceproviderlist = []
            for item in self.serviceProviderList:
                createNetworkOfferingCmd.serviceproviderlist.append({
                                                'service': item['service'],
                                                'provider': item['provider']
                                               })
                
            createNetworkOfferingCmd.servicecapabilitylist = []
            for item in self.serviceCapsList:
                createNetworkOfferingCmd.servicecapabilitylist.append({
                                                'service': item['service'],
                                                'capabilitytype': item['capabilitytype'],
                                                'capabilityvalue': item['capabilityvalue']
                                               })


            createNetworkOfferingResponse = self.apiClient.createNetworkOffering(createNetworkOfferingCmd)
            TestInternalLb.networkOfferingId = createNetworkOfferingResponse.id

            #enable network offering
            updateNetworkOfferingCmd = updateNetworkOffering.updateNetworkOfferingCmd()
            updateNetworkOfferingCmd.id = TestInternalLb.networkOfferingId
            updateNetworkOfferingCmd.state = "Enabled"
            updateNetworkOfferingResponse = self.apiClient.updateNetworkOffering(updateNetworkOfferingCmd)


            #list network offering to see if its enabled
            listNetworkOfferingsCmd = listNetworkOfferings.listNetworkOfferingsCmd()
            listNetworkOfferingsCmd.id = TestInternalLb.networkOfferingId
            listOffResponse = self.apiClient.listNetworkOfferings(listNetworkOfferingsCmd)

            self.assertNotEqual(len(listOffResponse), 0, "Check if the list network offerings API \
                                returns a non-empty response")


    def tearDown(self):
        #destroy the vm
        if TestInternalLb.vmId is not None:
            destroyVirtualMachineCmd = destroyVirtualMachine.destroyVirtualMachineCmd()
            destroyVirtualMachineCmd.id = TestInternalLb.vmId
            destroyVirtualMachineResponse = self.apiClient.destroyVirtualMachine(destroyVirtualMachineCmd)
