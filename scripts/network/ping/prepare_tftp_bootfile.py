#!/usr/bin/python



  #
  # Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
  # 
  # This software is licensed under the GNU General Public License v3 or later.
  # 
  # It is free software: you can redistribute it and/or modify
  # it under the terms of the GNU General Public License as published by
  # the Free Software Foundation, either version 3 of the License, or any later version.
  # This program is distributed in the hope that it will be useful,
  # but WITHOUT ANY WARRANTY; without even the implied warranty of
  # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  # GNU General Public License for more details.
  # 
  # You should have received a copy of the GNU General Public License
  # along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #
 

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
