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

  exec { "mac=`ifconfig xenbr0 |grep HWaddr |awk '{print $5}'`; /sbin/ebtables -I FORWARD -d ! $mac -i eth0 -p IPV4 --ip-prot udp --ip-dport 67:68 -j DROP":
    subscribe => Package['ebtables'],
    refreshonly => true,
    cwd       => '/',
  }

  package { 'nfs-server':
    ensure => latest,
  }

  file { '/opt/storage/secondary':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
    type   => 'directory',
  }

  file { '/opt/storage/secondary/template/tmpl/1/1':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
    type   => 'directory',
  }

  file { '/opt/storage/secondary/template/tmpl/1/5':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
    type   => 'directory',
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
    requires => Package['nfs-server'],
    ensure => 'running',
    enable => 'true',
  }

  exec { "wget-default-template-1-vhd":
    command => "/usr/bin/wget --output-document=/opt/storage/secondary/template/tmpl/1/1/dc68eb4c-228c-4a78-84fa-b80ae178fbfd.vhd http://download.cloud.com/templates/devcloud/defaulttemplates/1/dc68eb4c-228c-4a78-84fa-b80ae178fbfd.vhd",
    creates => "/opt/storage/secondary/template/tmpl/1/1/dc68eb4c-228c-4a78-84fa-b80ae178fbfd.vhd",
    requires => File['/opt/storage/secondary/template/tmpl/1/1'],
    cwd     => '/',
  }

  exec { "wget-default-template-1-props":
    command => "/usr/bin/wget --output-document=/opt/storage/secondary/template/tmpl/1/1/template.properties http://download.cloud.com/templates/devcloud/defaulttemplates/1/template.properties",
    creates => "/opt/storage/secondary/template/tmpl/1/1/template.properties",
    requires => File['/opt/storage/secondary/template/tmpl/1/1'],
    cwd     => '/',
  }

  exec { "wget-default-template-5-vhd":
    command => "/usr/bin/wget --output-document=/opt/storage/secondary/template/tmpl/1/5/ce5b212e-215a-3461-94fb-814a635b2215.vhd http://download.cloud.com/templates/devcloud/defaulttemplates/5/ce5b212e-215a-3461-94fb-814a635b2215.vhd",
    creates => "/opt/storage/secondary/template/tmpl/1/5/ce5b212e-215a-3461-94fb-814a635b2215.vhd",
    requires => File['/opt/storage/secondary/template/tmpl/1/5'],
    cwd     => '/',
  }

  exec { "wget-default-template-5-props":
    command => "/usr/bin/wget --output-document=/opt/storage/secondary/template/tmpl/1/5/template.properties http://download.cloud.com/templates/devcloud/defaulttemplates/5/template.properties",
    creates => "/opt/storage/secondary/template/tmpl/1/5/template.properties",
    requires => File['/opt/storage/secondary/template/tmpl/1/5'],
    cwd     => '/',
  }

  file { '/opt/storage/primary':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
    type   => 'directory',
  }

  exec { "configlocal":
    requires => File['/opt/storage/primary'],
    unless  => 'xe sr-list | grep local-storage',
    command => "hostuuid=`xe host-list |grep uuid|awk '{print $5}'`; xe sr-create host-uuid=$hostuuid name-label=local-storage shared=false type=file device-config:location=/opt/storage/primary",
    cwd     => '/',
  }


}
