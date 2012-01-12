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
  printf "Usage: %s [name label]  \n" $(basename $0) 
}

if [ -z $1 ]; then
  usage
  echo "3#no namelabel"
  exit 0
else
  namelabel=$1
fi

pid=`ps -ef | grep "dd" | grep $namelabel | grep -v "grep" | awk '{print $2}'`
if [ -z $pid ]; then
  echo "true"
  exit 0
fi 

kill -9 $pid
if [ $? -ne 0 ]; then
  echo "false"
  exit 0
fi
echo "true"
exit 0
