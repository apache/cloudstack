#!/usr/bin/python

import json
import os
import logging
import cs_ip

from pprint import pprint

class updateDataBag:

    qFile = {}
    fpath = ''
    bdata  = { }
    DPATH = "/var/chef/data_bags/vr"

    def __init__(self,qFile):
        self.qFile = qFile
        self.process()

    def save(self, dbag):
        try:
            handle = open(self.fpath, 'w')
        except IOError:
            logging.error("Could not write data bag %s", self.qFile.type)
        else:
            logging.debug("Writing data bag type %s", self.qFile.type)
        jsono = json.dumps(dbag, indent=4, sort_keys=True)
        handle.write(jsono)

    def load(self, key):
        data = self.bdata
        if not os.path.exists(self.DPATH):
           os.makedirs(self.DPATH)
        self.fpath = self.DPATH + '/' + key + '.json'
        try:
            handle = open(self.fpath)
        except IOError:
            logging.debug("Creating data bag type %s for key %s", self.qFile.type, key)
            data.update( { "id": key } )
        else:
            logging.debug("Loading data bag type %s for key %s", self.qFile.type, key)
            data = json.load(handle)
            handle.close()
        return data

    def process(self):
        if self.qFile.type == 'cl':
           self.transformCL()
           self.qFile.data = self.newData
        dbag = self.load( self.qFile.type )
        logging.info("Command of type %s received", self.qFile.type)
        if self.qFile.type == 'ips':
           dbag = self.processIP(dbag)
        self.save(dbag)
  
    def processIP(self, dbag):
        for ip in self.qFile.data["ip_address"]:
            dbag = cs_ip.merge(dbag, ip)
        return dbag

    def transformCL(self):
        # Convert the ip stuff to an ip object and pass that into cs_ip_merge
        # "eth0ip": "192.168.56.32",
        # "eth0mask": "255.255.255.0",
        self.newData = []
        self.qFile.setType("ips")
        self.processCLItem('0')
        self.processCLItem('1')

    def processCLItem(self, num):
        key = 'eth' + num + 'ip'
        dp  = {}
        if(key in self.qFile.data['cmdline']):
           dp['public_ip']    = self.qFile.data['cmdline'][key]
           dp['vlan_netmask'] = self.qFile.data['cmdline']['eth' + num + 'mask']
           dp['source_nat'] = False
           dp['add'] = True
           dp['one_to_one_nat'] = False
           if('localgw' in self.qFile.data['cmdline']):
               dp['vlan_gateway'] = self.qFile.data['cmdline']['localgw']
           else:
               dp['vlan_gateway'] = 'None'
           dp['nic_dev_id'] = num
           self.newData.append(dp)
            
class loadQueueFile:

    fileName = ''
    dpath = "/etc/cloudstack"
    data = {}

    def load(self):
        fn = self.dpath + '/' + self.fileName
        try:
            handle = open(fn)
        except IOError:
            logging.error("Could not open %s", fn)
        else:
            self.data = json.load(handle)
            self.type = self.data["type"]
            handle.close()
            proc = updateDataBag(self)

    def setFile(self, name):
        self.fileName = name

    def getType(self):
        return self.type
    
    def getData(self):
        return self.data 

    def setPath(self, path):
        self.dpath = path

