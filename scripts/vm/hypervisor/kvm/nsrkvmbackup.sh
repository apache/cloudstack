#!/bin/bash
version=0.7.4

OPTIND=1

dryRun=0
verb=0
logDir="/nsr/logs/cloudstack/"
snapPrefix="CSBKP_"
clusterClient=""
networkerServer=""
hvVersion=""
libvVersion=""
apiVersion=""
kvmDName=""
kvmDUuid=""
logFile=""

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

Usage:[-v] [-h] [-l log_dir] [-dr] [-s networker_server] [-c networker_cluster_client] [-t target_vm] [-u target_uuid] [-p snapprefix]

Options:
        -h Help and usage
        -v Enable verbose mode
        -l log_dir. Specify log directory. Default is /nsr/logs/cloudstack
        -s networker_server  Specifiy the EMC Networker server we are going to use
        -c networker_cluster_client  Specify the EMC Networker client CLUSTER to use
        -t target_vm KVM domain to backup
        -u target_uuid KVM domain to backup
        -p Snapshot Prefix for backups

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
backup_domain() {

name=$1
snapName=$2
log  "Preparing snapshots and gathering information for backing up  domain $name under snapshot name $snapName"

TARGETS=($(virsh domblklist $name --details | grep file | grep -v 'cdrom' | grep -v 'floppy' | awk '{print $3}'))
SOURCES=($(virsh domblklist $name --details | grep file | grep -v 'cdrom' | grep -v 'floppy' | awk '{print $4}'))

verbose "We found ${#TARGETS[@]} targets and ${#SOURCES[@]} sources"


if [ "${#TARGETS[@]}" -ne  "${#SOURCES[@]}" ]; then
        log  "Error!!! Targets and Sources mismatch. Aborting...."
        exit -6;
fi


diskspec=''
for target in ${TARGETS[@]}; do
diskspec="$diskspec --diskspec $target,snapshot=external"
disks="$disks  ${SOURCES[$target]}  "
done

virsh snapshot-create-as --domain $name --name $snapName --no-metadata --atomic --quiesce --disk-only $diskspec >/dev/null
if [ $? -ne 0 ]; then
        log "Agent not responding, trying to snapshot directly"
        if [[ "$verb" ]]; then
                virsh snapshot-create-as --domain $name --name $snapName --no-metadata --atomic --disk-only $diskspec
        else
                virsh snapshot-create-as --domain $name --name $snapName --no-metadata --atomic --disk-only $diskspec >/dev/null
        fi
        if [ $? -ne 0 ]; then
                log "Failed to create snapshot for $name"
                exit -7
        fi
        echo "Created snapshot(s) for $name"
fi

save -LL -q -s $networkerServer -c $clusterClient -N "$name" $disks
if [ $? -ne 0 ]; then
        logger "Unable to backup $disks for $name"
else
        logger "Backup $disks for $name completed!!"
fi


#Merge changes and conclude
SNAPSHOTS=($(virsh domblklist $name --details | grep file | grep -v 'cdrom' | grep -v 'floppy' | awk '{print $4}'))
for target in ${TARGETS[@]}; do
        log "Merging Snasphots for $target"
        if [[ "$verb" ]]; then
                virsh blockcommit $name $target --active --pivot
        else
                virsh blockcommit $name $target --active --pivot >/dev/null
        fi
        if [ $? -ne 0 ]; then
                log "Unable to merge disk %target changes for domain $name"
                exit -8
        fi
done
#Clean snapshots
for snapshot in ${SNAPSHOTS[@]}; do
        rm -f $snapshot
done

}



while getopts "h?vs:l:c:t:u:p:" opt; do
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
    t) kvmDName=$OPTARG
     ;;
    u) kvmDUuid=$OPTARG
     ;;
    p) snapPrefix=$OPTARG
     ;;
    v)  verb=1
      ;;
  esac
done

shift $((OPTIND-1))

[ "${1:-}" = "--" ] && shift

logFile=$logDir$kvmDName_$(date +'%Y_%m_%d_%I_%M_%p').log
mkdir -p $logDir


# Perform Initial sanity checks
sanity_checks


target_name=$(virsh domname $kvmDUuid)
target_uuid=$(virsh domuuid $kvmDName)

echo -e "\nLooking for domain to backup"
if [[ $kvmDName == $target_name && $kvmDUuid == $target_uuid ]]; then
        echo "Domain $kvmDName with UUID $kvmDUuid found...."
else
        echo "Domain not found on this host. Aborting....."
        log "Domain not found on this host. Check for the location of the Instance in the cloudstack management console"
        exit -5
fi

backup_domain $kvmDName $snapPrefix$kvmDName

# Wereturn 1 for the probe
exit 0
