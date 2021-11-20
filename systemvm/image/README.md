# Introduction

This is used to build SystemVM image for use with CloudStack under 7minutes
using debootstrap and qemu-nbd.

# Setting up Tools and Environment

Install debootstrap, qemu-utils (qemu-img, qemu-nbd) and libguestfs-tools:

    apt-get install debootstrap debian-keyring debian-archive-keyring qemu-utils libguestfs-tools apt-cacher-ng

Or, on CentOS distros with `epel-release` enabled:

    yum install epel-release centos-release-qemu-ev
    yum install debootstrap qemu-img-ev libguestfs-tools apt-cacher-ng

# How to build images

Just run build.sh as sudoer user or root, it will create KVM systemvm template
`systemvmtemplate-kvm.qcow2` in the current directory:

    sudo bash -x build.sh

To explicitly create images for XenServer (`systemvmtemplate-xen.vhd`) and VMware
(`systemvmtemplate-vmware.ova`) run:

    bash -x export.sh

Note: when building in docker container, because of NBD this may need privileged
container options:

    docker run -it --privileged --cap-add=ALL -v /dev:/dev <your container>
