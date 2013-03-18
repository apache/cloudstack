Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

===========================================================

# Setting up Tools and Environment

    - Install VirtualBox 4.2 or latest
    - Tool for exporting appliances: qemu-img, vboxmanage, vhd-util
    - Install [RVM](https://rvm.io/rvm/install)
    - Setup paths:
          export PATH=~/.rvm/bin:$PATH
    - Install Ruby 1.9.3, if it installed some other version:
          rvm install 1.9.3
    - Install bundler: (if you get any openssl issue see https://rvm.io/packages/openssl)
          gem install bundler

All the dependencies will be fetched automatically.

To save some time if you've downloaded iso of your distro, put the isos in:
tools/appliance/iso/

Note, gem may require gcc-4.2, make sure link exists:

    sudo ln -s /usr/bin/gcc /usr/bin/gcc-4.2

# How to build SystemVMs automatically

Just run build.sh, it will export archived appliances for KVM, Xen,
VMWare and HyperV in `dist`:

    sh build.sh [systemvmtemplate|systemvmtemplate64]

# Building SystemVM template appliance manually

List available appliances one can build:

    veewee vbox list

Modify scripts in definitions/*appliance*/ as per needs.
Build systemvm template appliance:

    veewee vbox build 'systemvmtemplate'

Start the box:

    veewee vbox up 'systemvmtemplate'

Halt the box:

    veewee vbox halt 'systemvmtemplate'

Now VirtualBox can be used to export appliance.
