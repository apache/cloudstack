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
                "email": "rmurphy@cloudstack.org",
                "firstname": "Ryan",
                "lastname": "Murphy",
                "username": "rmurphy",
                "password": "internalcloudstackpassword",
                },
            "ldapConfiguration_1":
                {
                "basedn": "dc=cloudstack,dc=org",
                "emailAttribute": "mail",
                "realnameAttribute": "cn",
                "userObject": "inetOrgPerson",
                "usernameAttribute": "uid",
                "hostname": "localhost",
                "port": "10389",
                "ldapUsername": "rmurphy",
                "ldapPassword": "password"
                }
        }


class TestLdap(cloudstackTestCase):
    """
    This tests attempts to register a LDAP server and authenticate as an LDAP user.
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
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as tde:
            raise Exception("Warning: Exception during cleanup : %s" % tde)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()

        self.acct = createAccount.createAccountCmd()
        self.acct.accounttype = 0
        self.acct.firstname = self.services["account"]["firstname"]
        self.acct.lastname = self.services["account"]["lastname"]
        self.acct.password = self.services["account"]["password"]
        self.acct.username = self.services["account"]["username"]
        self.acct.email = self.services["account"]["email"]
        self.acct.account = self.services["account"]["username"]
        self.acct.domainid = 1

        self.acctRes = self.apiClient.createAccount(self.acct)

        return

    def tearDown(self):

        try:
            deleteAcct = deleteAccount.deleteAccountCmd()
            deleteAcct.id = self.acctRes.id

            acct_name=self.acctRes.name

            self.apiClient.deleteAccount(deleteAcct)

            self.debug("Deleted the the following account name %s:" %acct_name)

            if(self.ldapconfRes==1):
                self._deleteLdapConfiguration(self.services["ldapConfiguration_1"])

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"])
    def test_01_addLdapConfiguration(self):
        """
        This test configures LDAP and attempts to authenticate as a user.
        """


        self.debug("start test")

        self.ldapconfRes=self._addLdapConfiguration(self.services["ldapConfiguration_1"])

        if(self.ldapconfRes==1):

            self.debug("Ldap Configuration was succcessful")

            loginRes = self._checkLogin(self.services["ldapConfiguration_1"]["ldapUsername"],self.services["ldapConfiguration_1"]["ldapPassword"])
            self.debug(loginRes)
            self.assertEquals(loginRes,1,"Ldap Authentication")

        else:

            self.debug("LDAP Configuration failed with exception")

            self.assertEquals(self.ldapconfRes,1,"addLdapConfiguration failed")


        self.debug("end test")

    def _addLdapConfiguration(self,ldapConfiguration):

        """

        :param ldapConfiguration

        """

        # Setup Global settings

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.basedn"
        updateConfigurationCmd.value = ldapConfiguration['basedn']
        updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
        self.debug("updated the parameter %s with value %s"%(updateConfigurationResponse.name, updateConfigurationResponse.value))

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.email.attribute"
        updateConfigurationCmd.value = ldapConfiguration['emailAttribute']
        updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
        self.debug("updated the parameter %s with value %s"%(updateConfigurationResponse.name, updateConfigurationResponse.value))


        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.realname.attribute"
        updateConfigurationCmd.value = ldapConfiguration['realnameAttribute']
        updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
        self.debug("updated the parameter %s with value %s"%(updateConfigurationResponse.name, updateConfigurationResponse.value))


        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.user.object"
        updateConfigurationCmd.value = ldapConfiguration['userObject']
        updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
        self.debug("updated the parameter %s with value %s"%(updateConfigurationResponse.name, updateConfigurationResponse.value))


        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.username.attribute"
        updateConfigurationCmd.value = ldapConfiguration['usernameAttribute']
        updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
        self.debug("updated the parameter %s with value %s"%(updateConfigurationResponse.name, updateConfigurationResponse.value))

        self.debug("start addLdapConfiguration test")

        ldapServer = addLdapConfiguration.addLdapConfigurationCmd()
        ldapServer.hostname = ldapConfiguration['hostname']
        ldapServer.port = ldapConfiguration['port']

        self.debug("calling addLdapConfiguration API command")
        try:
            self.apiClient.addLdapConfiguration(ldapServer)
            self.debug("addLdapConfiguration was successful")
            return 1
        except Exception, e:
            self.debug("addLdapConfiguration failed %s" %e)
            return 0

    def _deleteLdapConfiguration(self,ldapConfiguration):

        """

        :param ldapConfiguration

        """

        ldapServer = deleteLdapConfiguration.deleteLdapConfigurationCmd()
        ldapServer.hostname = ldapConfiguration["hostname"]

        try:
            self.apiClient.deleteLdapConfiguration(ldapServer)
            self.debug("deleteLdapConfiguration was successful")
            return 1
        except Exception, e:
            self.debug("deleteLdapConfiguration failed %s" %e)
            return 0

    def _checkLogin(self, username, password):
        """

        :param username:
        :param password:

        """
        self.debug("Attempting to login.")

        try:
            loginParams = login.loginCmd()
            loginParams.username = username
            loginParams.password = password
            loginRes = self.apiClient.login(loginParams)
            self.debug("login response %s" % loginRes)
            if loginRes is None:
                self.debug("login not successful")
                return 0
            else:
                self.debug("login successful")
                return 1

        except Exception, p:
            self.debug("login operation failed %s" %p)
        self.debug("end of Login")
