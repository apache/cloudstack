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

arch = 'amd64'

#
# NOTE: Before changing the version of the debian image make
# sure it is added to the userContent of jenkins.buildacloud.org
# and the copy task is updated on the systemvm builds
# This will prevent the inevitable build failure once the iso is
# removed from the debian mirrors
#
architectures = {
    :amd64 => {
        :os_type_id => 'Debian_64',
        :iso_file => 'debian-9.2.1-amd64-netinst.iso',
        :iso_src => 'https://cdimage.debian.org/debian-cd/current/amd64/iso-cd/debian-9.2.1-amd64-netinst.iso',
        :iso_sha512 => 'ebfe25dc593967e39349b205480b0ec0103ef4a0468c602f4857e13d06d407bfe876162399e0e8d4dea5675953dc37ab585316f307ccf9f4440124b4f719df04'
    }
}

config = {
    :cpu_count => '1',
    :memory_size => '512',
    :disk_size => '2000', :disk_format => 'VDI', :hostiocache => 'off',
    :iso_download_timeout => '1000',
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
    :kickstart_timeout => '1000',
    :kickstart_file => 'preseed.cfg',
    :ssh_login_timeout => '10000',
    :ssh_user => 'cloud',
    :ssh_password => 'cloud',
    :ssh_key => '',
    :ssh_host_port => '7222',
    :ssh_guest_port => '22',
    :sudo_cmd => "echo '%p'|sudo -S bash '%f'",
    :shutdown_cmd => 'halt -p',
    :postinstall_files => [
        # basic minimal vm creation
        'apt_upgrade.sh',
        'configure_grub.sh',
        'configure_locale.sh',
        'configure_networking.sh',
        'configure_acpid.sh',
        # turning it into a systemvm
        'install_systemvm_packages.sh',
        'configure_conntrack.sh',
        '../../cloud_scripts_shar_archive.sh',
        'configure_systemvm_services.sh',
        'authorized_keys.sh',
        'configure_persistent_config.sh',
        # cleanup & space-saving
        'cleanup.sh',
        'zerodisk.sh',
        # setup login stuff
        'configure_login.sh'
    ],
    :postinstall_timeout => '10000'
}

config.merge! architectures[arch.to_sym]

Veewee::Definition.declare(config)
