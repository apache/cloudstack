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

# Standalone readiness-check helper for the instance boot group feature. Kept separate from
# diagnostics.py, which is a general-purpose admin tool for system VMs: this script is invoked
# on behalf of user instance readiness rules and should not share code/behaviour with that tool.

import socket
import subprocess
import sys


def emit(stdout, stderr, exit_code):
    print('%s&&' % stdout)
    print('%s&&' % stderr)
    print('%s' % exit_code)


def check_ping(host, count=4):
    try:
        p = subprocess.Popen(['ping', '-c', str(count), host], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout, stderr = p.communicate()
        emit(stdout.decode().strip(), stderr.decode().strip(), p.returncode)
    except OSError as e:
        emit('', 'Exception occurred: %s' % e, 1)


def check_port(host, port, timeout=3):
    try:
        with socket.create_connection((host, int(port)), timeout=timeout):
            emit('connected', '', 0)
    except Exception as e:
        emit('', str(e), 1)


def main():
    if len(sys.argv) < 3:
        emit('', 'Usage: instance_readiness_check.py <ping|portcheck> <host> [port]', 1)
        return

    check_type = sys.argv[1]
    host = sys.argv[2]

    if check_type == 'ping':
        check_ping(host)
    elif check_type == 'portcheck':
        if len(sys.argv) < 4:
            emit('', 'portcheck requires a port argument', 1)
            return
        check_port(host, sys.argv[3])
    else:
        emit('', 'Unknown check type: %s' % check_type, 1)


if __name__ == "__main__":
    main()
