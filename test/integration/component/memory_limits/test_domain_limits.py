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

class TestDomainMemoryLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestDomainMemoryLimits,
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

        self.debug("Creating a sub-domain under: %s" % self.domain.name)
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

        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=5120,
                              account=self.child_do_admin.name,
                              domainid=self.child_do_admin.domainid)
	
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
                              max=5120,
                              account=self.admin.name,
                              domainid=self.admin.domainid)
        return

    @attr(tags=["advanced", "advancedns","simulator"])
    @attr(configuration='max.account.memory')
    def test_01_change_service_offering(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""
	#Keep max account memory (configuration) as 5 GB
        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM in the created domain
        # 2. Update Resource count for the root admin Memory usage
        # 3. Upgrade and downgrade service offering
        # 4. Resource count should list properly for the domain 
 
	#Keep max account memory (configuration) as 5 GB

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users =  { self.domain: self.admin,
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
                                            self.services["service_offering"]
                                            )
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
            resource_count  = get_updated_resource_count(self.apiclient, account=self.account, rtype=9)#RAM
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
    @attr(configuration='max.account.memory')
    def test_02_migrate_vm(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""
	#Keep max account memory (configuration) as 5 GB
        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM in the created domain
        # 2. Update Resource count for the root admin Memory usage
        # 3. Migrate vm to another host, resource count should list properly.
 
	#Keep max account memory (configuration) as 5 GB
        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users =  { self.domain: self.admin,
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
        return

    @attr(tags=["advanced", "advancedns","simulator"])
    @attr(configuration='max.account.memory')
    def test_03_delete_vm(self):
        """Test Deploy VM with 5 GB RAM & verify the usage"""
	#Keep max account memory (configuration) as 5 GB
        # Validate the following
        # 1. Create compute offering with 5 GB RAM & Deploy VM in the created domain
        # 2. Update Resource count for the root admin Memory usage
        # 3. Delete vm, resource count should list as 0 after delete operation.
 
	#Keep max account memory (configuration) as 5 GB

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()
        users =  { self.domain: self.admin,
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
    @attr(configuration='max.account.memory')
    def test_04_deploy_multiple_vm(self):
        """Test Deploy multiple VM with 5 GB RAM & verify the usage"""
	#Keep max account memory (configuration) as 5 GB
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
            self.debug("Creating an instance with service offering: %s" %
                                                    self.service_offering.name)

	    api_client = self.testClient.createUserApiClient(
                            UserName=self.account.name,
                            DomainName=self.account.domain)

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


class TestMultipleChildDomainsMemory(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestMultipleChildDomainsMemory,
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
        self.debug("Deploying an instance in account: %s" %
                                                account.name)

	if api_client is None:
	    api_client = self.apiclient

        try:
            vm = VirtualMachine.create(
                                api_client,
                                self.services["virtual_machine"],
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
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)
        self.parentd_admin = Account.create(
                            self.apiclient,
                            self.services["account"],
                            admin=True,
                            domainid=self.domain.id
                            )
         
        self.debug("Updating the Memory resource count for domain: %s" %
                                                            self.domain.name)
        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=10240,
                              account=self.parentd_admin.name,
                              domainid=self.parentd_admin.domainid)
        self.debug("Creating a sub-domain under: %s" % self.parent_domain.name)
        self.cdomain_1 = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.parent_domain.id)

        self.debug("Creating a sub-domain under: %s" % self.parent_domain.name)
        self.cdomain_2 = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.parent_domain.id)

        self.cadmin_1 = Account.create(
                                        self.apiclient,
                                        self.services["account"],
                                        admin=True,
                                        domainid=self.cdomain_1.id
                                        )
                
        self.debug("Updating the Memory resource count for domain: %s" %
                                                        self.cdomain_1.name)
        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=5120, 
                              domainid=self.cadmin_1.domainid)

        self.debug("Updating the Memory resource count for account: %s" %
                                                        self.cadmin_1.name)
        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=2148,
                              account=self.cadmin_1.name,
                              domainid=self.cadmin_1.domainid)

        self.cadmin_2 = Account.create(
                                        self.apiclient,
                                        self.services["account"],
                                        admin=True,
                                        domainid=self.cdomain_2.id
                                        )

        self.debug("Updating the Memory resource count for domain: %s" %
                                                        self.cdomain_2.name)
        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=5120, 
                              domainid=self.cadmin_2.domainid)

        self.debug("Updating the Memory resource count for domain: %s" %
                                                        self.cadmin_2.name)
        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=2148,
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

    @attr(tags=["advanced", "advancedns","simulator"])
    def test_01_multiple_child_domains(self):
        """Test memory limits with multiple child domains"""

        # Validate the following
        # 1. Create Domain1 with 10 GB RAM and 2 child domains with 5 GB
        #    each.Assign 2 GB for Domain1 admin1 & Domain1 User1 .Assign 2
        #    GB for Domain2 admin1 & Domain2 User1
        # 2. Deploy VM's by Domain1 admin1/user1/ Domain2 user1/Admin1 account
        #    and verify the resource updates
        # 3. Deploy VM by admin account after reaching max parent domain limit
        # 4. Deploy VM with child account after reaching max child domain limit
        # 5. Delete user account and verify the resource updates
        # 6. Destroy user/admin account VM's and verify the child & Parent
        #    domain resource updates

        self.debug("Creating service offering with 2 GB RAM")
        self.services["service_offering"]["memory"] = 2048
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering"]
                                            )
        # Adding to cleanup list after execution
        self.cleanup.append(self.service_offering)

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts()

	api_client_cadmin_1 = self.testClient.createUserApiClient(
                            UserName=self.cadmin_1.name,
                            DomainName=self.cadmin_1.domain)

	api_client_cadmin_2 = self.testClient.createUserApiClient(
                            UserName=self.cadmin_2.name,
                            DomainName=self.cadmin_2.domain)

        self.debug("Creating an instance with service offering: %s" %
                                                self.service_offering.name)
        vm_1 = self.createInstance(account=self.cadmin_1,
                                  service_off=self.service_offering, api_client=api_client_cadmin_1)

        vm_2 = self.createInstance(account=self.cadmin_2,
                                  service_off=self.service_offering, api_client=api_client_cadmin_2)

        resource_count_cadmin_1 = get_updated_resource_count(self.apiclient, account=self.cadmin_1, rtype=9)#RAM
        self.debug(resource_count_cadmin_1)

        resource_count_cadmin_2 = get_updated_resource_count(self.apiclient, account=self.cadmin_2, rtype=9)#RAM
        self.debug(resource_count_cadmin_2)

        self.debug(
            "Creating instance when Memory limit is fully used in parent domain")
        with self.assertRaises(Exception):
            self.createInstance(account=self.cadmin_1,
                                  service_off=self.service_offering, api_client=api_client_cadmin_1)

        self.debug(
            "Creating instance when Memory limit is fully used in child domain")
        with self.assertRaises(Exception):
            self.createInstance(account=self.cadmin_1,
                                  service_off=self.service_offering, api_client=api_client_cadmin_1)
        self.debug("Destroying instances: %s, %s" % (vm_1.name, vm_2.name))
        try:
            vm_1.delete(self.apiclient)
            vm_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

        self.debug("Checking resource count for account: %s" % self.cadmin_1.name)

        resource_count_cadmin_1 = get_updated_resource_count(self.apiclient, account=self.cadmin_1, rtype=9)#RAM
        self.debug(resource_count_cadmin_1)

        self.assertEqual(resource_count_cadmin_1, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=9))#RAM

        self.debug("Checking resource count for account: %s" % self.cadmin_2.name)

        resource_count_cadmin_2 = get_updated_resource_count(self.apiclient, account=self.cadmin_2, rtype=9)#RAM
        self.debug(resource_count_cadmin_2)

        self.assertEqual(resource_count_cadmin_2, 0 , "Resource count for %s should be 0" % get_resource_type(resource_id=9))#RAM
        return
