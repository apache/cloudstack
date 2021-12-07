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

PATH="/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin"
backupfolder=/tmp/bkpup_live_patch
logfile="/var/log/livepatchsystemvm.log"
newpath="/home/cloud/"
CMDLINE=/var/cache/cloud/cmdline
md5file=/var/cache/cloud/cloud-scripts-signature
svcfile=/var/cache/cloud/enabled_svcs
TYPE=$(grep -Po 'type=\K[a-zA-Z]*' $CMDLINE)
patchfailed=0


backup_old_package() {
  mkdir -p $backupfolder
  echo "Backing up keystore file and certificates" > $logfile
  mkdir -p $backupfolder/conf
  cp -r /usr/local/cloud/systemvm/conf/* $backupfolder/conf
  echo "Backing up agent package" >> $logfile
  zip -r $backupfolder/agent.zip /usr/local/cloud/systemvm/* >> $logfile 2>&1
  cp $md5file $backupfolder
  echo "Backing up cloud-scripts file" >> $logfile
  tar -zcvf $backupfolder/cloud-scripts.tgz /etc/ /var/ /opt/ /root/  >> $logfile 2>&1
}

restore_backup() {
  echo "Restoring cloud scripts" >> $logfile
  tar -xvf $backupfolder/cloud-scripts.tar -C / >> $logfile 2>&1
  echo "Restoring agent package" >> $logfile
  unzip $backupfolder/agent.zip -d /usr/local/cloud/systemvm/ >> $logfile 2>&1
  echo "Restore keystore file and certificates"
  mkdir -p "/usr/local/cloud/systemvm/conf/"
  cp -r $backupfolder/conf/* /usr/local/cloud/systemvm/conf/
  restart_services
  cp $backupfolder/cloud-scripts-signature $md5file
}

update_checksum() {
  newmd5=$(md5sum $1 | awk '{print $1}')
  echo "checksum: " ${newmd5} >> $logfile
  echo ${newmd5} > ${md5file}
}

restart_services() {
  systemctl daemon-reload
  while IFS= read -r line
    do
      echo "$line"
      systemctl restart "$line"
      sleep 5
      systemctl is-active --quiet "$line"
      if [ $? -gt 0 ]; then
        echo "Failed to start "$line" service. Patch Failed. Restoring backup" >> $logfile
        restore_backup
        patchfailed=1
        break
      fi
    done < "$svcfile"
}

cleanup_systemVM() {
  rm -rf $backupfolder
  rm -rf "$newpath""cloud-scripts.tgz" "$newpath""agent.zip" "$newpath""patch-sysvms.sh"
}

patch_systemvm() {
  rm -rf /usr/local/cloud/systemvm
  mkdir -p /usr/local/cloud/systemvm
  echo "All" | unzip $newpath/agent.zip -d /usr/local/cloud/systemvm >> $logfile 2>&1
  find /usr/local/cloud/systemvm/ -name \*.sh | xargs chmod 555

  echo "Extracting cloud scripts" >> $logfile
  tar -xvf $newpath/cloud-scripts.tgz -C / >> $logfile 2>&1

  if [ -f $backupfolder/conf/cloud.jks ]; then
    cp -r $backupfolder/conf/* /usr/local/cloud/systemvm/conf/
    echo "Restored keystore file and certs using backup" >> $logfile
  fi

  update_checksum $newpath/cloud-scripts.tgz

  if [ "$TYPE" == "consoleproxy" ] || [ "$TYPE" == "secstorage" ] || [[ "$TYPE" == *router ]]; then
    restart_services
  fi
}


backup_old_package
patch_systemvm
cleanup_systemVM

exit $patchfailed
