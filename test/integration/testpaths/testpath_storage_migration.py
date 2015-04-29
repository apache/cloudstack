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
                              format_volume_to_ext3)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             DiskOffering,
                             Volume,
                             Template,
                             VirtualMachine,
                             StoragePool,
                             Snapshot
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_virtual_machines,
                               list_storage_pools,
                               list_clusters,
                               list_hosts,
                               validateList
                               )
from marvin.codes import (PASS,
                          ZONETAG1,
                          CLUSTERTAG1)

from marvin.cloudstackAPI import (deleteVolume)

import hashlib
from marvin.sshClient import SshClient
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
        cls.unsupportedHypervisor = False
        cls.insuffPools = False

        if cls.hypervisor.lower() not in [
                "vmware",
                "kvm",
                "xenserver",
                "hyper-v"]:
            cls.unsupportedHypervisor = True
            return
        try:
            cls.pools = StoragePool.list(cls.apiclient, zoneid=cls.zone.id)
        except Exception as e:
            cls.insuffPools = True
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
        if self.unsupportedHypervisor or self.insuffPools:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        self.cleanup = []

    def tearDown(self):
        try:
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="True")
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

    @attr(tags=["advanced", "basic"], required_hardware="True")
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

    @attr(tags=["advanced", "basic"], required_hardware="True")
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

    @attr(tags=["advanced", "basic"], required_hardware="True")
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
        cls.unsupportedHypervisor = False
        cls.insuffPools = False
        if cls.hypervisor.lower() not in [
                "vmware",
                "kvm",
                "xenserver",
                "hyper-v"]:
            cls.unsupportedHypervisor = True
            return

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
            cls.insuffPools = True
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
        if self.unsupportedHypervisor or self.insuffPools:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
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

    @attr(tags=["advanced", "basic"], required_hardware="True")
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
        cls.unsupportedHypervisor = False
        cls.insuffPools = False
        if cls.hypervisor.lower() in ["kvm", "lxc"]:
            cls.unsupportedHypervisor = True
            return

        try:
            cls.pools = StoragePool.list(cls.apiclient, zoneid=cls.zone.id)
        except Exception as e:
            cls.insuffPools = True
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
        if self.unsupportedHypervisor or self.insuffPools:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        self.cleanup = []

    def tearDown(self):
        try:
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"],
        required_hardware="True")
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
    @attr(tags=["advanced", "basic"], required_hardware="True")
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
        """
        #Take VM snapshot to check data integrity
        try :
            vm_snapshot = VmSnapshot.create(self.apiclient, vmid = migrated_vm_response[0].id)
        except Exception as e:
            raise Exception("Warning: Exception during VM snapshot creation : %s" % e)
        
        #Delete the snapshot
        try :
            VmSnapshot.deleteVMSnapshot(self.apiclient, vmsnapshotid = vm_snapshot.id)
        except Exception as e:
            raise Exception("Warning: Exception during VM snapshot creation : %s" % e)
        """

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

def get_destination_pools_hosts(self, vm):
    """
    Get destination Pools for all volumes and destination Host for the VM
    This method is use in case we use the API migrate volume with storage
    """

    destinationPools = []
    vol_list = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
    # For each volume get destination pool
    for vol in vol_list:
        pool = GetDestinationPool(self, vol.storage, "CLUSTER")
        destinationPools.append(pool)
        #Get destination host
    destinationHost = self.GetDestinationHost(vm.hostid)
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
    print vm_volumes
    for vol in vm_volumes:
        spool  =  list_storage_pools(self.apiclient, id=vol.storageid)
        split_path = spool[0].path.split("/")
        pool_path = split_path[2]
        sshclient = SshClient(host = destinationHost.ipaddress, port = 22, user = "root", passwd = "freebsd")
        pool_data_vmdk = sshclient.execute("ls /vmfs/volumes/" + pool_path + "/" + vm.instancename +  "| grep vmdk")
        pool_data_vmx = sshclient.execute("ls /vmfs/volumes/" + pool_path + "/" + vm.instancename +  "| grep vmx")
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


class TestStorageLiveMigrationVmware(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestStorageLiveMigrationVmware, cls).getClsTestClient()
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
        cls.hypervisorNotSupported = False
        cls.NoResource = False
        if cls.hypervisor.lower() not in [
                "vmware"]:
            cls.hypervisorNotSupported = True
        # Get Hosts in the cluster and iscsi/vmfs storages for that cluster
        iscsi_pools = []
        try :
            list_vmware_clusters = list_clusters(cls.apiclient, hypervisor="vmware")
        except Exception as e:
            cls.NoResource = True
            return
        assert validateList(list_vmware_clusters)[0] == PASS
        if len(list_vmware_clusters) < 1 :
            cls.NoResource = True
            return
        else :
            for cluster in list_vmware_clusters :
                try:
                    list_esx_hosts = list_hosts(cls.apiclient, clusterid = cluster.id)
                except Exception as e:
                    cls.NoResource = True
                    return
                assert validateList(list_esx_hosts)[0] == PASS
                if len(list_esx_hosts) > 1 :
                    try:
                        list_storage = list_storage_pools(cls.apiclient, clusterid = cluster.id)
                    except Exception as e:
                        cls.NoResource = True
                        return
                    assert validateList(list_storage)[0] == PASS
                    for storage in list_storage :
                        if storage.type == "VMFS" :
                            iscsi_pools.append(storage)
                    if len(iscsi_pools) > 1:
                        my_cluster_id = cluster.id
                        break
                    else :
                        iscsi_pools = []
        if len(iscsi_pools) < 2 :
            cls.NoResource = True
            return
        cls.hosts = list_esx_hosts
        cls.pools = list_storage

        # Create an account
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        # Create Service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

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
        if self.unsupportedHypervisor or self.NoResource:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        self.cleanup = []

    def tearDown(self):
        try:
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def deploy_virtual_machine(self, service_offering_id, vm):
        """
        Function to Deploy VMs
        """
        virtual_machine = VirtualMachine.create(
        self.apiclient,
        self.testdata[vm],
        accountid=self.account.name,
        zoneid=self.zone.id,
        domainid=self.account.domainid,
        serviceofferingid=service_offering_id,
        templateid=self.template.id,
        )
        virtual_machine.getState(
                                 self.apiclient,
                                 "Running"
                                 )

        return virtual_machine
    
    def GetDestinationHost(self, hostsToavoid):
        """
        This method gives us the destination host to which VM will be migrated
        It takes the souce host i.e. hostsToavoid as input
        """
        destinationHost = None
        for host in self.hosts:
            if host.id not in hostsToavoid:
                destinationHost = host
                break
        return destinationHost
        

    @attr(tags=["advanced", "basic", "vmware", "vmfs"], required_hardware="True")
    def test_01_migrate_root_and_data_disk_live(self):
        """
        Migrate VMs/Volumes on VMware with VMFS storage
        """
        #List clusters and check if they have multiple hosts
        #and multiple storages
        #check if they storages are VMFS type
        self.debug("---------------This is the test no 1--------------")
        """
        Create a VM, live migrate the VM
        """
        vm = "virtual_machine2"
        virtual_machine_1 = self.deploy_virtual_machine(self.service_offering.id, vm)

        #Get destination host
        destinationHost = self.GetDestinationHost(virtual_machine_1.hostid)
        #Migrate the VM
        vm = MigrateVm(self, virtual_machine_1, destinationHost)
        #self.check_files(vm,destinationHost)
        
        self.debug("---------------This is the test no 2--------------")
        """
        Migrate the ROOT Volume
        """
        # Get ROOT volume and destination pool
        vol_list = list_volumes(self.apiclient, virtualmachineid=vm.id, type="ROOT", listall=True)
        root_vol = vol_list[0]
        destinationPool = GetDestinationPool(self, root_vol.storage, "CLUSTER")
        #Migrate ROOT volume
        islive = True
        MigrateDataVolume(self, root_vol, destinationPool, islive)
        check_files(self, vm ,destinationHost)
        
        self.debug("---------------This is the test no 3--------------")
        """
        Migrate the VM and ROOT volume
        """
        #Get all volumes to be migrated
 
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)

        check_files(self, vm,destinationHost)

        self.debug("---------------This is the test no 4--------------")
        """
        Add a data disk and migrate vm, data disk and root disk
        """

        data_disk_1 = Volume.create(
                                    self.apiclient,
                                    self.testdata["volume"],
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    diskofferingid=self.disk_offering.id
                                  )

        self.debug("Created volume with ID: %s" % data_disk_1.id)

        virtual_machine_1.attach_volume(
                         self.apiclient,
                         data_disk_1
                         )

        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)

        check_files(self, vm,destinationHost)
        self.debug("---------------This is the test no 5--------------")
        """
        Upload a Volume, Attach it to the VM, Migrate all the volumes and VM.
        """

                #upload a volume
        self.testdata["configurableData"]["upload_volume"]["format"] = "OVA"
        self.testdata["configurableData"]["upload_volume"]["url"] = "http://nfs1.lab.vmops.com/templates/burbank-systemvm-08012012.ova"
        upload_volume = Volume.upload(
                                      self.apiclient,
                                      self.testdata["configurableData"]["upload_volume"],
                                      account= self.account.name,
                                      domainid= self.domain.id,
                                      zoneid= self.zone.id
                                      )
        upload_volume.wait_for_upload(self.apiclient)
        virtual_machine_1.attach_volume(
                         self.apiclient,
                         upload_volume
                         )
 
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)

        check_files(self, vm,destinationHost)
        
        self.debug("---------------This is the test no 6--------------")
        """
        Create snapshots on all the volumes, Migrate all the volumes and VM.
        """
        #Get ROOT Volume
        vol_for_snap = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        for vol in vol_for_snap:
            snapshot = Snapshot.create(
                                        self.apiclient,
                                        volume_id = vol.id
                                        )
            snapshot.validateState(
                                   self.apiclient, 
                                    snapshotstate="backedup", 
                                    )
        # Migrate all volumes and VMs

        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)

        check_files(self, vm, destinationHost)

        self.debug("---------------This is the test no 7--------------")
        """
        Resize the data volume , Migrate all the volumes and VM.
        """
        data_disk_1.resize(
                           self.apiclient,
                           diskofferingid = self.resized_disk_offering.id
                           )
        # Migrate all volumes and VMs
        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)

        check_files(self, vm,destinationHost)

        self.debug("---------------This is the test no 8--------------")
        """
        Restore the VM , Migrate all the volumes and VM.
        """
        virtual_machine_1.restore(self.apiclient)
        virtual_machine_1.getState(
                                     self.apiclient,
                                     "Running"
                                     )
        # Migrate the VM and its volumes

        destinationHost, destinationPools, vol_list = get_destination_pools_hosts(self, vm)
        vm = MigrateVmWithVolume(self, virtual_machine_1, destinationHost, vol_list, destinationPools)

        check_files(self, vm,destinationHost)
