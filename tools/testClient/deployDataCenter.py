'''Deploy datacenters according to a json configuration file'''
import configGenerator
import cloudstackException
import cloudstackTestClient
import sys
import logging
from cloudstackAPI import * 
from optparse import OptionParser

module_logger = "testclient.deploy"


class deployDataCenters():
    def __init__(self, cfgFile):
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
            
            self.addHosts(cluster.hosts, zoneId, podId, clusterId, cluster.hypervisor)
            self.createPrimaryStorages(cluster.primaryStorages, zoneId, podId, clusterId)

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
                self.createVlanIpRanges("Basic", pod.guestIpRanges, zoneId, podId)
                
            self.createClusters(pod.clusters, zoneId, podId)
            
            
    def createVlanIpRanges(self, mode, ipranges, zoneId, podId=None, networkId=None):
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
            self.createVlanIpRanges("Advanced", ipranges, zoneId, networkId=networkId)

    def createZones(self, zones):
        for zone in zones:
            '''create a zone'''
            createzone = createZone.createZoneCmd()
            createzone.guestcidraddress = zone.guestcidraddress
            createzone.dns1 = zone.dns1
            createzone.dns2 = zone.dns2
            createzone.internaldns1 = zone.internaldns1
            createzone.internaldns2 = zone.internaldns2
            createzone.name = zone.name
            createzone.securitygroupenabled = zone.securitygroupenabled
            createzone.networktype = zone.networktype
            createzone.vlan = zone.vlan
            
            zoneresponse = self.apiClient.createZone(createzone)
            zoneId = zoneresponse.id
            
            '''create pods'''
            self.createpods(zone.pods, zone, zoneId)
            
            if zone.networktype == "Advanced":
                '''create pubic network'''
                self.createVlanIpRanges(zone.networktype, zone.ipranges, zoneId)
            
            self.createnetworks(zone.networks, zoneId)
            '''create secondary storage'''
            self.createSecondaryStorages(zone.secondaryStorages, zoneId)
        return

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
            registerUserRes = self.testClient.getApiClient().registerUserKeys(registerUser)
            apiKey = registerUserRes.apikey
            securityKey = registerUserRes.secretkey
            
        self.config.mgtSvr[0].port = 8080
        self.config.mgtSvr[0].apiKey = apiKey
        self.config.mgtSvr[0].securityKey = securityKey
        return apiKey, securityKey
        
    def loadCfg(self):
        try:
            self.config =  configGenerator.get_setup_config(self.configFile)
        except:
            raise cloudstackException.InvalidParameterException( \
                            "Failed to load config" + sys.exc_info())

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
            testClientLogger = logging.getLogger("testclient.deploy.deployDataCenters")
            fh = logging.FileHandler(testClientLogFile)
            fh.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s"))
            testClientLogger.addHandler(fh)
            testClientLogger.setLevel(logging.DEBUG)
        self.testClientLogger = testClientLogger
        
        self.testClient = cloudstackTestClient.cloudstackTestClient(mgt.mgtSvrIp, mgt.port, mgt.apiKey, mgt.securityKey, logging=self.testClientLogger)
        if mgt.apiKey is None:
            apiKey, securityKey = self.registerApiKey()
            self.testClient.close()
            self.testClient = cloudstackTestClient.cloudstackTestClient(mgt.mgtSvrIp, 8080, apiKey, securityKey, logging=self.testClientLogger)
        
        '''config database'''
        dbSvr = self.config.dbSvr
        self.testClient.dbConfigure(dbSvr.dbSvr, dbSvr.port, dbSvr.user, dbSvr.passwd, dbSvr.db)
        self.apiClient = self.testClient.getApiClient()
    
    def updateConfiguration(self, globalCfg):
        if globalCfg is None:
            return None
        
        for config in globalCfg:
            updateCfg = updateConfiguration.updateConfigurationCmd()
            updateCfg.name = config.name
            updateCfg.value = config.value
            self.apiClient.updateConfiguration(updateCfg)
            
    def deploy(self):
        self.loadCfg()
        self.createZones(self.config.zones)
        self.updateConfiguration(self.config.globalConfig)
        
        
if __name__ == "__main__":
    
    parser = OptionParser()
  
    parser.add_option("-i", "--intput", action="store", default="./datacenterCfg", dest="input", help="the path where the json config file generated, by default is ./datacenterCfg")
    
    (options, args) = parser.parse_args()
    
    deploy = deployDataCenters(options.input)    
    deploy.deploy()   
    
    '''
    create = createStoragePool.createStoragePoolCmd()
    create.clusterid = 1
    create.podid = 2
    create.name = "fdffdf"
    create.url = "nfs://jfkdjf/fdkjfkd"
    create.zoneid = 2
    
    deploy = deployDataCenters("./datacenterCfg")
    deploy.loadCfg()
    deploy.apiClient.createStoragePool(create)
    '''
