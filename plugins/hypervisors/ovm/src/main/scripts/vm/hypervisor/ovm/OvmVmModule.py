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
from OvmDiskModule import *
from OvmVifModule import *
from OvmHostModule import OvmHost
from string import Template
from OVSXXenVMConfig import *
from OVSSiteVM import start_vm, stop_vm, reset_vm
from OVSSiteCluster import *
from OvmStoragePoolModule import OvmStoragePool
from OVSXXenStore import xen_get_vm_path, xen_get_vnc_port
from OVSDB import db_get_vm
from OVSXMonitor import xen_get_vm_perf_metrics, xen_get_xm_info
from OVSXXenVM import xen_migrate_vm
from OVSSiteRMVM import unregister_vm, register_vm, set_vm_status
from OVSSiteVMInstall import install_vm_hvm
from OVSSiteRMServer import get_master_ip
from OVSXXenVMInstall import xen_change_vm_cdrom
from OVSXAPIUtil import XenAPIObject, session_login, session_logout


logger = OvmLogger("OvmVm")

class OvmVmDecoder(json.JSONDecoder):
    def decode(self, jStr):
        deDict = asciiLoads(jStr)
        vm = OvmVm()
        setAttrFromDict(vm, 'cpuNum', deDict, int)
        setAttrFromDict(vm, 'memory', deDict, long)
        setattr(vm, 'rootDisk', toOvmDisk(deDict['rootDisk']))
        setattr(vm, 'vifs', toOvmVifList(deDict['vifs']))
        setattr(vm, 'disks', toOvmDiskList(deDict['disks']))
        setAttrFromDict(vm, 'name', deDict)
        setAttrFromDict(vm, 'uuid', deDict)
        setAttrFromDict(vm, 'bootDev', deDict)
        setAttrFromDict(vm, 'type', deDict)
        return vm

class OvmVmEncoder(json.JSONEncoder):
    def default(self, obj):
        if not isinstance(obj, OvmVm): raise Exception("%s is not instance of OvmVm"%type(obj))
        dct = {}
        safeDictSet(obj, dct, 'cpuNum')
        safeDictSet(obj, dct, 'memory')
        safeDictSet(obj, dct, 'powerState')
        safeDictSet(obj, dct, 'name')
        safeDictSet(obj, dct, 'type')
        vifs = fromOvmVifList(obj.vifs)
        dct['vifs'] = vifs
        rootDisk = fromOvmDisk(obj.rootDisk)
        dct['rootDisk'] = rootDisk
        disks = fromOvmDiskList(obj.disks)
        dct['disks'] = disks
        return dct

def toOvmVm(jStr):
    return json.loads(jStr, cls=OvmVmDecoder)

def fromOvmVm(vm):
    return normalizeToGson(json.dumps(vm, cls=OvmVmEncoder))

class OvmVm(OvmObject):
    cpuNum = 0
    memory = 0
    rootDisk = None
    vifs = []
    disks = []
    powerState = ''
    name = ''
    bootDev = ''
    type = ''

    def _getVifs(self, vmName):
        vmPath = OvmHost()._vmNameToPath(vmName)
        domId = OvmHost()._getDomainIdByName(vmName)
        vifs = successToMap(xen_get_vifs(vmPath))
        lst = []
        for k in vifs:
            v = vifs[k]
            vifName = 'vif' + domId + '.' + k[len('vif'):]
            vif = OvmVif()
            (mac, bridge, type) = v.split(',')
            safeSetAttr(vif, 'name', vifName)
            safeSetAttr(vif, 'mac', mac)
            safeSetAttr(vif, 'bridge', bridge)
            safeSetAttr(vif, 'type', type)
            lst.append(vif)

        return lst

    def _getVifsFromConfig(self, vmPath):
        vifs = successToMap(xen_get_vifs(vmPath))
        lst = []
        for k in vifs:
            v = vifs[k]
            vif = OvmVif()
            (mac, bridge, type) = v.split(',')
            safeSetAttr(vif, 'name', k)
            safeSetAttr(vif, 'mac', mac)
            safeSetAttr(vif, 'bridge', bridge)
            safeSetAttr(vif, 'type', type)
            lst.append(vif)
        return lst

    def _getIsoMountPath(self, vmPath):
        vmName = basename(vmPath)
        priStoragePath = vmPath.rstrip(join('running_pool', vmName))
        return join(priStoragePath, 'iso_pool', vmName)

    def _getVmTypeFromConfigFile(self, vmPath):
        vmType = successToMap(xen_get_vm_type(vmPath))['type']
        return vmType.replace('hvm', 'HVM').replace('para', 'PV')

    def _tapAOwnerFile(self, vmPath):
        # Create a file with name convention 'host_ip_address' in vmPath
        # Because xm list doesn't return vm that has been stopped, we scan
        # primary storage for stopped vm. This file tells us which host it belongs
        # to. The file is used in OvmHost.getAllVms()
        self._cleanUpOwnerFile(vmPath)
        ownerFileName = makeOwnerFileName()
        fd = open(join(vmPath, ownerFileName), 'w')
        fd.write(ownerFileName)
        fd.close()

    def _cleanUpOwnerFile(self, vmPath):
        for f in os.listdir(vmPath):
            fp = join(vmPath, f)
            if isfile(fp) and f.startswith(OWNER_FILE_PREFIX):
                os.remove(fp)

    @staticmethod
    def create(jsonString):
        def dumpCfg(vmName, cfgPath):
            cfgFd = open(cfgPath, 'r')
            cfg = cfgFd.readlines()
            cfgFd.close()
            logger.info(OvmVm.create, "Start %s with configure:\n\n%s\n"%(vmName, "".join(cfg)))

        def setVifsType(vifs, type):
            for vif in vifs:
                vif.type = type

        def hddBoot(vm, vmPath):
            vmType = vm.type
            if vmType == "FROMCONFIGFILE":
                vmType = OvmVm()._getVmTypeFromConfigFile(vmPath)

            cfgDict = {}
            if vmType == "HVM":
                cfgDict['builder'] = "'hvm'"
                cfgDict['acpi'] = "1"
                cfgDict['apic'] = "1"
                cfgDict['device_model'] = "'/usr/lib/xen/bin/qemu-dm'"
                cfgDict['kernel'] = "'/usr/lib/xen/boot/hvmloader'"
                vifType = 'ioemu'
            else:
                cfgDict['bootloader'] = "'/usr/bin/pygrub'"
                vifType = 'netfront'

            cfgDict['name'] = "'%s'"%vm.name
            cfgDict['disk'] = "[]"
            cfgDict['vcpus'] = "''"
            cfgDict['memory'] = "''"
            cfgDict['on_crash'] = "'destroy'"
            cfgDict['on_reboot'] = "'restart'"
            cfgDict['vif'] = "[]"

            items = []
            for k in cfgDict.keys():
                item = " = ".join([k, cfgDict[k]])
                items.append(item)
            vmSpec = "\n".join(items)

            vmCfg = open(join(vmPath, 'vm.cfg'), 'w')
            vmCfg.write(vmSpec)
            vmCfg.close()

            setVifsType(vm.vifs, vifType)
            raiseExceptionIfFail(xen_set_vcpus(vmPath, vm.cpuNum))
            raiseExceptionIfFail(xen_set_memory(vmPath, BytesToM(vm.memory)))
            raiseExceptionIfFail(xen_add_disk(vmPath, vm.rootDisk.path, mode=vm.rootDisk.type))
            vifs = [OvmVif.toXenString(v) for v in vm.vifs]
            for vif in vifs:
                raiseExceptionIfFail(xen_set_vifs(vmPath, vif))

            for disk in vm.disks:
                raiseExceptionIfFail(xen_add_disk(vmPath, disk.path, mode=disk.type))

            raiseExceptionIfFail(xen_set_vm_vnc_password(vmPath, ""))
            cfgFile = join(vmPath, 'vm.cfg')
            # only HVM supports attaching cdrom
            if vmType == 'HVM':
                # Add an empty "hdc:cdrom" entry in config. Fisrt we set boot order to 'd' that is cdrom boot,
                # then 'hdc:cdrom' entry will be in disk list. Second, change boot order to 'c' which
                # is harddisk boot. VM can not start with an empty 'hdc:cdrom' when boot order is 'd'.
                # it's tricky !
                raiseExceptionIfFail(xen_config_boot_sequence(vmPath, 'd'))
                raiseExceptionIfFail(xen_config_boot_sequence(vmPath, 'c'))

            raiseExceptionIfFail(xen_correct_cfg(cfgFile, vmPath))
            xen_correct_qos_cfg(cfgFile)
            dumpCfg(vm.name, cfgFile)
            server = successToMap(get_master_ip())['ip']
            raiseExceptionIfFail(start_vm(vmPath, server))
            rs = SUCC()
            return rs

        def cdBoot(vm, vmPath):
            isoMountPath = None
            try:
                cdrom = None
                for disk in vm.disks:
                    if disk.isIso == True:
                        cdrom = disk
                        break
                if not cdrom: raise Exception("Cannot find Iso in disks")

                isoOnSecStorage = dirname(cdrom.path)
                isoName = basename(cdrom.path)
                isoMountPath = OvmVm()._getIsoMountPath(vmPath)
                OvmStoragePool()._mount(isoOnSecStorage, isoMountPath)
                isoPath = join(isoMountPath, isoName)
                if not exists(isoPath):
                    raise Exception("Cannot found iso %s at %s which mounts to %s"%(isoName, isoOnSecStorage, isoMountPath))

                stdout = run_cmd(args=['file', isoPath])
                if not stdout.strip().endswith("(bootable)"): raise Exception("ISO %s is not bootable"%cdrom.path)

                #now alter cdrom to correct path
                cdrom.path = isoPath
                if len(vm.vifs) != 0:
                    vif = vm.vifs[0]
                    #ISO boot must be HVM
                    vifCfg = ','.join([vif.mac, vif.bridge, 'ioemu'])
                else:
                    vifCfg = ''

                rootDiskSize = os.path.getsize(vm.rootDisk.path)
                rooDiskCfg = ':'.join([join(vmPath, basename(vm.rootDisk.path)), str(BytesToG(rootDiskSize)), 'True'])
                disks = [rooDiskCfg]
                for d in vm.disks:
                    if d.isIso: continue
                    size = os.path.getsize(d.path)
                    cfg = ':'.join([d.path, str(BytesToG(size)), 'True'])
                    disks.append(cfg)
                disksCfg = ','.join(disks)
                server = successToMap(get_master_ip())['ip']

                raiseExceptionIfFail(install_vm_hvm(vmPath, BytesToM(vm.memory), vm.cpuNum, vifCfg, disksCfg, cdrom.path, vncpassword='', dedicated_server=server))
                rs = SUCC()
                return rs
            except Exception, e:
                if isoMountPath and OvmStoragePool()._isMounted(isoMountPath):
                    doCmd(['umount', '-f', isoMountPath])
                errmsg = fmt_err_msg(e)
                raise Exception(errmsg)

        try:
            vm = toOvmVm(jsonString)
            logger.debug(OvmVm.create, "creating vm, spec:%s"%jsonString)
            rootDiskPath = vm.rootDisk.path
            if not exists(rootDiskPath): raise Exception("Cannot find root disk %s"%rootDiskPath)

            rootDiskDir = dirname(rootDiskPath)
            vmPath = join(dirname(rootDiskDir), vm.name)
            if not exists(vmPath):
                doCmd(['ln', '-s', rootDiskDir, vmPath])
            vmNameFile = open(join(rootDiskDir, 'vmName'), 'w')
            vmNameFile.write(vm.name)
            vmNameFile.close()

            OvmVm()._tapAOwnerFile(rootDiskDir)
            # set the VM to DOWN before starting, OVS agent will check this status
            set_vm_status(vmPath, 'DOWN')
            if vm.bootDev == "HDD":
                return hddBoot(vm, vmPath)
            elif vm.bootDev == "CD":
                return cdBoot(vm, vmPath)
            else:
                raise Exception("Unkown bootdev %s for %s"%(vm.bootDev, vm.name))

        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.create, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.create), errmsg)

    @staticmethod
    def stop(vmName):
        try:
            try:
                OvmHost()._getDomainIdByName(vmName)
            except NoVmFoundException, e:
                logger.info(OvmVm.stop, "vm %s is already stopped"%vmName)
                return SUCC()

            logger.info(OvmVm.stop, "Stop vm %s"%vmName)
            try:
                vmPath = OvmHost()._vmNameToPath(vmName)
            except Exception, e:
                errmsg = fmt_err_msg(e)
                logger.info(OvmVm.stop, "Cannot find link for vm %s on primary storage, treating it as stopped\n %s"%(vmName, errmsg))
                return SUCC()
            # set the VM to RUNNING before stopping, OVS agent will check this status
            set_vm_status(vmPath, 'RUNNING')
            raiseExceptionIfFail(stop_vm(vmPath))
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.stop, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.stop), errmsg)

    @staticmethod
    def reboot(vmName):
        try:
            #===================================================================
            # Xend has a bug of reboot. If reboot vm too quick, xend return success
            # but actually it refused reboot (seen from log)
            # vmPath = successToMap(xen_get_vm_path(vmName))['path']
            # raiseExceptionIfFail(reset_vm(vmPath))
            #===================================================================
            vmPath = OvmHost()._vmNameToPath(vmName)
            OvmVm.stop(vmName)
            raiseExceptionIfFail(start_vm(vmPath))
            vncPort= successToMap(xen_get_vnc_port(vmName))['vnc_port']
            logger.info(OvmVm.stop, "reboot vm %s, new vncPort is %s"%(vmName, vncPort))
            return toGson({"vncPort":str(vncPort)})
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.reboot, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.reboot), errmsg)

    @staticmethod
    def getDetails(vmName):
        try:
            vm = OvmVm()

            try:
                OvmHost()._getDomainIdByName(vmName)
                vmPath = OvmHost()._vmNameToPath(vmName)
                vifsFromConfig = False
            except NoVmFoundException, e:
                vmPath = OvmHost()._getVmPathFromPrimaryStorage(vmName)
                vifsFromConfig = True


            if not isdir(vmPath):
                # The case is, when vm starting was not completed at primaryStorageDownload or createVolume(e.g. mgmt server stop), the mgmt
                # server will keep vm state in staring, then a stop command will be sent. The stop command will delete bridges that vm attaches,
                # by retriving birdge info by OvmVm.getDetails(). In this case, the vm doesn't exists, so returns a fake object here.
                fakeDisk = OvmDisk()
                vm.rootDisk = fakeDisk
            else:
                if vifsFromConfig:
                    vm.vifs.extend(vm._getVifsFromConfig(vmPath))
                else:
                    vm.vifs.extend(vm._getVifs(vmName))

                safeSetAttr(vm, 'name', vmName)
                disks = successToMap(xen_get_vdisks(vmPath))['vdisks'].split(',')
                rootDisk = None
                #BUG: there is no way to get type of disk, assume all are "w"
                for d in disks:
                    if vmName in d:
                        rootDisk = OvmDisk()
                        safeSetAttr(rootDisk, 'path', d)
                        safeSetAttr(rootDisk, 'type', "w")
                        continue
                    disk = OvmDisk()
                    safeSetAttr(disk, 'path', d)
                    safeSetAttr(disk, 'type', "w")
                    vm.disks.append(disk)
                if not rootDisk: raise Exception("Cannot find root disk for vm %s"%vmName)
                safeSetAttr(vm, 'rootDisk', rootDisk)
                vcpus = int(successToMap(xen_get_vcpus(vmPath))['vcpus'])
                safeSetAttr(vm, 'cpuNum', vcpus)
                memory = MtoBytes(int(successToMap(xen_get_memory(vmPath))['memory']))
                safeSetAttr(vm, 'memory', memory)
                vmStatus = db_get_vm(vmPath)
                safeSetAttr(vm, 'powerState',  vmStatus['status'])
                vmType = successToMap(xen_get_vm_type(vmPath))['type'].replace('hvm', 'HVM').replace('para', 'PV')
                safeSetAttr(vm, 'type', vmType)

            rs = fromOvmVm(vm)
            logger.info(OvmVm.getDetails, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.getDetails, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.getDetails), errmsg)

    @staticmethod
    def getVmStats(vmName):
        def getVcpuNumAndUtils():
            try:
                session = session_login()
                refs = session.xenapi.VM.get_by_name_label(vmName)
                if len(refs) == 0:
                    raise Exception("No ref for %s found in xenapi VM objects"%vmName)
                vm = XenAPIObject('VM', session, refs[0])
                VM_metrics = XenAPIObject("VM_metrics", session, vm.get_metrics())
                items = VM_metrics.get_VCPUs_utilisation().items()
                nvCpus = len(items)
                if nvCpus == 0:
                    raise Exception("vm %s has 0 vcpus !!!"%vmName)

                xmInfo = successToMap(xen_get_xm_info())
                nCpus = int(xmInfo['nr_cpus'])
                totalUtils = 0.0
                # CPU utlization of VM = (total cpu utilization of each vcpu) / number of physical cpu
                for num, util in items:
                    totalUtils += float(util)
                avgUtils = float(totalUtils/nCpus) * 100
                return (nvCpus, avgUtils)
            finally:
                session_logout()


        try:
            try:
                OvmHost()._getDomainIdByName(vmName)
                vmPath = OvmHost()._vmNameToPath(vmName)
                (nvcpus, avgUtils) = getVcpuNumAndUtils()
                vifs = successToMap(xen_get_vifs(vmPath))
                rxBytes = 0
                txBytes = 0
                vifs = OvmVm()._getVifs(vmName)
                for vif in vifs:
                    rxp = join('/sys/class/net', vif.name, 'statistics/rx_bytes')
                    txp = join("/sys/class/net/", vif.name, "statistics/tx_bytes")
                    if not exists(rxp): raise Exception('can not find %s'%rxp)
                    if not exists(txp): raise Exception('can not find %s'%txp)
                    rxBytes += long(doCmd(['cat', rxp])) / 1000
                    txBytes += long(doCmd(['cat', txp])) / 1000
            except NoVmFoundException, e:
                vmPath = OvmHost()._getVmPathFromPrimaryStorage(vmName)
                nvcpus = int(successToMap(xen_get_vcpus(vmPath))['vcpus'])
                avgUtils = 0
                rxBytes = 0
                txBytes = 0

            rs = toGson({"cpuNum":nvcpus, "cpuUtil":avgUtils, "rxBytes":rxBytes, "txBytes":txBytes})
            logger.debug(OvmVm.getVmStats, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.getVmStats, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.getVmStats), errmsg)

    @staticmethod
    def migrate(vmName, targetHost):
        try:
            vmPath = OvmHost()._vmNameToPath(vmName)
            raiseExceptionIfFail(xen_migrate_vm(vmPath, targetHost))
            unregister_vm(vmPath)
            OvmVm()._cleanUpOwnerFile(vmPath)
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.migrate, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.migrate), errmsg)

    @staticmethod
    def register(vmName):
        try:
            vmPath = OvmHost()._vmNameToPath(vmName)
            raiseExceptionIfFail(register_vm(vmPath))
            OvmVm()._tapAOwnerFile(vmPath)
            vncPort= successToMap(xen_get_vnc_port(vmName))['vnc_port']
            rs = toGson({"vncPort":str(vncPort)})
            logger.debug(OvmVm.register, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.register, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.register), errmsg)

    @staticmethod
    def getVncPort(vmName):
        try:
            vncPort= successToMap(xen_get_vnc_port(vmName))['vnc_port']
            rs = toGson({"vncPort":vncPort})
            logger.debug(OvmVm.getVncPort, rs)
            return rs
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.getVncPort, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.getVncPort), errmsg)

    @staticmethod
    def detachOrAttachIso(vmName, iso, isAttach):
        try:
            if vmName in OvmHost.getAllVms():
                scope = 'both'
                vmPath = OvmHost()._vmNameToPath(vmName)
            else:
                scope = 'cfg'
                vmPath = OvmHost()._getVmPathFromPrimaryStorage(vmName)

            vmType = OvmVm()._getVmTypeFromConfigFile(vmPath)
            if vmType != 'HVM':
                raise Exception("Only HVM supports attaching/detaching ISO")

            if not isAttach:
                iso = ''
            else:
                isoName = basename(iso)
                isoMountPoint = OvmVm()._getIsoMountPath(vmPath)
                isoOnSecStorage = dirname(iso)
                OvmStoragePool()._mount(isoOnSecStorage, isoMountPoint)
                iso = join(isoMountPoint, isoName)

            exceptionIfNoSuccess(xen_change_vm_cdrom(vmPath, iso, scope))
            return SUCC()
        except Exception, e:
            errmsg = fmt_err_msg(e)
            logger.error(OvmVm.detachOrAttachIso, errmsg)
            raise XmlRpcFault(toErrCode(OvmVm, OvmVm.detachOrAttachIso), errmsg)

if __name__ == "__main__":
    import sys
    print OvmVm.getDetails(sys.argv[1])
    #print OvmVm.getVmStats(sys.argv[1])
