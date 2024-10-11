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
verb=0
logDir="/nsr/logs/cloudstack/"
snapPrefix="CSBKP_"$RANDOM"_"
clusterClient=""
networkerServer=""
hvVersion=""
libvVersion=""
apiVersion=""
kvmDName=""
kvmDUuid=""
logFile=""
mediaPool=""
retentionTime=""

log () {

    [[ "$verb" -eq 1 ]] && builtin echo "$@"
    if [[ "$1" == "-ne"  || "$1" == "-e" || "$1" == "-n" ]]; then
          builtin echo -e "$(date '+%Y-%m-%d %H-%M-%S>')" "${@: 2}" >> "$logFile"
    else
          builtin echo "$(date '+%Y-%m-%d %H-%M-%S>')" "$@" >> "$logFile"
    fi
}

vercomp(){
   local a b IFS=. -; set -f
   printf -v a %08d $1; printf -v b %08d $3
   test $a "$2" $b
}

usage() {
        echo "

Usage:[-v] [-h] [-l log_dir] [-dr] [-s networker_server] [-c networker_cluster_client] [-t target_vm] [-u target_uuid] [-p snapprefix] [-P media_pool ] [-R retention_time ]

Options:
        -h Help and usage
        -v Enable verbose mode
        -l log_dir. Specify log directory. Default is /nsr/logs/cloudstack
        -s networker_server  Specifiy the EMC Networker server we are going to use
        -c networker_cluster_client  Specify the EMC Networker client CLUSTER to use
        -t target_vm KVM domain to backup
        -u target_uuid KVM domain to backup
        -p Snapshot Prefix for backups
        -P mediaPool EMC Networker Media Pool
        -R retention_time Backup retention time
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

        log -ne "\t[4] Checking Permissions \t\t"
        if groups $USER | grep -q '\blibvirt\b'; then
            log -n "Success"
            log -ne "\t\t User $USER is part of libvirt group"
            echo
        else
            log "Failure - User $USER is not part of libvirt group"
            exit 6
        fi
        log "Environment Sanity Checks successfully passed"
}



echo "
Cloudstack B&R Framework - EMC Networker backup script
Version $version
"

backup_domain() {

        name=$1
        snapName=$2
        log  "Preparing snapshots and gathering information for backing up domain $name under snapshot name $snapName"
        log  "Retention time is $retentionTime"

        declare -A TRGSRC
        while IFS=',' read -r TARGET SOURCE
        do
                if [[ $SOURCE != "-" ]]; then
                        TRGSRC+=(["$TARGET"]="$SOURCE")
                fi
        done < <(virsh -c qemu:///system domblklist "$name" --details | grep file | grep -v 'cdrom' | grep -v 'floppy' | sed 's/  */,/g' |  cut -d',' -f 4-)
        diskspec=""
        for target in "${!TRGSRC[@]}"; do
          log -e "\tDisk for $target is at ${TRGSRC[${target}]}"
          diskspec="$diskspec --diskspec $target,snapshot=external"
          disks="$disks ${TRGSRC[${target}]} "
        done

        cmd="$(virsh -c qemu:///system snapshot-create-as --domain "$name" --name "$snapName" --no-metadata --atomic --quiesce --disk-only "$diskspec")"
        retVal=$?
        log "$cmd"
        if [ "$retVal" -ne 0 ]; then
                log "Agent not responding, trying to snapshot directly"
                cmd="$(virsh -c qemu:///system snapshot-create-as --domain "$name" --name "$snapName" --no-metadata --atomic --disk-only "$diskspec")"
                retVal=$?
                if [ "$retVal" -ne 0 ]; then
                        log "Failed to create snapshot for $name"
                        exit 7
                fi
                log "Created snapshot(s) for $name"
        fi
        cmd="$(save -LL -q -e "${retentionTime}" -s "$networkerServer" -c "$clusterClient" -N "$name" -b "$mediaPool" $disks)"
        retVal=$?
        log "$cmd"
        echo "$cmd" | grep -oE 'savetime=[0-9]{10}'
        if [ $retVal -ne 0 ]; then
                log "Unable to backup $disks for $name"
        else
                log "Backup $disks for $name completed!!"
        fi

        #Merge changes and conclude
        SNAPSHOTS="$(virsh -c qemu:///system domblklist "$name" --details | grep file | grep -v 'cdrom' | grep -v 'floppy' | awk '{print $4}')"
        for target in "${!TRGSRC[@]}"; do
                log "Merging Snasphots for $target"
                cmd="$(virsh -c qemu:///system blockcommit "$name" "$target" --active --pivot)"
                retVal=$?
                log "$cmd"
                if [ $retVal -ne 0 ]; then
                        log "Unable to merge disk %target changes for domain $name"
                        exit 8
                fi
        done
        #Clean snapshots
        for snapshot in $SNAPSHOTS; do
             log "Deleting Snapshot $snapshot"
             cmd=$(rm -f "$snapshot")
             retVal=$?
             log "$cmd"
             if [ $retVal -ne 0 ]; then
                     log "Unable to delete snapshot $snapshot"
                     exit 8
             fi
             log "Deleted Snapshot: $snapshot"
         done
}

while getopts "h?vs:l:c:t:u:p:P:R:" opt; do
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
     t) kvmDName="$OPTARG"
      ;;
     u) kvmDUuid="$OPTARG"
      ;;
     p) snapPrefix="$OPTARG"
      ;;
     P) mediaPool="$OPTARG"
          ;;
     R) retentionTime="$OPTARG"
       ;;
     v)  verb=1
       ;;
   esac
 done

 shift $((OPTIND-1))

 [ "${1:-}" = "--" ] && shift
 if [[ -z "$networkerServer" || -z "$kvmDName" || -z "$clusterClient" || -z "$kvmDUuid" ]];  then
     usage
     exit 255
 fi

 if [ ! -d "$logDir" ]; then
   mkdir -p "$logDir"
 fi

 logFile="$logDir/BACKUP-$kvmDName-$(date +'%Y_%m_%d_%I_%M_%p').log"

 # Perform Initial sanity checks
 sanity_checks

 log -e "\nLooking for domain $kvmDName with UUID $kvmDUuid"
 if [[ "$kvmDName" == $(virsh -c qemu:///system domname "$kvmDUuid" | head -1) && "$kvmDUuid" == $(virsh -c qemu:///system domuuid "$kvmDName" | head -1) ]]; then
         log "Domain found...."
 else
         log "Domain not found on this host. Aborting....."
         log "Check for the location of the Instance in the cloudstack management console"
         exit 5
 fi

 backup_domain "$kvmDName" "$snapPrefix$kvmDName"

 exit 0
