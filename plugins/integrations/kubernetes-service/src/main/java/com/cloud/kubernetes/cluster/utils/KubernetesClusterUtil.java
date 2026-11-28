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
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.nio.TrustAllManager;
import com.cloud.utils.ssh.SshHelper;

public class KubernetesClusterUtil {

    protected static Logger LOGGER = LogManager.getLogger(KubernetesClusterUtil.class);

    public static final String CLUSTER_NODE_READY_COMMAND = "sudo /opt/bin/kubectl get nodes | awk '{if ($1 == \"%s\" && $2 == \"Ready\") print $1}'";
    public static final String CLUSTER_NODE_VERSION_COMMAND = "sudo /opt/bin/kubectl get nodes | awk '{if ($1 == \"%s\") print $5}'";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] BOOTSTRAP_TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final Pattern JOIN_TOKEN = Pattern.compile("--token\\s+([a-z0-9]{6}\\.[a-z0-9]{16})(?!\\S)");
    private static final Pattern JOIN_ENDPOINT = Pattern.compile("kubeadm join ([0-9.]+):[0-9]+(?!\\S)");
    private static final Pattern JOIN_CA_CERT_HASH = Pattern.compile("--discovery-token-ca-cert-hash\\s+(sha256:[0-9a-f]{64})(?!\\S)");

    public static boolean isKubernetesClusterNodeReady(final KubernetesCluster kubernetesCluster, String ipAddress, int port,
                                                       String user, File sshKeyFile, String nodeName) throws Exception {
        Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port,
                user, sshKeyFile, null,
                String.format(CLUSTER_NODE_READY_COMMAND, nodeName.toLowerCase()),
                10000, 10000, 20000);
        if (result.first() && nodeName.equals(result.second().trim())) {
            return true;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Failed to retrieve status for node: %s in Kubernetes cluster: %s. Output: %s", nodeName, kubernetesCluster, result.second()));
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
                LOGGER.warn(String.format("Failed to retrieve state of node: %s in Kubernetes cluster: %s", nodeName, kubernetesCluster), e);
            }
            if (ready) {
                return true;
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                LOGGER.error(String.format("Error while waiting for Kubernetes cluster: %s node: %s to become ready", kubernetesCluster, nodeName), ie);
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
        if (StringUtils.isNotEmpty(hostName)) {
            hostName = hostName.toLowerCase();
        }
        while (System.currentTimeMillis() < timeoutTime) {
            Pair<Boolean, String> result = null;
            try {
                result = SshHelper.sshExecute(ipAddress, port, user, sshKeyFile, null,
                        String.format("sudo /opt/bin/kubectl uncordon %s", hostName),
                        10000, 10000, 30000);
                if (result.first()) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to uncordon node: %s on VM %s in Kubernetes cluster %s",
                    hostName, userVm, kubernetesCluster), e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                LOGGER.warn(String.format("Error while waiting for uncordon Kubernetes cluster %s node: %s on VM %s",
                    kubernetesCluster, hostName, userVm), ie);
            }
        }
        return false;
    }

    public static boolean isKubernetesClusterAddOnServiceRunning(final KubernetesCluster kubernetesCluster, final String ipAddress,
                                                                 final int port, final String user, final File sshKeyFile,
                                                                 final String namespace, String serviceName) {
        try {
            String cmd = "sudo /opt/bin/kubectl get pods --all-namespaces";
            if (StringUtils.isNotEmpty(namespace)) {
                cmd = String.format("sudo /opt/bin/kubectl get pods --namespace=%s", namespace);
            }
            Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port, user,
                    sshKeyFile, null, cmd,
                    10000, 10000, 10000);
            if (result.first() && StringUtils.isNotEmpty(result.second())) {
                String[] lines = result.second().split("\n");
                for (String line :
                        lines) {
                    if (line.contains(serviceName) && line.contains("Running")) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(String.format("Service %s in namespace %s for the Kubernetes cluster %s is running", serviceName, namespace, kubernetesCluster));
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn(String.format("Unable to retrieve service: %s running status in namespace %s for Kubernetes cluster %s", serviceName, namespace, kubernetesCluster), e);
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
                LOGGER.debug(String.format("Checking dashboard service for the Kubernetes cluster: %s to come up", kubernetesCluster));
            }
            if (isKubernetesClusterAddOnServiceRunning(kubernetesCluster, ipAddress, port, user, sshKeyFile, "kubernetes-dashboard", "kubernetes-dashboard")) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Dashboard service for the Kubernetes cluster %s is in running state", kubernetesCluster));
                }
                running = true;
                break;
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ex) {
                LOGGER.error(String.format("Error while waiting for Kubernetes cluster %s API dashboard service to be available", kubernetesCluster), ex);
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
                        sshKeyFile, null, "sudo cat /etc/kubernetes/user.conf 2>/dev/null || sudo cat /etc/kubernetes/admin.conf",
                        10000, 10000, 10000);

                if (result.first() && StringUtils.isNotEmpty(result.second())) {
                    kubeConfig = result.second();
                    break;
                } else  {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Failed to retrieve kube-config file for Kubernetes cluster: %s. Output: %s", kubernetesCluster, result.second()));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to retrieve kube-config file for Kubernetes cluster %s", kubernetesCluster), e);
            }
        }
        return kubeConfig;
    }

    public static int getKubernetesClusterReadyNodesCount(final KubernetesCluster kubernetesCluster, final String ipAddress,
                                                          final int port, final String user, final File sshKeyFile) throws Exception {
        Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port,
                user, sshKeyFile, null,
                "sudo /opt/bin/kubectl get nodes | grep -w 'Ready' | wc -l",
                10000, 10000, 20000);
        if (Boolean.TRUE.equals(result.first())) {
            return Integer.parseInt(result.second().trim().replace("\"", "")) + kubernetesCluster.getEtcdNodeCount().intValue();
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Failed to retrieve ready nodes for Kubernetes cluster %s. Output: %s", kubernetesCluster, result.second()));
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
                if (StringUtils.isNotEmpty(versionOutput)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Kubernetes cluster %s API has been successfully provisioned, %s", kubernetesCluster, versionOutput));
                    }
                    k8sApiServerSetup = true;
                    break;
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("API endpoint for Kubernetes cluster %s not available", kubernetesCluster), e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                LOGGER.error(String.format("Error while waiting for Kubernetes cluster %s API endpoint to be available", kubernetesCluster), ie);
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
                LOGGER.info("Waiting for Kubernetes cluster {} control node VMs to be accessible", kubernetesCluster);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    LOGGER.warn("Error while waiting for Kubernetes cluster {} control node VMs to be accessible", kubernetesCluster, ex);
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
                LOGGER.debug(String.format("Checking ready nodes for the Kubernetes cluster %s with total %d provisioned nodes", kubernetesCluster, kubernetesCluster.getTotalNodeCount()));
            }
            try {
                int nodesCount = KubernetesClusterUtil.getKubernetesClusterReadyNodesCount(kubernetesCluster, ipAddress, port,
                        user, sshKeyFile);
                if (nodesCount == kubernetesCluster.getTotalNodeCount()) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Kubernetes cluster %s has %d ready nodes now", kubernetesCluster, kubernetesCluster.getTotalNodeCount()));
                    }
                    return true;
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Kubernetes cluster %s has total %d provisioned nodes while %d ready now", kubernetesCluster, kubernetesCluster.getTotalNodeCount(), nodesCount));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to retrieve ready node count for Kubernetes cluster %s", kubernetesCluster), e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ex) {
                LOGGER.warn(String.format("Error while waiting during Kubernetes cluster %s ready node check", kubernetesCluster), ex);
            }
        }
        return false;
    }

    public static String generateBootstrapToken() {
        return RandomStringUtils.random(6, 0, 0, false, false, BOOTSTRAP_TOKEN_CHARS, RANDOM) + "." +
                RandomStringUtils.random(16, 0, 0, false, false, BOOTSTRAP_TOKEN_CHARS, RANDOM);
    }

    /**
     * kubeadm treats a TTL of 0 as never expiring, so never pass it one.
     */
    public static String getJoinTokenTtl(final long ttlSeconds) {
        return Math.max(ttlSeconds, 1) + "s";
    }

    public static String generateCertificateKey() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        return Hex.encodeHexString(key);
    }

    public static String getCaCertHash(final PublicKey caPublicKey) {
        return "sha256:" + DigestUtils.sha256Hex(caPublicKey.getEncoded());
    }

    /**
     * Parses the API server address from the output of kubeadm token create --print-join-command.
     * kubeadm prints the controlPlaneEndpoint if the cluster has one, else the advertise address,
     * so the API server certificate is valid for it.
     * @return the IPv4 address, or null if there is none
     */
    public static String parseJoinEndpointIp(final String joinCommand) {
        if (StringUtils.isBlank(joinCommand)) {
            return null;
        }
        Matcher endpoint = JOIN_ENDPOINT.matcher(joinCommand);
        if (!endpoint.find() || !NetUtils.isValidIp4(endpoint.group(1))) {
            return null;
        }
        return endpoint.group(1);
    }

    /**
     * Parses the output of openssl x509 -checkip.
     * @return whether the certificate is valid for the IP, or null if the output does not say
     */
    public static Boolean parseCertificateCheckIp(final String output, final String ip) {
        if (StringUtils.isBlank(output)) {
            return null;
        }
        if (output.contains(String.format("IP %s does NOT match certificate", ip))) {
            return false;
        }
        return output.contains(String.format("IP %s does match certificate", ip)) ? true : null;
    }

    /**
     * Parses the output of kubeadm token create --print-join-command.
     * @return the token and the CA certificate hash, or null if either is missing
     */
    public static Pair<String, String> parseJoinCommand(final String joinCommand) {
        if (StringUtils.isBlank(joinCommand)) {
            return null;
        }
        Matcher token = JOIN_TOKEN.matcher(joinCommand);
        Matcher hash = JOIN_CA_CERT_HASH.matcher(joinCommand);
        if (!token.find() || !hash.find()) {
            return null;
        }
        return new Pair<>(token.group(1), hash.group(1));
    }

    public static boolean clusterNodeVersionMatches(final String version,
                                                    final String ipAddress, final int port,
                                                    final String user, final File sshKeyFile,
                                                    final String hostName,
                                                    final long timeoutTime, final long waitDuration, final long vmId, KubernetesClusterVmMapDao vmMapDao) {
        int retry = 10;
        while (System.currentTimeMillis() < timeoutTime && retry-- > 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Checking if the Kubernetes version of cluster node %s is %s", hostName, version));
            }
            try {
                Pair<Boolean, String> result = SshHelper.sshExecute(
                        ipAddress, port,
                        user, sshKeyFile, null,
                        String.format(CLUSTER_NODE_VERSION_COMMAND, hostName.toLowerCase()),
                        10000, 10000, 20000);
                Pair<Boolean, String> clusterVersionMatchesAndValue = clusterNodeVersionMatches(result, version);
                if (Boolean.TRUE.equals(clusterVersionMatchesAndValue.first())) {
                    KubernetesClusterVmMapVO vmMapVO = vmMapDao.getClusterMapFromVmId(vmId);
                    String newNodeVersion = clusterVersionMatchesAndValue.second();
                    LOGGER.debug(String.format("Updating node %s Kubernetes version to %s", hostName, newNodeVersion));
                    vmMapVO.setNodeVersion(newNodeVersion);
                    vmMapDao.update(vmMapVO.getId(), vmMapVO);
                    return true;
                }
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Failed to retrieve Kubernetes version from cluster node : %s due to exception", hostName), e);
                }
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ex) {
                LOGGER.warn(String.format("Error while waiting during Kubernetes version check of cluster node : %s", hostName), ex);
            }
        }
        return false;
    }

    protected static Pair<Boolean, String> clusterNodeVersionMatches(final Pair<Boolean, String> result, final String version) {
        if (result == null || Boolean.FALSE.equals(result.first()) || StringUtils.isBlank(result.second())) {
            return new Pair<>(false, null);
        }
        String response = result.second();
        return new Pair<>(response.contains(String.format("v%s", version)), response);
    }
}
