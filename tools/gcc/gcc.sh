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
  printf "Usage:\n %s [source directory where the java script files are] [destination directory to put the result] \n" $(basename $0) >&2
}

if [ $# -ne 2 ]; then
  usage
  exit 2;
fi

set -x

jsfiles="--js $1/jquery-1.4.min.js --js $1/date.js "
for file in `ls -l $1/jquery*.js | grep -v jquery-1.4 | awk '{print $NF}'`; do
  jsfiles=`echo $jsfiles "--js " $file`
done
jsfiles=`echo $jsfiles "--js $1/cloud.core.callbacks.js --js $1/cloud.core.js "`
for file in `ls -l $1/cloud*.js | egrep -v 'callback|core.js' | awk '{print $NF}'`; do
  jsfiles=`echo $jsfiles "--js " $file`
done

java -jar compiler.jar $jsfiles --js_output_file $2/cloud.core.min.js
