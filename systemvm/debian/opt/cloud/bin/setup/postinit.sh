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

# This scripts before ssh.service but after cloud-early-config

CMDLINE=/var/cache/cloud/cmdline
for str in $(cat $CMDLINE)
  do
    KEY=$(echo $str | cut -d= -f1)
    VALUE=$(echo $str | cut -d= -f2)
    case $KEY in
      type)
        export TYPE=$VALUE
        ;;
      *)
        ;;
    esac
done

chmod -x /etc/systemd/system/cloud*.service
systemctl daemon-reload

if [ "$TYPE" == "router" ] || [ "$TYPE" == "vpcrouter" ] || [ "$TYPE" == "dhcpsrvr" ]
then
  if [ -x /opt/cloud/bin/update_config.py ]
  then
      /opt/cloud/bin/update_config.py cmd_line.json
      logger -t cloud "Updated config: cmd_line.json"
  fi
fi

if [ "$TYPE" == "router" ]
then
    python /opt/cloud/bin/baremetal-vr.py &
    logger -t cloud "Started baremetal-vr service"
fi
