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

days=60
dir=/var/cache/cloud/processed

if ! [ $# -eq 0 ]; then
  re='^[0-9]+$'
  if ! [[ $1 =~ $re ]] ; then
    echo "error: argument must be a number and will be interpreted as a period in days" >&2; exit 1
  else
    days=$1
  fi
  if ! [ -z $2 ]; then
    dir=$2
  fi
fi

files_to_delete=`find . -mtime +${days} 2>/dev/null`

if [ -z "$files_to_delete" ]
then
  echo "numberOfFiles: 0"
  exit 0
fi
errors=`rm $files_to_delete >/dev/null`

if [ $? ]
then
  echo -n "numberOfFiles: "
  echo $files_to_delete | wc -w
  echo "files: " $files_to_delete
else
  echo "ERRORS: " $errors
fi
