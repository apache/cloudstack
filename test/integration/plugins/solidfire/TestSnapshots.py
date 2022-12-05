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

from solidfire.factory import ElementFactory

from util import sf_util

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

from nose.plugins.attrib import attr

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, DiskOffering, ServiceOffering, Snapshot, StoragePool, Template, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_volumes, list_snapshots

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources, wait_until

# Prerequisites:
#  Only one zone
#  Only one pod
#  Only one cluster
#
# Running the tests:
#  Change the "hypervisor_type" variable to control which hypervisor type to test.
#  If using KVM, set the Global Setting "kvm.snapshot.enabled" equal to true.


class TestData():
    # constants
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    diskName = "diskname"
    diskOffering = "diskoffering"
    domainId = "domainId"
    hypervisor = "hypervisor"
    kvm = "kvm"
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
    vmWare = "vmware"
    volume_1 = "volume_1"
    volume_2 = "volume_2"
    xenServer = "xenserver"
    zoneId = "zoneId"

    # modify to control which hypervisor type to test
    hypervisor_type = xenServer

    def __init__(self):
        self.testdata = {
            TestData.solidFire: {
                TestData.mvip: "10.117.78.225",
                TestData.username: "admin",
                TestData.password: "admin",
                TestData.port: 443,
                TestData.url: "https://10.117.78.225:443"
            },
            TestData.primaryStorage: {
                "name": "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                "url": "MVIP=10.117.78.225;SVIP=10.117.94.225;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 4500000,
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "Any"
            },
            TestData.account: {
                "email": "test@test.com",
                "firstname": "John",
                "lastname": "Doe",
                "username": "test",
                "password": "test"
            },
            TestData.user: {
                "email": "user@test.com",
                "firstname": "Jane",
                "lastname": "Doe",
                TestData.username: "testuser",
                TestData.password: "password"
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
                "hypervisorsnapshotreserve": 200,
                "tags": "SolidFire_SAN_1"
            },
            TestData.diskOffering: {
                "name": "SF_DO_1",
                "displaytext": "SF_DO_1 (Min IOPS = 300; Max IOPS = 500)",
                "disksize": 128,
                "customizediops": False,
                "miniops": 300,
                "maxiops": 500,
                "hypervisorsnapshotreserve": 200,
                TestData.tags: TestData.storageTag,
                "storagetype": "shared"
            },
            TestData.volume_1: {
                TestData.diskName: "test-volume",
            },
            TestData.volume_2: {
                TestData.diskName: "test-volume-2",
            },
            TestData.zoneId: 1,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }


class TestSnapshots(cloudstackTestCase):
    _should_be_zero_volume_access_groups_in_list_err_msg = "There shouldn't be any volume access groups in this list."
    _should_be_zero_snapshots_in_list_err_msg = "There shouldn't be any snapshots in this list."
    _should_only_be_one_snapshot_in_list_err_msg = "There should only be one snapshot in this list."
    _should_be_two_snapshots_in_list_err_msg = "There should be two snapshots in this list."
    _should_be_three_snapshots_in_list_err_msg = "There should be three snapshots in this list."
    _should_be_zero_volumes_in_list_err_msg = "There shouldn't be any volumes in this list."
    _should_only_be_one_volume_in_list_err_msg = "There should only be one volume in this list."
    _should_be_two_volumes_in_list_err_msg = "There should be two volumes in this list."
    _should_be_three_volumes_in_list_err_msg = "There should be three volumes in this list."
    _should_be_four_volumes_in_list_err_msg = "There should be four volumes in this list."
    _should_be_five_volumes_in_list_err_msg = "There should be five volumes in this list."
    _should_be_six_volumes_in_list_err_msg = "There should be six volumes in this list."
    _should_be_seven_volumes_in_list_err_msg = "There should be seven volumes in this list."
    _should_be_five_items_in_list_err_msg = "There should be five items in this list."
    _should_be_six_items_in_list_err_msg = "There should be six items in this list."
    _should_be_seven_items_in_list_err_msg = "There should be seven items in this list."
    _sf_account_id_should_be_non_zero_int_err_msg = "The SolidFire account ID should be a non-zero integer."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestSnapshots, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        # Set up SolidFire connection
        solidfire = cls.testdata[TestData.solidFire]

        cls.sfe = ElementFactory.create(solidfire[TestData.mvip], solidfire[TestData.username], solidfire[TestData.password])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.template = get_template(cls.apiClient, cls.zone.id, hypervisor=TestData.hypervisor_type)
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])

        # Create test account
        cls.account = Account.create(
            cls.apiClient,
            cls.testdata["account"],
            admin=1
        )

        # Set up connection to make customized API calls
        cls.user = User.create(
            cls.apiClient,
            cls.testdata["user"],
            account=cls.account.name,
            domainid=cls.domain.id
        )

        url = cls.testdata[TestData.url]

        api_url = "http://" + url + ":8080/client/api"
        userkeys = User.registerUserKeys(cls.apiClient, cls.user.id)

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

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        cls.disk_offering = DiskOffering.create(
            cls.apiClient,
            cls.testdata[TestData.diskOffering]
        )

        # Resources that are to be destroyed
        cls._cleanup = [
            cls.compute_offering,
            cls.disk_offering,
            cls.user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)

            cls.primary_storage.delete(cls.apiClient)

            sf_util.purge_solidfire_volumes(cls.sfe)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiClient, self.cleanup)

    def test_01_create_volume_snapshot_using_sf_snapshot(self):
        if TestData.hypervisor_type == TestData.xenServer:
            sf_util.set_supports_resign(True, self.dbConnection)

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestSnapshots._sf_account_id_should_be_non_zero_int_err_msg)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        primary_storage_db_id = self._get_cs_storage_pool_db_id(self.primary_storage)

        vol_snap_1 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        vol_snap_3 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 3, TestSnapshots._should_be_three_snapshots_in_list_err_msg)

        self._delete_and_test_snapshot(vol_snap_2)

        self._delete_and_test_snapshot(vol_snap_1)

        self._delete_and_test_snapshot(vol_snap_3)

        vol_snap_1 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        self._delete_and_test_snapshot(vol_snap_1)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        self._delete_and_test_snapshot(vol_snap_2)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 0, self, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        vol_snap_1 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        vol_snap_3 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 3, TestSnapshots._should_be_three_snapshots_in_list_err_msg)

        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostypeid": self.template.ostypeid, "ispublic": "true"}

        template = Template.create_from_snapshot(self.apiClient, vol_snap_2, services)

        self.cleanup.append(template)

        virtual_machine_dict = {"name": "TestVM2", "displayname": "Test VM 2"}

        virtual_machine_2 = VirtualMachine.create(
            self.apiClient,
            virtual_machine_dict,
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine_2.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_2_root_volume = list_volumes_response[0]
        vm_2_root_volume_name = vm_2_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 2, self, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots_2 = self.sfe.list_snapshots(volume_id=sf_volume_2.volume_id).snapshots

        sf_util.check_list(sf_snapshots_2, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        vol_snap_a = self._create_and_test_snapshot(vm_2_root_volume, sf_volume_2, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot = Volume.create_from_snapshot(self.apiClient, vol_snap_a.id, services, account=self.account.name, domainid=self.domain.id)

        volume_created_from_snapshot_name = volume_created_from_snapshot.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 3, self, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)
        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        sf_util.check_list(sf_volume_3.volume_access_groups, 0, self, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        volume_created_from_snapshot = virtual_machine.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_snapshot(vol_snap_a)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 3, self, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)
        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        volume_created_from_snapshot = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_snapshot(vol_snap_2)

        self._delete_and_test_snapshot(vol_snap_3)

        self._delete_and_test_snapshot(vol_snap_1)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 2, self, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        virtual_machine_2.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        list_volumes_response = list_volumes(
            self.apiClient,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        data_volume = list_volumes_response[0]

        data_volume = Volume(data_volume.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 0, self, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

    @attr(hypervisor='XenServer')
    def test_02_create_volume_snapshot_using_sf_volume(self):
        if TestData.hypervisor_type != TestData.xenServer:
            return

        sf_util.set_supports_resign(False, self.dbConnection)

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestSnapshots._sf_account_id_should_be_non_zero_int_err_msg)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        primary_storage_db_id = self._get_cs_storage_pool_db_id(self.primary_storage)

        sf_volume_id = sf_volume.volume_id
        sf_volume_size = sf_volume.total_size

        vol_snap_1 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 1, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 2, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        vol_snap_3 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 3, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_2, sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_1, sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_3, sf_account_id, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vol_snap_1 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 4, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 5, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 2, self, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_1, sf_account_id, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_2, sf_account_id, 0, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        sf_volume_id = sf_volume.volume_id
        sf_volume_size = sf_volume.total_size

        vol_snap_1 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 1, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 2, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        vol_snap_3 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 3, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostypeid": self.template.ostypeid, "ispublic": "true"}

        template = Template.create_from_snapshot(self.apiClient, vol_snap_2, services)

        self.cleanup.append(template)

        virtual_machine_dict = {"name": "TestVM2", "displayname": "Test VM 2"}

        virtual_machine_2 = VirtualMachine.create(
            self.apiClient,
            virtual_machine_dict,
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine_2.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_2_root_volume = list_volumes_response[0]
        vm_2_root_volume_name = vm_2_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 5, self, TestSnapshots._should_be_five_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots_2 = self.sfe.list_snapshots(volume_id=sf_volume_2.volume_id).snapshots

        sf_util.check_list(sf_snapshots_2, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        sf_volume_id_2 = sf_volume_2.volume_id
        sf_volume_size_2 = sf_volume_2.total_size

        vol_snap_a = self._create_and_test_snapshot_2(vm_2_root_volume.id, sf_volume_id_2, sf_volume_id + 5, primary_storage_db_id, sf_volume_size_2,
                                                      sf_account_id, 6, TestSnapshots._should_be_six_volumes_in_list_err_msg)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot = Volume.create_from_snapshot(self.apiClient, vol_snap_a.id, services, account=self.account.name, domainid=self.domain.id)

        volume_created_from_snapshot_name = volume_created_from_snapshot.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 7, self, TestSnapshots._should_be_seven_volumes_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)
        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        sf_util.check_list(sf_volume_3.volume_access_groups, 0, self, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        volume_created_from_snapshot = virtual_machine.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_snapshot_2(vol_snap_a, sf_account_id, 6, TestSnapshots._should_be_six_volumes_in_list_err_msg)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 5, self, TestSnapshots._should_be_five_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        volume_created_from_snapshot = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_snapshot_2(vol_snap_2, sf_account_id, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_3, sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_1, sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        virtual_machine_2.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        list_volumes_response = list_volumes(
            self.apiClient,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        data_volume = list_volumes_response[0]

        data_volume = Volume(data_volume.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 0, self, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        time.sleep(60)

        virtual_machine.stop(self.apiClient, True)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        sf_volume_id = sf_volume.volume_id
        sf_volume_size = sf_volume.total_size

        vol_snap_1 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 1, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 2, self, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot = Volume.create_from_snapshot(self.apiClient, vol_snap_1.id, services, account=self.account.name, domainid=self.domain.id)

        volume_created_from_snapshot_name = volume_created_from_snapshot.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 3, self, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        sf_util.check_list(sf_volume_2.volume_access_groups, 0, self, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        volume_created_from_snapshot = virtual_machine.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        sf_volume_id_2 = sf_volume_2.volume_id
        sf_volume_size_2 = sf_volume_2.total_size

        vol_snap_a = self._create_and_test_snapshot_2(volume_created_from_snapshot.id, sf_volume_id_2, sf_volume_id + 3, primary_storage_db_id, sf_volume_size_2,
                                                      sf_account_id, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_1, sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_a, sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        list_volumes_response = list_volumes(
            self.apiClient,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vol_snap_a = self._create_and_test_snapshot_2(volume_created_from_snapshot.id, sf_volume_id_2, sf_volume_id + 4, primary_storage_db_id, sf_volume_size_2,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        data_volume = list_volumes_response[0]

        data_volume = Volume(data_volume.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        list_volumes_response = list_volumes(
            self.apiClient,
            listall=True
        )

        self.assertEqual(
            list_volumes_response,
            None,
            "'list_volumes_response' should be equal to 'None'."
        )

        self._delete_and_test_snapshot_2(vol_snap_a, sf_account_id, 0, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

    @attr(hypervisor='XenServer')
    def test_03_create_volume_snapshot_using_sf_volume_and_sf_snapshot(self):
        if TestData.hypervisor_type != TestData.xenServer:
            return

        sf_util.set_supports_resign(False, self.dbConnection)

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestSnapshots._sf_account_id_should_be_non_zero_int_err_msg)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        primary_storage_db_id = self._get_cs_storage_pool_db_id(self.primary_storage)

        sf_volume_id = sf_volume.volume_id
        sf_volume_size = sf_volume.total_size

        vol_snap_1 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 1, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 2, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_util.set_supports_resign(True, self.dbConnection)

        vol_snap_a = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_b = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostypeid": self.template.ostypeid, "ispublic": "true"}

        template_1 = Template.create_from_snapshot(self.apiClient, vol_snap_1, services)

        self.cleanup.append(template_1)

        virtual_machine_dict = {"name": "TestVM2", "displayname": "Test VM 2"}

        virtual_machine_2 = VirtualMachine.create(
            self.apiClient,
            virtual_machine_dict,
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=template_1.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine_2.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_2_root_volume = list_volumes_response[0]
        vm_2_root_volume_name = vm_2_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 4, self, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume_2.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot_1 = Volume.create_from_snapshot(self.apiClient, vol_snap_2.id, services, account=self.account.name, domainid=self.domain.id)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 5, self, TestSnapshots._should_be_five_volumes_in_list_err_msg)

        volume_created_from_snapshot_1 = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot_1
        )

        services = {"displaytext": "Template-A", "name": "Template-A-name", "ostypeid": self.template.ostypeid, "ispublic": "true"}

        template_a = Template.create_from_snapshot(self.apiClient, vol_snap_a, services)

        self.cleanup.append(template_a)

        virtual_machine_dict = {"name": "TestVM3", "displayname": "Test VM 3"}

        virtual_machine_3 = VirtualMachine.create(
            self.apiClient,
            virtual_machine_dict,
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=template_a.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine_3.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_3_root_volume = list_volumes_response[0]
        vm_3_root_volume_name = vm_3_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 6, self, TestSnapshots._should_be_six_volumes_in_list_err_msg)

        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_3_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume_3.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        services = {"diskname": "Vol-A", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot_a = Volume.create_from_snapshot(self.apiClient, vol_snap_b.id, services, account=self.account.name, domainid=self.domain.id)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 7, self, TestSnapshots._should_be_seven_volumes_in_list_err_msg)

        volume_created_from_snapshot_a = virtual_machine_3.attach_volume(
            self.apiClient,
            volume_created_from_snapshot_a
        )

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        # should still be 7 volumes because the SolidFire volume for the root disk of the VM just destroyed
        # is still needed for the SolidFire snapshots
        sf_util.check_list(sf_volumes, 7, self, TestSnapshots._should_be_seven_volumes_in_list_err_msg)

        virtual_machine_2.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 6, self, TestSnapshots._should_be_six_volumes_in_list_err_msg)

        virtual_machine_3.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 5, self, TestSnapshots._should_be_five_volumes_in_list_err_msg)

        data_volume = Volume(volume_created_from_snapshot_a.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 4, self, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        data_volume = Volume(volume_created_from_snapshot_1.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 3, self, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_1, sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        self._delete_and_test_snapshot(vol_snap_b)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        # should still be 2 volumes because the SolidFire volume for the root disk of the VM just destroyed
        # is still needed for the SolidFire snapshots
        sf_util.check_list(sf_volumes, 2, self, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        self._delete_and_test_snapshot(vol_snap_a)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_2, sf_account_id, 0, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

    @attr(hypervisor='XenServer')
    def test_04_create_volume_snapshot_using_sf_snapshot_and_archiving(self):
        if TestData.hypervisor_type != TestData.xenServer:
            return

        sf_util.set_supports_resign(True, self.dbConnection)

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestSnapshots._sf_account_id_should_be_non_zero_int_err_msg)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        primary_storage_db_id = self._get_cs_storage_pool_db_id(self.primary_storage)

        vol_snap_1_archive = self._create_and_test_archive_snapshot(vm_1_root_volume.id, sf_volume)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_3_archive = self._create_and_test_archive_snapshot(vm_1_root_volume.id, sf_volume)

        vol_snap_4 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        self._delete_and_test_archive_snapshot(vol_snap_3_archive)

        self._delete_and_test_snapshot(vol_snap_2)

        self._delete_and_test_snapshot(vol_snap_4)

        self._delete_and_test_archive_snapshot(vol_snap_1_archive)

        vol_snap_1_archive = self._create_and_test_archive_snapshot(vm_1_root_volume.id, sf_volume)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        self._delete_and_test_archive_snapshot(vol_snap_1_archive)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        self._delete_and_test_snapshot(vol_snap_2)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 0, self, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        vol_snap_1 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2_archive = self._create_and_test_archive_snapshot(vm_1_root_volume.id, sf_volume)

        vol_snap_3 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        vol_snap_4_archive = self._create_and_test_archive_snapshot(vm_1_root_volume.id, sf_volume)

        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostypeid": self.template.ostypeid, "ispublic": "true"}

        template = Template.create_from_snapshot(self.apiClient, vol_snap_2_archive, services)

        self.cleanup.append(template)

        virtual_machine_dict = {"name": "TestVM2", "displayname": "Test VM 2"}

        virtual_machine_2 = VirtualMachine.create(
            self.apiClient,
            virtual_machine_dict,
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine_2.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_2_root_volume = list_volumes_response[0]
        vm_2_root_volume_name = vm_2_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 2, self, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots_2 = self.sfe.list_snapshots(volume_id=sf_volume_2.volume_id).snapshots

        sf_util.check_list(sf_snapshots_2, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        vol_snap_a_archive = self._create_and_test_archive_snapshot(vm_2_root_volume.id, sf_volume_2)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot = Volume.create_from_snapshot(self.apiClient, vol_snap_a_archive.id, services, account=self.account.name, domainid=self.domain.id)

        volume_created_from_snapshot_name = volume_created_from_snapshot.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 3, self, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)
        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        sf_util.check_list(sf_volume_3.volume_access_groups, 0, self, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        volume_created_from_snapshot = virtual_machine.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_archive_snapshot(vol_snap_a_archive)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 3, self, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)
        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        volume_created_from_snapshot = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_archive_snapshot(vol_snap_4_archive)

        self._delete_and_test_snapshot(vol_snap_1)

        self._delete_and_test_archive_snapshot(vol_snap_2_archive)

        self._delete_and_test_snapshot(vol_snap_3)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 2, self, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        virtual_machine_2.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume_3 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        list_volumes_response = list_volumes(
            self.apiClient,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        data_volume = list_volumes_response[0]

        data_volume = Volume(data_volume.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 0, self, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

    def test_05_create_volume_snapshot_using_sf_snapshot_and_revert_volume_to_snapshot(self):
        if TestData.hypervisor_type != TestData.vmWare and TestData.hypervisor_type != TestData.kvm:
            return

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestSnapshots._sf_account_id_should_be_non_zero_int_err_msg)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 1, self, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = sf_util.get_sf_volume_by_name(self, sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume.volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        primary_storage_db_id = self._get_cs_storage_pool_db_id(self.primary_storage)

        vol_snap_1 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        virtual_machine.stop(self.apiClient, False)

        if TestData.hypervisor_type == TestData.vmWare:
            try:
                Volume.revertToSnapshot(self.apiClient, vol_snap_1.id)

                self.assertTrue(False, "An exception should have been thrown when trying to revert a volume to a snapshot and the volume is a root disk on VMware.")
            except:
                pass
        else:
            Volume.revertToSnapshot(self.apiClient, vol_snap_1.id)

        virtual_machine.start(self.apiClient)

        try:
            Volume.revertToSnapshot(self.apiClient, vol_snap_1.id)

            self.assertTrue(False, "An exception should have been thrown when trying to revert a volume to a snapshot and the volume is attached to a running VM.")
        except:
            pass

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot = Volume.create_from_snapshot(self.apiClient, vol_snap_2.id, services, account=self.account.name, domainid=self.domain.id)

        volume_created_from_snapshot_name = volume_created_from_snapshot.name

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, 2, self, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        sf_volume_2 = sf_util.get_sf_volume_by_name(self, sf_volumes, volume_created_from_snapshot_name)

        self._delete_and_test_snapshot(vol_snap_2)

        self._delete_and_test_snapshot(vol_snap_1)

        vol_snap_1 = self._create_and_test_snapshot(volume_created_from_snapshot, sf_volume_2, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot(volume_created_from_snapshot, sf_volume_2, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        volume_created_from_snapshot = virtual_machine.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        virtual_machine.stop(self.apiClient, False)

        Volume.revertToSnapshot(self.apiClient, vol_snap_1.id)

        virtual_machine.start(self.apiClient)

        try:
            Volume.revertToSnapshot(self.apiClient, vol_snap_1.id)

            self.assertTrue(False, "An exception should have been thrown when trying to revert a volume to a snapshot and the volume is attached to a running VM.")
        except:
            pass

        self._delete_and_test_snapshot(vol_snap_2)

        self._delete_and_test_snapshot(vol_snap_1)

        virtual_machine.delete(self.apiClient, True)

        volume_created_from_snapshot = Volume(volume_created_from_snapshot.__dict__)

        volume_created_from_snapshot.delete(self.apiClient)

    # used when SolidFire snapshots are being used for CloudStack volume snapshots
    def _check_snapshot_details(self, sf_snapshot_details, cs_snapshot_id, volume, sf_volume_id, sf_snapshot_id, storage_pool_id, sf_volume_size):
        if TestData.hypervisor_type == TestData.vmWare:
            expected_num_details = 7

            err_msg = TestSnapshots._should_be_seven_items_in_list_err_msg
        else:
            expected_num_details = 6

            err_msg = TestSnapshots._should_be_six_items_in_list_err_msg

        volume_id = sf_util.get_cs_volume_db_id(self.dbConnection, volume)

        sf_util.check_list(sf_snapshot_details, expected_num_details, self, err_msg)

        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "takeSnapshot", "true")
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "volumeId", sf_volume_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "snapshotId", sf_snapshot_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "sfStoragePoolId", storage_pool_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "sfVolumeSize", sf_volume_size)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "originalCloudStackVolumeId", volume_id)

        if TestData.hypervisor_type == TestData.vmWare:
            self._check_snapshot_detail_with(str.endswith, sf_snapshot_details, cs_snapshot_id, "vmdk", ".vmdk")

    # used when SolidFire volumes are being used for CloudStack volume snapshots
    def _check_snapshot_details_2(self, sf_snapshot_details, cs_snapshot_id, sf_volume_id, storage_pool_id, sf_volume_size):
        sf_util.check_list(sf_snapshot_details, 5, self, TestSnapshots._should_be_five_items_in_list_err_msg)

        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "volumeId", sf_volume_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "sfStoragePoolId", storage_pool_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "sfVolumeSize", sf_volume_size)
        self._check_snapshot_detail_with(str.startswith, sf_snapshot_details, cs_snapshot_id, "iqn", "/iqn.")
        self._check_snapshot_detail_size(sf_snapshot_details, cs_snapshot_id, "path", 36)

    def _check_snapshot_detail(self, sf_snapshot_details_list, cs_snapshot_id, snapshot_detail_key, snapshot_detail_value):
        for sf_snapshot_detail_dict in sf_snapshot_details_list:
            if sf_snapshot_detail_dict["volumeSnapshotId"] != cs_snapshot_id:
                raise Exception("This snapshot detail does not apply to the expected CloudStack volume snapshot.")

            if sf_snapshot_detail_dict["snapshotDetailsName"] == snapshot_detail_key:
                if sf_snapshot_detail_dict["snapshotDetailsValue"] == str(snapshot_detail_value):
                    return

        raise Exception("There is a problem with the snapshot details key '" + snapshot_detail_key + "' and value '" + str(snapshot_detail_value) + "'.")

    def _check_snapshot_detail_with(self, with_f, sf_snapshot_details_list, cs_snapshot_id, snapshot_detail_key, with_str):
        for sf_snapshot_detail_dict in sf_snapshot_details_list:
            if sf_snapshot_detail_dict["volumeSnapshotId"] != cs_snapshot_id:
                raise Exception("This snapshot detail does not apply to the expected CloudStack volume snapshot.")

            if sf_snapshot_detail_dict["snapshotDetailsName"] == snapshot_detail_key:
                if with_f(str(sf_snapshot_detail_dict["snapshotDetailsValue"]), with_str):
                    return

        raise Exception("There is a problem with the snapshot details key '" + snapshot_detail_key + "' and 'starts with/ends with' value '" + with_str + "'.")

    def _check_snapshot_detail_size(self, sf_snapshot_details_list, cs_snapshot_id, snapshot_detail_key, length):
        for sf_snapshot_detail_dict in sf_snapshot_details_list:
            if sf_snapshot_detail_dict["volumeSnapshotId"] != cs_snapshot_id:
                raise Exception("This snapshot detail does not apply to the expected CloudStack volume snapshot.")

            if sf_snapshot_detail_dict["snapshotDetailsName"] == snapshot_detail_key:
                if len(sf_snapshot_detail_dict["snapshotDetailsValue"]) == length:
                    return

        raise Exception("There is a problem with the snapshot details key '" + snapshot_detail_key + "' and 'length' value '" + str(length) + "'.")

    def _most_recent_sf_snapshot(self, sf_snapshots):
        sf_util.check_list_not_empty(self, sf_snapshots)

        most_recent_id = 0
        sf_snapshot_to_return = None

        for sf_snapshot in sf_snapshots:
            if (sf_snapshot.snapshot_id > most_recent_id):
                sf_snapshot_to_return = sf_snapshot

                most_recent_id = sf_snapshot.snapshot_id

        if (sf_snapshot_to_return == None):
            raise Exception("Unable to find the most recent SolidFire snapshot in the provided list")

        return sf_snapshot_to_return

    def _get_cs_volume_snapshot_db_id(self, vol_snap):
        return sf_util.get_db_id(self.dbConnection, "snapshots", vol_snap)

    def _get_cs_storage_pool_db_id(self, storage_pool):
        return sf_util.get_db_id(self.dbConnection, "storage_pool", storage_pool)

    def _get_sf_volume_by_id(self, sf_volumes, sf_volume_id):
        sf_util.check_list_not_empty(self, sf_volumes)

        sf_volume = None

        for volume in sf_volumes:
            if volume.volume_id == sf_volume_id:
                sf_volume = volume

                break

        self.assertNotEqual(
            sf_volume,
            None,
            "The SolidFire volume could not be found in the expected account."
        )

        return sf_volume

    def _get_snapshot_detail(self, sf_snapshot_details_list, key):
        for sf_snapshot_detail_dict in sf_snapshot_details_list:
            if sf_snapshot_detail_dict["snapshotDetailsName"] == key:
                return sf_snapshot_detail_dict["snapshotDetailsValue"]

        raise Exception("Unable to find the following snapshot details key: " + key)

    def _check_sf_snapshot_does_not_exist(self, sf_snapshots, sf_snapshot_id):
        for sf_snapshot in sf_snapshots:
            if sf_snapshot.snapshot_id == sf_snapshot:
                raise Exception("The following SolidFire snapshot ID should not exist: " + sf_snapshot_id)

    def _check_snapshot_details_do_not_exist(self, vol_snap_db_id):
        sql_query = "Select count(*) From snapshot_details Where snapshot_id = " + str(vol_snap_db_id)

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        self.assertEqual(
            sql_result[0][0],
            0,
            "Snapshot details should not exist for the following CloudStack volume snapshot DB ID: " + str(vol_snap_db_id)
        )

    # used when SolidFire snapshots are being used for CloudStack volume snapshots
    def _create_and_test_snapshot(self, volume_for_snapshot, sf_volume, primary_storage_db_id, expected_num_snapshots, snapshot_err_msg):
        vol_snap = Snapshot.create(
            self.apiClient,
            volume_id=volume_for_snapshot.id
        )

        self._wait_for_snapshot_state(vol_snap.id, Snapshot.BACKED_UP)

        sf_volume_id = sf_volume.volume_id

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume_id).snapshots

        sf_util.check_list(sf_snapshots, expected_num_snapshots, self, snapshot_err_msg)

        sf_snapshot = self._most_recent_sf_snapshot(sf_snapshots)

        sf_snapshot_details_request = {'snapshotid': vol_snap.id}
        sf_snapshot_details_response = self.cs_api.getVolumeSnapshotDetails(sf_snapshot_details_request)
        sf_snapshot_details = sf_snapshot_details_response['null'] # 'null' gets me the list that's in the dictionary

        vol_snap_db_id = self._get_cs_volume_snapshot_db_id(vol_snap)

        self._check_snapshot_details(sf_snapshot_details, vol_snap_db_id, volume_for_snapshot, sf_volume_id, sf_snapshot.snapshot_id, primary_storage_db_id, sf_volume.total_size)

        return vol_snap

    # used when SolidFire snapshots are being used for CloudStack volume snapshots to create a backup on secondary storage
    def _create_and_test_archive_snapshot(self, volume_id_for_snapshot, sf_volume):
        sf_volume_id = sf_volume.volume_id

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots_orig = self.sfe.list_snapshots(sf_volume_id).snapshots

        vol_snap = Snapshot.create(
            self.apiClient,
            volume_id=volume_id_for_snapshot,
            locationtype="secondary"
        )

        self._wait_for_snapshot_state(vol_snap.id, Snapshot.BACKED_UP)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(sf_volume_id).snapshots

        sf_util.check_list(sf_snapshots, len(sf_snapshots_orig), self, "A new SolidFire snapshot was detected.")

        vol_snap_db_id = self._get_cs_volume_snapshot_db_id(vol_snap)

        self._check_snapshot_details_do_not_exist(vol_snap_db_id)

        return vol_snap

    # used when SolidFire volumes are being used for CloudStack volume snapshots
    def _create_and_test_snapshot_2(self, volume_id_for_snapshot, sf_volume_id, sf_volume_id_for_volume_snapshot, primary_storage_db_id, sf_volume_size,
                                    sf_account_id, expected_num_volumes, volume_err_msg):
        vol_snap = Snapshot.create(
            self.apiClient,
            volume_id=volume_id_for_snapshot
        )

        self._wait_for_snapshot_state(vol_snap.id, Snapshot.BACKED_UP)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume_id).snapshots

        sf_util.check_list(sf_snapshots, 0, self, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        sf_snapshot_details_request = {'snapshotid': vol_snap.id}
        sf_snapshot_details_response = self.cs_api.getVolumeSnapshotDetails(sf_snapshot_details_request)
        sf_snapshot_details = sf_snapshot_details_response['null'] # 'null' gets me the list that's in the dictionary

        vol_snap_db_id = self._get_cs_volume_snapshot_db_id(vol_snap)

        self._check_snapshot_details_2(sf_snapshot_details, vol_snap_db_id, sf_volume_id_for_volume_snapshot, primary_storage_db_id, sf_volume_size)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, expected_num_volumes, self, volume_err_msg)

        sf_volume_for_snapshot = self._get_sf_volume_by_id(sf_volumes, sf_volume_id_for_volume_snapshot)

        sf_util.check_list(sf_volume_for_snapshot.volume_access_groups, 0, self, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        return vol_snap

    def _wait_for_snapshot_state(self, vol_snap_id, snapshot_state):
        retry_interval = 10
        num_tries = 10

        wait_result, return_val = wait_until(retry_interval, num_tries, TestSnapshots._check_snapshot_state, self.apiClient, vol_snap_id, snapshot_state)

        if not wait_result:
            raise Exception(return_val)

    @staticmethod
    def _check_snapshot_state(api_client, vol_snap_id, snapshot_state):
        volume_snapshot = list_snapshots(
            api_client,
            id=vol_snap_id,
            listall=True
        )[0]

        if str(volume_snapshot.state).lower() == snapshot_state.lower():
            return True, ""

        return False, "The snapshot is not in the '" + snapshot_state + "' state. State = " + str(volume_snapshot.state)

    # used when SolidFire snapshots are being used for CloudStack volume snapshots
    def _delete_and_test_snapshot(self, vol_snap):
        vol_snap_id = vol_snap.id
        vol_snap_db_id = self._get_cs_volume_snapshot_db_id(vol_snap)

        sf_snapshot_details_request = {'snapshotid': vol_snap_id}
        sf_snapshot_details_response = self.cs_api.getVolumeSnapshotDetails(sf_snapshot_details_request)
        sf_snapshot_details = sf_snapshot_details_response['null'] # 'null' gets me the list that's in the dictionary

        sf_volume_id = self._get_snapshot_detail(sf_snapshot_details, "volumeId")
        sf_snapshot_id = self._get_snapshot_detail(sf_snapshot_details, "snapshotId")

        vol_snap.delete(self.apiClient)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sfe.list_snapshots(volume_id=sf_volume_id).snapshots

        self._check_sf_snapshot_does_not_exist(sf_snapshots, sf_snapshot_id)

        self._check_snapshot_details_do_not_exist(vol_snap_db_id)

    # used when SolidFire snapshots are being used for CloudStack volume snapshots to create a backup on secondary storage
    def _delete_and_test_archive_snapshot(self, vol_snap):
        vol_snap.delete(self.apiClient)

    # used when SolidFire volumes are being used for CloudStack volume snapshots
    def _delete_and_test_snapshot_2(self, vol_snap, sf_account_id, expected_num_volumes, volume_err_msg):
        vol_snap_db_id = self._get_cs_volume_snapshot_db_id(vol_snap)

        vol_snap.delete(self.apiClient)

        self._check_snapshot_details_do_not_exist(vol_snap_db_id)

        # Get volume information from SolidFire cluster
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_util.check_list(sf_volumes, expected_num_volumes, self, volume_err_msg)
