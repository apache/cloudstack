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
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
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
                    "hypervisor": 'kvm',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
            "service_offerings":
                {
                    "small":
                        {
                            # Small service offering ID to for change VM
                            # service offering from medium to small
                            "name": "Small Instance",
                            "displaytext": "Small Instance",
                            "cpunumber": 1,
                            "cpuspeed": 100,
                            "memory": 256,
                        }
                },
            "template": {
                "displaytext": "Cent OS Template",
                "name": "Cent OS Template",
                "ostype": 'CentOS 5.3 (64-bit)',
                "passwordenabled": True,
                "ispublic": True
            },
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostype": 'CentOS 5.3 (64-bit)'
            # CentOS 5.3 (64-bit)
        }

class TestStorageMotionAndTemplate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestStorageMotionAndTemplate, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls._cleanup = []
        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=domain.id
        )
        cls._cleanup.append(cls.account)

        cls.small_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.small_offering)

        #create a virtual machine
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup.append(cls.virtual_machine)

    @classmethod
    def tearDownClass(cls):
        super(TestStorageMotionAndTemplate, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        super(TestStorageMotionAndTemplate, self).tearDown()

    @attr(tags=["advanced", "basic", "multipool", "storagemotion", "kvm"], required_hardware="true")
    def test_01_migrate_vm_with_volume_kvm(self):
        """Test migrate virtual machine with its volumes
        """
        # Validate the following
        # 1. List hosts for migration of a vm.
        # 2. List vm volumes and choose a pool for each of them.
        # 3. Migrate vm to a host and volume to a pool.
        # 4. listVM command should return this VM. State of this VM
        #    should be "Running" and the host should be the host
        #    to which the VM was migrated to.
        # 5. volumes in listVolumes should have been migrated to
        #    each respective pool

        if self.hypervisor.lower() in ["lxc"]:
            self.skipTest("Migration across clusters is not supported on LXC")

        hosts = Host.listForMigration(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check the number of hosts in the zone"
        )

        if hosts is None or len(hosts) == 0:
            self.skipTest("No valid hosts for storage motion. Skipping")

        host = hosts[0]

        list_volumes_response = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True
        )
        self.debug("List volumes: %s" % list_volumes_response)
        self.assertEqual(
            isinstance(list_volumes_response, list),
            True,
            "Check list volumes response for valid list"
        )
        self.assertNotEqual(
            list_volumes_response,
            None,
            "Check if volume exists in ListVolumes"
        )

        migrateto = {}
        for volume in list_volumes_response:
            pools = StoragePool.listForMigration(
                self.apiclient,
                id=volume.id
            )
            if not pools:
                self.skipTest("No suitable storage pools found for volume migration. Skipping")

            self.assertTrue(isinstance(pools, list), "invalid pool response from listStoragePoolsForMigration: %s" %pools)
            self.assertTrue(len(pools) > 0, "no valid storage pools found for migration")
            self.debug("Migrateto: %s" % {'volume':volume.id, 'pool':pools[0].id})

            migrateto[volume.id] = pools[0].id

        self.debug("Migrating VM-ID: %s to Host: %s" % (
            self.virtual_machine.id,
            host.id
        ))

        self.debug("hostid: %s, vmid: %s, migrateto: %s", host.id, self.virtual_machine.id, migrateto)
        self.virtual_machine.migrate_vm_with_volume(self.apiclient, host.id, migrateto)

        list_vm_response = list_virtual_machines(
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
            "Check virtual machine ID of migrated VM"
        )

        self.assertEqual(
            vm_response.hostid,
            host.id,
            "Check destination hostID of migrated VM"
        )

        self.assertEqual(
            vm_response.state,
            'Running',
            "Check the state of VM"
        )

        list_volumes_response = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )

        for volume_response in list_volumes_response:
            pool = migrateto[volume_response.id]
            self.assertEqual(volume_response.storageid, pool, "Check volume migrated to correct pool")

        return

    @attr(tags=["advanced", "basic", "multipool", "storagemotion", "kvm"], required_hardware="true")
    def test_02_migrate_vm_with_volume_and_removed_template_kvm(self):
        """Test migrate virtual machine with its volumes and root disk's template removed
        """
        # Validate the following
        # 1. Create template and VM from it
        # 1. List hosts for migration of said vm.
        # 2. List vm volumes and choose a pool for each of them.
        # 3. Delete the template
        # 4. Migrate vm to a host and volume to a pool.
        # 5. listVM command should return this VM. State of this VM
        #    should be "Running" and the host should be the host
        #    to which the VM was migrated to.
        # 6. volumes in listVolumes should have been migrated to
        #    each respective pool


        # Stop virtual machine
        self.virtual_machine.stop(self.api_client)

        template_volume = list_volumes(
            self.api_client,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True)
        self.assertTrue(isinstance(template_volume, list), "List volumes to create template failed.")

        template = Template.create(
            self.api_client,
            self.services["template"],
            volumeid=template_volume[0].id
        )
        self.cleanup.append(template)

        self.debug("Creating vm with: %s" % template)
        new_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=template.id,
            serviceofferingid=self.small_offering.id
        )
        self.cleanup.append(new_virtual_machine)
        self.debug("Created vm %s" % new_virtual_machine)

        self.debug("Deleting template: %s" % template)
        template.delete(self.apiclient)
        self.cleanup.remove(template)
        self.debug("Delete template: %s successful" % template)

        if self.hypervisor.lower() in ["lxc"]:
            self.skipTest("Migration across clusters is not supported on LXC")

        hosts = Host.listForMigration(
            self.apiclient,
            virtualmachineid=new_virtual_machine.id
        )

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check the number of hosts in the zone"
        )

        if hosts is None or len(hosts) == 0:
            self.skipTest("No valid hosts for storage motion. Skipping")

        host = hosts[0]

        list_volumes_response = list_volumes(
            self.apiclient,
            virtualmachineid=new_virtual_machine.id,
            listall=True
        )
        self.debug("List volumes: %s" % list_volumes_response)
        self.assertEqual(
            isinstance(list_volumes_response, list),
            True,
            "Check list volumes response for valid list"
        )
        self.assertNotEqual(
            list_volumes_response,
            None,
            "Check if volume exists in ListVolumes"
        )

        migrateto = {}
        for volume in list_volumes_response:
            pools = StoragePool.listForMigration(
                self.apiclient,
                id=volume.id
            )
            if not pools:
                self.skipTest("No suitable storage pools found for volume migration. Skipping")

            self.assertTrue(isinstance(pools, list), "invalid pool response from listStoragePoolsForMigration: %s" %pools)
            self.assertTrue(len(pools) > 0, "no valid storage pools found for migration")
            self.debug("Migrateto: %s" % {'volume':volume.id, 'pool':pools[0].id})

            migrateto[volume.id] = pools[0].id

        self.debug("Migrating VM-ID: %s to Host: %s" % (
            new_virtual_machine.id,
            host.id
        ))

        self.debug("hostid: %s, vmid: %s, migrateto: %s", host.id, new_virtual_machine.id, migrateto)
        new_virtual_machine.migrate_vm_with_volume(self.apiclient, host.id, migrateto)

        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=new_virtual_machine.id
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
            new_virtual_machine.id,
            "Check virtual machine ID of migrated VM"
        )

        self.assertEqual(
            vm_response.hostid,
            host.id,
            "Check destination hostID of migrated VM"
        )

        self.assertEqual(
            vm_response.state,
            'Running',
            "Check the state of VM"
        )

        list_volumes_response = list_volumes(
            self.apiclient,
            virtualmachineid=new_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )

        for volume_response in list_volumes_response:
            pool = migrateto[volume_response.id]
            self.assertEqual(volume_response.storageid, pool, "Check volume migrated to correct pool")

        return
