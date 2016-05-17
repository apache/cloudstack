#!/usr/bin/env python
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
# This script connects to the system vm socket and writes the
# authorized_keys and cmdline data to it. The system VM then
# reads it from /dev/vport0p1 in cloud_early_config
#

import argparse
import os
import socket

SOCK_FILE = "/var/lib/libvirt/qemu/{name}.agent"
PUB_KEY_FILE = "/root/.ssh/id_rsa.pub.cloud"
MESSAGE = "pubkey:{key}\ncmdline:{cmdline}\n"


def send_to_socket(sock_file, key_file, cmdline):
    if not os.path.exists(key_file):
        print("ERROR: ssh public key not found on host at {0}".format(key_file))
        return 1

    try:
        with open(key_file, "r") as f:
            pub_key = f.read()
    except IOError as e:
        print("ERROR: unable to open {0} - {1}".format(key_file, e.strerror))
        return 1

    # Keep old substitution from perl code:
    cmdline = cmdline.replace("%", " ")

    msg = MESSAGE.format(key=pub_key, cmdline=cmdline)

    if not os.path.exists(sock_file):
        print("ERROR: {0} socket not found".format(sock_file))
        return 1

    try:
        s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        s.connect(sock_file)
        s.sendall(msg)
        s.close()
    except IOError as e:
        print("ERROR: unable to connect to {0} - {1}".format(sock_file, e.strerror))
        return 1

    return 0    # Success


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Send configuration to system VM socket")
    parser.add_argument("-n", "--name", required=True, help="Name of VM")
    parser.add_argument("-p", "--cmdline", required=True, help="Command line")

    arguments = parser.parse_args()

    socket_file = SOCK_FILE.format(name=arguments.name)

    exit(send_to_socket(socket_file, PUB_KEY_FILE, arguments.cmdline))
