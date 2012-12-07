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

NOTE - This folder is a work in progress.  The project has not determined
how to best establish a nightly DevCloud build process, or how to distribute
the image.


===========================================================
# How to use devcloud

Install RVM with the latest ruby 1.9.3 patch set (not documented)


cd deps

	cd deps/
	#cleans up any old stuff
	./boxer.sh -c all
	#builds the dependent vms
	./boxer.sh -b all


After that is complete

	#go back to the devcloud homedir
	cd ../
	#bring up the devcloud vm
	vagrant up

If you get a vagrant error, at that point, try:

	source .rvmrc
	vagrant up

If you want to compile cloudstack in the devcloud vm:

	vim puppet/modules/devcloud/manifests/params.pp

and set

	$build_cloudstack = true

alternately, if you do not want to build cloudstack in the devcloud vm, set:

	$build_cloudstack = false


It will now bring up the devcloud vm for this first time.  Note that it will attempt to download the SSVM and CPVM templates so it will take a long time to launch initially.  It will also git clone the cloudstack repository and attempt to build an launch it.

You can optionally speed things up by packaging a successful devcloud instance build.  This will make subsequent launches must faster since it won't have to re-downoad the SSVM and CPVM.  Once it has successfully been built, you can run:

    #exports the devcloud vagrant instance and adds it as "devcloud" to vagrant boxlist
	./boxit.sh
	#modifies the Vagrant file to use this newly added instance
	sed -i 's,devcloudbase-xen,devcloud,g' Vagrantfile
