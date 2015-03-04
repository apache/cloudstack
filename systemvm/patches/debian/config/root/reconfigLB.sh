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


ret=0

new_config=$1

# save previous state
  mv /etc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg.old

  mv $new_config /etc/haproxy/haproxy.cfg
  if haproxy -p /var/run/haproxy.pid -f /etc/haproxy/haproxy.cfg -sf $(cat /var/run/haproxy.pid); then
    logger -t cloud "New haproxy instance successfully loaded, stopping previous one."
    ret=0
  else
    logger -t cloud "New instance failed to start, resuming previous one."
    mv /etc/haproxy/haproxy.cfg $new_config
    mv /etc/haproxy/haproxy.cfg.old /etc/haproxy/haproxy.cfg
    haproxy -p /var/run/haproxy.pid -f /etc/haproxy/haproxy.cfg -sf $(cat /var/run/haproxy.pid)
    ret=1
  fi

exit $ret

