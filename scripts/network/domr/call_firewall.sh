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



 

# $Id: call_firewall.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.0.0/java/scripts/vm/hypervisor/xenserver/patch/call_firewall.sh $
# firewall.sh -- allow some ports / protocols to vm instances
usage() {
  printf "Usage for Firewall rule  : %s: <domR eth1 ip> -F " $(basename $0) >&2
  printf "Usage for other purposes : %s: <domR eth1 ip> (-A|-D) -i <domR eth1 ip>  -r <target-instance-ip> -P protocol (-p port_range | -t icmp_type_code)  -l <public ip address> -d <target port> [-f <firewall ip> -u <firewall user> -y <firewall password> -z <firewall enable password> ] \n" $(basename $0) >&2
}

#set -x

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
domRIp=$1
shift

# Check if DomR is up and running. If not, exit with error code 1.
check_gw "$domRIp"
if [ $? -gt 0 ]
then
  exit 1
fi
fflag=
while getopts ':F' OPTION
do
  case $OPTION in 
  F)    fflag=1
                ;;
  \?)  ;;
  esac
done

if [ -n "$fflag" ]
then
	ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp "/root/firewall_rule.sh $*"
else
	ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp "/root/firewall.sh $*"
fi
exit $?









