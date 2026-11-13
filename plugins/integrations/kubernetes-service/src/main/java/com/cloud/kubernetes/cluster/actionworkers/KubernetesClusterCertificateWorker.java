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

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

import com.cloud.alert.AlertManager;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil.CertificateExpiry;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil.CertificateExpiryReport;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionManagerImpl;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;

/**
 * Checks the expiry of, and renews, the kubeadm-managed control plane certificates of a CKS cluster.
 * <p>
 * Renewal runs {@code kubeadm certs renew all} on one control node at a time, restarts that node's control plane
 * static pods so they load the new certificates and waits for its API server and the node to be ready before moving
 * to the next one. It stops at the first control node that fails. The certificate authorities are not rotated.
 * <p>
 * External etcd nodes created by CKS serve etcd over plain HTTP and hold no certificates, so there is nothing to
 * renew on them; only control nodes are touched.
 */
public class KubernetesClusterCertificateWorker extends KubernetesClusterActionWorker {

    public static final String ROTATION_SCRIPT_FILENAME = "rotate-kubernetes-certificates.sh";

    /** Epoch millis at which the expiry was last checked. */
    public static final String CERTIFICATE_CHECK_DETAIL = "certificateExpiryLastCheck";
    /** Epoch millis of the last alert that no control node could be checked; removed once a check works again. */
    public static final String CERTIFICATE_CHECK_FAILED_ALERT_DETAIL = "certificateExpiryCheckFailedAlert";
    /** Alert state of the first leaf certificate to expire, see {@link KubernetesClusterCertificateUtil#buildAlertState}. */
    public static final String CERTIFICATE_ALERT_DETAIL = "certificateExpiryAlert";
    /** Alert state of the first certificate authority to expire. */
    public static final String CA_CERTIFICATE_ALERT_DETAIL = "caCertificateExpiryAlert";
    /** Epoch millis at which the running rotation started; removed when it ends. */
    public static final String ROTATION_STARTED_DETAIL = "certificateRotationStarted";
    /** ID of the management server running the rotation; removed when it ends. */
    public static final String ROTATION_OWNER_DETAIL = "certificateRotationOwner";
    /** Epoch millis of the last failed rotation, used to back off automatic renewal. */
    public static final String ROTATION_FAILED_DETAIL = "certificateRotationLastFailure";
    /** Epoch millis at which the scanner first saw a rotation that had not recorded its start yet. */
    public static final String ROTATION_SEEN_DETAIL = "certificateRotationFirstSeen";
    /** Epoch millis of the last successful rotation, so a queued automatic renewal doesn't renew again. */
    public static final String ROTATION_SUCCEEDED_DETAIL = "certificateRotationLastSuccess";

    protected static final int NODE_READY_POLL_INTERVAL_MS = 5000;
    private static final Set<String> CLIENT_ONLY_KUBECONFIGS = Set.of("admin.conf", "super-admin.conf",
            KubernetesClusterCertificateUtil.ROOT_KUBECONFIG_ENTRY_NAME);
    private static final String CHECK_OUTPUT_SEPARATOR = "----- cks root kubeconfig -----";
    private static final String USER_KUBECONFIG_SEPARATOR = "----- cks user kubeconfig -----";

    /** Clusters whose rotation runs in this management server. */
    private static final Set<Long> ACTIVE_ROTATIONS = ConcurrentHashMap.newKeySet();

    @Inject
    protected AlertManager alertManager;

    private File rotationScriptFile;
    private long rotationTimeoutTime;
    private boolean automatic = false;
    private boolean rescueMode = false;
    private String rotationStartedAt;
    private boolean startedFromAlert = false;

    public KubernetesClusterCertificateWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    /** Marks a rotation started by the expiry check rather than by the API, which records its own event. */
    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }

    /** @return true if this management server is running a rotation of the cluster right now */
    public static boolean isRotationActive(long kubernetesClusterId) {
        return ACTIVE_ROTATIONS.contains(kubernetesClusterId);
    }

    /**
     * Only the SSH key is needed here; the templates and owner that the other workers load in their init() are not,
     * and loading them fails for a cluster whose template has since been removed.
     */
    @Override
    protected void init() {
        this.sshKeyFile = getManagementServerSshPublicKeyFile();
    }

    public static boolean isCertificateManagementSupported(KubernetesSupportedVersion version) {
        return version != null && KubernetesVersionManagerImpl.compareSemanticVersions(version.getSemanticVersion(),
                KubernetesClusterCertificateUtil.MIN_KUBERNETES_VERSION_CERTIFICATES_SUPPORT) >= 0;
    }

    protected void resolveClusterEndpoints() {
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();
        apiPublicIpAddress = resolveApiPublicIpAddress(networkDao.findById(kubernetesCluster.getNetworkId()));
        if (StringUtils.isEmpty(publicIpAddress)) {
            throw new CloudRuntimeException(String.format("Unable to retrieve the SSH address of Kubernetes cluster %s", kubernetesCluster.getName()));
        }
    }

    protected Pair<Boolean, String> runOnNode(String address, int port, String command, int timeoutMs) throws Exception {
        return SshHelper.sshExecute(address, port, getControlNodeLoginUser(), sshKeyFile, null, command, 10000, 10000, timeoutMs);
    }

    /////////////////////////////////////////////////////
    //////////////// Expiry check ///////////////////////
    /////////////////////////////////////////////////////

    /**
     * Runs {@code kubeadm certs check-expiration} on every control node and reads the client certificate of root's
     * kubeconfig, which CKS itself uses for kubectl. Nodes that can't be checked are listed in the report.
     * Also refreshes the stored kubeConfigData when its admin certificate expires before the one on the cluster,
     * e.g. after the certificates were renewed by hand.
     */
    public CertificateExpiryReport checkCertificateExpiry() {
        init();
        resolveClusterEndpoints();
        CertificateExpiryReport report = new CertificateExpiryReport();
        List<UserVm> controlVms = getControlNodeVMs();
        Date firstNodeAdminExpiry = null;
        boolean firstNodeUnreachable = false;
        boolean userKubeConfig = false;
        Date userKubeConfigExpiry = null;
        for (int i = 0; i < controlVms.size(); i++) {
            UserVm vm = controlVms.get(i);
            Pair<String, Integer> endpoint = getControlNodeSshEndpoint(vm, i);
            try {
                // one SSH session per node: kubeadm's exit status, then the certificate lines of root's kubeconfig and of user.conf
                String command = String.format("%s; rc=$?; echo '%s'; %s; echo '%s'; %s; exit $rc", KubernetesClusterCertificateUtil.CHECK_EXPIRATION_COMMAND,
                        CHECK_OUTPUT_SEPARATOR, KubernetesClusterCertificateUtil.ROOT_KUBECONFIG_CERTIFICATE_COMMAND,
                        USER_KUBECONFIG_SEPARATOR, KubernetesClusterCertificateUtil.USER_KUBECONFIG_CERTIFICATE_COMMAND);
                Pair<Boolean, String> result = runOnNode(endpoint.first(), endpoint.second(), command, 60000);
                String output = StringUtils.defaultString(result.second());
                String kubeadmOutput = StringUtils.substringBefore(output, CHECK_OUTPUT_SEPARATOR);
                List<CertificateExpiry> entries = KubernetesClusterCertificateUtil.parseCheckExpirationOutput(kubeadmOutput);
                if (!result.first() || entries.isEmpty()) {
                    logger.warn("Unable to check the certificate expiry on control node {} of Kubernetes cluster {}: {}", vm, kubernetesCluster, kubeadmOutput);
                    report.addUncheckedNode(vm.getHostName());
                    if (i == 0) {
                        firstNodeUnreachable = true;
                    }
                    continue;
                }
                String kubeConfigOutput = StringUtils.substringAfter(output, CHECK_OUTPUT_SEPARATOR);
                String userKubeConfigOutput = StringUtils.substringAfter(kubeConfigOutput, USER_KUBECONFIG_SEPARATOR);
                if (i == 0 && userKubeConfigOutput.contains(KubernetesClusterCertificateUtil.USER_KUBECONFIG_PRESENT)) {
                    userKubeConfig = true;
                    userKubeConfigExpiry = KubernetesClusterCertificateUtil.getKubeConfigClientCertificateExpiry(userKubeConfigOutput);
                }
                Date rootKubeConfigExpiry = KubernetesClusterCertificateUtil.getKubeConfigClientCertificateExpiry(
                        StringUtils.substringBefore(kubeConfigOutput, USER_KUBECONFIG_SEPARATOR));
                if (rootKubeConfigExpiry != null) {
                    // a copy of admin.conf: renewable only when admin.conf is (not in external CA mode)
                    boolean adminConfExternallyManaged = entries.stream().anyMatch(entry -> "admin.conf".equals(entry.getName()) && entry.isExternallyManaged());
                    entries.add(new CertificateExpiry(KubernetesClusterCertificateUtil.ROOT_KUBECONFIG_ENTRY_NAME, rootKubeConfigExpiry, false,
                            adminConfExternallyManaged));
                }
                // the kubeconfig is read from the first control node, so that is the one to compare with
                if (i == 0) {
                    for (CertificateExpiry entry : entries) {
                        if ("admin.conf".equals(entry.getName()) && !entry.isMissing()) {
                            firstNodeAdminExpiry = entry.getExpires();
                        }
                    }
                }
                report.add(vm.getHostName(), entries);
            } catch (Exception e) {
                logger.warn("Unable to check the certificate expiry on control node {} of Kubernetes cluster {}", vm, kubernetesCluster, e);
                report.addUncheckedNode(vm.getHostName());
                if (i == 0) {
                    firstNodeUnreachable = true;
                }
            }
        }
        // the kubeconfig is read from the first control node; don't spend the refresh timeout on it when it is down
        // and from user.conf rather than admin.conf when the operator put one there
        if (!firstNodeUnreachable) {
            refreshStaleKubeConfigData(userKubeConfig ? userKubeConfigExpiry : firstNodeAdminExpiry);
        }
        return report;
    }

    protected Date getStoredKubeConfigExpiry() {
        KubernetesClusterDetailsVO detail = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), KUBE_CONFIG_DATA_DETAIL);
        if (detail == null || StringUtils.isEmpty(detail.getValue())) {
            return null;
        }
        return KubernetesClusterCertificateUtil.getKubeConfigClientCertificateExpiry(new String(Base64.decodeBase64(detail.getValue()),
                com.cloud.utils.StringUtils.getPreferredCharset()));
    }

    protected void refreshStaleKubeConfigData(Date clusterKubeConfigExpiry) {
        if (clusterKubeConfigExpiry == null) {
            return;
        }
        Date storedExpiry = getStoredKubeConfigExpiry();
        if (storedExpiry == null || !storedExpiry.before(clusterKubeConfigExpiry)) {
            return;
        }
        logger.info("The stored kubeconfig of Kubernetes cluster {} expires on {}, before the kubeconfig on the cluster ({}), refreshing it",
                kubernetesCluster, storedExpiry, clusterKubeConfigExpiry);
        if (!refreshKubeConfigData(System.currentTimeMillis() + KUBE_CONFIG_REFRESH_TIMEOUT_MS)) {
            logger.warn("Unable to refresh the stored kubeconfig of Kubernetes cluster {}", kubernetesCluster);
        }
    }

    /////////////////////////////////////////////////////
    //////////////// Rotation ///////////////////////////
    /////////////////////////////////////////////////////

    /**
     * Renews the control plane certificates, one control node at a time, then refreshes the stored kubeconfig.
     * The cluster must be Running, or Alert to retry a failed rotation; it is RotatingCertificates meanwhile, which keeps every other cluster operation out.
     * On failure the cluster moves to Alert, an alert is raised and a CloudRuntimeException is thrown.
     */
    public boolean rotateCertificates() throws CloudRuntimeException {
        init();
        KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (!isCertificateManagementSupported(version)) {
            throw new CloudRuntimeException(String.format("Certificate rotation of Kubernetes cluster %s needs Kubernetes %s or later",
                    kubernetesCluster.getName(), KubernetesClusterCertificateUtil.MIN_KUBERNETES_VERSION_CERTIFICATES_SUPPORT));
        }
        // Marked active before the transition, so the scanner never takes it for an interrupted rotation
        if (!ACTIVE_ROTATIONS.add(kubernetesCluster.getId())) {
            throw new CloudRuntimeException(String.format("The certificates of Kubernetes cluster %s are already being rotated", kubernetesCluster.getName()));
        }
        try {
            KubernetesCluster before = kubernetesClusterDao.findById(kubernetesCluster.getId());
            startedFromAlert = before != null && KubernetesCluster.State.Alert.equals(before.getState());
            // The transition only succeeds from Running or Alert, so it also keeps out any other operation already in progress
            if (!stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.CertificateRotationRequested)) {
                KubernetesCluster current = kubernetesClusterDao.findById(kubernetesCluster.getId());
                throw new CloudRuntimeException(String.format("Unable to rotate the certificates of Kubernetes cluster %s, it is in state %s and must be Running or Alert",
                        kubernetesCluster.getName(), current == null ? null : current.getState()));
            }
            return doRotateCertificates();
        } finally {
            ACTIVE_ROTATIONS.remove(kubernetesCluster.getId());
        }
    }

    private boolean doRotateCertificates() {
        rotationStartedAt = String.valueOf(System.currentTimeMillis());
        kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), ROTATION_STARTED_DETAIL, rotationStartedAt, false);
        kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), ROTATION_OWNER_DETAIL, String.valueOf(ManagementServerNode.getManagementServerId()), false);
        logger.info("Rotating the control plane certificates of Kubernetes cluster {}", kubernetesCluster);
        rotationTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterCertificateRotationTimeout.value() * 1000;
        try {
            resolveClusterEndpoints();
            List<UserVm> controlVms = getControlNodeVMs();
            if (CollectionUtils.isEmpty(controlVms)) {
                failRotation(String.format("Unable to find the control nodes of Kubernetes cluster %s", kubernetesCluster.getName()), null);
            }
            rotationScriptFile = retrieveScriptFile(ROTATION_SCRIPT_FILENAME);
            // With expired certificates, stacked etcd on a renewed node can't get quorum until the others are renewed
            // too, so renew them all before waiting for the cluster. Only then: it restarts every control node at once.
            rescueMode = areCertificatesExpired(controlVms);
            if (rescueMode) {
                logger.warn("Certificates of Kubernetes cluster {} have expired, renewing the certificates on every control node before waiting for it",
                        kubernetesCluster);
            }
            for (int i = 0; i < controlVms.size(); i++) {
                UserVm vm = controlVms.get(i);
                if (System.currentTimeMillis() > rotationTimeoutTime) {
                    failRotation(String.format("Certificate rotation of Kubernetes cluster %s timed out before control node %s",
                            kubernetesCluster.getName(), vm.getHostName()), null);
                }
                if (!renewCertificatesOnControlNode(vm, i)) {
                    failRotation(String.format("Failed to renew the certificates on control node %s of Kubernetes cluster %s, " +
                                    "the remaining control nodes were left untouched", vm.getHostName(), kubernetesCluster.getName()), null);
                }
                if (!rescueMode && !waitForControlNodeReady(vm)) {
                    failRotation(String.format("Control node %s of Kubernetes cluster %s did not become ready after its certificates were renewed, " +
                                    "the remaining control nodes were left untouched", vm.getHostName(), kubernetesCluster.getName()), null);
                }
                logger.info("Renewed the certificates on control node {} of Kubernetes cluster {}", vm, kubernetesCluster);
            }
            if (rescueMode) {
                for (UserVm vm : controlVms) {
                    if (!waitForControlNodeReady(vm)) {
                        failRotation(String.format("Renewed the certificates on every control node of Kubernetes cluster %s, but control node %s did not become ready",
                                kubernetesCluster.getName(), vm.getHostName()), null);
                    }
                }
            }
            // a budget of its own: the certificates are renewed by now, even if that used up the rotation timeout
            if (!refreshKubeConfigData(System.currentTimeMillis() + KUBE_CONFIG_REFRESH_TIMEOUT_MS)) {
                failRotation(String.format("Renewed the certificates of Kubernetes cluster %s but failed to refresh its stored kubeconfig",
                        kubernetesCluster.getName()), null);
            }
        } catch (RotationFailedException e) {
            throw e;
        } catch (Exception e) {
            failRotation(String.format("Failed to rotate the certificates of Kubernetes cluster %s: %s", kubernetesCluster.getName(), e.getMessage()), e);
        } finally {
            if (rotationScriptFile != null) {
                rotationScriptFile.delete();
            }
        }
        if (isStillOwnRotation()) {
            // transition first, so the certificate scanner never sees RotatingCertificates without its details.
            // A cluster rotated from Alert goes back there: whatever else put it in Alert hasn't been checked, and
            // the status scanner moves it to Running once its nodes are ready
            stateTransitTo(kubernetesCluster.getId(), startedFromAlert ? KubernetesCluster.Event.OperationFailed : KubernetesCluster.Event.OperationSucceeded);
            clearRotationDetails();
        } else {
            // e.g. taken for an interrupted rotation meanwhile, maybe retried since: leave the state and details alone
            logger.warn("Renewed the certificates of Kubernetes cluster {}, but the cluster was taken out of this rotation meanwhile; leaving its state",
                    kubernetesCluster);
        }
        kubernetesClusterDetailsDao.removeDetail(kubernetesCluster.getId(), ROTATION_FAILED_DETAIL);
        kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), ROTATION_SUCCEEDED_DETAIL, String.valueOf(System.currentTimeMillis()), false);
        if (automatic) {
            recordEvent(KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_CERTIFICATES_ROTATE, EventVO.LEVEL_INFO,
                    String.format("Automatically renewed the control plane certificates of Kubernetes cluster %s. Kubeconfigs downloaded " +
                            "before keep working only until their old certificate expires; download it again", kubernetesCluster.getName()));
        }
        logger.info("Rotated the control plane certificates of Kubernetes cluster {}", kubernetesCluster);
        return true;
    }

    protected boolean renewCertificatesOnControlNode(UserVm vm, int controlNodeIndex) {
        Pair<String, Integer> endpoint = getControlNodeSshEndpoint(vm, controlNodeIndex);
        long remainingSeconds = Math.max(60, (rotationTimeoutTime - System.currentTimeMillis()) / 1000);
        try {
            SshHelper.scpTo(endpoint.first(), endpoint.second(), getControlNodeLoginUser(), sshKeyFile, null,
                    "~/", rotationScriptFile.getAbsolutePath(), "0755");
            String script = rotationScriptFile.getName();
            String command = String.format("sudo ./%s %d%s; rc=$?; rm -f ./%s; exit $rc", script, remainingSeconds, rescueMode ? " rescue" : "", script);
            int timeoutMs = (int) Math.min(Integer.MAX_VALUE, (remainingSeconds + 60) * 1000L);
            Pair<Boolean, String> result = runOnNode(endpoint.first(), endpoint.second(), command, timeoutMs);
            if (!result.first()) {
                logger.error("Certificate renewal failed on control node {} of Kubernetes cluster {}: {}", vm, kubernetesCluster, result.second());
            }
            return result.first();
        } catch (Exception e) {
            logger.error("Certificate renewal failed on control node {} of Kubernetes cluster {}", vm, kubernetesCluster, e);
            return false;
        }
    }

    /**
     * Whether a renewable leaf certificate on any control node has expired, per kubeadm. Every node counts: after an
     * interrupted rescue the first nodes are renewed while the others still hold etcd hostage. Nodes that can't be
     * checked are left out.
     */
    protected boolean areCertificatesExpired(List<UserVm> controlVms) {
        CertificateExpiryReport report = new CertificateExpiryReport();
        for (int i = 0; i < controlVms.size(); i++) {
            UserVm vm = controlVms.get(i);
            Pair<String, Integer> endpoint = getControlNodeSshEndpoint(vm, i);
            try {
                Pair<Boolean, String> result = runOnNode(endpoint.first(), endpoint.second(), KubernetesClusterCertificateUtil.CHECK_EXPIRATION_COMMAND, 60000);
                report.add(vm.getHostName(), KubernetesClusterCertificateUtil.parseCheckExpirationOutput(StringUtils.defaultString(result.second())));
            } catch (Exception e) {
                logger.warn("Unable to check whether the certificates on control node {} of Kubernetes cluster {} have expired", vm, kubernetesCluster, e);
            }
        }
        // Only what the control plane itself runs on: an expired admin.conf or super-admin.conf breaks no node
        Date now = new Date();
        return report.getEntries().stream().anyMatch(entry -> !entry.isAuthority() && !entry.isMissing() && !entry.isExternallyManaged()
                && !CLIENT_ONLY_KUBECONFIGS.contains(StringUtils.substringBefore(entry.getName(), " on ")) && entry.getExpires().before(now));
    }

    /** Whether the cluster is still in the rotation this worker started, not moved on by recovery or a later rotation */
    protected boolean isStillOwnRotation() {
        KubernetesCluster current = kubernetesClusterDao.findById(kubernetesCluster.getId());
        KubernetesClusterDetailsVO started = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), ROTATION_STARTED_DETAIL);
        return current != null && KubernetesCluster.State.RotatingCertificates.equals(current.getState())
                && started != null && StringUtils.equals(rotationStartedAt, started.getValue());
    }

    /**
     * The rotation script already waited for this node's API server, controller manager and scheduler; this checks
     * the cluster API endpoint answers and the node reports Ready.
     */
    protected boolean waitForControlNodeReady(UserVm vm) {
        if (!KubernetesClusterUtil.isKubernetesClusterServerRunning(kubernetesCluster, apiPublicIpAddress, CLUSTER_API_PORT,
                rotationTimeoutTime, NODE_READY_POLL_INTERVAL_MS)) {
            return false;
        }
        String hostName = StringUtils.lowerCase(vm.getHostName());
        return KubernetesClusterUtil.isKubernetesClusterNodeReady(kubernetesCluster, publicIpAddress, sshPort, getControlNodeLoginUser(),
                sshKeyFile, hostName, rotationTimeoutTime, NODE_READY_POLL_INTERVAL_MS);
    }

    private void failRotation(String message, Exception e) {
        logMessage(Level.ERROR, message, e);
        if (isStillOwnRotation()) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            clearRotationDetails();
        }
        kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), ROTATION_FAILED_DETAIL, String.valueOf(System.currentTimeMillis()), false);
        sendAlert(String.format("Certificate rotation failed for Kubernetes cluster %s", kubernetesCluster.getName()), message);
        // the API records the failure of its own async job event
        if (automatic) {
            recordEvent(KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_CERTIFICATES_ROTATE, EventVO.LEVEL_ERROR, message);
        }
        throw new RotationFailedException(message, e);
    }

    private void clearRotationDetails() {
        kubernetesClusterDetailsDao.removeDetail(kubernetesCluster.getId(), ROTATION_STARTED_DETAIL);
        kubernetesClusterDetailsDao.removeDetail(kubernetesCluster.getId(), ROTATION_OWNER_DETAIL);
        kubernetesClusterDetailsDao.removeDetail(kubernetesCluster.getId(), ROTATION_SEEN_DETAIL);
    }

    public void sendAlert(String subject, String body) {
        try {
            alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_KUBERNETES_CLUSTER_CERTIFICATE, kubernetesCluster.getZoneId(), null, subject, body);
        } catch (Exception e) {
            logger.warn("Unable to send the alert \"{}\" for Kubernetes cluster {}", subject, kubernetesCluster, e);
        }
    }

    public void recordEvent(String eventType, String level, String description) {
        try {
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, kubernetesCluster.getAccountId(), level,
                    eventType, true, description,
                    kubernetesCluster.getId(), ApiCommandResourceType.KubernetesCluster.toString(), 0);
        } catch (Exception e) {
            logger.warn("Unable to record the event \"{}\" for Kubernetes cluster {}", description, kubernetesCluster, e);
        }
    }

    private static class RotationFailedException extends CloudRuntimeException {
        RotationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
