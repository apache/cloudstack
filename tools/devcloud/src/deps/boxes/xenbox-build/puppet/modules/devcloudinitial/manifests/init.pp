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

  exec { "apt-update":
    command => "/usr/bin/apt-get update"
  }

  Exec["apt-update"] -> Package <| |>

  package {
    "linux-headers-${::kernelrelease}":
      ensure => latest;
    "xen-hypervisor-4.1-${debarch}":
      ensure  => latest,
      require => Package["linux-headers-${::kernelrelease}"];
    'xcp-xapi':
      ensure  => latest,
      require => Package["xen-hypervisor-4.1-${debarch}"];
    'iptables':
      ensure  => latest;
    'ebtables':
      ensure  => latest;
  }

  file {
  '/etc/iptables.save':
      ensure  => 'file',
      require => Package['iptables'],
      source  => 'puppet:///modules/devcloudinitial/iptables.save',
      group   => '0',
      mode    => '0644',
      owner   => '0';
  '/etc/xcp/network.conf':
      ensure  => 'file',
      require => Package['xcp-xapi'],
      content => "bridge",
      group   => '0',
      mode    => '0644',
      owner   => '0';
  '/etc/init.d/xend':
      ensure  => 'file',
      require => Package['xcp-xapi'],
      source  => 'puppet:///modules/devcloudinitial/xend',
      group   => '0',
      owner   => '0',
      mode    => '0755';
  '/etc/default/grub':
      ensure  => 'file',
      require => Package["xen-hypervisor-4.1-${debarch}"],
      source  => 'puppet:///modules/devcloudinitial/grub',
      group   => '0',
      mode    => '0644',
      owner   => '0';
  '/usr/share/qemu':
      ensure  => 'directory',
      require => Package["xen-hypervisor-4.1-${debarch}"],
      group   => '0',
      mode    => '0755',
      owner   => '0';
  '/usr/share/qemu/keymaps':
      ensure  => 'link',
      require => File['/usr/share/qemu'],
      group   => '0',
      mode    => '0777',
      owner   => '0',
      target  => '/usr/share/qemu-linaro/keymaps';
  '/etc/network/interfaces':
      ensure  => 'file',
      source  => 'puppet:///modules/devcloudinitial/interfaces',
      group   => '0',
      mode    => '0644',
      owner   => '0';
  '/etc/default/xen':
      ensure  => 'file',
      require => Package["xen-hypervisor-4.1-${debarch}"],
      source  => 'puppet:///modules/devcloudinitial/xen-defaults',
      group   => '0',
      mode    => '0644',
      owner   => '0';
  }

  service {
    'xendomains':
      ensure  => 'stopped',
      require => Package['xcp-xapi'],
      enable  => false;
  }

  exec { '/usr/sbin/update-grub':
    subscribe   => File['/etc/default/grub'],
    refreshonly => true,
    cwd         => '/',
  }

}
