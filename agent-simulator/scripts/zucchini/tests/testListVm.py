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
        def time_listAllVm():
            api = self.testClient.getApiClient()
            listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
            listVmCmd.account = 'admin'
            listVmCmd.zoneid = 1
            listVmCmd.domainid = 1
            numVms = len(api.listVirtualMachines(listVmCmd))

        t = timeit.Timer(time_listAllVm)
        l = t.repeat(5, 5)
        self.debug("Number of VMs: " + str(len(numVms)) + ", time for last 5 listVM calls : " + str(l))
