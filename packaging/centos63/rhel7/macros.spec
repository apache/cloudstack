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

%define _pythonparamiko %{nil}
%define _javaversion java => 1.7.0
%define _tomcatversion tomcat => 7.0
%define _vlanconfigtool iproute
%define _tomcatpathname tomcat
%define _managementstartscriptpath %{_sbindir}
%define _managementservice install -D packaging/centos63/%{_os}/cloud-management.service ${RPM_BUILD_ROOT}%{_unitdir}/%{name}-management.service
%define _managementserviceattribute %attr(0755,root,root) %{_unitdir}/%{name}-management.service
%define _iptablesservice Requires: iptables-services
%define _serverxmlname server7
%define _cloudstackmanagementconf install -D packaging/centos63/%{_os}/%{name}-management.conf ${RPM_BUILD_ROOT}%{_tmpfilesdir}/%{name}-management.conf
%define _cloudstackmanagementconfattr %attr(0755,root,root) %{_tmpfilesdir}/%{name}-management.conf
