#!/usr/bin/python

import json
import os
import logging
import cs_ip
import cs_guestnetwork

from pprint import pprint

class dataBag:

    bdata  = { }
    DPATH = "/var/chef/data_bags/vr"

    def load(self):
        data = self.bdata
        if not os.path.exists(self.DPATH):
           os.makedirs(self.DPATH)
        self.fpath = self.DPATH + '/' + self.key + '.json'
        try:
            handle = open(self.fpath)
        except IOError:
            logging.debug("Creating data bag type %s", self.key)
            data.update( { "id": self.key } )
        else:
            logging.debug("Loading data bag type %s",  self.key)
            data = json.load(handle)
            handle.close()
        self.dbag = data

    def save(self, dbag):
        try:
            handle = open(self.fpath, 'w')
        except IOError:
            logging.error("Could not write data bag %s", self.key)
        else:
            logging.debug("Writing data bag type %s", self.key)
        jsono = json.dumps(dbag, indent=4, sort_keys=True)
        handle.write(jsono)

    def getDataBag(self):
        return self.dbag

    def setKey(self, key):
        self.key = key

class updateDataBag:

    qFile = {}
    fpath = ''
    bdata  = { }
    DPATH = "/var/chef/data_bags/vr"

    def __init__(self,qFile):
        self.qFile = qFile
        self.process()

    def process(self):
        if self.qFile.type == 'cl':
           self.transformCL()
           self.qFile.data = self.newData
        self.db = dataBag()
        self.db.setKey( self.qFile.type )
        dbag = self.db.load( )
        logging.info("Command of type %s received", self.qFile.type)

        if self.qFile.type == 'ips':
           dbag = self.processIP(self.db.getDataBag())
        if self.qFile.type == 'guestnetwork':
           dbag = self.processGuestNetwork(self.db.getDataBag())
        self.db.save(dbag)
  
    def processGuestNetwork(self, dbag):
        d = self.qFile.data
        dp = {}
        dp['public_ip']      = d['router_guest_ip']
        dp['netmask']        = d['router_guest_netmask']
        dp['source_nat']     = False
        dp['add']            = d['add']
        dp['one_to_one_nat'] = False
        dp['gateway']        = d['router_guest_gateway']
        dp['nic_dev_id']     = d['device'][3]
        qf = loadQueueFile()
        qf.load({ 'ip_address' : [ dp ], 'type' : 'ips'})
        return cs_guestnetwork.merge(dbag, self.qFile.data)

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
        self.processCLItem('2')

    def processCLItem(self, num):
        key = 'eth' + num + 'ip'
        dp  = {}
        if(key in self.qFile.data['cmdline']):
           dp['public_ip']    = self.qFile.data['cmdline'][key]
           dp['netmask'] = self.qFile.data['cmdline']['eth' + num + 'mask']
           dp['source_nat'] = False
           dp['add'] = True
           dp['one_to_one_nat'] = False
           if('localgw' in self.qFile.data['cmdline']):
               dp['gateway'] = self.qFile.data['cmdline']['localgw']
           else:
               dp['gateway'] = 'None'
           dp['nic_dev_id'] = num
           self.newData = { 'ip_address' : [ dp ], 'type' : 'ips'}
            
class loadQueueFile:

    fileName = ''
    dpath = "/etc/cloudstack"
    data = {}

    def load(self, data):
        if data is not None:
            self.data = data
            self.type = self.data["type"]
            proc = updateDataBag(self)
            return
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

