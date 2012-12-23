#! /bin/bash
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

# Load RVM into a shell session *as a function*
if [[ -s "$HOME/.rvm/scripts/rvm" ]] ; then
# First try to load from a user install
  source "$HOME/.rvm/scripts/rvm"
elif [[ -s "/usr/local/rvm/scripts/rvm" ]] ; then
# Then try to load from a root install
  source "/usr/local/rvm/scripts/rvm"
else
  printf "ERROR: An RVM installation was not found.\n"
fi

BASEDIR=$PWD/boxes
DEVCLOUD_BASEBUILD_DIR=$BASEDIR/basebox-build
echo $DEVCLOUD_BASEBUILD_DIR
DEVCLOUD_XEN_BASEBUILD_DIR=$BASEDIR/xenbox-build
DEVCLOUD_BASE_NAME='devcloudbase'
DEVCLOUD_XEN_BASE_NAME='devcloudbase-xen'
OS='ubuntu-12.04.1-server-i386'


basebox()  {
        set +x
            rvm rvmrc trust $DEVCLOUD_BASEBUILD_DIR/
    case "$1" in
        build)
            cd $DEVCLOUD_BASEBUILD_DIR/
            set -ex
            vagrant basebox define $DEVCLOUD_BASE_NAME $OS
            cp definition.rb postinstall.sh preseed.cfg definitions/$DEVCLOUD_BASE_NAME/
            vagrant basebox build $DEVCLOUD_BASE_NAME -f -a -n -r
            vagrant basebox export $DEVCLOUD_BASE_NAME -f
            set +ex
            cd $DEVCLOUD_XEN_BASEBUILD_DIR
            set -ex
            vagrant box add $DEVCLOUD_BASE_NAME $DEVCLOUD_BASEBUILD_DIR/${DEVCLOUD_BASE_NAME}.box -f
            ;;
        clean)
            cd $DEVCLOUD_BASEBUILD_DIR/
            set -x
            rm -f iso/*.iso
            vagrant -f basebox destroy $DEVCLOUD_BASE_NAME #-f
            vagrant basebox undefine $DEVCLOUD_BASE_NAME
            #hackery to inherit the proper rvmrc for the hacked vagrant
                        set +x
                cd $BAS$DEVCLOUD_XEN_BASEBUILD_DIR
                set -x
            vagrant -f box remove $DEVCLOUD_BASE_NAME virtualbox
            set +x
            cd $DEVCLOUD_BASEBUILD_DIR
            set -x
            rm -f ${DEVCLOUD_BASE_NAME}.box
            set +x
            cd $BASEDIR
            #rvm --force gemset delete vagrant-release-cloudstack
            ;;
    esac
}

xenbox() {

   set +x
    rvm rvmrc trust $DEVCLOUD_XEN_BASEBUILD_DIR/
    case "$1" in
        build)
            cd $DEVCLOUD_XEN_BASEBUILD_DIR

            #adding it here because it needs to be added into the $VAGRANT_HOME of
            #the hacked vagrant
            set -ex
            vagrant up
            vagrant halt
            vagrant package default --output ${DEVCLOUD_XEN_BASE_NAME}.box
            vagrant box add $DEVCLOUD_XEN_BASE_NAME ${DEVCLOUD_XEN_BASE_NAME}.box -f
            ;;
        clean)
            cd $DEVCLOUD_XEN_BASEBUILD_DIR
            set -x
            vagrant -f box remove $DEVCLOUD_XEN_BASE_NAME virtualbox
            vagrant  destroy -f
            rm -f ${DEVCLOUD_XEN_BASE_NAME}.box
            set +x
            #rvm --force gemset delete vagrant-xen-cloudstack
            set -x
            ;;
    esac
}
usage() {

cat <<EOF
$(basename $0) [-h] [-b] [c] (basebox|xenbox) | all
Builds / cleans boxes used to build the devcloud box

where:
    -h  show this help text
    -b  builds the box(es)
    -c  cleans the box(es)

EOF
}

while getopts 'hbc' option; do
  case "$option" in
    h) usage
       exit
       ;;
    b) action="build"
       ;;
    c) action="clean"
       ;;
    ?) printf "illegal option: '%s'\n" "$OPTARG" >&2
       echo "$usage" >&2
       exit 1
       ;;
  esac
done
shift $((OPTIND - 1))

posargs=$@

#removes duplicate positionals

posargs=$(echo "$posargs" | tr ' ' '\n' | nl | sort -u -k2 | sort -n | cut -f2-)



for arg in $posargs; do

    case "$arg" in
        basebox)
            true
            ;;
        xenbox)
            true
            ;;
        all)
            true
            ;;
        *)
            usage
            exit 1
            ;;
    esac

done

cd $BASEDIR

for arg in $posargs; do
    case "$1" in
        "all")
            case "$action" in
                clean)
                    xenbox $action
                    basebox $action
                    ;;
                build)
                    basebox $action
                    xenbox $action
                    ;;
            esac
            ;;
        $arg)
            $arg $action
            ;;
        esac
done
