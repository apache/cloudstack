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
""" Test cases for VM/Volume recurring snapshot Test Path
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import (cleanup_resources,
                              is_snapshot_on_nfs,
                              validateList
                              )
from marvin.lib.base import (Account,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine,
                             SnapshotPolicy,
                             Snapshot,
                             Configurations
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_snapshots,
                               list_snapshot_policy
                               )

from marvin.codes import PASS

import time


class TestVolumeRecurringSnapshot(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVolumeRecurringSnapshot, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []

        if cls.hypervisor.lower() not in [
                "vmware",
                "kvm",
                "xenserver"]:
            raise unittest.SkipTest(
                "Storage migration not supported on %s" %
                cls.hypervisor)

        try:
            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)
            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
            # Create Service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
            )
            cls._cleanup.append(cls.service_offering)
            # Create Disk offering
            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
            )
            cls._cleanup.append(cls.disk_offering)
            # Deploy A VM
            cls.vm_1 = VirtualMachine.create(
                cls.userapiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
                diskofferingid=cls.disk_offering.id,
                mode=cls.zone.networktype
            )

            cls.volume = list_volumes(
                cls.apiclient,
                virtualmachineid=cls.vm_1.id,
                type='ROOT',
                listall=True
            )

            cls.data_volume = list_volumes(
                cls.apiclient,
                virtualmachineid=cls.vm_1.id,
                type='DATADISK',
                listall=True
            )

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"],required_hardware="true")
    def test_01_volume_snapshot(self):
        """ Test Volume (root) Snapshot
        # 1. Create Hourly, Daily,Weekly recurring snapshot policy for ROOT disk and 
                    Verify the presence of the corresponding snapshots on the Secondary Storage
        # 2. Delete the snapshot policy and verify the entry as Destroyed in snapshot_schedule
        # 3. Verify that maxsnaps should not consider manual snapshots for deletion
        # 4. Snapshot policy should reflect the correct timezone
        # 5. Verify that listSnapshotPolicies() should return all snapshot policies
                that belong to the account (both manual and recurring snapshots)
        # 6. Verify that listSnapshotPolicies() should not return snapshot 
                policies that have been deleted
        # 7. Verify that snapshot should not be created for VM in Destroyed state
        # 8. Verify that snapshot should get created after resuming the VM
        # 9. Verify that All the recurring policies associated with the VM should be 
                deleted after VM get destroyed.
        """
        # Step 1
        self.testdata["recurring_snapshot"]["intervaltype"] = 'HOURLY'

        recurring_snapshot = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )
        # ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot.id,
            volumeid=self.volume[0].id
        )
        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        timeout = self.testdata["timeout"]
        while True:
            snapshots = list_snapshots(
                self.apiclient,
                volumeid=self.volume[0].id,
                intervaltype=self.testdata[
                    "recurring_snapshot"]["intervaltype"],
                snapshottype='RECURRING',
                listall=True
            )

            if isinstance(snapshots, list):
                break

            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

        for snapshot in snapshots:
            self.assertEqual(
                self.dbclient.execute(
                    "select type_description from snapshots where name='%s'" %
                    snapshot.name)[0][0],
                "HOURLY"
            )

        time.sleep(180)

        for snapshot in snapshots:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        recurring_snapshot.delete(self.apiclient)

        self.assertEqual(
            self.dbclient.execute(
                "select * from snapshot_policy where uuid='%s'" %
                recurring_snapshot.id),
            []
        )

        self.testdata["recurring_snapshot"]["intervaltype"] = 'DAILY'
        self.testdata["recurring_snapshot"]["schedule"] = '00:00'
        recurring_snapshot_daily = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        list_snapshots_policy_daily = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_daily.id,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(list_snapshots_policy_daily)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        snap_db_daily = self.dbclient.execute(
            "select * from snapshot_policy where uuid='%s'" %
            recurring_snapshot_daily.id)

        validation_result_1 = validateList(snap_db_daily)

        self.assertEqual(
            validation_result_1[0],
            PASS,
            "snapshot_policy list validation failed due to %s" %
            validation_result_1[2])

        self.assertNotEqual(
            len(snap_db_daily),
            0,
            "Check DB Query result set"
        )

        recurring_snapshot_daily.delete(self.apiclient)

        self.assertEqual(
            self.dbclient.execute(
                "select * from snapshot_policy where uuid='%s'" %
                recurring_snapshot_daily.id),
            []
        )
    
        self.testdata["recurring_snapshot"]["intervaltype"] = 'WEEKLY'
        self.testdata["recurring_snapshot"]["schedule"] = '00:00:1'
        recurring_snapshot_weekly = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        list_snapshots_policy_weekly = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_weekly.id,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(list_snapshots_policy_weekly)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        snap_sch_2 = self.dbclient.execute(
            "select * from snapshot_policy where uuid='%s'" %
            recurring_snapshot_weekly.id)

        validation_result_2 = validateList(snap_sch_2)

        self.assertEqual(
            validation_result_2[0],
            PASS,
            "snapshot_policy list validation failed due to %s" %
            validation_result_2[2])

        self.assertNotEqual(
            len(snap_sch_2),
            0,
            "Check DB Query result set"
        )

        recurring_snapshot_weekly.delete(self.apiclient)

        self.assertEqual(
            self.dbclient.execute(
                "select * from snapshot_policy where uuid='%s'" %
                recurring_snapshot_weekly.id),
            []
        )

        self.testdata["recurring_snapshot"]["intervaltype"] = 'MONTHLY'
        self.testdata["recurring_snapshot"]["schedule"] = '00:00:1'
        recurring_snapshot_monthly = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        list_snapshots_policy_monthly = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_monthly.id,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(list_snapshots_policy_monthly)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        snap_sch_3 = self.dbclient.execute(
            "select * from snapshot_policy where uuid='%s'" %
            recurring_snapshot_monthly.id)

        validation_result = validateList(snap_sch_3)

        self.assertEqual(
            validation_result[0],
            PASS,
            "snapshot_policy list validation failed due to %s" %
            validation_result[2])

        self.assertNotEqual(
            len(snap_sch_3),
            0,
            "Check DB Query result set"
        )

        recurring_snapshot_monthly.delete(self.apiclient)

        self.assertEqual(
            self.dbclient.execute(
                "select * from snapshot_policy where uuid='%s'" %
                recurring_snapshot_weekly.id),
            []
        )

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.volume[0].id,
            intervaltype=self.testdata["recurring_snapshot"]["intervaltype"],
            snapshottype='RECURRING',
            listall=True
        )

        # Step 3
        self.testdata["recurring_snapshot"]["intervaltype"] = 'HOURLY'
        self.testdata["recurring_snapshot"]["schedule"] = 1
        recurring_snapshot_1 = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )
        # ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_1.id,
            volumeid=self.volume[0].id
        )
        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        timeout = self.testdata["timeout"]
        while True:
            snapshots = list_snapshots(
                self.apiclient,
                volumeid=self.volume[0].id,
                intervaltype=self.testdata[
                    "recurring_snapshot"]["intervaltype"],
                snapshottype='RECURRING',
                listall=True
            )

            if isinstance(snapshots, list):
                break

            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

        snap_to_delete = snapshots[0]

        time.sleep(
            (self.testdata["recurring_snapshot"]["maxsnaps"]) * 3600
        )

        snapshots_1 = list_snapshots(
            self.apiclient,
            volumeid=self.volume[0].id,
            intervaltype=self.testdata["recurring_snapshot"]["intervaltype"],
            snapshottype='RECURRING',
            listall=True
        )

        self.assertTrue(snap_to_delete not in snapshots_1)

        time.sleep(360)

        self.assertEqual(
            self.dbclient.execute(
                "select status  from snapshots where uuid='%s'" %
                snap_to_delete.id)[0][0],
            "Destroyed"
        )

        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                snap_to_delete.id))

        # Step 4
        recurring_snapshot = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )
        # ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot.id,
            volumeid=self.volume[0].id
        )
        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        time.sleep(180)
        snap_time_hourly = self.dbclient.execute(
            "select scheduled_timestamp from \
            snapshot_schedule where uuid='%s'" %
            recurring_snapshot.id)

        self.debug("Timestamp for hourly snapshot %s" % snap_time_hourly)
        recurring_snapshot.delete(self.apiclient)

        self.testdata["recurring_snapshot"]["intervaltype"] = 'DAILY'
        self.testdata["recurring_snapshot"]["schedule"] = '00:00'
        recurring_snapshot_daily = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        list_snapshots_policy_daily = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_daily.id,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(list_snapshots_policy_daily)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        time.sleep(180)
        snap_time_daily = self.dbclient.execute(
            "select scheduled_timestamp from \
                    snapshot_schedule where uuid='%s'" %
            recurring_snapshot_daily.id)

        self.debug("Timestamp for daily snapshot %s" % snap_time_daily)
        recurring_snapshot_daily.delete(self.apiclient)

        self.testdata["recurring_snapshot"]["intervaltype"] = 'WEEKLY'
        self.testdata["recurring_snapshot"]["schedule"] = '00:00:1'
        recurring_snapshot_weekly = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        list_snapshots_policy_weekly = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_weekly.id,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(list_snapshots_policy_weekly)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        time.sleep(180)
        snap_time_weekly = self.dbclient.execute(
            "select scheduled_timestamp from \
                    snapshot_schedule where uuid='%s'" %
            recurring_snapshot_weekly.id)

        self.debug("Timestamp for monthly snapshot %s" % snap_time_weekly)
        recurring_snapshot_weekly.delete(self.apiclient)

        self.testdata["recurring_snapshot"]["intervaltype"] = 'MONTHLY'
        self.testdata["recurring_snapshot"]["schedule"] = '00:00:1'
        recurring_snapshot_monthly = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        list_snapshots_policy_monthly = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_monthly.id,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(list_snapshots_policy_monthly)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        time.sleep(180)
        snap_time_monthly = self.dbclient.execute(
            "select scheduled_timestamp from \
                    snapshot_schedule where uuid='%s'" %
            recurring_snapshot_monthly.id)

        self.debug("Timestamp for monthly snapshot %s" % snap_time_monthly)

        recurring_snapshot_monthly.delete(self.apiclient)

        # Step 5
        self.testdata["recurring_snapshot"]["intervaltype"] = 'HOURLY'
        self.testdata["recurring_snapshot"]["schedule"] = 1
        recurring_snapshot_hourly = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )
        self.testdata["recurring_snapshot"]["intervaltype"] = 'MONTHLY'
        self.testdata["recurring_snapshot"]["schedule"] = '00:00:1'
        recurring_snapshot_monthly = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        for rec in [recurring_snapshot_hourly, recurring_snapshot_monthly]:
            self.assertTrue(
                rec.id in any(
                    policy['id']) for policy in list_snapshots_policy)

        recurring_snapshot_hourly.delete(self.apiclient)
        recurring_snapshot_monthly.delete(self.apiclient)

        # Step 6
        self.testdata["recurring_snapshot"]["intervaltype"] = 'HOURLY'
        self.testdata["recurring_snapshot"]["schedule"] = 1
        recurring_snapshot_hourly = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        self.testdata["recurring_snapshot"]["intervaltype"] = 'MONTHLY'
        self.testdata["recurring_snapshot"]["schedule"] = '00:00:1'
        recurring_snapshot_monthly = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        recurring_snapshot_monthly.delete(self.apiclient)

        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        self.assertTrue(
            recurring_snapshot_hourly.id in any(
                policy['id']) for policy in list_snapshots_policy)

        self.assertTrue(
            recurring_snapshot_monthly.id not in any(
                policy['id']) for policy in list_snapshots_policy)

        # Step 7
        self.testdata["recurring_snapshot"]["intervaltype"] = 'HOURLY'
        self.testdata["recurring_snapshot"]["schedule"] = 1
        recurring_snapshot = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )
        # ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot.id,
            volumeid=self.volume[0].id
        )
        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        timeout = self.testdata["timeout"]
        while True:
            snapshots = list_snapshots(
                self.apiclient,
                volumeid=self.volume[0].id,
                intervaltype=self.testdata[
                    "recurring_snapshot"]["intervaltype"],
                snapshottype='RECURRING',
                listall=True
            )

            if isinstance(snapshots, list):
                break

            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

        self.vm_1.delete(self.apiclient, expunge=False)

        time.sleep(3600)
        snapshot_list = Snapshot.list(
            self.apiclient,
            volumeid=self.volume[0].id
        )

        list_validation = validateList(snapshot_list)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        self.assertEqual(len(snapshot_list),
                         1,
                         "Verify that snapsot is not created after VM deletion"
                         )
        # Step 8
        self.vm_1.recover(self.apiclient)
        time.sleep(3600)

        snapshot_list = Snapshot.list(
            self.apiclient,
            volumeid=self.volume[0].id
        )

        self.assertEqual(len(snapshot_list),
                         2,
                         "Verify that snapsot is not created after VM deletion"
                         )
        # Step 9
        self.vm_1.delete(self.apiclient)
        time.sleep(180)
        with self.assertRaises(Exception):
            list_snapshots_policy = list_snapshot_policy(
                self.apiclient,
                volumeid=self.volume[0].id
            )

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_02_volume_max_snapshot(self):
        """ Test Volume Snapshot
        # 1. Create Hourly reccuring snapshot policy with maxsnaps=2 
                verify that when 3rd snapshot is taken first snapshot gets deleted
        """

        if self.hypervisor.lower() not in ["kvm", "vmware"]:
            self.skipTest("Skip test for hypervisor other than KVM and VMWare")

        # Step 1
        self.testdata["recurring_snapshot"]["intervaltype"] = 'HOURLY'
        self.testdata["recurring_snapshot"]["schedule"] = 1
        recurring_snapshot_1 = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )
        # ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_1.id,
            volumeid=self.volume[0].id
        )
        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        timeout = self.testdata["timeout"]
        while True:
            snapshots = list_snapshots(
                self.apiclient,
                volumeid=self.volume[0].id,
                intervaltype=self.testdata[
                    "recurring_snapshot"]["intervaltype"],
                snapshottype='RECURRING',
                listall=True
            )

            if isinstance(snapshots, list):
                break

            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

        snap_to_delete = snapshots[0]

        time.sleep(
            (self.testdata["recurring_snapshot"]["maxsnaps"]) * 3600
        )
        
        snapshots_1 = list_snapshots(
            self.apiclient,
            volumeid=self.volume[0].id,
            intervaltype=self.testdata["recurring_snapshot"]["intervaltype"],
            snapshottype='RECURRING',
            listall=True
        )

        self.assertTrue(snap_to_delete not in snapshots_1)

        time.sleep(360)

        self.assertEqual(
            self.dbclient.execute(
                "select status  from snapshots where uuid='%s'" %
                snap_to_delete.id)[0][0],
            "Destroyed"
        )

        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                snap_to_delete.id))

        # DATA DISK
        recurring_snapshot_data = SnapshotPolicy.create(
            self.apiclient,
            self.data_volume[0].id,
            self.testdata["recurring_snapshot"]
        )
        # ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_data.id,
            volumeid=self.data_volume[0].id
        )

        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        timeout = self.testdata["timeout"]
        while True:
            snapshots = list_snapshots(
                self.apiclient,
                volumeid=self.volume[0].id,
                intervaltype=self.testdata[
                    "recurring_snapshot"]["intervaltype"],
                snapshottype='RECURRING',
                listall=True
            )

            if isinstance(snapshots, list):
                break

            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

        data_snap_to_delete = snapshots[0]

        time.sleep(
            (self.testdata["recurring_snapshot"]["maxsnaps"]) * 3600
        )

        data_snapshots_1 = list_snapshots(
            self.apiclient,
            volumeid=self.volume[0].id,
            intervaltype=self.testdata["recurring_snapshot"]["intervaltype"],
            snapshottype='RECURRING',
            listall=True
        )

        self.assertTrue(data_snap_to_delete not in data_snapshots_1)

        time.sleep(360)

        self.assertEqual(
            self.dbclient.execute(
                "select status  from snapshots where uuid='%s'" %
                snap_to_delete.id)[0][0],
            "Destroyed"
        )

        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                data_snap_to_delete.id))

    @attr(tags=["advanced", "basic"],required_hardware="true")
    def test_03_volume_rec_snapshot(self):
        """ Test Volume (root) Snapshot
        # 1. For snapshot.delta.max > maxsnaps verify that when number of snapshot exceeds 
                maxsnaps value previous snapshot should get deleted from database but remain 
                on secondary storage and when the value exceeds snapshot.delta.max the 
                snapshot should get deleted from secondary storage
        """

        if self.hypervisor.lower() != "xenserver":
            self.skipTest("Skip test for hypervisor other than Xenserver")

        # Step 1
        self.testdata["recurring_snapshot"]["intervaltype"] = 'HOURLY'
        self.testdata["recurring_snapshot"]["schedule"] = 1
        recurring_snapshot_root = SnapshotPolicy.create(
            self.apiclient,
            self.volume[0].id,
            self.testdata["recurring_snapshot"]
        )

        Configurations.update(self.apiclient,
                              name="snapshot.delta.max",
                              value="3"
                              )

        list_snapshots_policy = list_snapshot_policy(
            self.apiclient,
            id=recurring_snapshot_root.id,
            volumeid=self.volume[0].id
        )
        list_validation = validateList(list_snapshots_policy)

        self.assertEqual(
            list_validation[0],
            PASS,
            "snapshot list validation failed due to %s" %
            list_validation[2])

        timeout = self.testdata["timeout"]
        while True:
            snapshots = list_snapshots(
                self.apiclient,
                volumeid=self.volume[0].id,
                intervaltype=self.testdata[
                    "recurring_snapshot"]["intervaltype"],
                snapshottype='RECURRING',
                listall=True
            )

            if isinstance(snapshots, list):
                break

            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

        time.sleep(3600 * 2)

        snapshots_2 = list_snapshots(
            self.apiclient,
            volumeid=self.volume[0].id,
            intervaltype=self.testdata["recurring_snapshot"]["intervaltype"],
            snapshottype='RECURRING',
            listall=True
        )

        self.assertTrue(snapshots[0] not in snapshots_2)

        for snapshot in snapshots_2:
            snapshots.append(snapshot)

        time.sleep(360)
        self.assertEqual(
            self.dbclient.execute(
                "select status  from snapshots where uuid='%s'" %
                snapshots[0].id)[0][0],
            "Destroyed"
        )

        self.assertTrue(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                snapshots[0].id))

        time.sleep(3600)

        snapshots_3 = list_snapshots(
            self.apiclient,
            volumeid=self.volume[0].id,
            intervaltype=self.testdata["recurring_snapshot"]["intervaltype"],
            snapshottype='RECURRING',
            listall=True
        )

        self.assertTrue(snapshots[1] not in snapshots_3)
        snapshots.append(snapshots_3[1])
        time.sleep(180)

        self.assertEqual(
            self.dbclient.execute(
                "select status  from snapshots where uuid='%s'" %
                snapshots[1].id)[0][0],
            "Destroyed"
        )

        for snapshot in [snapshots[0], snapshots[1]]:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        time.sleep(3600)

        snapshots_4 = list_snapshots(
            self.apiclient,
            volumeid=self.volume[0].id,
            intervaltype=self.testdata["recurring_snapshot"]["intervaltype"],
            snapshottype='RECURRING',
            listall=True
        )

        self.assertTrue(snapshots[2] not in snapshots_4)

        snapshots.append(snapshots_4[1])
        time.sleep(180)

        self.assertEqual(
            self.dbclient.execute(
                "select status  from snapshots where uuid='%s'" %
                snapshots[2].id)[0][0],
            "Destroyed"
        )

        for snapshot in [snapshots[0], snapshots[1], snapshots[2]]:
            self.assertFalse(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        return
