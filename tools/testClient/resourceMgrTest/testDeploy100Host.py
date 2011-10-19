'''
Created on Oct 18, 2011

@author: frank
'''
from cloudstackTestCase import *
from cloudstackAPI import *
import uuid

class TestDeploy100Hosts(cloudstackTestCase):
    def test_deploy100Hosts(self):
        apiClient = self.testClient.getApiClient()
        addHostCmd = addHost.addHostCmd()
        addHostCmd.hypervisor = "simulator"
        addHostCmd.clusterid = 1
        addHostCmd.zoneid = 1
        addHostCmd.podid = 1
        addHostCmd.url = "http://sim/10.223.63.1"
        addHostCmd.username = "placeholder"
        addHostCmd.password = "placeholder"
        addHostResponce = apiClient.addHost(addHostCmd)
        return addHostResponce.id