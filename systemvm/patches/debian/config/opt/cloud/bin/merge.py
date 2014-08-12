#!/usr/bin/python

import json
import os
import time
import logging
import cs_ip
import cs_guestnetwork
import cs_cmdline
import cs_vmp

from pprint import pprint

class dataBag:

    bdata  = { }
    DPATH = "/etc/cloudstack"

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
    DPATH = "/etc/cloudstack"

    def __init__(self,qFile):
        self.qFile = qFile
        self.process()

    def process(self):
        self.db = dataBag()
        self.db.setKey( self.qFile.type )
        dbag = self.db.load( )
        logging.info("Command of type %s received", self.qFile.type)

        if self.qFile.type == 'ips':
           dbag = self.processIP(self.db.getDataBag())
        if self.qFile.type == 'guestnetwork':
           dbag = self.processGuestNetwork(self.db.getDataBag())
        if self.qFile.type == 'cmdline':
           dbag = self.processCL(self.db.getDataBag())
        if self.qFile.type == 'cmdline':
           dbag = self.processCL(self.db.getDataBag())
        if self.qFile.type == 'vmpassword':
           dbag = self.processVMpassword(self.db.getDataBag())
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
        dp['nw_type']        = 'guest'
        qf = loadQueueFile()
        qf.load({ 'ip_address' : [ dp ], 'type' : 'ips'})
        if 'domain_name' not in d.keys() or d['domain_name'] == '':
            d['domain_name'] = "cloudnine.internal"
        return cs_guestnetwork.merge(dbag, self.qFile.data)

    def processVMpassword(self, dbag):
        return cs_vmp.merge(dbag, self.qFile.data)

    def processIP(self, dbag):
        for ip in self.qFile.data["ip_address"]:
            dbag = cs_ip.merge(dbag, ip)
        return dbag

    def processCL(self, dbag):
        # Convert the ip stuff to an ip object and pass that into cs_ip_merge
        # "eth0ip": "192.168.56.32",
        # "eth0mask": "255.255.255.0",
        self.newData = []
        self.processCLItem('0')
        self.processCLItem('1')
        self.processCLItem('2')
        return cs_cmdline.merge(dbag, self.qFile.data)

    def processCLItem(self, num):
        key = 'eth' + num + 'ip'
        dp  = {}
        if(key in self.qFile.data['cmd_line']):
           dp['public_ip']    = self.qFile.data['cmd_line'][key]
           dp['netmask'] = self.qFile.data['cmd_line']['eth' + num + 'mask']
           dp['source_nat'] = False
           dp['add'] = True
           dp['one_to_one_nat'] = False
           if('localgw' in self.qFile.data['cmd_line']):
               dp['gateway'] = self.qFile.data['cmd_line']['localgw']
           else:
               dp['gateway'] = 'None'
           dp['nic_dev_id'] = num
           dp['nw_type']    = 'control'
           qf = loadQueueFile()
           qf.load({ 'ip_address' : [ dp ], 'type' : 'ips'})
            
class loadQueueFile:

    fileName = ''
    configCache = "/var/cache/cloud"
    keep = True
    data = {}

    def load(self, data):
        if data is not None:
            self.data = data
            self.type = self.data["type"]
            proc = updateDataBag(self)
            return
        fn = self.configCache + '/' + self.fileName
        try:
            handle = open(fn)
        except IOError:
            logging.error("Could not open %s", fn)
        else:
            self.data = json.load(handle)
            self.type = self.data["type"]
            handle.close()
            if self.keep:
                self.__moveFile(fn, self.configCache + "/processed")
            else:
                os.remove(fn)
            proc = updateDataBag(self)

    def setFile(self, name):
        self.fileName = name

    def getType(self):
        return self.type
    
    def getData(self):
        return self.data 

    def setPath(self, path):
        self.configCache = path

    def __moveFile(self, origPath, path):
        if not os.path.exists(path):
            os.makedirs(path)
        timestamp = str(int(round(time.time())))
        os.rename(origPath, path + "/" + self.fileName + "." + timestamp)

