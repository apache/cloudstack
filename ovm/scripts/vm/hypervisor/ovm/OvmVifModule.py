'''
Created on May 17, 2011

@author: frank
'''
from OvmCommonModule import *

class OvmVifDecoder(json.JSONDecoder):
    def decode(self, jStr):
        deDict = asciiLoads(jStr)
        vif = OvmVif()
        vif.mac = deDict['mac']
        vif.bridge = deDict['bridge']
        return vif
    
class OvmVifEncoder(json.JSONEncoder):
    def default(self, obj):
        if not isinstance(obj, OvmVif): raise Exception("%s is not instance of OvmVif"%type(obj))
        dct = {}
        safeDictSet(obj, dct, 'mac')
        safeDictSet(obj, dct, 'bridge')
        safeDictSet(obj, dct, 'type')
        safeDictSet(obj, dct, 'name')
        return dct    

def fromOvmVif(vif):
    return normalizeToGson(json.dumps(vif, cls=OvmVifEncoder))

def fromOvmVifList(vifList):
    return [fromOvmVif(v) for v in vifList]

def toOvmVif(jStr):
    return json.loads(jStr, cls=OvmVifDecoder)

def toOvmVifList(jStr):
    vifs = []
    for i in jStr:
        vif = toOvmVif(i)
        vifs.append(vif)
    return vifs

class OvmVif(OvmObject):
    name = ''
    mac = ''
    bridge = ''
    type = ''
    mode = ''
    
    def toXenString(self):
        return "%s,%s,%s"%(self.mac, self.bridge, self.type)
