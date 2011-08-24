#!/usr/bin/env python
'''
Deploy Virtual Machine tests
'''
import unittest
import random
from cloudstackAPI import *

class Provision(unittest.case.TestCase):
    '''
        This should test basic provisioning of virtual machines
            1. Deploy a no-frills VM
            2. Deploy with tags - hosttags
            3. Deploy with/without security groups
    '''

    api = self.testClient.getApiClient()
    solist = {}

    def setUp(self):
        '''setup service offerings with tags'''
        for tag in ['TAG1', 'TAG2', 'TAG3']:
            csocmd = createServiceOffering.createServiceOfferingCmd()
            csocmd.cpunumber = 1
            csocmd.cpuspeed = 2048
            csocmd.displayText =  tag
            csocmd.memory = 7168
            csocmd.name = tag
            csocmd.hosttags = tag
            csocmd.storagetype = 'local'
            csoresponse = self.api.createServiceOffering(csocmd)
            self.debug("Created Service Offering: %s", tag)
            solist[tag]=csoresponse.id
        
        
    def tearDown(self):
        '''  Any cleanup post tests   '''
    
    def test_DeployVMWithHostTags(self):
        '''
            Deploy 3 virtual machines one with each hosttag
        '''
        for k,v in solist:
            deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
            deployVmCmd.zoneid = 1
            deployVmCmd.hypervisor='Simulator'
            deployVmCmd.serviceofferingid=v
            deployVmCmd.account='admin'
            deployVmCmd.domainid=1
            deployVmCmd.templateid=2
            deployVmResponse = self.api.deployVirtualMachine(deployVmCmd)
            self.debug("Deployed VM :%d in job: %d",deployVmResponse.id, deployVmResponse.jobid)


    def deployCmd(self, tag):
            deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
            deployVmCmd.zoneid = 1
            deployVmCmd.hypervisor='Simulator'
            deployVmCmd.serviceofferingid=solist[tag]
            deployVmCmd.account='admin'
            deployVmCmd.domainid=1
            deployVmCmd.templateid=2
            return deployVmCmd
            
    def deployN(self,nargs=300,batchsize=0):
        '''
            Deploy Nargs number of VMs concurrently in batches of size {batchsize}.
            When batchsize is 0 all Vms are deployed in one batch
            VMs will be deployed in 5:2:6 ratio
        '''
        tag1=nargs*5/13
        tag2=nargs*2/13
        tag3=nargs-tag1-tag2
        
        cmds = []
        [cmds.append(deployCmd('TAG1')) for i in range(tag1)]
        [cmds.append(deployCmd('TAG2')) for i in range(tag2)]
        [cmds.append(deployCmd('TAG3')) for i in range(tag3)]
        random.shuffle(cmds) #with mix-and-match of Tags
        
        if batchsize == 0:
            self.testClient.submitCmdsAndWait(cmds)
        else:
            while len(z) > 0:
                try:
                    newbatch = [cmds.pop() for b in range(batchsize)] #pop batchsize items
                    self.testClient.submitCmdsAndWait(newbatch)
                except IndexError:
                    break

    def test_bulkDeploy(self):
        deployN(130,0)
        deployN(nargs=3000,batchsize=100)
            
            
        
            
            



