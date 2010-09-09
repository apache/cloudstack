#!/usr/bin/env python
# $Id: NFSSR.py 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/vm/hypervisor/xenserver/xenserver56/NFSSR.py $
# Copyright (c) 2005-2007 XenSource, Inc. All use and distribution of this
# copyrighted material is governed by and subject to terms and conditions
# as licensed by XenSource, Inc. All other rights reserved.
# Xen, XenSource and XenEnterprise are either registered trademarks or
# trademarks of XenSource Inc. in the United States and/or other countries.
#
#
# FileSR: local-file storage repository

import SR, VDI, SRCommand, FileSR, util
import errno
import os, re, sys
import time
import xml.dom.minidom
import xs_errors
import nfs
import vhdutil
from lock import Lock
import cleanup

CAPABILITIES = ["SR_PROBE","SR_UPDATE", \
                "VDI_CREATE","VDI_DELETE","VDI_ATTACH","VDI_DETACH", \
                "VDI_UPDATE", "VDI_CLONE","VDI_SNAPSHOT","VDI_RESIZE","VDI_RESIZE_ONLINE"]

CONFIGURATION = [ [ 'server', 'hostname or IP address of NFS server (required)' ], \
                  [ 'serverpath', 'path on remote server (required)' ] ]

                  
DRIVER_INFO = {
    'name': 'NFS VHD',
    'description': 'SR plugin which stores disks as VHD files on a remote NFS filesystem',
    'vendor': 'Citrix Systems Inc',
    'copyright': '(C) 2008 Citrix Systems Inc',
    'driver_version': '1.0',
    'required_api_version': '1.0',
    'capabilities': CAPABILITIES,
    'configuration': CONFIGURATION
    }

def echo(fn):
    def wrapped(*v, **k):
        name = fn.__name__
        util.SMlog("#### NFS enter  %s ####" % name )
        res = fn(*v, **k)
        util.SMlog("#### NFS exit  %s ####" % name )
        return res
    return wrapped


# The mountpoint for the directory when performing an sr_probe.  All probes
# are guaranteed to be serialised by xapi, so this single mountpoint is fine.
PROBE_MOUNTPOINT = "probe"
NFSPORT = 2049
DEFAULT_TRANSPORT = "tcp"


class NFSSR(FileSR.FileSR):
    """NFS file-based storage repository"""
    
    def handles(type):
        return type == 'nfs'
    handles = staticmethod(handles)

    @echo
    def load(self, sr_uuid):
        self.lock = Lock(vhdutil.LOCK_TYPE_SR, self.uuid)
        self.sr_vditype = SR.DEFAULT_TAP
        if not self.dconf.has_key('server'):
            raise xs_errors.XenError('ConfigServerMissing')
        self.remoteserver = self.dconf['server']
        self.path = os.path.join(SR.MOUNT_BASE, sr_uuid)

        # Test for the optional 'nfsoptions' dconf attribute
        self.transport = DEFAULT_TRANSPORT
        if self.dconf.has_key('useUDP') and self.dconf['useUDP'] == 'true':
            self.transport = "udp"

    @echo
    def validate_remotepath(self, scan):
        if not self.dconf.has_key('serverpath'):
            if scan:
                try:
                    self.scan_exports(self.dconf['server'])
                except:
                    pass
            raise xs_errors.XenError('ConfigServerPathMissing')
        if not self._isvalidpathstring(self.dconf['serverpath']):
            raise xs_errors.XenError('ConfigServerPathBad', \
                  opterr='serverpath is %s' % self.dconf['serverpath'])
    @echo
    def check_server(self):
        try:
            nfs.check_server_tcp(self.remoteserver)
        except nfs.NfsException, exc:
            raise xs_errors.XenError('NFSVersion',
                                     opterr=exc.errstr)

    @echo
    def mount(self, mountpoint, remotepath):
        try:
            nfs.soft_mount(mountpoint, self.remoteserver, remotepath, self.transport)
        except nfs.NfsException, exc:
            raise xs_errors.XenError('NFSMount', opterr=exc.errstr)

    @echo
    @FileSR.locking("SRUnavailable")
    def attach(self, sr_uuid):
        self.validate_remotepath(False)
        #self.remotepath = os.path.join(self.dconf['serverpath'], sr_uuid)
        self.remotepath = self.dconf['serverpath']
        util._testHost(self.dconf['server'], NFSPORT, 'NFSTarget')
        self.mount_remotepath(sr_uuid)

    @echo
    def mount_remotepath(self, sr_uuid):
        if not self._checkmount():
            self.check_server()
            self.mount(self.path, self.remotepath)

        return super(NFSSR, self).attach(sr_uuid)

    @echo
    @FileSR.locking("SRUnavailable")
    def probe(self):
        # Verify NFS target and port
        util._testHost(self.dconf['server'], NFSPORT, 'NFSTarget')
        
        self.validate_remotepath(True)
        self.check_server()

        temppath = os.path.join(SR.MOUNT_BASE, PROBE_MOUNTPOINT)

        self.mount(temppath, self.dconf['serverpath'])
        try:
            return nfs.scan_srlist(temppath)
        finally:
            try:
                nfs.unmount(temppath, True)
            except:
                pass

    @echo
    @FileSR.locking("SRUnavailable")
    def detach(self, sr_uuid):
        """Detach the SR: Unmounts and removes the mountpoint"""
        if not self._checkmount():
            return
        util.SMlog("Aborting GC/coalesce")
        cleanup.abort(self.uuid)

        # Change directory to avoid unmount conflicts
        os.chdir(SR.MOUNT_BASE)

        try:
            nfs.unmount(self.path, True)
        except nfs.NfsException, exc:
            raise xs_errors.XenError('NFSUnMount', opterr=exc.errstr)

        return super(NFSSR, self).detach(sr_uuid)
        
    @echo
    @FileSR.locking("SRUnavailable")
    def create(self, sr_uuid, size):
        util._testHost(self.dconf['server'], NFSPORT, 'NFSTarget')
        self.validate_remotepath(True)
        if self._checkmount():
            raise xs_errors.XenError('NFSAttached')

        # Set the target path temporarily to the base dir
        # so that we can create the target SR directory
        self.remotepath = self.dconf['serverpath']
        try:
            self.mount_remotepath(sr_uuid)
        except Exception, exn:
            try:
                os.rmdir(self.path)
            except:
                pass
            raise exn

        #newpath = os.path.join(self.path, sr_uuid)
        #if util.ioretry(lambda: util.pathexists(newpath)):
        #    if len(util.ioretry(lambda: util.listdir(newpath))) != 0:
        #        self.detach(sr_uuid)
        #        raise xs_errors.XenError('SRExists')
        #else:
        #    try:
        #        util.ioretry(lambda: util.makedirs(newpath))
        #    except util.CommandException, inst:
        #        if inst.code != errno.EEXIST:
        #            self.detach(sr_uuid)
        #            raise xs_errors.XenError('NFSCreate', 
        #                opterr='remote directory creation error is %d' 
        #                % inst.code)
        self.detach(sr_uuid)

    @echo
    @FileSR.locking("SRUnavailable")
    def delete(self, sr_uuid):
        # try to remove/delete non VDI contents first
        super(NFSSR, self).delete(sr_uuid)
        try:
            if self._checkmount():
                self.detach(sr_uuid)

            # Set the target path temporarily to the base dir
            # so that we can remove the target SR directory
            self.remotepath = self.dconf['serverpath']
            self.mount_remotepath(sr_uuid)
            newpath = os.path.join(self.path, sr_uuid)

            if util.ioretry(lambda: util.pathexists(newpath)):
                util.ioretry(lambda: os.rmdir(newpath))
            self.detach(sr_uuid)
        except util.CommandException, inst:
            self.detach(sr_uuid)
            if inst.code != errno.ENOENT:
                raise xs_errors.XenError('NFSDelete')

    def vdi(self, uuid, loadLocked = False):
        if not loadLocked:
            return NFSFileVDINolock(self, uuid)
        return NFSFileVDI(self, uuid)
   
    @echo 
    def _checkmount(self):
        return util.ioretry(lambda: util.pathexists(self.path)) \
               and util.ioretry(lambda: util.ismount(self.path))

    @echo
    def scan_exports(self, target):
        util.SMlog("scanning2 (target=%s)" % target)
        dom = nfs.scan_exports(target)
        print >>sys.stderr,dom.toprettyxml()

class NFSFileVDI(FileSR.FileVDI):
    @echo
    def attach(self, sr_uuid, vdi_uuid):
        try:
            vdi_ref = self.sr.srcmd.params['vdi_ref']
            self.session.xenapi.VDI.remove_from_xenstore_data(vdi_ref, \
                    "vdi-type")
            self.session.xenapi.VDI.remove_from_xenstore_data(vdi_ref, \
                    "storage-type")
            self.session.xenapi.VDI.add_to_xenstore_data(vdi_ref, \
                    "storage-type", "nfs")
        except:
            util.logException("NFSSR:attach")
            pass

        return super(NFSFileVDI, self).attach(sr_uuid, vdi_uuid)

    @echo
    def clone(self, sr_uuid, vdi_uuid):
        timestamp_before = int(util.get_mtime(self.sr.path))
        ret = super(NFSFileVDI, self).clone(sr_uuid, vdi_uuid)
        timestamp_after = int(util.get_mtime(self.sr.path))
        if timestamp_after == timestamp_before:
            util.SMlog("SR dir timestamp didn't change, updating")
            timestamp_after += 1
            os.utime(self.sr.path, (timestamp_after, timestamp_after))
        return ret

class NFSFileVDINolock(NFSFileVDI):
    def load(self, vdi_uuid):
        self.lock = self.sr.lock
        self._load(vdi_uuid)


if __name__ == '__main__':
    SRCommand.run(NFSSR, DRIVER_INFO)
else:
    SR.registerSR(NFSSR)
