#!/bin/bash
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

fileSvr="http://nfs1.lab.vmops.com/templates/devcloud/"
install_xen() {
    aptitude update
    echo "install xen"
    aptitude -y install linux-headers-3.2.0-23-generic-pae
    aptitude -y install xen-hypervisor-4.1 xcp-xapi 
    echo "configure xen"

    sed -i -e 's/xend_start$/#xend_start/' -e 's/xend_stop$/#xend_stop/' /etc/init.d/xend
    update-rc.d xendomains disable

    sed -i 's/GRUB_DEFAULT=.\+/GRUB_DEFAULT="Xen 4.1-amd64"/' /etc/default/grub
    update-grub

    mkdir /usr/share/qemu
    ln -s /usr/share/qemu-linaro/keymaps /usr/share/qemu/keymaps

cat > /etc/network/interfaces << EOF
# The loopback network interface
auto lo
iface lo inet loopback

# The primary network interface
auto xenbr0
iface xenbr0 inet dhcp
    gateway 10.0.2.2
    bridge_ports eth0


auto eth0
iface eth0 inet dhcp
EOF

    echo TOOLSTACK=xapi > /etc/default/xen
    echo bridge > /etc/xcp/network.conf

    echo "reboot"
    reboot
}

postsetup() {
    #check xen dom0 is working
    xe host-list > /dev/null
    if [ $? -gt 0 ]; then
        print "xen dom0 is not running, make sure dom0 is installed"
        exit 1
    fi

    echo "configure NFS server"
    aptitude -y install nfs-server
    if [ ! -d /opt/storage/secondary ];then
        mkdir -p /opt/storage/secondary
        mkdir -p /opt/storage/secondary/template/tmpl/1/1
        mkdir -p /opt/storage/secondary/template/tmpl/1/5

        echo "/opt/storage/secondary *(rw,no_subtree_check,no_root_squash,fsid=0)" > /etc/exports
        wget $fileSvr/vmtemplates/1/dc68eb4c-228c-4a78-84fa-b80ae178fbfd.vhd  -P /opt/storage/secondary/template/tmpl/1/1/
        wget $fileSvr/vmtemplates/1/template.properties  -P /opt/storage/secondary/template/tmpl/1/1/
        wget $fileSvr/vmtemplates/5/ce5b212e-215a-3461-94fb-814a635b2215.vhd  -P /opt/storage/secondary/template/tmpl/1/5/
        wget $fileSvr/vmtemplates/5/template.properties  -P /opt/storage/secondary/template/tmpl/1/5/
        /etc/init.d/nfs-kernel-server restart
    fi

    echo "configure local storage"
    if [ ! -d /opt/storage/primary ]; then
        mkdir -p /opt/storage/primary
        hostuuid=`xe host-list |grep uuid|awk '{print $5}'`
        xe sr-create host-uuid=$hostuuid name-label=local-storage shared=false type=file device-config:location=/opt/storage/primary
    fi

    echo "generate ssh key"
    ssh-keygen -A -q 

    echo "configure xcp"
    wget $fileSvr/echo -P /usr/lib/xcp/plugins/
    chmod -R 777 /usr/lib/xcp

    sed -i 's/VNCTERM_LISTEN=.\+/VNCTERM_LISTEN="-v 0.0.0.0:1"/' /usr/lib/xcp/lib/vncterm-wrapper
    
    echo "install cloudstack "

    if [ ! -d /opt/cloudstack ];then
        aptitude -y install git unzip openjdk-6-jdk mysql-server ant
        mkdir /opt/cloudstack
        cd /opt/cloudstack
        git clone https://git-wip-us.apache.org/repos/asf/incubator-cloudstack.git
        mkdir incubator-cloudstack/target
        mkdir incubator-cloudstack/dist
        wget http://archive.apache.org/dist/tomcat/tomcat-6/v6.0.32/bin/apache-tomcat-6.0.32.zip -P /opt/cloudstack/
        unzip apache-tomcat-6.0.32.zip
        echo "exportCATALINA_HOME=/opt/cloudstack/apache-tomcat-6.0.32" >> /root/.bashrc
        cd ~
    fi

    echo "devCloud is ready to use"
}
usage() {
    print "$0 -p: presetup enviroment, e.g. install xen, configure xcp etc" 
    print "$0 -P: postsetup, install cloudstack, prepare template etc"
}

while getopts "pP" OPTION
do
    case $OPTION in
        p)
        install_xen
        exit 0
        ;;
        P)
        postsetup
        exit 0
        ;;
        ?)
        usage
        exit
        ;;
    esac
done
