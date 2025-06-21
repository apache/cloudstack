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
import socket

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries
# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, DiskOffering, ServiceOffering, Snapshot, StoragePool, Template, User
from marvin.lib.base import VirtualMachine, Volume, VmSnapshot

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_hosts, list_virtual_machines, \
    list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources, validateList
from marvin.codes import PASS
from nose.plugins.attrib import attr

# Prerequisites:
#  Only one zone
#  Only one pod
#  Only one cluster
#
#  One linstor storage pool for basic tests
#  Only KVM hypervisor is supported for linstor storage pool
#  KVM host(s) have linstor-satellite installed
#
#  For volume migration tests, additional resource-groups are required
#  One Linstor resource group on the same Linstor storage cluster/instance (name: acs-test-same)
#  One Linstor resource group on different Linstor storage cluster/instance (name: acs-test-diff)
#


class TestData:
    # constants
    account = "account"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    diskName = "diskname"
    diskOffering = "diskoffering"
    diskOfferingSameInstance = "diskOfferingSameInstance"
    diskOfferingDistinctInstance = "diskOfferingDistinctInstance"
    domainId = "domainId"
    hypervisor = "hypervisor"
    kvm = "kvm"
    login = "login"
    gatewayip = "gatewayip"
    one_GB_in_bytes = 1073741824
    password = "password"
    port = "port"
    primaryStorage = "primarystorage"
    primaryStorageSameInstance = "primaryStorageSameInstance"
    primaryStorageDistinctInstance = "primaryStorageDistinctInstance"
    provider = "provider"
    scope = "scope"
    powerFlex = "powerflex"
    storageTag = "linstor"
    storageTagSameInstance = "linstorsame"
    storageTagDistinctInstance = "linstordiff"
    tags = "tags"
    templateCacheNameKvm = "centos55-x86-64"
    testAccount = "testaccount"
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    virtualMachine2 = "virtualmachine2"
    virtualMachine3 = "virtualmachine3"
    virtualMachine4 = "virtualmachine4"
    volume_1 = "volume_1"
    volume_2 = "volume_2"
    volume_3 = "volume_3"
    volume_4 = "volume_4"
    zoneId = "zoneId"
    migrationTests = "migrationTests"

    # hypervisor type to test
    hypervisor_type = kvm

    def __init__(self, linstor_controller_url):
        self.testdata = {
            TestData.kvm: {
                TestData.username: "admin",
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
                "name": "LinstorPool-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                "url": linstor_controller_url,
                TestData.provider: "Linstor",
                TestData.tags: TestData.storageTag + "," + TestData.storageTagSameInstance + "," + TestData.storageTagDistinctInstance,
                TestData.hypervisor: "KVM",
                "details": {
                    "resourceGroup": "acs-basic"
                }
            },
            TestData.virtualMachine: {
                "name": "TestVM1",
                "displayname": "Test VM 1"
            },
            TestData.virtualMachine2: {
                "name": "TestVM2",
                "displayname": "Test VM 2"
            },
            TestData.virtualMachine3: {
                "name": "TestVM3",
                "displayname": "Test VM 3"
            },
            TestData.virtualMachine4: {
                "name": "TestVM4",
                "displayname": "Test VM 4"
            },
            TestData.computeOffering: {
                "name": "Linstor_Compute",
                "displaytext": "Linstor_Compute",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                TestData.tags: TestData.storageTag
            },
            TestData.diskOffering: {
                "name": "Linstor_Disk",
                "displaytext": "Linstor_Disk",
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
            TestData.volume_3: {
                TestData.diskName: "test-volume-3",
            },
            TestData.volume_4: {
                TestData.diskName: "test-volume-4",
            },
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            # for volume migration tests
            TestData.migrationTests: True,
            # Linstor storage pool on the same Linstor storage instance
            TestData.primaryStorageSameInstance: {
                "name": "Linstor-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                "url": linstor_controller_url,
                TestData.provider: "Linstor",
                TestData.tags: TestData.storageTag + "," + TestData.storageTagSameInstance,
                TestData.hypervisor: "KVM",
                "details": {
                    "resourceGroup": "acs-test-same"
                }
            },
            # Linstor storage pool on different Linstor storage instance
            TestData.primaryStorageDistinctInstance: {
                "name": "Linstor-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                "url": linstor_controller_url,
                TestData.provider: "Linstor",
                TestData.tags: TestData.storageTag + "," + TestData.storageTagDistinctInstance,
                TestData.hypervisor: "KVM",
                "details": {
                    "resourceGroup": "acs-test-diff"
                }
            },
            TestData.diskOfferingSameInstance: {
                "name": "Linstor_Disk_Same_Inst",
                "displaytext": "Linstor_Disk_Same_Inst",
                "disksize": 8,
                TestData.tags: TestData.storageTagSameInstance,
                "storagetype": "shared"
            },
            TestData.diskOfferingDistinctInstance: {
                "name": "Linstor_Disk_Diff_Inst",
                "displaytext": "Linstor_Disk_Diff_Inst",
                "disksize": 8,
                TestData.tags: TestData.storageTagDistinctInstance,
                "storagetype": "shared"
            },
        }

class ServiceReady:
    @classmethod
    def ready(cls, hostname: str, port: int) -> bool:
        try:
            s = socket.create_connection((hostname, port), timeout=1)
            s.close()
            return True
        except (ConnectionRefusedError, socket.timeout, OSError):
            return False

    @classmethod
    def wait(
            cls,
            hostname,
            port,
            wait_interval = 5,
            timeout = 90,
            service_name = 'ssh') -> bool:
        """
        Wait until the controller can be reached.
        :param hostname:
        :param port: port of the application
        :param wait_interval:
        :param timeout: time to wait until exit with False
        :param service_name: name of the service to wait
        :return:
        """
        starttime = int(round(time.time() * 1000))
        while not cls.ready(hostname, port):
            if starttime + timeout * 1000 < int(round(time.time() * 1000)):
                raise RuntimeError("{s} {h} cannot be reached.".format(s=service_name, h=hostname))
            time.sleep(wait_interval)
        return True

    @classmethod
    def wait_ssh_ready(cls, hostname, wait_interval = 1, timeout = 90):
        return cls.wait(hostname, 22, wait_interval, timeout, "ssh")


class TestLinstorVolumes(cloudstackTestCase):
    _volume_vm_id_and_vm_id_do_not_match_err_msg = "The volume's VM ID and the VM's ID do not match."
    _vm_not_in_running_state_err_msg = "The VM is not in the 'Running' state."
    _vm_not_in_stopped_state_err_msg = "The VM is not in the 'Stopped' state."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestLinstorVolumes, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        # first host has the linstor controller
        first_host = list_hosts(cls.apiClient)[0]

        cls.testdata = TestData(first_host.ipaddress).testdata

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

        if cls.testdata[TestData.migrationTests]:
            primarystorage_sameinst = cls.testdata[TestData.primaryStorageSameInstance]
            cls.primary_storage_same_inst = StoragePool.create(
                cls.apiClient,
                primarystorage_sameinst,
                scope=primarystorage_sameinst[TestData.scope],
                zoneid=cls.zone.id,
                provider=primarystorage_sameinst[TestData.provider],
                tags=primarystorage_sameinst[TestData.tags],
                hypervisor=primarystorage_sameinst[TestData.hypervisor]
            )

            primarystorage_distinctinst = cls.testdata[TestData.primaryStorageDistinctInstance]
            cls.primary_storage_distinct_inst = StoragePool.create(
                cls.apiClient,
                primarystorage_distinctinst,
                scope=primarystorage_distinctinst[TestData.scope],
                zoneid=cls.zone.id,
                provider=primarystorage_distinctinst[TestData.provider],
                tags=primarystorage_distinctinst[TestData.tags],
                hypervisor=primarystorage_distinctinst[TestData.hypervisor]
            )

            cls.disk_offering_same_inst = DiskOffering.create(
                cls.apiClient,
                cls.testdata[TestData.diskOfferingSameInstance]
            )

            cls.disk_offering_distinct_inst = DiskOffering.create(
                cls.apiClient,
                cls.testdata[TestData.diskOfferingDistinctInstance]
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
            startvm=False,
            mode='basic',
        )

        TestLinstorVolumes._start_vm(cls.virtual_machine)

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
            if cls.testdata[TestData.migrationTests]:
                cls._cleanup.append(cls.disk_offering_same_inst)
                cls._cleanup.append(cls.disk_offering_distinct_inst)

            cleanup_resources(cls.apiClient, cls._cleanup)

            cls.primary_storage.delete(cls.apiClient)

            if cls.testdata[TestData.migrationTests]:
                cls.primary_storage_same_inst.delete(cls.apiClient)
                cls.primary_storage_distinct_inst.delete(cls.apiClient)

        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.attached = False
        self.cleanup = []

    def tearDown(self):
        if self.attached:
            self.virtual_machine.detach_volume(self.apiClient, self.volume)

        cleanup_resources(self.apiClient, self.cleanup)

    @attr(tags=['basic'], required_hardware=False)
    def test_01_create_vm_with_volume(self):
        """Create VM with attached volume and expunge VM"""

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
            startvm=False,
            mode='basic',
        )

        TestLinstorVolumes._start_vm(test_virtual_machine)

        self.volume = test_virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vm = self._get_vm(test_virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestLinstorVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
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

    @attr(tags=['basic'], required_hardware=False)
    def test_02_attach_new_volume_to_stopped_vm(self):
        """Attach a volume to a stopped virtual machine, then start VM"""

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

        TestLinstorVolumes._start_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            "running",
            TestLinstorVolumes._vm_not_in_running_state_err_msg
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

    @attr(tags=['basic'], required_hardware=False)
    def test_03_attach_detach_attach_volume_to_vm(self):
        """Attach, detach, and attach volume to a running VM"""

        TestLinstorVolumes._start_vm(self.virtual_machine)

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
            TestLinstorVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
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
            TestLinstorVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
        )

    @attr(tags=['basic'], required_hardware=False)
    def test_04_detach_vol_stopped_vm_start(self):
        """Detach volume from a stopped VM, then start."""

        TestLinstorVolumes._start_vm(self.virtual_machine)

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
            TestLinstorVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
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
            TestLinstorVolumes._vm_not_in_stopped_state_err_msg
        )

        #######################################
        # STEP 3: Start VM with detached vol  #
        #######################################

        TestLinstorVolumes._start_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
        )

    @attr(tags=['basic'], required_hardware=False)
    def test_05_attach_volume_to_stopped_vm(self):
        """Attach a volume to a stopped virtual machine, then start VM"""

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
            TestLinstorVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'stopped',
            TestLinstorVolumes._vm_not_in_stopped_state_err_msg
        )

        TestLinstorVolumes._start_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            self.volume.virtualmachineid,
            vm.id,
            TestLinstorVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
        )

    @attr(tags=['basic'], required_hardware=False)
    def test_06_attached_volume_reboot_vm(self):
        """Attach volume to running VM, then reboot."""

        TestLinstorVolumes._start_vm(self.virtual_machine)

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
            TestLinstorVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
        )

        #######################################
        # STEP 2: Reboot VM with attached vol #
        #######################################
        TestLinstorVolumes._reboot_vm(self.virtual_machine)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
        )

    @attr(tags=['basic'], required_hardware=False)
    def test_07_detach_volume_reboot_vm(self):
        """Detach volume from a running VM, then reboot."""

        TestLinstorVolumes._start_vm(self.virtual_machine)

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
            TestLinstorVolumes._volume_vm_id_and_vm_id_do_not_match_err_msg
        )

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
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
            TestLinstorVolumes._vm_not_in_running_state_err_msg
        )

        #######################################
        # STEP 3: Reboot VM with detached vol #
        #######################################

        self.virtual_machine.reboot(self.apiClient)

        vm = self._get_vm(self.virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            'running',
            TestLinstorVolumes._vm_not_in_running_state_err_msg
        )

    @attr(tags=['basic'], required_hardware=False)
    def test_08_delete_volume_was_attached(self):
        """Delete volume that was attached to a VM and is detached now"""

        TestLinstorVolumes._start_vm(self.virtual_machine)

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

    @attr(tags=['basic'], required_hardware=False)
    def test_09_create_snapshot(self):
        """Create snapshot of root disk"""
        self.virtual_machine.stop(self.apiClient)

        volume = list_volumes(
            self.apiClient,
            virtualmachineid = self.virtual_machine.id,
            type = "ROOT",
            listall = True,
        )
        snapshot = Snapshot.create(
            self.apiClient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.domain.id,
        )

        self.assertIsNotNone(snapshot, "Could not create snapshot")

        snapshot.delete(self.apiClient)


    @attr(tags=['advanced', 'migration'], required_hardware=False)
    def test_10_migrate_volume_to_same_instance_pool(self):
        """Migrate volume to the same instance pool"""

        if not self.testdata[TestData.migrationTests]:
            self.skipTest("Volume migration tests not enabled, skipping test")

        #######################################
        # STEP 1: Create VM and Start VM      #
        #######################################

        test_virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine3],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=False,
            mode='basic',
        )

        TestLinstorVolumes._start_vm(test_virtual_machine)

        #######################################
        # STEP 2: Create vol and attach to VM #
        #######################################

        new_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_3],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering_same_inst.id
        )

        volume_to_delete_later = new_volume

        new_volume = test_virtual_machine.attach_volume(
            self.apiClient,
            new_volume
        )

        vm = self._get_vm(test_virtual_machine.id)

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
        # STEP 3: Stop VM and Migrate volume  #
        #######################################

        test_virtual_machine.stop(self.apiClient)

        vm = self._get_vm(test_virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            'stopped',
            str(vm.state)
        )

        pools = StoragePool.listForMigration(
            self.apiClient,
            id=new_volume.id
        )

        if not pools:
            self.skipTest("No suitable storage pools found for volume migration, skipping test")

        self.assertEqual(
            validateList(pools)[0],
            PASS,
            "Invalid pool response from findStoragePoolsForMigration API"
        )

        pool = pools[0]
        self.debug("Migrating Volume-ID: {} to Same Instance Pool: {}".format(new_volume.id, pool.id))

        try:
            Volume.migrate(
                self.apiClient,
                volumeid=new_volume.id,
                storageid=pool.id
            )
        except Exception as e:
            self.fail("Volume migration failed with error %s" % e)

        #######################################
        #  STEP 4: Detach and delete volume   #
        #######################################

        new_volume = test_virtual_machine.detach_volume(
            self.apiClient,
            new_volume
        )

        self.assertEqual(
            new_volume.virtualmachineid,
            None,
            "Check if attached to virtual machine"
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

        #######################################
        #  STEP 4: Delete VM                  #
        #######################################

        test_virtual_machine.delete(self.apiClient, True)

    @attr(tags=['advanced', 'migration'], required_hardware=False)
    def test_11_migrate_volume_to_distinct_instance_pool(self):
        """Migrate volume to distinct instance pool"""

        if not self.testdata[TestData.migrationTests]:
            self.skipTest("Volume migration tests not enabled, skipping test")

        #######################################
        # STEP 1: Create VM and Start VM      #
        #######################################

        test_virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine4],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=False,
            mode='basic',
        )

        TestLinstorVolumes._start_vm(test_virtual_machine)

        #######################################
        # STEP 2: Create vol and attach to VM #
        #######################################

        new_volume = Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_4],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering_distinct_inst.id
        )

        volume_to_delete_later = new_volume

        new_volume = test_virtual_machine.attach_volume(
            self.apiClient,
            new_volume
        )

        vm = self._get_vm(test_virtual_machine.id)

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
        # STEP 3: Stop VM and Migrate volume  #
        #######################################

        test_virtual_machine.stop(self.apiClient)

        vm = self._get_vm(test_virtual_machine.id)

        self.assertEqual(
            vm.state.lower(),
            'stopped',
            str(vm.state)
        )

        pools = StoragePool.listForMigration(
            self.apiClient,
            id=new_volume.id
        )

        if not pools:
            self.skipTest("No suitable storage pools found for volume migration, skipping test")

        self.assertEqual(
            validateList(pools)[0],
            PASS,
            "Invalid pool response from findStoragePoolsForMigration API"
        )

        pool = pools[0]
        self.debug("Migrating Volume-ID: {} to Distinct Instance Pool: {}".format(new_volume.id, pool.id))

        try:
            Volume.migrate(
                self.apiClient,
                volumeid=new_volume.id,
                storageid=pool.id
            )
        except Exception as e:
            self.fail("Volume migration failed with error %s" % e)

        #######################################
        #  STEP 4: Detach and delete volume   #
        #######################################

        new_volume = test_virtual_machine.detach_volume(
            self.apiClient,
            new_volume
        )

        self.assertEqual(
            new_volume.virtualmachineid,
            None,
            "Check if attached to virtual machine"
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

        #######################################
        #  STEP 4: Delete VM                  #
        #######################################

        test_virtual_machine.delete(self.apiClient, True)

    @attr(tags=["basic"], required_hardware=False)
    def test_12_create_vm_snapshots(self):
        """Test to create VM snapshots
        """
        vm = TestLinstorVolumes._start_vm(self.virtual_machine)

        try:
            # Login to VM and write data to file system
            self.debug("virt: {}".format(vm))
            ssh_client = self.virtual_machine.get_ssh_client(vm.ipaddress, retries=5)
            ssh_client.execute("echo 'hello world' > testfile")
            ssh_client.execute("sync")
        except Exception as exc:
            self.fail("SSH failed for Virtual machine {}: {}".format(self.virtual_machine.ssh_ip, exc))

        time.sleep(10)
        memory_snapshot = False
        vm_snapshot = VmSnapshot.create(
            self.apiClient,
            self.virtual_machine.id,
            memory_snapshot,
            "VMSnapshot1",
            "test snapshot"
        )
        self.assertEqual(
            vm_snapshot.state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

    @attr(tags=["basic"], required_hardware=False)
    def test_13_revert_vm_snapshots(self):
        """Test to revert VM snapshots
        """

        result = None
        try:
            ssh_client = self.virtual_machine.get_ssh_client(reconnect=True)
            result = ssh_client.execute("rm -rf testfile")
        except Exception as exc:
            self.fail("SSH failed for Virtual machine %s: %s".format(self.virtual_machine.ipaddress, exc))

        if result is not None and "No such file or directory" in str(result):
            self.fail("testfile not deleted")

        time.sleep(5)

        list_snapshot_response = VmSnapshot.list(
            self.apiClient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )

        self.assertEqual(
            list_snapshot_response[0].state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

        self.virtual_machine.stop(self.apiClient, forced=True)

        VmSnapshot.revertToSnapshot(
            self.apiClient,
            list_snapshot_response[0].id
        )

        TestLinstorVolumes._start_vm(self.virtual_machine)

        try:
            ssh_client = self.virtual_machine.get_ssh_client(reconnect=True)

            result = ssh_client.execute("cat testfile")

        except Exception as exc:
            self.fail("SSH failed for Virtual machine {}: {}".format(self.virtual_machine.ipaddress, exc))

        self.assertEqual(
            "hello world",
            result[0],
            "Check the content is the same as originally written"
        )

    @attr(tags=["basic"], required_hardware=False)
    def test_14_delete_vm_snapshots(self):
        """Test to delete vm snapshots
        """

        list_snapshot_response = VmSnapshot.list(
            self.apiClient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )
        VmSnapshot.deleteVMSnapshot(
            self.apiClient,
            list_snapshot_response[0].id)

        time.sleep(5)

        list_snapshot_response = VmSnapshot.list(
            self.apiClient,
            virtualmachineid=self.virtual_machine.id,
            listall=False)
        self.debug('list_snapshot_response -------------------- {}'.format(list_snapshot_response))

        self.assertIsNone(list_snapshot_response, "snapshot is already deleted")

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

        virtual_machine.delete(self.apiClient, True)

    def _get_volume(self, volume_id):
        list_vols_response = list_volumes(self.apiClient, id=volume_id)
        return list_vols_response[0]

    @classmethod
    def _get_vm(cls, vm_id):
        list_vms_response = list_virtual_machines(cls.apiClient, id=vm_id)
        return list_vms_response[0]

    @classmethod
    def _start_vm(cls, vm):
        vm_for_check = cls._get_vm(vm.id)

        if vm_for_check.state == VirtualMachine.STOPPED:
            vm.start(cls.apiClient)

            vm_for_check = cls._get_vm(vm.id)
            ServiceReady.wait_ssh_ready(vm_for_check.ipaddress)
        return vm_for_check

    @classmethod
    def _reboot_vm(cls, vm):
        vm_for_check = cls._get_vm(vm.id)
        vm.reboot(cls.apiClient)

        time.sleep(5)

        ServiceReady.wait_ssh_ready(vm_for_check.ipaddress)
