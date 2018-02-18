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
from OvmCommonModule import *
from OVSSiteSR import sp_create, sr_create, sr_do
from OVSParser import parse_ocfs2_cluster_conf
from OVSXCluster import clusterm_set_ocfs2_cluster_conf, clusterm_start_o2cb_service
from OVSSiteRMServer import get_master_ip
from OvmOCFS2Module import OvmOCFS2
import re

class OvmStoragePoolDecoder(json.JSONDecoder):
    def decode(self, jStr):
        dct = asciiLoads(jStr)
        pool = OvmStoragePool()
        setAttrFromDict(pool, 'uuid', dct)
        setAttrFromDict(pool, 'type', dct)
        setAttrFromDict(pool, 'path', dct)
        return pool

class OvmStoragePoolEncoder(json.JSONEncoder):
    def default(self, obj):
        if not isinstance(obj, OvmStoragePool): raise Exception("%s is not instance of OvmStoragePool"%type(obj))
        dct = {}
        safeDictSet(obj, dct, 'uuid')
        safeDictSet(obj, dct, 'type')
        safeDictSet(obj, dct, 'path')
        safeDictSet(obj, dct, 'mountPoint')
        safeDictSet(obj, dct, 'totalSpace')
        safeDictSet(obj, dct, 'freeSpace')
        safeDictSet(obj, dct, 'usedSpace')
        return dct

def fromOvmStoragePool(pool):
    return normalizeToGson(json.dumps(pool, cls=OvmStoragePoolEncoder))

def toOvmStoragePool(jStr):
    return json.loads(jStr, cls=OvmStoragePoolDecoder)

logger = OvmLogger('OvmStoragePool')   
class OvmStoragePool(OvmObject):
    uuid = ''
    type = ''
    path = ''
    mountPoint = ''
    totalSpace = 0
    freeSpace = 0
    usedSpace = 0

    def _getSrByNameLable(self, poolUuid):
        d = db_dump('sr')
        for uuid, sr in d.items():
            if sr.name_label == poolUuid:
                return sr
    
        raise Exception("No SR matching to %s" % poolUuid)
    
    def _getSpaceinfoOfDir(self, dir):
        stat = os.statvfs(dir)
        freeSpace = stat.f_frsize * stat.f_bavail;
        totalSpace = stat.f_blocks * stat.f_frsize;
        return (totalSpace, freeSpace)
    
    def _checkDirSizeForImage(self, dir, image):
        (x, free_storage_size) = OvmStoragePool()._getSpaceinfoOfDir(dir)
        image_size = os.path.getsize(image)
        if image_size > (free_storage_size + 1024 * 1024 * 1024):
            raise Exception("No space on dir %s (free storage:%s, vm size:%s)"%(dir, free_storage_size, image_size))
         
    def _getAllMountPoints(self):
        mps = []
        d = db_dump('sr')
        for uuid, sr in d.items():
            mps.append(sr.mountpoint)
        return mps
    
    def _isMounted(self, path):
        res = doCmd(['mount'])
        return (path in res)
    
    def _mount(self, target, mountpoint, readonly=False):
        if not exists(mountpoint):
            os.makedirs(mountpoint)
            
        if not OvmStoragePool()._isMounted(mountpoint):
            if readonly:
                doCmd(['mount', target, mountpoint, '-r'])
            else:
                doCmd(['mount', target, mountpoint])
    
    def _umount(self, mountpoint):
        umountCmd = ['umount', '-f', mountpoint]
        doCmd(umountCmd)
        ls = os.listdir(mountpoint)
        if len(ls) == 0:
            rmDirCmd = ['rm', '-r', mountpoint]
            doCmd(rmDirCmd)
        else:
            logger.warning(OvmStoragePool._umount, "Something wrong when umount %s, there are still files in directory:%s", mountpoint, " ".join(ls))
         
    @staticmethod
    def create(jStr):
        try:
            pool = toOvmStoragePool(jStr)
            logger.debug(OvmStoragePool.create, fromOvmStoragePool(pool))
            spUuid = jsonSuccessToMap(sp_create(pool.type, pool.path))['uuid']
            srUuid = jsonSuccessToMap(sr_create(spUuid, name_label=pool.uuid))['uuid']
            sr_do(srUuid, "initialize")
            rs = SUCC()
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmStoragePool.create, errmsg)
            raise XmlRpcFault(toErrCode(OvmStoragePool, OvmStoragePool.create), errmsg)
    
    @staticmethod
    def getDetailsByUuid(uuid):
        try:
            sr = OvmStoragePool()._getSrByNameLable(uuid)
            pool = OvmStoragePool()
            safeSetAttr(pool, 'uuid', uuid)
            #Note: the sr.sp.fs_type is not mapped to its class name which we use in mgmt server
            safeSetAttr(pool, 'type', sr.sp.__class__.__name__)
            safeSetAttr(pool, 'path', sr.sp.get_fs_spec())
            safeSetAttr(pool, 'mountPoint', sr.mountpoint)
            (totalSpace, freeSpace) = OvmStoragePool()._getSpaceinfoOfDir(sr.mountpoint)
            safeSetAttr(pool, 'totalSpace', totalSpace)
            safeSetAttr(pool, 'freeSpace', freeSpace)
            safeSetAttr(pool, 'usedSpace', totalSpace - freeSpace)
            res = fromOvmStoragePool(pool)
            logger.debug(OvmStoragePool.getDetailsByUuid, res)
            return res
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmStoragePool.getDetailsByUuid, errmsg)
            raise XmlRpcFault(toErrCode(OvmStoragePool, OvmStoragePool.getDetailsByUuid), errmsg)
    
    @staticmethod
    def downloadTemplate(uuid, secPath):
        secMountPoint = None
        try:
            logger.debug(OvmStoragePool.downloadTemplate, "download %s to pool %s"%(secPath, uuid))
            try:
                tmpUuid = get_uuid()
                secMountPoint = join("/var/cloud/", tmpUuid)
                if not exists(secMountPoint):
                    os.makedirs(secMountPoint)
    
                templateFile = None
                if secPath.endswith("raw"):
                    secPathDir = os.path.dirname(secPath)
                    templateFile = os.path.basename(secPath)
                else:
                    secPathDir = secPath
                    
                # mount as read-only
                mountCmd = ['mount.nfs', secPathDir, secMountPoint, '-r']
                doCmd(mountCmd)
    
                if not templateFile:
                    for f in os.listdir(secMountPoint):
                        if isfile(join(secMountPoint, f)) and f.endswith('raw'):
                            templateFile = f
                            break    
        
                if not templateFile:
                    raise Exception("Can not find raw template in secondary storage")
                templateSecPath = join(secMountPoint, templateFile)
    
                sr = OvmStoragePool()._getSrByNameLable(uuid)
                priStorageMountPoint = sr.mountpoint
                # Although mgmt server will check the size, we check again for safety
                OvmStoragePool()._checkDirSizeForImage(priStorageMountPoint, templateSecPath)
                seedDir = join(priStorageMountPoint, 'seed_pool', tmpUuid)
                if exists(seedDir):
                    raise Exception("%s already here, cannot override existing template" % seedDir)
                os.makedirs(seedDir)
    
                tgt = join(seedDir, templateFile)
                cpTemplateCmd = ['cp', templateSecPath, tgt]
                logger.info(OvmStoragePool.downloadTemplate, " ".join(cpTemplateCmd))
                doCmd(cpTemplateCmd)
                templateSize = os.path.getsize(tgt) 
                logger.info(OvmStoragePool.downloadTemplate, "primary_storage_download success:installPath:%s, templateSize:%s"%(tgt,templateSize))
                rs = toGson({"installPath":tgt, "templateSize":templateSize})
                return rs
            except Exception, e:
                errmsg = fmt_err_msg(e)
                logger.error(OvmStoragePool.downloadTemplate, errmsg)
                raise XmlRpcFault(toErrCode(OvmStoragePool, OvmStoragePool.downloadTemplate), errmsg)
        finally:
            if exists(secMountPoint):
                try:
                    OvmStoragePool()._umount(secMountPoint)
                except Exception, e:
                    errmsg = fmt_err_msg(e)
                    logger.error(OvmStoragePool.downloadTemplate, 'unmount secondary storage at %s failed, %s'%(secMountPoint, errmsg))

    @staticmethod
    def prepareOCFS2Nodes(clusterName, nodeString):        
        def configureEtcHosts(nodes):
            if not exists(ETC_HOSTS):
                orignalConf = ""
            else:
                fd = open(ETC_HOSTS, "r")
                orignalConf = fd.read()
                fd.close()
            
            pattern = r"(.*%s.*)|(.*%s.*)"
            newlines = []
            for n in nodes:
                p = pattern % (n["ip_address"], n["name"])
                orignalConf = re.sub(p, "", orignalConf)
                newlines.append("%s\t%s\n"%(n["ip_address"], n["name"]))
            
            orignalConf = orignalConf + "".join(newlines)
            # remove extra empty lines
            orignalConf = re.sub(r"\n\s*\n*", "\n", orignalConf)
            logger.debug(OvmStoragePool.prepareOCFS2Nodes, "Configure /etc/hosts:%s\n"%orignalConf)
            fd = open(ETC_HOSTS, "w")
            fd.write(orignalConf)
            fd.close()
        
        def configureHostName(nodes):
            myIp = successToMap(get_master_ip())['ip']
            nodeName = None
            for n in nodes:
                if myIp == n["ip_address"]:
                    nodeName = n["name"]
                    break
            
            if nodeName == None: raise Exception("Cannot find node equals to my ip address:%s"%myIp)
            if not exists(HOSTNAME_FILE):
                originalConf = ""
            else:
                fd = open(HOSTNAME_FILE, "r")
                originalConf = fd.read()
                fd.close()
            
            pattern = r"HOSTNAME=(.*)"
            # remove any old hostname
            originalConf = re.sub(pattern, "", originalConf)
            # remove extra empty lines
            originalConf = re.sub(r"\n\s*\n*", "\n", originalConf) + "\n" + "HOSTNAME=%s"%nodeName
            logger.debug(OvmStoragePool.prepareOCFS2Nodes, "Configure %s:%s\n"%(HOSTNAME_FILE,originalConf))
            fd = open(HOSTNAME_FILE, "w")
            fd.write(originalConf)
            fd.close()
            doCmd(['hostname', nodeName])
        
        def addNodes(nodes, clusterName):
            ocfs2 = OvmOCFS2()
            ocfs2._load()
            isOnline = ocfs2._isClusterOnline(clusterName)
            if not isOnline:
                ocfs2._prepareConf(clusterName)
            
            for n in nodes:
                ocfs2._addNode(n['name'], n['number'], n['ip_address'], 7777, clusterName, isOnline)
            
        def checkStaleCluster(clusterName):
            if exists('/sys/kernel/config/cluster/'):
                dirs = os.listdir('/sys/kernel/config/cluster/')
                for dir in dirs:
                    if dir != clusterName:
                        errMsg = '''CloudStack detected there is a stale cluster(%s) on host %s. Please manually clean up it first then add again by
1) remove the host from cloudstack 
2) umount all OCFS2 device on host
3) /etc/init.d/o2cb offline %s
4) /etc/init.d/o2cb restart
if this doesn't resolve the problem, please check oracle manual to see how to offline a cluster
    ''' % (dir, successToMap(get_master_ip())['ip'], dir)
                        raise Exception(errMsg)
            
        try:
            checkStaleCluster(clusterName)
            nodeString = nodeString.strip(";")
            nodes = []
            for n in nodeString.split(";"):
                params = n.split(":")
                if len(params) != 3: raise Exception("Wrong parameter(%s) in node string(%s)"%(n, nodeString))
                dict = {"number":params[0], "ip_address":params[1], "name":params[2]}
                nodes.append(dict)
            
            if len(nodes) > 255:
                raise Exception("%s nodes beyond maximum 255 allowed by OCFS2"%len(nodes))
            
            configureHostName(nodes)
            configureEtcHosts(nodes)
            addNodes(nodes, clusterName)
            OvmOCFS2()._start(clusterName)
            fd = open(OCFS2_CONF, 'r')
            conf = fd.readlines()
            fd.close()
            logger.debug(OvmStoragePool.prepareOCFS2Nodes, "Configure cluster.conf to:\n%s"%' '.join(conf))
            rs = SUCC()
            return rs
        
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmStoragePool.prepareOCFS2Nodes, errmsg)
            raise XmlRpcFault(toErrCode(OvmStoragePool, OvmStoragePool.prepareOCFS2Nodes), errmsg)
    
    @staticmethod
    def createTemplateFromVolume(secStorageMountPath, installPath, volumePath):
        try:
            secMountPoint = ""
            if not isfile(volumePath): raise Exception("Cannot find %s"%volumePath)
            vmCfg = join(dirname(volumePath), 'vm.cfg')
            vmName = getVmNameFromConfigureFile(vmCfg)
            if vmName in doCmd(['xm', 'list']):
                raise Exception("%s is still running, please stop it first then create template again"%vmName)
            
            tmpUuid = get_uuid()
            secMountPoint = join("/var/cloud/", tmpUuid)
            OvmStoragePool()._mount(secStorageMountPath, secMountPoint)
            installPath = installPath.lstrip('/')
            destPath = join(secMountPoint, installPath)
            #This prevent us deleting whole secondary in case we got a wrong installPath
            if destPath == secMountPoint: raise Exception("Install path equals to root of secondary storage(%s)"%destPath)
            if exists(destPath):
                logger.warning(OvmStoragePool.createTemplateFromVolume, "%s is already here, delete it since it is most likely stale"%destPath)
                doCmd(['rm', '-rf', destPath])
            OvmStoragePool()._checkDirSizeForImage(secMountPoint, volumePath)
            
            os.makedirs(destPath)
            newName = get_uuid() + ".raw"
            destName = join(destPath, newName)
            doCmd(['cp', volumePath, destName])
            size = os.path.getsize(destName)
            resInstallPath = join(installPath, newName)
            OvmStoragePool()._umount(secMountPoint)
            rs = toGson({"installPath":resInstallPath, "templateFileName":newName, "virtualSize":size, "physicalSize":size})
            return rs
        
        except Exception, e:
            try:
                if exists(secMountPoint):
                    OvmStoragePool()._umount(secMountPoint)
            except Exception, e:
                logger.warning(OvmStoragePool.createTemplateFromVolume, "umount %s failed"%secMountPoint)       
                
            errmsg = fmt_err_msg(e)
            logger.error(OvmStoragePool.createTemplateFromVolume, errmsg)
            raise XmlRpcFault(toErrCode(OvmStoragePool, OvmStoragePool.createTemplateFromVolume), errmsg)
    
    @staticmethod
    def delete(uuid):
        try:
            sr = OvmStoragePool()._getSrByNameLable(uuid)
            primaryStoragePath = sr.mountpoint
            OvmStoragePool()._umount(primaryStoragePath)
            rs = SUCC()
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmStoragePool.delete, errmsg)
            raise XmlRpcFault(toErrCode(OvmStoragePool, OvmStoragePool.delete), errmsg) 
        
    @staticmethod
    def copyVolume(secStorageMountPath, volumeFolderOnSecStorage, volumePath, storagePoolUuid, toSec):
        def copyToSecStorage(secMountPoint, volumeFolderOnSecStorage, volumePath):
            if not isfile(volumePath): raise Exception("Cannot find volume at %s"%volumePath)
            OvmStoragePool()._checkDirSizeForImage(secMountPoint, volumePath)
            volumeFolderOnSecStorage = volumeFolderOnSecStorage.lstrip("/")
            destPath = join(secMountPoint, volumeFolderOnSecStorage)
            #This prevent us deleting whole secondary in case we got a wrong volumeFolderOnSecStorage
            if destPath == secMountPoint: raise Exception("volume path equals to root of secondary storage(%s)"%destPath)
            if exists(destPath):
                logger.warning(OvmStoragePool.copyVolume, "%s already exists, delete it first"%destPath)
                doCmd(['rm', '-rf', destPath])
            os.makedirs(destPath)
            newName = get_uuid() + ".raw"
            destName = join(destPath, newName)
            doCmd(['cp', volumePath, destName])
            return destName
        
        def copyToPrimary(secMountPoint, volumeFolderOnSecStorage, volumePath, primaryMountPath):
            srcPath = join(secMountPoint, volumeFolderOnSecStorage.lstrip("/"), volumePath.lstrip("/"))
            if not srcPath.endswith(".raw"): srcPath = srcPath + ".raw"
            if not isfile(srcPath): raise Exception("Cannot find volume at %s"%srcPath)
            if not exists(primaryMountPath): raise Exception("Primary storage(%s) seems to have gone"%primaryMountPath)
            OvmStoragePool()._checkDirSizeForImage(primaryMountPath, srcPath)
            destPath = join(primaryMountPath, "sharedDisk")
            newName = get_uuid() + ".raw"
            destName = join(destPath, newName)
            doCmd(['cp', srcPath, destName])
            return destName
                      
        secMountPoint = ""
        try:
            tmpUuid = get_uuid()
            secMountPoint = join("/var/cloud/", tmpUuid)
            OvmStoragePool()._mount(secStorageMountPath, secMountPoint)
            if toSec:
                resultPath = copyToSecStorage(secMountPoint, volumeFolderOnSecStorage, volumePath)
            else:
                sr = OvmStoragePool()._getSrByNameLable(storagePoolUuid)
                primaryStoragePath = sr.mountpoint
                resultPath = copyToPrimary(secMountPoint, volumeFolderOnSecStorage, volumePath, primaryStoragePath)
            OvmStoragePool()._umount(secMountPoint)
            
            # ingratiate bad mgmt server design, it asks 'installPath' but it only wants the volume name without suffix
            volumeUuid = basename(resultPath).rstrip(".raw")
            rs = toGson({"installPath":volumeUuid})
            return rs
        except Exception, e:
            try:
                if exists(secMountPoint):
                    OvmStoragePool()._umount(secMountPoint)
            except Exception, e:
                logger.warning(OvmStoragePool.copyVolume, "umount %s failed"%secMountPoint)       
                
            errmsg = fmt_err_msg(e)
            logger.error(OvmStoragePool.copyVolume, errmsg)
            raise XmlRpcFault(toErrCode(OvmStoragePool, OvmStoragePool.copyVolume), errmsg)
                
                
            