import json
import os
import random
import sys

class ZoneCreator:
    """Creates a zone (and a pod and cluster for now)"""
    def __init__(self, api, zonenum, dns1='192.168.10.254', 
                 dns2='192.168.10.253', internaldns='192.168.10.254'):
        self._api = api
        self._zonenum = zonenum
        self._zonename = "ZONE%04d"%zonenum
        self._dns1 = dns1
        self._dns2 = dns2
        self._internaldns = internaldns
    
    def create(self):
        jsonresult = self._api.GET({'command': 'createZone', 'networktype':'Basic', 
                      'name':self._zonename, 'dns1':self._dns1, 'dns2':self._dns2,
                      'internaldns1':self._internaldns})
        if  jsonresult is  None:
           print "Failed to create zone"
           return 0
        jsonobj = json.loads(jsonresult)
        self._zoneid = jsonobj['createzoneresponse']['zone']['id']
        self._zonetoken = jsonobj['createzoneresponse']['zone']['zonetoken']
        print "Zone %s is created"%self._zonename
        print "zone=%s"%self._zonetoken
        #self.createPod()
        return self._zoneid


    def createPod(self):
        self._podname = "POD%04d"%self._zonenum
        self._clustername = "CLUSTER%04d"%self._zonenum
        jsonresult = self._api.GET({'command': 'createPod', 'zoneId':self._zoneid,
                          'name':self._podname, 'gateway':'192.168.1.1', 'netmask':'255.255.255.0',
                          'startIp':'192.168.1.100', 'endIp':'192.168.1.150'})
        if  jsonresult is  None:
           print "Failed to create pod"
           return 2
        jsonobj = json.loads(jsonresult)
        podid = jsonobj['createpodresponse']['pod']['id']
        jsonresult = self._api.GET({'command': 'addCluster', 'zoneId':self._zoneid,
                              'clustername':self._clustername, 'podId':podid, 'hypervisor':'KVM',
                              'clustertype':'CloudManaged'})
        if  jsonresult is  None:
           print "Failed to create cluster"
           return 3
        jsonobj = json.loads(jsonresult)
        clusterid = jsonobj['addclusterresponse']['cluster'][0]['id']
        print "pod=%s"%podid
        print "cluster=%s"%clusterid
        
