"""Deploy datacenters according to a json configuration file"""
import configGenerator
import cloudstackException
import cloudstackTestClient
import sys
import logging
from cloudstackAPI import *
from optparse import OptionParser

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
                self.createVlanIpRanges("Basic", pod.guestIpRanges, zoneId,\
                                        podId)

            self.createClusters(pod.clusters, zoneId, podId)

    def createVlanIpRanges(self, mode, ipranges, zoneId, podId=None,\
                           networkId=None):
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

    def createnetworks(self, networks, zoneId, mode):
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

            self.createVlanIpRanges(mode, ipranges, zoneId, networkId)

    def createPhysicalNetwork(self, name, zoneid, vlan=None):
        phynet = createPhysicalNetwork.createPhysicalNetworkCmd()
        phynet.zoneid = zoneid
        phynet.name = name
        if vlan:
            phynet.vlan = vlan
        return self.apiClient.createPhysicalNetwork(phynet)

    def updatePhysicalNetwork(self, networkid, state="Enabled", vlan=None):
        upnet = updatePhysicalNetwork.updatePhysicalNetworkCmd()
        upnet.id = networkid
        upnet.state = state
        if vlan:
            upnet.vlan = vlan
        return self.apiClient.updatePhysicalNetwork(upnet)

    def configureProviders(self, phynetwrk, zone):
        pnetprov = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
        pnetprov.physicalnetworkid = phynetwrk.id
        pnetprov.state = "Disabled"
        pnetprov.name = "VirtualRouter"
        pnetprovres = self.apiClient.listNetworkServiceProviders(pnetprov)

        vrprov = listVirtualRouterElements.listVirtualRouterElementsCmd()
        vrprov.nspid = pnetprovres[0].id
        vrprovresponse = self.apiClient.listVirtualRouterElements(vrprov)
        vrprovid = vrprovresponse[0].id

        vrconfig = \
        configureVirtualRouterElement.configureVirtualRouterElementCmd()
        vrconfig.enabled = "true"
        vrconfig.id = vrprovid
        vrconfigresponse = \
        self.apiClient.configureVirtualRouterElement(vrconfig)

        if zone.networktype == "Basic" and zone.securitygroupenabled:
            sgprovider = configGenerator.provider()
            sgprovider.name = "SecurityGroupProvider"
            zone.providers.append(sgprovider)

        for prov in zone.providers:
            pnetprov = \
            listNetworkServiceProviders.listNetworkServiceProvidersCmd()
            pnetprov.physicalnetworkid = phynetwrk.id
            pnetprov.name = prov.name
            pnetprov.state = "Disabled"
            pnetprovs = self.apiClient.listNetworkServiceProviders(pnetprov)

            upnetprov = \
            updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
            upnetprov.id = pnetprovs[0].id
            upnetprov.state = "Enabled"
            upnetprovresponse = \
            self.apiClient.updateNetworkServiceProvider(upnetprov)

    def addTrafficTypes(self, physical_network_id, traffictypes=None, \
                        network_labels=None):
        [self.addTrafficType(physical_network_id, traffictype) for \
         traffictype in traffictypes]

    def addTrafficType(self, physical_network_id, traffictype, \
                       network_label=None):
        traffic_type = addTrafficType.addTrafficTypeCmd()
        traffic_type.physicalnetworkid = physical_network_id
        traffic_type.traffictype = traffictype
        return self.apiClient.addTrafficType(traffic_type)

    def enableZone(self, zoneid, allocation_state="Enabled"):
        zoneCmd = updateZone.updateZoneCmd()
        zoneCmd.id = zoneid
        zoneCmd.allocationstate = allocation_state
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
            createzone.networktype = zone.networktype
            createzone.guestcidraddress = zone.guestcidraddress

            zoneresponse = self.apiClient.createZone(createzone)
            zoneId = zoneresponse.id

            phynetwrk = self.createPhysicalNetwork(zone.name + "-pnet", \
                                                   zoneId)

            self.addTrafficTypes(phynetwrk.id, ["Guest", "Public", \
                                                    "Management"])

            self.configureProviders(phynetwrk, zone)
            self.updatePhysicalNetwork(phynetwrk.id, "Enabled", vlan=zone.vlan)

            if zone.networktype == "Basic":
                listnetworkoffering = \
                listNetworkOfferings.listNetworkOfferingsCmd()

                listnetworkoffering.name = \
                "DefaultSharedNetworkOfferingWithSGService"

                listnetworkofferingresponse = \
                self.apiClient.listNetworkOfferings(listnetworkoffering)

                guestntwrk = configGenerator.network()
                guestntwrk.displaytext = "guestNetworkForBasicZone"
                guestntwrk.name = "guestNetworkForBasicZone"
                guestntwrk.zoneid = zoneId
                guestntwrk.networkofferingid = \
                        listnetworkofferingresponse[0].id
                self.createnetworks([guestntwrk], zoneId, zone.networktype)

            self.createpods(zone.pods, zone, zoneId)

            if zone.networktype == "Advanced":
                self.createVlanIpRanges(zone.networktype, zone.ipranges, \
                                        zoneId)

            self.createSecondaryStorages(zone.secondaryStorages, zoneId)
            self.enableZone(zoneId, "Enabled")
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
            registerUserRes = \
            self.testClient.getApiClient().registerUserKeys(registerUser)

            apiKey = registerUserRes.apikey
            securityKey = registerUserRes.secretkey

        self.config.mgtSvr[0].port = 8080
        self.config.mgtSvr[0].apiKey = apiKey
        self.config.mgtSvr[0].securityKey = securityKey
        return apiKey, securityKey

    def loadCfg(self):
        try:
            self.config = configGenerator.get_setup_config(self.configFile)
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
            testClientLogger = logging.getLogger("testclient.testengine.run")
            fh = logging.FileHandler(testClientLogFile)
            fh.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s"))
            testClientLogger.addHandler(fh)
            testClientLogger.setLevel(logging.INFO)
        self.testClientLogger = testClientLogger

        self.testClient = \
        cloudstackTestClient.cloudstackTestClient(mgt.mgtSvrIp, mgt.port, \
                                                  mgt.apiKey, \
                                                  mgt.securityKey, \
                                            logging=self.testClientLogger)
        if mgt.apiKey is None:
            apiKey, securityKey = self.registerApiKey()
            self.testClient.close()
            self.testClient = \
            cloudstackTestClient.cloudstackTestClient(mgt.mgtSvrIp, 8080, \
                                                      apiKey, securityKey, \
                                             logging=self.testClientLogger)

        """config database"""
        dbSvr = self.config.dbSvr
        self.testClient.dbConfigure(dbSvr.dbSvr, dbSvr.port, dbSvr.user, \
                                    dbSvr.passwd, dbSvr.db)
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

    parser.add_option("-i", "--intput", action="store", \
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
