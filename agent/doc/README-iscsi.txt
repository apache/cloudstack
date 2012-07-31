Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

0. Contents
===========
../sbin/vnetd: userspace daemon that runs the vnet
../module/2.6.18/vnet_module.ko: kernel module (alternative to vnetd)
../vnetd.sh: init script for vnet
../vn: helper script to create vnets

../id_rsa: the private key used to ssh to the routing domain 

createvm.sh: clones a vm image from a given template 
mountvm.sh: script to mount a remote (nfs) image directory
runvm.sh: script to run a vm
rundomr.sh: script to run a routing domain (domR) for a given vnet
listvmdisk.sh: lists disks belonging to a vm
createtmplt.sh: installs a template
listvmdisksize.sh: lists actuala and total usage per disk
../ipassoc.sh: associate / de-associate a public ip with an instance
../firewall.sh: add or remove firewall rules
stopvm.sh: stop the vm and remove the entry from xend
../delvm.sh: delete the vm image from zfs
../listclones.sh: list all filesystems that are clones under a parent fs

1. Install
==========
On the hosts that run the customer vms as well as the domR
a) Copy vn to /usr/sbin  on dom0
b) Copy module/2.6.18/vnet_module.ko to /lib/modules/`uname -r`/kernel
c) Run repos/vmdev/xen/xen-3.3.0/tools/vnet/examples/vnet-insert
Ensure that all iptables rules are flushed from domO before starting any domains
(use iptables -F)
d) Ensure that the ISCSI initiator is installed (yum install iscsi*)


2. Creating /deleting a vm image on Solaris ZFS
================
The template image consists of a filesystem to hold kernel and ramdisk (linux) 
or the pygrub file (linux) or nothing (windows). Contained within the template 
filesystem (but not visible using 'ls') is the root volume.

Use the createvm script to clone a template snapshot. For example:
./createvm.sh -t tank/volumes/demo/template/public/os/centos52-x86_64 -d /tank/volumes/demo/template/public/datadisk/ext3-8g -i /tank/demo/vm/chiradeep/i0007 -u /tank/demo/vm/chiradeep
  -t: the template fs snapshot
  -i: the target clone fs
  -u: the user's fs under which the clone will be created. If the user fs does not exist, it will be created.
  -d: the disk fs to be cloned under the image dir specified by -i
Once this is created, use the listvmdisk.sh to list the disks:
listvmdisk.sh -i tank/demo/vm/chiradeep/i0007 -r  (for the root disk)
listvmdisk.sh -i tank/demo/vm/chiradeep/i0007 -w  (for the data disk)
listvmdisk.sh -i tank/demo/vm/chiradeep/i0007 -d <n> (for the data disks)

This outputs the local target name (zfs name) and the ISCSI target name
separated by a comma:
tank/demo/vm/chiradeep/i0007/datadisk1-ext3-8g,iqn.1986-03.com.sun:02:0b6c18c9-7a13-e7c9-ce78-91af20023bb3

The local target name can be used to list total (-t)and actual(-a) disk usage:
./listvmdisksize.sh -d tank/demo/vm/chiradeep/i0007/datadisk1-ext3-8g -t
8589934592

Use the delvm.sh script to delete an instance. For example:
./delvm.sh -u tank/demo/vm/chiradeep -i tank/demo/vm/chiradeep/i0007
   -i: the instance fs to delete
   -u: the user fs to delete
Either -i or -u or both can be supplied.

Use the listclones.sh script to list all clones under a parent fs:
./listclones.sh -p tank/demo/vm

3. Mounting an image
==================
The image directory resides on the NFS server, you can mount it with the
mountvm.sh script. For example:
./mountvm.sh -m -h 192.168.1.248 -t iqn.1986-03.com.sun:02:bf65dcfd-42b5-6e0e-e08e-99ae311b39ba -l /images/chiradeep/i0005 -n centos52 -r tank/demo/vm/chiradeep/i0005 -1 iqn.1986-03.com.sun:02:6d505eee-bf64-6729-e362-bab6c148bbc8
  -h : the nfs/iscsi server host
  -l : the local directory 
  -r : the remote directory
  -n : the vm name (the same name used in runvm or rundomr)
  -r : the iscsi target name for the root volume (see listvmdisk above)
  -w : the iscsi target name for the swap volume (see listvmdisk above)
  -1 : the iscsi target name for the datadisk volume (see listvmdisk above)
  [-m | -u] : mount or unmount

4. Routing Domain (domR)
=======================
The routing domain for a customer needs to be started before any other VM in that vnet can start. To start a routing domain, for example:
./rundomr.sh  -v 0008 -m 128 -i 192.168.1.33   -g 65.37.141.1 -a aa:00:00:05:00:33  -l "domR-vnet0008" -A 06:01:02:03:04:05 -p 02:01:02:03:04:05 -n 255.255.255.0 -I 65.37.141.33 -N 255.255.255.128 -b eth1 -d "dns1=192.168.1.254 dns2=207.69.188.186 domain=vmops.org" /images/chiradeep/router
 -v : the is the 16-bit vnet-id specified in 4 hex characters
 -m : the ram size for the domain in megabytes (128 is usually sufficient)
 -a : the mac address of the eth0 of the domR
 -A : the mac address of the eth1 of the domR 
 -p : the mac address of the eth2 of the domR
 -i : the eth1 ip address in the datacenter LAN (e.g., 192.168.1.33)
 -n : the netmask of eth1
 -I : the eth2 ip address in the public LAN (e.g., 65.37.141.33)
 -N : the netmask of eth2 (e.g., 65.37.141.128)
 -b : the Xen bridge (typ.eth1) that eth2 has to be enslaved to (public LAN)
 -g : the default gateway in the public subnet (e.g., 65.37.141.1)
 -l : the vm name for the doMR
 -d : nameserver information in the format shown in the example
Note: -d option requires template tank/demo/template/public/t100001@12_16_2008
or later

5. Starting a vm
================
The VM files are assumed to exist in a single image directory with the following conventions:
  a) The kernel file begins with vmlinuz (e.g. vmlinuz-2.6.18.8-xen) (Linux)
  b) The root volume begins with vmi-root (e.g.,vmi-root-centos52-x86_64-pv)
  c) The data partition begins with datadisk1 (e.g., datadisk1-ext3-8g)
  d) The swap partition contains  "swap" (e.g., fedora-swap) (Linux only)

If booting Linux using pygrub, only the root and data files are needed. An
empty file called 'pygrub' must be placed in the image directory

To run the vm, see the following example
/runvm.sh  -v 0005 -i 10.1.1.56  -m 256  -g 192.168.1.33 -a 02:00:00:05:00:56  -l "centos5-2" -c 11 -n 2 -u 66 /images/chiradeep/i0007

 -v : the is the 16-bit vnet-id specified in 4 hex characters
 -i : this is the host ip address in the 10.x.y.z subnet (cannot be 10.1.1.1)
 -m : the ram size for the domain in megabytes
 -g : the eth1 ip address of the routing domain
 -a : the mac address of the eth0 of the vm
 -l : the vm name. This is also the hostname, ensure it is is a legal hostname
 -c : the VNC console id
 -w : the VNC password. If not specified, defaults to 'password'
 -n : the number of VCPUs (eq to number of cores) to allocate (default all)
 -u : the percentage of one VCPU to allocate (integer) (default no cap)
 <image dir>: the absolute path of the directory holding the VM files/volumes

The vncviewer can connect to the eth0 ip of dom0 and the specified vnc console number (e.g., 192.168.1.125:11).
The 'n' and 'u' parameters depends on the physical CPU of the host and the
number of compute units requested. For example, lets say 1 compute unit = 1Ghz
and the physical CPU is a quad-core CPU running at 3.0 Ghz. To request 2 cores
running 1 compute unit each, n = 2 and u= 2 x (1/3)*100

6. Associate a public Ip with a domR (source NAT)
===========================================
The example below shows how to associate the public ip 65.37.141.33 the
routing domain. This has to be run on the dom0 of the host hosting the
routing domain.

ipassoc.sh -A -r domR-vnet0007 -i 192.168.1.32 -l 65.37.141.33 -a 06:01:02:03:06:05
  -A|-D: create or delete an association
  -r: the name (label) of the routing domain
  -i: the eth1 ip of the routing domain
  -a: the mac address of eth2 in the routing domain (not required for -D)
  -l: the public ip to be used for source NAT

7. Firewall rules
=================
Each instance  can have firewall rules associated to allow
some ports through. By default, when created, an instance has all ports and
protocols blocked. In the following example, the 10.1.1.155 instance gets ssh
traffic and icmp pings opened up:
firewall.sh -A -i 192.168.1.133 -P tcp -p 22 -r 10.1.1.155 -l 65.37.141.33 -d
22
firewall.sh -A -i 192.168.1.133 -P icmp -t echo-request -r 10.1.1.155 -l
65.37.141.33
  -A|-D: add or delete a rule
  -i: the eth1 ip of the routing domain
  -r: the local eth0 ip of the target instance 
  -l: the public ip
  -P: the protocol (tcp, udp, icmp)
  -t: (for icmp) the icmp type
  -p: (for tcp and udp) the port (port range in the form of a:b)
  -d: (for tcp and udp) the target port (port range in the form of a:b)

8. Stopping and restarting a VM
===============================
You can use 'xm reboot vmname' to reboot the VM.
To stop it (and delete it from Xend's internal database), use
stopvm.sh -l <vmname>
This will not remove the vnet however. 
The stopvm script will NOT attempt to umount the root and data disks as well
To explicitly unmount the root disk data disks from the NFS server, run 
this on dom0:
mountvm.sh -u -l /images/u00000002/i0003
  -u: (no arguments)
  -l: the local directory on the compute server

9. Vnet cleanup
===============
When you kill the vnet task, all vnif* interfaces will disappear but the
bridges will linger.
You can use vnetcleanup.sh to clean up the vnet
vnetcleanup.sh -a will clean up all vnets
vnetcleanup.sh -v 0005 will only cleanup vnet0005.

10. VM Image Cleanup
===================
On ZFS, run delvm.sh, for example:
 ./delvm.sh -u tank/demo/vm/u00000003 -i tank/demo/vm/u00000003/i0001
  -u: the user fs (optional)
  -i: the instance fs (optional)

11. Template installation
=========================
Template installation involves copying the image file of the rootdisk to a
iscsi volume. For example:
createtmplt.sh -t rpool/volumes/demo/template/public/os/ubuntu8 -f
/rpool/volumes/demo/template/public/download/ubuntu8/ubuntu8.0.img -n ubuntu8 -s 12G
  -t: the filesystem (created if non-existent) where the volume will be mounted
  -f: the absolute path to the file containing the root disk image
  -n: the name of the template. The create volume will be vmi-root-$name
  -s: the size in gigabytes for the volume
  -h: if a hvm image

12. Mapping iscsi target names to VM names
==========================================
The mapiscsi.sh script maps iscsi names of targets logged in to by the routing
host/compute host:
[root@r-1-1-1 iscsi]# ./mapiscsi.sh 
iqn.1986-03.com.sun:02:ef4942ec-9f7e-4d71-e994-bb670867053e r-870-TEST-0186-root
iqn.1986-03.com.sun:02:599f5cc5-2f90-c1c3-9c5e-fef252345e64 r-870-TEST-0186-swap
iqn.1986-03.com.sun:02:0e893b01-fa32-682e-976d-d15781cf1a44 r-872-TEST-0187-root
iqn.1986-03.com.sun:02:21225d22-479c-4a35-dca0-ad56e60aa6f4 r-872-TEST-0187-swap
iqn.1986-03.com.sun:02:55b1a6d4-d202-e565-ffe1-ee63e4a48210 r-875-TEST-0188-root
iqn.1986-03.com.sun:02:4fac467c-7b63-6ffb-c207-aa35ccecfcd5 r-875-TEST-0188-swap

If no VM name can be found, the second field is blank

13. OpenVZ patch workarounds
============================
The openvz patch eliminates kernel oops related to bride reconfiguration.
However this requires an extra tickle to the bridge to make it actually send
packets to member port. The member port needs to be taken down (ifconfig down)
and up (ifconfig up).
This is done in 
a) rundomr.sh -- on creation of vnet bridge, the vnif is taken down and up
b) runvm.sh -- ditto
c) /etc/xen/qemu-ifup -- the interface (tapX.0) is taken down and then up
after the interface is added to the bridge. 
