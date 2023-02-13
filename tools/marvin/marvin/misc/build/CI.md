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


about
=====

This document talks about the *evolving* continuous test infrastructure used to setup, deploy, configure and test Apache CloudStack. Information here is useful for anyone involved in build, test, continuous integration even operators of CloudStack.

components
. nightly yum/apt repositories
. cobbler
    .. rhel / ubuntu/ debian kickstarts
    .. hypervisor kickstarts
    .. adding new profiles
. puppet
. dnsmasq 
. ntpd
. jenkins jnlp slave
. scheduling
. networking setup


[insert diagram here]

The above illustration shows a high-level view of the test infrastructure setup. In this section we explain the tools and their organization in the infrastructure. The workflow detailed in a later section shows how this setup works together:

1. At the center of the workflow is the "driver" appliance that manages the infrastructure. This is a Cent OS 6.2 VM running on a XenServer. The "driver"-VM is responsible for triggering the process when it is time for a test run.

The driver appliance is composed of the following parts:

a. Cobbler - cobbler is a provisioning PXE server (and much more) useful for rapid setup of linux machines. It can do DNS, DHCP, power management and package configuration via puppet. It is capable of managing network installation of both physical and virtual infrastructure. Cobbler comes with an expressive CLI as well as web-ui frontends for management.

Cobbler manages installations through profiles and systems:

profiles - these are text files called kickstarts defined for a distribution's installation. For eg: RHEL 6.1 or Ubuntu 12.04 LTS. Each of the machines in the test environment - hypervisors and cloudstack management servers contains a profile in the form of a kickstart.  

The profile list looks as follows:

[root@infra ~]# cobbler profile list
   cloudstack-rhel
   cloudstack-ubuntu
   rhel63-kvm
   rhel63-x86_64
   ubuntu1204-x86_64
   xen602
   xen56

systems - these are virtual/physical infrastructure mapped to cobbler profiles based on the hostnames of machines that can come alive within the environment.

[root@infra ~]# cobbler system list
   acs-qa-h11
   acs-qa-h20
   acs-qa-h21
   acs-qa-h23
   cloudstack-rhel
   cloudstack-ubuntu

When a new image needs to be added we create a 'distro' in cobbler and associate that with a profile's kickstart. Any new systems to be hooked-up to be serviced by the profile can then be added easily by cmd line.

b. Puppet master - Cobbler reimages machines on-demand but it is upto puppet recipes to do configuration management within them. The configuration management is required for kvm hypervisors (kvm agent for eg:) and for the cloudstack management server which needs mysql, cloudstack, etc. The puppetmasterd daemon on the driver-vm is responsible for 'kicking' nodes to initiate configuration management on themselves when they come alive. 

So the driver-vm is also the repository of all the puppet recipes for various modules that need to be configured for the test infrastructure to work. The modules are placed in /etc/puppet and bear the same structure as our GitHub repo. When we need to affect a configuration change on any of our systems we only change the GitHub repo and the systems in place are affected upon next run.

c. dnsmasq - DNS is controlled by cobbler but its configuration of hosts is set within dnsmasq.d/hosts. This is a simple 1-1 mapping of hostnames with IPs. For the most part this should be the single place where one needs to alter for replicating the test setup. Everywhere else only DNS names are/should-be used. open ports 53, 67 on server

d. dhcp - DHCP is also done by dnsmasq. All configuration is in /etc/dnsmasq.conf. static mac-ip-name mappings are given for hypervisors while the virtual instances get dynamic ips

e. ipmitool - ipmi for power management is setup on all the test servers and the ipmitool provides a convienient cli for booting the machines on the network into PXEing.

f. jenkins-slave - jenkins slave.jar is placed on the driver-vm as a service in /etc/init.d to react to jenkins schedules and to post reports to. The slave runs in headless mode as the driver-vm does not run X.

g. ntpd - ntp daemon is running and syncing time for all machines in the system. puppet depends on times to be in sync when configuring nodes. So does the management server when deployed in a cluster

h. puppet - set puppetmaster to listen on 8140

2. NFS storage - the nfs server is a single server serving as both primary and secondary storage. This is likely a limitation when compared to true production deployments but serves in good stead for a test setup. Where it becomes a limitation is in testing different storage backends. Object stores, local storage, clustered local storage etc are not addressed by this setup.

3. Hypervisor hosts - There currently are 4 hosts in this environment. These are arranged at the moment in three pods so as to be capable of being deployed in a two zone environment. One zone with two pods and and a second zone with a single pod. This covers tests that depend on 
a. single zone/pod/cluster
b. multiple cluster
c. inter-zone tests
d. multi-pod tests


marvin integration
==================

once cloudstack has been installed and the hypervisors prepared we are ready to use marvin to stitch together zones, pods, clusters and compute and storage to put together a 'cloud'.  once configured - we perform a cursory health check to see if we have all systemVMs running in all zones and that built-in templates are downloaded in all zones. Subsequently we are able to launch tests on this environment

Only the latest tests from git are run on the setup. This allows us to test in a pseudo-continuous fashion with a nightly build deployed on the environment. Each test run takes a few hours to finish.

control via github
==================

there are two GitHub repositories controlling the test infrastructure. 
a. The puppet recipes at gh:acs-infra-test
b. The gh:cloud-autodeploy repo that has the scripts to orchestrate the overall workflow

workflow
========

When jenkins triggers the job following sequence of actions occur on the test infrastructure

1. The deployment configuration is chosen based on the hypervisor being used. We currently have xen.cfg and kvm.cfg that are in the gh:cloud-autodeploy repo

2. A virtualenv python environment is chosen within which the configuration and test runs by marvin are isolated into. Virtualenv is great for sandboxing test environment runs. In to the virtualenv are copied all the latest tests from the git:incubator-cloudstack repo.

3. we fetch the last successful marvin build from builds.a.o and install it within this virtualenv. installing a new marvin on each run helps us test with the latest APIs available.

4. we fetch the latest version of the driver script from github:cloud-autodeploy. fetching the latest allows us to make adjustments to the infra without having to copy scripts in to the test infrastrcuture.

5. based on the hypervisor chosen we choose a profile for cobbler to reimage the hosts in the infrastructure. if xen is chosen we bring up the profile of the latest xen kickstart available in cobbler. currently - this is at xen 6.0.2. if kvm is chosen we can pick between ubuntu and rhel based host OS  kickstarts.

6. with this knowledge we kick off the driver script with the following cmd line arguments
    $ python configure.py -v $hypervisor -d $distro -p $profile -l $LOG_LVL 

The $distro argument chooses the hostOS of the mgmt server - this can be ubuntu / rhel. LOG_LVL can be set to INFO/DEBUG/WARN for troubleshooting and more verbose log output.

7. The configure script does various operations to prepare the environment:
    a. clears up any dirty cobbler systems from previous runs
    b. cleans up puppet certificates of these systems. puppet recipes will fail if puppetmaster finds an invalid certificate
    c. starts up a new xenserver VM that will act as the mgmt server. we chose to keep things simple by launching the vm on a xenserver. one could employ jclouds via jenkins to deploy the mgmt server VM on a dogfooded cloudstack.
    d. in parallel the deployment config of marvin is parsed through to find the hypervisors that need to be cleaned up, pxe booted and prepared for the cloudstack deployment.
    e. all the hosts in the marvin config are pxe booted via ipmi and cobbler takes over to reimage them with the profile chosen by the jenkins job run.
    f. while this is happening we also seed the secondary storage with the systemvm template reqd for the hypervisor. 
    g. all the primary stores in the marvin config are then cleaned for the next run.

8. While cobbler is reimaging the hosts with the right profiles, the configure script waits until all hosts are reachable over ssh. It also checks for essential services (http, mysql) ports to come up. Cobbler once done with refreshing the machine hands over the reins to puppet.

9. Puppet slaves within the machines in the environment reach out to puppetmaster to get their identity. mgmt server vm fetches its own recipe and starts configuring itself while hypervisors will do the same in case they need to be acting as kvm agents.

10. When the essential ports for mgmt server - 8080 and 8096 are open and listening we know that the mgmt server has come up successfully. We then go ahead and deploy the configuration specified by marvin.

11. After marvin finishes configuring the cloud - it performs a health check to see if the system is ready for running tests upon.

12. Tests are run using the nose test runner with the marvin plugin and reports are recorded by jenkins.

limitations
===========

enhancements
============
- packaging tests
- puppetize the cobbler appliance
- dogfooding
- run test fixes on idle environment upon checkin without deploy
- custom zones - using a marvin config file
- logging enhancements = archiving + syslog
- digest emails via jenkins. controlling spam
- external devices (LB, VPX, FW)
- mcollective?

future
======
- not everyone deploys cloudstack the same
- multiple hv environments with multiple hv configurations
- multiple storage configurations

troubleshooting
===============

acknowledgements
================
