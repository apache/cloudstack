'''
Created on May 17, 2011

@author: frank
'''

try:
    import json
except ImportError:
    import simplejson as json
    
from OvmObjectModule import *
import types
import logging
import popen2
from OvmFaultConstants import toErrCode, dispatchErrCode, NoVmFoundException
from xmlrpclib import Fault as XmlRpcFault
from OVSCommons import *
from OvmLoggerModule import OvmLogger
from OVSXXenStore import xen_get_vm_path

HEARTBEAT_TIMESTAMP_FORMAT='<timestamp>%s</timestamp>'
HEARTBEAT_TIMESTAMP_PATTERN='(\<timestamp\>\d+.\d+<\/timestamp\>)'
HEARTBEAT_DIR='heart_beat'
ETC_HOSTS='/etc/hosts'
HOSTNAME_FILE='/etc/sysconfig/network'

logger = OvmLogger('OvmCommon')

def setAttrFromDict(obj, name, refDict, convertFunc=None):
    if not convertFunc:
        setattr(obj, name, refDict[name])
    else:
        setattr(obj, name, convertFunc(refDict[name]))

def safeSetAttr(obj, name, value):
    if not hasattr(obj, name): raise Exception("%s doesn't have attribute %s"%(obj.__class__.__name__, name))
    setattr(obj, name, value)
    
def toAscii(jstr):
    return str(jstr).encode('ascii', 'ignore')

def toAsciiHook(dct):
    for k in dct:
        v = dct[k]
        if type(v) is types.UnicodeType:
            v = toAscii(v)
        del dct[k]
        k = toAscii(k)
        dct[k] = v
    return dct
    
def asciiLoads(jStr):
    jStr = str(jStr).replace("'", '"').replace('False', 'false').replace('True', 'true')
    return json.loads(jStr, object_hook=toAsciiHook)

def exceptionIfNoSuccess(str, errMsg=None):
    if not errMsg: errMsg = str
    if not "success" in str: raise Exception("%s (%s)"%(errMsg, str))

def successToMap(str, sep=';'):
    if not str.startswith("success"): raise Exception(str)
    str = str[len('success:'):]
    dct = {}
    for pair in str.split(sep):
        (key, value) = pair.split('=', 1)
        dct[key] = value
    return dct

def jsonSuccessToMap(str):
    dct = json.loads(str)
    if dct['status'] != 'SUCC': raise Exception(str)
    return dct['value']
    
def safeDictSet(obj, dct, name):
    if not hasattr(obj, name): raise Exception("%s has no attribute %s for encoding"%(obj.__class__.__name__, name))
    dct[name] = getattr(obj, name)

def normalizeToGson(str):
    return str.replace('\\', '').strip('"').replace('"{', '{').replace('}"', '}');

def toGson(obj):
    return normalizeToGson(json.dumps(obj))

def MtoBytes(M):
    return M * 1024 * 1024

def BytesToM(bytes):
    return bytes/(1024*1024)

def BytesToG(bytes):
    return bytes/(1024*1024*1024)

def doCmd(lst):
    cmds = [str(i) for i in lst]
    logger.debug(doCmd, ' '.join(cmds))
    res = run_cmd(cmds)
    logger.debug(doCmd, 'result:' + res)
    return res

def execute(cmd):
    p = popen2.Popen3(cmd, True)
    if (p.wait() != 0):
        raise Exception("Failed to execute command. Command: " + cmd + ", Error: " + p.childerr.read())
    return p.fromchild.read()

def getDomId(vm_name):
    return execute("xm list | grep " + vm_name + " | awk '{print $2}'").strip()
    
def raiseExceptionIfFail(res):
    if not "success" in res and not "SUCC" in res: raise Exception(res)

def ipToHeartBeatFileName(ip):
    return ip.replace('.', '_') + "_HEARTBEAT"

def getVmNameFromConfigureFile(cfgPath):
    fd = open(cfgPath)
    for i in fd.readlines():
        i = i.strip()
        if i.startswith('name'):
            (key, value) = i.split("=", 1)
            value = value.strip().strip("'")
            fd.close()
            return value
    fd.close()
    raise Exception('Cannot find vm name in %s'%cfgPath)
    
