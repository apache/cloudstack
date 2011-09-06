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
    def setUp(self):
        pass


    def tearDown(self):
        pass


    def test_createAccounts(self, numberOfAccounts=5):
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
            self.debug("successfully created account: %s, user: %s, id: %s"%(acctResponse.account, acctResponse.username, acctResponse.id))


    def deployCmd(self, account):
        deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVmCmd.zoneid = 1
        deployVmCmd.hypervisor='Simulator'
        deployVmCmd.account=account
        deployVmCmd.domainid=1
        deployVmCmd.templateid=10
        deployVmCmd.serviceofferingid=7
        return deployVmCmd


    def listVmsInAccountCmd(self, acct):
        api = self.testClient.getApiClient()
        listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
        listVmCmd.account = acct
        listVmCmd.zoneid = 1
        listVmCmd.domainid = 1
        listVmResponse = api.listVirtualMachines(listVmCmd)
        self.debug(listVmResponse)
        return listVmResponse


    def destroyVmCmd(self, key):
        api = self.testClient.getApiClient()
        destroyVmCmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        destroyVmCmd.id = key
        api.destroyVirtualMachine(destroyVmCmd)


    def test_stressDeploy(self):
        '''
            Deploy 20 Vms in each account
        '''
        api = self.testClient.getApiClient()
        for acct in range(1, 5):
            [api.deployVirtualMachine(self.deployCmd('user'+str(acct))) for x in range(0,20)]

    def test_stressDestroy(self):
        '''
            Cleanup all Vms in every account
        '''
        api = self.testClient.getApiClient()
        for acct in range(1, 6):
            for vm in self.listVmsInAccountCmd('user'+str(acct)):
                self.destroyVmCmd(vm.id)

    def test_combineStress(self):
        for i in range(0, 5):
            self.test_stressDestroy()
            self.test_stressDeploy()

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

