#!/usr/bin/env bash



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
 

# $Id: ipassoc.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/ipassoc.sh $
# ipassoc.sh -- associate/disassociate a public ip with an instance
#
#
# @VERSION@
usage() {
  printf "Usage:\n %s -A    -l <public-ip-address>   -c <dev> [-f] \n" $(basename $0) >&2
  printf " %s -D  -l <public-ip-address>  -c <dev> [-f] \n" $(basename $0) >&2
}


add_nat_entry() {
  local pubIp=$1
  logger -t cloud "$(basename $0):Adding nat entry for ip $pubIp on interface $ethDev"
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  sudo ip link set $ethDev up
  sudo ip addr add dev $ethDev $pubIp
  sudo iptables -D FORWARD -i $ethDev -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT
  sudo iptables -D FORWARD -i eth0 -o $ethDev  -j ACCEPT
  sudo iptables -t nat -D POSTROUTING   -j SNAT -o $ethDev --to-source $ipNoMask ;
  sudo iptables -A FORWARD -i $ethDev -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT
  sudo iptables -A FORWARD -i eth0 -o $ethDev  -j ACCEPT
  sudo iptables -t nat -I POSTROUTING   -j SNAT -o $ethDev --to-source $ipNoMask ;
  sudo arping -c 3 -I $ethDev -A -U -s $ipNoMask $ipNoMask;
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     logger -t cloud "$(basename $0):Failed adding nat entry for ip $pubIp on interface $ethDev"
     return 1
  fi
  logger -t cloud "$(basename $0):Added nat entry for ip $pubIp on interface $ethDev"

  return 0
   
}

del_nat_entry() {
  local pubIp=$1
  logger -t cloud "$(basename $0):Deleting nat entry for ip $pubIp on interface $ethDev"
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  local mask=$(echo $1 | awk -F'/' '{print $2}')
  [ "$mask" == "" ] && mask="32"
  sudo iptables -D FORWARD -i $ethDev -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT
  sudo iptables -D FORWARD -i eth0 -o $ethDev  -j ACCEPT
  sudo iptables -t nat -D POSTROUTING   -j SNAT -o $ethDev --to-source $ipNoMask;
  sudo ip addr del dev $ethDev "$ipNoMask/$mask"
 
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     return 1
  fi

  return $?
}


add_an_ip () {
  local pubIp=$1
  logger -t cloud "$(basename $0):Adding ip $pubIp on interface $ethDev"
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')

  sudo ip link set $ethDev up
  sudo ip addr add dev $ethDev $pubIp ;
  sudo arping -c 3 -I $ethDev -A -U -s $ipNoMask $ipNoMask;
  return $?
   
}

remove_an_ip () {
  local pubIp=$1
  logger -t cloud "$(basename $0):Removing ip $pubIp on interface $ethDev"
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  local mask=$(echo $1 | awk -F'/' '{print $2}')
  local existingIpMask=$(sudo ip addr show dev $ethDev | grep inet | awk '{print $2}'  | grep -w $ipNoMask)
  [ "$existingIpMask" == "" ] && return 0
  local existingMask=$(echo $existingIpMask | awk -F'/' '{print $2}')
  if [ "$existingMask" == "32" ] 
  then
    sudo ip addr del dev $ethDev $existingIpMask
    result=$?
  fi
  if [ "$existingMask" != "32" ] 
  then
        replaceIpMask=`sudo ip addr show dev $ethDev | grep inet | grep -v $existingIpMask | awk '{print $2}' | sort -t/ -k2 -n|tail -1`
        sudo ip addr del dev $ethDev $existingIpMask;
        if [ -n "$replaceIpMask" ]; then
          sudo ip addr del dev $ethDev $replaceIpMask;
          replaceIp=`echo $replaceIpMask | awk -F/ '{print $1}'`;
          sudo ip addr add dev $ethDev $replaceIp/$existingMask;
          sudo iptables -t nat -D POSTROUTING   -j SNAT -o $ethDev --to-source $ipNoMask ;
          sudo iptables -t nat -A POSTROUTING   -j SNAT -o $ethDev --to-source $replaceIp ;
        fi
    result=$?
  fi
  if [ $result -gt 0  -a $result -ne 2 ]
  then
     return 1
  fi
  return 0
}

#set -x

lflag=
fflag=
cflag=
op=""

while getopts 'fADa:l:c:' OPTION
do
  case $OPTION in
  A)	Aflag=1
		op="-A"
		;;
  D)	Dflag=1
		op="-D"
		;;
  f)	fflag=1
		;;
  l)	lflag=1
		publicIp="$OPTARG"
		;;
  c)	cflag=1
  		ethDev="$OPTARG"
  		;;
  ?)	usage
		exit 2
		;;
  esac
done


#Either the A flag or the D flag but not both
if [ "$Aflag$Dflag" != "1" ]
then
 usage
 exit 2
fi

if [ "$lflag$cflag" != "11" ] 
then
   usage
   exit 2
fi


if [ "$fflag" == "1" ] && [ "$Aflag" == "1" ]
then
  add_nat_entry  $publicIp 
  exit $?
fi

if [ "$Aflag" == "1" ]
then  
  add_an_ip  $publicIp 
  exit $?
fi

if [ "$fflag" == "1" ] && [ "$Dflag" == "1" ]
then
  del_nat_entry  $publicIp 
  exit $?
fi

if [ "$Dflag" == "1" ]
then
  remove_an_ip  $publicIp 
  exit $?
fi

exit 0
