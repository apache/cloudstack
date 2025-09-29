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

function getTemplateVersion() {
  projVersion=$1
  version="$(cut -d'-' -f1 <<<"$projVersion")"
  subversion1="$(cut -d'.' -f1 <<<"$version")"
  subversion2="$(cut -d'.' -f2 <<<"$version")"
  minorversion="$(cut -d'.' -f3 <<<"$version")"
  securityversion="$(cut -d'.' -f4 <<<"$version")"
  export CS_VERSION="${subversion1}"."${subversion2}"
  export CS_MINOR_VERSION="${minorversion}"
  export VERSION="${CS_VERSION}.${CS_MINOR_VERSION}"
  export CS_SYSTEMTEMPLATE_REPO="https://download.cloudstack.org/systemvm/${CS_VERSION}/"
}

function getGenericName() {
  hypervisor=$(echo "$1" | tr "[:upper:]" "[:lower:]")
  if [[ "$hypervisor" == "ovm3" ]]; then
    echo "ovm"
  elif [[ "$hypervisor" == "lxc" ]]; then
    echo "kvm"
  elif [[ "$hypervisor" == "xenserver" ]]; then
    echo "xen"
  else
    echo "$hypervisor"
  fi
}

function getGuestOS() {
  hypervisor=$(echo "$1" | tr "[:upper:]" "[:lower:]")
  if [[ "$hypervisor" == "vmware" || "$hypervisor" == "xenserver" ]]; then
    echo "Other Linux (64-bit)"
  else
    echo "Debian GNU/Linux 12 (64-bit)"
  fi
}

function getChecksum() {
  local fileData="$1"
  local hvName=$2
  while IFS= read -r line; do
    if [[ $line == *"$hvName"* ]]; then
      echo "$(cut -d' ' -f1 <<<"$line")"
    fi
  done <<< "$fileData"
}

function createMetadataFile() {
  local fileData=$(cat $SOURCEFILE)
  echo -e "["default"]\nversion = $VERSION.${securityversion}\ndownloadrepository = $CS_SYSTEMTEMPLATE_REPO\n" >> $METADATAFILE
  for template in "${templates[@]}"
  do
    section="${template%%:*}"
    sectionHv="${section%%-*}"
    hvName=$(getGenericName $sectionHv)
    guestos=$(getGuestOS $sectionHv)

    downloadurl="${template#*:}"
    arch=$(echo ${downloadurl#*"/systemvmtemplate-$VERSION-"} | cut -d'-' -f 1)
    templatename="systemvm-${sectionHv%.*}-${VERSION}-${arch}"
    checksum=$(getChecksum "$fileData" "$VERSION-${arch}-$hvName")
    filename=$(echo ${downloadurl##*'/'})
    echo -e "["$section"]\ntemplatename = $templatename\nchecksum = $checksum\ndownloadurl = $downloadurl\nfilename = $filename\narch = $arch\nguestos = $guestos\n" >> $METADATAFILE
  done
}

declare -a templates
getTemplateVersion $1
declare -A template_specs=(
  [kvm-x86_64]="x86_64-kvm.qcow2.bz2"
  [kvm-aarch64]="aarch64-kvm.qcow2.bz2"
  [vmware]="x86_64-vmware.ova"
  [xenserver]="x86_64-xen.vhd.bz2"
  [hyperv4]="x86_64-hyperv.vhd.zip"
  [lxc]="x86_64-kvm.qcow2.bz2"
  [ovm3]="x86_64-ovm.raw.bz2"
)

templates=()
for key in "${!template_specs[@]}"; do
  url="${CS_SYSTEMTEMPLATE_REPO}/systemvmtemplate-$VERSION-${template_specs[$key]}"
  templates+=("$key:$url")
done

PARENTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )/dist/systemvm-templates/"
mkdir -p $PARENTPATH
METADATAFILE=${PARENTPATH}"metadata.ini"
echo > $METADATAFILE
SOURCEFILE=${PARENTPATH}'sha512sum.txt'
createMetadataFile
