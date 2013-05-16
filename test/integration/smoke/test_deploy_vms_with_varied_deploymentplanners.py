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

#!/usr/bin/env python

import marvin
from marvin import cloudstackTestCase
from marvin.cloudstackTestCase import *

import unittest
import hashlib
import random

class TestDeployVmWithVariedPlanners(cloudstackTestCase):
    """
    This test tests that we can create serviceOfferings with different deployment Planners and deploy virtual machines into a user account
    using these service offerings and builtin template
    """
    def setUp(self):
        """
        CloudStack internally saves its passwords in md5 form and that is how we
        specify it in the API. Python's hashlib library helps us to quickly hash
        strings as follows
        """
        mdf = hashlib.md5()
        mdf.update('password')
        mdf_pass = mdf.hexdigest()
		
        self.apiClient = self.testClient.getApiClient() #Get ourselves an API client

        self.acct = createAccount.createAccountCmd() #The createAccount command
        self.acct.accounttype = 0                    #We need a regular user. admins have accounttype=1
        self.acct.firstname = 'test'
        self.acct.lastname = 'user'                 #What's up doc?
        self.acct.username = 'testuser'
        self.acct.password = mdf_pass				#The md5 hashed password string
        self.acct.email = 'test@domain.com'
        self.acct.account = 'testacct'
        self.acct.domainid = 1                       #The default ROOT domain
        self.acctResponse = self.apiClient.createAccount(self.acct)
        # And upon successful creation we'll log a helpful message in our logs
        # using the default debug logger of the test framework
        self.debug("successfully created account: %s, id: \
                   %s"%(self.acctResponse.name, \
                        self.acctResponse.id))
        
        #Create service offerings with varied planners
        self.svcOfferingFirstFit = createServiceOffering.createServiceOfferingCmd()
        self.svcOfferingFirstFit.name = 'Tiny Instance FirstFit'
        self.svcOfferingFirstFit.displaytext = 'Tiny Instance with FirstFitPlanner'
        self.svcOfferingFirstFit.cpuspeed = 100
        self.svcOfferingFirstFit.cpunumber = 1
        self.svcOfferingFirstFit.memory = 256
        self.svcOfferingFirstFit.deploymentplanner = 'FirstFitPlanner'
        self.svcOfferingFirstFitResponse = self.apiClient.createServiceOffering(self.svcOfferingFirstFit)
        
        self.debug("successfully created serviceofferring name: %s, id: \
				   %s, deploymentPlanner: %s"%(self.svcOfferingFirstFitResponse.name, \
						self.svcOfferingFirstFitResponse.id,self.svcOfferingFirstFitResponse.deploymentplanner))
        
		#Create service offerings with varied planners
        self.svcOfferingUserDispersing = createServiceOffering.createServiceOfferingCmd()
        self.svcOfferingUserDispersing.name = 'Tiny Instance UserDispersing'
        self.svcOfferingUserDispersing.displaytext = 'Tiny Instance with UserDispersingPlanner'
        self.svcOfferingUserDispersing.cpuspeed = 100
        self.svcOfferingUserDispersing.cpunumber = 1
        self.svcOfferingUserDispersing.memory = 256
        self.svcOfferingUserDispersing.deploymentplanner = 'FirstFitPlanner'
        self.svcOfferingUserDispersingResponse = self.apiClient.createServiceOffering(self.svcOfferingUserDispersing)
        
        self.debug("successfully created serviceofferring name: %s, id: \
                   %s, deploymentPlanner: %s"%(self.svcOfferingUserDispersingResponse.name, \
                        self.svcOfferingUserDispersingResponse.id,self.svcOfferingUserDispersingResponse.deploymentplanner))

    def test_DeployVm(self):
        """
        Let's start by defining the attributes of our VM that we will be
        deploying on CloudStack. We will be assuming a single zone is available
        and is configured and all templates are Ready

        The hardcoded values are used only for brevity.
        """
        deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVmCmd.zoneid = 1
        deployVmCmd.account = self.acct.account
        deployVmCmd.domainid = self.acct.domainid
        deployVmCmd.templateid = 5                   #For default template- CentOS 5.6(64 bit)
        deployVmCmd.serviceofferingid = self.svcOfferingFirstFitResponse.id

        deployVmResponse = self.apiClient.deployVirtualMachine(deployVmCmd)
        self.debug("VM %s was deployed in the job %s"%(deployVmResponse.id, deployVmResponse.jobid))

        # At this point our VM is expected to be Running. Let's find out what
        # listVirtualMachines tells us about VMs in this account

        listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
        listVmCmd.id = deployVmResponse.id
        listVmResponse = self.apiClient.listVirtualMachines(listVmCmd)

        self.assertNotEqual(len(listVmResponse), 0, "Check if the list API \
                            returns a non-empty response")

        vm1 = listVmResponse[0]

        self.assertEqual(vm1.id, deployVmResponse.id, "Check if the VM returned \
                         is the same as the one we deployed")
        self.assertEqual(vm1.state, "Running", "Check if VM has reached \
                         a state of running")
        

        deployVm2Cmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVm2Cmd.zoneid = 1
        deployVm2Cmd.account = self.acct.account
        deployVm2Cmd.domainid = self.acct.domainid
        deployVm2Cmd.templateid = 5                   #For default template- CentOS 5.6(64 bit)
        deployVm2Cmd.serviceofferingid = self.svcOfferingFirstFitResponse.id

        deployVm2Response = self.apiClient.deployVirtualMachine(deployVm2Cmd)
        self.debug("VM %s was deployed in the job %s"%(deployVm2Response.id, deployVm2Response.jobid))

        # At this point our VM is expected to be Running. Let's find out what
        # listVirtualMachines tells us about VMs in this account

        listVm2Cmd = listVirtualMachines.listVirtualMachinesCmd()
        listVm2Cmd.id = deployVm2Response.id
        listVm2Response = self.apiClient.listVirtualMachines(listVm2Cmd)
        self.assertNotEqual(len(listVm2Response), 0, "Check if the list API \
                            returns a non-empty response")
        vm2 = listVm2Response[0]
        self.assertEqual(vm2.id, deployVm2Response.id, "Check if the VM returned \
                         is the same as the one we deployed")
        self.assertEqual(vm2.state, "Running", "Check if VM has reached \
                         a state of running")


    def tearDown(self):                               # Teardown will delete the Account as well as the VM once the VM reaches "Running" state
        """
        And finally let us cleanup the resources we created by deleting the
        account. All good unittests are atomic and rerunnable this way
        """
        deleteAcct = deleteAccount.deleteAccountCmd()
        deleteAcct.id = self.acctResponse.id
        self.apiClient.deleteAccount(deleteAcct)
        deleteSvcOfferingFirstFit = deleteServiceOffering.deleteServiceOfferingCmd()
        deleteSvcOfferingFirstFit.id = self.svcOfferingFirstFitResponse.id
        self.apiClient.deleteServiceOffering(deleteSvcOfferingFirstFit);
        deleteSvcOfferingUserDispersing = deleteServiceOffering.deleteServiceOfferingCmd()
        deleteSvcOfferingUserDispersing.id = self.svcOfferingUserDispersingResponse.id
        self.apiClient.deleteServiceOffering(deleteSvcOfferingUserDispersing);
        