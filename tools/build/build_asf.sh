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
sourcedir=~/incubator-cloudstack/
outputdir=~/cs-asf-build/
branch='master'
tag='no'
certid='X'


usage(){
    echo "usage: $0 -v version [-b branch] [-s source dir] [-o output dir] [-t [-u]] [-h]"
    echo "  -v sets the version"
    echo "  -b sets the branch (defaults to 'master')"
    echo "  -s sets the source directory (defaults to $sourcedir)"
    echo "  -o sets the output directory (defaults to $outputdir)"
    echo "  -t tags the git repo with the version"
    echo "     -u sets the certificate ID to sign the tag with (if not provided, the default key is attempted)"
    echo "  -h"
}

while getopts v:s:o:b:tu:h opt
do
    case "$opt" in
      v)  version="$OPTARG";;
      s)  sourcedir="$OPTARG";;
      o)  outputdir="$OPTARG";;
      b)  branch="$OPTARG";;
      t)  tag='yes';;
      u)  certid="$OPTARG";;
      h)  usage
          exit 0;;
      \?)       # unknown flag
          usage
          exit 1;;
    esac
done
shift `expr $OPTIND - 1`

if [ $version == 'TESTBUILD' ]; then
    echo >&2 "A version must be specified with the -v option: build_asf.sh -v 4.0.0.RC1"
    exit 1
fi

echo "Using version: $version"
echo "Using source directory: $sourcedir"
echo "Using output directory: $outputdir"
echo "Using branch: $branch"
if [ $tag == 'yes' ]; then
    if [ $certid == 'X' ]; then
        echo "Tagging the branch with the version number, and signing the branch with your default certificate."
    else
        echo "Tagging the branch with the version number, and signing the branch with certificate ID $certid."
    fi
else
    echo "The branch will not be tagged.  You should consider doing this."
fi

if [ -d "$outputdir" ]; then
    rm $outputdir/*
else
    mkdir $outputdir
fi

cp $sourcedir/KEYS $outputdir/KEYS

cd $sourcedir
git archive --format=tar.gz --prefix=apache-cloudstack-$version-incubating-src/ $branch > $outputdir/apache-cloudstack-$version-incubating-src.tar.gz
git archive --format=zip --prefix=apache-cloudstack-$version-incubating-src/ $branch > $outputdir/apache-cloudstack-$version-incubating-src.zip

cd $outputdir
gpg -v --armor --output apache-cloudstack-$version-incubating-src.tar.gz.asc --detach-sig apache-cloudstack-$version-incubating-src.tar.gz
gpg -v --armor --output apache-cloudstack-$version-incubating-src.zip.asc --detach-sig apache-cloudstack-$version-incubating-src.zip
gpg -v --print-md MD5 apache-cloudstack-$version-incubating-src.tar.gz > apache-cloudstack-$version-incubating-src.tar.gz.md5
gpg -v --print-md MD5 apache-cloudstack-$version-incubating-src.zip > apache-cloudstack-$version-incubating-src.zip.md5
gpg -v --print-md SHA512 apache-cloudstack-$version-incubating-src.tar.gz > apache-cloudstack-$version-incubating-src.tar.gz.sha
gpg -v --print-md SHA512 apache-cloudstack-$version-incubating-src.zip > apache-cloudstack-$version-incubating-src.zip.sha

gpg -v --verify apache-cloudstack-$version.tar.gz.asc apache-cloudstack-$version-incubating-src.tar.gz
gpg -v --verify apache-cloudstack-$version.zip.asc apache-cloudstack-$version-incubating-src.zip

if [ $tag == 'yes' ]; then
  cd $sourcedir
  if [ $certid == 'X' ]; then
      git tag -s $version -m "Tagging release $version on branch $branch."
  else
      git tag -u $certid -s $version -m "Tagging release $version on branch $branch."
  fi
fi
