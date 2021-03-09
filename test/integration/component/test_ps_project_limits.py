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

""" P1 tests for primary storage Project limits

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
                             Host,
                             Domain,
                             Project,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               matchResourceCount,
                               isVmExpunged)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.codes import (PASS,
                          FAIL,
                          RESOURCE_PRIMARY_STORAGE)
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
        cls.zone = get_zone(cls.api_client, cloudstackTestClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
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

        try:
            self.vm = VirtualMachine.create(
                        self.apiclient,self.services["virtual_machine"],
                        templateid=self.template.id,projectid=self.project.id,
                        serviceofferingid=self.service_offering.id)
            projects = Project.list(self.apiclient,id=self.project.id, listall=True)
            self.assertEqual(validateList(projects)[0], PASS,\
                    "projects list validation failed")
            self.initialResourceCount = projects[0].primarystoragetotal
        except Exception as e:
            self.tearDown()
            self.skipTest("Exception occured in setup: %s" % e)
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
            self.domain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)
            self.admin = Account.create(
                            self.apiclient, self.services["account"],
                            admin=True, domainid=self.domain.id)

            # Create project as a domain admin
            self.project = Project.create(
                            self.apiclient,self.services["project"],
                            account=self.admin.name,domainid=self.admin.domainid)
            # Cleanup created project at end of test
            self.cleanup.append(self.project)
            self.cleanup.append(self.admin)
            self.cleanup.append(self.domain)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_VM_start_stop(self):
        """Test project primary storage count with VM stop/start operation

        # Validate the following
        # 1. Create VM with custom disk offering in a project and check
        #    initial primary storage count
        # 2. Stop the VM and verify primary storage count remains the same
        # 3. Start the VM and verify priamay storage count remains the same
        """

        try:
            self.vm.stop(self.apiclient)
        except Exception as e:
            self.fail("Faield to stop VM: %s" % e)

        expectedCount = self.initialResourceCount
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_PRIMARY_STORAGE,
                        projectid=self.project.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            self.vm.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start VM: %s" % e)

        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_PRIMARY_STORAGE,
                        projectid=self.project.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_migrate_vm(self):
        """Test migrate VM in project

        # Validate the following
        # 1. Create VM with custom disk offering in a project and check
        #    initial primary storage count
        # 2. List the hosts suitable for migrating the VM
        # 3. Migrate the VM and verify that primary storage count of project remains same"""
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate feature is not supported on %s" % self.hypervisor.lower())
        try:
            hosts = Host.list(self.apiclient,virtualmachineid=self.vm.id,
                              listall=True)
            self.assertEqual(validateList(hosts)[0], PASS, "hosts list validation failed")
            host = hosts[0]
            self.vm.migrate(self.apiclient, host.id)
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        expectedCount = self.initialResourceCount
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_PRIMARY_STORAGE,
                        projectid=self.project.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_delete_vm(self):
        """Test delete VM belonging to project

        # Validate the following
        # 1. Create VM with custom disk offering in a project and check
        #    initial primary storage count
        # 2. Delete VM and verify that it's expunged
        # 3. Verify that primary storage count of project equals 0"""

        try:
            self.vm.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to detroy VM: %s" % e)

        self.assertTrue(isVmExpunged(self.apiclient, self.vm.id, self.project.id),\
                "VM not expunged")

        totalallottedtime = timeout = 600
        while timeout >= 0:
            volumes = Volume.list(self.apiclient, projectid=self.project.id, listall=True)
            if volumes is None:
                break
            if timeout == 0:
                self.fail("Volume attached to VM not cleaned up even\
                        after %s seconds" % totalallottedtime)
            timeout -= 60
            time.sleep(60)

        expectedCount = 0
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_PRIMARY_STORAGE,
                        projectid=self.project.id)
        self.assertEqual(response[0], PASS, response[1])
        return
