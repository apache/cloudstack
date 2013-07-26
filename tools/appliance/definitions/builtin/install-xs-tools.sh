# get the latest xs tools available from xen.org
wget --no-check-certificate http://downloads.xen.org/XCP/debian/xs-tools-5.9.960.iso -o xs-tools.iso

sudo mount -o loop xs-tools.iso /mnt

#install the xs tools
sudo sh /mnt/Linux/install.sh
