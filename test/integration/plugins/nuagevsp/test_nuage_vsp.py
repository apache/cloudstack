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

""" P1 tests for NuageVsp network Plugin
"""
# Import Local Modules
from marvin.lib.base import Account
from nose.plugins.attrib import attr
from nuageTestCase import nuageTestCase


class TestNuageVsp(nuageTestCase):
    """ Test NuageVsp network plugin
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
        """ Test NuageVsp network plugin with basic Isolated Network functionality
        """

        # 1. Verify that the NuageVsp network service provider is successfully created and enabled.
        # 2. Create and enable Nuage Vsp Isolated Network offering, check if it is successfully created and enabled.
        # 3. Create an Isolated Network with Nuage Vsp Isolated Network offering, check if it is successfully created
        #    and is in the "Allocated" state.
        # 4. Deploy a VM in the created Isolated network, check if the Isolated network state is changed to
        #    "Implemented", and both the VM & VR are successfully deployed and are in the "Running" state.
        # 5. Deploy one more VM in the created Isolated network, check if the VM is successfully deployed and is in the
        #    "Running" state.
        # 6. Delete the created Isolated Network after destroying its VMs, check if the Isolated network is successfully
        #    deleted.

        self.debug("Validating the NuageVsp network service provider...")
        self.validate_NetworkServiceProvider("NuageVsp", state="Enabled")

        # Creating a network offering
        self.debug("Creating and enabling Nuage Vsp Isolated Network offering...")
        network_offering = self.create_NetworkOffering(
            self.test_data["nuage_vsp_services"]["isolated_network_offering"])
        self.validate_network_offering(network_offering, state="Enabled")

        # Creating a network
        self.debug("Creating an Isolated Network with Nuage Vsp Isolated Network offering...")
        network = self.create_Network(network_offering, gateway='10.1.1.1')
        self.validate_network(network, state="Allocated")

        # Deploying a VM in the network
        vm_1 = self.create_VM_in_Network(network)
        self.validate_network(network, state="Implemented")
        vr = self.get_network_router(network)
        self.check_router_state(vr, state="Running")
        self.check_vm_state(vm_1, state="Running")

        # VSPK verification
        self.verify_vsp_network(self.domain.id, network)
        self.verify_vsp_router(vr)
        self.verify_vsp_vm(vm_1)

        # Deploying one more VM in the network
        vm_2 = self.create_VM_in_Network(network)
        self.check_vm_state(vm_2, state="Running")

        # VSPK verification
        self.verify_vsp_vm(vm_2)

        # Deleting the network
        self.debug("Deleting the Isolated Network with Nuage Vsp Isolated Network offering...")
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_Network(network)
        with self.assertRaises(Exception):
            self.validate_network(network)
        self.debug("Isolated Network successfully deleted in CloudStack")

        # VSPK verification
        with self.assertRaises(Exception):
            self.verify_vsp_network(self.domain.id, network)
        self.debug("Isolated Network successfully deleted in VSD")
