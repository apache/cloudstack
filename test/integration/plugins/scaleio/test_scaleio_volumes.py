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
import time

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries
# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, DiskOffering, ServiceOffering, Snapshot, StoragePool, Template, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_hosts, list_virtual_machines, \
    list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

# Prerequisites:
#  Only one zone
#  Only one pod
#  Only one cluster
#
#  One ScaleIO storage pool
#  Only KVM hypervisor is supported for ScaleIO storage pool
#  KVM host(s) with ScaleIO Data Client (SDC) installed and connected to Metadata Manager (MDM)
#

class TestData():
    # constants
    account = "account"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    diskName = "diskname"
    diskOffering = "diskoffering"
    domainId = "domainId"
    hypervisor = "hypervisor"
    kvm = "kvm"
    login = "login"
    gatewayip = "gatewayip"
    one_GB_in_bytes = 1073741824
    password = "password"
    port = "port"
    primaryStorage = "primarystorage"
    provider = "provider"
    scope = "scope"
    powerFlex = "powerflex"
    storageTag = "pflex"
    tags = "tags"
    templateCacheNameKvm = "centos55-x86-64"
    testAccount = "testaccount"
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    virtualMachine2 = "virtualmachine2"
    volume_1 = "volume_1"
    volume_2 = "volume_2"
    kvm = "kvm"
    zoneId = "zoneId"

    # hypervisor type to test
    hypervisor_type = kvm

    def __init__(self):
        self.testdata = {
            TestData.kvm: {
                TestData.username: "root",
                TestData.password: "P@ssword123"
            },
            TestData.account: {
                "email": "test1@test1.com",
                "firstname": "John",
                "lastname": "Doe",
                "username": "test1",
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
                "email": "user@test1.com",
                "firstname": "Jane",
                "lastname": "Doe",
                "username": "test1user",
                "password": "password"
            },
            TestData.primaryStorage: {
                "name": "PowerFlexPool-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                "url": "powerflex://admin:P%40ssword123@10.10.2.130/cspool",
                TestData.provider: "PowerFlex",
                TestData.tags: TestData.storageTag,
                TestData.hypervisor: "KVM"
            },
            TestData.virtualMachine: {
                "name": "TestVM1",
                "displayname": "Test VM1"
            },
            TestData.virtualMachine2: {
                "name": "TestVM2",
                "displayname": "Test VM 2"
            },
            TestData.computeOffering: {
                "name": "PowerFlex_Compute",
                "displaytext": "PowerFlex_Compute",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                TestData.tags: TestData.storageTag
            },
            TestData.diskOffering: {
                "name": "PowerFlex_Disk",
                "displaytext": "PowerFlex_Disk",
                "disksize": 8,
                TestData.tags: TestData.storageTag,
                "storagetype": "shared"
            },
            TestData.volume_1: {
                TestData.diskName: "test-volume-1",
            },
            TestData.volume_2: {
                TestData.diskName: "test-volume-2",
            },
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "10.10.3.226"
        }


class TestScaleIOVolumes(cloudstackTestCase):
    _volume_vm_id_and_vm_id_do_not_match_err_msg = "The volume's VM ID and the VM's ID do not match."
    _vm_not_in_running_state_err_msg = "The VM is not in the 'Running' state."
    _vm_not_in_stopped_state_err_msg = "The VM is not in the 'Stopped' state."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestScaleIOVolumes, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()
        cls.testdata = TestData().testdata

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

        primarystorage = cls.testdata[TestData.primaryStorage]

        cls.primary_storage = StoragePool.create(
            cls.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=cls.zone.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
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

        TestScaleIOVolumes._start_vm(cls.virtual_machine)

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

        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.attached = False
        self.cleanup = []

    def tearDown(self):
        if self.attached:
            self.virtual_machine.detach_volume(self.apiClient, self.volume)

        cleanup_resources(self.apiClient, self.cleanup)

    def test_01_create_vm_with_volume(self):
        '''Create VM with attached volume and expunge VM'''

        #######################################
        # STEP 1: Create VM and attach volume #
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

        TestScaleIOVolumes._start_vm(test_virtual_machine)

        self.volume = test_virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vm = self._get_vm(test_virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestScaleIOVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

        #######################################
        #   STEP 2: Destroy and Expunge VM    #
        #######################################

        test_virtual_machine.delete(self.apiClient, True)

        self.attached = False

        vol = self._get_volume(self.volume.id)

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

    def test_02_attach_new_volume_to_stopped_vm(self):
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

        new_volume = self.virtual_machine.attach_volume(
            self.apiClient,
            new_volume
        )

        TestScaleIOVolumes._start_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            "running",
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

        # Detach volume
        new_volume = self.virtual_machine.detach_volume(
            self.apiClient,
            new_volume
        )

        self.assertEqual(
            new_volume.virtualmachineid,
            None,
            "The volume should not be attached to a VM."
        )

    def test_03_attach_detach_attach_volume_to_vm(self):
        '''Attach, detach, and attach volume to a running VM'''

        TestScaleIOVolumes._start_vm(self.virtual_machine)

        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestScaleIOVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

        #########################################
        # STEP 2: Detach volume from running VM #
        #########################################

        self.volume = self.virtual_machine.detach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = False

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            None,
            "The volume should not be attached to a VM."
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            str(vm.state)
        )

        #######################################
        # STEP 3: Attach volume to running VM #
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestScaleIOVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

    def test_04_detach_vol_stopped_vm_start(self):
        '''Detach volume from a stopped VM, then start.'''

        TestScaleIOVolumes._start_vm(self.virtual_machine)

        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestScaleIOVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

        #########################################
        # STEP 2: Detach volume from stopped VM #
        #########################################

        self.virtual_machine.stop(self.apiClient)

        self.volume = self.virtual_machine.detach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = False

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            None,
            "The volume should not be attached to a VM."
        )

        self.assertEqual(
            vm.state.lower(),
            'stopped',
            TestScaleIOVolumes._vm_not_in_stopped_state_err_msg
        )

        #######################################
        # STEP 3: Start VM with detached vol  #
        #######################################

        TestScaleIOVolumes._start_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

    def test_05_attach_volume_to_stopped_vm(self):
        '''Attach a volume to a stopped virtual machine, then start VM'''

        self.virtual_machine.stop(self.apiClient)

        #######################################
        # STEP 1: Attach volume to stopped VM #
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestScaleIOVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'stopped',
            TestScaleIOVolumes._vm_not_in_stopped_state_err_msg
        )

        TestScaleIOVolumes._start_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestScaleIOVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

    def test_06_attached_volume_reboot_vm(self):
        '''Attach volume to running VM, then reboot.'''

        TestScaleIOVolumes._start_vm(self.virtual_machine)

        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestScaleIOVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

        #######################################
        # STEP 2: Reboot VM with attached vol #
        #######################################
        TestScaleIOVolumes._reboot_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

    def test_07_detach_volume_reboot_vm(self):
        '''Detach volume from a running VM, then reboot.'''

        TestScaleIOVolumes._start_vm(self.virtual_machine)

        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################

        self.volume = self.virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestScaleIOVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

        #########################################
        # STEP 2: Detach volume from running VM #
        #########################################

        self.volume = self.virtual_machine.detach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = False

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            None,
            "The volume should not be attached to a VM."
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

        #######################################
        # STEP 3: Reboot VM with detached vol #
        #######################################

        self.virtual_machine.reboot(self.apiClient)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestScaleIOVolumes._vm_not_in_running_state_err_msg
        )

    def test_08_delete_volume_was_attached(self):
        '''Delete volume that was attached to a VM and is detached now'''

        TestScaleIOVolumes._start_vm(self.virtual_machine)

        #######################################
        # STEP 1: Create vol and attach to VM #
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

        new_volume = self.virtual_machine.attach_volume(
            self.apiClient,
            new_volume
        )

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            new_volume.virtualmachineid,
            vm.id,
            "Check if attached to virtual machine"
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            str(vm.state)
        )

        #######################################
        #  STEP 2: Detach and delete volume   #
        #######################################

        new_volume = self.virtual_machine.detach_volume(
            self.apiClient,
            new_volume
        )

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            new_volume.virtualmachineid,
            None,
            "Check if attached to virtual machine"
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            str(vm.state)
        )

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


    def _create_vm_using_template_and_destroy_vm(self, template):
        vm_name = "VM-%d" % random.randint(0, 100)

        virtual_machine_dict = {"name": vm_name, "displayname": vm_name}

        virtual_machine = VirtualMachine.create(
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
            virtualmachineid=virtual_machine.id,
            listall=True
        )

        vm_root_volume = list_volumes_response[0]
        vm_root_volume_name = vm_root_volume.name

        virtual_machine.delete(self.apiClient, True)

    def _get_bytes_from_gb(self, number_in_gb):
        return number_in_gb * 1024 * 1024 * 1024

    def _get_volume(self, volume_id):
        list_vols_response = list_volumes(self.apiClient, id=volume_id)
        return list_vols_response[0]

    def _get_vm(self, vm_id):
        list_vms_response = list_virtual_machines(self.apiClient, id=vm_id)
        return list_vms_response[0]

    def _get_template_cache_name(self):
        if TestData.hypervisor_type == TestData.kvm:
            return TestData.templateCacheNameKvm

        self.assert_(False, "Invalid hypervisor type")


    @classmethod
    def _start_vm(cls, vm):
        vm_for_check = list_virtual_machines(
            cls.apiClient,
            id=vm.id
        )[0]

        if vm_for_check.state == VirtualMachine.STOPPED:
            vm.start(cls.apiClient)

            # For KVM, just give it 90 seconds to boot up.
            if TestData.hypervisor_type == TestData.kvm:
                time.sleep(90)

    @classmethod
    def _reboot_vm(cls, vm):
        vm.reboot(cls.apiClient)

        # For KVM, just give it 90 seconds to boot up.
        if TestData.hypervisor_type == TestData.kvm:
            time.sleep(90)
