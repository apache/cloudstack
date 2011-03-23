#!/bin/sh
# Version @VERSION@

set -e

usage() {
  echo "$0 [-g |-d] <ip address>"
  echo " -g  output the gateway for this ip address"
  exit 1
}

gflag=

while getopts 'g' OPTION
do
  case $OPTION in
  g)    gflag=1
         ;;
  ?)    usage
         exit 1
         ;;
  esac
done

if [ "$gflag" != "1" ]
then
  usage
fi

shift $(($OPTIND - 1))
ipaddr=$1

[ -z "$ipaddr" ] && usage

device=$(ip addr | grep $1 | head -1 | awk '{print $NF}')
defaultdev=$(ip route | grep default | awk '{print $NF}')
if [ "$device" == "$defaultdev" ]
then
  gateway=$(ip route | grep default | awk '{print $3}')
fi

[ -n "$gflag" ] && echo $gateway && exit 0

