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



 


set -e
set -x


# $1: the dest host where we will run eeeeeeverything

# "${build.schedule.workingDir}/branches/GA-1.0.0-09-11-15/java/test/scripts/deploy-and-run-regression.sh" root@192.168.0.3 "${build.artifactsDir}"  /root/artifacts

dir=`dirname $0`
r=bootstrap-regression.sh

host="$1"
src="$2"
dest="$3"

srcbootstrapper="$dir"/"$r" # that script is launched from this one, remotely via ssh
destbootstrapper="$dest"/"$r" # this is the path on the remote machine


ssh -o BatchMode\ yes $host rm -rf $dest
scp -o BatchMode\ yes -r $src $host:$dest
scp -o BatchMode\ yes -r "$srcbootstrapper" $host:$destbootstrapper

ssh -o BatchMode\ yes $host $destbootstrapper

