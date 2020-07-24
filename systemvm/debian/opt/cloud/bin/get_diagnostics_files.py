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

import logging
import os
import re
import shlex
import subprocess as sp
import sys
import time
import zipfile


# Create zip archive and append files for retrieval
def zip_files(files):
    fList = files
    compression = zipfile.ZIP_DEFLATED
    time_str = time.strftime("%Y%m%d-%H%M%S")
    zf_name = '/root/diagnostics_files_' + time_str + '.zip'
    zf = zipfile.ZipFile(zf_name, 'w', compression)

    '''
    Initialize 3 empty arrays to collect found files, non-existent files
    and last one to collect temp files to be cleaned up when script exits
    '''
    files_found_list = []
    files_not_found_list = []
    files_from_shell_commands = []

    try:
        for f in fList:
            f = f.strip()

            if f in ('iptables', 'ipaddr', 'iprule', 'iproute'):
                f = execute_shell_script(f)
                files_from_shell_commands.append(f)

            if len(f) > 3 and f.startswith('[') and f.endswith(']'):
                f = execute_shell_script(f[1:-1])
                files_from_shell_commands.append(f)

            if os.path.isfile(f):
                try:
                    zf.write(f, f[f.rfind('/') + 1:])
                except OSError or RuntimeError as e:
                    files_not_found_list.append(f)
                else:
                    files_found_list.append(f)
    finally:
        cleanup(files_from_shell_commands)
        generate_retrieved_files_txt(zf, files_found_list, files_not_found_list)
        zf.close()
        print(zf_name)


def get_cmd(script):
    if script is None or len(script) == 0:
        return None

    cmd = None
    if script == 'iptables':
        cmd = 'iptables-save'
    elif script == 'ipaddr':
        cmd = 'ip address'
    elif script == 'iprule':
        cmd = 'ip rule list'
    elif script == 'iproute':
        cmd = 'ip route show table all'
    else:
        cmd = '/opt/cloud/bin/' + script
        if not os.path.isfile(cmd.split(' ')[0]):
            cmd = None

    return cmd


def execute_shell_script(script):
    script = script.strip()
    outputfile = script + '.log'

    with open(outputfile, 'wb', 0) as f:
        try:
            cmd = get_cmd(script)
            if cmd is None:
                f.write('Unable to generate command for ' + script + ', perhaps missing file')
            else:
                p = sp.Popen(cmd, shell=True, stdout=sp.PIPE, stderr=sp.PIPE)
                stdout, stderr = p.communicate()
                return_code = p.returncode
                if return_code is 0:
                    f.write(stdout)
                else:
                    f.write(stderr)
        except OSError as ex:
            delete_tmp_file_cmd = 'rm -f %s' % outputfile
            sp.check_call(shlex.split(delete_tmp_file_cmd))
        finally:
            f.close()
    return outputfile


def cleanup(file_list):
    files = ' '.join(file_list)
    cmd = 'rm -f %s' % files
    try:
        p = sp.Popen(shlex.split(cmd), stderr=sp.PIPE, stdout=sp.PIPE)
        p.communicate()
    except OSError as e:
        logging.debug("Failed to execute bash command")


def generate_retrieved_files_txt(zip_file, files_found, files_not_found):
    output_file = 'fileinfo.txt'
    try:
        with open(output_file, 'wb', 0) as man:
            for i in files_found:
                man.write(i + '\n')
            for j in files_not_found:
                man.write(j + 'File Not Found!!\n')
        zip_file.write(output_file, output_file)
    finally:
        cleanup_cmd = "rm -f %s" % output_file
        sp.check_call(shlex.split(cleanup_cmd))


if __name__ == '__main__':
    fileList = sys.argv[1:]
    zip_files(fileList)
