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
""" Test cases for VM/Volume snapshot Test Path
    Snapshot Test cases related to handling concurrent snapshots at host level.
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import (cleanup_resources, is_snapshot_on_nfs,
                              validateList)
from marvin.lib.base import (Account,
                             StoragePool,
                             Host,
                             ServiceOffering,
                             VirtualMachine,
                             Configurations,
                             Snapshot,
                             SnapshotPolicy,
                             )
from marvin.lib.common import (get_domain,
                               list_snapshot_policy,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_snapshots,
                               list_virtual_machines,
                               createChecksum,
                               )
from marvin.sshClient import SshClient
import time

from threading import Thread
from marvin.codes import PASS


def MigrateRootVolume(self,
                      vm,
                      destinationHost,
                      expectexception=False):
    """ Migrate given volume to type of storage pool mentioned in migrateto:

        Inputs:
            1. vm:               VM to be migrated
                                 is to be migrated
            2. expectexception:  If exception is expected while migration
            3. destinationHost:  Destination host where the VM\
                                 should get migrated
    """

    if expectexception:
        with self.assertRaises(Exception):
            VirtualMachine.migrate(
                vm,
                self.apiclient,
                hostid=destinationHost.id,
            )
    else:
        VirtualMachine.migrate(
            vm,
            self.apiclient,
            hostid=destinationHost.id,
        )

        migrated_vm_response = list_virtual_machines(
            self.apiclient,
            id=vm.id
        )

        self.assertEqual(
            isinstance(migrated_vm_response, list),
            True,
            "Check list virtual machines response for valid list"
        )

        self.assertNotEqual(
            migrated_vm_response,
            None,
            "Check if virtual machine exists in ListVirtualMachines"
        )

        migrated_vm = migrated_vm_response[0]

        vm_list = VirtualMachine.list(
            self.apiclient,
            id=migrated_vm.id
        )

        self.assertEqual(
            vm_list[0].hostid,
            destinationHost.id,
            "Check volume is on migrated pool"
        )
    return


def CreateSnapshot(self, root_volume, is_recurring):
    """Create Snapshot"""
    if is_recurring:
        self.testdata["recurring_snapshot"]["intervaltype"] = 'HOURLY'
        self.testdata["recurring_snapshot"]["schedule"] = 1

        recurring_snapshot = SnapshotPolicy.create(
            self.apiclient,
            root_volume.id,
            self.testdata["recurring_snapshot"]
        )
        self.rec_policy_pool.append(recurring_snapshot)
    else:
        root_vol_snapshot = Snapshot.create(
            self.apiclient,
            root_volume.id)

        self.snapshot_pool.append(root_vol_snapshot)

    return


class TestConcurrentSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestConcurrentSnapshots, cls).getClsTestClient()
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
        cls.vm_pool = []
        cls.snapshotSupported = True

        if cls.hypervisor.lower() in ["hyperv", "lxc"]:
            cls.snapshotSupported = False
            return

        # Set sleep time as per Snapshot Recurring Policy - HOURLY
        cls.sleep_time_for_hourly_policy = 60 * 60 * 1

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

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

            for i in range(4):
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

                cls.vm_pool.append(cls.vm)
                cls._cleanup.append(cls.vm)

            cls.checksum_pool = []
            cls.root_pool = []
            cls.snapshot_pool = []
            cls.rec_policy_pool = []
            for vm in cls.vm_pool:
                root_volumes = list_volumes(
                    cls.apiclient,
                    virtualmachineid=vm.id,
                    type='ROOT',
                    listall=True
                )

                checksum_root = createChecksum(
                    cls.testdata,
                    vm,
                    root_volumes[0],
                    "rootdiskdevice")

                cls.checksum_pool.append(checksum_root)
                cls.root_pool.append(root_volumes[0])
            try:
                cls.pools = StoragePool.list(cls.apiclient, zoneid=cls.zone.id)
            except Exception as e:
                raise unittest.SkipTest(e)

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            for vm in cls.vm_pool:
                vm.delete(cls.apiclient)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if not self.snapshotSupported:
            self.skipTest("Snapshot is not supported on %s" % self.hypervisor)

    def tearDown(self):
        try:
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

    @classmethod
    def StopVM(cls, vms):
        for vm in vms:
            vm.stop(cls.apiclient)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_concurrent_snapshots(self):
        """Concurrent Snapshots
            1. Create snapshot on 2 new VMs in parallel and check
                    1. all snapshot jobs are running
                    2. listSnapshots should list all the snapshots
                    3. Verify secondary_storage NFS share
                       contains the required volume under
                       /secondary/snapshots/$accountid/$volumeid/$snapshot_uuid.
                    4. Verify backup_snap_id was non null in "snapshots"table
            2. Perform step 1 for all the 4 VM's.
            3. Verify that VM gets migrated when snapshot
                is in pregress for the VM.
            4. Verify that snapshots get created when
                VM's are stoped in between snapshot creation.
            5. Perform live Migration then stop all the
                VM's after that verify that snapshot creation success .
            6. Verify success of snapshots creation in case:
                Stop the running VM while performing
                concurrent snapshot on volumes
            7. Verify success of snapshots creation in case:
                Start Migration of VM's and then Stop the running VM then
                performing concurrent snapshot on volumes
        """
        # Step 1
        try:
            create_snapshot_thread_1 = Thread(
                target=CreateSnapshot,
                args=(
                    self,
                    self.root_pool[0],
                    False))
            create_snapshot_thread_2 = Thread(
                target=CreateSnapshot,
                args=(
                    self,
                    self.root_pool[1],
                    False))
            create_snapshot_thread_1.start()
            create_snapshot_thread_2.start()
            create_snapshot_thread_1.join()
            create_snapshot_thread_2.join()

        except:
            self.debug("Error: unable to start thread")

        snapshots = list_snapshots(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )

        for snapshot in self.snapshot_pool:
            self.assertTrue(snapshot.id in any(
                s.id) for s in snapshots)

        for snapshot in self.snapshot_pool:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        for snapshot in self.snapshot_pool:
            snapshot.delete(self.apiclient)

        self.snapshot_pool = []
        # Step 2
        thread_pool = []
        for i in range(4):
            try:
                create_snapshot_thread_1 = Thread(
                    target=CreateSnapshot,
                    args=(
                        self,
                        self.root_pool[i],
                        False))
                thread_pool.append(create_snapshot_thread_1)

            except Exception as e:
                raise Exception(
                    "Warning: Exception unable to start thread : %s" %
                    e)

        for thread in thread_pool:
            thread.start()

        for thread in thread_pool:
            thread.join()

        snapshots = list_snapshots(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )

        for snapshot in self.snapshot_pool:
            self.assertTrue(snapshot.id in any(
                s.id) for s in snapshots)

        for snapshot in self.snapshot_pool:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        for snapshot in self.snapshot_pool:
            snapshot.delete(self.apiclient)

        self.snapshot_pool = []

        # Step 3
        # Recurring snapshot
        try:
            create_snapshot_thread_1 = Thread(
                target=CreateSnapshot,
                args=(
                    self,
                    self.root_pool[0],
                    True))
            create_snapshot_thread_2 = Thread(
                target=CreateSnapshot,
                args=(
                    self,
                    self.root_pool[1],
                    True))
            create_snapshot_thread_1.start()
            create_snapshot_thread_2.start()
            create_snapshot_thread_1.join()
            create_snapshot_thread_2.join()

        except:
            self.debug("Error: unable to start thread")

        for rec_snap in self.rec_policy_pool:
            list_snapshots_policy_1 = list_snapshot_policy(
                self.apiclient,
                id=rec_snap.id,
            )

            list_validation = validateList(list_snapshots_policy_1)

            self.assertEqual(
                list_validation[0],
                PASS,
                "snapshot list validation failed due to %s" %
                list_validation[2])

        time.sleep(self.sleep_time_for_hourly_policy)

        snapshots = list_snapshots(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )

        for snapshot in self.snapshot_pool:
            self.assertTrue(snapshot.id in any(
                s.id) for s in snapshots)

        for snapshot in self.snapshot_pool:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        for snapshot in self.snapshot_pool:
            snapshot.delete(self.apiclient)

        self.snapshot_pool = []

        for rec_snap_pol in self.rec_policy_pool:
            rec_snap_pol.delete(self.apiclient)

        self.rec_policy_pool = []

        # Step 4
        thread_pool = []
        for i in range(4):
            try:
                create_snapshot_thread_1 = Thread(
                    target=CreateSnapshot,
                    args=(
                        self,
                        self.root_pool[i],
                        True))
                thread_pool.append(create_snapshot_thread_1)

            except Exception as e:
                raise Exception(
                    "Warning: Exception unable to start thread : %s" %
                    e)

        for thread in thread_pool:
            thread.start()

        for thread in thread_pool:
            thread.join()

        for rec_snap in self.rec_policy_pool:
            list_snapshots_policy_1 = list_snapshot_policy(
                self.apiclient,
                id=rec_snap.id,
            )

            list_validation_1 = validateList(list_snapshots_policy_1)

            self.assertEqual(
                list_validation[0],
                PASS,
                "snapshot list validation failed due to %s" %
                list_validation_1[2])

        time.sleep(self.sleep_time_for_hourly_policy)

        for snapshot in self.snapshot_pool:
            self.assertTrue(snapshot.id in any(
                s.id) for s in snapshots)

        for snapshot in self.snapshot_pool:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        for snapshot in self.snapshot_pool:
            snapshot.delete(self.apiclient)

        self.snapshot_pool = []

        for rec_snap_pol in self.rec_policy_pool:
            rec_snap_pol.delete(self.apiclient)

        self.rec_policy_pool = []

        # Step 5

        try:
            thread_pool = []
            for i in range(4):
                create_snapshot_thread_1 = Thread(
                    target=CreateSnapshot,
                    args=(
                        self,
                        self.root_pool[i],
                        False))
                thread_pool.append(create_snapshot_thread_1)

            destinationHost = Host.listForMigration(
                self.apiclient,
                virtualmachineid=self.vm_pool[3].id)
            migrate_volume_thread_1 = Thread(target=MigrateRootVolume,
                                             args=(self,
                                                   self.vm_pool[3],
                                                   destinationHost[0]))

            thread_pool.append(migrate_volume_thread_1)

            for thread in thread_pool:
                thread.start()

            for thread in thread_pool:
                thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        snapshots = list_snapshots(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )

        for snapshot in self.snapshot_pool:
            self.assertTrue(snapshot.id in any(
                s.id) for s in snapshots)

        for snapshot in self.snapshot_pool:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        for snapshot in self.snapshot_pool:
            snapshot.delete(self.apiclient)

        self.snapshot_pool = []

        # Step 6
        try:
            thread_pool = []
            for i in range(4):
                create_snapshot_thread_1 = Thread(
                    target=CreateSnapshot,
                    args=(
                        self,
                        self.root_pool[i],
                        False))
                thread_pool.append(create_snapshot_thread_1)

            stop_vm_thread_1 = Thread(target=self.StopVM,
                                      args=(self.vm_pool,
                                            ))

            thread_pool.append(stop_vm_thread_1)

            for thread in thread_pool:
                thread.start()

            for thread in thread_pool:
                thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        snapshots = list_snapshots(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )

        for snapshot in self.snapshot_pool:
            self.assertTrue(snapshot.id in any(
                s.id) for s in snapshots)

        for snapshot in self.snapshot_pool:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        for snapshot in self.snapshot_pool:
            snapshot.delete(self.apiclient)

        self.snapshot_pool = []

        # Step 7
        thread_pool = []
        try:
            for i in range(2):
                destinationHost = Host.listForMigration(
                    self.apiclient,
                    virtualmachineid=self.vm_pool[i].id)
                migrate_volume_thread_1 = Thread(target=MigrateRootVolume,
                                                 args=(self,
                                                       self.vm_pool[i],
                                                       destinationHost[0]))

                thread_pool.append(migrate_volume_thread_1)

            for thread in thread_pool:
                thread.start()

            for thread in thread_pool:
                thread.join()

            for vm in self.vm_pool:
                if vm.state != "Stopped":
                    vm.stop(self.apiclient)

            thread_pool = []
            for vm in self.vm_pool[:2]:
                create_snapshot_thread_1 = Thread(
                    target=CreateSnapshot,
                    args=(
                        self,
                        self.root_pool[0],
                        False))
                thread_pool.append(create_snapshot_thread_1)

            for thread in thread_pool:
                thread.start()

            for thread in thread_pool:
                thread.join()

        except:
            self.debug("Error: unable to start thread")

        snapshots = list_snapshots(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )

        for snapshot in self.snapshot_pool:
            self.assertTrue(snapshot.id in any(
                s.id) for s in snapshots)

        for snapshot in self.snapshot_pool:
            self.assertTrue(
                is_snapshot_on_nfs(
                    self.apiclient,
                    self.dbclient,
                    self.config,
                    self.zone.id,
                    snapshot.id))

        for snapshot in self.snapshot_pool:
            snapshot.delete(self.apiclient)

        self.snapshot_pool = []

    @attr(tags=["advanced", "basic"])
    def test_02_concurrent_snapshots_configuration(self):
        """Concurrent Snapshots
            1. Verify that CreateSnapshot command fails when it
                takes more time than job.expire.minute
            2. Verify that snapshot creation fails if CreateSnapshot command
                takes more time than concurrent.snapshots.threshold.perhost
            3. Check the event generation when snapshot creation
                fails if CreateSnapshot takes more time than
                concurrent.snapshots.threshold.perhost

        """

        # Step 1
        if not self.testdata["configurableData"][
                "restartManagementServerThroughTestCase"]:
            self.skipTest(
                "Skip test if restartManagementServerThroughTestCase \
                        is not provided")

        configs = Configurations.list(
            self.apiclient,
            name="job.expire.minutes")
        orig_expire = configs[0].value

        Configurations.update(self.apiclient,
                              name="concurrent.snapshots.threshold.perhost",
                              value="2"
                              )

        Configurations.update(self.apiclient,
                              name="job.expire.minutes",
                              value="1"
                              )

        # Restart management server
        self.RestartServer()
        time.sleep(120)

        try:
            thread_pool = []
            for i in range(4):
                create_snapshot_thread_1 = Thread(
                    target=CreateSnapshot,
                    args=(
                        self,
                        self.root_pool[i],
                        False))
                thread_pool.append(create_snapshot_thread_1)

            for thread in thread_pool:
                thread.start()

            for thread in thread_pool:
                thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        Configurations.update(self.apiclient,
                              name="job.expire.minutes",
                              value=orig_expire
                              )

        # Restart management server
        self.RestartServer()
        time.sleep(120)

        # Step 2
        Configurations.update(self.apiclient,
                              name="concurrent.snapshots.threshold.perhost",
                              value="3"
                              )

        Configurations.update(self.apiclient,
                              name="job.expire.minutes",
                              value="1"
                              )

        # Restart management server
        self.RestartServer()
        time.sleep(120)

        configs = Configurations.list(
            self.apiclient,
            name="job.expire.minutes")

        with self.assertRaises(Exception):
            CreateSnapshot(self, self.root_pool[0], False)

        Configurations.update(self.apiclient,
                              name="job.expire.minutes",
                              value=orig_expire
                              )

        # Restart management server
        self.RestartServer()
        time.sleep(120)

        # Step 3
        configs = Configurations.list(
            self.apiclient,
            name="job.cancel.threshold.minutes")
        orig_cancel = configs[0].value

        Configurations.update(self.apiclient,
                              name="concurrent.snapshots.threshold.perhost",
                              value="3"
                              )

        Configurations.update(self.apiclient,
                              name="job.cancel.threshold.minutes",
                              value="1"
                              )

        self.RestartServer()
        time.sleep(120)

        configs = Configurations.list(
            self.apiclient,
            name="job.expire.minutes")

        with self.assertRaises(Exception):
            CreateSnapshot(self, self.root_pool[0], False)

        Configurations.update(self.apiclient,
                              name="job.cancel.threshold.minutes",
                              value=orig_cancel
                              )

        self.RestartServer()
        time.sleep(120)
        # Step 4
        Configurations.update(self.apiclient,
                              name="concurrent.snapshots.threshold.perhost",
                              value="3"
                              )

        self.RestartServer()
        time.sleep(120)

        Configurations.update(self.apiclient,
                              name="job.cancel.threshold.minutes",
                              value="1"
                              )

        self.RestartServer()
        time.sleep(120)

        with self.assertRaises(Exception):
            CreateSnapshot(self, self.root_pool[0], True)

        return
