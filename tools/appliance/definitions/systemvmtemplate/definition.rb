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
        :iso_file => 'debian-live-8.6.0-amd64-standard.iso',
        :iso_src => 'http://cdimage.debian.org/debian-cd/current-live/amd64/iso-hybrid/debian-live-8.6.0-amd64-standard.iso',
        :iso_md5 => '06b3cc0a1430c2aaf449b46c72fecee5'
    }
}

config = {
    :cpu_count => '1',
    :memory_size => '256',
    :disk_size => '3500', :disk_format => 'VDI', :hostiocache => 'off',
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
        # fix account and logins
        'configure_login.sh'
    ],
    :postinstall_timeout => '1200'
}

config.merge! architectures[arch.to_sym]

Veewee::Definition.declare(config)
