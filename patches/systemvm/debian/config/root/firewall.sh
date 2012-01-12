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
# $Id: firewall.sh 9947 2010-06-25 19:34:24Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/patches/xenserver/root/firewall.sh $
# firewall.sh -- allow some ports / protocols to vm instances
# @VERSION@

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage: %s: (-A|-D)   -r <target-instance-ip> -P protocol (-p port_range | -t icmp_type_code)  -l <public ip address> -d <target port> -s <source cidrs> [-G]   \n" $(basename $0) >&2
}

#set -x

get_dev_list() {
  ip link show | grep -e eth[2-9] | awk -F ":" '{print $2}'
  ip link show | grep -e eth1[0-9] | awk -F ":" '{print $2}'
}

ip_to_dev() {
  local ip=$1

  for dev in $DEV_LIST; do
    ip addr show dev $dev | grep inet | grep $ip &>> /dev/null
    [ $? -eq 0 ] && echo $dev && return 0
  done
  return 1
}


#Port (address translation) forwarding for tcp or udp
tcp_or_udp_entry() {
  local instIp=$1
  local dport0=$2
  local dport=$(echo $2 | sed 's/:/-/')
  local publicIp=$3
  local port=$4
  local op=$5
  local proto=$6
  local cidrs=$7
  logger -t cloud "$(basename $0): creating port fwd entry for PAT: public ip=$publicIp \
  instance ip=$instIp proto=$proto port=$port dport=$dport op=$op"

  #if adding, this might be a duplicate, so delete the old one first
  [ "$op" == "-A" ] && tcp_or_udp_entry $instIp $dport0 $publicIp $port "-D" $proto $cidrs
  # the delete operation may have errored out but the only possible reason is 
  # that the rules didn't exist in the first place
  local dev=$(ip_to_dev $publicIp)
  # shortcircuit the process if error and it is an append operation
  # continue if it is delete
  (sudo iptables -t nat $op PREROUTING --proto $proto -i $dev -d $publicIp \
           --destination-port $port -j DNAT  \
           --to-destination $instIp:$dport &>> $OUTFILE || [ "$op" == "-D" ]) &&
  (sudo iptables -t nat $op OUTPUT  --proto $proto -d $publicIp  \
           --destination-port $port -j DNAT  \
           --to-destination $instIp:$dport &>> $OUTFILE || [ "$op" == "-D" ]) &&
  (sudo iptables $op FORWARD -p $proto -s $cidrs -d $instIp -m state \
           --state ESTABLISHED,RELATED -m comment --comment "$publicIp:$port" -j ACCEPT &>>  $OUTFILE || [ "$op" == "-D" ]) &&
  (sudo iptables $op FORWARD -p $proto -s $cidrs -d $instIp  \
           --destination-port $dport0 -m state --state NEW -m comment --comment "$publicIp:$port" -j ACCEPT &>>  $OUTFILE)
      

  local result=$?
  logger -t cloud "$(basename $0): done port fwd entry for PAT: public ip=$publicIp op=$op result=$result"
  return $result
}


#Forward icmp
icmp_entry() {
  local instIp=$1
  local icmptype=$2
  local publicIp=$3
  local op=$4
  
  logger -t cloud "$(basename $0): creating port fwd entry for PAT: public ip=$publicIp \
  instance ip=$instIp proto=icmp port=$port dport=$dport op=$op"
  #if adding, this might be a duplicate, so delete the old one first
  [ "$op" == "-A" ] && icmp_entry $instIp $icmpType $publicIp "-D" 
  # the delete operation may have errored out but the only possible reason is 
  # that the rules didn't exist in the first place
  local dev=$(ip_to_dev $publicIp)
  sudo iptables -t nat $op PREROUTING --proto icmp -i $dev -d $publicIp --icmp-type $icmptype -j DNAT --to-destination $instIp &>>  $OUTFILE
       
  sudo iptables -t nat $op OUTPUT  --proto icmp -d $publicIp --icmp-type $icmptype -j DNAT --to-destination $instIp &>>  $OUTFILE
  sudo iptables $op FORWARD -p icmp -s 0/0 -d $instIp --icmp-type $icmptype  -j ACCEPT &>>  $OUTFILE
      
  result=$?
  logger -t cloud "$(basename $0): done port fwd entry for PAT: public ip=$publicIp op=$op result=$result"
  return $result
}



one_to_one_fw_entry() {
  local publicIp=$1
  local instIp=$2  
  local proto=$3
  local portRange=$4 
  local op=$5
  logger -t cloud "$(basename $0): create firewall entry for static nat: public ip=$publicIp \
  instance ip=$instIp proto=$proto portRange=$portRange op=$op"

  #if adding, this might be a duplicate, so delete the old one first
  [ "$op" == "-A" ] && one_to_one_fw_entry $publicIp $instIp $proto $portRange "-D" 
  # the delete operation may have errored out but the only possible reason is 
  # that the rules didn't exist in the first place

  local dev=$(ip_to_dev $publicIp)
  [ $? -ne 0 ] && echo "Could not find device associated with $publicIp" && return 1

  # shortcircuit the process if error and it is an append operation
  # continue if it is delete
  (sudo iptables -t nat $op  PREROUTING -i $dev -d $publicIp --proto $proto \
           --destination-port $portRange -j DNAT \
           --to-destination $instIp &>>  $OUTFILE || [ "$op" == "-D" ]) &&
  (sudo iptables $op FORWARD -i $dev -o eth0 -d $instIp --proto $proto \
           --destination-port $portRange -m state \
           --state NEW -j ACCEPT &>>  $OUTFILE )

  result=$?
  logger -t cloud "$(basename $0): done firewall entry public ip=$publicIp op=$op result=$result"
  return $result
}

fw_chain_for_ip() {
  local pubIp=$1
  if  iptables -t mangle -N FIREWALL_$pubIp &> /dev/null
  then
    logger -t cloud "$(basename $0): created a firewall chain for $pubIp"
    (sudo iptables -t mangle -A FIREWALL_$pubIp -j DROP) &&
    (sudo iptables -t mangle -I FIREWALL_$pubIp -m state --state RELATED,ESTABLISHED -j ACCEPT ) &&
    (sudo iptables -t mangle -I PREROUTING 2 -d $pubIp -j FIREWALL_$pubIp)
    return $?
  fi
  logger -t cloud "fw chain for $pubIp already exists"
  return 0
}

static_nat() {
  local publicIp=$1
  local instIp=$2  
  local op=$3
  local op2="-D"
  local rulenum=
  logger -t cloud "$(basename $0): static nat: public ip=$publicIp \
  instance ip=$instIp  op=$op"
  
  #TODO check error below
  fw_chain_for_ip $publicIp

  #if adding, this might be a duplicate, so delete the old one first
  [ "$op" == "-A" ] && static_nat $publicIp $instIp  "-D" 
  # the delete operation may have errored out but the only possible reason is 
  # that the rules didn't exist in the first place
  [ "$op" == "-A" ] && rulenum=1
  [ "$op" == "-A" ] && op2="-I"

  local dev=$(ip_to_dev $publicIp)
  [ $? -ne 0 ] && echo "Could not find device associated with $publicIp" && return 1

  # shortcircuit the process if error and it is an append operation
  # continue if it is delete
  (sudo iptables -t nat $op  PREROUTING -i $dev -d $publicIp -j DNAT \
           --to-destination $instIp &>>  $OUTFILE || [ "$op" == "-D" ]) &&
  (sudo iptables $op FORWARD -i $dev -o eth0 -d $instIp  -m state \
           --state NEW -j ACCEPT &>>  $OUTFILE || [ "$op" == "-D" ]) &&
  (sudo iptables -t nat $op2 POSTROUTING $rulenum -s $instIp -j SNAT \
           --to-source $publicIp &>> $OUTFILE || [ "$op" == "-D" ])

  result=$?
  logger -t cloud "$(basename $0): done static nat entry public ip=$publicIp op=$op result=$result"
  return $result
}



rflag=
Pflag=
pflag=
tflag=
lflag=
dflag=
sflag=
Gflag=
op=""

while getopts 'ADr:P:p:t:l:d:s:G' OPTION
do
  case $OPTION in
  A)    op="-A"
        ;;
  D)    op="-D"
        ;;
  r)    rflag=1
        instanceIp="$OPTARG"
        ;;
  P)    Pflag=1
        protocol="$OPTARG"
        ;;
  p)    pflag=1
        ports="$OPTARG"
        ;;
  t)    tflag=1
        icmptype="$OPTARG"
        ;;
  l)    lflag=1
        publicIp="$OPTARG"
        ;;
  s)    sflag=1
        cidrs="$OPTARG"
        ;;
  d)    dflag=1
        dport="$OPTARG"
        ;;
  G)    Gflag=1
        ;;
  ?)    usage
        unlock_exit 2 $lock $locked
        ;;
  esac
done

DEV_LIST=$(get_dev_list)
OUTFILE=$(mktemp)

#Firewall ports for one-to-one/static NAT
if [ "$Gflag" == "1" ]
then
  if [ "$protocol" == "" ] 
  then
    static_nat $publicIp $instanceIp  $op
  else
    one_to_one_fw_entry $publicIp $instanceIp  $protocol $dport $op
  fi
  result=$?
  if [ "$result" -ne 0 ] && [ "$op" != "-D" ]; then
      cat $OUTFILE >&2
  fi
  rm -f $OUTFILE
  if [ "$op" == "-D" ];then
     result=0
  fi
  unlock_exit $result $lock $locked
fi

if [ "$sflag" != "1" ]
then
    cidrs="0/0"
fi

case $protocol  in
  tcp|udp)    
        tcp_or_udp_entry $instanceIp $dport $publicIp $ports $op $protocol $cidrs
        result=$?
        if [ "$result" -ne 0 ] && [ "$op" != "-D" ];then
           cat $OUTFILE >&2
        fi
        rm -f $OUTFILE
        if [ "$op" == "-D" ];then
           result=0
        fi
        unlock_exit $result $lock $locked
        ;;
  "icmp")  
  
        icmp_entry $instanceIp $icmptype $publicIp $op 
        if [ "$op" == "-D" ];then
           result=0
        fi
        unlock_exit $? $lock $locked
        ;;
      *)
        printf "Invalid protocol-- must be tcp, udp or icmp\n" >&2
        unlock_exit 5 $lock $locked
        ;;
esac

unlock_exit 0 $lock $locked
