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
""" P1 tests for memory resource limits
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.integration.lib.base import (
                                        Account,
                                        ServiceOffering,
                                        VirtualMachine,
                                        Domain,
                                        Project
                                        )
from marvin.integration.lib.common import (get_domain,
                                        get_zone,
                                        get_template,
                                        cleanup_resources,
					wait_for_cleanup,
                                        get_updated_resource_count,
                                        find_suitable_host,
                                        get_resource_type
                                        )

class Services:
    """Test memory resource limit services
    """

    def __init__(self):
        self.services = {
                        "account": {
                                "email": "test@test.com",
                                "firstname": "Test",
                                "lastname": "User",
                                "username": "resource",
                                # Random characters are appended for unique
                                # username
                                "password": "password",
                         },
                         "service_offering": {
                                "name": "Tiny Instance",
                                "displaytext": "Tiny Instance",
                                "cpunumber": 1,
                                "cpuspeed": 100,    # in MHz
                                "memory": 5120,    # In MBs
                        },
                        "virtual_machine": {
                                "displayname": "TestVM",
                                "username": "root",
                                "password": "password",
                                "ssh_port": 22,
                                "hypervisor": 'KVM',
                                "privateport": 22,
                                "publicport": 22,
                                "protocol": 'TCP',
                                },
                         "network": {
                                "name": "Test Network",
                                "displaytext": "Test Network",
                                "netmask": '255.255.255.0'
                                },
                         "project": {
                                "name": "Project",
                                "displaytext": "Test project",
                                },
                         "domain": {
                                "name": "Domain",
                                },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        "sleep": 60,
                        "timeout": 10,
                        "mode": 'advanced',
                        # Networking mode: Advanced, Basic
                    }

class TestProjectsMemoryLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestProjectsMemoryLimits,
                               cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
	cls.services["mode"] = cls.zone.networktype

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
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
             self.services["account"],
             admin=True
             )
        self.cleanup = [self.account, ]

        self.debug("Setting up account and domain hierarchy")
        self.setupProjectAccounts()

	api_client = self.testClient.createUserApiClient(
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
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createInstance(self, project, service_off, networks=None, api_client=None):
        """Creates an instance in account"""
        self.debug("Deploying an instance in account: %s" %
                                                self.account.name)

	if api_client is None:
	    api_client = self.apiclient

        try:
            vm = VirtualMachine.create(
                                api_client,
                                self.services["virtual_machine"],
                                templateid=self.template.id,
                                projectid=project.id,
                                networkids=networks,
                                serviceofferingid=service_off.id)
            vms = VirtualMachine.list(api_client, id=vm.id, listall=True)
            self.assertIsInstance(vms,
                                  list,
                                  "List VMs should return a valid response")
            self.assertEqual(vms[0].state, "Running",
                             "Vm state should be running after deployment")
            return vm
        except Exception as e:
            self.fail("Failed to deploy an instance: %s" % e)  

    def setupProjectAccounts(self):

        self.debug("Creating a domain under: %s" % self.domain.name)
        self.domain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)
        self.admin = Account.create(
                            self.apiclient,
                            self.services["account"],
                            admin=True,
                            domainid=self.domain.id
                            )

        # Create project as a domain admin
        self.project = Project.create(self.apiclient,
                                 self.services["project"],
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

    @attr(tags=["advanced", "advancedns","simulator"])
    @attr(configuration='max.projects.memory')
    def test_01_project_vmlifecycle_reboot_instance(self):
        """Test max.projects.memory global configuration"""

        # Validate the following
        # 1. Set (max.project.memory=10) as the max limit to
        #    Domain1 (max.account.memory=10)
        # 2. Assign account to projects and verify the resource updates
        # 3. Deploy VM with the accounts added to the project
        # 4. Stop VM of an accounts added to the project to a new host
        # 5. Resource count should list properly
        # 6. Start VM of an accounts added to the project to a new host
        # 7. Resource count should list properly

        self.debug("Checking memory resource count for project: %s" % self.project.name)
        resource_count = get_updated_resource_count(self.apiclient, account=self.admin, project=self.project, rtype=9)#RAM
        self.debug(resource_count)
    
        self.debug("Stopping instance: %s" % self.vm.name)
        try:
            self.vm.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)

        self.debug("Checking memory resource count for project: %s" % self.project.name)
        resource_count = get_updated_resource_count(self.apiclient, account=self.admin, project=self.project, rtype=9)#RAM
        self.debug(resource_count)

        self.debug("Starting instance: %s" % self.vm.name)
        try:
            self.vm.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start instance: %s" % e)

        self.debug("Checking memory resource count for project: %s" % self.project.name)
        get_updated_resource_count(self.apiclient, account=self.admin, project=self.project, rtype=9)#RAM 
        self.debug(resource_count)
        return

    @attr(tags=["advanced", "advancedns","simulator"])
    @attr(configuration='max.projects.memory')
    def test_02_project_vmlifecycle_migrate_instance(self):
        """Test max.projects.memory global configuration"""

        # Validate the following
        # 1. Set (max.project.memory=10) as the max limit to
        #    Domain1 (max.account.memory=10)
        # 2. Assign account to projects and verify the resource updates
        # 3. Deploy VM with the accounts added to the project
        # 4. Migrate VM of an accounts added to the project to a new host
        # 5. Resource count should list properly.

        self.debug("Checking memory resource count for project: %s" % self.project.name)
        resource_count = get_updated_resource_count(self.apiclient, account=self.admin, project=self.project, rtype=9)#RAM 
        self.debug(resource_count)        

        host = find_suitable_host(self.apiclient, self.vm)
        self.debug("Migrating instance: %s to host: %s" %
                                                    (self.vm.name, host.name))
        try:
            self.vm.migrate(self.apiclient, host.id)
        except Exception as e:
            self.fail("Failed to migrate instance: %s" % e)
    
        self.debug("Checking memory resource count for project: %s" % self.project.name)
        resource_count = get_updated_resource_count(self.apiclient, account=self.admin, project=self.project, rtype=9)#RAM 
        self.debug(resource_count) 
        return

    @attr(tags=["advanced", "advancedns","simulator"])
    @attr(configuration='max.projects.memory')
    def test_03_project_vmlifecycle_delete_instance(self):
        """Test max.projects.memory global configuration"""

        # Validate the following
        # 1. Set (max.project.memory=10) as the max limit to
        #    Domain1 (max.account.memory=10)
        # 2. Assign account to projects and verify the resource updates
        # 3. Deploy VM with the accounts added to the project 
        # 4. Destroy VM of an accounts added to the project
        # 5. Resource count should list as 0 after destroying the instance

        self.debug("Checking memory resource count for project: %s" % self.project.name)
        resource_count = get_updated_resource_count(self.apiclient, account=self.admin, project=self.project, rtype=9)#RAM 
        self.debug(resource_count) 

        self.debug("Destroying instance: %s" % self.vm.name)
        try:
            self.vm.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

	# Wait for expunge interval to cleanup Memory
	wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        self.debug("Checking memory resource count for project: %s" % self.project.name)
        resource_count = get_updated_resource_count(self.apiclient, account=self.admin, project=self.project, rtype=9)#RAM 
        self.debug(resource_count)
        self.assertEqual(resource_count, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=9))#RAM         
        return

 
