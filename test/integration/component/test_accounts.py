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
#Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin import remoteSSHClient
from nose.plugins.attrib import attr
import datetime


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
                                    "memory": 64,
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
                                "ostypeid": 'bc66ada0-99e7-483b-befc-8fb0c2129b70',
                                "url": "http://download.cloud.com/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                                "hypervisor": 'XenServer',
                                "format": 'VHD',
                                "isfeatured": True,
                                "ispublic": True,
                                "isextractable": True,
                        },
                        "natrule": {
                                    "publicport": 22,
                                    "privateport": 22,
                                    "protocol": 'TCP',
                        },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                        "mode": 'advanced'
                    }


class TestAccounts(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestAccounts,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
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
        try:
            #Cleanup resources used
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
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"])
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
        self.debug("Created account: %s" % account.account.name)
        self.cleanup.append(account)
        list_accounts_response = list_accounts(
                                               self.apiclient,
                                               id=account.account.id
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
                            account.account.accounttype,
                            account_response.accounttype,
                            "Check Account Type of Created account"
                            )
        self.assertEqual(
                            account.account.name,
                            account_response.name,
                            "Check Account Name of Created account"
                            )
        # Create an User associated with account
        user = User.create(
                            self.apiclient,
                            self.services["user"],
                            account=account.account.name,
                            domainid=account.account.domainid
                            )
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
        return


class TestRemoveUserFromAccount(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestRemoveUserFromAccount,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        # Create an account
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"]
                            )

        cls._cleanup = [
                        cls.service_offering,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
            #Clean up, terminate the created instance, users etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"])
    def test_01_user_remove_VM_running(self):
        """Test Remove one user from the account
        """

        # Validate the following
        # 1. Create an account with 2 users.
        # 2. Start 2 VMs; one for each user of the account
        # 3. Remove one user from the account. Verify that account still exists.
        # 4. Verify that VM started by the removed user are still running

        # Create an User associated with account and VMs
        user_1 = User.create(
                            self.apiclient,
                            self.services["user"],
                            account=self.account.account.name,
                            domainid=self.account.account.domainid
                            )
        self.debug("Created user: %s" % user_1.id)
        user_2 = User.create(
                            self.apiclient,
                            self.services["user"],
                            account=self.account.account.name,
                            domainid=self.account.account.domainid
                            )
        self.debug("Created user: %s" % user_2.id)
        self.cleanup.append(user_2)

        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id
                                  )
        self.debug("Deployed VM in account: %s, ID: %s" % (
                                                           self.account.account.name,
                                                           vm_1.id
                                                           ))
        self.cleanup.append(vm_1)

        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id
                                  )
        self.debug("Deployed VM in account: %s, ID: %s" % (
                                                           self.account.account.name,
                                                           vm_2.id
                                                           ))
        self.cleanup.append(vm_2)

        # Remove one of the user
        self.debug("Deleting user: %s" % user_1.id)
        user_1.delete(self.apiclient)

        # Account should exist after deleting user
        accounts_response = list_accounts(
                                          self.apiclient,
                                          id=self.account.account.id
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
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
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

    @unittest.skip("Open Questions")
    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"])
    def test_02_remove_all_users(self):
        """Test Remove both users from the account
        """

        # Validate the following
        # 1. Remove both the users from the account.
        # 2. Verify account is removed
        # 3. Verify all VMs associated with that account got removed

        # Create an User associated with account and VMs
        user_1 = User.create(
                            self.apiclient,
                            self.services["user"],
                            account=self.account.account.name,
                            domainid=self.account.account.domainid
                            )
        self.debug("Created user: %s" % user_1.id)
        user_2 = User.create(
                            self.apiclient,
                            self.services["user"],
                            account=self.account.account.name,
                            domainid=self.account.account.domainid
                            )
        self.debug("Created user: %s" % user_2.id)
        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  serviceofferingid=self.service_offering.id
                                  )
        self.debug("Deployed VM in account: %s, ID: %s" % (
                                                           self.account.account.name,
                                                           vm_1.id
                                                           ))
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  serviceofferingid=self.service_offering.id
                                  )
        self.debug("Deployed VM in account: %s, ID: %s" % (
                                                           self.account.account.name,
                                                           vm_2.id
                                                           ))
        # Get users associated with an account
        # (Total 3: 2 - Created & 1 default generated while account creation)
        users = list_users(
                          self.apiclient,
                          account=self.account.account.name,
                          domainid=self.account.account.domainid
                          )
        self.assertEqual(
                         isinstance(users, list),
                         True,
                         "Check for valid list users response"
                         )
        for user in users:

            self.debug("Deleting user: %s" % user.id)
            cmd = deleteUser.deleteUserCmd()
            cmd.id = user.id
            self.apiclient.deleteUser(cmd)

        interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
        self.assertEqual(
                         isinstance(interval, list),
                         True,
                         "Check for valid list configurations response"
                         )
        self.debug("account.cleanup.interval: %s" % interval[0].value)

        # Sleep to ensure that all resources are deleted
        time.sleep(int(interval[0].value))

        # Account is removed after last user is deleted
        account_response = list_accounts(
                                         self.apiclient,
                                         id=self.account.account.id
                                         )
        self.assertEqual(
                            account_response,
                            None,
                            "Check List VM response"
                            )
        # All VMs associated with account are removed.
        vm_response = list_virtual_machines(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            vm_response,
                            None,
                            "Check List VM response"
                            )
        # DomR associated with account is deleted
        with self.assertRaises(Exception):
            list_routers(
                          self.apiclient,
                          account=self.account.account.name,
                          domainid=self.account.account.domainid
                        )
        return


class TestNonRootAdminsPrivileges(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNonRootAdminsPrivileges,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone settings
        cls.zone = get_zone(cls.api_client, cls.services)

        # Create an account, domain etc
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"],
                                   )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls._cleanup = [
                        cls.account,
                        cls.domain
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
            #Clean up, terminate the created accounts
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"])
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
        self.debug("Created account: %s" % account_1.account.name)
        self.cleanup.append(account_1)
        account_2 = Account.create(
                            self.apiclient,
                            self.services["account"]
                            )
        self.debug("Created account: %s" % account_2.account.name)
        self.cleanup.append(account_2)

        accounts_response = list_accounts(
                                          self.apiclient,
                                          domainid=self.domain.id
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

        # Create Domains, accounts etc
        cls.domain_1 = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )
        cls.domain_2 = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"],
                                            domainid=cls.domain_1.id
                                            )
        # Create account for doamin_1
        cls.account_1 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain_1.id
                            )

        # Create an account for domain_2
        cls.account_2 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain_2.id
                            )

        cls._cleanup = [
                        cls.account_1,
                        cls.account_2,
                        cls.service_offering,
                        cls.domain_1,
                        cls.domain_2,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
            #Clean up, terminate the created domains, accounts
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"])
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
               "Check Service offering ID for domain" + str(self.domain_1.name)
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


@unittest.skip("Open Questions")
class TestServiceOfferingHierarchy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestServiceOfferingHierarchy,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Create domain, service offerings etc
        cls.domain_1 = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )
        cls.domain_2 = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"],
                                   parentdomainid=cls.domain_1.id
                                   )
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"],
                                            domainid=cls.domain_1.id
                                            )
        # Create account for doamin_1
        cls.account_1 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain_1.id
                            )

        # Create an account for domain_2
        cls.account_2 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain_2.id
                            )

        cls._cleanup = [
                        cls.account_1,
                        cls.account_2,
                        cls.service_offering,
                        cls.domain_1,
                        cls.domain_2,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"])
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
               "Check Service offering ID for domain" + str(self.domain_1.name)
            )

        # Verify private service offering is not visible to other domain
        service_offerings = list_service_offering(
                                                  self.apiclient,
                                                  domainid=self.domain_2.id
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
               "Check Service offering ID for domain" + str(self.domain_2.name)
            )
        return


@unittest.skip("Open Questions")
class TesttemplateHierarchy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TesttemplateHierarchy,
                               cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone settings
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services["template"]["zoneid"] = cls.zone.id

        # Create domains, accounts and template
        cls.domain_1 = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )
        cls.domain_2 = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"],
                                   parentdomainid=cls.domain_1.id
                                   )

        # Create account for doamin_1
        cls.account_1 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain_1.id
                            )

        # Create an account for domain_2
        cls.account_2 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain_2.id
                            )

        cls.template = Template.register(
                                            cls.api_client,
                                            cls.services["template"],
                                            account=cls.account_1.account.name,
                                            domainid=cls.domain_1.id
                                        )
        cls._cleanup = [
                        cls.template,
                        cls.account_1,
                        cls.account_2,
                        cls.domain_1,
                        cls.domain_2,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"])
    def test_01_template_hierarchy(self):
        """Test to verify template at same level in hierarchy"""

        # Validate the following
        # 1. Verify template is visible for domain_1
        # 2. Verify template is also visible for domain_2

        # Sleep to ensure that template state is reflected across
        time.sleep(self.services["sleep"])

        templates = list_templates(
                                    self.apiclient,
                                    templatefilter='self',
                                    account=self.account_1.account.name,
                                    domainid=self.domain_1.id
                                )
        self.assertEqual(
                            isinstance(templates, list),
                            True,
                            "Check List templates for a valid response"
                        )
        self.assertNotEqual(
                            len(templates),
                            0,
                            "Check List Template response"
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
                                    templatefilter='self',
                                    account=self.account_2.account.name,
                                    domainid=self.domain_2.id
                                )
        self.assertEqual(
                            isinstance(templates, list),
                            True,
                            "Check List templates for a valid response"
                        )
        self.assertNotEqual(
                            len(templates),
                            0,
                            "Check List Service Offerings response"
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
        cls.api_client = super(
                               TestAddVmToSubDomain,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Setup working Environment- Create domain, zone, pod cluster etc.
        cls.domain = get_domain(
                                   cls.api_client,
                                   cls.services
                                   )
        cls.zone = get_zone(
                               cls.api_client,
                               cls.services,
                               )
        cls.sub_domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"],
                                   parentdomainid=cls.domain.id
                                   )

        # Create account for doamin_1
        cls.account_1 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        # Create an account for domain_2
        cls.account_2 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.sub_domain.id
                            )

        cls.service_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offering"],
                                    domainid=cls.domain.id
                                    )
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.vm_1 = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=cls.template.id,
                                    accountid=cls.account_1.account.name,
                                    domainid=cls.account_1.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )

        cls.vm_2 = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=cls.template.id,
                                    accountid=cls.account_2.account.name,
                                    domainid=cls.account_2.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )
        cls._cleanup = [
                        cls.account_2,
                        cls.account_1,
                        cls.sub_domain,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Clean up, terminate the created resources
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
            #Clean up, terminate the created resources
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"])
    def test_01_add_vm_to_subdomain(self):
        """ Test Sub domain allowed to launch VM  when a Domain level zone is
            created"""

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
        cls.api_client = super(
                               TestUserDetails,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain etc
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
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
                                            self.account.account.name)
        users = User.list(
                          self.apiclient,
                          account=self.account.account.name,
                          domainid=self.account.account.domainid
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
                                            self.account.account.name)
        users = User.list(
                          self.apiclient,
                          account=self.account.account.name,
                          domainid=self.account.account.domainid
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
                                            self.account.account.name)
        users = User.list(
                          self.apiclient,
                          account=self.account.account.name,
                          domainid=self.account.account.domainid
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

@unittest.skip("Login API response returns nothing")
class TestUserLogin(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestUserLogin,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain etc
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

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
                             username=self.account.account.name,
                             password=self.services["account"]["password"]
                             )
        self.assertEqual(respose, None, "Login response should not be none")
        self.debug("Login API response: %s" % respose)

        self.assertNotEqual(
                            respose.sessionkey,
                            None,
                            "Login to the CloudStack should be successful" +
                            "response shall have non Null key"
                            )
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
                                name=self.account.account.name,
                                domainid=self.account.account.domainid,
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
                             username=self.account.account.name,
                             password=self.services["account"]["password"]
                             )
        self.assertEqual(respose, None, "Login response should not be none")
        self.debug("Login API response: %s" % respose)

        self.assertNotEqual(
                            respose.sessionkey,
                            None,
                            "Login to the CloudStack should be successful" +
                            "response shall have non Null key"
                            )
        return


class TestDomainForceRemove(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestDomainForceRemove,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Setup working Environment- Create domain, zone, pod cluster etc.
        cls.domain = get_domain(
                                   cls.api_client,
                                   cls.services
                                   )
        cls.zone = get_zone(
                               cls.api_client,
                               cls.services,
                               )

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Clean up, terminate the created resources
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
            #Clean up, terminate the created resources
            cleanup_resources(self.apiclient, self.cleanup)
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["domains", "advanced", "advancedns", "simulator"])
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
        domain = Domain.create(
                                self.apiclient,
                                self.services["domain"],
                                parentdomainid=self.domain.id
                                )
        self.debug("Domain is created succesfully.")
        self.debug(
            "Checking if the created domain is listed in list domains API")
        domains = Domain.list(self.apiclient, id=domain.id, listall=True)

        self.assertEqual(
                         isinstance(domains, list),
                         True,
                         "List domains shall return a valid response"
                         )
        self.debug("Creating 2 user accounts in domain: %s" % domain.name)
        self.account_1 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     domainid=domain.id
                                     )

        self.account_2 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     domainid=domain.id
                                     )

        self.debug("Creating a tiny service offering for VM deployment")
        self.service_offering = ServiceOffering.create(
                                    self.apiclient,
                                    self.services["service_offering"],
                                    domainid=self.domain.id
                                    )

        self.debug("Deploying virtual machine in account 1: %s" %
                                                self.account_1.account.name)
        vm_1 = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.template.id,
                                    accountid=self.account_1.account.name,
                                    domainid=self.account_1.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                    )

        self.debug("Deploying virtual machine in account 2: %s" %
                                                self.account_2.account.name)
        vm_2 = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.template.id,
                                    accountid=self.account_2.account.name,
                                    domainid=self.account_2.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                    )

        networks = Network.list(
                                self.apiclient,
                                account=self.account_1.account.name,
                                domainid=self.account_1.account.domainid,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "List networks should return a valid response"
                         )
        network_1 = networks[0]
        self.debug("Default network in account 1: %s is %s" % (
                                                self.account_1.account.name,
                                                network_1.name))
        src_nat_list = PublicIPAddress.list(
                                    self.apiclient,
                                    associatednetworkid=network_1.id,
                                    account=self.account_1.account.name,
                                    domainid=self.account_1.account.domainid,
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
        #Create NAT rule
        nat_rule = NATRule.create(
                                  self.apiclient,
                                  vm_1,
                                  self.services["natrule"],
                                  ipaddressid=src_nat.id
                           )
        self.debug("Created PF rule on source NAT: %s" % src_nat.ipaddress)

        nat_rules = NATRule.list(self.apiclient, id=nat_rule.id)

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

        self.debug("Deleting domain with force option")
        try:
            domain.delete(self.apiclient, cleanup=True)
        except Exception as e:
            self.fail("Failed to delete domain: %s" % e)

        self.debug("Waiting for account.cleanup.interval" +
                   " to cleanup any remaining resouces")

        configurations = Configurations.list(
                                            self.apiclient,
                                            name="account.cleanup.interval",
                                            listall=True
                                            )
        self.debug("account.cleanup.interval: %s" %
                                            int(configurations[0].value))
        # Sleep to ensure that all resources are deleted
        time.sleep(int(configurations[0].value) * 2)
        self.debug("Checking if the resources in domain are deleted or not..")
        accounts = Account.list(
                                self.apiclient,
                                name=self.account_1.account.name,
                                domainid=self.account_1.account.domainid,
                                listall=True
                                )

        self.assertEqual(
            accounts,
            None,
            "Account should get automatically deleted after domain removal"
            )
        return

    @attr(tags=["domains", "advanced", "advancedns", "simulator"])
    def test_DeleteDomain(self):
        """ Test delete domain with force option"""

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
        domain = Domain.create(
                                self.apiclient,
                                self.services["domain"],
                                parentdomainid=self.domain.id
                                )
        self._cleanup.append(domain)
        self.debug("Domain: %s is created successfully." % domain.name)
        self.debug(
            "Checking if the created domain is listed in list domains API")
        domains = Domain.list(self.apiclient, id=domain.id, listall=True)

        self.assertEqual(
                         isinstance(domains, list),
                         True,
                         "List domains shall return a valid response"
                         )
        self.debug("Creating 2 user accounts in domain: %s" % domain.name)
        self.account_1 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     domainid=domain.id
                                     )
        self.cleanup.append(self.account_1)

        self.account_2 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     domainid=domain.id
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
                                                self.account_1.account.name)
        vm_1 = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.template.id,
                                    accountid=self.account_1.account.name,
                                    domainid=self.account_1.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                    )

        self.debug("Deploying virtual machine in account 2: %s" %
                                                self.account_2.account.name)
        vm_2 = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.template.id,
                                    accountid=self.account_2.account.name,
                                    domainid=self.account_2.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                    )

        networks = Network.list(
                                self.apiclient,
                                account=self.account_1.account.name,
                                domainid=self.account_1.account.domainid,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "List networks should return a valid response"
                         )
        network_1 = networks[0]
        self.debug("Default network in account 1: %s is %s" % (
                                                self.account_1.account.name,
                                                network_1.name))
        src_nat_list = PublicIPAddress.list(
                                    self.apiclient,
                                    associatednetworkid=network_1.id,
                                    account=self.account_1.account.name,
                                    domainid=self.account_1.account.domainid,
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
        #Create NAT rule
        nat_rule = NATRule.create(
                                  self.apiclient,
                                  vm_1,
                                  self.services["natrule"],
                                  ipaddressid=src_nat.id
                           )
        self.debug("Created PF rule on source NAT: %s" % src_nat.ipaddress)

        nat_rules = NATRule.list(self.apiclient, id=nat_rule.id)

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
        with self.assertRaises(Exception):
            domain.delete(self.apiclient, cleanup=False)
        return
