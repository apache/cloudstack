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

""" Tests for cpu resource limits related to projects
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (
                                        Account,
                                        ServiceOffering,
                                        VirtualMachine,
                                        Domain,
                                        Project
                                        )
from marvin.lib.common import (get_domain,
                                        get_zone,
                                        get_template,
                                        findSuitableHostForMigration,
                                        get_resource_type
                                        )
from marvin.lib.utils import cleanup_resources
from marvin.codes import ERROR_NO_HOST_FOR_MIGRATION

class TestProjectsCPULimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestProjectsCPULimits, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata["mode"] = cls.zone.networktype
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.testdata["ostype"]
                            )

        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.testdata["service_offering_multiple_cores"]
                                            )

        cls._cleanup = [cls.service_offering, ]
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
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            admin=True
                            )

        self.cleanup = [self.account, ]

        self.debug("Setting up account and domain hierarchy")
        self.setupProjectAccounts()

        api_client = self.testClient.getUserApiClient(
            UserName=self.admin.name,
            DomainName=self.admin.domain)

        self.debug("Creating an instance with service offering: %s" %
                   self.service_offering.name)
        self.vm = self.createInstance(project=self.project,
            service_off=self.service_offering, api_client=api_client)

        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createInstance(self, project, service_off, networks=None, api_client=None):
        """Creates an instance in account"""

        if api_client is None:
            api_client = self.api_client

        try:
            self.vm = VirtualMachine.create(
                        api_client,
                        self.testdata["virtual_machine"],
                        templateid=self.template.id,
                        projectid=project.id,
                        networkids=networks,
                        serviceofferingid=service_off.id)
            vms = VirtualMachine.list(api_client, projectid=project.id,
			              id=self.vm.id, listall=True)
            self.assertIsInstance(vms,
                    list,
                    "List VMs should return a valid response")
            self.assertEqual(vms[0].state, "Running",
                    "Vm state should be running after deployment")
            return self.vm
        except Exception as e:
            self.fail("Failed to deploy an instance: %s" % e)

    def setupProjectAccounts(self):

        self.debug("Creating a domain under: %s" % self.domain.name)
        self.domain = Domain.create(self.apiclient,
                                        services=self.testdata["domain"],
                                        parentdomainid=self.domain.id)
        self.admin = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            admin=True,
                            domainid=self.domain.id
                            )

        # Create project as a domain admin
        self.project = Project.create(self.apiclient,
                                 self.testdata["project"],
                                 account=self.admin.name,
                                 domainid=self.admin.domainid)
        # Cleanup created project at end of test
        self.cleanup.append(self.project)
        self.cleanup.append(self.admin)
        self.cleanup.append(self.domain)
        self.debug("Created project with domain admin with name: %s" %
                                                        self.project.name)

        projects = Project.list(self.apiclient, id=self.project.id,
                                listall=True)

        self.assertEqual(isinstance(projects, list), True,
                        "Check for a valid list projects response")
        project = projects[0]
        self.assertEqual(project.name, self.project.name,
                        "Check project name from list response")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_01_project_counts_start_stop_instance(self):

        # Validate the following
        # 1. Assign account to projects and verify the resource updates
        # 2. Deploy VM with the accounts added to the project
        # 3. Stop VM of an accounts added to the project.
        # 4. Resource count should list properly.

        project_list = Project.list(self.apiclient, id=self.project.id, listall=True)
        self.debug(project_list)
        self.assertIsInstance(project_list,
                              list,
                              "List Projects should return a valid response"
                              )
        resource_count = project_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

        self.assertEqual(resource_count, expected_resource_count,
                         "Resource count should match with the expected resource count")

        self.debug("Stopping instance: %s" % self.vm.name)
        try:
            self.vm.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)

        project_list = Project.list(self.apiclient, id=self.project.id, listall=True)
        self.assertIsInstance(project_list,
                              list,
                              "List Projects should return a valid response"
                              )
        resource_count_after_stop = project_list[0].cputotal

        self.assertEqual(resource_count, resource_count_after_stop,
                         "Resource count should be same after stopping the instance")

        self.debug("Starting instance: %s" % self.vm.name)
        try:
            self.vm.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start instance: %s" % e)

        project_list = Project.list(self.apiclient, id=self.project.id, listall=True)
        self.assertIsInstance(project_list,
                              list,
                              "List Projects should return a valid response"
                              )
        resource_count_after_start = project_list[0].cputotal

        self.assertEqual(resource_count, resource_count_after_start,
                         "Resource count should be same after starting the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="true")
    def test_02_project_counts_migrate_instance(self):

        # Validate the following
        # 1. Assign account to projects and verify the resource updates
        # 2. Deploy VM with the accounts added to the project
        # 3. Migrate VM of an accounts added to the project to a new host
        # 4. Resource count should list properly.
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate feature is not supported on %s" % self.hypervisor.lower())

        project_list = Project.list(self.apiclient, id=self.project.id, listall=True)
        self.assertIsInstance(project_list,
                              list,
                              "List Projects should return a valid response"
                              )
        resource_count = project_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

        self.assertEqual(resource_count, expected_resource_count,
                         "Resource count should match with the expected resource count")

        host = findSuitableHostForMigration(self.apiclient, self.vm.id)
        if host is None:
            self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)
        self.debug("Migrating instance: %s to host: %s" %
                                                    (self.vm.name, host.name))
        try:
            self.vm.migrate(self.apiclient, host.id)
        except Exception as e:
            self.fail("Failed to migrate instance: %s" % e)

        project_list = Project.list(self.apiclient, id=self.project.id, listall=True)
        self.assertIsInstance(project_list,
                              list,
                              "List Projects should return a valid response"
                              )
        resource_count_after_migrate = project_list[0].cputotal

        self.assertEqual(resource_count, resource_count_after_migrate,
                         "Resource count should be same after migrating the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_03_project_counts_delete_instance(self):

        # Validate the following
        # 1. Assign account to projects and verify the resource updates
        # 2. Deploy VM with the accounts added to the project
        # 3. Destroy VM of an accounts added to the project to a new host
        # 4. Resource count should list properly.

        project_list = Project.list(self.apiclient, id=self.project.id, listall=True)
        self.assertIsInstance(project_list,
                              list,
                              "List Projects should return a valid response"
                              )
        resource_count = project_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

        self.assertEqual(resource_count, expected_resource_count,
                         "Resource count should match with the expected resource count")

        self.debug("Destroying instance: %s" % self.vm.name)
        try:
            self.vm.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

        project_list = Project.list(self.apiclient, id=self.project.id, listall=True)
        self.assertIsInstance(project_list,
                              list,
                              "List Projects should return a valid response"
                              )
        resource_count_after_delete = project_list[0].cputotal
        self.assertEqual(resource_count_after_delete, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=8))#CPU
        return
