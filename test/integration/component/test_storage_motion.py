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
                    "hypervisor": 'XenServer',
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
                    "passwordenabled": True,
                },
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
        }

class TestStorageMotion(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestStorageMotion, cls).getClsTestClient()
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

        #create a virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
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
        cls.api_client = super(TestStorageMotion, cls).getClsTestClient().getApiClient()
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

    @attr(tags=["advanced", "basic", "multicluster", "storagemotion", "xenserver"], required_hardware="true")
    def test_01_migrate_vm_with_volume(self):
        """Test migrate virtual machine with its volumes
        """
        # Validate the following
        # 1. List hosts for migration of a vm. Pick a host that
        # requires storage motion too.
        # 2. Migrate vm to a host.
        # 3. listVM command should return this VM.State of this VM
        #    should be "Running" and the host should be the host
        #    to which the VM was migrated to in a different cluster
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

        # Migrate to a host that requires storage motion
        hosts[:] = [host for host in hosts if host.requiresStorageMotion]

        if hosts is None or len(hosts) == 0:
            self.skipTest("No valid hosts for storage motion. Skipping")



        host = hosts[0]
        self.debug("Migrating VM-ID: %s to Host: %s" % (
                                        self.virtual_machine.id,
                                        host.id
                                        ))

        cmd = migrateVirtualMachineWithVolume.migrateVirtualMachineWithVolumeCmd()
        cmd.hostid = host.id
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.migrateVirtualMachineWithVolume(cmd)

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
        return

    @attr(tags=["advanced", "basic", "multipool", "storagemotion", "xenserver"], required_hardware="false")
    def test_02_migrate_volume(self):
        """Test migrate volume of a running vm
        """
        # Validate the following
        # 1. List all the volumes of a vm. For each volume do step 2 to 4.
        # 2. List storage pools for migrating volume of a vm. Multiple
        #    storage pools should be present in the cluster.
        # 3. Migrate volume of the vm to another pool.
        # 4. Check volume is present in the new pool and is in Ready state.

        # TODO: add test case for data volume migrate and handle it for LXC

        list_volumes_response = list_volumes(
                                    self.apiclient,
                                    virtualmachineid=self.virtual_machine.id,
                                    listall=True
                                    )
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

        for volume in list_volumes_response:
            pools = StoragePool.listForMigration(
                              self.apiclient,
                              id=volume.id
                              )
            if not pools:
                self.skipTest("No suitable storage pools found for volume migration. Skipping")

            self.assertTrue(isinstance(pools, list), "invalid pool response from listStoragePoolsForMigration: %s" %pools)
            self.assertTrue(len(pools) > 0, "no valid storage pools found for migration")

            pool = pools[0]
            self.debug("Migrating Volume-ID: %s to Pool: %s" % (
                                volume.id,
                                pool.id
                                ))
            Volume.migrate(
                           self.apiclient,
                           volumeid=volume.id,
                           storageid=pool.id,
                           livemigrate='true'
                           )
            migrated_volume_response = list_volumes(
                                             self.apiclient,
                                             id=volume.id
                                             )
            self.assertEqual(
                             isinstance(migrated_volume_response, list),
                             True,
                             "Check list volumes response for valid list"
                             )
            self.assertNotEqual(
                                migrated_volume_response,
                                None,
                                "Check if volume exists in ListVolumes"
                                )
            migrated_volume = migrated_volume_response[0]
            self.assertEqual(
                             migrated_volume.state,
                             'Ready',
                             "Check migrated volume is in Ready state"
                             )
            self.assertEqual(
                             migrated_volume.storage,
                             pool.name,
                             "Check volume is on migrated pool"
                             )

        return
