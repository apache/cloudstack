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

""" P1 for LDAP Config
"""


#!/usr/bin/env python

import marvin
from marvin import cloudstackTestCase
from marvin.cloudstackTestCase import *
import unittest
import hashlib
import random
from marvin.cloudstackAPI import *
from marvin.cloudstackAPI import login
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr
import urllib



class Services:
    """Test LDAP Configuration
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "test",
                "lastname": "t",
                "username": "test",
                "password": "password",
                },
            "ldapCon_1":#valid values&Query filter as email.
                {
                    "ldapHostname": "10.147.38.163",
                    "port": "389",
                    "binddn": "CN=test,CN=Users,DC=hyd-qa,DC=com",
                    "bindpass": "aaaa_1111",
                    "queryfilter": "(&(mail=%e))",
                    "searchbase": "CN=Users,DC=hyd-qa,DC=com",
                    "ldapusername": "test",
                    "ldappasswd": "aaaa_1111"
                },
            "ldapCon_2": ##valid values&Query filter as displayName.
                {
                    "ldapHostname": "10.147.38.163",
                    "port": "389",
                    "binddn": "CN=test,CN=Users,DC=hyd-qa,DC=com",
                    "bindpass": "aaaa_1111",
                    "queryfilter": "(&(displayName=%u))",
                    "searchbase": "CN=Users,DC=hyd-qa,DC=com",
                    "ldapusername": "test",
                    "ldappasswd": "aaaa_1111"
                },
            "ldapCon_3": #Configuration with missing parameters value(queryfilter)
                {
                    "ldapHostname": "10.147.38.163",
                    "port": "389",
                    "binddn": "CN=test,CN=Users,DC=hyd-qa,DC=com",
                    "bindpass": "aaaa_1111",
                    "queryfilter": "",
                    "searchbase": "CN=Users,DC=hyd-qa,DC=com",
                    "ldapusername": "test",
                    "ldappasswd": "aaaa_1111"
                },

            "ldapCon_4": #invalid configuration-wrong query filter
                {
                    "ldapHostname": "10.147.38.163",
                    "port": "389",
                    "binddn": "CN=test,CN=Users,DC=hyd-qa,DC=com",
                    "bindpass": "aaaa_1111",
                    "queryfilter": "(&(displayName=%p))",
                    "searchbase":"CN=Users,DC=hyd-qa,DC=com",
                    "ldapusername": "test",
                    "ldappasswd": "aaaa_1111"
                },
            "ldapCon_5": #Configuration with invalid ldap credentials
                {
                    "ldapHostname": "10.147.38.163",
                    "port": "389",
                    "binddn": "CN=test,CN=Users,DC=hyd-qa,DC=com",
                    "bindpass": "aaaa_1111",
                    "queryfilter": "(&(displayName=%u))",
                    "searchbase": "CN=Users,DC=hyd-qa,DC=com",
                    "ldapusername": "test",
                    "ldappasswd": "aaaa"
                }



        }


class TestLdap(cloudstackTestCase):
    """
    This test perform registering ldap configuration details in CS and create a user[ldap user] in CS
     and  validate user credentials against LDAP server:AD
    """

    @classmethod
    def setUpClass(cls):

        cls.api_client = super(
            TestLdap,
            cls
        ).getClsTestClient().getApiClient()
        cls.services = Services().services
        cls.account = cls.services["account"]
        cls._cleanup = []



    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            #print "tear down class"
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as tde:
            raise Exception("Warning: Exception during cleanup : %s" % tde)
        return

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()

        self.acct = createAccount.createAccountCmd()
        self.acct.accounttype = 0 #We need a regular user. admins have accounttype=1
        self.acct.firstname = self.services["account"]["firstname"]
        self.acct.lastname = self.services["account"]["lastname"]
        self.acct.password = self.services["account"]["password"]
        self.acct.username = self.services["account"]["username"]
        self.acct.email = self.services["account"]["email"]
        self.acct.account = self.services["account"]["username"]
        self.acct.domainid = 1
        # mapping ldap user  by creating same user in  cloudstack

        self.acctRes = self.apiclient.createAccount(self.acct)


        return

    def tearDown(self):

        try:
            #Clean up, terminate the created accounts, domains etc

            deleteAcct = deleteAccount.deleteAccountCmd()
            deleteAcct.id = self.acctRes.id

            acct_name=self.acctRes.name

            self.apiclient.deleteAccount(deleteAcct)

            self.debug("Deleted the the following account name %s:" %acct_name)
            #delete only if ldapconfig registered  in CS
            if(self.ldapconfRes):
                deleteldapconfg=ldapRemove.ldapRemoveCmd()
                res=self.apiclient.ldapRemove(deleteldapconfg)


        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"])
    def test_01_configLDAP(self):
        '''
        This test is to verify ldapConfig API  with valid  values.(i.e query fileter as email)
        '''
        # 1. This test covers ldapConfig  & login API with valid ldap credentials..
        # require ldap configuration:ldapCon_1

        self.debug("start test")

        self.ldapconfRes=self._testldapConfig(self.services["ldapCon_1"])

        if(self.ldapconfRes==1):


            self.debug("configure ldap successful")

            #validating the user credentials with ldap Server
            loginRes = self.chkLogin(self.services["ldapCon_1"]["ldapusername"], self.services["ldapCon_1"]["ldappasswd"])
            self.assertEquals(loginRes,1,"ldap Authentication failed")

        else:

            self.debug("LDAP Configuration failed with exception")

            self.assertEquals(self.ldapconfRes,1,"ldapConfig API failed")


        self.debug("end test")

    @attr(tags=["advanced", "basic"])
    def test_02_configLDAP(self):
        '''
        This test is to verify ldapConfig API  with valid  values.(i.e query fileter as displayName)
        '''

        # 1. This test covers ldapConfig  & login API with valid ldap credentials.
        # 2. require ldap configuration:ldapCon_2

        self.debug("start test")
        self.ldapconfRes=self._testldapConfig(self.services["ldapCon_2"])
        self.assertEquals(self.ldapconfRes,1,"ldapConfig API failed")
        if(self.ldapconfRes==1):
            self.debug("configure ldap successful")
            #validating the user credentials with ldap Server
            loginRes = self.chkLogin(self.services["ldapCon_2"]["ldapusername"], self.services["ldapCon_2"]["ldappasswd"])
            self.assertEquals(loginRes,1,"ldap Authentication failed")
        else:
            self.debug("LDAP Configuration failed with exception")
        self.debug("end test")

    @attr(tags=["advanced", "basic"])
    def test_03_configLDAP(self):

        '''
        This test is to verify ldapConfig API  with missing config parameters value(i.queryfilter)
        '''

        # 1. Issue ldapConfig API with no ldap config parameter value and check behavior
        # 2. require ldap configuration:ldapCon_3

        self.debug("start test...")
        self.ldapconfRes=self._testldapConfig(self.services["ldapCon_3"])
        self.assertEquals(self.ldapconfRes,0,"LDAP configuration successful with invalid value.API failed")
        self.debug("end test")
    @attr(tags=["advanced", "basic"])
    def test_04_configLDAP(self):
        '''
        This test is to verify ldapConfig API with invalid configuration values(by passing wrong query filter)
        '''
        # 1. calling ldapConfig API with invalid query filter value and check behavior
        # 2. require ldap configuration:ldapCon_4

        self.debug("start test...")
        self.ldapconfRes=self._testldapConfig(self.services["ldapCon_4"])
        self.assertEquals(self.ldapconfRes,0,"API failed")


    @attr(tags=["advanced", "basic"])
    def test_05_configLDAP(self):

        '''
        This test is to verify login API functionality by passing wrong ldap credentials
        '''
        # 1.This script first  configure the ldap and validates the user credentials using login API
        # 2. require ldap configuration:ldapCon_5


        self.debug("start test")
        self.ldapconfRes=self._testldapConfig(self.services["ldapCon_5"])
        self.assertEquals(self.ldapconfRes,1,"API failed")
        #validating the cloudstack user credentials with ldap Server
        loginRes = self.chkLogin(self.services["ldapCon_5"]["ldapusername"], self.services["ldapCon_5"]["ldappasswd"])
        self.assertNotEqual(loginRes,1,"login API failed")
        self.debug("end test")

    @attr(tags=["advanced", "basic"])
    def test_06_removeLDAP(self):
        '''
        This test is to verify ldapRemove API functionality
        '''
        # 1. This script fist configures ldap and removes the configured ldap values
        # 2. require ldap configuration:ldapCon_1


        self.debug("start test")
        self.ldapconfRes=self._testldapConfig(self.services["ldapCon_1"])
        if(self.ldapconfRes==1):
            self.debug("ldap configured successfully")
            deleteldapconfg=ldapRemove.ldapRemoveCmd()
            res=self.apiclient.ldapRemove(deleteldapconfg)
            self.debug("ldap removed successfully")
            self.ldapconfRes=0
        else:

            self.debug("LDAP Configuration failed with exception")
            self.assertEquals(self.ldapconfRes,0,"ldapconfig API failed")
        self.debug("end test")

    def _testldapConfig(self,ldapSrvD):

        """

        :param ldapSrvD


        """
        #This Method takes dictionary as parameter,
        # reads the ldap configuration values from the passed dictionary and
        # register the ldapconfig detail in cloudstack
        # & return true or false based on ldapconfig API response

        self.debug("start ldapconfig  test")
        #creating the  ldapconfig cmd object
        lpconfig = ldapConfig.ldapConfigCmd()
        #Config the ldap server by assigning the ldapconfig dict variable values to ldapConfig object
        lpconfig.hostname = ldapSrvD["ldapHostname"]
        lpconfig.port = ldapSrvD["port"]
        lpconfig.binddn = ldapSrvD["binddn"]
        lpconfig.bindpass = ldapSrvD["bindpass"]
        lpconfig.searchbase = ldapSrvD["searchbase"]
        lpconfig.queryfilter = ldapSrvD["queryfilter"]

        #end of assigning the variables

        #calling the ldapconfig Api
        self.debug("calling ldapconfig API")
        try:
            lpconfig1 = self.apiclient.ldapConfig(lpconfig)
            self.debug("ldapconfig API succesfful")
            return 1
        except Exception, e:
            self.debug("ldapconfig API failed %s" %e)
            return 0

    def chkLogin(self, username, password):
        """

        :param username:
        :param password:

        """
        self.debug("login test")

        try:
            login1 = login.loginCmd()
            login1.username = username
            login1.password = password
            loginRes = self.apiclient.login(login1)
            self.debug("login response %s" % loginRes)
            if loginRes is None:
                self.debug("login not successful")
            else:
                self.debug("login successful")
                return 1

        except Exception, p:
            self.debug("login operation failed %s" %p)
        self.debug("end of Login")
