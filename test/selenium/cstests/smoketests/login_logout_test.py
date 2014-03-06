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

import unittest
import sys, os, time
import json

sys.path.append('./')

import browser.firefox as firefox
import cspages.login.loginpage as loginpage
import common.shared as shared
from cstests.smoketests.smokecfg import smokecfg

# from cstests.smoketests import smokecfg as smokecfg

class TestCSLoginLogout(unittest.TestCase):
    def setUp(self):
        # Create a new instance of the Firefox browser
        self.browser = firefox.Firefox('firefox')

    def tearDown(self):
        self.browser.quit_browser()

    def test_success(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['username'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login()
        shared.Shared.wait_for_element(self.browser.get_browser(), 'id', 'navigation', waittime = 300)

        time.sleep(5)

        self.loginpage.logout(directly_logout = True)
        shared.Shared.wait_for_element(self.browser.browser, 'class_name', 'login', waittime = 300)

    def test_failure_1(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['badusername'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_2(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['username'])
        self.loginpage.set_password(smokecfg['badpassword'])
        self.loginpage.login(expect_fail = True)

    def test_failure_3(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['badusername'])
        self.loginpage.set_password(smokecfg['badpassword'])
        self.loginpage.login(expect_fail = True)

    def test_failure_4(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['sqlinjection_1'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_5(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['sqlinjection_2'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_6(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['sqlinjection_3'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_7(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['sqlinjection_4'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_8(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())

        # wait for at most 5 minutes, in case we have an anoyingly slow server
        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'select-language', waittime = 300)

        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])

        shared.Shared.wait_for_element(self.browser.get_browser(), 'class_name', 'fields', waittime = 300)

        self.loginpage.set_username(smokecfg['sqlinjection_5'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

if __name__ == '__main__':
    unittest.main()
