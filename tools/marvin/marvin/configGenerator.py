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
from . import jsonHelper
from marvin.codes import *
from marvin.cloudstackException import GetDetailExceptionInfo
from marvin.config.test_data import test_data


class managementServer(object):

    def __init__(self):
        self.mgtSvrIp = None
        self.port = 8096
        self.apiKey = None
        self.securityKey = None
        self.useHttps = None
        self.certCAPath = None
        self.certPath = None


class dbServer(object):

    def __init__(self):
        self.dbSvr = None
        self.port = 3306
        self.user = "cloud"
        self.passwd = "cloud"
        self.db = "cloud"


class configuration(object):

    def __init__(self):
        self.name = None
        self.value = None


class logger(object):

    def __init__(self):
        self.LogFolderPath = None


class cloudstackConfiguration(object):

    def __init__(self):
        self.zones = []
        self.mgtSvr = []
        self.dbSvr = None
        self.globalConfig = []
        self.logger = None
        self.TestData = None


class zone(object):

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
        self.cacheStorages = []
        self.domain = None


class trafficType(object):

    def __init__(self, typ, labeldict=None):
        self.typ = typ  # Guest/Management/Public
        if labeldict:
            self.xen = labeldict['xen'] if 'xen' in list(labeldict.keys()) else None
            self.kvm = labeldict['kvm'] if 'kvm' in list(labeldict.keys()) else None
            self.vmware = labeldict['vmware']\
                if 'vmware' in list(labeldict.keys()) else None
            self.simulator = labeldict['simulator']\
                if 'simulator' in list(labeldict.keys()) else None


class pod(object):

    def __init__(self):
        self.gateway = None
        self.name = None
        self.netmask = None
        self.startip = None
        self.endip = None
        self.zoneid = None
        self.clusters = []
        self.vmwaredc = []
        '''Used in basic network mode'''
        self.guestIpRanges = []


class VmwareDc(object):

    def __init__(self):
        self.zoneid = None
        self.name = None
        self.vcenter = None
        self.username = None
        self.password = None


class cluster(object):

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


class host(object):

    def __init__(self):
        self.hypervisor = None
        self.password = None
        self.url = None
        self.username = None
        self.zoneid = None
        self.podid = None
        self.clusterid = None
        self.clustername = None
        self.hosttags = None
        self.allocationstate = None


class physicalNetwork(object):

    def __init__(self):
        self.name = None
        self.tags = []
        self.traffictypes = []
        self.broadcastdomainrange = 'Zone'
        self.vlan = None
        self.isolationmethods = []
        '''enable default virtual router provider'''
        vrouter = provider()
        vrouter.name = 'VirtualRouter'
        self.providers = [vrouter]


class provider(object):

    def __init__(self, name=None):
        self.name = name
        self.state = None
        self.broadcastdomainrange = 'ZONE'
        self.zoneid = None
        self.servicelist = []
        self.devices = []


class network(object):

    def __init__(self):
        self.displaytext = None
        self.name = None
        self.zoneid = None
        self.acltype = None
        self.domainid = None
        self.networkdomain = None
        self.networkofferingid = None
        self.ipranges = []


class iprange(object):

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


class primaryStorage(object):

    def __init__(self):
        self.name = None
        self.url = None
        self.details = None


class secondaryStorage(object):

    def __init__(self):
        self.url = None
        self.provider = None
        self.details = None


class cacheStorage(object):

    def __init__(self):
        self.url = None
        self.provider = None
        self.details = None


class s3(object):

    def __init__(self):
        self.accesskey = None
        self.secretkey = None
        self.bucket = None
        self.endpoint = None
        self.sockettimeout = None
        self.connectiontimeout = None
        self.maxerrorrety = None
        self.usehttps = None


class netscaler(object):

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
        req = list(zip(list(self.__dict__.keys()), list(self.__dict__.values())))
        return self.hostname + "?" + "&".join(["=".join([r[0], r[1]])
                                               for r in req])


class srx(object):

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
        req = list(zip(list(self.__dict__.keys()), list(self.__dict__.values())))
        return self.hostname + "?" + "&".join(["=".join([r[0], r[1]])
                                               for r in req])


class bigip(object):

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
        req = list(zip(list(self.__dict__.keys()), list(self.__dict__.values())))
        return self.hostname + "?" + "&".join(["=".join([r[0], r[1]])
                                               for r in req])


class ConfigManager(object):

    '''
    @Name: ConfigManager
    @Desc: 1. It provides the basic configuration facilities to marvin.
           2. User can just add configuration files for his tests, deployment
              etc, under one config folder before running their tests.
              cs/tools/marvin/marvin/config.
              They can remove all hard coded values from code and separate
              it out as config at this location.
              Either add this to the existing setup.cfg as separate section
              or add new configuration.
           3. This will thus removes hard coded tests and separate
              data from tests.
           4. This API is provided as an additional facility under
              cloudstackTestClient and users can get the
              configuration object as similar to apiclient,dbconnection
              etc to drive their test.
           5. They just add their configuration for a test,
              setup etc,at one single place under configuration dir
              and use "getConfigParser" API of cloudstackTestClient
              It will give them "configObj".They can either pass their own
              config file for parsing to "getConfig" or it will use
              default config file @ config/setup.cfg.
           6. They will then get the dictionary of parsed
              configuration and can use it further to drive their tests or
              config drive
           7. Test features, can  drive their setups thus removing hard coded
              values. Configuration default file will be under config and as
              setup.cfg.
           8. Users can use their own configuration file passed to
              "getConfig" API,once configObj is returned.
    '''

    def __init__(self, cfg_file=None):
        self.__filePath = cfg_file
        self.__parsedCfgDict = None
        '''
        Set the Configuration
        '''
        self.__setConfig()

    def __setConfig(self):
        if not self.__verifyFile():
            dirPath = os.path.dirname(__file__)
            self.__filePath = str(os.path.join(dirPath, "config/test_data.py"))
        self.__parsedCfgDict = self.__parseConfig()

    def __parseConfig(self):
        '''
        @Name : __parseConfig
        @Description: Parses the Input configuration Json file
                  and returns a dictionary from the file.
        @Input      : NA
        @Output     : Returns the parsed dictionary from json file
                      Returns None for invalid input or if parsing failed
        '''
        config_dict = None
        try:
            if self.__filePath.endswith(".py"):
                config_dict = test_data
            else:
                configLines = []
                with open(self.__filePath, 'r') as fp:
                    for line in fp:
                        ws = line.strip()
                        if not ws.startswith("#"):
                            configLines.append(ws)
                config = json.loads("\n".join(configLines))
                config_dict = config
        except Exception as e:
            # Will replace with log once we have logging done
            print("\n Exception occurred under ConfigManager:__parseConfig" \
                  " :%s", GetDetailExceptionInfo(e))
        finally:
            return config_dict

    def __verifyFile(self):
        '''
        @Name : __parseConfig
        @Description: Parses the Input configuration Json file
                  and returns a dictionary from the file.
        @Input      : NA
        @Output     : True or False based upon file input validity
                      and availability
        '''
        if self.__filePath is None or self.__filePath == '':
            return False
        return os.path.exists(self.__filePath)

    def getSectionData(self, section=None):
        '''
        @Name: getSectionData
        @Desc: Gets the Section data of a particular section
               under parsed dictionary
        @Input: Parsed Dictionary from configuration file
                section to be returned from this dict
        @Output:Section matching inside the parsed data
        '''
        if self.__parsedCfgDict is None or section is None:
            print("\nEither Parsed Dictionary is None or Section is None")
            return INVALID_INPUT
        if section is not None:
            return self.__parsedCfgDict.get(section)

    def getConfig(self):
        '''
        @Name  : getConfig
        @Desc  : Returns the Parsed Dictionary of Config Provided
        @Input : NA
        @Output: ParsedDict if successful if  cfg file provided is valid
                 None if cfg file is invalid or not able to be parsed
        '''
        out = self.__parsedCfgDict
        return out


def getDeviceUrl(obj):
    req = list(zip(list(obj.__dict__.keys()), list(obj.__dict__.values())))
    if obj.hostname:
        return "http://" + obj.hostname + "?" + "&".join(["=".join([r[0],
                                                                    r[1]])
                                                          for r in req])
    else:
        return None


def descSetupInBasicMode():
    '''sample code to generate setup configuration file'''
    zs = cloudstackConfiguration()

    for l in range(1):
        z = zone()
        z.dns1 = "8.8.8.8"
        z.dns2 = "8.8.4.4"
        z.internaldns1 = "192.168.110.254"
        z.internaldns2 = "192.168.110.253"
        z.name = "test" + str(l)
        z.networktype = 'Basic'
        z.securitygroupenabled = 'True'

        # If security groups are reqd
        sgprovider = provider()
        sgprovider.broadcastdomainrange = 'Pod'
        sgprovider.name = 'SecurityGroupProvider'

        pn = physicalNetwork()
        pn.name = "test-network"
        pn.traffictypes = [trafficType("Guest"), trafficType("Management")]
        pn.providers.append(sgprovider)

        z.physical_networks.append(pn)

        '''create 10 pods'''
        for i in range(2):
            p = pod()
            p.name = "test" + str(l) + str(i)
            p.gateway = "192.168.%d.1" % i
            p.netmask = "255.255.255.0"
            p.startip = "192.168.%d.150" % i
            p.endip = "192.168.%d.220" % i

            '''add two pod guest ip ranges'''
            for j in range(2):
                ip = iprange()
                ip.gateway = p.gateway
                ip.netmask = p.netmask
                ip.startip = "192.168.%d.%d" % (i, j * 20)
                ip.endip = "192.168.%d.%d" % (i, j * 20 + 10)

                p.guestIpRanges.append(ip)

            '''add 10 clusters'''
            for j in range(2):
                c = cluster()
                c.clustername = "test" + str(l) + str(i) + str(j)
                c.clustertype = "CloudManaged"
                c.hypervisor = "Simulator"

                '''add 10 hosts'''
                for k in range(2):
                    h = host()
                    h.username = "root"
                    h.password = "password"
                    memory = 8 * 1024 * 1024 * 1024
                    localstorage = 1 * 1024 * 1024 * 1024 * 1024
                    h.url = "http://sim/%d%d%d%d" % (l, i, j, k)
                    c.hosts.append(h)

                '''add 2 primary storages'''
                for m in range(2):
                    primary = primaryStorage()
                    primary.name = "primary" + \
                        str(l) + str(i) + str(j) + str(m)
                    primary.url = "nfs://localhost/path%s" % (str(l) + str(i) +
                                                              str(j) + str(m))
                    c.primaryStorages.append(primary)

                p.clusters.append(c)

            z.pods.append(p)

        '''add two secondary'''
        for i in range(5):
            secondary = secondaryStorage()
            secondary.url = "nfs://localhost/path" + str(l) + str(i)
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
    for k, v in global_settings.items():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        zs.globalConfig.append(cfg)

    return zs


def descSetupInEipMode():
    """
    Setting up an EIP/ELB enabled zone with netscaler provider
    """
    zs = cloudstackConfiguration()

    for l in range(1):
        z = zone()
        z.dns1 = "8.8.8.8"
        z.dns2 = "8.8.4.4"
        z.internaldns1 = "192.168.110.254"
        z.internaldns2 = "192.168.110.253"
        z.name = "test" + str(l)
        z.networktype = 'Basic'

        ips = iprange()
        ips.vlan = "49"
        ips.startip = "10.147.49.200"
        ips.endip = "10.147.49.250"
        ips.gateway = "10.147.49.1"
        ips.netmask = "255.255.255.0"
        z.ipranges.append(ips)

        # If security groups are reqd
        sgprovider = provider()
        sgprovider.broadcastdomainrange = 'Pod'
        sgprovider.name = 'SecurityGroupProvider'

        nsprovider = provider()
        nsprovider.name = 'Netscaler'
        ns = netscaler()
        ns.hostname = '10.147.40.100'
        nsprovider.devices.append(ns)

        pn = physicalNetwork()
        pn.name = "test-network"
        pn.traffictypes = [trafficType("Guest",
                                       {"xen": "cloud-guest"}),
                           trafficType("Management"),
                           trafficType("Public", {"xen": "cloud-public"})]
        pn.providers.extend([sgprovider, nsprovider])
        z.physical_networks.append(pn)

        '''create 10 pods'''
        for i in range(2):
            p = pod()
            p.name = "test" + str(l) + str(i)
            p.gateway = "192.168.%d.1" % i
            p.netmask = "255.255.255.0"
            p.startip = "192.168.%d.150" % i
            p.endip = "192.168.%d.220" % i

            '''add two pod guest ip ranges'''
            for j in range(2):
                ip = iprange()
                ip.gateway = p.gateway
                ip.netmask = p.netmask
                ip.startip = "192.168.%d.%d" % (i, j * 20)
                ip.endip = "192.168.%d.%d" % (i, j * 20 + 10)

                p.guestIpRanges.append(ip)

            '''add 10 clusters'''
            for j in range(2):
                c = cluster()
                c.clustername = "test" + str(l) + str(i) + str(j)
                c.clustertype = "CloudManaged"
                c.hypervisor = "Simulator"

                '''add 10 hosts'''
                for k in range(2):
                    h = host()
                    h.username = "root"
                    h.password = "password"
                    h.url = "http://Sim/%d%d%d%d" % (l, i, j, k)
                    c.hosts.append(h)

                '''add 2 primary storages'''
                for m in range(2):
                    primary = primaryStorage()
                    primary.name = "primary" + \
                        str(l) + str(i) + str(j) + str(m)
                    primary.url = "nfs://localhost/path%s" % (str(l) + str(i)
                                                              + str(j)
                                                              + str(m))
                    c.primaryStorages.append(primary)

                p.clusters.append(c)

            z.pods.append(p)

        '''add two secondary'''
        for i in range(5):
            secondary = secondaryStorage()
            secondary.url = "nfs://localhost/path" + str(l) + str(i)
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
    for k, v in global_settings.items():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        zs.globalConfig.append(cfg)

    return zs


def descSetupInAdvancedMode():
    '''sample code to generate setup configuration file'''
    zs = cloudstackConfiguration()

    for l in range(1):
        z = zone()
        z.dns1 = "8.8.8.8"
        z.dns2 = "8.8.4.4"
        z.internaldns1 = "192.168.110.254"
        z.internaldns2 = "192.168.110.253"
        z.name = "test" + str(l)
        z.networktype = 'Advanced'
        z.guestcidraddress = "10.1.1.0/24"
        z.vlan = "100-2000"

        pn = physicalNetwork()
        pn.name = "test-network"
        pn.traffictypes = [trafficType("Guest"), trafficType("Management"),
                           trafficType("Public")]

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
            p.name = "test" + str(l) + str(i)
            p.gateway = "192.168.%d.1" % i
            p.netmask = "255.255.255.0"
            p.startip = "192.168.%d.200" % i
            p.endip = "192.168.%d.220" % i

            '''add 10 clusters'''
            for j in range(2):
                c = cluster()
                c.clustername = "test" + str(l) + str(i) + str(j)
                c.clustertype = "CloudManaged"
                c.hypervisor = "Simulator"

                '''add 10 hosts'''
                for k in range(2):
                    h = host()
                    h.username = "root"
                    h.password = "password"
                    memory = 8 * 1024 * 1024 * 1024
                    localstorage = 1 * 1024 * 1024 * 1024 * 1024
                    # h.url = "http://sim/%d%d%d%d/cpucore=1&cpuspeed=8000&\
                    #    memory=%d&localstorage=%d"%(l, i, j, k, memory,
                    #                                localstorage)
                    h.url = "http://sim/%d%d%d%d" % (l, i, j, k)
                    c.hosts.append(h)

                '''add 2 primary storages'''
                for m in range(2):
                    primary = primaryStorage()
                    primary.name = "primary" + \
                        str(l) + str(i) + str(j) + str(m)
                    # primary.url = "nfs://localhost/path%s/size=%d" %
                    #    (str(l) + str(i) + str(j) + str(m), size)
                    primary.url = "nfs://localhost/path%s" % (str(l) + str(i)
                                                              + str(j)
                                                              + str(m))
                    c.primaryStorages.append(primary)

                p.clusters.append(c)

            z.pods.append(p)

        '''add two secondary'''
        for i in range(5):
            secondary = secondaryStorage()
            secondary.url = "nfs://localhost/path" + str(l) + str(i)
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
    for k, v in global_settings.items():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        zs.globalConfig.append(cfg)

    return zs

'''sample code to generate setup configuration file'''


def descSetupInAdvancedsgMode():
    zs = cloudstackConfiguration()

    for l in range(1):
        z = zone()
        z.dns1 = "8.8.8.8"
        z.dns2 = "8.8.4.4"
        z.internaldns1 = "192.168.110.254"
        z.internaldns2 = "192.168.110.253"
        z.name = "test" + str(l)
        z.networktype = 'Advanced'
        z.vlan = "100-2000"
        z.securitygroupenabled = "true"

        pn = physicalNetwork()
        pn.name = "test-network"
        pn.traffictypes = [trafficType("Guest"), trafficType("Management")]

        # If security groups are reqd
        sgprovider = provider()
        sgprovider.broadcastdomainrange = 'ZONE'
        sgprovider.name = 'SecurityGroupProvider'

        pn.providers.append(sgprovider)
        z.physical_networks.append(pn)

        '''create 10 pods'''
        for i in range(2):
            p = pod()
            p.name = "test" + str(l) + str(i)
            p.gateway = "192.168.%d.1" % i
            p.netmask = "255.255.255.0"
            p.startip = "192.168.%d.200" % i
            p.endip = "192.168.%d.220" % i

            '''add 10 clusters'''
            for j in range(2):
                c = cluster()
                c.clustername = "test" + str(l) + str(i) + str(j)
                c.clustertype = "CloudManaged"
                c.hypervisor = "Simulator"

                '''add 10 hosts'''
                for k in range(2):
                    h = host()
                    h.username = "root"
                    h.password = "password"
                    memory = 8 * 1024 * 1024 * 1024
                    localstorage = 1 * 1024 * 1024 * 1024 * 1024
                    # h.url = "http://sim/%d%d%d%d/cpucore=1&cpuspeed=8000&\
                    # memory=%d&localstorage=%d" % (l, i, j, k, memory,
                    # localstorage)
                    h.url = "http://sim/%d%d%d%d" % (l, i, j, k)
                    c.hosts.append(h)

                '''add 2 primary storages'''
                for m in range(2):
                    primary = primaryStorage()
                    primary.name = "primary" + \
                        str(l) + str(i) + str(j) + str(m)
                    primary.url = "nfs://localhost/path%s" % \
                        (str(l) + str(i) + str(j) + str(m))
                    c.primaryStorages.append(primary)

                p.clusters.append(c)

            z.pods.append(p)

        '''add two secondary'''
        for i in range(5):
            secondary = secondaryStorage()
            secondary.url = "nfs://localhost/path" + str(l) + str(i)
            z.secondaryStorages.append(secondary)

        '''add default guest network'''
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
    for k, v in global_settings.items():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        zs.globalConfig.append(cfg)

    return zs


def generate_setup_config(config, file=None):
    describe = config
    if file is None:
        return json.dumps(jsonHelper.jsonDump.dump(describe))
    else:
        fp = open(file, 'w')
        json.dump(jsonHelper.jsonDump.dump(describe), fp, indent=4)
        fp.close()


def getSetupConfig(file):
    try:
        config = cloudstackConfiguration()
        configLines = []
        with open(file, 'r') as fp:
            for line in fp:
                ws = line.strip()
                if not ws.startswith("#"):
                    configLines.append(ws)
        config = json.loads("\n".join(configLines))
        return jsonHelper.jsonLoader(config)
    except Exception as e:
        print("\nException Occurred under getSetupConfig %s" % \
              GetDetailExceptionInfo(e))

if __name__ == "__main__":
    parser = OptionParser()

    parser.add_option("-i", "--input", action="store", default=None,
                      dest="inputfile", help="input file")
    parser.add_option("-a", "--advanced", action="store_true", default=False,
                      dest="advanced", help="use advanced networking")
    parser.add_option("-s", "--advancedsg", action="store_true", default=False,
                      dest="advancedsg", help="use advanced networking \
with security groups")
    parser.add_option("-o", "--output", action="store",
                      default="./datacenterCfg", dest="output",
                      help="the path where the json config file generated, \
by default is ./datacenterCfg")

    (options, args) = parser.parse_args()

    if options.inputfile:
        config = getSetupConfig(options.inputfile)
    if options.advanced:
        config = descSetupInAdvancedMode()
    elif options.advancedsg:
        config = descSetupInAdvancedsgMode()
    else:
        config = descSetupInBasicMode()

    generate_setup_config(config, options.output)
