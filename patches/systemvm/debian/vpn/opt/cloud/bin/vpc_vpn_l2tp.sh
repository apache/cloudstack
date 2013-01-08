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
  printf "Create VPN     : %s -c -r <ip range for clients> -l <local ip> -p <ipsec psk> -s <public ip> -D <eth for public ip> -z < zone cidr>  \n" $(basename $0)
  printf "Delete VPN     : %s -d -l <local ip> -s <public ip> -D <eth for public ip> -z < zone cidr> \n" $(basename $0)
  printf "Add VPN User   : %s -u <username,password> \n" $(basename $0)
  printf "Remote VPN User: %s -U <username \n" $(basename $0)
}

iptables_() {
  local op=$1
  local public_ip=$2

  sudo iptables $op INPUT -i $dev -d $public_ip -p udp --dport 500 -j ACCEPT
  sudo iptables $op INPUT -i $dev -d $public_ip -p udp --dport 4500 -j ACCEPT
  sudo iptables $op INPUT -i $dev -d $public_ip -p udp --dport 1701 -j ACCEPT
  sudo iptables $op INPUT -i $dev -p ah -j ACCEPT
  sudo iptables $op INPUT -i $dev -p esp -j ACCEPT
  sudo iptables $op FORWARD -i ppp+ -d $zcidr -j ACCEPT
  sudo iptables $op FORWARD -i ppp+ -o ppp+ -j ACCEPT 
  sudo iptables $op INPUT -i ppp+ -p udp --dport 53 -j ACCEPT
  sudo iptables $op INPUT -i ppp+ -p tcp --dport 53 -j ACCEPT
  sudo iptables -t nat $op PREROUTING -i ppp+ -p udp --dport 53 -j DNAT --to-destination $local_ip
  sudo iptables -t nat $op PREROUTING -i ppp+ -p tcp --dport 53 -j DNAT --to-destination $local_ip
}

ipsec_server() {
  local op=$1
  if [ "$op" == "restart" ]
  then
    service ipsec stop
    service xl2tpd stop
    service ipsec start
    service xl2tpd start
    return $?
  fi
  service ipsec $op
  service xl2tpd $op
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

  remove_l2tp_ipsec_user $u
  echo "$u * $passwd *" >> /etc/ppp/chap-secrets
}

rflag=
pflag=
lflag=
sflag=
create=
destroy=
useradd=
userdel=

while getopts 'cdl:p:r:s:u:U:D:z' OPTION
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
  D)    dev="$OPTARG"
                ;;
  z)    zcidr="$OPTARG"
                ;;
  ?)	usage
		exit 2
		;;
  esac
done

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
