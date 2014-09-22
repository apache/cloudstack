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

 

# $Id: ipassoc.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/ipassoc.sh $
# ipassoc.sh -- associate/disassociate a public ip with an instance
# @VERSION@

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage:\n %s -A    -l <public-ip-address>   -c <dev> [-f] \n" $(basename $0) >&2
  printf " %s -D  -l <public-ip-address>  -c <dev> [-f] \n" $(basename $0) >&2
}

add_fw_chain_for_ip () {
  local pubIp=$(echo $1 | awk -F'/' '{print $1}')
  if sudo iptables -t mangle -N FIREWALL_$pubIp &> /dev/null
  then
    logger -t cloud "$(basename $0): created firewall chain for $pubIp"
    sudo iptables -t mangle -A FIREWALL_$pubIp -j DROP> /dev/null
    #ensure outgoing connections are maintained (first rule in chain)
    sudo iptables -t mangle -I FIREWALL_$pubIp -m state --state RELATED,ESTABLISHED -j ACCEPT> /dev/null
    #ensure that this table is after VPN chain
    sudo iptables -t mangle -I PREROUTING 2 -d $pubIp -j FIREWALL_$pubIp
    return $?
  fi
  logger -t cloud "$(basename $0): firewall chain for $pubIp already exists"
}

add_vpn_chain_for_ip () {
  local pubIp=$(echo $1 | awk -F'/' '{print $1}')
  if sudo iptables -t mangle -N VPN_$pubIp &> /dev/null
  then
    logger -t cloud "$(basename $0): created VPN chain for $pubIp"
    #ensure outgoing connections are maintained (first rule in chain)
    sudo iptables -t mangle -I VPN_$pubIp -m state --state RELATED,ESTABLISHED -j ACCEPT
    sudo iptables -t mangle -A VPN_$pubIp -j RETURN
    #ensure that this table is the first
    sudo iptables -t mangle -I PREROUTING 1 -d $pubIp -j VPN_$pubIp
    return $?
  fi
  logger -t cloud "$(basename $0): VPN chain for $pubIp already exists"
}

del_fw_chain_for_ip () {
  local pubIp=$(echo $1 | awk -F'/' '{print $1}')
  if ! sudo iptables -t mangle -N FIREWALL_$pubIp &> /dev/null
  then
    logger -t cloud "$(basename $0): destroying firewall chain for $pubIp"
    sudo iptables -t mangle -D PREROUTING  -d $pubIp -j FIREWALL_$pubIp
    sudo iptables -t mangle -F FIREWALL_$pubIp
    sudo iptables -t mangle -X FIREWALL_$pubIp 
    return $?
  fi
  # firewall chain got created as a result of testing for the chain, cleanup
  sudo iptables -t mangle -F FIREWALL_$pubIp
  sudo iptables -t mangle -X FIREWALL_$pubIp
  logger -t cloud "$(basename $0): firewall chain did not exist for $pubIp, cleaned up"
}

del_vpn_chain_for_ip () {
  local pubIp=$(echo $1 | awk -F'/' '{print $1}')
  if ! sudo iptables -t mangle -N VPN_$pubIp &> /dev/null
  then
    logger -t cloud "$(basename $0): destroying vpn chain for $pubIp"
    sudo iptables -t mangle -D PREROUTING  -d $pubIp -j VPN_$pubIp
    sudo iptables -t mangle -F VPN_$pubIp
    sudo iptables -t mangle -X VPN_$pubIp 
    return $?
  fi
  # vpn chain got created as a result of testing for the chain, cleanup
  sudo iptables -t mangle -F VPN_$pubIp
  sudo iptables -t mangle -X VPN_$pubIp
  logger -t cloud "$(basename $0): vpn chain did not exist for $pubIp, cleaned up"
}

remove_routing() {
  local pubIp=$1
  logger -t cloud "$(basename $0):Remove routing $pubIp on interface $ethDev"
  local ipNoMask=$(echo $pubIp | awk -F'/' '{print $1}')
  local mask=$(echo $pubIp | awk -F'/' '{print $2}')
  local tableNo=$(echo $ethDev | awk -F'eth' '{print $2}')

  local tableName="Table_$ethDev"
  local remainip=`ip addr show $ethDev | grep "inet "`
  if [ "$remainip" == "" ]
  then
# rules and routes will be deleted for the last ip of the interface.
     sudo ip rule delete fwmark $tableNo table $tableName
     sudo ip rule delete table $tableName
     sudo ip route flush  table $tableName 
     sudo ip route flush cache
     logger -t cloud "$(basename $0):Remove routing $pubIp - routes and rules deleted"
  fi
}

# copy eth0,eth1 and the current public interface
copy_routes_from_main() {
  local tableName=$1

#get the network masks from the main table
  local eth0Mask=$(ip route list scope link dev eth0 | awk '{print $1}')
  local eth1Mask=$(ip route list scope link dev eth1 | awk '{print $1}')
  local ethMask=$(getcidr $ethDev)

# eth0,eth1 and other know routes will be skipped, so as main routing table will decide the route. This will be useful if the interface is down and up.  
  sudo ip route add throw $eth0Mask table $tableName proto static 
  sudo ip route add throw $eth1Mask table $tableName proto static 
  sudo ip route add throw $ethMask  table $tableName proto static 
  return 0;
}

ip_addr_add() {
  local dev="$1"
  local ip="$2"
  sudo ip addr add dev $dev $ip brd + > /dev/null

}

add_routing() {
  local pubIp=$1
  logger -t cloud "$(basename $0):Add routing $pubIp on interface $ethDev"
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  local mask=$(echo $1 | awk -F'/' '{print $2}')

  local tableName="Table_$ethDev"
  local tablePresent=$(grep $tableName /etc/iproute2/rt_tables)
  local tableNo=$(echo $ethDev | awk -F'eth' '{print $2}')
  if [ "$tablePresent" == "" ]
  then
     if [ "$tableNo" == "" ]
     then
       return 0;
     fi
     sudo echo "$tableNo $tableName" >> /etc/iproute2/rt_tables
  fi

  copy_routes_from_main $tableName
# NOTE: this  entry will be deleted if the interface is down without knowing to Management server, in that case all the outside traffic will be send through main routing table or it will be the first public NIC.
  sudo ip route add default via $defaultGwIP table $tableName proto static
  sudo ip route flush cache

  local ethMask=$(getcidr $ethDev)
  local rulePresent=$(ip rule show | grep $ethMask)
  if [ "$rulePresent" == "" ]
  then
# rules will be added while adding the first ip of the interface 
     sudo ip rule add from $ethMask table $tableName
     sudo ip rule add fwmark $tableNo table $tableName
     logger -t cloud "$(basename $0):Add routing $pubIp rules added"
  fi
  return 0;
}
add_snat() {
  local pubIp=$1
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  if [ "$sflag" == "0" ]
  then
    logger -t cloud "$(basename $0):Remove SourceNAT $pubIp on interface $ethDev if it is present"
    sudo iptables -t nat -D POSTROUTING   -j SNAT -o $ethDev --to-source $ipNoMask ;
    return 0;
  fi

  logger -t cloud "$(basename $0):Added SourceNAT $pubIp on interface $ethDev"
  sudo iptables -t nat -D POSTROUTING   -j SNAT -o $ethDev --to-source $ipNoMask ;
  sudo iptables -t nat -A POSTROUTING   -j SNAT -o $ethDev --to-source $ipNoMask ;
  return $?
}
remove_snat() {
  if [ "$sflag" == "0" ]
  then
    return 0;
  fi

  local pubIp=$1
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  logger -t cloud "$(basename $0):Removing SourceNAT $pubIp on interface $ethDev"
  sudo iptables -t nat -D POSTROUTING   -j SNAT -o $ethDev --to-source $ipNoMask;
  return $?
}
add_first_ip() {
  local pubIp=$1
  logger -t cloud "$(basename $0):Adding first ip $pubIp on interface $ethDev"
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  local mask=$(echo $1 | awk -F'/' '{print $2}')
  sudo ip link show $ethDev | grep "state DOWN" > /dev/null
  local old_state=$?
  
  ip_addr_add $ethDev $pubIp

  sudo iptables -D FORWARD -i $ethDev -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT
  sudo iptables -D FORWARD -i eth0 -o $ethDev  -j FW_OUTBOUND
  sudo iptables -A FORWARD -i $ethDev -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT
  sudo iptables -A FORWARD -i eth0 -o $ethDev  -j FW_OUTBOUND

  add_snat $1
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     logger -t cloud "$(basename $0):Failed adding source nat entry for ip $pubIp on interface $ethDev"
     return 1
  fi

  logger -t cloud "$(basename $0):Added first ip $pubIp on interface $ethDev"
  if [ $if_keep_state -ne 1 -o $old_state -ne 0 ]
  then
      sudo ip link set $ethDev up
      sudo arping -c 1 -I $ethDev -A -U -s $ipNoMask $ipNoMask;
      sudo arping -c 1 -I $ethDev -A -U -s $ipNoMask $ipNoMask;
  fi

  # add interface gateway ip info into file, used on redundant router fail over for adding routes
  sed -i /"$ethDev "/d $IFACEGWIPFILE
  echo "$ethDev $defaultGwIP" >> $IFACEGWIPFILE

  add_routing $1

  return 0
}

remove_first_ip() {
  local pubIp=$1
  logger -t cloud "$(basename $0):Removing first ip $pubIp on interface $ethDev"
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  local mask=$(echo $1 | awk -F'/' '{print $2}')

  local existingIpMask=$(sudo ip addr show dev $ethDev | grep inet | awk '{print $2}'  | grep -w $ipNoMask)
  [ "$existingIpMask" == "" ] && return 0

  [ "$mask" == "" ] && mask="32"

  sudo iptables -D FORWARD -i $ethDev -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT
  sudo iptables -D FORWARD -i eth0 -o $ethDev  -j FW_OUTBOUND
  remove_snat $1
  
  sudo ip addr del dev $ethDev "$ipNoMask/$mask"
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     remove_routing $1
     sudo ip link set $ethDev down
     return 1
  fi

  for ipMask in $existingIpMask
  do
    if [ "$ipMask" == "$pubIp" ]
    then
            continue
    fi
    sudo ip addr add dev $ethDev $ipMask brd +
  done

  sed -i /"$ethDev "/d $IFACEGWIPFILE
  remove_routing $1
  sudo ip link set $ethDev down
  return $?
}


add_an_ip () {
  local pubIp=$1
  logger -t cloud "$(basename $0):Adding ip $pubIp on interface $ethDev"
  local ipNoMask=$(echo $1 | awk -F'/' '{print $1}')
  sudo ip link show $ethDev | grep "state DOWN" > /dev/null
  local old_state=$?

  ip_addr_add $ethDev $pubIp
  add_snat $1
  if [ $if_keep_state -ne 1 -o $old_state -ne 0 ]
  then
      sudo ip link set $ethDev up
      sudo arping -c 1 -I $ethDev -A -U -s $ipNoMask $ipNoMask;
      sudo arping -c 1 -I $ethDev -A -U -s $ipNoMask $ipNoMask;
  fi
  add_routing $1
  return $?
   
}

remove_an_ip () {
  local pubIp=$1
  logger -t cloud "$(basename $0):Removing ip $pubIp on interface $ethDev"
  remove_snat $1
  local existingIpMask=$(sudo ip addr show dev $ethDev | grep "inet " | awk '{print $2}')

  sudo ip addr del dev $ethDev $pubIp

  for ipMask in $existingIpMask
  do
    if [ "$ipMask" == "$pubIp" ]
    then
      continue
    fi
    sudo ip addr add dev $ethDev $ipMask brd +
  done

  remove_routing $1
  return 0
}

enable_rpsrfs() {
    #enable rps and rfs for this new interface
    if [  -f /etc/rpsrfsenable ]
    then
        enable=$(cat /etc/rpsrfsenable)
        if [ $enable -eq 1 ]
        then
          proc=$(cat /proc/cpuinfo | grep "processor" | wc -l)
          if [ $proc -le 1 ]
          then
              return $status;
          fi

          num=1
          num=$(($num<<$proc))
          num=$(($num-1));
          echo $num;
          hex=$(printf "%x\n" $num)
          echo $hex;
          #enable rps
          echo $hex > /sys/class/net/$ethDev/queues/rx-0/rps_cpus

          #enable rfs
          echo 256 > /sys/class/net/$ethDev/queues/rx-0/rps_flow_cnt

         fi
     fi
}

#set -x
sflag=0
lflag=
fflag=
cflag=
nflag=
op=""

is_master=0
is_redundant=0
if_keep_state=0
IFACEGWIPFILE='/var/cache/cloud/ifaceGwIp'
grep "redundant_router=1" /var/cache/cloud/cmdline > /dev/null
if [ $? -eq 0 ]
then
    is_redundant=1
    sudo /opt/cloud/bin/checkrouter.sh --no-lock|grep "Status: MASTER" > /dev/null 2>&1
    if [ $? -eq 0 ]
    then
        is_master=1
    fi
fi
if [ $is_redundant -eq 1 -a $is_master -ne 1 ]
then
    if_keep_state=1
fi

while getopts 'sfADna:l:c:g:' OPTION
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
  s)	sflag=1
		;;
  l)	lflag=1
		publicIp="$OPTARG"
		;;
  c)	cflag=1
  		ethDev="$OPTARG"
  		;;
  g)	gflag=1
  		defaultGwIP="$OPTARG"
  		;;
  n)   nflag=1
        ;;
  ?)	usage
                unlock_exit 2 $lock $locked
		;;
  esac
done


if [ "$Aflag$Dflag" != "1" ]
then
    usage
    unlock_exit 2 $lock $locked
fi

if [ "$lflag$cflag" != "11" ] 
then
    usage
    unlock_exit 2 $lock $locked
fi


if [ "$Aflag" == "1" ] && [ "$nflag" == "1" ]
then
    #enable rps, rfs for the new interface
    enable_rpsrfs

 fi

if [ "$fflag" == "1" ] && [ "$Aflag" == "1" ]
then
  add_first_ip  $publicIp  &&
  add_vpn_chain_for_ip $publicIp &&
  add_fw_chain_for_ip $publicIp 
  unlock_exit $? $lock $locked
fi

if [ "$Aflag" == "1" ]
then  
  add_an_ip  $publicIp  &&
  add_fw_chain_for_ip $publicIp 
  unlock_exit $? $lock $locked
fi

if [ "$fflag" == "1" ] && [ "$Dflag" == "1" ]
then
  remove_first_ip  $publicIp &&
  del_fw_chain_for_ip $publicIp &&
  del_vpn_chain_for_ip $publicIp
  unlock_exit $? $lock $locked
fi

if [ "$Dflag" == "1" ]
then
  remove_an_ip  $publicIp &&
  del_fw_chain_for_ip $publicIp 
  unlock_exit $? $lock $locked
fi

unlock_exit 0 $lock $locked

