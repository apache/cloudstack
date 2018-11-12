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

from nose.plugins.attrib import attr

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Domain,
                             Account,
                             ServiceOffering,
                             VirtualMachine, updateResourceCount)
from marvin.lib.common import (get_zone,
                               get_test_template)
from marvin.lib.utils import (cleanup_resources)


class Services:
    """Test Account Services
    """

    def __init__(self):
        self.services = {
            "domain": {
                "name": "Domain",
            },
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "fr3sca",
            },
            "user": {
                "email": "user@test.com",
                "firstname": "User",
                "lastname": "User",
                "username": "User",
                # Random characters are appended for unique
                # username
                "password": "fr3sca",
            },
            "service_offering_it_1": {
                "name": "InstanceType-1",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
            },
            "service_offering_it_2": {
                "name": "InstanceType-2",
                "displaytext": "Tiny Instance",
                "cpunumber": 2,
                "cpuspeed": 100,
                "memory": 512,
            },
            "virtual_machine_1": {
                "displayname": "Test VM1",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "virtual_machine_2": {
                "displayname": "Test VM2",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "template": {
                "displaytext": "Public Template",
                "name": "Public template",
                "ostype": 'CentOS 5.6 (64-bit)',
                "url": "",
                "hypervisor": '',
                "format": '',
                "isfeatured": True,
                "ispublic": True,
                "isextractable": True,
                "templatefilter": "self"
            },
            "natrule": {
                "publicport": 22,
                "privateport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.6 (64-bit)',
            "sleep": 60,
            "timeout": 10,
        }


class TestRAMCPUResourceAccounting(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestRAMCPUResourceAccounting,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        # Create an account, domain etc
        cls.domain = Domain.create(
            cls.api_client,
            cls.services["domain"],
        )
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )

        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor)

        cls.services["virtual_machine_1"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine_1"]["template"] = cls.template.id

        cls.services["virtual_machine_2"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine_2"]["template"] = cls.template.id

        cls._cleanup = [
            cls.account,
            cls.domain
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def get_resource_amount(self, resource_type):
        cmd = updateResourceCount.updateResourceCountCmd()
        cmd.account = self.account.name
        cmd.domainid = self.domain.id
        cmd.resourcetype = resource_type
        response = self.apiclient.updateResourceCount(cmd)
        amount = response[0].resourcecount
        return amount

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_so_removal_resource_update(self):

        self.service_offering_it_1 = ServiceOffering.create(
            self.api_client,
            self.services["service_offering_it_1"],
            domainid=self.domain.id
        )

        self.cleanup.append(self.service_offering_it_1)

        self.service_offering_it_2 = ServiceOffering.create(
            self.api_client,
            self.services["service_offering_it_2"],
            domainid=self.domain.id
        )

        self.cleanup.append(self.service_offering_it_2)

        vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine_1"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_it_1.id
        )

        self.debug("Deployed VM in account: %s, ID: %s" % (self.account.name, vm_1.id))
        self.cleanup.append(vm_1)

        vm_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine_2"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_it_2.id
        )

        self.debug("Deployed VM in account: %s, ID: %s" % (self.account.name, vm_2.id))
        self.cleanup.append(vm_2)

        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))

        self.assertEqual(cores, self.services['service_offering_it_1']['cpunumber'] + self.services['service_offering_it_2']['cpunumber'])
        self.assertEqual(ram, self.services['service_offering_it_1']['memory'] + self.services['service_offering_it_2']['memory'])

        self.service_offering_it_2.delete(self.apiclient)

        self.cleanup = self.cleanup[0:-1]

        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))

        self.assertEqual(cores, self.services['service_offering_it_1']['cpunumber'] + self.services['service_offering_it_2']['cpunumber'])
        self.assertEqual(ram, self.services['service_offering_it_1']['memory'] + self.services['service_offering_it_2']['memory'])
