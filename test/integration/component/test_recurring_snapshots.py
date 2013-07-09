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
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *

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
                         "mgmt_server": {
                                    "ipaddress": '192.168.100.21',
                                    "username": "root",
                                    "password": "password",
                                    "port": 22,
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


class TestRecurringSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestRecurringSnapshots, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.services["domainid"] = cls.domain.id
        cls.services["server_with_disk"]["zoneid"] = cls.zone.id
        cls.services["server_with_disk"]["diskoffering"] = cls.disk_offering.id

        cls.services["server_without_disk"]["zoneid"] = cls.zone.id

        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id
        cls.services["diskoffering"] = cls.disk_offering.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = cls.virtual_machine_with_disk = \
                    VirtualMachine.create(
                                cls.api_client,
                                cls.services["server_with_disk"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )
        cls.virtual_machine_without_disk = \
                    VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server_without_disk"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                    )
        cls._cleanup = [
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
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
    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_recurring_snapshot_root_disk(self):
        """Test Recurring Snapshot Root Disk
        """
        #1. Create snapshot policy for root disk
        #2. ListSnapshot policy should return newly created policy
        #3. Verify only most recent number (maxsnaps) snapshots retailed

        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid=self.virtual_machine_with_disk.id,
                        type='ROOT',
                        listall=True
                        )
        self.assertEqual(
                            isinstance(volume, list),
                            True,
                            "Check list response returns a valid list"
                        )
        recurring_snapshot = SnapshotPolicy.create(
                                           self.apiclient,
                                           volume[0].id,
                                           self.services["recurring_snapshot"]
                                        )
        self.cleanup.append(recurring_snapshot)

        #ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
                                                     self.apiclient,
                                                     id=recurring_snapshot.id,
                                                     volumeid=volume[0].id
                                                     )
        self.assertEqual(
                            isinstance(list_snapshots_policy, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            list_snapshots_policy,
                            None,
                            "Check if result exists in list item call"
                            )
        snapshots_policy = list_snapshots_policy[0]
        self.assertEqual(
                        snapshots_policy.id,
                        recurring_snapshot.id,
                        "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                        snapshots_policy.maxsnaps,
                        self.services["recurring_snapshot"]["maxsnaps"],
                        "Check interval type in list resources call"
                        )
        # Sleep for (maxsnaps+1) hours to verify
        # only maxsnaps snapshots are retained
        time.sleep(
            (self.services["recurring_snapshot"]["maxsnaps"]) * 3600
            )

        timeout = self.services["timeout"]
        while True:
            snapshots = list_snapshots(
                        self.apiclient,
                        volumeid=volume[0].id,
                        intervaltype=\
                        self.services["recurring_snapshot"]["intervaltype"],
                        snapshottype='RECURRING',
                        listall=True
                        )

            if isinstance(snapshots, list):
                break

            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

            time.sleep(1)
            timeout = timeout - 1

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
        return

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_recurring_snapshot_data_disk(self):
        """Test Recurring Snapshot data Disk
        """
        #1. Create snapshot policy for data disk
        #2. ListSnapshot policy should return newly created policy
        #3. Verify only most recent number (maxsnaps) snapshots retailed

        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid=self.virtual_machine_with_disk.id,
                        type='DATADISK',
                        listall=True
                        )

        self.assertEqual(
                            isinstance(volume, list),
                            True,
                            "Check list response returns a valid list"
                        )

        recurring_snapshot = SnapshotPolicy.create(
                                    self.apiclient,
                                    volume[0].id,
                                    self.services["recurring_snapshot"]
                                )
        self.cleanup.append(recurring_snapshot)
        #ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
                                                     self.apiclient,
                                                     id=recurring_snapshot.id,
                                                     volumeid=volume[0].id
                                                     )

        self.assertEqual(
                            isinstance(list_snapshots_policy, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            list_snapshots_policy,
                            None,
                            "Check if result exists in list item call"
                            )
        snapshots_policy = list_snapshots_policy[0]
        self.assertEqual(
                        snapshots_policy.id,
                        recurring_snapshot.id,
                        "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                            snapshots_policy.maxsnaps,
                            self.services["recurring_snapshot"]["maxsnaps"],
                            "Check interval type in list resources call"
                        )

        # Sleep for (maxsnaps) hours to verify only maxsnaps snapshots are
        # retained
        time.sleep(
            (self.services["recurring_snapshot"]["maxsnaps"]) * 3600
            )

        timeout = self.services["timeout"]
        while True:
            snapshots = list_snapshots(
                            self.apiclient,
                            volumeid=volume[0].id,
                            intervaltype=\
                            self.services["recurring_snapshot"]["intervaltype"],
                            snapshottype='RECURRING',
                            listall=True
                            )

            if isinstance(snapshots, list):
                break

            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

            time.sleep(1)
            timeout = timeout - 1

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
        return