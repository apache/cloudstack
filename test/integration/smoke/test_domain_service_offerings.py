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
""" BVT tests for Service offerings"""

# Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import updateServiceOffering
from marvin.lib.utils import (isAlmostEqual,
                              cleanup_resources,
                              random_gen)
from marvin.lib.base import (Domain,
                             ServiceOffering,
                             Account,
                             VirtualMachine)
from marvin.lib.common import (list_service_offering,
                               list_virtual_machines,
                               get_domain,
                               get_zone,
                               get_test_template,
                               list_hosts)
from nose.plugins.attrib import attr

import time
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.decoratorGenerators import skipTestIf

_multiprocess_shared_ = True


class TestCreateDomainsServiceOffering(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.services = self.testClient.getParsedTestDataConfig()

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCreateDomainsServiceOffering, cls).getClsTestClient()
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
                TestCreateDomainsServiceOffering,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_01_create_domain_service_offering(self):
        """Test to create service offering for specified domain"""

        # Validate the following:
        # 1. createServiceOfferings should return a valid information
        #    for newly created offering
        # 2. The Cloud Database contains the valid information
        offering_data_domainid = "{0},{1}".format(self.domain_11.id, self.domain_2.id)

        service_offering = ServiceOffering.create(
                                self.apiclient,
                                self.services["service_offerings"]["tiny"],
                                domainid=offering_data_domainid
                            )
        self.cleanup.append(service_offering)

        self.debug(
            "Created service offering with ID: %s" %
            service_offering.id)

        list_service_response = list_service_offering(
            self.apiclient,
            id=service_offering.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is created"
        )

        self.assertEqual(
            list_service_response[0].cpunumber,
            self.services["service_offerings"]["tiny"]["cpunumber"],
            "Check cpunumber in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].cpuspeed,
            self.services["service_offerings"]["tiny"]["cpuspeed"],
            "Check cpuspeed in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].displaytext,
            self.services["service_offerings"]["tiny"]["displaytext"],
            "Check server displaytext in createServiceOfferings"
        )
        self.assertEqual(
            list_service_response[0].memory,
            self.services["service_offerings"]["tiny"]["memory"],
            "Check memory in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].name,
            self.services["service_offerings"]["tiny"]["name"],
            "Check name in createServiceOffering"
        )
        self.assertCountEqual(
            list_service_response[0].domainid.split(","),
            offering_data_domainid.split(","),
            "Check domainid in createServiceOffering"
        )
        return


class TestDomainsServiceOfferings(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDomainsServiceOfferings, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
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
        cls.services['mode'] = cls.zone.networktype

        cls.service_offering = ServiceOffering.create(
                                    cls.apiclient,
                                    cls.services["service_offerings"]["tiny"],
                                    domainid=cls.domain_1.id
                                )
        template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_test_template() failed to return template"

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls._cleanup = [
            cls.service_offering,
            cls.domain_11,
            cls.domain_1,
            cls.domain_2,
            cls.domain_3
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestDomainsServiceOfferings,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_02_edit_domain_service_offering(self):
        """Test to update existing service offering"""

        # Validate the following:
        # 1. updateServiceOffering should return a valid information for the updated offering
        # 2. updateServiceOffering should fail when trying to add child domain but parent domain is
        #    also passed
        # 3. updateServiceOffering should be  able to add new domain to the offering
        self.debug("Updating service offering with ID: %s" %
                   self.service_offering.id)

        cmd = updateServiceOffering.updateServiceOfferingCmd()
        # Add parameters for API call
        cmd.id = self.service_offering.id
        input_domainid ="{0},{1},{2}".format(self.domain_1.id, self.domain_11.id, self.domain_2.id)
        result_domainid = "{0},{1}".format(self.domain_1.id, self.domain_2.id)
        cmd.domainid = input_domainid
        self.apiclient.updateServiceOffering(cmd)

        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is updated"
        )

        try:
            self.assertCountEqual(
                list_service_response[0].domainid.split(","),
                input_domainid.split(","),
                "Check child domainid in updateServiceOffering, should fail"
            )
            self.fail("Child domain added to offering when parent domain already exist. Must be an error.")
        except AssertionError:
            self.debug("Child domain check successful")

        self.assertCountEqual(
            list_service_response[0].domainid.split(","),
            result_domainid.split(","),
            "Check domainid in updateServiceOffering"
        )

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_03_deploy_vm_domain_service_offering(self):
        """Test to deploying VM for an existing domain specified service offering"""

        # Validate the following:
        # 1. VM deployment should fail for the user from a different domain
        # 2. VM deployment should work for users from domains for which offering is specified
        self.debug("Deploying VM using service offering with ID: %s" %
                   self.service_offering.id)

        self.invalid_account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_3.id
        )
        self.cleanup.append(self.invalid_account)

        try:
            VirtualMachine.create(
                self.apiclient,
                self.services["small"],
                accountid=self.invalid_account.name,
                domainid=self.invalid_account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.services["mode"]
            )
            self.fail("VM deployed for a user from domain which has not been specified for service offering. Must be an error.")
        except CloudstackAPIException:
            self.debug("VM deployment for invalid user check")

        self.valid_account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_1.id
        )
        self.cleanup.append(self.valid_account_1)
        self.vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.valid_account_1.name,
            domainid=self.valid_account_1.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.debug("VM deployed for first subdomain %s" % self.valid_account_1.domainid)

        self.valid_account_2 = Account.create(
            self.apiclient,
            self.services["account2"],
            domainid=self.domain_2.id
        )
        self.cleanup.append(self.valid_account_2)
        self.vm_2 = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.valid_account_2.name,
            domainid=self.valid_account_2.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.debug("VM deployed for second subdomain %s" % self.valid_account_2.domainid)

        self.valid_account_3 = Account.create(
            self.apiclient,
            self.services["user"],
            domainid=self.domain_11.id
        )
        self.cleanup.append(self.valid_account_3)
        self.vm_3 = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.valid_account_3.name,
            domainid=self.valid_account_3.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.debug("VM deployed for first child subdomain %s" % self.valid_account_3.domainid)

        return
