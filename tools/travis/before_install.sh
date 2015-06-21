#!/bin/bash
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
#
# This script should be used to install additional dependencies
# This includes: installing ubuntu packages, custom services
# or internet downloads.
#

export TEST_JOB_NUMBER=`echo $TRAVIS_JOB_NUMBER | cut -d. -f1`
export TEST_SEQUENCE_NUMBER=`echo $TRAVIS_JOB_NUMBER | cut -d. -f2`

echo "REGRESSION_CYCLE=$REGRESSION_CYCLE"
echo "TEST_JOB_NUMBER=$TEST_JOB_NUMBER"
echo "TEST_SEQUENCE_NUMBER=$TEST_SEQUENCE_NUMBER"

#run regression test only on $REGRESSION_CYCLE
MOD=$(( $TEST_JOB_NUMBER % $REGRESSION_CYCLE ))

echo "MOD=$MOD"

if [ $MOD -ne 0 ]; then
 if [ $TEST_SEQUENCE_NUMBER -ge $REGRESSION_INDEX ]; then
   #skip test
   echo "Skipping tests ... SUCCESS !"
   exit 0
 fi
fi

echo -e "#### System Information ####"

echo -e "\nJava Version: "
javac -version

echo -e "\nMaven Version: "
mvn -v

echo -e "\nUpdating the system: "
sudo apt-get -q -y update > /dev/null

echo -e "\nInstalling MySQL: "

sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password password your_password'
sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password_again password your_password'
sudo apt-get -q -y install mysql-server > /dev/null

sudo /etc/init.d/mysql start

echo -e "\nInstalling Tomcat: "
wget -q -O tomcat.tar.gz http://archive.apache.org/dist/tomcat/tomcat-6/v6.0.33/bin/apache-tomcat-6.0.33.tar.gz
sudo mkdir -p /opt/tomcat
sudo tar xfv tomcat.tar.gz -C /opt/tomcat --strip 1 > /dev/null

echo -e "\nInstalling Development tools: "

sudo apt-get -q -y install uuid-runtime genisoimage python-setuptools python-pip netcat > /dev/null

echo "<settings>
  <mirrors>
    <mirror>
      <id>Central</id>
      <url>http://repo1.maven.org/maven2</url>
      <mirrorOf>central</mirrorOf>
      <!-- United States, St. Louis-->
    </mirror>
  </mirrors>
</settings>" > ~/.m2/settings.xml

echo -e "\nInstalling some python packages: "

sudo pip install lxml > /dev/null
sudo pip install texttable > /dev/null

#Download project dependencies in a way we can retry if there's a failure, without failing the whole build
RETRY_COUNT=3

#Resolve plugins first
echo -e "\nDownloading Plugin dependencies"
for ((i=0;i<$RETRY_COUNT;i++))
do
 mvn org.apache.maven.plugins:maven-dependency-plugin:resolve-plugins > /dev/null
 if [[ $? -eq 0 ]]; then
   echo -e "\nPlugin dependencies downloaded successfully"
   break;
 fi
 echo -e "\nDependency download failed"
 #Test DNS record
 host repo1.maven.org
 while ! nc -vzw 5 repo1.maven.org 80; do echo -e "\nFailed to connect to repo1.maven.org:80 will retry in 10 seconds"; sleep 10; done
done

#Resolve remaining deps
cd tools/travis
./downloadDeps.sh 2> /dev/null
echo -e "\nDownloading Project dependencies"
echo -e "$(cat pom.xml |wc -l) lines in dummy pom.xml"

for ((i=0;i<$RETRY_COUNT;i++))
do
 mvn org.apache.maven.plugins:maven-dependency-plugin:resolve > /dev/null
 if [[ $? -eq 0 ]]; then
   echo -e "\nProject dependencies downloaded successfully"
   break;
 fi
 echo -e "\nDependency download failed"
 #Test DNS record
 host repo1.maven.org
 while ! nc -vzw 5 repo1.maven.org 80; do echo -e "\nFailed to connect to repo1.maven.org:80 will retry in 10 seconds"; sleep 10; done
done
cd ../..
