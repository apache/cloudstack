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
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (
                                        Account,
                                        ServiceOffering,
                                        VirtualMachine,
                                        Resources,
                                        Domain,
                                        Project
                                        )
from marvin.lib.common import (get_domain,
                                        get_zone,
                                        get_template
                                        )
from marvin.lib.utils import cleanup_resources

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

class TestMaxMemoryLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMaxMemoryLimits, cls).getClsTestClient()
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

        self.debug("Creating service offering with 5 GB RAM")

        self.cleanup = [self.account, ]
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createInstance(self, service_off, account=None,
                        project=None, networks=None, api_client=None):
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
                                accountid=account.name if account else None,
                                domainid=account.domainid if account else None,
                                projectid=project.id if project else None,
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

    def setupAccounts(self, account_limit=2, domain_limit=2, project_limit=2):

        self.debug("Creating a domain under: %s" % self.domain.name)
        self.child_domain = Domain.create(self.apiclient,
                                    services=self.services["domain"],
                                    parentdomainid=self.domain.id)


        self.debug("domain crated with domain id %s" % self.child_domain.id)

        self.child_do_admin = Account.create(
                        self.apiclient,
                        self.services["account"],
                        admin=True,
                        domainid=self.child_domain.id
                        )

        self.debug("domain admin created for domain id %s" %
        self.child_do_admin.domainid)

        # Create project as a domain admin
        self.project = Project.create(self.apiclient,
                             self.services["project"],
                             account=self.child_do_admin.name,
                             domainid=self.child_do_admin.domainid)

        # Cleanup created project at end of test
        self.cleanup.append(self.project)


        # Cleanup accounts created
        self.cleanup.append(self.child_do_admin)
        self.cleanup.append(self.child_domain)

        self.debug("Updating the Memory resource count for domain: %s" %
                                                        self.child_domain.name)
        # Update resource limits for account 1
        responses = Resources.updateLimit(self.apiclient,
                          resourcetype=9,
                          max=(account_limit * 1024),
                          account=self.child_do_admin.name,
                          domainid=self.child_do_admin.domainid
                          )

        self.debug("Memory Resource count for child domain admin account is now: %s" %
                    responses.max)

        # Update resource limits for project
        responses = Resources.updateLimit(self.apiclient,
                          resourcetype=9,
                          max=(project_limit * 1024),
                          projectid=self.project.id)

        self.debug("Memory Resource count for project is now")
        self.debug(responses.max)

        # TODO: Update the Memory limit for domain only
        responses = Resources.updateLimit(self.apiclient,
                                          resourcetype=9,
                                          max=(domain_limit * 1024),
                                          domainid=self.child_domain.id)

        self.debug("Memory Resource count for domain %s with id %s is now %s" %
                    (responses.domain, responses.domainid, responses.max))
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_01_deploy_vm_domain_limit_reached(self):
        """Test Try to deploy VM with admin account where account has not used
           the resources but @ domain they are not available"""

        # Validate the following
        # 1. Try to deploy VM with admin account where account has not used the
        #    resources but @ domain they are not available
        # 2. Deploy VM should error out saying  ResourceAllocationException
        #    with "resource limit exceeds"

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts(account_limit=8, domain_limit=4)

        api_client = self.testClient.getUserApiClient(
                            UserName=self.child_do_admin.name,
                            DomainName=self.child_do_admin.domain)

        self.debug("Creating instance with domain %s and admin account %s" %
                   (self.child_do_admin.domainid,
                     self.child_do_admin.name))

        with self.assertRaises(Exception):
            self.createInstance(account=self.child_do_admin,
                              service_off=self.service_offering, api_client=api_client)
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_02_deploy_vm_account_limit_reached(self):
        """Test Try to deploy VM with admin account where account has used
           the resources but @ domain they are available"""

        # Validate the following
        # 1. Try to deploy VM with admin account where account has used the
        #    resources but @ domain they are available
        # 2. Deploy VM should error out saying  ResourceAllocationException
        #    with "resource limit exceeds"

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts(account_limit=7, domain_limit=14)

        api_client = self.testClient.getUserApiClient(
                            UserName=self.child_do_admin.name,
                            DomainName=self.child_do_admin.domain)

        self.debug("Deploying instance with account: %s" %
                                                self.child_do_admin.name)
        self.createInstance(account=self.child_do_admin,
                              service_off=self.service_offering, api_client=api_client)

        self.debug("Deploying instance in account 1 when Memory limit is reached")

        with self.assertRaises(Exception):
            self.createInstance(account=self.child_do_admin,
                              service_off=self.service_offering, api_client=api_client)
        return

    @attr(tags=["advanced", "advancedns","simulator"], required_hardware="false")
    def test_03_deploy_vm_project_limit_reached(self):
        """Test TTry to deploy VM with admin account where account has not used
        the resources but @ project they are not available"""

        # Validate the following
        # 1. Try to deploy VM with admin account where account has not used the
        #    resources but @ project they are not available
        # 2. Deploy VM should error out saying  ResourceAllocationException
        #    with "resource limit exceeds"

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts(account_limit=8,domain_limit=8, project_limit=4)

        api_client = self.testClient.getUserApiClient(
                            UserName=self.child_do_admin.name,
                            DomainName=self.child_do_admin.domain)

        self.debug("Deploying instance with project: %s" % self.project.name)
        with self.assertRaises(Exception):
            self.createInstance(project = self.project,
                            service_off=self.service_offering, api_client=api_client)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_04_deployVm__account_limit_reached(self):
        """Test Try to deploy VM with admin account where account has used
           the resources but @ project they are available"""

        # Validate the following
        # 1. Try to deploy VM with admin account where account has used the
        #    resources but @ project they are available
        # 2. Deploy VM should error out saying  ResourceAllocationException
        #    with "resource limit exceeds"

        self.debug("Setting up account and domain hierarchy")
        self.setupAccounts(account_limit=6, project_limit=12, domain_limit=12)

        api_client = self.testClient.getUserApiClient(
                            UserName=self.child_do_admin.name,
                            DomainName=self.child_do_admin.domain)

        self.debug("Deploying instance with account: %s" %
                                                self.child_do_admin.name)
        self.createInstance(account=self.child_do_admin,
                              service_off=self.service_offering, api_client=api_client)

        self.debug("Deploying instance in account: %s when memory limit is reached" %
                                                self.child_do_admin.name)

        with self.assertRaises(Exception):
            self.createInstance(project=self.project, account=self.child_do_admin,
                             service_off=self.service_offering, api_client=api_client)
        return
