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
sbin/vnetd: userspace daemon that runs the vnet
module/2.6.18/vnet_module.ko: kernel module (alternative to vnetd)
vnetd.sh: init script for vnet
vn: helper script to create vnets

id_rsa: the private key used to ssh to the routing domain 

createvm.sh: clones a vm image from a given template 
mountvm.sh: script to mount a remote (nfs) image directory
runvm.sh: script to run a vm
rundomr.sh: script to run a routing domain (domR) for a given vnet
ipassoc.sh: associate / de-associate a public ip with an instance
firewall.sh: add or remove firewall rules
stopvm.sh: stop the vm and remove the entry from xend
delvm.sh: delete the vm image from zfs
loadbalancer.sh: configure the loadbalancer
listclones.sh: list all filesystems that are clones under a parent fs

1. Install
==========
On the hosts that run the customer vms as well as the domR
a) Copy vn to /usr/sbin  on dom0
Either (vnetd):
  1) Copy sbin/vnetd to /usr/sbin on dom0
  2) Copy vnetd.sh to /etc/init.d/vnetd on dom0
  3) run chkconfig vnetd on
OR
  1) Copy module/2.6.18/vnet_module.ko to /lib/modules/`uname -r`/kernel
  2) Run repos/vmdev/xen/xen-3.3.0/tools/vnet/examples/vnet-insert
Ensure that all iptables rules are flushed from domO before starting any domains
(use iptables -F)


2. Creating /deleting a vm image on Solaris ZFS
================
Use the createvm script to clone a template snapshot. For example:
createvm.sh -t tank/template/public/t100001@12_3_2008 -i tank/demo/vm/u00000002/i0001 -u tank/demo/vm/u00000002 -d /tank/demo/template/public/datadisk/ext3-8g
  -t: the template fs snapshot
  -i: the target clone fs
  -u: the user's fs under which the clone will be created. If the user fs does not exist, it will be created.
  -d: the disk fs to be cloned under the image dir specified by -i
Once this is created, use the listvmdisk.sh to list the disks:
listvmdisk.sh -i tank/demo/vm/u00000002/i0001 -r  (for the root disk)
listvmdisk.sh -i tank/demo/vm/u00000002/i0001 -d <n> (for the data disks)

Use the delvm.sh script to delete an instance. For example:
./delvm.sh -u tank/demo/vm/u00000003 -i tank/demo/vm/u00000003/i0001
   -i: the instance fs to delete
   -u: the user fs to delete
Either -i or -u or both can be supplied.

Use the listclones.sh script to list all clones under a parent fs:
./listclones.sh -p tank/demo/vm

3. Mounting an image
==================
If the image directory resides on the NFS server, you can mount it with the
mountvm.sh script. For example:
./mountvm.sh -h sol10-1.lab.vmops.com -l /images/u00000002/i0001 -r
/tank/vm/demo/u00000002/i0001 -m
  -h : the nfs server host
  -l : the local directory 
  -r : the remote directory
  [-m | -u] : mount or unmount


4. Routing Domain (domR)
=======================
The routing domain for a customer needs to be started before any other VM in that vnet can start. To start a routing domain, for example:
./rundomr.sh  -v 0008 -m 128 -i 192.168.1.33   -g 65.37.141.1 -a aa:00:00:05:00:33  -l "domR-vnet0008" -A 06:01:02:03:04:05 -p 02:01:02:03:04:05 -n 255.255.255.0 -I 65.37.141.33 -N 255.255.255.128 -b eth1 -d "dns1=192.168.1.254 dns2=207.69.188.186 domain=vmops.org" /images/templates/t100001
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
  b) The root filesystem begins with vmi-root (e.g., vmi-root-centos.5-2.64.img)
  c) The data partition begins with vmi-data1 (e.g., vmi-data1.img)
  d) The swap partition ends with ".swap" (e.g., centos64.swap) (Linux only)

If booting Linux using pygrub, only the root and data files are needed. An
empty file called 'pygrub' must be placed in the image directory

To run the vm, see the following example
/runvm.sh  -v 0005 -i 10.1.1.122  -m 256  -g 192.168.1.108 -a 02:00:00:05:00:22  -l "centos.5-2.64" -c 11 -n 2 -u 66 /images/u00000002/i0003

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
 <image dir>: the absolute path of the directory holding the VM files

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

7.5 Loadbalancer rules
=====================
Loadbalancing is provided by HAProxy running within the routing domain. Because the rules are large and consist of many components, it is expected that the entire HAProxy configuration file is provided to the script. This is copied over to the routing domain and the haproxy process is restarted. 
loadbalancer.sh -A -i 192.168.1.35 -l 65.37.141.30 -d 80 -f /tmp/haproxy.cfg 
New haproxy instance successfully loaded, stopping previous one.
  -A|-D: add or delete a rule
  -i: the eth1 ip of the routing domain
  -l: the public ip
  -d: the target port 
  -f: the haproxy configuration file

8. Stopping and restarting a VM
===============================
You can use 'xm reboot vmname' to reboot the VM.
To stop it (and delete it from Xend's internal database), use
stopvm.sh -l <vmname>
This will not remove the vnet however. 
The stopvm script will attempt to umount the root and data disks as well
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

10. TODO
=======
5. Automatic install instead of manual steps of (1)
