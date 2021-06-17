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

#set -x

usage() {
  echo "Usage:"
  echo "  `basename $0`: <hypervisor>"
  echo "  `basename $0`: {xenserver, kvm, vmware, lxc, ovm}"
  exit 2
}

[ $# -lt 1 ] && usage


HYPERVISOR=$1

INSTALL_SYS_TMPLT=/usr/share/cloudstack-common/scripts/storage/secondary/cloud-install-sys-tmplt
EXPORT_PATH=/exports/secondary

if [ ! -d ${EXPORT_PATH} ]; then
	echo "ERROR: Secondary Storage path '${EXPORT_PATH}' not found."
	exit 3
fi

URL="http://jenkins.buildacloud.org/job/build-systemvm64-main/lastSuccessfulBuild/artifact/tools/appliance/dist"
case $HYPERVISOR in
 	kvm)
		TO_DOWNLOAD=${URL}/systemvm64template-main-4.6.0-kvm.qcow2.bz2
		;;
	xenserver)
		TO_DOWNLOAD=${URL}/systemvm64template-main-4.6.0-xen.vhd.bz2
		;;
	vmware)
		TO_DOWNLOAD=${URL}/systemvm64template-main-4.6.0-vmware.ova
		;;
	lxc)
		TO_DOWNLOAD=${URL}/systemvm64template-main-4.6.0-kvm.qcow2.bz2
		;;
	ovm)
		TO_DOWNLOAD=${URL}/systemvm64template-main-4.6.0-ovm.raw.bz2
		;;
	*)
		echo "ERROR: hypervisor not found"
		exit 4
		;;
esac

${INSTALL_SYS_TMPLT} -m ${EXPORT_PATH} -u ${TO_DOWNLOAD} -h $HYPERVISOR

exit 0
