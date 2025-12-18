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
""" Tests for API listing of disk offerings with different filters
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


class TestListDiskOfferings(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestListDiskOfferings, cls).getClsTestClient()
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
        cls.disk_offerings = DiskOffering.list(cls.apiclient, listall=True)

        cls.disk_offering = DiskOffering.create(cls.apiclient,
                                                cls.services["disk_offering"],
                                                domainid=cls.domain.id)
        cls._cleanup.append(cls.disk_offering)

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

        cls.disk_offering_child_domain = DiskOffering.create(cls.apiclient,
                                                             cls.services["disk_offering"],
                                                             domainid=cls.child_domain_1.id,
                                                             zoneid=cls.zone.id,
                                                             encrypt=True)
        cls._cleanup.append(cls.disk_offering_child_domain)

    @classmethod
    def tearDownClass(cls):
        super(TestListDiskOfferings, cls).tearDownClass()

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_list_disk_offerings_id_filter(self):
        """ Test list disk offerings with id filter
        """
        # List all disk offerings
        disk_offerings = DiskOffering.list(self.apiclient, id=self.disk_offering.id)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings),
            1,
            "List disk offerings response has incorrect length"
        )
        # Verify the id of the disk offering returned is the same as the one requested
        self.assertEqual(
            disk_offerings[0].id,
            self.disk_offering.id,
            "List disk offerings should return the disk offering requested"
        )

        disk_offerings = DiskOffering.list(self.apiclient, id=-1)
        self.assertIsNone(disk_offerings, "List disk offerings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_disk_offerings_name_filter(self):
        """ Test list disk offerings with name filter
        """
        disk_offerings = DiskOffering.list(self.apiclient, name=self.services["disk_offering"]["name"])
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings),
            2,
            "List disk offerings response has incorrect length"
        )
        # Verify the name of the disk offering returned is the same as the one requested
        self.assertEqual(
            disk_offerings[0].name,
            self.services["disk_offering"]["name"],
            "List disk offerings should return the disk offering requested"
        )
        self.assertEqual(
            disk_offerings[1].name,
            self.services["disk_offering"]["name"],
            "List disk offerings should return the disk offering requested"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_disk_offerings_zoneid_filter(self):
        """ Test list disk offerings with zoneid filter
        """
        disk_offerings_zone_1 = DiskOffering.list(self.apiclient, zoneid=self.zone.id)
        self.assertTrue(
            isinstance(disk_offerings_zone_1, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings_zone_1) - len(self.disk_offerings),
            2,
            "List disk offerings response has incorrect length"
        )

        for disk_offering in disk_offerings_zone_1:
            self.assertTrue(
                disk_offering.zoneid is None or disk_offering.zoneid == self.zone.id,
                "List disk offerings should return the disk offering requested"
            )

        if len(self.zones) > 1:
            disk_offerings_zone_2 = DiskOffering.list(self.apiclient, zoneid=self.zones[1].id)
            self.assertTrue(
                isinstance(disk_offerings_zone_2, list),
                "List disk offerings response is not a valid list"
            )
            for disk_offering in disk_offerings_zone_2:
                self.assertTrue(
                    disk_offering.zoneid is None or disk_offering.zoneid == self.zones[1].id,
                    "List disk offerings should return the disk offering requested"
                )

            self.assertEqual(len(disk_offerings_zone_1) - len(disk_offerings_zone_2), 1)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_list_disk_offerings_domainid_filter(self):
        """ Test list disk offerings with domainid filter
        """
        disk_offerings = DiskOffering.list(self.apiclient, domainid=self.domain.id)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings),
            1,
            "List disk offerings response has incorrect length"
        )
        self.assertEqual(
            disk_offerings[0].domainid,
            self.domain.id,
            "List disk offerings should return the disk offering requested"
        )

        disk_offerings = DiskOffering.list(self.apiclient, domainid=self.child_domain_1.id)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings),
            1,
            "List disk offerings response has incorrect length"
        )
        self.assertEqual(
            disk_offerings[0].domainid,
            self.child_domain_1.id,
            "List disk offerings should return the disk offering requested"
        )

        disk_offerings = DiskOffering.list(self.apiclient, domainid=-1)
        self.assertIsNone(disk_offerings, "List disk offerings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_list_disk_offerings_encrypted_filter(self):
        """ Test list disk offerings with encrypted filter
        """
        disk_offerings = DiskOffering.list(self.apiclient, encrypt=True)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )

        self.assertEqual(
            len(disk_offerings),
            1,
            "List disk offerings response has incorrect length"
        )
        self.assertTrue(
            disk_offerings[0].encrypt,
            "List disk offerings should return the disk offering requested"
        )

        disk_offerings = DiskOffering.list(self.apiclient, encrypt=False)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings) - len(self.disk_offerings),
            1,
            "List disk offerings response has incorrect length"
        )
        for disk_offering in disk_offerings:
            self.assertFalse(
                disk_offering.encrypt,
                "List disk offerings should return the disk offering requested"
            )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_06_list_disk_offerings_keyword_filter(self):
        """ Test list disk offerings with keyword filter
        """
        disk_offerings = DiskOffering.list(self.apiclient, keyword=self.disk_offering.name)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings),
            2,
            "List disk offerings response has incorrect length"
        )
        self.assertEqual(
            disk_offerings[0].name,
            self.disk_offering.name,
            "List disk offerings should return the disk offering requested"
        )
        self.assertEqual(
            disk_offerings[1].name,
            self.disk_offering.name,
            "List disk offerings should return the disk offering requested"
        )

        disk_offerings = DiskOffering.list(self.apiclient, keyword="random")
        self.assertIsNone(disk_offerings, "List disk offerings response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_07_list_disk_offering_isrecursive_filter(self):
        """ Test list disk offerings with isrecursive parameter
        """
        disk_offerings = DiskOffering.list(self.domainadmin_api_client, isrecursive=True)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings) - len(self.disk_offerings),
            2,
            "List disk offerings response has incorrect length"
        )

        disk_offerings = DiskOffering.list(self.domainadmin_api_client, isrecursive=False)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings) - len(self.disk_offerings),
            1,
            "List disk offerings response has incorrect length"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_08_list_disk_offering_no_filter(self):
        """ Test list disk offerings with no filters
        """
        disk_offerings = DiskOffering.list(self.apiclient)
        self.assertTrue(
            isinstance(disk_offerings, list),
            "List disk offerings response is not a valid list"
        )
        self.assertEqual(
            len(disk_offerings) - len(self.disk_offerings),
            2,
            "List disk offerings response has incorrect length"
        )
