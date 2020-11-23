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

#
# This script builds Debian packages for CloudStack and does
# so by altering the debian/changelog file and add the Ubuntu
# version as a suffix, for example: 4.10.0~xenial
#
# These packages can be build using Docker for example:
#
# Assume that the cloudstack source is present in /tmp/cloudstack
#
# Ubuntu 16.04
# docker run -ti -v /tmp:/src ubuntu:16.04 /bin/bash -c "apt-get update && apt-get install -y dpkg-dev python debhelper openjdk-8-jdk genisoimage python-mysql.connector maven lsb-release devscripts dh-systemd python-setuptools && /src/cloudstack/packaging/build-deb.sh"
#

function usage() {
    cat << USAGE
Usage: ./build-deb.sh [OPTIONS]...
Package CloudStack for Debian based distribution.

If there's a "branding" string in the POM version (e.g. x.y.z.a-NAME[-SNAPSHOT]), the branding name will
be used in the final generated package like: cloudstack-management_x.y.z.a-NAME-SNAPSHOT~xenial_all.deb
note that you can override/provide "branding" string with "-b, --brand" flag as well.

Optional arguments:
   -b, --brand string                      Set branding to be used in package name (it will override any branding string in POM version)
   -T, --use-timestamp                     Use epoch timestamp instead of SNAPSHOT in the package name (if not provided, use "SNAPSHOT")
   -o, --output-directory                  The output directory of packages

Other arguments:
   -h, --help                              Display this help message and exit

Examples:
   build-deb.sh --use-timestamp
   build-deb.sh --brand foo

USAGE
    exit 0
}

BRANDING=""
USE_TIMESTAMP="false"

while [ -n "$1" ]; do
    case "$1" in
        -h | --help)
            usage
            ;;

        -b | --brand)
            if [ -n "$BRANDING" ]; then
                echo "ERROR: you have already entered value for -b, --brand"
                exit 1
            else
                BRANDING=$2
                shift 2
            fi
            ;;

        -T | --use-timestamp)
            if [ "$USE_TIMESTAMP" == "true" ]; then
                echo "ERROR: you have already entered value for -T, --use-timestamp"
                exit 1
            else
                USE_TIMESTAMP="true"
                shift 1
            fi
            ;;

        -o | --output-directory)
            if [ -n "$OUTPUT_DIR" ]; then
                echo "ERROR: you have already entered value for -o, --output-directory"
                exit 1
            else
                OUTPUT_DIR=$2
                shift 2
            fi
            ;;

        -*|*)
            echo "ERROR: no such option $1. -h or --help for help"
            exit 1
            ;;
    esac
done

if [ -z "$(which dch)" ] ; then
    echo -e "dch not found, please install devscripts at first. \nDEB Build Failed"
    exit 1
fi

NOW="$(date +'%Y%m%dT%H%M%S')"
PWD=$(cd $(dirname "$0") && pwd -P)
cd $PWD/../

# Fail early if working directory is NOT clean and --use-timestamp was provided
if [ "$USE_TIMESTAMP" == "true" ]; then
    if [ -n "$(cd $PWD; git status -s)" ]; then
        echo "Erro: You have uncommitted changes and asked for --use-timestamp to be used."
        echo "      --use-timestamp flag is going to temporarily change  POM versions  and"
        echo "      revert them at the end of build, and there's no  way we can do partial"
        echo "      revert. Please commit your changes first or omit --use-timestamp flag."
        exit 1
    fi
fi

VERSION=$(head -n1 debian/changelog  |awk -F [\(\)] '{print $2}')
DISTCODE=$(lsb_release -sc)

if [ "$USE_TIMESTAMP" == "true" ]; then
    # use timestamp instead of SNAPSHOT
    if echo "$VERSION" | grep -q SNAPSHOT ; then
        # apply/override branding, if provided
        if [ "$BRANDING" != "" ]; then
            VERSION=$(echo "$VERSION" | cut -d '-' -f 1) # remove any existing branding from POM version to be overriden
            VERSION="$VERSION-$BRANDING-$NOW"
        else
            VERSION=`echo $VERSION | sed 's/-SNAPSHOT/-'$NOW'/g'`
        fi

        branch=$(cd $PWD; git rev-parse --abbrev-ref HEAD)
        (cd $PWD; ./tools/build/setnextversion.sh --version $VERSION --sourcedir . --branch $branch --no-commit)
    fi
else
    # apply/override branding, if provided
    if [ "$BRANDING" != "" ]; then
        VERSION=$(echo "$VERSION" | cut -d '-' -f 1) # remove any existing branding from POM version to be overriden
        VERSION="$VERSION-$BRANDING"

        branch=$(cd $PWD; git rev-parse --abbrev-ref HEAD)
        (cd $PWD; ./tools/build/setnextversion.sh --version $VERSION --sourcedir . --branch $branch --no-commit)
    fi
fi

/bin/cp debian/changelog debian/changelog.$NOW

dch -b -v "${VERSION}~${DISTCODE}" -u low -m "Apache CloudStack Release ${VERSION}"
sed -i '0,/ UNRELEASED;/s// unstable;/g' debian/changelog

dpkg-checkbuilddeps
dpkg-buildpackage -uc -us -b

/bin/mv debian/changelog.$NOW debian/changelog

if [ -n "$OUTPUT_DIR" ];then
    mkdir -p "$OUTPUT_DIR"
    mv ../*${VERSION}* "$OUTPUT_DIR"
    echo "====== CloudStack packages have been moved to $OUTPUT_DIR ======"
fi

if [ "$USE_TIMESTAMP" == "true" ]; then
    (cd $PWD; git reset --hard)
fi
