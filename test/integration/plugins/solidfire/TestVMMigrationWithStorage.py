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
import XenAPI

from solidfire.factory import ElementFactory

from util import sf_util

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, DiskOffering, ServiceOffering, StoragePool, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_hosts, list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

# Prerequisites:
#  Only one zone
#  Only one pod
#  Two clusters (have system VMs (including the VR) running on local or NFS storage)
#
# Running the tests:
#  Verify the "xen_server_hostname_src" and "xen_server_hostname_dest" variables are correct.


class TestData():
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId1 = "clusterId1"
    clusterId2 = "clusterId2"
    computeOffering1 = "computeoffering1"
    computeOffering2 = "computeoffering2"
    computeOffering3 = "computeoffering3"
    diskName = "diskname"
    diskOffering1 = "diskoffering1"
    diskOffering2 = "diskoffering2"
    domainId = "domainid"
    hypervisor = "hypervisor"
    login = "login"
    mvip = "mvip"
    name = "name"
    password = "password"
    podId = "podid"
    port = "port"
    primaryStorage = "primarystorage"
    primaryStorage2 = "primarystorage2"
    provider = "provider"
    scope = "scope"
    solidFire = "solidfire"
    storageTag = "SolidFire_SAN_1"
    storageTag2 = "SolidFire_Volume_1"
    tags = "tags"
    templateCacheName = "centos56-x86-64-xen"
    testAccount = "testaccount"
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    virtualMachine2 = "virtualmachine2"
    volume_1 = "volume_1"
    xenServer = "xenserver"
    zoneId = "zoneid"

    xen_server_hostname_src = "XenServer-6.5-1"
    xen_server_hostname_dest = "XenServer-6.5-3"

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
                "username": "test",
                "password": "test"
            },
            TestData.testAccount: {
                "email": "test2@test2.com",
                "firstname": "Jane",
                "lastname": "Doe",
                "username": "test2",
                "password": "test"
            },
            TestData.user: {
                "email": "user@test.com",
                "firstname": "Jane",
                "lastname": "Doe",
                "username": "testuser",
                "password": "password"
            },
            TestData.primaryStorage: {
                TestData.name: "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                TestData.url: "MVIP=10.117.40.120;SVIP=10.117.41.120;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 4500000,
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "Any",
                TestData.zoneId: 1
            },
            TestData.primaryStorage2: {
                TestData.name: "SolidFireShared-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: "MVIP=10.117.40.120;SVIP=10.117.41.120;" +
                        "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                        "minIops=5000;maxIops=50000;burstIops=75000",
                TestData.provider: "SolidFireShared",
                TestData.tags: TestData.storageTag2,
                TestData.capacityIops: 5000,
                TestData.capacityBytes: 1099511627776,
                TestData.hypervisor: "XenServer",
                TestData.podId: 1,
                TestData.zoneId: 1
            },
            TestData.virtualMachine: {
                "name": "TestVM",
                "displayname": "Test VM"
            },
            TestData.computeOffering1: {
                "name": "SF_CO_1",
                "displaytext": "SF_CO_1 (Min IOPS = 1,000; Max IOPS = 2,000)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": 1000,
                "maxiops": 2000,
                "hypervisorsnapshotreserve": 125,
                TestData.tags: TestData.storageTag,
            },
            TestData.computeOffering2: {
                "name": "SF_CO_2",
                "displaytext": "SF_CO_2 (Min IOPS = 1,000; Max IOPS = 2,000)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": 1000,
                "maxiops": 2000,
                "hypervisorsnapshotreserve": 100,
                TestData.tags: TestData.storageTag,
            },
            TestData.computeOffering3: {
                "name": "SF_CO_3",
                "displaytext": "SF_CO_3 Desc",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                TestData.tags: TestData.storageTag2,
            },
            TestData.diskOffering1: {
                "name": "SF_DO_1",
                "displaytext": "SF_DO_1 (Min IOPS = 3,000; Max IOPS = 6,000)",
                "disksize": 100,
                "customizediops": False,
                "miniops": 3000,
                "maxiops": 6000,
                "hypervisorsnapshotreserve": 125,
                TestData.tags: TestData.storageTag,
                "storagetype": "shared"
            },
            TestData.diskOffering2: {
                "name": "SF_DO_2",
                "displaytext": "SF_DO_2 (Min IOPS = 3,000; Max IOPS = 6,000)",
                "disksize": 100,
                "customizediops": False,
                "miniops": 3000,
                "maxiops": 6000,
                "hypervisorsnapshotreserve": 100,
                TestData.tags: TestData.storageTag,
                "storagetype": "shared"
            },
            TestData.volume_1: {
                TestData.diskName: "test-volume",
            },
            TestData.zoneId: 1,
            TestData.clusterId1: 1,
            TestData.clusterId2: 2,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }


class TestVMMigrationWithStorage(cloudstackTestCase):
    _sf_account_id_should_be_non_zero_int_err_msg = "The SolidFire account ID should be a non-zero integer."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestVMMigrationWithStorage, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        xenserver = cls.testdata[TestData.xenServer]

        # Set up xenAPI connection
        host_ip = "https://" + \
                  list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId1], name=TestData.xen_server_hostname_src)[0].ipaddress

        # Set up XenAPI connection
        cls.xen_session_1 = XenAPI.Session(host_ip)

        cls.xen_session_1.xenapi.login_with_password(xenserver[TestData.username], xenserver[TestData.password])

        # Set up xenAPI connection
        host_ip = "https://" + \
                  list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId2], name=TestData.xen_server_hostname_dest)[0].ipaddress

        # Set up XenAPI connection
        cls.xen_session_2 = XenAPI.Session(host_ip)

        cls.xen_session_2.xenapi.login_with_password(xenserver[TestData.username], xenserver[TestData.password])

        # Set up SolidFire connection
        solidfire = cls.testdata[TestData.solidFire]

        cls.sfe = ElementFactory.create(solidfire[TestData.mvip], solidfire[TestData.username], solidfire[TestData.password])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.cluster_1 = list_clusters(cls.apiClient, id=cls.testdata[TestData.clusterId1])[0]
        cls.cluster_2 = list_clusters(cls.apiClient, id=cls.testdata[TestData.clusterId2])[0]
        cls.template = get_template(cls.apiClient, cls.zone.id, cls.configData["ostype"])
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
            primarystorage
        )

        cls.compute_offering_1 = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering1]
        )

        cls.compute_offering_2 = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering2]
        )

        cls.compute_offering_3 = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering3]
        )

        cls.disk_offering_1 = DiskOffering.create(
            cls.apiClient,
            cls.testdata[TestData.diskOffering1]
        )

        cls.disk_offering_2 = DiskOffering.create(
            cls.apiClient,
            cls.testdata[TestData.diskOffering2]
        )

        # Resources that are to be destroyed
        cls._cleanup = [
            cls.compute_offering_1,
            cls.compute_offering_2,
            cls.compute_offering_3,
            cls.disk_offering_1,
            cls.disk_offering_2,
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
        try:
            cleanup_resources(self.apiClient, self.cleanup)

            sf_util.purge_solidfire_volumes(self.sfe)
        except Exception as e:
            logging.debug("Exception in tearDownClass(self): %s" % e)

    def test_01_storage_migrate_root_and_data_disks(self):
        src_host, dest_host = self._get_source_and_dest_hosts()

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering_1.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            hostid=src_host.id,
            startvm=True
        )

        self.cleanup.append(virtual_machine)

        cs_root_volume = list_volumes(self.apiClient, listall=True, virtualmachineid=virtual_machine.id)[0]

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self,
                                                  TestVMMigrationWithStorage._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_root_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_root_volume.name, self)

        cs_data_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_1],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering_1.id
        )

        self.cleanup.append(cs_data_volume)

        cs_data_volume = virtual_machine.attach_volume(
            self.apiClient,
            cs_data_volume
        )

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_data_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_data_volume.name, self)

        sf_root_volume, sf_data_volume = self._migrate_and_verify(virtual_machine, dest_host, cs_root_volume, cs_data_volume, sf_account_id,
                                                                  sf_root_volume, sf_data_volume, self.xen_session_1, self.xen_session_2)

        src_host, dest_host = dest_host, src_host

        self._migrate_and_verify(virtual_machine, dest_host, cs_root_volume, cs_data_volume, sf_account_id, sf_root_volume, sf_data_volume,
                                 self.xen_session_2, self.xen_session_1)

    def test_02_storage_migrate_root_and_data_disks(self):
        primarystorage2 = self.testdata[TestData.primaryStorage2]

        primary_storage_2 = StoragePool.create(
            self.apiClient,
            primarystorage2,
            clusterid=self.cluster_1.id
        )

        primary_storage_3 = StoragePool.create(
            self.apiClient,
            primarystorage2,
            clusterid=self.cluster_2.id
        )

        src_host, dest_host = self._get_source_and_dest_hosts()

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering_3.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            hostid=src_host.id,
            startvm=True
        )

        cs_data_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_1],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering_1.id
        )

        self.cleanup = [
            virtual_machine,
            cs_data_volume,
            primary_storage_2,
            primary_storage_3
        ]

        cs_data_volume = virtual_machine.attach_volume(
            self.apiClient,
            cs_data_volume
        )

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self,
                                                  TestVMMigrationWithStorage._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_data_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_data_volume.name, self)

        sf_data_volume = self._migrate_and_verify_one_disk_only(virtual_machine, dest_host, cs_data_volume, sf_account_id,
                                                                sf_data_volume, self.xen_session_1, self.xen_session_2)

        src_host, dest_host = dest_host, src_host

        self._migrate_and_verify_one_disk_only(virtual_machine, dest_host, cs_data_volume, sf_account_id, sf_data_volume,
                                               self.xen_session_2, self.xen_session_1)

    # The hypervisor snapshot reserve isn't large enough for either the compute or disk offering.
    def test_03_storage_migrate_root_and_data_disks_fail(self):
        self._execute_migration_failure(self.compute_offering_2.id, self.disk_offering_2.id)

    # The hypervisor snapshot reserve isn't large enough for the compute offering.
    def test_04_storage_migrate_root_disk_fails(self):
        self._execute_migration_failure(self.compute_offering_2.id, self.disk_offering_1.id)

    # The hypervisor snapshot reserve isn't large enough for the disk offering.
    def test_05_storage_migrate_data_disk_fails(self):
        self._execute_migration_failure(self.compute_offering_1.id, self.disk_offering_2.id)

    def _execute_migration_failure(self, compute_offering_id, disk_offering_id):
        src_host, dest_host = self._get_source_and_dest_hosts()

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=compute_offering_id,
            templateid=self.template.id,
            domainid=self.domain.id,
            hostid=src_host.id,
            startvm=True
        )

        self.cleanup.append(virtual_machine)

        cs_root_volume = list_volumes(self.apiClient, listall=True, virtualmachineid=virtual_machine.id)[0]

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self,
                                                  TestVMMigrationWithStorage._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_root_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_root_volume.name, self)

        cs_data_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_1],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=disk_offering_id
        )

        self.cleanup.append(cs_data_volume)

        cs_data_volume = virtual_machine.attach_volume(
            self.apiClient,
            cs_data_volume
        )

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_data_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_data_volume.name, self)

        self._fail_migrate_and_verify(virtual_machine, dest_host, cs_root_volume, cs_data_volume, sf_account_id,
                                      sf_root_volume, sf_data_volume, self.xen_session_1, self.xen_session_2)

    def _get_source_and_dest_hosts(self):
        hosts = list_hosts(self.apiClient)

        for host in hosts:
            if host.name == TestData.xen_server_hostname_src:
                src_host = host
            elif host.name == TestData.xen_server_hostname_dest:
                dest_host = host

        self.assertIsNotNone(src_host, "Could not locate the source host")

        self.assertIsNotNone(dest_host, "Could not locate the destination host")

        return src_host, dest_host

    def _migrate_and_verify(self, virtual_machine, dest_host, cs_root_volume, cs_data_volume, sf_account_id, src_sf_root_volume, src_sf_data_volume,
                            src_xen_session, dest_xen_session):
        self._verifyFields(cs_root_volume, src_sf_root_volume)
        self._verifyFields(cs_data_volume, src_sf_data_volume)

        virtual_machine.migrate_vm_with_volume(self.apiClient, dest_host.id)

        cs_root_volume = self._get_updated_cs_volume(cs_root_volume.id)
        cs_data_volume = self._get_updated_cs_volume(cs_data_volume.id)

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        dest_sf_root_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_root_volume.name, self)
        dest_sf_data_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_data_volume.name, self)

        self._verifyFields(cs_root_volume, dest_sf_root_volume)
        self._verifyFields(cs_data_volume, dest_sf_data_volume)

        self._verify_no_basic_volume_details()

        self._verify_different_volume_access_groups(src_sf_root_volume, dest_sf_root_volume)
        self._verify_different_volume_access_groups(src_sf_data_volume, dest_sf_data_volume)

        self._verify_same_account(src_sf_root_volume, dest_sf_root_volume)
        self._verify_same_account(src_sf_data_volume, dest_sf_data_volume)

        self._verifySfVolumeIds(src_sf_root_volume, dest_sf_root_volume)
        self._verifySfVolumeIds(src_sf_data_volume, dest_sf_data_volume)

        self._verify_xenserver_state(src_xen_session, src_sf_root_volume, dest_xen_session, dest_sf_root_volume)
        self._verify_xenserver_state(src_xen_session, src_sf_data_volume, dest_xen_session, dest_sf_data_volume)

        return dest_sf_root_volume, dest_sf_data_volume

    def _migrate_and_verify_one_disk_only(self, virtual_machine, dest_host, cs_volume, sf_account_id, src_sf_volume, src_xen_session, dest_xen_session):
        self._verifyFields(cs_volume, src_sf_volume)

        virtual_machine.migrate_vm_with_volume(self.apiClient, dest_host.id)

        cs_volume = self._get_updated_cs_volume(cs_volume.id)

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        dest_sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_volume.name, self)

        self._verifyFields(cs_volume, dest_sf_volume)

        self._verify_no_basic_volume_details()

        self._verify_different_volume_access_groups(src_sf_volume, dest_sf_volume)

        self._verify_same_account(src_sf_volume, dest_sf_volume)

        self._verifySfVolumeIds(src_sf_volume, dest_sf_volume)

        self._verify_xenserver_state(src_xen_session, src_sf_volume, dest_xen_session, dest_sf_volume)

        return dest_sf_volume

    def _fail_migrate_and_verify(self, virtual_machine, dest_host, cs_root_volume, cs_data_volume, sf_account_id, src_sf_root_volume, src_sf_data_volume,
                                 src_xen_session, dest_xen_session):
        self._verifyFields(cs_root_volume, src_sf_root_volume)
        self._verifyFields(cs_data_volume, src_sf_data_volume)

        class MigrationException(Exception):
            def __init__(self, *args, **kwargs):
                Exception.__init__(self, *args, **kwargs)

        try:
            virtual_machine.migrate_vm_with_volume(self.apiClient, dest_host.id)

            raise MigrationException("The migration did not fail (as expected).")
        except MigrationException:
            raise
        except Exception:
            pass

        self._verify_no_basic_volume_details()

        cs_root_volume_refreshed = self._get_updated_cs_volume(cs_root_volume.id)
        cs_data_volume_refreshed = self._get_updated_cs_volume(cs_data_volume.id)

        self._verifyFields(cs_root_volume_refreshed, src_sf_root_volume)
        self._verifyFields(cs_data_volume_refreshed, src_sf_data_volume)

        sf_volumes = sf_util.get_not_active_sf_volumes(self.sfe, sf_account_id)

        dest_sf_root_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_root_volume.name, self)
        dest_sf_data_volume = sf_util.check_and_get_sf_volume(sf_volumes, cs_data_volume.name, self)

        self._verify_xenserver_state(dest_xen_session, dest_sf_root_volume, src_xen_session, src_sf_root_volume)
        self._verify_xenserver_state(dest_xen_session, dest_sf_data_volume, src_xen_session, src_sf_data_volume)

    def _verify_different_volume_access_groups(self, src_sf_volume, dest_sf_volume):
        src_vags = src_sf_volume.volume_access_groups

        sf_util.check_list(src_vags, 1, self, "'src_vags' should be a list with only one element in it.")

        dest_vags = dest_sf_volume.volume_access_groups

        sf_util.check_list(dest_vags, 1, self, "'dest_vags' should be a list with only one element in it.")

        self.assertNotEqual(src_vags[0], dest_vags[0], "The source and destination volumes should not be in the same volume access group.")

    def _get_updated_cs_volume(self, cs_volume_id):
        return list_volumes(self.apiClient, listall=True, id=cs_volume_id)[0]

    def _verify_same_account(self, src_sf_volume, dest_sf_volume):
        self.assertEqual(src_sf_volume.account_id, dest_sf_volume.account_id, "The source and destination volumes should be in the same SolidFire account.")

    def _verifySfVolumeIds(self, src_sf_volume, dest_sf_volume):
        self.assertTrue(src_sf_volume.volume_id < dest_sf_volume.volume_id,
                     "The destination SolidFire root volume's ID should be greater than the id of the source one.")

    # verify the name, folder, and iscsi_name
    def _verifyFields(self, cs_volume, sf_volume):
        self.assertTrue(cs_volume.name == sf_volume.name, "The CloudStack volume name does not match the SolidFire volume name.")

        cs_volume_folder = self._get_cs_volume_folder(cs_volume.id)

        self.assertTrue(int(cs_volume_folder) == sf_volume.volume_id, "The CloudStack folder name does not match the SolidFire volume ID.")

        cs_volume_iscsi_name = self._get_cs_volume_iscsi_name(cs_volume.id)

        self.assertTrue(cs_volume_iscsi_name == sf_util.format_iqn(sf_volume.iqn), "The CloudStack volume iscsi_name does not match the SolidFire volume IQN.")

    def _get_cs_volume_property(self, cs_volume_id, volume_property):
        sql_query = "Select " + volume_property + " From volumes Where uuid = '" + cs_volume_id + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        return sql_result[0][0]

    def _get_cs_volume_folder(self, cs_volume_id):
        return self._get_cs_volume_property(cs_volume_id, "folder")

    def _get_cs_volume_iscsi_name(self, cs_volume_id):
        return self._get_cs_volume_property(cs_volume_id, "iscsi_name")

    def _verify_no_basic_volume_details(self):
        sql_query = "Select id From volume_details Where name like 'basic_'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        sf_util.check_list(sql_result, 0, self, "The cloud.volume_details table should not have any name fields that start with 'basic_'.")

    def _verify_xenserver_state(self, xen_session_1, sf_volume_1, xen_session_2, sf_volume_2):
        sr_name = sf_util.format_iqn(sf_volume_1.iqn)

        sf_util.check_xen_sr(sr_name, xen_session_1, self, False)

        sr_name = sf_util.format_iqn(sf_volume_2.iqn)

        sf_util.check_xen_sr(sr_name, xen_session_2, self)
