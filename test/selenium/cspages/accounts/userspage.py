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

class UsersPage(CloudStackPage):

    def __init__(self, browser):
        self.browser = browser
        self.users = []

    @try_except_decor
    def get_users(self):
        ele = self.browser.find_element_by_xpath("//div[@class='container cloudStack-widget cloudBrowser']")
        rows = ele.find_elements_by_xpath("//div[@class='panel']/div[2]/div[@class='view list-view']/div[@class='data-table']/table[@class='body']/tbody/tr")
        for row in rows:
            user = {}
            columes = row.find_elements_by_tag_name('td')
            user['username'] =   columes[0].get_attribute('title').lower()
            user['firstname'] =   columes[1].get_attribute('title').lower()
            user['lastname'] = columes[2].get_attribute('title').lower()
            self.users.append(user)

    @try_except_decor
    def user_exists(self, username):
        if len(self.users) == 0:
            self.get_users()
        users = [u for u in self.users if u['username'] == username.lower()]
        if len(users) > 0:
            return True
        else:
            return False

    @try_except_decor
    def add_user(self, username = "", password = "", email = "", firstname = "", lastname = "", timezone = ""):
        if len(username) == 0 or len(password) == 0 or len(email) == 0 or len(firstname) == 0 or len(lastname) == 0:
            return;
        if self.user_exists(username):
            return

        # click Add User
        ele = self.browser.find_element_by_xpath("//div[@class='container cloudStack-widget cloudBrowser']")
        ele1 = ele.find_element_by_xpath("//div[@class='panel']/div[2]/div[@class='view list-view']/div[@class='toolbar']/div[@class='button action add reduced-hide']/span")
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
        Shared.option_selection(self.browser, 'id', 'label_timezone', timezone)
        self.button_ok()

        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def delete_user(self, username = "", firstname = "", lastname = ""):
        if len(username) == 0 or len(firstname) == 0 or len(lastname) == 0:
            return False;
        if self.user_exists(username) == False:
            print "The user does not exist"
            return False

        # find the user
        ele = self.browser.find_element_by_xpath("//div[@class='container cloudStack-widget cloudBrowser']")
        ele1 = ele.find_element_by_xpath("//div[@class='panel']/div[2]/div[@class='view list-view']/div[@class='data-table']/table[@class='body']/tbody")
        ele2 = ele1.find_elements_by_tag_name('tr')
        for e in ele2:
            ele3 = e.find_elements_by_tag_name('td')
            # move mouse to quickview
            if len(ele3) > 3 and \
                ele3[0].text == username and \
                ele3[1].text == firstname and \
                ele3[2].text == lastname:
                ele3[3].find_element_by_tag_name('span').click()
                Shared.wait_for_element(self.browser, 'class_name', 'details')
                # delete user
                ele = self.browser.find_element_by_xpath("//div[@id='details-tab-details']/div[@class='details']/div/table/tbody/tr/td/div[@class='buttons']")
                ele1 = ele.find_element_by_xpath("//div[@class='action remove single text' and @title='Delete User']/span").click()
                Shared.wait_for_element(self.browser, 'class_name', 'ui-dialog-buttonset')
                self.button_yes()
                break

        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def button_yes(self):
        eles = self.browser.find_elements_by_xpath("//div[@class='ui-dialog-buttonset']/button[@type='button' and @role='button']")
        for e in eles:
            ele = e.find_element_by_class_name('ui-button-text')
            if e.text == 'Yes':
                e.click()
                break
        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def button_no(self):
        ele = self.browser.find_element_by_xpath("/html/body/div[4]/div[10]/div/button[1]/span").click()
        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def button_ok(self):
        eles = self.browser.find_elements_by_xpath("//button[@type='button' and @role='button']")
        for e in eles:
            if e.text == 'OK':
                e.click()
                break
        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')

    @try_except_decor
    def button_cancel(self):
        eles = self.browser.find_elements_by_xpath("//button[@type='button' and @role='button']")
        for e in eles:
            if e.text == 'Cancel':
                e.click()
                break
        Shared.wait_for_element(self.browser, 'class_name', 'fixed-header')
