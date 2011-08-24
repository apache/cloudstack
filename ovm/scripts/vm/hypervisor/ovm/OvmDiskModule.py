'''
Created on May 17, 2011

@author: frank
'''

from OvmCommonModule import *

class OvmDiskDecoder(json.JSONDecoder):
    def decode(self, jStr):
        deDict = asciiLoads(jStr)
        disk = OvmDisk()
        setAttrFromDict(disk, 'path', deDict)
        setAttrFromDict(disk, 'type', deDict)
        setAttrFromDict(disk, 'isIso', deDict)
        return disk

class OvmDiskEncoder(json.JSONEncoder):
    def default(self, obj):
        if not isinstance(obj, OvmDisk): raise Exception("%s is not instance of OvmDisk"%type(obj))
        dct = {}
        safeDictSet(obj, dct, 'path')
        safeDictSet(obj, dct, 'type')
        return dct    

def fromOvmDisk(disk):
    return normalizeToGson(json.dumps(disk, cls=OvmDiskEncoder))

def fromOvmDiskList(diskList):
    return [fromOvmDisk(d) for d in diskList]

def toOvmDisk(jStr):
    return json.loads(jStr, cls=OvmDiskDecoder)

def toOvmDiskList(jStr):
    disks = []
    for i in jStr:
        d = toOvmDisk(i)
        disks.append(d)
    return disks

class OvmDisk(OvmObject):
    path = ''
    type = ''
    isIso = False
   
    
if __name__ == "__main__":
    print toOvmDisk('''{"type":"w","path":"/data/data.raw"}''')
    print toOvmDisk('''{"path":"/data/data.raw'","type":"w"}''')