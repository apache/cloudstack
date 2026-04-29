#!/bin/bash
## Licensed to the Apache Software Foundation (ASF) under one
## or more contributor license agreements.  See the NOTICE file
## distributed with this work for additional information
## regarding copyright ownership.  The ASF licenses this file
## to you under the Apache License, Version 2.0 (the
## "License"); you may not use this file except in compliance
## with the License.  You may obtain a copy of the License at
##
##   http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing,
## software distributed under the License is distributed on an
## "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
## KIND, either express or implied.  See the License for the
## specific language governing permissions and limitations
## under the License.

version=0.9.1
OPTIND=1
OPTIND=1
verb=0
logDir="/nsr/logs/cloudstack/"
clusterClient=""
networkerServer=""
hvVersion=""
libvVersion=""
apiVersion=""
logFile=""
destination=""
volume=""
ssid=""
newvolumeUuid=""
POOLPATH=""

log () {
     [[ "$verb" -eq 1 ]] && builtin echo "$@"
     if [[ "$1" == "-ne"  || "$1" == "-e" || "$1" == "-n" ]]; then
         builtin echo -e "$(date '+%Y-%m-%d %H-%M-%S>')" "${@: 2}" >> "$logFile"
     else
          builtin echo "$(date '+%Y-%m-%d %H-%M-%S>')" "$@" >> "$logFile"
     fi
}

vercomp() {
       local a b IFS=. -; set -f
       printf -v a %08d $1; printf -v b %08d $3
       test $a "$2" $b
}

usage() {
        echo "

Usage:[-v] [-h] [-l log_dir] [-s networker_server] [-c networker_cluster_client] [-S ssid] [-d networker_destination_client ] [ -a volume ] [ -p pool_local_path ]

Options:
        -h Help and usage
        -v Enable log mode
        -l log_dir. Specify log directory. Default is /nsr/logs/cloudstack
        -s networker_server  Specify the EMC Networker server we are going to use
        -c networker_cluster_client  Specify the EMC Networker client CLUSTER to use
        -d networker_destination_client Specify the EMC Networker client that is going to be used for the restore
        -a volume Specify volume to restore
        -p pool_local_path Local Path to Pool
        -S ssid Specify SSID
Supplements Apache Cloudstack B&R Framework  EMC Networker plugin and performs the backup of the Virtual Machines
"

}

sanity_checks() {
        log "Performing environment sanity checks..."

        log -ne "\t[1] Checking if Networker is installed\t"
        if [[ $(systemctl list-unit-files | grep networker) = *networker* ]]; then
                        log "Success"
        else
                        log "Failure"
                        log -e "\n\tNetworker Service NOT FOUND. Make sure that Networker client is properly installed"
                        exit 1
        fi

        log -ne "\t[2] Checking if Networker is running\t"
        if [[ $(systemctl is-active networker) = *active* ]]; then
                        log "Success"
        else
                        log "Failure"
                        log -e "\n\tNetworker Service is not running. Investigate Networker logs, startup server and try again"
                        exit 2
        fi
        log -ne "\t[3] Checking Networker DNS Resolution\t"
        if [[ $(getent hosts "$networkerServer") = *$networkerServer* ]]; then
                        log "Success"
        else
                        log "Failure"
                        log -e "\n\tNetworker Server cannot be resolved. Backups will most probably fail. Consider adding the ip/hostname to /etc/host or fix DNS resolution"
                        exit 3
        fi

        log -ne "\t[4] Checking QEMU / Libvirt Versions \t"
        hvVersion=$(virsh version | grep hypervisor | awk '{print $(NF)}')
        libvVersion=$(virsh version | grep libvirt | awk '{print $(NF)}' | tail -n 1)
        apiVersion=$(virsh version | grep API | awk '{print $(NF)}')
        if vercomp "$hvVersion" \> 2.1.2; then
                log -n "Success"
                log -ne "\t\t [ Libvirt: $libvVersion apiVersion: $apiVersion ]"
                echo
        else
                log "Failure"
                log -e "\n\tYour QEMU version $hvVersion is unsupported. Consider upgrading at least to latest QEMU at branch 2"
                exit 4
        fi
        log "Environment Sanity Checks successfully passed"
}
echo "
Cloudstack B&R Framework - EMC Networker backup script
Version $version
"
restore_all_volumes() {
        log "Preparing restore for SAVESET $ssid"
        cmd="$(sudo recover -s "$networkerServer" -S "$ssid" -iY)"
        retVal=$?
        log "$cmd"
        if [ "$retVal" -ne 0 ]; then
                log "Unable to restore SAVESET $ssid"
                exit 4
        else
                log "Restore of SAVESET $ssid has been completed"
        fi
}

restore_volume() {
        log "Preparning restore for volume $volume at destination as  $POOLPATH/$newvolumeUuid"
        cmd="$(recover -R "$destination" -c "$clusterClient" -s "$networkerServer" -a "$POOLPATH/$volume" -iR)"
        retVal=$?
        log "$cmd"
        if [ "$retVal" -ne 0 ]; then
                log "Unable to restore SAVESET $ssid"
                exit 5
        else
                log "Restore of SAVESET $ssid has been completed"
        fi

        if [ -f "$POOLPATH/$volume.R" ]; then
            mv "$POOLPATH/$volume.R" "$POOLPATH/$newvolumeUuid"
        else
            mv "$POOLPATH/$volume" "$POOLPATH/$newvolumeUuid"
        fi

        if [ -f "$POOLPATH/$newvolumeUuid" ]; then
                log "Volume restored under path/name: $POOLPATH/$newvolumeUuid"

        else
                log "Unable to verify final restored volume: $POOLPATH/$newvolumeUuid"
                exit 6
        fi

}

while getopts "h?vs:l:c:d:a:S:n:p:" opt; do
  case "$opt" in
    h|\?)
      usage
      exit 254
      ;;
    c) clusterClient="$OPTARG"
     ;;
    s) networkerServer="$OPTARG"
     ;;
    l) logDir="$OPTARG"
     ;;
    d) destination="$OPTARG"
     ;;
    a) volume="$OPTARG"
     ;;
    S) ssid="$OPTARG"
     ;;
    n) newvolumeUuid="$OPTARG"
     ;;
    p) POOLPATH="$OPTARG"
     ;;
    v)  verb=1
      ;;
  esac
done
shift $((OPTIND-1))

[ "${1:-}" = "--" ] && shift

if [[ -n "$newvolumeUuid" ]]; then
  if [[ -z "$networkerServer" || -z "$destination" || -z "$clusterClient" || -z "$newvolumeUuid" || -z "$POOLPATH" ]];  then
    usage
    exit 255
  fi
elif [[ -n "$ssid" ]]; then
   if [[ -z "$networkerServer" ]];  then
      usage
      exit 255
    fi
elif [[ -n "$ssid" && -n "$volumeUuid" ]]; then
      echo "You can either restore a whole saveset or part of it but not both."
      exit 250
else
  exit 255
fi

if [ ! -d "$logDir" ]; then
  mkdir -p "$logDir"
fi


if [[ -z "$ssid" && -n "$newvolumeUuid" ]]; then
        logFile="$logDir/RESTORE-$newvolumeUuid-$(date +'%Y_%m_%d_%I_%M_%p').log"
        # Perform Initial sanity checks
        sanity_checks
        restore_volume
elif [[ -n "$ssid" && -z "$newvolumeUuid" ]]; then
        logFile="$logDir/RESTORE-$ssid-$(date +'%Y_%m_%d_%I_%M_%p').log"
        # Perform Initial sanity checks
        sanity_checks
        restore_all_volumes
fi
exit 0
