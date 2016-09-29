#!/usr/bin/env python
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
#
# Usage: python gen-l10n.py <path to message properties file> <output directory>

import codecs
import json
import os
import sys
from optparse import OptionParser


def generateL10nFile(propFile, outputFile):
    ts = {}
    with open(propFile, 'r') as f:
        for line in f.read().split('\n'):
            if line.startswith('#') or line.startswith('\n') or line.startswith('\r') or line.strip() == "":
                continue
            key, _, value = line.partition('=')
            if key in ts:
                print("[Warning] Found a duplicate translation for key " + key)
            value = value.replace('\#', '#') \
                         .replace('\=', '=') \
                         .replace('\!', '!') \
                         .replace('\:', ':') \
                         .replace('\+', '+') \
                         .replace('\,', ',') \
                         .replace('\>', '>') \
                         .replace('\<', '<') \
                         .replace('\\>', '>') \
                         .replace('\\<', '<') \
                         .replace('\\,', ',') \
                         .replace('\\ ', ' ') \
                         .replace('\\+', '+') \
                         .replace('\\\\', '') \
                         .decode('unicode-escape')
            ts[key] = value

    print("Exporting compiled dictionary: %s" % outputFile)
    with codecs.open(outputFile, "w", "utf-8") as f:
        f.write("// Licensed to the Apache Software Foundation (ASF) under one\n")
        f.write("// or more contributor license agreements.  See the NOTICE file\n")
        f.write("// distributed with this work for additional information\n")
        f.write("// regarding copyright ownership.  The ASF licenses this file\n")
        f.write("// to you under the Apache License, Version 2.0 (the\n")
        f.write("// \"License\"); you may not use this file except in compliance\n")
        f.write("// with the License.  You may obtain a copy of the License at\n")
        f.write("//\n")
        f.write("//   http://www.apache.org/licenses/LICENSE-2.0\n")
        f.write("//\n")
        f.write("// Unless required by applicable law or agreed to in writing,\n")
        f.write("// software distributed under the License is distributed on an\n")
        f.write("// \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n")
        f.write("// KIND, either express or implied.  See the License for the\n")
        f.write("// specific language governing permissions and limitations\n")
        f.write("// under the License.\n")
        f.write("var dictionary = ")
        f.write(json.dumps(ts, ensure_ascii=False, separators=(',\n', ':',), sort_keys=True))
        f.write(";")


def parseFileName(propFileName):
    return propFileName.split('messages_')[-1] \
                       .replace('properties', 'js') \
                       .replace('messages.js', 'en.js')


def main():
    parser = OptionParser()
    parser.add_option("-o", "--output", dest="outputDir",
                      help="The path to the generated l10n js file")

    parser.add_option("-i", "--input", dest="inputDir",
                      help="The path to source messages properties files")

    (options, args) = parser.parse_args()
    if options.inputDir is None or options.outputDir is None:
        print("Please provide messages and l10n output directory paths")
        sys.exit(1)

    if not os.path.exists(options.outputDir):
        os.makedirs(options.outputDir)

    for propFile in os.listdir(options.inputDir):
        inputFile = "%s/%s" % (options.inputDir, propFile)
        outputFile = "%s/%s" % (options.outputDir, parseFileName(propFile))
        generateL10nFile(inputFile, outputFile)


if __name__ == "__main__":
    main()
