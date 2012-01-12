#!/usr/bin/env bash
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



 

# $Id: checkchildren.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/checkchildren.sh $
# checkchdilren.sh -- Does this path has children?

usage() {
  printf "Usage:  %s path \n" $(basename $0) >&2
}

if [ $# -ne 1 ]
then
  usage
  exit 1
fi

#set -x

fs=$1
if [ "${fs:0:1}" != "/" ]
then
  fs="/"$fs
fi

if [ -d $fs ]
then
  if [ `ls -l $fs | grep -v total | wc -l | awk '{print $1}'` -eq 0 ]
  then
    exit 0
  else
    exit 1
  fi
fi

exit 0
