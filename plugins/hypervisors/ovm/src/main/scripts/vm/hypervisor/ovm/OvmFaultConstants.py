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
OvmDispatcherStub = 0
OvmHostErrCodeStub = 1000
OvmVmErrCodeStub = 2000
OvmStoragePoolErrCodeStub = 3000
OvmNetworkErrCodeStub = 4000
OvmVolumeErrCodeStub = 5000
OvmOCFS2ErrCodeStub = 6000

class NoVmFoundException(Exception):
    pass

class ShellExceutedFailedException(Exception):
    stderr = ''
    errCode = -1000
    
    def __init__(self, err, code):
        Exception.__init__(self, "%s, return code:%s"%(err, code))
        self.stderr = err
        self.errCode = code

errCode = {
       # OvmDispatch is not class, these error codes are reserved
       "OvmDispatch.InvalidCallMethodFormat":OvmDispatcherStub+1,
       "OvmDispatch.InvaildClass":OvmDispatcherStub+2,
       "OvmDispatch.InvaildFunction":OvmDispatcherStub+3,
       "OvmVm.reboot":OvmDispatcherStub+4,
        
       "OvmHost.registerAsPrimary":OvmHostErrCodeStub+1,
       "OvmHost.registerAsVmServer":OvmHostErrCodeStub+2,
       "OvmHost.ping":OvmHostErrCodeStub+3,
       "OvmHost.getDetails":OvmHostErrCodeStub+4,
       "OvmHost.getPerformanceStats":OvmHostErrCodeStub+5,
       "OvmHost.getAllVms":OvmHostErrCodeStub+6,
       "OvmHost.fence":OvmHostErrCodeStub+7,
       "OvmHost.setupHeartBeat":OvmHostErrCodeStub+8,
       "OvmHost.pingAnotherHost":OvmHostErrCodeStub+9,
       
       "OvmVm.create":OvmVmErrCodeStub+1,
       "OvmVm.stop":OvmVmErrCodeStub+2,
       "OvmVm.getDetails":OvmVmErrCodeStub+3,
       "OvmVm.getVmStats":OvmVmErrCodeStub+4,
       "OvmVm.migrate":OvmVmErrCodeStub+5,
       "OvmVm.register":OvmVmErrCodeStub+6,
       "OvmVm.getVncPort":OvmVmErrCodeStub+7,
       "OvmVm.detachOrAttachIso":OvmVmErrCodeStub+8,
       
       "OvmStoragePool.create":OvmStoragePoolErrCodeStub+1,
       "OvmStoragePool.getDetailsByUuid":OvmStoragePoolErrCodeStub+2,
       "OvmStoragePool.downloadTemplate":OvmStoragePoolErrCodeStub+3,
       "OvmStoragePool.prepareOCFS2Nodes":OvmStoragePoolErrCodeStub+4,
       "OvmStoragePool.createTemplateFromVolume":OvmStoragePoolErrCodeStub+5,
       "OvmStoragePool._umount":OvmStoragePoolErrCodeStub+6,
       "OvmStoragePool.copyVolume":OvmStoragePoolErrCodeStub+7,
       "OvmStoragePool.delete":OvmStoragePoolErrCodeStub+8,
       
       "OvmNetwork.createBridge":OvmNetworkErrCodeStub+1,
       "OvmNetwork.deleteBridge":OvmNetworkErrCodeStub+2,
       "OvmNetwork.createVlan":OvmNetworkErrCodeStub+3,
       "OvmNetwork.deleteVlan":OvmNetworkErrCodeStub+4,
       "OvmNetwork.getAllBridges":OvmNetworkErrCodeStub+5,
       "OvmNetwork.getBridgeByIp":OvmNetworkErrCodeStub+6,
       "OvmNetwork.createVlanBridge":OvmNetworkErrCodeStub+7,
       "OvmNetwork.deleteVlanBridge":OvmNetworkErrCodeStub+8,
       
       "OvmVolume.createDataDisk":OvmVolumeErrCodeStub+1,
       "OvmVolume.createFromTemplate":OvmVolumeErrCodeStub+2,
       "OvmVolume.destroy":OvmVolumeErrCodeStub+3,
       
       "OvmOCFS2._addNode":OvmOCFS2ErrCodeStub+1,
}


def toErrCode(clz, func):
    global errCode
    if not callable(func): raise Exception("%s is not a callable, cannot get error code"%func)
    name = clz.__name__ + '.' + func.__name__
    if name not in errCode.keys(): return -1
    return errCode[name]

def dispatchErrCode(funcName):
    name = "OvmDispatch." + funcName
    if name not in errCode.keys(): return -1
    return errCode[name]
