#!/bin/bash -e
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

echo "Params $#"
if [ $# -lt 1 ]; then
    echo "Invalid input. Valid usage: ./upgrade-kubernetes.sh UPGRADE_VERSION"
    echo "eg: ./upgrade-kubernetes.sh 1.16.3"
    exit 1
fi
UPGRADE_VERSION="${1}"

export PATH=$PATH:/opt/bin

ISO_MOUNT_DIR=/mnt/k8sdisk
BINARIES_DIR=${ISO_MOUNT_DIR}/

OFFLINE_INSTALL_ATTEMPT_SLEEP=5
MAX_OFFLINE_INSTALL_ATTEMPTS=10
offline_attempts=1
while true; do
  if (( "$offline_attempts" > "$MAX_OFFLINE_INSTALL_ATTEMPTS" )); then
    echo "Warning: Offline install timed out!"
    break
  fi
  set +e
  output=`blkid -o device -t TYPE=iso9660`
  set -e
  if [ "$output" != "" ]; then
    while read -r line; do
      if [ ! -d "${ISO_MOUNT_DIR}" ]; then
        mkdir "${ISO_MOUNT_DIR}"
      fi
      retval=0
      set +e
      mount -o ro "${line}" "${ISO_MOUNT_DIR}"
      retval=$?
      set -e
      if [ $retval -eq 0 ]; then
        if [ -d "$BINARIES_DIR" ]; then
          break
        else
          umount "${line}" && rmdir "${ISO_MOUNT_DIR}"
        fi
      fi
    done <<< "$output"
  fi
  if [ -d "$BINARIES_DIR" ]; then
    break
  fi
  echo "Waiting for Binaries directory $BINARIES_DIR to be available, sleeping for $OFFLINE_INSTALL_ATTEMPT_SLEEP seconds, attempt: $offline_attempts"
  sleep $OFFLINE_INSTALL_ATTEMPT_SLEEP
  offline_attempts=$[$offline_attempts + 1]
done

if [ -d "$BINARIES_DIR" ]; then
  ### Binaries available offline ###
  echo "Installing binaries from ${BINARIES_DIR}"

  cd /opt/bin

  cp ${BINARIES_DIR}/k8s/kubeadm /opt/bin
  chmod +x kubeadm

  output=`ls ${BINARIES_DIR}/docker/`
  if [ "$output" != "" ]; then
    while read -r line; do
        docker load < "${BINARIES_DIR}/docker/$line"
    done <<< "$output"
  fi

  tar -f "${BINARIES_DIR}/cni/cni-plugins-amd64.tgz" -C /opt/cni/bin -xz
  tar -f "${BINARIES_DIR}/cri-tools/crictl-linux-amd64.tar.gz" -C /opt/bin -xz

  if [ "${UPGRADE_VERSION}" != '' ]; then
    kubeadm upgrade apply ${UPGRADE_VERSION} -y
  fi

  systemctl stop kubelet
  cp -a ${BINARIES_DIR}/k8s/{kubelet,kubectl} /opt/bin
  chmod +x {kubelet,kubectl}
  systemctl restart kubelet

  if [ "${UPGRADE_VERSION}" != '' ]; then
    kubectl apply -f ${BINARIES_DIR}/network.yaml
    kubectl apply -f ${BINARIES_DIR}/dashboard.yaml
  fi

  umount "${ISO_MOUNT_DIR}" && rmdir "${ISO_MOUNT_DIR}"
fi