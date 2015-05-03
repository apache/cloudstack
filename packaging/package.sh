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
    echo "-d centos7|centos6|debian     To build a package for a distribution"
    echo "-s simulator|SIMULATOR        To build for Simulator"
    echo "-j jetty                      To build with jetty (default is tomcat)"
    echo ""
    echo "Examples: ./package.sh -p|--pack oss|OSS"
    echo "          ./package.sh -p|--pack noredist|NOREDIST"
    echo "          ./package.sh (Default OSS)"
    exit 1
}

# checkDeps
# $1 distribution name
function checkDeps(){
    # Check JDK
    if [ "$1" == "debian" ]
    then
    JDK=$(dpkg -l | grep "openjdk-.-jdk")
    for version in $JDK; do [ "${version:8:1}" -ge "7" ] && break; done;
    else 
    JDK=$(rpm -qa | grep "java-1...0-openjdk-devel")
    for version in $JDK; do [ "${version:7:1}" -ge "7" ] && break; done;
    fi
    if [ "$?" -gt "0" ] || [ -z "$JDK" ] ; then
        echo -e "JDK-devel 1.7.0+ not found\nCannot retrieve version to package\nPackage Build Failed"
        exit 2
    fi
    # Check Maven
    MVN=`which mvn`
    if [ -z "$MVN" ] ; then
        MVN=`locate bin/mvn | grep -e mvn$ | tail -1`
        if [ -z "$MVN" ] ; then
            echo -e "mvn not found\ncannot retrieve version to package\nPackage Build Failed"
            exit 2
        fi
    fi
}
# packageRPM/DEB
#   $1 redist flag
#   $2 simulator flag
#   $3 distribution name
#   $4 servlet engine name
function packageDEB() {
    export OSSNOSS $1
    export SIM $2
    export SERVLETENGINE $3
    dpkg-buildpackage -uc -us
    mkdir ../dist
    mv ../*.deb ../dist
    mv ../*.tar.gz ../dist
    mv ../*.changes ../dist
    mv ../*.dsc ../dist
}
function packageRPM() {
    CWD=`pwd`
    RPMDIR=$CWD/../dist/rpmbuild
    PACK_PROJECT=cloudstack
    if [ -n "$1" ] ; then
        DEFOSSNOSS="-D_ossnoss $1"
    fi
    if [ -n "$2" ] ; then
        DEFSIM="-D_sim $2"
    fi
    if [ -n "$4" ] ; then
        SERVLETENGINE="-D_servletengine $4"
    fi

    DISTRO=$3

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
    echo ". cleaning up old dist folder"
    rm -rf $RPMDIR
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

    (cd $RPMDIR; rpmbuild --define "_topdir $RPMDIR" "${DEFVER}" "${DEFREL}" ${DEFPRE+"${DEFPRE}"} ${DEFOSSNOSS+"$DEFOSSNOSS"} ${DEFSIM+"$DEFSIM"} ${SERVLETENGINE+"$SERVLETENGINE"} -bb SPECS/cloud.spec)

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
SERVLETENGINE="catalina"
    
    SHORTOPTS="hp:d:j"
    LONGOPTS="help,pack:,simulator:distribution,jetty"
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
        -j | --jetty)
            SERVLETENGINE=jetty
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
    checkDeps $TARGETDISTRO
    if [ $TARGETDISTRO == "debian" ]
    then
        packageDEB "$packageval" "$sim" "$SERVLETENGINE"
    else
        packageRPM "$packageval" "$sim" "$TARGETDISTRO" "$SERVLETENGINE"
    fi
