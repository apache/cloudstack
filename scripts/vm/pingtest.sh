#!/usr/bin/env bash
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


# $Id: pingtest.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/pingtest.sh $
# pingtest.sh -- ping 
usage() {
  printf "Usage:\n %s -i <domR eth1 ip>  -p <private-ip-address> \n" $(basename $0) >&2
  printf " %s -h <computing-agent-host-ip>  \n" $(basename $0) >&2
  printf " %s -g \n" $(basename $0) >&2
}

check_gw() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -gt 0 ]
  then
    sleep 1
    ping -c 1 -n -q $1 > /dev/null
  fi
  return $?;
}

# ping the vm's private IP from the domR
ping_vm() {
  local routerIp=$1
  local vmIp=$2
  ssh -o StrictHostKeyChecking=no -p 3922 -i /root/.ssh/id_rsa.cloud root@$routerIp "ping -c 1 -n -q $vmIp"

  if [ $? -gt 0 ]
  then
     arping_vm $routerIp $vmIp
     return $?
  fi

  return $?;
}

arping_vm() {
  local routerIp=$1
  local vmIp=$2
  ssh -o StrictHostKeyChecking=no -p 3922 -i /root/.ssh/id_rsa.cloud root@$routerIp "arping -c 1 -q $vmIp"

  # if return code of ping is > 0, the ping failed, return a result
  if [ $? -gt 0 ]
  then
     return 1
  fi

  return $?;
}

# ping the default route
ping_default_route() {
  defaultRoute=`route |grep default|awk '{print $2}'`
  ping -c 1 -n -q $defaultRoute > /dev/null
  return $?
}

# ping the computing host
ping_host() {
  ping -c 1 -n -q $1 > /dev/null

  if [ $? -gt 0  ]
  then
     return 1
  fi

  return $?;
}

iflag=
pflag=
hflag=
gflag=

while getopts 'i:p:h:g' OPTION
do
  case $OPTION in
  i)	iflag=1
		domRIp="$OPTARG"
		;;
  p)	pflag=1
		privateIp="$OPTARG"
		;;
  h)    hflag=1
        hostIp="$OPTARG"
        ;;
  g)    gflag=1
        ;;
  ?)	usage
		exit 2
		;;
  esac
done

# make sure both domRIp and vm private ip are set
if [ "$iflag$hflag$gflag" != "1" ]
then
 usage
 exit 2
fi

if [ "$iflag" == "1" ]
then
  if [ "$pflag" != "1" ]
  then
    usage
    exit 3
  fi
fi

if [ "$iflag" == "1" ]
then
  # check if gateway domain is up and running
  if ! check_gw "$domRIp"
  then
    printf "Unable to ping the routing domain, exiting\n" >&2
    exit 4
  fi

  if ! ping_vm $domRIp $privateIp
  then
    printf "Unable to ping the vm, exiting\n" >&2
    exit 5
  fi
fi

if [ "$hflag" == "1" ]
then
  if ! ping_host "$hostIp"
  then
    # first ping default route to make sure we can get out successfully before returning error
    if ! ping_default_route
    then
      printf "Unable to ping default route, exiting\n" >&2
      exit 7
    fi

    printf "Unable to ping computing host, exiting\n" >&2
    exit 6
  fi
fi

if [ "$gflag" == "1" ]
then
  if ! ping_default_route
  then
    printf "Unable to ping default route\n" >&2
    exit 8
  fi
fi

exit 0
