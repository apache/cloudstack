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
from marvin.lib.base import Account, ServiceOffering, User, Host, StoragePool, VirtualMachine

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_hosts, list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

# Prerequisites:
#  Only one zone
#  Only one pod
#  Only one cluster (two hosts for XenServer / one host for KVM with another added/removed during the tests)
#
# Running the tests:
#  Change the "hypervisor_type" variable to control which hypervisor type to test.
#  If using XenServer, set a breakpoint on each test after the first one. When the breakpoint is hit, reset the
#   added/removed host to a snapshot state and re-start it. Once it's up and running, run the test code.
#  Check that ip_address_of_new_xenserver_host / ip_address_of_new_kvm_host is correct.
#  If using XenServer, verify the "xen_server_master_hostname" variable is correct.
#  If using KVM, verify the "kvm_1_ip_address" variable is correct.
#
# Note:
#  If you do have more than one cluster, you might need to change this line: cls.cluster = list_clusters(cls.apiClient)[0] and
#   this variable's value: TestData.clusterId.


class TestData:
    #constants
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    displayText = "displaytext"
    diskSize = "disksize"
    domainId = "domainId"
    hypervisor = "hypervisor"
    kvm = "kvm"
    mvip = "mvip"
    name = "name"
    newXenServerHost = "newXenServerHost"
    newKvmHost = "newKvmHost"
    newHostDisplayName = "newHostDisplayName"
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
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    volume_1 = "volume_1"
    xenServer = "xenserver"
    zoneId = "zoneid"

    # modify to control which hypervisor type to test
    hypervisor_type = xenServer
    xen_server_master_hostname = "XenServer-6.5-1"
    kvm_1_ip_address = "10.117.40.111"
    ip_address_of_new_xenserver_host = "10.117.40.118"
    ip_address_of_new_kvm_host = "10.117.40.115"

    def __init__(self):
        self.testdata = {
            TestData.solidFire: {
                TestData.mvip: "10.117.78.225",
                TestData.username: "admin",
                TestData.password: "admin",
                TestData.port: 443,
                TestData.url: "https://10.117.78.225:443"
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
                TestData.username: "test",
                TestData.password: "test"
            },
            TestData.user: {
                "email": "user@test.com",
                "firstname": "Jane",
                "lastname": "Doe",
                TestData.username: "testuser",
                TestData.password: "password"
            },
            TestData.newXenServerHost: {
                TestData.username: "root",
                TestData.password: "solidfire",
                TestData.url: "http://" + TestData.ip_address_of_new_xenserver_host,
                TestData.podId : "1",
                TestData.zoneId: "1"
            },
            TestData.newKvmHost: {
                TestData.username: "root",
                TestData.password: "solidfire",
                TestData.url: "http://" + TestData.ip_address_of_new_kvm_host,
                TestData.podId : "1",
                TestData.zoneId: "1"
            },
            TestData.primaryStorage: {
                TestData.name: "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                TestData.url: "MVIP=10.117.78.225;SVIP=10.117.94.225;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 4500000,
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "Any"
            },
            TestData.primaryStorage2: {
                TestData.name: "SolidFireShared-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: "MVIP=10.117.78.225;SVIP=10.117.94.225;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "minIops=5000;maxIops=50000;burstIops=75000",
                TestData.provider: "SolidFireShared",
                TestData.tags: TestData.storageTag2,
                TestData.capacityIops: 5000,
                TestData.capacityBytes: 1099511627776,
                TestData.hypervisor: "XenServer",
                TestData.podId: 1
            },
            TestData.virtualMachine: {
                TestData.name: "TestVM",
                "displayname": "Test VM"
            },
            TestData.computeOffering: {
                TestData.name: "SF_CO_1",
                TestData.displayText: "SF_CO_1 (Min IOPS = 10,000; Max IOPS = 15,000)",
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
            TestData.volume_1: {
                "diskname": "testvolume",
            },
            TestData.newHostDisplayName: "XenServer-6.5-3",
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }


class TestAddRemoveHosts(cloudstackTestCase):
    _vag_id_should_be_non_zero_int_err_msg = "The SolidFire VAG ID should be a non-zero integer."
    _sf_account_id_should_be_non_zero_int_err_msg = "The SolidFire account ID should be a non-zero integer."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestAddRemoveHosts, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        if TestData.hypervisor_type == TestData.xenServer:
            cls.xs_pool_master_ip = list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name=TestData.xen_server_master_hostname)[0].ipaddress

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
            cls.testdata[TestData.account],
            admin=1
        )

        # Set up connection to make customized API calls
        user = User.create(
            cls.apiClient,
            cls.testdata[TestData.user],
            account=cls.account.name,
            domainid=cls.domain.id
        )

        url = cls.testdata[TestData.url]

        api_url = "http://" + url + ":8080/client/api"
        userkeys = User.registerUserKeys(cls.apiClient, user.id)

        cls.cs_api = SignedAPICall.CloudStack(api_url, userkeys.apikey, userkeys.secretkey)

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        cls._cleanup = [
            cls.compute_offering,
            user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)

            sf_util.purge_solidfire_volumes(cls.sfe)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.virtual_machine = None

        self.cleanup = []

    def tearDown(self):
        try:
            if self.virtual_machine is not None:
                self.virtual_machine.delete(self.apiClient, True)

            cleanup_resources(self.apiClient, self.cleanup)
        except Exception as e:
            logging.debug("Exception in tearDown(self): %s" % e)

    def test_add_remove_host_with_solidfire_plugin_1(self):
        primarystorage = self.testdata[TestData.primaryStorage]

        primary_storage = StoragePool.create(
            self.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=self.zone.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage)

        self.virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        if TestData.hypervisor_type == TestData.xenServer:
            root_volume = self._get_root_volume(self.virtual_machine)

            sf_iscsi_name = sf_util.get_iqn(self.cs_api, root_volume, self)
            self._perform_add_remove_xenserver_host(primary_storage.id, sf_iscsi_name)
        elif TestData.hypervisor_type == TestData.kvm:
            self._perform_add_remove_kvm_host(primary_storage.id)
        else:
            self.assertTrue(False, "Invalid hypervisor type")

    def test_add_remove_host_with_solidfire_plugin_2(self):
        if TestData.hypervisor_type != TestData.xenServer:
            return

        primarystorage2 = self.testdata[TestData.primaryStorage2]

        primary_storage_2 = StoragePool.create(
            self.apiClient,
            primarystorage2,
            scope=primarystorage2[TestData.scope],
            zoneid=self.zone.id,
            clusterid=self.cluster.id,
            provider=primarystorage2[TestData.provider],
            tags=primarystorage2[TestData.tags],
            capacityiops=primarystorage2[TestData.capacityIops],
            capacitybytes=primarystorage2[TestData.capacityBytes],
            hypervisor=primarystorage2[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage_2)

        sf_iscsi_name = self._get_iqn_2(primary_storage_2)

        self._perform_add_remove_xenserver_host(primary_storage_2.id, sf_iscsi_name)

    def test_add_remove_host_with_solidfire_plugin_3(self):
        if TestData.hypervisor_type != TestData.xenServer:
            return

        primarystorage = self.testdata[TestData.primaryStorage]

        primary_storage = StoragePool.create(
            self.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=self.zone.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage)

        self.virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        root_volume = self._get_root_volume(self.virtual_machine)

        sf_iscsi_name = sf_util.get_iqn(self.cs_api, root_volume, self)

        primarystorage2 = self.testdata[TestData.primaryStorage2]

        primary_storage_2 = StoragePool.create(
            self.apiClient,
            primarystorage2,
            scope=primarystorage2[TestData.scope],
            zoneid=self.zone.id,
            clusterid=self.cluster.id,
            provider=primarystorage2[TestData.provider],
            tags=primarystorage2[TestData.tags],
            capacityiops=primarystorage2[TestData.capacityIops],
            capacitybytes=primarystorage2[TestData.capacityBytes],
            hypervisor=primarystorage2[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage_2)

        self._perform_add_remove_xenserver_host(primary_storage.id, sf_iscsi_name)

    def test_add_remove_host_with_solidfire_plugin_4(self):
        if TestData.hypervisor_type != TestData.xenServer:
            return

        primarystorage2 = self.testdata[TestData.primaryStorage2]

        primary_storage_2 = StoragePool.create(
            self.apiClient,
            primarystorage2,
            scope=primarystorage2[TestData.scope],
            zoneid=self.zone.id,
            clusterid=self.cluster.id,
            provider=primarystorage2[TestData.provider],
            tags=primarystorage2[TestData.tags],
            capacityiops=primarystorage2[TestData.capacityIops],
            capacitybytes=primarystorage2[TestData.capacityBytes],
            hypervisor=primarystorage2[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage_2)

        sf_iscsi_name = self._get_iqn_2(primary_storage_2)

        primarystorage = self.testdata[TestData.primaryStorage]

        primary_storage = StoragePool.create(
            self.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=self.zone.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage)

        self.virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        self._perform_add_remove_xenserver_host(primary_storage_2.id, sf_iscsi_name)

    # Make sure each host is in its own VAG.
    # Create a VM that needs a new volume from the storage that has a VAG per host.
    # Verify the volume is in all VAGs.
    # Remove one of the hosts.
    # Check that the IQN is no longer in its previous VAG, but that the volume ID is still in that VAG, though.
    # Add the host back into the cluster. The IQN should be added to a VAG that already has an IQN from this cluster in it.
    def test_vag_per_host_5(self):
        hosts = list_hosts(self.apiClient, clusterid=self.cluster.id)

        self.assertTrue(
            len(hosts) >= 2,
            "There needs to be at least two hosts."
        )

        unique_vag_ids = self._get_unique_vag_ids(hosts)

        self.assertTrue(len(hosts) == len(unique_vag_ids), "To run this test, each host should be in its own VAG.")

        primarystorage = self.testdata[TestData.primaryStorage]

        primary_storage = StoragePool.create(
            self.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=self.zone.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage)

        self.virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        root_volume = self._get_root_volume(self.virtual_machine)

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, primary_storage.id, self, TestAddRemoveHosts._sf_account_id_should_be_non_zero_int_err_msg)

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, root_volume.name, self)

        sf_vag_ids = sf_util.get_vag_ids(self.cs_api, self.cluster.id, primary_storage.id, self)

        sf_util.check_vags(sf_volume, sf_vag_ids, self)

        host = Host(hosts[0].__dict__)

        host_iqn = self._get_host_iqn(host)

        all_vags = sf_util.get_all_vags(self.sfe)

        host_vag = self._get_host_vag(host_iqn, all_vags)

        self.assertTrue(host_vag != None, "The host should be in a VAG.")

        host.delete(self.apiClient)

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, root_volume.name, self)

        sf_util.check_vags(sf_volume, sf_vag_ids, self)

        all_vags = sf_util.get_all_vags(self.sfe)

        host_vag = self._get_host_vag(host_iqn, all_vags)

        self.assertTrue(host_vag == None, "The host should not be in a VAG.")

        details = {
            TestData.username: "root",
            TestData.password: "solidfire",
            TestData.url: "http://" + host.ipaddress,
            TestData.podId : host.podid,
            TestData.zoneId: host.zoneid
        }

        host = Host.create(
            self.apiClient,
            self.cluster,
            details,
            hypervisor=host.hypervisor
        )

        self.assertTrue(
            isinstance(host, Host),
            "'host' is not a 'Host'."
        )

        hosts = list_hosts(self.apiClient, clusterid=self.cluster.id)

        unique_vag_ids = self._get_unique_vag_ids(hosts)

        self.assertTrue(len(hosts) == len(unique_vag_ids) + 1, "There should be one more host than unique VAG.")

    def _get_unique_vag_ids(self, hosts):
        all_vags = sf_util.get_all_vags(self.sfe)

        unique_vag_ids = []

        for host in hosts:
            host = Host(host.__dict__)

            host_iqn = self._get_host_iqn(host)

            host_vag = self._get_host_vag(host_iqn, all_vags)

            if host_vag != None and host_vag.volume_access_group_id not in unique_vag_ids:
                unique_vag_ids.append(host_vag.volume_access_group_id)

        return unique_vag_ids

    def _get_host_vag(self, host_iqn, vags):
        self.assertTrue(host_iqn, "'host_iqn' should not be 'None'.")
        self.assertTrue(vags, "'vags' should not be 'None'.")

        self.assertTrue(isinstance(host_iqn, str), "'host_iqn' should be a 'string'.")
        self.assertTrue(isinstance(vags, list), "'vags' should be a 'list'.")

        for vag in vags:
            if host_iqn in vag.initiators:
                return vag

        return None

    def _perform_add_remove_xenserver_host(self, primary_storage_id, sr_name):
        xen_sr = self.xen_session.xenapi.SR.get_by_name_label(sr_name)[0]

        pbds = self.xen_session.xenapi.SR.get_PBDs(xen_sr)

        self._verify_all_pbds_attached(pbds)

        num_pbds = len(pbds)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, primary_storage_id, self)

        host_iscsi_iqns = self._get_xenserver_host_iscsi_iqns()

        sf_vag = self._get_sf_vag(sf_vag_id)

        sf_vag_initiators = self._get_sf_vag_initiators(sf_vag)

        self._verifyVag(host_iscsi_iqns, sf_vag_initiators)

        sf_vag_initiators_len_orig = len(sf_vag_initiators)

        xen_session = XenAPI.Session("https://" + TestData.ip_address_of_new_xenserver_host)

        xenserver = self.testdata[TestData.xenServer]

        xen_session.xenapi.login_with_password(xenserver[TestData.username], xenserver[TestData.password])

        xen_session.xenapi.pool.join(self.xs_pool_master_ip, xenserver[TestData.username], xenserver[TestData.password])

        time.sleep(60)

        pbds = self.xen_session.xenapi.SR.get_PBDs(xen_sr)

        self.assertEqual(
            len(pbds),
            num_pbds + 1,
            "'len(pbds)' is not equal to 'num_pbds + 1'."
        )

        num_pbds = num_pbds + 1

        num_pbds_not_attached = 0

        for pbd in pbds:
            pbd_record = self.xen_session.xenapi.PBD.get_record(pbd)

            if pbd_record["currently_attached"] == False:
                num_pbds_not_attached = num_pbds_not_attached + 1

        self.assertEqual(
            num_pbds_not_attached,
            1,
            "'num_pbds_not_attached' is not equal to 1."
        )

        host = Host.create(
            self.apiClient,
            self.cluster,
            self.testdata[TestData.newXenServerHost],
            hypervisor="XenServer"
        )

        self.assertTrue(
            isinstance(host, Host),
            "'host' is not a 'Host'."
        )

        pbds = self.xen_session.xenapi.SR.get_PBDs(xen_sr)

        self.assertEqual(
            len(pbds),
            num_pbds,
            "'len(pbds)' is not equal to 'num_pbds'."
        )

        self._verify_all_pbds_attached(pbds)

        host_iscsi_iqns = self._get_xenserver_host_iscsi_iqns()

        sf_vag = self._get_sf_vag(sf_vag_id)

        sf_vag_initiators = self._get_sf_vag_initiators(sf_vag)

        self._verifyVag(host_iscsi_iqns, sf_vag_initiators)

        sf_vag_initiators_len_new = len(sf_vag_initiators)

        self.assertEqual(
            sf_vag_initiators_len_new,
            sf_vag_initiators_len_orig + 1,
            "sf_vag_initiators_len_new' != sf_vag_initiators_len_orig + 1"
        )

        host.delete(self.apiClient)

        pbds = self.xen_session.xenapi.SR.get_PBDs(xen_sr)

        self.assertEqual(
            len(pbds),
            num_pbds,
            "'len(pbds)' is not equal to 'num_pbds'."
        )

        self._verify_all_pbds_attached(pbds)

        host_iscsi_iqns = self._get_xenserver_host_iscsi_iqns()

        sf_vag = self._get_sf_vag(sf_vag_id)

        sf_vag_initiators = self._get_sf_vag_initiators(sf_vag)

        self.assertEqual(
            len(host_iscsi_iqns) - 1,
            len(sf_vag_initiators),
            "'len(host_iscsi_iqns) - 1' is not equal to 'len(sf_vag_initiators)'."
        )

        host_ref = self.xen_session.xenapi.host.get_by_name_label(self.testdata[TestData.newHostDisplayName])[0]

        self.xen_session.xenapi.pool.eject(host_ref)

        time.sleep(120)

        pbds = self.xen_session.xenapi.SR.get_PBDs(xen_sr)

        self.assertEqual(
            len(pbds),
            num_pbds - 1,
            "'len(pbds)' is not equal to 'num_pbds - 1'."
        )

        self._verify_all_pbds_attached(pbds)

        host_iscsi_iqns = self._get_xenserver_host_iscsi_iqns()

        sf_vag = self._get_sf_vag(sf_vag_id)

        sf_vag_initiators = self._get_sf_vag_initiators(sf_vag)

        self._verifyVag(host_iscsi_iqns, sf_vag_initiators)

        sf_vag_initiators_len_new = len(sf_vag_initiators)

        self.assertEqual(
            sf_vag_initiators_len_new,
            sf_vag_initiators_len_orig,
            "sf_vag_initiators_len_new' != sf_vag_initiators_len_orig"
        )

    def _perform_add_remove_kvm_host(self, primary_storage_id):
        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, primary_storage_id, self)

        kvm_login = self.testdata[TestData.kvm]

        kvm_hosts = []

        kvm_hosts.append(TestData.kvm_1_ip_address)

        host_iscsi_iqns = self._get_kvm_host_iscsi_iqns(kvm_hosts, kvm_login[TestData.username], kvm_login[TestData.password])

        sf_vag = self._get_sf_vag(sf_vag_id)

        sf_vag_initiators = self._get_sf_vag_initiators(sf_vag)

        self._verifyVag(host_iscsi_iqns, sf_vag_initiators)

        sf_vag_initiators_len_orig = len(sf_vag_initiators)

        host = Host.create(
            self.apiClient,
            self.cluster,
            self.testdata[TestData.newKvmHost],
            hypervisor="KVM"
        )

        self.assertTrue(
            isinstance(host, Host),
            "'host' is not a 'Host'."
        )

        kvm_hosts = []

        kvm_hosts.append(TestData.kvm_1_ip_address)
        kvm_hosts.append(TestData.ip_address_of_new_kvm_host)

        host_iscsi_iqns = self._get_kvm_host_iscsi_iqns(kvm_hosts, kvm_login[TestData.username], kvm_login[TestData.password])

        sf_vag = self._get_sf_vag(sf_vag_id)

        sf_vag_initiators = self._get_sf_vag_initiators(sf_vag)

        self._verifyVag(host_iscsi_iqns, sf_vag_initiators)

        sf_vag_initiators_len_new = len(sf_vag_initiators)

        self.assertEqual(
            sf_vag_initiators_len_new,
            sf_vag_initiators_len_orig + 1,
            "sf_vag_initiators_len_new' != sf_vag_initiators_len_orig + 1"
        )

        host.delete(self.apiClient)

        kvm_hosts = []

        kvm_hosts.append(TestData.kvm_1_ip_address)

        host_iscsi_iqns = self._get_kvm_host_iscsi_iqns(kvm_hosts, kvm_login[TestData.username], kvm_login[TestData.password])

        sf_vag = self._get_sf_vag(sf_vag_id)

        sf_vag_initiators = self._get_sf_vag_initiators(sf_vag)

        self._verifyVag(host_iscsi_iqns, sf_vag_initiators)

        sf_vag_initiators_len_new = len(sf_vag_initiators)

        self.assertEqual(
            sf_vag_initiators_len_new,
            sf_vag_initiators_len_orig,
            "sf_vag_initiators_len_new' != sf_vag_initiators_len_orig"
        )

    def _verify_all_pbds_attached(self, pbds):
        for pbd in pbds:
            pbd_record = self.xen_session.xenapi.PBD.get_record(pbd)

            self.assertEqual(
                pbd_record["currently_attached"],
                True,
                "Not all PBDs are currently attached."
            )

    def _get_root_volume(self, vm):
        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=vm.id,
            listall=True
        )

        self.assertNotEqual(
            list_volumes_response,
            None,
            "'list_volumes_response' should not be equal to 'None'."
        )

        self.assertEqual(
            len(list_volumes_response) > 0,
            True,
            "'len(list_volumes_response)' should be greater than 0."
        )

        for volume in list_volumes_response:
            if volume.type.upper() == "ROOT":
                return volume

        self.assertTrue(False, "Unable to locate the ROOT volume of the VM with the following ID: " + str(vm.id))

    def _get_iqn_2(self, primary_storage):
        sql_query = "Select path From storage_pool Where uuid = '" + str(primary_storage.id) + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        return sql_result[0][0]

    def _get_host_iqn(self, host):
        sql_query = "Select url From host Where uuid = '" + str(host.id) + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        return sql_result[0][0]

    def _get_xenserver_host_iscsi_iqns(self):
        hosts = self.xen_session.xenapi.host.get_all()

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "'hosts' is not a list."
        )

        host_iscsi_iqns = []

        for host in hosts:
            host_iscsi_iqns.append(self._get_xenserver_host_iscsi_iqn(host))

        return host_iscsi_iqns

    def _get_xenserver_host_iscsi_iqn(self, host):
        other_config = self.xen_session.xenapi.host.get_other_config(host)

        return other_config["iscsi_iqn"]

    def _get_kvm_host_iscsi_iqns(self, kvm_ip_addresses, common_username, common_password):
        host_iscsi_iqns = []

        for kvm_ip_address in kvm_ip_addresses:
            host_iscsi_iqn = self._get_kvm_iqn(kvm_ip_address, common_username, common_password)

            host_iscsi_iqns.append(host_iscsi_iqn)

        return host_iscsi_iqns

    def _get_kvm_iqn(self, ip_address, username, password):
        ssh_connection = sf_util.get_ssh_connection(ip_address, username, password)

        searchFor = "InitiatorName="

        stdout = ssh_connection.exec_command("sudo grep " + searchFor + " /etc/iscsi/initiatorname.iscsi")[1]

        result = stdout.read()

        ssh_connection.close()

        self.assertFalse(result is None, "Unable to locate the IQN of the KVM host (None)")
        self.assertFalse(len(result.strip()) == 0, "Unable to locate the IQN of the KVM host (Zero-length string)")

        return result[len(searchFor):].strip()

    def _get_sf_vag(self, sf_vag_id):
        return self.sfe.list_volume_access_groups(sf_vag_id, 1).volume_access_groups[0]

    def _get_sf_vag_initiators(self, sf_vag):
        return sf_vag.initiators

    def _verifyVag(self, host_iscsi_iqns, sf_vag_initiators):
        self.assertEqual(
            isinstance(host_iscsi_iqns, list),
            True,
            "'host_iscsi_iqns' is not a list."
        )

        self.assertEqual(
            isinstance(sf_vag_initiators, list),
            True,
            "'sf_vag_initiators' is not a list."
        )

        self.assertEqual(
            len(host_iscsi_iqns),
            len(sf_vag_initiators),
            "Lists are not the same size."
        )

        for host_iscsi_iqn in host_iscsi_iqns:
            # an error should occur if host_iscsi_iqn is not in sf_vag_initiators
            sf_vag_initiators.index(host_iscsi_iqn)

    @classmethod
    def _connect_to_hypervisor(cls):
        if TestData.hypervisor_type == TestData.kvm:
            pass
        elif TestData.hypervisor_type == TestData.xenServer:
            host_ip = "https://" + \
                  list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name=TestData.xen_server_master_hostname)[0].ipaddress

            cls.xen_session = XenAPI.Session(host_ip)

            xen_server = cls.testdata[TestData.xenServer]

            cls.xen_session.xenapi.login_with_password(xen_server[TestData.username], xen_server[TestData.password])

