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

""" Tests for cpu resource limits related to domains
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackAPI import createServiceOffering
from marvin.cloudstackTestCase import cloudstackTestCase
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
                                        findSuitableHostForMigration,
                                        get_resource_type
                                        )
from marvin.lib.utils import cleanup_resources
from marvin.codes import ERROR_NO_HOST_FOR_MIGRATION

class TestDomainCPULimitsUpdateResources(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDomainCPULimitsUpdateResources, cls).getClsTestClient()
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

        # Create service, disk offerings  etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering_multiple_cores"]
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

        self.child_domain = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id
        )
        self.child_do_admin = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.child_domain.id
        )
        # Cleanup the resources created at end of test
        self.cleanup.append(self.child_do_admin)
        self.cleanup.append(self.child_domain)

        Resources.updateLimit(
            self.apiclient,
            resourcetype=8,
            max=16,
            account=self.child_do_admin.name,
            domainid=self.child_do_admin.domainid
        )

        self.domain = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id
        )

        self.admin = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        # Cleanup the resources created at end of test
        self.cleanup.append(self.admin)
        self.cleanup.append(self.domain)

        Resources.updateLimit(
            self.apiclient,
            resourcetype=8,
            max=16,
            account=self.admin.name,
            domainid=self.admin.domainid
        )
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_01_multiple_core_vm_start_stop_instance(self):
        """Test Deploy VM with 4 core CPU & verify the usage"""

        # Validate the following
        # 1. Create two domains and set specific resource (cpu) limit for them
        # 2. Create compute offering with 4 core CPU & deploy vm
        # 3. Update Resource count for the domains
        # 4. Reboot instance and check resource count
        # 5. Resource count should list properly.

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.domain: self.admin,
                 self.child_domain: self.child_do_admin
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
                "Resource count should be same as before, after stopping the instance")

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
                "Resource count should be same as before, after starting the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="true")
    def test_02_multiple_core_vm_migrate_instance(self):
        """Test Deploy VM with 4 core CPU & verify the usage"""

        # Validate the following
        # 1. Create two domains and set specific resource (cpu) limit for them
        # 2. Create compute offering with 4 core CPU & deploy vm
        # 3. Update Resource count for the domains
        # 4. Migrate instance to new host and check resource count
        # 5. Resource count should list properly.
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.domain: self.admin,
                 self.child_domain: self.child_do_admin
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
                "Resource count should be same as before, after migrating the instance")
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_03_multiple_core_vm_delete_instance(self):
        """Test Deploy VM with 4 core CPU & verify the usage"""

        # Validate the following
        # 1. Create two domains and set specific resource (cpu) limit for them
        # 2. Create compute offering with 4 core CPU & deploy vm
        # 3. Update Resource count for the domains
        # 4. delete instance and check resource count
        # 5. Resource count should list properly.

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.domain: self.admin,
                 self.child_domain: self.child_do_admin
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
            resource_count_after_delete = account_list[0].cputotal

            self.assertEqual(resource_count_after_delete, 0,
                "Resource count for %s should be 0" % get_resource_type(resource_id=8))#CPU
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_04_deploy_multiple_vm_with_multiple_core(self):
        """Test Deploy multiple VM with 4 core CPU & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 4 core CPU
        # 2. Deploy multiple VMs within domain with this service offering
        # 3. Update Resource count for the domain
        # 4. CPU usage should list properly
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.debug("Creating service offering with 4 CPU cores")
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.testdata["service_offering_multiple_cores"]
                                            )
        # Adding to cleanup list after execution
        self.cleanup.append(self.service_offering)

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.domain: self.admin,
                 self.child_domain: self.child_do_admin
                 }
        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain

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
            cmd = self.testdata["service_offering_multiple_cores"]
            cmd['cpunumber'] = '20'

            self.so = ServiceOffering.create(
                self.api_client,
                cmd
            )
            self.cleanup.append(self.so)

            with self.assertRaises(Exception):
                self.createInstance(service_off=self.so, api_client=api_client)

            account_list = Account.list(self.apiclient, id=self.account.id)
            self.assertIsInstance(account_list,
                list,
                "List Accounts should return a valid response"
            )
            resource_count = account_list[0].cputotal

            expected_resource_count = int(self.service_offering.cpunumber) * 4 #Total 4 VMs

            self.assertEqual(resource_count, expected_resource_count,
                "Initial resource count should be 4")

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
                "Resource count should match with the expected count")
            #vm migration is not supported in LXC. So don't need to execute below vm migration code
            if self.hypervisor.lower() in ['lxc']:
                continue
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

            self.assertEqual(resource_count_after_migrate, resource_count_after_delete,
                "Resource count should not change after migrating the instance")
        return

class TestMultipleChildDomains(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMultipleChildDomains, cls).getClsTestClient()
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

        # Create service, disk offerings  etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering_multiple_cores"]
        )
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.testdata["service_offering_multiple_cores"]
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
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createInstance(self, account, service_off, networks=None, api_client=None):
        """Creates an instance in account"""

        if api_client is None:
            api_client = self.apiclient

        self.debug("Deploying an instance in account: %s" %
                       account.name)
        try:
            vm = VirtualMachine.create(
                 api_client,
                 self.testdata["virtual_machine"],
                 templateid=self.template.id,
                 accountid=account.name,
                 domainid=account.domainid,
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

        self.parent_domain = Domain.create(self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id)
        self.parentd_admin = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.debug("Updating the Memory resource limit for domain: %s" %
                   self.domain.name)
        Resources.updateLimit(self.apiclient,
            resourcetype=8,
            max=10,
            domainid=self.parentd_admin.domainid,
            account=self.parentd_admin.name)
        self.debug("Creating a sub-domain under: %s" % self.parent_domain.name)
        self.cdomain_1 = Domain.create(self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.parent_domain.id)

        self.debug("Creating a sub-domain under: %s" % self.parent_domain.name)
        self.cdomain_2 = Domain.create(self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.parent_domain.id)

        self.cadmin_1 = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.cdomain_1.id
        )

        self.debug("Updating the Memory resource count for domain: %s" %
                   self.cdomain_1.name)
        Resources.updateLimit(self.apiclient,
            resourcetype=8,
            max=4,
            domainid=self.cadmin_1.domainid)

        self.debug("Updating the Memory resource count for account: %s" %
                   self.cadmin_1.name)
        Resources.updateLimit(self.apiclient,
            resourcetype=8,
            max=2,
            account=self.cadmin_1.name,
            domainid=self.cadmin_1.domainid)

        self.cadmin_2 = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.cdomain_2.id
        )

        self.debug("Updating the Memory resource count for domain: %s" %
                   self.cdomain_2.name)
        Resources.updateLimit(self.apiclient,
            resourcetype=8,
            max=5,
            domainid=self.cadmin_2.domainid)

        self.debug("Updating the Memory resource count for account: %s" %
                   self.cadmin_2.name)
        Resources.updateLimit(self.apiclient,
            resourcetype=8,
            max=3,
            account=self.cadmin_2.name,
            domainid=self.cadmin_2.domainid)
        # Cleanup the resources created at end of test
        self.cleanup.append(self.cadmin_1)
        self.cleanup.append(self.cadmin_2)
        self.cleanup.append(self.cdomain_1)
        self.cleanup.append(self.cdomain_2)
        self.cleanup.append(self.parentd_admin)
        self.cleanup.append(self.parent_domain)

        users = {
            self.parent_domain: self.parentd_admin,
            self.cdomain_1: self.cadmin_1,
            self.cdomain_2: self.cadmin_2
        }
        return users

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_01_multiple_child_domains(self):
        """Test CPU limits with multiple child domains"""

        # Validate the following
        # 1. Create Domain1 with 10 core CPU and 2 child domains with 4 core
        #    each.Assign 2 cores for Domain1 admin1 & Domain1 User1 .Assign 2
        #    cores for Domain2 admin1 & Domain2 User1
        # 2. Deploy VM's by Domain1 admin1/user1/ Domain2 user1/Admin1 account
        #    and verify the resource updates
        # 3. Deploy VM by admin account after reaching max parent domain limit
        # 4. Deploy VM with child account after reaching max child domain limit
        # 5. Destroy user/admin account VM's and verify the child & Parent
        #    domain resource updates

        self.debug("Creating service offering with 2 CPU cores")
        self.testdata["service_offering"]["cpunumber"] = 2
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.testdata["service_offering"]
                                            )
        # Adding to cleanup list after execution
        self.cleanup.append(self.service_offering)

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()

        api_client_cadmin_1 = self.testClient.getUserApiClient(
            UserName=self.cadmin_1.name,
            DomainName=self.cadmin_1.domain)

        api_client_cadmin_2 = self.testClient.getUserApiClient(
            UserName=self.cadmin_2.name,
            DomainName=self.cadmin_2.domain)

        self.debug("Creating an instance with service offering: %s" %
                   self.service_offering.name)
        vm_1 = self.createInstance(account=self.cadmin_1,
            service_off=self.service_offering, api_client=api_client_cadmin_1)

        vm_2 = self.createInstance(account=self.cadmin_2,
            service_off=self.service_offering, api_client=api_client_cadmin_2)

        self.debug("Checking resource count for account: %s" % self.cadmin_1.name)

        account_list = Account.list(self.apiclient, id=self.cadmin_1.id)
        self.assertIsInstance(account_list,
            list,
            "List Accounts should return a valid response"
        )
        resource_count_cadmin_1 = account_list[0].cputotal

        self.debug(resource_count_cadmin_1)

        self.debug("Checking resource count for account: %s" % self.cadmin_2.name)
        account_list = Account.list(self.apiclient, id=self.cadmin_2.id)
        self.assertIsInstance(account_list,
            list,
            "List Accounts should return a valid response"
        )
        resource_count_cadmin_2 = account_list[0].cputotal

        self.debug(resource_count_cadmin_2)

        self.debug(
            "Creating instance when CPU limit is fully used in child domain 1")
        with self.assertRaises(Exception):
            self.createInstance(account=self.cadmin_1,
                service_off=self.service_offering, api_client=api_client_cadmin_1)

        self.debug(
            "Creating instance when CPU limit is fully used in child domain 2")
        with self.assertRaises(Exception):
            self.createInstance(account=self.cadmin_2,
                service_off=self.service_offering, api_client=api_client_cadmin_2)
        self.debug("Destroying instances: %s, %s" % (vm_1.name, vm_2.name))
        try:
            vm_1.delete(self.apiclient)
            vm_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

        self.debug("Checking resource count for account: %s" % self.cadmin_1.name)

        account_list = Account.list(self.apiclient, id=self.cadmin_1.id)
        self.assertIsInstance(account_list,
            list,
            "List Accounts should return a valid response"
        )
        resource_count_cadmin_1 = account_list[0].cputotal

        self.debug(resource_count_cadmin_1)
        self.assertEqual(resource_count_cadmin_1, 0, "Resource count for %s should be 0" % get_resource_type(resource_id=8))#CPU

        self.debug("Checking resource count for account: %s" % self.cadmin_2.name)
        account_list = Account.list(self.apiclient, id=self.cadmin_2.id)
        self.assertIsInstance(account_list,
            list,
            "List Accounts should return a valid response"
        )
        resource_count_cadmin_2 = account_list[0].cputotal

        self.debug(resource_count_cadmin_2)
        self.assertEqual(resource_count_cadmin_2, 0, "Resource count for %s should be 0" % get_resource_type(resource_id=8))#CPU
        return
