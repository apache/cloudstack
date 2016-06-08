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
from marvin.lib.base import Account, Nuage
from marvin.cloudstackAPI import deleteNuageVspDevice
# Import System Modules
from nose.plugins.attrib import attr
import copy


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

    # validate_NuageVspDevice - Validates the addition of Nuage VSP device in the Nuage VSP Physical Network
    def validate_NuageVspDevice(self):
        """Validates the addition of Nuage VSP device in the Nuage VSP Physical Network"""
        self.debug("Validating the addition of Nuage VSP device in the Nuage VSP Physical Network - %s" %
                   self.vsp_physical_network.id)
        nuage_vsp_device = Nuage.list(self.api_client,
                                      physicalnetworkid=self.vsp_physical_network.id
                                      )
        self.assertEqual(isinstance(nuage_vsp_device, list), True,
                         "List Nuage VSP device should return a valid list"
                         )
        self.debug("Successfully validated the addition of Nuage VSP device in the Nuage VSP Physical Network - %s" %
                   self.vsp_physical_network.id)

    # delete_NuageVspDevice - Deletes the Nuage VSP device in the Nuage VSP Physical Network
    def delete_NuageVspDevice(self):
        """Deletes the Nuage VSP device in the Nuage VSP Physical Network"""
        self.debug("Deleting the Nuage VSP device in the Nuage VSP Physical Network - %s" %
                   self.vsp_physical_network.id)
        nuage_vsp_device = Nuage.list(self.api_client,
                                      physicalnetworkid=self.vsp_physical_network.id
                                      )[0]
        cmd = deleteNuageVspDevice.deleteNuageVspDeviceCmd()
        cmd.vspdeviceid = nuage_vsp_device.vspdeviceid
        self.api_client.deleteNuageVspDevice(cmd)
        self.debug("Successfully deleted the Nuage VSP device in the Nuage VSP Physical Network - %s" %
                   self.vsp_physical_network.id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_nuage_vsp_device(self):
        """ Test Nuage VSP device in the Nuage VSP Physical Network
        """

        # 1. Verify that the Nuage VSP network service provider is successfully created and enabled in the Nuage VSP
        #    Physical Network.
        # 2. Verify that the Nuage VSP device is successfully created in the Nuage VSP Physical Network.
        # 3. Delete the Nuage VSP device in the Nuage VSP Physical Network, verify that the Nuage VSP device is
        #    successfully deleted in the Nuage VSP Physical Network.
        # 4. Add the Nuage VSP device in the Nuage VSP Physical Network with invalid VSD credentials, verify that the
        #    Nuage VSP device failed to add in the Nuage VSP Physical Network.
        # 5. Add the Nuage VSP device in the Nuage VSP Physical Network with valid VSD credentials, verify that the
        #    Nuage VSP device is successfully added in the Nuage VSP Physical Network.

        # Nuage VSP network service provider validation
        self.debug("Validating the Nuage VSP network service provider in the Nuage VSP Physical Network...")
        self.validate_NetworkServiceProvider("NuageVsp", state="Enabled")

        # Nuage VSP device validation
        self.debug("Validating the Nuage VSP device in the Nuage VSP Physical Network...")
        self.validate_NuageVspDevice()

        # Nuage VSP device deletion
        self.debug("Deleting the Nuage VSP device in the Nuage VSP Physical Network...")
        self.delete_NuageVspDevice()

        # Nuage VSP device validation
        self.debug("Validating the Nuage VSP device in the Nuage VSP Physical Network...")
        with self.assertRaises(Exception):
            self.validate_NuageVspDevice()
        self.debug("Successfully deleted the Nuage VSP device in the Nuage VSP Physical Network")

        # Adding the Nuage VSP device with invalid VSD credentials
        self.debug("Adding the Nuage VSP device in the Nuage VSP Physical Network with invalid VSD credentials...")
        vsd_info = self.nuage_vsp_device.__dict__
        invalid_vsd_info = copy.deepcopy(vsd_info)
        invalid_vsd_info["password"] = ""
        with self.assertRaises(Exception):
            Nuage.add(self.api_client, invalid_vsd_info, self.vsp_physical_network.id)
        self.debug("Failed to add the Nuage VSP device in the Nuage VSP Physical Network due to invalid VSD "
                   "credentials")

        # Nuage VSP device validation
        self.debug("Validating the Nuage VSP device in the Nuage VSP Physical Network...")
        with self.assertRaises(Exception):
            self.validate_NuageVspDevice()
        self.debug("The Nuage VSP device is not added in the Nuage VSP Physical Network")

        # Adding the Nuage VSP device with valid VSD credentials
        self.debug("Adding the Nuage VSP device in the Nuage VSP Physical Network with valid VSD credentials...")
        Nuage.add(self.api_client, vsd_info, self.vsp_physical_network.id)

        # Nuage VSP device validation
        self.debug("Validating the Nuage VSP device in the Nuage VSP Physical Network...")
        self.validate_NuageVspDevice()

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
        # 7. Delete all the created objects (cleanup).

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
        self.verify_vsd_network(self.domain.id, network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)

        # Deploying one more VM in the network
        vm_2 = self.create_VM(network)
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vm_2)

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
            self.verify_vsd_network(self.domain.id, network)
        self.debug("Isolated Network successfully deleted in VSD")
