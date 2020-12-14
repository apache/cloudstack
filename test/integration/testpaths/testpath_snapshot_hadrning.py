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
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (cleanup_resources,
                              is_snapshot_on_nfs,
                              validateList)
from marvin.lib.base import (Account,
                             Cluster,
                             StoragePool,
                             DiskOffering,
                             ServiceOffering,
                             Host,
                             Configurations,
                             Template,
                             VirtualMachine,
                             Snapshot,
                             SnapshotPolicy,
                             Volume
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_snapshots,
                               list_virtual_machines,
                               createChecksum,
                               compareChecksum
                               )
from marvin.sshClient import SshClient
import time

from marvin.codes import (
    CLUSTERTAG1,
    CLUSTERTAG2,
    PASS,
    BACKED_UP,
    UP)

from threading import Thread


def checkIntegrityOfSnapshot(
        self,
        snapshotsToRestore,
        checksumToCompare,
        disk_type="root"):

    if disk_type == "root":
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

        time.sleep(60)
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
        time.sleep(60)
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
            service=self.testdata,
            original_checksum=checksumToCompare,
            disk_type="datadiskdevice_1",
            virt_machine=temp_vm
        )

        temp_vm.delete(self.apiclient)
        volumeFormSnap.delete(self.apiclient)

    return


def GetDestinationHost(self,
                       hostid,
                       currentHosts,
                       hostList
                       ):
    """ Get destination host in same cluster to migrate
    vm to
    """

    destinationHost = None
    clusterid = None
    for host in self.Hosts:
        if host.id == hostid:
            clusterid = host.clusterid
            break

    for host in hostList:
        if host.clusterid == clusterid:
            destinationHost = host
            break

    return destinationHost


def MigrateRootVolume(self,
                      vm,
                      destinationHost,
                      expectexception=False):
    """ Migrate given volume to type of storage pool mentioned in migrateto:

        Inputs:
            1. volume:           Volume to be migrated
            2. migrate_to:       Scope of desired Storage pool to which volume
                                 is to be migrated
            3. expectexception:  If exception is expected while migration
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


class TestSnapshotsHardning(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSnapshotsHardning, cls).getClsTestClient()
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

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        try:

            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )

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

            cls.service_offering_ha = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                offerha=True
            )

            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
            )
            cls._cleanup.append(cls.disk_offering)

            cls.vm = VirtualMachine.create(
                cls.apiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
                diskofferingid=cls.disk_offering.id,
                mode=cls.zone.networktype
            )
            cls._cleanup.append(cls.vm)

            cls.root_volume = list_volumes(
                cls.userapiclient,
                virtualmachineid=cls.vm.id,
                type='ROOT',
                listall=True
            )
            cls.data_volume = list_volumes(
                cls.userapiclient,
                virtualmachineid=cls.vm.id,
                type='DATA',
                listall=True
            )

            cls.vm_ha = VirtualMachine.create(
                cls.apiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering_ha.id,
                zoneid=cls.zone.id,
                diskofferingid=cls.disk_offering.id,
                mode=cls.zone.networktype
            )
            cls._cleanup.append(cls.vm_ha)

            cls._cleanup.append(cls.account)
            cls.root_volume_ha = list_volumes(
                cls.userapiclient,
                virtualmachineid=cls.vm_ha.id,
                type='ROOT',
                listall=True
            )

            cls.clusterList = Cluster.list(cls.apiclient)
            cls.Hosts = Host.list(cls.apiclient)
            cls.exceptionList = []

            configs = Configurations.list(
                cls.apiclient,
                name="snapshot.delta.max")
            cls.delta_max = configs[0].value

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

    def EnableMaintenance(self, hostid):
        Host.enableMaintenance(self.apiclient, id=hostid)
        return

    def StartVM(self, vm):
        vm.start(self.apiclient)

        return

    def RebootVM(self, vm):
        vm.reboot(self.apiclient)

        return

    def StopVM(self, vm):
        vm.stop(self.apiclient)

        return

    def ClearSnapshots(self, snapshots):
        if snapshots:
            for snap in snapshots:
                snap.delete(self.apiclient)

        return

    def CreateSnapshot(self, root_volume, is_recurring):
        """Create Snapshot"""
        try:
            if is_recurring:
                recurring_snapshot = SnapshotPolicy.create(
                    self.apiclient,
                    root_volume.id,
                    self.testdata["recurring_snapshot"]
                )
                self.rec_policy_pool.append(recurring_snapshot)
            else:
                root_vol_snap = Snapshot.create(
                    self.apiclient,
                    root_volume.id)

                self.snapshot_pool.append(root_vol_snap)
        except Exception as e:
            self.exceptionList = []
            self.exceptionList.append(e)
        return

    def CreateDeltaSnapshot(self, volume):
        for i in range(int(self.delta_max)):
            Snapshot.create(
                self.apiclient,
                volume.id)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_snapshot_hardning_kvm(self):
        """snapshot hardning
            1. Take VM snapshot then migrate the VM to another host
                and again take volume snapshot and check its integrity
            2. Verify that snapshot gets created successfuly while VM
                is getting Migrated to another host
            3. Verify that snapshot should succeed after vm's are HA-ed
                to different host and also check its integrity
            4. Take ROOT volume snapshot and when snapshot is in progress
                bring the host down then once the VM is HA-ed
                to different host take snapshot of root volume and
                Check the integrity of this snapshot
            5. Stop the VM, initiate VM snapshot and while snapshot
                is still in progress start the VM and check
                the integrity of the snapshot
            6. Initiate ROOT volume snapshot and while snapshot is
                in pregress Stop the VM Verify that the VM stops
                successfully and check integrity of snapshot
            7. Initiate ROOT volume snapshot and while snapshot is
                in pregress Reboot the VM
                Verify that the VM reboot successfully and
                check integrity of snapshot
            8. Initiate ROOT volume snapshot and while snapshot is
                in progress create snapshot of the same volume
               and check integrity of both the snapshots
            9. Initiate snapshot of DATA volume and while snapshot is
                in progress detach the volume verify that volume
                gets detached successfully also check integrity of snapshot
            10. Initiate snapshot of a detached volume and while snapshot is
                in progress attach the volume to A VM verify that volume
                gets attached successfully also check integrity of snapshot
        """
        if self.hypervisor.lower() != "kvm":
            self.skipTest("Skip test for Hypervisor other than KVM")

        # Step 1

        root_volume = list_volumes(
            self.userapiclient,
            virtualmachineid=self.vm.id,
            type='ROOT',
            listall=True
        )

        checksum_root = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm,
            disk=root_volume[0],
            disk_type="rootdiskdevice")

        Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=root_volume[0].id,
            listall=True)

        destinationHost = Host.listForMigration(
            self.apiclient,
            virtualmachineid=self.vm.id)
        sameClusHosts = Host.list(self.apiclient, virtualmachineid=self.vm.id)

        current_host = self.vm.hostid
        hostToMigarte = GetDestinationHost(
            self,
            current_host,
            sameClusHosts,
            destinationHost)

        MigrateRootVolume(self,
                          self.vm,
                          hostToMigarte)

        Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        # Step 2
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume[0],
                    False))

            destinationHost = Host.listForMigration(
                self.apiclient,
                virtualmachineid=self.vm.id)

            migrate_rootvolume_thread = Thread(target=MigrateRootVolume,
                                               args=(self,
                                                     self.vm,
                                                     destinationHost[0]))

            create_snapshot_thread.start()
            migrate_rootvolume_thread.start()
            create_snapshot_thread.join()
            migrate_rootvolume_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        # Step 3

        vm_host_id = self.vm_ha.hostid

        checksum_root_ha = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm_ha,
            disk=self.root_volume_ha[0],
            disk_type="rootdiskdevice")

        self.CreateSnapshot(
            self.root_volume_ha[0],
            False)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume_ha[0].id,
            listall=True)

        Host.enableMaintenance(self.apiclient, id=vm_host_id)
        time.sleep(180)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume_ha[0].id,
            listall=True)

        Host.cancelMaintenance(self.apiclient, id=vm_host_id)
        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root_ha)

        # Step 4
        # Scenario to be tested
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume_ha[0],
                    False))

            host_enable_maint_thread = Thread(
                target=Host.enableMaintenance,
                args=(
                    self.apiclient,
                    vm_host_id))

            create_snapshot_thread.start()
            host_enable_maint_thread.start()
            create_snapshot_thread.join()
            host_enable_maint_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        self.CreateSnapshot(self.root_volume_ha[0], False)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume_ha[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root_ha)

        Host.cancelMaintenance(self.apiclient, vm_host_id)

        # Step 5
        self.vm.stop(self.apiclient)
        time.sleep(90)
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume[0],
                    False))

            start_vm_thread = Thread(target=self.StartVM,
                                     args=([self.vm]))

            create_snapshot_thread.start()
            start_vm_thread.start()
            create_snapshot_thread.join()
            start_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            self.vm.name)[0][0]

        self.assertEqual(
            state,
            "Running",
            "Check if vm has started properly")

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        # Step 6
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume[0],
                    False))

            stop_vm_thread = Thread(target=self.StopVM,
                                    args=([self.vm]))

            create_snapshot_thread.start()
            stop_vm_thread.start()
            create_snapshot_thread.join()
            stop_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            self.vm.name)[0][0]

        self.assertEqual(
            state,
            "Stopped",
            "Check if vm has started properly")

        self.vm.start(self.apiclient)
        time.sleep(180)
        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        # Step 7

        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume[0],
                    False))

            reboot_vm_thread = Thread(target=self.RebootVM,
                                      args=([self.vm]))

            create_snapshot_thread.start()
            reboot_vm_thread.start()
            create_snapshot_thread.join()
            reboot_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            self.vm.name)[0][0]

        self.assertEqual(
            state,
            "Running",
            "Check if vm has started properly")

        time.sleep(180)
        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        # Step 8 pending(actual 9)
        # Step 9

        checksum_data = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm,
            disk=self.data_volume[0],
            disk_type="datadiskdevice_1")

        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.data_volume[0],
                    False))

            detach_vm_thread = Thread(
                target=self.vm.detach_volume,
                args=(
                    self.apiclient,
                    self.data_volume[0]))

            create_snapshot_thread.start()
            detach_vm_thread.start()
            create_snapshot_thread.join()
            detach_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        self.vm.reboot(self.apiclient)
        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.data_volume[0].id,
            listall=True)

        data_volume_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm.id,
            type='DATA',
            listall=True
        )

        self.assertEqual(
            data_volume_list,
            None,
            "check if volume is detached"
        )

        checkIntegrityOfSnapshot(
            self,
            snapshots[0],
            checksum_data,
            disk_type="data")

        # Step 10

        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.data_volume[0],
                    False))

            attach_vm_thread = Thread(
                target=self.vm.attach_volume,
                args=(
                    self.apiclient,
                    self.data_volume[0]))

            create_snapshot_thread.start()
            attach_vm_thread.start()
            create_snapshot_thread.join()
            attach_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        self.vm.reboot(self.apiclient)
        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.data_volume[0].id,
            listall=True)

        data_volume_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm.id,
            type='DATA',
            listall=True
        )

        self.assertNotEqual(
            data_volume_list,
            [],
            "check if volume is detached"
        )

        checkIntegrityOfSnapshot(
            self,
            snapshots[0],
            checksum_root,
            disk_type="data")

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_02_snapshot_hardning_xenserver(self):
        """snapshot hardning
            1. Take VM snapshot then migrate the VM to another
                host and again take
                volume snapshot and check its intigrity
            2. Verify that snapshot gets created successfuly
                while VM is getting
                Migrated to another host
            3. Verify that snapshot should succeed after vm's are
                HA-ed to different host
                and also check its integrity
            4. Take ROOT volume snapshot and when snapshot is
                in progress bring the host down
                then once the VM is HA-ed to different host
                take snapshot of root volume
                and Check the integrity of this snapshot
            5. Stop the VM, initiate VM snapshot and
                while snapshot is still in progress
                start the VM and check the integrity of the snapshot
            6. Initiate ROOT volume snapshot and while snapshot is
                in pregress Stop the VM
                Verify that the VM stops successfully and
                check integrity of snapshot
            7. Initiate ROOT volume snapshot and while snapshot is
                in pregress Reboot the VM
                Verify that the VM reboot successfully and
                check integrity of snapshot
            8. Initiate ROOT volume snapshot and while snapshot is
                in pregress create snapshot of the same volume
                and check integrity of both the snapshots
            9. Initiate snapshot of DATA volume and while snapshot
                is in progress detach the volume
                verify that volume gets detached successfully
                also check integrity of snapshot
            10. Initiate snapshot of a detached volume and
                while snapshot is in progress attach the volume
                to A VM verify that volume gets attached
                successfully also check integrity of snapshot
        """
        if self.hypervisor != "xenserver":
            self.skipTest("Skip test for server other than XenServer")

        # Step 1

        root_volume = list_volumes(
            self.userapiclient,
            virtualmachineid=self.vm.id,
            type='ROOT',
            listall=True
        )

        checksum_root = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm,
            disk=root_volume[0],
            disk_type="rootdiskdevice")

        Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=root_volume[0].id,
            listall=True)

        destinationHost = Host.listForMigration(
            self.apiclient,
            virtualmachineid=self.vm.id)
        sameClusHosts = Host.list(self.apiclient, virtualmachineid=self.vm.id)

        current_host = self.vm.hostid
        hostToMigarte = GetDestinationHost(
            self,
            current_host,
            sameClusHosts,
            destinationHost)

        MigrateRootVolume(self,
                          self.vm,
                          hostToMigarte)

        self.CreateDeltaSnapshot(root_volume[0])

        Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        self.CreateDeltaSnapshot(root_volume[0])

        # Step 2
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume[0],
                    False))

            destinationHost = Host.listForMigration(
                self.apiclient,
                virtualmachineid=self.vm.id)
            migrate_rootvolume_thread = Thread(target=MigrateRootVolume,
                                               args=(self,
                                                     self.vm,
                                                     destinationHost[0]))

            create_snapshot_thread.start()
            migrate_rootvolume_thread.start()
            create_snapshot_thread.join()
            migrate_rootvolume_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        self.CreateDeltaSnapshot(root_volume[0])
        # Step 3

        vm_host_id = self.vm_ha.hostid

        checksum_root_ha = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm_ha,
            disk=self.root_volume_ha[0],
            disk_type="rootdiskdevice")

        self.CreateSnapshot(
            self.root_volume_ha[0],
            False)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume_ha[0].id,
            listall=True)

        self.CreateDeltaSnapshot(self.root_volume_ha[0])

        Host.enableMaintenance(self.apiclient, id=vm_host_id)
        time.sleep(180)

        self.CreateSnapshot(self.root_volume[0], False)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume_ha[0].id,
            listall=True)

        Host.cancelMaintenance(self.apiclient, id=vm_host_id)
        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root_ha)

        self.CreateDeltaSnapshot(self.root_volume_ha[0])
        # Step 4
        # Scenario to be tested
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume_ha[0],
                    False))

            host_enable_maint_thread = Thread(
                target=Host.enableMaintenance,
                args=(
                    self.apiclient,
                    vm_host_id))

            create_snapshot_thread.start()
            host_enable_maint_thread.start()
            create_snapshot_thread.join()
            host_enable_maint_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        self.CreateDeltaSnapshot(self.root_volume_ha[0])

        self.CreateSnapshot(self.root_volume_ha[0], False)

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume_ha[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root_ha)

        Host.cancelMaintenance(self.apiclient, vm_host_id)

        # Step 5
        self.vm.stop(self.apiclient)
        time.sleep(90)
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume[0],
                    False))

            start_vm_thread = Thread(target=self.StartVM,
                                     args=([self.vm]))

            create_snapshot_thread.start()
            start_vm_thread.start()
            create_snapshot_thread.join()
            start_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            self.vm.name)[0][0]

        self.assertEqual(
            state,
            "Running",
            "Check if vm has started properly")

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        self.CreateDeltaSnapshot(root_volume[0])
        # Step 6
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume[0],
                    False))

            stop_vm_thread = Thread(target=self.StopVM,
                                    args=([self.vm]))

            create_snapshot_thread.start()
            stop_vm_thread.start()
            create_snapshot_thread.join()
            stop_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            self.vm.name)[0][0]

        self.assertEqual(
            state,
            "Stopped",
            "Check if vm has started properly")

        self.vm.start(self.apiclient)
        time.sleep(180)
        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        self.CreateDeltaSnapshot(root_volume[0])
        # Step 7

        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.root_volume[0],
                    False))

            reboot_vm_thread = Thread(target=self.RebootVM,
                                      args=([self.vm]))

            create_snapshot_thread.start()
            reboot_vm_thread.start()
            create_snapshot_thread.join()
            reboot_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            self.vm.name)[0][0]

        self.assertEqual(
            state,
            "Running",
            "Check if vm has started properly")

        time.sleep(180)
        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.root_volume[0].id,
            listall=True)

        checkIntegrityOfSnapshot(self, snapshots[0], checksum_root)

        self.CreateDeltaSnapshot(root_volume[0])
        # Step 8 pending(actual 9)
        # Step 9

        checksum_data = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm,
            disk=self.data_volume[0],
            disk_type="datadiskdevice_1")

        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.data_volume[0],
                    False))

            detach_vm_thread = Thread(
                target=self.vm.detach_volume,
                args=(
                    self.apiclient,
                    self.data_volume[0]))

            create_snapshot_thread.start()
            detach_vm_thread.start()
            create_snapshot_thread.join()
            detach_vm_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        self.vm.reboot(self.apiclient)
        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.data_volume[0].id,
            listall=True)

        data_volume_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm.id,
            type='DATA',
            listall=True
        )

        self.assertEqual(
            data_volume_list,
            None,
            "check if volume is detached"
        )

        checkIntegrityOfSnapshot(
            self,
            snapshots[0],
            checksum_data,
            disk_type="data")

        self.CreateDeltaSnapshot(self.data_volume[0])
        # Step 10

        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self.data_volume[0],
                    False))

            attach_volume_thread = Thread(
                target=self.vm.attach_volume,
                args=(
                    self.apiclient,
                    self.data_volume[0]))

            create_snapshot_thread.start()
            attach_volume_thread.start()
            create_snapshot_thread.join()
            attach_volume_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        self.vm.reboot(self.apiclient)
        snapshots = list_snapshots(
            self.apiclient,
            volumeid=self.data_volume[0].id,
            listall=True)

        data_volume_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm.id,
            type='DATA',
            listall=True
        )

        self.assertNotEqual(
            data_volume_list,
            [],
            "check if volume is detached"
        )

        checkIntegrityOfSnapshot(
            self,
            snapshots[0],
            checksum_root,
            disk_type="data")

        self.CreateDeltaSnapshot(self.data_volume[0])
        return

    def test_03_snapshot_hardning_configuration(self):
        """snapshot hardning
            1. Verify the snapshot failuar for smaller value of
                backup.snapshot.wait then snapshot success for
                larger value of backup.snapshot.wait
                and check the integrity of the snapshot
            2.

        """

        # Step 1
        if not self.testdata["configurableData"][
                "restartManagementServerThroughTestCase"]:
            self.skipTest(
                "Skip test if restartManagementServerThroughTestCase\
                        is not provided")

        configs = Configurations.list(
            self.apiclient,
            name="backup.snapshot.wait")
        orig_backup = configs[0].value

        Configurations.update(self.apiclient,
                              name="backup.snapshot.wait",
                              value="10"
                              )

        # Restart management server
        self.RestartServer()
        time.sleep(120)

        checksum_root = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm,
            disk=self.root_volume,
            disk_type="rootdiskdevice")

        with self.assertRaises(Exception):
            Snapshot.create(
                self.apiclient,
                self.root_volume[0].id)

        Configurations.update(self.apiclient,
                              name="backup.snapshot.wait",
                              value=orig_backup
                              )

        # Restart management server
        self.RestartServer()
        time.sleep(120)

        configs = Configurations.list(
            self.apiclient,
            name="backup.snapshot.wait")
        orig_backup = configs[0].value

        snapshot_2 = Snapshot.create(
            self.apiclient,
            self.root_volume[0].id)

        time.sleep(360)

        self.assertEqual(
            self.dbclient.execute(
                "select status  from snapshots where id='%s'" %
                snapshot_2.id)[0][0],
            "BackedUp"
        )
        checkIntegrityOfSnapshot(self, snapshot_2, checksum_root)

        return


class TestHardening(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestHardening, cls).getClsTestClient()
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

        configs = Configurations.list(
            cls.apiclient,
            name="snapshot.delta.max")
        cls.delta_max = configs[0].value

        clusterid_tag_mapping = {}
        cwps_no = 0
        cls.unsupportedHypervisor = False
        if cls.hypervisor.lower() not in [
                "vmware",
                "kvm",
                "xenserver",
                "hyper-v"]:
            cls.unsupportedHypervisor = True
            return

        try:
            cls.pools = StoragePool.list(
                cls.apiclient,
                zoneid=cls.zone.id,
                scope="CLUSTER")

            for storagePool in cls.pools:
                if storagePool.state.lower() == UP:
                    if storagePool.clusterid not in clusterid_tag_mapping:
                        cwps_no += 1
                        StoragePool.update(
                            cls.apiclient,
                            id=storagePool.id,
                            tags=['cwps' + repr(cwps_no)])

                        clusterid_tag_mapping[
                            storagePool.clusterid] = [cwps_no]

                    else:
                        cwps_no = clusterid_tag_mapping[
                            storagePool.clusterid][0]

                        StoragePool.update(
                            cls.apiclient,
                            id=storagePool.id,
                            tags=['cwps' + repr(cwps_no)])

                        clusterid_tag_mapping[
                            storagePool.clusterid].append(cwps_no)

            cls.pools = StoragePool.list(
                cls.apiclient,
                zoneid=cls.zone.id,
                scope="CLUSTER")

            # Check clusterid count is 2
            # Check each clusterid has two Storage Pool Tags
            # which indicate two Storage Pools exist.
            assert (len(clusterid_tag_mapping)) >= 2 and\
                (len(tags) for tags in clusterid_tag_mapping.values(
                )) >= 2, "There must be atleast two Clusters and\
                each must have atleast two cluster wide storage pools in\
                Up state in the setup"

        except Exception as e:
            raise unittest.SkipTest(e)

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
            cls.service_offering_cluster1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.service_offering_cluster1)

            cls.service_offering_cluster2 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=CLUSTERTAG2
            )

            cls._cleanup.append(cls.service_offering_cluster1)

            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)

            # Create Disk offering
            cls.disk_offering_cluster1 = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.disk_offering_cluster1)

            # Create VM on CWPS
            cls.vm = VirtualMachine.create(
                cls.apiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering_cluster1.id,
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        if self.unsupportedHypervisor:
            self.skipTest("VM migration is not supported on %s" % self.hypervisor)

    def tearDown(self):
        try:
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def GetStoragePoolTag(self, rootVolume):

        storagePoolTag = None

        # Storage Pool Id to migrate to
        for storagePool in self.pools:
            if storagePool.id == rootVolume.storageid:
                storagePoolTag = storagePool.tags

        return storagePoolTag

    def GetDestinationPool(
            self,
            currentStoragePoolTag,
            poolsToavoid,
            migrateToDiffCluster=False):

        destinationPool = None

        # Storage Pool Id to migrate to
        if migrateToDiffCluster:
            for storagePool in self.pools:
                if storagePool.tags != currentStoragePoolTag:
                    destinationPool = storagePool
                    break
        else:
            for storagePool in self.pools:
                if storagePool.name not in poolsToavoid:
                    if storagePool.tags == currentStoragePoolTag:
                        destinationPool = storagePool
                        break

        return destinationPool

    def CreateSnapshot(self, root_volume, is_recurring):
        """Create Snapshot"""
        try:
            if is_recurring:
                recurring_snapshot = SnapshotPolicy.create(
                    self.apiclient,
                    root_volume.id,
                    self.testdata["recurring_snapshot"]
                )
                self.rec_policy_pool.append(recurring_snapshot)
            else:
                root_vol_snap = Snapshot.create(
                    self.apiclient,
                    root_volume.id)

                self.snapshot_pool.append(root_vol_snap)
        except Exception as e:
            self.exceptionList = []
            self.exceptionList.append(e)
        return

    def CreateDeltaSnapshot(self, volume):
        for i in range(int(self.delta_max)):
            Snapshot.create(
                self.apiclient,
                volume.id)
        return

    def GetUpdatedRootVolume(self):
        """
        Return Updated Root Volume.

        The storage pool of ROOT Volume
        changes after migration.
        """

        # Get ROOT Volume
        root_volumes_list = Volume.list(
            self.apiclient,
            virtualmachineid=self.vm.id,
            type='ROOT',
            listall=True
        )

        status = validateList(root_volumes_list)
        self.assertEqual(status[0], PASS, "Check list of ROOT Volumes.")

        rootVolume = root_volumes_list[0]

        return rootVolume

    def LiveMigrateVolume(self,
                          volume,
                          destinationPool,
                          islive=False,
                          expectexception=False
                          ):
        """ Migrate given volume to type of storage pool mentioned in migrateto:

            Inputs:
                1. volume:           Volume to be migrated
                2. migrate_to:       Scope of desired Storage pool
                                     to which volume is
                                     to be migrated
                3. expectexception:  If exception is expected while migration
        """

        if expectexception:
            with self.assertRaises(Exception):
                Volume.migrate(
                    self.apiclient,
                    volumeid=volume.id,
                    storageid=destinationPool.id,
                    livemigrate=islive
                )
        else:
            Volume.migrate(
                self.apiclient,
                volumeid=volume.id,
                storageid=destinationPool.id,
                livemigrate=islive
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
                str(migrated_volume.state).lower(),
                'ready',
                "Check migrated volume is in Ready state"
            )

            self.assertEqual(
                migrated_volume.storage,
                destinationPool.name,
                "Check volume is on migrated pool"
            )
        return

    @attr(tags=["basic", "advanced"], required_hardware="true")
    def test_06_hardening(self):
        """ Hardening

            1. Attach Volume when snapshot on this volume is
               still in progress to a VM in different cluster.
            2. Volume Snapshot after Vms have migrated to
               a different storage pool in same cluster.
            3. Volume Snapshot after Vms have migrated to
               a different cluster.
            4. Volume Snapshot after Vm has live migrated to
               a different storage with in the same cluster.
            5. Volume Snapshot after Vm has live migrated to
               a different cluster.
            6. Volume migration when snapshot is in progress.
            7. Storage live migration when snapshot is in progress.

        """

        # Get ROOT Volume
        root_volume = self.GetUpdatedRootVolume()

        checksum_root = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm,
            disk=root_volume,
            disk_type="rootdiskdevice")

        data_volume_created = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.vm.attach_volume(
            self.apiclient,
            data_volume_created
        )

        self.vm.reboot(self.apiclient)

        data_volumes_list = Volume.list(
            self.apiclient,
            id=data_volume_created.id
        )

        data_volume = data_volumes_list[0]

        checksum_data = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm,
            disk=data_volume,
            disk_type="datadiskdevice_1")

        # Detach DATA Volume
        self.vm.detach_volume(
            self.apiclient,
            data_volume
        )

        self.vm.reboot(self.apiclient)

        current_storagepool_tag = self.GetStoragePoolTag(root_volume)

        # Create VM on CWPS
        vm_in_cluster2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster2.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        # Step 1

        self.CreateSnapshot(data_volume, False)

        vm_in_cluster2.attach_volume(self.apiclient, data_volume)

        vm_in_cluster2.reboot(self.apiclient)

        snapshots_list = list_snapshots(
            self.apiclient,
            volumeid=data_volume.id,
            listall=True)

        data_volume_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_in_cluster2.id,
            type='DATA',
            listall=True
        )

        self.assertNotEqual(
            data_volume_list,
            [],
            "check if volume is detached"
        )

        self.CreateDeltaSnapshot(data_volume)

        self.CreateSnapshot(data_volume,
                            False)

        snapshots_list = list_snapshots(
            self.apiclient,
            volumeid=data_volume_list[0].id,
            listall=True)

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

        checkIntegrityOfSnapshot(
            self,
            snapshots_list[0],
            checksum_data,
            disk_type="data")

        # Detach DATA Volume
        vm_in_cluster2.detach_volume(
            self.apiclient,
            data_volume_list[0]
        )

        vm_in_cluster2.reboot(self.apiclient)

        # Step 2
        self.CreateSnapshot(self,
                            root_volume,
                            False)

        snapshots_list = Snapshot.list(
            self.userapiclient,
            volumeid=root_volume.id)

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

        self.vm.stop(self.apiclient)

        # Migration
        destination_storage_pool = self.GetDestinationPool(
            self,
            current_storagepool_tag,
            root_volume.storage,
            migrateToDiffCluster=False)

        MigrateRootVolume(self, self.vm, destination_storage_pool)

        self.vm.start(self.apiclient)

        self.CreateSnapshot(self,
                            root_volume,
                            False)

        new_snapshots_list = list_snapshots(
            self.userapiclient,
            volumeid=root_volume.id)

        status = validateList(new_snapshots_list)
        self.assertEqual(status[0], PASS, "Snapshots List Validation Failed")

        # Verify Snapshot state
        self.assertEqual(
            new_snapshots_list[0].state.lower() in [
                BACKED_UP,
            ],
            True,
            "Snapshot state is not as expected. It is %s" %
            new_snapshots_list[0].state
        )

        checkIntegrityOfSnapshot(
            self,
            new_snapshots_list[0],
            checksum_root,
            disk_type="root")

        # Step 3
        self.vm.stop(self.apiclient)

        # Get Updated ROOT Volume
        root_volume = self.GetUpdatedRootVolume(self)
        current_storagepool_tag = self.GetStoragePoolTag(self, root_volume)

        destination_storage_pool = self.GetDestinationPool(
            self,
            current_storagepool_tag,
            [],
            migrateToDiffCluster=True)

        # Migration
        MigrateRootVolume(self, self.vm, destination_storage_pool)

        self.vm.start(self.apiclient)

        self.CreateSnapshot(self,
                            root_volume,
                            False)

        snapshots_list = list_snapshots(
            self.userapiclient,
            volumeid=root_volume.id)

        checkIntegrityOfSnapshot(
            self,
            snapshots_list[0],
            checksum_root,
            disk_type="root")

        # Get Updated ROOT Volume
        root_volume = self.GetUpdatedRootVolume(self)
        current_storagepool_tag = self.GetStoragePoolTag(self, root_volume)

        # Step 4
        # Migration

        destination_storage_pool = self.GetDestinationPool(
            self,
            current_storagepool_tag,
            root_volume.storage,
            migrateToDiffCluster=False)

        # Migrate
        self.LiveMigrateVolume(
            self,
            root_volume,
            destination_storage_pool,
            islive=True)

        self.CreateSnapshot(self,
                            root_volume,
                            False)

        snapshots_list = list_snapshots(
            self.userapiclient,
            volumeid=root_volume.id)

        checkIntegrityOfSnapshot(
            self,
            snapshots_list[0],
            checksum_root,
            disk_type="root")

        # Step 5
        # Live Migration

        # Get Updated ROOT Volume
        root_volume = self.GetUpdatedRootVolume(self)

        current_storagepool_tag = self.GetStoragePoolTag(self, root_volume)

        destination_storage_pool = self.GetDestinationPool(
            self,
            current_storagepool_tag,
            [],
            migrateToDiffCluster=True)

        # Migrate
        self.LiveMigrateVolume(
            self,
            root_volume,
            destination_storage_pool,
            islive=True)

        current_storagepool_tag = self.GetStoragePoolTag(self, data_volume)

        destination_storage_pool = self.GetDestinationPool(
            self,
            current_storagepool_tag,
            [],
            migrateToDiffCluster=True)

        # Migrate
        self.LiveMigrateVolume(
            self,
            data_volume,
            destination_storage_pool,
            islive=True)

        self.CreateSnapshot(self,
                            root_volume,
                            False)

        snapshots_list = list_snapshots(
            self.userapiclient,
            volumeid=root_volume.id)

        checkIntegrityOfSnapshot(
            self,
            snapshots_list[0],
            checksum_root,
            disk_type="root")

        self.vm.stop(self.apiclient)

        # Get Updated ROOT Volume
        root_volume = self.GetUpdatedRootVolume(self)

        current_storagepool_tag = self.GetStoragePoolTag(self, root_volume)

        # Step 6
        try:
            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self,
                    root_volume,
                    False))

            destination_storage_pool = self.GetDestinationPool(
                self,
                current_storagepool_tag,
                root_volume.storage,
                migrateToDiffCluster=False)

            migrate_volume_thread = Thread(target=MigrateRootVolume,
                                           args=(self,
                                                 self.vm,
                                                 destination_storage_pool))

            create_snapshot_thread.start()
            migrate_volume_thread.start()
            create_snapshot_thread.join()
            migrate_volume_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        snapshots_list = list_snapshots(
            self.userapiclient,
            volumeid=root_volume.id)

        self.assertTrue(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                snapshots_list[0].id),
            "Snapshot is not on Secondary Storage.")

        checkIntegrityOfSnapshot(self, snapshots_list[0], checksum_root)

        # Step 7
        self.vm.start(self.apiclient)

        # Get Updated ROOT Volume

        root_volume = self.GetUpdatedRootVolume(self)

        current_storagepool_tag = self.GetStoragePoolTag(self, root_volume)

        # live
        try:

            create_snapshot_thread = Thread(
                target=self.CreateSnapshot,
                args=(
                    self,
                    root_volume,
                    False))

            destination_storage_pool = self.GetDestinationPool(
                self,
                current_storagepool_tag,
                root_volume.storage,
                migrateToDiffCluster=False)

            live_migrate_volume_thread = Thread(target=self.LiveMigrateVolume,
                                                args=(self,
                                                      root_volume,
                                                      destination_storage_pool,
                                                      True))

            create_snapshot_thread.start()
            live_migrate_volume_thread.start()
            create_snapshot_thread.join()
            live_migrate_volume_thread.join()

        except Exception as e:
            raise Exception(
                "Warning: Exception unable to start thread : %s" %
                e)

        snapshots_list = list_snapshots(
            self.userapiclient,
            volumeid=root_volume.id)

        checkIntegrityOfSnapshot(
            self,
            snapshots_list[0],
            checksum_root,
            disk_type="root")

        data_volume_created.delete(self.apiclient)
        return
