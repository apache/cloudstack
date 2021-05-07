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
""" Test update resource count API method
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (random_gen,
                              cleanup_resources)
from marvin.lib.base import (Domain,
                             Account,
                             ServiceOffering,
                             VirtualMachine,
                             Network,
                             User,
                             NATRule,
                             Template,
                             PublicIPAddress,
                             Resources)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_accounts,
                               list_virtual_machines,
                               list_service_offering,
                               list_templates,
                               list_users,
                               get_builtin_template_info,
                               wait_for_cleanup)
from nose.plugins.attrib import attr
from marvin.cloudstackException import CloudstackAPIException
import time


class Services:

    """These are some configurations that will get implemented in ACS. They are put here to follow some sort of standard that seems to be in place.
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "Tester",
                "username": "test",
                "password": "acountFirstUserPass",
            },
            "service_offering_custom": {
                "name": "Custom service offering test",
                "displaytext": "Custom service offering test",
                "cpunumber": None,
                "cpuspeed": None,
                # in MHz
                "memory": None,
                # In MBs
            },
            "service_offering_normal": {
                "name": "Normal service offering",
                "displaytext": "Normal service offering",
                "cpunumber": 2,
                "cpuspeed": 1000,
                # in MHz
                "memory": 512,
                # In MBs
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "sleep": 60,
            "timeout": 10
        }


class TestUpdateResourceCount(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUpdateResourceCount, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering_custom = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering_custom"]
        )
        cls.service_offering_normal = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering_normal"]
        )
        cls._cleanup = [cls.service_offering_custom, cls.service_offering_normal]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.account = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % self.account.name)
        self.cleanup.append(self.account)
        
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "advancedns",
            "sg"],
        required_hardware="false")
    def test_01_updateResourceCount(self):
        """Test update resource count for an account using a custom service offering to deploy a VM.
        """
        
        # This test will execute the following steps to assure resource count update is working properly
        # 1. Create an account.
        # 2. Start 2 VMs; one with normal service offering and other with a custom service offering
        # 3. Call the update resource count method and check the CPU and memory values.
        #    The two VMs will add up to 3 CPUs and 1024Mb of RAM.
        # 4. If the return of updateResourceCount method matches with the expected one, the test passes; otherwise, it fails.
        # 5. Remove everything created by deleting the account
        
        vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_custom.id,
            customcpunumber = 1,
            customcpuspeed = 1000,
            custommemory = 512
        )
        
        self.debug("Deployed VM 1 in account: %s, ID: %s" % (
            self.account.name,
            vm_1.id
        ))
        
        vm_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_normal.id
        )
        self.debug("Deployed VM 2 in account: %s, ID: %s" % (
            self.account.name,
            vm_2.id
        ))

        resourceCountCpu = Resources.updateCount(
            self.apiclient,
            resourcetype=8,
            account=self.account.name,
            domainid=self.account.domainid
        ) 
            
        self.debug("ResourceCount for CPU: %s" % (
            resourceCountCpu[0].resourcecount
        ))
        self.assertEqual(
            resourceCountCpu[0].resourcecount,
            3,
            "The number of CPU cores does not seem to be right."
        )
        resourceCountMemory = Resources.updateCount(
            self.apiclient,
            resourcetype=9,
            account=self.account.name,
            domainid=self.account.domainid
        ) 
            
        self.debug("ResourceCount for memory: %s" % (
            resourceCountMemory[0].resourcecount
        ))
        self.assertEqual(
            resourceCountMemory[0].resourcecount,
            1024,
            "The memory amount does not seem to be right."
        )
        return
