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
# nfs.py: NFS related utility functions

import util, errno, os, xml.dom.minidom

# The algorithm for tcp and udp (at least in the linux kernel) for 
# NFS timeout on softmounts is as follows:
#
# UDP:
# As long as the request wasn't started more than timeo * (2 ^ retrans) 
# in the past, keep doubling the timeout.
#
# TCP:
# As long as the request wasn't started more than timeo * (1 + retrans) 
# in the past, keep increaing the timeout by timeo.
#
# The time when the retrans may retry has been made will be:
# For udp: timeo * (2 ^ retrans * 2 - 1)
# For tcp: timeo * n! where n is the smallest n for which n! > 1 + retrans
#
# thus for retrans=1, timeo can be the same for both tcp and udp, 
# because the first doubling (timeo*2) is the same as the first increment
# (timeo+timeo).

SOFTMOUNT_TIMEOUT  = int((40.0/3.0) * 10.0) # 1/10 s
SOFTMOUNT_RETRANS  = 0x7fffffff
RPCINFO_BIN = "/usr/sbin/rpcinfo"
SHOWMOUNT_BIN = "/usr/sbin/showmount"


class NfsException(Exception):
    def __init__(self, errstr):
        self.errstr = errstr


def check_server_tcp(server):
    """Make sure that NFS over TCP/IP V3 is supported on the server. 
    Returns True if everything is OK, False otherwise."""
    try:
        util.ioretry(lambda: util.pread([RPCINFO_BIN,"-t", 
                                         "%s" % server, "nfs","3"]), 
                     errlist=[errno.EPERM], maxretry=2, nofail=True)
    except util.CommandException, inst:
        raise NfsException("rpcinfo failed or timed out: return code %d" % 
                           inst.code)


def soft_mount(mountpoint, remoteserver, remotepath, transport):
    """Mount the remote NFS export at 'mountpoint'"""
    try:
        if not util.ioretry(lambda: util.isdir(mountpoint)):
            util.ioretry(lambda: util.makedirs(mountpoint))
    except util.CommandException, inst:
        raise NfsException("Failed to make directory: code is %d" % 
                            inst.code)

    options = "soft,timeo=%d,retrans=%d,%s" % (SOFTMOUNT_TIMEOUT,
                                               SOFTMOUNT_RETRANS,
                                               transport)

    # CA-27534: don't cache directory attributes.
    # so slaves follow snapshot name flips consistently.
    options += ',acdirmin=0,acdirmax=0'

    try:
        util.ioretry(lambda: 
                     util.pread(["mount.nfs", "%s:%s" 
                                 % (remoteserver, remotepath),  
                                 mountpoint, "-o", options]),
                                 errlist=[errno.EPIPE, errno.EIO], 
                     maxretry=2, nofail=True)
    except util.CommandException, inst:
        raise NfsException("mount failed with return code %d" % inst.code)


def unmount(mountpoint, rmmountpoint):
    """Unmount the mounted mountpoint"""
    try:
        util.pread(["umount", mountpoint])
    except util.CommandException, inst:
        raise NfsException("umount failed with return code %d" % inst.code)

    if rmmountpoint:
        try:
            os.rmdir(mountpoint)
        except OSError, inst:
            raise NfsException("rmdir failed with error '%s'" % inst.strerror)


def scan_exports(target):
    util.SMlog("scanning")
    cmd = [SHOWMOUNT_BIN, "--no-headers", "-e", target]
    dom = xml.dom.minidom.Document()
    element = dom.createElement("nfs-exports")
    dom.appendChild(element)
    for val in util.pread2(cmd).split('\n'):
        if not len(val):
            continue
        entry = dom.createElement('Export')
        element.appendChild(entry)
        
        subentry = dom.createElement("Target")
        entry.appendChild(subentry)
        textnode = dom.createTextNode(target)
        subentry.appendChild(textnode)
            
        (path, access) = val.split()
        subentry = dom.createElement("Path")
        entry.appendChild(subentry)
        textnode = dom.createTextNode(path)
        subentry.appendChild(textnode)

        subentry = dom.createElement("Accesslist")
        entry.appendChild(subentry)
        textnode = dom.createTextNode(access)
        subentry.appendChild(textnode)

    return dom

def scan_srlist(path):
    dom = xml.dom.minidom.Document()
    element = dom.createElement("SRlist")
    dom.appendChild(element)
    for val in filter(util.match_uuid, util.ioretry( \
            lambda: util.listdir(path))):
        fullpath = os.path.join(path, val)
        if not util.ioretry(lambda: util.isdir(fullpath)):
            continue
            
        entry = dom.createElement('SR')
        element.appendChild(entry)
        
        subentry = dom.createElement("UUID")
        entry.appendChild(subentry)
        textnode = dom.createTextNode(val)
        subentry.appendChild(textnode)

    return dom.toprettyxml()
