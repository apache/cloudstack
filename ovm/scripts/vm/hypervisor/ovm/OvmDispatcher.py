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
