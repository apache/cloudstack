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

""" Tests for secondary storage - Maximum Limits

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domain+or+accounts

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-1466

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domains+and+accounts
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             ServiceOffering,
                             Resources,
                             Domain,
                             Project,
                             Template)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_builtin_template_info,
                               matchResourceCount)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.codes import PASS, FAIL, RESOURCE_SECONDARY_STORAGE

class TestMaxSecondaryStorageLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestMaxSecondaryStorageLimits,
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

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["volume"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
        cls._cleanup = [cls.service_offering]
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
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def registerTemplate(self, inProject=False):
        """Register and download template by default in the account/domain,
        in project if stated so"""

        try:
            builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
            self.services["template_2"]["url"] = builtin_info[0]
            self.services["template_2"]["hypervisor"] = builtin_info[1]
            self.services["template_2"]["format"] = builtin_info[2]

            template = Template.register(self.userapiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.child_do_admin.name if not inProject else None,
                                     domainid=self.child_do_admin.domainid if not inProject else None,
                                     projectid=self.project.id if inProject else None)

            template.download(self.apiclient)

            templates = Template.list(self.userapiclient,
                                      templatefilter=\
                                      self.services["template_2"]["templatefilter"],
                                      id=template.id)
            self.assertEqual(validateList(templates)[0], PASS,\
                             "templates list validation failed")

            self.templateSize = (templates[0].size / (1024**3))
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    def setupAccounts(self):

        try:
            self.child_domain = Domain.create(self.apiclient,services=self.services["domain"],
                                          parentdomainid=self.domain.id)

            self.child_do_admin = Account.create(self.apiclient, self.services["account"], admin=True,
                                             domainid=self.child_domain.id)

            self.userapiclient = self.testClient.getUserApiClient(
                                    UserName=self.child_do_admin.name,
                                    DomainName=self.child_do_admin.domain)

            # Create project as a domain admin
            self.project = Project.create(self.apiclient, self.services["project"],
                                      account=self.child_do_admin.name,
                                      domainid=self.child_do_admin.domainid)

            # Cleanup created project at end of test
            self.cleanup.append(self.project)

            # Cleanup accounts created
            self.cleanup.append(self.child_do_admin)
            self.cleanup.append(self.child_domain)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    def updateSecondaryStorageLimits(self, accountLimit=None, domainLimit=None, projectLimit=None):

        try:
            # Update resource limits for account
            if accountLimit is not None:
                Resources.updateLimit(self.apiclient, resourcetype=11,
                                max=accountLimit, account=self.child_do_admin.name,
                                domainid=self.child_do_admin.domainid)

            if projectLimit is not None:
                Resources.updateLimit(self.apiclient, resourcetype=11,
                                              max=projectLimit, projectid=self.project.id)

            if domainLimit is not None:
                Resources.updateLimit(self.apiclient, resourcetype=11,
                                              max=domainLimit, domainid=self.child_domain.id)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_deploy_vm_domain_limit_reached(self):
        """Test Try to deploy VM with admin account where account has not used
            the resources but @ domain they are not available

        # Validate the following
        # 1. Try to register template with admin account where account has not used the
        #    resources but @ domain they are not available
        # 2. Template registration should fail"""

        response = self.setupAccounts()
        self.assertEqual(response[0], PASS, response[1])

        response = self.registerTemplate()
        self.assertEqual(response[0], PASS, response[1])

        expectedCount = self.templateSize
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_SECONDARY_STORAGE,
                        accountid=self.child_do_admin.id)
        self.assertEqual(response[0], PASS, response[1])

        domainLimit = self.templateSize

        response = self.updateSecondaryStorageLimits(domainLimit=domainLimit)
        self.assertEqual(response[0], PASS, response[1])

        with self.assertRaises(Exception):
            template = Template.register(self.userapiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.child_do_admin.name,
                                     domainid=self.child_do_admin.domainid)
            template.delete(self.userapiclient)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_deploy_vm_account_limit_reached(self):
        """Test Try to deploy VM with admin account where account has used
            the resources but @ domain they are available

        # Validate the following
        # 1. Try to register template with admin account where account has used the
        #    resources but @ domain they are available
        # 2. Template registration should fail"""

        response = self.setupAccounts()
        self.assertEqual(response[0], PASS, response[1])

        response = self.registerTemplate()
        self.assertEqual(response[0], PASS, response[1])

        expectedCount = self.templateSize
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_SECONDARY_STORAGE,
                        accountid=self.child_do_admin.id)
        self.assertEqual(response[0], PASS, response[1])

        accountLimit = self.templateSize

        response = self.updateSecondaryStorageLimits(accountLimit=accountLimit)
        self.assertEqual(response[0], PASS, response[1])

        with self.assertRaises(Exception):
            template = Template.register(self.userapiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.child_do_admin.name,
                                     domainid=self.child_do_admin.domainid)
            template.delete(self.userapiclient)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_deploy_vm_project_limit_reached(self):
        """Test TTry to deploy VM with admin account where account has not used
        the resources but @ project they are not available

        # Validate the following
        # 1. Try to register template with admin account where account has not used the
        #    resources but @ project they are not available
        # 2. Template registration should error out saying  ResourceAllocationException
        #    with "resource limit exceeds"""

        response = self.setupAccounts()
        self.assertEqual(response[0], PASS, response[1])

        response = self.registerTemplate(inProject=True)
        self.assertEqual(response[0], PASS, response[1])

        try:
            projects = Project.list(self.userapiclient, id=self.project.id, listall=True)
        except Exception as e:
            self.fail("failed to get projects list: %s" % e)

        self.assertEqual(validateList(projects)[0], PASS,
            "projects list validation failed")
        self.assertEqual(self.templateSize, projects[0].secondarystoragetotal, "Resource count %s\
                 not matching with the expcted count: %s" %
                 (projects[0].secondarystoragetotal, self.templateSize))

        projectLimit = self.templateSize

        response = self.updateSecondaryStorageLimits(projectLimit=projectLimit)
        self.assertEqual(response[0], PASS, response[1])

        with self.assertRaises(Exception):
            template = Template.register(self.userapiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     projectid=self.project.id)
            template.delete(self.userapiclient)
        return
