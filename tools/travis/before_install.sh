#!/bin/bash
#
# This script should be used to install additional dependencies
# This includes: installing ubuntu packages, custom services
# or internet downloads.
#

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
