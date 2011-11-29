#!/usr/bin/env python

'''
############################################################
# Experimental state of scripts 
#    * Need to be reviewed
#    * Only a sandbox
#    * DNS settings are internal
#    * VLAN settings are internal
#    * IP Ranges are internal 
#    * Storage share is internal
############################################################
'''

from optparse import OptionParser
from configGenerator import *
import random


def getGlobalSettings():
    global_settings = {'expunge.delay': '60',
                       'expunge.interval': '60',
                       'storage.cleanup.interval': '300',
                       'account.cleanup.interval': '600',
                       'expunge.workers': '3',
                       'workers': '10',
                       'use.user.concentrated.pod.allocation': 'false',
                       'vm.allocation.algorithm': 'random',
                       'vm.op.wait.interval': '5',
                       'guest.domain.suffix': 'sandbox.xen',
                       'instance.name': 'XENQA',
                       'direct.agent.load.size': '1000',
                       'default.page.size': '10000',
                       'linkLocalIp.nums': '10',
                       'check.pod.cidrs': 'true',
                       'secstorage.allowed.internal.sites': '10.147.28.0/24',
                      }
    for k, v in global_settings.iteritems():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        yield cfg


def describeResources(dbnode='localhost', mshost='localhost', xenhost='localhost'):
    zs = cloudstackConfiguration()
    numberofpods = 1

    clustersPerPod = 1

    z = zone()
    z.dns1 = '10.147.28.6'
    z.dns2 = '10.223.110.254'
    z.internaldns1 = '10.147.28.6'
    z.internaldns2 = '10.223.110.254'
    z.name = 'Sandbox-XEN'
    z.networktype = 'Advanced'
    z.guestcidraddress = '10.1.1.0/24'
    z.vlan='665-669'

    p = pod()
    p.name = 'POD1'
    p.gateway = '10.147.31.1'
    p.startip = '10.147.31.110'
    p.endip = '10.147.31.114'
    p.netmask = '255.255.255.0'

    v = iprange()
    v.vlan = '32'
    v.gateway = '10.147.32.1'
    v.startip = '10.147.32.110'
    v.endip = '10.147.32.114'
    v.netmask = '255.255.255.0'

    c = cluster()
    c.clustername = 'XEN0'
    c.hypervisor = 'XenServer'
    c.clustertype = 'CloudManaged'

    h = host()
    h.username = 'root'
    h.password = 'password'
    h.url = 'http://%s'%(xenhost)
    c.hosts.append(h)

    ps = primaryStorage()
    ps.name = 'PS0'
    ps.url = 'nfs://10.147.28.6/export/home/prasanna/taxila' ## TODO: Make this configurable
    c.primaryStorages.append(ps)
    p.clusters.append(c)

    secondary = secondaryStorage()
    secondary.url = 'nfs://10.147.28.6/export/home/prasanna/secondary' ## TODO: Make this configurable

    z.pods.append(p)
    z.ipranges.append(v)
    z.secondaryStorages.append(secondary)
    zs.zones.append(z)

    '''Add mgt server'''
    mgt = managementServer()
    mgt.mgtSvrIp = mshost
    zs.mgtSvr.append(mgt)

    '''Add a database'''
    db = dbServer()
    db.dbSvr = opts.dbnode
    zs.dbSvr = db

    '''Add some configuration'''
    [zs.globalConfig.append(cfg) for cfg in getGlobalSettings()]

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
    parser.add_option('-o', '--output', action='store', default='./sandbox.xen.cfg', dest='output', help='the path where the json config file generated')
    parser.add_option('-d', '--dbnode', dest='dbnode', help='hostname/ip of the database node', action='store')
    parser.add_option('-m', '--mshost', dest='mshost', help='hostname/ip of management server', action='store')
    parser.add_option('-k', '--hypervisor', dest='hypervisor', help='hostname/ip of hypervisor server', action='store')

    (opts, args) = parser.parse_args()
    cfg = describeResources(opts.dbnode, opts.mshost, opts.hypervisor)
    generate_setup_config(cfg, opts.output)
