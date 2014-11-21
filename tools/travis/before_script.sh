#!/bin/bash
#
# This script should be used to bring up the environment.
#

export CATALINA_BASE=/opt/tomcat
export CATALINA_HOME=/opt/tomcat
export M2_HOME="/usr/local/maven-3.2.1/"
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=500m"

mvn -Dsimulator -Djava.net.preferIPv4Stack=true -pl :cloud-client-ui jetty:run 2>&1 > /dev/null &

while ! nc -vz localhost 8096 2>&1 > /dev/null; do sleep 10; done
python -m marvin.deployDataCenter -i setup/dev/advanced.cfg 2>&1 || true