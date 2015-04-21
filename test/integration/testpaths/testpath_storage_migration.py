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
""" Test cases for Test Paths Storage Migration
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (cleanup_resources,
                              random_gen,
                              format_volume_to_ext3,
                              validateList,
                              is_server_ssh_ready
                              )
from marvin.lib.base import (Account,
                             ServiceOffering,
                             DiskOffering,
                             Volume,
                             Template,
                             VirtualMachine,
                             StoragePool,
                             Snapshot,
                             VmSnapshot,
                             Configurations,
                             Host,
                             NATRule,
                             PublicIPAddress,
                             StaticNATRule,
                             FireWallRule,
                             Network
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_virtual_machines,
                               list_clusters,
                               list_storage_pools,
                               list_hosts,
                               get_windows_template,
                               list_publicIP,
                               list_nat_rules,
                               list_ssvms
                               )

from marvin.cloudstackAPI import (deleteVolume,
                                  enableStorageMaintenance,
                                  cancelStorageMaintenance
                                  )
import hashlib
from marvin.sshClient import SshClient
from marvin.codes import FAILED, PASS, FAIL
from ddt import ddt, data, unpack
import time
from threading import Thread

def GetDestinationPool(self,
                       poolsToavoid,
                       migrateto
                       ):
    """ Get destination pool which has scope same as migrateto
    and which is not in avoid set
    """

    destinationPool = None

    # Get Storage Pool Id to migrate to
    for storagePool in self.pools:
        if storagePool.scope == migrateto:
            if storagePool.name not in poolsToavoid:
                destinationPool = storagePool
                break

    return destinationPool


def MigrateRootVolume(self,
                      vm,
                      destinationPool,
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
                # virtualmachineid=vm.id,
                storageid=destinationPool.id,
            )
    else:
        VirtualMachine.migrate(
            vm,
            self.apiclient,
            # virtualmachineid=vm.id,
            storageid=destinationPool.id,
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

        root_volumes_cluster_list = Volume.list(
            self.apiclient,
            virtualmachineid=migrated_vm.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        self.assertEqual(
            root_volume_cluster.storage,
            destinationPool.name,
            "Check volume is on migrated pool"
        )
    return


def MigrateDataVolume(self,
                      volume,
                      destinationPool,
                      islive=False,
                      expectexception=False
                      ):
    """ Migrate given volume to type of storage pool mentioned in migrateto:

        Inputs:
            1. volume:           Volume to be migrated
            2. migrate_to:       Scope of desired Storage pool to which volume
                                 is to be migrated
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


def createChecksum(self, virtual_machine, disk, disk_type):
    """ Write data on the disk and return the md5 checksum"""

    random_data_0 = random_gen(size=100)
    # creating checksum(MD5)
    m = hashlib.md5()
    m.update(random_data_0)
    checksum_random_data_0 = m.hexdigest()
    try:
        ssh_client = SshClient(
            virtual_machine.ssh_ip,
            virtual_machine.ssh_port,
            virtual_machine.username,
            virtual_machine.password
        )
    except Exception as e:
        self.fail("SSH failed for VM: %s" %
                  e)

    self.debug("Formatting volume: %s to ext3" % disk.id)
    # Format partition using ext3
    # Note that this is the second data disk partition of virtual machine
    # as it was already containing data disk before attaching the new volume,
    # Hence datadiskdevice_2

    format_volume_to_ext3(
        ssh_client,
        self.testdata["volume_write_path"][
            virtual_machine.hypervisor][disk_type]
    )
    cmds = ["fdisk -l",
            "mkdir -p %s" % self.testdata["data_write_paths"]["mount_dir"],
            "mount -t ext3 %s1 %s" % (
                self.testdata["volume_write_path"][
                    virtual_machine.hypervisor][disk_type],
                self.testdata["data_write_paths"]["mount_dir"]
            ),
            "mkdir -p %s/%s/%s " % (
                self.testdata["data_write_paths"]["mount_dir"],
                self.testdata["data_write_paths"]["sub_dir"],
                self.testdata["data_write_paths"]["sub_lvl_dir1"],
            ),
            "echo %s > %s/%s/%s/%s" % (
                random_data_0,
                self.testdata["data_write_paths"]["mount_dir"],
                self.testdata["data_write_paths"]["sub_dir"],
                self.testdata["data_write_paths"]["sub_lvl_dir1"],
                self.testdata["data_write_paths"]["random_data"]
            ),
            "cat %s/%s/%s/%s" % (
                self.testdata["data_write_paths"]["mount_dir"],
                self.testdata["data_write_paths"]["sub_dir"],
                self.testdata["data_write_paths"]["sub_lvl_dir1"],
                self.testdata["data_write_paths"]["random_data"]
            )
            ]

    for c in cmds:
        self.debug("Command: %s" % c)
        result = ssh_client.execute(c)
        self.debug(result)

    # Unmount the storage
    cmds = [
        "umount %s" % (self.testdata["data_write_paths"]["mount_dir"]),
    ]

    for c in cmds:
        self.debug("Command: %s" % c)
        ssh_client.execute(c)

    return checksum_random_data_0


def compareChecksum(
        self,
        original_checksum,
        disk_type,
        virt_machine=None,
        disk=None,
        new_vm=False):
    """
    Create md5 checksum of the data present on the disk and compare
    it with the given checksum
    """

    if disk_type == "datadiskdevice_1" and new_vm:
        new_virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        new_virtual_machine.start(self.userapiclient)

        self.debug("Attaching volume: %s to VM: %s" % (
            disk.id,
            new_virtual_machine.id
        ))

        new_virtual_machine.attach_volume(
            self.apiclient,
            disk
        )

        # Rebooting is required so that newly attached disks are detected
        self.debug("Rebooting : %s" % new_virtual_machine.id)
        new_virtual_machine.reboot(self.apiclient)

    else:
        # If the disk is root disk then no need to create new VM
        # Just start the original machine on which root disk is
        new_virtual_machine = virt_machine
        if new_virtual_machine.state != "Running":
            new_virtual_machine.start(self.userapiclient)

    try:
        # Login to VM to verify test directories and files

        self.debug(
            "SSH into (Public IP: ) %s " % new_virtual_machine.ssh_ip)
        ssh = SshClient(
            new_virtual_machine.ssh_ip,
            new_virtual_machine.ssh_port,
            new_virtual_machine.username,
            new_virtual_machine.password
        )
    except Exception as e:
        self.fail("SSH access failed for VM: %s, Exception: %s" %
                  (new_virtual_machine.ipaddress, e))

    # Mount datadiskdevice_1 because this is the first data disk of the new
    # virtual machine
    cmds = ["blkid",
            "fdisk -l",
            "mkdir -p %s" % self.testdata["data_write_paths"]["mount_dir"],
            "mount -t ext3 %s1 %s" % (
                self.testdata["volume_write_path"][
                    new_virtual_machine.hypervisor][disk_type],
                self.testdata["data_write_paths"]["mount_dir"]
            ),
            ]

    for c in cmds:
        self.debug("Command: %s" % c)
        result = ssh.execute(c)
        self.debug(result)

    returned_data_0 = ssh.execute(
        "cat %s/%s/%s/%s" % (
            self.testdata["data_write_paths"]["mount_dir"],
            self.testdata["data_write_paths"]["sub_dir"],
            self.testdata["data_write_paths"]["sub_lvl_dir1"],
            self.testdata["data_write_paths"]["random_data"]
        ))

    n = hashlib.md5()
    n.update(returned_data_0[0])
    checksum_returned_data_0 = n.hexdigest()

    self.debug("returned_data_0: %s" % returned_data_0[0])

    # Verify returned data
    self.assertEqual(
        original_checksum,
        checksum_returned_data_0,
        "Cheskum does not match with checksum of original data"
    )

    # Unmount the Sec Storage
    cmds = [
        "umount %s" % (self.testdata["data_write_paths"]["mount_dir"]),
    ]

    for c in cmds:
        self.debug("Command: %s" % c)
        ssh.execute(c)

    if new_vm:
        new_virtual_machine.detach_volume(
            self.apiclient,
            disk
        )

        new_virtual_machine.delete(self.apiclient)

    return


class TestStorageMigration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestStorageMigration, cls).getClsTestClient()
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
                "xenserver",
                "hyper-v"]:
            raise unittest.SkipTest(
                "Storage migration not supported on %s" %
                cls.hypervisor)

        try:
            cls.pools = StoragePool.list(cls.apiclient, zoneid=cls.zone.id)
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
            cls.service_offering_zone1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=ZONETAG1
            )
            cls._cleanup.append(cls.service_offering_zone1)

            cls.service_offering_cluster1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.service_offering_cluster1)

            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenabled:
                cls.testdata["service_offering"]["storagetype"] = 'local'

                cls.service_offering_local1 = ServiceOffering.create(
                    cls.apiclient,
                    cls.testdata["service_offering"]
                )
                cls._cleanup.append(cls.service_offering_local1)

            # Create Disk offering
            cls.disk_offering_zone1 = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
                tags=ZONETAG1
            )
            cls._cleanup.append(cls.disk_offering_zone1)

            cls.disk_offering_cluster1 = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.disk_offering_cluster1)

            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenabled:
                cls.testdata["disk_offering"]["storagetype"] = 'local'

                cls.disk_offering_local1 = DiskOffering.create(
                    cls.apiclient,
                    cls.testdata["disk_offering"]
                )
                cls._cleanup.append(cls.disk_offering_local1)
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
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"])
    def test_01_migrate_root_and_data_disk_nonlive(self):
        """ Test migrate Volume (root and data disk)

        # 1. Deploy a VM on cluster wide primary storage.
        # 2. Migrate root and data volume from cluster-to-cluster,
        #    cluster-to-zone, cluster-to-local.
        # 3. Deploy a VM on zone wide primary storage..
        # 4. Migrate root and data volume from zone-to-zone, zone-to-local.
        # 5. Deploy a VM on local storage.
        # 6. Migrate root and data volume from local-to-local.

        Each Migration has following steps:

            a. Write data to disk, create checksum
            b. Migrate the volume to suitable pool
            c. Attach disk to VM and compare checksum with
               checksum of data on disk, they should match

        In addition to this,
        Create snapshot of root and data disk after migration.
        For root disk, create template from snapshot, deploy Vm and compare checksum
        For data disk, Create volume from snapshot, attach to VM and compare checksum

        """

        # Skipping test case for XenSever because ZWPS scenarios present in the test case
        # Skipping for Vmware because on Vmware zwps and cwps can not exist at the same time
        # so the test case for Vmware is written separately

        if self.hypervisor.lower() in ["xenserver", "vmware"]:
            self.skipTest("Skip test case for %s" % self.hypervisor.lower())

        try:
            self.pools = StoragePool.list(self.apiclient, zoneid=self.zone.id)
            assert len(list(storagePool for storagePool in self.pools
                            if storagePool.scope == "ZONE")) >= 2,\
                "There must be at least two zone wide\
                storage pools available in the setup"
            assert len(list(storagePool for storagePool in self.pools
                            if storagePool.scope == "CLUSTER")) >= 2,\
                "There must be at least two cluster wide\
                storage pools available in the setup"
        except Exception as e:
            self.skipTest(e)

        # Adding tags to Storage Pools
        cluster_no = 1
        zone_no = 1
        self.debug("Storage Pools: %s" % self.pools)
        for storagePool in self.pools:
            if storagePool.scope == "ZONE":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['zwps' + repr(zone_no)])
                zone_no += 1
            elif storagePool.scope == "CLUSTER":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['cwps' + repr(cluster_no)])
                cluster_no += 1

        # Step 1
        # Create VM on CWPS
        vm_cluster = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        # Get ROOT Volume
        root_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        # Create DATA Volume on Cluster Wide Storage
        data_volume_clust = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust
        )

        data_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust.id
        )

        root_vol_snap = Snapshot.create(
            self.apiclient,
            root_volume_cluster.id)

        data_vol_snap = Snapshot.create(
            self.apiclient,
            data_volume_clust.id)

        vm_cluster.detach_volume(
            self.apiclient,
            data_volume_clust)

        # Step 2
        # Migrate ROOT Volume from CWPS to other CWPS, this consists of below 3 steps
        # 1. Create Checksum
        # 2. Migrate Volume
        # 3. Compare checksum with data on volume on new pool
        checksum_random_root_cluster = createChecksum(
            self,
            vm_cluster,
            root_volume_cluster,
            "rootdiskdevice")

        vm_cluster.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self,
            [root_volume_cluster.storage],
            "CLUSTER")
        MigrateRootVolume(self, vm_cluster, destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_root_cluster,
            "rootdiskdevice",
            virt_machine=vm_cluster,
            disk=None,
        )
        self.debug("Done with compare checksum")

        vm_cluster.stop(self.userapiclient)
        # Try to Migrate ROOT Volume from CWPS to ZWPS
        destinationPool = GetDestinationPool(
            self,
            [root_volume_cluster.storage],
            "ZONE")

        MigrateRootVolume(
            self,
            vm_cluster,
            destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_root_cluster,
            "rootdiskdevice",
            virt_machine=vm_cluster,
            disk=None,
        )

        vm_cluster.stop(self.userapiclient)
        # Try to Migrate ROOT Volume from CWPS to Local
        destinationPool = GetDestinationPool(
            self,
            [root_volume_cluster.storage],
            "HOST")

        MigrateRootVolume(
            self,
            vm_cluster,
            destinationPool,
            expectexception=True)

        vm_cluster.start(self.userapiclient)

        checksum_random_data_cluster = createChecksum(
            self,
            vm_cluster,
            data_volumes_cluster_list[0],
            "datadiskdevice_1")

        vm_cluster.detach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        vm_cluster.stop(self.userapiclient)

        # Migrate DATA Volume from CWPS to other CWPS - Create checksum,
        # migrate volume, compare checksum

        destinationPool = GetDestinationPool(
            self,
            [data_volumes_cluster_list[0].storage],
            "CLUSTER")

        MigrateDataVolume(
            self,
            data_volumes_cluster_list[0],
            destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_cluster_list[0],
            new_vm=True)

        # Add more data to disks
        data_volume_clust_2 = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust_2.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust_2
        )

        data_disk_2_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust_2.id
        )

        # Ensure we can add data to newly added disks
        createChecksum(
            self,
            vm_cluster,
            data_disk_2_volumes_cluster_list[0],
            "datadiskdevice_2")

        vm_cluster.detach_volume(
            self.apiclient,
            data_volume_clust_2
        )

        templateFromSnapshot = Template.create_from_snapshot(
            self.apiclient,
            root_vol_snap,
            self.testdata["template_2"])

        vm_from_temp = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=templateFromSnapshot.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        root_volumes_snap_list = Volume.list(
            self.apiclient,
            virtualmachineid=vm_from_temp.id,
            type='ROOT',
            listall=True
        )

        root_volume_snap = root_volumes_snap_list[0]

        createChecksum(
            self,
            vm_from_temp,
            root_volume_snap,
            "rootdiskdevice")

        templateFromSnapshot.delete(self.apiclient)

        volumeFormSnap = Volume.create_from_snapshot(
            self.apiclient,
            data_vol_snap.id,
            self.testdata["volume"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

        vm_from_temp.attach_volume(
            self.userapiclient,
            volumeFormSnap
        )

        data_from_snap = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=volumeFormSnap.id
        )

        vm_from_temp.reboot(self.userapiclient)

        createChecksum(
            self,
            vm_from_temp,
            data_from_snap[0],
            "datadiskdevice_1")

        vm_from_temp.detach_volume(
            self.userapiclient,
            volumeFormSnap
        )

        volumeFormSnap.delete(self.apiclient)

        vm_from_temp.delete(self.apiclient)

        vm_cluster.stop(self.userapiclient)
        # Try to Migrate DATA Volume from CWPS to ZWPS
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_cluster_list[0].storage],
            "ZONE")
        MigrateDataVolume(
            self,
            data_volumes_cluster_list[0],
            destinationPool)

        vm_cluster.start(self.userapiclient)
        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_cluster_list[0],
            new_vm=True)

        vm_cluster.stop(self.userapiclient)
        # Try to Migrate DATA Volume from CWPS to Local Storage
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_cluster_list[0].storage],
            "HOST")
        MigrateDataVolume(
            self,
            data_volumes_cluster_list[0],
            destinationPool)

        # Delete ROOT and DATA Volume from CWPS
        self.debug("Deleting Volume %s" % data_volume_clust.id)

        vm_cluster.detach_volume(self.apiclient, data_volume_clust)

        # Delete volume:
        data_volume_clust.delete(self.apiclient)

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_clust.id,
            ), None, "Volume list should be empty")

        # Destroy and expunge VM
        vm_cluster.delete(self.apiclient)

        self.assertEqual(
            VirtualMachine.list(
                self.apiclient,
                id=vm_cluster.id
            ), None, "VM list should be empty")

        # Step 3
        # Create VM on ZWPS
        vm_zone = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_zone1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        vm_zone.start(self.userapiclient)

        # Get ROOT Volume Id
        root_volumes_zone_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_zone.id,
            type='ROOT',
            listall=True
        )
        root_volume_zone = root_volumes_zone_list[0]

        # Create DATA Volume on ZWPS
        data_volume_zone = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_zone1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_zone.id)

        data_volumes_zone_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_zone.id
        )

        vm_zone.attach_volume(
            self.userapiclient,
            data_volume_zone
        )

        # Step 4
        # Migrate ROOT Volume from ZWPS to other ZWPS
        checksum_random_root_zone = createChecksum(
            self,
            vm_zone,
            data_volumes_zone_list[0],
            "rootdiskdevice")

        vm_zone.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self, [
                root_volume_zone.storage], "ZONE")
        MigrateRootVolume(self, vm_zone, destinationPool)

        vm_zone.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_root_zone,
            "rootdiskdevice",
            virt_machine=vm_zone,
            disk=None,
        )

        vm_zone.stop(self.userapiclient)
        # Try to Migrate ROOT Volume from ZWPS to Local Storage
        destinationPool = GetDestinationPool(
            self, [
                root_volume_zone.storage], "HOST")
        MigrateRootVolume(self, vm_zone, destinationPool, expectexception=True)

        # Try to Migrate ROOT Volume from ZWPS to Cluster wide Storage

        destinationPool = GetDestinationPool(
            self, [
                root_volume_zone.storage], "CLUSTER")
        MigrateRootVolume(
            self,
            vm_zone,
            destinationPool)  # , expectexception=True)

        vm_zone.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_root_zone,
            "rootdiskdevice",
            virt_machine=vm_zone,
            disk=None,
        )

        checksum_random_data_zone = createChecksum(
            self,
            vm_zone,
            data_volumes_zone_list[0],
            "datadiskdevice_1")

        vm_zone.stop(self.userapiclient)

        # Migrate DATA Volume from ZWPS to other ZWPS
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_zone_list[0].storage],
            "ZONE")
        MigrateDataVolume(self, data_volumes_zone_list[0], destinationPool)

        compareChecksum(
            self,
            checksum_random_data_zone,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_zone_list[0],
            new_vm=True)

        vm_zone.stop(self.userapiclient)
        # Try to Migrate DATA Volume from ZWPS to Local Storage
        destinationPool = GetDestinationPool(
            self, [
                data_volume_zone.storage], "HOST")
        MigrateDataVolume(
            self,
            data_volume_zone,
            destinationPool,
            expectexception=True)

        # Try to Migrate DATA Volume from ZWPS to Cluster wide Storage
        destinationPool = GetDestinationPool(
            self, [
                data_volume_zone.storage], "CLUSTER")
        MigrateDataVolume(
            self,
            data_volume_zone,
            destinationPool)

        vm_zone.start(self.userapiclient)
        compareChecksum(
            self,
            checksum_random_data_zone,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_zone_list[0],
            new_vm=True)

        # Delete ROOT and DATA Volume from ZWPS

        self.debug("Deleting Volume %s" % data_volume_zone.id)

        vm_zone.detach_volume(self.apiclient, data_volume_zone)
        # Delete volume
        data_volume_zone.delete(self.apiclient)

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_zone.id,
            ), None, "Volume list should be empty")

        # Destroy and expunge VM
        vm_zone.delete(self.apiclient)

        self.assertEqual(
            VirtualMachine.list(
                self.apiclient,
                id=vm_zone.id
            ), None, "VM list should be empty")

        # Step 5
        localStoragePoolsPresent = True
        try:
            self.assertEqual(
                len(list(storagePool for storagePool in self.pools
                         if storagePool.scope == "HOST")), 2)
        except Exception as e:
            localStoragePoolsPresent = False

        if localStoragePoolsPresent:
            # Create VM on local storage
            vm_local = VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering_local1.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )

            # Get ROOT Volume id
            root_volumes_local_list = list_volumes(
                self.apiclient,
                virtualmachineid=vm_local.id,
                type='ROOT',
                listall=True
            )

            root_volume_local = root_volumes_local_list[0]

            # Create DATA Volume on local storage
            data_volume_local = Volume.create(
                self.apiclient,
                self.testdata["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering_local1.id
            )

            self.debug("Created volume with ID: %s" % data_volume_local.id)

            data_volumes_local_list = Volume.list(
                self.userapiclient,
                listall=self.testdata["listall"],
                id=data_volume_local.id
            )

            vm_local.attach_volume(
                self.userapiclient,
                data_volume_local
            )

            # Step 6
            # Migrate root and data volume from Local to another Local storage
            checksum_random_root_local = createChecksum(
                self,
                vm_local,
                data_volumes_local_list[0],
                "rootdiskdevice")

            vm_local.stop(self.userapiclient)

            destinationPool = GetDestinationPool(
                self, [
                    root_volume_local.storage], "HOST")
            MigrateRootVolume(self, vm_local, destinationPool)

            vm_local.start(self.userapiclient)

            compareChecksum(
                self,
                checksum_random_root_local,
                "rootdiskdevice",
                virt_machine=vm_cluster,
                disk=None,
            )

            checksum_random_data_local = createChecksum(
                self,
                vm_local,
                data_volumes_local_list[0],
                "datadiskdevice_1")

            vm_local.stop(self.userapiclient)
            destinationPool = GetDestinationPool(
                self,
                [data_volumes_local_list[0].storage],
                "HOST")
            MigrateDataVolume(
                self,
                data_volumes_local_list[0],
                destinationPool)

            vm_local.start(self.userapiclient)
            compareChecksum(
                self,
                checksum_random_data_local,
                "datadiskdevice_1",
                virt_machine=None,
                disk=data_volumes_local_list[0],
                new_vm=True)

            # Delete ROOT and DATA Volume from Local Storage

            self.debug("Deleting Volume %s" % data_volume_local.id)

            vm_local.detach_volume(self.apiclient, data_volume_local)
            # Delete volume
            data_volume_local.delete(self.apiclient)

            self.assertEqual(
                Volume.list(
                    self.apiclient,
                    id=data_volume_local.id,
                ), None, "Volumes list should be empty")

            # Destroy and expunge VM
            vm_local.delete(self.apiclient)

            self.assertEqual(
                VirtualMachine.list(
                    self.apiclient,
                    id=vm_local.id
                ), None, "VM list should be empty")
        return

    @attr(tags=["advanced", "basic"])
    def test_02_migration_nonlive_xenserver_supported(self):
        """ Test migrate Volume (root and data disk) for Hypervisor Xenserver

        # 1. Deploy a VM on cluster wide primary storage.
        # 2. Migrate root and data volume from
             cluster-to-cluster,cluster-to-local.
        # 3. Deploy a VM on local storage.
        # 4. Migrate root and data volume from local-to-local.

        Each Migration has following steps:

            a. Write data to disk, create checksum
            b. Migrate the volume to suitable pool
            c. Attach disk to VM and compare checksum with
               checksum of data on disk, they should match

        In addition to this,
        Create snapshot of root and data disk after migration.
        For root disk, create template from snapshot, deploy Vm and compare checksum
        For data disk, Create volume from snapshot, attach to VM and compare checksum
        """

        # Check if Hypervisor is Xenserver
        if self.hypervisor.lower() != "xenserver":
            self.skipTest("This test case is written specifically for xenserver,\
                    it does not include ZWPS scenarios")

        try:
            self.pools = StoragePool.list(self.apiclient, zoneid=self.zone.id)
            assert len(list(storagePool for storagePool in self.pools
                            if storagePool.scope == "CLUSTER")) >= 2,\
                "There must be at least two cluster wide\
                storage pools available in the setup"
        except Exception as e:
            self.skipTest(e)

        # Adding tags to Storage Pools
        cluster_no = 1
        self.debug("Storage Pools: %s" % self.pools)
        for storagePool in self.pools:
            if storagePool.scope == "CLUSTER":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['cwps' + repr(cluster_no)])
                cluster_no += 1
        # Create VM on CWPS
        vm_cluster = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        # Get ROOT Volume Id
        root_volumes_cluster_list = Volume.list(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        data_volume_clust = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust
        )

        data_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust.id
        )

        root_vol_snap = Snapshot.create(
            self.apiclient,
            root_volume_cluster.id)

        data_vol_snap = Snapshot.create(
            self.apiclient,
            data_volume_clust.id)

        vm_cluster.detach_volume(
            self.apiclient,
            data_volume_clust)

        # Migrate ROOT Volume from CWPS to other CWPS
        checksum_random_root_cluster = createChecksum(
            self,
            vm_cluster,
            root_volume_cluster,
            "rootdiskdevice")

        vm_cluster.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self,
            [root_volume_cluster.storage],
            "CLUSTER")
        MigrateRootVolume(self, vm_cluster, destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_root_cluster,
            "rootdiskdevice",
            virt_machine=vm_cluster,
            disk=None,
        )
        self.debug("Done with compare checksum after first checksum")

        vm_cluster.start(self.userapiclient)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust
        )

        vm_cluster.reboot(self.userapiclient)

        checksum_random_data_cluster = createChecksum(
            self,
            vm_cluster,
            data_volumes_cluster_list[0],
            "datadiskdevice_1")

        vm_cluster.stop(self.userapiclient)

        # Migrate DATA Volume from CWPS to other CWPS
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_cluster_list[0].storage],
            "CLUSTER")

        MigrateDataVolume(self, data_volumes_cluster_list[0], destinationPool)

        vm_cluster.start(self.userapiclient)

        vm_cluster.detach_volume(self.apiclient, data_volumes_cluster_list[0])

        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_cluster_list[0],
            new_vm=True)

        # snapshot test case t14 compare checksum for same VM
        vm_cluster.attach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        vm_cluster.reboot(self.apiclient)

        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_1",
            virt_machine=vm_cluster,
            disk=data_volumes_cluster_list[0]
        )

        # Add more data to disks
        data_volume_clust_2 = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust_2.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust_2
        )

        data_disk_2_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust_2.id
        )

        createChecksum(
            self,
            vm_cluster,
            data_disk_2_volumes_cluster_list[0],
            "datadiskdevice_2")

        vm_cluster.detach_volume(
            self.apiclient,
            data_volume_clust_2
        )

        vm_cluster.stop(self.userapiclient)
        # Try to Migrate ROOT Volume from CWPS to Local
        destinationPool = GetDestinationPool(
            self, [
                root_volume_cluster.storage], "HOST")
        MigrateRootVolume(
            self,
            vm_cluster,
            destinationPool,
            expectexception=True)

        # Try to Migrate DATA Volume from CWPS to Local Storage
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_cluster_list[0].storage],
            "HOST")
        MigrateDataVolume(
            self,
            data_volumes_cluster_list[0],
            destinationPool,
            expectexception=True)

        # Restore snapshot to preveous stage
        # Convert root snap to template and boot vm from that n then check data
        # Convert data snap to volume and attach it toa VM and then check data

        templateFromSnapshot = Template.create_from_snapshot(
            self.apiclient,
            root_vol_snap,
            self.testdata["template_2"])

        vm_from_temp = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=templateFromSnapshot.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        root_volumes_snap_list = Volume.list(
            self.apiclient,
            virtualmachineid=vm_from_temp.id,
            type='ROOT',
            listall=True
        )

        root_volume_snap = root_volumes_snap_list[0]

        createChecksum(
            self,
            vm_from_temp,
            root_volume_snap,
            "rootdiskdevice")

        templateFromSnapshot.delete(self.apiclient)

        volumeFormSnap = Volume.create_from_snapshot(
            self.apiclient,
            data_vol_snap.id,
            self.testdata["volume"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

        vm_from_temp.attach_volume(
            self.userapiclient,
            volumeFormSnap
        )

        data_from_snap = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=volumeFormSnap.id
        )

        vm_from_temp.reboot(self.userapiclient)

        createChecksum(
            self,
            vm_from_temp,
            data_from_snap[0],
            "datadiskdevice_1")

        vm_from_temp.detach_volume(
            self.userapiclient,
            volumeFormSnap
        )

        volumeFormSnap.delete(self.apiclient)

        vm_from_temp.delete(self.apiclient)

        # Delete ROOT and DATA Volume from CWPS
        self.debug("Deleting Volume %s" % data_volume_clust.id)

        vm_cluster.detach_volume(self.apiclient, data_volume_clust)

        # Delete volume:
        data_volume_clust.delete(self.apiclient)

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_clust.id,
            ), None, "Volume list should be empty")

        # Destroy and expunge VM
        vm_cluster.delete(self.apiclient)

        self.assertEqual(
            VirtualMachine.list(
                self.apiclient,
                id=vm_cluster.id
            ), None, "VM list should be empty")

        localStoragePoolsAvailable = True
        try:
            self.assertEqual(
                len(list(storagePool for storagePool in self.pools
                         if storagePool.scope == "HOST")), 2)
        except Exception as e:
            localStoragePoolsAvailable = False

        if localStoragePoolsAvailable:
            # Create VM on local storage
            vm_local = VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering_local1.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )

            # Get ROOT Volume id
            root_volumes_local_list = Volume.list(
                self.apiclient,
                virtualmachineid=vm_local.id,
                type='ROOT',
                listall=True
            )

            root_volume_local = root_volumes_local_list[0]

            # Create DATA Volume on local storage
            data_volume_local = Volume.create(
                self.apiclient,
                self.testdata["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering_local1.id
            )

            self.debug("Created volume with ID: %s" % data_volume_local.id)

            data_volumes_local_list = Volume.list(
                self.userapiclient,
                listall=self.testdata["listall"],
                id=data_volume_local.id
            )

            vm_local.attach_volume(
                self.userapiclient,
                data_volume_local
            )

            vm_local.reboot(self.userapiclient)

            createChecksum(
                self,
                vm_local,
                root_volume_local,
                "rootdiskdevice")

            vm_local.stop(self.userapiclient)

            # Migrate root and data volume from Local to another Local storage
            destinationPool = GetDestinationPool(
                self, [
                    root_volume_local.storage], "HOST")
            MigrateRootVolume(
                self,
                vm_local,
                destinationPool,
                expectexception=True)

            vm_local.detach_volume(self.apiclient, data_volume_local)

            destinationPool = GetDestinationPool(
                self,
                [data_volumes_local_list[0].storage],
                "HOST")
            MigrateDataVolume(
                self,
                data_volumes_local_list[0],
                destinationPool,
                expectexception=True)

            # Delete ROOT and DATA Volume from Local Storage

            self.debug("Deleting Volume %s" % data_volume_local.id)

            # Delete volume
            data_volume_local.delete(self.apiclient)

            self.assertEqual(
                Volume.list(
                    self.apiclient,
                    id=data_volume_local.id,
                ), None, "Volumes list should be empty")

            # Destroy and expunge VM
            vm_local.delete(self.apiclient)

            self.assertEqual(
                VirtualMachine.list(
                    self.apiclient,
                    id=vm_local.id
                ), None, "VM list should be empty")

        return

    @attr(tags=["advanced", "basic"])
    def test_03_migrate_root_and_data_disk_nonlive_cwps_vmware(self):
        """ Test migrate Volume (root and data disk)

        # 1. Deploy a VM on cluster wide primary storage.
        # 2. Migrate root and data volume from cluster-to-cluster
        # 3. Deploy a VM on local storage.
        # 4. Migrate root and data volume from local-to-local.

        Each Migration has following steps:

            a. Write data to disk, create checksum
            b. Migrate the volume to suitable pool
            c. Attach disk to VM and compare checksum with
               checksum of data on disk, they should match

        In addition to this,
        Create snapshot of root and data disk after migration.
        For root disk, create template from snapshot, deploy Vm and compare checksum
        For data disk, Create volume from snapshot, attach to VM and compare checksum

        """
        # Test case only written for Vmware hypervisor as it
        # does not run CWPS and ZWPS scenatios together
        if self.hypervisor.lower() is not "vmware":
            self.skipTest("Skip test case for %s" % self.hypervisor.lower())

        try:
            self.pools = StoragePool.list(self.apiclient, zoneid=self.zone.id)
            assert len(list(storagePool for storagePool in self.pools
                            if storagePool.scope == "CLUSTER")) >= 2,\
                "There must be at least two cluster wide\
                storage pools available in the setup"
        except Exception as e:
            self.skipTest(e)

        # Adding tags to Storage Pools
        cluster_no = 1
        self.debug("Storage Pools: %s" % self.pools)
        for storagePool in self.pools:
            if storagePool.scope == "CLUSTER":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['cwps' + repr(cluster_no)])
                cluster_no += 1

        # Step 1
        # Create VM on CWPS
        vm_cluster = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        # Get ROOT Volume Id
        root_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        # Create DATA Volume on Cluster Wide Storage
        data_volume_clust = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust
        )

        data_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust.id
        )

        root_vol_snap = Snapshot.create(
            self.apiclient,
            root_volume_cluster.id)

        data_vol_snap = Snapshot.create(
            self.apiclient,
            data_volume_clust.id)

        vm_cluster.detach_volume(
            self.apiclient,
            data_volume_clust)

        # Step 2
        # Migrate ROOT Volume from CWPS to other CWPS
        checksum_random_root_cluster = createChecksum(
            self,
            vm_cluster,
            root_volume_cluster,
            "rootdiskdevice")

        vm_cluster.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self,
            [root_volume_cluster.storage],
            "CLUSTER")
        MigrateRootVolume(self, vm_cluster, destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_root_cluster,
            "rootdiskdevice",
            virt_machine=vm_cluster,
            disk=None,
        )
        self.debug("Done with compare checksum")

        vm_cluster.start(self.userapiclient)
        checksum_random_data_cluster = createChecksum(
            self,
            vm_cluster,
            data_volumes_cluster_list[0],
            "datadiskdevice_1")

        vm_cluster.detach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        vm_cluster.stop(self.userapiclient)

        # Migrate DATA Volume from CWPS to other CWPS
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_cluster_list[0].storage],
            "CLUSTER")
        MigrateDataVolume(
            self,
            data_volumes_cluster_list[0],
            destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_cluster_list[0],
            new_vm=True)

        # snapshot test case t14 compare checksum for same VM
        vm_cluster.attach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        vm_cluster.reboot(self.apiclient)

        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_1",
            virt_machine=vm_cluster,
            disk=data_volumes_cluster_list[0]
        )

        # Add more data to disks
        data_volume_clust_2 = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust_2.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust_2
        )

        data_disk_2_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust_2.id
        )

        createChecksum(
            self,
            vm_cluster,
            data_disk_2_volumes_cluster_list[0],
            "datadiskdevice_2")

        vm_cluster.detach_volume(
            self.apiclient,
            data_volume_clust_2
        )

        templateFromSnapshot = Template.create_from_snapshot(
            self.apiclient,
            root_vol_snap,
            self.testdata["template_2"])

        vm_from_temp = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=templateFromSnapshot.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        root_volumes_snap_list = Volume.list(
            self.apiclient,
            virtualmachineid=vm_from_temp.id,
            type='ROOT',
            listall=True
        )

        root_volume_snap = root_volumes_snap_list[0]

        createChecksum(
            self,
            vm_from_temp,
            root_volume_snap,
            "rootdiskdevice")

        templateFromSnapshot.delete(self.apiclient)

        volumeFormSnap = Volume.create_from_snapshot(
            self.apiclient,
            data_vol_snap.id,
            self.testdata["volume"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

        vm_from_temp.attach_volume(
            self.userapiclient,
            volumeFormSnap
        )

        data_from_snap = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=volumeFormSnap.id
        )

        vm_from_temp.reboot(self.userapiclient)

        createChecksum(
            self,
            vm_from_temp,
            data_from_snap[0],
            "datadiskdevice_1")

        vm_from_temp.detach_volume(
            self.userapiclient,
            volumeFormSnap
        )

        volumeFormSnap.delete(self.apiclient)

        vm_from_temp.delete(self.apiclient)

        # Delete ROOT and DATA Volume from CWPS
        self.debug("Deleting Volume %s" % data_volume_clust.id)

        vm_cluster.detach_volume(self.apiclient, data_volume_clust)

        # Delete volume:
        data_volume_clust.delete(self.apiclient)

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_clust.id,
            ), None, "Volume list should be empty")

        # Destroy and expunge VM
        vm_cluster.delete(self.apiclient)

        self.assertEqual(
            VirtualMachine.list(
                self.apiclient,
                id=vm_cluster.id
            ), None, "VM list should be empty")

        localStoragePoolsPresent = True
        # Step 5
        try:
            self.assertEqual(
                len(list(storagePool for storagePool in self.pools
                         if storagePool.scope == "HOST")), 2)
        except Exception as e:
            localStoragePoolsPresent = False

        if localStoragePoolsPresent:
            # Create VM on local storage
            vm_local = VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering_local1.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )

            # Get ROOT Volume id
            root_volumes_local_list = list_volumes(
                self.apiclient,
                virtualmachineid=vm_local.id,
                type='ROOT',
                listall=True
            )

            root_volume_local = root_volumes_local_list[0]

            # Create DATA Volume on local storage
            data_volume_local = Volume.create(
                self.apiclient,
                self.testdata["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering_local1.id
            )

            self.debug("Created volume with ID: %s" % data_volume_local.id)

            data_volumes_local_list = Volume.list(
                self.userapiclient,
                listall=self.testdata["listall"],
                id=data_volume_local.id
            )

            vm_local.attach_volume(
                self.userapiclient,
                data_volume_local
            )

            # Step 6
            # Migrate root and data volume from Local to another Local storage
            checksum_random_root_local = createChecksum(
                self,
                vm_local,
                data_volumes_local_list[0],
                "rootdiskdevice")

            vm_local.stop(self.userapiclient)

            destinationPool = GetDestinationPool(
                self, [
                    root_volume_local.storage], "HOST")
            MigrateRootVolume(self, vm_local, destinationPool)

            vm_local.start(self.userapiclient)

            compareChecksum(
                self,
                checksum_random_root_local,
                "rootdiskdevice",
                virt_machine=vm_cluster,
                disk=None,
            )

            checksum_random_data_local = createChecksum(
                self,
                vm_local,
                data_volumes_local_list[0],
                "datadiskdevice_1")

            vm_local.stop(self.userapiclient)
            destinationPool = GetDestinationPool(
                self,
                [data_volumes_local_list[0].storage],
                "HOST")
            MigrateDataVolume(
                self,
                data_volumes_local_list[0],
                destinationPool)

            vm_local.start(self.userapiclient)
            compareChecksum(
                self,
                checksum_random_data_local,
                "datadiskdevice_1",
                virt_machine=None,
                disk=data_volumes_local_list[0],
                new_vm=True)

            # Delete ROOT and DATA Volume from Local Storage

            self.debug("Deleting Volume %s" % data_volume_local.id)

            vm_local.detach_volume(self.apiclient, data_volume_local)
            # Delete volume
            data_volume_local.delete(self.apiclient)

            self.assertEqual(
                Volume.list(
                    self.apiclient,
                    id=data_volume_local.id,
                ), None, "Volumes list should be empty")

            # Destroy and expunge VM
            vm_local.delete(self.apiclient)

            self.assertEqual(
                VirtualMachine.list(
                    self.apiclient,
                    id=vm_local.id
                ), None, "VM list should be empty")
        return

    @attr(tags=["advanced", "basic"])
    def test_04_migrate_root_and_data_disk_nonlive_zwps_vmware(self):
        """ Test migrate Volume (root and data disk)

        # 1. Deploy a VM on zone wide primary storage..
        # 2. Migrate root and data volume from zone-to-zone, zone-to-local.

        Each Migration has following steps:

            a. Write data to disk, create checksum
            b. Migrate the volume to suitable pool
            c. Attach disk to VM and compare checksum with
               checksum of data on disk, they should match

        """
        # Test case only written for Vmware hypervisor as it
        # does not run CWPS and ZWPS scenatios together
        if self.hypervisor.lower() is not "vmware":
            self.skipTest("Skip test case for %s" % self.hypervisor.lower())

        try:
            self.pools = StoragePool.list(self.apiclient, zoneid=self.zone.id)
            assert len(list(storagePool for storagePool in self.pools
                            if storagePool.scope == "ZONE")) >= 2,\
                "There must be at least two zone wide\
                storage pools available in the setup"
        except Exception as e:
            self.skipTest(e)

        # Adding tags to Storage Pools
        zone_no = 1
        self.debug("Storage Pools: %s" % self.pools)
        for storagePool in self.pools:
            if storagePool.scope == "ZONE":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['zwps' + repr(zone_no)])
                zone_no += 1

        # Step 3
        # Create VM on ZWPS
        vm_zone = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_zone1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        vm_zone.start(self.userapiclient)

        # Get ROOT Volume Id
        root_volumes_zone_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_zone.id,
            type='ROOT',
            listall=True
        )
        root_volume_zone = root_volumes_zone_list[0]

        # Create DATA Volume on ZWPS
        data_volume_zone = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_zone1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_zone.id)

        data_volumes_zone_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_zone.id
        )

        vm_zone.attach_volume(
            self.userapiclient,
            data_volume_zone
        )

        # Step 4
        # Migrate ROOT Volume from ZWPS to other ZWPS
        checksum_random_root_zone = createChecksum(
            self,
            vm_zone,
            data_volumes_zone_list[0],
            "rootdiskdevice")

        vm_zone.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self, [
                root_volume_zone.storage], "ZONE")
        MigrateRootVolume(self, vm_zone, destinationPool)

        vm_zone.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_root_zone,
            "rootdiskdevice",
            virt_machine=vm_zone,
            disk=None,
        )
        vm_zone.start(self.userapiclient)

        compareChecksum(
            self,
            checksum_random_root_zone,
            "rootdiskdevice",
            virt_machine=vm_zone,
            disk=None,
        )

        checksum_random_data_zone = createChecksum(
            self,
            vm_zone,
            data_volumes_zone_list[0],
            "datadiskdevice_1")

        vm_zone.stop(self.userapiclient)

        # Migrate DATA Volume from ZWPS to other ZWPS
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_zone_list[0].storage],
            "ZONE")
        MigrateDataVolume(self, data_volumes_zone_list[0], destinationPool)

        compareChecksum(
            self,
            checksum_random_data_zone,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_zone_list[0],
            new_vm=True)

        # Delete ROOT and DATA Volume from ZWPS
        self.debug("Deleting Volume %s" % data_volume_zone.id)

        vm_zone.detach_volume(self.apiclient, data_volume_zone)
        # Delete volume
        data_volume_zone.delete(self.apiclient)

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_zone.id,
            ), None, "Volume list should be empty")

        # Destroy and expunge VM
        vm_zone.delete(self.apiclient)

        self.assertEqual(
            VirtualMachine.list(
                self.apiclient,
                id=vm_zone.id
            ), None, "VM list should be empty")
        return


class NegativeTestStorageMigration(cloudstackTestCase):

    exceptionList = []

    @classmethod
    def setUpClass(cls):
        testClient = super(
            NegativeTestStorageMigration,
            cls).getClsTestClient()
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
                "xenserver",
                "hyper-v"]:
            raise unittest.SkipTest(
                "Storage migration not supported on %s" %
                cls.hypervisor)

        try:
            cls.pools = StoragePool.list(cls.apiclient, zoneid=cls.zone.id)
            assert len(list(storagePool for storagePool in cls.pools
                            if storagePool.scope == "ZONE")) >= 2,\
                "There must be at least two zone wide\
                storage pools available in the setup"
            assert len(list(storagePool for storagePool in cls.pools
                            if storagePool.scope == "CLUSTER")) >= 2,\
                "There must be at least two cluster wide\
                storage pools available in the setup"
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
            cls.service_offering_zone1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=ZONETAG1
            )
            cls._cleanup.append(cls.service_offering_zone1)

            cls.service_offering_cluster1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.service_offering_cluster1)

            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenabled:
                cls.testdata["service_offering"]["storagetype"] = 'local'

                cls.service_offering_local1 = ServiceOffering.create(
                    cls.apiclient,
                    cls.testdata["service_offering"]
                )
                cls._cleanup.append(cls.service_offering_local1)

            # Create Disk offering
            cls.disk_offering_zone1 = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
                tags=ZONETAG1
            )
            cls._cleanup.append(cls.disk_offering_zone1)

            cls.disk_offering_cluster1 = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.disk_offering_cluster1)

            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenabled:
                cls.testdata["disk_offering"]["storagetype"] = 'local'

                cls.disk_offering_local1 = DiskOffering.create(
                    cls.apiclient,
                    cls.testdata["disk_offering"]
                )
                cls._cleanup.append(cls.disk_offering_local1)
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
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createMigrationJob(self, volume):
        try:
            destinationPool = None
            for storagePool in self.pools:
                if storagePool.scope == "CLUSTER":
                    if storagePool.name != volume.storage:
                        destinationPool = storagePool
                        break

            with self.assertRaises(Exception):
                Volume.migrate(
                    self.apiclient,
                    volumeid=volume.id,
                    storageid=destinationPool.id,
                    livemigrate='false'
                )
        except Exception as e:
            self.exceptionList.append(e)

    def createTemplateJob(self, virtual_machine, root_volume):
        try:
            services = {"displaytext": "templ",
                        "name": "vol_template",
                        "ostypeid": virtual_machine.ostypeid}
            with self.assertRaises(Exception):
                Template.create(self.apiclient,
                                services,
                                volumeid=root_volume.id
                                )

        except Exception as e:
            self.exceptionList.append(e)

    def createDestroyVmJob(self, virtual_machine):
        try:
            with self.assertRaises(Exception):
                virtual_machine.delete(self.apiclient)

        except Exception as e:
            self.exceptionList.append(e)

    @attr(tags=["advanced", "basic"])
    def test_01_migrate_data_disk_negative_test(self):
        """ Negative test cases

        # 1. Deploy a VM on cluster wide primary storage.
        # 2. Add some data to disks and create checksum
        # 3. Migrate root and data volume from cluster-to-cluster wide storage pool
        # 4. While migration(ROOT disk) is in progress try following scenarios, they should fail:
             I. Take snapshot of the disk
             II. Create Template from the volume
             III. Destroy the instance
        # 5. Compare checksum after migration

        """
        # Adding tags to Storage Pools
        cluster_no = 1
        self.debug("Storage Pools: %s" % self.pools)
        for storagePool in self.pools:
            if storagePool.scope == "CLUSTER":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['cwps' + repr(cluster_no)])
                cluster_no += 1

        # Step 1
        # Create VM on CWPS
        vm_cluster = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            diskofferingid=self.disk_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        # Get ROOT Volume Id
        disk_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='DATADISK',
            listall=True
        )

        data_disk = disk_volumes_cluster_list[0]

        root_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        # Step 2
        # Calculate checksum of ROOT and DATA Disks

        checksum_root_disk = self.createChecksum(
            self,
            vm_cluster,
            root_volume_cluster,
            "rootdiskdevice")

        checksum_data_disk = self.createChecksum(
            self,
            vm_cluster,
            data_disk,
            "datadiskdevice_1")

        volumes = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_disk.id
        )

        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check list response returns a valid list"
        )

        volume = volumes[0]

        vm_cluster.detach_volume(
            self.apiclient,
            volume
        )

        vm_cluster.stop(self.userapiclient)

        try:
            destinationPool = GetDestinationPool(
                self,
                [root_volume_cluster.storage],
                "CLUSTER")
            thread_1 = Thread(
                target=MigrateRootVolume,
                args=(
                    self,
                    vm_cluster,
                    destinationPool,
                ))
            thread_2 = Thread(
                target=self.createTemplateJob,
                args=(
                    vm_cluster,
                    root_volume_cluster,
                ))
            thread_1.start()
            thread_2.start()
            thread_1.join()
            thread_2.join()
        except:
            self.debug("Error: unable to start thread")

        try:
            destinationPool = GetDestinationPool(
                self,
                [root_volume_cluster.storage],
                "CLUSTER")
            thread_3 = Thread(
                target=MigrateRootVolume,
                args=(
                    self,
                    vm_cluster,
                    destinationPool,
                ))
            thread_4 = Thread(
                target=self.createMigrationJob,
                args=(
                    root_volume_cluster,
                ))
            thread_3.start()
            timeout = 60
            while timeout >= 0:
                if volume.state == "Migrating":
                    break
                timeout -= 5
                time.sleep(5)
            thread_4.start()
            thread_3.join()
            thread_4.join()
        except:
            self.debug("Error: unable to start thread")

        vm_cluster.start(self.userapiclient)

        vm_cluster.attach_volume(
            self.apiclient,
            volume
        )

        vm_cluster.reboot(self.userapiclient)

        disk_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='DATADISK',
            listall=True
        )

        data_disk = disk_volumes_cluster_list[0]

        root_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        self.debug("Done with create checksum")

        compareChecksum(
            self,
            checksum_root_disk,
            "rootdiskdevice",
            virt_machine=vm_cluster
        )

        compareChecksum(
            self,
            checksum_data_disk,
            "datadiskdevice_1",
            virt_machine=vm_cluster
        )

        vm_cluster.detach_volume(
            self.userapiclient,
            volume
        )

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = volume.id
        self.apiclient.deleteVolume(cmd)

        vm_cluster.stop(self.userapiclient)

        try:
            destinationPool = GetDestinationPool(
                self,
                [root_volume_cluster.storage],
                "CLUSTER")
            thread_5 = Thread(
                target=MigrateRootVolume,
                args=(
                    self,
                    vm_cluster,
                    destinationPool,
                ))
            thread_6 = Thread(
                target=self.createDestroyVmJob,
                args=(
                    vm_cluster,
                ))
            thread_5.start()
            thread_6.start()
            thread_5.join()
            thread_6.join()
        except:
            self.debug("Error: unable to start thread")

        # Raise exception if there was any exception raised from the threads
        if self.exceptionList:
            for i in self.exceptionList:
                raise(i)

        vm_cluster.delete(self.apiclient)
        return


class TestLiveStorageMigration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestLiveStorageMigration, cls).getClsTestClient()
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

        if cls.hypervisor.lower() in ["kvm", "lxc"]:
            raise unittest.SkipTest(
                "Live Storage migration not supported on %s" %
                cls.hypervisor)

        try:
            cls.pools = StoragePool.list(cls.apiclient, zoneid=cls.zone.id)
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
            cls.service_offering_zone1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=ZONETAG1
            )
            cls._cleanup.append(cls.service_offering_zone1)

            cls.service_offering_cluster1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.service_offering_cluster1)

            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenabled:
                cls.testdata["service_offering"]["storagetype"] = 'local'

                cls.service_offering_local1 = ServiceOffering.create(
                    cls.apiclient,
                    cls.testdata["service_offering"]
                )
                cls._cleanup.append(cls.service_offering_local1)

            # Create Disk offering
            cls.disk_offering_zone1 = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
                tags=ZONETAG1
            )
            cls._cleanup.append(cls.disk_offering_zone1)

            cls.disk_offering_cluster1 = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.disk_offering_cluster1)

            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenabled:
                cls.testdata["disk_offering"]["storagetype"] = 'local'

                cls.disk_offering_local1 = DiskOffering.create(
                    cls.apiclient,
                    cls.testdata["disk_offering"]
                )
                cls._cleanup.append(cls.disk_offering_local1)
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
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"])
    def test_01_migrate_live(self):
        """ Test migrate Volume (root and data disk)

        # 1. Deploy a VM on cluster wide primary storage.
        # 2. Migrate root and data volume to two different storage pools\
             in same cluster.

        """

        try:
            self.pools = StoragePool.list(self.apiclient, zoneid=self.zone.id)
            assert len(list(storagePool for storagePool in self.pools
                            if storagePool.scope == "CLUSTER")) >= 3,\
                "There must be at least three cluster wide\
                storage pools available in the setup"
        except Exception as e:
            self.skipTest(e)

        storagePools_to_avoid = []

        # Adding tags to Storage Pools
        cluster_no = 1

        for storagePool in self.pools:
            if storagePool.scope == "CLUSTER":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['cwps' + repr(cluster_no)])
            cluster_no += 1

        # Step 1
        # Create VM on CWPS
        vm_cluster = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        # Get ROOT Volume
        root_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        # Create DATA Volume on Cluster Wide Storage
        data_volume_clust_1 = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust_1.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust_1
        )

        data_disk_1_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust_1.id
        )

        data_volume_1_cluster = data_disk_1_volumes_cluster_list[0]

        # Step 2
        # Migrate ROOT Volume from CWPS to other CWPS
        checksum_random_root_cluster = createChecksum(
            self,
            vm_cluster,
            root_volume_cluster,
            "rootdiskdevice")

        # Get Destnation Pool
        # Avoid storage Pool on which ROOT disk exists
        storagePools_to_avoid = [root_volume_cluster.storage]

        destinationPool_for_root_disk = GetDestinationPool(
            self,
            storagePools_to_avoid,
            "CLUSTER")

        # Migrate
        MigrateDataVolume(
            self,
            root_volume_cluster,
            destinationPool_for_root_disk,
            islive=True)

        compareChecksum(
            self,
            checksum_random_root_cluster,
            "rootdiskdevice",
            virt_machine=vm_cluster,
            disk=None,
        )

        # Migrate DATA Volume from CWPS to other CWPS
        checksum_random_data_cluster = createChecksum(
            self,
            vm_cluster,
            data_volume_1_cluster,
            "datadiskdevice_1")

        # Get Destnation Pool
        # Avoid storage Pool allocated for ROOT disk, and Pool on which DATA
        # disk1 exists
        storagePools_to_avoid = [
            data_volume_1_cluster.storage,
            destinationPool_for_root_disk.name]

        destinationPool_for_data_disk1 = GetDestinationPool(
            self,
            storagePools_to_avoid,
            "CLUSTER")

        # Migrate
        MigrateDataVolume(
            self,
            data_volume_1_cluster,
            destinationPool_for_data_disk1,
            islive=True)

        # Check VM State is Running
        self.assertEqual(
            vm_cluster.state,
            "Running",
            "Check VM State is running or not.")

        vm_cluster.detach_volume(
            self.userapiclient,
            data_volume_clust_1
        )

        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volume_1_cluster,
            new_vm=True)

        # Destroy and expunge VM and data disk
        vm_cluster.delete(self.apiclient)

        self.assertEqual(
            VirtualMachine.list(
                self.apiclient,
                id=vm_cluster.id
            ), None, "VM list should be empty")

        # Delete volume:
        data_volume_clust_1.delete(self.apiclient)

        # Verify associated disks are deleted
        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_clust_1.id,
            ), None, "Volume list should be empty")

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=root_volume_cluster.id,
            ), None, "Volume list should be empty")

        return

    @unittest.skip(
        "Requires setup with 2 pods - Each pod having 2 clusters. \
            Yet to be tested")
    @attr(tags=["advanced", "basic"])
    def test_02_migration_live_different_pods(self):
        """ Test migrate Volume (root and data disk)

        # 1. Deploy a VM on cluster wide primary storage.
        # 2. Migrate root and data volume to two different storage pools\
             in same cluster.

        """

        try:
            self.pools = StoragePool.list(self.apiclient, zoneid=self.zone.id)
            assert len(list(storagePool for storagePool in self.pools
                            if storagePool.scope == "ZONE")) >= 2,\
                "There must be at least two zone wide\
                 storage pools available in the setup"

            assert len(list(storagePool for storagePool in self.pools
                            if storagePool.scope == "CLUSTER")) >= 3,\
                "There must be at least two cluster wide\
                storage pools available in the setup"
        except Exception as e:
            self.skipTest(e)

        storagePools_to_avoid = []

        # Adding tags to Storage Pools
        cluster_no = 1
        zone_no = 1
        self.debug("Storage Pools: %s" % self.pools)
        for storagePool in self.pools:
            if storagePool.scope == "ZONE":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['zwps' + repr(zone_no)])
                zone_no += 1
            elif storagePool.scope == "CLUSTER":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=['cwps' + repr(cluster_no)])
                cluster_no += 1

        # Step 1
        # Create VM on CWPS
        vm_cluster = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cluster1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        # Get ROOT Volume
        root_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_cluster.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        # Create DATA Volume on Cluster Wide Storage
        data_volume_clust_1 = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust_1.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust_1
        )

        data_disk_1_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust_1.id
        )

        data_volume_1_cluster = data_disk_1_volumes_cluster_list[0]

        # Step 2
        # Migrate ROOT Volume from CWPS to other CWPS
        checksum_random_root_cluster = createChecksum(
            self,
            vm_cluster,
            root_volume_cluster,
            "rootdiskdevice")

        # Get Destnation Pool
        # Avoid storage Pool on which ROOT disk exists
        storagePools_to_avoid = [root_volume_cluster.storage]

        destinationPool_for_root_disk = GetDestinationPool(
            self,
            storagePools_to_avoid,
            "CLUSTER")

        # Migrate
        MigrateDataVolume(
            self,
            root_volume_cluster,
            destinationPool_for_root_disk,
            islive=True)

        compareChecksum(
            self,
            checksum_random_root_cluster,
            "rootdiskdevice",
            virt_machine=vm_cluster,
            disk=None,
        )

        # Migrate DATA Volume from CWPS to other CWPS
        checksum_random_data_cluster = createChecksum(
            self,
            vm_cluster,
            data_volume_1_cluster,
            "datadiskdevice_1")

        # Get Destnation Pool
        # Avoid storage Pool allocated for ROOT disk, and Pool on which DATA
        # disk1 exists

        storagePools_to_avoid = [
            data_volume_1_cluster.storage,
            destinationPool_for_root_disk.name]

        destinationPool_for_data_disk1 = GetDestinationPool(
            self,
            storagePools_to_avoid,
            "CLUSTER")

        # Migrate
        MigrateDataVolume(
            self,
            data_volume_1_cluster,
            destinationPool_for_data_disk1,
            islive=True)

        # Check VM State is Running
        self.assertEqual(
            vm_cluster.state,
            "Running",
            "Check VM State is running or not.")

        vm_cluster.detach_volume(
            self.userapiclient,
            data_volume_clust_1
        )

        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volume_1_cluster,
            new_vm=True)

        # Add disk 2

        data_volume_clust_2 = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_cluster1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_clust_2.id)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust_2
        )

        data_disk_2_volumes_cluster_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_clust_2.id
        )

        data_volume_2_cluster = data_disk_2_volumes_cluster_list[0]

        # Add data to second data disk

        checksum_random_data_cluster = createChecksum(
            self,
            vm_cluster,
            data_volume_2_cluster,
            "datadiskdevice_2")

        # TO-DO Migration

        compareChecksum(
            self,
            checksum_random_data_cluster,
            "datadiskdevice_2",
            virt_machine=None,
            disk=data_volume_2_cluster,
            new_vm=True)

        # TO-DO: Create Snapshot, Migrate and Restore Snapshot
        # But Restore snapshot to previous stage
        # is currently not working, need to investigate

        # Step 3
        # Create VM on ZWPS
        vm_zone = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_zone1.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        vm_zone.start(self.userapiclient)

        # Get ROOT Volume Id
        root_volumes_zone_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm_zone.id,
            type='ROOT',
            listall=True
        )
        root_volume_zone = root_volumes_zone_list[0]

        # Create DATA Volume on ZWPS
        data_volume_zone = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_zone1.id
        )

        self.debug("Created volume with ID: %s" % data_volume_zone.id)

        data_volumes_zone_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_zone.id
        )

        vm_zone.attach_volume(
            self.userapiclient,
            data_volumes_zone_list[0]
        )

        # Step 4
        # Migrate ROOT Volume from ZWPS to other ZWPS
        checksum_random_root_zone = createChecksum(
            self,
            vm_zone,
            root_volume_zone,
            "rootdiskdevice")

        destinationPool = GetDestinationPool(self, root_volume_zone, "ZONE")
        MigrateRootVolume(self, vm_zone, destinationPool)

        compareChecksum(
            self,
            checksum_random_root_zone,
            "rootdiskdevice",
            virt_machine=vm_zone,
            disk=None,
        )

        # Try to Migrate ROOT Volume from ZWPS to Cluster wide Storage

        destinationPool = GetDestinationPool(self, root_volume_zone, "CLUSTER")
        MigrateRootVolume(
            self,
            vm_zone,
            destinationPool,
            expectexception=True)

        compareChecksum(
            self,
            checksum_random_root_zone,
            "rootdiskdevice",
            virt_machine=vm_cluster,
            disk=None,
        )

        # DATA Disk

        checksum_random_data_zone = createChecksum(
            self,
            vm_zone,
            data_volumes_zone_list[0],
            "datadiskdevice_1")

        # Migrate DATA Volume from ZWPS to other ZWPS
        destinationPool = GetDestinationPool(
            self,
            data_volumes_zone_list[0],
            "ZONE")
        MigrateDataVolume(self, data_volumes_zone_list[0], destinationPool)

        compareChecksum(
            self,
            checksum_random_data_zone,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_zone_list[0],
            new_vm=True)

        # Try to Migrate DATA Volume from ZWPS to Cluster wide Storage
        destinationPool = GetDestinationPool(self, data_volume_zone, "CLUSTER")
        MigrateDataVolume(
            self,
            data_volume_zone,
            destinationPool,
            expectexception=True)

        compareChecksum(
            self,
            checksum_random_data_zone,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_zone_list[0],
            new_vm=True)

        # Try to Migrate DATA Volume from ZWPS to Cluster wide Storage
        destinationPool = GetDestinationPool(self, data_volume_zone, "CLUSTER")
        MigrateDataVolume(
            self,
            data_volume_zone,
            destinationPool,
            expectexception=True)

        compareChecksum(
            self,
            checksum_random_data_zone,
            "datadiskdevice_1",
            virt_machine=None,
            disk=data_volumes_zone_list[0],
            new_vm=True)

        # Destroy and expunge VM and data disk
        vm_zone.delete(self.apiclient)

        self.assertEqual(
            VirtualMachine.list(
                self.apiclient,
                id=vm_cluster.id
            ), None, "VM list should be empty")

        # Delete volume:
        data_volume_zone.delete(self.apiclient)

        # Verify associated disks are deleted
        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_zone.id,
            ), None, "Volume list should be empty")

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=root_volume_zone.id,
            ), None, "Volume list should be empty")

        # TO-DO: Local Storage

        # Destroy and expunge VM and data disk
        vm_cluster.delete(self.apiclient)

        self.assertEqual(
            VirtualMachine.list(
                self.apiclient,
                id=vm_cluster.id
            ), None, "VM list should be empty")

        # Delete volume:
        data_volume_clust_1.delete(self.apiclient)
        data_volume_clust_2.delete(self.apiclient)

        # Verify associated disks are deleted
        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_clust_1.id,
            ), None, "Volume list should be empty")

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=data_volume_clust_2.id,
            ), None, "Volume list should be empty")

        self.assertEqual(
            Volume.list(
                self.apiclient,
                id=root_volume_cluster.id,
            ), None, "Volume list should be empty")

        return

def VmSnapshotToCheckDataIntegrity(self,vm):
    """
    This method takes VMSnapshot of the VM post migration to check data integrity.
    VM snapshot is not possible if VM's volumes have snapshots.
    So, first we will check if there are any volume snapshots after migration 
    and delete them if there are any. Once VM snapshot is successful,
    Delete the VM snapshot 
    """
    volumes = list_volumes(self.apiclient, virtualmachineid = vm.id, listall=True)
    for vol in volumes:
        snapshot = Snapshot.list(self.apiclient, volumeid = vol.id, listall=True)
        if(snapshot):
            for snap in snapshot:
                try:
                    Snapshot.deletesnap(self.apiclient, snapid = snap.id)
                except Exception as e:
                    raise Exception("Warning: Exception during Volume snapshot deletion : %s" % e)
    #Take VM snapshot to check data integrity
    try :
        vm_snapshot = VmSnapshot.create(self.apiclient, vmid = vm.id)
    except Exception as e:
        raise Exception("Warning: Exception during VM snapshot creation : %s" % e)

    #Delete the snapshot
    try :
        VmSnapshot.deleteVMSnapshot(self.apiclient, vmsnapshotid = vm_snapshot.id)
    except Exception as e:
        raise Exception("Warning: Exception during VM snapshot deletion : %s" % e)

    return

def MigrateVmWithVolume(self,vm,destinationHost,volumes,pools):
    """
        This method is used to migrate a vm and its volumes using migrate virtual machine with volume API
        INPUTS:
               1. vm -> virtual machine object
               2. destinationHost -> the host to which VM will be migrated
               3. volumes -> list of volumes which are to be migrated
               4. pools -> list of destination pools
    """
    vol_pool_map = {}
    for vol,pool in zip(volumes,pools):
        vol_pool_map.update({vol.id:pool.id})

    vm.migrate_vm_with_volume(
                                  self.apiclient,
                                  hostid=destinationHost.id,
                                  migrateto = vol_pool_map
                                  )
    vm.getState(
                    self.apiclient,
                    "Running"
                )
    #check for the VM's host and volume's storage post migration
    migrated_vm_response = list_virtual_machines(self.apiclient, id=vm.id)
    self.assertEqual(
                         isinstance(migrated_vm_response, list),
                         True,
                         "Check list virtual machines response for valid list"
                    )
    self.assertEqual(
                         migrated_vm_response[0].hostid,
                         destinationHost.id,
                         "VM did not migrate to a specified host"
                    )

    for vol,pool in zip(volumes,pools):
        migrated_volume_response = list_volumes(self.apiclient, virtualmachineid = migrated_vm_response[0].id, name=vol.name, listall = True)
        self.assertEqual(
                         isinstance(migrated_volume_response, list),
                         True,
                         "Check list virtual machines response for valid list"
                        )
        self.assertEqual(
                         migrated_volume_response[0].storageid,
                         pool.id,
                         "Volume did not migrate to a specified pool"
                        )

        self.assertEqual(
                         str(migrated_volume_response[0].state).lower(),
                         'ready',
                         "Check migrated volume is in Ready state"
                        )

    return migrated_vm_response[0]

def MigrateVm(self, vm, destinationHost):
    """
    This method is to migrate a VM using migrate virtual machine API
    """

    vm.migrate(
                    self.apiclient,
                    hostid=destinationHost.id,
                 )
    vm.getState(
                    self.apiclient,
                    "Running"
                    )
    #check for the VM's host and volume's storage post migration
    migrated_vm_response = list_virtual_machines(self.apiclient, id=vm.id)
    self.assertEqual(
                         isinstance(migrated_vm_response, list),
                         True,
                         "Check list virtual machines response for valid list"
                        )
    self.assertEqual(
                         migrated_vm_response[0].hostid,
                         destinationHost.id,
                         "VM did not migrate to a specified host"
                        )
    return migrated_vm_response[0]

def get_destination_pools_hosts(self, vm,storage_scope, storage_type):
    """
    Get destination Pools for all volumes and destination Host for the VM
    This method is use in case we use the API migrate volume with storage
    """

    destinationPools = []
    vol_list = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
    # For each volume get destination pool
    for vol in vol_list:
        pool = GetDestinationStoragePool(self, vol.storage, storage_scope, storage_type)
        destinationPools.append(pool)
        #Get destination host
    destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
    return destinationHost, destinationPools, vol_list

def check_files(self, vm, destinationHost):
    """
    Check for VMX and VMDK files
            INPUTS :
                      1. vm -> The Virtual Machine object
                      2. destinationHost -> The host to which we want to migrate the VM
    """
    # list volumes and their pools
    # Here we list all the volumes of the VM , then login to the destination host
    # and check for vmx and vmdk files in the storage

    vm_volumes = list_volumes(self.apiclient, virtualmachineid = vm.id, listall=True)
    for vol in vm_volumes:
        spool  =  list_storage_pools(self.apiclient, id=vol.storageid)
        split_path = spool[0].path.split("/")
        pool_path = split_path[2]
        if spool[0].type == "NetworkFilesystem":
            pool_path = spool[0].id.replace("-","")
        sshclient = SshClient(
                              host = destinationHost.ipaddress, 
                              #port = self.testdata['configurableData']['host']["publicport"],
                              port=22,
                              #user = self.testdata['configurableData']['host']["username"],
                              user = "root",
                              #passwd = self.testdata['configurableData']['host']["password"],
                              passwd="freebsd@123"
                              )
        pool_data_vmdk = sshclient.execute("ls /vmfs/volumes/" + pool_path + "/" + vm.instancename +  "| grep vmdk")
        pool_data_vmx = sshclient.execute("ls /vmfs/volumes/" + pool_path + "/" + vm.instancename +  "| grep vmx")
        self.debug("------------------------volume's actual path is: %s" %vol.path)
        vol_path_db = self.dbclient.execute("select path from volumes where uuid='%s';" % vol.id)
        self.debug("-----------------------volume's  path in DB is: %s" %vol_path_db)
        vol_name_db = self.dbclient.execute("select name from volumes where uuid='%s';" % vol.id)
        self.debug("-----------------------volume's  name in DB is: %s" %vol_name_db)
        if(pool_data_vmx):
            vmx_file = vm.instancename + ".vmx"
            if vol.type == "ROOT":
                self.assertIn(
                      vmx_file,
                      pool_data_vmx,
                      "The VMX files are missing"
                      )
        if(pool_data_vmdk):
            vmdk_file1 = vol.path + ".vmdk"
            vmdk_file2 = vol.path + "-flat.vmdk"

            self.assertIn(
                      vmdk_file1,
                      pool_data_vmdk,
                      "The VMDK files are missing"
                      )
            self.assertIn(
                      vmdk_file2,
                      pool_data_vmdk,
                      "The VMDK flat files are missing"
                      )
    return

def GetDestinationStoragePool(self, poolsToavoid, storage_scope, storage_type):
    """ Get destination pool which has scope same as migrateto
        and which is not in avoid set
    """

    destinationPool = None
    destinationCluster = None
    if storage_scope == "within_cluster" or storage_scope == "across_cluster":
        scope = "CLUSTER"
    else :
        scope = "ZONE"

    pool = list_storage_pools(self.apiclient, name = poolsToavoid)
    clusters = list_clusters(self.apiclient, listall=True)
    if storage_scope == "across_cluster":
        for cluster in clusters:
            if cluster.id not in pool[0].clusterid:
                if len(list_storage_pools(self.apiclient, clusterid = cluster.id)) > 0:
                    destinationCluster = cluster
                    break
        pools_in_cluster = list_storage_pools(self.apiclient, clusterid = destinationCluster.id, scope = scope)
        for pool in pools_in_cluster:
            if pool.type == storage_type:
                destinationPool=pool
                break
        return destinationPool
    elif storage_scope == "within_cluster":
        destinationCluster = list_clusters(self.apiclient, id=pool[0].clusterid, listall=True)[0]
        storagepools = list_storage_pools(self.apiclient, clusterid = destinationCluster.id, scope = scope)
        for pool in storagepools:
            if pool.name not in poolsToavoid and pool.type == storage_type:
                destinationPool = pool
        return destinationPool
    elif storage_scope == "ZONE":
        storagepools = list_storage_pools(self.apiclient, scope = scope)
        for pool in storagepools:
            if pool.name not in poolsToavoid and pool.type == storage_type: 
                destinationPool = pool
        return destinationPool

def restart_mgmt_server(self, hostip, port, username, password):
    """Restarts the management server"""

    try:
        # Get the SSH client
        ssh = is_server_ssh_ready(
            hostip,
            port,
            username,
            password,
        )
        result = ssh.execute("/etc/init.d/cloudstack-management restart")
        res = str(result)
        # Server Stop - OK
        # Server Start - OK
        if res.count("OK") != 2:
            raise ("ErrorInReboot!")
    except Exception as e:
        raise e
    return 

def check_host_capacity(self, hostid, vm):
    """Checks whether host has enough capacity to migrate the VM
    """
    host = list_hosts(self.apiclient, id=hostid, listall=True)[0]
    host_memory_available_in_MB = (host.memorytotal - host.memoryallocated)/1024*1024*0.8
    memory_of_vm = vm.memory
    host_cpu_available_in_MHz = (host.cpuspeed - host.cpuspeed * float(host.cpuallocated.replace("%",""))/100)*0.8
    cpu_of_vm = vm.cpuspeed
    if host_memory_available_in_MB > memory_of_vm and host_cpu_available_in_MHz > cpu_of_vm:
        return PASS
    else:
        return FAILED

def check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1):
    """
    This function allocated a public ip, and creates a nat rule for the VM
    Then tries to ssh into the VM using that public IP
    This function again is to check VM accessibility post migration
    """
    src_nat_ip_addrs = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
    self.assertEqual(
            isinstance(src_nat_ip_addrs, list),
            True,
            "Check list response returns a valid list"
        )
    src_nat_ip_addr = src_nat_ip_addrs[0]
        # Open up firewall port for SSH
    firewall_rule = FireWallRule.create(
                    self.apiclient,
                    ipaddressid=src_nat_ip_addr.id,
                    protocol=self.testdata["natrule"]["protocol"],
                    cidrlist=['0.0.0.0/0'],
                    startport=self.testdata["natrule"]["publicport"],
                    endport=self.testdata["natrule"]["publicport"]
                 )

        # Create NAT rule
    nat_rule = NATRule.create(
            self.apiclient,
            virtual_machine_1,
            self.testdata["natrule"],
            src_nat_ip_addr.id
         )

    list_nat_rule_response = list_nat_rules(
            self.apiclient,
            id=nat_rule.id
         )
    self.assertEqual(
            isinstance(list_nat_rule_response, list),
            True,
            "Check list response returns a valid list"
         )

    self.assertNotEqual(
            len(list_nat_rule_response),
            0,
            "Check Port Forwarding Rule is created"
         )
    self.assertEqual(
            list_nat_rule_response[0].id,
            nat_rule.id,
            "Check Correct Port forwarding Rule is returned"
        )
        # SSH virtual machine to test port forwarding
    try:
        self.debug("SSHing into VM with IP address %s with NAT IP %s" %
                       (
                           virtual_machine_1.ipaddress,
                           src_nat_ip_addr.ipaddress
                       ))

        virtual_machine_1.get_ssh_client(src_nat_ip_addr.ipaddress)
        vm_response = VirtualMachine.list(
                self.apiclient,
                id=virtual_machine_1.id
            )
        if vm_response[0].state != 'Running':
            self.fail(
                    "State of VM : %s is not found to be Running" % str(
                        virtual_machine_1.ipaddress))
    except Exception as e:
        self.fail(
                "SSH Access failed for %s: %s" %
                (virtual_machine_1.ipaddress, e)
            )

    try:
        nat_rule.delete(self.apiclient)
    except Exception as e:
        self.fail("NAT Rule Deletion Failed: %s" % e)

    try:
        firewall_rule.delete(self.apiclient)
    except Exception as e:
        self.fail("Firewall Rule Deletion Failed: %s" % e)

    return

@ddt
class TestStorageLiveMigrationVmware(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestStorageLiveMigrationVmware, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.dbclient = cls.testClient.getDbConnection()
        cls.exceptionList = []
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
                "xenserver",
                "hyper-v"]:
            raise unittest.SkipTest(
                "Storage migration not supported on %s" %
                cls.hypervisor)
        # Get Hosts in the cluster and iscsi/vmfs storages for that cluster
        iscsi_pools = []
        try :
            cls.list_vmware_clusters = list_clusters(cls.apiclient, hypervisor="vmware")
        except Exception as e:
            raise unittest.SkipTest(e)

        if len(cls.list_vmware_clusters) < 1 :
            raise unittest.SkipTest("There is no cluster available in the setup")
        else :
            for cluster in cls.list_vmware_clusters :
                try:
                    list_esx_hosts = list_hosts(cls.apiclient, clusterid = cluster.id)
                except Exception as e:
                    raise unittest.SkipTest(e)
                if len(list_esx_hosts) > 1 :
                    try:
                        list_storage = list_storage_pools(cls.apiclient, clusterid = cluster.id)
                    except Exception as e:
                        raise unittest.SkipTest(e)
                    for storage in list_storage :
                        if storage.type == "VMFS" :
                            iscsi_pools.append(storage)
                    if len(iscsi_pools) > 1:
                        break
                    else :
                        iscsi_pools = []
        if len(iscsi_pools) < 2 :
            raise unittest.SkipTest("Not enough resources available in the setup")
        cls.hosts = list_esx_hosts
        cls.pools = list_storage

        # Create an account
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
        # Create Service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        if cls.zone.localstorageenabled:
            cls.testdata["service_offering"]["storagetype"] = 'local'
            cls.service_offering_local1 = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"]
            )
        cls._cleanup.append(cls.service_offering_local1)

        # Create Disk offering
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.testdata["disk_offering"]
        )
        # Create disk offering for resize
        cls.resized_disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.testdata["resized_disk_offering"]
        )
        if cls.zone.localstorageenabled:
            cls.testdata["disk_offering"]["storagetype"] = 'local'
            cls.disk_offering_local1 = DiskOffering.create(
            cls.apiclient,
            cls.testdata["disk_offering"]
            )
        cls._cleanup.append(cls.disk_offering_local1)

        # Register windows 2012 server Template if it is not present
        cls.windows_template = get_windows_template(
            cls.apiclient,
            cls.zone.id,
            ostype_desc = "Windows Server 2012 (64-bit)",
            template_type = "USER",
            hypervisor = "VMware",
            template_filter = "all"
            )
            
        #cls.template = get_windows_template(cls.apiclient, cls.zone.id ,ostype_desc="Windows Server 2012 (64-bit)")
        cls.testdata["vgpu"]["Windows Server 2012 (64-bit)"]["url"] = "http://10.147.28.7/templates/CPP_XD_Interop_Templates/VMWare/Win2012.ova"
        cls.testdata["vgpu"]["Windows Server 2012 (64-bit)"]["format"] = "OVA"

        if cls.windows_template == FAILED:
            if "http://pleaseupdateURL/dummy.vhd" in cls.testdata[
                    "vgpu"]["Windows Server 2012 (64-bit)"]["url"]:
                raise unittest.SkipTest(
                    "Check Test Data file if it has the valid template URL")
            cls.windows_template = Template.register(
                cls.apiclient,
                cls.testdata["vgpu"]["Windows Server 2012 (64-bit)"],
                hypervisor="VMware",
                zoneid=cls.zone.id,
            )
            timeout = cls.testdata["vgpu"]["timeout"]

            while True:
                time.sleep(cls.testdata["vgpu"]["sleep"])
                list_template_response = Template.list(
                    cls.apiclient,
                    templatefilter=cls.testdata["templatefilter"],
                    id=cls.windows_template.id
                )
                if (isinstance(list_template_response, list)) is not True:
                    raise unittest.SkipTest(
                        "Check list template api response returns a valid list")

                if len(list_template_response) is None:
                    raise unittest.SkipTest(
                        "Check template registered is in List Templates")
                template_response = list_template_response[0]
                if template_response.isready:
                    break
                if timeout == 0:
                    raise unittest.SkipTest(
                        "Failed to download template(ID: %s). " %
                        template_response.id)

                timeout = timeout - 1

        cls._cleanup.append(cls.disk_offering)
        cls._cleanup.append(cls.resized_disk_offering)

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
        # Cancel maintenance state of all hosts 
        list_host = list_hosts(self.apiclient, listall=True)
        for host in list_host:
            if host.resourcestate == "Maintenance":
                Host.cancelMaintenance(self.apiclient, id=host.id)
                Host.getState(
                              self.apiclient,
                              host.id,
                              "Up",
                              "Enabled"
                             )
        # Cancel maintenance state of all storage pools
        list_pools = list_storage_pools(self.apiclient, listall=True)
        for pool in list_pools:
            if pool.state == "Maintenance":
                cmd = cancelStorageMaintenance.cancelStorageMaintenanceCmd()
                cmd.id = pool.id
                self.apiclient.cancelStorageMaintenance(cmd)
                StoragePool.getState(
                                     self.apiclient,
                                     pool.id,
                                     "Up"
                                    )
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def get_vm_state(self, apiclient, vmid, state, timeout=600):
        """List VM and check if its state is as expected
        @returnValue - List[Result, Reason]
                       1) Result - FAIL if there is any exception
                       in the operation or VM state does not change
                       to expected state in given time else PASS
                       2) Reason - Reason for failure"""

        returnValue = [FAIL, "VM state not trasited to %s,\
                        operation timed out" % state]

        while timeout > 0:
            try:
                projectid = None
                if hasattr(self, "projectid"):
                    projectid = self.projectid
                vms = VirtualMachine.list(apiclient, projectid=projectid,
                          id=vmid, listAll=True)
                validationresult = validateList(vms)
                if validationresult[0] == FAIL:
                    raise Exception("VM list validation failed: %s" % validationresult[2])
                elif str(vms[0].state).lower().decode("string_escape") == str(state).lower():
                    returnValue = [PASS, None]
                    break
            except Exception as e:
                returnValue = [FAIL, e]
                break
            time.sleep(60)
            timeout -= 60
        return returnValue

    def deploy_virtual_machine(self, service_offering_id, vm, template_id):
        """
        Function to Deploy VMs
        """
        virtual_machine = VirtualMachine.create(
        self.userapiclient,
        self.testdata[vm],
        accountid=self.account.name,
        zoneid=self.zone.id,
        domainid=self.account.domainid,
        serviceofferingid=service_offering_id,
        templateid=template_id
        )
        virtual_machine.getState(
                                 self.userapiclient,
                                 "Running"
                                 )

        return virtual_machine

    def GetDestinationHost(self, hostsToavoid, vm, scope):
        """
        This method gives us the destination host to which VM will be migrated
        It takes the source host i.e. hostsToavoid as input
        """
        destinationHost = None
        destinationCluster = None
        host = list_hosts(self.apiclient, id = hostsToavoid)
        clusters = list_clusters(self.apiclient, listall=True)
        if scope == "across_cluster":
            for cluster in clusters:
                if cluster.id not in host[0].clusterid:
                    hosts_in_cluster = list_hosts(self.apiclient, clusterid = cluster.id)
                    if len(hosts_in_cluster)!=0:
                        destinationCluster = cluster
                        break
            hosts = list_hosts(self.apiclient, clusterid = destinationCluster.id)
            for host in hosts:
                response = check_host_capacity(self, host.id, vm)
                if response == PASS:
                    destinationHost=host
            return destinationHost
        elif scope == "within_cluster":
            hosts = list_hosts(self.apiclient, clusterid = host[0].clusterid)
            for host in hosts:
                response = check_host_capacity(self, host.id, vm)
                if host.id not in hostsToavoid and response is PASS:
                    destinationHost = host
                    break
            return destinationHost

    def GetDestinationHostLocal(self, hostsToavoid, vm, scope):
        """
        This method gives us the destination host to which VM will be migrated
        It takes the souce host i.e. hostsToavoid as input
        """
        destinationHost=None
        destinationCluster = None
        if scope == "across_cluster":
            host = list_hosts(self.apiclient, id = hostsToavoid)
            clusters = list_clusters(self.apiclient, listall=True)
            for cluster in clusters:
                if cluster.id not in host[0].clusterid:
                    hosts_in_cluster = list_hosts(self.apiclient, clusterid = cluster.id)
                    if len(hosts_in_cluster)!=0:
                        destinationCluster = cluster
                        break
            hosts = list_hosts(self.apiclient, clusterid = destinationCluster.id)
            for host in hosts:
                response = check_host_capacity(self, host.id, vm)
                if host.id not in hostsToavoid and response == PASS:
                    pool = list_storage_pools(self.apiclient, scope = "Host", name = host.name + " Local Storage")
                    if pool:
                        destinationHost = host
                        break
            return destinationHost

        for host in self.hosts:
            response = check_host_capacity(self, host.id, vm)
            if host.id not in hostsToavoid and response == PASS:
                pool = list_storage_pools(self.apiclient, scope = "Host", name = host.name + " Local Storage")
                if pool:
                    destinationHost = host
                    break
        return destinationHost

    def takeVmSnapshotNegative(self, vm_id):
        """
        This method takes VM snapshots and stores the exception
        To be used in the negative scenario where we take snapshot when 
        migration is in progress
        """
        try:
            with self.assertRaises(Exception):
                VmSnapshot.create(self.apiclient, vmid = vm_id)

        except Exception as e:
            self.exceptionList.append(e)

    def resizeVolumeNegative(self, volume):
        """
        This method resizes volume and stores the exception
        To be used in the negative scenario where we resize a volume when
        migration is in progress
        """
        try:
            with self.assertRaises(Exception):
                volume.resize(self.apiclient, diskofferingid = self.resized_disk_offering.id)

        except Exception as e:
            self.exceptionList.append(e)

    def takeVolumeSnapshotNegative(self, volumeid):
        """
        This method takes volume snapshots and stores the exception
        To be used in the negative scenario where we take snapshot when 
        migration is in progress
        """
        try:
            with self.assertRaises(Exception):
                Snapshot.create(self.apiclient, volume_id = volumeid)

        except Exception as e:
            self.exceptionList.append(e)

    def stopVmNegative(self, vm):
        """
        This method tries to destroy a VM and stores the exception
        To be used in the negative scenario where destroy a VM when
        migration is in progress
        """
        try:
            with self.assertRaises(Exception):
                vm.stop(self.apiclient)
        
        except Exception as e:
            self.exceptionList.append(e)

    @data(('VMFS', 'within_cluster', 'linux'), ('VMFS', 'within_cluster', 'windows'), ('VMFS', 'across_cluster', 'linux'), ('VMFS', 'across_cluster', 'windows'), 
          ('NetworkFilesystem', 'within_cluster', 'linux'), ('NetworkFilesystem', 'within_cluster', 'windows'), ('NetworkFilesystem', 'across_cluster', 'linux'), 
          ('NetworkFilesystem', 'across_cluster', 'windows'))
    @unpack
    @attr(tags=["advanced", "basic", "vmware", "vmfs", "shared"], required_hardware="true")
    def test_01_vm_and_volumes_live_migration_for_vmware_vmfs(self, first_value, second_value, third_value):
        """
        This Test Path tests vMotion for NFS as well as VMFS within cluster,
        across cluster and for both windows and linux VMs using DATA DRIVEN TESTING.
        This test will run once for each of the 8 configurations give as @data
        1. Migrate VM from one host to another
        2. Migrate VMs ROOT volume from one storage to another
        3. Migrate VM to another Host and ROOT volume to another storage
        4. Attach a data disk to VM, migrate VM to a different host and its volumes to different pools.
        5. Upload a volume, attach it to VM, migrate VM to a different host and its volumes to different pools.
        6. Create volume snapshots on all volumes , migrate VM to a different host and its volumes to different pools.
        7. Resize the data disk, migrate VM to a different host and its volumes to different pools.
        8. Restore the VM, migrate VM to a different host and its volumes to different pools.
        9. Detach the data disk, create another VM, attach the data disk to that VM and then migrate that VM and its volumes.
        10. Detach upload volume, attach it to the 2nd VM, and then migrate that VM and its volumes.
        11. Create snapshots for all volumes of 2nd vM, then migrate VM and its volumes.

        After each storage migration step, following validation is done
        a) Create VM snapshots to check data integrity - @method used : VmSnapshotToCheckDataIntegrity(self,vm)
        b) Login to the Host/storage pool and check for the VMDK and VMX files for VM and its volumes - @method used : check_files(self, vm,destinationHost)
        c) Check for VM accessibility by sshing to the VM - @method used : check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        """
        storage_type = first_value
        storage_scope = second_value
        ostype = third_value

        if ostype == 'windows':
            template_id = self.windows_template.id
        else:
            template_id = self.template.id

        count_host=0
        count_pool=0
        storage_pool = []
        if len(self.list_vmware_clusters) < 2:
            if (storage_scope == "across_cluster"):
                raise unittest.SkipTest("The setup doesn't have more than one cluster, so can't execute these set of tests")
        if len(self.list_vmware_clusters) >= 2:
            for cluster in self.list_vmware_clusters:
                if len(list_hosts(self.apiclient, clusterid = cluster.id)) >= 1:
                    count_host += 1
                pools = list_storage_pools(self.apiclient, clusterid = cluster.id )
                for pool in pools:
                    if pool.storage == storage_type:
                        storage_pool.append(pool)
                if len(storage_pool) >= 1: 
                    count_pool += 1
                storage_pool = []
        if storage_scope == "across_cluster":
            if count_host < 2 | count_pool < 2:
                raise unittest.SkipTest("The setup doesn't have enough pools or enough hosts. To run these tests the setup must have atleast 2 clusters, each having min 1 host and 1 vmfs storage pools")

        self.debug("---------------This is the test no 1--------------")
        """
        Create a VM, live migrate the VM
        """
        vm = "virtual_machine2"
        virtual_machine_1 = self.deploy_virtual_machine(self.service_offering.id, vm, template_id)
        self.cleanup.append(virtual_machine_1)
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        #Get destination host
        destinationHost = self.GetDestinationHost(vm.hostid, virtual_machine_1, storage_scope)
        #Migrate the VM
        if storage_scope == "across_cluster":
            vol_list = []
            destinationPools = []
            vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
            VmSnapshotToCheckDataIntegrity(self,vm)
            check_files(self, vm,destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        else :
            vm = MigrateVm(self, virtual_machine_1, destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 2--------------")
        """
        Migrate the ROOT Volume
        Can't migrate a volume to another cluster, so won't run this test in that case
        """
        # Get ROOT volume and destination pool
        if storage_scope != "across_cluster":
            vol_list = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)
            root_vol = vol_list[0]
            destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type)
            #Migrate ROOT volume
            islive = True
            MigrateDataVolume(self, root_vol, destinationPool, islive)
            VmSnapshotToCheckDataIntegrity(self,vm)
            check_files(self, vm ,destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 3--------------")
        """
        Migrate the VM and ROOT volume
        """
        #Get all volumes to be migrated
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 4--------------")
        """
        Add a data disk and migrate vm, data disk and root disk
        """

        data_disk_1 = Volume.create(
                                    self.userapiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )

        self.debug("Created volume with ID: %s" % data_disk_1.id)

        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )

        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 5--------------")
        """
        Upload a Volume, Attach it to the VM, Migrate all the volumes and VM.
        """
        #upload a volume
        self.testdata["configurableData"]["upload_volume"]["format"] = "OVA"
        self.testdata["configurableData"]["upload_volume"]["url"] = "http://download.cloud.com/templates/acton/acton-systemvm-01062012.ova"
        upload_volume = Volume.upload(
                                      self.userapiclient,
                                      self.testdata["configurableData"]["upload_volume"],
                                      account= self.account.name,
                                      domainid= self.domain.id,
                                      zoneid= self.zone.id
                                      )
        upload_volume.wait_for_upload(self.userapiclient)
        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         upload_volume
                         )

        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        self.debug("........................checking for files before taking snapshot ..................................")
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        
        self.debug("---------------This is the test no 6--------------")
        """
        Create snapshots on all the volumes, Migrate all the volumes and VM.
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.userapiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.userapiclient, 
                                    snapshotstate="backedup", 
                                    )
        # Migrate all volumes and VMs
        self.debug("..................................checking for files just after taking snapshot...................................")
        check_files(self, vm,destinationHost)
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        self.debug("........................checking for files after taking snapshot and migrating VMs........................")
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 7--------------")
        """
        Resize the data volume , Migrate all the volumes and VM.
        """
        data_disk_1.resize(
                           self.userapiclient,
                           diskofferingid = self.resized_disk_offering.id
                           )
        # Migrate all volumes and VMs
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 8--------------")
        """
        Restore the VM , Migrate all the volumes and VM.
        """
        virtual_machine_1.restore(self.userapiclient)
        virtual_machine_1.getState(
                                     self.userapiclient,
                                     "Running"
                                     )
        # Migrate the VM and its volumes
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 9--------------")
        """
        Detach the Data disk, Deploy another VM, attach the data disk and migrate.
        """

        virtual_machine_1.detach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        vm = "virtual_machine3"
        virtual_machine_2 = self.deploy_virtual_machine(self.service_offering.id, vm, self.template.id)
        self.cleanup.append(virtual_machine_2)
        virtual_machine_2.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_2.id, listall=True)[0]
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

        self.debug("---------------This is the test no 10--------------")
        """
        Detach the uploaded volume, attach it to another vm and migrate.
        """

        virtual_machine_1.detach_volume(
                         self.userapiclient,
                         upload_volume
                         )

        virtual_machine_2.attach_volume(
                         self.userapiclient,
                         upload_volume
                         )
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

        self.debug("---------------This is the test no 11--------------")
        """
        Create snapshots on all the volumes, Migrate all the volumes and VM.
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.userapiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.userapiclient, 
                                    snapshotstate="backedup", 
                                    )
        # Migrate all volumes and VMs

        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

    @data(('within_cluster', 'linux'), ('within_cluster', 'windows'), ('across_cluster', 'linux'), ('across_cluster', 'windows'))
    @unpack
    @attr(tags=["advanced", "basic", "vmware", "vmfs", "local"], required_hardware="true")
    def test_02_vm_and_volumes_live_migration_for_vmware_vmfs_local_storage(self, first_value, second_value):
        """
        This Test Path tests vMotion for VM in local storage within cluster,
        across cluster and for both windows and linux VMs using DATA DRIVEN TESTING
        1. Migrate VM from one host to another
        2. Migrate VMs ROOT volume from one storage to another
        3. Migrate VM to another Host and ROOT volume to another storage
        4. Attach a data disk to VM, migrate VM to a different host and its volumes to different pools.
        5. Upload a volume, attach it to VM, migrate VM to a different host and its volumes to different pools.
        6. Create volume snapshots on all volumes , migrate VM to a different host and its volumes to different pools.
        7. Resize the data disk, migrate VM to a different host and its volumes to different pools.
        8. Restore the VM, migrate VM to a different host and its volumes to different pools.
        9. Detach the data disk, create another VM, attach the data disk to that VM and then migrate that VM and its volumes.
        10. Detach upload volume, attach it to the 2nd VM, and then migrate that VM and its volumes.
        11. Create snapshots for all volumes of 2nd vM, then migrate VM and its volumes.

        After each storage migration step, following validation is done
        a) Create VM snapshots to check data integrity - @method used : VmSnapshotToCheckDataIntegrity(self,vm)
        c) Check for VM accessibility by sshing to the VM - @method used : check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        """
        if  not self.zone.localstorageenabled:
            raise unittest.SkipTest("The setup doesn't have local storage enabled")
        scope = first_value
        ostype = second_value
        if ostype == 'windows':
            template_id = self.windows_template.id
        else:
            template_id = self.template.id

        count_host=0
        count_pool=0
        pool_local = []
        if len(self.list_vmware_clusters) < 2:
            if (scope == "across_cluster"):
                raise unittest.SkipTest("The setup doesn't have more than one cluster, so can't execute these set of tests")
        if len(self.list_vmware_clusters) >= 2:
            for cluster in self.list_vmware_clusters:
                if len(list_hosts(self.apiclient, clusterid = cluster.id)) >= 1:
                    count_host += 1
                pools = list_storage_pools(self.apiclient, clusterid = cluster.id )
                for pool in pools:
                    if pool.scope == "HOST":
                        pool_local.append(pool)
                if len(pool_local) >= 1: 
                    count_pool += 1
                pool_local = []
        if scope == "across_cluster":
            if count_host < 2 | count_pool < 2:
                raise unittest.SkipTest("The setup doesn't have enough pools or enough hosts. To run these tests the setup must have atleast 2 clusters, each having min 1 host and 1 local storage pools")

        self.debug("---------------This is the test no 1--------------")
        """
        Create a VM, live migrate the VM
        """
        vm = "virtual_machine2"
        virtual_machine_1 = self.deploy_virtual_machine(self.service_offering_local1.id, vm, template_id)
        self.cleanup.append(virtual_machine_1)
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]

        #Get destination host
        destinationHost = self.GetDestinationHostLocal(vm.hostid, virtual_machine_1, scope)
        #Migrate the VM
        vol_list = []
        destinationPools = []
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 2--------------")
        """
        Add a data disk and migrate vm
        """

        data_disk_1 = Volume.create(
                                    self.userapiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering_local1.id
                                  )

        self.debug("Created volume with ID: %s" % data_disk_1.id)

        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )

        destinationHost = self.GetDestinationHostLocal(vm.hostid, vm, scope)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 3--------------")
        """
        Upload a Volume, Attach it to the VM, Migrate all the volumes and VM.
        """
        #upload a volume
        self.testdata["configurableData"]["upload_volume"]["format"] = "OVA"
        self.testdata["configurableData"]["upload_volume"]["url"] = "http://download.cloud.com/templates/acton/acton-systemvm-01062012.ova"
        upload_volume = Volume.upload(
                                      self.userapiclient,
                                      self.testdata["configurableData"]["upload_volume"],
                                      account= self.account.name,
                                      domainid= self.domain.id,
                                      zoneid= self.zone.id
                                      )
        upload_volume.wait_for_upload(self.userapiclient)
        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         upload_volume
                         )

        destinationHost = self.GetDestinationHostLocal(vm.hostid, vm, scope)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 6--------------")
        """
        Create snapshots on all the volumes, Migrate VM.
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.userapiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.userapiclient, 
                                    snapshotstate="backedup", 
                                    )
        # Migrate all volumes and VMs

        destinationHost = self.GetDestinationHostLocal(vm.hostid, vm, scope)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 7--------------")
        """
        Resize the data volume , Migrate all the volumes and VM.
        """
        data_disk_1.resize(
                           self.userapiclient,
                           diskofferingid = self.resized_disk_offering.id
                           )
        # Migrate all volumes and VMs
        destinationHost = self.GetDestinationHostLocal(virtual_machine_1.hostid, vm, scope)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)


        self.debug("---------------This is the test no 8--------------")
        """
        Restore the VM , Migrate all the volumes and VM.
        """
        virtual_machine_1.restore(self.userapiclient)
        virtual_machine_1.getState(
                                     self.userapiclient,
                                     "Running"
                                     )
        # Migrate the VM and its volumes
        destinationHost = self.GetDestinationHostLocal(vm.hostid, vm, scope)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 9--------------")
        """
        Detach the Data disk, Deploy another VM, attach the data disk and migrate.
        """

        virtual_machine_1.detach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        vm = "virtual_machine3"
        virtual_machine_2 = self.deploy_virtual_machine(self.service_offering_local1.id, vm, self.template.id)
        self.cleanup.append(virtual_machine_2)
        virtual_machine_2.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_2.id, listall=True)[0]
        destinationHost = self.GetDestinationHostLocal(vm.hostid, vm, scope)
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

        self.debug("---------------This is the test no 10--------------")
        """
        Detach the uploaded volume, attach it to another vm and migrate.
        """

        virtual_machine_1.detach_volume(
                         self.userapiclient,
                         upload_volume
                         )

        virtual_machine_2.attach_volume(
                         self.userapiclient,
                         upload_volume
                         )
        destinationHost = self.GetDestinationHostLocal(vm.hostid, vm, scope)
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

        self.debug("---------------This is the test no 11--------------")
        """
        Create snapshots on all the volumes, Migrate all the volumes and VM.
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.userapiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.userapiclient, 
                                    snapshotstate="backedup", 
                                    )
        # Migrate all volumes and VMs

        destinationHost = self.GetDestinationHostLocal(vm.hostid, vm, scope)
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

    @data(('VMFS', 'within_cluster', 'windows'), ('VMFS', 'within_cluster', 'linux'), ('VMFS', 'across_cluster', 'linux'), ('VMFS', 'across_cluster', 'windows'), 
          ('NetworkFilesystem', 'within_cluster', 'linux'), ('NetworkFilesystem', 'within_cluster', 'windows'), ('NetworkFilesystem', 'across_cluster', 'linux'), 
          ('NetworkFilesystem', 'across_cluster', 'windows'))
    @unpack
    @attr(tags=["advanced", "basic", "vmware", "vmfs", "zwps"], required_hardware="true")
    def test_03_vm_and_volumes_live_migration_for_vmware_vmfs_across_zwps_cwps(self, first_value, second_value, third_value):
        """
        This Test Path tests vMotion for NFS as well as VMFS within cluster,
        across cluster and across zones and for both windows and linux VMs using DATA DRIVEN TESTING
        1. Migrate VM from one host to another
        2. Migrate VMs ROOT volume from one storage to another
        3. Migrate VM to another Host and ROOT volume to another storage
        4. Attach a data disk to VM, migrate VM to a different host and its volumes to different pools.
        5. Upload a volume, attach it to VM, migrate VM to a different host and its volumes to different pools.
        6. Create volume snapshots on all volumes , migrate VM to a different host and its volumes to different pools.
        7. Resize the data disk, migrate VM to a different host and its volumes to different pools.
        8. Restore the VM, migrate VM to a different host and its volumes to different pools.
        9. Detach the data disk, create another VM, attach the data disk to that VM and then migrate that VM and its volumes.
        10. Detach upload volume, attach it to the 2nd VM, and then migrate that VM and its volumes.
        11. Create snapshots for all volumes of 2nd vM, then migrate VM and its volumes.

        After each storage migration step, following validation is done
        a) Create VM snapshots to check data integrity - @method used : VmSnapshotToCheckDataIntegrity(self,vm)
        b) Login to the Host/storage pool and check for the VMDK and VMX files for VM and its volumes - @method used : check_files(self, vm,destinationHost)
        c) Check for VM accessibility by sshing to the VM - @method used : check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        """
        storage_type = first_value
        storage_scope = second_value
        ostype = third_value
        self.debug("------------------------------------- third value is also there..................%s..................." %ostype)
        if ostype == 'windows':
            template_id = self.windows_template.id
        else:
            template_id = self.template.id

        scope = "ZONE"
        list_zwps_pools = list_storage_pools(self.apiclient, scope="ZONE", listall=True)
        zwps_pools = []
        for pool in list_zwps_pools:
            if pool.type == storage_type:
                zwps_pools.append(pool)
        if len(zwps_pools) < 2:
            raise unittest.SkipTest("The setup doesn't have enough zone wide primary storages of %s type, we need atleast 2" %storage_type)

        count_host=0
        count_pool=0
        pool_vmfs = []
        if len(self.list_vmware_clusters) < 2:
            if storage_scope == "across_cluster":
                raise unittest.SkipTest("The setup doesn't have more than one cluster, so can't execute these set of tests")
        if len(self.list_vmware_clusters) >= 2:
            for cluster in self.list_vmware_clusters:
                if len(list_hosts(self.apiclient, clusterid = cluster.id)) >= 1:
                    count_host += 1
                pools = list_storage_pools(self.apiclient, clusterid = cluster.id )
                for pool in pools:
                    if pool.storage is storage_type:
                        pool_vmfs.append(pool)
                if len(pool_vmfs) >= 1: 
                    count_pool += 1
                pool_vmfs = []
        if storage_scope == "across_cluster":
            if count_host < 2 | count_pool < 2:
                raise unittest.SkipTest("The setup doesn't have enough pools or enough hosts. To run these tests the setup must have atleast 2 clusters, each having min 1 host and 1 vmfs storage pools")

        self.debug("---------------This is the test no 1--------------")
        """
        Create a VM, live migrate the VM
        """
        vm = "virtual_machine2"
        virtual_machine_1 = self.deploy_virtual_machine(self.service_offering.id, vm, template_id)
        self.cleanup.append(virtual_machine_1)
        #Get destination host
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        destinationHost = self.GetDestinationHost(vm.hostid, virtual_machine_1, storage_scope)
        #Migrate the VM
        if storage_scope == "different_cluster":
            vol_list = []
            destinationPools = []
            vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
            VmSnapshotToCheckDataIntegrity(self,vm)
            check_files(self, vm,destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        else :
            vm = MigrateVm(self, virtual_machine_1, destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 2--------------")
        """
        Migrate the ROOT Volume to zwps
        Can't migrate a volume to another cluster, so won't run this test in that case
        """
        # Get ROOT volume and destination pool
        if storage_scope != "across_cluster":
            vol_list = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)
            root_vol = vol_list[0]
            destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type)
            #Migrate ROOT volume
            islive = True
            MigrateDataVolume(self, root_vol, destinationPool, islive)
            VmSnapshotToCheckDataIntegrity(self,vm)
            check_files(self, vm ,destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 3--------------")
        """
        Migrate the VM and ROOT volume to zwps
        """
        #Get all volumes to be migrated
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type)
        destinationPools.append(destinationPool)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 4--------------")
        """
        Add a data disk and migrate vm, data disk to zwps and root disk to cwps
        """

        data_disk_1 = Volume.create(
                                    self.userapiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )

        self.debug("Created volume with ID: %s" % data_disk_1.id)

        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )

        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)[0]
        vol_list.append(data_vol)
        #get destination Pool for DATA volume
        destinationPool = GetDestinationStoragePool(self, data_vol.storage, scope, storage_type)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 5--------------")
        """
        Upload a Volume, Attach it to the VM, Migrate all the volumes and VM.
        1st data disk to zwps
        2nd data disk to cwps
        root disk to zwps
        """
        #upload a volume
        self.testdata["configurableData"]["upload_volume"]["format"] = "OVA"
        self.testdata["configurableData"]["upload_volume"]["url"] = "http://download.cloud.com/templates/acton/acton-systemvm-01062012.ova"
        upload_volume = Volume.upload(
                                      self.userapiclient,
                                      self.testdata["configurableData"]["upload_volume"],
                                      account= self.account.name,
                                      domainid= self.domain.id,
                                      zoneid= self.zone.id
                                      )
        upload_volume.wait_for_upload(self.userapiclient)
        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         upload_volume
                         )

        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, scope, storage_type)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        self.debug("..............these are the volumes %s " %vol_list)
        self.debug("..............these are the pools %s " %destinationPools)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("........................checking for files before taking snapshot ..................................")
        check_files(self, vm,destinationHost)
        
        self.debug("---------------This is the test no 6--------------")
        """
        Create snapshots on all the volumes, Migrate all the volumes and VM.
        root disk to cwps
        data1 to cwps
        data2 to zwps
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.userapiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.userapiclient, 
                                    snapshotstate="backedup", 
                                    )
        # Migrate all volumes and VMs
        self.debug("..................................checking for files just after taking snapshot...................................")
        check_files(self, vm,destinationHost)
        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, scope, storage_type)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("........................checking for files after taking snapshot and migrating VMs........................")
        check_files(self, vm,destinationHost)

        self.debug("---------------This is the test no 7--------------")
        """
        Resize the data volume , Migrate all the volumes and VM.
        root disk to zwps
        data1 to zwps 
        data2 to zwps
        """
        data_disk_1.resize(
                           self.userapiclient,
                           diskofferingid = self.resized_disk_offering.id
                           )
        # Migrate all volumes and VMs
        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, scope, storage_type)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, scope, storage_type)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 8--------------")
        """
        Restore the VM , Migrate all the volumes and VM.
        root to cpws
        data1 to zwps
        data2 to cwps
        """
        virtual_machine_1.restore(self.userapiclient)
        virtual_machine_1.getState(
                                     self.userapiclient,
                                     "Running"
                                     )

        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, scope, storage_type)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 9--------------")
        """
        Detach the Data disk, Deploy another VM, attach the data disk and migrate.
        root to zwps
        data to cwps
        """

        virtual_machine_1.detach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        vm = "virtual_machine3"
        virtual_machine_2 = self.deploy_virtual_machine(self.service_offering.id, vm, self.template.id)
        self.cleanup.append(virtual_machine_2)
        virtual_machine_2.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        # Get destination Host
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_2.id, listall=True)[0]
        destinationHost = self.GetDestinationHost(vm.hostid, virtual_machine_2, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=virtual_machine_2.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=virtual_machine_2.id, type="DATADISK", listall=True)[0]
        vol_list.append(data_vol)
        #get destination Pool for DATA volume
        destinationPool = GetDestinationStoragePool(self, data_vol.storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

        self.debug("---------------This is the test no 10--------------")
        """
        Detach the uploaded volume, attach it to another vm and migrate.
        root to cwps
        data1 to zwps
        data2 to zwps
        """

        virtual_machine_1.detach_volume(
                         self.userapiclient,
                         upload_volume
                         )

        virtual_machine_2.attach_volume(
                         self.userapiclient,
                         upload_volume
                         )
        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, scope, storage_type)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, scope, storage_type)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

        self.debug("---------------This is the test no 11--------------")
        """
        Create snapshots on all the volumes, Migrate all the volumes and VM.
        root to zwps
        data1 to cwps
        data2 to zwps
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.userapiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.userapiclient, 
                                    snapshotstate="backedup", 
                                    )

        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, storage_scope, storage_type)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, scope, storage_type)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

    @data(('within_cluster', 'linux'), ('within_cluster', 'windows'), ('across_cluster', 'linux'), ('across_cluster', 'windows'))
    @unpack
    @attr(tags=["advanced", "basic", "vmware", "vmfs", "tagged"], required_hardware="true")
    def test_04_vm_and_volumes_live_migration_for_vmware_across_nfs_vmfs(self, first_value, second_value):
        """
        This Test Path tests vMotion from NFS <---> VMFS , within cluster
        across cluster and across zones and for both windows and linux VMs using DATA DRIVEN TESTING
        1. Migrate VM from one host to another
        2. Migrate VMs ROOT volume from one storage to another
        3. Migrate VM to another Host and ROOT volume to another storage
        4. Attach a data disk to VM, migrate VM to a different host and its volumes to different pools.
        5. Upload a volume, attach it to VM, migrate VM to a different host and its volumes to different pools.
        6. Create volume snapshots on all volumes , migrate VM to a different host and its volumes to different pools.
        7. Resize the data disk, migrate VM to a different host and its volumes to different pools.
        8. Restore the VM, migrate VM to a different host and its volumes to different pools.
        9. Detach the data disk, create another VM, attach the data disk to that VM and then migrate that VM and its volumes.
        10. Detach upload volume, attach it to the 2nd VM, and then migrate that VM and its volumes.
        11. Create snapshots for all volumes of 2nd vM, then migrate VM and its volumes.

        After each storage migration step, following validation is done
        a) Create VM snapshots to check data integrity - @method used : VmSnapshotToCheckDataIntegrity(self,vm)
        b) Login to the Host/storage pool and check for the VMDK and VMX files for VM and its volumes - @method used : check_files(self, vm,destinationHost)
        c) Check for VM accessibility by sshing to the VM - @method used : check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        """

        storage_type_nfs = "NetworkFilesystem"
        storage_type_vmfs = "VMFS"
        storage_scope = first_value
        ostype = second_value
        scope = "ZONE"

        if ostype == 'windows':
            template_id = self.windows_template.id
        else:
            template_id = self.template.id

        list_zwps_pools = list_storage_pools(self.apiclient, scope="ZONE", listall=True)
        zwps_vmfs_pools = []
        zwps_nfs_pools = []
        for pool in list_zwps_pools:
            if pool.type == storage_type_vmfs:
                zwps_vmfs_pools.append(pool)
            elif pool.type == storage_type_nfs:
                zwps_nfs_pools.append(pool)
        if len(zwps_vmfs_pools) < 1 :
            raise unittest.SkipTest("The setup doesn't have enough zone wide primary storages of %s type, we need atleast 2" %storage_type_vmfs)
        if len(zwps_nfs_pools) < 1 :
            raise unittest.SkipTest("The setup doesn't have enough zone wide primary storages of %s type, we need atleast 2" %storage_type_nfs)

        count_host=0
        count_pool_nfs=0
        count_pool_vmfs=0
        pool_vmfs = []
        pool_nfs = []
        if len(self.list_vmware_clusters) < 2:
            if storage_scope == "across_cluster":
                raise unittest.SkipTest("The setup doesn't have more than one cluster, so can't execute these set of tests")
        if len(self.list_vmware_clusters) >= 2:
            for cluster in self.list_vmware_clusters:
                if len(list_hosts(self.apiclient, clusterid = cluster.id)) >= 1:
                    count_host += 1
                pools = list_storage_pools(self.apiclient, clusterid = cluster.id )
                for pool in pools:
                    if pool.storage is storage_type_vmfs:
                        pool_vmfs.append(pool)
                    elif pool.storage is storage_type_nfs:
                        pool_nfs.append(pool)
                if len(pool_vmfs) >= 1: 
                    count_pool_vmfs += 1
                if len(pool_nfs) >= 1:
                    count_pool_nfs += 1
                pool_vmfs = []
                pool_nfs = []
        if storage_scope == "across_cluster":
            if count_host < 2 or count_pool_vmfs < 2 or count_pool_nfs < 2:
                raise unittest.SkipTest("The setup doesn't have enough pools or enough hosts. To run these tests the setup must have atleast 2 clusters, each having min 2 host 2 vmfs storage pools and 2 nfs storage pools")

        self.debug("---------------This is the test no 1--------------")
        """
        Create a VM, live migrate the VM
        """
        vm = "virtual_machine2"
        virtual_machine_1 = self.deploy_virtual_machine(self.service_offering.id, vm, template_id)
        self.cleanup.append(virtual_machine_1)
        #Get destination host
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        destinationHost = self.GetDestinationHost(vm.hostid, virtual_machine_1, storage_scope)
        #Migrate the VM
        if storage_scope == "different_cluster":
            vol_list = []
            destinationPools = []
            vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
            VmSnapshotToCheckDataIntegrity(self,vm)
            check_files(self, vm,destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        else :
            vm = MigrateVm(self, virtual_machine_1, destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 2--------------")
        """
        Migrate the ROOT Volume to zwps
        Can't migrate a volume to another cluster, so won't run this test in that case
        """
        # Get ROOT volume and destination pool
        if storage_scope != "across_cluster":
            vol_list = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)
            root_vol = vol_list[0]
            destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type_nfs)
            #Migrate ROOT volume
            islive = True
            MigrateDataVolume(self, root_vol, destinationPool, islive)
            VmSnapshotToCheckDataIntegrity(self,vm)
            check_files(self, vm ,destinationHost)
            check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 3--------------")
        """
        Migrate the VM and ROOT volume to zwps nfs
        """
        #Get all volumes to be migrated
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 4--------------")
        """
        Add a data disk and migrate vm, 
        data disk to zwps nfs and 
        root disk to cwps vmfs
        """

        data_disk_1 = Volume.create(
                                    self.userapiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )

        self.debug("Created volume with ID: %s" % data_disk_1.id)

        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )

        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)[0]
        vol_list.append(data_vol)
        #get destination Pool for DATA volume
        destinationPool = GetDestinationStoragePool(self, data_vol.storage, scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 5--------------")
        """
        Upload a Volume, Attach it to the VM, Migrate all the volumes and VM.
        1st data disk to cwps vmfs
        2nd data disk to zwps vmfs
        root disk to zwps nfs
        """
        #upload a volume
        self.testdata["configurableData"]["upload_volume"]["format"] = "OVA"
        self.testdata["configurableData"]["upload_volume"]["url"] = "http://download.cloud.com/templates/acton/acton-systemvm-01062012.ova"
        upload_volume = Volume.upload(
                                      self.userapiclient,
                                      self.testdata["configurableData"]["upload_volume"],
                                      account= self.account.name,
                                      domainid= self.domain.id,
                                      zoneid= self.zone.id
                                      )
        upload_volume.wait_for_upload(self.userapiclient)
        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         upload_volume
                         )

        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, storage_scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        self.debug("..............these are the volumes %s " %vol_list)
        self.debug("..............these are the pools %s " %destinationPools)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("........................checking for files before taking snapshot ..................................")
        check_files(self, vm,destinationHost)
        
        self.debug("---------------This is the test no 6--------------")
        """
        Create snapshots on all the volumes, Migrate all the volumes and VM.
        root disk to zwps vmfs
        data1 to zwps nfs
        data2 to cwps nfs
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.userapiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.userapiclient, 
                                    snapshotstate="backedup", 
                                    )
        # Migrate all volumes and VMs
        self.debug("..................................checking for files just after taking snapshot...................................")
        check_files(self, vm,destinationHost)
        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, storage_scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("........................checking for files after taking snapshot and migrating VMs........................")
        check_files(self, vm,destinationHost)

        self.debug("---------------This is the test no 7--------------")
        """
        Resize the data volume , Migrate all the volumes and VM.
        root disk to cwps vmfs
        data1 to zwps vmfs
        data2 to cwps nfs
        """
        data_disk_1.resize(
                           self.userapiclient,
                           diskofferingid = self.resized_disk_offering.id
                           )
        # Migrate all volumes and VMs
        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, storage_scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 8--------------")
        """
        Restore the VM , Migrate all the volumes and VM.
        root to zwps nfs
        data1 to cwps nfs
        data2 to zwps vmfs
        """
        virtual_machine_1.restore(self.userapiclient)
        virtual_machine_1.getState(
                                     self.userapiclient,
                                     "Running"
                                     )

        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, storage_scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.debug("---------------This is the test no 9--------------")
        """
        Detach the Data disk, Deploy another VM, attach the data disk and migrate.
        root to zwps nfs
        data to cwps vmfs
        """

        virtual_machine_1.detach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        vm = "virtual_machine3"
        virtual_machine_2 = self.deploy_virtual_machine(self.service_offering.id, vm, template_id)
        self.cleanup.append(virtual_machine_2)
        virtual_machine_2.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        # Get destination Host
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_2.id, listall=True)[0]
        destinationHost = self.GetDestinationHost(vm.hostid, virtual_machine_2, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=virtual_machine_2.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=virtual_machine_2.id, type="DATADISK", listall=True)[0]
        vol_list.append(data_vol)
        #get destination Pool for DATA volume
        destinationPool = GetDestinationStoragePool(self, data_vol.storage, storage_scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

        self.debug("---------------This is the test no 10--------------")
        """
        Detach the uploaded volume, attach it to another vm and migrate.
        root to cwps vmfs
        data1 to zwps nfs
        data2 to zwps vmfs
        """

        virtual_machine_1.detach_volume(
                         self.userapiclient,
                         upload_volume
                         )

        virtual_machine_2.attach_volume(
                         self.userapiclient,
                         upload_volume
                         )
        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

        self.debug("---------------This is the test no 11--------------")
        """
        Create snapshots on all the volumes, Migrate all the volumes and VM.
        root to cwps nfs
        data1 to cwps vmfs
        data2 to cwps nfs
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.userapiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.userapiclient, 
                                    snapshotstate="backedup", 
                                    )
        # Migrate all volumes and VMs

        # Get destination Host
        destinationHost = self.GetDestinationHost(vm.hostid, vm, storage_scope)
        vol_list = []
        destinationPools = []
        #list ROOT volume
        root_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        vol_list.append(root_vol)
        #get destination Pool for ROOT volume
        destinationPool = GetDestinationStoragePool(self, root_vol.storage, storage_scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        #list DATA volume
        data_vol = list_volumes(self.apiclient, virtualmachineid=vm.id, type="DATADISK", listall=True)
        #vol_list.append(data_vol)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[0])
        destinationPool = GetDestinationStoragePool(self, data_vol[0].storage, storage_scope, storage_type_vmfs)
        destinationPools.append(destinationPool)
        #get destination Pool for 1st DATA volume
        vol_list.append(data_vol[1])
        destinationPool = GetDestinationStoragePool(self, data_vol[1].storage, storage_scope, storage_type_nfs)
        destinationPools.append(destinationPool)
        # Migrate and verify
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self,vm)
        check_files(self, vm,destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)

    @attr(tags=["advanced", "basic", "vmware", "vmfs", "negative"], required_hardware="true")
    def test_05_vm_and_volumes_live_migration_for_vmware_negative_scenarios(self):
        """
        Test scenarios like :
        1. Take VM snapshot when vMotion is in progress
        2. Take Volume snapshot when vMotion is in progress
        3. Resize volume when vMotion is in progress
        4. Set vmware.vcenter.session.timeout to a low value and do migration
        """
        # Deploy a VM, create a data disks and attach it to the VM
        # Restart management server

        vm = "virtual_machine2"
        virtual_machine_1 = self.deploy_virtual_machine(self.service_offering.id, vm, self.template.id)
        #self.cleanup.append(virtual_machine_1)
        data_disk_1 = Volume.create(
                                    self.userapiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )
        self.debug("Created volume with ID: %s" % data_disk_1.id)

        virtual_machine_1.attach_volume(
                         self.userapiclient,
                         data_disk_1
                         )
        storage_scope = "within_cluster"
        storage_type = "VMFS"

        """
        1. VM snapshot negative test
        """
        try :
            vm = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
            destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
            thread_1 = Thread(
                              target = MigrateVmWithVolume,
                              args = (self, virtual_machine_1, destinationHost, vol_list, destinationPools,)
                              )
            thread_2 = Thread(
                              target = self.takeVmSnapshotNegative,
                              args = (virtual_machine_1.id,)
                              )
            thread_1.start()
            time.sleep(10)
            thread_2.start()
            thread_1.join()
            thread_2.join()
        except:
            self.debug("Error: unable to start thread")
            

        """
        2. Volume snapshot negative test
        """
        # list ROOT volume
        root_vol = list_volumes(self.apiclient, listall=True, type="ROOT", virtualmachineid=virtual_machine_1.id)[0]
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        try :
            destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
            thread_3 = Thread(
                              target = MigrateVmWithVolume,
                              args = (self, virtual_machine_1, destinationHost, vol_list, destinationPools,)
                              )
            thread_4 = Thread(
                              target = self.takeVolumeSnapshotNegative,
                              args = (root_vol.id,)
                            )
            thread_3.start()
            time.sleep(10)
            thread_4.start()
            thread_3.join()
            thread_4.join()
        except:
            self.debug("Error: unable to start thread")

        """
        3. Resize volume negative test
        """

        vm = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        try :
            destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
            thread_5 = Thread(
                              target = MigrateVmWithVolume,
                              args = (self, virtual_machine_1, destinationHost, vol_list, destinationPools,)
                              )
            thread_6 = Thread(
                              target = self.resizeVolumeNegative,
                              args = (data_disk_1,)
                            )
            thread_5.start()
            time.sleep(10)
            thread_6.start()
            thread_5.join()
            thread_6.join()
        except:
            self.debug("Error: unable to start thread")

        """
        4. Deploy a windows VM, set vmware.vcenter.session.timeout to a low value
           and do migration, migration should fail 
        """

        vm = "virtual_machine3"
        virtual_machine_2 = self.deploy_virtual_machine(self.service_offering.id, vm, self.windows_template.id)
        # list host for the VM
        vm_host = list_hosts(self.apiclient, id=virtual_machine_2.hostid, listall=True)[0]
        # list cluster for that host
        vm_cluster = list_clusters(self.apiclient, id=vm_host.clusterid, listall=True)[0]
        #list all hosts in the cluster
        host_list = list_hosts(self.apiclient, clusterid=vm_cluster.id, listall=True) 
        Configurations.update(
                              self.apiclient,
                              "vmware.vcenter.session.timeout",
                              "60"
                              )
        # Restart management server
        restart_mgmt_server(
                            self,
                            self.apiclient.connection.mgtSvr,
                            22,
                            self.apiclient.connection.user,
                            self.apiclient.connection.passwd
                            )
        time.sleep(120)
        for host in host_list:
            Host.getState(
                          self.apiclient,
                          host.id,
                          "Up",
                          "Enabled"
                          )

        self.cleanup.append(virtual_machine_2)
        vm = list_virtual_machines(self.apiclient, id=virtual_machine_2.id, listall=True)[0]
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm, storage_scope, storage_type)
        try :
            with self.assertRaises(Exception):
                MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        except Exception as e:
            self.exceptionList.append(e)

        Configurations.update(
                              self.apiclient,
                              "vmware.vcenter.session.timeout",
                              "1200"
                              )
        # Restart management server
        restart_mgmt_server(
                            self,
                            self.apiclient.connection.mgtSvr,
                            22,
                            self.apiclient.connection.user,
                            self.apiclient.connection.passwd
                            )
        time.sleep(120)
        for host in host_list:
            Host.getState(
                          self.apiclient,
                          host.id,
                          "Up",
                          "Enabled"
                          )

        if self.exceptionList:
            for i in self.exceptionList:
                raise(i)
        # Change the VMWare session timeout to 60s

    @attr(tags=["advanced", "basic", "vmware", "vmfs", "maint"], required_hardware="true")
    def test_06_vm_and_volumes_live_migration_for_vmware_host_maintenance(self):
        """
        Test scenarios for Host Maintenance
        1. Create 2 VMs on 1 Host and attach data disks to each of them
        2. Put the host on which VMs are created to maintenance mode
        3. Wait for the host to get to maintenance mode, the 2 VMs should be in running state
           and also check that systemvms are in running state.
        4. Now try to migrate VM1 to the host in maintenance mode, it should fail
        5. Cancel maintenance mode of the host
        6. Now attach a data disk to VM2 and migrate the VM and its volumes.
        7. Restore VM1 and migrate the vm and its volumes.
        8. Detach all data disks from VM1 and VM2, create VM3, attach all the data disks to VM3 and then migrate vm and its volumes

        After each storage migration step, following validation is done
        a) Create VM snapshots to check data integrity - @method used : VmSnapshotToCheckDataIntegrity(self,vm)
        b) Login to the Host/storage pool and check for the VMDK and VMX files for VM and its volumes - @method used : check_files(self, vm,destinationHost)
        c) Check for VM accessibility by sshing to the VM - @method used : check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        """
        storage_scope = "within_cluster"
        storage_type = "VMFS"
        # Deploy 2 Virtual Machines
        vm = "virtual_machine2"
        virtual_machine_1 = VirtualMachine.create(
        self.apiclient,
        self.testdata[vm],
        accountid=self.account.name,
        zoneid=self.zone.id,
        domainid=self.account.domainid,
        serviceofferingid=self.service_offering.id,
        templateid=self.template.id
        )
        self.cleanup.append(virtual_machine_1)
        virtual_machine_1.getState(
                                 self.apiclient,
                                 "Running"
                                 )
        vm = "virtual_machine3"
        virtual_machine_2 = VirtualMachine.create(
        self.apiclient,
        self.testdata[vm],
        accountid=self.account.name,
        zoneid=self.zone.id,
        domainid=self.account.domainid,
        serviceofferingid=self.service_offering.id,
        templateid=self.template.id,
        hostid = virtual_machine_1.hostid
        )
        self.cleanup.append(virtual_machine_2)
        virtual_machine_2.getState(
                                 self.apiclient,
                                 "Running"
                                 )
        # Create 2 data disks 
        data_disk_1 = Volume.create(
                                    self.apiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )
        data_disk_2 = Volume.create(
                                    self.apiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )
        # Attach disks to VMs
        virtual_machine_1.attach_volume(
                         self.apiclient,
                         data_disk_1
                         )
        virtual_machine_2.attach_volume(
                         self.apiclient,
                         data_disk_2
                         )
        # Enable Maintenenace state for the Host
        maintenance_host_id = virtual_machine_1.hostid
        Host.enableMaintenance(self.apiclient, id=maintenance_host_id)

        Host.getState(
                      self.apiclient,
                      maintenance_host_id,
                      "Up",
                      "Maintenance"
                      )
        # list VMs post migration
        list_vm_1 = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        list_vm_2 = list_virtual_machines(self.apiclient, id=virtual_machine_2.id, listall=True)[0]

        # check Status
        virtual_machine_2.getState(
                                 self.apiclient,
                                 "Running"
                                 )
        virtual_machine_1.getState(
                                 self.apiclient,
                                 "Running"
                                 )
        self.assertNotEqual(
                         maintenance_host_id,
                         list_vm_1.hostid,
                         "Virtual MAchine has not migrated"
                         )
        self.assertNotEqual(
                         maintenance_host_id,
                         list_vm_2.hostid,
                         "Virtual Machine has not migrated"
                         )
        # list systemvms and check for their status
        ssvm_list = list_ssvms(self.apiclient, listall=True)
        for ssvm in ssvm_list:
            self.get_vm_state(
                         self.apiclient,
                         ssvm.id,
                         "Running"
                         ) 

        # Try vMotion on virtual_machine_1 to the host which is in maintenance
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, list_vm_1, storage_scope, storage_type)
        try:
            with self.assertRaises(Exception):
                MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        except Exception as e:
            self.debug(" Migration failed as expected since the Host is in maintenance state as the exception says :  %s " %e)
        VmSnapshotToCheckDataIntegrity(self,list_vm_1)

        # Cancel Host maintenance state
        Host.cancelMaintenance(self.apiclient, id=maintenance_host_id)
        Host.getState(
                      self.apiclient,
                      maintenance_host_id,
                      "Up",
                      "Enabled"
                      )
        # Create another disk attach it to  virtual_machine_2 and migrate it.
        data_disk_3 = Volume.create(
                                    self.apiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )
        # Attach disks to VMs
        virtual_machine_2.attach_volume(
                         self.apiclient,
                         data_disk_3
                         )
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, list_vm_2, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self, vm)
        check_files(self, vm, destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)
        
        # Restore virtual_machine_1 and then migrate
        virtual_machine_1.restore(self.apiclient)
        virtual_machine_1.getState(
                                     self.apiclient,
                                     "Running"
                                     )
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, list_vm_1, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self, vm)
        check_files(self, vm, destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)

        self.testdata["virtual_machine3"]["name"] = "TestVM5"
        self.testdata["virtual_machine3"]["displayname"] = "TestVM5"
        virtual_machine_3 = VirtualMachine.create(
        self.apiclient,
        self.testdata["virtual_machine3"],
        accountid=self.account.name,
        zoneid=self.zone.id,
        domainid=self.account.domainid,
        serviceofferingid=self.service_offering.id,
        templateid=self.template.id
        )
        self.cleanup.append(virtual_machine_3)
        virtual_machine_3.getState(
                                 self.apiclient,
                                 "Running"
                                 )
        # detach the disks from VM_1 and VM_2, attach them to VM_3
        virtual_machine_2.detach_volume(
                         self.apiclient,
                         data_disk_2
                         )

        virtual_machine_1.detach_volume(
                         self.apiclient,
                         data_disk_1
                         )
        
        virtual_machine_3.attach_volume(
                         self.apiclient,
                         data_disk_2
                         )

        virtual_machine_3.attach_volume(
                         self.apiclient,
                         data_disk_1
                         )
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, virtual_machine_3, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_3, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self, vm)
        check_files(self, vm, destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_3)

        
    @attr(tags=["advanced", "basic", "vmware", "vmfs", "maint"], required_hardware="true")
    def test_07_vm_and_volumes_live_migration_for_vmware_storage_maintenance(self):
        """
        Test scenarios for storage Maintenance
        1. Create 2 VMs  and attach data disks to each of them
        2. Put the storage on which VMs are created to maintenance mode
        3. Wait for the storage to get to maintenance mode, the 2 VMs should be in stopped state
           and also check that systemvms are in running state.
        4. Now try to migrate a volume to the storage in maintenance mode, it should pass
        5. Cancel maintenance mode of the storage
        6. Start the VMs.
        7. Migrate both the VMs and their volumes
        8. Detach all data disks from VM1 and VM2, create VM3, attach all the data disks to VM3 and then migrate vm and its volumes

        After each storage migration step, following validation is done
        a) Create VM snapshots to check data integrity - @method used : VmSnapshotToCheckDataIntegrity(self,vm)
        b) Login to the Host/storage pool and check for the VMDK and VMX files for VM and its volumes - @method used : check_files(self, vm,destinationHost)
        c) Check for VM accessibility by sshing to the VM - @method used : check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        """
        storage_scope = "within_cluster"
        storage_type = "VMFS"
        # Deploy 2 Virtual Machines
        vm = "virtual_machine2"
        virtual_machine_1 = VirtualMachine.create(
        self.apiclient,
        self.testdata[vm],
        accountid=self.account.name,
        zoneid=self.zone.id,
        domainid=self.account.domainid,
        serviceofferingid=self.service_offering.id,
        templateid=self.template.id
        )
        self.cleanup.append(virtual_machine_1)
        virtual_machine_1.getState(
                                 self.apiclient,
                                 "Running"
                                 )
        vm = "virtual_machine3"
        virtual_machine_2 = VirtualMachine.create(
        self.apiclient,
        self.testdata[vm],
        accountid=self.account.name,
        zoneid=self.zone.id,
        domainid=self.account.domainid,
        serviceofferingid=self.service_offering.id,
        templateid=self.template.id,
        hostid = virtual_machine_1.hostid
        )
        self.cleanup.append(virtual_machine_2)
        virtual_machine_2.getState(
                                 self.apiclient,
                                 "Running"
                                 )
        # Create 2 data disks 
        data_disk_1 = Volume.create(
                                    self.apiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )
        data_disk_2 = Volume.create(
                                    self.apiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )
        # Attach disks to VMs
        virtual_machine_1.attach_volume(
                         self.apiclient,
                         data_disk_1
                         )
        virtual_machine_2.attach_volume(
                         self.apiclient,
                         data_disk_2
                         )

        """
        Storage maintenance
        """
        # Get the root volume of virtual_machine_1 and put it's pool in maintenance mode
        
        root_vol_1 = list_volumes(self.apiclient, virtualmachineid=virtual_machine_1.id, type="ROOT", listall=True)[0]
        maintenance_pool_id = root_vol_1.storageid
        # Enable maintenance mode for storage pool
        cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        cmd.id = maintenance_pool_id
        self.apiclient.enableStorageMaintenance(cmd)
        StoragePool.getState(
                             self.apiclient,
                             maintenance_pool_id,
                             "Maintenance"
                             )
        # When storage pool goes to maintenance state the VM should be stopped
        virtual_machine_1.getState(
                                     self.apiclient,
                                     "Stopped"
                                     )
                # list systemvms and check for their status
        ssvm_list = list_ssvms(self.apiclient, listall=True)
        for ssvm in ssvm_list:
            self.get_vm_state(
                         self.apiclient,
                         ssvm.id,
                         "Running"
                         )
        # Try to migrate the root volume of virtual_machine_2 to the pool in maintenance mode. It should succeed
        root_vol_2 = list_volumes(self.apiclient, virtualmachineid=virtual_machine_2.id, type="ROOT", listall=True)[0]
        if root_vol_2.storageid == maintenance_pool_id:
            virtual_machine_2.getState(
                                     self.apiclient,
                                     "Stopped"
                                     )
        else:
            
            destinationPool = list_storage_pools(self.apiclient, id=maintenance_pool_id, listall=True)[0]
            islive = True
            MigrateDataVolume(self, root_vol_2, destinationPool, islive)
        # cancel maintenance mode of the pool

        cmd = cancelStorageMaintenance.cancelStorageMaintenanceCmd()
        cmd.id = maintenance_pool_id
        self.apiclient.cancelStorageMaintenance(cmd)

        StoragePool.getState(
                             self.apiclient,
                             maintenance_pool_id,
                             "Up"
                             )
        # When storage pool comes out of maintenance state the VM should be started

        list_vm_1 = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        self.debug("...............................................Print state of the VM.....................%s............." %list_vm_1.state)

        if list_vm_1.state == "Stopped":
            virtual_machine_1.start(self.apiclient)

        virtual_machine_1.getState(
                                     self.apiclient,
                                     "Running"
                                     )

        # Again try vMotion of both VMs
        #list_vm_1 = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        list_vm_2 = list_virtual_machines(self.apiclient, id=virtual_machine_2.id, listall=True)[0]
        if list_vm_2.state == "Stopped":
            virtual_machine_2.start(self.apiclient)

        virtual_machine_2.getState(
                                     self.apiclient,
                                     "Running"
                                     )

        list_vm1 = list_virtual_machines(self.apiclient, id=virtual_machine_1.id, listall=True)[0]
        list_vm2 = list_virtual_machines(self.apiclient, id=virtual_machine_2.id, listall=True)[0]
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, list_vm1, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self, vm)
        check_files(self, vm, destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_1)
        
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, list_vm2, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_2, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self, vm)
        check_files(self, vm, destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_2)
        
        self.testdata["virtual_machine3"]["name"] = "TestVM"
        self.testdata["virtual_machine3"]["displayname"] = "TestVM"
        virtual_machine_3 = VirtualMachine.create(
        self.apiclient,
        self.testdata["virtual_machine3"],
        accountid=self.account.name,
        zoneid=self.zone.id,
        domainid=self.account.domainid,
        serviceofferingid=self.service_offering.id,
        templateid=self.template.id
        )
        self.cleanup.append(virtual_machine_3)
        virtual_machine_3.getState(
                                 self.apiclient,
                                 "Running"
                                 )
        # detach the disks from VM_1 and VM_2, attach them to VM_3
        virtual_machine_2.detach_volume(
                         self.apiclient,
                         data_disk_2
                         )

        virtual_machine_1.detach_volume(
                         self.apiclient,
                         data_disk_1
                         )

        virtual_machine_3.attach_volume(
                         self.apiclient,
                         data_disk_2
                         )

        virtual_machine_3.attach_volume(
                         self.apiclient,
                         data_disk_1
                         )
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, virtual_machine_3, storage_scope, storage_type)
        vm = MigrateVmWithVolume(self, virtual_machine_3, destinationHost, vol_list, destinationPools)
        VmSnapshotToCheckDataIntegrity(self, vm)
        check_files(self, vm, destinationHost)
        check_for_vm_access_by_ssh_using_nat(self,virtual_machine_3)
