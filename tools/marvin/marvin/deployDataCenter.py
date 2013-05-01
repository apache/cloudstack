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
import cloudstackException
import cloudstackTestClient
import logging
from cloudstackAPI import *
from os import path
from optparse import OptionParser

class deployDataCenters():

    def __init__(self, cfgFile):
        if not path.exists(cfgFile) \
           and not path.exists(path.abspath(cfgFile)):
            raise IOError("config file %s not found. please specify a valid config file"%cfgFile)
        self.configFile = cfgFile

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
            self.apiClient.addHost(hostcmd)

    def createClusters(self, clusters, zoneId, podId):
        if clusters is None:
            return

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
            clusterId = clusterresponse[0].id

            self.addHosts(cluster.hosts, zoneId, podId, clusterId,\
                          cluster.hypervisor)
            self.createPrimaryStorages(cluster.primaryStorages, zoneId, podId,\
                                       clusterId)

    def createPrimaryStorages(self, primaryStorages, zoneId, podId, clusterId):
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
            self.apiClient.createStoragePool(primarycmd)

    def createpods(self, pods, zoneId, networkId=None):
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
            podId = createpodResponse.id

            if pod.guestIpRanges is not None and networkId is not None:
                self.createVlanIpRanges("Basic", pod.guestIpRanges, zoneId,\
                                        podId, networkId)

            self.createClusters(pod.clusters, zoneId, podId)

    def createVlanIpRanges(self, mode, ipranges, zoneId, podId=None,\
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

            self.apiClient.createVlanIpRange(vlanipcmd)

    def createSecondaryStorages(self, secondaryStorages, zoneId):
        if secondaryStorages is None:
            return
        for secondary in secondaryStorages:
            secondarycmd = addSecondaryStorage.addSecondaryStorageCmd()
            secondarycmd.url = secondary.url
            secondarycmd.zoneid = zoneId
            self.apiClient.addSecondaryStorage(secondarycmd)

    def createnetworks(self, networks, zoneId):
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
            networkId = networkcmdresponse.id
            return networkId

    def createPhysicalNetwork(self, net, zoneid):
        phynet = createPhysicalNetwork.createPhysicalNetworkCmd()
        phynet.zoneid = zoneid
        phynet.name = net.name
        phynetwrk = self.apiClient.createPhysicalNetwork(phynet)
        self.addTrafficTypes(phynetwrk.id, net.traffictypes)
        return phynetwrk

    def updatePhysicalNetwork(self, networkid, state="Enabled", vlan=None):
        upnet = updatePhysicalNetwork.updatePhysicalNetworkCmd()
        upnet.id = networkid
        upnet.state = state
        if vlan:
            upnet.vlan = vlan
        return self.apiClient.updatePhysicalNetwork(upnet)
    
    def enableProvider(self, provider_id):
        upnetprov = \
        updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
        upnetprov.id = provider_id
        upnetprov.state = "Enabled"
        self.apiClient.updateNetworkServiceProvider(upnetprov)

    def configureProviders(self, phynetwrk, providers):
        """
        We will enable the virtualrouter elements for all zones. Other providers 
        like NetScalers, SRX, etc are explicitly added/configured
        """
        
        for provider in providers:
            pnetprov = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
            pnetprov.physicalnetworkid = phynetwrk.id
            pnetprov.state = "Disabled"
            pnetprov.name = provider.name
            pnetprovres = self.apiClient.listNetworkServiceProviders(pnetprov)

            if pnetprovres and len(pnetprovres) > 0: 
                if provider.name == 'VirtualRouter'\
                   or provider.name == 'VpcVirtualRouter':
                    vrprov = listVirtualRouterElements.listVirtualRouterElementsCmd()
                    vrprov.nspid = pnetprovres[0].id
                    vrprovresponse = self.apiClient.listVirtualRouterElements(vrprov)
                    vrprovid = vrprovresponse[0].id
        
                    vrconfig = \
                    configureVirtualRouterElement.configureVirtualRouterElementCmd()
                    vrconfig.enabled = "true"
                    vrconfig.id = vrprovid
                    self.apiClient.configureVirtualRouterElement(vrconfig)
                    self.enableProvider(pnetprovres[0].id)
                elif provider.name == 'SecurityGroupProvider':
                    self.enableProvider(pnetprovres[0].id)
            elif provider.name in ['Netscaler', 'JuniperSRX', 'F5BigIp']:
                netprov = addNetworkServiceProvider.addNetworkServiceProviderCmd()
                netprov.name = provider.name
                netprov.physicalnetworkid = phynetwrk.id
                result = self.apiClient.addNetworkServiceProvider(netprov)
                for device in provider.devices:
                    if provider.name == 'Netscaler':
                        dev = addNetscalerLoadBalancer.addNetscalerLoadBalancerCmd()
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
                        raise cloudstackException.InvalidParameterException("Device %s doesn't match any know provider type"%device)
                self.enableProvider(result.id)
            
    def addTrafficTypes(self, physical_network_id, traffictypes):
        [self.addTrafficType(physical_network_id, traffic_type) for traffic_type in traffictypes]

    def addTrafficType(self, physical_network_id, traffictype):
        traffic_type = addTrafficType.addTrafficTypeCmd()
        traffic_type.physicalnetworkid = physical_network_id
        traffic_type.traffictype = traffictype.typ
        traffic_type.kvmnetworklabel = traffictype.kvm if traffictype.kvm is not None else None
        traffic_type.xennetworklabel = traffictype.xen if traffictype.xen is not None else None
        traffic_type.vmwarenetworklabel = traffictype.vmware if traffictype.vmware is not None else None
        traffic_type.simulatorlabel = traffictype.simulator if traffictype.simulator is not None else None
        return self.apiClient.addTrafficType(traffic_type)

    def enableZone(self, zoneid, allocation_state="Enabled"):
        zoneCmd = updateZone.updateZoneCmd()
        zoneCmd.id = zoneid
        zoneCmd.allocationstate = allocation_state
        return self.apiClient.updateZone(zoneCmd)

    def updateZoneDetails(self, zoneid, details):
        zoneCmd = updateZone.updateZoneCmd()
        zoneCmd.id = zoneid
        zoneCmd.details = details
        return self.apiClient.updateZone(zoneCmd)

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
            createzone.guestcidraddress = zone.guestcidraddress
            
            zoneresponse = self.apiClient.createZone(createzone)
            zoneId = zoneresponse.id

            for pnet in zone.physical_networks:
                phynetwrk = self.createPhysicalNetwork(pnet, zoneId)
                self.configureProviders(phynetwrk, pnet.providers)
                self.updatePhysicalNetwork(phynetwrk.id, "Enabled", vlan=pnet.vlan)

            if zone.networktype == "Basic":
                listnetworkoffering = listNetworkOfferings.listNetworkOfferingsCmd()
                listnetworkoffering.name = "DefaultSharedNetscalerEIPandELBNetworkOffering" \
                        if len(filter(lambda x : x.typ == 'Public', zone.physical_networks[0].traffictypes)) > 0 \
                        else "DefaultSharedNetworkOfferingWithSGService"
                if zone.networkofferingname  is not None:
                   listnetworkoffering.name = zone.networkofferingname 

                listnetworkofferingresponse = \
                self.apiClient.listNetworkOfferings(listnetworkoffering)

                guestntwrk = configGenerator.network()
                guestntwrk.displaytext = "guestNetworkForBasicZone"
                guestntwrk.name = "guestNetworkForBasicZone"
                guestntwrk.zoneid = zoneId
                guestntwrk.networkofferingid = \
                        listnetworkofferingresponse[0].id
                        
                networkid = self.createnetworks([guestntwrk], zoneId)
                self.createpods(zone.pods, zoneId, networkid)
                if self.isEipElbZone(zone):
                    self.createVlanIpRanges(zone.networktype, zone.ipranges, \
                                        zoneId, forvirtualnetwork=True)

            if zone.networktype == "Advanced":
                self.createpods(zone.pods, zoneId)
                self.createVlanIpRanges(zone.networktype, zone.ipranges, \
                                        zoneId)

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
        if zone.networktype == "Basic" and len(filter(lambda x : x.typ == 'Public', zone.physical_networks[0].traffictypes)) > 0:
            return True
        return False

    def registerApiKey(self):
        listuser = listUsers.listUsersCmd()
        listuser.account = "admin"
        listuserRes = self.testClient.getApiClient().listUsers(listuser)
        userId = listuserRes[0].id
        apiKey = listuserRes[0].apikey
        securityKey = listuserRes[0].secretkey
        if apiKey is None:
            registerUser = registerUserKeys.registerUserKeysCmd()
            registerUser.id = userId
            registerUserRes = \
            self.testClient.getApiClient().registerUserKeys(registerUser)

            apiKey = registerUserRes.apikey
            securityKey = registerUserRes.secretkey

        self.config.mgtSvr[0].port = 8080
        self.config.mgtSvr[0].apiKey = apiKey
        self.config.mgtSvr[0].securityKey = securityKey
        return apiKey, securityKey

    def getCfg(self):
        if self.config is not None:
            return self.config
        return None

    def loadCfg(self):
        try:
            self.config = configGenerator.get_setup_config(self.configFile)
        except:
            raise cloudstackException.InvalidParameterException( \
                            "Failed to load config %s"%self.configFile)

        mgt = self.config.mgtSvr[0]

        loggers = self.config.logger
        testClientLogFile = None
        self.testCaseLogFile = None
        self.testResultLogFile = None
        if loggers is not None and len(loggers) > 0:
            for log in loggers:
                if log.name == "TestClient":
                    testClientLogFile = log.file
                elif log.name == "TestCase":
                    self.testCaseLogFile = log.file
                elif log.name == "TestResult":
                    self.testResultLogFile = log.file

        testClientLogger = None
        if testClientLogFile is not None:
            testClientLogger = logging.getLogger("testclient.testengine.run")
            fh = logging.FileHandler(testClientLogFile)
            fh.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s"))
            testClientLogger.addHandler(fh)
            testClientLogger.setLevel(logging.INFO)
        self.testClientLogger = testClientLogger

        self.testClient = \
        cloudstackTestClient.cloudstackTestClient(mgt.mgtSvrIp, mgt.port, \
                                                  mgt.user, mgt.passwd, \
                                                  mgt.apiKey, \
                                                  mgt.securityKey, \
                                            logging=self.testClientLogger)
        if mgt.apiKey is None:
            apiKey, securityKey = self.registerApiKey()
            self.testClient = \
            cloudstackTestClient.cloudstackTestClient(mgt.mgtSvrIp, 8080, \
                                                      mgt.user, mgt.passwd, \
                                                      apiKey, securityKey, \
                                             logging=self.testClientLogger)

        """config database"""
        dbSvr = self.config.dbSvr
        if dbSvr is not None:
          self.testClient.dbConfigure(dbSvr.dbSvr, dbSvr.port, dbSvr.user, \
                                      dbSvr.passwd, dbSvr.db)

        self.apiClient = self.testClient.getApiClient()
        """set hypervisor"""
        if mgt.hypervisor:
            self.apiClient.hypervisor = mgt.hypervisor
        else:
            self.apiClient.hypervisor = "XenServer"     #Defaults to Xenserver

    def updateConfiguration(self, globalCfg):
        if globalCfg is None:
            return None

        for config in globalCfg:
            updateCfg = updateConfiguration.updateConfigurationCmd()
            updateCfg.name = config.name
            updateCfg.value = config.value
            self.apiClient.updateConfiguration(updateCfg)

    def copyAttributesToCommand(self, source, command):

        map(lambda attr : setattr(command, attr, getattr(source, attr, None)),
                filter(lambda attr : not attr.startswith("__") and
                    attr not in [ "required", "isAsync" ], dir(command)))


    def configureS3(self, s3):

        if s3 is None:
            return

        command = addS3.addS3Cmd()

        self.copyAttributesToCommand(s3, command)

        self.apiClient.addS3(command)

    def deploy(self):
        self.loadCfg()
        self.updateConfiguration(self.config.globalConfig)
        self.createZones(self.config.zones)
        self.configureS3(self.config.s3)

if __name__ == "__main__":

    parser = OptionParser()

    parser.add_option("-i", "--input", action="store", \
                      default="./datacenterCfg", dest="input", help="the path \
                      where the json config file generated, by default is \
                      ./datacenterCfg")

    (options, args) = parser.parse_args()

    deploy = deployDataCenters(options.input)
    deploy.deploy()

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
