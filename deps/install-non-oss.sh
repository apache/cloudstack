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

# https://my.vmware.com/group/vmware/downloads/get-download?downloadGroup=VS-MGMT-SDK700
mvn install:install-file -Dfile=vim25_70.jar  -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim25   -Dversion=7.0   -Dpackaging=jar

# https://my.vmware.com/group/vmware/downloads/get-download?downloadGroup=VS-MGMT-SDK80
mvn install:install-file -Dfile=vim25_80.jar  -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim25   -Dversion=8.0   -Dpackaging=jar

# From https://my.vmware.com/group/vmware/get-download?downloadGroup=VS-MGMT-SDK65
mvn install:install-file -Dfile=pbm_65.jar -DgroupId=com.cloud.com.vmware -DartifactId=vmware-pbm -Dversion=6.5 -Dpackaging=jar

# From https://my.vmware.com/group/vmware/downloads/get-download?downloadGroup=VS-MGMT-SDK67
mvn install:install-file -Dfile=pbm_67.jar -DgroupId=com.cloud.com.vmware -DartifactId=vmware-pbm -Dversion=6.7 -Dpackaging=jar

# https://my.vmware.com/group/vmware/downloads/get-download?downloadGroup=VS-MGMT-SDK700
mvn install:install-file -Dfile=pbm_70.jar    -DgroupId=com.cloud.com.vmware -DartifactId=vmware-pbm     -Dversion=7.0   -Dpackaging=jar

# https://my.vmware.com/group/vmware/downloads/get-download?downloadGroup=VS-MGMT-SDK80
mvn install:install-file -Dfile=pbm_80.jar    -DgroupId=com.cloud.com.vmware -DartifactId=vmware-pbm     -Dversion=8.0   -Dpackaging=jar

# From https://github.com/vmware/vsphere-automation-sdk-java/tree/master/lib
mvn install:install-file -Dfile=vapi-runtime-2.15.0.jar -DgroupId=com.vmware.vapi -DartifactId=vapi-runtime -Dversion=2.15.0 -Dpackaging=jar
mvn install:install-file -Dfile=vapi-runtime-2.37.0.jar -DgroupId=com.vmware.vapi -DartifactId=vapi-runtime -Dversion=2.37.0 -Dpackaging=jar
mvn install:install-file -Dfile=vapi-runtime-2.40.0.jar -DgroupId=com.vmware.vapi -DartifactId=vapi-runtime -Dversion=2.40.0 -Dpackaging=jar
mvn install:install-file -Dfile=vapi-authentication-2.15.0.jar -DgroupId=com.vmware.vapi -DartifactId=vapi-authentication -Dversion=2.15.0 -Dpackaging=jar
mvn install:install-file -Dfile=vapi-authentication-2.37.0.jar -DgroupId=com.vmware.vapi -DartifactId=vapi-authentication -Dversion=2.37.0 -Dpackaging=jar
mvn install:install-file -Dfile=vapi-authentication-2.40.0.jar -DgroupId=com.vmware.vapi -DartifactId=vapi-authentication -Dversion=2.40.0 -Dpackaging=jar
mvn install:install-file -Dfile=vsphereautomation-client-sdk-3.3.0.jar -DgroupId=com.vmware.vsphereautomation.client -DartifactId=vsphereautomation-client-sdk -Dversion=3.3.0 -Dpackaging=jar
mvn install:install-file -Dfile=vsphereautomation-client-sdk-4.0.0.jar -DgroupId=com.vmware.vsphereautomation.client -DartifactId=vsphereautomation-client-sdk -Dversion=4.0.0 -Dpackaging=jar

# From https://customerconnect.vmware.com/downloads/details?downloadGroup=NSX-4011-SDK-JAVA&productId=1324
mvn install:install-file -Dfile=nsx-java-sdk-4.1.0.2.0.jar -DgroupId=com.vmware -DartifactId=nsx-java-sdk -Dversion=4.1.0.2.0 -Dpackaging=jar
mvn install:install-file -Dfile=nsx-gpm-java-sdk-4.1.0.2.0.jar -DgroupId=com.vmware -DartifactId=nsx-gpm-java-sdk -Dversion=4.1.0.2.0 -Dpackaging=jar
mvn install:install-file -Dfile=nsx-policy-java-sdk-4.1.0.2.0.jar -DgroupId=com.vmware -DartifactId=nsx-policy-java-sdk -Dversion=4.1.0.2.0 -Dpackaging=jar

# From http://support.netapp.com/  (not available online, contact your support representative)
# Version: 4.0 (http://community.netapp.com/t5/Developer-Network-Articles-and-Resources/NetApp-Manageability-NM-SDK-Introduction-and-Download-Information/ta-p/86418)
if [ -e cloud-manageontap.jar ]; then mv cloud-manageontap.jar manageontap.jar;  fi
mvn install:install-file -Dfile=manageontap.jar     -DgroupId=com.cloud.com.netapp -DartifactId=manageontap     -Dversion=4.0   -Dpackaging=jar

# From https://juniper.github.io/contrail-maven/snapshots/net/juniper/contrail/juniper-contrail-api/1.0-SNAPSHOT/juniper-contrail-api-1.0-20131001.003401-3.jar
# New version can be found at https://github.com/Juniper/contrail-maven/tree/master/releases/net/juniper/contrail/juniper-contrail-api/1.2
mvn install:install-file -Dfile=juniper-contrail-api-1.0-SNAPSHOT.jar -DgroupId=net.juniper.contrail -DartifactId=juniper-contrail-api -Dversion=1.0-SNAPSHOT -Dpackaging=jar

# From https://github.com/radu-todirica/tungsten-api/raw/master/net/juniper/tungsten/juniper-tungsten-api/2.0/juniper-tungsten-api-2.0.jar
mvn install:install-file -Dfile=juniper-tungsten-api-2.0.jar -DgroupId=net.juniper.tungsten -DartifactId=juniper-tungsten-api -Dversion=2.0 -Dpackaging=jar

exit 0
