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



 

# Deploy console proxy package to an existing VM template
usage() {
  printf "Usage: %s: -d [work directory to deploy to] -z [zip file]" $(basename $0) >&2
}

deploydir=
zipfile=

#set -x

while getopts 'd:z:' OPTION
do
  case "$OPTION" in
  d)	deploydir="$OPTARG"
		;;
  z)    zipfile="$OPTARG"
                ;;
  ?)	usage
		exit 2
		;;
  esac
done

printf "NOTE: You must have root privileges to install and run this program.\n"

if [ "$deploydir" == "" ]; then
  printf "ERROR: Unable to find deployment work directory $deploydir\n"
  exit 3; 	 
fi
if [ ! -f $deploydir/consoleproxy.tar.gz ]
then
  printf "ERROR: Unable to find existing console proxy template file (consoleproxy.tar.gz) to work on at $deploydir\n"
  exit 5
fi

if [ "$zipfile" == "" ]; then 
    zipfile="console-proxy.zip"
fi

if ! mkdir -p /mnt/consoleproxy
then
  printf "ERROR: Unable to create /mnt/consoleproxy for mounting template image\n"
  exit 5
fi

tar xvfz $deploydir/consoleproxy.tar.gz -C $deploydir
mount -o loop $deploydir/vmi-root-fc8-x86_64-domP /mnt/consoleproxy

if ! unzip -o $zipfile -d /mnt/consoleproxy/usr/local/vmops/consoleproxy
then
  printf "ERROR: Unable to unzip $zipfile to $deploydir\n"
  exit 6
fi

umount /mnt/consoleproxy

pushd $deploydir
tar cvf consoleproxy.tar vmi-root-fc8-x86_64-domP

mv -f consoleproxy.tar.gz consoleproxy.tar.gz.old 
gzip consoleproxy.tar
popd

if [ ! -f $deploydir/consoleproxy.tar.gz ]
then
	mv consoleproxy.tar.gz.old consoleproxy.tar.gz
  	printf "ERROR: failed to deploy and recreate the template at $deploydir\n"
fi

printf "SUCCESS: Installation is now complete. please go to $deploydir to review it\n"
exit 0
