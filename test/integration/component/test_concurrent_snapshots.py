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
                             Volume,
                             DiskOffering,
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
import datetime

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
                        "recurring_daily_snapshot": {
                                    "intervaltype": 'DAILY',
                                    # Frequency of snapshots
                                    "maxsnaps": 2,  # Should be min 2
                                    "schedule": (datetime.datetime.now() + datetime.timedelta(minutes = 5)).strftime("%M:%H"),
                                    "timezone": 'IST',
                                    # Timezone Formats - http://cloud.mindtouch.us/CloudStack_Documentation/Developer's_Guide%3A_CloudStack
                                },
                        "recurring_weekly_snapshot": {
                            "intervaltype": 'WEEKLY',
                            # Frequency of snapshots
                            "maxsnaps": 2,  # Should be min 2
                            "timezone": 'IST',
                            "schedule":  (datetime.datetime.now() + datetime.timedelta(days = 1) + datetime.timedelta(minutes = 5)).strftime("%M:%H:%w"),
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
            cls.disk_offering = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering"]
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
        except Exception, e:
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
    def test_01_snapshot_concurrency(self):
        """Test concurrent snapshots using snapshot policies
        """
        # Validate the following
        # Step1: Creating a data volume
        # Step2: Listing the volumes for a user after creating data volume
        # Step3: Attaching and Detaching data volume created to Virtual Machine
        # Step4: Perform daily and weekly recurring snapshot at the same time on the root
        #         disk of VM and keep the maxsnapshots as 1
        # 2. listSnapshots should list the snapshot that was created
        #    snapshot folder in secondary storage should contain only one
        #    snapshot image(/secondary/snapshots/$accountid/$volumeid/)


        # Listing volumes for a user before creating a volume
        list_volumes_before = Volume.list(
            self.apiclient,
            #virtualmachineid=self.virtual_machine.id,
            type="DATADISK",
            account=self.account.name
        )

        # Creating a data volume
        volume_created = Volume.create(
            self.apiclient,
            self.services["volume"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.assertIsNotNone(volume_created, "Data volume creation failed")

        # Listing volumes for a user after creating data volume
        list_volumes_after = Volume.list(
            self.apiclient,
            type="DATADISK",
            account=self.account.name
        )

        # Attaching data volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.apiclient,
            volume_created
        )

        list_volumes = Volume.list(
            self.apiclient,
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEquals(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )

        # Detaching data volume from Virtual Machine
        self.virtual_machine.detach_volume(
            self.apiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.apiclient,
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )

        # Create a snapshot policy
        recurring_daily_snapshot = SnapshotPolicy.create(
                                           self.apiclient,
                                           detached_volume.id,
                                           self.services["recurring_daily_snapshot"]
                                        )
        recurring_weekly_snapshot = SnapshotPolicy.create(
                                           self.apiclient,
                                           detached_volume.id,
                                           self.services["recurring_weekly_snapshot"]
                                        )

        self.cleanup.append(recurring_daily_snapshot)

        snapshot_daily_policy = list_snapshot_policy(
                                        self.apiclient,
                                        id=recurring_daily_snapshot.id,
                                        volumeid=detached_volume.id
                                        )
        snapshot_weekly_policy = list_snapshot_policy(
                                        self.apiclient,
                                        id=recurring_weekly_snapshot.id,
                                        volumeid=detached_volume.id
                                        )
        self.assertEqual(
                            isinstance(snapshot_daily_policy, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            snapshot_weekly_policy,
                            None,
                            "Check if result exists in list item call"
                            )

        self.assertEqual(
                        snapshot_daily_policy[0].id,
                        recurring_daily_snapshot.id,
                        "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                        snapshot_daily_policy[0].maxsnaps,
                        self.services["recurring_daily_snapshot"]["maxsnaps"],
                        "Check interval type in list resources call"
                        )
        # Sleep for (maxsnaps+1) hours to verify
        # only maxsnaps snapshots are retained
        time.sleep(
            (int(self.services["recurring_daily_snapshot"]["maxsnaps"]) + 1) * 3600
            )

        # Verify the snapshot was created or not
        snapshots = list_snapshots(
                        self.apiclient,
                        volumeid=detached_volume.id,
                        intervaltype=\
                        self.services["recurring_daily_snapshot"]["intervaltype"],
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
                         self.services["recurring_daily_snapshot"]["maxsnaps"],
                         "Check maximum number of recurring snapshots retained"
                        )
        snapshot = snapshots[0]
        # Sleep to ensure that snapshot is reflected in sec storage
        time.sleep(self.services["sleep"])
        self.assertTrue(is_snapshot_on_nfs(self.apiclient, self.dbclient, self.config, self.zone.id, snapshot.id))
        return
