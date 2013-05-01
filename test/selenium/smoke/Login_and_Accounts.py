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

import sys, os
sys.path.append(os.path.abspath(os.path.dirname(__file__) + '/'+'../lib'))


from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
import unittest, time
import Global_Locators
import initialize



class login(unittest.TestCase):

    
    def setUp(self):

        MS_URL = initialize.getMSip()
        self.driver = initialize.getOrCreateWebdriver()
        self.base_url = "http://"+ MS_URL +":8080/" # Your management Server IP goes here
        self.verificationErrors = []


    def test_login(self):
        
        # Here we will clear the test box for Username and Password and fill them with actual login data.
        # After that we will click Login (Submit button)
        driver = self.driver
        driver.maximize_window()
        driver.get(self.base_url + "client/")
        driver.find_element_by_css_selector(Global_Locators.login_username_css).clear() 
        driver.find_element_by_css_selector(Global_Locators.login_username_css).send_keys("admin")
        driver.find_element_by_css_selector(Global_Locators.login_password_css).clear() 
        driver.find_element_by_css_selector(Global_Locators.login_password_css).send_keys("password")
        driver.find_element_by_css_selector(Global_Locators.login_submit_css).click()
        time.sleep(5)
    



    def is_element_present(self, how, what):
        
        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True

    
    
    def tearDown(self):
        
        self.assertEqual([], self.verificationErrors)



################################################################################################################################################



class logout(unittest.TestCase):

    
    
    def setUp(self):
    
        self.driver = initialize.getOrCreateWebdriver()
        self.driver.implicitly_wait(100)
        self.verificationErrors = []

        
         
    def test_logout(self):
        
        # Here we will clear the test box for Username and Password and fill them with actual login data.
        # After that we will click Login (Submit button)
        driver = self.driver
        driver.find_element_by_xpath("//div[@id='navigation']/ul/li").click()
        driver.find_element_by_css_selector("div.icon.options").click()
        driver.find_element_by_link_text("Logout").click()



    
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True

    
    
    def tearDown(self):
        
        self.assertEqual([], self.verificationErrors)



################################################################################################################################################



class login_test(unittest.TestCase):


    
    def setUp(self):
    
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []

         
    def test_logintest(self):
        
        # Here we will clear the test box for Username and Password and fill them with actual login data.
        # After that we will click Login (Submit button)
        driver = self.driver
        driver.find_element_by_css_selector(Global_Locators.login_username_css).clear() 
        driver.find_element_by_css_selector(Global_Locators.login_username_css).send_keys("test")
        driver.find_element_by_css_selector(Global_Locators.login_password_css).clear() 
        driver.find_element_by_css_selector(Global_Locators.login_password_css).send_keys("password")
        driver.find_element_by_css_selector(Global_Locators.login_submit_css).click()
        time.sleep(5)
    
    
    
    def is_element_present(self, how, what):
    
        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
    
    
    
    def tearDown(self):
        
        self.assertEqual([], self.verificationErrors)
    
        
################################################################################################################################################


class createAcc(unittest.TestCase):


    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []
        
        
    
    def test_createacc(self):
        
        driver = self.driver
        self.driver.implicitly_wait(100)
        driver.find_element_by_xpath("//div[@id='navigation']/ul/li[8]/span[2]").click()
        driver.find_element_by_xpath("//div[3]/span").click()
        driver.find_element_by_id("label_username").clear()
        driver.find_element_by_id("label_username").send_keys("test")
        driver.find_element_by_id("password").clear()
        driver.find_element_by_id("password").send_keys("password")
        driver.find_element_by_id("label_confirm_password").clear()
        driver.find_element_by_id("label_confirm_password").send_keys("password")
        driver.find_element_by_id("label_email").clear()
        driver.find_element_by_id("label_email").send_keys("test@citrix.com")
        driver.find_element_by_id("label_first_name").clear()
        driver.find_element_by_id("label_first_name").send_keys("test")
        driver.find_element_by_id("label_last_name").clear()
        driver.find_element_by_id("label_last_name").send_keys("test")
        driver.find_element_by_id("label_domain").click()
        Select(driver.find_element_by_id("label_type")).select_by_visible_text("Admin")
        Select(driver.find_element_by_id("label_timezone")).select_by_visible_text("[UTC-08:00] Pacific Standard Time")
        driver.find_element_by_xpath("//button[@type='button']").click()
   
        # Go to Dashboard
        driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()     
        time.sleep(30)
   
   
   
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   
    
   
    def tearDown(self):
        
        self.assertEqual([], self.verificationErrors)
   
   
    
################################################################################################################################################


class tearAcc(unittest.TestCase):


    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []
   
        
    
    def test_tearacc(self):
        
        driver = self.driver
        driver.find_element_by_css_selector("li.navigation-item.accounts").click()
        driver.find_element_by_css_selector("tr.odd > td.name.first").click()
        driver.find_element_by_css_selector("a[alt=\"Delete account\"] > span.icon").click()
        driver.find_element_by_xpath("(//button[@type='button'])[2]").click()
        
        # Go to Dashboard
        driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
        time.sleep(30)
   
   
   
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   
   
    def tearDown(self):
        
        self.driver.quit()
        self.assertEqual([], self.verificationErrors)
        
        
        
################################################################################################################################################
