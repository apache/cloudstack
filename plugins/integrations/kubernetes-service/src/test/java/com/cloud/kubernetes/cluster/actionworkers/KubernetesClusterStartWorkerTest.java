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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

public class KubernetesClusterStartWorkerTest {

    private static final String CONTROL_NODE_IP = "10.1.1.10";
    private static final String SERVER_IP = "203.0.113.5";

    private static String kubeadmConfig(String controlNodeIp, String serverIp) throws IOException {
        String template;
        try (InputStream in = Objects.requireNonNull(KubernetesClusterStartWorkerTest.class.getResourceAsStream("/conf/k8s-control-node.yml"))) {
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String config = KubernetesClusterStartWorker.setKubeadmApiServerAddresses(template, controlNodeIp, serverIp)
                .replace("{{ k8s.api_server_port }}", "6443");
        int start = config.indexOf("path: /etc/kubernetes/kubeadm-config.yaml");
        Assert.assertTrue(start >= 0);
        int end = config.indexOf("- path:", start);
        Assert.assertTrue(end > start);
        return config.substring(start, end);
    }

    @Test
    public void testGetApiServerAddresses() {
        Assert.assertEquals(List.of(CONTROL_NODE_IP, SERVER_IP), KubernetesClusterStartWorker.getApiServerAddresses(CONTROL_NODE_IP, SERVER_IP));
        Assert.assertEquals(List.of(CONTROL_NODE_IP), KubernetesClusterStartWorker.getApiServerAddresses(CONTROL_NODE_IP, CONTROL_NODE_IP));
    }

    @Test
    public void testExternalEtcdKubeadmConfigCertCoversControlNodeIp() throws IOException {
        String config = kubeadmConfig(CONTROL_NODE_IP, SERVER_IP);
        Assert.assertFalse(config.contains("{{ k8s_control.server_ip") || config.contains("{{ k8s_control.node_ip"));
        Assert.assertTrue(config.contains("        certSANs: [\"" + CONTROL_NODE_IP + "\", \"" + SERVER_IP + "\"]\n"));
        Assert.assertTrue(config.contains("      controlPlaneEndpoint: " + SERVER_IP + ":6443\n"));
        Assert.assertTrue(config.contains("        advertiseAddress: " + CONTROL_NODE_IP + "\n"));
    }

    @Test
    public void testExternalEtcdKubeadmConfigSameIp() throws IOException {
        String config = kubeadmConfig(CONTROL_NODE_IP, CONTROL_NODE_IP);
        Assert.assertTrue(config.contains("        certSANs: [\"" + CONTROL_NODE_IP + "\"]\n      controlPlaneEndpoint: " + CONTROL_NODE_IP + ":6443\n"));
        Assert.assertTrue(config.contains("        advertiseAddress: " + CONTROL_NODE_IP + "\n"));
    }
}
