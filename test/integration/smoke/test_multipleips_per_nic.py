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

#Test from the Marvin - Testing in Python wiki
from marvin.codes import FAILED

#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

#Import Integration Libraries

#base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, VirtualMachine, ServiceOffering

#utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources

#common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template

from marvin.cloudstackAPI.addIpToNic import addIpToNicCmd
from marvin.cloudstackAPI.removeIpFromNic import removeIpFromNicCmd
from marvin.cloudstackAPI.listNics import listNicsCmd


from nose.plugins.attrib import attr

class TestDeployVM(cloudstackTestCase):
    """Test deploy a VM into a user account
    """

    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        if self.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % self.testdata["ostype"]

        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        #create a service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offerings"]["small"]
        )
        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]

        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. listVirtualMachines returns accurate information

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id
        )

        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"\
            % self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(list_vms, list),
            True,
            "List VM response was not a valid list"
        )
        self.assertNotEqual(
            len(list_vms),
            0,
            "List VM response was empty"
        )

        vm = list_vms[0]
        self.assertEqual(
            vm.id,
            self.virtual_machine.id,
            "Virtual Machine ids do not match"
        )
        self.assertEqual(
            vm.name,
            self.virtual_machine.name,
            "Virtual Machine names do not match"
        )
        self.assertEqual(
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )

    @attr(tags = ['advanced',  'basic'], required_hardware="false")
    def test_nic_secondaryip_add_remove(self):
    #TODO: SIMENH: add verification
        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)
        vm = list_vms[0]
        nicid = vm.nic[0].id
        cmd = addIpToNicCmd()
        cmd.nicid = nicid
        response =  self.apiclient.addIpToNic(cmd)
        self.debug('IP address acquired to nic is =%s' % response.ipaddress)

        #remove the ip from nic
        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)
        vmid = self.virtual_machine.id
        cmd = listNicsCmd()
        cmd.virtualmachineid = vmid
        list_nics =  self.apiclient.listNics(cmd)

        nic = list_nics[0]
        ipid = nic.secondaryip[0].id;

        cmd = removeIpFromNicCmd()
        cmd.id = ipid
        response =  self.apiclient.removeIpFromNic(cmd)

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
