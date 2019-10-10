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


# Copies keys that enable SSH communication with system vms
# $1 = new public key
# $2 = new private key

#set -x
set -e

TMP=/tmp
MOUNTPATH=${HOME}/systemvm_mnt
TMPDIR=${TMP}/cloud/systemvm
umask 022

clean_up() {
  rm -rf --preserve-root $MOUNTPATH
}

backup_iso() {
  $SUDO cp -b ${systemvmpath} ${systemvmpath}.bak
}

inject_into_iso() {
  local isofile=${systemvmpath}
  local newpubkey=$2
  local tmpiso=${TMP}/$1
  mkdir -p $MOUNTPATH
  [ ! -f $isofile ] && echo "$(basename $0): Could not find systemvm iso patch file $isofile" && return 1
  bsdtar -C $MOUNTPATH -xf $isofile
  [ $? -ne 0 ] && echo "$(basename $0): Failed to extract original iso $isofile" && clean_up && return 1
  diff -q $MOUNTPATH/authorized_keys $newpubkey &> /dev/null && clean_up && return 0
  backup_iso
  [ $? -ne 0 ] && echo "$(basename $0): Failed to backup original iso $isofile" && clean_up && return 1
  $SUDO cp $newpubkey $MOUNTPATH/authorized_keys
  [ $? -ne 0 ] && echo "$(basename $0): Failed to copy key $newpubkey from original iso to new iso " && clean_up && return 1
  mkisofs -quiet -r -o $tmpiso $MOUNTPATH
  [ $? -ne 0 ] && echo "$(basename $0): Failed to create new iso $tmpiso from $MOUNTPATH" && clean_up && return 1
  $SUDO cp -f $tmpiso $isofile
  [ $? -ne 0 ] && echo "$(basename $0): Failed to overwrite old iso $isofile with $tmpiso" && return 1
  clean_up
}

copy_priv_key() {
  local newprivkey=$1
  diff -q $newprivkey $(dirname $0)/id_rsa.cloud && return 0
  $SUDO cp -fb $newprivkey $(dirname $0)/id_rsa.cloud
  $SUDO chmod 644 $(dirname $0)/id_rsa.cloud
  return $?
}

if [[ "$EUID" -ne 0  ]]
then
   SUDO="sudo -n "
fi

mkdir -p $MOUNTPATH

[ $# -ne 3 ] && echo "Usage: $(basename $0)  <new public key file> <new private key file> <systemvm iso path>" && exit 3
newpubkey=$1
newprivkey=$2
systemvmpath=$3
[ ! -f $newpubkey ] && echo "$(basename $0): Could not open $newpubkey" && exit 3
[ ! -f $newprivkey ] && echo "$(basename $0): Could not open $newprivkey" && exit 3

command -v mkisofs > /dev/null   || (echo "$(basename $0): mkisofs not found, please install or ensure PATH is accurate" ; exit 4)

if [ ! -x "$(command -v bsdtar)" ]; then
  echo "bsdtar is unavailable. Skipping ssh key insertion in systemvm.iso"
else
  inject_into_iso systemvm.iso $newpubkey
  [ $? -ne 0 ] && exit 5
  copy_priv_key $newprivkey
fi
