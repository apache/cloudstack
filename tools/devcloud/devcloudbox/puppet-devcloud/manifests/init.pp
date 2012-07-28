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

}
