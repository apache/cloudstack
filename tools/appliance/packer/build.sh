#!/bin/bash

#set -x

vboxversion="4.3.18"
varsystemvmname="systemvmtest"
packjsonname="cssysvm_template"
bbuildheader="FEATURE-CENIK123-VPCVRR"
guestaddnpath="http://download.virtualbox.org/virtualbox/"$vboxversion"/VBoxGuestAdditions_"$vboxversion".iso"
btimestamp=$(date +%s)
userroot="~/Development/GitRepositories/SungardASCloudstack3"
csroot=$userroot"/cloudstack"
export PACKER_LOG=true
packerroot=$csroot"/tools/appliance/packer"
vmroot=$packerroot"/vm/"

packercommand="build"

#set +x

while getopts 'VI' OPTION
do
  case $OPTION in
  V) 
     packercommand="validate"
     ;;
  I)
     packercommand="inspect"
     ;;
  ?)	usage
     echo "build [Packer Validate => -V],[ ]"
		;;
  esac
done
set +x

echo "Packer command is ===>"$packercommand


echo "Packer/Vagrant Build Header/Directory for this project ==> "$bbuildheader

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
echo "vm relative Output folder is here ==> $xfolder"
packertemplatefolder="packertemplate/"
packerjsonfile=$packertemplatefolder$packjsonname$btimestamp.json
echo "Packer json file goes here ==> $packerjsonfile"
bvagrantoutput="Vagrantfile"
echo "Vagrantfile goes here ==> $bvagrantoutput"

bbuildoutput="build.sh"
echo "build.sh file goes here ==> $bbuildoutput"

boutputfolder=$vmroot$xofn
echo "Build output folder is ==> $boutputfolder"

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

VAGRANTFILE_API_VERSION = "2"
BOX_NAME = $qbboxname
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = BOX_NAME+".box"

 
  	config.vm.network "private_network", ip: "192.168.22.214", adapter: "2",auto_config: false
    
	config.ssh.private_key_path ="../../validation/vagrant"
        config.ssh.host = "192.168.22.214"
        config.ssh.port = "3922"
        config.ssh.username = "root"


        config.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"

  
   config.vm.provider "virtualbox" do |vb|
     vb.gui = true
     vb.name = "$bboxtitle"
      vb.customize ["modifyvm", :id, "--memory", "1024"]
      vb.customize ["modifyvm", :id, "--nic2", "hostonly"]
      vb.customize ["modifyvm", :id, "--nic3", "nat"]     
      vb.customize ["storageattach", :id, "--storagectl","IDE Controller", "--type","dvddrive","--medium","../../iso/testing.iso","--device","0","--port","1"]

 
  end

   config.vm.provision "shell", path: "../../provision/enablevagrant.sh"
   config.vm.provision "shell", path: "../../provision/proc_cmdline.sh"
end
EOF2

#************************
#Build the packer.json image for Cloudstack SystemVM's

mkdir -m 777 ./$packertemplatefolder
 cat <<"EOF1" > ./$packerjsonfile
{

  "variables": {
     "cloudstackroot" : "",
     "guestadditionspath" : "",
     "ptstamp":"",
     "voutput":"",
     "buildheader":"",
     "vboxv":"",
     "insysvmname":"systemvm",
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
      "environment_vars": [
          "VBOXVERSION={{user `vboxv`}}",
          "SYSTEMVMNAME={{user `insysvmname`}}"
      ],
      "scripts": [
        "{{user `cloudstackroot`}}/tools/appliance/packer/scripts/postinstall.sh",
      	"{{user `cloudstackroot`}}/tools/appliance/packer/scripts/vagrant.sh",
        "{{user `cloudstackroot`}}/tools/appliance/packer/scripts/overlay_scripts.sh",
        "{{user `cloudstackroot`}}/tools/appliance/packer/scripts/cleanup.sh",
        "{{user `cloudstackroot`}}/tools/appliance/packer/scripts/zerodisk.sh"
      ]
    }

],


  "builders": [
     {
      "type": "virtualbox-iso",
      "virtualbox_version_file": "/root/.vbox_version",
      "name": "{{user `ptstamp` }}_{{user `buildheader`}}",
      "output_directory": "output/",
      "boot_wait": "10s",
      "disk_size": 10000,
      "guest_os_type": "Debian",
      "http_directory": "{{user `cloudstackroot`}}/tools/appliance/packer/http",
      "iso_checksum": "7339b668a81b417ac023d73739dc6a03",
      "iso_checksum_type": "md5",
      "iso_url": "http://ftp.cae.tntech.edu/debian-cd/debian-7.4.0-i386-netinst.iso",
      "ssh_username": "root",
      "ssh_password": "password",
      "ssh_port": 22,
      "ssh_wait_timeout": "10000s",
      "guest_additions_sha256": "e5b425ec4f6a62523855c3cbd3975d17f962f27df093d403eab27c0e7f71464a",
      "guest_additions_url": "{{user `guestadditionspath`}}",
      "guest_additions_path": "/tmp/guestAdditions/VBoxGuestAdditions-{{.Version}}.iso",
      "shutdown_command": "echo 'halt -p' > shutdown.sh; echo 'password'|sudo -S sh 'shutdown.sh'",
      "boot_command": [
        "<esc><wait>",
        "install <wait>",
        "preseed/url=http://{{.HTTPIP}}:{{.HTTPPort}}/preseed.cfg.txt <wait>",
	"ignore_loglevel <wait>",
        "debian-installer=en_US <wait>",
        "auto <wait>",
        "locale=en_US <wait>",
        "kbd-chooser/method=us <wait>",
        "netcfg/get_hostname=systemvmtest <wait>",
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
ga=guestadditionspath=$guestaddnpath
vbv=vboxv=$vboxversion
svmn=insysvmname=$varsystemvmname

echo "Var's --> $v, $b, $vo"

logoutput="log/"
echo "Log output directory ==> $logoutput"

cat <<EOF3 > ./$bbuildoutput
mkdir -m 777 ./${logoutput}
packer $packercommand \
-var $v \
-var $ga \
-var $b \
-var $vo \
-var $csr \
-var $vbv \
-var $svmn \
./${packerjsonfile} \
2> ./${logoutput}build-$btimestamp.txt | tee ./${logoutput}packerbuild_$btimestamp-vagrant.log

EOF3


cat ./$bbuildoutput
chmod -R 777 $bbuildoutput

# build the system.
./$bbuildoutput

if [[($packercommand != validate)||($packercommand = inspect)]]
then
    #ok let's bring the systemvm up
    echo "Starting system vm"$bboxname" in folder "$boutputfolder", using "$bboxtitle" as a title"

    
    vagrant up
    echo "Use the command ===>> cd "$boutputfolder" to access the vagrant box folder"
fi
