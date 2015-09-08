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

usage() {
    echo "Usage: %s: -f <image location>\n"
}

fflag=
image=

while getopts 'f:' OPTION
do
    case $OPTION in
    f)
        fflag=1
	image="$OPTARG"
        ;;
    ?)
        usage
        exit 2
        ;;
    esac
done

if [ "$fflag" != "1" ]
then
 usage
 exit 2
fi

# Find the virtual size of the disk image
size_in_bytes=$(qemu-img info $image | grep "virtual size" | awk '{print $4}')

# Strip off the leading '('
size_in_bytes=${size_in_bytes:1}

echo "$size_in_bytes"
exit 0
