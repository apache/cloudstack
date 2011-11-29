#!/usr/bin/env python

'''
############################################################
# Experimental state of scripts 
#    * Need to be reviewed
#    * Only a sandbox
############################################################
'''

from optparse import OptionParser
from configGenerator import *
import random


def getGlobalSettings():
    global_settings = {'expunge.delay': '60',
                       'expunge.interval': '60',
                       'expunge.workers': '3',
                       'workers': '10',
                       'use.user.concentrated.pod.allocation': 'false',
                       'vm.allocation.algorithm': 'random',
                       'vm.op.wait.interval': '5',
                       'guest.domain.suffix': 'sandbox.simulator',
                       'instance.name': 'SIMQA',
                       'direct.agent.load.size': '1000',
                       'default.page.size': '10000',
                       'linkLocalIp.nums': '10',
                       'check.pod.cidrs': 'false',
                      }
    for k, v in global_settings.iteritems():
        cfg = configuration()
        cfg.name = k
        cfg.value = v
        yield cfg


def describeResources(dbnode='localhost', mshost='localhost'):
    zs = cloudstackConfiguration()
    numberofpods = 1

    clustersPerPod = 10
    hostsPerCluster = 2

    z = zone()
    z.dns1 = '4.2.2.2'
    z.dns2 = '10.223.110.254'
    z.internaldns1 = '10.147.28.6'
    z.internaldns2 = '10.223.110.254'
    z.name = 'Sandbox-Simulator'
    z.networktype = 'Advanced'
    z.guestcidraddress = '10.1.1.0/24'
    z.vlan='100-300'

    p = pod()
    p.name = 'POD0'
    p.gateway = '172.1.1.1'
    p.startip = '172.1.1.2'
    p.endip = '172.1.1.200'
    p.netmask = '255.255.255.0'

    v = iprange()
    v.vlan = '30'
    v.gateway = '172.1.2.1'
    v.startip = '172.1.2.2'
    v.endip = '172.1.2.200'
    v.netmask = '255.255.255.0'

    curhost = 1
    for i in range(1, clustersPerPod + 1):
        c = cluster()
        c.clustername = 'POD1-CLUSTER' + str(i)
        c.hypervisor = 'Simulator'
        c.clustertype = 'CloudManaged'

        for j in range(1, hostsPerCluster + 1):
            h = host()
            h.username = 'root'
            h.password = 'password'
            h.url = 'http://sim/test-%d'%(curhost)
            c.hosts.append(h)
            curhost = curhost + 1

        ps = primaryStorage()
        ps.name = 'spool'+str(i)
        ps.url = 'nfs://172.16.24.32/export/path/'+str(i)
        c.primaryStorages.append(ps)
        p.clusters.append(c)


    secondary = secondaryStorage()
    secondary.url = 'nfs://172.16.25.32/secondary/path'

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
    parser.add_option('-o', '--output', action='store', default='./sandbox.cfg', dest='output', help='the path where the json config file generated')
    parser.add_option('-d', '--dbnode', dest='dbnode', help='hostname/ip of the database node', action='store')
    parser.add_option('-m', '--mshost', dest='mshost', help='hostname/ip of management server', action='store')

    (opts, args) = parser.parse_args()
    cfg = describeResources(opts.dbnode, opts.mshost)
    generate_setup_config(cfg, opts.output)
