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

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

from nose.plugins.attrib import attr

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, DiskOffering, ServiceOffering, Snapshot, StoragePool, Template, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_hosts, list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

from solidfire import solidfire_element_api as sf_api

# on April 10, 2016: Ran 3 tests in 7742.481s with three hosts
# on May 2, 2016: Ran 3 tests in 7409.770s with two hosts


class TestData():
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    diskName = "diskname"
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
    templateName = "templatename"
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    volume_1 = "volume_1"
    volume_2 = "volume_2"
    xenServer = "xenserver"
    zoneId = "zoneId"

    def __init__(self):
        self.testdata = {
            TestData.solidFire: {
                TestData.mvip: "192.168.139.112",
                TestData.login: "admin",
                TestData.password: "admin",
                TestData.port: 443,
                TestData.url: "https://192.168.139.112:443"
            },
            TestData.xenServer: {
                TestData.username: "root",
                TestData.password: "solidfire"
            },
            TestData.account: {
                "email": "test@test.com",
                "firstname": "John",
                "lastname": "Doe",
                "username": "test",
                "password": "test"
            },
            "testaccount": {
                "email": "test2@test2.com",
                "firstname": "Jane",
                "lastname": "Doe",
                TestData.username: "test2",
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
                "url": "MVIP=192.168.139.112;SVIP=10.10.8.112;" +
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
            "testdiskofferings": {
                "customiopsdo": {
                    "name": "SF_Custom_Iops_DO",
                    "displaytext": "Customized Iops DO",
                    "disksize": 128,
                    "customizediops": True,
                    "miniops": 500,
                    "maxiops": 1000,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "customsizedo": {
                    "name": "SF_Custom_Size_DO",
                    "displaytext": "Customized Size DO",
                    "disksize": 175,
                    "customizediops": False,
                    "miniops": 500,
                    "maxiops": 1000,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "customsizeandiopsdo": {
                    "name": "SF_Custom_Iops_Size_DO",
                    "displaytext": "Customized Size and Iops DO",
                    "disksize": 200,
                    "customizediops": True,
                    "miniops": 400,
                    "maxiops": 800,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "newiopsdo": {
                    "name": "SF_New_Iops_DO",
                    "displaytext": "New Iops (min=350, max = 700)",
                    "disksize": 128,
                    "miniops": 350,
                    "maxiops": 700,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "newsizedo": {
                    "name": "SF_New_Size_DO",
                    "displaytext": "New Size: 175",
                    "disksize": 175,
                    "miniops": 400,
                    "maxiops": 800,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "newsizeandiopsdo": {
                    "name": "SF_New_Size_Iops_DO",
                    "displaytext": "New Size and Iops",
                    "disksize": 200,
                    "miniops": 200,
                    "maxiops": 400,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                }
            },
            TestData.volume_1: {
                TestData.diskName: "test-volume",
            },
            TestData.volume_2: {
                TestData.diskName: "test-volume-2",
            },
            TestData.templateName: "CentOS 5.6(64-bit) no GUI (XenServer)",
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "192.168.129.50"
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
    _sf_account_id_should_be_non_zero_int_err_msg = "The SolidFire account ID should be a non-zero integer."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestSnapshots, cls).getClsTestClient()
        cls.apiClient = testclient.getApiClient()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        # Set up xenAPI connection
        host_ip = "https://" + \
                  list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name="XenServer-6.5-1")[0].ipaddress

        # Set up XenAPI connection
        cls.xen_session = XenAPI.Session(host_ip)

        xenserver = cls.testdata[TestData.xenServer]

        cls.xen_session.xenapi.login_with_password(xenserver[TestData.username], xenserver[TestData.password])

        # Set up SolidFire connection
        cls.sf_client = sf_api.SolidFireAPI(endpoint_dict=cls.testdata[TestData.solidFire])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.cluster = list_clusters(cls.apiClient)[0]
        cls.template = get_template(cls.apiClient, cls.zone.id, template_name=cls.testdata[TestData.templateName])
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

            cls._purge_solidfire_volumes()
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiClient, self.cleanup)

    @attr(hypervisor='XenServer')
    def test_01_create_volume_snapshot_using_sf_snapshot(self):
        self._set_supports_resign(True)

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        sf_account_id = self._get_sf_account_id(self.account.id, self.primary_storage.id)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume['volumeID'])

        self._check_list(sf_snapshots, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        primary_storage_db_id = self._get_cs_storage_pool_db_id(self.primary_storage)

        vol_snap_1 = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        vol_snap_3 = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 3, TestSnapshots._should_be_three_snapshots_in_list_err_msg)

        self._delete_and_test_snapshot(vol_snap_2)

        self._delete_and_test_snapshot(vol_snap_1)

        self._delete_and_test_snapshot(vol_snap_3)

        vol_snap_1 = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)

        self._delete_and_test_snapshot(vol_snap_1)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)

        self._delete_and_test_snapshot(vol_snap_2)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 0, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume['volumeID'])

        self._check_list(sf_snapshots, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        vol_snap_1 = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        vol_snap_3 = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 3, TestSnapshots._should_be_three_snapshots_in_list_err_msg)

        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostype": "CentOS 5.6 (64-bit)", "ispublic": "true"}

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_2_root_volume = list_volumes_response[0]
        vm_2_root_volume_name = vm_2_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, vm_2_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots_2 = self.sf_client.list_snapshots(volume_id=sf_volume_2['volumeID'])

        self._check_list(sf_snapshots_2, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        vol_snap_a = self._create_and_test_snapshot(vm_2_root_volume.id, sf_volume_2, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "size": 100, "ispublic": True}

        volume_created_from_snapshot = Volume.create_from_snapshot(self.apiClient, vol_snap_a.id, services, account=self.account.name, domainid=self.domain.id)

        volume_created_from_snapshot_name = volume_created_from_snapshot.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)
        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = self._get_sf_volume_by_name(sf_volumes, volume_created_from_snapshot_name)

        self._check_list(sf_volume_3['volumeAccessGroups'], 0, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        volume_created_from_snapshot = virtual_machine.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_snapshot(vol_snap_a)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)
        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = self._get_sf_volume_by_name(sf_volumes, volume_created_from_snapshot_name)

        volume_created_from_snapshot = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_snapshot(vol_snap_2)

        self._delete_and_test_snapshot(vol_snap_3)

        self._delete_and_test_snapshot(vol_snap_1)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = self._get_sf_volume_by_name(sf_volumes, volume_created_from_snapshot_name)

        virtual_machine_2.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume_3 = self._get_sf_volume_by_name(sf_volumes, volume_created_from_snapshot_name)

        list_volumes_response = list_volumes(
            self.apiClient,
            listall=True
        )

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        data_volume = list_volumes_response[0]

        data_volume = Volume(data_volume.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 0, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

    @attr(hypervisor='XenServer')
    def test_02_create_volume_snapshot_using_sf_volume(self):
        self._set_supports_resign(False)

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        sf_account_id = self._get_sf_account_id(self.account.id, self.primary_storage.id)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume['volumeID'])

        self._check_list(sf_snapshots, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        primary_storage_db_id = self._get_cs_storage_pool_db_id(self.primary_storage)

        sf_volume_id = sf_volume['volumeID']
        sf_volume_size = sf_volume['totalSize']

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
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume['volumeID'])

        self._check_list(sf_snapshots, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        sf_volume_id = sf_volume['volumeID']
        sf_volume_size = sf_volume['totalSize']

        vol_snap_1 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 1, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 2, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        vol_snap_3 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 3, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostype": "CentOS 5.6 (64-bit)", "ispublic": "true"}

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_2_root_volume = list_volumes_response[0]
        vm_2_root_volume_name = vm_2_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 5, TestSnapshots._should_be_five_volumes_in_list_err_msg)

        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, vm_2_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots_2 = self.sf_client.list_snapshots(volume_id=sf_volume_2['volumeID'])

        self._check_list(sf_snapshots_2, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        sf_volume_id_2 = sf_volume_2['volumeID']
        sf_volume_size_2 = sf_volume_2['totalSize']

        vol_snap_a = self._create_and_test_snapshot_2(vm_2_root_volume.id, sf_volume_id_2, sf_volume_id + 5, primary_storage_db_id, sf_volume_size_2,
                                                      sf_account_id, 6, TestSnapshots._should_be_six_volumes_in_list_err_msg)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "size": 100, "ispublic": True}

        volume_created_from_snapshot = Volume.create_from_snapshot(self.apiClient, vol_snap_a.id, services, account=self.account.name, domainid=self.domain.id)

        volume_created_from_snapshot_name = volume_created_from_snapshot.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 7, TestSnapshots._should_be_seven_volumes_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)
        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = self._get_sf_volume_by_name(sf_volumes, volume_created_from_snapshot_name)

        self._check_list(sf_volume_3['volumeAccessGroups'], 0, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        volume_created_from_snapshot = virtual_machine.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_snapshot_2(vol_snap_a, sf_account_id, 6, TestSnapshots._should_be_six_volumes_in_list_err_msg)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 5, TestSnapshots._should_be_five_volumes_in_list_err_msg)

        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, vm_2_root_volume_name)
        sf_volume_3 = self._get_sf_volume_by_name(sf_volumes, volume_created_from_snapshot_name)

        volume_created_from_snapshot = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        self._delete_and_test_snapshot_2(vol_snap_2, sf_account_id, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_3, sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_1, sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        virtual_machine_2.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        list_volumes_response = list_volumes(
            self.apiClient,
            listall=True
        )

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        data_volume = list_volumes_response[0]

        data_volume = Volume(data_volume.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 0, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        time.sleep(60)

        virtual_machine.stop(self.apiClient, True)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)

        sf_volume_id = sf_volume['volumeID']
        sf_volume_size = sf_volume['totalSize']

        vol_snap_1 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 1, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "size": 100, "ispublic": True}

        volume_created_from_snapshot = Volume.create_from_snapshot(self.apiClient, vol_snap_1.id, services, account=self.account.name, domainid=self.domain.id)

        volume_created_from_snapshot_name = volume_created_from_snapshot.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, volume_created_from_snapshot_name)

        self._check_list(sf_volume_2['volumeAccessGroups'], 0, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        volume_created_from_snapshot = virtual_machine.attach_volume(
            self.apiClient,
            volume_created_from_snapshot
        )

        sf_volume_id_2 = sf_volume_2['volumeID']
        sf_volume_size_2 = sf_volume_2['totalSize']

        vol_snap_a = self._create_and_test_snapshot_2(volume_created_from_snapshot.id, sf_volume_id_2, sf_volume_id + 3, primary_storage_db_id, sf_volume_size_2,
                                                      sf_account_id, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_1, sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_a, sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        list_volumes_response = list_volumes(
            self.apiClient,
            listall=True
        )

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vol_snap_a = self._create_and_test_snapshot_2(volume_created_from_snapshot.id, sf_volume_id_2, sf_volume_id + 4, primary_storage_db_id, sf_volume_size_2,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        data_volume = list_volumes_response[0]

        data_volume = Volume(data_volume.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

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
        self._set_supports_resign(False)

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]
        vm_1_root_volume_name = vm_1_root_volume.name

        sf_account_id = self._get_sf_account_id(self.account.id, self.primary_storage.id)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        sf_volume = self._get_sf_volume_by_name(sf_volumes, vm_1_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume['volumeID'])

        self._check_list(sf_snapshots, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        primary_storage_db_id = self._get_cs_storage_pool_db_id(self.primary_storage)

        sf_volume_id = sf_volume['volumeID']
        sf_volume_size = sf_volume['totalSize']

        vol_snap_1 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 1, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        vol_snap_2 = self._create_and_test_snapshot_2(vm_1_root_volume.id, sf_volume_id, sf_volume_id + 2, primary_storage_db_id, sf_volume_size,
                                                      sf_account_id, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        self._set_supports_resign(True)

        vol_snap_a = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 1, TestSnapshots._should_only_be_one_snapshot_in_list_err_msg)

        vol_snap_b = self._create_and_test_snapshot(vm_1_root_volume.id, sf_volume, primary_storage_db_id, 2, TestSnapshots._should_be_two_snapshots_in_list_err_msg)

        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostype": "CentOS 5.6 (64-bit)", "ispublic": "true"}

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_2_root_volume = list_volumes_response[0]
        vm_2_root_volume_name = vm_2_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        sf_volume_2 = self._get_sf_volume_by_name(sf_volumes, vm_2_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume_2['volumeID'])

        self._check_list(sf_snapshots, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "size": 100, "ispublic": True}

        volume_created_from_snapshot_1 = Volume.create_from_snapshot(self.apiClient, vol_snap_2.id, services, account=self.account.name, domainid=self.domain.id)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 5, TestSnapshots._should_be_five_volumes_in_list_err_msg)

        volume_created_from_snapshot_1 = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot_1
        )

        services = {"displaytext": "Template-A", "name": "Template-A-name", "ostype": "CentOS 5.6 (64-bit)", "ispublic": "true"}

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

        self._check_list(list_volumes_response, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        vm_3_root_volume = list_volumes_response[0]
        vm_3_root_volume_name = vm_3_root_volume.name

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 6, TestSnapshots._should_be_six_volumes_in_list_err_msg)

        sf_volume_3 = self._get_sf_volume_by_name(sf_volumes, vm_3_root_volume_name)

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume_3['volumeID'])

        self._check_list(sf_snapshots, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        services = {"diskname": "Vol-A", "zoneid": self.testdata[TestData.zoneId], "size": 100, "ispublic": True}

        volume_created_from_snapshot_a = Volume.create_from_snapshot(self.apiClient, vol_snap_b.id, services, account=self.account.name, domainid=self.domain.id)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 7, TestSnapshots._should_be_seven_volumes_in_list_err_msg)

        volume_created_from_snapshot_a = virtual_machine_3.attach_volume(
            self.apiClient,
            volume_created_from_snapshot_a
        )

        virtual_machine.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        # should still be 7 volumes because the SolidFire volume for the root disk of the VM just destroyed
        # is still needed for the SolidFire snapshots
        self._check_list(sf_volumes, 7, TestSnapshots._should_be_seven_volumes_in_list_err_msg)

        virtual_machine_2.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 6, TestSnapshots._should_be_six_volumes_in_list_err_msg)

        virtual_machine_3.delete(self.apiClient, True)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 5, TestSnapshots._should_be_five_volumes_in_list_err_msg)

        data_volume = Volume(volume_created_from_snapshot_a.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 4, TestSnapshots._should_be_four_volumes_in_list_err_msg)

        data_volume = Volume(volume_created_from_snapshot_1.__dict__)

        data_volume.delete(self.apiClient)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 3, TestSnapshots._should_be_three_volumes_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_1, sf_account_id, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        self._delete_and_test_snapshot(vol_snap_b)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        # should still be 2 volumes because the SolidFire volume for the root disk of the VM just destroyed
        # is still needed for the SolidFire snapshots
        self._check_list(sf_volumes, 2, TestSnapshots._should_be_two_volumes_in_list_err_msg)

        self._delete_and_test_snapshot(vol_snap_a)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, 1, TestSnapshots._should_only_be_one_volume_in_list_err_msg)

        self._delete_and_test_snapshot_2(vol_snap_2, sf_account_id, 0, TestSnapshots._should_be_zero_volumes_in_list_err_msg)

    def _set_supports_resign(self, supports_resign):
        supports_resign = str(supports_resign)

        sql_query = "Update host_details Set value = '" + supports_resign + "' Where name = 'supportsResign'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        self.dbConnection.execute(sql_query)

    def _check_list(self, in_list, expected_size_of_list, err_msg):
        self.assertEqual(
            isinstance(in_list, list),
            True,
            "'in_list' is not a list."
        )

        self.assertEqual(
            len(in_list),
            expected_size_of_list,
            err_msg
        )

    def _check_list_not_empty(self, in_list):
        self.assertEqual(
            isinstance(in_list, list),
            True,
            "'in_list' is not a list."
        )

        self.assertGreater(
            len(in_list),
            0,
            "The size of 'in_list' must be greater than zero."
        )

    # used when SolidFire snapshots are being used for CloudStack volume snapshots
    def _check_snapshot_details(self, sf_snapshot_details, cs_snapshot_id, sf_volume_id, sf_snapshot_id, storage_pool_id, sf_volume_size):
        self._check_list(sf_snapshot_details, 5, TestSnapshots._should_be_five_items_in_list_err_msg)

        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "takeSnapshot", "true")
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "volumeId", sf_volume_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "snapshotId", sf_snapshot_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "sfStoragePoolId", storage_pool_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "sfVolumeSize", sf_volume_size)

    # used when SolidFire volumes are being used for CloudStack volume snapshots
    def _check_snapshot_details_2(self, sf_snapshot_details, cs_snapshot_id, sf_volume_id, storage_pool_id, sf_volume_size):
        self._check_list(sf_snapshot_details, 5, TestSnapshots._should_be_five_items_in_list_err_msg)

        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "volumeId", sf_volume_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "sfStoragePoolId", storage_pool_id)
        self._check_snapshot_detail(sf_snapshot_details, cs_snapshot_id, "sfVolumeSize", sf_volume_size)
        self._check_snapshot_detail_starts_with(sf_snapshot_details, cs_snapshot_id, "iqn", "/iqn.")
        self._check_snapshot_detail_size(sf_snapshot_details, cs_snapshot_id, "path", 36)

    def _check_snapshot_detail(self, sf_snapshot_details_list, cs_snapshot_id, snapshot_detail_key, snapshot_detail_value):
        for sf_snapshot_detail_dict in sf_snapshot_details_list:
            if sf_snapshot_detail_dict["volumeSnapshotId"] != cs_snapshot_id:
                raise Exception("This snapshot detail does not apply to the expected CloudStack volume snapshot.")

            if sf_snapshot_detail_dict["snapshotDetailsName"] == snapshot_detail_key:
                if sf_snapshot_detail_dict["snapshotDetailsValue"] == str(snapshot_detail_value):
                    return

        raise Exception("There is a problem with the snapshot details key '" + snapshot_detail_key + "' and value '" + str(snapshot_detail_value) + "'.")

    def _check_snapshot_detail_starts_with(self, sf_snapshot_details_list, cs_snapshot_id, snapshot_detail_key, starts_with):
        for sf_snapshot_detail_dict in sf_snapshot_details_list:
            if sf_snapshot_detail_dict["volumeSnapshotId"] != cs_snapshot_id:
                raise Exception("This snapshot detail does not apply to the expected CloudStack volume snapshot.")

            if sf_snapshot_detail_dict["snapshotDetailsName"] == snapshot_detail_key:
                if sf_snapshot_detail_dict["snapshotDetailsValue"].startswith(starts_with):
                    return

        raise Exception("There is a problem with the snapshot details key '" + snapshot_detail_key + "' and 'starts with' value '" + starts_with + "'.")

    def _check_snapshot_detail_size(self, sf_snapshot_details_list, cs_snapshot_id, snapshot_detail_key, length):
        for sf_snapshot_detail_dict in sf_snapshot_details_list:
            if sf_snapshot_detail_dict["volumeSnapshotId"] != cs_snapshot_id:
                raise Exception("This snapshot detail does not apply to the expected CloudStack volume snapshot.")

            if sf_snapshot_detail_dict["snapshotDetailsName"] == snapshot_detail_key:
                if len(sf_snapshot_detail_dict["snapshotDetailsValue"]) == length:
                    return

        raise Exception("There is a problem with the snapshot details key '" + snapshot_detail_key + "' and 'length' value '" + str(length) + "'.")

    def _most_recent_sf_snapshot(self, sf_snapshots):
        self._check_list_not_empty(sf_snapshots)

        most_recent_id = 0
        sf_snapshot_to_return = None

        for sf_snapshot in sf_snapshots:
            if (sf_snapshot['snapshotID'] > most_recent_id):
                sf_snapshot_to_return = sf_snapshot

                most_recent_id = sf_snapshot['snapshotID']

        if (sf_snapshot_to_return == None):
            raise Exception("Unable to find the most recent SolidFire snapshot in the provided list")

        return sf_snapshot_to_return

    def _get_cs_volume_snapshot_db_id(self, vol_snap):
        return self._get_db_id("snapshots", vol_snap)

    def _get_cs_storage_pool_db_id(self, storage_pool):
        return self._get_db_id("storage_pool", storage_pool)

    def _get_db_id(self, table, db_obj):
        sql_query = "Select id From " + table + " Where uuid = '" + str(db_obj.id) + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        return sql_result[0][0]

    def _get_sf_volume_by_name(self, sf_volumes, sf_volume_name):
        self._check_list_not_empty(sf_volumes)

        sf_volume = None

        for volume in sf_volumes:
            if volume['name'] == sf_volume_name:
                sf_volume = volume

                break

        self.assertNotEqual(
            sf_volume,
            None,
            "The SolidFire volume could not be found in the expected account."
        )

        return sf_volume

    def _get_sf_volume_by_id(self, sf_volumes, sf_volume_id):
        self._check_list_not_empty(sf_volumes)

        sf_volume = None

        for volume in sf_volumes:
            if volume['volumeID'] == sf_volume_id:
                sf_volume = volume

                break

        self.assertNotEqual(
            sf_volume,
            None,
            "The SolidFire volume could not be found in the expected account."
        )

        return sf_volume

    def _get_sf_account_id(self, cs_account_id, primary_storage_id):
        sf_account_id_request = {'accountid': cs_account_id, 'storageid': primary_storage_id}
        sf_account_id_result = self.cs_api.getSolidFireAccountId(sf_account_id_request)
        sf_account_id = sf_account_id_result['apisolidfireaccountid']['solidFireAccountId']

        self.assertEqual(
            isinstance(sf_account_id, int),
            True,
            TestSnapshots._sf_account_id_should_be_non_zero_int_err_msg
        )

        return sf_account_id

    def _get_snapshot_detail(self, sf_snapshot_details_list, key):
        for sf_snapshot_detail_dict in sf_snapshot_details_list:
            if sf_snapshot_detail_dict["snapshotDetailsName"] == key:
                return sf_snapshot_detail_dict["snapshotDetailsValue"]

        raise Exception("Unable to find the following snapshot details key: " + key)

    def _check_sf_snapshot_does_not_exist(self, sf_snapshots, sf_snapshot_id):
        for sf_snapshot in sf_snapshots:
            if sf_snapshot["snapshotID"] == sf_snapshot:
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
    def _create_and_test_snapshot(self, volume_id_for_snapshot, sf_volume, primary_storage_db_id, expected_num_snapshots, snapshot_err_msg):
        vol_snap = Snapshot.create(
            self.apiClient,
            volume_id=volume_id_for_snapshot
        )

        sf_volume_id = sf_volume['volumeID']

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume_id)

        self._check_list(sf_snapshots, expected_num_snapshots, snapshot_err_msg)

        sf_snapshot = self._most_recent_sf_snapshot(sf_snapshots)

        sf_snapshot_details_request = {'snapshotid': vol_snap.id}
        sf_snapshot_details_response = self.cs_api.getVolumeSnapshotDetails(sf_snapshot_details_request)
        sf_snapshot_details = sf_snapshot_details_response['null'] # 'null' gets me the list that's in the dictionary

        vol_snap_db_id = self._get_cs_volume_snapshot_db_id(vol_snap)

        self._check_snapshot_details(sf_snapshot_details, vol_snap_db_id, sf_volume_id, sf_snapshot['snapshotID'], primary_storage_db_id, sf_volume['totalSize'])

        return vol_snap

    # used when SolidFire volumes are being used for CloudStack volume snapshots
    def _create_and_test_snapshot_2(self, volume_id_for_snapshot, sf_volume_id, sf_volume_id_for_volume_snapshot, primary_storage_db_id, sf_volume_size,
                                    sf_account_id, expected_num_volumes, volume_err_msg):
        vol_snap = Snapshot.create(
            self.apiClient,
            volume_id=volume_id_for_snapshot
        )

        # Get snapshot information for volume from SolidFire cluster
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume_id)

        self._check_list(sf_snapshots, 0, TestSnapshots._should_be_zero_snapshots_in_list_err_msg)

        sf_snapshot_details_request = {'snapshotid': vol_snap.id}
        sf_snapshot_details_response = self.cs_api.getVolumeSnapshotDetails(sf_snapshot_details_request)
        sf_snapshot_details = sf_snapshot_details_response['null'] # 'null' gets me the list that's in the dictionary

        vol_snap_db_id = self._get_cs_volume_snapshot_db_id(vol_snap)

        self._check_snapshot_details_2(sf_snapshot_details, vol_snap_db_id, sf_volume_id_for_volume_snapshot, primary_storage_db_id, sf_volume_size)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, expected_num_volumes, volume_err_msg)

        sf_volume_for_snapshot = self._get_sf_volume_by_id(sf_volumes, sf_volume_id_for_volume_snapshot)

        self._check_list(sf_volume_for_snapshot['volumeAccessGroups'], 0, TestSnapshots._should_be_zero_volume_access_groups_in_list_err_msg)

        return vol_snap

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
        sf_snapshots = self.sf_client.list_snapshots(volume_id=sf_volume_id)

        self._check_sf_snapshot_does_not_exist(sf_snapshots, sf_snapshot_id)

        self._check_snapshot_details_do_not_exist(vol_snap_db_id)

    # used when SolidFire volumes are being used for CloudStack volume snapshots
    def _delete_and_test_snapshot_2(self, vol_snap, sf_account_id, expected_num_volumes, volume_err_msg):
        vol_snap_db_id = self._get_cs_volume_snapshot_db_id(vol_snap)

        vol_snap.delete(self.apiClient)

        self._check_snapshot_details_do_not_exist(vol_snap_db_id)

        # Get volume information from SolidFire cluster
        sf_volumes = self.sf_client.list_volumes_for_account(account_id=sf_account_id)

        self._check_list(sf_volumes, expected_num_volumes, volume_err_msg)

    @classmethod
    def _purge_solidfire_volumes(cls):
        deleted_volumes = cls.sf_client.list_deleted_volumes()

        for deleted_volume in deleted_volumes:
            cls.sf_client.purge_deleted_volume(deleted_volume['volumeID'])

