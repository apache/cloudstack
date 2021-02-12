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
""" 

Test cases relating to acess checks for deleteNetwork() for Admin, domain admin and regular users

"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.cloudstackException import CloudstackAclException
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True

class TestIsolatedNetworkDelete(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        """
        Create the following domain tree and accounts that are reqiured for executing listNetwork() test cases:
        Under ROOT - create 2 domaind D1 and D2
        Under D1 - Create 2 subdomain D11 and D12
        Under D11 - Create subdimain D111

        Under each of the domain create 1 admin user and couple of regular users.

        As each of these users , deploy Virtual machines which results in an isolated network being created.

        """

        cls.testclient = super(TestIsolatedNetworkDelete, cls).getClsTestClient()
        cls.apiclient = cls.testclient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.acldata = cls.testdata["acl"]

        cls.domain_1 = None
        cls.domain_2 = None
        cls.cleanup = []

        try:
            # backup default apikey and secretkey
            cls.default_apikey = cls.apiclient.connection.apiKey
            cls.default_secretkey = cls.apiclient.connection.securityKey

            # Create domains 
            cls.domain_1 = Domain.create(
                                       cls.apiclient,
                                       cls.acldata["domain1"]
                                       )
            cls.domain_11 = Domain.create(
                                       cls.apiclient,
                                       cls.acldata["domain11"],
                                       parentdomainid=cls.domain_1.id
                                       )
            cls.domain_111 = Domain.create(
                                       cls.apiclient,
                                       cls.acldata["domain111"],
                                       parentdomainid=cls.domain_11.id,
                                       )
            cls.domain_12 = Domain.create(
                                       cls.apiclient,
                                       cls.acldata["domain12"],
                                       parentdomainid=cls.domain_1.id
                                       )
            cls.domain_2 = Domain.create(
                                       cls.apiclient,
                                       cls.acldata["domain2"]
                                       )
            # Create  1 admin account and 2 user accounts for doamin_1
            cls.account_d1 = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD1"],
                                admin=True,
                                domainid=cls.domain_1.id
                                )
    
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d1)
            cls.user_d1_apikey = user.apikey
            cls.user_d1_secretkey = user.secretkey
            
            cls.account_d1a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD1A"],
                                admin=False,
                                domainid=cls.domain_1.id
                                )        
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d1a)
            cls.user_d1a_apikey = user.apikey
            cls.user_d1a_secretkey = user.secretkey
            
    
            cls.account_d1b = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD1B"],
                                admin=False,
                                domainid=cls.domain_1.id
                                )
    
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d1b)
            cls.user_d1b_apikey = user.apikey
            cls.user_d1b_secretkey = user.secretkey
      
            # Create  1 admin and 2 user accounts for doamin_11
            cls.account_d11 = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD11"],
                                admin=True,
                                domainid=cls.domain_11.id
                                )        
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d11)
            cls.user_d11_apikey = user.apikey
            cls.user_d11_secretkey = user.secretkey
    
            cls.account_d11a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD11A"],
                                admin=False,
                                domainid=cls.domain_11.id
                                )        
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d11a)
            cls.user_d11a_apikey = user.apikey
            cls.user_d11a_secretkey = user.secretkey
    
            cls.account_d11b = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD11B"],
                                admin=False,
                                domainid=cls.domain_11.id
                                )  
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d11b)
            cls.user_d11b_apikey = user.apikey
            cls.user_d11b_secretkey = user.secretkey
    
            # Create  2 user accounts and 1 admin account for doamin_111
    
            cls.account_d111 = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD111"],
                                admin=True,
                                domainid=cls.domain_111.id
                                )        
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d111)
            cls.user_d111_apikey = user.apikey
            cls.user_d111_secretkey = user.secretkey
          
            cls.account_d111a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD111A"],
                                admin=False,
                                domainid=cls.domain_111.id
                                )        
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d111a)
            cls.user_d111a_apikey = user.apikey
            cls.user_d111a_secretkey = user.secretkey
          
            cls.account_d111b = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD111B"],
                                admin=False,
                                domainid=cls.domain_111.id
                                )        
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d111b)
            cls.user_d111b_apikey = user.apikey
            cls.user_d111b_secretkey = user.secretkey
          
            # Create  2 user accounts for doamin_12
            cls.account_d12a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD12A"],
                                admin=False,
                                domainid=cls.domain_12.id
                                )        
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d12a)
            cls.user_d12a_apikey = user.apikey
            cls.user_d12a_secretkey = user.secretkey
    
            cls.account_d12b = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD12B"],
                                admin=False,
                                domainid=cls.domain_12.id
                                )  
          
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d12b)
            cls.user_d12b_apikey = user.apikey
            cls.user_d12b_secretkey = user.secretkey
          
            # Create 1 user account for domain_2
    
            cls.account_d2a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD2"],
                                admin=False,
                                domainid=cls.domain_2.id
                                )
            
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d2a)
            cls.user_d2a_apikey = user.apikey
            cls.user_d2a_secretkey = user.secretkey
                             
                    
            # Create 1 user account and admin account in "ROOT" domain
    
            cls.account_roota = Account.create(
                                cls.apiclient,
                                cls.acldata["accountROOTA"],
                                admin=False,
                                )
            
            user = cls.generateKeysForUser(cls.apiclient,cls.account_roota)
            cls.user_roota_apikey = user.apikey
            cls.user_roota_secretkey = user.secretkey
    
            cls.account_root = Account.create(
                                cls.apiclient,
                                cls.acldata["accountROOT"],
                                admin=True,
                                )
            
            user = cls.generateKeysForUser(cls.apiclient,cls.account_root)
            cls.user_root_apikey = user.apikey
            cls.user_root_secretkey = user.secretkey
    
            # create service offering
            cls.service_offering = ServiceOffering.create(
                                    cls.apiclient,
                                    cls.acldata["service_offering"]["small"]
                                    )
            
            cls.zone = get_zone(cls.apiclient,cls.testclient.getZoneForTests())
            cls.acldata['mode'] = cls.zone.networktype
            cls.template = get_template(cls.apiclient, cls.zone.id, cls.acldata["ostype"])
    
            cls.apiclient.connection.apiKey = cls.default_apikey
            cls.apiclient.connection.securityKey = cls.default_secretkey
    
            list_isolated_network_offerings_response = NetworkOffering.list(
                                                             cls.apiclient,
                                                             name="DefaultIsolatedNetworkOfferingWithSourceNatService"
                                                             )
            cls.isolated_network_offering_id = list_isolated_network_offerings_response[0].id
    
            ## Create Network objects for Update API related test cases
      
            cls.apiclient.connection.apiKey = cls.user_root_apikey
            cls.apiclient.connection.securityKey = cls.user_root_secretkey 
            cls.network_root = cls.createNetwork(cls.apiclient,cls.account_root,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_d1_apikey
            cls.apiclient.connection.securityKey = cls.user_d1_secretkey 
            cls.network_d1 =  cls.createNetwork(cls.apiclient,cls.account_d1,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_d1a_apikey
            cls.apiclient.connection.securityKey = cls.user_d1a_secretkey 
            cls.network_d1a =  cls.createNetwork(cls.apiclient,cls.account_d1a,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_d1b_apikey
            cls.apiclient.connection.securityKey = cls.user_d1b_secretkey 
            cls.network_d1b =  cls.createNetwork(cls.apiclient,cls.account_d1b,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_d11a_apikey
            cls.apiclient.connection.securityKey = cls.user_d11a_secretkey 
            cls.network_d11a = cls.createNetwork(cls.apiclient,cls.account_d11a,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_d11b_apikey
            cls.apiclient.connection.securityKey = cls.user_d11b_secretkey 
            cls.network_d11b = cls.createNetwork(cls.apiclient,cls.account_d11b,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_d12a_apikey
            cls.apiclient.connection.securityKey = cls.user_d12a_secretkey 
            cls.network_d12a =  cls.createNetwork(cls.apiclient,cls.account_d12a,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_roota_apikey
            cls.apiclient.connection.securityKey = cls.user_roota_secretkey 
            cls.network_roota = cls.createNetwork(cls.apiclient,cls.account_roota,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_d111a_apikey
            cls.apiclient.connection.securityKey = cls.user_d111a_secretkey 
            cls.network_d111a =  cls.createNetwork(cls.apiclient,cls.account_d111a,cls.isolated_network_offering_id,cls.zone)
    
            cls.apiclient.connection.apiKey = cls.user_d111b_apikey
            cls.apiclient.connection.securityKey = cls.user_d111b_secretkey 
            cls.network_d111b =  cls.createNetwork(cls.apiclient,cls.account_d111b,cls.isolated_network_offering_id,cls.zone)
    
    
            cls.apiclient.connection.apiKey = cls.user_d2a_apikey
            cls.apiclient.connection.securityKey = cls.user_d2a_secretkey 
            cls.network_d2a = cls.createNetwork(cls.apiclient,cls.account_d2a,cls.isolated_network_offering_id,cls.zone)
    
            cls.cleanup = [
                            cls.account_root,
                            cls.account_roota,
                            cls.service_offering,
                            ]
        except Exception as e:
                cls.domain_1.delete(cls.apiclient,cleanup="true")
                cls.domain_2.delete(cls.apiclient,cleanup="true")
                cleanup_resources(cls.apiclient, cls.cleanup)
                raise Exception("Failed to create the setup required to execute the test cases: %s" % e)

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestIsolatedNetworkDelete, cls).getClsTestClient().getApiClient()
        cls.apiclient.connection.apiKey = cls.default_apikey
        cls.apiclient.connection.securityKey = cls.default_secretkey
        cls.domain_1.delete(cls.apiclient,cleanup="true")
        cls.domain_2.delete(cls.apiclient,cleanup="true")
        cleanup_resources(cls.apiclient, cls.cleanup)
        return

    def setUp(cls):
        cls.apiclient = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()

    def tearDown(cls):
        # restore back default apikey and secretkey
        cls.apiclient.connection.apiKey = cls.default_apikey
        cls.apiclient.connection.securityKey = cls.default_secretkey
        return

## Test cases relating to delete Network as admin user

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_admin(self):
        """
        Validate that Admin should be able to delete network he owns
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey

        self.network_root.delete(self.apiclient)
        response = Network.list(self.apiclient,listall="true",id=self.network_root.id)

        self.assertEqual(response,
                    None,
                    "Admin User is not able to restart network he owns")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_admin_foruserinsamedomain(self):

        """
        Validate that Admin should be able to delete network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey

        self.network_roota.delete(self.apiclient)
        response = Network.list(self.apiclient,listall="true",id=self.network_roota.id)

        self.assertEqual(response,
                    None,
                    "Admin User is not able to delete network owned by users his domain")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_admin_foruserinotherdomain(self):

        # Validate that Admin should be able to delete network for users in his sub domain
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey

        self.network_d12a.delete(self.apiclient)
        response = Network.list(self.apiclient,listall="true",id=self.network_d12a.id)

        self.assertEqual(response,
                    None,
                    "Admin User is not able to delete network owned other users in other domain")

## Test cases relating to delete Network as domain admin user

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_domaindmin(self):
        """
        Validate that Domain admin should be able to delete network for himslef
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey

        self.network_d1.delete(self.apiclient)
        response = Network.list(self.apiclient,listall="true",id=self.network_d1.id)

        self.assertEqual(response,
                    None,
                    "Domain admin User is not able to delete a network he owns")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_domaindmin_foruserinsamedomain(self):

        """
        Validate that Domain admin should be able to delete network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey

        self.network_d1a.delete(self.apiclient)
        response = Network.list(self.apiclient,listall="true",id=self.network_d1a.id)
        self.assertEqual(response,
                    None,
                    "Domain admin User is not able to delete a network that is owned by user in the same domain")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_domaindmin_foruserinsubdomain(self):

        """
        Validate that Domain admin should be able to delete network for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey

        self.network_d11a.delete(self.apiclient)
        response = Network.list(self.apiclient,listall="true",id=self.network_d11a.id)

        self.assertEqual(response,
                    None,
                    "Domain admin User is not able to delete a network that is owned by user in the subdomain")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_domaindmin_forcrossdomainuser(self):

        """
        Validate that Domain admin should be able to delete network for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey

        try:
            response = self.network_d2a.delete(self.apiclient)
            self.fail("Domain admin is allowed to delete network for users not in his domain ")
        except Exception as e:
                self.debug ("When Domain admin tries to delete network for user in a different domain %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                    self.fail("Error message validation failed when Domain admin tries to delete network for users not in his domain ")


## Test cases relating deleting network as regular user

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_user(self):

        """
        Validate that Regular should be able to delete network for himslef
        """
        self.apiclient.connection.apiKey = self.user_d111a_apikey
        self.apiclient.connection.securityKey = self.user_d111a_secretkey

        self.network_d111a.delete(self.apiclient)
        response = Network.list(self.apiclient,listall="true",id=self.network_d111a.id)

        self.assertEqual(response,
                    None,
                    "User is not able to delete a network he owns")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_user_foruserinsamedomain(self):

        """
        Validate that Regular user should NOT be able to delete network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d111a_apikey
        self.apiclient.connection.securityKey = self.user_d111a_secretkey

        try:
                response = self.network_d111b.delete(self.apiclient)
                self.fail("Regular user is allowed to delete network for users in his domain ")
        except Exception as e:
                self.debug ("When user tries to delete network for users in his domain %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                    self.fail("Regular user is allowed to delete network for users in his domain ")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deleteNetwork_user_foruserinotherdomain(self):

        """
        Validate that Domain admin should be NOT be able to delete network for users in other domains
        """
        self.apiclient.connection.apiKey = self.user_d111a_apikey
        self.apiclient.connection.securityKey = self.user_d111a_secretkey

        try:
                response = self.network_d11b.delete(self.apiclient)
                self.fail("Regular user is allowed to delete network for users not in his domain ")
        except Exception as e:
                self.debug ("When user tries to delete network for users in other domain %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                    self.fail("Error message validation failed when Regular user tries to delete network for users not in his domain ")

    @staticmethod
    def generateKeysForUser(apiclient,account):
        user = User.list(
                          apiclient,
                          account=account.name,
                          domainid=account.domainid
                          )[0]

        return (User.registerUserKeys(
                        apiclient,
                        user.id
                      ))

    @staticmethod
    def createNetwork(apiclient,account,isolated_network_offering_id,zone):
        network= {
                 "name": "Network-",
                 "displaytext": "Network-",
                 "gateway" :"10.223.1.1",
                 "netmask" :"255.255.255.0",
                 "startip" :"10.223.1.2",
                 "endip" :"10.223.1.100",
                }

        network["name"] = account.name +" -forupdate"
        network["displayname"] = account.name + "-forupdate"

        network = Network.create(
                         apiclient,
                         network,
                         networkofferingid=isolated_network_offering_id,
                         zoneid=zone.id
                         )
        return network

