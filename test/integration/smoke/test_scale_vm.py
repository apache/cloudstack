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
""" P1 tests for Scaling up Vm
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

class TestScaleVm(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        testClient = super(TestScaleVm, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ('kvm', 'hyperv', 'lxc'):
            raise unittest.SkipTest("ScaleVM is not supported on KVM, Hyper-V or LXC. Hence, skipping the test")

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
            cls.services["service_offerings"]["small"]
        )

        cls.big_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["big"]
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
        cls.apiclient = super(TestScaleVm, cls).getClsTestClient().getApiClient()
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
    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_scale_vm(self):
        """Test scale virtual machine 
        """
        # Validate the following
        # Scale up the vm and see if it scales to the new svc offering and is finally in running state

        #        VirtualMachine should be updated to tell cloudstack it has PV tools
        #        available and successfully scaled. We will only mock that behaviour
        #        here but it is not expected in production since the VM scaling is not
        #        guaranteed until tools are installed, vm rebooted
        self.virtual_machine.update(self.apiclient, isdynamicallyscalable='true')

        self.debug("Scaling VM-ID: %s to service offering: %s and state %s" % (
            self.virtual_machine.id,
            self.big_offering.id,
            self.virtual_machine.state
            ))

        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.big_offering.id
        cmd.id = self.virtual_machine.id
        self.apiclient.scaleVirtualMachine(cmd)

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
            vm_response.id,
            self.virtual_machine.id,
            "Check virtual machine ID of scaled VM"
        )

        self.debug("Scaling VM-ID: %s from service offering: %s to new service offering %s and the response says %s" % (
            self.virtual_machine.id,
            self.virtual_machine.serviceofferingid,
            self.big_offering.id,
            vm_response.serviceofferingid
            ))
        self.assertEqual(
            vm_response.serviceofferingid,
            self.big_offering.id,
            "Check service offering of the VM"
        )

        self.assertEqual(
            vm_response.state,
            'Running',
            "Check the state of VM"
        )
        return
