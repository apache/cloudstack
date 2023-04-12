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

try:
    import json
except ImportError:
    import simplejson as json

from OvmObjectModule import *
import types
import logging
import popen2
import subprocess
from OvmFaultConstants import toErrCode, dispatchErrCode, NoVmFoundException, ShellExceutedFailedException
from xmlrpclib import Fault as XmlRpcFault
from OVSCommons import *
from OvmLoggerModule import OvmLogger
from OVSXXenStore import xen_get_vm_path
from OVSSiteRMServer import get_master_ip

HEARTBEAT_TIMESTAMP_FORMAT='<timestamp>%s</timestamp>'
HEARTBEAT_TIMESTAMP_PATTERN='(\<timestamp\>\d+.\d+<\/timestamp\>)'
HEARTBEAT_DIR='heart_beat'
ETC_HOSTS='/etc/hosts'
HOSTNAME_FILE='/etc/sysconfig/network'
OWNER_FILE_PREFIX='host_'
OCFS2_CONF='/etc/ocfs2/cluster.conf'

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

def runCmd(cmds):
    process = subprocess.Popen(cmds, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()
    if process.returncode != 0:
        raise ShellExceutedFailedException(stderr, process.returncode)
    return stdout

def doCmd(lst):
    cmds = [str(i) for i in lst]
    cmdStr = ' '.join(cmds)
    logger.debug(doCmd, cmdStr)
    res = runCmd(cmdStr)
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

def makeOwnerFileName():
    hostIp = successToMap(get_master_ip())['ip']
    ownerFileName = OWNER_FILE_PREFIX + hostIp.replace('.', '_')
    return ownerFileName
