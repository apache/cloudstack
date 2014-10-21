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

set -x

install_packages() {

  # dev tools, ssh, nfs
  yum -y install wget telnet tcpdump sed awk ssh htop

  # utlities
  yum -y install httpd
}

start_services() {
    service httpd start
}

httpd_configure() {
    # start httpd on boot
    chkconfig httpd on
    # open port 80
    iptables -I INPUT -p tcp --dport 80 -j ACCEPT
    # create a test page
    echo "<h1> Hello, World </h1>" > /var/www/html/test.html
    # give 755 permissions and ownership
    chmod -R 755 /var/www/html/
    chown -R apache:apache /var/www/html/
}

begin=$(date +%s)

install_packages
httpd_configure
start_services

fin=$(date +%s)
t=$((fin-begin))

echo "Testing Builtin baked in $t seconds"
