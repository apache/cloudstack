'''
Created on Oct 18, 2011

@author: frank
'''
from cloudstackTestCase import *
from cloudstackAPI import *
import uuid
import threading
import random
import time

class Task(threading.Thread):
    def __init__(self, func, param=None):
        super(Task, self).__init__()
        self.func = func
        self.param = param
    
    def run(self):
        self.func(self.param)
        
    def doTask(self):
        self.start()

class TestDeploy100Hosts(cloudstackTestCase):
    hosts = []
    def deployHost(self, url):
        apiClient = self.testClient.getApiClient()
        addHostCmd = addHost.addHostCmd()
        addHostCmd.hypervisor = "simulator"
        addHostCmd.clusterid = 1
        addHostCmd.zoneid = 1
        addHostCmd.podid = 1
        addHostCmd.url = "http://sim/%s"%url
        addHostCmd.username = "placeholder"
        addHostCmd.password = "placeholder"
        addHostResponce = apiClient.addHost(addHostCmd)
        return addHostResponce[0].id
    
    def randomCancelMaintenance(self):
        def run(param):
            while(1):
                try:
                    interval = random.randint(1, 2)
                    time.sleep(interval)
                    if len(self.hosts) == 0:
                        continue
                    
                    index = random.randint(0, len(self.hosts)-1)
                    hostId = self.hosts[index]
                    apiClient = self.testClient.getApiClient()
                    cMaintainCmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
                    cMaintainCmd.id = hostId
                    response = apiClient.cancelHostMaintenance(cMaintainCmd)
                    id = response.id
                    print "Host %s cancelled maintenance mode" % id
                except Exception, e:
                    print e
                
        t = Task(run)
        t.doTask()
        
    def randomEnterMaintenance(self):
        def run(param):
            while(1):
                try:
                    interval = random.randint(1, 2)
                    time.sleep(interval)
                    if len(self.hosts) == 0:
                        continue
                    index = random.randint(0, len(self.hosts)-1)
                    hostId = self.hosts[index]
                    apiClient = self.testClient.getApiClient()
                    maintainCmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
                    maintainCmd.id = hostId
                    response = apiClient.prepareHostForMaintenance(maintainCmd)
                    id = response.id
                    print "Host %s entered maintenance mode" % id
                except Exception, e:
                    print e
            
        t = Task(run)
        t.doTask()

        
    def test_deploy100Hosts(self):
        #for i in range(200):
            #self.hosts.append(self.deployHost(i))
        for i in range(200):
            self.hosts.append(i)
        self.randomEnterMaintenance()
        self.randomCancelMaintenance()
        while(1): time.sleep(10000)
            
