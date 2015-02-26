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

function usage() {
    echo ""
    echo "usage: ./package.sh [-p|--pack] [-h|--help] [ARGS]"
    echo ""
    echo "The commonly used Arguments are:"
    echo "-p|--pack oss|OSS             To package with only redistributable libraries (default)"
    echo "-p|--pack noredist|NOREDIST   To package with non-redistributable libraries"
    echo "-d centos7|centos63|fedora20|fedora21  To build a package for a distribution"
    echo "-s simulator|SIMULATOR        To build for Simulator"
    echo ""
    echo "Examples: ./package.sh -p|--pack oss|OSS"
    echo "          ./package.sh -p|--pack noredist|NOREDIST"
    echo "          ./package.sh (Default OSS)"
    exit 1
}

# packaging
#   $1 redist flag
#   $2 simulator flag
#   $3 distribution name
function packaging() {
    CWD=`pwd`
    RPMDIR=$CWD/../dist/rpmbuild
    PACK_PROJECT=cloudstack
    if [ -n "$1" ] ; then
        DEFOSSNOSS="-D_ossnoss $1"
    fi
    if [ -n "$2" ] ; then
        DEFSIM="-D_sim $2"
    fi

    DISTRO=$3
    MVN=`which mvn`
    if [ -z "$MVN" ] ; then
        MVN=`locate bin/mvn | grep -e mvn$ | tail -1`
        if [ -z "$MVN" ] ; then
            echo "mvn not found\n cannot retrieve version to package\n RPM Build Failed"
            exit 2
        fi
    fi
    VERSION=`(cd ../; $MVN org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version) | grep --color=none '^[0-9]\.'`
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

    echo Preparing to package Apache CloudStack ${VERSION}

    mkdir -p $RPMDIR/SPECS
    mkdir -p $RPMDIR/BUILD
    mkdir -p $RPMDIR/RPMS
    mkdir -p $RPMDIR/SRPMS
    mkdir -p $RPMDIR/SOURCES/$PACK_PROJECT-$VERSION

    echo ". preparing source tarball"
    (cd ../; tar -c --exclude .git --exclude dist  .  | tar -C $RPMDIR/SOURCES/$PACK_PROJECT-$VERSION -x )
    (cd $RPMDIR/SOURCES/; tar -czf $PACK_PROJECT-$VERSION.tgz $PACK_PROJECT-$VERSION)

    echo ". executing rpmbuild"
    cp $DISTRO/cloud.spec $RPMDIR/SPECS

    (cd $RPMDIR; rpmbuild --define "_topdir $RPMDIR" "${DEFVER}" "${DEFREL}" ${DEFPRE+"${DEFPRE}"} ${DEFOSSNOSS+"$DEFOSSNOSS"} ${DEFSIM+"$DEFSIM"} -bb SPECS/cloud.spec)

    if [ $? -ne 0 ]; then
        echo "RPM Build Failed "
        exit 1
    else
        echo "RPM Build Done"
    fi
    exit

}


TARGETDISTRO=""
sim=""
packageval=""

    SHORTOPTS="hp:d:"
    LONGOPTS="help,pack:,simulator:distribution"
    ARGS=$(getopt -s bash -u -a --options $SHORTOPTS  --longoptions $LONGOPTS --name $0 -- "$@")
    eval set -- "$ARGS"
    echo "$ARGS"
    while [ $# -gt 0 ] ; do
        case "$1" in
            -h | --help)
            usage
            exit 0
            ;;
        -p | --pack)
            echo "Doing CloudStack Packaging ....."
            packageval=$2
            echo "$packageval"
            if [ "$packageval" == "oss" -o "$packageval" == "OSS" ] ; then
                packageval=""
            elif [ "$packageval" == "noredist" -o "$packageval" == "NOREDIST" ] ; then
                packageval="noredist"
            else
                echo "Error: Incorrect value provided in package.sh script, Please see help ./package.sh --help|-h for more details."
                exit 1
            fi
            shift
            ;;
        -s | --simulator)
            sim=$2
            echo "$sim"
            if [ "$sim" == "default" -o "$sim" == "DEFAULT" ] ; then
                sim = "false"
            elif [ "$sim" == "simulator" -o "$sim" == "SIMULATOR" ] ; then
                sim="simulator"
            else
                echo "Error: Incorrect value provided in package.sh script for -o, Please see help ./package.sh --help|-h for more details."
		exit 1
            fi
            shift
            ;;
        -d | --distribution)
            TARGETDISTRO=$2
            shift
            ;;
        -)
            echo "Unrecognized option..."
            usage
            exit 1
            ;;
        *)
            shift
            ;;
        esac
    done

    if [ -z "$TARGETDISTRO" ]
    then
        echo "Missing target distribution"
        usage
        exit 1
    fi

    packaging "$packageval" "$sim" "$TARGETDISTRO"
