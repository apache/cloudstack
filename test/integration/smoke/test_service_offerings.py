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
""" BVT tests for Service offerings"""

#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import isAlmostEqual
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr


_multiprocess_shared_ = True

class Services:
    """Test Service offerings Services
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
            "off":
                {
                    "name": "Service Offering",
                    "displaytext": "Service Offering",
                    "cpunumber": 1,
                    "cpuspeed": 100, # MHz
                    "memory": 128, # in MBs
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
            "medium": # Create a medium virtual machine instance
                {
                    "displayname": "testserver",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
            "service_offerings":
                {
                    "tiny":
                        {
                            "name": "Tiny Instance",
                            "displaytext": "Tiny Instance",
                            "cpunumber": 1,
                            "cpuspeed": 100, # in MHz
                            "memory": 128, # In MBs
                        },
                    "small":
                        {
                            # Small service offering ID to for change VM
                            # service offering from medium to small
                            "name": "Small Instance",
                            "displaytext": "Small Instance",
                            "cpunumber": 1,
                            "cpuspeed": 100,
                            "memory": 128,
                        },
                    "medium":
                        {
                            # Medium service offering ID to for
                            # change VM service offering from small to medium
                            "name": "Medium Instance",
                            "displaytext": "Medium Instance",
                            "cpunumber": 1,
                            "cpuspeed": 100,
                            "memory": 256,
                        }
                },
            "ostype": 'CentOS 5.3 (64-bit)',
        }


class TestCreateServiceOffering(cloudstackTestCase):
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.services = Services().services

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "eip", "sg"])
    def test_01_create_service_offering(self):
        """Test to create service offering"""

        # Validate the following:
        # 1. createServiceOfferings should return a valid information for newly created offering
        # 2. The Cloud Database contains the valid information

        service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["off"]
        )
        self.cleanup.append(service_offering)

        self.debug("Created service offering with ID: %s" % service_offering.id)

        list_service_response = list_service_offering(
            self.apiclient,
            id=service_offering.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is created"
        )
        service_response = list_service_response[0]

        self.assertEqual(
            list_service_response[0].cpunumber,
            self.services["off"]["cpunumber"],
            "Check server id in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].cpuspeed,
            self.services["off"]["cpuspeed"],
            "Check cpuspeed in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].displaytext,
            self.services["off"]["displaytext"],
            "Check server displaytext in createServiceOfferings"
        )
        self.assertEqual(
            list_service_response[0].memory,
            self.services["off"]["memory"],
            "Check memory in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].name,
            self.services["off"]["name"],
            "Check name in createServiceOffering"
        )
        return


class TestServiceOfferings(cloudstackTestCase):
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestServiceOfferings, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

        cls.service_offering_1 = ServiceOffering.create(
            cls.api_client,
            cls.services["off"]
        )
        cls.service_offering_2 = ServiceOffering.create(
            cls.api_client,
            cls.services["off"]
        )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["medium"]["zoneid"] = cls.zone.id
        cls.services["medium"]["template"] = template.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=domain.id
                            )

        cls.small_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["small"]
                                    )

        cls.medium_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["medium"]
                                    )
        cls.medium_virtual_machine = VirtualMachine.create(
                                       cls.api_client,
                                       cls.services["medium"],
                                       accountid=cls.account.name,
                                       domainid=cls.account.domainid,
                                       serviceofferingid=cls.medium_offering.id,
                                       mode=cls.services["mode"]
                                    )
        cls._cleanup = [
                        cls.small_offering,
                        cls.medium_offering,
                        cls.account
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestServiceOfferings, cls).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "eip", "sg"])
    def test_02_edit_service_offering(self):
        """Test to update existing service offering"""

        # Validate the following:
        # 1. updateServiceOffering should return
        #    a valid information for newly created offering

        #Generate new name & displaytext from random data
        random_displaytext = random_gen()
        random_name = random_gen()

        self.debug("Updating service offering with ID: %s" %
                   self.service_offering_1.id)

        cmd = updateServiceOffering.updateServiceOfferingCmd()
        #Add parameters for API call
        cmd.id = self.service_offering_1.id
        cmd.displaytext = random_displaytext
        cmd.name = random_name
        self.apiclient.updateServiceOffering(cmd)

        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering_1.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is updated"
        )

        self.assertEqual(
            list_service_response[0].displaytext,
            random_displaytext,
            "Check server displaytext in updateServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].name,
            random_name,
            "Check server name in updateServiceOffering"
        )

        return

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "eip", "sg"])
    def test_03_delete_service_offering(self):
        """Test to delete service offering"""

        # Validate the following:
        # 1. deleteServiceOffering should return
        #    a valid information for newly created offering

        self.debug("Deleting service offering with ID: %s" %
                   self.service_offering_2.id)

        self.service_offering_2.delete(self.apiclient)

        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering_2.id
        )

        self.assertEqual(
            list_service_response,
            None,
            "Check if service offering exists in listDiskOfferings"
        )

        return

    @attr(tags=["advanced", "advancedns", "smoke"])
    def test_04_change_offering_small(self):
        """Test to change service to a small capacity
        """
        # Validate the following
        # 1. Log in to the Vm .We should see that the CPU and memory Info of
        #    this Vm matches the one specified for "Small" service offering.
        # 2. Using  listVM command verify that this Vm
        #    has Small service offering Id.

        self.debug("Stopping VM - ID: %s" % self.medium_virtual_machine.id)
        self.medium_virtual_machine.stop(self.apiclient)
        # Ensure that VM is in stopped state
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=self.medium_virtual_machine.id
        )
        if isinstance(list_vm_response, list):
            vm = list_vm_response[0]
            if vm.state == 'Stopped':
                self.debug("VM state: %s" % vm.state)
            else:
                raise Exception(
                    "Failed to stop VM (ID: %s) in change service offering" % vm.id)

        self.debug("Change Service offering VM - ID: %s" %
                   self.medium_virtual_machine.id)

        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.medium_virtual_machine.id
        cmd.serviceofferingid = self.small_offering.id
        self.apiclient.changeServiceForVirtualMachine(cmd)

        self.debug("Starting VM - ID: %s" % self.medium_virtual_machine.id)
        self.medium_virtual_machine.start(self.apiclient)
        # Ensure that VM is in running state
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=self.medium_virtual_machine.id
        )

        if isinstance(list_vm_response, list):
            vm = list_vm_response[0]
            if vm.state == 'Running':
                self.debug("VM state: %s" % vm.state)
            else:
                raise Exception(
                    "Failed to start VM (ID: %s) after changing service offering" % vm.id)

        try:
            ssh = self.medium_virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %\
                (self.medium_virtual_machine.ipaddress, e)
            )

        cpuinfo = ssh.execute("cat /proc/cpuinfo")
        cpu_cnt = len([i for i in cpuinfo if "processor" in i])
        #'cpu MHz\t\t: 2660.499'
        cpu_speed = [i for i in cpuinfo if "cpu MHz" in i][0].split()[3]
        meminfo = ssh.execute("cat /proc/meminfo")
        #MemTotal:        1017464 kB
        total_mem = [i for i in meminfo if "MemTotal" in i][0].split()[1]

        self.debug(
            "CPU count: %s, CPU Speed: %s, Mem Info: %s" % (
                cpu_cnt,
                cpu_speed,
                total_mem
                ))
        self.assertAlmostEqual(
            int(cpu_cnt),
            self.small_offering.cpunumber,
            "Check CPU Count for small offering"
        )
        self.assertAlmostEqual(
            list_vm_response[0].cpuspeed,
            self.small_offering.cpuspeed,
            "Check CPU Speed for small offering"
        )
        self.assertTrue(
            isAlmostEqual(int(int(total_mem) / 1024),
                int(self.small_offering.memory),
                range=20
            ),
            "Check Memory(kb) for small offering"
        )
        return
