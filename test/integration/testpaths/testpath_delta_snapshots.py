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
""" Test cases for Delta Snapshots Test Path
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              is_snapshot_on_nfs)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             Template,
                             VirtualMachine,
                             Volume,
                             Configurations,
                             Snapshot
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               createChecksum,
                               compareChecksum
                               )
from marvin.sshClient import SshClient
from marvin.codes import (PASS, FAIL, BACKED_UP, ROOT, DATA)
import time


def checkIntegrityOfSnapshot(
        self, snapshotsToRestore, checksumToCompare, disk_type=ROOT):
    """
    Check integrity of snapshot created of ROOT or DATA Disk:

    If ROOT Disk: Deploy a Vm from a template created from the snapshot
                  and checking the contents of the ROOT disk.
    If DATA Disk: Users can create a volume from the snapshot.
                  The volume can then be mounted to a VM and files
                  recovered as needed.

    Inputs:
    1. snapshotsToRestore: Snapshots whose integrity is
                           to be checked.

    2. checksumToCompare:  The contents of ROOT Disk to be compared.

    3. disk_type:          The type of disk - ROOT or DATA Disk
                           of which snapshot was created.

    """
    if disk_type == ROOT:
        # Create template from snapshot
        template_from_snapshot = Template.create_from_snapshot(
            self.apiclient,
            snapshotsToRestore,
            self.testdata["template_2"])

        self.assertNotEqual(
            template_from_snapshot,
            None,
            "Check if result exists in list item call"
        )

        # Deploy VM
        vm_from_temp = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=template_from_snapshot.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        self.assertNotEqual(
            vm_from_temp,
            None,
            "Check if result exists in list item call"
        )

        # Verify contents of ROOT disk match with snapshot

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksumToCompare,
            disk_type="rootdiskdevice",
            virt_machine=vm_from_temp
        )

        vm_from_temp.delete(self.apiclient)
        template_from_snapshot.delete(self.apiclient)
    else:
        volumeFormSnap = Volume.create_from_snapshot(
            self.apiclient,
            snapshotsToRestore.id,
            self.testdata["volume"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

        temp_vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )
        temp_vm.attach_volume(
            self.apiclient,
            volumeFormSnap
        )

        temp_vm.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            self.testdata,
            checksumToCompare,
            "datadiskdevice_1",
            temp_vm
        )

        temp_vm.delete(self.apiclient)
        volumeFormSnap.delete(self.apiclient)

    return


class TestDeltaSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeltaSnapshots, cls).getClsTestClient()
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

        cls.snapshots_created = []
        cls._cleanup = []

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        cls.skiptest = False

        if cls.hypervisor.lower() not in ["xenserver"]:
            cls.skiptest = True

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

            if cls.testdata["configurableData"][
                    "restartManagementServerThroughTestCase"]:
                # Set snapshot delta max value
                Configurations.update(cls.apiclient,
                                      name="snapshot.delta.max",
                                      value="3"
                                      )

                # Restart management server
                cls.RestartServer()
                time.sleep(120)

            configs = Configurations.list(
                cls.apiclient,
                name="snapshot.delta.max")
            cls.delta_max = configs[0].value

            cls.vm = VirtualMachine.create(
                cls.apiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
                mode=cls.zone.networktype
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
        if self.skiptest:
            self.skipTest(
                "Delta snapshot not supported on %s" %
                self.hypervisor)
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            for snapshot in self.snapshots_created:
                snapshot.delete(self.apiclient)
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def RestartServer(cls):
        """Restart management server"""

        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management restart"
        sshClient.execute(command)

        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_delta_snapshots(self):
        """ Delta Snapshots
            1. Create file on ROOT disk of deployed VM.
            2. Create Snapshot of ROOT disk.
            3. Verify secondary storage count.
            4. Check integrity of Full Snapshot.
            5. Delete delta snaphshot and check integrity of\
               remaining snapshots.
            6. Delete full snapshot and verify it is deleted from\
               secondary storage.
        """
        checksum_created = []
        full_snapshot_count = 0
        delta_snapshot_count = 0

        # Mulitply delta max value by 2 to set loop counter
        # to create 2 Snapshot chains
        snapshot_loop_count = int(self.delta_max) * 2

        # Get ROOT Volume
        root_volumes_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm.id,
            type=ROOT,
            listall=True
        )

        status = validateList(root_volumes_list)
        self.assertEqual(
            status[0],
            PASS,
            "Check listVolumes response for ROOT Disk")

        root_volume = root_volumes_list[0]

        # Get Secondary Storage Value from Database
        qryresult_before_snapshot = self.dbclient.execute(
            " select id, account_name, secondaryStorageTotal\
                    from account_view where account_name = '%s';" %
            self.account.name)

        self.assertNotEqual(
            len(qryresult_before_snapshot),
            0,
            "Check sql query to return SecondaryStorageTotal of account")

        storage_qry_result_old = qryresult_before_snapshot[0]
        secondary_storage_old = storage_qry_result_old[2]

        # Create Snapshots

        for i in range(snapshot_loop_count):

            # Step 1
            checksum_root = createChecksum(
                self.testdata,
                self.vm,
                root_volume,
                "rootdiskdevice")

            time.sleep(30)
            checksum_created.append(checksum_root)

            # Step 2
            root_vol_snapshot = Snapshot.create(
                self.apiclient,
                root_volume.id)

            self.snapshots_created.append(root_vol_snapshot)

            snapshots_list = Snapshot.list(self.apiclient,
                                           id=root_vol_snapshot.id)

            status = validateList(snapshots_list)
            self.assertEqual(status[0], PASS, "Check listSnapshots response")

            # Verify Snapshot state
            self.assertEqual(
                snapshots_list[0].state.lower() in [
                    BACKED_UP,
                ],
                True,
                "Snapshot state is not as expected. It is %s" %
                snapshots_list[0].state
            )

            self.assertEqual(
                snapshots_list[0].volumeid,
                root_volume.id,
                "Snapshot volume id is not matching with the vm's volume id")

            # Step 3
            qryresult_after_snapshot = self.dbclient.execute(
                " select id, account_name, secondaryStorageTotal\
                        from account_view where account_name = '%s';" %
                self.account.name)

            self.assertNotEqual(
                len(qryresult_after_snapshot),
                0,
                "Check sql query to return SecondaryStorageTotal of account")

            storage_qry_result_from_database = qryresult_after_snapshot[0]
            secondary_storage_from_database = storage_qry_result_from_database[
                2]

            snapshot_size = snapshots_list[0].physicalsize
            secondary_storage_after_snapshot = secondary_storage_old + \
                snapshot_size

            # Reset full_snapshot_count to 0 before start of new snapshot chain
            if delta_snapshot_count == (int(self.delta_max) - 1):
                full_snapshot_count = 0
                delta_snapshot_count = 0

            # Full Snapshot of each Snapshot chain
            if full_snapshot_count == 0:

                full_snapshot_count += 1
                full_snapshot_size = snapshot_size

                # Check secondary storage count for Full Snapshots
                self.assertEqual(
                    secondary_storage_from_database,
                    secondary_storage_after_snapshot,
                    "Secondary storage count after full snapshot\
                                should be incremented by size of snapshot.")
                # Step 4
                checkIntegrityOfSnapshot(
                    self,
                    snapshots_list[0],
                    checksum_root,
                    disk_type=DATA)

            else:

                # Delta Snapshot of each Snapshot chain
                delta_snapshot_count += 1
                delta_snapshot_size = snapshot_size

                # Check secondary storage count for Delta Snapshots
                self.assertTrue(delta_snapshot_size < full_snapshot_size,
                                "Delta Snapshot size should be less than\
                                Full Snapshot.")

                self.assertEqual(
                    secondary_storage_from_database,
                    secondary_storage_after_snapshot,
                    "Secondary storage count after delta snapshot\
                                should be incremented by size of snapshot.")

            secondary_storage_old = secondary_storage_from_database

        # Step 5

        # Check in Secondary Storage- Snapshots: S1, S2, S3 are present
        for i in range(int(self.delta_max)):
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    self.snapshots_created[i].id),
                "Check: Snapshot is not on Secondary Storage.")

        # Delete S2

        Snapshot.delete(self.snapshots_created[1], self.apiclient)

        snapshots_list = Snapshot.list(self.apiclient,
                                       id=self.snapshots_created[1].id)

        status = validateList(snapshots_list)
        self.assertEqual(status[0], FAIL, "Snapshots Not Deleted.")

        # Check integrity of Snapshots for S1 and S3
        checkIntegrityOfSnapshot(
            self,
            self.snapshots_created[0],
            checksum_created[0],
            disk_type=DATA)

        checkIntegrityOfSnapshot(
            self,
            self.snapshots_created[2],
            checksum_created[2],
            disk_type=DATA)

        # Delete S3

        Snapshot.delete(self.snapshots_created[2], self.apiclient)

        snapshots_list = Snapshot.list(self.apiclient,
                                       id=self.snapshots_created[2].id)

        status = validateList(snapshots_list)
        self.assertEqual(status[0], FAIL, "Snapshots Not Deleted.")

        # Check in Secondary Storage - Snapshots: S2, S3 are deleted
        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                self.snapshots_created[2].id),
            "Check: Snapshot 2 is still on Secondary Storage. Not Deleted.")

        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                self.snapshots_created[1].id),
            "Check: Snapshot 3 is still on Secondary Storage. Not Deleted.")

        # Restore Snapshots for S1
        checkIntegrityOfSnapshot(
            self,
            self.snapshots_created[0],
            checksum_created[0],
            disk_type=DATA)

        # Step 6
        # Delete S1

        Snapshot.delete(self.snapshots_created[0], self.apiclient)

        snapshots_list = Snapshot.list(self.apiclient,
                                       id=self.snapshots_created[0].id)

        status = validateList(snapshots_list)
        self.assertEqual(status[0], FAIL, "Snapshots Not Deleted.")

        # Check in Secondary Storage - Snapshots: All - S1, S2, S3 are deleted

        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                self.snapshots_created[2].id),
            "Check: Snapshot 3 is still on Secondary Storage. Not Deleted.")

        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                self.snapshots_created[1].id),
            "Check: Snapshot 2 is still on Secondary Storage. Not Deleted.")

        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                self.snapshots_created[0].id),
            "Check: Snapshot 1 is still on Secondary Storage. Not Deleted.")

        return
