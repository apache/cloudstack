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

package com.cloud.kubernetes.cluster.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.SecureRandom;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.log4j.Logger;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.nio.TrustAllManager;
import com.cloud.utils.ssh.SshHelper;
import com.google.common.base.Strings;

public class KubernetesClusterUtil {

    protected static final Logger LOGGER = Logger.getLogger(KubernetesClusterUtil.class);

    public static boolean isKubernetesClusterNodeReady(final KubernetesCluster kubernetesCluster, String ipAddress, int port,
                                                       String user, File sshKeyFile, String nodeName) throws Exception {
        Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port,
                user, sshKeyFile, null,
                String.format("sudo kubectl get nodes | awk '{if ($1 == \"%s\" && $2 == \"Ready\") print $1}'", nodeName.toLowerCase()),
                10000, 10000, 20000);
        if (result.first() && nodeName.equals(result.second().trim())) {
            return true;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Failed to retrieve status for node: %s in Kubernetes cluster : %s. Output: %s", nodeName, kubernetesCluster.getName(), result.second()));
        }
        return false;
    }

    public static boolean isKubernetesClusterNodeReady(final KubernetesCluster kubernetesCluster, final String ipAddress, final int port,
                                                       final String user, final File sshKeyFile, final String nodeName,
                                                       final long timeoutTime, final int waitDuration) {
        while (System.currentTimeMillis() < timeoutTime) {
            boolean ready = false;
            try {
                ready = isKubernetesClusterNodeReady(kubernetesCluster, ipAddress, port, user, sshKeyFile, nodeName);
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to retrieve state of node: %s in Kubernetes cluster : %s", nodeName, kubernetesCluster.getName()), e);
            }
            if (ready) {
                return true;
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                LOGGER.error(String.format("Error while waiting for Kubernetes cluster : %s node: %s to become ready", kubernetesCluster.getName(), nodeName), ie);
            }
        }
        return false;
    }

    /**
     * Mark a given node in a given Kubernetes cluster as schedulable.
     * kubectl uncordon command will be called through SSH using IP address and port of the host virtual machine or load balancer.
     * Multiple retries with a given delay can be used.
     * uncordon is required when a particular node in Kubernetes cluster is drained (usually during upgrade)
     * @param kubernetesCluster
     * @param ipAddress
     * @param port
     * @param user
     * @param sshKeyFile
     * @param userVm
     * @param timeoutTime
     * @param waitDuration
     * @return
     */
    public static boolean uncordonKubernetesClusterNode(final KubernetesCluster kubernetesCluster,
                                                        final String ipAddress, final int port,
                                                        final String user, final File sshKeyFile,
                                                        final UserVm userVm, final long timeoutTime,
                                                        final int waitDuration) {
        String hostName = userVm.getHostName();
        if (!Strings.isNullOrEmpty(hostName)) {
            hostName = hostName.toLowerCase();
        }
        while (System.currentTimeMillis() < timeoutTime) {
            Pair<Boolean, String> result = null;
            try {
                result = SshHelper.sshExecute(ipAddress, port, user, sshKeyFile, null,
                        String.format("sudo kubectl uncordon %s", hostName),
                        10000, 10000, 30000);
                if (result.first()) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to uncordon node: %s on VM ID : %s in Kubernetes cluster : %s",
                    hostName, userVm.getUuid(), kubernetesCluster.getName()), e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                LOGGER.warn(String.format("Error while waiting for uncordon Kubernetes cluster : %s node: %s on VM : %s",
                    kubernetesCluster.getName(), hostName, userVm.getUuid()), ie);
            }
        }
        return false;
    }

    public static boolean isKubernetesClusterAddOnServiceRunning(final KubernetesCluster kubernetesCluster, final String ipAddress,
                                                                 final int port, final String user, final File sshKeyFile,
                                                                 final String namespace, String serviceName) {
        try {
            String cmd = "sudo kubectl get pods --all-namespaces";
            if (!Strings.isNullOrEmpty(namespace)) {
                cmd = String.format("sudo kubectl get pods --namespace=%s", namespace);
            }
            Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port, user,
                    sshKeyFile, null, cmd,
                    10000, 10000, 10000);
            if (result.first() && !Strings.isNullOrEmpty(result.second())) {
                String[] lines = result.second().split("\n");
                for (String line :
                        lines) {
                    if (line.contains(serviceName) && line.contains("Running")) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(String.format("Service : %s in namespace: %s for the Kubernetes cluster : %s is running", serviceName, namespace, kubernetesCluster.getName()));
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn(String.format("Unable to retrieve service: %s running status in namespace %s for Kubernetes cluster : %s", serviceName, namespace, kubernetesCluster.getName()), e);
        }
        return false;
    }

    public static boolean isKubernetesClusterDashboardServiceRunning(final KubernetesCluster kubernetesCluster, String ipAddress,
                                                                     final int port, final String user, final File sshKeyFile,
                                                                     final long timeoutTime, final long waitDuration) {
        boolean running = false;
        // Check if dashboard service is up running.
        while (System.currentTimeMillis() < timeoutTime) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Checking dashboard service for the Kubernetes cluster : %s to come up", kubernetesCluster.getName()));
            }
            if (isKubernetesClusterAddOnServiceRunning(kubernetesCluster, ipAddress, port, user, sshKeyFile, "kubernetes-dashboard", "kubernetes-dashboard")) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Dashboard service for the Kubernetes cluster : %s is in running state", kubernetesCluster.getName()));
                }
                running = true;
                break;
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ex) {
                LOGGER.error(String.format("Error while waiting for Kubernetes cluster: %s API dashboard service to be available", kubernetesCluster.getName()), ex);
            }
        }
        return running;
    }

    public static String getKubernetesClusterConfig(final KubernetesCluster kubernetesCluster, final String ipAddress, final int port,
                                                    final String user, final File sshKeyFile, final long timeoutTime) {
        String kubeConfig = "";
        while (System.currentTimeMillis() < timeoutTime) {
            try {
                Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port, user,
                        sshKeyFile, null, "sudo cat /etc/kubernetes/admin.conf",
                        10000, 10000, 10000);

                if (result.first() && !Strings.isNullOrEmpty(result.second())) {
                    kubeConfig = result.second();
                    break;
                } else  {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Failed to retrieve kube-config file for Kubernetes cluster : %s. Output: %s", kubernetesCluster.getName(), result.second()));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to retrieve kube-config file for Kubernetes cluster : %s", kubernetesCluster.getName()), e);
            }
        }
        return kubeConfig;
    }

    public static int getKubernetesClusterReadyNodesCount(final KubernetesCluster kubernetesCluster, final String ipAddress,
                                                          final int port, final String user, final File sshKeyFile) throws Exception {
        Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port,
                user, sshKeyFile, null,
                "sudo kubectl get nodes | awk '{if ($2 == \"Ready\") print $1}' | wc -l",
                10000, 10000, 20000);
        if (result.first()) {
            return Integer.parseInt(result.second().trim().replace("\"", ""));
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Failed to retrieve ready nodes for Kubernetes cluster : %s. Output: %s", kubernetesCluster.getName(), result.second()));
            }
        }
        return 0;
    }

    public static boolean isKubernetesClusterServerRunning(final KubernetesCluster kubernetesCluster, final String ipAddress,
                                                           final int port, final long timeoutTime, final long waitDuration) {
        boolean k8sApiServerSetup = false;
        while (System.currentTimeMillis() < timeoutTime) {
            try {
                final SSLContext sslContext = SSLUtils.getSSLContext();
                sslContext.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
                URL url = new URL(String.format("https://%s:%d/version", ipAddress, port));
                HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
                con.setSSLSocketFactory(sslContext.getSocketFactory());
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String versionOutput = br.lines().collect(Collectors.joining());
                if (!Strings.isNullOrEmpty(versionOutput)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Kubernetes cluster : %s API has been successfully provisioned, %s", kubernetesCluster.getName(), versionOutput));
                    }
                    k8sApiServerSetup = true;
                    break;
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("API endpoint for Kubernetes cluster : %s not available", kubernetesCluster.getName()), e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                LOGGER.error(String.format("Error while waiting for Kubernetes cluster : %s API endpoint to be available", kubernetesCluster.getName()), ie);
            }
        }
        return k8sApiServerSetup;
    }

    public static boolean isKubernetesClusterControlVmRunning(final KubernetesCluster kubernetesCluster, final String ipAddress,
                                                              final int port, final long timeoutTime) {
        boolean controlVmRunning = false;
        while (!controlVmRunning && System.currentTimeMillis() < timeoutTime) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ipAddress, port), 10000);
                controlVmRunning = true;
            } catch (IOException e) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Waiting for Kubernetes cluster : %s control node VMs to be accessible", kubernetesCluster.getName()));
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    LOGGER.warn(String.format("Error while waiting for Kubernetes cluster : %s control node VMs to be accessible", kubernetesCluster.getName()), ex);
                }
            }
        }
        return controlVmRunning;
    }

    public static boolean validateKubernetesClusterReadyNodesCount(final KubernetesCluster kubernetesCluster,
                                                                   final String ipAddress, final int port,
                                                                   final String user, final File sshKeyFile,
                                                                   final long timeoutTime, final long waitDuration) {
        while (System.currentTimeMillis() < timeoutTime) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Checking ready nodes for the Kubernetes cluster : %s with total %d provisioned nodes", kubernetesCluster.getName(), kubernetesCluster.getTotalNodeCount()));
            }
            try {
                int nodesCount = KubernetesClusterUtil.getKubernetesClusterReadyNodesCount(kubernetesCluster, ipAddress, port,
                        user, sshKeyFile);
                if (nodesCount == kubernetesCluster.getTotalNodeCount()) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Kubernetes cluster : %s has %d ready nodes now", kubernetesCluster.getName(), kubernetesCluster.getTotalNodeCount()));
                    }
                    return true;
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Kubernetes cluster : %s has total %d provisioned nodes while %d ready now", kubernetesCluster.getName(), kubernetesCluster.getTotalNodeCount(), nodesCount));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to retrieve ready node count for Kubernetes cluster : %s", kubernetesCluster.getName()), e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ex) {
                LOGGER.warn(String.format("Error while waiting during Kubernetes cluster : %s ready node check", kubernetesCluster.getName()), ex);
            }
        }
        return false;
    }

    public static String generateClusterToken(final KubernetesCluster kubernetesCluster) {
        String token = kubernetesCluster.getUuid();
        token = token.replaceAll("-", "");
        token = token.substring(0, 22);
        token = token.substring(0, 6) + "." + token.substring(6);
        return token;
    }

    public static String generateClusterHACertificateKey(final KubernetesCluster kubernetesCluster) {
        String uuid = kubernetesCluster.getUuid();
        StringBuilder token = new StringBuilder(uuid.replaceAll("-", ""));
        while (token.length() < 64) {
            token.append(token);
        }
        return token.toString().substring(0, 64);
    }
}
