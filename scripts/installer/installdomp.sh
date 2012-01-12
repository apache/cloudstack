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



 

# $Id: installdomp.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/installer/installdomp.sh $

#set -x
usage() {
 echo "Usage: $(basename $0) <templates base location>"
 echo "eg: $(basename $0) tank/volumes/demo/template"
}

fflag=

while getopts 'f' OPTION
do
  case $OPTION in
  f)	fflag=1
        ;;
  ?)	usage
		exit 2
		;;
  esac
done

shift $(($OPTIND - 1))

if [ $# -lt 1 ]
then
 usage
 exit 2
fi

tmpltfs=$1/private/u000000/os/consoleproxy
if [ "$fflag" == "1" ] 
then
  zfs destroy -r $tmpltfs 2> /dev/null
fi

snaps=$(zfs list -t snapshot -r $tmpltfs)
if [ $? -eq 0 -a "$snaps" != "no datasets available" ]
then
  echo "Warning: some snapshots already exist at target location $tmpltfs"
  echo "Use -f to delete these first"
  exit 2
fi

$(dirname $0)/createtmplt.sh  -t $tmpltfs -n consoleproxy -f /root/template/vmi-root-fc8-x86_64-domP.img.bz2 -s 3 -d consoleproxy -u

rm -f /$tmpltfs/consoleproxy.tar

exit 0
