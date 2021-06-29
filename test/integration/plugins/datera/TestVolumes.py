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
import unittest
import random
import os
import json
import time
import math
import XenAPI
import collections
import distutils.util

logger = logging.getLogger(__name__)
logger_handler = logging.FileHandler('/var/tmp/{}.log'.format(__name__))
logger_formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
logger_handler.setFormatter(logger_formatter)
logger.addHandler(logger_handler)
logger.setLevel(logging.INFO)

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

from nose.plugins.attrib import attr

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import (Account, DiskOffering, ServiceOffering,
                             StoragePool, User, VirtualMachine, Volume)

# common - commonly used methods for all tests are listed here
from marvin.lib.common import (get_domain, get_template, get_zone,
                               list_clusters, list_hosts,
                               list_virtual_machines,
                               list_volumes, list_disk_offering)

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

from marvin.cloudstackAPI import resizeVolume

#from dfs_sdk import DateraApi
from dfs_sdk import get_api


class TestData():
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    managedComputeOffering = "managedComputeoffering"
    nonManagedComputeOffering = "nonManagedComputeoffering"
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
    Datera = "datera"
    storageTag = "Datera_SAN_1"
    tags = "tags"
    templateCacheName = "centos56-x86-64-xen"  # TODO
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
            TestData.Datera: {
                TestData.mvip: "172.19.2.214",
                TestData.login: "admin",
                TestData.password: "password",
                TestData.port: 80,
                TestData.url: "https://172.19.2.214:443"
            },
            TestData.xenServer: {
                TestData.username: "root",
                TestData.password: "password"
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
                "name": "Datera-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                "url": "MVIP=172.19.2.214;SVIP=172.28.214.9;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=password;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "numReplicas=3;",
                TestData.provider: "Datera",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 4500000,
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "Any"
            },
            TestData.virtualMachine: {
                "name": "TestVM",
                "displayname": "TestVM",
                "privateport": 22,
                "publicport": 22,
                "protocol": "tcp"
            },
            TestData.virtualMachine2: {
                "name": "TestVM2",
                "displayname": "TestVM2",
                "privateport": 22,
                "publicport": 22,
                "protocol": "tcp"
            },
            TestData.managedComputeOffering: {
                "name": "DT_CO_1",
                "displaytext": "DT_CO_1 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": "10000",
                "maxiops": "15000",
                "hypervisorsnapshotreserve": 200,
                "tags": TestData.storageTag
            },
            TestData.nonManagedComputeOffering: {
                "name": "DT_CO_2",
                "displaytext": "DT_CO_2 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": "10000",
                "maxiops": "15000",
                "hypervisorsnapshotreserve": 200,
                "tags": TestData.storageTag
            },

            TestData.diskOffering: {
                "name": "DT_DO_1",
                "displaytext": "DT_DO_1 (5GB Min IOPS = 300; Max IOPS = 500)",
                "disksize": 5,
                "customizediops": False,
                "miniops": 300,
                "maxiops": 500,
                "hypervisorsnapshotreserve": 200,
                TestData.tags: TestData.storageTag,
                "storagetype": "shared"
            },
            "testdiskofferings": {
                "customiopsdo": {
                    "name": "DT_Custom_Iops_DO",
                    "displaytext": "Customized Iops DO",
                    "disksize": 5,
                    "customizediops": True,
                    "miniops": 500,
                    "maxiops": 1000,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "customsizedo": {
                    "name": "DT_Custom_Size_DO",
                    "displaytext": "Customized Size DO",
                    "disksize": 5,
                    "customizediops": False,
                    "miniops": 500,
                    "maxiops": 1000,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "customsizeandiopsdo": {
                    "name": "DT_Custom_Iops_Size_DO",
                    "displaytext": "Customized Size and Iops DO",
                    "disksize": 10,
                    "customizediops": True,
                    "miniops": 400,
                    "maxiops": 800,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "newiopsdo": {
                    "name": "DT_New_Iops_DO",
                    "displaytext": "New Iops (min=350, max = 700)",
                    "disksize": 5,
                    "miniops": 350,
                    "maxiops": 700,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "newsizedo": {
                    "name": "DT_New_Size_DO",
                    "displaytext": "New Size: 10",
                    "disksize": 10,
                    "customizediops": False,
                    "miniops": 400,
                    "maxiops": 800,
                    "hypervisorsnapshotreserve": 200,
                    TestData.tags: TestData.storageTag,
                    "storagetype": "shared"
                },
                "newsizeandiopsdo": {
                    "name": "DT_New_Size_Iops_DO",
                    "displaytext": "New Size and Iops",
                    "disksize": 10,
                    "customizediops": False,
                    "miniops": 200,
                    "maxiops": 800,
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
            TestData.templateName: "tiny linux kvm",  # TODO
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
        }

    def update(self, overrideFileName):
        if os.path.exists(overrideFileName):
            with open(overrideFileName) as fd:
                self.testdata = self._update(self.testdata, json.loads(fd.read()))

    def _update(self, d, u):

        for k, v in u.items():
            if isinstance(v, collections.Mapping):
                r = self.update(d.get(k, {}), v)
                d[k] = r
            else:
                d[k] = u[k]
        return d


class TestVolumes(cloudstackTestCase):
    _should_only_be_one_vm_in_list_err_msg = "There should only be one VM in this list."
    _should_only_be_one_volume_in_list_err_msg = "There should only be one volume in this list."
    _volume_vm_id_and_vm_id_do_not_match_err_msg = "The volume's VM ID and the VM's ID do not match."
    _vm_not_in_running_state_err_msg = "The VM is not in the 'Running' state."
    _vm_not_in_stopped_state_err_msg = "The VM is not in the 'Stopped' state."
    _sr_not_shared_err_msg = "The SR is not shared."
    _list_should_be_empty = "The list should be empty."
    _volume_resize_err = "The Volume was not resized correctly."

    @classmethod
    def setUpXenServer(cls):

        # Set up xenAPI connection
        hosts = list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId])
        xenserver_info = cls.testdata[TestData.xenServer]

        for h in hosts:
            host_ip = "https://" + h.ipaddress
            try:
                cls.xen_session = XenAPI.Session(host_ip)
                cls.xen_session.xenapi.login_with_password(xenserver_info[TestData.username],
                                                           xenserver_info[TestData.password])
                break
            except XenAPI.Failure as e:
                pass

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.managedComputeOffering]
        )

        cls.device_name = 'xvdb'

    @classmethod
    def setUpKVM(cls):
        logger.info("Setting up KVM")
        # KVM doesn't support root disks
        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.nonManagedComputeOffering]
        )

        cls.device_name = 'vdb'

    @classmethod
    def setUpClass(cls):
        """
        1. Init ACS API and DB connection
        2. Init Datera API connection
        3. Create ACS Primary storage
        4. Create ACS compute and disk offering.
        5. Create ACS data disk without attaching to a VM 
        """
        logger.info("Setting up Class")

        # Set up API client
        testclient = super(TestVolumes, cls).getClsTestClient()
        cls.apiClient = testclient.getApiClient()
        cls.dbConnection = testclient.getDbConnection()

        # Setup test data
        td = TestData()
        if cls.config.TestData and cls.config.TestData.Path:
            td.update(cls.config.TestData.Path)
        cls.testdata = td.testdata

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_name=cls.config.zones[0].name)
        cls.cluster = list_clusters(cls.apiClient)[0]
        cls.template = get_template(cls.apiClient, cls.zone.id)
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])

        # Set up datera connection
        datera = cls.testdata[TestData.Datera]
        cls.dt_client = get_api(
            username=datera[TestData.login],
            password=datera[TestData.password],
            hostname=datera[TestData.mvip],
            version="v2"
        )

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
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        cls.disk_offering = DiskOffering.create(
            cls.apiClient,
            cls.testdata[TestData.diskOffering]
        )

        cls.disk_offering_new = DiskOffering.create(
            cls.apiClient,
            cls.testdata['testdiskofferings']['newsizeandiopsdo']
        )

        cls.supports_resign = cls._get_supports_resign()

        # Set up hypervisor specific connections
        if cls.cluster.hypervisortype.lower() == 'xenserver':
            cls.setUpXenServer()
        if cls.cluster.hypervisortype.lower() == 'kvm':
            cls.setUpKVM()

        # Create 1 data volume_1
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
            cls.compute_offering,
            cls.disk_offering,
            cls.disk_offering_new,
            cls.user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        logger.info("Tearing Down Class")
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)

            cls.primary_storage.delete(cls.apiClient)

            cls._purge_datera_volumes()

        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        logger.info("Setup test")
        self.attached = False
        self.cleanup = []

    def tearDown(self):
        logger.info("Tearing Down test")
        cleanup_resources(self.apiClient, self.cleanup)

    @classmethod
    def _set_supports_resign(cls, val):

        supports_resign = str(val).lower()
        cls.supports_resign = val

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench

        sql_query = "Update host_details Set value = '" + supports_resign + "' Where name = 'supportsResign'"
        cls.dbConnection.execute(sql_query)

        sql_query = "Update cluster_details Set value = '" + supports_resign + "' Where name = 'supportsResign'"
        cls.dbConnection.execute(sql_query)

    @classmethod
    def _get_supports_resign(cls):

        sql_query = "SELECT value from cluster_details Where name='supportsResign' AND cluster_id=%d" % cls.testdata[
            TestData.clusterId]

        sql_result = cls.dbConnection.execute(sql_query)
        logger.warn(sql_result)

        if len(sql_result) < 1:
            return False

        return bool(distutils.util.strtobool(sql_result[0][0].lower()))

    def _get_cs_storage_pool_db_id(self, storage_pool):
        return self._get_db_id("storage_pool", storage_pool)

    def _get_db_id(self, table, db_obj):
        sql_query = "Select id From " + table + " Where uuid = '" + str(db_obj.id) + "'"
        sql_result = self.dbConnection.execute(sql_query)
        return sql_result[0][0]

    @classmethod
    def _purge_datera_volumes(cls):
        logger.warn("Deleting all volumes")
        for ai in list(cls.dt_client.app_instances.get().values()):
            logger.warn(ai)
            if 'CS-T' in ai['name']:
                ai.set(admin_state="offline")
                ai.delete()

    def test_01_attach_new_volume_to_stopped_VM(self):

        '''Attach a volume to a stopped virtual machine, then start VM'''
        # Create VM and volume for tests
        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True,
            mode='advanced'
        )
        self.cleanup.append(virtual_machine)

        template_volume_name = \
            self._get_app_instance_name_from_cs_volume(self.template,
                                                       vol_type='TEMPLATE')
        dt_volume = self._check_and_get_dt_volume(template_volume_name)

        virtual_machine.stop(self.apiClient, forced=True)

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

        new_volume = virtual_machine.attach_volume(
            self.apiClient,
            new_volume
        )

        newvolume = self._check_and_get_cs_volume(new_volume.id, self.testdata[TestData.volume_2][TestData.diskName])

        virtual_machine.start(self.apiClient)

        vm = self._get_vm(virtual_machine.id)

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

        dt_volume_size = self._get_volume_size_with_hsr(newvolume)

        iqn = self._get_iqn(newvolume)

        dt_new_volname = self._get_app_instance_name_from_cs_volume(newvolume)

        dt_volume = self._check_and_get_dt_volume(dt_new_volname)

        self._check_size_and_iops(dt_volume, newvolume, dt_volume_size)

        initiator_group_name = self._get_initiator_group_name()

        self._check_initiator_group(dt_volume, initiator_group_name)

        self._check_hypervisor(iqn)
        logger.info("Detach volume from the VM")
        virtual_machine.detach_volume(
            self.apiClient,
            new_volume
        )

    def test_02_attach_detach_attach_volume(self):
        '''Attach, detach, and attach volume to a running VM'''

        # Create VM and volume for tests
        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True,
            mode='advanced'
        )
        self.cleanup.append(virtual_machine)

        self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        #######################################
        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################
        #######################################
        self.volume = virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(virtual_machine.id)

        initiator_group_name = self._get_initiator_group_name()

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

        iqn = self._get_iqn(self.volume)

        dt_volume_size = self._get_volume_size_with_hsr(self.volume)

        dt_volume_name = self._get_app_instance_name_from_cs_volume(self.volume)

        dt_volume = self._check_and_get_dt_volume(dt_volume_name)

        self._check_initiator_group(dt_volume, initiator_group_name)

        self._check_size_and_iops(dt_volume, vol, dt_volume_size)

        self._check_hypervisor(iqn)

        #########################################
        #########################################
        # STEP 2: Detach volume from running VM #
        #########################################
        #########################################

        self.volume = virtual_machine.detach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = False

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(virtual_machine.id)

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

        dt_volume = self._check_and_get_dt_volume(dt_volume_name)

        self._check_initiator_group(dt_volume, initiator_group_name, False)

        self._check_hypervisor(iqn, False)

        #######################################
        #######################################
        # STEP 3: Attach volume to running VM #
        #######################################
        #######################################

        time.sleep(30)
        self.volume = virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(virtual_machine.id)

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

        dt_volume = self._check_and_get_dt_volume(dt_volume_name)

        self._check_initiator_group(dt_volume, initiator_group_name)

        self._check_hypervisor(iqn)

    def test_03_attached_volume_reboot_VM(self):
        '''Attach volume to running VM, then reboot.'''
        # Create VM and volume for tests
        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True,
            mode='advanced'
        )
        self.cleanup.append(virtual_machine)

        self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        #######################################
        #######################################
        # STEP 1: Attach volume to running VM #
        #######################################
        #######################################

        self.volume = virtual_machine.attach_volume(
            self.apiClient,
            self.volume
        )

        self.attached = True

        dt_volume_name = self._get_app_instance_name_from_cs_volume(self.volume)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(virtual_machine.id)

        initiator_group_name = self._get_initiator_group_name()

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

        iqn = self._get_iqn(self.volume)

        volume_size_gb = self._get_volume_size_with_hsr(self.volume)

        dt_volume = self._check_and_get_dt_volume(dt_volume_name)

        self._check_size_and_iops(dt_volume, vol, volume_size_gb)

        self._check_initiator_group(dt_volume, initiator_group_name)

        self._check_hypervisor(iqn)

        #######################################
        #######################################
        # STEP 2: Reboot VM with attached vol #
        #######################################
        #######################################
        virtual_machine.reboot(self.apiClient)

        vol = self._check_and_get_cs_volume(self.volume.id, self.testdata[TestData.volume_1][TestData.diskName])

        vm = self._get_vm(virtual_machine.id)

        iqn = self._get_iqn(self.volume)

        dt_volume_size = self._get_volume_size_with_hsr(self.volume)

        dt_volume = self._check_and_get_dt_volume(dt_volume_name)

        self._check_size_and_iops(dt_volume, vol, dt_volume_size)

        self._check_initiator_group(dt_volume, initiator_group_name)

        self._check_hypervisor(iqn)

    def _check_if_device_visible_in_vm(self, vm, dev_name):

        try:
            ssh_client = vm.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (vm.ipaddress, e))

        cmd = "iostat | grep %s" % dev_name
        res = ssh_client.execute(cmd)
        logger.warn(cmd)
        logger.warn(res)

        if not res:
            self.fail("Device %s not found on VM: %s" % (dev_name, vm.ipaddress))

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

    def _check_initiator_group(self, dt_volume, initiator_group_name, should_exist=True):

        volume_initiator_groups = dt_volume['storage_instances']['storage-1']['acl_policy']['initiator_groups']

        if should_exist:
            self.assertTrue(
                initiator_group_name in volume_initiator_groups[0],
                "Initiator group not assigned to volume"
            )

        else:

            self.assertTrue(
                len(volume_initiator_groups) == 0,
                "Initiator group still asigined to volume, should have been removed"
            )

    def _check_volume(self, volume, volume_name, disk_offering):
        self.assertTrue(
            volume.name.startswith(volume_name),
            "The volume name is incorrect."
        )

        self.assertEqual(
            volume.diskofferingid,
            disk_offering.id,
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

    def _check_size_and_iops(self, dt_volume, cs_volume, size):

        dt_max_total_iops = dt_volume['storage_instances']['storage-1']['volumes']['volume-1']['performance_policy'][
            'total_iops_max']
        self.assertEqual(
            dt_max_total_iops,
            cs_volume.maxiops,
            "Check QOS - Max IOPS: " + str(dt_max_total_iops)
        )

        dt_volume_size = dt_volume['storage_instances']['storage-1']['volumes']['volume-1']['size']
        self.assertEqual(
            dt_volume_size,
            size,
            "Check volume size: " + str(dt_volume_size)
        )

    def _check_and_get_cs_volume(self, volume_id, volume_name, disk_offering=None):

        if not disk_offering:
            disk_offering = self.disk_offering

        list_volumes_response = list_volumes(
            self.apiClient,
            id=volume_id
        )

        self._check_list(list_volumes_response, 1, TestVolumes._should_only_be_one_volume_in_list_err_msg)

        cs_volume = list_volumes_response[0]

        self._check_volume(cs_volume, volume_name, disk_offering)

        return cs_volume

    def _get_app_instance_name_from_cs_volume(self, cs_volume, vol_type='VOLUME'):
        """
        Get Datera app_instance name based on ACS data object types
        Eg. CS-V-test-volume-7XWJ5Q-dfc41254-371a-40b3-b410-129eb79893c0
        """
        app_inst_prefix = 'CS'

        if vol_type == 'VOLUME':
            vol_type_char = 'V'
            uuid = cs_volume.id
            name = cs_volume.name
            app_instance_name = app_inst_prefix + '-' + vol_type_char + '-' + name + '-' + uuid

        if vol_type == 'TEMPLATE':
            vol_type_char = 'T'
            uuid = cs_volume.id
            primary_storage_db_id = str(self._get_cs_storage_pool_db_id(self.primary_storage))
            app_instance_name = app_inst_prefix + '-' + vol_type_char + '-' + uuid + '-' + primary_storage_db_id

        return app_instance_name

    def _get_iqn(self, cs_volume):
        """
        Get IQN for the CS volume from Datera
        """
        app_instance_name = self._get_app_instance_name_from_cs_volume(cs_volume)
        app_instance = self.dt_client.app_instances.get(app_instance_name)
        return app_instance['storage_instances']['storage-1']['access']['iqn']

    def _get_cs_volume_size_with_hsr(self, cs_volume):

        disk_size_bytes = cs_volume.size

        disk_offering_id = cs_volume.diskofferingid

        disk_offering = list_disk_offering(self.apiClient, id=disk_offering_id)[0]

        hsr = disk_offering.hypervisorsnapshotreserve

        disk_size_with_hsr_bytes = disk_size_bytes + (disk_size_bytes * hsr) / 100

        disk_size_with_hsr_gb = int(math.ceil(disk_size_with_hsr_bytes / (1024 ** 3)))

        return disk_size_with_hsr_gb

    def _get_volume_size_with_hsr(self, cs_volume):

        app_instance_name = self._get_app_instance_name_from_cs_volume(cs_volume)
        app_instance = self.dt_client.app_instances.get(app_instance_name)

        volume_size_gb = app_instance['storage_instances']['storage-1']['volumes']['volume-1']['size']

        self.assertEqual(
            isinstance(volume_size_gb, int),
            True,
            "The volume size should be a non-zero integer."
        )

        return volume_size_gb

    def _get_initiator_group_name(self):
        init_group_prefix = 'CS-InitiatorGroup'
        initiator_group_name = init_group_prefix + '-' + self.cluster.id
        self.dt_client.initiator_groups.get(initiator_group_name)
        return initiator_group_name

    def _get_dt_volumes(self):
        return self.dt_client.app_instances.get()

    def _get_vm(self, vm_id):
        list_vms_response = list_virtual_machines(self.apiClient, id=vm_id)

        self._check_list(list_vms_response, 1, TestVolumes._should_only_be_one_vm_in_list_err_msg)

        return list_vms_response[0]

    def _check_and_get_dt_volume(self, dt_volume_name, should_exist=True):
        dt_volume = None
        dt_volumes = self._get_dt_volumes()

        for volume in list(dt_volumes.values()):
            if volume['name'] == dt_volume_name:
                dt_volume = volume
                break

        if should_exist:
            self.assertNotEqual(
                dt_volume,
                None,
                "Check if Datera volume was created: " + str(dt_volumes)
            )
        else:
            self.assertEqual(
                dt_volume,
                None,
                "Check if volume was deleted: " + str(dt_volumes)
            )

        return dt_volume

    def _resize_volume(self, volume, new_disk_offering):

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = self.volume.id
        cmd.diskofferingid = new_disk_offering.id

        self.apiClient.resizeVolume(cmd)

        do_size_bytes = int(new_disk_offering.disksize * (1024 ** 3))
        retries = 3
        success = False

        while retries > 0:
            retries -= 1

            list_volumes_response = list_volumes(
                self.apiClient,
                id=volume.id
            )

            for vol in list_volumes_response:
                if vol.id == volume.id and \
                                int(vol.size) == do_size_bytes and \
                                vol.state == 'Ready':
                    success = True

            if success:
                break
            else:
                time.sleep(10)

        self.assertEqual(success, True, self._volume_resize_err)

    def _check_hypervisor(self, iqn, should_exist=True):
        if self.cluster.hypervisortype.lower() == 'xenserver':
            self._check_xen_sr(iqn, should_exist)
        else:
            return

    def _check_xen_sr(self, iqn, should_exist=True):

        xen_sr_name = "/" + iqn + "/0"
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

    def _check_if_device_removed_in_vm(self, vm, dev_name):

        try:
            ssh_client = vm.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (vm.ipaddress, e))

        cmd = "iostat | grep %s" % dev_name
        res = ssh_client.execute(cmd)
        logger.warn(cmd)
        logger.warn(res)

        if res:
            self.fail("Device %s still attached on VM: %s" % (dev_name, vm.ipaddress))

    def _start_device_io(self, vm, dev_name):

        try:
            ssh_client = vm.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (vm.ipaddress, e))

        cmd = "dd if=/dev/urandom of=/dev/%s &" % dev_name
        res = ssh_client.execute(cmd)
        logger.warn(cmd)
        logger.warn(res)

    def _stop_device_io(self, vm, dev_name):

        try:
            ssh_client = vm.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (vm.ipaddress, e))

        cmd = "killall -9 dd"
        res = ssh_client.execute(cmd)
        logger.warn(cmd)
        logger.warn(res)

    def _get_bytes_written(self, vm, dev_name):

        try:
            ssh_client = vm.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (vm.ipaddress, e))

        cmd = "iostat | grep %s " % dev_name
        res = ssh_client.execute(cmd)
        logger.warn(cmd)
        logger.warn(res)

        self.assertNotEqual(res, None, "Error getting iostat info")

        ret_data = ' '.join(map(str, res)).strip()
        return int(ret_data.split()[-1])
