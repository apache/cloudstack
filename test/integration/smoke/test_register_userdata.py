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
#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             Account,
                             UserData)
from marvin.lib.common import get_test_template, get_zone, list_virtual_machines
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr

from marvin.lib.common import (get_domain, get_template)


class TestRegisteredUserdata(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestRegisteredUserdata, cls)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.testdata = self.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())

        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        #create a service offering
        small_service_offering = self.testdata["service_offerings"]["small"]
        small_service_offering['storagetype'] = 'local'
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            small_service_offering
        )
        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]


    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_CRUD_operations_userdata(self):

        self.userdata1 = UserData.register(
            self.apiclient,
            name="UserdataName",
            userdata="testUserdata1",
            account=self.account.name,
            domainid=self.account.domainid
        )

        list_userdata = UserData.list(self.apiclient, id=self.userdata1.userdata.id)

        self.debug(
            "List userdata response : %s" \
            % list_userdata
        )

        self.debug(
            "Verify listUserData response with id: %s" \
            % self.userdata1.userdata.id
        )

        self.assertNotEqual(
            len(list_userdata),
            0,
            "List userdata was empty"
        )

        userdata = list_userdata[0]
        self.assertEqual(
            userdata.id,
            self.userdata1.userdata.id,
            "userdata ids do not match"
        )

        deleteResponse = UserData.delete(
            self.apiclient,
            id=self.userdata1.userdata.id,
            account=self.account.name,
            domainid=self.account.domainid
        )

        list_userdata = UserData.list(self.apiclient, id=self.userdata1.userdata.id)

        self.assertEqual(
            len(list_userdata),
            0,
            "List userdata was empty"
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=True)
    def test_deploy_vm_with_registerd_userdata(self):

        self.userdata = UserData.register(
            self.apiclient,
            name="UserdataName",
            userdata="testUserdata",
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            userdataid=self.userdata.id
        )

        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s" \
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
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )
        self.assertEqual(
            vm.userdataid,
            self.userdata.id,
            "Virtual Machine names do not match"
        )
