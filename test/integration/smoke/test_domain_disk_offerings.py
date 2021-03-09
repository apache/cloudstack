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
""" BVT tests for Disk offerings"""

#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import updateDiskOffering
from marvin.lib.utils import (isAlmostEqual,
                              cleanup_resources,
                              random_gen)
from marvin.lib.base import (Domain,
                             DiskOffering,
                             Account,
                             Volume)
from marvin.lib.common import (list_disk_offering,
                               get_domain,
                               get_zone,
                               list_hosts)
from nose.plugins.attrib import attr

import time
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.decoratorGenerators import skipTestIf


_multiprocess_shared_ = True

class TestCreateDomainsDiskOffering(cloudstackTestCase):

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCreateDomainsDiskOffering, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        # Create domains
        cls.domain_1 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls.domain_11 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain11"],
            parentdomainid=cls.domain_1.id
        )
        cls.domain_2 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain2"]
        )
        cls._cleanup = [
            cls.domain_11,
            cls.domain_1,
            cls.domain_2
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestCreateDomainsDiskOffering,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_01_create_disk_offering(self):
        """Test to create disk offering

        # Validate the following:
        # 1. createDiskOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information
        """
        offering_data_domainid = "{0},{1}".format(self.domain_11.id, self.domain_2.id)

        disk_offering = DiskOffering.create(
                            self.apiclient,
                            self.services["disk_offering"],
                            domainid=offering_data_domainid
                        )
        self.cleanup.append(disk_offering)

        self.debug("Created Disk offering with ID: %s" % disk_offering.id)

        list_disk_response = list_disk_offering(
                                    self.apiclient,
                                    id=disk_offering.id
                                )
        self.assertEqual(
            isinstance(list_disk_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_disk_response),
            0,
            "Check Disk offering is created"
        )
        disk_response = list_disk_response[0]

        self.assertEqual(
            disk_response.displaytext,
            self.services["disk_offering"]["displaytext"],
            "Check server displaytext in createDiskOffering"
        )
        self.assertEqual(
            disk_response.name,
            self.services["disk_offering"]["name"],
            "Check name in createDiskOffering"
        )
        self.assertCountEqual(
            disk_response.domainid.split(","),
            offering_data_domainid.split(","),
            "Check domainid in createDiskOffering"
        )
        return

class TestDomainsDiskOfferings(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):

        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDomainsDiskOfferings, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Create domains
        cls.domain_1 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls.domain_11 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain11"],
            parentdomainid=cls.domain_1.id
        )
        cls.domain_2 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain2"]
        )
        cls.domain_3 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain12"]
        )

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"],
                                    domainid=cls.domain_1.id
                                )
        cls._cleanup = [
            cls.disk_offering,
            cls.domain_11,
            cls.domain_1,
            cls.domain_2,
            cls.domain_3
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(TestDomainsDiskOfferings, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns",  "smoke"], required_hardware="false")
    def test_02_edit_disk_offering(self):
        """Test to update existing disk offering"""

        # 1. updateDiskOffering should return a valid information for the updated offering
        # 2. updateDiskOffering should fail when trying to add child domain but parent domain is
        #    also passed
        # 3. updateDiskOffering should be able to add new domain to the offering
        self.debug("Updating disk offering with ID: %s" %
                   self.disk_offering.id)

        cmd = updateDiskOffering.updateDiskOfferingCmd()
        # Add parameters for API call
        cmd.id = self.disk_offering.id
        input_domainid ="{0},{1},{2}".format(self.domain_1.id, self.domain_11.id, self.domain_2.id)
        result_domainid = "{0},{1}".format(self.domain_1.id, self.domain_2.id)
        cmd.domainid = input_domainid
        self.apiclient.updateDiskOffering(cmd)

        list_disk_response = list_disk_offering(
            self.apiclient,
            id=self.disk_offering.id
        )
        self.assertEqual(
            isinstance(list_disk_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_disk_response),
            0,
            "Check Disk offering is updated"
        )

        try:
            self.assertCountEqual(
                list_disk_response[0].domainid.split(","),
                input_domainid.split(","),
                "Check child domainid in updateDiskOffering, should fail"
            )
            self.fail("Child domain added to offering when parent domain already exist. Must be an error.")
        except AssertionError:
            self.debug("Child domain check successful")

        self.assertCountEqual(
            list_disk_response[0].domainid.split(","),
            result_domainid.split(","),
            "Check domainid in updateDiskOffering"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_03_create_volume_domain_disk_offering(self):
        """Test to creating volume for an existing domain specified disk offering"""

        # Validate the following:
        # 1. Volume creation should fail for the user from a different domain
        # 2. Volume creation should work for users from domains for which offering is specified
        self.debug("Deploying VM using disk offering with ID: %s" %
                   self.disk_offering.id)

        self.invalid_account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_3.id
        )
        self.cleanup.append(self.invalid_account)

        try:
            Volume.create(
                self.apiclient,
                self.services["volume"],
                zoneid=self.zone.id,
                account=self.invalid_account.name,
                domainid=self.invalid_account.domainid,
                diskofferingid=self.disk_offering.id
            )
            self.fail("Volume created for a user from domain which has not been specified for service offering. Must be an error.")
        except CloudstackAPIException:
            self.debug("Volume creation for invalid user check")

        self.valid_account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_1.id
        )
        self.cleanup.append(self.valid_account_1)
        Volume.create(
            self.apiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.valid_account_1.name,
            domainid=self.valid_account_1.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.debug("Volume created for first subdomain %s" % self.valid_account_1.domainid)

        self.valid_account_2 = Account.create(
            self.apiclient,
            self.services["account2"],
            domainid=self.domain_2.id
        )
        self.cleanup.append(self.valid_account_2)
        Volume.create(
            self.apiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.valid_account_2.name,
            domainid=self.valid_account_2.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.debug("Volume created for second subdomain %s" % self.valid_account_2.domainid)

        self.valid_account_3 = Account.create(
            self.apiclient,
            self.services["user"],
            domainid=self.domain_11.id
        )
        self.cleanup.append(self.valid_account_3)
        Volume.create(
            self.apiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.valid_account_3.name,
            domainid=self.valid_account_3.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.debug("Volume created for first child subdomain %s" % self.valid_account_3.domainid)

        return
