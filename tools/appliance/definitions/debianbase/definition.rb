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

arch = ENV['VM_ARCH'] || 'i386'

architectures = {
    :i386 => {
        :os_type_id => 'Debian',
        :iso_file => 'debian-7.6.0-i386-netinst.iso',
        :iso_src => 'http://cdimage.debian.org/debian-cd/7.6.0/i386/iso-cd/debian-7.6.0-i386-netinst.iso',
        :iso_md5 => '528e1a7315da1bbf50bd4d187880a519',
    },
    :amd64 => {
        :os_type_id => 'Debian_64',
        :iso_file => 'debian-7.6.0-amd64-netinst.iso',
        :iso_src => 'http://cdimage.debian.org/debian-cd/7.6.0/amd64/iso-cd/debian-7.6.0-amd64-netinst.iso',
        :iso_md5 => '8a3c2ad7fd7a9c4c7e9bcb5cae38c135'
    }
}

config = {
    :cpu_count => '1',
    :memory_size => '256',
    :disk_size => '2500', :disk_format => 'VDI', :hostiocache => 'off',
    :iso_download_timeout => '1200',
    :boot_wait => '10',
    :boot_cmd_sequence => [
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
        'keyboard-configuration/xkb-keymap=us ',
        '<Enter>'
    ],
    :kickstart_port => '7122',
    :kickstart_timeout => '1200',
    :kickstart_file => 'preseed.cfg',
    :ssh_login_timeout => '1200',
    :ssh_user => 'root',
    :ssh_password => 'password',
    :ssh_key => '',
    :ssh_host_port => '7222',
    :ssh_guest_port => '22',
    :sudo_cmd => "echo '%p'|sudo -S sh '%f'",
    :shutdown_cmd => 'halt -p',
    :postinstall_files => [
        # basic minimal vm creation
        'configure_login.sh',
        'cleanup.sh',
        'zerodisk.sh'
    ],
    :postinstall_timeout => '1200'
}

config.merge! architectures[arch.to_sym]

Veewee::Definition.declare(config)
