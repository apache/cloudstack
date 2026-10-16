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
""" BVT tests for volume migration between RBD (Ceph) and NFS primary
storage pools on KVM.

Every combination below is covered, for plain volumes
(TestVolumeMigrationRbdNfs) as well as for encrypted volumes
(TestEncryptedVolumeMigrationRbdNfs):

* source and destination pool: RBD -> NFS, NFS -> RBD, NFS -> NFS and
  RBD -> RBD
* online (the Instance keeps running, migrateVolume with livemigrate) and
  offline (the Instance is stopped, or the volume is not attached)
* ROOT and DATA volumes
* volumes with and without a backing chain: every test deploys its own
  Instance on its source pool, so the first migration moves a ROOT volume
  cloned from the template (a qcow2 backing file on NFS, an RBD clone on
  Ceph); the DATA volume, and the ROOT volume once it was migrated, have no
  chain.

Besides the volume being on the expected pool, the data of each volume is
verified on the host after every migration, as the Instance sees it (that is,
decrypted): a pattern written to the DATA volume, and a checksum of the boot
area of the ROOT volume. For encrypted volumes the encryption the destination
pool requires is verified too: native librbd LUKS2 on RBD, qemu LUKS on NFS.

Requirements: KVM, an RBD pool and the "nfs" primary storage of the test
data. The NFS -> NFS migrations also need the "nfs2" primary storage and the
RBD -> RBD migrations a second RBD pool; those are skipped otherwise. The
encrypted volume tests need a host with RBD volume encryption support.
"""

import base64
import hashlib
import re
import xml.etree.ElementTree as ET

from marvin.cloudstackAPI import (deleteStoragePool,
                                  detachVolume,
                                  enableStorageMaintenance)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             DiskOffering,
                             Host,
                             ServiceOffering,
                             StoragePool,
                             Template,
                             VirtualMachine,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_pod,
                               get_zone,
                               list_clusters,
                               list_hosts,
                               list_storage_pools)
from marvin.lib.utils import random_gen
from marvin.sshClient import SshClient

from nose.plugins.attrib import attr

_multiprocess_shared_ = False

# Runs on the host of an Instance to access one of its disks as the Instance sees it, i.e. decrypted:
#   disk-io.sh <domain> <disk alias> write|read "<qemu-io command>"
# A write goes through the HMP qemu-io command of the disk device. A read goes through a read-only NBD
# export of the node of the disk, as the output of HMP qemu-io ends up (buffered) in the log of qemu.
DISK_IO_SCRIPT = r'''
dom=$1; dev=$2; mode=$3; cmd=$4
qmp() { virsh qemu-monitor-command "$dom" "$1"; }
read qdev node <<<$(qmp '{"execute":"query-block"}' | python3 -c '
import json, sys
for b in json.load(sys.stdin)["return"]:
    q = b.get("qdev", "")
    if sys.argv[1] in (q, b.get("device", "")) or q.startswith("/machine/peripheral/%s/" % sys.argv[1]):
        print(q or b["device"], b["inserted"]["node-name"])
        break' "$dev")
[ -z "$node" ] && { echo "no block node found for disk $dev"; exit 1; }
if [ "$mode" = write ]; then
    virsh qemu-monitor-command "$dom" --hmp "qemu-io -d $qdev \"$cmd\""
    exit $?
fi
sock=$(ls -d /var/lib/libvirt/qemu/domain-*-$dom | head -1)/volume-migration-test.sock
qmp "{\"execute\":\"nbd-server-start\",\"arguments\":{\"addr\":{\"type\":\"unix\",\"data\":{\"path\":\"$sock\"}}}}" >/dev/null
qmp "{\"execute\":\"block-export-add\",\"arguments\":{\"type\":\"nbd\",\"id\":\"volmig\",\"node-name\":\"$node\",\"writable\":false}}" >/dev/null
qemu-io -r -f raw "nbd+unix:///$node?socket=$sock" -c "$cmd"
rc=$?
qmp '{"execute":"block-export-del","arguments":{"id":"volmig"}}' >/dev/null
sleep 1
qmp '{"execute":"nbd-server-stop"}' >/dev/null
exit $rc
'''
DISK_IO_SCRIPT_PATH = "/tmp/cs-volume-migration-test-disk-io.sh"

DATA_PATTERN = "0xa5"
DATA_PATTERN_OFFSET = "1M"
DATA_PATTERN_LENGTH = "4M"
DATA_PATTERN_BYTES = 4 * 1024 * 1024
# The start of a ROOT volume that the guest does not write to: the MBR and the gap in front of the first
# partition (which traditionally starts at sector 63) that holds the boot loader. Further on, the guest
# updates its file systems (e.g. the mount time in the superblock of /boot) while it runs.
ROOT_CHECKSUM_LENGTH = "31k"


class VolumeMigrationRbdNfsBase(cloudstackTestCase):
    """Set up and helpers of the volume migration tests between RBD and NFS pools on KVM"""

    # whether the ROOT and DATA volumes of the Instances are encrypted
    encrypted = False

    @classmethod
    def setUpClass(cls):
        testClient = super(VolumeMigrationRbdNfsBase, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.dbclient = testClient.getDbConnection()
        cls.services = testClient.getParsedTestDataConfig()
        cls._cleanup = []
        cls.created_pools = []
        cls.saved_pool_tags = {}
        cls.unsupported_reason = None

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()

        if cls.hypervisor.lower() != 'kvm':
            cls.unsupported_reason = "requires KVM"
            return

        rbd_pools = [pool for pool in list_storage_pools(cls.apiclient, zoneid=cls.zone.id) or []
                     if pool.type == 'RBD' and pool.state == 'Up']
        if not rbd_pools:
            cls.unsupported_reason = "requires an RBD storage pool"
            return
        cls.rbd_pool = rbd_pools[0]
        cls.rbd_pool2 = rbd_pools[1] if len(rbd_pools) > 1 else None

        cls.cluster = None
        for cluster in list_clusters(cls.apiclient, zoneid=cls.zone.id) or []:
            if list_hosts(cls.apiclient, clusterid=cluster.id):
                cls.cluster = cluster
                break
        if cls.cluster is None:
            cls.unsupported_reason = "requires a cluster with hosts"
            return

        cls.host_config = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        if cls.encrypted and not cls.host_with_rbd_encryption_support_exists():
            cls.unsupported_reason = "requires a host with RBD volume encryption support"
            return

        cls.template = Template.register(
            cls.apiclient,
            cls.services["test_templates_cloud_init"][cls.hypervisor.lower()],
            zoneid=cls.zone.id,
            hypervisor=cls.hypervisor,
        )
        cls.template.download(cls.apiclient)

        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = cls.template.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        # The offerings carry a storage tag of their own, which is put on the pool an Instance has to be
        # deployed on, and on all the pools the volumes are migrated between.
        cls.pool_tag = "volmig-" + random_gen(size=6).lower()
        encryption_args = {"encryptroot": True} if cls.encrypted else {}
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"],
            tags=cls.pool_tag,
            **encryption_args
        )
        cls._cleanup.append(cls.service_offering)

        encryption_args = {"encrypt": True} if cls.encrypted else {}
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"],
            tags=cls.pool_tag,
            **encryption_args
        )
        cls._cleanup.append(cls.disk_offering)

        cls.nfs_pool = cls.create_nfs_pool(cls.services["nfs"])
        cls.nfs_pool2 = cls.create_nfs_pool(cls.services["nfs2"]) if "nfs2" in cls.services else None

        cls.pools = [pool for pool in (cls.rbd_pool, cls.rbd_pool2, cls.nfs_pool, cls.nfs_pool2) if pool is not None]
        for pool in cls.pools:
            cls.saved_pool_tags[pool.id] = getattr(pool, 'tags', "") or ""

    @classmethod
    def create_nfs_pool(cls, services):
        pool = StoragePool.create(
            cls.apiclient,
            services,
            clusterid=cls.cluster.id,
            zoneid=cls.zone.id,
            podid=cls.pod.id
        )
        cls.created_pools.append(pool)
        return pool

    @classmethod
    def host_with_rbd_encryption_support_exists(cls):
        # librbd encryption support is only tracked in host_details, it is not part of the API response
        hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, type='Routing', hypervisor='KVM', state='Up') or []
        for host in hosts:
            supported = cls.dbclient.execute(
                "select hd.value from host_details hd join host h on h.id=hd.host_id "
                "where h.uuid='%s' and hd.name='host.volume.encryption.rbd'" % host.id
            )
            if supported and supported[0][0].lower() == 'true':
                return True
        return False

    @classmethod
    def tag_pools(cls, pools):
        """Put the storage tag of the offerings on the given pools (on top of their own tags) and remove it
        from the other ones"""
        for pool in cls.pools:
            tags = [tag for tag in cls.saved_pool_tags[pool.id].split(",") if tag]
            if pool in pools:
                tags.append(cls.pool_tag)
            StoragePool.update(cls.apiclient, id=pool.id, tags=",".join(tags))

    @classmethod
    def tearDownClass(cls):
        try:
            super(VolumeMigrationRbdNfsBase, cls).tearDownClass()
        finally:
            for pool in getattr(cls, 'pools', []):
                try:
                    StoragePool.update(cls.apiclient, id=pool.id, tags=cls.saved_pool_tags[pool.id])
                except Exception as e:
                    cls.debug("Warning: failed to restore the tags of storage pool %s: %s" % (pool.id, e))
            for pool in cls.created_pools:
                cls.delete_storage_pool(pool)

    @classmethod
    def delete_storage_pool(cls, pool):
        """Put the pool in maintenance and force delete it, so a volume left
        behind by a failed test does not leak the pool into the next run."""
        try:
            cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
            cmd.id = pool.id
            cls.apiclient.enableStorageMaintenance(cmd)
            cmd = deleteStoragePool.deleteStoragePoolCmd()
            cmd.id = pool.id
            cmd.forced = True
            cls.apiclient.deleteStoragePool(cmd)
        except Exception as e:
            cls.debug("Warning: failed to delete storage pool %s: %s" % (pool.id, e))

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        if self.unsupported_reason:
            self.skipTest("Skipping test: %s" % self.unsupported_reason)

    def tearDown(self):
        super(VolumeMigrationRbdNfsBase, self).tearDown()

    def require_pools(self, *pools):
        if None in pools:
            self.skipTest("Skipping test: requires a second NFS (\"nfs2\" in the test data) or RBD storage pool")

    # Instances and volumes

    def deploy_instance(self, pool):
        """Deploy an Instance with a DATA volume, both volumes allocated on the given pool. The ROOT volume
        is cloned from the template, so it has a backing chain (a qcow2 backing file or an RBD parent)."""
        self.tag_pools([pool])
        try:
            instance = VirtualMachine.create(
                self.apiclient,
                self.services,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.services["mode"]
            )
            self.cleanup.append(instance)
            data_volume = Volume.create(
                self.apiclient,
                self.services,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering.id
            )
            self.cleanup.append(data_volume)
            instance.attach_volume(self.apiclient, data_volume)
        finally:
            self.tag_pools(self.pools)

        root_volume = Volume.list(self.apiclient, virtualmachineid=instance.id, type='ROOT', listall=True)[0]
        for volume in (root_volume, data_volume):
            self.assert_volume_on_pool(volume, pool)
        # Destroying the Instance does not remove its DATA volume, which is to be detached first
        self.cleanup.remove(data_volume)
        self.cleanup.insert(0, _DetachedVolume(self.apiclient, data_volume))
        return instance, root_volume, data_volume

    def migrate_online(self, instance, volumes, pool):
        for volume in volumes:
            Volume.migrate(self.apiclient, volumeid=volume.id, storageid=pool.id, livemigrate=True)
        self.assert_instance_state(instance, 'Running')

    def migrate_offline(self, instance, volumes, pool):
        instance.stop(self.apiclient)
        for volume in volumes:
            Volume.migrate(self.apiclient, volumeid=volume.id, storageid=pool.id, livemigrate=False)
        instance.start(self.apiclient)

    def migrate_detached(self, instance, volume, pool):
        # the Instance is stopped, as not every disk bus (e.g. IDE) supports hot unplugging
        instance.stop(self.apiclient)
        instance.detach_volume(self.apiclient, volume)
        Volume.migrate(self.apiclient, volumeid=volume.id, storageid=pool.id, livemigrate=False)
        instance.attach_volume(self.apiclient, volume)
        instance.start(self.apiclient)

    def run_migrations(self, source_pool, destination_pools, online):
        """Deploy an Instance on source_pool and migrate its ROOT and DATA volumes along destination_pools,
        verifying the volumes after every migration, and again after restarting the Instance."""
        instance, root_volume, data_volume = self.deploy_instance(source_pool)
        disks = InstanceDisks(self, instance, root_volume, data_volume)
        disks.mark()

        for pool in destination_pools:
            if online:
                self.migrate_online(instance, (root_volume, data_volume), pool)
            else:
                self.migrate_offline(instance, (root_volume, data_volume), pool)
            self.verify_volumes(instance, disks, (root_volume, data_volume), pool)
            # the volumes also have to be usable when the Instance is started again
            instance.stop(self.apiclient)
            instance.start(self.apiclient)
            self.verify_volumes(instance, disks, (root_volume, data_volume), pool)

    # Checks

    def verify_volumes(self, instance, disks, volumes, pool):
        for volume in volumes:
            self.assert_volume_on_pool(volume, pool)
        self.assert_instance_state(instance, 'Running')
        disks.verify(volumes, pool)

    def assert_volume_on_pool(self, volume, pool):
        migrated_volume = Volume.list(self.apiclient, id=volume.id, listall=True)[0]
        self.assertEqual(
            migrated_volume.storageid,
            pool.id,
            "Volume %s is on storage pool %s instead of %s" % (volume.id, migrated_volume.storage, pool.name)
        )
        self.assertEqual(migrated_volume.state, 'Ready', "Volume %s is not Ready after the migration" % volume.id)

    def assert_instance_state(self, instance, state):
        self.assertEqual(
            VirtualMachine.list(self.apiclient, id=instance.id)[0].state,
            state,
            "Instance %s is not %s" % (instance.id, state)
        )

    # The tests

    def online_migration_from_rbd(self):
        """Online migrate a ROOT volume with a backing chain (an RBD clone of the template) and a DATA volume
        from RBD to NFS, then (without chain) NFS -> NFS, NFS -> RBD and RBD -> RBD"""
        self.run_migrations(self.rbd_pool, [pool for pool in (self.nfs_pool, self.nfs_pool2, self.rbd_pool, self.rbd_pool2)
                                            if pool is not None], online=True)

    def online_migration_from_nfs(self, destination_pool):
        """Online migrate a ROOT volume with a backing chain (a qcow2 file backed by the template) and a DATA
        volume from NFS to destination_pool"""
        self.run_migrations(self.nfs_pool, [destination_pool], online=True)

    def offline_migration_from_rbd(self):
        """Offline migrate a ROOT volume with a backing chain (an RBD clone of the template) and a DATA volume
        from RBD to NFS, then (without chain) NFS -> NFS, NFS -> RBD and RBD -> RBD"""
        self.run_migrations(self.rbd_pool, [pool for pool in (self.nfs_pool, self.nfs_pool2, self.rbd_pool, self.rbd_pool2)
                                            if pool is not None], online=False)

    def offline_migration_from_nfs(self, destination_pool):
        """Offline migrate a ROOT volume with a backing chain (a qcow2 file backed by the template) and a DATA
        volume from NFS to destination_pool"""
        self.run_migrations(self.nfs_pool, [destination_pool], online=False)

    def detached_volume_migration(self):
        """Migrate a DATA volume that is not attached to an Instance between all the pools"""
        instance, root_volume, data_volume = self.deploy_instance(self.rbd_pool)
        disks = InstanceDisks(self, instance, root_volume, data_volume)
        disks.mark()
        for pool in [pool for pool in (self.nfs_pool, self.nfs_pool2, self.rbd_pool2, self.rbd_pool) if pool is not None]:
            self.migrate_detached(instance, data_volume, pool)
            self.verify_volumes(instance, disks, (data_volume,), pool)


class _DetachedVolume:
    """Cleanup wrapper that detaches a DATA volume (if it still is attached) before deleting it"""

    def __init__(self, apiclient, volume):
        self.apiclient = apiclient
        self.volume = volume

    def delete(self, apiclient):
        volumes = Volume.list(apiclient, id=self.volume.id, listall=True)
        if not volumes:
            return
        if getattr(volumes[0], 'virtualmachineid', None):
            cmd = detachVolume.detachVolumeCmd()
            cmd.id = self.volume.id
            apiclient.detachVolume(cmd)
        self.volume.delete(apiclient)


class InstanceDisks:
    """Verifies the data of the ROOT and DATA volumes of an Instance on its host, as the Instance sees them
    (decrypted): a pattern written to the DATA volume, and a checksum of the boot area of the ROOT volume. For
    encrypted volumes, the encryption of the disks in the XML domain of the Instance is verified as well."""

    def __init__(self, test, instance, root_volume, data_volume):
        self.test = test
        self.instance = instance
        self.root_volume = root_volume
        self.data_volume = data_volume
        self.root_checksum = None

    def _host_ssh(self):
        vm = VirtualMachine.list(self.test.apiclient, id=self.instance.id)[0]
        host = Host.list(self.test.apiclient, id=vm.hostid)[0]
        ssh = SshClient(host=host.ipaddress, port=22, user=self.test.host_config['username'],
                        passwd=self.test.host_config['password'])
        script = base64.b64encode(DISK_IO_SCRIPT.encode()).decode()
        ssh.execute("echo %s | base64 -d > %s" % (script, DISK_IO_SCRIPT_PATH))
        return ssh, vm.instancename

    def _disk(self, ssh, instance_name, volume):
        """The disk element of the volume in the XML domain; its serial is the start of the volume uuid"""
        domain = ET.fromstring("\n".join(ssh.execute("virsh dumpxml %s" % instance_name)))
        serial = volume.id.replace("-", "")[:20]
        for disk in domain.findall("./devices/disk"):
            if disk.findtext("serial") == serial:
                return disk
        self.test.fail("Disk of volume %s not found in the XML domain of %s" % (volume.id, instance_name))

    def _disk_io(self, volume, mode, command):
        ssh, instance_name = self._host_ssh()
        alias = self._disk(ssh, instance_name, volume).find("alias").get("name")
        return "\n".join(ssh.execute("bash %s %s %s %s '%s' 2>&1" % (DISK_IO_SCRIPT_PATH, instance_name, alias, mode, command)))

    def _root_checksum(self):
        dump = [line for line in self._disk_io(self.root_volume, "read", "read -v 0 %s" % ROOT_CHECKSUM_LENGTH).splitlines()
                if re.match(r"^[0-9a-f]{8}:", line)]
        return hashlib.md5("\n".join(dump).encode()).hexdigest() if dump else None

    def _data_pattern_present(self):
        out = self._disk_io(self.data_volume, "read", "read -P %s %s %s" % (DATA_PATTERN, DATA_PATTERN_OFFSET, DATA_PATTERN_LENGTH))
        return "verification failed" not in out and ("read %d/%d bytes" % (DATA_PATTERN_BYTES, DATA_PATTERN_BYTES)) in out

    def mark(self):
        self._disk_io(self.data_volume, "write", "write -P %s %s %s" % (DATA_PATTERN, DATA_PATTERN_OFFSET, DATA_PATTERN_LENGTH))
        self.test.assertTrue(self._data_pattern_present(), "Failed to write the test pattern to volume %s" % self.data_volume.id)
        self.root_checksum = self._root_checksum()
        self.test.assertIsNotNone(self.root_checksum, "Failed to read volume %s" % self.root_volume.id)

    def verify(self, volumes, pool):
        for volume in volumes:
            if volume.id == self.data_volume.id:
                self.test.assertTrue(self._data_pattern_present(),
                                     "The data of volume %s is not intact after the migration to %s" % (volume.id, pool.name))
            else:
                self.test.assertEqual(self._root_checksum(), self.root_checksum,
                                      "The data of volume %s is not intact after the migration to %s" % (volume.id, pool.name))
            if self.test.encrypted:
                self._verify_encryption(volume, pool)

    def _verify_encryption(self, volume, pool):
        """An RBD volume is encrypted natively by librbd (LUKS2), a volume on NFS by qemu (LUKS)"""
        ssh, instance_name = self._host_ssh()
        disk = self._disk(ssh, instance_name, volume)
        encryption = disk.find("encryption")
        if encryption is None:
            encryption = disk.find("source/encryption")
        self.test.assertIsNotNone(encryption, "Volume %s is not encrypted after the migration to %s" % (volume.id, pool.name))
        if pool.type == 'RBD':
            self.test.assertEqual(encryption.get("format"), "luks2")
            self.test.assertEqual(encryption.get("engine"), "librbd")
        else:
            self.test.assertEqual(encryption.get("format"), "luks")
            self.test.assertNotEqual(encryption.get("engine"), "librbd")


class TestVolumeMigrationRbdNfs(VolumeMigrationRbdNfsBase):
    """Migration of plain volumes between RBD and NFS pools on KVM"""

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_online_migration_from_rbd(self):
        """Online migration: RBD (ROOT with chain) -> NFS -> NFS -> RBD -> RBD"""
        self.online_migration_from_rbd()

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_online_migration_from_nfs_to_rbd(self):
        """Online migration: NFS (ROOT with chain) -> RBD"""
        self.online_migration_from_nfs(self.rbd_pool)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_online_migration_from_nfs_to_nfs(self):
        """Online migration: NFS (ROOT with chain) -> NFS"""
        self.require_pools(self.nfs_pool2)
        self.online_migration_from_nfs(self.nfs_pool2)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_offline_migration_from_rbd(self):
        """Offline migration: RBD (ROOT with chain) -> NFS -> NFS -> RBD -> RBD"""
        self.offline_migration_from_rbd()

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_05_offline_migration_from_nfs_to_rbd(self):
        """Offline migration: NFS (ROOT with chain) -> RBD"""
        self.offline_migration_from_nfs(self.rbd_pool)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_06_offline_migration_from_nfs_to_nfs(self):
        """Offline migration: NFS (ROOT with chain) -> NFS"""
        self.require_pools(self.nfs_pool2)
        self.offline_migration_from_nfs(self.nfs_pool2)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_07_detached_volume_migration(self):
        """Migration of a detached DATA volume: RBD -> NFS -> NFS -> RBD -> RBD"""
        self.detached_volume_migration()


class TestEncryptedVolumeMigrationRbdNfs(VolumeMigrationRbdNfsBase):
    """Migration of encrypted volumes between RBD and NFS pools on KVM"""

    encrypted = True

    @attr(tags=["advanced", "advancedns", "smoke", "diskencrypt"], required_hardware="true")
    def test_01_encrypted_online_migration_from_rbd(self):
        """Online migration of encrypted volumes: RBD (ROOT with chain) -> NFS -> NFS -> RBD -> RBD"""
        self.online_migration_from_rbd()

    @attr(tags=["advanced", "advancedns", "smoke", "diskencrypt"], required_hardware="true")
    def test_02_encrypted_online_migration_from_nfs_to_rbd(self):
        """Online migration of encrypted volumes: NFS (ROOT with chain) -> RBD"""
        self.online_migration_from_nfs(self.rbd_pool)

    @attr(tags=["advanced", "advancedns", "smoke", "diskencrypt"], required_hardware="true")
    def test_03_encrypted_online_migration_from_nfs_to_nfs(self):
        """Online migration of encrypted volumes: NFS (ROOT with chain) -> NFS"""
        self.require_pools(self.nfs_pool2)
        self.online_migration_from_nfs(self.nfs_pool2)

    @attr(tags=["advanced", "advancedns", "smoke", "diskencrypt"], required_hardware="true")
    def test_04_encrypted_offline_migration_from_rbd(self):
        """Offline migration of encrypted volumes: RBD (ROOT with chain) -> NFS -> NFS -> RBD -> RBD"""
        self.offline_migration_from_rbd()

    @attr(tags=["advanced", "advancedns", "smoke", "diskencrypt"], required_hardware="true")
    def test_05_encrypted_offline_migration_from_nfs_to_rbd(self):
        """Offline migration of encrypted volumes: NFS (ROOT with chain) -> RBD"""
        self.offline_migration_from_nfs(self.rbd_pool)

    @attr(tags=["advanced", "advancedns", "smoke", "diskencrypt"], required_hardware="true")
    def test_06_encrypted_offline_migration_from_nfs_to_nfs(self):
        """Offline migration of encrypted volumes: NFS (ROOT with chain) -> NFS"""
        self.require_pools(self.nfs_pool2)
        self.offline_migration_from_nfs(self.nfs_pool2)

    @attr(tags=["advanced", "advancedns", "smoke", "diskencrypt"], required_hardware="true")
    def test_07_encrypted_detached_volume_migration(self):
        """Migration of a detached encrypted DATA volume: RBD -> NFS -> NFS -> RBD -> RBD"""
        self.detached_volume_migration()
