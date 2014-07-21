#!/bin/bash -l
# note: the -l is needed here for bash to always make a login shell and load rvm if it hasn't been loaded
#
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

# build script which wraps around veewee and virtualbox to create the systemvm template

function usage() {
  cat <<END
Usage:
   ./build.sh [veewee_template [version [branch [BUILD_NUMBER]]]

   * Set \$appliance to provide veewee definition name to build
     (or use command line arg, default systemvmtemplate)
   * Set \$version to provide version to apply to built appliance
     (or use command line arg, default empty)
   * Set \$branch to provide branch name to apply to built appliance
     (or use command line arg, default from running \`git status\`)
   * Set \$BUILD_NUMBER to provide build number to apply to built appliance
     (or use command line arg, default empty)
   * Set \$DEBUG=1 to enable debug logging
   * Set \$TRACE=1 to enable trace logging
   * Set \$VEEWEE_ARGS to pass veewee custom arguments
     (default: empty)
   * Set \$VEEWEE_BUILD_ARGS to pass veewee exec build custom arguments
     (default: --nogui --auto)
END
  exit 0
}
echo $@ | grep help >/dev/null && usage
echo $@ | grep '\-h' >/dev/null && usage

# requires 32-bit vhd-util and faketime binaries to be available (even for 64 bit builds)
# Something like (on centos 6.5)...
# * faketime
#    wget -q http://bits.xensource.com/oss-xen/release/4.2.0/xen-4.2.0.tar.gz
#    sudo yum -y install libuuid.i686
#    cd repo/libfaketime/
#    vim Makefile
#    # (tune 32 bit)
#    make
#    sudo make install
# * vhd-util
#    sudo yum -y install python-devel dev86 iasl iasl-devel libuuid libuuid-devel \
#        glib-devel glib2 glib2-devel yajl yajl-devel
#    wget -q http://bits.xensource.com/oss-xen/release/4.2.0/xen-4.2.0.tar.gz
#    tar xzvf xen-4.2.0.tar.gz
#    cd xen-4.2.0/tools/
#    wget https://github.com/citrix-openstack/xenserver-utils/raw/master/blktap2.patch -qO - | patch -p0
#    ./configure --disable-monitors --disable-ocamltools --disable-rombios --disable-seabios
#    make
#    sudo cp ./blktap2/vhd/lib/libvhd.so.1.0 /usr/lib64/
#    ldconfig
#    sudo ldconfig
#    sudo cp blktap2/vhd/vhd-util /usr/lib64/cloud/common/scripts/vm/hypervisor/xenserver
#    faketime 2010-01-01 vhd-util convert

set -e

###
### Configuration
###
# whether to show DEBUG logs
DEBUG="${DEBUG:-}"
# whether to have other commands trace their actions
TRACE="${TRACE:-0}"
JENKINS_HOME=${JENKINS_HOME:-}
if [[ ! -z "${JENKINS_HOME}" ]]; then
  DEBUG=1
fi
VEEWEE_ARGS="${VEEWEE_ARGS:-}"
if [[ "${VEEWEE_ARGS}" == "" && "${TRACE}" == "1" ]]; then
  VEEWEE_ARGS="${VEEWEE_ARGS} --debug"
fi
VEEWEE_BUILD_ARGS="${VEEWEE_BUILD_ARGS:-${VEEWEE_ARGS} --nogui --auto}"

# which veewee definition to use
appliance="${1:-${appliance:-systemvmtemplate}}"

# optional version tag to put into the image filename
version="${2:-${version:-}}"

# branch tag to put into the image filename, populated from `git status` if unset
branch="${3:-${branch:-}}"

# optional (jenkins) build number tag to put into the image filename
BUILD_NUMBER="${4:-${BUILD_NUMBER:-}}"

arch="${arch:-i386}"
if [ "${appliance}" == "systemvm64template" ]; then
  arch="amd64"
  export VM_ARCH="${arch}"
  rm -rf definitions/systemvm64template
  cp -r definitions/systemvmtemplate definitions/systemvm64template
fi

# while building with vbox, we need a quite unique appliance name in order to prevent conflicts with multiple
# concurrent executors on jenkins
if [ -z "${branch}" ] ; then
 branch=`(git name-rev --no-undefined --name-only HEAD 2>/dev/null || echo unknown) | sed -e 's/remotes\/.*\///g'`
fi

branch_tag=
if [ ! -z "${branch}" ]; then
  branch_tag="-${branch}"
fi

version_tag=
if [ ! -z "${version}" ]; then
  if [ ! -z "${BUILD_NUMBER}" ]; then
    version="${version}.${BUILD_NUMBER}"
  fi
  version_tag="-${version}"
elif [ ! -z "${BUILD_NUMBER}" ]; then
  version="${BUILD_NUMBER}"
  version_tag="-${BUILD_NUMBER}"
fi

appliance_build_name=${appliance}${branch_tag}${version_tag}

# how to tell sed to use extended regular expressions
os=`uname`
sed_regex_option="-E"
if [ "${os}" == "Linux" ]; then
  sed_regex_option="-r"
fi

# logging support
if [[ "${DEBUG}" == "1" ]]; then
  set -x
fi

# Create custom template definition
if [ "${appliance}" != "${appliance_build_name}" ]; then
  cp -r "definitions/${appliance}" "definitions/${appliance_build_name}"
  set +e
  sed ${sed_regex_option} -i -e "s/^CLOUDSTACK_RELEASE=.+/CLOUDSTACK_RELEASE=${version}/" \
      "definitions/${appliance_build_name}/configure_systemvm_services.sh"
  set -e
fi

# Initialize veewee and dependencies
bundle

# Clean and start building the appliance
bundle exec veewee vbox destroy ${appliance_build_name} ${VEEWEE_ARGS}
bundle exec veewee vbox build ${appliance_build_name} ${VEEWEE_BUILD_ARGS}
bundle exec veewee vbox halt ${appliance_build_name} ${VEEWEE_ARGS}

while [[ `vboxmanage list runningvms | grep ${appliance_build_name} | wc -l` -ne 0 ]];
do
  echo "Waiting for ${appliance_build_name} to shutdown"
  sleep 2;
done

# Get appliance uuids
machine_uuid=`vboxmanage showvminfo ${appliance_build_name} | grep UUID | head -1 | awk '{print $2}'`
hdd_uuid=`vboxmanage showvminfo ${appliance_build_name} | grep vdi | head -1 | awk '{print $8}' | cut -d ')' -f 1`
hdd_path=`vboxmanage list hdds | grep "${appliance_build_name}\/" | grep vdi | cut -c 14- | sed 's/^ *//'`

# Remove any shared folder
shared_folders=`vboxmanage showvminfo ${appliance_build_name} | grep Name | grep Host`
while [ "$shared_folders" != "" ]
do
  vboxmanage sharedfolder remove ${appliance_build_name} --name "`echo $shared_folders | head -1 | cut -c 8- | cut -d \' -f 1`"
  shared_folders=`vboxmanage showvminfo ${appliance_build_name} | grep Name | grep Host`
done

# Compact the virtual hdd
vboxmanage modifyhd $hdd_uuid --compact

# Start exporting
rm -fr dist *.ova *.vhd *.vdi *.qcow* *.bz2 *.vmdk *.ovf
mkdir dist

# Export for XenServer
which faketime >/dev/null 2>&1 && which vhd-util >/dev/null 2>&1
if [ $? == 0 ]; then
  set -e
  vboxmanage internalcommands converttoraw -format vdi "$hdd_path" img.raw
  vhd-util convert -s 0 -t 1 -i img.raw -o stagefixed.vhd
  faketime '2010-01-01' vhd-util convert -s 1 -t 2 -i stagefixed.vhd -o ${appliance_build_name}-xen.vhd
  rm *.bak
  bzip2 ${appliance_build_name}-xen.vhd
  echo "${appliance_build_name} exported for XenServer: dist/${appliance_build_name}-xen.vhd.bz2"
else
  echo "** Skipping ${appliance_build_name} export for XenServer: faketime or vhd-util command is missing. **"
  echo "** faketime source code is available from https://github.com/wolfcw/libfaketime **"
fi

# Exit shell if exporting fails for any format
set -e

# Export for KVM
vboxmanage internalcommands converttoraw -format vdi "$hdd_path" raw.img
qemu-img convert -f raw -c -O qcow2 raw.img ${appliance_build_name}-kvm.qcow2
rm raw.img
bzip2 ${appliance_build_name}-kvm.qcow2
echo "${appliance_build_name} exported for KVM: dist/${appliance_build_name}-kvm.qcow2.bz2"

# Export both ova and vmdk for VMWare
vboxmanage clonehd $hdd_uuid ${appliance_build_name}-vmware.vmdk --format VMDK
bzip2 ${appliance_build_name}-vmware.vmdk
echo "${appliance_build_name} exported for VMWare: dist/${appliance_build_name}-vmware.vmdk.bz2"
vboxmanage export $machine_uuid --output ${appliance_build_name}-vmware.ovf
mv ${appliance_build_name}-vmware.ovf ${appliance_build_name}-vmware.ovf-orig
java -cp convert Convert convert_ovf_vbox_to_esx.xslt ${appliance_build_name}-vmware.ovf-orig ${appliance_build_name}-vmware.ovf
tar -cf ${appliance_build_name}-vmware.ova ${appliance_build_name}-vmware.ovf ${appliance_build_name}-vmware-disk[0-9].vmdk
rm -f ${appliance_build_name}-vmware.ovf ${appliance_build_name}-vmware.ovf-orig ${appliance_build_name}-vmware-disk[0-9].vmdk
echo "${appliance_build_name} exported for VMWare: dist/${appliance_build_name}-vmware.ova"

# Export for HyperV
vboxmanage clonehd $hdd_uuid ${appliance_build_name}-hyperv.vhd --format VHD
# HyperV doesn't support import a zipped image from S3, but we create a zipped version to save space on the jenkins box
zip ${appliance_build_name}-hyperv.vhd.zip ${appliance_build_name}-hyperv.vhd
echo "${appliance_build_name} exported for HyperV: dist/${appliance_build_name}-hyperv.vhd"

mv *-hyperv.vhd *-hyperv.vhd.zip *.bz2 *.ova dist/

rm -rf "definitions/${appliance_build_name}"
