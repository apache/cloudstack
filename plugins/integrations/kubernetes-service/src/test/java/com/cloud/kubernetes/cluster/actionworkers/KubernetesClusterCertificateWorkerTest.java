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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil.CertificateExpiryReport;
import com.cloud.kubernetes.version.KubernetesSupportedVersionVO;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;

@RunWith(MockitoJUnitRunner.Silent.class)
public class KubernetesClusterCertificateWorkerTest {

    private static final long CLUSTER_ID = 7L;

    // see KubernetesClusterCertificateUtilTest
    private static final String CHECK_EXPIRATION_OUTPUT =
            "CERTIFICATE                EXPIRES                  RESIDUAL TIME   CERTIFICATE AUTHORITY   EXTERNALLY MANAGED\n" +
            "admin.conf                 Nov 09, 2027 19:52 UTC   364d            ca                      no\n" +
            "apiserver                  Nov 09, 2027 19:52 UTC   364d            ca                      no\n" +
            "\n" +
            "CERTIFICATE AUTHORITY   EXPIRES                  RESIDUAL TIME   EXTERNALLY MANAGED\n" +
            "ca                      Nov 06, 2036 19:52 UTC   9y              no\n";
    private static final String CLIENT_CERTIFICATE_DATA = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJnRENDQVNhZ0F3SUJBZ0lKQUtpUVNqdnZrSkN1TUFvR0NDcUdTTTQ5QkFNRE1EUXhGekFWQmdOVkJBb1QNCkRuTjVjM1JsYlRwdFlYTjBaWEp6TVJrd0Z3WURWUVFERXhCcmRXSmxjbTVsZEdWekxXRmtiV2x1TUI0WERUSTINCk1ERXdNVEF3TURBd01Gb1hEVEkzTURFd01qQXdNREF3TUZvd05ERVhNQlVHQTFVRUNoTU9jM2x6ZEdWdE9tMWgNCmMzUmxjbk14R1RBWEJnTlZCQU1URUd0MVltVnlibVYwWlhNdFlXUnRhVzR3V1RBVEJnY3Foa2pPUFFJQkJnZ3ENCmhrak9QUU1CQndOQ0FBUktSTGpVaDlYUGh4R0JvdUFNaFd5MmdmUWJZaG9HbEFwMVFkRDZKQ2tYTTM2YnBCSmINCjB0NGJRblJJU2pmUkpnWDVETHpBOFZhNC96Q3ZhVDdETW1jTG95RXdIekFkQmdOVkhRNEVGZ1FVVndXTXdmU0oNClRHQjBQLzJweDF4Nzh6eEVSQzR3Q2dZSUtvWkl6ajBFQXdNRFNBQXdSUUlnY1F6SGtsNk9WQm52T0tHNUNkNnMNClNxZmtMYU1oMFk1M0ovcHlBOFAwMm9FQ0lRRHB4ZkxEUFl5QzhLRVZvaW5YSUN2clo1SWVJNG5IRFUwYUtGZ2sNCml4ZGIwZz09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K";
    // what the combined check command prints: kubeadm's tables, root's kubeconfig certificate line, and nothing for
    // user.conf when there is none
    private static final String CHECK_OUTPUT = CHECK_EXPIRATION_OUTPUT + "----- cks root kubeconfig -----\n" +
            "    client-certificate-data: " + CLIENT_CERTIFICATE_DATA + "\n" +
            "----- cks user kubeconfig -----\n";

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private KubernetesClusterDao kubernetesClusterDao;
    @Mock
    private KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    @Mock
    private KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Mock
    private KubernetesSupportedVersionVO version;
    @Mock
    private UserVm control1;
    @Mock
    private UserVm control2;
    @Mock
    private UserVm control3;

    private KubernetesClusterCertificateWorker worker;
    private File scriptFile;

    @Before
    public void setUp() throws IOException {
        clusterManager.kubernetesClusterDao = kubernetesClusterDao;
        clusterManager.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        clusterManager.kubernetesSupportedVersionDao = kubernetesSupportedVersionDao;
        Mockito.when(kubernetesCluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(kubernetesCluster.getName()).thenReturn("cks");
        Mockito.when(kubernetesCluster.getKubernetesVersionId()).thenReturn(3L);
        Mockito.when(kubernetesSupportedVersionDao.findById(3L)).thenReturn(version);
        Mockito.when(version.getSemanticVersion()).thenReturn("1.31.1");
        Mockito.when(control1.getHostName()).thenReturn("cks-control-1");
        Mockito.when(control2.getHostName()).thenReturn("cks-control-2");
        Mockito.when(control3.getHostName()).thenReturn("cks-control-3");

        worker = Mockito.spy(new KubernetesClusterCertificateWorker(kubernetesCluster, clusterManager));
        scriptFile = File.createTempFile("rotate-kubernetes-certificates.sh", ".sh");
        Mockito.doNothing().when(worker).init();
        Mockito.doNothing().when(worker).resolveClusterEndpoints();
        Mockito.doReturn(Arrays.asList(control1, control2, control3)).when(worker).getControlNodeVMs();
        Mockito.doReturn(scriptFile).when(worker).retrieveScriptFile(KubernetesClusterCertificateWorker.ROTATION_SCRIPT_FILENAME);
        Mockito.doReturn(true).when(worker).stateTransitTo(ArgumentMatchers.anyLong(), ArgumentMatchers.any(KubernetesCluster.Event.class));
        Mockito.doReturn(true).when(worker).renewCertificatesOnControlNode(ArgumentMatchers.any(UserVm.class), ArgumentMatchers.anyInt());
        Mockito.doReturn(true).when(worker).waitForControlNodeReady(ArgumentMatchers.any(UserVm.class));
        Mockito.doReturn(false).when(worker).areCertificatesExpired(ArgumentMatchers.anyList());
        Mockito.doReturn(true).when(worker).isStillOwnRotation();
        Mockito.doReturn(true).when(worker).refreshKubeConfigData(ArgumentMatchers.anyLong());
        Mockito.doNothing().when(worker).sendAlert(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.doNothing().when(worker).recordEvent(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
    }

    @After
    public void tearDown() {
        scriptFile.delete();
    }

    @Test
    public void rotationRenewsOneControlNodeAtATimeThenRefreshesTheKubeConfig() {
        Assert.assertTrue(worker.rotateCertificates());

        InOrder inOrder = Mockito.inOrder(worker, kubernetesClusterDetailsDao);
        inOrder.verify(worker).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.CertificateRotationRequested);
        inOrder.verify(kubernetesClusterDetailsDao).addDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.eq(KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL), ArgumentMatchers.anyString(), ArgumentMatchers.eq(false));
        inOrder.verify(kubernetesClusterDetailsDao).addDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.eq(KubernetesClusterCertificateWorker.ROTATION_OWNER_DETAIL), ArgumentMatchers.anyString(), ArgumentMatchers.eq(false));
        inOrder.verify(worker).renewCertificatesOnControlNode(control1, 0);
        inOrder.verify(worker).waitForControlNodeReady(control1);
        inOrder.verify(worker).renewCertificatesOnControlNode(control2, 1);
        inOrder.verify(worker).waitForControlNodeReady(control2);
        inOrder.verify(worker).renewCertificatesOnControlNode(control3, 2);
        inOrder.verify(worker).waitForControlNodeReady(control3);
        inOrder.verify(worker).refreshKubeConfigData(ArgumentMatchers.anyLong());
        inOrder.verify(worker).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationSucceeded);
        inOrder.verify(kubernetesClusterDetailsDao).removeDetail(CLUSTER_ID, KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL);
        inOrder.verify(kubernetesClusterDetailsDao).removeDetail(CLUSTER_ID, KubernetesClusterCertificateWorker.ROTATION_OWNER_DETAIL);
        Assert.assertFalse(KubernetesClusterCertificateWorker.isRotationActive(CLUSTER_ID));
        Mockito.verify(worker, Mockito.never()).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        Mockito.verify(worker, Mockito.never()).sendAlert(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        // the API records its own event; only an automatic renewal records one here
        Mockito.verify(worker, Mockito.never()).recordEvent(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Assert.assertFalse("the local copy of the script is cleaned up", scriptFile.exists());
    }

    @Test
    public void automaticRotationRecordsAnEvent() {
        worker.setAutomatic(true);
        Assert.assertTrue(worker.rotateCertificates());
        Mockito.verify(worker).recordEvent(ArgumentMatchers.eq("KUBERNETES.CLUSTER.CERTIFICATES.ROTATE"), ArgumentMatchers.eq("INFO"), ArgumentMatchers.anyString());
    }

    private void assertRotationFailed() {
        try {
            worker.rotateCertificates();
            Assert.fail("rotation should have failed");
        } catch (CloudRuntimeException e) {
            // expected
        }
        Mockito.verify(worker).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        Mockito.verify(worker, Mockito.never()).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationSucceeded);
        Mockito.verify(worker).sendAlert(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.verify(kubernetesClusterDetailsDao).addDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.eq(KubernetesClusterCertificateWorker.ROTATION_FAILED_DETAIL), ArgumentMatchers.anyString(), ArgumentMatchers.eq(false));
        Mockito.verify(kubernetesClusterDetailsDao).removeDetail(CLUSTER_ID, KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL);
        Mockito.verify(kubernetesClusterDetailsDao).removeDetail(CLUSTER_ID, KubernetesClusterCertificateWorker.ROTATION_OWNER_DETAIL);
        Assert.assertFalse(KubernetesClusterCertificateWorker.isRotationActive(CLUSTER_ID));
        // the API's own async job event records the failure; only an automatic renewal records one here
        Mockito.verify(worker, Mockito.never()).recordEvent(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
    }

    @Test
    public void automaticRotationRecordsItsFailure() {
        worker.setAutomatic(true);
        Mockito.doReturn(false).when(worker).renewCertificatesOnControlNode(control1, 0);
        Assert.assertThrows(CloudRuntimeException.class, () -> worker.rotateCertificates());
        Mockito.verify(worker).recordEvent(ArgumentMatchers.eq("KUBERNETES.CLUSTER.CERTIFICATES.ROTATE"), ArgumentMatchers.eq("ERROR"), ArgumentMatchers.anyString());
    }

    @Test
    public void rotationOfAnUnreachableClusterRenewsEveryNodeBeforeWaiting() {
        // expired certificates: with stacked etcd no node gets quorum until all are renewed
        Mockito.doReturn(true).when(worker).areCertificatesExpired(ArgumentMatchers.anyList());

        Assert.assertTrue(worker.rotateCertificates());

        InOrder inOrder = Mockito.inOrder(worker);
        inOrder.verify(worker).renewCertificatesOnControlNode(control1, 0);
        inOrder.verify(worker).renewCertificatesOnControlNode(control2, 1);
        inOrder.verify(worker).renewCertificatesOnControlNode(control3, 2);
        inOrder.verify(worker).waitForControlNodeReady(control1);
        inOrder.verify(worker).waitForControlNodeReady(control2);
        inOrder.verify(worker).waitForControlNodeReady(control3);
        inOrder.verify(worker).refreshKubeConfigData(ArgumentMatchers.anyLong());
        inOrder.verify(worker).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationSucceeded);
    }

    @Test
    public void secondRotationOfTheSameClusterIsRefusedWithoutDisturbingTheFirst() {
        Mockito.doAnswer(invocation -> {
            // a second rotation starts while the first renews node 0
            Assert.assertThrows(CloudRuntimeException.class, () -> worker.rotateCertificates());
            Assert.assertTrue(KubernetesClusterCertificateWorker.isRotationActive(CLUSTER_ID));
            return true;
        }).when(worker).renewCertificatesOnControlNode(control1, 0);

        Assert.assertTrue(worker.rotateCertificates());
        Mockito.verify(worker, Mockito.times(1)).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.CertificateRotationRequested);
        Assert.assertFalse(KubernetesClusterCertificateWorker.isRotationActive(CLUSTER_ID));
    }

    @Test
    public void rotationFromAlertReturnsToAlertForTheStatusScanner() {
        KubernetesClusterVO alertCluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(alertCluster.getState()).thenReturn(KubernetesCluster.State.Alert);
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(alertCluster);

        Assert.assertTrue(worker.rotateCertificates());

        // whatever else put it in Alert hasn't been checked; the status scanner reconciles it to Running
        Mockito.verify(worker).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        Mockito.verify(worker, Mockito.never()).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationSucceeded);
        Mockito.verify(worker, Mockito.never()).sendAlert(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.verify(kubernetesClusterDetailsDao, Mockito.never()).addDetail(ArgumentMatchers.eq(CLUSTER_ID),
                ArgumentMatchers.eq(KubernetesClusterCertificateWorker.ROTATION_FAILED_DETAIL), ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean());
    }

    @Test
    public void rotationLeavesAClusterTakenMeanwhileInItsState() {
        // e.g. moved to Alert as an interrupted rotation by another management server, and maybe retried since
        Mockito.doReturn(false).when(worker).isStillOwnRotation();
        Assert.assertTrue(worker.rotateCertificates());
        Mockito.verify(worker, Mockito.never()).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationSucceeded);
        Mockito.verify(kubernetesClusterDetailsDao, Mockito.never()).removeDetail(CLUSTER_ID, KubernetesClusterCertificateWorker.ROTATION_STARTED_DETAIL);
    }

    @Test
    public void rotationOfAnUnreachableClusterFailsWhenANodeDoesNotComeBack() {
        Mockito.doReturn(true).when(worker).areCertificatesExpired(ArgumentMatchers.anyList());
        Mockito.doReturn(false).when(worker).waitForControlNodeReady(control2);

        assertRotationFailed();

        Mockito.verify(worker).renewCertificatesOnControlNode(control3, 2);
        Mockito.verify(worker, Mockito.never()).refreshKubeConfigData(ArgumentMatchers.anyLong());
    }

    @Test
    public void rotationStopsAtTheFirstControlNodeThatFailsToRenew() {
        Mockito.doReturn(false).when(worker).renewCertificatesOnControlNode(control2, 1);

        assertRotationFailed();

        Mockito.verify(worker).renewCertificatesOnControlNode(control1, 0);
        Mockito.verify(worker).waitForControlNodeReady(control1);
        Mockito.verify(worker).renewCertificatesOnControlNode(control2, 1);
        Mockito.verify(worker, Mockito.never()).waitForControlNodeReady(control2);
        Mockito.verify(worker, Mockito.never()).renewCertificatesOnControlNode(ArgumentMatchers.eq(control3), ArgumentMatchers.anyInt());
        Mockito.verify(worker, Mockito.never()).refreshKubeConfigData(ArgumentMatchers.anyLong());
    }

    @Test
    public void rotationStopsWhenAControlNodeDoesNotBecomeReady() {
        Mockito.doReturn(false).when(worker).waitForControlNodeReady(control1);

        assertRotationFailed();

        Mockito.verify(worker).renewCertificatesOnControlNode(control1, 0);
        Mockito.verify(worker, Mockito.never()).renewCertificatesOnControlNode(ArgumentMatchers.eq(control2), ArgumentMatchers.anyInt());
        Mockito.verify(worker, Mockito.never()).renewCertificatesOnControlNode(ArgumentMatchers.eq(control3), ArgumentMatchers.anyInt());
        Mockito.verify(worker, Mockito.never()).refreshKubeConfigData(ArgumentMatchers.anyLong());
    }

    @Test
    public void rotationFailsWhenTheKubeConfigCannotBeRefreshed() {
        Mockito.doReturn(false).when(worker).refreshKubeConfigData(ArgumentMatchers.anyLong());

        assertRotationFailed();

        Mockito.verify(worker).renewCertificatesOnControlNode(control3, 2);
    }

    @Test
    public void rotationFailsOnAnUnexpectedError() {
        Mockito.doThrow(new CloudRuntimeException("no public IP")).when(worker).resolveClusterEndpoints();

        assertRotationFailed();

        Mockito.verify(worker, Mockito.never()).renewCertificatesOnControlNode(ArgumentMatchers.any(UserVm.class), ArgumentMatchers.anyInt());
    }

    @Test
    public void rotationIsRefusedUnlessTheClusterCanTransitionFromRunning() {
        Mockito.doReturn(false).when(worker).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.CertificateRotationRequested);

        Assert.assertThrows(CloudRuntimeException.class, () -> worker.rotateCertificates());

        Mockito.verify(worker, Mockito.never()).renewCertificatesOnControlNode(ArgumentMatchers.any(UserVm.class), ArgumentMatchers.anyInt());
        Mockito.verify(worker, Mockito.never()).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        Mockito.verify(worker, Mockito.never()).sendAlert(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        // a refused request leaves the running rotation's bookkeeping alone
        Mockito.verify(kubernetesClusterDetailsDao, Mockito.never()).addDetail(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean());
        Mockito.verify(kubernetesClusterDetailsDao, Mockito.never()).removeDetail(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString());
    }

    @Test
    public void rotationIsActiveWhileRunning() {
        Mockito.doAnswer(invocation -> {
            Assert.assertTrue(KubernetesClusterCertificateWorker.isRotationActive(CLUSTER_ID));
            return true;
        }).when(worker).renewCertificatesOnControlNode(ArgumentMatchers.any(UserVm.class), ArgumentMatchers.anyInt());
        Assert.assertTrue(worker.rotateCertificates());
        Assert.assertFalse(KubernetesClusterCertificateWorker.isRotationActive(CLUSTER_ID));
    }

    @Test
    public void rotationIsRefusedBeforeKubernetes120() {
        Mockito.when(version.getSemanticVersion()).thenReturn("1.19.16");

        Assert.assertThrows(CloudRuntimeException.class, () -> worker.rotateCertificates());

        Mockito.verify(worker, Mockito.never()).stateTransitTo(ArgumentMatchers.anyLong(), ArgumentMatchers.any(KubernetesCluster.Event.class));
        Mockito.verify(worker, Mockito.never()).renewCertificatesOnControlNode(ArgumentMatchers.any(UserVm.class), ArgumentMatchers.anyInt());
    }

    @Test
    public void certificateManagementVersionGate() {
        Assert.assertFalse(KubernetesClusterCertificateWorker.isCertificateManagementSupported(null));
        Mockito.when(version.getSemanticVersion()).thenReturn("1.19.16");
        Assert.assertFalse(KubernetesClusterCertificateWorker.isCertificateManagementSupported(version));
        Mockito.when(version.getSemanticVersion()).thenReturn("1.20.0");
        Assert.assertTrue(KubernetesClusterCertificateWorker.isCertificateManagementSupported(version));
        Mockito.when(version.getSemanticVersion()).thenReturn("1.33.1");
        Assert.assertTrue(KubernetesClusterCertificateWorker.isCertificateManagementSupported(version));
    }

    private void storeKubeConfig(String certificateData) {
        KubernetesClusterDetailsVO detail = Mockito.mock(KubernetesClusterDetailsVO.class);
        String kubeConfig = "users:\n- name: kubernetes-admin\n  user:\n    client-certificate-data: " + certificateData + "\n";
        Mockito.when(detail.getValue()).thenReturn(Base64.encodeBase64String(kubeConfig.getBytes(StandardCharsets.UTF_8)));
        Mockito.when(kubernetesClusterDetailsDao.findDetail(CLUSTER_ID, KubernetesClusterActionWorker.KUBE_CONFIG_DATA_DETAIL)).thenReturn(detail);
    }

    @Test
    public void checkReadsEveryControlNodeAndRefreshesAStaleStoredKubeConfig() throws Exception {
        // the stored kubeconfig's certificate expires in Jan 2027, the cluster's admin.conf in Nov 2027
        storeKubeConfig(CLIENT_CERTIFICATE_DATA);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2222)).when(worker).getControlNodeSshEndpoint(control1, 0);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2223)).when(worker).getControlNodeSshEndpoint(control2, 1);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2224)).when(worker).getControlNodeSshEndpoint(control3, 2);
        Mockito.doReturn(new Pair<>(true, CHECK_OUTPUT)).when(worker).runOnNode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.contains(KubernetesClusterCertificateUtil.CHECK_EXPIRATION_COMMAND), ArgumentMatchers.anyInt());
        Mockito.doReturn(new Pair<>(false, "connection refused")).when(worker).runOnNode(ArgumentMatchers.eq("10.1.1.1"), ArgumentMatchers.eq(2224),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());

        CertificateExpiryReport report = worker.checkCertificateExpiry();

        Assert.assertEquals(List.of("cks-control-3"), report.getUncheckedNodes());
        // 3 certificates from kubeadm and root's kubeconfig, on two nodes
        Assert.assertEquals(8, report.getEntries().size());
        // root's kubeconfig expires first: renewing syncs it with admin.conf
        Assert.assertEquals("/root/.kube/config on cks-control-1", report.getFirstLeafExpiry().getName());
        Assert.assertEquals("ca on cks-control-1", report.getFirstAuthorityExpiry().getName());
        Mockito.verify(worker).refreshKubeConfigData(ArgumentMatchers.anyLong());
    }

    @Test
    public void checkComparesTheStoredKubeConfigWithUserConfWhenThereIsOne() throws Exception {
        // getKubernetesClusterConfig hands out user.conf; its certificate is the one stored, so nothing is stale
        storeKubeConfig(CLIENT_CERTIFICATE_DATA);
        String withUserConf = CHECK_OUTPUT + "user.conf present\n    client-certificate-data: " + CLIENT_CERTIFICATE_DATA + "\n";
        Mockito.doReturn(new Pair<>("10.1.1.1", 2222)).when(worker).getControlNodeSshEndpoint(control1, 0);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2223)).when(worker).getControlNodeSshEndpoint(control2, 1);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2224)).when(worker).getControlNodeSshEndpoint(control3, 2);
        Mockito.doReturn(new Pair<>(true, withUserConf)).when(worker).runOnNode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());

        CertificateExpiryReport report = worker.checkCertificateExpiry();

        Assert.assertEquals(12, report.getEntries().size());
        Mockito.verify(worker, Mockito.never()).refreshKubeConfigData(ArgumentMatchers.anyLong());

        // a user.conf without a client certificate (e.g. a token) can't be compared, so it is left alone too
        Mockito.doReturn(new Pair<>(true, CHECK_OUTPUT + "user.conf present\n")).when(worker).runOnNode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        worker.checkCertificateExpiry();
        Mockito.verify(worker, Mockito.never()).refreshKubeConfigData(ArgumentMatchers.anyLong());
    }

    @Test
    public void expiryForRescueLooksAtEveryControlNode() throws Exception {
        String expired = CHECK_EXPIRATION_OUTPUT.replace("apiserver                  Nov 09, 2027 19:52 UTC   364d     ",
                "apiserver                  Jan 01, 2020 00:00 UTC   <invalid>");
        Mockito.doReturn(new Pair<>("10.1.1.1", 2222)).when(worker).getControlNodeSshEndpoint(control1, 0);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2223)).when(worker).getControlNodeSshEndpoint(control2, 1);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2224)).when(worker).getControlNodeSshEndpoint(control3, 2);
        Mockito.doReturn(new Pair<>(true, CHECK_EXPIRATION_OUTPUT)).when(worker).runOnNode(ArgumentMatchers.eq("10.1.1.1"), ArgumentMatchers.eq(2222),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        Mockito.doThrow(new java.io.IOException("timeout")).when(worker).runOnNode(ArgumentMatchers.eq("10.1.1.1"), ArgumentMatchers.eq(2224),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        Mockito.doReturn(new Pair<>(true, CHECK_EXPIRATION_OUTPUT)).when(worker).runOnNode(ArgumentMatchers.eq("10.1.1.1"), ArgumentMatchers.eq(2223),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        Mockito.doCallRealMethod().when(worker).areCertificatesExpired(ArgumentMatchers.anyList());
        List<UserVm> nodes = Arrays.asList(control1, control2, control3);

        Assert.assertFalse(worker.areCertificatesExpired(nodes));

        // an expired admin client certificate breaks no control plane component: no reason to restart every node at once
        String adminExpired = CHECK_EXPIRATION_OUTPUT.replace("admin.conf                 Nov 09, 2027 19:52 UTC   364d     ",
                "admin.conf                 Jan 01, 2020 00:00 UTC   <invalid>");
        Assert.assertNotEquals(CHECK_EXPIRATION_OUTPUT, adminExpired);
        Mockito.doReturn(new Pair<>(true, adminExpired)).when(worker).runOnNode(ArgumentMatchers.eq("10.1.1.1"), ArgumentMatchers.eq(2223),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        Assert.assertFalse(worker.areCertificatesExpired(nodes));

        // the first node already renewed by an interrupted rescue, the second still expired
        Mockito.doReturn(new Pair<>(false, expired)).when(worker).runOnNode(ArgumentMatchers.eq("10.1.1.1"), ArgumentMatchers.eq(2223),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        Assert.assertTrue(worker.areCertificatesExpired(nodes));
    }

    @Test
    public void rootKubeConfigIsNotRenewableInExternalCaMode() throws Exception {
        String external = CHECK_OUTPUT.replace("ca                      no\n", "ca                      yes\n");
        Mockito.doReturn(new Pair<>("10.1.1.1", 2222)).when(worker).getControlNodeSshEndpoint(ArgumentMatchers.any(UserVm.class), ArgumentMatchers.anyInt());
        Mockito.doReturn(new Pair<>(true, external)).when(worker).runOnNode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());

        CertificateExpiryReport report = worker.checkCertificateExpiry();

        Assert.assertEquals("/root/.kube/config on cks-control-1", report.getFirstLeafExpiry().getName());
        Assert.assertNull(report.getFirstRenewableLeafExpiry());
    }

    @Test
    public void checkSkipsTheKubeConfigRefreshWhenTheFirstControlNodeIsDown() throws Exception {
        storeKubeConfig(CLIENT_CERTIFICATE_DATA);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2222)).when(worker).getControlNodeSshEndpoint(control1, 0);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2223)).when(worker).getControlNodeSshEndpoint(control2, 1);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2224)).when(worker).getControlNodeSshEndpoint(control3, 2);
        Mockito.doReturn(new Pair<>(true, CHECK_OUTPUT)).when(worker).runOnNode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        Mockito.doThrow(new java.io.IOException("timeout")).when(worker).runOnNode(ArgumentMatchers.eq("10.1.1.1"), ArgumentMatchers.eq(2222),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());

        CertificateExpiryReport report = worker.checkCertificateExpiry();

        Assert.assertEquals(List.of("cks-control-1"), report.getUncheckedNodes());
        Mockito.verify(worker, Mockito.never()).refreshKubeConfigData(ArgumentMatchers.anyLong());
    }

    @Test
    public void storedKubeConfigIsLeftAloneWhenCurrent() {
        storeKubeConfig(CLIENT_CERTIFICATE_DATA);
        Date storedExpiry = worker.getStoredKubeConfigExpiry();
        Assert.assertNotNull(storedExpiry);

        worker.refreshStaleKubeConfigData(storedExpiry);
        worker.refreshStaleKubeConfigData(new Date(storedExpiry.getTime() - 1000));
        worker.refreshStaleKubeConfigData(null);
        Mockito.verify(worker, Mockito.never()).refreshKubeConfigData(ArgumentMatchers.anyLong());

        worker.refreshStaleKubeConfigData(new Date(storedExpiry.getTime() + 1000));
        Mockito.verify(worker).refreshKubeConfigData(ArgumentMatchers.anyLong());
    }

    private PortForwardingRuleVO sshRule(long ipId, int sourcePort, int destinationPort) {
        PortForwardingRuleVO rule = Mockito.mock(PortForwardingRuleVO.class);
        Mockito.when(rule.getSourceIpAddressId()).thenReturn(ipId);
        Mockito.when(rule.getSourcePortStart()).thenReturn(sourcePort);
        Mockito.when(rule.getDestinationPortStart()).thenReturn(destinationPort);
        Mockito.when(rule.getState()).thenReturn(FirewallRule.State.Active);
        return rule;
    }

    @Test
    public void controlNodeSshEndpoint() {
        PortForwardingRulesDao portForwardingRulesDao = Mockito.mock(PortForwardingRulesDao.class);
        IPAddressDao ipAddressDao = Mockito.mock(IPAddressDao.class);
        worker.portForwardingRulesDao = portForwardingRulesDao;
        worker.ipAddressDao = ipAddressDao;
        IPAddressVO clusterIp = Mockito.mock(IPAddressVO.class);
        Mockito.when(clusterIp.getAddress()).thenReturn(new Ip("192.0.2.10"));
        IPAddressVO otherIp = Mockito.mock(IPAddressVO.class);
        Mockito.when(otherIp.getAddress()).thenReturn(new Ip("192.0.2.99"));
        Mockito.when(ipAddressDao.findById(1L)).thenReturn(clusterIp);
        Mockito.when(ipAddressDao.findById(2L)).thenReturn(otherIp);
        Mockito.when(control1.getId()).thenReturn(101L);
        Mockito.when(control2.getId()).thenReturn(102L);
        Mockito.when(control2.getPrivateIpAddress()).thenReturn("10.1.1.12");

        // directly reachable network: the VM's own address
        worker.publicIpAddress = "10.1.1.11";
        worker.sshPort = 22;
        Assert.assertEquals(new Pair<>("10.1.1.12", 22), worker.getControlNodeSshEndpoint(control2, 1));

        // behind the cluster public IP: the VM's SSH port forwarding rule on that IP, not a user's on another one
        worker.publicIpAddress = "192.0.2.10";
        worker.sshPort = 2222;
        List<PortForwardingRuleVO> rules = Arrays.asList(sshRule(2L, 2022, 22), sshRule(1L, 8080, 80), sshRule(1L, 2224, 22));
        Mockito.when(portForwardingRulesDao.listByVm(102L)).thenReturn(rules);
        Assert.assertEquals(new Pair<>("192.0.2.10", 2224), worker.getControlNodeSshEndpoint(control2, 1));

        // no rule (external load balancer): by position
        Mockito.when(portForwardingRulesDao.listByVm(101L)).thenReturn(Collections.emptyList());
        Assert.assertEquals(new Pair<>("192.0.2.10", 2222), worker.getControlNodeSshEndpoint(control1, 0));
        Assert.assertEquals(new Pair<>("192.0.2.10", 2223), worker.getControlNodeSshEndpoint(control1, 1));
    }
}
