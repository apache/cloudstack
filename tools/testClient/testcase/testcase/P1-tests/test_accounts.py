# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" P1 tests for Account
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *
import remoteSSHClient
import datetime


class Services:
    """Test Account Services
    """

    def __init__(self):
        self.services = {
                        "domain": {
                                   "name": "Domain",
                                   },
                        "zone": {
                                 "dns1": '121.242.190.180',
                                 "internaldns1": '192.168.100.1',
                                 "name" : "Test Zone",
                                 "networktype" : "Basic",
                                 "dns2": '121.242.190.211',
                                 },
                         "pod": {
                                 "name": "Test Pod",
                                 "gateway": '192.168.100.1',
                                 "netmask": '255.255.255.0',
                                 "startip": '192.168.100.132',
                                 "endip": '192.168.100.140',
                                 },
                         "public_ip": {
                                 "gateway": '192.168.100.1',
                                 "netmask": '255.255.255.0',
                                 "forvirtualnetwork": False,
                                 "startip": '192.168.100.142',
                                 "endip": '192.168.100.149',
                                 "vlan": "untagged",
                                 },
                         "cluster": {
                                "clustername": "Xen Cluster",
                                "clustertype": "CloudManaged",
                                # CloudManaged or ExternalManaged"
                                "hypervisor": "XenServer",
                                # Hypervisor type
                                },
                         "host": {
                                "hypervisor": 'XenServer',
                                # Hypervisor type
                                "clustertype": 'CloudManaged',
                                # CloudManaged or ExternalManaged"
                                "url": 'http://192.168.100.211',
                                "username": "root",
                                "password": "fr3sca",
                                "port": 22,
                                "ipaddress": '192.168.100.211'
                            },

                         "primary_storage": {
                                "name": "Test Primary",
                                "url": "nfs://192.168.100.150/mnt/DroboFS/Shares/nfsclo3",
                                # Format: File_System_Type/Location/Path
                            },
                        "sec_storage": {
                                 "url": "nfs://192.168.100.150/mnt/DroboFS/Shares/nfsclo4"
                                 # Format: File_System_Type/Location/Path


                            },
                        "mgmt_server": {
                                        "ipaddress": '192.168.100.154',
                                        "port": 22,
                                        "username": 'root',
                                        "password": 'fr3sca',
                        },
                        "sysVM": {
                                        "mnt_dir": '/mnt/test',
                                        "sec_storage": '192.168.100.150',
                                        "path": 'TestSec',
                                        "command": '/usr/lib64/cloud/agent/scripts/storage/secondary/cloud-install-sys-tmplt',
                                        "download_url": 'http://download.cloud.com/releases/2.2.0/systemvm.vhd.bz2',
                                        "hypervisor": "xenserver",
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
                                    "cpuspeed": 100, # in MHz
                                    "memory": 64, # In MBs
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
                                "ostypeid": 126,
                                "url": "http://download.cloud.com/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                                "hypervisor": 'XenServer',
                                "format" : 'VHD',
                                "isfeatured": True,
                                "ispublic": True,
                                "isextractable": True,
                        },
                        "ostypeid": 12,
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                        "mode":'advanced'
                    }


class TestAccounts(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestAccounts, cls).getClsTestClient().getApiClient()
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
        cls.api_client = super(TestRemoveUserFromAccount, cls).getClsTestClient().getApiClient()
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
        cls.api_client = super(TestNonRootAdminsPrivileges, cls).getClsTestClient().getApiClient()
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
        cls.api_client = super(TestServiceOfferingSiblings, cls).getClsTestClient().getApiClient()
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
        cls.api_client = super(TestServiceOfferingHierarchy, cls).getClsTestClient().getApiClient()
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
        cls.api_client = super(TesttemplateHierarchy, cls).getClsTestClient().getApiClient()
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

@unittest.skip("Open Questions")
class TestAddVmToSubDomain(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestAddVmToSubDomain, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Setup working Environment- Create domain, zone, pod cluster etc.
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )
        cls.zone = Zone.create(
                               cls.api_client,
                               cls.services["zone"],
                               domainid=cls.domain.id
                               )
        cls.services["pod"]["zoneid"] = cls.zone.id

        cls.pod = Pod.create(
                             cls.api_client,
                             cls.services["pod"]
                             )
        cls.services["public_ip"]["zoneid"] = cls.zone.id
        cls.services["public_ip"]["podid"] = cls.pod.id

        cls.public_ip_range = PublicIpRange.create(
                                              cls.api_client,
                                              cls.services["public_ip"]
                                              )
        cls.services["cluster"]["zoneid"] = cls.zone.id
        cls.services["cluster"]["podid"] = cls.pod.id

        cls.cluster = Cluster.create(
                                     cls.api_client,
                                     cls.services["cluster"]
                                     )

        cls.services["host"]["zoneid"] = cls.zone.id
        cls.services["host"]["podid"] = cls.pod.id

        cls.host = Host.create(
                               cls.api_client,
                               cls.cluster,
                               cls.services["host"]
                               )

        cls.services["primary_storage"]["zoneid"] = cls.zone.id
        cls.services["primary_storage"]["podid"] = cls.pod.id

        cls.primary_storage = StoragePool.create(
                                        cls.api_client,
                                        cls.services["primary_storage"],
                                        cls.cluster.id
                                        )

        # before adding Sec Storage, First download System Templates on it
        download_systemplates_sec_storage(
                                    cls.services["mgmt_server"],
                                    cls.services["sysVM"]
                                    )

        cls.services["sec_storage"]["zoneid"] = cls.zone.id
        cls.services["sec_storage"]["podid"] = cls.pod.id

        cls.secondary_storage = SecondaryStorage.create(
                                        cls.api_client,
                                        cls.services["sec_storage"]
                                        )
        # After adding Host, Clusters wait for SSVMs to come up
        wait_for_ssvms(
                       cls.api_client,
                       cls.zone.id,
                       cls.pod.id
                       )

        ssvm_response = list_ssvms(
                                    cls.api_client,
                                    systemvmtype='secondarystoragevm',
                                    hostid=cls.host.id,
                                    sleep=cls.services["sleep"]
                                )
        if isinstance(ssvm_response, list):
            ssvm = ssvm_response[0]
        else:
            raise Exception("List SSVM failed")
        
        # Download BUILTIN templates
        download_builtin_templates(
                                   cls.api_client,
                                   cls.zone.id,
                                   cls.services["cluster"]["hypervisor"],
                                   cls.services["host"],
                                   ssvm.linklocalip
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
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup the accounts
            cls.account_1.delete(cls.api_client)
            cls.account_2.delete(cls.api_client)

            cleanup_wait = list_configurations(
                                          cls.api_client,
                                          name='account.cleanup.interval'
                                          )
            # Sleep for account.cleanup.interval * 2 to wait for expunge of
            # resources associated with that account
            if isinstance(cleanup_wait, list):
                sleep_time = int(cleanup_wait[0].value) * 2
            
            time.sleep(sleep_time)
            
            # Delete Service offerings and sub-domains
            cls.service_offering.delete(cls.api_client)
            cls.sub_domain.delete(cls.api_client)

            # Enable maintenance mode of 
            cls.host.enableMaintenance(cls.api_client)
            cls.primary_storage.enableMaintenance(cls.api_client)

            # Destroy SSVMs and wait for volumes to cleanup 
            ssvms = list_ssvms(
                               cls.api_client,
                               zoneid=cls.zone.id
                               )
            
            if isinstance(ssvms, list):
                for ssvm in ssvms:
                    cmd = destroySystemVm.destroySystemVmCmd()
                    cmd.id = ssvm.id
                    cls.api_client.destroySystemVm(cmd)

            # Sleep for account.cleanup.interval*2 to wait for SSVM volume
            # to cleanup
            time.sleep(sleep_time)

            # Cleanup Primary, secondary storage, hosts, zones etc.
            cls.secondary_storage.delete(cls.api_client)
            cls.host.delete(cls.api_client)
            
            cls.primary_storage.delete(cls.api_client)
            cls.cluster.delete(cls.api_client)
            cls.pod.delete(cls.api_client)
            cls.zone.delete(cls.api_client)
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
