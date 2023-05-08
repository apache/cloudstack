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
backupfolder=/var/cache/cloud/bkpup_live_patch
logfile="/var/log/livepatchsystemvm.log"
newpath="/var/cache/cloud/"
CMDLINE=/var/cache/cloud/cmdline
md5file=/var/cache/cloud/cloud-scripts-signature
svcfile=/var/cache/cloud/enabled_svcs
TYPE=$(grep -Po 'type=\K[a-zA-Z]*' $CMDLINE)
patchfailed=0
backuprestored=0

backup_old_package() {
  mkdir -p $backupfolder
  if [ -d /usr/local/cloud/systemvm/conf/ ]; then
    echo "Backing up keystore file and certificates" > $logfile 2>&1
    mkdir -p $backupfolder/conf
    cp -r /usr/local/cloud/systemvm/conf/* $backupfolder/conf
  fi
  if [ -d /usr/local/cloud/systemvm/ ]; then
    echo "Backing up agent package" >> $logfile 2>&1
    cd /usr/local/cloud/systemvm/
    zip -r $backupfolder/agent.zip * >> $logfile 2>&1 2>&1
    cd -
  fi
  cp $md5file $backupfolder
  echo "Backing up cloud-scripts file" >> $logfile 2>&1
  tar -zcvf $backupfolder/cloud-scripts.tgz /etc/ /var/ /opt/ /root/  >> $logfile 2>&1
}

restore_backup() {
  echo "Restoring cloud scripts" >> $logfile 2>&1
  tar -xvf $backupfolder/cloud-scripts.tar -C / >> $logfile 2>&1
  echo "Restoring agent package" >> $logfile 2>&1
  if [ -f $backupfolder/agent.zip ]; then
    unzip $backupfolder/agent.zip -d /usr/local/cloud/systemvm/ >> $logfile 2>&1
    echo "Restore keystore file and certificates" >> $logfile 2>&1
    mkdir -p "/usr/local/cloud/systemvm/conf/"
    cp -r $backupfolder/conf/* /usr/local/cloud/systemvm/conf/
  fi
  backuprestored=1
  restart_services
  cp $backupfolder/cloud-scripts-signature $md5file
}

update_checksum() {
  newmd5=$(md5sum $1 | awk '{print $1}')
  echo "checksum: " ${newmd5} >> $logfile 2>&1
  echo ${newmd5} > ${md5file}
}

restart_services() {
  systemctl daemon-reload
  while IFS= read -r line
    do
      for svc in ${line}; do
        systemctl is-active --quiet "$svc"
        if [ $? -eq 0 ]; then
          systemctl restart "$svc"
          systemctl is-active --quiet "$svc"
          if [ $? -gt 0 ]; then
            echo "Failed to start "$svc" service. Patch Failed. Retrying again" >> $logfile 2>&1
            if [ $backuprestored == 0 ]; then
              restore_backup
            fi
            patchfailed=1
            break
          fi
        fi
      done	
      if [ $patchfailed == 1 ]; then
        return
      fi
    done < "$svcfile"
    if [ "$TYPE" == "consoleproxy" ]; then
      vncport=8080
      if [ -f /root/vncport ]
      then
        vncport=`cat /root/vncport`
        log_it "vncport read: ${vncport}"
      fi
      iptables -A INPUT -i eth2 -p tcp -m state --state NEW -m tcp --dport $vncport -j ACCEPT
    fi
}

cleanup_systemVM() {
  rm -rf $backupfolder
  mv "$newpath"cloud-scripts.tgz /usr/share/cloud/cloud-scripts.tgz
  rm -rf "$newpath""agent.zip" "$newpath""patch-sysvms.sh"
}

patch_systemvm() {
  rm -rf /usr/local/cloud/systemvm

  if [ "$TYPE" == "consoleproxy" ] || [ "$TYPE" == "secstorage" ]; then
    echo "All" | unzip $newpath/agent.zip -d /usr/local/cloud/systemvm >> $logfile 2>&1
    mkdir -p /usr/local/cloud/systemvm
    find /usr/local/cloud/systemvm/ -name \*.sh | xargs chmod 555
  fi
  echo "Extracting cloud scripts" >> $logfile 2>&1
  tar -xvf $newpath/cloud-scripts.tgz -C / >> $logfile 2>&1

  if [ -f $backupfolder/conf/cloud.jks ]; then
    cp -r $backupfolder/conf/* /usr/local/cloud/systemvm/conf/
    echo "Restored keystore file and certs using backup" >> $logfile 2>&1
  fi

  update_checksum $newpath/cloud-scripts.tgz

  if [ "$TYPE" == "consoleproxy" ] || [ "$TYPE" == "secstorage" ] || [[ "$TYPE" == *router ]]; then
    restart_services
  fi
}


backup_old_package
patch_systemvm
cleanup_systemVM

if [ $patchfailed == 0 ]; then
  echo "version:$(cat ${md5file}) "
fi

exit $patchfailed
