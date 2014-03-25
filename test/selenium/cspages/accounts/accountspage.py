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

class AccountsPage(CloudStackPage):

    def __init__(self, browser):
        self.browser = browser
        self.accounts = []

    @try_except_decor
    def get_accounts(self):
        rows = self.browser.find_elements_by_xpath("//div[@class='data-table']/table[@class='body']/tbody/tr")
        for row in rows:
            account = {}
            columes = row.find_elements_by_tag_name('td')
            account['Name'] =   columes[0].get_attribute('title').lower()
            account['Role'] =   columes[1].get_attribute('title').lower()
            account['Domain'] = columes[2].get_attribute('title').lower()
            account['State'] =  columes[3].get_attribute('title').lower()
            self.accounts.append(account)

    @try_except_decor
    def account_exists(self, name):
        if len(self.accounts) == 0:
            self.get_accounts()
        account = [acct for acct in self.accounts if acct['Name'] == name.lower()]
        if len(account) > 0:
            return True
        else:
            return False

    @try_except_decor
    def add_account(self, username = "", password = "", email = "", firstname = "", lastname = "", domain = "", account = "", type = "", timezone = "", network_domain = ""):
        # type = role
        if len(username) == 0 or len(password) == 0 or len(email) == 0 or len(firstname) == 0 or len(lastname) == 0 or len(domain) == 0 or len(type) == 0:
            return;
        if type not in ('User', 'Admin'):
            print "Account type must be either User or Admin."
            return;
        if self.account_exists(username):
            return

        # click Add Account
        ele = self.browser.find_element_by_xpath("//div[@class='toolbar']")
        ele1 = ele.find_element_by_xpath("//div[3]/span")
        ele1.click()

        Shared.wait_for_element(self.browser, 'id', 'label_username')
        ele = self.browser.find_element_by_xpath("(//input[@name='username' and @type='text' and @id='label_username'])")
        ele.send_keys(username)
        ele = self.browser.find_element_by_xpath("(//input[@name='password' and @type='password' and @id='password'])")
        ele.send_keys(password)
        ele = self.browser.find_element_by_xpath("(//input[@name='password-confirm' and @type='password' and @id='label_confirm_password'])")
        ele.send_keys(password)
        ele = self.browser.find_element_by_xpath("(//input[@name='email' and @type='text' and @id='label_email'])")
        ele.send_keys(email)
        ele = self.browser.find_element_by_xpath("(//input[@name='firstname' and @type='text' and @id='label_first_name'])")
        ele.send_keys(firstname)
        ele = self.browser.find_element_by_xpath("(//input[@name='lastname' and @type='text' and @id='label_last_name'])")
        ele.send_keys(lastname)
        Shared.option_selection(self.browser, 'id', 'label_domain', 'ROOT')
        if len(account) > 0:
            ele = self.browser.find_element_by_xpath("(//input[@name='account' and @type='text' and @id='label_account'])")
            ele.send_keys(account)
        Shared.option_selection(self.browser, 'id', 'label_type', type)
        Shared.option_selection(self.browser, 'id', 'label_timezone', timezone)
        if len(network_domain) > 0:
            ele = self.browser.find_element_by_xpath("(//input[@name='networkdomain' and @type='text' and @id='label_network_domain'])")
            ele.send_keys(network_domain)
        self.button_add()

        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def select_account(self, username = "", domain = "", type = ""):
        if len(username) == 0 or len(domain) == 0 or len(type) == 0:
            return False;
        if self.account_exists(username) == False:
            print "The account does not exist"
            return False

        # select the account
        ele = self.browser.find_element_by_xpath("//div[@class='data-table']/div[@class='fixed-header']/table")
        ele1 = ele.find_element_by_xpath("//tbody")
        ele2 = ele1.find_elements_by_tag_name('tr')
        for e in ele2:
            ele3 = e.find_elements_by_tag_name('td')
            # move mouse to quickview
            if len(ele3) > 4 and \
                ele3[0].text == username and \
                ele3[1].text == type and \
                ele3[2].text == domain and \
                ele3[3].text == 'enabled':
                ele3[4].find_element_by_tag_name('span').click()
                Shared.wait_for_element(self.browser, 'class_name', 'details')
                # select account
                ele = self.browser.find_element_by_xpath("//div[@id='details-tab-details']/div[@class='details']/div/table/tbody/tr/td[@class='view-all']")
                ele1 = ele.find_element_by_tag_name('a').find_element_by_tag_name('span').click()
                break

        Shared.wait_for_element(self.browser, 'class_name', 'view-all')

    @try_except_decor
    def delete_account(self, username = "", domain = "", type = ""):
        if len(username) == 0 or len(domain) == 0 or len(type) == 0:
            return False;
        if self.account_exists(username) == False:
            print "The account does not exist"
            return False

        # find the account
        ele = self.browser.find_element_by_xpath("//div[@class='data-table']/div[@class='fixed-header']/table")
        ele1 = ele.find_element_by_xpath("//tbody")
        ele2 = ele1.find_elements_by_tag_name('tr')
        for e in ele2:
            ele3 = e.find_elements_by_tag_name('td')
            # move mouse to quickview
            if len(ele3) > 4 and \
                ele3[0].text == username and \
                ele3[1].text == type and \
                ele3[2].text == domain and \
                ele3[3].text == 'enabled':
                ele3[4].find_element_by_tag_name('span').click()
                Shared.wait_for_element(self.browser, 'class_name', 'details')
                # delete account
                ele = self.browser.find_element_by_xpath("//div[@id='details-tab-details']/div[@class='details']/div/table/tbody/tr/td/div[@class='buttons']")
                ele1 = ele.find_element_by_xpath("//div[@class='action remove single text' and @title='Delete account']/span").click()
                Shared.wait_for_element(self.browser, 'class_name', 'ui-dialog-buttonset')
                self.button_yes()
                break

        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def button_cancel(self):
        ele = self.browser.find_element_by_xpath("/html/body/div[4]/div[2]/div/button[1]/span").click()
        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def button_add(self):
        ele = self.browser.find_element_by_xpath("/html/body/div[4]/div[2]/div/button[2]/span").click()
        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def button_no(self):
        ele = self.browser.find_element_by_xpath("/html/body/div[4]/div[10]/div/button[1]/span").click()
        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def button_yes(self):
        ele = self.browser.find_element_by_xpath("/html/body/div[4]/div[10]/div/button[2]/span").click()
        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')
