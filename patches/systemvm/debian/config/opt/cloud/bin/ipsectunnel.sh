# Copyright 2012 Citrix Systems, Inc. Licensed under the
# Apache License, Version 2.0 (the "License"); you may not use this
# file except in compliance with the License.  Citrix Systems, Inc.
# reserves all rights not expressly granted by the License.
# You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/usr/bin/env bash
source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

vpnconfdir="/etc/ipsec.d"
vpnoutmark="0x525"

usage() {
    printf "Usage: %s: (-A|-D) -l <left-side vpn peer> -n <left-side guest cidr> -g <left-side gateway> -r <right-side vpn peer> -N <right-side private subnets> -e <esp policy> -i <ike policy> -t <lifetime> -s <pre-shared secret> \n" $(basename $0) >&2
}

#set -x

start_ipsec() {
    service ipsec status > /dev/null
    if [ $? -ne 0 ]
    then
        service ipsec start > /dev/null
    fi
}

enable_iptable() {
  sudo iptables -A INPUT -i $outIf -p udp -m udp --dport 500 -j ACCEPT
  for net in $rightnets
  do
    sudo iptables -A FORWARD -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
    sudo iptables -A OUTPUT -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
  done
  # Prevent NAT on "marked" VPN traffic, so need to be the first one on POSTROUTING chain
  sudo iptables -t nat -I POSTROUTING -t nat -o $outIf -m mark --mark $vpnoutmark -j ACCEPT
}

disable_iptable() {
  sudo iptables -D INPUT -i $outIf -p udp -m udp --dport 500 -j ACCEPT
  for net in $rightnets
  do
    sudo iptables -D FORWARD -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
    sudo iptables -D OUTPUT -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
  done
  sudo iptables -t nat -D POSTROUTING -t nat -o $outIf -m mark --mark $vpnoutmark -j ACCEPT
}

ipsec_tunnel_del() {
  disable_iptable
  sudo ipsec auto --down vpn-$rightpeer
  sudo ipsec auto --delete vpn-$rightpeer
  outIp=$leftpeer
  local vpnconffile=$vpnconfdir/ipsec.vpn-$rightpeer.conf
  local vpnsecretsfile=$vpnconfdir/ipsec.vpn-$rightpeer.secrets
  logger -t cloud "$(basename $0): removing configuration for ipsec tunnel to $rightpeer"
  sudo rm -f $vpnconffile
  sudo rm -f $vpnsecretsfile
  sudo ipsec auto --rereadall
}

ipsec_tunnel_add() {
  #need to unify with remote access VPN
  start_ipsec

  outIp=$leftpeer
  sudo mkdir -p $vpnconfdir
  local vpnconffile=$vpnconfdir/ipsec.vpn-$rightpeer.conf
  local vpnsecretsfile=$vpnconfdir/ipsec.vpn-$rightpeer.secrets

  logger -t cloud "$(basename $0): creating configuration for ipsec tunnel: left peer=$leftpeer \
    left net=$leftnet left gateway=$leftgw right peer=$rightpeer right network=$rightnets phase1 policy=$ikepolicy \
    phase2 policy=$esppolicy lifetime=$time secret=$secret"

  [ "$op" == "-A" ] && ipsec_tunnel_del $rightpeer $rightnets "-D"
    sudo echo "conn vpn-$rightpeer" > $vpnconffile &&
    sudo echo "  left=$leftpeer" >> $vpnconffile &&
    sudo echo "  leftsubnet=$leftnet" >> $vpnconffile &&
    sudo echo "  leftnexthop=$leftgw" >> $vpnconffile &&
    sudo echo "  right=$rightpeer" >> $vpnconffile &&
    sudo echo "  rightsubnets={$rightnets}" >> $vpnconffile &&
    sudo echo "  type=tunnel" >> $vpnconffile &&
    sudo echo "  authby=secret" >> $vpnconffile &&
    sudo echo "  keyexchange=ike" >> $vpnconffile &&
    sudo echo "  pfs=no" >> $vpnconffile &&
    sudo echo "  esp=$esppolicy" >> $vpnconffile &&
    sudo echo "  salifetime=${time}s" >> $vpnconffile &&
    sudo echo "  ike=$ikepolicy" >> $vpnconffile &&
    sudo echo "  ikelifetime=${time}s" >> $vpnconffile &&
    sudo echo "  keyingtries=3" >> $vpnconffile &&
    sudo echo "  dpddelay=30" >> $vpnconffile &&
    sudo echo "  dpdtimeout=120" >> $vpnconffile &&
    sudo echo "  dpdaction=restart" >> $vpnconffile &&
    sudo echo "  auto=add" >> $vpnconffile &&
    sudo echo "$leftpeer $rightpeer: PSK \"$secret\"" > $vpnsecretsfile &&

    sudo chmod 0400 $vpnsecretsfile

    enable_iptable

    sudo ipsec auto --rereadall
    sudo ipsec auto --add vpn-$rightpeer
    sudo ipsec auto --up vpn-$rightpeer

  logger -t cloud "$(basename $0): done ipsec tunnel entry for right peer=$rightpeer right networks=$rightnets"

  #20 seconds for checking if it's ready
  for i in {1..4}
  do
    logger -t cloud "$(basename $0): checking connection status..."
    /opt/cloud/bin/checks2svpn.sh $rightpeer
    result=$?
    if [ $result -eq 0 ]
    then
        break
    fi
    sleep 5
  done
  if [ $result -eq 0 ]
  then
    logger -t cloud "$(basename $0): connect to remote successful"
  else
    logger -t cloud "$(basename $0): fail to connect to remote, status code: $result"
    logger -t cloud "$(basename $0): would stop site-to-site VPN connection"
    ipsec_tunnel_del
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
op=""

while getopts 'ADl:n:g:r:N:e:i:t:s:' OPTION
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
        leftgw="$OPTARG"
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
        time="$OPTARG"
        ;;
  s)    sflag=1
        secret="$OPTARG"
        ;;
  ?)    usage
        unlock_exit 2 $lock $locked
        ;;
  esac
done

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
    unlock_exit 5 $lock $locked
fi

unlock_exit $ret $lock $locked
