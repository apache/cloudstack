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

"""
Smoke tests for NAS backup and recovery on StorPool primary storage.

Assumes the environment already has NAS backup configured for the zone
(a NAS backup repository and an imported backup offering).
"""

import math
import time
import uuid

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED, KVM
from marvin.lib.base import (Account,
                             Backup,
                             BackupOffering,
                             BackupRepository,
                             DiskOffering,
                             SecurityGroup,
                             ServiceOffering,
                             StoragePool,
                             VirtualMachine,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_template,
                               list_disk_offering,
                               list_networks,
                               list_service_offering,
                               list_storage_pools,
                               list_zones)
from marvin.lib.utils import cleanup_resources, get_hypervisor_type
from nose.plugins.attrib import attr
from storpool import spapi

from sp_util import StorPoolHelper, TestData


def _template_size_gb(template):
    size_bytes = getattr(template, "size", None)
    if not size_bytes:
        return 1
    return max(1, int(math.ceil(float(size_bytes) / (1024 ** 3))))


class TestNasBackupStorPool(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestNasBackupStorPool, cls).setUpClass()
        try:
            cls.setUpCloudStack()
        except Exception:
            cls.cleanUpCloudStack()
            raise

    @classmethod
    def setUpCloudStack(cls):
        cls._setup_common()
        if cls.unsupportedHypervisor:
            return

        cls.vm = cls._create_test_vm("SP-NasBkp-%s" % uuid.uuid4(), cls.disk_offering.id)
        cls._verify_setup_volumes(cls.vm)

    @classmethod
    def _setup_common(cls):
        """Build everything shared by the StorPool-only and mixed-storage NAS
        backup suites: account, template, network, backup offering, StorPool
        primary storage/service/disk offering. Does not create the test VM,
        so subclasses can plug in a different disk offering for it."""
        config = cls.getClsConfig()
        StorPoolHelper.logger = cls

        zone_cfg = config.zones[0]
        assert zone_cfg is not None
        cls.zone_cfg = zone_cfg

        testClient = super(TestNasBackupStorPool, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.skip_reason = None
        cls.nfs_storage_pool = None
        cls.nfs_tag = None

        td = TestData()
        cls.testdata = td.testdata
        cls.helper = StorPoolHelper()

        cls.services = testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = list_zones(cls.apiclient, name=zone_cfg.name)[0]
        assert cls.zone is not None
        cls.services["mode"] = cls.zone.networktype

        cls.hypervisor = get_hypervisor_type(cls.apiclient)
        if str(cls.hypervisor).lower() != KVM.lower():
            cls.unsupportedHypervisor = True
            cls.skip_reason = "NAS backup on StorPool requires KVM"
            return

        sp_pools = cls.helper.get_pool(zone_cfg)
        if not sp_pools:
            cls.unsupportedHypervisor = True
            cls.skip_reason = "No StorPool primary storage configured in zone"
            return

        cls.spapi = spapi.Api(host=zone_cfg.spEndpoint, port=zone_cfg.spEndpointPort,
                              auth=zone_cfg.spAuthToken, multiCluster=True)

        template = get_template(cls.apiclient, cls.zone.id, account="system")
        if template == FAILED:
            assert False, "get_template() failed to return a system template"
        cls.template = template

        offerings = BackupOffering.listByZone(cls.apiclient, cls.zone.id) or []
        nas_offerings = [o for o in offerings
                         if getattr(o, "provider", None) and "nas" in str(o.provider).lower()]
        if not nas_offerings:
            cls.unsupportedHypervisor = True
            cls.skip_reason = "No imported NAS backup offering found in the zone"
            return
        cls.backup_offering = BackupOffering(nas_offerings[0].__dict__)

        primarystorage = sp_pools[0]
        storage_pools = list_storage_pools(cls.apiclient, name=primarystorage["name"])
        if not storage_pools:
            cls.unsupportedHypervisor = True
            cls.skip_reason = "StorPool primary storage pool %s not found" % primarystorage["name"]
            return
        cls.primary_storage = storage_pools[0]

        sp_tag = primarystorage["tags"] if primarystorage["tags"] else primarystorage["name"]
        cls.helper.updateStoragePoolTags(cls.apiclient, cls.primary_storage.id, sp_tag)
        cls.sp_tag = sp_tag

        cls.template_size_gb = _template_size_gb(cls.template)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            {
                "name": "sp-nas-so-%s" % uuid.uuid4(),
                "displaytext": "StorPool NAS offering (template %s GiB)" % cls.template_size_gb,
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "tags": sp_tag,
            })
        cls._cleanup.append(cls.service_offering)

        cls.disk_offering = cls._get_or_create_disk_offering(
            sp_tag, "StorPool NAS backup disk offering")

        cls.account = cls.helper.create_account(
            cls.apiclient,
            cls.services["account"],
            accounttype=1,
            domainid=cls.domain.id,
            roleid=1)
        cls._cleanup.append(cls.account)

        if cls.zone.securitygroupsenabled:
            securitygroup = SecurityGroup.list(
                cls.apiclient, account=cls.account.name, domainid=cls.account.domainid)[0]
            cls.helper.set_securityGroups(
                cls.apiclient,
                account=cls.account.name,
                domainid=cls.account.domainid,
                id=securitygroup.id)

        cls.network_ids = None
        if str(cls.zone.networktype).lower() == "advanced":
            networks = list_networks(cls.apiclient, zoneid=cls.zone.id, listall=True) or []
            guest_networks = [n for n in networks
                              if str(getattr(n, "traffictype", "")).lower() == "guest"]
            shared = [n for n in guest_networks
                      if str(getattr(n, "type", "")).lower() == "shared"]
            picked = (shared or guest_networks)[0] if (shared or guest_networks) else None
            if picked is None:
                cls.unsupportedHypervisor = True
                cls.skip_reason = "Advanced zone has no existing guest network to deploy on"
                return
            cls.network_ids = [picked.id]

    @classmethod
    def _get_or_create_disk_offering(cls, tag, displaytext):
        disk_offerings = list_disk_offering(cls.apiclient, name=tag)
        if disk_offerings is None:
            disk_offering = DiskOffering.create(
                cls.apiclient,
                {
                    "name": tag,
                    "displaytext": displaytext,
                    "disksize": 5,
                    "storagetype": "shared",
                    "tags": tag,
                })
            cls._cleanup.append(disk_offering)
            return disk_offering
        return disk_offerings[0]

    @classmethod
    def _create_test_vm(cls, name, diskofferingid):
        vm = VirtualMachine.create(
            cls.apiclient,
            {"name": name},
            zoneid=cls.zone.id,
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            diskofferingid=diskofferingid,
            hypervisor=cls.hypervisor,
            networkids=cls.network_ids,
            rootdisksize=cls.template_size_gb,
            mode=cls.services["mode"])
        cls._cleanup.insert(0, vm)
        return vm

    @classmethod
    def _verify_setup_volumes(cls, vm):
        volumes = Volume.list(cls.apiclient, virtualmachineid=vm.id, listall=True)
        assert isinstance(volumes, list) and len(volumes) >= 1
        for vol in volumes:
            if cls.nfs_storage_pool and vol.storageid == cls.nfs_storage_pool.id:
                continue
            cls.helper.verify_storpool_volume(cls.spapi, vol)
        return volumes

    @classmethod
    def tearDownClass(cls):
        cls.cleanUpCloudStack()

    @classmethod
    def cleanUpCloudStack(cls):
        try:
            if hasattr(cls, "vm") and cls.vm is not None:
                cls._remove_vm_backups_and_offering(cls.apiclient, cls.vm)
            if hasattr(cls, "_cleanup") and cls._cleanup:
                cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def _remove_vm_backups_and_offering(cls, apiclient, vm):
        try:
            backups = Backup.list(apiclient, vm.id) or []
            for backup in backups:
                try:
                    Backup.delete(apiclient, backup.id, forced=True)
                except Exception:
                    Backup.delete(apiclient, backup.id)
        except Exception:
            pass
        try:
            if hasattr(cls, "backup_offering") and cls.backup_offering is not None:
                cls.backup_offering.removeOffering(apiclient, vm.id, forced=True)
        except Exception:
            pass

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        self.cleanup_backups = []
        if self.unsupportedHypervisor:
            self.skipTest(self.skip_reason or "Skipping unsupported environment")
        self._remove_vm_backups_and_offering(self.apiclient, self.vm)

    def tearDown(self):
        try:
            for obj in list(self.cleanup_backups):
                try:
                    Backup.delete(self.apiclient, obj.id, forced=True)
                except Exception:
                    pass
            for obj in self.cleanup:
                if isinstance(obj, VirtualMachine):
                    self._remove_vm_backups_and_offering(self.apiclient, obj)
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def _verify_vm_volumes_on_storpool(self, vm, spapi_client=None):
        """Verify every volume of vm. A volume that lives on the NFS primary
        storage pool (only possible in the mixed-storage suite, where
        self.nfs_storage_pool is set) is checked against NFS instead of
        StorPool, so the NAS backup lifecycle tests can run unmodified
        against either an all-StorPool VM or a mixed StorPool+NFS one."""
        volumes = Volume.list(self.apiclient, virtualmachineid=vm.id, listall=True)
        self.assertTrue(isinstance(volumes, list) and len(volumes) >= 1,
                        "VM should have at least one volume")
        for vol in volumes:
            if self.nfs_storage_pool and vol.storageid == self.nfs_storage_pool.id:
                self.assertFalse(vol.path and vol.path.startswith("/dev/storpool-byid/"),
                                 "Volume %s is on NFS storage %s, should not have a StorPool path, got %s" %
                                 (vol.id, self.nfs_storage_pool.id, vol.path))
                continue
            self.assertTrue(vol.path and vol.path.startswith("/dev/storpool-byid/"),
                            "Volume %s path should be /dev/storpool-byid/..., got %s" %
                            (vol.id, vol.path))
            self.helper.verify_storpool_volume(spapi_client or self.spapi, vol)
            spvolume = self.helper.get_storpool_volume_by_path(spapi_client or self.spapi, vol.path)
            sp_uuid_tag = (spvolume.tags or {}).get("uuid")
            self.assertEqual(sp_uuid_tag, vol.id,
                             "StorPool volume uuid tag %s should match CloudStack volume id %s for %s" %
                             (sp_uuid_tag, vol.id, vol.path))
        return volumes

    def _write_guest_marker(self, vm, marker, ipaddress=None):
        """Create a marker file in the guest and sync it to disk before backup."""
        try:
            ssh = vm.get_ssh_client(ipaddress=ipaddress, reconnect=True) if ipaddress \
                else vm.get_ssh_client(reconnect=True)
            ssh.execute("touch %s; sync" % marker)
        except Exception as err:
            self.fail("SSH failed writing marker on VM %s: %s" %
                      (ipaddress or getattr(vm, "ipaddress", vm.id), err))

    def _assert_guest_reachable(self, vm, marker=None, ipaddress=None, msg=None):
        if ipaddress is None:
            listed = VirtualMachine.list(
                self.apiclient, id=vm.id, listall=True) or []
            self.assertTrue(listed and listed[0].state == "Running",
                            "VM %s should be Running for SSH check" % vm.id)
            ipaddress = listed[0].ipaddress
        try:
            ssh = vm.get_ssh_client(ipaddress=ipaddress, reconnect=True)
            if marker:
                result = ssh.execute("ls %s" % marker)
                self.assertEqual(result[0], marker,
                                 msg or ("Guest should retain marker %s" % marker))
            else:
                result = ssh.execute("echo ok")
                self.assertTrue(result and result[0].strip() == "ok",
                                msg or "Guest should answer SSH after restore")
        except Exception as err:
            self.fail("SSH failed for VM %s (%s): %s" %
                      (vm.id, ipaddress, err))

    def _create_backup(self, vmid, name=None):
        Backup.create(self.apiclient, vmid, name)
        backups = Backup.list(self.apiclient, vmid) or []
        self.assertTrue(backups, "Backup.create should produce a listable backup")
        return backups[0]

    def _backup_type(self, backup):
        return getattr(backup, 'type', 'FULL') or 'FULL'

    @attr(tags=["advanced", "backup", "storpool"], required_hardware="true")
    def test_01_vm_backup_lifecycle(self):
        """Assign offering, create and delete an ad-hoc NAS backup on StorPool VM."""
        backups = Backup.list(self.apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        self.backup_offering.assignOffering(self.apiclient, self.vm.id)
        backup = self._create_backup(self.vm.id)
        self.cleanup_backups.append(backup)

        self._verify_vm_volumes_on_storpool(self.vm)

        Backup.delete(self.apiclient, backup.id)
        backups = Backup.list(self.apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        self.backup_offering.removeOffering(self.apiclient, self.vm.id)

    @attr(tags=["advanced", "backup", "storpool"], required_hardware="true")
    def test_02_create_vm_from_backup(self):
        """Create a new VM from NAS backup and verify volumes via StorPool API.
        """
        self.backup_offering.assignOffering(self.apiclient, self.vm.id)

        marker = "storpool_nas_backup_%s.txt" % int(time.time())
        self._write_guest_marker(self.vm, marker)

        time.sleep(5)
        backup = self._create_backup(self.vm.id, "sp-nas-backup1")
        self.cleanup_backups.append(backup)

        self.backup_offering.removeOffering(self.apiclient, self.vm.id)

        new_vm_name = "vm-from-sp-nas-%s" % int(time.time())
        vm_services = self.services.get("small", self.services)
        new_vm = Backup.createVMFromBackup(
            self.apiclient,
            vm_services,
            mode=self.services["mode"],
            backupid=backup.id,
            vmname=new_vm_name,
            accountname=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            networkids=self.network_ids)
        self.cleanup.append(new_vm)

        self.assertEqual(new_vm.name, new_vm_name)
        self.assertEqual(new_vm.state, "Running")
        self.assertEqual(new_vm.zoneid, self.zone.id)

        volumes = self._verify_vm_volumes_on_storpool(new_vm)
        self.assertEqual(2, len(volumes),
                         "New VM should have ROOT + DATADISK volumes on StorPool")

        self._assert_guest_reachable(
            new_vm, marker=marker, ipaddress=new_vm.ipaddress,
            msg="Instance created from backup should retain guest file")

    @attr(tags=["advanced", "backup", "storpool"], required_hardware="true")
    def test_03_restore_vm_and_volume_from_backup(self):
        """Restore VM in place and restore a volume attach; verify StorPool volumes."""
        self.backup_offering.assignOffering(self.apiclient, self.vm.id)

        marker = "storpool_nas_restore_%s.txt" % int(time.time())
        self._write_guest_marker(self.vm, marker)
        time.sleep(5)
        backup = self._create_backup(self.vm.id)
        self.cleanup_backups.append(backup)

        self.vm.stop(self.apiclient, forced=True)
        Backup.restoreVM(self.apiclient, backup.id)
        self.vm.start(self.apiclient)
        self._verify_vm_volumes_on_storpool(self.vm)
        self._assert_guest_reachable(
            self.vm, marker=marker,
            msg="In-place restore should boot and retain guest file")

        target_vm = VirtualMachine.create(
            self.apiclient,
            {"name": "SP-NasAttach-%s" % uuid.uuid4()},
            zoneid=self.zone.id,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            networkids=self.network_ids,
            rootdisksize=self.template_size_gb,
            mode=self.services["mode"],
            startvm=False)
        self.cleanup.append(target_vm)

        volumes = Volume.list(self.apiclient, virtualmachineid=self.vm.id, listall=True)
        data_disk = next((v for v in volumes if v.type == "DATADISK"), None)
        self.assertIsNotNone(data_disk, "Source VM should have a data disk")

        Backup.restoreVolumeFromBackupAndAttachToVM(
            self.apiclient,
            backupid=backup.id,
            volumeid=data_disk.id,
            virtualmachineid=target_vm.id)
        target_vm.start(self.apiclient)

        target_volumes = self._verify_vm_volumes_on_storpool(target_vm)
        self.assertEqual(2, len(target_volumes),
                         "Target VM should have ROOT + restored DATADISK volumes")
        self._assert_guest_reachable(
            target_vm, msg="VM with restored volume attached should boot and answer SSH")

        self.backup_offering.removeOffering(self.apiclient, self.vm.id)

    @attr(tags=["advanced", "backup", "storpool"], required_hardware="true")
    def test_04_restore_destroyed_vm_from_backup(self):
        """Destroy (no expunge) then restoreBackup — exercises restoreVolumesOfDestroyedVMs.

        Uses a ROOT-only VM: destroyVirtualMachine detaches data disks, which would
        make volume-count validation fail against a ROOT+DATA backup.
        """
        destroyed_vm = VirtualMachine.create(
            self.apiclient,
            {"name": "SP-NasDestroyed-%s" % uuid.uuid4()},
            zoneid=self.zone.id,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            networkids=self.network_ids,
            rootdisksize=self.template_size_gb,
            mode=self.services["mode"])
        self.cleanup.append(destroyed_vm)

        self.backup_offering.assignOffering(self.apiclient, destroyed_vm.id)

        marker = "storpool_nas_destroyed_%s.txt" % int(time.time())
        self._write_guest_marker(destroyed_vm, marker)
        time.sleep(5)
        backup = self._create_backup(destroyed_vm.id)
        self.cleanup_backups.append(backup)

        destroyed_vm.delete(self.apiclient, expunge=False)

        listed = VirtualMachine.list(
            self.apiclient, id=destroyed_vm.id, listall=True) or []
        self.assertTrue(listed, "Destroyed VM should still be listable")
        self.assertEqual(listed[0].state, "Destroyed")

        Backup.restoreVM(self.apiclient, backup.id)

        restored = VirtualMachine.list(
            self.apiclient, id=destroyed_vm.id, listall=True) or []
        self.assertTrue(restored)
        self.assertEqual(restored[0].state, "Stopped",
                         "restoreBackup should leave the Instance Stopped")

        destroyed_vm.start(self.apiclient)
        volumes = self._verify_vm_volumes_on_storpool(destroyed_vm)
        self.assertEqual(1, len(volumes),
                         "ROOT-only destroyed VM restore should have one StorPool volume")
        self._assert_guest_reachable(
            destroyed_vm, marker=marker,
            msg="Restored destroyed VM should boot and retain guest file")

        self.backup_offering.removeOffering(self.apiclient, destroyed_vm.id)

    @attr(tags=["advanced", "backup", "storpool"], required_hardware="true")
    def test_05_backup_stopped_vm(self):
        """Restore a NAS backup that was taken while the StorPool VM was stopped.
        """
        self.backup_offering.assignOffering(self.apiclient, self.vm.id)

        marker = "storpool_nas_stopped_restore_%s.txt" % int(time.time())
        self._write_guest_marker(self.vm, marker)
        time.sleep(5)

        self.vm.stop(self.apiclient, forced=True)

        backup = self._create_backup(self.vm.id, "sp-nas-stopped-restore-backup")
        self.cleanup_backups.append(backup)
        self.assertEqual(self._backup_type(backup).upper(), "FULL",
                         "A stopped-VM NAS backup on StorPool should always be a FULL backup")

        Backup.restoreVM(self.apiclient, backup.id)

        self.vm.start(self.apiclient)
        self._verify_vm_volumes_on_storpool(self.vm)
        self._assert_guest_reachable(
            self.vm, marker=marker,
            msg="Restore of a stopped-VM StorPool backup should boot and retain the guest file")

        self.backup_offering.removeOffering(self.apiclient, self.vm.id)


class TestNasBackupMixedStorage(TestNasBackupStorPool):
    """Repeats the NAS backup smoke tests (test_01 .. test_05, inherited
    unchanged from TestNasBackupStorPool) against a VM with mixed-provider
    volumes: ROOT on StorPool, DATADISK on NFS primary storage.

    The NFS primary storage pool, its name and storage tag are taken from
    the Marvin cfg file (the "nfs://" primaryStorages entry for the zone).
    Other suites assume that pool is left in Maintenance, so it is always
    put into Maintenance before this suite touches it, taken out of
    Maintenance only for the duration of creating volumes/VMs on it, and
    put back into Maintenance once every test in this class has finished
    (including when setup itself fails).
    """

    @classmethod
    def setUpCloudStack(cls):
        cls._setup_common()
        if cls.unsupportedHypervisor:
            return

        cls._setup_nfs_environment()
        if cls.unsupportedHypervisor:
            return

        cls.nfs_storage_pool = cls._ensure_maintenance(cls.nfs_storage_pool.id, enable=False)

        cls.vm = cls._create_test_vm("SP-NasMixed-%s" % uuid.uuid4(), cls.nfs_disk_offering.id)
        cls._verify_setup_volumes(cls.vm)

    @classmethod
    def _setup_nfs_environment(cls):
        nfs_pools = cls.helper.get_nfs_pool(cls.zone_cfg)
        if not nfs_pools:
            cls.unsupportedHypervisor = True
            cls.skip_reason = "No NFS primary storage configured in zone"
            return
        nfs_cfg = nfs_pools[0]

        storage_pools = list_storage_pools(cls.apiclient, name=nfs_cfg["name"])
        if not storage_pools:
            cls.unsupportedHypervisor = True
            cls.skip_reason = "NFS primary storage pool %s not found" % nfs_cfg["name"]
            return
        cls.nfs_storage_pool = storage_pools[0]

        cls.nfs_tag = nfs_cfg["tags"] if nfs_cfg["tags"] else nfs_cfg["name"]
        cls.helper.updateStoragePoolTags(cls.apiclient, cls.nfs_storage_pool.id, cls.nfs_tag)

        # Other suites assume the NFS pool is unavailable; make sure that
        # invariant holds before this suite starts using it.
        cls.nfs_storage_pool = cls._ensure_maintenance(cls.nfs_storage_pool.id, enable=True)

        cls.nfs_disk_offering = cls._get_or_create_disk_offering(
            cls.nfs_tag, "NFS NAS backup disk offering")
        cls.nfs_service_offering = cls._get_or_create_nfs_service_offering()

    @classmethod
    def _get_or_create_nfs_service_offering(cls):
        service_offerings = list_service_offering(cls.apiclient, name=cls.nfs_tag)
        if service_offerings is None:
            service_offering = ServiceOffering.create(
                cls.apiclient,
                {
                    "name": cls.nfs_tag,
                    "displaytext": "NFS NAS offering (%s)" % cls.nfs_tag,
                    "cpunumber": 1,
                    "cpuspeed": 500,
                    "memory": 512,
                    "storagetype": "shared",
                    "tags": cls.nfs_tag,
                })
            cls._cleanup.append(service_offering)
            return service_offering
        return service_offerings[0]

    @classmethod
    def _ensure_maintenance(cls, pool_id, enable):
        pool = list_storage_pools(cls.apiclient, id=pool_id)[0]
        if enable and pool.state != "Maintenance":
            pool = StoragePool.enableMaintenance(cls.apiclient, pool_id)
        elif not enable and pool.state == "Maintenance":
            pool = StoragePool.cancelMaintenance(cls.apiclient, pool_id)
        return pool

    @classmethod
    def cleanUpCloudStack(cls):
        try:
            super(TestNasBackupMixedStorage, cls).cleanUpCloudStack()
        finally:
            if getattr(cls, "nfs_storage_pool", None) is not None:
                cls._ensure_maintenance(cls.nfs_storage_pool.id, enable=True)
