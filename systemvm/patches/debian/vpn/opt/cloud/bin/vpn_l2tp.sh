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

 


#set -x
usage() {
  printf "Usage:\n"
  printf "Create VPN     : %s -c -r <ip range for clients> -l <local ip> -p <ipsec psk> -s <public ip> -i <eth for public ip> \n" $(basename $0)
  printf "Delete VPN     : %s -d -l <local ip> -s <public ip> -D <eth for public ip> -C < local cidr> \n" $(basename $0)
  printf "Add VPN User   : %s -u <username,password> \n" $(basename $0)
  printf "Remote VPN User: %s -U <username \n" $(basename $0)
}

get_intf_ip() {
  ip addr show $1 | grep -w inet | awk '{print $2}' | awk -F'/' '{print $1}'
}

iptables_() {
   local op=$1
   local public_ip=$2
   local is_vpc=false
   local forward_action="ACCEPT"
   if grep "vpcrouter" /var/cache/cloud/cmdline &> /dev/null
   then
	is_vpc=true
   fi

   sudo iptables $op INPUT -i $dev --dst $public_ip -p udp -m udp --dport 500 -j ACCEPT
   sudo iptables $op INPUT -i $dev --dst $public_ip -p udp -m udp --dport 4500 -j ACCEPT
   sudo iptables $op INPUT -i $dev --dst $public_ip -p udp -m udp --dport 1701 -j ACCEPT
   sudo iptables $op INPUT -i $dev -p ah -j ACCEPT
   sudo iptables $op INPUT -i $dev -p esp -j ACCEPT
   if $is_vpc
   then
       # Need to apply the following ACL rules as well.
       if sudo iptables -N VPN_FORWARD &> /dev/null
       then
           sudo iptables -I FORWARD -i ppp+ -j VPN_FORWARD
           sudo iptables -I FORWARD -o ppp+ -j VPN_FORWARD
           sudo iptables -A VPN_FORWARD -j DROP
       fi
       sudo iptables $op VPN_FORWARD -i ppp+ -o ppp+ -j RETURN
       sudo iptables $op VPN_FORWARD -i ppp+ -d $cidr -j RETURN
       sudo iptables $op VPN_FORWARD -s $cidr -o ppp+ -j RETURN
   else
       sudo iptables $op FORWARD -i ppp+ -d $cidr -j ACCEPT
       sudo iptables $op FORWARD -s $cidr -o ppp+ -j ACCEPT
       sudo iptables $op FORWARD -i ppp+ -o ppp+ -j ACCEPT
   fi
   sudo iptables $op INPUT -i ppp+ -m udp -p udp --dport 53 -j ACCEPT
   sudo iptables $op INPUT -i ppp+ -m tcp -p tcp --dport 53 -j ACCEPT
   sudo iptables -t nat $op PREROUTING -i ppp+ -p tcp -m tcp --dport 53 -j  DNAT --to-destination $local_ip
   sudo iptables -t nat $op PREROUTING -i ppp+ -p udp -m udp --dport 53 -j  DNAT --to-destination $local_ip

   if $is_vpc
   then
       return
   fi

   if sudo iptables -t mangle -N VPN_$public_ip &> /dev/null
   then
     logger -t cloud "$(basename $0): created VPN chain in PREROUTING mangle"
     sudo iptables -t mangle -I PREROUTING -d $public_ip -j VPN_$public_ip
     sudo iptables -t mangle -A VPN_$public_ip -j RETURN
   fi
   op2="-D"
   [ "$op" == "-A" ] && op2="-I"
   sudo iptables -t mangle $op VPN_$public_ip  -p ah -j ACCEPT
   sudo iptables -t mangle $op VPN_$public_ip  -p esp -j ACCEPT
}

start_ipsec() {
  service ipsec status > /dev/null
  if [ $? -ne 0 ]
  then
    service ipsec start > /dev/null
    #Wait until ipsec started, 5 seconds at most
    for i in {1..5}
    do
      logger -t cloud "$(basename $0): waiting ipsec start..."
      service ipsec status > /dev/null
      result=$?
      if [ $result -eq 0 ]
      then
          break
      fi
      sleep 1
    done
  fi
  service ipsec status > /dev/null
  return $?
}

ipsec_server() {
   local op=$1
   case $op in
       "start")     start_ipsec
                    sudo service xl2tpd start
                    ;;
        "stop")     sudo service xl2tpd stop
                    ;;
        "restart")  start_ipsec
                    sudo ipsec auto --rereadall
                    service xl2tpd stop
                    service xl2tpd start
                    ;;
   esac
}

create_l2tp_ipsec_vpn_server() {
   local ipsec_psk=$1
   local public_ip=$2
   local client_range=$3
   local local_ip=$4

   sed -i -e "s/left=.*$/left=$public_ip/" /etc/ipsec.d/l2tp.conf
   echo ": PSK \"$ipsec_psk\"" > /etc/ipsec.d/ipsec.any.secrets
   sed -i -e "s/^ip range = .*$/ip range = $client_range/"  /etc/xl2tpd/xl2tpd.conf
   sed -i -e "s/^local ip = .*$/local ip = $local_ip/"  /etc/xl2tpd/xl2tpd.conf

   sed -i -e "s/^ms-dns.*$/ms-dns $local_ip/" /etc/ppp/options.xl2tpd

   iptables_ "-D" $public_ip
   iptables_ "-I" $public_ip

   ipsec_server "restart"

   ipsec auto --rereadsecrets 
   ipsec auto --replace L2TP-PSK
}

destroy_l2tp_ipsec_vpn_server() {
   local public_ip=$1

   ipsec auto --down L2TP-PSK

   iptables_ "-D" $public_ip
   
   ipsec_server "stop"
}

remove_l2tp_ipsec_user() {
   local u=$1
   sed -i -e "/^$u .*$/d" /etc/ppp/chap-secrets
   if [ -x /usr/bin/tdbdump ]; then
      pid=$(tdbdump /var/run/pppd2.tdb | grep -w $u | awk -F';' '{print $4}' | awk -F= '{print $2}')
      [ "$pid" != "" ] && kill -9 $pid
   fi
   return 0
}

add_l2tp_ipsec_user() {
   local u=$1
   local passwd=$2

   uptodate=$(grep "^$u \* \"$passwd\" \*$" /etc/ppp/chap-secrets)
   if [ "$uptodate" == "" ]
   then
       remove_l2tp_ipsec_user $u
       echo "$u * \"$passwd\" *" >> /etc/ppp/chap-secrets
   fi
}

rflag=
pflag=
lflag=
sflag=
create=
destroy=
useradd=
userdel=
dev=
cidr=

while getopts 'cdl:p:r:s:u:U:i:C:' OPTION
do
  case $OPTION in
  c)	create=1
		;;
  d)	destroy=1
		;;
  u)	useradd=1
		user_pwd="$OPTARG"
		;;
  U)	userdel=1
		user="$OPTARG"
		;;
  r)	rflag=1
		client_range="$OPTARG"
		;;
  p)	pflag=1
		ipsec_psk="$OPTARG"
		;;
  l)	lflag=1
		local_ip="$OPTARG"
		;;
  s)	sflag=1
		server_ip="$OPTARG"
		;;
  i)    dev="$OPTARG"
                ;;
  C)    cidr="$OPTARG"
                ;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$dev" == "" ]
then
    dev="eth2"
fi

if [ "$cidr" == "" ]
then
    cidr=$(get_intf_ip "eth0")
fi

[ "$create$destroy" == "11" ] || [ "$create$destroy$useradd$userdel" == "" ] && usage && exit 2
[ "$create" == "1" ] && [ "$lflag$pflag$rflag$sflag" != "1111" ] && usage && exit 2

if [ "$create" == "1" ]; then
    create_l2tp_ipsec_vpn_server $ipsec_psk $server_ip $client_range $local_ip
    exit $?
fi

if [ "$destroy" == "1" ]; then
   destroy_l2tp_ipsec_vpn_server $server_ip
   exit $?
fi

if [ "$useradd" == "1" ]; then
   u=$(echo $user_pwd | awk -F',' '{print $1}')
   pwd=$(echo $user_pwd | awk -F',' '{print $2}')
   add_l2tp_ipsec_user $u $pwd
   exit $?
fi
if [ "$userdel" == "1" ]; then
   remove_l2tp_ipsec_user $user 
   exit $?
fi
