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
""" BVT tests for Network offerings"""

#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (listNetworkOfferings,
                                  updateNetworkOffering)
from marvin.lib.utils import (isAlmostEqual,
                              cleanup_resources,
                              random_gen)
from marvin.lib.base import (Domain,
                             NetworkOffering,
                             Account,
                             Network)
from marvin.lib.common import (get_domain,
                               get_zone,
                               list_hosts)
from nose.plugins.attrib import attr

import time
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.decoratorGenerators import skipTestIf


_multiprocess_shared_ = True

class TestCreateDomainsNetworkOffering(cloudstackTestCase):

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
        testClient = super(TestCreateDomainsNetworkOffering, cls).getClsTestClient()
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
                TestCreateDomainsNetworkOffering,
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
    def test_01_create_network_offering(self):
        """Test to create network offering

        # Validate the following:
        # 1. createNetworkOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information
        """
        offering_data_domainid = "{0},{1}".format(self.domain_11.id, self.domain_2.id)

        network_offering = NetworkOffering.create(
                            self.apiclient,
                            self.services["network_offering"],
                            domainid=offering_data_domainid
                        )
        self.cleanup.append(network_offering)

        self.debug("Created Network offering with ID: %s" % network_offering.id)

        cmd = listNetworkOfferings.listNetworkOfferingsCmd()
        cmd.id = network_offering.id
        list_network_response = self.apiclient.listNetworkOfferings(cmd)
        self.assertEqual(
            isinstance(list_network_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_network_response),
            0,
            "Check Network offering is created"
        )
        network_response = list_network_response[0]

        self.assertEqual(
            network_response.id,
            network_offering.id,
            "Check server id in createNetworkOffering"
        )
        self.assertCountEqual(
            network_response.domainid.split(","),
            offering_data_domainid.split(","),
            "Check domainid in createNetworkOffering"
        )
        return

class TestDomainsNetworkOfferings(cloudstackTestCase):

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
        testClient = super(TestDomainsNetworkOfferings, cls).getClsTestClient()
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

        cls.network_offering = NetworkOffering.create(
                                    cls.apiclient,
                                    cls.services["network_offering"],
                                    domainid=cls.domain_1.id
                                )
        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')
        cls._cleanup = [
            cls.network_offering,
            cls.domain_11,
            cls.domain_1,
            cls.domain_2,
            cls.domain_3
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(TestDomainsNetworkOfferings, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns",  "smoke"], required_hardware="false")
    def test_02_edit_network_offering(self):
        """Test to update existing network offering"""

        # 1. updateNetworkOffering should return a valid information for the updated offering
        # 2. updateNetworkOffering should fail while trying to add child domain but parent domain
        #    is also passed
        # 3. updateNetworkOffering should be able to add new domain to the offering
        self.debug("Updating network offering with ID: %s" %
                   self.network_offering.id)

        cmd = updateNetworkOffering.updateNetworkOfferingCmd()
        # Add parameters for API call
        cmd.id = self.network_offering.id
        input_domainid ="{0},{1},{2}".format(self.domain_1.id, self.domain_11.id, self.domain_2.id)
        result_domainid = "{0},{1}".format(self.domain_1.id, self.domain_2.id)
        cmd.domainid = input_domainid
        self.apiclient.updateNetworkOffering(cmd)

        cmd = listNetworkOfferings.listNetworkOfferingsCmd()
        cmd.id = self.network_offering.id
        list_network_response = self.apiclient.listNetworkOfferings(cmd)
        self.assertEqual(
            isinstance(list_network_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_network_response),
            0,
            "Check Network offering is updated"
        )

        try:
            self.assertCountEqual(
                list_network_response[0].domainid.split(","),
                input_domainid.split(","),
                "Check child domainid in updateServiceOffering, should fail"
            )
            self.fail("Child domain added to offering when parent domain already exist. Must be an error.")
        except AssertionError:
            self.debug("Child domain check successful")

        domainid = "{0},{1}".format(self.domain_1.id, self.domain_2.id)
        self.assertCountEqual(
            list_network_response[0].domainid.split(","),
            result_domainid.split(","),
            "Check domainid in createNetworkOffering"
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
    def test_03_create_network_domain_network_offering(self):
        """Test to creating network for an existing domain specified network offering"""

        # Validate the following:
        # 1. Network creation should fail for the user from a different domain
        # 2. Network creation should work for users from domains for which offering is specified
        self.debug("Deploying VM using network offering with ID: %s" %
                   self.network_offering.id)

        self.invalid_account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_3.id
        )
        self.cleanup.append(self.invalid_account)

        try:
            Network.create(
                self.apiclient,
                self.services["network"],
                self.invalid_account.name,
                self.invalid_account.domainid,
                zoneid=self.zone.id,
                networkofferingid=self.network_offering.id
            )
            self.fail("Network created for a user from domain which has not been specified for service offering. Must be an error.")
        except CloudstackAPIException:
            self.debug("Network creation for invalid user check")

        self.valid_account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_1.id
        )
        self.cleanup.append(self.valid_account_1)
        Network.create(
            self.apiclient,
            self.services["network"],
            self.valid_account_1.name,
            self.valid_account_1.domainid,
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )
        self.debug("Network created for first subdomain %s" % self.valid_account_1.domainid)

        self.valid_account_2 = Account.create(
            self.apiclient,
            self.services["account2"],
            domainid=self.domain_2.id
        )
        self.cleanup.append(self.valid_account_2)
        Network.create(
            self.apiclient,
            self.services["network"],
            self.valid_account_2.name,
            self.valid_account_2.domainid,
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )
        self.debug("Network created for second subdomain %s" % self.valid_account_2.domainid)

        self.valid_account_3 = Account.create(
            self.apiclient,
            self.services["user"],
            domainid=self.domain_11.id
        )
        self.cleanup.append(self.valid_account_3)
        Network.create(
            self.apiclient,
            self.services["network"],
            self.valid_account_3.name,
            self.valid_account_3.domainid,
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )
        self.debug("Network created for first child subdomain %s" % self.valid_account_3.domainid)

        return
