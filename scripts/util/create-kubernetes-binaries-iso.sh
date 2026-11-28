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

# Default cloudstack-kubernetes-provider (CCM) release. v1.2.0 is the first
# release that initialises nodes (sets their providerID and zone) correctly.
DEFAULT_CCM_VERSION="v1.2.0"
CSI_DRIVER_VERSION="v3.0.0"

# Fail on HTTP errors instead of saving the error page as the file
download() {
  curl -fsSL --retry 3 "$1" -o "$2"
}

# Point the only container image in manifest $1 at image $2
set_manifest_image() {
  local count
  count=$(grep -c -E "^[[:space:]-]*image:" "$1" || true)
  if [ "${count}" != "1" ]; then
    echo "ERROR: expected one image in $1, found ${count}"
    exit 1
  fi
  sed -i -E "s|^([[:space:]-]*image:).*|\1 $2|" "$1"
}

if [ $# -lt 6 ]; then
    echo "Invalid input. Valid usage: ./create-kubernetes-binaries-iso.sh OUTPUT_PATH KUBERNETES_VERSION CNI_VERSION CRICTL_VERSION WEAVENET_NETWORK_YAML_CONFIG DASHBOARD_YAML_CONFIG BUILD_NAME [ARCH] [ETCD_VERSION]"
    echo "eg: ./create-kubernetes-binaries-iso.sh ./ 1.11.4 0.7.1 1.11.1 https://github.com/weaveworks/weave/releases/download/latest_release/weave-daemonset-k8s-1.11.yaml https://raw.githubusercontent.com/kubernetes/dashboard/v1.10.0/src/deploy/recommended/kubernetes-dashboard.yaml setup-v1.11.4 amd64"
    echo "Optional environment variables:"
    echo "  AUTOSCALER_VERSION  cluster-autoscaler release, e.g. 1.33.6 (default: the newest release for the Kubernetes minor version)"
    echo "  CCM_VERSION         cloudstack-kubernetes-provider release, e.g. v1.2.0 (default: ${DEFAULT_CCM_VERSION})"
    exit 1
fi

ARCH="amd64"
ARCH_SUFFIX="x86_64"
if [ -n "${8}" ]; then
  if [ "${8}" = "x86_64" ] || [ "${8}" = "amd64" ]; then
    ARCH="amd64"
    ARCH_SUFFIX="x86_64"
  elif [ "${8}" = "aarch64" ] || [ "${8}" = "arm64" ]; then
    ARCH="arm64"
    ARCH_SUFFIX="aarch64"
  else
    echo "ERROR: ARCH must be 'x86_64' or 'aarch64'. If the optional parameter ARCH is not set then 'x86_64' is used."
    exit 1
  fi
fi

RELEASE="v${2}"
VAL="1.18.0"
output_dir="${1}"
start_dir="$PWD"
# A fresh directory, so files left by a failed build don't end up in this ISO
iso_dir=$(mktemp -d)
chmod 755 "${iso_dir}"
trap 'rm -rf "${iso_dir}"' EXIT
working_dir="${iso_dir}/"
mkdir -p "${working_dir}"
build_name="${7}-${ARCH_SUFFIX}.iso"
[ -z "${build_name}" ] && build_name="setup-${RELEASE}-${ARCH_SUFFIX}.iso"

CNI_VERSION="v${3}"
echo "Downloading CNI ${CNI_VERSION}..."
cni_dir="${working_dir}/cni/"
mkdir -p "${cni_dir}"
# Older CNI releases name the archive without "linux-"
CNI_URL="https://github.com/containernetworking/plugins/releases/download/${CNI_VERSION}/cni-plugins-linux-${ARCH}-${CNI_VERSION}.tgz"
if ! download "${CNI_URL}" "${cni_dir}/cni-plugins-${ARCH}.tgz"; then
  CNI_URL="https://github.com/containernetworking/plugins/releases/download/${CNI_VERSION}/cni-plugins-${ARCH}-${CNI_VERSION}.tgz"
  download "${CNI_URL}" "${cni_dir}/cni-plugins-${ARCH}.tgz"
fi

CRICTL_VERSION="v${4}"
echo "Downloading CRI tools ${CRICTL_VERSION}..."
crictl_dir="${working_dir}/cri-tools/"
mkdir -p "${crictl_dir}"
CRICTL_URL="https://github.com/kubernetes-incubator/cri-tools/releases/download/${CRICTL_VERSION}/crictl-${CRICTL_VERSION}-linux-${ARCH}.tar.gz"
download "${CRICTL_URL}" "${crictl_dir}/crictl-linux-${ARCH}.tar.gz"

echo "Downloading Kubernetes tools ${RELEASE}..."
k8s_dir="${working_dir}/k8s"
mkdir -p "${k8s_dir}"
cd "${k8s_dir}"
for binary in kubeadm kubelet kubectl; do
  download "https://dl.k8s.io/release/${RELEASE}/bin/linux/${ARCH}/${binary}" "${binary}"
done
kubeadm_file_permissions=`stat --format '%a' kubeadm`
chmod +x kubeadm

echo "Downloading kubelet.service ${RELEASE}..."
cd "${start_dir}"
kubelet_service_file="${working_dir}/kubelet.service"
if [[ `echo "${2} $VAL" | awk '{print ($1 < $2)}'` == 1 ]]; then
  KUBELET_SERVICE_URL="https://raw.githubusercontent.com/kubernetes/kubernetes/${RELEASE}/build/debs/kubelet.service"
else
  KUBELET_SERVICE_URL="https://raw.githubusercontent.com/shapeblue/cloudstack-nonoss/main/cks/kubelet.service"
fi
download "${KUBELET_SERVICE_URL}" "${kubelet_service_file}"
sed -i "s:/usr/bin:/opt/bin:g" "${kubelet_service_file}"

echo "Downloading 10-kubeadm.conf ${RELEASE}..."
kubeadm_conf_file="${working_dir}/10-kubeadm.conf"
if [[ `echo "${2} $val" | awk '{print ($1 < $2)}'` == 1 ]]; then
  KUBEADM_CONF_URL="https://raw.githubusercontent.com/kubernetes/kubernetes/${RELEASE}/build/debs/10-kubeadm.conf"
else
  KUBEADM_CONF_URL="https://raw.githubusercontent.com/shapeblue/cloudstack-nonoss/main/cks/10-kubeadm.conf"
fi
download "${KUBEADM_CONF_URL}" "${kubeadm_conf_file}"
sed -i "s:/usr/bin:/opt/bin:g" "${kubeadm_conf_file}"

NETWORK_CONFIG_URL="${5}"
echo "Downloading network config ${NETWORK_CONFIG_URL}"
network_conf_file="${working_dir}/network.yaml"
download "${NETWORK_CONFIG_URL}" "${network_conf_file}"

DASHBORAD_CONFIG_URL="${6}"
echo "Downloading dashboard config ${DASHBORAD_CONFIG_URL}"
dashboard_conf_file="${working_dir}/dashboard.yaml"
download "${DASHBORAD_CONFIG_URL}" "${dashboard_conf_file}"

# The cluster-autoscaler only supports the Kubernetes minor version it was
# released with, so use the newest release for this minor version unless
# AUTOSCALER_VERSION is set.
KUBERNETES_MINOR=$(echo "${2}" | cut -d. -f1-2)
if [ -z "${AUTOSCALER_VERSION}" ]; then
  AUTOSCALER_VERSION=$(git ls-remote --tags --refs https://github.com/kubernetes/autoscaler.git "cluster-autoscaler-${KUBERNETES_MINOR}.*" \
    | sed -n -E 's|.*refs/tags/cluster-autoscaler-([0-9]+\.[0-9]+\.[0-9]+)$|\1|p' | sort -V | tail -n 1)
  if [ -z "${AUTOSCALER_VERSION}" ]; then
    echo "ERROR: could not find a cluster-autoscaler release for Kubernetes ${KUBERNETES_MINOR} (is git installed and github.com reachable?). Set AUTOSCALER_VERSION to choose one."
    exit 1
  fi
fi
AUTOSCALER_VERSION="${AUTOSCALER_VERSION#v}"
if [ "$(echo "${AUTOSCALER_VERSION}" | cut -d. -f1-2)" != "${KUBERNETES_MINOR}" ]; then
  echo "WARNING: cluster-autoscaler ${AUTOSCALER_VERSION} is not built for Kubernetes ${KUBERNETES_MINOR}"
fi
# The example manifest names an image that is not versioned, so use the
# upstream image (it includes the CloudStack provider) of the same release.
AUTOSCALER_IMAGE="registry.k8s.io/autoscaling/cluster-autoscaler:v${AUTOSCALER_VERSION}"
AUTOSCALER_URL="https://raw.githubusercontent.com/kubernetes/autoscaler/cluster-autoscaler-${AUTOSCALER_VERSION}/cluster-autoscaler/cloudprovider/cloudstack/examples/cluster-autoscaler-standard.yaml"
echo "Downloading kubernetes cluster autoscaler ${AUTOSCALER_URL}"
autoscaler_conf_file="${working_dir}/autoscaler.yaml"
download "${AUTOSCALER_URL}" "${autoscaler_conf_file}"
set_manifest_image "${autoscaler_conf_file}" "${AUTOSCALER_IMAGE}"
# The example manifest's ClusterRole lacks read access that newer releases
# need: VolumeAttachments (1.32 and later) and DRA objects (1.35 and later).
# Without it the autoscaler never finishes syncing its caches and never
# scales, so grant it in an additional ClusterRole.
cat >> "${autoscaler_conf_file}" <<EOF

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: cluster-autoscaler-extra
  labels:
    k8s-addon: cluster-autoscaler.addons.k8s.io
    k8s-app: cluster-autoscaler
rules:
  - apiGroups: ["storage.k8s.io"]
    resources: ["volumeattachments"]
    verbs: ["watch", "list", "get"]
  - apiGroups: ["resource.k8s.io"]
    resources: ["deviceclasses", "resourceslices", "resourceclaims"]
    verbs: ["watch", "list", "get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: cluster-autoscaler-extra
  labels:
    k8s-addon: cluster-autoscaler.addons.k8s.io
    k8s-app: cluster-autoscaler
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-autoscaler-extra
subjects:
  - kind: ServiceAccount
    name: cluster-autoscaler
    namespace: kube-system
EOF

CCM_VERSION="${CCM_VERSION:-${DEFAULT_CCM_VERSION}}"
CCM_VERSION="v${CCM_VERSION#v}"
CCM_IMAGE="apache/cloudstack-kubernetes-provider:${CCM_VERSION}"
PROVIDER_URL="https://raw.githubusercontent.com/apache/cloudstack-kubernetes-provider/${CCM_VERSION}/deployment.yaml"
echo "Downloading kubernetes cluster provider ${PROVIDER_URL}"
provider_conf_file="${working_dir}/provider.yaml"
download "${PROVIDER_URL}" "${provider_conf_file}"
set_manifest_image "${provider_conf_file}" "${CCM_IMAGE}"

csi_conf_file="${working_dir}/manifest.yaml"
echo "Including CloudStack CSI Driver manifest"
CSI_SNAPSHOT_CRDS_URL="https://github.com/cloudstack/cloudstack-csi-driver/releases/download/${CSI_DRIVER_VERSION}/snapshot-crds.yaml"
CSI_MANIFEST_URL="https://github.com/cloudstack/cloudstack-csi-driver/releases/download/${CSI_DRIVER_VERSION}/manifest.yaml"
download "${CSI_SNAPSHOT_CRDS_URL}" "${working_dir}/snapshot-crds.yaml"
download "${CSI_MANIFEST_URL}" "${csi_conf_file}"

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
output=`printf "%s\n" ${output} ${AUTOSCALER_IMAGE} ${CCM_IMAGE}`

# Extract images from manifest.yaml and add to output
csi_images=`grep "image:" "${csi_conf_file}" | cut -d ':' -f2- | tr -d ' ' | tr -d "'"`
output=`printf "%s\n%s" "${output}" "${csi_images}" | awk 'NF && !seen[$0]++'`

image_list=""
while read -r line; do
    echo "Downloading image $line ---"
    if [[ $line == kubernetesui* ]] || [[ $line == apache* ]] || [[ $line == weaveworks* ]]; then
      line="docker.io/${line}"
    fi
    sudo ctr image pull "$line"
    image_ref="$line"
    if [[ $line != *@* ]]; then
      image_ref="$line@`sudo ctr image ls "name==$line" | awk 'NR == 2 {print $3}'`"
    fi
    image_list=`printf "%s\nIMAGE=%s" "${image_list}" "${image_ref}"`
    image_name=`echo "$line" | grep -oE "[^/]+$"`
    sudo ctr image export "${working_dir}/docker/$image_name.tar" "$line"
    # --sync: finish garbage collection now, so it can't remove layers the next pull shares
    sudo ctr image rm --sync "$line"
done <<< "$output"

echo "Restore kubeadm permissions..."
if [ -z "${kubeadm_file_permissions}" ]; then
    kubeadm_file_permissions=644
fi
chmod ${kubeadm_file_permissions} "${working_dir}/k8s/kubeadm"

echo "Updating imagePullPolicy to IfNotPresent in yaml files..."
sed -i "s/imagePullPolicy:.*/imagePullPolicy: IfNotPresent/g" ${working_dir}/*.yaml

# Optional parameter ETCD_VERSION
ETCD_VERSION=""
ETCD_URL=""
if [ -n "${9}" ]; then
  # Install etcd dependencies
  etcd_dir="${working_dir}/etcd"
  mkdir -p "${etcd_dir}"
  ETCD_VERSION=v${9}
  ETCD_URL="https://github.com/etcd-io/etcd/releases/download/${ETCD_VERSION}/etcd-${ETCD_VERSION}-linux-amd64.tar.gz"
  download "${ETCD_URL}" "${etcd_dir}/etcd-linux-amd64.tar.gz"
fi

# Record what went into the ISO
cat > "${working_dir}/versions.txt" <<EOF
# Contents of ${build_name}, built by create-kubernetes-binaries-iso.sh on $(date -u +%Y-%m-%dT%H:%M:%SZ)
# The autoscaler and CCM manifests are the ones at these URLs with their image set to the
# *_IMAGE below; the autoscaler manifest also gets the cluster-autoscaler-extra ClusterRole.
ARCH=${ARCH}
KUBERNETES_VERSION=${RELEASE}
CNI_PLUGINS_VERSION=${CNI_VERSION}
CNI_PLUGINS_URL=${CNI_URL}
CRICTL_VERSION=${CRICTL_VERSION}
CRICTL_URL=${CRICTL_URL}
KUBELET_SERVICE_URL=${KUBELET_SERVICE_URL}
KUBEADM_CONF_URL=${KUBEADM_CONF_URL}
NETWORK_MANIFEST_URL=${NETWORK_CONFIG_URL}
DASHBOARD_MANIFEST_URL=${DASHBORAD_CONFIG_URL}
AUTOSCALER_VERSION=${AUTOSCALER_VERSION}
AUTOSCALER_MANIFEST_URL=${AUTOSCALER_URL}
AUTOSCALER_IMAGE=${AUTOSCALER_IMAGE}
CCM_VERSION=${CCM_VERSION}
CCM_MANIFEST_URL=${PROVIDER_URL}
CCM_IMAGE=${CCM_IMAGE}
CSI_DRIVER_VERSION=${CSI_DRIVER_VERSION}
CSI_SNAPSHOT_CRDS_URL=${CSI_SNAPSHOT_CRDS_URL}
CSI_MANIFEST_URL=${CSI_MANIFEST_URL}
ETCD_VERSION=${ETCD_VERSION}
ETCD_URL=${ETCD_URL}
EOF
echo "${image_list}" | awk 'NF && !seen[$0]++' >> "${working_dir}/versions.txt"

mkisofs -o "${output_dir}/${build_name}" -J -R -l "${iso_dir}"
