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



 

echo "Converting raw image to qcow2"
qemu-img  convert -f raw -O qcow2 systemvm.img systemvm.qcow2
echo "Compressing qcow2..."
bzip2 -c systemvm.qcow2 > systemvm.qcow2.bz2
echo "Done qcow2"
echo "Converting raw image to vmdk"
qemu-img  convert -f raw -O vmdk systemvm.img systemvm.vmdk
echo "Compressing vmdk..."
bzip2 -c systemvm.vmdk > systemvm.vmdk.bz2
echo "Done vmdk"
