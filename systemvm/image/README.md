# Introduction

This is used to build SystemVM image for use with CloudStack.

# Setting up Tools and Environment

Install debootstrap, qemu-utils (qemu-img, qemu-nbd) and libguestfs-tools:

    apt-get install debootstrap debian-keyring debian-archive-keyring qemu-utils libguestfs-tools apt-cacher-ng

Or, on CentOS distros with `epel-release` enabled:

    yum install epel-release centos-release-qemu-ev
    yum install debootstrap qemu-img-ev libguestfs-tools apt-cacher-ng

# How to build images

Just run build.sh as sudoer user or root, it will create a image.qcow2 systemvm
template in current directory: (roughly takes 5mins)

    sudo bash -x build.sh

To export the template to KVM, VMware and XenServer, run: (roughly takes 5mins)

    sudo bash -x export.sh image.qcow2

Note: if building in docker container, because of NBD this may need privileged
container options:

    docker run -it --privileged --cap-add=ALL -v /dev:/dev <your container>
