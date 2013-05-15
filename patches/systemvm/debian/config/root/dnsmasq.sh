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
  printf "Usage: %s:  <path to new dnsmasq config file>\n" $(basename $0) >&2
}

set -x
#backup the old config file
cp /etc/dnsmasq.conf /etc/dnsmasq.conf.bak

#apply the new confg
echo $1
cp $1 /etc/dnsmasq.conf

#restart the dnsmasq
service dnsmasq restart
result=$?
if [ "$result" -ne "0" ]
then
   echo "could not configure dnsmasq"
   echo "reverting to the old config"
   cp /etc/dnsmasq.config.bak /etc/dnsmasq.conf
   service dnsmasq restart
   exit 2
fi
rm $1
echo "success"
