#!/usr/bin/env python

'''
############################################################
# Experimental state of scripts 
#    * Need to be reviewed
#    * Only a sandbox
############################################################
'''

from ConfigParser import SafeConfigParser
from optparse import OptionParser
from configGenerator import *
import random


def getGlobalSettings(config):
   for k, v in dict(config.items('globals')).iteritems():
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
    zs.mgtSvr.append(mgt)

    '''Add a database'''
    db = dbServer()
    db.dbSvr = config.get('environment', 'database')
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
