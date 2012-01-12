#!/bin/bash
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

#set -x
 
usage() {
  printf "Usage: %s [uuid of this host] [interval in seconds]\n" $(basename $0)

}

if [ -z $1 ]; then
  usage
  exit 2
fi

if [ -z $2 ]; then
  usage
  exit 3
fi

if [ ! -f /opt/xensource/bin/xenheartbeat.sh ]; then
  printf "Error: Unable to find xenheartbeat.sh to launch\n"
  exit 4
fi

for psid in `ps -ef | grep xenheartbeat | grep -v grep | awk '{print $2}'`; do
  kill $psid
done

nohup /opt/xensource/bin/xenheartbeat.sh $1 $2 >/dev/null 2>/dev/null &
echo "======> DONE <======"
