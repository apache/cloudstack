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
                                        Resources,
                                        Domain
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

class TestMemoryLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestMemoryLimits,
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

        self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)
        self.vm = self.createInstance(service_off=self.service_offering)

        resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
        self.debug(resource_count) 
 
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

    @attr(tags=["advanced", "advancedns","simulator"]) 
    def test_01_reboot_instance(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM as root admin
        # 2. Update Resource count for the root admin Memory usage
        # 3. Stop and start instance, resource count should list properly. 
 
        self.debug("Stopping instance: %s" % self.vm.name)
        try:
            self.vm.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)
        resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
        self.debug(resource_count)

        self.debug("Starting instance: %s" % self.vm.name)
        try:
            self.vm.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start instance: %s" % e)
        resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
        self.debug(resource_count) 
        return

    @attr(tags=["advanced", "advancedns","simulator"]) 
    def test_02_migrate_instance(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM as root admin
        # 2. Update Resource count for the root admin Memory usage
        # 3. Migrate vm, resource count should list properly.

        host = find_suitable_host(self.apiclient, self.vm)
        self.debug("Migrating instance: %s to host: %s" % (self.vm.name, host.name))
        try:
            self.vm.migrate(self.apiclient, host.id)
        except Exception as e:
            self.fail("Failed to migrate instance: %s" % e)
        resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
        self.debug(resource_count) 
        return

    @attr(tags=["advanced", "advancedns","simulator"]) 
    def test_03_delete_instance(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM as root admin
        # 2. Update Resource count for the root admin Memory usage
        # 3. Delete instance, resource count should be 0 after delete operation. 

        self.debug("Destroying instance: %s" % self.vm.name)
        try:
            self.vm.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

	# Wait for expunge interval to cleanup Memory
	wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
        self.debug(resource_count)
        self.assertEqual(resource_count, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=9))#RAM
        return

    @attr(tags=["advanced", "advancedns","simulator"]) 
    def test_04_deploy_multiple_vm_with_5gb_ram(self):
        """Test Deploy multiple VM with 5 GB RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB RAM
        # 2. Deploy multiple VMs with this service offering
        # 3. Update Resource count for the root admin Memory usage
        # 4. Memory usage should list properly 

        self.debug("Creating instances with service offering: %s" %
                                                    self.service_offering.name)
        vm_1 = self.createInstance(service_off=self.service_offering)
        self.createInstance(service_off=self.service_offering) 
 
        resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
        self.debug(resource_count)
        self.debug("Destroying instance: %s" % vm_1.name)
        try:
            vm_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)
        resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
        self.debug(resource_count)
        return

class TestMemoryUpdateResources(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestMemoryUpdateResources,
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
        self.child_domain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)
        self.child_do_admin = Account.create(
                            self.apiclient,
                            self.services["account"],
                            admin=True,
                            domainid=self.child_domain.id
                            )
        # Cleanup the resources created at end of test
        self.cleanup.append(self.child_do_admin)
        self.cleanup.append(self.child_domain)

        self.debug("Updating the Memory resource count for domain: %s" %
                                                            self.domain.name)
        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=15360,
                              account=self.child_do_admin.name,
                              domainid=self.child_do_admin.domainid)

        self.debug("Creating a domain under: %s" % self.domain.name)

        self.domain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)
        self.admin = Account.create(
                                    self.apiclient,
                                    self.services["account"],
                                    admin=True,
                                    domainid=self.domain.id)
        # Cleanup the resources created at end of test	
	self.cleanup.append(self.admin)
        self.cleanup.append(self.domain)

        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=15360,
                              account=self.admin.name,
                              domainid=self.admin.domainid)
        return

    @attr(tags=["advanced", "advancedns","simulator"]) 
    def test_01_change_service_offering(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM as root admin
        # 2. Update Resource count for the root admin Memory usage
        # 3. Upgrade service offering, resource count should list properly.
        # 4. Downgrade service offering, resource count should list properly.

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.domain: self.admin,
                 self.child_domain: self.child_do_admin
                 }
        for domain, admin in users.items():
            self.account = admin
            self.domain = domain
            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)

	    api_client = self.testClient.createUserApiClient(
                            UserName=self.account.name,
                            DomainName=self.account.domain)

            vm = self.createInstance(service_off=self.service_offering, api_client=api_client)

            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)
            self.debug("Stopping instance: %s" % vm.name)
            try:
                vm.stop(self.apiclient)
            except Exception as e:
                self.fail("Failed to stop instance: %s" % e)
            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)

            self.debug("Creating service offering with 7 GB RAM")
            self.services["service_offering"]["memory"] = 7168
            self.service_offering_7gb = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering"])
            # Adding to cleanup list after execution
            self.cleanup.append(self.service_offering_7gb)

            self.debug(
                "Upgrade service offering of instance %s from %s to %s" %
                                            (vm.name,
                                             self.service_offering.name,
                                             self.service_offering_7gb.name))

            try:
                vm.change_service_offering(self.apiclient,
                                serviceOfferingId=self.service_offering_7gb.id)
            except Exception as e:
                self.fail("Failed to change service offering of vm %s - %s" %
                                                                (vm.name, e))
            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)

            self.debug(
                "Down grade service offering of instance %s from %s to %s" %
                                            (vm.name,
                                             self.service_offering_7gb.name,
                                             self.service_offering.name))

            try:
                vm.change_service_offering(self.apiclient,
                                serviceOfferingId=self.service_offering.id)
            except Exception as e:
                self.fail("Failed to change service offering of vm %s - %s" %
                                                                (vm.name, e))
            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)

            self.debug("Starting instance: %s" % vm.name)
            try:
                vm.start(self.apiclient)
            except Exception as e:
                self.fail("Failed to start instance: %s" % e)
            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count) 
        return

    @attr(tags=["advanced", "advancedns","simulator"]) 
    def test_02_migrate_instance(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM as root admin
        # 2. Update Resource count for the root admin Memory usage 
        # 3. Migrate vm, resource count should list properly.

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.domain: self.admin,
                 self.child_domain: self.child_do_admin
                 }
        for domain, admin in users.items():
            self.account = admin
            self.domain = domain
            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)

	    api_client = self.testClient.createUserApiClient(
                            UserName=self.account.name,
                            DomainName=self.account.domain)

            vm = self.createInstance(service_off=self.service_offering, api_client=api_client)

            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)
        
            host = find_suitable_host(self.apiclient, vm)
            self.debug("Migrating instance: %s to host: %s" %
                                                        (vm.name, host.name))
            try:
                vm.migrate(self.apiclient, host.id)
            except Exception as e:
                self.fail("Failed to migrate instance: %s" % e)
            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)

            self.debug("Assigning VM to account: %s in domain: %s" %
                                                (self.admin.name,
                                                self.admin.domain))
            # TODO: Assign Virtual Machine function implementation 
        return

    @attr(tags=["advanced", "advancedns","simulator"]) 
    def test_03_delete_instance(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM as root admin
        # 2. Update Resource count for the root admin Memory usage
        # 3. Delete instance, resource count should be 0 after delete operation. 

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users = {self.domain: self.admin,
                 self.child_domain: self.child_do_admin
                 }
        for domain, admin in users.items():
            self.account = admin
            self.domain = domain
            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)

	    api_client = self.testClient.createUserApiClient(
                            UserName=self.account.name,
                            DomainName=self.account.domain)

            vm = self.createInstance(service_off=self.service_offering, api_client=api_client)

            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count) 

            self.debug("Destroying instance: %s" % vm.name)
            try:
                vm.delete(self.apiclient)
            except Exception as e:
                self.fail("Failed to delete instance: %s" % e)

	    # Wait for expunge interval to cleanup Memory
            wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)
            self.assertEqual(resource_count, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=9))#RAM
        return

    @attr(tags=["advanced", "advancedns","simulator"]) 
    def test_04_deploy_multiple_vm_with_5gb_ram(self):
        """Test Deploy multiple VM with 5 GB RAM& verify the usage"""

        # Validate the following
        # 1. Create compute offering with 5 GB RAM
        # 2. Deploy multiple VMs with this service offering
        # 3. Update Resource count for the root admin Memory usage
        # 4. Memory usage should list properly 

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users =  { self.domain: self.admin,
                   self.child_domain: self.child_do_admin
                 }
        for domain, admin in users.items():
            self.account = admin
            self.domain = domain

	    api_client = self.testClient.createUserApiClient(
                            UserName=self.account.name,
                            DomainName=self.account.domain)

            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)
            vm_1 = self.createInstance(service_off=self.service_offering, api_client=api_client)
            vm_2 = self.createInstance(service_off=self.service_offering, api_client=api_client)
            vm_3 = self.createInstance(service_off=self.service_offering, api_client=api_client) 

            self.debug("Deploying instance - Memory capacity is fully utilized")
            with self.assertRaises(Exception):
                self.createInstance(service_off=self.service_offering, api_client=api_client)

            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)
            self.debug("Destroying instance: %s" % vm_1.name)
            try:
                vm_1.delete(self.apiclient)
            except Exception as e:
                self.fail("Failed to delete instance: %s" % e)
            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)

            host = find_suitable_host(self.apiclient, vm_2)
            self.debug("Migrating instance: %s to host: %s" % (vm_2.name,
                                                               host.name))
            try:
                vm_2.migrate(self.apiclient, host.id)
            except Exception as e:
                self.fail("Failed to migrate instance: %s" % e)
            resource_count = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
            self.debug(resource_count)

	    vm_2.delete(self.apiclient)
	    vm_3.delete(self.apiclient)  
        return 
