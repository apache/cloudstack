#!/usr/bin/python
# Copyright (C) 2006-2007 XenSource Ltd.
# Copyright (C) 2008-2009 Citrix Ltd.
#
# This program is free software; you can redistribute it and/or modify 
# it under the terms of the GNU Lesser General Public License as published 
# by the Free Software Foundation; version 2.1 only.
#
# This program is distributed in the hope that it will be useful, 
# but WITHOUT ANY WARRANTY; without even the implied warranty of 
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
# GNU Lesser General Public License for more details.
#
# Script to coalesce and garbage collect VHD-based SR's in the background
#

import os
import sys
import time
import signal
import subprocess
import getopt
import datetime
import exceptions
import traceback
import base64
import zlib

import XenAPI
import util
import lvutil
import vhdutil
import lvhdutil
import lvmcache
import journaler
import fjournaler
import lock
import atomicop
from refcounter import RefCounter
from ipc import IPCFlag
from lvmanager import LVActivator

# Disable automatic leaf-coalescing. Online leaf-coalesce is currently not 
# possible due to lvhd_stop_using_() not working correctly. However, we leave 
# this option available through the explicit LEAFCLSC_FORCE flag in the VDI 
# record for use by the offline tool (which makes the operation safe by pausing 
# the VM first)
AUTO_ONLINE_LEAF_COALESCE_ENABLED = False

LOG_FILE = "/var/log/SMlog"
FLAG_TYPE_ABORT = "abort"     # flag to request aborting of GC/coalesce

# process "lock", used simply as an indicator that a process already exists 
# that is doing GC/coalesce on this SR (such a process holds the lock, and we 
# check for the fact by trying the lock). 
LOCK_TYPE_RUNNING = "running" 
lockRunning = None


class AbortException(util.SMException):
    pass

################################################################################
#
#  Util
#
class Util:
    RET_RC     = 1
    RET_STDOUT = 2
    RET_STDERR = 4

    PREFIX = {"G": 1024 * 1024 * 1024, "M": 1024 * 1024, "K": 1024}

    def log(text):
        f = open(LOG_FILE, 'a')
        f.write("<%d> %s\t%s\n" % (os.getpid(), datetime.datetime.now(), text))
        f.close()
    log = staticmethod(log)

    def logException(tag):
        info = sys.exc_info()
        if info[0] == exceptions.SystemExit:
            # this should not be happening when catching "Exception", but it is
            sys.exit(0)
        tb = reduce(lambda a, b: "%s%s" % (a, b), traceback.format_tb(info[2]))
        Util.log("*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*")
        Util.log("         ***********************")
        Util.log("         *  E X C E P T I O N  *")
        Util.log("         ***********************")
        Util.log("%s: EXCEPTION %s, %s" % (tag, info[0], info[1]))
        Util.log(tb)
        Util.log("*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*")
    logException = staticmethod(logException)

    def doexec(args, expectedRC, inputtext=None, ret=None, log=True):
        "Execute a subprocess, then return its return code, stdout, stderr"
        proc = subprocess.Popen(args,
                                stdin=subprocess.PIPE,\
                                stdout=subprocess.PIPE,\
                                stderr=subprocess.PIPE,\
                                shell=True,\
                                close_fds=True)
        (stdout, stderr) = proc.communicate(inputtext)
        stdout = str(stdout)
        stderr = str(stderr)
        rc = proc.returncode
        if log:
            Util.log("`%s`: %s" % (args, rc))
        if type(expectedRC) != type([]):
            expectedRC = [expectedRC]
        if not rc in expectedRC:
            reason = stderr.strip()
            if stdout.strip():
                reason = "%s (stdout: %s)" % (reason, stdout.strip())
            Util.log("Failed: %s" % reason)
            raise util.CommandException(rc, args, reason)

        if ret == Util.RET_RC:
            return rc
        if ret == Util.RET_STDERR:
            return stderr
        return stdout
    doexec = staticmethod(doexec)

    def runAbortable(func, ret, ns, abortTest, pollInterval, timeOut):
        """execute func in a separate thread and kill it if abortTest signals
        so"""
        resultFlag = IPCFlag(ns)
        pid = os.fork()
        if pid:
            startTime = time.time()
            while True:
                if resultFlag.test("success"):
                    Util.log("  Child process completed successfully")
                    resultFlag.clear("success")
                    return
                if resultFlag.test("failure"):
                    resultFlag.clear("failure")
                    raise util.SMException("Child process exited with error")
                if abortTest():
                    os.killpg(pid, signal.SIGKILL)
                    raise AbortException("Aborting due to signal")
                if timeOut and time.time() - startTime > timeOut:
                    os.killpg(pid, signal.SIGKILL)
                    raise util.SMException("Timed out")
                time.sleep(pollInterval)
        else:
            os.setpgrp()
            try:
                if func() == ret:
                    resultFlag.set("success")
                else:
                    resultFlag.set("failure")
            except:
                resultFlag.set("failure")
            os._exit(0)
    runAbortable = staticmethod(runAbortable)

    def num2str(number):
        for prefix in ("G", "M", "K"):
            if number >= Util.PREFIX[prefix]:
                return "%.2f%s" % (float(number) / Util.PREFIX[prefix], prefix)
        return "%s" % number
    num2str = staticmethod(num2str)

    def numBits(val):
        count = 0
        while val:
            count += val & 1
            val = val >> 1
        return count
    numBits = staticmethod(numBits)

    def countBits(bitmap1, bitmap2):
        """return bit count in the bitmap produced by ORing the two bitmaps"""
        len1 = len(bitmap1)
        len2 = len(bitmap2)
        lenLong = len1
        lenShort = len2
        bitmapLong = bitmap1
        if len2 > len1:
            lenLong = len2
            lenShort = len1
            bitmapLong = bitmap2

        count = 0
        for i in range(lenShort):
            val = ord(bitmap1[i]) | ord(bitmap2[i])
            count += Util.numBits(val)

        for i in range(i + 1, lenLong):
            val = ord(bitmapLong[i])
            count += Util.numBits(val)
        return count
    countBits = staticmethod(countBits)

    def getThisScript():
        thisScript = util.get_real_path(__file__)
        if thisScript.endswith(".pyc"):
            thisScript = thisScript[:-1]
        return thisScript
    getThisScript = staticmethod(getThisScript)

    def getThisHost():
        uuid = None
        f = open("/etc/xensource-inventory", 'r')
        for line in f.readlines():
            if line.startswith("INSTALLATION_UUID"):
                uuid = line.split("'")[1]
        f.close()
        return uuid
    getThisHost = staticmethod(getThisHost)


################################################################################
#
#  XAPI
#
class XAPI:
    USER = "root"
    PLUGIN_ON_SLAVE = "on-slave"
    PLUGIN_PAUSE_VDIS = "atomicop.py"

    CONFIG_SM = 0
    CONFIG_OTHER = 1

    class LookupError(util.SMException):
        pass
    
    def getSession():
        session = XenAPI.xapi_local()
        session.xenapi.login_with_password(XAPI.USER, '')
        return session
    getSession = staticmethod(getSession)

    def __init__(self, session, srUuid):
        self.sessionPrivate = False
        self.session = session
        if self.session == None:
            self.session = self.getSession()
            self.sessionPrivate = True
        self._srRef = self.session.xenapi.SR.get_by_uuid(srUuid)
        self.srRecord = self.session.xenapi.SR.get_record(self._srRef)
        self.hostUuid = Util.getThisHost()
        self._hostRef = self.session.xenapi.host.get_by_uuid(self.hostUuid)

    def __del__(self):
        if self.sessionPrivate:
            self.session.xenapi.session.logout()

    def isInvalidHandleError(exception):
        return exception.details[0] == "HANDLE_INVALID"
    isInvalidHandleError = staticmethod(isInvalidHandleError)

    def isPluggedHere(self):
        pbds = self.getAttachedPBDs()
        for pbdRec in pbds:
            if pbdRec["host"] == self._hostRef:
                return True
        return False

    def isMaster(self):
        if self.srRecord["shared"]:
            pool = self.session.xenapi.pool.get_all_records().values()[0]
            return pool["master"] == self._hostRef
        else:
            pbds = self.getAttachedPBDs()
            if len(pbds) < 1:
                raise util.SMException("Local SR not attached")
            elif len(pbds) > 1:
                raise util.SMException("Local SR multiply attached")
            return pbds[0]["host"] == self._hostRef

    def getAttachedPBDs(self):
        """Return PBD records for all PBDs of this SR that are currently
        attached"""
        attachedPBDs = []
        pbds = self.session.xenapi.PBD.get_all_records()
        for pbdRec in pbds.values():
            if pbdRec["SR"] == self._srRef and pbdRec["currently_attached"]:
                attachedPBDs.append(pbdRec)
        return attachedPBDs

    def getOnlineHosts(self):
        onlineHosts = []
        hosts = self.session.xenapi.host.get_all_records()
        for hostRef, hostRecord in hosts.iteritems():
            metricsRef = hostRecord["metrics"]
            metrics = self.session.xenapi.host_metrics.get_record(metricsRef)
            if metrics["live"]:
                onlineHosts.append(hostRef)
        return onlineHosts

    def ensureInactive(self, hostRef, args):
        text = self.session.xenapi.host.call_plugin( \
                hostRef, self.PLUGIN_ON_SLAVE, "multi", args)
        Util.log("call-plugin returned: '%s'" % text)

    def getRefVDI(self, vdi):
        return self.session.xenapi.VDI.get_by_uuid(vdi.uuid)

    def singleSnapshotVDI(self, vdi):
        return self.session.xenapi.VDI.snapshot(vdi.getRef(), {"type":"single"})

    def forgetVDI(self, vdiUuid):
        """Forget the VDI, but handle the case where the VDI has already been
        forgotten (i.e. ignore errors)"""
        try:
            vdiRef = self.session.xenapi.VDI.get_by_uuid(vdiUuid)
            self.session.xenapi.VDI.forget(vdiRef)
        except XenAPI.Failure:
            pass

    def getConfigVDI(self, kind, vdi):
        if kind == self.CONFIG_SM:
            return self.session.xenapi.VDI.get_sm_config(vdi.getRef())
        elif kind == self.CONFIG_OTHER:
            return self.session.xenapi.VDI.get_other_config(vdi.getRef())
        assert(False)

    def setConfigVDI(self, kind, vdi, map):
        if kind == self.CONFIG_SM:
            self.session.xenapi.VDI.set_sm_config(vdi.getRef(), map)
        elif kind == self.CONFIG_OTHER:
            self.session.xenapi.VDI.set_other_config(vdi.getRef(), map)
        else:
            assert(False)

    def srUpdate(self):
        Util.log("Starting asynch srUpdate for SR %s" % self.srRecord["uuid"])
        abortFlag = IPCFlag(self.srRecord["uuid"])
        task = self.session.xenapi.Async.SR.update(self._srRef)
        for i in range(60):
            status = self.session.xenapi.task.get_status(task)
            if not status == "pending":
                Util.log("SR.update_asynch status changed to [%s]" % status)
                return
            if abortFlag.test(FLAG_TYPE_ABORT):
                Util.log("Abort signalled during srUpdate, cancelling task...")
                try:
                    self.session.xenapi.task.cancel(task)
                    Util.log("Task cancelled")
                except:
                    pass
                return
            time.sleep(1)
        Util.log("Asynch srUpdate still running, but timeout exceeded.")

    def atomicOp(self, vdiList, op, args, mustExist = False):
        vdiRefs = []
        for vdi in vdiList:
            Util.log("atomicOp: will pause %s" % vdi.toString())
            try:
                vdiRefs.append(vdi.getRef())
            except XenAPI.Failure:
                Util.log("atomicOp: can't find %s" % vdi.toString())
                if mustExist:
                    raise
        if len(vdiRefs) == 0:
            Util.log("atomicOp: no VDIs found in DB, not pausing anything")
            fn = getattr(atomicop, op)
            ret = fn(self.session, args)
        else:
            ret = self.session.xenapi.SR.lvhd_stop_using_these_vdis_and_call_script(\
                    vdiRefs, self.PLUGIN_PAUSE_VDIS, op, args)
        Util.log("Plugin returned: %s" % ret)
        if ret == atomicop.RET_EXCEPTION:
            raise util.SMException("Exception in atomic %s" % op)
        if ret == atomicop.RET_SUCCESS:
            return True
        return False


################################################################################
#
#  VDI
#
class VDI:
    """Object representing a VDI of a VHD-based SR"""

    POLL_INTERVAL = 1
    POLL_TIMEOUT  = 30
    DEVICE_MAJOR  = 202
    DRIVER_NAME_VHD = "vhd"

    # config keys & values
    DB_VHD_PARENT = "vhd-parent"
    DB_VDI_TYPE = "vdi_type"
    DB_VHD_BLOCKS = "vhd-blocks"
    DB_LEAFCLSC = "leaf-coalesce" # config key
    LEAFCLSC_DISABLED = "no" # set by user; means do not leaf-coalesce
    LEAFCLSC_FORCE = "force" # set by user; means skip snap-coalesce
    LEAFCLSC_OFFLINE = "offline" # set here for informational purposes: means
                                 # no space to snap-coalesce or unable to keep 
                                 # up with VDI
    CONFIG_TYPE = {
            DB_VHD_PARENT: XAPI.CONFIG_SM,
            DB_VDI_TYPE:   XAPI.CONFIG_SM,
            DB_VHD_BLOCKS: XAPI.CONFIG_SM,
            DB_LEAFCLSC:   XAPI.CONFIG_OTHER }

    LIVE_LEAF_COALESCE_MAX_SIZE = 100 * 1024 * 1024 # bytes
    LIVE_LEAF_COALESCE_TIMEOUT = 10 # seconds

    JRN_RELINK = "relink" # journal entry type for relinking children
    JRN_COALESCE = "coalesce" # to communicate which VDI is being coalesced
    JRN_LEAF = "leaf" # used in coalesce-leaf

    PRINT_INDENTATION = 4

    def __init__(self, sr, uuid):
        self.sr         = sr
        self.scanError  = True
        self.uuid       = uuid
        self.parentUuid = ""
        self.sizeVirt   = -1
        self._sizeVHD   = -1
        self.hidden     = False
        self.parent     = None
        self.children   = []
        self._vdiRef    = None
        self._config    = {}
        self._configDirty = {}
        self._clearRef()

    def load(self):
        """Load VDI info"""
        pass # abstract

    def getDriverName(self):
        return self.DRIVER_NAME_VHD

    def getRef(self):
        if self._vdiRef == None:
            self._vdiRef = self.sr.xapi.getRefVDI(self)
        return self._vdiRef

    def getConfig(self, key, default = None):
        kind = self.CONFIG_TYPE[key]
        self._configLazyInit(kind)
        if self._config[kind].get(key):
            return self._config[kind][key]
        return default

    def setConfig(self, key, val):
        kind = self.CONFIG_TYPE[key]
        self._configLazyInit(kind)
        self._config[kind][key] = val
        self._configDirty[kind] = True

    def delConfig(self, key):
        kind = self.CONFIG_TYPE[key]
        self._configLazyInit(kind)
        if self._config[kind].get(key):
            del self._config[kind][key]
            self._configDirty[kind] = True

    def updateConfig(self):
        for kind in self._config.keys():
            if self._configDirty[kind]:
                self.sr.xapi.setConfigVDI(kind, self, self._config[kind])
                self._configDirty[kind] = False

    def setConfigUpdate(self, key, val):
        self.setConfig(key, val)
        self.updateConfig()

    def delConfigUpdate(self, key):
        self.delConfig(key)
        self.updateConfig()

    def getVHDBlocks(self):
        val = self.getConfig(VDI.DB_VHD_BLOCKS)
        if not val:
            self.updateBlockInfo()
            val = self.getConfig(VDI.DB_VHD_BLOCKS)
        bitmap = zlib.decompress(base64.b64decode(val))
        return bitmap

    def isCoalesceable(self):
        """A VDI is coalesceable if it has no siblings and is not a leaf"""
        return not self.scanError and \
                self.parent and \
                len(self.parent.children) == 1 and \
                self.hidden and \
                len(self.children) > 0 

    def isLeafCoalesceable(self):
        """A VDI is leaf-coalesceable if it has no siblings and is a leaf"""
        return not self.scanError and \
                self.parent and \
                len(self.parent.children) == 1 and \
                not self.hidden and \
                len(self.children) == 0

    def canLiveCoalesce(self):
        """Can we stop-and-leaf-coalesce this VDI? The VDI must be
        isLeafCoalesceable() already"""
        return self.getSizeVHD() <= self.LIVE_LEAF_COALESCE_MAX_SIZE or \
                self.getConfig(self.DB_LEAFCLSC) == self.LEAFCLSC_FORCE

    def getAllPrunable(self):
        if len(self.children) == 0: # base case
            # it is possible to have a hidden leaf that was recently coalesced 
            # onto its parent, its children already relinked but not yet 
            # reloaded - in which case it may not be garbage collected yet: 
            # some tapdisks could still be using the file.
            if self.sr.journaler.get(self.JRN_RELINK, self.uuid):
                return []
            if not self.scanError and self.hidden:
                return [self]
            return []

        thisPrunable = True
        vdiList = []
        for child in self.children:
            childList = child.getAllPrunable()
            vdiList.extend(childList)
            if child not in childList:
                thisPrunable = False

        if not self.scanError and thisPrunable:
            vdiList.append(self)
        return vdiList

    def getSizeVHD(self):
        return self._sizeVHD

    def getTreeRoot(self):
        "Get the root of the tree that self belongs to"
        root = self
        while root.parent:
            root = root.parent
        return root
        
    def getTreeHeight(self):
        "Get the height of the subtree rooted at self"
        if len(self.children) == 0:
            return 1

        maxChildHeight = 0
        for child in self.children:
            childHeight = child.getTreeHeight()                
            if childHeight > maxChildHeight:
                maxChildHeight = childHeight

        return maxChildHeight + 1

    def updateBlockInfo(self):
        val = base64.b64encode(self._queryVHDBlocks())
        self.setConfigUpdate(VDI.DB_VHD_BLOCKS, val)

    def rename(self, uuid):
        "Rename the VDI file"
        assert(not self.sr.vdis.get(uuid))
        self._clearRef()
        oldUuid = self.uuid
        self.uuid = uuid
        self.children = [] 
        # updating the children themselves is the responsiblity of the caller
        del self.sr.vdis[oldUuid]
        self.sr.vdis[self.uuid] = self

    def delete(self):
        "Physically delete the VDI"
        lock.Lock.cleanup(self.uuid, lvhdutil.NS_PREFIX_LVM + self.sr.uuid)
        self._clear()

    def printTree(self, indentSize = 0):
        indent = " " * indentSize
        Util.log("%s%s" % (indent, self.toString()))
        for child in self.children:
            child.printTree(indentSize + VDI.PRINT_INDENTATION)

    def toString(self):
        strHidden = ""
        if self.hidden:
            strHidden = "*"
        strSizeVHD = ""
        if self._sizeVHD > 0:
            strSizeVHD = Util.num2str(self._sizeVHD)

        return "%s%s(%s/%s)" % (strHidden, self.uuid[0:8],
                Util.num2str(self.sizeVirt), strSizeVHD)

    def validate(self):
        if not vhdutil.check(self.path):
            raise util.SMException("VHD %s corrupted" % self.toString())

    def _clear(self):
        self.uuid = ""
        self.path = ""
        self.parentUuid = ""
        self.parent = None
        self._clearRef()

    def _clearRef(self):
        self._vdiRef = None
        for kind in [XAPI.CONFIG_SM, XAPI.CONFIG_OTHER]:
            self._config[kind] = None
            self._configDirty[kind] = False

    def _configLazyInit(self, kind):
        if self._config[kind] == None:
            self._config[kind] = self.sr.xapi.getConfigVDI(kind, self)

    def _coalesceBegin(self):
        """Coalesce self onto parent. Only perform the actual coalescing of
        VHD, but not the subsequent relinking. We'll do that as the next step,
        after reloading the entire SR in case things have changed while we
        were coalescing"""
        self.validate()
        self.parent.validate()
        if self.sr.journaler.get(self.JRN_RELINK, self.uuid):
            # this means we had done the actual coalescing already and just 
            # need to finish relinking and/or refreshing the children
            Util.log("==> Coalesce apparently already done: skipping")
        else:
            self.parent._increaseSizeVirt(self.sizeVirt)
            self._coalesceVHD(0)
            self.parent.validate()
            #self._verifyContents(0)
            self.parent.updateBlockInfo()

    def _verifyContents(self, timeOut):
        Util.log("  Coalesce verification on %s" % self.toString())
        abortTest = lambda:IPCFlag(self.sr.uuid).test(FLAG_TYPE_ABORT)
        Util.runAbortable(lambda: self._runTapdiskDiff(), True,
                self.sr.uuid, abortTest, VDI.POLL_INTERVAL, timeOut)
        Util.log("  Coalesce verification succeeded")

    def _runTapdiskDiff(self):
        cmd = "tapdisk-diff -n %s:%s -m %s:%s" % \
                (self.getDriverName(), self.path, \
                self.parent.getDriverName(), self.parent.path)
        Util.doexec(cmd, 0)
        return True

    def _coalesceVHD(self, timeOut):
        Util.log("  Running VHD coalesce on %s" % self.toString())
        abortTest = lambda:IPCFlag(self.sr.uuid).test(FLAG_TYPE_ABORT)
        Util.runAbortable(lambda: vhdutil.coalesce(self.path), None,
                self.sr.uuid, abortTest, VDI.POLL_INTERVAL, timeOut)
        util.fistpoint.activate("LVHDRT_coalescing_VHD_data",self.sr.uuid)

    def _relinkSkip(self):
        """Relink children of this VDI to point to the parent of this VDI"""
        abortFlag = IPCFlag(self.sr.uuid)
        for child in self.children:
            if abortFlag.test(FLAG_TYPE_ABORT):
                raise AbortException("Aborting due to signal")
            Util.log("  Relinking %s from %s to %s" % (child.toString(), \
                    self.toString(), self.parent.toString()))
            util.fistpoint.activate("LVHDRT_relinking_grandchildren",self.sr.uuid)
            child._setParent(self.parent)
        self.children = []

    def _reloadChildren(self, vdiSkip):
        """Pause & unpause all VDIs in the subtree to cause blktap to reload
        the VHD metadata for this file in any online VDI"""
        abortFlag = IPCFlag(self.sr.uuid)
        for child in self.children:
            if child == vdiSkip:
                continue
            if abortFlag.test(FLAG_TYPE_ABORT):
                raise AbortException("Aborting due to signal")
            Util.log("  Reloading VDI %s" % child.toString())
            child._reload()

    def _reload(self):
        """Pause & unpause to cause blktap to reload the VHD metadata"""
        for child in self.children:
            child._reload()

        # only leaves can be attached
        if len(self.children) == 0:
            try:
                self.sr.xapi.atomicOp([self], "noop", {})
            except XenAPI.Failure, e:
                if self.sr.xapi.isInvalidHandleError(e):
                    Util.log("VDI %s appears to have been deleted, ignoring" % \
                            self.toString())
                else:
                    raise

    def _loadInfoParent(self):
        ret = vhdutil.getParent(self.path, lvhdutil.extractUuid)
        if ret:
            self.parentUuid = ret

    def _setParent(self, parent):
        vhdutil.setParent(self.path, parent.path, False)
        self.parent = parent
        self.parentUuid = parent.uuid
        parent.children.append(self)
        try:
            self.setConfigUpdate(self.DB_VHD_PARENT, self.parentUuid)
            Util.log("Updated the vhd-parent field for child %s with %s" % \
                     (self.uuid, self.parentUuid))
        except:
            Util.log("Failed to update %s with vhd-parent field %s" % \
                     (self.uuid, self.parentUuid))

    def _loadInfoHidden(self):
        hidden = vhdutil.getHidden(self.path)
        self.hidden = (hidden != 0)

    def _setHidden(self, hidden = True):
        vhdutil.setHidden(self.path, hidden)
        self.hidden = hidden

    def _increaseSizeVirt(self, size, atomic = True):
        """ensure the virtual size of 'self' is at least 'size'. Note that 
        resizing a VHD must always be offline and atomically: the file must
        not be open by anyone and no concurrent operations may take place.
        Thus we use the Agent API call for performing paused atomic 
        operations. If the caller is already in the atomic context, it must
        call with atomic = False"""
        if self.sizeVirt >= size:
            return
        Util.log("  Expanding VHD virt size for VDI %s: %s -> %s" % \
                (self.toString(), Util.num2str(self.sizeVirt), \
                Util.num2str(size)))

        msize = vhdutil.getMaxResizeSize(self.path) * 1024 * 1024
        if (size <= msize):
            vhdutil.setSizeVirtFast(self.path, size)
        else:
            if atomic:
                args = self._resizeArgs(size)
                vdiList = self._getAllSubtree()
                if not self.sr.xapi.atomicOp(vdiList, "resize", args):
                    raise util.SMException("Failed to resize atomically")
            else:
                self._setSizeVirt(size)

        self.sizeVirt = vhdutil.getSizeVirt(self.path)

    def _setSizeVirt(self, size):
        """WARNING: do not call this method directly unless all VDIs in the
        subtree are guaranteed to be unplugged (and remain so for the duration
        of the operation): this operation is only safe for offline VHDs"""
        jFile = os.path.join(vhdutil.VHD_JOURNAL_LOCATION, self.uuid)
        vhdutil.setSizeVirt(self.path, size, jFile)

    def _queryVHDBlocks(self):
        return vhdutil.getBlockBitmap(self.path)

    def _getCoalescedSizeData(self):
        """Get the data size of the resulting VHD if we coalesce self onto
        parent. We calculate the actual size by using the VHD block allocation
        information (as opposed to just adding up the two VHD sizes to get an
        upper bound)"""
        # make sure we don't use stale BAT info from vdi_rec since the child 
        # was writable all this time
        self.delConfigUpdate(VDI.DB_VHD_BLOCKS)
        blocksChild = self.getVHDBlocks()
        blocksParent = self.parent.getVHDBlocks()
        numBlocks = Util.countBits(blocksChild, blocksParent)
        Util.log("Num combined blocks = %d" % numBlocks)
        sizeData = numBlocks * vhdutil.VHD_BLOCK_SIZE
        assert(sizeData <= self.sizeVirt)
        return sizeData

    def _calcExtraSpaceForCoalescing(self):
        sizeData = self._getCoalescedSizeData()
        sizeCoalesced = sizeData + vhdutil.calcOverheadBitmap(sizeData) + \
                vhdutil.calcOverheadEmpty(self.sizeVirt)
        Util.log("Coalesced size = %s" % Util.num2str(sizeCoalesced))
        return sizeCoalesced - self.parent.getSizeVHD()

    def _calcExtraSpaceForLeafCoalescing(self):
        """How much extra space in the SR will be required to
        [live-]leaf-coalesce this VDI"""
        # the space requirements are the same as for inline coalesce
        return self._calcExtraSpaceForCoalescing()

    def _calcExtraSpaceForSnapshotCoalescing(self):
        """How much extra space in the SR will be required to
        snapshot-coalesce this VDI"""
        return self._calcExtraSpaceForCoalescing() + \
                vhdutil.calcOverheadEmpty(self.sizeVirt) # extra snap leaf

    def _getAllSubtree(self):
        """Get self and all VDIs in the subtree of self as a flat list"""
        vdiList = [self]
        for child in self.children:
            vdiList.extend(child._getAllSubtree())
        return vdiList

    def _resizeArgs(self, size):
        args = {
                "type": self.sr.TYPE,
                "uuid": self.uuid,
                "path": self.path,
                "size": str(size),
                "srUuid": self.sr.uuid
        }
        return args


class FileVDI(VDI):
    """Object representing a VDI in a file-based SR (EXT or NFS)"""

    FILE_SUFFIX = ".vhd"

    def extractUuid(path):
        path = os.path.basename(path.strip())
        if not path.endswith(FileVDI.FILE_SUFFIX):
            return None
        uuid = path.replace(FileVDI.FILE_SUFFIX, "")
        # TODO: validate UUID format
        return uuid
    extractUuid = staticmethod(extractUuid)

    def load(self, info = None):
        if not info:
            if not util.pathexists(self.path):
                raise util.SMException("%s not found" % self.path)
            try:
                info = vhdutil.getVHDInfo(self.path, self.extractUuid)
            except util.SMException:
                Util.log(" [VDI %s: failed to read VHD metadata]" % self.uuid)
                return
        self.parent     = None
        self.children   = []
        self.parentUuid = info.parentUuid
        self.sizeVirt   = info.sizeVirt
        self._sizeVHD   = info.sizePhys
        self.hidden     = info.hidden
        self.scanError  = False
        self.path       = os.path.join(self.sr.path, "%s%s" % \
                (self.uuid, self.FILE_SUFFIX))

    def rename(self, uuid):
        oldPath = self.path
        VDI.rename(self, uuid)
        fileName = "%s%s" % (self.uuid, self.FILE_SUFFIX)
        self.path = os.path.join(self.sr.path, fileName)
        assert(not util.pathexists(self.path))
        Util.log("Renaming %s -> %s" % (oldPath, self.path))
        os.rename(oldPath, self.path)

    def delete(self):
        if len(self.children) > 0:
            raise util.SMException("VDI %s has children, can't delete" % \
                    self.uuid)
        try:
            self.sr.lock()
            try:
                os.unlink(self.path)
            finally:
                self.sr.unlock()
        except OSError:
            raise util.SMException("os.unlink(%s) failed" % self.path)
        self.sr.xapi.forgetVDI(self.uuid)
        VDI.delete(self)


class LVHDVDI(VDI):
    """Object representing a VDI in an LVHD SR"""

    JRN_ZERO = "zero" # journal entry type for zeroing out end of parent
    DRIVER_NAME_RAW = "aio"

    def load(self, vdiInfo):
        self.parent     = None
        self.children   = []
        self._sizeVHD   = -1
        self.scanError  = vdiInfo.scanError
        self.raw        = vdiInfo.vdiType == lvhdutil.VDI_TYPE_RAW
        self.sizeLV     = vdiInfo.sizeLV
        self.sizeVirt   = vdiInfo.sizeVirt
        self.lvName     = vdiInfo.lvName
        self.lvActive   = vdiInfo.lvActive
        self.lvReadonly = vdiInfo.lvReadonly
        self.hidden     = vdiInfo.hidden
        self.parentUuid = vdiInfo.parentUuid
        self.path       = os.path.join(self.sr.path, self.lvName)

    def getDriverName(self):
        if self.raw:
            return self.DRIVER_NAME_RAW
        return self.DRIVER_NAME_VHD

    def inflate(self, size):
        """inflate the LV containing the VHD to 'size'"""
        if self.raw:
            return
        self._activate()
        self.sr.lock()
        try:
            lvhdutil.inflate(self.sr.journaler, self.sr.uuid, self.uuid, size)
            util.fistpoint.activate("LVHDRT_inflating_the_parent",self.sr.uuid)
        finally:
            self.sr.unlock()
        self.sizeLV = self.sr.lvmCache.getSize(self.lvName)
        self._sizeVHD = -1

    def deflate(self):
        """deflate the LV containing the VHD to minimum"""
        if self.raw:
            return
        self._activate()
        self.sr.lock()
        try:
            lvhdutil.deflate(self.sr.lvmCache, self.lvName, self.getSizeVHD())
        finally:
            self.sr.unlock()
        self.sizeLV = self.sr.lvmCache.getSize(self.lvName)
        self._sizeVHD = -1

    def inflateFully(self):
        self.inflate(lvhdutil.calcSizeVHDLV(self.sizeVirt))

    def inflateParentForCoalesce(self):
        """Inflate the parent only as much as needed for the purposes of
        coalescing"""
        if self.parent.raw:
            return
        inc = self._calcExtraSpaceForCoalescing()
        if inc > 0:
            util.fistpoint.activate("LVHDRT_coalescing_before_inflate_grandparent",self.sr.uuid)
            self.parent.inflate(self.parent.sizeLV + inc)

    def updateBlockInfo(self):
        if not self.raw:
            VDI.updateBlockInfo(self)

    def rename(self, uuid):
        oldUuid = self.uuid
        oldLVName = self.lvName
        VDI.rename(self, uuid)
        self.lvName = lvhdutil.LV_PREFIX[lvhdutil.VDI_TYPE_VHD] + self.uuid
        if self.raw:
            self.lvName = lvhdutil.LV_PREFIX[lvhdutil.VDI_TYPE_RAW] + self.uuid
        self.path = os.path.join(self.sr.path, self.lvName)
        assert(not self.sr.lvmCache.checkLV(self.lvName))

        self.sr.lvmCache.rename(oldLVName, self.lvName)
        if self.sr.lvActivator.get(oldUuid, False):
            self.sr.lvActivator.replace(oldUuid, self.uuid, self.lvName, False)

        ns = lvhdutil.NS_PREFIX_LVM + self.sr.uuid
        (cnt, bcnt) = RefCounter.check(oldUuid, ns)
        RefCounter.set(self.uuid, cnt, bcnt, ns)
        RefCounter.reset(oldUuid, ns)

    def delete(self):
        if len(self.children) > 0:
            raise util.SMException("VDI %s has children, can't delete" % \
                    self.uuid)
        self.sr.lock()
        try:
            self.sr.lvmCache.remove(self.lvName)
        finally:
            self.sr.unlock()
        RefCounter.reset(self.uuid, lvhdutil.NS_PREFIX_LVM + self.sr.uuid)
        self.sr.xapi.forgetVDI(self.uuid)
        VDI.delete(self)

    def getSizeVHD(self):
        if self._sizeVHD == -1:
            self._loadInfoSizeVHD()
        return self._sizeVHD

    def _loadInfoSizeVHD(self):
        """Get the physical utilization of the VHD file. We do it individually
        (and not using the VHD batch scanner) as an optimization: this info is
        relatively expensive and we need it only for VDI's involved in
        coalescing."""
        if self.raw:
            return
        self._activate()
        self._sizeVHD = vhdutil.getSizePhys(self.path)
        if self._sizeVHD <= 0:
            raise util.SMException("phys size of %s = %d" % \
                    (self.toString(), self._sizeVHD))

    def _loadInfoHidden(self):
        if self.raw:
            self.hidden = self.sr.lvmCache.getHidden(self.lvName)
        else:
            VDI._loadInfoHidden(self)

    def _setHidden(self, hidden = True):
        if self.raw:
            self.sr.lvmCache.setHidden(self.lvName, hidden)
            self.hidden = hidden
        else:
            VDI._setHidden(self, hidden)

    def toString(self):
        strType = "VHD"
        if self.raw:
            strType = "RAW"
        strHidden = ""
        if self.hidden:
            strHidden = "*"
        strSizeVHD = ""
        if self._sizeVHD > 0:
            strSizeVHD = Util.num2str(self._sizeVHD)
        strActive = "n"
        if self.lvActive:
            strActive = "a"
        return "%s%s[%s](%s/%s/%s|%s)" % (strHidden, self.uuid[0:8], strType,
                Util.num2str(self.sizeVirt), strSizeVHD,
                Util.num2str(self.sizeLV), strActive)

    def validate(self):
        if not self.raw:
            VDI.validate(self)

    def _coalesceBegin(self):
        """LVHD parents must first be activated, inflated, and made writable"""
        try:
            self._activateChain()
            self.sr.lvmCache.setReadonly(self.parent.lvName, False)
            self.parent.validate()
            self.inflateParentForCoalesce()
            VDI._coalesceBegin(self)
        finally:
            self.parent._loadInfoSizeVHD()
            self.parent.deflate()
            self.sr.lvmCache.setReadonly(self.lvName, True)

    def _setParent(self, parent):
        self._activate()
        if self.lvReadonly:
            self.sr.lvmCache.setReadonly(self.lvName, False)

        try:
            vhdutil.setParent(self.path, parent.path, parent.raw)
        finally:
            if self.lvReadonly:
                self.sr.lvmCache.setReadonly(self.lvName, True)
        self._deactivate()
        self.parent = parent
        self.parentUuid = parent.uuid
        parent.children.append(self)
        try:
            self.setConfigUpdate(self.DB_VHD_PARENT, self.parentUuid)
            Util.log("Updated the vhd-parent field for child %s with %s" % \
                     (self.uuid, self.parentUuid))
        except:
            Util.log("Failed to update the vhd-parent with %s for child %s" % \
                     (self.parentUuid, self.uuid))

    def _activate(self):
        self.sr.lvActivator.activate(self.uuid, self.lvName, False)

    def _activateChain(self):
        vdi = self
        while vdi:
            vdi._activate()
            vdi = vdi.parent

    def _deactivate(self):
        self.sr.lvActivator.deactivate(self.uuid, False)

    def _increaseSizeVirt(self, size, atomic = True):
        "ensure the virtual size of 'self' is at least 'size'"
        self._activate()
        if not self.raw:
            VDI._increaseSizeVirt(self, size, atomic)
            return

        # raw VDI case
        offset = self.sizeLV
        if self.sizeVirt < size:
            oldSize = self.sizeLV
            self.sizeLV = util.roundup(lvutil.LVM_SIZE_INCREMENT, size)
            Util.log("  Growing %s: %d->%d" % (self.path, oldSize, self.sizeLV))
            self.sr.lvmCache.setSize(self.lvName, self.sizeLV)
            offset = oldSize
        unfinishedZero = False
        jval = self.sr.journaler.get(self.JRN_ZERO, self.uuid)
        if jval:
            unfinishedZero = True
            offset = int(jval)
        length = self.sizeLV - offset
        if not length:
            return

        if unfinishedZero:
            Util.log("  ==> Redoing unfinished zeroing out")
        else:
            self.sr.journaler.create(self.JRN_ZERO, self.uuid, \
                    str(offset))
        Util.log("  Zeroing %s: from %d, %dB" % (self.path, offset, length))
        abortTest = lambda:IPCFlag(self.sr.uuid).test(FLAG_TYPE_ABORT)
        func = lambda: util.zeroOut(self.path, offset, length)
        Util.runAbortable(func, True, self.sr.uuid, abortTest,
                VDI.POLL_INTERVAL, 0)
        self.sr.journaler.remove(self.JRN_ZERO, self.uuid)

    def _setSizeVirt(self, size):
        """WARNING: do not call this method directly unless all VDIs in the
        subtree are guaranteed to be unplugged (and remain so for the duration
        of the operation): this operation is only safe for offline VHDs"""
        self._activate()
        jFile = lvhdutil.createVHDJournalLV(self.sr.lvmCache, self.uuid,
                vhdutil.MAX_VHD_JOURNAL_SIZE)
        try:
            lvhdutil.setSizeVirt(self.sr.journaler, self.sr.uuid, self.uuid,
                    size, jFile)
        finally:
            lvhdutil.deleteVHDJournalLV(self.sr.lvmCache, self.uuid)

    def _queryVHDBlocks(self):
        self._activate()
        return VDI._queryVHDBlocks(self)

    def _calcExtraSpaceForCoalescing(self):
        if self.parent.raw:
            return 0 # raw parents are never deflated in the first place
        sizeCoalesced = lvhdutil.calcSizeVHDLV(self._getCoalescedSizeData())
        Util.log("Coalesced size = %s" % Util.num2str(sizeCoalesced))
        return sizeCoalesced - self.parent.sizeLV

    def _calcExtraSpaceForLeafCoalescing(self):
        """How much extra space in the SR will be required to
        [live-]leaf-coalesce this VDI"""
        # we can deflate the leaf to minimize the space requirements
        deflateDiff = self.sizeLV - lvhdutil.calcSizeLV(self.getSizeVHD())
        return self._calcExtraSpaceForCoalescing() - deflateDiff

    def _calcExtraSpaceForSnapshotCoalescing(self):
        return self._calcExtraSpaceForCoalescing() + \
                lvhdutil.calcSizeLV(self.getSizeVHD())

    def _resizeArgs(self, size):
        args = VDI._resizeArgs(self, size)
        args["vgName"] = self.sr.vgName
        args["lvSize"] = str(self.sizeLV)
        return args



################################################################################
#
# SR
#
class SR:
    TYPE_FILE = "file"
    TYPE_LVHD = "lvhd"
    TYPES = [TYPE_LVHD, TYPE_FILE]

    LOCK_RETRY_INTERVAL = 3
    LOCK_RETRY_ATTEMPTS = 20
    LOCK_RETRY_ATTEMPTS_LOCK = 100

    SCAN_RETRY_ATTEMPTS = 3

    JRN_CLONE = "clone" # journal entry type for the clone operation (from SM)
    TMP_RENAME_PREFIX = "OLD_"

    KEY_OFFLINE_COALESCE_NEEDED = "leaf_coalesce_need_offline"
    KEY_OFFLINE_COALESCE_OVERRIDE = "leaf_coalesce_offline_override"

    def getInstance(uuid, xapiSession):
        xapi = XAPI(xapiSession, uuid)
        type = normalizeType(xapi.srRecord["type"])
        if type == SR.TYPE_FILE:
            return FileSR(uuid, xapi)
        elif type == SR.TYPE_LVHD:
            return LVHDSR(uuid, xapi)
        raise util.SMException("SR type %s not recognized" % type)
    getInstance = staticmethod(getInstance)

    def __init__(self, uuid, xapi):
        self.uuid = uuid
        self.path = ""
        self.name = ""
        self.vdis = {}
        self.vdiTrees = []
        self.journaler = None
        self.xapi = xapi
        self._locked = 0
        self._srLock = lock.Lock(vhdutil.LOCK_TYPE_SR, self.uuid)
        self.name = unicode(self.xapi.srRecord["name_label"]).encode("utf-8", "replace")
        self._failedCoalesceTargets = []

        if not self.xapi.isPluggedHere():
            raise util.SMException("SR %s not attached on this host" % uuid)

        if not self.xapi.isMaster():
            raise util.SMException("This host is NOT master, will not run")

    def scan(self, force = False):
        """Scan the SR and load VDI info for each VDI. If called repeatedly,
        update VDI objects if they already exist"""
        pass # abstract

    def scanLocked(self, force = False):
        self.lock()
        try:
            self.scan(force)
        finally:
            self.unlock()

    def getVDI(self, uuid):
        return self.vdis.get(uuid)

    def hasWork(self):
        if len(self.findGarbage()) > 0:
            return True
        if self.findCoalesceable():
            return True
        if self.findLeafCoalesceable():
            return True
        if self.needUpdateBlockInfo():
            return True
        return False

    def findCoalesceable(self):
        """Find a coalesceable VDI. Return a vdi that should be coalesced
        (choosing one among all coalesceable candidates according to some
        criteria) or None if there is no VDI that could be coalesced"""
        # finish any VDI for which a relink journal entry exists first
        journals = self.journaler.getAll(VDI.JRN_RELINK)
        for uuid in journals.iterkeys():
            vdi = self.getVDI(uuid)
            if vdi and vdi not in self._failedCoalesceTargets:
                return vdi

        candidates = []
        for vdi in self.vdis.values():
            if vdi.isCoalesceable() and vdi not in self._failedCoalesceTargets:
                candidates.append(vdi)

        # pick one in the tallest tree
        treeHeight = dict()
        for c in candidates:
            height = c.getTreeRoot().getTreeHeight()
            if treeHeight.get(height):
                treeHeight[height].append(c)
            else:
                treeHeight[height] = [c]

        freeSpace = self.getFreeSpace()
        heights = treeHeight.keys()
        heights.sort(reverse=True)
        for h in heights:
            for c in treeHeight[h]:
                spaceNeeded = c._calcExtraSpaceForCoalescing()
                if spaceNeeded <= freeSpace:
                    Util.log("Coalesce candidate: %s (tree height %d)" % \
                            (c.toString(), h))
                    return c
                else:
                    Util.log("No space to coalesce %s (free space: %d)" % \
                            (c.toString(), freeSpace))
        return None

    def findLeafCoalesceable(self):
        """Find leaf-coalesceable VDIs in each VHD tree"""
        candidates = []
        for vdi in self.vdis.values():
            if not vdi.isLeafCoalesceable():
                continue
            if vdi in self._failedCoalesceTargets:
                continue
            if vdi.getConfig(vdi.DB_LEAFCLSC) == vdi.LEAFCLSC_DISABLED:
                Util.log("Leaf-coalesce disabled for %s" % vdi.toString())
                continue
            if not (AUTO_ONLINE_LEAF_COALESCE_ENABLED or \
                    vdi.getConfig(vdi.DB_LEAFCLSC) == vdi.LEAFCLSC_FORCE):
                continue
            candidates.append(vdi)

        freeSpace = self.getFreeSpace()
        for candidate in candidates:
            # check the space constraints to see if leaf-coalesce is actually 
            # feasible for this candidate
            spaceNeeded = candidate._calcExtraSpaceForSnapshotCoalescing()
            spaceNeededLive = spaceNeeded
            if spaceNeeded > freeSpace:
                spaceNeededLive = candidate._calcExtraSpaceForLeafCoalescing()
                if candidate.canLiveCoalesce():
                    spaceNeeded = spaceNeededLive
            if spaceNeeded <= freeSpace:
                Util.log("Leaf-coalesce candidate: %s" % candidate.toString())
                return candidate
            else:
                Util.log("No space to leaf-coalesce %s (free space: %d)" % \
                        (candidate.toString(), freeSpace))
                if spaceNeededLive <= freeSpace:
                    Util.log("...but enough space if skip snap-coalesce")
                    candidate.setConfigUpdate(VDI.DB_LEAFCLSC, 
                            VDI.LEAFCLSC_OFFLINE)

        return None

    def coalesce(self, vdi, dryRun):
        """Coalesce vdi onto parent"""
        Util.log("Coalescing %s -> %s" % \
                (vdi.toString(), vdi.parent.toString()))
        if dryRun:
            return

        try:
            self._coalesce(vdi)
        except util.SMException, e:
            if isinstance(e, AbortException):
                self.cleanup()
                raise
            else:
                self._failedCoalesceTargets.append(vdi)
                Util.logException("coalesce")
                Util.log("Coalesce failed, skipping")
        self.cleanup()

    def coalesceLeaf(self, vdi, dryRun):
        """Leaf-coalesce vdi onto parent"""
        Util.log("Leaf-coalescing %s -> %s" % \
                (vdi.toString(), vdi.parent.toString()))
        if dryRun:
            return

        try:
            try:
                self._coalesceLeaf(vdi)
            finally:
                vdi.delConfigUpdate(vdi.DB_LEAFCLSC)
        except (util.SMException, XenAPI.Failure), e:
            if isinstance(e, AbortException):
                self.cleanup()
                raise
            else:
                self._failedCoalesceTargets.append(vdi)
                Util.logException("leaf-coalesce")
                Util.log("Leaf-coalesce failed, skipping")
        self.cleanup()

    def garbageCollect(self, dryRun = False):
        vdiList = self.findGarbage()
        Util.log("Found %d VDIs for deletion:" % len(vdiList))
        for vdi in vdiList:
            Util.log("  %s" % vdi.toString())
        if not dryRun:
            self.deleteVDIs(vdiList)
        self.cleanupJournals(dryRun)

    def findGarbage(self):
        vdiList = []
        for vdi in self.vdiTrees:
            vdiList.extend(vdi.getAllPrunable())
        return vdiList

    def deleteVDIs(self, vdiList):
        for vdi in vdiList:
            if IPCFlag(self.uuid).test(FLAG_TYPE_ABORT):
                raise AbortException("Aborting due to signal")
            Util.log("Deleting unlinked VDI %s" % vdi.toString())
            self.deleteVDI(vdi)

    def deleteVDI(self, vdi):
        assert(len(vdi.children) == 0)
        del self.vdis[vdi.uuid]
        if vdi.parent:
            vdi.parent.children.remove(vdi)
        if vdi in self.vdiTrees:
            self.vdiTrees.remove(vdi)
        vdi.delete()

    def getFreeSpace(self):
        return 0

    def cleanup(self):
        Util.log("In cleanup")
        return

    def toString(self):
        if self.name:
            ret = "%s ('%s')" % (self.uuid[0:4], self.name)
        else:
            ret = "%s" % self.uuid
        return ret

    def printVDIs(self):
        Util.log("-- SR %s has %d VDIs (%d VHD trees) --" % \
                (self.toString(), len(self.vdis), len(self.vdiTrees)))
        for vdi in self.vdiTrees:
            vdi.printTree()

    def lock(self):
        """Acquire the SR lock. Nested acquire()'s are ok. Check for Abort
        signal to avoid deadlocking (trying to acquire the SR lock while the
        lock is held by a process that is trying to abort us)"""
        self._locked += 1
        if self._locked > 1:
            return

        abortFlag = IPCFlag(self.uuid)
        for i in range(SR.LOCK_RETRY_ATTEMPTS_LOCK):
            if self._srLock.acquireNoblock():
                return
            if abortFlag.test(FLAG_TYPE_ABORT):
                raise AbortException("Abort requested")
            time.sleep(SR.LOCK_RETRY_INTERVAL)
        raise util.SMException("Unable to acquire the SR lock")

    def unlock(self):
        assert(self._locked > 0)
        self._locked -= 1
        if self._locked == 0:
            self._srLock.release()

    def needUpdateBlockInfo(self):
        for vdi in self.vdis.values():
            if vdi.scanError or len(vdi.children) == 0:
                continue
            if not vdi.getConfig(vdi.DB_VHD_BLOCKS):
                return True
        return False

    def updateBlockInfo(self):
        for vdi in self.vdis.values():
            if vdi.scanError or len(vdi.children) == 0:
                continue
            if not vdi.getConfig(vdi.DB_VHD_BLOCKS):
                vdi.updateBlockInfo()

    def cleanupCoalesceJournals(self):
        """Remove stale coalesce VDI indicators"""
        entries = self.journaler.getAll(VDI.JRN_COALESCE)
        for uuid, jval in entries.iteritems():
            self.journaler.remove(VDI.JRN_COALESCE, uuid)

    def cleanupJournals(self, dryRun):
        """delete journal entries for non-existing VDIs"""
        for t in [LVHDVDI.JRN_ZERO, VDI.JRN_RELINK, SR.JRN_CLONE]:
            entries = self.journaler.getAll(t)
            for uuid, jval in entries.iteritems():
                if self.getVDI(uuid):
                    continue
                if t == SR.JRN_CLONE:
                    baseUuid, clonUuid = jval.split("_")
                    if self.getVDI(baseUuid):
                        continue
                Util.log("  Deleting stale '%s' journal entry for %s "
                        "(%s)" % (t, uuid, jval))
                if not dryRun:
                    self.journaler.remove(t, uuid)

    def _coalesce(self, vdi):
        # JRN_COALESCE is used to check which VDI is being coalesced in order 
        # to decide whether to abort the coalesce. We remove the journal as 
        # soon as the VHD coalesce step is done, because we don't expect the 
        # rest of the process to take long
        self.journaler.create(vdi.JRN_COALESCE, vdi.uuid, "1")
        vdi._coalesceBegin()
        self.journaler.remove(vdi.JRN_COALESCE, vdi.uuid)

        util.fistpoint.activate("LVHDRT_before_create_relink_journal",self.uuid)

        # we now need to relink the children: lock the SR to prevent ops like 
        # SM.clone from manipulating the VDIs we'll be relinking and rescan the 
        # SR first in case the children changed since the last scan
        if not self.journaler.get(vdi.JRN_RELINK, vdi.uuid):
            self.journaler.create(vdi.JRN_RELINK, vdi.uuid, "1")

        self.lock()
        try:
            self.scan()
            vdi._relinkSkip()
        finally:
            self.unlock()

        vdi.parent._reloadChildren(vdi)
        self.journaler.remove(vdi.JRN_RELINK, vdi.uuid)

    def _coalesceLeaf(self, vdi):
        """Leaf-coalesce VDI vdi. Return true if we succeed, false if we cannot
        complete due to external changes, namely vdi_delete and vdi_snapshot 
        that alter leaf-coalescibility of vdi"""
        while not vdi.canLiveCoalesce():
            prevSizeVHD = vdi.getSizeVHD()
            if not self._snapshotCoalesce(vdi):
                return False
            if vdi.getSizeVHD() >= prevSizeVHD:
                Util.log("Snapshot-coalesce did not help, abandoning attempts")
                vdi.setConfigUpdate(vdi.DB_LEAFCLSC, vdi.LEAFCLSC_OFFLINE)
                break
        return self._liveLeafCoalesce(vdi)

    def _snapshotCoalesce(self, vdi):
        # Note that because we are not holding any locks here, concurrent SM 
        # operations may change this tree under our feet. In particular, vdi 
        # can be deleted, or it can be snapshotted.
        assert(AUTO_ONLINE_LEAF_COALESCE_ENABLED)
        Util.log("Single-snapshotting %s" % vdi.toString())
        util.fistpoint.activate("LVHDRT_coaleaf_delay_1", self.uuid)
        try:
            ret = self.xapi.singleSnapshotVDI(vdi)
            Util.log("Single-snapshot returned: %s" % ret)
        except XenAPI.Failure, e:
            if self.xapi.isInvalidHandleError(e):
                Util.log("The VDI appears to have been concurrently deleted")
                return False
            raise
        self.scanLocked()
        tempSnap = vdi.parent
        if not tempSnap.isCoalesceable():
            Util.log("The VDI appears to have been concurrently snapshotted")
            return False
        Util.log("Coalescing parent %s" % tempSnap.toString())
        util.fistpoint.activate("LVHDRT_coaleaf_delay_2", self.uuid)
        self._coalesce(tempSnap)
        self.deleteVDI(tempSnap)
        if not vdi.isLeafCoalesceable():
            Util.log("The VDI tree appears to have been altered since")
            return False
        return True

    def _liveLeafCoalesce(self, vdi):
        args = {"srUuid": self.uuid, "vdiUuid": vdi.uuid}
        util.fistpoint.activate("LVHDRT_coaleaf_delay_3", self.uuid)
        try:
            if not self.xapi.atomicOp([vdi], "coalesce_leaf", args, True):
                Util.log("%s is no longer leaf-coalesceable" % vdi.toString())
                return False
        except XenAPI.Failure, e:
            if self.xapi.isInvalidHandleError(e):
                Util.log("The VDI appears to have been deleted meanwhile")
                return False
        self.scanLocked()
        return True

    def _doCoalesceLeaf(self, vdi):
        pass # abstract

    def _removeStaleVDIs(self, uuidsPresent):
        for uuid in self.vdis.keys():
            if not uuid in uuidsPresent:
                Util.log("VDI %s disappeared since last scan" % \
                        self.vdis[uuid].toString())
                del self.vdis[uuid]

    def _buildTree(self, force):
        self.vdiTrees = []
        for vdi in self.vdis.values():
            if vdi.parentUuid:
                parent = self.getVDI(vdi.parentUuid)
                if not parent:
                    if vdi.uuid.startswith(self.TMP_RENAME_PREFIX):
                        self.vdiTrees.append(vdi)
                        continue
                    if force:
                        Util.log("ERROR: Parent VDI %s not found! (for %s)" % \
                                (vdi.parentUuid, vdi.uuid))
                        self.vdiTrees.append(vdi)
                        continue
                    else:
                        raise util.SMException("Parent VDI %s of %s not " \
                                "found" % (vdi.parentUuid, vdi.uuid))
                vdi.parent = parent
                parent.children.append(vdi)
            else:
                self.vdiTrees.append(vdi)


class FileSR(SR):
    TYPE = SR.TYPE_FILE

    def __init__(self, uuid, xapi):
        SR.__init__(self, uuid, xapi)
        self.path = "/var/run/sr-mount/%s" % self.uuid
        self.journaler = fjournaler.Journaler(self.path)

    def scan(self, force = False):
        if not util.pathexists(self.path):
            raise util.SMException("directory %s not found!" % self.uuid)
        vhds = self._scan(force)
        for uuid, vhdInfo in vhds.iteritems():
            vdi = self.getVDI(uuid)
            if not vdi:
                Util.log("Found new VDI when scanning: %s" % uuid)
                vdi = FileVDI(self, uuid)
                self.vdis[uuid] = vdi
            vdi.load(vhdInfo)
        self._removeStaleVDIs(vhds.keys())
        self._buildTree(force)
        self.printVDIs()

    def getFreeSpace(self):
        return util.get_fs_size(self.path) - util.get_fs_utilisation(self.path)

    def findLeafCoalesceable(self):
        return None # not implemented for FileSR

    def _scan(self, force):
        for i in range(SR.SCAN_RETRY_ATTEMPTS):
            error = False
            pattern = os.path.join(self.path, "*%s" % FileVDI.FILE_SUFFIX)
            vhds = vhdutil.getAllVHDs(pattern, FileVDI.extractUuid)
            for uuid, vhdInfo in vhds.iteritems():
                if vhdInfo.error:
                    error = True
                    break
            if not error:
                return vhds
            Util.log("Scan error on attempt %d" % i)
        if force:
            return vhds
        raise util.SMException("Scan error")

    def deleteVDI(self, vdi):
        self._checkSlaves(vdi)
        SR.deleteVDI(self, vdi)

    def _checkSlaves(self, vdi):
        onlineHosts = self.xapi.getOnlineHosts()
        abortFlag = IPCFlag(self.uuid)
        for pbdRecord in self.xapi.getAttachedPBDs():
            hostRef = pbdRecord["host"]
            if hostRef == self.xapi._hostRef:
                continue
            if abortFlag.test(FLAG_TYPE_ABORT):
                raise AbortException("Aborting due to signal")
            try:
                self._checkSlave(hostRef, vdi)
            except util.CommandException:
                if onlineHosts.__contains__(hostRef):
                    raise

    def _checkSlave(self, hostRef, vdi):
        call  = (hostRef, "nfs-on-slave", "check", { 'path': vdi.path })
        Util.log("Checking with slave: %s" % repr(call))
        _host = self.xapi.session.xenapi.host
        text  = _host.call_plugin(*call)

class LVHDSR(SR):
    TYPE = SR.TYPE_LVHD
    SUBTYPES = ["lvhdoiscsi", "lvhdohba"]

    def __init__(self, uuid, xapi):
        SR.__init__(self, uuid, xapi)
        self.vgName = "%s%s" % (lvhdutil.VG_PREFIX, self.uuid)
        self.path = os.path.join(lvhdutil.VG_LOCATION, self.vgName)
        self.lvmCache = lvmcache.LVMCache(self.vgName)
        self.lvActivator = LVActivator(self.uuid, self.lvmCache)
        self.journaler = journaler.Journaler(self.lvmCache)

    def deleteVDI(self, vdi):
        if self.lvActivator.get(vdi.uuid, False):
            self.lvActivator.deactivate(vdi.uuid, False)
        self._checkSlaves(vdi)
        SR.deleteVDI(self, vdi)

    def getFreeSpace(self):
        stats = lvutil._getVGstats(self.vgName)
        return stats['physical_size'] - stats['physical_utilisation']

    def cleanup(self):
        if not self.lvActivator.deactivateAll():
            Util.log("ERROR deactivating LVs while cleaning up")

    def needUpdateBlockInfo(self):
        for vdi in self.vdis.values():
            if vdi.scanError or vdi.raw or len(vdi.children) == 0:
                continue
            if not vdi.getConfig(vdi.DB_VHD_BLOCKS):
                return True
        return False

    def updateBlockInfo(self):
        for vdi in self.vdis.values():
            if vdi.scanError or vdi.raw or len(vdi.children) == 0:
                continue
            if not vdi.getConfig(vdi.DB_VHD_BLOCKS):
                vdi.updateBlockInfo()

    def scan(self, force = False):
        vdis = self._scan(force)
        for uuid, vdiInfo in vdis.iteritems():
            vdi = self.getVDI(uuid)
            if not vdi:
                Util.log("Found new VDI when scanning: %s" % uuid)
                vdi = LVHDVDI(self, uuid)
                self.vdis[uuid] = vdi
            vdi.load(vdiInfo)
        self._removeStaleVDIs(vdis.keys())
        self._buildTree(force)
        self.printVDIs()
        self._handleInterruptedCoalesceLeaf()

    def _scan(self, force):
        for i in range(SR.SCAN_RETRY_ATTEMPTS):
            error = False
            self.lvmCache.refresh()
            vdis = lvhdutil.getVDIInfo(self.lvmCache)
            for uuid, vdiInfo in vdis.iteritems():
                if vdiInfo.scanError:
                    error = True
                    break
            if not error:
                return vdis
            Util.log("Scan error, retrying (%d)" % i)
        if force:
            return vdis
        raise util.SMException("Scan error")

    def _liveLeafCoalesce(self, vdi):
        """If the parent is raw and the child was resized (virt. size), then
        we'll need to resize the parent, which can take a while due to zeroing
        out of the extended portion of the LV. Do it before pausing the child
        to avoid a protracted downtime"""
        if vdi.parent.raw and vdi.sizeVirt > vdi.parent.sizeVirt:
            self.lvmCache.setReadonly(vdi.parent.lvName, False)
            vdi.parent._increaseSizeVirt(vdi.sizeVirt)

        parentUuid = vdi.parent.uuid
        if not SR._liveLeafCoalesce(self, vdi):
            return False

        # fix the activation records after the UUIDs have been changed
        if self.lvActivator.get(parentUuid, False):
            self.lvActivator.replace(parentUuid, vdi.uuid, vdi.lvName, False)
        else:
            self.lvActivator.remove(vdi.uuid, False)
        return True

    def _doCoalesceLeaf(self, vdi):
        """Actual coalescing of a leaf VDI onto parent. Must be called in an
        offline/atomic context"""
        vdi._activateChain()
        self.journaler.create(VDI.JRN_LEAF, vdi.uuid, vdi.parent.uuid)
        self.lvmCache.setReadonly(vdi.parent.lvName, False)
        vdi.parent._setHidden(False)
        vdi.deflate()
        vdi.inflateParentForCoalesce()
        vdi.parent._increaseSizeVirt(vdi.sizeVirt, False)
        vdi.validate()
        vdi.parent.validate()
        util.fistpoint.activate("LVHDRT_coaleaf_before_coalesce", self.uuid)
        timeout = vdi.LIVE_LEAF_COALESCE_TIMEOUT
        if vdi.getConfig(vdi.DB_LEAFCLSC) == vdi.LEAFCLSC_FORCE:
            Util.log("Leaf-coalesce forced, will not use timeout")
            timeout = 0
        vdi._coalesceVHD(timeout)
        util.fistpoint.activate("LVHDRT_coaleaf_after_coalesce", self.uuid)
        vdi.parent.validate()
        #vdi._verifyContents(timeout / 2)

        # rename
        vdiUuid = vdi.uuid
        oldNameLV = vdi.lvName
        origParentUuid = vdi.parent.uuid
        vdi.rename(self.TMP_RENAME_PREFIX + vdiUuid)
        util.fistpoint.activate("LVHDRT_coaleaf_one_renamed", self.uuid)
        vdi.parent.rename(vdiUuid)
        util.fistpoint.activate("LVHDRT_coaleaf_both_renamed", self.uuid)
        self._updateSlavesOnRename(vdi.parent, oldNameLV)

        # Note that "vdi.parent" is now the single remaining leaf and "vdi" is 
        # garbage

        # update the VDI record
        vdi.parent.delConfig(VDI.DB_VHD_PARENT)
        if vdi.parent.raw:
            vdi.parent.setConfig(VDI.DB_VDI_TYPE, lvhdutil.VDI_TYPE_RAW)
        vdi.parent.delConfig(VDI.DB_VHD_BLOCKS)
        vdi.parent.updateConfig()
        util.fistpoint.activate("LVHDRT_coaleaf_after_vdirec", self.uuid)

        # fix the refcounts: the remaining node should inherit the binary 
        # refcount from the leaf (because if it was online, it should remain 
        # refcounted as such), but the normal refcount from the parent (because 
        # this node is really the parent node) - minus 1 if it is online (since 
        # non-leaf nodes increment their normal counts when they are online and 
        # we are now a leaf, storing that 1 in the binary refcount).
        ns = lvhdutil.NS_PREFIX_LVM + self.uuid
        cCnt, cBcnt = RefCounter.check(vdi.uuid, ns)
        pCnt, pBcnt = RefCounter.check(vdi.parent.uuid, ns)
        pCnt = pCnt - cBcnt
        assert(pCnt >= 0)
        RefCounter.set(vdi.parent.uuid, pCnt, cBcnt, ns)

        # delete the obsolete leaf & inflate the parent (in that order, to 
        # minimize free space requirements)
        parent = vdi.parent
        vdi._setHidden(True)
        vdi.parent.children = []
        vdi.parent = None
        util.fistpoint.activate("LVHDRT_coaleaf_before_delete", self.uuid)
        self.deleteVDI(vdi)
        util.fistpoint.activate("LVHDRT_coaleaf_after_delete", self.uuid)
        self.xapi.forgetVDI(origParentUuid)
        parent.inflateFully()

        util.fistpoint.activate("LVHDRT_coaleaf_before_remove_j", self.uuid)
        self.journaler.remove(VDI.JRN_LEAF, vdiUuid)

    def _handleInterruptedCoalesceLeaf(self):
        """An interrupted leaf-coalesce operation may leave the VHD tree in an 
        inconsistent state. If the old-leaf VDI is still present, we revert the 
        operation (in case the original error is persistent); otherwise we must 
        finish the operation"""
        entries = self.journaler.getAll(VDI.JRN_LEAF)
        for uuid, parentUuid in entries.iteritems():
            childLV = lvhdutil.LV_PREFIX[lvhdutil.VDI_TYPE_VHD] + uuid
            tmpChildLV = lvhdutil.LV_PREFIX[lvhdutil.VDI_TYPE_VHD] + \
                    self.TMP_RENAME_PREFIX + uuid
            parentLV1 = lvhdutil.LV_PREFIX[lvhdutil.VDI_TYPE_VHD] + parentUuid
            parentLV2 = lvhdutil.LV_PREFIX[lvhdutil.VDI_TYPE_RAW] + parentUuid
            parentPresent = (self.lvmCache.checkLV(parentLV1) or \
                    self.lvmCache.checkLV(parentLV2))
            if parentPresent or self.lvmCache.checkLV(tmpChildLV):
                self._undoInterruptedCoalesceLeaf(uuid, parentUuid)
            else:
                self._finishInterruptedCoalesceLeaf(uuid, parentUuid)
            self.journaler.remove(VDI.JRN_LEAF, uuid)

    def _undoInterruptedCoalesceLeaf(self, childUuid, parentUuid):
        Util.log("*** UNDO LEAF-COALESCE")
        parent = self.getVDI(parentUuid)
        if not parent:
            parent = self.getVDI(childUuid)
            if not parent:
                raise util.SMException("Neither %s nor %s found" % \
                        (parentUuid, childUuid))
            Util.log("Renaming parent back: %s -> %s" % (childUuid, parentUuid))
            parent.rename(parentUuid)
        util.fistpoint.activate("LVHDRT_coaleaf_undo_after_rename", self.uuid)

        child = self.getVDI(childUuid)
        if not child:
            child = self.getVDI(self.TMP_RENAME_PREFIX + childUuid)
            if not child:
                raise util.SMException("Neither %s nor %s found" % \
                        (childUuid, self.TMP_RENAME_PREFIX + childUuid))
            Util.log("Renaming child back to %s" % childUuid)
            child.rename(childUuid)
            Util.log("Updating the VDI record")
            child.setConfig(VDI.DB_VHD_PARENT, parentUuid)
            child.setConfig(VDI.DB_VDI_TYPE, lvhdutil.VDI_TYPE_VHD)
            child.updateConfig()
            util.fistpoint.activate("LVHDRT_coaleaf_undo_after_rename2", self.uuid)

            # refcount (best effort - assume that it had succeeded if the 
            # second rename succeeded; if not, this adjustment will be wrong, 
            # leading to a non-deactivation of the LV)
            ns = lvhdutil.NS_PREFIX_LVM + self.uuid
            cCnt, cBcnt = RefCounter.check(child.uuid, ns)
            pCnt, pBcnt = RefCounter.check(parent.uuid, ns)
            pCnt = pCnt + cBcnt
            RefCounter.set(parent.uuid, pCnt, 0, ns)
            util.fistpoint.activate("LVHDRT_coaleaf_undo_after_refcount", self.uuid)

        parent.deflate()
        child.inflateFully()
        util.fistpoint.activate("LVHDRT_coaleaf_undo_after_deflate", self.uuid)
        if child.hidden:
            child._setHidden(False)
        if not parent.hidden:
            parent._setHidden(True)
        if not parent.lvReadonly:
            self.lvmCache.setReadonly(parent.lvName, True)
        util.fistpoint.activate("LVHDRT_coaleaf_undo_end", self.uuid)
        Util.log("*** leaf-coalesce undo successful")
        if util.fistpoint.is_active("LVHDRT_coaleaf_stop_after_recovery"):
            child.setConfigUpdate(VDI.DB_LEAFCLSC, VDI.LEAFCLSC_DISABLED)

    def _finishInterruptedCoalesceLeaf(self, childUuid, parentUuid):
        Util.log("*** FINISH LEAF-COALESCE")
        vdi = self.getVDI(childUuid)
        if not vdi:
            raise util.SMException("VDI %s not found" % childUuid)
        vdi.inflateFully()
        util.fistpoint.activate("LVHDRT_coaleaf_finish_after_inflate", self.uuid)
        try:
            self.xapi.forgetVDI(parentUuid)
        except XenAPI.Failure:
            pass
        util.fistpoint.activate("LVHDRT_coaleaf_finish_end", self.uuid)
        Util.log("*** finished leaf-coalesce successfully")

    def _checkSlaves(self, vdi):
        """Confirm with all slaves in the pool that 'vdi' is not in use. We
        try to check all slaves, including those that the Agent believes are
        offline, but ignore failures for offline hosts. This is to avoid cases
        where the Agent thinks a host is offline but the host is up."""
        args = {"vgName" : self.vgName,
                "action1": "deactivateNoRefcount",
                "lvName1": vdi.lvName,
                "action2": "cleanupLock",
                "uuid2"  : vdi.uuid,
                "ns2"    : lvhdutil.NS_PREFIX_LVM + self.uuid}
        onlineHosts = self.xapi.getOnlineHosts()
        abortFlag = IPCFlag(self.uuid)
        for pbdRecord in self.xapi.getAttachedPBDs():
            hostRef = pbdRecord["host"]
            if hostRef == self.xapi._hostRef:
                continue
            if abortFlag.test(FLAG_TYPE_ABORT):
                raise AbortException("Aborting due to signal")
            Util.log("Checking with slave %s (path %s)" % (hostRef, vdi.path))
            try:
                self.xapi.ensureInactive(hostRef, args)
            except XenAPI.Failure:
                if onlineHosts.__contains__(hostRef):
                    raise

    def _updateSlavesOnRename(self, vdi, oldNameLV):
        activeVBDs = util.get_attached_VBDs(self.xapi.session, vdi.uuid)
        slaves = util.get_hosts(self.xapi.session, activeVBDs)
        pool = self.xapi.session.xenapi.pool.get_all()[0]
        master = self.xapi.session.xenapi.pool.get_master(pool)
        if master in slaves:
            slaves.remove(master)
        if len(slaves) == 0:
            Util.log("VDI %s not attached on any slave" % vdi.toString())
            return

        util.SMlog("Updating %s to %s on %d slaves:" % \
                (oldNameLV, vdi.lvName, len(slaves)))
        for slave in slaves:
            util.SMlog("Updating slave %s" % slave)
            args = {"vgName" : self.vgName,
                    "action1": "deactivateNoRefcount",
                    "lvName1": oldNameLV,
                    "action2": "refresh",
                    "lvName2": vdi.lvName}
            text = self.xapi.session.xenapi.host.call_plugin( \
                    slave, self.xapi.PLUGIN_ON_SLAVE, "multi", args)
            util.SMlog("call-plugin returned: '%s'" % text)


################################################################################
#
#  Helpers
#
def daemonize():
    pid = os.fork()
    if pid:
        os.waitpid(pid, 0)
        Util.log("New PID [%d]" % pid)
        return False
    os.chdir("/")
    os.setsid()
    pid = os.fork()
    if pid:
        Util.log("Will finish as PID [%d]" % pid)
        os._exit(0)
    for fd in [0, 1, 2]:
        try:
            os.close(fd)
        except OSError:
            pass
    # we need to fill those special fd numbers or pread won't work
    sys.stdin = open("/dev/null", 'r')
    sys.stderr = open("/dev/null", 'w')
    sys.stdout = open("/dev/null", 'w')
    return True

def normalizeType(type):
    if type in LVHDSR.SUBTYPES:
        type = SR.TYPE_LVHD
    if type in ["lvm", "lvmoiscsi", "lvmohba"]:
        # temporary while LVHD is symlinked as LVM
        type = SR.TYPE_LVHD
    if type in ["ext", "nfs"]:
        type = SR.TYPE_FILE
    if not type in SR.TYPES:
        raise util.SMException("Unsupported SR type: %s" % type)
    return type

def _gcLoop(sr, dryRun):
    failedCandidates = []
    while True:
        if not sr.xapi.isPluggedHere():
            Util.log("SR no longer attached, exiting")
            break
        sr.scanLocked()
        if not sr.hasWork():
            Util.log("No work, exiting")
            break

        if not lockRunning.acquireNoblock():
            Util.log("Another instance already running, exiting")
            break
        try:
            sr.cleanupCoalesceJournals()
            sr.scanLocked()
            sr.updateBlockInfo()

            if len(sr.findGarbage()) > 0:
                sr.garbageCollect(dryRun)
                sr.xapi.srUpdate()
                continue

            candidate = sr.findCoalesceable()
            if candidate:
                util.fistpoint.activate("LVHDRT_finding_a_suitable_pair",sr.uuid)
                sr.coalesce(candidate, dryRun)
                sr.xapi.srUpdate()
                continue

            candidate = sr.findLeafCoalesceable()
            if candidate:
                sr.coalesceLeaf(candidate, dryRun)
                sr.xapi.srUpdate()
                continue

            Util.log("No work left")
        finally:
            lockRunning.release()

def _gc(session, srUuid, dryRun):
    init(srUuid)
    sr = SR.getInstance(srUuid, session)

    try:
        _gcLoop(sr, dryRun)
    finally:
        sr.cleanup()
        Util.log("Final SR state:")
        Util.log(sr.toString())
        sr.printVDIs()

def _abort(srUuid):
    """If successful, we return holding lockRunning; otherwise exception
    raised."""
    Util.log("=== SR %s: abort ===" % (srUuid))
    init(srUuid)
    if not lockRunning.acquireNoblock():
        gotLock = False
        Util.log("Aborting currently-running instance (SR %s)" % srUuid)
        abortFlag = IPCFlag(srUuid)
        abortFlag.set(FLAG_TYPE_ABORT)
        for i in range(SR.LOCK_RETRY_ATTEMPTS):
            gotLock = lockRunning.acquireNoblock()
            if gotLock:
                break
            time.sleep(SR.LOCK_RETRY_INTERVAL)
        abortFlag.clear(FLAG_TYPE_ABORT)
        if not gotLock:
            raise util.SMException("SR %s: error aborting existing process" % \
                    srUuid)

def coalesceLeafAtomic(session, srUuid, vdiUuid):
    """Coalesce a leaf node onto its parent. This should be invoked in the
    stop_using_these_vdis_() context to ensure that the leaf is offline. It is
    dangerous to invoke this function otherwise. Return True on success, False
    if the target VDI is no longer leaf-coalesceable"""
    Util.log("=== SR %s: coalesceLeafAtomic for VDI %s ===" % (srUuid, vdiUuid))
    init(srUuid)
    sr = SR.getInstance(srUuid, session)
    sr.lock()
    try:
        sr.scan()
        vdi = sr.getVDI(vdiUuid)
        if not vdi.isLeafCoalesceable():
            return False
        try:
            sr._doCoalesceLeaf(vdi)
        except:
            Util.logException("_doCoalesceLeaf")
            sr._handleInterruptedCoalesceLeaf()
            raise
    finally:
        sr.cleanup()
        sr.unlock()
        Util.log("final SR state:")
        Util.log(sr.toString())
        sr.printVDIs()
    return True

def init(srUuid):
    global lockRunning
    if not lockRunning:
        lockRunning = lock.Lock(LOCK_TYPE_RUNNING, srUuid) 

def usage():
    output = """Garbage collect and/or coalesce VHDs in a VHD-based SR

Parameters:
    -u --uuid UUID   SR UUID
 and one of:
    -g --gc          garbage collect, coalesce, and repeat while there is work
    -G --gc_force    garbage collect once, aborting any current operations
    -a --abort       abort any currently running operation (GC or coalesce)
    -q --query       query the current state (GC'ing, coalescing or not running)
    -x --disable     disable GC/coalesce (will be in effect until you exit)

Options:
    -b --background  run in background (return immediately) (valid for -g only)
    -f --force       continue in the presence of VHDs with errors (when doing
                     GC, this might cause removal of any such VHDs) (only valid
                     for -G) (DANGEROUS)
    """
   #-d --dry-run     don't actually perform any SR-modifying operations
   #-t               perform a custom operation (for testing)
    print output
    Util.log("(Invalid usage)")
    sys.exit(1)


##############################################################################
#
#  API
#
def abort(srUuid):
    """Abort GC/coalesce if we are currently GC'ing or coalescing a VDI pair.
    """
    _abort(srUuid)
    Util.log("abort: releasing the process lock")
    lockRunning.release()

def gc(session, srUuid, inBackground, dryRun = False):
    """Garbage collect all deleted VDIs in SR "srUuid". Fork & return 
    immediately if inBackground=True. 
    
    The following algorithm is used:
    1. If we are already GC'ing in this SR, return
    2. If we are already coalescing a VDI pair:
        a. Scan the SR and determine if the VDI pair is GC'able
        b. If the pair is not GC'able, return
        c. If the pair is GC'able, abort coalesce
    3. Scan the SR
    4. If there is nothing to collect, nor to coalesce, return
    5. If there is something to collect, GC all, then goto 3
    6. If there is something to coalesce, coalesce one pair, then goto 3
    """
    Util.log("=== SR %s: gc ===" % srUuid)
    if inBackground:
        if daemonize():
            # we are now running in the background. Catch & log any errors 
            # because there is no other way to propagate them back at this 
            # point
            
            try:
                _gc(None, srUuid, dryRun)
            except AbortException:
                Util.log("Aborted")
            except Exception:
                Util.logException("gc")
                Util.log("* * * * * SR %s: ERROR\n" % srUuid)
            os._exit(0)
    else:
        _gc(session, srUuid, dryRun)

def gc_force(session, srUuid, force = False, dryRun = False):
    """Garbage collect all deleted VDIs in SR "srUuid".
    The following algorithm is used:
    1. If we are already GC'ing or coalescing a VDI pair, abort GC/coalesce
    2. Scan the SR
    3. GC
    4. return
    """
    Util.log("=== SR %s: gc_force ===" % srUuid)
    init(srUuid)
    sr = SR.getInstance(srUuid, session)
    if not lockRunning.acquireNoblock():
        _abort(srUuid)
    else:
        Util.log("Nothing was running, clear to proceed")

    if force:
        Util.log("FORCED: will continue even if there are VHD errors")
    sr.scanLocked(force)
    sr.cleanupCoalesceJournals()

    try:
        sr.garbageCollect(dryRun)
    finally:
        sr.cleanup()
        Util.log("final SR state:")
        Util.log(sr.toString())
        sr.printVDIs()
        lockRunning.release()

def get_state(srUuid):
    """Return whether GC/coalesce is currently running or not. The information
    is not guaranteed for any length of time if the call is not protected by
    locking.
    """
    init(srUuid)
    if lockRunning.acquireNoblock():
        lockRunning.release()
        return False
    return True

def should_preempt(session, srUuid):
    sr = SR.getInstance(srUuid, session)
    entries = sr.journaler.getAll(VDI.JRN_COALESCE)
    if len(entries) == 0:
        return False
    elif len(entries) > 1:
        raise util.SMException("More than one coalesce entry: " + entries)
    sr.scanLocked()
    coalescedUuid = entries.popitem()[0]
    garbage = sr.findGarbage()
    for vdi in garbage:
        if vdi.uuid == coalescedUuid:
            return True
    return False

def get_coalesceable_leaves(session, srUuid, vdiUuids):
    coalesceable = []
    sr = SR.getInstance(srUuid, session)
    sr.scanLocked()
    for uuid in vdiUuids:
        vdi = sr.getVDI(uuid)
        if not vdi:
            raise util.SMException("VDI %s not found" % uuid)
        if vdi.isLeafCoalesceable():
            coalesceable.append(uuid)
    return coalesceable

##############################################################################
#
#  CLI
#
def main():
    action     = ""
    uuid       = ""
    background = False
    force      = False
    dryRun     = False
    test       = False
    shortArgs  = "gGaqxu:bfdt"
    longArgs   = ["gc", "gc_force", "abort", "query", "disable",
            "uuid", "background", "force", "dry-run", "test"]

    try:
        opts, args = getopt.getopt(sys.argv[1:], shortArgs, longArgs)
    except getopt.GetoptError:
        usage()
    for o, a in opts:
        if o in ("-g", "--gc"):
            action = "gc"
        if o in ("-G", "--gc_force"):
            action = "gc_force"
        if o in ("-a", "--abort"):
            action = "abort"
        if o in ("-q", "--query"):
            action = "query"
        if o in ("-x", "--disable"):
            action = "disable"
        if o in ("-u", "--uuid"):
            uuid = a
        if o in ("-b", "--background"):
            background = True
        if o in ("-f", "--force"):
            force = True
        if o in ("-d", "--dry-run"):
            Util.log("Dry run mode")
            dryRun = True
        if o in ("-t"):
            action = "test"

    if not action or not uuid:
        usage()
    elif action != "query":
        print "All output goes in %s" % LOG_FILE

    if action == "gc":
        gc(None, uuid, background, dryRun)
    elif action == "gc_force":
        gc_force(None, uuid, force, dryRun)
    elif action == "abort":
        abort(uuid)
    elif action == "query":
        print "Currently running: %s" % get_state(uuid)
    elif action == "disable":
        print "Disabling GC/coalesce for %s" % uuid
        _abort(uuid)
        raw_input("Press enter to re-enable...")
        print "GC/coalesce re-enabled"
        lockRunning.release()
    elif action == "test":
        Util.log("Test operation")
        pass


if __name__ == '__main__':
    main()
