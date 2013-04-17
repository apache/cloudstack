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
""" BVT tests for Primary Storage
"""
import marvin
from marvin import cloudstackTestCase
from marvin.cloudstackTestCase import *

import unittest
import hashlib
import random


class TestUpdatePhysicalNetwork(cloudstackTestCase):
    """
    This test updates the existing physicalnetwork with a new vlan range.
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
        self.acct.firstname = 'bharat'
        self.acct.lastname = 'kumar'                 #What's up doc?
        self.acct.password = mdf_pass                #The md5 hashed password string
        self.acct.username = 'bharat'
        self.acct.email = 'bharat@kumar.com'
        self.acct.account = 'bharat'
        self.acct.domainid = 1                       #The default ROOT domain
        self.acctResponse = self.apiClient.createAccount(self.acct)
        # using the default debug logger of the test framework
        self.debug("successfully created account: %s, user: %s, id: \
                   %s"%(self.acctResponse.account.account, \
                        self.acctResponse.account.username, \
                        self.acctResponse.account.id))

    def test_UpdatePhysicalNetwork(self):
        """
        Let's start by defining the attributes of our VM that we will be
        deploying on CloudStack. We will be assuming a single zone is available
        and is configured and all templates are Ready

        The hardcoded values are used only for brevity.
        """
        listPhysicalNetworksCmd = listPhysicalNetworks.listPhysicalNetworksCmd()
        listPhysicalNetworksResponse = self.apiClient.listPhysicalNetworks(listPhysicalNetworksCmd)
        
        self.assertNotEqual(len(listPhysicalNetworksResponse), 0, "Check if the list API \
                           returns a non-empty response")
        
        networkid = listPhysicalNetworksResponse[0].id
        updatePhysicalNetworkCmd = updatePhysicalNetwork.updatePhysicalNetworkCmd()
        updatePhysicalNetworkCmd.id = networkid
        updatePhysicalNetworkCmd.vlan = "4090-4091"
        updatePhysicalNetworkResponse = self.apiClient.updatePhysicalNetwork(updatePhysicalNetworkCmd)
        self.assertNotEqual((updatePhysicalNetworkResponse.len), 0, "Check if the list API \
                           returns a non-empty response")
        
        updatePhysicalNetworkCmd = updatePhysicalNetwork.updatePhysicalNetworkCmd()
        updatePhysicalNetworkCmd.id = networkid
        updatePhysicalNetworkCmd.vlan = "4092-4096"
        updatePhysicalNetworkResponse = self.apiClient.updatePhysicalNetwork(updatePhysicalNetworkCmd)
        self.assertNotEqual((updatePhysicalNetworkResponse.len), 0, "Check if the list API \
                                returns a non-empty response")

        vlanranges= updatePhysicalNetworkResponse.vlan
        range = ""
        vlanranges = vlanranges.split(";")
        for vlan in vlanranges:
            if (vlan == "4090-4096"):
               range = vlan
    
        self.assertEqual(range, "4090-4096", "check if adding the range is successful")

        updatePhysicalNetworkCmd = updatePhysicalNetwork.updatePhysicalNetworkCmd()
        updatePhysicalNetworkCmd.id = networkid
        updatePhysicalNetworkCmd.removevlan = "4090-4096"
        updatePhysicalNetworkResponse = self.apiClient.updatePhysicalNetwork(updatePhysicalNetworkCmd)
        self.assertNotEqual((updatePhysicalNetworkResponse.len), 0, "Check if the list API \
                    returns a non-empty response")

        vlanranges= updatePhysicalNetworkResponse.vlan
        range = ""
        vlanranges = vlanranges.split(";")

        for vlan in vlanranges:
            if (vlan == "4090-4096"):
               range = vlan
    
    
        self.assertEqual(range, "", "check if removing the range is successful")

    
    def tearDown(self):                               # Teardown will delete the Account as well as the VM once the VM reaches "Running" state
        """
        And finally let us cleanup the resources we created by deleting the
        account. All good unittests are atomic and rerunnable this way
        """
        deleteAcct = deleteAccount.deleteAccountCmd()
        deleteAcct.id = self.acctResponse.account.id
        self.apiClient.deleteAccount(deleteAcct)

