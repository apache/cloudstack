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
""" P1 tests for Account
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (random_gen,
                              cleanup_resources,
                              validateList)
from marvin.cloudstackAPI import *
from marvin.lib.base import (Domain,
                             Account,
                             ServiceOffering,
                             VirtualMachine,
                             Network,
                             User,
                             NATRule,
                             Template,
                             PublicIPAddress, Role)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template,
                               list_accounts,
                               list_virtual_machines,
                               list_service_offering,
                               list_templates,
                               list_users,
                               get_builtin_template_info,
                               wait_for_cleanup)
from nose.plugins.attrib import attr
from marvin.cloudstackException import CloudstackAPIException
from marvin.codes import PASS
import time

from pyVmomi.VmomiSupport import GetVersionFromVersionUri

class Services:

    """Test Account Services
    """

    def __init__(self):
        self.services = {
            "domain": {
                "name": "Domain",
            },
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "fr3sca",
            },
            "role": {
                "name": "MarvinFake Role",
                "type": "User",
                "description": "Fake Role created by Marvin test"
            },
            "user": {
                "email": "user@test.com",
                "firstname": "User",
                "lastname": "User",
                "username": "User",
                # Random characters are appended for unique
                # username
                "password": "fr3sca",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                # in MHz
                "memory": 128,
                # In MBs
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "template": {
                "displaytext": "Public Template",
                "name": "Public template",
                "ostype": 'CentOS 5.6 (64-bit)',
                "url": "",
                "hypervisor": '',
                "format": '',
                "isfeatured": True,
                "ispublic": True,
                "isextractable": True,
                "templatefilter": "self"
            },
            "natrule": {
                "publicport": 22,
                "privateport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.6 (64-bit)',
            "sleep": 60,
            "timeout": 10,
        }


class TestAccounts(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAccounts, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup = [cls.service_offering]
        return

    @classmethod
    def tearDownClass(cls):
        super(TestAccounts,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestAccounts,self).tearDown()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "advancedns",
            "sg"],
        required_hardware="false")
    def test_01_create_account(self):
        """Test Create Account and user for that account
        """

        # Validate the following
        # 1. Create an Account. Verify the account is created.
        # 2. Create User associated with that account. Verify the created user

        # Create an account
        account = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % account.name)
        self.cleanup.append(account)
        list_accounts_response = list_accounts(
            self.apiclient,
            id=account.id
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "Check list accounts for valid data"
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "Check List Account response"
        )

        account_response = list_accounts_response[0]
        self.assertEqual(
            account.accounttype,
            account_response.accounttype,
            "Check Account Type of Created account"
        )
        self.assertEqual(
            account.name,
            account_response.name,
            "Check Account Name of Created account"
        )
        # Create an User associated with account
        user = User.create(
            self.apiclient,
            self.services["user"],
            account=account.name,
            domainid=account.domainid
        )
        self.cleanup.append(user)
        self.debug("Created user: %s" % user.id)
        list_users_response = list_users(
            self.apiclient,
            id=user.id
        )
        self.assertEqual(
            isinstance(list_users_response, list),
            True,
            "Check list users for valid data"
        )

        self.assertNotEqual(
            len(list_users_response),
            0,
            "Check List User response"
        )

        user_response = list_users_response[0]
        self.assertEqual(
            user.username,
            user_response.username,
            "Check username of Created user"
        )
        self.assertEqual(
            user.state,
            user_response.state,
            "Check state of created user"
        )
        self.assertEqual(
            "native",
            user_response.usersource,
            "Check user source of created user"
        )
        return


    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"],
          required_hardware="false")
    def test_02_update_account(self):
        """
        Tests that accounts can be updated with new name, network domain, dynamic role
        :return:
        """
        dynamic_roles_active = self.apiclient.listCapabilities(listCapabilities.listCapabilitiesCmd()).dynamicrolesenabled
        if not dynamic_roles_active:
            self.skipTest("Dynamic Role-Based API checker not enabled, skipping test")

        ts = str(time.time())
        network_domain = 'mycloud.com'

        # role will be in cleanup list first so it won't be deleted before the account it is on.
        role = Role.create(self.apiclient, self.services['role'])
        self.cleanup.append(role)

        account = Account.create(self.apiclient, self.services['account'])
        self.cleanup.append(account)

        account.update(self.apiclient, newname=account.name + ts)
        account.update(self.apiclient, roleid=role.id)
        account.update(self.apiclient, networkdomain=network_domain)

        list_accounts_response = list_accounts(self.apiclient, id=account.id)
        test_account = list_accounts_response[0]

        self.assertEqual(
            test_account.roleid, role.id,
            "Check the role for the account is changed")

        self.assertEqual(
            test_account.networkdomain, network_domain,
            "Check the domain for the account is changed")

        self.assertEqual(
            test_account.name, account.name + ts,
            "Check the name for the account is changed")

        try:
            account.update(self.apiclient, newname="")
            self.fail("Account name change to empty name succeeded. Must be error.")
        except CloudstackAPIException:
            pass



class TestRemoveUserFromAccount(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestRemoveUserFromAccount,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        # Create an account
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"]
        )
        cls._cleanup.append(cls.account)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestRemoveUserFromAccount,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestRemoveUserFromAccount,self).tearDown()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "advancedns",
            "sg"],
        required_hardware="false")
    def test_01_user_remove_VM_running(self):
        """Test Remove one user from the account
        """

        # Validate the following
        # 1. Create an account with 2 users.
        # 2. Start 2 VMs; one for each user of the account
        # 3. Remove one user from the account. Verify that account
        #    still exists.
        # 4. Verify that VM started by the removed user are still running

        # Create an User associated with account and VMs
        user_1 = User.create(
            self.apiclient,
            self.services["user"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created user: %s" % user_1.id)
        self.cleanup.append(user_1)

        user_2 = User.create(
            self.apiclient,
            self.services["user"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created user: %s" % user_2.id)
        self.cleanup.append(user_2)

        vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.debug("Deployed VM in account: %s, ID: %s" % (
            self.account.name,
            vm_1.id
        ))
        self.cleanup.append(vm_1)

        vm_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.debug("Deployed VM in account: %s, ID: %s" % (
            self.account.name,
            vm_2.id
        ))
        self.cleanup.append(vm_2)

        # Remove one of the user
        self.debug("Deleting user: %s" % user_1.id)
        user_1.delete(self.apiclient)
        self.cleanup.remove(user_1)

        # Account should exist after deleting user
        accounts_response = list_accounts(
            self.apiclient,
            id=self.account.id
        )
        self.assertEqual(
            isinstance(accounts_response, list),
            True,
            "Check for valid list accounts response"
        )

        self.assertNotEqual(
            len(accounts_response),
            0,
            "Check List Account response"
        )
        vm_response = list_virtual_machines(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check for valid list VM response"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check List VM response"
        )

        # VMs associated with that account should be running
        for vm in vm_response:
            self.assertEqual(
                vm.state,
                'Running',
                "Check state of VMs associated with account"
            )
        return


class TestNonRootAdminsPrivileges(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestNonRootAdminsPrivileges,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        # Create an account, domain etc
        cls.domain = Domain.create(
            cls.api_client,
            cls.services["domain"],
        )
        cls._cleanup.append(cls.domain)
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        return

    @classmethod
    def tearDownClass(cls):
        super(TestNonRootAdminsPrivileges,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestNonRootAdminsPrivileges,self).tearDown()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "advancedns",
            "sg"],
        required_hardware="false")
    def test_01_non_root_admin_Privileges(self):
        """Test to verify Non Root admin previleges"""

        # Validate the following
        # 1. Create few accounts/users in ROOT domain
        # 2. Verify listAccounts API gives only accounts associated with new
        #    domain.

        # Create accounts for ROOT domain
        account_1 = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % account_1.name)
        self.cleanup.append(account_1)
        account_2 = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % account_2.name)
        self.cleanup.append(account_2)

        accounts_response = list_accounts(
            self.apiclient,
            domainid=self.domain.id,
            listall=True
        )

        self.assertEqual(
            isinstance(accounts_response, list),
            True,
            "Check list accounts response for valid data"
        )

        self.assertEqual(
            len(accounts_response),
            1,
            "Check List accounts response"
        )
        # Verify only account associated with domain is listed
        for account in accounts_response:
            self.assertEqual(
                account.domainid,
                self.domain.id,
                "Check domain ID of account"
            )
        return


class TestServiceOfferingSiblings(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
            TestServiceOfferingSiblings,
            cls
        ).getClsTestClient().getApiClient()
        cls.services = Services().services

        cls._cleanup = []

        # Create Domains, accounts etc
        cls.domain_1 = Domain.create(
            cls.api_client,
            cls.services["domain"]
        )
        cls._cleanup.append(cls.domain_1)
        cls.domain_2 = Domain.create(
            cls.api_client,
            cls.services["domain"]
        )
        cls._cleanup.append(cls.domain_2)
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"],
            domainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.service_offering)
        # Create account for doamin_1
        cls.account_1 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.account_1)

        # Create an account for domain_2
        cls.account_2 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain_2.id
        )
        cls._cleanup.append(cls.account_2)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestServiceOfferingSiblings,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestServiceOfferingSiblings,self).tearDown()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "advancedns",
            "sg"],
        required_hardware="false")
    def test_01_service_offering_siblings(self):
        """Test to verify service offerings at same level in hierarchy"""

        # Validate the following
        # 1. Verify service offering is visible for domain_1
        # 2. Verify service offering is not visible for domain_2

        service_offerings = list_service_offering(
            self.apiclient,
            domainid=self.domain_1.id
        )
        self.assertEqual(
            isinstance(service_offerings, list),
            True,
            "Check if valid list service offerings response"
        )

        self.assertNotEqual(
            len(service_offerings),
            0,
            "Check List Service Offerings response"
        )

        for service_offering in service_offerings:
            self.debug("Validating service offering: %s" % service_offering.id)
            self.assertEqual(
                service_offering.id,
                self.service_offering.id,
                "Check Service offering ID for domain" +
                str(self.domain_1.name)
            )
        # Verify private service offering is not visible to other domain
        service_offerings = list_service_offering(
            self.apiclient,
            domainid=self.domain_2.id
        )
        self.assertEqual(
            service_offerings,
            None,
            "Check List Service Offerings response for other domain"
        )
        return


class TestServiceOfferingHierarchy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
            TestServiceOfferingHierarchy,
            cls
        ).getClsTestClient().getApiClient()
        cls.services = Services().services

        cls._cleanup = []

        # Create domain, service offerings etc
        cls.domain_1 = Domain.create(
            cls.api_client,
            cls.services["domain"]
        )
        cls._cleanup.append(cls.domain_1)
        cls.domain_2 = Domain.create(
            cls.api_client,
            cls.services["domain"],
            parentdomainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.domain_2)
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"],
            domainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.service_offering)
        # Create account for doamin_1
        cls.account_1 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.account_1)
        # Create an account for domain_2
        cls.account_2 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain_2.id
        )
        cls._cleanup.append(cls.account_2)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestServiceOfferingHierarchy,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestServiceOfferingHierarchy,self).tearDown()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "advancedns",
            "sg"],
        required_hardware="false")
    def test_01_service_offering_hierarchy(self):
        """Test to verify service offerings at same level in hierarchy"""

        # Validate the following
        # 1. Verify service offering is visible for domain_1
        # 2. Verify service offering is also visible for domain_2

        service_offerings = list_service_offering(
            self.apiclient,
            domainid=self.domain_1.id
        )
        self.assertEqual(
            isinstance(service_offerings, list),
            True,
            "Check List Service Offerings for a valid response"
        )
        self.assertNotEqual(
            len(service_offerings),
            0,
            "Check List Service Offerings response"
        )

        for service_offering in service_offerings:
            self.assertEqual(
                service_offering.id,
                self.service_offering.id,
                "Check Service offering ID for domain" +
                str(self.domain_1.name)
            )

        # Verify private service offering is not visible to other domain
        service_offerings = list_service_offering(
            self.apiclient,
            domainid=self.domain_2.id
        )
        self.assertEqual(
            service_offerings,
            None,
            "Check List Service Offerings for a valid response"
        )
        return


class TestTemplateHierarchy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestTemplateHierarchy, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []

        # Create domains, accounts and template
        cls.domain_1 = Domain.create(
            cls.api_client,
            cls.services["domain"]
        )
        cls._cleanup.append(cls.domain_1)
        cls.domain_2 = Domain.create(
            cls.api_client,
            cls.services["domain"],
            parentdomainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.domain_2)

        # Create account for doamin_1
        cls.account_1 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.account_1)

        # Create an account for domain_2
        cls.account_2 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain_2.id
        )
        cls._cleanup.append(cls.account_2)

        builtin_info = get_builtin_template_info(cls.api_client, cls.zone.id)
        cls.services["template"]["url"] = builtin_info[0]
        cls.services["template"]["hypervisor"] = builtin_info[1]
        cls.services["template"]["format"] = builtin_info[2]

        # Register new template
        cls.template = Template.register(
            cls.api_client,
            cls.services["template"],
            zoneid=cls.zone.id,
            account=cls.account_1.name,
            domainid=cls.domain_1.id,
            hypervisor=cls.hypervisor
        )
        cls._cleanup.append(cls.template)

        # Wait for template to download
        cls.template.download(cls.api_client)

        # Wait for template status to be changed across
        time.sleep(60)
        return

    @classmethod
    def tearDownClass(cls):
        super(TestTemplateHierarchy,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestTemplateHierarchy,self).tearDown()

    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"], required_hardware="true")
    def test_01_template_hierarchy(self):
        """Test to verify template at same level in hierarchy"""

        # Validate the following
        # 1. Verify template is visible for domain_1
        # 2. Verify template is also visible for domain_2

        # Sleep to ensure that template state is reflected across

        templates = list_templates(
            self.apiclient,
            templatefilter='self',
            account=self.account_1.name,
            domainid=self.domain_1.id
        )
        self.assertEqual(
            isinstance(templates, list),
            True,
            "Template response %s is not a list" % templates
        )
        self.assertNotEqual(
            len(templates),
            0,
            "No templates found"
        )

        for template in templates:
            self.assertEqual(
                template.id,
                self.template.id,
                "Check Template ID for domain" + str(self.domain_1.name)
            )

        # Verify private service offering is not visible to other domain
        templates = list_templates(
            self.apiclient,
            id=self.template.id,
            templatefilter='all',
            account=self.account_2.name,
            domainid=self.domain_2.id
        )
        self.assertEqual(
            isinstance(templates, list),
            True,
            "Template response %s is not a list" % templates
        )
        self.assertNotEqual(
            len(templates),
            0,
            "No templates found"
        )

        for template in templates:
            self.assertEqual(
                template.id,
                self.template.id,
                "Check Template ID for domain" + str(self.domain_2.name)
            )
        return


class TestAddVmToSubDomain(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAddVmToSubDomain, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []

        cls.sub_domain = Domain.create(
            cls.api_client,
            cls.services["domain"],
            parentdomainid=cls.domain.id
        )
        cls._cleanup.append(cls.sub_domain)

        # Create account for doamin_1
        cls.account_1 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account_1)

        # Create an account for domain_2
        cls.account_2 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.sub_domain.id
        )
        cls._cleanup.append(cls.account_2)

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.service_offering)

        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.vm_1 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account_1.name,
            domainid=cls.account_1.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.vm_1)

        cls.vm_2 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account_2.name,
            domainid=cls.account_2.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.vm_2)
        return

    @classmethod
    def tearDownClass(cls):
        super(TestAddVmToSubDomain,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestAddVmToSubDomain,self).tearDown()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "advancedns",
            "sg"],
        required_hardware="false")
    def test_01_add_vm_to_subdomain(self):
        """ Test Sub domain allowed to launch VM  when a Domain
            level zone is created"""

        # Validate the following
        # 1. Verify VM created by Account_1 is in Running state
        # 2. Verify VM created by Account_2 is in Running state

        vm_response = list_virtual_machines(
            self.apiclient,
            id=self.vm_1.id
        )
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check List VM for a valid response"
        )
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check List Template response"
        )

        for vm in vm_response:
            self.debug("VM ID: %s and state: %s" % (vm.id, vm.state))
            self.assertEqual(
                vm.state,
                'Running',
                "Check State of Virtual machine"
            )

        vm_response = list_virtual_machines(
            self.apiclient,
            id=self.vm_2.id
        )
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check List Template response"
        )

        for vm in vm_response:
            self.debug("VM ID: %s and state: %s" % (vm.id, vm.state))
            self.assertEqual(
                vm.state,
                'Running',
                "Check State of Virtual machine"
            )
        return


class TestUserDetails(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUserDetails, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        super(TestUserDetails,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestUserDetails,self).tearDown()

    @attr(tags=[
        "role",
        "accounts",
        "simulator",
        "advanced",
        "advancedns",
        "basic",
        "eip",
        "sg"
    ])
    def test_updateUserDetails(self):
        """Test user update API
        """

        # Steps for test scenario
        # 1. create a user account
        # 2. update the user details (firstname, lastname, user) with
        #    updateUser API
        # 3. listUsers in the account
        # 4. delete the account
        # Validate the following
        # 1. listAccounts should show account created successfully
        # 2. updateUser API should return valid response
        # 3. user should be updated with new details

        self.debug("Creating an user account..")
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

        # Fetching the user details of account
        self.debug(
            "Fetching user details for account: %s" %
            self.account.name)
        users = User.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(users, list),
            True,
            "List users should return a valid list for account"
        )
        user_1 = users[0]
        self.debug("Updating the details of user: %s" % user_1.name)
        firstname = random_gen()
        lastname = random_gen()

        self.debug("New firstname: %s, lastname: %s" % (firstname, lastname))
        User.update(
            self.apiclient,
            user_1.id,
            firstname=firstname,
            lastname=lastname
        )

        # Fetching the user details of account
        self.debug(
            "Fetching user details for user: %s" % user_1.name)
        users = User.list(
            self.apiclient,
            id=user_1.id,
            listall=True
        )

        self.assertEqual(
            isinstance(users, list),
            True,
            "List users should return a valid list for account"
        )
        user_1 = users[0]
        self.assertEqual(
            user_1.firstname,
            firstname,
            "User's first name should be updated with new one"
        )
        self.assertEqual(
            user_1.lastname,
            lastname,
            "User's last name should be updated with new one"
        )
        return

    @attr(tags=[
        "role",
        "accounts",
        "simulator",
        "advanced",
        "advancedns",
        "basic",
        "eip",
        "sg"
    ])
    def test_updateAdminDetails(self):
        """Test update admin details
        """

        # Steps for test scenario
        # 1. create a admin account
        # 2. update the user details (firstname, lastname, user) with
        #    updateUser API
        # 3. listUsers in the account
        # 4. delete the account
        # Validate the following
        # 1. listAccounts should show account created successfully
        # 2. updateUser API should return valid response
        # 3. user should be updated with new details

        self.debug("Creating a ROOT admin account")
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
        )
        self.cleanup.append(self.account)

        # Fetching the user details of account
        self.debug(
            "Fetching user details for account: %s" %
            self.account.name)
        users = User.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(users, list),
            True,
            "List users should return a valid list for account"
        )
        user_1 = users[0]
        self.debug("Updating the details of user: %s" % user_1.name)
        firstname = random_gen()
        lastname = random_gen()

        self.debug("New firstname: %s, lastname: %s" % (firstname, lastname))
        User.update(
            self.apiclient,
            user_1.id,
            firstname=firstname,
            lastname=lastname
        )

        # Fetching the user details of account
        self.debug(
            "Fetching user details for user: %s" % user_1.name)
        users = User.list(
            self.apiclient,
            id=user_1.id,
            listall=True
        )

        self.assertEqual(
            isinstance(users, list),
            True,
            "List users should return a valid list for account"
        )
        user_1 = users[0]
        self.assertEqual(
            user_1.firstname,
            firstname,
            "User's first name should be updated with new one"
        )
        self.assertEqual(
            user_1.lastname,
            lastname,
            "User's last name should be updated with new one"
        )
        return

    @attr(tags=[
        "role",
        "accounts",
        "simulator",
        "advanced",
        "advancedns",
        "basic",
        "eip",
        "sg"
    ])
    def test_updateDomainAdminDetails(self):
        """Test update domain admin details
        """

        # Steps for test scenario
        # 2. update the user details (firstname, lastname, user) with
        #    updateUser API
        # 3. listUsers in the account
        # 4. delete the account
        # Validate the following
        # 1. listAccounts should show account created successfully
        # 2. updateUser API should return valid response
        # 3. user should be updated with new details

        self.debug("Creating a domain admin account")
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

        # Fetching the user details of account
        self.debug(
            "Fetching user details for account: %s" %
            self.account.name)
        users = User.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(users, list),
            True,
            "List users should return a valid list for account"
        )
        user_1 = users[0]
        self.debug("Updating the details of user: %s" % user_1.name)
        firstname = random_gen()
        lastname = random_gen()

        self.debug("New firstname: %s, lastname: %s" % (firstname, lastname))
        User.update(
            self.apiclient,
            user_1.id,
            firstname=firstname,
            lastname=lastname
        )

        # Fetching the user details of account
        self.debug(
            "Fetching user details for user: %s" % user_1.name)
        users = User.list(
            self.apiclient,
            id=user_1.id,
            listall=True
        )

        self.assertEqual(
            isinstance(users, list),
            True,
            "List users should return a valid list for account"
        )
        user_1 = users[0]
        self.assertEqual(
            user_1.firstname,
            firstname,
            "User's first name should be updated with new one"
        )
        self.assertEqual(
            user_1.lastname,
            lastname,
            "User's last name should be updated with new one"
        )
        return


class TestUserLogin(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUserLogin, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        super(TestUserLogin,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestUserLogin,self).tearDown()

    @attr(tags=["login", "accounts", "simulator", "advanced",
                "advancedns", "basic", "eip", "sg"])
    def test_LoginApiUuidResponse(self):
        """Test if Login API does not return UUID's
        """

        # Steps for test scenario
        # 1. create a user account
        # 2. login to the user account with given credentials (loginCmd)
        # 3. delete the user account
        # Validate the following
        # 1. listAccounts should return account created
        # 2. loginResponse should have UUID only is response. Assert by
        #    checking database id is not same as response id
        #    Login also succeeds with non NULL sessionId in response

        self.debug("Creating an user account..")
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

        self.debug("Logging into the cloudstack with login API")
        respose = User.login(
            self.apiclient,
            username=self.account.name,
            password=self.services["account"]["password"]
        )

        self.debug("Login API response: %s" % respose)

        self.assertNotEqual(
            respose.sessionkey,
            None,
            "Login to the CloudStack should be successful" +
            "response shall have non Null key"
        )
        return

    @attr(tags=["simulator", "advanced",
                "advancedns", "basic", "eip", "sg"])
    def test_ApiListDomain(self):
        """Test case to check the correctness of List domain API, to make sure that no field is missed in the output.
        """

        # Steps for test scenario
        # 1. create a domain
        # 2. Create a sub-domain with domain created in step 1 as parent.
        # Validate the following
        # 1. listDomains returns created domain and sub-domain
        # 2. The list Domain response has all the expected 44 elements/fields in it.

        listDomainResponseElements = ["id", "name", "level", "parentdomainid", "parentdomainname", "haschild", "path",
                                      "state",
                                      "vmlimit", "vmtotal", "vmavailable", "iplimit", "iptotal", "ipavailable",
                                      "volumelimit",
                                      "volumetotal", "volumeavailable", "snapshotlimit", "snapshottotal",
                                      "snapshotavailable",
                                      "templatelimit", "templatetotal", "templateavailable", "projectlimit",
                                      "projecttotal", "projectavailable",
                                      "networklimit", "networktotal", "networkavailable", "vpclimit", "vpctotal",
                                      "vpcavailable",
                                      "cpulimit", "cputotal", "cpuavailable", "memorylimit", "memorytotal",
                                      "memoryavailable", "primarystoragelimit",
                                      "primarystoragetotal", "primarystorageavailable", "secondarystoragelimit",
                                      "secondarystoragetotal", "secondarystorageavailable"
                                      ]

        self.debug("Creating a domain for testing list domain reponse")
        domain = Domain.create(
            self.apiclient,
            self.services["domain"],
            parentdomainid=self.domain.id
        )
        self.cleanup.append(domain)
        self.debug("Domain: %s is created successfully." % domain.name)

        self.debug("Validating the created domain")
        list_domain = Domain.list(self.api_client, id=domain.id)
        domain_list_validation_result = validateList(list_domain)

        self.assertEqual(domain_list_validation_result[0], PASS,
                         "Domain list validation failed due to %s" %
                         domain_list_validation_result[2])

        subDomain = Domain.create(
            self.apiclient,
            self.services["domain"],
            parentdomainid=domain.id
        )
        self.cleanup.append(subDomain)
        self.debug("Sub-Domain: %s is created successfully." % subDomain.name)

        self.debug("Validating the created sub-domain")
        list_sub_domain = Domain.list(self.api_client, id=subDomain.id)
        subdomain_list_validation_result = validateList(list_sub_domain)

        self.assertEqual(subdomain_list_validation_result[0], PASS,
                         "Sub-Domain list validation failed due to %s" %
                         subdomain_list_validation_result[2])

        self.debug("Checking that the listDomain response has all the elements.")

        domainOutputString = list_domain[0].__dict__

        for element in listDomainResponseElements:
            self.assertTrue((element.lower() in domainOutputString), element + " field is missing in list domain rsponse.")

        self.debug("Verified that the listDomain response has all the elements.")

        self.debug("Checking that the list sub-domain response has all the elements.")

        subdomainOutputString = list_sub_domain[0].__dict__

        for element in listDomainResponseElements:
            self.assertTrue((element.lower() in subdomainOutputString), element + " field is missing in list domain rsponse.")

        self.debug("Verified that the list sub-Domain response has all the elements.")

        return

    @attr(tags=["login", "accounts", "simulator", "advanced",
                "advancedns", "basic", "eip", "sg"])
    def test_LoginApiDomain(self):
        """Test login API with domain
        """

        # Steps for test scenario
        # 1. create a domain
        # 2. create user in the domain
        # 3. login to the user account above using UUID domain/user
        # 4. delete the user account
        # Validate the following
        # 1. listDomains returns created domain
        # 2. listAccounts returns created user
        # 3. loginResponse should have UUID only in responses
        #    Login also succeeds with non NULL sessionId in response

        self.debug("Creating a domain for login with API domain test")
        domain = Domain.create(
            self.apiclient,
            self.services["domain"],
            parentdomainid=self.domain.id
        )
        self.cleanup.append(domain)
        self.debug("Domain: %s is created succesfully." % domain.name)
        self.debug(
            "Checking if the created domain is listed in list domains API")
        domains = Domain.list(self.apiclient, id=domain.id, listall=True)

        self.assertEqual(
            isinstance(domains, list),
            True,
            "List domains shall return a valid response"
        )
        self.debug("Creating an user account in domain: %s" % domain.name)
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=domain.id
        )
        self.cleanup.append(self.account)

        accounts = Account.list(
            self.apiclient,
            name=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )

        self.assertEqual(
            isinstance(accounts, list),
            True,
            "List accounts should return a valid response"
        )

        self.debug("Logging into the cloudstack with login API")
        respose = User.login(
            self.apiclient,
            username=self.account.name,
            password=self.services["account"]["password"],
            domainid=domain.id)
        self.debug("Login API response: %s" % respose)

        self.assertNotEqual(
            respose.sessionkey,
            None,
            "Login to the CloudStack should be successful" +
            "response shall have non Null key"
        )
        return


class TestUserAPIKeys(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUserAPIKeys, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        # Create an account, domain etc
        cls.domain = Domain.create(
            cls.api_client,
            cls.services["domain"],
        )
        cls._cleanup.append(cls.domain)
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=False,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.domain_2 = Domain.create(
            cls.api_client,
            cls.services["domain"],
        )
        cls._cleanup.append(cls.domain_2)
        cls.account_2 = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=False,
            domainid=cls.domain_2.id
        )
        cls._cleanup.append(cls.account_2)
        return

    @classmethod
    def tearDownClass(cls):
        super(TestUserAPIKeys,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestUserAPIKeys,self).tearDown()

    @attr(tags=[
        "role",
        "accounts",
        "simulator",
        "advanced",
        "advancedns",
        "basic",
        "eip",
        "sg"
    ])
    def test_user_key_renew_same_account(self):
        # Create an User associated with the account
        user_1 = User.create(
            self.apiclient,
            self.services["user"],
            account=self.account.name,
            domainid=self.domain.id
        )
        self.cleanup.append(user_1)
        account_response = list_accounts(
            self.apiclient,
            id=self.account.id
        )[0]
        self.assertEqual(
            hasattr(account_response, 'user'),
            True,
            "Users are included in account response")

        account_users = account_response.user
        self.assertEqual(
            isinstance(account_users, list),
            True,
            "Check account for valid data"
        )
        self.assertNotEqual(
            len(account_users),
            0,
            "Check number of User in Account")
        [user] = [u for u in account_users if u.username == user_1.username]
        self.assertEqual(
            user.apikey,
            None,
            "Check that the user don't have an API key yet")

        self.debug("Register API keys for user")
        userkeys = User.registerUserKeys(self.apiclient, user_1.id)
        users = list_accounts(
            self.apiclient,
            id=self.account.id
        )[0].user
        [user] = [u for u in users if u.id == user_1.id]
        self.assertEqual(
            user.apikey,
            userkeys.apikey,
            "Check User api key")
        user.secretkey = self.get_secret_key(user.id)
        self.assertEqual(
            user.secretkey,
            userkeys.secretkey,
            "Check User having secret key")

        self.debug("Get test client with user keys")
        cs_api = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.debug("Renew API keys for user using current keys")
        new_keys = User.registerUserKeys(cs_api, user_1.id)
        self.assertNotEqual(
            userkeys.apikey,
            new_keys.apikey,
            "Check API key is different")
        new_keys.secretkey = self.get_secret_key(user_1.id)
        self.assertNotEqual(
            userkeys.secretkey,
            new_keys.secretkey,
            "Check secret key is different")

    def get_secret_key(self, id):
        cmd = getUserKeys.getUserKeysCmd()
        cmd.id = id
        keypair = self.apiclient.getUserKeys(cmd)
        return keypair.secretkey

    @attr(tags=[
        "role",
        "accounts",
        "simulator",
        "advanced",
        "advancedns",
        "basic",
        "eip",
        "sg"
    ])
    def test_user_cannot_renew_other_keys(self):
        cs_api = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.debug("Try to change API key of an account in another domain")
        users = list_accounts(
            self.apiclient,
            id=self.account_2.id
        )[0].user
        with self.assertRaises(CloudstackAPIException) as e:
            User.registerUserKeys(cs_api, users[0].id)


class TestDomainForceRemove(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDomainForceRemove, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        super(TestDomainForceRemove,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestDomainForceRemove,self).tearDown()

    @attr(
        tags=[
            "domains",
            "advanced",
            "advancedns",
            "simulator",
            "dvs"],
        required_hardware="false")
    def test_forceDeleteDomain(self):
        """ Test delete domain with force option"""

        # Steps for validations
        # 1. create a domain DOM
        # 2. create 2 users under this domain
        # 3. deploy 1 VM into each of these user accounts
        # 4. create PF / FW rules for port 22 on these VMs for their
        #    respective accounts
        # 5. delete the domain with force=true option
        # Validate the following
        # 1. listDomains should list the created domain
        # 2. listAccounts should list the created accounts
        # 3. listvirtualmachines should show the Running VMs
        # 4. PF and FW rules should be shown in listFirewallRules
        # 5. domain should delete successfully and above three list calls
        #    should show all the resources now deleted. listRouters should
        #    not return any routers in the deleted accounts/domains

        self.debug("Creating a domain for login with API domain test")
        self.child_domain = Domain.create(
            self.apiclient,
            self.services["domain"],
            parentdomainid=self.domain.id
        )
        self.cleanup.append(self.child_domain)
        self.debug("Domain is created succesfully.")
        self.debug(
            "Checking if the created domain is listed in list domains API")
        domains = Domain.list(self.apiclient, id=self.child_domain.id, listall=True)

        self.assertEqual(
            isinstance(domains, list),
            True,
            "List domains shall return a valid response"
        )
        self.debug("Creating 2 user accounts in domain: %s" % self.child_domain.name)
        self.account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.child_domain.id
        )
        self.cleanup.append(self.account_1)

        self.account_2 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.child_domain.id
        )
        self.cleanup.append(self.account_2)

        vm_1 = None
        vm_2 = None
        try:
            self.debug("Creating a tiny service offering for VM deployment")
            self.service_offering = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"],
                domainid=self.domain.id
            )
            self.cleanup.append(self.service_offering)

            self.debug("Deploying virtual machine in account 1: %s" %
                       self.account_1.name)
            self.vm_1 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                templateid=self.template.id,
                accountid=self.account_1.name,
                domainid=self.account_1.domainid,
                serviceofferingid=self.service_offering.id
            )
            self.cleanup.append(self.vm_1)

            self.debug("Deploying virtual machine in account 2: %s" %
                       self.account_2.name)
            self.vm_2 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                templateid=self.template.id,
                accountid=self.account_2.name,
                domainid=self.account_2.domainid,
                serviceofferingid=self.service_offering.id
            )
            self.cleanup.append(self.vm_2)

            networks = Network.list(
                self.apiclient,
                account=self.account_1.name,
                domainid=self.account_1.domainid,
                listall=True
            )
            self.assertEqual(
                isinstance(networks, list),
                True,
                "List networks should return a valid response"
            )
            network_1 = networks[0]
            self.debug("Default network in account 1: %s is %s" % (
                self.account_1.name,
                network_1.name))
            src_nat_list = PublicIPAddress.list(
                self.apiclient,
                associatednetworkid=network_1.id,
                account=self.account_1.name,
                domainid=self.account_1.domainid,
                listall=True,
                issourcenat=True,
            )
            self.assertEqual(
                isinstance(src_nat_list, list),
                True,
                "List Public IP should return a valid source NAT"
            )
            self.assertNotEqual(
                len(src_nat_list),
                0,
                "Length of response from listPublicIp should not be 0"
            )

            src_nat = src_nat_list[0]

            self.debug(
                "Trying to create a port forwarding rule in source NAT: %s" %
                src_nat.ipaddress)
            # Create NAT rule
            self.nat_rule = NATRule.create(
                self.apiclient,
                self.vm_1,
                self.services["natrule"],
                ipaddressid=src_nat.id
            )
            self.cleanup.append(self.nat_rule)
            self.debug("Created PF rule on source NAT: %s" % src_nat.ipaddress)

            nat_rules = NATRule.list(self.apiclient, id=self.nat_rule.id)

            self.assertEqual(
                isinstance(nat_rules, list),
                True,
                "List NAT should return a valid port forwarding rules"
            )

            self.assertNotEqual(
                len(nat_rules),
                0,
                "Length of response from listLbRules should not be 0"
            )
        except Exception as e:
            self.fail(e)

        self.debug("Deleting domain with force option")
        try:
            self.child_domain.delete(self.apiclient, cleanup=True)
            self.cleanup.remove(self.child_domain)
            self.cleanup.remove(self.account_1)
            self.cleanup.remove(self.account_2)
            self.cleanup.remove(self.vm_1)
            self.cleanup.remove(self.vm_2)
            self.cleanup.remove(self.nat_rule)

        except Exception as e:
            self.debug("Waiting for account.cleanup.interval" +
                       " to cleanup any remaining resouces")
            # Sleep 3*account.gc to ensure that all resources are deleted
            wait_for_cleanup(self.apiclient, ["account.cleanup.interval"] * 3)
            with self.assertRaises(CloudstackAPIException):
                Domain.list(
                    self.apiclient,
                    id=self.child_domain.id,
                    listall=True
                )

        self.debug("Checking if the resources in domain are deleted")
        with self.assertRaises(CloudstackAPIException):
            Account.list(
                self.apiclient,
                name=self.account_1.name,
                domainid=self.account_1.domainid,
                listall=True
            )
        return

    @attr(
        tags=[
            "domains",
            "advanced",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_DeleteDomain(self):
        """ Test delete domain without force option"""

        # Steps for validations
        # 1. create a domain DOM
        # 2. create 2 users under this domain
        # 3. deploy 1 VM into each of these user accounts
        # 4. create PF / FW rules for port 22 on these VMs for their
        #    respective accounts
        # 5. delete the domain with force=false option
        # Validate the following
        # 1. listDomains should list the created domain
        # 2. listAccounts should list the created accounts
        # 3. listvirtualmachines should show the Running VMs
        # 4. PF and FW rules should be shown in listFirewallRules
        # 5. domain deletion should fail saying there are resources under use

        self.debug("Creating a domain for login with API domain test")
        self.child_domain = Domain.create(
            self.apiclient,
            self.services["domain"],
            parentdomainid=self.domain.id
        )
        # in this test delete domain *should* fail so we need to housekeep:
        self.cleanup.append(self.child_domain)
        self.debug("Domain: %s is created successfully." % self.child_domain.name)
        self.debug(
            "Checking if the created domain is listed in list domains API")
        domains = Domain.list(self.apiclient, id=self.child_domain.id, listall=True)

        self.assertEqual(
            isinstance(domains, list),
            True,
            "List domains shall return a valid response"
        )
        self.debug("Creating 2 user accounts in domain: %s" % self.child_domain.name)
        self.account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.child_domain.id
        )
        self.cleanup.append(self.account_1)

        self.account_2 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.child_domain.id
        )
        self.cleanup.append(self.account_2)

        self.debug("Creating a tiny service offering for VM deployment")
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.service_offering)

        self.debug("Deploying virtual machine in account 1: %s" %
                   self.account_1.name)
        self.vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account_1.name,
            domainid=self.account_1.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(self.vm_1)

        self.debug("Deploying virtual machine in account 2: %s" %
                   self.account_2.name)
        self.vm_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account_2.name,
            domainid=self.account_2.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(self.vm_2)

        networks = Network.list(
            self.apiclient,
            account=self.account_1.name,
            domainid=self.account_1.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response"
        )
        network_1 = networks[0]
        self.debug("Default network in account 1: %s is %s" % (
            self.account_1.name,
            network_1.name))
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            associatednetworkid=network_1.id,
            account=self.account_1.name,
            domainid=self.account_1.domainid,
            listall=True,
            issourcenat=True,
        )
        self.assertEqual(
            isinstance(src_nat_list, list),
            True,
            "List Public IP should return a valid source NAT"
        )
        self.assertNotEqual(
            len(src_nat_list),
            0,
            "Length of response from listPublicIp should not be 0"
        )

        src_nat = src_nat_list[0]

        self.debug(
            "Trying to create a port forwarding rule in source NAT: %s" %
            src_nat.ipaddress)
        # Create NAT rule
        self.nat_rule = NATRule.create(
            self.apiclient,
            self.vm_1,
            self.services["natrule"],
            ipaddressid=src_nat.id
        )
        self.cleanup.append(self.nat_rule)
        self.debug("Created PF rule on source NAT: %s" % src_nat.ipaddress)

        nat_rules = NATRule.list(self.apiclient, id=self.nat_rule.id)

        self.assertEqual(
            isinstance(nat_rules, list),
            True,
            "List NAT should return a valid port forwarding rules"
        )

        self.assertNotEqual(
            len(nat_rules),
            0,
            "Length of response from listLbRules should not be 0"
        )

        self.debug("Deleting domain without force option")
        try:
            self.child_domain.delete(self.apiclient, cleanup=False)
            self.cleanup.remove(self.nat_rule)
            self.cleanup.remove(self.vm_2)
            self.cleanup.remove(self.vm_1)
            self.cleanup.remove(self.account_2)
            self.cleanup.remove(self.account_1)
            self.cleanup.remove(self.child_domain)
            self.fail("shouldn't be able to delete domain")
        except:
            pass
        return

class TestMoveUser(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMoveUser, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype

        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        super(TestMoveUser,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.account1 = Account.create(
            self.apiclient,
            self.testdata["acl"]["accountD1"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account1)

        self.account2 = Account.create(
            self.apiclient,
            self.testdata["acl"]["accountD1A"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account2)

        self.user = User.create(
            self.apiclient,
            self.testdata["user"],
            account=self.account1.name,
            domainid=self.account1.domainid
        )
        self.cleanup.append(self.user)

        return

    def tearDown(self):
        super(TestMoveUser,self).tearDownClass()

    @attr(tags=["domains", "advanced", "advancedns", "simulator","dvs"], required_hardware="false")
    def test_move_user_to_accountID(self):

        self.user.move(self.api_client, dest_accountid=self.account2.id)

        self.assertEqual(
            self.account2.name,
            self.user.list(self.apiclient, id=self.user.id)[0].account,
            "Check user source of created user"
        )
        return

    @attr(tags=["domains", "advanced", "advancedns", "simulator","dvs"], required_hardware="false")
    def test_move_user_to_account_name(self):

        self.user.move(self.api_client, dest_account=self.account2.name)

        self.assertEqual(
            self.account2.name,
            self.user.list(self.apiclient, id=self.user.id)[0].account,
            "Check user source of created user"
        )
        return

    @attr(tags=["domains", "advanced", "advancedns", "simulator","dvs"], required_hardware="false")
    def test_move_user_to_different_domain(self):
        domain2 = Domain.create(self.api_client,
                                self.testdata["domain"],
                                parentdomainid=self.domain.id
                                )
        self.cleanup.append(domain2)

        account_different_domain = Account.create(
            self.apiclient,
            self.testdata["acl"]["accountD1B"],
            domainid=domain2.id
        )
        self.cleanup.append(account_different_domain)
        try:
            self.user.move(self.api_client, dest_account=account_different_domain.name)
        except Exception:
            pass
        else:
            self.fail("It should not be allowed to move users across accounts in different domains, failing")

        return

    @attr(tags=["domains", "advanced", "advancedns", "simulator","dvs"], required_hardware="false")
    def test_move_user_incorrect_account_id(self):

        try:
            self.user.move(self.api_client, dest_accountid='incorrect-account-id')
        except Exception:
            pass
        else:
            self.fail("moving to non-existing account should not be possible, failing")
        return

    @attr(tags=["domains", "advanced", "advancedns", "simulator","dvs"], required_hardware="false")
    def test_move_user_incorrect_account_name(self):

        try:
            self.user.move(self.api_client, dest_account='incorrect-account-name')
        except Exception:
            pass
        else:
            self.fail("moving to non-existing account should not be possible, failing")
        return
