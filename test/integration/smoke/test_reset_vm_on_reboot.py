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
from marvin.codes import FAILED
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

_multiprocess_shared_ = True

class TestResetVmOnReboot(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        testClient = super(TestResetVmOnReboot, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()        

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = zone.networktype

        template = get_template(
            cls.apiclient,
            zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings ??
        cls.services["small"]["zoneid"] = zone.id
        cls.services["small"]["template"] = template.id

        # Create account, service offerings, vm.
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"],
            isvolatile="true"
        )

        #create a virtual machine
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
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
        cls.apiclient = super(TestResetVmOnReboot, cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.apiclient, cls._cleanup)
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
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_reset_vm_on_reboot(self):
    #TODO: SIMENH: add new test to check volume contents
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
