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

if [ $# -lt 2 ]; then
  echo "Usage: /bin/bash $0 PLUGIN_CONFDIR TARGETSCRIPTDIR" >&2
  echo "   eg: /bin/bash $0 ./plugins/integrations/kubernetes-service/src/main/resources/conf ./scripts/util/scripts" >&2
  exit 1
fi

PLUGIN_CONFDIR="${1}"
SCRIPTS_DIR="${2}"

# Control node
## setup-kube-system

mkdir -p ${SCRIPTS_DIR}/control-node

cat > "${SCRIPTS_DIR}/control-node/setup-kube-system" <<\!
#!/bin/bash -e

if [ $# -lt 8 ]; then
  echo "Usage: /bin/bash $0 BINARIES_DIR CLUSTER_TOKEN CLUSTER_INITARGS REGISTRY_URL REGISTRY_URL_ENDPOINT REGISTRY_USERNAME REGISTRY_PASSWORD REGISTRY_TOKEN" >&2
  exit 1
fi

BINARIES_DIR="$1"
CLUSTER_TOKEN="$2"
CLUSTER_INITARGS="$3"
REGISTRY_URL="$4"
REGISTRY_URL_ENDPOINT="$5"
REGISTRY_USERNAME="$6"
REGISTRY_PASSWORD="$7"
REGISTRY_TOKEN="$8"

if [ -f ${BINARIES_DIR}/scripts/control-node/deploy-kube-system.tmpl ]; then
  sed -e "s/{{ k8s_control_node.cluster.token }}/${CLUSTER_TOKEN}/g" -e "s/{{ k8s_control_node.cluster.initargs }}/${CLUSTER_INITARGS}/g" ${BINARIES_DIR}/scripts/control-node/deploy-kube-system.tmpl > /opt/bin/deploy-kube-system
fi

if [ -f ${BINARIES_DIR}/scripts/control-node/setup-containerd.tmpl ]; then
  sed -e "s/{{registry.url}}/${REGISTRY_URL}/g" -e "s/{{registry.url.endpoint}}/${REGISTRY_URL_ENDPOINT}/g" -e "s/{{registry.username}}/${REGISTRY_USERNAME}/g" -e "s/{{registry.password}}/${REGISTRY_PASSWORD}/g" -e "s/{{registry.token}}/${REGISTRY_TOKEN}/g" ${BINARIES_DIR}/scripts/control-node/setup-containerd.tmpl > /opt/bin/setup-containerd
fi

!

sed -e "/- path: \/opt\/bin\/setup-kube-system/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-control-node.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  | sed -e '/if \[ -f "${BINARIES_DIR}\/scripts\/control-node\/setup-kube-system" \]; then/,/^fi/d' -e '/^while true; do/,/^done/d' -e '/if \[ "$EJECT_ISO_FROM_OS" = true \]/,/fi/d' | egrep -v "^#!|^BINARIES_DIR|^EJECT_ISO_FROM_OS|umount" >> "${SCRIPTS_DIR}/control-node/setup-kube-system"

# Helm and Cilium-specific updates

BINARIES_LINE=$(grep -n 'Installing binaries from' "${SCRIPTS_DIR}/control-node/setup-kube-system" | cut -f1 -d: | head -1)

if [ -z "${BINARIES_LINE}" ]; then
  echo "Unable to find the Installing binaries from... line in the setup script (setup-kube-system). Unable to generate scripts." >&2
  exit 99
fi

echo "$(expr ${BINARIES_LINE} - 1)a${BINARIES_LINE},$(expr ${BINARIES_LINE} + 23)" > /tmp/setup.patch

cat <<\! >> /tmp/setup.patch
>   if [ -f ${BINARIES_DIR}/installs/helm-*-linux-amd64.tar.gz ]
>   then
>     TMPDIR=$(mktemp -d)
>     tar -xzf ${BINARIES_DIR}/installs/helm-*-linux-amd64.tar.gz -C ${TMPDIR}
>     /bin/mv ${TMPDIR}/linux-amd64/helm /usr/local/bin/helm
>     /bin/rm -rf ${TMPDIR}
>   fi
> #
>   if [ -f ${BINARIES_DIR}/installs/cilium-*-cli-*.tar.gz ]
>   then
>     tar -xzf ${BINARIES_DIR}/installs/cilium-*-cli-*.tar.gz -C /usr/local/bin
>     chmod 755 /usr/local/bin/cilium
>   fi
> #
>   K8S_HELM=/tmp/k8shelm/
>   if [ -d "${BINARIES_DIR}/installs/charts" ]; then
>     mkdir -p "${K8S_HELM}/charts"
>     cp ${BINARIES_DIR}/installs/charts/*.*gz "${K8S_HELM}/charts"
>   fi
>   if [ -d "${BINARIES_DIR}/installs/helm-overrides" ]; then
>     mkdir -p "${K8S_HELM}/overrides"
>     cp ${BINARIES_DIR}/installs/helm-overrides/*.yaml "${K8S_HELM}/overrides"
>   fi
> #
!

patch ${SCRIPTS_DIR}/control-node/setup-kube-system < /tmp/setup.patch

## deploy-kube-system

sed -e "/- path: \/opt\/bin\/deploy-kube-system/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-control-node.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  > "${SCRIPTS_DIR}/control-node/deploy-kube-system.tmpl"

# Helm and Cilium-specific updates

sed -i -e '/weave.works/d' -e '/network.yaml/d' "${SCRIPTS_DIR}/control-node/deploy-kube-system.tmpl"

FIRST_SUCCESS=$(grep -n '/home/cloud/success' "${SCRIPTS_DIR}/control-node/deploy-kube-system.tmpl" | grep -v ':if' | cut -f1 -d: | head -1)

if [ -z "${FIRST_SUCCESS}" ]; then
  echo "Unable to find the /home/cloud/success line in the deployment script (deploy-kube-system.tmpl). Unable to generate scripts." >&2
  exit 99
fi

echo "$(expr ${FIRST_SUCCESS} - 1)a${FIRST_SUCCESS},$(expr ${FIRST_SUCCESS} + 3)" > /tmp/deploy.patch

cat <<\! >> /tmp/deploy.patch
> K8S_HELM=/tmp/k8shelm/
> helm upgrade --install cilium ${K8S_HELM}/charts/cilium-*.tgz -f ${K8S_HELM}/overrides/cilium-overrides.yaml -n kube-system
> /bin/rm -rf ${K8S_HELM}
> #
!

patch "${SCRIPTS_DIR}/control-node/deploy-kube-system.tmpl" < /tmp/deploy.patch


## setup-containerd
sed -e "/- path: \/opt\/bin\/setup-containerd/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-control-node.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  > "${SCRIPTS_DIR}/control-node/setup-containerd.tmpl"

# Control node add

mkdir -p ${SCRIPTS_DIR}/control-node-add

cat > "${SCRIPTS_DIR}/control-node-add/setup-kube-system" <<\!
#!/bin/bash -e

if [ $# -lt 9 ]; then
  echo "Usage: /bin/bash $0 BINARIES_DIR CLUSTER_JOIN_IP CLUSTER_TOKEN CERTIFICATE_KEY REGISTRY_URL REGISTRY_URL_ENDPOINT REGISTRY_USERNAME REGISTRY_PASSWORD REGISTRY_TOKEN" >&2
  exit 1
fi

BINARIES_DIR="$1"
CLUSTER_JOIN_IP="$2"
CLUSTER_TOKEN="$3"
CERTIFICATE_KEY="$4"
REGISTRY_URL="$5"
REGISTRY_URL_ENDPOINT="$6"
REGISTRY_USERNAME="$7"
REGISTRY_PASSWORD="$8"
REGISTRY_TOKEN="$9"

if [ -f ${BINARIES_DIR}/scripts/control-node-add/deploy-kube-system.tmpl ]; then
  sed -e "s/{{ k8s_control_node.cluster.token }}/${CLUSTER_TOKEN}/g" -e "s/{{ k8s_control_node.cluster.ha.certificate.key }}/${CERTIFICATE_KEY}/g" -e "s/{{ k8s_control_node.join_ip }}/${CLUSTER_JOIN_IP}/g" ${BINARIES_DIR}/scripts/control-node-add/deploy-kube-system.tmpl > /opt/bin/deploy-kube-system
fi

if [ -f ${BINARIES_DIR}/scripts/control-node-add/setup-containerd.tmpl ]; then
  sed -e "s/{{registry.url}}/${REGISTRY_URL}/g" -e "s/{{registry.url.endpoint}}/${REGISTRY_URL_ENDPOINT}/g" -e "s/{{registry.username}}/${REGISTRY_USERNAME}/g" -e "s/{{registry.password}}/${REGISTRY_PASSWORD}/g" -e "s/{{registry.token}}/${REGISTRY_TOKEN}/g" ${BINARIES_DIR}/scripts/control-node-add/setup-containerd.tmpl > /opt/bin/setup-containerd
fi

!

sed -e "/- path: \/opt\/bin\/setup-kube-system/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-control-node-add.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  | sed -e '/if \[ -f "${BINARIES_DIR}\/scripts\/control-node-add\/setup-kube-system" \]; then/,/^fi/d' -e '/^while true; do/,/^done/d' -e '/if \[ "$EJECT_ISO_FROM_OS" = true \]/,/fi/d' | egrep -v "^#!|^BINARIES_DIR|^EJECT_ISO_FROM_OS|umount" >> "${SCRIPTS_DIR}/control-node-add/setup-kube-system"

## deploy-kube-system

sed -e "/- path: \/opt\/bin\/deploy-kube-system/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-control-node-add.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  > "${SCRIPTS_DIR}/control-node-add/deploy-kube-system.tmpl"

## setup-containerd
sed -e "/- path: \/opt\/bin\/setup-containerd/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-control-node-add.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  > "${SCRIPTS_DIR}/control-node-add/setup-containerd.tmpl"

# Node

mkdir -p ${SCRIPTS_DIR}/node

cat > "${SCRIPTS_DIR}/node/setup-kube-system" <<\!
#!/bin/bash -e

if [ $# -lt 8 ]; then
  echo "Usage: /bin/bash $0 BINARIES_DIR CLUSTER_JOIN_IP CLUSTER_TOKEN REGISTRY_URL REGISTRY_URL_ENDPOINT REGISTRY_USERNAME REGISTRY_PASSWORD REGISTRY_TOKEN" >&2
  exit 1
fi

BINARIES_DIR="$1"
CLUSTER_JOIN_IP="$2"
CLUSTER_TOKEN="$3"
REGISTRY_URL="$4"
REGISTRY_URL_ENDPOINT="$5"
REGISTRY_USERNAME="$6"
REGISTRY_PASSWORD="$7"
REGISTRY_TOKEN="$8"

if [ -f ${BINARIES_DIR}/scripts/node/deploy-kube-system.tmpl ]; then
  sed -e "s/{{ k8s_control_node.cluster.token }}/${CLUSTER_TOKEN}/g" -e "s/{{ k8s_control_node.join_ip }}/${CLUSTER_JOIN_IP}/g" ${BINARIES_DIR}/scripts/node/deploy-kube-system.tmpl > /opt/bin/deploy-kube-system
fi

if [ -f ${BINARIES_DIR}/scripts/node/setup-containerd.tmpl ]; then
  sed -e "s/{{registry.url}}/${REGISTRY_URL}/g" -e "s/{{registry.url.endpoint}}/${REGISTRY_URL_ENDPOINT}/g" -e "s/{{registry.username}}/${REGISTRY_USERNAME}/g" -e "s/{{registry.password}}/${REGISTRY_PASSWORD}/g" -e "s/{{registry.token}}/${REGISTRY_TOKEN}/g" ${BINARIES_DIR}/scripts/node/setup-containerd.tmpl > /opt/bin/setup-containerd
fi

!

sed -e "/- path: \/opt\/bin\/setup-kube-system/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-node.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  | sed -e '/if \[ -f "${BINARIES_DIR}\/scripts\/node\/setup-kube-system" \]; then/,/^fi/d' -e '/^while true; do/,/^done/d' -e '/if \[ "$EJECT_ISO_FROM_OS" = true \]/,/fi/d' | egrep -v "^#!|^BINARIES_DIR|^EJECT_ISO_FROM_OS|umount" >> "${SCRIPTS_DIR}/node/setup-kube-system"

## deploy-kube-system

sed -e "/- path: \/opt\/bin\/deploy-kube-system/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-node.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  > "${SCRIPTS_DIR}/node/deploy-kube-system.tmpl"

## setup-containerd
sed -e "/- path: \/opt\/bin\/setup-containerd/,/- path: /p" -e d "${PLUGIN_CONFDIR}/k8s-node.yml" | sed -e "0,/content: /d" -e "/- path: /d" -e "s/^      //"  > "${SCRIPTS_DIR}/node/setup-containerd.tmpl"
