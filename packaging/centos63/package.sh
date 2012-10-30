#!/bin/bash

CWD=`pwd`
RPMDIR=$CWD/../../dist/rpmbuild



VERSION=`(cd ../../; mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version) | grep -v '^\['`
if echo $VERSION | grep SNAPSHOT ; then
  REALVER=`echo $VERSION | cut -d '-' -f 1`
  DEFVER="-D_ver $REALVER"
  DEFPRE="-D_prerelease 1"
  DEFREL="-D_rel SNAPSHOT"
else
  DEFVER="-D_ver $REALVER"
  DEFPRE=
  DEFREL=
fi

mkdir -p $RPMDIR/SPECS
mkdir -p $RPMDIR/SOURCES/cloud-$VERSION


(cd ../../; tar -c --exclude .git --exclude dist  .  | tar -C $RPMDIR/SOURCES/cloud-$VERSION -x )
(cd $RPMDIR/SOURCES/; tar -czf cloud-$VERSION.tgz cloud-$VERSION)

cp cloud.spec $RPMDIR/SPECS

(cd $RPMDIR; rpmbuild -ba SPECS/cloud.spec "-D_topdir $RPMDIR" "$DEFVER" "$DEFREL" "$DEFPRE" )
