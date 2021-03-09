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

from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.base import Account
from marvin.lib.utils import cleanup_resources
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

import inspect
import logging
import os
import re


class TestStaticRoles(cloudstackTestCase):
    """Tests static role api-checker
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__
        self.cleanup = []
        self.testdata = {
            "account": {
                "email": "mtu@test.cloud",
                "firstname": "Marvin",
                "lastname": "TestUser",
                "username": "staticrole_acctest-",
                "password": "password",
            }
        }

        feature_enabled = self.apiclient.listCapabilities(listCapabilities.listCapabilitiesCmd()).dynamicrolesenabled
        if feature_enabled:
            self.skipTest("Dynamic role-based API checker is enabled, skipping tests for static role-base API checker")

        commandsProperties = []
        try:
            sshClient = SshClient(
                self.mgtSvrDetails["mgtSvrIp"],
                22,
                self.mgtSvrDetails["user"],
                self.mgtSvrDetails["passwd"],
                retries=1,
                log_lvl=logging.INFO
            )
            result = sshClient.runCommand("cat /etc/cloudstack/management/commands.properties")
            if 'status' in result and result['status'] == 'SUCCESS' and 'stdout' in result and len(result['stdout']) > 0:
                commandsProperties = result['stdout']
        except Exception:
            self.debug("Failed to ssh into mgmt server host and grab commands.properties file")
            testDir = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
            localFileName = os.path.abspath(testDir + "/../../../client/conf/commands.properties.in")
            if os.path.isfile(localFileName):
                self.info("Detected that we're running in developer mode with maven, using file at:" + localFileName)
                with open(localFileName) as f:
                    commandsProperties = f.readlines()

        if len(commandsProperties) < 1:
            self.skipTest("Unable to find commands.properties, skipping this test")

        apiMap = {}
        for line in commandsProperties:
            if not line or line == '' or line == '\n' or line.startswith('#'):
                continue
            name, value = line.split('=')
            apiMap[name.strip()] = value.strip()

        self.roleApiMap = {} # role to list of apis allowed
        octetKey = {'Admin':1, 'DomainAdmin':4, 'User':8}
        for role in list(octetKey.keys()):
            for api in sorted(apiMap.keys()):
                if (octetKey[role] & int(apiMap[api])) > 0:
                    if role not in self.roleApiMap:
                        self.roleApiMap[role] = []
                    self.roleApiMap[role].append(api)


    def tearDown(self):
        try:
           cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


    def translateRoleToAccountType(self, role_type):
        if role_type == 'User':
            return 0
        elif role_type == 'Admin':
            return 1
        elif role_type == 'DomainAdmin':
            return 2
        return -1


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_static_role_account_acls(self):
        """
            Tests allowed APIs for common account types
        """
        for role in ['Admin', 'DomainAdmin', 'User']:
            accountType = self.translateRoleToAccountType(role)
            account = Account.create(
                self.apiclient,
                self.testdata['account'],
                admin=accountType
            )
            self.cleanup.append(account)
            userApiClient = self.testClient.getUserApiClient(UserName=account.name, DomainName=account.domain, type=accountType)
            allowedApis = [x.name for x in userApiClient.listApis(listApis.listApisCmd())]
            allApis = [x.name for x in self.apiclient.listApis(listApis.listApisCmd())]
            for api in self.roleApiMap[role]:
                if api not in allApis:
                    continue
                if api not in allowedApis:
                    self.fail("API configured in commands.properties not returned by listApis: " + api + " for role: " + role)
