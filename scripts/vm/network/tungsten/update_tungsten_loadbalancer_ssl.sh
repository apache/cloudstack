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
i=0;
while [ ! -f /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf ];
do
  sleep 1;
  ((i=i+1))
  if [ $i -eq 5 ]; then exit 1; fi
done
if grep -qw "bind $5:$6 $" /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf; then
rm -rf /var/lib/contrail/loadbalancer/haproxy/$1/$2.pem
cat >> /var/lib/contrail/loadbalancer/haproxy/$1/$2.pem << EOF
$3
$4
EOF
sed -i "/bind $5:$6 $/c\	bind $5:$6 ssl crt /var/lib/contrail/loadbalancer/haproxy/$1/$2.pem" /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf
container=$(docker ps | grep contrail-vrouter-agent | awk '{print $1}')
netns=$(ls /var/run/netns | grep $1)
docker exec $container ip netns exec $netns haproxy -D -f /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf \
-p /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.pid -sf $(cat /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.pid) &>/dev/null
fi
