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

enable_iptable() {
  sudo iptables -A INPUT -i $outIf -p udp -m udp --dport 500 -j ACCEPT
  for net in $rightnets
  do
    sudo iptables -A PREROUTING -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
  done
}

disable_iptable() {
  sudo iptables -D INPUT -i $outIf -p udp -m udp --dport 500 -j ACCEPT
  for net in $rightnets
  do
    sudo iptables -D PREROUTING -t mangle -s $leftnet -d $net -j MARK --set-mark $vpnoutmark
  done
}

ipsec_tunnel_del() {
  disable_iptable
  outIp=$leftpeer
  local vpnconffile=$vpnconfdir/ipsec.vpn-$rightpeer.conf
  local vpnsecretsfile=$vpnconfdir/ipsec.vpn-$rightpeer.secrets
  logger -t cloud "$(basename $0): removing configuration for ipsec tunnel to $rightpeer"
  sudo rm -f $vpnconffile
  sudo rm -f $vpnsecretsfile
}

ipsec_tunnel_add() {
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
    sudo echo "  auto=start" >> $vpnconffile &&
    sudo echo "$leftpeer $rightpeer: PSK \"$secret\"" > $vpnsecretsfile &&

    sudo chmod 0400 $vpnsecretsfile

    enable_iptable

    sudo service ipsec restart
    # Prevent NAT on "marked" VPN traffic
    sudo iptables -D POSTROUTING -t nat -o $outIf -j SNAT --to-source $outIp
    sudo iptables -D POSTROUTING -t nat -o $outIf -m mark ! --mark $vpnoutmark -j SNAT --to-source $outIp
    sudo iptables -A POSTROUTING -t nat -o $outIf -m mark ! --mark $vpnoutmark -j SNAT --to-source $outIp

  result=$?
  logger -t cloud "$(basename $0): done ipsec tunnel entry for right peer=$rightpeer right networks=$rightnets"
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

#Firewall ports for one-to-one/static NAT
if [ "$opflag" == "1" ]
then
    ipsec_tunnel_add
elif [ "$opflag" == "2" ]
then
    ipsec_tunnel_del
else
    printf "Invalid action specified, must choose -A or -D to add/del tunnels\n" >&2
    unlock_exit 5 $lock $locked
fi

unlock_exit 0 $lock $locked
