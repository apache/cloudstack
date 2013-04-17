#!/usr/bin/env python

import marvin
from marvin import cloudstackTestCase
from marvin.cloudstackTestCase import *
from marvin.integration.lib.base import *

import unittest
import hashlib
import random
import os
import string

class TestDeployVmWithUserData(cloudstackTestCase):
    """
    This test deploys a virtual machine into a user account
    using the small service offering and builtin template
    """
    def setUp(self):
        password = "password"

        self.apiClient = self.testClient.getApiClient() #Get ourselves an API client

        self.acct = createAccount.createAccountCmd() #The createAccount command
        self.acct.accounttype = 0                    #We need a regular user. admins have accounttype=1
        self.acct.firstname = 'firstname'
        self.acct.lastname = 'lastname'
        self.acct.password = password
        self.acct.username = 'user1'
        self.acct.email = 'user1@user.com'
        self.acct.account = 'user1'
        self.acct.domainid = 1                       #The default ROOT domain
        self.acctResponse = self.apiClient.createAccount(self.acct)

        self.debug("Successfully created account: %s, user: %s, id: \
                   %s"%(self.acctResponse.account.account, \
                        self.acctResponse.account.username, \
                        self.acctResponse.account.id))

        # Generate userdata of 2500 bytes. This is larger than the 2048 bytes limit.
        # CS however allows for upto 4K bytes in the code. So this must succeed.
        # Overall, the query length must not exceed 4K, for then the json decoder
        # will fail this operation at the marvin client side itself.
        user_data = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(2500))

        self.virtual_machine = {
          "displayname": "Test VM",
          "username": "root",
          "password": "password",
          "ssh_port": 22,
          "hypervisor": 'VMware',
          "privateport": 22,
          "publicport": 22,
          "protocol": 'TCP',
        }
        #self.virtual_machine["userdata"] = base64.b64encode(user_data)
        self.virtual_machine["userdata"] = user_data

    def test_DeployVm(self):
        """
        Let's start by defining the attributes of our VM that we will be
        deploying on CloudStack. We will be assuming a single zone is available
        and is configured and all templates are Ready
        """
        deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVmCmd.zoneid = 1
        deployVmCmd.account = self.acct.account
        deployVmCmd.domainid = self.acct.domainid
        deployVmCmd.templateid = 7
        deployVmCmd.serviceofferingid = 1

        # Userdata is passed in the virtual_machine dictionary.
        deployVmResponse = VirtualMachine.create(
          self.apiClient,
          self.virtual_machine,
          accountid=self.acct.account,
          domainid=self.acct.domainid,
          serviceofferingid=deployVmCmd.serviceofferingid,
          templateid=deployVmCmd.templateid,
          zoneid=deployVmCmd.zoneid
        )

        # At this point our VM is expected to be Running. Let's find out what
        # listVirtualMachines tells us about VMs in this account

        listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
        listVmCmd.id = deployVmResponse.id
        listVmResponse = self.apiClient.listVirtualMachines(listVmCmd)

        self.assertNotEqual(len(listVmResponse), 0, "Check if the list API \
                            returns a non-empty response")

        vm = listVmResponse[0]

        self.assertEqual(vm.id, deployVmResponse.id, "Check if the VM returned \
                         is the same as the one we deployed")


        self.assertEqual(vm.state, "Running", "Check if VM has reached \
                         a state of running")

    def tearDown(self):
        """
        Delete the account created. This will clear the VM belonging to that account as well.
        """
        deleteAcct = deleteAccount.deleteAccountCmd()
        deleteAcct.id = self.acctResponse.account.id
        self.apiClient.deleteAccount(deleteAcct)
