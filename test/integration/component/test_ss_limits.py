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

""" P1 tests for secondary storage limits

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domain+or+accounts

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-1466

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domains+and+accounts
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Domain,
                             Template,
                             Iso)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               matchResourceCount,
                               createSnapshotFromVirtualMachineVolume,
                               list_zones,
                               get_builtin_template_info)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.codes import (PASS,
                          FAIL,
                          FAILED,
                          RESOURCE_SECONDARY_STORAGE,
                          CHILD_DOMAIN_ADMIN,
                          ROOT_DOMAIN_ADMIN)
from ddt import ddt, data
import time

@ddt
class TestSecondaryStorageLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestSecondaryStorageLimits,
                               cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
        # Fill services from the external config file
        cls.services = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cloudstackTestClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype
        cls.hypervisor = cloudstackTestClient.getHypervisorInfo()

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["volume"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        try:
            cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
            if cls.service_offering == FAILED:
                raise Exception("Creating service offering failed")
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setup class: %s" % e)
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

    def setupAccount(self, accountType):
        """Setup the account required for the test"""

        try:
            if accountType == CHILD_DOMAIN_ADMIN:
                self.domain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)

            self.account = Account.create(self.apiclient, self.services["account"],
                                      domainid=self.domain.id, admin=True)
            self.cleanup.append(self.account)
            if accountType == CHILD_DOMAIN_ADMIN:
                self.cleanup.append(self.domain)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]
 
    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags = ["advanced"], required_hardware="true")
    def test_01_register_template(self, value):
        """Test register template
        # Validate the following:
        1. Create a root domain admin/ child domain admin account
        2. Register and download a template according to hypervisor type
        3. Verify that the template is listed
        4. Verify that the secondary storage count for the account equals the size
           of the template
        5. Delete the template
        6. Verify that the secondary storage resource count of the account equals 0
       """
        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        apiclient = self.testClient.getUserApiClient(
                                UserName=self.account.name,
                                DomainName=self.account.domain)

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.services["template_2"]["url"] = builtin_info[0]
        self.services["template_2"]["hypervisor"] = builtin_info[1]
        self.services["template_2"]["format"] = builtin_info[2]

        try:
            template = Template.register(apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.account.name,
                                     domainid=self.account.domainid,
                                     hypervisor=self.hypervisor)

            template.download(apiclient)
        except Exception as e:
            self.fail("Failed to register template: %s" % e)

        time.sleep(120)
        templates = Template.list(apiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=template.id)
        self.assertEqual(validateList(templates)[0],PASS,\
                        "templates list validation failed")

        templateSize = (templates[0].size / (1024**3))
        expectedCount = templateSize
        response = matchResourceCount(
                        apiclient, expectedCount,
                        RESOURCE_SECONDARY_STORAGE,
                        accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            template.delete(apiclient)
        except Exception as e:
            self.fail("Failed to delete template: %s" % e)

        expectedCount = 0
        response = matchResourceCount(
                        apiclient, expectedCount,
                        RESOURCE_SECONDARY_STORAGE,
                        accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags=["advanced"], required_hardware="true")
    def test_02_create_template_snapshot(self, value):
        """Test create snapshot and templates from volume

        # Validate the following
        1. Create root domain/child domain admin account
        2. Deploy VM in the account
        3. Create snapshot from the virtual machine root volume
        4. Create template from the snapshot
        5. Verify that the secondary storage count of the account equals
           the size of the template"""

        if self.hypervisor.lower() in ['hyperv', 'lxc']:
            self.skipTest("Snapshots feature is not supported on %s" % self.hypervisor.lower())

        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        self.virtualMachine = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                            accountid=self.account.name, domainid=self.account.domainid,
                            serviceofferingid=self.service_offering.id)

        self.assertNotEqual(self.virtualMachine, FAILED, "VM deployment failed")

        apiclient = self.testClient.getUserApiClient(
                                UserName=self.account.name,
                                DomainName=self.account.domain)
        self.assertNotEqual(apiclient, FAILED,\
            "Failed to create api client for account: %s" % self.account.name)

        try:
            self.virtualMachine.stop(apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)

        self.debug("Creating snapshot from ROOT volume: %s" % self.virtualMachine.name)
        response = createSnapshotFromVirtualMachineVolume(apiclient, self.account, self.virtualMachine.id)
        self.assertEqual(response[0], PASS, response[1])
        snapshot = response[1]

        snapshotSize = (snapshot.physicalsize / (1024 ** 3))

        try:
            template = Template.create_from_snapshot(apiclient,
                                        snapshot=snapshot,
                                        services=self.services["template_2"])
        except Exception as e:
            self.fail("Failed to create template: %s" % e)

        time.sleep(120)
        templates = Template.list(apiclient,
                                  templatefilter=\
                                  self.services["template_2"]["templatefilter"],
                                  id=template.id)
        self.assertEqual(validateList(templates)[0],PASS,\
                        "templates list validation failed")

        templateSize = (templates[0].size / (1024**3))

        expectedSecondaryStorageCount = int(templateSize + snapshotSize)
        response = matchResourceCount(self.apiclient, expectedSecondaryStorageCount,
                                      resourceType=RESOURCE_SECONDARY_STORAGE,
                                      accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags = ["advanced"], required_hardware="true")
    def test_03_register_iso(self, value):
        """Test register iso
        Steps and validations:
        1. Create a root domain/child domain admin account
        2. Register a test iso in the account
        3. Wait till the iso is downloaded and is in ready state
        3. Verify that secondary storage resource count of the account equals the
           iso size
        4. Delete the iso
        5. Verify that the secondary storage count of the account equals 0
        """
        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        self.services["iso"]["zoneid"] = self.zone.id
        try:
            iso = Iso.create(
                         self.apiclient,
                         self.services["iso"],
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        except Exception as e:
            self.fail("Failed to create Iso: %s" % e)

        timeout = 600
        isoList = None
        while timeout >= 0:
            isoList = Iso.list(self.apiclient,
                                      isofilter="self",
                                      id=iso.id)
            self.assertEqual(validateList(isoList)[0],PASS,\
                            "iso list validation failed")
            if isoList[0].isready:
                break
            time.sleep(60)
            timeout -= 60

        self.assertNotEqual(timeout, 0,\
                "template not downloaded completely")

        isoSize = (isoList[0].size / (1024**3))
        expectedCount = isoSize
        response = matchResourceCount(self.apiclient, expectedCount,
                                      resourceType=RESOURCE_SECONDARY_STORAGE,
                                      accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            iso.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete Iso")

        expectedCount = 0
        response = matchResourceCount(self.apiclient, expectedCount,
                                      resourceType=RESOURCE_SECONDARY_STORAGE,
                                      accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags = ["advanced"], required_hardware="true")
    def test_04_copy_template(self, value):
        """Test copy template between zones

        Steps and validations:
        This test requires at least two zones present in the setup
        1. Create a root domain/child domain admin account
        2. Register and download a template in the account
        3. Verify the secondary storage resource count of the account
           equals the size of the template
        4. Copy this template to other zone
        5. Verify that the secondary storage resource count is now doubled
           as there are two templates now in two zones under the admin account
        """

        zones = list_zones(self.apiclient)
        self.assertEqual(validateList(zones)[0], PASS, "zones list validation faield")

        if len(zones) < 2:
            self.skipTest("At least 2 zones should be present for this test case")

        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.services["template_2"]["url"] = builtin_info[0]
        self.services["template_2"]["hypervisor"] = builtin_info[1]
        self.services["template_2"]["format"] = builtin_info[2]

        try:
            template = Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.account.name,
                                     domainid=self.account.domainid,
                                     hypervisor=self.hypervisor)

            template.download(self.apiclient)
        except Exception as e:
            self.fail("Failed to register template: %s" % e)

        templates = Template.list(self.apiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=template.id)
        self.assertEqual(validateList(templates)[0],PASS,\
                         "templates list validation failed")

        templateSize = (templates[0].size / (1024**3))
        expectedCount = templateSize
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_SECONDARY_STORAGE,
                        accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        templateDestinationZoneId = None
        for zone in zones:
            if template.zoneid != zone.id :
                templateDestinationZoneId = zone.id
                break

        template.copy(self.apiclient, destzoneid=templateDestinationZoneId,
                      sourcezoneid = template.zoneid)

        expectedCount = (templateSize * 2)
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_SECONDARY_STORAGE,
                        accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return
