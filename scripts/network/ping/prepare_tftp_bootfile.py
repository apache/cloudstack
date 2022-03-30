#!/usr/bin/python
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


# Usage: prepare_tftp_bootfile.py tftp_dir mac cifs_server share directory image_to_restore cifs_username cifs_password
import os, sys
from sys import exit
from os import makedirs
from os.path import exists, join

restore_template = '''DEFAULT default
PROMPT 1
TIMEOUT 26
DISPLAY boot.msg
LABEL default
KERNEL kernel
APPEND vga=normal devfs=nomount pxe ramdisk_size=66000 load_ramdisk=1 init=/linuxrc prompt_ramdisk=0 initrd=initrd.gz root=/dev/ram0 rw noapic nolapic lba combined_mode=libata ide0=noprobe nomce pci=nomsi irqpoll quiet Server="%s" Share="%s" Directory="%s" Image_To_Restore="%s" After_Completion="Reboot" CIFS_Preferred="Y" Zsplit_Preferred="Y" AUTO="Y" User="%s" Passwd="%s" Extend_Parts_Whenever_Possible="N" Replace_BIOS="N" IP="%s" Netmask="%s" Gateway="%s"
'''

backup_template = '''DEFAULT default
PROMPT 1
TIMEOUT 26
DISPLAY boot.msg
LABEL default
KERNEL kernel
APPEND vga=normal devfs=nomount pxe ramdisk_size=66000 load_ramdisk=1 init=/linuxrc prompt_ramdisk=0 initrd=initrd.gz root=/dev/ram0 rw noapic nolapic lba combined_mode=libata ide0=noprobe nomce pci=nomsi irqpoll quiet Server="%s" Share="%s" Directory="%s" Image_To_Restore="Create_New_Image" New_Image_Name="%s" Already_Existing_Image="Replace" Store_MD5="N" Compression_Type="gzip" After_Completion="Reboot" Minimize_Before_Storing="N" Repart="N" CIFS_Preferred="Y" AUTO="Y" User="%s" Passwd="%s" Extend_Parts_Whenever_Possible="N" Replace_BIOS="N" IP="%s" Netmask="%s" Gateway="%s"
'''


cmd = ''
tftp_dir = ''
mac = ''
cifs_server = ''
share = ''
directory = ''
template_dir = ''
cifs_username = ''
cifs_password = ''
ip = ''
netmask = ''
gateway = ''

def prepare(is_restore):
    try:
        pxelinux = join(tftp_dir, "pxelinux.cfg")
        if exists(pxelinux) == False:
            makedirs(pxelinux)

        cfg_name = "01-" + mac.replace(':','-').lower()
        cfg_path = join(pxelinux, cfg_name)
        f = open(cfg_path, "w")
        if is_restore:
            fmt = restore_template
        else:
            fmt = backup_template
        stuff = fmt % (cifs_server, share, directory, template_dir, cifs_username, cifs_password, ip, netmask, gateway)
        f.write(stuff)
        f.close()
        return 0
    except Exception, e:
        print e
        return 1


if __name__ == "__main__":
    if len(sys.argv) < 12:
        print "Usage: prepare_tftp_bootfile.py tftp_dir mac cifs_server share directory image_to_restor cifs_username cifs_password ip netmask gateway"
        exit(1)

    (cmd, tftp_dir, mac, cifs_server, share, directory, template_dir, cifs_username, cifs_password, ip, netmask, gateway) = sys.argv[1:]

    if cmd == "restore":
        ret = prepare(True)
    elif cmd == "backup":
        ret = prepare(False)
    else:
        print "Unknown cmd: %s"%cmd
        ret = 1

    exit(ret)
