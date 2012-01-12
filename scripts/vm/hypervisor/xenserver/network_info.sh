#!/bin/sh
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

