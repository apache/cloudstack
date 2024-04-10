#!/bin/bash

function create_user() {
    username=$1
    password=$2

    # Create the user with the specified username
    sudo useradd -m -s /bin/bash $username

    # Set the user's password
    echo "$username:$password" | sudo chpasswd

    echo "User '$username' has been created with the password '$password'"
}

sudo mkdir -p /opt/bin
create_user cloud password

echo $SSHKEY
if [[ ! -z "$SSHKEY" ]]; then
  mkdir -p /home/cloud/.ssh/
  mkdir .ssh
  echo $SSHKEY > ~/.ssh/authorized_keys
else
  echo "Please place Management server public key in the variables"
  exit 1
fi
