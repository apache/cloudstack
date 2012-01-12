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
# $Id: setup_iscsi.sh 9879 2010-06-24 02:41:46Z anthony $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xenserver/setup_iscsi.sh $
# Version @VERSION@

#set -x
 
usage() {
  printf "Usage: %s [uuid of host script is running on]\n" $(basename $0) >&2
}


if [ -z $1 ]; then
  usage
  exit 2
fi

if [ -f /etc/iscsi/initiatorname.iscsi ]; then
  printf "=======> DONE <======\n"
  exit 0
fi

name=`iscsi-iname`
echo "InitiatorName="$name > /etc/iscsi/initiatorname.iscsi
xe host-param-set uuid=$1 other-config:iscsi_iqn=`cat /etc/iscsi/initiatorname.iscsi | cut -d= -f 2`
xe host-param-list uuid=$1 | grep other-config | grep "multipathing: true" >/dev/null
if [ $? -eq 0 ]; then
  ln -f -s /etc/iscsi/iscsid-mpath.conf /etc/iscsi/iscsid.conf
else
  ln -f -s /etc/iscsi/iscsid-default.conf /etc/iscsi/iscsid.conf
fi
service open-iscsi restart
lines=`ps -ef | grep iscsid | grep -v grep | wc -l`
if [ $lines -eq 0 ]
then
  exit 1
fi
printf "=======> DONE <======\n"

