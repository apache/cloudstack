

cat << "EOF" >./VagrantfileTEST1
# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
BUILD_TIMESTAMP=$1
VAGRANTFILE_API_VERSION = "2"
TIMESTAMP = (((Time.now.getutc).to_i).to_s)
BOX_NAME = "packer_"+BUILD_TIMESTAMP+"_FEATURE-CENIK123-VPCVRR_virtualbox"
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  # All Vagrant configuration is done here. The most common configuration
  # options are documented and commented below. For a complete reference,
  # please see the online documentation at vagrantup.com.

  # Every Vagrant virtual environment requires a box to build off of.
  config.vm.box = BOX_NAME+".box"

 
  # Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
   config.vm.network "private_network", ip: "169.254.2.245", auto_config: false
   config.vm.network "forwarded_port", guest: 3922 , host: 2223, host_ip: "196.254.2.245", guest_ip: "169.254.2.214" 
  # If true, then any SSH connections made will enable agent forwarding.
  # Default value: false
  #config.ssh.forward_agent = true

  #config.ssh.username = "root"
 #config.ssh.password = "password"
 # config.ssh.host = "169.254.2.214"
  config.ssh.port = "2223"
#  config.ssh.guest_port = "3922"


  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"


  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

  # Provider-specific configuration so you can fine-tune various
  # backing providers for Vagrant. These expose provider-specific options.
  # Example for VirtualBox:
  #
   config.vm.provider "virtualbox" do |vb|
  #   # Don't boot with headless mode
     vb.gui = true
     vb.name = "TVM"+TIMESTAMP
  #
  #   # Use VBoxManage to customize the VM. For example to change memory:
  #   vb.customize ["modifyvm", :id, "--memory", "1024"]
     vb.customize ["modifyvm", :id, "--nic2", "nat"]
     vb.customize ["modifyvm", :id, "--nic3", "nat"]     
   end
  #
  # View the documentation for the provider you're using for more
  # information on available options.

end

EOF
