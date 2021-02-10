#!/usr/bin/env python3
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

from pyVim.connect import SmartConnect, SmartConnectNoSSL, Disconnect
from pyVmomi import vim
import atexit
import sys
import argparse
import json

isDebugLogs = False
hostClusterNameDict = {}
pgHostNameDict = {}
networksDict = {}

def log_message(msg):
    if isDebugLogs == True:
        print(msg)

def get_clusters(content, cluster=None):
    if cluster is not None:
        log_message("Getting clusters (name=" + cluster + ") ...")
    else:
        log_message("Getting clusters ...")
    cluster_view = content.viewManager.CreateContainerView(content.rootFolder,
                                                        [vim.ClusterComputeResource],
                                                        True)
    clusters = []
    if cluster is not None:
        for c in cluster_view.view:
            if c.name == cluster:
                clusters.append(c)
                hosts = c.host
                for host in hosts:
                    hostClusterNameDict[host.name] = c.name 
                break
    else:
        for c in cluster_view.view:
            clusters.append(c)
            hosts = c.host
            for host in hosts:
                hostClusterNameDict[host.name] = c.name 
    cluster_view.Destroy()
    log_message('\t{} cluster(s) found'.format(len(clusters)))
    for c in clusters:
        log_message('\t' + c.name)
    return clusters


def get_vm_hosts(clusters):
    log_message("Getting ESX hosts ...")
    hosts = []
    for cluster in clusters:
        hosts.extend(cluster.host)
    log_message('\t{} host(s) found'.format(len(hosts)))
    for host in hosts:
        log_message('\t' + host.name)
    return hosts


def get_vms(content):
    log_message("Getting VMs ...")
    vm_view = content.viewManager.CreateContainerView(content.rootFolder,
                                                      [vim.VirtualMachine],
                                                      True)
    obj = [vm for vm in vm_view.view]
    vm_view.Destroy()
    return obj


def get_hosts_port_groups(hosts):
    log_message("Collecting portGroups on hosts. This may take a while ...")
    hostPgDict = {}
    for host in hosts:
        pgs = host.config.network.portgroup
        hostPgDict[host] = pgs
        for pg in pgs:
            pgHostNameDict[pg.spec.name] = host.name
        log_message("\tHost {} done.".format(host.name))
    log_message("\tPortgroup collection complete.")
    return hostPgDict


def get_vm_info(vm, hostPgDict):
    vmPowerState = vm.runtime.powerState
    log_message('\tVM: ' + vm.name + '(' + vmPowerState + ')')
    get_vm_nics(vm, hostPgDict)


def get_vm_nics(vm, hostPgDict):
    try:
        for dev in vm.config.hardware.device:
            if isinstance(dev, vim.vm.device.VirtualEthernetCard):
                dev_backing = dev.backing
                portGroup = None
                vlanId = None
                isolatedPvlan = None
                isolatedPvlanType = None
                vSwitch = None
                if hasattr(dev_backing, 'port'):
                    portGroupKey = dev.backing.port.portgroupKey
                    dvsUuid = dev.backing.port.switchUuid
                    try:
                        dvs = content.dvSwitchManager.QueryDvsByUuid(dvsUuid)
                    except:
                        log_message('\tError: Unable retrieve details for distributed vSwitch ' + dvsUuid)
                        portGroup = ''
                        vlanId = ''
                        vSwitch = ''
                    else:
                        pgObj = dvs.LookupDvPortGroup(portGroupKey)
                        portGroup = pgObj.config.name
                        try:
                            if isinstance(pgObj.config.defaultPortConfig.vlan, vim.dvs.VmwareDistributedVirtualSwitch.PvlanSpec):
                                for pvlanConfig in dvs.config.pvlanConfig:
                                    if pvlanConfig.secondaryVlanId == pgObj.config.defaultPortConfig.vlan.pvlanId:
                                        vlanId = str(pvlanConfig.primaryVlanId)
                                        isolatedPvlanType = pvlanConfig.pvlanType
                                        isolatedPvlan = str(pgObj.config.defaultPortConfig.vlan.pvlanId)
                                        break
                            else:
                                vlanId = str(pgObj.config.defaultPortConfig.vlan.vlanId)
                        except AttributeError:
                            log_message('\tError: Unable retrieve details for portgroup ' + portGroup)
                            vlanId = ''
                        vSwitch = str(dvs.name)
                else:
                    portGroup = dev.backing.network.name
                    vmHost = vm.runtime.host
                    # global variable hostPgDict stores portGroups per host
                    pgs = hostPgDict[vmHost]
                    for p in pgs:
                        if portGroup in p.key:
                            vlanId = str(p.spec.vlanId)
                            vSwitch = str(p.spec.vswitchName)
                if portGroup is None:
                    portGroup = ''
                if vlanId is None:
                    vlanId = ''
                vmHostName = None
                vmClusterName = None
                try:
                    vmHostName = vm.runtime.host.name
                except AttributeError:
                    vmHostName = ''
                try:
                    vmClusterName = vm.runtime.host.parent.name
                except AttributeError:
                    vmClusterName = ''
                add_network(portGroup, vlanId, isolatedPvlanType, isolatedPvlan, vSwitch, vm.name, dev.deviceInfo.label, dev.macAddress, vmClusterName, vmHostName)
                log_message('\t\t' + dev.deviceInfo.label + '->' + dev.macAddress +
                      ' @ ' + vSwitch + '->' + portGroup +
                      ' (VLAN ' + vlanId + ')')
    except AttributeError:
        log_message('\tError: Unable retrieve details for ' + vm.name)

def add_network(portGroup, vlanId, isolatedPvlanType, isolatedPvlan, vSwitch, vmName, vmDeviceLabel, vmMacAddress, vmClusterName, vmHostName):
    key = vSwitch + '->' + portGroup + ' (VLAN ' + vlanId + ')'
    device = {"label": vmDeviceLabel, "macaddress": vmMacAddress}
    vm = {"name":vmName, "device": device}
    if key in networksDict:
        network = networksDict[key]
        network["virtualmachines"].append(vm)
        networksDict[key] = network
    else:
        vms = [vm]
        try:
            host = pgHostNameDict[portGroup]
        except KeyError:
            host = vmHostName
        try:
            cluster = hostClusterNameDict[host]
        except KeyError:
            cluster = vmClusterName
        
        network = {"portgroup": portGroup, "cluster": cluster, "host": host, "switch": vSwitch, "virtualmachines": vms}
        if vlanId != '':
            network["vlanid"] = vlanId
        if isolatedPvlan is not None:
            network["isolatedpvlan"] = isolatedPvlan
        if isolatedPvlanType is not None:
            network["isolatedpvlantype"] = isolatedPvlanType
        networksDict[key] = network


def get_args():
    parser = argparse.ArgumentParser(
        description='Arguments for talking to vCenter')

    parser.add_argument('-s', '--host',
                        required=True,
                        action='store',
                        help='vSpehre service to connect to')

    parser.add_argument('-o', '--port',
                        type=int,
                        default=443,
                        action='store',
                        help='Port to connect on')

    parser.add_argument('-u', '--user',
                        required=True,
                        action='store',
                        help='User name to use')

    parser.add_argument('-p', '--password',
                        required=False,
                        action='store',
                        help='Password to use')

    parser.add_argument('-c', '--cluster',
                        required=False,
                        action='store',
                        help='Cluster for which discover networks')

    parser.add_argument('-S', '--disable_ssl_verification',
                        required=False,
                        action='store_true',
                        help='Disable ssl host certificate verification')

    parser.add_argument('-d', '--debug',
                        required=False,
                        action='store_true',
                        help='Debug log messages')

    args = parser.parse_args()
    return args


def main():
    global content, isDebugLogs, hostClusterNameDict, pgHostNameDict, networksDict
    args = get_args()
    if args.password:
        password = args.password
    else:
        password = getpass.getpass(prompt='Enter password for host %s and '
                                   'user %s: ' % (args.host, args.user))
    if args.debug:
        isDebugLogs = True
    if args.disable_ssl_verification:
        serviceInstance = SmartConnectNoSSL(host=args.host,
                               user=args.user,
                               pwd=password,
                               port=int(args.port))
    else:
        serviceInstance = SmartConnect(host=args.host,
                          user=args.user,
                          pwd=password,
                          port=int(args.port))

    atexit.register(Disconnect, serviceInstance)
    content = serviceInstance.RetrieveContent()
    if args.cluster:
        clusters = get_clusters(content, args.cluster)
    else:        
        clusters = get_clusters(content)
    hosts = []
    if len(clusters) > 0:
        hosts = get_vm_hosts(clusters)
    if len(hosts) > 0:
        hostPgDict = get_hosts_port_groups(hosts)
        vms = get_vms(content)
        log_message('\t{} VM(s) found'.format(len(vms)))
        for vm in vms:
            get_vm_info(vm, hostPgDict)
    networks = list(networksDict.values())
    response = {"count": len(networks), "networks": networks}
    print(json.dumps(response, indent=2, sort_keys=True))

# Main section
if __name__ == "__main__":
    sys.exit(main())
