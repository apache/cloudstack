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
""" Tests for API listing of service offerings with different filters
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED
from marvin.lib.base import (Account,
                             Domain,
                             Volume,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain, list_accounts,
                               list_zones, list_clusters, list_hosts)
# Import System modules
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestListServiceOfferings(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestListServiceOfferings, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.apiclient)
        cls.zones = list_zones(cls.apiclient)
        cls.zone = cls.zones[0]
        cls.clusters = list_clusters(cls.apiclient)
        cls.cluster = cls.clusters[0]
        cls.hosts = list_hosts(cls.apiclient)
        cls.account = list_accounts(cls.apiclient, name="admin")[0]
        cls._cleanup = []
        cls.service_offerings = ServiceOffering.list(cls.apiclient)
        cls.system_service_offerings = ServiceOffering.list(cls.apiclient, issystem=True)

        cls.child_domain_1 = Domain.create(
            cls.apiclient,
            cls.services["domain"],
            parentdomainid=cls.domain.id
        )
        cls._cleanup.append(cls.child_domain_1)

        cls.account_1 = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account_1)

        cls.domainadmin_api_client = testClient.getUserApiClient(
            UserName=cls.account_1.user[0].username,
            DomainName=cls.domain.name,
            type=2
        )

        cls.system_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"],
            issystem=True,
            name="custom_system_offering",
            systemvmtype="domainrouter"
        )
        cls._cleanup.append(cls.system_offering)

        cls.service_offering_1 = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"],
            cpunumber=2,
            cpuspeed=2000,
            domainid=cls.child_domain_1.id,
            encryptroot=True,
            name="custom_offering_1",
            zoneid=cls.zone.id
        )
        cls._cleanup.append(cls.service_offering_1)


    @classmethod
    def tearDownClass(cls):
        super(TestListServiceOfferings, cls).tearDownClass()

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_list_service_offerings_cpunumber_filter(self):
        """Test list service offerings with cpunumber filter
        """
        # List all service offerings with cpunumber 1
        service_offerings = ServiceOffering.list(
            self.apiclient,
            cpunumber=1
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings) - len(self.service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertGreaterEqual(
                service_offering.cpunumber,
                1,
                "List ServiceOfferings response has incorrect cpunumber"
            )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            cpunumber=99999
        )
        self.assertIsNone(service_offerings, "List ServiceOfferings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_service_offerings_cpuspeed_filter(self):
        """Test list service offerings with cpuspeed filter
        """
        # List all service offerings with cpuspeed 1000
        service_offerings = ServiceOffering.list(
            self.apiclient,
            cpuspeed=1000
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertGreaterEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertGreaterEqual(
                service_offering.cpuspeed,
                1000,
                "List ServiceOfferings response has incorrect cpuspeed"
            )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            cpuspeed=99999
        )
        self.assertIsNone(service_offerings, "List ServiceOfferings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_service_offerings_memory_filter(self):
        """Test list service offerings with memory filter
        """
        # List all service offerings with memory 256
        service_offerings = ServiceOffering.list(
            self.apiclient,
            memory=256
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertGreaterEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertGreaterEqual(
                service_offering.memory,
                256,
                "List ServiceOfferings response has incorrect memory"
            )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            memory=99999
        )
        self.assertIsNone(service_offerings, "List ServiceOfferings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_list_service_offerings_domainid_filter(self):
        """Test list service offerings with domainid filter
        """
        # List all service offerings with domainid
        service_offerings = ServiceOffering.list(
            self.apiclient,
            domainid=self.domain.id
        )
        self.assertIsNone(
            service_offerings,
            "List ServiceOfferings response is not None"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertEqual(
                service_offering.domainid,
                self.child_domain_1.id,
                "List ServiceOfferings response has incorrect domainid"
            )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_list_service_offerings_encryptroot_filter(self):
        """Test list service offerings with encryptroot filter
        """
        # List all service offerings with encryptroot True
        service_offerings = ServiceOffering.list(
            self.apiclient,
            encryptroot=True
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertGreaterEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertTrue(
                service_offering.encryptroot,
                "List ServiceOfferings response has incorrect encryptroot"
            )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            encryptroot=False
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertGreaterEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertFalse(
                service_offering.encryptroot,
                "List ServiceOfferings response has incorrect encryptroot"
            )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_06_list_service_offerings_id_filter(self):
        """Test list service offerings with id filter
        """
        # List all service offerings with id
        service_offerings = ServiceOffering.list(
            self.apiclient,
            id=self.system_offering.id
        )
        self.assertIsNone(
            service_offerings,
            "List ServiceOfferings response is not None"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            id=self.service_offering_1.id
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        self.assertEqual(
            service_offerings[0].id,
            self.service_offering_1.id,
            "List ServiceOfferings response has incorrect id"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            id=-1
        )
        self.assertIsNone(service_offerings, "List ServiceOfferings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_07_list_service_offerings_isrecursive_filter(self):
        """Test list service offerings with isrecursive filter
        """
        # List all service offerings with listall True
        service_offerings = ServiceOffering.list(
            self.domainadmin_api_client,
            isrecursive=True
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            len(self.service_offerings) + 1,
            "List ServiceOfferings response is empty"
        )

        # List all service offerings with isrecursive False
        service_offerings = ServiceOffering.list(
            self.domainadmin_api_client,
            isrecursive=False
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertGreaterEqual(
            len(service_offerings),
            len(self.service_offerings),
            "List ServiceOfferings response is empty"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_08_list_service_offerings_issystem_filter(self):
        """Test list service offerings with issystem filter
        """
        # List all service offerings with issystem True
        service_offerings = ServiceOffering.list(
            self.apiclient,
            issystem=True
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            len(self.system_service_offerings) + 1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertTrue(
                service_offering.issystem,
                "List ServiceOfferings response has incorrect issystem"
            )

        # List all service offerings with issystem False
        service_offerings = ServiceOffering.list(
            self.apiclient,
            issystem=False
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            len(self.service_offerings) + 1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertFalse(
                service_offering.issystem,
                "List ServiceOfferings response has incorrect issystem"
            )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_09_list_service_offerings_keyword_filter(self):
        """Test list service offerings with keyword filter
        """
        # List all service offerings with keyword
        service_offerings = ServiceOffering.list(
            self.apiclient,
            keyword=self.system_offering.name
        )
        self.assertIsNone(
            service_offerings,
            "List ServiceOfferings response is not None"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            keyword=self.service_offering_1.name
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        self.assertEqual(
            service_offerings[0].name,
            self.service_offering_1.name,
            "List ServiceOfferings response has incorrect name"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            keyword="invalid"
        )
        self.assertIsNone(service_offerings, "List ServiceOfferings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_10_list_service_offerings_name_filter(self):
        """Test list service offerings with name filter
        """
        # List all service offerings with name
        service_offerings = ServiceOffering.list(
            self.apiclient,
            name=self.system_offering.name
        )
        self.assertIsNone(
            service_offerings,
            "List ServiceOfferings response is not None"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            name=self.system_offering.name,
            issystem=True
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        self.assertEqual(
            service_offerings[0].name,
            self.system_offering.name,
            "List ServiceOfferings response has incorrect name"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            name=self.service_offering_1.name
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        self.assertEqual(
            service_offerings[0].name,
            self.service_offering_1.name,
            "List ServiceOfferings response has incorrect name"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            name="invalid"
        )
        self.assertIsNone(service_offerings, "List ServiceOfferings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_11_list_service_offerings_systemvmtype_filter(self):
        """Test list service offerings with systemvmtype filter
        """
        # List all service offerings with systemvmtype domainrouter
        service_offerings = ServiceOffering.list(
            self.apiclient,
            systemvmtype="domainrouter"
        )
        self.assertIsNone(
            service_offerings,
            "List ServiceOfferings response is not None"
        )

        service_offerings = ServiceOffering.list(
            self.apiclient,
            systemvmtype="domainrouter",
            issystem=True
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertGreaterEqual(
            len(service_offerings),
            1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertEqual(
                service_offering.systemvmtype,
                "domainrouter",
                "List ServiceOfferings response has incorrect systemvmtype"
            )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_12_list_service_offerings_zoneid_filter(self):
        """Test list service offerings with zoneid filter
        """
        service_offerings = ServiceOffering.list(
            self.apiclient,
            zoneid=self.zone.id
        )
        self.assertTrue(
            isinstance(service_offerings, list),
            "List ServiceOfferings response is not a valid list"
        )
        self.assertEqual(
            len(service_offerings),
            len(self.service_offerings) + 1,
            "List ServiceOfferings response is empty"
        )
        for service_offering in service_offerings:
            self.assertTrue(
                service_offering.zoneid is None or service_offering.zoneid == self.zone.id,
                "List ServiceOfferings response has incorrect zoneid"
            )

        if len(self.zones) > 1:
            service_offerings = ServiceOffering.list(
                self.apiclient,
                zoneid=self.zones[1].id
            )
            if service_offerings is not None:
                self.assertTrue(
                    isinstance(service_offerings, list),
                    "List ServiceOfferings response is not a valid list"
                )
                self.assertEqual(
                    len(service_offerings),
                    len(self.service_offerings),
                    "List ServiceOfferings response is empty"
                )
