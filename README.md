Apache CloudStack Version 4.2.0

# About Apache CloudStack

Apache CloudStack is software designed to deploy 
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

Visit us at [Apache CloudStack](http://cloudstack.apache.org).

## Mailing lists
[Development Mailing List](mailto:dev-subscribe@cloudstack.apache.org)
[Users Mailing List](mailto:users-subscribe@cloudstack.apache.org)
[Commits Mailing List](mailto:commits-subscribe@cloudstack.apache.org)
[Issues Mailing List](mailto:issues-subscribe@cloudstack.apache.org)
[Marketing Mailing List](mailto:marketing-subscribe@cloudstack.apache.org)

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

See the INSTALL file.

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
