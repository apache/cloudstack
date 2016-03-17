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

""" Tests for Basic VPC Network Functionality with NuageVsp network Plugin
"""
# Import Local Modules
from marvin.lib.base import Account
from nose.plugins.attrib import attr
from nuageTestCase import nuageTestCase


class TestVpcNetworkNuage(nuageTestCase):
    """ Test Basic VPC Network Functionality with NuageVsp network Plugin
    """

    @classmethod
    def setUpClass(cls):
        super(TestVpcNetworkNuage, cls).setUpClass()
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.cleanup = [self.account]
        return

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_nuage_vpc_network(self):
        """ Test Basic VPC Network Functionality with NuageVsp network Plugin
        """

        # 1. Create Nuage VSP VPC offering, check if it is successfully created and enabled.
        # 2. Create a VPC with Nuage VSP VPC offering, check if it is successfully created and enabled.
        # 3. Create Nuage Vsp VPC Network offering, check if it is successfully created and enabled.
        # 4. Create an ACL list in the created VPC, and add an ACL item to it.
        # 5. Create a VPC Network with Nuage Vsp VPC Network offering and the created ACL list, check if it is
        #    successfully created, is in the "Implemented" state, and is added to the VPC VR.
        # 6. Deploy a VM in the created VPC network, check if the VM is successfully deployed and is in the "Running"
        #    state.
        # 7. Verify that the created ACL item is successfully implemented in Nuage Vsp.

        # Creating a VPC offering
        self.debug("Creating Nuage VSP VPC offering...")
        vpc_offering = self.create_VpcOffering(self.test_data["nuage_vsp_services"]["vpc_offering"])
        self.validate_vpc_offering(vpc_offering, state="Enabled")

        # Creating a VPC
        self.debug("Creating a VPC with Nuage VSP VPC offering...")
        vpc = self.create_Vpc(vpc_offering, cidr='10.1.0.0/16')
        self.validate_vpc(vpc, state="Enabled")

        # Creating a network offering
        self.debug("Creating Nuage Vsp VPC Network offering...")
        network_offering = self.create_NetworkOffering(self.test_data["nuage_vsp_services"]["vpc_network_offering"])
        self.validate_network_offering(network_offering, state="Enabled")

        # Creating an ACL list
        acl_list = self.create_network_acl_list(name="acl", description="acl", vpc=vpc)

        # Creating an ACL item
        acl_item = self.create_network_acl_rule(self.test_data["ingress_rule"], acl_list=acl_list)

        # Creating a VPC network in the VPC
        self.debug("Creating a VPC network with Nuage Vsp VPC Network offering...")
        vpc_network = self.create_Network(network_offering, gateway='10.1.1.1', vpc=vpc, acl_list=acl_list)
        self.validate_network(vpc_network, state="Implemented")
        vr = self.get_network_router(vpc_network)
        self.check_router_state(vr, state="Running")

        # Deploying a VM in the VPC network
        vm = self.create_VM_in_Network(vpc_network)
        self.check_vm_state(vm, state="Running")

        # VSPK verification
        self.verify_vsp_network(self.domain.id, vpc_network, vpc)
        self.verify_vsp_router(vr)
        self.verify_vsp_vm(vm)

        # VSPK verification for ACL item
        self.verify_vsp_firewall_rule(acl_item)
