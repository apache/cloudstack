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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *

import requests


class TestLogin(cloudstackTestCase):
    """
        Tests default login API handler
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.server_details = self.config.__dict__["mgtSvr"][0].__dict__
        self.server_url = "http://%s:8080/client/api" % self.server_details['mgtSvrIp']
        self.testdata = {
            "account": {
                "email": "login-user@test.cloud",
                "firstname": "TestLoginFirstName",
                "lastname": "TestLoginLastName",
                "username": "testloginuser-",
                "password": "password123",
            }
        }
        self.cleanup = []


    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def login(self, username, password, domain="/"):
        """
            Logs in and returns a session to be used for subsequent API calls
        """
        args = {}
        args["command"] = 'login'
        args["username"] = username
        args["password"] = password
        args["domain"] = domain
        args["response"] = "json"

        session = requests.Session()

        try:
            resp = session.post(self.server_url, params=args, verify=False)
        except requests.exceptions.ConnectionError as e:
            self.fail("Failed to attempt login request to mgmt server")
            return None, None

        return resp, session


    @attr(tags = ["devcloud", "advanced", "advancedns", "advancedsg", "smoke",
                  "basic", "sg"], required_hardware="false")
    def login_test_saml_user(self):
        """
            Tests that SAML users are not allowed CloudStack local log in

            Creates account across various account types and converts them to
            a SAML user and tests that they are not able to log in; then
            converts them back as a CloudStack user account and verifies that
            they are allowed to log in and make API requests
        """
        # Tests across various account types: 0=User, 1=Root Admin, 2=Domain Admin
        for account_type in range(0, 3):
            account = Account.create(
                self.apiclient,
                self.testdata['account'],
                admin=account_type
            )
            self.cleanup.append(account)

            username = account.user[0].username
            password = self.testdata['account']['password']

            # Convert newly created account user to SAML user
            user_id = self.dbclient.execute("select id from user where uuid='%s'" % account.user[0].id)[0][0]
            self.dbclient.execute("update user set source='SAML2' where id=%d" % user_id)

            response, session = self.login(username, password)
            self.assertEqual(
                response.json()['loginresponse']['errorcode'],
                531,
                "SAML user should not be allowed to log in, error code 531 not returned"
            )
            self.assertEqual(
                response.json()['loginresponse']['errortext'],
                "User is not allowed CloudStack login",
                "Invalid error message returned, SAML user should not be allowed to log in"
            )

            # Convert newly created account user back to normal source
            self.dbclient.execute("update user set source='UNKNOWN' where id=%d" % user_id)

            response, session = self.login(username, password)
            self.assertEqual(
                response.status_code,
                200,
                "Login response code was not 200"
            )
            self.assertTrue(
                len(response.json()['loginresponse']['sessionkey']) > 0,
                "Invalid session key received"
            )

            args = {}
            args["command"] = 'listUsers'
            args["listall"] = 'true'
            args["response"] = "json"
            response = session.get(self.server_url, params=args)
            self.assertEqual(
                response.status_code,
                200,
                "listUsers response code was not 200"
            )
            self.assertTrue(
                len(response.json()['listusersresponse']['user']) > 0,
                "listUsers list is empty or zero"
            )
