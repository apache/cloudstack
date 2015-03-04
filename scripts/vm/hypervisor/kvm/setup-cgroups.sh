#!/bin/bash
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



# Script to fix cgroups co-mounted issue
# Applies to RHEL7 versions only
# Detect if cpu,cpuacct cgroups are co-mounted
# If co-mounted, unmount and mount them seperately 

#set -x

#Check distribution version for RHEL 
if [ -f '/etc/redhat-release' ];
then
    #Check RHEL version for 7 
   if grep 'Red Hat Enterprise Linux Server release 7' /etc/redhat-release > /dev/null
   then
        # Check if cgroups if co-mounted 
        if [ -d '/sys/fs/cgroup/cpu,cpuacct' ];
        then
   	    # cgroups co-mounted. Requires remount
            umount /sys/fs/cgroup/cpu,cpuacct
            rm /sys/fs/cgroup/cpu
            rm /sys/fs/cgroup/cpuacct
            rm -rf /sys/fs/cgroup/cpu,cpuacct
            mkdir -p /sys/fs/cgroup/cpu
            mkdir -p /sys/fs/cgroup/cpuacct
            mount -t cgroup -o cpu cpu "/sys/fs/cgroup/cpu"
            mount -t cgroup -o cpuacct cpuacct "/sys/fs/cgroup/cpuacct"
            # Verify that cgroups are not co-mounted 
            if [ -d '/sys/fs/cgroup/cpu,cpuacct' ];
            then
   	        echo "cgroups still co-mounted"
		exit 1;
	    fi
	fi    
   fi
fi

exit 0
