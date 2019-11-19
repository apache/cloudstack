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


echo "Params $#"
if [ $# -lt 3 ]; then
    echo "Invalid input. Valid usage: ./create-binaries-iso KUBERNETES_VERSION CNI_VERSION CRICTL_VERSION, eg: ./create-binaries-iso 1.11.4 0.7.1 1.11.1"
    exit 1
fi

RELEASE="v${1}"
start_dir="$PWD"
iso_dir="${start_dir}/iso"
working_dir="${iso_dir}/${RELEASE}"
mkdir -p "${working_dir}"

CNI_VERSION="v${2}"
echo "Downloading CNI ${CNI_VERSION}..."
cni_dir="${working_dir}/cni/${CNI_VERSION}"
mkdir -p "${cni_dir}"
curl -L "https://github.com/containernetworking/plugins/releases/download/${CNI_VERSION}/cni-plugins-amd64-${CNI_VERSION}.tgz" -o "${cni_dir}/cni-plugins-amd64-${CNI_VERSION}.tgz"

CRICTL_VERSION="v${3}"
echo "Downloading CRI tools ${CRICTL_VERSION}..."
crictl_dir="${working_dir}/cri-tools/${CRICTL_VERSION}"
mkdir -p "${crictl_dir}"
curl -L "https://github.com/kubernetes-incubator/cri-tools/releases/download/${CRICTL_VERSION}/crictl-${CRICTL_VERSION}-linux-amd64.tar.gz" -o "${crictl_dir}/crictl-${CRICTL_VERSION}-linux-amd64.tar.gz"

echo "Downloading Kubernetes tools ${RELEASE}..."
k8s_dir="${working_dir}/k8s"
mkdir -p "${k8s_dir}"
cd "${k8s_dir}"
curl -L --remote-name-all https://storage.googleapis.com/kubernetes-release/release/${RELEASE}/bin/linux/amd64/{kubeadm,kubelet,kubectl}
kubeadm_file_permissions=`stat --format '%a' kubeadm`
chmod +x kubeadm

echo "Downloading kubelet.service ${RELEASE}..."
cd $start_dir
kubelet_service_file="${working_dir}/kubelet.service"
touch "${kubelet_service_file}"
curl -sSL "https://raw.githubusercontent.com/kubernetes/kubernetes/${RELEASE}/build/debs/kubelet.service" | sed "s:/usr/bin:/opt/bin:g" > ${kubelet_service_file}
echo "Downloading 10-kubeadm.conf ${RELEASE}..."
kubeadm_conf_file="${working_dir}/10-kubeadm.conf"
touch "${kubeadm_conf_file}"
curl -sSL "https://raw.githubusercontent.com/kubernetes/kubernetes/${RELEASE}/build/debs/10-kubeadm.conf" | sed "s:/usr/bin:/opt/bin:g" > ${kubeadm_conf_file}

echo "Fetching k8s docker images..."
docker -v
if [ $? -ne 0 ]; then
    echo "Installing docker..."
    sudo apt update && sudo apt install docker.io -y
    sudo systemctl enable docker && sudo systemctl start docker
fi
mkdir -p "${working_dir}/docker"
output=`${k8s_dir}/kubeadm config images list`
while read -r line; do
    echo "Downloading docker image $line ---"
    sudo docker pull "$line"
    image_name=`echo "$line" | grep -oE "[^/]+$"`
    sudo docker save "$line" > "${working_dir}/docker/$image_name.tar"
done <<< "$output"

echo "Restore kubeadm permissions..."
if [ "${kubeadm_file_permissions}" -eq "" ]; then
    kubeadm_file_permissions=644
fi
chmod ${kubeadm_file_permissions} "${working_dir}/k8s/kubeadm"

mkisofs -o setup.iso -J -R -l "${iso_dir}"

rm -rf "${iso_dir}"