Apache CloudStack (Incubating) Version 4.0.0

# About Apache CloudStack (Incubating)

Apache CloudStack (Incubating) is software designed to deploy 
and manage large networks of virtual machines, as a highly 
available, highly scalable Infrastructure as a Service (IaaS) 
cloud computing platform. CloudStack is used by a number of 
service providers to offer public cloud services, and by many 
companies to provide an on-premises (private) cloud offering.

Apache CloudStack currently supports the most popular hypervisors: 
VMware, Oracle VM, KVM, XenServer and Xen Cloud Platform. 
CloudStack also offers bare metal management of servers, 
using PXE to provision OS images and IPMI to manage the server. 
Apache CloudStack offers three methods for managing cloud 
computing environments: an easy to use Web interface, command 
line tools, and a full-featured RESTful API.

Visit us at [cloudstack.org](http://incubator.apache.org/cloudstack).

## Mailing lists
[Development Mailing List](mailto:cloudstack-dev-subscribe@incubator.apache.org)
[Users Mailing list](mailto:cloudstack-users-subscribe@incubator.apache.org)
[Commits mailing list](mailto:cloudstack-commits-subscribe@incubator.apache.org)

# License

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

# Building CloudStack

By default, CloudStack will only build with supporting packages
that are appropved by the ASF as being compatible with the Apache
Software License Version 2.

## Default build

To build the default build target, use maven3 and execute:

mvn install

## Including optional third party libraries in your build

If you want to build this software against one of the optional 
third party libraries, follow the instructions below:

These third parties jars are non available in Maven central, and
need to be located and downloaded by the developer themselves.
The libraries to download are listed below, by the feature that
they support.

For F5 load balancing support:
cloud-iControl.jar     

For Netscaler support:
cloud-netscaler.jar    
cloud-netscaler-sdx.jar

For NetApp Storage Support:
cloud-manageontap.jar  

For VMware Support:
vmware-vim.jar         
vmware-vim25.jar       
vmware-apputils.jar    

Once downloaded (and named the same as listed above), they can be 
installed into your local maven repository with the following command: 

cd deps&&sh ./install-non-oss.sh

To perform the build, run the following command:

mvn -Dnonoss install

## Running a developer environment

To run the webapp client:

mvn org.apache.tomcat.maven:tomcat7-maven-plugin:2.0-beta-1:run -pl :cloud-client-ui -am -Pclient

Then hit: http://localhost:8080/cloud-client-ui/

or add in your ~/.m2/settings.xml
  <pluginGroups>
    <pluginGroup>org.apache.tomcat.maven</pluginGroup>
  </pluginGroups>
and save your fingers with mvn tomcat7:run -pl :cloud-client-ui -am -Pclient 

Optionally add -Dnonoss to either of the commands above.

If you want to use ide debug: replace mvn with mvnDebug and attach your ide debugger to port 8000

# Notice of Cryptographic Software

This distribution includes cryptographic software. The country in which you currently 
reside may have restrictions on the import, possession, use, and/or re-export to another 
country, of encryption software. BEFORE using any encryption software, please check your 
country's laws, regulations and policies concerning the import, possession, or use, and 
re-export of encryption software, to see if this is permitted. See http://www.wassenaar.org/ 
for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has 
classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which 
includes information security software using or performing cryptographic functions with 
asymmetric algorithms. The form and manner of this Apache Software Foundation distribution 
makes it eligible for export under the License Exception ENC Technology Software 
Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 
740.13) for both object code and source code.

The following provides more details on the included cryptographic software: 

  CloudStack makes use of JaSypt cryptographic libraries

  CloudStack has a system requirement of MySQL, and uses native database encryption 
  functionality. 

  CloudStack makes use of the Bouncy Castle general-purpose encryption library.

  CloudStack can optionally interacts with and controls OpenSwan-based VPNs.

  CloudStack has a dependency on Apache WSS4J as part of the AWSAPI implementation. 

  CloudStack has a dependency on and makes use of JSch - a java SSH2 implementation. 
