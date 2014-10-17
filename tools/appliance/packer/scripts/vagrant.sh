#!/usr/bin/env bash
set -x

  # Create a 'vagrant' user if it's not there
 
  id vagrant
  
  if [[ $? -ne 0 ]]
  then
    useradd -G admin vagrant
  else
    usermod -a -G admin vagrant
  fi


#Install vagrant ssh key
mkdir /home/vagrant
mkdir /home/vagrant/.ssh
wget --no-check-certificate -O authorized_keys 'https://github.com/mitchellh/vagrant/raw/master/keys/vagrant.pub'
mv authorized_keys /home/vagrant/.ssh
chown -R vagrant /home/vagrant/.ssh
chmod -R go-rwsx /home/vagrant/.ssh

#This should've been done in baseTest.sh
#Add vagrant user to passwordless sudo
#cp /etc/sudoers{,.orig}
#sed -i -e 's/%sudo\s\+ALL=(ALL\(:ALL\)\?)\s\+ALL/%sudo ALL=NOPASSWD:ALL/g' /etc/sudoers
