import unittest
import sys, os, time
import json

sys.path.append('./')

import browser.firefox as firefox
import cspages.loginpage as loginpage
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
        self.loginpage.set_username(smokecfg['username'])
        self.loginpage.set_password(smokecfg['password'])
#       self.loginpage.set_language(smokecfg['language'])
        self.loginpage.login()
        shared.Shared.wait_for_element(self.browser.browser, 'id', 'navigation')

        time.sleep(5)

        self.loginpage.logout()
        shared.Shared.wait_for_element(self.browser.browser, 'class_name', 'login')

    def test_failure_1(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        self.loginpage.set_username(smokecfg['badusername'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_2(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        self.loginpage.set_username(smokecfg['username'])
        self.loginpage.set_password(smokecfg['badpassword'])
        self.loginpage.login(expect_fail = True)

    def test_failure_3(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        self.loginpage.set_username(smokecfg['badusername'])
        self.loginpage.set_password(smokecfg['badpassword'])
        self.loginpage.login(expect_fail = True)

    def test_failure_4(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        self.loginpage.set_username(smokecfg['sqlinjection_1'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_5(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        self.loginpage.set_username(smokecfg['sqlinjection_2'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_6(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        self.loginpage.set_username(smokecfg['sqlinjection_3'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_7(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        self.loginpage.set_username(smokecfg['sqlinjection_4'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

    def test_failure_8(self):
        self.browser.set_url(smokecfg['cssite'])
        self.loginpage = loginpage.LoginPage(self.browser.get_browser())
        self.loginpage.set_username(smokecfg['sqlinjection_5'])
        self.loginpage.set_password(smokecfg['password'])
        self.loginpage.login(expect_fail = True)

if __name__ == '__main__':
    unittest.main()
