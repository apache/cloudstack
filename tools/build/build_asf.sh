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
while getopts v:s:o:b: opt
do
    case "$opt" in
      v)  version="$OPTARG";;
      s)  sourcedir="$OPTARG";;
      o)  outputdir="$OPTARG";;
      b)  branch="$OPTARG";;
      \?)       # unknown flag
          echo >&2 \
      "usage: $0 [-v version number] [-s source directory (defaults to $sourcedir)] [-o output directory (defaults to $outputdir)]"
      exit 1;;
    esac
done
shift `expr $OPTIND - 1`

echo $version
echo $sourcedir
echo $outputdir

if [ -d "$outputdir" ]; then
    rm $outputdir/*
else
    mkdir $outputdir
fi

cp $sourcedir/KEYS $outputdir/KEYS

cd $sourcedir
git archive --format=tar.gz --prefix=$version/ $branch > $outputdir/cloudstack-source-$version.tar.gz
git archive --format=zip --prefix=$version/ $branch > $outputdir/cloudstack-source-$version.zip

cd $outputdir
gpg -v --armor --output cloudstack-source-$version.tar.gz.asc --detach-sig cloudstack-source-$version.tar.gz
gpg -v --armor --output cloudstack-source-$version.zip.asc --detach-sig cloudstack-source-$version.zip
gpg -v --print-md MD5 cloudstack-source-$version.tar.gz > cloudstack-source-$version.tar.gz.md5
gpg -v --print-md MD5 cloudstack-source-$version.zip > cloudstack-source-$version.zip.md5
gpg -v --print-md SHA512 cloudstack-source-$version.tar.gz > cloudstack-source-$version.tar.gz.sha
gpg -v --print-md SHA512 cloudstack-source-$version.zip > cloudstack-source-$version.zip.sha

gpg -v --verify cloudstack-source-$version.tar.gz.asc cloudstack-source-$version.tar.gz
gpg -v --verify cloudstack-source-$version.zip.asc cloudstack-source-$version.zip
