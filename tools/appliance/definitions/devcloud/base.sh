# Update the box
apt-get -y update
#below are needed for ruby perhaps
#apt-get -y install linux-headers-$(uname -r) build-essential
#apt-get -y install zlib1g-dev libssl-dev libreadline-gplv2-dev
apt-get -y install curl unzip
apt-get clean

echo 'cloud ALL=NOPASSWD:ALL' > /etc/sudoers.d/cloud

# Tweak sshd to prevent DNS resolution (speed up logins)
echo 'UseDNS no' >> /etc/ssh/sshd_config
