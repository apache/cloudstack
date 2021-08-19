
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


'''
############################################################
# Experimental state of scripts 
#    * Need to be reviewed
#    * Only a sandbox
############################################################
'''
import random
import marvin
from configparser import SafeConfigParser
from optparse import OptionParser
from marvin.configGenerator import *


def getGlobalSettings(config):
   for k, v in dict(config.items('globals')).items():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        yield cfg


def describeResources(config):
    zs = cloudstackConfiguration()

    z = zone()
    z.dns1 = config.get('environment', 'dns1')
    z.dns2 = config.get('environment', 'dns2')
    z.internaldns1 = config.get('environment', 'internal_dns1')
    z.internaldns2 = config.get('environment', 'internal_dns2')
    z.name = 'z0'
    z.networktype = 'Advanced'
    z.guestcidraddress = '10.1.1.0/24'
    z.securitygroupenabled = 'false'
    
    vpcprovider = provider()
    vpcprovider.name = 'VpcVirtualRouter'

    lbprovider = provider()
    lbprovider.name = 'InternalLbVm'
    
    pn = physical_network()
    pn.name = "z0-pnet"
    pn.traffictypes = [traffictype("Guest"), traffictype("Management"), traffictype("Public")]
    pn.isolationmethods = ["VLAN"]
    pn.vlan = config.get('cloudstack', 'z0.guest.vlan')
    pn.providers.append(vpcprovider)
    pn.providers.append(lbprovider)
    
    z.physical_networks.append(pn)

    p = pod()
    p.name = 'z0p0'
    p.gateway = config.get('cloudstack', 'z0p0.private.gateway')
    p.startip =  config.get('cloudstack', 'z0p0.private.pod.startip')
    p.endip =  config.get('cloudstack', 'z0p0.private.pod.endip')
    p.netmask = config.get('cloudstack', 'z0p0.private.netmask')

    v = iprange()
    v.gateway = config.get('cloudstack', 'z0p0.public.gateway')
    v.startip = config.get('cloudstack', 'z0p0.public.vlan.startip')
    v.endip = config.get('cloudstack', 'z0p0.public.vlan.endip') 
    v.netmask = config.get('cloudstack', 'z0p0.public.netmask')
    v.vlan = config.get('cloudstack', 'z0p0.public.vlan')
    z.ipranges.append(v)

    c = cluster()
    c.clustername = 'z0p0c0'
    c.hypervisor = config.get('cloudstack', 'hypervisor')
    c.clustertype = 'CloudManaged'

    h = host()
    #Host 1
    h.username = 'root'
    h.password = config.get('cloudstack', 'host.password')
    h.url = 'http://%s'%(config.get('cloudstack', 'z0p0c0h0.host'))
    c.hosts.append(h)

    #Host 2
    h1 = host()
    h1.username = 'root'
    h1.password = config.get('cloudstack', 'host.password')
    h1.url = 'http://%s'%(config.get('cloudstack', 'z0p0c0h1.host'))
    c.hosts.append(h1)

    #Primary 1
    ps = primaryStorage()
    ps.name = 'z0p0c0ps0'
    ps.url = config.get('cloudstack', 'z0p0c0ps0.primary.pool')
    c.primaryStorages.append(ps)

    #Primary 2
    ps1 = primaryStorage()
    ps1.name = 'z0p0c0ps1'
    ps1.url = config.get('cloudstack', 'z0p0c0ps1.primary.pool')
    c.primaryStorages.append(ps1)

    p.clusters.append(c)
    z.pods.append(p)

    #Pod 2
    p1 = pod()
    p1.name = 'z0p1'
    p1.gateway = config.get('cloudstack', 'z0p1.private.gateway')
    p1.startip =  config.get('cloudstack', 'z0p1.private.pod.startip')
    p1.endip =  config.get('cloudstack', 'z0p1.private.pod.endip')
    p1.netmask = config.get('cloudstack', 'z0p1.private.netmask')

    #Second public range
    v1 = iprange()
    v1.gateway = config.get('cloudstack', 'z0p1.public.gateway')
    v1.startip = config.get('cloudstack', 'z0p1.public.vlan.startip')
    v1.endip = config.get('cloudstack', 'z0p1.public.vlan.endip') 
    v1.netmask = config.get('cloudstack', 'z0p1.public.netmask')
    v1.vlan = config.get('cloudstack', 'z0p1.public.vlan')
    z.ipranges.append(v1)

    #cluster in pod 2
    c1 = cluster()
    c1.clustername = 'z0p1c0'
    c1.hypervisor = config.get('cloudstack', 'hypervisor')
    c1.clustertype = 'CloudManaged'

    #Host 1
    h2 = host()
    h2.username = 'root'
    h2.password = config.get('cloudstack', 'host.password')
    h2.url = 'http://%s'%(config.get('cloudstack', 'z0p1c0h0.host'))
    c1.hosts.append(h2)
    #Primary 1
    ps2 = primaryStorage()
    ps2.name = 'z0p1c0ps0'
    ps2.url = config.get('cloudstack', 'z0p1c0ps0.primary.pool')
    c1.primaryStorages.append(ps2)

    p1.clusters.append(c1)
    z.pods.append(p1)

    secondary = secondaryStorage()
    secondary.url = config.get('cloudstack', 'z0.secondary.pool')
    secondary.provider = "NFS"
    z.secondaryStorages.append(secondary)

    '''Add zone'''
    zs.zones.append(z)

    '''Add mgt server'''
    mgt = managementServer()
    mgt.mgtSvrIp = config.get('environment', 'mshost')
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
    testClientLogger = logger()
    testClientLogger.name = 'TestClient'
    testClientLogger.file = '/var/log/testclient.log'

    testCaseLogger = logger()
    testCaseLogger.name = 'TestCase'
    testCaseLogger.file = '/var/log/testcase.log'

    zs.logger.append(testClientLogger)
    zs.logger.append(testCaseLogger)
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
