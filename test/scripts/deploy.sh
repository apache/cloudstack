#!/bin/bash
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



 


. /etc/rc.d/init.d/functions

set -x

usage() {
  printf "Usage: %s: [-d] [-r] \n" $(basename $0) >&2
}

deploy_server() {
  ssh root@$1 "yum remove -y \"cloud*\" ; yum clean all ; cd /usr/share/cloud/ && rm -rf * && rm -rf /var/cache/cloud ; yum install -y cloud-client cloud-premium cloud-usage"
  ssh root@$1 "cloud-setup-databases alena@$2 xenserver"
  ssh root@$1 "cd /usr/share/cloud/management/conf; sed '/db.cloud.url.params/ d' db.properties > db.properties1; dos2unix db.properties1; mv -f db.properties1 db.properties"
  ssh root@$1 "cd /usr/share/cloud/management/conf; echo \db.cloud.url.params=includeInnodbStatusInDeadlockExceptions=true\&logSlowQueries=true\&prepStmtCacheSize=517\&cachePrepStmts=true >> db.properties"
  if [ $? -gt 0 ]; then echo "failed to install on $1"; return 2; fi
 echo "Management server is deployed successfully"
}

deploy_db() {
  echo "Deploying database on $1"
 # ssh root@$1 "cp $CONFIGDIR/conf/templates.sql /usr/share/cloud/setup/templates.xenserver.sql "
ssh root@$1 "cloud-setup-databases alena@$1 --deploy-as=root xenserver"
  if [ $? -gt 0 ]; then echo "failed to deploy db on $1"; return 2; fi
  echo "Database is deployed successfully"
}

run_server() {
  ssh root@$1 "service cloud-management start; service cloud-usage start 2>&1 &"
}

stop_server() {
  ssh root@$1 "service cloud-management stop; service cloud-usage stop 2>&1 &"
}

dir=$(dirname "$0")
if [ -f $dir/../deploy.properties ]; then
  . "$dir/../deploy.properties"
fi

if [ "$USER" == "" ]; then
  printf "ERROR: Need tospecify the user\n"
  exit 4
fi

if [ "$SERVER" == "" ]; then
  printf "ERROR: Need to specify the management server\n"
  exit 1
fi

installflag=
deployflag=
runflag=
killflag=
setupflag=
mgmtIp=
mode=expert
distdir=dist

while getopts 'idkrsb:D' OPTION
do
  case $OPTION in
  i)    installflag=1
        ;;
  d)    deployflag=1
        ;;
  k)    killflag=1
        ;;
  r)    runflag=1
        ;;
  b)    distdir="$OPTARG"
        ;;
  s)    setupflag=1
        mode=setup
        ;;
  D)    developer=1; distdir=dist/developer;;

  ?)    usage
  esac
done


if [ "$setupflag$deployflag$installflag$killflag$runflag" == "" ]
then
  usage
  exit
fi


if [ "$installflag" == "1" ]
then
    echo "Deploying server...."

    for i in $SERVER
    do
      echo -n "$i: "
      deploy_server $i $DB
      if [ $? -gt 0 ]; then  failure ; else success;fi
      echo
    done
fi

if [ "$deployflag" == "1" ]
then
    echo "Deploying database..."
    for i in $DB
    do
      echo -n "$i: "
      deploy_db $i
      if [ $? -gt 0 ]; then  failure ; else success;fi
      echo
    done
fi


if [ "$runflag" == "1" ]
then
   echo "Starting Management server"
   for i in $SERVER
   do
      run_server $i
      sleep 30
   done


echo "Setting up configuration values..."
    for i in $LB
   do
      DST='../src/'
      java -cp ${DST}commons-httpclient-3.1.jar:${DST}mysql-connector-java-5.1.7-bin.jar:${DST}commons-logging-1.1.1.jar:${DST}commons-codec-1.4.jar:${DST}cloud-test.jar:${DST}log4j-1.2.15.jar:${DST}trilead-ssh2-build213.jar:${DST}cloud-utils.jar:.././conf com.cloud.test.regression.Deploy -h $i -f ../conf/deploy.xml
      echo "Restarting Management server to apply configuration values"
   done

echo "Restarting management servers"
  for i in $SERVER
   do
      echo "Restarting Management server to apply configuration values"
      stop_server $i
      run_server $i
      sleep 60
   done


echo "Adding secondary/primary storage/hosts..."
    for i in $LB
   do
      DST='../src/'
      java -cp ${DST}commons-httpclient-3.1.jar:${DST}mysql-connector-java-5.1.7-bin.jar:${DST}commons-logging-1.1.1.jar:${DST}commons-codec-1.4.jar:${DST}cloud-test.jar:${DST}log4j-1.2.15.jar:${DST}trilead-ssh2-build213.jar:${DST}cloud-utils.jar:.././conf com.cloud.test.regression.Deploy -h $i -f ../conf/config.xml
   done

fi
