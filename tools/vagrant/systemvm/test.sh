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

# build script which wraps around test-kitchen to test the systemvm

function usage() {
  cat <<END
Usage:
   ./build.sh
END
  exit 0
}
echo $@ | grep help >/dev/null && usage
echo $@ | grep '\-h' >/dev/null && usage

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

kitchen_args=
if [[ "${DEBUG}" == "1" ]]; then
  kitchen_args="-l debug"
fi

# optional (jenkins) build number tag to put into the image filename
VPC_IP="${VPC_IP:-192.168.56.30}"
export VPC_IP

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

function prepare() {
  log INFO "preparing for build"
  setup_ruby
  rm -f systemvm.iso
}

function box_update() {
  log INFO "invoking vagrant box update"
  vagrant box update
  log INFO "vagrant box update complete"
}

function converge_kitchen() {
  log INFO "invoking test-kitchen converge"
  kitchen create ${kitchen_args}
  kitchen converge ${kitchen_args}
  log INFO "test-kitchen complete"
}

function verify_kitchen() {
  log INFO "invoking test-kitchen verify"

  kitchen verify ${kitchen_args}

  # re-run busser test with patched serverspec gem to get a rspec.xml
  kitchen exec ${kitchen_args} -c '
BUSSER_ROOT="/tmp/busser" GEM_HOME="/tmp/busser/gems" GEM_PATH="/tmp/busser/gems" GEM_CACHE="/tmp/busser/gems/cache"
export BUSSER_ROOT GEM_HOME GEM_PATH GEM_CACHE
/tmp/kitchen/bootstrap.sh
sudo -E /tmp/busser/bin/busser test
'

  # ssh to machine ourselves to avoid kitchen output
  (cd .kitchen/kitchen-vagrant/default-systemvm; vagrant ssh-config) > vagrant_ssh_config
  add_on_exit rm -f vagrant_ssh_config
  scp -F vagrant_ssh_config default:/tmp/rspec.xml rspec.xml
  log INFO "test results in rspec.xml"

  log INFO "test-kitchen complete"
}

function destroy_kitchen() {
  log INFO "invoking test-kitchen destroy"
  kitchen destroy ${kitchen_args}
  log INFO "test-kitchen destroy complete"
}
###
### Main invocation
###

function main() {
  prepare
  box_update
  add_on_exit destroy_kitchen
  converge_kitchen
  verify_kitchen
  add_on_exit log INFO "BUILD SUCCESSFUL"
}

# we only run main() if not source-d
return 2>/dev/null || main
