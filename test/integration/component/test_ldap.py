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
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (updateConfiguration,
                                  createAccount,
                                  deleteAccount,
                                  addLdapConfiguration,
                                  deleteLdapConfiguration)
from marvin.cloudstackAPI import login
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr
import telnetlib
from ddt import ddt, data


@ddt
class TestLdap(cloudstackTestCase):

    """
    This tests attempts to register a LDAP server and authenticate as an LDAP user.
    """

    @classmethod
    def setUpClass(cls):

        testClient = super(TestLdap, cls).getClsTestClient()
        cls.api_client = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls._cleanup = []
        cls.delflag = 0
        cls.reason = ""

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
        self.acct.firstname = self.services[
            "configurableData"]["ldap_account"]["firstname"]
        self.acct.lastname = self.services[
            "configurableData"]["ldap_account"]["lastname"]
        self.acct.password = self.services[
            "configurableData"]["ldap_account"]["password"]
        self.acct.username = self.services[
            "configurableData"]["ldap_account"]["username"]
        self.acct.email = self.services[
            "configurableData"]["ldap_account"]["email"]
        self.acct.account = self.services[
            "configurableData"]["ldap_account"]["username"]
        self.acct.domainid = 1

        if self.acct.firstname == ""or self.acct.lastname == "" or self.acct.password == "" or self.acct.username == ""\
                or self.services["configurableData"]["ldap_configuration"]["ldapUsername"] == "" or \
                self.acct.account == "" or \
                self.services["configurableData"]["ldap_configuration"]["ldapPassword"] == "":

            self.debug("Please rerun the test by providing values in ldap configuration user details")
            self.skipTest("Please rerun the test by providing proper values in configuration file")

        else:
            self.delflag = 1
            self.acctRes = self.apiClient.createAccount(self.acct)
        self.assertEqual(self.delflag,  1, "LDAP account details are not provided,please check the configuration")
        return

    def tearDown(self):

        self.debug("In tear down%s" % self.delflag)

        try:
                deleteAcct = deleteAccount.deleteAccountCmd()
                deleteAcct.id = self.acctRes.id

                acct_name = self.acctRes.name

                self.apiClient.deleteAccount(deleteAcct)

                self.debug(
                    "Deleted the the following account name %s:" %
                    acct_name)

                if self.ldapconfRes == 1:
                    self._deleteldapconfiguration(
                        self.services["configurableData"]["ldap_configuration"])

        except Exception as e:
                raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_addLdapConfiguration(self):
        """
        This test configures LDAP and attempts to authenticate as a user.
        """

        self.ldapconfRes = self._addLdapConfiguration(
            self.services["configurableData"]["ldap_configuration"])

        if self.ldapconfRes == 1:

            self.debug("Ldap Configuration was successful")

            loginRes = self._checklogin(
                self.services["configurableData"]["ldap_configuration"]["ldapUsername"],
                self.services["configurableData"]["ldap_configuration"]["ldapPassword"])
            self.debug(loginRes)
            self.assertEqual(loginRes, 1, self.reason)

        else:

            self.debug("LDAP Configuration failed with exception")

            self.assertEqual(
                self.ldapconfRes,
                1,
                self.reason)

    def test_02_validateldapsecuritypatch(self):

        self.ldapconfRes = self._addLdapConfiguration(
            self.services["configurableData"]["ldap_configuration"])
        self.assertEqual(self.ldapconfRes, 1, "Ldap Configuration failed")
        loginRes = self._checklogin(
            self.services["configurableData"]["ldap_configuration"]["ldapUsername"], "")
        self.assertNotEqual(loginRes, 1, "login API Successful with empty password")

    @data("basedn", "ldapPassword")
    def test_03_validateldapbindnobasedn(self, value):
        """
        This test is to verify ldapbind functionality without passing required bind parameters.
        """
        bindvalue = self.services["configurableData"]["ldap_configuration"][value]

        if len(bindvalue) > 0:
            self.services["configurableData"]["ldap_configuration"][value] = ""
            self.ldapconfRes = self._addLdapConfiguration(self.services["configurableData"]["ldap_configuration"])
            if self.reason.__contains__("addLdapConfiguration failed"):
                self.assertEqual(self.ldapconfRes, 1, "Ldap Configuration not successful")
            else:
                self.assertNotEqual(self.ldapconfRes, 1, "Ldap Configuration successful with invalid values-i.e."
                                                         " allowing anonymous bind")
        self.services["configurableData"]["ldap_configuration"][value] = value

    def _addLdapConfiguration(self, ldapConfiguration):
        """

        :param ldapConfiguration

        """
        self.chkConfig = self._checkLdapConfiguration(ldapConfiguration)
        if not self.chkConfig:
            return 0

        # Setup Global settings

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.basedn"
        updateConfigurationCmd.value = ldapConfiguration['basedn']
        updateConfigurationResponse = self.apiClient.updateConfiguration(
            updateConfigurationCmd)
        self.debug(
            "updated the parameter %s with value %s" %
            (updateConfigurationResponse.name,
             updateConfigurationResponse.value))

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.email.attribute"
        updateConfigurationCmd.value = ldapConfiguration['emailAttribute']
        updateConfigurationResponse = self.apiClient.updateConfiguration(
            updateConfigurationCmd)
        self.debug(
            "updated the parameter %s with value %s" %
            (updateConfigurationResponse.name,
             updateConfigurationResponse.value))

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.user.object"
        updateConfigurationCmd.value = ldapConfiguration['userObject']
        updateConfigurationResponse = self.apiClient.updateConfiguration(
            updateConfigurationCmd)
        self.debug(
            "updated the parameter %s with value %s" %
            (updateConfigurationResponse.name,
             updateConfigurationResponse.value))

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.username.attribute"
        updateConfigurationCmd.value = ldapConfiguration['usernameAttribute']
        updateConfigurationResponse = self.apiClient.updateConfiguration(
            updateConfigurationCmd)
        self.debug(
            "updated the parameter %s with value %s" %
            (updateConfigurationResponse.name,
             updateConfigurationResponse.value))

        self.debug("start addLdapConfiguration test")

        ldapServer = addLdapConfiguration.addLdapConfigurationCmd()
        ldapServer.hostname = ldapConfiguration['hostname']
        ldapServer.port = ldapConfiguration['port']

        self.debug("calling addLdapConfiguration API command")
        try:
            self.apiClient.addLdapConfiguration(ldapServer)
            self.debug("addLdapConfiguration was successful")
            return 1
        except Exception as e:
            self.debug("addLdapConfiguration failed %s Check the Passed passed ldap attributes" % e)
            self.reason = "addLdapConfiguration failed %s Check the Passed passed ldap attributes" % e
            return 0

    def _checkLdapConfiguration(self, ldapConfiguration):

        """"
            This function checks the passed ldap server in the configuration is up and running or not.
             """""

        flag = False
        try:
            tn = telnetlib.Telnet(ldapConfiguration['hostname'], ldapConfiguration['port'], timeout=15)
            if tn is not None:
                tn.set_debuglevel(1)
                print(tn.msg("Connected to the server"))
                self.debug("Ldap Server is Up and listening on the port %s" % tn.msg("Connected to the server"))
                flag = True
                tn.close()
        except Exception as e:
            self.debug(" Not able to reach the LDAP server ,please check the Services on LDAP %s and  exception is %s"
                       % ((ldapConfiguration['hostname']), e))
            self.reason = "Not able to reach the LDAP server ,please check the Services on LDAP %s and exception is %s"\
                % ((ldapConfiguration['hostname']), e)
        return flag

    def _deleteldapconfiguration(self, ldapConfiguration):
        """

        :param ldapConfiguration

        """

        ldapServer = deleteLdapConfiguration.deleteLdapConfigurationCmd()
        ldapServer.hostname = ldapConfiguration["hostname"]

        try:
            self.apiClient.deleteLdapConfiguration(ldapServer)
            self.debug("deleteLdapConfiguration was successful")
            return 1
        except Exception as e:
            self.debug("deleteLdapConfiguration failed %s" % e)
            return 0

    def _checklogin(self, username, password):
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

        except Exception as p:
            self.debug("login operation failed %s" % p)
            self.reason = "Login operation Failed %s" % p

