#!/bin/bash



  #
  # Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
 

# $Id: dhcp_entry.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/dhcp_entry.sh $
# dhcp_entry.sh -- add dhcp entry on domr
#
# @VERSION@

usage() {
  printf "Usage: %s: -r <domr-ip> -m <vm mac> -v <vm ip> -n <vm name>\n" $(basename $0) >&2
  exit 2
}

cert="/root/.ssh/id_rsa.cloud"

add_dhcp_entry() {
  local domr=$1
  local mac=$2
  local ip=$3
  local vm=$4
  local dfltrt=$5
  local staticrt=$6
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domr "/root/edithosts.sh $mac $ip $vm $dfltrt $staticrt" >/dev/null
  return $?
}

domrIp=
vmMac=
vmIp=
vmName=
staticrt=
dfltrt=

while getopts 'r:m:v:n:d:s:' OPTION
do
  case $OPTION in
  r)	domrIp="$OPTARG"
		;;
  v)	vmIp="$OPTARG"
		;;
  m)	vmMac="$OPTARG"
		;;
  n)	vmName="$OPTARG"
		;;
  s)	staticrt="$OPTARG"
		;;
  d)	dfltrt="$OPTARG"
		;;
  ?)    usage
		exit 1
		;;
  esac
done

add_dhcp_entry $domrIp $vmMac $vmIp $vmName $dfltrt $staticrt

exit $?
