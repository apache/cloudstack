#!/usr/bin/env bash

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

set -o errexit
set -o nounset
set -o pipefail

if [[ "${TRACE-0}" == "1" ]]; then
    set -o xtrace
fi

if [ $# -lt 8 ]; then
    echo "============================================================================================================"
    echo "CloudStack Kubernetes Service (CKS) - ISO Creation Script with Cilium"
    echo "============================================================================================================"
    echo ""
    echo "This script creates an ISO image containing Kubernetes binaries and dependencies for CKS with Cilium CNI."
    echo ""
    echo "Usage:"
    echo "  ./create-kubernetes-binaries-iso-with-cilium.sh OUTPUT_PATH KUBERNETES_VERSION CNI_VERSION CRICTL_VERSION BUILD_NAME ARCH ETCD_VERSION CILIUM_VERSION"
    echo ""
    echo "Parameters:"
    echo "  OUTPUT_PATH         - Directory where the ISO file will be saved (e.g., ./ or /tmp/)"
    echo "  KUBERNETES_VERSION  - Kubernetes version without 'v' prefix (e.g., 1.35.0)"
    echo "  CNI_VERSION         - CNI Plugins version without 'v' prefix (e.g., 1.9.0)"
    echo "  CRICTL_VERSION      - CRI Tools version without 'v' prefix (e.g., 1.35.0)"
    echo "  BUILD_NAME          - Name for the output ISO file without extension (e.g., cks-v1.35.0)"
    echo "  ARCH                - Target architecture: amd64, x86_64, arm64, or aarch64"
    echo "  ETCD_VERSION        - etcd version without 'v' prefix (e.g., 3.6.7)"
    echo "  CILIUM_VERSION      - Cilium version without 'v' prefix (e.g., 1.19.0)"
    echo ""
    echo "Example:"
    echo "  ./create-kubernetes-binaries-iso-with-cilium.sh ./ 1.35.0 1.9.0 1.35.0 cks-v1.35.0 amd64 3.6.7 1.19.0"
    echo ""
    echo "Output:"
    echo "  The script will generate: cks-v1.35.0-x86_64.iso (or cks-v1.35.0-aarch64.iso for ARM)"
    echo "============================================================================================================"
    exit 1
fi

ARCH="amd64"
ARCH_SUFFIX="x86_64"
if [ "${6}" = "x86_64" ] || [ "${6}" = "amd64" ]; then
  ARCH="amd64"
  ARCH_SUFFIX="x86_64"
elif [ "${6}" = "aarch64" ] || [ "${6}" = "arm64" ]; then
  ARCH="arm64"
  ARCH_SUFFIX="aarch64"
else
  echo "ERROR: ARCH must be one of: x86_64, amd64, aarch64, or arm64."
  exit 1
fi

RELEASE="v${2}"
MIN_UPSTREAM_VERSION="1.18.0"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# version_lt returns 0 (true) if $1 < $2 using semver-aware comparison
version_lt() {
  [ "$(printf '%s\n%s' "$1" "$2" | sort -V | head -n1)" = "$1" ] && [ "$1" != "$2" ]
}

output_dir="${1}"
start_dir="$PWD"
iso_dir=$(mktemp -d)
trap 'rm -rf "${iso_dir}"' EXIT
working_dir="${iso_dir}/"
if [ -n "${5}" ]; then
  build_name="${5}-${ARCH_SUFFIX}.iso"
else
  build_name="setup-${RELEASE}-${ARCH_SUFFIX}.iso"
fi

CNI_VERSION="v${3}"
echo "Downloading CNI ${CNI_VERSION}..."
cni_dir="${working_dir}/cni/"
mkdir -p "${cni_dir}"
if ! curl -sSf -L "https://github.com/containernetworking/plugins/releases/download/${CNI_VERSION}/cni-plugins-linux-${ARCH}-${CNI_VERSION}.tgz" -o "${cni_dir}/cni-plugins-${ARCH}.tgz" 2>/dev/null; then
  echo "Primary CNI URL failed, trying legacy URL format..."
  if ! curl -sSf -L "https://github.com/containernetworking/plugins/releases/download/${CNI_VERSION}/cni-plugins-${ARCH}-${CNI_VERSION}.tgz" -o "${cni_dir}/cni-plugins-${ARCH}.tgz"; then
    echo "ERROR: Failed to download CNI plugins ${CNI_VERSION} for ${ARCH} from both URL formats."
    exit 1
  fi
fi

CRICTL_VERSION="v${4}"
echo "Downloading CRI tools ${CRICTL_VERSION}..."
crictl_dir="${working_dir}/cri-tools/"
mkdir -p "${crictl_dir}"
curl -sS -L "https://github.com/kubernetes-incubator/cri-tools/releases/download/${CRICTL_VERSION}/crictl-${CRICTL_VERSION}-linux-${ARCH}.tar.gz" -o "${crictl_dir}/crictl-linux-${ARCH}.tar.gz"

echo "Downloading Kubernetes tools ${RELEASE}..."
k8s_dir="${working_dir}/k8s"
mkdir -p "${k8s_dir}"
cd "${k8s_dir}"
for binary in kubeadm kubelet kubectl; do
  echo "  Downloading ${binary}..."
  curl --progress-bar -fL "https://dl.k8s.io/release/${RELEASE}/bin/linux/${ARCH}/${binary}" -o "${binary}"
done
kubeadm_file_permissions=$(stat --format '%a' kubeadm)
chmod +x kubeadm

echo "Downloading kubelet.service ${RELEASE}..."
cd "${start_dir}"
kubelet_service_file="${working_dir}/kubelet.service"
touch "${kubelet_service_file}"
if version_lt "${2}" "${MIN_UPSTREAM_VERSION}"; then
  curl -sSfL "https://raw.githubusercontent.com/kubernetes/kubernetes/${RELEASE}/build/debs/kubelet.service" | sed "s:/usr/bin:/opt/bin:g" > "${kubelet_service_file}"
else
  sed "s:/usr/bin:/opt/bin:g" "${SCRIPT_DIR}/kubelet.service" > "${kubelet_service_file}"
fi

echo "Downloading 10-kubeadm.conf ${RELEASE}..."
kubeadm_conf_file="${working_dir}/10-kubeadm.conf"
touch "${kubeadm_conf_file}"
if version_lt "${2}" "${MIN_UPSTREAM_VERSION}"; then
  curl -sSfL "https://raw.githubusercontent.com/kubernetes/kubernetes/${RELEASE}/build/debs/10-kubeadm.conf" | sed "s:/usr/bin:/opt/bin:g" > "${kubeadm_conf_file}"
else
  sed "s:/usr/bin:/opt/bin:g" "${SCRIPT_DIR}/10-kubeadm.conf" > "${kubeadm_conf_file}"
fi

AUTOSCALER_URL="https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/cloudstack/examples/cluster-autoscaler-standard.yaml"
echo "Downloading kubernetes cluster autoscaler ${AUTOSCALER_URL}"
autoscaler_conf_file="${working_dir}/autoscaler.yaml"
curl -sSL "${AUTOSCALER_URL}" -o "${autoscaler_conf_file}"

PROVIDER_URL="https://raw.githubusercontent.com/apache/cloudstack-kubernetes-provider/main/deployment.yaml"
echo "Downloading kubernetes cluster provider ${PROVIDER_URL}"
provider_conf_file="${working_dir}/provider.yaml"
curl -sSL "${PROVIDER_URL}" -o "${provider_conf_file}"

CILIUM_VERSION="${8}"
echo "Generating Cilium ${CILIUM_VERSION} manifest..."
network_conf_file="${working_dir}/network.yaml"

# Check if helm is installed
if ! command -v helm > /dev/null 2>&1; then
  echo "ERROR: Helm is required to generate Cilium manifests. Please install Helm first."
  echo "Visit: https://helm.sh/docs/intro/install/"
  exit 1
fi

# Add the Cilium Helm repository only if it is not already configured
if ! helm repo list 2>/dev/null | awk 'NR>1 {print $1}' | grep -qx "cilium"; then
  helm repo add cilium https://helm.cilium.io/ > /dev/null 2>&1
fi

echo "Updating Helm repositories..."
helm repo update
echo "Generating Cilium manifest with Helm..."
if ! helm template cilium cilium/cilium --version "${CILIUM_VERSION}" \
  --kube-version "${RELEASE}" \
  --namespace kube-system \
  --set kubeProxyReplacement=true \
  --set socketLB.hostNamespaceOnly=true \
  --set cni.exclusive=true \
  --set cni.chainingMode=portmap \
  --set bgpControlPlane.enabled=true \
  --set encryption.enabled=true \
  --set encryption.type=wireguard \
  --set encryption.nodeEncryption=true \
  --set gatewayAPI.enabled=false \
  --set ingressController.enabled=false \
  --set l2announcements.enabled=true \
  --set l2podAnnouncements.enabled=true \
  --set l2podAnnouncements.interfacePattern=eth.* \
  --set ipam.mode=cluster-pool \
  --set ipam.operator.clusterPoolIPv4PodCIDRList={10.168.0.0/16} \
  --set ipam.operator.clusterPoolIPv4MaskSize=24 \
  --set hubble.relay.enabled=false \
  --set hubble.ui.enabled=false > "${network_conf_file}"; then
  echo "ERROR: Failed to generate Cilium manifest with Helm"
  echo "Check if Cilium version ${CILIUM_VERSION} exists in the Helm repository"
  exit 1
fi
echo "Cilium manifest generated successfully"

DASHBOARD_VERSION="2.7.0"
echo "Downloading Kubernetes Dashboard v${DASHBOARD_VERSION}..."
dashboard_conf_file="${working_dir}/dashboard.yaml"
curl -sSL "https://raw.githubusercontent.com/kubernetes/dashboard/v${DASHBOARD_VERSION}/aio/deploy/recommended.yaml" -o "${dashboard_conf_file}"

csi_conf_file="${working_dir}/manifest.yaml"
echo "Including CloudStack CSI Driver manifest"
wget https://github.com/cloudstack/cloudstack-csi-driver/releases/download/v3.0.0/snapshot-crds.yaml -O "${working_dir}/snapshot-crds.yaml"
wget https://github.com/cloudstack/cloudstack-csi-driver/releases/download/v3.0.0/manifest.yaml -O "${csi_conf_file}"

echo "Fetching k8s docker images..."
if ! ctr -v > /dev/null 2>&1; then
    echo "Installing containerd..."
    if [ -f /etc/redhat-release ]; then
      if command -v dnf > /dev/null 2>&1; then
        PKG_MGR="dnf"
      else
        PKG_MGR="yum"
      fi
      sudo $PKG_MGR --assumeyes remove docker-common docker container-selinux docker-selinux docker-engine
      sudo $PKG_MGR --assumeyes install lvm2 device-mapper device-mapper-persistent-data device-mapper-event device-mapper-libs device-mapper-event-libs container-selinux containerd.io
    elif [ -f /etc/debian_version ] || command -v apt-get > /dev/null 2>&1; then
      sudo apt-get update && sudo apt-get install containerd.io --yes
    fi
    sudo systemctl enable --now containerd
fi
mkdir -p "${working_dir}/docker"
output=$("${k8s_dir}/kubeadm" config images list --kubernetes-version="${RELEASE}")

# Don't forget about the other image !
autoscaler_image=$(grep "image:" "${autoscaler_conf_file}" | cut -d ':' -f2- | tr -d ' ')
output=$(printf "%s\n" "${output}" "${autoscaler_image}")

provider_image=$(grep "image:" "${provider_conf_file}" | cut -d ':' -f2- | tr -d ' ')
output=$(printf "%s\n" "${output}" "${provider_image}")

# Extract images from manifest.yaml and add to output
csi_images=$(grep "image:" "${csi_conf_file}" | cut -d ':' -f2- | tr -d ' ' | tr -d "'")
output=$(printf "%s\n%s" "${output}" "${csi_images}")

# Extract all images from yaml manifests (including Cilium network.yaml and Dashboard)
echo "Extracting images from manifest files..."
for i in "${network_conf_file}" "${dashboard_conf_file}"; do
  images=$(grep "image:" "$i" | cut -d ':' -f2- | tr -d ' ' | tr -d "'" | tr -d '"')
  output=$(printf "%s\n" "${output}" "${images}")
done

while read -r line; do
    echo "Downloading image $line ---"
    if [[ $line == kubernetesui* ]] || [[ $line == apache* ]] || [[ $line == weaveworks* ]]; then
      line="docker.io/${line}"
    fi
    if [[ $line == kong* ]]; then
      line="docker.io/library/${line}"
    fi
    line=$(echo "$line" | tr -d '"' | tr -d "'")

    # Pull image using containerd with k8s.io namespace
    if ! sudo ctr -n k8s.io images pull "$line"; then
      echo "ERROR: Failed to pull image $line"
      echo "Trying with default namespace..."
      if ! sudo ctr images pull "$line"; then
        echo "ERROR: Failed to pull image $line with both namespaces. Skipping..."
        continue
      fi
      # If successful with default namespace, use it for export
      image_name=$(echo "$line" | grep -oE "[^/]+$")
      sudo ctr images export "${working_dir}/docker/$image_name.tar" "$line"
      sudo ctr images rm "$line"
    else
      # If successful with k8s.io namespace
      image_name=$(echo "$line" | grep -oE "[^/]+$")
      sudo ctr -n k8s.io images export "${working_dir}/docker/$image_name.tar" "$line"
      sudo ctr -n k8s.io images rm "$line"
    fi
done <<< "$output"

echo "Restore kubeadm permissions..."
if [ -z "${kubeadm_file_permissions}" ]; then
    kubeadm_file_permissions=644
fi
chmod "${kubeadm_file_permissions}" "${working_dir}/k8s/kubeadm"

echo "Updating imagePullPolicy to IfNotPresent in yaml files..."
sed -i "s/imagePullPolicy:.*/imagePullPolicy: IfNotPresent/g" "${working_dir}"/*.yaml

etcd_dir="${working_dir}/etcd"
mkdir -p "${etcd_dir}"
ETCD_VERSION=v${7}
echo "Downloading etcd ${ETCD_VERSION}..."
curl -sSfL "https://github.com/etcd-io/etcd/releases/download/${ETCD_VERSION}/etcd-${ETCD_VERSION}-linux-${ARCH}.tar.gz" -o "${etcd_dir}/etcd-linux-${ARCH}.tar.gz"

# Validate that the downloaded etcd archive is a valid tar.gz
if ! tar -tzf "${etcd_dir}/etcd-linux-${ARCH}.tar.gz" > /dev/null; then
    echo "ERROR: Downloaded etcd archive is invalid or corrupted."
    exit 1
fi

mkisofs -o "${output_dir}/${build_name}" -J -R -l "${iso_dir}"