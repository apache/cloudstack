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
import types
from OvmCommonModule import *
from xmlrpclib import Fault
from OVSCommons import *
import OvmFaultConstants

import OvmHostModule
import OvmStoragePoolModule
import OvmVmModule
import OvmNetworkModule
import OvmVolumeModule
import OvmSecurityGroupModule
from OvmHaHeartBeatModule import OvmHaHeartBeat

ExposedClass = {}
logger = OvmLogger('OvmDispatcher')
def InitOvmDispacther():
    global ExposedClass
    modules = [ eval(attr) for attr in globals() if isinstance(eval(attr), types.ModuleType) ]
    for m in modules:
        for name in dir(m):
            clz = getattr(m, name)
            if type(clz) is types.TypeType and issubclass(clz, OvmObject):
                ExposedClass[name] = clz
    logger.debug(InitOvmDispacther, "Discovered exposed class:\n\n%s"%"\n".join(ExposedClass))

@exposed
def OvmDispatch(methodName, *params):
    global ExposedClass
    p = methodName.split('.')
    if len(p) != 2:
        logger.error(OvmDispatch, "%s is not a vaild format, should be classname.methodname"%p)
        raise Fault(dispatchErrCode('InvalidCallMethodFormat'), "%s is not a vaild format, should be classname.methodname"%p)
    clzName = p[0]
    funcName = p[1]
    if clzName not in ExposedClass.keys():
        logger.error(OvmDispatch, "class %s is not exposed by agent"%clzName)
        raise Fault(dispatchErrCode('InvaildClass'), "class %s is not exposed by agent"%clzName)
    clz = ExposedClass[clzName]
    if not hasattr(clz, funcName):
        logger.error(OvmDispatch, "class %s has no function %s"%(clzName, funcName))
        raise Fault(dispatchErrCode('InvaildFunction'), "class %s has no function %s"%(clzName, funcName))
    logger.debug(OvmDispatch, "Entering %s.%s ===>"%(clzName, funcName))
    rs = getattr(clz, funcName)(*params)
    logger.debug(OvmDispatch, "Exited %s.%s <==="%(clzName, funcName))
    return rs
