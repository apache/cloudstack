#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.




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





