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

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (
    updateConfiguration,
    deleteAccount,
    addLdapConfiguration,
    linkDomainToLdap,
    deleteLdapConfiguration,
    disableAccount)
from marvin.lib.common import get_domain
from marvin.lib.base import (Account,
                             Configurations,
                             Domain)
from marvin.cloudstackAPI import login
from marvin.lib.utils import (cleanup_resources)
from nose.plugins.attrib import attr
import telnetlib
import random
import string


def randomword(length):
    return ''.join(random.choice(string.lowercase) for i in range(length))


def addLdapConfiguration1(cls, ldapConfiguration):
    """
            :param ldapConfiguration
            """
    cls.chkConfig = checkLdapConfiguration(cls, ldapConfiguration)
    if not cls.chkConfig:
        return 0

    # Setup Global settings
    Configurations.update(
        cls.apiClient,
        name="ldap.basedn",
        value=ldapConfiguration['basedn']
    )
    Configurations.update(
        cls.apiClient,
        name="ldap.bind.password",
        value=ldapConfiguration['bindpassword']
    )
    Configurations.update(
        cls.apiClient,
        name="ldap.bind.principal",
        value=ldapConfiguration['principal']
    )
    Configurations.update(
        cls.apiClient,
        name="ldap.email.attribute",
        value=ldapConfiguration['emailAttribute']
    )
    Configurations.update(
        cls.apiClient,
        name="ldap.user.object",
        value=ldapConfiguration['userObject']
    )
    Configurations.update(
        cls.apiClient,
        name="ldap.username.attribute",
        value=ldapConfiguration['usernameAttribute']
    )
    Configurations.update(
        cls.apiClient,
        name="ldap.nested.groups.enable",
        value="true"
    )

    ldapServer = addLdapConfiguration.addLdapConfigurationCmd()
    ldapServer.hostname = ldapConfiguration['hostname']
    ldapServer.port = ldapConfiguration['port']

    cls.debug("calling addLdapConfiguration API command")
    try:
        cls.apiClient.addLdapConfiguration(ldapServer)
        cls.debug("addLdapConfiguration was successful")
        return 1
    except Exception as e:
        cls.debug(
            "addLdapConfiguration failed %s Check the Passed passed"
            " ldap attributes" %
            e)
        cls.reason = "addLdapConfiguration failed %s Check the Passed " \
                     "passed ldap attributes" % e
        raise Exception(
            "addLdapConfiguration failed %s Check the Passed passed"
            " ldap attributes" %
            e)
        return 1


def checklogin(cls, username, password, domain, method):
    """
    :param username:
    :param password:
    """
    cls.debug("Attempting to login.")
    try:
        loginParams = login.loginCmd()
        loginParams.username = username
        loginParams.password = password
        loginParams.domain = domain
        loginRes = cls.apiClient.login(loginParams, method)
        cls.debug("login response %s" % loginRes)
        if loginRes is None:
            cls.debug("login not successful")
            return 0
        else:
            cls.debug("login successful")
            return 1
    except Exception as p:
        cls.debug("login operation failed %s" % p)
        cls.reason = "Login operation Failed %s" % p


def checkLdapConfiguration(cls, ldapConfiguration):
    """This function checks whether the passed ldap server in
        the configuration is up and running or not.
                 """
    flag = False
    try:
        tn = telnetlib.Telnet(
            ldapConfiguration['hostname'],
            ldapConfiguration['port'],
            timeout=15)
        if tn is not None:
            tn.set_debuglevel(1)
            print(tn.msg("Connected to the server"))
            cls.debug(
                "Ldap Server is Up and listening on the port %s" %
                tn.msg("Connected to the server"))
            flag = True
            tn.close()
    except Exception as e:
        cls.debug(
            "Not able to reach the LDAP server ,"
            "please check the Services on LDAP %s and  exception is %s" %
            ((ldapConfiguration['hostname']), e))
        cls.reason = "Not able to reach the LDAP server ,please check" \
                     " the Services on LDAP %s and exception is %s" \
                     % ((ldapConfiguration['hostname']), e)
    return flag


class TestLdap(cloudstackTestCase):
    """
    LDAP AutoImport smoke tests
    """
    @classmethod
    def setUpClass(cls):
        """
        :type cls: object
        """
        testClient = super(TestLdap, cls).getClsTestClient()
        cls.api_client = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.cleanup = []
        cls.domain = get_domain(cls.api_client)
        cls.delflag = 0
        cls.reason = ""

        cls.apiClient = cls.testClient.getApiClient()
        try:
            cls.ldapconfRes = addLdapConfiguration1(
                cls, cls.services["configurableData"]["ldap_configuration"])
        except Exception as e:
            raise Exception("Configuring LDAP failed. Check attributes")

        cls.cleanup.append(cls.ldapconfRes)

    @classmethod
    def tearDownClass(cls):
        """
        #cleanup includes : delete normal account, remove ldap configuration
        :type cls: object
        """
        testClient = super(TestLdap, cls).getClsTestClient()
        cls.api_client = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        if cls.ldapconfRes == 1:
            ldapserver = deleteLdapConfiguration.deleteLdapConfigurationCmd()
            ldapserver.hostname = cls.services["configurableData"][
                "ldap_configuration"]["hostname"]

            try:
                cls.apiClient.deleteLdapConfiguration(ldapserver)
                cls.debug("deleteLdapConfiguration was successful")
                return 1
            except Exception as e:
                cls.debug("deleteLdapConfiguration failed %s" % e)
                return 0

    def setUp(self):
        self.user = self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"]
        self.password = self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"]
        self.delflag1 = 0
        self.delflag2 = 0
        self.delflag3 = 0
        self.delflag4 = 0

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        self.parent_domain = Domain.create(
            self.apiclient,
            services=self.services["domain"],
            parentdomainid=self.domain.id)

        self.ldaplink = linkDomainToLdap.linkDomainToLdapCmd()
        self.ldaplink.domainid = self.parent_domain.id
        self.ldaplink.accounttype = self.services[
            "configurableData"]["link_ldap_details"]["accounttype"]
        self.ldaplink.name = self.services[
            "configurableData"]["link_ldap_details"]["name"]
        self.ldaplink.type = self.services[
            "configurableData"]["link_ldap_details"]["type"]
        if self.services["configurableData"][
            "link_ldap_details"]["admin"] is not None:
            self.ldaplink.admin = self.services[
                "configurableData"]["link_ldap_details"]["admin"]

        if self.ldaplink.domainid == "" or self.ldaplink.accounttype == "" \
                or self.ldaplink.name == "" \
                or self.ldaplink.type == "":
            self.debug(
                "Please rerun the test by providing "
                "values in link_ldap configuration user details")
            self.skipTest(
                "Please rerun the test by providing "
                "proper values in configuration file(link ldap)")
        else:
            self.delflag1 = 1
            self.ldaplinkRes = self.apiClient.linkDomainToLdap(self.ldaplink)
        self.assertEqual(
            self.delflag1,
            1,
            "Linking LDAP failed,please check the configuration")
        loginRes = checklogin(self,
                              self.user, self.password,
                              self.parent_domain.name,
                              method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, 1, self.reason)

        lsap_user = Account.list(self.api_client,
                                 domainid=self.parent_domain.id,
                                 name=self.user
                                 )
        self.ldapacctID = lsap_user[0].id

    def tearDown(self):

        try:
            self.parent_domain.delete(self.apiclient, cleanup=True)
        except Exception as e:
            raise Exception(
                "Warning: Exception during cleanup of domain : %s" % e)
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_ldap(self):
        """Check the linkDomainToLdap functionality"""
        self.domain1 = Domain.create(
            self.apiclient,
            services=self.services["domain"],
            parentdomainid=self.domain.id)

        self.ldaplink4 = linkDomainToLdap.linkDomainToLdapCmd()
        self.ldaplink4.domainid = self.domain1.id
        self.ldaplink4.accounttype = self.services[
            "configurableData"]["link_ldap_details"]["accounttype"]
        self.ldaplink4.name = self.services[
            "configurableData"]["link_ldap_details"]["name"]
        self.ldaplink4.type = self.services[
            "configurableData"]["link_ldap_details"]["type"]
        if self.services["configurableData"][
            "link_ldap_details"]["admin"] is not None:
            self.ldaplink4.admin = self.services[
                "configurableData"]["link_ldap_details"]["admin"]

        try:
            self.ldaplinkRes4 = self.apiClient.linkDomainToLdap(self.ldaplink4)

        except Exception as e:
            raise Exception(
                "Linking LDAP failed,please check the configuration")

        try:
            self.domain1.delete(self.apiclient)
        except Exception as e:
            raise Exception(
                "Warning: Exception during deletion of domain : %s" % e)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_02_ldap(self):
        """User is both in LDAP and imported into CS(i.e already logged in
           once.So just check the log in again)"""
        loginRes = checklogin(
            self,
            self.user,
            self.password,
            self.parent_domain.name,
            method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, 1, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_03_ldap(self):
        """User in LDAP, wrong password --> login should fail"""
        loginRes = checklogin(
            self,
            self.user,
            randomword(8),
            self.parent_domain.name,
            method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, None, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_04_ldap(self):
        """User is only present locally, password is wrong --> login should
          fail"""
        loginRes = checklogin(
            self,
            self.services["configurableData"]["ldap_account"]["username"],
            randomword(10),
            "",
            method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, None, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_05_ldap(self):
        """user is not present anywhere --> login should fail"""
        loginRes = checklogin(self, randomword(10), randomword(10),
                              self.parent_domain.name,
                              method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, None, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_06_ldap(self):
        """Delete the LDAP user from CS and try to login --> User should be
          created again"""
        try:

            deleteAcct2 = deleteAccount.deleteAccountCmd()
            deleteAcct2.id = self.ldapacctID

            acct_name = self.services["configurableData"][
                "link_ldap_details"]["linkLdapUsername"]

            self.apiClient.deleteAccount(deleteAcct2)

            self.debug(
                "Deleted the the following account name %s:" %
                acct_name)

        except Exception as e:
            raise Exception(
                "Warning: Exception during deleting "
                "ldap imported account : %s" %
                e)

        loginRes = checklogin(
            self,
            self.user,
            self.password,
            self.parent_domain.name,
            method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, 1, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_07_ldap(self):
        """Lock the user from CS and attempt to login --> login should fail"""
        self.lockAcct = disableAccount.disableAccountCmd()
        self.lockAcct.lock = 'true'
        self.lockAcct.account = self.services["configurableData"][
            "ldap_account"]["username"]
        self.lockAcct.domainid = self.parent_domain.id
        self.apiClient.disableAccount(self.lockAcct)

        loginRes = checklogin(
            self,
            self.user,
            self.password,
            self.parent_domain.name,
            method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, None, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_08_ldap(self):
        """Create different domains and link all of them to LDAP. Check
                        login in each domain --> login should be successful"""
        try:
            loginRes = checklogin(
                self,
                self.user,
                self.password,
                self.parent_domain.name,
                method="POST")
            self.debug(loginRes)
            self.assertEqual(loginRes, 1, self.reason)

            self.domain2 = Domain.create(
                self.apiclient,
                services=self.services["domain"],
                parentdomainid=self.domain.id)

            # here link ldap to domain
            self.ldaplink2 = linkDomainToLdap.linkDomainToLdapCmd()
            self.ldaplink2.domainid = self.domain2.id
            self.ldaplink2.accounttype = self.services[
                "configurableData"]["link_ldap_details"]["accounttype"]
            self.ldaplink2.name = self.services[
                "configurableData"]["link_ldap_details"]["name"]
            self.ldaplink2.type = self.services[
                "configurableData"]["link_ldap_details"]["type"]

            if self.services["configurableData"][
                "link_ldap_details"]["admin"] is not None:
                self.ldaplink2.admin = self.services[
                    "configurableData"]["link_ldap_details"]["admin"]

            if self.ldaplink2.domainid == "" \
                    or self.ldaplink2.accounttype == "" \
                    or self.ldaplink2.name == "" \
                    or self.ldaplink2.type == "":
                self.debug(
                    "Please rerun the test by providing"
                    " values in link_ldap configuration user details")
                self.skipTest(
                    "Please rerun the test by providing "
                    "proper values in configuration file(link ldap)")

            else:
                self.delflag2 = 1
                self.ldaplinkRes2 = self.apiClient.linkDomainToLdap(
                    self.ldaplink2)
            self.assertEqual(
                self.delflag2,
                1,
                "Linking LDAP failed,please check the configuration")

            loginRes = checklogin(
                self,
                self.user,
                self.password,
                self.domain2.name,
                method="POST")
            self.debug(loginRes)
            self.assertEqual(loginRes, 1, self.reason)

            self.domain3 = Domain.create(
                self.apiclient,
                services=self.services["domain"],
                parentdomainid=self.domain.id)
            # here link ldap to domain
            self.ldaplink3 = linkDomainToLdap.linkDomainToLdapCmd()
            self.ldaplink3.domainid = self.domain3.id
            self.ldaplink3.accounttype = self.services[
                "configurableData"]["link_ldap_details"]["accounttype"]
            self.ldaplink3.name = self.services[
                "configurableData"]["link_ldap_details"]["name"]
            self.ldaplink3.type = self.services[
                "configurableData"]["link_ldap_details"]["type"]
            if self.services["configurableData"][
                "link_ldap_details"]["admin"] is not None:
                self.ldaplink3.admin = self.services[
                    "configurableData"]["link_ldap_details"]["admin"]

            if self.ldaplink3.domainid == "" \
                    or self.ldaplink3.accounttype == "" \
                    or self.ldaplink3.name == "" \
                    or self.ldaplink3.type == "":
                self.debug(
                    "Please rerun the test by providing"
                    " values in link_ldap configuration user details")
                self.skipTest(
                    "Please rerun the test by providing "
                    "proper values in configuration file(link ldap)")
            else:
                self.delflag3 = 1
                self.ldaplinkRes3 = self.apiClient.linkDomainToLdap(
                    self.ldaplink3)
            self.assertEqual(
                self.delflag3,
                1,
                "Linking LDAP failed,please check the configuration")
            loginRes = checklogin(
                self,
                self.user,
                self.password,
                self.domain2.name,
                method="POST")
            self.debug(loginRes)
            self.assertEqual(loginRes, 1, self.reason)

        finally:
            try:
                self.domain2.delete(self.apiclient, cleanup=True)
            except Exception as e:
                raise Exception(
                    "Warning: Exception during deletion of domain : %s" % e)

            try:
                self.domain3.delete(self.apiclient, cleanup=True)
            except Exception as e:
                raise Exception(
                    "Warning: Exception during deletion of domain : %s" % e)

        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_09_ldap(self):
        """ Enable nested groups and try to login with a user that is in
             nested group --> login should be successful"""
        if self.services["configurableData"]["link_ldap_details"]["linkLdapNestedUser"] == "":
            self.skipTest("No nested user mentioned")
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.nested.groups.enable"
        updateConfigurationCmd.value = 'true'
        self.apiClient.updateConfiguration(updateConfigurationCmd)
        loginRes = checklogin(
            self,
            self.services["configurableData"]["link_ldap_details"]["linkLdapNestedUser"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapNestedPassword"],
            self.parent_domain.name,
            method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, 1, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_10_ldap(self):
        """Check db tables"""
        db_check = 1

        domainID = self.dbclient.execute(
            "SELECT id FROM domain WHERE uuid=" + "'" +
            self.parent_domain.id + "'" + ";",
            db="cloud")

        dbChecking = self.dbclient.execute(
            "SELECT type,name,account_type "
            "FROM ldap_trust_map WHERE domain_id=" + "'" +
            str(domainID[0][0]) + "'" + ";",
            db="cloud")

        if dbChecking is not None and str(
                dbChecking[0][0]) == \
                self.services["configurableData"]["link_ldap_details"]["type"] \
                and str(
                    dbChecking[0][1]) == \
                        self.services["configurableData"]["link_ldap_details"]["name"] \
                and str(
                    dbChecking[0][2]) == \
                        self.services["configurableData"]["link_ldap_details"]["accounttype"]:
            db_check = 0
        self.assertEqual(db_check, 0, "DB check failed")

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_11_ldap(self):
        """Password/domain empty --> login should fail"""
        loginRes = checklogin(
            self,
            "", "", self.parent_domain.name, method="POST")
        self.debug(loginRes)
        self.assertEqual(loginRes, None, self.reason)


