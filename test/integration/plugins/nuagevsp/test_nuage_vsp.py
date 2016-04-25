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

""" P1 tests for Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import Account
# Import System Modules
from nose.plugins.attrib import attr


class TestNuageVsp(nuageTestCase):
    """ Test Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls):
        super(TestNuageVsp, cls).setUpClass()
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
    def test_nuage_vsp(self):
        """ Test Nuage VSP SDN plugin with basic Isolated Network functionality
        """

        # 1. Verify that the Nuage VSP network service provider is successfully created and enabled.
        # 2. Create and enable Nuage VSP Isolated Network offering, check if it is successfully created and enabled.
        # 3. Create an Isolated Network with Nuage VSP Isolated Network offering, check if it is successfully created
        #    and is in the "Allocated" state.
        # 4. Deploy a VM in the created Isolated network, check if the Isolated network state is changed to
        #    "Implemented", and both the VM & VR are successfully deployed and are in the "Running" state.
        # 5. Deploy one more VM in the created Isolated network, check if the VM is successfully deployed and is in the
        #    "Running" state.
        # 6. Delete the created Isolated Network after destroying its VMs, check if the Isolated network is successfully
        #    deleted.

        self.debug("Validating the Nuage VSP network service provider...")
        self.validate_NetworkServiceProvider("NuageVsp", state="Enabled")

        # Creating a network offering
        self.debug("Creating and enabling Nuage VSP Isolated Network offering...")
        network_offering = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")

        # Creating a network
        self.debug("Creating an Isolated Network with Nuage VSP Isolated Network offering...")
        network = self.create_Network(network_offering)
        self.validate_Network(network, state="Allocated")

        # Deploying a VM in the network
        vm_1 = self.create_VM(network)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")

        # VSD verification
        self.verify_vsp_network(self.domain.id, network)
        self.verify_vsp_router(vr)
        self.verify_vsp_vm(vm_1)

        # Deploying one more VM in the network
        vm_2 = self.create_VM(network)
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsp_vm(vm_2)

        # Deleting the network
        self.debug("Deleting the Isolated Network with Nuage VSP Isolated Network offering...")
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_Network(network)
        with self.assertRaises(Exception):
            self.validate_Network(network)
        self.debug("Isolated Network successfully deleted in CloudStack")

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsp_network(self.domain.id, network)
        self.debug("Isolated Network successfully deleted in VSD")
