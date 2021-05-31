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

"""
@Desc :
class DeployDataCenters: Deploys DeleteDataCenters according to a json
                         configuration file.
class DeleteDataCenters: Deletes a DataCenter based upon the dc cfg
                         settings provided.
                         This settings file is the exported
                         configuration from DeployDataCenters post
                         its success
"""
from marvin import configGenerator
from marvin.cloudstackException import (
    InvalidParameterException,
    GetDetailExceptionInfo)
from marvin.cloudstackAPI import *
from marvin.codes import (FAILED, SUCCESS)
from marvin.lib.utils import (random_gen)
from marvin.config.test_data import test_data
from sys import exit
import os
import errno
import pickle
from time import sleep, strftime, localtime
from optparse import OptionParser


class DeployDataCenters(object):

    '''
    @Desc : Deploys the Data Center with information provided.
            Once the Deployment is successful, it will export
            the DataCenter settings to an obj file
            ( can be used if wanted to delete the created DC)
    '''

    def __init__(self,
                 test_client,
                 cfg,
                 logger=None,
                 log_folder_path=None
                 ):
        self.__testClient = test_client
        self.__config = cfg
        self.__tcRunLogger = logger
        self.__logFolderPath = log_folder_path
        self.__apiClient = None
        self.__cleanUp = {}

    def __persistDcConfig(self):
        try:
            if self.__logFolderPath:
                dc_file_path = os.path.join(self.__logFolderPath, "dc_entries.obj")
            else:
                ts = strftime("%b_%d_%Y_%H_%M_%S", localtime())
                dc_file_path = "dc_entries_" + str(ts) + ".obj"

            file_to_write = open(dc_file_path, 'w')
            if file_to_write:
                pickle.dump(self.__cleanUp, file_to_write)
                print("\n=== Data Center Settings are dumped to %s===" % dc_file_path)
                self.__tcRunLogger.debug("\n=== Data Center Settings are dumped to %s===" % dc_file_path)
        except Exception as e:
            print("Exception Occurred  while persisting DC Settings: %s" % \
                  GetDetailExceptionInfo(e))

    def __cleanAndExit(self):
        try:
            print("\n===deploy dc failed, so cleaning the created entries===")
            if not test_data.get("deleteDC", None):
                print("\n=== Deploy DC Clean Up flag not set. So, exiting ===")
                exit(1)
            self.__tcRunLogger.debug(
                "===Deploy DC Failed, So Cleaning to Exit===")
            remove_dc_obj = DeleteDataCenters(self.__testClient,
                                              dc_cfg=self.__cleanUp,
                                              tc_run_logger=self.__tcRunLogger
                                              )
            if remove_dc_obj:
                if remove_dc_obj.removeDataCenter() == FAILED:
                    print("\n===Removing DataCenter Failed===")
                    self.__tcRunLogger.debug(
                        "===Removing DataCenter Failed===")
                else:
                    print("\n===Removing DataCenter Successful===")
                    self.__tcRunLogger.debug(
                        "===Removing DataCenter Successful===")
            exit(1)
        except Exception as e:
            print("Exception Occurred during DC CleanUp: %s" % \
                  GetDetailExceptionInfo(e))

    def __addToCleanUp(self, type, id):
        if type not in list(self.__cleanUp.keys()):
            self.__cleanUp[type] = []
        self.__cleanUp[type].append(id)
        if "order" not in list(self.__cleanUp.keys()):
            self.__cleanUp["order"] = []
        if type not in self.__cleanUp["order"]:
            self.__cleanUp["order"].append(type)

    def addHosts(self, hosts, zoneId, podId, clusterId, hypervisor):
        if hosts is None:
            print("\n === Invalid Hosts Information ====")
            return
        failed_cnt = 0
        for host in hosts:
            try:
                hostcmd = addHost.addHostCmd()
                hostcmd.clusterid = clusterId
                hostcmd.hosttags = host.hosttags
                hostcmd.hypervisor = host.hypervisor
                hostcmd.password = host.password
                hostcmd.podid = podId
                hostcmd.url = host.url
                hostcmd.username = host.username
                hostcmd.zoneid = zoneId
                hostcmd.hypervisor = hypervisor
                if hostcmd.hypervisor.lower() == "baremetal":
                    hostcmd.hostmac=host.hostmac
                    hostcmd.cpunumber=host.cpunumber
                    hostcmd.cpuspeed=host.cpuspeed
                    hostcmd.memory=host.memory
                    hostcmd.hosttags=host.hosttags
                ret = self.__apiClient.addHost(hostcmd)
                if ret:
                    self.__tcRunLogger.debug("=== Add Host Successful ===")
                    self.__addToCleanUp("Host", ret[0].id)
            except Exception as e:
                failed_cnt = failed_cnt + 1
                print("Exception Occurred :%s" % GetDetailExceptionInfo(e))
                self.__tcRunLogger.exception(
                    "=== Adding Host Failed :%s===" % str(
                        host.url))
                if failed_cnt == len(hosts):
                    self.__cleanAndExit()
                continue

    def addVmWareDataCenter(self, vmwareDc):
        try:
            vdc = addVmwareDc.addVmwareDcCmd()
            vdc.zoneid = vmwareDc.zoneid
            vdc.name = vmwareDc.name
            vdc.vcenter = vmwareDc.vcenter
            vdc.username = vmwareDc.username
            vdc.password = vmwareDc.password
            ret = self.__apiClient.addVmwareDc(vdc)
            if ret.id:
                self.__tcRunLogger.debug("=== Adding VmWare DC Successful===")
                self.__addToCleanUp("VmwareDc", ret.id)
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception("=== Adding VmWare DC Failed===")
            self.__cleanAndExit()

    def addBaremetalRct(self, config):
        networktype= config.zones[0].networktype
        baremetalrcturl= config.zones[0].baremetalrcturl
        if networktype is None or baremetalrcturl  is None:
            return
        if networktype.lower()=="advanced":

            try:
                brctcmd = addBaremetalRct.addBaremetalRctCmd()
                brctcmd.baremetalrcturl=baremetalrcturl
                ret = self.__apiClient.addBaremetalRct(brctcmd)
                if ret.id:
                    self.__tcRunLogger.debug("=== Adding Baremetal Rct file  Successful===")
                    self.__addToCleanUp("BaremetalRct", ret.id)
            except Exception as e:
                print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
                self.__tcRunLogger.exception("=== Adding  Baremetal Rct file Failed===")
                self.__cleanAndExit()

    def createClusters(self, clusters, zoneId, podId, vmwareDc=None):
        try:
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
                clusterresponse = self.__apiClient.addCluster(clustercmd)
                if clusterresponse[0].id:
                    clusterId = clusterresponse[0].id
                    self.__tcRunLogger.\
                        debug("Cluster Name : %s Id : %s Created Successfully"
                              % (str(cluster.clustername), str(clusterId)))
                    self.__addToCleanUp("Cluster", clusterId)
                if cluster.hypervisor.lower() != "vmware":
                    self.addHosts(cluster.hosts, zoneId, podId, clusterId,
                                  cluster.hypervisor)
                self.waitForHost(zoneId, clusterId)
                if cluster.hypervisor.lower() != "baremetal":
                    self.createPrimaryStorages(cluster.primaryStorages,
                                               zoneId,
                                               podId,
                                               clusterId)

        except Exception as e:
            print("Exception Occurred %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception("====Cluster %s Creation Failed"
                                         "=====" %
                                         str(cluster.clustername))
            self.__cleanAndExit()

    def waitForHost(self, zoneId, clusterId):
        """
        Wait for the hosts in the zoneid, clusterid to be up
        2 retries with 30s delay
        """
        try:
            retry, timeout = 2, 30
            cmd = listHosts.listHostsCmd()
            cmd.clusterid, cmd.zoneid = clusterId, zoneId
            hosts = self.__apiClient.listHosts(cmd)
            while retry != 0:
                for host in hosts:
                    if host.state != 'Up':
                        break
                sleep(timeout)
                retry = retry - 1
        except Exception as e:
            print("\nException Occurred:%s" %\
                  GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception("=== List Hosts Failed===")
            self.__cleanAndExit()

    def createPrimaryStorages(self,
                              primaryStorages,
                              zoneId,
                              podId=None,
                              clusterId=None):
        try:
            if primaryStorages is None:
                return
            for primary in primaryStorages:
                primarycmd = createStoragePool.createStoragePoolCmd()
                if primary.details:
                    for key, value in vars(primary.details).items():
                        primarycmd.details.append({ key: value})
                primarycmd.name = primary.name

                primarycmd.tags = primary.tags
                primarycmd.url = primary.url
                if primary.scope == 'zone' or clusterId is None:
                    primarycmd.scope = 'zone'
                    primarycmd.hypervisor = primary.hypervisor
                else:
                    primarycmd.podid = podId
                    primarycmd.clusterid = clusterId
                primarycmd.zoneid = zoneId

                ret = self.__apiClient.createStoragePool(primarycmd)
                if ret.id:
                    self.__tcRunLogger.debug(
                        "=== Creating Storage Pool Successful===")
                    self.__addToCleanUp("StoragePool", ret.id)
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("=== Create Storage Pool Failed===")
            self.__cleanAndExit()

    def createPods(self,
                   pods,
                   zoneId,
                   networkId=None):
        try:
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
                createpodResponse = self.__apiClient.createPod(createpod)
                if createpodResponse.id:
                    podId = createpodResponse.id
                    self.__tcRunLogger.debug("Pod Name : %s Id : %s "
                                             "Created Successfully" %
                                             (str(pod.name), str(podId)))
                    self.__addToCleanUp("Pod", podId)
                if pod.guestIpRanges is not None and networkId is not None:
                    self.createVlanIpRanges("Basic", pod.guestIpRanges, zoneId,
                                            podId, networkId)
                self.createClusters(pod.clusters, zoneId, podId,
                                    vmwareDc=pod.vmwaredc)
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("====Pod: %s Creation "
                          "Failed=====" % str(pod.name))
            self.__cleanAndExit()

    def createVlanIpRanges(self, mode, ipranges, zoneId, podId=None,
                           networkId=None, forvirtualnetwork=None):
        try:
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
                ret = self.__apiClient.createVlanIpRange(vlanipcmd)
                if ret.id:
                    self.__tcRunLogger.debug(
                        "=== Creating Vlan Ip Range Successful===")
                    self.__addToCleanUp("VlanIpRange", ret.id)
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("=== Create Vlan Ip Range Failed===")
            self.__cleanAndExit()

    def createSecondaryStorages(self, secondaryStorages, zoneId):
        try:
            if secondaryStorages is None:
                return
            for secondary in secondaryStorages:
                secondarycmd = addImageStore.addImageStoreCmd()
                secondarycmd.url = secondary.url
                secondarycmd.provider = secondary.provider
                secondarycmd.details = []

                if secondarycmd.provider.lower() in ('s3', "swift", "smb"):
                    for key, value in vars(secondary.details).items():
                        secondarycmd.details.append({
                                                    'key': key,
                                                    'value': value
                                                    })
                if secondarycmd.provider.lower() in ("nfs", "smb"):
                    secondarycmd.zoneid = zoneId
                ret = self.__apiClient.addImageStore(secondarycmd)
                if ret.id:
                    self.__tcRunLogger.debug(
                        "===Add Image Store Successful===")
                    self.__addToCleanUp("ImageStore", ret.id)
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("=== Add Image Store Failed===")
            self.__cleanAndExit()

    def createCacheStorages(self, cacheStorages, zoneId):
        try:
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
                    for key, value in vars(cache.details).items():
                        cachecmd.details.append({
                                                'key': key,
                                                'value': value
                                                })
                ret = self.__apiClient.createSecondaryStagingStore(cachecmd)
                if ret.id:
                    self.__tcRunLogger.debug(
                        "===Creating Secondary StagingStore Successful===")
                    self.__addToCleanUp("SecondaryStagingStore", ret.id)
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("=== Creating "
                          "SecondaryStagingStorage Failed===")
            self.__cleanAndExit()

    def createNetworks(self, networks, zoneId):
        try:
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
                networkcmdresponse = self.__apiClient.createNetwork(networkcmd)
                if networkcmdresponse.id:
                    networkId = networkcmdresponse.id
                    self.__tcRunLogger.\
                        debug("Creating Network Name : %s Id : %s Successful"
                              % (str(network.name), str(networkId)))
                    self.__addToCleanUp("Network", networkId)
                    return networkId
        except Exception as e:
            print("Exception Occurred %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("====Network : %s "
                          "Creation Failed=====" % str(network.name))
            self.__cleanAndExit()

    def createPhysicalNetwork(self, net, zoneid):
        try:
            phynet = createPhysicalNetwork.createPhysicalNetworkCmd()
            phynet.zoneid = zoneid
            phynet.name = net.name
            phynet.isolationmethods = net.isolationmethods
            if net.tags:
                phynet.tags = net.tags

            phynetwrk = self.__apiClient.createPhysicalNetwork(phynet)
            if phynetwrk.id:
                self.__tcRunLogger.\
                    debug("Creating Physical Network Name : "
                          "%s Id : %s Successful" % (str(phynet.name),
                                                     str(phynetwrk.id)))
                self.__addToCleanUp("PhysicalNetwork", phynetwrk.id)
            self.addTrafficTypes(phynetwrk.id, net.traffictypes)
            return phynetwrk
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception("====Physical Network "
                                         "Creation Failed=====")
            self.__cleanAndExit()

    def updatePhysicalNetwork(self, networkid, state="Enabled",
                              vlan=None):
        try:
            upnet = updatePhysicalNetwork.updatePhysicalNetworkCmd()
            upnet.id = networkid
            upnet.state = state
            if vlan:
                upnet.vlan = vlan
            ret = self.__apiClient.updatePhysicalNetwork(upnet)
            return ret
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("====Update Physical Network Failed=====")
            self.__cleanAndExit()

    def enableProvider(self, provider_id):
        try:
            upnetprov =\
                updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
            upnetprov.id = provider_id
            upnetprov.state = "Enabled"
            ret = self.__apiClient.updateNetworkServiceProvider(upnetprov)
            if ret.id:
                self.__tcRunLogger.debug(
                    "===Update Network Service Provider Successfull===")
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception(
                    "====Update Network Service Provider Failed=====")
            self.__cleanAndExit()

    def configureProviders(self, phynetwrk, providers):
        """
        We will enable the virtualrouter elements for all zones. Other
        providers like NetScalers, SRX, etc are explicitly added/configured
        """
        try:
            for provider in providers:
                pnetprov = listNetworkServiceProviders.\
                    listNetworkServiceProvidersCmd()
                pnetprov.physicalnetworkid = phynetwrk.id
                pnetprov.state = "Disabled"
                pnetprov.name = provider.name
                pnetprovres = self.__apiClient.listNetworkServiceProviders(
                    pnetprov)
                if pnetprovres and len(pnetprovres) > 0:
                    if provider.name == 'VirtualRouter'\
                       or provider.name == 'VpcVirtualRouter':
                        vrprov = listVirtualRouterElements.\
                            listVirtualRouterElementsCmd()
                        vrprov.nspid = pnetprovres[0].id
                        vrprovresponse = self.__apiClient.\
                            listVirtualRouterElements(vrprov)
                        vrprovid = vrprovresponse[0].id
                        vrconfig = \
                            configureVirtualRouterElement.\
                            configureVirtualRouterElementCmd()
                        vrconfig.enabled = "true"
                        vrconfig.id = vrprovid
                        self.__apiClient.\
                            configureVirtualRouterElement(vrconfig)
                        self.enableProvider(pnetprovres[0].id)
                    elif provider.name == 'InternalLbVm':
                        internallbprov = listInternalLoadBalancerElements.\
                            listInternalLoadBalancerElementsCmd()
                        internallbprov.nspid = pnetprovres[0].id
                        internallbresponse = self.__apiClient.\
                            listInternalLoadBalancerElements(internallbprov)
                        internallbid = internallbresponse[0].id
                        internallbconfig = \
                            configureInternalLoadBalancerElement.\
                            configureInternalLoadBalancerElementCmd()
                        internallbconfig.enabled = "true"
                        internallbconfig.id = internallbid
                        self.__apiClient.\
                            configureInternalLoadBalancerElement(
                                internallbconfig)
                        self.enableProvider(pnetprovres[0].id)
                    elif provider.name == 'SecurityGroupProvider':
                        self.enableProvider(pnetprovres[0].id)
                elif provider.name in ['JuniperContrailRouter',
                                       'JuniperContrailVpcRouter']:
                    netprov = addNetworkServiceProvider.\
                        addNetworkServiceProviderCmd()
                    netprov.name = provider.name
                    netprov.physicalnetworkid = phynetwrk.id
                    result = self.__apiClient.addNetworkServiceProvider(netprov)
                    self.enableProvider(result.id)
                elif provider.name in ['Netscaler', 'JuniperSRX', 'F5BigIp', 'NiciraNvp']:
                    netprov = addNetworkServiceProvider.\
                        addNetworkServiceProviderCmd()
                    netprov.name = provider.name
                    netprov.physicalnetworkid = phynetwrk.id
                    result = self.__apiClient.addNetworkServiceProvider(
                        netprov)
                    if result.id:
                        self.__tcRunLogger.\
                            debug("==== AddNetworkServiceProvider "
                                  "Successful=====")
                        self.__addToCleanUp(
                            "NetworkServiceProvider",
                            result.id)
                    if provider.devices is not None:
                        for device in provider.devices:
                            if provider.name == 'Netscaler':
                                dev = addNetscalerLoadBalancer.\
                                    addNetscalerLoadBalancerCmd()
                                dev.username = device.username
                                dev.password = device.password
                                dev.networkdevicetype = device.networkdevicetype
                                dev.url = configGenerator.getDeviceUrl(device)
                                dev.physicalnetworkid = phynetwrk.id
                                ret = self.__apiClient.addNetscalerLoadBalancer(
                                    dev)
                                if ret.id:
                                    self.__tcRunLogger.\
                                        debug("==== AddNetScalerLB "
                                              "Successful=====")
                                    self.__addToCleanUp(
                                        "NetscalerLoadBalancer",
                                        ret.id)
                            elif provider.name == 'JuniperSRX':
                                dev = addSrxFirewall.addSrxFirewallCmd()
                                dev.username = device.username
                                dev.password = device.password
                                dev.networkdevicetype = device.networkdevicetype
                                dev.url = configGenerator.getDeviceUrl(device)
                                dev.physicalnetworkid = phynetwrk.id
                                ret = self.__apiClient.addSrxFirewall(dev)
                                if ret.id:
                                    self.__tcRunLogger.\
                                        debug("==== AddSrx "
                                              "Successful=====")
                                    self.__addToCleanUp("SrxFirewall", ret.id)
                            elif provider.name == 'F5BigIp':
                                dev = addF5LoadBalancer.addF5LoadBalancerCmd()
                                dev.username = device.username
                                dev.password = device.password
                                dev.networkdevicetype = device.networkdevicetype
                                dev.url = configGenerator.getDeviceUrl(device)
                                dev.physicalnetworkid = phynetwrk.id
                                ret = self.__apiClient.addF5LoadBalancer(dev)
                                if ret.id:
                                    self.__tcRunLogger.\
                                        debug("==== AddF5 "
                                              "Successful=====")
                                    self.__addToCleanUp("F5LoadBalancer", ret.id)
                            elif provider.name == 'NiciraNvp':
                                cmd =  addNiciraNvpDevice.addNiciraNvpDeviceCmd()
                                cmd.hostname = device.hostname
                                cmd.username = device.username
                                cmd.password = device.password
                                cmd.transportzoneuuid = device.transportzoneuuid
                                cmd.physicalnetworkid = phynetwrk.id
                                ret = self.__apiClient.addNiciraNvpDevice(cmd)
                                self.__tcRunLogger.\
                                    debug("==== AddNiciraNvp Successful =====")
                                self.__addToCleanUp("NiciraNvp", ret.id)
                            else:
                                raise InvalidParameterException(
                                    "Device %s doesn't match "
                                    "any know provider "
                                    "type" % device)
                    self.enableProvider(result.id)
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("====List Network "
                          "Service Providers Failed=====")
            self.__cleanAndExit()

    def addTrafficTypes(self, physical_network_id, traffictypes):
        [self.addTrafficType(physical_network_id, traffic_type)
            for traffic_type in traffictypes]

    def addTrafficType(self, physical_network_id, traffictype):
        try:
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
            ret = self.__apiClient.addTrafficType(traffic_type)
            if ret.id:
                self.__tcRunLogger.debug("===Add TrafficType Successful====")
                self.__addToCleanUp("TrafficType", ret.id)
                return ret
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("==== Add TrafficType Failed=====")
            self.__cleanAndExit()

    def enableZone(self, zoneid, allocation_state="Enabled"):
        try:
            zoneCmd = updateZone.updateZoneCmd()
            zoneCmd.id = zoneid
            zoneCmd.allocationstate = allocation_state
            ret = self.__apiClient.updateZone(zoneCmd)
            if ret.id:
                self.__tcRunLogger.debug("==== Enable Zone SuccessFul=====")
                return ret
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception("==== Enable Zone Failed=====")
            self.__cleanAndExit()

    def updateZoneDetails(self, zoneid, details):
        try:
            zoneCmd = updateZone.updateZoneCmd()
            zoneCmd.id = zoneid
            zoneCmd.details = details
            ret = self.__apiClient.updateZone(zoneCmd)
            if ret.id:
                self.__tcRunLogger.debug("=== Update Zone SuccessFul===")
                return ret
        except Exception as e:
            print("Exception Occurred: %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception("==== Update Zone  Failed=====")
            self.__cleanAndExit()

    def createZone(self, zone, rec=0):
        try:
            zoneresponse = self.__apiClient.createZone(zone)
            if zoneresponse.id:
                self.__addToCleanUp("Zone", zoneresponse.id)
                self.__tcRunLogger.\
                    debug("Zone Name : %s Id : %s Created Successfully" %
                          (str(zone.name), str(zoneresponse.id)))
                return zoneresponse.id
            else:
                self.__tcRunLogger.\
                    exception("====Zone : %s Creation Failed=====" %
                              str(zone.name))
                print("\n====Zone : %s Creation Failed=====" % str(zone.name))
                if not rec:
                    zone.name = zone.name + "_" + random_gen()
                    self.__tcRunLogger.\
                        debug("====Recreating Zone With New Name : "
                              "%s" % zone.name)
                    print("\n====Recreating Zone With New Name ====", \
                        str(zone.name))
                    return self.createZone(zone, 1)
        except Exception as e:
            print("\nException Occurred under createZone : %s" % \
                  GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception("====Create Zone Failed ===")
            return FAILED

    def createZones(self, zones):
        try:
            for zone in zones:
                zonecmd = createZone.createZoneCmd()
                zonecmd.dns1 = zone.dns1
                zonecmd.dns2 = zone.dns2
                zonecmd.internaldns1 = zone.internaldns1
                zonecmd.internaldns2 = zone.internaldns2
                zonecmd.name = zone.name
                zonecmd.securitygroupenabled = zone.securitygroupenabled
                zonecmd.localstorageenabled = zone.localstorageenabled
                zonecmd.networktype = zone.networktype
                zonecmd.domain = zone.domain
                if zone.securitygroupenabled != "true":
                    zonecmd.guestcidraddress = zone.guestcidraddress
                zoneId = self.createZone(zonecmd)
                if zoneId == FAILED:
                    self.__tcRunLogger.\
                        exception(
                            "====Zone: %s Creation Failed. So Exiting=====" %
                            str(zone.name))
                    self.__cleanAndExit()
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
                        if len([x for x in zone.physical_networks[0].
                                      traffictypes if x.typ == 'Public']) > 0 \
                        else "DefaultSharedNetworkOfferingWithSGService"
                    if zone.networkofferingname is not None:
                        listnetworkoffering.name = zone.networkofferingname
                    listnetworkofferingresponse = \
                        self.__apiClient.listNetworkOfferings(
                            listnetworkoffering)
                    guestntwrk = configGenerator.network()
                    guestntwrk.displaytext = "guestNetworkForBasicZone"
                    guestntwrk.name = "guestNetworkForBasicZone"
                    guestntwrk.zoneid = zoneId
                    guestntwrk.networkofferingid = \
                        listnetworkofferingresponse[0].id
                    networkid = self.createNetworks([guestntwrk], zoneId)
                    self.createPods(zone.pods, zoneId, networkid)
                    if self.isEipElbZone(zone):
                        self.createVlanIpRanges(
                            zone.networktype, zone.ipranges,
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
                        self.__apiClient.listNetworkOfferings(
                            listnetworkoffering)
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
                    networkcmdresponse = self.__apiClient.createNetwork(
                        networkcmd)
                    if networkcmdresponse.id:
                        self.__addToCleanUp("Network", networkcmdresponse.id)
                        self.__tcRunLogger.\
                            debug("create Network Successful. NetworkId : %s "
                                  % str(networkcmdresponse.id))
                    self.createPods(zone.pods, zoneId, networkcmdresponse.id)
                '''Note: Swift needs cache storage first'''
                self.createCacheStorages(zone.cacheStorages, zoneId)
                self.createSecondaryStorages(zone.secondaryStorages, zoneId)
                #add zone wide primary storages if any
                if zone.primaryStorages:
                    self.createPrimaryStorages(zone.primaryStorages,
                                               zoneId,
                                               )
                enabled = getattr(zone, 'enabled', 'True')
                if enabled == 'True' or enabled is None:
                    self.enableZone(zoneId, "Enabled")
                details = getattr(zone, 'details')
                if details is not None:
                    det = [d.__dict__ for d in details]
                    self.updateZoneDetails(zoneId, det)
            return
        except Exception as e:
            print("\nException Occurred %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception("==== Create Zones Failed ===")

    def isEipElbZone(self, zone):
        if (zone.networktype == "Basic"
            and len([x for x in zone.physical_networks[0].traffictypes if x.typ == 'Public']) > 0):
            return True
        return False

    def setClient(self):
        '''
        @Name : setClient
        @Desc : Sets the API Client retrieved from test client
        '''
        self.__apiClient = self.__testClient.getApiClient()

    def updateConfiguration(self, globalCfg):
        try:
            if globalCfg is None or self.__apiClient is None:
                return None
            for config in globalCfg:
                updateCfg = updateConfiguration.updateConfigurationCmd()
                updateCfg.name = config.name
                updateCfg.value = config.value
                ret = self.__apiClient.updateConfiguration(updateCfg)
                if ret.id:
                    self.__tcRunLogger.debug(
                        "==UpdateConfiguration Successfull===")
        except Exception as e:
            print("Exception Occurred %s" % GetDetailExceptionInfo(e))
            self.__tcRunLogger.\
                exception("===UpdateConfiguration Failed===")
            self.__cleanAndExit()

    def copyAttributesToCommand(self, source, command):
        list(map(lambda attr: setattr(command, attr, getattr(source, attr, None)),
            [attr for attr in dir(command) if not attr.startswith("__") and attr not in
                   ["required", "isAsync"]]))

    def configureS3(self, s3):
        try:
            if s3 is None:
                return
            command = addS3.addS3Cmd()
            self.copyAttributesToCommand(s3, command)
            ret = self.__apiClient.addS3(command)
            if ret.id:
                self.__tcRunLogger.debug("===AddS3 Successfull===")
                self.__addToCleanUp("s3", ret.id)
        except Exception as e:
            self.__tcRunLogger.exception("====AddS3 Failed===")
            self.__cleanAndExit()

    def deploy(self):
        try:
            print("\n==== Deploy DC Started ====")
            self.__tcRunLogger.debug("\n==== Deploy DC Started ====")
            '''
            Step1 : Set the Client
            '''
            self.setClient()
            '''
            Step2: Update the Configuration
            '''
            self.updateConfiguration(self.__config.globalConfig)
            '''
            Step3 :Deploy the Zone
            '''
            self.createZones(self.__config.zones)
            self.configureS3(self.__config.s3)
            '''
            Persist the Configuration to an external file post DC creation
            '''
            self.__persistDcConfig()
            print("\n====Deploy DC Successful=====")
            self.__tcRunLogger.debug("\n====Deploy DC Successful====")
            '''
            Upload baremetalSwitch configuration(.rct) file if  enabled zone has baremetal isolated network.
            '''
            self.addBaremetalRct(self.__config)
            self.__tcRunLogger.debug("\n==== AddbaremetalRct Successful====")

            return SUCCESS
        except Exception as e:
            print("\nException Occurred Under deploy :%s" % \
                  GetDetailExceptionInfo(e))
            self.__tcRunLogger.debug("\n====Deploy DC Failed====")
            print("\n====Deploy DC Failed====")
            self.__cleanAndExit()
            return FAILED


class DeleteDataCenters:

    '''
    @Desc : Deletes the Data Center using the settings provided.
            tc_client :Client for deleting the DC.
            dc_cfg_file : obj file exported by DeployDataCenter
            when successfully created DC.
                          This file is serialized one containing
                          entries with successful DC.
            dc_cfg: If dc_cfg_file, is not available, we can use
            the dictionary of elements to delete.
            tc_run_logger: Logger to dump log messages.
    '''

    def __init__(self,
                 tc_client,
                 dc_cfg_file=None,
                 dc_cfg=None,
                 tc_run_logger=None
                 ):
        self.__dcCfgFile = dc_cfg_file
        self.__dcCfg = dc_cfg
        self.__tcRunLogger = tc_run_logger
        self.__apiClient = None
        self.__testClient = tc_client

    def __deleteCmds(self, cmd_name, cmd_obj):
        '''
        @Name : __deleteCmds
        @Desc : Deletes the entities provided by cmd
        '''
        if cmd_name.lower() == "deletehostcmd":
            cmd_obj.forcedestroylocalstorage = "true"
            cmd_obj.force = "true"
            '''
            Step1 : Prepare Host For Maintenance
            '''
            host_maint_cmd = prepareHostForMaintenance.\
                prepareHostForMaintenanceCmd()
            host_maint_cmd.id = cmd_obj.id
            host_maint_resp = self.__apiClient.prepareHostForMaintenance(
                host_maint_cmd)
            if host_maint_resp:
                '''
                Step2 : List Hosts for Resource State
                '''
                list_host_cmd = listHosts.listHostsCmd()
                list_host_cmd.id = cmd_obj.id
                retries = 3
                for i in range(retries):
                    list_host_resp = self.__apiClient.\
                        listHosts(list_host_cmd)
                    if (list_host_resp) and\
                            (list_host_resp[0].resourcestate == 'Maintenance'):
                        break
                    sleep(30)
        if cmd_name.lower() == "deletestoragepoolcmd":
            cmd_obj.forced = "true"
            store_maint_cmd = enableStorageMaintenance.\
                enableStorageMaintenanceCmd()
            store_maint_cmd.id = cmd_obj.id
            store_maint_resp = self.__apiClient.\
                enableStorageMaintenance(store_maint_cmd)
            if store_maint_resp:
                list_store_cmd = listStoragePools.listStoragePoolsCmd()
                list_store_cmd.id = cmd_obj.id
                retries = 3
                for i in range(retries):
                    store_maint_resp = self.__apiClient.\
                        listStoragePools(list_store_cmd)
                    if (store_maint_resp) and \
                            (store_maint_resp[0].state == 'Maintenance'):
                        break
                    sleep(30)
        return cmd_obj

    def __setClient(self):
        '''
        @Name : setClient
        @Desc : Sets the API Client retrieved from test client
        '''
        self.__apiClient = self.__testClient.getApiClient()

    def __cleanEntries(self):
        '''
        @Name : __cleanAndEntries
        @Description: Cleans up the created DC in order of creation
        '''
        try:
            ret = FAILED
            if "order" in list(self.__dcCfg.keys()) and len(self.__dcCfg["order"]):
                self.__dcCfg["order"].reverse()
            print("\n====Clean Up Entries===", self.__dcCfg)
            for type in self.__dcCfg["order"]:
                self.__tcRunLogger.debug(
                    "====CleanUp Started For Type: %s====" %
                    type)
                if type:
                    temp_ids = self.__dcCfg[type]
                    ids = [items for items in temp_ids if items]
                    for id in ids:
                        del_mod = "delete" + type
                        del_cmd = getattr(
                            globals()[del_mod],
                            del_mod + "Cmd")
                        del_cmd_obj = del_cmd()
                        del_cmd_resp = getattr(
                            globals()[del_mod],
                            del_mod + "Response")
                        del_cmd_obj.id = id
                        del_cmd_obj = self.__deleteCmds(
                            del_mod +
                            "Cmd",
                            del_cmd_obj)
                        del_func = getattr(self.__apiClient, del_mod)
                        del_cmd_resp = del_func(del_cmd_obj)
                        if del_cmd_resp:
                            self.__tcRunLogger.debug(
                                "====%s CleanUp Failed. ID: %s ===" %
                                (type, id))
                        else:
                            self.__tcRunLogger.debug(
                                "====%s CleanUp Successful. ID : %s===" %
                                (type, id))
            ret = SUCCESS
        except Exception as e:
            print("\n==== Exception Under __cleanEntries: %s ==== % " \
                  % GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception(
                "\n==== Exception Under __cleanEntries: %s ==== % " %
                GetDetailExceptionInfo(e))
        finally:
            return ret

    def removeDataCenter(self):
        '''
        @Name : removeDataCenter
        @Desc : Removes the Data Center provided by Configuration
                If Input dc file configuration is None, uses the cfg provided
                else uses the dc file to get the configuration
        '''
        try:
            self.__setClient()
            self.__tcRunLogger.debug("====DeployDC: CleanUp Started====")
            print("\n====DeployDC: CleanUp Started====")
            ret = FAILED
            if self.__dcCfgFile:
                file_to_read = open(self.__dcCfgFile, 'r')
                if file_to_read:
                    self.__dcCfg = pickle.load(file_to_read)
            if self.__dcCfg:
                ret = self.__cleanEntries()
        except Exception as e:
            print("\n==== Exception Under removeDataCenter: %s ====" % \
                  GetDetailExceptionInfo(e))
            self.__tcRunLogger.exception(
                "===DeployDC CleanUp FAILED: %s ====" %
                GetDetailExceptionInfo(e))
        finally:
            return ret

def mkdirpath(path):
    try:
        os.makedirs(path)
    except OSError as exc:
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise

if __name__ == "__main__":
    '''
    @Desc : This module facilitates the following:
            1. Deploying DataCenter by using the input provided
            configuration.
              EX: python deployDataCenter.py -i <inp-cfg-file> [-l <directory with logs and output data location>]
            2. Removes a created DataCenter by providing
            the input configuration file and data center settings file
              EX: python deployDataCenter.py -i <inp-cfg-file>
              -r <dc_exported_entries> [-l <directory with logs and output data location>] 
    '''
    parser = OptionParser()
    parser.add_option("-i", "--input", action="store", default=None, dest="input",
                      help="The path where the json zones config file is located.")

    parser.add_option("-r", "--remove", action="store", default=None, dest="remove",
                      help="The path to file where the created dc entries are kept.")

    parser.add_option("-l", "--logdir", action="store", default=None, dest="logdir",
                      help="The directory where result logs of running the script are stored:"
                      "[dc_entries.obj, failed_plus_exceptions.txt, runinfo.txt]." +
                      "Created automatically if doesn't exists.")

    (options, args) = parser.parse_args()

    '''
    Verify the input validity
    '''
    if options.input is None and options.remove is None:
        print("\n==== For DeployDataCenter: Please Specify a valid Input Configuration File====")
        print("\n==== For DeleteDataCenters: Please Specify a valid Input Configuration File and DC Settings====")
        exit(1)

    '''
    Imports the Modules Required
    '''
    from marvin.marvinLog import MarvinLog
    from marvin.cloudstackTestClient import CSTestClient

    '''
    Step1: Create the Logger
    '''
    if (options.input) and not (os.path.isfile(options.input)):
        print("\n=== Invalid Input Config File Path, Please Check ===")
        exit(1)

    log_obj = MarvinLog("CSLog")
    cfg = configGenerator.getSetupConfig(options.input)
    log = cfg.logger

    if options.logdir != None:
        mkdirpath(options.logdir)

    ret = log_obj.createLogs("DeployDataCenter", log, options.logdir, options.logdir == None)
    if ret != FAILED:
        log_folder_path = log_obj.getLogFolderPath()
        tc_run_logger = log_obj.getLogger()
    else:
        print("\n===Log Creation Failed. Please Check===")
        exit(1)

    '''
    Step2 : Create Test Client
    '''
    obj_tc_client = CSTestClient(cfg.mgtSvr[0], cfg.dbSvr,
                                 logger=tc_run_logger)
    if obj_tc_client and obj_tc_client.createTestClient() == FAILED:
        print("\n=== TestClient Creation Failed===")
        exit(1)

    '''
    Step3: Verify and continue whether to deploy a DC or remove a DC
    '''

    if (options.input) and (os.path.isfile(options.input)) and \
            (options.remove is None):
        '''
        @Desc : Deploys a Data Center with provided Config
        '''
        deploy = DeployDataCenters(obj_tc_client,
                                   cfg,
                                   tc_run_logger,
                                   log_folder_path=log_folder_path)
        if deploy.deploy() == FAILED:
            print("\n===Deploy Failed===")
            tc_run_logger.debug("\n===Deploy Failed===");
            exit(1)


    if options.remove and os.path.isfile(options.remove) and options.input:
        '''
        @Desc : Removes a Data Center with provided Config
        '''
        remove_dc_obj = DeleteDataCenters(obj_tc_client,
                                          dc_cfg_file=options.remove,
                                          tc_run_logger=tc_run_logger
                                          )
        if remove_dc_obj:
            if remove_dc_obj.removeDataCenter() == FAILED:
                print("\n===Removing DataCenter Failed===")
                tc_run_logger.debug("\n===Removing DataCenter Failed===")
                exit(1)
            else:
                print("\n===Removing DataCenter Successful===")
                tc_run_logger.debug("\n===Removing DataCenter Successful===")

    # All OK exit with 0 exitcode
    exit(0)
