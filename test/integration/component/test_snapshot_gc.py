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
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
#from marvin.cloudstackAPI import *
from marvin.lib.utils import (
                              is_snapshot_on_nfs,
                              cleanup_resources)
from marvin.lib.base import (Account,
                             Snapshot,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_accounts,
                               list_snapshots,
                               wait_for_cleanup)

class Services:
    """Test Snapshots Services
    """

    def __init__(self):
        self.services = {
                        "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 200,    # in MHz
                                    "memory": 256,      # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small Disk",
                                    "name": "Small Disk",
                                    "disksize": 1
                        },
                        "server_with_disk":
                                    {
                                        "displayname": "Test VM -With Disk",
                                        "username": "root",
                                        "password": "password",
                                        "ssh_port": 22,
                                        "hypervisor": 'XenServer',
                                        "privateport": 22,
                                        "publicport": 22,
                                        "protocol": 'TCP',
                                },

                        "server_without_disk":
                                    {
                                        "displayname": "Test VM-No Disk",
                                        "username": "root",
                                        "password": "password",
                                        "ssh_port": 22,
                                        "hypervisor": 'XenServer',
                                        "privateport": 22,
                                        # For NAT rule creation
                                        "publicport": 22,
                                        "protocol": 'TCP',
                                },
                        "server": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                                },
                        "recurring_snapshot": {
                                    "intervaltype": 'HOURLY',
                                    # Frequency of snapshots
                                    "maxsnaps": 1,  # Should be min 2
                                    "schedule": 1,
                                    "timezone": 'US/Arizona',
                                    # Timezone Formats - http://cloud.mindtouch.us/CloudStack_Documentation/Developer's_Guide%3A_CloudStack
                                },
                        "templates": {
                                    "displaytext": 'Template',
                                    "name": 'Template',
                                    "ostype": "CentOS 5.3 (64-bit)",
                                    "templatefilter": 'self',
                                },
                        "volume": {
                                   "diskname": "APP Data Volume",
                                   "size": 1,   # in GBs
                                   "diskdevice": ['/dev/xvdb', '/dev/sdb', '/dev/hdb', '/dev/vdb' ],   # Data Disk
                        },
                        "paths": {
                                    "mount_dir": "/mnt/tmp",
                                    "sub_dir": "test",
                                    "sub_lvl_dir1": "test1",
                                    "sub_lvl_dir2": "test2",
                                    "random_data": "random.data",
                        },
                        "ostype": "CentOS 5.3 (64-bit)",
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                    }


class TestAccountSnapshotClean(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAccountSnapshotClean, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("snapshots are not supported on %s" % cls.hypervisor.lower())

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id
        cls._cleanup = []

        try:
            # Create VMs, NAT Rules etc
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=cls.domain.id
                                )

            cls.services["account"] = cls.account.name

            if cls.zone.localstorageenabled:
                cls.services["service_offering"]["storagetype"] = "local"
            cls.service_offering = ServiceOffering.create(
                                                cls.api_client,
                                                cls.services["service_offering"]
                                                )

            cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )

            # Get the Root disk of VM
            volumes = list_volumes(
                                cls.api_client,
                                virtualmachineid=cls.virtual_machine.id,
                                type='ROOT',
                                listall=True
                                )
            volume = volumes[0]

            # Create a snapshot from the ROOTDISK
            cls.snapshot = Snapshot.create(cls.api_client, volume.id)
        except Exception as e:
            cls.tearDownClass()
            unittest.SkipTest("setupClass fails for %s" % cls.__name__)
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(speed = "slow")
    @attr(tags=["advanced", "advancedns", "basic", "sg"], required_hardware="true")
    def test_02_accountSnapshotClean(self):
        """Test snapshot cleanup after account deletion
        """
        # Validate the following
        # 1. listAccounts API should list out the newly created account
        # 2. listVirtualMachines() command should return the deployed VM.
        #    State of this VM should be "Running"
        # 3. a)listSnapshots should list the snapshot that was created.
        #    b)verify that secondary storage NFS share contains the reqd volume
        #      under /secondary/snapshots/$accountid/$volumeid/$snapshot_id
        # 4. a)listAccounts should not list account that is deleted
        #    b) snapshot image($snapshot_id) should be deleted from the
        #       /secondary/snapshots/$accountid/$volumeid/

        try:
            accounts = list_accounts(
                                 self.apiclient,
                                 id=self.account.id
                                 )
            self.assertEqual(
                            isinstance(accounts, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertNotEqual(
                             len(accounts),
                             0,
                             "Check list Accounts response"
                             )

            # Verify the snapshot was created or not
            snapshots = list_snapshots(
                                   self.apiclient,
                                   id=self.snapshot.id
                                   )
            self.assertEqual(
                            isinstance(snapshots, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertNotEqual(
                            snapshots,
                            None,
                            "No such snapshot %s found" % self.snapshot.id
                            )
            self.assertEqual(
                            snapshots[0].id,
                            self.snapshot.id,
                            "Check snapshot id in list resources call"
                        )

            self.assertTrue(is_snapshot_on_nfs(self.apiclient, self.dbclient, self.config, self.zone.id, self.snapshot.id),
                "Snapshot was not found on NFS")
        except Exception as e:
            self._cleanup.append(self.account)
            self.fail("Exception occured: %s" % e)

        self.debug("Deleting account: %s" % self.account.name)
        # Delete account
        self.account.delete(self.apiclient)

        # Wait for account cleanup interval
        wait_for_cleanup(self.apiclient, configs=["account.cleanup.interval"])

        with self.assertRaises(Exception):
            accounts = list_accounts(
                                 self.apiclient,
                                 id=self.account.id
                                 )

        self.assertFalse(is_snapshot_on_nfs(self.apiclient, self.dbclient, self.config, self.zone.id, self.snapshot.id),
                                            "Snapshot was still found on NFS after account gc")
        return
