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

class puppet-devcloud {

  package { 'ebtables':
    ensure => latest,
  }

  service { 'ebtables':
    require => Package['ebtables'],
    ensure  => 'running',
    enable  => 'true',
  }

  package { 'iptables':
    ensure => latest,
  }

  file { '/etc/iptables.save':
    require => Package['iptables'],
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloud/iptables.save',
    group   => '0',
    mode    => '644',
    owner   => '0',
  }

  file { '/tmp/configebtables.sh':
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloud/configebtables.sh',
    group   => '0',
    mode    => '777',
    owner   => '0',
  }

  exec { "/tmp/configebtables.sh":
    require => [
      File['/tmp/configebtables.sh'],
      Service['ebtables']
      ],
    subscribe => Package['ebtables'],
    refreshonly => true,
    cwd       => '/',
    path      => '/sbin/:/usr/bin/:/bin',
  }

  package { 'nfs-server':
    ensure => latest,
  }

  file { '/opt/storage':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner => '0',
  }

  file { '/opt/storage/secondary':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/opt/storage/secondary/template':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/opt/storage/secondary/template/tmpl':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/opt/storage/secondary/template/tmpl/1':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/opt/storage/secondary/template/tmpl/1/1':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/opt/storage/secondary/template/tmpl/1/5':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/etc/exports':
    require => Package['nfs-server'],
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloud/exports',
    mode    => '644',
    owner   => '0',
    group   => '0',
  }

  service { 'nfs-kernel-server':
    require => Package['nfs-server'],
    ensure => 'running',
    enable => 'true',
  }

# TODO - it would be great to have an MD5 sum to check for each of these downloads, so that the files can be re-downloaded if they have been changed.

  exec { '/usr/bin/wget http://download.cloud.com/templates/devcloud/defaulttemplates/1/dc68eb4c-228c-4a78-84fa-b80ae178fbfd.vhd -P /opt/storage/secondary/template/tmpl/1/1/':
    creates => '/opt/storage/secondary/template/tmpl/1/1/dc68eb4c-228c-4a78-84fa-b80ae178fbfd.vhd',
    require => File['/opt/storage/secondary/template/tmpl/1/1/'],
    timeout => '0',
  }

  exec { '/usr/bin/wget http://download.cloud.com/templates/devcloud/defaulttemplates/1/template.properties -P /opt/storage/secondary/template/tmpl/1/1/':
    creates => '/opt/storage/secondary/template/tmpl/1/1/template.properties',
    require => File['/opt/storage/secondary/template/tmpl/1/1/'],
  }

  exec { '/usr/bin/wget http://download.cloud.com/templates/devcloud/defaulttemplates/5/ce5b212e-215a-3461-94fb-814a635b2215.vhd -P /opt/storage/secondary/template/tmpl/1/5/':
    creates => '/opt/storage/secondary/template/tmpl/1/5/ce5b212e-215a-3461-94fb-814a635b2215.vhd',
    require => File['/opt/storage/secondary/template/tmpl/1/5/'],
    timeout => '0',
  }

  exec { '/usr/bin/wget http://download.cloud.com/templates/devcloud/defaulttemplates/5/template.properties -P /opt/storage/secondary/template/tmpl/1/5/':
    creates => '/opt/storage/secondary/template/tmpl/1/5/template.properties',
    require => File['/opt/storage/secondary/template/tmpl/1/5/'],
  }

  exec { 'getecho':
    command => '/usr/bin/wget http://download.cloud.com/templates/devcloud/echo -P /usr/lib/xcp/plugins/',
    creates => '/usr/lib/xcp/plugins/echo',
  }

  exec { '/bin/chmod -R 777 /usr/lib/xcp':
    require => Exec['getecho'],
  }

  file { '/opt/storage/primary':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/tmp/configlocalstorage.sh':
    ensure  => 'file',
    group   => '0',
    mode    => '777',
    owner   => '0',
    source  => 'puppet:///modules/puppet-devcloud/configlocalstorage.sh',
  }

  exec { "configlocal":
    require => [
      File['/opt/storage/primary'],
      File['/tmp/configlocalstorage.sh']
      ],
    command => '/tmp/configlocalstorage.sh',
    cwd     => '/',
  }

  file { '/tmp/configvnc.sh':
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloud/configvnc.sh',
    mode    => '777',
    group   => '0',
    owner   => '0',
  }

  exec { "configvnc":
    require => File['/tmp/configvnc.sh'],
    command => '/tmp/configvnc.sh',
    cwd     => '/',
  }

  package { 'git':
    ensure  => latest,
  }

  package { 'unzip':
    ensure  => latest,
  }

  package { 'mysql-server':
    ensure  => latest,
  }

  package { 'ant':
    ensure  => latest,
  }

  package { 'openjdk-6-jdk':
    ensure  => latest,
  }

  file { '/opt/cloudstack':
    ensure  => 'directory',
    group   => '0',
    mode    => '755',
    owner   => '0',
  }

  file { '/tmp/updatecode.sh':
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloud/updatecode.sh',
    mode    => '777',
    owner   => '0',
    group   => '0',
  }

  exec { 'get_code':
    require => [
      Package['git'],
      File['/opt/cloudstack/'],
      File['/tmp/updatecode.sh']
      ],
    command => '/tmp/updatecode.sh',
    cwd     => '/opt/cloudstack/',
    timeout => '0',
  }

  file { '/opt/cloudstack/incubator-cloudstack/target':
    ensure      => 'directory',
    group       => '0',
    mode        => '755',
    owner       => '0',
    require    => Exec['get_code'],
  }

  file { '/opt/cloudstack/incubator-cloudstack/dist':
    ensure      => 'directory',
    group       => '0',
    mode        => '755',
    owner       => '0',
    require    => Exec['get_code'],
  }

  exec { 'downloadtomcat':
    command => '/usr/bin/wget http://archive.apache.org/dist/tomcat/tomcat-6/v6.0.32/bin/apache-tomcat-6.0.32.zip -P /opt/cloudstack/',
    creates => '/opt/cloudstack/apache-tomcat-6.0.32.zip',
    require => File['/opt/cloudstack/'],
    timeout => '0',
  }

  exec { "unziptomcat":
    require => [
      Package['unzip'],
      Exec["downloadtomcat"]
      ],
    creates => "/opt/cloudstack/apache-tomcat-6.0.32",
    command => "/usr/bin/unzip apache-tomcat-6.0.32.zip",
    cwd     => "/opt/cloudstack",
    timeout => '0',
  }

  exec { "catalina_home":
    require => Exec["unziptomcat"],
    unless  => '/bin/grep CATALINA_HOME /root/.bashrc',
    command => '/bin/echo "export CATALINA_HOME=/opt/cloudstack/apache-tomcat-6.0.32" >> /root/.bashrc',
    cwd     => '/',
  }

  package { 'mkisofs':
    ensure  => latest,
  }

  exec { "build_cloudstack":
    require => [
      Package['ant'],
      Exec["catalina_home"],
      File['/opt/cloudstack/incubator-cloudstack/dist'],
      File['/opt/cloudstack/incubator-cloudstack/target'],
      Package['mkisofs']
      ],
    command => "/usr/bin/ant clean-all build-all deploy-server deploydb",
    cwd     => "/opt/cloudstack/incubator-cloudstack/",
    timeout => '0',
  }

  file { '/opt/cloudstack/startdevcloud.sh':
    ensure  => 'file', 
    source  => 'puppet:///modules/puppet-devcloud/startdevcloud.sh', 
    mode    => '777', 
    owner   => '0', 
    group   => '0',
  }

  exec { "start_cloudstack":
    require => [
      Exec["build_cloudstack"],
      File["/opt/cloudstack/startdevcloud.sh"]
      ],
    command => "/opt/cloudstack/startdevcloud.sh",
    cwd     => "/opt/cloudstack/",
  }

}
