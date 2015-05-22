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

#set -x

cfg=
version=
log=/var/log/cloud.log

log_it() {
    logger -t cloud "$*"
    echo "$(date) : $*" >> $log
}

while getopts 'c:' OPTION
do
  case $OPTION in
      c) cfg="$OPTARG"
          ;;
  esac
done

while read line
do
    #comment
    if [[ $line == \#* ]]
    then
	    continue
    fi

    if [ "$line" == "<version>" ]
    then
        read line
        version=$line
        log_it "VR config: configuation format version $version"
        #skip </version>
        read line
        continue
    fi

    if [ "$line" == "<script>" ]
    then
        read line
        log_it "VR config: executing: $line"
        eval $line >> $log 2>&1
        if [ $? -ne 0 ]
        then
            log_it "VR config: executing failed: $line"
            # expose error info to mgmt server
            echo "VR config: execution failed: \"$line\", check $log in VR for details " 1>&2
            exit 1
        fi
        #skip </script>
        read line
        log_it "VR config: execution success "
        continue
    fi

    if [ "$line" == "<file>" ]
    then
        read line
        file=$line
        log_it "VR config: creating file: $file"
        rm -f $file
        while read -r line
        do
            if [ "$line" == "</file>" ]
            then
                break
            fi
            echo $line >> $file
        done
        log_it "VR config: create file success"
        continue
    fi
done < $cfg

#remove the configuration file, log file should have all the records as well
rm -f $cfg

exit 0
