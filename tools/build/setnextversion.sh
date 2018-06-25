#!/bin/sh
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

version='TESTBUILD'
sourcedir=~/cloudstack/
branch='master'

usage(){
    echo "usage: $0 -v version [-b branch] [-s source dir] [-h]"
    echo "  -v sets the version"
    echo "  -b sets the branch (defaults to 'master')"
    echo "  -s sets the source directory (defaults to $sourcedir)"
    echo "  -h"
}

while getopts v:s:b:h opt
do
    case "$opt" in
      v)  version="$OPTARG";;
      s)  sourcedir="$OPTARG";;
      b)  branch="$OPTARG";;
      h)  usage
          exit 0;;
      /?)       # unknown flag
          usage
          exit 1;;
    esac
done
shift `expr $OPTIND - 1`

if [ $version == 'TESTBUILD' ]; then
    echo >&2 "A version must be specified with the -v option: $0 -v 4.0.0.RC1"
    exit 1
fi

echo "Using version: $version"
echo "Using source directory: $sourcedir"
echo "Using branch: $branch"

cd $sourcedir

echo 'checking out correct branch'
git checkout $branch

echo 'determining current mvn version'
export currentversion=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\['`
echo "found $currentversion"

echo 'setting version numbers'
mvn versions:set -DnewVersion=$version -P vmware -P developer -P systemvm -P simulator -P baremetal -P ucs -Dnoredist
mv deps/XenServerJava/pom.xml.versionsBackup deps/XenServerJava/pom.xml
perl -pi -e "s/$currentversion/$version/" deps/XenServerJava/pom.xml
perl -pi -e "s/$currentversion/$version/" tools/apidoc/pom.xml
perl -pi -e "s/$currentversion/$version/" debian/changelog
perl -pi -e "s/$currentversion/$version/" tools/marvin/setup.py
perl -pi -e "s/$currentversion/$version/" services/iam/plugin/pom.xml
perl -pi -e "s/$currentversion/$version/" services/iam/pom.xm
perl -pi -e "s/$currentversion/$version/" services/iam/server/pom.xml
perl -pi -e "s/$currentversion/$version/" tools/checkstyle/pom.xml
perl -pi -e "s/$currentversion/$version/" services/console-proxy/plugin/pom.xml
# Dockerfiles
perl -pi -e "s/Version=\"$currentversion\"/Version=\"$version\"/" tools/docker/Dockerfile
perl -pi -e "s/Version=\"$currentversion\"/Version=\"$version\"/" tools/docker/Dockerfile.marvin
# centos6 based dockerfile
perl -pi -e "s/Version=\"$currentversion\"/Version=\"$version\"/" tools/docker/Dockerfile.centos6
perl -pi -e "s/cloudstack-common-(.*).el6.x86_64.rpm/cloudstack-common-${version}.el6.x86_64.rpm/" tools/docker/Dockerfile.centos6
perl -pi -e "s/cloudstack-management-(.*)el6.x86_64.rpm/cloudstack-management-${version}.el6.x86_64.rpm/" tools/docker/Dockerfile.centos6
perl -pi -e "s/Marvin-(.*).tar.gz/Marvin-${version}.tar.gz/" tools/docker/Dockerfile.marvin
# systemtpl.sh:  system vm template version without -SNAPSHOT

git clean -f

echo 'commit changes'
git commit -a -s -m "Updating pom.xml version numbers for release $version"
export commitsh=`git show HEAD | head -n 1 | cut -d ' ' -f 2`

echo "committed as $commitsh"
