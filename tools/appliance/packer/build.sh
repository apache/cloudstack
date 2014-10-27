#!/bin/bash


#set -x

export PACKER_LOG=true
csroot="/Users/karl.harris/Development/GitRepositories/SungardASCloudstack3/cloudstack"


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

xfolder="./"$xofn"/"
echo "Output folder is here ==> $xfolder"

packertemplatefolder="packertemplate/"

packerjsonfile=$packertemplatefolder"cssysvm_template"$btimestamp.json
echo "Packer json file goes here ==> $packerjsonfile"

bvagrantoutput="Vagrantfile"
echo "Vagrantfile goes here ==> $bvagrantoutput"

bbuildoutput="build.sh"
echo "build.sh file goes here ==> $bbuildoutput"

#************************

export PACKER_CACHE_DIR=$PWD"/iso"
echo "ISO CACHE ==>$PACKER_CACHE_DIR"
cd ./vm/

mkdir $xfolder
chmod -R 777 $xfolder
cd $xfolder

#but first put a customized vagrantfile in the timestamped
#directory pointed to by xfolder.
cat <<EOF2 > ./$bvagrantoutput

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

mkdir -m 777 ./$packertemplatefolder
 cat <<"EOF1" > ./$packerjsonfile
{

  "variables": {
     "cloudstackroot" : "",
     "ptstamp":"",
     "voutput":"",
     "buildheader":"",
     "dummy":"dummy"
   },


  "provisioners": [
    {	
      "type": "shell","scripts": [
        "{{user `cloudstackroot`}}/tools/appliance/packer/scripts/baseTest.sh"

      ]
    },
  {
      "type":"file",
      "source":"{{user `cloudstackroot`}}/systemvm/patches/debian/config",
      "destination":"/opt/cloudstack/systemvm/patches/debian"
  }, 
 {
      "type":"file",
      "source":"{{user `cloudstackroot`}}/systemvm/patches/debian/vpn",
      "destination":"/opt/cloudstack/systemvm/patches/debian"
},
    {	
      "type": "shell",
      "scripts": [
        "{{user `cloudstackroot`}}/tools/appliance/packer/scripts/postinstall.sh",
      	"{{user `cloudstackroot`}}/tools/appliance/packer/scripts/vagrant.sh",
        "{{user `cloudstackroot`}}/tools/appliance/packer/scripts/cleanup.sh",
        "{{user `cloudstackroot`}}/tools/appliance/packer/scripts/zerodisk.sh"
      ]
    }

],


  "builders": [
     {
      "type": "virtualbox-iso",
      "name": "{{user `ptstamp` }}_{{user `buildheader`}}",
      "output_directory": "output/",
      "boot_wait": "10s",
      "disk_size": 2500,
      "guest_os_type": "Debian",
      "http_directory": "{{user `cloudstackroot`}}/tools/appliance/packer/http",
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
	    "output": "packerbuilt_{{.BuildName}}_{{.Provider}}.box"
	}
]

}
EOF1
#************************
v=ptstamp=$bptstamp
b=buildheader=$bbuildheader
vo=voutput=$xfolder
csr=cloudstackroot=$csroot
echo "Var's --> $v, $b, $vo"

logoutput="log/"
echo "Log output directory ==> $logoutput"

cat <<EOF3 > ./$bbuildoutput
mkdir -m 777 ./${logoutput}
packer build \
-var $v \
-var $b \
-var $vo \
-var $csr \
./${packerjsonfile} \
2> ./${logoutput}build-$btimestamp.txt | tee ./${logoutput}packerbuild_$btimestamp-vagrant.log

EOF3


cat ./$bbuildoutput
chmod -R 777 $bbuildoutput

# build the system.
./$bbuildoutput

#ok let's bring the systemvm up
echo "Starting system vm \("$bboxname"\) in folder "$boutputfolder", using "$bboxtitle" as a title"

vagrant up

