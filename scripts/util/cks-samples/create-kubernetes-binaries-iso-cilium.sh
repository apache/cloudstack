#!/bin/bash -x
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

if [ $# -lt 8 ]; then
    echo "Invalid input. Valid usage: ./create-kubernetes-binaries-iso-cilium.sh OUTPUT_PATH KUBERNETES_VERSION CNI_VERSION CRICTL_VERSION CILIUM_VERSION HELM_VERSION DASHBOARD_YAML_CONFIG BUILD_NAME"
    echo "eg: ./create-kubernetes-binaries-iso-cilium.sh ./ 1.27.14 1.4.0 1.29.0 1.15.1 3.14.2 https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml setup-v1.27.14"
    exit 1
fi

RELEASE="v${2}"
VAL="1.18.0"
output_dir="${1}"
start_dir="$PWD"
iso_dir="/tmp/iso"
working_dir="${iso_dir}/"
mkdir -p "${working_dir}"
build_name="${8}.iso"
[ -z "${build_name}" ] && build_name="setup-${RELEASE}.iso"

CNI_VERSION="v${3}"
echo "Downloading CNI ${CNI_VERSION}..."
cni_dir="${working_dir}/cni/"
mkdir -p "${cni_dir}"
cni_status_code=$(curl -L  --write-out "%{http_code}\n" "https://github.com/containernetworking/plugins/releases/download/${CNI_VERSION}/cni-plugins-linux-amd64-${CNI_VERSION}.tgz" -o "${cni_dir}/cni-plugins-amd64.tgz")
if [[ ${cni_status_code} -eq 404 ]] ; then
  curl -L  --write-out "%{http_code}\n" "https://github.com/containernetworking/plugins/releases/download/${CNI_VERSION}/cni-plugins-amd64-${CNI_VERSION}.tgz" -o "${cni_dir}/cni-plugins-amd64.tgz"
fi

CRICTL_VERSION="v${4}"
echo "Downloading CRI tools ${CRICTL_VERSION}..."
crictl_dir="${working_dir}/cri-tools/"
mkdir -p "${crictl_dir}"
curl -L "https://github.com/kubernetes-incubator/cri-tools/releases/download/${CRICTL_VERSION}/crictl-${CRICTL_VERSION}-linux-amd64.tar.gz" -o "${crictl_dir}/crictl-linux-amd64.tar.gz"

echo "Downloading Kubernetes tools ${RELEASE}..."
k8s_dir="${working_dir}/k8s"
mkdir -p "${k8s_dir}"
cd "${k8s_dir}"
curl -L --remote-name-all https://storage.googleapis.com/kubernetes-release/release/${RELEASE}/bin/linux/amd64/{kubeadm,kubelet,kubectl}
kubeadm_file_permissions=`stat --format '%a' kubeadm`
chmod +x kubeadm

echo "Downloading kubelet.service ${RELEASE}..."
cd "${start_dir}"
kubelet_service_file="${working_dir}/kubelet.service"
touch "${kubelet_service_file}"
if [[ `echo "${2} $VAL" | awk '{print ($1 < $2)}'` == 1 ]]; then
  curl -sSL "https://raw.githubusercontent.com/kubernetes/kubernetes/${RELEASE}/build/debs/kubelet.service" | sed "s:/usr/bin:/opt/bin:g" > ${kubelet_service_file}
else
  curl -sSL "https://raw.githubusercontent.com/shapeblue/cloudstack-nonoss/main/cks/kubelet.service" | sed "s:/usr/bin:/opt/bin:g" > ${kubelet_service_file}
fi

echo "Downloading 10-kubeadm.conf ${RELEASE}..."
kubeadm_conf_file="${working_dir}/10-kubeadm.conf"
touch "${kubeadm_conf_file}"
if [[ `echo "${2} $val" | awk '{print ($1 < $2)}'` == 1 ]]; then
  curl -sSL "https://raw.githubusercontent.com/kubernetes/kubernetes/${RELEASE}/build/debs/10-kubeadm.conf" | sed "s:/usr/bin:/opt/bin:g" > ${kubeadm_conf_file}
else
  curl -sSL "https://raw.githubusercontent.com/shapeblue/cloudstack-nonoss/main/cks/10-kubeadm.conf" | sed "s:/usr/bin:/opt/bin:g" > ${kubeadm_conf_file}
fi

HELM_VERSION="${6}"
mkdir -p ${iso_dir}/installs
curl -L -o ${iso_dir}/installs/helm-v${HELM_VERSION}-linux-amd64.tar.gz https://get.helm.sh/helm-v${HELM_VERSION}-linux-amd64.tar.gz

CILIUM_VERSION="${5}"
echo "Downloading Cilium Helm chart version ${CILIUM_VERSION} and cilium cli"
mkdir -p ${iso_dir}/installs/charts
curl -Lo ${iso_dir}/installs/charts/cilium-${CILIUM_VERSION}.tgz https://helm.cilium.io/cilium-${CILIUM_VERSION}.tgz

CILIUM_CLI_VERSION=$(curl -s https://raw.githubusercontent.com/cilium/cilium-cli/main/stable.txt)
CLI_ARCH=amd64
curl -Lo ${iso_dir}/installs/cilium-linux-${CLI_ARCH}-cli-${CILIUM_CLI_VERSION}.tar.gz --fail https://github.com/cilium/cilium-cli/releases/download/${CILIUM_CLI_VERSION}/cilium-linux-${CLI_ARCH}.tar.gz

DASHBOARD_CONFIG_URL="${7}"
echo "Downloading dashboard config ${DASHBOARD_CONFIG_URL}"
dashboard_conf_file="${working_dir}/dashboard.yaml"
curl -sSL ${DASHBOARD_CONFIG_URL} -o ${dashboard_conf_file}

# TODO : Change the url once merged
AUTOSCALER_URL="https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/cloudstack/examples/cluster-autoscaler-standard.yaml"
echo "Downloading kubernetes cluster autoscaler ${AUTOSCALER_URL}"
autoscaler_conf_file="${working_dir}/autoscaler.yaml"
curl -sSL ${AUTOSCALER_URL} -o ${autoscaler_conf_file}

PROVIDER_URL="https://raw.githubusercontent.com/apache/cloudstack-kubernetes-provider/main/deployment.yaml"
echo "Downloading kubernetes cluster provider ${PROVIDER_URL}"
provider_conf_file="${working_dir}/provider.yaml"
curl -sSL ${PROVIDER_URL} -o ${provider_conf_file}

echo "Fetching k8s docker images..."
ctr -v
if [ $? -ne 0 ]; then
    echo "Installing containerd..."
    if [ -f /etc/redhat-release ]; then
      sudo yum -y remove docker-common docker container-selinux docker-selinux docker-engine
      sudo yum -y install lvm2 device-mapper device-mapper-persistent-data device-mapper-event device-mapper-libs device-mapper-event-libs
      sudo yum install -y http://mirror.centos.org/centos/7/extras/x86_64/Packages/container-selinux-2.107-3.el7.noarch.rpm
      sudo yum install -y containerd.io
    elif [ -f /etc/lsb-release ]; then
      sudo apt update && sudo apt install containerd.io -y
    fi
    sudo systemctl enable containerd && sudo systemctl start containerd
fi
mkdir -p "${working_dir}/docker"
output=`${k8s_dir}/kubeadm config images list --kubernetes-version=${RELEASE}`

# Don't forget about the yaml images !
for i in ${network_conf_file} ${dashboard_conf_file}
do
  images=`grep "image:" $i | cut -d ':' -f2- | tr -d ' ' | tr -d "'"`
  output=`printf "%s\n" ${output} ${images}`
done

# Don't forget about the other image !
autoscaler_image=`grep "image:" ${autoscaler_conf_file} | cut -d ':' -f2- | tr -d ' '`
output=`printf "%s\n" ${output} ${autoscaler_image}`

provider_image=`grep "image:" ${provider_conf_file} | cut -d ':' -f2- | tr -d ' '`
output=`printf "%s\n" ${output} ${provider_image}`

cp -r helm-overrides/ ${iso_dir}/installs

chart_images=`for chart in ${iso_dir}/installs/charts/*; do chartfile=${chart##*/}; chartbase=${chartfile%-*}; if [ -f ${iso_dir}/installs/helm-overrides/${chartbase}-overrides.yaml ]; then helm template -f ${iso_dir}/installs/helm-overrides/${chartbase}-overrides.yaml ${chart}; else helm template ${chart}; fi; done | grep "[[:space:]]image:" | cut -d ':' -f2- | tr -d ' ' | tr -d '\r' | tr -d "'" | tr -d '"' | sort -u`
output=`printf "%s\n" ${output} ${chart_images}`

while read -r line; do
    echo "Downloading image $line ---"
    if [[ $line == kubernetesui* ]] || [[ $line == apache* ]]; then
      line="docker.io/${line}"
    fi
    if [ ! -z "${https_proxy}" ]; then
      sudo https_proxy=${https_proxy} ctr image pull "$line"
    else
      sudo ctr image pull "$line"
    fi
    image_name=`echo "$line" | grep -oE "[^/]+$"`
    sudo ctr image export "${working_dir}/docker/$image_name.tar" "$line"
    sudo ctr image rm "$line"
done <<< "$output"

if [ -d scripts ]; then
    /bin/cp -r scripts ${iso_dir}
fi

echo "Restore kubeadm permissions..."
if [ -z "${kubeadm_file_permissions}" ]; then
    kubeadm_file_permissions=644
fi
chmod ${kubeadm_file_permissions} "${working_dir}/k8s/kubeadm"

echo "Updating imagePullPolicy to IfNotPresent in yaml files..."
sed -i "s/imagePullPolicy:.*/imagePullPolicy: IfNotPresent/g" ${working_dir}/*.yaml

mkisofs -o "${output_dir}/${build_name}" -J -R -l "${iso_dir}"

rm -rf "${iso_dir}"
