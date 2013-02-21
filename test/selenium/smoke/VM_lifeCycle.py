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



class deployVM(unittest.TestCase):


    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []
        

    def test_deployvm(self):
    
        
        ## Action Part
        # VM will be named Auto-VM and this VM will be used in all subsequent tests. 
        # Deploy an Instance named Auto-VM Default CentOS no GUI Template
        
        driver = self.driver
        self.driver.implicitly_wait(30)
        driver.refresh() ## Most Important step. Failure to do this will change XPATH location and Scripts will fail. 
      
        
        # Click on Instances link
        driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
        
        # Click on add Instance on Instances page
        driver.find_element_by_xpath(Global_Locators.add_instance_xpath).click()
        
        # Following select template action will fire automatically... ignore it. And leave following commented.
        # driver.find_element_by_xpath("(//input[@name='select-template'])[3]").click()
        #Click on Next button on Instances Wizard.
        driver.find_element_by_xpath(Global_Locators.add_instance_next_xpath).click()
        
        # Nothing to do here as we will be using  all default settings. (Default CentOS no GUI template should be highlighted here.  Click Next
        driver.find_element_by_xpath(Global_Locators.add_instance_next_xpath).click()
        
        # Nothing to do here. Medium Instance compute offering should be selected here.  Click Next
        driver.find_element_by_xpath(Global_Locators.add_instance_next_xpath).click()
        
        # Nothing to do here. Data Disk Offering : No Thanks!!. Click Next
        driver.find_element_by_xpath(Global_Locators.add_instance_next_xpath).click()
        
        # Since this is our first instance; we must provide a network name. We will use Test-Network as out network name.
        driver.find_element_by_xpath("(//input[@name='new-network-name'])[2]").click()
        driver.find_element_by_xpath("(//input[@name='new-network-name'])[2]").clear()
        driver.find_element_by_xpath("(//input[@name='new-network-name'])[2]").send_keys("Test-Network")
        
        #Click next
        driver.find_element_by_xpath(Global_Locators.add_instance_next_xpath).click()
        
        # Give our VM a name here. Use Auto-VM as name 
        driver.find_element_by_xpath("(//input[@name='displayname'])[2]").click()
        
        driver.find_element_by_xpath("(//input[@name='displayname'])[2]").clear()
        
        driver.find_element_by_xpath("(//input[@name='displayname'])[2]").send_keys("Auto-VM")
        
        # All data filled. Click Launch VM. (It has the same xpath as Next button. So we will use Next Variable here.
        driver.find_element_by_xpath(Global_Locators.add_instance_next_xpath).click()

        print '\n' + '\n' + "VM Deployment is complete... wait for 5 mins to check deployment status" + '\n' + '\n'
 
 
 
        ## Verification Part
 
    
        ## Now we must wait for some random time (Educated guess based on experience) and check if VM has been deployed and if it is in running state.
        ## Should take about 4 min to deploy VM.. but we will wait 5 mins and check the status , we will do this twice. So total 2 check within 10 mins with first check occuring at 5th min.
                
         
        driver.refresh() # Refresh UI Page; This polls latest status.
        
        # Click on Instances link
        driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
        
        linkclass = None
        linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
        count = 1
        
        while (count > 0):
            
            time.sleep(300)
            for link in linkclass:
                
                if link.text == "Auto-VM": # We will search for our VM in this table
                    print "found VM in table ..  checking status..." + '\n' + '\n'
                    link.click()
                    
                    status = driver.find_element_by_xpath(Global_Locators.state_xpath).text  ## get the status of our VM
                    
                    if status == "Running" :
                        print "VM is in running state... continuing with other tests."+ '\n' + '\n'
                        break
                    else:
                        print "Need to check one more time after 5 mins"
                        continue
            count = count - 1        
   
    
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   
   
   
    
    def tearDown(self):
        self.assertEqual([], self.verificationErrors)





################################################################################################################################################################################################



class destroyVM(unittest.TestCase):



    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []

    
    def test_destroyvm(self):
      
      driver = self.driver
      self.driver.implicitly_wait(100)

      ## Action part
      # Click on Instances link and find our instance
      driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
      time.sleep(2)
    
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
    
      for link in linkclass:
        
         if link.text == "Auto-VM": # We will search for our VM in this table
            link.click()
             
      # Click on Destroy Instance button and confirm
      time.sleep(2)
      driver.find_element_by_css_selector(Global_Locators.destroyinstance_css).click()
      time.sleep(2)
      
      # Click ok on confirmation
      driver.find_element_by_xpath(Global_Locators.yesconfirmation_xapth).click()
      time.sleep(2)
      
      # Go to Dashboard
      # driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      driver.refresh()
        
      ## Verification part
      time.sleep(60)
      
      # Click on Instances link and find our instance
      driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
      time.sleep(2)
        
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
        
      for link in linkclass:
            
          if link.text == "Auto-VM": # We will search for our VM in this table
              link.click()
              
      
      status = driver.find_element_by_xpath(Global_Locators.state_xpath).text  ## get the status of our VM
      if status == "Destroyed" :
          print "VM is Destroyed...."+ '\n' + '\n'
      else:
          print "Something went wrong"
  
  
  
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
  
   

    def tearDown(self):

        self.assertEqual([], self.verificationErrors)
        
        
        


################################################################################################################################################################################################




class rebootVM(unittest.TestCase):



    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []
        
        
    def test_rebootvm(self):

        driver = self.driver
        self.driver.implicitly_wait(30)
        print "Verify this test manually for now" 
        
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(vmLifeAndNetwork.Server_Ip, username='root', password='password')
        print '\n' + '\n' + "Before Reboot ...Executing command date ... "  + '\n' + '\n'
        stdin, stdout, stderr = ssh.exec_command('date')
        print stdout.readlines()
        print '\n' + '\n' + "Before Reboot ...Executing command last reboot | head -1 ..." + '\n' + '\n'
        stdin, stdout, stderr = ssh.exec_command('last reboot | head -1')
        print '\n' + '\n' + "Before Reboot ...Executing command uptime..." + '\n' + '\n'
        stdin, stdout, stderr = ssh.exec_command('uptime')
        print stdout.readlines()
        ssh.close() 
        
        
        driver.refresh()
        
        driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
        
        linkclass = None
        linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
        count = 1
        
        while (count > 0):
            
            #time.sleep(300)
            for link in linkclass:
                
                if link.text == "Auto-VM": # We will search for our VM in this table
                    print "found VM in table ..  Rebooting now..." + '\n' + '\n'
                    link.click()
                    
        driver.find_element_by_css_selector(Global_Locators.rebootinstance_css).click()
        driver.find_element_by_xpath(Global_Locators.actionconfirm_xpath).click()
        
        # Sleep for 5 mins to ensure system gets rebooted.
        time.sleep(300)
        
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(vmLifeAndNetwork.Server_Ip, username='root', password='password')
        print '\n' + '\n' + "After Reboot ...Executing command date ... "  + '\n' + '\n'
        stdin, stdout, stderr = ssh.exec_command('date')
        print stdout.readlines()
        print '\n' + '\n' + "After Reboot ...Executing command last reboot | head -1 ..." + '\n' + '\n'
        stdin, stdout, stderr = ssh.exec_command('last reboot | head -1')
        print '\n' + '\n' + "After Reboot ...Executing command uptime..." + '\n' + '\n'
        stdin, stdout, stderr = ssh.exec_command('uptime')
        print stdout.readlines()
        ssh.close() 


    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   
   
    def tearDown(self):
        self.assertEqual([], self.verificationErrors)
        
        
#########################################################################################################################################################
        
        
        
class restoreVM(unittest.TestCase):

    
    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []
    
    
    def test_restorevm(self):
        
      driver = self.driver
      self.driver.implicitly_wait(100)
    
      ## Action part
      # Click on Instances link and find our instance
      driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
    
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
    
      for link in linkclass:
        
         if link.text == "Auto-VM": # We will search for our VM in this table
             
             link.click()        
      
      # Click on Destroy Instance button and confirm
      driver.find_element_by_css_selector(Global_Locators.restoreinstance_css).click()
      
      # Click ok on confirmation
      driver.find_element_by_xpath(Global_Locators.yesconfirmation_xapth).click()

      # Go to Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      
        
      ## Verification part
     
      time.sleep(60)
      
      # Click on Instances link and find our instance
      driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
        
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
        
      for link in linkclass:
            
          if link.text == "Auto-VM": # We will search for our VM in this table
              link.click()
      
      
      status = driver.find_element_by_xpath(Global_Locators.state_xpath).text  ## get the status of our VM
            
      if status == "Stopped" :
          print "VM is Restored. but in stopped state.. will start now."+ '\n' + '\n'
     
      else:
          print "Something went wrong"
     
      
      
        
      #VM will be in stop state so we must start it now
      # Click on Instances link and find our instance
      driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
    
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
    
      for link in linkclass:
        
         if link.text == "Auto-VM": # We will search for our VM in this table
             link.click()
      
      # Click on Start Instance.
      driver.find_element_by_css_selector(Global_Locators.startinstance_css).click()
      time.sleep(2)
      
      # Dismiss confirmation by clicking Yes 
      driver.find_element_by_xpath(Global_Locators.yesconfirmation_xapth).click()
      time.sleep(2)
      
      # Go to Dashboard
      driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      time.sleep(2)
    
      print "VM is Started."+ '\n' + '\n'
      
      # status = None
      time.sleep(60)
      
      # Dismiss the Start Instance information box.
      driver.find_element_by_xpath(Global_Locators.actionconfirm_xpath).click()
      time.sleep(2)



    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   
   
   
    def tearDown(self):
      
        self.assertEqual([], self.verificationErrors)
        
        
        
#########################################################################################################################################################
        
        
        
class startVM(unittest.TestCase):



    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []
        
    
    def test_startvm(self):
        
      driver = self.driver
      self.driver.implicitly_wait(100)

      ## Action part
      #driver.refresh() ## Most Important step. Failure to do this will change XPATH location and Scripts will fail.
      # Click on Instances link and find our instance
      driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
    
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
    
      for link in linkclass:
        
         if link.text == "Auto-VM": # We will search for our VM in this table
             print "found VM in table ..  checking status..." + '\n' + '\n'
             link.click()

      
      
      # Click on Start Instance.
      driver.find_element_by_css_selector(Global_Locators.startinstance_css).click()
      time.sleep(2)
      
      # Dismiss confirmation by clicking Yes 
      driver.find_element_by_xpath(Global_Locators.yesconfirmation_xapth).click()
      time.sleep(2)
      
      # Go to Dashboard
      #driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
      driver.refresh()
      
      
      ## Verification part
      # status = None
      time.sleep(60)
      
      # Dismiss the Start Instance information box.
      driver.find_element_by_xpath(Global_Locators.actionconfirm_xpath).click()
      time.sleep(2)

      # Click on Instances link and find our instance
      driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
      time.sleep(2)  
      
      linkclass = None
      linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
        
      for link in linkclass:
            
          if link.text == "Auto-VM": # We will search for our VM in this table
              link.click()
    
            
      status = driver.find_element_by_xpath(Global_Locators.state_xpath).text  ## get the status of our VM
      
      if status == "Running" :
          print "VM is in Running state..."+ '\n' + '\n'

      else:
          print "Something went wrong"

      # Go to Dashboard
      driver.refresh()

   
    
    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   

    def tearDown(self):

        self.assertEqual([], self.verificationErrors)
        
        


#########################################################################################################################################################
        
        
        
class stopVM(unittest.TestCase):

    def setUp(self):
        
        self.driver = initialize.getOrCreateWebdriver()
        self.verificationErrors = []
        
    
    def test_stopvm(self):
        
        driver = self.driver
        self.driver.implicitly_wait(100)
      
        ## Action part
        driver.refresh() ## Important step.
        
        # Click on Instances link and find our instance
        driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
        
        linkclass = None
        linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
        
        for link in linkclass:
            
            if link.text == "Auto-VM": # We will search for our VM in this table
                print "found VM in table ..  checking status..." + '\n' + '\n'
                link.click()
    
        
        # HWe are on our VM information page.
        driver.find_element_by_css_selector(Global_Locators.stopinstance_css).click()
        time.sleep(2)
        
        # a Pop up must appear; below we will check the force stop check box and then we will click ok.
        driver.find_element_by_id(Global_Locators.stopinstanceforce_id).click()
        driver.find_element_by_xpath(Global_Locators.actionconfirm_xpath).click()
        time.sleep(2)
        
        # Go to Dahsboard
        #driver.find_element_by_xpath(Global_Locators.dashboard_xpath).click()
        driver.refresh()
        
        # Should take less than min to stop the instance. We will check twice at interval of 45 seconds o be safe. 
        ## Verification part
        time.sleep(60)
        
        # Click on Instances link and find our instance
        driver.find_element_by_xpath(Global_Locators.instances_xpath).click()
        
        linkclass = None
        linkclass = driver.find_elements_by_xpath(Global_Locators.instances_table_xpath) # This returns a list of all VM names in tables
        
        for link in linkclass:
            
            if link.text == "Auto-VM": # We will search for our VM in this table
                link.click()
    
            
        status = driver.find_element_by_xpath(Global_Locators.state_xpath).text  ## get the status of our VM
        
        if status == "Stopped" :
            print "VM is in Stopped state...."+ '\n' + '\n'
        else:
            print "Something went wrong"
   
                

    def is_element_present(self, how, what):

        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
   
   

    def tearDown(self):

        self.assertEqual([], self.verificationErrors)


#########################################################################################################################################################
