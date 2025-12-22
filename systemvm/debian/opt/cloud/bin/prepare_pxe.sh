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

set +u

err_exit() {
    echo $1
    exit 1
}

success() {
    exit 0
}

TFTP_ROOT='/opt/tftpboot'
PXELINUX_CFG_DIR='/opt/tftpboot/pxelinux.cfg'

kernel_nfs_path=$1
kernel_file_name=`basename $kernel_nfs_path`
initrd_nfs_path=$2
initrd_file_name=`basename $initrd_nfs_path`
tmpt_uuid=$3
pxe_cfg_filename=$4
ks_file=$5

kernel_path=$tmpt_uuid/$kernel_file_name
initrd_path=$tmpt_uuid/$initrd_file_name

cat > $PXELINUX_CFG_DIR/$pxe_cfg_filename <<EOF
DEFAULT default
PROMPT 1
TIMEOUT 26
DISPLAY boot.msg
LABEL default
KERNEL $kernel_path
APPEND ramdisk_size=66000 initrd=$initrd_path ks=$ks_file

EOF

tmpt_dir=$TFTP_ROOT/$tmpt_uuid
if [ -d $tmpt_dir ]; then
    success
fi

mkdir -p $tmpt_dir

mnt_path=/tmp/$(uuid)

mkdir -p $mnt_path
mount `dirname $kernel_nfs_path` $mnt_path
cp -f $mnt_path/$kernel_file_name $tmpt_dir/$kernel_file_name
umount $mnt_path

mount `dirname $initrd_nfs_path` $mnt_path
cp -f $mnt_path/$initrd_file_name $tmpt_dir/$initrd_file_name
umount $mnt_path

success
