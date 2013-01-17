# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import json
import os
from optparse import OptionParser
import jsonHelper

class managementServer():
    def __init__(self):
        self.mgtSvrIp = None
        self.port = 8096
        self.apiKey = None
        self.securityKey = None
        
class dbServer():
    def __init__(self):
        self.dbSvr = None
        self.port = 3306
        self.user = "cloud"
        self.passwd = "cloud"
        self.db = "cloud"

class configuration():
    def __init__(self):
        self.name = None
        self.value = None

class logger():
    def __init__(self):
        '''TestCase/TestClient'''
        self.name = None
        self.file = None

class cloudstackConfiguration():
    def __init__(self):
        self.zones = []
        self.mgtSvr = []
        self.dbSvr = None
        self.globalConfig = []
        self.logger = []

class zone():
    def __init__(self):
        self.dns1 = None
        self.internaldns1 = None
        self.name = None
        '''Basic or Advanced'''
        self.networktype = None
        self.dns2 = None
        self.internaldns2 = None
        self.securitygroupenabled = None
        self.localstorageenabled = None
        '''default public network, in advanced mode'''
        self.ipranges = []
        self.physical_networks = []
        self.pods = []
        self.secondaryStorages = []
        
class traffictype():
    def __init__(self, typ, labeldict=None):
        self.typ = typ #Guest/Management/Public
        if labeldict:
            self.xen = labeldict['xen'] if 'xen' in labeldict.keys() else None
            self.kvm = labeldict['kvm'] if 'kvm' in labeldict.keys() else None
            self.vmware = labeldict['vmware'] if 'vmware' in labeldict.keys() else None
            self.simulator = labeldict['simulator'] if 'simulator' in labeldict.keys() else None
        #{
        #    'xen' : 'cloud-xen',
        #    'kvm' : 'cloud-kvm',
        #    'vmware' : 'cloud-vmware'
        #}

class pod():
    def __init__(self):
        self.gateway = None
        self.name = None
        self.netmask = None
        self.startip = None
        self.endip = None
        self.zoneid = None
        self.clusters = []
        '''Used in basic network mode'''
        self.guestIpRanges = []

class cluster():
    def __init__(self):
        self.clustername = None
        self.clustertype = None
        self.hypervisor = None
        self.zoneid = None
        self.podid = None
        self.password = None
        self.url = None
        self.username = None
        self.hosts = []
        self.primaryStorages = []
  
class host():
    def __init__(self):
        self.hypervisor = None
        self.password = None
        self.url = None
        self.username = None
        self.zoneid = None
        self.podid = None
        self.clusterid = None
        self.clustername = None
        self.cpunumber = None
        self.cpuspeed = None
        self.hostmac = None
        self.hosttags = None
        self.memory = None
        
class physical_network():
    def __init__(self):
        self.name = None
        self.tags = []
        self.traffictypes = []
        self.broadcastdomainrange = 'Zone'
        self.vlan = None
        '''enable default virtual router provider'''
        vrouter = provider()
        vrouter.name = 'VirtualRouter'
        self.providers = [vrouter]

class provider():
    def __init__(self, name=None):
        self.name = name
        self.state = None
        self.broadcastdomainrange = 'ZONE'
        self.zoneid = None
        self.servicelist = []
        self.devices = []

class network():
    def __init__(self):
        self.displaytext = None
        self.name = None
        self.zoneid = None
        self.acltype = None
        self.domainid = None
        self.networkdomain = None
        self.networkofferingid = None
        self.ipranges = []
      
class iprange():
    def __init__(self):
        '''tagged/untagged'''
        self.gateway = None
        self.netmask = None
        self.startip = None
        self.endip = None
        self.vlan = None
        '''for account specific '''
        self.account = None
        self.domain = None

class primaryStorage():
    def __init__(self):
        self.name = None
        self.url = None

class secondaryStorage():
    def __init__(self):
        self.url = None

class s3():
    def __init__(self):
        self.accesskey = None
        self.secretkey = None
        self.bucket = None
        self.endpoint = None
        self.sockettimeout = None
        self.connectiontimeout = None
        self.maxerrorrety = None
        self.usehttps = None

class netscaler():
    def __init__(self, hostname=None, username='nsroot', password='nsroot'):
        self.hostname = hostname
        self.username = username
        self.password = password
        self.networkdevicetype = 'NetscalerVPXLoadBalancer'
        self.publicinterface = '1/1'
        self.privateinterface = '1/1'
        self.numretries = '2'
        self.lbdevicecapacity = '50'
        self.lbdevicededicated = 'false'

    def getUrl(self):
        return repr(self)

    def __repr__(self):
        req = zip(self.__dict__.keys(), self.__dict__.values())
        return self.hostname+"?" + "&".join(["=".join([r[0], r[1]]) \
                                                     for r in req])

class srx():
    def __init__(self, hostname=None, username='root', password='admin'):
        self.hostname = hostname
        self.username = username
        self.password = password
        self.networkdevicetype = 'JuniperSRXFirewall'
        self.publicinterface = '1/1'
        self.privateinterface = '1/1'
        self.numretries = '2'
        self.fwdevicededicated = 'false'
        self.timeout = '300'
        self.publicnetwork = 'untrusted'
        self.privatenetwork = 'trusted'

    def getUrl(self):
        return repr(self)

    def __repr__(self):
        req = zip(self.__dict__.keys(), self.__dict__.values())
        return self.hostname+"?" + "&".join(["=".join([r[0], r[1]]) \
                                                     for r in req])

class bigip():
    def __init__(self, hostname=None, username='root', password='default'):
        self.hostname = hostname
        self.username = username
        self.password = password
        self.networkdevicetype = 'F5BigIpLoadBalancer'
        self.publicinterface = '1/1'
        self.privateinterface = '1/1'
        self.numretries = '2'
        self.lbdevicededicated = 'false'
        self.lbdevicecapacity = '50'

    def getUrl(self):
        return repr(self)

    def __repr__(self):
        req = zip(self.__dict__.keys(), self.__dict__.values())
        return self.hostname+"?" + "&".join(["=".join([r[0], r[1]]) \
                                                     for r in req])

def getDeviceUrl(obj):
    req = zip(obj.__dict__.keys(), obj.__dict__.values())
    if obj.hostname:
        return "http://" + obj.hostname+"?" + "&".join(["=".join([r[0], r[1]]) \
                                                     for r in req])
    else:
        return None

  
'''sample code to generate setup configuration file'''
def describe_setup_in_basic_mode():
    zs = cloudstackConfiguration()
    
    for l in range(1):
        z = zone()
        z.dns1 = "8.8.8.8"
        z.dns2 = "4.4.4.4"
        z.internaldns1 = "192.168.110.254"
        z.internaldns2 = "192.168.110.253"
        z.name = "test"+str(l)
        z.networktype = 'Basic'
        z.securitygroupenabled = 'True'
    
        #If security groups are reqd
        sgprovider = provider()
        sgprovider.broadcastdomainrange = 'Pod'
        sgprovider.name = 'SecurityGroupProvider'
        
        pn = physical_network()
        pn.name = "test-network"
        pn.traffictypes = [traffictype("Guest"), traffictype("Management")]
        pn.providers.append(sgprovider)
        
        z.physical_networks.append(pn)
        
        '''create 10 pods'''
        for i in range(2):
            p = pod()
            p.name = "test" +str(l) + str(i)
            p.gateway = "192.168.%d.1"%i
            p.netmask = "255.255.255.0"
            p.startip = "192.168.%d.150"%i
            p.endip = "192.168.%d.220"%i
        
            '''add two pod guest ip ranges'''
            for j in range(2):
                ip = iprange()
                ip.gateway = p.gateway
                ip.netmask = p.netmask
                ip.startip = "192.168.%d.%d"%(i,j*20)
                ip.endip = "192.168.%d.%d"%(i,j*20+10)
            
                p.guestIpRanges.append(ip)
        
            '''add 10 clusters'''
            for j in range(2):
                c = cluster()
                c.clustername = "test"+str(l)+str(i) + str(j)
                c.clustertype = "CloudManaged"
                c.hypervisor = "Simulator"
            
                '''add 10 hosts'''
                for k in range(2):
                    h = host()
                    h.username = "root"
                    h.password = "password"
                    memory = 8*1024*1024*1024
                    localstorage=1*1024*1024*1024*1024
                    #h.url = "http://sim/%d%d%d%d/cpucore=1&cpuspeed=8000&memory=%d&localstorage=%d"%(l,i,j,k,memory,localstorage)
                    h.url = "http://sim/%d%d%d%d"%(l,i,j,k)
                    c.hosts.append(h)
                
                '''add 2 primary storages'''
                for m in range(2):
                    primary = primaryStorage()
                    primary.name = "primary"+str(l) + str(i) + str(j) + str(m)
                    #primary.url = "nfs://localhost/path%s/size=%d"%(str(l) + str(i) + str(j) + str(m), size)
                    primary.url = "nfs://localhost/path%s"%(str(l) + str(i) + str(j) + str(m))
                    c.primaryStorages.append(primary)
        
                p.clusters.append(c)
            
            z.pods.append(p)
            
        '''add two secondary'''
        for i in range(5):
            secondary = secondaryStorage()
            secondary.url = "nfs://localhost/path"+str(l) + str(i)
            z.secondaryStorages.append(secondary)
        
        zs.zones.append(z)
    
    '''Add one mgt server'''
    mgt = managementServer()
    mgt.mgtSvrIp = "localhost"
    zs.mgtSvr.append(mgt)
    
    '''Add a database'''
    db = dbServer()
    db.dbSvr = "localhost"
    
    zs.dbSvr = db
    
    '''add global configuration'''
    global_settings = {'expunge.delay': '60',
                       'expunge.interval': '60',
                       'expunge.workers': '3',
                       }
    for k,v in global_settings.iteritems():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        zs.globalConfig.append(cfg)
    
    ''''add loggers'''
    testClientLogger = logger()
    testClientLogger.name = "TestClient"
    testClientLogger.file = "/tmp/testclient.log"
    
    testCaseLogger = logger()
    testCaseLogger.name = "TestCase"
    testCaseLogger.file = "/tmp/testcase.log"
    
    zs.logger.append(testClientLogger)
    zs.logger.append(testCaseLogger)
    
    return zs

def describe_setup_in_eip_mode():
    """
    Setting up an EIP/ELB enabled zone with netscaler provider
    """
    zs = cloudstackConfiguration()
    
    for l in range(1):
        z = zone()
        z.dns1 = "8.8.8.8"
        z.dns2 = "4.4.4.4"
        z.internaldns1 = "192.168.110.254"
        z.internaldns2 = "192.168.110.253"
        z.name = "test"+str(l)
        z.networktype = 'Basic'

        ips = iprange()
        ips.vlan = "49"
        ips.startip = "10.147.49.200"
        ips.endip = "10.147.49.250"
        ips.gateway = "10.147.49.1"
        ips.netmask = "255.255.255.0"
        z.ipranges.append(ips)
        
        #If security groups are reqd
        sgprovider = provider()
        sgprovider.broadcastdomainrange = 'Pod'
        sgprovider.name = 'SecurityGroupProvider'
        
        nsprovider = provider()
        nsprovider.name = 'Netscaler'
        ns = netscaler()
        ns.hostname = '10.147.40.100'
        nsprovider.devices.append(ns)
        
        pn = physical_network()
        pn.name = "test-network"
        pn.traffictypes = [traffictype("Guest", {"xen": "cloud-guest"}), traffictype("Management"), traffictype("Public", { "xen": "cloud-public"})]
        pn.providers.extend([sgprovider, nsprovider])
        z.physical_networks.append(pn)
        
        '''create 10 pods'''
        for i in range(2):
            p = pod()
            p.name = "test" +str(l) + str(i)
            p.gateway = "192.168.%d.1"%i
            p.netmask = "255.255.255.0"
            p.startip = "192.168.%d.150"%i
            p.endip = "192.168.%d.220"%i
        
            '''add two pod guest ip ranges'''
            for j in range(2):
                ip = iprange()
                ip.gateway = p.gateway
                ip.netmask = p.netmask
                ip.startip = "192.168.%d.%d"%(i,j*20)
                ip.endip = "192.168.%d.%d"%(i,j*20+10)
            
                p.guestIpRanges.append(ip)
        
            '''add 10 clusters'''
            for j in range(2):
                c = cluster()
                c.clustername = "test"+str(l)+str(i) + str(j)
                c.clustertype = "CloudManaged"
                c.hypervisor = "Simulator"
            
                '''add 10 hosts'''
                for k in range(2):
                    h = host()
                    h.username = "root"
                    h.password = "password"
                    #h.url = "http://Sim/%d%d%d%d/cpucore=1&cpuspeed=8000&memory=%d&localstorage=%d"%(l,i,j,k,memory,localstorage)
                    h.url = "http://Sim/%d%d%d%d"%(l,i,j,k)
                    c.hosts.append(h)
                
                '''add 2 primary storages'''
                for m in range(2):
                    primary = primaryStorage()
                    primary.name = "primary"+str(l) + str(i) + str(j) + str(m)
                    #primary.url = "nfs://localhost/path%s/size=%d"%(str(l) + str(i) + str(j) + str(m), size)
                    primary.url = "nfs://localhost/path%s"%(str(l) + str(i) + str(j) + str(m))
                    c.primaryStorages.append(primary)
        
                p.clusters.append(c)
            
            z.pods.append(p)
            
        '''add two secondary'''
        for i in range(5):
            secondary = secondaryStorage()
            secondary.url = "nfs://localhost/path"+str(l) + str(i)
            z.secondaryStorages.append(secondary)
        
        zs.zones.append(z)
    
    '''Add one mgt server'''
    mgt = managementServer()
    mgt.mgtSvrIp = "localhost"
    zs.mgtSvr.append(mgt)
    
    '''Add a database'''
    db = dbServer()
    db.dbSvr = "localhost"
    
    zs.dbSvr = db
    
    '''add global configuration'''
    global_settings = {'expunge.delay': '60',
                       'expunge.interval': '60',
                       'expunge.workers': '3',
                       }
    for k,v in global_settings.iteritems():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        zs.globalConfig.append(cfg)
    
    ''''add loggers'''
    testClientLogger = logger()
    testClientLogger.name = "TestClient"
    testClientLogger.file = "/tmp/testclient.log"
    
    testCaseLogger = logger()
    testCaseLogger.name = "TestCase"
    testCaseLogger.file = "/tmp/testcase.log"
    
    zs.logger.append(testClientLogger)
    zs.logger.append(testCaseLogger)
    
    return zs
    
'''sample code to generate setup configuration file'''
def describe_setup_in_advanced_mode():
    zs = cloudstackConfiguration()
    
    for l in range(1):
        z = zone()
        z.dns1 = "8.8.8.8"
        z.dns2 = "4.4.4.4"
        z.internaldns1 = "192.168.110.254"
        z.internaldns2 = "192.168.110.253"
        z.name = "test"+str(l)
        z.networktype = 'Advanced'
        z.guestcidraddress = "10.1.1.0/24"
        z.vlan = "100-2000"
        
        pn = physical_network()
        pn.name = "test-network"
        pn.traffictypes = [traffictype("Guest"), traffictype("Management"), traffictype("Public")]

        vpcprovider = provider('VpcVirtualRouter')

        nsprovider = provider('Netscaler')
        nsprovider.devices.append(netscaler(hostname='10.147.40.100'))

        srxprovider = provider('JuniperSRX')
        srxprovider.devices.append(srx(hostname='10.147.40.3'))

        f5provider = provider('F5BigIp')
        f5provider.devices.append(bigip(hostname='10.147.40.3'))

        pn.providers.extend([vpcprovider, nsprovider, srxprovider, f5provider])
        z.physical_networks.append(pn)
    
        '''create 10 pods'''
        for i in range(2):
            p = pod()
            p.name = "test" +str(l) + str(i)
            p.gateway = "192.168.%d.1"%i
            p.netmask = "255.255.255.0"
            p.startip = "192.168.%d.200"%i
            p.endip = "192.168.%d.220"%i
        
            '''add 10 clusters'''
            for j in range(2):
                c = cluster()
                c.clustername = "test"+str(l)+str(i) + str(j)
                c.clustertype = "CloudManaged"
                c.hypervisor = "Simulator"
            
                '''add 10 hosts'''
                for k in range(2):
                    h = host()
                    h.username = "root"
                    h.password = "password"
                    memory = 8*1024*1024*1024
                    localstorage=1*1024*1024*1024*1024
                    #h.url = "http://sim/%d%d%d%d/cpucore=1&cpuspeed=8000&memory=%d&localstorage=%d"%(l,i,j,k,memory,localstorage)
                    h.url = "http://sim/%d%d%d%d"%(l,i,j,k)
                    c.hosts.append(h)
                
                '''add 2 primary storages'''
                for m in range(2):
                    primary = primaryStorage()
                    primary.name = "primary"+str(l) + str(i) + str(j) + str(m)
                    #primary.url = "nfs://localhost/path%s/size=%d"%(str(l) + str(i) + str(j) + str(m), size)
                    primary.url = "nfs://localhost/path%s"%(str(l) + str(i) + str(j) + str(m))
                    c.primaryStorages.append(primary)
        
                p.clusters.append(c)
            
            z.pods.append(p)
            
        '''add two secondary'''
        for i in range(5):
            secondary = secondaryStorage()
            secondary.url = "nfs://localhost/path"+str(l) + str(i)
            z.secondaryStorages.append(secondary)
        
        '''add default public network'''
        ips = iprange()
        ips.vlan = "26"
        ips.startip = "172.16.26.2"
        ips.endip = "172.16.26.100"
        ips.gateway = "172.16.26.1"
        ips.netmask = "255.255.255.0"
        z.ipranges.append(ips)
        
        
        zs.zones.append(z)
    
    '''Add one mgt server'''
    mgt = managementServer()
    mgt.mgtSvrIp = "localhost"
    zs.mgtSvr.append(mgt)
    
    '''Add a database'''
    db = dbServer()
    db.dbSvr = "localhost"
    
    zs.dbSvr = db
    
    '''add global configuration'''
    global_settings = {'expunge.delay': '60',
                       'expunge.interval': '60',
                       'expunge.workers': '3',
                       }
    for k,v in global_settings.iteritems():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        zs.globalConfig.append(cfg)
    
    ''''add loggers'''
    testClientLogger = logger()
    testClientLogger.name = "TestClient"
    testClientLogger.file = "/tmp/testclient.log"
    
    testCaseLogger = logger()
    testCaseLogger.name = "TestCase"
    testCaseLogger.file = "/tmp/testcase.log"
    
    zs.logger.append(testClientLogger)
    zs.logger.append(testCaseLogger)
    
    return zs

def generate_setup_config(config, file=None):
    describe = config
    if file is None:
        return json.dumps(jsonHelper.jsonDump.dump(describe))
    else:
        fp = open(file, 'w')
        json.dump(jsonHelper.jsonDump.dump(describe), fp, indent=4)
        fp.close()
        
    
def get_setup_config(file):
    if not os.path.exists(file):
        raise IOError("config file %s not found. please specify a valid config file"%file)
    config = cloudstackConfiguration()
    configLines = []
    with open(file, 'r') as fp:
        for line in fp:
            ws = line.strip()
            if not ws.startswith("#"):
                configLines.append(ws)
    config = json.loads("\n".join(configLines))
    return jsonHelper.jsonLoader(config)

if __name__ == "__main__":
    parser = OptionParser()
  
    parser.add_option("-i", "--input", action="store", default=None , dest="inputfile", help="input file")
    parser.add_option("-a", "--advanced", action="store_true", default=False, dest="advanced", help="use advanced networking")
    parser.add_option("-o", "--output", action="store", default="./datacenterCfg", dest="output", help="the path where the json config file generated, by default is ./datacenterCfg")
    
    (options, args) = parser.parse_args()
    
    if options.inputfile:
        config = get_setup_config(options.inputfile)
    if options.advanced:
        config = describe_setup_in_advanced_mode()
    else:
        config = describe_setup_in_basic_mode()
        
    generate_setup_config(config, options.output)
    
    
