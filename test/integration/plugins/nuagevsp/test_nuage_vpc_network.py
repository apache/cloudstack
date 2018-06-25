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

""" Component tests for basic VPC Network functionality with
Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import Account, VPC
# Import System Modules
from nose.plugins.attrib import attr


class TestNuageVpcNetwork(nuageTestCase):
    """ Test basic VPC Network functionality with Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls, zone=None):
        super(TestNuageVpcNetwork, cls).setUpClass()
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
        """ Test basic VPC Network functionality with Nuage VSP SDN plugin
        """

        # 1. Create Nuage VSP VPC offering, check if it is successfully
        #    created and enabled.
        # 2. Create a VPC with Nuage VSP VPC offering, check if it is
        #    successfully created and enabled.
        # 3. Create Nuage VSP VPC Network offering, check if it is successfully
        #    created and enabled.
        # 4. Create an ACL list in the created VPC, and add an ACL item to it.
        # 5. Create a VPC Network with Nuage VSP VPC Network offering and the
        #    created ACL list, check if it is successfully created, is in the
        #    "Implemented" state, and is added to the VPC VR.
        # 6. Verify that the VPC VR has no Public IP and NIC as it is not the
        #    Source NAT service provider.
        # 7. Deploy a VM in the created VPC network, check if the VM is
        #    successfully deployed and is in the "Running" state.
        # 8. Verify that the created ACL item is successfully implemented in
        #    Nuage VSP.
        # 9. Delete all the created objects (cleanup).

        # Creating a VPC offering
        self.debug("Creating Nuage VSP VPC offering...")
        vpc_offering = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_offering, state="Enabled")

        # Creating a VPC
        self.debug("Creating a VPC with Nuage VSP VPC offering...")
        vpc = self.create_Vpc(vpc_offering, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating a network offering
        self.debug("Creating Nuage VSP VPC Network offering...")
        network_offering = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")

        # Creating an ACL list
        acl_list = self.create_NetworkAclList(
            name="acl", description="acl", vpc=vpc)

        # Creating an ACL item
        acl_item = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], acl_list=acl_list)

        # Creating a VPC network in the VPC
        self.debug("Creating a VPC network with Nuage VSP VPC Network "
                   "offering...")
        vpc_network = self.create_Network(
            network_offering, vpc=vpc, acl_list=acl_list)
        self.validate_Network(vpc_network, state="Implemented")
        vr = self.get_Router(vpc_network)
        self.check_Router_state(vr, state="Running")

        # Verifying that the VPC VR has no public IP and NIC
        self.verify_VRWithoutPublicIPNIC(vr)
        # Verifying that the VPC has no src NAT ip
        self.verify_vpc_has_no_src_nat(vpc)

        # Deploying a VM in the VPC network
        vm = self.create_VM(vpc_network)
        self.check_VM_state(vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_network, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm)

        # VSD verification for ACL item
        self.verify_vsd_firewall_rule(acl_item)

        self.restart_Vpc(vpc, cleanup=True)

        self.validate_Network(vpc_network, state="Implemented")
        vr = self.get_Router(vpc_network)
        self.verify_vsd_router(vr)

    @attr(
        tags=["advanced", "nuagevsp", "multizone"], required_hardware="false")
    def test_nuage_vpc_network_multizone(self):
        """ Test basic VPC Network functionality with Nuage VSP SDN plugin on
        multiple zones
        """

        # Repeat the tests in the above testcase "test_nuage_vpc_network" on
        # multiple zones

        self.debug("Testing basic VPC Network functionality with Nuage VSP "
                   "SDN plugin on multiple zones...")
        if len(self.zones) == 1:
            self.skipTest("There is only one Zone configured: skipping test")
        for zone in self.zones:
            self.debug("Zone - %s" % zone.name)
            # Get Zone details
            self.getZoneDetails(zone=zone)
            # Configure VSD sessions
            self.configureVSDSessions()
            self.test_nuage_vpc_network()
