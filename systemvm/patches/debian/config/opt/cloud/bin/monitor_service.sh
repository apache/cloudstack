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

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

set -x
usage() {
  printf "Usage: %s: -c config string \n" $(basename $0) >&2
}

configFile='/etc/monitor.conf'

create_config() {
services=$1;
services_list=$(echo $services | cut -d, -f1- --output-delimiter=" ");

echo "#Monitor services config" >$configFile

for s in $services_list
do
service=$(echo $s | cut -d: -f1);
processname=$(echo $s | cut -d: -f2);
service_name=$(echo $s | cut -d: -f3);
pidfile=$(echo $s | cut -d: -f4);

echo "$service" >> $configFile;
echo $processname >> $configFile
echo $service_name >> $configFile
echo $pidfile >> $configFile



done

}

config=$2
if [ -n "$3" ]
then

#delete cron job before updating config file
crontab -l|grep "monitorServices.py"

if [ $? -eq 0 ]
then
    t=`date +%s`;
    touch /tmp/monitor-$t.txt;
    conf=/tmp/monitor-$t.txt
    crontab -l >$conf
    sed -i /#monitoringConfig/,+3d $conf
    crontab $conf
    rm $conf
fi


logger -t cloud "deleted crontab entry for monitoring services"
unlock_exit 0 $lock $locked
fi

create_config $config

#add cron job
crontab -l|grep "monitorServices.py"
if [ $? -ne 0 ]
   then
      (crontab -l ;echo -e "#monitoringConfig\nSHELL=/bin/bash\nPATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin\n */3 * * * * /usr/bin/python /root/monitorServices.py") | crontab -
      logger -t cloud "added crontab entry for monitoring services"
fi


unlock_exit 0 $lock $locked

