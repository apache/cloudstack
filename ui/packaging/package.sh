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

set -e
set -x

ROOT=$PWD

function package_deb() {
    VERSION=$(cd ../; grep --color=none \"version\" package.json | cut -d '"' -f4)
    DATE=$(date +"%Y%m%d")
    sed -i "s/VERSION/$VERSION-$DATE/g" debian/changelog
    dpkg-buildpackage -uc -us
}

function package_rpm() {
    CWD=`pwd`
    RPMDIR=$CWD/../build

    VERSION=$(cd ../; grep --color=none \"version\" package.json | cut -d '"' -f4)
    REALVER=`echo $VERSION`
    DEFVER="-D_ver $REALVER"
    DEFREL="-D_rel $(date +"%Y%m%d")"

    echo Preparing to package CloudStack Primate ${VERSION}

    mkdir -p $RPMDIR/SPECS
    mkdir -p $RPMDIR/BUILD
    mkdir -p $RPMDIR/RPMS
    mkdir -p $RPMDIR/SRPMS
    mkdir -p $RPMDIR/SOURCES/cloudstack

    echo ". preparing source tarball"
    (cd ../; tar -c --exclude .git --exclude build  .  | tar -C $RPMDIR/SOURCES/cloudstack -x )
    (cd $RPMDIR/SOURCES/; tar -czf primate-$VERSION.tgz cloudstack)

    echo ". executing rpmbuild"
    cp centos/primate.spec $RPMDIR/SPECS

    (cd $RPMDIR; rpmbuild --define "_topdir $RPMDIR" "${DEFVER}" "${DEFREL}" ${DEFPRE+"${DEFPRE}"} -bb SPECS/primate.spec)

    if [ $? -ne 0 ]; then
        echo "RPM Build Failed "
        exit 1
    else
        echo "RPM Build Done"
    fi
}

case "$1" in
  deb ) package_deb
      ;;
  rpm ) package_rpm
      ;;
  * )   package_rpm
        package_deb
      ;;
esac
