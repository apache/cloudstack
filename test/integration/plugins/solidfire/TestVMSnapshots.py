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

import logging
import random
import SignedAPICall
import time
import XenAPI

from solidfire.factory import ElementFactory

from util import sf_util

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, DiskOffering, ServiceOffering, StoragePool, User, VirtualMachine, VmSnapshot, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_hosts, list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

# Prerequisites:
#  Only one zone
#  Only one pod
#  Only one cluster

# Running the tests:
#  Change the "hypervisor_type" variable to control which hypervisor type to test.
#  If using XenServer, verify the "xen_server_hostname" variable is correct.


class TestData:
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    diskOffering = "diskoffering"
    domainId = "domainId"
    hypervisor = "hypervisor"
    login = "login"
    mvip = "mvip"
    password = "password"
    port = "port"
    primaryStorage = "primarystorage"
    provider = "provider"
    scope = "scope"
    solidFire = "solidfire"
    storageTag = "SolidFire_SAN_1"
    tags = "tags"
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    volume_1 = "volume_1"
    xenServer = "xenserver"
    zoneId = "zoneId"

    # modify to control which hypervisor type to test
    hypervisor_type = xenServer
    xen_server_hostname = "XenServer-6.5-1"

    def __init__(self):
        self.testdata = {
            TestData.solidFire: {
                TestData.mvip: "10.117.40.120",
                TestData.username: "admin",
                TestData.password: "admin",
                TestData.port: 443,
                TestData.url: "https://10.117.40.120:443"
            },
            TestData.xenServer: {
                TestData.username: "root",
                TestData.password: "solidfire"
            },
            TestData.account: {
                "email": "test@test.com",
                "firstname": "John",
                "lastname": "Doe",
                TestData.username: "test",
                TestData.password: "test"
            },
            TestData.user: {
                "email": "user@test.com",
                "firstname": "Jane",
                "lastname": "Doe",
                TestData.username: "testuser",
                TestData.password: "password"
            },
            TestData.primaryStorage: {
                "name": "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                "url": "MVIP=10.117.40.120;SVIP=10.117.41.120;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 4500000,
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "Any"
            },
            TestData.virtualMachine: {
                "name": "TestVM",
                "displayname": "Test VM"
            },
            TestData.computeOffering: {
                "name": "SF_CO_1",
                "displaytext": "SF_CO_1 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": "10000",
                "maxiops": "15000",
                "hypervisorsnapshotreserve": 400,
                TestData.tags: TestData.storageTag
            },
            TestData.diskOffering: {
                "name": "SF_DO_1",
                "displaytext": "SF_DO_1 (Min IOPS = 300; Max IOPS = 500)",
                "disksize": 128,
                "customizediops": False,
                "miniops": 300,
                "maxiops": 500,
                "hypervisorsnapshotreserve": 400,
                TestData.tags: TestData.storageTag,
                "storagetype": "shared"
            },
            TestData.volume_1: {
                "diskname": "testvolume",
            },
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }


class TestVMSnapshots(cloudstackTestCase):
    _should_be_no_vm_snapshots_err_msg = "There should be no VM snapshots."
    _should_only_be_one_vm_snapshot_err_msg = "There should only be one VM snapshot."
    _should_only_be_one_root_volume_err_msg = "There should only be one root volume."
    _path_should_have_changed_err_msg = "The 'path' in the 'DB' should have changed."
    _path_should_not_have_changed_err_msg = "The 'path' in the 'DB' should not have changed."
    _should_only_be_one_vdi_err_msg = "There should only be one VDI."
    _should_be_three_vdis_err_msg = "There should be three VDIs."
    _active_vdis_should_not_be_the_same_err_msg = "The active VDIs should not be the same."
    _active_vdis_should_be_the_same_err_msg = "The active VDIs should be the same."
    _snapshot_vdis_should_be_the_same_err_msg = "The snapshot VDIs should be the same."
    _base_vdis_should_be_the_same_err_msg = "The base VDIs should be the same."
    _snapshot_parent_not_correct_err_msg = "Snapshot's parent is not correct."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestVMSnapshots, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()

        cls.testdata = TestData().testdata

        # Set up XenAPI connection
        host_ip = "https://" + \
                  list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name=TestData.xen_server_hostname)[0].ipaddress

        cls.xen_session = XenAPI.Session(host_ip)

        xenserver = cls.testdata[TestData.xenServer]

        cls.xen_session.xenapi.login_with_password(xenserver[TestData.username], xenserver[TestData.password])

        # Set up SolidFire connection
        solidfire = cls.testdata[TestData.solidFire]

        cls.sfe = ElementFactory.create(solidfire[TestData.mvip], solidfire[TestData.username], solidfire[TestData.password])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        template = get_template(cls.apiClient, cls.zone.id, hypervisor=TestData.hypervisor_type)
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])

        # Create test account
        cls.account = Account.create(
            cls.apiClient,
            cls.testdata[TestData.account],
            admin=1
        )

        # Set up connection to make customized API calls
        user = User.create(
            cls.apiClient,
            cls.testdata[TestData.user],
            account=cls.account.name,
            domainid=cls.domain.id
        )

        url = cls.testdata[TestData.url]

        api_url = "http://" + url + ":8080/client/api"
        userkeys = User.registerUserKeys(cls.apiClient, user.id)

        cls.cs_api = SignedAPICall.CloudStack(api_url, userkeys.apikey, userkeys.secretkey)

        primarystorage = cls.testdata[TestData.primaryStorage]

        cls.primary_storage = StoragePool.create(
            cls.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=cls.zone.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        cls.disk_offering = DiskOffering.create(
            cls.apiClient,
            cls.testdata[TestData.diskOffering]
        )

        # Create VM and volume for tests
        cls.virtual_machine = VirtualMachine.create(
            cls.apiClient,
            cls.testdata[TestData.virtualMachine],
            accountid=cls.account.name,
            zoneid=cls.zone.id,
            serviceofferingid=compute_offering.id,
            templateid=template.id,
            domainid=cls.domain.id,
            startvm=True
        )

        cls._cleanup = [
            compute_offering,
            cls.disk_offering,
            user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            time.sleep(60)

            cls.virtual_machine.delete(cls.apiClient, True)

            cleanup_resources(cls.apiClient, cls._cleanup)

            cls.primary_storage.delete(cls.apiClient)

            sf_util.purge_solidfire_volumes(cls.sfe)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiClient, self.cleanup)
        except Exception as e:
            logging.debug("Exception in tearDown(self): %s" % e)

    def test_01_take_VM_snapshot(self):
        self.virtual_machine.start(self.apiClient)

        root_volumes = list_volumes(self.apiClient, type="ROOT", listAll="true")

        sf_util.check_list(root_volumes, 1, self, TestVMSnapshots._should_only_be_one_root_volume_err_msg)

        root_volume = root_volumes[0]

        volume_id = {'volumeid': root_volume.id}

        sf_iscsi_name_result = self.cs_api.getVolumeiScsiName(volume_id)
        sf_iscsi_name = sf_iscsi_name_result['apivolumeiscsiname']['volumeiScsiName']

        sf_util.check_iscsi_name(sf_iscsi_name, self)

        root_volume_path_1 = self._get_path(volume_id)

        #######################################
        #######################################
        # STEP 1: Take snapshot of running VM #
        #######################################
        #######################################
        vm_snapshot = VmSnapshot.create(
            self.apiClient,
            vmid=self.virtual_machine.id,
            snapshotmemory="false",
            name="Test Snapshot",
            description="Test Snapshot Desc"
        )

        list_vm_snapshots = VmSnapshot.list(self.apiClient, listAll="true")

        self._verify_vm_snapshot(list_vm_snapshots, vm_snapshot)

        root_volume_path_2 = self._get_path(volume_id)

        self.assertEqual(
            root_volume_path_1,
            root_volume_path_2,
            TestVMSnapshots._path_should_not_have_changed_err_msg
        )

        xen_sr = self.xen_session.xenapi.SR.get_by_name_label(sf_iscsi_name)[0]

        xen_vdis = self.xen_session.xenapi.SR.get_VDIs(xen_sr)

        sf_util.check_list(xen_vdis, 3, self, TestVMSnapshots._should_be_three_vdis_err_msg)

        vdis_after_create = self._get_vdis(xen_vdis)

        vdiSnapshotOf = self.xen_session.xenapi.VDI.get_record(vdis_after_create.snapshot_vdi["snapshot_of"])

        self.assertEqual(
            vdiSnapshotOf["uuid"],
            vdis_after_create.active_vdi["uuid"],
            TestVMSnapshots._snapshot_parent_not_correct_err_msg
        )

        #######################################
        #######################################
        ###  STEP 2: Revert VM to Snapshot  ###
        #######################################
        #######################################
        self.virtual_machine.stop(self.apiClient)

        VmSnapshot.revertToSnapshot(self.apiClient, vmsnapshotid=vm_snapshot.id)

        list_vm_snapshots = VmSnapshot.list(self.apiClient, listAll="true")

        sf_util.check_list(list_vm_snapshots, 1, self, TestVMSnapshots._should_only_be_one_vm_snapshot_err_msg)

        root_volume_path_3 = self._get_path(volume_id)

        self.assertNotEqual(
            root_volume_path_1,
            root_volume_path_3,
            TestVMSnapshots._path_should_have_changed_err_msg
        )

        xen_vdis = self.xen_session.xenapi.SR.get_VDIs(xen_sr)

        sf_util.check_list(xen_vdis, 3, self, TestVMSnapshots._should_be_three_vdis_err_msg)

        vdis_after_revert = self._get_vdis(xen_vdis)

        self.assertNotEqual(
            vdis_after_create.active_vdi["uuid"],
            vdis_after_revert.active_vdi["uuid"],
            TestVMSnapshots._active_vdis_should_not_be_the_same_err_msg
        )

        self.assertEqual(
            vdis_after_create.snapshot_vdi["uuid"],
            vdis_after_revert.snapshot_vdi["uuid"],
            TestVMSnapshots._snapshot_vdis_should_be_the_same_err_msg
        )

        self.assertEqual(
            vdis_after_create.base_vdi["uuid"],
            vdis_after_revert.base_vdi["uuid"],
            TestVMSnapshots._base_vdis_should_be_the_same_err_msg
        )

        #######################################
        #######################################
        ##### STEP 3: Delete VM snapshot  #####
        #######################################
        #######################################
        VmSnapshot.deleteVMSnapshot(self.apiClient, vmsnapshotid=vm_snapshot.id)

        list_vm_snapshots = VmSnapshot.list(self.apiClient, listAll="true")

        self.assertEqual(
            list_vm_snapshots,
            None,
            TestVMSnapshots._should_be_no_vm_snapshots_err_msg
        )

        root_volume_path_4 = self._get_path(volume_id)

        self.assertEqual(
            root_volume_path_3,
            root_volume_path_4,
            TestVMSnapshots._path_should_not_have_changed_err_msg
        )

        xen_vdis = self.xen_session.xenapi.SR.get_VDIs(xen_sr)

        sf_util.check_list(xen_vdis, 1, self, TestVMSnapshots._should_only_be_one_vdi_err_msg)

        vdis_after_delete = self._get_vdis(xen_vdis, True)

        self.assertEqual(
            vdis_after_revert.active_vdi["uuid"],
            vdis_after_delete.active_vdi["uuid"],
            TestVMSnapshots._active_vdis_should_be_the_same_err_msg
        )

        #######################################
        #######################################
        #####      STEP 4: Start VM       #####
        #######################################
        #######################################
        self.virtual_machine.start(self.apiClient)

    def test_02_take_VM_snapshot_with_data_disk(self):
        self.virtual_machine.start(self.apiClient)

        data_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_1],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        self.cleanup = [data_volume]

        self.virtual_machine.attach_volume(self.apiClient, data_volume)

        root_volumes = list_volumes(self.apiClient, type="ROOT", listAll="true")

        sf_util.check_list(root_volumes, 1, self, TestVMSnapshots._should_only_be_one_root_volume_err_msg)

        root_volume = root_volumes[0]

        root_volume_id = {'volumeid': root_volume.id}

        sf_iscsi_name_result = self.cs_api.getVolumeiScsiName(root_volume_id)
        sf_iscsi_root_volume_name = sf_iscsi_name_result['apivolumeiscsiname']['volumeiScsiName']

        sf_util.check_iscsi_name(sf_iscsi_root_volume_name, self)

        root_volume_path_1 = self._get_path(root_volume_id)

        data_volumes = list_volumes(self.apiClient, type="DATADISK", listAll="true")

        sf_util.check_list(data_volumes, 1, self, "There should only be one data volume.")

        data_volume = data_volumes[0]

        data_volume_id = {'volumeid': data_volume.id}

        sf_iscsi_name_result = self.cs_api.getVolumeiScsiName(data_volume_id)
        sf_iscsi_data_volume_name = sf_iscsi_name_result['apivolumeiscsiname']['volumeiScsiName']

        sf_util.check_iscsi_name(sf_iscsi_data_volume_name, self)

        data_volume_path_1 = self._get_path(data_volume_id)

        #######################################
        #######################################
        # STEP 1: Take snapshot of running VM #
        #######################################
        #######################################
        vm_snapshot = VmSnapshot.create(
            self.apiClient,
            vmid=self.virtual_machine.id,
            snapshotmemory="false",
            name="Test Snapshot",
            description="Test Snapshot Desc"
        )

        list_vm_snapshots = VmSnapshot.list(self.apiClient, listAll="true")

        self._verify_vm_snapshot(list_vm_snapshots, vm_snapshot)

        root_volume_path_2 = self._get_path(root_volume_id)

        self.assertEqual(
            root_volume_path_1,
            root_volume_path_2,
            TestVMSnapshots._path_should_not_have_changed_err_msg
        )

        data_volume_path_2 = self._get_path(data_volume_id)

        self.assertEqual(
            data_volume_path_1,
            data_volume_path_2,
            TestVMSnapshots._path_should_not_have_changed_err_msg
        )

        root_volume_xen_sr = self.xen_session.xenapi.SR.get_by_name_label(sf_iscsi_root_volume_name)[0]

        root_volume_xen_vdis = self.xen_session.xenapi.SR.get_VDIs(root_volume_xen_sr)

        sf_util.check_list(root_volume_xen_vdis, 3, self, TestVMSnapshots._should_be_three_vdis_err_msg)

        root_volume_vdis_after_create = self._get_vdis(root_volume_xen_vdis)

        vdiSnapshotOf = self.xen_session.xenapi.VDI.get_record(root_volume_vdis_after_create.snapshot_vdi["snapshot_of"])

        self.assertEqual(
            vdiSnapshotOf["uuid"],
            root_volume_vdis_after_create.active_vdi["uuid"],
            TestVMSnapshots._snapshot_parent_not_correct_err_msg
        )

        data_volume_xen_sr = self.xen_session.xenapi.SR.get_by_name_label(sf_iscsi_data_volume_name)[0]

        data_volume_xen_vdis = self.xen_session.xenapi.SR.get_VDIs(data_volume_xen_sr)

        sf_util.check_list(data_volume_xen_vdis, 3, self, TestVMSnapshots._should_be_three_vdis_err_msg)

        data_volume_vdis_after_create = self._get_vdis(data_volume_xen_vdis)

        vdiSnapshotOf = self.xen_session.xenapi.VDI.get_record(data_volume_vdis_after_create.snapshot_vdi["snapshot_of"])

        self.assertEqual(
            vdiSnapshotOf["uuid"],
            data_volume_vdis_after_create.active_vdi["uuid"],
            TestVMSnapshots._snapshot_parent_not_correct_err_msg
        )

        #######################################
        #######################################
        ###  STEP 2: Revert VM to Snapshot  ###
        #######################################
        #######################################
        self.virtual_machine.stop(self.apiClient)

        VmSnapshot.revertToSnapshot(self.apiClient, vmsnapshotid=vm_snapshot.id)

        list_vm_snapshots = VmSnapshot.list(self.apiClient, listAll="true")

        sf_util.check_list(list_vm_snapshots, 1, self, TestVMSnapshots._should_only_be_one_vm_snapshot_err_msg)

        root_volume_path_3 = self._get_path(root_volume_id)

        self.assertNotEqual(
            root_volume_path_1,
            root_volume_path_3,
            TestVMSnapshots._path_should_have_changed_err_msg
        )

        root_volume_xen_vdis = self.xen_session.xenapi.SR.get_VDIs(root_volume_xen_sr)

        sf_util.check_list(root_volume_xen_vdis, 3, self, TestVMSnapshots._should_be_three_vdis_err_msg)

        root_volume_vdis_after_revert = self._get_vdis(root_volume_xen_vdis)

        self.assertNotEqual(
            root_volume_vdis_after_create.active_vdi["uuid"],
            root_volume_vdis_after_revert.active_vdi["uuid"],
            TestVMSnapshots._active_vdis_should_not_be_the_same_err_msg
        )

        self.assertEqual(
            root_volume_vdis_after_create.snapshot_vdi["uuid"],
            root_volume_vdis_after_revert.snapshot_vdi["uuid"],
            TestVMSnapshots._snapshot_vdis_should_be_the_same_err_msg
        )

        self.assertEqual(
            root_volume_vdis_after_create.base_vdi["uuid"],
            root_volume_vdis_after_revert.base_vdi["uuid"],
            TestVMSnapshots._base_vdis_should_be_the_same_err_msg
        )

        data_volume_path_3 = self._get_path(data_volume_id)

        self.assertNotEqual(
            data_volume_path_1,
            data_volume_path_3,
            TestVMSnapshots._path_should_have_changed_err_msg
        )

        data_volume_xen_vdis = self.xen_session.xenapi.SR.get_VDIs(data_volume_xen_sr)

        sf_util.check_list(data_volume_xen_vdis, 3, self, TestVMSnapshots._should_be_three_vdis_err_msg)

        data_volume_vdis_after_revert = self._get_vdis(data_volume_xen_vdis)

        self.assertNotEqual(
            data_volume_vdis_after_create.active_vdi["uuid"],
            data_volume_vdis_after_revert.active_vdi["uuid"],
            TestVMSnapshots._active_vdis_should_not_be_the_same_err_msg
        )

        self.assertEqual(
            data_volume_vdis_after_create.snapshot_vdi["uuid"],
            data_volume_vdis_after_revert.snapshot_vdi["uuid"],
            TestVMSnapshots._snapshot_vdis_should_be_the_same_err_msg
        )

        self.assertEqual(
            data_volume_vdis_after_create.base_vdi["uuid"],
            data_volume_vdis_after_revert.base_vdi["uuid"],
            TestVMSnapshots._base_vdis_should_be_the_same_err_msg
        )

        #######################################
        #######################################
        ##### STEP 3: Delete VM snapshot  #####
        #######################################
        #######################################
        VmSnapshot.deleteVMSnapshot(self.apiClient, vmsnapshotid=vm_snapshot.id)

        list_vm_snapshots = VmSnapshot.list(self.apiClient, listAll="true")

        self.assertEqual(
            list_vm_snapshots,
            None,
            TestVMSnapshots._should_be_no_vm_snapshots_err_msg
        )

        root_volume_path_4 = self._get_path(root_volume_id)

        self.assertEqual(
            root_volume_path_3,
            root_volume_path_4,
            TestVMSnapshots._path_should_not_have_changed_err_msg
        )

        root_volume_xen_vdis = self.xen_session.xenapi.SR.get_VDIs(root_volume_xen_sr)

        sf_util.check_list(root_volume_xen_vdis, 1, self, TestVMSnapshots._should_only_be_one_vdi_err_msg)

        root_volume_vdis_after_delete = self._get_vdis(root_volume_xen_vdis, True)

        self.assertEqual(
            root_volume_vdis_after_revert.active_vdi["uuid"],
            root_volume_vdis_after_delete.active_vdi["uuid"],
            TestVMSnapshots._active_vdis_should_be_the_same_err_msg
        )

        data_volume_path_4 = self._get_path(data_volume_id)

        self.assertEqual(
            data_volume_path_3,
            data_volume_path_4,
            TestVMSnapshots._path_should_not_have_changed_err_msg
        )

        data_volume_xen_vdis = self.xen_session.xenapi.SR.get_VDIs(data_volume_xen_sr)

        sf_util.check_list(data_volume_xen_vdis, 1, self, TestVMSnapshots._should_only_be_one_vdi_err_msg)

        data_volume_vdis_after_delete = self._get_vdis(data_volume_xen_vdis, True)

        self.assertEqual(
            data_volume_vdis_after_revert.active_vdi["uuid"],
            data_volume_vdis_after_delete.active_vdi["uuid"],
            TestVMSnapshots._active_vdis_should_be_the_same_err_msg
        )

        #######################################
        #######################################
        #####      STEP 4: Start VM       #####
        #######################################
        #######################################
        self.virtual_machine.detach_volume(self.apiClient, data_volume)

        self.virtual_machine.start(self.apiClient)

    def _get_path(self, volume_id):
        path_result = self.cs_api.getPathForVolume(volume_id)

        return path_result['apipathforvolume']['path']

    def _verify_vm_snapshot(self, list_vm_snapshots, vm_snapshot):
        sf_util.check_list(list_vm_snapshots, 1, self, TestVMSnapshots._should_only_be_one_vm_snapshot_err_msg)

        vm_snapshot_from_list = list_vm_snapshots[0]

        self.assertEqual(
            vm_snapshot.id,
            vm_snapshot_from_list.id,
            "There is a problem with the VM snapshot ID."
        )

        self.assertEqual(
            vm_snapshot.virtualmachineid,
            self.virtual_machine.id,
            "The ID of the snapshot's virtual machine does not match the expected virtual machine."
        )

        self.assertEqual(
            vm_snapshot.state,
            "Ready",
            "The snapshot is not in the 'Ready' state."
        )

    def _get_vdis(self, xen_vdis, only_active_expected=False):
        expected_number_of_vdis = 1 if only_active_expected else 3

        self.assertEqual(
            len(xen_vdis),
            expected_number_of_vdis,
            "The list had an unexpected number of items in it."
        )

        active_vdi = None
        snapshot_vdi = None
        base_vdi = None

        for temp_vdi in xen_vdis:
            temp = self.xen_session.xenapi.VDI.get_record(temp_vdi)

            if temp["name_label"] == "base copy":
                base_vdi = temp
            else:
                if temp["is_a_snapshot"] == True:
                    snapshot_vdi = temp
                else:
                    active_vdi = temp

        self.assertNotEqual(
            active_vdi,
            None,
            "The active VDI could not be located."
        )

        if only_active_expected:
            self.assertEqual(
                snapshot_vdi,
                None,
                "The snapshot VDI should not be present."
            )

            self.assertEqual(
                base_vdi,
                None,
                "The base VDI should not be present."
            )
        else:
            self.assertNotEqual(
                snapshot_vdi,
                None,
                "The snapshot VDI could not be located."
            )

            self.assertNotEqual(
                base_vdi,
                None,
                "The base VDI could not be located."
            )

        class VdiCollection(object):
            pass

        vdis = VdiCollection()

        vdis.active_vdi = active_vdi
        vdis.snapshot_vdi = snapshot_vdi
        vdis.base_vdi = base_vdi

        return vdis
