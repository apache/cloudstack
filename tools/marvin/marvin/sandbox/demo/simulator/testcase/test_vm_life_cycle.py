# -*- encoding: utf-8 -*-
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

""" BVT tests for Virtual Machine Life Cycle
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.sshClient import SshClient

from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *

#Import System modules
import time

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
                "medium":   # Create a medium virtual machine instance
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
                        "memory": 64, # In MBs
                    },
                 "small":
                    {
                     # Small service offering ID to for change VM
                     # service offering from medium to small
                        "name": "Small Instance",
                        "displaytext": "Small Instance",
                        "cpunumber": 1,
                        "cpuspeed": 500,
                        "memory": 256
                    },
                "medium":
                    {
                    # Medium service offering ID to for
                    # change VM service offering from small to medium
                        "name": "Medium Instance",
                        "displaytext": "Medium Instance",
                        "cpunumber": 1,
                        "cpuspeed": 1000,
                        "memory": 1024
                    }
                },
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostypeid": 'ccde7156-9b8b-4fb9-bf08-530dedf4dc61',
            # CentOS 5.3 (64-bit)
            "mode":'advanced',
        }


class TestDeployVM(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.services = Services().services
        # Get Zone, Domain and templates
        domain = get_domain(self.apiclient, self.services)
        zone = get_zone(self.apiclient, self.services)

        template = get_template(
                            self.apiclient,
                            zone.id,
                            self.services["ostypeid"]
                            )
        # Set Zones and disk offerings
        self.services["small"]["zoneid"] = zone.id
        self.services["small"]["template"] = template.id

        self.services["medium"]["zoneid"] = zone.id
        self.services["medium"]["template"] = template.id

        # Create Account, VMs, NAT Rules etc
        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=domain.id
                            )

        self.service_offering = ServiceOffering.create(
                                    self.apiclient,
                                    self.services["service_offerings"]["tiny"]
                                    )
        # Cleanup
        self.cleanup = [
                        self.service_offering,
                        self.account
                        ]

    def test_deploy_vm(self):
        """Test Deploy Virtual Machine
        """

        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. listVirtualMachines returns accurate information
        # 3. The Cloud Database contains the valid information

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["small"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                )

        list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.virtual_machine.id
                                                 )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(

                            vm_response.id,
                            self.virtual_machine.id,
                            "Check virtual machine id in listVirtualMachines"
                        )

        self.assertEqual(
                    vm_response.displayname,
                    self.virtual_machine.displayname,
                    "Check virtual machine displayname in listVirtualMachines"
                    )
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


class TestVMLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestVMLifeCycle, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client, cls.services)
        zone = get_zone(cls.api_client, cls.services)
        template = get_template(
                            cls.api_client,
                            zone.id,
                            cls.services["ostypeid"]
                            )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = zone.id
        cls.services["small"]["template"] = template.id

        cls.services["medium"]["zoneid"] = zone.id
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
        #create small and large virtual machines
        cls.small_virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["small"],
                                        accountid=cls.account.account.name,
                                        domainid=cls.account.account.domainid,
                                        serviceofferingid=cls.small_offering.id,
                                        mode=cls.services["mode"]
                                        )
        cls.medium_virtual_machine = VirtualMachine.create(
                                       cls.api_client,
                                       cls.services["medium"],
                                       accountid=cls.account.account.name,
                                       domainid=cls.account.account.domainid,
                                       serviceofferingid=cls.medium_offering.id,
                                       mode=cls.services["mode"]
                                    )
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["small"],
                                        accountid=cls.account.account.name,
                                        domainid=cls.account.account.domainid,
                                        serviceofferingid=cls.small_offering.id,
                                        mode=cls.services["mode"]
                                        )
        cls._cleanup = [
                        cls.small_offering,
                        cls.medium_offering,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.api_client = super(TestVMLifeCycle, cls).getClsTestClient().getApiClient()
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

    def test_01_stop_vm(self):
        """Test Stop Virtual Machine
        """

        # Validate the following
        # 1. Should Not be able to login to the VM.
        # 2. listVM command should return
        #    this VM.State of this VM should be ""Stopped"".

        self.debug("Stopping VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.stop(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check virtual machine is in stopped state"
                        )
        return

    def test_02_start_vm(self):
        """Test Start Virtual Machine
        """
        # Validate the following
        # 1. listVM command should return this VM.State
        #    of this VM should be Running".

        self.debug("Starting VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.start(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.small_virtual_machine.id
                )
        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return

    def test_04_change_offering_small(self):
        """Change Offering to a small capacity
        """

        # Validate the following
        # 1. Log in to the Vm .We should see that the CPU and memory Info of
        #    this Vm matches the one specified for "Small" service offering.
        # 2. Using  listVM command verify that this Vm
        #    has Small service offering Id.

        self.debug("Stopping VM - ID: %s" % self.medium_virtual_machine.id)

        self.medium_virtual_machine.stop(self.apiclient)

        # Poll listVM to ensure VM is stopped properly
        timeout = self.services["timeout"]

        while True:
            time.sleep(self.services["sleep"])

            # Ensure that VM is in stopped state
            list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.medium_virtual_machine.id
                                            )

            if isinstance(list_vm_response, list):

                vm = list_vm_response[0]
                if vm.state == 'Stopped':
                    self.debug("VM state: %s" % vm.state)
                    break

            if timeout == 0:
                    raise Exception(
                        "Failed to stop VM (ID: %s) in change service offering" % vm.id)

            timeout = timeout - 1

        self.debug("Change Service offering VM - ID: %s" %
                                    self.medium_virtual_machine.id)

        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.medium_virtual_machine.id
        cmd.serviceofferingid = self.small_offering.id
        self.apiclient.changeServiceForVirtualMachine(cmd)

        self.debug("Starting VM - ID: %s" % self.medium_virtual_machine.id)
        self.medium_virtual_machine.start(self.apiclient)

        # Poll listVM to ensure VM is started properly
        timeout = self.services["timeout"]

        while True:
            time.sleep(self.services["sleep"])

            # Ensure that VM is in running state
            list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.medium_virtual_machine.id
                                            )

            if isinstance(list_vm_response, list):

                vm = list_vm_response[0]
                if vm.state == 'Running':
                    self.debug("VM state: %s" % vm.state)
                    break

            if timeout == 0:
                    raise Exception(
                        "Failed to start VM (ID: %s) after changing service offering" % vm.id)

            timeout = timeout - 1

        return

    def test_06_destroy_vm(self):
        """Test destroy Virtual Machine
        """

        # Validate the following
        # 1. Should not be able to login to the VM.
        # 2. listVM command should return this VM.State
        #    of this VM should be "Destroyed".

        self.debug("Destroy VM - ID: %s" % self.small_virtual_machine.id)
        self.small_virtual_machine.delete(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Destroyed",
                            "Check virtual machine is in destroyed state"
                        )
        return

    def test_07_restore_vm(self):
        """Test recover Virtual Machine
        """

        # Validate the following
        # 1. listVM command should return this VM.
        #    State of this VM should be "Stopped".
        # 2. We should be able to Start this VM successfully.

        self.debug("Recovering VM - ID: %s" % self.small_virtual_machine.id)

        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.recoverVirtualMachine(cmd)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check virtual machine is in Stopped state"
                        )

        return
