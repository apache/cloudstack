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

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

from nose.plugins.attrib import attr

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, DiskOffering, ServiceOffering, StoragePool, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_hosts, list_virtual_machines, \
    list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

from solidfire import solidfire_element_api as sf_api

# on April 14, 2016: Ran 11 tests in 2494.043s with three hosts (resign = True)
# on April 14, 2016: Ran 11 tests in 2033.516s with three hosts (resign = False)

# on May 2, 2016: Ran 11 tests in 2352.461s with two hosts (resign = True)
# on May 2, 2016: Ran 11 tests in 1982.066s with two hosts (resign = False)


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
    templateCacheName = "centos56-x86-64-xen"
    templateName = "templatename"
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


class TestVolumes(cloudstackTestCase):
    _should_only_be_one_vm_in_list_err_msg = "There should only be one VM in this list."
    _should_only_be_one_volume_in_list_err_msg = "There should only be one volume in this list."
    _sf_account_id_should_be_non_zero_int_err_msg = "The SolidFire account ID should be a non-zero integer."
    _vag_id_should_be_non_zero_int_err_msg = "The SolidFire VAG ID should be a non-zero integer."
    _volume_size_should_be_non_zero_int_err_msg = "The SolidFire volume size should be a non-zero integer."
    _volume_vm_id_and_vm_id_do_not_match_err_msg = "The volume's VM ID and the VM's ID do not match."
    _vm_not_in_running_state_err_msg = "The VM is not in the 'Running' state."
    _vm_not_in_stopped_state_err_msg = "The VM is not in the 'Stopped' state."
    _sr_not_shared_err_msg = "The SR is not shared."
    _volume_response_should_not_be_zero_err_msg = "The length of the response for the SolidFire-volume query should not be zero."
    _list_should_be_empty = "The list should be empty."
    _volume_should_not_be_in_a_vag = "The volume should not be in a volume access group."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestVolumes, cls).getClsTestClient()
        cls.apiClient = testclient.getApiClient()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        cls.supports_resign = True

        cls._set_supports_resign()

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

        # Create VM and volume for tests
        cls.virtual_machine = VirtualMachine.create(
            cls.apiClient,
            cls.testdata[TestData.virtualMachine],
            accountid=cls.account.name,
            zoneid=cls.zone.id,
            serviceofferingid=cls.compute_offering.id,
            templateid=cls.template.id,
            domainid=cls.domain.id,
            startvm=True
        )

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

            cls._purge_solidfire_volumes()
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.attached = False
        self.cleanup = []

    def tearDown(self):
        if self.attached:
            self.virtual_machine.detach_volume(self.apiClient, self.volume)

        cleanup_resources(self.apiClient, self.cleanup)

    @attr(hypervisor='XenServer')
    def test_00_check_template_cache(self):
        if self.supports_resign == False:
            return

        sf_volumes = self._get_sf_volumes()

        sf_volume = self._check_and_get_sf_volume(sf_volumes, TestData.templateCacheName)

        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            0,
            "The volume should not be in a VAG."
        )

        sf_account_id = sf_volume["accountID"]

        sf_account = self.sf_client.get_account_by_id(sf_account_id)["account"]

        sf_account_name = sf_account["username"]

        self.assertEqual(
            sf_account_name.endswith("_1"),
            True,
            "The template cache volume's account does not end with '_1'."
        )

    @attr(hypervisor='XenServer')
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

        self.virtual_machine.start(self.apiClient)

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

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_volume_size = self._get_volume_size_with_hsr(new_volume)

        sf_vag_id = self._get_vag_id()

        sf_iscsi_name = self._get_iqn(new_volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, newvolume.name)

        self._check_size_and_iops(sf_volume, newvolume, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

        # Detach volume
        new_volume = self.virtual_machine.detach_volume(
            self.apiClient,
            new_volume
        )

    @attr(hypervisor='XenServer')
    def test_02_attach_detach_attach_volume(self):
        '''Attach, detach, and attach volume to a running VM'''

        self.virtual_machine.start(self.apiClient)

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_vag_id = self._get_vag_id()

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

        sf_iscsi_name = self._get_iqn(self.volume)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

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

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            0,
            "The volume should not be in a VAG."
        )

        self._check_xen_sr(sf_iscsi_name, False)

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

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

    @attr(hypervisor='XenServer')
    def test_03_attached_volume_reboot_VM(self):
        '''Attach volume to running VM, then reboot.'''

        self.virtual_machine.start(self.apiClient)

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_vag_id = self._get_vag_id()

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

        sf_iscsi_name = self._get_iqn(self.volume)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

        #######################################
        #######################################
        # STEP 2: Reboot VM with attached vol #
        #######################################
        #######################################
        self.virtual_machine.reboot(self.apiClient)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        sf_iscsi_name = self._get_iqn(self.volume)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

    @attr(hypervisor='XenServer')
    def test_04_detach_volume_reboot(self):
        '''Detach volume from a running VM, then reboot.'''

        self.virtual_machine.start(self.apiClient)

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_vag_id = self._get_vag_id()

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

        sf_iscsi_name = self._get_iqn(self.volume)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

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

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_xen_sr(sf_iscsi_name, False)

        #######################################
        #######################################
        # STEP 3: Reboot VM with detached vol #
        #######################################
        #######################################

        self.virtual_machine.reboot(self.apiClient)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_xen_sr(sf_iscsi_name, False)

    @attr(hypervisor='XenServer')
    def test_05_detach_vol_stopped_VM_start(self):
        '''Detach volume from a stopped VM, then start.'''

        self.virtual_machine.start(self.apiClient)

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_vag_id = self._get_vag_id()

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

        sf_iscsi_name = self._get_iqn(self.volume)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

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

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_xen_sr(sf_iscsi_name, False)

        #######################################
        #######################################
        # STEP 3: Start VM with detached vol  #
        #######################################
        #######################################

        self.virtual_machine.start(self.apiClient)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(self.virtual_machine.id)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_xen_sr(sf_iscsi_name, False)

    @attr(hypervisor='XenServer')
    def test_06_attach_volume_to_stopped_VM(self):
        '''Attach a volume to a stopped virtual machine, then start VM'''

        self.virtual_machine.stop(self.apiClient)

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_vag_id = self._get_vag_id()

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

        sf_iscsi_name = self._get_iqn(self.volume)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

        self.virtual_machine.start(self.apiClient)

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

        sf_iscsi_name = self._get_iqn(self.volume)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

    @attr(hypervisor='XenServer')
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
            startvm=True
        )

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

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_vag_id = self._get_vag_id()

        sf_iscsi_name = self._get_iqn(self.volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

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

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_xen_sr(sf_iscsi_name, False)

    @attr(hypervisor='XenServer')
    def test_08_delete_volume_was_attached(self):
        '''Delete volume that was attached to a VM and is detached now'''

        self.virtual_machine.start(self.apiClient)

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

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_volume_size = self._get_volume_size_with_hsr(new_volume)

        sf_vag_id = self._get_vag_id()

        sf_iscsi_name = self._get_iqn(new_volume)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self._check_vag(sf_volume, sf_vag_id)

        self._check_xen_sr(sf_iscsi_name)

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

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            0,
            TestVolumes._volume_should_not_be_in_a_vag
        )

        self._check_xen_sr(sf_iscsi_name, False)

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

        sf_volumes = self._get_sf_volumes(sf_account_id)

        self._check_and_get_sf_volume(sf_volumes, vol.name, False)

    @attr(hypervisor='XenServer')
    def test_09_attach_volumes_multiple_accounts(self):
        '''Attach a data disk to a VM in one account and attach another data disk to a VM in another account'''

        self.virtual_machine.start(self.apiClient)

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
            startvm=True
        )

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

        sf_vag_id = self._get_vag_id()

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        sf_volume_size = self._get_volume_size_with_hsr(vol)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        sf_iscsi_name = self._get_iqn(self.volume)

        self._check_xen_sr(sf_iscsi_name)

        self._check_vag(sf_volume, sf_vag_id)

        sf_test_account_id = self._get_sf_account_id(self.primary_storage.id, test_account.id)

        sf_test_volumes = self._get_sf_volumes(sf_test_account_id)

        sf_test_volume = self._check_and_get_sf_volume(sf_test_volumes, test_vol.name)

        sf_test_volume_size = self._get_volume_size_with_hsr(test_vol)

        self._check_size_and_iops(sf_test_volume, test_vol, sf_test_volume_size)

        sf_test_iscsi_name = self._get_iqn(test_volume)

        self._check_xen_sr(sf_test_iscsi_name)

        self._check_vag(sf_test_volume, sf_vag_id)

    @attr(hypervisor='XenServer')
    def test_10_attach_more_than_one_disk_to_VM(self):
        '''Attach more than one disk to a VM'''

        self.virtual_machine.start(self.apiClient)

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

        sf_account_id = self._get_sf_account_id(self.primary_storage.id, self.account.id)

        sf_volume_size = self._get_volume_size_with_hsr(self.volume)

        sf_volume_2_size = self._get_volume_size_with_hsr(volume_2)

        sf_vag_id = self._get_vag_id()

        sf_volumes = self._get_sf_volumes(sf_account_id)

        sf_volume = self._check_and_get_sf_volume(sf_volumes, vol.name)

        self._check_size_and_iops(sf_volume, vol, sf_volume_size)

        sf_iscsi_name = self._get_iqn(self.volume)

        self._check_xen_sr(sf_iscsi_name)

        self._check_vag(sf_volume, sf_vag_id)

        sf_volume_2 = self._check_and_get_sf_volume(sf_volumes, vol_2.name)

        self._check_size_and_iops(sf_volume_2, vol_2, sf_volume_2_size)

        sf_iscsi_name_2 = self._get_iqn(volume_2)

        self._check_xen_sr(sf_iscsi_name_2)

        self._check_vag(sf_volume_2, sf_vag_id)

        self.virtual_machine.detach_volume(self.apiClient, volume_2)

    '''
    @attr(hypervisor = 'XenServer')
    def _test_11_attach_disk_to_running_vm_change_iops(self):
        Attach a disk to a running VM, then change iops
        self.custom_iops_disk_offering = DiskOffering.create(
            
        )'''

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

    def _check_iscsi_name(self, sf_iscsi_name):
        self.assertEqual(
            sf_iscsi_name[0],
            "/",
            "The iSCSI name needs to start with a forward slash."
        )

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

    def _check_size_and_iops(self, sf_volume, volume, size):
        self.assertEqual(
            sf_volume['qos']['minIOPS'],
            volume.miniops,
            "Check QOS - Min IOPS: " + str(sf_volume['qos']['minIOPS'])
        )

        self.assertEqual(
            sf_volume['qos']['maxIOPS'],
            volume.maxiops,
            "Check QOS - Max IOPS: " + str(sf_volume['qos']['maxIOPS'])
        )

        self.assertEqual(
            sf_volume['totalSize'],
            size,
            "Check SF volume size: " + str(sf_volume['totalSize'])
        )

    def _check_vag(self, sf_volume, sf_vag_id):
        self.assertEqual(
            len(sf_volume['volumeAccessGroups']),
            1,
            "The volume should only be in one VAG."
        )

        self.assertEqual(
            sf_volume['volumeAccessGroups'][0],
            sf_vag_id,
            "The volume is not in the VAG with the following ID: " + str(sf_vag_id) + "."
        )

    def _check_and_get_cs_volume(self, volume_id, volume_name):
        list_volumes_response = list_volumes(
            self.apiClient,
            id=volume_id
        )

        self._check_list(list_volumes_response, 1, TestVolumes._should_only_be_one_volume_in_list_err_msg)

        cs_volume = list_volumes_response[0]

        self._check_volume(cs_volume, volume_name)

        return cs_volume

    def _get_sf_account_id(self, primary_storage_id, account_id):
        sf_account_id_request = {'storageid': primary_storage_id, 'accountid': account_id}
        sf_account_id_result = self.cs_api.getSolidFireAccountId(sf_account_id_request)
        sf_account_id = sf_account_id_result['apisolidfireaccountid']['solidFireAccountId']

        self.assertEqual(
            isinstance(sf_account_id, int),
            True,
            TestVolumes._sf_account_id_should_be_non_zero_int_err_msg
        )

        return sf_account_id

    def _get_volume_size_with_hsr(self, cs_volume):
        # Get underlying SF volume size with hypervisor snapshot reserve
        sf_volume_size_request = {'volumeid': cs_volume.id}
        sf_volume_size_result = self.cs_api.getSolidFireVolumeSize(sf_volume_size_request)
        sf_volume_size = sf_volume_size_result['apisolidfirevolumesize']['solidFireVolumeSize']

        self.assertEqual(
            isinstance(sf_volume_size, int),
            True,
            "The SolidFire volume size should be a non-zero integer."
        )

        return sf_volume_size

    def _get_vag_id(self):
        # Get SF Volume Access Group ID
        sf_vag_id_request = {'clusterid': self.cluster.id, 'storageid': self.primary_storage.id}
        sf_vag_id_result = self.cs_api.getSolidFireVolumeAccessGroupId(sf_vag_id_request)
        sf_vag_id = sf_vag_id_result['apisolidfirevolumeaccessgroupid']['solidFireVolumeAccessGroupId']

        self.assertEqual(
            isinstance(sf_vag_id, int),
            True,
            TestVolumes._vag_id_should_be_non_zero_int_err_msg
        )

        return sf_vag_id

    def _get_iqn(self, volume):
        # Get volume IQN
        sf_iscsi_name_request = {'volumeid': volume.id}
        sf_iscsi_name_result = self.cs_api.getVolumeiScsiName(sf_iscsi_name_request)
        sf_iscsi_name = sf_iscsi_name_result['apivolumeiscsiname']['volumeiScsiName']

        self._check_iscsi_name(sf_iscsi_name)

        return sf_iscsi_name

    def _get_vm(self, vm_id):
        list_vms_response = list_virtual_machines(self.apiClient, id=vm_id)

        self._check_list(list_vms_response, 1, TestVolumes._should_only_be_one_vm_in_list_err_msg)

        return list_vms_response[0]

    def _check_and_get_sf_volume(self, sf_volumes, sf_volume_name, should_exist=True):
        sf_volume = None

        for volume in sf_volumes:
            if volume['name'] == sf_volume_name:
                sf_volume = volume
                break

        if should_exist:
            self.assertNotEqual(
                sf_volume,
                None,
                "Check if SF volume was created in correct account: " + str(sf_volumes)
            )
        else:
            self.assertEqual(
                sf_volume,
                None,
                "Check if SF volume was deleted: " + str(sf_volumes)
            )

        return sf_volume

    def _check_xen_sr(self, xen_sr_name, should_exist=True):
        if should_exist:
            xen_sr = self.xen_session.xenapi.SR.get_by_name_label(xen_sr_name)[0]

            self.sr_shared = self.xen_session.xenapi.SR.get_shared(xen_sr)

            self.assertEqual(
                self.sr_shared,
                True,
                TestVolumes._sr_not_shared_err_msg
            )
        else:
            xen_sr = self.xen_session.xenapi.SR.get_by_name_label(xen_sr_name)

            self._check_list(xen_sr, 0, TestVolumes._list_should_be_empty)

    def _get_sf_volumes(self, sf_account_id=None):
        if sf_account_id is not None:
            sf_volumes = self.sf_client.list_volumes_for_account(sf_account_id)
        else:
            sf_volumes = self.sf_client.list_active_volumes()

        self.assertNotEqual(
            len(sf_volumes),
            0,
            TestVolumes._volume_response_should_not_be_zero_err_msg
        )

        return sf_volumes

    @classmethod
    def _set_supports_resign(cls):
        supports_resign = str(cls.supports_resign)

        sql_query = "Update host_details Set value = '" + supports_resign + "' Where name = 'supportsResign'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        cls.dbConnection.execute(sql_query)

    @classmethod
    def _purge_solidfire_volumes(cls):
        deleted_volumes = cls.sf_client.list_deleted_volumes()

        for deleted_volume in deleted_volumes:
            cls.sf_client.purge_deleted_volume(deleted_volume['volumeID'])

