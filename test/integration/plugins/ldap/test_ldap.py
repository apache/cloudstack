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

#Import Local Modules
from integration.plugins.ldap.ldap_test_data import LdapTestData
from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

# for login validation
import requests

#Import System modules
import logging

logger = logging.getLogger(__name__)
logger_handler = logging.FileHandler('/tmp/MarvinLogs/{}.log'.format(__name__))
logger_formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
logger_handler.setFormatter(logger_formatter)
logger.addHandler(logger_handler)
logger.setLevel(logging.DEBUG)


class TestLDAP(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        '''
            needs to
             - create the applicable ldap accounts in the directory server
             - create three domains:
             -- LDAP/manual
             -- LDAP/import
             -- LDAP/sync
        '''
        logger.info("Setting up Class")
        testClient = super(TestLDAP, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()

        try:
            # Setup test data
            cls.testdata = LdapTestData()
            if cls.config.TestData and cls.config.TestData.Path:
                logger.debug("reading extra config from '" + cls.config.TestData.Path + "'")
                cls.testdata.update(cls.config.TestData.Path)
            logger.debug(cls.testdata)

            cls.services = testClient.getParsedTestDataConfig()
            cls.services["configurableData"]["ldap_configuration"] = cls.testdata.testdata["ldap_configuration"]
            logger.debug(cls.services["configurableData"]["ldap_configuration"])

            # Get Zone, Domain
            cls.domain = get_domain(cls.apiclient)
            logger.debug("standard domain: %s" % cls.domain.id)
            cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

            cls._cleanup = []

            # Build the test env
            cls.create_domains(cls.testdata)
            cls.configure_ldap_for_domains(cls.testdata)
        except Exception as e:
            logger.debug("Exception in setUpClass(cls): %s" % e)
            cls.tearDownClass()
            raise Exception("setup failed due to %s", e)

        return

    @classmethod
    def configure_ldap_for_domains(cls, td) :
        cmd = addLdapConfiguration.addLdapConfigurationCmd()
        cmd.hostname = td.testdata[LdapTestData.configuration][LdapTestData.hostname]
        cmd.port = td.testdata[LdapTestData.configuration][LdapTestData.port]

        logger.debug("configuring ldap server for domain %s" % LdapTestData.manualDomain)
        cmd.domainid = cls.manualDomain.id
        response = cls.apiclient.addLdapConfiguration(cmd)
        cls.globalLdap = response

        logger.debug("configuring ldap server for domain %s" % LdapTestData.importDomain)
        cmd.domainid = cls.importDomain.id
        response = cls.apiclient.addLdapConfiguration(cmd)
        cls.importLdap = response

        logger.debug("configuring ldap server for domain %s" % LdapTestData.syncDomain)
        cmd.domainid = cls.syncDomain.id
        response = cls.apiclient.addLdapConfiguration(cmd)
        cls.syncLdap = response

    @classmethod
    def remove_ldap_configuration_for_domains(cls) :
        logger.debug("deleting configurations for ldap server")
        cmd = deleteLdapConfiguration.deleteLdapConfigurationCmd()

        cmd.hostname = cls.globalLdap.hostname
        cmd.port = cls.globalLdap.port
        cmd.domainid = cls.globalLdap.domainid
        response = cls.apiclient.deleteLdapConfiguration(cmd)
        logger.debug("configuration deleted for %s" % response)

        cmd.hostname = cls.importLdap.hostname
        cmd.port = cls.importLdap.port
        cmd.domainid = cls.importLdap.domainid
        response = cls.apiclient.deleteLdapConfiguration(cmd)
        logger.debug("configuration deleted for %s" % response)

        cmd.hostname = cls.syncLdap.hostname
        cmd.port = cls.syncLdap.port
        cmd.domainid = cls.syncLdap.domainid
        logger.debug("deleting configuration %s" % cmd)
        response = cls.apiclient.deleteLdapConfiguration(cmd)
        logger.debug("configuration deleted for %s" % response)


    @classmethod
    def create_domains(cls, td):
        # create a parent domain
        cls.parentDomain = cls.create_domain(td.testdata["domains"][0], parent_domain=cls.domain.id)
        cls.manualDomain = cls.create_domain(td.testdata["domains"][1], parent_domain=cls.parentDomain.id)
        cls.importDomain = cls.create_domain(td.testdata["domains"][2], parent_domain=cls.parentDomain.id)
        cls.syncDomain = cls.create_domain(td.testdata["domains"][3], parent_domain=cls.parentDomain.id)

    @classmethod
    def create_domain(cls, domain_to_create, parent_domain = None):
        logger.debug("Creating domain: %s under %s" % (domain_to_create[LdapTestData.name], parent_domain))
        if parent_domain:
            domain_to_create["parentdomainid"] = parent_domain
        tmpDomain = Domain.create(cls.apiclient, domain_to_create)
        logger.debug("Created domain %s with id %s " % (tmpDomain.name, tmpDomain.id))
        cls._cleanup.append(tmpDomain)
        return tmpDomain

    @classmethod
    def tearDownClass(cls):
        logger.info("Tearing Down Class")
        try:
            cleanup_resources(cls.apiclient, reversed(cls._cleanup))
            cls.remove_ldap_configuration_for_domains()
            logger.debug("done cleaning up resources in tearDownClass(cls) %s")
        except Exception as e:
            logger.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

        self.server_details = self.config.__dict__["mgtSvr"][0].__dict__
        self.server_url = "http://%s:8080/client/api" % self.server_details['mgtSvrIp']

        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_01_manual(self):
        '''
        test if an account can be imported

        prerequisite
        a ldap host is configured
        a domain is linked to cloudstack
        '''
        domainid = self.manualDomain.id
        type = "OU"
        ldapdomain = self.testdata.admins

        self.set_ldap_settings_on_domain(domainid)

        self.bind_domain_to_ldap(domainid, ldapdomain)

        cmd = listLdapUsers.listLdapUsersCmd()
        cmd.domainid = self.manualDomain.id
        response = self.apiclient.listLdapUsers(cmd)
        logger.info("users found for linked domain %s" % response)
        self.assertEqual(len(response), len(self.testdata.testdata[LdapTestData.users]), "unexpected number of ldap users")

        cmd = ldapCreateAccount.ldapCreateAccountCmd()
        cmd.domainid = self.manualDomain.id
        cmd.accounttype = 0
        cmd.username = 'dahn'
        response = self.apiclient.ldapCreateAccount(cmd)
        self.assertEqual(len(response.user), 1, "only one user 'dahn' should be present")

        # cleanup
        # last results id should be the account
        response = Account.list(self.apiclient, id=response.id)
        self.assertEqual(len(response),
                         1,
                         "only one account (for user 'dahn') should be present")
        # this is needed purely for cleanup:
        account_created = Account(response[0].__dict__)
        self._cleanup.append(account_created)

        return

    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_02_import(self):
        '''
        test if components are synced

        prerequisite
        a ldap host is configured
        a domain is linked to cloudstack
        '''
        domainid = self.importDomain.id
        type = "Group"
        accounttype = 2
        ldapdomain = self.testdata.admins

        self.set_ldap_settings_on_domain(domainid)

        self.bind_domain_to_ldap(domainid, ldapdomain, accounttype=accounttype, type=type)

        cmd = importLdapUsers.importLdapUsersCmd()
        cmd.domainid = domainid
        cmd.accounttype = 0
        response = self.apiclient.importLdapUsers(cmd)
        self.assertEqual(len(response), len(self.testdata.testdata[LdapTestData.users]), "unexpected number of ldap users")


        # cleanup
        response = Account.list(self.apiclient, domainid=domainid)
        self.assertEqual(len(response), len(self.testdata.testdata[LdapTestData.users]), "only one account (for user 'dahn') should be present")
        # this is needed purely for cleanup:
        for account in response:
            account_created = Account(account.__dict__)
            self._cleanup.append(account_created)

        return

    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_03_sync(self):
        '''
        test if components are synced

        prerequisite
        a ldap host is configured
        a domain is linked to cloudstack
        some accounts in that domain are linked to groups in ldap
        '''
        domainid = self.syncDomain.id
        username = 'dahn'

        self.set_ldap_settings_on_domain(domainid)

        self.create_sync_accounts()

        # validate the user doesn't exist
        response = User.list(self.apiclient,domainid=domainid,username=username)
        self.assertEqual(response, None, "user should not exist yet")

        # login of dahn should create a user in account juniors
        args = {}
        args["command"] = 'login'
        args["username"] = username
        args["password"] = 'password'
        args["domain"] = "/"+self.parentDomain.name+"/"+self.syncDomain.name
        args["response"] = "json"

        session = requests.Session()

        try:
            resp = session.post(self.server_url, params=args, verify=False)
        except requests.exceptions.ConnectionError, e:
            self.fail("Failed to attempt login request to mgmt server")

        # now validate the user exists in domain
        response = User.list(self.apiclient,domainid=domainid,username=username)
        self.assertEqual(len(response), 1, "user should exist by now")

        return

    def create_sync_accounts(self):
        logger.debug("creating account: %s" % LdapTestData.seniors)
        self.bind_account_to_ldap(
            account=self.testdata.testdata[LdapTestData.syncAccounts][1]["name"],
            ldapdomain=self.testdata.testdata[LdapTestData.syncAccounts][1]["group"],
            accounttype=self.testdata.testdata[LdapTestData.syncAccounts][1]["accounttype"])
        self.bind_account_to_ldap(
            account=self.testdata.testdata[LdapTestData.syncAccounts][0]["name"],
            ldapdomain=self.testdata.testdata[LdapTestData.syncAccounts][0]["group"],
            accounttype=self.testdata.testdata[LdapTestData.syncAccounts][0]["accounttype"])

    #  make this an init method that add to the _cleanup list
    def bind_account_to_ldap(self, account, ldapdomain, type="Group", accounttype=0):
        cmd = linkAccountToLdap.linkAccountToLdapCmd()

        cmd.domainid = self.syncDomain.id
        cmd.account = account
        cmd.ldapdomain = ldapdomain
        cmd.type = type
        cmd.accounttype = accounttype

        response = self.apiclient.linkAccountToLdap(cmd)
        logger.info("account linked to ladp %s" % response)

        # this is needed purely for cleanup:
        response = Account.list(self.apiclient, id=response.accountid)
        account_created = Account(response[0].__dict__)
        self._cleanup.append(account_created)

    def bind_domain_to_ldap(self, domainid, ldapdomain, type="OU", accounttype=0):
        cmd = linkDomainToLdap.linkDomainToLdapCmd()
        cmd.domainid = domainid
        cmd.type = type
        cmd.accounttype = accounttype
        cmd.ldapdomain = ldapdomain
        response = self.apiclient.linkDomainToLdap(cmd)
        logger.info("domain linked to ladp %s" % response)

    def set_ldap_settings_on_domain(self, domainid):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.domainid = domainid
        cmd.name = LdapTestData.basednConfig
        cmd.value = self.testdata.testdata[LdapTestData.configuration][LdapTestData.basedn]
        response = self.apiclient.updateConfiguration(cmd)
        logger.debug("set the basedn: %s" % response)
        cmd.name = LdapTestData.ldapPwConfig
        cmd.value = self.testdata.testdata[LdapTestData.configuration][LdapTestData.ldapPw]
        response = self.apiclient.updateConfiguration(cmd)
        logger.debug("set the pw: %s" % response)
        cmd.name = LdapTestData.principalConfig
        cmd.value = self.testdata.testdata[LdapTestData.configuration][LdapTestData.principal]
        response = self.apiclient.updateConfiguration(cmd)
        logger.debug("set the id: %s" % response)

