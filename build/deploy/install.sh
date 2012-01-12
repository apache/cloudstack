#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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



 


# install.sh -- installs MySQL, Java, Tomcat, and the VMOps server 

#set -x
set -e

EX_NOHOSTNAME=15
EX_SELINUX=16

function usage() {
  printf "Usage: %s [path to server-setup.xml]\n" $(basename $0) >&2
  exit 64
}

function checkhostname() {
  if hostname | grep -qF . ; then true ; else
    echo "You need to have a fully-qualified host name for the setup to work." > /dev/stderr
    echo "Please use your operating system's network setup tools to set one." > /dev/stderr
    exit $EX_NOHOSTNAME
  fi
}

function checkselinux() {
#### before checking arguments, make sure SELINUX is "permissible" in /etc/selinux/config
  if /usr/sbin/getenforce | grep -qi enforcing ; then borked=1 ; fi
  if grep -i SELINUX=enforcing /etc/selinux/config ; then borked=1 ; fi
  if [ "$borked" == "1" ] ; then
    echo "SELINUX is set to enforcing, please set it to permissive in /etc/selinux/config" > /dev/stderr
    echo "then reboot the machine, after which you can run the install script again." > /dev/stderr
    exit $EX_SELINUX
  fi
}

checkhostname
checkselinux

if [ "$1" == "" ]; then
  usage
fi

if [ ! -f $1 ]; then
  echo "Error: Unable to find $1"  > /dev/stderr
  exit 2
fi

#### check that all files exist
if [ ! -f apache-tomcat-6.0.18.tar.gz ]; then
  printf "Error: Unable to find apache-tomcat-6.0.18.tar.gz\n" > /dev/stderr
  exit 3
fi

if [ ! -f MySQL-client-5.1.30-0.glibc23.x86_64.rpm ]; then
  printf "Error: Unable to find MySQL-client-5.1.30-0.glibc23.x86_64.rpm\n" > /dev/stderr
  exit 4
fi

if [ ! -f MySQL-server-5.1.30-0.glibc23.x86_64.rpm ]; then
  printf "Error: Unable to find MySQL-server-5.1.30-0.glibc23.x86_64.rpm\n" > /dev/stderr
  exit 5
fi

if [ ! -f jdk-6u13-linux-amd64.rpm.bin ]; then
  printf "Error: Unable to find jdk-6u13-linux-amd64.rpm.bin\n" > /dev/stderr
  exit 6
fi

#if [ ! -f osol.tar.bz2 ]; then
#  printf "Error: Unable to find osol.tar.bz2\n"
#  exit 7
#fi

if [ ! -f apache-tomcat-6.0.18.tar.gz ]; then
  printf "Error: Unable to find apache-tomcat-6.0.18.tar.gz\n" > /dev/stderr
  exit 8
fi

if [ ! -f vmops-*.zip ]; then
  printf "Error: Unable to find vmops install file\n" > /dev/stderr
  exit 9
fi

if [ ! -f catalina ] ; then
  printf "Error: Unable to find catalina initscript\n" > /dev/stderr
  exit 10
fi

if [ ! -f usageserver ] ; then
  printf "Error: Unable to find usageserver initscript\n" > /dev/stderr
  exit 11
fi

###### install Apache
# if [ ! -d /usr/local/tomcat ] ; then
  echo "installing Apache..."
  mkdir -p /usr/local/tomcat
  tar xfz apache-tomcat-6.0.18.tar.gz -C /usr/local/tomcat
  ln -s /usr/local/tomcat/apache-tomcat-6.0.18 /usr/local/tomcat/current
# fi
# if [ ! -f /etc/profile.d/catalinahome.sh ] ; then
#   echo "export CATALINA_HOME=/usr/local/tomcat/current" >> /etc/profile.d/catalinahome.sh
# fi
source /etc/profile.d/catalinahome.sh
# if [ ! -f /etc/init.d/catalina ] ; then
  cp -f catalina /etc/init.d
  /sbin/chkconfig catalina on
# fi

####### set up usage server as a service
if [ ! -f /etc/init.d/usageserver ] ; then
  cp -f usageserver /etc/init.d
  /sbin/chkconfig usageserver on
fi

##### set up mysql
if rpm -q MySQL-server MySQL-client > /dev/null 2>&1 ; then true ; else
  echo "installing MySQL..."
  yum localinstall --nogpgcheck -y MySQL-*.rpm
fi

#### install JDK
echo "installing JDK..."
sh jdk-6u13-linux-amd64.rpm.bin
rm -rf /usr/bin/java
ln -s /usr/java/default/bin/java /usr/bin/java

#### setting up OSOL image
#mkdir -p $CATALINA_HOME/webapps/images
#echo "copying Open Solaris image, this may take a few moments..."
#cp osol.tar.bz2 $CATALINA_HOME/webapps/images

#### deploying database
unzip -o vmops-*.zip
cd vmops-*
sh deploy-server.sh -d "$CATALINA_HOME"
cd db
sh deploy-db.sh "../../$1" templates.sql

exit 0
