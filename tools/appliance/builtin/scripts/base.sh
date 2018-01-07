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

set -eux

sed -i 's/^GSS/\#GSS/g' /etc/ssh/sshd_config
echo "PermitRootLogin yes" >> /etc/ssh/sshd_config
echo "UseDNS no" >> /etc/ssh/sshd_config

yum -y update

yum install -y tmux vim htop wget telnet tcpdump sed awk ssh httpd net-tools open-vm-tools
systemctl enable httpd

iptables -I INPUT -p tcp -m tcp --dport 22 -j ACCEPT
iptables -I INPUT -p tcp -m tcp --dport 80 -j ACCEPT
iptables-save > /etc/sysconfig/iptables
