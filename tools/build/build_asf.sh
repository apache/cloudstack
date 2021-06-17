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
outputdir=/tmp/cloudstack-build/
branch='main' # DH(20140604): maybe change default to `git symbolic-ref --short HEAD`
tag='no'
certid='X'
committosvn='X'

usage(){
    echo "usage: $0 -v version [-b branch] [-s source dir] [-o output dir] [-t] [-u] [-c] [-h]"
    echo "  -v sets the version"
    echo "  -b sets the branch (defaults to 'main')"
    echo "  -s sets the source directory (defaults to $sourcedir)"
    echo "  -o sets the output directory (defaults to $outputdir)"
    echo "  -t tags the git repo with the version"
    echo "  -u sets the certificate ID to sign with (if not provided, the default key is attempted)"
    echo "  -c commits build artifacts to cloudstack dev dist dir in svn"
    echo "  -h"
}

while getopts v:s:o:b:u:tch opt
do
    case "$opt" in
      v)  version="$OPTARG";;
      s)  sourcedir="$OPTARG";;
      o)  outputdir="$OPTARG";;
      b)  branch="$OPTARG";;
      t)  tag="yes";;
      u)  certid="$OPTARG";;
      c)  committosvn="yes";;
      h)  usage
          exit 0;;
      /?)       # unknown flag
          usage
          exit 1;;
    esac
done
shift `expr $OPTIND - 1`

if [ $version == "TESTBUILD" ]; then
    echo >&2 "A version must be specified with the -v option: build_asf.sh -v 4.0.0.RC1"
    exit 1
fi

echo "Using version: $version"
echo "Using source directory: $sourcedir"
echo "Using output directory: $outputdir"
echo "Using branch: $branch"
if [ "$tag" == "yes" ]; then
    if [ "$certid" == "X" ]; then
        echo "Tagging the branch with the version number, and signing the branch with your default certificate."
    else
        echo "Tagging the branch with the version number, and signing the branch with certificate ID $certid."
    fi
else
    echo "The branch will not be tagged.  You should consider doing this."
fi

if [ -d "$outputdir" ]; then
    rm -r $outputdir/*
else
    mkdir $outputdir
fi

cd $sourcedir

echo 'checking out correct branch'
git checkout $branch

echo 'determining current mvn version'
export currentversion=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\['`
echo "found $currentversion"

echo 'setting version numbers'

mvn versions:set -DnewVersion=$version -P developer,systemvm -Dnoredist -Dsimulator
mvn versions:set -DnewVersion=$version -pl tools/checkstyle
mv deps/XenServerJava/pom.xml.versionsBackup deps/XenServerJava/pom.xml
perl -pi -e "s/<cs.xapi.version>6.2.0-1-SNAPSHOT<\/cs.xapi.version>/<cs.xapi.version>6.2.0-1<\/cs.xapi.version>/" pom.xml
perl -pi -e "s/-SNAPSHOT//" deps/XenServerJava/pom.xml
perl -pi -e "s/-SNAPSHOT//" tools/apidoc/pom.xml
perl -pi -e "s/-SNAPSHOT//" build/replace.properties
perl -pi -e "s/-SNAPSHOT//" tools/marvin/setup.py
perl -pi -e "s/-SNAPSHOT//" tools/marvin/marvin/deployAndRun.py
perl -pi -e "s/-SNAPSHOT//" tools/docker/Dockerfile
perl -pi -e "s/-SNAPSHOT//" tools/docker/Dockerfile.marvin
perl -pi -e "s/-SNAPSHOT//" tools/docker/Dockerfile.centos6

case "$currentversion" in 
  *-SNAPSHOT*)
    perl -pi -e 's/-SNAPSHOT//' debian/rules
    ;;
esac

# set debian changelog entry
tmpfilenm=$$.tmp
echo "cloudstack ($version) unstable; urgency=low" >>$tmpfilenm 
echo >>$tmpfilenm
echo "  * Update the version to $version" >>$tmpfilenm
echo >>$tmpfilenm
echo " -- the Apache CloudStack project <dev@cloudstack.apache.org>  `date '+%a, %d %b %Y %T %z'`" >>$tmpfilenm
echo >>$tmpfilenm

cat debian/changelog >>$tmpfilenm
mv $tmpfilenm debian/changelog

git clean -f

#create a RC branch
RC_BRANCH_SUFFIX="RC"`date +%Y%m%dT%H%M`
BRANCHNAME=$version-$RC_BRANCH_SUFFIX
git branch $BRANCHNAME
git checkout $BRANCHNAME


echo 'commit changes'
git commit -a -s -m "Updating pom.xml version numbers for release $version"
export commitsh=`git show HEAD | head -n 1 | cut -d ' ' -f 2`

echo "committed as $commitsh"

echo 'archiving'
git archive --format=tar --prefix=apache-cloudstack-$version-src/ $BRANCHNAME > $outputdir/apache-cloudstack-$version-src.tar
bzip2 $outputdir/apache-cloudstack-$version-src.tar

cd $outputdir
echo 'armor'
if [ "$certid" == "X" ]; then
  gpg -v --armor --output apache-cloudstack-$version-src.tar.bz2.asc --detach-sig apache-cloudstack-$version-src.tar.bz2
else
  gpg -v --default-key $certid --armor --output apache-cloudstack-$version-src.tar.bz2.asc --detach-sig apache-cloudstack-$version-src.tar.bz2
fi

echo 'sha512'
sha512sum apache-cloudstack-$version-src.tar.bz2 > apache-cloudstack-$version-src.tar.bz2.sha512

echo 'verify'
gpg -v --verify apache-cloudstack-$version-src.tar.bz2.asc apache-cloudstack-$version-src.tar.bz2

if [ "$tag" == "yes" ]; then
  echo 'tag'
  cd $sourcedir
  if [ "$certid" == "X" ]; then
      git tag -s $version -m "Tagging release $version on branch $branch."
  else
      git tag -u $certid -s $version -m "Tagging release $version on branch $branch."
  fi
fi

if [ "$committosvn" == "yes" ]; then
  echo 'committing artifacts to svn'
  rm -Rf /tmp/cloudstack-dev-dist
  cd /tmp
  svn co https://dist.apache.org/repos/dist/dev/cloudstack/ cloudstack-dev-dist
  cd cloudstack-dev-dist
  if [ -d "$version" ]; then
    cd $version
    svn rm *
  else
    mkdir $version
    svn add $version
    cd $version
  fi
  cp $outputdir/apache-cloudstack-$version-src.tar.bz2 .
  cp $outputdir/apache-cloudstack-$version-src.tar.bz2.asc .
  cp $outputdir/apache-cloudstack-$version-src.tar.bz2.sha512 .
  svn add apache-cloudstack-$version-src.tar.bz2
  svn add apache-cloudstack-$version-src.tar.bz2.asc
  svn add apache-cloudstack-$version-src.tar.bz2.sha512
  svn commit -m "Committing release candidate artifacts for $version to dist/dev/cloudstack in preparation for release vote"
fi

echo "completed.  use commit-sh of $commitsh when starting the VOTE thread"
