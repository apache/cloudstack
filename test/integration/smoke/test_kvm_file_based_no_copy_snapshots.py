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
""" Smoke tests for KVM file-based no-copy volume snapshots (the [kvm.file.based.volume.snapshot.no.copy] setting).

With the setting enabled on an NFS, SharedMountPoint or local primary storage pool, a volume snapshot freezes the volume's
current QCOW2 file and moves the volume onto a new delta file backed by it, instead of copying the volume. Revert moves the
volume onto a new delta over the snapshot's file and delete merges the snapshot's file into its neighbour.

The tests write data to the volume files on the host with qemu-io while the Instance is stopped, and check after every
operation that the volume holds the expected data and that the files on the pool match what CloudStack reports.

Requirements:
 - a KVM zone with an NFS, SharedMountPoint or local (Filesystem) primary storage pool and a QCOW2 template;
 - [snapshot.backup.to.secondary] set to false (the setting is not dynamic, so the test does not change it);
 - qemu-io and qemu-img on the KVM hosts, and SSH access to them with the host credentials of the Marvin configuration.
"""
import threading
import time

from marvin.cloudstackAPI import createTemplate
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED
from marvin.lib.base import (Account,
                             Configurations,
                             DiskOffering,
                             Host,
                             Network,
                             NetworkOffering,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             VirtualMachine,
                             VmSnapshot,
                             Volume)
from marvin.lib.common import get_domain, get_suitable_test_template, get_zone
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

NO_COPY_SETTING = "kvm.file.based.volume.snapshot.no.copy"
BACKUP_TO_SECONDARY_SETTING = "snapshot.backup.to.secondary"
HOST_CAPABILITY = "host.kvm.diskonlysnapshot.singlevolume"
SUPPORTED_POOL_TYPES = ("NetworkFilesystem", "SharedMountPoint", "Filesystem")
# Setting values are cached by the management server for 30 seconds.
SETTING_CACHE_SECONDS = 35
MIB = 1024 * 1024


CONCURRENT_DELETE_REFUSALS = ("deleted or changed concurrently", "already destroyed")
INCREMENTAL_SNAPSHOT_SETTING = "kvm.incremental.snapshot"


class TestKvmFileBasedNoCopySnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        test_client = super(TestKvmFileBasedNoCopySnapshots, cls).getClsTestClient()
        cls.apiclient = test_client.getApiClient()
        cls.services = test_client.getParsedTestDataConfig()
        cls.hypervisor = test_client.getHypervisorInfo()
        cls._cleanup = []
        cls.pools_with_setting_changed = []
        cls.skip_reason = None
        cls.ssh_clients = {}
        try:
            cls.set_up_class(test_client)
        except Exception:
            # unittest does not call tearDownClass when setUpClass fails.
            cls.tearDownClass()
            raise

    @classmethod
    def set_up_class(cls, test_client):
        if cls.hypervisor.lower() != "kvm":
            cls.skip_reason = "KVM file-based no-copy snapshots are only supported on KVM"
            return

        cls.zone = get_zone(cls.apiclient, test_client.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)

        backup_to_secondary = Configurations.list(cls.apiclient, name=BACKUP_TO_SECONDARY_SETTING)[0].value
        if backup_to_secondary.lower() != "false":
            cls.skip_reason = "[%s] must be false (it is not dynamic, so the test does not change it)" % BACKUP_TO_SECONDARY_SETTING
            return

        pools = StoragePool.list(cls.apiclient, zoneid=cls.zone.id) or []
        cls.pools = [p for p in pools if p.type in SUPPORTED_POOL_TYPES and p.state == "Up"]
        if not cls.pools:
            cls.skip_reason = "No Up primary storage pool of type %s in the zone" % "/".join(SUPPORTED_POOL_TYPES)
            return

        hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, type="Routing", hypervisor="KVM", state="Up") or []
        if not hosts or any(str(h.details[HOST_CAPABILITY] if h.details else None).lower() != "true" for h in hosts):
            cls.skip_reason = "Every Up KVM host must advertise [%s] (a KVM agent with disk-only snapshot support)" % HOST_CAPABILITY
            return

        for cluster_id in set(p.clusterid for p in cls.pools if p.clusterid):
            if Configurations.list(cls.apiclient, name=INCREMENTAL_SNAPSHOT_SETTING, clusterid=cluster_id)[0].value.lower() == "true":
                cls.skip_reason = "[%s] is enabled in cluster %s; no-copy snapshots are not used with incremental snapshots" % (
                    INCREMENTAL_SNAPSHOT_SETTING, cluster_id)
                return

        cls.host_config = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        for host in hosts:
            tools = cls.ssh_to_host(host.id).execute("command -v qemu-io qemu-img virsh | wc -l")
            if not tools or tools[-1].strip() != "3":
                cls.skip_reason = "qemu-io, qemu-img and virsh are needed on KVM host %s for the checks" % host.name
                return

        template = get_suitable_test_template(cls.apiclient, cls.zone.id, cls.services["ostype"], cls.hypervisor)
        if template == FAILED:
            cls.skip_reason = "No suitable template for KVM in the zone"
            return

        cls.account = Account.create(cls.apiclient, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.account)
        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offering"])
        cls._cleanup.append(cls.service_offering)
        cls.disk_offering = DiskOffering.create(cls.apiclient, cls.services["disk_offering"])
        cls._cleanup.append(cls.disk_offering)

        l2_offering = NetworkOffering.list(cls.apiclient, name="DefaultL2NetworkOffering")[0]
        cls.network = Network.create(cls.apiclient, cls.services["l2-network"], accountid=cls.account.name,
                                     domainid=cls.account.domainid, networkofferingid=l2_offering.id, zoneid=cls.zone.id)

        cls.virtual_machine = VirtualMachine.create(cls.apiclient, cls.services["virtual_machine"], templateid=template.id,
                                                    accountid=cls.account.name, domainid=cls.account.domainid,
                                                    serviceofferingid=cls.service_offering.id, networkids=[cls.network.id],
                                                    zoneid=cls.zone.id, mode="advanced")

        # Changed last, so that nothing above can fail with the setting changed.
        global_value = Configurations.list(cls.apiclient, name=NO_COPY_SETTING)[0].value
        for pool in cls.pools:
            current = Configurations.list(cls.apiclient, name=NO_COPY_SETTING, storageid=pool.id)[0].value
            if current.lower() != "true":
                cls.pools_with_setting_changed.append((pool.id, current, global_value))
                Configurations.update(cls.apiclient, name=NO_COPY_SETTING, value="true", storageid=pool.id)
        if cls.pools_with_setting_changed:
            time.sleep(SETTING_CACHE_SECONDS)

    @classmethod
    def tearDownClass(cls):
        for client in cls.ssh_clients.values():
            client.close()
        try:
            super(TestKvmFileBasedNoCopySnapshots, cls).tearDownClass()
        finally:
            for pool_id, value, global_value in cls.pools_with_setting_changed:
                cls.restore_pool_setting(pool_id, value, global_value)

    @classmethod
    def restore_pool_setting(cls, pool_id, value, global_value):
        """Resets the pool's value when it was inherited from the global one, so that the pool keeps following later global changes."""
        if value == global_value:
            Configurations.reset(cls.apiclient, name=NO_COPY_SETTING, storageid=pool_id)
        else:
            Configurations.update(cls.apiclient, name=NO_COPY_SETTING, value=value, storageid=pool_id)

    def setUp(self):
        if self.skip_reason:
            self.skipTest(self.skip_reason)
        self.cleanup = []
        # Every test starts from a running Instance, even when an earlier test failed with it stopped.
        vm = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id, listall=True)[0]
        if vm.state != "Running":
            self.start_vm()

    # ---------------------------------------------------------------- helpers

    @classmethod
    def ssh_to_host(cls, host_id):
        """One SSH connection per host for the whole class, so the checks do not open a connection per command."""
        if host_id not in cls.ssh_clients:
            host = Host.list(cls.apiclient, id=host_id)[0]
            cls.ssh_clients[host_id] = SshClient(host.ipaddress, 22, cls.host_config["username"], cls.host_config["password"])
        return cls.ssh_clients[host_id]

    def any_host_with_pool(self, pool):
        hosts = Host.list(self.apiclient, zoneid=self.zone.id, type="Routing", hypervisor="KVM", state="Up") or []
        if pool.scope == "HOST":
            hosts = [h for h in hosts if h.ipaddress == pool.ipaddress]
        elif pool.scope == "CLUSTER":
            hosts = [h for h in hosts if h.clusterid == pool.clusterid]
        self.assertTrue(hosts, "No Up KVM host can reach pool %s" % pool.name)
        return hosts[0]

    def pool_dir(self, pool):
        if pool.type == "NetworkFilesystem":
            return "/mnt/%s" % pool.id
        return pool.path

    def volume(self, volume_id):
        return Volume.list(self.apiclient, id=volume_id, listall=True)[0]

    def volume_pool(self, volume):
        pool = StoragePool.list(self.apiclient, id=volume.storageid)[0]
        if pool.type not in SUPPORTED_POOL_TYPES:
            self.skipTest("Volume %s was placed on pool %s of type %s" % (volume.name, pool.name, pool.type))
        return pool

    def run_on_host(self, pool, command):
        ssh = self.ssh_to_host(self.any_host_with_pool(pool).id)
        return "\n".join(ssh.execute(command))

    def file_path(self, pool, file_name):
        return "%s/%s" % (self.pool_dir(pool), file_name)

    def file_exists(self, pool, file_name):
        return self.run_on_host(pool, "test -e %s && echo yes || echo no" % self.file_path(pool, file_name)).strip().endswith("yes")

    def assert_files_removed(self, pool, file_names, message):
        remaining = [f for f in file_names if self.file_exists(pool, f)]
        self.assertFalse(remaining, "%s: %s still on the pool" % (message, remaining))

    def backing_chain(self, pool, file_name):
        """File names of the QCOW2 chain from the given file down to its base."""
        out = self.run_on_host(pool, "qemu-img info -U --backing-chain %s" % self.file_path(pool, file_name))
        return [line.split(":", 1)[1].strip().rsplit("/", 1)[-1] for line in out.splitlines() if line.startswith("image:")]

    def assert_backed_by(self, pool, file_name, expected_backing_file, message):
        chain = self.backing_chain(pool, file_name)
        self.assertTrue(len(chain) > 1 and chain[1] == expected_backing_file, "%s: chain of %s is %s" % (message, file_name, chain))

    def assert_no_backing_file(self, pool, file_name, message):
        chain = self.backing_chain(pool, file_name)
        self.assertEqual(len(chain), 1, "%s: chain of %s is %s" % (message, file_name, chain))

    def assert_instance_uses_db_paths(self, message):
        """Every volume of the running Instance is used by its domain from the file CloudStack has for it, so no other disk moved."""
        vm = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id, listall=True)[0]
        self.assertEqual(vm.state, "Running")
        out = self.ssh_to_host(vm.hostid).execute("virsh domblklist %s" % vm.instancename)
        sources = [line.split()[1] for line in out[2:] if len(line.split()) > 1 and line.split()[1] != "-"]
        for volume in Volume.list(self.apiclient, virtualmachineid=vm.id, listall=True):
            self.assertTrue(any(source.endswith("/" + volume.path) for source in sources),
                            "%s: the domain does not use %s volume %s from %s: %s" % (message, volume.type, volume.name, volume.path, sources))

    def write_pattern(self, pool, file_name, pattern, offset):
        """Writes 1 MiB of the pattern to a volume file. The Instance using it must be stopped."""
        out = self.run_on_host(pool, "qemu-io -f qcow2 -c 'write -P %s %d 1M' %s 2>&1" % (pattern, offset, self.file_path(pool, file_name)))
        self.assertIn("wrote 1048576/1048576", out, "qemu-io write failed: %s" % out)

    def has_pattern(self, pool, file_name, pattern, offset):
        out = self.run_on_host(pool, "qemu-io -r -U -f qcow2 -c 'read -P %s %d 1M' %s 2>&1" % (pattern, offset, self.file_path(pool, file_name)))
        return "read 1048576/1048576" in out and "verification failed" not in out

    def assert_pattern(self, pool, file_name, pattern, offset, message):
        self.assertTrue(self.has_pattern(pool, file_name, pattern, offset), "%s: file %s does not hold pattern %s at offset %d"
                        % (message, file_name, pattern, offset))

    def stop_vm(self):
        self.virtual_machine.stop(self.apiclient)

    def start_vm(self):
        self.virtual_machine.start(self.apiclient)

    def take_snapshot(self, volume):
        started = time.time()
        snapshot = Snapshot.create(self.apiclient, volume.id, account=self.account.name, domainid=self.account.domainid)
        self.debug("Snapshot %s of volume %s took %.1f s" % (snapshot.id, volume.id, time.time() - started))
        self.assertEqual(snapshot.state, "BackedUp", "Snapshot %s is in state %s" % (snapshot.id, snapshot.state))
        return snapshot

    def assert_refused(self, call, message):
        with self.assertRaises(Exception) as context:
            call()
        self.assertIn("no-copy", str(context.exception), "%s was refused for another reason: %s" % (message, context.exception))

    def create_data_volume(self, name):
        """Creates a data volume attached to the Instance; it is removed with its snapshots when the test ends, pass or fail."""
        services = dict(self.services["volume"], diskname=name)
        volume = Volume.create(self.apiclient, services, zoneid=self.zone.id, account=self.account.name,
                               domainid=self.account.domainid, diskofferingid=self.disk_offering.id)
        self.addCleanup(self.remove_data_volume, volume.id)
        self.virtual_machine.attach_volume(self.apiclient, volume)
        return self.volume(volume.id)

    def remove_data_volume(self, volume_id):
        """Deletes the volume's snapshots first (so that later tests see no leftover no-copy snapshots), then detaches and expunges it."""
        volumes = Volume.list(self.apiclient, id=volume_id, listall=True)
        if not volumes:
            return
        for snapshot in Snapshot.list(self.apiclient, volumeid=volume_id, listall=True) or []:
            if snapshot.state != "Destroyed":
                Snapshot(snapshot.__dict__).delete(self.apiclient)
        volume = volumes[0]
        if volume.virtualmachineid:
            self.detach_with_instance_stopped(volume)
        Volume(volume.__dict__).destroy(self.apiclient, expunge=True)

    def detach_with_instance_stopped(self, volume):
        """Detaches with the Instance stopped: hot-detach depends on the guest acknowledging the unplug, which minimal templates may not do in time."""
        self.stop_vm()
        self.virtual_machine.detach_volume(self.apiclient, volume)
        self.start_vm()

    # ------------------------------------------------------------------ tests

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_take_revert_delete_on_a_running_instance(self):
        """A snapshot of a running Instance's volume is taken without a copy, reverts to the frozen data and merges back on delete"""
        volume = self.create_data_volume("nocopy-take-revert-delete")
        pool = self.volume_pool(volume)
        original_file = volume.path

        self.stop_vm()
        self.write_pattern(pool, original_file, "0xaa", 0)
        self.start_vm()

        snapshot = self.take_snapshot(volume)
        after_take = self.volume(volume.id)
        self.assertNotEqual(after_take.path, original_file, "The volume did not move onto a new delta file")
        self.assert_backed_by(pool, after_take.path, original_file, "The new delta file is not backed by the frozen volume file")
        self.assert_instance_uses_db_paths("After the take")

        self.stop_vm()
        self.write_pattern(pool, after_take.path, "0xbb", 0)
        self.assert_pattern(pool, original_file, "0xaa", 0, "The frozen file changed after the snapshot")

        Volume.revertToSnapshot(self.apiclient, snapshot.id)
        after_revert = self.volume(volume.id)
        self.assertNotIn(after_revert.path, (original_file, after_take.path), "Revert did not create a new delta file")
        self.assert_backed_by(pool, after_revert.path, original_file, "The reverted volume is not backed by the snapshot's file")
        self.assert_pattern(pool, after_revert.path, "0xaa", 0, "Revert did not restore the snapshot's data")
        self.assert_files_removed(pool, [after_take.path], "Revert left the discarded delta file")

        self.start_vm()
        snapshot.delete(self.apiclient)
        after_delete = self.volume(volume.id)
        self.assertEqual(after_delete.path, original_file, "Deleting the only snapshot did not merge the volume back into its file")
        self.assert_instance_uses_db_paths("After the live merge")
        self.stop_vm()
        self.assert_pattern(pool, original_file, "0xaa", 0, "Data lost by the merge")
        self.assert_no_backing_file(pool, original_file, "The data volume still has a backing file")
        self.assert_files_removed(pool, [after_revert.path], "The merge left the delta file")

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_chain_of_snapshots(self):
        """Takes and deletes, in any order, on a running and a stopped Instance keep the newest data of every block; only the latest snapshot can be reverted"""
        volume = self.create_data_volume("nocopy-chain")
        pool = self.volume_pool(volume)
        files = [volume.path]

        # Every layer writes its own block and overwrites block 0, so a merge in the wrong order or onto the wrong file shows in block 0.
        snapshots = []
        for index, pattern in enumerate(("0x11", "0x22", "0x33")):
            self.stop_vm()
            self.write_pattern(pool, files[-1], pattern, (index + 1) * MIB)
            self.write_pattern(pool, files[-1], pattern, 0)
            if index < 2:
                self.start_vm()
            snapshots.append(self.take_snapshot(self.volume(volume.id)))  # the third one with the Instance stopped
            files.append(self.volume(volume.id).path)
            if index < 2:
                self.assert_instance_uses_db_paths("After take %d" % (index + 1))
            self.assert_backed_by(pool, files[-1], files[-2], "Snapshot %d did not freeze the volume's file" % (index + 1))
        self.write_pattern(pool, files[-1], "0x44", 4 * MIB)
        self.write_pattern(pool, files[-1], "0x44", 0)

        self.assert_refused(lambda: Volume.revertToSnapshot(self.apiclient, snapshots[0].id), "Revert of an older snapshot")

        self.start_vm()
        snapshots[1].delete(self.apiclient)  # middle snapshot, Instance running: merge of a lower layer and rebase of the file above it
        self.assert_instance_uses_db_paths("After deleting the middle snapshot")
        self.stop_vm()
        snapshots[0].delete(self.apiclient)  # oldest snapshot while a newer one exists, Instance stopped
        self.start_vm()
        snapshots[2].delete(self.apiclient)  # last snapshot, Instance running: merge of the volume's active file
        self.assert_instance_uses_db_paths("After deleting the last snapshot")

        self.assertEqual(self.volume(volume.id).path, files[0], "Deleting every snapshot did not bring the volume back to its own file")
        self.stop_vm()
        for index, pattern in enumerate(("0x11", "0x22", "0x33", "0x44")):
            self.assert_pattern(pool, files[0], pattern, (index + 1) * MIB, "Data lost after deleting the snapshots")
        self.assert_pattern(pool, files[0], "0x44", 0, "A block written in several layers does not hold the newest data")
        self.assert_no_backing_file(pool, files[0], "The data volume still has a backing file")
        self.assert_files_removed(pool, files[1:], "The merges left delta files")

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_operations_that_would_break_the_chain_are_refused(self):
        """Volume or template from the snapshot, an Instance snapshot, hot-attach and storage migration are refused"""
        volume = self.create_data_volume("nocopy-guards")
        pool = self.volume_pool(volume)
        snapshot = self.take_snapshot(volume)

        self.assert_refused(lambda: Volume.create_from_snapshot(self.apiclient, snapshot.id, self.services["volume"],
                                                                account=self.account.name, domainid=self.account.domainid,
                                                                zoneid=self.zone.id),
                            "Creating a volume from the snapshot")

        def create_template():
            cmd = createTemplate.createTemplateCmd()
            cmd.snapshotid = snapshot.id
            cmd.name = cmd.displaytext = "nocopy-template"
            cmd.ostypeid = self.virtual_machine.ostypeid
            self.apiclient.createTemplate(cmd)
        self.assert_refused(create_template, "Creating a template from the snapshot")

        self.assert_refused(lambda: VmSnapshot.create(self.apiclient, self.virtual_machine.id, snapshotmemory="false"),
                            "An Instance snapshot")
        self.assertFalse(VmSnapshot.list(self.apiclient, virtualmachineid=self.virtual_machine.id, listall=True),
                         "A refused Instance snapshot left a record behind")

        self.detach_with_instance_stopped(volume)
        self.assert_refused(lambda: self.virtual_machine.attach_volume(self.apiclient, volume),
                            "Attaching a volume with no-copy snapshots to a running Instance")

        other_pools = [p for p in StoragePool.listForMigration(self.apiclient, id=volume.id) or [] if p.id != pool.id]
        if other_pools:
            self.assert_refused(lambda: Volume.migrate(self.apiclient, volumeid=volume.id, storageid=other_pools[0].id),
                                "Migrating the volume")
        else:
            self.debug("No other pool to migrate the volume to; storage migration not checked")

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_concurrent_deletes_of_one_snapshot(self):
        """Two concurrent deletes of the same snapshot: exactly one succeeds and the volume ends on its own file"""
        volume = self.create_data_volume("nocopy-concurrent")
        pool = self.volume_pool(volume)
        original_file = volume.path
        snapshot = self.take_snapshot(volume)
        delta_file = self.volume(volume.id).path

        user_apiclient = self.testClient.getUserApiClient(UserName=self.account.name, DomainName=self.account.domain)
        self.assertNotEqual(user_apiclient, FAILED, "Could not create an API client for the test account")
        results = []

        def delete(apiclient):
            try:
                snapshot.delete(apiclient)
                results.append("deleted")
            except Exception as e:
                results.append("refused: %s" % e)

        threads = [threading.Thread(target=delete, args=(client,)) for client in (self.apiclient, user_apiclient)]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join(600)
        self.assertFalse([t for t in threads if t.is_alive()], "A delete did not finish within 10 minutes")

        self.assertEqual(results.count("deleted"), 1, "Expected exactly one delete to succeed: %s" % results)
        refusal = next(r for r in results if r != "deleted")
        self.assertTrue(any(text in refusal for text in CONCURRENT_DELETE_REFUSALS), "The other delete failed for another reason: %s" % refusal)
        current = self.volume(volume.id)
        self.assertEqual(current.state, "Ready")
        self.assertEqual(current.path, original_file, "The volume is not back on its own file")
        self.assert_no_backing_file(pool, original_file, "The data volume still has a backing file")
        self.assert_files_removed(pool, [delta_file], "The merge left the delta file")

    @attr(tags=["advanced"], required_hardware="true")
    def test_05_expunging_a_volume_removes_its_snapshot_files(self):
        """Expunging a volume with no-copy snapshots leaves none of its files on the pool"""
        volume = self.create_data_volume("nocopy-expunge")
        pool = self.volume_pool(volume)
        files = [volume.path]
        for _ in range(2):
            self.take_snapshot(self.volume(volume.id))
            files.append(self.volume(volume.id).path)
        self.assertEqual(len(set(files)), 3)

        self.detach_with_instance_stopped(volume)
        Volume(volume.__dict__).destroy(self.apiclient, expunge=True)

        # The frozen files are deleted while the volume is expunged, so they must be gone as soon as the call returns.
        self.assert_files_removed(pool, files, "Files of the expunged volume")
        self.assertFalse([s for s in Snapshot.list(self.apiclient, volumeid=volume.id, listall=True) or [] if s.state != "Destroyed"],
                         "Snapshots of the expunged volume are still listed")

    @attr(tags=["advanced"], required_hardware="true")
    def test_06_setting_disabled_takes_regular_snapshots(self):
        """With the setting disabled on the pool, a snapshot is a regular copy and the volume does not move"""
        volume = self.create_data_volume("nocopy-disabled")
        pool = self.volume_pool(volume)
        previous = Configurations.list(self.apiclient, name=NO_COPY_SETTING, storageid=pool.id)[0].value
        global_value = Configurations.list(self.apiclient, name=NO_COPY_SETTING)[0].value
        Configurations.update(self.apiclient, name=NO_COPY_SETTING, value="false", storageid=pool.id)
        try:
            time.sleep(SETTING_CACHE_SECONDS)
            snapshot = self.take_snapshot(volume)
            self.assertEqual(self.volume(volume.id).path, volume.path, "A regular snapshot moved the volume")
            snapshot.delete(self.apiclient)
        finally:
            self.restore_pool_setting(pool.id, previous, global_value)
            time.sleep(SETTING_CACHE_SECONDS)
