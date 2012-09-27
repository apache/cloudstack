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
# $Id: setup_iscsi.sh 9879 2010-06-24 02:41:46Z anthony $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xenserver/setup_iscsi.sh $

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

