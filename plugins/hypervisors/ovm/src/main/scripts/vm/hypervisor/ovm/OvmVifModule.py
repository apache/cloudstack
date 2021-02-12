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
