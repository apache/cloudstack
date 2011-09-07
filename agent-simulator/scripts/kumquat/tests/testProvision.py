#!/usr/bin/env python
try:
    import unittest2 as unittest
except ImportError:
    import unittest

import random
import hashlib
from cloudstackTestCase import *

class Provision(cloudstackTestCase):
    '''
    '''

    so = '10' #default

    def setUp(self):
        pass


    def tearDown(self):
        pass

    @unittest.skip("already done")
    def test_createAccounts(self, numberOfAccounts=850):
        '''
        Create a bunch of user accounts
        '''
        mdf = hashlib.md5()
        mdf.update('password')
        mdf_pass = mdf.hexdigest()
        api = self.testClient.getApiClient()
        for i in range(1, numberOfAccounts + 1):
            acct = createAccount.createAccountCmd()
            acct.accounttype = 0
            acct.firstname = 'user' + str(i)
            acct.lastname = 'user' + str(i)
            acct.password = mdf_pass
            acct.username = 'user' + str(i)
            acct.email = 'user@example.com'
            acct.account = 'user' + str(i)
            acct.domainid = 1
            acctResponse = api.createAccount(acct)
            self.debug(acctResponse) 


    def test_setupServiceOffering(self):
        socreate = createServiceOffering.createServiceOfferingCmd()
        socreate.cpunumber = 1
        socreate.cpuspeed = 100
        socreate.displaytext = 'Sample SO'
        socreate.memory = 128
        socreate.name = 'Sample SO'
        api = self.testClient.getApiClient()
        soresponse = api.createServiceOffering(socreate)
        self.so = soresponse.id

    def deployCmd(self, acct):
        deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVmCmd.zoneid = 1
        deployVmCmd.hypervisor='Simulator'
        deployVmCmd.account=acct
        deployVmCmd.domainid=1
        deployVmCmd.templateid=2
        deployVmCmd.serviceofferingid=self.so
        return deployVmCmd


    def test_stressDeploy(self):
        '''
            Deploy 5 Vms in each account
        '''
        api = self.testClient.getApiClient()
        for acct in range(122, 850):
            [api.deployVirtualMachine(self.deployCmd('user'+str(acct))) for x in range(0, 5)]


    def deployN(self,nargs=300,batchsize=0):
        '''
        Deploy Nargs number of VMs concurrently in batches of size {batchsize}.
        When batchsize is 0 all Vms are deployed in one batch
        VMs will be deployed in 5:2:6 ratio
        '''
        cmds = []

        if batchsize == 0:
            self.testClient.submitCmdsAndWait(cmds)
        else:
            while len(z) > 0:
                try:
                    newbatch = [cmds.pop() for b in range(batchsize)] #pop batchsize items
                    self.testClient.submitCmdsAndWait(newbatch)
                except IndexError:
                    break
