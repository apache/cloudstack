#!/usr/bin/python
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

import sys
from merge import QueueFile
import logging
import subprocess
from subprocess import PIPE, STDOUT
import os
import os.path
import configure
import json

logging.basicConfig(filename='/var/log/cloud.log', level=logging.DEBUG, format='%(asctime)s %(message)s')

# first commandline argument should be the file to process
if (len(sys.argv) != 2):
    print "[ERROR]: Invalid usage"
    sys.exit(1)

# FIXME we should get this location from a configuration class
jsonPath = "/var/cache/cloud/%s"
jsonCmdConfigPath = jsonPath % sys.argv[1]
currentGuestNetConfig = "/etc/cloudstack/guestnetwork.json"

def finish_config():
    # Converge
    returncode = configure.main([])
    sys.exit(returncode)

def process_file():
    print "[INFO] Processing JSON file %s" % sys.argv[1]
    qf = QueueFile()
    qf.setFile(sys.argv[1])
    qf.load(None)
    # Converge
    finish_config()

def is_guestnet_configured(guestnet_dict, key):
    
    existing_key = None
    new_eth_key = None
    
    for k1, v in guestnet_dict.iteritems():
        for k2 in key:
            if k1 == k2 and len(guestnet_dict[k1]) > 0:
                existing_key = k1
        if existing_key:
            break
    
    if not existing_key:
        return False
    
    file = open(jsonCmdConfigPath)
    new_guestnet_dict = json.load(file)
    
    for k1, v in new_guestnet_dict.iteritems():
        for k2 in key:
            if k1 == k2 and len(new_guestnet_dict[k1]) > 0:
                new_eth_key = k1
        if new_eth_key:
            break
    
    if not new_eth_key:
        '''
        Why is the new guest net dictionary empty?
          1. It might be setting up a single VPC, no  need to continue.
          2. Did we get any RTNETLINK error? If so, it might be trying to unplug the Guest Net NIC. Let's not allow it for now.
          3. Might be a bug on the Java side.
        Return True so we won't process an empty file. However, we have to investigate it!
        '''
        return True

    old_eth = guestnet_dict[existing_key][0]
    new_eth = new_guestnet_dict[new_eth_key][0]
    
    new_mac = new_eth["mac_address"].encode('utf-8')
    old_mac = old_eth["mac_address"].encode('utf-8')
    new_ip = new_eth["router_guest_ip"].encode('utf-8')
    old_ip = old_eth["router_guest_ip"].encode('utf-8')
    
    if (new_mac == old_mac) and (new_ip == old_ip):
        print "[WARN] Guest Network already configured. Will skip the file to avoid RTNETLINK errors."
        return True
    
    return False

if not (os.path.isfile(jsonCmdConfigPath) and os.access(jsonCmdConfigPath, os.R_OK)):
    print "[ERROR]: You are telling me to process %s, but i can't access it" % jsonCmdConfigPath
    sys.exit(1)

# If the command line json file is unprocessed process it
# This is important or, the control interfaces will get deleted!
if os.path.isfile(jsonPath % "cmd_line.json"):
    qf = QueueFile()
    qf.setFile("cmd_line.json")
    qf.load(None)

# If the guest network is already configured and have the same IP, do not try to configure it again otherwise it will break
if sys.argv[1] == "guest_network.json":
    if os.path.isfile(currentGuestNetConfig):
        file = open(currentGuestNetConfig)
        guestnet_dict = json.load(file)
    
        if not is_guestnet_configured(guestnet_dict, ['eth1', 'eth2', 'eth3']):
            process_file()
        else:
            finish_config() 
else:
    process_file()