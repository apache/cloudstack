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


 

# $Id: versions.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/versions.sh $
# Output Linux distribution.

REV=`uname -r`
MACH=`uname -m`
KERNEL=`uname -r`
DIST="Unknown Linux"
REV="X.Y"
CODENAME=""

if [ -f /etc/redhat-release ] ; then
	DIST=`cat /etc/redhat-release | awk '{print $1}'`
	CODENAME=`cat /etc/redhat-release | sed s/.*\(// | sed s/\)//`
	REV=`cat /etc/redhat-release | awk '{print $3,$4}' | grep -o "[0-9.]*"`
elif [ -f /etc/lsb-release ] ; then
	DIST=`cat /etc/lsb-release | grep DISTRIB_ID | tr "\n" ' '| sed s/.*=//`
	REV=`cat /etc/lsb-release | grep DISTRIB_RELEASE | tr "\n" ' '| sed s/.*=//`
	CODENAME=`cat /etc/lsb-release | grep DISTRIB_CODENAME | tr "\n" ' '| sed s/.*=//`
fi
	
echo Host.OS=${DIST} 
echo Host.OS.Version=${REV} 
echo Host.OS.Kernel.Version=${KERNEL}
