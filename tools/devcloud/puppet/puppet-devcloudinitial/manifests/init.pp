class puppet-devcloudinitial {

  package { 'linux-headers-3.2.0-23-generic':
    ensure => latest,
  }

  package { 'xen-hypervisor-4.1-amd64':
    ensure => latest,
  }

  package { 'xcp-xapi':
    require => Package['xen-hypervisor-4.1-amd64'],
    ensure  => latest,
  }

  file { '/etc/xcp/network.conf':
    require => Package['xcp-xapi'],
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloudinitial/network.conf',
    group   => '0',
    mode    => '644',
    owner   => '0',
  }

  file { '/etc/init.d/xend':
    require => Package['xcp-xapi'],
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloudinitial/xend',
    group   => '0',
    owner   => '0',
    mode    => '755',
  }

  service { 'xendomains':
    require => Package['xcp-xapi'],
    ensure  => 'stopped',
    enable  => 'false',
  }

  file { '/etc/default/grub':
    require => Package['xen-hypervisor-4.1-amd64'],
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloudinitial/grub',
    group   => '0',
    mode    => '644',
    owner   => '0',
  }

  exec { "/usr/sbin/update-grub":
    subscribe => File['/etc/default/grub'],
    refreshonly => true,
    cwd       => '/',
  }

  file { '/usr/share/qemu':
    require => Package['xen-hypervisor-4.1-amd64'],
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/usr/share/qemu/keymaps':
    require => File['/usr/share/qemu'],
    ensure => 'link',
    group  => '0',
    mode   => '777',
    owner  => '0',
    target => '/usr/share/qemu-linaro/keymaps',
  }

  file { '/etc/network/interfaces':
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloudinitial/interfaces',
    group   => '0',
    mode    => '644',
    owner   => '0',
  }

  file { '/etc/default/xen':
    require => Package['xen-hypervisor-4.1-amd64'],
    ensure  => 'file',
    source  => 'puppet:///modules/puppet-devcloudinitial/xen-defaults',
    group   => '0',
    mode    => '644',
    owner   => '0',
  }

  user { 'root':
    ensure           => 'present',
    comment          => 'root',
    gid              => '0',
    home             => '/root',
    password         => '$6$SCixzUjT$sVs9PwR2g7XdHSLnQW5Zsy2dVpVV3qESFV4Joniusbu3BqWUtKgc91vwEDwPhLqyCYM3kKR1.7G9g2Hu/pTQN/',
    password_max_age => '99999',
    password_min_age => '0',
    shell            => '/bin/bash',
    uid              => '0',
  }

}
