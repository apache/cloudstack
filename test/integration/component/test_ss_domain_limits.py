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

""" P1 tests for secondary storage domain limits

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domain+or+accounts

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-1466

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domains+and+accounts
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Resources,
                             Domain,
                             Template)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_builtin_template_info,
                               list_zones,
                               isDomainResourceCountEqualToExpectedCount)
from marvin.lib.utils import (cleanup_resources, validateList)
from marvin.codes import (PASS,
                          FAIL,
                          RESOURCE_SECONDARY_STORAGE)

class TestMultipleChildDomain(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestMultipleChildDomain,
                               cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        # Fill services from the external config file
        cls.services = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cloudstackTestClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def updateDomainResourceLimits(self, parentdomainlimit, subdomainlimit):
        """Update secondary storage limits of the parent domain and its
        child domains"""

        try:
            #Update resource limit for domain
            Resources.updateLimit(self.apiclient, resourcetype=11,
                              max=parentdomainlimit,
                              domainid=self.parent_domain.id)

            # Update Resource limit for sub-domains
            Resources.updateLimit(self.apiclient, resourcetype=11,
                              max=subdomainlimit,
                              domainid=self.cadmin_1.domainid)

            Resources.updateLimit(self.apiclient, resourcetype=11,
                              max=subdomainlimit,
                              domainid=self.cadmin_2.domainid)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    def setupAccounts(self):
        try:
            self.parent_domain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)
            self.parentd_admin = Account.create(self.apiclient, self.services["account"],
                                            admin=True, domainid=self.parent_domain.id)

            # Create sub-domains and their admin accounts
            self.cdomain_1 = Domain.create(self.apiclient,
                                       services=self.services["domain"],
                                       parentdomainid=self.parent_domain.id)
            self.cdomain_2 = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.parent_domain.id)

            self.cadmin_1 = Account.create(self.apiclient, self.services["account"],
                                       admin=True, domainid=self.cdomain_1.id)

            self.cadmin_2 = Account.create(self.apiclient, self.services["account"],
                                       admin=True, domainid=self.cdomain_2.id)

            # Cleanup the resources created at end of test
            self.cleanup.append(self.cadmin_1)
            self.cleanup.append(self.cadmin_2)
            self.cleanup.append(self.cdomain_1)
            self.cleanup.append(self.cdomain_2)
            self.cleanup.append(self.parentd_admin)
            self.cleanup.append(self.parent_domain)

            users = {
                 self.cdomain_1: self.cadmin_1,
                 self.cdomain_2: self.cadmin_2
                 }
        except Exception as e:
            return [FAIL, e, None]
        return [PASS, None, users]

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_multiple_domains_secondary_storage_limits(self):
        """Test secondary storage limit of domain and its sub-domains

        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        2. Register template in child domain 1 so that total secondary storage
           is less than the limit of child domain
        3. Set the child domain limit equal to template size and parent domain
           domain limit as double of the template size
        4. Repeat step 2 for child domain 2
        5. Try to register template in parent domain now so that the total secondary storage in
           parent domain (including that in sub-domains is more than the secondary
           storage limit of the parent domain)
        6. Delete the admin account of child domain 1 and check resource count
           of the parent domain
        7. Delete template in account 2 and check secondary storage count
           of parent domain

        # Validations:
        1. Step 2 and 4 should succeed
        2. Step 5 should fail as the resource limit exceeds in parent domain
        3. After step 6, resource count in parent domain should decrease by equivalent
           quantity
        4. After step 7, resource count in parent domain should be 0"""

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])

        try:
            builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
            self.services["template_2"]["url"] = builtin_info[0]
            self.services["template_2"]["hypervisor"] = builtin_info[1]
            self.services["template_2"]["format"] = builtin_info[2]

            templateChildAccount1 = Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.cadmin_1.name,
                                     domainid=self.cadmin_1.domainid)

            templateChildAccount1.download(self.apiclient)

            templates = Template.list(self.apiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=templateChildAccount1.id)
            if validateList(templates)[0] == FAIL:
                raise Exception("templates list validation failed")

            self.templateSize = int(int(templates[0].size) / (1024**3))
        except Exception as e:
            self.fail("Failed with exception as ee: %s" % e)

        subdomainlimit = (self.templateSize)

        result = self.updateDomainResourceLimits(((subdomainlimit*2)), subdomainlimit)
        self.assertEqual(result[0], PASS, result[1])

        # Checking Primary Storage count of Parent domain admin before deleting child domain user account
        result = isDomainResourceCountEqualToExpectedCount(
                        self.apiclient, self.cadmin_1.domainid,
                        self.templateSize, RESOURCE_SECONDARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")

        try:
            templateChildAccount2 = Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.cadmin_2.name,
                                     domainid=self.cadmin_2.domainid)

            templateChildAccount2.download(self.apiclient)
        except Exception as e:
            self.fail("Failed while registering/downloading template: %s" % e)

        result = isDomainResourceCountEqualToExpectedCount(
                        self.apiclient, self.cadmin_2.domainid,
                        self.templateSize, RESOURCE_SECONDARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")

        with self.assertRaises(Exception):
            Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.parentd_admin.name,
                                     domainid=self.parentd_admin.domainid)

        self.cadmin_1.delete(self.apiclient)
        self.cleanup.remove(self.cadmin_1)

        result = isDomainResourceCountEqualToExpectedCount(
                        self.apiclient, self.parent_domain.id,
                        self.templateSize, RESOURCE_SECONDARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")

        try:
            templateChildAccount2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete template: %s" % e)

        result = isDomainResourceCountEqualToExpectedCount(
                        self.apiclient, self.parent_domain.id,
                        0, RESOURCE_SECONDARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_multiple_domains_secondary_storage_counts(self):
        """Test secondary storage counts in multiple child domains
        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        Repeat following steps for both the child domains
        2. Register template in child domain
        3. Check if the resource count for domain is updated correctly
        4. Delete the template
        5. Verify that the resource count for the domain is 0

        """
        users = None
        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])
        users = result[2]

        for domain, admin in users.items():
            self.account = admin
            self.domain = domain

            try:
                builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
                self.services["template_2"]["url"] = builtin_info[0]
                self.services["template_2"]["hypervisor"] = builtin_info[1]
                self.services["template_2"]["format"] = builtin_info[2]

                template = Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.account.name,
                                     domainid=self.account.domainid)

                template.download(self.apiclient)

                templates = Template.list(self.apiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=template.id)
                if validateList(templates)[0] == FAIL:
                    raise Exception("templates list validation failed")

                templateSize = int(int(templates[0].size) / (1024**3))
                expectedCount = templateSize
                result = isDomainResourceCountEqualToExpectedCount(
                                    self.apiclient, self.domain.id,
                                    expectedCount, RESOURCE_SECONDARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                template.delete(self.apiclient)

                expectedCount = 0
                result = isDomainResourceCountEqualToExpectedCount(
                                    self.apiclient, self.domain.id,
                                    expectedCount, RESOURCE_SECONDARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")
            except Exception as e:
                self.fail("Failed to get zone list: %s" % e)
	    return

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_copy_template(self):
        """Test secondary storage counts in multiple child domains
        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        Repeat following steps for both the child domains
        2. Register template in child domain
        3. Check if the resource count for domain is updated correctly
        4. Copy template to other zone
        5. Verify that secondary storage count for the domain is doubled
           as there are two templates now
        """

        zones = list_zones(self.apiclient)
        self.assertEqual(validateList(zones)[0], PASS, "zones list validation faield")

        if len(zones) < 2:
            self.skipTest("At least 2 zones should be present for this test case")

        users = None
        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])
        users = result[2]

        for domain, admin in users.items():
            self.account = admin
            self.domain = domain

            try:
                builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
                self.services["template_2"]["url"] = builtin_info[0]
                self.services["template_2"]["hypervisor"] = builtin_info[1]
                self.services["template_2"]["format"] = builtin_info[2]

                template = Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.account.name,
                                     domainid=self.account.domainid)

                template.download(self.apiclient)

                templates = Template.list(self.apiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=template.id)
                if validateList(templates)[0] == FAIL:
                    raise Exception("templates list validation failed")

                templateSize = int(int(templates[0].size) / (1024**3))
                expectedCount = templateSize
                result = isDomainResourceCountEqualToExpectedCount(
                                    self.apiclient, self.domain.id,
                                    expectedCount, RESOURCE_SECONDARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                templateDestinationZoneId = None
                for zone in zones:
                    if template.zoneid != zone.id :
                        templateDestinationZoneId = zone.id
                        break

                template.copy(self.apiclient, destzoneid=templateDestinationZoneId,
                              sourcezoneid = template.zoneid)

                expectedCount *= 2
                result = isDomainResourceCountEqualToExpectedCount(
                                    self.apiclient, self.domain.id,
                                    expectedCount, RESOURCE_SECONDARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")
            except Exception as e:
                self.fail("Failed to get zone list: %s" % e)
	    return

class TestDeleteAccount(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestDeleteAccount,
                               cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        # Fill services from the external config file
        cls.services = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client)
        cls.services["mode"] = cls.zone.networktype
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setupAccounts(self):
        try:
            self.parent_domain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)
            self.parentd_admin = Account.create(self.apiclient, self.services["account"],
                                            admin=True, domainid=self.parent_domain.id)

            # Create sub-domains and their admin accounts
            self.cdomain_1 = Domain.create(self.apiclient,
                                       services=self.services["domain"],
                                       parentdomainid=self.parent_domain.id)
            self.cdomain_2 = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.parent_domain.id)

            self.cadmin_1 = Account.create(self.apiclient, self.services["account"],
                                       admin=True, domainid=self.cdomain_1.id)

            self.cadmin_2 = Account.create(self.apiclient, self.services["account"],
                                       admin=True, domainid=self.cdomain_2.id)

            # Cleanup the resources created at end of test
            self.cleanup.append(self.cadmin_2)
            self.cleanup.append(self.cdomain_1)
            self.cleanup.append(self.cdomain_2)
            self.cleanup.append(self.parentd_admin)
            self.cleanup.append(self.parent_domain)

            users = {
                 self.cdomain_1: self.cadmin_1,
                 self.cdomain_2: self.cadmin_2
                 }
        except Exception as e:
            return [FAIL, e, None]
        return [PASS, None, users]

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_create_template_delete_account(self):
        """Test secondary storage limit of domain and its sub-domains

        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        2. Register template in child domain 1
        3. Verify that the secondary storage count for child domain 1 equals
           the template size
        4. Register template in child domain 2
        5. Vreify that the seconday storage count for child domain 2 equals
           the template size
        6. Verify that the secondary storage count for parent domain equals
           double of template size
        7. Delete child domain 1 admin account
        8. Verify that secondary storage count for parent domain now equals
           to only 1 template size
        """

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])

        try:
            builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
            self.services["template_2"]["url"] = builtin_info[0]
            self.services["template_2"]["hypervisor"] = builtin_info[1]
            self.services["template_2"]["format"] = builtin_info[2]

            template = Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.cadmin_1.name,
                                     domainid=self.cadmin_1.domainid)

            template.download(self.apiclient)

            templates = Template.list(self.apiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=template.id)
            if validateList(templates)[0] == FAIL:
                raise Exception("templates list validation failed")

            self.templateSize = int(int(templates[0].size) / (1024**3))
        except Exception as e:
            self.fail("Failed with exception as ee: %s" % e)

        # Checking Primary Storage count of Parent domain admin before deleting child domain user account
        expectedCount = self.templateSize
        result = isDomainResourceCountEqualToExpectedCount(
                                    self.apiclient, self.cadmin_1.domainid,
                                    expectedCount, RESOURCE_SECONDARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")

        try:
            template = Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.cadmin_2.name,
                                     domainid=self.cadmin_2.domainid)

            template.download(self.apiclient)
        except Exception as e:
            self.fail("Failed while registering/downloading template: %s" % e)

        result = isDomainResourceCountEqualToExpectedCount(
                                    self.apiclient, self.cadmin_2.domainid,
                                    expectedCount, RESOURCE_SECONDARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")

        self.templateSize = int((int(templates[0].size)*2) / (1024**3))

        expectedCount = self.templateSize
        result = isDomainResourceCountEqualToExpectedCount(
                                    self.apiclient, self.parent_domain.id,
                                    expectedCount, RESOURCE_SECONDARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")

        try:
            self.cadmin_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete account: %s" % e)

        expectedCount /= 2
        result = isDomainResourceCountEqualToExpectedCount(
                                    self.apiclient, self.parent_domain.id,
                                    expectedCount, RESOURCE_SECONDARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")
        return
