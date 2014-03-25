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

from selenium import webdriver
from selenium.common.exceptions import *
from selenium.webdriver.support.ui import WebDriverWait # available since 2.4.0
from selenium.webdriver.common.action_chains import ActionChains as action
from common import Global_Locators
from cspages.cspage import CloudStackPage

from common.shared import *

import pdb

class LoginPage(CloudStackPage):
    def __init__(self, browser):
        self.browser = browser
        self.username = ""
        self.password = ""
        self.language = ""

    @try_except_decor
    def set_username(self, username):
        self.username = username
        usernameElement = self.browser.find_element_by_css_selector(Global_Locators.login_username_css)
        usernameElement.send_keys(self.username)

    @try_except_decor
    def set_password(self, password):
        self.password = password
        passwordElement = self.browser.find_element_by_css_selector(Global_Locators.login_password_css)
        passwordElement.send_keys(self.password)
        self.pwelement = passwordElement

    @try_except_decor
    def set_language(self, language):
        self.language = language
        options = self.browser.find_elements_by_xpath('/html/body/div[3]/form/div[2]/div[4]/select/option')
        for option in options:
            if len(option.get_attribute('text')) > 0 and option.get_attribute('text').lower() == language.lower():
                option.click()
                break

    @try_except_decor
    def login(self, expect_fail = False):
        if self.username == "" or self.password == "":
            print "Must set email and password before logging in"
            return
        loginElement = self.browser.find_element_by_css_selector(Global_Locators.login_submit_css)
        loginElement.click()

        time.sleep(3)
        try:
            # in case we have that "Hello and Welcome to CloudStack" page
            ele = None
            ele = self.browser.find_element_by_xpath("//input[@type='submit' and @class='button goTo advanced-installation' and @value='I have used CloudStack before, skip this guide']")
            if ele is not None:
                ele.click()
                time.sleep(2)
        except NoSuchElementException as err:
            pass

    @try_except_decor
    def logout(self, directly_logout = False):

        Shared.wait_for_element(self.browser, 'id', 'user')

        # must click this icon options first
        if directly_logout == False:
            try:
                ele = self.browser.find_element_by_xpath("//div[@id='user-options' and @style='display: block;']")
                if ele is None:
                    ele1 = self.browser.find_element_by_xpath("//div[@id='user' and @class='button']/div[@class='icon options']/div[@class='icon arrow']").click()
            except NoSuchElementException as err:
                ele1 = self.browser.find_element_by_xpath("//div[@id='user' and @class='button']/div[@class='icon options']/div[@class='icon arrow']").click()
        time.sleep(1)

        ele2 = self.browser.find_element_by_xpath("//div[@id='user' and @class='button']/div[@id='user-options']/a[1]").click()

        Shared.wait_for_element(self.browser, 'class_name', 'login')

    @try_except_decor
    def get_error_msg(self, loginpage_url):
        if loginpage_url is not None and len(loginpage_url) > 0 and \
            (self.browser.current_url.find(loginpage_url) > -1 or loginpage_url.find(self.browser.current_url) > -1):
            ele = self.browser.find_element_by_id('std-err')
            return ele.text
        else:
            return ""
