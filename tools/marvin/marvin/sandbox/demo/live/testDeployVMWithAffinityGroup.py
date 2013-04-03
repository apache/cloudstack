#!/usr/bin/env python
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



import marvin
from marvin.cloudstackTestCase import *
from marvin.remoteSSHClient import remoteSSHClient 
import hashlib
import random

class TestDeployVmWithAffinityGroup(cloudstackTestCase):
    """
    This test deploys a virtual machine into a user account 
    using the small service offering and builtin template
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
		self.acct.accounttype = 0					#We need a regular user. admins have accounttype=1
		self.acct.firstname = 'test'
		self.acct.lastname = 'user'				 #What's up doc?
		self.acct.password = mdf_pass				#The md5 hashed password string
		self.acct.username = 'testuser'
		self.acct.email = 'testuser@xyz.com'
		self.acct.account = 'testacct'
		self.acct.domainid = 1					   #The default ROOT domain
		self.acctResponse = self.apiClient.createAccount(self.acct)
		# And upon successful creation we'll log a helpful message in our logs
		# using the default debug logger of the test framework
		self.debug("successfully created account: %s, user: %s, id: \
				   %s"%(self.acctResponse.account.account, \
						self.acctResponse.account.username, \
						self.acctResponse.account.id))
		

		self.zone = listZones.listZonesCmd()
		self.zone.uuid = self.apiClient.listZones(self.zone)[0].id

		self.service_offering = listServiceOfferings.listServiceOfferingsCmd()
		self.service_offering.uuid = self.apiClient.listServiceOfferings(self.service_offering)[0].id

		self.template = listTemplates.listTemplatesCmd()
		self.template.templatefilter = 'featured'
		self.template.name = 'CentOS'
		self.template.uuid = self.apiClient.listTemplates(self.template)[0].id        

    def test_DeployVm(self):
        """
        Let's start by defining the attributes of our VM that we will be
        deploying on CloudStack. We will be assuming a single zone is available
        and is configured and all templates are Ready

        The hardcoded values are used only for brevity. 
		
		First create the host anti-affinity group for this account
        """
	createAGCmd = createAffinityGroup.createAffinityGroupCmd()
	createAGCmd.name = 'webvms1'
	createAGCmd.type = 'host anti-affinity'
	createAGCmd.account = self.acct.account
	createAGCmd.domainid = self.acct.domainid
		
	createAGResponse = self.apiClient.createAffinityGroup(createAGCmd)
	self.debug("AffinityGroup %s was created in the job %s"%(createAGResponse.id, createAGResponse.jobid))
    
	
	deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
	deployVmCmd.zoneid = self.zone.uuid
	deployVmCmd.templateid = self.template.uuid #CentOS 5.6 builtin
	deployVmCmd.serviceofferingid = self.service_offering.uuid
	deployVmCmd.account = self.acct.account
	deployVmCmd.domainid = self.acct.domainid
	deployVmCmd.affinitygroupnames=[]
	deployVmCmd.affinitygroupnames.append(str(createAGResponse.name))
	deployVmResponse = self.apiClient.deployVirtualMachine(deployVmCmd)
	self.debug("VM %s was deployed in the job %s"%(deployVmResponse.id, deployVmResponse.jobid))

	# At this point our VM is expected to be Running. Let's find out what
	# listVirtualMachines tells us about VMs in this account

	listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
	listVmCmd.id = deployVmResponse.id
	listVmResponse = self.apiClient.listVirtualMachines(listVmCmd)

	self.assertNotEqual(len(listVmResponse), 0, "Check if the list API \
						returns a non-empty response")

	vm = listVmResponse[0]
	self.assertEqual(vm.state, "Running", "Check if VM has reached Running state in CS")

	VM1hostid = vm.hostid

	#Deploy another VM in same affinity group
	deployVm2Cmd = deployVirtualMachine.deployVirtualMachineCmd()
	deployVm2Cmd.zoneid = self.zone.uuid
	deployVm2Cmd.templateid = self.template.uuid #CentOS 5.6 builtin
	deployVm2Cmd.serviceofferingid = self.service_offering.uuid
	deployVm2Cmd.account = self.acct.account
	deployVm2Cmd.domainid = self.acct.domainid
	deployVm2Cmd.affinitygroupnames=[]
	deployVm2Cmd.affinitygroupnames.append(str(createAGResponse.name))

	deployVm2Response = self.apiClient.deployVirtualMachine(deployVm2Cmd)
	self.debug("VM2 %s was deployed in the job %s"%(deployVm2Response.id, deployVm2Response.jobid))
	
	# At this point our VM is expected to be Running. Let's find out what
	# listVirtualMachines tells us about VMs in this account

	listVm2Cmd = listVirtualMachines.listVirtualMachinesCmd()
	listVm2Cmd.id = deployVm2Response.id
	listVm2Response = self.apiClient.listVirtualMachines(listVm2Cmd)

	self.assertNotEqual(len(listVm2Response), 0, "Check if the list API \
						returns a non-empty response")

	vm2 = listVm2Response[0]
	self.assertEqual(vm2.state, "Running", "Check if VM has reached Running state in CS")

	VM2hostid = vm2.hostid
	
	self.assertNotEqual(VM1hostid, VM2hostid, "The hosts of the 2 VM's in the host anti-affinity group are not different, test failed")                         
	
    def tearDown(self):
    	"""
		And finally let us cleanup the resources we created by deleting the
		account. All good unittests are atomic and rerunnable this way
		"""
        deleteAcct = deleteAccount.deleteAccountCmd()
        deleteAcct.id = self.acctResponse.account.id
        self.apiClient.deleteAccount(deleteAcct)
        self.testClient.close()
