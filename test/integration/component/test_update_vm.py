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


from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.integration.lib.base import Account, VirtualMachine, ServiceOffering
from marvin.integration.lib.utils import cleanup_resources
from marvin.integration.lib.common import get_zone, get_domain, get_template
from nose.plugins.attrib import attr

class TestData(object):
    """Test data object that is required to create resources
    """
    def __init__(self):
        self.testdata = {
            #data to create an account
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
            },
            #data reqd for virtual machine creation
            "virtual_machine" : {
            },
            #small service offering
            "service_offering": {
                "small": {
                    "name": "Small Instance",
                    "displaytext": "Small Instance",
                    "cpunumber": 1,
                    "cpuspeed": 100,
                    "memory": 256,
                },
            },
            "ostype": 'CentOS 5.3 (64-bit)',
        }


class TestUpdateVirtualMachine(cloudstackTestCase):
    """Test deploy a VM into a user account
    """

    def setUp(self):
        self.testdata = TestData().testdata
        self.apiclient = self.testClient.getApiClient()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient, self.testdata)
        self.zone = get_zone(self.apiclient, self.testdata)
        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        #create a service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]["small"]
        )
        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]

    @attr(tags = ['advanced', 'simulator', 'basic', 'sg'])
    def test_update_vm_name(self):
        """Test Update VirtualMachine Name

        # Validate the following:
        # 1. VirtualMachine has uuid name, displayname
        # 2. listVirtualMachines returns accurate information
        # 3. Stop the VM
        # 4. updateVirtualmachine no args and then new displayname
        # 5. listVirtualMachines nad check the displayName set
        # 6. start the VM
        # 7. Verify displayName is still set
        """
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

        self.debug(
            "VirtualMachine launched with id, name, displayname: %s %s %s"\
            % (self.virtual_machine.id, vm.name, vm.displayname)
        )

        self.assertEqual(
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )
        self.debug("Stopping VirtualMachine to update displayname")

        self.virtual_machine.stop(self.apiclient)
        #CLOUDSTACK-3184: update without args as this results in an NPE
        self.virtual_machine.update(self.apiclient)

        self.virtual_machine.update(self.apiclient, displayname='newdisplayname')
        list_vms = VirtualMachine.list(self.apiclient, id=vm.id)
        vmnew = list_vms[0]

        self.assertNotEqual(vmnew.displayname, vm.displayname,
            msg="displayname remained the same after updateVirtualMachine")
        self.assertEqual(vmnew.displayname, 'newdisplayname',
            msg="display name not updated successfully, displayname is %s" % vmnew.displayname)

        self.debug("Starting VirtualMachine after updated displayname")
        self.virtual_machine.start(self.apiclient)
        list_vms = VirtualMachine.list(self.apiclient, id=vm.id)
        vmnewstarted = list_vms[0]

        self.assertEqual(vmnew.displayname, vmnewstarted.displayname,
            msg="display name changed on start, displayname is %s" % vmnewstarted.displayname)

    def tearDown(self):
        try:
           cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)