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

package com.cloud.kubernetes.cluster;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.api.command.user.kubernetes.cluster.RotateKubernetesClusterCertificatesCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterCertificateWorker;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil.CertificateExpiry;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil.CertificateExpiryReport;
import com.cloud.kubernetes.version.KubernetesSupportedVersionVO;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

@RunWith(MockitoJUnitRunner.Silent.class)
public class KubernetesClusterCertificateManagementTest {

    private static final long CLUSTER_ID = 11L;
    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    @Mock
    KubernetesClusterDao kubernetesClusterDao;
    @Mock
    KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    @Mock
    KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Mock
    AccountManager accountManager;

    @Spy
    @InjectMocks
    KubernetesClusterManagerImpl manager;

    @Mock
    KubernetesClusterVO cluster;
    @Mock
    KubernetesSupportedVersionVO version;
    @Mock
    KubernetesClusterCertificateWorker worker;

    private final Map<String, String> details = new HashMap<>();
    private final Map<ConfigKey<?>, Object> originalDefaults = new HashMap<>();
    private Date now;

    @Before
    public void setUp() throws Exception {
        now = new Date();
        Mockito.when(cluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(cluster.getName()).thenReturn("cks");
        Mockito.when(cluster.getAccountId()).thenReturn(2L);
        Mockito.when(cluster.getKubernetesVersionId()).thenReturn(4L);
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.CloudManaged);
        Mockito.when(kubernetesSupportedVersionDao.findById(4L)).thenReturn(version);
        Mockito.when(version.getSemanticVersion()).thenReturn("1.32.5");

        Mockito.when(kubernetesClusterDetailsDao.findDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(1);
            String value = details.get(name);
            return value == null ? null : new KubernetesClusterDetailsVO(CLUSTER_ID, name, value, false);
        });
        Mockito.doAnswer(invocation -> details.put(invocation.getArgument(1), invocation.getArgument(2)))
                .when(kubernetesClusterDetailsDao).addDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean());
        Mockito.doAnswer(invocation -> details.remove(invocation.getArgument(1)))
                .when(kubernetesClusterDetailsDao).removeDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.anyString());

        Mockito.doReturn(worker).when(manager).createCertificateWorker(cluster);
        Mockito.doReturn(true).when(manager).stateTransitTo(ArgumentMatchers.anyLong(), ArgumentMatchers.any(KubernetesCluster.Event.class));

        setDefault(KubernetesClusterService.KubernetesServiceEnabled, "true");
        setDefault(KubernetesClusterService.KubernetesClusterCertificateCheckInterval, "6");
        setDefault(KubernetesClusterService.KubernetesClusterCertificateExpiryAlertDays, "60,30");
        setDefault(KubernetesClusterService.KubernetesClusterCertificateAutoRenewEnabled, "true");
        setDefault(KubernetesClusterService.KubernetesClusterCertificateAutoRenewDays, "30");
        setDefault(KubernetesClusterService.KubernetesClusterCertificateRotationTimeout, "3600");
    }

    @After
    public void tearDown() throws Exception {
        for (Map.Entry<ConfigKey<?>, Object> entry : originalDefaults.entrySet()) {
            Field field = ConfigKey.class.getDeclaredField("_defaultValue");
            field.setAccessible(true);
            field.set(entry.getKey(), entry.getValue());
            clearCachedValue(entry.getKey());
        }
    }

    // non-dynamic keys such as cloud.kubernetes.service.enabled cache their value, which would leak into other tests
    private void clearCachedValue(ConfigKey<?> key) throws Exception {
        Field cached = ConfigKey.class.getDeclaredField("_value");
        cached.setAccessible(true);
        cached.set(key, null);
    }

    private void setDefault(ConfigKey<?> key, String value) throws Exception {
        Field field = ConfigKey.class.getDeclaredField("_defaultValue");
        field.setAccessible(true);
        originalDefaults.putIfAbsent(key, field.get(key));
        field.set(key, value);
        clearCachedValue(key);
    }

    private CertificateExpiryReport report(long leafDays, long caDays) {
        CertificateExpiryReport report = new CertificateExpiryReport();
        report.add("cks-control-1", Arrays.asList(
                new CertificateExpiry("apiserver", new Date(now.getTime() + leafDays * DAY + 3600_000L), false, false),
                new CertificateExpiry("admin.conf", new Date(now.getTime() + (leafDays + 10) * DAY), false, false),
                new CertificateExpiry("ca", new Date(now.getTime() + caDays * DAY + 3600_000L), true, false)));
        return report;
    }

    /** Runs a check at now + elapsedDays, the certificates then expiring after the given number of days. */
    private void checkAt(long elapsedDays, long leafDays, long caDays) {
        Date when = new Date(now.getTime() + elapsedDays * DAY);
        Date oldNow = now;
        now = when;
        Mockito.doReturn(report(leafDays, caDays)).when(worker).checkCertificateExpiry();
        now = oldNow;
        manager.processKubernetesClusterCertificates(cluster, when);
    }

    private ArgumentCaptor<String> verifyAlerts(int times) {
        ArgumentCaptor<String> subjects = ArgumentCaptor.forClass(String.class);
        Mockito.verify(worker, Mockito.times(times)).sendAlert(subjects.capture(), ArgumentMatchers.anyString());
        Mockito.verify(worker, Mockito.times(times)).recordEvent(ArgumentMatchers.eq(KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_CERTIFICATES_EXPIRY),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        return subjects;
    }

    private ArgumentCaptor<String> verifyAlertBodies(int times) {
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        Mockito.verify(worker, Mockito.times(times)).sendAlert(ArgumentMatchers.anyString(), bodies.capture());
        return bodies;
    }

    @Test
    public void noAlertWhileFarFromExpiry() {
        checkAt(0, 200, 3000);
        Mockito.verify(worker).checkCertificateExpiry();
        verifyAlerts(0);
        Mockito.verify(worker, Mockito.never()).rotateCertificates();
        Assert.assertNotNull(details.get(KubernetesClusterCertificateWorker.CERTIFICATE_CHECK_DETAIL));
    }

    @Test
    public void checkRunsAtMostOncePerInterval() {
        checkAt(0, 200, 3000);
        manager.processKubernetesClusterCertificates(cluster, new Date(now.getTime() + TimeUnit.HOURS.toMillis(5)));
        Mockito.verify(worker, Mockito.times(1)).checkCertificateExpiry();
        manager.processKubernetesClusterCertificates(cluster, new Date(now.getTime() + TimeUnit.HOURS.toMillis(6)));
        Mockito.verify(worker, Mockito.times(2)).checkCertificateExpiry();
    }

    @Test
    public void eachThresholdAlertsOnceThenAutoRenewTriggers() throws Exception {
        setDefault(KubernetesClusterService.KubernetesClusterCertificateAutoRenewEnabled, "false");
        // the same certificates, checked as their expiry approaches
        checkAt(0, 59, 3000);
        verifyAlerts(1);
        checkAt(1, 58, 3000);
        verifyAlerts(1);
        checkAt(29, 30, 3000);
        ArgumentCaptor<String> subjects = verifyAlerts(2);
        Assert.assertTrue(subjects.getAllValues().get(0).contains("expires in 59 days"));
        Assert.assertTrue(subjects.getAllValues().get(1).contains("expires in 30 days"));
        checkAt(30, 29, 3000);
        verifyAlerts(2);
        Mockito.verify(worker, Mockito.never()).rotateCertificates();

        setDefault(KubernetesClusterService.KubernetesClusterCertificateAutoRenewEnabled, "true");
        checkAt(31, 28, 3000);
        verifyAlerts(2);
        Mockito.verify(worker).setAutomatic(true);
        Mockito.verify(worker).rotateCertificates();
    }

    @Test
    public void automaticRenewalRunsOffTheScannerThread() {
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        manager._certificateRotationExecutor = executor;
        try {
            checkAt(0, 25, 3000);
            verifyAlerts(1);
            // handed to the rotation executor; the scanner doesn't wait for it
            Mockito.verify(executor).execute(ArgumentMatchers.any(Runnable.class));
            Mockito.verify(worker, Mockito.never()).rotateCertificates();
        } finally {
            manager._certificateRotationExecutor = null;
        }
    }

    @Test
    public void alertAdviceStatesTheEffectiveAutoRenewDays() throws Exception {
        setDefault(KubernetesClusterService.KubernetesClusterCertificateAutoRenewEnabled, "false");
        setDefault(KubernetesClusterService.KubernetesClusterCertificateAutoRenewDays, "365");
        checkAt(0, 59, 3000);
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        Mockito.verify(worker).sendAlert(ArgumentMatchers.anyString(), bodies.capture());
        Assert.assertTrue(bodies.getValue().contains("Run rotateKubernetesClusterCertificates"));

        setDefault(KubernetesClusterService.KubernetesClusterCertificateAutoRenewEnabled, "true");
        checkAt(1, 29, 3000);
        Mockito.verify(worker, Mockito.times(2)).sendAlert(ArgumentMatchers.anyString(), bodies.capture());
        Assert.assertTrue(bodies.getValue(), bodies.getValue().contains("renewed automatically 180 days before"));
    }

    @Test
    public void noAutomaticRenewalWhileAControlNodeIsUnreachable() {
        CertificateExpiryReport partial = report(25, 3000);
        partial.addUncheckedNode("cks-control-2");
        Mockito.doReturn(partial).when(worker).checkCertificateExpiry();
        manager.processKubernetesClusterCertificates(cluster, now);
        ArgumentCaptor<String> bodies = verifyAlertBodies(1);
        // the alert doesn't promise an automatic renewal that is on hold
        Assert.assertTrue(bodies.getValue(), bodies.getValue().contains("on hold until control nodes [cks-control-2] can be checked"));
        Mockito.verify(worker, Mockito.never()).rotateCertificates();
    }

    @Test
    public void clusterInAlertIsCheckedButNotRenewedAutomatically() {
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Alert);
        checkAt(0, 25, 3000);
        verifyAlerts(1);
        Mockito.verify(worker, Mockito.never()).rotateCertificates();
    }

    @Test
    public void queuedAutomaticRotationIsSkippedWhenNoLongerNeeded() {
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(cluster);
        long queuedAt = now.getTime();

        // renewed through the API while the automatic renewal waited for a thread
        details.put(KubernetesClusterCertificateWorker.ROTATION_SUCCEEDED_DETAIL, String.valueOf(queuedAt + 1000));
        manager.runQueuedAutomaticRotation(CLUSTER_ID, queuedAt);
        Mockito.verify(worker, Mockito.never()).rotateCertificates();

        // failed meanwhile, e.g. through the API: the backoff applies
        details.put(KubernetesClusterCertificateWorker.ROTATION_SUCCEEDED_DETAIL, String.valueOf(queuedAt - DAY));
        details.put(KubernetesClusterCertificateWorker.ROTATION_FAILED_DETAIL, String.valueOf(queuedAt + 1000));
        manager.runQueuedAutomaticRotation(CLUSTER_ID, queuedAt);
        Mockito.verify(worker, Mockito.never()).rotateCertificates();
        details.remove(KubernetesClusterCertificateWorker.ROTATION_FAILED_DETAIL);

        // no longer Running
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Alert);
        manager.runQueuedAutomaticRotation(CLUSTER_ID, queuedAt);
        Mockito.verify(worker, Mockito.never()).rotateCertificates();

        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Running);
        manager.runQueuedAutomaticRotation(CLUSTER_ID, queuedAt);
        Mockito.verify(worker).setAutomatic(true);
        Mockito.verify(worker).rotateCertificates();
    }

    @Test
    public void automaticRotationIsQueuedOnceAndRequeuedAfterARejection() {
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        manager._certificateRotationExecutor = executor;
        try {
            Mockito.doThrow(new RejectedExecutionException("shutting down")).doNothing()
                    .when(executor).execute(ArgumentMatchers.any(Runnable.class));
            manager.scheduleAutomaticRotation(cluster);
            // the rejected one isn't left marked as queued
            manager.scheduleAutomaticRotation(cluster);
            // still waiting for a thread: not queued again
            manager.scheduleAutomaticRotation(cluster);
            Mockito.verify(executor, Mockito.times(2)).execute(ArgumentMatchers.any(Runnable.class));
        } finally {
            manager._certificateRotationExecutor = null;
        }
    }

    @Test
    public void renewedCertificatesStartTheThresholdsOver() {
        Mockito.when(worker.rotateCertificates()).thenReturn(true);
        checkAt(0, 25, 3000);
        verifyAlerts(1);
        Mockito.verify(worker).rotateCertificates();
        // renewed: a new expiry date, a year out, then a year later the thresholds alert again
        checkAt(1, 364, 3000);
        verifyAlerts(1);
        checkAt(306, 59, 3000);
        verifyAlerts(2);
    }

    @Test
    public void certificateAuthorityExpiryAlertsButIsNeverRenewed() {
        checkAt(0, 300, 45);
        ArgumentCaptor<String> subjects = verifyAlerts(1);
        Assert.assertTrue(subjects.getValue().contains("certificate authority"));
        Mockito.verify(worker, Mockito.never()).rotateCertificates();
    }

    @Test
    public void autoRenewBacksOffAfterAFailedRotation() {
        Mockito.when(worker.rotateCertificates()).thenThrow(new CloudRuntimeException("node 2 failed"));
        details.put(KubernetesClusterCertificateWorker.ROTATION_FAILED_DETAIL, String.valueOf(now.getTime() - TimeUnit.HOURS.toMillis(2)));
        checkAt(0, 20, 3000);
        Mockito.verify(worker, Mockito.never()).rotateCertificates();

        details.put(KubernetesClusterCertificateWorker.ROTATION_FAILED_DETAIL, String.valueOf(now.getTime() - TimeUnit.HOURS.toMillis(25)));
        checkAt(1, 19, 3000);
        Mockito.verify(worker).rotateCertificates();
    }

    @Test
    public void externallyManagedCertificatesDoNotTriggerAutoRenew() {
        CertificateExpiryReport report = new CertificateExpiryReport();
        report.add("cks-control-1", Arrays.asList(
                new CertificateExpiry("apiserver-etcd-client", new Date(now.getTime() + 10 * DAY), false, true),
                new CertificateExpiry("apiserver", new Date(now.getTime() + 300 * DAY), false, false)));
        Mockito.doReturn(report).when(worker).checkCertificateExpiry();
        manager.processKubernetesClusterCertificates(cluster, now);
        verifyAlerts(1);
        Mockito.verify(worker, Mockito.never()).rotateCertificates();
    }

    @Test
    public void nothingHappensWhenTheCheckIsDisabledOrTheVersionTooOld() throws Exception {
        setDefault(KubernetesClusterService.KubernetesClusterCertificateCheckInterval, "0");
        manager.processKubernetesClusterCertificates(cluster, now);
        setDefault(KubernetesClusterService.KubernetesClusterCertificateCheckInterval, "6");
        Mockito.when(version.getSemanticVersion()).thenReturn("1.19.4");
        manager.processKubernetesClusterCertificates(cluster, now);
        Mockito.verify(worker, Mockito.never()).checkCertificateExpiry();
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void unreachableClusterIsRetriedAtTheNextInterval() {
        CertificateExpiryReport empty = new CertificateExpiryReport();
        empty.addUncheckedNode("cks-control-1");
        Mockito.doReturn(empty).when(worker).checkCertificateExpiry();
        manager.processKubernetesClusterCertificates(cluster, now);
        manager.processKubernetesClusterCertificates(cluster, new Date(now.getTime() + 60_000));
        Mockito.verify(worker, Mockito.times(1)).checkCertificateExpiry();
        // no expiry alert, only the one that the check could not run
        Mockito.verify(worker, Mockito.never()).recordEvent(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.verify(worker, Mockito.times(1)).sendAlert(ArgumentMatchers.contains("Unable to check"), ArgumentMatchers.anyString());
    }

    @Test
    public void clusterThatCannotBeReachedAtAllAlerts() {
        Mockito.doThrow(new CloudRuntimeException("Unable to retrieve the SSH address")).when(worker).checkCertificateExpiry();
        manager.processKubernetesClusterCertificates(cluster, now);
        Mockito.verify(worker).sendAlert(ArgumentMatchers.contains("Unable to check"), ArgumentMatchers.anyString());
        Mockito.verify(worker, Mockito.never()).rotateCertificates();
    }

    @Test
    public void clusterWhoseNodesCannotBeCheckedAlertsOnceADay() {
        CertificateExpiryReport nothing = new CertificateExpiryReport();
        nothing.addUncheckedNode("cks-control-1");
        Mockito.doReturn(nothing).when(worker).checkCertificateExpiry();
        manager.processKubernetesClusterCertificates(cluster, now);
        manager.processKubernetesClusterCertificates(cluster, new Date(now.getTime() + TimeUnit.HOURS.toMillis(6)));
        Mockito.verify(worker, Mockito.times(1)).sendAlert(ArgumentMatchers.contains("Unable to check"), ArgumentMatchers.anyString());
        manager.processKubernetesClusterCertificates(cluster, new Date(now.getTime() + TimeUnit.HOURS.toMillis(24)));
        Mockito.verify(worker, Mockito.times(2)).sendAlert(ArgumentMatchers.contains("Unable to check"), ArgumentMatchers.anyString());

        // reachable again: the next failure alerts straight away
        Mockito.doReturn(report(200, 3000)).when(worker).checkCertificateExpiry();
        manager.processKubernetesClusterCertificates(cluster, new Date(now.getTime() + TimeUnit.HOURS.toMillis(30)));
        Assert.assertNull(details.get(KubernetesClusterCertificateWorker.CERTIFICATE_CHECK_FAILED_ALERT_DETAIL));
    }

    @Test
    public void scannerDoesNothingWhileThePluginIsDisabled() throws Exception {
        setDefault(KubernetesClusterService.KubernetesServiceEnabled, "false");
        manager.new KubernetesClusterCertificateScanner().reallyRun();
        Mockito.verify(kubernetesClusterDao, Mockito.never()).findManagedKubernetesClustersInState(ArgumentMatchers.any());
        Mockito.verify(manager, Mockito.never()).createCertificateWorker(ArgumentMatchers.any());
    }

    @Test
    public void rotationOwnedByThisRestartedServerIsMovedToAlertRightAway() {
        Mockito.when(kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.RotatingCertificates))
                .thenReturn(Collections.singletonList(cluster));
        details.put(KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL, String.valueOf(now.getTime() - TimeUnit.MINUTES.toMillis(2)));
        details.put(KubernetesClusterCertificateWorker.ROTATION_OWNER_DETAIL, String.valueOf(ManagementServerNode.getManagementServerId()));
        manager.recoverInterruptedCertificateRotations(now);
        Mockito.verify(manager).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        Assert.assertNull(details.get(KubernetesClusterCertificateWorker.ROTATION_OWNER_DETAIL));
    }

    @Test
    public void rotationThatJustFinishedGetsNoStartTime() {
        // listed as RotatingCertificates, but finished and cleared its details before it was looked at
        Mockito.when(kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.RotatingCertificates))
                .thenReturn(Collections.singletonList(cluster));
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(cluster);
        manager.recoverInterruptedCertificateRotations(now);
        Assert.assertNull(details.get(KubernetesClusterCertificateWorker.ROTATION_SEEN_DETAIL));
    }

    @Test
    public void rotationWithoutStartTimeIsTimedFromWhenFirstSeen() {
        Mockito.when(kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.RotatingCertificates))
                .thenReturn(Collections.singletonList(cluster));
        KubernetesClusterVO rotating = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(rotating.getState()).thenReturn(KubernetesCluster.State.RotatingCertificates);
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(rotating);
        manager.recoverInterruptedCertificateRotations(now);
        Mockito.verify(manager, Mockito.never()).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        // the rotation's own start detail is never written here, it would race with the rotation writing it
        Assert.assertNull(details.get(KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL));
        Assert.assertEquals(String.valueOf(now.getTime()), details.get(KubernetesClusterCertificateWorker.ROTATION_SEEN_DETAIL));

        // still without a start well past the timeout: it died before recording it
        manager.recoverInterruptedCertificateRotations(new Date(now.getTime() + TimeUnit.HOURS.toMillis(3)));
        Mockito.verify(manager).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        Assert.assertNull(details.get(KubernetesClusterCertificateWorker.ROTATION_SEEN_DETAIL));
    }

    @Test
    public void interruptedRotationIsMovedToAlertOnceStale() {
        Mockito.when(kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.RotatingCertificates))
                .thenReturn(Collections.singletonList(cluster));
        // owned by another management server, which may still be running it
        details.put(KubernetesClusterCertificateWorker.ROTATION_OWNER_DETAIL, String.valueOf(ManagementServerNode.getManagementServerId() + 1));
        details.put(KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL, String.valueOf(now.getTime() - TimeUnit.MINUTES.toMillis(30)));
        manager.recoverInterruptedCertificateRotations(now);
        Mockito.verify(manager, Mockito.never()).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);

        details.put(KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL, String.valueOf(now.getTime() - TimeUnit.HOURS.toMillis(3)));
        manager.recoverInterruptedCertificateRotations(now);
        Mockito.verify(manager).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        Mockito.verify(worker).sendAlert(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Assert.assertNull(details.get(KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL));
        Assert.assertNotNull(details.get(KubernetesClusterCertificateWorker.ROTATION_FAILED_DETAIL));
    }

    private RotateKubernetesClusterCertificatesCmd rotateCmd() {
        RotateKubernetesClusterCertificatesCmd cmd = Mockito.mock(RotateKubernetesClusterCertificatesCmd.class);
        Mockito.when(cmd.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(cmd.getActualCommandName()).thenReturn("rotateKubernetesClusterCertificates");
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(cluster);
        return cmd;
    }

    @Test
    public void rotationApiValidation() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            RotateKubernetesClusterCertificatesCmd cmd = rotateCmd();
            Assert.assertEquals(cluster, manager.validateKubernetesClusterCertificateRotationParameters(cmd));

            Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Upgrading);
            Assert.assertThrows(InvalidParameterValueException.class, () -> manager.validateKubernetesClusterCertificateRotationParameters(cmd));

            // a failed rotation leaves the cluster in Alert, and it can be retried from there
            Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Alert);
            Assert.assertEquals(cluster, manager.validateKubernetesClusterCertificateRotationParameters(cmd));

            Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Running);
            Mockito.when(version.getSemanticVersion()).thenReturn("1.18.0");
            Assert.assertThrows(InvalidParameterValueException.class, () -> manager.validateKubernetesClusterCertificateRotationParameters(cmd));

            Mockito.when(version.getSemanticVersion()).thenReturn("1.32.5");
            Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.ExternalManaged);
            Assert.assertThrows(InvalidParameterValueException.class, () -> manager.validateKubernetesClusterCertificateRotationParameters(cmd));

            Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(null);
            Assert.assertThrows(InvalidParameterValueException.class, () -> manager.validateKubernetesClusterCertificateRotationParameters(cmd));
        } finally {
            CallContext.unregister();
        }
    }

    private KubernetesCluster.State next(KubernetesCluster.State state, KubernetesCluster.Event event) {
        try {
            return KubernetesCluster.State.getStateMachine().getNextState(state, event);
        } catch (NoTransitionException e) {
            return null;
        }
    }

    @Test
    public void stateMachineAllowsRotationOnlyFromRunning() {
        Assert.assertEquals(KubernetesCluster.State.RotatingCertificates, next(KubernetesCluster.State.Running, KubernetesCluster.Event.CertificateRotationRequested));
        Assert.assertNull(next(KubernetesCluster.State.Upgrading, KubernetesCluster.Event.CertificateRotationRequested));
        Assert.assertEquals(KubernetesCluster.State.RotatingCertificates, next(KubernetesCluster.State.Alert, KubernetesCluster.Event.CertificateRotationRequested));
        Assert.assertNull(next(KubernetesCluster.State.Stopped, KubernetesCluster.Event.CertificateRotationRequested));
        Assert.assertNull(next(KubernetesCluster.State.RotatingCertificates, KubernetesCluster.Event.CertificateRotationRequested));
        Assert.assertNull(next(KubernetesCluster.State.RotatingCertificates, KubernetesCluster.Event.UpgradeRequested));
        Assert.assertNull(next(KubernetesCluster.State.RotatingCertificates, KubernetesCluster.Event.ScaleUpRequested));
        Assert.assertEquals(KubernetesCluster.State.Running, next(KubernetesCluster.State.RotatingCertificates, KubernetesCluster.Event.OperationSucceeded));
        Assert.assertEquals(KubernetesCluster.State.Alert, next(KubernetesCluster.State.RotatingCertificates, KubernetesCluster.Event.OperationFailed));
    }
}
