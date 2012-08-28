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

# Apache CloudStack 

Apache CloudStack is a massively scalable free/libre open source Infrastructure as a Service cloud platform. 

Visit us at [cloudstack.org](http://cloudstack.org) or join #cloudstack on irc.freenode.net

## Binary Downloads

Downloads are available from: 
http://cloudstack.org/download.html

## Supported Hypervisors

* XenServer
* KVM 
* VMware ESX/ESXi (via vCenter)
* Oracle VM
* XCP

## Mailing lists
[Development Mailing List](mailto:cloudstack-dev-subscribe@incubator.apache.org)
[Users Mailing list](mailto:cloudstack-users-subscribe@incubator.apache.org)
[Commits mailing list](mailto:cloudstack-commits-subscribe@incubator.apache.org)

#Maven build
Some third parties jars are non available in Maven central.
So install it with: cd deps&&sh ./install-non-oss.sh
Now you are able to activate nonoss build with adding -Dnonoss to maven cli.

to run webapp client:
mvn org.apache.tomcat.maven:tomcat7-maven-plugin:2.0-beta-1:run -pl :cloud-client-ui -am
then hit: http://localhost:8080/cloud-client-ui/
or add in your ~/.m2/settings.xml
  <pluginGroups>
    <pluginGroup>org.apache.tomcat.maven</pluginGroup>
  </pluginGroups>
and save your fingers with mvn tomcat7:run -pl :cloud-client-ui -am

