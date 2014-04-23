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
""" BVT tests for Virtual Machine IAM effect
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.codes import FAILED
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True
class Services:
    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {
            #data for domains and accounts
            "domain1": {
                "name": "Domain1",
             },
            "account1A": {
                "email": "test1A@test.com",
                "firstname": "test1A",
                "lastname": "User",
                "username": "test1A",
                "password": "password",
            },
            "account1B": {
                "email": "test1B@test.com",
                "firstname": "test1B",
                "lastname": "User",
                "username": "test1B",
                "password": "password",
            },                         
            "domain2": {
                "name": "Domain2",
             },
            "account2A": {
                "email": "test2A@test.com",
                "firstname": "test2A",
                "lastname": "User",
                "username": "test2A",
                "password": "password",
            },
            #data reqd for virtual machine creation
            "virtual_machine1A" : {
                "name" : "test1Avm",
                "displayname" : "Test1A  VM",
            },
            "virtual_machine1B" : {
                "name" : "test1Bvm",
                "displayname" : "Test1B  VM",
            }, 
            "virtual_machine2A" : {
                "name" : "test2Avm",
                "displayname" : "Test2A  VM",
            },                                                 
            #small service offering
            "service_offering": {
                "small": {
                    "name": "Small Instance",
                    "displaytext": "Small Instance",
                    "cpunumber": 1,
                    "cpuspeed": 100,
                    "memory": 128,
                },
            },
            "ostype": 'CentOS 5.6 (64-bit)',
            # iam group and policy information
            "service_desk_iam_grp" : {
                "name" : "Service Desk",
                "description" : "Service Desk IAM Group"
            },
            "vm_readonly_iam_policy" : {
                "name" : "VM Read Only Access",
                "description" : "VM read only access iam policy"
            },
        }



class TestVMIam(cloudstackTestCase):

    @classmethod
    def setUpClass(self):
        testClient = super(TestVMIam, self).getClsTestClient()
        self.apiclient = testClient.getApiClient()
        self.services = Services().services
        
        # backup default apikey and secretkey
        self.default_apikey = self.apiclient.connection.apiKey
        self.default_secretkey = self.apiclient.connection.securityKey

        # Create domains and accounts etc
        self.domain_1 = Domain.create(
                                   self.apiclient,
                                   self.services["domain1"]
                                   )
        self.domain_2 = Domain.create(
                                   self.apiclient,
                                   self.services["domain2"]
                                   )
        # Create two accounts for doamin_1
        self.account_1A = Account.create(
                            self.apiclient,
                            self.services["account1A"],
                            admin=False,
                            domainid=self.domain_1.id
                            )
        
        self.account_1B = Account.create(
                            self.apiclient,
                            self.services["account1B"],
                            admin=False,
                            domainid=self.domain_1.id
                            )        

        # Create an account for domain_2
        self.account_2A = Account.create(
                            self.apiclient,
                            self.services["account2A"],
                            admin=False,
                            domainid=self.domain_2.id
                            )
        
        # Fetch user details to register apiKey for them
        self.user_1A = User.list(
                          self.apiclient,
                          account=self.account_1A.name,
                          domainid=self.account_1A.domainid
                          )[0]
       
        user_1A_key = User.registerUserKeys(
                        self.apiclient,
                        self.user_1A.id
                      )  
        self.user_1A_apikey = user_1A_key.apikey
        self.user_1A_secretkey = user_1A_key.secretkey
        
                         
        self.user_1B = User.list(
                          self.apiclient,
                          account=self.account_1B.name,
                          domainid=self.account_1B.domainid
                          )[0]
       
        user_1B_key = User.registerUserKeys(
                        self.apiclient,
                        self.user_1B.id
                      )  
       
        self.user_1B_apikey = user_1B_key.apikey
        self.user_1B_secretkey = user_1B_key.secretkey                    

 
        self.user_2A = User.list(
                          self.apiclient,
                          account=self.account_2A.name,
                          domainid=self.account_2A.domainid
                          )[0]
       
        user_2A_key = User.registerUserKeys(
                        self.apiclient,
                        self.user_2A.id
                      )  
        self.user_2A_apikey = user_2A_key.apikey
        self.user_2A_secretkey = user_2A_key.secretkey
                
        # create service offering
        self.service_offering = ServiceOffering.create(
                                self.apiclient,
                                self.services["service_offering"]["small"]
                                )
        
        self.zone = get_zone(self.apiclient, testClient.getZoneForTests())
        self.services['mode'] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.services["ostype"])

        # deploy 3 VMs for three accounts
        self.virtual_machine_1A = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine1A"],
            accountid=self.account_1A.name,
            zoneid=self.zone.id,
            domainid=self.account_1A.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id
        )  
        
        self.virtual_machine_1B = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine1B"],
            accountid=self.account_1B.name,
            zoneid=self.zone.id,
            domainid=self.account_1B.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id
        )  
        
        self.virtual_machine_2A = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine2A"],
            accountid=self.account_2A.name,
            zoneid=self.zone.id,
            domainid=self.account_2A.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id
        )   
        
        self.srv_desk_grp = IAMGroup.create(
            self.apiclient, 
            self.services["service_desk_iam_grp"]
        )                             

        self.vm_read_policy = IAMPolicy.create(
            self.apiclient, 
            self.services["vm_readonly_iam_policy"]
        )
        
        self.srv_desk_grp.attachPolicy(
            self.apiclient, [self.vm_read_policy]
        )
        
        vm_grant_policy_params = {}
        vm_grant_policy_params['name'] = "policyGrantVirtualMachine" + self.virtual_machine_1A.id
        vm_grant_policy_params['description'] = "Policy to grant permission to VirtualMachine " + self.virtual_machine_1A.id
        self.vm_grant_policy = IAMPolicy.create(
            self.apiclient, 
            vm_grant_policy_params
        )   
        
        self._cleanup = [
                        self.account_1A,
                        self.account_1B,
                        self.domain_1,
                        self.account_2A,
                        self.domain_2,
                        self.service_offering,
                        self.vm_read_policy,
                        self.srv_desk_grp,
                        self.vm_grant_policy
                        ]

    @classmethod
    def tearDownClass(self):
        self.apiclient = super(TestVMIam, self).getClsTestClient().getApiClient()
        cleanup_resources(self.apiclient, self._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        # restore back default apikey and secretkey
        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        cleanup_resources(self.apiclient, self.cleanup)
        return

    

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_01_list_own_vm(self):
        #  listVM command should return owne's VM

        self.debug("Listing VM for account: %s" % self.account_1A.name)

        self.apiclient.connection.apiKey = self.user_1A_apikey
        self.apiclient.connection.securityKey = self.user_1A_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            1,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
            list_vm_response[0].name,
            self.virtual_machine_1A.name,
            "Virtual Machine names do not match"
        )

        self.debug("Listing VM for account: %s" % self.account_1B.name)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            1,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
            list_vm_response[0].name,
            self.virtual_machine_1B.name,
            "Virtual Machine names do not match"
        )
        
        self.debug("Listing VM for account: %s" % self.account_2A.name)

        self.apiclient.connection.apiKey = self.user_2A_apikey
        self.apiclient.connection.securityKey = self.user_2A_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            1,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
            list_vm_response[0].name,
            self.virtual_machine_2A.name,
            "Virtual Machine names do not match"
        )
                
        return
        
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_02_grant_domain_vm(self):
 
        # Validate the following
        # 1. Grant domain2 VM access to account_1B
        # 2. listVM command should return account_1B and domain_2 VMs.

        self.debug("Granting Domain %s VM read only access to account: %s" % (self.domain_2.name, self.account_1B.name))
        
        self.srv_desk_grp.addAccount(self.apiclient, [self.account_1B])
        domain_permission = {}
        domain_permission['action'] = "listVirtualMachines"
        domain_permission['entitytype'] = "VirtualMachine"
        domain_permission['scope'] = "DOMAIN"
        domain_permission['scopeid'] = self.domain_2.id
        self.vm_read_policy.addPermission(self.apiclient, domain_permission)
        
        self.debug("Listing VM for account: %s" % self.account_1B.name)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            2,
                            "Check VM available in List Virtual Machines"
                        )

        list_vm_names = [list_vm_response[0].name, list_vm_response[1].name]
        
        self.assertEqual( self.virtual_machine_1B.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )
        
        self.assertEqual( self.virtual_machine_2A.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )        
        
        return


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_03_grant_account_vm(self):
 
        # Validate the following
        # 1. Grant account_1A VM access to account_1B
        # 2. listVM command should return account_1A and account_1B VMs.

        self.debug("Granting Account %s VM read only access to account: %s" % (self.account_1A.name, self.account_1B.name))
        
        account_permission = {}
        account_permission['action'] = "listVirtualMachines"
        account_permission['entitytype'] = "VirtualMachine"
        account_permission['scope'] = "ACCOUNT"
        account_permission['scopeid'] = self.account_1A.id
        self.vm_read_policy.addPermission(self.apiclient, account_permission)
        
        self.debug("Listing VM for account: %s" % self.account_1B.name)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            3,
                            "Check VM available in List Virtual Machines"
                        )

        list_vm_names = [list_vm_response[0].name, list_vm_response[1].name, list_vm_response[2].name]
        
        self.assertEqual( self.virtual_machine_1B.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )
        
        self.assertEqual( self.virtual_machine_1A.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )    
                
        self.assertEqual( self.virtual_machine_2A.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )        
        
        return


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_04_revoke_account_vm(self):
 
        # Validate the following
        # 1. Revoke account_1A VM access from account_1B
        # 2. listVM command should not return account_1A VMs.

        self.debug("Revoking Account %s VM read only access from account: %s" % (self.account_1A.name, self.account_1B.name))
        
        account_permission = {}
        account_permission['action'] = "listVirtualMachines"
        account_permission['entitytype'] = "VirtualMachine"
        account_permission['scope'] = "ACCOUNT"
        account_permission['scopeid'] = self.account_1A.id
        self.vm_read_policy.removePermission(self.apiclient, account_permission)
        
        self.debug("Listing VM for account: %s" % self.account_1B.name)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            2,
                            "Check VM available in List Virtual Machines"
                        )

        list_vm_names = [list_vm_response[0].name, list_vm_response[1].name]
        
       
        self.assertEqual( self.virtual_machine_1A.name in list_vm_names,
                          False,
                          "Accessible Virtual Machine names do not match"
                          )    
        return
    
    
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_05_revoke_domain_vm(self):
 
        # Validate the following
        # 1. Revoke account_1A VM access from account_1B
        # 2. listVM command should not return account_1A VMs.

        self.debug("Revoking Domain %s VM read only access from account: %s" % (self.domain_1.name, self.account_1B.name))
        
        domain_permission = {}
        domain_permission['action'] = "listVirtualMachines"
        domain_permission['entitytype'] = "VirtualMachine"
        domain_permission['scope'] = "DOMAIN"
        domain_permission['scopeid'] = self.domain_2.id
        self.vm_read_policy.removePermission(self.apiclient, domain_permission)
        
        self.debug("Listing VM for account: %s" % self.account_1B.name)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            1,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
            list_vm_response[0].name,
            self.virtual_machine_1B.name,
            "Virtual Machine names do not match"
        )
         
        return    
    
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_06_grant_resource_vm(self):
 
        # Validate the following
        # 1. Grant a particular vm access to account_1B
        # 2. listVM command should return account_1B VMs and granted VM.

        self.debug("Granting VM %s read only access to account: %s" % (self.virtual_machine_1A.name, self.account_1B.name))
        
        res_permission = {}
        res_permission['action'] = "listVirtualMachines"
        res_permission['entitytype'] = "VirtualMachine"
        res_permission['scope'] = "RESOURCE"
        res_permission['scopeid'] = self.virtual_machine_1A.id
        self.vm_read_policy.addPermission(self.apiclient, res_permission)
        
        self.debug("Listing VM for account: %s" % self.account_1B.name)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            2,
                            "Check VM available in List Virtual Machines"
                        )

        list_vm_names = [list_vm_response[0].name, list_vm_response[1].name]
        
        self.assertEqual( self.virtual_machine_1B.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )
        
        self.assertEqual( self.virtual_machine_1A.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )    
                
        return    
    
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_07_revoke_resource_vm(self):
 
        # Validate the following
        # 1. Grant a particular vm access to account_1B
        # 2. listVM command should return account_1B VMs and granted VM.

        self.debug("Revoking VM %s read only access from account: %s" % (self.virtual_machine_1A.name, self.account_1B.name))
        
        res_permission = {}
        res_permission['action'] = "listVirtualMachines"
        res_permission['entitytype'] = "VirtualMachine"
        res_permission['scope'] = "RESOURCE"
        res_permission['scopeid'] = self.virtual_machine_1A.id
        self.vm_read_policy.removePermission(self.apiclient, res_permission)
        
        self.debug("Listing VM for account: %s" % self.account_1B.id)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            1,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
            list_vm_response[0].name,
            self.virtual_machine_1B.name,
            "Virtual Machine names do not match"
        )
        
        return      
    
    
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_08_policy_attach_account(self):
 
        # Validate the following
        # 1. Grant a particular vm access to account_1B by directly attaching policy to account
        # 2. listVM command should return account_1B VMs and granted VM.

        self.debug("Granting VM %s read only access to account: %s by attaching policy to account" % (self.virtual_machine_1A.name, self.account_1B.name))
        
        res_permission = {}
        res_permission['action'] = "listVirtualMachines"
        res_permission['entitytype'] = "VirtualMachine"
        res_permission['scope'] = "RESOURCE"
        res_permission['scopeid'] = self.virtual_machine_1A.id
        self.vm_grant_policy.addPermission(self.apiclient, res_permission)
        self.vm_grant_policy.attachAccount(self.apiclient, [self.account_1B])
        
        self.debug("Listing VM for account: %s" % self.account_1B.id)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            2,
                            "Check VM available in List Virtual Machines"
                        )

        list_vm_names = [list_vm_response[0].name, list_vm_response[1].name]
        
        self.assertEqual( self.virtual_machine_1B.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )
        
        self.assertEqual( self.virtual_machine_1A.name in list_vm_names,
                          True,
                          "Accessible Virtual Machine names do not match"
                          )    
                
        return     
    
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "selfservice"])
    def test_09_policy_detach_account(self):
 
        # Validate the following
        # 1. Revoking a particular vm access from account_1B by detaching policy from account
        # 2. listVM command should return account_1B VMs.

        self.debug("Revoking VM %s read only access from account: %s by detaching policy from account" % (self.virtual_machine_1A.name, self.account_1B.name))
        
        self.vm_grant_policy.detachAccount(self.apiclient, [self.account_1B])
        
        self.debug("Listing VM for account: %s" % self.account_1B.id)
        self.apiclient.connection.apiKey = self.user_1B_apikey
        self.apiclient.connection.securityKey = self.user_1B_secretkey
        list_vm_response = list_virtual_machines(
                                            self.apiclient
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                            len(list_vm_response),
                            1,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
            list_vm_response[0].name,
            self.virtual_machine_1B.name,
            "Virtual Machine names do not match"
        )
        
        return         