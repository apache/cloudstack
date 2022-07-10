#!/bin/bash
version=0.7.4


OPTIND=1

dryRun=0
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

verbose () {
        [[ "$verb" ]] && builtin echo $@
}

log () {
         builtin echo $(date "+%Y-%m-%d %H-%M-%S: ") $@ >> $logFile
         builtin echo $@
}

vercomp(){
   local a b IFS=. -; set -f
   printf -v a %08d $1; printf -v b %08d $3
   test $a "$2" $b
}


usage() {
        echo "

Usage:[-v] [-h] [-l log_dir] [-s networker_server] [-c networker_cluster_client] [-S ssid] [-d networker_destination_client ] [ -a volume ] [ -p pool_local_path ]

Options:
        -h Help and usage
        -v Enable verbose mode
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
        echo "Performing environment sanity checks..."

        verbose -ne "\t[1] Checking if Networker is installed\t"
        if [[ $(systemctl list-unit-files | grep networker) = *networker* ]]; then
                        verbose "Success"
        else
                        verbose "Failure"
                        log -e "\n\tNetworker Service NOT FOUND. Make sure that Networker client is properly installed"
                        exit -1
        fi

        verbose -ne "\t[2] Checking if Networker is running\t"
        if [[ $(systemctl is-active networker) = *active* ]]; then
                        verbose "Success"
        else
                        verbose "Failure"
                        log -e "\n\tNetworker Service is not running. Investigate Networker logs, startup server and try again"
                        exit -2
        fi
        verbose -ne "\t[3] Checking Networker DNS Resolution\t"
        if [[ $(getent hosts $networkerServer) = *$networkerServer* ]]; then
                        verbose "Success"
        else
                        verbose "Failure"
                        log -e "\n\tNetworker Server cannot be resolved. Backups will most probably fail. Consider adding the ip/hostname to /etc/host or fix DNS resolution"
                        exit -3
        fi

        verbose -ne "\t[4] Checking QEMU / Libvirt Versions \t"
        hvVersion=$(virsh version | grep hypervisor | awk '{print $(NF)}')
        libvVersion=$(virsh version | grep libvirt | awk '{print $(NF)}')
        apiVersion=$(virsh version | grep API | awk '{print $(NF)}')
        if vercomp $hvVersion \> 2.1.2; then
                verbose "Success"
        else
                verbose "Failure"
                log -e "\n\tYour QEMU version $hvVersion is unsupported. Consider upgrading at least to latest QEMU at branch 2"
                exit -4
        fi


        log "Environment Sanity Checks successfully passed"
}



echo "
Cloudstack B&R Framework - EMC Networker backup script
Version $version
"
restore_all_volumes() {
        log "Preparing restore for SAVESET $ssid"
        recover -s $networkerServer -S $ssid -iY
        if [ $? -ne 0 ]; then
                log "Unable to restore SAVESET $ssid"
                exit -4
        else
                log "Restore of SAVESET $ssid has been completed"
        fi
}
restore_volume() {
        log "Preparning restore for volume $volume at destination as  $POOLPATH/$newvolumeUuid"
        echo "recover -R $destination -c $clusterClient -s $networkerServer -a $POOLPATH $volume -iR"
        recover -R $destination -c $clusterClient -s $networkerServer -a $POOLPATH/$volume -iR >> $logFile
        if [ $? -ne 0 ]; then
                log "Unable to restore SAVESET $ssid"
                exit -5
        else
                log "Restore of SAVESET $ssid has been completed"
        fi

        mv $POOLPATH/$volume.R $POOLPATH/$newvolumeUuid

        if [ -f "$POOLPATH/$newvolumeUuid" ]; then
                log "Volume restored under path/name: $POOLPATH/$newvolumeUuid"

        else
                log "Unable to veirfy final restored volume: $POOLPATH/$newvolumeUui"
                exit -6
        fi

}

while getopts "h?vs:l:c:d:a:S:n:p:" opt; do
  case "$opt" in
    h|\?)
      usage
      exit 0
      ;;
    c) clusterClient=$OPTARG
     ;;
    s) networkerServer=$OPTARG
     ;;
    l) logDir=$OPTARG
       mkdir -p $logDir
     ;;
    d) destination=$OPTARG
     ;;
    a) volume=$OPTARG
     ;;
    S) ssid=$OPTARG
     ;;
    n) newvolumeUuid=$OPTARG
     ;;
    p) POOLPATH=$OPTARG
     ;;
    v)  verb=1
      ;;
  esac
done
shift $((OPTIND-1))

[ "${1:-}" = "--" ] && shift

logFile=$logDir"RESTORE_"$kvmDName_$(date +'%Y_%m_%d_%I_%M_%p').log
mkdir -p $logDir


# Perform Initial sanity checks
sanity_checks

if [ -z "$ssid" ]
then
        restore_volume
else
        restore_all_volumes
fi

# Wereturn 1 for the probe
exit 0
