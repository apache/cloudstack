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
""" P1 tests for Storage motion
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.sshClient import SshClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True
class Services:
    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {
                "disk_offering":{
                    "displaytext": "Small",
                    "name": "Small",
                    "disksize": 1
                },
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
                 "implicitplanner":
                    {
                     # Small service offering ID to for change VM
                     # service offering from medium to small
                        "name": "Implicit Strict",
                        "displaytext": "Implicit Strict",
                        "cpunumber": 1,
                        "cpuspeed": 500,
                        "memory": 512,
                        "deploymentplanner": "ImplicitDedicationPlanner"
                    }
                },
                "template": {
                    "displaytext": "Cent OS Template",
                    "name": "Cent OS Template",
                    "passwordenabled": True,
                },
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
        }

class TestImplicitPlanner(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestImplicitPlanner, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        # Set template id
        cls.services["small"]["template"] = template.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=domain.id
                            )

        cls.small_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["implicitplanner"]
                                    )

        cls._cleanup = [
                        cls.small_offering,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.api_client = super(TestImplicitPlanner, cls).getClsTestClient().getApiClient()
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

    # This test requires multi host and at least one host which is empty (no vms should
    # be running on that host). It uses an implicit planner to deploy instances and the
    # instances of a new account should go to an host that doesn't have vms of any other
    # account.
    @attr(tags = ["advanced", "basic", "multihosts", "implicitplanner"])
    def test_01_deploy_vm_with_implicit_planner(self):
        """Test implicit planner is placing vms of an account on implicitly dedicated hosts.
        """
        # Validate the following
        # 1. Deploy a vm using implicit planner. It should go on to a
        #    host that is empty (not running vms of any other account)
        # 2. Deploy another vm it should get deployed on the same host.

        # list and find an empty host
        all_hosts = list_hosts(
                           self.apiclient,
                           type='Routing',
                           )

        empty_host = None
        for host in all_hosts:
            vms_on_host = list_virtual_machines(
                                                self.api_client,
                                                hostid=host.id,
                                                listall=True)

            # If no user vms on host, then further check for any System vms running
            if vms_on_host is None:
                ssvms_on_host = list_ssvms(
                                           self.api_client,
                                           hostid=host.id,
                                           listall=True)

                # If no ssvms running on host, then further check for routers
                if ssvms_on_host is None:
                    routers_on_host = list_routers(
                                                   self.api_client,
                                                   hostid=host.id,
                                                   listall=True)

                    # If no routers are running on host, then this host can be considered
                    # as empty for implicit dedication

                    if routers_on_host is None:
                        empty_host = host
                        self.services["small"]["zoneid"] = host.zoneid
                        break

        #If no empty host is found, return
        if empty_host is None:
           self.skipTest("Did not find any empty hosts, Skipping")

        #create a virtual machine
        virtual_machine_1 = VirtualMachine.create(
                                        self.api_client,
                                        self.services["small"],
                                        accountid=self.account.name,
                                        domainid=self.account.domainid,
                                        serviceofferingid=self.small_offering.id,
                                        mode=self.services["mode"]
                                        )

        list_vm_response_1 = list_virtual_machines(
                                            self.apiclient,
                                            id=virtual_machine_1.id
                                            )
        self.assertEqual(
                        isinstance(list_vm_response_1, list),
                        True,
                        "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            list_vm_response_1,
                            None,
                            "Check virtual machine is listVirtualMachines"
                            )

        vm_response_1 = list_vm_response_1[0]

        self.assertEqual(
                        vm_response_1.id,
                        virtual_machine_1.id,
                        "Check virtual machine ID of VM"
                        )

        virtual_machine_2 = VirtualMachine.create(
                                                self.api_client,
                                                self.services["small"],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                serviceofferingid=self.small_offering.id,
                                                mode=self.services["mode"]
                                                )

        list_vm_response_2 = list_virtual_machines(
                                            self.apiclient,
                                            id=virtual_machine_2.id
                                            )
        self.assertEqual(
                isinstance(list_vm_response_2, list),
                True,
                "Check list response returns a valid list"
                )

        self.assertNotEqual(
                            list_vm_response_2,
                            None,
                            "Check virtual machine is listVirtualMachines"
                            )

        vm_response_2 = list_vm_response_2[0]

        self.assertEqual(
                vm_response_2.id,
                virtual_machine_2.id,
                "Check virtual machine ID of VM"
                )

        self.assertEqual(
                vm_response_1.hostid,
                vm_response_2.hostid,
                "Check both vms have the same host id"
                )
        return
