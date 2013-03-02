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
import tempfile
import os.path
import os

iso_folder = ''
copy_to = ''

def cmd(cmdstr, err=True):
    if os.system(cmdstr) != 0 and err:
        raise Exception("Failed to run shell command: %s" % cmdstr)
    
def prepare():
    try:
        kernel = os.path.join(copy_to, "vmlinuz")
        initrd = os.path.join(copy_to, "initrd.img")
        if os.path.exists(kernel) and os.path.exists(initrd):
            print "Having template(%s) prepared already, skip copying" % copy_to
            return 0
        else:
            if not os.path.exists(copy_to):
                os.makedirs(copy_to)

        mnt_path = tempfile.mkdtemp()
        try:
            mnt = "mount %s %s" % (iso_folder, mnt_path)
            cmd(mnt)
            
            kernel = os.path.join(mnt_path, "vmlinuz")
            initrd = os.path.join(mnt_path, "initrd.img")
            cp = "cp -f %s %s/" % (kernel, copy_to)
            cmd(cp)
            cp = "cp -f %s %s/" % (initrd, copy_to)
            cmd(cp)
        finally:
            umnt = "umount %s" % mnt_path
            cmd(umnt, False)
            rm = "rm -r %s" % mnt_path
            cmd(rm, False)
        return 0
    except Exception, e:
        print e
        return 1
    
if __name__ == "__main__":
    if len(sys.argv) < 3:
        print "Usage: prepare_kickstart_kerneal_initrd.py path_to_kernel_initrd_iso path_kernel_initrd_copy_to"
	sys.exit(1)
    
    (iso_folder, copy_to) = sys.argv[1:]
    sys.exit(prepare())
    
