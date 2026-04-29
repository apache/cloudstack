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
from OVSSiteRMServer import get_master_ip, register_server
from OVSCommons import *
from OVSXMonitor import xen_get_xm_info
from OVSXSysInfo import get_agent_version
from OVSSiteRMServer import get_srv_agent_status
from OVSXMonitor import sys_perf_info
from OVSDB import db_get_vm
from OvmStoragePoolModule import OvmStoragePool
from OvmHaHeartBeatModule import OvmHaHeartBeat
import re

logger = OvmLogger('OvmHost')

class OvmHostEncoder(json.JSONEncoder):
    def default(self, obj):
        if not isinstance(obj, OvmHost): raise Exception("%s is not instance of OvmHost"%type(obj))
        dct = {}
        safeDictSet(obj, dct, 'masterIp')
        safeDictSet(obj, dct, 'cpuNum')
        safeDictSet(obj, dct, 'cpuSpeed')
        safeDictSet(obj, dct, 'totalMemory')
        safeDictSet(obj, dct, 'freeMemory')
        safeDictSet(obj, dct, 'dom0Memory')
        safeDictSet(obj, dct, 'agentVersion')
        safeDictSet(obj, dct, 'name')
        safeDictSet(obj, dct, 'dom0KernelVersion')
        safeDictSet(obj, dct, 'hypervisorVersion')
        return dct


def fromOvmHost(host):
    return normalizeToGson(json.dumps(host, cls=OvmHostEncoder))

class OvmHost(OvmObject):
    masterIp = ''
    cpuNum = 0
    cpuSpeed = 0
    totalMemory = 0
    freeMemory = 0
    dom0Memory = 0
    agentVersion = ''
    name = ''
    dom0KernelVersion = ''
    hypervisorVersion = ''

    def _getVmPathFromPrimaryStorage(self, vmName):
        '''
        we don't have a database to store vm states, so there is no way to retrieve information of a vm
        when it was already stopped. The trick is to try to find the vm path in primary storage then we
        can read information from its configure file.
        '''
        mps = OvmStoragePool()._getAllMountPoints()
        vmPath = None
        for p in mps:
            vmPath = join(p, 'running_pool', vmName)
            if exists(vmPath): break
        if not vmPath:
            logger.error(self._getVmPathFromPrimaryStorage, "Cannot find link for %s in any primary storage, the vm was really gone!"%vmName)
            raise Exception("Cannot find link for %s in any primary storage, the vm was really gone!"%vmName)
        return vmPath

    def _vmNameToPath(self, vmName):
        # the xen_get_vm_path always sucks!!!
        #return successToMap((vmName))['path']
        return self._getVmPathFromPrimaryStorage(vmName)

    def _getAllDomains(self):
        stdout = timeout_command(["xm", "list"])
        l = [ line.split()[:2] for line in stdout.splitlines() ]
        l = [ (name, id) for (name, id) in l if name not in ("Name", "Domain-0") ]
        return l

    def _getDomainIdByName(self, vmName):
        l = self._getAllDomains()
        for name, id in l:
            if vmName == name: return id
        raise NoVmFoundException("No domain id for %s found"%vmName)

    @staticmethod
    def registerAsPrimary(hostname, username="oracle", password="password", port=8899, isSsl=False):
        try:
            logger.debug(OvmHost.registerAsPrimary, "ip=%s, username=%s, password=%s, port=%s, isSsl=%s"%(hostname, username, password, port, isSsl))
            exceptionIfNoSuccess(register_server(hostname, 'site', False, username, password, port, isSsl),
                             "Register %s as site failed"%hostname)
            exceptionIfNoSuccess(register_server(hostname, 'utility', False, username, password, port, isSsl),
                             "Register %s as utility failed"%hostname)
            rs = SUCC()
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.registerAsPrimary, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.registerAsPrimary), errmsg)

    @staticmethod
    def registerAsVmServer(hostname, username="oracle", password="password", port=8899, isSsl=False):
        try:
            logger.debug(OvmHost.registerAsVmServer, "ip=%s, username=%s, password=%s, port=%s, isSsl=%s"%(hostname, username, password, port, isSsl))
            exceptionIfNoSuccess(register_server(hostname, 'xen', False, username, password, port, isSsl),
                             "Register %s as site failed"%hostname)
            rs = SUCC()
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.registerAsVmServer, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.registerAsVmServer), errmsg)

    @staticmethod
    def ping(hostname):
        try:
            logger.debug(OvmHost.ping, "ping %s"%hostname)
            exceptionIfNoSuccess(get_srv_agent_status(hostname), "Ovs agent is down")
            rs = SUCC()
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.ping, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.ping, errmsg))

    @staticmethod
    def getDetails():
        try:
            obj = OvmHost()
            masterIp = successToMap(get_master_ip())
            safeSetAttr(obj, 'masterIp', masterIp['ip'])
            xmInfo = successToMap(xen_get_xm_info())
            totalMemory = MtoBytes(long(xmInfo['total_memory']))
            safeSetAttr(obj, 'totalMemory', totalMemory)
            freeMemory = MtoBytes(long(xmInfo['free_memory']))
            safeSetAttr(obj, 'freeMemory', freeMemory)
            dom0Memory = totalMemory - freeMemory
            safeSetAttr(obj, 'dom0Memory', dom0Memory)
            cpuNum = int(xmInfo['nr_cpus'])
            safeSetAttr(obj, 'cpuNum', cpuNum)
            cpuSpeed = int(xmInfo['cpu_mhz'])
            safeSetAttr(obj, 'cpuSpeed', cpuSpeed)
            name = xmInfo['host']
            safeSetAttr(obj, 'name', name)
            dom0KernelVersion = xmInfo['release']
            safeSetAttr(obj, 'dom0KernelVersion', dom0KernelVersion)
            hypervisorVersion = xmInfo['xen_major'] + '.' + xmInfo['xen_minor'] + xmInfo['xen_extra']
            safeSetAttr(obj, 'hypervisorVersion', hypervisorVersion)
            agtVersion = successToMap(get_agent_version())
            safeSetAttr(obj, 'agentVersion', agtVersion['agent_version'])
            res = fromOvmHost(obj)
            logger.debug(OvmHost.getDetails, res)
            return res
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.getDetails, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.getDetails), errmsg)

    @staticmethod
    def getPerformanceStats(bridgeName):
        try:
            rxBytesPath = join("/sys/class/net/", bridgeName, "statistics/rx_bytes")
            txBytesPath = join("/sys/class/net/", bridgeName, "statistics/tx_bytes")
            if not exists(rxBytesPath): raise Exception("Cannot find %s"%rxBytesPath)
            if not exists(txBytesPath): raise Exception("Cannot find %s"%txBytesPath)
            rxBytes = long(doCmd(['cat', rxBytesPath])) / 1000
            txBytes = long(doCmd(['cat', txBytesPath])) / 1000
            sysPerf = successToMap(sys_perf_info())
            cpuUtil = float(100 - float(sysPerf['cpu_idle']) * 100)
            freeMemory = MtoBytes(long(sysPerf['mem_free']))
            xmInfo = successToMap(xen_get_xm_info())
            totalMemory = MtoBytes(long(xmInfo['total_memory']))
            rs = toGson({"cpuUtil":cpuUtil, "totalMemory":totalMemory, "freeMemory":freeMemory, "rxBytes":rxBytes, "txBytes":txBytes})
            logger.info(OvmHost.getPerformanceStats, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.getPerformanceStats, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.getPerformanceStats), errmsg)

    @staticmethod
    def getAllVms():
        def scanStoppedVmOnPrimaryStorage(vms):
            def isMyVmDirLink(path):
                return (islink(path) and exists(join(path, 'vm.cfg')) and ('-' in basename(path)) and (exists(join(path, makeOwnerFileName()))))

            mps = OvmStoragePool()._getAllMountPoints()
            for mountPoint in mps:
                runningPool = join(mountPoint, 'running_pool')
                if not exists(runningPool):
                    logger.debug(OvmHost.getAllVms, "Primary storage %s not existing, skip it. this should be first getAllVms() called from Ovm resource configure"%runningPool)
                    continue

                for dir in os.listdir(runningPool):
                    vmDir = join(runningPool, dir)
                    if not isMyVmDirLink(vmDir):
                        logger.debug(OvmHost.getAllVms, "%s is not our vm directory, skip it"%vmDir)
                        continue
                    if vms.has_key(dir):
                        logger.debug(OvmHost.getAllVms, "%s is already in running list, skip it"%dir)
                        continue

                    logger.debug(OvmHost.getAllVms, "Found a stopped vm %s on primary storage %s, report it to management server" % (dir, mountPoint))
                    vms[dir] = "DOWN"


        try:
            l = OvmHost()._getAllDomains()
            dct = {}
            host = OvmHost()
            for name, id in l:
                try:
                    vmPath = host._getVmPathFromPrimaryStorage(name)
                    vmStatus = db_get_vm(vmPath)
                    dct[name] = vmStatus['status']
                except Exception, e:
                    logger.debug(OvmHost.getAllVms, "Cannot find link for %s on primary storage, treat it as Error"%name)
                    dct[name] = 'ERROR'

            scanStoppedVmOnPrimaryStorage(dct)
            rs = toGson(dct)
            logger.info(OvmHost.getAllVms, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.getAllVms, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.getAllVms), errmsg)

    @staticmethod
    def fence(ip):
        # try 3 times to avoid race condition that read when heartbeat file is being written
        def getTimeStamp(hbFile):
            for i in range(1, 3):
                f = open(hbFile, 'r')
                str = f.readline()
                items = re.findall(HEARTBEAT_TIMESTAMP_PATTERN, str)
                if len(items) == 0:
                    logger.debug(OvmHost.fence, "Get an incorrect heartbeat data %s, will retry %s times" % (str, 3-i))
                    f.close()
                    time.sleep(5)
                else:
                    f.close()
                    timestamp = items[0]
                    return timestamp.lstrip('<timestamp>').rstrip('</timestamp>')

        # totally check in 6 mins, the update frequency is 2 mins
        def check(hbFile):
            for i in range(1, 6):
                ts = getTimeStamp(hbFile)
                time.sleep(60)
                nts = getTimeStamp(hbFile)
                if ts != nts: return True
                else: logger.debug(OvmHost.fence, '%s is not updated, old value=%s, will retry %s times'%(hbFile, ts, 6-i))
            return False

        try:
            mountpoints = OvmStoragePool()._getAllMountPoints()
            hbFile = None
            for m in mountpoints:
                p = join(m, HEARTBEAT_DIR, ipToHeartBeatFileName(ip))
                if exists(p):
                    hbFile = p
                    break

            if not hbFile: raise Exception('Can not find heartbeat file for %s in pools %s'%(ip, mountpoints))
            rs = toGson({"isLive":check(hbFile)})
            logger.debug(OvmHost.fence, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.fence, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.fence), errmsg)

    @staticmethod
    def setupHeartBeat(poolUuid, ip):
        try:
            sr = OvmStoragePool()._getSrByNameLable(poolUuid)
            OvmHaHeartBeat.start(sr.mountpoint, ip)
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.setupHeartBeat, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.setupHeartBeat), errmsg)

    @staticmethod
    def pingAnotherHost(ip):
        try:
            doCmd(['ping', '-c', '1', '-n', '-q', ip])
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmHost.pingAnotherHost, errmsg)
            raise XmlRpcFault(toErrCode(OvmHost, OvmHost.pingAnotherHost), errmsg)

if __name__ == "__main__":
    print OvmHost.getAllVms()
