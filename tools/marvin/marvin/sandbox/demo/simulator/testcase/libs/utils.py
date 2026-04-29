# -*- encoding: utf-8 -*-
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

"""Utilities functions
"""

import time
import marvin.sshClient
from marvin.cloudstackAPI import *
import marvin.cloudstackConnection
from marvin.sshClient import SshClient
#from marvin.cloudstackConnection import cloudConnection
import marvin.configGenerator
import logging
import string
import random

def random_gen(size=6, chars=string.ascii_uppercase + string.digits):
    """Generate Random Strings of variable length"""
    return ''.join(random.choice(chars) for x in range(size))

def cleanup_resources(api_client, resources):
    """Delete resources"""
    for obj in resources:
        obj.delete(api_client)

def is_server_ssh_ready(ipaddress, port, username, password, retries=50):
    """Return ssh handle else wait till sshd is running"""
    loop_cnt = retries
    while True:
        try:
            ssh = marvin.sshClient.SshClient(
                                            ipaddress,
                                            port,
                                            username,
                                            password
                                            )
        except Exception as e:
            if loop_cnt == 0:
                raise e
            loop_cnt = loop_cnt - 1
            time.sleep(30)
        else:
            return ssh


def format_volume_to_ext3(ssh_client, device="/dev/sda"):
    """Format attached storage to ext3 fs"""
    cmds = [
            "echo -e 'n\np\n1\n\n\nw' | fdisk %s" % device,
            "mkfs.ext3 %s1" % device,
           ]
    for c in cmds:
        ssh_client.execute(c)

def fetch_api_client(config_file='datacenterCfg'):
    """Fetch the Cloudstack API Client"""
    config = configGenerator.get_setup_config(config_file)
    mgt = config.mgtSvr[0]
    testClientLogger = logging.getLogger("testClient")
    asyncTimeout = 3600
    return cloudstackAPIClient.CloudStackAPIClient(
            cloudstackConnection.cloudConnection(
                                                mgt,
                                                asyncTimeout,
                                                testClientLogger
                                                )
                                            )

def get_process_status(hostip, port, username, password, linklocalip, process):
    """Double hop and returns a process status"""

    #SSH to the machine
    ssh = marvin.sshClient.SshClient(
                                          hostip,
                                          port,
                                          username,
                                          password
                            )
    ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -ostricthostkeychecking=no "
    ssh_command = ssh_command + "-oUserKnownHostsFile=/dev/null -p 3922 %s %s" \
                        % (linklocalip, process)

    # Double hop into router
    timeout = 5
    # Ensure the SSH login is successful
    while True:
        res = ssh.execute(ssh_command)

        if res[0] != "Host key verification failed.":
            break
        elif timeout == 0:
            break

        time.sleep(5)
        timeout = timeout - 1
    return res
