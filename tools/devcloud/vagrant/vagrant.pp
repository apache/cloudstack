group { 'vagranttest':
  ensure => 'present',
  gid    => '5000',
}

group { 'admin':
  ensure => 'present',
  gid    => '1002',
}

user { 'vagranttest':
  ensure  => 'present',
  comment => 'vagrant,,,',
  gid     => '5000',
  groups  => ['adm', 'cdrom', 'dip', 'plugdev', 'lpadmin', 'sambashare', 'admin'],
  home    => '/home/vagranttest',
  shell   => '/bin/bash',
  uid     => '5000',
}

file { '/home/vagranttest':
  ensure => 'directory',
  group  => '1002',
  mode   => '755',
  owner  => '5000',
}

file { '/home/vagranttest/.ssh':
  ensure => 'directory',
  group  => '1002',
  mode   => '775',
  owner  => '5000',
}

$auth_key = "ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA6NF8iallvQVp22WDkTkyrtvp9eWW6A8YVr+kz4TjGYe7gHzIw+niNltGEFHzD8+v1I2YJ6oXevct1YeS0o9HZyN1Q9qgCgzUFtdOKLv6IedplqoPkcmF0aYet2PkEDo3MlTBckFXPITAMzF8dJSIFo9D8HfdOV0IAdx4O7PtixWKn5y2hMNG0zQPyUecp4pzC6kivAIhyfHilFR61RGL+GPXQ2MWZWFYbAGjyiYJnAmCP3NOTd0jMZEnDkbUvxhMmBYSdETk1rRgm+R4LOzFUGaHqHDLKLX+FIPKcF96hrucXzcWyLbIbEgE98OHlnVYCzRdK8jlqm8tehUc9c9WhQ== vagrant insecure public key"

file { '/home/vagranttest/.ssh/authorized_keys':
  ensure  => 'file',
  content => $auth_key,
  group   => '1002',
  mode    => '664',
  owner   => '5000',
}
