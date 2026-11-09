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

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil.CertificateExpiry;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterCertificateUtil.CertificateExpiryReport;

public class KubernetesClusterCertificateUtilTest {

    // Output of kubeadm v1.31 on a stacked etcd control node
    static final String CHECK_EXPIRATION_OUTPUT =
            "[check-expiration] Reading configuration from the cluster...\n" +
            "[check-expiration] FYI: You can look at this config file with 'kubectl -n kube-system get cm kubeadm-config -o yaml'\n" +
            "\n" +
            "CERTIFICATE                EXPIRES                  RESIDUAL TIME   CERTIFICATE AUTHORITY   EXTERNALLY MANAGED\n" +
            "admin.conf                 Nov 09, 2027 19:52 UTC   364d            ca                      no      \n" +
            "apiserver                  Nov 09, 2027 19:52 UTC   364d            ca                      no      \n" +
            "apiserver-etcd-client      Nov 09, 2027 19:52 UTC   364d            etcd-ca                 no      \n" +
            "apiserver-kubelet-client   Nov 09, 2027 19:52 UTC   364d            ca                      no      \n" +
            "controller-manager.conf    Nov 09, 2027 19:52 UTC   364d            ca                      no      \n" +
            "etcd-healthcheck-client    Nov 09, 2027 19:52 UTC   364d            etcd-ca                 no      \n" +
            "etcd-peer                  Nov 09, 2027 19:52 UTC   364d            etcd-ca                 no      \n" +
            "etcd-server                Nov 09, 2027 19:52 UTC   364d            etcd-ca                 no      \n" +
            "front-proxy-client         Nov 01, 2027 08:05 UTC   356d            front-proxy-ca          no      \n" +
            "scheduler.conf             Nov 09, 2027 19:52 UTC   364d            ca                      no      \n" +
            "super-admin.conf           Nov 09, 2027 19:52 UTC   364d            ca                      no      \n" +
            "\n" +
            "CERTIFICATE AUTHORITY   EXPIRES                  RESIDUAL TIME   EXTERNALLY MANAGED\n" +
            "ca                      Nov 06, 2036 19:52 UTC   9y              no      \n" +
            "etcd-ca                 Nov 06, 2036 19:52 UTC   9y              no      \n" +
            "front-proxy-ca          Mar 03, 2035 10:00 UTC   8y              no      \n";

    // Certificate of a kubeconfig user, valid until Jan 2 00:00:00 2027 GMT
    static final String CLIENT_CERTIFICATE_DATA = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJnRENDQVNhZ0F3SUJBZ0lKQUtpUVNqdnZrSkN1TUFvR0NDcUdTTTQ5QkFNRE1EUXhGekFWQmdOVkJBb1QNCkRuTjVjM1JsYlRwdFlYTjBaWEp6TVJrd0Z3WURWUVFERXhCcmRXSmxjbTVsZEdWekxXRmtiV2x1TUI0WERUSTINCk1ERXdNVEF3TURBd01Gb1hEVEkzTURFd01qQXdNREF3TUZvd05ERVhNQlVHQTFVRUNoTU9jM2x6ZEdWdE9tMWgNCmMzUmxjbk14R1RBWEJnTlZCQU1URUd0MVltVnlibVYwWlhNdFlXUnRhVzR3V1RBVEJnY3Foa2pPUFFJQkJnZ3ENCmhrak9QUU1CQndOQ0FBUktSTGpVaDlYUGh4R0JvdUFNaFd5MmdmUWJZaG9HbEFwMVFkRDZKQ2tYTTM2YnBCSmINCjB0NGJRblJJU2pmUkpnWDVETHpBOFZhNC96Q3ZhVDdETW1jTG95RXdIekFkQmdOVkhRNEVGZ1FVVndXTXdmU0oNClRHQjBQLzJweDF4Nzh6eEVSQzR3Q2dZSUtvWkl6ajBFQXdNRFNBQXdSUUlnY1F6SGtsNk9WQm52T0tHNUNkNnMNClNxZmtMYU1oMFk1M0ovcHlBOFAwMm9FQ0lRRHB4ZkxEUFl5QzhLRVZvaW5YSUN2clo1SWVJNG5IRFUwYUtGZ2sNCml4ZGIwZz09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K";
    static final Date CLIENT_CERTIFICATE_EXPIRY = utc(2027, 1, 2, 0, 0);

    static Date utc(int year, int month, int day, int hour, int minute) {
        return Date.from(ZonedDateTime.parse(String.format("%04d-%02d-%02dT%02d:%02d:00Z", year, month, day, hour, minute)).toInstant());
    }

    static String kubeConfig(String certificateData) {
        return "apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    certificate-authority-data: Y2E=\n" +
                "    server: https://10.1.1.10:6443\n" +
                "  name: kubernetes\n" +
                "kind: Config\n" +
                "users:\n" +
                "- name: kubernetes-admin\n" +
                "  user:\n" +
                "    client-certificate-data: " + certificateData + "\n" +
                "    client-key-data: a2V5\n";
    }

    private CertificateExpiry find(List<CertificateExpiry> entries, String name) {
        return entries.stream().filter(e -> e.getName().equals(name)).findFirst().orElse(null);
    }

    @Test
    public void parseCheckExpirationOutputReadsBothTables() {
        List<CertificateExpiry> entries = KubernetesClusterCertificateUtil.parseCheckExpirationOutput(CHECK_EXPIRATION_OUTPUT);
        Assert.assertEquals(14, entries.size());
        Assert.assertEquals(11, entries.stream().filter(e -> !e.isAuthority()).count());
        Assert.assertEquals(3, entries.stream().filter(CertificateExpiry::isAuthority).count());

        CertificateExpiry admin = find(entries, "admin.conf");
        Assert.assertFalse(admin.isAuthority());
        Assert.assertFalse(admin.isMissing());
        Assert.assertFalse(admin.isExternallyManaged());
        Assert.assertEquals(utc(2027, 11, 9, 19, 52), admin.getExpires());

        Assert.assertEquals(utc(2027, 11, 1, 8, 5), find(entries, "front-proxy-client").getExpires());
        CertificateExpiry ca = find(entries, "ca");
        Assert.assertTrue(ca.isAuthority());
        Assert.assertEquals(utc(2036, 11, 6, 19, 52), ca.getExpires());
        Assert.assertEquals(utc(2035, 3, 3, 10, 0), find(entries, "front-proxy-ca").getExpires());
    }

    @Test
    public void parseCheckExpirationOutputHandlesMissingExpiredAndExternalCertificates() {
        String output = "CERTIFICATE                EXPIRES                  RESIDUAL TIME   CERTIFICATE AUTHORITY   EXTERNALLY MANAGED\n" +
                "admin.conf                 Jan 05, 2026 10:00 UTC   <invalid>       ca                      no\n" +
                "apiserver                  Feb 07, 2027 10:00 UTC   89d             ca                      no\n" +
                "!MISSING! apiserver-etcd-client\n" +
                "front-proxy-client         Dec 24, 2026 10:00 UTC   44d             front-proxy-ca          yes\n" +
                "\n" +
                "CERTIFICATE AUTHORITY   EXPIRES                  RESIDUAL TIME   EXTERNALLY MANAGED\n" +
                "ca                      Nov 06, 2036 19:52 UTC   9y              no\n" +
                "!MISSING! etcd-ca\n" +
                "some unexpected line\n";
        List<CertificateExpiry> entries = KubernetesClusterCertificateUtil.parseCheckExpirationOutput(output);
        Assert.assertEquals(6, entries.size());
        Assert.assertEquals(utc(2026, 1, 5, 10, 0), find(entries, "admin.conf").getExpires());
        CertificateExpiry missing = find(entries, "apiserver-etcd-client");
        Assert.assertTrue(missing.isMissing());
        Assert.assertFalse(missing.isAuthority());
        Assert.assertTrue(find(entries, "front-proxy-client").isExternallyManaged());
        Assert.assertTrue(find(entries, "etcd-ca").isMissing());
        Assert.assertTrue(find(entries, "etcd-ca").isAuthority());
    }

    @Test
    public void parseCheckExpirationOutputOfAFailureIsEmpty() {
        Assert.assertTrue(KubernetesClusterCertificateUtil.parseCheckExpirationOutput(null).isEmpty());
        Assert.assertTrue(KubernetesClusterCertificateUtil.parseCheckExpirationOutput("").isEmpty());
        Assert.assertTrue(KubernetesClusterCertificateUtil.parseCheckExpirationOutput("sudo: /opt/bin/kubeadm: command not found").isEmpty());
        Assert.assertTrue(KubernetesClusterCertificateUtil.parseCheckExpirationOutput(
                "error execution phase check-expiration: unknown command \"certs\" for \"kubeadm\"").isEmpty());
    }

    @Test
    public void reportPicksTheFirstExpiryOfEachKind() {
        CertificateExpiryReport report = new CertificateExpiryReport();
        report.add("control-1", KubernetesClusterCertificateUtil.parseCheckExpirationOutput(CHECK_EXPIRATION_OUTPUT));
        report.add("control-2", Arrays.asList(
                new CertificateExpiry("apiserver", utc(2027, 2, 1, 0, 0), false, false),
                new CertificateExpiry("front-proxy-client", utc(2026, 12, 1, 0, 0), false, true),
                new CertificateExpiry("apiserver-etcd-client", null, false, false)));
        report.addUncheckedNode("control-3");

        Assert.assertFalse(report.isEmpty());
        Assert.assertEquals(Collections.singletonList("control-3"), report.getUncheckedNodes());
        Assert.assertEquals("front-proxy-client on control-2", report.getFirstLeafExpiry().getName());
        Assert.assertEquals("apiserver on control-2", report.getFirstRenewableLeafExpiry().getName());
        Assert.assertEquals(utc(2027, 2, 1, 0, 0), report.getFirstRenewableLeafExpiry().getExpires());
        Assert.assertEquals("front-proxy-ca on control-1", report.getFirstAuthorityExpiry().getName());
    }

    @Test
    public void emptyReportHasNoExpiry() {
        CertificateExpiryReport report = new CertificateExpiryReport();
        Assert.assertTrue(report.isEmpty());
        Assert.assertNull(report.getFirstLeafExpiry());
        Assert.assertNull(report.getFirstRenewableLeafExpiry());
        Assert.assertNull(report.getFirstAuthorityExpiry());
    }

    @Test
    public void kubeConfigClientCertificateExpiry() {
        Assert.assertEquals(CLIENT_CERTIFICATE_EXPIRY, KubernetesClusterCertificateUtil.getKubeConfigClientCertificateExpiry(kubeConfig(CLIENT_CERTIFICATE_DATA)));
        // what ROOT_KUBECONFIG_CERTIFICATE_COMMAND prints
        Assert.assertEquals(CLIENT_CERTIFICATE_EXPIRY, KubernetesClusterCertificateUtil.getKubeConfigClientCertificateExpiry(
                "    client-certificate-data: " + CLIENT_CERTIFICATE_DATA + "\n"));
    }

    @Test
    public void kubeConfigWithoutUsableClientCertificate() {
        Assert.assertNull(KubernetesClusterCertificateUtil.getKubeConfigClientCertificateExpiry(null));
        Assert.assertNull(KubernetesClusterCertificateUtil.getKubeConfigClientCertificateExpiry("users:\n- name: x\n  user:\n    token: abc\n"));
        Assert.assertNull(KubernetesClusterCertificateUtil.getKubeConfigClientCertificateExpiry(kubeConfig("bm90IGEgY2VydGlmaWNhdGU=")));
    }

    @Test
    public void parseAlertThresholds() {
        Assert.assertEquals(Arrays.asList(60, 30), KubernetesClusterCertificateUtil.parseAlertThresholds("60,30"));
        Assert.assertEquals(Arrays.asList(90, 30, 7), KubernetesClusterCertificateUtil.parseAlertThresholds(" 7, 30 ,90,30"));
        Assert.assertEquals(Collections.singletonList(14), KubernetesClusterCertificateUtil.parseAlertThresholds("abc,0,-5,14,"));
        Assert.assertTrue(KubernetesClusterCertificateUtil.parseAlertThresholds("").isEmpty());
        Assert.assertTrue(KubernetesClusterCertificateUtil.parseAlertThresholds(null).isEmpty());
    }

    @Test
    public void thresholdToAlert() {
        List<Integer> thresholds = Arrays.asList(60, 30);
        Assert.assertNull(KubernetesClusterCertificateUtil.getThresholdToAlert(61, thresholds, null));
        Assert.assertEquals(Integer.valueOf(60), KubernetesClusterCertificateUtil.getThresholdToAlert(60, thresholds, null));
        // already alerted for 60: nothing until 30 is reached
        Assert.assertNull(KubernetesClusterCertificateUtil.getThresholdToAlert(45, thresholds, 60));
        Assert.assertEquals(Integer.valueOf(30), KubernetesClusterCertificateUtil.getThresholdToAlert(30, thresholds, 60));
        Assert.assertNull(KubernetesClusterCertificateUtil.getThresholdToAlert(10, thresholds, 30));
        // first seen inside both windows: one alert, for the smaller threshold
        Assert.assertEquals(Integer.valueOf(30), KubernetesClusterCertificateUtil.getThresholdToAlert(12, thresholds, null));
        // expiry itself alerts once more, after the thresholds
        Assert.assertEquals(Integer.valueOf(0), KubernetesClusterCertificateUtil.getThresholdToAlert(-3, thresholds, null));
        Assert.assertEquals(Integer.valueOf(0), KubernetesClusterCertificateUtil.getThresholdToAlert(-3, thresholds, 30));
        Assert.assertNull(KubernetesClusterCertificateUtil.getThresholdToAlert(-10, thresholds, 0));
        Assert.assertNull(KubernetesClusterCertificateUtil.getThresholdToAlert(5, Collections.emptyList(), null));
    }

    @Test
    public void alertStateIsTiedToTheExpiryDate() {
        Date expiry = utc(2027, 1, 2, 0, 0);
        String state = KubernetesClusterCertificateUtil.buildAlertState(expiry, 30);
        Assert.assertEquals(Integer.valueOf(30), KubernetesClusterCertificateUtil.getLastAlertedThreshold(state, expiry));
        // renewed: the expiry moved, so the thresholds start over
        Assert.assertNull(KubernetesClusterCertificateUtil.getLastAlertedThreshold(state, utc(2028, 1, 2, 0, 0)));
        // another control node's copy, renewed minutes apart, is the same expiry
        Assert.assertEquals(Integer.valueOf(30), KubernetesClusterCertificateUtil.getLastAlertedThreshold(state, utc(2027, 1, 2, 0, 7)));
        Assert.assertEquals(Integer.valueOf(30), KubernetesClusterCertificateUtil.getLastAlertedThreshold(state, utc(2026, 12, 30, 0, 0)));
        Assert.assertNull(KubernetesClusterCertificateUtil.getLastAlertedThreshold(null, expiry));
        Assert.assertNull(KubernetesClusterCertificateUtil.getLastAlertedThreshold("garbage", expiry));
        Assert.assertNull(KubernetesClusterCertificateUtil.getLastAlertedThreshold("1:x", expiry));
    }

    @Test
    public void daysUntilAndAutoRenew() {
        Date now = utc(2026, 11, 9, 12, 0);
        Assert.assertEquals(30, KubernetesClusterCertificateUtil.daysUntil(utc(2026, 12, 9, 12, 0), now));
        Assert.assertEquals(29, KubernetesClusterCertificateUtil.daysUntil(utc(2026, 12, 9, 11, 59), now));
        Assert.assertEquals(0, KubernetesClusterCertificateUtil.daysUntil(utc(2026, 11, 9, 13, 0), now));
        Assert.assertEquals(-1, KubernetesClusterCertificateUtil.daysUntil(utc(2026, 11, 9, 11, 0), now));

        Assert.assertTrue(KubernetesClusterCertificateUtil.isAutoRenewDue(30, 30));
        Assert.assertTrue(KubernetesClusterCertificateUtil.isAutoRenewDue(-2, 30));
        Assert.assertFalse(KubernetesClusterCertificateUtil.isAutoRenewDue(31, 30));
        Assert.assertFalse(KubernetesClusterCertificateUtil.isAutoRenewDue(5, 0));
        // capped, so a freshly renewed certificate (about 365 days) is never due again right away
        Assert.assertFalse(KubernetesClusterCertificateUtil.isAutoRenewDue(364, 400));
        Assert.assertTrue(KubernetesClusterCertificateUtil.isAutoRenewDue(180, 400));
    }
}
