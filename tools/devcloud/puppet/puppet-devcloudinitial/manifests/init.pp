class puppet-devcloudinitial {

  package { 'linux-headers-3.2.0-23-generic':
    ensure => latest,
  }

  package { 'xen-hypervisor-4.1-amd64':
    ensure => latest,
  }

  package { 'xcp-xapi':
    ensure => latest,
  }

  file { '/etc/xcp/network.conf':
    ensure  => 'file',
    source  => 'puppet:///files/puppet-devcloudinitial/files/network.conf',
    group   => '0',
    mode    => '644',
    owner   => '0',
  }

  exec { "/bin/sed -i -e 's/xend_start$/#xend_start/' -e 's/xend_stop$/#xend_stop/' /etc/init.d/xend":
    cwd     => '/etc/init.d',
  }

  service { 'xendomains':
    ensure => 'stopped',
    enable => 'false',
  }

  file { '/etc/default/grub':
    ensure  => 'file',
    source  => 'puppet:///files/puppet-devcloudinitial/files/grub',
    group   => '0',
    mode    => '644',
    owner   => '0',
  }

  exec { "/usr/sbin/update-grub":
    cwd     => '/',
  }

  file { '/usr/share/qemu':
    ensure => 'directory',
    group  => '0',
    mode   => '755',
    owner  => '0',
  }

  file { '/usr/share/qemu/keymaps':
    ensure => 'link',
    group  => '0',
    mode   => '777',
    owner  => '0',
    target => '/usr/share/qemu-linaro/keymaps',
  }

  file { '/etc/network/interfaces':
    ensure  => 'file',
    source  => 'puppet:///files/puppet-devcloudinitial/files/interfaces',
    group   => '0',
    mode    => '644',
    owner   => '0',
  }

  file { '/etc/default/xen':
    ensure  => 'file',
    source  => 'puppet:///files/puppet-devcloudinitial/files/xen-defaults',
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
