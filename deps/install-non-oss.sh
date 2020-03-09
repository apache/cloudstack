#!/bin/sh
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

# From https://devcentral.f5.com
# Version: unknown
mvn install:install-file -Dfile=cloud-iControl.jar      -DgroupId=com.cloud.com.f5     -DartifactId=icontrol        -Dversion=1.0   -Dpackaging=jar

# From Citrix
# Version: unknown
mvn install:install-file -Dfile=cloud-netscaler-sdx.jar -DgroupId=com.cloud.com.citrix -DartifactId=netscaler-sdx   -Dversion=1.0   -Dpackaging=jar

# From https://my.vmware.com/group/vmware/get-download?downloadGroup=VSP510-WEBSDK-510
# Version: 5.1, Release-date: 2012-09-10, Build: 774886
mvn install:install-file -Dfile=vim25_51.jar        -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim25    -Dversion=5.1   -Dpackaging=jar

# From https://my.vmware.com/group/vmware/get-download?downloadGroup=WEBSDK550
mvn install:install-file -Dfile=vim25_55.jar        -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim25    -Dversion=5.5   -Dpackaging=jar

# From https://my.vmware.com VMware-vSphere-SDK-6.0.0-3634981.zip
mvn install:install-file -Dfile=vim25_60.jar        -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim25    -Dversion=6.0   -Dpackaging=jar

# From https://my.vmware.com/group/vmware/get-download?downloadGroup=VS-MGMT-SDK65
mvn install:install-file -Dfile=vim25_65.jar        -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim25    -Dversion=6.5   -Dpackaging=jar

# From https://my.vmware.com/group/vmware/details?downloadGroup=WEBCLIENTSDK67U2&productId=742
mvn install:install-file -Dfile=vim25_67.jar        -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim25    -Dversion=6.7   -Dpackaging=jar
