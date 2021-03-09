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

from marvin.cloudstackAPI import destroySystemVm

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, Router, ServiceOffering, StoragePool, User, VirtualMachine, Zone

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_hosts, list_ssvms, list_routers

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources, wait_until

# Prerequisites:
#  * Only use one SolidFire cluster for the two primary storages based on the "SolidFire" storage plug-in.
#  * Do not run other workloads on the SolidFire cluster while running this test as this test checks at a certain
#     point to make sure no active SolidFire volumes exist.
#  * Only one zone
#  * Only one secondary storage VM and one console proxy VM running on NFS (no virtual router or user VMs exist)
#  * Only one pod
#  * Only one cluster
#
# Running the tests:
#  Change the "hypervisor_type" variable to control which hypervisor type to test.
#  If using XenServer, verify the "xen_server_hostname" variable is correct.
#  Set the Global Setting "storage.cleanup.enabled" to true.
#  Set the Global Setting "storage.cleanup.interval" to 150.
#  Set the Global Setting "storage.cleanup.delay" to 60.


class TestData():
    # constants
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterid"
    computeOffering = "computeoffering"
    diskOffering = "diskoffering"
    domainId = "domainid"
    email = "email"
    firstname = "firstname"
    hypervisor = "hypervisor"
    kvm = "kvm"
    lastname = "lastname"
    max_iops = "maxiops"
    min_iops = "miniops"
    mvip = "mvip"
    name = "name"
    password = "password"
    port = "port"
    primaryStorage = "primarystorage"
    provider = "provider"
    scope = "scope"
    solidFire = "solidfire"
    storageTag = "SolidFire_SAN_1"
    systemOffering = "systemoffering"
    systemOfferingFailure = "systemofferingFailure"
    tags = "tags"
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    xenServer = "xenserver"
    zoneId = "zoneid"

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
            TestData.kvm: {
                TestData.username: "root",
                TestData.password: "solidfire"
            },
            TestData.xenServer: {
                TestData.username: "root",
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
                TestData.name: TestData.get_name_for_solidfire_storage(),
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
            TestData.virtualMachine: {
                TestData.name: "TestVM",
                "displayname": "Test VM"
            },
            TestData.computeOffering: {
                TestData.name: "SF_CO_1",
                "displaytext": "SF_CO_1 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                TestData.min_iops: 10000,
                TestData.max_iops: 15000,
                "hypervisorsnapshotreserve": 200,
                TestData.tags: TestData.storageTag
            },
            TestData.systemOffering: {
                TestData.name: "SF_SO_1",
                "displaytext": "Managed SO (Min IOPS = 4,000; Max IOPS = 8,000)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                TestData.min_iops: 4000,
                TestData.max_iops: 8000,
                TestData.tags: TestData.storageTag,
                "issystem": True
            },
            TestData.systemOfferingFailure: {
                TestData.name: "SF_SO_2",
                "displaytext": "Managed SO (Customized IOPS)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": True,
                TestData.tags: TestData.storageTag,
                "issystem": True
            },
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }

    @staticmethod
    def get_name_for_solidfire_storage():
        return "SolidFire-%d" % random.randint(0, 100)


class TestManagedSystemVMs(cloudstackTestCase):
    _unique_name_suffix = "-Temp"

    _secondary_storage_unique_name = "Cloud.com-SecondaryStorage"
    _secondary_storage_temp_unique_name = _secondary_storage_unique_name + _unique_name_suffix

    _console_proxy_unique_name = "Cloud.com-ConsoleProxy"
    _console_proxy_temp_unique_name = _console_proxy_unique_name + _unique_name_suffix

    _virtual_router_unique_name = "Cloud.com-SoftwareRouter"
    _virtual_router_temp_unique_name = _virtual_router_unique_name + _unique_name_suffix

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestManagedSystemVMs, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        cls._connect_to_hypervisor()

        # Set up SolidFire connection
        solidfire = cls.testdata[TestData.solidFire]

        cls.sfe = ElementFactory.create(solidfire[TestData.mvip], solidfire[TestData.username], solidfire[TestData.password])

        # Get Resources from Cloud Infrastructure
        cls.zone = Zone(get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId]).__dict__)
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

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        systemoffering = cls.testdata[TestData.systemOffering]

        systemoffering[TestData.name] = "Managed SSVM"
        systemoffering['systemvmtype'] = "secondarystoragevm"

        cls.secondary_storage_offering = ServiceOffering.create(
            cls.apiClient,
            systemoffering
        )

        systemoffering[TestData.name] = "Managed CPVM"
        systemoffering['systemvmtype'] = "consoleproxy"

        cls.console_proxy_offering = ServiceOffering.create(
            cls.apiClient,
            systemoffering
        )

        systemoffering[TestData.name] = "Managed VR"
        systemoffering['systemvmtype'] = "domainrouter"

        cls.virtual_router_offering = ServiceOffering.create(
            cls.apiClient,
            systemoffering
        )

        # Resources that are to be destroyed
        cls._cleanup = [
            cls.secondary_storage_offering,
            cls.console_proxy_offering,
            cls.virtual_router_offering,
            cls.compute_offering,
            cls.user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)
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

    def test_01_create_system_vms_on_managed_storage(self):
        self._disable_zone_and_delete_system_vms(None, False)

        primary_storage = self.testdata[TestData.primaryStorage]

        primary_storage_1 = StoragePool.create(
            self.apiClient,
            primary_storage
        )

        self._prepare_to_use_managed_storage_for_system_vms()

        enabled = "Enabled"

        self.zone.update(self.apiClient, id=self.zone.id, allocationstate=enabled)

        system_vms = self._wait_for_and_get_running_system_vms(2)

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

        # This virtual machine was only created and started so that the virtual router would be created and started.
        # Just delete this virtual machine once it has been created and started.
        virtual_machine.delete(self.apiClient, True)

        virtual_router = list_routers(self.apiClient, listall=True, state="Running")[0]

        system_vms.append(virtual_router)

        self._check_system_vms(system_vms, primary_storage_1.id)

        primary_storage[TestData.name] = TestData.get_name_for_solidfire_storage()

        primary_storage_2 = StoragePool.create(
            self.apiClient,
            primary_storage
        )

        StoragePool.enableMaintenance(self.apiClient, primary_storage_1.id)

        self._wait_for_storage_cleanup_thread(system_vms)

        sf_util.purge_solidfire_volumes(self.sfe)

        system_vms = self._wait_for_and_get_running_system_vms(2)

        virtual_router = list_routers(self.apiClient, listall=True, state="Running")[0]

        system_vms.append(virtual_router)

        self._check_system_vms(system_vms, primary_storage_2.id)

        StoragePool.cancelMaintenance(self.apiClient, primary_storage_1.id)

        primary_storage_1.delete(self.apiClient)

        self._disable_zone_and_delete_system_vms(virtual_router)

        self._wait_for_storage_cleanup_thread(system_vms)

        sf_util.purge_solidfire_volumes(self.sfe)

        primary_storage_2.delete(self.apiClient)

        self._verify_no_active_solidfire_volumes()

        self._prepare_to_stop_using_managed_storage_for_system_vms()

        self.zone.update(self.apiClient, id=self.zone.id, allocationstate=enabled)

        self._wait_for_and_get_running_system_vms(2)

    def test_02_failure_to_create_service_offering_with_customized_iops(self):
        try:
            ServiceOffering.create(
                self.apiClient,
                self.testdata[TestData.systemOfferingFailure]
            )
        except:
            return

        self.assertTrue(False, "The service offering was created, but should not have been.")

    def _prepare_to_use_managed_storage_for_system_vms(self):
        self._update_system_vm_unique_name(TestManagedSystemVMs._secondary_storage_unique_name, TestManagedSystemVMs._secondary_storage_temp_unique_name)
        self._update_system_vm_unique_name(TestManagedSystemVMs._console_proxy_unique_name, TestManagedSystemVMs._console_proxy_temp_unique_name)
        self._update_system_vm_unique_name(TestManagedSystemVMs._virtual_router_unique_name, TestManagedSystemVMs._virtual_router_temp_unique_name)

        self._update_system_vm_unique_name_based_on_uuid(self.secondary_storage_offering.id, TestManagedSystemVMs._secondary_storage_unique_name)
        self._update_system_vm_unique_name_based_on_uuid(self.console_proxy_offering.id, TestManagedSystemVMs._console_proxy_unique_name)
        self._update_system_vm_unique_name_based_on_uuid(self.virtual_router_offering.id, TestManagedSystemVMs._virtual_router_unique_name)

    def _prepare_to_stop_using_managed_storage_for_system_vms(self):
        self._update_system_vm_unique_name_based_on_uuid(self.secondary_storage_offering.id, None)
        self._update_system_vm_unique_name_based_on_uuid(self.console_proxy_offering.id, None)
        self._update_system_vm_unique_name_based_on_uuid(self.virtual_router_offering.id, None)

        self._update_system_vm_unique_name(TestManagedSystemVMs._secondary_storage_temp_unique_name, TestManagedSystemVMs._secondary_storage_unique_name)
        self._update_system_vm_unique_name(TestManagedSystemVMs._console_proxy_temp_unique_name, TestManagedSystemVMs._console_proxy_unique_name)
        self._update_system_vm_unique_name(TestManagedSystemVMs._virtual_router_temp_unique_name, TestManagedSystemVMs._virtual_router_unique_name)

    def _wait_for_storage_cleanup_thread(self, system_vms):
        retry_interval = 60
        num_tries = 10

        wait_result, return_val = wait_until(retry_interval, num_tries, self._check_resource_state, system_vms)

        if not wait_result:
            raise Exception(return_val)

    def _check_resource_state(self, system_vms):
        try:
            self._verify_system_vms_deleted(system_vms)

            return True, None
        except:
            return False, "The system is not in the necessary state."

    def _verify_system_vms_deleted(self, system_vms):
        for system_vm in system_vms:
            cs_root_volume = self._get_root_volume_for_system_vm(system_vm.id, 'Expunged')

            self._verify_managed_system_vm_deleted(cs_root_volume.name)

    def _disable_zone_and_delete_system_vms(self, virtual_router, verify_managed_system_vm_deleted=True):
        self.zone.update(self.apiClient, id=self.zone.id, allocationstate="Disabled")

        if virtual_router is not None:
            Router.destroy(self.apiClient, virtual_router.id)

            if verify_managed_system_vm_deleted:
                cs_root_volume = self._get_root_volume_for_system_vm(virtual_router.id, 'Expunged')

                self._verify_managed_system_vm_deleted(cs_root_volume.name)

        # list_ssvms lists the secondary storage VM and the console proxy VM
        system_vms = list_ssvms(self.apiClient)

        for system_vm in system_vms:
            destroy_ssvm_cmd = destroySystemVm.destroySystemVmCmd()

            destroy_ssvm_cmd.id = system_vm.id

            self.apiClient.destroySystemVm(destroy_ssvm_cmd)

            if verify_managed_system_vm_deleted:
                cs_root_volume = self._get_root_volume_for_system_vm(system_vm.id, 'Expunged')

                self._verify_managed_system_vm_deleted(cs_root_volume.name)

    def _verify_managed_system_vm_deleted(self, cs_root_volume_name):
        sf_not_active_volumes = sf_util.get_not_active_sf_volumes(self.sfe)

        sf_root_volume = sf_util.check_and_get_sf_volume(sf_not_active_volumes, cs_root_volume_name, self)

        self.assertEqual(
            len(sf_root_volume.volume_access_groups),
            0,
            "The volume should not be in a volume access group."
        )

        if TestData.hypervisor_type == TestData.xenServer:
            sr_name = sf_util.format_iqn(sf_root_volume.iqn)

            sf_util.check_xen_sr(sr_name, self.xen_session, self, False)
        elif TestData.hypervisor_type == TestData.kvm:
            list_hosts_response = list_hosts(
                self.apiClient,
                type="Routing"
            )

            kvm_login = self.testdata[TestData.kvm]

            sf_util.check_kvm_access_to_volume(sf_root_volume.iqn, list_hosts_response, kvm_login[TestData.username], kvm_login[TestData.password], self, False)
        else:
            self.assertTrue(False, "Invalid hypervisor type")

    def _wait_for_and_get_running_system_vms(self, expected_number_of_system_vms):
        retry_interval = 60
        num_tries = 10

        wait_result, return_val = wait_until(retry_interval, num_tries, self._check_number_of_running_system_vms, expected_number_of_system_vms)

        if not wait_result:
            raise Exception(return_val)

        return return_val

    def _check_number_of_running_system_vms(self, expected_number_of_system_vms):
        # list_ssvms lists the secondary storage VM and the console proxy VM
        system_vms = list_ssvms(self.apiClient, state="Running")

        if system_vms is not None and len(system_vms) == expected_number_of_system_vms:
            return True, system_vms

        return False, "Timed out waiting for running system VMs"

    def _verify_no_active_solidfire_volumes(self):
        sf_active_volumes = sf_util.get_active_sf_volumes(self.sfe)

        sf_util.check_list(sf_active_volumes, 0, self, "There should be no active SolidFire volumes in the cluster.")

    def _check_system_vms(self, system_vms, primary_storage_id):
        sf_active_volumes = sf_util.get_active_sf_volumes(self.sfe)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, primary_storage_id, self)

        for system_vm in system_vms:
            cs_root_volume = self._get_root_volume_for_system_vm(system_vm.id, 'Ready')
            sf_root_volume = sf_util.check_and_get_sf_volume(sf_active_volumes, cs_root_volume.name, self)

            sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, cs_root_volume, self)

            sf_util.check_size_and_iops(sf_root_volume, cs_root_volume, sf_volume_size, self)

            self._check_iops_against_iops_of_system_offering(cs_root_volume, self.testdata[TestData.systemOffering])

            sf_util.check_vag(sf_root_volume, sf_vag_id, self)

            if TestData.hypervisor_type == TestData.xenServer:
                sr_name = sf_util.format_iqn(sf_root_volume.iqn)

                sf_util.check_xen_sr(sr_name, self.xen_session, self)
            elif TestData.hypervisor_type == TestData.kvm:
                list_hosts_response = list_hosts(
                    self.apiClient,
                    type="Routing"
                )

                kvm_login = self.testdata[TestData.kvm]

                sf_util.check_kvm_access_to_volume(sf_root_volume.iqn, list_hosts_response, kvm_login[TestData.username], kvm_login[TestData.password], self)
            else:
                self.assertTrue(False, "Invalid hypervisor type")

    def _check_iops_against_iops_of_system_offering(self, cs_volume, system_offering):
        self.assertEqual(
            system_offering[TestData.min_iops],
            cs_volume.miniops,
            "Check QoS - Min IOPS: of " + cs_volume.name + " should be " + str(system_offering[TestData.min_iops])
        )

        self.assertEqual(
            system_offering[TestData.max_iops],
            cs_volume.maxiops,
            "Check QoS - Min IOPS: of " + cs_volume.name + " should be " + str(system_offering[TestData.max_iops])
        )

    def _get_root_volume_for_system_vm(self, system_vm_id, state):
        sql_query = "Select id From vm_instance Where uuid = '" + system_vm_id + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        instance_id = sql_result[0][0]

        sql_query = "Select uuid, name, min_iops, max_iops From volumes Where instance_id = " + str(instance_id) + \
            " and state = '" + state + "' Order by removed desc"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        uuid = sql_result[0][0]
        name = sql_result[0][1]
        min_iops = sql_result[0][2]
        max_iops = sql_result[0][3]

        class CloudStackVolume(object):
            pass

        cs_volume = CloudStackVolume()

        cs_volume.id = uuid
        cs_volume.name = name
        cs_volume.miniops = min_iops
        cs_volume.maxiops = max_iops

        return cs_volume

    def _update_system_vm_unique_name(self, unique_name, new_unique_name):
        sql_query = "Update disk_offering set unique_name = '" + new_unique_name + "' Where unique_name = '" + unique_name + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        self.dbConnection.execute(sql_query)

    def _update_system_vm_unique_name_based_on_uuid(self, uuid, new_unique_name):
        if (new_unique_name is None):
            sql_query = "Update disk_offering set unique_name = NULL Where uuid = '" + uuid + "'"
        else:
            sql_query = "Update disk_offering set unique_name = '" + new_unique_name + "' Where uuid = '" + uuid + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        self.dbConnection.execute(sql_query)

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

