#!/usr/bin/env python3
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

import shlex
import subprocess
import sys


def run_cmd(command):
    if command is not None:
        try:
            p = subprocess.Popen(shlex.split(command), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout, stderr = p.communicate()
            return_code = p.returncode

        except OSError as e:
            stdout = "Check your command type"
            stderr = "Exception occurred: %s" % e
            return_code = 1

        finally:
            print('%s&&' % stdout.strip())
            print('%s&&' % stderr.strip())
            print('%s' % return_code)


def get_command():
    arguments = sys.argv
    cmd = " ".join(arguments[1:])
    cmd_type = sys.argv[1]

    if cmd_type == 'ping':
        if '-c' in arguments:
            return cmd
        else:
            return cmd + " -c 4"

    elif cmd_type == 'traceroute':
        if '-m' in arguments:
            return cmd
        else:
            return cmd + " -m 20"

    elif cmd_type == 'arping':
        if '-c' in arguments:
            return cmd
        else:
            return cmd + " -c 4"

    else:
        return None


if __name__ == "__main__":
    command = get_command()
    run_cmd(command)
