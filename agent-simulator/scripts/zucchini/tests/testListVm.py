#!/usr/bin/env python
try:
    import unittest2 as unittest
except ImportError:
    import unittest

import timeit
import random
from cloudstackAPI import *
from cloudstackTestCase import *


class ListVmTests(cloudstackTestCase):
    '''
    List Virtual Machine tests
    '''

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def listAllVm(self):
        numVms = 0
        api = self.testClient.getApiClient()
        listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
        listVmCmd.account = 'admin'
        listVmCmd.zoneid = 1
        listVmCmd.domainid = 1
        listVmResponse = api.listVirtualMachines(listVmCmd)
        if listVmResponse is not None:
            numVms = len(listVmResponse)

    @unittest.skip("skipping")
    def test_timeListVm(self):
        t = timeit.Timer(self.listAllVm)
        l = t.repeat(50, 50)
