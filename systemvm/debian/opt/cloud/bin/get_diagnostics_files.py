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

import zipfile
import sys
import time
import re
import subprocess as sp
import shlex
import os
import logging

fileList = sys.argv[1:]


# Create zip archive and append files for retrieval
def zip_files(files):
    fList = files
    compression = zipfile.ZIP_DEFLATED
    time_str = time.strftime("%Y%m%d-%H%M%S")
    zf_name = '/root/diagnostics_files_' + time_str + '.tar'
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
            # [IPTABLES], [ROUE] and [IFCONFIG], remove square brackets
            if '[' in f:
                shell_script = re.sub(r'\W+', "", f).strip().lower()
                f = execute_shell_script(shell_script)
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
        print zf_name


def execute_shell_script(script):
    # Ex. iptables.log
    outputfile = script + '.log'

    if script == 'iptables':
        cmd = 'iptables-save'
    elif script == 'ifconfig':
        cmd = 'ifconfig'
    elif script == 'route':
        cmd = 'netstat -rn'
    else:
        cmd = script
    with open(outputfile, 'wb', 0) as f:
        try:
            p = sp.Popen(shlex.split(cmd), stdout=sp.PIPE, stderr=sp.PIPE)
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
    zip_files(fileList)
