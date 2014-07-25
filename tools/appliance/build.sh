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
   ./build.sh [veewee_template [version [branch [BUILD_NUMBER [arch [ssh_key]]]]]

   * Set \$appliance to provide veewee definition name to build
     (or use command line arg, default systemvmtemplate)
   * Set \$version to provide version to apply to built appliance
     (or use command line arg, default empty)
   * Set \$branch to provide branch name to apply to built appliance
     (or use command line arg, default from running \`git status\`)
   * Set \$BUILD_NUMBER to provide build number to apply to built appliance
     (or use command line arg, default empty)
   * Set \$arch to provide the (debian) os architecture to inject
     (or use command line arg, default i386, other option amd64)
   * Set \$ssh_key to provide root ssh public key to inject
     (or use command line arg, default set in the veewee definition its authorized_keys.sh)
   * Set \$clean_vbox to try pretty hard to remove all our vms and disk from
     virtualbox before and after running the rest of the build. This should
     not be needed since we try hard to use VBoxManage nicely, but, various
     error conditions / timing issues are quite hard to fully contain
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

# (debian) os architecture to build
arch="${5:-${arch:-i386}}"
export VM_ARCH="${arch}"

# optional root SSH public key to write to /root/.ssh/authorized_keys
# note the cs management server overwrites this, so the only reason to
# set this is when working with the VM while it is not under management
# server control
ssh_key="${6:-${ssh_key:-}}"

# whether to attempt to clean up all our virtualbox vms/disks before/after run
clean_vbox="${clean_vbox:-}"

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

# mac address of the vm we create
mac_address=

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
  if [ "${appliance}" == "systemvm64template" ]; then
    arch="amd64"
    export VM_ARCH="${arch}"
    rm -rf definitions/systemvm64template # in case of left-over cruft from failed build
    cp -r definitions/systemvmtemplate definitions/systemvm64template
    add_on_exit rm -rf definitions/systemvm64template
  fi

  if [ "${appliance}" != "${appliance_build_name}" ]; then
    cp -r "definitions/${appliance}" "definitions/${appliance_build_name}"
    set +e
    sed ${sed_regex_option} -i -e "s/^CLOUDSTACK_RELEASE=.+/CLOUDSTACK_RELEASE=${version}/" \
        "definitions/${appliance_build_name}/configure_systemvm_services.sh"
    if [ ! -z "${ssh_key}" ]; then
      # ssh key lines can contain /
      sed ${sed_regex_option} -i -e "s|^key=.+|key=\"${ssh_key}\"|" \
          "definitions/${appliance_build_name}/authorized_keys.sh"
    fi
    set -e
    add_on_exit rm -rf "definitions/${appliance_build_name}"
  fi

  ./shar_cloud_scripts.sh
  add_on_exit rm -f cloud_scripts_shar_archive.sh
}

function setup_ruby() {
  local bundle_args=
  if [[ ! -z "${JENKINS_HOME}" ]]; then
    # inspired by https://github.com/CloudBees-community/rubyci-clickstart/blob/master/bin/run-ci
    # also see https://rvm.io/integration/jenkins
    # .rvmrc won't get trusted/auto-loaded by jenkins by default
    export VAGRANT_HOME=$HOME/.vagrant.d-release-cloudstack
    rvm use ruby-1.9.3@vagrant-release-cloudstack --create
    # do not use --deployment since that requires Gemfile.lock...and we prefer an up-to-date veewee
    bundle_args="--path vendor/bundle"
  fi
  bundle check || bundle install ${bundle_args}
}

function stop_vbox() {
  log INFO "stoppping all virtualbox vms for ${USER}"
  bundle exec ./vbox_vm_clean.rb
}

function clean_vbox() {
  log INFO "deleting all virtualbox vms and disks for ${USER}"
  bundle exec ./vbox_vm_clean.rb --delete
  bundle exec ./vbox_disk_clean.rb
}

function prepare() {
  log INFO "preparing for build"
  setup_ruby
  rm -rf dist *.ova *.vhd *.vdi *.qcow* *.bz2 *.vmdk *.ovf
  mkdir dist
}

function veewee_destroy() {
  log INFO "destroying existing veewee image, if any"
  set +e
  bundle exec veewee vbox destroy "${appliance_build_name}" ${VEEWEE_ARGS}
  set -e
}

function veewee_build() {
  log INFO "building new image with veewee"
  bundle exec veewee vbox build "${appliance_build_name}" ${VEEWEE_BUILD_ARGS}
}

function veewee_halt() {
  log INFO "shutting down new vm with veewee"
  bundle exec veewee vbox halt "${appliance_build_name}" ${VEEWEE_ARGS}
}

function save_mac_address() {
  log INFO "saving new vm mac address"
  mac_address=`vboxmanage showvminfo --details --machinereadable ${appliance_build_name} | grep macaddress1= | sed 's/macaddress1=//' | sed 's/"//g'`
  if [ "${mac_address}" == "" ]; then
    error "Could not find mac address for appliance ${appliance_build_name}"
  fi
}

function check_appliance_shutdown() {
  log INFO "waiting for veewee appliance to shut down..."
  ! (vboxmanage list runningvms | grep "${appliance_build_name}")
  local result=$?
  if [ ${result} -eq 0 ]; then
    log INFO "...veewee appliance shut down ok"
  else
    log INFO "...veewee appliance still running"
  fi
  return ${result}
}

function check_appliance_disk_ready() {
  log INFO "waiting for veewee appliance disk to be available..."
  # local hdd_path="vboxmanage showvminfo '${appliance_build_name}' --machinereadable | \
  #   egrep '(SATA|IDE) Controller-[0-9]+-[0-9]+' | grep -v '.iso' | \
  #   grep -v '="none"' | egrep -o '=".*"' | sed 's/=//' | sed 's/"//g'"
  local hdd_path=`vboxmanage list hdds | grep "${appliance_build_name}\/" | grep vdi | \
      cut -c 14- | sed ${sed_regex_option} 's/^ *//'`
  disk_state=`vboxmanage showhdinfo "${hdd_path}" | egrep '^State:' | sed 's/State://' | egrep -o '[a-zA-Z]+' | awk '{print tolower($0)}'`
  if [ "${disk_state}" == "notcreated" ]; then
    log ERROR "disk ${hdd_path} in state notcreated"
    return 1
  elif [ "${disk_state}" == "created" ]; then
    log INFO "disk ${hdd_path} in state created"
    return 0
  elif [ "${disk_state}" == "lockedread" ]; then
    log INFO "disk ${hdd_path} in state lockedread"
    return 1
  elif [ "${disk_state}" == "lockedwrite" ]; then
    log INFO "disk ${hdd_path} in state lockedwrite"
    return 1
  elif [ "${disk_state}" == "inaccessible" ]; then
    log INFO "disk ${hdd_path} in state inaccessible"
    return 1
  elif [ "${disk_state}" == "creating" ]; then
    log WARN "disk ${hdd_path} in state creating"
    return 1
  elif [ "${disk_state}" == "deleting" ]; then
    log WARN "disk ${hdd_path} in state deleting"
    return 1
  else
    log WARN "disk ${hdd_path} has unknown disk state ${disk_state}"
    return 1
  fi
}

function remove_shares() {
  log INFO "removing shared folders from appliance..."
  set +e
  local shared_folders=`vboxmanage showvminfo "${appliance_build_name}" | grep Name | grep Host`
  if [ "${shared_folders}" == "" ]; then
    return 0
  fi
  folder_name=`echo "${shared_folders}" | head -1 | cut -c 8- | cut -d \' -f 1`
  vboxmanage sharedfolder remove "${appliance_build_name}" --name "${folder_name}"
  ! (vboxmanage showvminfo "${appliance_build_name}" | grep Name | grep Host)
  local result=$?
  set -e
  if [ ${result} -eq 0 ]; then
    log INFO "...veewee appliance shared folders removed"
  else
    log INFO "...veewee appliance still has shared folders"
  fi
  return ${result}
}

function compact_hdd() {
  log INFO "compacting image"
  vboxmanage modifyhd "${1}" --compact
}

function xen_server_export() {
  log INFO "creating xen server export"
  local hdd_path="${1}"
  set +e
  which faketime >/dev/null 2>&1 && which vhd-util >/dev/null 2>&1
  local result=$?
  set -e
  if [ ${result} == 0 ]; then
    vboxmanage internalcommands converttoraw -format vdi "${hdd_path}" img.raw
    vhd-util convert -s 0 -t 1 -i img.raw -o stagefixed.vhd
    faketime '2010-01-01' vhd-util convert -s 1 -t 2 -i stagefixed.vhd -o "${appliance_build_name}-xen.vhd"
    rm *.bak
    bzip2 "${appliance_build_name}-xen.vhd"
    mv "${appliance_build_name}-xen.vhd.bz2" dist/
    log INFO "${appliance} exported for XenServer: dist/${appliance_build_name}-xen.vhd.bz2"
  else
    log WARN "** Skipping ${appliance_build_name} export for XenServer: faketime or vhd-util command is missing. **"
    log WARN "** faketime source code is available from https://github.com/wolfcw/libfaketime **"
  fi
}

function kvm_export() {
  set +e
  which faketime >/dev/null 2>&1 && which vhd-util >/dev/null 2>&1
  local result=$?
  set -e
  if [ ${result} == 0 ]; then
    log INFO "creating kvm export"
    local hdd_path="${1}"
    vboxmanage internalcommands converttoraw -format vdi "${hdd_path}" raw.img
    qemu-img convert -f raw -c -O qcow2 raw.img "${appliance_build_name}-kvm.qcow2"
    add_on_exit rm -f raw.img
    bzip2 "${appliance_build_name}-kvm.qcow2"
    mv "${appliance_build_name}-kvm.qcow2.bz2" dist/
    log INFO "${appliance} exported for KVM: dist/${appliance_build_name}-kvm.qcow2.bz2"
  else
    log WARN "** Skipping ${appliance_build_name} export for KVM: qemu-img is missing. **"
  fi
}

function vmware_export() {
  log INFO "creating vmware export"
  local machine_uuid="${1}"
  local hdd_uuid="${2}"
  vboxmanage clonehd "${hdd_uuid}" "${appliance_build_name}-vmware.vmdk" --format VMDK
  bzip2 "${appliance_build_name}-vmware.vmdk"
  mv "${appliance_build_name}-vmware.vmdk.bz2" dist/
  vboxmanage export "${machine_uuid}" --output "${appliance_build_name}-vmware.ovf"
  log INFO "${appliance} exported for VMWare: dist/${appliance_build_name}-vmware.{vmdk.bz2,ovf}"
  add_on_exit rm -f ${appliance_build_name}-vmware.ovf
  add_on_exit rm -f ${appliance_build_name}-vmware-disk[0-9].vmdk

  # xsltproc doesn't support this XSLT so we use java to run this one XSLT
  mv ${appliance_build_name}-vmware.ovf ${appliance_build_name}-vmware.ovf-orig
  java -cp convert Convert convert_ovf_vbox_to_esx.xslt \
      ${appliance_build_name}-vmware.ovf-orig \
      ${appliance_build_name}-vmware.ovf
  add_on_exit rm -f ${appliance_build_name}-vmware.ovf-orig

  tar -cf ${appliance_build_name}-vmware.ova \
      ${appliance_build_name}-vmware.ovf \
      ${appliance_build_name}-vmware-disk[0-9].vmdk
  mv ${appliance_build_name}-vmware.ova dist/
  log INFO "${appliance} exported for VMWare: dist/${appliance_build_name}-vmware.ova"
}

function vagrant_export() {
  log INFO "creating vagrant export"
  # this is based on veewee export logic, but, we don't want to use veewee export,
  # since it makes optimistic assumptions about VM shutdown/halt leading to available
  # disks and the like
  disk="${appliance_build_name}-vmware.ovf"
  mkdir -p "box/${appliance_build_name}"
  cp "${disk}" "box/${appliance_build_name}/box.ovf"
  cat >box/${appliance_build_name}/Vagrantfile <<END
Vagrant::Config.run do |config|
  # This Vagrantfile is auto-generated by `vagrant package` to contain
  # the MAC address of the box. Custom configuration should be placed in
  # the actual `Vagrantfile` in this box.
  config.vm.base_mac = "${mac_address}"
end

# Load include vagrant file if it exists after the auto-generated
# so it can override any of the settings
include_vagrantfile = File.expand_path("../include/_Vagrantfile", __FILE__)
load include_vagrantfile if File.exist?(include_vagrantfile)
END
  ( cd box/${appliance_build_name}; tar cf "../${appliance_build_name}.box" . )
  mv "box/${appliance_build_name}.box" dist/
  log INFO "${appliance} exported for vagrant: dist/${appliance_build_name}.box"
}

function hyperv_export() {
  log INFO "creating hyperv export"
  local hdd_uuid="${1}"
  vboxmanage clonehd "${hdd_uuid}" "${appliance_build_name}-hyperv.vhd" --format VHD
  # HyperV doesn't support import a zipped image from S3,
  # but we create a zipped version to save space on the jenkins box
  zip "${appliance_build_name}-hyperv.vhd.zip" "${appliance_build_name}-hyperv.vhd"
  mv "${appliance_build_name}-hyperv.vhd.zip" "${appliance_build_name}-hyperv.vhd" dist/
  log INFO "${appliance} exported for HyperV: dist/${appliance_build_name}-hyperv.vhd.zip"
}

###
### Main invocation
###

function main() {
  prepare
  if [ "${clean_vbox}" == "1" ]; then
    clean_vbox --delete
    add_on_exit clean_vbox --delete
  else
    stop_vbox # some extra encouragement for virtualbox to stop things
  fi
  create_definition
  veewee_destroy # in case of left-over cruft from failed build
  add_on_exit veewee_destroy
  veewee_build
  save_mac_address
  veewee_halt
  stop_vbox # some extra encouragement for virtualbox to stop things
  retry 10 check_appliance_shutdown
  retry 10 check_appliance_disk_ready
  retry 10 remove_shares

  # Get appliance uuids
  local vm_info=`vboxmanage showvminfo "${appliance_build_name}"`
  local machine_uuid=`echo "${vm_info}" | grep UUID | head -1 | awk '{print $2}'`
  local hdd_uuid=`echo "${vm_info}" | grep vdi | head -1 | awk '{print $8}' | cut -d ')' -f 1`
  local hdd_path=`vboxmanage list hdds | grep "${appliance_build_name}\/" | grep vdi | \
      cut -c 14- | sed ${sed_regex_option} 's/^ *//'`

  compact_hdd "${hdd_uuid}"
  xen_server_export "${hdd_path}"
  kvm_export "${hdd_path}"
  vmware_export "${machine_uuid}" "${hdd_uuid}"
  vagrant_export "${machine_uuid}" "${hdd_uuid}"
  hyperv_export "${hdd_uuid}"
  add_on_exit log INFO "BUILD SUCCESSFUL"
}

# we only run main() if not source-d
return 2>/dev/null || main
