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

import json
import logging
import os
import random

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries
from marvin.lib.base import Account, Configurations, ServiceOffering, \
    Snapshot, StoragePool, Template, User, VirtualMachine, Volume
from marvin.lib.common import get_domain, get_zone, list_hosts, list_virtual_machines, list_volumes
from marvin.lib.utils import cleanup_resources
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

from linstor_test_utils import ServiceReady, get_guest_template

# Prerequisites:
#  Only one zone / pod / cluster
#  Only KVM hypervisor (Linstor only supports KVM)
#  One Linstor storage pool (resource group without a LUKS layer; plain volumes)
#  NFS secondary storage (incremental snapshots are only supported on NFS secondary)
#  'lin.backup.snapshots' enabled (default true) so snapshots are backed up to secondary storage.
#  'kvm.snapshot.enabled' enabled (default true): snapshots are taken on running VMs (the tests
#    set it). Markers are synced before each snapshot, so the crash-consistent images contain them.
#
# What this exercises (the Linstor incremental snapshot feature):
#  Volumes on Linstor primary storage back their snapshots up to secondary storage as qcow2 files.
#  With kvm.incremental.snapshot enabled, every snapshot after a full one is stored as a delta
#  qcow2 (a content diff produced by qemu-img rebase) whose backing file is the previous snapshot,
#  so a chain member only consumes the space of the blocks that changed since its parent.
#
#  * chaining:  full -> delta -> delta, parent links recorded on the image store refs
#  * rotation:  snapshot.delta.max caps the chain length, then a new full is taken
#  * restore:   reverting to a chain member follows the backing chain
#  * flatten:   templates/volumes created from a mid-chain delta get standalone content
#  * fallback:  a missing parent file on secondary storage degrades to a full backup
#  * exclusion: encrypted volumes are always backed up as full copies
#
# Note on verification: the parent/child relationship of the backups is bookkeeping internal to the
# management server (snapshot_store_ref.parent_snapshot_id), not exposed through the API, so these
# tests read it from the DB where a DB connection is available and otherwise fall back to comparing
# physical sizes (a delta of a small change is orders of magnitude smaller than a full).

MARKER_PATH = "/root/cs_incr_marker.txt"


class TestData:
    account = "account"
    computeOffering = "computeoffering"
    domainId = "domainId"
    hypervisor = "hypervisor"
    provider = "provider"
    scope = "scope"
    storageTag = "linstor"
    tags = "tags"
    user = "user"
    virtualMachine = "virtualmachine"
    zoneId = "zoneId"

    def __init__(self, linstor_controller_url):
        self.testdata = {
            TestData.account: {
                "email": "test-incr@test.com",
                "firstname": "John",
                "lastname": "Doe",
                "username": "test-incr",
                "password": "test"
            },
            TestData.user: {
                "email": "user-incr@test.com",
                "firstname": "Jane",
                "lastname": "Doe",
                "username": "test-incr-user",
                "password": "password"
            },
            "primarystorage": {
                "name": "LinstorIncrPool-%d" % random.randint(0, 100000),
                TestData.scope: "ZONE",
                "url": linstor_controller_url,
                TestData.provider: "Linstor",
                TestData.tags: TestData.storageTag,
                TestData.hypervisor: "KVM",
                "details": {
                    "resourceGroup": "acs-basic"
                }
            },
            TestData.virtualMachine: {
                "name": "TestIncrVM",
                "displayname": "Test Incremental VM"
            },
            TestData.computeOffering: {
                "name": "Linstor_Compute_Incr",
                "displaytext": "Linstor_Compute_Incr",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                TestData.tags: TestData.storageTag
            },
            TestData.zoneId: 1,
            TestData.domainId: 1,
        }


class TestLinstorIncrementalSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testclient = super(TestLinstorIncrementalSnapshots, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.dbConnection = testclient.getDbConnection()

        cls._cleanup = []
        cls.skip_reason = None
        cls.original_config = {}

        # The first host runs the Linstor controller (per the Linstor test prerequisites).
        first_host = list_hosts(cls.apiClient)[0]
        cls.testdata = TestData(first_host.ipaddress).testdata

        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])
        cls.template = get_guest_template(cls.apiClient, cls.zone.id, hypervisor="KVM")

        # Host SSH credentials, needed by the tests that inspect or manipulate the backed-up qcow2
        # files on secondary storage. A full marvin config carries these under
        # zones->pods->clusters->hosts; a lightweight config may omit them - fall back to
        # HOST_SSH_USER / HOST_SSH_PASSWORD env vars, and skip those tests if neither is present.
        cls.hostConfig = None
        try:
            cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0] \
                .__dict__["hosts"][0].__dict__
        except (KeyError, IndexError, AttributeError, TypeError):
            host_user = os.environ.get("HOST_SSH_USER")
            host_pass = os.environ.get("HOST_SSH_PASSWORD")
            if host_user and host_pass:
                cls.hostConfig = {"username": host_user, "password": host_pass}

        # The feature under test needs snapshot backups on secondary storage and the (cluster-scoped,
        # here globally set) incremental snapshot switch. Save the original values and restore them in
        # tearDownClass so the run leaves no configuration behind.
        cls._set_config("lin.backup.snapshots", "true")
        cls._set_config("kvm.incremental.snapshot", "true")
        # all snapshots in these tests are taken while the VM is running (crash-consistent
        # storage snapshots; also exercises the running-VM path of the feature)
        cls._set_config("kvm.snapshot.enabled", "true")

        primarystorage = cls.testdata["primarystorage"]
        api_token = os.environ.get("LINSTOR_API_TOKEN")
        if api_token:
            primarystorage["details"]["lin.auth.apitoken"] = api_token

        try:
            cls.primary_storage = StoragePool.create(
                cls.apiClient,
                primarystorage,
                scope=primarystorage[TestData.scope],
                zoneid=cls.zone.id,
                provider=primarystorage[TestData.provider],
                tags=primarystorage[TestData.tags],
                hypervisor=primarystorage[TestData.hypervisor]
            )
        except Exception as e:
            cls.skip_reason = (
                "Could not register the Linstor primary storage pool (%s). If the Linstor controller "
                "requires authentication, set the LINSTOR_API_TOKEN env var to a valid controller API "
                "token before running these tests." % e)
            return

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        cls.account = Account.create(cls.apiClient, cls.testdata[TestData.account], admin=1)
        cls.user = User.create(
            cls.apiClient, cls.testdata[TestData.user],
            account=cls.account.name, domainid=cls.domain.id)

        cls._cleanup = [
            cls.compute_offering,
            cls.user,
            cls.account,
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)
            if getattr(cls, "primary_storage", None) is not None:
                cls.primary_storage.delete(cls.apiClient)
        except Exception as e:
            logging.debug("Exception in tearDownClass: %s" % e)
        finally:
            cls._restore_configs()

    def setUp(self):
        if self.skip_reason:
            self.skipTest(self.skip_reason)
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiClient, self.cleanup)

    # --------------------------------------------------------------------- #
    # Tests
    # --------------------------------------------------------------------- #

    @attr(tags=['basic'], required_hardware=True)
    def test_01_second_snapshot_is_incremental(self):
        """The first snapshot is a full backup, later ones are deltas chained to their parent."""
        vm = self._deploy_vm("TestIncrVM-chain")

        self._write_marker(vm, "incr-chain-v1")
        snap_full = self._snapshot_root_volume(vm)
        self._assert_full(snap_full)

        self._write_marker(vm, "incr-chain-v2")
        snap_delta1 = self._snapshot_root_volume(vm)
        self._assert_delta(snap_delta1, parent=snap_full)

        self._write_marker(vm, "incr-chain-v3")
        snap_delta2 = self._snapshot_root_volume(vm)
        self._assert_delta(snap_delta2, parent=snap_delta1, full=snap_full)

        # a delta of a one-line change must be far smaller than the full backup
        self.assertLess(
            int(snap_delta1.physicalsize) * 4, int(snap_full.physicalsize),
            "Delta snapshot (%s bytes) is not substantially smaller than the full one (%s bytes)"
            % (snap_delta1.physicalsize, snap_full.physicalsize))

        # if we can reach the files on secondary storage, the delta must be a qcow2 whose
        # backing file is its parent, referenced by a relative name (portable across mounts)
        info = self._qemu_img_info_of_backed_up_snapshot(snap_delta1)
        if info is not None:
            backing = info.get("backing-filename")
            self.assertIsNotNone(backing, "Delta snapshot qcow2 has no backing file: %s" % json.dumps(info))
            self.assertNotIn("/", backing, "Backing file %r is not a relative name" % backing)

    @attr(tags=['basic'], required_hardware=True)
    def test_02_revert_to_delta_snapshot(self):
        """Reverting to a mid-chain delta restores exactly that snapshot's content."""
        vm = self._deploy_vm("TestIncrVM-revert")

        self._write_marker(vm, "incr-revert-v1")
        snap1 = self._snapshot_root_volume(vm)

        self._write_marker(vm, "incr-revert-v2")
        snap2 = self._snapshot_root_volume(vm)
        self._assert_delta(snap2, parent=snap1)

        # change the data once more so a successful revert is detectable
        self._write_marker(vm, "incr-revert-v3-CHANGED")
        vm.stop(self.apiClient)

        # revert to the delta: the restore has to follow the backing chain (full + delta)
        Volume.revertToSnapshot(self.apiClient, snap2.id)
        self._start_vm(vm)
        self.assertEqual("incr-revert-v2", self._read_marker(vm),
                         "Revert to the delta snapshot did not restore its content")

        # revert to the chain-starting full as well
        vm.stop(self.apiClient)
        Volume.revertToSnapshot(self.apiClient, snap1.id)
        self._start_vm(vm)
        self.assertEqual("incr-revert-v1", self._read_marker(vm),
                         "Revert to the full snapshot did not restore its content")

    @attr(tags=['basic'], required_hardware=True)
    def test_03_delta_max_rotates_chain(self):
        """After snapshot.delta.max chain members the chain is ended and a new full is taken."""
        original = Configurations.list(self.apiClient, name="snapshot.delta.max")[0].value
        Configurations.update(self.apiClient, name="snapshot.delta.max", value="2")
        try:
            vm = self._deploy_vm("TestIncrVM-rotate")

            snap1 = self._snapshot_root_volume(vm)   # chain member 1: full
            snap2 = self._snapshot_root_volume(vm)   # chain member 2: delta, reaches the cap
            snap3 = self._snapshot_root_volume(vm)   # must start a new chain: full

            self._assert_full(snap1)
            self._assert_delta(snap2, parent=snap1)
            self._assert_full(snap3, full=snap1)

            ref2 = self._image_store_ref(snap2)
            if ref2 is not None:
                self.assertTrue(ref2["end_of_chain"],
                                "Snapshot that reached snapshot.delta.max was not marked end of chain")
        finally:
            Configurations.update(self.apiClient, name="snapshot.delta.max", value=original)

    @attr(tags=['basic'], required_hardware=True)
    def test_04_template_from_delta_snapshot(self):
        """A template created from a mid-chain delta must contain the full (flattened) content."""
        vm = self._deploy_vm("TestIncrVM-tmpl")

        self._write_marker(vm, "incr-tmpl-v1")
        snap1 = self._snapshot_root_volume(vm)

        self._write_marker(vm, "incr-tmpl-v2")
        snap2 = self._snapshot_root_volume(vm)
        self._assert_delta(snap2, parent=snap1)

        template = Template.create_from_snapshot(
            self.apiClient, snap2,
            {
                "name": "incr-tmpl",
                "displaytext": "template from delta snapshot",
                "ostypeid": self.template.ostypeid,
                "ispublic": False,
            })
        self.cleanup.insert(0, template)

        # a VM deployed from that template must carry the delta's content: the template was
        # created from a delta qcow2 and only works if the chain got flattened along the way
        vm_from_template = VirtualMachine.create(
            self.apiClient,
            {"name": "TestIncrVM-fromtmpl", "displayname": "TestIncrVM-fromtmpl"},
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=template.id,
            domainid=self.domain.id,
            startvm=True,
            mode='basic',
        )
        self.cleanup.insert(0, vm_from_template)
        # the template holds a crash-consistent image (snapshot of a running VM), so the first
        # boot replays the filesystem journal / fscks and can take considerably longer
        self._start_vm(vm_from_template, ssh_timeout=600)
        self.assertEqual("incr-tmpl-v2", self._read_marker(vm_from_template),
                         "VM from the delta-snapshot template does not contain the snapshot's content")

    @attr(tags=['basic'], required_hardware=True)
    def test_05_full_backup_when_parent_file_missing(self):
        """If the parent file vanished from secondary storage, the next snapshot degrades to a full."""
        if not self.hostConfig:
            self.skipTest("No host SSH credentials available (set HOST_SSH_USER/HOST_SSH_PASSWORD or "
                          "provide them in the marvin config) - cannot manipulate secondary storage")

        vm = self._deploy_vm("TestIncrVM-fallback")

        snap1 = self._snapshot_root_volume(vm, add_to_cleanup=False)
        self._assert_full(snap1)

        # remove the would-be parent behind CloudStack's back
        if not self._delete_backed_up_snapshot_file(snap1):
            self.skipTest("Could not remove the backed-up snapshot from secondary storage")

        # the driver still offers snap1 as parent (its ref is Ready), but the agent must
        # notice the missing file, fall back to a full copy and report it as such, upon
        # which the management server drops the parent link again
        snap2 = self._snapshot_root_volume(vm)
        self._assert_full(snap2, full=snap1)

        # snap1's backing file is gone; delete it via the API and tolerate the missing file
        try:
            Snapshot.delete(snap1, self.apiClient)
        except Exception as e:
            logging.debug("Deleting the sabotaged snapshot failed (tolerated): %s" % e)

    @attr(tags=['basic'], required_hardware=True)
    def test_06_delete_mid_chain_member(self):
        """Deleting a mid-chain delta keeps its children restorable (leaf-first physical deletion)."""
        vm = self._deploy_vm("TestIncrVM-del")

        self._write_marker(vm, "incr-del-v1")
        snap1 = self._snapshot_root_volume(vm)

        self._write_marker(vm, "incr-del-v2")
        snap2 = self._snapshot_root_volume(vm, add_to_cleanup=False)
        self._assert_delta(snap2, parent=snap1)

        self._write_marker(vm, "incr-del-v3")
        snap3 = self._snapshot_root_volume(vm)
        self._assert_delta(snap3, parent=snap2, full=snap1)

        # snap3's delta is backed by snap2's file: deleting snap2 must not break snap3
        Snapshot.delete(snap2, self.apiClient)

        vm.stop(self.apiClient)

        Volume.revertToSnapshot(self.apiClient, snap3.id)
        self._start_vm(vm)
        self.assertEqual("incr-del-v3", self._read_marker(vm),
                         "Revert to a delta broke after its parent snapshot was deleted")

    @attr(tags=['basic'], required_hardware=True)
    def test_07_encrypted_volumes_stay_full(self):
        """Snapshots of encrypted volumes are never incremental (a delta would need the LUKS secret)."""
        if not self._encryption_capable_host_exists():
            self.skipTest("No KVM host with volume-encryption support found")

        offering_data = dict(self.testdata[TestData.computeOffering])
        offering_data["name"] = offering_data["displaytext"] = "Linstor_Compute_Incr_Enc"
        offering = ServiceOffering.create(self.apiClient, offering_data, encryptroot=True)
        self.cleanup.append(offering)

        vm = VirtualMachine.create(
            self.apiClient,
            {"name": "TestIncrVM-enc", "displayname": "TestIncrVM-enc"},
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=False,
            mode='basic',
        )
        self.cleanup.insert(0, vm)
        # the root volume is only provisioned on first start; a never-started
        # VM's volume stays Allocated and cannot be snapshotted
        self._start_vm(vm)

        snap1 = self._snapshot_root_volume(vm)
        snap2 = self._snapshot_root_volume(vm)

        self._assert_full(snap1)
        self._assert_full(snap2, full=snap1)

    # --------------------------------------------------------------------- #
    # Helpers
    # --------------------------------------------------------------------- #

    @classmethod
    def _set_config(cls, name, value):
        """Set a global configuration, remembering the original value for tearDownClass."""
        if name not in cls.original_config:
            cls.original_config[name] = Configurations.list(cls.apiClient, name=name)[0].value
        Configurations.update(cls.apiClient, name=name, value=value)

    @classmethod
    def _restore_configs(cls):
        for name, value in cls.original_config.items():
            try:
                Configurations.update(cls.apiClient, name=name, value=value)
            except Exception as e:
                logging.debug("Could not restore configuration %s=%s: %s" % (name, value, e))

    def _deploy_vm(self, name):
        vm = VirtualMachine.create(
            self.apiClient,
            {"name": name, "displayname": name},
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=False,
            mode='basic',
        )
        self.cleanup.insert(0, vm)
        self._start_vm(vm)
        return vm

    def _snapshot_root_volume(self, vm, add_to_cleanup=True):
        root = list_volumes(self.apiClient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        snapshot = Snapshot.create(
            self.apiClient,
            volume_id=root.id,
            account=self.account.name,
            domainid=self.domain.id,
        )
        self.assertIsNotNone(snapshot, "Could not create snapshot of root volume")
        if add_to_cleanup:
            self.cleanup.insert(0, snapshot)
        return snapshot

    def _image_store_ref(self, snapshot):
        """The snapshot's image-store reference bookkeeping, or None if the DB is not reachable.

        The parent link and end-of-chain flag are internal to the management server and not part of
        any API response, so they can only be checked directly in the database.
        """
        try:
            rows = self.dbConnection.execute(
                "SELECT ss.parent_snapshot_id, ss.end_of_chain, ss.physical_size, ss.install_path "
                "FROM snapshot_store_ref ss JOIN snapshots s ON s.id = ss.snapshot_id "
                "WHERE s.uuid = '%s' AND ss.store_role = 'Image' AND ss.state != 'Destroyed'"
                % snapshot.id)
        except Exception as e:
            logging.debug("DB lookup of the image store ref failed: %s" % e)
            return None
        if not rows:
            return None
        return {
            "parent_snapshot_id": rows[0][0],
            "end_of_chain": bool(rows[0][1]),
            "physical_size": rows[0][2],
            "install_path": rows[0][3],
        }

    def _db_snapshot_id(self, snapshot):
        rows = self.dbConnection.execute("SELECT id FROM snapshots WHERE uuid = '%s'" % snapshot.id)
        return rows[0][0] if rows else None

    def _assert_full(self, snapshot, full=None):
        """Assert the snapshot was backed up as a standalone full copy.

        Without DB access the parent link cannot be checked; if a reference full backup of the
        same volume is given, fall back to comparing physical sizes (a full is in the order of
        the volume's used space, a delta of a small change is orders of magnitude smaller).
        """
        ref = self._image_store_ref(snapshot)
        if ref is not None:
            self.assertEqual(
                0, ref["parent_snapshot_id"],
                "Snapshot %s should be a full backup but has parent %s" % (snapshot.name, ref["parent_snapshot_id"]))
        elif full is not None:
            self.assertGreater(
                int(snapshot.physicalsize) * 2, int(full.physicalsize),
                "Snapshot %s (%s bytes) does not look like a full backup (reference full %s is %s bytes)"
                % (snapshot.name, snapshot.physicalsize, full.name, full.physicalsize))
        else:
            logging.debug("No DB access - cannot verify that snapshot %s is a full backup" % snapshot.name)

    def _assert_delta(self, snapshot, parent, full=None):
        """Assert the snapshot was backed up as a delta of the given parent.

        Without DB access the parent link cannot be checked; the fallback compares the size
        against the chain's full backup (never against the parent - that may itself be a
        similarly sized delta).
        """
        ref = self._image_store_ref(snapshot)
        if ref is not None:
            self.assertEqual(
                self._db_snapshot_id(parent), ref["parent_snapshot_id"],
                "Snapshot %s should be an incremental backup of %s" % (snapshot.name, parent.name))
        else:
            if full is None:
                full = parent
            self.assertLess(
                int(snapshot.physicalsize) * 4, int(full.physicalsize),
                "Snapshot %s (%s bytes) does not look like a delta (full backup %s is %s bytes)"
                % (snapshot.name, snapshot.physicalsize, full.name, full.physicalsize))

    def _vm_ssh(self, vm):
        # The VM is deployed stopped, so its instance has no ssh_ip yet; the IP may also change across
        # stop/start cycles. Always pass the current address from a fresh lookup.
        ipaddress = self._get_vm(vm.id).ipaddress
        return vm.get_ssh_client(ipaddress=ipaddress, reconnect=True, retries=5)

    def _write_marker(self, vm, content):
        ssh = self._vm_ssh(vm)
        ssh.execute("echo '%s' > %s" % (content, MARKER_PATH))
        ssh.execute("sync")

    def _read_marker(self, vm):
        ssh = self._vm_ssh(vm)
        result = ssh.execute("cat %s" % MARKER_PATH)
        return result[0].strip() if result else None

    @classmethod
    def _encryption_capable_host_exists(cls):
        hosts = list_hosts(cls.apiClient, zoneid=cls.zone.id, type='Routing', hypervisor='KVM', state='Up')
        return any(getattr(h, "encryptionsupported", False) for h in (hosts or []))

    @classmethod
    def _get_vm(cls, vm_id):
        return list_virtual_machines(cls.apiClient, id=vm_id)[0]

    @classmethod
    def _start_vm(cls, vm, ssh_timeout=120):
        vm_for_check = cls._get_vm(vm.id)
        if vm_for_check.state == VirtualMachine.STOPPED:
            vm.start(cls.apiClient)
            vm_for_check = cls._get_vm(vm.id)
        ServiceReady.wait_ssh_ready(vm_for_check.ipaddress, timeout=ssh_timeout)
        return vm_for_check

    def _host_ssh(self):
        host = list_hosts(self.apiClient, type='Routing', hypervisor='KVM', state='Up')[0]
        return SshClient(
            host=host.ipaddress, port=22,
            user=self.hostConfig['username'], passwd=self.hostConfig['password'])

    def _on_mounted_secondary(self, snapshot, action):
        """Self-mount the secondary NFS export on a host and run action(ssh, snapshot_path) on it."""
        ref = self._image_store_ref(snapshot)
        if ref is None or not ref["install_path"]:
            return None
        try:
            store = self.dbConnection.execute(
                "SELECT url FROM image_store WHERE role = 'Image' AND removed IS NULL LIMIT 1")
        except Exception as e:
            logging.debug("DB lookup of the image store url failed: %s" % e)
            return None
        if not store or not store[0][0] or not store[0][0].startswith("nfs://"):
            return None
        server, export = store[0][0][len("nfs://"):].split("/", 1)

        ssh = self._host_ssh()
        mount_point = "/tmp/cs_sectest_%d" % random.randint(0, 100000)
        try:
            ssh.execute("mkdir -p %s" % mount_point)
            ssh.execute("mount -t nfs %s:/%s %s" % (server, export, mount_point))
            return action(ssh, "%s/%s" % (mount_point, ref["install_path"]))
        except Exception as e:
            logging.debug("Action on mounted secondary storage failed: %s" % e)
            return None
        finally:
            ssh.execute("umount %s 2>/dev/null; rmdir %s 2>/dev/null" % (mount_point, mount_point))

    def _qemu_img_info_of_backed_up_snapshot(self, snapshot):
        if not self.hostConfig:
            return None

        def qemu_img_info(ssh, path):
            out = ssh.execute("qemu-img info --output=json %s" % path)
            return json.loads("".join(out)) if out else None

        return self._on_mounted_secondary(snapshot, qemu_img_info)

    def _delete_backed_up_snapshot_file(self, snapshot):
        def delete_file(ssh, path):
            ssh.execute("rm -f %s" % path)
            return True

        return bool(self._on_mounted_secondary(snapshot, delete_file))
