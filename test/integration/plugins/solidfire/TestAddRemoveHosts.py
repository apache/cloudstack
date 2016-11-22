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

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, ServiceOffering, User, Host, StoragePool, VirtualMachine

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_hosts, list_clusters, list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

from solidfire import solidfire_element_api as sf_api


class TestData:
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    displayText = "displaytext"
    diskSize = "disksize"
    domainId = "domainId"
    hypervisor = "hypervisor"
    login = "login"
    mvip = "mvip"
    name = "name"
    newHost = "newHost"
    newHostDisplayName = "newHostDisplayName"
    osType = "ostype"
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
    urlOfNewHost = "urlOfNewHost"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    volume_1 = "volume_1"
    xenServer = "xenserver"
    zoneId = "zoneid"

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
            TestData.urlOfNewHost: "https://192.168.129.243",
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
            TestData.newHost: {
                TestData.username: "root",
                TestData.password: "solidfire",
                TestData.url: "http://192.168.129.243",
                TestData.podId : "1",
                TestData.zoneId: "1"
            },
            TestData.primaryStorage: {
                TestData.name: "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                TestData.url: "MVIP=192.168.139.112;SVIP=10.10.8.112;" +
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
                TestData.url: "MVIP=192.168.139.112;SVIP=10.10.8.112;" +
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
            "volume2": {
                "diskname": "testvolume2",
            },
            TestData.newHostDisplayName: "XenServer-6.5-3",
            TestData.osType: "CentOS 5.6(64-bit) no GUI (XenServer)",
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "192.168.129.50"
        }


class TestAddRemoveHosts(cloudstackTestCase):
    _vag_id_should_be_non_zero_int_err_msg = "The SolidFire VAG ID should be a non-zero integer."
    _sf_account_id_should_be_non_zero_int_err_msg = "The SolidFire account ID should be a non-zero integer."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestAddRemoveHosts, cls).getClsTestClient()
        cls.apiClient = testclient.getApiClient()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        cls.xs_pool_master_ip = list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name="XenServer-6.5-1")[0].ipaddress

        # Set up XenAPI connection
        host_ip = "https://" + cls.xs_pool_master_ip

        cls.xen_session = XenAPI.Session(host_ip)

        xenserver = cls.testdata[TestData.xenServer]

        cls.xen_session.xenapi.login_with_password(xenserver[TestData.username], xenserver[TestData.password])

        # Set up SolidFire connection
        cls.sf_client = sf_api.SolidFireAPI(endpoint_dict=cls.testdata[TestData.solidFire])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.cluster = list_clusters(cls.apiClient)[0]
        cls.template = get_template(cls.apiClient, cls.zone.id, cls.testdata[TestData.osType])
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

            cls._purge_solidfire_volumes()
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

        root_volume = self._get_root_volume(self.virtual_machine)

        sf_iscsi_name = self._get_iqn(root_volume)

        self._perform_add_remove_host(primary_storage.id, sf_iscsi_name)

    def test_add_remove_host_with_solidfire_plugin_2(self):
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

        self._perform_add_remove_host(primary_storage_2.id, sf_iscsi_name)

    def test_add_remove_host_with_solidfire_plugin_3(self):
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

        sf_iscsi_name = self._get_iqn(root_volume)

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

        self._perform_add_remove_host(primary_storage.id, sf_iscsi_name)

    def test_add_remove_host_with_solidfire_plugin_4(self):
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

        self._perform_add_remove_host(primary_storage_2.id, sf_iscsi_name)

    def _perform_add_remove_host(self, primary_storage_id, sf_iscsi_name):
        xen_sr = self.xen_session.xenapi.SR.get_by_name_label(sf_iscsi_name)[0]

        pbds = self.xen_session.xenapi.SR.get_PBDs(xen_sr)

        self._verify_all_pbds_attached(pbds)

        num_pbds = len(pbds)

        sf_vag_id = self._get_sf_vag_id(self.cluster.id, primary_storage_id)

        host_iscsi_iqns = self._get_host_iscsi_iqns()

        sf_vag = self._get_sf_vag(sf_vag_id)

        sf_vag_initiators = self._get_sf_vag_initiators(sf_vag)

        self._verifyVag(host_iscsi_iqns, sf_vag_initiators)

        sf_vag_initiators_len_orig = len(sf_vag_initiators)

        xen_session = XenAPI.Session(self.testdata[TestData.urlOfNewHost])

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
            self.testdata[TestData.newHost],
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

        host_iscsi_iqns = self._get_host_iscsi_iqns()

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

        host_iscsi_iqns = self._get_host_iscsi_iqns()

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

        host_iscsi_iqns = self._get_host_iscsi_iqns()

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

        self.assert_(False, "Unable to locate the ROOT volume of the VM with the following ID: " + str(vm.id))

    def _get_iqn(self, volume):
        # Get volume IQN
        sf_iscsi_name_request = {'volumeid': volume.id}
        # put this commented line back once PR 1403 is in
        # sf_iscsi_name_result = self.cs_api.getVolumeiScsiName(sf_iscsi_name_request)
        sf_iscsi_name_result = self.cs_api.getSolidFireVolumeIscsiName(sf_iscsi_name_request)
        # sf_iscsi_name = sf_iscsi_name_result['apivolumeiscsiname']['volumeiScsiName']
        sf_iscsi_name = sf_iscsi_name_result['apisolidfirevolumeiscsiname']['solidFireVolumeIscsiName']

        self._check_iscsi_name(sf_iscsi_name)

        return sf_iscsi_name

    def _get_iqn_2(self, primary_storage):
        sql_query = "Select path From storage_pool Where uuid = '" + str(primary_storage.id) + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        return sql_result[0][0]

    def _check_iscsi_name(self, sf_iscsi_name):
        self.assertEqual(
            sf_iscsi_name[0],
            "/",
            "The iSCSI name needs to start with a forward slash."
        )

    def _get_host_iscsi_iqns(self):
        hosts = self.xen_session.xenapi.host.get_all()

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "'hosts' is not a list."
        )

        host_iscsi_iqns = []

        for host in hosts:
            host_iscsi_iqns.append(self._get_host_iscsi_iqn(host))

        return host_iscsi_iqns

    def _get_host_iscsi_iqn(self, host):
        other_config = self.xen_session.xenapi.host.get_other_config(host)

        return other_config["iscsi_iqn"]

    def _get_sf_vag_id(self, cluster_id, primary_storage_id):
        # Get SF Volume Access Group ID
        sf_vag_id_request = {'clusterid': cluster_id, 'storageid': primary_storage_id}
        sf_vag_id_result = self.cs_api.getSolidFireVolumeAccessGroupId(sf_vag_id_request)
        sf_vag_id = sf_vag_id_result['apisolidfirevolumeaccessgroupid']['solidFireVolumeAccessGroupId']

        self.assertEqual(
            isinstance(sf_vag_id, int),
            True,
            TestAddRemoveHosts._vag_id_should_be_non_zero_int_err_msg
        )

        return sf_vag_id

    def _get_sf_vag(self, sf_vag_id):
        return self.sf_client.list_volume_access_groups(sf_vag_id, 1)["volumeAccessGroups"][0]

    def _get_sf_vag_initiators(self, sf_vag):
        return sf_vag["initiators"]

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

    @classmethod
    def _purge_solidfire_volumes(cls):
        deleted_volumes = cls.sf_client.list_deleted_volumes()

        for deleted_volume in deleted_volumes:
            cls.sf_client.purge_deleted_volume(deleted_volume['volumeID'])

