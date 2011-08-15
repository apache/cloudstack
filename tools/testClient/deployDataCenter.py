'''Deploy datacenters according to a json configuration file'''
import configGenerator
import cloudstackException
import cloudstackTestClient
import sys
import logging
from cloudstackAPI import * 
class deployDataCenters():
    def __init__(self, cfgFile):
        self.configFile = cfgFile
    
    def addHosts(self, hosts, zoneId, podId, clusterId):
        if hosts is None:
            return
        for host in hosts:
            hostcmd = addHost.addHostCmd()
            hostcmd.clusterid = clusterId
            hostcmd.clustername = host.clustername
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
            clusterId = clusterresponse.id
            
            self.addhosts(cluster.hosts, zoneId, podId, clusterId)
            
    def createPrimaryStorages(self, primaryStorages, zoneId, podId):
        if primaryStorages is None:
            return
        for primary in primaryStorages:
            primarycmd = createStoragePool.createStoragePoolCmd()
            primarycmd.clusterid = primary.clusterid
            primarycmd.details = primary.details
            primarycmd.name = primary.name
            primarycmd.podid = podId
            primarycmd.tags = primary.tags
            primarycmd.url = primary.url
            primarycmd.zoneid = zoneId
            self.apiClient.createStoragePool(primarycmd)
            
    def createpods(self, pods, zone, zoneId):
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
            
            if pod.guestIpRanges is not None:
                self.createipRanges("Basic", pod.guestIpRanges, zoneId, podId)
                
            self.createClusters(pod.clusters, zoneId, podId)
            self.createPrimaryStorages(pod.primaryStorages, zoneId, podId)
            
    def createipRanges(self, mode, ipranges, zoneId, podId=None, networkId=None):
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
            vlanipcmd.vlan = iprange.vlan
            vlanipcmd.zoneid = zoneId
            if mode == "Basic":
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
            ipranges = network.ipranges
            if ipranges is None:
                continue
            iprange = ipranges.pop()
            networkcmd = createNetwork.createNetworkCmd()
            networkcmd.account = network.account
            networkcmd.displaytext = network.displaytext
            networkcmd.domainid = network.domainid
            networkcmd.endip = iprange.endip
            networkcmd.gateway = iprange.gateway
            networkcmd.isdefault = network.isdefault
            networkcmd.isshared = network.isshared
            networkcmd.name = network.name
            networkcmd.netmask = iprange.netmask
            networkcmd.networkdomain = network.networkdomain
            networkcmd.networkofferingid = network.networkofferingid
            networkcmdresponse = self.apiClient.createNetwork(networkcmd)
            networkId = networkcmdresponse.id
            
            self.createipRanges("Advanced", ipranges, zoneId, networkId=networkId)
    def createZones(self, zones):
        for zone in zones:
            '''create a zone'''
            createzone = createZone.createZoneCmd()
            createzone.dns1 = zone.dns1
            createzone.dns2 = zone.dns2
            createzone.internaldns1 = zone.internaldns1
            createzone.internaldns2 = zone.internaldns2
            createzone.name = zone.name
            createzone.guestcidraddress = zone.guestcidraddress
            createzone.securitygroupenabled = zone.securitygroupenabled
            createzone.vlan = zone.vlan
            
            zoneresponse = self.apiClient.createZone(createzone)
            zoneId = zoneresponse.id
            
            '''create pods'''
            self.createpods(zone.pods, zone, zoneId)
            
            if zone.networktype == "Advanced":
                '''create pubic network'''
                self.createipRanges(zone.networktype, zone.ipranges, zoneId)
            
            self.createnetworks(zone.networks, zoneId)
            '''create secondary storage'''
            self.createSecondaryStorages(zone.secondaryStorages, zoneId)
            
    def deploy(self):
        try:
            config =  configGenerator.get_setup_config(self.configFile)
        except:
            raise cloudstackException.InvalidParameterException("Failed to load cofig" + sys.exc_value)
        
        mgt = config.mgtSvr[0]
        
        loggers = config.logger
        testClientLogFile = None
        if loggers is not None and len(loggers) > 0:
            for log in loggers:
                if log.name == "TestClient":
                    testClientLogFile = log.file
        testClientLogger = None
        if testClientLogFile is not None:
            testClientLogger = logging.getLogger("testClient")
            fh = logging.FileHandler(testClientLogFile)
            testClientLogger.addHandler(fh)
            testClientLogger.setLevel(logging.DEBUG)
        
        self.testClient = cloudstackTestClient.cloudstackTestClient(mgt.mgtSvrIp, mgt.port, mgt.apiKey, mgt.securityKey, logging=testClientLogger)
        
        '''config database'''
        dbSvr = config.dbSvr
        self.testClient.dbConfigure(dbSvr.dbSvr, dbSvr.port, dbSvr.user, dbSvr.passwd, dbSvr.db)
        self.apiClient = self.testClient.getApiClient()
        
        self.createZones(config.zones)
            