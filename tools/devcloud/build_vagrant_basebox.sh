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

# Assumes that rvm is installed and you have ruby 1.9.2 installed
# Assumes that you have virtual box installed

mkdir ~/builddevcloud
cd ~/builddevcloud
git clone https://github.com/jedi4ever/veewee.git
#TODO: We need to get this patched version of Vagrant to the upstream project
#      or implement the desired changes to Vagrant as plugin modules and
#      host it within the ASF git repo
git clone https://github.com/chipchilders/vagrant.git
export rvm_trust_rvmrcs_flag=1
cd veewee
bundle install
rake install
vagrant basebox define 'devcloudbase' 'ubuntu-12.04-server-i386'
wget --no-check-certificate -O ./definitions/devcloudbase/definition.rb https://git-wip-us.apache.org/repos/asf\?p\=incubator-cloudstack.git\;a\=blob_plain\;f\=tools/devcloud/veewee/definition.rb\;hb\=HEAD
wget --no-check-certificate -O ./definitions/devcloudbase/postinstall.sh https://git-wip-us.apache.org/repos/asf\?p\=incubator-cloudstack.git\;a\=blob_plain\;f\=tools/devcloud/veewee/postinstall.sh\;hb\=HEAD
wget --no-check-certificate -O ./definitions/devcloudbase/preseed.cfg https://git-wip-us.apache.org/repos/asf\?p\=incubator-cloudstack.git\;a\=blob_plain\;f\=tools/devcloud/veewee/preseed.cfg\;hb\=HEAD
vagrant basebox build 'devcloudbase' -f -a -n
# possibly use -r here too ^
vagrant basebox export 'devcloudbase' -f
vagrant basebox destroy 'devcloudbase' -f
vagrant box add 'devcloud' 'devcloudbase.box' -f
rm -f devcloudbase.box

