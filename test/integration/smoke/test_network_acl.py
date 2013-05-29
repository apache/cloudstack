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
""" Tests for Network ACLs in VPC
"""
#Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr

class TestNetworkACL(cloudstackTestCase):
    networkOfferingId = 11
    networkId = None
    vmId = None
    vpcId = None
    aclId = None

    zoneId = 1
    serviceOfferingId = 1 
    templateId = 5

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()


    
    @attr(tags=["advanced"])
    def test_networkAcl(self):

        # 1) Create VPC
        self.createVPC()

        # 2) Create ACl
        self.createACL()

        # 3) Create ACl Item
        self.createACLItem()

        # 4) Create network with ACL
        self.createNetwork()
        # 5) Deploy a vm
        self.deployVm()

    def createACL(self):
        createAclCmd = createNetworkACLList.createNetworkACLListCmd()
        createAclCmd.name = "acl1"
        createAclCmd.description = "new acl"
        createAclCmd.vpcId = TestNetworkACL.vpcId
        createAclResponse = self.apiClient.createNetworkACLList(createAclCmd)
        TestNetworkACL.aclId = createAclResponse.id

    def createACLItem(self):
        createAclItemCmd = createNetworkACL.createNetworkACLCmd()
        createAclItemCmd.cidr = "0.0.0.0/0"
        createAclItemCmd.protocol = "TCP"
        createAclItemCmd.number = "10"
        createAclItemCmd.action = "Deny"
        createAclItemCmd.aclId = TestNetworkACL.aclId
        createAclItemResponse = self.apiClient.createNetworkACL(createAclItemCmd)
        self.assertIsNotNone(createAclItemResponse.id, "Network failed to aclItem")

    def createVPC(self):
        createVPCCmd = createVPC.createVPCCmd()
        createVPCCmd.name = "new vpc"
        createVPCCmd.cidr = "10.1.1.0/24"
        createVPCCmd.displaytext = "new vpc"
        createVPCCmd.vpcofferingid = 1
        createVPCCmd.zoneid = self.zoneId
        createVPCResponse = self.apiClient.createVPC(createVPCCmd)
        TestNetworkACL.vpcId = createVPCResponse.id


    def createNetwork(self):
        createNetworkCmd = createNetwork.createNetworkCmd()
        createNetworkCmd.name = "vpc network"
        createNetworkCmd.displaytext = "vpc network"
        createNetworkCmd.netmask = "255.255.255.0"
        createNetworkCmd.gateway = "10.1.1.1"
        createNetworkCmd.zoneid = self.zoneId
        createNetworkCmd.vpcid = TestNetworkACL.vpcId
        createNetworkCmd.networkofferingid = TestNetworkACL.networkOfferingId
        createNetworkCmd.aclId = TestNetworkACL.aclId
        createNetworkResponse = self.apiClient.createNetwork(createNetworkCmd)
        TestNetworkACL.networkId = createNetworkResponse.id

        self.assertIsNotNone(createNetworkResponse.id, "Network failed to create")

    def deployVm(self):
        deployVirtualMachineCmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVirtualMachineCmd.networkids = TestNetworkACL.networkId
        deployVirtualMachineCmd.serviceofferingid = TestNetworkACL.serviceOfferingId
        deployVirtualMachineCmd.zoneid = TestNetworkACL.zoneId
        deployVirtualMachineCmd.templateid = TestNetworkACL.templateId
        deployVirtualMachineCmd.hypervisor = "XenServer"
        deployVMResponse = self.apiClient.deployVirtualMachine(deployVirtualMachineCmd)
        TestNetworkACL.vmId = deployVMResponse.id

    def tearDown(self):
        #destroy the vm
        if TestNetworkACL.vmId is not None:
            destroyVirtualMachineCmd = destroyVirtualMachine.destroyVirtualMachineCmd()
            destroyVirtualMachineCmd.id = TestNetworkACL.vmId
            destroyVirtualMachineResponse = self.apiClient.destroyVirtualMachine(destroyVirtualMachineCmd)
