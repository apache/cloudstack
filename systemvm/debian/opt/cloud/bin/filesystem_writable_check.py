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

import os


def check_filesystem():
    ST_RDONLY = 1
    if os.ST_RDONLY is not None:
        ST_RDONLY = os.ST_RDONLY

    stat1 = os.statvfs('/root')
    readOnly1 = bool(stat1.f_flag & ST_RDONLY)

    if (readOnly1):
        print("Read-only file system : monitor results (/root) file system is mounted as read-only")
        exit(1)

    stat2 = os.statvfs('/var/cache/cloud')
    readOnly2 = bool(stat2.f_flag & ST_RDONLY)

    if (readOnly2):
        print("Read-only file system : config info (/var/cache/cloud) file system is mounted as read-only")
        exit(1)

    print("file system is writable")
    exit(0)


if __name__ == "__main__":
    check_filesystem()
