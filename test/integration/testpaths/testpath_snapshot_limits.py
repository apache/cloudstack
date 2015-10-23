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
""" Test cases for Storage and Snapshot Limits Test Path
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             DiskOffering,
                             Volume,
                             Resources,
                             VirtualMachine,
                             Snapshot
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template
                               )

from marvin.codes import (BACKED_UP, PASS, FAIL)


class TestStorageSnapshotsLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestStorageSnapshotsLimits, cls).getClsTestClient()
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
        cls.snapshotSupported = True

        if cls.hypervisor.lower() in ["hyperv", "lxc"]:
            cls.snapshotSupported = False
            return

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

            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
            )

            cls._cleanup.append(cls.disk_offering)

            cls.vm = VirtualMachine.create(
                cls.userapiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
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
        if not self.snapshotSupported:
            self.skipTest(
                "Snapshots are not supported on %s" %
                self.hypervisor)


    def tearDown(self):
        try:
            data_volumes_list = Volume.list(
                self.userapiclient,
                id=self.data_volume_created.id,
                virtualmachineid=self.vm.id
            )
            status = validateList(data_volumes_list)
            self.assertEqual(
                status[0],
                PASS,
                "DATA Volume List Validation Failed")

            self.vm.detach_volume(
                self.userapiclient,
                data_volumes_list[0]
            )

            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["basic", "advanced"], required_hardware="true")
    def test_01_storage_snapshots_limits(self):
        """ Storage and Snapshot Limit
            1.   Create Snapshot of ROOT disk.
            2.   Verify the Secondary Storage value
                 is increased by the size of snapshot.
            3.   Delete Snaphshot.
            4.   Verify the Secondary
                 Storage value is decreased by the size of snapshot.
            5.   Set the Snapshot limit of Account.
            6.   Create Snasphots till limit is reached.
            7.   Create Snapshot of ROOT Volume.
                 Creation should fail.
            8.   Delete few Snapshots.
            9.   Create Snapshot again.
                 Creation should succeed.
        """

        # Get ROOT Volume
        root_volumes_list = Volume.list(
            self.userapiclient,
            virtualmachineid=self.vm.id,
            type='ROOT'
        )

        status = validateList(root_volumes_list)
        self.assertEqual(status[0], PASS, "ROOT Volume List Validation Failed")

        root_volume = root_volumes_list[0]

        self.data_volume_created = Volume.create(
            self.userapiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )

        self.cleanup.append(self.data_volume_created)

        data_volumes_list = Volume.list(
            self.userapiclient,
            id=self.data_volume_created.id
        )

        status = validateList(data_volumes_list)
        self.assertEqual(status[0], PASS, "DATA Volume List Validation Failed")

        self.data_volume = data_volumes_list[0]

        self.vm.attach_volume(
            self.userapiclient,
            self.data_volume
        )

        # Get Secondary Storage Value from Database
        qryresult_before_snapshot = self.dbclient.execute(
            " select id, account_name, secondaryStorageTotal\
                    from account_view where account_name = '%s';" %
            self.account.name)

        status = validateList(qryresult_before_snapshot)
        self.assertEqual(
            status[0],
            PASS,
            "Check sql query to return SecondaryStorageTotal of account")

        secStorageBeforeSnapshot = qryresult_before_snapshot[0][2]

        # Step 1
        snapshot = Snapshot.create(
            self.userapiclient,
            root_volume.id)

        snapshots_list = Snapshot.list(self.userapiclient,
                                       id=snapshot.id)

        status = validateList(snapshots_list)
        self.assertEqual(status[0], PASS, "Snapshots List Validation Failed")

        # Verify Snapshot state
        self.assertEqual(
            snapshots_list[0].state.lower() in [
                BACKED_UP,
            ],
            True,
            "Snapshot state is not as expected. It is %s" %
            snapshots_list[0].state
        )

        # Step 2
        qryresult_after_snapshot = self.dbclient.execute(
            " select id, account_name, secondaryStorageTotal\
                        from account_view where account_name = '%s';" %
            self.account.name)

        status = validateList(qryresult_after_snapshot)
        self.assertEqual(
            status[0],
            PASS,
            "Check sql query to return SecondaryStorageTotal of account")

        secStorageAfterSnapshotCreated = qryresult_after_snapshot[0][2]

        snapshot_size = snapshots_list[0].physicalsize
        secStorageIncreased = secStorageBeforeSnapshot + \
            snapshot_size

        self.assertEqual(
            secStorageIncreased,
            secStorageAfterSnapshotCreated,
            "Secondary storage Total after Snapshot\
                        should be incremented by size of snapshot.")

        # Step 3
        snapshot.delete(self.apiclient)

        snapshots_list = Snapshot.list(self.userapiclient,
                                       id=snapshot.id)

        status = validateList(snapshots_list)
        self.assertEqual(status[0], FAIL, "Snapshots Not Deleted.")

        # Step 4
        qryresult_after_snapshot_deleted = self.dbclient.execute(
            " select id, account_name, secondaryStorageTotal\
                        from account_view where account_name = '%s';" %
            self.account.name)

        status = validateList(qryresult_after_snapshot_deleted)
        self.assertEqual(
            status[0],
            PASS,
            "Check sql query to return SecondaryStorageTotal of account")

        secStorageAfterSnapshotDeleted = qryresult_after_snapshot_deleted[0][2]

        secStorageDecreased = secStorageAfterSnapshotCreated - \
            snapshot_size

        self.assertEqual(
            secStorageDecreased,
            secStorageAfterSnapshotDeleted,
            "Secondary storage Total after Snapshot\
                        should be incremented by size of snapshot.")

        # Step 5
        # Set Snapshot Limit for account
        Resources.updateLimit(self.apiclient, resourcetype=3,
                              max=1, account=self.account.name,
                              domainid=self.account.domainid)

        # Step 6
        snapshot = Snapshot.create(
            self.userapiclient,
            root_volume.id)

        snapshots_list = Snapshot.list(self.userapiclient,
                                       id=snapshot.id)

        status = validateList(snapshots_list)
        self.assertEqual(status[0], PASS, "Snapshots List Validation Failed")

        # Verify Snapshot state
        self.assertEqual(
            snapshots_list[0].state.lower() in [
                BACKED_UP,
            ],
            True,
            "Snapshot state is not as expected. It is %s" %
            snapshots_list[0].state
        )

        # Step 7
        with self.assertRaises(Exception):
            Snapshot.create(
                self.userapiclient,
                self.data_volume.id)

        # Step 8
        snapshot.delete(self.userapiclient)

        snapshots_list = Snapshot.list(self.userapiclient,
                                       id=snapshot.id)

        status = validateList(snapshots_list)
        self.assertEqual(status[0], FAIL, "Snapshots Not Deleted.")

        # Step 9
        snapshot = Snapshot.create(
            self.userapiclient,
            root_volume.id)

        snapshots_list = Snapshot.list(self.userapiclient,
                                       id=snapshot.id)

        status = validateList(snapshots_list)
        self.assertEqual(status[0], PASS, "Snapshots List Validation Failed")

        # Verify Snapshot state
        self.assertEqual(
            snapshots_list[0].state.lower() in [
                BACKED_UP,
            ],
            True,
            "Snapshot state is not as expected. It is %s" %
            snapshots_list[0].state
        )

        return
