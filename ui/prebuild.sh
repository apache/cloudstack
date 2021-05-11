#!/usr/bin/env bash

configFile='./public/config.json'
tmpFile='./public/config.json.tmp'
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
