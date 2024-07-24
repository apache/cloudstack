Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

===========================================================

# Introduction

This is used to build appliances for use with CloudStack. Currently two
build profiles are available for building systemvmtemplate (Debian based) and
CentOS based built-in user VM template.

# Setting up Tools and Environment

- Install packer and latest KVM, qemu on a Linux machine
- Install tools for exporting appliances: qemu-img, ovftool, faketime, sharutils
- Build and install `vhd-util` as described in build.sh or use pre-built
  binaries at:

      http://packages.shapeblue.com/systemvmtemplate/vhd-util
      http://packages.shapeblue.com/systemvmtemplate/libvhd.so.1.0

# How to build appliances

Just run build.sh, it will export archived appliances for KVM, XenServer,
VMWare and HyperV in `dist` directory:

    bash build.sh systemvmtemplate
    bash build.sh builtin
