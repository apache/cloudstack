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
# Miscellaneous scsi utility functions
#

import util, SR
import os
import re
import xs_errors
import base64
import time
import errno
import glob

PREFIX_LEN = 4
SUFFIX_LEN = 12
SECTOR_SHIFT = 9

def gen_hash(st, len):
    hs = 0
    for i in st:
        hs = ord(i) + (hs << 6) + (hs << 16) - hs
    return str(hs)[0:len]

def gen_uuid_from_serial(iqn, serial):
    if len(serial) < SUFFIX_LEN:
        raise util.CommandException(1)
    prefix = gen_hash(iqn, PREFIX_LEN)
    suffix = gen_hash(serial, SUFFIX_LEN)
    str = prefix.encode("hex") + suffix.encode("hex")
    return str[0:8]+'-'+str[8:12]+'-'+str[12:16]+'-'+str[16:20]+'-'+str[20:32]

def gen_serial_from_uuid(iqn, uuid):
    str = uuid.replace('-','')
    prefix = gen_hash(iqn, PREFIX_LEN)
    if str[0:(PREFIX_LEN * 2)].decode("hex") != prefix:
        raise util.CommandException(1)
    return str[(PREFIX_LEN * 2):].decode("hex")

def getsize(path):
    dev = getdev(path)
    sysfs = os.path.join('/sys/block',dev,'size')
    size = 0
    if os.path.exists(sysfs):
        try:
            f=open(sysfs, 'r')
            size = (long(f.readline()) << SECTOR_SHIFT)
            f.close()
        except:
            pass
    return size

def getuniqueserial(path):
    dev = getdev(path)
    output = gen_rdmfile()
    try:
        cmd = ["md5sum"]
        txt = util.pread3(cmd, getSCSIid(path))
        return txt.split(' ')[0]
    except:
        return ''

def gen_uuid_from_string(str):
    if len(str) < (PREFIX_LEN + SUFFIX_LEN):
        raise util.CommandException(1)
    return str[0:8]+'-'+str[8:12]+'-'+str[12:16]+'-'+str[16:20]+'-'+str[20:32]

def SCSIid_sanitise(str):
    text = re.sub("^\s+","",str)
    return re.sub("\s+","_",text)

def getSCSIid(path):
    dev = rawdev(path)
    cmd = ["scsi_id", "-g", "-s", "/block/%s" % dev]
    return SCSIid_sanitise(util.pread2(cmd)[:-1])

def compareSCSIid_2_6_18(SCSIid, path):
    serial = getserial(path)
    len_serial = len(serial)
    if (len_serial == 0 ) or (len_serial > (len(SCSIid) - 1)):
        return False
    list_SCSIid = list(SCSIid)
    list_serial = list_SCSIid[1:(len_serial + 1)]
    serial_2_6_18 = ''.join(list_serial)
    if (serial == serial_2_6_18):
        return True
    else:
        return False

def getserial(path):
    dev = os.path.join('/dev',getdev(path))
    try:
        cmd = ["sginfo", "-s", dev]
        text = re.sub("\s+","",util.pread2(cmd))
    except:
        raise xs_errors.XenError('EIO', \
              opterr='An error occured querying device serial number [%s]' \
                           % dev)
    try:
        return text.split("'")[1]
    except:
        return ''

def getmanufacturer(path):
    cmd = ["sginfo", "-M", path]
    try:
        for line in filter(match_vendor, util.pread2(cmd).split('\n')):
            return line.replace(' ','').split(':')[-1]
    except:
        return ''

def cacheSCSIidentifiers():
    SCSI = {}
    SYS_PATH = "/dev/disk/by-scsibus/*"
    for node in glob.glob(SYS_PATH):
        dev = os.path.realpath(node)
        HBTL = os.path.basename(node).split("-")[-1].split(":")
        line = "NONE %s %s %s %s 0 %s" % \
               (HBTL[0],HBTL[1],HBTL[2],HBTL[3],dev)
        ids = line.split()
        SCSI[ids[6]] = ids
    return SCSI

def scsi_dev_ctrl(ids, cmd):
    f = -1
    for i in range(0,10):
        try:
            str = "scsi %s-single-device %s %s %s %s" % \
                  (cmd, ids[1],ids[2],ids[3],ids[4])
            util.SMlog(str)
            f=open('/proc/scsi/scsi', 'w')
            print >>f, str
            f.close()
            return
        except IOError, e:
            util.SMlog("SCSI_DEV_CTRL: Failure, %s [%d]" % (e.strerror,e.errno))
            if f >= 0:
                f.close()
            if e.errno == errno.ENXIO:
                util.SMlog("Device has disappeared already")
                return
            f = -1
            time.sleep(6)
            continue
    raise xs_errors.XenError('EIO', \
            opterr='An error occured during the scsi operation')

def getdev(path):
    realpath = os.path.realpath(path)
    if match_dm(realpath):
        newpath = realpath.replace("/dev/mapper/","/dev/disk/by-id/scsi-")
    else:
        newpath = path
    return os.path.realpath(newpath).split('/')[-1]

def rawdev(dev):
    return re.sub("[0-9]*$","",getdev(dev))

def getSessionID(path):
    for line in filter(match_session, util.listdir(path)):
        return line.split('-')[-1]

def match_session(s):
    regex = re.compile("^SESSIONID-")
    return regex.search(s, 0)

def match_vendor(s):
    regex = re.compile("^Vendor:")
    return regex.search(s, 0)

def match_dm(s):
    regex = re.compile("mapper/")
    return regex.search(s, 0)    
    
def match_sd(s):
    regex = re.compile("/dev/sd")
    return regex.search(s, 0)

def _isSCSIdev(dev):
    if match_dm(dev):
        path = dev.replace("/dev/mapper/","/dev/disk/by-id/scsi-")
    else:
        path = dev
    return match_sd(os.path.realpath(path))

def gen_rdmfile():
    return "/tmp/%s" % util.gen_uuid()

def add_serial_record(session, sr_ref, devstring):
    try:
        conf = session.xenapi.SR.get_sm_config(sr_ref)
        conf['devserial'] = devstring
        session.xenapi.SR.set_sm_config(sr_ref, conf)
    except:
        pass

def get_serial_record(session, sr_ref):
    try:
        conf = session.xenapi.SR.get_sm_config(sr_ref)
        return conf['devserial']
    except:
        return ""

def devlist_to_serialstring(devlist):
    serial = ''
    for dev in devlist:
        try:
            devserial = "scsi-%s" % getSCSIid(dev)
            if not len(devserial) > 0:
                continue
            if len(serial):
                serial += ','
            serial += devserial
        except:
            pass
    
    return serial

def gen_synthetic_page_data(uuid):
    # For generating synthetic page data for non-raw LUNs
    # we set the vendor ID to XENSRC
    # Note that the Page 80 serial number must be limited
    # to 16 characters
    page80 = ""
    page80 += "\x00\x80"
    page80 += "\x00\x12"
    page80 += uuid[0:16]
    page80 += "  "
    
    page83 = ""
    page83 += "\x00\x83"
    page83 += "\x00\x31"
    page83 += "\x02\x01\x00\x2d"
    page83 += "XENSRC  "
    page83 += uuid
    page83 += " "
    return ["",base64.b64encode(page80),base64.b64encode(page83)]
    
def gen_raw_page_data(path):
    default = ""
    page80 = ""
    page83 = ""
    try:
        cmd = ["sg_inq", "-r", path]
        text = util.pread2(cmd)
        default = base64.b64encode(text)
            
        cmd = ["sg_inq", "--page=0x80", "-r", path]
        text = util.pread2(cmd)
        page80 = base64.b64encode(text)
            
        cmd = ["sg_inq", "--page=0x83", "-r", path]
        text = util.pread2(cmd)
        page83 = base64.b64encode(text)
    except:
        pass
    return [default,page80,page83]

def update_XS_SCSIdata(session, vdi_ref, vdi_uuid, data):
        try:
            session.xenapi.VDI.remove_from_xenstore_data(vdi_ref, "vdi-uuid")
        except:
            pass
        
        try:
            session.xenapi.VDI.remove_from_xenstore_data(vdi_ref, "scsi/0x12/default")
        except:
            pass
        
        try:
            session.xenapi.VDI.remove_from_xenstore_data(vdi_ref, "scsi/0x12/0x80")
        except:
            pass

        try:
            session.xenapi.VDI.remove_from_xenstore_data(vdi_ref, "scsi/0x12/0x83")
        except:
            pass

        try:
            session.xenapi.VDI.add_to_xenstore_data(vdi_ref, "vdi-uuid", vdi_uuid)
            if len(data[0]):
                session.xenapi.VDI.add_to_xenstore_data(vdi_ref, "scsi/0x12/default", data[0])

            if len(data[1]):
                session.xenapi.VDI.add_to_xenstore_data(vdi_ref, "scsi/0x12/0x80", data[1])

            if len(data[2]):
                session.xenapi.VDI.add_to_xenstore_data(vdi_ref, "scsi/0x12/0x83", data[2])
        except:
            pass

def rescan(ids, scanstring='- - -'):
    for id in ids:
        refresh_HostID(id, scanstring)

def _genArrayIdentifier(dev):
    try:
        cmd = ["sg_inq", "--page=0xc8", "-r", dev]
        id = util.pread2(cmd)
        return id.encode("hex")[180:212]
    except:
        return ""


def _genHostList(procname):
    # loop through and check all adapters
    ids = []
    try:
        for dir in util.listdir('/sys/class/scsi_host'):
            filename = os.path.join('/sys/class/scsi_host',dir,'proc_name')
            if os.path.exists(filename):
                f = open(filename, 'r')
                if f.readline().find(procname) != -1:
                    ids.append(dir.replace("host",""))
                f.close()
    except:
        pass
    return ids

def _genReverseSCSIidmap(SCSIid, pathname="scsibus"):
    util.SMlog("map_by_scsibus: sid=%s" % SCSIid)

    devices = []
    for link in glob.glob('/dev/disk/by-id/scsi-%s' % SCSIid):
        devices.append(os.path.realpath(link))
    return devices

def _genReverseSCSidtoLUNidmap(SCSIid):
    devices = []
    for link in glob.glob('/dev/disk/by-scsibus/%s-*' % SCSIid):
        devices.append(link.split('-')[-1])
    return devices

def _dosgscan():
    regex=re.compile("([^:]*):\s+scsi([0-9]+)\s+channel=([0-9]+)\s+id=([0-9]+)\s+lun=([0-9]+)")
    scan=util.pread2(["/usr/bin/sg_scan"]).split('\n')
    sgs=[]
    for line in scan:
        m=regex.match(line)
        if m:
            device=m.group(1)
            host=m.group(2)
            channel=m.group(3)
            sid=m.group(4)
            lun=m.group(5)
            sgs.append([device,host,channel,sid,lun])
    return sgs

def refresh_HostID(HostID, scanstring):
    LUNs = glob.glob('/sys/class/scsi_disk/%s*' % HostID)
    li = []
    for l in LUNs:
        chan = re.sub(":[0-9]*$",'',os.path.basename(l))
        if chan not in li:
            li.append(chan)

    fullrescan = True
    if len(li) and scanstring == "- - -":
        fullrescan = False
        for c in li:
            if not refresh_scsi_channel(c):
                fullrescan = True

    if fullrescan:
        util.SMlog("Rescanning HostID %s with %s" % (HostID, scanstring))
        path = '/sys/class/scsi_host/host%s/scan' % HostID
        if os.path.exists(path):
            try:
                f=open(path, 'w')
                f.write('%s\n' % scanstring)
                f.close()
            except:
                pass
        # Host Bus scan issued, now try to detect channels
        if util.wait_for_path("/sys/class/scsi_disk/%s*" % HostID, 5):
            # At least one LUN is mapped
            LUNs = glob.glob('/sys/class/scsi_disk/%s*' % HostID)
            li = []
            for l in LUNs:
                chan = re.sub(":[0-9]*$",'',os.path.basename(l))
                if chan not in li:
                    li.append(chan)
            for c in li:
                refresh_scsi_channel(c)
    

def refresh_scsi_channel(channel):
    DEV_WAIT = 5
    util.SMlog("Refreshing channel %s" % channel)
    util.wait_for_path('/dev/disk/by-scsibus/*-%s*' % channel, DEV_WAIT)
    LUNs = glob.glob('/dev/disk/by-scsibus/*-%s*' % channel)
    try:
        rootdevs = util.dom0_disks()
    except:
        util.SMlog("Failed to query root disk, failing operation")
        return False
    
    # a) Find a LUN to issue a Query LUNs command
    li = []
    Query = False
    for lun in LUNs:
        try:
            hbtl = lun.split('-')[-1]
            h = hbtl.split(':')
            l=util.pread2(["/usr/bin/sg_luns","-q",lun]).split('\n')
            li = []
            for i in l:
                if len(i):
                    li.append(int(i[0:4], 16))
            util.SMlog("sg_luns query returned %s" % li)
            Query = True
            break
        except:
            pass
    if not Query:
        util.SMlog("Failed to detect or query LUN on Channel %s" % channel)
        return False

    # b) Remove stale LUNs
    current = glob.glob('/dev/disk/by-scsibus/*-%s:%s:%s*' % (h[0],h[1],h[2]))
    for cur in current:
        lunID = int(cur.split(':')[-1])
        newhbtl = ['',h[0],h[1],h[2],str(lunID)]
        if os.path.realpath(cur) in rootdevs:
            # Don't touch the rootdev
            if lunID in li: li.remove(lunID)
            continue
        
        # Check if LUN is stale, and remove it
        if not lunID in li:
            util.SMlog("Stale LUN detected. Removing HBTL: %s" % newhbtl)
            scsi_dev_ctrl(newhbtl,"remove")
            util.wait_for_nopath(cur, DEV_WAIT)
            continue
        else:
            li.remove(lunID)

        # Query SCSIid, check it matches, if not, re-probe
        cur_SCSIid = os.path.basename(cur).split("-%s:%s:%s" % (h[0],h[1],h[2]))[0]
        real_SCSIid = getSCSIid(cur)
        if cur_SCSIid != real_SCSIid:
            util.SMlog("HBTL %s does not match, re-probing" % newhbtl)
            scsi_dev_ctrl(newhbtl,"remove")
            util.wait_for_nopath(cur, DEV_WAIT)
            scsi_dev_ctrl(newhbtl,"add")
            util.wait_for_path('/dev/disk/by-scsibus/%s-%s' % (real_SCSIid,hbtl), DEV_WAIT)
            pass

    # c) Probe for any LUNs that are not present in the system
    for l in li:
        newhbtl = ['',h[0],h[1],h[2],str(l)]
        util.SMlog("Probing new HBTL: %s" % newhbtl)
        scsi_dev_ctrl(newhbtl,"add")
        util.wait_for_path('/dev/disk/by-scsibus/*-%s' % hbtl, DEV_WAIT)

    return True
