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
from marvin.lib.base import (
                                        Account,
                                        ServiceOffering,
                                        VirtualMachine,
                                        Resources,
                                        Domain
                                        )
from marvin.lib.common import (get_domain,
                                        get_zone,
                                        get_template,
                                        wait_for_cleanup,
                                        findSuitableHostForMigration,
                                        get_resource_type
                                        )
from marvin.lib.utils import cleanup_resources
from marvin.codes import ERROR_NO_HOST_FOR_MIGRATION

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
                                "memory": 2048,    # In MBs
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

class TestMemoryLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMemoryLimits, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

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

        self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)
        self.vm = self.createInstance(service_off=self.service_offering)

        self.cleanup = [self.account, ]
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createInstance(self, service_off, networks=None, api_client=None):
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
                                accountid=self.account.name,
                                domainid=self.account.domainid,
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

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_01_stop_start_instance(self):
        """Test Deploy VM with specified RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with specified RAM & Deploy VM as root admin
        # 2 .List Resource count for the root admin Memory usage
        # 3. Stop and start instance, resource count should list properly.

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].memorytotal

        expected_resource_count = int(self.services["service_offering"]["memory"])

        self.assertEqual(resource_count, expected_resource_count,
                         "Resource count should match with the expected resource count")

        self.debug("Stopping instance: %s" % self.vm.name)
        try:
            self.vm.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)
        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count_after_stop = account_list[0].memorytotal

        self.assertEqual(resource_count, resource_count_after_stop,
                         "Resource count should be same after stopping the instance")

        self.debug("Starting instance: %s" % self.vm.name)
        try:
            self.vm.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start instance: %s" % e)
        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count_after_start = account_list[0].memorytotal

        self.assertEqual(resource_count, resource_count_after_start,
                         "Resource count should be same after stopping the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="true")
    def test_02_migrate_instance(self):
        """Test Deploy VM with specified RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with specified RAM & Deploy VM as root admin
        # 2. List Resource count for the root admin Memory usage
        # 3. Migrate vm, resource count should list properly.
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate feature is not supported on %s" % self.hypervisor.lower())

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].memorytotal

        expected_resource_count = int(self.services["service_offering"]["memory"])

        self.assertEqual(resource_count, expected_resource_count,
                         "Resource count should match with the expected resource count")

        host = findSuitableHostForMigration(self.apiclient, self.vm.id)
        if host is None:
            self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)
        self.debug("Migrating instance: %s to host: %s" % (self.vm.name, host.name))
        try:
            self.vm.migrate(self.apiclient, host.id)
        except Exception as e:
            self.fail("Failed to migrate instance: %s" % e)

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count_after_migrate = account_list[0].memorytotal

        self.assertEqual(resource_count, resource_count_after_migrate,
                         "Resource count should be same after stopping the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_03_delete_instance(self):
        """Test Deploy VM with specified GB RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with specified RAM & Deploy VM as root admin
        # 2. List Resource count for the root admin Memory usage
        # 3. Delete instance, resource count should be 0 after delete operation.

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].memorytotal

        expected_resource_count = int(self.services["service_offering"]["memory"])

        self.assertEqual(resource_count, expected_resource_count,
                         "Resource count should match with the expected resource count")

        self.debug("Destroying instance: %s" % self.vm.name)
        try:
            self.vm.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

	    # Wait for expunge interval to cleanup Memory
	    wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count_after_delete = account_list[0].memorytotal
        self.assertEqual(resource_count_after_delete, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=9))#RAM
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_04_deploy_multiple_vm(self):
        """Test Deploy multiple VM with specified RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with specified RAM
        # 2. Deploy multiple VMs with this service offering
        # 3. List Resource count for the root admin Memory usage
        # 4. Memory usage should list properly

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].memorytotal

        expected_resource_count = int(self.services["service_offering"]["memory"])

        self.assertEqual(resource_count, expected_resource_count,
                         "Resource count should match with the expected resource count")

        self.debug("Creating two instances with service offering: %s" %
                                                    self.service_offering.name)
        vm_1 = self.createInstance(service_off=self.service_offering)
        self.createInstance(service_off=self.service_offering)

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count_new = account_list[0].memorytotal

        expected_resource_count = int(self.services["service_offering"]["memory"]) * 3 #Total 3 VMs

        self.assertEqual(resource_count_new, expected_resource_count,
                         "Resource count should match with the expected resource count")

        self.debug("Destroying instance: %s" % vm_1.name)
        try:
            vm_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count_after_delete = account_list[0].memorytotal

        expected_resource_count -= int(self.services["service_offering"]["memory"])

        self.assertEqual(resource_count_after_delete, expected_resource_count,
                         "Resource count should match with the expected resource count")
        return

class TestDomainMemoryLimitsConfiguration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDomainMemoryLimitsConfiguration, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
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

        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createInstance(self, service_off, networks=None, api_client=None):
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
                                accountid=self.account.name,
                                domainid=self.account.domainid,
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

    def setupAccounts(self):

        self.debug("Creating a domain under: %s" % self.domain.name)
        self.child_domain_1 = Domain.create(self.apiclient,
                                            services=self.services["domain"],
                                            parentdomainid=self.domain.id)

        self.child_do_admin_1 = Account.create(
                                self.apiclient,
                                self.services["account"],
                                admin=True,
                                domainid=self.child_domain_1.id
                                )
        # Cleanup the resources created at end of test
        self.cleanup.append(self.child_do_admin_1)
        self.cleanup.append(self.child_domain_1)

        self.debug("Creating a domain under: %s" % self.domain.name)

        self.child_domain_2 = Domain.create(self.apiclient,
                                              services=self.services["domain"],
                                              parentdomainid=self.domain.id)

        self.child_do_admin_2 = Account.create(
                                    self.apiclient,
                                    self.services["account"],
                                    admin=True,
                                    domainid=self.child_domain_2.id)
        # Cleanup the resources created at end of test
        self.cleanup.append(self.child_do_admin_2)
        self.cleanup.append(self.child_domain_2)

        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_01_stop_start_instance(self):
        """Test Deploy VM with 5 GB memory & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB memory in child domains of root domain & Deploy VM
        # 2. List Resource count memory usage
        # 3. Stop and Start instance, check resource count.
        # 4. Resource count should list properly.

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.child_domain_1: self.child_do_admin_1,
                 self.child_domain_2: self.child_do_admin_2
                 }
        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain

            api_client = self.testClient.getUserApiClient(
                             UserName=self.account.name,
                             DomainName=self.account.domain)

            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)
            vm = self.createInstance(service_off=self.service_offering, api_client=api_client)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count = account_list[0].memorytotal

            expected_resource_count = int(self.services["service_offering"]["memory"])

            self.assertEqual(resource_count, expected_resource_count,
                         "Initial resource count should match with the expected resource count")

            self.debug("Stopping instance: %s" % vm.name)
            try:
                vm.stop(self.apiclient)
            except Exception as e:
                self.fail("Failed to stop instance: %s" % e)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count_after_stop = account_list[0].memorytotal

            self.assertEqual(resource_count, resource_count_after_stop,
                         "Resource count should be same after stopping the instance")

            self.debug("Starting instance: %s" % vm.name)
            try:
                vm.start(self.apiclient)
            except Exception as e:
                self.fail("Failed to start instance: %s" % e)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count_after_start = account_list[0].memorytotal

            self.assertEqual(resource_count_after_stop, resource_count_after_start,
                         "Resource count should be same after starting the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="true")
    def test_02_migrate_instance(self):
        """Test Deploy VM with specified memory & verify the usage"""

        # Validate the following
        # 1. Create compute offering with specified memory in child domains of root domain & Deploy VM
        # 2. List Resource count
        # 3. Migrate instance to another host
        # 4. Resource count should list properly.
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate feature is not supported on %s" % self.hypervisor.lower())

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.child_domain_1: self.child_do_admin_1,
                 self.child_domain_2: self.child_do_admin_2
                 }
        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain

            api_client = self.testClient.getUserApiClient(
                             UserName=self.account.name,
                             DomainName=self.account.domain)

            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)
            vm = self.createInstance(service_off=self.service_offering, api_client=api_client)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count = account_list[0].memorytotal

            expected_resource_count = int(self.services["service_offering"]["memory"])

            self.assertEqual(resource_count, expected_resource_count,
                         "Initial resource count should with the expected resource count")

            host = findSuitableHostForMigration(self.apiclient, vm.id)
            if host is None:
                self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)
            self.debug("Migrating instance: %s to host: %s" %
                                                        (vm.name, host.name))
            try:
                vm.migrate(self.apiclient, host.id)
            except Exception as e:
                self.fail("Failed to migrate instance: %s" % e)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count_after_migrate = account_list[0].memorytotal

            self.assertEqual(resource_count, resource_count_after_migrate,
                         "Resource count should be same after starting the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_03_delete_instance(self):
        """Test Deploy VM with specified RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with specified RAM in child domains of root domain & Deploy VM
        # 2. List Resource count for the Memory usage
        # 3. Delete instance
        # 4. Resource count should list as 0

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.child_domain_1: self.child_do_admin_1,
                 self.child_domain_2: self.child_do_admin_2
                 }
        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain

            api_client = self.testClient.getUserApiClient(
                             UserName=self.account.name,
                             DomainName=self.account.domain)

            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)
            vm = self.createInstance(service_off=self.service_offering, api_client=api_client)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count = account_list[0].memorytotal

            expected_resource_count = int(self.services["service_offering"]["memory"])

            self.assertEqual(resource_count, expected_resource_count,
                         "Initial resource count should match with the expected resource count")

            self.debug("Destroying instance: %s" % vm.name)
            try:
                vm.delete(self.apiclient)
            except Exception as e:
                self.fail("Failed to delete instance: %s" % e)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count = account_list[0].memorytotal
            self.assertEqual(resource_count, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=9))#RAM
        return

    @attr(tags=["advanced", "advancedns","simulator"])
    @attr(configuration='max.account.memory')
    def test_04_deploy_multiple_vm(self):
        """Test Deploy multiple VM with 2 GB memory & verify the usage"""
	    #keep the configuration value - max.account.memory = 8192 (maximum 4 instances per account with 2 GB RAM)

        # Validate the following
        # 1. Create compute offering with 2 GB RAM
        # 2. Deploy multiple VMs with this service offering in child domains of root domain
        # 3. List Resource count for the root admin Memory usage
        # 4. Memory usage should list properly

        self.debug("Creating service offering with 2 GB RAM")
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering"]
                                            )
        # Adding to cleanup list after execution
        self.cleanup.append(self.service_offering)

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.child_domain_1: self.child_do_admin_1,
                 self.child_domain_2: self.child_do_admin_2
                 }
        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain

            memory_account_gc = Resources.list(self.apiclient,
                                resourcetype = 9, #Memory
                                account = self.account.name,
                                domainid = self.domain.id
                                )

            if memory_account_gc[0].max != 8192:
                self.skipTest("This test case requires configuration value max.account.memory to be 8192")

	        api_client = self.testClient.getUserApiClient(
                             UserName=self.account.name,
                             DomainName=self.account.domain)

            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)
            vm_1 = self.createInstance(service_off=self.service_offering, api_client=api_client)
            vm_2 = self.createInstance(service_off=self.service_offering, api_client=api_client)
            self.createInstance(service_off=self.service_offering, api_client=api_client)
            self.createInstance(service_off=self.service_offering, api_client=api_client)

            self.debug("Deploying instance - memory capacity is fully utilized")
            with self.assertRaises(Exception):
                self.createInstance(service_off=self.service_offering, api_client=api_client)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count = account_list[0].memorytotal

            expected_resource_count = int(self.services["service_offering"]["memory"]) * 4 #Total 4 vms

            self.assertEqual(resource_count, expected_resource_count,
                         "Initial resource count should with the expected resource count")

            self.debug("Destroying instance: %s" % vm_1.name)
            try:
                vm_1.delete(self.apiclient)
            except Exception as e:
                self.fail("Failed to delete instance: %s" % e)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count_after_delete = account_list[0].memorytotal

            expected_resource_count -= int(self.services["service_offering"]["memory"])

            self.assertEqual(resource_count_after_delete, expected_resource_count,
                         "Resource count should match with the expected resource count")

            host = findSuitableHostForMigration(self.apiclient, vm_2.id)
            if host is None:
                self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)
            self.debug("Migrating instance: %s to host: %s" % (vm_2.name,
                                                               host.name))
            try:
                vm_2.migrate(self.apiclient, host.id)
            except Exception as e:
                self.fail("Failed to migrate instance: %s" % e)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                                  list,
                                  "List Accounts should return a valid response"
                                  )
            resource_count_after_migrate = account_list[0].memorytotal

            self.debug(resource_count_after_migrate)
            self.assertEqual(resource_count_after_delete, resource_count_after_migrate,
                         "Resource count should be same after migrating the instance")
        return
