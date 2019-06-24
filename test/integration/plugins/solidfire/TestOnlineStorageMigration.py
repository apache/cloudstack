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

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, ServiceOffering, Snapshot, StoragePool, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import list_clusters, get_domain, list_hosts, list_snapshots, get_template, list_volumes, get_zone

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

# Prerequisites:
#  Only one zone
#  Only one pod
#  Only one cluster
#
# Running the tests:
#  If using XenServer, verify the "xen_server_hostname" variable is correct.
#
# Note:
#  If you do have more than one cluster, you might need to change this line: cls.cluster = list_clusters(cls.apiClient)[0]


class TestData():
    # constants
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    domainId = "domainId"
    email = "email"
    firstname = "firstname"
    hypervisor = "hypervisor"
    lastname = "lastname"
    managementServer = "managementServer"
    mvip = "mvip"
    name = "name"
    nfs_folder = "/export/primary2"
    nfs_path = "nfs://10.117.40.114" + nfs_folder
    nfs_storage_tag = "NFS-123"
    password = "password"
    podId = "podId"
    port = "port"
    primaryStorage = "primarystorage"
    primaryStorage2 = "primarystorage2"
    provider = "provider"
    scope = "scope"
    solidFire = "solidfire"
    storageTag = "SolidFire_SAN_1"
    tags = "tags"
    url = "url"
    user = "user"
    username = "username"
    xenServer = "xenserver"
    zoneId = "zoneId"

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
            TestData.managementServer: {
                TestData.username: "cloudstack",
                TestData.password: "solidfire"
            },
            TestData.account: {
                TestData.email: "test@test.com",
                TestData.firstname: "John",
                TestData.lastname: "Doe",
                TestData.username: "test",
                TestData.password: "test"
            },
            TestData.user: {
                TestData.email: "user@test.com",
                TestData.firstname: "Jane",
                TestData.lastname: "Doe",
                TestData.username: "testuser",
                TestData.password: "password"
            },
            TestData.primaryStorage: {
                TestData.name: "NFS-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.clusterId: 1,
                TestData.url: TestData.nfs_path,
                TestData.tags: TestData.nfs_storage_tag
            },
            TestData.primaryStorage2: {
                TestData.name: "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                TestData.url: "MVIP=10.117.40.120;SVIP=10.117.41.120;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 4500000,
                TestData.capacityBytes: 2251799813685248, # 2 PiB
                TestData.hypervisor: "Any"
            },
            TestData.computeOffering: {
                TestData.name: "SF_CO_1",
                "displaytext": "SF_CO_1 (Min IOPS = 300; Max IOPS = 600)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": "300",
                "maxiops": "600",
                "hypervisorsnapshotreserve": 200,
                TestData.tags: TestData.nfs_storage_tag
            },
            TestData.zoneId: 1,
            TestData.podId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }


class TestOnlineStorageMigration(cloudstackTestCase):
    _should_only_be_one_volume_in_list_err_msg = "There should only be one volume in this list."
    _volume_not_on_correct_primary_storage = "The volume is not on the correct primary storage."
    _snapshot_not_associated_with_correct_volume = "The snapshot is not associated with the correct volume."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestOnlineStorageMigration, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        sf_util.set_supports_resign(True, cls.dbConnection)

        cls._connect_to_hypervisor()

        # Set up SolidFire connection
        solidfire = cls.testdata[TestData.solidFire]

        cls.sfe = ElementFactory.create(solidfire[TestData.mvip], solidfire[TestData.username], solidfire[TestData.password])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.cluster = list_clusters(cls.apiClient)[0]
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
            podid=cls.testdata[TestData.podId],
            clusterid=cls.cluster.id,
            tags=primarystorage[TestData.tags]
        )

        primarystorage2 = cls.testdata[TestData.primaryStorage2]

        cls.primary_storage_2 = StoragePool.create(
            cls.apiClient,
            primarystorage2,
            scope=primarystorage2[TestData.scope],
            zoneid=cls.zone.id,
            provider=primarystorage2[TestData.provider],
            tags=primarystorage2[TestData.tags],
            capacityiops=primarystorage2[TestData.capacityIops],
            capacitybytes=primarystorage2[TestData.capacityBytes],
            hypervisor=primarystorage2[TestData.hypervisor]
        )

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        # Resources that are to be destroyed
        cls._cleanup = [
            cls.compute_offering,
            cls.user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)

            cls.primary_storage.delete(cls.apiClient)
            cls.primary_storage_2.delete(cls.apiClient)

            ms = cls.testdata[TestData.managementServer]
            ip_address = cls.testdata[TestData.url]

            cls._delete_all_files(ip_address, ms[TestData.username], ms[TestData.password], TestData.nfs_folder)

            sf_util.purge_solidfire_volumes(cls.sfe)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiClient, self.cleanup)

    def test_online_migrate_volume_from_nfs_storage_to_managed_storage(self):
        if TestData.hypervisor_type != TestData.xenServer:
            return

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self._get_vm_name(),
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        self.cleanup.append(virtual_machine)

        vm_root_volume = self._get_only_volume(virtual_machine.id)

        self._verify_volume_on_primary_storage(vm_root_volume, self.primary_storage)

        # Migrate the root disk from NFS storage to managed storage.

        Volume.migrate(self.apiClient, livemigrate=True, volumeid=vm_root_volume.id, storageid=self.primary_storage_2.id)

        vm_root_volume = self._get_only_volume(virtual_machine.id)

        self._verify_volume_on_primary_storage(vm_root_volume, self.primary_storage_2)

    def test_online_migrate_volume_with_snapshot_from_nfs_storage_to_managed_storage(self):
        if TestData.hypervisor_type != TestData.xenServer:
            return

        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self._get_vm_name(),
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        self.cleanup.append(virtual_machine)

        vm_root_volume = self._get_only_volume(virtual_machine.id)

        self._verify_volume_on_primary_storage(vm_root_volume, self.primary_storage)

        vol_snap = Snapshot.create(
            self.apiClient,
            volume_id=vm_root_volume.id
        )

        self.cleanup.append(vol_snap)

        self._verify_snapshot_belongs_to_volume(vol_snap.id, vm_root_volume.id)

        # Migrate the root disk from NFS storage to managed storage.

        Volume.migrate(self.apiClient, livemigrate=True, volumeid=vm_root_volume.id, storageid=self.primary_storage_2.id)

        vm_root_volume = self._get_only_volume(virtual_machine.id)

        self._verify_volume_on_primary_storage(vm_root_volume, self.primary_storage_2)

        self._verify_snapshot_belongs_to_volume(vol_snap.id, vm_root_volume.id)

    def _verify_snapshot_belongs_to_volume(self, snapshot_id, volume_id):
        snapshot = list_snapshots(
            self.apiClient,
            id=snapshot_id
        )[0]

        self.assertEqual(
            snapshot.volumeid,
            volume_id,
            TestOnlineStorageMigration._snapshot_not_associated_with_correct_volume
        )

    def _get_only_volume(self, virtual_machine_id):
        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine_id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestOnlineStorageMigration._should_only_be_one_volume_in_list_err_msg)

        return list_volumes_response[0]

    def _verify_volume_on_primary_storage(self, vm_root_volume, primary_storage):
        self.assertEqual(
            vm_root_volume.storageid,
            primary_storage.id,
            TestOnlineStorageMigration._volume_not_on_correct_primary_storage
        )

    def _get_vm_name(self):
        number = random.randint(0, 1000)

        vm_name = {
            TestData.name: "VM-%d" % number,
            "displayname": "Test VM %d" % number
        }

        return vm_name

    @classmethod
    def _delete_all_files(cls, ip_address, username, password, path):
        ssh_connection = sf_util.get_ssh_connection(ip_address, username, password)

        ssh_connection.exec_command("sudo rm -rf " + path + "/*")

        ssh_connection.close()

    @classmethod
    def _connect_to_hypervisor(cls):
        host_ip = "https://" + \
              list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name=TestData.xen_server_hostname)[0].ipaddress

        cls.xen_session = XenAPI.Session(host_ip)

        xen_server = cls.testdata[TestData.xenServer]

        cls.xen_session.xenapi.login_with_password(xen_server[TestData.username], xen_server[TestData.password])
