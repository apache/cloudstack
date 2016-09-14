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
from marvin.lib.base import Account, DiskOffering, ServiceOffering, StoragePool, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_hosts, list_virtual_machines, \
    list_volumes, list_hosts

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

# Prerequisites:
#  Only one zone
#  Only one pod
#  Only one cluster
#
# Running the tests:
#  Change the "hypervisor_type" variable to control which hypervisor type to test.
#  If using XenServer, verify the "xen_server_hostname" variable is correct.
#  If using XenServer, change the "supports_cloning" variable to True or False as desired.


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
    templateCacheNameKvm = "centos55-x86-64"
    templateCacheNameXenServer = "centos56-x86-64-xen"
    testAccount = "testaccount"
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    virtualMachine2 = "virtualmachine2"
    volume_1 = "volume_1"
    volume_2 = "volume_2"
    xenServer = "xenserver"
    zoneId = "zoneId"

    # modify to control which hypervisor type to test
    hypervisor_type = xenServer
    xen_server_hostname = "XenServer-6.5-1"

    def __init__(self):
        self.testdata = {
            TestData.solidFire: {
                TestData.mvip: "192.168.139.112",
                TestData.username: "admin",
                TestData.password: "admin",
                TestData.port: 443,
                TestData.url: "https://192.168.139.112:443"
            },
            TestData.kvm: {
                TestData.username: "root",
                TestData.password: "solidfire"
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
            TestData.virtualMachine2: {
                "name": "TestVM2",
                "displayname": "Test VM 2"
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
                TestData.tags: TestData.storageTag
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
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "192.168.129.50"
        }


class TestVolumes(cloudstackTestCase):
    _should_only_be_one_vm_in_list_err_msg = "There should only be one VM in this list."
    _should_only_be_one_volume_in_list_err_msg = "There should only be one volume in this list."
    _should_only_be_one_host_in_list_err_msg = "There should only be one host in this list."
    _sf_account_id_should_be_non_zero_int_err_msg = "The SolidFire account ID should be a non-zero integer."
    _volume_size_should_be_non_zero_int_err_msg = "The SolidFire volume size should be a non-zero integer."
    _volume_vm_id_and_vm_id_do_not_match_err_msg = "The volume's VM ID and the VM's ID do not match."
    _vm_not_in_running_state_err_msg = "The VM is not in the 'Running' state."
    _vm_not_in_stopped_state_err_msg = "The VM is not in the 'Stopped' state."
    _volume_response_should_not_be_zero_err_msg = "The length of the response for the SolidFire-volume query should not be zero."
    _volume_should_not_be_in_a_vag = "The volume should not be in a volume access group."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestVolumes, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        cls._handle_supports_cloning()

        cls._connect_to_hypervisor()

        # Set up SolidFire connection
        solidfire = cls.testdata[TestData.solidFire]

        cls.sfe = ElementFactory.create(solidfire[TestData.mvip], solidfire[TestData.username], solidfire[TestData.password])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.cluster = list_clusters(cls.apiClient)[0]
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

        # Create VM and volume for tests
        cls.virtual_machine = VirtualMachine.create(
            cls.apiClient,
            cls.testdata[TestData.virtualMachine],
            accountid=cls.account.name,
            zoneid=cls.zone.id,
            serviceofferingid=cls.compute_offering.id,
            templateid=cls.template.id,
            domainid=cls.domain.id,
            startvm=False
        )

        TestVolumes._start_vm(cls.virtual_machine)

        cls.volume = Volume.create(
            cls.apiClient,
            cls.testdata[TestData.volume_1],
            account=cls.account.name,
            domainid=cls.domain.id,
            zoneid=cls.zone.id,
            diskofferingid=cls.disk_offering.id
        )

        # Resources that are to be destroyed
        cls._cleanup = [
            cls.volume,
            cls.virtual_machine,
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
        self.attached = False
        self.cleanup = []

    def tearDown(self):
        if self.attached:
            self.virtual_machine.detach_volume(self.apiClient, self.volume)

        cleanup_resources(self.apiClient, self.cleanup)

    def test_00_check_template_cache(self):
        if self._supports_cloning == False:
            return

        sf_volumes = self._get_active_sf_volumes()

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, self._get_template_cache_name(), self)

        self.assertEqual(
            len(sf_volume.volume_access_groups),
            0,
            "The volume should not be in a VAG."
        )

        sf_account_id = sf_volume.account_id

        sf_account = self.sfe.get_account_by_id(sf_account_id).account

        sf_account_name = sf_account.username

        self.assertEqual(
            sf_account_name.endswith("_1"),
            True,
            "The template cache volume's account does not end with '_1'."
        )

    def test_01_attach_new_volume_to_stopped_VM(self):
        '''Attach a volume to a stopped virtual machine, then start VM'''

        self.virtual_machine.stop(self.apiClient)

        new_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_2],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        self.cleanup.append(new_volume)

        self._check_and_get_cs_volume(new_volume.id, self.testdata[TestData.volume_2][TestData.diskName])

        new_volume = self.virtual_machine.attach_volume(
            self.apiClient,
            new_volume
        )

        newvolume = self._check_and_get_cs_volume(new_volume.id, self.testdata[TestData.volume_2][TestData.diskName])

        TestVolumes._start_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            newvolume.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            "running",
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, new_volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, new_volume, self)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, newvolume.name, self)

        sf_util.check_size_and_iops(sf_volume, newvolume, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        # Detach volume
        new_volume = self.virtual_machine.detach_volume(
            self.apiClient,
            new_volume
        )

    def test_02_attach_detach_attach_volume(self):
        '''Attach, detach, and attach volume to a running VM'''

        TestVolumes._start_vm(self.virtual_machine)

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        #######################################
        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        #########################################
        #########################################
        # STEP 2: Detach volume from running VM #
        #########################################
        #########################################

        self.volume = self.virtual_machine.detach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = False

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            None,
            "The volume should not be attached to a VM."
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            str(vm.state)
        )

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        self.assertEqual(
            len(sf_volume.volume_access_groups),
            0,
            "The volume should not be in a VAG."
        )

        self._check_host_side(sf_iscsi_name, vm.hostid, False)

        #######################################
        #######################################
        # STEP 3: Attach volume to running VM #
        #######################################
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

    def test_03_attached_volume_reboot_VM(self):
        '''Attach volume to running VM, then reboot.'''

        TestVolumes._start_vm(self.virtual_machine)

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        #######################################
        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        #######################################
        #######################################
        # STEP 2: Reboot VM with attached vol #
        #######################################
        #######################################
        TestVolumes._reboot_vm(self.virtual_machine)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

    def test_04_detach_volume_reboot(self):
        '''Detach volume from a running VM, then reboot.'''

        TestVolumes._start_vm(self.virtual_machine)

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        #######################################
        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        #########################################
        #########################################
        # STEP 2: Detach volume from running VM #
        #########################################
        #########################################

        self.volume = self.virtual_machine.detach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = False

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            None,
            "The volume should not be attached to a VM."
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        self.assertEqual(
            len(sf_volume.volume_access_groups),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_host_side(sf_iscsi_name, vm.hostid, False)

        #######################################
        #######################################
        # STEP 3: Reboot VM with detached vol #
        #######################################
        #######################################

        self.virtual_machine.reboot(self.apiClient)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        self.assertEqual(
            len(sf_volume.volume_access_groups),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_host_side(sf_iscsi_name, vm.hostid, False)

    def test_05_detach_vol_stopped_VM_start(self):
        '''Detach volume from a stopped VM, then start.'''

        TestVolumes._start_vm(self.virtual_machine)

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        #######################################
        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        hostid = vm.hostid

        #########################################
        #########################################
        # STEP 2: Detach volume from stopped VM #
        #########################################
        #########################################

        self.virtual_machine.stop(self.apiClient)

        self.volume = self.virtual_machine.detach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = False

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            None,
            "The volume should not be attached to a VM."
        )

        self.assertEqual(
            vm.state.lower(),
            'stopped',
            TestVolumes._vm_not_in_stopped_state_err_msg
        )

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        self.assertEqual(
            len(sf_volume.volume_access_groups),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_host_side(sf_iscsi_name, hostid, False)

        #######################################
        #######################################
        # STEP 3: Start VM with detached vol  #
        #######################################
        #######################################

        TestVolumes._start_vm(self.virtual_machine)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        self.assertEqual(
            len(sf_volume.volume_access_groups),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_host_side(sf_iscsi_name, vm.hostid, False)

    def test_06_attach_volume_to_stopped_VM(self):
        '''Attach a volume to a stopped virtual machine, then start VM'''

        self.virtual_machine.stop(self.apiClient)

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        #######################################
        #######################################
        # STEP 1: Attach volume to stopped VM #
        #######################################
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'stopped',
            TestVolumes._vm_not_in_stopped_state_err_msg
        )

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        if TestData.hypervisor_type == TestData.kvm:
            self._check_host_side(sf_iscsi_name, None, False)
        elif TestData.hypervisor_type == TestData.xenServer:
            self._check_host_side(sf_iscsi_name)

        TestVolumes._start_vm(self.virtual_machine)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

    def test_07_destroy_expunge_VM_with_volume(self):
        '''Destroy and expunge VM with attached volume'''

        #######################################
        #######################################
        # STEP 1: Create VM and attach volume #
        #######################################
        #######################################

        test_virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine2],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=False
        )

        TestVolumes._start_vm(test_virtual_machine)

        self.volume = test_virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(test_virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            TestVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestVolumes._vm_not_in_running_state_err_msg
        )

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        hostid = vm.hostid

        #######################################
        #######################################
        #   STEP 2: Destroy and Expunge VM    #
        #######################################
        #######################################

        test_virtual_machine.delete(self.apiClient, True)

        self.attached = False

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        self.assertEqual(
            vol.virtualmachineid,
            None,
            "Check if attached to virtual machine"
        )

        self.assertEqual(
            vol.vmname,
            None,
            "Check if VM was expunged"
        )

        list_virtual_machine_response = list_virtual_machines(
            self.apiClient,
            id=test_virtual_machine.id
        )

        self.assertEqual(
            list_virtual_machine_response,
            None,
            "Check if VM was actually expunged"
        )

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        self.assertEqual(
            len(sf_volume.volume_access_groups),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_host_side(sf_iscsi_name, hostid, False)

    def test_08_delete_volume_was_attached(self):
        '''Delete volume that was attached to a VM and is detached now'''

        TestVolumes._start_vm(self.virtual_machine)

        #######################################
        #######################################
        # STEP 1: Create vol and attach to VM #
        #######################################
        #######################################

        new_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_2],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        volume_to_delete_later = new_volume

        self._check_and_get_cs_volume(new_volume.id, self.testdata[TestData.volume_2][TestData.diskName])

        new_volume = self.virtual_machine.attach_volume(
            self.apiClient,
            new_volume
        )

        vol = self._check_and_get_cs_volume(new_volume.id, self.testdata[TestData.volume_2][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            "Check if attached to virtual machine"
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            str(vm.state)
        )

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, new_volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, new_volume, self)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        #######################################
        #######################################
        #  STEP 2: Detach and delete volume   #
        #######################################
        #######################################

        new_volume = self.virtual_machine.detach_volume(
            self.apiClient,
            new_volume
        )

        vol = self._check_and_get_cs_volume(new_volume.id, self.testdata[TestData.volume_2][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            None,
            "Check if attached to virtual machine"
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            str(vm.state)
        )

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        self.assertEqual(
            len(sf_volume.volume_access_groups),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_host_side(sf_iscsi_name, vm.hostid, False)

        volume_to_delete_later.delete(self.apiClient)

        list_volumes_response = list_volumes(
            self.apiClient,
            id=new_volume.id
        )

        self.assertEqual(
            list_volumes_response,
            None,
            "Check volume was deleted"
        )

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self, False)

    def test_09_attach_volumes_multiple_accounts(self):
        '''Attach a data disk to a VM in one account and attach another data disk to a VM in another account'''

        TestVolumes._start_vm(self.virtual_machine)

        #######################################
        #######################################
        # STEP 1: Create account, VM, and vol #
        #######################################
        #######################################

        test_account = Account.create(
            self.apiClient,
            self.testdata[TestData.testAccount],
            admin=1
        )

        self.cleanup.append(test_account)

        test_virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine2],
            accountid=test_account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=False
        )

        TestVolumes._start_vm(test_virtual_machine)

        test_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_2],
            zoneid=self.zone.id,
            account=test_account.name,
            domainid=self.domain.id,
            diskofferingid=self.disk_offering.id
        )

        self._check_and_get_cs_volume(test_volume.id, self.testdata[TestData.volume_2][TestData.diskName])

        #######################################
        #######################################
        #    STEP 2: Attach volumes to VMs    #
        #######################################
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vol.virtualmachineid,
            vm.id,
            "Check if attached to virtual machine"
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            str(vm.state)
        )

        test_volume = test_virtual_machine.attach_volume(
            self.apiClient,
            test_volume
        )

        test_vol = self._check_and_get_cs_volume(test_volume.id, self.testdata[TestData.volume_2][TestData.diskName])

        test_vm = self._get_vm(test_virtual_machine.id)

        self.assertEqual(
            test_vol.virtualmachineid,
            test_vm.id,
            "Check if attached to virtual machine of other acct"
        )

        self.assertEqual(
            test_vm.state.lower(),
            'running',
            str(test_vm.state)
        )

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, vol, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        sf_test_account_id = sf_util.get_sf_account_id(self.cs_api, test_account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_test_volumes = self._get_active_sf_volumes(sf_test_account_id)

        sf_test_volume = sf_util.check_and_get_sf_volume(sf_test_volumes, test_vol.name, self)

        sf_test_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, test_vol, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_test_volume_size)

        sf_util.check_size_and_iops(sf_test_volume, test_vol, sf_test_volume_size, self)

        sf_test_iscsi_name = sf_util.get_iqn(self.cs_api, test_volume, self)

        self._check_host_side(sf_test_iscsi_name, test_vm.hostid)

        sf_util.check_vag(sf_test_volume, sf_vag_id, self)

    def test_10_attach_more_than_one_disk_to_VM(self):
        '''Attach more than one disk to a VM'''

        TestVolumes._start_vm(self.virtual_machine)

        volume_2 = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_2],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.domain.id,
            diskofferingid=self.disk_offering.id
        )

        self.cleanup.append(volume_2)

        self._check_and_get_cs_volume(volume_2.id, self.testdata[TestData.volume_2][TestData.diskName])

        #######################################
        #######################################
        #    Step 1: Attach volumes to VM     #
        #######################################
        #######################################

        self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        self.virtual_machine.attach_volume(
            self.apiClient,
            volume_2
        )

        vol_2 = self._check_and_get_cs_volume(volume_2.id, self.testdata[TestData.volume_2][TestData.diskName])

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, TestVolumes._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, self.volume, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_size)

        sf_volume_2_size = sf_util.get_volume_size_with_hsr(self.cs_api, volume_2, self)

        self._verify_hsr(self.disk_offering.disksize, self.disk_offering.hypervisorsnapshotreserve, sf_volume_2_size)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        sf_volumes = self._get_active_sf_volumes(sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, vol.name, self)

        sf_util.check_size_and_iops(sf_volume, vol, sf_volume_size, self)

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, self.volume, self)

        vm = self._get_vm(self.virtual_machine.id)

        self._check_host_side(sf_iscsi_name, vm.hostid)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        sf_volume_2 = sf_util.check_and_get_sf_volume(sf_volumes, vol_2.name, self)

        sf_util.check_size_and_iops(sf_volume_2, vol_2, sf_volume_2_size, self)

        sf_iscsi_name_2 = sf_util.get_iqn(self.cs_api, volume_2, self)

        self._check_host_side(sf_iscsi_name_2, vm.hostid)

        sf_util.check_vag(sf_volume_2, sf_vag_id, self)

        self.virtual_machine.detach_volume(self.apiClient, volume_2)

    def _check_volume(self, volume, volume_name):
        self.assertTrue(
            volume.name.startswith(volume_name),
            "The volume name is incorrect."
        )

        self.assertEqual(
            volume.diskofferingid,
            self.disk_offering.id,
            "The disk offering is incorrect."
        )

        self.assertEqual(
            volume.zoneid,
            self.zone.id,
            "The zone is incorrect."
        )

        self.assertEqual(
            volume.storagetype,
            self.disk_offering.storagetype,
            "The storage type is incorrect."
        )

    def _check_and_get_cs_volume(self, volume_id, volume_name):
        list_volumes_response = list_volumes(
            self.apiClient,
            id=volume_id
        )

        sf_util.check_list(list_volumes_response, 1, self, TestVolumes._should_only_be_one_volume_in_list_err_msg)

        cs_volume = list_volumes_response[0]

        self._check_volume(cs_volume, volume_name)

        return cs_volume

    def _verify_hsr(self, cs_volume_size_in_gb, hsr, sf_volume_size_in_bytes):
        cs_volume_size_including_hsr_in_bytes = self._get_cs_volume_size_including_hsr_in_bytes(cs_volume_size_in_gb, hsr)

        self.assertTrue(
            cs_volume_size_including_hsr_in_bytes == sf_volume_size_in_bytes,
            "HSR does not add up correctly."
        );

    def _get_cs_volume_size_including_hsr_in_bytes(self, cs_volume_size_in_gb, hsr):
        if TestData.hypervisor_type == TestData.kvm:
            return self._get_bytes_from_gb(cs_volume_size_in_gb)

        lowest_hsr = 10

        if hsr < lowest_hsr:
            hsr = lowest_hsr;

        return self._get_bytes_from_gb(cs_volume_size_in_gb + (cs_volume_size_in_gb * (hsr / 100)))

    def _get_bytes_from_gb(self, number_in_gb):
        return number_in_gb * 1024 * 1024 * 1024

    def _get_vm(self, vm_id):
        list_vms_response = list_virtual_machines(self.apiClient, id=vm_id)

        sf_util.check_list(list_vms_response, 1, self, TestVolumes._should_only_be_one_vm_in_list_err_msg)

        return list_vms_response[0]

    def _check_xen_sr(self, xen_sr_name, should_exist=True):
        sf_util.check_xen_sr(xen_sr_name, self.xen_session, self, should_exist)

    def _get_active_sf_volumes(self, sf_account_id=None):
        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        self.assertNotEqual(
            len(sf_volumes),
            0,
            TestVolumes._volume_response_should_not_be_zero_err_msg
        )

        return sf_volumes

    def _get_template_cache_name(self):
        if TestData.hypervisor_type == TestData.kvm:
            return TestData.templateCacheNameKvm
        elif TestData.hypervisor_type == TestData.xenServer:
            return TestData.templateCacheNameXenServer

        self.assert_(False, "Invalid hypervisor type")

    def _get_modified_iscsi_name(self, sf_iscsi_name):
        sf_iscsi_name = sf_iscsi_name.replace("/", "")

        return sf_iscsi_name[:-1]

    def _check_host_side(self, sf_iscsi_name, vm_hostid=None, should_exist=True):
        if TestData.hypervisor_type == TestData.kvm:
            self._check_kvm_host_side(self._get_modified_iscsi_name(sf_iscsi_name), vm_hostid, should_exist)
        elif TestData.hypervisor_type == TestData.xenServer:
            self._check_xen_sr(sf_iscsi_name, should_exist)

    def _check_kvm_host_side(self, sf_iscsi_name, vm_hostid, should_exist=True):
        if vm_hostid is None:
            list_hosts_response = list_hosts(
                self.apiClient,
                type="Routing"
            )
        else:
            list_hosts_response = list_hosts(
                self.apiClient,
                id=vm_hostid
            )

            sf_util.check_list(list_hosts_response, 1, self, TestVolumes._should_only_be_one_host_in_list_err_msg)

        kvm_login = self.testdata[TestData.kvm]

        for cs_host in list_hosts_response:
            ssh_connection = sf_util.get_ssh_connection(cs_host.ipaddress, kvm_login[TestData.username], kvm_login[TestData.password])

            stdin, stdout, stderr = ssh_connection.exec_command("ls /dev/disk/by-path | grep " + sf_iscsi_name)

            result = stdout.read()

            ssh_connection.close()

            if should_exist:
                self.assertFalse(result is None, "Unable to locate 'by-path' field on the KVM host (None)")
                self.assertFalse(len(result.strip()) <= len(sf_iscsi_name), "Unable to locate the 'by-path' field on the KVM host (Zero-length string)")
            else:
                self.assertTrue(result is None or len(result.strip()) == 0, "Found the 'by-path' field on the KVM host, but did not expect to")

    @classmethod
    def _start_vm(cls, vm):
        vm.start(cls.apiClient)

        # Libvirt appears to have an issue detaching a volume from a VM while the VM is booting up.
        # The XML sent to update the VM seems correct, but it doesn't appear to update the XML that describes the VM.
        # For KVM, just give it 90 seconds to boot up.
        if TestData.hypervisor_type == TestData.kvm:
            time.sleep(90)

    @classmethod
    def _reboot_vm(cls, vm):
        vm.reboot(cls.apiClient)

        # Libvirt appears to have an issue detaching a volume from a VM while the VM is booting up.
        # The XML sent to update the VM seems correct, but it doesn't appear to update the XML that describes the VM.
        # For KVM, just give it 90 seconds to boot up.
        if TestData.hypervisor_type == TestData.kvm:
            time.sleep(90)

    @classmethod
    def _handle_supports_cloning(cls):
        if TestData.hypervisor_type == TestData.kvm:
            cls._supports_cloning = True
        elif TestData.hypervisor_type == TestData.xenServer:
            # For XenServer, it is OK to set this to True or False depending on what you'd like tested
            cls._supports_cloning = True

            sf_util.set_supports_resign(cls._supports_cloning, cls.dbConnection)

    @classmethod
    def _connect_to_hypervisor(cls):
        if TestData.hypervisor_type == TestData.kvm:
            pass
        elif TestData.hypervisor_type == TestData.xenServer:
            host_ip = "https://" + \
                  list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name=TestData.xen_server_hostname)[0].ipaddress

            cls.xen_session = XenAPI.Session(host_ip)

            xen_server = cls.testdata[TestData.xenServer]

            cls.xen_session.xenapi.login_with_password(xen_server[TestData.username], xen_server[TestData.password])

