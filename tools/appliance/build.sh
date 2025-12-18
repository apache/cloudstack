#!/bin/bash -l
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

# build script which wraps around packer and virtualbox to create the systemvm template

function usage() {
  cat <<END
Usage:
   ./build.sh [template] [version] [BUILD_NUMBER]

   * Set \$appliance to provide definition name to build
     (or use command line arg, default systemvmtemplate)
   * Set \$version to provide version to apply to built appliance
     (or use command line arg, default empty)
   * Set \$target_arch to provide target architecture
     (or use command line arg, default to current architecture. Currently x86_64 and aarch64 are implemented)
   * Set \$BUILD_NUMBER to provide build number to apply to built appliance
     (or use command line arg, default empty)
   * Set \$DEBUG=1 to enable debug logging
   * Set \$TRACE=1 to enable trace logging
END
  exit 0
}

for i in $@; do
    if [ "$i" == "-h" -o "$i" == "--help" -o "$i" == "help" ]; then
        usage
    fi
done

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
#    Install on yum-based:
#    sudo yum -y install python-devel dev86 iasl iasl-devel libuuid libuuid-devel \
#        glib-devel glib2 glib2-devel yajl yajl-devel
#    Install on apt-based:
#    sudo apt-get install -y python python-dev bcc bin86 iasl uuid-dev \
#        libglib2.0-dev libyajl-dev build-essential libc6-dev zlib1g-dev libncurses5-dev \
#        patch iasl libbz2-dev e2fslibs-dev xz-utils gettext
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
#
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

# get current system architecture
base_arch=`arch`

# which packer definition to use
appliance="${1:-${appliance:-systemvmtemplate}}"

# optional version tag to put into the image filename
version="${2:-${version:-}}"

# which architecture to build the template for
target_arch="${3:-${target_arch:-${base_arch}}}"

# optional (jenkins) build number tag to put into the image filename
BUILD_NUMBER="${4:-${BUILD_NUMBER:-}}"

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

appliance_build_name="${appliance}${version_tag}-${target_arch}"

###
### Generic helper functions
###

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

function log() {
  local level=${1?}
  shift

  if [[ "${DEBUG}" != "1" && "${level}" == "DEBUG" ]]; then
    return
  fi

  local code=
  local line="[$(date '+%F %T')] $level: $*"
  if [ -t 2 ]
  then
    case "$level" in
      INFO) code=36 ;;
      DEBUG) code=30 ;;
      WARN) code=33 ;;
      ERROR) code=31 ;;
      *) code=37 ;;
    esac
    echo -e "\033[${code}m${line}\033[0m"
  else
    echo "$line"
  fi >&2
}

function error() {
  log ERROR $@
  exit 1
}

# cleanup code support
declare -a on_exit_items

function on_exit() {
  for (( i=${#on_exit_items[@]}-1 ; i>=0 ; i-- )) ; do
    sleep 2
    log DEBUG "on_exit: ${on_exit_items[i]}"
    eval ${on_exit_items[i]}
  done
}

function add_on_exit() {
  local n=${#on_exit_items[*]}
  on_exit_items[${n}]="$*"
  if [ ${n} -eq 0 ]; then
    log DEBUG "Setting trap"
    trap on_exit EXIT
  fi
}

# retry code support
function retry() {
  local times=$1
  shift
  local count=0
  while [ ${count} -lt ${times} ]; do
    "$@" && break
    count=$(( $count +  1 ))
    sleep ${count}
  done

  if [ ${count} -eq ${times} ]; then
    error "Failed ${times} times: $@"
  fi
}

###
### Script logic
###

function create_definition() {
  if [ "${appliance}" != "${appliance_build_name}" ]; then
    cp -r "${appliance}" "${appliance_build_name}"
    set +e
    if [ ! -z "${version}" ]; then
      if [ -f "${appliance_build_name}/scripts/configure_systemvm_services.sh" ]; then
          sed ${sed_regex_option} -i -e "s/^CLOUDSTACK_RELEASE=.+/CLOUDSTACK_RELEASE=${version}/" \
              "${appliance_build_name}/scripts/configure_systemvm_services.sh"
      fi
    fi
    set -e
    add_on_exit rm -rf "${appliance_build_name}"
  fi

  ./shar_cloud_scripts.sh
  add_on_exit rm -f cloud_scripts_shar_archive.sh
}

function prepare() {
  log INFO "preparing for build"
  rm -rf dist *.ova *.vhd *.vdi *.qcow* *.bz2 *.vmdk *.ovf
}

function packer_build() {
  log INFO "building new image with packer"
  cd ${appliance_build_name} && packer build template-base_${base_arch}-target_${target_arch}.json && cd ..
}

function stage_vmx() {
  cat << VMXFILE > "${1}.vmx"
.encoding = "UTF-8"
displayname = "${1}"
annotation = "${1}"
guestos = "otherlinux-64"
virtualHW.version = "13"
config.version = "8"
numvcpus = "1"
cpuid.coresPerSocket = "1"
memsize = "256"
pciBridge0.present = "TRUE"
pciBridge4.present = "TRUE"
pciBridge4.virtualDev = "pcieRootPort"
pciBridge4.functions = "8"
pciBridge5.present = "TRUE"
pciBridge5.virtualDev = "pcieRootPort"
pciBridge5.functions = "8"
pciBridge6.present = "TRUE"
pciBridge6.virtualDev = "pcieRootPort"
pciBridge6.functions = "8"
pciBridge7.present = "TRUE"
pciBridge7.virtualDev = "pcieRootPort"
pciBridge7.functions = "8"
vmci0.present = "TRUE"
floppy0.present = "FALSE"
ide0:0.clientDevice = "FALSE"
ide0:0.present = "TRUE"
ide0:0.deviceType = "atapi-cdrom"
ide0:0.autodetect = "TRUE"
ide0:0.startConnected = "FALSE"
mks.enable3d = "false"
svga.autodetect = "false"
svga.vramSize = "4194304"
scsi0:0.present = "TRUE"
scsi0:0.deviceType = "disk"
scsi0:0.fileName = "$2"
scsi0:0.mode = "persistent"
scsi0:0.writeThrough = "false"
scsi0.virtualDev = "lsilogic"
scsi0.present = "TRUE"
vmci0.unrestricted = "false"
vcpu.hotadd = "false"
vcpu.hotremove = "false"
firmware = "bios"
mem.hotadd = "false"
VMXFILE
}

function xen_server_export() {
  log INFO "creating xen server export"
  set +e
  which faketime >/dev/null 2>&1 && which vhd-util >/dev/null 2>&1
  local result=$?
  set -e
  if [ ${result} == 0 ]; then
    qemu-img convert -f qcow2 -O raw "dist/${appliance}" img.raw
    vhd-util convert -s 0 -t 1 -i img.raw -o stagefixed.vhd
    faketime '2010-01-01' vhd-util convert -s 1 -t 2 -i stagefixed.vhd -o "${appliance_build_name}-xen.vhd"
    rm -f *.bak
    bzip2 "${appliance_build_name}-xen.vhd"
    mv "${appliance_build_name}-xen.vhd.bz2" dist/
    log INFO "${appliance} exported for XenServer: dist/${appliance_build_name}-xen.vhd.bz2"
  else
    log WARN "** Skipping ${appliance_build_name} export for XenServer: faketime or vhd-util command is missing. **"
    log WARN "** faketime source code is available from https://github.com/wolfcw/libfaketime **"
  fi
}

function ovm_export() {
  log INFO "creating OVM export"
  qemu-img convert -f qcow2 -O raw "dist/${appliance}" "dist/${appliance_build_name}-ovm.raw"
  cd dist && bzip2 "${appliance_build_name}-ovm.raw" && cd ..
  log INFO "${appliance} exported for OracleVM: dist/${appliance_build_name}-ovm.raw.bz2"
}

function kvm_export() {
  log INFO "creating kvm export"
  set +e
  qemu-img convert -o compat=0.10 -f qcow2 -c -O qcow2 "dist/${appliance}" "dist/${appliance_build_name}-kvm.qcow2"
  local qemuresult=$?
  cd dist && bzip2 "${appliance_build_name}-kvm.qcow2" && cd ..
  log INFO "${appliance} exported for KVM: dist/${appliance_build_name}-kvm.qcow2.bz2"
}

function vmware_export() {
  log INFO "creating vmware export"
  qemu-img convert -f qcow2 -O vmdk "dist/${appliance}" "dist/${appliance_build_name}-vmware.vmdk"

  if ! ovftool_loc="$(type -p "ovftool")" || [ -z "$ovftool_loc" ]; then
    log INFO "ovftool not found, skipping ova generation for VMware"
    return
  fi

  log INFO "ovftool found, using it to export ova file"
  CDIR=$PWD
  cd dist
  chmod 666 ${appliance_build_name}-vmware.vmdk
  stage_vmx ${appliance_build_name}-vmware ${appliance_build_name}-vmware.vmdk
  ovftool ${appliance_build_name}-vmware.vmx ${appliance_build_name}-vmware.ova
  rm -f *vmx *vmdk
  cd $CDIR
  log INFO "${appliance} exported for VMWare: dist/${appliance_build_name}-vmware.ova"
}

function hyperv_export() {
  log INFO "creating hyperv export"
  qemu-img convert -f qcow2 -O vpc "dist/${appliance}" "dist/${appliance_build_name}-hyperv.vhd"
  CDIR=$PWD
  cd dist
  zip "${appliance_build_name}-hyperv.vhd.zip" "${appliance_build_name}-hyperv.vhd"
  rm -f *vhd
  cd $CDIR
  log INFO "${appliance} exported for HyperV: dist/${appliance_build_name}-hyperv.vhd.zip"
}

###
### Main invocation
###

function main() {
  prepare

  create_definition
  packer_build

  # process the disk at dist
  kvm_export
  if [ "${target_arch}" == "x86_64" ]; then
    ovm_export
    xen_server_export
    vmware_export
    hyperv_export
  fi
  rm -f "dist/${appliance}"
  cd dist && chmod +r * && cd ..
  cd dist && md5sum * > md5sum.txt && cd ..
  cd dist && sha512sum * > sha512sum.txt && cd ..
  add_on_exit log INFO "BUILD SUCCESSFUL"
}

# we only run main() if not source-d
return 2>/dev/null || main
