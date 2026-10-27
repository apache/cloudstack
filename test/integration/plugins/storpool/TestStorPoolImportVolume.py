# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information regarding copyright
# ownership.  The ASF licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.
""" Smoke test for the StorPool-specific listVolumesForImport/importVolume/unmanageVolume flow.

Exercises the StorPool primary storage driver fast path (StorPoolPrimaryDataStoreDriver
.listVolumesForImport, used instead of dispatching a command to a KVM host) and verifies
that importVolume tags the underlying StorPool volume with "cs"/"uuid" (and the disk
offering's StorPool QoS tier, if set), while unmanageVolume clears those tags again so the
volume is reported again by listVolumesForImport.

Also covers importing a volume/VM whose disk offering carries the StorPool QoS class detail
(SP_QOSCLASS), which must end up as the "qc" tag on the StorPool volume.

The importVm tests use a volume created from a snapshot of a real VM's ROOT disk (rather than
a blank data volume), so the imported VM actually has an OS on it, and verify over SSH that it
boots - not just that the import API calls succeeded.
"""
import math
import uuid

from marvin.cloudstackAPI import unmanageVolume, listVolumesForImport, importVolume, importVm
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.codes import FAILED
from marvin.lib.base import (Account,
                             DiskOffering,
                             ServiceOffering,
                             Snapshot,
                             Tag,
                             VirtualMachine,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template,
                               list_clusters,
                               list_networks,
                               list_storage_pools)
from nose.plugins.attrib import attr
from storpool import spapi, sptypes

from sp_util import TestData, StorPoolHelper

get_suitable_test_template.__test__ = False

_multiprocess_shared_ = True


class TestStorPoolImportVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestStorPoolImportVolume, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() != "kvm":
            raise unittest.SkipTest("StorPool import/unmanage volume is only available for KVM")

        config = cls.getClsConfig()
        zone = config.zones[0]
        StorPoolHelper.logger = cls
        cls.helper = StorPoolHelper()
        sp_pools = cls.helper.get_pool(zone)
        if sp_pools is None:
            raise unittest.SkipTest("This test needs at least 2 StorPool primary storages configured in the zone")
        cls.spapi = spapi.Api(host=zone.spEndpoint, port=zone.spEndpointPort, auth=zone.spAuthToken, multiCluster=True)

        sp_testdata = TestData().testdata
        cls.pool_tag = sp_testdata[TestData.sp_template_1]["tags"]

        storage_pools = list_storage_pools(cls.apiclient, name=sp_pools[0]["name"])
        if not storage_pools:
            raise unittest.SkipTest("StorPool primary storage %s is not registered in CloudStack yet" % sp_pools[0]["name"])
        cls.primary_storage = storage_pools[0]

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls._cleanup = []

        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            sp_testdata[TestData.diskOfferingSmall]
        )
        cls._cleanup.append(cls.disk_offering)

        cls.qos_tag = cls.pool_tag
        cls.disk_offering_qos = DiskOffering.create(
            cls.apiclient,
            sp_testdata[TestData.diskOfferingSmall],
            details=[{"SP_QOSCLASS": cls.qos_tag}]
        )
        cls._cleanup.append(cls.disk_offering_qos)

        service_offering_data = dict(cls.testdata["service_offerings"]["tiny"])
        service_offering_data["name"] = "StorPoolImportTest-%s" % uuid.uuid4()
        service_offering_data["tags"] = cls.pool_tag
        cls.service_offering = ServiceOffering.create(cls.apiclient, service_offering_data)
        cls._cleanup.append(cls.service_offering)

        service_offering_qos_data = dict(cls.testdata["service_offerings"]["tiny"])
        service_offering_qos_data["name"] = "StorPoolImportQosTest-%s" % uuid.uuid4()
        service_offering_qos_data["tags"] = cls.pool_tag
        cls.service_offering_qos = ServiceOffering.create(
            cls.apiclient,
            service_offering_qos_data,
            diskofferingid=cls.disk_offering_qos.id
        )
        cls._cleanup.append(cls.service_offering_qos)

        template = get_suitable_test_template(
            cls.apiclient,
            cls.primary_storage.zoneid,
            cls.testdata["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return a template"

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.testdata["virtual_machine"],
            templateid=template.id,
            zoneid=cls.primary_storage.zoneid,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.zone.networktype
        )
        cls._cleanup.append(cls.virtual_machine)
        cls.virtual_machine.stop(cls.apiclient, forced=True)

        root_volume = Volume.list(cls.apiclient, virtualmachineid=cls.virtual_machine.id,
                                   type="ROOT", listall=True)[0]
        cls.root_snapshot = Snapshot.create(
            cls.apiclient,
            volume_id=root_volume.id,
            account=cls.account.name,
            domainid=cls.account.domainid
        )
        cls._cleanup.append(cls.root_snapshot)

        disk_offering_boot_data = dict(sp_testdata[TestData.diskOfferingSmall])
        disk_offering_boot_data["name"] = "StorPoolImportBootTest"
        disk_offering_boot_data["disksize"] = int(math.ceil(cls.root_snapshot.virtualsize / (1024.0 ** 3)))
        cls.disk_offering_boot = DiskOffering.create(cls.apiclient, disk_offering_boot_data)
        cls._cleanup.append(cls.disk_offering_boot)

    @classmethod
    def tearDownClass(cls):
        super(TestStorPoolImportVolume, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def storpool_volume(self, path):
        name = path.split("/")[3]
        volumes = self.spapi.volumeList(volumeName="~" + name)
        self.assertTrue(len(volumes) > 0, "Could not find StorPool volume for path %s" % path)
        return volumes[0]

    def create_storpool_volume(self, diskname, disk_offering_id):
        return Volume.create(
            self.apiclient,
            {"diskname": diskname},
            zoneid=self.primary_storage.zoneid,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=disk_offering_id,
            storageid=self.primary_storage.id
        )

    def create_bootable_storpool_volume(self, diskname):
        """ Clones cls.root_snapshot (a snapshot of a real VM's ROOT disk) into a new volume, so
        it has an actual OS on it - unlike create_storpool_volume()'s blank data volume, this is
        usable as a VM's ROOT disk after import. storageid can't be combined with snapshotid
        (CreateVolumeCmd.getStorageId() rejects that combination), so disk_offering_boot's StorPool
        tag is what pins the resulting volume onto cls.primary_storage instead. """
        return Volume.create(
            self.apiclient,
            {"diskname": diskname},
            zoneid=self.primary_storage.zoneid,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_boot.id,
            snapshotid=self.root_snapshot.id
        )

    def assert_vm_boots_over_ssh(self, vm):
        """ Starts the (powered-off, just-imported) VM and runs a command over SSH, to prove it
        actually boots the cloned OS disk - not just that the import API calls succeeded. """
        vm.start(self.apiclient)
        ssh_client = vm.get_ssh_client(ipaddress=vm.ipaddress, retries=30)
        result = ssh_client.execute("echo storpool-import-boot-check")
        self.assertEqual(["storpool-import-boot-check"], result,
                          "The imported VM should boot and be reachable over SSH")

    def create_sp_volume_directly(self, tags):
        """ Creates a volume on StorPool without going through CloudStack at all - simulates a
        volume that pre-exists on the pool (e.g. provisioned by external tooling), the realistic
        "import pre-existing storage" scenario, as opposed to create_storpool_volume()'s
        CloudStack-created-then-unmanaged volume. Returns (sp_volume_name, path); the caller must
        delete the volume directly via self.spapi.volumeDelete() if it is never imported, since
        self.cleanup only drains CloudStack-tracked marvin.lib.base.Volume instances. """
        volume_desc = sptypes.VolumeCreateDesc(
            size=1 * 1024 * 1024 * 1024,
            template=self.pool_tag,
            tags=tags
        )
        created = self.spapi.volumeCreate(volume_desc)
        sp_volume_name = created.name
        path = "/dev/storpool-byid/%s" % sp_volume_name.lstrip("~")
        return sp_volume_name, path

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_01_import_volume_tags_storpool_volume_with_cs_and_uuid(self):
        """ importVolume should tag the StorPool volume with cs/uuid (and unmanageVolume
        should clear those tags again), so a volume can round-trip between CloudStack-managed
        and available-for-import. """

        volume = self.create_storpool_volume("StorPoolImportTest", self.disk_offering.id)

        sp_volume = self.storpool_volume(volume.path)
        self.assertEqual("volume", sp_volume.tags.get("cs"),
                          "A CloudStack-managed volume should carry the cs=volume StorPool tag")

        cmd = unmanageVolume.unmanageVolumeCmd()
        cmd.id = volume.id
        self.apiclient.unmanageVolume(cmd)

        sp_volume = self.storpool_volume(volume.path)
        self.assertFalse(sp_volume.tags.get("cs"),
                          "unmanageVolume should clear the StorPool cs tag")

        cmd = listVolumesForImport.listVolumesForImportCmd()
        cmd.storageid = self.primary_storage.id
        volumes_for_import = self.apiclient.listVolumesForImport(cmd)
        self.assertTrue(isinstance(volumes_for_import, list),
                         "listVolumesForImport should return a valid list")
        matching = [v for v in volumes_for_import if v.path == volume.path]
        self.assertEqual(1, len(matching),
                          "The unmanaged volume should be listed as available for import")
        self.assertEqual("QCOW2", matching[0].format,
                          "StorPool volumes are always reported as QCOW2")

        cmd = importVolume.importVolumeCmd()
        cmd.storageid = self.primary_storage.id
        cmd.path = volume.path
        cmd.diskofferingid = self.disk_offering.id
        imported = self.apiclient.importVolume(cmd)
        self.assertIsNotNone(imported, "importVolume should return the imported volume")
        self.cleanup.append(Volume(imported.__dict__))

        sp_volume = self.storpool_volume(volume.path)
        self.assertEqual("volume", sp_volume.tags.get("cs"),
                          "importVolume should re-tag the StorPool volume as cs=volume")
        self.assertEqual(imported.id, sp_volume.tags.get("uuid"),
                          "importVolume should tag the StorPool volume with the new CloudStack volume uuid")

        cmd = listVolumesForImport.listVolumesForImportCmd()
        cmd.storageid = self.primary_storage.id
        volumes_for_import = self.apiclient.listVolumesForImport(cmd)
        matching = [v for v in (volumes_for_import or []) if v.path == volume.path]
        self.assertEqual(0, len(matching),
                          "A re-imported volume should no longer be listed as available for import")

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_02_list_volumes_for_import_by_path(self):
        """ listVolumesForImport with a specific path should look up that one StorPool volume
        directly (StorPoolPrimaryDataStoreDriver's single-volume Describe lookup), instead of
        browsing/filtering the whole pool. """

        volume = self.create_storpool_volume("StorPoolImportPathTest", self.disk_offering.id)

        cmd = unmanageVolume.unmanageVolumeCmd()
        cmd.id = volume.id
        self.apiclient.unmanageVolume(cmd)

        cmd = listVolumesForImport.listVolumesForImportCmd()
        cmd.storageid = self.primary_storage.id
        cmd.path = volume.path
        volumes_for_import = self.apiclient.listVolumesForImport(cmd)
        self.assertTrue(isinstance(volumes_for_import, list) and len(volumes_for_import) == 1,
                         "listVolumesForImport with a path should return exactly that one volume")
        self.assertEqual(volume.path, volumes_for_import[0].path,
                          "listVolumesForImport with a path should return the matching volume")
        self.assertEqual("QCOW2", volumes_for_import[0].format,
                          "StorPool volumes are always reported as QCOW2")

        cmd = importVolume.importVolumeCmd()
        cmd.storageid = self.primary_storage.id
        cmd.path = volume.path
        cmd.diskofferingid = self.disk_offering.id
        imported = self.apiclient.importVolume(cmd)
        self.assertIsNotNone(imported, "importVolume should return the imported volume")
        self.cleanup.append(Volume(imported.__dict__))

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_03_import_vm_from_shared_storage_on_storpool(self):
        """ importVm(importSource=SHARED) against a disk already sitting on a StorPool pool
        should record the disk as the VM's ROOT volume, and tag the StorPool volume with
        cs/uuid/cvm, since the disk is now owned by (and attached to) a CloudStack VM. The disk
        is a clone of a real VM's ROOT disk, so the imported VM is actually bootable, and is
        verified to boot over SSH. """

        volume = self.create_bootable_storpool_volume("StorPoolImportVmTest")

        cmd = unmanageVolume.unmanageVolumeCmd()
        cmd.id = volume.id
        self.apiclient.unmanageVolume(cmd)

        networks = list_networks(self.apiclient, account=self.account.name, domainid=self.account.domainid)
        self.assertTrue(isinstance(networks, list) and len(networks) > 0, "account should have a network")
        clusters = list_clusters(self.apiclient, zoneid=self.primary_storage.zoneid)
        self.assertTrue(isinstance(clusters, list) and len(clusters) > 0, "zone should have a cluster")

        cmd = importVm.importVmCmd()
        cmd.zoneid = self.primary_storage.zoneid
        cmd.hypervisor = "KVM"
        cmd.importsource = "shared"
        cmd.clusterid = clusters[0].id
        cmd.name = "StorPoolImportVm-%s" % uuid.uuid4()
        cmd.serviceofferingid = self.service_offering.id
        cmd.diskpath = volume.path
        cmd.networkid = networks[0].id
        cmd.storageid = self.primary_storage.id
        cmd.account = self.account.name
        cmd.domainid = self.account.domainid
        imported = self.apiclient.importVm(cmd)
        self.assertIsNotNone(imported, "importVm should return the imported VM")
        imported_vm = VirtualMachine(imported.__dict__, self.testdata)

        try:
            root_volumes = Volume.list(self.apiclient, virtualmachineid=imported.id, type="ROOT", listall=True)
            self.assertTrue(isinstance(root_volumes, list) and len(root_volumes) > 0,
                             "the imported VM should have a ROOT volume")
            root_volume = root_volumes[0]
            self.assertEqual(volume.path, root_volume.path,
                              "the imported ROOT volume should be the disk that was unmanaged")

            sp_volume = self.storpool_volume(root_volume.path)
            self.assertEqual("volume", sp_volume.tags.get("cs"),
                              "importVm should tag the StorPool volume as cs=volume")
            self.assertEqual(root_volume.id, sp_volume.tags.get("uuid"),
                              "importVm should tag the StorPool volume with the new CloudStack volume uuid")
            self.assertEqual(imported.id, sp_volume.tags.get("cvm"),
                              "importVm should tag the StorPool volume with the owning VM's uuid")

            self.assert_vm_boots_over_ssh(imported_vm)
        finally:
            imported_vm.delete(self.apiclient, expunge=True)

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_04_import_volume_tags_storpool_volume_with_qos_class(self):
        """ importVolume should tag the StorPool volume with "qc" set to the QOS_CLASS
        (SP_QOSCLASS) detail configured on the disk offering used for the import. """

        volume = self.create_storpool_volume("StorPoolImportQosTest", self.disk_offering_qos.id)

        cmd = unmanageVolume.unmanageVolumeCmd()
        cmd.id = volume.id
        self.apiclient.unmanageVolume(cmd)

        cmd = importVolume.importVolumeCmd()
        cmd.storageid = self.primary_storage.id
        cmd.path = volume.path
        cmd.diskofferingid = self.disk_offering_qos.id
        imported = self.apiclient.importVolume(cmd)
        self.assertIsNotNone(imported, "importVolume should return the imported volume")
        self.cleanup.append(Volume(imported.__dict__))

        sp_volume = self.storpool_volume(volume.path)
        self.assertEqual(self.qos_tag, sp_volume.tags.get("qc"),
                          "importVolume should tag the StorPool volume with the disk offering's "
                          "QOS_CLASS (SP_QOSCLASS) detail")

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_05_import_vm_from_shared_storage_on_storpool_with_qos_class(self):
        """ importVm(importSource=SHARED) using a service offering linked to a disk offering
        that has a QOS_CLASS (SP_QOSCLASS) detail should tag the imported ROOT volume's
        StorPool volume with cs/uuid/cvm, as well as "qc" set to that QOS_CLASS value. The disk
        is a clone of a real VM's ROOT disk, so the imported VM is actually bootable, and is
        verified to boot over SSH. """

        volume = self.create_bootable_storpool_volume("StorPoolImportVmQosTest")

        cmd = unmanageVolume.unmanageVolumeCmd()
        cmd.id = volume.id
        self.apiclient.unmanageVolume(cmd)

        networks = list_networks(self.apiclient, account=self.account.name, domainid=self.account.domainid)
        self.assertTrue(isinstance(networks, list) and len(networks) > 0, "account should have a network")
        clusters = list_clusters(self.apiclient, zoneid=self.primary_storage.zoneid)
        self.assertTrue(isinstance(clusters, list) and len(clusters) > 0, "zone should have a cluster")

        cmd = importVm.importVmCmd()
        cmd.zoneid = self.primary_storage.zoneid
        cmd.hypervisor = "KVM"
        cmd.importsource = "shared"
        cmd.clusterid = clusters[0].id
        cmd.name = "StorPoolImportVmQos-%s" % uuid.uuid4()
        cmd.serviceofferingid = self.service_offering_qos.id
        cmd.diskpath = volume.path
        cmd.networkid = networks[0].id
        cmd.storageid = self.primary_storage.id
        cmd.account = self.account.name
        cmd.domainid = self.account.domainid
        imported = self.apiclient.importVm(cmd)
        self.assertIsNotNone(imported, "importVm should return the imported VM")
        imported_vm = VirtualMachine(imported.__dict__, self.testdata)

        try:
            root_volumes = Volume.list(self.apiclient, virtualmachineid=imported.id, type="ROOT", listall=True)
            self.assertTrue(isinstance(root_volumes, list) and len(root_volumes) > 0,
                             "the imported VM should have a ROOT volume")
            root_volume = root_volumes[0]
            self.assertEqual(volume.path, root_volume.path,
                              "the imported ROOT volume should be the disk that was unmanaged")

            sp_volume = self.storpool_volume(root_volume.path)
            self.assertEqual("volume", sp_volume.tags.get("cs"),
                              "importVm should tag the StorPool volume as cs=volume")
            self.assertEqual(root_volume.id, sp_volume.tags.get("uuid"),
                              "importVm should tag the StorPool volume with the new CloudStack volume uuid")
            self.assertEqual(imported.id, sp_volume.tags.get("cvm"),
                              "importVm should tag the StorPool volume with the owning VM's uuid")
            self.assertEqual(self.qos_tag, sp_volume.tags.get("qc"),
                              "importVm should tag the StorPool volume with the linked disk offering's "
                              "QOS_CLASS (SP_QOSCLASS) detail")

            self.assert_vm_boots_over_ssh(imported_vm)
        finally:
            imported_vm.delete(self.apiclient, expunge=True)

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_06_list_volumes_for_import_by_path_excludes_managed_volume(self):
        """ listVolumesForImport with a path should never report an already CloudStack-managed
        (StorPool "cs"-tagged) volume as available for import - StorPoolPrimaryDataStoreDriver's
        single-volume Describe lookup must apply the same "already managed" exclusion the
        browse-all case does, unlike the internal lookup unmanageVolume() uses to find that same
        volume (see test_01, which unmanages successfully, exercising the same volume while it is
        still managed, the way this test's path-lookup check does). """

        volume = self.create_storpool_volume("StorPoolListImportExcludesManagedTest", self.disk_offering.id)
        self.cleanup.append(volume)

        sp_volume = self.storpool_volume(volume.path)
        self.assertEqual("volume", sp_volume.tags.get("cs"),
                          "A CloudStack-managed volume should carry the cs=volume StorPool tag")

        cmd = listVolumesForImport.listVolumesForImportCmd()
        cmd.storageid = self.primary_storage.id
        cmd.path = volume.path
        volumes_for_import = self.apiclient.listVolumesForImport(cmd)
        matching = [v for v in (volumes_for_import or []) if v.path == volume.path]
        self.assertEqual(0, len(matching),
                          "A still-managed volume should not be listed as available for import, "
                          "even when looked up by its exact path")

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_07_import_volume_created_directly_on_storpool(self):
        """ A volume StorPool provisioned directly - one CloudStack never created and so never
        tagged cs/uuid on - should show up via listVolumesForImport and be importable, exactly
        like a CloudStack-created-then-unmanaged volume. Every other test here only ever exercises
        the latter (create via CloudStack, unmanage, re-import), never a genuinely external volume,
        which is the realistic "import pre-existing storage" scenario this feature targets. """

        sp_volume_name, path = self.create_sp_volume_directly({"createdby": "storpool-direct-test"})

        imported = None
        try:
            sp_volume = self.storpool_volume(path)
            self.assertIsNone(sp_volume.tags.get("cs"),
                               "A volume StorPool created directly should not carry CloudStack's cs tag")

            cmd = listVolumesForImport.listVolumesForImportCmd()
            cmd.storageid = self.primary_storage.id
            volumes_for_import = self.apiclient.listVolumesForImport(cmd)
            matching = [v for v in (volumes_for_import or []) if v.path == path]
            self.assertEqual(1, len(matching),
                              "A volume created directly on StorPool should be listed as "
                              "available for import")
            self.assertEqual("QCOW2", matching[0].format,
                              "StorPool volumes are always reported as QCOW2")

            cmd = importVolume.importVolumeCmd()
            cmd.storageid = self.primary_storage.id
            cmd.path = path
            cmd.diskofferingid = self.disk_offering.id
            imported = self.apiclient.importVolume(cmd)
            self.assertIsNotNone(imported, "importVolume should return the imported volume")
            self.cleanup.append(Volume(imported.__dict__))

            sp_volume = self.storpool_volume(path)
            self.assertEqual("volume", sp_volume.tags.get("cs"),
                              "importVolume should tag the StorPool volume as cs=volume")
            self.assertEqual(imported.id, sp_volume.tags.get("uuid"),
                              "importVolume should tag the StorPool volume with the new CloudStack volume uuid")
        finally:
            if imported is None:
                self.spapi.volumeDelete(volumeName=sp_volume_name)

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_08_list_volumes_for_import_excludes_externally_cs_tagged_volume(self):
        """ listVolumesForImport must exclude a volume tagged cs=volume regardless of whether
        CloudStack itself ever created or knew about it - the StorPool "cs" tag alone is the
        source of truth for "already managed", not a CloudStack DB record (contrast test_07,
        where the same kind of directly-created, CloudStack-unknown volume - but untagged - is
        correctly importable). This is checked browsing all volumes, by exact path, and then by
        actually attempting importVolume against the same disk - all three must reject it. """

        sp_volume_name, path = self.create_sp_volume_directly({"cs": "volume"})
        try:
            cmd = listVolumesForImport.listVolumesForImportCmd()
            cmd.storageid = self.primary_storage.id
            volumes_for_import = self.apiclient.listVolumesForImport(cmd)
            matching = [v for v in (volumes_for_import or []) if v.path == path]
            self.assertEqual(0, len(matching),
                              "A volume tagged cs=volume should not be listed as available for "
                              "import, even though CloudStack has no record of it")

            cmd = listVolumesForImport.listVolumesForImportCmd()
            cmd.storageid = self.primary_storage.id
            cmd.path = path
            volumes_for_import = self.apiclient.listVolumesForImport(cmd)
            matching = [v for v in (volumes_for_import or []) if v.path == path]
            self.assertEqual(0, len(matching),
                              "A volume tagged cs=volume should also be excluded when looked up "
                              "by its exact path")

            cmd = importVolume.importVolumeCmd()
            cmd.storageid = self.primary_storage.id
            cmd.path = path
            cmd.diskofferingid = self.disk_offering.id
            try:
                self.apiclient.importVolume(cmd)
                self.fail("importVolume should fail for a volume already tagged cs=volume")
            except Exception as e:
                self.debug("importVolume correctly failed for a cs-tagged volume: %s" % e)
        finally:
            self.spapi.volumeDelete(volumeName=sp_volume_name)

    @attr(tags=["advanced", "basic", "sg"], required_hardware=False)
    def test_09_unmanage_volume_clears_all_storpool_tags(self):
        """ unmanageVolume should clear every StorPool tag CloudStack set - cs, uuid, qc, cvm
        and vc-policy - checked in isolation from a subsequent re-import (contrast test_01,
        which only asserts on cs/uuid, and only as part of an unmanage-then-reimport round
        trip). The volume is attached to cls.virtual_machine (so it picks up the cvm tag) and
        the VM is given a vc-policy resource tag (so the volume picks that up too) before being
        detached and unmanaged. CloudStack rejects unmanageVolume on a still-attached volume, so
        the volume has to be detached first anyway - the tags are checked as cleared here too in
        case that invariant ever changes. """

        volume = self.create_storpool_volume("StorPoolUnmanageClearsTagsTest", self.disk_offering_qos.id)

        self.virtual_machine.attach_volume(self.apiclient, volume)
        Tag.create(
            self.apiclient,
            resourceIds=self.virtual_machine.id,
            resourceType='UserVm',
            tags={'vc-policy': 'test'}
        )

        sp_volume = self.storpool_volume(volume.path)
        self.assertEqual("volume", sp_volume.tags.get("cs"),
                          "A CloudStack-managed volume should carry the cs=volume StorPool tag")
        self.assertEqual(volume.id, sp_volume.tags.get("uuid"),
                          "A CloudStack-managed volume should carry its CloudStack uuid as a StorPool tag")
        self.assertEqual(self.qos_tag, sp_volume.tags.get("qc"),
                          "A CloudStack-managed volume on a QoS disk offering should carry the qc tag")
        self.assertEqual(self.virtual_machine.id, sp_volume.tags.get("cvm"),
                          "A volume attached to a VM should carry the owning VM's uuid as the cvm tag")
        self.assertEqual("test", sp_volume.tags.get("vc-policy"),
                          "A vc-policy tag set on the owning VM should be pushed to the StorPool volume")

        try:
            self.virtual_machine.detach_volume(self.apiclient, volume)

            cmd = unmanageVolume.unmanageVolumeCmd()
            cmd.id = volume.id
            self.apiclient.unmanageVolume(cmd)

            sp_volume = self.storpool_volume(volume.path)
            self.assertFalse(sp_volume.tags.get("cs"), "unmanageVolume should clear the cs tag")
            self.assertFalse(sp_volume.tags.get("uuid"), "unmanageVolume should clear the uuid tag")
            self.assertFalse(sp_volume.tags.get("qc"), "unmanageVolume should clear the qc tag")
            self.assertFalse(sp_volume.tags.get("cvm"), "unmanageVolume should clear the cvm tag")
            self.assertFalse(sp_volume.tags.get("vc-policy"), "unmanageVolume should clear the vc-policy tag")
        finally:
            # The volume is unmanaged, so it is no longer a CloudStack-tracked resource for
            # self.cleanup to delete - remove it directly on StorPool instead.
            self.spapi.volumeDelete(volumeName="~" + volume.path.split("/")[3])
