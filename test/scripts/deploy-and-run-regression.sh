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



 


set -e
set -x


# $0: the name of myself
# $1: the dest host where we will run eeeeeeverything
# $2: the source dir to copy files from, on this machine
# $3: the dest dir in the dest machine where everything will be run

# "${build.schedule.workingDir}/branches/GA-1.0.0-09-11-15/java/test/scripts/deploy-and-run-regression.sh" root@192.168.0.3 "${build.artifactsDir}"  /root/artifacts

dir=`dirname $0`
r=bootstrap-regression.sh

host="$1"
src="$2"
dest="$3"

srcbootstrapper="$dir"/"$r" # that script is launched from this one, remotely via ssh
destbootstrapper="$dest"/"$r" # this is the path on the remote machine


ssh -o BatchMode\ yes $host rm -rf $dest
scp -o BatchMode\ yes -r $src $host:$dest
scp -o BatchMode\ yes -r "$srcbootstrapper" $host:$destbootstrapper

ssh -o BatchMode\ yes $host $destbootstrapper

