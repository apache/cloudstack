> Licensed to the Apache Software Foundation (ASF) under one
> or more contributor license agreements.  See the NOTICE file
> distributed with this work for additional information
> regarding copyright ownership.  The ASF licenses this file
> to you under the Apache License, Version 2.0 (the
> "License"); you may not use this file except in compliance
> with the License.  You may obtain a copy of the License at
> 
>   http://www.apache.org/licenses/LICENSE-2.0
> 
> Unless required by applicable law or agreed to in writing,
> software distributed under the License is distributed on an
> "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
> KIND, either express or implied.  See the License for the
> specific language governing permissions and limitations
> under the License.


---------------------------------------------------------------------------
This README describes the various tools available with Apache Cloudstack -
for compiling, deploying, building and testing the project
---------------------------------------------------------------------------

DevCloud
=========================================================
Under tools/devcloud

NOTE - DevCloud (tools/devcloud) is a work in progress.  The project has not
determined how to best establish a nightly DevCloud build process, or how to
distribute the image. 

#### Contents: ####

Under tools/devcloud are various scripts used to build the devcloud image.
devcloudsetup.sh - the origional devcloud build script (assumes an Ubuntu 12.04
VM image)

        $ cd tools/devcloud

* build_vagrant_basebox.sh - a script that uses VirtualBox, VeeWee, Vagrant
(patched) and puppet to create a devcloud basebox
* veewee - configuration files used to build a basic Ubuntu 12.04 vagrant box
via VeeWee
* basebuild - The Vagrantfile and puppet module that gets applied to the basic
Ubuntu 12.04 box
* devcloudbox - The Vagrantfile and puppet module that is used with the
[hopefully] distributed devcloud base box

#### Instructions: ####

To build a "devcloud base box", run you need a system with VirtualBox and rvm
installed (use ruby 1.9.2).  Run build_vagrant_basebox.sh to build the base
box.

To use the "devcloud base box" that is created in the previous step, you need
to have installed a forked version of Vagrant (until we make the changes
plugins instead of direct source patches) that can be found here:

Once installed per the Vagrant installation process, run:

        $ vagrant box add devcloud [path to devcloud.box]

Then, either go into the devcloudbox folder of your checked out version of the
CloudStack code (cloudstack/tools/devcloud/devcloudbox), or copy the
contents of that folder to another location.

Assuming the patched Vagrant installation is working, you then
simply run "vagrant up" from within that directory.

#### Installation ####

Install DevCloud Base system:

1. get code from https://github.com/jedi4ever/veewee, and install
2. veewee vbox define devcloud ubuntu-12.04-server-i386
3. put these two files(definition.rb and preseed.cfg) under ./definition/devcloud/
3. veewee vbox build devcloud


Marvin
=========================================================
Under tools/marvin

Marvin is the functional testing framework for CloudStack written in python.
Writing of unittests and functional tests with Marvin makes testing with
cloudstack easier 

Visit the
[wiki](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Testing+with+Python)
for the most updated information

#### Dependencies ####
Marvin will require the following dependencies, these will be automatically
downloaded from the python cheeseshop when you install Marvin.

- mysql-connector-python, 
- paramiko,
- nose,
- unittest-xml-reporting,

#### Installation ####

        $ untar Marvin-0.1.0.tar.gz 
        $ cd Marvin-0.1.0
        $ python setup.py install

#### Features ####

1. very handy cloudstack API python wrapper
2. support async job executing in parallel
3. remote ssh login/execute command
4. mysql query 

#### Examples ####

Examples on how to develop your own configuration can be found in the marvin sandbox.
Under tools/marvin/marvin/sandbox

To generate the config for a deployment. Alter the .properties file in the sandbox. For example the
simualtordemo.properties after modification can generate the config file as
shown below

        $ python simulator_setup.py -i simulatordemo.properties -o simulatordemo.cfg

To deploy the environment and run the tests

        $ python -m marvin.deployAndRun -c simulatordemo.cfg -t /tmp/t.log -r /tmp/r.log -d testcase

#### Tests #### 

Functional Tests written using marvin can be found under test/integration
folder. These are tests that are written to be run against a live deployed
system.

To run the tests - you should have marvin installed and correctly importable.
The tests are long running and are best monitored by external hudson jobs.

Also you will have to point marvin to the right configuration file that has
details about your cloudstack deployment. For more help on how to write the
config file and run tests check the tutorial at :

[] (https://cwiki.apache.org/confluence/display/CLOUDSTACK/Testing+with+Python)

#### Build Verification Testing (BVT) ####

These test cases are the core functionality tests that ensure the application
is stable and can be tested thoroughly.  These BVT cases definitions are
located at :
[] (https://docs.google.com/a/cloud.com/spreadsheet/ccc?key=0Ak8acbfxQG8ndEppOGZSLV9mUF9idjVkTkZkajhTZkE&invite=CPij0K0L)

##### Guidelines on tests #####

BVT test cases are being developed using Python unittests2. Following are
certain guidelines being followed

1. Tests exercised for the same resource should ideally be present under a
single suite or file.

2. Time-consuming operations that create new cloud resources like server
creation, volume creation etc should not necessarily be exercised per unit
test. The resources can be shared by creating them at the class-level using
setUpClass and shared across all instances during a single run.

3. Certain tests pertaining to NAT, Firewall and Load Balancing warrant fresh
resources per test. Hence a call should be taken by the stakeholders regarding
sharing resources.

4. Ensure that the tearDown/tearDownClass functions clean up all the resources
created during the test run.  

For more information about unittests: [] (http://docs.python.org/library/unittest.html)

##### BVT Tests #####
Under test/integration/smoke

The following files contain these BVT cases:

1. test_vm_life_cycle.py - VM Life Cycle tests
2. test_volumes.py - Volumes related tests
3. test_snapshots.py - Snapshots related tests
4. test_disk_offerings.py - Disk Offerings related tests
5. test_service_offerings.py - Service Offerings related tests
6. test_hosts.py - Hosts and Clusters related tests
7. test_iso.py - ISO related tests
8. test_network.py - Network related tests
9. test_primary_storage.py - Primary storage related tests
10. test_secondary_storage.py - Secondary storage related tests
11. test_ssvm.py - SSVM & CPVM related tests
12. test_templates.py - Templates related tests
13. test_routers.py - Router related tests


##### P1 Tests #####
Under test/integration/component

These test cases are the core functionality tests that ensure the application
is stable and can be tested thoroughly.  These P1 cases definitions are located
at :
[] (https://docs.google.com/a/clogeny.com/spreadsheet/ccc?key=0Aq5M2ldK6eyedDJBa0EzM0RPNmdVNVZOWnFnOVJJcHc&hl=en_US)

The following files contain these P1 cases:

1. test_snapshots.py - Snapshots related tests
2. test_routers.py - Router related tests
3. test_usage.py - Usage realted tests
4. test_account.py - Account related tests
5. test_resource_limits.py - Resource limits tests
6. test_security_groups.py - Security groups related tests
7. test_templates.py - templates related tests
8. test_volumes.py - Volumes related tests
9. test_blocker_bugs.py - Blocker bugs tests
10. test_project_configs.py - Project global configuration related tests
11. test_project_limits.py - Project resource limits related tests
12. test_project_resources.py - Project resource creation related tests
13. test_project_usage.py - Project usage related tests
14. test_projects - Projects functionality tests

Marvin Sandbox
=========================================================
In: tools/marvin/marvin/sandbox

In here you should find a few common deployment models of CloudStack that you
can configure with properties files to suit your own deployment. One deployment
model for each of - advanced zone, basic zone and a simulator demo are given.  

$ ls -
basic/
advanced/
simulator/

Each property file is divided into logical sections and should be familiar to
those who have deployed CloudStack before. Once you have your properties file
you will have to create a JSON configuration of your deployment using the
python script provided in the respective folder.

The demo files are from the tutorial for testing with python that can be found at
   https://cwiki.apache.org/confluence/display/CLOUDSTACK/Testing+with+Python

A common deployment model of a simulator.cfg that can be used for debugging is
included. This will configure an advanced zone with simulators that can be used
for debugging purposes when you do not have hardware to debug with.

To do this:
$ cd cloudstack-oss/
$ ant run-simulator #This will start up the mgmt server with the simulator seeded

## In another shell
$ ant run-simulator

test/conf - EC2 script
=========================================================

To run submitCertEC2 and deleteCertEC2 scripts, update parameters in conf/tool.properties file:

* host - ip address of the host where cloud-bridge software is installed
* port - port cloud-bridge software is listening to
* accesspoint - access point for cloud-bridge REST request
* version - Amazon EC2 api version supported by cloud-bridge
* signaturemethod - HmacSHA1 or HmacSHA256
* expires - the date when certificate expires
