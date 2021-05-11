#!/usr/bin/env bash

configFile='./public/config.json'
tmpFile='./public/config.json.tmp'

echo "Post-build: removing all docHelp suffixes in ${configFile}"
node > ${tmpFile} <<EOF
//Read data
var data = require('${configFile}');

//Manipulate data
data.docHelpMappings = {};

//Output data
console.log(JSON.stringify(data, null, 2));

EOF

mv ${tmpFile} ${configFile}