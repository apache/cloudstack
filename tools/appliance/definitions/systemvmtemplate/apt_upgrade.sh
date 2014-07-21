# Update the box

export DEBIAN_FRONTEND=noninteractive
export DEBIAN_PRIORITY=critical

apt-get -q -y --force-yes update
apt-get -q -y --force-yes install curl unzip
apt-get clean
