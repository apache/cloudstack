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

class devcloudinitial {

if $::architecture == 'x86_64'{
  $debarch='amd64'
}
else {
  $debarch='i386'
}
  package {
    "linux-headers-${::kernelrelease}":
      ensure => latest;
    "xen-hypervisor-4.1-${debarch}":
      ensure => latest,
      require => Package["linux-headers-${::kernelrelease}"];
    "xcp-xapi":
      require => Package["xen-hypervisor-4.1-${debarch}"],
      ensure  => latest;
  }

  file {
     '/etc/xcp/network.conf':
        require => Package['xcp-xapi'],
        ensure  => 'file',
        source  => 'puppet:///modules/devcloudinitial/network.conf',
        group   => '0',
        mode    => '644',
        owner   => '0';
    '/etc/init.d/xend':
        require => Package['xcp-xapi'],
        ensure  => 'file',
        source  => 'puppet:///modules/devcloudinitial/xend',
        group   => '0',
        owner   => '0',
        mode    => '755';
    '/etc/default/grub':
        require => Package["xen-hypervisor-4.1-${debarch}"],
        ensure  => 'file',
        source  => 'puppet:///modules/devcloudinitial/grub',
        group   => '0',
        mode    => '644',
        owner   => '0';
    '/usr/share/qemu':
        require => Package["xen-hypervisor-4.1-${debarch}"],
        ensure => 'directory',
        group  => '0',
        mode   => '755',
        owner  => '0';
    '/usr/share/qemu/keymaps':
        require => File['/usr/share/qemu'],
        ensure => 'link',
        group  => '0',
        mode   => '777',
        owner  => '0',
        target => '/usr/share/qemu-linaro/keymaps';
    '/etc/network/interfaces':
        ensure  => 'file',
        source  => 'puppet:///modules/devcloudinitial/interfaces',
        group   => '0',
        mode    => '644',
        owner   => '0';
    '/etc/default/xen':
        require => Package["xen-hypervisor-4.1-${debarch}"],
        ensure  => 'file',
        source  => 'puppet:///modules/devcloudinitial/xen-defaults',
        group   => '0',
        mode    => '644',
        owner   => '0';

  }

  service {
    'xendomains':
      require => Package['xcp-xapi'],
      ensure  => 'stopped',
      enable  => false;
  }


  exec { "/usr/sbin/update-grub":
    subscribe => File['/etc/default/grub'],
    refreshonly => true,
    cwd       => '/',
  }

}
