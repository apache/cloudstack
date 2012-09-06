#!/bin/sh

mvn install:install-file -Dfile=cloud-iControl.jar      -DgroupId=com.cloud.com.f5     -DartifactId=icontrol        -Dversion=1.0   -Dpackaging=jar
mvn install:install-file -Dfile=cloud-netscaler.jar     -DgroupId=com.cloud.com.citrix -DartifactId=netscaler       -Dversion=1.0   -Dpackaging=jar
mvn install:install-file -Dfile=cloud-netscaler-sdx.jar -DgroupId=com.cloud.com.citrix -DartifactId=netscaler-sdx   -Dversion=1.0   -Dpackaging=jar
mvn install:install-file -Dfile=cloud-manageontap.jar   -DgroupId=com.cloud.com.netapp -DartifactId=manageontap     -Dversion=1.0   -Dpackaging=jar
mvn install:install-file -Dfile=vmware-vim.jar          -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim      -Dversion=1.0   -Dpackaging=jar
mvn install:install-file -Dfile=vmware-vim25.jar        -DgroupId=com.cloud.com.vmware -DartifactId=vmware-vim25    -Dversion=1.0   -Dpackaging=jar
mvn install:install-file -Dfile=vmware-apputils.jar     -DgroupId=com.cloud.com.vmware -DartifactId=vmware-apputils -Dversion=1.0   -Dpackaging=jar
