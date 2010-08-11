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
# LUNperVDI: Generic Raw LUN handler, used by HBASR and ISCSISR
#

import SR, VDI, SRCommand, util
import os, sys
import scsiutil
import xs_errors

class RAWVDI(VDI.VDI):
    def load(self, vdi_uuid):
        if not self.sr.attached:
            raise xs_errors.XenError('SRUnavailable')

        self.uuid = vdi_uuid
        self.location = vdi_uuid
        self.managed = True
        try:
            vdi_ref = self.sr.session.xenapi.VDI.get_by_uuid(vdi_uuid)
            self.managed = self.sr.session.xenapi.VDI.get_managed(vdi_ref)
            self.sm_config = self.sr.session.xenapi.VDI.get_sm_config(vdi_ref)
            self.path = self.sr.mpathmodule.path(self.sm_config['SCSIid'])
        except:
            pass
        if self.sr.cmd == "vdi_introduce":
            self.managed = True
        
    def _query(self, path, id):
        self.uuid = scsiutil.gen_uuid_from_string(scsiutil.getuniqueserial(path))
        self.location = self.uuid
        self.vendor = scsiutil.getmanufacturer(path)
        self.serial = scsiutil.getserial(path)
        self.LUNid = id
        self.size = scsiutil.getsize(path)
        self.SCSIid = scsiutil.getSCSIid(path)
        self.path = path
        sm_config = util.default(self, "sm_config", lambda: {})
        sm_config['LUNid'] = str(self.LUNid)
        sm_config['SCSIid'] = self.SCSIid
        self.sm_config = sm_config

    def introduce(self, sr_uuid, vdi_uuid):
        self.sm_config = self.sr.srcmd.params['vdi_sm_config']
        vdi_path = self.sr._getLUNbySMconfig(self.sm_config)
        self._query(vdi_path, self.sm_config['LUNid'])
        vdi_uuid = self.uuid
        self.sr.vdis[vdi_uuid] = self

        try:
            util._getVDI(self.sr, vdi_uuid)
            self.sr.vdis[vdi_uuid]._db_update()
            # For reasons I don't understand, VDI._db_update() doesn't set the
            # managed flag, so we do that ourselves here.
            vdi_ref = self.sr.session.xenapi.VDI.get_by_uuid(vdi_uuid)
            self.sr.session.xenapi.VDI.set_managed(vdi_ref, self.managed)
        except:
            self.sr.vdis[vdi_uuid]._db_introduce()
        return super(RAWVDI, self).get_params()

    def create(self, sr_uuid, vdi_uuid, size):
        VDIs = util._getVDIs(self.sr)
        self.sr._loadvdis()
        smallest = 0
        for vdi in VDIs:
            if not vdi['managed'] \
                   and long(vdi['virtual_size']) >= long(size) \
                   and self.sr.vdis.has_key(vdi['uuid']):
                if not smallest:
                    smallest = long(vdi['virtual_size'])
                    v = vdi
                elif long(vdi['virtual_size']) < smallest:
                    smallest = long(vdi['virtual_size'])
                    v = vdi
        if smallest > 0:
            self.managed = True
            self.sr.session.xenapi.VDI.set_managed(v['vdi_ref'], self.managed)
            return super(RAWVDI, self.sr.vdis[v['uuid']]).get_params()
        raise xs_errors.XenError('SRNoSpace')

    def delete(self, sr_uuid, vdi_uuid):
        try:
            vdi = util._getVDI(self.sr, vdi_uuid)
            if not vdi['managed']:
                return
            sm_config = vdi['sm_config']
            self.sr.session.xenapi.VDI.set_managed(vdi['vdi_ref'], False)
        except:
            pass
        
    def attach(self, sr_uuid, vdi_uuid):
        self.sr._loadvdis()
        if not self.sr.vdis.has_key(vdi_uuid):
            raise xs_errors.XenError('VDIUnavailable')
        if not util.pathexists(self.path):
            self.sr.refresh()
            if not util.wait_for_path(self.path, MAX_TIMEOUT):
                util.SMlog("Unable to detect LUN attached to host [%s]" % self.sr.path)
                raise xs_errors.XenError('VDIUnavailable')
        return super(RAWVDI, self).attach(sr_uuid, vdi_uuid)

    def detach(self, sr_uuid, vdi_uuid):
        self.sr._loadvdis()
        if not self.sr.vdis.has_key(vdi_uuid):
            raise xs_errors.XenError('VDIUnavailable')

    def _set_managed(self, vdi_uuid, managed):
        try:
            vdi = util._getVDI(self.sr, vdi_uuid)
            self.sr.session.xenapi.VDI.set_managed(vdi['vdi_ref'], managed)
        except:
            raise xs_errors.XenError('VDIUnavailable')

