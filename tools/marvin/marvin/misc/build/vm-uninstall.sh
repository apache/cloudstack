
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

# Uninstalls a given VM

usage() {
  printf "Usage: %s: -n <vm name>\n" $(basename $0) >&2
  exit 2
}

vmname=
while getopts 'n:' OPTION
do
    case $OPTION in
        n)    vmname="$OPTARG"
            ;;
        ?)    usage
            exit 1
            ;;
    esac
done

for vdi_uuid in $(xe vdi-list name-label=$vmname | grep ^uuid | awk '{print $5}')
do
    xe vdi-unlock --force uuid=$vdi_uuid
    xe vdi-destroy uuid=$vdi_uuid
done
xe vm-uninstall force=true vm=$vmname
