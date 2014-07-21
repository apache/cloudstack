# Update the box

export DEBIAN_FRONTEND=noninteractive
export DEBIAN_PRIORITY=critical

add_backports () {
    sed -i '/backports/d' /etc/apt/sources.list
    echo 'deb http://http.us.debian.org/debian wheezy-backports main' >> /etc/apt/sources.list
}

add_backports

apt-get -q -y --force-yes update
apt-get -q -y --force-yes install curl unzip
apt-get clean
