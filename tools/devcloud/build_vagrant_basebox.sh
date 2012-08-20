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
# Assumes that you have wget installed
set -x
PROGNAME=$(basename $0)
function error_exit {

   # Display error message and exit
   echo "${PROGNAME}:  $*" 1>&2
   exit 1
}


# Load RVM into a shell session *as a function*
if [[ -s "$HOME/.rvm/scripts/rvm" ]] ; then
# First try to load from a user install
  source "$HOME/.rvm/scripts/rvm"

elif [[ -s "/usr/local/rvm/scripts/rvm" ]] ; then

# Then try to load from a root install
  source "/usr/local/rvm/scripts/rvm"

else

  printf "ERROR: An RVM installation was not found.\n"

fi

mkdir  ~/builddevcloud || error_exit
cd ~/builddevcloud || error_exit
git clone https://github.com/jedi4ever/veewee.git || error_exit
#TODO: We need to get this patched version of Vagrant to the upstream project
#      or implement the desired changes to Vagrant as plugin modules and
#      host it within the ASF git repo
git clone https://github.com/chipchilders/vagrant.git || error_exit
export rvm_trust_rvmrcs_flag=1 || error_exit
cd vagrant || error_exit
bundle install || error_exit "could not bundle install vagrant"
rake install  || error_exit "could not rake vagrant"
cd ~/builddevcloud/veewee || error_exit
bundle install || error_exit
rake install || error_exit
bundle exec vagrant basebox define 'devcloudbase' 'ubuntu-12.04-server-i386' || error_exit "couldn't basebox define"
wget --no-check-certificate -O ./definitions/devcloudbase/definition.rb https://git-wip-us.apache.org/repos/asf\?p\=incubator-cloudstack.git\;a\=blob_plain\;f\=tools/devcloud/veewee/definition.rb\;hb\=HEAD || error_exit "couldn't get file"
wget --no-check-certificate -O ./definitions/devcloudbase/postinstall.sh https://git-wip-us.apache.org/repos/asf\?p\=incubator-cloudstack.git\;a\=blob_plain\;f\=tools/devcloud/veewee/postinstall.sh\;hb\=HEAD || error_exit "couldn't get file"
wget --no-check-certificate -O ./definitions/devcloudbase/preseed.cfg https://git-wip-us.apache.org/repos/asf\?p\=incubator-cloudstack.git\;a\=blob_plain\;f\=tools/devcloud/veewee/preseed.cfg\;hb\=HEAD || error_exit "couldn't get file"
bundle exec vagrant basebox build 'devcloudbase' -f -a -n || error_exit "couldn't basebox build"
# possibly use -r here too ^
bundle exec vagrant basebox export 'devcloudbase' -f || error_exit "couldn't basebox export"
bundle exec vagrant basebox destroy 'devcloudbase' -f || error_exit "couldn't basebox destroy"
bundle exec vagrant box add 'devcloudbase' 'devcloudbase.box' -f || error_exit "couldn't basebox add"
rm -f devcloudbase.box || error_exit
cd ~/builddevcloud/vagrant || error_exit
mkdir devcloudbase || error_exit
cd devcloudbase || error_exit
mkdir puppet-devcloudinitial || error_exit
mkdir puppet-devcloudinitial/files || error_exit
mkdir puppet-devcloudinitial/manifests || error_exit
wget --no-check-certificate -O Vagrantfile "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/Vagrantfile;hb=HEAD" || error_exit
wget --no-check-certificate -O puppet-devcloudinitial/init.pp "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/puppet-devcloudinitial/init.pp;hb=HEAD" || error_exit
wget --no-check-certificate -O puppet-devcloudinitial/Modulefile "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/puppet-devcloudinitial/Modulefile;hb=HEAD" || error_exit
wget --no-check-certificate -O puppet-devcloudinitial/files/grub "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/puppet-devcloudinitial/files/grub;hb=HEAD" || error_exit
wget --no-check-certificate -O puppet-devcloudinitial/files/interfaces "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/puppet-devcloudinitial/files/interfaces;hb=HEAD" || error_exit
wget --no-check-certificate -O puppet-devcloudinitial/files/network.conf "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/puppet-devcloudinitial/files/network.conf;hb=HEAD" || error_exit
wget --no-check-certificate -O puppet-devcloudinitial/files/xen-defaults "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/puppet-devcloudinitial/files/xen-defaults;hb=HEAD" || error_exit
wget --no-check-certificate -O puppet-devcloudinitial/files/xend "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/puppet-devcloudinitial/files/xend;hb=HEAD" || error_exit
wget --no-check-certificate -O puppet-devcloudinitial/manifests/init.pp "https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=blob_plain;f=tools/devcloud/basebuild/puppet-devcloudinitial/manifests/init.pp;hb=HEAD" || error_exit
cd ~/builddevcloud/vagrant/
bundle install || error_exit
rake install || error_exit
cd ~/builddevcloud/vagrant/devcloudbase/
bundle exec vagrant up || error_exit "vagrant up failed"
bundle exec vagrant halt || error_exit "vagrant halt failed"
bundle exec vagrant package default --output ~/devcloud.box || error_exit "vagrant package failed"
bundle exec vagrant destroy -f || error_exit "vagrant destroy failed"
bundle exec vagrant box remove devcloudbase virtualbox || error_exit "vagrant box remove failed"

echo "Your new devcloud base box is stored in ~/devcloud.box"
