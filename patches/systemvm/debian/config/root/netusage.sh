#!/usr/bin/env bash
# netusage.sh -- create iptable rules to gather network stats, running within DomR
#
usage() {
  printf "Usage: %s -[c|g|r] [-[a|d] <public interface>]\n" $(basename $0)  >&2
}

create_usage_rules () {
  iptables -N NETWORK_STATS > /dev/null
  iptables -I FORWARD -j NETWORK_STATS > /dev/null
  iptables -I INPUT -j NETWORK_STATS > /dev/null
  iptables -I OUTPUT -j NETWORK_STATS > /dev/null
  iptables -A NETWORK_STATS -i eth0 -o eth2 > /dev/null
  iptables -A NETWORK_STATS -i eth2 -o eth0 > /dev/null
  iptables -A NETWORK_STATS -o eth2 ! -i eth0 -p tcp > /dev/null
  iptables -A NETWORK_STATS -i eth2 ! -o eth0 -p tcp > /dev/null
  return $?
}

add_public_interface () {
  local pubIf=$1
  iptables -A NETWORK_STATS -i eth0 -o $pubIf > /dev/null
  iptables -A NETWORK_STATS -i $pubIf -o eth0 > /dev/null
  iptables -A NETWORK_STATS -o $pubIf ! -i eth0 -p tcp > /dev/null
  iptables -A NETWORK_STATS -i $pubIf ! -o eth0 -p tcp > /dev/null
  return $?
}

delete_public_interface () {
  local pubIf=$1
  echo $pubIf >> /root/removedVifs
  return $?
}

get_usage () {
  iptables -L NETWORK_STATS -n -v -x | awk '$1 ~ /^[0-9]+$/ { printf "%s:", $2}'; /root/clearUsageRules.sh > /dev/null
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     printf $?
     return 1
  fi
}

reset_usage () {
  iptables -Z NETWORK_STATS > /dev/null
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     return 1
  fi
}

#set -x

cflag=
gflag=
rflag=
iflag=
aflag=
dflag=

while getopts 'cgra:d:' OPTION
do
  case $OPTION in
  c)	cflag=1
		;;
  g)	gflag=1
		;;
  r)	rflag=1
		;;
  a)    aflag=1
        publicIf="$OPTARG"
        ;;
  d)    dflag=1
        publicIf="$OPTARG"
        ;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$cflag" == "1" ] 
then
  create_usage_rules  
  exit $?
fi

if [ "$gflag" == "1" ] 
then
  get_usage 
  exit $?
fi

if [ "$rflag" == "1" ] 
then
  reset_usage  
  exit $?
fi

if [ "$aflag" == "1" ] 
then
  add_public_interface $publicIf 
  exit $?
fi

if [ "$dflag" == "1" ] 
then
  delete_public_interface $publicIf 
  exit $?
fi

exit 0

