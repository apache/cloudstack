from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (
    updateConfiguration,
    createAccount,
    deleteAccount,
    addLdapConfiguration,
    linkDomainToLdap,
    deleteLdapConfiguration,
    createDomain,
    deleteDomain,
    disableAccount)
from marvin.cloudstackAPI import login
from nose.plugins.attrib import attr
import telnetlib
from ddt import ddt
import random
import string

class1 = 0


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
    updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
    updateConfigurationCmd.name = "ldap.basedn"
    updateConfigurationCmd.value = ldapConfiguration['basedn']
    updateConfigurationResponse = cls.apiClient.updateConfiguration(
        updateConfigurationCmd)
    cls.debug(
        "updated the parameter %s with value %s" %
        (updateConfigurationResponse.name,
         updateConfigurationResponse.value))

    updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
    updateConfigurationCmd.name = "ldap.bind.password"
    updateConfigurationCmd.value = ldapConfiguration['bindpassword']
    updateConfigurationResponse = cls.apiClient.updateConfiguration(
        updateConfigurationCmd)
    cls.debug(
        "updated the parameter %s with value %s" %
        (updateConfigurationResponse.name,
         updateConfigurationResponse.value))

    updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
    updateConfigurationCmd.name = "ldap.bind.principal"
    updateConfigurationCmd.value = ldapConfiguration['principal']
    updateConfigurationResponse = cls.apiClient.updateConfiguration(
        updateConfigurationCmd)
    cls.debug(
        "updated the parameter %s with value %s" %
        (updateConfigurationResponse.name,
         updateConfigurationResponse.value))

    updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
    updateConfigurationCmd.name = "ldap.email.attribute"
    updateConfigurationCmd.value = ldapConfiguration['emailAttribute']
    updateConfigurationResponse = cls.apiClient.updateConfiguration(
        updateConfigurationCmd)
    cls.debug(
        "updated the parameter %s with value %s" %
        (updateConfigurationResponse.name,
         updateConfigurationResponse.value))

    updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
    updateConfigurationCmd.name = "ldap.user.object"
    updateConfigurationCmd.value = ldapConfiguration['userObject']
    updateConfigurationResponse = cls.apiClient.updateConfiguration(
        updateConfigurationCmd)
    cls.debug(
        "updated the parameter %s with value %s" %
        (updateConfigurationResponse.name,
         updateConfigurationResponse.value))

    updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
    updateConfigurationCmd.name = "ldap.username.attribute"
    updateConfigurationCmd.value = ldapConfiguration['usernameAttribute']
    updateConfigurationResponse = cls.apiClient.updateConfiguration(
        updateConfigurationCmd)
    cls.debug(
        "updated the parameter %s with value %s" %
        (updateConfigurationResponse.name,
         updateConfigurationResponse.value))

    cls.debug("start addLdapConfiguration test")

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
        return 1


def checkLdapConfiguration(cls, ldapConfiguration):
    """"
                This function checks the passed ldap server in
                the configuration is up and running or not.
                 """""
    flag = False
    try:
        tn = telnetlib.Telnet(
            ldapConfiguration['hostname'],
            ldapConfiguration['port'],
            timeout=15)
        if tn is not None:
            tn.set_debuglevel(1)
            print tn.msg("Connected to the server")
            cls.debug(
                "Ldap Server is Up and listening on the port %s" %
                tn.msg("Connected to the server"))
            flag = True
            tn.close()
    except Exception as e:
        cls.debug(
            " Not able to reach the LDAP server ,"
            "please check the Services on LDAP %s and  exception is %s" %
            ((ldapConfiguration['hostname']), e))
        cls.reason = "Not able to reach the LDAP server ,please check" \
                     " the Services on LDAP %s and exception is %s" \
                     % ((ldapConfiguration['hostname']), e)
    return flag


@ddt
class TestLdap1(cloudstackTestCase):
    """
    15 test cases in this class.
    """
    @classmethod
    def setUpClass(cls):

        # configure ldap
        """

        :type cls: object
        """
        testClient = super(TestLdap1, cls).getClsTestClient()
        cls.api_client = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.cleanup = []
        cls.delflag = 0
        cls.reason = ""
        cls.apiClient = cls.testClient.getApiClient()
        cls.acct = createAccount.createAccountCmd()
        cls.acct.accounttype = 0
        cls.acct.firstname = cls.services[
            "configurableData"]["ldap_account"]["firstname"]
        cls.acct.lastname = cls.services[
            "configurableData"]["ldap_account"]["lastname"]
        cls.acct.password = cls.services[
            "configurableData"]["ldap_account"]["password"]
        cls.acct.username = cls.services[
            "configurableData"]["ldap_account"]["username"]
        cls.acct.email = cls.services[
            "configurableData"]["ldap_account"]["email"]
        cls.acct.account = cls.services[
            "configurableData"]["ldap_account"]["username"]
        cls.acct.domainid = 1
        if cls.acct.firstname == "" or cls.acct.lastname == "" \
            or cls.acct.password == "" or cls.acct.username == "" \
            or cls.services["configurableData"]["ldap_configuration"]["ldapUsername"] == "" \
                or cls.acct.account == "" or \
                cls.services["configurableData"]["ldap_configuration"]["ldapPassword"] == "":

            cls.debug(
                "Please rerun the test by providing values"
                " in ldap configuration user details")
            cls.skipTest(
                "Please rerun the test by providing proper"
                " values in configuration file")
        else:
            cls.delflag = 1
            cls.acctRes = cls.apiClient.createAccount(cls.acct)
        cls.cleanup.append(cls.acctRes)
        cls.ldapconfRes = addLdapConfiguration1(
            cls, cls.services["configurableData"]["ldap_configuration"])
        if cls.ldapconfRes == 1:
            cls.debug("Ldap Configuration was successful")
        else:
            cls.debug("LDAP Configuration failed with exception")
        if cls.ldapconfRes == 1:
            global class1
            class1 = 1
        cls.cleanup.append(cls.ldapconfRes)

    @classmethod
    def tearDownClass(cls):
        # delete ldap configuration
        """
        #cleanup includes : delete normal account, remove ldap configuration
        :type cls: object
        """
        testClient = super(TestLdap1, cls).getClsTestClient()
        cls.api_client = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        try:
            deleteAcct = deleteAccount.deleteAccountCmd()
            deleteAcct.id = cls.acctRes.id

            acct_name = cls.acctRes.name

            cls.apiClient.deleteAccount(deleteAcct)

            cls.debug(
                "Deleted the the following account name %s:" %
                acct_name)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
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
        # create a domain
        # link ldap to domain
        self.delflag = 0
        self.delflag2 = 0
        self.delflag3 = 0
        self.delflag4 = 0
        self.delflag5 = 0
        self.delflag6 = 0
        self.assertEquals(
            class1,
            1,
            self.reason)
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.domain = createDomain.createDomainCmd()
        self.domain.name = self.services["configurableData"][
            "link_ldap_details"]["domain_name"]

        if self.domain.name == "":
            self.debug(
                "Please rerun the test by providing"
                " values in ldap configuration user details")
            self.skipTest(
                "Please rerun the test by providing"
                " proper values in configuration file")
        else:
            self.delflag = 1
            self.domainRes = self.apiClient.createDomain(self.domain)
        self.assertEquals(
            self.delflag,
            1,
            "Domain creation failed,please check the configuration")
        # here link ldap to domain
        self.ldaplink = linkDomainToLdap.linkDomainToLdapCmd()
        self.ldaplink.domainid = self.domainRes.id
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

        if self.ldaplink.domainid == "" or self.ldaplink.accounttype == "" or self.ldaplink.name == "" \
                or self.ldaplink.type == "":
            self.debug(
                "Please rerun the test by providing "
                "values in link_ldap configuration user details")
            self.skipTest(
                "Please rerun the test by providing "
                "proper values in configuration file(link ldap)")
        else:
            self.delflag2 = 1
            self.ldaplinkRes = self.apiClient.linkDomainToLdap(self.ldaplink)
        self.assertEquals(
            self.delflag2,
            1,
            "Linking LDAP failed,please check the configuration")
        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, 1, self.reason)
        self.ldapacctIDtemp = self.dbclient.execute(
            "SELECT uuid FROM account WHERE account_name=" +
            "'" +
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"] +
            "'" +
            " AND removed IS NULL;",
            db="cloud")
        self.ldapacctID = self.ldapacctIDtemp[0][0]  # unicode
        self.cleanup.append(self.domainRes)

    def tearDown(self):
        # remove the link ldap to domain D1
        # cleanup includes : delete the ldap imported account(s), delete the
        # domain
        self.debug("In tear down%s" % self.delflag)
        # deleting ldap account here
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
            raise Exception("Warning: Exception during cleanup : %s" % e)
        # deleting domain here
        try:
            deleteDomain1 = deleteDomain.deleteDomainCmd()
            deleteDomain1.id = self.domainRes.id
            deleteDomain1.cleanup = 'true'

            domain_name = self.domainRes.name

            self.apiClient.deleteDomain(deleteDomain1)

            self.debug(
                "Deleted the the following account name %s:" %
                domain_name)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_ldap(self):
        # User is not in LDAP but locally present --> just test the login
        # functionality
        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, 1, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_02_ldap(self):
        # User is both in LDAP and imported into CS(i.e already logged in
        # once.
        # So just check the log in again)
        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, 1, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_03_ldap(self):
        # User in LDAP, wrong password --> login should fail
        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            randomword(8),
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, None, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_04_ldap(self):
        # User is only present locally, password is wrong --> login should
        #  fail
        loginRes = self._checklogin(
            self.services["configurableData"]["ldap_account"]["username"],
            randomword(10),
            "",
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, None, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_05_ldap(self):
        # user is not present anywhere --> login should fail
        loginRes = self._checklogin(randomword(10), randomword(10),
                                    self.services["configurableData"]["link_ldap_details"]["domain_name"],
                                    method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, None, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_06_ldap(self):
        # Delete the LDAP user from CS and try to login --> User should be
        # created again
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
                "Warning: Exception during deleting ldap imported account : %s" %
                e)

        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, 1, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_07_ldap(self):
        # Lock the user from CS and attempt to login --> login should fail
        self.lockAcct = disableAccount.disableAccountCmd()
        self.lockAcct.lock = 'true'
        self.lockAcct.account = self.services["configurableData"][
            "ldap_configuration"]["ldapUsername"]
        self.lockAcct.domainid = self.domainRes.id
        self.apiClient.disableAccount(self.lockAcct)

        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, None, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_09_ldap(self):
        # Create different domains and link all of them to LDAP. Check
        #  login in each domain --> login should be successful
        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, 1, self.reason)

        self.domain2 = createDomain.createDomainCmd()
        self.domain2.name = "Domain_2"
        if self.domain2.name == "":
            self.debug(
                "Please rerun the test by providing values"
                " in ldap configuration user details")
            self.skipTest(
                "Please rerun the test by providing"
                " proper values in configuration file")
        else:
            self.delflag3 = 1
            self.domainRes2 = self.apiClient.createDomain(self.domain2)
        self.assertEquals(
            self.delflag3,
            1,
            "Domain creation failed,please check the configuration")

        # here link ldap to domain
        self.ldaplink2 = linkDomainToLdap.linkDomainToLdapCmd()
        self.ldaplink2.domainid = self.domainRes2.id
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
                or self.ldaplink2.accounttype == ""\
                or self.ldaplink2.name == "" \
                or self.ldaplink2.type == "":
            self.debug(
                "Please rerun the test by providing"
                " values in link_ldap configuration user details")
            self.skipTest(
                "Please rerun the test by providing "
                "proper values in configuration file(link ldap)")

        else:
            self.delflag4 = 1
            self.ldaplinkRes2 = self.apiClient.linkDomainToLdap(self.ldaplink2)
        self.assertEquals(
            self.delflag4,
            1,
            "Linking LDAP failed,please check the configuration")

        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.domain2.name,
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, 1, self.reason)
        # Domain 2 ends here
        # Domain 3 starts here
        self.domain3 = createDomain.createDomainCmd()
        self.domain3.name = "Domain_3"

        if self.domain3.name == "":
            self.debug(
                "Please rerun the test by providing "
                "values in ldap configuration user details")
            self.skipTest(
                "Please rerun the test by providing"
                " proper values in configuration file")
        else:
            self.delflag5 = 1
            self.domainRes3 = self.apiClient.createDomain(self.domain3)
        self.assertEquals(
            self.delflag5,
            1,
            "Domain creation failed,please check the configuration")

        # here link ldap to domain
        self.ldaplink3 = linkDomainToLdap.linkDomainToLdapCmd()
        self.ldaplink3.domainid = self.domainRes3.id
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
            self.delflag6 = 1
            self.ldaplinkRes3 = self.apiClient.linkDomainToLdap(self.ldaplink3)
        self.assertEquals(
            self.delflag6,
            1,
            "Linking LDAP failed,please check the configuration")
        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.domain2.name,
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, 1, self.reason)
        # domain 3 ends here
        # deleting domain 2 and 3 here
        try:
            deleteDomain2 = deleteDomain.deleteDomainCmd()
            deleteDomain2.id = self.domainRes2.id
            deleteDomain2.cleanup = 'true'

            domain_name = self.domainRes2.name

            self.apiClient.deleteDomain(deleteDomain2)
            self.debug(
                "Deleted the the following account name %s:" %
                domain_name)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        try:
            deleteDomain3 = deleteDomain.deleteDomainCmd()
            deleteDomain3.id = self.domainRes3.id
            deleteDomain3.cleanup = 'true'

            domain_name = self.domainRes3.name

            self.apiClient.deleteDomain(deleteDomain3)
            self.debug(
                "Deleted the the following account name %s:" %
                domain_name)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_12_ldap(self):
            # Enable nested groups and try to login with a user that is in
            # nested group --> login should be successful
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.nested.groups.enable"
        updateConfigurationCmd.value = 'true'
        self.apiClient.updateConfiguration(updateConfigurationCmd)
        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapNestedUser"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapNestedPassword"],
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, 1, self.reason)

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_13_ldap(self):
        # Check db tables
        db_check = 1
        domainID = self.dbclient.execute(
            "SELECT id FROM domain WHERE uuid=" + "'" +
            self.domainRes.id + "'" + ";",
            db="cloud")
        dbChecking = self.dbclient.execute(
            "SELECT type,name,account_type FROM ldap_trust_map WHERE domain_id=" + "'" +
            str(domainID[0][0]) + "'" + ";",
            db="cloud")

        if dbChecking is not None and str(
            dbChecking[0][0]) == self.services["configurableData"]["link_ldap_details"]["type"] and str(
            dbChecking[0][1]) == self.services["configurableData"]["link_ldap_details"]["name"] and str(
                dbChecking[0][2]) == self.services["configurableData"]["link_ldap_details"]["accounttype"]:
            db_check = 0
        self.assertEquals(db_check, 0, "DB check failed")

    def test_14_ldap(self):
        # Password/domain empty --> login should fail
        loginRes = self._checklogin("", "", self.services["configurableData"][
                                    "link_ldap_details"]["domain_name"], method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, None, self.reason)

    def test_15_ldap(self):
        # Read-time out parameter value should be set to a low value --> login
        # should fail
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.read.timeout"
        updateConfigurationCmd.value = 0.01
        self.apiClient.updateConfiguration(updateConfigurationCmd)
        loginRes = self._checklogin(
            self.services["configurableData"]["link_ldap_details"]["linkLdapUsername"],
            self.services["configurableData"]["link_ldap_details"]["linkLdapPassword"],
            self.services["configurableData"]["link_ldap_details"]["domain_name"],
            method="POST")
        self.debug(loginRes)
        self.assertEquals(loginRes, None, self.reason)
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "ldap.read.timeout"
        updateConfigurationCmd.value = 1000
        self.apiClient.updateConfiguration(updateConfigurationCmd)

    def _checklogin(cls, username, password, domain, method):
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
