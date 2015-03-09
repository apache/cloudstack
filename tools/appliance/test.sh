#!/bin/bash -xl
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

# test script for build.sh which tries a variety of options/configs to make different vms

set -e

DEBUG="${DEBUG:-}"
TRACE="${TRACE:-0}"

###
### Configuration
###

if [[ "${DEBUG}" == "1" ]]; then
  set -x
fi

# which test to run
test_to_run=${1:-}
# build.sh settings for running the tests
appliance=debianbase
version=`date "+%Y%m%d%H%M%S"`
branch=`git status | grep '# On branch' | awk '{print $4}'`
BUILD_NUMBER="${BUILD_NUMBER:-}"
ssh_key=

# where we are running the tests from
CURR_DIR=${PWD}
# where this script is
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# ensure we are running in isolation
if [ ${CURR_DIR} == ${SCRIPT_DIR} ]; then
  mkdir -p ../appliance-work
  cd ../appliance-work
  CURR_DIR=${PWD}
fi

###
### testing 'framework'
###

function test_result() {
  log INFO "$@"
  add_on_exit log INFO "$@"
}

function run_test() {
  set +e
  cleanup
  fixture
  log INFO running test: "$@"
  eval $@
  result=$?
  if ${result}; then
    test_result "$@" FAIL
  else
    test_result "$@" OK
  fi
  cleanup
  set -e
}

function cleanup() {
  (
    cd ${CURR_DIR};
    rm -rf iso definitions Gemfile shar_cloud_scripts convert_ovf_vbox_to_esx.xslt .rvmrc;
  )
}

function fixture() {
  (
    cd ${CURR_DIR};
    mkdir -p ${SCRIPT_DIR}/iso;
    ln -s ${SCRIPT_DIR}/iso;
    mkdir definitions;
    ln -s ${SCRIPT_DIR}/definitions/${appliance} definitions/${appliance};

    ln -s ${SCRIPT_DIR}/Gemfile;
    ln -s ${SCRIPT_DIR}/shar_cloud_scripts.sh;
    ln -s ${SCRIPT_DIR}/convert_ovf_vbox_to_esx.xslt;
    ln -s ${SCRIPT_DIR}/.rvmrc;
  )
}

###
### Test definitions
###

function do_test_vm() {
  prepare
  create_definition
  veewee_build
  retry 10 check_appliance_shutdown
  retry 10 remove_shares
  veewee_destroy
}

function do_test_export() {
  prepare
  create_definition
  veewee_build
  retry 10 check_appliance_shutdown
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
  hyperv_export "${hdd_uuid}"

  veewee_destroy
}

function test_basic_veewee_invocation() {
  appliance=debianbase
  appliance_build_name=${appliance}${branch_tag}${version_tag}
  do_test_vm
}

function test_export() {
  appliance=debianbase
  appliance_build_name=${appliance}${branch_tag}${version_tag}
  do_test_export
}

function test_systemvm() {
  appliance=systemvmtemplate
  appliance_build_name=${appliance}${branch_tag}${version_tag}
  do_test_vm
}

function test_systemvm64() {
  appliance=systemvm64template
  appliance_build_name=${appliance}${branch_tag}${version_tag}
  do_test_vm
}

function test_suite() {
  if [ "${test_to_run}" == "" ]; then
    # list of all tests goes here
    run_test test_basic_veewee_invocation
    run_test test_systemvm
    run_test test_systemvm64
    run_test test_export
  else
    run_test "${test_to_run}"
  fi
}

###
### Main invocation
###

source ${SCRIPT_DIR}/build.sh
return 2>/dev/null || test_suite
