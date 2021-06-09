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

if [ -f /root/cloudstack-mount.sh ];then
    echo "/root/cloudstack-mount.sh already exists"
    exit
fi

cat >/root/cloudstack-mount.sh <<EOF
#/bin/bash
mkdir -p /cloudstack/{opt,usr,etc,var,root}/{data,upper,workdir}

mount -o ro /dev/cdrom /media/cdrom
DEBIANVERSION=\$(cat /etc/debian_version | cut -d '.' -f1)
if [ -f /media/cdrom/cloudstack-patches-debian-\$DEBIANVERSION.tgz ];then
    old_sha512sum=
    if [ -f /cloudstack/root/upper/cloudstack-patches-debian-\$DEBIANVERSION.tgz.sha512sum ];then
        old_sha512sum=\$(cat /cloudstack/root/upper/cloudstack-patches-debian-\$DEBIANVERSION.tgz.sha512sum)
    fi
    if [ -f /media/cdrom/cloudstack-patches-debian-\$DEBIANVERSION.tgz.sha512sum ];then
        new_sha512sum=\$(cat /media/cdrom/cloudstack-patches-debian-\$DEBIANVERSION.tgz.sha512sum)
    fi
    if [ "\$new_sha512sum" != "\$old_sha512sum" ];then
        echo "New sha512sum is \$new_sha512sum, while old sha512sum is \$old_sha512sum, uncompressing cloudstack-patches-debian-\$DEBIANVERSION.tgz ..."
        tar xzf /media/cdrom/cloudstack-patches-debian-\$DEBIANVERSION.tgz -C /cloudstack/
        cp -f /media/cdrom/cloudstack-patches-debian-\$DEBIANVERSION.tgz.sha512sum /cloudstack/root/upper/cloudstack-patches-debian-\$DEBIANVERSION.tgz.sha512sum
        sync && reboot
    fi
fi

touch /cloudstack/root/upper/cloudstack-mount.mounted

mount_overlayfs() {
    if grep -qs "overlay \$1 " /proc/mounts; then
        mount -t overlay -o remount,lowerdir=\$2:\$1,upperdir=\$3,workdir=\$4 overlay \$1
    else
        mount -t overlay -o lowerdir=\$2:\$1,upperdir=\$3,workdir=\$4 overlay \$1
    fi
}

mount_overlayfs /etc /cloudstack/etc/data /cloudstack/etc/upper /cloudstack/etc/workdir
mount_overlayfs /opt /cloudstack/opt/data /cloudstack/opt/upper /cloudstack/opt/workdir
mount_overlayfs /var /cloudstack/var/data /cloudstack/var/upper /cloudstack/var/workdir
mount_overlayfs /usr /cloudstack/usr/data /cloudstack/usr/upper /cloudstack/usr/workdir
mount_overlayfs /root /cloudstack/root/data /cloudstack/root/upper /cloudstack/root/workdir

systemctl daemon-reload
EOF

chmod +x /root/cloudstack-mount.sh

cat >/lib/systemd/system/cloudstack-mount.service <<EOF
[Unit]
Description=Mount overlayfs and unpack cloudstack packages
ConditionPathExists=/root/cloudstack-mount.sh
Before=cloud-early-config.service network-pre.target
Wants=cloud-early-config.service network-pre.target

Requires=local-fs.target
After=local-fs.target

[Install]
WantedBy=multi-user.target

[Service]
Type=oneshot
ExecStart=/bin/bash -c /root/cloudstack-mount.sh
TimeoutSec=0
StandardOutput=tty
EOF

cat >/etc/systemd/system/cloud-early-config.service <<EOF
[Unit]
Description=CloudStack post-boot patching service using cmdline
DefaultDependencies=no

Before=network-pre.target
Wants=network-pre.target

Requires=local-fs.target
After=local-fs.target

[Install]
WantedBy=multi-user.target

[Service]
Type=oneshot
ExecStartPre=bash -c "sleep 1"
ExecStartPre=bash -c "while [ ! -f /root/cloudstack-mount.mounted ]; do sleep 0.1; done"
ExecStart=/opt/cloud/bin/setup/cloud-early-config
RemainAfterExit=true
TimeoutStartSec=5min
EOF

systemctl daemon-reload
systemctl enable cloudstack-mount.service
systemctl enable cloud-early-config.service
