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
import glob
import json

OCCURRENCES = 1

logging.basicConfig(filename='/var/log/cloud.log', level=logging.DEBUG, format='%(asctime)s  %(filename)s %(funcName)s:%(lineno)d %(message)s')

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
    returncode = configure.main(sys.argv)
    sys.exit(returncode)


def process_file():
    print "[INFO] Processing JSON file %s" % sys.argv[1]
    qf = QueueFile()
    qf.setFile(sys.argv[1])
    qf.load(None)
    # Converge
    finish_config()


def is_guestnet_configured(guestnet_dict, keys):

    existing_keys = []
    new_eth_key = None

    for k1, v1 in guestnet_dict.iteritems():
        if k1 in keys and len(v1) > 0:
            existing_keys.append(k1)

    if not existing_keys:
        '''
        It seems all the interfaces have been removed. Let's allow a new configuration to come in.
        '''
        print "[WARN] update_config.py :: Reconfiguring guest network..."
        return False

    fn = min(glob.iglob(jsonCmdConfigPath + '*'), key=os.path.getctime)
    file = open(fn)
    new_guestnet_dict = json.load(file)

    if not new_guestnet_dict['add']:
        '''
        Guest network has to be removed.
        '''
        print "[INFO] update_config.py :: Removing guest network..."
        return False

    '''
    Check if we have a new guest network ready to be setup
    '''
    device = new_guestnet_dict['device']

    if device in existing_keys:
        '''
        Device already configured, ignore.
        '''
        return True

    exists = False

    for key in existing_keys:
        for interface in guestnet_dict[key]:
            new_mac = new_guestnet_dict["mac_address"].encode('utf-8')
            old_mac = interface["mac_address"].encode('utf-8')
            new_ip = new_guestnet_dict["router_guest_ip"].encode('utf-8')
            old_ip = interface["router_guest_ip"].encode('utf-8')

            if (new_mac == old_mac) and (new_ip == old_ip):
                exists = True
                break

        if exists:
            break

    return exists

filename = min(glob.iglob(jsonCmdConfigPath + '*'), key=os.path.getctime)
if not (os.path.isfile(filename) and os.access(filename, os.R_OK)):
    print "[ERROR] update_config.py :: You are telling me to process %s, but i can't access it" % jsonCmdConfigPath
    sys.exit(1)

# If the guest network is already configured and have the same IP, do not try to configure it again otherwise it will break
if sys.argv[1] and sys.argv[1].count("guest_network.json") == OCCURRENCES:
    if os.path.isfile(currentGuestNetConfig):
        file = open(currentGuestNetConfig)
        guestnet_dict = json.load(file)

        if not is_guestnet_configured(guestnet_dict, ['eth1', 'eth2', 'eth3', 'eth4', 'eth5', 'eth6', 'eth7', 'eth8', 'eth9']):
            print "[INFO] update_config.py :: Processing Guest Network."
            process_file()
        else:
            print "[INFO] update_config.py :: No need to process Guest Network."
            finish_config()
    else:
        print "[INFO] update_config.py :: No GuestNetwork configured yet. Configuring first one now."
        process_file()
else:
    print "[INFO] update_config.py :: Processing incoming file => %s" % sys.argv[1]
    process_file()
