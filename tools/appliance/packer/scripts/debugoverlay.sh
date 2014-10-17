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

set -x
begin=$(date +%s)

#Start Function definitions 
log_it() {
  echo "$(date) $0 $@" >> debugoverlay$begin.log
}

configure_overlay() {
  log_it "Start the overlays"
  snapshot_dir="/opt/cloudstack"
   chmod -R 755 $snapshot_dir/systemvm/patches/debian/config/*
   chmod -R 755 $snapshot_dir/systemvm/patches/debian/vpn/*
   cp -rv $snapshot_dir/systemvm/patches/debian/config/* /
   cp -rv $snapshot_dir/systemvm/patches/debian/vpn/* / 
  mkdir -p /usr/share/cloud/
  cd $snapshot_dir/systemvm/patches/debian/config
  tar -cvf /usr/share/cloud/cloud-scripts-debug-$begin.tar *
  cd $snapshot_dir/systemvm/patches/debian/vpn
  tar -rvf /usr/share/cloud/cloud-scripts-debug-$begin.tar *
  log_it "Overlays complete. Check /usr/share/cloud/cloud-scripts-debug-$begin.tar for file used"

}

log_it "Start debugoverlay.sh"

configure_overlay

fin=$(date +%s)
t=$((fin-begin))

echo "Overlay performed, finished building systemvm appliance in $t seconds"
