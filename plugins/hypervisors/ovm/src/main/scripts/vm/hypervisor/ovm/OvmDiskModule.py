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
'''
Created on May 17, 2011

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
