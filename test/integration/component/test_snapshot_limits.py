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
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             VirtualMachine,
                             SnapshotPolicy,
                             ServiceOffering)
from marvin.lib.common import (get_zone,
                               get_template,
                               get_domain,
                               list_volumes,
                               list_snapshots,
                               list_snapshot_policy)
from marvin.lib.utils import is_snapshot_on_nfs
import time


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


class TestSnapshotLimit(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSnapshotLimit, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("snapshots are not supported on %s" % cls.hypervisor.lower())
        cls._cleanup = []

        try:
            template = get_template(
                                cls.api_client,
                                cls.zone.id,
                                cls.services["ostype"]
                                )
            cls.services["server"]["zoneid"] = cls.zone.id

            cls.services["template"] = template.id

            # Create VMs, NAT Rules etc
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=cls.domain.id
                                )
            cls._cleanup.append(cls.account)

            cls.services["account"] = cls.account.name

            if cls.zone.localstorageenabled:
                cls.services["service_offering"]["storagetype"] = "local"
            cls.service_offering = ServiceOffering.create(
                                                cls.api_client,
                                                cls.services["service_offering"]
                                                )
            cls._cleanup.append(cls.service_offering)
            cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )
            cls._cleanup.append(cls.virtual_machine)
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
    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_04_snapshot_limit(self):
        """Test snapshot limit in snapshot policies
        """
        # Validate the following
        # 1. Perform hourly recurring snapshot on the root disk of VM and keep
        #    the maxsnapshots as 1
        # 2. listSnapshots should list the snapshot that was created
        #    snapshot folder in secondary storage should contain only one
        #    snapshot image(/secondary/snapshots/$accountid/$volumeid/)

        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=self.virtual_machine.id,
                            type='ROOT',
                            listall=True
                            )
        self.assertEqual(
                            isinstance(volumes, list),
                            True,
                            "Check list response returns a valid list"
                        )
        volume = volumes[0]

        # Create a snapshot policy
        recurring_snapshot = SnapshotPolicy.create(
                                           self.apiclient,
                                           volume.id,
                                           self.services["recurring_snapshot"]
                                        )
        self.cleanup.append(recurring_snapshot)

        snapshot_policy = list_snapshot_policy(
                                        self.apiclient,
                                        id=recurring_snapshot.id,
                                        volumeid=volume.id
                                        )
        self.assertEqual(
                            isinstance(snapshot_policy, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            snapshot_policy,
                            None,
                            "Check if result exists in list item call"
                            )

        self.assertEqual(
                        snapshot_policy[0].id,
                        recurring_snapshot.id,
                        "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                        snapshot_policy[0].maxsnaps,
                        self.services["recurring_snapshot"]["maxsnaps"],
                        "Check interval type in list resources call"
                        )
        # Sleep for (maxsnaps+1) hours to verify
        # only maxsnaps snapshots are retained
        time.sleep(
            (int(self.services["recurring_snapshot"]["maxsnaps"]) + 1) * 3600
            )

        # Verify the snapshot was created or not
        snapshots = list_snapshots(
                        self.apiclient,
                        volumeid=volume.id,
                        intervaltype=\
                        self.services["recurring_snapshot"]["intervaltype"],
                        snapshottype='RECURRING',
                        listall=True
                        )

        self.assertEqual(
                            isinstance(snapshots, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                         len(snapshots),
                         self.services["recurring_snapshot"]["maxsnaps"],
                         "Check maximum number of recurring snapshots retained"
                        )
        snapshot = snapshots[0]
        # Sleep to ensure that snapshot is reflected in sec storage
        time.sleep(self.services["sleep"])
        self.assertTrue(is_snapshot_on_nfs(self.apiclient, self.dbclient, self.config, self.zone.id, snapshot.id))
        return
