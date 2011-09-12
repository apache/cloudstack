#!/usr/bin/env python
'''
List Virtual Machine tests
'''
try:
    import unittest2 as unittest
except ImportError:
    import unittest

import timeit
import random
from cloudstackAPI import *
from cloudstackTestCase import *

class ListVmTests(cloudstackTestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_listAllVm(self):
        numVms = 0
        api = self.testClient.getApiClient()
        listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
        listVmCmd.account = 'admin'
        listVmCmd.zoneid = 1
        listVmCmd.domainid = 1
        listVmResponse = api.listVirtualMachines(listVmCmd)
        if listVmResponse is not None:
            numVms = len()

    t = timeit.Timer(test_listAllVm)
    l = t.repeat(5, 5)
    self.debug("Number of VMs: " + str(len(numVms)) + ", time for last 5 listVM calls : " + str(l))
