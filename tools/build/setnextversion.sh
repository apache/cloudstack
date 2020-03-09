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

usage() {
    cat << USAGE
Usage: setnextversion.sh --version string [OPTIONS]...
Set the next version of CloudStack in the POMs.

Mandatory arguments:
   -v, --version string                    Set the next version to be applied

Optional arguments:
   -b, --branch string                     Set the branch to update the version into (default "master")
   -s, --sourcedir string                  Set the source directory to clone repo into (default "$sourcedir")
   -n, --no-commit                         Apply only the version change and don't git commit them (default "false")

Other arguments:
   -h, --help                              Display this help message and exit

Examples:
   setnextversion.sh --version x.y.z.a-SNAPSHOT
   setnextversion.sh --version x.y.z.a-SNAPSHOT --branch foo-feature
   setnextversion.sh --version x.y.z.a-SNAPSHOT --sourcedir /path/to/cloudstack/repo
   setnextversion.sh --version x.y.z.a-SNAPSHOT --no-commit

USAGE
    exit 0
}

while [ -n "$1" ]; do
    case "$1" in
        -h | --help)
            usage
            ;;

        -v | --version)
            if [ -n "$version" ]; then
                echo "ERROR: you have already entered value for -v, --version"
                exit 1
            else
                version=$2
                shift 2
            fi
            ;;

        -b | --branch)
            if [ -n "$branch" ]; then
                echo "ERROR: you have already entered value for -b, --branch"
                exit 1
            else
                branch=$2
                shift 2
            fi
            ;;

        -s | --sourcedir)
            if [ -n "$sourcedir" ]; then
                echo "ERROR: you have already entered value for -s, --sourcedir"
                exit 1
            else
                sourcedir=$2
                shift 2
            fi
            ;;

        -n | --no-commit)
            if [ "$nocommit" == "true" ]; then
                echo "ERROR: you have already entered value for -n, --no-commit"
                exit 1
            else
                nocommit="true"
                shift 1
            fi
            ;;

        -*|*)
            echo "ERROR: no such option $1. -h or --help for help"
            exit 1
            ;;
    esac
done

if [ -z "$version" ]; then
    echo >&2 "A version must be specified with the -v, --version option: $0 -v 4.0.0.RC1"
    exit 1
fi

if [ -z "$branch" ]; then
    branch="master"
fi

if [ -z "$sourcedir" ]; then
    sourcedir="~/cloudstack/"
fi

if [ -z "$nocommit" ]; then
    nocommit="false"
fi

echo "Using version          : $version"
echo "Using source directory : $sourcedir"
echo "Using branch           : $branch"

cd $sourcedir

echo "checking out correct branch"
git checkout $branch

echo "determining current POM version"
export currentversion=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version -B | grep -v '\['`
echo "found $currentversion"

echo "setting new version numbers"
mvn versions:set -DnewVersion=$version -P vmware -P developer -P systemvm -P simulator -Dnoredist versions:commit

perl -pi -e "s/$currentversion/$version/" debian/changelog
perl -pi -e "s/$currentversion/$version/" tools/apidoc/pom.xml
perl -pi -e "s/$currentversion/$version/" tools/checkstyle/pom.xml
perl -pi -e "s/$currentversion/$version/" tools/marvin/setup.py

# Dockerfiles
perl -pi -e "s/Version=\"$currentversion\"/Version=\"$version\"/" tools/docker/Dockerfile

# Marvin Dockerfiles
perl -pi -e "s/Version=\"$currentversion\"/Version=\"$version\"/" tools/docker/Dockerfile.marvin
perl -pi -e "s/Marvin-(.*).tar.gz/Marvin-${version}.tar.gz/" tools/docker/Dockerfile.marvin

# systemtpl.sh:  system vm template version without -SNAPSHOT

git clean -f

if [ "$nocommit" == "false" ]; then
    echo "commit changes"
    git commit -a -s -m "Updating pom.xml version numbers for release $version"
    export commitsh=`git show HEAD | head -n 1 | cut -d ' ' -f 2`

    echo "committed as $commitsh"
fi
