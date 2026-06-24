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
import socket
import time

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries
from marvin.cloudstackAPI import createVolume
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.base import Account, Configurations, Host, ServiceOffering, \
    Snapshot, StoragePool, User, VirtualMachine, Volume
from marvin.lib.common import get_domain, get_template, get_zone, list_hosts, list_virtual_machines, list_volumes
from marvin.lib.utils import cleanup_resources
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

# Prerequisites:
#  Only one zone / pod / cluster
#  Only KVM hypervisor (Linstor only supports KVM)
#  At least one KVM host with volume-encryption support (host.encryptionsupported == True),
#    i.e. cryptsetup/qemu with LUKS available on the host.
#  One Linstor storage pool whose resource-group can add a LUKS layer (encrypted volumes).
#  'lin.backup.snapshots' enabled (default true) so snapshots are backed up to secondary storage
#    as qcow2 -- that is the path these tests are meant to exercise. With it disabled, snapshots
#    stay on primary as Linstor system snapshots and a different (rollback) code path is used.
#
# What this exercises (the encrypted-snapshot round trip):
#  * backup:  decrypted DRBD device  -> LUKS-encrypted qcow2 on secondary
#  * revert:  encrypted qcow2        -> decrypted, written to the DRBD device (Linstor re-encrypts)
#  * create:  encrypted qcow2        -> new volume via createVolumeFromSnapshot (KVMStorageProcessor)
#
# Note on verification: Linstor encrypts inside the DRBD stack (LUKS layer), so the libvirt domain
# XML does NOT carry <encryption format='luks'> like hypervisor-based encryption does. Correctness
# is therefore verified by a data round trip (write marker -> snapshot -> change -> restore -> read),
# and encryption-at-rest is verified by inspecting the backed-up qcow2 with 'qemu-img info'.

MARKER_PATH = "/root/cs_enc_marker.txt"


class TestData:
    account = "account"
    computeOffering = "computeoffering"
    diskName = "diskname"
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
                "email": "test-enc@test.com",
                "firstname": "John",
                "lastname": "Doe",
                "username": "test-enc",
                "password": "test"
            },
            TestData.user: {
                "email": "user-enc@test.com",
                "firstname": "Jane",
                "lastname": "Doe",
                "username": "test-enc-user",
                "password": "password"
            },
            "primarystorage": {
                "name": "LinstorEncPool-%d" % random.randint(0, 100000),
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
                "name": "TestEncVM",
                "displayname": "Test Encrypted VM"
            },
            # encryptroot=True is passed as a create kwarg, not in this dict
            TestData.computeOffering: {
                "name": "Linstor_Compute_Encrypted",
                "displaytext": "Linstor_Compute_Encrypted",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                TestData.tags: TestData.storageTag
            },
            TestData.diskName: "restored-from-enc-snap",
            TestData.zoneId: 1,
            TestData.domainId: 1,
        }


class ServiceReady:
    @classmethod
    def ready(cls, hostname, port):
        try:
            s = socket.create_connection((hostname, port), timeout=1)
            s.close()
            return True
        except (ConnectionRefusedError, socket.timeout, OSError):
            return False

    @classmethod
    def wait(cls, hostname, port, wait_interval=5, timeout=120, service_name='ssh'):
        starttime = int(round(time.time() * 1000))
        while not cls.ready(hostname, port):
            if starttime + timeout * 1000 < int(round(time.time() * 1000)):
                raise RuntimeError("{s} {h} cannot be reached.".format(s=service_name, h=hostname))
            time.sleep(wait_interval)
        return True

    @classmethod
    def wait_ssh_ready(cls, hostname, wait_interval=2, timeout=120):
        return cls.wait(hostname, 22, wait_interval, timeout, "ssh")


class TestLinstorEncryptedSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testclient = super(TestLinstorEncryptedSnapshots, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.dbConnection = testclient.getDbConnection()

        cls._cleanup = []
        cls.skip_reason = None

        # Linstor is KVM-only, so the hypervisor type is not probed via getHypervisorInfo() (which is
        # only populated when nosetests is invoked with --hypervisor). Instead we require an actual KVM
        # host that supports volume encryption below.

        # The first host runs the Linstor controller (per the Linstor test prerequisites).
        first_host = list_hosts(cls.apiClient)[0]
        cls.testdata = TestData(first_host.ipaddress).testdata

        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])
        cls.template = get_template(cls.apiClient, cls.zone.id, hypervisor="KVM")

        # Host SSH credentials, only needed by test_03 to inspect the backed-up qcow2 on secondary
        # storage. A full marvin config carries these under zones->pods->clusters->hosts, but a
        # lightweight config may omit them; in that case fall back to HOST_SSH_USER / HOST_SSH_PASSWORD
        # env vars. Never fail class setup over this - the other tests don't need host SSH.
        cls.hostConfig = None
        try:
            cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0] \
                .__dict__["hosts"][0].__dict__
        except (KeyError, IndexError, AttributeError, TypeError):
            host_user = os.environ.get("HOST_SSH_USER")
            host_pass = os.environ.get("HOST_SSH_PASSWORD")
            if host_user and host_pass:
                cls.hostConfig = {"username": host_user, "password": host_pass}

        if not cls._encryption_capable_host_exists():
            cls.skip_reason = "No KVM host with volume-encryption support found"
            return

        # Ensure snapshots are backed up to secondary storage (the path under test).
        Configurations.update(cls.apiClient, name="lin.backup.snapshots", value="true")

        primarystorage = cls.testdata["primarystorage"]
        # Registering the pool makes the management server call the Linstor controller (to read the
        # resource-group capacity). If the controller enforces authentication, that call needs an API
        # token, supplied as the 'lin.auth.apitoken' add-pool detail. Provide it via LINSTOR_API_TOKEN
        # so it is never hard-coded; leave it unset for an unauthenticated controller.
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

        # Compute offering with encrypted root, pinned to the Linstor pool via the storage tag.
        cls.compute_offering_encrypted = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering],
            encryptroot=True
        )

        cls.account = Account.create(cls.apiClient, cls.testdata[TestData.account], admin=1)
        cls.user = User.create(
            cls.apiClient, cls.testdata[TestData.user],
            account=cls.account.name, domainid=cls.domain.id)

        cls._cleanup = [
            cls.compute_offering_encrypted,
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
    def test_01_revert_encrypted_root_snapshot(self):
        """Snapshot an encrypted root volume, change it, revert, and verify the data and boot."""
        vm = self._deploy_encrypted_vm("TestEncVM-revert")

        # 1. write a marker into the encrypted root volume
        self._write_marker(vm, "linstor-encrypted-v1")

        # 2. snapshot the (stopped) root volume -> encrypted qcow2 on secondary
        vm.stop(self.apiClient)
        snapshot = self._snapshot_root_volume(vm)

        # 3. change the data so a successful revert is detectable
        self._start_vm(vm)
        self._write_marker(vm, "linstor-encrypted-v2-CHANGED")

        # 4. revert the volume to the snapshot (requires the VM stopped)
        vm.stop(self.apiClient)
        Volume.revertToSnapshot(self.apiClient, snapshot.id)

        # 5. the VM must boot again and the original data must be back
        self._start_vm(vm)
        restored = self._read_marker(vm)
        self.assertEqual(
            "linstor-encrypted-v1", restored,
            "Reverted encrypted root volume has wrong content (got %r) - decryption/round-trip broken" % restored
        )

    @attr(tags=['basic'], required_hardware=True)
    def test_02_create_volume_from_encrypted_snapshot_is_rejected(self):
        """Creating a new volume from an encrypted volume's snapshot must be rejected by CloudStack.

        CloudStack core (VolumeApiServiceImpl) unconditionally blocks this for any encrypted source
        volume ("Cannot create new volumes from encrypted volume snapshots"), so the request must never
        reach the storage layer. This is a guard test: if the limitation is ever lifted, decryption
        support for the create-from-snapshot path (KVMStorageProcessor / LinstorStorageAdaptor) must be
        added and this test updated accordingly.
        """
        vm = self._deploy_encrypted_vm("TestEncVM-create")

        self._write_marker(vm, "linstor-encrypted-create-src")
        vm.stop(self.apiClient)
        snapshot = self._snapshot_root_volume(vm)

        cmd = createVolume.createVolumeCmd()
        cmd.name = "%s-%d" % (self.testdata[TestData.diskName], random.randint(0, 100000))
        cmd.zoneid = self.zone.id
        cmd.account = self.account.name
        cmd.domainid = self.domain.id
        cmd.snapshotid = snapshot.id

        try:
            self.apiClient.createVolume(cmd)
            self.fail("Creating a volume from an encrypted volume snapshot should have been rejected")
        except CloudstackAPIException as e:
            self.assertIn(
                "encrypted volume snapshots", str(e),
                "Unexpected error creating volume from encrypted snapshot: %s" % e
            )

    @attr(tags=['basic'], required_hardware=True)
    def test_03_backed_up_snapshot_qcow2_is_encrypted(self):
        """The qcow2 written to secondary storage for an encrypted volume must itself be LUKS encrypted."""
        if not self.hostConfig:
            self.skipTest("No host SSH credentials available (set HOST_SSH_USER/HOST_SSH_PASSWORD or "
                          "provide them in the marvin config) - cannot inspect the secondary-storage qcow2")
        vm = self._deploy_encrypted_vm("TestEncVM-atrest")
        self._write_marker(vm, "linstor-encrypted-atrest")
        vm.stop(self.apiClient)
        snapshot = self._snapshot_root_volume(vm)

        info = self._qemu_img_info_of_backed_up_snapshot(snapshot)
        if info is None:
            self.skipTest("Could not locate the backed-up snapshot on secondary storage to inspect it")

        encrypted = bool(info.get("encrypted")) or "encrypt" in json.dumps(info.get("format-specific", {}))
        self.assertTrue(
            encrypted,
            "Backed-up snapshot qcow2 is NOT encrypted at rest: %s" % json.dumps(info)
        )

    # --------------------------------------------------------------------- #
    # Helpers
    # --------------------------------------------------------------------- #

    def _deploy_encrypted_vm(self, name):
        vm = VirtualMachine.create(
            self.apiClient,
            {"name": name, "displayname": name},
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering_encrypted.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=False,
            mode='basic',
        )
        self.cleanup.insert(0, vm)
        self._start_vm(vm)
        return vm

    def _snapshot_root_volume(self, vm):
        root = list_volumes(self.apiClient, virtualmachineid=vm.id, type="ROOT", listall=True)[0]
        snapshot = Snapshot.create(
            self.apiClient,
            volume_id=root.id,
            account=self.account.name,
            domainid=self.domain.id,
        )
        self.assertIsNotNone(snapshot, "Could not create snapshot of encrypted root volume")
        self.cleanup.insert(0, snapshot)
        return snapshot

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
        hosts = Host.list(cls.apiClient, zoneid=cls.zone.id, type='Routing', hypervisor='KVM', state='Up')
        return any(getattr(h, "encryptionsupported", False) for h in (hosts or []))

    @classmethod
    def _get_vm(cls, vm_id):
        return list_virtual_machines(cls.apiClient, id=vm_id)[0]

    @classmethod
    def _start_vm(cls, vm):
        vm_for_check = cls._get_vm(vm.id)
        if vm_for_check.state == VirtualMachine.STOPPED:
            vm.start(cls.apiClient)
            vm_for_check = cls._get_vm(vm.id)
        ServiceReady.wait_ssh_ready(vm_for_check.ipaddress)
        return vm_for_check

    def _host_ssh(self):
        host = list_hosts(self.apiClient, type='Routing', hypervisor='KVM', state='Up')[0]
        return SshClient(
            host=host.ipaddress, port=22,
            user=self.hostConfig['username'], passwd=self.hostConfig['password'])

    def _qemu_img_info_of_backed_up_snapshot(self, snapshot):
        """Self-mount the secondary NFS export on a host and run 'qemu-img info' on the snapshot file."""
        # The backed-up snapshot's physical path on secondary storage isn't exposed via the API, so we
        # read it from the DB. The DB may be unreachable from where the tests run (e.g. MariaDB bound to
        # localhost on the management server); in that case return None so the test skips.
        try:
            rows = self.dbConnection.execute(
                "SELECT ss.install_path "
                "FROM snapshot_store_ref ss JOIN snapshots s ON s.id = ss.snapshot_id "
                "WHERE s.uuid = '%s' AND ss.store_role = 'Image'" % snapshot.id)
            store = self.dbConnection.execute(
                "SELECT url FROM image_store WHERE role = 'Image' AND removed IS NULL LIMIT 1")
        except Exception as e:
            logging.debug("DB lookup for snapshot install path failed: %s" % e)
            return None

        if not rows or not rows[0][0] or not store or not store[0][0]:
            return None
        install_path = rows[0][0]
        url = store[0][0]  # e.g. nfs://<server>/<export-path>
        if not url.startswith("nfs://"):
            return None
        server, export = url[len("nfs://"):].split("/", 1)

        ssh = self._host_ssh()
        mount_point = "/tmp/cs_sectest_%d" % random.randint(0, 100000)
        try:
            ssh.execute("mkdir -p %s" % mount_point)
            ssh.execute("mount -t nfs -o ro %s:/%s %s" % (server, export, mount_point))
            out = ssh.execute("qemu-img info --output=json %s/%s" % (mount_point, install_path))
            return json.loads("".join(out)) if out else None
        except Exception as e:
            logging.debug("qemu-img info on secondary failed: %s" % e)
            return None
        finally:
            ssh.execute("umount %s 2>/dev/null; rmdir %s 2>/dev/null" % (mount_point, mount_point))
