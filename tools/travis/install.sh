#!/bin/bash
#
# This should be used to create the build.
#

export CATALINA_BASE=/opt/tomcat
export CATALINA_HOME=/opt/tomcat
export M2_HOME="/usr/local/maven-3.2.1/"
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=500m"

# Compile Cloudstack
mvn -q -Pimpatient -Dsimulator clean install

# Compile API Docs
cd tools/apidoc
mvn -q clean install
cd ../../

# Compile marvin
cd tools/marvin
mvn -q clean install
sudo python setup.py install 2>&1 > /dev/null
cd ../../

# Deploy the database
mvn -q -Pdeveloper -pl developer -Ddeploydb
mvn -q -Pdeveloper -pl developer -Ddeploydb-simulator