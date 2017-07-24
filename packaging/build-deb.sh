#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

#set -e

#
# This script builds Debian packages for CloudStack and does
# so by altering the debian/changelog file and add the Ubuntu
# version as a suffix, for example: 4.9.0~xenial
#
# To build packages for Ubuntu 14.04 run this script on a
# 14.04 system. The same goes for Ubuntu 16.04
#
# The biggest difference between those two versions is the
# sysvinit vs systemd and Java 7 vs Java 8
#
# These packages can be build using Docker for example:
#
# Assume that the cloudstack source is present in /tmp/cloudstack
#
# Ubuntu 16.04
# docker run -ti -v /tmp:/src ubuntu:16.04 /bin/bash -c "apt-get update && apt-get install -y dpkg-dev python debhelper openjdk-8-jdk genisoimage python-mysql.connector maven lsb-release devscripts dh-systemd python-setuptools && /src/cloudstack/packaging/build-deb.sh"
#
# Ubuntu 14.04
# docker run -ti -v /tmp:/src ubuntu:14.04 /bin/bash -c "apt-get update && apt-get install -y dpkg-dev python debhelper openjdk-8-jdk genisoimage python-mysql.connector maven lsb-release devscripts dh-systemd python-setuptools && /src/cloudstack/packaging/build-deb.sh"
#

cd `dirname $0`
cd ..

DCH=$(which dch)
if [ -z "$DCH" ] ; then
    echo -e "dch not found, please install devscripts at first. \nDEB Build Failed"
    exit
fi

VERSION=$(head -n1 debian/changelog  |awk -F [\(\)] '{print $2}')
DISTCODE=$(lsb_release -sc)

/bin/cp debian/changelog /tmp/changelog.orig

dch -b -v "${VERSION}~${DISTCODE}" -u low -m "Apache CloudStack Release ${VERSION}"
sed -i '0,/ UNRELEASED;/s// unstable;/g' debian/changelog

dpkg-checkbuilddeps
dpkg-buildpackage -uc -us -b

/bin/mv /tmp/changelog.orig debian/changelog
