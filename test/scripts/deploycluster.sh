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




set -x

usage() {
  printf "Usage: %s: [-f] [-d] [-i] \n" $(basename $0) >&2
}


path=
db=
ips=

while getopts 'p:d:i:' OPTION
do
  case $OPTION in
  p)    path="$OPTARG"
        ;;
  d)    db="$OPTARG"
        ;;
  i)    ips="$OPTARG"
        ;;

  ?)    usage
  esac
done


deploy_server() {
  echo "Deploying management server software on remote machine $1"

  rsync -avzh -e ssh $path/cloudstack-oss root@$1:/root/
  if [ $? -gt 0 ]; then echo "failed to rsync software between $path/cloudstack-oss and and remote machine $1"; return 2; fi

  ssh root@$1 "cd /root/cloudstack-oss && ant clean build-all deploy-server"
  if [ $? -gt 0 ]; then echo "failed to deploy cluster on $1"; return 2; fi


  ssh root@$1 "dir=\`cat ~/.bashrc  | grep 'export CATALINA_HOME' | awk -F '=' '{ print \$2}'\` && file=\$dir/conf/db.properties && sed '/cluster.node.IP/ d' \$file > db.properties1; dos2unix db.properties1; mv -f db.properties1 \$file; echo \cluster.node.IP=$1 >> \$file && file=\$dir/conf/db.properties && sed '/db.cloud.host/ d' \$file > db.properties1; dos2unix db.properties1; mv -f db.properties1 \$file; echo \db.cloud.host=$db >> \$file && file=\$dir/conf/db.properties && sed '/db.usage.host/ d' \$file > db.properties1; dos2unix db.properties1; mv -f db.properties1 \$file; echo \db.usage.host=$db >> \$file"

if [ $? -gt 0 ]; then echo "failed to setup db.properties file on remote $1"; return 2; fi

#ssh root@$1 "cd /root/cloudstack-oss && nohup ant run &"
  #if [ $? -gt 0 ]; then echo "failed to start the softare on remote $1"; return 2; fi

  echo "Remote management server is deployed as a part of cluster setup; you have to start it manually by logging in remotely"
}

export IFS=","
for ip in $ips; do
  echo "$ip"
  deploy_server $ip
done





