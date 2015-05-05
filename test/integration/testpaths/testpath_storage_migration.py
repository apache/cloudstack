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
from marvin.lib.utils import (cleanup_resources)
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
                               createChecksum,
                               compareChecksum
                               )
from marvin.codes import (ZONETAG1,
                          CLUSTERTAG1)

from marvin.cloudstackAPI import (deleteVolume)

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

            cls.new_virtual_machine = VirtualMachine.create(
                cls.apiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
                mode=cls.zone.networktype
            )

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
        For root disk, create template from snapshot, 
                deploy Vm and compare checksum
        For data disk, Create volume from snapshot, 
                attach to VM and compare checksum

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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=root_volume_cluster,
            disk_type="rootdiskdevice")

        vm_cluster.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self,
            [root_volume_cluster.storage],
            "CLUSTER")
        MigrateRootVolume(self, vm_cluster, destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_cluster
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
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_cluster
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_volumes_cluster_list[0],
            disk_type="datadiskdevice_1")

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

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_cluster_list[0])

        self.new_virtual_machine.reboot(self.apiclient)
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_disk_2_volumes_cluster_list[0],
            disk_type="datadiskdevice_2")

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
            service=self.testdata,
            virtual_machine=vm_from_temp,
            disk=root_volume_snap,
            disk_type="rootdiskdevice")

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
            service=self.testdata,
            virtual_machine=vm_from_temp,
            disk=data_from_snap[0],
            disk_type="datadiskdevice_1")

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

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.delete(self.apiclient)

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
            service=self.testdata,
            virtual_machine=vm_zone,
            disk=data_volumes_zone_list[0],
            disk_type="rootdiskdevice")

        vm_zone.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self, [
                root_volume_zone.storage], "ZONE")
        MigrateRootVolume(self, vm_zone, destinationPool)

        vm_zone.start(self.userapiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_zone,
            disk_type="rootdiskdevice",
            virt_machine=vm_zone
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
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_zone,
            disk_type="rootdiskdevice",
            virt_machine=vm_zone
        )

        checksum_random_data_zone = createChecksum(
            service=self.testdata,
            virtual_machine=vm_zone,
            disk=data_volumes_zone_list[0],
            disk_type="datadiskdevice_1")

        vm_zone.stop(self.userapiclient)

        # Migrate DATA Volume from ZWPS to other ZWPS
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_zone_list[0].storage],
            "ZONE")
        MigrateDataVolume(self, data_volumes_zone_list[0], destinationPool)

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_zone_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_zone,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_zone_list[0])

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

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_zone_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_zone,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_zone_list[0])

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
                service=self.testdata,
                virtual_machine=vm_local,
                disk=data_volumes_local_list[0],
                disk_type="rootdiskdevice")

            vm_local.stop(self.userapiclient)

            destinationPool = GetDestinationPool(
                self, [
                    root_volume_local.storage], "HOST")
            MigrateRootVolume(self, vm_local, destinationPool)

            vm_local.start(self.userapiclient)

            compareChecksum(
                self.apiclient,
                service=self.testdata,
                original_checksum=checksum_random_root_local,
                disk_type="rootdiskdevice",
                virt_machine=vm_cluster
            )

            checksum_random_data_local = createChecksum(
                service=self.testdata,
                virtual_machine=vm_local,
                disk=data_volumes_local_list[0],
                disk_type="datadiskdevice_1")

            vm_local.stop(self.userapiclient)
            destinationPool = GetDestinationPool(
                self,
                [data_volumes_local_list[0].storage],
                "HOST")
            MigrateDataVolume(
                self,
                data_volumes_local_list[0],
                destinationPool)

            self.new_virtual_machine.attach_volume(
                self.apiclient,
                data_volumes_local_list[0]
            )

            # Rebooting is required so that newly attached disks are detected
            self.new_virtual_machine.reboot(self.apiclient)

            vm_local.start(self.userapiclient)
            compareChecksum(
                self.apiclient,
                service=self.testdata,
                original_checksum=checksum_random_data_local,
                disk_type="datadiskdevice_1",
                virt_machine=self.new_virtual_machine
            )

            self.new_virtual_machine.detach_volume(
                self.apiclient,
                data_volumes_local_list[0])

            self.new_virtual_machine.reboot(self.apiclient)
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=root_volume_cluster,
            disk_type="rootdiskdevice")

        vm_cluster.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self,
            [root_volume_cluster.storage],
            "CLUSTER")
        MigrateRootVolume(self, vm_cluster, destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_cluster
        )
        self.debug("Done with compare checksum after first checksum")

        vm_cluster.start(self.userapiclient)

        vm_cluster.attach_volume(
            self.userapiclient,
            data_volume_clust
        )

        vm_cluster.reboot(self.userapiclient)

        checksum_random_data_cluster = createChecksum(
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_volumes_cluster_list[0],
            disk_type="datadiskdevice_1")

        vm_cluster.stop(self.userapiclient)

        # Migrate DATA Volume from CWPS to other CWPS
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_cluster_list[0].storage],
            "CLUSTER")

        MigrateDataVolume(self, data_volumes_cluster_list[0], destinationPool)

        vm_cluster.start(self.userapiclient)

        vm_cluster.detach_volume(self.apiclient, data_volumes_cluster_list[0])

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_cluster_list[0])

        self.new_virtual_machine.reboot(self.apiclient)

        # snapshot test case t14 compare checksum for same VM
        vm_cluster.attach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        vm_cluster.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_1",
            virt_machine=vm_cluster
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_disk_2_volumes_cluster_list[0],
            disk_type="datadiskdevice_2")

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
            service=self.testdata,
            virtual_machine=vm_from_temp,
            disk=root_volume_snap,
            disk_type="rootdiskdevice")

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
            service=self.testdata,
            virtual_machine=vm_from_temp,
            disk=data_from_snap[0],
            disk_type="datadiskdevice_1")

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
                service=self.testdata,
                virtual_machine=vm_local,
                disk=root_volume_local,
                disk_type="rootdiskdevice")

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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=root_volume_cluster,
            disk_type="rootdiskdevice")

        vm_cluster.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self,
            [root_volume_cluster.storage],
            "CLUSTER")
        MigrateRootVolume(self, vm_cluster, destinationPool)

        vm_cluster.start(self.userapiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_cluster
        )
        self.debug("Done with compare checksum")

        vm_cluster.start(self.userapiclient)
        checksum_random_data_cluster = createChecksum(
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_volumes_cluster_list[0],
            disk_type="datadiskdevice_1")

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

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_cluster_list[0])

        self.new_virtual_machine.reboot(self.apiclient)
        # snapshot test case t14 compare checksum for same VM
        vm_cluster.attach_volume(
            self.apiclient,
            data_volumes_cluster_list[0]
        )

        vm_cluster.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_1",
            virt_machine=vm_cluster
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_disk_2_volumes_cluster_list[0],
            disk_type="datadiskdevice_2")

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
            service=self.testdata,
            virtual_machine=vm_from_temp,
            disk=root_volume_snap,
            disk_type="rootdiskdevice")

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
            service=self.testdata,
            virtual_machine=vm_from_temp,
            disk=data_from_snap[0],
            disk_type="datadiskdevice_1")

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
                service=self.testdata,
                virtual_machine=vm_local,
                disk=data_volumes_local_list[0],
                disk_type="rootdiskdevice")

            vm_local.stop(self.userapiclient)

            destinationPool = GetDestinationPool(
                self, [
                    root_volume_local.storage], "HOST")
            MigrateRootVolume(self, vm_local, destinationPool)

            vm_local.start(self.userapiclient)

            compareChecksum(
                self.apiclient,
                service=self.testdata,
                original_checksum=checksum_random_root_local,
                disk_type="rootdiskdevice",
                virt_machine=vm_cluster
            )

            checksum_random_data_local = createChecksum(
                service=self.testdata,
                virtual_machine=vm_local,
                disk=data_volumes_local_list[0],
                disk_type="datadiskdevice_1")

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

            self.new_virtual_machine.attach_volume(
                self.apiclient,
                data_volumes_local_list[0]
            )

            # Rebooting is required so that newly attached disks are detected
            self.new_virtual_machine.reboot(self.apiclient)

            compareChecksum(
                self.apiclient,
                service=self.testdata,
                original_checksum=checksum_random_data_local,
                disk_type="datadiskdevice_1",
                virt_machine=self.new_virtual_machine
            )

            self.new_virtual_machine.detach_volume(
                self.apiclient,
                data_volumes_local_list[0])

            self.new_virtual_machine.reboot(self.apiclient)
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
            service=self.testdata,
            virtual_machine=vm_zone,
            disk=data_volumes_zone_list[0],
            disk_type="rootdiskdevice")

        vm_zone.stop(self.userapiclient)

        destinationPool = GetDestinationPool(
            self, [
                root_volume_zone.storage], "ZONE")
        MigrateRootVolume(self, vm_zone, destinationPool)

        vm_zone.start(self.userapiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_zone,
            disk_type="rootdiskdevice",
            virt_machine=vm_zone
        )

        checksum_random_data_zone = createChecksum(
            service=self.testdata,
            virtual_machine=vm_zone,
            disk=data_volumes_zone_list[0],
            disk_type="datadiskdevice_1")

        vm_zone.stop(self.userapiclient)

        # Migrate DATA Volume from ZWPS to other ZWPS
        destinationPool = GetDestinationPool(
            self,
            [data_volumes_zone_list[0].storage],
            "ZONE")
        MigrateDataVolume(self, data_volumes_zone_list[0], destinationPool)

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_zone_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_zone,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_zone_list[0])

        self.new_virtual_machine.reboot(self.apiclient)
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
        # 4. While migration(ROOT disk) is in progress try following scenarios, 
                they should fail:
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=root_volume_cluster,
            disk_type="rootdiskdevice")

        checksum_data_disk = self.createChecksum(
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_disk,
            disk_type="datadiskdevice_1")

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
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_root_disk,
            disk_type="rootdiskdevice",
            virt_machine=vm_cluster
        )

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_data_disk,
            disk_type="datadiskdevice_1",
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
        # 2. Migrate root and data volume to two different storage pools
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=root_volume_cluster,
            disk_type="rootdiskdevice")

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
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_cluster
        )

        # Migrate DATA Volume from CWPS to other CWPS
        checksum_random_data_cluster = createChecksum(
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_volume_1_cluster,
            disk_type="datadiskdevice_1")

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

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volume_1_cluster
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volume_1_cluster)

        self.new_virtual_machine.reboot(self.apiclient)
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

    @attr(tags=["advanced", "basic"])
    def test_02_migration_live_different_pods(self):
        """ Test migrate Volume (root and data disk)

        # 1. Deploy a VM on cluster wide primary storage.
        # 2. Migrate root and data volume to two different storage pools
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=root_volume_cluster,
            disk_type="rootdiskdevice")

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
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_cluster
        )

        # Migrate DATA Volume from CWPS to other CWPS
        checksum_random_data_cluster = createChecksum(
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_volume_1_cluster,
            disk_type="datadiskdevice_1")

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

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volume_1_cluster
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volume_1_cluster)

        self.new_virtual_machine.reboot(self.apiclient)
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
            service=self.testdata,
            virtual_machine=vm_cluster,
            disk=data_volume_2_cluster,
            disk_type="datadiskdevice_2")

        # TO-DO Migration
        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volume_2_cluster
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_cluster,
            disk_type="datadiskdevice_2",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volume_2_cluster)

        self.new_virtual_machine.reboot(self.apiclient)

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
            service=self.testdata,
            virtual_machine=vm_zone,
            disk=root_volume_zone,
            disk_type="rootdiskdevice")

        destinationPool = GetDestinationPool(self, root_volume_zone, "ZONE")
        MigrateRootVolume(self, vm_zone, destinationPool)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_zone,
            disk_type="rootdiskdevice",
            virt_machine=vm_zone
        )

        # Try to Migrate ROOT Volume from ZWPS to Cluster wide Storage

        destinationPool = GetDestinationPool(self, root_volume_zone, "CLUSTER")
        MigrateRootVolume(
            self,
            vm_zone,
            destinationPool,
            expectexception=True)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_root_zone,
            disk_type="rootdiskdevice",
            virt_machine=vm_cluster
        )

        # DATA Disk

        checksum_random_data_zone = createChecksum(
            service=self.testdata,
            virtual_machine=vm_zone,
            disk=data_volumes_zone_list[0],
            disk_type="datadiskdevice_1")

        # Migrate DATA Volume from ZWPS to other ZWPS
        destinationPool = GetDestinationPool(
            self,
            data_volumes_zone_list[0],
            "ZONE")
        MigrateDataVolume(self, data_volumes_zone_list[0], destinationPool)

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_zone_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_zone,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_zone_list[0])

        self.new_virtual_machine.reboot(self.apiclient)

        # Try to Migrate DATA Volume from ZWPS to Cluster wide Storage
        destinationPool = GetDestinationPool(self, data_volume_zone, "CLUSTER")
        MigrateDataVolume(
            self,
            data_volume_zone,
            destinationPool,
            expectexception=True)

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_zone_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)
        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_zone,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_zone_list[0])

        self.new_virtual_machine.reboot(self.apiclient)

        # Try to Migrate DATA Volume from ZWPS to Cluster wide Storage
        destinationPool = GetDestinationPool(self, data_volume_zone, "CLUSTER")
        MigrateDataVolume(
            self,
            data_volume_zone,
            destinationPool,
            expectexception=True)

        self.new_virtual_machine.attach_volume(
            self.apiclient,
            data_volumes_zone_list[0]
        )

        # Rebooting is required so that newly attached disks are detected
        self.new_virtual_machine.reboot(self.apiclient)
        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_random_data_zone,
            disk_type="datadiskdevice_1",
            virt_machine=self.new_virtual_machine
        )

        self.new_virtual_machine.detach_volume(
            self.apiclient,
            data_volumes_zone_list[0])

        self.new_virtual_machine.reboot(self.apiclient)

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
