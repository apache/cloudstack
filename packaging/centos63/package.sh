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
mkdir -p $RPMDIR/SOURCES/cloudstack-$VERSION


(cd ../../; tar -c --exclude .git --exclude dist  .  | tar -C $RPMDIR/SOURCES/cloudstack-$VERSION -x )
(cd $RPMDIR/SOURCES/; tar -czf cloudstack-$VERSION.tgz cloudstack-$VERSION)

cp cloud.spec $RPMDIR/SPECS

(cd $RPMDIR; rpmbuild -ba SPECS/cloud.spec "-D_topdir $RPMDIR" "$DEFVER" "$DEFREL" "$DEFPRE" )
