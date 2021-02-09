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

echo -e "#### System Information ####"
echo -e "\nO.S. information:"
echo $(uname -a)

echo -e "\nWho am I:"
whoami

echo -e "\nJava Version: "
javac -version

echo -e "\nMaven Version: "
mvn -v

echo -e "\nPython Version: "
python --version

echo -e "\nPip Version: "
pip --version

echo -e "\nDisk Status: "
df

echo -e "\nMemory Status: "
free

echo -e "\nTotal CPUs: "
nproc

echo -e "\nCheck Git status"
git status

echo -e "\nCleaning up stale files in /tmp: "
sudo find /tmp -type f -mtime +2 | grep -v "`sudo lsof | grep /tmp |awk '{print $9}'|sed -e '1 d' |sort |uniq | tr \\n \|`" | xargs sudo rm -vf

echo -e "\nUpdating the system: "
sudo apt-get -y clean
sudo apt-get -y update > /dev/null

echo -e "\nInstalling MySQL: "

export DEBIAN_FRONTEND=noninteractive
sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password password password'
sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password_again password password'
sudo apt-get -q -y install mysql-server > /dev/null

mysql -uroot -ppassword -e "SET PASSWORD = PASSWORD(''); FLUSH PRIVILEGES;"
sudo service mysql restart

echo -e "\nInstalling Development tools: "
RETRY_COUNT=3

sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 1397BC53640DB551
sudo sh -c 'echo "deb http://mirrors.kernel.org/ubuntu bionic-updates main" >> /etc/apt/sources.list'
sudo apt-get update -q -y > /dev/null
sudo apt-get -q -y -t bionic-updates install openjdk-11-jdk
sudo apt-get -q -y install uuid-runtime genisoimage netcat > /dev/null
if [[ $? -ne 0 ]]; then
  echo -e "\napt-get packages failed to install"
fi

sudo apt-get -q -y -V install freeipmi-common libfreeipmi16 libgcrypt20 libgpg-error-dev libgpg-error0 libopenipmi0 ipmitool libpython-dev libssl-dev libffi-dev python-openssl build-essential --no-install-recommends > /dev/null

#sudo apt-get -y install python3 python3-pip
#sudo apt-get -y install python3-devel # in order to be able to pip3 install pycrypto
#echo `python3 --version | cut -d ' ' -f 2` >> /home/travis/build/apache/cloudstack/.python-version

pyenv install `cat /home/travis/build/apache/cloudstack/.python-version`

echo -e "\nIPMI version"
ipmitool -V

curl -sL https://deb.nodesource.com/setup_12.x | sudo -E bash -
sudo apt-get install -y nodejs

echo -e "\nNode version"
npm version

echo "<settings>
  <mirrors>
    <mirror>
      <id>Central</id>
      <url>https://repo1.maven.org/maven2</url>
      <mirrorOf>central</mirrorOf>
      <!-- United States, St. Louis-->
    </mirror>
  </mirrors>
</settings>" > ~/.m2/settings.xml

echo -e "\nInstalling some python packages: "

for ((i=0;i<$RETRY_COUNT;i++))
do
  python3 -m pip install --user --upgrade urllib3 lxml paramiko nose texttable ipmisim pyopenssl pycrypto mock flask netaddr pylint pycodestyle six astroid > /tmp/piplog
  python2 -m pip install --user --upgrade pylint pycodestyle >> /tmp/piplog
  if [[ $? -eq 0 ]]; then
    echo -e "\npython packages installed successfully"
    break;
  fi
  echo -e "\npython packages failed to install"
  cat /tmp/piplog
done

echo -e "\nVersion of pip packages:\n"
echo $(pip freeze)