#!/usr/bin/env python
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


import marvin
from configparser import SafeConfigParser
from optparse import OptionParser
from marvin.configGenerator import *
import random


def getGlobalSettings(config):
   for k, v in dict(config.items('globals')).items():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        yield cfg


def describeResources(config):
    zs = cloudstackConfiguration()

    z = zone()
    z.dns1 = config.get('environment', 'dns')
    z.internaldns1 = config.get('environment', 'dns')
    z.name = 'Sandbox-%s'%(config.get('environment', 'hypervisor'))
    z.networktype = 'Advanced'
    z.guestcidraddress = '10.1.1.0/24'
    z.securitygroupenabled = 'false'
    
    vpcprovider = provider()
    vpcprovider.name = 'VpcVirtualRouter'
    
    pn = physicalNetwork()
    pn.name = "Sandbox-pnet"
    pn.traffictypes = [trafficType("Guest"), trafficType("Management"), trafficType("Public")]
    pn.isolationmethods = ["VLAN"]
    pn.providers.append(vpcprovider)
    pn.vlan = config.get('cloudstack', 'zone.vlan')
    
    z.physical_networks.append(pn)

    p = pod()
    p.name = 'POD0'
    p.gateway = config.get('cloudstack', 'private.gateway')
    p.startip =  config.get('cloudstack', 'private.pod.startip')
    p.endip =  config.get('cloudstack', 'private.pod.endip')
    p.netmask = '255.255.255.0'

    v = iprange()
    v.gateway = config.get('cloudstack', 'public.gateway')
    v.startip = config.get('cloudstack', 'public.vlan.startip')
    v.endip = config.get('cloudstack', 'public.vlan.endip') 
    v.netmask = '255.255.255.0'
    v.vlan = config.get('cloudstack', 'public.vlan')
    z.ipranges.append(v)

    c = cluster()
    c.clustername = 'C0'
    c.hypervisor = config.get('environment', 'hypervisor')
    c.clustertype = 'CloudManaged'

    h = host()
    h.username = 'root'
    h.password = 'password'
    h.url = 'http://%s'%(config.get('cloudstack', 'host'))
    c.hosts.append(h)
    
    h = host()
    h.username = 'root'
    h.password = 'password'
    h.url = 'http://%s'%(config.get('cloudstack', 'host2'))
    c.hosts.append(h)

    ps = primaryStorage()
    ps.name = 'PS0'
    ps.url = config.get('cloudstack', 'pool')
    c.primaryStorages.append(ps)

    p.clusters.append(c)
    z.pods.append(p)

    secondary = secondaryStorage()
    secondary.url = config.get('cloudstack', 'secondary')
    z.secondaryStorages.append(secondary)

    '''Add zone'''
    zs.zones.append(z)

    '''Add mgt server'''
    mgt = managementServer()
    mgt.mgtSvrIp = config.get('environment', 'mshost')
    mgt.user = config.get('environment', 'mshost.user')
    mgt.passwd = config.get('environment', 'mshost.passwd')
    zs.mgtSvr.append(mgt)

    '''Add a database'''
    db = dbServer()
    db.dbSvr = config.get('environment', 'mysql.host')
    db.user = config.get('environment', 'mysql.cloud.user')
    db.passwd = config.get('environment', 'mysql.cloud.passwd')
    zs.dbSvr = db

    '''Add some configuration'''
    [zs.globalConfig.append(cfg) for cfg in getGlobalSettings(config)]

    ''''add loggers'''
    testLogger = logger()
    testLogger.logFolderPath = '/tmp/'
    zs.logger = testLogger

    return zs


if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option('-i', '--input', action='store', default='setup.properties', \
                      dest='input', help='file containing environment setup information')
    parser.add_option('-o', '--output', action='store', default='./sandbox.cfg', \
                      dest='output', help='path where environment json will be generated')


    (opts, args) = parser.parse_args()

    cfg_parser = SafeConfigParser()
    cfg_parser.read(opts.input)

    cfg = describeResources(cfg_parser)
    generate_setup_config(cfg, opts.output)
