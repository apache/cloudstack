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
Created on June 2, 2011

'''
from OvmCommonModule import *
from OvmStoragePoolModule import OvmStoragePool
from OVSXUtility import xen_create_disk
from OvmHostModule import OvmHost
import os

logger = OvmLogger("OvmVolume")

class OvmVolumeDecoder(json.JSONDecoder):
    def decode(self, jStr):
        deDict = asciiLoads(jStr)
        vol = OvmVolume()
        setAttrFromDict(vol, 'uuid', deDict)
        setAttrFromDict(vol, 'size', deDict, long)
        setAttrFromDict(vol, 'poolUuid', deDict)
        return vol
    
class OvmVolumeEncoder(json.JSONEncoder):
    def default(self, obj):
        if not isinstance(obj, OvmVolume): raise Exception("%s is not instance of OvmVolume"%type(obj))
        dct = {}
        safeDictSet(obj, dct, 'name')
        safeDictSet(obj, dct, 'uuid')
        safeDictSet(obj, dct, 'poolUuid')
        safeDictSet(obj, dct, 'path')
        safeDictSet(obj, dct, 'size')
        return dct
    
def toOvmVolume(jStr):
    return json.loads(jStr, cls=OvmVolumeDecoder)

def fromOvmVolume(vol):
    return normalizeToGson(json.dumps(vol, cls=OvmVolumeEncoder))

class OvmVolume(OvmObject):
    name = ''
    uuid = ''
    poolUuid = ''
    path = ''
    size = 0
    
    @staticmethod
    def createDataDisk(poolUuid, size, isRoot):
        try:
            vol = OvmVolume()
            vol.size = long(size)
            vol.poolUuid = poolUuid
            pool = OvmStoragePool()
            sr = pool._getSrByNameLable(vol.poolUuid)
            if isRoot:
                path = join(sr.mountpoint, 'running_pool', get_uuid())
            else:
                path = join(sr.mountpoint, 'shareDisk')
            if not exists(path): os.makedirs(path)
            freeSpace = pool._getSpaceinfoOfDir(path)
            if freeSpace < vol.size:
                raise Exception("%s has not enough space (available:%s, required:%s"%(path, freeSpace, vol.size))
            
            vol.uuid = get_uuid()
            vol.name = vol.uuid + '.raw'
            filePath = join(path, vol.name)
            exceptionIfNoSuccess(xen_create_disk(filePath, BytesToM(vol.size)), "Create datadisk %s failed"%filePath)
            vol.path = filePath
            rs = fromOvmVolume(vol)
            logger.debug(OvmVolume.createDataDisk, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVolume.createDataDisk, errmsg)
            raise XmlRpcFault(toErrCode(OvmVolume, OvmVolume.createDataDisk, errmsg))
        
    @staticmethod
    def createFromTemplate(poolUuid, templateUrl):
        try:
            if not exists(templateUrl):
                raise Exception("Cannot find template:%s"%templateUrl)
            sr = OvmStoragePool()._getSrByNameLable(poolUuid)
            volDirUuid = get_uuid()
            volUuid = get_uuid()
            priStorageMountPoint = sr.mountpoint
            volDir = join(priStorageMountPoint, 'running_pool', volDirUuid)
            if exists(volDir):
                raise Exception("Volume dir %s alreay existed, can not override"%volDir)
            os.makedirs(volDir)
            OvmStoragePool()._checkDirSizeForImage(volDir, templateUrl)
            volName = volUuid + '.raw'
            tgt = join(volDir, volName)
            cpVolCmd = ['cp', templateUrl, tgt]
            doCmd(cpVolCmd)
            volSize = os.path.getsize(tgt)
            vol = OvmVolume()
            vol.name = volName
            vol.path = tgt
            vol.size = volSize
            vol.uuid = volUuid
            vol.poolUuid = poolUuid
            rs = fromOvmVolume(vol)
            logger.debug(OvmVolume.createFromTemplate, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVolume.createFromTemplate, errmsg)
            raise XmlRpcFault(toErrCode(OvmVolume, OvmVolume.createFromTemplate), errmsg)
    
    @staticmethod
    def destroy(poolUuid, path):
        try:
            OvmStoragePool()._getSrByNameLable(poolUuid)
            if not exists(path): raise Exception("Cannot find %s"%path)
            dir = dirname(path)
            if exists(join(dir, 'vm.cfg')):
                # delete root disk
                vmNamePath = join(dir, 'vmName')
                if exists(vmNamePath):
                    vmNameFd = open(vmNamePath, 'r')
                    vmName = vmNameFd.readline()
                    vmName = vmName.rstrip('\n')
                    link = join(dirname(dir), vmName)
                    doCmd(['rm', '-rf', link])
                    vmNameFd.close()
                else:
                    logger.warning(OvmVolume.destroy, "Can not find vmName file in %s"%dir)
                doCmd(['rm','-rf', dir])
            else:
                doCmd(['rm', path])
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVolume.destroy, errmsg)
            raise XmlRpcFault(toErrCode(OvmVolume, OvmVolume.destroy), errmsg)
        
        
                              
if __name__ == "__main__":
    print OvmVolume.detachOrAttachIso(sys.argv[1], '', False)
                              
                              
                              
                              