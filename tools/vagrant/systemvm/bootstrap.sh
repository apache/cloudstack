#!/bin/bash

set -e
set -x

# script invoked by Test-Kitchen shell provisioner to further
# customize the VM prior to running tests

# for internet access
cat >>/etc/network/interfaces <<END

iface eth1 inet dhcp
auto eth1
END
ifup eth1

# /opt/chef/embedded/bin/gem, etc, expected by test-kitchen
apt-get install curl
curl -L https://www.opscode.com/chef/install.sh | bash
