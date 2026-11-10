// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.kubernetes.cluster.actionworkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;

/**
 * Gathers what the VM map check of the Kubernetes cluster status scanner needs from the cluster: the pattern of the
 * host names CloudStack gives the VMs it creates for the cluster, and the names of the nodes Kubernetes knows about.
 * It only reads, and changes nothing on the cluster.
 */
public class KubernetesClusterVmMapCheckWorker extends KubernetesClusterResourceModifierActionWorker {

    protected static final String LIST_NODES_COMMAND = "sudo /opt/bin/kubectl get nodes -o name";
    private static final String NODE_RESOURCE_PREFIX = "node/";
    private static final int LEGACY_MAX_CLUSTER_PREFIX_LENGTH = 40;
    /** The names end in the creation time in milliseconds in hex, 11 digits since 2004, so not one a user picks by chance. */
    private static final int MIN_NAME_SUFFIX_LENGTH = 10;

    public KubernetesClusterVmMapCheckWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    /**
     * @return the pattern matching the host names of the control, worker and etcd node VMs that CloudStack creates
     * for the cluster: {@code <prefix>-control-<hex>}, {@code <prefix>-node-<hex>} and
     * {@code <etcd prefix>-<index>-<hex>}, with the prefix truncated to 40 characters as well, as it was before
     * it was allowed 43.
     */
    public Pattern getClusterVmHostNamePattern() {
        String prefix = getKubernetesClusterNodeNamePrefix();
        String nodePrefix = Pattern.quote(prefix);
        if (prefix.length() > LEGACY_MAX_CLUSTER_PREFIX_LENGTH) {
            nodePrefix = String.format("(?:%s|%s)", nodePrefix, Pattern.quote(prefix.substring(0, LEGACY_MAX_CLUSTER_PREFIX_LENGTH)));
        }
        String etcdPrefix = Pattern.quote(getEtcdNodeNameForCluster());
        return Pattern.compile(String.format("^(?:%s-(?:control|node)|%s-\\d+)-[0-9a-f]{%d,}$",
                nodePrefix, etcdPrefix, MIN_NAME_SUFFIX_LENGTH), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Lists the nodes of the cluster with kubectl on its control node, over SSH as the other operations do.
     *
     * @return the lower case names of the nodes, or null if they could not be listed
     */
    public List<String> listKubernetesNodeNames() {
        try {
            Pair<String, Integer> sshIpPort = getKubernetesClusterServerIpSshPort(null);
            if (StringUtils.isEmpty(sshIpPort.first())) {
                logger.debug("No address to reach the control node of Kubernetes cluster {} over SSH", kubernetesCluster);
                return null;
            }
            Pair<Boolean, String> result = SshHelper.sshExecute(sshIpPort.first(), sshIpPort.second(), getLoginUser(),
                    getManagementServerSshPublicKeyFile(), null, LIST_NODES_COMMAND, 10000, 10000, 20000);
            if (!Boolean.TRUE.equals(result.first())) {
                logger.debug("Failed to list the nodes of Kubernetes cluster {}: {}", kubernetesCluster, result.second());
                return null;
            }
            // kubectl succeeded, so no node listed means the cluster has none
            return parseNodeNames(result.second());
        } catch (Exception e) {
            logger.debug("Failed to list the nodes of Kubernetes cluster {}", kubernetesCluster, e);
            return null;
        }
    }

    private String getLoginUser() {
        try {
            return getControlNodeLoginUser();
        } catch (CloudRuntimeException e) {
            // the VM of the first map entry may itself be gone, which is what the check is looking for
            return CLUSTER_NODE_VM_USER;
        }
    }

    /**
     * Parses the output of {@code kubectl get nodes -o name}, one {@code node/<name>} per line. Anything else, such as
     * a warning, is ignored.
     */
    protected static List<String> parseNodeNames(String output) {
        List<String> nodeNames = new ArrayList<>();
        if (StringUtils.isBlank(output)) {
            return nodeNames;
        }
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(NODE_RESOURCE_PREFIX) && trimmed.length() > NODE_RESOURCE_PREFIX.length()) {
                nodeNames.add(trimmed.substring(NODE_RESOURCE_PREFIX.length()).toLowerCase(Locale.ROOT));
            }
        }
        return nodeNames;
    }
}
