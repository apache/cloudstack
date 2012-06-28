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

#lock="biglock"
#locked=$(getLockFile $lock)
#if [ "$locked" != "1" ]
#then
#    exit 1
#fi

vpnconfdir="/etc/ipsec.d"
vpninmark="10"
vpnoutmark="15"
inIf="eth0"
outIf="eth2"

usage() {
  printf "Usage: %s: (-A|-D) -r <right-side vpn peer> -R <right-side private ip> -p <right-side private subnet> -e <esp encryption> -E <esp hash> -l <sa lifetime> -i <ike encryption> -I <ike hash> -L <ike lifetime> -s <pre-shared secret> \n" $(basename $0) >&2
}

#set -x

get_dev_list() {
  ip link show | grep -e eth[2-9] | awk -F ":" '{print $2}'
  ip link show | grep -e eth1[0-9] | awk -F ":" '{print $2}'
}

#ip_to_dev() {
#  local ip=$1
#
#  for dev in $DEV_LIST; do
#    ip addr show dev $dev | grep inet | grep $ip &>> /dev/null
#    [ $? -eq 0 ] && echo $dev && return 0
#  done
#  return 1
#}

get_left_info() {
  leftpeer=`ip addr show dev $outIf | grep inet | grep brd | awk '{print $2}' | cut -d'/' -f1`
  leftpriv=`ip addr show dev $inIf | grep inet | grep brd | awk '{print $2}' | cut -d'/' -f1`
  leftnet=`ip route show | grep $inIf | head -1 | awk '{print $1}'`
  leftgw=`ip route show | grep $outIf | grep default | head -1 | awk '{print $3}'`
}

nonat_chain() {
  get_left_info
  outIp=$leftpeer
  if iptables -L VPN_$outIp -t mangle > /dev/null 2>&1 ; then
    VPNCHAIN="1"
  else
    # Create VPN_outIp chain and push all traffic through it to prevent NAT in the tunnel
    sudo iptables -N VPN_$outIp -t mangle
    sudo iptables -A FORWARD -t mangle -j VPN_$outIp
    sudo iptables -A OUTPUT -t mangle -j VPN_$outIp
    # Explicitly trust all ESP / VPN traffic
    sudo iptables -A PREROUTING -t mangle -d $outIp -p esp -j MARK --set-mark $vpninmark
    sudo iptables -A FORWARD -t filter -i $outIf -m mark --mark $vpninmark -j ACCEPT
    sudo iptables -A FORWARD -t filter -i $outIf -m mark --mark $vpnoutmark -j ACCEPT
    sudo /etc/init.d/ipsec start
  fi
}

ipsec_tunnel_del() {
  get_left_info
  outIp=$leftpeer
  local rightpeer=$1
  local rightnet=$2
  local op=$3
  local vpnconffile=$vpnconfdir/ipsec.vpn-$rightpeer.conf
  local vpnsecretsfile=$vpnconfdir/ipsec.vpn-$rightpeer.secrets
  logger -t cloud "$(basename $0): removing configuration for ipsec tunnel to $rightpeer"
  sudo rm -f $vpnconffile
  sudo rm -f $vpnsecretsfile
  sudo iptables $op VPN_$outIp -t mangle -o $outIf -d $rightnet -j MARK --set-mark $vpnoutmark
}

ipsec_tunnel_add() {
  get_left_info
  outIp=$leftpeer
  nonat_chain

  sudo mkdir -p $vpnconfdir
  local rightpeer=$1
  local rightpriv=$2
  local rightnet=$3
  local espcrypt=$4
  local esphash=$5
  local salife=$6
  local ikecrypt=$7
  local ikehash=$8
  local ikelife=$9
  local secret=${10}
  local vpnconffile=$vpnconfdir/ipsec.vpn-$rightpeer.conf
  local vpnsecretsfile=$vpnconfdir/ipsec.vpn-$rightpeer.secrets

  logger -t cloud "$(basename $0): creating configuration for ipsec tunnel: left peer=$leftpeer \
    right peer=$rightpeer right network=$rightnet phase1 encryption=$espcrypt phase1 hash=$esphash \
    phase2 encryption=$ikecrypt phase2 hash=$ikehash secret=$secret"

  [ "$op" == "-A" ] && ipsec_tunnel_del $rightpeer $rightnet "-D"
    sudo echo "conn vpn-$rightpeer" > $vpnconffile &&
    sudo echo "  left=$leftpeer" >> $vpnconffile &&
    sudo echo "  leftsubnet=$leftnet" >> $vpnconffile &&
    sudo echo "  leftnexthop=$leftgw" >> $vpnconffile &&
    sudo echo "  leftsourceip=$leftpriv" >> $vpnconffile &&
    sudo echo "  right=$rightpeer" >> $vpnconffile &&
    sudo echo "  rightsubnets={$rightnet}" >> $vpnconffile &&
    sudo echo "  rightsourceip=$rightpriv" >> $vpnconffile &&
    sudo echo "  type=tunnel" >> $vpnconffile &&
    sudo echo "  authby=secret" >> $vpnconffile &&
    sudo echo "  keyexchange=ike" >> $vpnconffile &&
    sudo echo "  pfs=no" >> $vpnconffile &&
    sudo echo "  esp=$espcrypt;$esphash" >> $vpnconffile &&
    sudo echo "  salifetime=${salife}s" >> $vpnconffile &&
    sudo echo "  ike=$ikecrypt;$ikehash" >> $vpnconffile &&
    sudo echo "  ikelifetime=${ikelife}s" >> $vpnconffile &&
    sudo echo "  auto=start" >> $vpnconffile &&
    sudo echo "$leftpeer $rightpeer: PSK $secret" > $vpnsecretsfile &&
    sudo chmod 0400 $vpnsecretsfile &&
    sudo iptables -A VPN_$outIp -t mangle -o $outIf -d $rightnet -j MARK --set-mark $vpnoutmark &&
    sudo /etc/init.d/ipsec reload
    # Prevent NAT on "marked" VPN traffic
    sudo iptables -D POSTROUTING -t nat -o $outIf -j SNAT --to-source $outIp
    sudo iptables -D POSTROUTING -t nat -o $outIf -m mark ! --mark $vpnoutmark -j SNAT --to-source $outIp
    sudo iptables -A POSTROUTING -t nat -o $outIf -m mark ! --mark $vpnoutmark -j SNAT --to-source $outIp

  result=$?
  logger -t cloud "$(basename $0): done ipsec tunnel entry for right peer=$rightpeer right network=$rightnet"
  return $result
}

rflag=
Rflag=
pflag=
eflag=
Eflag=
lflag=
iflag=
Iflag=
Lflag=
sflag=
op=""

while getopts 'ADr:R:p:e:E:l:i:I:L:s:' OPTION
do
  case $OPTION in
  A)    opflag=1
        op="-A"
        ;;
  D)    opflag=2
        op="-D"
        ;;
  r)    rflag=1
        rightpeer="$OPTARG"
        ;;
  R)    Rflag=1
        rightpriv="$OPTARG"
        ;;
  p)    pflag=1
        rightnet="$OPTARG"
        ;;
  e)    eflag=1
        espcrypt="$OPTARG"
        ;;
  E)    Eflag=1
        esphash="$OPTARG"
        ;;
  l)    lflag=1
        salife="$OPTARG"
        ;;
  i)    iflag=1
        ikecrypt="$OPTARG"
        ;;
  I)    Iflag=1
        ikehash="$OPTARG"
        ;;
  L)    Lflag=1
        ikelife="$OPTARG"
        ;;
  s)    sflag=1
        secret="$OPTARG"
        ;;
  ?)    usage
#       unlock_exit 2 $lock $locked
        ;;
  esac
done

DEV_LIST=$(get_dev_list)
OUTFILE=$(mktemp)

#Firewall ports for one-to-one/static NAT
if [ "$opflag" == "1" ]
then
    ipsec_tunnel_add $rightpeer $rightpriv $rightnet $espcrypt $esphash $salife $ikecrypt $ikehash $ikelife $secret
elif [ "$opflag" == "2" ]
then
    ipsec_tunnel_del $rightpeer $rightnet $op
else
    printf "Invalid action specified, must choose -A or -D to add/del tunnels\n" >&2
#    unlock_exit 5 $lock $locked
fi

#unlock_exit 0 $lock $locked
