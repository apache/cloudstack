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

""" Tests for cpu resource limits
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (
                                        Account,
                                        ServiceOffering,
                                        VirtualMachine,
                                        Domain,
                                        Resources
                                        )
from marvin.lib.common import (get_domain,
                                        get_zone,
                                        get_template,
                                        findSuitableHostForMigration,
                                        get_resource_type
                                        )
from marvin.lib.utils import cleanup_resources
from marvin.codes import ERROR_NO_HOST_FOR_MIGRATION




class TestCPULimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCPULimits, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        #cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata["mode"] = cls.zone.networktype
        #cls.services["mode"] = cls.zone.networktype

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
        """Creates an instance in account
        """
        if api_client is None:
            api_client = self.apiclient

        self.debug("Deploying an instance in account: %s" %
                       self.account.name)
        try:
            vm = VirtualMachine.create(
                    api_client,
                    self.testdata["virtual_machine"],
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
    def test_01_multiplecore_start_stop_instance(self):
        """Test Deploy VM with multiple core CPU & verify the usage"""

        # Validate the following
        # 1. Deploy VM with multiple core CPU & verify the usage
        # 2. Stop VM & verify the update resource count of Root Admin Account
        # 3. Start VM & verify the update resource count of Root Admin Account
        # 4. Resource count should list properly.

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

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
        resource_count_after_stop = account_list[0].cputotal

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
        resource_count_after_start = account_list[0].cputotal

        self.assertEqual(resource_count, resource_count_after_start,
                         "Resource count should be same after stopping the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="true")
    def test_02_multiplecore_migrate_instance(self):
        """Test Deploy VM with multiple core CPU & verify the usage"""

        # Validate the following
        # 1. Deploy VM with multiple core CPU & verify the usage
        # 2. Migrate VM & verify updated resource count of Root Admin Account
        # 3. Resource count should list properly.
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

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
        resource_count_after_migrate = account_list[0].cputotal

        self.assertEqual(resource_count, resource_count_after_migrate,
                         "Resource count should be same after migrating the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_03_multiplecore_delete_instance(self):
        """Test Deploy VM with multiple core CPU & verify the usage"""

        # Validate the following
        # 1. Deploy VM with multiple core CPU & verify the usage
        # 2. Destroy VM & verify update resource count of Root Admin Account
        # 3. Resource count should list properly.

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

        self.assertEqual(resource_count, expected_resource_count,
                         "Resource count should match with the expected resource count")

        self.debug("Destroying instance: %s" % self.vm.name)
        try:
            self.vm.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].cputotal
        self.assertEqual(resource_count, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=8))#CPU
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="true")
    def test_04_deploy_multiple_vm_with_multiple_cpus(self):
        """Test Deploy multiple VM with 4 core CPU & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 4 core CPU
        # 2. Deploy multiple VMs with this service offering
        # 3. List Resource count for the root admin CPU usage
        # 4. CPU usage should list properly
        # 5. Destroy one VM among multiple VM's and verify the resource count
        # 6. Migrate VM from & verify resource updates
        # 7. List resource count for Root Admin
        # 8. Failed to deploy VM and verify the resource usage

        self.debug("Creating service offering with 4 CPU cores")
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.testdata["service_offering_multiple_cores"]
                                            )
        # Adding to cleanup list after execution
        self.cleanup.append(self.service_offering)

        self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)
        vm_1 = self.createInstance(service_off=self.service_offering)
        vm_2 = self.createInstance(service_off=self.service_offering)
        self.createInstance(service_off=self.service_offering)

        account_list = Account.list(self.apiclient, id=self.account.id)
        self.assertIsInstance(account_list,
                              list,
                              "List Accounts should return a valid response"
                              )
        resource_count = account_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber) * 4 #Total 4 Vms
        self.assertTrue(resource_count == expected_resource_count,
                         "Resource count does not match the expected vavlue")
        return

class TestDomainCPULimitsConfiguration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDomainCPULimitsConfiguration, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        #cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.testdata["mode"] = cls.zone.networktype
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.testdata["ostype"]
                            )

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.testdata["service_offering_multiple_cores"]
                                            )

        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id

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
        """Creates an instance in account
        """
        if api_client is None:
            api_client = self.apiclient

        self.debug("Deploying an instance in account: %s" %
                       self.account.name)
        try:
            vm = VirtualMachine.create(
                    api_client,
                    self.testdata["virtual_machine"],
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

        self.debug("Creating a sub-domain under: %s" % self.domain.name)
        self.child_domain_1 = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id
        )
        self.child_do_admin_1 = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.child_domain_1.id
        )
        # Cleanup the resources created at end of test
        self.cleanup.append(self.child_do_admin_1)
        self.cleanup.append(self.child_domain_1)

        self.child_domain_2 = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id
        )

        self.child_do_admin_2 = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.child_domain_2.id
        )

        # Cleanup the resources created at end of test
        self.cleanup.append(self.child_do_admin_2)
        self.cleanup.append(self.child_domain_2)

        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_01_stop_start_instance(self):
        """Test Deploy VM with 4 core CPU & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 4 core CPU & Deploy VM
        # 2. List Resource count CPU usage
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
        resource_count = account_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

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
        resource_count_after_stop = account_list[0].cputotal

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
        resource_count_after_start = account_list[0].cputotal

        self.assertEqual(resource_count_after_stop, resource_count_after_start,
            "Resource count should be same after starting the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="true")
    def test_02_migrate_instance(self):
        """Test Deploy VM with 4 core CPU & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 4 core CPU & Deploy VM
        # 2. List Resource count
        # 3. Migrate instance to another host
        # 4. Resource count should list properly.
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

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
        resource_count = account_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

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
        resource_count_after_migrate = account_list[0].cputotal

        self.assertEqual(resource_count, resource_count_after_migrate,
            "Resource count should be same after starting the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_03_delete_instance(self):
        """Test Deploy VM with 4 core CPU & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 4 core CPU & Deploy VM
        # 2. List Resource count for the CPU usage
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
        resource_count = account_list[0].cputotal

        expected_resource_count = int(self.service_offering.cpunumber)

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
        resource_count = account_list[0].cputotal
        self.assertEqual(resource_count, 0, "Resource count for %s should be 0" % get_resource_type(resource_id=8))#CPU
        return

    @attr(tags=["advanced", "advancedns","simulator"])
    @attr(configuration='max.account.cpus')
    def test_04_deploy_multiple_vm_with_multiple_cpus(self):
        """Test Deploy multiple VM with 4 core CPU & verify the usage"""
        #keep the configuration value - max.account.cpus number = 16
        # Validate the following
        # 1. Create compute offering with 4 core CPU
        # 2. Deploy multiple VMs with this service offering
        # 3. List Resource count for the root admin CPU usage
        # 4. CPU usage should list properly

        self.debug("Creating service offering with 4 CPU cores")
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.testdata["service_offering_multiple_cores"]
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

            cpu_account_gc = Resources.list(self.apiclient,
                                resourcetype = 8, #CPU
                                account = self.account.name,
                                domainid = self.domain.id
                                )

            if cpu_account_gc[0].max != 16:
                self.skipTest("This test case requires configuration value max.account.cpus to be 16")

            api_client = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain)

            self.debug("Creating an instance with service offering: %s" %
                       self.service_offering.name)
            vm_1 = self.createInstance(service_off=self.service_offering, api_client=api_client)
            vm_2 = self.createInstance(service_off=self.service_offering, api_client=api_client)
            self.createInstance(service_off=self.service_offering, api_client=api_client)
            self.createInstance(service_off=self.service_offering, api_client=api_client)

            self.debug("Deploying instance - CPU capacity is fully utilized")
            with self.assertRaises(Exception):
                self.createInstance(service_off=self.service_offering, api_client=api_client)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                list,
                "List Accounts should return a valid response"
            )
            resource_count = account_list[0].cputotal

            expected_resource_count = int(self.service_offering.cpunumber) * 4 #Total 4 vms

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
            resource_count_after_delete = account_list[0].cputotal

            expected_resource_count -= int(self.service_offering.cpunumber)

            self.assertEqual(resource_count_after_delete, expected_resource_count,
                "Resource count should be less than before after deleting the instance")

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
            resource_count_after_migrate = account_list[0].cputotal

            self.debug(resource_count_after_migrate)
            self.assertEqual(resource_count_after_delete, resource_count_after_migrate,
                "Resource count should be same after migrating the instance")
