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

DIR=$(dirname $0)
configFile="$DIR/public/config.json"
tmpFile="$DIR/public/config.json.tmp"
echo "Pre-build: list all docHelp suffixes in ${configFile}"
for m in $(grep "docHelp: '" -R ./src | sed "s/^.*: '//g" | sed "s/',//g" | sort | uniq); do
    docHelpMappings+="${m},"
done;

node > ${tmpFile} <<EOF
// Read config
var data = require('${configFile}');

// Add docHelpMappings
var suffixes = '${docHelpMappings}';
suffixes = suffixes.split(',');
var mappings = {}
for (const suffix of suffixes) {
  if (suffix) {
    mappings[suffix] = suffix;
  }
}
data.docHelpMappings = mappings;

// Output config
console.log(JSON.stringify(data, null, 2));

EOF

mv ${tmpFile} ${configFile}
