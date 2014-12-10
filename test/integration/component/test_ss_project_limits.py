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

""" P1 tests for secondary storage Project limits

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domain+or+accounts

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-1466

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domains+and+accounts
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Domain,
                             Project,
                             Template,
                             Iso,
                             Resources)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               matchResourceCount,
                               get_builtin_template_info,
                               createSnapshotFromVirtualMachineVolume)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.codes import (PASS,
                          FAIL,
                          FAILED,
                          RESOURCE_SECONDARY_STORAGE)
import time

class TestProjectsVolumeLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestProjectsVolumeLimits,
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

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["volume"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        try:
            cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setUpClass: %s" % e)
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
        response = self.setupProjectAccounts()
        self.assertEqual(response[0], PASS, response[1])
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setupProjectAccounts(self):

        try:
            self.parentDomain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)
            self.domainAdmin = Account.create(
                            self.apiclient, self.services["account"],
                            admin=True, domainid=self.parentDomain.id)

            # Create project as a domain admin
            self.project = Project.create(
                            self.apiclient,self.services["project"],
                            account=self.domainAdmin.name,domainid=self.parentDomain.id)
            # Cleanup created project at end of test
            self.cleanup.append(self.project)
            self.cleanup.append(self.domainAdmin)
            self.cleanup.append(self.parentDomain)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_register_template_with_project(self):
        """Test register template
        # Validate the following:
        1. Create a project
        2. Register and download a template according to hypervisor type in the project
        3. Verify that the template is listed
        4. Verify that the secondary storage count for the project equals the size
           of the template
        5. Delete the template
        6. Verify that the secondary storage resource count of the project equals 0
       """

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.services["template_2"]["url"] = builtin_info[0]
        self.services["template_2"]["hypervisor"] = builtin_info[1]
        self.services["template_2"]["format"] = builtin_info[2]

        try:
            template = Template.register(self.apiclient,
                                            self.services["template_2"],
                                            zoneid=self.zone.id,
                                            projectid=self.project.id)

            template.download(self.apiclient)
        except Exception as e:
            self.fail("Failed to register template: %s" % e)

        templates = Template.list(self.apiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=template.id,
                                      )
        self.assertEqual(validateList(templates)[0],PASS,\
                        "templates list validation failed")

        templates = Template.list(self.apiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=template.id,
                                      )
        self.assertEqual(validateList(templates)[0],PASS,\
                        "templates list validation failed")

        templateSize = (templates[0].size / (1024**3))
        expectedCount = templateSize
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_SECONDARY_STORAGE,
                        projectid=self.project.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            template.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete template: %s" % e)

        expectedCount = 0
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_SECONDARY_STORAGE,
                        projectid=self.project.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @attr(tags = ["advanced"], required_hardware="true")
    def test_02_register_iso(self):
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
        try:
            self.projectMember = Account.create(
                            self.apiclient, self.services["account"],
                            domainid=self.parentDomain.id)
            self.cleanup.insert(0, self.projectMember)
            self.project.addAccount(self.apiclient, account=self.projectMember.name)
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        self.services["iso"]["zoneid"] = self.zone.id
        try:
            iso = Iso.create(
                         self.apiclient,
                         self.services["iso"],
                         account=self.projectMember.name,
                         domainid=self.projectMember.domainid
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
                                      projectid=self.project.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            iso.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete Iso")

        expectedCount = 0
        response = matchResourceCount(self.apiclient, expectedCount,
                                      resourceType=RESOURCE_SECONDARY_STORAGE,
                                      projectid=self.project.id)
        self.assertEqual(response[0], PASS, response[1])
        return
