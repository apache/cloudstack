#
# NOTE: Before changing the version of the debian image make
# sure it is added to the userContent of jenkins.buildacloud.org
# and the copy task is updated on the systemvm builds
# This will prevent the inevitable build failure once the iso is
# removed from the debian mirrors
#
Veewee::Definition.declare({
  :cpu_count => '1',
  :memory_size=> '256',
  :disk_size => '3000', :disk_format => 'VDI', :hostiocache => 'off',
  :os_type_id => 'Debian_64',
  :iso_file => "debian-7.8.0-amd64-netinst.iso",
  :iso_src => "http://cdimage.debian.org/cdimage/archive/7.8.0/amd64/iso-cd/debian-7.8.0-amd64-netinst.iso",
  :iso_md5 => "a91fba5001cf0fbccb44a7ae38c63b6e",
  :iso_download_timeout => "1000",
  :boot_wait => "10", :boot_cmd_sequence => [
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
  :kickstart_port => "7122",
  :kickstart_timeout => "10000",
  :kickstart_file => "preseed.cfg",
  :ssh_login_timeout => "10000",
  :ssh_user => "root",
  :ssh_password => "password",
  :ssh_key => "",
  :ssh_host_port => "7222",
  :ssh_guest_port => "22",
  :sudo_cmd => "echo '%p'|sudo -S sh '%f'",
  :shutdown_cmd => "halt -p",
  :postinstall_files => [
    "base.sh",
    "postinstall.sh",
    "cleanup.sh",
    "zerodisk.sh"
  ],
  :postinstall_timeout => "10000"
})
