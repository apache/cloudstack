#!/bin/bash


set -x

bbuildheader="FEATURE-CENIK123-VPCVRR"
echo "Packer/Vagrant Build Header/Directory for this project ==> "$bbuildheader

btimestamp=$(date +%s)
echo "Packer/Vagrant Timestamp for this build => $btimestamp"

bptstamp=$btimestamp

bboxname="packerbuilt_"$btimestamp"_"$bbuildheader"_virtualbox"

qbboxname=\"$bboxname\"

echo "Virtualbox Name is ==> $bboxname"
echo "QBBox Name(virtual box name with quotes) ==> $qbboxname"

bboxtitle="TVM$btimestamp"
echo "Virtualbox Title is (used for vagrant up) => $bboxtitle"

xofn="output_"$btimestamp"_"$bbuildheader"_virtualbox"
echo "Virtualbox output folder name is ==> $xofn"

xfolder="./vm/"$xofn"/"
echo "Output folder is here ==> $xfolder"

packerjsonfile="./packertemplates/cssysvm_template$btimestamp.json"
echo "Packer json file goes here ==> $packerjsonfile"

bvagrantoutput=$xfolder"Vagrantfile"
echo "Vagrantfile goes here ==> $bvagrantoutput"

#************************
#but first put a customized vagrantfile in the timestamped
#directory pointed to by xfolder.

mkdir $xfolder

cat <<EOF2 > $bvagrantoutput

# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"
#TIMESTAMP = (((Time.now.getutc).to_i).to_s)
BOX_NAME = $qbboxname
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = BOX_NAME+".box"

 
  #	config.vm.network :hostonly, "192.168.56.110", :adapter => 4
  	config.vm.network "private_network", ip: "169.254.2.214", adapter: "2"
  # config.vm.network "forwarded_port", guest: 3922 , host: 2223, host_ip: "192.168.56.1", guest_ip: "192.168.56.110"
    
  #config.ssh.forward_agent = true

#	config.ssh.username = "vagrant"
#	config.ssh.password = "vagrant"
	config.ssh.private_key_path ="../../validation/vagrant"
        config.ssh.host = "169.254.2.214"
        config.ssh.port = "3922"

#Fix "is not a tty" error when /sbin/ip addr flush dev eth 2> /dev/null command is issued
        config.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"
#  config.ssh.guest_port = "3922"


  # config.vm.synced_folder "shared", "~/development/cloudstack"


  # config.vm.synced_folder "../data", "/vagrant_data"

  
   config.vm.provider "virtualbox" do |vb|
     vb.gui = true
     vb.name = "$bboxtitle"
      vb.customize ["modifyvm", :id, "--memory", "1024"]
      vb.customize ["modifyvm", :id, "--nic2", "hostonly"]
      vb.customize ["modifyvm", :id, "--nic3", "nat"]     
      vb.customize ["modifyvm", :id, "--nicpromisc2", "allow-all"]
 #	 vb.customize("post-boot", ["guestcontrol", :id, "exec", "--username", "vagrant", "--password", "vagrant", "touch /home/vagrant/tpp.test"]) 
  end

   config.vm.provision "shell", path: "../../provision/enablevagrant.sh"
   config.vm.provision "shell", path: "../../provision/proc_cmdline.sh"
   config.vm.provision "shell", path: "../../provision/overlay_scripts.sh"   
end
EOF2

#************************
#Build the packer.json image for Cloudstack SystemVM's
cat <<"EOF1" > $packerjsonfile

{

  "provisioners": [
    {	
      "type": "shell","scripts": [
        "scripts/baseTest.sh"

      ]
    },
  {
      "type":"file",
      "source":"./../../../../cloudstack/systemvm/patches/debian/config",
      "destination":"/opt/cloudstack/systemvm/patches/debian"
  }, 
 {
      "type":"file",
      "source":"./../../../../cloudstack/systemvm/patches/debian/vpn",
      "destination":"/opt/cloudstack/systemvm/patches/debian"
},
    {	
      "type": "shell",
      "scripts": [
        "scripts/postinstall.sh",
      	"scripts/vagrant.sh",
        "scripts/cleanup.sh",
        "scripts/zerodisk.sh"
      ]
    }

],

  "variables": {
     "ptstamp":"",
     "voutput":"",
     "buildheader":"",
     "dummy":"dummy"
   },

  "builders": [
     {
      "type": "virtualbox-iso",
      "name": "{{user `ptstamp` }}_{{user `buildheader`}}",
      "output_directory": "{{user `voutput`}}output/",
      "boot_wait": "10s",
      "disk_size": 2500,
      "guest_os_type": "Debian",
      "http_directory": "http",
      "iso_checksum": "7339b668a81b417ac023d73739dc6a03",
      "iso_checksum_type": "md5",
      "iso_url": "http://ftp.cae.tntech.edu/debian-cd/debian-7.4.0-i386-netinst.iso",
      "ssh_username": "root",
      "ssh_password": "password",
      "ssh_port": 22,
      "ssh_wait_timeout": "10000s",
      "guest_additions_mode":"attach",
      "shutdown_command": "echo 'halt -p' > shutdown.sh; echo 'password'|sudo -S sh 'shutdown.sh'",
      "virtualbox_version_file": ".vbox_version",
      "boot_command": [
        "<esc><wait>",
        "install <wait>",
        "preseed/url=http://{{.HTTPIP}}:{{.HTTPPort}}/preseed.cfg.txt <wait>",
	"ignore_loglevel <wait>",
        "debian-installer=en_US <wait>",
        "auto <wait>",
        "locale=en_US <wait>",
        "kbd-chooser/method=us <wait>",
        "netcfg/get_hostname=systemvm <wait>",
        "netcfg/get_domain=apache.org <wait>",
	"fb=false <wait>",
        "debconf/frontend=noninteractive <wait>",
	"boot_debug=2 <wait>",
        "console-setup/ask_detect=false <wait>",
        "console-keymaps-at/keymap=us <wait>",
        "keyboard-configuration/xkb-keymap=us <wait>",
        "<enter><wait>"
      ],
       "vboxmanage": [
        ["modifyvm","{{.Name}}","--memory","256"],
        ["modifyvm","{{.Name}}","--cpus","1"]


      ]
   }
],
    "Post-processors":[
	{
	    "type":"vagrant",
	    "keep_input_artifact":true,
            "vagrantfile_template" : "./vm/output_{{.BuildName}}_{{.Provider}}/Vagrantfile",
	    "output": "./vm/output_{{.BuildName}}_{{.Provider}}/packer_{{.BuildName}}_{{.Provider}}.box"
	}
]

}
EOF1
#************************
v=ptstamp=$bptstamp
b=buildheader=$bbuildheader
vo=voutput=$xfolder
echo "Var's --> $v, $b, $vo"

packer build \
-var $v \
-var $b \
-var $vo \
$packerjsonfile \
2> ./log/build-$btimestamp.txt | tee ./log/packerbuild_$btimestamp-vagrant.log

#ok let's bring the systemvm up
echo "Starting system vm \("$bboxname"\) in folder "$boutputfolder", using "$bboxtitle" as a title"
chmod 777 ./vm/$boutputfolder
cd ./vm/$boutputfolder/
vagrant up

