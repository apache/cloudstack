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

Veewee::Definition.declare({
  :cpu_count => '1',
  :memory_size=> '256',
  :disk_size => '2048', :disk_format => 'VMDK', :hostiocache => 'off',
  :os_type_id => 'Debian',
  :iso_file => "debian-wheezy-DI-b4-i386-netinst.iso",
  :iso_src => "http://cdimage.debian.org/cdimage/wheezy_di_beta4/i386/iso-cd/debian-wheezy-DI-b4-i386-netinst.iso",
  :iso_md5 => "34d0ae973715c7a31646281c70839809",
  :iso_download_timeout => "1000",
  :boot_wait => "10", :boot_cmd_sequence => [
     '<Esc>',
     'install ',
     'preseed/url=http://%IP%:%PORT%/preseed.cfg ',
     'debian-installer=en_US ',
     'auto ',
     'locale=en_US ',
     'kbd-chooser/method=us ',
     'netcfg/get_hostname=systemvm ',
     'netcfg/get_domain=apache.org ',
     'fb=false ',
     'debconf/frontend=noninteractive ',
     'console-setup/ask_detect=false ',
     'console-keymaps-at/keymap=us ',
     '<Enter>'
  ],
  :kickstart_port => "7122",
  :kickstart_timeout => "10000",
  :kickstart_file => "preseed.cfg",
  :ssh_login_timeout => "10000",
  :ssh_user => "root",
  :ssh_password => "password",
  :ssh_key => "",
  :ssh_host_port => "7222",
  :ssh_guest_port => "22",
  :sudo_cmd => "echo '%p'|sudo -S sh '%f'",
  :shutdown_cmd => "halt -p",
  :postinstall_files => [
    "postinstall.sh",
  ],
  :postinstall_timeout => "10000"
})
