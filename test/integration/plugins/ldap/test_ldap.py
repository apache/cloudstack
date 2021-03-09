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
from .ldap_test_data import LdapTestData
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (listLdapUsers,
                             ldapCreateAccount,
                             importLdapUsers,
                             User,
                             Domain,
                             Account,
                             addLdapConfiguration,
                             deleteLdapConfiguration,
                             linkAccountToLdap,
                             linkDomainToLdap,
                             updateConfiguration)
from marvin.lib.common import (get_domain,
                               get_zone)
from nose.plugins.attrib import attr

# for login validation
import requests

import logging

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
        cls.logger = logging.getLogger(__name__)
        stream_handler = logging.StreamHandler()
        logger_formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
        stream_handler.setFormatter(logger_formatter)
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(stream_handler)

        cls.logger.info("Setting up Class")
        testClient = super(TestLDAP, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()

        try:
            # Setup test data
            cls.testdata = LdapTestData()
            if cls.config.TestData and cls.config.TestData.Path:
                cls.logger.debug("reading extra config from '" + cls.config.TestData.Path + "'")
                cls.testdata.update(cls.config.TestData.Path)
            cls.logger.debug(cls.testdata)

            cls.services = testClient.getParsedTestDataConfig()
            cls.services["configurableData"]["ldap_configuration"] = cls.testdata.testdata["ldap_configuration"]
            cls.logger.debug(cls.services["configurableData"]["ldap_configuration"])

            # Get Zone, Domain
            cls.domain = get_domain(cls.apiclient)
            cls.logger.debug("standard domain: %s" % cls.domain.id)
            cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

            cls._cleanup = []

            # Build the test env
            cls.create_domains(cls.testdata)
            cls.configure_ldap_for_domains(cls.testdata)

            cls.test_user = [
                cls.testdata.testdata[LdapTestData.users][0][LdapTestData.uid],
                cls.testdata.testdata[LdapTestData.users][1][LdapTestData.uid],
                cls.testdata.testdata[LdapTestData.users][2][LdapTestData.uid]
            ]
        except Exception as e:
            cls.logger.debug("Exception in setUpClass(cls): %s" % e)
            cls.tearDownClass()
            raise Exception("setup failed due to %s", e)

        return

    @classmethod
    def tearDownClass(cls):
        cls.logger.info("Tearing Down Class")
        try:
            cleanup_resources(cls.apiclient, reversed(cls._cleanup))
            cls.remove_ldap_configuration_for_domains()
            cls.logger.debug("done cleaning up resources in tearDownClass(cls) %s")
        except Exception as e:
            cls.logger.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

        self.server_details = self.config.__dict__["mgtSvr"][0].__dict__
        self.server_url = "http://%s:8080/client/api" % self.server_details['mgtSvrIp']

        return

    def tearDown(self):
        try:
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
        cmd = listLdapUsers.listLdapUsersCmd()
        cmd.domainid = self.manualDomain.id
        cmd.userfilter = "LocalDomain"
        response = self.apiclient.listLdapUsers(cmd)
        self.logger.info("users found for linked domain %s" % response)
        self.assertEqual(len(response), len(self.testdata.testdata[LdapTestData.users]), "unexpected number (%d) of ldap users" % len(self.testdata.testdata[LdapTestData.users]))

        cmd = ldapCreateAccount.ldapCreateAccountCmd()
        cmd.domainid = self.manualDomain.id
        cmd.accounttype = 0
        cmd.username = self.test_user[1]
        create_response = self.apiclient.ldapCreateAccount(cmd)

        # cleanup
        # last results id should be the account
        list_response = Account.list(self.apiclient, id=create_response.id)
        account_created = Account(list_response[0].__dict__)
        self.cleanup.append(account_created)

        self.assertEqual(len(create_response.user), 1, "only one user %s should be present" % self.test_user[1])

        self.assertEqual(len(list_response),
                         1,
                         "only one account (for user %s) should be present" % self.test_user[1])

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

        cmd = importLdapUsers.importLdapUsersCmd()
        cmd.domainid = domainid
        cmd.accounttype = 0
        import_response = self.apiclient.importLdapUsers(cmd)

        # this is needed purely for cleanup:
        # cleanup
        list_response = Account.list(self.apiclient, domainid=domainid)
        for account in list_response:
            account_created = Account(account.__dict__)
            self.logger.debug("account to clean: %s (id: %s)" % (account_created.name, account_created.id))
            self.cleanup.append(account_created)

        self.assertEqual(len(import_response), len(self.testdata.testdata[LdapTestData.users]), "unexpected number of ldap users")

        self.assertEqual(len(list_response), len(self.testdata.testdata[LdapTestData.users]), "only one account (for user %s) should be present" % self.test_user[1])

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
        username = self.test_user[1]

        # validate the user doesn't exist
        response = User.list(self.apiclient,domainid=domainid,username=username)
        self.assertEqual(response, None, "user should not exist yet")

        self.logon_test_user(username)

        # now validate the user exists in domain
        response = User.list(self.apiclient,domainid=domainid,username=username)
        for user in response:
            user_created = User(user.__dict__)
            self.logger.debug("user to clean: %s (id: %s)" % (user_created.username, user_created.id))
            self.cleanup.append(user_created)

        # now verify the creation of the user
        self.assertEqual(len(response), 1, "user should exist by now")

        return

    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_04_filtered_list_of_users(self):
        '''
        test if we can get a filtered list of ldap users

        prerequisite
        a ldap host is configured
        a couple of ldapdomains are linked to cloudstack domains
        some accounts in those domain are linked to groups in ldap
        some ldap accounts are linked and present with the same uid
        some ldap accounts are not yet linked but present at other locations in cloudstack

        NOTE 1: if this test is run last only the explicitely imported test user from test_03_sync
         is in the system. The accounts from test_01_manual and test_02_import should have been cleared
         by the test tearDown(). We can not depend on test_03_sync having run so the test must avoid
         depending on it either being available or not.

        NOTE 2: this test will not work if the ldap users UIDs are already present in the ACS instance
         against which is being tested
        '''
        cmd = listLdapUsers.listLdapUsersCmd()
        cmd.userfilter = "NoFilter"
        cmd.domainid = self.manualDomain.id
        response = self.apiclient.listLdapUsers(cmd)
        self.logger.debug(cmd.userfilter + " : " + str(response))
        self.assertEqual(len(response), len(self.testdata.testdata[LdapTestData.users]), "unexpected number of ldap users")

        # create a non ldap user with the uid of cls.test_user[0] in parentDomain
        # create a manual import of a cls.test_user[1] in manualDomain
        # log on with test_user[2] in an syncDomain

        # we can now test all four filtertypes in syncDomain and inspect the respective outcomes for validity

        self.logon_test_user(self.test_user[2])

        cmd.userfilter = "LocalDomain"
        cmd.domainid = self.syncDomain.id
        response = self.apiclient.listLdapUsers(cmd)
        self.logger.debug(cmd.userfilter + " : " + str(response))
        self.assertEqual(len(response),
                         len(self.testdata.testdata[LdapTestData.users]) - 1,
                         "unexpected number of ldap users")

    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_05_relink_account_and_reuse_user(self):
        '''
        test if an account and thus a user can be removed and re-added

        test if components still are synced

        prerequisite
        a ldap host is configured
        a domain is linked to cloudstack
        some accounts in that domain are linked to groups in ldap
        '''
        domainid = self.syncDomain.id
        username = self.test_user[1]

        # validate the user doesn't exist
        response = User.list(self.apiclient,domainid=domainid,username=username)
        self.assertEqual(response, None, "user should not exist yet")

        self.logon_test_user(username)

        # now validate the user exists in domain
        response = User.list(self.apiclient,domainid=domainid,username=username)
        # for user in response:
        #     user_created = User(user.__dict__)
        #     self.debug("user to clean: %s (id: %s)" % (user_created.username, user_created.id))
        #     # we don't cleanup to test if re-adding fails
        #     self.cleanup.append(user_created)

        # now verify the creation of the user
        self.assertEqual(len(response), 1, "user should exist by now")

        # delete the account - quick implementation: user[1] happens to be a junior
        self.junior_account.delete(self.apiclient)

        # add the account with the same ldap group
        self.bind_account_to_ldap(
            account=self.testdata.testdata[LdapTestData.syncAccounts][0]["name"],
            ldapdomain=self.testdata.testdata[LdapTestData.syncAccounts][0]["group"],
            accounttype=self.testdata.testdata[LdapTestData.syncAccounts][0]["accounttype"])

        # logon the user - should succeed - reported to fail
        self.logon_test_user(username)

        # now verify the creation of the user
        response = User.list(self.apiclient,domainid=domainid,username=username)
        for user in response:
            user_created = User(user.__dict__)
            self.debug("user to clean: %s (id: %s)" % (user_created.username, user_created.id))
            # we don't cleanup to test if re-adding fails
            # self.cleanup.append(user_created)
        self.assertEqual(len(response), 1, "user should exist again")
        return


    def logon_test_user(self, username, domain = None):
        # login of dahn should create a user in account juniors
        args = {}
        args["command"] = 'login'
        args["username"] = username
        args["password"] = 'password'
        if domain == None:
            args["domain"] = "/" + self.parentDomain.name + "/" + self.syncDomain.name
        else:
            args["domain"] = domain
        args["response"] = "json"
        session = requests.Session()
        try:
            resp = session.post(self.server_url, params=args, verify=False)
        except requests.exceptions.ConnectionError as e:
            self.fail("Failed to attempt login request to mgmt server")


    @classmethod
    def create_domains(cls, td):
        # create a parent domain
        cls.parentDomain = cls.create_domain(td.testdata["domains"][0], parent_domain=cls.domain.id)
        cls.manualDomain = cls.create_domain(td.testdata["domains"][1], parent_domain=cls.parentDomain.id)
        cls.importDomain = cls.create_domain(td.testdata["domains"][2], parent_domain=cls.parentDomain.id)
        cls.syncDomain = cls.create_domain(td.testdata["domains"][3], parent_domain=cls.parentDomain.id)

    @classmethod
    def create_domain(cls, domain_to_create, parent_domain = None):
        cls.logger.debug("Creating domain: %s under %s" % (domain_to_create[LdapTestData.name], parent_domain))
        if parent_domain:
            domain_to_create["parentdomainid"] = parent_domain
        tmpDomain = Domain.create(cls.apiclient, domain_to_create)
        cls.logger.debug("Created domain %s with id %s " % (tmpDomain.name, tmpDomain.id))
        cls._cleanup.append(tmpDomain)
        return tmpDomain

    @classmethod
    def configure_ldap_for_domains(cls, td) :
        cmd = addLdapConfiguration.addLdapConfigurationCmd()
        cmd.hostname = td.testdata[LdapTestData.configuration][LdapTestData.hostname]
        cmd.port = td.testdata[LdapTestData.configuration][LdapTestData.port]

        cls.logger.debug("configuring ldap server for domain %s" % LdapTestData.manualDomain)
        cmd.domainid = cls.manualDomain.id
        response = cls.apiclient.addLdapConfiguration(cmd)
        cls.manualLdap = response

        cls.logger.debug("configuring ldap server for domain %s" % LdapTestData.importDomain)
        cmd.domainid = cls.importDomain.id
        response = cls.apiclient.addLdapConfiguration(cmd)
        cls.importLdap = response

        cls.logger.debug("configuring ldap server for domain %s" % LdapTestData.syncDomain)
        cmd.domainid = cls.syncDomain.id
        response = cls.apiclient.addLdapConfiguration(cmd)
        cls.syncLdap = response

        cls.set_ldap_settings_on_domain(domainid=cls.manualDomain.id)
        cls.bind_domain_to_ldap(domainid=cls.manualDomain.id, ldapdomain=cls.testdata.admins)

        cls.set_ldap_settings_on_domain(domainid=cls.importDomain.id)
        cls.bind_domain_to_ldap(domainid=cls.importDomain.id, ldapdomain=cls.testdata.admins,
                                accounttype=2, type="Group") # just to be testing different types

        cls.set_ldap_settings_on_domain(domainid=cls.syncDomain.id)
        cls.create_sync_accounts()

    @classmethod
    def remove_ldap_configuration_for_domains(cls) :
        cls.logger.debug("deleting configurations for ldap server")
        cmd = deleteLdapConfiguration.deleteLdapConfigurationCmd()

        cmd.hostname = cls.manualLdap.hostname
        cmd.port = cls.manualLdap.port
        cmd.domainid = cls.manualLdap.domainid
        response = cls.apiclient.deleteLdapConfiguration(cmd)
        cls.logger.debug("configuration deleted for %s" % response)

        cmd.hostname = cls.importLdap.hostname
        cmd.port = cls.importLdap.port
        cmd.domainid = cls.importLdap.domainid
        response = cls.apiclient.deleteLdapConfiguration(cmd)
        cls.logger.debug("configuration deleted for %s" % response)

        cmd.hostname = cls.syncLdap.hostname
        cmd.port = cls.syncLdap.port
        cmd.domainid = cls.syncLdap.domainid
        cls.logger.debug("deleting configuration %s" % cmd)
        response = cls.apiclient.deleteLdapConfiguration(cmd)
        cls.logger.debug("configuration deleted for %s" % response)


    @classmethod
    def create_sync_accounts(cls):
        cls.logger.debug("creating account: %s" % LdapTestData.seniors)
        cls.senior_account = cls.bind_account_to_ldap(
            account=cls.testdata.testdata[LdapTestData.syncAccounts][1]["name"],
            ldapdomain=cls.testdata.testdata[LdapTestData.syncAccounts][1]["group"],
            accounttype=cls.testdata.testdata[LdapTestData.syncAccounts][1]["accounttype"])
        cls.junior_account = cls.bind_account_to_ldap(
            account=cls.testdata.testdata[LdapTestData.syncAccounts][0]["name"],
            ldapdomain=cls.testdata.testdata[LdapTestData.syncAccounts][0]["group"],
            accounttype=cls.testdata.testdata[LdapTestData.syncAccounts][0]["accounttype"])

    @classmethod
    def bind_account_to_ldap(cls, account, ldapdomain, type="Group", accounttype=0):
        cmd = linkAccountToLdap.linkAccountToLdapCmd()

        cmd.domainid = cls.syncDomain.id
        cmd.account = account
        cmd.ldapdomain = ldapdomain
        cmd.type = type
        cmd.accounttype = accounttype

        response = cls.apiclient.linkAccountToLdap(cmd)
        cls.logger.info("account linked to ladp %s" % response)

        # this is needed purely for cleanup:
        response = Account.list(cls.apiclient, id=response.accountid)
        account_created = Account(response[0].__dict__)
        cls._cleanup.append(account_created)
        return account_created

    @classmethod
    def bind_domain_to_ldap(cls, domainid, ldapdomain, type="OU", accounttype=0):
        cmd = linkDomainToLdap.linkDomainToLdapCmd()
        cmd.domainid = domainid
        cmd.type = type
        cmd.accounttype = accounttype
        cmd.ldapdomain = ldapdomain
        response = cls.apiclient.linkDomainToLdap(cmd)
        cls.logger.info("domain linked to ladp %s" % response)

    @classmethod
    def set_ldap_settings_on_domain(cls, domainid):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.domainid = domainid
        cmd.name = LdapTestData.basednConfig
        cmd.value = cls.testdata.testdata[LdapTestData.configuration][LdapTestData.basedn]
        response = cls.apiclient.updateConfiguration(cmd)
        cls.logger.debug("set the basedn: %s" % response)
        cmd.name = LdapTestData.ldapPwConfig
        cmd.value = cls.testdata.testdata[LdapTestData.configuration][LdapTestData.ldapPw]
        response = cls.apiclient.updateConfiguration(cmd)
        cls.logger.debug("set the pw: %s" % response)
        cmd.name = LdapTestData.principalConfig
        cmd.value = cls.testdata.testdata[LdapTestData.configuration][LdapTestData.principal]
        response = cls.apiclient.updateConfiguration(cmd)
        cls.logger.debug("set the id: %s" % response)
        if LdapTestData.groupPrinciple in cls.testdata.testdata[LdapTestData.configuration] :
            cmd.name = LdapTestData.groupPrinciple
            cmd.value = cls.testdata.testdata[LdapTestData.configuration][LdapTestData.groupPrinciple]
            response = cls.apiclient.updateConfiguration(cmd)
            cls.logger.debug("set the id: %s" % response)


## python ldap utility functions
