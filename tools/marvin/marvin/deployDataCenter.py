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

"""Deploy datacenters according to a json configuration file"""
import configGenerator
from cloudstackException import (
    InvalidParameterException,
    GetDetailExceptionInfo)

import logging
from cloudstackAPI import *
from os import path
from time import sleep
from optparse import OptionParser
from marvin.codes import (FAILED, SUCCESS)
from sys import exit


class DeployDataCenters(object):
    '''
    @Desc : Deploys the Data Center with information provided
    '''
    def __init__(self, test_client, cfg, logger=None):
        self.testClient = test_client
        self.config = cfg
        self.tcRunLogger = logger
        self.apiClient = None

    def addHosts(self, hosts, zoneId, podId, clusterId, hypervisor):
        if hosts is None:
            return
        for host in hosts:
            hostcmd = addHost.addHostCmd()
            hostcmd.clusterid = clusterId
            hostcmd.cpunumber = host.cpunumer
            hostcmd.cpuspeed = host.cpuspeed
            hostcmd.hostmac = host.hostmac
            hostcmd.hosttags = host.hosttags
            hostcmd.hypervisor = host.hypervisor
            hostcmd.memory = host.memory
            hostcmd.password = host.password
            hostcmd.podid = podId
            hostcmd.url = host.url
            hostcmd.username = host.username
            hostcmd.zoneid = zoneId
            hostcmd.hypervisor = hypervisor
            if self.apiClient.addHost(hostcmd) == FAILED:
                self.tcRunLogger.exception("=== Adding Host Failed===")
                exit(1)

    def addVmWareDataCenter(self, vmwareDc):
        vdc = addVmwareDc.addVmwareDcCmd()
        vdc.zoneid = vmwareDc.zoneid
        vdc.name = vmwareDc.name
        vdc.vcenter = vmwareDc.vcenter
        vdc.username = vmwareDc.username
        vdc.password = vmwareDc.password
        if self.apiClient.addVmwareDc(vdc) == FAILED:
            self.tcRunLogger.exception("=== Adding VmWare DC Failed===")
            exit(1)

    def createClusters(self, clusters, zoneId, podId, vmwareDc=None):
        if clusters is None:
            return
        if vmwareDc is not None:
            vmwareDc.zoneid = zoneId
            self.addVmWareDataCenter(vmwareDc)

        for cluster in clusters:
            clustercmd = addCluster.addClusterCmd()
            clustercmd.clustername = cluster.clustername
            clustercmd.clustertype = cluster.clustertype
            clustercmd.hypervisor = cluster.hypervisor
            clustercmd.password = cluster.password
            clustercmd.podid = podId
            clustercmd.url = cluster.url
            clustercmd.username = cluster.username
            clustercmd.zoneid = zoneId
            clusterresponse = self.apiClient.addCluster(clustercmd)
            if clusterresponse != FAILED and clusterresponse[0].id is not None:
                clusterId = clusterresponse[0].id
                self.tcRunLogger.\
                    debug("Cluster Name : %s Id : %s Created Successfully"
                          % (str(cluster.clustername), str(clusterId)))
            else:
                self.tcRunLogger.exception("====Cluster %s Creation Failed"
                                           "=====" % str(cluster.clustername))
                exit(1)
            if cluster.hypervisor.lower() != "vmware":
                self.addHosts(cluster.hosts, zoneId, podId, clusterId,
                              cluster.hypervisor)
            self.waitForHost(zoneId, clusterId)
            self.createPrimaryStorages(cluster.primaryStorages,
                                       zoneId,
                                       podId,
                                       clusterId)

    def waitForHost(self, zoneId, clusterId):
        """
        Wait for the hosts in the zoneid, clusterid to be up

        2 retries with 30s delay
        """
        retry, timeout = 2, 30
        cmd = listHosts.listHostsCmd()
        cmd.clusterid, cmd.zoneid = clusterId, zoneId
        hosts = self.apiClient.listHosts(cmd)
        if hosts == FAILED:
            self.tcRunLogger.exception("=== List Hosts Failed===")
            exit(1)
        while retry != 0:
            for host in hosts:
                if host.state != 'Up':
                    break
            sleep(timeout)
            retry = retry - 1

    def createPrimaryStorages(self,
                              primaryStorages,
                              zoneId,
                              podId,
                              clusterId):
        if primaryStorages is None:
            return
        for primary in primaryStorages:
            primarycmd = createStoragePool.createStoragePoolCmd()
            primarycmd.details = primary.details
            primarycmd.name = primary.name
            primarycmd.podid = podId
            primarycmd.tags = primary.tags
            primarycmd.url = primary.url
            primarycmd.zoneid = zoneId
            primarycmd.clusterid = clusterId
            if self.apiClient.createStoragePool(primarycmd) == FAILED:
                self.tcRunLogger.\
                    exception("=== Create Storage Pool Failed===")
                exit(1)

    def createPods(self,
                   pods,
                   zoneId,
                   networkId=None):
        if pods is None:
            return
        for pod in pods:
            createpod = createPod.createPodCmd()
            createpod.name = pod.name
            createpod.gateway = pod.gateway
            createpod.netmask = pod.netmask
            createpod.startip = pod.startip
            createpod.endip = pod.endip
            createpod.zoneid = zoneId
            createpodResponse = self.apiClient.createPod(createpod)
            if createpodResponse != \
                    FAILED and createpodResponse.id is not None:
                podId = createpodResponse.id
                self.tcRunLogger.debug("Pod Name : %s Id : %s "
                                       "Created Successfully" %
                                       (str(pod.name), str(podId)))
            else:
                self.tcRunLogger.\
                    exception("====Pod: %s Creation "
                              "Failed=====" % str(pod.name))
                exit(1)

            if pod.guestIpRanges is not None and networkId is not None:
                self.createVlanIpRanges("Basic", pod.guestIpRanges, zoneId,
                                        podId, networkId)

            self.createClusters(pod.clusters, zoneId, podId,
                                vmwareDc=pod.vmwaredc)

    def createVlanIpRanges(self, mode, ipranges, zoneId, podId=None,
                           networkId=None, forvirtualnetwork=None):
        if ipranges is None:
            return
        for iprange in ipranges:
            vlanipcmd = createVlanIpRange.createVlanIpRangeCmd()
            vlanipcmd.account = iprange.account
            vlanipcmd.domainid = iprange.domainid
            vlanipcmd.endip = iprange.endip
            vlanipcmd.gateway = iprange.gateway
            vlanipcmd.netmask = iprange.netmask
            vlanipcmd.networkid = networkId
            vlanipcmd.podid = podId
            vlanipcmd.startip = iprange.startip
            vlanipcmd.zoneid = zoneId
            vlanipcmd.vlan = iprange.vlan
            if mode == "Basic":
                if forvirtualnetwork:
                    vlanipcmd.forvirtualnetwork = "true"
                else:
                    vlanipcmd.forvirtualnetwork = "false"
            else:
                vlanipcmd.forvirtualnetwork = "true"
            if self.apiClient.createVlanIpRange(vlanipcmd) == FAILED:
                self.tcRunLogger.\
                    exception("=== Create Vlan Ip Range Failed===")
                exit(1)

    def createSecondaryStorages(self, secondaryStorages, zoneId):
        if secondaryStorages is None:
            return
        for secondary in secondaryStorages:
            secondarycmd = addImageStore.addImageStoreCmd()
            secondarycmd.url = secondary.url
            secondarycmd.provider = secondary.provider
            secondarycmd.details = []

            if secondarycmd.provider == 'S3' \
                    or secondarycmd.provider == "Swift":
                for key, value in vars(secondary.details).iteritems():
                    secondarycmd.details.append({
                                                'key': key,
                                                'value': value
                                                })
            if secondarycmd.provider == "NFS":
                secondarycmd.zoneid = zoneId
            if self.apiClient.addImageStore(secondarycmd) == FAILED:
                self.tcRunLogger.\
                    exception("=== Add Image Store Failed===")
                exit(1)

    def createCacheStorages(self, cacheStorages, zoneId):
        if cacheStorages is None:
            return
        for cache in cacheStorages:
            cachecmd = createSecondaryStagingStore.\
                createSecondaryStagingStoreCmd()
            cachecmd.url = cache.url
            cachecmd.provider = cache.provider
            cachecmd.zoneid = zoneId
            cachecmd.details = []

            if cache.details:
                for key, value in vars(cache.details).iteritems():
                    cachecmd.details.append({
                                            'key': key,
                                            'value': value
                                            })
            if self.apiClient.createSecondaryStagingStore(cachecmd) == FAILED:
                self.tcRunLogger.\
                    exception("=== Create "
                              "SecondaryStagingStorage Failed===")
                exit(1)

    def createNetworks(self, networks, zoneId):
        if networks is None:
            return
        for network in networks:
            networkcmd = createNetwork.createNetworkCmd()
            networkcmd.displaytext = network.displaytext
            networkcmd.name = network.name
            networkcmd.networkofferingid = network.networkofferingid
            networkcmd.zoneid = zoneId

            ipranges = network.ipranges
            if ipranges:
                iprange = ipranges.pop()
                networkcmd.startip = iprange.startip
                networkcmd.endip = iprange.endip
                networkcmd.gateway = iprange.gateway
                networkcmd.netmask = iprange.netmask

            networkcmdresponse = self.apiClient.createNetwork(networkcmd)
            if networkcmdresponse != \
                    FAILED and networkcmdresponse.id is not None:
                networkId = networkcmdresponse.id
                self.tcRunLogger.\
                    debug("Network Name : %s Id : %s Created Successfully"
                          % (str(network.name), str(networkId)))
            else:
                self.tcRunLogger.\
                    exception("====Network : %s "
                              "Creation Failed=====" % str(network.name))
                exit(1)
            return networkId

    def createPhysicalNetwork(self, net, zoneid):
        phynet = createPhysicalNetwork.createPhysicalNetworkCmd()
        phynet.zoneid = zoneid
        phynet.name = net.name
        phynet.isolationmethods = net.isolationmethods
        phynetwrk = self.apiClient.createPhysicalNetwork(phynet)
        if phynetwrk != FAILED and phynetwrk.id is not None:
            self.tcRunLogger.debug("Physical Network Name : %s Id : %s "
                                   "Created Successfully" %
                                   (str(phynet.name),
                                    str(phynetwrk.id)))
        else:
            self.tcRunLogger.exception("====Physical Network "
                                       "Creation Failed=====")
            exit(1)
        self.addTrafficTypes(phynetwrk.id, net.traffictypes)
        return phynetwrk

    def updatePhysicalNetwork(self, networkid, state="Enabled",
                              vlan=None):
        upnet = updatePhysicalNetwork.updatePhysicalNetworkCmd()
        upnet.id = networkid
        upnet.state = state
        if vlan:
            upnet.vlan = vlan
        ret = self.apiClient.updatePhysicalNetwork(upnet)
        if ret == FAILED:
            self.tcRunLogger.\
                exception("====Update Physical Network Failed=====")
            exit(1)
        else:
            return ret

    def enableProvider(self, provider_id):
        upnetprov =\
            updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
        upnetprov.id = provider_id
        upnetprov.state = "Enabled"
        if self.apiClient.updateNetworkServiceProvider(upnetprov) == FAILED:
            self.tcRunLogger.\
                exception("====Update Network Service Provider Failed=====")
            exit(1)

    def configureProviders(self, phynetwrk, providers):
        """
        We will enable the virtualrouter elements for all zones. Other
        providers like NetScalers, SRX, etc are explicitly added/configured
        """
        for provider in providers:
            pnetprov = listNetworkServiceProviders.\
                listNetworkServiceProvidersCmd()
            pnetprov.physicalnetworkid = phynetwrk.id
            pnetprov.state = "Disabled"
            pnetprov.name = provider.name
            pnetprovres = self.apiClient.listNetworkServiceProviders(pnetprov)
            if pnetprovres == FAILED:
                self.tcRunLogger.\
                    exception("====List Network "
                              "Service Providers Failed=====")
                exit(1)

            if pnetprovres and len(pnetprovres) > 0:
                if provider.name == 'VirtualRouter'\
                   or provider.name == 'VpcVirtualRouter':
                    vrprov = listVirtualRouterElements.\
                        listVirtualRouterElementsCmd()
                    vrprov.nspid = pnetprovres[0].id
                    vrprovresponse = self.apiClient.\
                        listVirtualRouterElements(vrprov)
                    vrprovid = vrprovresponse[0].id

                    vrconfig = \
                        configureVirtualRouterElement.\
                        configureVirtualRouterElementCmd()
                    vrconfig.enabled = "true"
                    vrconfig.id = vrprovid
                    if self.apiClient.\
                        configureVirtualRouterElement(vrconfig) == \
                            FAILED:
                        self.tcRunLogger.\
                            exception("====ConfigureVirtualRouterElement "
                                      "Failed=====")
                        exit(1)
                    self.enableProvider(pnetprovres[0].id)
                elif provider.name == 'InternalLbVm':
                    internallbprov = listInternalLoadBalancerElements.\
                        listInternalLoadBalancerElementsCmd()
                    internallbprov.nspid = pnetprovres[0].id
                    internallbresponse = self.apiClient.\
                        listInternalLoadBalancerElements(internallbprov)
                    if internallbresponse == FAILED:
                        self.tcRunLogger.\
                            exception("====List "
                                      "InternalLoadBalancerElements "
                                      "Failed=====")
                        exit(1)
                    internallbid = internallbresponse[0].id
                    internallbconfig = \
                        configureInternalLoadBalancerElement.\
                        configureInternalLoadBalancerElementCmd()
                    internallbconfig.enabled = "true"
                    internallbconfig.id = internallbid
                    self.apiClient.\
                        configureInternalLoadBalancerElement(internallbconfig)
                    self.enableProvider(pnetprovres[0].id)
                elif provider.name == 'SecurityGroupProvider':
                    self.enableProvider(pnetprovres[0].id)
            elif provider.name in ['Netscaler', 'JuniperSRX', 'F5BigIp']:
                netprov = addNetworkServiceProvider.\
                    addNetworkServiceProviderCmd()
                netprov.name = provider.name
                netprov.physicalnetworkid = phynetwrk.id
                result = self.apiClient.addNetworkServiceProvider(netprov)
                for device in provider.devices:
                    if provider.name == 'Netscaler':
                        dev = addNetscalerLoadBalancer.\
                            addNetscalerLoadBalancerCmd()
                        dev.username = device.username
                        dev.password = device.password
                        dev.networkdevicetype = device.networkdevicetype
                        dev.url = configGenerator.getDeviceUrl(device)
                        dev.physicalnetworkid = phynetwrk.id
                        self.apiClient.addNetscalerLoadBalancer(dev)
                    elif provider.name == 'JuniperSRX':
                        dev = addSrxFirewall.addSrxFirewallCmd()
                        dev.username = device.username
                        dev.password = device.password
                        dev.networkdevicetype = device.networkdevicetype
                        dev.url = configGenerator.getDeviceUrl(device)
                        dev.physicalnetworkid = phynetwrk.id
                        self.apiClient.addSrxFirewall(dev)
                    elif provider.name == 'F5BigIp':
                        dev = addF5LoadBalancer.addF5LoadBalancerCmd()
                        dev.username = device.username
                        dev.password = device.password
                        dev.networkdevicetype = device.networkdevicetype
                        dev.url = configGenerator.getDeviceUrl(device)
                        dev.physicalnetworkid = phynetwrk.id
                        self.apiClient.addF5LoadBalancer(dev)
                    else:
                        raise InvalidParameterException("Device %s "
                                                        "doesn't match "
                                                        "any know provider "
                                                        "type" % device)
                self.enableProvider(result.id)

    def addTrafficTypes(self, physical_network_id, traffictypes):
        [self.addTrafficType(physical_network_id, traffic_type)
            for traffic_type in traffictypes]

    def addTrafficType(self, physical_network_id, traffictype):
        traffic_type = addTrafficType.addTrafficTypeCmd()
        traffic_type.physicalnetworkid = physical_network_id
        traffic_type.traffictype = traffictype.typ
        traffic_type.kvmnetworklabel = traffictype.kvm\
            if traffictype.kvm is not None else None
        traffic_type.xennetworklabel = traffictype.xen\
            if traffictype.xen is not None else None
        traffic_type.vmwarenetworklabel = traffictype.vmware\
            if traffictype.vmware is not None else None
        traffic_type.simulatorlabel = traffictype.simulator\
            if traffictype.simulator is not None else None
        ret = self.apiClient.addTrafficType(traffic_type)
        if ret == FAILED:
            self.tcRunLogger.\
                exception("==== Add TrafficType Failed=====")
        else:
            return ret

    def enableZone(self, zoneid, allocation_state="Enabled"):
        zoneCmd = updateZone.updateZoneCmd()
        zoneCmd.id = zoneid
        zoneCmd.allocationstate = allocation_state
        ret = self.apiClient.updateZone(zoneCmd)
        if ret == FAILED:
            self.tcRunLogger.exception("==== Update Zone Failed=====")
        else:
            return ret

    def updateZoneDetails(self, zoneid, details):
        zoneCmd = updateZone.updateZoneCmd()
        zoneCmd.id = zoneid
        zoneCmd.details = details
        ret = self.apiClient.updateZone(zoneCmd)
        if ret == FAILED:
            self.tcRunLogger.exception("==== Update Zone  Failed=====")
        else:
            return ret

    def createZones(self, zones):
        for zone in zones:
            createzone = createZone.createZoneCmd()
            createzone.dns1 = zone.dns1
            createzone.dns2 = zone.dns2
            createzone.internaldns1 = zone.internaldns1
            createzone.internaldns2 = zone.internaldns2
            createzone.name = zone.name
            createzone.securitygroupenabled = zone.securitygroupenabled
            createzone.localstorageenabled = zone.localstorageenabled
            createzone.networktype = zone.networktype
            if zone.securitygroupenabled != "true":
                createzone.guestcidraddress = zone.guestcidraddress

            zoneresponse = self.apiClient.createZone(createzone)
            if zoneresponse != FAILED and zoneresponse.id is not None:
                zoneId = zoneresponse.id
                self.tcRunLogger.debug("Zone Name : %s Id : %s "
                                       "Created Successfully" %
                                       (str(zone.name), str(zoneId)))
            else:
                self.tcRunLogger.\
                    exception("====ZoneCreation :  %s Failed=====" %
                              str(zone.name))
                exit(1)

            for pnet in zone.physical_networks:
                phynetwrk = self.createPhysicalNetwork(pnet, zoneId)
                self.configureProviders(phynetwrk, pnet.providers)
                self.updatePhysicalNetwork(phynetwrk.id, "Enabled",
                                           vlan=pnet.vlan)

            if zone.networktype == "Basic":
                listnetworkoffering =\
                    listNetworkOfferings.listNetworkOfferingsCmd()
                listnetworkoffering.name =\
                    "DefaultSharedNetscalerEIPandELBNetworkOffering" \
                    if len(filter(lambda x:
                                  x.typ == 'Public',
                                  zone.physical_networks[0].
                                  traffictypes)) > 0 \
                    else "DefaultSharedNetworkOfferingWithSGService"
                if zone.networkofferingname is not None:
                    listnetworkoffering.name = zone.networkofferingname

                listnetworkofferingresponse = \
                    self.apiClient.listNetworkOfferings(listnetworkoffering)
                if listnetworkofferingresponse == FAILED:
                    self.tcRunLogger.\
                        exception("==== "
                                  "ListNetworkOfferingResponse Failed=====")
                    exit(1)
                guestntwrk = configGenerator.network()
                guestntwrk.displaytext = "guestNetworkForBasicZone"
                guestntwrk.name = "guestNetworkForBasicZone"
                guestntwrk.zoneid = zoneId
                guestntwrk.networkofferingid = \
                    listnetworkofferingresponse[0].id

                networkid = self.createNetworks([guestntwrk], zoneId)
                self.createPods(zone.pods, zoneId, networkid)
                if self.isEipElbZone(zone):
                    self.createVlanIpRanges(zone.networktype, zone.ipranges,
                                            zoneId, forvirtualnetwork=True)

            isPureAdvancedZone = (zone.networktype == "Advanced"
                                  and zone.securitygroupenabled != "true")
            if isPureAdvancedZone:
                self.createPods(zone.pods, zoneId)
                self.createVlanIpRanges(zone.networktype, zone.ipranges,
                                        zoneId)
            elif (zone.networktype == "Advanced"
                  and zone.securitygroupenabled == "true"):
                listnetworkoffering =\
                    listNetworkOfferings.listNetworkOfferingsCmd()
                listnetworkoffering.name =\
                    "DefaultSharedNetworkOfferingWithSGService"
                if zone.networkofferingname is not None:
                    listnetworkoffering.name = zone.networkofferingname

                listnetworkofferingresponse = \
                    self.apiClient.listNetworkOfferings(listnetworkoffering)
                if listnetworkofferingresponse == FAILED:
                    self.tcRunLogger.\
                        exception("==== ListNetworkOfferingResponse "
                                  "Failed=====")
                    exit(1)
                networkcmd = createNetwork.createNetworkCmd()
                networkcmd.displaytext = "Shared SG enabled network"
                networkcmd.name = "Shared SG enabled network"
                networkcmd.networkofferingid =\
                    listnetworkofferingresponse[0].id
                networkcmd.zoneid = zoneId

                ipranges = zone.ipranges
                if ipranges:
                    iprange = ipranges.pop()
                    networkcmd.startip = iprange.startip
                    networkcmd.endip = iprange.endip
                    networkcmd.gateway = iprange.gateway
                    networkcmd.netmask = iprange.netmask
                    networkcmd.vlan = iprange.vlan

                networkcmdresponse = self.apiClient.createNetwork(networkcmd)
                if networkcmdresponse != \
                        FAILED and networkcmdresponse.id is not None:
                    networkId = networkcmdresponse.id
                    self.tcRunLogger.\
                        debug("Network Id : %s "
                              "Created Successfully" % str(networkId))
                else:
                    self.tcRunLogger.\
                        exception("====Network Creation Failed=====")
                    exit(1)
                self.createPods(zone.pods, zoneId, networkId)

            '''Note: Swift needs cache storage first'''
            self.createCacheStorages(zone.cacheStorages, zoneId)
            self.createSecondaryStorages(zone.secondaryStorages, zoneId)

            enabled = getattr(zone, 'enabled', 'True')
            if enabled == 'True' or enabled is None:
                self.enableZone(zoneId, "Enabled")
            details = getattr(zone, 'details')
            if details is not None:
                det = [d.__dict__ for d in details]
                self.updateZoneDetails(zoneId, det)

        return

    def isEipElbZone(self, zone):
        if (zone.networktype == "Basic"
            and len(filter(lambda x: x.typ == 'Public',
                           zone.physical_networks[0].traffictypes)) > 0):
            return True
        return False

    def setClient(self):
        self.apiClient = self.testClient.getApiClient()

    def updateConfiguration(self, globalCfg):
        if globalCfg is None or self.apiClient is None:
            return None

        for config in globalCfg:
            updateCfg = updateConfiguration.updateConfigurationCmd()
            updateCfg.name = config.name
            updateCfg.value = config.value
            if self.apiClient.updateConfiguration(updateCfg) == FAILED:
                self.tcRunLogger.\
                    exception("===UpdateConfiguration Failed===")
                exit(1)

    def copyAttributesToCommand(self, source, command):
        map(lambda attr: setattr(command, attr, getattr(source, attr, None)),
            filter(lambda attr: not attr.startswith("__") and attr not in
                   ["required", "isAsync"], dir(command)))

    def configureS3(self, s3):
        if s3 is None:
            return
        command = addS3.addS3Cmd()
        self.copyAttributesToCommand(s3, command)
        if self.apiClient.addS3(command) == FAILED:
            self.tcRunLogger.exception("====AddS3 Failed===")
            exit(1)

    def deploy(self):
        try:
            self.setClient()
            self.updateConfiguration(self.config.globalConfig)
            self.createZones(self.config.zones)
            self.configureS3(self.config.s3)
            return SUCCESS
        except Exception, e:
            print "\nException Occurred Under deploy :%s" % \
                  GetDetailExceptionInfo(e)
            return FAILED

if __name__ == "__main__":
    '''
    @Desc : This facility is mainly to deploy
            DataCenter by using this module as script
            by using the input provided configuration.
    '''
    parser = OptionParser()
    parser.add_option("-i", "--input", action="store",
                      default="./datacenterCfg", dest="input",
                      help="the path \
                      where the json config file generated, by default is \
                      ./datacenterCfg")

    (options, args) = parser.parse_args()
    if options.input:
        '''
        Imports the Modules Required
        '''
        from marvin.marvinLog import MarvinLog
        from marvin.cloudstackTestClient import CSTestClient
        '''
        Step1 : Parse and Create the config from input config provided
        '''
        cfg = configGenerator.getSetupConfig(options.input)
        print "\n***********CONFIG*******",cfg
        log_obj = MarvinLog("CSLog")
        log_check = False
        if log_obj is not None:
            log_check = True
            ret = log_obj.createLogs("DeployDataCenter",
                                     cfg.logger)
            if ret != FAILED:
                log_folder_path = log_obj.getLogFolderPath()
                tc_run_logger = log_obj.getLogger()
        if log_check is False:
            print "\nLog Creation Failed. Please Check"
        else:
            print "\nAll Logs will be available under %s" % \
                  str(log_folder_path)
        '''
        Step2: Create the Test Client
        '''
        obj_tc_client = CSTestClient(cfg.mgtSvr[0], cfg.dbSvr,
                                     logger=tc_run_logger)
        if obj_tc_client is not None and obj_tc_client.createTestClient() \
                != FAILED:
            deploy = DeployDataCenters(obj_tc_client, cfg, tc_run_logger)
            if deploy.deploy() == FAILED:
                print "\nDeploy DC Failed"
                exit(1)
        else:
            print "\nTestClient Creation Failed. Please Check"
            exit(1)
    else:
        print "\n Please Specify a Valid Input Configuration File"

    """
    create = createStoragePool.createStoragePoolCmd()
    create.clusterid = 1
    create.podid = 2
    create.name = "fdffdf"
    create.url = "nfs://jfkdjf/fdkjfkd"
    create.zoneid = 2

    deploy = deployDataCenters("./datacenterCfg")
    deploy.loadCfg()
    deploy.apiClient.createStoragePool(create)
    """
