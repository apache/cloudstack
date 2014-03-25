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
import common.shared as shared
from cstests.smoketests.smokecfg import smokecfg
import cspages.login.loginpage as loginpage
import cspages.dashboard.dashboardpage as dashboardpage
import cspages.accounts.accountspage as accountspage
import cspages.accounts.userspage as userspage

# from cstests.smoketests import smokecfg as smokecfg

class TestCSDeleteUser(unittest.TestCase):
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

        shared.Shared.wait_for_element(self.browser.browser, 'id', 'navigation')

        time.sleep(3)

        self.dashboardpage = dashboardpage.DashboardPage(self.browser.get_browser())

        # navigate to Accounts page
        self.dashboardpage.navigate_to('accounts')

        # make sure we are on Accounts page
        activeitem = self.dashboardpage.get_active_item()
        if activeitem.find('accounts') < 0:
            self.assertRaises(ValueError, self.dashboardpage.get_active_item(), activeitem)

        # now we are at Accounts page
        self.accountspage = accountspage.AccountsPage(self.browser.get_browser())
        self.accountspage.select_account(username = smokecfg['account']['username'],
                                      domain = smokecfg['account']['domain'],
                                      type = smokecfg['account']['type'],
                                     )

        # now we are at users page
        self.userspage = userspage.UsersPage(self.browser.get_browser())
        self.userspage.delete_user(username  = smokecfg['new user']['username'],
                                   firstname = smokecfg['new user']['firstname'],
                                   lastname  = smokecfg['new user']['lastname'],
                                  )

        self.loginpage.logout()

        shared.Shared.wait_for_element(self.browser.browser, 'class_name', 'login')

    def xtest_failure_8(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        # language selection must be done before username and password
        self.loginpage.set_language(smokecfg['language'])
        self.loginpage.set_username(smokecfg['sqlinjection_5'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

if __name__ == '__main__':
    unittest.main()
