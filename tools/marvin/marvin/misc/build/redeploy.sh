
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

#set -x
usage() {
  printf "Usage: %s:\n
	[-s nfs path to secondary storage <nfs-server:/export/path> ] [-u url to system template] [-h hypervisor type (kvm|xenserver|vmware) ]\n" $(basename $0) >&2

  printf "\nThe -s flag will clean the secondary path and install the specified
hypervisor's system template as per -h, if -h is not given then xenserver is
assumed\n"

}

failed() {
	exit $1
}

#flags
sflag=
hflag=
uflag=

VERSION="1.0.1"
echo "Redeploy Version: $VERSION"

#some defaults
spath='nfs2.lab.vmops.com:/export/home/bvt/secondary'
hypervisor='xenserver'
sysvmurl='http://download.cloudstack.org/templates/acton/acton-systemvm-02062012.vhd.bz2'
systemvm_seeder='/usr/share/cloudstack-common/scripts/storage/secondary/cloud-install-sys-tmplt'

while getopts 'u:s:h:' OPTION
do
  case $OPTION in
  s)    sflag=1
		spath="$OPTARG"
  		;;
  h)    hflag=1
		hypervisor="$OPTARG"
		;;
  u)    uflag=1
		sysvmurl="$OPTARG"
		;;
  ?)	usage
		failed 2
		;;
  esac
done

if [[ -e /etc/redhat-release ]]
then
	cat /etc/redhat-release
else
	echo "script works on rpm environments only"
	exit 5
fi

#check if process is running
proc=$(ps aux | grep cloud | wc -l)
if [[ $proc -lt 2 ]]
then
        echo "Cloud process not running"
        if [[ -e /var/run/cloud-management.pid ]]
        then
            rm -f /var/run/cloud-management.pid
        fi
else
        #stop service
        service cloud-management stop
fi

#TODO: archive old logs
#refresh log state
cat /dev/null > /var/log/cloud/management/management-server.log
cat /dev/null > /var/log/cloud/management/api-server.log
cat /dev/null > /var/log/cloud/management/catalina.out

#replace disk size reqd to 1GB max
sed -i 's/DISKSPACE=5120000/DISKSPACE=20000/g' $systemvm_seeder

if [[ "$uflag" != "1" && "$hypervisor" != "xenserver" ]]
then
    echo "URL of systemvm template is reqd."
    usage
fi

if [[ "$sflag" == "1" ]]
then
	mkdir -p /tmp/secondary
	mount -t nfs $spath /tmp/secondary
	rm -rf /tmp/secondary/*

	if [[ "$hflag" == "1" && "$hypervisor" == "xenserver" ]]
	then
		bash -x $systemvm_seeder -m /tmp/secondary/ -u $sysvmurl -h xenserver
	elif [[ "$hflag" == "1" && "$hypervisor" == "kvm" ]]
	then
		bash -x $systemvm_seeder -m /tmp/secondary/ -u $sysvmurl -h kvm
	elif [[ "$hflag" == "1" && "$hypervisor" == "vmware" ]]
	then
		bash -x $systemvm_seeder -m /tmp/secondary/ -u $sysvmurl -h vmware
	else
		bash -x $systemvm_seeder -m /tmp/secondary/ -u $sysvmurl -h xenserver
	fi
	umount /tmp/secondary
else
    echo "please provide the nfs secondary storage path where templates are stored"
    usage
fi
