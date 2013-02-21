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
import initialize
import Global_Locators




class Disk_offering_Add(unittest.TestCase):

    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []


    
    def test_diskadd(self):
        
      driver = self.driver
      self.driver.implicitly_wait(200)
      
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      # Go to Service Offerings
      driver.find_element_by_xpath(Global_Locators.serviceOfferings_xpath).click()
      
      #Select Disk offering
      driver.find_element_by_xpath(Global_Locators.Offering_disk_xpath).click()
      
      # Add offering
      driver.find_element_by_xpath(Global_Locators.Offering_add_xpath).click()
      
      # Following have names.. so they do not have their global entries.
      driver.find_element_by_name("name").clear()
      driver.find_element_by_name("name").send_keys("Test Disk Name")
      driver.find_element_by_name("description").clear()
      driver.find_element_by_name("description").send_keys("Test Disk Description")
      driver.find_element_by_name("disksize").clear()
      driver.find_element_by_name("disksize").send_keys("1")
      driver.find_element_by_xpath("//button[@type='button']").click()
      time.sleep(20)
      
      ##Verification will be if this offering shows up into table and we can actually edit it.
      
   
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   
   
    def tearDown(self):
        self.assertEqual([], self.verificationErrors)
        




class Disk_offering_Edit(unittest.TestCase):



    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []

    
    def test_diskedit(self):
        
      driver = self.driver
      self.driver.implicitly_wait(200)
      
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      # Go to Service Offerings
      driver.find_element_by_xpath(Global_Locators.serviceOfferings_xpath).click()
      
      #Select Disk offering
      driver.find_element_by_xpath(Global_Locators.Offering_disk_xpath).click()
      
      # We will be searching for our disk offering into the table
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.Offering_table_xpath) # This returns a list of all Offerings in table
    
      for link in linkclass:
        
         if link.text == "Test Disk Name":
            link.click()
    
      time.sleep(2)
      
      # Click Edit
      driver.find_element_by_css_selector(Global_Locators.Offering_edit_css).click()
      
      #Change name
      driver.find_element_by_name(Global_Locators.Offering_editname_name).clear()
      driver.find_element_by_name(Global_Locators.Offering_editname_name).send_keys("Test Name")
      
      # Change Description
      driver.find_element_by_name(Global_Locators.Offering_editdescription_name).clear()
      driver.find_element_by_name(Global_Locators.Offering_editdescription_name).send_keys("Test Description")
      
      #Click Done
      driver.find_element_by_css_selector(Global_Locators.Offering_editdone_css).click()
      time.sleep(10)
      
   

   
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True

   
      
    def tearDown(self):
        self.assertEqual([], self.verificationErrors)
        
        # Now we will find this offering and delete it!!
        
        
        
        
        
        
class Disk_offering_Delete(unittest.TestCase):


    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []

    
    def test_diskdelete(self):
        
      driver = self.driver
      self.driver.implicitly_wait(200)
      
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      # Go to Service Offerings
      driver.find_element_by_xpath(Global_Locators.serviceOfferings_xpath).click()
      
      #Select Disk offering
      driver.find_element_by_xpath(Global_Locators.Offering_disk_xpath).click()

      ## Action part
      # We will be searching for our disk offering into the table
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.Offering_table_xpath) # This returns a list of all Offerings in table
    
      for link in linkclass:
        
         if link.text == "Test Name":
            link.click()
    
      time.sleep(2)
      
      # Click Delete
      driver.find_element_by_css_selector(Global_Locators.Offering_delete_css).click()
      time.sleep(2)
      driver.find_element_by_xpath(Global_Locators.yesconfirmation_xapth).click()
      time.sleep(20)
      

      
    def is_element_present(self, how, what):

      try: self.driver.find_element(by=how, value=what)
      except NoSuchElementException, e: return False
      return True
   
      

    def tearDown(self):

        self.assertEqual([], self.verificationErrors)

        
        
        
        
        
        
        
class Compute_offering_Add(unittest.TestCase):


    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []


    
    def test_computeadd(self):
        
      driver = self.driver
      self.driver.implicitly_wait(200)
      
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      # Go to Service Offerings
      driver.find_element_by_xpath(Global_Locators.serviceOfferings_xpath).click()
      
      #Select Compute offering
      driver.find_element_by_xpath(Global_Locators.Offering_compute_xpath).click()

      ## Action part

      # Add offering
      driver.find_element_by_xpath(Global_Locators.Offering_add_xpath).click()
      
      # Following do not have Global locators
      driver.find_element_by_id("label_name").clear()
      driver.find_element_by_id("label_name").send_keys("Test Compute Name")
      driver.find_element_by_id("label_description").clear()
      driver.find_element_by_id("label_description").send_keys("Test Compute Description")
      driver.find_element_by_id("label_num_cpu_cores").clear()
      driver.find_element_by_id("label_num_cpu_cores").send_keys("2")
      driver.find_element_by_id("label_cpu_mhz").clear()
      driver.find_element_by_id("label_cpu_mhz").send_keys("2000")
      driver.find_element_by_id("label_memory_mb").clear()
      driver.find_element_by_id("label_memory_mb").send_keys("2048")
      driver.find_element_by_id("label_network_rate").clear()
      driver.find_element_by_id("label_network_rate").send_keys("10")
      driver.find_element_by_id("label_offer_ha").click()
      driver.find_element_by_xpath("//button[@type='button']").click()

      time.sleep(2)
                  
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      
      time.sleep(30)



    def is_element_present(self, how, what):

      try: self.driver.find_element(by=how, value=what)
      except NoSuchElementException, e: return False
      return True
   
   

    def tearDown(self):

        self.assertEqual([], self.verificationErrors)
        






class Compute_offering_Edit(unittest.TestCase):



    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []


    
    def test_computeedit(self):
        
        
      driver = self.driver
      self.driver.implicitly_wait(200)
      
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      ## Action part
      # Go to Service Offerings
      driver.find_element_by_xpath(Global_Locators.serviceOfferings_xpath).click()
      
      #Select Compute offering
      driver.find_element_by_xpath(Global_Locators.Offering_compute_xpath).click()

      # We will be searching for our disk offering into the table
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.Offering_table_xpath) # This returns a list of all Offerings in table
    
      for link in linkclass:
        
         if link.text == "Test Compute Name":
            link.click()
    
      time.sleep(2)

      
      # Click Edit
      driver.find_element_by_css_selector(Global_Locators.Offering_edit_css).click()
      
      #Change name
      driver.find_element_by_name(Global_Locators.Offering_editname_name).clear()
      driver.find_element_by_name(Global_Locators.Offering_editname_name).send_keys("Test Name")
      
      # Change Description
      driver.find_element_by_name(Global_Locators.Offering_editdescription_name).clear()
      driver.find_element_by_name(Global_Locators.Offering_editdescription_name).send_keys("Test Description")
      
      #Click Done
      driver.find_element_by_css_selector(Global_Locators.Offering_editdone_css).click()
      time.sleep(10)
      



    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   


    def tearDown(self):
        self.assertEqual([], self.verificationErrors)
        




        
class Compute_offering_Delete(unittest.TestCase):



    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []


    
    def test_computedelete(self):
        
        
      driver = self.driver
      self.driver.implicitly_wait(200)
      
      #Make sure you are on Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
      
      # Go to Service Offerings
      driver.find_element_by_xpath(Global_Locators.serviceOfferings_xpath).click()
      
      #Select Compute offering
      driver.find_element_by_xpath(Global_Locators.Offering_compute_xpath).click()

      ## Action part
      # We will be searching for our disk offering into the table
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.Offering_table_xpath) # This returns a list of all Offerings in table
    
      for link in linkclass:
        
         if link.text == "Test Name": 
            link.click()
    
      time.sleep(2)
      
      # Click Delete
      
      driver.find_element_by_css_selector(Global_Locators.Offering_deletecompute_css).click()
      driver.find_element_by_xpath(Global_Locators.yesconfirmation_xapth).click()

      time.sleep(20)
      


    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   
         

    def tearDown(self):
      
        self.assertEqual([], self.verificationErrors)
