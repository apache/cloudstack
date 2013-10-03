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
""" P1 tests for reset Vm on reboot
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr

_multiprocess_shared_ = True

class Services:
    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {

            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended in create account to
                # ensure unique username generated each time
                "password": "password",
            },
            "small":
            # Create a small virtual machine instance with disk offering
                {
                    "displayname": "testserver",
                    "username": "root", # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
            "service_offerings":
                {
                    "small":
                        {
                            # Small service offering ID to for change VM
                            # service offering from medium to small
                            "name": "SmallInstance_volatile",
                            "displaytext": "SmallInstance_volatile",
                            "cpunumber": 1,
                            "cpuspeed": 100,
                            "memory": 256,
                        },
                },
            #Change this
            "template": {
                "displaytext": "xs",
                "name": "xs",
                "passwordenabled": False,
            },
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
        }


class TestResetVmOnReboot(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestResetVmOnReboot, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client, cls.services)
        zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = zone.networktype

        template = get_template(
            cls.api_client,
            zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings ??
        cls.services["small"]["zoneid"] = zone.id
        cls.services["small"]["template"] = template.id

        # Create account, service offerings, vm.
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offerings"]["small"],
            isvolatile="true"
        )

        #create a virtual machine
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup = [
            cls.small_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        cls.api_client = super(TestResetVmOnReboot, cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(hypervisor="xenserver")
    @attr(tags=["advanced", "basic"])
    def test_01_reset_vm_on_reboot(self):
        """Test reset virtual machine on reboot
        """
        # Validate the following
        # create vm and list the volume for that VM. Reboot vm and check if root volume is different as before.

        volumelist_before_reboot = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )

        self.assertNotEqual(
            volumelist_before_reboot,
            None,
            "Check if volume is in listvolumes"
        )
        volume_before_reboot = volumelist_before_reboot[0]


        self.debug("Rebooting vm %s " % (self.virtual_machine.id))

        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.id = self.virtual_machine.id
        self.apiclient.rebootVirtualMachine(cmd)

        volumelist_after_reboot = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )

        self.assertNotEqual(
            volumelist_after_reboot,
            None,
            "Check if volume is in listvolumes"
        )

        volume_after_reboot = volumelist_after_reboot[0]
        self.assertNotEqual(
            volume_after_reboot.id,
            volume_before_reboot.id,
            "Check whether volumes are different before and after reboot"
        )

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            list_vm_response,
            None,
            "Check virtual machine is listVirtualMachines"
        )

        vm_response = list_vm_response[0]
        self.assertEqual(
                    vm_response.state,
                    'Running',
                    "Check the state of VM"
                )
        return
