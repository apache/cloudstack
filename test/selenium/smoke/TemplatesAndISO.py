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

'''
ISO PART YET TO BE ADDED:: remove this after adding it.
'''

import sys, os
sys.path.append(os.path.abspath(os.path.dirname(__file__) + '/'+'../lib'))



from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
import unittest, time
import initialize
import Global_Locators




class Template_Add(unittest.TestCase):



    def setUp(self):

        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []


    
    def test_templateadd(self):
        
        
      driver = self.driver

      ## Action part
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      # Go to Templates
      driver.find_element_by_xpath(Global_Locators.templates_xpath).click()

      #Select Template from drop down list
      driver.find_element_by_xpath(Global_Locators.template_xpath).click()
      
      # Add Template
      driver.find_element_by_xpath(Global_Locators.AddTemplate_xpath).click()

      # Following have names.. so they do not have their global entries.
      driver.find_element_by_id("label_name").clear()
      driver.find_element_by_id("label_name").send_keys("Test Template Ubuntu")
      driver.find_element_by_id("label_description").clear()
      driver.find_element_by_id("label_description").send_keys("Ubuntu 10.04")
      driver.find_element_by_id("URL").clear()
      driver.find_element_by_id("URL").send_keys("http://nfs1.lab.vmops.com/templates/Ubuntu/Ubuntuu-10-04-64bit-server.vhd")
      Select(driver.find_element_by_id("label_os_type")).select_by_visible_text("Ubuntu 10.04 (64-bit)")
      driver.find_element_by_id("label_public").click()
      driver.find_element_by_id("label_featured").click()
      driver.find_element_by_xpath("//button[@type='button']").click()

      time.sleep(2)

      # Go to Dash Board
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      
      
      time.sleep(600)
      
      ##Verification will be if this offering shows up into table and we can actually edit it.

      
      
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   


    def tearDown(self):
      
        self.assertEqual([], self.verificationErrors)
        






class Template_Edit(unittest.TestCase):



    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []
    
    
    
    def test_templateedit(self):
        
      driver = self.driver

      ## Action part
      
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      # Go to Templates
      driver.find_element_by_xpath(Global_Locators.templates_xpath).click()

      #Select Template from drop down list
      driver.find_element_by_xpath(Global_Locators.template_xpath).click()
      
      
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.template_table_xpath) # This returns a list 
    
      for link in linkclass:
        
         if link.text == "Test Template Ubuntu": # We will search for our VM in this table
            link.click()
    
      time.sleep(2)
    
      # Change name
      driver.find_element_by_name("name").clear()
      driver.find_element_by_name("name").send_keys("Test template")


      # Change Description
      driver.find_element_by_name("displaytext").clear()
      driver.find_element_by_name("displaytext").send_keys("ubuntu")
      
      driver.find_element_by_css_selector(Global_Locators.template_editdone_css).click()
      time.sleep(2)
      
      #Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(10)



    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True

   
      
    def tearDown(self):

        self.assertEqual([], self.verificationErrors)
        

# Now we will find this offering and delete it!!
        
        
        
        
        
        
class Template_Delete(unittest.TestCase):
    

    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []


    
    def test_templatedelete(self):

      driver = self.driver

      ## Action part
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      # Go to Templates
      driver.find_element_by_xpath(Global_Locators.templates_xpath).click()

      #Select Template from drop down list
      driver.find_element_by_xpath(Global_Locators.template_xpath).click()
      
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.template_table_xpath) # This returns a list 
    
      for link in linkclass:
        
         if link.text == "Test Template": # We will search for our VM in this table
            link.click()
    
      time.sleep(2)
      
      driver.find_element_by_css_selector(Gloabl_Locators.template_delete_css).click()
      driver.find_element_by_xpath(Global_Locators.yesconfirmation_xapth).click()
      
      time.sleep(2)
      
      #Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()

      time.sleep(20)
      

        
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
                 


    def tearDown(self):

        self.assertEqual([], self.verificationErrors)
