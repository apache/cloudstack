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

""" Tests for praimary storage - Maximum Limits

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
                             Resources,
                             Domain,
                             Project,
                             Volume,
                             DiskOffering)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.codes import PASS, FAIL

class TestMaxPrimaryStorageLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestMaxPrimaryStorageLimits,
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
        try:
            response = self.setupAccounts()
            if response[0] == FAIL:
                self.skipTest("Failure while setting up accounts: %s" % response[1])
            self.services["disk_offering"]["disksize"] = 2
            self.disk_offering = DiskOffering.create(self.apiclient, self.services["disk_offering"])
            self.cleanup.append(self.disk_offering)
        except Exception as e:
            self.tearDown()
            self.skipTest("Failure in setup: %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setupAccounts(self):

        try:
            self.child_domain = Domain.create(self.apiclient,services=self.services["domain"],
                                          parentdomainid=self.domain.id)

            self.child_do_admin = Account.create(self.apiclient, self.services["account"], admin=True,
                                             domainid=self.child_domain.id)

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

    def updatePrimaryStorageLimits(self, accountLimit=None, domainLimit=None,
                                   projectLimit=None):

        try:
            # Update resource limits for account
            if accountLimit:
                Resources.updateLimit(self.apiclient, resourcetype=10,
                                max=accountLimit, account=self.child_do_admin.name,
                                domainid=self.child_do_admin.domainid)

            if projectLimit:
                Resources.updateLimit(self.apiclient, resourcetype=10,
                                              max=projectLimit, projectid=self.project.id)

            if domainLimit:
                Resources.updateLimit(self.apiclient, resourcetype=10,
                                              max=domainLimit, domainid=self.child_domain.id)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    @attr(tags=["advanced","selfservice"])
    def test_01_deploy_vm_domain_limit_reached(self):
        """Test Try to deploy VM with admin account where account has not used
            the resources but @ domain they are not available

        # Validate the following
        # 1. Try to deploy VM with admin account where account has not used the
        #    resources but @ domain they are not available
        # 2. Deploy VM should error out saying  ResourceAllocationException
        #    with "resource limit exceeds"""

        self.virtualMachine = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                            accountid=self.child_do_admin.name, domainid=self.child_do_admin.domainid,
                            serviceofferingid=self.service_offering.id)

        accounts = Account.list(self.apiclient, id=self.child_do_admin.id)
        self.assertEqual(validateList(accounts)[0], PASS,
            "accounts list validation failed")

        self.initialResourceCount = int(accounts[0].primarystoragetotal)
        domainLimit = self.initialResourceCount + 3

        self.debug("Setting up account and domain hierarchy")
        response = self.updatePrimaryStorageLimits(domainLimit=domainLimit)
        self.assertEqual(response[0], PASS, response[1])

        self.services["volume"]["size"] = self.services["disk_offering"]["disksize"] = 2

        try:
            disk_offering = DiskOffering.create(self.apiclient,
                                    services=self.services["disk_offering"])
            self.cleanup.append(disk_offering)
            Volume.create(self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   account=self.child_do_admin.name,
                                   domainid=self.child_do_admin.domainid,
                                   diskofferingid=disk_offering.id)
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        with self.assertRaises(Exception):
            Volume.create(self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   account=self.child_do_admin.name,
                                   domainid=self.child_do_admin.domainid,
                                   diskofferingid=disk_offering.id)
        return

    @attr(tags=["advanced","selfservice"])
    def test_02_deploy_vm_account_limit_reached(self):
        """Test Try to deploy VM with admin account where account has used
            the resources but @ domain they are available"""

        self.virtualMachine = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                            accountid=self.child_do_admin.name, domainid=self.child_do_admin.domainid,
                            serviceofferingid=self.service_offering.id)

        accounts = Account.list(self.apiclient, id=self.child_do_admin.id)
        self.assertEqual(validateList(accounts)[0], PASS,
            "accounts list validation failed")

        self.initialResourceCount = int(accounts[0].primarystoragetotal)
        accountLimit = self.initialResourceCount + 3

        self.debug("Setting up account and domain hierarchy")
        response = self.updatePrimaryStorageLimits(accountLimit=accountLimit)
        self.assertEqual(response[0], PASS, response[1])

        self.services["volume"]["size"] = self.services["disk_offering"]["disksize"] = 2

        try:
            disk_offering = DiskOffering.create(self.apiclient,
                                    services=self.services["disk_offering"])
            self.cleanup.append(disk_offering)
            Volume.create(self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   account=self.child_do_admin.name,
                                   domainid=self.child_do_admin.domainid,
                                   diskofferingid=disk_offering.id)
        except Exception as e:
            self.fail("failed to create volume: %s" % e)

        with self.assertRaises(Exception):
            Volume.create(self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   account=self.child_do_admin.name,
                                   domainid=self.child_do_admin.domainid,
                                   diskofferingid=disk_offering.id)
        return

    @attr(tags=["advanced","selfservice"])
    def test_03_deploy_vm_project_limit_reached(self):
        """Test TTry to deploy VM with admin account where account has not used
        the resources but @ project they are not available

        # Validate the following
        # 1. Try to deploy VM with admin account where account has not used the
        #    resources but @ project they are not available
        # 2. Deploy VM should error out saying  ResourceAllocationException
        #    with "resource limit exceeds"""

        self.virtualMachine = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                            projectid=self.project.id,
                            serviceofferingid=self.service_offering.id)

        try:
            projects = Project.list(self.apiclient, id=self.project.id, listall=True)
        except Exception as e:
            self.fail("failed to get projects list: %s" % e)

        self.assertEqual(validateList(projects)[0], PASS,
            "projects list validation failed")
        self.initialResourceCount = int(projects[0].primarystoragetotal)

        projectLimit = self.initialResourceCount + 3

        self.debug("Setting up account and domain hierarchy")
        response = self.updatePrimaryStorageLimits(projectLimit=projectLimit)
        self.assertEqual(response[0], PASS, response[1])

        self.services["volume"]["size"] = self.services["disk_offering"]["disksize"] = 2

        try:
            disk_offering = DiskOffering.create(self.apiclient,
                                    services=self.services["disk_offering"])
            self.cleanup.append(disk_offering)
            Volume.create(self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   projectid=self.project.id,
                                   diskofferingid=disk_offering.id)
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        with self.assertRaises(Exception):
            Volume.create(self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   projectid=self.project.id,
                                   diskofferingid=disk_offering.id)
        return
