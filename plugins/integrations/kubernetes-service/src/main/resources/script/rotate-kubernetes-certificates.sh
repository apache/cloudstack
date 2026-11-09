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

# Renews the kubeadm-managed certificates of this control node and restarts its
# control plane static pods so they load them. The certificate authorities are
# left alone. Safe to rerun: an interrupted earlier run is recovered first.
#
# Usage: ./rotate-kubernetes-certificates.sh [WAIT_TIMEOUT_SECONDS] [rescue]
#
# rescue is for a cluster whose API server isn't answering, e.g. because its
# certificates have expired. With stacked etcd and several control nodes, one
# renewed node can't get etcd quorum until the others are renewed as well, so
# this node only waits for its static pods to be back, not for them to be
# healthy: without quorum the API server keeps restarting until it gets one.

WAIT_TIMEOUT="${1:-300}"
RESCUE="${2:-}"

# the binaries CKS installs come first, ahead of any the template ships
export PATH=/opt/bin:$PATH:/usr/local/bin:/usr/sbin:/sbin
CRI_ENDPOINT=unix:///run/containerd/containerd.sock

MANIFESTS_DIR=/etc/kubernetes/manifests
HOLD_DIR=/etc/kubernetes/manifests-cks-certificate-rotation
COMPONENTS="kube-apiserver kube-controller-manager kube-scheduler etcd"
API_SERVER_PORT=6443
# kubelet looks at the static pod manifests every 20 seconds by default
KUBELET_FILE_CHECK_WAIT=25

log() {
  echo "$(date -u '+%Y-%m-%dT%H:%M:%SZ') $*"
}

# Puts back any manifest moved out by this script, also when it fails half-way,
# so the control plane is never left without its static pods
restore_manifests() {
  if [ ! -d "${HOLD_DIR}" ]; then
    return 0
  fi
  for manifest in "${HOLD_DIR}"/*.yaml; do
    [ -e "${manifest}" ] || continue
    if [ -e "${MANIFESTS_DIR}/$(basename "${manifest}")" ]; then
      rm -f "${manifest}"
    else
      mv "${manifest}" "${MANIFESTS_DIR}/"
    fi
  done
  rmdir "${HOLD_DIR}" 2>/dev/null || true
}
trap restore_manifests EXIT

# Keeps every distinct earlier /root/.kube/config: an existing backup is never overwritten
backup_root_kubeconfig() {
  local backup=/root/.kube/config.cks-backup
  if [ -e "${backup}" ]; then
    cmp -s /root/.kube/config "${backup}" && return 0
    backup="${backup}.$(date +%s)"
  fi
  cp -p /root/.kube/config "${backup}"
}

container_running() {
  [ -n "$(crictl --runtime-endpoint "${CRI_ENDPOINT}" ps -q --name "^${1}\$" 2>/dev/null)" ]
}

# A new sandbox of the static pod is up, not one torn down by this run; its
# containers may still be restarting
pod_ready() {
  local id
  for id in $(crictl --runtime-endpoint "${CRI_ENDPOINT}" pods -q --state ready --name "^${1}-" 2>/dev/null); do
    case " ${old_pods} " in
      *" ${id} "*) ;;
      *) return 0 ;;
    esac
  done
  return 1
}

endpoint_healthy() {
  [ "$(curl -sk --max-time 5 "$1" 2>/dev/null)" = "ok" ]
}

served_certificate_current() {
  if ! command -v openssl >/dev/null 2>&1; then
    return 0
  fi
  local expected served
  expected=$(openssl x509 -noout -enddate -in /etc/kubernetes/pki/apiserver.crt 2>/dev/null)
  served=$(echo | openssl s_client -connect 127.0.0.1:${API_SERVER_PORT} 2>/dev/null | openssl x509 -noout -enddate 2>/dev/null)
  [ -n "${expected}" ] && [ "${expected}" = "${served}" ]
}

# Returns non-zero if the API server isn't ready within the given number of seconds
wait_for_api_server() {
  local limit=$(( $(date +%s) + $1 ))
  [ "${limit}" -gt "${deadline}" ] && limit=${deadline}
  while ! endpoint_healthy "https://127.0.0.1:${API_SERVER_PORT}/readyz"; do
    if [ "$(date +%s)" -ge "${limit}" ]; then
      return 1
    fi
    sleep 3
  done
}

deadline=$(( $(date +%s) + WAIT_TIMEOUT ))

restore_manifests

if [ ! -f /etc/kubernetes/admin.conf ] || [ ! -f "${MANIFESTS_DIR}/kube-apiserver.yaml" ]; then
  log "ERROR: this is not a kubeadm control plane node"
  exit 1
fi

# The static pods must be stopped and confirmed gone before their manifests come back
if ! crictl --runtime-endpoint "${CRI_ENDPOINT}" ps -q >/dev/null 2>&1; then
  log "ERROR: unable to list the containers with crictl on ${CRI_ENDPOINT}, so the control plane pods restart can't be confirmed"
  exit 1
fi

# kubeadm reads its configuration from the cluster and silently falls back to
# defaults when the API server is down, e.g. right after an interrupted run, so
# give it time to come up. Carry on if it doesn't: once the certificates have
# expired it never becomes ready, and renewing is the way out.
if [ "${RESCUE}" = "rescue" ]; then
  log "Rescue mode: renewing without waiting for the API server"
elif ! wait_for_api_server 180; then
  log "WARNING: the API server is not ready, renewing with the kubeadm configuration it can read"
fi

# root's kubeconfig is replaced with admin.conf below; note now, before kubeadm
# renews admin.conf, whether it is something else worth a backup
root_kubeconfig_custom=false
if [ -f /root/.kube/config ] && ! cmp -s /etc/kubernetes/admin.conf /root/.kube/config; then
  root_kubeconfig_custom=true
fi

log "Renewing the control plane certificates"
apiserver_before=$(sha256sum /etc/kubernetes/pki/apiserver.crt | cut -d' ' -f1)
admin_before=$(sha256sum /etc/kubernetes/admin.conf | cut -d' ' -f1)
# under -e a failing command substitution would exit before kubeadm's error is printed
if ! renew_output=$(kubeadm certs renew all 2>&1); then
  echo "${renew_output}"
  log "ERROR: kubeadm certs renew all failed"
  exit 1
fi
echo "${renew_output}"
# kubeadm exits 0 without renewing anything signed by a CA whose key it doesn't have (external CA mode)
if echo "${renew_output}" | grep -q "can't be renewed" || \
   [ "$(sha256sum /etc/kubernetes/pki/apiserver.crt | cut -d' ' -f1)" = "${apiserver_before}" ] || \
   [ "$(sha256sum /etc/kubernetes/admin.conf | cut -d' ' -f1)" = "${admin_before}" ]; then
  log "ERROR: kubeadm did not renew the control plane certificates, are they signed by an external CA?"
  exit 1
fi

# CKS runs kubectl as root with /root/.kube/config, a copy of admin.conf
mkdir -p /root/.kube
if [ "${root_kubeconfig_custom}" = true ]; then
  backup_root_kubeconfig
fi
cp /etc/kubernetes/admin.conf /root/.kube/config
chmod 600 /root/.kube/config

log "Restarting the control plane static pods"
old_pods=$(crictl --runtime-endpoint "${CRI_ENDPOINT}" pods -q 2>/dev/null | tr '\n' ' ')
mkdir -p "${HOLD_DIR}"
moved=""
for component in ${COMPONENTS}; do
  if [ -f "${MANIFESTS_DIR}/${component}.yaml" ]; then
    mv "${MANIFESTS_DIR}/${component}.yaml" "${HOLD_DIR}/"
    moved="${moved} ${component}"
  fi
done

sleep ${KUBELET_FILE_CHECK_WAIT}
for component in ${moved}; do
  while container_running "${component}"; do
    if [ "$(date +%s)" -ge "${deadline}" ]; then
      log "ERROR: timed out waiting for ${component} to stop"
      exit 1
    fi
    sleep 2
  done
done

restore_manifests

if [ "${RESCUE}" = "rescue" ]; then
  log "Rescue mode: waiting for the control plane pods to be back"
  while true; do
    # control nodes of a cluster with separate etcd nodes run no etcd of their own
    if { [ ! -f "${MANIFESTS_DIR}/etcd.yaml" ] || container_running etcd; } && \
       pod_ready kube-apiserver && pod_ready kube-controller-manager && pod_ready kube-scheduler; then
      break
    fi
    if [ "$(date +%s)" -ge "${deadline}" ]; then
      log "ERROR: timed out waiting for the control plane pods to come back"
      exit 1
    fi
    sleep 3
  done
  log "Control plane certificates renewed, the control plane becomes healthy once every control node is renewed"
  exit 0
fi

log "Waiting for the control plane to become healthy"
while true; do
  if endpoint_healthy "https://127.0.0.1:${API_SERVER_PORT}/readyz" && \
     endpoint_healthy "https://127.0.0.1:10257/healthz" && \
     endpoint_healthy "https://127.0.0.1:10259/healthz" && \
     served_certificate_current; then
    break
  fi
  if [ "$(date +%s)" -ge "${deadline}" ]; then
    log "ERROR: timed out waiting for the control plane to become healthy"
    exit 1
  fi
  sleep 3
done

log "Control plane certificates renewed"
