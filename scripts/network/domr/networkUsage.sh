#!/usr/bin/env bash
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



 

# $Id: networkUsage.sh 9879 2010-06-24 02:41:46Z anthony $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xenserver/networkUsage.sh $
# networkUsage.sh -- create iptable rules to gather network stats
usage() {
  printf "Usage: %s -[c|g|r] -i <domR eth1 ip> [-[a|d] <public interface>]\n" $(basename $0)  >&2
}

# check if gateway domain is up and running
check_gw() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -gt 0 ]
  then
    sleep 1
    ping -c 1 -n -q $1 > /dev/null
  fi
  return $?;
}

cert="/root/.ssh/id_rsa.cloud"

while getopts 'cgri:a:d:' OPTION
do
  case $OPTION in
  i)	iflag=1
		domRIp="$OPTARG"
		;;
  esac
done

# check if gateway domain is up and running
if ! check_gw "$domRIp"
then
   printf "Unable to ping the routing domain, exiting\n" >&2
   exit 3
fi

ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp "/root/netusage.sh $*"
exit $?
