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

source /root/func.sh

vpnconfdir="/etc/ipsec.d"
vpnoutmark="0x525"
vpninmark="0x524"

usage() {
    printf "Usage: %s: (-A|-D) -l <left-side vpn peer> -n <left-side guest cidr> -g <left-side next hop> -r <right-side vpn peer> -N <right-side private subnets> -e <esp policy> -i <ike policy> -t <ike lifetime> -T <esp lifetime> -s <pre-shared secret> -d <dpd 0 or 1> [ -p <passive or not> -c <check if up on creation> -S <disable vpn ports iptables> ]\n" $(basename $0) >&2
}

#set -x

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

enable_iptables_subnets() {
  for net in $rightnets
  do
    sudo iptables -I FORWARD -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
    sudo iptables -A OUTPUT -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
    sudo iptables -I FORWARD -t mangle -s $net -d $leftnet -j MARK --set-mark $vpninmark
    sudo iptables -A INPUT -t mangle -s $net -d $leftnet -j MARK --set-mark $vpninmark
  done
  return 0
}

#
# Add the right side here to close the gap, so we're sure no one else comes in
#   also double check the default behaviour of ipsec to drop if wrong....
check_and_enable_iptables() {
  sudo iptables-save | grep "A INPUT -i $outIf -p udp -m udp --dport 500 -j ACCEPT"
  if [ $? -ne 0 ]
  then
      sudo iptables -A INPUT -i $outIf -p udp -m udp --dport 500 $iptables_secure -j ACCEPT
      sudo iptables -A INPUT -i $outIf -p udp -m udp --dport 4500 $iptables_secure -j ACCEPT
      sudo iptables -A INPUT -i $outIf -p 50 $iptables_secure -j ACCEPT
      # Prevent NAT on "marked" VPN traffic, so need to be the first one on POSTROUTING chain
      sudo iptables -t nat -I POSTROUTING -t nat -o $outIf -m mark --mark $vpnoutmark -j ACCEPT
  fi
  return 0
}

disable_iptables_subnets() {
  for net in $rightnets
  do
    sudo iptables -D FORWARD -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
    sudo iptables -D OUTPUT -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
    sudo iptables -D FORWARD -t mangle -s $net -d $leftnet -j MARK --set-mark $vpninmark
    sudo iptables -D INPUT -t mangle -s $net -d $leftnet -j MARK --set-mark $vpninmark
  done
  return 0
}

check_and_disable_iptables() {
  find $vpnconfdir -name "ipsec.vpn*.conf" | grep ipsec
  if [ $? -ne 0 ]
  then
    #Nobody else use s2s vpn now, so delete the iptables rules
    sudo iptables -D INPUT -i $outIf -p udp -m udp --dport 500 $iptables_secure -j ACCEPT
    sudo iptables -D INPUT -i $outIf -p udp -m udp --dport 4500 $iptables_secure -j ACCEPT
    sudo iptables -D INPUT -i $outIf -p 50 $iptables_secure -j ACCEPT
    sudo iptables -t nat -D POSTROUTING -t nat -o $outIf -m mark --mark $vpnoutmark -j ACCEPT
  fi
  return 0
}

ipsec_tunnel_del() {
  disable_iptables_subnets
  sudo ipsec auto --down vpn-$rightpeer
  sudo ipsec auto --delete vpn-$rightpeer
  outIp=$leftpeer
  local vpnconffile=$vpnconfdir/ipsec.vpn-$rightpeer.conf
  local vpnsecretsfile=$vpnconfdir/ipsec.vpn-$rightpeer.secrets
  logger -t cloud "$(basename $0): removing configuration for ipsec tunnel to $rightpeer"
  sudo rm -f $vpnconffile
  sudo rm -f $vpnsecretsfile
  sudo ipsec auto --rereadall
  check_and_disable_iptables
  return 0
}

ipsec_tunnel_add() {
  #need to unify with remote access VPN
  start_ipsec

  if [ $? -ne 0 ]
  then
      logger -t cloud "$(basename $0): Failed to start ipsec service!"
      return 1
  fi

  outIp=$leftpeer
  sudo mkdir -p $vpnconfdir
  local vpnconffile=$vpnconfdir/ipsec.vpn-$rightpeer.conf
  local vpnsecretsfile=$vpnconfdir/ipsec.vpn-$rightpeer.secrets

  logger -t cloud "$(basename $0): creating configuration for ipsec tunnel: left peer=$leftpeer \
    left net=$leftnet left gateway=$leftnexthop right peer=$rightpeer right network=$rightnets phase1 policy=$ikepolicy \
    phase2 policy=$esppolicy secret=$secret"

  [ "$op" == "-A" ] && ipsec_tunnel_del

  check_and_enable_iptables

    sudo echo "conn vpn-$rightpeer" > $vpnconffile &&
    sudo echo "  left=$leftpeer" >> $vpnconffile &&
    sudo echo "  leftsubnet=$leftnet" >> $vpnconffile &&
    sudo echo "  leftnexthop=$leftnexthop" >> $vpnconffile &&
    sudo echo "  right=$rightpeer" >> $vpnconffile &&
    sudo echo "  rightsubnets={$rightnets}" >> $vpnconffile &&
    sudo echo "  type=tunnel" >> $vpnconffile &&
    sudo echo "  authby=secret" >> $vpnconffile &&
    sudo echo "  keyexchange=ike" >> $vpnconffile &&
    sudo echo "  ike=$ikepolicy" >> $vpnconffile &&
    sudo echo "  ikelifetime=${ikelifetime}s" >> $vpnconffile &&
    sudo echo "  esp=$esppolicy" >> $vpnconffile &&
    sudo echo "  salifetime=${esplifetime}s" >> $vpnconffile &&
    sudo echo "  pfs=$pfs" >> $vpnconffile &&
    sudo echo "  keyingtries=2" >> $vpnconffile &&
    sudo echo "  auto=start" >> $vpnconffile &&
    sudo echo "$leftpeer $rightpeer: PSK \"$secret\"" > $vpnsecretsfile &&
    sudo chmod 0400 $vpnsecretsfile

    if [ $dpd -ne 0 ]
    then
        sudo echo "  dpddelay=30" >> $vpnconffile &&
        sudo echo "  dpdtimeout=120" >> $vpnconffile &&
        sudo echo "  dpdaction=restart" >> $vpnconffile
    fi

    enable_iptables_subnets

    sudo ipsec auto --rereadall
    sudo ipsec auto --add vpn-$rightpeer

  logger -t cloud "$(basename $0): done ipsec tunnel entry for right peer=$rightpeer right networks=$rightnets"

  result=0

  if [ $passive -eq 0 ]
  then
      sudo ipsec auto --up vpn-$rightpeer &
  fi
  if [ $checkup -eq 1 ]
  then

    #5 seconds for checking if it's ready
    for i in {1..5}
    do
      logger -t cloud "$(basename $0): checking connection status..."
      /opt/cloud/bin/checks2svpn.sh $rightpeer
      result=$?
      if [ $result -eq 0 ]
      then
          break
      fi
      sleep 1
    done
    if [ $result -eq 0 ]
    then
      logger -t cloud "$(basename $0): connect to remote successful"
    else
      logger -t cloud "$(basename $0): fail to connect to remote, status code: $result"
      logger -t cloud "$(basename $0): would stop site-to-site VPN connection"
      ipsec_tunnel_del
    fi
  fi
  return $result
}

rflag=
pflag=
eflag=
Eflag=
lflag=
iflag=
Iflag=
sflag=
passive=0
op=""
checkup=0
secure=1

while getopts 'ADSpcl:n:g:r:N:e:i:t:T:s:d:' OPTION
do
  case $OPTION in
  A)    opflag=1
        op="-A"
        ;;
  D)    opflag=2
        op="-D"
        ;;
  l)    lflag=1
        leftpeer="$OPTARG"
        ;;
  n)    nflag=1
        leftnet="$OPTARG"
        ;;
  g)    gflag=1
        leftnexthop="$OPTARG"
        ;;
  r)    rflag=1
        rightpeer="$OPTARG"
        ;;
  N)    Nflag=1
        rightnets="$OPTARG"
        ;;
  e)    eflag=1
        esppolicy="$OPTARG"
        ;;
  i)    iflag=1
        ikepolicy="$OPTARG"
        ;;
  t)    tflag=1
        ikelifetime="$OPTARG"
        ;;
  T)    Tflag=1
        esplifetime="$OPTARG"
        ;;
  s)    sflag=1
        secret="$OPTARG"
        ;;
  d)    dflag=1
        dpd="$OPTARG"
        ;;
  p)    passive=1
        ;;
  c)    checkup=1
        ;;
  S)    secure=0
        ;;
  ?)    usage
        exit 2
        ;;
  esac
done

logger -t cloud "$(basename $0): parameters $*"
if [ $secure -eq 1 ]
then
   iptables_secure=" -s $rightpeer -d $leftpeer "
fi

# get interface for public ip
ip link|grep BROADCAST|grep -v eth0|cut -d ":" -f 2 > /tmp/iflist
while read i
do
    ip addr show $i|grep "$leftpeer"
    if [ $? -eq 0 ]
    then
        outIf=$i
        break
    fi
done < /tmp/iflist

rightnets=${rightnets//,/ }
pfs="no"
echo "$esppolicy" | grep "modp" > /dev/null
if [ $? -eq 0 ]
then
    pfs="yes"
fi

ret=0
#Firewall ports for one-to-one/static NAT
if [ "$opflag" == "1" ]
then
    ipsec_tunnel_add
    ret=$?
elif [ "$opflag" == "2" ]
then
    ipsec_tunnel_del
    ret=$?
else
    printf "Invalid action specified, must choose -A or -D to add/del tunnels\n" >&2
    exit 5
fi

exit $ret
