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


# $Id: dhcp_entry.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/dhcp_entry.sh $
# dhcp_entry.sh -- add dhcp entry on domr
# @VERSION@

usage() {
  printf "Usage: %s: -r <domr-ip> -m <vm mac> -v <vm ip> -n <vm name> -s <static route> -d <default router> -N <dns> -6 <vm IPv6> -u <duid> [-z]\n" $(basename $0) >&2
  exit 2
}

cert="/root/.ssh/id_rsa.cloud"

domrIp=
vmMac=
vmIp=
vmName=
staticrt=
dfltrt=
dns=
ipv6=
duid=

opts=

while getopts 'r:m:v:n:d:s:N:6:u:z' OPTION
do
  case $OPTION in
  r)  domrIp="$OPTARG"
      ;;
  v)  vmIp="$OPTARG"
      opts="$opts -4 $vmIp"
      ;;
  m)  vmMac="$OPTARG"
      opts="$opts -m $vmMac"
      ;;
  n)  vmName="$OPTARG"
      opts="$opts -h $vmName"
      ;;
  s)  staticrt="$OPTARG"
      opts="$opts -s $staticrt"
      ;;
  d)  dfltrt="$OPTARG"
      opts="$opts -d $dfltrt"
      ;;
  N)  dns="$OPTARG"
      opts="$opts -n $dns"
      ;;
  6)  ipv6="$OPTARG"
      opts="$opts -6 $ipv6"
      ;;
  u)  duid="$OPTARG"
      opts="$opts -u $duid"
      ;;
  z)  opts="$opts -N"
      ;;
  ?)  usage
      exit 1
      ;;
  esac
done

ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domrIp "/root/edithosts.sh $opts " >/dev/null

exit $?
