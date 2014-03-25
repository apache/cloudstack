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

class DashboardPage(CloudStackPage):

    def __init__(self, browser):
        self.browser = browser
        self.active_item = ""
        self.items = []

    @try_except_decor
    def get_active_item(self):
        self.active_item = ""
        lis = self.browser.find_elements_by_xpath("//*[@id='navigation']/ul/li")
        for li in lis:
            if li.get_attribute('class').find('active') > 0:
                self.active_item = li.get_attribute('class')[:(li.get_attribute('class').index(' active'))]
                return self.active_item

    @try_except_decor
    def get_items(self):
        lis = self.browser.find_elements_by_xpath("//*[@id='navigation']/ul/li")
        for li in lis:
            item = li.get_attribute('class')[len('navigation-item '):]
            if item.find('active') > 0:
                item = item[:(item.index(' active'))]
            if item.find('first') > 0:
                item = item[:(item.index(' first'))]
            if item.find('last') > 0:
                item = item[:(item.index(' last'))]
            self.items.append(item.lower())
        return self.items
#       import pdb
#       pdb.set_trace()

    @try_except_decor
    def navigate_to(self, item_name):
        if len(self.items) == 0:
            self.get_items()
        if item_name is None or len(item_name) == 0 or \
           item_name.lower() not in self.items or \
           (len(self.active_item) > 0 and self.active_item.lower().find(item_name.lower()) > 0):
            return

        lis = self.browser.find_elements_by_xpath("//*[@id='navigation']/ul/li")
        for li in lis:
            if li.get_attribute('class').lower().find(item_name.lower()) > 0:
                li.click()
                time.sleep(3)
                return

