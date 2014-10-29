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

set -e
set -x

# clean up stuff copied in by veewee
function cleanup_veewee() {
  # this has to be here since it is the last file to run (and we remove ourselves)
  rm -fv /root/*.iso
  rm -fv /root/{apt_upgrade,authorized_keys,build_time,cleanup,install_systemvm_packages,zerodisk}.sh
  rm -fv /root/configure_{acpid,conntrack,grub,locale,login,networking,systemvm_services}.sh
  rm -fv .veewee_version .veewee_params .vbox_version
}

# Zero out the free space to save space in the final image:
function zero_disk() {
  cleanup_veewee

  for path in / /boot /usr /var /opt /tmp /home
  do
    dd if=/dev/zero of=${path}/zero bs=1M || true
    sync
    rm -f ${path}/zero
  done
}

return 2>/dev/null || zero_disk
