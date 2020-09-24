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
from OvmCommonModule import *
import traceback
import time
import re
 
logger = OvmLogger("OvmNetwork")

class Filter:
    class Network:
        IFNAME_LO     = r'(lo)'
        IFNAME_BRIDGE = r'(xenbr\d+|vlan\d+)'
        IFNAME_PIF    = r'(eth\d+$|bond\d+$)'
        IFNAME_VLAN   = r'(eth\d+.\d+$|bond\d+.\d+$)'


class Parser(object):
    '''
    classdocs
    '''    
    def findall(self, pattern, samples):
        """
        @param pattern: search pattern
        @param result: Parser line execution result
        @return : list of search
        find result of Parser which has same pattern
        findall Parser find all pattern in a string
        """
        result = []
        for line in samples:
            items = re.findall(pattern, line)
            for item in items:
                result.append(item)
        return result
    
    def checkPattern(self, pattern, cmd_result):
        """
        @param pattern: search pattern
        @param cmd_result: Parser line execution result
        @return : True (if pattern is occurred)
        """
        for line in cmd_result:
            items = re.findall(pattern, line)
            if len(items) > 0:
                return True
        return False
    
    def search(self, cmd_result, pattern):
        return None
    
class OvmVlanDecoder(json.JSONDecoder):
    def decode(self, jStr):
        deDict = asciiLoads(jStr)
        vlan = OvmVlan()
        setAttrFromDict(vlan, 'vid', deDict, int)
        setAttrFromDict(vlan, 'pif', deDict)
        return vlan

class OvmVlanEncoder(json.JSONEncoder):
    def default(self, obj):
        if not isinstance(obj, OvmVlan): raise Exception("%s is not instance of OvmVlan"%type(obj))
        dct = {}
        safeDictSet(obj, dct, 'name')
        safeDictSet(obj, dct, 'vid')
        safeDictSet(obj, dct, 'pif')
        return dct
    
def toOvmVlan(jStr):
    return json.loads(jStr, cls=OvmVlanDecoder)

def fromOvmVlan(vlan):
    return normalizeToGson(json.dumps(vlan, cls=OvmVlanEncoder))

class OvmBridgeDecoder(json.JSONDecoder):
    def decode(self, jStr):
        deDic = asciiLoads(jStr)
        bridge = OvmBridge()
        setAttrFromDict(bridge, 'name', deDic)
        setAttrFromDict(bridge, 'attach', deDic)
        return bridge

class OvmBridgeEncoder(json.JSONEncoder):
    def default(self, obj):
        if not isinstance(obj, OvmBridge): raise Exception("%s is not instance of OvmBridge"%type(obj))
        dct = {}
        safeDictSet(obj, dct, 'name')
        safeDictSet(obj, dct, 'attach')
        safeDictSet(obj, dct, 'interfaces')
        return dct
    
def toOvmBridge(jStr):
    return json.loads(jStr, cls=OvmBridgeDecoder)

def fromOvmBridge(bridge):
    return normalizeToGson(json.dumps(bridge, cls=OvmBridgeEncoder))

class OvmInterface(OvmObject):
    name = ''

class OvmVlan(OvmInterface):
    vid = 0
    pif = ''

class OvmBridge(OvmInterface):
    attach = ''
    interfaces = []

        
class OvmNetwork(OvmObject):
    '''
    Network
    '''

    @property
    def pifs(self):
        return self._getInterfaces("pif")

    @property
    def vlans(self):
        return self._getInterfaces("vlan")

    @property
    def bridges(self):
        return self._getInterfaces("bridge")
    
    def __init__(self):
        self.Parser = Parser()

    def _createVlan(self, vlan):
        """
        @param jsonString : parameter from client side
        @return : succ xxxxx
        ex. jsonString => {vid:100, pif:eth0}
        ex. return     => 
        """
        
        #Pre-condition
        #check Physical Interface Name
        if vlan.pif not in self.pifs.keys():    
            msg = "Physical Interface(%s) does not exist" % vlan.pif
            logger.debug(self._createVlan, msg)
            raise Exception(msg)

        #Pre-condition    
        #check Vlan Interface Name
        ifName = "%s.%s" % (vlan.pif, vlan.vid)
        if ifName in self.vlans.keys():
            msg = "Vlan Interface(%s) already exist, return it" % ifName
            logger.debug(self._createVlan, msg)
            return self.vlans[ifName]
            
        doCmd(['vconfig', 'add', vlan.pif, vlan.vid])
        self.bringUP(ifName)
        logger.debug(self._createVlan, "Create vlan %s successfully"%ifName)
        return self.vlans[ifName]
    
    def _deleteVlan(self, name):
        if name not in self.vlans.keys():
            raise Exception("No vlan device %s found"%name)
        
        vlan = self.vlans[name]
        self.bringDown(vlan.name)
        doCmd(['vconfig', 'rem', vlan.name])
        logger.debug(self._deleteVlan, "Delete vlan %s successfully"%vlan.name)
        
    
    def _createBridge(self, bridge):
        """
        @return : success
        ex. {bridge:xapi100, attach:eth0.100}
        create bridge interface, and attached it 
        cmd 1: ip link add bridge
        cmd 2: ip link set dev
        """
        
        if "xenbr" not in bridge.name and "vlan" not in bridge.name:
            raise Exception("Invalid bridge name %s. Bridge name must be in partten xenbr/vlan, e.g. xenbr0"%bridge.name)
        
        #pre-condition
        #check Bridge Interface Name
        if bridge.name in self.bridges.keys():
            msg = "Bridge(%s) already exist, return it" % bridge.name
            logger.debug(self._createBridge, msg)
            return self.bridges[bridge.name]

        #pre-condition
        #check attach must exist
        #possible to attach in PIF or VLAN 
        if bridge.attach not in self.vlans.keys() and bridge.attach not in self.pifs.keys():
            msg = "%s is not either pif or vlan" % bridge.attach
            logger.error(self._createBridge, msg)
            raise Exception(msg)

        doCmd(['ip', 'link', 'add', 'name', bridge.name, 'type', 'bridge'])
        doCmd(['ip', 'link', 'set', 'dev', bridge.attach, 'master', bridge.name])
        self.bringUP(bridge.name)
        logger.debug(self._createBridge, "Create bridge %s on %s successfully"%(bridge.name, bridge.attach))
        return self.bridges[bridge.name]
    
    def _getBridges(self):
        return self.bridges.keys()
    
    def _getVlans(self):
        return self.vlans.keys()
    
    def _deleteBridge(self, name):
        if name not in self.bridges.keys():
            raise Exception("Can not find bridge %s"%name)
        
        bridge = self.bridges[name]
        if bridge.attach in bridge.interfaces: bridge.interfaces.remove(bridge.attach)
        if len(bridge.interfaces) != 0:
            logger.debug(self._deleteBridge, "There are still some interfaces(%s) on bridge %s"%(bridge.interfaces, bridge.name))
            return False
        self.bringDown(bridge.name)
        doCmd(['ip', 'link', 'del', bridge.name])
        logger.debug(self._deleteBridge, "Delete bridge %s successfully"%bridge.name)
        return True
        
    def _getInterfaces(self, type):
        """
        @param type : ["pif", "bridge", "tap"]
        @return : dictionary of Interface Objects
        get All Interfaces based on type
        """
        devices = os.listdir('/sys/class/net')
        ifs = {}
        if type == "pif":
            devs = self.Parser.findall(Filter.Network.IFNAME_PIF, devices)
            for dev in set(devs):
                ifInst = OvmInterface()
                ifInst.name = dev
                ifs[dev] = ifInst
                
        elif type == "vlan":
            devs = self.Parser.findall(Filter.Network.IFNAME_VLAN, devices)
            for dev in set(devs):
                ifInst = OvmVlan()
                ifInst.name = dev
                (pif, vid) = dev.split('.')
                ifInst.pif = pif
                ifInst.vid = vid
                ifs[dev] = ifInst
                 
        elif type == "bridge":
            devs = self.Parser.findall(Filter.Network.IFNAME_BRIDGE, devices)
            for dev in set(devs):
                ifInst = OvmBridge()
                ifInst.name = dev
                devs = os.listdir(join('/sys/class/net', dev, 'brif'))
                ifInst.interfaces = devs
                attches = self.Parser.findall(Filter.Network.IFNAME_PIF, devs) + self.Parser.findall(Filter.Network.IFNAME_VLAN, devs)
                if len(attches) > 1: raise Exception("Multiple PIF on bridge %s (%s)"%(dev, attches))
                elif len(attches) == 0: ifInst.attach = "null"
                elif len(attches) == 1: ifInst.attach = attches[0]
                ifs[dev] = ifInst

        return ifs
    
    def bringUP(self, ifName):
        doCmd(['ifconfig', ifName, 'up'])
    
    def bringDown(self, ifName):
        doCmd(['ifconfig', ifName, 'down'])
           
    @staticmethod
    def createBridge(jStr):
        try:
            network = OvmNetwork()
            network._createBridge(toOvmBridge(jStr))
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.createBridge, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.createBridge), errmsg)
    
    @staticmethod
    def deleteBridge(name):
        try:
            network = OvmNetwork()
            network._deleteBridge(name)
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.deleteBridge, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.deleteBridge), errmsg)
    
    @staticmethod
    def getAllBridges():
        try:
            network = OvmNetwork()
            rs = toGson(network._getBridges())
            logger.debug(OvmNetwork.getAllBridges, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.getAllBridges, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.getAllBridges), errmsg)
    
    @staticmethod
    def getBridgeByIp(ip):
        try:
            routes = doCmd(['ip', 'route']).split('\n')
            brName = None
            for r in routes:
                if ip in r and "xenbr" in r or "vlan" in r:
                    brName = r.split(' ')[2]
                    break
            if not brName: raise Exception("Cannot find bridge with IP %s"%ip)
            logger.debug(OvmNetwork.getBridgeByIp, "bridge:%s, ip:%s"%(brName, ip))
            return toGson({"bridge":brName})
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.getBridgeByIp, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.getBridgeByIp), errmsg)              
    
    @staticmethod
    def getVlans():
        try:
            network = OvmNetwork()
            rs = toGson(network._getVlans())
            logger.debug(OvmNetwork.getVlans, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.getVlans, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.getVlans), errmsg)                        
    
    @staticmethod
    def createVlan(jStr):
        try:
            network = OvmNetwork()
            vlan = network._createVlan(toOvmVlan(jStr))
            rs = fromOvmVlan(vlan)
            logger.debug(OvmNetwork.createVlan, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.createVlan, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.createVlan), errmsg)
    
    @staticmethod
    def createVlanBridge(bridgeDetails, vlanDetails):
        try:
            network = OvmNetwork()
            v = toOvmVlan(vlanDetails)
            b = toOvmBridge(bridgeDetails)
            vlan = network._createVlan(v)
            b.attach = vlan.name
            network._createBridge(b)
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.createVlanBridge, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.createVlanBridge), errmsg)
    
    @staticmethod
    def deleteVlanBridge(name):
        try:
            network = OvmNetwork()
            if name not in network.bridges.keys():
                logger.debug(OvmNetwork.deleteVlanBridge, "No bridge %s found"%name)
                return SUCC()
            
            bridge = network.bridges[name]
            vlanName = bridge.attach
            if network._deleteBridge(name):
                if vlanName != "null":
                    network._deleteVlan(vlanName)
                else:
                    logger.warning(OvmNetwork.deleteVlanBridge, "Bridge %s has no vlan device"%name)
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.deleteVlanBridge, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.deleteVlanBridge), errmsg)
    
    @staticmethod
    def getBridgeDetails(name):
        try:
            network = OvmNetwork()
            if name not in network.bridges.keys():
                raise Exception("No bridge %s found"%name)
            bridge = network.bridges[name]
            rs = fromOvmBridge(bridge)
            logger.debug(OvmNetwork.getBridgeDetails, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.getBridgeDetails, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.getBridgeDetails), errmsg)
           
    @staticmethod
    def deleteVlan(name):
        try:
            network = OvmNetwork()
            network._deleteVlan(name)
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmNetwork.deleteVlan, errmsg)
            raise XmlRpcFault(toErrCode(OvmNetwork, OvmNetwork.deleteVlan), errmsg)                      
        
if __name__ == "__main__":
    try:
        OvmNetwork.getBridgeDetails(sys.argv[1])
        #=======================================================================
        # txt = json.dumps({"vid":104, "pif":"eth0"})
        # txt2 = json.dumps({"name":"xapi3", "attach":"eth0.104"})
        # print nw.createVlan(txt)
        # print nw.createBridge(txt2)
        # 
        # nw.deleteBridge("xapi3")
        # nw.deleteVlan("eth0.104")
        #=======================================================================
        
    except Exception, e:
        print e
