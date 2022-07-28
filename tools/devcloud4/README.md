# Devcloud 4

## Introduction

The follow project aims to simplify getting a full Apache CloudStack environment running on your machine. You can either take the easy ride and run `vagrant up` in either one of the 'binary installation' directories or compile CloudStack yourself. See for instructions in the 'basic' and 'advanced' directories.

The included VagrantFile will give you:

 - Management
     - NFS Server
     - MySQL Server
     - Router
     - * Cloudstack Management Server * (Only given in binary installation)

 - XenServer 6.2

## Getting started

1. Due to the large amount of data to be pulled from the Internet, it's probably not a good idea to do this over WiFi or Mobile data.

1. Given the amount of virtual machines this brings up it is recommended you have at least 8gb of ram before attempting this.

1. Ensure your system has `git` installed.

1. When on Windows, make sure you've set the git option `autocrlf` to `false`:

      ```
      git config --global core.autocrlf false
      ```

1. Clone the repository:

	```
	git clone https://github.com/imduffy15/devcloud4.git
	```

1. Download and Install [VirtualBox](https://www.virtualbox.org/wiki/Downloads)
   
   On Windows7, the Xenserver VM crashed immediately after booting with a General Protection Fault. 
   Installing VirtualBox version 4.3.6r91406 (https://www.virtualbox.org/wiki/Download_Old_Builds_4_3) fixed the problem, but only downgrade if the latest version does not work for you.
 
1. Download and install [Vagrant](https://www.vagrantup.com/downloads.html)

1. Ensure all Vagrant Plugins are installed:

	```bash
	vagrant plugin install vagrant-berkshelf vagrant-omnibus
	```

1. Download and install [ChefDK](https://downloads.chef.io/chef-dk/)

### Configure virtualbox

1. Open virtualbox and navigate to its preferences/settings window. 

1. Click onto the network tab and then onto the host only network tab. 

1. Configure your adapters as follows:

   - On Windows, the adapternames are different, and map as follows:
     - vboxnet0: VirtualBox Host-Only Ethernet Adapter
     - vboxnet1: VirtualBox Host-Only Ethernet Adapter 2
     - vboxnet2: VirtualBox Host-Only Ethernet Adapter 3
    
    #### For Basic Networking you only need:

    ##### vboxnet0
    - IPv4 IP address of 192.168.22.1
    - Subnet of 255.255.255.0
    - DHCP server disabled
    
    #### For Advanced Networking you will need:
    
    
    
    ##### vboxnet1
    - IPv4 IP address of 192.168.23.1
    - Subnet of 255.255.255.0
    - DHCP server disabled
    
    
    
    ##### vboxnet2
    - IPv4 IP address of 192.168.24.1
    - Subnet of 255.255.255.0
    - DHCP server disabled

## Defaults

### Management Server

 - IP: 192.168.22.5
 - Username: vagrant or root
 - Password: vagrant

### Hypervisor

 - IP: 192.168.22.10
 - Username: root
 - Password: password
    
