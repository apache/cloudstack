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



 

# BUILDING vhd-util on Linux
# The xen repository has a tool called vhd-util that compiles and runs on any linux system 
# (http://xenbits.xensource.com/xen-4.0-testing.hg?file/8e8dd38374e9/tools/blktap2/vhd/ or full Xen source at http://www.xen.org/products/xen_source.html).
# Apply this patch: http://lists.xensource.com/archives/cgi-bin/mesg.cgi?a=xen-devel&i=006101cb22f6%242004dd40%24600e97c0%24%40zhuo%40cloudex.cn.
# Build the vhd-util tool:
#    cd tools/blktap2
#    make
#    sudo make install

echo "Backing up systemvm.img"
cp systemvm.img systemvm.img.tmp
echo "Converting raw image to fixed vhd"
vhd-util convert -s 0 -t 1 -i systemvm.img.tmp -o systemvm.vhd
echo "Converting fixed vhd to dynamic vhd"
vhd-util convert -s 1 -t 2 -i systemvm.vhd -o systemvm.vhd
echo "Compressing..."
bzip2 -c systemvm.vhd > systemvm.vhd.bz2
echo "Done"
