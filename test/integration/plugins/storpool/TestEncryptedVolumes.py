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

# Import Local Modules
from marvin.codes import FAILED, KVM, PASS, XEN_SERVER, RUNNING
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual
from marvin.lib.base import (Account,
                             Cluster,
                             Configurations,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             Template,
                             VirtualMachine,
                             VmSnapshot,
                             Volume,
                             SecurityGroup,
                             Role,
                             DiskOffering,
                             )
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_disk_offering,
                               list_hosts,
                               list_snapshots,
                               list_storage_pools,
                               list_volumes,
                               list_virtual_machines,
                               list_configurations,
                               list_service_offering,
                               list_clusters,
                               list_zones)
from marvin.cloudstackAPI import (listOsTypes,
                                  listTemplates,
                                  listHosts,
                                  createTemplate,
                                  createVolume,
                                  getVolumeSnapshotDetails,
                                  resizeVolume,
                                  listZones,
                                  migrateVirtualMachine,
                                  findHostsForMigration,
                                  revertSnapshot,
                                  deleteSnapshot,
                                  detachVolume)
from marvin.sshClient import SshClient

import math
import time
import pprint
import random
import subprocess
from storpool import spapi
from storpool import sptypes
import unittest

import uuid
from sp_util import (TestData, StorPoolHelper)

# space added on the StorPool volume for the LUKS header of encrypted volumes
LUKS_HEADER_SIZE = 2097152
GiB = 1024 ** 3

class TestEncryptedVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestEncryptedVolumes, cls).setUpClass()
        try:
            cls.setUpCloudStack()
        except Exception:
            try:
                cls.cleanUpCloudStack()
            except Exception as e:
                cls.debug("Cleanup after failed setup: %s" % e)
            raise

    @classmethod
    def setUpCloudStack(cls):
        testClient = super(TestEncryptedVolumes, cls).getClsTestClient()

        cls._cleanup = []

        config = cls.getClsConfig()
        StorPoolHelper.logger = cls
        cls.logger = StorPoolHelper.logger

        cls.apiclient = testClient.getApiClient()

        zone = config.zones[0]
        assert zone is not None

        storage_pools = zone.primaryStorages
        sp_pools = []
        for storage in storage_pools:
            if storage['provider'] and "StorPool" in storage['provider']:
                sp_pools.append(storage)

        if len(sp_pools) < 2:
            cls.debug("Cannot perform the tests because there aren't the required count of StorPool storage pools %s" % sp_pools)
            return

        cls.zone = list_zones(cls.apiclient, name=zone.name)[0]

        cls.spapi = spapi.Api(host=zone.spEndpoint, port=zone.spEndpointPort, auth=zone.spAuthToken, multiCluster=True)
        cls.helper = StorPoolHelper()

        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ("hyperv", "lxc"):
            cls.unsupportedHypervisor = True
            return

        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)

        td = TestData()
        cls.testdata = td.testdata

        storpool_primary_storage = sp_pools[0]
        cls.template_name = storpool_primary_storage["name"]

        storage_pool = list_storage_pools(
            cls.apiclient,
            name=cls.template_name
            )
        if storage_pool is None:
            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage)
        else:
            storage_pool = storage_pool[0]
        cls.storage_pool = storage_pool
        cls.helper.updateStoragePoolTags(cls.apiclient, cls.storage_pool.id, cls.testdata[TestData.sp_template_1]["tags"])

        cls.debug(pprint.pformat(storage_pool))

        cls.primary_storage = storage_pool

        storpool_primary_storage = sp_pools[1]
        cls.template_name = storpool_primary_storage["name"]

        storage_pool = list_storage_pools(
            cls.apiclient,
            name=cls.template_name
            )
        if storage_pool is None:
            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage)
        else:
            storage_pool = storage_pool[0]
        cls.storage_pool = storage_pool
        cls.helper.updateStoragePoolTags(cls.apiclient, cls.storage_pool.id, cls.testdata[TestData.sp_template_2]["tags"])

        cls.debug(pprint.pformat(storage_pool))

        cls.primary_storage2 = storage_pool

        storpool_service_offerings_ssd = cls.testdata[TestData.serviceOfferingEncrypted]

        service_offerings_ssd = list_service_offering(
            cls.apiclient,
            name=storpool_service_offerings_ssd["name"]
        )

        if service_offerings_ssd is None:
            service_offerings_ssd = ServiceOffering.create(cls.apiclient, storpool_service_offerings_ssd, encryptroot=True)
        else:
            service_offerings_ssd = service_offerings_ssd[0]

        cls.service_offering = service_offerings_ssd
        cls.debug(pprint.pformat(cls.service_offering))

        cls.disk_offering_ssd_encrypted = list_disk_offering(
            cls.apiclient,
            name=cls.testdata[TestData.diskOfferingEncrypted]["name"]
        )

        if cls.disk_offering_ssd_encrypted is None:
            cls.disk_offering_ssd_encrypted = DiskOffering.create(cls.apiclient, cls.testdata[TestData.diskOfferingEncrypted], encrypt=True)
        else:
            cls.disk_offering_ssd_encrypted = cls.disk_offering_ssd_encrypted[0]

        cls.disk_offering_encrypted_custom = list_disk_offering(
            cls.apiclient,
            name=cls.testdata[TestData.diskOfferingEncryptedCustom]["name"]
        )

        if cls.disk_offering_encrypted_custom is None:
            cls.disk_offering_encrypted_custom = DiskOffering.create(cls.apiclient, cls.testdata[TestData.diskOfferingEncryptedCustom], custom=True, encrypt=True)
        else:
            cls.disk_offering_encrypted_custom = cls.disk_offering_encrypted_custom[0]

        template = get_template(
            cls.apiclient,
            cls.zone.id,
            account="system"
        )

        if template == FAILED:
            assert False, "get_template() failed to return template\
                     with description %s" % cls.services["ostype"]

        cls.root_disk_size = int(math.ceil(template.size / GiB))

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        role = Role.list(cls.apiclient, name='Root Admin')

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id,
            roleid= 1
        )

        securitygroup = SecurityGroup.list(cls.apiclient, account=cls.account.name, domainid=cls.account.domainid)[0]
        cls.helper.set_securityGroups(cls.apiclient, account=cls.account.name, domainid=cls.account.domainid,
                                      id=securitygroup.id)
        cls._cleanup.append(cls.account)

        cls.volume_1 = Volume.create(
            cls.apiclient,
            {"diskname": "StorPoolEncryptedDiskLiveMigrate"},
            zoneid=cls.zone.id,
            diskofferingid=cls.disk_offering_ssd_encrypted.id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )
        cls._cleanup.append(cls.volume_1)

        cls.volume_2 = Volume.create(
            cls.apiclient,
            {"diskname": "StorPoolEncryptedDiskVMSnapshot"},
            zoneid=cls.zone.id,
            diskofferingid=cls.disk_offering_ssd_encrypted.id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )
        cls._cleanup.append(cls.volume_2)

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            {"name": "StorPool-LiveMigrate-VM%s" % uuid.uuid4()},
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=cls.root_disk_size
        )
        cls._cleanup.append(cls.virtual_machine)

        cls.virtual_machine2 = VirtualMachine.create(
            cls.apiclient,
            {"name": "StorPool-VMSnapshots%s" % uuid.uuid4()},
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=cls.root_disk_size
        )
        cls._cleanup.append(cls.virtual_machine2)

        cls.virtual_machine3 = VirtualMachine.create(
            cls.apiclient,
            {"name": "StorPool-VolumeSnapshots%s" % uuid.uuid4()},
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=cls.root_disk_size
        )
        cls._cleanup.append(cls.virtual_machine3)

        cls.template = template
        cls.hostid = cls.virtual_machine.hostid
        cls.random_data_0 = random_gen(size=100)
        cls.test_dir = "/tmp"
        cls.random_data = "random.data"
        return

    @classmethod
    def tearDownClass(cls):
        cls.cleanUpCloudStack()

    @classmethod
    def cleanUpCloudStack(cls):
        errors = []
        for obj in reversed(getattr(cls, "_cleanup", [])):
            try:
                cls.delete_object(obj)
            except Exception as e:
                cls.debug("Could not clean up %s %s: %s" % (obj.__class__.__name__, obj.id, e))
                errors.append("%s %s: %s" % (obj.__class__.__name__, obj.id, e))
        if errors:
            raise Exception("Warning: Exceptions during cleanup: %s" % "; ".join(errors))

    @classmethod
    def delete_object(cls, obj):
        if isinstance(obj, Volume):
            volume = list_volumes(cls.apiclient, id=obj.id, listall=True)
            if volume is None:
                return
            if getattr(volume[0], "virtualmachineid", None):
                cmd = detachVolume.detachVolumeCmd()
                cmd.id = obj.id
                cls.apiclient.detachVolume(cmd)
        obj.delete(cls.apiclient)

    def forget_object(self, id):
        '''
            Removes an object deleted by a test from the cleanup list
        '''
        self.__class__._cleanup = [o for o in self._cleanup if o.id != id]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                     %s" % self.hypervisor)
        return

    def tearDown(self):
        return


# live migrate VM with encrypted volumes to another host
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_live_migrate_vm(self):
        '''
        Live Migrate VM to another host with encrypted volumes
        '''
        self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume_1
            )

        volumes = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        )

        vm_host = list_hosts(self.apiclient, id=self.virtual_machine.hostid)[0]
        self.logger.debug(vm_host)
        # sshc = SshClient(
        #     host=vm_host.name,
        #     port=22,
        #     user=None,
        #     passwd=None)
        #
        # for volume in volumes:
        #     cmd = 'blkid %s' % volume.path
        #     result = sshc.execute(cmd)
        #     if "LUKS" not in result:
        #         self.fail("The volume isn't encrypted %s" % volume)


        dest_host_cmd = findHostsForMigration.findHostsForMigrationCmd()
        dest_host_cmd.virtualmachineid = self.virtual_machine.id
        host = self.apiclient.findHostsForMigration(dest_host_cmd)[0]

        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.hostid = host.id
        self.apiclient.migrateVirtualMachine(cmd)

# VM snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_vm_snapshot(self):
        self.virtual_machine2.attach_volume(
            self.apiclient,
            self.volume_2
            )

        try:
            ssh_client = self.virtual_machine2.get_ssh_client(reconnect=True)

            cmds = [
                "echo %s > %s/%s" %
                (self.random_data_0, self.test_dir, self.random_data),
                "sync",
                "sleep 1",
                "sync",
                "sleep 1",
                "cat %s/%s" %
                (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)
        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine2.ipaddress)
        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data has be write into temp file!"
        )

        time.sleep(30)
        MemorySnapshot = False
        vm_snapshot = VmSnapshot.create(
            self.apiclient,
            self.virtual_machine2.id,
            MemorySnapshot,
            "TestSnapshot",
            "Display Text"
        )
        self._cleanup.append(vm_snapshot)
        self.assertEqual(
            vm_snapshot.state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

# Revert VM snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_revert_vm_snapshots(self):
        """Test to revert VM snapshots
        """

        try:
            ssh_client = self.virtual_machine2.get_ssh_client(reconnect=True)

            cmds = [
                "rm -rf %s/%s" % (self.test_dir, self.random_data),
                "ls %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine2.ipaddress)

        if str(result[0]).index("No such file or directory") == -1:
            self.fail("Check the random data has be delete from temp file!")

        time.sleep(30)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine2.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )

        self.assertEqual(
            list_snapshot_response[0].state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

        self.virtual_machine2.stop(self.apiclient, forced=True)

        VmSnapshot.revertToSnapshot(
            self.apiclient,
            list_snapshot_response[0].id
            )

        self.virtual_machine2.start(self.apiclient)

        try:
            ssh_client = self.virtual_machine2.get_ssh_client(reconnect=True)

            cmds = [
                "cat %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine2.ipaddress)

        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data is equal with the ramdom file!"
        )

    # Delete VM snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_delete_vm_snapshots(self):
        """Test to delete vm snapshots
        """

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine2.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )
        VmSnapshot.deleteVMSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)
        self.forget_object(list_snapshot_response[0].id)

        time.sleep(30)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            #vmid=self.virtual_machine.id,
            virtualmachineid=self.virtual_machine2.id,
            listall=False)
        self.debug('list_snapshot_response -------------------- %s' % list_snapshot_response)

        self.assertIsNone(list_snapshot_response, "snapshot is already deleted")

# Take volume snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_05_snapshot_volume_with_secondary(self):
        '''
            Test Create snapshot and backup to secondary
        '''
        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "false")
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine3.id,
                        type = "ROOT",
                        listall = True,
                        )

        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        self._cleanup.append(snapshot)
        self.assertIsNotNone(snapshot, "Could not create snapshot")
        self.assertEqual("BackedUp", snapshot.state, "Snapshot %s is in state %s" % (snapshot.id, snapshot.state))

        snapshot_stores = list_snapshots(
            self.apiclient,
            id = snapshot.id,
            showunique = False,
            listall = True,
        )
        self.assertIsNotNone(snapshot_stores, "Could not list the storages of snapshot %s" % snapshot.id)
        self.assertTrue(any(s.datastoretype == "Image" and s.datastorestate == "Ready" for s in snapshot_stores),
            "Snapshot %s is not copied to secondary storage, it is on %s" % (snapshot.id,
                [(s.datastorename, s.datastoretype, s.datastorestate) for s in snapshot_stores]))


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_06_snapshot_volume_on_primary(self):
        '''
            Test Create snapshot and backup to secondary
        '''
        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "true")
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine3.id,
                        type = "ROOT",
                        listall = True,
                        )
        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        self._cleanup.append(snapshot)
        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapshot_details")
        except spapi.ApiError as err:
            raise Exception(err)
        self.assertIsNotNone(snapshot, "Could not create snapshot")
# Rever Volume snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_07_revert_volume_on_primary(self):
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine3.id,
                        type = "ROOT",
                        listall = True,
                        )[0]
        snapshot = list_snapshots(
            self.apiclient,
            volumeid = volume.id,
            listall=True
        )[0]
        self.virtual_machine3.stop(self.apiclient, forced=True)

        cmd = revertSnapshot.revertSnapshotCmd()
        cmd.id = snapshot.id
        revertcmd = self.apiclient.revertSnapshot(cmd)

# Delete volume snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_08_delete_volume_on_primary(self):
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine3.id,
                        type = "ROOT",
                        listall = True,
                        )[0]
        snapshot = list_snapshots(
            self.apiclient,
            volumeid = volume.id,
            listall=True
        )[0]
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = snapshot.id
        self.apiclient.deleteSnapshot(cmd)
        self.forget_object(snapshot.id)

# Live migrate encrypted volume
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_09_live_migrate_volume(self):
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        type = "ROOT",
                        listall = True,
                        )[0]

        Volume.migrate(self.apiclient, volumeid=volume.id, storageid=self.primary_storage2.id, livemigrate=True)

# Resize encrypted data volume attached to a running VM
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_10_resize_encrypted_volume_on_running_vm(self):
        '''
            Live resize of an encrypted volume keeps the LUKS header allowance on StorPool
            and the guest sees the full requested size
        '''
        self.ensure_vm_is_running(self.virtual_machine2)
        size = self.get_unused_disk_size(self.virtual_machine2)
        new_size = size + 2
        volume = Volume.create(
            self.apiclient,
            {"diskname": "StorPoolEncryptedDiskLiveResize"},
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering_encrypted_custom.id,
            account=self.account.name,
            domainid=self.account.domainid,
            size=size
        )
        self._cleanup.append(volume)
        self.virtual_machine2.attach_volume(self.apiclient, volume)
        self.check_volume_size(volume.id, size * GiB)
        self.check_guest_disk_size(self.virtual_machine2, size * GiB)

        volume.resize(self.apiclient, size=new_size)

        self.check_volume_size(volume.id, new_size * GiB)
        self.check_guest_disk_size(self.virtual_machine2, new_size * GiB, old_size=size * GiB)

# Resize detached encrypted data volume
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_11_resize_detached_encrypted_volume(self):
        '''
            Offline resize of an encrypted volume keeps the LUKS header allowance on StorPool
            and the guest sees the full requested size after attach
        '''
        self.ensure_vm_is_running(self.virtual_machine2)
        size = self.get_unused_disk_size(self.virtual_machine2)
        new_size = size + 2
        volume = Volume.create(
            self.apiclient,
            {"diskname": "StorPoolEncryptedDiskOfflineResize"},
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering_encrypted_custom.id,
            account=self.account.name,
            domainid=self.account.domainid,
            size=size
        )
        self._cleanup.append(volume)
        self.virtual_machine2.attach_volume(self.apiclient, volume)
        self.check_volume_size(volume.id, size * GiB)
        self.check_guest_disk_size(self.virtual_machine2, size * GiB)
        self.virtual_machine2.detach_volume(self.apiclient, volume)

        volume.resize(self.apiclient, size=new_size)
        self.check_volume_size(volume.id, new_size * GiB)

        self.virtual_machine2.attach_volume(self.apiclient, volume)
        self.check_guest_disk_size(self.virtual_machine2, new_size * GiB, old_size=size * GiB)

# Resize encrypted ROOT volume of a running VM
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_12_resize_encrypted_root_volume_on_running_vm(self):
        '''
            Live resize of an encrypted ROOT volume keeps the LUKS header allowance on StorPool
            and the guest sees the full requested size
        '''
        self.ensure_vm_is_running(self.virtual_machine)
        root = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type="ROOT",
            listall=True,
        )[0]
        self.check_volume_size(root.id, root.size)
        old_size = root.size
        new_size = self.get_unused_disk_size(self.virtual_machine)

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = root.id
        cmd.size = new_size
        self.apiclient.resizeVolume(cmd)

        self.check_volume_size(root.id, new_size * GiB)
        self.check_guest_disk_size(self.virtual_machine, new_size * GiB, old_size=old_size)

    def ensure_vm_is_running(self, vm):
        state = VirtualMachine.list(self.apiclient, id=vm.id, listall=True)[0].state
        if state != RUNNING:
            vm.start(self.apiclient)

    def check_volume_size(self, volume_id, expected_size):
        '''
            Checks the size in CloudStack is the requested one and
            the StorPool volume has additional space for the LUKS header
        '''
        volume = list_volumes(self.apiclient, id=volume_id, listall=True)[0]
        self.assertEqual(expected_size, volume.size,
            "CloudStack volume size %s is not the expected %s" % (volume.size, expected_size))
        name = volume.path.split("/")[3]
        try:
            sp_volume = self.spapi.volumeList(volumeName="~" + name)[0]
        except spapi.ApiError as err:
            raise Exception(err)
        self.assertEqual(expected_size + LUKS_HEADER_SIZE, sp_volume.size,
            "StorPool volume size %s is not the CloudStack size %s plus the LUKS header %s" % (sp_volume.size, expected_size, LUKS_HEADER_SIZE))

    def get_unused_disk_size(self, vm):
        '''
            Returns a size in GiB bigger than all the disks of the VM,
            so the disks used in the resize tests can be told apart in the guest OS
        '''
        volumes = list_volumes(self.apiclient, virtualmachineid=vm.id, listall=True)
        return max(int(math.ceil(v.size / GiB)) for v in volumes) + 1

    def check_guest_disk_size(self, vm, expected_size, old_size=None, retries=12, interval=5):
        '''
            Checks that one of the disks in the guest OS has the expected size.
            The sizes used in the tests are unique among the VM disks, so after a resize
            no disk should be left with the old size.
            The LUKS header created by QEMU for ROOT disks is a bit smaller than the reserved
            space, so after a VM start the guest may see a size slightly bigger than the requested one.
        '''
        def has_size(sizes, size):
            return any(size <= s < size + LUKS_HEADER_SIZE for s in sizes)

        sizes = []
        for _ in range(retries):
            try:
                ssh_client = vm.get_ssh_client(reconnect=True)
                result = ssh_client.execute("for d in /sys/block/*; do cat $d/size; done")
            except Exception:
                self.fail("SSH failed for Virtual machine: %s" % vm.ipaddress)
            sizes = [int(s) * 512 for s in result if s.strip().isdigit()]
            if has_size(sizes, expected_size) and (old_size is None or not has_size(sizes, old_size)):
                return
            time.sleep(interval)
        self.fail("Expected a disk with size %s (and none with the old size %s) in the guest of VM %s, the guest sees %s" % (expected_size, old_size, vm.name, sizes))
