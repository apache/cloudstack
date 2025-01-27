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

usage() {
  printf "Usage: %s:\n
	[-m mgmt-server ] \n
	[-c config-file ] \n
	[-d db node url ]\n" $(basename $0) >&2
}

failed() {
	exit $1
}

#defaults
FMT=$(date +"%d_%I_%Y_%s")
MGMT_SVR="localhost"
CONFIG="demo/simulator/simulator-smoke.cfg"
DB_SVR="localhost"

while getopts 'd:m:c:' OPTION
do
  case $OPTION in
  d)    dflag=1
		DB_SVR="$OPTARG"
		;;
  m)    mflag=1
		MGMT_SVR="$OPTARG"
		;;
  c)    cflag=1
		CONFIG="$OPTARG"
		;;
  ?)	usage
		failed 2
		;;
  esac
done

$(mysql -uroot -Dcloud -h$MGMT_SVR -s -N -r -e"update configuration set value='8096' where name='integration.api.port'")
version_tuple=$(python -c 'import sys; print(sys.version_info[:2])')

if [[ $version_tuple == "(2, 7)" ]]
then
    python -m marvin.deployAndRun -c $CONFIG -t /tmp/t.log -r /tmp/r.log -d /tmp
    sleep 60
    python -m marvin.deployAndRun -c $CONFIG -t /tmp/t.log -r /tmp/r.log -f testSetupSuccess.py -l
    cat /tmp/r.log
    echo "Done"
else
    echo "Python version 2.7 not detected on system. Aborting"
fi
