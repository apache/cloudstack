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

kernel = None
initrd = None
copy_to = None

def cmd(cmdstr, err=True):
    print cmdstr
    if os.system(cmdstr) != 0 and err:
        raise Exception("Failed to run shell command: %s" % cmdstr)
    
def prepare():
    global kernel, initrd, copy_to
    try:
        k = os.path.join(copy_to, "vmlinuz")
        i = os.path.join(copy_to, "initrd.img")
        if os.path.exists(k) and os.path.exists(i):
            print "Having template(%s) prepared already, skip copying" % copy_to
            return 0
        else:
            if not os.path.exists(copy_to):
                os.makedirs(copy_to)


        def copy_from_nfs(src, dst):
            mnt_path = tempfile.mkdtemp()
            try:
                nfs_path = os.path.dirname(src)
                filename = os.path.basename(src)
                t = os.path.join(mnt_path, filename)
                mnt = "mount %s %s" % (nfs_path, mnt_path)
                cmd(mnt)
                cp = "cp -f %s %s" % (t, dst)
                cmd(cp)
            finally:
                umnt = "umount %s" % mnt_path
                cmd(umnt, False)
                rm = "rm -r %s" % mnt_path
                cmd(rm, False)

        copy_from_nfs(kernel, copy_to)
        copy_from_nfs(initrd, copy_to)
    except Exception, e:
        print e
        return 1
    
if __name__ == "__main__":
    if len(sys.argv) < 4:
        print "Usage: prepare_kickstart_kerneal_initrd.py path_to_kernel path_to_initrd path_kernel_initrd_copy_to"
	sys.exit(1)
    
    (kernel, initrd, copy_to) = sys.argv[1:]
    sys.exit(prepare())
    
