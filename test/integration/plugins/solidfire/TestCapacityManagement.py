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
from marvin.lib.base import Account, ServiceOffering, StoragePool, User, VirtualMachine

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_hosts

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
#  If you do have more than one cluster, you might need to change this variable: TestData.clusterId.


class TestData():
    # constants
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    computeOffering2 = "computeoffering2"
    domainId = "domainId"
    email = "email"
    firstname = "firstname"
    hypervisor = "hypervisor"
    lastname = "lastname"
    mvip = "mvip"
    name = "name"
    password = "password"
    port = "port"
    primaryStorage = "primarystorage"
    primaryStorage2 = "primarystorage2"
    provider = "provider"
    scope = "scope"
    solidFire = "solidfire"
    storageTag = "SolidFire_SAN_1"
    storageTag2 = "SolidFire_SAN_2"
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
                TestData.name: "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                TestData.url: "MVIP=10.117.40.120;SVIP=10.117.41.120;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 100000,
                TestData.capacityBytes: 214748364800, # 200 GiB
                TestData.hypervisor: "Any"
            },
            TestData.primaryStorage2: {
                TestData.name: "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                TestData.url: "MVIP=10.117.40.120;SVIP=10.117.41.120;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag2,
                TestData.capacityIops: 800,
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
                TestData.tags: TestData.storageTag
            },
            TestData.computeOffering2: {
                TestData.name: "SF_CO_2",
                "displaytext": "SF_CO_2 (Min IOPS = 300; Max IOPS = 600)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": "300",
                "maxiops": "600",
                "hypervisorsnapshotreserve": 200,
                TestData.tags: TestData.storageTag2
            },
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }


class TestCapacityManagement(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestCapacityManagement, cls).getClsTestClient()

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
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
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

        cls.compute_offering_2 = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering2]
        )

        # Resources that are to be destroyed
        cls._cleanup = [
            cls.compute_offering,
            cls.compute_offering_2,
            cls.user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)

            cls.primary_storage.delete(cls.apiClient)
            cls.primary_storage_2.delete(cls.apiClient)

            sf_util.purge_solidfire_volumes(cls.sfe)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiClient, self.cleanup)

    def test_01_not_enough_storage_space(self):
        self._run_vms(self.compute_offering.id)

    def test_02_not_enough_storage_performance(self):
        self._run_vms(self.compute_offering_2.id)

    def _run_vms(self, compute_offering_id):
        try:
            # Based on the primary storage's space or performance and the storage requirements
            # of the compute offering, we should fail to create a VM on the third try.
            for _ in range(0, 3):
                number = random.randint(0, 1000)

                vm_name = {
                    TestData.name: "VM-%d" % number,
                    "displayname": "Test VM %d" % number
                }

                virtual_machine = VirtualMachine.create(
                    self.apiClient,
                    vm_name,
                    accountid=self.account.name,
                    zoneid=self.zone.id,
                    serviceofferingid=compute_offering_id,
                    templateid=self.template.id,
                    domainid=self.domain.id,
                    startvm=True
                )

                self.cleanup.append(virtual_machine)
        except:
            pass

        self.assertEqual(
            len(self.cleanup),
            2,
            "Only two VMs should have been successfully created."
        )

    @classmethod
    def _connect_to_hypervisor(cls):
        host_ip = "https://" + \
              list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name=TestData.xen_server_hostname)[0].ipaddress

        cls.xen_session = XenAPI.Session(host_ip)

        xen_server = cls.testdata[TestData.xenServer]

        cls.xen_session.xenapi.login_with_password(xen_server[TestData.username], xen_server[TestData.password])
