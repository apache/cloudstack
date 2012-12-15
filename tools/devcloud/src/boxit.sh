#! /bin/bash -ex

vagrant halt
vagrant package default --output devcloud.box
vagrant box add devcloud devcloud.box -f
