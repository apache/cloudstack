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

    - Install latest VirtualBox (at least 4.2)
    - Install tools for exporting appliances: qemu-img, vboxmanage, vhd-util
    - Install [RVM](https://rvm.io/rvm/install)
    - Install shar
          yum install sharutils
    - Setup paths:
          export PATH=~/.rvm/bin:$PATH
    - Install Ruby 1.9.3, if it installed some other version:
          rvm install 1.9.3
    - Set rvm to use that 1.9.3
          rvm use ruby-1.9.3
    - Install bundler: (if you get any openssl issue see https://rvm.io/packages/openssl)
          gem install bundler

All the dependencies will be fetched automatically.

To save some time if you've downloaded iso of your distro, put the isos in:
tools/appliance/iso/

Note, gem may require gcc-4.2, make sure link exists:

    sudo ln -s /usr/bin/gcc /usr/bin/gcc-4.2

# Setting up jenkins (CI) builds

All the tools listed above are expected to be available. If you follow

    http://rvm.io/integration/jenkins

then you'll need to do a bit of logic to load RVM in jenkins. In the
build script you put into jenkins, start it with
```
#!/bin/bash -l
```

to ensure a login shell, then add something like
```
# inspired by https://github.com/CloudBees-community/rubyci-clickstart/blob/master/bin/run-ci
# also see https://rvm.io/integration/jenkins
# .rvmrc won't get trusted/auto-loaded by jenkins by default
export VAGRANT_HOME=$HOME/.vagrant.d-release-cloudstack
rvm use ruby-1.9.3@vagrant-release-cloudstack --create
# do not use --deployment since that requires Gemfile.lock...and we prefer an up-to-date veewee
bundle_args="--path vendor/bundle"
```


# How to build SystemVMs automatically

Just run build.sh, it will export archived appliances for KVM, XenServer,
VMWare and HyperV in `dist`:

    bash build.sh [systemvmtemplate|systemvmtemplate64]

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

To build the systemvm64template by hand using veewee, set VM_ARCH=amd64 and use
the systemvmtemplate:

    export VM_ARCH=amd64
    cp -r definitions/systemvmtemplate definitions/systemvm64template
    veewee vbox build 'systemvm64template'

Troubleshooting
===============
If you see following line in the screen, then veewee is failing 
extracting vboxmanage version.

    Downloading vbox guest additions iso v  - http://download.virtualbox.org/vi

You would be able to check it manually by typing:

    vboxmanage --version

If you're using Fedora for example, you'll need to install `kernel-devel`
package and run `/etc/init.d/vboxdrv setup` to get veewee working.

Testing
=======
The ./test.sh script tries out a few different default ways to invoke build.sh.

See ../vagrant/systemvm for a test setup that uses vagrant+serverspec to
provide actual integration tests that verify the built systemvm is up to spec.
