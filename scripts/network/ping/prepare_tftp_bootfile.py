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

template = '''DEFAULT default
PROMPT 1
TIMEOUT 26
DISPLAY boot.msg
LABEL default
KERNEL kernel
APPEND vga=normal devfs=nomount pxe ramdisk_size=66000 load_ramdisk=1 init=/linuxrc prompt_ramdisk=0 initrd=initrd.gz root=/dev/ram0 rw noapic nolapic lba combined_mode=libata ide0=noprobe nomce pci=nomsi irqpoll quiet Server="%s" Share="%s" Directory="%s" Image_To_Restore="%s" After_Completion="Reboot" CIFS_Preferred="Y" Zsplit_Preferred="Y" AUTO="Y" User="%s" Passwd="%s" Extend_Parts_Whenever_Possible="N" Replace_BIOS="N" IP="%s" Netmask="%s" Gateway="%s"
'''

tftp_dir = ''
mac = ''
cifs_server = ''
share = ''
directory = ''
image_to_restore = ''
cifs_username = ''
cifs_password = ''
ip = ''
netmask = ''
gateway = ''

def prepare_boot_file():
    try:
        pxelinux = join(tftp_dir, "pxelinux.cfg")
        if exists(pxelinux) == False:
            makedirs(pxelinux)

        cfg_name = "01-" + mac.replace(':','-').lower()
        cfg_path = join(pxelinux, cfg_name)
        f = open(cfg_path, "w")
        stuff = template % (cifs_server, share, directory, image_to_restore, cifs_username, cifs_password, ip, netmask, gateway)
        f.write(stuff)
        f.close()
        return 0
    except IOError, e:
        print e
        return 1

if __name__ == "__main__":
    if len(sys.argv) < 12:
        print "Usage: prepare_tftp_bootfile.py tftp_dir mac cifs_server share directory image_to_restor cifs_username cifs_password ip netmask gateway"
        exit(1)

    tftp_dir = sys.argv[1]
    mac = sys.argv[2]
    cifs_server = sys.argv[3]
    share = sys.argv[4]
    directory = sys.argv[5]
    image_to_restore = sys.argv[6]
    cifs_username = sys.argv[7]
    cifs_password = sys.argv[8]
    ip = sys.argv[9]
    netmask = sys.argv[10]
    gateway = sys.argv[11]
    

    ret = prepare_boot_file()
    exit(ret)
