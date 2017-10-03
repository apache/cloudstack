#!/bin/bash

echo "Preparing to package Cloudian Connector"

RPMDIR="$PWD/rpmbuild"
PKG="cloudian-cloudstack"

VERSION=`(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version) | grep --color=none '^[0-9]\.'`
if echo $VERSION | grep -q SNAPSHOT ; then
    REALVER=`echo $VERSION | cut -d '-' -f 1`
    DEFVER="-D_ver $REALVER"
    DEFPRE="-D_prerelease 1"
    DEFREL="-D_rel SNAPSHOT"
else
    REALVER=`echo $VERSION`
    DEFVER="-D_ver $REALVER"
    DEFREL="-D_rel 1"
fi

rm -fr $RPMDIR
mkdir -p "$RPMDIR/SPECS"
mkdir -p "$RPMDIR/BUILD"
mkdir -p "$RPMDIR/RPMS"
mkdir -p "$RPMDIR/SRPMS"
mkdir -p "$RPMDIR/SOURCES/$PKG"

echo ". preparing source tarball"
(tar -c --exclude .git --exclude target --exclude *iml --exclude rpmbuild . | tar -C "$RPMDIR/SOURCES/$PKG" -x )
(cd "$RPMDIR/SOURCES/$PKG"; tar -czf "../$PKG.tgz" ".")

echo ". executing rpmbuild"
cp "cloudian.spec" "$RPMDIR/SPECS"

(cd "$RPMDIR"; rpmbuild --define "_topdir $RPMDIR" "${DEFVER}" "${DEFREL}" ${DEFPRE+"$DEFPRE"} ${DEFOSSNOSS+"$DEFOSSNOSS"} ${DEFSIM+"$DEFSIM"} -bb SPECS/cloudian.spec)
if [ $? -ne 0 ]; then
    echo "RPM Build Failed "
    exit 3
else
    echo "RPM Build Done"
    mv rpmbuild/RPMS/noarch/$PKG*rpm .
fi
exit
