#!/usr/bin/env bash

configFile='./public/config.json'
tmpFile='./public/config.json.tmp'

echo "Post-build: removing all docHelp suffixes in ${configFile}"
node > ${tmpFile} <<EOF
// Read config
var data = require('${configFile}');

// Clear docHelpMappings
data.docHelpMappings = {};

// Output config
console.log(JSON.stringify(data, null, 2));

EOF

mv ${tmpFile} ${configFile}
